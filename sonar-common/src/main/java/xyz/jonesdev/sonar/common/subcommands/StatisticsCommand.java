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

package xyz.jonesdev.sonar.common.subcommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;
import xyz.jonesdev.sonar.common.statistics.CachedBandwidthStatistics;

import static xyz.jonesdev.sonar.api.Sonar.DECIMAL_FORMAT;
import static xyz.jonesdev.sonar.api.jvm.JVMProcessInformation.*;

@SubcommandInfo(
  name = "statistics",
  aliases = {"stats"},
  arguments = {"network", "memory", "cpu"},
  argumentsRequired = false
)
public final class StatisticsCommand extends Subcommand {

  @Override
  protected void execute(final @NotNull CommandInvocation invocation) {
    String type = "general";
    if (invocation.getRawArguments().length >= 2) {
      try {
        type = invocation.getRawArguments()[1].toLowerCase();
      } catch (Exception exception) {
        invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get().getConfig().getMessagesConfig().getString("commands.statistics.unknown-type"),
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
          Placeholder.unparsed("statistics", getArguments())));
        return;
      }
    }

    invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
      Sonar.get().getConfig().getMessagesConfig().getString("commands.statistics.header"),
      Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
      Placeholder.unparsed("statistics-type", type)));
    invocation.getSource().sendMessage(Component.empty());

    TagResolver.@NotNull Single[] placeholders = null;

    switch (type) {
      case "general": {
        final long seconds = Sonar.get().getLaunchTimer().delay() / 1000L;
        final long days = seconds / (24L * 60L * 60L);
        final long hours = (seconds % (24L * 60L * 60L)) / (60L * 60L);
        final long minutes = (seconds % (60L * 60L)) / 60L;

        placeholders = new TagResolver.Single[]{
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
          Placeholder.unparsed("verified", DECIMAL_FORMAT.format(Sonar.get().getVerifiedPlayerController().estimatedSize())),
          Placeholder.unparsed("verifying", DECIMAL_FORMAT.format(Sonar.get().getFallback().getConnected().size())),
          Placeholder.unparsed("blacklisted", DECIMAL_FORMAT.format(Sonar.get().getFallback().getBlacklist().estimatedSize())),
          Placeholder.unparsed("queued", DECIMAL_FORMAT.format(Sonar.get().getFallback().getQueue().getPlayers().size())),
          Placeholder.unparsed("server-uptime", String.format("%dd %dh %dm %ds", days, hours, minutes, seconds % 60L)),
          Placeholder.unparsed("total-joins", DECIMAL_FORMAT.format(Sonar.get().getStatistics().getTotalPlayersJoined())),
          Placeholder.unparsed("total-attempts", DECIMAL_FORMAT.format(Sonar.get().getStatistics().getTotalAttemptedVerifications())),
          Placeholder.unparsed("total-failed", DECIMAL_FORMAT.format(Sonar.get().getStatistics().getTotalFailedVerifications()))
        };
        break;
      }

      case "cpu": {
        placeholders = new TagResolver.Single[]{
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
          Placeholder.unparsed("process-cpu", DECIMAL_FORMAT.format(getProcessCPUUsage())),
          Placeholder.unparsed("system-cpu", DECIMAL_FORMAT.format(getSystemCPUUsage())),
          Placeholder.unparsed("average-process-cpu", DECIMAL_FORMAT.format(getAverageProcessCPUUsage())),
          Placeholder.unparsed("average-system-cpu", DECIMAL_FORMAT.format(getAverageSystemCPUUsage())),
          Placeholder.unparsed("virtual-core-count", DECIMAL_FORMAT.format(getVirtualCores()))
        };
        break;
      }

      case "memory": {
        placeholders = new TagResolver.Single[]{
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
          Placeholder.unparsed("free-memory", formatMemory(getFreeMemory())),
          Placeholder.unparsed("used-memory", formatMemory(getUsedMemory())),
          Placeholder.unparsed("max-memory", formatMemory(getMaxMemory())),
          Placeholder.unparsed("total-memory", formatMemory(getTotalMemory()))
        };
        break;
      }

      case "network": {
        placeholders = new TagResolver.Single[]{
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
          Placeholder.unparsed("incoming-traffic", CachedBandwidthStatistics.INCOMING.getCachedSecond()),
          Placeholder.unparsed("outgoing-traffic", CachedBandwidthStatistics.OUTGOING.getCachedSecond()),
          Placeholder.unparsed("incoming-traffic-ttl", CachedBandwidthStatistics.INCOMING.getCachedTtl()),
          Placeholder.unparsed("outgoing-traffic-ttl", CachedBandwidthStatistics.OUTGOING.getCachedTtl())
        };
        break;
      }
    }

    for (final String msg : Sonar.get().getConfig().getMessagesConfig().getStringList("commands.statistics." + type)) {
      invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(msg, placeholders));
    }
  }
}
