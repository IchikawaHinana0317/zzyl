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
Write-Host "开始部署中州养老后端"
Write-Host "镜像：$FullImageName"
Write-Host "容器：$ContainerName"
Write-Host "端口：${HostPort}:${ContainerPort}"
Write-Host "环境文件：$EnvFile"
Write-Host "========================================"

# 1. 检查 Docker
docker version

if ($LASTEXITCODE -ne 0) {
    throw "Docker 不可用，请确认 Docker Desktop 已启动"
}

# 2. 检查镜像是否已经由 Jenkins 构建成功
docker image inspect $FullImageName *> $null

if ($LASTEXITCODE -ne 0) {
    throw "未找到镜像：$FullImageName"
}

# 3. 检查环境变量文件
if (-not (Test-Path $EnvFile)) {
    throw "环境变量文件不存在：$EnvFile"
}

# 4. 判断同名旧容器是否存在
$ExistingContainer = docker ps -a `
    --filter "name=^/${ContainerName}$" `
    --format "{{.Names}}"

if ($ExistingContainer -eq $ContainerName) {

    # 判断旧容器是否正在运行
    $RunningContainer = docker ps `
        --filter "name=^/${ContainerName}$" `
        --format "{{.Names}}"

    if ($RunningContainer -eq $ContainerName) {
        Write-Host "停止旧容器：$ContainerName"

        docker stop $ContainerName

        if ($LASTEXITCODE -ne 0) {
            throw "停止旧容器失败：$ContainerName"
        }
    }

    Write-Host "删除旧容器：$ContainerName"

    docker rm $ContainerName

    if ($LASTEXITCODE -ne 0) {
        throw "删除旧容器失败：$ContainerName"
    }
}

# 5. 启动新容器
Write-Host "启动新容器：$ContainerName"

docker run -d `
    --name $ContainerName `
    --restart unless-stopped `
    --env-file $EnvFile `
    -p "${HostPort}:${ContainerPort}" `
    -v "zzyl-upload:/app/uploadPath" `
    -v "zzyl-logs:/app/logs" `
    $FullImageName

if ($LASTEXITCODE -ne 0) {
    throw "Docker 容器启动命令执行失败"
}

# 6. 等待 Spring Boot 启动
Write-Host "等待 Spring Boot 启动..."

$MaxAttempts = 30
$Started = $false

for ($Attempt = 1; $Attempt -le $MaxAttempts; $Attempt++) {

    Start-Sleep -Seconds 2

    $ContainerStatus = docker inspect `
        --format "{{.State.Status}}" `
        $ContainerName 2>$null

    if ($ContainerStatus -ne "running") {
        Write-Host "容器已停止，输出日志："
        docker logs --tail 200 $ContainerName

        throw "容器启动后异常退出"
    }

    # 测试宿主机9000端口是否已经可以建立TCP连接
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
        # Spring Boot可能仍在启动，继续等待
    }
    finally {
        $TcpClient.Close()
    }

    Write-Host "第 ${Attempt} 次检查：服务尚未就绪"
}

# 7. 输出状态和日志
Write-Host "当前容器："
docker ps --filter "name=^/${ContainerName}$"

Write-Host "容器最近100行日志："
docker logs --tail 100 $ContainerName

if (-not $Started) {
    throw "等待超时，Spring Boot 端口 $HostPort 尚未开放"
}

Write-Host "========================================"
Write-Host "部署成功"
Write-Host "后端访问地址：http://localhost:$HostPort"
Write-Host "========================================"