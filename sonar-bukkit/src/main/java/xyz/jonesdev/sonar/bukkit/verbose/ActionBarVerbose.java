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

package xyz.jonesdev.sonar.bukkit.verbose;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.statistics.Statistics;
import xyz.jonesdev.sonar.api.verbose.Verbose;
import xyz.jonesdev.sonar.common.fallback.traffic.TrafficCounter;
import xyz.jonesdev.sonar.common.verbose.VerboseAnimation;

import java.util.ArrayList;
import java.util.Collection;

import static xyz.jonesdev.sonar.api.format.MemoryFormatter.formatMemory;

@RequiredArgsConstructor
public final class ActionBarVerbose implements Verbose {
  private final Server server;
  @Getter
  private final Collection<String> subscribers = new ArrayList<>();

  public void update() {
    // Clean up blacklisted IPs
    Sonar.get().getFallback().getBlacklisted().cleanUp(false);

    final TextComponent component = new TextComponent(
      Sonar.get().getConfig().ACTION_BAR_LAYOUT
        .replace("%queued%",
          Sonar.DECIMAL_FORMAT.format(Sonar.get().getFallback().getQueue().getQueuedPlayers().size()))
        .replace("%verifying%", Sonar.DECIMAL_FORMAT.format(Sonar.get().getFallback().getConnected().size()))
        .replace("%verified%", Sonar.DECIMAL_FORMAT.format(Sonar.get().getFallback().getVerified().size()))
        .replace("%blacklisted%",
          Sonar.DECIMAL_FORMAT.format(Sonar.get().getFallback().getBlacklisted().estimatedSize()))
        .replace("%total-joins%", Sonar.DECIMAL_FORMAT.format(Statistics.TOTAL_TRAFFIC.get(0)))
        .replace("%real-joins%", Sonar.DECIMAL_FORMAT.format(Statistics.REAL_TRAFFIC.get(0)))
        .replace("%failed-verify%", Sonar.DECIMAL_FORMAT.format(Statistics.FAILED_VERIFICATIONS.get(0)))
        .replace("%incoming-traffic%", TrafficCounter.INCOMING.getCachedSecond())
        .replace("%outgoing-traffic%", TrafficCounter.OUTGOING.getCachedSecond())
        .replace("%incoming-traffic-ttl%", TrafficCounter.INCOMING.getCachedTtl())
        .replace("%outgoing-traffic-ttl%", TrafficCounter.OUTGOING.getCachedTtl())
        .replace("%used-memory%", formatMemory(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()))
        .replace("%free-memory%", formatMemory(Runtime.getRuntime().freeMemory()))
        .replace("%total-memory%", formatMemory(Runtime.getRuntime().totalMemory()))
        .replace("%max-memory%", formatMemory(Runtime.getRuntime().maxMemory()))
        .replace("%animation%", VerboseAnimation.nextAnimation())
    );

    synchronized (subscribers) {
      for (final String subscriber : subscribers) {
        final Player player = server.getPlayer(subscriber);
        if (player != null) {
          //player.sendMessage(ChatMessageType.ACTION_BAR, component);
        }
      }
    }
  }
}
