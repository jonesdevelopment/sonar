plugins {
  id("net.minecrell.plugin-yml.bungee") version "0.6.0"
}

apply(plugin = "net.minecrell.plugin-yml.bungee")

bungee {
  name = rootProject.name
  version = rootProject.version.toString().split("-")[0]
  main = "xyz.jonesdev.sonar.bungee.SonarBungeePlugin"
  author = "Jones Development, Sonar Contributors"
  softDepends = setOf("Geyser-BungeeCord", "floodgate", "Protocolize", "ViaVersion", "packetevents", "FastLogin")
}

dependencies {
  implementation(project(":api"))
  implementation(project(":common"))

  compileOnly("net.md_5:bungeecord-proxy:master-SNAPSHOT")
  testCompileOnly("net.md_5:bungeecord-proxy:master-SNAPSHOT")

  // adventure platform support
  implementation("net.kyori:adventure-platform-bungeecord:4.3.2")
  implementation("net.kyori:adventure-platform-api:4.3.2")
  implementation("net.kyori:adventure-platform-facet:4.3.2")
  // adventure nbt
  implementation("net.kyori:adventure-nbt:4.17.0")

  // Implement bStats.org for metrics
  implementation("org.bstats:bstats-bungeecord:3.0.2")

  // Library/dependency loading
  implementation("com.alessiodp.libby:libby-bungee:2.0.0-SNAPSHOT")
}

tasks {
  shadowJar {
    relocate("net.kyori", "xyz.jonesdev.sonar.libs.kyori")
  }
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11
