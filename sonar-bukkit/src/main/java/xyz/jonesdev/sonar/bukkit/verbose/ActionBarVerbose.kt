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

package xyz.jonesdev.sonar.bukkit.verbose

import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Server
import xyz.jonesdev.sonar.api.Sonar
import xyz.jonesdev.sonar.api.format.MemoryFormatter.formatMemory
import xyz.jonesdev.sonar.api.verbose.Verbose
import xyz.jonesdev.sonar.common.verbose.VerboseAnimation

class ActionBarVerbose(private val server: Server) : Verbose {
  private val subscribers: MutableCollection<String> = ArrayList()

  override fun getSubscribers(): MutableCollection<String> {
    return subscribers
  }

  fun update() {
    val component = TextComponent(
      Sonar.get().config.ACTION_BAR_LAYOUT
        .replace("%queued%", Sonar.DECIMAL_FORMAT.format(Sonar.get().fallback.queue.getQueuedPlayers().size))
        .replace("%verifying%", Sonar.DECIMAL_FORMAT.format(Sonar.get().fallback.connected.size))
        .replace("%verified%", Sonar.DECIMAL_FORMAT.format(Sonar.get().fallback.verified.size))
        .replace("%blacklisted%", Sonar.DECIMAL_FORMAT.format(Sonar.get().fallback.blacklisted.estimatedSize()))
        .replace("%total%", Sonar.DECIMAL_FORMAT.format(Sonar.get().statistics.get("total", 0)))
        .replace("%used-memory%", formatMemory(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()))
        .replace("%free-memory%", formatMemory(Runtime.getRuntime().freeMemory()))
        .replace("%total-memory%", formatMemory(Runtime.getRuntime().totalMemory()))
        .replace("%max-memory%", formatMemory(Runtime.getRuntime().maxMemory()))
        .replace("%animation%", VerboseAnimation.nextAnimation())
    )

    synchronized(subscribers) {
      for (subscriber in subscribers) {
        val player = server.getPlayer(subscriber) ?: continue
        // TODO: action bar sending
      }
    }
  }
}
