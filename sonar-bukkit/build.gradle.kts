import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
  id("net.minecrell.plugin-yml.bukkit") version "0.6.0" apply true
}

bukkit {
  name = rootProject.name
  description = rootProject.description
  version = rootProject.version.toString()
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

  compileOnly(rootProject.libs.spigot)

  implementation(rootProject.libs.adventure.platform.bukkit)
  implementation(rootProject.libs.adventure.platform.api)
  implementation(rootProject.libs.adventure.platform.facet)
  implementation(rootProject.libs.adventure.nbt)
  implementation(rootProject.libs.bstats.bukkit)
  implementation(rootProject.libs.libby.bukkit)
}

tasks {
  shadowJar {
    relocate("net.kyori", "xyz.jonesdev.sonar.libs.kyori")
  }
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11
