param(
    [string]$Directory = (Join-Path $PSScriptRoot "..\..\envs")
)

$ErrorActionPreference = "Stop"
$template = Join-Path $PSScriptRoot "..\..\.env.example"
# Only the tracked template is read; existing local files are never opened or overwritten.
$templateBytes = [System.IO.File]::ReadAllBytes($template)
New-Item -ItemType Directory -Path $Directory -Force | Out-Null
$names = @("common", "identity", "catalog", "inventory", "ordering", "payment",
    "shipping", "promotion", "notification", "chat", "recommendation")
foreach ($name in $names) {
    $path = Join-Path $Directory "$name.env"
    if (Test-Path -LiteralPath $path) { continue }
    $bytes = $templateBytes
    if ($name -ne "common") {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes(
            "# Optional $name overrides; shared values are in common.env.`n" +
            "# Unset options use application YAML defaults. Add KEY=value overrides only as needed.`n")
    }
    # CreateNew also refuses to overwrite a file created after the existence check.
    $stream = [System.IO.File]::Open($path, [System.IO.FileMode]::CreateNew,
        [System.IO.FileAccess]::Write, [System.IO.FileShare]::None)
    try { $stream.Write($bytes, 0, $bytes.Length) }
    finally { $stream.Dispose() }
    Write-Host "Created $path"
}
Write-Host "Review local placeholders in common.env before starting infrastructure or E2E tests."
