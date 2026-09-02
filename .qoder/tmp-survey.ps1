$ErrorActionPreference = 'SilentlyContinue'

Write-Host '=== sync-permissions.mjs ==='
Select-String -Path 'D:\develop\auto-test--platform\frontend\scripts\sync-permissions.mjs' -Pattern 'suite|case' | ForEach-Object {
  Write-Host ("{0}: {1}" -f $_.LineNumber, $_.Line.Trim())
}

Write-Host '=== docs md counts ==='
foreach ($f in @('SRS', 'PRD', 'LLD', 'DDP', 'HLD')) {
  $p = "D:\develop\auto-test--platform\docs\md\$f.md"
  if (Test-Path $p) {
    $c1 = (Select-String -Path $p -Pattern '测试套件').Count
    $c2 = (Select-String -Path $p -Pattern '测试用例').Count
    $c3 = (Select-String -Path $p -Pattern '自动化用例').Count
    Write-Host ("{0}.md  测试套件={1}  测试用例={2}  自动化用例={3}" -f $f, $c1, $c2, $c3)
  }
}

Write-Host '=== docs ui files ==='
Get-ChildItem -Path 'D:\develop\auto-test--platform\docs\ui' -Recurse -Include '*.html', '*.js' |
  Select-String -Pattern '测试套件|测试用例|自动化用例' -List |
  ForEach-Object { Write-Host $_.Path }

Write-Host '=== V26 defect TEST_CASE ==='
Select-String -Path 'D:\develop\auto-test--platform\backend\platform-server\src\main\resources\db\migration\V26__add_defect_module.sql' -Pattern 'TEST_CASE|target_type' | ForEach-Object {
  Write-Host ("{0}: {1}" -f $_.LineNumber, $_.Line.Trim())
}

Write-Host '=== frontend suite/case refs (other views) ==='
Get-ChildItem -Path 'D:\develop\auto-test--platform\frontend\src' -Recurse -Include '*.vue', '*.ts' |
  Where-Object { $_.FullName -notmatch 'views\\cases|api\\(suite|case)\.ts' } |
  Select-String -Pattern "from '@/api/(suite|case)'|/suites|/cases|suiteId|suiteIds" -List |
  ForEach-Object { Write-Host $_.Path }

Write-Host '=== frontend 测试用例/自动化用例 text ==='
Get-ChildItem -Path 'D:\develop\auto-test--platform\frontend\src' -Recurse -Include '*.vue', '*.ts' |
  Select-String -Pattern '测试用例|自动化用例' -List |
  ForEach-Object { Write-Host $_.Path }

Write-Host '=== backend java suiteId refs (other pkgs) ==='
Get-ChildItem -Path 'D:\develop\auto-test--platform\backend' -Recurse -Include '*.java' |
  Where-Object { $_.FullName -notmatch 'execution\\(entity|mapper|service|controller|engine|dto)' } |
  Select-String -Pattern 'SuiteId|SuiteIds|TestSuite|TestCase|suite_case|test_suite|test_case' -List |
  ForEach-Object { Write-Host $_.Path }

Write-Host '=== KeywordExecutorResolvRefsTest TEST_CASE ==='
Select-String -Path 'D:\develop\auto-test--platform\backend\platform-server\src\test\java\com\platform\execution\engine\KeywordExecutorResolveRefsTest.java' -Pattern 'TEST_CASE|TestCase' | ForEach-Object {
  Write-Host ("{0}: {1}" -f $_.LineNumber, $_.Line.Trim())
}
