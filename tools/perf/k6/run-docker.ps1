param(
  [string]$Scenario = "smoke",
  [string]$BaseUrl = $(If ($env:BASE_URL) { $env:BASE_URL } Else { "http://host.docker.internal:8080" }),
  [int]$Vu = $(If ($env:VU) { [int]$env:VU } Else { 5 }),
  [string]$Duration = $(If ($env:DURATION) { $env:DURATION } Else { "2m" }),
  [string]$ResultsDir
)

$ErrorActionPreference = "Stop"

$localScripts = (Convert-Path $PSScriptRoot)
$containerScripts = "/scripts"
$scriptInContainer = "$containerScripts/scenarios/$Scenario.js"

if (-not $ResultsDir) { $ResultsDir = Join-Path $localScripts 'results' }
if (-not (Test-Path $ResultsDir)) { New-Item -ItemType Directory -Path $ResultsDir | Out-Null }

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$summaryHost = Join-Path $ResultsDir "$Scenario-$timestamp.json"
$summaryInContainer = "$containerScripts/results/$Scenario-$timestamp.json"

# Use native Windows path for Docker Desktop volume mounting
$localScriptsDocker = $localScripts

Write-Host "Running k6 in Docker: $Scenario"
Write-Host "BASE_URL=$BaseUrl VU=$Vu DURATION=$Duration"

docker run --rm -i `
  -e BASE_URL=$BaseUrl `
  -e VU=$Vu `
  -e DURATION=$Duration `
  -v "$localScriptsDocker":"$containerScripts" `
  grafana/k6:latest run --summary-export "$summaryInContainer" "$scriptInContainer"

Write-Host "Summary exported: $summaryHost"


