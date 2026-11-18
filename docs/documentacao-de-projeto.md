# Documentação de projeto — PCCheck

## Objetivo

O PCCheck é uma aplicação **local** de identificação, diagnóstico básico e monitoramento de hardware. Ele coleta
informações reais do computador onde o agente está em execução e as apresenta de forma compreensível para um usuário
comum. Projeto da disciplina Projeto Integrador de Sistemas Operacionais (versão atual: **1.0.0**).

Uma aplicação hospedada na nuvem não consegue inspecionar o hardware de quem a visita; por isso a coleta sempre ocorre
na máquina do usuário.

## Problema

Um usuário comum não tem uma forma simples de saber qual hardware o computador possui, se há algo preocupante
(temperatura, disco cheio, memória saturada) e como o equipamento se comporta ao longo do tempo.

## Público

- Usuários domésticos que querem entender o próprio computador.
- Estudantes e avaliadores da disciplina, que verificam a aplicação e a documentação.
- Desenvolvedores que manterão o projeto.

## Escopo

**Dentro do escopo (módulos A, B e C):**

| Módulo | Nome | Responsabilidade |
|---|---|---|
| A | Hardware Overview | Identificar e apresentar componentes e suas propriedades. |
| B | Health Check | Avaliar indicadores disponíveis e apresentar alertas explicáveis. |
| C | Live Monitor | Atualizar métricas periodicamente e exibir gráficos de desempenho. |

**Fora do escopo:** análise de processos, compatibilidade com jogos, exportação de relatórios, banco de dados
(o histórico fica só em memória), testes de estresse e qualquer ação que altere o computador.

## Requisitos funcionais

### Módulo A — Hardware Overview

- Coletar sistema operacional, CPU, memória, GPU, placa-mãe e armazenamento (unidades e volumes).
- Um cartão por componente, com horário e origem da última coleta e botão **Atualizar**.
- Quando um dado não existir, mostrar **Não disponível**. Nunca inventar ou estimar valores.
- Falha de um coletor não impede a leitura dos demais.

### Módulo B — Health Check

- Temperatura da CPU: comparar a leitura real com limites configuráveis (Normal, Atenção ou Crítico).
- Espaço em disco: espaço livre e percentual por volume.
- Uso de RAM: aviso de uso elevado, sem classificar como defeito.
- Saúde dos discos (SMART): vida útil restante, temperatura, horas ligado e dados gravados, quando o leitor de sensores roda como administrador; sem leitura, o relatório informa o motivo e que a saúde física não foi avaliada.
- Coleta incompleta: informar dado indisponível, não falha física.
- Cada alerta contém identificador, componente, severidade, título, descrição, valor observado, limite e horário.
- Severidades: Informação, Atenção e Crítico.

### Módulo C — Live Monitor

- CPU (total e por processador lógico), RAM, GPU, sensores (temperaturas e ventoinhas) e discos.
- Gráficos temporais com unidades explícitas (%, °C, MiB/s, RPM).
- Horário da última atualização e estado da conexão.
- Coleta a cada 2 segundos (configurável), em segundo plano, sem acúmulo de tarefas.

## Requisitos não funcionais

- **Dados reais:** nenhum valor simulado é apresentado como medição.
- **Segurança:** o backend escuta somente em `127.0.0.1`; não há endpoints de controle remoto. O leitor de sensores
  roda como administrador, mas não abre portas de rede (pipe nomeado local) e termina junto com o backend.
- **Privacidade:** os dados de inventário permanecem no computador local.
- **Desempenho:** baixo consumo de CPU e memória; a frequência de coleta pode ser reduzida.
- **Robustez:** falha de GPU ou de sensores não derruba CPU, RAM ou discos; exceções são tratadas.
- **Sistema operacional:** a primeira versão prioriza Windows, com código específico isolado.
- **Testes:** conversões, percentuais e regras de diagnóstico têm testes automatizados.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Agente de coleta | Java 21 e OSHI 6.6.5 |
| Backend | Spring Boot 3.3.5 (API local) |
| Frontend | React 19 com TypeScript, Vite, Vitest e Testing Library |
| Automação | Maven, JUnit 5, scripts Shell e Batch |
| Hardware (GPU e sensores) | Leitor de sensores interno: C# (.NET Framework 4.8) + LibreHardwareMonitorLib 0.9.6, driver PawnIO |

## Arquitetura

Visão resumida; diagramas completos em [diagramas.md](diagramas.md).

1. O **agente** consulta o sistema operacional pelo OSHI e, para GPU e sensores, o leitor de sensores interno (iniciado pelo backend).
2. Os **coletores** convertem os resultados em modelos padronizados, cada um com estado próprio.
3. O **backend** expõe a API local e executa as regras do Health Check.
4. O **frontend** consulta a API e apresenta os módulos A, B e C.

## Limitações conhecidas

- Frequência, núcleos e fabricante dependem do que o sistema operacional e o driver informam.
- O uso de CPU é uma média em 500 ms e varia a cada execução.
- **GPU:** identificação e VRAM informada pelo sistema (o OSHI não distingue VRAM dedicada de compartilhada). A
  utilização, a memória em uso e a temperatura vêm do leitor de sensores interno (não exigem administrador).
- **Armazenamento:** o tipo (SSD/HDD) só aparece quando o SO informa (Windows: `MSFT_PhysicalDisk`; Linux: `/sys/block`); pen drives e discos em que o SO não informa ficam "Não disponível". Nunca é deduzido do nome do modelo.
- **Jogos instalados:** só a Steam é lida (outras lojas não). A lista vem dos arquivos da própria Steam e inclui ferramentas (ex.: redistribuíveis). Steam em local não padrão exige `PCCHECK_STEAM_DIR`.
- **Placa-mãe:** textos genéricos de fabricantes (como "To be filled by O.E.M.") e placeholders (como `x.x`) são descartados e aparecem como "Não informado pelo fabricante" (diferente de "Não disponível", quando o dado não pôde ser lido). No Windows, o modelo vem de `Win32_BaseBoard.Product` quando o OSHI não o informa (o OSHI lê `Win32_BaseBoard.Model`, que quase nunca é preenchido). O modelo do computador nunca é usado como modelo da placa.
- **Sensores:** temperatura da CPU e ventoinhas da placa-mãe exigem permissão de administrador (aviso do Windows ao
  iniciar) e o driver PawnIO; sem eles ficam "Sem leitura", com o motivo. Placas-mãe com chip de monitoramento não
  suportado pela LibreHardwareMonitorLib não informam ventoinhas.
- **Limites do Health Check:** são padrões genéricos do projeto, não limites oficiais do fabricante.
- **SMART:** lido pelo leitor de sensores (LibreHardwareMonitorLib), que exige administrador. São avaliadas a vida útil restante (SSDs) e a temperatura; setores realocados, setores pendentes e erros de leitura não são lidos, então "indicadores normais" não garante ausência de falhas.
- **Atualização:** Hardware Overview e Health Check atualizam apenas pelos botões **Atualizar** e **Reavaliar**; só o Live Monitor atualiza sozinho.
- **Validação em hardware real:** as leituras de temperatura, GPU e sensores foram validadas com testes automatizados e
  dados de exemplo. O comportamento com sensores reais depende de teste na máquina de cada usuário.

## Conceitos de Sistemas Operacionais aplicados

| Conceito | Como aparece no projeto |
|---|---|
| Consulta de uso de CPU e memória | Uso de CPU medido pela diferença entre contadores; RAM com total, usada, disponível e percentual. |
| Memória virtual e paginação | Arquivo de paginação/swap, memória comprometida em relação ao limite (Windows: Commit Limit; Linux: `CommitLimit`/`Committed_AS` de `/proc/meminfo`, com overcommit) e páginas lidas/gravadas em disco por segundo (Linux: `/proc/vmstat`). O Health Check só aponta pressão de memória com evidência sustentada: paginação contínua **e** RAM cheia. |
| Interfaces do SO para leitura de hardware | Cada card e painel tem "De onde vêm estes dados?", com a fonte de cada campo no Windows (registro, WMI, contadores de desempenho, APIs como `GetPerformanceInfo`) e no Linux (`/proc`, `/sys`, comandos como `lspci` e `lsblk`). A coluna do SO em uso é destacada. Catálogo único em `frontend/src/modules/sources/sourceCatalog.ts`, conferido no código do OSHI 6.6.5; um teste garante que todo campo exibido tenha fonte. |
| Identificação do sistema operacional | Nome, versão, build, arquitetura e tempo de atividade (OSHI lê `/proc` no Linux e os contadores de desempenho do Windows). |
| Recursos de hardware | Núcleos físicos e lógicos, frequências, placa de vídeo e placa-mãe. Valores inválidos (como `-1`) são descartados. |
| Dispositivos de bloco e SMART | Tipo de cada disco informado pelo SO (Windows: WMI `MSFT_PhysicalDisk`; Linux: `/sys/block/<disco>/queue/rotational`, ignorando removíveis) e saúde pelo SMART, que exige privilégio de administrador para o acesso direto ao dispositivo. |
| Armazenamento e sistema de arquivos | Unidades físicas e volumes montados, com capacidade, espaço livre e percentual de uso. Cada volume é ligado ao disco físico pelas partições (Windows: WMI `Win32_DiskDriveToDiskPartition`/`Win32_LogicalDiskToPartition`; Linux: udev e `/proc/mounts`). |
| Arquivos e diretórios de outros programas | Jogos instalados lidos dos arquivos da Steam (`libraryfolders.vdf` e `appmanifest_*.acf`), só leitura, com caminhos por SO (registro do Windows ou pastas do Linux/Flatpak), limite de tamanho por arquivo e arquivos inválidos listados sem esconder os demais. |
| Concorrência | Coleta periódica em thread de segundo plano, uma coleta por vez, sem bloquear a interface. |
| Agendamento de tarefas e processos em segundo plano | `scripts/agendar-coleta` registra a coleta no agendador do SO (`schtasks` no Windows, `cron` no Linux). Cada execução é um processo de curta duração cujo código de saída fica registrado no agendador; o backend consulta o agendador executando `schtasks`/`crontab` com tempo limite. |
| Arquivos, travas, permissões e variáveis de ambiente | Histórico em JSON Lines com rotação diária e retenção; gravação sob trava exclusiva de arquivo (concorrência entre agendador e backend); no Linux, pasta `700` e arquivos `600`; pasta e retenção configuráveis por `PCCHECK_DATA_DIR` e `PCCHECK_RETENCAO_DIAS`. |
| Shell Script e automação | `scripts/` compila e executa agente e backend; `tests/` confere a saída. |
