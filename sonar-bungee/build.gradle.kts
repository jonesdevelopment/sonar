dependencies {
  compileOnly(project(":api"))
  compileOnly(project(":common"))

  implementation("com.velocitypowered:velocity-native:1.1.9") {
    exclude(group = "com.google.guava")
    exclude(group = "io.netty")
    exclude(group = "org.checkerframework")
  }

  compileOnly("net.md_5:bungeecord:1.20.2-rc2-SNAPSHOT")

  // Implement bStats.org for metrics
  implementation("org.bstats:bstats-bungeecord:3.0.2")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8
