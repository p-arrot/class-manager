param(
    [string]$BaseUrl = "http://localhost"
)

$ErrorActionPreference = "Stop"

function Test-Endpoint($Name, $Url) {
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 15
        Write-Host "[OK] $Name $($response.StatusCode) $Url"
    } catch {
        Write-Host "[FAIL] $Name $Url"
        throw
    }
}

Test-Endpoint "Frontend" $BaseUrl
Test-Endpoint "Backend health" "$BaseUrl/api/health"
Write-Host "基础健康检查通过。"
