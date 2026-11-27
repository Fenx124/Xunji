# 启动本机 Redis、Spring Boot 与 Vite。不打印 .env 中的口令。
# 用法（在仓库根目录）：powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

function Read-DotEnv([string]$Path) {
    $map = @{}
    if (-not (Test-Path $Path)) {
        return $map
    }
    foreach ($line in Get-Content -Path $Path -Encoding UTF8) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) { continue }
        $eq = $trimmed.IndexOf("=")
        if ($eq -lt 1) { continue }
        $map[$trimmed.Substring(0, $eq).Trim()] = $trimmed.Substring($eq + 1).Trim()
    }
    return $map
}

function Test-Port([int]$Port) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $iar = $client.BeginConnect("127.0.0.1", $Port, $null, $null)
        $ok = $iar.AsyncWaitHandle.WaitOne(400, $false)
        if (-not $ok) { return $false }
        $client.EndConnect($iar) | Out-Null
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

$envFile = Join-Path $Root ".env"
if (-not (Test-Path $envFile)) {
    Write-Host "缺少 $envFile 。请先复制 .env.example 为 .env 并填写 MYSQL_ROOT_PASSWORD。"
    exit 1
}
$cfg = Read-DotEnv $envFile
$redisPort = 6379
if ($cfg.ContainsKey("REDIS_PORT") -and $cfg["REDIS_PORT"]) { $redisPort = [int]$cfg["REDIS_PORT"] }
$redisPassword = $cfg["REDIS_PASSWORD"]
if (-not $redisPassword) { $redisPassword = "smartstudy_redis_123" }

if (-not (Test-Port 3306)) {
    Write-Host "MySQL 未在 3306 监听。请先启动本机 MySQL80，并已导入 sql/schema.sql。"
    exit 1
}

$redisHome = "D:\Redis\Redis-x64-5.0.14.1"
$redisExe = Join-Path $redisHome "redis-server.exe"
$redisConf = Join-Path $redisHome "redis.windows.conf"
if (Test-Port $redisPort) {
    Write-Host "Redis 已在端口 $redisPort 监听，跳过启动。"
} elseif (-not (Test-Path $redisExe)) {
    Write-Host "未找到 $redisExe ，且 $redisPort 没有服务。请安装 Redis 或改脚本中的路径。"
    exit 1
} else {
    Write-Host "启动 Redis ..."
    Start-Process -FilePath $redisExe -ArgumentList @($redisConf, "--requirepass", $redisPassword, "--bind", "127.0.0.1", "--port", "$redisPort") -WorkingDirectory $redisHome -WindowStyle Minimized
    $ready = $false
    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Milliseconds 300
        if (Test-Port $redisPort) { $ready = $true; break }
    }
    if (-not $ready) {
        Write-Host "Redis 未能在预期时间内监听 $redisPort 。"
        exit 1
    }
}

if (Test-Port 8080) {
    Write-Host "8080 已被占用，跳过后端启动。"
} else {
    Write-Host "启动 Spring Boot（新窗口，首次会下载依赖）..."
    Start-Process -FilePath "mvn" -ArgumentList @("-f", "backend/pom.xml", "spring-boot:run") -WorkingDirectory $Root -WindowStyle Normal
}

if (Test-Port 5173) {
    Write-Host "5173 已被占用，跳过前端启动。"
} else {
    Write-Host "启动 Vite（新窗口）..."
    Start-Process -FilePath "npm" -ArgumentList @("run", "dev") -WorkingDirectory (Join-Path $Root "frontend") -WindowStyle Normal
}

Write-Host ""
Write-Host "已发起启动。后端就绪后打开 http://127.0.0.1:5173"
Write-Host "默认管理员见 .env 的 INIT_ADMIN_USERNAME / INIT_ADMIN_PASSWORD。"
Write-Host "停止：关闭两个新窗口；Redis 可用 scripts/stop-local.ps1 结束本脚本拉起的 redis-server。"
