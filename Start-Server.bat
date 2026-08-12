@echo off
setlocal
cd /d "%~dp0"

if not exist "Start-Server.ps1" (
    echo [ERROR] Start-Server.ps1 was not found in %CD%
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%CD%\Start-Server.ps1" -MinMemoryGB 8 -MaxMemoryGB 8
set "SERVER_EXIT=%ERRORLEVEL%"
echo Server stopped with exit code %SERVER_EXIT%.
if not defined NO_PAUSE pause
exit /b %SERVER_EXIT%
