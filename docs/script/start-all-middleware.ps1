# Silent startup of all middleware (MySQL -> Redis -> RabbitMQ)
# Called by start-all-middleware.vbs (hidden window)
# Uses Start-Process to create independent background processes
# (hidden window + output redirected to log files)
# Fixes: VBS hidden window cannot launch foreground console apps via 'start cmd /k'

$mysqlLog  = 'D:\software\mysql-8.0\data\mysql-startup.log'
$redisLog  = 'D:\software\redis\redis-startup.log'
$rabbitLog = 'D:\software\rabbitmq\rabbitmq-startup.log'

Write-Output '[1/3] Starting MySQL ...'
Start-Process -FilePath 'D:\software\mysql-8.0\bin\mysqld.exe' `
    -ArgumentList '--console' `
    -RedirectStandardOutput $mysqlLog `
    -RedirectStandardError ($mysqlLog + '.err') `
    -WindowStyle Hidden

Start-Sleep -Seconds 5

Write-Output '[2/3] Starting Redis ...'
Start-Process -FilePath 'D:\software\redis\redis-server.exe' `
    -RedirectStandardOutput $redisLog `
    -RedirectStandardError ($redisLog + '.err') `
    -WindowStyle Hidden

Start-Sleep -Seconds 3

Write-Output '[3/3] Starting RabbitMQ ...'
$env:ERLANG_HOME = 'D:\software\erlang'
Start-Process -FilePath 'D:\software\rabbitmq\sbin\rabbitmq-server.bat' `
    -RedirectStandardOutput $rabbitLog `
    -RedirectStandardError ($rabbitLog + '.err') `
    -WindowStyle Hidden

Write-Output 'All middleware startup commands sent.'
Write-Output 'MySQL        : localhost:3306'
Write-Output 'Redis        : localhost:6379'
Write-Output 'RabbitMQ UI  : http://localhost:15672'
Write-Output 'Logs         : *-startup.log (.err for errors)'
