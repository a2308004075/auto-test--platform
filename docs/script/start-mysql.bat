@echo off
chcp 65001 >nul
title MySQL 8.0
echo ============================================
echo   启动 MySQL 8.0
echo ============================================
echo.

tasklist /FI "IMAGENAME eq mysqld.exe" 2>nul | find /I "mysqld.exe" >nul
if "%errorlevel%"=="0" (
    echo   MySQL 已经在运行，跳过启动。
    echo.
    pause
    exit /b 0
)

D:\software\mysql-8.0\bin\mysqld --console

pause
