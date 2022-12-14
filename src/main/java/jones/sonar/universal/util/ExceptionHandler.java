package jones.sonar.universal.util;

import io.netty.channel.Channel;
import jones.sonar.universal.blacklist.Blacklist;
import jones.sonar.universal.data.ServerStatistics;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.net.InetSocketAddress;

@UtilityClass
public class ExceptionHandler {
    public void handle(final Channel channel, final Throwable cause) {

        // forcibly close connections without using a future (delayed)
        channel.close();

        ServerStatistics.BLOCKED_CONNECTIONS++;

        // IOException can be thrown by disconnecting from the server
        // We need to exempt clients for that, so they won't get false blacklisted
        if (cause instanceof IOException) return;

        // blacklist the ip address
        Blacklist.addToBlacklist(((InetSocketAddress) channel.remoteAddress()).getAddress());
    }
}
