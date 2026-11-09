@echo off
rem Double-click to start a standalone dev Minecraft with the Guhs mod (test worlds are in run\saves).
rem Guhs 1.10.0 Het Vadsrijk, round X (last fixes + zone-1 release prep; all zones open, see SPIEKBRIEF ch. 2 for the release view): make a NEW world (Creative, cheats on) and type /guhs vadsrijk.
rem See SPIEKBRIEF.md (long cheat sheet, NL), TESTEN_RONDE_X.md, RELEASE_CHECKLIST.md and EINDRAPPORT_RONDE_X.md.
rem (In this worktree run\saves starts empty: make a new world.)
cd /d "%~dp0"
set "JAVA_HOME=%APPDATA%\PrismLauncher\java\java-runtime-delta"
call "%~dp0gradlew.bat" runClient
if errorlevel 1 pause
