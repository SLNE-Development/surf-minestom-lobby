package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.commandapi.dsl.*
import dev.slne.minestom.lobby.api.command.commandapi.suggestion.SuggestionInfo
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import dev.slne.minestom.lobby.server.upload.UploadHandler
import dev.slne.minestom.lobby.server.upload.UploadService
import dev.slne.surf.api.core.messages.adventure.sendText
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.CommandSender

private const val KIND_NODE = "kind"
private const val KEY_NODE = "key"

fun uploadsCommand(uploads: UploadService) = commandTree("uploads") {
    withPermission(LobbyPermissions.UPLOADS_COMMAND)

    val kindSuggestions: (SuggestionInfo) -> Collection<String> = { uploads.directoryNames }

    anyExecutorSuspend { sender, _ ->
        sender.sendOverview(uploads)
    }

    literalArgument("list") {
        anyExecutorSuspend { sender, _ ->
            sender.sendOverview(uploads)
        }

        stringArgument(KIND_NODE) {
            replaceSuggestions(kindSuggestions)

            anyExecutorSuspend { sender, args ->
                val kind = args.get<String>(KIND_NODE)
                val handler = uploads.handler(kind)

                if (handler == null) {
                    sender.sendUnknownKind(uploads, kind)
                    return@anyExecutorSuspend
                }

                sender.sendEntries(kind, handler)
            }
        }
    }

    literalArgument("delete") {
        stringArgument(KIND_NODE) {
            replaceSuggestions(kindSuggestions)

            stringArgument(KEY_NODE) {
                replaceSuggestionsAsync { info ->
                    val kind = info.previousArgs.getOptional<String>(KIND_NODE)
                        ?: return@replaceSuggestionsAsync emptyList()

                    uploads.handler(kind)
                        ?.list()
                        ?.map { it.key }
                        .orEmpty()
                }

                anyExecutorSuspend { sender, args ->
                    val kind = args.get<String>(KIND_NODE)
                    val key = args.get<String>(KEY_NODE)
                    val handler = uploads.handler(kind)

                    if (handler == null) {
                        sender.sendUnknownKind(uploads, kind)
                        return@anyExecutorSuspend
                    }

                    val deleted = handler.deleteOrReport(sender, key)
                        ?: return@anyExecutorSuspend

                    if (deleted) {
                        sender.sendText {
                            appendSuccessPrefix()
                            success("Der Eintrag ")
                            variableValue(key)
                            success(" wurde aus ")
                            variableValue(kind)
                            success(" gelöscht.")
                        }
                    } else {
                        sender.sendText {
                            appendInfoPrefix()
                            info("Der Eintrag ")
                            variableValue(key)
                            info(" existierte nicht in ")
                            variableValue(kind)
                            info(".")
                        }
                    }
                }
            }
        }
    }
}

private suspend fun CommandSender.sendOverview(uploads: UploadService) {
    sendText {
        spacer("Uploads:")
    }

    for (name in uploads.directoryNames) {
        val entries = uploads.handler(name)?.list().orEmpty()

        sendText {
            darkSpacer(" - ")
            variableValue(name)
            spacer(" (")
            variableValue(entries.size)
            spacer(" Einträge)")
            clickRunsCommand("/uploads list $name")
        }
    }
}

private suspend fun CommandSender.sendEntries(kind: String, handler: UploadHandler) {
    val entries = handler.list()

    sendText {
        variableValue(kind)
        spacer(" enthält ")
        variableValue(entries.size)
        spacer(" Einträge:")
    }

    for ((key, detail) in entries) {
        val line = text()
            .append(text(" - ", NamedTextColor.DARK_GRAY))
            .append(text(key, NamedTextColor.GOLD))

        detail?.let { detail ->
            line.append(text(" ($detail)", NamedTextColor.GRAY))
        }

        sendMessage(line)
    }
}

private fun CommandSender.sendUnknownKind(uploads: UploadService, kind: String) {
    sendText {
        appendErrorPrefix()
        error("Es gibt keine Upload-Art ")
        variableValue(kind)
        error(". Verfügbar: ")
        variableValue(uploads.directoryNames.joinToString(", "))
    }
}

/**
 * Deletes [key] and returns whether it existed, or `null` after reporting a handler that refused
 * the deletion.
 */
private suspend fun UploadHandler.deleteOrReport(sender: CommandSender, key: String): Boolean? =
    runCatching { delete(key) }.getOrElse { failure ->
        if (failure !is IllegalStateException && failure !is IllegalArgumentException) {
            throw failure
        }

        sender.sendText {
            appendErrorPrefix()
            error("Der Eintrag ")
            variableValue(key)
            error(" konnte nicht gelöscht werden: ")
            error(failure.message ?: "Unbekannter Fehler")
        }
        null
    }
