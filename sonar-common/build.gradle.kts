dependencies {
  compileOnly(project(":api"))

  // adventure nbt for packets
  compileOnly("net.kyori:adventure-nbt:4.17.0")

  // CAPTCHA image generation
  implementation("xyz.jonesdev:capja:1.0.3")
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11
