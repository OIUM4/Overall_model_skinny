# Generate HTML visualization from Step1 JSON output
$ErrorActionPreference = "Stop"

$Search = $PSScriptRoot
$Libs   = Join-Path (Split-Path $Search -Parent) "newlibs"

Set-Location $Search

$Step1Input = if ($args[0]) { $args[0] } else { "output/(8,2)_step1.json" }
$OutputHtml = if ($args[1]) { $args[1] } else { "output/(8,2)_step1_visual.html" }
$SolNumber  = if ($args[2]) { $args[2] } else { "0" }

if (-not (Test-Path $Step1Input)) {
    Write-Host "ERROR: file not found: $Step1Input" -ForegroundColor Red
    exit 1
}

$jarList = (Get-ChildItem $Libs -Filter "*.jar" | ForEach-Object { $_.FullName }) -join ";"
$cp = "$jarList;$Search"

Write-Host ">>> Input : $Step1Input"
Write-Host ">>> Output: $OutputHtml"
Write-Host ">>> Solution #$SolNumber"

& java -cp $cp boomerangsearch.solutiontotikz.Step1ToHtml $Step1Input $OutputHtml $SolNumber

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host ">>> Opening in browser..." -ForegroundColor Green
    Start-Process $OutputHtml
} else {
    Write-Host ">>> FAILED" -ForegroundColor Red
    exit 1
}
