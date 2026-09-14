@echo off
setlocal EnableDelayedExpansion

:: ============================================================
:: Verify all Flyway migrations on a TEMPORARY database.
:: The dev database is NEVER touched by this script.
::
:: Usage:
::   verify-migrations.bat
::
:: What it does:
::   1. Drops and re-creates the temp database:
::      auto_test_platform_verify
::   2. Runs every V*.sql migration file in numeric version
::      order (simulates Flyway on an empty database)
::   3. On success: drops the temp database and reports PASS
::   4. On failure: KEEPS the temp database for inspection
::      and reports the failing file
::
:: Use this before pushing new migrations.
:: The dev database (auto_test_platform) and its data are
:: not modified in any way.
:: ============================================================

set "MYSQL_EXE=D:\software\mysql-8.0\bin\mysql.exe"
set "MYSQL_USER=root"
set "MYSQL_PASS=pp2024"
set "DEV_DB=auto_test_platform"
set "VERIFY_DB=auto_test_platform_verify"
set "MIGRATION_DIR=%~dp0..\platform-server\src\main\resources\db\migration"

if not exist "%MYSQL_EXE%" (
    echo [ERROR] mysql.exe not found: %MYSQL_EXE%
    echo Make sure MySQL is installed at the configured path.
    pause
    exit /b 1
)

if not exist "%MIGRATION_DIR%" (
    echo [ERROR] migration dir not found: %MIGRATION_DIR%
    pause
    exit /b 1
)

echo.
echo ============================================================
echo  Verify all Flyway migrations on temp db: %VERIFY_DB%
echo  Dev database %DEV_DB% is NOT touched.
echo ============================================================
echo.

:: ---- 1. Re-create the temp database ----

echo DROP DATABASE IF EXISTS %VERIFY_DB%;> "%TEMP%\verify_db.sql"
echo CREATE DATABASE %VERIFY_DB% DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;>> "%TEMP%\verify_db.sql"

"%MYSQL_EXE%" -u %MYSQL_USER% -p%MYSQL_PASS% < "%TEMP%\verify_db.sql" 2> "%TEMP%\verify_err.log"
if !errorlevel! neq 0 (
    echo [ERROR] Failed to create temp db. Is MySQL running?
    type "%TEMP%\verify_err.log"
    del "%TEMP%\verify_err.log" 2>nul
    del "%TEMP%\verify_db.sql" 2>nul
    pause
    exit /b 1
)
del "%TEMP%\verify_err.log" 2>nul
del "%TEMP%\verify_db.sql" 2>nul
echo [OK] Temp db created.
echo.

:: ---- 2. Run every migration in numeric version order ----
:: dir /o:n would sort V10 before V2 (lexicographic), so use
:: PowerShell to sort by the numeric part of the file name.

set /a TOTAL=0

for /f "usebackq delims=" %%f in (`powershell -NoProfile -Command "Get-ChildItem -LiteralPath '%MIGRATION_DIR%' -Filter 'V*.sql' | Where-Object { $_.Name -match '^V(\d+)__' } | Sort-Object { [int]($_.Name -replace '^V(\d+)__.*','$1') } | ForEach-Object { $_.Name }"`) do (
    set /a TOTAL+=1
    echo [RUN ] %%f
    "%MYSQL_EXE%" -u %MYSQL_USER% -p%MYSQL_PASS% --default-character-set=utf8mb4 %VERIFY_DB% < "%MIGRATION_DIR%\%%f" 2> "%TEMP%\mig_err.log"
    if !errorlevel! neq 0 (
        echo [FAIL] %%f
        echo.
        type "%TEMP%\mig_err.log"
        del "%TEMP%\mig_err.log" 2>nul
        goto :RESULT_FAIL
    )
)
del "%TEMP%\mig_err.log" 2>nul

if !TOTAL! EQU 0 (
    echo [ERROR] No migration files found in %MIGRATION_DIR%
    goto :RESULT_FAIL
)

:: ---- 3. Success: drop the temp database ----

echo DROP DATABASE IF EXISTS %VERIFY_DB%;> "%TEMP%\verify_db.sql"
"%MYSQL_EXE%" -u %MYSQL_USER% -p%MYSQL_PASS% < "%TEMP%\verify_db.sql" 2>nul
del "%TEMP%\verify_db.sql" 2>nul

echo.
echo ============================================================
echo  [PASS] All !TOTAL! migrations executed successfully.
echo  Temp db %VERIFY_DB% has been dropped.
echo ============================================================
echo.
pause
exit /b 0

:RESULT_FAIL
echo.
echo ============================================================
echo  [FAIL] Verification stopped at the file above.
echo  Temp db %VERIFY_DB% is KEPT for inspection.
echo  Drop it manually after debugging:
echo    "%MYSQL_EXE%" -u %MYSQL_USER% -p%MYSQL_PASS% -e "DROP DATABASE %VERIFY_DB%;"
echo ============================================================
echo.
pause
exit /b 1
