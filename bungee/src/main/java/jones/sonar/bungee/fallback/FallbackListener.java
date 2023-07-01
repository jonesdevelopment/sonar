/*
 * Copyright (C) 2023 jones
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package jones.sonar.bungee.fallback;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import io.netty.channel.ChannelPipeline;
import jones.sonar.api.Sonar;
import jones.sonar.api.fallback.Fallback;
import jones.sonar.bungee.fallback.compress.FallbackPacketCompressor;
import jones.sonar.bungee.fallback.compress.FallbackPacketDecompressor;
import jones.sonar.bungee.fallback.session.dummy.DummyPacketHandler;
import jones.sonar.common.fallback.FallbackChannelHandler;
import jones.sonar.common.fallback.FallbackTimeoutHandler;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.compress.PacketCompressor;
import net.md_5.bungee.compress.PacketDecompressor;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PipelineUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static jones.sonar.api.fallback.FallbackPipelines.HANDLER;
import static jones.sonar.bungee.fallback.FallbackListener.CachedMessages.*;
import static net.md_5.bungee.netty.PipelineUtils.FRAME_PREPENDER;
import static net.md_5.bungee.netty.PipelineUtils.TIMEOUT_HANDLER;

@RequiredArgsConstructor
@SuppressWarnings("deprecation")
public final class FallbackListener implements Listener {
  private final Fallback fallback;

  private static final Field CHANNEL_WRAPPER;

  static {
    try {
      CHANNEL_WRAPPER = InitialHandler.class.getDeclaredField("ch");
      CHANNEL_WRAPPER.setAccessible(true);
    } catch (Throwable throwable) {
      throw new IllegalStateException(throwable);
    }
  }

  public static class CachedMessages {
    static TextComponent TOO_MANY_PLAYERS;
    static TextComponent BLACKLISTED;
    static TextComponent ALREADY_VERIFYING;
    static TextComponent ALREADY_QUEUED;
    static TextComponent TOO_MANY_ONLINE_PER_IP;
    static TextComponent TOO_FAST_RECONNECT;
    public static TextComponent UNEXPECTED_ERROR;

    public static void update() {
      ALREADY_VERIFYING = new TextComponent(Sonar.get().getConfig().ALREADY_VERIFYING);
      ALREADY_QUEUED = new TextComponent(Sonar.get().getConfig().ALREADY_QUEUED);
      TOO_MANY_PLAYERS = new TextComponent(Sonar.get().getConfig().TOO_MANY_PLAYERS);
      BLACKLISTED = new TextComponent(Sonar.get().getConfig().BLACKLISTED);
      TOO_MANY_ONLINE_PER_IP = new TextComponent(Sonar.get().getConfig().TOO_MANY_ONLINE_PER_IP);
      TOO_FAST_RECONNECT = new TextComponent(Sonar.get().getConfig().TOO_FAST_RECONNECT);
      UNEXPECTED_ERROR = new TextComponent(Sonar.get().getConfig().UNEXPECTED_ERROR);
    }
  }

  @EventHandler
  public void handle(final @NotNull PostLoginEvent event) throws Throwable {
    final InitialHandler initialHandler = (InitialHandler) event.getPlayer().getPendingConnection();
    final ChannelWrapper channelWrapper = (ChannelWrapper) CHANNEL_WRAPPER.get(initialHandler);

    final ChannelPipeline pipeline = channelWrapper.getHandle().pipeline();

    final int compressionThreshold = BungeeCord.getInstance().getConfig().getCompressionThreshold();

    if (compressionThreshold <= 0) return;

    final VelocityCompressor velocityCompressor = Natives.compress.get().create(-1);

    // You don't need the frame decoder anymore
    if (pipeline.get(FRAME_PREPENDER) != null) {
      pipeline.remove(FRAME_PREPENDER);
    }

    // Replace compression handlers
    if (pipeline.get(PacketCompressor.class) != null
      && pipeline.get(PacketDecompressor.class) != null) {
      pipeline.replace(
        PacketCompressor.class,
        "compress",
        new FallbackPacketCompressor(
          compressionThreshold,
          velocityCompressor
        )
      );
      pipeline.replace(
        PacketDecompressor.class,
        "decompress",
        new FallbackPacketDecompressor(
          compressionThreshold,
          velocityCompressor
        )
      );
    }
  }

  @EventHandler
  public void handle(final @NotNull PreLoginEvent event) throws Throwable {
    fallback.getSonar().getStatistics().increment("total");

    final InetAddress inetAddress = event.getConnection().getAddress().getAddress();

    if (fallback.getBlacklisted().contains(inetAddress.toString())) {
      event.setCancelled(true);
      event.setCancelReason(BLACKLISTED);
      return;
    }

    final InitialHandler initialHandler = (InitialHandler) event.getConnection();
    final ChannelWrapper channelWrapper = (ChannelWrapper) CHANNEL_WRAPPER.get(initialHandler);

    // Check if the number of online players using the same IP address as
    // the connecting player is greater than the configured amount
    final int maxOnlinePerIp = fallback.getSonar().getConfig().MAXIMUM_ONLINE_PER_IP;

    if (maxOnlinePerIp > 0) {
      final long onlinePerIp = ProxyServer.getInstance().getPlayers().stream()
        .filter(player -> Objects.equals(player.getAddress().getAddress(), inetAddress))
        .count();

      // We use '>=' because the player connecting to the server hasn't joined yet
      if (onlinePerIp >= maxOnlinePerIp) {
        event.setCancelled(true);
        event.setCancelReason(TOO_MANY_ONLINE_PER_IP);
        return;
      }
    }

    if (fallback.getVerified().contains(inetAddress.toString())) return;
    if (!fallback.getSonar().getConfig().ENABLE_VERIFICATION) return;

    // Check if Fallback is already verifying a player
    // → is another player with the same IP address connected to Fallback?
    if (fallback.getConnected().contains(inetAddress.toString())) {
      event.setCancelled(true);
      event.setCancelReason(ALREADY_VERIFYING);
      return;
    }

    // We cannot allow too many players on our Fallback server
    if (fallback.getQueue().getQueuedPlayers().size() > fallback.getSonar().getConfig().MAXIMUM_QUEUED_PLAYERS
      || fallback.getConnected().size() > fallback.getSonar().getConfig().MAXIMUM_VERIFYING_PLAYERS) {
      event.setCancelled(true);
      event.setCancelReason(TOO_MANY_PLAYERS);
      return;
    }

    // Check if the IP address is reconnecting too quickly while being unverified
    if (!fallback.getAttemptLimiter().attempt(inetAddress)) {
      event.setCancelled(true);
      event.setCancelReason(TOO_FAST_RECONNECT);
      return;
    }

    // Check if the player is already queued since we don't want bots to flood the queue
    // TODO: do some performance testing
    if (fallback.getQueue().getQueuedPlayers().stream()
      .anyMatch(pair -> Objects.equals(pair.getFirst(), inetAddress))) {
      event.setCancelled(true);
      event.setCancelReason(ALREADY_QUEUED);
      return;
    }

    handleLogin(initialHandler, channelWrapper);
  }

  private void handleLogin(final InitialHandler initialHandler, final ChannelWrapper channelWrapper) {
    if (!fallback.getSonar().getConfig().ENABLE_VERIFICATION) return;

    final InetAddress inetAddress = initialHandler.getAddress().getAddress();

    // We don't want to check players that have already been verified
    if (fallback.getVerified().contains(inetAddress.toString())) return;

    // Run in the channel's event loop
    channelWrapper.getHandle().eventLoop().execute(() -> {

      // Do not continue if the connection is closed
      if (channelWrapper.isClosed()) return;

      final ChannelPipeline pipeline = channelWrapper.getHandle().pipeline();

      // Replace timeout handler to avoid known exploits or issues
      // We also want to timeout bots quickly to avoid flooding
      pipeline.replace(
        TIMEOUT_HANDLER,
        TIMEOUT_HANDLER,
        new FallbackTimeoutHandler(
          fallback.getSonar().getConfig().VERIFICATION_TIMEOUT,
          TimeUnit.MILLISECONDS
        )
      );

      // TODO: fix
      final HandlerBoss handlerBoss = (HandlerBoss) pipeline.get(PipelineUtils.BOSS_HANDLER);
      handlerBoss.setHandler(new DummyPacketHandler(initialHandler.getName(), inetAddress));

      // We have to add this pipeline to monitor whenever the client disconnects
      // to remove them from the list of connected and queued players
      pipeline.addFirst(HANDLER, FallbackChannelHandler.INSTANCE);

      // Queue the connection for further processing
      fallback.getQueue().queue(inetAddress, () -> channelWrapper.getHandle().eventLoop().execute(() -> {

        // Do not continue if the connection is closed
        if (channelWrapper.isClosed()) return;

        // TODO: create Fallback for BungeeCord
        handlerBoss.setHandler(initialHandler);

        // Create an instance for the connected player
        /*final ProxiedPlayer player;
        try {
          player = (ProxiedPlayer) CONNECTED_PLAYER.invokeExact(
            mcConnection.server,
            event.getGameProfile(),
            mcConnection,
            inboundConnection.getVirtualHost().orElse(null),
            isPremium,
            inboundConnection.getIdentifiedKey()
          );
        } catch (Throwable throwable) {
          // This should not happen
          fallback.getLogger().error("Error while processing {}: {}", event.getUsername(), throwable);
          mcConnection.close(true);
          return;
        }

        // Check if the player is already connected to the proxy
        // We use the default Velocity method for this to avoid incompatibilities
        if (!mcConnection.server.canRegisterConnection(player)) {
          player.disconnect0( // TODO: Cache translations?
            Component.translatable("velocity.error.already-connected-proxy", NamedTextColor.RED), true
          );
          return;
        }

        // Create an instance for the Fallback connection
        final FallbackPlayer fallbackPlayer = new FallbackPlayer(
          fallback,
          player, mcConnection, channel, pipeline, inetAddress,
          player.getProtocolVersion().getProtocol()
        );

        // Only log the processing message if the server isn't under attack
        // We let the user override this through the configuration
        if (!fallback.isUnderAttack() || fallback.getSonar().getConfig().LOG_DURING_ATTACK) {
          fallback.getLogger().info("Processing: {}{} ({})",
            event.getUsername(), inetAddress, fallbackPlayer.getProtocolVersion());
        }

        // Mark the player as connected → verifying players
        fallback.getConnected().add(inetAddress);

        // Check if compression is enabled in the Sonar configuration
        if (fallback.getSonar().getConfig().ENABLE_COMPRESSION) {
          final int threshold = mcConnection.server.getConfiguration().getCompressionThreshold();

          // Set compression
          if (threshold >= 0 && mcConnection.getProtocolVersion().compareTo(MINECRAFT_1_8) >= 0) {
            mcConnection.write(new SetCompression(threshold));
            mcConnection.setCompressionThreshold(threshold);
          }
        }

        // Send LoginSuccess packet to spoof our fake lobby
        final ServerLoginSuccess success = new ServerLoginSuccess();

        success.setUsername(player.getUsername());
        success.setProperties(player.getGameProfileProperties());
        success.setUuid(player.getUniqueId());

        mcConnection.write(success);

        // Set the state to a custom one, so we can receive and send more packets
        mcConnection.setAssociation(player);
        mcConnection.setState(StateRegistry.PLAY);

        final long keepAliveId = ThreadLocalRandom.current().nextInt();

        // We have to add this pipeline to monitor all incoming traffic
        // We add the pipeline after the MinecraftDecoder since we want
        // the packets to be processed and decoded already
        fallbackPlayer.getPipeline().addAfter(
          MINECRAFT_DECODER,
          DECODER,
          new FallbackPacketDecoder(fallbackPlayer, keepAliveId)
        );

        // ==================================================================
        if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
          // The first step of the verification is a simple KeepAlive packet
          // We don't want to waste resources by directly sending all packets to
          // the client, which is why we first send a KeepAlive packet and then
          // wait for a valid response to continue the verification process
          final KeepAlive keepAlive = new KeepAlive();

          keepAlive.setRandomId(keepAliveId);

          mcConnection.write(keepAlive);
        } else {
          // KeepAlive packets do not exist during the login process on 1.7.
          // We have to fall back to the regular method of verification

          // Set session handler to custom fallback handler to intercept all incoming packets
          mcConnection.setSessionHandler(new FallbackSessionHandler(
            mcConnection.getSessionHandler(), fallbackPlayer
          ));

          // Send JoinGame packet
          mcConnection.write(LEGACY_JOIN_GAME);
        }
        // ==================================================================*/
      }));
    });
  }
}
