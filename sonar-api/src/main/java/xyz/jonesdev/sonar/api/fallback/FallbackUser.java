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

package xyz.jonesdev.sonar.api.fallback;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.api.timer.SystemTimer;

import java.net.InetAddress;
import java.util.UUID;

public interface FallbackUser {
  @NotNull
  Channel getChannel();

  @NotNull
  ChannelPipeline getPipeline();

  @NotNull
  InetAddress getInetAddress();

  @NotNull
  ProtocolVersion getProtocolVersion();

  @NotNull
  SystemTimer getLoginTimer();

  boolean isReceivedClientSettings();

  void setReceivedClientSettings(final boolean receivedClientSettings);

  boolean isReceivedPluginMessage();

  void setReceivedPluginMessage(final boolean receivedPluginMessage);

  boolean isGeyser();

  /**
   * Disconnect the player during/after verification
   * using our custom Disconnect packet.
   *
   * @param reason      Disconnect message component
   */
  void disconnect(final @NotNull Component reason);

  /**
   * Takes over the channel and begins the verification process
   *
   * @param username Username of the player
   * @param uuid     UUID of the player
   * @param encoder  Name of the encoder pipeline
   * @param decoder  Name of the decoder pipeline
   * @param timeout  Name of the read timeout pipeline
   * @param handler  Name of the main pipeline
   */
  void hijack(final @NotNull String username, final @NotNull UUID uuid,
              final @NotNull String encoder, final @NotNull String decoder,
              final @NotNull String timeout, final @NotNull String handler);

  /**
   * Sends a packet/message to the player
   *
   * @param msg Message to send to the player
   */
  default void write(final @NotNull Object msg) {
    if (getChannel().isActive()) {
      getChannel().writeAndFlush(msg, getChannel().voidPromise());
    } else {
      ReferenceCountUtil.release(msg);
    }
  }

  /**
   * Queues a buffered message that will be
   * sent once all messages are flushed.
   */
  default void delayedWrite(final @NotNull Object msg) {
    if (getChannel().isActive()) {
      getChannel().write(msg, getChannel().voidPromise());
    } else {
      ReferenceCountUtil.release(msg);
    }
  }

  /**
   * Disconnects the player who failed the verification
   * and caches them in FAILED_VERIFICATIONS.
   * If the player fails the verification twice,
   * the player will be temporarily denied from verifying.
   *
   * @param reason Reason for failing the verification
   * @apiNote The {@link xyz.jonesdev.sonar.api.event.impl.UserVerifyFailedEvent}
   * will not be thrown if no reason is given
   */
  void fail(final @NotNull String reason);
}
