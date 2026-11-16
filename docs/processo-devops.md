# Processo de desenvolvimento e execução — PCCheck

## Ambiente

| Ferramenta | Versão |
|---|---|
| JDK | 21 |
| Maven | 3.9 ou superior |
| Node.js | 22.12 ou superior (`npm test` exige essa versão) |
| Git | qualquer versão recente |
| Sistema | Windows (prioritário) ou Linux |

No Windows, os scripts `tests/*.sh` exigem um shell Bash e `curl`: use o **Git Bash** ou o WSL.

## Versionamento

- A versão **1.0.0** aparece em todos os módulos: POMs (raiz, agente e backend), `PcCheckAgent.VERSION`, `/api/ping`,
  scripts, `frontend/package.json` e `package-lock.json`. Ao mudar a versão, atualize todos esses pontos.
- Cada versão tem entrada em [CHANGELOG.md](CHANGELOG.md), com o que mudou e as pendências conhecidas.

## Branches, commits e Pull Requests

Fluxo recomendado para contribuições (detalhes em [CONTRIBUTING.md](../CONTRIBUTING.md)):

1. Crie uma branch a partir da `main`, com nome descritivo (ex.: `feat/lhm-gpu`, `docs/organizacao`).
2. Faça commits pequenos, com mensagens no imperativo.
3. Rode os testes antes de abrir o Pull Request.
4. Atualize a documentação e o CHANGELOG quando o comportamento mudar.

## Execução

Use dois terminais.

**Terminal 1 — backend** (na raiz do projeto):
```
scripts\run-backend.bat        (Windows)
scripts/run-backend.sh         (Linux)
```
Os scripts instalam o pom pai e o agente no repositório Maven local e iniciam a API em `127.0.0.1:8080`.

**Terminal 2 — frontend:**
```
cd frontend
npm install
npm run dev
```

Antes do backend, para GPU e sensores, abra o LibreHardwareMonitor como administrador e ative o
*Remote Web Server* (passo a passo em [libre-hardware-monitor.md](libre-hardware-monitor.md)).

## Testes

| Comando | O que verifica | Pré-requisito |
|---|---|---|
| `mvn -pl agent test` | Conversões, percentuais, valores inválidos, isolamento de falhas, parser do LibreHardwareMonitor | JDK 21 e Maven |
| `mvn -pl backend -am test` | Regras do Health Check, histórico, motor de coleta, DTOs | JDK 21 e Maven |
| `mvn -pl agent package` + `tests/verificar-agente.sh` | Executa o agente real e confere as seções da saída | Bash |
| `tests/verificar-api.sh` | Cada componente do inventário tem estado próprio | Backend em execução |
| `tests/verificar-saude.sh` | Estrutura de `/api/health-check` | Backend em execução |
| `tests/verificar-live.sh` | Estrutura de `/api/live` após algumas coletas | Backend em execução |
| `cd frontend && npm test` | Formatação, serviços, estados da interface, navegação | Node.js 22.12+ |
| `npm run build` | Verificação de tipos e build de produção | Node.js 22.12+ |
| `scripts/testes-documentacao.sh` | Documentos obrigatórios, links internos e marcadores pendentes | Bash |

Os testes automatizados usam **dados fictícios** e não substituem a conferência em hardware real. Para isso, compare
os valores com o Gerenciador de Tarefas e o Gerenciador de Dispositivos (Windows) ou com `lscpu`, `free -h` e `df -h`
(Linux).

## Integração contínua e deploy

- **CI:** não há pipeline configurada neste projeto. Os testes são executados localmente.
- **Deploy:** não há. O PCCheck é executado na máquina do usuário e não publica nada na internet.

## Solução de problemas

Problemas reais encontrados ao executar o projeto do zero no Windows:

| Sintoma | Causa e solução |
|---|---|
| `mvn` (ou `java`) não é reconhecido | O Maven/JDK não está no `PATH` do terminal. Feche e abra o terminal depois de instalar. Para a sessão atual: `$env:Path += ";C:\Tools\apache-maven-3.9.9\bin"` (ajuste o caminho). |
| `Could not find artifact com.pccheck:pccheck-parent:pom` | O pom pai não está no repositório Maven local. Use `scripts/run-backend` ou rode `mvn -N install` e `mvn -pl agent install`. |
| `Port 8080 was already in use` | Já há um backend rodando. Encerre o processo: `netstat -ano \| findstr :8080` e depois `taskkill /PID <número> /F`. |
| `npm error Missing script: "dev"` | O comando foi executado fora da pasta `frontend`. Entre nela com `cd frontend`. |
| `npm test` falha ao iniciar | Node.js abaixo de 22.12. Atualize o Node.js. |
| Tela mostra erro de comunicação com o backend | O backend não está em execução. Confira `http://127.0.0.1:8080/api/ping`. |
| `tests/*.sh` não rodam no Windows | Use o Git Bash ou o WSL. |
| Temperatura, GPU ou ventoinhas "Sem leitura" | Abra o LibreHardwareMonitor como administrador e ative *Options → Remote Web Server → Run*. O sistema não estima o valor. |
