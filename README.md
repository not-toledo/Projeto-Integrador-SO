# PCCheck

Aplicação local de identificação, diagnóstico básico e monitoramento de hardware.
Projeto Integrador de Sistemas Operacionais.

**Versão atual: V0.3 — Módulo A: coleta de hardware** (sistema operacional, CPU, memória, GPU, placa-mãe e armazenamento, via agente Java + OSHI, com API de inventário).

## Objetivo e problema

Um usuário comum não tem uma forma simples de saber qual hardware o computador possui, se há algo preocupante
(temperatura, disco cheio, RAM saturada) e como o equipamento se comporta ao longo do tempo. O PCCheck coleta
essas informações **reais** do próprio computador e as apresenta de forma compreensível.

Módulos previstos (escopo fechado):

- **A — Hardware Overview**: identifica os componentes e suas propriedades.
- **B — Health Check**: avalia indicadores disponíveis e gera alertas explicáveis.
- **C — Live Monitor**: métricas em tempo real com gráficos.

Dados indisponíveis são tratados como "não disponível": o sistema nunca simula nem estima valores.

## Integrantes

- Gabriel — *(completar com sobrenome, RA e demais integrantes do grupo)*

## Tecnologias

- Java 21 e [OSHI](https://github.com/oshi/oshi) 6.6.5 (agente de coleta)
- Spring Boot 3.3.5 (API local com o inventário de hardware em `/api/inventory`)
- React + TypeScript com Vite (interface; página inicial mínima, evolui a partir da V0.4)
- Maven, JUnit 5, Git/GitHub, Shell Script

## Conceitos de Sistemas Operacionais aplicados

| Conceito | Como aparece no projeto |
|---|---|
| Consulta de uso de CPU e memória | O agente mede o uso de CPU em um intervalo de 500 ms e calcula total, usada, disponível e percentual de RAM. |
| Identificação do sistema operacional | Nome, versão, build, arquitetura e tempo de atividade, obtidos das APIs do próprio SO (o OSHI lê `/proc` no Linux e os contadores de desempenho do Windows). |
| Recursos do sistema e hardware | Núcleos físicos e processadores lógicos, frequências informadas pelo sistema, placa de vídeo e placa-mãe. Valores inválidos (como `-1`) e textos genéricos de fabricantes são descartados. |
| Armazenamento e sistema de arquivos | Unidades físicas e volumes montados, com capacidade, espaço livre e percentual de uso (consulta ao sistema de arquivos do SO). |
| Shell Script e automação | `scripts/run-agent.sh/.bat` e `run-backend.sh/.bat` compilam e executam o agente e o backend; `tests/verificar-agente.sh` e `tests/verificar-api.sh` conferem a saída. |

Previstos nas próximas versões: temperaturas, SMART e coleta periódica em segundo plano.

## Funcionalidades implementadas (V0.3)

- Coleta de sistema operacional, CPU, memória, GPU, placa-mãe e armazenamento (unidades e volumes) do computador local.
- Isolamento de falhas: se um coletor falhar, os demais continuam funcionando; cada resultado informa `OK`, `Não disponível` ou `ERRO`.
- Modelos de resposta padronizados (`OsInfo`, `CpuInfo`, `MemoryInfo`, `GpuInfo`, `MotherboardInfo`, `StorageInfo`, `SystemSnapshot`) e DTOs da API (`InventoryDto`, `ComponentDto`).
- Saída em texto no terminal.
- Backend com `GET /api/inventory` (inventário de hardware; cada componente com estado próprio) e `GET /api/ping`, escutando apenas em `127.0.0.1`. Detalhes em [docs/api-inventario.md](docs/api-inventario.md).
- Frontend de estrutura inicial (a interface do Módulo A vem na V0.4).

## Estrutura do repositório

```
pccheck/
├── agent/      # Agente local (Java 21 + OSHI): collector/, model/ e testes unitários
├── backend/    # API Spring Boot (escuta só em 127.0.0.1:8080)
├── frontend/   # React + TypeScript (Vite)
├── scripts/    # Automação: run-agent e run-backend (.sh / .bat)
├── tests/      # Verificação ponta a ponta: verificar-agente.sh e verificar-api.sh
├── docs/       # Documentação e histórico (CHANGELOG)
├── .gitignore
└── README.md
```

## Instalação e execução

Requisitos: **JDK 21**, **Maven 3.9+** (e Node.js 20+ para o frontend). Funciona em Windows e Linux; a primeira versão prioriza o Windows.

### Agente

```
mvn -pl agent package
java -jar agent/target/pccheck-agent-0.3.0-all.jar
```

### Backend

O backend usa os coletores do agente no mesmo processo; por isso o agente precisa estar instalado no repositório Maven local antes:

```
mvn -pl agent install
mvn -f backend/pom.xml spring-boot:run
```

Teste: abrir `http://127.0.0.1:8080/api/inventory` (inventário) ou `http://127.0.0.1:8080/api/ping` → `{"status":"UP","app":"PCCheck","version":"0.3.0"}`.

### Frontend

```
cd frontend
npm install
npm run dev
```

## Script de automação

- `scripts/run-agent.sh` (Linux/macOS) ou `scripts\run-agent.bat` (Windows): compila o agente (executando os testes unitários) e o inicia em seguida.
- `scripts/run-backend.sh` ou `scripts\run-backend.bat`: instala o agente no repositório Maven local e inicia o backend.

Execute a partir de qualquer pasta; os scripts entram na raiz do projeto sozinhos.

## Como testar e verificar

1. **Testes unitários** (conversões, percentuais, tratamento de valores inválidos, isolamento de falhas e formatação):
   `mvn -pl agent test`
2. **Verificação ponta a ponta** (executa o agente real e confere as seções da saída):
   `mvn -pl agent package` e depois `tests/verificar-agente.sh` (Linux/macOS/Git Bash).
3. **Verificação da API** (com o backend em execução): `tests/verificar-api.sh` confere que cada componente do inventário tem estado próprio.
4. **Conferência manual**: compare os valores do agente com o Gerenciador de Tarefas e o Gerenciador de Dispositivos (Windows) ou `lscpu`, `free -h` e `df -h` (Linux).
   Memória total, núcleos, modelo da CPU/GPU e capacidade dos volumes devem coincidir.

## Limitações conhecidas

- Frequência, núcleos e fabricante dependem do que o sistema operacional e o driver informam; ausentes aparecem como "Não disponível".
- O uso de CPU é uma média em 500 ms e varia a cada execução.
- GPU: apenas identificação e VRAM informada pelo sistema (o OSHI não distingue VRAM dedicada de compartilhada); utilização/telemetria fica para a V0.7.
- Armazenamento: o tipo (SSD/HDD) não é informado, pois não há evidência confiável via OSHI; o espaço livre é o disponível ao usuário.
- Placa-mãe: textos genéricos como "To be filled by O.E.M." são descartados e aparecem como "Não disponível".
- Temperatura e SMART ainda não foram implementados (V0.5).
- A interface ainda não exibe o inventário (V0.4).
