#!/usr/bin/env bash
# Instala o agente (com testes) no repositório Maven local e inicia o backend em 127.0.0.1:8080.
# Requer JDK 21 e Maven. No Windows (Git Bash), compila o leitor de sensores na primeira vez se o .NET SDK existir.
set -euo pipefail
cd "$(dirname "$0")/.."
if [ ! -f sensor-host/dist/PCCheck.SensorHost.exe ] && command -v dotnet >/dev/null 2>&1; then
  dotnet publish sensor-host/PCCheck.SensorHost.csproj -c Release -o sensor-host/dist --nologo -v quiet
fi
mvn -q -N install
mvn -q -pl agent install
mvn -f backend/pom.xml spring-boot:run
