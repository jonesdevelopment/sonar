import net.kyori.indra.git.IndraGitExtension

plugins {
  java
  alias(libs.plugins.shadow)
  alias(libs.plugins.indra.git)
  alias(libs.plugins.spotless)
}

allprojects {
  repositories {
    mavenCentral()
  }

  apply(plugin = "java")
  apply(plugin = "com.gradleup.shadow")
  apply(plugin = "com.diffplug.spotless")

  dependencies {
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)

    testCompileOnly(rootProject.libs.lombok)
    testAnnotationProcessor(rootProject.libs.lombok)
  }

  spotless {
    java {
      endWithNewline()
      formatAnnotations()
      removeUnusedImports()
      trimTrailingWhitespace()
      indentWithSpaces(2)
    }
  }

  tasks {
    shadowJar {
      // Set the file name of the shadowed jar
      archiveFileName.set("${rootProject.name}-${project.name.replaceFirstChar { it.uppercase() }}.jar")

      // Remove file timestamps
      isPreserveFileTimestamps = false

      // Exclude unnecessary metadata information
      exclude("META-INF/*/**")
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
        attributes["Implementation-Vendor"] = "CaptchaGenerator Contributors"
        // Include the Git branch and Git commit SHA
        attributes["Git-Branch"] = gitBranch
        attributes["Git-Commit"] = gitCommit
      }
    }

    java.sourceCompatibility = JavaVersion.VERSION_11
    java.targetCompatibility = JavaVersion.VERSION_11
  }
}
