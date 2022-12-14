package jones.sonar.bungee.command.impl;

import jones.sonar.bungee.command.CommandExecution;
import jones.sonar.bungee.command.SubCommand;
import jones.sonar.bungee.config.Messages;
import jones.sonar.universal.data.connection.manager.ConnectionDataManager;
import jones.sonar.universal.platform.bungee.SonarBungee;

import java.util.Arrays;

public final class VerifyCommand extends SubCommand {

    public VerifyCommand() {
        super("verify",
                "Verification management",
                "sonar.verify",
                Arrays.asList("size", "clear", "reset", "purge"));
    }

    @Override
    public void execute(final CommandExecution execution) {
        if (execution.arguments.length > 1) {
            switch (execution.arguments[1]) {
                case "reset":
                case "clear": {
                    if (ConnectionDataManager.getVerifying() == 0) {
                        execution.send(Messages.Values.VERIFICATION_EMPTY);
                        return;
                    }

                    final long verifying = ConnectionDataManager.getVerifying();

                    ConnectionDataManager.getVerifyingData().forEach(ConnectionDataManager::remove);

                    final long difference = Math.max(verifying - ConnectionDataManager.getVerifying(), 0);

                    execution.send(Messages.Values.VERIFICATION_CLEAR
                            .replaceAll("%verifying%", SonarBungee.INSTANCE.FORMAT.format(difference))
                            .replaceAll("%s%", difference == 1 ? "" : "s"));
                    return;
                }

                case "size": {
                    final long verifying = ConnectionDataManager.getVerifying();

                    if (verifying == 0) {
                        execution.send(Messages.Values.VERIFICATION_EMPTY);
                        return;
                    }

                    execution.send(Messages.Values.VERIFICATION_SIZE
                            .replaceAll("%verifying%", SonarBungee.INSTANCE.FORMAT.format(verifying))
                            .replaceAll("%s%", verifying == 1 ? "" : "s"));
                    return;
                }

                case "purge": {
                    execution.send(Messages.Values.VERIFICATION_PURGING);

                    if (ConnectionDataManager.getVerifying() > 0) {

                        // remove all blacklisted but still existing players
                        ConnectionDataManager.removeAllUnused();

                        // reset all stages of all verifying players to 1 to
                        // force another reconnect when verifying during an attack
                        ConnectionDataManager.resetCheckStage(1);

                        ConnectionDataManager.getVerifyingData().forEach(ConnectionDataManager::remove);

                        // garbage collect
                        System.gc();

                        execution.send(Messages.Values.VERIFICATION_PURGE_COMPLETE);
                    } else {
                        execution.send(Messages.Values.VERIFICATION_PURGE_NONE);
                    }
                    return;
                }
            }
        }

        execution.sendUsage("/ab verify <clear|size|purge>");
    }
}
