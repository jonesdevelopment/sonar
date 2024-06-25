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

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

@Getter
public final class SubcommandRegistry {
  private final List<Subcommand> subcommands = new Vector<>();

  /**
   * Registers a single or multiple subcommand(s)
   *
   * @param subcommand Array of Subcommands to register
   */
  public void register(final Subcommand @NotNull ... subcommand) {
    subcommands.addAll(Arrays.asList(subcommand));
  }

  /**
   * Unregisters a single or multiple subcommand(s)
   *
   * @param subcommand Array of Subcommands to unregister
   */
  @SuppressWarnings("unused")
  public void unregister(final Subcommand @NotNull ... subcommand) {
    subcommands.removeAll(Arrays.asList(subcommand));
  }
}
