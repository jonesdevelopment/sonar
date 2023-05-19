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

package jones.sonar.velocity.command;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import jones.sonar.common.command.CommandHelper;
import jones.sonar.common.command.CommandInvocation;
import jones.sonar.common.command.CommandSender;
import jones.sonar.common.command.subcommand.SubCommand;
import jones.sonar.common.command.subcommand.SubCommandManager;
import net.kyori.adventure.text.Component;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class SonarCommand implements SimpleCommand {
    private static final Cache<CommandSource, Long> delay = Caffeine.newBuilder()
            .expireAfterWrite(1L, TimeUnit.SECONDS)
            .build();
    private static final Component ONLY_PLAYERS = Component.text(
            "§cYou can only execute this command as a player."
    );
    private static final Component CANNOT_RUN_YET = Component.text(
            "§cYou can only execute this command every second."
    );
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.#");

    @Override
    public void execute(final Invocation invocation) {
        if (delay.asMap().containsKey(invocation.source())) {
            invocation.source().sendMessage(CANNOT_RUN_YET);

            final long timestamp = delay.asMap().get(invocation.source());
            final double left = 1D - ((System.currentTimeMillis() - (double) timestamp) / 1000D);
            final String format = decimalFormat.format(left);

            final Component pleaseWaitAnother = Component.text("§cPlease wait another §l" + format + "s§r§c.");

            invocation.source().sendMessage(pleaseWaitAnother);
            return;
        }

        delay.put(invocation.source(), System.currentTimeMillis());

        var subCommand = Optional.<SubCommand>empty();

        final CommandSender sender = message -> invocation.source().sendMessage(Component.text(message));

        if (invocation.arguments().length > 0) {
            subCommand = SubCommandManager.getSubCommands().stream()
                    .filter(sub -> sub.getInfo().name().equalsIgnoreCase(invocation.arguments()[0])
                            || (sub.getInfo().aliases().length > 0
                            && Arrays.stream(sub.getInfo().aliases())
                            .anyMatch(alias -> alias.equalsIgnoreCase(invocation.arguments()[0]))))
                    .findFirst();

            if (subCommand.isEmpty()) {
                sender.sendMessage("§cThis subcommand could not be found.");
            }
        }

        subCommand.ifPresentOrElse(sub -> {
            if (sub.getInfo().onlyPlayers() && !(invocation.source() instanceof Player)) {
                invocation.source().sendMessage(ONLY_PLAYERS);
                return;
            }

            final CommandInvocation commandInvocation = new CommandInvocation(
                    sender,
                    sub,
                    invocation.arguments()
            );

            sub.execute(commandInvocation);
        }, () -> CommandHelper.printHelp(sender));
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("sonar.command");
    }
}
