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
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.utility.component.ComponentHolder;

import java.time.Instant;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.utility.protocol.ProtocolUtil.*;
import static xyz.jonesdev.sonar.common.utility.protocol.VarIntUtil.readVarInt;
import static xyz.jonesdev.sonar.common.utility.protocol.VarIntUtil.writeVarInt;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class UniversalChatPacket implements FallbackPacket {
  private static final int DIV_FLOOR = -Math.floorDiv(-20, 8);

  public static final byte CHAT_TYPE = (byte) 0;
  public static final byte SYSTEM_TYPE = (byte) 1;
  public static final byte GAME_INFO_TYPE = (byte) 2;

  private ComponentHolder componentHolder;
  private String message;
  private byte type;
  private boolean signedPreview;
  private boolean unsigned = false;
  private Instant timestamp;
  private Instant expiry;
  private long salt;
  private boolean signed;
  private byte[] signature;

  public UniversalChatPacket(final @NotNull Component component,
                             final byte type) {
    this(new ComponentHolder(component), type);
  }

  public UniversalChatPacket(final ComponentHolder componentHolder, final byte type) {
    this.componentHolder = componentHolder;
    this.type = type;
  }

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    // Serialized message
    componentHolder.write(byteBuf, protocolVersion);

    // Type
    if (protocolVersion.compareTo(MINECRAFT_1_19_1) >= 0) {
      byteBuf.writeBoolean(type == GAME_INFO_TYPE);
    } else if (protocolVersion.compareTo(MINECRAFT_1_19) >= 0) {
      writeVarInt(byteBuf, type);
    } else if (protocolVersion.compareTo(MINECRAFT_1_8) >= 0) {
      byteBuf.writeByte(type);
    }

    // Sender
    if (protocolVersion.compareTo(MINECRAFT_1_16) >= 0
      && protocolVersion.compareTo(MINECRAFT_1_19) < 0) {
      writeUUID(byteBuf, EMPTY_UUID);
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    message = readString(byteBuf, 256);

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19_1) <= 0) {
        final long expiresAt = byteBuf.readLong();
        final long saltLong = byteBuf.readLong();
        final byte[] signatureBytes = readByteArray(byteBuf);

        if (saltLong != 0L && signatureBytes.length > 0) {
          signature = signatureBytes;
          expiry = Instant.ofEpochMilli(expiresAt);
        } else if ((protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19_1) >= 0
          || saltLong == 0L) && signatureBytes.length == 0) {
          unsigned = true;
        } else {
          throw new CorruptedFrameException("Invalid signature");
        }

        signedPreview = byteBuf.readBoolean();
        if (signedPreview && unsigned) {
          throw new CorruptedFrameException("Signature missing");
        }

        if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19_1) >= 0) {
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
        timestamp = Instant.ofEpochMilli(byteBuf.readLong());
        salt = byteBuf.readLong();
        signed = byteBuf.readBoolean();
        if (signed) {
          byte[] sign = new byte[256];
          byteBuf.readBytes(sign);
          signature = sign;
        } else {
          signature = new byte[0];
        }

        readVarInt(byteBuf);
        byte[] bytes = new byte[DIV_FLOOR];
        byteBuf.readBytes(bytes);
      }
    }
  }
}
