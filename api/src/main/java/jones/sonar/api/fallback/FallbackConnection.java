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

package jones.sonar.api.fallback;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import jones.sonar.api.Sonar;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;

@Getter
@RequiredArgsConstructor
public final class FallbackConnection<Player, Connection> {
    private final Fallback fallback;
    private final Player player;
    private final Connection connection;
    private final Channel channel;
    private final ChannelPipeline pipeline;
    private final InetAddress inetAddress;
    private final int protocolVersion;
    private final long loginTimestamp = System.currentTimeMillis();

    public void fail(final String reason) {
        channel.close();

        fallback.getBlacklisted().add(inetAddress);

        Sonar.get().getLogger().info("[Fallback] " + inetAddress + " has failed the bot check for: " + reason);
    }
}
