package dev.slne.minestom.lobby.server.mixin.extension;

import net.minestom.server.network.packet.server.ServerPacket;

public interface MutablePlayerPacketOutEvent {
  void surf$setPacket(ServerPacket packet);
}
