#!/bin/sh
# Maven Wrapper script - downloads and runs Maven
# Based on Apache Maven Wrapper

set -e

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd "$(dirname "$0")" && pwd)}"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    distributionUrl=$(grep 'distributionUrl' "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2- | tr -d '\r\n')
fi

MAVEN_HOME="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven"
MAVEN_CMD="$MAVEN_HOME/bin/mvn"

if [ ! -f "$MAVEN_CMD" ]; then
    echo "Downloading Maven from $distributionUrl"
    mkdir -p "$MAVEN_PROJECTBASEDIR/.mvn/wrapper"

    if command -v curl > /dev/null 2>&1; then
        curl -fsSL -o "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven.zip" "$distributionUrl"
    elif command -v wget > /dev/null 2>&1; then
        wget -q -O "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven.zip" "$distributionUrl"
    else
        echo "Error: Neither curl nor wget found. Cannot download Maven."
        exit 1
    fi

    mkdir -p "$MAVEN_HOME"
    unzip -q -o "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven.zip" -d "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/"
    mv "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/apache-maven-"*/* "$MAVEN_HOME/"
    rmdir "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/apache-maven-"*
    rm "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven.zip"
    chmod +x "$MAVEN_CMD"
fi

exec "$MAVEN_CMD" "$@"
