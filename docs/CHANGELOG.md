# Histórico de versões

## V4.0.0 — NEXCORE (nova identidade)
- O produto passou a se chamar **NEXCORE** (antes: PCCheck). Renomeados: interface (título, logotipo, textos), `/api/ping` (`"app":"NEXCORE"`), mensagens dos scripts, documentação, artefatos Maven (`nexcore-parent`, `nexcore-agent`, `nexcore-backend`) e npm (`nexcore-frontend`), leitor de sensores (`NEXCORE.SensorHost.exe`, nome que aparece no aviso do Windows), tarefa agendada (`NEXCORE\Coleta`), pipe (`nexcore-sensors-*`) e chaves de configuração (`nexcore.*`).
- Mantidos de propósito (internos, sem efeito para o usuário): o pacote Java `com.pccheck` e nomes de classes como `PcCheckAgent`. As entradas antigas deste histórico continuam com o nome da época.
- Compatibilidade: variáveis `PCCHECK_DATA_DIR`, `PCCHECK_RETENCAO_DIAS` e `PCCHECK_STEAM_DIR` continuam valendo quando a versão `NEXCORE_*` não está definida. Na primeira execução, as coletas de `%LOCALAPPDATA%\PCCheck` (ou `~/.local/state/pccheck`) são **copiadas** para a pasta do NEXCORE, sem apagar a antiga. `agendar-coleta remover` também remove a tarefa antiga `PCCheck\Coleta`.
- Logotipo oficial em `branding/nexcore-logo.png`; `branding/gerar-recursos.ps1` gera ícone (`nexcore.ico`, 16 a 256 px), PNGs, favicon e imagens do instalador, recortando só a margem preta para o símbolo ficar legível em tamanhos pequenos.
- Versão 4.0.0 em todos os módulos.
- Testes: variáveis antigas e migração das coletas (`HistoryStoreTest`).

## V3.2 — Quem é melhor, sem precisar interpretar números
- Novo estado `NEAR_REQUIRED` ("Próximo do exigido"): benchmark com confiança alta e 85% a 99% do exigido. Antes ia para "Revisão manual"; agora diz que o desempenho é parecido e que o jogo deve rodar com pouca folga, e a classificação geral fica "Compatibilidade limitada" em vez de "Dados insuficientes". Com o desempenho por núcleo também abaixo de 85%, continua revisão manual.
- Toda comparação por benchmark começa com uma frase direta: "Seu processador rende 94% do exigido (Intel Core i5-8400): 6% a menos", "é 62% mais rápido" ou "praticamente iguais" (diferença < 3%). Os motivos da classificação geral mostram só essa frase.
- Interface: duelo do benchmark em cada linha (seu × exigido, mesma escala, vencedor destacado e manchete "O exigido é melhor..."/"O seu ... é melhor"); detalhes técnicos em "Como foi comparado". "Revisão manual" passou a neutro (cinza) e "Próximo do exigido" usa o âmbar. Na tabela dos recomendados, "Abaixo do recomendado" no lugar de "Abaixo do mínimo".
- Validado com Elden Ring nesta máquina: i7-4790K × i5-8400 = 94% → "Próximo do exigido".
- Testes: `BenchmarkComparisonTest` (próximo do exigido e frase direta), `duel.test.ts` e testes da tela de jogos.
- Rodapé: o atalho "Histórico" deu lugar a "Processos". A página do Histórico (coleta agendada) segue no endereço `#/history`.

## V3.1 — Processos
- `GET /api/processes`: processos em execução lidos pelo OSHI em duas leituras com 1 s de intervalo. CPU por processo = tempo de CPU ganho ÷ tempo decorrido ÷ processadores lógicos (% da CPU inteira); processo novo ou PID reaproveitado fica sem medição (`null`), nunca zero. Processo ocioso (PID 0) omitido; o próprio backend vem marcado. Leituras seguidas em menos de 2 s são reaproveitadas. Somente leitura: não há rota para encerrar ou alterar processos.
- Tela **Processos** (no lugar do Histórico no menu; o Histórico continua em `#/history` e no rodapé): totais de processos e threads, maiores consumos de CPU e memória, tabela com filtro, agrupamento por nome, ordenação por coluna e atualização a cada 5 s; prioridade com nome (Windows: classe pela prioridade base; Linux: nice); quadro "O que estes dados significam".
- Limites mostrados na tela: no Windows o estado é sempre "em execução" (coluna Estado só no Linux) e o dono de processos do sistema exige administrador.
- Validado nesta máquina: 262 processos, 3.397 threads; resposta em cerca de 1,5 s.
- Testes: `ProcessSamplerTest` (backend), `table.test.ts` e `ProcessesPage.test.tsx` (frontend), `tests/verificar-processos.sh`.

## V3.0 — Nova interface
- Identidade visual escura e minimalista: preto, cinzas e uma cor de destaque (verde) só para estados; divisórias finas tracejadas; fonte pixelada [Silkscreen](https://github.com/googlefonts/silkscreen) (licença OFL, incluída em `frontend/public/fonts/`, sem acesso à internet) apenas em títulos. O tema claro foi removido.
- Cabeçalho fixo com as 6 telas e botão MENU em telas estreitas; cada tela tem endereço próprio (`#/hardware`, `#/live`...), sem biblioteca de rotas, e o botão Voltar do navegador funciona. Rodapé com atalhos, GitHub e créditos.
- Nova tela **Início** (abre por padrão): apresentação, "Seu computador" (inventário real e números da máquina), funcionalidades, "Agora" (CPU, RAM, GPU e temperaturas ao vivo com minigráficos, "Sem leitura" quando falta sensor), "Vai rodar?" (passos e classificações do Módulo D) e "Sobre o projeto". Nenhum valor demonstrativo: tudo vem da API.
- Ilustrações próprias em SVG isométrico (processador e camadas do sistema), geradas no código.
- Telas internas com cabeçalho padrão (módulo, título, coleta e ações); cards, tabelas, gráficos, avisos e estados de carregamento no novo padrão. Serviços, regras e dados sem alteração.
- Acessibilidade: atalho "Pular para o conteúdo", foco visível, campos com contraste 3:1, animações desligadas com "reduzir movimento".
- `tests/verificar-live.sh`: a GPU pode ter leitura real desde o leitor de sensores (V1.5); a verificação aceita leitura real ou indisponível e continua rejeitando outro estado.
- Testes: navegação por endereço e menu (`App.test.tsx`, `navigation.test.ts`) e tela Início (`HomePage.test.tsx`).

## V2.9 — Comparação de CPU e GPU por benchmark
- Tabela `benchmarks/blender-opendata.csv` (Blender Open Data, CC0; 6.394 linhas, snapshot de 2026-11-18): mediana, quartis, execuções e núcleos por modelo, backend e versão do Blender. Gerada por `BlenderDatasetBuilder` (reaproveita as regras de `HardwareModels`) via `scripts/atualizar-benchmarks.sh/.bat`, que param se o backend estiver aberto.
- `BenchmarkCatalog` + `GameCompatibilityEvaluator`: CPU/GPU de modelos diferentes passam a ser comparadas pela alternativa do mesmo fabricante, na mesma versão e backend, com confiança alta/média e margens próprias; resultado com o campo `benchmark` e a fonte na interface. Sem dado comparável, continua revisão manual.
- Proteções conferidas com dados reais: GPUs só dentro da mesma geração (no Blender a RTX 2060 tem ≈ 5× a GTX 1050 Ti; em jogos, ≈ 2,5×); GTX × RTX fora do OptiX (núcleos de ray tracing); CPU por núcleo físico (por thread, o i7-4790K saía "mais fraco por núcleo" que o i5-6600 por causa do Hyper-Threading); AMD em CUDA (ZLUDA) e notebooks descartados.
- Validado nesta máquina (i7-4790K, GTX 1050 Ti): GTX 1050 Ti × GTX 1060 = 59% (abaixo), × GTX 970 = 83% (abaixo), × GTX 950 = 182% (atende); i7-4790K × i5-6600 = 162% (atende), × i7-7700K = 87% (revisão).
- `HardwareModels` e o construtor do catálogo com CSV passaram a ser públicos.
- Testes: `BenchmarkTest` (gerador e catálogo) e `BenchmarkComparisonTest` (decisões), teste da fonte na tela de jogos.

## V2.8 — Jogos instalados e pré-checagem antes de jogar
- Agente: `DiskInfo.mountPoints` liga cada volume ao disco físico (OSHI: WMI `Win32_DiskDriveToDiskPartition`/`Win32_LogicalDiskToPartition`; Linux: udev e `/proc/mounts`). Card de armazenamento mostra "Volumes neste disco".
- `GET /api/games/installed`: jogos da Steam lidos de `libraryfolders.vdf` e `appmanifest_*.acf` (leitor VDF próprio, sem dependência nova), com volume, espaço livre e tipo SSD/HDD de cada biblioteca e estado da instalação. Pasta da Steam pelo registro, pastas padrão ou `PCCHECK_STEAM_DIR`. O ID da conta Steam não é lido.
- `GET /api/games/{appId}/precheck`: memória disponível agora × RAM mínima, uso de CPU e aplicativos que mais usam CPU e memória, atualização pendente, espaço no disco do jogo, jogo que pede SSD instalado num HDD, plano de energia e bateria.
- Interface: bloco "Jogos instalados neste PC (Steam)" com "Verificar compatibilidade" e painel "Pré-checagem antes de jogar" na comparação.
- `CommandRunner` (execução de comandos fixos com tempo limite) extraído do `SchedulerStatus` e reaproveitado pelo `powercfg`. `Formatting` passou a ser público.
- Validado nesta máquina: 2 bibliotecas (C: SSD e D: HDD), 15 itens com tamanhos iguais aos da Steam; bateria fictícia do desktop ("System Battery", sem descarga) corretamente ignorada.
- Testes: `InstalledGamesScannerTest`, `PrecheckTest` (backend), `InstalledGames.test.tsx`, `PrecheckPanel.test.tsx` e volumes no teste do card de armazenamento; novo `tests/verificar-jogos.sh` e `mountPoints` em `tests/verificar-api.sh`.

## V2.7.1 — Correções
- `run-backend.bat`/`.sh`: param com mensagem clara se a porta 8080 já estiver em uso, **antes** de reinstalar o agente. Antes, rodar o script com outro backend aberto trocava o jar do agente embaixo do backend em execução (erro 500 no Histórico: `NoClassDefFoundError`) e o novo backend falhava por porta em uso.
- Saúde dos discos: texto e limites citam só os indicadores que o disco informou (o alerta do HDD dizia "a vida útil e a temperatura" e mostrava limite de vida útil); disco sem vida útil nem temperatura passa a "não avaliado" em vez de "normal".
- Validado em hardware real com administrador: `smart:0` SSD (vida útil 70%, 48 °C) e `smart:1` HDD (35 °C, 50.475 h ligado).
- Revisão de código sem uso (classes, métodos, construtores, exports, dependências, arquivos e classes CSS): removida a constante `NOT_SUPPORTED` do Live Monitor, que não era usada. O restante está em uso (classes do Spring são carregadas por anotação; construtores curtos de compatibilidade servem aos testes).

## V2.7 — Saúde dos discos e tipo SSD/HDD
- Agente: tipo de mídia e barramento de cada disco informados pelo SO (Windows: WMI `MSFT_PhysicalDisk`, sem administrador; Linux: `/sys/block/<disco>/queue/rotational`, ignorando removíveis). Nunca deduzido do nome do modelo; pen drives ficam sem tipo.
- Leitor de sensores: `IsStorageEnabled` ligado e campo `id` por hardware (ex.: `/ssd/0`), que liga a leitura ao disco físico. O parser Java lê temperatura, vida útil restante, dados gravados, horas ligado e quantidade de vezes ligado (nomes conferidos na LibreHardwareMonitorLib 0.9.6 e na saída real).
- Health Check: o aviso fixo "SMART não suportado" foi substituído pelo `DiskHealthDiagnostic`: um alerta por disco (`smart:<n>`), com vida útil (Atenção ≤ 20%, Crítico ≤ 5%) e temperatura (Atenção ≥ 55 °C em HDD, ≥ 70 °C em SSD). "Indicadores normais" deixa claro que setores realocados e erros de leitura não são lidos. Sem leitura, `smart` informa o motivo. Limites em `pccheck.health.disk.*`.
- Módulo D: "SSD required" passa a ser comparado com os discos reais (antes era ignorado).
- Interface: campo "Tipo" no card de armazenamento, com a fonte em "De onde vêm estes dados?".
- Requisitos RF20 e RN08 atualizados (SMART deixou de ser "não suportado").
- Testes: `DiskHealthTest` (agente, com a saída real do leitor), `DiskHealthDiagnosticTest`, testes de SSD no parser e no avaliador, e teste do card de armazenamento.

## V2.6 — Coleta agendada e histórico
- Agente: modo `--registrar-coleta` (uma coleta gravada no histórico, código de saída para o agendador) e `--onde` (pasta do histórico). Pacote `history`: JSON Lines com rotação diária, retenção (7 dias, `PCCHECK_RETENCAO_DIAS`) que só apaga arquivos do próprio histórico, gravação sob trava exclusiva de arquivo e, no Linux, pasta `700` e arquivos `600` (sem alterar pastas existentes). Pasta por `PCCHECK_DATA_DIR` ou padrão do SO.
- Scripts `agendar-coleta.bat` (Agendador de Tarefas, `schtasks`, via `javaw` sem janela) e `agendar-coleta.sh` (`cron` do usuário, sem duplicar a linha e preservando as demais): `instalar [minutos]`, `remover`, `status`, `agora`. O agente é copiado para a pasta de dados, então a tarefa sobrevive a `mvn clean`. Não exigem administrador.
- Backend: `GET /api/history?days=N` (lê os mesmos arquivos com o mesmo código do agente) e situação da tarefa consultando `schtasks`/`crontab` com tempo limite.
- Interface: página **Histórico** com CPU, RAM, paginação, memória comprometida e espaço livre por volume ao longo dos dias; lacunas quando não houve coleta. `LineChart` aceita rótulos de data.
- `MiniJson` (leitor JSON do agente) passou a ser público para ser reaproveitado pelo histórico.
- Testes: `HistoryStoreTest` (agente), `HistoryServiceTest` e `SchedulerStatusTest` (backend), `HistoryPage.test.tsx`. Script do Linux validado com `crontab` simulado; no Windows, `agora`, `status` e validação de argumentos foram executados, mas a criação real da tarefa (`instalar`) não foi executada nesta validação.

## V2.5 — De onde vêm os dados
- Cada card da visão geral e cada painel do Live Monitor ganhou "De onde vêm estes dados?": a interface do sistema operacional que fornece cada campo, no Windows e no Linux (registro, WMI, contadores de desempenho, APIs do Kernel32/Psapi/PowrProf, `/proc`, `/sys`, `lspci`, `lshw`, `lsblk`). Na visão geral, a coluna do SO deste computador é destacada. Aparece também quando a coleta falha.
- Fontes conferidas no bytecode do OSHI 6.6.5, no agente e no leitor de sensores; campos sem suporte aparecem como tal (ex.: telemetria da GPU no Linux). Catálogo único em `frontend/src/modules/sources/sourceCatalog.ts`.
- Testes: `sourceCatalog.test.tsx` (cobertura de todos os campos exibidos, destaque da plataforma e exibição com falha de coleta).

## V2.4 — Pressão de memória e paginação
- Agente: `MemoryInfo` ganhou `virtualMemory` (arquivo de paginação/swap e memória comprometida em relação ao limite) e, no Live Monitor, `paging` (páginas lidas/gravadas por segundo, pela diferença entre amostras). Dados do OSHI, sem dependência nova.
- Health Check: novo `MemoryPressureDiagnostic`. `memory:commit` avisa com memória comprometida a partir de 90% do limite no Windows (no Linux é informativo, por causa do overcommit). `memory:paging` avisa somente quando a média de páginas gravadas no último minuto passa de 1 MiB/s **e** a RAM está acima do limite de aviso — um pico isolado ou paginação sem RAM cheia não geram aviso. Limites configuráveis (`pccheck.health.memory-pressure.*`).
- Interface: card de RAM com paginação/swap e memória comprometida; Live Monitor com memória comprometida, taxa de paginação e gráfico de páginas gravadas.
- Testes: `MemoryPressureCollectorsTest` (agente), `MemoryPressureDiagnosticTest` (backend) e testes de frontend.
- **Correção (Linux):** o OSHI 6.6.5 informa como "memória virtual em uso" a soma de RAM usada e swap usado, que não é memória comprometida. No Linux o agente passa a ler `Committed_AS` diretamente de `/proc/meminfo`; sem leitura, o campo fica não disponível. No Windows nada muda (`CommitTotal` de `GetPerformanceInfo`).

## V2.3 — Classificação geral da compatibilidade com jogos
- Nova classificação geral (`verdict`) por jogo: **Compatível**, **Compatibilidade limitada**, **Não recomendado** e **Dados insuficientes**, com código estável, motivos e listas de componentes abaixo do mínimo, abaixo do recomendado e pendentes de confirmação. Falta de dado nunca vira compatível nem incompatível. Campo aditivo; `overallMinimum`/`overallRecommended` mantidos.
- Interface: faixa com a classificação e os motivos acima das tabelas; estado `UNKNOWN` passa a ser exibido como "Dados insuficientes".
- Avisos: o aviso sobre FPS saiu da lista de limitações (já estava no rodapé fixo, ficava duplicado); o aviso de CPU/GPU só aparece quando algum desses itens ficou em revisão manual.
- Parser: jogos antigos que publicam os requisitos em um parágrafo ("Mínimo: Processador de 1.7 GHz, 512 MB de RAM, ...") passam a gerar um campo por item (vírgulas dentro de parênteses são respeitadas); o bloco "Recomendado:" publicado dentro do campo dos mínimos é separado; `&reg;`, `&trade;` e `&copy;` são decodificados; "processador" e "placa de vídeo" reconhecidos em linhas sem rótulo. Ex.: Portal (AppID 400) passou de nenhum requisito comparável para CPU, RAM e GPU em mínimos e recomendados.
- Testes: `GameCompatibilityEvaluatorTest` (8 cenários de classificação), `SteamRequirementsParserTest` e `SteamRequirementsServiceTest` (HTML real do Portal) e `GameCompatibilityPage.test.tsx` (novo).

## V2.2 — Identificação da placa-mãe
- **Modelo da placa-mãe no Windows.** O OSHI 6.6.5 lê `Win32_BaseBoard.Model`, que os fabricantes quase nunca preenchem; o nome da placa fica em `Win32_BaseBoard.Product` (SMBIOS tipo 2). Quando o OSHI não traz o modelo, o agente consulta `Product` uma única vez (resultado memorizado) pelo mesmo mecanismo de WMI do OSHI, sem dependência nova. Ex.: Gigabyte H81M-S1 passou de "Não disponível" para "H81M-S1".
- **"Não informado pelo fabricante" ≠ "Não disponível".** `MotherboardInfo` ganhou `notReportedFields`: campos que o firmware tem, mas com texto genérico ("To be filled by O.E.M.", "x.x"). Interface e saída do agente exibem a diferença. Campo novo e aditivo na API.
- O modelo do computador (`systemModel`) continua separado e nunca é copiado para o modelo da placa.
- Testes: `HardwareCollectorsTest`, `MetricsTest`, `SnapshotPrinterTest` e `HardwareOverviewPage.test.tsx` ampliados.

## V2.1 — Correções do módulo de compatibilidade com jogos
- **Parser da Steam corrigido.** A substituição `replaceAll(..., "\n")` inseria a letra "n" no lugar da quebra de linha, então todos os requisitos viravam uma única linha e cada jogo resultava em um só campo "RAM" (CPU, GPU, SO e armazenamento nunca eram avaliados). Era a causa das 2 falhas em `SteamRequirementsParserTest`.
- Cada linha agora é classificada pelo rótulo publicado pela Steam (inglês e português, inclusive "Graphics (AMD):"); palavras-chave só como alternativa. "Graphics: GTX 970, 4 GB memory" deixa de virar requisito de RAM; cabeçalhos "Minimum:" e o aviso "Requires a 64-bit processor" são ignorados.
- `HardwareAliasCatalog` (2 CPUs e 2 GPUs, comparação por trecho de texto e ordem indefinida entre alternativas) substituído por `HardwareModels`, que identifica o modelo exato de CPU/GPU (Intel Core, Core Ultra, Ryzen, FX, GeForce GT/GTX/RTX, Radeon RX/R5-R9/HD, Intel HD/UHD/Arc). Sufixos contam (i5-8400 ≠ i5-8400T; RTX 3060 ≠ 3060 Ti) e todas as alternativas ("i5-8400 / Ryzen 5 3600") são consideradas.
- Sistema operacional comparado por versão numérica (Windows 11 atende a "Windows 10"; "Windows 7/8/10" exige 7) e exigência de 64 bits.
- RAM: margem de 90% vai para revisão manual em vez de "abaixo" (memória reservada pelo sistema/GPU integrada).
- Linhas não comparáveis (DirectX, Rede, Notas) saem da tabela de resultados (continuam no texto original); o resumo diz o que foi confirmado e o que falta revisar, em vez de quase sempre "Análise incompleta".
- Testes: `HardwareModelsTest` (novo), `SteamRequirementsParserTest` e `GameCompatibilityEvaluatorTest` ampliados com HTML real da Steam e os casos acima. Validado contra a Steam real (Cyberpunk 2077 e Hollow Knight).

## V1.5 — Sensores sem programa externo
- Removida a dependência do LibreHardwareMonitor em execução (servidor web na porta 8085). O usuário não precisa mais abrir outro programa.
- Novo leitor de sensores interno `sensor-host/` (C#, .NET Framework 4.8, ~3 MB) com a biblioteca LibreHardwareMonitorLib 0.9.6. O backend o inicia ao subir, pedindo permissão de administrador ao Windows; se recusada, roda sem elevação (GPU continua com leitura).
- Comunicação por pipe nomeado local com nome aleatório (sem porta de rede); o leitor termina junto com o backend e é reiniciado se cair (até 3 vezes).
- Agente: pacote `com.pccheck.agent.lhm` substituído por `com.pccheck.agent.sensors` (`SensorHostClient`, `SensorHostParser`, `SensorReading`, `SensorSource`). Sem o parser de textos regionais ("45,0 °C"): o leitor envia números. `jna-platform` declarado explicitamente (mesma versão do OSHI).
- Mensagens de "Sem leitura" agora dizem o motivo real (sem administrador, sem driver PawnIO, leitor não compilado). Novo `CollectorUnavailableException` leva esse motivo até a interface.
- Configuração: `pccheck.lhm.url` substituída por `pccheck.sensors.enabled`, `pccheck.sensors.elevate` e `pccheck.sensors.host-path`.
- Novo `scripts/build-sensor-host.bat`; `run-backend` compila o leitor na primeira execução. Guia em `docs/sensores.md` (substitui `docs/libre-hardware-monitor.md`).
- Testes: `SensorHostParserTest` e `SensorHostClientTest` (substituem `LhmParserTest` e `LhmClientTest`).

## V1.1–V1.4 — Módulo D: Game Compatibility (implementação inicial)
- Pesquisa Steam: `SteamStoreClient` e `SteamGameSearchService`, validação de termo, limite de 10 resultados, timeouts, tratamento de HTTP 429/5xx e cache de pesquisa.
- Requisitos: `SteamRequirementsService` consulta `appdetails` por AppID, valida `success=true` e `type=game`, preserva o HTML original e mantém cache por 12 horas com atualização manual.
- Parsing: `SteamRequirementsParser` limpa HTML e reconhece RAM, armazenamento, SO, CPU e GPU; cada campo preserva texto original, valor extraído, confiança e estado.
- Compatibilidade: `GameCompatibilityEvaluator` reutiliza o inventário atual. RAM e espaço livre são comparados numericamente; CPU/GPU só são aprovados para equivalências explícitas do catálogo inicial; SO e modelos sem equivalência ficam em revisão manual.
- API: `/api/games/search`, `/api/games/{appId}/requirements` e `/api/games/{appId}/compatibility`.
- Interface: nova aba de compatibilidade, pesquisa com debounce, seleção por AppID, comparação de mínimos/recomendados, requisitos originais, fonte, horário, avisos e atualização.
- Testes adicionados para pesquisa controlada, URL fixa, HTTP 429/5xx, JSON inválido, parsing, unidades, VRAM, dados ausentes, aliases explícitos e estados de compatibilidade.
- Validação pendente: Maven não está instalado neste ambiente; testes/build Java e fluxo com Steam/hardware reais precisam ser executados em ambiente de desenvolvimento. A instalação npm também não concluiu neste ambiente.

## V1.0 — Versão estável dos módulos A, B e C
- Escopo congelado: nenhuma funcionalidade nova. A V1.0 fecha os módulos A, B e C; o Módulo D (compatibilidade com jogos) começa na V1.1.
- Fins de linha: novo `.gitattributes` (LF no repositório, LF nos `.sh`, CRLF nos `.bat`). Antes, os arquivos do Windows apareciam todos como modificados no Git e os `.sh` com CRLF falhavam no Git Bash/WSL.
- Documentação: removidas as cópias antigas `docs/api-inventario.md`, `docs/api-live.md` e `docs/api-saude.md` (a `api-live.md` estava desatualizada: ainda dizia que a GPU era sempre indisponível). A versão válida e referenciada fica em `docs/api/`.
- Scripts: removidos `scripts/build-agent.sh` e `.bat` (restos da V0.1 que não funcionam com as dependências atuais; use `run-agent`).
- Versão 1.0.0 em todos os módulos: POMs (raiz, agente e backend), `PcCheckAgent.VERSION`, `/api/ping`, scripts, `tests/verificar-agente.sh`, `package.json` e `package-lock.json`, além de README e documentação.
- Pendências que dependem do grupo (não foram preenchidas para não inventar dados): integrantes (README), licença (README e arquivo `LICENSE`) e canal de contato de segurança (`SECURITY.md`).
- Não validado nesta etapa: build e testes (`mvn test`, `npm test`, `npm run build`) e o fluxo completo em máquina real precisam ser executados pelo grupo antes de marcar a tag `v1.0.0`.

## Pós-V0.9 — Organização da documentação
- Documentação reorganizada no formato do repositório de referência (aula-documentacao-software): `README.md` enxuto na raiz; `docs/` com documentação de projeto, API, processo (DevOps), diagramas e checklist; `templates/README-projeto.md`; `CONTRIBUTING.md` e `SECURITY.md`.
- Documentos de API movidos para `docs/api/`; links atualizados.
- Diagramas em Mermaid, cada um com descrição em texto.
- Novo `scripts/testes-documentacao.sh`: verifica documentos obrigatórios, marcadores pendentes, links internos e segredos óbvios.
- Pendências explícitas: integrantes, licença e canal de contato de segurança.

## Pós-V0.9 — GPU e sensores via LibreHardwareMonitor
- O OSHI 6.6.5 não fornece telemetria de GPU e, no Windows, só consulta o Open Hardware Monitor antigo para sensores (por isso "Não suportado" na GPU e "Sem leitura" nos sensores). Agora o agente lê o `data.json` do servidor web local do LibreHardwareMonitor (novo pacote `com.pccheck.agent.lhm`: `LhmClient`, `LhmParser`, `LhmReading`), sem nova dependência.
- Live Monitor: a GPU mostra placa, utilização, memória de vídeo (em uso e total) e temperatura, com gráfico; Sensores ganha temperatura da GPU e gráfico de temperaturas CPU/GPU. `GpuLive` e `SensorLive` ganharam campos (`vramTotalBytes`, `temperatureCelsius`, `name`; `gpuTemperatureCelsius`).
- Health Check: a temperatura da CPU usa a leitura do LibreHardwareMonitor e, na falta dela, o OSHI.
- Sem o LibreHardwareMonitor rodando, GPU e sensores continuam "Sem leitura" (nunca estimados), agora com a orientação de como ativar. Configuração em `pccheck.lhm.url` (somente localhost; em branco desativa).
- Testes: `LhmParserTest` e `LhmClientTest` (JSON de exemplo nos formatos novo e antigo, números em pt-BR) e novos casos em `LiveMonitorPage.test.tsx`. Guia em `docs/libre-hardware-monitor.md`.

## V0.9 — Integração e estabilização
- Revisão de integração dos módulos A, B e C (backend e frontend) **sem novas funcionalidades**: a documentação da API do Live Monitor foi conferida contra o código (intervalo mínimo de 500 ms, histórico de 10 a 3600 amostras, coleta sem acúmulo, `limit` menor que 1 devolve lista vazia) e não precisou de alterações.
- Logs: o aviso repetido do OSHI sobre `MSAcpi_ThermalZoneTemperature` (sensor ausente, uma linha por coleta) passa a ficar oculto por `logging.level.oshi.util.platform.windows.WmiQueryHandler=ERROR` em `application.properties`. A interface continua mostrando "Sem leitura"; remover a linha restaura os avisos.
- Documentação: README com a versão atual, requisito de **Node.js 22.12+** (o Vitest 5 não roda em versões anteriores), orientação de usar dois terminais e entrar em `frontend/`, e nova seção **Solução de problemas** com as falhas reais encontradas ao executar do zero no Windows (PATH do Maven, `pccheck-parent` ausente, porta 8080 ocupada, `Missing script: "dev"`, Git Bash para os `tests/*.sh`).
- `frontend/README.md` substituído: era o texto padrão do template do Vite; agora descreve requisitos, comandos, proxy da API e estrutura de `src/`.
- Versão 0.9.0 em todos os módulos: POMs (raiz, agente e backend), `PcCheckAgent.VERSION`, `/api/ping`, scripts, `tests/verificar-agente.sh`, `package.json` e `package-lock.json` do frontend.
- Pendências conhecidas: `scripts/build-agent.sh` e `.bat` são restos da V0.1 e não funcionam com as dependências atuais (substituídos por `run-agent`); a validação do fluxo completo em máquina real depende de execução manual (ver relatório da V0.9).

## V0.8.1 — Ajustes finais da interface e versão 0.8.0
- Correção na coleta da placa-mãe: versões com placeholder numérico, como `x.x` ou `X.XX`, passam a ser descartadas e aparecem como "Não disponível" (`Metrics.cleanOemText`, com teste em `HardwareCollectorsTest`).
- Versão 0.8.0 em todos os módulos: POMs, `PcCheckAgent.VERSION`, `/api/ping`, scripts e frontend.
- Correção no `run-backend` (`.bat` e `.sh`): instala o pom pai (`mvn -N install`) antes do agente, evitando a falha de dependência `pccheck-parent` ao subir o backend com o repositório Maven local limpo.
- Correção do teste `App.test.tsx`: a verificação de saída do Live Monitor passa a usar o título "Por processador lógico", já que o Hardware Overview também tem um cartão "CPU".
- Documentação: README revisado (títulos sem número de versão, limitação de atualização separada por tela).

## V0.8 — Módulo C: interface
- Nova aba **Live Monitor** (`LiveMonitorPage`) no frontend, consumindo `GET /api/live/history` (carga inicial) e `GET /api/live` (consultas seguintes), sem alterar o backend nem o agente.
- Indicadores atuais e gráficos temporais em SVG, **sem nova dependência**: CPU (total e por processador lógico), RAM, temperatura da CPU, ventoinhas, leitura/escrita e tempo ocupado por unidade, ocupação dos volumes. Eixos com unidade explícita (%, °C, MiB/s) e horário inicial/final.
- Métricas indisponíveis: valor ausente gera **lacuna** no gráfico (nunca zero nem interpolação); grupos sem dado mostram "Sem leitura"; a GPU aparece como "Não suportado"; falha de coleta de um grupo (ERROR) não afeta os demais cartões.
- Horário da última leitura, intervalo de coleta, quantidade de amostras e duração da última coleta. **Estado da conexão/coleta**: Conectando, Aguardando a primeira coleta, Conectado, Sem amostras novas, Coleta com problemas e Sem conexão com o backend (a última amostra recebida continua visível e a conexão é retomada sozinha).
- Consulta periódica sem acúmulo: o hook `useLiveMonitor` agenda a próxima consulta só depois que a anterior termina (setTimeout encadeado), segue o intervalo de coleta do backend (1 a 10 s), recua após falhas (2 s, 4 s, 8 s… até 10 s) e interrompe tudo ao sair da aba. Histórico limitado a 300 amostras no navegador; amostra repetida é ignorada e histórico recomeça se o backend for reiniciado.
- Lógica pura em `modules/live/series.ts` (histórico, extração de séries, geometria dos gráficos, estado da conexão) e `format.ts`; serviço `liveService` com validação do formato da resposta.
- Testes (Vitest): `series.test.ts`, `format.test.ts`, `liveService.test.ts`, `LiveMonitorPage.test.tsx` e navegação em `App.test.tsx`.
- Versão 0.8.0 em todos os módulos: POMs, agente (`PcCheckAgent.VERSION`), backend, `/api/ping`, scripts e frontend.

## V0.7 — Módulo C: coleta periódica
- Agente: `LiveSampler` coleta, a cada amostra, CPU (total e por processador lógico), RAM, discos (leitura/escrita/ocupação por unidade e espaço livre dos volumes) e sensores (temperatura e ventoinhas), cada grupo isolado por `CollectorRunner`. Usa a diferença entre contadores da amostra anterior, sem bloquear.
- GPU: sempre "não suportado" (o OSHI não fornece utilização nem memória de vídeo em uso); nenhum valor é inventado.
- Novos modelos: `LiveSample`, `CpuLive`, `GpuLive`, `DiskLive`, `DiskActivity`, `SensorLive`; `Metrics.ratePerSecond` e `Metrics.busyPercent` (descartam contadores inválidos ou reiniciados).
- Backend: pacote `monitor` com `MonitorEngine` (uma coleta por vez, exceções registradas, detecção de coleta travada) e `SampleHistory` (histórico limitado em memória). `LiveMonitorService` agenda a coleta em thread própria com `scheduleWithFixedDelay` (sem acúmulo de tarefas).
- Endpoints `GET /api/live` e `GET /api/live/history?limit=N` (apenas leem a memória; a coleta ocorre em segundo plano). Documentação em `docs/api-live.md`.
- Configuração: `pccheck.monitor.interval-ms` (padrão 2000, mínimo 500) e `pccheck.monitor.history-size` (padrão 300).
- Testes: 14 novos no backend (histórico, motor, DTO) e 2 no agente (taxas); novo `tests/verificar-live.sh`.
- Versão 0.7.0 em POMs, agente, `/api/ping` e scripts. Frontend sem alterações (0.6.0); gráficos e indicadores são a V0.8.

## V0.6 — Módulo B: interface
- Página `HealthCheckPage` consumindo `GET /api/health-check`: resumo de saúde (severidade geral e contagem por nível) e lista de alertas, do mais grave para o menos grave.
- Níveis visuais consistentes (Informação, Atenção, Crítico): cor da borda + selo com texto, para não depender só da cor; suporte a tema escuro.
- Cada alerta mostra componente, título, descrição, valor observado (ou "Sem leitura"), limite e natureza (normal, preventivo ou dado indisponível), deixando claro que não comprova defeito.
- Estados: carregando, erro de comunicação (com "Reavaliar"), última avaliação mantida se a atualização falhar, e trava contra requisições simultâneas.
- Navegação entre Hardware Overview e Health Check (`App.tsx`), sem nova dependência.
- Serviço `healthService` com validação do formato; módulo `modules/health/` (tipos, rótulos, hook, componentes).
- Testes (Vitest): cenários normal, atenção, crítico e sensor ausente, estados da página, serviço, rótulos e navegação.
- Backend e agente permanecem na versão 0.5.0 (sem alterações). Frontend 0.6.0.

## V0.5 — Módulo B: diagnósticos
- Pacote `diagnostic` no backend: `Severity` (Informação, Atenção, Crítico), `AlertKind` (normal, preventivo, indisponível), `Alert`, `HealthThresholds`, `HealthReport` e `HealthEvaluator`.
- Diagnósticos independentes: `TemperatureDiagnostic`, `DiskSpaceDiagnostic`, `MemoryDiagnostic`, `SmartDiagnostic` e `CollectionDiagnostic` (coleta incompleta). A falha de um não impede os demais.
- Agente: `SensorCollector` e `SensorInfo` (temperatura da CPU via OSHI); `Metrics.validTemperature` descarta 0, NaN e valores implausíveis.
- Endpoint `GET /api/health-check` (`HealthCheckController`/`HealthCheckService`); documentação em `docs/api-saude.md`.
- Limites configuráveis em `application.properties` (`pccheck.health.*`), com a origem documentada (padrões genéricos do projeto, não do fabricante).
- SMART: **não suportado** nesta versão (o OSHI não expõe SMART); o alerta informa que a saúde física não foi avaliada.
- Uso alto de RAM gera só aviso (Atenção); ausência de sensor gera só Informação; nenhum alerta afirma defeito.
- Testes: 29 novos no backend (regras, limites, isolamento de falhas) e 1 no agente; novo `tests/verificar-saude.sh`.
- Versão 0.5.0 em POMs, agente, `/api/ping` e scripts. Frontend sem alterações (permanece 0.4.0; a interface do Módulo B é a V0.6).

## V0.4 — Módulo A: interface
- Dashboard em React + TypeScript (`HardwareOverviewPage`) com cartões para sistema operacional, CPU, RAM, GPU, placa-mãe e armazenamento.
- Estados de carregamento, erro de comunicação (com tentativa nova), indisponibilidade e falha de coleta por componente.
- Horário e origem da última coleta; botão Atualizar com trava contra requisições simultâneas; mantém a última coleta se a atualização falhar.
- Serviço `inventoryService` com validação do formato da resposta; formatadores que exibem "Não disponível" para valores ausentes ou inválidos.
- Proxy `/api` no Vite (dev e preview) para `127.0.0.1:8080`.
- Testes com Vitest e Testing Library (18 testes). Agente e backend permanecem na versão 0.3.0 (sem alterações).

## V0.3 — Módulo A: coleta de hardware
- Coletores `GpuCollector`, `MotherboardCollector` e `DiskCollector` (unidades físicas e volumes), isolados por `CollectorRunner`.
- Novos modelos: `GpuInfo`, `MotherboardInfo`, `DiskInfo`, `VolumeInfo`, `StorageInfo`; `SystemSnapshot` ampliado.
- `Metrics`: descarte de textos genéricos de fabricantes (`cleanOemText`) e cálculo de espaço usado (`usedBytes`).
- Backend: `GET /api/inventory` com `InventoryDto`/`ComponentDto` (estado OK/UNAVAILABLE/ERROR por componente) e `InventoryService`.
- O backend usa os coletores do agente no mesmo processo (dependência Maven `pccheck-agent`); o agente gera também o jar executável `-all`.
- Testes: 25 no agente e 1 no backend; novo `tests/verificar-api.sh`; scripts `run-backend.sh e .bat`.
- Documentação da API em `docs/api-inventario.md`.

## V0.2 — Coleta básica
- Integração do OSHI 6.6.5 no agente.
- Coletores `OsCollector`, `CpuCollector` e `RamCollector`, executados por `CollectorRunner` (falha isolada por coletor).
- Modelos `OsInfo`, `CpuInfo`, `MemoryInfo`, `SystemSnapshot` e `CollectorResult`.
- `Metrics`: tratamento de valores inválidos/sentinelas, percentuais e carga de CPU.
- Impressão em texto (`SnapshotPrinter`) com "Nao disponivel" para dados ausentes.
- Testes unitários JUnit 5 (13 testes) e `tests/verificar-agente.sh`.
- Scripts `scripts/run-agent.sh` e `.bat` (substituem `build-agent`, que não funciona com dependências).
- POM pai com versões fixas de plugins (compiler, surefire, jar); agente gera jar executável (shade).
- README conforme seções 5 e 6 do Projeto.md.

## V0.1 — Estrutura e execução mínima
- Estrutura de pastas conforme a especificação (agent, backend, frontend, docs).
- POM pai Maven (Java 21) com módulos agent e backend.
- Agente mínimo que inicia e informa versão e Java em uso.
- Backend Spring Boot 3.3.5 com GET /api/ping, escutando só em 127.0.0.1.
- Frontend React + TypeScript (Vite) com página inicial mínima.
