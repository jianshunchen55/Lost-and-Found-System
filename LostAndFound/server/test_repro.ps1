$headers = @{
    "Content-Type" = "application/json"
}

$suffix = Get-Random
$username = "testuser$suffix"
$password = "password"

# Register
$registerBody = @{
    username = $username
    password = $password
    email = "$username@example.com"
} | ConvertTo-Json

Write-Host "Registering user $username..."
try {
    $regResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method Post -Body $registerBody -Headers $headers
    Write-Host "Registered."
} catch {
    Write-Host "Register failed: $_"
    # Try login anyway if user exists
}

# Login
$loginBody = @{
    username = $username
    password = $password
} | ConvertTo-Json

Write-Host "Logging in..."
try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -Headers $headers
    $token = $loginResponse.accessToken
    Write-Host "Logged in. Token: $token"
} catch {
    Write-Host "Login failed: $_"
    exit
}

$headers.Add("Authorization", "Bearer $token")

# Publish Lost Item
Write-Host "Publishing Lost Item..."
$lostItem = @{
    title = "My Lost Keys"
    category = "Daily"
    description = "Lost my keys"
    lostTime = "2023-10-01 10:00:00"
    contact = "123456"
} | ConvertTo-Json
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/lost/add" -Method Post -Body $lostItem -Headers $headers
} catch {
    Write-Host "Failed to add lost item: $_"
}

# Publish Found Item
Write-Host "Publishing Found Item..."
$foundItem = @{
    title = "Found Wallet"
    category = "Daily"
    description = "Found a wallet"
    foundTime = "2023-10-02 10:00:00"
    contact = "654321"
} | ConvertTo-Json
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/found/add" -Method Post -Body $foundItem -Headers $headers
} catch {
    Write-Host "Failed to add found item: $_"
}

# Verify
Write-Host "`n--- VERIFICATION ---"

# Get Lost Items
Write-Host "`nFetching My Lost Items (/api/my/lost)..."
try {
    $lostResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/my/lost" -Method Get -Headers $headers
    $lostItems = $lostResponse.records
    Write-Host "Lost Items Count: $($lostItems.Count)"
    $lostItems | ForEach-Object { Write-Host " - [$($_.id)] $($_.title)" }
} catch {
    Write-Host "Failed to fetch lost items: $_"
}

# Get Found Items
Write-Host "`nFetching My Found Items (/api/my/found)..."
try {
    $foundResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/my/found" -Method Get -Headers $headers
    $foundItems = $foundResponse.records
    Write-Host "Found Items Count: $($foundItems.Count)"
    $foundItems | ForEach-Object { Write-Host " - [$($_.id)] $($_.title)" }
} catch {
    Write-Host "Failed to fetch found items: $_"
}
