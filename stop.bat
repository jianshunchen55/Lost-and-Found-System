@echo off
title LostAndFound - Stop

echo ========================================
echo Lost and Found System - Stop
echo ========================================
echo.

:: Stop Frontend (port 8081)
echo Stopping Frontend (port 8081)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8081" ^| findstr "LISTENING"') do (
    echo Found PID: %%a
    taskkill /pid %%a /f > nul 2>&1
    echo [OK] Frontend stopped
)

:: Stop Backend (port 8080)
echo Stopping Backend (port 8080)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    echo Found PID: %%a
    taskkill /pid %%a /f > nul 2>&1
    echo [OK] Backend stopped
)

:: Try to kill by window titles
taskkill /fi "WINDOWTITLE eq LostAndFound-Frontend*" /f > nul 2>&1
taskkill /fi "WINDOWTITLE eq LostAndFound-Backend*" /f > nul 2>&1

echo.
echo ========================================
echo All services stopped!
echo ========================================
echo.
echo If any processes are still running,
echo please end them manually in Task Manager.
echo.
pause
