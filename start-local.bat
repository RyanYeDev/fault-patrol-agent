@echo off
chcp 65001 >nul
rem ============================================================
rem 故障巡检 Agent 平台 - Windows/WSL 一键启动脚本
rem 用法：双击运行；保持本窗口打开即保持服务运行
rem 说明：WSL2 在最后一个会话结束后会自动关闭虚拟机，
rem        因此本脚本启动服务后会停留在等待状态，让服务持续可用。
rem        停止服务：关闭本窗口后执行 docker compose down，或重启电脑。
rem ============================================================

echo [1/2] 启动故障巡检 Agent 平台（docker compose up -d）...
wsl -d Ubuntu -- bash -c "cd /mnt/e/code/fault-patrol-agent && docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d"

echo.
echo [2/2] 等待服务就绪...
wsl -d Ubuntu -- bash -c "cd /mnt/e/code/fault-patrol-agent && for i in $(seq 1 60); do if docker logs fault-patrol-agent-app 2>&1 | grep -q '自动装配完成'; then echo READY; exit 0; fi; sleep 5; done; echo TIMEOUT; exit 1"

echo.
echo ============================================================
echo  服务已启动：
echo    首页/诊断演示:  http://localhost:8091
echo    巡检工具 MCP:   http://localhost:8092/sse
echo    RabbitMQ 控制台: http://localhost:15672  (admin/admin123)
echo    Prometheus:     http://localhost:9090
echo    Jaeger UI:      http://localhost:16686
echo.
echo  保持本窗口打开，服务持续运行。按 Ctrl+C 或关闭窗口后
echo  WSL 会在片刻后自动休眠（服务停止）。
echo ============================================================
echo.

rem 保持 WSL 会话活跃，防止虚拟机自动关闭
wsl -d Ubuntu -- bash -c "echo 保持 WSL 运行中...; exec tail -f /dev/null"
