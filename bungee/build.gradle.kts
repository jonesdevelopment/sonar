repositories {
  maven(url = "https://oss.sonatype.org/content/repositories/snapshots/") // BungeeCord
}

dependencies {
  compileOnly(project(":sonar-api"))
  compileOnly(project(":sonar-common"))

  compileOnly("net.md-5:bungeecord-api:1.20-R0.1-SNAPSHOT")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8