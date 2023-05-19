repositories {
    maven(url = "https://repo.papermc.io/repository/maven-public/") // Velocity
}

dependencies {
    implementation(project(":sonar-api"))
    implementation(project(":sonar-common"))
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17