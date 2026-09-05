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
$headers = @{ 'X-API-Key' = 'e2e-test-key' }

function Get-Json($path) {
    return Invoke-RestMethod -Uri "$baseUrl$path" -Method Get -Headers $headers
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

    $invalidStatus = (& curl.exe -sS -o NUL -w '%{http_code}' `
        -H 'X-API-Key: invalid-e2e-key' "$baseUrl/actuator/health").Trim()
    if ($invalidStatus -ne '401') {
        throw "Una API-key non autorizzata ha restituito HTTP $invalidStatus invece di 401."
    }

    Invoke-RestMethod -Uri "$baseUrl/mock/reset" -Method Post -Headers $headers | Out-Null
    $initial = Get-Json '/mock/state'
    $initialCurrent = Get-Json '/stato'
    if ($initial.temperatura -ne 19.0 -or $initial.umidita -ne 50.0 -or $initial.relay_acceso) {
        throw "Stato mock iniziale inatteso: $($initial | ConvertTo-Json -Compress)"
    }
    if ($initialCurrent.temperatura -ne 19.0 -or $initialCurrent.umidita -ne 50.0 `
        -or $initialCurrent.temperatura_target -ne 20.5 `
        -or $null -eq $initialCurrent.temperatura_esterna `
        -or $null -eq $initialCurrent.umidita_esterna) {
        throw "Stato corrente iniziale inatteso: $($initialCurrent | ConvertTo-Json -Compress)"
    }

    Wait-For 'accensione relay sotto target' {
        (Get-Json '/mock/state').relay_acceso -eq $true
    }
    $heated = Get-Json '/mock/state'
    $heatedCurrent = Get-Json '/stato'
    if (!$heatedCurrent.relay_acceso -or $heatedCurrent.umidita -ne 50.0 `
        -or $heatedCurrent.temperatura_target -ne 20.5 `
        -or $null -eq $heatedCurrent.umidita_esterna) {
        throw "Stato corrente acceso inatteso: $($heatedCurrent | ConvertTo-Json -Compress)"
    }

    $newTemperature = @{ temperatura = 21.0; umidita = 50.0 } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/mock/temperature" -Method Put `
        -Headers $headers -ContentType 'application/json' -Body $newTemperature | Out-Null

    Wait-For 'spegnimento relay al raggiungimento target' {
        (Get-Json '/mock/state').relay_acceso -eq $false
    }
    $cooled = Get-Json '/mock/state'
    $cooledCurrent = Get-Json '/stato'
    if ($cooledCurrent.temperatura -ne 21.0 -or $cooledCurrent.umidita -ne 50.0 `
        -or $cooledCurrent.relay_acceso -or $cooledCurrent.temperatura_target -ne 20.5 `
        -or $null -eq $cooledCurrent.temperatura_esterna `
        -or $null -eq $cooledCurrent.umidita_esterna) {
        throw "Stato corrente spento inatteso: $($cooledCurrent | ConvertTo-Json -Compress)"
    }

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
        ApiKeyValida = 'e2e-test-key'
        ApiKeyNonValidaHttp = $invalidStatus
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
