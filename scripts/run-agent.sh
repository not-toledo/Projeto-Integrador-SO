#!/usr/bin/env bash
# Compila (com testes) e executa o agente NEXCORE. Requer JDK 21 e Maven.
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -q -pl agent package
java -jar agent/target/nexcore-agent-4.0.0-all.jar
