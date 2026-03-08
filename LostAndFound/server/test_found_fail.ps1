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

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method Post -Body $registerBody -Headers $headers -ErrorAction SilentlyContinue

# Login
$loginBody = @{
    username = $username
    password = $password
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -Headers $headers
$token = $loginResponse.accessToken
$headers.Add("Authorization", "Bearer $token")

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
    Write-Host "Error: $($_.Exception.Message)"
    $reader = New-Object System.IO.StreamReader $_.Exception.Response.GetResponseStream()
    $responseBody = $reader.ReadToEnd()
    Write-Host "Response Body: $responseBody"
}
