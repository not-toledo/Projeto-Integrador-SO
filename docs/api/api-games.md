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

### Regras de comparação

| Componente | Regra |
|---|---|
| Identificação | Pelo rótulo publicado pela Steam (`Processor:`/`Processador:`, `Memory:`/`Memória:`, `Graphics:`/`Placa de vídeo:`, `Storage:`, `OS:`/`SO:`); palavras-chave só para linhas sem rótulo. DirectX, Rede, Notas adicionais e o aviso "Requires a 64-bit processor" não são comparados (ficam no texto original). |
| RAM | Memória visível ao Windows ≥ requisito → atende. Entre 90% e 100% → revisão manual (parte da RAM costuma ficar reservada para o sistema ou GPU integrada). Abaixo de 90% → abaixo. |
| Armazenamento | Espaço livre do maior volume ≥ requisito. |
| Sistema operacional | Windows: versão numérica local ≥ menor versão citada (`Windows 7/8/10` → 7; Windows 11 atende a "Windows 10"); exigência de 64 bits conferida pela arquitetura. Outros sistemas → revisão manual. |
| CPU e GPU | Atende só quando o modelo detectado é **o mesmo** de um dos modelos publicados (sufixos contam: i5-8400 ≠ i5-8400T, RTX 3060 ≠ RTX 3060 Ti). GPU do mesmo modelo também confere a VRAM. Modelos diferentes ou requisitos sem modelo exato ("Intel Core i5") → revisão manual: o PCCheck não estima desempenho entre modelos. |
| SSD | "SSD required", "Requer SSD" ou "Storage: 70 GB SSD" geram um requisito `SSD`; "SSD recommended", "preferível", "opcional" e "HDD/SSD" não. Atende se há um SSD (tipo informado pelo SO); abaixo se todos os discos internos têm tipo conhecido e nenhum é SSD; senão `UNKNOWN`. Discos USB não contam. |

Resumo (`overallMinimum`/`overallRecommended`): "Abaixo dos ..." se algum item ficou abaixo; "Atende aos ..." se todos
foram confirmados; "Nenhum item abaixo dos ...; revisar manualmente: CPU, GPU" quando parte foi confirmada; e
"Análise incompleta" quando nada pôde ser confirmado.

### Classificação geral (`verdict`)

| `code` | `label` | Quando |
|---|---|---|
| `NOT_RECOMMENDED` | Não recomendado | Algum item ficou comprovadamente abaixo do mínimo (`BELOW_MINIMUM`). |
| `COMPATIBLE` | Compatível | Todos os itens dos mínimos e dos recomendados foram confirmados, ou a Steam não publicou recomendados. |
| `LIMITED` | Compatibilidade limitada | Mínimos confirmados; recomendados abaixo ou não verificáveis. |
| `INSUFFICIENT_DATA` | Dados insuficientes | Nenhum item abaixo do mínimo, mas algum item dos mínimos não pôde ser confirmado (ou não há mínimos publicados). |

Campos: `reasons` (motivos em texto), `belowMinimum` e `belowRecommended` (componentes comprovadamente abaixo) e
`pendingReview` (componentes sem confirmação). Falta de dado nunca vira compatível nem incompatível.

`warnings` traz só limitações que afetaram o jogo consultado (por exemplo, CPU/GPU em revisão manual). O aviso de que a
avaliação não garante FPS fica no rodapé fixo da interface.

## Limitações e segurança

- O backend monta URLs fixas da Steam a partir do AppID; não aceita URL arbitrária.
- Timeout de conexão/leitura; HTTP 429 e 5xx tratados como indisponibilidade.
- A fonte de dados pode mudar ou limitar chamadas.
- A comparação não estima FPS nem garante desempenho.
