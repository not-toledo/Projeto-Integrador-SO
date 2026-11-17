# Diagramas — PCCheck

Os diagramas usam Mermaid, renderizado pelo GitHub. Cada diagrama tem descrição em texto para leitores que não podem
vê-lo.

## Visão geral dos componentes

```mermaid
flowchart LR
    subgraph Computador do usuário
        FE[Frontend React<br/>porta 5173 em desenvolvimento]
        BE[Backend Spring Boot<br/>127.0.0.1:8080]
        AG[Agente Java + OSHI<br/>coletores e modelos]
        SO[(Sistema operacional)]
        SH[Leitor de sensores<br/>sensor-host, administrador]
    end
    FE -- "/api (proxy do Vite)" --> BE
    BE --> AG
    AG -- OSHI --> SO
    AG -- "inicia e lê (pipe nomeado local)" --> SH
    SH -- "driver PawnIO" --> SO
```

**Descrição em texto:** o frontend envia requisições `/api` para o backend. O backend usa o agente, que lê o sistema
operacional pela biblioteca OSHI e, para GPU e sensores, inicia e consulta o leitor de sensores interno (`sensor-host`) por um pipe nomeado local. Tudo
acontece no mesmo computador.

## Fluxo do Live Monitor

```mermaid
sequenceDiagram
    participant UI as Frontend
    participant API as Backend
    participant Agente as Agente (LiveSampler)
    participant SH as Leitor de sensores
    loop a cada 2 s, uma coleta por vez
        Agente->>SH: lê o pipe (cache de 1 s)
        SH-->>Agente: sensores e GPU (ou falha)
        Agente->>Agente: CPU, RAM, discos, GPU, sensores
        Agente-->>API: amostra com estado por grupo
    end
    UI->>API: GET /api/live/history
    API-->>UI: histórico em memória
```

**Descrição em texto:** o agendador do backend executa uma coleta por vez. Em cada coleta, o agente consulta o
leitor de sensores (com cache de 1 segundo), monta CPU, RAM, GPU, discos e sensores, e guarda a amostra na memória.
O frontend lê o histórico pela API. Se o leitor de sensores não responder, só GPU e sensores ficam "Sem leitura"; os
demais grupos seguem normalmente.

## Estados de um grupo de métricas

```mermaid
stateDiagram-v2
    [*] --> OK: dado lido
    [*] --> UNAVAILABLE: sistema não informa
    [*] --> ERROR: coleta falhou
    OK --> UNAVAILABLE: leitura some
    UNAVAILABLE --> OK: leitura volta
    ERROR --> OK: próxima coleta funciona
```

**Descrição em texto:** cada grupo (CPU, RAM, GPU, discos, sensores) pode estar em OK, UNAVAILABLE ou ERROR. Os
estados mudam a cada coleta. Nenhum deles estima valores.
