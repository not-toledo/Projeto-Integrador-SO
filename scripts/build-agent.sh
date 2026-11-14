#!/usr/bin/env bash
# Compila e executa o agente sem Maven (apenas JDK 21). Útil na V0.1, que não tem dependências.
set -euo pipefail
cd "$(dirname "$0")/.."
rm -rf agent/out && mkdir -p agent/out
javac --release 21 -d agent/out $(find agent/src/main/java -name "*.java")
java -cp agent/out com.pccheck.agent.PcCheckAgent
