#!/bin/bash
set -e

if [[ -e /usr/libexec/java_home ]]; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
fi

[ ! -d "./superpom" ] && cd ..
[ ! -d "./superpom" ] && cd ..

mvn clean
mvn install -P with_sources,with_javadoc

mvn dependency:resolve -f okapi-ui/swt/core-ui/pom.xml -PWIN_SWT -PWIN_64_SWT -PCOCOA_64_SWT -PLinux_x86_swt -PLinux_x86_64_swt

ant clean -f deployment/maven
ant -f deployment/maven

mvn clean -f applications/integration-tests
mvn integration-test -f applications/integration-tests

echo
echo "Paused. Press Enter to continue."
read
