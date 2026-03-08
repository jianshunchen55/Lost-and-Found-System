$headers = @{
    "Content-Type" = "application/json"
}

# Login to get token
$loginBody = @{
    username = "student"
    password = "password"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -Headers $headers
    $token = $loginResponse.accessToken
    Write-Host "Logged in. Token: $token"
} catch {
    Write-Host "Login failed: $_"
    exit
}

$headers.Add("Authorization", "Bearer $token")

# Get Lost Items
Write-Host "`nFetching My Lost Items..."
try {
    $lostResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/my/lost" -Method Get -Headers $headers
    $lostItems = $lostResponse.records
    Write-Host "Lost Items Count: $($lostItems.Count)"
    $lostItems | ForEach-Object { Write-Host " - [$($_.id)] $($_.title)" }
} catch {
    Write-Host "Failed to fetch lost items: $_"
}

# Get Found Items
Write-Host "`nFetching My Found Items..."
try {
    $foundResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/my/found" -Method Get -Headers $headers
    $foundItems = $foundResponse.records
    Write-Host "Found Items Count: $($foundItems.Count)"
    $foundItems | ForEach-Object { Write-Host " - [$($_.id)] $($_.title)" }
} catch {
    Write-Host "Failed to fetch found items: $_"
}
