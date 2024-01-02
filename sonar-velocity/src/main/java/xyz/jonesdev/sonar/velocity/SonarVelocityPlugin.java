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

package xyz.jonesdev.sonar.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;

@Getter
public final class SonarVelocityPlugin {
  private final ProxyServer server;
  private final Logger logger;
  private final Path dataDirectory;
  private final Metrics.Factory metricsFactory;
  private SonarVelocity bootstrap;

  @Inject
  public SonarVelocityPlugin(final ProxyServer server,
                             final Logger logger,
                             final @DataDirectory Path dataDirectory,
                             final Metrics.Factory metricsFactory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.metricsFactory = metricsFactory;
  }

  @Subscribe
  public void handle(final ProxyInitializeEvent event) {
    bootstrap = new SonarVelocity(this);
    bootstrap.initialize();
  }

  @Subscribe
  public void handle(final ProxyShutdownEvent event) {
    bootstrap.shutdown();
  }
}
