dependencies {
  implementation("org.yaml:snakeyaml:2.0")
  implementation("com.zaxxer:HikariCP:4.0.3") // 5.0 doesn't support Java 8
  //implementation("com.mysql:mysql-connector-j:8.0.33")

  compileOnly("org.jetbrains:annotations:24.0.1")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8