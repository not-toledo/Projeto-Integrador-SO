#!/usr/bin/env bash
# Verificacao da API do Live Monitor (V0.7). Com o backend em execucao (scripts/run-backend.sh):
#   tests/verificar-live.sh [url-base]    (padrao: http://127.0.0.1:8080)
# Aguarda algumas coletas, confere a estrutura e que o historico cresce sem passar do limite.
set -uo pipefail
BASE="${1:-http://127.0.0.1:8080}"

get() { curl -sS --max-time 20 "$BASE$1"; }

if ! BODY="$(get /api/live)"; then
  echo "FALHA: nao foi possivel acessar $BASE/api/live (o backend esta em execucao?)"
  exit 2
fi

FAIL=0
check() { # descricao corpo padrao
  if grep -qF -- "$3" <<<"$2"; then echo "OK    $1"; else echo "FALHA $1"; FAIL=1; fi
}

echo "Aguardando coletas em segundo plano (6 s)..."
sleep 6
BODY="$(get /api/live)"
HIST="$(get '/api/live/history?limit=3')"
echo "$BODY"
echo "-----"

check "estado da coleta" "$BODY" '"status":{"running":true'
check "amostra mais recente" "$BODY" '"latest":{"collectedAt"'
for group in cpu memory gpu disks sensors; do
  check "grupo $group" "$BODY" "\"$group\":{\"status\":"
done
check "historico retorna amostras" "$HIST" '"samples":[{'
check "GPU informa nao suportado (sem inventar valor)" "$BODY" '"gpu":{"status":"UNAVAILABLE"'

N="$(grep -o '"collectedAt"' <<<"$HIST" | wc -l)"
if [ "$N" -le 3 ] && [ "$N" -ge 1 ]; then echo "OK    limite do historico respeitado ($N amostras)"; else echo "FALHA limite do historico ($N)"; FAIL=1; fi

if grep -qF '"consecutiveFailures":0' <<<"$BODY"; then echo "OK    sem falhas consecutivas"; else echo "AVISO: ha falhas consecutivas de coleta (veja lastError acima)."; fi
echo "Obs.: sensores, GPU e taxas de disco podem constar como indisponiveis; isso e esperado em muitos computadores."

if [ "$FAIL" -eq 0 ]; then echo "RESULTADO: verificacao concluida com sucesso."; else echo "RESULTADO: verificacao FALHOU."; fi
exit $FAIL
