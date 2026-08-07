$ErrorActionPreference = "Stop"
$ProjectDir = $PSScriptRoot
$LibsDir = Join-Path (Split-Path $ProjectDir -Parent) "newlibs"
$cp = "$LibsDir\*;$ProjectDir"

Write-Host "=== STEP1-only Boomerang Search Compile ===" -ForegroundColor White

if (-not (Test-Path $LibsDir)) {
    throw "Dependency directory not found: $LibsDir`nCreate ../newlibs and place the required jars there. See README.md."
}

$requiredJars = @(
    "jackson-annotations-2.11.0.jar",
    "jackson-core-2.11.0.jar",
    "jackson-databind-2.11.0.jar",
    "picocli-4.3.2.jar",
    "gurobi.jar"
)
foreach ($jar in $requiredJars) {
    $path = Join-Path $LibsDir $jar
    if (-not (Test-Path $path)) {
        throw "Missing dependency: $path"
    }
}

New-Item -ItemType Directory -Force -Path "$ProjectDir\output" | Out-Null

# Remove stale compiled classes first, especially classes from the deleted STEP2 phase.
if (Test-Path "$ProjectDir\boomerangsearch") {
    Remove-Item -Recurse -Force "$ProjectDir\boomerangsearch"
}

$modules = @(
    @{ Name = "step1";          Files = "$ProjectDir\step1\*.java" },
    @{ Name = "solutiontotikz"; Files = "$ProjectDir\solutiontotikz\*.java" },
    @{ Name = "main";           Files = "$ProjectDir\*.java" }
)

$i = 0
foreach ($m in $modules) {
    $i++
    Write-Host "[$i/$($modules.Count + 2)] Compiling $($m.Name) ..." -ForegroundColor Cyan
    $files = Get-ChildItem $m.Files | ForEach-Object { $_.FullName }
    & javac -encoding UTF-8 -cp $cp -d $ProjectDir $files
    if ($LASTEXITCODE -ne 0) { throw "$($m.Name) compilation failed" }
    Write-Host "      Done" -ForegroundColor Green
}

$i++
Write-Host "[$i/$($modules.Count + 2)] Packaging jars ..." -ForegroundColor Cyan
Push-Location $ProjectDir
foreach ($jar in @("boomerangsearch.jar","solutiontotikz.jar")) {
    if (Test-Path $jar) { Remove-Item -Force $jar }
}
& jar cfm boomerangsearch.jar META-INF\boomerangManifest.txt boomerangsearch\
if ($LASTEXITCODE -ne 0) { throw "Packaging boomerangsearch.jar failed" }
& jar cfm solutiontotikz.jar META-INF\solutionToTikzManifest.txt boomerangsearch\
if ($LASTEXITCODE -ne 0) { throw "Packaging solutiontotikz.jar failed" }
Pop-Location

$i++
Write-Host "[$i/$($modules.Count + 2)] Done" -ForegroundColor Green
Write-Host "`n=== STEP1-only build completed successfully ===" -ForegroundColor Yellow
