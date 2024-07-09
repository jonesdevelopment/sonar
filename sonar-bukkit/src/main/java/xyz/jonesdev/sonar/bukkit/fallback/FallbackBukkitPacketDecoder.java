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

package xyz.jonesdev.sonar.bukkit.fallback;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.common.fallback.FallbackPacketHandlerAdapter;

import java.util.List;

final class FallbackBukkitPacketDecoder extends FallbackPacketHandlerAdapter {

  FallbackBukkitPacketDecoder() {
    super("encoder", "decoder", "packet_handler", "timeout");
  }

  @Override
  protected void decode(final @NotNull ChannelHandlerContext ctx,
                        final @NotNull Object msg,
                        final @NotNull List<Object> out) throws Exception {
    if (msg instanceof ByteBuf) {
      final ByteBuf byteBuf = (ByteBuf) msg;
      if (byteBuf.isReadable()) {
        final ByteBuf transformed = ctx.alloc().buffer().writeBytes(byteBuf);

        try {
          out.add(transformed.retain());
        } finally {
          transformed.release();
        }
      }
    }
  }
}
