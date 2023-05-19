<html lang="en">
  <body>
    <h2>1.0 Introduction</h2>
    <p>
      The old version of Sonar had a lot of features but wasn't a good Antibot overall.
      <br>
      This is supposed to replace the old Sonar version.
      <br>
      It does not contain a ton of bloat and is supposed to be very light-weight.
    </p>
    <h2>1.1 Goals</h2>
    <ul>
      <li>
        Fast bot check
      </li>
      <li>
        Slow join prevention
      </li>
      <li>
        Multi-platform support
      </li>
      <li>
        No annoying captcha(s)
      </li>
      <li>
        No vpn/proxy check
      </li>
    </ul>
    <h2>2.0 Checks</h2>
    <p>
      Sonar operates using a one main check for handling incoming bot traffic: Fallback
      <br>
      Sonar's Fallback component is supposed to analyze a player's behavior before joining
      the actual server therefore stopping malicious traffic from ever getting to the back-end.
    </p>
    <p>
      The second component is in-game player analysis such as checking if the player joined
      is sending a legit amount of traffic to the server.
      This component does not act as an exploit-prevention system and will not protect you
      from in-game attacks using crash exploits.
    </p>
    <h2>2.1 How Sonar operates</h2>
    <p>
      Sonar hooks itself into the <a href="https://netty.io/4.1/api/io/netty/channel/ChannelPipeline.html">netty channel pipeline</a> of an
      incoming connection to analyse the traffic (packets) sent by the client.
      <br>
      However, Sonar also sends packets to check if the client responds accordingly.
      This only happens when the client has not been verified by Fallback before.
    </p>
    <h2>2.2 False positives</h2>
    <p>
      Sonar's in-game check is which analyses packet behavior of a client should theoretically
      not falsely punish a player since it only checks for <b>illegal</b> and <b>impossible</b> packets.
      <br>
      However, there are edge cases where the Sonar's Fallback might not receive a packet
      in the necessary time period. In that case, Sonar is trying to account for those edge
      cases and lag in order to prevent false blacklists.
    </p>
  </body>
</html>
