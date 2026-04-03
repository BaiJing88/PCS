@echo off
cd /d C:\Users\Administrator\Desktop\PCS

echo Cleaning all build caches...
if exist "PCS-Fabric\.gradle"    rmdir /s /q "PCS-Fabric\.gradle"
if exist "PCS-Fabric\build"     rmdir /s /q "PCS-Fabric\build"
if exist "PCS-API\.gradle"     rmdir /s /q "PCS-API\.gradle"
if exist "PCS-API\build"        rmdir /s /q "PCS-API\build"
if exist "PCS-Spigot\.gradle"   rmdir /s /q "PCS-Spigot\.gradle"
if exist "PCS-Spigot\build"     rmdir /s /q "PCS-Spigot\build"
if exist "PCS-CentralController\.gradle"   rmdir /s /q "PCS-CentralController\.gradle"
if exist "PCS-CentralController\build"      rmdir /s /q "PCS-CentralController\build"

echo Building all modules...
call gradlew.bat build --no-daemon
pause
