import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
  alias(libs.plugins.pluginyml.bukkit) apply true
}

bukkit {
  name = rootProject.name
  main = "xyz.jonesdev.sonar.bukkit.SonarBukkitPlugin"
  authors = listOf("Jones Development", "Sonar Contributors")
  website = "https://sonar.top/discord/"
  load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
  softDepend = listOf("Geyser-Spigot", "floodgate", "Protocolize", "ProtocolSupport",
    "ViaVersion", "packetevents", "ProtocolLib", "FastLogin")
  apiVersion = "1.13" // ignore legacy plugin warning
  foliaSupported = true

  commands {
    register("sonar") {
      // No permission checks here
    }
  }
}

repositories {
  maven(url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot
  maven(url = "https://oss.sonatype.org/content/repositories/snapshots/") // BungeeCord Chat API
}

dependencies {
  implementation(project(":api"))
  implementation(project(":common"))

  compileOnly(rootProject.libs.spigot)

  implementation(rootProject.libs.adventure.platform.bukkit) {
    exclude(module = "adventure-nbt")
  }
  implementation(rootProject.libs.bstats.bukkit)
  implementation(rootProject.libs.libby.bukkit)
}

tasks {
  shadowJar {
    relocate("net.kyori", "xyz.jonesdev.sonar.libs.kyori")
  }
}
