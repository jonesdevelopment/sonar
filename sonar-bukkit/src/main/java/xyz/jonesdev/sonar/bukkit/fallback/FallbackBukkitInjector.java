/*
 * Copyright (C) 2023-2024 Sonar Contributors
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

package xyz.jonesdev.sonar.bukkit.fallback;

import io.netty.channel.*;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.ReflectiveOperationException;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.common.fallback.injection.FallbackInjectedChannelInitializer;

import java.lang.reflect.Field;
import java.util.List;

import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_PACKET_HANDLER;

// Check out these links if you want to see some more magic
// https://github.com/retrooper/packetevents/blob/2.0/spigot/src/main/java/io/github/retrooper/packetevents/util/SpigotReflectionUtil.java
// https://github.com/dmulloy2/ProtocolLib/blob/master/TinyProtocol/src/main/java/com/comphenix/tinyprotocol/TinyProtocol.java
@UtilityClass
public class FallbackBukkitInjector {
  private final String MODIFIED_PACKAGE_NAME;
  private final String LEGACY_NMS_PACKAGE;
  private final String OBC_PACKAGE;

  private final BukkitServerVersion SERVER_VERSION;
  private final boolean USES_LEGACY_PACKAGING, OBFUSCATED;

  private final Class<?> MINECRAFT_SERVER_CLASS;
  private final Class<?> CRAFTBUKKIT_SERVER_CLASS;
  private final Class<?> SERVER_CONNECTION_CLASS;

  private Object MINECRAFT_SERVER_INSTANCE;
  private final Object MINECRAFT_SERVER_CONNECTION_INSTANCE;

  static {
    final String bukkitPackage = Bukkit.getServer().getClass().getPackage().getName();

    MODIFIED_PACKAGE_NAME = bukkitPackage.split("\\.")[3];
    LEGACY_NMS_PACKAGE = "net.minecraft.server." + MODIFIED_PACKAGE_NAME + ".";
    OBC_PACKAGE = bukkitPackage + ".";

    SERVER_VERSION = resolveServerVersion();
    USES_LEGACY_PACKAGING = SERVER_VERSION.compareTo(BukkitServerVersion.MINECRAFT_1_17) < 0;

    try {
      OBFUSCATED = isObfuscated();
      // Minecraft classes
      MINECRAFT_SERVER_CLASS = getNMSClass("server.MinecraftServer", "MinecraftServer");
      SERVER_CONNECTION_CLASS = getNMSClass(OBFUSCATED ? "server.network.ServerConnection" : "server.network.ServerConnectionListener", "ServerConnection");
      // CraftBukkit classes
      CRAFTBUKKIT_SERVER_CLASS = getOBCClass("CraftServer");

      try {
        // 1.20.5+
        MINECRAFT_SERVER_INSTANCE = getFieldAt(MINECRAFT_SERVER_CLASS, MINECRAFT_SERVER_CLASS, 0).get(null);
      } catch (Exception exception) {
        MINECRAFT_SERVER_INSTANCE = getFieldAt(CRAFTBUKKIT_SERVER_CLASS, MINECRAFT_SERVER_CLASS, 0).get(Bukkit.getServer());
      }

      MINECRAFT_SERVER_CONNECTION_INSTANCE = getFieldAt(MINECRAFT_SERVER_CLASS, SERVER_CONNECTION_CLASS, 0).get(MINECRAFT_SERVER_INSTANCE);
    } catch (Exception exception) {
      throw new ReflectiveOperationException(exception);
    }
  }

  private @NotNull Field getFieldAt(final @NotNull Class<?> clazz, final @NotNull Class<?> type, final int index) {
    int currentIndex = 0;
    for (final Field field : clazz.getDeclaredFields()) {
      field.setAccessible(true);

      if (type.isAssignableFrom(field.getType())) {
        if (currentIndex++ == index) {
          return field;
        }
      }
    }

    if (clazz.getSuperclass() != null) {
      return getFieldAt(clazz.getSuperclass(), type, index);
    }
    throw new IllegalStateException("Could not find field #" + index + " in " + clazz.getName());
  }

  private @NotNull BukkitServerVersion resolveServerVersion() {
    final String bukkitVersion = Bukkit.getBukkitVersion();

    for (final BukkitServerVersion serverVersion : BukkitServerVersion.REVERSED_VALUES) {
      if (bukkitVersion.contains(serverVersion.getRelease())) {
        Sonar.get().getLogger().info("Detected Minecraft {} on Bukkit {}",
          serverVersion.getRelease(), bukkitVersion);
        return serverVersion;
      }
    }

    // Throw an error if we cannot find the server version
    throw new IllegalStateException("Could not find server version " + bukkitVersion);
  }

  private boolean isObfuscated() {
    try {
      Class.forName("net.minecraft.server.network.PlayerConnection");
      return true;
    } catch (ClassNotFoundException exception) {
      return false;
    }
  }

  private @NotNull Class<?> getNMSClass(final String modern, final String legacy) throws ClassNotFoundException {
    return Class.forName(USES_LEGACY_PACKAGING ? LEGACY_NMS_PACKAGE + legacy : "net.minecraft." + modern);
  }

  private @NotNull Class<?> getOBCClass(final String clazz) throws ClassNotFoundException {
    return Class.forName(OBC_PACKAGE + clazz);
  }

  // Credits to TinaProtocol for this idea!
  private final ChannelHandler CLIENT_INITIALIZER = new ChannelInitializer<>() {

    @Override
    protected void initChannel(final @NotNull Channel channel) throws Exception {
      // We don't have to call the initialization method, since the channel is already initialized
      channel.pipeline().addLast(new FallbackInjectedChannelInitializer(null,
        pipeline -> pipeline.addBefore("decoder", FALLBACK_PACKET_HANDLER,
          new FallbackBukkitPacketDecoder())));
    }
  };

  private final ChannelHandler SERVER_INITIALIZER = new ChannelInboundHandlerAdapter() {

    @Override
    public void channelRead(final @NotNull ChannelHandlerContext ctx, final Object msg) throws Exception {
      final Channel serverChannel = (Channel) msg;
      serverChannel.pipeline().addFirst(CLIENT_INITIALIZER);
      ctx.fireChannelRead(msg);
    }
  };

  public void inject() {
    try {
      for (int i = 0; i < 2; i++) {
        // We're using a label here, so we can escape the inner loops
        injection: {
          final Field field = getFieldAt(SERVER_CONNECTION_CLASS, List.class, i);
          final List<?> list = (List<?>) field.get(MINECRAFT_SERVER_CONNECTION_INSTANCE);

          // Late-bind could be enabled or this isn't out target field
          if (list.isEmpty()) {
            break injection;
          }

          for (final Object object : list) {
            // We're only looking for a list of channel futures...
            if (!(object instanceof ChannelFuture)) {
              break injection;
            }

            // Get the future that contains the server connection
            final Channel serverChannel = ((ChannelFuture) object).channel();
            // Add our server channel listener
            serverChannel.pipeline().addFirst(SERVER_INITIALIZER);
          }
          return;
        }
      }
    } catch (Exception exception) {
      throw new ReflectiveOperationException(exception);
    }
  }
}
