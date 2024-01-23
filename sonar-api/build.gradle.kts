dependencies {
  compileOnly("com.github.Carleslc.Simple-YAML:Simple-Yaml:1.8.4") {
    exclude(group = "org.yaml")
  }

  compileOnly("org.jetbrains:annotations:24.0.1")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8
