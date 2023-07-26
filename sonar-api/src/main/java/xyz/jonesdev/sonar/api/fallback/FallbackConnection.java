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

package xyz.jonesdev.sonar.api.fallback;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;

public interface FallbackConnection<X, Y> {
  @NotNull Fallback getFallback();

  @NotNull X getPlayer();

  @NotNull Y getConnection();

  @NotNull Channel getChannel();

  @NotNull ChannelPipeline getPipeline();

  @NotNull InetAddress getInetAddress();

  int getProtocolId();

  default void fail(final @Nullable String reason) {
    if (getChannel().isActive()) {
      getChannel().close();
    }

    if (reason != null) {
      getFallback().getLogger().info("{} ({}) has failed the bot check for: {}",
        getInetAddress(), getProtocolId(), reason);
    }
  }
}
