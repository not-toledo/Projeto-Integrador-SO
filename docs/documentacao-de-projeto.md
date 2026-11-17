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
(o histórico fica só em memória), SMART de discos, testes de estresse e qualquer ação que altere o computador.

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
- SMART: não suportado nesta versão; o relatório informa isso explicitamente.
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
- **Armazenamento:** o tipo (SSD/HDD) não é informado, pois não há evidência confiável via OSHI.
- **Placa-mãe:** textos genéricos de fabricantes (como "To be filled by O.E.M.") e placeholders (como `x.x`) são descartados.
- **Sensores:** temperatura da CPU e ventoinhas da placa-mãe exigem permissão de administrador (aviso do Windows ao
  iniciar) e o driver PawnIO; sem eles ficam "Sem leitura", com o motivo. Placas-mãe com chip de monitoramento não
  suportado pela LibreHardwareMonitorLib não informam ventoinhas.
- **Limites do Health Check:** são padrões genéricos do projeto, não limites oficiais do fabricante.
- **SMART:** não suportado (o OSHI não o expõe).
- **Atualização:** Hardware Overview e Health Check atualizam apenas pelos botões **Atualizar** e **Reavaliar**; só o Live Monitor atualiza sozinho.
- **Validação em hardware real:** as leituras de temperatura, GPU e sensores foram validadas com testes automatizados e
  dados de exemplo. O comportamento com sensores reais depende de teste na máquina de cada usuário.

## Conceitos de Sistemas Operacionais aplicados

| Conceito | Como aparece no projeto |
|---|---|
| Consulta de uso de CPU e memória | Uso de CPU medido pela diferença entre contadores; RAM com total, usada, disponível e percentual. |
| Identificação do sistema operacional | Nome, versão, build, arquitetura e tempo de atividade (OSHI lê `/proc` no Linux e os contadores de desempenho do Windows). |
| Recursos de hardware | Núcleos físicos e lógicos, frequências, placa de vídeo e placa-mãe. Valores inválidos (como `-1`) são descartados. |
| Armazenamento e sistema de arquivos | Unidades físicas e volumes montados, com capacidade, espaço livre e percentual de uso. |
| Concorrência | Coleta periódica em thread de segundo plano, uma coleta por vez, sem bloquear a interface. |
| Shell Script e automação | `scripts/` compila e executa agente e backend; `tests/` confere a saída. |
