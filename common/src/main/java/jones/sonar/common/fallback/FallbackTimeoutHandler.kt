/*
 * Copyright (C) 2023, jones
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

package jones.sonar.common.fallback

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import io.netty.handler.timeout.IdleStateHandler
import java.util.concurrent.TimeUnit

class FallbackTimeoutHandler(timeout: Long, timeUnit: TimeUnit?) : IdleStateHandler(timeout, 0L, 0L, timeUnit) {
  private var closed = false

  @Throws(Exception::class)
  override fun channelIdle(
    ctx: ChannelHandlerContext,
    idleStateEvent: IdleStateEvent
  ) {
    assert(idleStateEvent.state() == IdleState.READER_IDLE)
    readTimedOut(ctx)
  }

  @Throws(Exception::class)
  private fun readTimedOut(ctx: ChannelHandlerContext) {
    if (!closed) {

      // The netty (default) ReadTimeoutHandler would normally just throw an Exception
      // The default ReadTimeoutHandler does only check for the boolean 'closed' and
      // still throws the Exception even if the channel is closed
      if (ctx.channel().isActive) {
        ctx.close()
      }

      closed = true
    }
  }
}
