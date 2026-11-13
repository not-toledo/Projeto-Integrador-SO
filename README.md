# PCCheck

Aplicação local de identificação, diagnóstico básico e monitoramento de hardware.
Projeto Integrador de Sistemas Operacionais.

**Versão atual: V0.2 — coleta básica** (sistema operacional, CPU e memória, via agente Java + OSHI).

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
- Spring Boot 3.3.5 (API local; endpoint mínimo, evolui a partir da V0.3)
- React + TypeScript com Vite (interface; página inicial mínima, evolui a partir da V0.4)
- Maven, JUnit 5, Git/GitHub, Shell Script

## Conceitos de Sistemas Operacionais aplicados

| Conceito | Como aparece no projeto |
|---|---|
| Consulta de uso de CPU e memória | O agente mede o uso de CPU em um intervalo de 500 ms e calcula total, usada, disponível e percentual de RAM. |
| Identificação do sistema operacional | Nome, versão, build, arquitetura e tempo de atividade, obtidos das APIs do próprio SO (o OSHI lê `/proc` no Linux e os contadores de desempenho do Windows). |
| Recursos do sistema e hardware | Núcleos físicos e processadores lógicos, frequências informadas pelo sistema. Valores inválidos (como `-1`) são descartados. |
| Shell Script e automação | `scripts/run-agent.sh` e `run-agent.bat` compilam e executam o agente; `tests/verificar-agente.sh` confere a saída. |

Previstos nas próximas versões: espaço em disco, temperaturas, SMART, coleta periódica em segundo plano.

## Funcionalidades implementadas (V0.2)

- Coleta de sistema operacional, CPU e memória do computador local.
- Isolamento de falhas: se um coletor falhar, os demais continuam funcionando; cada resultado informa `OK`, `Não disponível` ou `ERRO`.
- Modelos de resposta padronizados (`OsInfo`, `CpuInfo`, `MemoryInfo`, `SystemSnapshot`).
- Saída em texto no terminal.
- Backend com `GET /api/ping` (apenas em `127.0.0.1`) e frontend de estrutura inicial.

## Estrutura do repositório

```
pccheck/
├── agent/      # Agente local (Java 21 + OSHI): collector/, model/ e testes unitários
├── backend/    # API Spring Boot (escuta só em 127.0.0.1:8080)
├── frontend/   # React + TypeScript (Vite)
├── scripts/    # Automação: run-agent.sh / run-agent.bat
├── tests/      # Verificação ponta a ponta: verificar-agente.sh
├── docs/       # Documentação e histórico (CHANGELOG)
├── .gitignore
└── README.md
```

## Instalação e execução

Requisitos: **JDK 21**, **Maven 3.9+** (e Node.js 20+ para o frontend). Funciona em Windows e Linux; a primeira versão prioriza o Windows.

### Agente

```
mvn -pl agent package
java -jar agent/target/pccheck-agent-0.2.0.jar
```

### Backend

```
cd backend
mvn spring-boot:run
```

Teste: abrir `http://127.0.0.1:8080/api/ping` → `{"status":"UP","app":"PCCheck","version":"0.2.0"}`.

### Frontend

```
cd frontend
npm install
npm run dev
```

## Script de automação

`scripts/run-agent.sh` (Linux/macOS) ou `scripts\run-agent.bat` (Windows): compila o agente (executando os testes
unitários) e o inicia em seguida. Execute a partir de qualquer pasta; o script entra na raiz do projeto sozinho.

## Como testar e verificar

1. **Testes unitários** (conversões, percentuais, tratamento de valores inválidos, isolamento de falhas e formatação):
   `mvn -pl agent test`
2. **Verificação ponta a ponta** (executa o agente real e confere as seções da saída):
   `mvn -pl agent package` e depois `tests/verificar-agente.sh` (Linux/macOS/Git Bash).
3. **Conferência manual**: compare os valores do agente com o Gerenciador de Tarefas (Windows) ou `lscpu` e `free -h` (Linux).
   Memória total, número de núcleos e modelo da CPU devem coincidir.

## Limitações conhecidas

- Frequência, núcleos e fabricante dependem do que o sistema operacional e o driver informam; ausentes aparecem como "Não disponível".
- O uso de CPU é uma média em 500 ms e varia a cada execução.
- GPU, placa-mãe, discos, temperatura e SMART ainda não foram implementados (V0.3 em diante).
- O backend e o frontend ainda não consomem os dados do agente (V0.3/V0.4).
