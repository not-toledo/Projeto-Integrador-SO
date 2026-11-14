# PCCheck

Aplicação local de identificação, diagnóstico básico e monitoramento de hardware.
Projeto Integrador de Sistemas Operacionais.

**Versão atual: V0.6 — Módulo B: interface** (aba Health Check com resumo de saúde e lista de alertas, sobre a API de diagnósticos da V0.5). O dashboard do Módulo A (V0.4) continua disponível na aba Hardware Overview.

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
- React + TypeScript com Vite, Vitest e Testing Library (interface do Módulo A)
- Maven, JUnit 5, Git/GitHub, Shell Script

## Conceitos de Sistemas Operacionais aplicados

| Conceito | Como aparece no projeto |
|---|---|
| Consulta de uso de CPU e memória | O agente mede o uso de CPU em um intervalo de 500 ms e calcula total, usada, disponível e percentual de RAM. |
| Identificação do sistema operacional | Nome, versão, build, arquitetura e tempo de atividade, obtidos das APIs do próprio SO (o OSHI lê `/proc` no Linux e os contadores de desempenho do Windows). |
| Recursos do sistema e hardware | Núcleos físicos e processadores lógicos, frequências informadas pelo sistema, placa de vídeo e placa-mãe. Valores inválidos (como `-1`) e textos genéricos de fabricantes são descartados. |
| Armazenamento e sistema de arquivos | Unidades físicas e volumes montados, com capacidade, espaço livre e percentual de uso (consulta ao sistema de arquivos do SO). |
| Shell Script e automação | `scripts/run-agent.sh/.bat` e `run-backend.sh/.bat` compilam e executam o agente e o backend; `tests/verificar-agente.sh` e `tests/verificar-api.sh` conferem a saída. |

Previstos nas próximas versões: coleta periódica em segundo plano (V0.7).

## Funcionalidades implementadas (V0.3)

- Coleta de sistema operacional, CPU, memória, GPU, placa-mãe e armazenamento (unidades e volumes) do computador local.
- Isolamento de falhas: se um coletor falhar, os demais continuam funcionando; cada resultado informa `OK`, `Não disponível` ou `ERRO`.
- Modelos de resposta padronizados (`OsInfo`, `CpuInfo`, `MemoryInfo`, `GpuInfo`, `MotherboardInfo`, `StorageInfo`, `SystemSnapshot`) e DTOs da API (`InventoryDto`, `ComponentDto`).
- Saída em texto no terminal.
- Backend com `GET /api/inventory` (inventário de hardware; cada componente com estado próprio) e `GET /api/ping`, escutando apenas em `127.0.0.1`. Detalhes em [docs/api-inventario.md](docs/api-inventario.md).
- Dashboard do Módulo A (frontend): um cartão por componente (sistema operacional, CPU, RAM, GPU, placa-mãe e armazenamento), com barras de utilização, horário e origem da última coleta e botão **Atualizar** (coleta manual).
- Estados da interface: carregando; erro de comunicação com o backend (com "Tentar novamente"); componente indisponível ("Não disponível") e componente com falha de coleta, sem afetar os demais cartões. Se uma atualização falhar, a última coleta bem-sucedida continua visível com um aviso.

## Health Check (V0.5, Módulo B)

`GET /api/health-check` avalia indicadores **realmente disponíveis** e devolve alertas explicáveis (id, componente, severidade, título, descrição, valor observado, limite e horário). Detalhes em [docs/api-saude.md](docs/api-saude.md).

- **Severidades**: Informação, Atenção e Crítico. Cada alerta também diz se é *normal*, *preventivo* ou *dado indisponível*.
- **Temperatura da CPU**: compara a leitura real com limites configuráveis; sem sensor, informa indisponibilidade (nunca estima).
- **Espaço em disco**: por volume, com espaço livre, total e percentual.
- **RAM**: uso alto gera só aviso de uso elevado, jamais defeito.
- **SMART**: não suportado nesta versão; o alerta diz que a saúde física dos discos não foi avaliada.
- **Limites**: padrões genéricos do projeto (não do fabricante), configuráveis em `application.properties` (`pccheck.health.*`).
- O módulo só informa: não altera configurações, não faz reparos nem testes de estresse.

### Interface do Health Check (V0.6)

Na interface, use a aba **Health Check**: resumo de saúde (Informação, Atenção ou Crítico, com contagem) e a lista de alertas, do mais grave para o menos grave. Cada alerta mostra valor observado, limite e se é normal, preventivo ou dado indisponível (nenhum deles comprova defeito). O botão **Reavaliar** faz uma nova coleta.

## Estrutura do repositório

```
pccheck/
├── agent/      # Agente local (Java 21 + OSHI): collector/, model/ e testes unitários
├── backend/    # API Spring Boot (escuta só em 127.0.0.1:8080): controller/, service/, dto/, diagnostic/ (Módulo B), config/
├── frontend/   # React + TypeScript (Vite): components/, modules/hardware/, modules/health/, pages/, services/
├── scripts/    # Automação: run-agent e run-backend (.sh / .bat)
├── tests/      # Verificação ponta a ponta: verificar-agente.sh, verificar-api.sh e verificar-saude.sh
├── docs/       # Documentação e histórico (CHANGELOG)
├── .gitignore
└── README.md
```

## Instalação e execução

Requisitos: **JDK 21**, **Maven 3.9+** (e Node.js 20+ para o frontend). Funciona em Windows e Linux; a primeira versão prioriza o Windows.

### Agente

```
mvn -pl agent package
java -jar agent/target/pccheck-agent-0.5.0-all.jar
```

### Backend

O backend usa os coletores do agente no mesmo processo; por isso o agente precisa estar instalado no repositório Maven local antes:

```
mvn -pl agent install
mvn -f backend/pom.xml spring-boot:run
```

Teste: abrir `http://127.0.0.1:8080/api/inventory` (inventário) ou `http://127.0.0.1:8080/api/ping` → `{"status":"UP","app":"PCCheck","version":"0.5.0"}`.

### Frontend

Com o backend em execução (`scripts/run-backend.sh` ou `.bat`):

```
cd frontend
npm install
npm run dev
```

Abra o endereço exibido pelo Vite (normalmente `http://localhost:5173`). Em desenvolvimento, o Vite encaminha `/api` para
`http://127.0.0.1:8080`, então não há configuração de CORS. Para outro endereço de API, defina `VITE_API_BASE`.

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
4. **Testes do frontend** (formatação de valores, serviço de API e estados da interface: carregando, erro, indisponível, falha parcial e atualização; no Health Check, os cenários normal, atenção, crítico e sensor ausente): `cd frontend && npm test`. Verificação de tipos e build: `npm run build`.
5. **Testes das regras do Health Check** (limites, severidades, valores ausentes, isolamento de falhas): `mvn -pl backend -am test`. Com o backend em execução, `tests/verificar-saude.sh` confere a estrutura da resposta de `/api/health-check`.
6. **Conferência manual**: compare os valores do agente com o Gerenciador de Tarefas e o Gerenciador de Dispositivos (Windows) ou `lscpu`, `free -h` e `df -h` (Linux).
   Memória total, núcleos, modelo da CPU/GPU e capacidade dos volumes devem coincidir com o que o dashboard exibe.

## Limitações conhecidas

- Frequência, núcleos e fabricante dependem do que o sistema operacional e o driver informam; ausentes aparecem como "Não disponível".
- O uso de CPU é uma média em 500 ms e varia a cada execução.
- GPU: apenas identificação e VRAM informada pelo sistema (o OSHI não distingue VRAM dedicada de compartilhada); utilização/telemetria fica para a V0.7.
- Armazenamento: o tipo (SSD/HDD) não é informado, pois não há evidência confiável via OSHI; o espaço livre é o disponível ao usuário.
- Placa-mãe: textos genéricos como "To be filled by O.E.M." são descartados e aparecem como "Não disponível".
- Temperatura: depende de o sistema expor um sensor via OSHI; em muitos computadores (especialmente Windows sem driver específico) fica "não disponível". A leitura de temperatura **não foi testada em hardware real** nesta versão (apenas testes automatizados com valores de exemplo).
- SMART não é suportado (o OSHI não o expõe); exigiria ferramenta externa e privilégios elevados.
- Os limites do Health Check são genéricos, não oficiais do fabricante; ajuste-os conforme seu equipamento.
- A interface do Health Check atualiza apenas manualmente (botão **Reavaliar**); atualização periódica é do Módulo C. Os cenários normal, atenção, crítico e sensor ausente foram validados com dados de exemplo nos testes, não com sensores reais.
- A interface atualiza apenas manualmente (botão **Atualizar**); atualização periódica e gráficos vêm no Módulo C (V0.7/V0.8).
- O dashboard foi verificado por testes automatizados e pelo proxy de desenvolvimento; a aparência em navegador com dados reais depende de teste na sua máquina.
