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

package xyz.jonesdev.sonar.api.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DatabaseTable(tableName = "verified_players")
public final class VerifiedPlayer {
  @DatabaseField(generatedId = true)
  private int id;

  @DatabaseField(
    columnName = "ip_address",
    canBeNull = false,
    uniqueIndexName = "ip_address_player_uuid_idx",
    width = 16
  )
  private String inetAddress;

  @DatabaseField(
    columnName = "player_uuid",
    canBeNull = false,
    uniqueIndexName = "ip_address_player_uuid_idx",
    width = 36
  )
  private UUID playerUUID;

  @DatabaseField(columnName = "timestamp", canBeNull = false)
  private long timestamp;

  public VerifiedPlayer(final String inetAddress, final UUID playerUUID, final long timestamp) {
    this.inetAddress = inetAddress;
    this.playerUUID = playerUUID;
    this.timestamp = timestamp;
  }
}
