rootProject.name = "Sonar"

sequenceOf("api", "bukkit", "bungee", "common", "velocity").forEach {
  val path = "sonar-$it"
  val project = ":$it"

  include(project)

  project(project).projectDir = file(path)
}
