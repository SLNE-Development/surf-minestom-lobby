package dev.slne.minestom.lobby.api.player

import revxrsal.commands.minestom.actor.MinestomCommandActor

fun MinestomCommandActor.asLobbyPlayer() = asPlayer() as? LobbyPlayer
fun MinestomCommandActor.requireLobbyPlayer() = requirePlayer() as LobbyPlayer