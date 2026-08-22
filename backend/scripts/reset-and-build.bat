@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion

:: ============================================================
:: 一键重置数据库 + 清理构建
:: 用于修复 Flyway 迁移冲突、checksum 不匹配等问题
:: ============================================================

set "MYSQL_EXE=D:\software\mysql-8.0\bin\mysql.exe"
set "MYSQL_USER=root"
set "MYSQL_PASS=pp2024"
set "DB_NAME=auto_test_platform"

echo.
echo ============================================================
echo  [1/3] 重置数据库: %DB_NAME%
echo ============================================================
echo.

:: 检查 MySQL 是否可用
if not exist "%MYSQL_EXE%" (
    echo [ERROR] MySQL 未找到: %MYSQL_EXE%
    echo 请确认 MySQL 已安装并配置正确路径。
    pause
    exit /b 1
)

:: 重置数据库
echo DROP DATABASE IF EXISTS %DB_NAME%;> "%TEMP%\reset_db.sql"
echo CREATE DATABASE %DB_NAME% DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;>> "%TEMP%\reset_db.sql"

"%MYSQL_EXE%" -u %MYSQL_USER% -p%MYSQL_PASS% < "%TEMP%\reset_db.sql" 2>nul
if !errorlevel! neq 0 (
    echo [ERROR] 数据库重置失败，请检查 MySQL 是否正在运行。
    del "%TEMP%\reset_db.sql" 2>nul
    pause
    exit /b 1
)

del "%TEMP%\reset_db.sql" 2>nul
echo [OK] 数据库 %DB_NAME% 已重置

echo.
echo ============================================================
echo  [2/3] 清理并重建 Maven 项目
echo ============================================================
echo.

:: 定位 backend 目录（脚本所在目录的上级）
set "SCRIPT_DIR=%~dp0"
set "BACKEND_DIR=%SCRIPT_DIR%.."

pushd "%BACKEND_DIR%"
call mvn clean install -DskipTests -q
if !errorlevel! neq 0 (
    echo [ERROR] Maven 构建失败，请检查编译错误。
    popd
    pause
    exit /b 1
)
popd
echo [OK] Maven 构建成功

echo.
echo ============================================================
echo  [3/3] 完成
echo ============================================================
echo.
echo  数据库已重置，Maven 已重建。
echo  现在可以启动后端服务，Flyway 将从零执行全部迁移。
echo.
pause
