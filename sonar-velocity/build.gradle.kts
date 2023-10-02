val velocityVersion = "3.2.0-SNAPSHOT"

dependencies {
  compileOnly(project(":api"))
  compileOnly(project(":common"))

  compileOnly("com.velocitypowered:velocity-proxy:$velocityVersion")
  testCompileOnly("com.velocitypowered:velocity-proxy:$velocityVersion")

  compileOnly("com.velocitypowered:velocity-api:$velocityVersion")
  //annotationProcessor("com.velocitypowered:velocity-api:$velocityVersion")

  testCompileOnly("com.velocitypowered:velocity-api:$velocityVersion")
  //testAnnotationProcessor("com.velocitypowered:velocity-api:$velocityVersion")

  // Implement bStats.org for metrics
  implementation("org.bstats:bstats-velocity:3.0.2")
}

tasks {
  processResources {
    val props = mapOf(
      "name" to rootProject.name,
      "version" to rootProject.version.toString().split("-")[0],
      "description" to "Effective Anti-bot plugin for Velocity, BungeeCord, and Bukkit (1.7-latest)",
      "url" to "https://jonesdev.xyz/discord/",
      "main" to "xyz.jonesdev.sonar.velocity.SonarVelocityPlugin"
    )
    inputs.properties(props)
    filesMatching("velocity-plugin.json") {
      expand(props)
    }
  }
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11
