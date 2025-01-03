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

package xyz.jonesdev.sonar.api.database.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;

@Getter
@ToString
@DatabaseTable(tableName = "sonar_fingerprints")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class VerifiedPlayer {
  @SuppressWarnings("unused")
  @DatabaseField(generatedId = true)
  private int id;

  @DatabaseField(
    columnName = "fingerprint",
    canBeNull = false,
    width = 48
  )
  private String fingerprint;

  @DatabaseField(
    columnName = "timestamp",
    canBeNull = false
  )
  private Timestamp timestamp;

  public VerifiedPlayer(final @NotNull String fingerprint,
                        final long timestamp) {
    this.fingerprint = fingerprint;
    this.timestamp = new Timestamp(timestamp);
  }
}
