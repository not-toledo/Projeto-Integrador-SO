# API — Game Compatibility (Módulo D)

Base: `http://127.0.0.1:8080/api/games`. As consultas Steam são feitas pelo backend; inventário local não é enviado à Steam.

## Pesquisar jogos

`GET /search?query=nome`

- Query entre 2 e 100 caracteres.
- Retorna até 10 itens: `appId`, `name`, `headerImage`, `storeUrl`.
- Cache de pesquisa: 5 minutos.
- Erros de validação: HTTP 400; indisponibilidade Steam: HTTP 503.

## Requisitos

`GET /{appId}/requirements`

- `appId` deve ser positivo.
- `refresh=true` ignora o cache.
- Retorna texto original mínimo/recomendado, campos extraídos, `fetchedAt`, fonte e avisos.
- Cache de requisitos: 12 horas.

## Compatibilidade

`GET /{appId}/compatibility`

- Obtém os requisitos e coleta novamente o inventário local através do `InventoryService` existente.
- Retorna avaliação independente de mínimos e recomendados, com estado por componente.
- Estados: `COMPATIBLE`, `BELOW_MINIMUM`, `UNKNOWN`, `MANUAL_REVIEW`.
- Um estado desconhecido ou revisão manual nunca é convertido em compatível.

## Limitações e segurança

- O backend monta URLs fixas da Steam a partir do AppID; não aceita URL arbitrária.
- Timeout de conexão/leitura; HTTP 429 e 5xx tratados como indisponibilidade.
- A fonte de dados pode mudar ou limitar chamadas.
- A comparação não estima FPS nem garante desempenho.
