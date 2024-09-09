package xyz.jonesdev.sonar.common.fallback.protocol.packets.play;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class VehicleMovePacket implements FallbackPacket {
  private double x, y, z;
  private float yaw, pitch;

  @Override
  public void encode(ByteBuf byteBuf, ProtocolVersion protocolVersion) throws Exception {
    byteBuf.writeDouble(x);
    byteBuf.writeDouble(y);
    byteBuf.writeDouble(z);
    byteBuf.writeFloat(yaw);
    byteBuf.writeFloat(pitch);
  }

  @Override
  public void decode(ByteBuf byteBuf, ProtocolVersion protocolVersion) throws Exception {
    x = byteBuf.readDouble();
    y = byteBuf.readDouble();
    z = byteBuf.readDouble();
    yaw = byteBuf.readFloat();
    pitch = byteBuf.readFloat();
  }
}
