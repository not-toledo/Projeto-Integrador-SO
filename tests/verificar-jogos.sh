#!/usr/bin/env bash
# Verificacao dos jogos instalados e da pre-checagem (V2.8). Com o backend em execucao (scripts/run-backend.sh):
#   tests/verificar-jogos.sh [appId]    (padrao: o primeiro jogo instalado encontrado)
# Funciona sem Steam instalada: confere que a resposta diz que a Steam nao foi encontrada.
set -uo pipefail
BASE="${PCCHECK_API:-http://127.0.0.1:8080/api/games}"

if ! INSTALLED="$(curl -sS --max-time 30 "$BASE/installed")"; then
  echo "FALHA: nao foi possivel acessar $BASE/installed (o backend esta em execucao?)"
  exit 2
fi

FAIL=0
check() { # descricao texto padrao
  if grep -qF -- "$3" <<<"$2"; then echo "OK    $1"; else echo "FALHA $1"; FAIL=1; fi
}

check "jogos instalados: steamFound" "$INSTALLED" '"steamFound"'
check "jogos instalados: lista de jogos" "$INSTALLED" '"games":['
check "jogos instalados: problemas" "$INSTALLED" '"problems":['
if grep -qF '"LastOwner"' <<<"$INSTALLED" || grep -qiF '"lastowner"' <<<"$INSTALLED"; then
  echo "FALHA a resposta expoe o ID da conta Steam"; FAIL=1
else
  echo "OK    ID da conta Steam nao exposto"
fi

APP="${1:-$(grep -oE '"appId":[0-9]+' <<<"$INSTALLED" | head -n 1 | cut -d: -f2)}"
if [ -z "$APP" ]; then
  echo "AVISO: nenhum jogo instalado; pre-checagem nao verificada (informe um AppID como argumento)."
else
  if ! PRE="$(curl -sS --max-time 60 "$BASE/$APP/precheck")"; then
    echo "FALHA: pre-checagem do AppID $APP nao respondeu"; FAIL=1
  else
    echo "Pre-checagem do AppID $APP:"
    check "pre-checagem: resultado geral" "$PRE" '"overall":"'
    for id in ram cpu install power battery; do
      check "pre-checagem: item $id" "$PRE" "\"id\":\"$id\""
    done
  fi
fi

if [ "$FAIL" -eq 0 ]; then echo "RESULTADO: verificacao concluida com sucesso."; else echo "RESULTADO: verificacao FALHOU."; fi
exit $FAIL
