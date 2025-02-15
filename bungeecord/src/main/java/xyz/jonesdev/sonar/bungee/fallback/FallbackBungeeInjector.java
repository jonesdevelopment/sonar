/*
 * Copyright (C) 2025 Sonar Contributors
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

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import lombok.experimental.UtilityClass;
import lombok.val;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.channel.BungeeChannelInitializer;
import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.common.fallback.netty.FallbackInjectedChannelInitializer;

import java.lang.reflect.Field;

import static net.md_5.bungee.netty.PipelineUtils.PACKET_DECODER;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_PACKET_HANDLER;

@UtilityClass
public class FallbackBungeeInjector {
  public void inject() {
    try {
      final Field childField;

      try {
        // This field only exists on BungeeCord build 1908 and below.
        // See https://github.com/SpigotMC/BungeeCord/pull/3776 for more info.
        //noinspection all
        childField = PipelineUtils.class.getField("SERVER_CHILD");
      } catch (NoSuchFieldException exception) {
        injectOnModernBungeeCord();
        return;
      }

      childField.setAccessible(true);
      injectOnOldBungeeCord(childField);
    } catch (Exception exception) {
      Sonar.get0().getLogger().error("An error occurred while injecting {}", exception);
    }
  }

  private static void injectOnOldBungeeCord(final @NotNull Field childField) throws Exception {
    // Make sure to store the original channel initializer
    //noinspection unchecked
    val originalInitializer = (ChannelInitializer<Channel>) childField.get(null);
    final ChannelInitializer<Channel> injectedInitializer = new FallbackInjectedChannelInitializer(
      originalInitializer, pipeline -> pipeline.addAfter(PACKET_DECODER, FALLBACK_PACKET_HANDLER,
      new FallbackBungeeInboundHandler()));

    final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
    unsafeField.setAccessible(true);
    final Unsafe unsafe = (Unsafe) unsafeField.get(null);

    final Object base = unsafe.staticFieldBase(childField);
    final long offset = unsafe.staticFieldOffset(childField);

    // Replace the original channel initializer with our new injected initializer
    unsafe.putObject(base, offset, injectedInitializer);
  }

  // See https://github.com/SpigotMC/BungeeCord/pull/3787
  private static void injectOnModernBungeeCord() {
    // The order is important...
    final BungeeChannelInitializer original = ProxyServer.getInstance().unsafe().getFrontendChannelInitializer();
    final BungeeChannelInitializer newInitializer = BungeeChannelInitializer.create(
      channel -> {
        // https://github.com/SpigotMC/BungeeCord/pull/3787#issuecomment-2661059876
        if (original.getChannelAcceptor().accept(channel)) {
          return false;
        }
        FallbackInjectedChannelInitializer.inject(channel,
          pipeline -> pipeline.addAfter(PACKET_DECODER, FALLBACK_PACKET_HANDLER,
            new FallbackBungeeInboundHandler()));
        return true;
      });
    ProxyServer.getInstance().unsafe().setFrontendChannelInitializer(newInitializer);
  }
}
