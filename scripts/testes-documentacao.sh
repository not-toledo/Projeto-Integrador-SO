#!/usr/bin/env bash
# Verificações estruturais da documentação (não validam o conteúdo editorial nem o funcionamento do código).
# Uso: scripts/testes-documentacao.sh   (executar na raiz do projeto ou de qualquer pasta)
set -uo pipefail
cd "$(dirname "$0")/.." || exit 2

FALHAS=0
falha() { echo "FALHA: $1"; FALHAS=$((FALHAS + 1)); }
ok() { echo "OK:    $1"; }

# 1. Arquivos obrigatórios existem e não estão vazios.
OBRIGATORIOS=(
  README.md CONTRIBUTING.md SECURITY.md
  docs/documentacao-de-projeto.md docs/documentacao-api.md docs/processo-devops.md
  docs/diagramas.md docs/checklist-documentacao.md docs/CHANGELOG.md docs/sensores.md
  templates/README-projeto.md
)
for arq in "${OBRIGATORIOS[@]}"; do
  if [ ! -s "$arq" ]; then falha "$arq ausente ou vazio"; else ok "$arq"; fi
done

# 2. Pasta docs/ tem Markdown.
if [ -z "$(find docs -name '*.md' -print -quit 2>/dev/null)" ]; then
  falha "docs/ não contém arquivos Markdown"
else
  ok "docs/ contém Markdown"
fi

# 3. Marcadores de conteúdo pendente não resolvidos (TODO, TBD, XXX, LOREM).
# node_modules (dependências locais, fora do Git) não é documentação do projeto.
PENDENTES=$(grep -rnE '\b(TODO|TBD|XXX|LOREM)\b' README.md CONTRIBUTING.md SECURITY.md docs templates --include='*.md' --exclude-dir=node_modules 2>/dev/null)
if [ -n "$PENDENTES" ]; then
  falha "marcadores pendentes encontrados:"
  echo "$PENDENTES"
else
  ok "nenhum marcador TODO/TBD/XXX/LOREM"
fi

# 4. Links relativos internos apontam para arquivos existentes.
LINKS_QUEBRADOS=0
while IFS= read -r doc; do
  dir="$(dirname "$doc")"
  while IFS= read -r alvo; do
    caminho="${alvo%%#*}"
    [ -z "$caminho" ] && continue
    case "$caminho" in http://*|https://*|mailto:*) continue ;; esac
    if [ ! -e "$dir/$caminho" ]; then
      falha "link quebrado em $doc: $alvo"
      LINKS_QUEBRADOS=$((LINKS_QUEBRADOS + 1))
    fi
  done < <(grep -oE '\]\([^)]+\)' "$doc" | sed -E 's/^\]\(//; s/\)$//')
done < <(find . -name '*.md' -not -path '*/node_modules/*' -not -path '*/target/*' -not -path './templates/*' | sed 's#^\./##')
# templates/ é copiado para a raiz de outro projeto; seus links são relativos à raiz e não são verificados aqui.
[ "$LINKS_QUEBRADOS" -eq 0 ] && ok "links internos válidos"

# 5. Nenhum segredo óbvio na documentação.
if grep -rniE '(password|senha|token|api[_-]?key|secret)[[:space:]]*[:=][[:space:]]*[^[:space:]<]{6,}' README.md docs templates --include='*.md' --exclude-dir=node_modules 2>/dev/null; then
  falha "possível segredo em texto (verifique as linhas acima)"
else
  ok "nenhum segredo óbvio"
fi

echo
if [ "$FALHAS" -eq 0 ]; then
  echo "Documentação: todas as verificações passaram."
  exit 0
fi
echo "Documentação: $FALHAS falha(s)."
exit 1
