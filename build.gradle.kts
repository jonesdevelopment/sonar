buildscript {
  dependencies {
    classpath("gradle.plugin.io.toolebox:gradle-git-versioner:1.6.7")
  }
}

plugins {
  id("java")
  id("io.github.goooler.shadow") version "8.1.7"
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
    maven(url = "https://repo.jonesdev.xyz/releases/") // Bungee & Velocity proxy module
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/") // libby
  }

  apply(plugin = "java")
  apply(plugin = "io.github.goooler.shadow")

  dependencies {
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    testCompileOnly("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")

    // adventure
    compileOnly("net.kyori:adventure-text-minimessage:4.16.0")
    compileOnly("net.kyori:adventure-text-serializer-gson:4.16.0")

    compileOnly("com.j256.ormlite:ormlite-jdbc:6.1") // ORMLite
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.1.8") // caching
    compileOnly("io.netty:netty-all:4.1.108.Final") // netty

    // Library/dependency loading
    compileOnly("com.alessiodp.libby:libby-core:2.0.0-SNAPSHOT")
  }

  tasks {
    shadowJar {
      // Set the file name of the shadowed jar
      archiveFileName.set("${rootProject.name}-${project.name.replaceFirstChar { it.uppercase() }}.jar")

      // Remove file timestamps
      isPreserveFileTimestamps = false

      // Relocate libraries
      relocate("org.bstats", "xyz.jonesdev.sonar.libs.bstats")
      relocate("com.alessiodp.libby", "xyz.jonesdev.sonar.libs.libby")
      relocate("com.simpleyaml", "xyz.jonesdev.sonar.libs.yaml")
      relocate("com.google.gson", "xyz.jonesdev.sonar.libs.gson")
      relocate("com.j256.ormlite", "xyz.jonesdev.sonar.libs.ormlite")
      relocate("com.github.benmanes.caffeine", "xyz.jonesdev.sonar.libs.caffeine")
      relocate("com.mysql", "xyz.jonesdev.sonar.libs.mysql")
      relocate("org.mariadb", "xyz.jonesdev.sonar.libs.mariadb")
      relocate("xyz.jonesdev.capja", "xyz.jonesdev.sonar.libs.capja")

      // Exclude unnecessary metadata information
      exclude("META-INF/*/**")
    }

    compileJava {
      options.encoding = "UTF-8"
    }

    jar {
      manifest {
        // Set the implementation version, so we can create exact version
        // information in-game and make it accessible to the user.
        attributes["Implementation-Version"] = rootProject.version
        attributes["Implementation-Vendor"] = "Jones Development, Sonar Contributors"
      }
    }
  }
}

tasks {
  // This is a small wrapper tasks to simplify the building process
  register("build-sonar") {
    val subprojects = listOf("api", "common", "bukkit", "bungee", "velocity")
    val buildTasks = subprojects.flatMap { listOf("$it:clean", "$it:shadowJar") }
    dependsOn(buildTasks)
  }
}
