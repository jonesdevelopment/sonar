package xyz.jonasdev.sonar.paper.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.api.command.SonarCommand;

import java.util.Collection;
import java.util.Collections;

public final class PaperSonarCommand implements BasicCommand, SonarCommand {
  @Override
  public void execute(final CommandSourceStack commandSourceStack, final String @NonNull [] args) {
    final CommandSender sender = commandSourceStack.getSender();
    final InvocationSource invocationSource = new InvocationSource(
      sender instanceof Player ? ((Player) sender).getUniqueId() : null,
      sender,
      sender::hasPermission
    );
    handle(invocationSource, args);
  }

  @Override
  public @NonNull Collection<String> suggest(final CommandSourceStack commandSourceStack, final String @NonNull [] args) {
    // Do not allow tab completion if the player does not have the required permission
    return commandSourceStack.getSender().hasPermission("sonar.command") ?
      getCachedTabSuggestions(args) : Collections.emptyList();
  }
}
