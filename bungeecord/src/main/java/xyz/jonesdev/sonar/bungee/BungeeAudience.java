/*
 * Copyright (C) 2026 Sonar Contributors
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

package xyz.jonesdev.sonar.bungee;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Minimal {@link Audience} implementation backed by the native BungeeCord chat API.
 * <br>
 * BungeeCord does not ship Adventure, so we used to bundle adventure-platform-bungeecord (Adventure 4.x).
 * That conflicted with the Adventure 5.x libraries Sonar loads at runtime, because both end up
 * in the same relocated package. Converting components to JSON and passing them to BungeeCord's own
 * {@link ComponentSerializer} allows us to depend on exactly one Adventure version.
 */
final class BungeeAudience implements Audience {
  /**
   * Protocol version of Minecraft 1.16, the first version with support for hex colors
   */
  private static final int MINECRAFT_1_16_PROTOCOL = 735;

  private final CommandSender sender;

  BungeeAudience(final @NotNull CommandSender sender) {
    this.sender = sender;
  }

  @Override
  public void sendMessage(final @NotNull Component message) {
    sender.sendMessage(toBungee(message));
  }

  @Override
  public void sendActionBar(final @NotNull Component message) {
    // Only players have an action bar
    if (sender instanceof ProxiedPlayer player) {
      player.sendMessage(ChatMessageType.ACTION_BAR, toBungee(message));
    }
  }

  @Override
  public void showTitle(final @NotNull Title title) {
    // Only players can see titles
    if (sender instanceof ProxiedPlayer player) {
      final Title.Times times = title.times();
      ProxyServer.getInstance().createTitle()
        .title(toBungee(title.title()))
        .subTitle(toBungee(title.subtitle()))
        // Use the same defaults as Title#DEFAULT_TIMES (0.5s, 3.5s, 1s)
        .fadeIn(times == null ? 10 : toTicks(times.fadeIn()))
        .stay(times == null ? 70 : toTicks(times.stay()))
        .fadeOut(times == null ? 20 : toTicks(times.fadeOut()))
        .send(player);
    }
  }

  private static int toTicks(final @NotNull Duration duration) {
    return (int) (duration.toMillis() / 50L);
  }

  private @NotNull BaseComponent[] toBungee(final @NotNull Component component) {
    // Downsample hex colors for clients that do not support them
    final boolean legacyClient = sender instanceof ProxiedPlayer player
      && player.getPendingConnection().getVersion() < MINECRAFT_1_16_PROTOCOL;
    final GsonComponentSerializer serializer = legacyClient
      ? GsonComponentSerializer.colorDownsamplingGson()
      : GsonComponentSerializer.gson();
    return ComponentSerializer.parse(serializer.serialize(component));
  }
}
