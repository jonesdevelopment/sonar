dependencies {
  // TODO: add these libraries/dependencies using a class loader
  implementation("com.zaxxer:HikariCP:4.0.3") // 5.0 doesn't support Java 8
  implementation("com.mysql:mysql-connector-j:8.0.33") // JDBC
  implementation("com.github.Carleslc.Simple-YAML:Simple-Yaml:1.8.4") // yaml config

  compileOnly("org.jetbrains:annotations:24.0.1")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8
