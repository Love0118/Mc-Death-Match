@echo off
setlocal
set "POWERSHELL=powershell.exe"
where pwsh.exe >nul 2>nul && set "POWERSHELL=pwsh.exe"

%POWERSHELL% -NoProfile -ExecutionPolicy Bypass -File "%~dp0project\Build-Server.ps1"
set "BUILD_EXIT=%ERRORLEVEL%"
if not "%BUILD_EXIT%"=="0" pause
exit /b %BUILD_EXIT%
