package dev.slne.minestom.lobby.server.mixin;

import dev.slne.minestom.lobby.server.command.commandapi.CommandAPIHook;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandResult;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandManager.class)
abstract class CommandManagerMixin {

  @Inject(
      method = "commandExists(Ljava/lang/String;)Z",
      at = @At("HEAD"),
      cancellable = true,
      remap = false,
      require = 1
  )
  private void surf$commandApiOwnsName(String commandName, CallbackInfoReturnable<Boolean> cir) {
    if (CommandAPIHook.owns(commandName)) {
      cir.setReturnValue(true);
    }
  }

  /**
   * Injected at the parse call rather than at the head, so a {@code PlayerCommandEvent} listener
   * still sees the command and can cancel or rewrite it before the dispatcher does.
   */
  @Inject(
      method = "execute(Lnet/minestom/server/command/CommandSender;Ljava/lang/String;)Lnet/minestom/server/command/builder/CommandResult;",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minestom/server/command/CommandManager;parseCommand(Lnet/minestom/server/command/CommandSender;Ljava/lang/String;)Lnet/minestom/server/command/CommandParser$Result;",
          remap = false
      ),
      cancellable = true,
      remap = false,
      require = 1
  )
  private void surf$dispatchCommandApi(
      CommandSender sender,
      String command,
      CallbackInfoReturnable<CommandResult> cir
  ) {
    final CommandResult result = CommandAPIHook.execute(sender, command);
    if (result != null) {
      cir.setReturnValue(result);
    }
  }

  @Inject(
      method = "createDeclareCommandsPacket(Lnet/minestom/server/entity/Player;)Lnet/minestom/server/network/packet/server/play/DeclareCommandsPacket;",
      at = @At("RETURN"),
      cancellable = true,
      remap = false,
      require = 1
  )
  private void surf$declareCommandApi(
      Player player,
      CallbackInfoReturnable<DeclareCommandsPacket> cir
  ) {
    cir.setReturnValue(CommandAPIHook.declare(cir.getReturnValue(), player));
  }
}
