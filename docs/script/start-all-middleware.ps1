# Silent startup of all middleware (MySQL -> Redis -> RabbitMQ)
# Called by start-all-middleware.vbs (via WMI for process isolation)
# Uses Start-Process WITHOUT output redirection to avoid handle inheritance issues
# that cause child processes to exit when the parent PowerShell exits.
# The VBS wrapper uses WMI Win32_Process.Create to run this script under
# WmiPrvSE.exe, ensuring the entire process tree survives parent exit.

Write-Output '[1/3] Starting MySQL ...'
if (Get-Process -Name mysqld -ErrorAction SilentlyContinue) {
    Write-Output '      MySQL is already running, skipping startup.'
} else {
    Start-Process -FilePath 'D:\software\mysql-8.0\bin\mysqld.exe' `
        -ArgumentList '--console' `
        -WindowStyle Hidden
}

Start-Sleep -Seconds 5

Write-Output '[2/3] Starting Redis ...'
Start-Process -FilePath 'D:\software\redis\redis-server.exe' `
    -WindowStyle Hidden

Start-Sleep -Seconds 3

Write-Output '[3/3] Starting RabbitMQ ...'
$env:ERLANG_HOME = 'D:\software\erlang'
Start-Process -FilePath 'D:\software\rabbitmq\sbin\rabbitmq-server.bat' `
    -WindowStyle Hidden

Write-Output ''
Write-Output 'All middleware startup commands sent.'
Write-Output 'MySQL        : localhost:3306'
Write-Output 'Redis        : localhost:6379'
Write-Output 'RabbitMQ UI  : http://localhost:15672'
