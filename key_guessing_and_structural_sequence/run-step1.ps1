param(
    [int]$BlockSize = 4,
    [int]$Regime = 3,
    [int]$ExtBefore = 4,
    [int]$Rounds = 22,
    [int]$ExtAfter = 5,
    [double]$Pr = 57.36,
    [int]$Threads = 8,
    [int]$Solutions = 2,
    [string]$Output = "output/step1_(4,3).json"
)

$ErrorActionPreference = "Stop"
$ProjectDir = $PSScriptRoot
Set-Location $ProjectDir

if (-not (Test-Path "$ProjectDir\boomerangsearch.jar")) {
    throw "boomerangsearch.jar not found. Run .\compile.ps1 first."
}

$argsList = @(
    "-ea",
    "-jar", "boomerangsearch.jar",
    "-verbose",
    "-nonOpts1",
    "-bs=$BlockSize",
    "-tk=$Regime",
    "-rb=$ExtBefore",
    "-r=$Rounds",
    "-rf=$ExtAfter",
    "-pr=$Pr",
    "-s1o=$Output",
    "-t=$Threads",
    "-sols1=$Solutions"
)

Write-Host "=== STEP1 solve ===" -ForegroundColor Cyan
Write-Host ("java " + ($argsList -join " "))
& java @argsList
if ($LASTEXITCODE -ne 0) {
    throw "STEP1 solve failed with exit code $LASTEXITCODE"
}
