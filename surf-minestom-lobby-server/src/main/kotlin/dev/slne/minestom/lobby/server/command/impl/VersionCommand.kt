package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutorSuspend
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandAPICommand
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import dev.slne.minestom.lobby.server.version.LOBBY_DOWNLOAD_URL
import dev.slne.minestom.lobby.server.version.LobbyBuildInfo
import dev.slne.minestom.lobby.server.version.LobbyVersionService
import dev.slne.minestom.lobby.server.version.LobbyVersionStatus
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import net.kyori.adventure.text.Component.text
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
    sendText {
        appendInfoPrefix()
        info("Dieser Server läuft auf Surf Minestom Lobby ")
        variableValue(buildInfo.displayVersion)

        buildInfo.commit?.let {
            hoverEvent(buildText {
                info("Commit: ")
                variableValue(it)
            })
            clickCopiesToClipboard(it)
        }
    }

    buildInfo.commitTime?.let { commitTime ->
        sendText {
            spacer("Commit vom ")
            variableValue(commitTime.toString())
        }
    }

    sendText {
        spacer("Minecraft ")
        variableValue(MinecraftServer.VERSION_NAME)
        spacer(" (Protokoll ")
        variableValue(MinecraftServer.PROTOCOL_VERSION.toString())
        spacer(")")
    }
}

private fun CommandSender.sendStatus(status: LobbyVersionStatus) = when (status) {
    LobbyVersionStatus.UpToDate -> sendMessage(
        text("Der Server läuft auf dem neuesten Build.", Colors.SUCCESS)
    )

    LobbyVersionStatus.DevelopmentBuild -> sendMessage(
        text(
            "Der Server läuft auf einer Entwicklungsversion.",
            Colors.SPACER
        )
    )

    is LobbyVersionStatus.Behind -> {
        sendText {
            spacer("Der Server ist ")
            variableValue(status.builds.toString())
            spacer(" Build(s) hinter dem neuesten Build #${status.latestBuildNumber}.")

            status.latestCommit?.let { commit ->
                hoverEvent(buildText {
                    spacer("Neuester Commit: ")
                    variableValue(commit)
                })
            }
        }
        sendText {
            spacer("Download: ")
            variableValue(LOBBY_DOWNLOAD_URL)
            clickOpensUrl(LOBBY_DOWNLOAD_URL)
        }
    }

    is LobbyVersionStatus.CheckFailed -> sendMessage(
        buildText {
            error("Die Build-Prüfung ist fehlgeschlagen: ")
            variableValue(status.reason)
        }
    )
}
