package dev.slne.minestom.lobby.server.command.commandapi

import dev.slne.minestom.lobby.api.command.commandapi.CommandPath
import dev.slne.minestom.lobby.api.command.commandapi.argument.ArgumentDefinition
import dev.slne.minestom.lobby.api.command.commandapi.argument.ArgumentKind
import dev.slne.minestom.lobby.api.command.commandapi.argument.InputShape
import dev.slne.minestom.lobby.api.command.commandapi.argument.SuggestionMode
import dev.slne.minestom.lobby.api.command.commandapi.executor.CommandArguments
import dev.slne.minestom.lobby.api.command.commandapi.executor.ParsedArgument
import dev.slne.minestom.lobby.api.command.commandapi.suggestion.SafeSuggestions
import dev.slne.minestom.lobby.api.command.commandapi.suggestion.StringTooltip
import dev.slne.minestom.lobby.api.command.commandapi.suggestion.SuggestionFilter
import dev.slne.minestom.lobby.api.command.commandapi.suggestion.SuggestionInfo
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSet
import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.potion.PotionEffect
import net.minestom.server.sound.SoundEvent

internal class MinestomSuggestionAdapter(
    private val conditions: MinestomConditions,
) {
    fun adapt(
        path: CommandPath,
        compiledArguments: List<CompiledArgument<*>>,
        fixedByName: Map<String, String>,
    ) {
        val compiledByName = Object2ObjectOpenHashMap<String, CompiledArgument<*>>(
            compiledArguments.size
        )
        compiledArguments.forEach { argument ->
            compiledByName[argument.definition.nodeName] = argument
        }

        compiledArguments.forEach { compiled ->
            val definition = compiled.definition
            val suggestionOwner = suggestionOwner(definition)
            val suggestionMode = suggestionOwner.suggestions

            if (!requiresServerSuggestions(definition)) return@forEach

            val argumentIndex = path.arguments.indexOfFirst { argument ->
                argument.nodeName == definition.nodeName
            }

            check(argumentIndex >= 0) {
                "Compiled suggestion argument '${definition.nodeName}' is not in its command path"
            }

            val builtInDefinition = builtInDefinition(definition)
            val accumulatedConditions = conditionsFor(path, argumentIndex)

            compiled.native.setSuggestionCallback { sender, context, nativeSuggestion ->
                val logicalInput = CompletionCapture.logicalInput(nativeSuggestion.input)
                val range = SuggestionCursor.scan(
                    input = logicalInput,
                    shape = definition.inputShape,
                    delimiter = definition.listDelimiter,
                    argumentStart = if (definition.inputShape == InputShape.GREEDY) {
                        argumentStart(
                            context,
                            path.arguments,
                            argumentIndex,
                            fixedByName,
                        )
                    } else {
                        null
                    },
                )

                nativeSuggestion.start = range.start
                nativeSuggestion.length = range.length

                val info = suggestionInfo(
                    sender,
                    context,
                    path,
                    argumentIndex,
                    compiledByName,
                    fixedByName,
                    range.current,
                )

                CompletionCapture.record(
                    MinestomSuggestionRequest(
                        commandName = context.commandName,
                        argumentName = definition.nodeName,
                        input = logicalInput,
                        range = range,
                        providerDescription = providerDescription(suggestionMode),
                        resolve = {
                            if (!conditions.canUse(
                                    sender,
                                    accumulatedConditions.permissions,
                                    accumulatedConditions.requirements,
                                )
                            ) {
                                emptyList()
                            } else {
                                resolveEntries(
                                    suggestionOwner,
                                    builtInEntries(builtInDefinition),
                                    suggestionMode,
                                    info,
                                    range.current,
                                )
                            }
                        },
                    ),
                )
            }
        }
    }

    private fun requiresServerSuggestions(definition: ArgumentDefinition<*>): Boolean {
        return definition.suggestions != SuggestionMode.BuiltIns || when (val kind =
            definition.kind) {
            is ArgumentKind.Custom<*, *> -> {
                requiresServerSuggestions(kind.base) ||
                        builtInEntries(builtInDefinition(kind.base)).isNotEmpty()
            }

            is ArgumentKind.List<*> -> {
                requiresServerSuggestions(kind.element) ||
                        builtInEntries(builtInDefinition(kind.element)).isNotEmpty()
            }

            else -> hasServerBuiltIns(definition)
        }
    }

    private fun conditionsFor(path: CommandPath, argumentIndex: Int): AccumulatedConditions {
        var permissionCapacity = path.permissions.size
        var requirementCapacity = path.requirements.size

        for (index in 0..argumentIndex) {
            val definition = path.arguments[index]
            permissionCapacity += definition.permissions.size
            requirementCapacity += definition.requirements.size
        }

        val permissions = ObjectOpenHashSet<String>(permissionCapacity)
        permissions.addAll(path.permissions)

        val requirements = ObjectArrayList<(CommandSender) -> Boolean>(requirementCapacity)
        requirements.addAll(path.requirements)

        for (index in 0..argumentIndex) {
            val definition = path.arguments[index]
            permissions.addAll(definition.permissions)
            requirements.addAll(definition.requirements)
        }

        return AccumulatedConditions(
            permissions = permissions,
            requirements = requirements,
        )
    }

    private fun suggestionInfo(
        sender: CommandSender,
        context: CommandContext,
        path: CommandPath,
        argumentIndex: Int,
        compiledByName: Map<String, CompiledArgument<*>>,
        fixedByName: Map<String, String>,
        current: String,
    ): SuggestionInfo {
        val previous = ObjectArrayList<ParsedArgument>(argumentIndex)

        for (index in 0 until argumentIndex) {
            val definition = path.arguments[index]
            val name = definition.nodeName

            val fixed = fixedByName[name]
            val compiled = compiledByName[name]

            val presentInContext =
                compiled != null && context.has(name)

            val raw = if (presentInContext) {
                context.getRaw(name)
            } else {
                null
            }

            val nativeValue = if (presentInContext) {
                context.get<Any?>(name)
            } else {
                null
            }

            val parsed =
                presentInContext &&
                        !(nativeValue == null && raw.isNullOrBlank())

            val value = when {
                fixed != null -> fixed
                parsed -> compiled.read(sender, context)
                else -> definition.defaultValue?.invoke(sender)
            }

            previous += ParsedArgument(
                name = name,
                value = value,
                raw = when {
                    fixed != null -> fixed
                    parsed -> raw
                    else -> null
                },
                present = fixed != null || parsed,
            )
        }

        return SuggestionInfo(
            sender = sender,
            previousArgs = CommandArguments.of(previous),
            currentInput = CompletionCapture.logicalInput(context.input),
            currentArg = current,
        )
    }

    private fun argumentStart(
        context: CommandContext,
        preceding: List<ArgumentDefinition<*>>,
        precedingCount: Int,
        fixedByName: Map<String, String>,
    ): Int {
        val input = CompletionCapture.logicalInput(context.input)
        var cursor = context.commandName.length

        for (index in 0 until precedingCount) {
            val definition = preceding[index]

            while (cursor < input.length && input[cursor].isWhitespace()) {
                cursor++
            }

            val raw = context.getRaw(definition.nodeName)
                ?: fixedByName[definition.nodeName]

            if (raw != null) {
                cursor = (cursor + raw.length).coerceAtMost(input.length)
            }
        }

        while (cursor < input.length && input[cursor].isWhitespace()) {
            cursor++
        }

        return cursor
    }

    private suspend fun resolveEntries(
        definition: ArgumentDefinition<*>,
        builtIns: List<StringTooltip>,
        mode: SuggestionMode<*>,
        info: SuggestionInfo,
        current: String,
    ): List<StringTooltip> {
        val includeBuiltIns = when (mode) {
            SuggestionMode.BuiltIns,
            is SuggestionMode.Include<*>,
            is SuggestionMode.IncludeSafe<*>,
                -> true

            is SuggestionMode.Replace<*>,
            is SuggestionMode.ReplaceSafe<*>,
                -> false
        }

        val providerEntries = when (mode) {
            SuggestionMode.BuiltIns -> emptyList()

            is SuggestionMode.Include<*> ->
                filter(
                    entries = ObjectArrayList(mode.provider.suggest(info)),
                    current = current,
                    policy = mode.provider.filterPolicy,
                )

            is SuggestionMode.Replace<*> ->
                filter(
                    entries = ObjectArrayList(mode.provider.suggest(info)),
                    current = current,
                    policy = mode.provider.filterPolicy,
                )

            is SuggestionMode.IncludeSafe<*> ->
                filter(
                    entries = resolveSafe(definition, mode.provider, info),
                    current = current,
                    policy = SuggestionFilter.PREFIX,
                )

            is SuggestionMode.ReplaceSafe<*> ->
                filter(
                    entries = resolveSafe(definition, mode.provider, info),
                    current = current,
                    policy = SuggestionFilter.PREFIX,
                )
        }

        val uniqueProviderEntries = Object2ObjectOpenHashMap<String, StringTooltip>(
            providerEntries.size,
        )

        val providerOrder = ObjectArrayList<String>(providerEntries.size)

        providerEntries.forEach { entry ->
            if (!uniqueProviderEntries.containsKey(entry.suggestion)) {
                providerOrder += entry.suggestion
                uniqueProviderEntries[entry.suggestion] = entry
            }
        }

        if (!includeBuiltIns) {
            val result = ObjectArrayList<StringTooltip>(providerOrder.size)

            providerOrder.forEach { suggestion ->
                result += checkNotNull(uniqueProviderEntries[suggestion])
            }

            return result
        }

        val entriesByText = Object2ObjectOpenHashMap<String, StringTooltip>(
            builtIns.size + providerEntries.size,
        )

        val order = ObjectArrayList<String>(
            builtIns.size + providerEntries.size,
        )

        filter(
            entries = builtIns,
            current = current,
            policy = SuggestionFilter.PREFIX,
        ).forEach { entry ->
            if (!entriesByText.containsKey(entry.suggestion)) {
                order += entry.suggestion
                entriesByText[entry.suggestion] = entry
            }
        }

        providerOrder.forEach { suggestion ->
            val entry = checkNotNull(uniqueProviderEntries[suggestion])

            if (!entriesByText.containsKey(suggestion)) {
                order += suggestion
            }

            entriesByText[suggestion] = entry
        }

        val result = ObjectArrayList<StringTooltip>(order.size)

        order.forEach { suggestion ->
            result += checkNotNull(entriesByText[suggestion])
        }

        return result
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun resolveSafe(
        definition: ArgumentDefinition<*>,
        provider: SafeSuggestions<*>,
        info: SuggestionInfo,
    ): List<StringTooltip> {
        val typedProvider = provider as SafeSuggestions<Any?>
        val stringify = definition.stringify as (Any?) -> String

        val suggestions = typedProvider.suggest(info)
        val result = ObjectArrayList<StringTooltip>()

        suggestions.forEach { entry ->
            result += StringTooltip(
                suggestion = stringify(entry.suggestion),
                tooltip = entry.tooltip,
            )
        }

        return result
    }

    private fun filter(
        entries: List<StringTooltip>,
        current: String,
        policy: SuggestionFilter,
    ): List<StringTooltip> {
        if (policy == SuggestionFilter.NONE) {
            return ObjectArrayList(entries)
        }

        val result = ObjectArrayList<StringTooltip>(entries.size)

        entries.forEach { entry ->
            if (
                entry.suggestion.startsWith(
                    current,
                    ignoreCase = true,
                )
            ) {
                result += entry
            }
        }

        return result
    }

    private fun hasServerBuiltIns(definition: ArgumentDefinition<*>): Boolean =
        when (definition.kind) {
            ArgumentKind.Player,
            is ArgumentKind.Players,
            ArgumentKind.Entity,
            is ArgumentKind.Entities,
            ArgumentKind.Sound,
            ArgumentKind.PotionEffect,
            ArgumentKind.Biome,
            ArgumentKind.Enchantment,
            is ArgumentKind.Resource<*>,
            ArgumentKind.Instance,
                -> true

            else -> false
        }

    private fun builtInEntries(definition: ArgumentDefinition<*>): List<StringTooltip> {
        val values = ObjectArrayList<String>()

        when (val kind = definition.kind) {
            ArgumentKind.Boolean -> {
                values += "true"
                values += "false"
            }

            is ArgumentKind.Literal -> {
                values += kind.literal
            }

            is ArgumentKind.MultiLiteral -> {
                values.addAll(kind.literals)
            }

            is ArgumentKind.Enum<*> -> {
                values.ensureCapacity(kind.values.size)

                kind.values.forEach { value ->
                    values += stringify(definition, value)
                }
            }

            ArgumentKind.GameMode -> {
                values.ensureCapacity(GameMode.entries.size)

                GameMode.entries.forEach { value ->
                    values += stringify(definition, value)
                }
            }

            ArgumentKind.Player -> values.addAll(
                entityBuiltIns(
                    playersOnly = true,
                    single = true,
                ),
            )

            is ArgumentKind.Players -> values.addAll(
                entityBuiltIns(
                    playersOnly = true,
                    single = false,
                ),
            )

            ArgumentKind.Entity -> values.addAll(
                entityBuiltIns(
                    playersOnly = false,
                    single = true,
                ),
            )

            is ArgumentKind.Entities -> values.addAll(
                entityBuiltIns(
                    playersOnly = false,
                    single = false,
                ),
            )

            ArgumentKind.Sound -> {
                SoundEvent.values().forEach { value ->
                    values += value.name()
                }
            }

            ArgumentKind.PotionEffect -> {
                PotionEffect.values().forEach { value ->
                    values += value.key().asString()
                }
            }

            ArgumentKind.Biome -> {
                MinecraftServer
                    .getBiomeRegistry()
                    .keys()
                    .forEach { key ->
                        values += key.name()
                    }
            }

            ArgumentKind.Enchantment -> {
                MinecraftServer
                    .getEnchantmentRegistry()
                    .keys()
                    .forEach { key ->
                        values += key.name()
                    }
            }

            is ArgumentKind.Resource<*> -> {
                kind.registry.keys().forEach { key ->
                    values += key.name()
                }
            }

            ArgumentKind.Instance -> {
                values.addAll(instanceBuiltIns())
            }

            else -> Unit
        }

        val result = ObjectArrayList<StringTooltip>(values.size)

        values.forEach { value ->
            result += StringTooltip(value, null)
        }

        return result
    }

    private fun instanceBuiltIns(): List<String> {
        val instances = MinecraftServer.getInstanceManager().instances
        val dimensionNameCounts = Object2IntOpenHashMap<String>()

        instances.forEach { instance ->
            dimensionNameCounts.addTo(
                instance.dimensionName,
                1,
            )
        }

        val result = ObjectArrayList<String>(instances.size * 2)

        instances.forEach { instance ->
            result += instance.uuid.toString()
        }

        instances.forEach { instance ->
            if (dimensionNameCounts.getInt(instance.dimensionName) == 1) {
                result += instance.dimensionName
            }
        }

        return result
    }

    private fun entityBuiltIns(playersOnly: Boolean, single: Boolean): List<String> = buildList {
        val result = ObjectArrayList<String>()

        if (playersOnly) {
            val playerNames = ObjectArrayList<String>(
                MinecraftServer
                    .getConnectionManager()
                    .onlinePlayers
                    .size,
            )

            MinecraftServer
                .getConnectionManager()
                .onlinePlayers
                .forEach { player ->
                    playerNames += player.username
                }

            playerNames.sortWith(String.CASE_INSENSITIVE_ORDER)
            result.addAll(playerNames)
        }

        val selectors = if (playersOnly) {
            PLAYER_SELECTORS
        } else {
            ENTITY_SELECTORS
        }

        selectors.forEach { selector ->
            if (!single || selector in SINGLE_SELECTORS) {
                result += selector
            }
        }

        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun stringify(definition: ArgumentDefinition<*>, value: Any?): String =
        (definition.stringify as (Any?) -> String)(value)

    private fun providerDescription(mode: SuggestionMode<*>): String = when (mode) {
        SuggestionMode.BuiltIns -> "built-in suggestions"
        is SuggestionMode.Include<*> -> mode.provider.javaClass.name
        is SuggestionMode.Replace<*> -> mode.provider.javaClass.name
        is SuggestionMode.IncludeSafe<*> -> mode.provider.javaClass.name
        is SuggestionMode.ReplaceSafe<*> -> mode.provider.javaClass.name
    }

    private companion object {
        val SINGLE_SELECTORS: ObjectSet<String> = ObjectSet.of(
            "@p",
            "@r",
            "@s",
            "@n",
        )

        val PLAYER_SELECTORS: ObjectSet<String> = ObjectSet.of(
            "@p",
            "@r",
            "@a",
            "@s",
        )

        val ENTITY_SELECTORS: ObjectSet<String> = ObjectSet.of(
            "@p",
            "@r",
            "@a",
            "@e",
            "@s",
            "@n",
        )
    }
}
