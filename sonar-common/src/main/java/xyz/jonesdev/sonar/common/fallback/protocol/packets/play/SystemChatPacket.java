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

package xyz.jonesdev.sonar.common.fallback.protocol.packets.play;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.util.ComponentHolder;

import java.util.UUID;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class SystemChatPacket implements FallbackPacket {
  private static final int DIV_FLOOR = -Math.floorDiv(-20, 8);

  private ComponentHolder componentHolder;
  private String message;

  public SystemChatPacket(final @NotNull ComponentHolder componentHolder) {
    this.componentHolder = componentHolder;
  }

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    // Serialized message
    componentHolder.write(byteBuf, protocolVersion);

    // Type
    if (protocolVersion.compareTo(MINECRAFT_1_19_1) >= 0) {
      byteBuf.writeBoolean(false); // it's not the GAME_INFO type
    } else if (protocolVersion.compareTo(MINECRAFT_1_19) >= 0) {
      writeVarInt(byteBuf, 1); // system chat
    } else if (protocolVersion.compareTo(MINECRAFT_1_8) >= 0) {
      byteBuf.writeByte(1); // system chat
    }

    // Sender
    if (protocolVersion.compareTo(MINECRAFT_1_16) >= 0
      && protocolVersion.compareTo(MINECRAFT_1_19) < 0) {
      writeUUID(byteBuf, UUID.randomUUID());
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    message = readString(byteBuf, 256);

    if (protocolVersion.compareTo(MINECRAFT_1_19) >= 0) {
      if (protocolVersion.compareTo(MINECRAFT_1_19_1) <= 0) {
        byteBuf.readLong(); // expiresAt
        final long saltLong = byteBuf.readLong();
        final byte[] signatureBytes = readByteArray(byteBuf);
        boolean unsigned = false;

        if (saltLong != 0L && signatureBytes.length > 0) {
          // No need to store the valid signature
        } else if ((protocolVersion.compareTo(MINECRAFT_1_19_1) >= 0
          || saltLong == 0L) && signatureBytes.length == 0) {
          unsigned = true;
        } else {
          throw new CorruptedFrameException("Invalid signature");
        }

        final boolean signedPreview = byteBuf.readBoolean();
        if (signedPreview && unsigned) {
          throw new CorruptedFrameException("Signature missing");
        }

        if (protocolVersion.compareTo(MINECRAFT_1_19_1) >= 0) {
          final int size = readVarInt(byteBuf);
          if (size < 0 || size > 5) {
            throw new CorruptedFrameException("Invalid previous messages");
          }

          for (int i = 0; i < size; i++) {
            readUUID(byteBuf);
            readByteArray(byteBuf);
          }

          if (byteBuf.readBoolean()) {
            readUUID(byteBuf);
            readByteArray(byteBuf);
          }
        }
      } else {
        byteBuf.readLong(); // timestamp
        byteBuf.readLong(); // salt
        final boolean signed = byteBuf.readBoolean();

        if (signed) {
          final byte[] sign = new byte[256];
          byteBuf.readBytes(sign);
        }

        readVarInt(byteBuf);
        final byte[] bytes = new byte[DIV_FLOOR];
        byteBuf.readBytes(bytes);
      }
    }
  }
}
