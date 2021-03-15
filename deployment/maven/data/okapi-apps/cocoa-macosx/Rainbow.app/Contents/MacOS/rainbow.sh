#!/bin/bash

OKAPI_HOME="${OKAPI_HOME:-$(dirname "$0")/../../../}"

JAVA="$("$OKAPI_HOME/check_java.sh")"

if [ -z "$JAVA" ]; then
    exit 1
fi

# Get rid of process serial number from GUI launch
if [[ "$*" == -psn* ]]; then
    shift
fi

exec "$JAVA" -XstartOnFirstThread -Xdock:name="Rainbow" -cp "$OKAPI_HOME/lib/*" net.sf.okapi.applications.rainbow.Main "$@"
