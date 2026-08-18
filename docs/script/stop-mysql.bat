@echo off
chcp 65001 >nul
echo ============================================
echo   停止 MySQL 8.0
echo ============================================
echo.

D:\software\mysql-8.0\bin\mysqladmin -uroot -ppp2024 shutdown

echo.
echo MySQL 已停止。
pause
