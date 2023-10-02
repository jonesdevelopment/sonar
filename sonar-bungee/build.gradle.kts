plugins {
  id("net.minecrell.plugin-yml.bungee") version "0.6.0"
}

apply(plugin = "net.minecrell.plugin-yml.bungee")

bungee {
  name = rootProject.name
  version = rootProject.version.toString().split("-")[0]
  main = "xyz.jonesdev.sonar.bungee.SonarBungeePlugin"
  author = "Jones Development, Sonar Contributors"
  description = "Effective Anti-bot plugin for Velocity, BungeeCord, and Bukkit (1.7-latest)"
}

dependencies {
  compileOnly(project(":api"))
  compileOnly(project(":common"))

  implementation("com.velocitypowered:velocity-native:1.1.9") {
    exclude(group = "com.google.guava")
    exclude(group = "io.netty")
    exclude(group = "org.checkerframework")
  }

  compileOnly("net.md_5:bungeecord:1.20.2-rc2-SNAPSHOT")

  // MiniMessage platform support
  implementation("net.kyori:adventure-platform-bungeecord:4.3.1")

  // Implement bStats.org for metrics
  implementation("org.bstats:bstats-bungeecord:3.0.2")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8
