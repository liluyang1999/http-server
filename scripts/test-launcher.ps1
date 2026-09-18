# Validate argument handling without starting a server or needing a free port.
$ErrorActionPreference = 'Stop'
$launches = [Collections.Generic.List[object]]::new()
function java {
    $launches.Add(@($args))
    $global:LASTEXITCODE = 0
}
$checks = 0
foreach ($arguments in @(
    @{ Port = -2 }, @{ Port = -1 }, @{ Port = 65536 },
    @{ HttpsPort = -2 }, @{ HttpsPort = -1 }, @{ HttpsPort = 65536 },
    @{ Mode = 'socket'; HttpsPort = 8443 }, @{ Mode = 'socket'; KeyStore = 'example.p12' },
    @{ Mode = 'client'; Port = 8081 }, @{ Mode = 'client'; HttpsPort = 8443 },
    @{ Mode = 'jdk'; Url = 'http://localhost/' }, @{ Mode = 'socket'; Url = 'http://localhost/' },
    @{ KeyStore = 'example.p12' }, @{ HttpsPort = 8443 }
)) {
    $before = $launches.Count
    $rejected = $false
    try { & (Join-Path $PSScriptRoot 'run.ps1') @arguments }
    catch { $rejected = $true }
    if (-not $rejected -or $launches.Count -ne $before) {
        throw ('Invalid arguments reached Java: ' + ($arguments | ConvertTo-Json -Compress))
    }
    $checks++
}
foreach ($arguments in @(@{ Port = 0 }, @{ Mode = 'socket'; Port = 0 }, @{ Mode = 'client'; Url = 'http://127.0.0.1:9999/' })) {
    $before = $launches.Count
    & (Join-Path $PSScriptRoot 'run.ps1') @arguments
    if ($launches.Count -ne $before + 1) { throw 'Valid arguments did not reach Java.' }
    $checks++
}
Write-Output "LAUNCHER_ARGUMENTS_OK $checks"
