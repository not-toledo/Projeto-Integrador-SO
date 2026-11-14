# Histórico de versões

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
