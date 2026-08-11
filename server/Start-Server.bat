@echo off
setlocal
cd /d "%~dp0"

set "MIN_MEMORY=8G"
set "MAX_MEMORY=8G"
if not defined SERVER_PORT set "SERVER_PORT=25565"
set "PAPER_JAR="

for /f "delims=" %%F in ('dir /b /a-d /o-d "paper-1.21.8-*.jar" 2^>nul') do if not defined PAPER_JAR set "PAPER_JAR=%%F"
if not defined PAPER_JAR (
    echo [ERROR] paper-1.21.8-*.jar was not found in %CD%
    pause
    exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java was not found in PATH. Install Java 21 or newer.
    pause
    exit /b 1
)

> "%CD%\eula.txt" echo eula=true
echo Starting %PAPER_JAR% on port %SERVER_PORT% with %MIN_MEMORY%-%MAX_MEMORY% memory...
java -Xms%MIN_MEMORY% -Xmx%MAX_MEMORY% -jar "%PAPER_JAR%" --nogui --port %SERVER_PORT%

set "SERVER_EXIT=%ERRORLEVEL%"
echo Server stopped with exit code %SERVER_EXIT%.
if not defined NO_PAUSE pause
exit /b %SERVER_EXIT%
