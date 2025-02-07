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

package xyz.jonesdev.sonar.common.subcommand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;

import java.util.List;

import static xyz.jonesdev.sonar.api.profiler.SimpleProcessProfiler.*;

@SubcommandInfo(
  name = "statistics",
  aliases = {"stats"},
  arguments = {"network", "memory", "cpu"},
  argumentsRequired = false
)
public final class StatisticsCommand extends Subcommand {

  @Override
  protected void execute(final @NotNull InvocationSource source, final String @NotNull [] args) {
    String type = "general";
    if (args.length >= 2) {
      try {
        type = args[1].toLowerCase();
      } catch (Exception exception) {
        source.sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get0().getConfig().getMessagesConfig().getString("commands.statistics.unknown-type"),
          Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
          Placeholder.unparsed("statistics", getArguments())));
        return;
      }
    }

    source.sendMessage(MiniMessage.miniMessage().deserialize(
      Sonar.get0().getConfig().getMessagesConfig().getString("commands.statistics.header"),
      Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
      Placeholder.unparsed("statistics-type", type)));
    source.sendMessage(Component.empty());

    TagResolver.@NotNull Single[] placeholders = null;

    switch (type) {
      case "general": {
        final long seconds = Sonar.get0().getLaunchTimer().delay() / 1000L;
        final long days = seconds / (24L * 60L * 60L);
        final long hours = (seconds % (24L * 60L * 60L)) / (60L * 60L);
        final long minutes = (seconds % (60L * 60L)) / 60L;
        final String serverUptime = String.format("%dd %dh %dm %ds", days, hours, minutes, seconds % 60L);

        placeholders = new TagResolver.Single[]{
          Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
          Placeholder.unparsed("verified", Sonar.DECIMAL_FORMAT.format(Sonar.get0().getVerifiedPlayerController().getCache().size())),
          Placeholder.unparsed("verifying", Sonar.DECIMAL_FORMAT.format(Sonar.get0().getFallback().getConnected().size())),
          Placeholder.unparsed("blacklisted", Sonar.DECIMAL_FORMAT.format(Sonar.get0().getFallback().getBlacklist().estimatedSize())),
          Placeholder.unparsed("queued", Sonar.DECIMAL_FORMAT.format(Sonar.get0().getFallback().getQueue().getPlayers().size())),
          Placeholder.unparsed("server-uptime", serverUptime),
          Placeholder.unparsed("total-joins", Sonar.DECIMAL_FORMAT.format(Sonar.get0().getStatistics().getTotalPlayersJoined())),
          Placeholder.unparsed("total-attempts", Sonar.DECIMAL_FORMAT.format(Sonar.get0().getStatistics().getTotalAttemptedVerifications())),
          Placeholder.unparsed("total-failed", Sonar.DECIMAL_FORMAT.format(Sonar.get0().getStatistics().getTotalFailedVerifications()))
        };
        break;
      }

      case "cpu": {
        placeholders = new TagResolver.Single[]{
          Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
          Placeholder.unparsed("process-cpu", Sonar.DECIMAL_FORMAT.format(getProcessCPUUsage())),
          Placeholder.unparsed("system-cpu", Sonar.DECIMAL_FORMAT.format(getSystemCPUUsage())),
          Placeholder.unparsed("average-process-cpu", Sonar.DECIMAL_FORMAT.format(getAverageProcessCPUUsage())),
          Placeholder.unparsed("average-system-cpu", Sonar.DECIMAL_FORMAT.format(getAverageSystemCPUUsage())),
          Placeholder.unparsed("virtual-core-count", Sonar.DECIMAL_FORMAT.format(getVirtualCores()))
        };
        break;
      }

      case "memory": {
        placeholders = new TagResolver.Single[]{
          Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
          Placeholder.unparsed("free-memory", formatMemory(getFreeMemory())),
          Placeholder.unparsed("used-memory", formatMemory(getUsedMemory())),
          Placeholder.unparsed("max-memory", formatMemory(getMaxMemory())),
          Placeholder.unparsed("total-memory", formatMemory(getTotalMemory()))
        };
        break;
      }

      case "network": {
        placeholders = new TagResolver.Single[]{
          Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
          Placeholder.unparsed("incoming-traffic", Sonar.get0().getStatistics().getPerSecondIncomingBandwidthFormatted()),
          Placeholder.unparsed("outgoing-traffic", Sonar.get0().getStatistics().getPerSecondOutgoingBandwidthFormatted()),
          Placeholder.unparsed("incoming-traffic-ttl", formatMemory(Sonar.get0().getStatistics().getTotalIncomingBandwidth())),
          Placeholder.unparsed("outgoing-traffic-ttl", formatMemory(Sonar.get0().getStatistics().getTotalOutgoingBandwidth())),
        };
        break;
      }
    }

    final List<String> parts = Sonar.get0().getConfig().getMessagesConfig().getStringList(
      "commands.statistics." + type);
    for (final String msg : parts) {
      source.sendMessage(MiniMessage.miniMessage().deserialize(msg, placeholders));
    }
  }
}
