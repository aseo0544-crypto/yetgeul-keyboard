# data/ 자료(낱말 대응표, 문장 쌍) 검사: 표기 오류, 사전 반영 여부, 문장 일치율
# JDK만 있으면 됨 (안드로이드 SDK 불필요)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$out = Join-Path $env:TEMP 'yetgeul-data'
New-Item -ItemType Directory -Force $out | Out-Null
$src = @(Get-ChildItem app\src\main\java\com\yetgeul\keyboard\engine\*.java) + @(Get-ChildItem engine-test\*.java)
javac -encoding UTF-8 -d $out $src.FullName
[Console]::OutputEncoding = [Text.Encoding]::UTF8
java "-Dstdout.encoding=UTF-8" -cp $out DataCheck data
