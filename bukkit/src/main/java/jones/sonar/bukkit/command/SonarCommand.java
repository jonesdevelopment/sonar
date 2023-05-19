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

package jones.sonar.bukkit.command;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jones.sonar.common.command.CommandHelper;
import jones.sonar.common.command.CommandInvocation;
import jones.sonar.common.command.InvocationSender;
import jones.sonar.common.command.subcommand.SubCommand;
import jones.sonar.common.command.subcommand.SubCommandManager;
import lombok.var;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class SonarCommand implements CommandExecutor {
    private static final Cache<CommandSender, Long> delay = CacheBuilder.newBuilder()
            .expireAfterWrite(500L, TimeUnit.MILLISECONDS)
            .build();
    private static final String ONLY_PLAYERS = "§cYou can only execute this command as a player.";
    private static final String CANNOT_RUN_YET = "§cYou can only execute this command every 0.5 seconds.";
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.#");

    @Override
    public boolean onCommand(final CommandSender sender,
                             final Command command,
                             final String label,
                             final String[] args) {
        if (delay.asMap().containsKey(sender)) {
            sender.sendMessage(CANNOT_RUN_YET);

            final long timestamp = delay.asMap().get(sender);
            final double left = 0.5D - ((System.currentTimeMillis() - (double) timestamp) / 1000D);
            final String format = decimalFormat.format(left);

            final String pleaseWaitAnother = "§cPlease wait another §l" + format + "s§r§c.";

            sender.sendMessage(pleaseWaitAnother);
            return false;
        }

        delay.put(sender, System.currentTimeMillis());

        var subCommand = Optional.<SubCommand>empty();

        final InvocationSender invocationSender = sender::sendMessage;

        if (args.length > 0) {
            subCommand = SubCommandManager.getSubCommands().stream()
                    .filter(sub -> sub.getInfo().name().equalsIgnoreCase(args[0])
                            || (sub.getInfo().aliases().length > 0
                            && Arrays.stream(sub.getInfo().aliases())
                            .anyMatch(alias -> alias.equalsIgnoreCase(args[0]))))
                    .findFirst();
        }

        if (!subCommand.isPresent()) {
            CommandHelper.printHelp(invocationSender);
            return false;
        }

        subCommand.ifPresent(sub -> {
            if (sub.getInfo().onlyPlayers() && !(sender instanceof Player)) {
                sender.sendMessage(ONLY_PLAYERS);
                return;
            }

            final CommandInvocation commandInvocation = new CommandInvocation(
                    invocationSender,
                    sub,
                    args
            );

            sub.execute(commandInvocation);
        });
        return false;
    }
}
