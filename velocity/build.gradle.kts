repositories {
    maven(url = "https://repo.papermc.io/repository/maven-public/") // Velocity
    maven(url = "https://maven.elytrium.net/repo/") // Velocity proxy module
}

val velocityVersion = "3.2.0-SNAPSHOT"

dependencies {
    implementation(project(":sonar-api"))
    implementation(project(":sonar-common"))

    compileOnly("com.velocitypowered:velocity-proxy:$velocityVersion") // Proxy module
    compileOnly("net.kyori:adventure-nbt:4.13.1") // nbt

    compileOnly("com.velocitypowered:velocity-api:$velocityVersion")
    annotationProcessor("com.velocitypowered:velocity-api:$velocityVersion")

    testCompileOnly("com.velocitypowered:velocity-api:$velocityVersion")
    testAnnotationProcessor("com.velocitypowered:velocity-api:$velocityVersion")
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17