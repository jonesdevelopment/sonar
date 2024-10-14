package xyz.jonesdev.sonar.common.fallback.protocol.packets.configuration;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
@AllArgsConstructor
public final class EnabledFeaturesPacket implements FallbackPacket {

  private List<String> features = new ArrayList<>();

  public EnabledFeaturesPacket() {
    features.add("minecraft:vanilla");
  }

  @Override
  public void encode(ByteBuf byteBuf, ProtocolVersion protocolVersion) throws Exception {
    ProtocolUtil.writeVarInt(byteBuf, features.size());
    for (String feature : features) {
      ProtocolUtil.writeString(byteBuf, feature);
    }
  }

  @Override
  public void decode(ByteBuf byteBuf, ProtocolVersion protocolVersion) throws Exception {

  }
}
