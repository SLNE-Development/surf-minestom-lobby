package dev.slne.minestom.lobby.server.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import java.io.IOException;
import java.net.Socket;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.packet.PacketParser;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.player.PlayerSocketConnection;
import net.minestom.server.network.socket.Server;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = Server.class, remap = false)
abstract class ServerMixin {

  @Unique
  private static final String SURF$SEND_BUFFER_SIZE_PROPERTY = "minestom.send-buffer-size";

  @Unique
  private static final String SURF$RECEIVE_BUFFER_SIZE_PROPERTY = "minestom.receive-buffer-size";

  @Unique
  private static final String SURF$NETWORK_DEBUG_PROPERTY = "surf.network-debug";

  @Unique
  private static final boolean SURF$LINUX = System.getProperty("os.name", "").startsWith("Linux");

  @Unique
  private static final boolean SURF$NETWORK_DEBUG = Boolean.getBoolean(SURF$NETWORK_DEBUG_PROPERTY);

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

  @Redirect(
      method = "playerReadLoop",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minestom/server/network/player/PlayerSocketConnection;read(Lnet/minestom/server/network/packet/PacketParser;)V"
      ),
      remap = false,
      require = 1
  )
  private void surf$read(
      PlayerSocketConnection connection,
      PacketParser<ClientPacket> packetParser
  ) throws IOException {
    try {
      connection.read(packetParser);
    } catch (IOException | RuntimeException | Error throwable) {
      surf$logSocketFailure("read", connection, throwable);
      throw throwable;
    }
  }

  @Redirect(
      method = "playerWriteLoop",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minestom/server/network/player/PlayerSocketConnection;flushSync()V",
          ordinal = 0
      ),
      remap = false,
      require = 1
  )
  private void surf$flush(PlayerSocketConnection connection) throws IOException {
    try {
      connection.flushSync();
    } catch (IOException | RuntimeException | Error throwable) {
      surf$logSocketFailure("write", connection, throwable);
      throw throwable;
    }
  }

  @Unique
  private static void surf$logSocketFailure(
      String operation,
      PlayerSocketConnection connection,
      Throwable throwable
  ) {
    if (!SURF$NETWORK_DEBUG) {
      return;
    }

    MinecraftServer.LOGGER.warn(
        "[NetworkDebug] Socket {} failed: identifier={}, loginUsername={}, remote={}, "
            + "clientState={}, serverState={}, online={}, channelOpen={}",
        operation,
        connection.getIdentifier(),
        connection.getLoginUsername(),
        connection.getRemoteAddress(),
        connection.getClientState(),
        connection.getServerState(),
        connection.isOnline(),
        connection.getChannel().isOpen(),
        throwable
    );
  }
}
