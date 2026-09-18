param([switch]$Test)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$buildRoot = Join-Path $projectRoot 'build'
if ((Test-Path -LiteralPath $buildRoot) -and ((Get-Item -LiteralPath $buildRoot).Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'build must not be a link.' }
$stage = Join-Path $buildRoot ('.compile-' + [Guid]::NewGuid().ToString('N'))
$classes = Join-Path $stage 'classes'
$testClasses = Join-Path $stage 'tests'
$dependency = Join-Path $projectRoot 'lib\gson-2.8.9.jar'
foreach ($commandName in @('javac','java','jar')) { $null = Get-Command $commandName -ErrorAction Stop }
if (-not (Test-Path -LiteralPath $dependency)) { throw 'The committed Gson JAR is missing; no dependency will be downloaded.' }
$null = New-Item -ItemType Directory -Path $classes,$testClasses -Force
try {
    $sources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src') -Filter '*.java' -Recurse | ForEach-Object FullName)
    & javac --release 19 -encoding UTF-8 -Xlint:all -Werror -cp $dependency -d $classes @sources
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
    Copy-Item -LiteralPath (Join-Path $projectRoot 'src\web.properties') -Destination $classes
    if (Test-Path -LiteralPath (Join-Path $projectRoot 'src\web')) { Copy-Item -LiteralPath (Join-Path $projectRoot 'src\web') -Destination $classes -Recurse }
    if ($Test) {
        $tests = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'tests') -Filter '*.java' -Recurse | ForEach-Object FullName)
        & javac --release 19 -encoding UTF-8 -Xlint:all -Werror -cp "$classes;$dependency" -d $testClasses @tests
        if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
        foreach ($testFile in (Get-ChildItem -LiteralPath (Join-Path $projectRoot 'tests') -Filter '*Test.java')) {
            & java "-Djdk.net.unixdomain.tmpdir=$buildRoot" -ea -cp "$classes;$testClasses;$dependency" $testFile.BaseName
            if ($LASTEXITCODE -ne 0) { throw ('Test failed: ' + $testFile.BaseName) }
        }
    }
    $null = New-Item -ItemType Directory -Path (Join-Path $buildRoot 'lib') -Force
    Copy-Item -LiteralPath $dependency -Destination (Join-Path $buildRoot 'lib\gson-2.8.9.jar')
    $manifest = Join-Path $stage 'MANIFEST.MF'
    [IO.File]::WriteAllText($manifest, "Manifest-Version: 1.0`nMain-Class: Main`nClass-Path: lib/gson-2.8.9.jar`n`n", [Text.UTF8Encoding]::new($false))
    $stageJar = Join-Path $stage 'http-server.jar'
    & jar --create --file $stageJar --manifest $manifest --date=2023-06-24T00:00:00Z -C $classes .
    if ($LASTEXITCODE -ne 0) { throw 'JAR packaging failed.' }
    [IO.File]::Move($stageJar, (Join-Path $buildRoot 'http-server.jar'), $true)
    if ($Test) { & (Join-Path $PSScriptRoot 'test-launcher.ps1') }
    Write-Output 'BUILD_OK build/http-server.jar (offline, Java release 19)'
} finally {
    $resolvedStage = [IO.Path]::GetFullPath($stage)
    if (-not $resolvedStage.StartsWith($buildRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe temporary directory.' }
    if (Test-Path -LiteralPath $resolvedStage) { Remove-Item -LiteralPath $resolvedStage -Recurse -Force }
}
