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

package xyz.jonesdev.sonar.common.fallback.verification;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.common.fallback.protocol.CaptchaPreparer;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.map.MapCaptchaInfo;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.SetPlayerPositionPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.SetPlayerPositionRotationPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.SystemChatPacket;
import xyz.jonesdev.sonar.common.util.exception.QuietDecoderException;

import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

public final class FallbackCaptchaHandler extends FallbackVerificationHandler {

  public FallbackCaptchaHandler(final @NotNull FallbackUser user) {
    super(user);

    // Disconnect the player if there is no CAPTCHA available at the moment
    if (!CaptchaPreparer.isCaptchaAvailable()) {
      user.disconnect(Sonar.get0().getConfig().getVerification().getCurrentlyPreparing());
      throw QuietDecoderException.INSTANCE;
    }

    this.tries = Sonar.get0().getConfig().getVerification().getMap().getMaxTries();

    // We may not be on slot 4, so we need to make sure to synchronize the current slot
    user.delayedWrite(CAPTCHA_HELD_ITEM_SLOT);
    user.delayedWrite(CAPTCHA_SET_CONTAINER_SLOT);
    // Send random captcha to the player
    final MapCaptchaInfo captcha = CaptchaPreparer.getRandomCaptcha();
    this.answer = captcha.getAnswer().toLowerCase();
    captcha.delayedWrite(user);
    // Teleport the player to the position above the platform
    user.delayedWrite(CAPTCHA_POSITION);
    // Make sure the player cannot move
    user.delayedWrite(user.isGeyser() ? NO_MOVE_ABILITIES_BEDROCK : NO_MOVE_ABILITIES);
    // Make sure the player knows that they have to enter the code in chat
    user.delayedWrite(enterCodeMessage);
    // Send all packets in one flush
    user.channel().flush();
  }

  private final String answer;
  private int tries, lastCountdownIndex, keepAliveStreak;

  @Override
  public void handle(final @NotNull FallbackPacket packet) {
    // Check if the player took too long to enter the CAPTCHA
    final int maxDuration = Sonar.get0().getConfig().getVerification().getMap().getMaxDuration();
    checkState(!user.getLoginTimer().elapsed(maxDuration), "took too long to enter CAPTCHA");

    if (packet instanceof SystemChatPacket) {
      final SystemChatPacket chat = (SystemChatPacket) packet;
      // Finish the verification if the player entered the correct code
      if (chat.getMessage().toLowerCase().equals(answer)) {
        finishVerification();
        return;
      }
      // Decrement the number of tries left
      checkState(tries-- > 0, "failed CAPTCHA too often");
      // Send the player a chat message to let them know that the code they entered is incorrect
      user.write(incorrectCaptcha[tries]);
    } else if (packet instanceof SetPlayerPositionPacket
      || packet instanceof SetPlayerPositionRotationPacket) {
      // A position packet is sent approximately every second
      if (Sonar.get0().getConfig().getVerification().getGamemode().isSurvivalOrAdventure()) {
        final long difference = maxDuration - user.getLoginTimer().delay();
        final int index = (int) (difference / 1000D);
        // Make sure we can safely get and send the packet
        if (lastCountdownIndex != index && index >= 0 && xpCountdown.length > index) {
          user.write(xpCountdown[index]);
        }
        lastCountdownIndex = index;
      }
      // Send a KeepAlive packet every few seconds
      if (keepAliveStreak++ > 20) {
        keepAliveStreak = 0;
        // Send a KeepAlive packet to prevent timeout
        user.write(RANDOM_KEEP_ALIVE);
      }
    }
  }
}
