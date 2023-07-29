val velocityVersion = "3.2.0-SNAPSHOT"

dependencies {
  compileOnly(project(":api"))
  compileOnly(project(":common"))

  compileOnly("com.velocitypowered:velocity-proxy:$velocityVersion") // Proxy module

  compileOnly("com.velocitypowered:velocity-api:$velocityVersion")
  annotationProcessor("com.velocitypowered:velocity-api:$velocityVersion")

  testCompileOnly("com.velocitypowered:velocity-api:$velocityVersion")
  testAnnotationProcessor("com.velocitypowered:velocity-api:$velocityVersion")

  // Implement bStats.org for metrics
  implementation("org.bstats:bstats-velocity:3.0.2")
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11
