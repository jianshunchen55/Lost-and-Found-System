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

Write-Host "Checking Lost Items..."
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/lost/list?page=1&size=100" -Method Get -Headers $headers
    $items = $response.records | Where-Object { $_.title -eq "My Lost Keys" }
    
    if ($items) {
        foreach ($item in $items) {
            Write-Host "Found Item to delete: ID=$($item.id), Title=$($item.title), ImageUrl=$($item.imageUrl)"
            
            # Delete it
            try {
                Invoke-RestMethod -Uri "http://localhost:8080/api/lost/$($item.id)" -Method Delete -Headers $headers
                Write-Host "Deleted Lost Item ID: $($item.id)"
            } catch {
                Write-Host "Failed to delete Lost Item ID: $($item.id) - $_"
            }
        }
    } else {
        Write-Host "No Lost Items found with title 'My Lost Keys'"
    }
} catch {
    Write-Host "Error fetching lost items: $_"
}

Write-Host "Checking Found Items..."
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/found/list?page=1&size=100" -Method Get -Headers $headers
    $items = $response.records | Where-Object { $_.title -eq "My Lost Keys" }
    
    if ($items) {
         foreach ($item in $items) {
            Write-Host "Found Item to delete: ID=$($item.id), Title=$($item.title), ImageUrl=$($item.imageUrl)"
             # Delete it
            try {
                Invoke-RestMethod -Uri "http://localhost:8080/api/found/$($item.id)" -Method Delete -Headers $headers
                Write-Host "Deleted Found Item ID: $($item.id)"
            } catch {
                Write-Host "Failed to delete Found Item ID: $($item.id) - $_"
            }
        }
    } else {
        Write-Host "No Found Items found with title 'My Lost Keys'"
    }
} catch {
    Write-Host "Error fetching found items: $_"
}
