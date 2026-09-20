# Starts Aionn Modulith Backend with local env files loaded
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$envFiles = @(
    ".env",
    "envs/common.env",
    "envs/identity.env",
    "envs/catalog.env",
    "envs/inventory.env",
    "envs/ordering.env",
    "envs/payment.env",
    "envs/shipping.env",
    "envs/promotion.env",
    "envs/notification.env",
    "envs/chat.env",
    "envs/recommendation.env"
)

foreach ($file in $envFiles) {
    $fullPath = Join-Path $root $file
    if (Test-Path -LiteralPath $fullPath) {
        Get-Content $fullPath | ForEach-Object {
            $line = $_.Trim()
            if ($line -and -not $line.StartsWith("#")) {
                if ($line -match "^([^=]+)=(.*)$") {
                    $key = $Matches[1].Trim()
                    $val = $Matches[2].Trim()
                    if (($val.StartsWith('"') -and $val.EndsWith('"')) -or ($val.StartsWith("'") -and $val.EndsWith("'"))) {
                        $val = $val.Substring(1, $val.Length - 2)
                    }
                    [System.Environment]::SetEnvironmentVariable($key, $val, [System.EnvironmentVariableTarget]::Process)
                    Set-Item "env:$key" $val
                }
            }
        }
    }
}

if (-not $env:SPRING_PROFILES_ACTIVE) {
    $env:SPRING_PROFILES_ACTIVE = "dev"
}

Write-Host "Environment configured for Aionn Backend. Starting application..."
Set-Location $root
& .\gradlew.bat :app:bootRun

