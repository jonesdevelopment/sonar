buildscript {
  repositories {
    maven {
      url = uri("https://plugins.gradle.org/m2/")
    }
  }

  dependencies {
    classpath("gradle.plugin.io.toolebox:gradle-git-versioner:1.6.7")
  }
}

plugins {
  id("java")
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("io.toolebox.git-versioner") version "1.6.7"

  kotlin("jvm") version "1.8.22"
}

apply(plugin = "io.toolebox.git-versioner")

val semanticVersion = "2.0.0"

versioner {
  pattern {
    pattern = "$semanticVersion-%h(-%c)"
  }
}

allprojects {
  repositories {
    mavenCentral() // Lombok
    maven(url = "https://repo.papermc.io/repository/maven-public") // Velocity natives
    maven(url = "https://jonesdev.xyz/releases/") // Bungee & Velocity proxy module
  }
}

subprojects {
  apply(plugin = "java")
  apply(plugin = "kotlin")
  apply(plugin = "com.github.johnrengelman.shadow")

  dependencies {
    compileOnly("org.projectlombok:lombok:1.18.28")
    annotationProcessor("org.projectlombok:lombok:1.18.28")

    testCompileOnly("org.projectlombok:lombok:1.18.28")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.28")

    compileOnly("io.netty:netty-all:4.1.94.Final") // netty

    implementation(kotlin("stdlib-jdk8")) // kotlin
    implementation("net.kyori:adventure-nbt:4.14.0") // nbt
  }

  kotlin {
    // We use 8 for every project except for Velocity
    jvmToolchain(8)
  }
}

dependencies {
  sequenceOf("api", "bukkit", "bungee", "common", "velocity").forEach {
    implementation(project(":sonar-$it"))
  }
}

tasks {
  jar {
    manifest {
      attributes["Implementation-Version"] = version
    }
  }

  // relocation for all libraries to avoid issues
  // with other plugins using the same libraries
  shadowJar {
    relocate("com.zaxxer.hikari", "jones.sonar.lib.hikari")
    relocate("com.mysql.cj", "jones.sonar.lib.mysql")
    relocate("com.mysql.jdbc", "jones.sonar.lib.jdbc")
    relocate("org.yaml.snakeyaml", "jones.sonar.lib.snakeyaml")
    relocate("net.kyori.adventure.nbt", "jones.sonar.lib.nbt")
  }

  compileJava {
    options.encoding = "UTF-8"
  }
}