@echo off
setlocal
cd /d "%~dp0"

echo =====================================================================
echo           Creating MobiStock Desktop Shortcut...
echo =====================================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command "$dir = (Get-Item .).FullName; $desktop = [Environment]::GetFolderPath('Desktop'); $wsh = New-Object -ComObject WScript.Shell; $sc = $wsh.CreateShortcut(\"$desktop\MobiStock.lnk\"); $sc.TargetPath = \"$dir\run.bat\"; $sc.WorkingDirectory = \"$dir\"; $sc.Description = 'MobiStock - Mobile Inventory & POS'; if (Test-Path \"$dir\mobistock.ico\") { $sc.IconLocation = \"$dir\mobistock.ico,0\" }; $sc.Save(); Write-Host 'Shortcut created successfully on Desktop: MobiStock.lnk' -ForegroundColor Green"

echo.
pause
