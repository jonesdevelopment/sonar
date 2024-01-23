package xyz.jonesdev.sonar.bungee.fallback.handler;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.protocol.packet.LoginRequest;

public abstract class ReplaceInitialHandler extends DummyInitialHandler {
  public ReplaceInitialHandler(BungeeCord bungee, ListenerInfo listener) {
    super(bungee, listener);
  }

  @Override
  public void handle(LoginRequest loginRequest) throws Exception {
    super.handle(loginRequest);
    getChannelWrapper().getHandle().pipeline().get(HandlerBoss.class).setHandler(getTarget());
  }
}
