#!/usr/bin/env bash
# Verificacao da API de processos (V3.1). Com o backend em execucao (scripts/run-backend.sh):
#   tests/verificar-processos.sh [url-base]    (padrao: http://127.0.0.1:8080)
set -uo pipefail
BASE="${1:-http://127.0.0.1:8080}"

if ! BODY="$(curl -sS --max-time 30 "$BASE/api/processes")"; then
  echo "FALHA: nao foi possivel acessar $BASE/api/processes (o backend esta em execucao?)"
  exit 2
fi

FAIL=0
check() { # descricao padrao
  if grep -qF -- "$2" <<<"$BODY"; then echo "OK    $1"; else echo "FALHA $1"; FAIL=1; fi
}

check "leitura sem erro" '"error":null'
check "intervalo de medicao" '"sampleMillis":1000'
check "lista de processos" '"processes":[{"pid":'
check "proprio backend marcado" '"self":true'
if grep -qE '"osFamily":"(windows|linux)"' <<<"$BODY"; then echo "OK    familia do SO"; else echo "FALHA familia do SO"; FAIL=1; fi
if grep -qF '"pid":0,' <<<"$BODY"; then echo "FALHA processo ocioso (PID 0) na lista"; FAIL=1; else echo "OK    sem o processo ocioso"; fi
if grep -qE '"cpuPercent":(1[0-9]{2}\.[0-9]*[1-9]|[2-9][0-9]{2})' <<<"$BODY"; then
  echo "FALHA cpuPercent acima de 100"; FAIL=1
else
  echo "OK    cpuPercent ate 100"
fi
# Somente leitura: nao pode haver rota que altere processos.
CODE="$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/api/processes")"
if [ "$CODE" = "405" ] || [ "$CODE" = "404" ]; then echo "OK    sem rota de alteracao (DELETE -> $CODE)"; else echo "FALHA DELETE respondeu $CODE"; FAIL=1; fi

N="$(grep -o '"pid":' <<<"$BODY" | wc -l)"
echo "Processos na resposta: $N"
if [ "$FAIL" -eq 0 ]; then echo "RESULTADO: verificacao concluida com sucesso."; else echo "RESULTADO: verificacao FALHOU."; exit 1; fi
