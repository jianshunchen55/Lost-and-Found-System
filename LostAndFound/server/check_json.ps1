$headers = @{
    "Content-Type" = "application/json"
}

# Login
$loginBody = @{
    username = "stu001"
    password = "password"
} | ConvertTo-Json

try {
    # Try login
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -Headers $headers
    $token = $loginResponse.accessToken
    Write-Host "Login successful. Token acquired."
} catch {
    Write-Host "Login failed: $_"
    # Fallback to the token from user prompt if login fails (it might expire but worth a shot)
    $token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzdHUwMDEiLCJleHAiOjE3NjcxNjkzMzYsImlhdCI6MTc2NzA4MjkzNn0.FKog1Vh6LFJ76i0maIPw03G-l9E6MMi6tUbReY-x9dE"
    Write-Host "Using fallback token."
}

$headers.Add("Authorization", "Bearer $token")

# Get Found Items - Raw JSON
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/my/found" -Method Get -Headers $headers
    Write-Host "Raw Content:"
    # Print first 500 chars to avoid flooding
    $content = $response.Content
    if ($content.Length -gt 1000) {
        Write-Host $content.Substring(0, 1000)
    } else {
        Write-Host $content
    }
} catch {
    Write-Host "Failed to fetch items: $_"
}
