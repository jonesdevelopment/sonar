repositories {
  maven(url = "https://repo.opencollab.dev/main") // Floodgate API
}

dependencies {
  compileOnly(project(":api"))

  compileOnly("org.geysermc.floodgate:api:2.2.0-SNAPSHOT")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8
