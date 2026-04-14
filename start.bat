@echo off
title LostAndFound - Start

echo ========================================
echo Lost and Found System - Start
echo ========================================
echo.

:: Get script directory
set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%LostAndFound"
set "CLIENT_DIR=%PROJECT_DIR%\client"
set "SERVER_DIR=%PROJECT_DIR%\server"

echo Project directory: %PROJECT_DIR%
echo.

:: Check Node.js
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Node.js not found! Please install Node.js first.
    pause
    exit /b 1
)
echo [OK] Node.js found
node --version

:: Check Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Java not found! Please install JDK 11+.
    pause
    exit /b 1
)
echo [OK] Java found
java -version 2>&1 | findstr "version"

:: Check MySQL
echo.
echo Checking MySQL...
sc query MySQL80 >nul 2>nul
if %errorlevel% equ 0 (
    sc query MySQL80 | find "RUNNING" >nul
    if %errorlevel% equ 0 (
        echo [OK] MySQL80 is running
    ) else (
        echo [WARN] MySQL80 service exists but is NOT running!
        echo.
        echo Please start MySQL first by ONE of these methods:
        echo   1. Press Win+R, type "services.msc", find MySQL80 and start it
        echo   2. Run "net start MySQL80" as Administrator
        echo.
        pause
    )
) else (
    sc query MySQL >nul 2>nul
    if %errorlevel% equ 0 (
        sc query MySQL | find "RUNNING" >nul
        if %errorlevel% equ 0 (
            echo [OK] MySQL is running
        ) else (
            echo [WARN] MySQL service exists but is NOT running!
            echo.
            echo Please start MySQL first by ONE of these methods:
            echo   1. Press Win+R, type "services.msc", find MySQL and start it
            echo   2. Run "net start MySQL" as Administrator
            echo.
            pause
        )
    ) else (
        echo [WARN] Cannot find MySQL service (MySQL80 or MySQL)
        echo Please make sure MySQL is installed and running.
        echo.
        pause
    )
)

echo.
echo ========================================
echo Starting services...
echo ========================================
echo.

:: Start Backend
echo Starting Backend on port 8080...
if exist "%SERVER_DIR%\pom.xml" (
    echo Using Maven to start...
    start /d "%SERVER_DIR%" "LostAndFound-Backend" cmd /k "mvn spring-boot:run"
) else if exist "%SERVER_DIR%\target\*.jar" (
    echo Using JAR to start...
    start /d "%SERVER_DIR%" "LostAndFound-Backend" cmd /k "java -jar target\*.jar"
) else (
    echo ERROR: Cannot find pom.xml or JAR in %SERVER_DIR%
    pause
    exit /b 1
)

:: Wait for backend
echo.
echo Waiting for backend to start (15 seconds)...
timeout /t 15 /nobreak > nul

:: Start Frontend
echo Starting Frontend on port 8081...
if not exist "%CLIENT_DIR%\node_modules" (
    echo First run, installing dependencies...
    cd /d "%CLIENT_DIR%"
    call npm install
    if %errorlevel% neq 0 (
        echo ERROR: npm install failed
        pause
        exit /b 1
    )
)

start /d "%CLIENT_DIR%" "LostAndFound-Frontend" cmd /k "npm run serve"

echo.
echo ========================================
echo All services started!
echo ========================================
echo.
echo Frontend: http://localhost:8081
echo Backend: http://localhost:8080
echo.
echo Database: MySQL localhost:3306/lostfound
echo Username: root  Password: 123456
echo.
echo Press any key to open browser...
pause > nul

start http://localhost:8081

echo.
echo Tip: Close this window will NOT stop services.
echo      Run stop.bat to stop all services.
echo.
pause
