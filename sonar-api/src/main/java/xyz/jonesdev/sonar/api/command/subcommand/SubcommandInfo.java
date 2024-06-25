/*
 * Copyright (C) 2023-2024 Sonar Contributors
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

package xyz.jonesdev.sonar.api.command.subcommand;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SubcommandInfo {

  /**
   * @return Name of the subcommand
   */
  String name();

  /**
   * @return Aliases of the subcommand
   */
  String[] aliases() default {};

  /**
   * @return Arguments of the subcommand
   */
  String[] arguments() default {};

  /**
   * @return Whether the subcommands requires arguments
   */
  boolean argumentsRequired() default true;

  /**
   * @return Whether the subcommands can only be executed by a player
   */
  boolean onlyPlayers() default false;

  /**
   * @return Whether the subcommands can only be executed by console
   */
  boolean onlyConsole() default false;
}
