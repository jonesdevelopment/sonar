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

package jones.sonar.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.slf4j.Logger;

import java.nio.file.Path;

@Getter
@Plugin(id = "sonar",
        name = "Sonar",
        version = "2.0.0",
        authors = "jonesdev.xyz",
        url = "https://jonesdev.xyz/",
        description = "Anti-bot plugin for Velocity, BungeeCord and Bukkit (1.8-latest)"
)
public final class SonarVelocityPlugin {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public SonarVelocityPlugin(final ProxyServer server,
                               final Logger logger,
                               final @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void handle(final ProxyInitializeEvent event) {
        SonarVelocity.INSTANCE.enable(this);
    }
}
