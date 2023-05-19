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
    description = "Sonar anti bot"
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
}

dependencies {
    implementation(project(":sonar-api"))
    implementation(project(":sonar-common"))
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8