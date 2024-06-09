<div align="center">
  <!-- Introduction -->
  <p>
    <h1>💫 Sonar</h1>
    Lightweight and easy-to-use anti-bot plugin for your Minecraft server. Supporting every client and server version.
    <br>
    An effective and extensible solution for protecting your Minecraft server against all kinds of bot attacks
  </p>
  
  <!-- Badges & icons -->
  [![](https://github.com/jonesdevelopment/sonar/actions/workflows/gradle.yml/badge.svg)](https://github.com/jonesdevelopment/sonar/actions/workflows/gradle.yml)
  [![](https://www.codefactor.io/repository/github/jonesdevelopment/sonar/badge/main)](https://www.codefactor.io/repository/github/jonesdevelopment/sonar/overview/main)
  [![](https://img.shields.io/github/v/release/jonesdevelopment/sonar)](https://github.com/jonesdevelopment/sonar/releases)
  [![](https://img.shields.io/github/issues/jonesdevelopment/sonar)](https://github.com/jonesdevelopment/sonar/issues)
  [![](https://img.shields.io/discord/923308209769426994.svg?logo=discord)](https://jonesdev.xyz/discord)
  [![](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
  <br>
  <br>
  <!-- Quick navigation -->
  [Releases](https://github.com/jonesdevelopment/sonar/releases)
  |
  [Issues](https://github.com/jonesdevelopment/sonar/issues)
  |
  [Pull Requests](https://github.com/jonesdevelopment/sonar/pulls)
  |
  [Discord](https://jonesdev.xyz/discord)
  |
  [License](https://github.com/jonesdevelopment/sonar/?tab=readme-ov-file#license)
</div>

## Design and Goal

* Effective, lightweight, and easy-to-use
* No unnecessary features and clean code
* Protection against all kinds of bot attacks
* No player should be annoyed by any sort of [CAPTCHA](https://en.wikipedia.org/wiki/CAPTCHA)
* No sort of checking for VPNs or proxies
* Multi-platform support (See [supported versions](https://docs.jonesdev.xyz/sonar/supported-versions))

## Checks

- [Fallback](https://github.com/jonesdevelopment/sonar#fallback) is Sonar's main anti-bot component
- Sonar queues new player logins to prevent spam login attacks
- Sonar checks the handshake packets for legitimacy
- Sonar makes sure some packets cannot be duplicated illegitimately

### Fallback

Fallback analyzes a player's behavior before joining the actual server, therefore stopping malicious traffic from ever reaching the backend.
It is supposed to be an instant, powerful, and invisible method of verification that should prevent all typical and advanced types of bots.

* Sends the player to a lightweight fake server when they connect for the first time.
* Analyzes if the player is sending the necessary packets.
* Analyzes if the player is sending legitimate packets.
* Checks if the player is obeying client gravity.
* Checks if the player is colliding with blocks correctly.

Fallback also protects against huge spambot attacks since it queues the incoming connections, therefore making it technically impossible to have a ton of bots join the server at the same time.

### False positives

Fallback is unlikely to ever falsely prevent a player from joining the server since Minecraft uses the TCP protocol, which means that packets are always sent in the correct order. Therefore, lag or ping should not affect the bot check. However, there are some edge cases where Fallback might not receive packets within the necessary time period. In this case, Sonar tries to account for these edge cases in order to prevent false positives. If you or one of your players experiences a false positive, make sure to report it by opening a [GitHub issue](https://github.com/jonesdevelopment/sonar/issues) or a ticket on [Discord](https://jonesdev.xyz/discord/).

## Building

If you want to build your own version of Sonar, please take a look at the [Sonar building documentation](https://docs.jonesdev.xyz/development/building).
<br>
You can also take a look at the [gradle documentation](https://docs.gradle.org/current/userguide/userguide.html) for a better understanding of Gradle.

## Contributing

If you are interested in contributing, you can check out the [Contributing Guidelines](https://github.com/jonesdevelopment/sonar/blob/main/.github/CONTRIBUTING.md) for detailed instructions.

## License

Sonar is licensed under the [GNU General Public License 3.0](https://www.gnu.org/licenses/gpl-3.0.en.html).

## Credits

- Special thanks to the [contributors of Sonar](https://github.com/jonesdevelopment/sonar/graphs/contributors).
- The Varint decoding was taken from [Velocity](https://github.com/PaperMC/Velocity).
