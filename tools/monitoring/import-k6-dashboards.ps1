$ErrorActionPreference = "Stop"

$dashDir = Join-Path $PSScriptRoot "dashboards"
if (-not (Test-Path $dashDir)) { New-Item -ItemType Directory -Path $dashDir | Out-Null }

function Download-Dashboard {
  param(
    [int]$Id,
    [string]$OutFile
  )
  $urls = @(
    "https://grafana.com/api/dashboards/$Id/revisions/latest/download",
    "https://grafana.com/api/dashboards/$Id/revisions/last/download"
  )
  foreach ($u in $urls) {
    try {
      Invoke-WebRequest -UseBasicParsing -Uri $u -OutFile $OutFile
      Write-Host ("Downloaded {0} from {1} -> {2}" -f $Id, $u, $OutFile)
      return
    } catch {
      Write-Host ("Failed {0}: {1}" -f $u, $_.Exception.Message) -ForegroundColor Yellow
    }
  }
  throw ("Failed to download dashboard {0}" -f $Id)
}

Download-Dashboard -Id 2587 -OutFile (Join-Path $dashDir "k6-2587.json")
Download-Dashboard -Id 19630 -OutFile (Join-Path $dashDir "k6-19630.json")

Write-Host "Dashboards downloaded. Restart Grafana to apply provisioning."

