param(
    [ValidateSet("all", "identity", "catalog", "inventory", "ordering", "payment", "shipping", "promotion", "notification", "chat", "recommendation")]
    [string]$Module = "all"
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

# This PowerShell script starts the application with mock configuration, runs the specified E2E module tests, and cleans up after completion.

# 1. Stop any running gradle daemons first
Write-Host "Stopping any running gradle daemons..."
.\gradlew --stop

# 2. Load env files
Get-Content envs/common.env, envs/identity.env, envs/catalog.env, envs/inventory.env, envs/ordering.env, envs/payment.env, envs/shipping.env, envs/promotion.env, envs/notification.env, envs/chat.env, envs/recommendation.env | ForEach-Object {
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
    "FLYWAY_ENABLED" = "true"

    # The suite creates an isolated database per run, but the dev profile adds classpath:db-demo to the
    # Flyway locations and seeds it with hundreds of demo orders. An order-expiry job then auto-cancels
    # them at startup, and the outbox dispatcher spends the whole run working through that backlog ahead
    # of anything the E2E scripts actually produce - so a script waiting on an integration event times
    # out while the queue is still full of data nobody asked for. application-dev.yml contains nothing
    # but those locations, and the only profile-gated bean in the codebase is @Profile("prod"), so
    # running without dev changes nothing except the absence of demo data.
    "SPRING_PROFILES_ACTIVE" = "e2e"

    # Shorten the recommendation offline-job cadence so a behavioural E2E can observe a profile refresh
    # inside one run instead of waiting the production fifteen minutes. ShedLock's lockAtLeastFor still
    # floors the effective period at thirty seconds, so the E2E script polls rather than sleeping once.
    "RECOMMENDATION_SCHEDULING_PROFILE_REFRESH_DELAY_MS" = "10000"
    "RECOMMENDATION_SCHEDULING_POPULARITY_DELAY_MS" = "10000"
    "RECOMMENDATION_SCHEDULING_ITEM_SIMILARITY_DELAY_MS" = "10000"

    # Shorten the recommendation cache TTLs for the same reason. The home slate is cached per user in
    # Redis for fifteen minutes, so a script that polls for a newly rebuilt profile would keep being
    # served the slate it cached before the refresh existed. Redis is shared across runs rather than
    # isolated with the database, which is also why trending - a single shared key - is shortened.
    "RECOMMENDATION_CACHE_HOME_L1_TTL_SECONDS" = "5"
    "RECOMMENDATION_CACHE_HOME_L2_TTL_SECONDS" = "5"
    "RECOMMENDATION_CACHE_TRENDING_L1_TTL_SECONDS" = "5"
    "RECOMMENDATION_CACHE_TRENDING_L2_TTL_SECONDS" = "5"
}

foreach ($key in $overrides.Keys) {
    $val = $overrides[$key]
    [System.Environment]::SetEnvironmentVariable($key, $val, [System.EnvironmentVariableTarget]::Process)
    Set-Item "env:$key" $val
}

$baseUrl = "http://127.0.0.1:8080"
$healthUrl = "$baseUrl/actuator/health"
$existingListener = Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue |
    Select-Object -First 1
if ($existingListener) {
    throw "Port 8080 is already in use by PID $($existingListener.OwningProcess). Stop it before running E2E."
}

$tempDatabase = "aionn_e2e_$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
$createDatabaseSql = "CREATE DATABASE $tempDatabase;"
docker exec aionn-modulith-postgres psql -v ON_ERROR_STOP=1 `
    -U $env:POSTGRES_USER -d postgres -c $createDatabaseSql
if ($LASTEXITCODE -ne 0) {
    throw "Unable to create isolated E2E database $tempDatabase."
}
$env:POSTGRES_DB = $tempDatabase

# Register cleanup block to run on exit
$cleanup = {
    param($proc, $applicationListenerPid, $database, $databaseUser)
    if ($null -ne $proc) {
        Write-Host "Stopping application launcher (PID: $($proc.Id))..."
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    }
    $listener = Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue |
        Where-Object OwningProcess -eq $applicationListenerPid |
        Select-Object -First 1
    if ($null -ne $applicationListenerPid -and $listener) {
        Write-Host "Stopping application server (PID: $applicationListenerPid)..."
        Stop-Process -Id $listener.OwningProcess -Force -ErrorAction SilentlyContinue
    }
    $terminateConnectionsSql = "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$database' AND pid <> pg_backend_pid();"
    docker exec aionn-modulith-postgres psql -v ON_ERROR_STOP=1 `
        -U $databaseUser -d postgres -c $terminateConnectionsSql | Out-Null
    docker exec aionn-modulith-postgres psql -v ON_ERROR_STOP=1 `
        -U $databaseUser -d postgres -c "DROP DATABASE IF EXISTS $database;" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Unable to remove isolated E2E database $database."
    }
}

$process = $null
$applicationListenerPid = $null
try {
    # 3. Start application in the background
    Write-Host "Starting application with mock/local environment under background..."
    $logDirectory = Join-Path $PSScriptRoot "..\build\e2e"
    New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null
    $stdoutLog = Join-Path $logDirectory "application.stdout.log"
    $stderrLog = Join-Path $logDirectory "application.stderr.log"
    $process = Start-Process -FilePath ".\gradlew.bat" `
        -ArgumentList ":app:bootRun", "--no-daemon", "--console=plain" `
        -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog

    # 4. Wait for the app to become healthy
    Write-Host "Waiting for app to start up at $healthUrl..."

    $healthy = $false
    for ($i = 1; $i -le 90; $i++) {
        if ($process.HasExited) {
            Write-Host "--- application stderr ---"
            Get-Content $stderrLog -Tail 80 -ErrorAction SilentlyContinue
            Write-Host "--- application stdout ---"
            Get-Content $stdoutLog -Tail 120 -ErrorAction SilentlyContinue
            throw "Application process died during startup with exit code $($process.ExitCode)."
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
        Write-Host "--- application stderr ---"
        Get-Content $stderrLog -Tail 80 -ErrorAction SilentlyContinue
        throw "Application did not become healthy after 180 seconds."
    }

    $applicationListener = Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction Stop |
        Select-Object -First 1
    $applicationListenerPid = $applicationListener.OwningProcess

    # Flyway has now prepared the schema. Seed only the smallest reference
    # fixture required to exercise product publication and checkout.
    $fixture = Get-Content (Join-Path $PSScriptRoot "fixtures\e2e-prerequisites.sql") -Raw
    $fixture | docker compose -p aionn-modulith-backend `
        -f docker/docker-compose.yml --env-file envs/common.env `
        exec -T postgres psql -v ON_ERROR_STOP=1 `
        -U $env:POSTGRES_USER -d $env:POSTGRES_DB
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to apply E2E prerequisite fixture to PostgreSQL."
    }

    # 5. Run requested module E2E smoke tests and retain every result.
    $modules = @("identity", "catalog", "inventory", "ordering", "payment",
        "shipping", "promotion", "notification", "chat", "recommendation")
    $results = [System.Collections.Generic.List[object]]::new()
    foreach ($name in $modules) {
        if ($Module -ne "all" -and $Module -ne $name) {
            continue
        }

        Write-Host "Running E2E tests for $name module..."
        & bash "scripts/$name/test-$name-e2e.sh"
        $exitCode = $LASTEXITCODE
        $results.Add([pscustomobject]@{
            Module = $name
            Result = if ($exitCode -eq 0) { "PASS" } else { "FAIL" }
            ExitCode = $exitCode
        })
    }

    Write-Host "`nE2E module summary:"
    $results | Format-Table -AutoSize
    $failed = @($results | Where-Object ExitCode -ne 0)
    if ($failed.Count -gt 0) {
        throw "E2E suite failed for: $($failed.Module -join ', '). Logs: $logDirectory"
    }
}
finally {
    # 6. Ensure the app is stopped
    & $cleanup $process $applicationListenerPid $tempDatabase $env:POSTGRES_USER
}
