$ErrorActionPreference = 'Stop'
$cr = [string][char]13
$lf = [string][char]10
$gbk = [System.Text.Encoding]::GetEncoding(936)

# 1) reset-and-build.bat: UTF-8 + LF + chcp 65001  ->  GBK + CRLF, drop chcp line
$p1 = 'D:\develop\auto-test-platform\backend\scripts\reset-and-build.bat'
$t1 = [System.IO.File]::ReadAllText($p1, [System.Text.Encoding]::UTF8)
$t1 = $t1.Replace($cr, '')
$lines1 = @($t1 -split $lf)
$lines1 = @($lines1 | Where-Object { $_ -notmatch '^chcp 65001' })
while ($lines1.Count -gt 0 -and $lines1[-1] -eq '') { $lines1 = @($lines1[0..($lines1.Count - 2)]) }
$out1 = ($lines1 -join ($cr + $lf)) + $cr + $lf
[System.IO.File]::WriteAllText($p1, $out1, $gbk)

# 2) start-server.bat: normalize line endings to CRLF (pure ASCII)
$p2 = 'D:\develop\auto-test-platform\backend\start-server.bat'
$t2 = [System.IO.File]::ReadAllText($p2, [System.Text.Encoding]::UTF8)
$t2 = $t2.Replace($cr, '')
$lines2 = @($t2 -split $lf)
while ($lines2.Count -gt 0 -and $lines2[-1] -eq '') { $lines2 = @($lines2[0..($lines2.Count - 2)]) }
$out2 = ($lines2 -join ($cr + $lf)) + $cr + $lf
[System.IO.File]::WriteAllText($p2, $out2, $gbk)

# verify both files
foreach ($p in @($p1, $p2)) {
    $b = [System.IO.File]::ReadAllBytes($p)
    $t = $gbk.GetString($b)
    $crlf = ([Regex]::Matches($t, ($cr + $lf))).Count
    $totalLf = ([Regex]::Matches($t, $lf)).Count
    Write-Output ('FILE: ' + $p)
    Write-Output ('  CRLF: ' + $crlf + '  LF_ONLY: ' + ($totalLf - $crlf) + '  HAS_CHCP: ' + ($t -match 'chcp'))
    Write-Output ('  HAS_CHINESE: ' + ($t -match('[\u4e00-\u9fff]')))
}
Write-Output 'FIX_DONE'
