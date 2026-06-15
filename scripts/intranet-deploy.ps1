param(
    [ValidateSet("init", "start", "stop", "restart", "status", "logs", "backup", "update")]
    [string]$Action = "start",

    [string]$ServerIp = "",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$BackendDir = Join-Path $Root "backend"
$EnvFile = Join-Path $BackendDir ".env"
$EnvExample = Join-Path $BackendDir ".env.intranet.example"
$BackupRoot = Join-Path $Root "backups"

function Assert-Command($Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing command: $Name. Install it and try again."
    }
}

function Load-EnvFile($Path) {
    $result = @{}
    if (-not (Test-Path -LiteralPath $Path)) {
        return $result
    }
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }
        $parts = $line.Split("=", 2)
        $result[$parts[0].Trim()] = $parts[1].Trim()
    }
    return $result
}

function Initialize-Env {
    if (Test-Path -LiteralPath $EnvFile) {
        Write-Host "backend/.env already exists. Skip init."
        return
    }
    Copy-Item -LiteralPath $EnvExample -Destination $EnvFile
    if ($ServerIp) {
        $content = Get-Content -Raw -LiteralPath $EnvFile
        $content = $content.Replace("SERVER_IP=192.168.1.100", "SERVER_IP=$ServerIp")
        $content = $content.Replace("KKFILEVIEW_BASE_URL=http://192.168.1.100:8012", "KKFILEVIEW_BASE_URL=http://${ServerIp}:8012")
        Set-Content -LiteralPath $EnvFile -Value $content -Encoding UTF8
    }
    Write-Host "Created backend/.env. Edit DB_PASSWORD, MINIO_ROOT_PASSWORD, and JWT_SECRET before starting."
}

function Compose-Up {
    Assert-Command "docker"
    if (-not (Test-Path -LiteralPath $EnvFile)) {
        Initialize-Env
        throw "Edit backend/.env passwords and SERVER_IP, then run start again."
    }
    Push-Location $BackendDir
    try {
        if ($SkipBuild) {
            docker compose up -d
        } else {
            docker compose up -d --build
        }
    } finally {
        Pop-Location
    }
}

function Compose-Down {
    Assert-Command "docker"
    Push-Location $BackendDir
    try {
        docker compose down
    } finally {
        Pop-Location
    }
}

function Show-Status {
    Assert-Command "docker"
    Push-Location $BackendDir
    try {
        docker compose ps
    } finally {
        Pop-Location
    }
}

function Show-Logs {
    Assert-Command "docker"
    Push-Location $BackendDir
    try {
        docker compose logs --tail=200 backend frontend postgres redis minio kkfileview
    } finally {
        Pop-Location
    }
}

function Backup-Data {
    Assert-Command "docker"
    $envMap = Load-EnvFile $EnvFile
    $dbUser = if ($envMap.ContainsKey("DB_USERNAME")) { $envMap["DB_USERNAME"] } else { "edu" }
    $dbName = if ($envMap.ContainsKey("DB_NAME")) { $envMap["DB_NAME"] } else { "edu" }
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $backupDir = Join-Path $BackupRoot $stamp
    New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

    $dbBackup = Join-Path $backupDir "postgres-$dbName.sql"
    docker exec edu-postgres pg_dump -U $dbUser $dbName | Set-Content -LiteralPath $dbBackup -Encoding UTF8

    $minioVolume = docker inspect edu-minio --format '{{ range .Mounts }}{{ if eq .Destination "/data" }}{{ .Name }}{{ end }}{{ end }}'
    if (-not $minioVolume) {
        throw "Cannot detect edu-minio /data volume."
    }
    docker run --rm -v "${minioVolume}:/data:ro" -v "${backupDir}:/backup" alpine tar -cf /backup/minio-data.tar -C /data .

    Write-Host "Backup completed: $backupDir"
}

switch ($Action) {
    "init" { Initialize-Env }
    "start" { Compose-Up; Show-Status }
    "stop" { Compose-Down }
    "restart" { Compose-Down; Compose-Up; Show-Status }
    "status" { Show-Status }
    "logs" { Show-Logs }
    "backup" { Backup-Data }
    "update" { git pull; Compose-Up; Show-Status }
}
