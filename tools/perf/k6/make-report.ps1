param(
  [string]$ResultsDir,
  [string]$OutFile
)

$ErrorActionPreference = "Stop"

if (-not $ResultsDir) { $ResultsDir = Join-Path $PSScriptRoot 'results' }
if (-not $OutFile) { $OutFile = (Join-Path $PSScriptRoot '../../docs/issue-#34/perf-analysis.md') }

function Get-MetricValue {
  param($metrics, [string]$name, [string]$field)
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

function Summarize-Result {
  param($jsonPath)
  $data = Get-Content $jsonPath -Raw | ConvertFrom-Json
  $metrics = $data.metrics
  $summary = [ordered]@{}
  $summary.Scenario = [System.IO.Path]::GetFileNameWithoutExtension($jsonPath)
  $summary.VUs = Get-MetricValue $metrics 'vus' 'value'
  $summary.VUsMax = Get-MetricValue $metrics 'vus_max' 'value'
  $summary.Iterations = Get-MetricValue $metrics 'iterations' 'count'
  $summary.RPS = Get-MetricValue $metrics 'http_reqs' 'rate'
  $summary.Requests = Get-MetricValue $metrics 'http_reqs' 'count'
  $summary.ErrorRate = Get-MetricValue $metrics 'http_req_failed' 'rate'
  $summary.P50 = Get-MetricValue $metrics 'http_req_duration' 'p(50)'
  $summary.P90 = Get-MetricValue $metrics 'http_req_duration' 'p(90)'
  $summary.P95 = Get-MetricValue $metrics 'http_req_duration' 'p(95)'
  $summary.P99 = Get-MetricValue $metrics 'http_req_duration' 'p(99)'
  return $summary
}

if (-not (Test-Path $ResultsDir)) {
  Write-Error "Results directory not found: $ResultsDir"
}

$files = Get-ChildItem -Path $ResultsDir -Filter *.json | Sort-Object LastWriteTime -Descending
if ($files.Count -eq 0) {
  Write-Error "No k6 summary JSON found in $ResultsDir"
}

$rows = @()
foreach ($f in $files) {
  try { $rows += (Summarize-Result $f.FullName) } catch { Write-Warning "Skip $($f.Name): $($_.Exception.Message)" }
}

$outPath = $OutFile
if (-not [System.IO.Path]::IsPathRooted($outPath)) {
  $outPath = Join-Path (Resolve-Path ".").Path $outPath
}
$outDir = Split-Path -Parent $outPath
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }

$md = @()
$md += "# k6 Performance Analysis"
$md += ""
$md += "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$md += ""
$md += "| Scenario | VUs | VUsMax | RPS | Requests | ErrorRate | p50(ms) | p90(ms) | p95(ms) | p99(ms) |"
$md += "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|"
foreach ($r in $rows) {
  $rps = if ($null -ne $r.RPS) { [double]$r.RPS } else { 0 }
  $req = if ($null -ne $r.Requests) { [double]$r.Requests } else { 0 }
  $err = if ($null -ne $r.ErrorRate) { [double]$r.ErrorRate } else { 0 }
  $p50 = if ($null -ne $r.P50) { [double]$r.P50 } else { 0 }
  $p90 = if ($null -ne $r.P90) { [double]$r.P90 } else { 0 }
  $p95 = if ($null -ne $r.P95) { [double]$r.P95 } else { 0 }
  $p99 = if ($null -ne $r.P99) { [double]$r.P99 } else { 0 }
  $md += "| $($r.Scenario) | $($r.VUs) | $($r.VUsMax) | {0:N2} | {1:N0} | {2:P2} | {3:N2} | {4:N2} | {5:N2} | {6:N2} |" -f $rps, $req, $err, $p50, $p90, $p95, $p99
}

Set-Content -Path $OutFile -Value ($md -join "`n") -Encoding UTF8
Write-Host "Report written: $OutFile"


