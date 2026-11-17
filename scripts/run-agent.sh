#!/usr/bin/env bash
# Compila (com testes) e executa o agente PCCheck. Requer JDK 21 e Maven.
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -q -pl agent package
java -jar agent/target/pccheck-agent-1.0.0-all.jar
