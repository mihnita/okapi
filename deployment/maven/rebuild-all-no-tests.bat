cd ../../
call mvn clean
if ERRORLEVEL 1 goto end

call mvn install -DskipITs -DskipTests
if ERRORLEVEL 1 goto end

cd deployment/maven
call ant
if ERRORLEVEL 1 goto end

:end
pause
