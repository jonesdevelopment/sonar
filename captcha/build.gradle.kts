dependencies {
  compileOnly(project(":api"))
  compileOnly(rootProject.libs.imagefilters)

  testCompileOnly(project(":api"))
  testCompileOnly(rootProject.libs.imagefilters)
}
