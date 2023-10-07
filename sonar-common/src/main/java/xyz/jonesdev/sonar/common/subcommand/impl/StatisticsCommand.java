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

package xyz.jonesdev.sonar.common.subcommand.impl;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;
import xyz.jonesdev.sonar.api.command.subcommand.argument.Argument;
import xyz.jonesdev.sonar.api.fallback.traffic.TrafficCounter;
import xyz.jonesdev.sonar.api.profiler.JVMProfiler;
import xyz.jonesdev.sonar.api.statistics.Statistics;

import static xyz.jonesdev.sonar.api.Sonar.DECIMAL_FORMAT;

@SubcommandInfo(
  name = "statistics",
  aliases = {"stats"},
  description = "Show session statistics of this server",
  arguments = {
    @Argument("network"),
    @Argument("memory"),
    @Argument("cpu")
  },
  argumentsRequired = false
)
public final class StatisticsCommand extends Subcommand implements JVMProfiler {
  private enum StatisticType {
    GENERAL,
    NETWORK,
    MEMORY,
    CPU
  }

  @Override
  public void execute(final @NotNull CommandInvocation invocation) {
    StatisticType type = StatisticType.GENERAL;
    if (invocation.getRawArguments().length >= 2) {
      try {
        type = StatisticType.valueOf(invocation.getRawArguments()[1].toUpperCase());
      } catch (Exception exception) {
        // TODO: make messages configurable/translatable
        invocation.getSender().sendMessage(SONAR.getConfig().getPrefix()
          + "<red>Unknown statistics type! Available statistics: <gray>" + getArguments());
        return;
      }
    }

    invocation.getSender().sendMessage("<yellow>Showing " + type.name().toLowerCase() + " statistics for this session:");
    invocation.getSender().sendMessage("<yellow>Please note that session statistics are not saved when the server restarts.");
    invocation.getSender().sendMessage();

    switch (type) {
      case GENERAL: {
        invocation.getSender().sendMessage(" <gray>▪ <green>Verified IP addresses: <white>" + DECIMAL_FORMAT.format(SONAR.getVerifiedPlayerController().estimatedSize()));
        invocation.getSender().sendMessage(" <gray>▪ <green>Verifying IP addresses: <white>" + DECIMAL_FORMAT.format(SONAR.getFallback().getConnected().size()));
        invocation.getSender().sendMessage(" <gray>▪ <green>Blacklisted IP addresses: <white>" + DECIMAL_FORMAT.format(SONAR.getFallback().getBlacklisted().estimatedSize()));
        invocation.getSender().sendMessage(" <gray>▪ <green>Currently queued logins: <white>" + DECIMAL_FORMAT.format(SONAR.getFallback().getQueue().getQueuedPlayers().size()));
        invocation.getSender().sendMessage(" <gray>▪ <green>Total non-unique joins: <white>" + DECIMAL_FORMAT.format(Statistics.TOTAL_TRAFFIC.get()));
        invocation.getSender().sendMessage(" <gray>▪ <green>Total verification attempts: <white>" + DECIMAL_FORMAT.format(Statistics.REAL_TRAFFIC.get()));
        invocation.getSender().sendMessage(" <gray>▪ <green>Total failed verifications: <white>" + DECIMAL_FORMAT.format(Statistics.FAILED_VERIFICATIONS.get()));
        break;
      }

      case NETWORK: {
        invocation.getSender().sendMessage(" <gray>▪ <green>Current incoming used bandwidth: <white>" + TrafficCounter.INCOMING.getCachedSecond());
        invocation.getSender().sendMessage(" <gray>▪ <green>Current outgoing used bandwidth: <white>" + TrafficCounter.OUTGOING.getCachedSecond());
        invocation.getSender().sendMessage(" <gray>▪ <green>Total incoming used bandwidth: <white>" + TrafficCounter.INCOMING.getCachedTtl());
        invocation.getSender().sendMessage(" <gray>▪ <green>Total outgoing used bandwidth: <white>" + TrafficCounter.OUTGOING.getCachedTtl());
        break;
      }

      case MEMORY: {
        invocation.getSender().sendMessage(" <gray>▪ <green>Total free memory (JVM): <white>" + formatMemory(getFreeMemory()));
        invocation.getSender().sendMessage(" <gray>▪ <green>Total used memory (JVM): <white>" + formatMemory(getUsedMemory()));
        invocation.getSender().sendMessage(" <gray>▪ <green>Total maximum memory (JVM): <white>" + formatMemory(getMaxMemory()));
        invocation.getSender().sendMessage(" <gray>▪ <green>Total allocated memory (JVM): <white>" + formatMemory(getTotalMemory()));
        break;
      }

      case CPU: {
        invocation.getSender().sendMessage(" <gray>▪ <green>Process CPU usage right now: <white>" + DECIMAL_FORMAT.format(getProcessCPUUsage()) + "%");
        invocation.getSender().sendMessage(" <gray>▪ <green>System CPU usage right now: <white>" + DECIMAL_FORMAT.format(getSystemCPUUsage()) + "%");
        invocation.getSender().sendMessage(" <gray>▪ <green>Per-core process CPU usage: <white>" + DECIMAL_FORMAT.format(getAverageProcessCPUUsage()) + "%");
        invocation.getSender().sendMessage(" <gray>▪ <green>Per-core system CPU usage: <white>" + DECIMAL_FORMAT.format(getAverageSystemCPUUsage()) + "%");
        invocation.getSender().sendMessage(" <gray>▪ <green>General system load average: <white>" + DECIMAL_FORMAT.format(getSystemLoadAverage()) + "%");
        invocation.getSender().sendMessage(" <gray>▪ <green>Total virtual cpu cores (JVM): <white>" + DECIMAL_FORMAT.format(getVirtualCores()));
        break;
      }
    }
  }
}
