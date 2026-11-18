#!/usr/bin/env bash
# Verificacao da API de Health Check (V0.5). Com o backend em execucao (scripts/run-backend.sh):
#   tests/verificar-saude.sh [url]    (padrao: http://127.0.0.1:8080/api/health-check)
# Confere a estrutura da resposta; nao julga se os valores estao "bons" (isso depende do computador).
set -uo pipefail
URL="${1:-http://127.0.0.1:8080/api/health-check}"

if ! BODY="$(curl -sS --max-time 30 "$URL")"; then
  echo "FALHA: nao foi possivel acessar $URL (o backend esta em execucao?)"
  exit 2
fi
echo "$BODY"
echo "-----"

FAIL=0
check() { # descricao padrao
  if grep -qF -- "$2" <<<"$BODY"; then echo "OK    $1"; else echo "FALHA $1"; FAIL=1; fi
}

check "horario da avaliacao" '"collectedAt"'
check "origem dos dados" '"source"'
check "severidade geral" '"overall"'
check "origem dos limites" '"thresholdsSource"'
check "alerta de temperatura" '"id":"temperature:cpu"'
check "alerta de memoria" '"id":"memory:usage"'
check "alerta de saude dos discos (SMART)" '"id":"smart'
check "alerta de disco (ou indisponibilidade)" '"id":"disk-space'

echo "Resumo: $(grep -o '"overall":"[A-Z]*"' <<<"$BODY")"
echo "Obs.: temperatura e SMART podem constar como indisponiveis (exigem administrador); isso e esperado em muitos computadores."

if [ "$FAIL" -eq 0 ]; then echo "RESULTADO: verificacao concluida com sucesso."; else echo "RESULTADO: verificacao FALHOU."; fi
exit $FAIL
