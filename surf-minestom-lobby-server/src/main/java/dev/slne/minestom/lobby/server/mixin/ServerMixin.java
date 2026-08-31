package dev.slne.minestom.lobby.server.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import java.net.Socket;
import net.minestom.server.network.socket.Server;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Server.class, remap = false)
abstract class ServerMixin {

  @Unique
  private static final String SURF$SEND_BUFFER_SIZE_PROPERTY = "minestom.send-buffer-size";
  @Unique
  private static final String SURF$RECEIVE_BUFFER_SIZE_PROPERTY = "minestom.receive-buffer-size";

  @Unique
  private static final boolean SURF$LINUX = System.getProperty("os.name", "").startsWith("Linux");

  @WrapWithCondition(
      method = "configureSocket",
      at = @At(
          value = "INVOKE",
          target = "Ljava/net/Socket;setSendBufferSize(I)V"
      ),
      remap = false
  )
  private static boolean surf$setSendBufferSize(Socket socket, int size) {
    return !SURF$LINUX || System.getProperty(SURF$SEND_BUFFER_SIZE_PROPERTY) != null;
  }

  @WrapWithCondition(
      method = "configureSocket",
      at = @At(
          value = "INVOKE",
          target = "Ljava/net/Socket;setReceiveBufferSize(I)V"
      ),
      remap = false
  )
  private static boolean surf$setReceiveBufferSize(Socket socket, int size) {
    return !SURF$LINUX || System.getProperty(SURF$RECEIVE_BUFFER_SIZE_PROPERTY) != null;
  }
}
