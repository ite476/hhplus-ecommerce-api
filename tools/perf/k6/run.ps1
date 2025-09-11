param(
  [string]$Scenario = "smoke",
  [string]$BaseUrl = $env:BASE_URL,
  [int]$Vu = $(If ($env:VU) { [int]$env:VU } Else { 5 }),
  [string]$Duration = $(If ($env:DURATION) { $env:DURATION } Else { "2m" }),
  [string]$InfluxUrl = $env:INFLUX_URL
)

$ErrorActionPreference = "Stop"

if (-not $BaseUrl) { $BaseUrl = "http://localhost:8080" }

$scriptPath = Join-Path $PSScriptRoot "scenarios/$Scenario.js"
if (-not (Test-Path $scriptPath)) {
  Write-Error "Scenario script not found: $scriptPath"
}

$resultsDir = Join-Path $PSScriptRoot "results"
if (-not (Test-Path $resultsDir)) { New-Item -ItemType Directory -Path $resultsDir | Out-Null }

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$summaryFile = Join-Path $resultsDir "$Scenario-$timestamp.json"

Write-Host "Running k6 scenario: $Scenario"
Write-Host "BASE_URL=$BaseUrl VU=$Vu DURATION=$Duration"

if ($InfluxUrl) {
  $outArg = "--out influxdb=$InfluxUrl"
} else {
  $outArg = ""
}

k6 run `
  -e BASE_URL=$BaseUrl `
  -e VU=$Vu `
  -e DURATION=$Duration `
  --summary-export "$summaryFile" `
  $outArg `
  "$scriptPath"

Write-Host "Summary exported: $summaryFile"

