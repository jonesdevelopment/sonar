dependencies {
  compileOnly(project(":api"))
  implementation(project(":captcha"))
  compileOnly(rootProject.libs.adventure.nbt)
}
