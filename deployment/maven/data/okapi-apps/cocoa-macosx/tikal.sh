#!/bin/bash

cmdpath="$0"
while [ -L "${cmdpath}" ]; do cmdpath=$(readlink "${cmdpath}"); done
OKAPI_HOME="${OKAPI_HOME:-$(dirname "${cmdpath}")}"

JAVA_HOME="$(/usr/libexec/java_home -v 1.8+)"

if [ $? -ne 0 ]; then
    echo "Okapi requires Java 1.8 or higher."
    exit 1
fi

JAVA="$JAVA_HOME/bin/java"

exec "$JAVA" -XstartOnFirstThread -cp "$OKAPI_HOME/lib/*" net.sf.okapi.applications.tikal.Main "$@"
