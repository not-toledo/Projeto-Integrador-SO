# Histórico de versões

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
