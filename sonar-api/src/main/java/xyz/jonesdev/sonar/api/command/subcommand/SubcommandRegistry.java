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

package xyz.jonesdev.sonar.api.command.subcommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface SubcommandRegistry {
  List<SubCommand> subcommands = Collections.synchronizedList(new ArrayList<>());

  default List<SubCommand> getSubcommands() {
    return Collections.unmodifiableList(subcommands);
  }

  default void register(final SubCommand... subcommand) {
    subcommands.addAll(Arrays.asList(subcommand));
  }

  default void unregister(final SubCommand... subcommand) {
    subcommands.removeAll(Arrays.asList(subcommand));
  }
}
