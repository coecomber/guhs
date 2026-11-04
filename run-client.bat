@echo off
rem Double-click to start a standalone dev Minecraft with the Guhs mod (test worlds are in run\saves).
rem Guhs 1.10.0 Het Vadsrijk: make a NEW world (Creative, cheats on) and type /guhs vadsrijk. See TESTEN_VADSRIJK.md (zone 1 after the fix round + zone 2).
rem (In this worktree run\saves starts empty: make a new world.)
cd /d "%~dp0"
set "JAVA_HOME=%APPDATA%\PrismLauncher\java\java-runtime-delta"
call "%~dp0gradlew.bat" runClient
if errorlevel 1 pause
