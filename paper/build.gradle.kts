import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
  alias(libs.plugins.pluginyml.paper) apply true
}

paper {
  name = rootProject.name
  main = "xyz.jonesdev.sonar.paper.SonarPaperPlugin"
  authors = listOf("Jones Development", "Sonar Contributors")
  website = "https://jonesdev.xyz/discord/"
  load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
  apiVersion = "1.21" // ignore legacy plugin warning
  foliaSupported = true

  serverDependencies {
    listOf("Geyser-Spigot", "floodgate", "Protocolize", "ProtocolSupport",
      "ViaVersion", "packetevents", "ProtocolLib", "FastLogin").forEach {
        register(it) {
          required = false
        }
    }
  }
}

java {
  // Modern Paper requires Java 21
  java.sourceCompatibility = JavaVersion.VERSION_21
  java.targetCompatibility = JavaVersion.VERSION_21
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
  maven(url = "https://repo.papermc.io/repository/maven-public/") // Paper
}

dependencies {
  implementation(project(":api"))
  implementation(project(":common"))
  implementation(project(":bukkit"))

  compileOnly(rootProject.libs.paper)

  implementation(rootProject.libs.bstats.bukkit)
  implementation(rootProject.libs.libby.paper)
}
