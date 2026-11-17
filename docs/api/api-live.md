# API do Live Monitor (Módulo C — coleta periódica)

O backend coleta métricas em **segundo plano**, a cada `pccheck.monitor.interval-ms` (padrão 2000 ms), e guarda as
últimas `pccheck.monitor.history-size` amostras (padrão 300) **somente em memória**; ao reiniciar o backend, o histórico
é perdido. As requisições HTTP apenas leem esse histórico (não disparam coleta). O backend escuta somente em `127.0.0.1`.

| Endpoint | Descrição |
|---|---|
| `GET /api/live` | Estado da coleta e a amostra mais recente (`latest` é `null` até a primeira coleta terminar). |
| `GET /api/live/history?limit=N` | Últimas `N` amostras (padrão: todas as mantidas), da mais antiga para a mais recente. |

## Estado da coleta (`status`)

| Campo | Descrição |
|---|---|
| `running` | O agendador está ativo. |
| `collecting` | Há uma coleta em andamento neste instante. |
| `stalled` | A coleta em andamento está demorando muito mais que o esperado (possível bloqueio). |
| `intervalMs`, `historyCapacity`, `sampleCount` | Intervalo, capacidade e quantidade atual de amostras. |
| `lastSuccessAt` | Horário da última amostra bem-sucedida (`null` se nenhuma). |
| `consecutiveFailures`, `lastError` | Falhas seguidas e a última mensagem de erro (`null` se a última coleta funcionou). |

## Amostra

`collectedAt` (UTC), `durationMs` (tempo gasto na coleta) e um grupo por métrica, cada um com `status`
(`OK`, `UNAVAILABLE` ou `ERROR`), `message` e `data`. **Campo `null` dentro de `data` = sem leitura.**
A falha de um grupo (por exemplo, sensores) não afeta os demais.

| Grupo | Campos de `data` |
|---|---|
| `cpu` | `totalPercent` e `perCorePercent` (lista por processador lógico; elemento `null` = sem leitura daquele núcleo). |
| `memory` | `totalBytes`, `availableBytes`, `usedBytes`, `usedPercent`. |
| `gpu` | `utilizationPercent`, `vramUsedBytes`, `vramTotalBytes`, `temperatureCelsius` e `name` da placa principal (a de maior memória de vídeo), lidos pelo **leitor de sensores interno** (ver [sensores.md](../sensores.md)). `UNAVAILABLE`, com o motivo, se não houver leitura. |
| `disks` | `drives` (`name`, `readBytesPerSecond`, `writeBytesPerSecond`, `busyPercent`) e `volumes` (espaço livre, como no inventário); cada lista pode ser `null` independentemente. |
| `sensors` | `cpuTemperatureCelsius`, `gpuTemperatureCelsius` e `fanSpeedsRpm` (somente leituras válidas). Vêm do leitor de sensores interno (CPU e ventoinhas da placa-mãe exigem administrador e o driver PawnIO); sem ele, usam o OSHI como alternativa. `UNAVAILABLE`, com o motivo, se nenhuma fonte expõe sensores. |

## Como as métricas são calculadas

- **CPU**: diferença entre os contadores de tempo de CPU da amostra atual e da anterior (não bloqueia a coleta). A primeira
  amostra mede desde a criação do coletor.
- **Disco (leitura/escrita/ocupado)**: diferença entre contadores acumulados entre duas amostras. Na primeira amostra de
  cada unidade ainda não há valor anterior, então a taxa aparece como `null` (sem leitura). Contador que diminui (reinício)
  também vira `null`. O `busyPercent` é limitado a 100.
- Nenhum valor é estimado ou simulado.

## Garantias de robustez

- **Sem acúmulo de tarefas**: uma única thread executa uma coleta por vez e só agenda a próxima depois que a anterior termina
  (`scheduleWithFixedDelay`). Se uma coleta travar, nenhuma outra é empilhada; `status.stalled` indica o problema.
- **Exceções tratadas**: qualquer falha é registrada em `status.lastError`/`consecutiveFailures` e a coleta seguinte tenta de novo.
- **Histórico limitado**: ao encher, a amostra mais antiga é descartada.

## Configuração (`application.properties`)

```
pccheck.monitor.interval-ms=2000
pccheck.monitor.history-size=300
pccheck.sensors.enabled=true
pccheck.sensors.elevate=true
pccheck.sensors.host-path=
```

`pccheck.sensors.*` controla o leitor de sensores interno (ver [sensores.md](../sensores.md)): ligar/desligar, pedir permissão de administrador ao iniciar e o caminho do executável (vazio procura em `sensor-host/dist/`).

Intervalo mínimo de 500 ms; histórico entre 10 e 3600 amostras. Valores fora da faixa impedem o backend de iniciar, com
mensagem explicativa. Aumentar o intervalo reduz o consumo de CPU.
