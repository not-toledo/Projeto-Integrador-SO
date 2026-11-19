#!/usr/bin/env bash
# Atualiza a tabela de benchmarks de CPU e GPU (backend/src/main/resources/benchmarks/blender-opendata.csv)
# a partir do snapshot diario do Blender Open Data (https://opendata.blender.org, licenca CC0).
#   scripts/atualizar-benchmarks.sh [opendata-latest.zip]
# Sem argumento, baixa o snapshot (~100 MB) para uma pasta temporaria e o apaga no fim.
# Requer JDK 21, Maven e curl. Nao e executado durante o uso do NEXCORE: o CSV gerado fica no repositorio.
set -euo pipefail
cd "$(dirname "$0")/.."
# O agente e reinstalado abaixo: com um backend aberto, isso quebraria o backend em execucao.
if (exec 3<>/dev/tcp/127.0.0.1/8080) 2>/dev/null; then
  echo "A porta 8080 esta em uso (backend do NEXCORE aberto). Feche o backend e rode este script de novo." >&2
  exit 1
fi

URL="https://opendata.blender.org/snapshots/opendata-latest.zip"
OUT="backend/src/main/resources/benchmarks/blender-opendata.csv"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

ZIP="${1:-}"
if [ -z "$ZIP" ]; then
  ZIP="$TMP/opendata-latest.zip"
  echo "Baixando $URL ..."
  curl -fSL --max-time 900 -o "$ZIP" "$URL"
fi

mvn -q -N install
mvn -q -pl agent install -DskipTests
mvn -q -f backend/pom.xml compile dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory="$TMP/lib"
SEP=":"; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";";; esac
java -cp "backend/target/classes${SEP}$TMP/lib/*" com.pccheck.benchmark.BlenderDatasetBuilder "$ZIP" "$OUT"
echo "Tabela atualizada: $OUT (revise o diff e rode os testes do backend antes do commit)."
