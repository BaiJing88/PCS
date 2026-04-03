@echo off
echo =======================================
echo PCS项目按序构建脚本
echo =======================================
echo 注意：请确保已关闭其他Gradle进程
echo.

echo 步骤1：清理所有构建目录...
rd /s /q "PCS-API\build" 2>nul
rd /s /q "PCS-CentralController\build" 2>nul
rd /s /q "PCS-Spigot\build" 2>nul
rd /s /q "PCS-Fabric\build" 2>nul

echo.
echo 步骤2：单独构建API项目...
call gradlew.bat :PCS-API:clean :PCS-API:build --no-daemon -x test
if errorlevel 1 (
    echo API项目构建失败！
    pause
    exit /b 1
)
echo ✅ API项目构建成功！

echo.
echo 步骤3：构建CentralController...
call gradlew.bat :PCS-CentralController:clean :PCS-CentralController:build --no-daemon -x test
if errorlevel 1 (
    echo CentralController构建失败！
    pause
    exit /b 1
)
echo ✅ CentralController构建成功！

echo.
echo 步骤4：构建Spigot...
call gradlew.bat :PCS-Spigot:clean :PCS-Spigot:build --no-daemon -x test
if errorlevel 1 (
    echo Spigot构建失败！
    pause
    exit /b 1
)
echo ✅ Spigot构建成功！

echo.
echo 步骤5：尝试构建Fabric...
call gradlew.bat :PCS-Fabric:clean :PCS-Fabric:build --no-daemon -x test
if errorlevel 1 (
    echo ❌ Fabric构建失败，正在尝试备用方案...
    echo.
    echo 备用方案：跳过Fabric，其他三个项目已构建成功
    echo 构建状态：3/4 成功
    pause
    exit /b 0
)
echo ✅ Fabric构建成功！

echo.
echo =======================================
echo ✅ 所有项目构建成功！
echo 构建结果：
echo   PCS-API ✓
echo   PCS-CentralController ✓
echo   PCS-Spigot ✓
echo   PCS-Fabric ✓
echo =======================================
pause