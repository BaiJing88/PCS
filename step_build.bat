@echo off
echo Step 1: Building API project...
cd /d "%~dp0"
.\gradlew.bat :PCS-API:clean :PCS-API:build --no-daemon -x test

echo.
echo Step 2: Building CentralController...
.\gradlew.bat :PCS-CentralController:clean :PCS-CentralController:build --no-daemon -x test

echo.
echo Step 3: Building Spigot...
.\gradlew.bat :PCS-Spigot:clean :PCS-Spigot:build --no-daemon -x test

echo.
echo Step 4: Temporarily modifying Fabric build to skip API dependency...
copy "PCS-Fabric\build.gradle" "PCS-Fabric\build.gradle.backup"

echo Creating temporary build.gradle without API dependency...
echo // TEMPORARY BUILD FILE - REMOVE API DEPENDENCY FOR NOW > "PCS-Fabric\temp_build.gradle"
echo plugins { >> "PCS-Fabric\temp_build.gradle"
echo     id 'fabric-loom' version '1.7.4' >> "PCS-Fabric\temp_build.gradle"
echo     id 'java-library' >> "PCS-Fabric\temp_build.gradle"
echo } >> "PCS-Fabric\temp_build.gradle"
echo >> "PCS-Fabric\temp_build.gradle"
echo group = 'com.pcs' >> "PCS-Fabric\temp_build.gradle"
echo version = '1.0.0' >> "PCS-Fabric\temp_build.gradle"
echo >> "PCS-Fabric\temp_build.gradle"
echo java { >> "PCS-Fabric\temp_build.gradle"
echo     sourceCompatibility = JavaVersion.VERSION_17 >> "PCS-Fabric\temp_build.gradle"
echo     targetCompatibility = JavaVersion.VERSION_17 >> "PCS-Fabric\temp_build.gradle"
echo } >> "PCS-Fabric\temp_build.gradle"
echo >> "PCS-Fabric\temp_build.gradle"
echo dependencies { >> "PCS-Fabric\temp_build.gradle"
echo     minecraft "com.mojang:minecraft:1.21.1" >> "PCS-Fabric\temp_build.gradle"
echo     mappings "net.fabricmc:yarn:1.21.1+build.1:v2" >> "PCS-Fabric\temp_build.gradle"
echo     modApi "net.fabricmc.fabric-api:fabric-api:0.116.9+1.21.1" >> "PCS-Fabric\temp_build.gradle"
echo } >> "PCS-Fabric\temp_build.gradle"

move "PCS-Fabric\temp_build.gradle" "PCS-Fabric\build.gradle"

echo.
echo Step 5: Building Fabric...
.\gradlew.bat :PCS-Fabric:clean :PCS-Fabric:build --no-daemon -x test

echo.
echo Step 6: Restoring original Fabric build.gradle...
move "PCS-Fabric\build.gradle.backup" "PCS-Fabric\build.gradle"

echo.
echo Build complete!