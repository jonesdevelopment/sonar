rootProject.name = "Sonar"

sequenceOf(
  "api",
  "captcha",
  "common",
  "bukkit",
  "bungeecord",
  "paper",
  "velocity"
).forEach {
  include(":$it")
}
