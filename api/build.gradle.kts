repositories {
  maven(url = "https://jitpack.io/") // simple-yaml
}

dependencies {
  compileOnly(rootProject.libs.simpleyaml) {
    exclude(group = "org.yaml")
  }
  compileOnly(rootProject.libs.annotations)
}

tasks {
  shadowJar {
    archiveFileName = "sonar-api-${rootProject.version}.jar"
  }
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11