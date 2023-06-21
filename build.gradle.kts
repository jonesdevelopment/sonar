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
  }
}

subprojects {
  apply(plugin = "java")
  apply(plugin = "com.github.johnrengelman.shadow")

  dependencies {
    compileOnly("io.netty:netty-all:4.1.94.Final") // netty

    compileOnly("org.projectlombok:lombok:1.18.28")
    annotationProcessor("org.projectlombok:lombok:1.18.28")

    testCompileOnly("org.projectlombok:lombok:1.18.28")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.28")
  }
}

dependencies {
  sequenceOf("api", "bukkit", "bungee", "common", "velocity").forEach {
    implementation(project(":sonar-$it"))
  }
}

tasks {
  compileJava {
    options.encoding = "UTF-8"
  }
}