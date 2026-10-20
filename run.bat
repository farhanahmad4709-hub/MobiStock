@echo off
setlocal enabledelayedexpansion
title MobiStock - Starting System...

cd /d "%~dp0"

echo =====================================================================
echo                     MobiStock - Auto Launcher
echo =====================================================================
echo.

REM -----------------------------------------------------------------------
REM 1. Detect Java Environment
REM -----------------------------------------------------------------------
echo [1/4] Checking Java Runtime Environment...
set "JAVA_EXE="

where java >nul 2>&1
if %errorlevel% equ 0 (
    set "JAVA_EXE=java"
    goto :java_found
)

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
        goto :java_found
    )
)

for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-17*" "C:\Program Files\Java\jdk-17*" "C:\Program Files\Microsoft\jdk-17*") do (
    if exist "%%D\bin\java.exe" (
        set "JAVA_HOME=%%D"
        set "PATH=%%D\bin;!PATH!"
        set "JAVA_EXE=%%D\bin\java.exe"
        goto :java_found
    )
)

echo [ERROR] Java JDK 17 could not be found automatically.
echo Please install JDK 17 or configure JAVA_HOME.
echo.
pause
exit /b 1

:java_found
echo       Found Java: !JAVA_EXE!
echo.

REM -----------------------------------------------------------------------
REM 2. Check and Start MySQL Service / Process
REM -----------------------------------------------------------------------
echo [2/4] Checking MySQL Database...
powershell -NoProfile -Command "if (Get-Process mysqld -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }"
if %errorlevel% equ 0 goto :mysql_running

echo       MySQL is not running. Starting MySQL...

REM Try starting as Windows service first
sc query mysql >nul 2>&1
if %errorlevel% equ 0 (
    net start mysql >nul 2>&1
)

REM Check if started via service
powershell -NoProfile -Command "if (Get-Process mysqld -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }"
if %errorlevel% equ 0 goto :mysql_wait

REM Try starting via standard XAMPP paths
set "XAMPP_DIR="
if exist "C:\xampp\mysql\bin\mysqld.exe" set "XAMPP_DIR=C:\xampp"
if not defined XAMPP_DIR if exist "D:\xampp\mysql\bin\mysqld.exe" set "XAMPP_DIR=D:\xampp"
if not defined XAMPP_DIR if exist "E:\xampp\mysql\bin\mysqld.exe" set "XAMPP_DIR=E:\xampp"

if defined XAMPP_DIR (
    echo       Starting MySQL via XAMPP from !XAMPP_DIR!...
    start "" /B "!XAMPP_DIR!\mysql\bin\mysqld.exe" --defaults-file="!XAMPP_DIR!\mysql\bin\my.ini" --standalone
) else (
    echo [WARNING] Could not locate XAMPP mysql directory.
    echo Please ensure XAMPP MySQL is started manually.
)

:mysql_wait
echo       Waiting for MySQL port 3306...
powershell -NoProfile -Command "$i=0; while ($i -lt 15) { if (Test-NetConnection -ComputerName 127.0.0.1 -Port 3306 -InformationLevel Quiet) { exit 0 }; Start-Sleep -Seconds 1; $i++ }; exit 1"
if %errorlevel% equ 0 (
    echo       MySQL is ready on port 3306!
) else (
    echo [WARNING] MySQL took longer than expected to respond. Continuing...
)
goto :mysql_done

:mysql_running
echo       MySQL is already running.

:mysql_done
echo.

REM -----------------------------------------------------------------------
REM 3. Check for Packaged Application JAR
REM -----------------------------------------------------------------------
echo [3/4] Checking MobiStock application package...
if exist "target\mobistock-1.0.0.jar" goto :jar_ready

echo       Application package target\mobistock-1.0.0.jar not found.
echo       Building package with Maven, please wait...
call mvnw.cmd package -DskipTests
if %errorlevel% neq 0 (
    echo [ERROR] Build failed. Please inspect errors above.
    pause
    exit /b 1
)

:jar_ready
echo       MobiStock package is ready.
echo.

REM -----------------------------------------------------------------------
REM 4. Start Browser Watcher and Run Application
REM -----------------------------------------------------------------------
echo [4/4] Starting MobiStock server...
echo       Your browser will automatically open to http://localhost:8080
echo.
echo =====================================================================
echo   MobiStock is now active! Keep this window open while using the app.
echo   Press CTRL+C to stop the server when done.
echo =====================================================================
echo.

REM Launch background probe to open browser when app is ready
start "" /B powershell -NoProfile -WindowStyle Hidden -Command "$i=0; while ($i -lt 45) { try { $r = Invoke-WebRequest -Uri 'http://localhost:8080/login' -UseBasicParsing -TimeoutSec 1; if ($r.StatusCode -eq 200) { Start-Process 'http://localhost:8080'; exit 0 } } catch {}; Start-Sleep -Seconds 1; $i++ }"

REM Run Spring Boot JAR
"!JAVA_EXE!" -jar "target\mobistock-1.0.0.jar"

pause
