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

package xyz.jonesdev.sonar.api.command;

import xyz.jonesdev.cappuccino.Cappuccino;
import xyz.jonesdev.cappuccino.ExpiringCache;

import java.util.*;

public interface SonarBaseCommand {
  List<String> TAB_SUGGESTIONS = new ArrayList<>();
  Map<String, List<String>> ARG_TAB_SUGGESTIONS = new HashMap<>();
  ExpiringCache<Object> DELAY = Cappuccino.buildExpiring(500L);
  List<Object> CACHED_HELP_MESSAGE = new Vector<>();
  int COPYRIGHT_YEAR = Calendar.getInstance().get(Calendar.YEAR);
}
