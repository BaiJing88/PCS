@echo off
cd /d c:\Users\Administrator\Desktop\PCS
echo === Building PCS-Spigot ===
gradlew.bat :PCS-Spigot:build -x test
echo SPIGOT_EXIT=%ERRORLEVEL%
