@echo off
REM Compila o leitor de sensores (sensor-host) em sensor-host\dist\NEXCORE.SensorHost.exe.
REM Requer apenas para compilar: .NET SDK 8 ou superior. Para executar basta o .NET Framework 4.8 (ja vem no Windows 10/11).
cd /d "%~dp0\.."
where dotnet >nul 2>nul || (echo [NEXCORE] .NET SDK nao encontrado: instale-o para compilar o leitor de sensores. & exit /b 1)
dotnet publish sensor-host\NEXCORE.SensorHost.csproj -c Release -o sensor-host\dist --nologo -v quiet || exit /b 1
echo [NEXCORE] Leitor de sensores compilado em sensor-host\dist\NEXCORE.SensorHost.exe
