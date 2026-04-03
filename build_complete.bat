@echo off
echo ========================================
echo PCS 完整构建脚本 (修复版)
echo ========================================
echo.

echo [1/6] 清理旧构建...
gradlew.bat clean

echo.
echo [2/6] 发布 API 项目到本地仓库...
gradlew.bat :PCS-API:publishToMavenLocal --no-daemon

echo.
echo [3/6] 构建 CentralController...
gradlew.bat :PCS-CentralController:build --no-daemon -x test

echo.
echo [4/6] 构建 Spigot...
gradlew.bat :PCS-Spigot:build --no-daemon -x test

echo.
echo [5/6] 构建 Fabric...
gradlew.bat :PCS-Fabric:build --no-daemon -x test

echo.
echo ========================================
echo 构建完成！
echo.
echo 生成的文件：
echo.
echo API: PCS-API\build\libs\PCS-API-1.0.0.jar
echo CentralController: PCS-CentralController\build\libs\pcs-controller-1.0.0.jar  
echo Spigot: PCS-Spigot\build\libs\PCS-Spigot-1.0.0.jar
echo Fabric: PCS-Fabric\build\libs\pcs-fabric-*.jar
echo ========================================
pause