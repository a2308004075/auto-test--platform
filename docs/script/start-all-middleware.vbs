Set WshShell = CreateObject("WScript.Shell")
scriptDir = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)
WshShell.CurrentDirectory = scriptDir
' 第三个参数 True 表示等待 PowerShell 执行完 ps1 再退出，
' 确保 Start-Process 已发出中间件启动命令，避免 wscript 过早退出导致子进程被清理
WshShell.Run "powershell -ExecutionPolicy Bypass -NoProfile -File """ & scriptDir & "\start-all-middleware.ps1"""", 0, True
Set WshShell = Nothing
