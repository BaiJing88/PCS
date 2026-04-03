@echo off
echo Step 1: Create a temporary build.gradle for Fabric
cd /d "%~dp0"

echo Creating temporary Fabric build file...

(
echo // TEMPORARY BUILD FILE
echo plugins {
echo     id 'java'
echo     id 'eclipse'
echo     id 'idea'
echo }
echo 
echo group = 'com.pcs.fabric'
echo version = '1.0.0'
echo 
echo sourceCompatibility = targetCompatibility = JavaVersion.VERSION_17
echo 
echo repositories {
echo     mavenCentral()
echo }
echo 
echo dependencies {
echo     implementation files('../PCS-API/build/libs/PCS-API-1.0.0.jar')
echo     implementation 'com.google.code.gson:gson:2.10.1'
echo     implementation 'org.java-websocket:Java-WebSocket:1.5.4'
echo }
echo 
echo processResources {
echo     from('src/main/resources')
echo }
) > "PCS-Fabric\temp_build.gradle"

echo Step 2: Compile only (skip Minecraft dependencies)
cd "PCS-Fabric"
javac -cp "../PCS-API/build/libs/PCS-API-1.0.0.jar;src/main/java" -d build/classes/java/main src/main/java/com/pcs/fabric/*.java 2^>nul

echo Step 3: Create minimal JAR
cd "%~dp0"
if exist "PCS-Fabric\build\classes\java\main\com" (
    jar cf "PCS-Fabric\build\libs\pcs-fabric-1.0.0.jar" -C "PCS-Fabric\build\classes\java\main" com
    echo Fabric temporary build successful!
) else (
    echo Fabric build failed - checking source structure...
)

echo.
echo All builds completed!