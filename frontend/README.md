# PCCheck — Frontend

Interface web do PCCheck (React 19 + TypeScript, empacotada com Vite). Mostra os três módulos: **Hardware Overview**, **Health Check** e **Live Monitor**, consumindo a API local do backend. Visão geral do projeto no [README principal](../README.md).

## Requisitos

- Node.js **22.22.2 ou superior** (o `npm run dev` aceita 20.19+, mas as dependências de teste atuais exigem 22.22.2+).
- Backend em execução em `http://127.0.0.1:8080` (`scripts/run-backend.sh` ou `scripts\run-backend.bat`, na raiz do projeto).

## Comandos

Execute dentro desta pasta (`frontend/`):

| Comando | O que faz |
|---|---|
| `npm install` | Instala as dependências (necessário na primeira vez). |
| `npm run dev` | Inicia o servidor de desenvolvimento (normalmente em `http://localhost:5173`). |
| `npm test` | Executa os testes automatizados (Vitest + Testing Library). |
| `npm run build` | Verifica os tipos (`tsc`) e gera a versão de produção em `dist/`. |
| `npm run lint` | Executa o Oxlint. |

## Comunicação com o backend

- Em desenvolvimento e no `preview`, o Vite encaminha `/api` para `http://127.0.0.1:8080` (ver `vite.config.ts`), então não há configuração de CORS.
- Para usar outro endereço de API, defina a variável `VITE_API_BASE`.
- Hardware Overview e Health Check atualizam sob demanda, pelos botões **Atualizar** e **Reavaliar**. O Live Monitor atualiza sozinho, seguindo o intervalo de coleta do backend (1 a 10 s).

## Estrutura de `src/`

```
src/
├── components/        # componentes visuais compartilhados
├── modules/           # lógica pura e componentes por módulo
│   ├── hardware/      # formatação do Hardware Overview
│   ├── health/        # rótulos do Health Check
│   └── live/          # séries, gráficos SVG e formatação do Live Monitor
├── pages/             # uma página por aba (HardwareOverview, HealthCheck, LiveMonitor)
├── services/          # chamadas à API (inventory, health, live) com validação das respostas
└── App.tsx            # navegação entre as abas
```

## Solução de problemas

- `npm error Missing script: "dev"`: o comando foi executado fora desta pasta. Use `cd frontend`.
- A tela mostra erro de comunicação: o backend não está rodando. Confira `http://127.0.0.1:8080/api/ping`.
