@echo off
REM Coleta agendada do PCCheck no Agendador de Tarefas do Windows (schtasks).
REM
REM   scripts\agendar-coleta.bat instalar [minutos]   cria/atualiza a tarefa (padrao: a cada 15 minutos)
REM   scripts\agendar-coleta.bat remover              remove a tarefa (os arquivos de coleta sao mantidos)
REM   scripts\agendar-coleta.bat status               mostra a tarefa e os arquivos de coleta
REM   scripts\agendar-coleta.bat agora                faz uma coleta imediatamente
REM
REM Cada execucao grava uma linha em <dados>\coletas\coletas-AAAA-MM-DD.jsonl (um arquivo por dia) e apaga
REM arquivos com mais de 7 dias. <dados> = PCCHECK_DATA_DIR ou LOCALAPPDATA\PCCheck.
REM Retencao: variavel PCCHECK_RETENCAO_DIAS (1 a 365). Para que a tarefa use essas variaveis, defina-as
REM para o usuario com "setx" (a tarefa nao herda variaveis definidas so neste terminal).
REM Requer JDK 21 e Maven (para compilar o agente na primeira vez). Nao exige administrador.
setlocal EnableDelayedExpansion
cd /d "%~dp0\.."

set "TASK=PCCheck\Coleta"
set "JAR=agent\target\pccheck-agent-1.0.0-all.jar"
set "ACAO=%~1"
if "%ACAO%"=="" set "ACAO=status"

if not exist "%JAR%" (
    echo [PCCheck] Compilando o agente...
    call mvn -q -pl agent package || exit /b 1
)

REM Pasta do historico, informada pelo proprio agente (mesma regra usada na gravacao).
set "COLETAS="
for /f "delims=" %%D in ('java -jar "%JAR%" --onde 2^>nul') do set "COLETAS=%%D"
if not defined COLETAS (echo [PCCheck] Nao foi possivel executar o agente com Java. & exit /b 1)
for %%P in ("%COLETAS%\..") do set "DADOS=%%~fP"

if /i "%ACAO%"=="instalar" goto :instalar
if /i "%ACAO%"=="remover" goto :remover
if /i "%ACAO%"=="status" goto :status
if /i "%ACAO%"=="agora" goto :agora
echo Uso: %~nx0 [instalar [minutos] ^| remover ^| status ^| agora]
exit /b 2

:instalar
set "MIN=%~2"
if "%MIN%"=="" set "MIN=15"
REM Somente numeros de 1 a 1439 (limite do schtasks para /SC MINUTE).
echo %MIN%| findstr /r "^[0-9][0-9]*$" >nul || (echo [PCCheck] Intervalo invalido: "%MIN%". Use minutos de 1 a 1439. & exit /b 2)
if %MIN% LSS 1 (echo [PCCheck] Intervalo invalido: use minutos de 1 a 1439. & exit /b 2)
if %MIN% GTR 1439 (echo [PCCheck] Intervalo invalido: use minutos de 1 a 1439. & exit /b 2)

REM javaw executa sem abrir uma janela de console a cada coleta.
set "JAVAW="
for /f "delims=" %%J in ('where javaw 2^>nul') do if not defined JAVAW set "JAVAW=%%J"
if not defined JAVAW (echo [PCCheck] javaw.exe nao encontrado no PATH. & exit /b 1)

REM Copia o agente para a pasta de dados: a tarefa continua funcionando mesmo apos "mvn clean".
if not exist "%DADOS%\bin" mkdir "%DADOS%\bin" || exit /b 1
copy /y "%JAR%" "%DADOS%\bin\pccheck-agent.jar" >nul || (echo [PCCheck] Falha ao copiar o agente. & exit /b 1)

schtasks /Create /TN "%TASK%" /SC MINUTE /MO %MIN% /F /TR "\"%JAVAW%\" -jar \"%DADOS%\bin\pccheck-agent.jar\" --registrar-coleta" >nul
if errorlevel 1 (echo [PCCheck] O Agendador de Tarefas recusou a criacao da tarefa. & exit /b 1)
schtasks /Run /TN "%TASK%" >nul
echo [PCCheck] Tarefa "%TASK%" instalada: uma coleta a cada %MIN% minuto^(s^). Primeira coleta iniciada agora.
echo [PCCheck] Coletas em: %COLETAS%
if defined PCCHECK_DATA_DIR echo [PCCheck] Atencao: PCCHECK_DATA_DIR so vale para a tarefa se definida com setx.
exit /b 0

:remover
schtasks /Query /TN "%TASK%" >nul 2>nul || (echo [PCCheck] A tarefa "%TASK%" nao esta instalada. & exit /b 0)
schtasks /Delete /TN "%TASK%" /F >nul || exit /b 1
echo [PCCheck] Tarefa "%TASK%" removida. As coletas continuam em: %COLETAS%
exit /b 0

:status
schtasks /Query /TN "%TASK%" /V /FO LIST 2>nul || echo [PCCheck] Tarefa "%TASK%" nao instalada. Use: %~nx0 instalar
echo.
echo [PCCheck] Pasta das coletas: %COLETAS%
if exist "%COLETAS%" (dir /b /o:n "%COLETAS%\coletas-*.jsonl" 2>nul) else (echo [PCCheck] Ainda nao ha coletas.)
exit /b 0

:agora
java -jar "%JAR%" --registrar-coleta 2>nul
exit /b %errorlevel%
