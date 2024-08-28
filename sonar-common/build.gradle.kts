dependencies {
  compileOnly(project(":api"))
  implementation(project(":captcha"))
  compileOnly(rootProject.libs.adventure.nbt)
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11
