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
import org.jetbrains.annotations.Nullable;
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
public final class AttackTracker implements JVMProfiler {
  public static final AttackTracker INSTANCE = new AttackTracker();
  private @Nullable AttackStatistics currentAttack;
  private int attackThreshold;

  @Getter
  @ToString
  @RequiredArgsConstructor
  public static final class AttackStatistics {
    private final SystemTimer duration = new SystemTimer();
    private final SystemTimer timer = new SystemTimer();
    private int peakJoinsPerSecond;
    private double peakProcessCPUUsage;
    private long peakProcessMemoryUsage;
  }

  public void checkIfUnderAttack() {
    final int joinsPerSecond = Sonar.get().getVerboseHandler().getJoinsPerSecond().estimatedSize();
    final int verifyingPlayers = Sonar.get().getFallback().getConnected().size();
    final int queuedPlayers = Sonar.get().getFallback().getQueue().getQueuedPlayers().size();
    final int minPlayers = Sonar.get().getConfig().getMinPlayersForAttack();

    if (joinsPerSecond > minPlayers // Check the number of bots/joins per second.
      || verifyingPlayers > minPlayers // Check the number of verifying players.
      || queuedPlayers > minPlayers) { // Check the number of queued players.
      // Increment attack threshold
      ++attackThreshold;

      // An attack has been detected
      if (currentAttack == null) {
        currentAttack = new AttackStatistics();
        Sonar.get().getEventManager().publish(new AttackDetectedEvent());
      } else {
        // Reset attack timer if we're still under attack
        currentAttack.timer.reset();
      }

      // Update attack statistics
      if (joinsPerSecond > currentAttack.peakJoinsPerSecond) {
        // Update joins per second peak if necessary
        currentAttack.peakJoinsPerSecond = joinsPerSecond;
      }
      final double processCPUUsage = getProcessCPUUsage();
      if (processCPUUsage > currentAttack.peakProcessCPUUsage) {
        // Update cpu usage peak if necessary
        currentAttack.peakProcessCPUUsage = processCPUUsage;
      }
      final long processMemoryUsage = getUsedMemory();
      if (processMemoryUsage > currentAttack.peakProcessMemoryUsage) {
        // Update memory consumption peak if necessary
        currentAttack.peakProcessMemoryUsage = processMemoryUsage;
      }
    } else if (currentAttack != null) {
      if (currentAttack.duration.delay() > Sonar.get().getConfig().getMinAttackDuration()
        && currentAttack.timer.delay() > Sonar.get().getConfig().getAttackCooldownDelay()) {
        // The current attack has stopped
        Sonar.get().getEventManager().publish(new AttackMitigatedEvent(currentAttack));

        if (++attackThreshold > Sonar.get().getConfig().getMinAttackThreshold()) {
          // Save attack statistics
          final long deltaInMillis = currentAttack.duration.delay();
          final String peakCPU = Sonar.DECIMAL_FORMAT.format(currentAttack.peakProcessCPUUsage);
          final String peakMem = formatMemory(currentAttack.peakProcessMemoryUsage);
          final String peakBPS = Sonar.DECIMAL_FORMAT.format(currentAttack.peakJoinsPerSecond);
          final long minutes = deltaInMillis / (60 * 1000); // Convert milliseconds to minutes
          final double seconds = (deltaInMillis % (60 * 1000)) / 1000D; // Convert remaining milliseconds to seconds
          final String formattedDuration = String.format("%d minutes, %.0f seconds", minutes, seconds);
          final String startTimestamp = String.valueOf(currentAttack.duration.getStart() / 1000L);
          final String endTimestamp = String.valueOf(System.currentTimeMillis() / 1000L);
          final long blacklisted = Sonar.get().getFallback().getBlacklisted().estimatedSize();
          final long verified = Sonar.get().getVerifiedPlayerController().estimatedSize();
          final long failed = Statistics.FAILED_VERIFICATIONS.get();

          // Post webhook to Discord
          Optional.ofNullable(Sonar.get().getConfig().getDiscordWebhook()).ifPresent(webhook -> webhook.post(() -> {
            final SonarConfiguration.Webhook.Embed config = Sonar.get().getConfig().getWebhook().getEmbed();
            final String description = config.getDescription()
              .replace("%start-timestamp%", startTimestamp)
              .replace("%end-timestamp%", endTimestamp)
              .replace("%duration%", formattedDuration)
              .replace("%peak-cpu%", peakCPU)
              .replace("%peak-memory%", peakMem)
              .replace("%peak-bps%", peakBPS)
              .replace("%total-blacklisted%", Sonar.DECIMAL_FORMAT.format(blacklisted))
              .replace("%total-failed%", Sonar.DECIMAL_FORMAT.format(failed))
              .replace("%total-success%", Sonar.DECIMAL_FORMAT.format(verified));
            return new SonarConfiguration.Webhook.Embed(
              config.getTitle(), config.getTitleUrl(), description, config.getR(), config.getG(), config.getB());
          }));
        }

        // Reset the attack status
        currentAttack = null;
        // Reset threshold
        attackThreshold = 0;
      }
    } else {
      // Reset threshold
      attackThreshold = 0;
    }
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean isCurrentlyUnderAttack() {
    final int minDelay = Sonar.get().getConfig().getAttackCooldownDelay();
    return currentAttack != null && currentAttack.timer.delay() < minDelay;
  }
}
