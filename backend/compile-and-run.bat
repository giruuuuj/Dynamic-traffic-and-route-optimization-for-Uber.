@echo off
echo Compiling Uber Traffic Optimization Backend...

REM Set Java home
set JAVA_HOME=C:\Program Files\Java\jdk-25

REM Create target directories
if not exist target\classes mkdir target\classes
if not exist target\lib mkdir target\lib

REM Download required dependencies (simplified version)
echo Downloading Spring Boot dependencies...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-webflux/3.2.0/spring-boot-starter-webflux-3.2.0.jar' -OutFile 'target\lib\spring-boot-starter-webflux-3.2.0.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-data-neo4j/3.2.0/spring-boot-starter-data-neo4j-3.2.0.jar' -OutFile 'target\lib\spring-boot-starter-data-neo4j-3.2.0.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-data-redis-reactive/3.2.0/spring-boot-starter-data-redis-reactive-3.2.0.jar' -OutFile 'target\lib\spring-boot-starter-data-redis-reactive-3.2.0.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/projectlombok/lombok/1.18.30/lombok-1.18.30.jar' -OutFile 'target\lib\lombok-1.18.30.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/springframework/spring-webflux/6.1.5/spring-webflux-6.1.5.jar' -OutFile 'target\lib\spring-webflux-6.1.5.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/springframework/spring-context/6.1.5/spring-context-6.1.5.jar' -OutFile 'target\lib\spring-context-6.1.5.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/springframework/spring-core/6.1.5/spring-core-6.1.5.jar' -OutFile 'target\lib\spring-core-6.1.5.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/springframework/spring-beans/6.1.5/spring-beans-6.1.5.jar' -OutFile 'target\lib\spring-beans-6.1.5.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/io/projectreactor/reactor-core/3.5.10/reactor-core-3.5.10.jar' -OutFile 'target\lib\reactor-core-3.5.10.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.3/jackson-databind-2.15.3.jar' -OutFile 'target\lib\jackson-databind-2.15.3.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar' -OutFile 'target\lib\slf4j-api-2.0.9.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/ch/qos/logback/logback-classic/1.4.14/logback-classic-1.4.14.jar' -OutFile 'target\lib\logback-classic-1.4.14.jar'"

REM Compile all Java files
echo Compiling Java source files...
"%JAVA_HOME%\bin\javac.exe" -d target\classes -cp "target\lib\*" ^
    src\main\java\com\uber\traffic\*.java ^
    src\main\java\com\uber\traffic\model\*.java ^
    src\main\java\com\uber\traffic\algorithm\*.java ^
    src\main\java\com\uber\traffic\dto\*.java ^
    src\main\java\com\uber\traffic\repository\*.java ^
    src\main\java\com\uber\traffic\service\*.java ^
    src\main\java\com\uber\traffic\controller\*.java ^
    src\main\java\com\uber\traffic\config\*.java

if %ERRORLEVEL% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Compilation successful!

REM Create classpath
set CLASSPATH=target\classes
for %%f in (target\lib\*.jar) do (
    set CLASSPATH=!CLASSPATH!;target\lib\%%f
)

REM Run the application
echo Starting application...
"%JAVA_HOME%\bin\java.exe" -cp "%CLASSPATH%" com.uber.traffic.TrafficOptimizationApplication

pause
