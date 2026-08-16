package dev.slne.minestom.lobby.server.mixin;

import dev.slne.minestom.lobby.server.mixin.extension.MutablePlayerPacketOutEvent;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.network.packet.server.ServerPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings("UnstableApiUsage")
@Mixin(PlayerPacketOutEvent.class)
abstract class PlayerPacketOutEventMixin implements MutablePlayerPacketOutEvent {

  @Shadow
  @Final
  @Mutable
  private ServerPacket packet;

  @Override
  public void surf$setPacket(ServerPacket packet) {
    this.packet = packet;
  }
}
