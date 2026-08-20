package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutorSuspend
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.literalArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.replaceSuggestions
import dev.slne.minestom.lobby.api.command.commandapi.dsl.replaceSuggestionsAsync
import dev.slne.minestom.lobby.api.command.commandapi.dsl.stringArgument
import dev.slne.minestom.lobby.api.command.commandapi.suggestion.SuggestionInfo
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import dev.slne.minestom.lobby.server.upload.UploadHandler
import dev.slne.minestom.lobby.server.upload.UploadService
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.event.ClickEvent
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
                        sender.sendMessage(
                            text()
                                .append(text("Der Eintrag ", NamedTextColor.GRAY))
                                .append(text(key, NamedTextColor.GOLD))
                                .append(text(" wurde aus ", NamedTextColor.GRAY))
                                .append(text(kind, NamedTextColor.GOLD))
                                .append(text(" gelöscht.", NamedTextColor.GRAY))
                        )
                    } else {
                        sender.sendMessage(
                            text()
                                .append(text("In ", NamedTextColor.GRAY))
                                .append(text(kind, NamedTextColor.GOLD))
                                .append(text(" ist kein Eintrag ", NamedTextColor.GRAY))
                                .append(text(key, NamedTextColor.GOLD))
                                .append(text(" gespeichert.", NamedTextColor.GRAY))
                        )
                    }
                }
            }
        }
    }
}

private suspend fun CommandSender.sendOverview(uploads: UploadService) {
    sendMessage(text("Uploads:", NamedTextColor.GRAY))

    for (name in uploads.directoryNames) {
        val entries = uploads.handler(name)?.list().orEmpty()

        sendMessage(
            text()
                .append(text(" - ", NamedTextColor.DARK_GRAY))
                .append(text(name, NamedTextColor.GOLD))
                .append(text(" (", NamedTextColor.GRAY))
                .append(text(entries.size, NamedTextColor.GOLD))
                .append(text(" Einträge)", NamedTextColor.GRAY))
                .clickEvent(ClickEvent.runCommand("/uploads list $name"))
        )
    }
}

private suspend fun CommandSender.sendEntries(kind: String, handler: UploadHandler) {
    val entries = handler.list()

    sendMessage(
        text()
            .append(text(kind, NamedTextColor.GOLD))
            .append(text(" enthält ", NamedTextColor.GRAY))
            .append(text(entries.size, NamedTextColor.GOLD))
            .append(text(" Einträge:", NamedTextColor.GRAY))
    )

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
    sendMessage(
        text()
            .append(text("Es gibt keine Upload-Art ", NamedTextColor.RED))
            .append(text(kind, NamedTextColor.GOLD))
            .append(text(". Verfügbar: ", NamedTextColor.RED))
            .append(text(uploads.directoryNames.joinToString(", "), NamedTextColor.GOLD))
    )
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

        sender.sendMessage(
            text(
                failure.message ?: "Der Eintrag konnte nicht gelöscht werden.",
                NamedTextColor.RED
            )
        )

        null
    }
