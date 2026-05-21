#!/bin/sh
APP_NAME="Gradle"
APP_BASE_NAME=
CLASSPATH=/gradle/wrapper/gradle-wrapper.jar
exec java   -classpath "" org.gradle.wrapper.GradleWrapperMain "$@"