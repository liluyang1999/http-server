# Generates a local, short-lived certificate using the installed JDK; no downloads.
param([string]$Destination = (Join-Path $PSScriptRoot '..\.demo-tls\server.p12'))
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$target = [IO.Path]::GetFullPath($Destination)
$allowedRoot = Join-Path $projectRoot '.demo-tls'
if (-not $target.StartsWith($allowedRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Destination must be inside this project/.demo-tls.' }
if ([IO.Path]::GetDirectoryName($target) -ine $allowedRoot) { throw 'Use a file directly in .demo-tls, without nested directories.' }
if (Test-Path -LiteralPath $target) { throw 'Refusing to overwrite an existing keystore.' }
if ((Test-Path -LiteralPath $allowedRoot) -and ((Get-Item -LiteralPath $allowedRoot).Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw '.demo-tls must not be a link.' }
if (-not $env:HTTP_TLS_PASSWORD -or $env:HTTP_TLS_PASSWORD.Length -lt 12) { throw 'Set HTTP_TLS_PASSWORD to a local demo password of at least 12 characters first.' }
$null = Get-Command keytool -ErrorAction Stop
$null = New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force
& keytool -genkeypair -alias local-demo -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore $target -storepass:env HTTP_TLS_PASSWORD -keypass:env HTTP_TLS_PASSWORD -validity 7 -dname 'CN=localhost' -ext 'SAN=dns:localhost,ip:127.0.0.1'
if ($LASTEXITCODE -ne 0) { throw 'keytool failed.' }
Write-Output ('Created local demo keystore: ' + $target)
Write-Output 'The certificate is self-signed and is not installed into any system trust store.'
