# Backend Setup and Running Guide

## Step 1: Navigate to Backend Directory
```
cd "d:/project 2/Uber Dynamic traffic and route optimization/backend"
```

## Step 2: Compile the Java Server
```
javac SimpleTrafficServer.java
```

## Step 3: Run the Backend Server
```
java SimpleTrafficServer
```

## Alternative: Use the Batch Script
```
cd "d:/project 2/Uber Dynamic traffic and route optimization"
.\run-simple-backend.bat
```

## What You Should See:
- Server starting message
- Available at: http://localhost:8080
- Ready to handle API requests

## Test the Backend:
Open browser and go to: http://localhost:8080/api/v1/routes/health

## Available Endpoints:
- GET /api/v1/routes/health - Health check
- POST /api/v1/routes/calculate - Calculate route
- POST /api/v1/routes/alternatives - Alternative routes
- GET /api/v1/traffic/realtime - Real-time traffic
- GET /api/v1/analytics - Analytics data

## Troubleshooting:
1. Make sure Java is installed (Java 17+)
2. Check if port 8080 is available
3. Look for any compilation errors
4. Server should show "Starting Uber Traffic Optimization Server"
