plugins {
  id("net.minecrell.plugin-yml.bungee") version "0.6.0"
}

apply(plugin = "net.minecrell.plugin-yml.bungee")

bungee {
  name = "Sonar"
  version = rootProject.version.toString().split("-")[0]
  main = "xyz.jonesdev.sonar.bungee.SonarBungeePlugin"
  author = "Jones Development, Sonar Contributors"
  softDepends = setOf("Geyser-BungeeCord", "floodgate", "Protocolize", "ViaVersion")
}

dependencies {
  compileOnly(project(":api"))
  compileOnly(project(":common"))

  compileOnly("net.md_5:bungeecord-proxy:master-SNAPSHOT")
  testCompileOnly("net.md_5:bungeecord-proxy:master-SNAPSHOT")

  // MiniMessage platform support
  implementation("net.kyori:adventure-platform-bungeecord:4.3.2")

  // Implement bStats.org for metrics
  implementation("org.bstats:bstats-bungeecord:3.0.2")

  // Library/dependency loading
  implementation("com.alessiodp.libby:libby-bungee:2.0.0-SNAPSHOT")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8
