<div align="center">
  <!-- Introduction -->
  <h1>💫 Sonar</h1>
  Lightweight and easy-to-use anti-bot plugin for your Minecraft server.
  <br>
  Sonar keeps your server safe from common and even sophisticated bot attacks.
  <br><br>

  <!-- Badges & icons -->
  [![](https://github.com/jonesdevelopment/sonar/actions/workflows/gradle.yml/badge.svg)](https://github.com/jonesdevelopment/sonar/actions/workflows/gradle.yml)
  [![](https://www.codefactor.io/repository/github/jonesdevelopment/sonar/badge/main)](https://www.codefactor.io/repository/github/jonesdevelopment/sonar/overview/main)
  [![](https://img.shields.io/github/v/release/jonesdevelopment/sonar)](https://github.com/jonesdevelopment/sonar/releases)
  [![](https://img.shields.io/github/issues/jonesdevelopment/sonar)](https://github.com/jonesdevelopment/sonar/issues)
  [![](https://img.shields.io/discord/923308209769426994.svg?logo=discord)](https://jonesdev.xyz/discord)
  [![](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
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

> ### Notice about Sonar 3.0
> 
> We're excited to announce the public release of Sonar 3.0.
> If you are interested, please check out https://sonar.top/. You can read more about what sets Sonar 3.0 apart from Sonar 2.0 [here](<https://docs.sonar.top/faq>).
> If you want more robust protection, better performance, and awesome new features, check out Sonar 3.0!

## Design and Goal
* Effective, lightweight, and easy-to-use
* No unnecessary features and clean code
* Protection against all kinds of bot attacks
* No player should be annoyed by any sort of [CAPTCHA](https://en.wikipedia.org/wiki/CAPTCHA)
* No sort of checking for VPNs or proxies
* Multi-platform support (See [supported versions](https://docs.jonesdev.xyz/sonar/supported-versions))

## Checks
Sonar analyzes a player's behavior before joining the actual server, therefore stopping malicious traffic from ever reaching the backend. It is supposed to be an instant, powerful, and simple method of verification that should prevent all typical and advanced types of bots.

1. Sonar sends the player to a lightweight fake server when they connect for the first time.
2. Sonar verifies that players obey the laws of Minecraft's physics, including gravity and proper block collision.
3. Sonar verifies that players send legitimate packets when interacting with vehicles (e.g. boats).
4. Sonar makes sure that players send legitimate packets according to the [vanilla Minecraft protocol](<https://wiki.vg/Protocol>).

Sonar also protects against spambot attacks since it queues the incoming connections, therefore making it technically impossible to have a ton of bots join the server at the same time.

### False positives
Sonar is unlikely to ever falsely prevent a player from joining the server since Minecraft uses the TCP protocol, which means that packets are always sent in the correct order. Therefore, lag or ping should not affect the bot check.
If you or one of your players experiences a false positive, make sure to report it by opening a [GitHub issue](https://github.com/jonesdevelopment/sonar/issues/new/choose) or a ticket on the [Discord server](https://jonesdev.xyz/discord/).

## Sponsors
Massive thanks to the sponsors of Sonar who help keep this project running:

<a href="https://github.com/Hydoxl"><img src="https://images.weserv.nl/?url=avatars.githubusercontent.com/u/107579333?v=4&h=50&w=50&fit=cover&mask=circle&maxage=7d" alt="logo" align="center"></a>

### Past Sponsors

<a href="https://github.com/ItzErpandX"><img src="https://images.weserv.nl/?url=avatars.githubusercontent.com/u/84748484?v=4?s=400?v=4&h=50&w=50&fit=cover&mask=circle&maxage=7d" alt="logo" align="center"></a>

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
