/*
 *  Copyright (c) 2023, jones (https://jonesdev.xyz) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jones.sonar.common.fallback;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public final class FallbackTimeoutHandler extends IdleStateHandler {
    private boolean closed;

    public FallbackTimeoutHandler(final long timeout, final TimeUnit timeUnit) {
        super(timeout, 0L, 0L, timeUnit);
    }

    @Override
    protected void channelIdle(final ChannelHandlerContext ctx,
                               final IdleStateEvent idleStateEvent) throws Exception {
        assert idleStateEvent.state() == IdleState.READER_IDLE;

        readTimedOut(ctx);
    }

    private void readTimedOut(final ChannelHandlerContext ctx) throws Exception {
        if (!closed) {

            // ==========================================================
            // The netty (default) ReadTimeoutHandler would normally just throw an Exception
            // The default ReadTimeoutHandler does only check for the boolean 'closed' and
            // still throws the Exception even if the channel is closed
            // This was discovered and fixed by @jones
            // ==========================================================

            if (ctx.channel().isActive()) {
                ctx.close();
            }

            closed = true;
        }
    }
}
