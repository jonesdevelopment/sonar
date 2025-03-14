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

package xyz.jonesdev.sonar.api;

import io.netty.channel.ChannelPipeline;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@Getter
@RequiredArgsConstructor
public enum SonarPlatform {
  BUKKIT("Bukkit", 19110, "packet_handler",
    pipeline -> pipeline.context("outbound_config") != null ? "outbound_config" : "encoder"),
  BUNGEE("BungeeCord", 19109, "inbound-boss",
    pipeline -> "packet-encoder"),
  VELOCITY("Velocity", 19107, "handler",
    pipeline -> "minecraft-encoder");

  private final String displayName;
  private final int metricsId;
  private final String connectionHandler;
  private final Function<ChannelPipeline, String> encoder;
}
