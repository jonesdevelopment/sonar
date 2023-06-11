<html lang="en">
  <body>
    <h2>1.0 Introduction</h2>
    <p>
      Free software, high quality, and open-source;
      <br>
      Sonar efficiently scales to large networks and operates with minimal resources.
      <br>
      <p>
        The old version of Sonar had a ton of features, but it never really protected
        against advanced types of bots. This is going to change:
        <br>
        This version features more advanced checks and less bloated features
        and is designed to work on more than one type of server.
      </p>
    </p>
    <h2>1.1 Design</h2>
    <ul>
      <li>
        Effective and lightweight
      </li>
      <li>
        No complicated installation
      </li>
      <li>
        Avoid unnecessary features
      </li>
      <li>
        Protection against exploits
      </li>
      <li>
        Protection against bots
        <ul>
          <li>
            No annoying captcha
          </li>
          <li>
            No vpn or proxy check
          </li>
        </ul>
      </li>
      <li>
        Multi-platform support
      </li>
    </ul>
    <h2>2.0 Checks</h2>
    <p>
      Sonar has one main component called Fallback.
      <br>
      Fallback analyzes a player's behavior before joining the actual server; therefore
      stopping malicious traffic from ever getting to the backend.
      <br>
      It is supposed to be an instant, powerful, and invisible method of verification
      which should prevent all typical and advanced types of bots.
      <br>
      <h3>Fallback</h3>
      <ul>
        <li>
          Sends the player to a lightweight fake server when they first connect
        </li>
        <li>
          Analyzes if the player is sending the necessary packets
        </li>
        <li>
          Analyzes if the player is sending legitimate packets
        </li>
        <li>
          Checks if the player is obeying client gravity
        </li>
        <li>
          Redirect the player to the backend server without them actually noticing that they
          were checked for being a bot
        </li>
      </ul>
      Fallback also protects from huge spam bot attacks since it queues the incoming connections,
      therefore making it technically impossible to make a ton of bots join the server at the same time.
    </p>
    <p>
      Exploit prevention is done by checking for exceptions within the pipeline to ensure
      the player is sending the correct packets to the server.
      <br>
      Note: This does not block backend attacks and should not be used as a first line of defense.
    </p>
    <h2>2.1 How Sonar operates</h2>
    <p>
      Sonar hooks itself into the <a href="https://netty.io/4.1/api/io/netty/channel/ChannelPipeline.html">netty channel pipeline</a> of an
      incoming connection to analyze the traffic (packets) sent by the client.
      <br>
      However, Sonar also sends packets to check if the client responds accordingly.
      This only happens when the client has not been verified by Fallback before.
    </p>
    <h2>2.2 False positives</h2>
    <p>
      Fallback is unlikely to ever falsely prevent a player from joining the server
      since Minecraft uses the TCP protocol which means that packets are always sent in the
      correct order. Lag should not affect the bot check.
      <br>
      However, there are edge cases where the Sonar's Fallback might not receive a packet
      in a necessary time period. In that case, Sonar is trying to account for those edge
      cases in order to prevent false positives.
      <br>
      If you or one of your players experiences a false positive, make sure to report them
      by opening a <a href="https://github.com/jonesdevelopment/sonar-antibot/issues">GitHub issue</a> or a ticket on the <a href="https://discord.jonesdev.xyz/">Discord server</a>.
    </p>
    <h2>3.0 Building</h2>
    <p>
      You can build the jar using <code>gradle shadowJar</code> in the main project.
      <br>
      If there are any issues, you can also try <code>gradle shadowJar --stacktrace</code>
      or <code>gradle shadowJar --debug</code>.
      <br>
      You can also take a look at the <a href="https://docs.gradle.org/current/userguide/userguide.html">gradle documentation</a>.
    </p>
    <h2>3.1 Contributing</h2>
    Pull requests are welcome, but please follow some simple rules in order for your
    pull request to be merged:
    <br>
    <ul>
      <li>
        Test your code before you commit and push it
      </li>
      <li>
        Try to use the same code style as the rest of the project
      </li>
      <li>
        Try to avoid bloat - Sonar is supposed to be lightweight
      </li>
    </ul>
    <h2>3.2 License</h2>
    <p>
      Sonar is licensed under the GNU General Public License 3.0.
    </p>
    <a href="https://www.gnu.org/licenses/gpl-3.0"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg"/></a>
    <h2>3.3 Credits</h2>
    <p>
      Special thanks to the <a href="https://github.com/jonesdevelopment/sonar-antibot/graphs/contributors">contributors of Sonar</a>.
      <br>
      Parts of the Java Reflections used by the <a href="https://github.com/jonesdevelopment/sonar-antibot/tree/main/velocity">velocity</a> module were taken from <a href="https://github.com/Elytrium/LimboAPI">LimboAPI</a>.
      <br>
      <a href="https://github.com/jonesdevelopment/sonar-antibot/blob/main/common/src/main/java/jones/sonar/common/fallback/dimension/PacketDimension.java">PacketDimension</a> was taken from <a href="https://github.com/Elytrium/LimboAPI/blob/master/api/src/main/java/net/elytrium/limboapi/api/chunk/Dimension.java">LimboAPI/Dimension</a>
      <br>
      <a href="https://github.com/jonesdevelopment/sonar-antibot/blob/main/velocity/src/main/java/jones/sonar/velocity/fallback/dimension/Biome.java">Biome</a> was taken from <a href="https://github.com/Elytrium/LimboAPI/blob/master/plugin/src/main/java/net/elytrium/limboapi/material/Biome.java">LimboAPI/Biome</a>
      <br>
      Parts of the Yaml configuration system used by the <a href="https://github.com/jonesdevelopment/sonar-antibot/tree/main/api">api</a> module were taken from <a href="https://github.com/SpigotMC">SpigotMC</a>'s Yaml configuration system.
    </p> 
  </body>
</html>
