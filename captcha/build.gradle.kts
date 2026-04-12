plugins {
  java
  alias(libs.plugins.shadow)
  alias(libs.plugins.spotless)
  application
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(rootProject.libs.lombok)
  annotationProcessor(rootProject.libs.lombok)

  testCompileOnly(rootProject.libs.lombok)
  testAnnotationProcessor(rootProject.libs.lombok)

  implementation(rootProject.libs.imagefilters)
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
    archiveFileName.set("${rootProject.name}.jar")
    isPreserveFileTimestamps = false
    exclude("META-INF/*/**")
  }

  compileJava {
    options.encoding = "UTF-8"
  }

  jar {
    manifest {
      attributes["Main-Class"] = "com.captcha.CaptchaGeneratorUI"
      attributes["Implementation-Title"] = rootProject.name
      attributes["Implementation-Version"] = rootProject.version
    }
  }

  java.sourceCompatibility = JavaVersion.VERSION_11
  java.targetCompatibility = JavaVersion.VERSION_11
}

application {
  mainClass.set("com.captcha.CaptchaGeneratorUI")
}
