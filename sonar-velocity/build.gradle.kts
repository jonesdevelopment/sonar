dependencies {
  implementation(project(":api"))
  implementation(project(":common"))

  compileOnly(rootProject.libs.velocity)
  testCompileOnly(rootProject.libs.velocity)

  implementation(rootProject.libs.bstats.velocity)
  implementation(rootProject.libs.libby.velocity)
}

tasks {
  processResources {
    val props = mapOf(
      "version" to rootProject.version.toString(),
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
