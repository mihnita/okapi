#!/bin/bash

OKAPI_HOME="${OKAPI_HOME:-$(dirname "$0")/../../../}"

JAVA="$("$OKAPI_HOME/check_java.sh")"

if [ -z "$JAVA" ]; then
    exit 1
fi

exec "$JAVA" -XstartOnFirstThread -Xdock:name="Ratel" -cp "$OKAPI_HOME/lib/*" net.sf.okapi.applications.ratel.Main "$@"
