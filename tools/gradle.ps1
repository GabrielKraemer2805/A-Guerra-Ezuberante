$GradleArgs = $args
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$portable = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools\jdk17') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if ($portable) { $env:JAVA_HOME = $portable.FullName }
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
    throw 'Defina JAVA_HOME para um JDK 17 completo ou prepare um JDK 17 portátil em .tools/jdk17/.'
}
$versionOutput = & (Join-Path $env:JAVA_HOME 'bin\java.exe') --version
if (($versionOutput -join ' ') -notmatch '(openjdk|java) 17\.') { throw 'Este projeto exige um JDK 17. Confira JAVA_HOME.' }
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle-user'
if (-not $GradleArgs) { $GradleArgs = @('build') }
Push-Location $projectRoot
try {
    & (Join-Path $projectRoot 'gradlew.bat') @GradleArgs
    $result = $LASTEXITCODE
} finally { Pop-Location }
exit $result
