/*
 * Copyright (C) 2023 Sonar Contributors
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

package xyz.jonesdev.sonar.bungee.fallback;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import io.netty.channel.ChannelPipeline;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.compress.PacketCompressor;
import net.md_5.bungee.compress.PacketDecompressor;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.packet.Kick;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.ReflectiveOperationException;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.bungee.fallback.compress.FallbackPacketCompressor;
import xyz.jonesdev.sonar.bungee.fallback.compress.FallbackPacketDecompressor;
import xyz.jonesdev.sonar.bungee.fallback.handler.FallbackInitialHandler;

import java.lang.reflect.Field;

import static net.md_5.bungee.netty.PipelineUtils.FRAME_PREPENDER;

@RequiredArgsConstructor
public final class FallbackListener implements Listener {
  private final Fallback fallback;

  private static final Field CHANNEL_WRAPPER;

  static {
    try {
      CHANNEL_WRAPPER = InitialHandler.class.getDeclaredField("ch");
      CHANNEL_WRAPPER.setAccessible(true);
    } catch (Throwable throwable) {
      throw new ReflectiveOperationException(throwable);
    }
  }

  @EventHandler
  @SuppressWarnings("deprecation")
  public void handle(final @NotNull PostLoginEvent event) throws Throwable {
    if (Sonar.get().getConfig().getLockdown().isEnabled()) {
      if (!event.getPlayer().hasPermission("sonar.lockdown.bypass")) {
        final PendingConnection pendingConnection = event.getPlayer().getPendingConnection();
        // Try to close the channel with a custom serialized disconnect component
        if (pendingConnection instanceof FallbackInitialHandler) {
          final FallbackInitialHandler fallbackInitialHandler = (FallbackInitialHandler) pendingConnection;
          final Component component = Sonar.get().getConfig().getLockdown().getDisconnect();
          final String serialized = JSONComponentSerializer.json().serialize(component);
          fallbackInitialHandler.closeWith(new Kick(serialized));
        } else {
          // Fallback by disconnecting without a message
          pendingConnection.disconnect();
          Sonar.get().getLogger().warn("Fallback handler of {} is missing", event.getPlayer().getName());
        }

        if (Sonar.get().getConfig().getLockdown().isLogAttempts()) {
          Sonar.get().getLogger().info(Sonar.get().getConfig().getLockdown().getConsoleLog()
            .replace("%player%", event.getPlayer().getName())
            .replace("%ip%", Sonar.get().getConfig()
              .formatAddress(event.getPlayer().getAddress().getAddress()))
            .replace("%protocol%",
              String.valueOf(event.getPlayer().getPendingConnection().getVersion())));
        }
        return;
      } else if (Sonar.get().getConfig().getLockdown().isNotifyAdmins()) {
        event.getPlayer().sendMessage(new TextComponent(Sonar.get().getConfig().getLockdown().getNotification()));
      }
    }

    final InitialHandler initialHandler = (InitialHandler) event.getPlayer().getPendingConnection();
    final ChannelWrapper channelWrapper = (ChannelWrapper) CHANNEL_WRAPPER.get(initialHandler);

    final ChannelPipeline pipeline = channelWrapper.getHandle().pipeline();

    // You don't need the frame decoder anymore
    if (pipeline.get(FRAME_PREPENDER) != null) {
      pipeline.remove(FRAME_PREPENDER);
    }

    final int compressionThreshold = BungeeCord.getInstance().getConfig().getCompressionThreshold();

    // Replace compression handlers
    if (compressionThreshold > 0
      && pipeline.get(PacketCompressor.class) != null
      && pipeline.get(PacketDecompressor.class) != null) {
      final VelocityCompressor compressor = Natives.compress.get().create(-1);

      // Replace (de)compressor with Velocity's to ensure better performance
      pipeline.replace(
        PacketCompressor.class,
        "compress",
        // Create a new compressor instance with the Velocity compressor
        new FallbackPacketCompressor(compressionThreshold, compressor)
      );
      pipeline.replace(
        PacketDecompressor.class,
        "decompress",
        // Create a new decompressor instance with the Velocity decompressor
        new FallbackPacketDecompressor(compressionThreshold, compressor)
      );
    }
  }
}
