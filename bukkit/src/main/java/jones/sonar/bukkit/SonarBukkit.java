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

package jones.sonar.bukkit;

import jones.sonar.api.Sonar;
import jones.sonar.api.SonarPlatform;
import jones.sonar.api.SonarProvider;
import jones.sonar.bukkit.command.SonarCommand;
import jones.sonar.bukkit.verbose.ActionBarVerbose;
import jones.sonar.common.SonarPlugin;
import lombok.Getter;

import java.util.Objects;

public enum SonarBukkit implements Sonar, SonarPlugin<SonarBukkitPlugin> {

    INSTANCE;

    @Getter
    private SonarBukkitPlugin plugin;

    @Getter
    private ActionBarVerbose actionBarVerbose;

    @Override
    public SonarPlatform getPlatform() {
        return SonarPlatform.BUKKIT;
    }

    @Override
    public void enable(final SonarBukkitPlugin plugin) {
        this.plugin = plugin;

        // Set the API to this class
        SonarProvider.set(this);

        plugin.getLogger().info("Initializing Sonar...");

        // Register Sonar command
        Objects.requireNonNull(plugin.getCommand("sonar")).setExecutor(new SonarCommand());

        // Initialize action bar verbose
        actionBarVerbose = new ActionBarVerbose();

        // Register action bar verbose task
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, actionBarVerbose::update,
                100L, 100L);
    }

    @Override
    public void disable() {
    }
}
