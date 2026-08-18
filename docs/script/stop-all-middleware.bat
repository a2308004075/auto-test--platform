@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion
echo ============================================
echo   一键停止所有中间件
echo   停止顺序：RabbitMQ -> Redis -> MySQL
echo ============================================
echo.

echo [1/3] 停止 RabbitMQ ...
set ERLANG_HOME=D:\software\erlang
D:\software\rabbitmq\sbin\rabbitmqctl.bat stop >nul 2>&1
if !errorlevel!==0 (
    echo       RabbitMQ 已停止。
) else (
    echo       RabbitMQ 未在运行或停止失败，尝试强制终止 ...
    taskkill /F /IM erl.exe >nul 2>&1
    taskkill /F /IM epmd.exe >nul 2>&1
)
timeout /t 2 /nobreak >nul

echo [2/3] 停止 Redis ...
taskkill /F /IM redis-server.exe >nul 2>&1
if !errorlevel!==0 (
    echo       Redis 已停止。
) else (
    echo       Redis 未在运行。
)
timeout /t 1 /nobreak >nul

echo [3/3] 停止 MySQL ...
D:\software\mysql-8.0\bin\mysqladmin -uroot -ppp2024 shutdown >nul 2>&1
if !errorlevel!==0 (
    echo       MySQL 已停止。
) else (
    echo       MySQL 未在运行或停止失败，尝试强制终止 ...
    taskkill /F /IM mysqld.exe >nul 2>&1
)

echo.
echo ============================================
echo   所有中间件已停止！
echo ============================================
echo.
pause
