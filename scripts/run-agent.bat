@echo off
REM Compila (com testes) e executa o agente NEXCORE. Requer JDK 21 e Maven.
cd /d "%~dp0\.."
call mvn -q -pl agent package || exit /b 1
java -jar agent\target\nexcore-agent-4.0.0-all.jar
