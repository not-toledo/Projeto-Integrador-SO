# Plano incremental — Módulo D: Game Compatibility

O Módulo D consulta os requisitos públicos da Steam pelo backend e compara os dados interpretáveis com o inventário local do Módulo A. O inventário nunca é enviado à Steam.

## V1.1 — Integração de pesquisa Steam
- `SteamStoreClient`: chamadas HTTP com timeout, User-Agent e tratamento de HTTP 429, 5xx, JSON inválido e interrupção.
- `SteamGameSearchService`: validação de 2–100 caracteres, limite de 10 resultados e cache de pesquisa por 5 minutos.
- `GET /api/games/search?query=...`: retorna AppID, nome, imagem e URL oficial construída pelo backend.
- Testes automatizados usam JSON controlado e cobrem URL de pesquisa, HTTP 429, 5xx e JSON inválido. O timeout de conexão/leitura é configurado no cliente; precisa de validação integrada.

**Critério:** pesquisa real retorna AppID e nome; falha externa não derruba os módulos A–C.

## V1.2 — Obtenção e parsing de requisitos
- `SteamRequirementsService`: consulta `appdetails` por AppID positivo, valida `success=true` e `type=game`, mantém cache por 12 horas e aceita atualização manual.
- `SteamRequirementsParser`: remove HTML preservando linhas, rótulos e texto original; reconhece RAM, armazenamento, sistema operacional, CPU e GPU.
- Campos interpretados guardam texto original, valor normalizado, confiança e estado do parsing.
- `GET /api/games/{appId}/requirements?refresh=false`.

**Critério:** requisitos de jogos reais são apresentados; ausências e texto não reconhecido ficam explícitos.

## V1.3 — Motor de compatibilidade
- `GameCompatibilityEvaluator` reutiliza `InventoryService`; não cria outro coletor.
- RAM, VRAM e espaço livre são comparados numericamente quando ambos os lados são interpretáveis.
- CPU/GPU só são aprovados para modelos equivalentes no catálogo explícito; VRAM é comparada se informada nos dois lados. SO é aprovado apenas quando a versão/plataforma extraída corresponde; não há benchmark inventado.
- Estados: `COMPATIBLE`, `BELOW_MINIMUM`, `UNKNOWN` e `MANUAL_REVIEW`.
- Mínimos e recomendados são avaliados separadamente.
- `GET /api/games/{appId}/compatibility`.

**Critério:** desconhecido nunca é compatível; cada resultado informa requisito, dado detectado e explicação.

## V1.4 — Interface e integração final
- Nova aba Compatibilidade com jogos.
- Pesquisa com debounce, seleção por AppID, resultado comparativo, requisitos originais, fonte, horário, avisos e atualização.
- Estados de carregamento, erro e resultado vazio.
- Testes e documentação da API atualizados.

**Critério:** fluxo ponta a ponta validado com jogos reais e sem regressões nos módulos A–C.

## Limitações da implementação inicial
- O parser é heurístico: requisitos escritos em formatos incomuns podem ficar sem interpretação.
- CPU/GPU só recebem aprovação automática para os modelos cobertos pelo catálogo inicial; SO só quando a versão extraída corresponde exatamente. Demais casos exigem revisão manual.
- Armazenamento usa o maior espaço livre de um volume individual, nunca soma partições.
- A Steam Store é uma interface pública sem garantia de estabilidade/SLA para terceiros.
- Compatibilidade não significa FPS, estabilidade ou desempenho garantidos.
