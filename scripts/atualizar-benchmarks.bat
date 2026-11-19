@echo off
REM Atualiza a tabela de benchmarks de CPU e GPU (backend\src\main\resources\benchmarks\blender-opendata.csv)
REM a partir do snapshot diario do Blender Open Data (https://opendata.blender.org, licenca CC0).
REM   scripts\atualizar-benchmarks.bat [opendata-latest.zip]
REM Sem argumento, baixa o snapshot (~100 MB) para uma pasta temporaria e o apaga no fim.
REM Requer JDK 21, Maven e curl (incluso no Windows 10 1803+). Nao e executado durante o uso do NEXCORE.
setlocal
cd /d "%~dp0\.."
REM O agente e reinstalado abaixo: com um backend aberto, isso quebraria o backend em execucao.
netstat -ano -p tcp | findstr /r /c:":8080 .*LISTENING" >nul
if not errorlevel 1 (
  echo A porta 8080 esta em uso ^(backend do NEXCORE aberto^). Feche o backend e rode este script de novo.
  exit /b 1
)
set "OUT=backend\src\main\resources\benchmarks\blender-opendata.csv"
set "TMPDIR=%TEMP%\nexcore-benchmarks-%RANDOM%%RANDOM%"
mkdir "%TMPDIR%" || exit /b 1
set "ZIP=%~1"
if "%ZIP%"=="" (
  set "ZIP=%TMPDIR%\opendata-latest.zip"
  echo Baixando https://opendata.blender.org/snapshots/opendata-latest.zip ...
  curl -fSL --max-time 900 -o "%TMPDIR%\opendata-latest.zip" https://opendata.blender.org/snapshots/opendata-latest.zip || goto :falha
)
call mvn -q -N install || goto :falha
call mvn -q -pl agent install -DskipTests || goto :falha
call mvn -q -f backend\pom.xml compile dependency:copy-dependencies -DincludeScope=runtime "-DoutputDirectory=%TMPDIR%\lib" || goto :falha
java -cp "backend\target\classes;%TMPDIR%\lib\*" com.pccheck.benchmark.BlenderDatasetBuilder "%ZIP%" "%OUT%" || goto :falha
echo Tabela atualizada: %OUT% (revise o diff e rode os testes do backend antes do commit).
rmdir /s /q "%TMPDIR%"
exit /b 0
:falha
rmdir /s /q "%TMPDIR%"
exit /b 1
