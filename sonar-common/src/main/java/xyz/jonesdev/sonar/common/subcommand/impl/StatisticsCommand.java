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

package xyz.jonesdev.sonar.common.subcommand.impl;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
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
  protected void execute(final @NotNull CommandInvocation invocation) {
    StatisticType type = StatisticType.GENERAL;
    if (invocation.getRawArguments().length >= 2) {
      try {
        type = StatisticType.valueOf(invocation.getRawArguments()[1].toUpperCase());
      } catch (Exception exception) {
        invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getUnknownStatisticType()
          .replace("%statistics%", getArguments()));
        return;
      }
    }

    invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getStatisticsHeader()
      .replace("%type%", type.name().toLowerCase()));
    invocation.getSender().sendMessage();

    switch (type) {
      case GENERAL: {
        final long seconds = Sonar.get().getLaunchTimer().delay() / 1000L;
        final long days = seconds / (24L * 60L * 60L);
        final long hours = (seconds % (24L * 60L * 60L)) / (60L * 60L);
        final long minutes = (seconds % (60L * 60L)) / 60L;
        invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getGeneralStatistics()
          .replace("%verified%", DECIMAL_FORMAT.format(SONAR.getVerifiedPlayerController().estimatedSize()))
          .replace("%verifying%", DECIMAL_FORMAT.format(SONAR.getFallback().getConnected().size()))
          .replace("%blacklisted%", DECIMAL_FORMAT.format(SONAR.getFallback().getBlacklist().estimatedSize()))
          .replace("%queued%", DECIMAL_FORMAT.format(SONAR.getFallback().getQueue().getQueuedPlayers().size()))
          .replace("%uptime%", String.format("%dd %dh %dm %ds", days, hours, minutes, seconds % 60L))
          .replace("%total_joins%", DECIMAL_FORMAT.format(Statistics.TOTAL_TRAFFIC.get()))
          .replace("%total_attempts%", DECIMAL_FORMAT.format(Statistics.REAL_TRAFFIC.get()))
          .replace("%total_failed%", DECIMAL_FORMAT.format(Statistics.FAILED_VERIFICATIONS.get())));
        break;
      }

      case CPU: {
        invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getCpuStatistics()
          .replace("%process_cpu%", DECIMAL_FORMAT.format(getProcessCPUUsage()))
          .replace("%system_cpu%", DECIMAL_FORMAT.format(getSystemCPUUsage()))
          .replace("%average_process_cpu%", DECIMAL_FORMAT.format(getAverageProcessCPUUsage()))
          .replace("%average_system_cpu%", DECIMAL_FORMAT.format(getAverageSystemCPUUsage()))
          .replace("%load_average%", DECIMAL_FORMAT.format(getSystemLoadAverage()))
          .replace("%virtual_cores%", DECIMAL_FORMAT.format(getVirtualCores())));
        break;
      }

      case MEMORY: {
        invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getMemoryStatistics()
          .replace("%free_memory%", formatMemory(getFreeMemory()))
          .replace("%used_memory%", formatMemory(getUsedMemory()))
          .replace("%max_memory%", formatMemory(getMaxMemory()))
          .replace("%total_memory%", formatMemory(getTotalMemory())));
        break;
      }

      case NETWORK: {
        invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getNetworkStatistics()
          .replace("%incoming%", TrafficCounter.INCOMING.getCachedSecond())
          .replace("%outgoing%", TrafficCounter.OUTGOING.getCachedSecond())
          .replace("%ttl_incoming%", TrafficCounter.INCOMING.getCachedTtl())
          .replace("%ttl_outgoing%", TrafficCounter.OUTGOING.getCachedTtl()));
        break;
      }
    }
  }
}
