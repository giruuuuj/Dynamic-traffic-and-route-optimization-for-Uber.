@echo off
echo ========================================
echo Starting Uber Traffic Optimization System
echo ========================================

echo.
echo Step 1: Starting Backend Server...
cd backend
echo Starting Java backend server on port 8080...
start "Backend Server" cmd /k "cd /d %cd% && java SimpleTrafficServer"

echo.
echo Step 2: Starting Frontend...
cd ../frontend
echo Starting React frontend on port 3000...
start "Frontend" cmd /k "cd /d %cd% && npm start"

echo.
echo ========================================
echo Services Starting...
echo ========================================
echo Backend: http://localhost:8080
echo Frontend: http://localhost:3000
echo.
echo Both services should start in separate windows.
echo Press any key to exit this launcher...
pause
