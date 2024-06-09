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

package xyz.jonesdev.sonar.api.verbose;

import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.attack.AttackTracker;
import xyz.jonesdev.sonar.api.timer.SystemTimer;

import java.util.Collection;
import java.util.UUID;
import java.util.Vector;

import static xyz.jonesdev.sonar.api.timer.SystemTimer.DATE_FORMATTER;

@Getter
public final class Verbose implements Observable {
  private final @NotNull Collection<UUID> subscribers = new Vector<>(0);
  private int animationIndex;

  // Run action bar verbose
  public void update() {
    // Don't prepare component if there are no subscribers
    Component component = null;
    for (final UUID subscriber : subscribers) {
      final Audience audience = Sonar.get().audience(subscriber);
      if (audience == null) continue;

      // Only prepare component if there are subscribers
      if (component == null) {
        final AttackTracker.AttackStatistics attackStatistics = Sonar.get().getAttackTracker().getCurrentAttack();
        final SystemTimer attackDuration = attackStatistics == null ? null : attackStatistics.getDuration();
        component = replaceStatistic(attackDuration != null
          ? Sonar.get().getConfig().getVerbose().getActionBarLayoutDuringAttack()
          : Sonar.get().getConfig().getVerbose().getActionBarLayout())
          .replaceText(TextReplacementConfig.builder().once().matchLiteral("%attack-duration%")
            .replacement(attackDuration == null ? "00:00" : DATE_FORMATTER.format(attackDuration.delay()))
            .build())
          .replaceText(TextReplacementConfig.builder().once().matchLiteral("%animation%")
            .replacement(nextAnimation())
            .build());
      }
      // Send the action bar to all online subscribers
      audience.sendActionBar(component);
    }
  }

  public String nextAnimation() {
    final var animations = Sonar.get().getConfig().getVerbose().getAnimation();
    final int nextIndex = ++animationIndex % animations.size();
    return animations.toArray(new String[0])[nextIndex];
  }
}
