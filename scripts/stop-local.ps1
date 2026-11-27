# 结束本机 6379 上的 redis-server，以及占用 8080 / 5173 的监听进程。
# 不停止 Windows 服务 MySQL80。

function Stop-Listener([int]$Port, [string]$Name) {
    $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (-not $conns) {
        Write-Host "$Name 未在 $Port 监听。"
        return
    }
    $pids = $conns | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($procId in $pids) {
        try {
            Stop-Process -Id $procId -Force -ErrorAction Stop
            Write-Host "已结束 $Name 进程 $procId （端口 $Port）。"
        } catch {
            Write-Host "无法结束 $Name 进程 $procId ：$($_.Exception.Message)"
        }
    }
}

Stop-Listener 5173 "Vite"
Stop-Listener 8080 "Spring Boot"
Stop-Listener 6379 "Redis"
Write-Host "MySQL 服务未改动。"
