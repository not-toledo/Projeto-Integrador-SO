#!/usr/bin/env bash
# Instala o agente (com testes) no repositório Maven local e inicia o backend em 127.0.0.1:8080.
# Requer JDK 21 e Maven. No Windows (Git Bash), compila o leitor de sensores na primeira vez se o .NET SDK existir.
set -euo pipefail
cd "$(dirname "$0")/.."
# Se a porta 8080 já está em uso (outro backend aberto), para antes de reinstalar o agente: trocar o jar
# com o backend aberto quebra o backend que está rodando (erro 500 em telas ainda não abertas).
if (exec 3<>/dev/tcp/127.0.0.1/8080) 2>/dev/null; then
  echo "A porta 8080 já está em uso: o backend do NEXCORE provavelmente já está aberto." >&2
  echo "Feche o outro backend (Ctrl+C) e rode este script de novo." >&2
  exit 1
fi
if [ ! -f sensor-host/dist/NEXCORE.SensorHost.exe ] && command -v dotnet >/dev/null 2>&1; then
  dotnet publish sensor-host/NEXCORE.SensorHost.csproj -c Release -o sensor-host/dist --nologo -v quiet
fi
mvn -q -N install
mvn -q -pl agent install
mvn -f backend/pom.xml spring-boot:run
