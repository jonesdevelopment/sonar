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
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.compress.PacketCompressor;
import net.md_5.bungee.compress.PacketDecompressor;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.bungee.fallback.compress.FallbackPacketCompressor;
import xyz.jonesdev.sonar.bungee.fallback.compress.FallbackPacketDecompressor;
import xyz.jonesdev.sonar.common.exception.ReflectionException;

import java.lang.reflect.Field;

import static net.md_5.bungee.netty.PipelineUtils.FRAME_PREPENDER;
import static xyz.jonesdev.sonar.bungee.fallback.FallbackListener.CachedMessages.LOCKDOWN_DISCONNECT;

@SuppressWarnings("deprecation")
@RequiredArgsConstructor
public final class FallbackListener implements Listener {
  private final Fallback fallback;

  private static final Field CHANNEL_WRAPPER;

  static {
    try {
      CHANNEL_WRAPPER = InitialHandler.class.getDeclaredField("ch");
      CHANNEL_WRAPPER.setAccessible(true);
    } catch (Throwable throwable) {
      throw new ReflectionException(throwable);
    }
  }

  public static class CachedMessages {
    static TextComponent LOCKDOWN_DISCONNECT;

    public static void update() {
      LOCKDOWN_DISCONNECT = new TextComponent(Sonar.get().getConfig().LOCKDOWN_DISCONNECT);
    }
  }

  @EventHandler
  public void handle(final @NotNull PostLoginEvent event) throws Throwable {
    if (fallback.getSonar().getConfig().LOCKDOWN_ENABLED) {
      if (!event.getPlayer().hasPermission("sonar.lockdown")) {
        event.getPlayer().disconnect(LOCKDOWN_DISCONNECT);

        if (fallback.getSonar().getConfig().LOCKDOWN_LOG_ATTEMPTS) {
          fallback.getSonar().getLogger().info(
            fallback.getSonar().getConfig().LOCKDOWN_CONSOLE_LOG
              .replace("%player%", event.getPlayer().getName())
              .replace("%ip%", event.getPlayer().getAddress().getAddress().toString())
              .replace("%protocol%",
                String.valueOf(event.getPlayer().getPendingConnection().getVersion()))
          );
        }
        return;
      } else if (fallback.getSonar().getConfig().LOCKDOWN_ENABLE_NOTIFY) {
        event.getPlayer().sendMessage(
          new TextComponent(fallback.getSonar().getConfig().LOCKDOWN_NOTIFICATION)
        );
      }
    }

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
}
