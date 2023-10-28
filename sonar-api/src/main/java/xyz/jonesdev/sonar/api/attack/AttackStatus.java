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

package xyz.jonesdev.sonar.api.attack;

import lombok.*;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.event.impl.AttackDetectedEvent;
import xyz.jonesdev.sonar.api.event.impl.AttackMitigatedEvent;
import xyz.jonesdev.sonar.api.profiler.JVMProfiler;
import xyz.jonesdev.sonar.api.statistics.Statistics;
import xyz.jonesdev.sonar.api.timer.SystemTimer;

import java.util.Optional;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttackStatus implements JVMProfiler {
  public static final AttackStatus INSTANCE = new AttackStatus();
  private AttackStatistics currentAttack;
  private final SystemTimer lastAttack = new SystemTimer();

  @Getter
  @RequiredArgsConstructor
  @ToString(exclude = {"duration", "timer"})
  public static class AttackStatistics {
    private final SystemTimer duration = new SystemTimer();
    private final SystemTimer timer = new SystemTimer();
    private int peakJoinsPerSecond;
    private double peakProcessCPUUsage;
    private long peakProcessMemoryUsage;
  }

  public void checkIfUnderAttack() {
    final int joinsPerSecond = Sonar.get().getVerboseHandler().getJoinsPerSecond().estimatedSize();
    final int minPlayers = Sonar.get().getConfig().getMinPlayersForAttack();

    if (joinsPerSecond > minPlayers) {
      if (!lastAttack.elapsed(500L)) {
        // We wait a few milliseconds before we detect another attack
        // We do this to prevent spamming and other exploits
        return;
      }

      // An attack has been detected
      if (currentAttack == null) {
        currentAttack = new AttackStatistics();
        Optional.ofNullable(Sonar.get().getConfig().getDiscordWebhook()).ifPresent(webhook -> {
          webhook.post(() -> Sonar.get().getConfig().getWebhook().getAttackStartEmbed());
        });
        Sonar.get().getEventManager().publish(new AttackDetectedEvent());
      } else {
        // Reset attack timer if we're still under attack
        currentAttack.timer.reset();
      }

      // Update statistics
      final double processCPUUsage = getProcessCPUUsage();
      final long processMemoryUsage = getUsedMemory();

      if (joinsPerSecond > currentAttack.peakJoinsPerSecond) {
        currentAttack.peakJoinsPerSecond = joinsPerSecond;
      }
      if (processCPUUsage > currentAttack.peakProcessCPUUsage) {
        currentAttack.peakProcessCPUUsage = processCPUUsage;
      }
      if (processMemoryUsage > currentAttack.peakProcessMemoryUsage) {
        currentAttack.peakProcessMemoryUsage = processMemoryUsage;
      }
    } else if (currentAttack != null) {
      // An attack has stopped
      Sonar.get().getEventManager().publish(new AttackMitigatedEvent(currentAttack));
      // Post webhook to Discord
      Optional.ofNullable(Sonar.get().getConfig().getDiscordWebhook()).ifPresent(webhook -> {
        final long deltaInMillis = currentAttack.duration.delay();
        final String peakCPU = Sonar.DECIMAL_FORMAT.format(currentAttack.peakProcessCPUUsage);
        final String peakMem = formatMemory(currentAttack.peakProcessMemoryUsage);
        final String peakBPS = Sonar.DECIMAL_FORMAT.format(currentAttack.peakJoinsPerSecond);
        // Run the rest asynchronously
        webhook.post(() -> {
          final SonarConfiguration.Webhook.Embed embed = Sonar.get().getConfig().getWebhook().getAttackEndEmbed();
          final long minutes = deltaInMillis / (60 * 1000); // Convert milliseconds to minutes
          final long seconds = (deltaInMillis % (60 * 1000)) / 1000L; // Convert remaining milliseconds to seconds
          final long milliseconds = deltaInMillis % 1000; // Get remaining milliseconds
          final String formattedDuration = String.format("%d minutes, %d.%d seconds", minutes, seconds, milliseconds);
          embed.setDescription(embed.getDescription()
            .replace("%duration%", formattedDuration)
            .replace("%peak-cpu%", peakCPU)
            .replace("%peak-memory%", peakMem)
            .replace("%peak-bps%", peakBPS)
            .replace("%total-blacklisted%", Sonar.DECIMAL_FORMAT.format(Sonar.get().getFallback().getBlacklisted().estimatedSize()))
            .replace("%total-failed%", Sonar.DECIMAL_FORMAT.format(Statistics.FAILED_VERIFICATIONS.get()))
            .replace("%total-success%", Sonar.DECIMAL_FORMAT.format(Sonar.get().getVerifiedPlayerController().estimatedSize())));
          return embed;
        });
      });
      // Reset the attack status
      currentAttack = null;
      lastAttack.reset();
    }
  }

  @SuppressWarnings("all")
  public boolean isCurrentlyUnderAttack() {
    final int minDelay = Sonar.get().getConfig().getAttackCooldownDelay();
    return currentAttack != null && currentAttack.timer.delay() < minDelay;
  }
}
