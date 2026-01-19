@echo off
echo Starting Uber Traffic Optimization Backend...

REM Set Java home (adjust if needed)
set JAVA_HOME=C:\Program Files\Java\jdk-25

REM Create classpath with all dependencies
set CLASSPATH=.

REM Add all JAR files from target directory to classpath
for %%f in (target\*.jar) do (
    set CLASSPATH=!CLASSPATH!;target\%%f
)

REM Add Maven dependencies
for %%f in (target\lib\*.jar) do (
    set CLASSPATH=!CLASSPATH!;target\lib\%%f
)

REM Run the application
echo Running with classpath: %CLASSPATH%
"%JAVA_HOME%\bin\java.exe" -cp "%CLASSPATH%" com.uber.traffic.TrafficOptimizationApplication

pause
