repositories {
    maven(url = "https://repo.papermc.io/repository/maven-public/") // Velocity
    maven(url = "https://maven.elytrium.net/repo/") // Velocity proxy module
}

dependencies {
    implementation(project(":sonar-api"))
    implementation(project(":sonar-common"))

    compileOnly("com.velocitypowered:velocity-proxy:3.2.0-SNAPSHOT") // Proxy module

    compileOnly("net.kyori:adventure-nbt:4.13.1") // Proxy module

    compileOnly("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")

    testCompileOnly("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
    testAnnotationProcessor("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17