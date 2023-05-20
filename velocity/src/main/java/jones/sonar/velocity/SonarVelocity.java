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

import com.velocitypowered.proxy.VelocityServer;
import jones.sonar.api.Sonar;
import jones.sonar.api.SonarPlatform;
import jones.sonar.api.SonarProvider;
import jones.sonar.api.config.SonarConfiguration;
import jones.sonar.common.SonarPlugin;
import jones.sonar.velocity.command.SonarCommand;
import jones.sonar.velocity.fallback.FallbackConnectionLimiter;
import jones.sonar.velocity.fallback.FallbackListener;
import jones.sonar.velocity.verbose.ActionBarVerbose;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

public enum SonarVelocity implements Sonar, SonarPlugin<SonarVelocityPlugin> {

    INSTANCE;

    @Getter
    private SonarVelocityPlugin plugin;

    @Getter
    private ActionBarVerbose actionBarVerbose;

    @Getter
    private SonarConfiguration config;

    @Override
    public SonarPlatform getPlatform() {
        return SonarPlatform.VELOCITY;
    }

    @Override
    public void enable(final SonarVelocityPlugin plugin) {
        this.plugin = plugin;

        final long start = System.currentTimeMillis();

        // Set the API to this class
        SonarProvider.set(this);

        plugin.getLogger().info("Initializing Sonar...");

        // Initialize configuration
        config = new SonarConfiguration(plugin.getDataDirectory().toFile());
        config.load();

        // Register Sonar command
        plugin.getServer().getCommandManager().register("sonar", new SonarCommand());

        // Register Fallback listener
        plugin.getServer().getEventManager().register(plugin, new FallbackListener(
                (VelocityServer) plugin.getServer(), plugin.getLogger()
        ));

        // Apply filter (connection limiter) to Fallback
        Sonar.get().getFallback().setFilter(FallbackConnectionLimiter::shouldAllow);

        // Register Fallback queue task
        plugin.getServer().getScheduler().buildTask(plugin, getFallback().getQueue()::poll)
                .repeat(500L, TimeUnit.MILLISECONDS)
                .schedule();

        // Initialize action bar verbose
        actionBarVerbose = new ActionBarVerbose(plugin.getServer());

        // Register action bar verbose task
        plugin.getServer().getScheduler().buildTask(plugin, actionBarVerbose::update)
                .repeat(100L, TimeUnit.MILLISECONDS)
                .schedule();

        // Done
        final long startDelay = System.currentTimeMillis() - start;

        plugin.getLogger().info("Done ({}s)!", String.format("%.3f", startDelay / 1000D));
    }

    @Override
    public void disable() {
        // Do nothing
    }
}
