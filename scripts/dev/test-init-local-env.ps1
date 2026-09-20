$ErrorActionPreference = "Stop"
$directory = Join-Path ([System.IO.Path]::GetTempPath()) ("aionn-env-test-" + [Guid]::NewGuid())
$initializer = Join-Path $PSScriptRoot "init-local-env.ps1"
$names = @("common", "identity", "catalog", "inventory", "ordering", "payment",
    "shipping", "promotion", "notification", "chat", "recommendation")
New-Item -ItemType Directory -Path $directory | Out-Null
try {
    & $initializer -Directory $directory
    if (@(Get-ChildItem -LiteralPath $directory -File).Count -ne 11) {
        throw "Expected exactly 11 generated files."
    }
    foreach ($name in $names) {
        $path = Join-Path $directory "$name.env"
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing $name.env" }
        if ($name -ne "common") {
            $assignments = @(Get-Content -LiteralPath $path | Where-Object {
                $_.Trim() -and -not $_.Trim().StartsWith("#")
            })
            if ($assignments.Count -ne 0) { throw "$name.env must contain comments only." }
        }
    }
    $common = Join-Path $directory "common.env"
    $template = Join-Path $PSScriptRoot "..\..\.env.example"
    if ((Get-FileHash -LiteralPath $common).Hash -ne (Get-FileHash -LiteralPath $template).Hash) {
        throw "common.env must preserve the tracked template exactly."
    }
    foreach ($name in $names) {
        [System.IO.File]::WriteAllText((Join-Path $directory "$name.env"), "# sentinel-$name`n")
    }
    & $initializer -Directory $directory
    foreach ($name in $names) {
        $actual = [System.IO.File]::ReadAllText((Join-Path $directory "$name.env"))
        if ($actual -cne "# sentinel-$name`n") { throw "Existing $name.env was overwritten." }
    }
    if (@(Get-ChildItem -LiteralPath $directory -File).Count -ne 11) {
        throw "Second invocation changed the file count."
    }
    Write-Host "PASS: all 11 files created; template preserved; second run preserved every sentinel."
}
finally {
    # Only remove the uniquely created test directory, never repository environment files.
    Remove-Item -LiteralPath $directory -Recurse -Force -Confirm:$false
}
