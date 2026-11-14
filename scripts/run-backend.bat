@echo off
REM Instala o agente (com testes) no repositorio Maven local e inicia o backend em 127.0.0.1:8080.
REM Requer JDK 21 e Maven.
cd /d "%~dp0\.."
call mvn -q -N install || exit /b 1
call mvn -q -pl agent install || exit /b 1
call mvn -f backend\pom.xml spring-boot:run
