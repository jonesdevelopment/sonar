val velocityVersion = "3.3.0-SNAPSHOT"

dependencies {
  implementation(project(":api"))
  implementation(project(":common"))

  compileOnly("com.velocitypowered:velocity-proxy:$velocityVersion")
  testCompileOnly("com.velocitypowered:velocity-proxy:$velocityVersion")

  // Implement bStats.org for metrics
  implementation("org.bstats:bstats-velocity:3.0.2")

  // Library/dependency loading
  implementation("com.alessiodp.libby:libby-velocity:2.0.0-SNAPSHOT")
}

tasks {
  processResources {
    val props = mapOf(
      "version" to rootProject.version.toString().split("-")[0],
      "description" to rootProject.description,
      "url" to "https://jonesdev.xyz/discord/",
      "main" to "xyz.jonesdev.sonar.velocity.SonarVelocityPlugin"
    )
    inputs.properties(props)
    filesMatching("velocity-plugin.json") {
      expand(props)
    }
  }
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17
