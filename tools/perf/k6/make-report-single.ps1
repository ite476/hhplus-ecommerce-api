param(
  [Parameter(Mandatory=$true)][string]$InputJson,
  [Parameter(Mandatory=$true)][string]$OutFile
)

$ErrorActionPreference = "Stop"

function Get-MetricValue { param($metrics, [string]$name, [string]$field)
  function Get-PropValue { param($obj, [string]$propName)
    if ($null -eq $obj) { return $null }
    $p = $obj.PSObject.Properties | Where-Object { $_.Name -eq $propName } | Select-Object -First 1
    if ($p) { return $p.Value } else { return $null }
  }
  if ($null -ne $metrics.$name) {
    $m = $metrics.$name
    $count = Get-PropValue $m 'count'; if ($field -eq 'count' -and $null -ne $count) { return [double]$count }
    $rate = Get-PropValue $m 'rate'; if ($field -eq 'rate' -and $null -ne $rate) { return [double]$rate }
    $value = Get-PropValue $m 'value'; if ($field -eq 'value' -and $null -ne $value) { return [double]$value }
    $values = Get-PropValue $m 'values'; if ($null -ne $values) { $val = Get-PropValue $values $field; if ($null -ne $val) { return [double]$val } }
    $percentiles = Get-PropValue $m 'percentiles'; if ($null -ne $percentiles) { $val = Get-PropValue $percentiles $field; if ($null -ne $val) { return [double]$val } }
  }
  return $null
}

$data = Get-Content $InputJson -Raw | ConvertFrom-Json
$m = $data.metrics

$scenario = [System.IO.Path]::GetFileNameWithoutExtension($InputJson)
$vu = Get-MetricValue $m 'vus' 'value'
$rps = Get-MetricValue $m 'http_reqs' 'rate'
$err = Get-MetricValue $m 'http_req_failed' 'rate'
$p50 = Get-MetricValue $m 'http_req_duration' 'p(50)'
$p90 = Get-MetricValue $m 'http_req_duration' 'p(90)'
$p95 = Get-MetricValue $m 'http_req_duration' 'p(95)'
$p99 = Get-MetricValue $m 'http_req_duration' 'p(99)'

$lines = @()
$lines += "# $scenario"
$lines += ""
$lines += "## 📊 핵심 지표"
$lines += "| 항목 | 값 |"
$lines += "|---|---|"
$lines += "| VU | $vu |"
$lines += "| RPS | {0:N2} |" -f $rps
$lines += "| Error rate | {0:P2} |" -f $err
$lines += "| p50 (ms) | {0:N2} |" -f $p50
$lines += "| p90 (ms) | {0:N2} |" -f $p90
$lines += "| p95 (ms) | {0:N2} |" -f $p95
$lines += "| p99 (ms) | {0:N2} |" -f $p99
$lines += ""
$lines += "## 📝 관찰"
$lines += "- "

$outDir = Split-Path -Parent $OutFile
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }
Set-Content -Path $OutFile -Value ($lines -join "`n") -Encoding UTF8
Write-Host "Scenario report written: $OutFile"


