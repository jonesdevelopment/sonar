/*
 * Copyright (C) 2024 Sonar Contributors
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

package xyz.jonesdev.sonar.common.fallback.protocol.packets.play;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class AnimationPacket implements FallbackPacket {
  private Hand hand = Hand.MAIN_HAND;
  private int entityId = -1;
  private LegacyAnimationType type = LegacyAnimationType.SWING_ARM;

  @Override
  public void encode(ByteBuf byteBuf, ProtocolVersion protocolVersion) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void decode(ByteBuf byteBuf, ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      this.entityId = byteBuf.readInt();
      this.type = LegacyAnimationType.getById(byteBuf.readByte());
    } else {
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) > 0) { // Only 1.9+ has offhand
        hand = Hand.values()[ProtocolUtil.readVarInt(byteBuf)];
      } else {
        hand = Hand.MAIN_HAND;
      }
    }
  }

  public enum Hand {
    MAIN_HAND,
    OFF_HAND
  }

  @Getter
  public enum LegacyAnimationType {
    NO_ANIMATION,
    SWING_ARM,
    DAMAGE_ANIMATION,
    LEAVE_BED,
    EAT_FOOD,
    CRITICAL_EFFECT,
    MAGIC_CRITICAL_EFFECT,
    UNKNOWN(102),
    CROUCH(104),
    @SuppressWarnings("SpellCheckingInspection")
    UNCROUCH(105);

    private final int id;

    LegacyAnimationType(int id) {
      this.id = id;
    }

    LegacyAnimationType() {
      this.id = ordinal();
    }

    public static LegacyAnimationType getById(int id) {
      if (id >= 0 && id <= 7) {
        return LegacyAnimationType.values()[id];
      }
      switch (id) {
        case 102:
          return UNKNOWN;
        case 104:
          return CROUCH;
        case 105:
          return UNCROUCH;
        default:
          throw new IllegalArgumentException("Unknown type with id: " + id);
      }
    }
  }
}
