@echo off
rem DEV ONLY: automatic in-game screenshot round (script: tools\autocheck\autocheck.txt).
rem Creates a fresh world 'guhs_autocheck', takes screenshots and quits by itself.
rem Result: run\screenshots\autocheck\*.png and run\screenshots\autocheck\report.txt
cd /d "%~dp0"
set "JAVA_HOME=%APPDATA%\PrismLauncher\java\java-runtime-delta"
call "%~dp0gradlew.bat" --no-daemon runAutocheckClient
if errorlevel 1 pause
