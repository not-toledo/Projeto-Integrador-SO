#!/usr/bin/env bash
# Verificacao da API de inventario (V0.3). Com o backend em execucao (scripts/run-backend.sh):
#   tests/verificar-api.sh [url]    (padrao: http://127.0.0.1:8080/api/inventory)
# Confere que cada componente tem estado proprio (OK, UNAVAILABLE ou ERROR).
set -uo pipefail
URL="${1:-http://127.0.0.1:8080/api/inventory}"

if ! BODY="$(curl -sS --max-time 20 "$URL")"; then
  echo "FALHA: nao foi possivel acessar $URL (o backend esta em execucao?)"
  exit 2
fi
echo "$BODY"
echo "-----"

FAIL=0
check() { # descricao padrao
  if grep -qF -- "$2" <<<"$BODY"; then echo "OK    $1"; else echo "FALHA $1"; FAIL=1; fi
}

check "horario da coleta" '"collectedAt"'
check "origem dos dados" '"source"'
for comp in os cpu memory gpu motherboard storage; do
  check "componente $comp" "\"$comp\":{\"status\":"
done

if grep -qF '"status":"ERROR"' <<<"$BODY"; then echo "AVISO: ha componente com status ERROR (veja acima)."; fi

if [ "$FAIL" -eq 0 ]; then echo "RESULTADO: verificacao concluida com sucesso."; else echo "RESULTADO: verificacao FALHOU."; fi
exit $FAIL
