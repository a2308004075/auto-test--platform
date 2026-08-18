@echo off
chcp 65001 >nul
title RabbitMQ
echo ============================================
echo   启动 RabbitMQ
echo ============================================
echo.

set ERLANG_HOME=D:\software\erlang
D:\software\rabbitmq\sbin\rabbitmq-server.bat

pause
