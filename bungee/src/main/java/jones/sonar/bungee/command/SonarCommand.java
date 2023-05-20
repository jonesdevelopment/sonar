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

package jones.sonar.bungee.command;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jones.sonar.common.command.CommandHelper;
import jones.sonar.common.command.CommandInvocation;
import jones.sonar.common.command.InvocationSender;
import jones.sonar.common.command.subcommand.SubCommand;
import jones.sonar.common.command.subcommand.SubCommandManager;
import lombok.var;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class SonarCommand extends Command {
    private static final Cache<CommandSender, Long> delay = CacheBuilder.newBuilder()
            .expireAfterWrite(500L, TimeUnit.MILLISECONDS)
            .build();
    private static final TextComponent ONLY_PLAYERS = new TextComponent(
            "§cYou can only execute this command as a player."
    );
    private static final TextComponent CANNOT_RUN_YET = new TextComponent(
            "§cYou can only execute this command every 0.5 seconds."
    );
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.#");

    public SonarCommand() {
        super("sonar", "sonar.command");
    }

    @Override
    public void execute(final CommandSender sender, final String[] args) {
        if (delay.asMap().containsKey(sender)) {
            sender.sendMessage(CANNOT_RUN_YET);

            final long timestamp = delay.asMap().get(sender);
            final double left = 0.5D - ((System.currentTimeMillis() - (double) timestamp) / 1000D);
            final String format = decimalFormat.format(left);

            final TextComponent pleaseWaitAnother = new TextComponent("§cPlease wait another §l" + format + "s§r§c.");

            sender.sendMessage(pleaseWaitAnother);
            return;
        }

        delay.put(sender, System.currentTimeMillis());

        var subCommand = Optional.<SubCommand>empty();

        var invocationSender = new InvocationSender<CommandSender>() {

            @Override
            public void sendMessage(final String message) {
                sender.sendMessage(message);
            }

            @Override
            public CommandSender getPlayer() {
                return sender;
            }
        };

        if (args.length > 0) {
            subCommand = SubCommandManager.getSubCommands().stream()
                    .filter(sub -> sub.getInfo().name().equalsIgnoreCase(args[0])
                            || (sub.getInfo().aliases().length > 0
                            && Arrays.stream(sub.getInfo().aliases())
                            .anyMatch(alias -> alias.equalsIgnoreCase(args[0]))))
                    .findFirst();

            if (subCommand.isPresent()) {
                final String permission = "sonar." + subCommand.get().getInfo().name();

                if (!sender.hasPermission(permission)) {
                    sender.sendMessage(new TextComponent(
                            "§cYou do not have permission to execute this subcommand. §7(" + permission + ")"
                    ));
                    return;
                }
            }
        }

        if (!subCommand.isPresent()) {
            CommandHelper.printHelp(invocationSender);
            return;
        }

        subCommand.ifPresent(sub -> {
            if (sub.getInfo().onlyPlayers() && !(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ONLY_PLAYERS);
                return;
            }

            final CommandInvocation commandInvocation = new CommandInvocation(
                    sender.getName(),
                    invocationSender,
                    sub,
                    args
            );

            sub.execute(commandInvocation);
        });
    }
}
