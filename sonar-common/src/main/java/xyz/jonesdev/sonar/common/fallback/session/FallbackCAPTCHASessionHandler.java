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

package xyz.jonesdev.sonar.common.fallback.session;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.captcha.ItemType;
import xyz.jonesdev.sonar.common.fallback.protocol.captcha.MapCaptchaInfo;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.PlayerPositionLookPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.PlayerPositionPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.SetSlotPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.UniversalChatPacket;

import java.util.UUID;

import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

public final class FallbackCAPTCHASessionHandler extends FallbackSessionHandler {

  public FallbackCAPTCHASessionHandler(final FallbackUser user, final String username, final UUID uuid) {
    super(user, username, uuid);

    this.tries = Sonar.get().getConfig().getVerification().getMap().getMaxTries();

    // Set slot to map
    user.delayedWrite(new SetSlotPacket(36, 1,
      ItemType.FILLED_MAP.getId(user.getProtocolVersion()), SetSlotPacket.MAP_NBT));
    // Send random captcha to the player
    final MapCaptchaInfo captcha = MAP_INFO_PREPARER.getRandomCaptcha();
    this.answer = captcha.getAnswer();
    captcha.delayedWrite(user);
    // Teleport the player to the position above the platform
    user.delayedWrite(CAPTCHA_POSITION);
    // Make sure the player cannot move
    user.delayedWrite(user.isGeyser() ? CAPTCHA_ABILITIES_BEDROCK : CAPTCHA_ABILITIES);
    // Make sure the player knows what to do
    user.delayedWrite(enterCodeMessage);
    // Send all packets in one flush
    user.getChannel().flush();
  }

  private final String answer;
  private int tries, lastCountdownIndex, keepAliveStreak;

  @Override
  public void handle(final @NotNull FallbackPacket packet) {
    // Check if the player took too long to enter the captcha
    final int maxDuration = Sonar.get().getConfig().getVerification().getMap().getMaxDuration();
    checkState(!user.getLoginTimer().elapsed(maxDuration), "took too long to enter captcha");

    // Handle incoming chat messages
    if (packet instanceof UniversalChatPacket) {
      final UniversalChatPacket chat = (UniversalChatPacket) packet;

      // Captcha is correct, finish verification
      if (chat.getMessage().equals(answer)) {
        finishVerification();
        return;
      }

      // Captcha is incorrect, remove one try
      checkState(tries-- > 0, "failed captcha too often");
      user.write(incorrectCaptcha);
    } else if (packet instanceof PlayerPositionPacket
      || packet instanceof PlayerPositionLookPacket) {
      // A position packet is sent approximately every second
      final long difference = maxDuration - user.getLoginTimer().delay();
      final int index = (int) (difference / 1000D);
      // Make sure we can actually safely get and send the packet
      if (lastCountdownIndex != index && index >= 0 && xpCountdown.length > index) {
        // Send the countdown using the experience bar
        user.write(xpCountdown[index]);
      }
      lastCountdownIndex = index;
      // Send a KeepAlive packet every few seconds
      if (keepAliveStreak++ > 20) {
        keepAliveStreak = 0;
        // Send a KeepAlive packet to prevent timeout
        user.write(CAPTCHA_KEEP_ALIVE);
      }
    }
  }
}
