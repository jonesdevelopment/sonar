rootProject.name = "Sonar"

sequenceOf("api", "captcha", "common", "bukkit", "bungee", "velocity").forEach {
  val path = "sonar-$it"
  val project = ":$it"

  include(project)

  project(project).projectDir = file(path)
}
