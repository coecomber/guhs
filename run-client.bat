@echo off
rem Double-click to start a standalone dev Minecraft with the Guhs mod (test worlds are in run\saves).
rem Guhs 1.10.10 Guhwinter (branch 1109-int): open the world "Guhwinter test" (copy of the server world, plaza set up).
rem Cheat sheet (NL): guhs_winter\SPIEKBRIEF.md. Jump through the phases with /guhs winter datum 2026-12-06 12:00 (or: echt).
rem Integration notes: guhs_winter\INTEGRATIE.md. Do not run gradle in this folder from an agent (purple-black textures).
cd /d "%~dp0"
set "JAVA_HOME=%APPDATA%\PrismLauncher\java\java-runtime-delta"
call "%~dp0gradlew.bat" runClient
if errorlevel 1 pause
