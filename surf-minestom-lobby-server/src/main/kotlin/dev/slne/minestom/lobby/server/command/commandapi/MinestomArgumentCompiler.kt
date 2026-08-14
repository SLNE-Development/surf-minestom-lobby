package dev.slne.minestom.lobby.server.command.commandapi

import dev.slne.minestom.lobby.api.command.commandapi.CommandAPI
import dev.slne.minestom.lobby.api.command.commandapi.argument.*
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.command.ArgumentParserType
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.arguments.ArgumentEnum
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.arguments.ArgumentWord
import net.minestom.server.command.builder.exception.ArgumentSyntaxException
import net.minestom.server.entity.Entity
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.potion.PotionEffect
import net.minestom.server.registry.BuiltinRegistries
import net.minestom.server.registry.Registry
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.entity.EntityFinder
import net.minestom.server.utils.location.RelativeVec
import java.io.Serial
import java.util.*

private const val UUID_WITHOUT_INSTANCE_MESSAGE =
    "The instance should not be null when searching by UUID"

internal data class CompiledArgument<T>(
    val definition: ArgumentDefinition<*>,
    val native: Argument<T>,
    private val converter: (CommandSender, T) -> Any?,
) {
    fun read(sender: CommandSender, context: CommandContext): Any? =
        converter(sender, context.get(native))

    fun read(sender: CommandSender, input: String): Any? =
        converter(sender, native.parse(sender, input))

    @Suppress("UNCHECKED_CAST")
    fun convertNative(sender: CommandSender, value: Any?): Any? =
        (converter as (CommandSender, Any?) -> Any?)(sender, value)
}

internal class ComponentArgumentSyntaxException(
    val component: Component,
    message: String,
    input: String,
    errorCode: Int,
) : ArgumentSyntaxException(message, input, errorCode) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 3797775872812258704L
    }
}

internal class MinestomArgumentCompiler {
    fun compile(definition: ArgumentDefinition<*>): CompiledArgument<*> =
        when (val kind = definition.kind) {
            ArgumentKind.Boolean -> identity(definition, ArgumentType.Boolean(definition.nodeName))
            is ArgumentKind.Integer -> identity(
                definition,
                ArgumentType.Integer(definition.nodeName).apply {
                    min(kind.min)
                    max(kind.max)
                },
            )

            is ArgumentKind.Long -> identity(
                definition,
                ArgumentType.Long(definition.nodeName).apply {
                    min(kind.min)
                    max(kind.max)
                },
            )

            is ArgumentKind.Float -> identity(
                definition,
                ArgumentType.Float(definition.nodeName).apply {
                    min(kind.min)
                    max(kind.max)
                },
            )

            is ArgumentKind.Double -> identity(
                definition,
                ArgumentType.Double(definition.nodeName).apply {
                    min(kind.min)
                    max(kind.max)
                },
            )

            ArgumentKind.Word -> identity(definition, ArgumentType.Word(definition.nodeName))
            ArgumentKind.Text -> identity(definition, ArgumentType.String(definition.nodeName))
            ArgumentKind.GreedyString -> converting(
                definition,
                ArgumentType.StringArray(definition.nodeName),
            ) { words -> words.joinToString(" ") }

            is ArgumentKind.Literal -> identity(definition, ArgumentType.Literal(kind.literal))
            is ArgumentKind.MultiLiteral -> identity(
                definition,
                ArgumentType.Word(definition.nodeName).from(*kind.literals.toTypedArray()),
            )

            is ArgumentKind.Enum<*> -> compileEnum(definition, kind, allowNativeEnum = true)
            ArgumentKind.Uuid -> identity(definition, ArgumentType.UUID(definition.nodeName))
            ArgumentKind.IntegerRange -> identity(
                definition,
                ArgumentType.IntRange(definition.nodeName)
            )

            ArgumentKind.FloatRange -> identity(
                definition,
                ArgumentType.FloatRange(definition.nodeName)
            )

            ArgumentKind.Command -> converting(
                definition,
                ArgumentType.Command(definition.nodeName),
            ) { result -> result.input }

            ArgumentKind.Player -> compileEntitySelector(
                definition,
                single = true,
                playersOnly = true,
                allowEmpty = false,
            )

            is ArgumentKind.Players -> compileEntitySelector(
                definition,
                single = false,
                playersOnly = true,
                allowEmpty = kind.allowEmpty,
            )

            ArgumentKind.Entity -> compileEntitySelector(
                definition,
                single = true,
                playersOnly = false,
                allowEmpty = false,
            )

            is ArgumentKind.Entities -> compileEntitySelector(
                definition,
                single = false,
                playersOnly = false,
                allowEmpty = kind.allowEmpty,
            )

            ArgumentKind.EntityType -> identity(
                definition,
                ArgumentType.EntityType(definition.nodeName)
            )

            ArgumentKind.Position -> compilePosition(
                definition,
                block = false,
                twoDimensional = false
            )

            ArgumentKind.Position2D -> compilePosition(
                definition,
                block = false,
                twoDimensional = true
            )

            ArgumentKind.BlockPosition -> compilePosition(
                definition,
                block = true,
                twoDimensional = false
            )

            ArgumentKind.Rotation -> compileRotation(definition)
            ArgumentKind.Angle -> compileAngle(definition)
            ArgumentKind.Axis -> compileAxis(definition)

            ArgumentKind.BlockState -> identity(
                definition,
                ArgumentType.BlockState(definition.nodeName)
            )

            ArgumentKind.ItemStack -> identity(
                definition,
                ArgumentType.ItemStack(definition.nodeName)
            )

            ArgumentKind.Component -> identity(
                definition,
                ArgumentType.Component(definition.nodeName)
            )

            ArgumentKind.Nbt -> identity(definition, ArgumentType.NBT(definition.nodeName))
            ArgumentKind.NbtCompound -> identity(
                definition,
                ArgumentType.NbtCompound(definition.nodeName)
            )

            ArgumentKind.ResourceLocation -> identity(
                definition,
                ArgumentType.ResourceLocation(definition.nodeName),
            )

            ArgumentKind.Time -> identity(definition, ArgumentType.Time(definition.nodeName))
            ArgumentKind.TeamColor -> identity(
                definition,
                ArgumentType.TeamColor(definition.nodeName)
            )

            ArgumentKind.Particle -> identity(
                definition,
                ArgumentType.Particle(definition.nodeName)
            )

            ArgumentKind.GameMode -> compileGameMode(definition)

            ArgumentKind.Sound -> compileSound(definition)
            ArgumentKind.PotionEffect -> compilePotionEffect(definition)
            ArgumentKind.Biome -> compileBiome(definition)
            ArgumentKind.Enchantment -> compileEnchantment(definition)
            is ArgumentKind.Resource<*> -> compileResource(definition, kind)
            ArgumentKind.Instance -> compileInstance(definition)

            is ArgumentKind.Custom<*, *> -> compileCustom(definition, kind)
            is ArgumentKind.List<*> -> compileList(definition, kind)
        }

    private fun compilePosition(
        definition: ArgumentDefinition<*>,
        block: Boolean,
        twoDimensional: Boolean,
    ): CompiledArgument<RelativeVec> {
        val native: Argument<RelativeVec> = when {
            block -> ArgumentType.RelativeBlockPosition(definition.nodeName)
            twoDimensional -> ArgumentType.RelativeVec2(definition.nodeName)
            else -> ArgumentType.RelativeVec3(definition.nodeName)
        }
        return convertingWithSender(definition, native) { sender, relative ->
            relative.fromSender(sender)
        }
    }

    private fun compileRotation(definition: ArgumentDefinition<*>): CompiledArgument<*> {
        val base = ArgumentType.RelativeVec2(definition.nodeName)
        val native = MinestomDelegatingArgument(
            id = definition.nodeName,
            base = base,
            parserOverride = ArgumentParserType.ROTATION,
            nodePropertiesOverride = { null },
            convert = { info ->
                val view = info.baseValue.fromView((info.sender as? Entity)?.position)
                Rotation(view.x().toFloat(), view.z().toFloat())
            },
        )
        return identity(definition, native)
    }

    private fun compileAngle(definition: ArgumentDefinition<*>): CompiledArgument<*> {
        val native = MinestomDelegatingArgument(
            id = definition.nodeName,
            base = ArgumentType.Word(definition.nodeName),
            parserOverride = ArgumentParserType.ANGLE,
            nodePropertiesOverride = { null },
            convert = { info -> parseAngle(info.baseValue, info.sender) },
        )
        return identity(definition, native)
    }

    private fun compileAxis(definition: ArgumentDefinition<*>): CompiledArgument<*> {
        val native = MinestomDelegatingArgument(
            id = definition.nodeName,
            base = ArgumentType.Word(definition.nodeName),
            parserOverride = ArgumentParserType.SWIZZLE,
            nodePropertiesOverride = { null },
            convert = { info -> parseAxes(info.baseValue) },
        )
        return identity(definition, native)
    }

    private fun compileGameMode(definition: ArgumentDefinition<*>): CompiledArgument<*> {
        val native = MinestomDelegatingArgument(
            id = definition.nodeName,
            base = ArgumentType.Word(definition.nodeName),
            parserOverride = ArgumentParserType.GAMEMODE,
            nodePropertiesOverride = { null },
            convert = { info -> parseGameMode(info.baseValue) },
        )
        return identity(definition, native)
    }

    /**
     * Parses vanilla's angle syntax: an absolute float, or `~` optionally followed by a float
     * offset applied to the sender's current yaw. The result is normalized into `[-180, 180)`,
     * matching vanilla's displayed angle range.
     */
    private fun parseAngle(input: String, sender: CommandSender): Float {
        val relative = input.startsWith("~")
        val offsetText = if (relative) input.substring(1) else input
        val magnitude = if (relative && offsetText.isEmpty()) {
            0f
        } else {
            offsetText.toFloatOrNull()?.takeIf(Float::isFinite)
                ?: CommandAPI.failWithString("Invalid angle '$input'")
        }
        val raw = if (relative) {
            magnitude + ((sender as? Entity)?.position?.yaw ?: 0f)
        } else {
            magnitude
        }
        return ((raw % 360f) + 540f) % 360f - 180f
    }

    private fun parseAxes(input: String): Set<Axis> {
        val axes = input.map { char ->
            Axis.entries.singleOrNull { axis -> axis.name.single().equals(char, true) }
                ?: CommandAPI.failWithString("Unknown axis '$char'")
        }
        if (axes.distinct().size != axes.size) CommandAPI.failWithString("Axes cannot be repeated")
        return axes.toSet()
    }

    private fun parseGameMode(input: String): GameMode =
        GameMode.entries.firstOrNull { mode -> mode.name.equals(input, ignoreCase = true) }
            ?: CommandAPI.failWithString("Unknown game mode '$input'")

    private fun compileSound(definition: ArgumentDefinition<*>): CompiledArgument<Key> =
        converting(definition, ArgumentType.ResourceLocation(definition.nodeName)) { key ->
            SoundEvent.fromKey(key)
                ?: CommandAPI.failWithString("Unknown sound '${key.asString()}'")
        }

    private fun compilePotionEffect(definition: ArgumentDefinition<*>): CompiledArgument<Key> =
        converting(definition, ArgumentType.ResourceLocation(definition.nodeName)) { key ->
            PotionEffect.fromKey(key)
                ?: CommandAPI.failWithString("Unknown potion effect '${key.asString()}'")
        }

    private fun compileBiome(definition: ArgumentDefinition<*>): CompiledArgument<String> =
        resourceArgument(definition, BuiltinRegistries.BIOME.name()) {
            MinecraftServer.process().registries().biome()
        }

    private fun compileEnchantment(definition: ArgumentDefinition<*>): CompiledArgument<String> =
        resourceArgument(definition, BuiltinRegistries.ENCHANTMENT.name()) {
            MinecraftServer.process().registries().enchantment()
        }

    private fun compileResource(
        definition: ArgumentDefinition<*>,
        kind: ArgumentKind.Resource<*>,
    ): CompiledArgument<String> = resourceArgument(definition, kind.identifier) { kind.registry }

    private fun resourceArgument(
        definition: ArgumentDefinition<*>,
        identifier: String,
        registry: () -> Registry<*>,
    ): CompiledArgument<String> = converting(
        definition,
        ArgumentType.Resource(definition.nodeName, identifier),
    ) { input ->
        val key = if (Key.parseable(input)) Key.key(input) else null
        key?.let { registry().getKey(it) }
            ?: CommandAPI.failWithString("Unknown $identifier resource '$input'")
    }

    private fun compileInstance(definition: ArgumentDefinition<*>): CompiledArgument<String> =
        converting(definition, ArgumentType.Word(definition.nodeName)) { input ->
            resolveInstance(input)
        }

    private fun resolveInstance(input: String): Instance {
        val instances = MinecraftServer.getInstanceManager().instances
        runCatching { UUID.fromString(input) }.getOrNull()?.let { uuid ->
            instances.singleOrNull { instance -> instance.uuid == uuid }?.let { return it }
        }
        val byDimension = instances.filter { instance -> instance.dimensionName == input }
        return byDimension.singleOrNull()
            ?: CommandAPI.failWithString("Unknown or ambiguous instance '$input'; use its UUID")
    }

    @Suppress("UNCHECKED_CAST")
    private fun compileCustom(
        definition: ArgumentDefinition<*>,
        kind: ArgumentKind.Custom<*, *>,
    ): CompiledArgument<*> {
        val base = compileDelegatedBase(kind.base)
        val nativeBase = base.native as Argument<Any?>
        val parser = kind.parser as (CustomArgumentInfo<Any?>) -> Any?
        val native = MinestomDelegatingArgument(
            id = definition.nodeName,
            base = nativeBase,
            convert = { info ->
                parser(
                    CustomArgumentInfo(
                        sender = info.sender,
                        currentInput = info.currentInput,
                        baseValue = base.convertNative(info.sender, info.baseValue),
                    ),
                )
            },
        )
        applyCompositeDefault(definition, native)
        return identity(definition, native)
    }

    private fun compileDelegatedBase(definition: ArgumentDefinition<*>): CompiledArgument<*> {
        val compiled = when (val kind = definition.kind) {
            is ArgumentKind.Enum<*> -> compileEnum(definition, kind, allowNativeEnum = false)
            else -> compile(definition)
        }
        requireNotNull(compiled.native.parser()) {
            "Argument '${definition.nodeName}' cannot be used as a custom or mapped base"
        }
        return compiled
    }

    private fun compileList(
        definition: ArgumentDefinition<*>,
        kind: ArgumentKind.List<*>,
    ): CompiledArgument<*> {
        val element = compile(kind.element)
        require(!element.native.useRemaining()) { "List elements cannot consume remaining input" }
        val native = MinestomDelegatingArgument(
            id = definition.nodeName,
            base = ArgumentType.StringArray(definition.nodeName),
            convert = { info ->
                parseList(
                    sender = info.sender,
                    input = info.currentInput,
                    delimiter = kind.delimiter,
                    allowEmpty = kind.allowEmpty,
                    element = element,
                )
            },
        )
        applyCompositeDefault(definition, native)
        return identity(definition, native)
    }

    private fun <T> applyCompositeDefault(
        definition: ArgumentDefinition<*>,
        native: Argument<T>,
    ) {
        definition.defaultValue?.let { defaultValue ->
            @Suppress("UNCHECKED_CAST")
            native.setDefaultValue { sender -> defaultValue(sender) as T }
        }
    }

    private fun parseList(
        sender: CommandSender,
        input: String,
        delimiter: Char,
        allowEmpty: Boolean,
        element: CompiledArgument<*>,
    ): List<Any?> = input.split(delimiter).map { part ->
        val candidate = part.trim()
        if (!allowEmpty && candidate.isEmpty()) {
            throw ArgumentSyntaxException(
                "List elements cannot be empty",
                input,
                EMPTY_LIST_ELEMENT,
            )
        }
        element.read(sender, candidate)
    }

    private fun <E : Enum<E>> compileEnum(
        definition: ArgumentDefinition<*>,
        kind: ArgumentKind.Enum<E>,
        allowNativeEnum: Boolean,
    ): CompiledArgument<*> {
        val enumClass = kind.values.first().declaringJavaClass
        val formatted = kind.values.map(kind.formatter)
        val constants = EnumSet.allOf(enumClass)
        val nativeFormat = when {
            kind.values != constants -> null
            formatted == constants.map { it.name } -> ArgumentEnum.Format.DEFAULT
            formatted == constants.map { it.name.lowercase() } -> ArgumentEnum.Format.LOWER_CASED
            formatted == constants.map { it.name.uppercase() } -> ArgumentEnum.Format.UPPER_CASED
            else -> null
        }

        if (
            allowNativeEnum &&
            nativeFormat != null &&
            definition.suggestions == SuggestionMode.BuiltIns
        ) {
            return identity(
                definition,
                ArgumentType.Enum(definition.nodeName, enumClass).setFormat(nativeFormat),
            )
        }

        val valuesByInput = formatted.zip(kind.values).toMap()
        return converting(
            definition,
            ArgumentType.Word(definition.nodeName).from(*formatted.toTypedArray()),
        ) { input -> valuesByInput.getValue(input) }
    }

    private fun <T> identity(
        definition: ArgumentDefinition<*>,
        native: Argument<T>,
    ): CompiledArgument<T> = converting(definition, native) { value -> value }

    private fun <T> converting(
        definition: ArgumentDefinition<*>,
        native: Argument<T>,
        converter: (T) -> Any?,
    ): CompiledArgument<T> = CompiledArgument(
        definition,
        finish(definition, native),
    ) { _, value -> converter(value) }

    private fun <T> convertingWithSender(
        definition: ArgumentDefinition<*>,
        native: Argument<T>,
        converter: (CommandSender, T) -> Any?,
    ): CompiledArgument<T> = CompiledArgument(definition, finish(definition, native), converter)

    private fun compileEntitySelector(
        definition: ArgumentDefinition<*>,
        single: Boolean,
        playersOnly: Boolean,
        allowEmpty: Boolean,
    ): CompiledArgument<EntityFinder> {
        val native = ArgumentType.Entity(definition.nodeName)
            .singleEntity(single)
            .onlyPlayers(playersOnly)
        return convertingWithSender(definition, native) { sender, finder ->
            val found = try {
                finder.find(sender)
            } catch (failure: NullPointerException) {
                if (failure.message == UUID_WITHOUT_INSTANCE_MESSAGE) {
                    CommandAPI.failWithString("UUID targets require an instanced player sender")
                }
                throw failure
            }
            val selected: List<Entity> = found.let { entities ->
                if (playersOnly) entities.filterIsInstance<Player>() else entities
            }
            when {
                single && selected.size != 1 -> {
                    CommandAPI.failWithString("Expected exactly one target")
                }

                !single && !allowEmpty && selected.isEmpty() -> {
                    CommandAPI.failWithString("No targets matched")
                }

                single -> selected.first()
                else -> Collections.unmodifiableList(ArrayList(selected))
            }
        }
    }

    private fun <T> finish(
        definition: ArgumentDefinition<*>,
        native: Argument<T>,
    ): Argument<T> {
        val delegate: Argument<T> = native
        native.setCallback { sender, failure ->
            val message = (failure as? ComponentArgumentSyntaxException)?.component
                ?: Component.text("Invalid ${definition.nodeName}: ${failure.input}")
            sender.sendMessage(message)
        }
        return if (
            definition.suggestions != SuggestionMode.BuiltIns &&
            (native is ArgumentEnum<*> || native is ArgumentWord && native.hasRestrictions())
        ) {
            MinestomDelegatingArgument(
                id = delegate.id,
                base = delegate,
                convert = { info -> info.baseValue },
            )
        } else {
            native
        }
    }

    private companion object {
        const val EMPTY_LIST_ELEMENT = 1_221
    }
}
