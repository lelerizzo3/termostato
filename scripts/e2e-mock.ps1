param(
    [int]$Port = 8080,
    [switch]$DebugNtfy
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $projectRoot 'target\termostato.jar'
if (!(Test-Path $jar)) {
    throw "Jar non trovato: $jar. Eseguire prima 'mvn clean package'."
}

$runDirectory = Join-Path $env:TEMP ("termostato-e2e-" + [guid]::NewGuid().ToString())
New-Item -ItemType Directory -Path $runDirectory | Out-Null
$stdout = Join-Path $runDirectory 'stdout.log'
$stderr = Join-Path $runDirectory 'stderr.log'
$debugArgument = if ($DebugNtfy) { '--termostato.debug-mode=true' } else { '--termostato.debug-mode=false' }
$arguments = @(
    '-jar', $jar,
    '--spring.profiles.active=mock',
    "--server.port=$Port",
    "--termostato.config-file=$runDirectory\config.json",
    "--termostato.calendario-file=$runDirectory\calendario.json",
    "--termostato.database-path=$runDirectory\termostato.db",
    '--termostato.ntfy-url=https://ntfy.sh',
    '--termostato.ntfy-topic=sliverd',
    $debugArgument
)

$process = Start-Process -FilePath 'java' -ArgumentList $arguments -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
$baseUrl = "http://localhost:$Port"

function Get-Json($path) {
    return Invoke-RestMethod -Uri "$baseUrl$path" -Method Get
}

function Wait-For($description, [scriptblock]$condition, [int]$timeoutSeconds = 30) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    do {
        try {
            if (& $condition) {
                return
            }
        } catch {
            # Il server può essere ancora in fase di bootstrap.
        }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    throw "Timeout durante: $description"
}

try {
    Wait-For 'avvio applicazione e health UP' {
        try { (Get-Json '/actuator/health').status -eq 'UP' } catch { $false }
    }

    Invoke-RestMethod -Uri "$baseUrl/mock/reset" -Method Post | Out-Null
    $initial = Get-Json '/mock/state'
    if ($initial.temperatura -ne 19.0 -or $initial.relay_acceso) {
        throw "Stato mock iniziale inatteso: $($initial | ConvertTo-Json -Compress)"
    }

    Wait-For 'accensione relay sotto target' {
        (Get-Json '/mock/state').relay_acceso -eq $true
    }
    $heated = Get-Json '/mock/state'

    $newTemperature = @{ temperatura = 21.0 } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/mock/temperature" -Method Put `
        -ContentType 'application/json' -Body $newTemperature | Out-Null

    Wait-For 'spegnimento relay al raggiungimento target' {
        (Get-Json '/mock/state').relay_acceso -eq $false
    }
    $cooled = Get-Json '/mock/state'

    Wait-For 'registrazione log del ciclo spento' {
        $currentLogs = @(Get-Json '/log')
        (@($currentLogs | Where-Object { $_.caldaia_accesa -eq $false }).Count -gt 0)
    }
    $logs = @(Get-Json '/log')
    if (!($logs | Where-Object { $_.caldaia_accesa -eq $true })) {
        throw 'Il log non contiene un ciclo con caldaia accesa.'
    }
    if (!($logs | Where-Object { $_.caldaia_accesa -eq $false })) {
        throw 'Il log non contiene un ciclo con caldaia spenta.'
    }

    [pscustomobject]@{
        Esito = 'PASS'
        StatoIniziale = ($initial | ConvertTo-Json -Compress)
        StatoAcceso = ($heated | ConvertTo-Json -Compress)
        StatoSpento = ($cooled | ConvertTo-Json -Compress)
        RecordLog = $logs.Count
        Ntfy = 'https://ntfy.sh (client reale)'
        DatabaseCreato = Test-Path (Join-Path $runDirectory 'termostato.db')
    } | Format-List
} finally {
    if (!$process.HasExited) {
        Stop-Process -Id $process.Id -Force
    }
    Write-Host "Log E2E: $runDirectory"
    if (Test-Path $stderr) {
        $errors = Get-Content $stderr
        if ($errors) { $errors | Select-Object -Last 10 }
    }
    Remove-Item -Recurse -Force $runDirectory -ErrorAction SilentlyContinue
}
