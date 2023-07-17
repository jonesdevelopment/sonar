rootProject.name = "Sonar"

sequenceOf("api", "bukkit", "bungee", "common", "velocity").forEach {
  val project = ":sonar-$it"

  include(project)

  project(project).projectDir = file(it)
}
