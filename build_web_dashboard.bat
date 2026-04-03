@echo off
echo ========================================
echo PCS Web仪表盘构建脚本
echo ========================================
echo.

echo [1/4] 创建Web仪表盘分发目录...
if not exist "web_dashboard" mkdir web_dashboard
if not exist "web_dashboard\assets" mkdir web_dashboard\assets

echo.

echo [2/4] 复制Web控制面板文件...
copy "pcs_dashboard_pro.html" "web_dashboard\index.html" >nul
copy "config_tab.js" "web_dashboard\" >nul

echo.

echo [3/4] 创建配置说明文档...
echo # PCS Web控制面板使用说明 > web_dashboard\README.md
echo. >> web_dashboard\README.md
echo ## 特性 >> web_dashboard\README.md
echo. >> web_dashboard\README.md
echo - 多主题支持（深色、浅色、复古） >> web_dashboard\README.md
echo - 响应式设计 >> web_dashboard\README.md
echo - 实时服务器状态监控 >> web_dashboard\README.md
echo - 完整的服务器管理功能 >> web_dashboard\README.md
echo - 数据导入导出 >> web_dashboard\README.md
echo - 玩家管理系统 >> web_dashboard\README.md
echo. >> web_dashboard\README.md
echo ## 部署 >> web_dashboard\README.md
echo. >> web_dashboard\README.md
echo 1. 将web_dashboard文件夹上传到Web服务器 >> web_dashboard\README.md
echo 2. 通过浏览器访问index.html >> web_dashboard\README.md
echo 3. 默认主题为深色模式，可在侧边栏切换 >> web_dashboard\README.md

echo.

echo [4/4] 创建示例API配置文件...
echo {
echo   "api_endpoint": "http://localhost:8080/api",
echo   "websocket_endpoint": "ws://localhost:8080/ws",
echo   "auth_token": "your_token_here",
echo   "refresh_interval": 30
echo } > web_dashboard\config.example.json

echo.
echo ========================================
echo Web仪表盘构建完成！
echo.
echo 生成目录：web_dashboard\
echo 主文件：web_dashboard\index.html
echo ========================================
pause