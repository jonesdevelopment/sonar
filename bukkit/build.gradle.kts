import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
  id("net.minecrell.plugin-yml.bukkit") version "0.5.3"
}

apply(plugin = "net.minecrell.plugin-yml.bukkit")

bukkit {
  name = rootProject.name
  version = rootProject.version.toString()
  main = "jones.sonar.bukkit.SonarBukkitPlugin"
  authors = listOf("Jones Development", "Sonar Contributors")
  website = "https://jonesdev.xyz/discord/"
  description = "Effective Anti-bot plugin for Velocity, BungeeCord, and Bukkit (1.7-latest)"
  load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD

  commands {
    register("sonar") {
      permission = "sonar.command"
    }
  }
}

repositories {
  maven(url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot
  maven(url = "https://oss.sonatype.org/content/repositories/snapshots/") // BungeeCord Chat API
}

dependencies {
  compileOnly(project(":sonar-api"))
  compileOnly(project(":sonar-common"))

  // We have to use 1.8 for backwards compatibility
  compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")

  // Implement bStats.org for metrics
  implementation("org.bstats:bstats-bukkit:3.0.2")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8
