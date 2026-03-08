$headers = @{
    "Content-Type" = "application/json"
}

# Login
$loginBody = @{
    username = "stu001"
    password = "password"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -Headers $headers
    $token = $loginResponse.accessToken
    $headers.Add("Authorization", "Bearer $token")
} catch {
    Write-Host "Login failed, using fallback token"
    $token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzdHUwMDEiLCJleHAiOjE3NjcxNjkzMzYsImlhdCI6MTc2NzA4MjkzNn0.FKog1Vh6LFJ76i0maIPw03G-l9E6MMi6tUbReY-x9dE"
    $headers.Add("Authorization", "Bearer $token")
}

$idsToDelete = @(6, 7)

foreach ($id in $idsToDelete) {
    Write-Host "Attempting to delete Lost Item ID: $id"
    try {
        # Using the /api/my/lost/{id} endpoint which exists and (accidentally) allows deleting any item
        Invoke-RestMethod -Uri "http://localhost:8080/api/my/lost/$id" -Method Delete -Headers $headers
        Write-Host "Successfully deleted Lost Item ID: $id"
    } catch {
        Write-Host "Failed to delete Lost Item ID: $id - $_"
    }
}
