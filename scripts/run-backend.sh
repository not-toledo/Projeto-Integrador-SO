#!/usr/bin/env bash
# Instala o agente (com testes) no repositório Maven local e inicia o backend em 127.0.0.1:8080.
# Requer JDK 21 e Maven.
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -q -pl agent install
mvn -f backend/pom.xml spring-boot:run
