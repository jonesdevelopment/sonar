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

package xyz.jonesdev.sonar.common.fallback.protocol.packets;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.protocol.ProtocolUtil;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class Disconnect implements FallbackPacket {
  private @Nullable String reason;

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    reason = ProtocolUtil.readString(byteBuf);
  }

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    if (reason == null) {
      throw new IllegalStateException("No reason specified");
    }

    ProtocolUtil.writeString(byteBuf, reason);
  }

  public static Disconnect create(final String serialized) {
    return new Disconnect(serialized);
  }
}
