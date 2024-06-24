plugins {
  id("net.minecrell.plugin-yml.bungee") version "0.6.0" apply true
}

bungee {
  name = rootProject.name
  description = rootProject.description
  version = rootProject.version.toString()
  main = "xyz.jonesdev.sonar.bungee.SonarBungeePlugin"
  author = "Jones Development, Sonar Contributors"
  softDepends = setOf("Geyser-BungeeCord", "floodgate", "Protocolize", "ViaVersion", "packetevents", "FastLogin")
}

dependencies {
  implementation(project(":api"))
  implementation(project(":common"))

  compileOnly(rootProject.libs.bungeecord)
  testCompileOnly(rootProject.libs.bungeecord)

  implementation(rootProject.libs.adventure.platform.bungee)
  implementation(rootProject.libs.adventure.platform.api)
  implementation(rootProject.libs.adventure.platform.facet)
  implementation(rootProject.libs.adventure.nbt)
  implementation(rootProject.libs.bstats.bungee)
  implementation(rootProject.libs.libby.bungee)
}

tasks {
  shadowJar {
    relocate("net.kyori", "xyz.jonesdev.sonar.libs.kyori")
  }
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11
