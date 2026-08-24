package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutorSuspend
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandAPICommand
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import dev.slne.minestom.lobby.server.version.LOBBY_DOWNLOAD_URL
import dev.slne.minestom.lobby.server.version.LobbyBuildInfo
import dev.slne.minestom.lobby.server.version.LobbyVersionService
import dev.slne.minestom.lobby.server.version.LobbyVersionStatus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender

fun versionCommand(versionService: LobbyVersionService) = commandAPICommand("version") {
    withAliases("ver", "about")
    withPermission(LobbyPermissions.VERSION_COMMAND)

    anyExecutorSuspend { sender, _ ->
        sender.sendBuild(versionService.buildInfo)
        sender.sendStatus(versionService.status())
    }
}

private fun CommandSender.sendBuild(buildInfo: LobbyBuildInfo) {
    val version = text()
        .append(text("Dieser Server läuft Surf Minestom Lobby ", NamedTextColor.GRAY))
        .append(text(buildInfo.displayVersion, NamedTextColor.GOLD))

    buildInfo.commit?.let { commit ->
        version
            .hoverEvent(text("Commit: $commit", NamedTextColor.GRAY))
            .clickEvent(ClickEvent.copyToClipboard(commit))
    }

    sendMessage(version)

    buildInfo.commitTime?.let { commitTime ->
        sendMessage(
            text()
                .append(text("Commit vom ", NamedTextColor.GRAY))
                .append(text(commitTime.toString(), NamedTextColor.GOLD))
        )
    }

    sendMessage(
        text()
            .append(text("Minecraft ", NamedTextColor.GRAY))
            .append(text(MinecraftServer.VERSION_NAME, NamedTextColor.GOLD))
            .append(text(" (Protokoll ", NamedTextColor.GRAY))
            .append(text(MinecraftServer.PROTOCOL_VERSION, NamedTextColor.GOLD))
            .append(text(")", NamedTextColor.GRAY))
    )
}

private fun CommandSender.sendStatus(status: LobbyVersionStatus) = when (status) {
    LobbyVersionStatus.UpToDate -> sendMessage(
        text("Der Server läuft auf dem neuesten Build.", NamedTextColor.GRAY)
    )

    LobbyVersionStatus.DevelopmentBuild -> sendMessage(
        text(
            "Entwicklungs-Build - es gibt keine Build-Nummer zum Vergleichen.",
            NamedTextColor.GRAY
        )
    )

    is LobbyVersionStatus.Behind -> {
        val behind = text()
            .append(text("Der Server ist ", NamedTextColor.GRAY))
            .append {
                if (status.atLeast) {
                    text("mindestens ", NamedTextColor.GRAY)
                } else {
                    Component.empty()
                }
            }
            .append(text(status.builds, NamedTextColor.GOLD))
            .append {
                if (status.builds == 1) {
                    text(" Build ", NamedTextColor.GRAY)
                } else {
                    text(" Builds ", NamedTextColor.GRAY)
                }
            }
            .append(text("hinter dem neuesten Build ", NamedTextColor.GRAY))
            .append(text("#${status.latestBuildNumber}", NamedTextColor.GOLD))
            .append(text(".", NamedTextColor.GRAY))

        status.latestCommit?.let { commit ->
            behind.hoverEvent(text("Neuester Commit: $commit", NamedTextColor.GRAY))
        }

        sendMessage(behind)

        sendMessage(
            text()
                .append(text("Download: ", NamedTextColor.GRAY))
                .append(
                    text(LOBBY_DOWNLOAD_URL, NamedTextColor.GOLD)
                        .clickEvent(ClickEvent.openUrl(LOBBY_DOWNLOAD_URL))
                )
        )
    }

    is LobbyVersionStatus.CheckFailed -> sendMessage(
        text()
            .append(text("Die Build-Prüfung ist fehlgeschlagen: ", NamedTextColor.RED))
            .append(text(status.reason, NamedTextColor.GOLD))
    )
}
