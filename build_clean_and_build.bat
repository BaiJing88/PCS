@echo off
echo Cleaning previous builds...
if exist "PCS-API\build" rmdir /s /q "PCS-API\build"
if exist "PCS-CentralController\build" rmdir /s /q "PCS-CentralController\build"
if exist "PCS-Spigot\build" rmdir /s /q "PCS-Spigot\build"
if exist "PCS-Fabric\build" rmdir /s /q "PCS-Fabric\build"

echo Building API project first...
gradlew PCS-API:clean PCS-API:build -x test

echo Building CentralController...
gradlew PCS-CentralController:clean PCS-CentralController:build -x test

echo Building Spigot...
gradlew PCS-Spigot:clean PCS-Spigot:build -x test

echo Building Fabric...
gradlew PCS-Fabric:clean PCS-Fabric:build -x test

echo Build complete!