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

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.event.impl.UserVerifySuccessEvent;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.api.model.VerifiedPlayer;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketListener;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.ClientInformationPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.PluginMessagePacket;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.fallback.FallbackUserWrapper.closeWith;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.transferToOrigin;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.BRAND_CHANNEL;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.BRAND_CHANNEL_LEGACY;

@RequiredArgsConstructor
public abstract class FallbackSessionHandler implements FallbackPacketListener {
  protected final FallbackUser user;
  protected final String username;
  protected final UUID uuid;

  protected static final Random RANDOM = new Random();

  protected void checkState(final boolean state, final String failReason) {
    // Fails the verification if the condition is not met
    if (!state) {
      // Let the API know that the user has failed the verification
      user.fail(failReason);
    }
  }

  protected final void finishVerification() {
    // Increment number of total successful verifications
    GlobalSonarStatistics.totalSuccessfulVerifications++;

    // Add verified player to the database
    Sonar.get().getVerifiedPlayerController().add(new VerifiedPlayer(
      user.getInetAddress(), uuid, user.getLoginTimer().getStart()));

    // Call the VerifySuccessEvent for external API usage
    Sonar.get().getEventManager().publish(new UserVerifySuccessEvent(
      username, uuid, user, user.getLoginTimer().delay()));

    // If enabled, transfer the player back to the origin server.
    // This feature was introduced by Mojang in Minecraft version 1.20.5.
    if (transferToOrigin != null
      && user.getProtocolVersion().compareTo(MINECRAFT_1_20_5) >= 0) {
      // Send the transfer packet to the player and close the channel
      closeWith(user.getChannel(), user.getProtocolVersion(), transferToOrigin);
    } else {
      // Disconnect player with the verification success message
      user.disconnect(Sonar.get().getConfig().getVerification().getVerificationSuccess());
    }

    Sonar.get().getFallback().getLogger().info(
      Sonar.get().getConfig().getMessagesConfig().getString("verification.logs.successful")
        .replace("<username>", username)
        .replace("<time-taken>", user.getLoginTimer().toString()));
  }

  protected final void checkClientInformation(final @NotNull ClientInformationPacket clientSettings) {
    // Ensure that the client locale is correct
    validateClientLocale(clientSettings.getLocale());
    // Check if the player sent an unused bit flag in the skin section
    checkState((clientSettings.getSkinParts() & 0x80) == 0, "sent unused bit flag");

    // Mark the ClientSettings packet as received
    user.setReceivedClientSettings(true);
  }

  protected final void checkPluginMessage(final @NotNull PluginMessagePacket pluginMessage) {
    final boolean usingModernChannel = pluginMessage.getChannel().equals(BRAND_CHANNEL);
    final boolean usingLegacyChannel = pluginMessage.getChannel().equals(BRAND_CHANNEL_LEGACY);

    // Skip this payload if it does not contain client brand information
    if (!usingModernChannel && !usingLegacyChannel) {
      return;
    }

    // Make sure the player isn't sending the client brand multiple times
    checkState(!user.isReceivedPluginMessage(), "sent duplicate plugin message");
    // Check if the channel is correct - 1.13 uses the new namespace
    // system ('minecraft:' + channel) and anything below 1.13 uses
    // the legacy namespace system ('MC|' + channel).
    final boolean v1_13 = user.getProtocolVersion().compareTo(MINECRAFT_1_13) >= 0;
    checkState(usingLegacyChannel || v1_13, "invalid brand channel");

    // Validate the client branding using a regex to filter unwanted characters.
    if (Sonar.get().getConfig().getVerification().getBrand().isEnabled()) {
      validateClientBrand(pluginMessage.getData());
    }

    // Mark the PluginMessage packet as received
    user.setReceivedPluginMessage(true);
  }

  protected final void validateClientBrand(final byte @NotNull [] data) {
    // No need to check for empty brands since ProtocolUtil#readBrandMessage
    // already performs invalid string checks by default.
    String read = new String(data, StandardCharsets.UTF_8);
    checkState(read.length() > 1, "client brand is too short");
    // Remove the invalid character at the beginning of the client brand
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_8) >= 0) {
      read = read.substring(1);
    }
    // Check if the decoded client brand string is too long
    checkState(read.length() < Sonar.get().getConfig().getVerification().getBrand().getMaxLength(),
      "client brand is too long: " + read.length());
    // Check for illegal client brands
    checkState(!read.equals("Vanilla"), "illegal client brand: " + read);
    // Regex pattern for validating client brands
    final Pattern pattern = Sonar.get().getConfig().getVerification().getBrand().getValidRegex();
    checkState(pattern.matcher(read).matches(), "client brand does not match pattern");
  }

  protected final void validateClientLocale(final @NotNull String locale) {
    // Check the client locale by performing a simple regex check
    // that disallows non-ascii characters by default.
    final Pattern pattern = Sonar.get().getConfig().getVerification().getValidLocaleRegex();
    checkState(pattern.matcher(locale).matches(), "client locale does not match pattern");
  }
}
