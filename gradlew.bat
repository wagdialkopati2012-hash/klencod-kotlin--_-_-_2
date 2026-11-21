@echo off
set APP_HOME=%~dp0
set JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
if exist "%JAR%" (
  java -Dorg.gradle.appname=gradlew -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
  exit /b %ERRORLEVEL%
)
where gradle >nul 2>nul
if %ERRORLEVEL%==0 (
  echo gradle-wrapper.jar not found; using system gradle. Run: gradle wrapper 1>&2
  gradle %*
  exit /b %ERRORLEVEL%
)
echo ERROR: gradle-wrapper.jar is missing and no system gradle was found. 1>&2
echo Open the project in Android Studio or run: gradle wrapper 1>&2
exit /b 1
