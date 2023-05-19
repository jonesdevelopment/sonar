plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.github.johnrengelman.shadow")

    repositories {
        mavenCentral() // Lombok
    }

    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.26")
        annotationProcessor("org.projectlombok:lombok:1.18.26")

        testCompileOnly("org.projectlombok:lombok:1.18.26")
        testAnnotationProcessor("org.projectlombok:lombok:1.18.26")
    }
}

dependencies {
    sequenceOf("api", "bukkit", "bungee", "common", "velocity").forEach {
        implementation(project(":sonar-$it"))
    }
}