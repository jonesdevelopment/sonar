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

package jones.sonar.bungee;

import jones.sonar.api.Sonar;
import jones.sonar.api.SonarPlatform;
import jones.sonar.api.SonarProvider;
import jones.sonar.api.config.SonarConfiguration;
import jones.sonar.api.logger.Logger;
import jones.sonar.bungee.command.SonarCommand;
import jones.sonar.bungee.verbose.ActionBarVerbose;
import jones.sonar.common.SonarPlugin;
import lombok.Getter;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public enum SonarBungee implements Sonar, SonarPlugin<SonarBungeePlugin> {

    INSTANCE;

    @Getter
    private SonarBungeePlugin plugin;

    @Getter
    private ActionBarVerbose actionBarVerbose;

    @Getter
    private SonarConfiguration config;

    @Getter
    private Logger logger;

    @Override
    public SonarPlatform getPlatform() {
        return SonarPlatform.BUNGEE;
    }

    @Override
    public void enable(final SonarBungeePlugin plugin) {
        this.plugin = plugin;

        final long start = System.currentTimeMillis();

        // Set the API to this class
        SonarProvider.set(this);

        plugin.getLogger().info("Initializing Sonar...");

        // Initialize logger
        logger = new Logger() {

            @Override
            public void info(final String message, final Object... args) {
                plugin.getLogger().log(Level.INFO, message, args);
            }

            @Override
            public void warn(final String message, final Object... args) {
                plugin.getLogger().log(Level.WARNING, message, args);
            }

            @Override
            public void error(final String message, final Object... args) {
                plugin.getLogger().log(Level.SEVERE, message, args);
            }
        };

        // Initialize configuration
        config = new SonarConfiguration(plugin.getDataFolder());
        config.load();

        // Register Sonar command
        plugin.getServer().getPluginManager().registerCommand(plugin, new SonarCommand());

        // Initialize action bar verbose
        actionBarVerbose = new ActionBarVerbose(plugin.getServer());

        // Register action bar verbose task
        plugin.getServer().getScheduler().schedule(plugin, actionBarVerbose::update,
                100L, TimeUnit.MILLISECONDS);

        // Done
        final long startDelay = System.currentTimeMillis() - start;

        plugin.getLogger().info("Done (" + String.format("%.3f", startDelay / 1000D) + "s)!");
    }

    @Override
    public void disable() {
        // Do nothing
    }
}
