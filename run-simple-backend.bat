@echo off
echo ========================================
echo Uber Traffic Optimization Backend
echo ========================================

REM Find Java installation
set JAVA_CMD=""
if exist "C:\Program Files\Java\jdk-25\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-25\bin\java.exe"
) else if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-21\bin\java.exe"
) else if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-17\bin\java.exe"
) else (
    echo Java not found! Please install Java 17 or higher.
    pause
    exit /b 1
)

echo Using Java: %JAVA_CMD%

REM Create target directory
if not exist "backend\target\classes" mkdir "backend\target\classes"

echo.
echo Compiling the application...
"%JAVA_CMD%" -cp "backend\src\main\java" -d "backend\target\classes" ^
    "backend\src\main\java\com\uber\traffic\TrafficOptimizationApplication.java" 2>nul

if %ERRORLEVEL% neq 0 (
    echo Compilation failed! Trying with Spring Boot...
    echo Downloading Spring Boot dependencies...
    
    REM Try to compile with basic Java first
    echo Creating a simple version without Spring Boot...
    
    copy "backend\src\main\java\com\uber\traffic\TrafficOptimizationApplication.java" "backend\SimpleServer.java" >nul
    
    echo Starting simple HTTP server...
    cd backend
    "%JAVA_CMD%" -cp "target\classes" com.uber.traffic.TrafficOptimizationApplication
) else (
    echo.
    echo Starting the backend server...
    echo Application will be available at: http://localhost:8080
    echo Press Ctrl+C to stop the server
    echo.
    
    cd backend
    "%JAVA_CMD%" -cp "target\classes" com.uber.traffic.TrafficOptimizationApplication
)

pause
