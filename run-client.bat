@echo off
rem Double-click to start a standalone dev Minecraft with the Guhs mod (test worlds are in run\saves).
cd /d "%~dp0"
set "JAVA_HOME=%APPDATA%\PrismLauncher\java\java-runtime-delta"
call "%~dp0gradlew.bat" runClient
if errorlevel 1 pause
