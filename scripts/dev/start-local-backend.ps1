# Loads envs/*.env into the process environment, then starts the backend.
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

# common.env first so module files override the shared defaults.
$envFiles = @("common", "identity", "catalog", "inventory", "ordering", "payment",
    "shipping", "promotion", "notification", "chat", "recommendation")

foreach ($name in $envFiles) {
    $path = Join-Path $root "envs/$name.env"
    if (-not (Test-Path -LiteralPath $path)) { continue }
    foreach ($line in Get-Content -LiteralPath $path) {
        $line = $line.Trim()
        if (-not $line -or $line.StartsWith("#")) { continue }
        $separator = $line.IndexOf("=")
        if ($separator -le 0) { continue }
        $key = $line.Substring(0, $separator).Trim()
        $value = $line.Substring($separator + 1).Trim()
        if ($value.Length -ge 2 -and
            (($value[0] -eq '"' -and $value[-1] -eq '"') -or ($value[0] -eq "'" -and $value[-1] -eq "'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        [System.Environment]::SetEnvironmentVariable($key, $value, [System.EnvironmentVariableTarget]::Process)
    }
}

if (-not $env:SPRING_PROFILES_ACTIVE) { $env:SPRING_PROFILES_ACTIVE = "dev" }

Write-Host "Environment loaded from envs/*.env. Starting application..."
Set-Location $root
& .\gradlew.bat :app:bootRun
