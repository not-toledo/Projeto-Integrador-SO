@echo off
REM Compila (com testes) e executa o agente PCCheck. Requer JDK 21 e Maven.
cd /d "%~dp0\.."
call mvn -q -pl agent package || exit /b 1
java -jar agent\target\pccheck-agent-0.5.0-all.jar
