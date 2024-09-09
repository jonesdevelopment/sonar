package xyz.jonesdev.sonar.common.fallback.protocol.packets.play;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class EntityAnimationPacket implements FallbackPacket {
  private int entityId;
  private Type type;

  @Override
  public void encode(ByteBuf byteBuf, ProtocolVersion protocolVersion) throws Exception {
    ProtocolUtil.writeVarInt(byteBuf, entityId);
    byteBuf.writeByte(type.ordinal());
  }

  @Override
  public void decode(ByteBuf byteBuf, ProtocolVersion protocolVersion) throws Exception {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("SpellCheckingInspection")
  public enum Type {
    SWING_MAIN_ARM,
    HURT,
    WAKE_UP,
    // 1.9+?
    SWING_OFF_HAND, // Eat food on 1.7
    CRITICAL_HIT,
    MAGIC_CRITICAL_HIT
    // unknown (102), crouch (104), uncrouch(105) only exist on 1.7 and unused here
  }
}
