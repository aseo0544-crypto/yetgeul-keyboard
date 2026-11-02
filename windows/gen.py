#!/usr/bin/env python3
"""Java 엔진의 사전·자모 표를 C# 엔진에 채우고, 하나짜리 실행 파일 옛글키보드.ps1을 만든다."""
import re, os
here = os.path.dirname(os.path.abspath(__file__))
root = os.path.dirname(here)
eng = os.path.join(root, "app/src/main/java/com/yetgeul/keyboard/engine")
tables = open(os.path.join(eng, "JamoTables.java"), encoding="utf-8").read()
dic = open(os.path.join(eng, "Dict.java"), encoding="utf-8").read()
cs = open(os.path.join(here, "Engine.cs.tpl"), encoding="utf-8").read()
for k in ("CHO", "JUNG", "JONG"):
    cs = cs.replace("@@%s@@" % k, re.search(k + r" = \{(.*?)\};", tables).group(1))
entries = re.findall(r'^\s+((?:any|noun|stem)\("[^"]*", "[^"]*"\);)', dic, re.M)
cs = cs.replace("@@DICT@@", "\n".join("            " + e for e in entries))
parts = re.search(r"PARTICLES = \{(.*?)\};", dic, re.S).group(1)
cs = cs.replace("@@PARTICLES@@", " ".join(parts.split()))
open(os.path.join(here, "Engine.cs"), "w", encoding="utf-8").write(cs)
app = open(os.path.join(here, "app.ps1.tpl"), encoding="utf-8").read()
app = app.replace("@@ENGINE@@", cs.replace("'@", "' @"))
# PowerShell 5.1은 BOM 없는 UTF-8을 ANSI로 읽으므로 BOM을 붙임
open(os.path.join(here, "옛글키보드.ps1"), "w", encoding="utf-8-sig", newline="\r\n").write(app)
print("entries:", len(entries))
