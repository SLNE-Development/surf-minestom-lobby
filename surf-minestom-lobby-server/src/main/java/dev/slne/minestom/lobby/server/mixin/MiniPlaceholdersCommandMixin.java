package dev.slne.minestom.lobby.server.mixin;

import java.util.Collection;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * MiniPlaceholders 3.2.0 is compiled against Minestom 26.2, where
 * {@code ConnectionManager#getOnlinePlayers} returned a {@code Collection}. Minestom 26.3 returns a
 * {@code Set}, so the original call fails with a {@code NoSuchMethodError} as soon as the
 * {@code /miniplaceholders} command lists the online players.
 */
@Mixin(targets = "io.github.miniplaceholders.minestom.command.MinestomCommand", remap = false)
abstract class MiniPlaceholdersCommandMixin {

  @Redirect(
      method = "lambda$new$0",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minestom/server/network/ConnectionManager;getOnlinePlayers()Ljava/util/Collection;",
          remap = false
      ),
      remap = false,
      require = 1
  )
  private static Collection<Player> surf$onlinePlayers(ConnectionManager connectionManager) {
    return connectionManager.getOnlinePlayers();
  }
}
