/*
 * Copyright (C) 2023, jones
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

package jones.sonar.velocity.verbose

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import jones.sonar.api.Sonar
import jones.sonar.api.verbose.Verbose
import jones.sonar.common.verbose.VerboseAnimation
import net.kyori.adventure.text.Component
import java.text.DecimalFormat

class ActionBarVerbose(private val server: ProxyServer) : Verbose {
  private val subscribers: MutableCollection<String> = ArrayList()

  override fun getSubscribers(): MutableCollection<String> {
    return subscribers
  }

  fun update() {
    val component: Component = Component.text(
      Sonar.get().config.ACTION_BAR_LAYOUT
        .replace("%queued%", decimalFormat.format(Sonar.get().fallback.queue.queuedPlayers.size))
        .replace("%verifying%", decimalFormat.format(Sonar.get().fallback.connected.size))
        .replace("%blacklisted%", decimalFormat.format(Sonar.get().fallback.blacklisted.size))
        .replace("%total%", decimalFormat.format(Sonar.get().statistics.get("total", 0)))
        .replace("%animation%", VerboseAnimation.nextState())
    )

    synchronized(subscribers) {
      for (subscriber in subscribers) {
        server.getPlayer(subscriber).ifPresent { player: Player ->
          player.sendActionBar(component)
        }
      }
    }
  }

  companion object {
    private val decimalFormat = DecimalFormat("#,###")
  }
}
