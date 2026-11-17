# Checklist de revisão da documentação

Use antes de abrir um Pull Request que altere a documentação ou o comportamento do projeto.

## Estrutura

- [ ] `README.md` na raiz, com objetivo, instalação, configuração, execução e testes.
- [ ] `docs/` contém documentação de projeto, processo, API, diagramas e checklist.
- [ ] `templates/README-projeto.md` existe para novos projetos.
- [ ] `CONTRIBUTING.md` e `SECURITY.md` existem na raiz.
- [ ] Todos os links internos apontam para arquivos existentes (`scripts/testes-documentacao.sh` verifica).

## Conteúdo

- [ ] Objetivo, escopo, requisitos e arquitetura estão atualizados.
- [ ] Tecnologias e versões conferem com os arquivos de build (`pom.xml`, `package.json`).
- [ ] Comandos foram copiados do projeto e podem ser executados como estão.
- [ ] Testes, pipelines e integrações são descritos como executados **somente** quando há evidência.
- [ ] Limitações conhecidas estão listadas.
- [ ] Pendências aparecem como "Pendente: ..." e não ficaram vazias ou sem explicação.
- [ ] Nenhuma senha, token, chave ou dado pessoal real aparece nos exemplos.

## Sincronização com o código

- [ ] Endpoints e exemplos de resposta conferem com os controllers e DTOs.
- [ ] Chaves de `application.properties` citadas na documentação existem no arquivo.
- [ ] Versão nova registrada no `CHANGELOG.md`.
- [ ] Diagramas refletem a arquitetura atual, com descrição em texto.

## Qualidade

- [ ] Frases verificáveis: nada de "funciona corretamente" sem critério.
- [ ] Quando algo não se aplica, a seção diz por quê, em vez de conteúdo fictício.
