@echo off
setlocal EnableDelayedExpansion

:: ============================================================
:: 一键数据库工具链：备份 → 重置 → 恢复数据 → Maven 重建
::
:: 用法：
::   reset-and-build.bat          完整链路（重置前自动备份业务数据，
::                                重置后自动导入，保留开发期间的业务数据）
::   reset-and-build.bat fresh    全新初始化（重置前仍会备份兜底，
::                                但不导入数据，用于迁移 checksum 冲突
::                                等需要 Flyway 全新建表的场景）
::
:: 备份策略（两段式导出，合并为单个备份文件）：
::   第一段：全库表结构（含运行时表，保证重置后所有表完整存在）
::   第二段：业务数据（排除运行时表数据，运行时表恢复为空表）
::   排除的运行时数据：登录日志/JWT黑名单/执行记录/测试结果/仓库拉取日志
::
:: 恢复后启动应用时，Flyway 依据备份中保留的迁移历史自动补执行
:: 新增的迁移，旧数据自动演进到最新表结构（新表新列自动生效）。
:: 备份文件位于 backend\scripts\backup\，按时间戳命名，保留最近 10 份。
:: 空库快照（无 CREATE TABLE）自动跳过导入，效果等同 fresh 模式。
:: ============================================================

set "MYSQL_EXE=D:\software\mysql-8.0\bin\mysql.exe"
set "DUMP_EXE=D:\software\mysql-8.0\bin\mysqldump.exe"
set "MYSQL_USER=root"
set "MYSQL_PASS=pp2024"
set "DB_NAME=auto_test_platform"
set "BACKUP_DIR=%~dp0backup"
set "KEEP_COUNT=10"
set "MODE=%~1"

:: 运行时数据表（仅排除其数据，表结构仍会备份）
set "IGNORES=--ignore-table=%DB_NAME%.login_log --ignore-table=%DB_NAME%.token_blacklist --ignore-table=%DB_NAME%.test_execution --ignore-table=%DB_NAME%.test_result --ignore-table=%DB_NAME%.code_repository_pull_log"

echo.
echo ============================================================
echo  [1/5] 备份数据库: %DB_NAME%
echo ============================================================
echo.

if not exist "%DUMP_EXE%" (
    echo [ERROR] 未找到 mysqldump: %DUMP_EXE%
    echo 请确认 MySQL 已安装于正确路径。
    pause
    exit /b 1
)

if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set "TS=%%i"
set "BACKUP_FILE=%BACKUP_DIR%\backup_%TS%.sql"

echo 备份文件: %BACKUP_FILE%
echo 排除运行时数据: login_log, token_blacklist, test_execution, test_result, code_repository_pull_log
echo.

:: 第一段：全库表结构
"%DUMP_EXE%" -u %MYSQL_USER% -p%MYSQL_PASS% --single-transaction --default-character-set=utf8mb4 --no-data --add-drop-table --databases %DB_NAME% --result-file="%BACKUP_FILE%" 2> "%TEMP%\dump_err.log"
if !errorlevel! neq 0 (
    echo [ERROR] 表结构备份失败，已中止，数据库未被修改。
    type "%TEMP%\dump_err.log"
    del "%TEMP%\dump_err.log" 2>nul
    del "%BACKUP_FILE%" 2>nul
    echo 请确认 MySQL 正在运行且账号密码正确。
    pause
    exit /b 1
)

:: 第二段：业务数据（追加到同一备份文件）
"%DUMP_EXE%" -u %MYSQL_USER% -p%MYSQL_PASS% --single-transaction --default-character-set=utf8mb4 --no-create-info %IGNORES% %DB_NAME% >> "%BACKUP_FILE%" 2> "%TEMP%\dump_err.log"
if !errorlevel! neq 0 (
    echo [ERROR] 业务数据备份失败，已中止，数据库未被修改。
    type "%TEMP%\dump_err.log"
    del "%TEMP%\dump_err.log" 2>nul
    del "%BACKUP_FILE%" 2>nul
    pause
    exit /b 1
)
del "%TEMP%\dump_err.log" 2>nul

:: 备份文件基本校验（文件未正常生成视为失败；空库快照约 600 字节，放行）
for %%f in ("%BACKUP_FILE%") do set "BK_SIZE=%%~zf"
if !BK_SIZE! LSS 200 (
    echo [ERROR] 备份文件未正常生成（!BK_SIZE! 字节），已中止，数据库未被修改。
    del "%BACKUP_FILE%" 2>nul
    pause
    exit /b 1
)

:: 清理旧备份，保留最近 %KEEP_COUNT% 份
for /f "skip=%KEEP_COUNT% delims=" %%f in ('dir /b /o-d "%BACKUP_DIR%\backup_*.sql" 2^>nul') do (
    del "%BACKUP_DIR%\%%f" 2>nul
)

echo [OK] 备份完成（!BK_SIZE! 字节）

echo.
echo ============================================================
echo  [2/5] 重置数据库: %DB_NAME%
echo ============================================================
echo.

if not exist "%MYSQL_EXE%" (
    echo [ERROR] MySQL 未找到: %MYSQL_EXE%
    echo 请确认 MySQL 已安装于正确路径。
    pause
    exit /b 1
)

:: 重置数据库
echo DROP DATABASE IF EXISTS %DB_NAME%;> "%TEMP%\reset_db.sql"
echo CREATE DATABASE %DB_NAME% DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;>> "%TEMP%\reset_db.sql"

"%MYSQL_EXE%" -u %MYSQL_USER% -p%MYSQL_PASS% < "%TEMP%\reset_db.sql" 2>nul
if !errorlevel! neq 0 (
    echo [ERROR] 数据库重置失败，请检查 MySQL 是否正在运行。
    echo 备份文件: %BACKUP_FILE%
    del "%TEMP%\reset_db.sql" 2>nul
    pause
    exit /b 1
)

del "%TEMP%\reset_db.sql" 2>nul
echo [OK] 数据库 %DB_NAME% 已重置

echo.
echo ============================================================
echo  [3/5] 导入备份数据
echo ============================================================
echo.

if /i "%MODE%"=="fresh" (
    echo [SKIP] fresh 模式：跳过数据导入，启动应用后由 Flyway 全新初始化
    goto SKIP_IMPORT
)

:: 空库快照（无任何表）自动跳过导入
findstr /c:"CREATE TABLE" "%BACKUP_FILE%" >nul 2>&1
if !errorlevel! neq 0 (
    echo [SKIP] 备份为空库快照，跳过数据导入
    goto SKIP_IMPORT
)

"%MYSQL_EXE%" -u %MYSQL_USER% -p%MYSQL_PASS% --default-character-set=utf8mb4 < "%BACKUP_FILE%" 2> "%TEMP%\import_err.log"
if !errorlevel! neq 0 (
    echo [ERROR] 备份数据导入失败，数据库当前为空库。
    type "%TEMP%\import_err.log"
    del "%TEMP%\import_err.log" 2>nul
    echo.
    echo 备份文件: %BACKUP_FILE%
    echo 可手动重试导入：
    echo   "%MYSQL_EXE%" -u %MYSQL_USER% -p%MYSQL_PASS% --default-character-set=utf8mb4 ^< "%BACKUP_FILE%"
    pause
    exit /b 1
)
del "%TEMP%\import_err.log" 2>nul
echo [OK] 备份数据已导入

:SKIP_IMPORT

echo.
echo ============================================================
echo  [4/5] 重新构建 Maven 项目
echo ============================================================
echo.

:: 定位 backend 目录（脚本所在目录的上级）
set "SCRIPT_DIR=%~dp0"
set "BACKEND_DIR=%SCRIPT_DIR%.."

pushd "%BACKEND_DIR%"
call mvn clean install -DskipTests -q
if !errorlevel! neq 0 (
    echo [ERROR] Maven 构建失败，请根据上方日志修复后重新构建。
    echo 数据库数据已恢复，不受影响。
    popd
    pause
    exit /b 1
)
popd
echo [OK] Maven 构建成功

echo.
echo ============================================================
echo  [5/5] 完成
echo ============================================================
echo.

if /i "%MODE%"=="fresh" (
    echo  数据库已重置为空库，Maven 已重建。
    echo  启动后端服务后，Flyway 将全新执行所有迁移。
) else (
    echo  数据库已重置并恢复备份数据，Maven 已重建。
    echo  启动后端服务后，Flyway 将自动补执行新增的迁移，
    echo  旧数据自动演进到最新表结构。
    echo  运行时表（日志/执行记录等）表结构保留、数据清空。
)
echo.
echo  备份目录: %BACKUP_DIR%，保留最近 %KEEP_COUNT% 份
echo.
pause
