param(
    [switch]$WithAi
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$env:COMPOSE_PARALLEL_LIMIT = '1'

Push-Location $projectRoot
try {
    $composeArgs = if ($WithAi) { @('--profile', 'ai') } else { @() }
    & docker compose @composeArgs up -d --build
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    Write-Host 'Watching application source. Keep this terminal open; press Ctrl+C to stop watching.'
    & docker compose watch --no-up lifestyle-web travel-service property-service integration-service
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
