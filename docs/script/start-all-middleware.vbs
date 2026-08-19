Set fso = CreateObject("Scripting.FileSystemObject")
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)

' Use WMI Win32_Process.Create to launch PowerShell under WmiPrvSE.exe (system service).
' This detaches the entire process tree from the caller (IDE terminal or Explorer),
' preventing Job Object kill-on-close from terminating middleware services.
Set wmi = GetObject("winmgmts:\\.\root\cimv2:Win32_Process")
cmd = "powershell -NoProfile -ExecutionPolicy Bypass -File """ & scriptDir & "\start-all-middleware.ps1"""
wmi.Create cmd
