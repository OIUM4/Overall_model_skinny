$ErrorActionPreference = "Stop"
$ProjectDir = $PSScriptRoot
$LibsDir = Join-Path (Split-Path $ProjectDir -Parent) "newlibs"
$cp = "$LibsDir\*;$ProjectDir"

Write-Host "=== Boomerang Search Compile ===" -ForegroundColor White
New-Item -ItemType Directory -Force -Path "$ProjectDir\output" | Out-Null

$modules = @(
    @{ Name = "step1";           Files = "$ProjectDir\step1\*.java" },
    @{ Name = "step2";           Files = "$ProjectDir\step2\*.java" },
    @{ Name = "solutiontotikz";  Files = "$ProjectDir\solutiontotikz\*.java" },
    @{ Name = "main";            Files = "$ProjectDir\*.java" }
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

# Remove old jars
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
Write-Host "`n=== All compiled successfully ===" -ForegroundColor Yellow
