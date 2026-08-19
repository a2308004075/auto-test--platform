@echo off
title RabbitMQ
echo ============================================
echo   启动 RabbitMQ
echo ============================================
echo.

set ERLANG_HOME=D:\software\erlang
call D:\software\rabbitmq\sbin\rabbitmq-plugins.bat enable rabbitmq_management
call D:\software\rabbitmq\sbin\rabbitmq-server.bat

pause
