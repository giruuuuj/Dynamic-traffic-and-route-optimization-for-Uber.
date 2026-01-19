@echo off
echo ========================================
echo Uber Traffic Optimization Backend Setup
echo ========================================

REM Set Java home (try multiple common locations)
set JAVA_HOME=""
if exist "C:\Program Files\Java\jdk-25" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-25"
) else if exist "C:\Program Files\Java\jdk-21" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21"
) else if exist "C:\Program Files\Java\jdk-17" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-17"
) else (
    echo Java not found in common locations. Please ensure Java 17+ is installed.
    pause
    exit /b 1
)

echo Using Java from: %JAVA_HOME%

REM Create target directories
if not exist "backend\target\classes" mkdir "backend\target\classes"
if not exist "backend\target\lib" mkdir "backend\target\lib"

echo.
echo ========================================
echo Compiling Java Source Files
echo ========================================

REM Compile all Java files
"%JAVA_HOME%\bin\javac.exe" -d "backend\target\classes" -cp "backend\target\lib\*" ^
    "backend\src\main\java\com\uber\traffic\*.java" ^
    "backend\src\main\java\com\uber\traffic\model\*.java" ^
    "backend\src\main\java\com\uber\traffic\algorithm\*.java" ^
    "backend\src\main\java\com\uber\traffic\dto\*.java" ^
    "backend\src\main\java\com\uber\traffic\repository\*.java" ^
    "backend\src\main\java\com\uber\traffic\service\*.java" ^
    "backend\src\main\java\com\uber\traffic\controller\*.java" ^
    "backend\src\main\java\com\uber\traffic\config\*.java"

if %ERRORLEVEL% neq 0 (
    echo.
    echo ❌ Compilation failed!
    echo Please check the error messages above.
    pause
    exit /b 1
)

echo.
echo ✅ Compilation successful!

echo.
echo ========================================
echo Starting Application
echo ========================================

REM Create classpath
set CLASSPATH=backend\target\classes
for %%f in (backend\target\lib\*.jar) do (
    set "CLASSPATH=!CLASSPATH!;backend\target\lib\%%f"
)

echo Starting Uber Traffic Optimization Backend...
echo Application will be available at: http://localhost:8080
echo Press Ctrl+C to stop the server
echo.

cd backend
"%JAVA_HOME%\bin\java.exe" -cp "%CLASSPATH%" com.uber.traffic.TrafficOptimizationApplication

pause
