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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import jones.sonar.api.Sonar;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.net.InetSocketAddress;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public final class FallbackChannelHandler extends ChannelInboundHandlerAdapter {
    public static final FallbackChannelHandler INSTANCE = new FallbackChannelHandler(Sonar.get());
    private final Sonar sonar;

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        val inetAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();

        sonar.getFallback().getConnected().remove(inetAddress);

        sonar.getLogger().info("[Fallback] Disconnect: {}", inetAddress);
    }
}
