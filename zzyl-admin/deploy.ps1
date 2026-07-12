param(
    [Parameter(Mandatory = $true)]
    [string]$ImageTag,

    [string]$ImageName = "zzyl-admin",

    [string]$ContainerName = "zzyl-admin",

    [int]$HostPort = 9000,

    [int]$ContainerPort = 9000,

    [string]$EnvFile = "C:\ProgramData\Jenkins\secrets\zzyl.env"
)

$ErrorActionPreference = "Stop"

$FullImageName = "${ImageName}:${ImageTag}"

Write-Host "========================================"
Write-Host "Starting deployment"
Write-Host "Image: $FullImageName"
Write-Host "Container: $ContainerName"
Write-Host "Port mapping: ${HostPort}:${ContainerPort}"
Write-Host "Env file: $EnvFile"
Write-Host "========================================"

# Check Docker
docker version

if ($LASTEXITCODE -ne 0) {
    throw "Docker is unavailable. Make sure Docker Desktop is running."
}

# Check image
docker image inspect $FullImageName *> $null

if ($LASTEXITCODE -ne 0) {
    throw "Docker image not found: $FullImageName"
}

# Check env file
if (-not (Test-Path $EnvFile)) {
    throw "Environment file not found: $EnvFile"
}

# Check whether old container exists
$ExistingContainer = docker ps -a `
    --filter "name=^/${ContainerName}$" `
    --format "{{.Names}}"

if ($ExistingContainer -eq $ContainerName) {

    $RunningContainer = docker ps `
        --filter "name=^/${ContainerName}$" `
        --format "{{.Names}}"

    if ($RunningContainer -eq $ContainerName) {
        Write-Host "Stopping old container: $ContainerName"

        docker stop $ContainerName

        if ($LASTEXITCODE -ne 0) {
            throw "Failed to stop old container: $ContainerName"
        }
    }

    Write-Host "Removing old container: $ContainerName"

    docker rm $ContainerName

    if ($LASTEXITCODE -ne 0) {
        throw "Failed to remove old container: $ContainerName"
    }
}

# Start new container
Write-Host "Starting new container: $ContainerName"

docker run -d `
    --name $ContainerName `
    --restart unless-stopped `
    --env-file $EnvFile `
    -p "${HostPort}:${ContainerPort}" `
    -v "zzyl-upload:/app/uploadPath" `
    -v "zzyl-logs:/app/logs" `
    $FullImageName

if ($LASTEXITCODE -ne 0) {
    throw "Failed to execute docker run."
}

Write-Host "Waiting for Spring Boot to start..."

$MaxAttempts = 30
$Started = $false

for ($Attempt = 1; $Attempt -le $MaxAttempts; $Attempt++) {

    Start-Sleep -Seconds 2

    $ContainerStatus = docker inspect `
        --format "{{.State.Status}}" `
        $ContainerName 2>$null

    if ($ContainerStatus -ne "running") {
        Write-Host "Container stopped unexpectedly. Recent logs:"
        docker logs --tail 200 $ContainerName

        throw "Container exited during startup."
    }

    $TcpClient = New-Object System.Net.Sockets.TcpClient

    try {
        $AsyncResult = $TcpClient.BeginConnect(
            "127.0.0.1",
            $HostPort,
            $null,
            $null
        )

        $Connected = $AsyncResult.AsyncWaitHandle.WaitOne(
            1000,
            $false
        )

        if ($Connected -and $TcpClient.Connected) {
            $TcpClient.EndConnect($AsyncResult)
            $Started = $true
            break
        }
    }
    catch {
        # Application may still be starting
    }
    finally {
        $TcpClient.Close()
    }

    Write-Host "Health check attempt ${Attempt}: service is not ready yet."
}

Write-Host "Current container status:"
docker ps --filter "name=^/${ContainerName}$"

Write-Host "Recent container logs:"
docker logs --tail 100 $ContainerName

if (-not $Started) {
    throw "Startup timed out. Port $HostPort is not open."
}

Write-Host "========================================"
Write-Host "Deployment succeeded"
Write-Host "Backend URL: http://localhost:$HostPort"
Write-Host "========================================"