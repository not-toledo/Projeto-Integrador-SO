# Documentação de APIs — PCCheck

O backend expõe uma API REST local em `http://127.0.0.1:8080`. Todas as rotas começam com `/api`.

## Endpoints

| Método | Rota | Descrição | Detalhes |
|---|---|---|---|
| GET | `/api/ping` | Verifica se o backend está ativo. | — |
| GET | `/api/inventory` | Inventário de hardware (Módulo A). | [api-inventario.md](api/api-inventario.md) |
| GET | `/api/health-check` | Relatório do Health Check (Módulo B). | [api-saude.md](api/api-saude.md) |
| GET | `/api/live` | Amostra mais recente do Live Monitor (Módulo C). | [api-live.md](api/api-live.md) |
| GET | `/api/live/history?limit=N` | Histórico recente (Módulo C). | [api-live.md](api/api-live.md) |
| GET | `/api/history?days=N` | Histórico da coleta agendada (dias). | [api-historico.md](api/api-historico.md) |
| GET | `/api/games/search?query=...` | Pesquisa jogos na Steam (Módulo D). | [api-games.md](api/api-games.md) |
| GET | `/api/games/{appId}/requirements` | Requisitos Steam mínimos/recomendados (Módulo D). | [api-games.md](api/api-games.md) |
| GET | `/api/games/{appId}/compatibility` | Comparação com hardware local (Módulo D). | [api-games.md](api/api-games.md) |

## Convenções

- **Estado por componente:** `OK`, `UNAVAILABLE` ou `ERROR`.
- **`UNAVAILABLE`:** o sistema não forneceu a informação; não indica defeito.
- **`ERROR`:** a coleta daquele grupo falhou; os demais grupos continuam válidos.
- **`null`:** dado indisponível; nenhum valor é estimado.
- **Horários:** ISO 8601.
- **Módulo D:** estados de comparação `COMPATIBLE`, `BELOW_MINIMUM`, `UNKNOWN` e `MANUAL_REVIEW`. Desconhecido não é compatível.

## Documentos

- [Inventário (Módulo A)](api/api-inventario.md)
- [Health Check (Módulo B)](api/api-saude.md)
- [Live Monitor (Módulo C)](api/api-live.md)
- [Histórico (coleta agendada)](api/api-historico.md)
- [Game Compatibility (Módulo D)](api/api-games.md)
- [GPU e sensores (leitor de sensores interno)](sensores.md)

## Configuração

Os parâmetros ficam em `backend/src/main/resources/application.properties` nos prefixos `pccheck.health.*`,
`pccheck.monitor.*`, `pccheck.sensors.*` e `pccheck.steam.*`.
