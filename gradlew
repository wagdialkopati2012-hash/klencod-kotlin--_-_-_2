#!/bin/sh
# Gradle wrapper launcher.
# Uses gradle/wrapper/gradle-wrapper.jar when present; otherwise falls back to a
# locally installed `gradle` (run `gradle wrapper` once to regenerate the jar).
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then JAVACMD="$JAVA_HOME/bin/java"; else JAVACMD=java; fi
if [ -f "$JAR" ]; then
  exec "$JAVACMD" -Dorg.gradle.appname=gradlew -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
elif command -v gradle >/dev/null 2>&1; then
  echo "gradle-wrapper.jar not found; using system gradle. Run: gradle wrapper" >&2
  exec gradle "$@"
else
  echo "ERROR: gradle-wrapper.jar is missing and no system 'gradle' was found." >&2
  echo "Open the project in Android Studio (it regenerates the wrapper) or install Gradle and run: gradle wrapper" >&2
  exit 1
fi
