dependencies {
  compileOnly(project(":sonar-api"))
  compileOnly(project(":sonar-common"))

  implementation("com.velocitypowered:velocity-native:1.1.9") {
    exclude(group = "com.google.guava")
    exclude(group = "io.netty")
    exclude(group = "org.checkerframework")
  }

  compileOnly("io.papermc:waterfall-proxy:1.20-R0.1-SNAPSHOT")

  // Implement bStats.org for metrics
  implementation("org.bstats:bstats-bungeecord:3.0.2")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8
