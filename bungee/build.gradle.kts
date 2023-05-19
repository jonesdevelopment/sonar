repositories {
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/") // BungeeCord
}

dependencies {
    implementation(project(":sonar-api"))
    implementation(project(":sonar-common"))
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8