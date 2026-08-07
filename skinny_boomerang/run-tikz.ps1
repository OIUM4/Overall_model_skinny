# Generate TikZ figure from Step1 + Step2 outputs (matched by Step1 solution index)
$ErrorActionPreference = "Stop"

$Search = $PSScriptRoot
$Libs   = Join-Path (Split-Path $Search -Parent) "newlibs"

Set-Location $Search

$Step1Input = if ($args[0]) { $args[0] } else { "output/(8,2)_step1.json" }
$Step2Input = if ($args[1]) { $args[1] } else { "output/(8,2)_step2.json" }
$OutputTex  = if ($args[2]) { $args[2] } else { "output/(8,2)_figure.tex" }
$SolNumber  = if ($args[3]) { $args[3] } else { "0" }



foreach ($f in @($Step1Input, $Step2Input)) {
    if (-not (Test-Path $f)) {
        Write-Host "ERROR: file not found: $f" -ForegroundColor Red
        exit 1
    }
}

$jarList = (Get-ChildItem $Libs -Filter "*.jar" | ForEach-Object { $_.FullName }) -join ";"
$cp = "$jarList;$Search"

Write-Host ">>> Step1 grid : $Step1Input (solution #$SolNumber)"
Write-Host ">>> Step2 overlay: $Step2Input (best Step2 for Step1 #$SolNumber)"
Write-Host ">>> Tip: re-run .\run-step2.ps1 after .\compile.ps1 if Step2 JSON has no step1SolutionIndex."
Write-Host ""

Write-Host ">>> Generating Step1 TikZ..."
$tikz1 = & java -cp $cp boomerangsearch.solutiontotikz.SolutionToTikz -s1i $Step1Input -sol $SolNumber 2>&1 | Where-Object { $_ -is [string] }
if ($LASTEXITCODE -ne 0) {
    Write-Host ($tikz1 -join "`n") -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host ">>> Generating Step2 TikZ overlay..."
$tikz2Job = Start-Process -FilePath java -ArgumentList @(
    "-cp", $cp,
    "boomerangsearch.solutiontotikz.SolutionToTikz",
    "-s2i", $Step2Input,
    "-s1sol", $SolNumber,
    "-s2b"
) -WorkingDirectory $Search -RedirectStandardOutput "$Search\output\_tikz2_tmp.txt" -RedirectStandardError "$Search\output\_tikz2_meta.txt" -NoNewWindow -Wait -PassThru

if ($tikz2Job.ExitCode -ne 0) {
    Get-Content "$Search\output\_tikz2_meta.txt" | Write-Host -ForegroundColor Red
    exit $tikz2Job.ExitCode
}

Get-Content "$Search\output\_tikz2_meta.txt" | ForEach-Object { Write-Host $_ -ForegroundColor DarkGray }
$tikz2 = Get-Content "$Search\output\_tikz2_tmp.txt" -ErrorAction SilentlyContinue
Remove-Item "$Search\output\_tikz2_tmp.txt", "$Search\output\_tikz2_meta.txt" -ErrorAction SilentlyContinue

$header = @"
\documentclass[border=5pt]{standalone}
\usepackage{tikz}
\begin{document}
"@

$footer = "\end{document}"

$body = $tikz1 -join "`n"
$overlay = ($tikz2 | Where-Object { $_ }) -join "`n"

$body = $body -replace '\\makeatother\s*\n\\end\{tikzpicture\}', ("`n% Step2 differential values overlay (Step1 #$SolNumber)`n" + $overlay + "`n`n\makeatother`n\end{tikzpicture}")

$content = $header + "`n" + $body + "`n" + $footer

New-Item -ItemType Directory -Force -Path (Split-Path $OutputTex -Parent) | Out-Null
Set-Content -Encoding UTF8 -Path $OutputTex -Value $content
Write-Host ""
Write-Host ">>> SUCCESS: $OutputTex created" -ForegroundColor Green
Write-Host ">>> Compile with: pdflatex $OutputTex"
