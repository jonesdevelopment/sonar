package xyz.jonesdev.sonar.bungee.fallback;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.md_5.bungee.connection.InitialHandler;

@AllArgsConstructor
@Getter
public class DisconnectHandler extends ChannelDuplexHandler {

  private final FallbackInitialHandler initialHandler;
  private final InitialHandler original;

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    FallbackInitialHandler.recordMaps.remove(original, initialHandler);
    super.channelInactive(ctx);
  }
}
