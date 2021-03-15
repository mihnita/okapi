@echo off

if not exist ".\superpom" cd ..
if not exist ".\superpom" cd ..

call mvn clean
if ERRORLEVEL 1 goto end
call mvn install -P with_sources,with_javadoc
if ERRORLEVEL 1 goto end

call mvn dependency:resolve -f okapi-ui/swt/core-ui/pom.xml -PWIN_SWT -PWIN_64_SWT -PCOCOA_64_SWT -PLinux_x86_swt -PLinux_x86_64_swt
if ERRORLEVEL 1 goto end

call ant clean -f deployment/maven
if ERRORLEVEL 1 goto end
call ant -f deployment/maven
if ERRORLEVEL 1 goto end

call mvn clean -f applications/integration-tests
if ERRORLEVEL 1 goto end
call mvn integration-test -f applications/integration-tests
if ERRORLEVEL 1 goto end

echo .
echo "Paused. Press Enter to continue."
:end
pause
