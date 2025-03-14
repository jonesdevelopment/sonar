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

package xyz.jonesdev.sonar.captcha.legacy;

import com.jhlabs.image.TransformFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class RippleFilter extends TransformFilter {
  private float xAmplitude, yAmplitude;

  @Override
  protected void transformInverse(final int x, final int y, final float @NotNull [] out) {
    final float nx = (float) y / 13f;
    final float ny = (float) x / 13f;
    final float fx = (float) Math.sin(nx);
    final float fy = (float) Math.sin(ny);

    out[0] = (float) x + xAmplitude * fx;
    out[1] = (float) y + yAmplitude * fy;
  }
}
