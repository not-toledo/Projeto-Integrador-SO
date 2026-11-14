#!/usr/bin/env bash
# Verificacao de ponta a ponta do agente (V0.3): executa o jar e confere a saida.
# Uso: tests/verificar-agente.sh [caminho-do-jar]
# Pre-requisito: agente compilado (mvn -pl agent package) ou scripts/run-agent.sh.
set -uo pipefail
cd "$(dirname "$0")/.."
JAR="${1:-agent/target/pccheck-agent-0.3.0-all.jar}"

if [ ! -f "$JAR" ]; then
  echo "FALHA: jar nao encontrado em $JAR. Compile com: mvn -pl agent package"
  exit 2
fi

OUT="$(java -jar "$JAR" 2>&1)"
STATUS=$?
echo "$OUT"
echo "-----"

FAIL=0
check() { # descricao padrao
  if grep -qF -- "$2" <<<"$OUT"; then echo "OK    $1"; else echo "FALHA $1"; FAIL=1; fi
}

[ "$STATUS" -eq 0 ] || { echo "FALHA agente terminou com codigo $STATUS"; FAIL=1; }
check "secao Sistema operacional" "[Sistema operacional]"
check "secao CPU" "[CPU]"
check "secao Memoria" "[Memoria]"
check "secao Placa de video" "[Placa de video]"
check "secao Placa-mae" "[Placa-mae]"
check "secao Armazenamento" "[Armazenamento]"
check "horario da coleta" "Coleta em:"

# Secao com erro nao deve acontecer em maquina normal; apenas alerta (pode depender de driver/permissao).
if grep -qF "ERRO -" <<<"$OUT"; then echo "AVISO: ha coletor com ERRO (veja acima)."; fi

if [ "$FAIL" -eq 0 ]; then echo "RESULTADO: verificacao concluida com sucesso."; else echo "RESULTADO: verificacao FALHOU."; fi
exit $FAIL
