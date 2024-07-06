repositories {
  maven(url = "https://jitpack.io/") // simple-yaml
}

dependencies {
  compileOnly(rootProject.libs.simpleyaml) {
    exclude(group = "org.yaml")
  }
  compileOnly(rootProject.libs.annotations)
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11
