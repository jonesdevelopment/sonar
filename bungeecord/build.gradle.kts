plugins {
  alias(libs.plugins.pluginyml.bungee) apply true
}

bungee {
  name = rootProject.name
  main = "xyz.jonesdev.sonar.bungee.SonarBungeePlugin"
  author = "Jones Development, Sonar Contributors"
  softDepends = setOf("Geyser-BungeeCord", "floodgate", "Protocolize", "ViaVersion", "packetevents", "FastLogin")
}

dependencies {
  implementation(project(":api"))
  implementation(project(":common"))

  compileOnly(rootProject.libs.bungeecord)
  testCompileOnly(rootProject.libs.bungeecord)

  implementation(rootProject.libs.adventure.platform.bungee) {
    exclude(module = "adventure-nbt")
  }
  implementation(rootProject.libs.bstats.bungee)
  implementation(rootProject.libs.libby.bungee)
}

tasks {
  shadowJar {
    relocate("net.kyori", "xyz.jonesdev.sonar.libs.kyori")

    archiveFileName = "Sonar-Bungee.jar" // Hardcode a shortened version of the name
  }
}
