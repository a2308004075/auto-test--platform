@echo off
chcp 65001 >nul
echo ============================================
echo   停止 RabbitMQ
echo ============================================
echo.

set ERLANG_HOME=D:\software\erlang
D:\software\rabbitmq\sbin\rabbitmqctl.bat stop

echo.
echo RabbitMQ 已停止。
pause
