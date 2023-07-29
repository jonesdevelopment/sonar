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

package xyz.jonesdev.sonar.bungee.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import net.md_5.bungee.netty.PipelineUtils;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public final class NettyInjector {
  public void inject(final ChannelInitializer<Channel> childHandler) {
    try {
      final Field childField = PipelineUtils.class.getDeclaredField("SERVER_CHILD");
      childField.setAccessible(true);

      final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
      unsafeField.setAccessible(true);
      final Unsafe unsafe = (Unsafe) unsafeField.get(null);

      final Object base = unsafe.staticFieldBase(childField);
      final long offset = unsafe.staticFieldOffset(childField);

      unsafe.putObject(base, offset, childHandler);
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }
}
