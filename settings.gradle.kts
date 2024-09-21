rootProject.name = "Sonar"

sequenceOf(
  "api",
  "captcha",
  "common",
  "bukkit",
  "bungeecord",
  "velocity"
).forEach {
  include(":$it")
}
