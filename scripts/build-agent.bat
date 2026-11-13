@echo off
REM Compila e executa o agente sem Maven (apenas JDK 21).
cd /d "%~dp0\.."
if exist agent\out rmdir /s /q agent\out
mkdir agent\out
dir /s /b agent\src\main\java\*.java > agent\out\sources.txt
javac --release 21 -d agent\out @agent\out\sources.txt || exit /b 1
java -cp agent\out com.pccheck.agent.PcCheckAgent
