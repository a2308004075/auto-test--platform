@echo off
setlocal EnableDelayedExpansion
echo ============================================
echo   停止 Redis
echo ============================================
echo.

taskkill /F /IM redis-server.exe >nul 2>&1
if !errorlevel!==0 (
    echo Redis 已停止。
) else (
    echo Redis 未在运行。
)

pause
