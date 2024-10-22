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
public final class EntityAnimationPacket implements FallbackPacket {
  private int entityId;
  private Type type;

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) throws Exception {
    ProtocolUtil.writeVarInt(byteBuf, entityId);
    byteBuf.writeByte(type.ordinal());
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) throws Exception {
    throw new UnsupportedOperationException();
  }

  public enum Type {
    SWING_MAIN_ARM,
    HURT,
    WAKE_UP,
    // 1.9+?
    SWING_OFF_HAND, // Eat food on 1.7
    CRITICAL_HIT,
    MAGIC_CRITICAL_HIT
    // unknown (102), crouch (104), uncrouch(105) only exist on 1.7 and unused here
  }
}
