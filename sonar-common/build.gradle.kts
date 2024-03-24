dependencies {
  compileOnly(project(":api"))

  implementation("xyz.jonesdev.capja:capja:0.1.0")
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11
