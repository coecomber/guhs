@echo off
rem Double-click to start a standalone dev Minecraft with the Guhs mod (test worlds are in run\saves).
rem Guhs 1.10.0 Het Vadsrijk, round Y (Vadsrijk <-> survival: FTB chapter, vitrine, Garderobe, separation; all zones open, see SPIEKBRIEF ch. X2 for the release view; FTB Essentials included like the server): make a NEW world (Creative, cheats on) and type /guhs vadsrijk.
rem See SPIEKBRIEF.md (long cheat sheet, NL), TESTEN_RONDE_Y.md, RELEASE_CHECKLIST.md and EINDRAPPORT_RONDE_Y.md.
rem (In this worktree run\saves starts empty: make a new world.)
cd /d "%~dp0"
set "JAVA_HOME=%APPDATA%\PrismLauncher\java\java-runtime-delta"
call "%~dp0gradlew.bat" runClient
if errorlevel 1 pause
