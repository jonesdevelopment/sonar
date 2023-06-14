plugins {
    kotlin("jvm") version "1.8.22"
}

repositories {
    maven(url = "https://repo.papermc.io/repository/maven-public/") // Velocity
    maven(url = "https://maven.elytrium.net/repo/") // Velocity proxy module // TODO: set up own repo
}

val velocityVersion = "3.2.0-SNAPSHOT"

dependencies {
    compileOnly(project(":sonar-api"))
    compileOnly(project(":sonar-common"))

    compileOnly("com.velocitypowered:velocity-proxy:$velocityVersion") // Proxy module

    compileOnly("net.kyori:adventure-nbt:4.14.0")

    compileOnly("com.velocitypowered:velocity-api:$velocityVersion")
    annotationProcessor("com.velocitypowered:velocity-api:$velocityVersion")

    testCompileOnly("com.velocitypowered:velocity-api:$velocityVersion")
    testAnnotationProcessor("com.velocitypowered:velocity-api:$velocityVersion")

    compileOnly(kotlin("stdlib-jdk8"))
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

kotlin {
    jvmToolchain(17)
}