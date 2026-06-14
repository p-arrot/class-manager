param(
    [string]$Test = "",
    [string]$MavenArgs = "test"
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$backendDir = Join-Path $repoRoot "backend"
$volumeName = "class-manager-maven-repo"
$existingVolume = docker volume ls --quiet --filter "name=^$volumeName$"
if (-not $existingVolume) {
    docker volume create $volumeName | Out-Null
}

$dockerArgs = @(
    "run",
    "--rm",
    "-v",
    "${backendDir}:/workspace",
    "-v",
    "${volumeName}:/root/.m2",
    "-w",
    "/workspace",
    "maven:3.9.9-eclipse-temurin-21",
    "mvn"
)

if ($Test) {
    $dockerArgs += "-Dtest=$Test"
}

$dockerArgs += $MavenArgs.Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries)

docker @dockerArgs
exit $LASTEXITCODE
