/*
 * Copyright (C) 2023 jones
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either protocolVersion.getProtocol() 3 of the License, or
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

package jones.sonar.velocity.fallback.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.CorruptedFrameException;
import lombok.Data;

import java.io.IOException;
import java.util.BitSet;

import static com.velocitypowered.api.network.ProtocolVersion.*;
import static com.velocitypowered.proxy.protocol.ProtocolUtils.writeVarInt;

// pasted from
// https://github.com/Leymooo/BungeeCord/blob/master/protocol/src/main/java/ru/leymooo/botfilter/packets/EmptyChunkPacket.java
@Data
public class EmptyChunkPacket implements MinecraftPacket {
  private void write1_14Heightmaps(final ByteBuf buf, final ProtocolVersion protocolVersion) {
    try (final ByteBufOutputStream output = new ByteBufOutputStream(buf)) {
      output.writeByte(10);
      output.writeUTF("");
      output.writeByte(10);
      output.writeUTF("root");
      output.writeByte(12);
      output.writeUTF("MOTION_BLOCKING");

      final long[] tag = new long[protocolVersion.getProtocol() < MINECRAFT_1_18.getProtocol() ? 36 : 37];
      output.writeInt(tag.length);

      for (long l : tag) {
        output.writeLong(l);
      }

      buf.writeByte(0);
      buf.writeByte(0);
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf,
                     final ProtocolUtils.Direction direction,
                     final ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(final ByteBuf byteBuf,
                     final ProtocolUtils.Direction direction,
                     final ProtocolVersion protocolVersion) {
    writeVarInt(byteBuf, 0x21);
    byteBuf.writeInt(0);
    byteBuf.writeInt(0);

    if (protocolVersion.getProtocol() < MINECRAFT_1_17.getProtocol()) {
      byteBuf.writeBoolean(true);
    }

    if (protocolVersion.getProtocol() >= MINECRAFT_1_16.getProtocol()
      && protocolVersion.getProtocol() < MINECRAFT_1_16_2.getProtocol()) {
      byteBuf.writeBoolean(true);
    }

    if (protocolVersion.getProtocol() < MINECRAFT_1_17.getProtocol()) {
      if (protocolVersion.getProtocol() == MINECRAFT_1_8.getProtocol()) {
        byteBuf.writeShort(1);
      } else {
        writeVarInt(byteBuf, 0);
      }
    } else if (protocolVersion.getProtocol() < MINECRAFT_1_18.getProtocol()) {
      final BitSet bitSet = new BitSet();

      for (int i = 0; i < 16; i++) {
        bitSet.set(i, false);
      }

      long[] mask = bitSet.toLongArray();
      writeVarInt(byteBuf, mask.length);

      for (long l : mask) {
        byteBuf.writeLong(l);
      }
    }

    if (protocolVersion.getProtocol() >= MINECRAFT_1_14.getProtocol()) {
      write1_14Heightmaps(byteBuf, protocolVersion);

      if (protocolVersion.getProtocol() >= MINECRAFT_1_15.getProtocol()
        && protocolVersion.getProtocol() < MINECRAFT_1_18.getProtocol()) {
        if (protocolVersion.getProtocol() >= MINECRAFT_1_16_2.getProtocol()) {
          writeVarInt(byteBuf, 1024);

          for (int i = 0; i < 1024; i++) {
            writeVarInt(byteBuf, 1);
          }
        } else {
          for (int i = 0; i < 1024; i++) {
            byteBuf.writeInt(0);
          }
        }
      }
    }

    if (protocolVersion.getProtocol() < MINECRAFT_1_13.getProtocol()) {
      writeArray(byteBuf, new byte[256]); //1.8 - 1.12.2
    } else if (protocolVersion.getProtocol() < MINECRAFT_1_15.getProtocol()) {
      writeArray(byteBuf, new byte[1024]); //1.13 - 1.14.4
    } else if (protocolVersion.getProtocol() < MINECRAFT_1_18.getProtocol()) {
      writeVarInt(byteBuf, 0); //1.15 - 1.17.1
    } else {
      byte[] sectionData = new byte[]{0, 0, 0, 0, 0, 0, 1, 0};

      writeVarInt(byteBuf, sectionData.length * 16);

      for (int i = 0; i < 16; i++) {
        byteBuf.writeBytes(sectionData);
      }
    }

    if (protocolVersion.getProtocol() >= MINECRAFT_1_9_4.getProtocol()) {
      writeVarInt(byteBuf, 0);
    }

    if (protocolVersion.getProtocol() >= MINECRAFT_1_18.getProtocol()) {
      byte[] lightData = new byte[]{1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 3, -1, -1, 0, 0};
      byteBuf.writeBytes(lightData);
    }
  }

  private static void writeArray(final ByteBuf byteBuf, final byte[] bytes) {
    if (bytes.length > Short.MAX_VALUE) {
      throw new CorruptedFrameException("Array too long");
    }

    writeVarInt(byteBuf, bytes.length);
    byteBuf.writeBytes(bytes);
  }

  @Override
  public boolean handle(final MinecraftSessionHandler minecraftSessionHandler) {
    return false;
  }
}
