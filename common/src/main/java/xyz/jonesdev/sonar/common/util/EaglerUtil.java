/*
 * Copyright (C) 2025 Sonar Contributors
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

package xyz.jonesdev.sonar.common.util;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class EaglerUtil {
  // https://github.com/lax1dude/eaglerxserver/blob/main/core/src/main/java/net/lax1dude/eaglercraft/backend/server/adapter/PipelineAttributes.java
  public static final AttributeKey<Object> EAGLER_LISTENER_DATA = AttributeKey.valueOf("$eagler0");
  public static final AttributeKey<Object> EAGLER_PIPELINE_DATA = AttributeKey.valueOf("$eagler1");

  public boolean isEaglerConnection(final @NotNull Channel channel) {
    return channel.attr(EAGLER_LISTENER_DATA).get() != null
      || channel.attr(EAGLER_PIPELINE_DATA).get() != null;
  }
}
