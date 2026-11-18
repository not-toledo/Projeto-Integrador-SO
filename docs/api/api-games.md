# API — Game Compatibility (Módulo D)

Base: `http://127.0.0.1:8080/api/games`. As consultas Steam são feitas pelo backend; inventário local não é enviado à Steam.

## Pesquisar jogos

`GET /search?query=nome`

- Query entre 2 e 100 caracteres.
- Retorna até 10 itens: `appId`, `name`, `headerImage`, `storeUrl`.
- Cache de pesquisa: 5 minutos.
- Erros de validação: HTTP 400; indisponibilidade Steam: HTTP 503.

## Jogos instalados

`GET /installed`

- Lê, só localmente, os arquivos da Steam: `steamapps/libraryfolders.vdf` (bibliotecas) e `steamapps/appmanifest_<AppID>.acf` (um por jogo). Nada é enviado à internet nem alterado nas pastas da Steam.
- Pasta da Steam: `PCCHECK_STEAM_DIR`, se definida; no Windows, o registro (`HKCU\Software\Valve\Steam\SteamPath`, depois `HKLM\SOFTWARE\WOW6432Node\Valve\Steam\InstallPath`) e `C:\Program Files (x86)\Steam`; no Linux, `~/.steam/steam`, `~/.local/share/Steam` e a pasta do Flatpak.
- Resposta: `steamFound`, `steamPath`, `libraries` (`path`, `mountPoint`, `freeBytes`, `totalBytes`, `mediaType`, `available`, `gameCount`), `games` (`appId`, `name`, `installDir`, `libraryPath`, `mountPoint`, `mediaType`, `sizeOnDiskBytes`, `state`, `lastUpdated`, `storeUrl`), `problems` e `scannedAt`.
- `state` vem do `StateFlags` do manifesto: `INSTALLED`, `UPDATE_REQUIRED` (instalado, atualização pendente) ou `INCOMPLETE` (download ou instalação não concluídos).
- `mediaType` (SSD/HDD) é o do disco físico que contém a biblioteca, ligado pelos volumes das partições.
- Campos pessoais do manifesto, como o ID da conta (`LastOwner`), não são lidos.
- Manifesto inválido entra em `problems`, sem esconder os demais. Steam não encontrada: `steamFound=false` e a orientação em `problems`.
- A lista inclui ferramentas da Steam (ex.: Steamworks Common Redistributables), que não têm requisitos de jogo.

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
| CPU e GPU | Mesmo modelo de um dos publicados → atende (sufixos contam: i5-8400 ≠ i5-8400T, RTX 3060 ≠ RTX 3060 Ti); GPU do mesmo modelo também confere a VRAM. Modelos diferentes → comparação por benchmark (abaixo), quando possível. Requisito sem modelo exato ("Intel Core i5") ou sem dado comparável → revisão manual. |
| SSD | "SSD required", "Requer SSD" ou "Storage: 70 GB SSD" geram um requisito `SSD`; "SSD recommended", "preferível", "opcional" e "HDD/SSD" não. Atende se há um SSD (tipo informado pelo SO); abaixo se todos os discos internos têm tipo conhecido e nenhum é SSD; senão `UNKNOWN`. Discos USB não contam. |

Resumo (`overallMinimum`/`overallRecommended`): "Abaixo dos ..." se algum item ficou abaixo; "Atende aos ..." se todos
foram confirmados; "Nenhum item abaixo dos ...; revisar manualmente: CPU, GPU" quando parte foi confirmada; e
"Análise incompleta" quando nada pôde ser confirmado.

### Comparação de CPU e GPU por benchmark

Fonte: [Blender Open Data](https://opendata.blender.org) (licença CC0), tabela `backend/src/main/resources/benchmarks/blender-opendata.csv` gerada por `scripts/atualizar-benchmarks` (sem internet durante o uso). Pontuação = soma das amostras por minuto das cenas monster, junkshop e classroom (a mesma do site); por modelo, backend e versão do Blender: mediana, quartis e quantidade de execuções (mínimo 3).

| Regra | Motivo |
|---|---|
| Só compara pontuações da **mesma versão do Blender e do mesmo backend** (CPU, OptiX, CUDA, HIP, oneAPI); entre as possíveis, a com mais execuções do lado com menos. | Versões usam cenas diferentes; backends não são comparáveis. |
| Usa primeiro a alternativa publicada **do mesmo fabricante** ("GTX 1060 / RX 580": placa NVIDIA → GTX 1060). | A equivalência é a declarada pelo desenvolvedor; evita comparar fabricantes. |
| GPU só com GPU da **mesma geração de arquitetura** (ex.: Maxwell/Pascal, Turing, Ampere, RDNA 2). | Gerações novas ganham muito mais na renderização que nos jogos (ex.: RTX 2060 ≈ 5× a GTX 1050 Ti no Blender, ≈ 2,5× em jogos). |
| GTX × RTX não usa o OptiX (só o CUDA). | No OptiX, os núcleos de ray tracing das RTX aceleram a renderização, não os jogos. |
| Execuções de notebook ("Laptop", "Mobile", "Max-Q"), com mais de uma GPU, com mais de um soquete e placas fora do backend nativo (ex.: AMD em CUDA via ZLUDA) são descartadas. | Mesmo nome, outra potência ou configuração fora do padrão. |

Decisão pela razão local ÷ exigido (heurística do projeto): confiança **alta** (10+ execuções de cada lado e intervalo interquartil até 25% da mediana) atende com ≥ 1,0 e fica abaixo com < 0,85; confiança **média**, com ≥ 1,15 e < 0,70. Entre as margens: revisão manual. CPU: a renderização usa todos os núcleos e os jogos dependem mais do desempenho por **núcleo físico** (não por thread, para não penalizar Hyper-Threading/SMT): total maior mas por núcleo abaixo de 85% → revisão manual; total menor mas por núcleo maior → revisão manual. GPU com desempenho suficiente e VRAM detectada menor que a publicada → revisão manual.

Cada resultado comparado traz `benchmark` (`localModel`, `requiredModel`, `localScore`, `requiredScore`, `localSamples`, `requiredSamples`, `backend`, `blenderVersion`, `ratio`, `confidence`, `source`). A pontuação mede renderização, não desempenho em jogos.

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

## Pré-checagem antes de jogar

`GET /{appId}/precheck`

Avalia o estado do PC **agora** para o jogo (a compatibilidade compara o hardware instalado). Leva cerca de 1 s (duas leituras de processos). Cada verificação tem `status` próprio: `OK`, `ATTENTION`, `INFO` (só informa) ou `UNAVAILABLE` (não verificado, nunca tratado como ok). `overall` é `ATTENTION` se alguma verificação for `ATTENTION`; senão `READY`.

| `id` | Fonte | Regra (heurísticas do projeto) |
|---|---|---|
| `ram` | Memória disponível (OSHI) e RAM mínima publicada | `OK` se disponível ≥ mínimo; `INFO` entre 50% e 100% do mínimo; `ATTENTION` abaixo de 50%. Sem requisito interpretável ou sem Steam: `INFO`, sem comparar. |
| `cpu` | Uso total da CPU entre duas leituras (~1 s) | `ATTENTION` a partir de 50%, citando os 3 aplicativos que mais usam CPU. |
| `install` | Manifesto da Steam (`StateFlags`) | `ATTENTION` com atualização pendente ou instalação incompleta; `INFO` se o jogo não está instalado. |
| `game-disk-space` | Volume da biblioteca do jogo | `ATTENTION` com espaço livre abaixo do limite do Health Check (`pccheck.health.disk-free-warning-percent`). |
| `game-ssd` | Requisito "SSD" e tipo do disco do jogo | Só quando os requisitos pedem SSD: `ATTENTION` se o jogo está num HDD; `UNAVAILABLE` se o SO não informa o tipo. |
| `power` | Windows: `powercfg /getactivescheme` (GUID dos planos padrão); Linux: `/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor` | `ATTENTION` no plano Economia de energia; Equilibrado e Alto desempenho `OK`; planos personalizados e o governador do Linux só `INFO`. |
| `battery` | Fontes de energia (OSHI) | `ATTENTION` só se uma bateria informa que está descarregando (desktops podem informar uma bateria fictícia, que não conta). |

`topCpu` e `topMemory`: até 5 aplicativos (processos com o mesmo nome somados), sem o processo ocioso e sem o próprio PCCheck. CPU em % do total (carga do OSHI dividida pelos processadores lógicos); memória é a soma do conjunto de trabalho (RSS), em que páginas compartilhadas podem ser contadas mais de uma vez. O PCCheck não encerra nem altera processos. Sem leitura de processos: `processError` com o motivo.

## Limitações e segurança

- O backend monta URLs fixas da Steam a partir do AppID; não aceita URL arbitrária.
- Timeout de conexão/leitura; HTTP 429 e 5xx tratados como indisponibilidade.
- A fonte de dados pode mudar ou limitar chamadas.
- A comparação não estima FPS nem garante desempenho.
