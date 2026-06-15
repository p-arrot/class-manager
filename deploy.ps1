param(
    [ValidateSet("init", "start", "stop", "restart", "status", "logs", "backup", "update")]
    [string]$Action = "start",

    [string]$ServerIp = "",
    [switch]$SkipBuild
)

$script = Join-Path $PSScriptRoot "scripts\intranet-deploy.ps1"
& $script -Action $Action -ServerIp $ServerIp -SkipBuild:$SkipBuild
