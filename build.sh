#!/usr/bin/env bash
# Fix permissions so we are actually able to run our gradle script
chmod +x gradlew
# You can also use clean shadowJar if something goes wrong here
# e.g. "./gradlew clean shadowJar --stacktrace"
./gradlew build-sonar --stacktrace
