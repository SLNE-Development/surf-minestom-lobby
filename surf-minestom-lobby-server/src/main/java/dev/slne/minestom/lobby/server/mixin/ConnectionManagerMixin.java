package dev.slne.minestom.lobby.server.mixin;

import dev.slne.minestom.lobby.server.duck.ConnectionManagerDuck;
import dev.slne.minestom.lobby.server.player.config.LobbyConfiguration;
import java.util.Set;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands the configuration phase to {@link LobbyConfiguration}, which runs it as the ordered task
 * sequence vanilla uses.
 */
@Mixin(ConnectionManager.class)
abstract class ConnectionManagerMixin implements ConnectionManagerDuck {

  @Accessor(value = "configurationPlayers", remap = false)
  @Override
  public abstract Set<Player> surf$configurationPlayers();

  @Accessor(value = "keepAlivePlayers", remap = false)
  @Override
  public abstract Set<Player> surf$keepAlivePlayers();

  @Inject(
      method = "doConfiguration(Lnet/minestom/server/entity/Player;Z)V",
      at = @At("HEAD"),
      cancellable = true,
      remap = false,
      require = 1
  )
  private void surf$runVanillaTaskOrder(
      Player player,
      boolean isFirstConfig,
      CallbackInfo ci
  ) {
    ci.cancel();
    LobbyConfiguration.doConfiguration(player, isFirstConfig);
  }
}
