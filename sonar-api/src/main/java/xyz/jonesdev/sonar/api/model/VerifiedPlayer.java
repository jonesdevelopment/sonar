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

package xyz.jonesdev.sonar.api.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.UUID;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DatabaseTable(tableName = "sonar_verified_players")
public final class VerifiedPlayer {
  @SuppressWarnings("unused")
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
  private UUID playerUuid;

  @DatabaseField(
    columnName = "timestamp",
    canBeNull = false
  )
  private Timestamp timestamp;

  public VerifiedPlayer(final @NotNull InetAddress inetAddress,
                        final @NotNull UUID playerUuid,
                        final long timestamp) {
    this(inetAddress.toString(), playerUuid, timestamp);
  }

  public VerifiedPlayer(final @NotNull String inetAddress,
                        final @NotNull UUID playerUuid,
                        final long timestamp) {
    this.inetAddress = inetAddress;
    this.playerUuid = playerUuid;
    this.timestamp = new Timestamp(timestamp);
  }
}
