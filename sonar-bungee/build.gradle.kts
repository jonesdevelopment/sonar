plugins {
  id("net.minecrell.plugin-yml.bungee") version "0.6.0"
}

apply(plugin = "net.minecrell.plugin-yml.bungee")

bungee {
  name = "Sonar"
  version = rootProject.version.toString().split("-")[0]
  main = "xyz.jonesdev.sonar.bungee.SonarBungeePlugin"
  author = "Jones Development, Sonar Contributors"
}

dependencies {
  compileOnly(project(":api"))
  compileOnly(project(":common"))

  compileOnly("net.md_5:bungeecord:1.20.2-rc2-SNAPSHOT")

  // MiniMessage platform support
  implementation("net.kyori:adventure-platform-bungeecord:4.3.1")

  // Implement bStats.org for metrics
  implementation("org.bstats:bstats-bungeecord:3.0.2")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8
