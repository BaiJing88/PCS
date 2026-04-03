@echo off
cd /d c:\Users\Administrator\Desktop\PCS
gradlew.bat :PCS-API:build -x test
echo API_BUILD_EXIT=%ERRORLEVEL%
if %ERRORLEVEL% EQU 0 (
    gradlew.bat :PCS-Spigot:build -x test
    echo SPIGOT_BUILD_EXIT=%ERRORLEVEL%
    gradlew.bat :PCS-CentralController:build -x test
    echo CENTRAL_BUILD_EXIT=%ERRORLEVEL%
)
