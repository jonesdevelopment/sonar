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

package jones.sonar.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.slf4j.Logger;

@Getter
@Plugin(id = "sonar",
        name = "Sonar",
        version = "0.1.0",
        authors = "jonesdev.xyz",
        url = "https://jonesdev.xyz/",
        description = "Anti-bot plugin for Velocity, BungeeCord and Spigot (1.8-latest)"
)
public final class SonarVelocityPlugin {
    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public SonarVelocityPlugin(final ProxyServer server, final Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void handle(final ProxyInitializeEvent event) {
        SonarVelocity.INSTANCE.enable(this);
    }
}
