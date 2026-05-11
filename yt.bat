@echo off
setlocal

set URL=%~1

if "%URL%"=="" (
    echo Usage: yt "https://youtube.com/watch?v=..."
    exit /b 1
)

set SCRIPT_DIR=%~dp0
setx PATH "%PATH%;%SCRIPT_DIR%" >nul 2>&1

curl -s http://localhost:8080/api/health >nul 2>&1
if errorlevel 1 (
    echo Starting Spring Boot...
    start /B "%SCRIPT_DIR%gradlew.bat" bootRun >nul 2>&1
    echo Please wait...
    timeout /t 10 /nobreak >nul
)

curl -s -X POST http://localhost:8080/api/convert ^
  -H "Content-Type: application/json" ^
  -d "{\"url\": \"%URL%\"}"

echo.