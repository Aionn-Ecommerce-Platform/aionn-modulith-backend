param(
    [ValidateSet("all", "identity", "catalog", "inventory", "ordering")]
    [string]$Module = "all"
)

# This PowerShell script starts the application with mock configuration, runs the specified E2E module tests, and cleans up after completion.

# 1. Stop any running gradle daemons first
Write-Host "Stopping any running gradle daemons..."
.\gradlew --stop

# 2. Load env files
Get-Content envs/common.env, envs/identity.env, envs/catalog.env, envs/inventory.env, envs/ordering.env | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        if ($line -match "^([^=]+)=(.*)$") {
            $key = $Matches[1].Trim()
            $val = $Matches[2].Trim()
            if ($val.StartsWith('"') -and $val.EndsWith('"')) {
                $val = $val.Substring(1, $val.Length - 2)
            }
            if ($val.StartsWith("'") -and $val.EndsWith("'")) {
                $val = $val.Substring(1, $val.Length - 2)
            }
            [System.Environment]::SetEnvironmentVariable($key, $val, [System.EnvironmentVariableTarget]::Process)
            Set-Item "env:$key" $val
        }
    }
}

# Override to use mock providers for local testing / smoke testing
$overrides = @{
    "CAPTCHA_PROVIDER" = "mock"
    "CAPTCHA_EXPECTED_TOKEN" = ""
    "TWILIO_ENABLED" = "false"
    "IDENTITY_AUTH_GOOGLE_PROVIDER" = "mock"
    "IDENTITY_AUTH_FACEBOOK_PROVIDER" = "mock"
    "IDENTITY_MEDIA_PROVIDER" = "mock"
    "IDENTITY_KYC_PROVIDER" = "local"
}

foreach ($key in $overrides.Keys) {
    $val = $overrides[$key]
    [System.Environment]::SetEnvironmentVariable($key, $val, [System.EnvironmentVariableTarget]::Process)
    Set-Item "env:$key" $val
}

# 3. Start application in the background
Write-Host "Starting application with mock/local environment under background..."
$process = Start-Process -FilePath "powershell.exe" -ArgumentList "-Command .\gradlew :app:bootRun --no-daemon" -PassThru -NoNewWindow

# Register cleanup block to run on exit
$cleanup = {
    param($proc)
    Write-Host "Stopping application server (PID: $($proc.Id))..."
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
}

try {
    # 4. Wait for the app to become healthy
    $baseUrl = "http://127.0.0.1:8080"
    $healthUrl = "$baseUrl/actuator/health"
    Write-Host "Waiting for app to start up at $healthUrl..."

    $healthy = $false
    for ($i = 1; $i -le 90; $i++) {
        if ($process.HasExited) {
            Write-Error "Application process died during startup."
            exit 1
        }

        try {
            $response = Invoke-RestMethod -Uri $healthUrl -UseBasicParsing -ErrorAction SilentlyContinue
            if ($response.status -eq "UP") {
                Write-Host "Application is UP and healthy!"
                $healthy = $true
                break
            }
        } catch {}

        Start-Sleep -Seconds 2
    }

    if (-not $healthy) {
        Write-Error "Application did not become healthy after 90 seconds."
        exit 1
    }

    # 5. Run the requested module E2E smoke tests using bash
    if ($Module -eq "identity" -or $Module -eq "all") {
        Write-Host "Running E2E tests for Identity module..."
        bash scripts/identity/test-identity-e2e.sh
    }
    
    if ($Module -eq "catalog" -or $Module -eq "all") {
        Write-Host "Running E2E tests for Catalog module..."
        bash scripts/catalog/test-catalog-e2e.sh
    }

    if ($Module -eq "inventory" -or $Module -eq "all") {
        Write-Host "Running E2E tests for Inventory module..."
        bash scripts/inventory/test-inventory-e2e.sh
    }
 
    if ($Module -eq "ordering" -or $Module -eq "all") {
        Write-Host "Running E2E tests for Ordering module..."
        bash scripts/ordering/test-ordering-e2e.sh
    }
}
finally {
    # 6. Ensure the app is stopped
    & $cleanup $process
}
