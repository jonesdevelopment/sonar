dependencies {
  compileOnly(project(":api"))
  compileOnly(rootProject.libs.imagefilters)

  testImplementation(project(":api"))
  testImplementation(rootProject.libs.imagefilters)
}
