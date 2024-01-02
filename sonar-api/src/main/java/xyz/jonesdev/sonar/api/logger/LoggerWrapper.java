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

package xyz.jonesdev.sonar.api.logger;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;

/**
 * The whole purpose of doing this is, so we can have one logger for every module.
 * We wouldn't need to get the plugin logger every time we try to use it.
 * Additionally, we are also able to access this logger from {@link xyz.jonesdev.sonar.api.Sonar}.
 */
public interface LoggerWrapper {
  void info(final String message, final Object... args);

  void warn(final String message, final Object... args);

  void error(final String message, final Object... args);

  // Taken from
  // https://github.com/j256/ormlite-core/blob/master/src/main/java/com/j256/ormlite/logger/Logger.java
  String ARG_STRING = "{}";
  int ARG_STRING_LENGTH = ARG_STRING.length();
  Object UNKNOWN_ARG = new Object();

  default String buildFullMessage(final @NotNull String msg, final Object... args) {
    StringBuilder sb = null;
    int lastIndex = 0;
    int argC = 0;
    while (true) {
      int argIndex = msg.indexOf(ARG_STRING, lastIndex);
      // no more {} arguments?
      if (argIndex == -1) {
        break;
      }
      if (sb == null) {
        // we build this lazily in case there is no {} in the msg
        sb = new StringBuilder(128);
      }
      // add the string before the arg-string
      sb.append(msg, lastIndex, argIndex);
      // shift our last-index past the arg-string
      lastIndex = argIndex + ARG_STRING_LENGTH;
      // add the arguments
      if (argC < args.length) {
        appendArg(sb, args[argC]);
      }
      argC++;
    }
    if (sb == null) {
      return msg;
    } else {
      sb.append(msg, lastIndex, msg.length());
      return sb.toString();
    }
  }

  default void appendArg(final StringBuilder stringBuilder, final Object arg) {
    if (arg == UNKNOWN_ARG) {
      // ignore it
    } else if (arg == null) {
      stringBuilder.append("null");
    } else if (arg.getClass().isArray()) {
      stringBuilder.append('[');
      int length = Array.getLength(arg);
      for (int i = 0; i < length; i++) {
        if (i > 0) {
          stringBuilder.append(", ");
        }
        appendArg(stringBuilder, Array.get(arg, i));
      }
      stringBuilder.append(']');
    } else {
      stringBuilder.append(arg);
    }
  }
}
