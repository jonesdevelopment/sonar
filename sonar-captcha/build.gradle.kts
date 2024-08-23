dependencies {
  compileOnly(project(":api"))
  compileOnly(rootProject.libs.imagefilters)

  testCompileOnly(project(":api"))
  testCompileOnly(rootProject.libs.imagefilters)
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11
