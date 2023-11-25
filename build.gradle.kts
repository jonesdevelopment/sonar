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
    maven(url = "https://repo.jonesdev.xyz/releases/") // Bungee & Velocity proxy module
  }

  apply(plugin = "java")
  apply(plugin = "com.github.johnrengelman.shadow")

  dependencies {
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")

    val adventureVersion = "4.14.0"

    // adventure
    implementation("net.kyori:adventure-text-minimessage:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-gson:$adventureVersion")
    // adventure nbt
    implementation("net.kyori:adventure-nbt:$adventureVersion")

    implementation("com.j256.ormlite:ormlite-jdbc:6.1") // ORM
    implementation("xyz.jonesdev:cappuccino:0.1.6-SNAPSHOT") // expiring cache

    compileOnly("io.netty:netty-all:4.1.101.Final") // netty
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
    archiveFileName.set("Sonar.jar")

    // bStats has to be relocated to the Sonar package otherwise it throws an exception
    // https://github.com/Bastian/bstats-metrics/blob/master/base/src/main/java/org/bstats/MetricsBase.java#L251
    relocate("org.bstats", "xyz.jonesdev.sonar.libs.bstats")

    // https://github.com/jonesdevelopment/sonar/issues/46
    // Relocate some packages, so we don't run into issues where we accidentally use Velocity's classes
    relocate("org.simpleyaml", "xyz.jonesdev.sonar.libs.yaml")
    relocate("com.google.gson", "xyz.jonesdev.sonar.libs.gson")
    relocate("com.j256.ormlite", "xyz.jonesdev.sonar.libs.ormlite")
    relocate("xyz.jonesdev.cappuccino", "xyz.jonesdev.sonar.libs.cappuccino")
    relocate("net.kyori.examination", "xyz.jonesdev.sonar.libs.examination")
    // We have to be careful here, so we don't accidentally break adventure
    relocate("net.kyori.adventure", "xyz.jonesdev.sonar.libs.adventure") {
      exclude("net.kyori.adventure.text.**")
      exclude("net.kyori.adventure.audience.**")
    }

    // Exclude unnecessary metadata information
    exclude("META-INF/versions/**")
  }

  compileJava {
    options.encoding = "UTF-8"
  }

  // This is a small wrapper tasks to simplify the building process
  register("build-sonar") {
    dependsOn(clean, shadowJar)
  }
}
