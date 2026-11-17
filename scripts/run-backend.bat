@echo off
REM Instala o agente (com testes) no repositorio Maven local e inicia o backend em 127.0.0.1:8080.
REM Requer JDK 21 e Maven. O leitor de sensores e compilado na primeira vez (requer .NET SDK); sem ele,
REM o backend funciona normalmente e GPU/sensores aparecem como "Sem leitura".
REM Ao iniciar, o Windows pede permissao de administrador para o leitor de sensores (temperatura da CPU e ventoinhas).
cd /d "%~dp0\.."
if not exist sensor-host\dist\PCCheck.SensorHost.exe call scripts\build-sensor-host.bat
call mvn -q -N install || exit /b 1
call mvn -q -pl agent install || exit /b 1
call mvn -f backend\pom.xml spring-boot:run
