@echo off
echo 正在部署新构建的jar...
copy /Y "c:\Users\Administrator\Desktop\PCS\PCS-CentralController\build\libs\pcs-central-controller.jar" "c:\Users\Administrator\Desktop\PCS\test-server\pcs-central-controller.jar"
echo Central Controller: %ERRORLEVEL%
copy /Y "c:\Users\Administrator\Desktop\PCS\PCS-Spigot\build\libs\PCS-Spigot-1.0.1.jar" "c:\Users\Administrator\Desktop\PCS\test-server\plugins\PCS-Spigot-1.0.1.jar"
echo Spigot Plugin: %ERRORLEVEL%
echo 部署完成
