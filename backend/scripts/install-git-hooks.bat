@echo off
chcp 65001 >nul 2>&1
:: ============================================================
:: 安装 Git hooks（团队成员 clone 后运行一次即可）
:: ============================================================

set "SCRIPT_DIR=%~dp0"
set "HOOKS_SRC=%SCRIPT_DIR%hooks"
set "HOOKS_DST=%SCRIPT_DIR%..\..\.git\hooks"

if not exist "%HOOKS_DST%" (
    echo [ERROR] .git/hooks 目录不存在，请确认你在 Git 仓库根目录下。
    pause
    exit /b 1
)

:: 安装 pre-commit hook
if exist "%HOOKS_SRC%\pre-commit" (
    copy /y "%HOOKS_SRC%\pre-commit" "%HOOKS_DST%\pre-commit" >nul
    echo [OK] pre-commit hook 已安装
)

echo.
echo  Git hooks 安装完成！
echo  此后每次 git commit 都会自动检查：
echo    1. 已存在迁移文件是否被修改（阻断）
echo    2. 实体类变更是否缺少迁移文件（提醒）
echo    3. 新增迁移文件提示（信息）
echo.
pause
