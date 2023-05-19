import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("net.minecrell.plugin-yml.bukkit") version "0.5.3"
}

apply(plugin = "net.minecrell.plugin-yml.bukkit")

bukkit {
    name = rootProject.name
    version = rootProject.version.toString()
    main = "jones.sonar.bukkit.SonarBukkitPlugin"
    author = "jonesdev.xyz"
    website = "https://discord.jonesdev.xyz/"
    description = "Anti-bot plugin for Velocity, BungeeCord and Spigot (1.8-latest)"
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
}

repositories {
    maven(url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/") // BungeeCord Chat API
}

dependencies {
    implementation(project(":sonar-api"))
    implementation(project(":sonar-common"))

    // use 1.8 for backwards compatibility
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8