![](https://img.shields.io/github/v/release/jonesdevelopment/sonar-antibot) ![](https://img.shields.io/github/issues/jonesdevelopment/sonar-antibot) ![](https://img.shields.io/github/downloads/jonesdevelopment/sonar-antibot/total)

## Introduction

The old version of Sonar had a ton of features, but it never really protected against advanced types of bots.
This version features more advanced checks and fewer bloated features, and is designed to work on more than one type of
server.

Please note that this version is still in development and may have bugs and other issues.
If you want to report a bug or issue, please open
a [GitHub issue](https://github.com/jonesdevelopment/sonar-antibot/issues) or ticket
on [Discord](https://jonesdev.xyz/discord/).
If you encounter a false positive, please
read [this](https://github.com/jonesdevelopment/sonar-antibot/tree/main#false-positives).
Make sure to join the Jones Development [Discord](https://jonesdev.xyz/discord/) in order to receive update
notifications.

## Design

* Effective and lightweight
* No complicated installation
* No unnecessary features
* Protection against exploits
* Protection against bots
  * No annoying captchas
  * No VPN or proxy check
* Multi-platform support

## Checks

- **Fallback** is designed to prevent all types of bots.
- Exceptions occurring in the [pipeline](https://netty.io/4.1/api/io/netty/channel/ChannelPipeline.html) are handled
  accordingly.
- Spigot & BungeeCord
  - The compression method is changed
    to [Velocity's libdeflate](https://github.com/PaperMC/Velocity/tree/dev/3.0.0/native).
  - The Varint decoder is updated to Velocity's
    improved [MinecraftVarintFrameDecoder](https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/protocol/netty/MinecraftVarintFrameDecoder.java).

## Fallback

Fallback analyzes a player's behavior before joining the actual server, therefore stopping malicious traffic from ever
reaching the backend.
It is supposed to be an instant, powerful, and invisible method of verification that should prevent all typical and
advanced types of bots.

* Sends the player to a lightweight fake server when they connect for the first time.
* Analyzes if the player is sending the necessary packets.
* Analyzes if the player is sending legitimate packets.
* ~~Checks if the player is obeying client gravity.~~ (planned for a future update)
* Redirects the player to the backend server without them actually noticing that they were checked for being a bot.

Fallback also protects from huge spam bot attacks since it queues the incoming connections, therefore making it
technically impossible to have a ton of bots join the server at the same time.

## False positives

Fallback is unlikely to ever falsely prevent a player from joining the server since Minecraft uses the TCP protocol
which means that packets are always sent in the correct order. Lag should not affect the bot check.
However, there are some edge cases where Fallback might not receive packets within the necessary time period. In that
case, Sonar tries to account for these edge cases in order to prevent false positives. For example, some higher
Minecraft versions have a bug where the client sometimes sends a packet out of order. Sonar accounts for that and does
not falsely blacklist clients.
If you or one of your players experiences a false positive, make sure to report it by opening
a [GitHub issue](https://github.com/jonesdevelopment/sonar-antibot/issues) or a ticket
on [Discord](https://jonesdev.xyz/discord/).

## Building

- You can build the jar file using `gradle build-sonar` in the main project.
  - If this does not work, try `gradle shadowJar` as a fallback option.
- To clean temporary files and build files, use `gradle clean`.
- If there are any issues, you can also try `gradle shadowJar --stacktrace` to see exceptions.

You can also take a look at the [gradle documentation](https://docs.gradle.org/current/userguide/userguide.html).

## Contributing

If you are interested in contributing, you can check out
the [Contributing Guidelines](https://github.com/jonesdevelopment/sonar-antibot/blob/main/CONTRIBUTING.md) for detailed
instructions.

## License

Sonar is licensed under the GNU General Public License 3.0.

[![](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

## Credits

- Special thanks to the [contributors of Sonar](https://github.com/jonesdevelopment/sonar-antibot/graphs/contributors).
- The nbt mappings were taken from [LimboAPI](https://github.com/Elytrium/LimboAPI).
- The compression and Varint decoding was taken from [Velocity](https://github.com/PaperMC/Velocity).
