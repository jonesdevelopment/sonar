import net.kyori.indra.git.IndraGitExtension

plugins {
  java
  alias(libs.plugins.shadow)
  alias(libs.plugins.indra.git) apply true
}

allprojects {
  repositories {
    mavenCentral()
    maven(url = "https://repo.jonesdev.xyz/releases/") // Bungee & Velocity proxy module
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/") // libby
  }

  apply(plugin = "java")
  apply(plugin = "io.github.goooler.shadow")

  dependencies {
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)

    testCompileOnly(rootProject.libs.lombok)
    testAnnotationProcessor(rootProject.libs.lombok)

    compileOnly(rootProject.libs.adventure.minimessage)
    compileOnly(rootProject.libs.adventure.serializer)
    compileOnly(rootProject.libs.ormlite)
    compileOnly(rootProject.libs.caffeine)
    compileOnly(rootProject.libs.netty)
    compileOnly(rootProject.libs.libby.core)
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
      relocate("org.h2", "xyz.jonesdev.sonar.libs.h2")

      // Exclude unnecessary metadata information
      exclude("META-INF/*/**")

      // Minimize shadowed jar file
      minimize {
        exclude(project(":api"))
      }
    }

    compileJava {
      options.encoding = "UTF-8"
    }

    jar {
      manifest {
        val indra = rootProject.extensions.getByType(IndraGitExtension::class.java)
        val gitBranch = indra.branchName() ?: "unknown"
        val gitCommit = indra.commit()?.name?.substring(0, 8) ?: "unknown"

        // Set the implementation version, so we can create exact version
        // information in-game and make it accessible to the user.
        attributes["Implementation-Title"] = rootProject.name
        attributes["Implementation-Version"] = rootProject.version
        attributes["Implementation-Vendor"] = "Jones Development, Sonar Contributors"
        // Include the Git branch and Git commit SHA
        attributes["Git-Branch"] = gitBranch
        attributes["Git-Commit"] = gitCommit
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
