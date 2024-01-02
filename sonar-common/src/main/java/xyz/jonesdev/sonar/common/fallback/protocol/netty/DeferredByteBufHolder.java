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

package xyz.jonesdev.sonar.common.fallback.protocol.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.util.IllegalReferenceCountException;
import lombok.AllArgsConstructor;
import lombok.ToString;

// Taken from
// https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/protocol/util/DeferredByteBufHolder.java
@ToString
@AllArgsConstructor
public class DeferredByteBufHolder implements ByteBufHolder {
  private ByteBuf backing;

  @Override
  public ByteBuf content() {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    if (backing.refCnt() <= 0) {
      throw new IllegalReferenceCountException(backing.refCnt());
    }
    return backing;
  }

  @Override
  public ByteBufHolder copy() {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    return new DeferredByteBufHolder(backing.copy());
  }

  @Override
  public ByteBufHolder duplicate() {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    return new DeferredByteBufHolder(backing.duplicate());
  }

  @Override
  public ByteBufHolder retainedDuplicate() {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    return new DeferredByteBufHolder(backing.retainedDuplicate());
  }

  @Override
  public ByteBufHolder replace(final ByteBuf content) {
    if (content == null) {
      throw new NullPointerException("content");
    }
    backing = content;
    return this;
  }

  @Override
  public int refCnt() {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    return backing.refCnt();
  }

  @Override
  public ByteBufHolder retain() {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    backing.retain();
    return this;
  }

  @Override
  public ByteBufHolder retain(final int increment) {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    backing.retain(increment);
    return this;
  }

  @Override
  public ByteBufHolder touch() {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    backing.touch();
    return this;
  }

  @Override
  public ByteBufHolder touch(final Object hint) {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    backing.touch(hint);
    return this;
  }

  @Override
  public boolean release() {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    return backing.release();
  }

  @Override
  public boolean release(final int decrement) {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    return backing.release(decrement);
  }
}
