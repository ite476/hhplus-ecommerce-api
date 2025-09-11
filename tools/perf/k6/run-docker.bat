@echo off
setlocal

REM Usage: run-docker.bat [scenario]
set SCENARIO=%1
if "%SCENARIO%"=="" set SCENARIO=smoke

if "%BASE_URL%"=="" set BASE_URL=http://host.docker.internal:8080
if "%VU%"=="" set VU=5
if "%DURATION%"=="" set DURATION=2m

set "SCRIPT_DIR=%~dp0"
REM Trim trailing backslash to avoid Docker -v quoting issues
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

set OUT_ARG=
if not "%INFLUX_URL%"=="" set OUT_ARG=--out influxdb=%INFLUX_URL%

echo Running k6 in Docker: %SCENARIO%
echo BASE_URL=%BASE_URL% VU=%VU% DURATION=%DURATION% INFLUX_URL=%INFLUX_URL%

docker run --rm -e BASE_URL=%BASE_URL% -e VU=%VU% -e DURATION=%DURATION% %OUT_ARG% -v "%SCRIPT_DIR%:/scripts" grafana/k6:latest run --summary-export /scripts/results/%SCENARIO%-latest.json /scripts/scenarios/%SCENARIO%.js

endlocal

