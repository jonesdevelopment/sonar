import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
  id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

apply(plugin = "net.minecrell.plugin-yml.bukkit")

bukkit {
  name = "Sonar"
  version = rootProject.version.toString().split("-")[0]
  main = "xyz.jonesdev.sonar.bukkit.SonarBukkitPlugin"
  authors = listOf("Jones Development", "Sonar Contributors")
  website = "https://jonesdev.xyz/discord/"
  load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD

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
  compileOnly(project(":api"))
  compileOnly(project(":common"))

  // MiniMessage platform support
  implementation("net.kyori:adventure-platform-bukkit:4.3.1")

  // We have to use 1.8 for backwards compatibility
  compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")

  // Implement bStats.org for metrics
  implementation("org.bstats:bstats-bukkit:3.0.2")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8
