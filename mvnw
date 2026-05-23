#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.3.2

set -e

# OS specific support.
cygwin=false
darwin=false
mingw=false
msys=false
case "$(uname)" in
  CYGWIN*) cygwin=true ;;
  MINGW*)  mingw=true  ;;
  Darwin*) darwin=true ;;
  MSYS*)   msys=true   ;;
esac

# Find the project base dir (directory that contains .mvn)
find_maven_basedir() {
  basedir="$(dirname "$0")"
  wdir="$basedir"
  while [ "$wdir" != / ]; do
    if [ -d "$wdir/.mvn" ]; then
      basedir="$wdir"
      break
    fi
    wdir="$(cd "$wdir/.." && pwd)"
  done
  printf '%s' "$(cd "$basedir" && pwd)"
}

BASE_DIR="$(find_maven_basedir)"
MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$BASE_DIR}"

WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"

# Read distributionUrl and wrapperUrl from properties
distributionUrl=
wrapperUrl=
if [ -r "$WRAPPER_PROPERTIES" ]; then
  while IFS='=' read -r key value; do
    case "$key" in
      distributionUrl) distributionUrl="$value" ;;
      wrapperUrl)      wrapperUrl="$value"       ;;
    esac
  done < "$WRAPPER_PROPERTIES"
fi

# Download wrapper jar if missing
if [ ! -r "$WRAPPER_JAR" ] && [ -n "$wrapperUrl" ]; then
  if command -v curl > /dev/null 2>&1; then
    curl -fsSL -o "$WRAPPER_JAR" "$wrapperUrl" || true
  elif command -v wget > /dev/null 2>&1; then
    wget -q -O "$WRAPPER_JAR" "$wrapperUrl" || true
  fi
fi

# Locate Java
if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi

if ! command -v "$JAVACMD" > /dev/null 2>&1; then
  echo "ERROR: JAVA_HOME is not set and 'java' was not found in PATH." >&2
  exit 1
fi

# Cygwin path conversion
if $cygwin; then
  [ -n "$JAVA_HOME" ]   && JAVA_HOME="$(cygpath --unix "$JAVA_HOME")"
  [ -n "$CLASSPATH" ]   && CLASSPATH="$(cygpath --path --unix "$CLASSPATH")"
  MAVEN_PROJECTBASEDIR="$(cygpath --path --unix "$MAVEN_PROJECTBASEDIR")"
  WRAPPER_JAR="$(cygpath --path --unix "$WRAPPER_JAR")"
fi

exec "$JAVACMD" \
  ${MAVEN_OPTS:+$MAVEN_OPTS} \
  -classpath "$WRAPPER_JAR" \
  "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
  org.apache.maven.wrapper.MavenWrapperMain \
  "$distributionUrl" "$@"
