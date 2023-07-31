dependencies {
  implementation("com.github.Carleslc.Simple-YAML:Simple-Yaml:1.8.4") // yaml config
  implementation("com.mysql:mysql-connector-j:8.1.0") // TODO: class loader

  compileOnly("org.jetbrains:annotations:24.0.1")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8
