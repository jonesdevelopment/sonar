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
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")

    // adventure
    implementation("net.kyori:adventure-text-minimessage:4.16.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.16.0")
    implementation("net.kyori:adventure-nbt:4.16.0")

    compileOnly("com.j256.ormlite:ormlite-jdbc:6.1") // ORMLite
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.1.8") // caching
    compileOnly("io.netty:netty-all:4.1.108.Final") // netty

    // Library/dependency loading
    compileOnly("com.alessiodp.libby:libby-core:2.0.0-SNAPSHOT")
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
    // Set the file name of the shadowed jar
    archiveFileName.set("${rootProject.name}.jar")

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

    // TODO: actually fix this :/
    // We have to be careful here, so we don't accidentally break adventure
    relocate("net.kyori", "xyz.jonesdev.sonar.libs.kyori") {
      exclude("net.kyori.adventure.text.**")
      exclude("net.kyori.adventure.audience.Audience")
      exclude("net.kyori.adventure.title.*")
    }

    // Exclude unnecessary metadata information
    exclude("META-INF/*/**")
    // We want to load our own Gson dynamically
    exclude("com/google/gson/**")
  }

  compileJava {
    options.encoding = "UTF-8"
  }

  // This is a small wrapper tasks to simplify the building process
  register("build-sonar") {
    dependsOn(clean, shadowJar)
  }
}
