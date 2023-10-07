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
          + "§cUnknown statistics type! Available statistics: §7" + getArguments());
        return;
      }
    }

    invocation.getSender().sendMessage("§eShowing " + type.name().toLowerCase() + " statistics for this session:");
    invocation.getSender().sendMessage("§ePlease note that session statistics are not saved when the server restarts.");
    invocation.getSender().sendMessage();

    switch (type) {
      case GENERAL: {
        invocation.getSender().sendMessage(" §7▪ §aVerified IP addresses: §f" + DECIMAL_FORMAT.format(SONAR.getVerifiedPlayerController().estimatedSize()));
        invocation.getSender().sendMessage(" §7▪ §aVerifying IP addresses: §f" + DECIMAL_FORMAT.format(SONAR.getFallback().getConnected().size()));
        invocation.getSender().sendMessage(" §7▪ §aBlacklisted IP addresses: §f" + DECIMAL_FORMAT.format(SONAR.getFallback().getBlacklisted().estimatedSize()));
        invocation.getSender().sendMessage(" §7▪ §aCurrently queued logins: §f" + DECIMAL_FORMAT.format(SONAR.getFallback().getQueue().getQueuedPlayers().size()));
        invocation.getSender().sendMessage(" §7▪ §aTotal non-unique joins: §f" + DECIMAL_FORMAT.format(Statistics.TOTAL_TRAFFIC.get()));
        invocation.getSender().sendMessage(" §7▪ §aTotal verification attempts: §f" + DECIMAL_FORMAT.format(Statistics.REAL_TRAFFIC.get()));
        invocation.getSender().sendMessage(" §7▪ §aTotal failed verifications: §f" + DECIMAL_FORMAT.format(Statistics.FAILED_VERIFICATIONS.get()));
        break;
      }

      case NETWORK: {
        invocation.getSender().sendMessage(" §7▪ §aCurrent incoming used bandwidth: §f" + TrafficCounter.INCOMING.getCachedSecond());
        invocation.getSender().sendMessage(" §7▪ §aCurrent outgoing used bandwidth: §f" + TrafficCounter.OUTGOING.getCachedSecond());
        invocation.getSender().sendMessage(" §7▪ §aTotal incoming used bandwidth: §f" + TrafficCounter.INCOMING.getCachedTtl());
        invocation.getSender().sendMessage(" §7▪ §aTotal outgoing used bandwidth: §f" + TrafficCounter.OUTGOING.getCachedTtl());
        break;
      }

      case MEMORY: {
        invocation.getSender().sendMessage(" §7▪ §aTotal free memory (JVM): §f" + formatMemory(getFreeMemory()));
        invocation.getSender().sendMessage(" §7▪ §aTotal used memory (JVM): §f" + formatMemory(getUsedMemory()));
        invocation.getSender().sendMessage(" §7▪ §aTotal maximum memory (JVM): §f" + formatMemory(getMaxMemory()));
        invocation.getSender().sendMessage(" §7▪ §aTotal allocated memory (JVM): §f" + formatMemory(getTotalMemory()));
        break;
      }

      case CPU: {
        invocation.getSender().sendMessage(" §7▪ §aProcess CPU usage right now: §f" + DECIMAL_FORMAT.format(getProcessCPUUsage()) + "%");
        invocation.getSender().sendMessage(" §7▪ §aSystem CPU usage right now: §f" + DECIMAL_FORMAT.format(getSystemCPUUsage()) + "%");
        invocation.getSender().sendMessage(" §7▪ §aPer-core process CPU usage: §f" + DECIMAL_FORMAT.format(getAverageProcessCPUUsage()) + "%");
        invocation.getSender().sendMessage(" §7▪ §aPer-core system CPU usage: §f" + DECIMAL_FORMAT.format(getAverageSystemCPUUsage()) + "%");
        invocation.getSender().sendMessage(" §7▪ §aGeneral system load average: §f" + DECIMAL_FORMAT.format(getSystemLoadAverage()) + "%");
        invocation.getSender().sendMessage(" §7▪ §aTotal virtual cpu cores (JVM): §f" + DECIMAL_FORMAT.format(getVirtualCores()));
        break;
      }
    }
  }
}
