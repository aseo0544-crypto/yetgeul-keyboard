@echo off
rem Yetgeul keyboard PC launcher: runs yetgeul-keyboard-pc.ps1 in the same folder
powershell -NoProfile -ExecutionPolicy Bypass -Command "Unblock-File -LiteralPath '%~dp0yetgeul-keyboard-pc.ps1' -ErrorAction SilentlyContinue"
start "" powershell -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "%~dp0yetgeul-keyboard-pc.ps1"
