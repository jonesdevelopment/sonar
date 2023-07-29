/*
 * Copyright (C) 2023 Sonar Contributors
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

package xyz.jonesdev.sonar.common.command.impl;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.argument.Argument;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;
import xyz.jonesdev.sonar.api.database.DatabaseType;

import java.io.File;
import java.nio.file.Files;

@SubcommandInfo(
  name = "database",
  description = "Data storage management",
  arguments = {
    @Argument("info"),
    @Argument("purge"),
  }
)
public final class DatabaseCommand extends Subcommand {
  // use this as a "lock" to prevent players from spamming purge
  private boolean purging = false;

  @Override
  public void execute(final @NotNull CommandInvocation invocation) {
    if (SONAR.getConfig().DATABASE == DatabaseType.NONE) {
      invocation.getSender().sendMessage(SONAR.getConfig().DATABASE_NOT_SELECTED);
      return;
    }

    switch (invocation.getArguments()[1].toLowerCase()) {
      case "info": {
        invocation.getSender().sendMessage();
        invocation.getSender().sendMessage(" §eData storage information");
        invocation.getSender().sendMessage();
        invocation.getSender().sendMessage(" §a▪ §7Current data storage type: §f${sonar.config.DATABASE}");

        switch (SONAR.getConfig().DATABASE) {
          case MYSQL: {
            invocation.getSender().sendMessage(" §a▪ §7Database URL: §f${sonar.config.DATABASE_URL}");
            invocation.getSender().sendMessage(" §a▪ §7Database port: §f${sonar.config.DATABASE_PORT}");
            invocation.getSender().sendMessage(" §a▪ §7Database name: §f${sonar.config.DATABASE_NAME}");
            invocation.getSender().sendMessage(" §a▪ §7Query limit: §f${Sonar.DECIMAL_FORMAT.format(sonar.config.DATABASE_QUERY_LIMIT)}");
            break;
          }

          case YAML: {
            final File file = new File(SONAR.getPluginDataFolder(), SONAR.getConfig().DATABASE_FILE_NAME + ".yml");

            if (!file.exists()) {
              invocation.getSender().sendMessage(" §c▪ §7File does not exist?!");
              return;
            }

            invocation.getSender().sendMessage(" §a▪ §7File name: §f${sonar.config.DATABASE_FILE_NAME}.yml");

            try {
              final long fileSize = Files.size(file.toPath());
              invocation.getSender().sendMessage(" §a▪ §7File size: §f${MemoryFormatter.formatMemory(fileSize)}");
            } catch (Exception exception) {
              exception.printStackTrace();
            }
            break;
          }

          default: {
            throw new IllegalStateException("Invalid argument");
          }
        }
        invocation.getSender().sendMessage();
      }

      case "purge": {
        if (invocation.getArguments().length == 2) {
          invocation.getSender().sendMessage(SONAR.getConfig().DATABASE_PURGE_CONFIRM);
          return;
        }

        // This is a security feature
        if (!SONAR.getConfig().ALLOW_PURGING) {
          invocation.getSender().sendMessage(SONAR.getConfig().DATABASE_PURGE_DISALLOWED);
          return;
        }

        if (purging) {
          invocation.getSender().sendMessage(SONAR.getConfig().DATABASE_PURGE_ALREADY);
          return;
        }

        purging = true;
        try {
          SONAR.getDatabase().purge();

          SONAR.reload();
          invocation.getSender().sendMessage(SONAR.getConfig().DATABASE_PURGE);
        } catch (Throwable throwable) {
          throwable.printStackTrace();
        }
        purging = false;
      }

      default: {
        incorrectUsage(invocation.getSender());
        break;
      }
    }
  }
}
