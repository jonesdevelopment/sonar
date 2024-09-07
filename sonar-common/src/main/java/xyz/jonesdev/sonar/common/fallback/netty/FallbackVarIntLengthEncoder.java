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

package xyz.jonesdev.sonar.common.fallback.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static xyz.jonesdev.sonar.common.util.ProtocolUtil.varIntBytes;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.writeVarInt;

// https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/protocol/netty/MinecraftVarintLengthEncoder.java
@ChannelHandler.Sharable
public final class FallbackVarIntLengthEncoder extends MessageToMessageEncoder<ByteBuf> {
  public static final FallbackVarIntLengthEncoder INSTANCE = new FallbackVarIntLengthEncoder();

  @Override
  protected void encode(final @NotNull ChannelHandlerContext ctx,
                        final @NotNull ByteBuf byteBuf,
                        final @NotNull List<Object> out) throws Exception {
    final int length = byteBuf.readableBytes();
    final int varIntLength = varIntBytes(length);

    final ByteBuf lenBuf = ctx.alloc().buffer(varIntLength);

    writeVarInt(lenBuf, length);
    out.add(lenBuf);
    out.add(byteBuf.retain());
  }
}
