buildscript {
  dependencies {
    classpath("gradle.plugin.io.toolebox:gradle-git-versioner:1.6.7")
  }
}

plugins {
  id("java")
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("io.toolebox.git-versioner") version "1.6.7"
}

apply(plugin = "io.toolebox.git-versioner")

versioner {
  pattern {
    pattern = "$version-%h-%c-%b"
  }
}

allprojects {
  repositories {
    mavenCentral() // Lombok
    maven(url = "https://jitpack.io") // simple-yaml
    maven(url = "https://repo.papermc.io/repository/maven-public") // Velocity natives
    maven(url = "https://repo.jonesdev.xyz/releases/") // Bungee & Velocity proxy module
  }
}

subprojects {
  apply(plugin = "java")
  apply(plugin = "com.github.johnrengelman.shadow")

  dependencies {
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")

    compileOnly("io.netty:netty-all:4.1.99.Final") // netty

    implementation("net.kyori:adventure-nbt:4.14.0") // nbt
    implementation("com.j256.ormlite:ormlite-jdbc:6.1") // ORM
    implementation("xyz.jonesdev:cappuccino:0.1.6-SNAPSHOT") // expiring cache
  }
}

dependencies {
  sequenceOf("api", "bukkit", "bungee", "common", "velocity").forEach {
    // We want the jar to actually contain our modules
    implementation(project(":$it"))
  }
}

tasks {
  jar {
    manifest {
      // Set the implementation version, so we can create exact version
      // information in-game and make it accessible to the user.
      attributes["Implementation-Version"] = version
      attributes["Implementation-Vendor"] = "Jones Development, Sonar Contributors"
    }
  }

  shadowJar {
    // bStats has to be relocated to the Sonar package otherwise it throws an exception.
    relocate("org.bstats", "xyz.jonesdev.sonar.bstats")
    // Set the file name of the shadowed jar
    archiveFileName.set("${rootProject.name}.jar")
  }

  compileJava {
    options.encoding = "UTF-8"
  }

  // This is a small wrapper tasks to simplify the building process
  register("build-sonar") {
    dependsOn(clean, shadowJar)
  }
}
