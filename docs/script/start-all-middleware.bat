@echo off
chcp 65001 >nul
echo ============================================
echo   一键启动所有中间件
echo   启动顺序：MySQL -> Redis -> RabbitMQ
echo ============================================
echo.

echo [1/3] 启动 MySQL ...
start "MySQL" cmd /k "D:\software\mysql-8.0\bin\mysqld --console"

echo 等待 MySQL 就绪 ...
ping -n 6 127.0.0.1 >nul

echo [2/3] 启动 Redis ...
start "Redis" cmd /k "D:\software\redis\redis-server.exe"

echo 等待 Redis 就绪 ...
ping -n 4 127.0.0.1 >nul

echo [3/3] 启动 RabbitMQ ...
start "RabbitMQ" cmd /k "call D:\develop\auto-test-platform\docs\script\start-rabbitmq.bat"

echo.
echo ============================================
echo   所有中间件启动命令已发送！
echo   MySQL        : localhost:3306
echo   Redis        : localhost:6379
echo   RabbitMQ UI  : http://localhost:15672
echo ============================================
echo.
pause
