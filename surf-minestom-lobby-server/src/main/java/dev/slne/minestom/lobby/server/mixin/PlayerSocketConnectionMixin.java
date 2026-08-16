package dev.slne.minestom.lobby.server.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.player.PlayerSocketConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("UnstableApiUsage")
@Mixin(PlayerSocketConnection.class)
abstract class PlayerSocketConnectionMixin {

  @Inject(
      method = "writePacketSync",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minestom/server/event/ListenerHandle;call(Lnet/minestom/server/event/Event;)V",
          shift = Shift.AFTER
      ),
      remap = false
  )
  private void surf$replaceOutgoingPacket(
      NetworkBuffer buffer,
      SendablePacket packet,
      boolean compressed,
      CallbackInfoReturnable<Boolean> cir,

      @Local(name = "event") PlayerPacketOutEvent event,
      @Local(argsOnly = true, name = "packet") LocalRef<SendablePacket> packetRef
  ) {
    packetRef.set(event.getPacket());
  }
}
