# Documentação de APIs — PCCheck

O backend expõe uma API REST **local**, que escuta somente em `http://127.0.0.1:8080`. Não há autenticação porque
não há acesso remoto. Todas as rotas começam com `/api`.

## Endpoints

| Método | Rota | Descrição | Detalhes |
|---|---|---|---|
| GET | `/api/ping` | Verifica se o backend está ativo. Resposta: `{"status":"UP","app":"PCCheck","version":"0.9.0"}`. | — |
| GET | `/api/inventory` | Inventário de hardware (Módulo A). | [api/api-inventario.md](api/api-inventario.md) |
| GET | `/api/health-check` | Relatório do Health Check (Módulo B). | [api/api-saude.md](api/api-saude.md) |
| GET | `/api/live` | Amostra mais recente do Live Monitor e estado da coleta (Módulo C). | [api/api-live.md](api/api-live.md) |
| GET | `/api/live/history?limit=N` | Histórico recente de amostras (Módulo C). | [api/api-live.md](api/api-live.md) |

## Convenções de resposta

- **Estado por componente:** cada grupo de dados tem `status` (`OK`, `UNAVAILABLE` ou `ERROR`), `message` e `data`.
- **`UNAVAILABLE`:** o sistema não forneceu a informação. Não indica defeito.
- **`ERROR`:** a coleta daquele grupo falhou. As demais informações continuam válidas.
- **`null` dentro de `data`:** sem leitura. Nenhum valor é estimado ou preenchido.
- **Horários:** em ISO 8601 (UTC).

## Documentos de API

- [Inventário (Módulo A)](api/api-inventario.md)
- [Health Check (Módulo B)](api/api-saude.md)
- [Live Monitor (Módulo C)](api/api-live.md)
- [GPU e sensores via LibreHardwareMonitor](libre-hardware-monitor.md)

## Configuração

Os parâmetros ficam em `backend/src/main/resources/application.properties` (prefixos `pccheck.health.*`,
`pccheck.monitor.*` e `pccheck.lhm.*`).
