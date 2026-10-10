$projectRoot = Split-Path -Parent $PSScriptRoot
$toolsRoot = Join-Path $projectRoot '.tools'

$jdkHome = Get-ChildItem -Directory (Join-Path $toolsRoot 'jdk17') |
    Select-Object -First 1 -ExpandProperty FullName
$nodeHome = Get-ChildItem -Directory (Join-Path $toolsRoot 'node22') |
    Select-Object -First 1 -ExpandProperty FullName
$gitHome = Join-Path $toolsRoot 'git\cmd'
$ghHome = Join-Path $toolsRoot 'gh\bin'

if (-not $jdkHome -or -not $nodeHome) {
    throw '找不到專案本地 JDK 17 或 Node.js，請先完成環境準備。'
}

$env:JAVA_HOME = $jdkHome
$env:Path = @(
    (Join-Path $jdkHome 'bin')
    $nodeHome
    $gitHome
    $ghHome
    $env:Path
) -join [IO.Path]::PathSeparator

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host '已為目前 PowerShell 工作階段啟用專案本地 JDK、Node.js、Git 與 GitHub CLI。'
