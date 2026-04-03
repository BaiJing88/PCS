@echo off
cd /d C:\Users\Administrator\Desktop\PCS
del /f /q "%USERPROFILE%\.gradle\wrapper\dists\gradle-8.8-bin\o87mkasfl7m88gwxgpsor9cx\*.lck" 2>nul
del /f /q "%USERPROFILE%\.gradle\wrapper\dists\gradle-8.8-bin\o87mkasfl7m88gwxgpsor9cx\*.ok" 2>nul
call gradlew.bat build --no-daemon
pause
