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

package jones.sonar.velocity.verbose;

import com.velocitypowered.api.proxy.ProxyServer;
import jones.sonar.api.verbose.Verbose;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Collection;

@RequiredArgsConstructor
public final class ActionBarVerbose implements Verbose {
    private final ProxyServer server;
    @Getter
    private final Collection<String> subscribers = new ArrayList<>();

    private static final String[] states = {"▙", "▛", "▜", "▟"};
    private static int stateIndex = 0;

    private static String nextState() {
        return states[++stateIndex % states.length];
    }

    public void update() {
        final Component component = Component.text("§e§lSonar §7> §f" + nextState());

        for (final String subscriber : subscribers) {
            server.getPlayer(subscriber).ifPresent(player -> {
                player.sendActionBar(component);
            });
        }
    }
}
