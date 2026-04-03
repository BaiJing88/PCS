@echo off
cd /d c:\Users\Administrator\Desktop\PCS
echo === Building PCS-CentralController ===
gradlew.bat :PCS-CentralController:build -x test
echo CENTRAL_EXIT=%ERRORLEVEL%
