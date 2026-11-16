# Como contribuir

Obrigado pelo interesse. Antes de contribuir, leia a [documentação de projeto](docs/documentacao-de-projeto.md) e o
[processo de desenvolvimento](docs/processo-devops.md).

## Fluxo

1. Abra uma **Issue** descrevendo o problema ou a melhoria, com critérios de aceite.
2. Crie uma branch a partir da `main` com nome descritivo (ex.: `feat/lhm-gpu`, `fix/parser-ventoinhas`, `docs/api`).
3. Faça commits pequenos, com mensagens no imperativo (ex.: "Adiciona parser de ventoinhas").
4. Rode os testes relevantes (veja a tabela em [processo-devops.md](docs/processo-devops.md)).
5. Abra um **Pull Request** para `main`, com descrição do que mudou e como foi testado.

## Regras do projeto

- Nunca apresente dados simulados como medições reais. Se um dado não existe, mostre "Não disponível" ou "Sem leitura".
- Não inclua senhas, tokens, chaves ou dados pessoais reais, nem em exemplos.
- Atualize a documentação e o `CHANGELOG.md` quando o comportamento mudar.
- Não adicione dependências sem justificar no Pull Request.
- Antes de abrir o PR, rode `scripts/testes-documentacao.sh` se alterou documentos.
