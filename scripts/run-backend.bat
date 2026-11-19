@echo off
REM Instala o agente (com testes) no repositorio Maven local e inicia o backend em 127.0.0.1:8080.
REM Requer JDK 21 e Maven. O leitor de sensores e compilado na primeira vez (requer .NET SDK); sem ele,
REM o backend funciona normalmente e GPU/sensores aparecem como "Sem leitura".
REM Ao iniciar, o Windows pede permissao de administrador para o leitor de sensores (temperatura da CPU e ventoinhas).
cd /d "%~dp0\.."
REM Se a porta 8080 ja esta em uso (outro backend aberto), para antes de reinstalar o agente: trocar o jar
REM com o backend aberto quebra o backend que esta rodando (erro 500 em telas ainda nao abertas).
netstat -ano -p tcp | findstr /r /c:":8080 .*LISTENING" >nul
if not errorlevel 1 (
  echo A porta 8080 ja esta em uso: o backend do NEXCORE provavelmente ja esta aberto.
  echo Feche a outra janela do backend ^(Ctrl+C^) e rode este script de novo.
  exit /b 1
)
if not exist sensor-host\dist\NEXCORE.SensorHost.exe call scripts\build-sensor-host.bat
call mvn -q -N install || exit /b 1
call mvn -q -pl agent install || exit /b 1
call mvn -f backend\pom.xml spring-boot:run
