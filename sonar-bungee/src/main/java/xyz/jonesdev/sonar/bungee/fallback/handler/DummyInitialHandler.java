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

package xyz.jonesdev.sonar.bungee.fallback.handler;

import lombok.Getter;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Set;
import java.util.UUID;

@Getter
public class DummyInitialHandler extends InitialHandler {

  private final InitialHandler target;
  @Getter
  private ChannelWrapper channelWrapper;

  public DummyInitialHandler(BungeeCord bungee, ListenerInfo listener) {
    this(bungee, listener, new InitialHandler(bungee, listener));
  }

  public DummyInitialHandler(BungeeCord bungee, ListenerInfo listener, InitialHandler target) {
    super(bungee, listener);
    this.target=target;
  }

  @Override
  public Handshake getHandshake() {
    return target.getHandshake();
  }

  @Override
  public String toString() {
    return target.toString();
  }

  @Override
  public boolean isConnected() {
    return target.isConnected();
  }

  @Override
  public boolean isLegacy() {
    return target.isLegacy();
  }

  @Override
  public boolean isOnlineMode() {
    return target.isOnlineMode();
  }

  @Override
  public InetSocketAddress getAddress() {
    return target.getAddress();
  }

  @Override
  public InetSocketAddress getVirtualHost() {
    return target.getVirtualHost();
  }

  @Override
  public int getVersion() {
    return target.getVersion();
  }

  @Override
  public ListenerInfo getListener() {
    return target.getListener();
  }

  @Override
  public LoginRequest getLoginRequest() {
    return target.getLoginRequest();
  }

  @Override
  public LoginResult getLoginProfile() {
    return target.getLoginProfile();
  }

  @Override
  public PluginMessage getBrandMessage() {
    return target.getBrandMessage();
  }

  @Override
  public Set<String> getRegisteredChannels() {
    return target.getRegisteredChannels();
  }

  @Override
  public SocketAddress getSocketAddress() {
    return target.getSocketAddress();
  }

  @Override
  public String getExtraDataInHandshake() {
    return target.getExtraDataInHandshake();
  }

  @Override
  public String getName() {
    return target.getName();
  }

  @Override
  public String getUUID() {
    return target.getUUID();
  }

  @Override
  public UUID getOfflineId() {
    return target.getOfflineId();
  }

  @Override
  public UUID getUniqueId() {
    return target.getUniqueId();
  }

  @Override
  public void relayMessage(PluginMessage input) throws Exception {
    target.relayMessage(input);
  }

  @Override
  public void writabilityChanged(ChannelWrapper channel) throws Exception {
    target.writabilityChanged(channel);
  }

  @Override
  public boolean shouldHandle(PacketWrapper packet) throws Exception {
    return target.shouldHandle(packet);
  }

  @Override
  public void disconnect(BaseComponent... reason) {
    target.disconnect(reason);
  }

  @Override
  public void disconnect(String reason) {
    target.disconnect(reason);
  }

  @Override
  public void disconnect(BaseComponent reason) {
    target.disconnect(reason);
  }

  @Override
  public void disconnected(ChannelWrapper channel) throws Exception {
    this.channelWrapper=channel;
    target.disconnected(channel);
  }

  @Override
  public Unsafe unsafe() {
    return target.unsafe();
  }

  @Override
  public void setUniqueId(UUID uuid) {
    target.setUniqueId(uuid);
  }

  @Override
  public void setOnlineMode(boolean onlineMode) {
    target.setOnlineMode(onlineMode);
  }

  @Override
  public void connected(ChannelWrapper channel) throws Exception {
    target.connected(channel);
  }

  @Override
  public void exception(Throwable t) throws Exception {
    target.exception(t);
  }

  @Override
  public void handle(Chat chat) throws Exception {
    target.handle(chat);
  }

  @Override
  public void handle(Kick kick) throws Exception {
    target.handle(kick);
  }

  @Override
  public void handle(Team team) throws Exception {
    target.handle(team);
  }

  @Override
  public void handle(Login login) throws Exception {
    target.handle(login);
  }

  @Override
  public void handle(Title title) throws Exception {
    target.handle(title);
  }

  @Override
  public void handle(Subtitle title) throws Exception {
    target.handle(title);
  }

  @Override
  public void handle(BossBar bossBar) throws Exception {
    target.handle(bossBar);
  }

  @Override
  public void handle(ClientChat chat) throws Exception {
    target.handle(chat);
  }

  @Override
  public void handle(LegacyPing ping) throws Exception {
    target.handle(ping);
  }

  @Override
  public void handle(PingPacket ping) throws Exception {
    target.handle(ping);
  }

  @Override
  public void handle(Respawn respawn) throws Exception {
    target.handle(respawn);
  }

  @Override
  public void handle(SystemChat chat) throws Exception {
    target.handle(chat);
  }

  @Override
  public void handle(TitleTimes title) throws Exception {
    target.handle(title);
  }

  @Override
  public void handle(ClearTitles title) throws Exception {
    target.handle(title);
  }

  @Override
  public void handle(Commands commands) throws Exception {
    target.handle(commands);
  }

  @Override
  public void handle(EntityStatus status) throws Exception {
    target.handle(status);
  }

  @Override
  public void handle(GameState gameState) throws Exception {
    target.handle(gameState);
  }

  @Override
  public void handle(Handshake handshake) throws Exception {
    target.handle(handshake);
  }

  @Override
  public void handle(KeepAlive keepAlive) throws Exception {
    target.handle(keepAlive);
  }

  @Override
  public void handle(PacketWrapper packet) throws Exception {
    target.handle(packet);
  }

  @Override
  public void handle(ClientCommand command) throws Exception {
    target.handle(command);
  }

  @Override
  public void handle(ServerData serverData) throws Exception {
    target.handle(serverData);
  }

  @Override
  public void handle(ClientSettings settings) throws Exception {
    target.handle(settings);
  }

  @Override
  public void handle(ClientStatus clientStatus) throws Exception {
    target.handle(clientStatus);
  }

  @Override
  public void handle(LoginRequest loginRequest) throws Exception {
    target.handle(loginRequest);
  }

  @Override
  public void handle(LoginSuccess loginSuccess) throws Exception {
    target.handle(loginSuccess);
  }

  @Override
  public void handle(ViewDistance viewDistance) throws Exception {
    target.handle(viewDistance);
  }

  @Override
  public void handle(LoginPayloadRequest request) throws Exception {
    target.handle(request);
  }

  @Override
  public void handle(PluginMessage pluginMessage) throws Exception {
    target.handle(pluginMessage);
  }

  @Override
  public void handle(StatusRequest statusRequest) throws Exception {
    target.handle(statusRequest);
  }

  @Override
  public void handle(LoginPayloadResponse response) throws Exception {
    target.handle(response);
  }

  @Override
  public void handle(PlayerListItem playerListItem) throws Exception {
    target.handle(playerListItem);
  }

  @Override
  public void handle(SetCompression setCompression) throws Exception {
    target.handle(setCompression);
  }

  @Override
  public void handle(StatusResponse statusResponse) throws Exception {
    target.handle(statusResponse);
  }

  @Override
  public void handle(TabCompleteRequest tabComplete) throws Exception {
    target.handle(tabComplete);
  }

  @Override
  public void handle(LegacyHandshake legacyHandshake) throws Exception {
    target.handle(legacyHandshake);
  }

  @Override
  public void handle(ScoreboardScore scoreboardScore) throws Exception {
    target.handle(scoreboardScore);
  }

  @Override
  public void handle(TabCompleteResponse tabResponse) throws Exception {
    target.handle(tabResponse);
  }

  @Override
  public void handle(EncryptionResponse encryptResponse) throws Exception {
    target.handle(encryptResponse);
  }

  @Override
  public void handle(EncryptionRequest encryptionRequest) throws Exception {
    target.handle(encryptionRequest);
  }

  @Override
  public void handle(LoginAcknowledged loginAcknowledged) throws Exception {
    target.handle(loginAcknowledged);
  }

  @Override
  public void handle(PlayerListItemRemove playerListItem) throws Exception {
    target.handle(playerListItem);
  }

  @Override
  public void handle(PlayerListItemUpdate playerListItem) throws Exception {
    target.handle(playerListItem);
  }

  @Override
  public void handle(ScoreboardDisplay displayScoreboard) throws Exception {
    target.handle(displayScoreboard);
  }

  @Override
  public void handle(StartConfiguration startConfiguration) throws Exception {
    target.handle(startConfiguration);
  }

  @Override
  public void handle(FinishConfiguration finishConfiguration) throws Exception {
    target.handle(finishConfiguration);
  }

  @Override
  public void handle(ScoreboardObjective scoreboardObjective) throws Exception {
    target.handle(scoreboardObjective);
  }

  @Override
  public void handle(ScoreboardScoreReset scoreboardScoreReset) throws Exception {
    target.handle(scoreboardScoreReset);
  }

  @Override
  public void handle(PlayerListHeaderFooter playerListHeaderFooter) throws Exception {
    target.handle(playerListHeaderFooter);
  }
}
