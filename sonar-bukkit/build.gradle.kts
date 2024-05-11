import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
  id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

apply(plugin = "net.minecrell.plugin-yml.bukkit")

bukkit {
  name = rootProject.name
  version = rootProject.version.toString().split("-")[0]
  main = "xyz.jonesdev.sonar.bukkit.SonarBukkitPlugin"
  authors = listOf("Jones Development", "Sonar Contributors")
  website = "https://jonesdev.xyz/discord/"
  load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
  softDepend = listOf("Geyser-Spigot", "floodgate", "Protocolize", "ProtocolSupport",
    "ViaVersion", "packetevents", "ProtocolLib", "FastLogin")
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

  // adventure platform support
  implementation("net.kyori:adventure-platform-bukkit:4.3.2")
  implementation("net.kyori:adventure-platform-api:4.3.2")
  implementation("net.kyori:adventure-platform-facet:4.3.2")

  // adventure minimessage
  compileOnly("net.kyori:adventure-text-minimessage:4.16.0")
  compileOnly("net.kyori:adventure-text-serializer-gson:4.16.0")
  // adventure nbt
  implementation("net.kyori:adventure-nbt:4.16.0")

  // We have to use 1.8 for backwards compatibility
  compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")

  // Implement bStats.org for metrics
  implementation("org.bstats:bstats-bukkit:3.0.2")

  // Library/dependency loading
  implementation("com.alessiodp.libby:libby-bukkit:2.0.0-SNAPSHOT")
}

tasks {
  shadowJar {
    relocate("net.kyori", "xyz.jonesdev.sonar.libs.kyori")
  }
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11
