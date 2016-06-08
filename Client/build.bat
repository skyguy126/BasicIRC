@echo off
echo Compiling...
javac -d build -cp lib\*.jar src\*.java
echo Building JAR...
cd build
jar cfm ..\Client.jar manifest.txt socketclient
echo Done!
PAUSE