# Histórico de versões

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
