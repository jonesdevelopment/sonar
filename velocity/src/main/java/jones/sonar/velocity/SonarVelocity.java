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

import jones.sonar.api.Sonar;
import jones.sonar.api.SonarPlatform;
import jones.sonar.api.SonarProvider;
import jones.sonar.api.fallback.Fallback;
import jones.sonar.api.statistics.Statistics;
import jones.sonar.common.SonarPlugin;
import jones.sonar.common.command.subcommand.SubCommandManager;
import jones.sonar.common.fallback.FallbackManager;
import jones.sonar.common.statistics.SonarStatistics;
import jones.sonar.velocity.command.SonarCommand;
import lombok.Getter;

public enum SonarVelocity implements Sonar, SonarPlugin<SonarVelocityPlugin> {

    INSTANCE;

    @Getter
    private final Fallback fallback = new FallbackManager();

    @Getter
    private final Statistics statistics = new SonarStatistics();

    @Getter
    private SonarVelocityPlugin plugin;

    @Override
    public SonarPlatform getPlatform() {
        return SonarPlatform.VELOCITY;
    }

    @Override
    public void enable(final SonarVelocityPlugin plugin) {
        this.plugin = plugin;

        // Set the API to this class
        SonarProvider.set(this);

        plugin.getLogger().info("Initializing Sonar...");

        // Register Sonar command
        plugin.getServer().getCommandManager().register("sonar", new SonarCommand());

        // Initialize all subcommands
        SubCommandManager.initialize();
    }

    @Override
    public void disable() {
        // Do nothing
    }
}
