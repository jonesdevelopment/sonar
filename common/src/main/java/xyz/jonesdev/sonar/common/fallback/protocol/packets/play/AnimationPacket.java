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

package xyz.jonesdev.sonar.common.fallback.protocol.packets.play;

import io.netty.buffer.ByteBuf;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"entityId", "hand"})
public final class AnimationPacket implements FallbackPacket {
  private int entityId = -1;
  private int hand = MAIN_HAND;
  private LegacyAnimationType type = LegacyAnimationType.SWING_ARM;

  public static final int MAIN_HAND = 0;

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void decode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_8)) {
      entityId = byteBuf.readInt();
      type = LegacyAnimationType.getById(byteBuf.readByte());
    } else if (protocolVersion.greaterThan(ProtocolVersion.MINECRAFT_1_8)) {
      // Only 1.9+ clients have an offhand
      hand = ProtocolUtil.readVarInt(byteBuf);
    }
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
