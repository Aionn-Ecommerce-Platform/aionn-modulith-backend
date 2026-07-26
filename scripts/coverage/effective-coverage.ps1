param([string]$Module = 'promotion')

$xmlPath = "modules\$Module\build\reports\jacoco\test\jacocoTestReport.xml"
[xml]$doc = Get-Content $xmlPath

$excludeDirs = @(
    'application/dto', 'application/mapper', 'application/port', 'application/usecase',
    'adapter/rest/dto', 'adapter/rest/mapper', 'adapter/rest/support', 'adapter/rest/exception',
    'adapter/rest/config', 'domain/event', 'domain/valueobject',
    'infrastructure/config', 'infrastructure/persistence/entity',
    'infrastructure/persistence/mapper', 'infrastructure/persistence/repository',
    'infrastructure/persistence/adapter', 'infrastructure/observability',
    'infrastructure/integration', 'infrastructure/gateway', 'infrastructure/scheduling',
    'infrastructure/listener'
)

$lineMissed = 0; $lineCovered = 0; $branchMissed = 0; $branchCovered = 0
$uncovered = @()

foreach ($pkg in $doc.report.package) {
    $pkgName = $pkg.name -replace '\\', '/'
    $skip = $false
    foreach ($d in $excludeDirs) {
        if ($pkgName -like "*$d" -or $pkgName -like "*$d/*") { $skip = $true; break }
    }
    if ($skip) { continue }
    foreach ($sf in $pkg.sourcefile) {
        if ($sf.name -like '*MapperImpl.java') { continue }
        $lm = 0; $lc = 0; $bm = 0; $bc = 0
        foreach ($c in $sf.counter) {
            switch ($c.type) {
                'LINE' { $lm = [int]$c.missed; $lc = [int]$c.covered }
                'BRANCH' { $bm = [int]$c.missed; $bc = [int]$c.covered }
            }
        }
        $lineMissed += $lm; $lineCovered += $lc
        $branchMissed += $bm; $branchCovered += $bc
        if ($lm -gt 0) { $uncovered += [pscustomobject]@{ File = "$pkgName/$($sf.name)"; Missed = $lm; Covered = $lc } }
    }
}

$lineTotal = $lineMissed + $lineCovered
$branchTotal = $branchMissed + $branchCovered
Write-Output ("Module {0}: effective LINE {1:P1} ({2}/{3}), BRANCH {4:P1} ({5}/{6})" -f `
        $Module, ($lineCovered / [math]::Max($lineTotal, 1)), $lineCovered, $lineTotal, `
    ($branchCovered / [math]::Max($branchTotal, 1)), $branchCovered, $branchTotal)
$uncovered | Sort-Object Missed -Descending | Select-Object -First 20 | Format-Table -AutoSize
