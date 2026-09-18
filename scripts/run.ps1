param(
    [ValidateSet('jdk','socket','client')][string]$Mode = 'jdk',
    [ValidateRange(0,65535)][int]$Port = 0,
    [ValidateRange(0,65535)][int]$HttpsPort = 0,
    [string]$KeyStore = '',
    [string]$Url = ''
)
$ErrorActionPreference = 'Stop'
$hasPort = $PSBoundParameters.ContainsKey('Port')
$hasTls = $PSBoundParameters.ContainsKey('HttpsPort')
if ($Mode -ne 'jdk' -and ($hasTls -or $KeyStore)) { throw 'HTTPS options require -Mode jdk.' }
if ($Mode -eq 'client' -and $hasPort) { throw 'Client mode uses -Url, not -Port.' }
if ($Mode -ne 'client' -and $Url) { throw '-Url requires -Mode client.' }
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$buildRoot = Join-Path $projectRoot 'build'
$jar = Join-Path $buildRoot 'http-server.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Run scripts/build.ps1 -Test first.' }
$jvm = @("-Djdk.net.unixdomain.tmpdir=$buildRoot")
switch ($Mode) {
    'jdk' {
        $arguments = @('-jar',$jar)
        if ($hasPort) { $arguments += @('--port',"$Port") }
        if ($hasTls) {
            if (-not $KeyStore) { throw 'HTTPS requires -KeyStore and HTTP_TLS_PASSWORD in the environment.' }
            $arguments += @('--https-port',"$HttpsPort",'--keystore',$KeyStore)
        } elseif ($KeyStore) { throw '-KeyStore requires -HttpsPort.' }
    }
    'socket' {
        if (-not $hasPort) { $Port = 9999 }
        $arguments = @('-cp',$jar,'server.Server',"$Port")
    }
    'client' {
        $arguments = @('-cp',$jar,'client.Client')
        if ($Url) { $arguments += $Url }
    }
}
& java @jvm @arguments
if ($LASTEXITCODE -ne 0) { throw ('Java exited with code ' + $LASTEXITCODE) }
