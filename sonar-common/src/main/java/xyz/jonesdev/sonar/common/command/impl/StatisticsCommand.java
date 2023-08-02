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

package xyz.jonesdev.sonar.common.command.impl;

import com.sun.management.OperatingSystemMXBean;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.argument.Argument;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;
import xyz.jonesdev.sonar.api.format.MemoryFormatter;
import xyz.jonesdev.sonar.api.statistics.Statistics;
import xyz.jonesdev.sonar.common.fallback.traffic.TrafficCounter;

import java.lang.management.ManagementFactory;

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
public final class StatisticsCommand extends Subcommand {
  private static final Runtime RUNTIME = Runtime.getRuntime();
  private static final OperatingSystemMXBean OPERATING_SYSTEM_MX_BEAN =
    (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

  @Override
  public void execute(final @NotNull CommandInvocation invocation) {
    invocation.getSender().sendMessage();
    invocation.getSender().sendMessage(" §eStatistics (this session)");
    invocation.getSender().sendMessage();

    if (invocation.getArguments().length >= 2) {
      switch (invocation.getArguments()[1].toLowerCase()) {
        case "network": {
          invocation.getSender().sendMessage(" §a▪ §7Current incoming used bandwidth: §f" + TrafficCounter.INCOMING.getCachedSecond());
          invocation.getSender().sendMessage(" §a▪ §7Current outgoing used bandwidth: §f" + TrafficCounter.OUTGOING.getCachedSecond());
          invocation.getSender().sendMessage(" §a▪ §7Total incoming used bandwidth: §f" + TrafficCounter.INCOMING.getCachedTtl());
          invocation.getSender().sendMessage(" §a▪ §7Total outgoing used bandwidth: §f" + TrafficCounter.OUTGOING.getCachedTtl());
          invocation.getSender().sendMessage();
          return;
        }

        case "memory": {
          final long freeMemory = RUNTIME.freeMemory();
          final long totalMemory = RUNTIME.totalMemory();
          final long maxMemory = RUNTIME.maxMemory();
          final long usedMemory = totalMemory - freeMemory;
          invocation.getSender().sendMessage(" §a▪ §7Total free memory (JVM): §f" + MemoryFormatter.formatMemory(freeMemory));
          invocation.getSender().sendMessage(" §a▪ §7Total used memory (JVM): §f" + MemoryFormatter.formatMemory(usedMemory));
          invocation.getSender().sendMessage(" §a▪ §7Total maximum memory (JVM): §f" + MemoryFormatter.formatMemory(maxMemory));
          invocation.getSender().sendMessage(" §a▪ §7Total allocated memory (JVM): §f" + MemoryFormatter.formatMemory(totalMemory));
          invocation.getSender().sendMessage();
          return;
        }

        case "cpu": {
          final int virtualCores = RUNTIME.availableProcessors();
          final double process = OPERATING_SYSTEM_MX_BEAN.getProcessCpuLoad() * 100;
          final double system = OPERATING_SYSTEM_MX_BEAN.getSystemCpuLoad() * 100;
          final double averageP = process / virtualCores;
          final double averageS = system / virtualCores;
          final double loadAverage = OPERATING_SYSTEM_MX_BEAN.getSystemLoadAverage() * 100;
          invocation.getSender().sendMessage(" §a▪ §7Process CPU usage right now: §f" + DECIMAL_FORMAT.format(process) + "%");
          invocation.getSender().sendMessage(" §a▪ §7System CPU usage right now: §f" + DECIMAL_FORMAT.format(system) + "%");
          invocation.getSender().sendMessage(" §a▪ §7Per-core process CPU usage: §f" + DECIMAL_FORMAT.format(averageP) + "%");
          invocation.getSender().sendMessage(" §a▪ §7Per-core system CPU usage: §f" + DECIMAL_FORMAT.format(averageS) + "%");
          invocation.getSender().sendMessage(" §a▪ §7General system load average: §f" + DECIMAL_FORMAT.format(loadAverage) + "%");
          invocation.getSender().sendMessage(" §a▪ §7Total virtual cpu cores (JVM): §f" + virtualCores);
          invocation.getSender().sendMessage();
          return;
        }

        default: {
          break;
        }
      }
    }

    final int total = Statistics.TOTAL_TRAFFIC.get(0);
    final int real = Statistics.REAL_TRAFFIC.get(0);
    final int failed = Statistics.FAILED_VERIFICATIONS.get(0);
    final int queued = SONAR.getFallback().getQueue().getQueuedPlayers().size();
    final int verifying = SONAR.getFallback().getConnected().size();
    final int verified = SONAR.getFallback().getVerified().size();
    final int blacklisted = SONAR.getFallback().getBlacklisted().estimatedSize();

    invocation.getSender().sendMessage(" §a▪ §7Verified IP addresses: §f" + DECIMAL_FORMAT.format(verified));
    invocation.getSender().sendMessage(" §a▪ §7Verifying IP addresses: §f" + DECIMAL_FORMAT.format(verifying));
    invocation.getSender().sendMessage(" §a▪ §7Blacklisted IP addresses: §f" + DECIMAL_FORMAT.format(blacklisted));
    invocation.getSender().sendMessage(" §a▪ §7Currently queued logins: §f" + DECIMAL_FORMAT.format(queued));
    invocation.getSender().sendMessage(" §a▪ §7Total non-unique joins: §f" + DECIMAL_FORMAT.format(total));
    invocation.getSender().sendMessage(" §a▪ §7Total verification attempts: §f" + DECIMAL_FORMAT.format(real));
    invocation.getSender().sendMessage(" §a▪ §7Total failed verifications: §f" + DECIMAL_FORMAT.format(failed));
    invocation.getSender().sendMessage();
  }
}
