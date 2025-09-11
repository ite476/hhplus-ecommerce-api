@echo off
setlocal

REM Usage: run.bat [scenario]
set SCENARIO=%1
if "%SCENARIO%"=="" set SCENARIO=smoke

set SCRIPT_DIR=%~dp0
powershell -ExecutionPolicy Bypass -File "%SCRIPT_DIR%run.ps1" -Scenario %SCENARIO%

endlocal

