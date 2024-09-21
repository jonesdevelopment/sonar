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

package xyz.jonesdev.sonar.common.fallback.protocol.packets.handshake;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.ToString;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;

import static xyz.jonesdev.sonar.common.util.ProtocolUtil.readString;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.readVarInt;

@Getter
@ToString(of = {"protocolVersionId", "hostname", "port"})
public final class HandshakePacket implements FallbackPacket {
  private int protocolVersionId;
  private String hostname;
  private int port;
  private int intent;

  private static final String FORGE_TOKEN = "\0FML\0";
  private static final int MAXIMUM_HOSTNAME_LENGTH = 255 + FORGE_TOKEN.length() + 1;

  // https://wiki.vg/Protocol#Handshaking
  public static final int STATUS = 1;
  public static final int LOGIN = 2;
  public static final int TRANSFER = 3;

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    protocolVersionId = readVarInt(byteBuf);
    hostname = readString(byteBuf, MAXIMUM_HOSTNAME_LENGTH);
    port = byteBuf.readUnsignedShort();
    intent = readVarInt(byteBuf);
  }
}
