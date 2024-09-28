/*
 * Copyright (C) 2024 Sonar Contributors
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

package xyz.jonesdev.sonar.api.fallback;

import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.api.timer.SystemTimer;

import java.net.InetAddress;

public interface FallbackUser {
  @NotNull
  Channel channel();

  @NotNull
  InetAddress getInetAddress();

  @NotNull
  ProtocolVersion getProtocolVersion();

  @NotNull
  SystemTimer getLoginTimer();

  @NotNull
  String getFingerprint();

  @NotNull
  String getUsername();

  boolean isForceCaptcha();

  void setForceCaptcha(final boolean isForceCaptcha);

  boolean isGeyser();

  /**
   * Disconnect the player during/after verification
   * using our custom Disconnect packet.
   *
   * @param reason      Disconnect message component
   */
  void disconnect(final @NotNull Component reason);

  /**
   * Sends a packet/message to the player
   *
   * @param msg Message to send to the player
   */
  default void write(final @NotNull Object msg) {
    if (channel().isActive()) {
      channel().writeAndFlush(msg, channel().voidPromise());
    } else {
      ReferenceCountUtil.release(msg);
    }
  }

  /**
   * Queues a buffered message that will be
   * sent once all messages are flushed.
   */
  default void delayedWrite(final @NotNull Object msg) {
    if (channel().isActive()) {
      channel().write(msg, channel().voidPromise());
    } else {
      ReferenceCountUtil.release(msg);
    }
  }
}
