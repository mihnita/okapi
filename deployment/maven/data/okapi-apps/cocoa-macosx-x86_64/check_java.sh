#!/bin/bash

JAVA_HOME="$(/usr/libexec/java_home -v 1.8+)"

if [ $? -ne 0 ]; then
    # `java_home` can't find JREs, so direct users to download JDK.
    osascript <<EOF
display alert "Java 1.8 or later is required to run this program." message "Click \"More Info…\" to visit the Java download website." buttons {"More Info…", "OK"} as critical
if the button returned of the result is "More Info…" then
	open location "http://www.oracle.com/technetwork/java/javase/downloads/index.html"
end if
EOF
    exit 1
fi

echo "$JAVA_HOME/bin/java"
