plugins {
    kotlin("jvm") version "1.8.21"
}

dependencies {
    compileOnly(project(":sonar-api"))

    implementation(kotlin("stdlib-jdk8"))
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

kotlin {
    jvmToolchain(8)
}