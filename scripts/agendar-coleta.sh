#!/usr/bin/env bash
# Coleta agendada do PCCheck no cron do usuario (Linux).
#
#   scripts/agendar-coleta.sh instalar [minutos]   cria/atualiza a linha no crontab (padrao: a cada 15 minutos)
#   scripts/agendar-coleta.sh remover              remove a linha (os arquivos de coleta sao mantidos)
#   scripts/agendar-coleta.sh status               mostra a linha e os arquivos de coleta
#   scripts/agendar-coleta.sh agora                faz uma coleta imediatamente
#
# Cada execucao grava uma linha em <dados>/coletas/coletas-AAAA-MM-DD.jsonl (um arquivo por dia) e apaga arquivos
# com mais de 7 dias. <dados> = $PCCHECK_DATA_DIR ou ${XDG_STATE_HOME:-~/.local/state}/pccheck (pasta 700,
# arquivos 600). Retencao: PCCHECK_RETENCAO_DIAS (1 a 365). Se definidas, as duas variaveis sao gravadas na linha
# do cron, porque o cron nao le o ambiente do terminal. Requer JDK 21 e Maven. Nao exige root.
set -euo pipefail
cd "$(dirname "$0")/.."

MARKER="# PCCheck: coleta agendada"
JAR="agent/target/pccheck-agent-1.0.0-all.jar"
ACAO="${1:-status}"

if [ ! -f "$JAR" ]; then
  echo "[PCCheck] Compilando o agente..."
  mvn -q -pl agent package
fi

# Pasta do historico, informada pelo proprio agente (mesma regra usada na gravacao).
COLETAS="$(java -jar "$JAR" --onde 2>/dev/null)" || { echo "[PCCheck] Nao foi possivel executar o agente com Java."; exit 1; }
DADOS="$(dirname "$COLETAS")"

crontab_sem_pccheck() {
  # "crontab -l" falha quando o usuario ainda nao tem crontab: tratado como vazio.
  crontab -l 2>/dev/null | grep -vF "$MARKER" || true
}

case "$ACAO" in
  instalar)
    MIN="${2:-15}"
    if ! [[ "$MIN" =~ ^[0-9]+$ ]] || [ "$MIN" -lt 1 ] || [ "$MIN" -gt 60 ]; then
      echo "[PCCheck] Intervalo invalido: \"$MIN\". Use minutos de 1 a 60."; exit 2
    fi
    command -v crontab >/dev/null || { echo "[PCCheck] crontab nao encontrado (instale o cron)."; exit 1; }
    JAVA="$(command -v java)"
    # Copia o agente para a pasta de dados: a tarefa continua funcionando mesmo apos "mvn clean".
    install -d -m 700 "$DADOS/bin"
    install -m 600 "$JAR" "$DADOS/bin/pccheck-agent.jar"
    if [ "$MIN" -eq 60 ]; then QUANDO="0 * * * *"; else QUANDO="*/$MIN * * * *"; fi
    AMBIENTE=""
    if [ -n "${PCCHECK_DATA_DIR:-}" ]; then AMBIENTE+="PCCHECK_DATA_DIR=$(printf '%q' "$PCCHECK_DATA_DIR") "; fi
    if [ -n "${PCCHECK_RETENCAO_DIAS:-}" ]; then AMBIENTE+="PCCHECK_RETENCAO_DIAS=$(printf '%q' "$PCCHECK_RETENCAO_DIAS") "; fi
    LINHA="$QUANDO ${AMBIENTE}$(printf '%q' "$JAVA") -jar $(printf '%q' "$DADOS/bin/pccheck-agent.jar") --registrar-coleta >/dev/null 2>&1 $MARKER"
    { crontab_sem_pccheck; echo "$LINHA"; } | crontab -
    java -jar "$DADOS/bin/pccheck-agent.jar" --registrar-coleta 2>/dev/null || true
    echo "[PCCheck] Coleta agendada no cron: a cada $MIN minuto(s). Primeira coleta feita agora."
    echo "[PCCheck] Coletas em: $COLETAS"
    ;;
  remover)
    if ! crontab -l 2>/dev/null | grep -qF "$MARKER"; then
      echo "[PCCheck] A coleta agendada nao esta instalada."; exit 0
    fi
    crontab_sem_pccheck | crontab -
    echo "[PCCheck] Coleta agendada removida do cron. As coletas continuam em: $COLETAS"
    ;;
  status)
    crontab -l 2>/dev/null | grep -F "$MARKER" || echo "[PCCheck] Coleta agendada nao instalada. Use: $0 instalar"
    echo
    echo "[PCCheck] Pasta das coletas: $COLETAS"
    if [ -d "$COLETAS" ]; then ls -l "$COLETAS"; else echo "[PCCheck] Ainda nao ha coletas."; fi
    ;;
  agora)
    java -jar "$JAR" --registrar-coleta 2>/dev/null
    ;;
  *)
    echo "Uso: $0 [instalar [minutos] | remover | status | agora]"; exit 2
    ;;
esac
