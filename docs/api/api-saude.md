# API de Health Check (Módulo B)

`GET http://127.0.0.1:8080/api/health-check`

Cada chamada faz uma nova coleta e avalia os indicadores disponíveis. O módulo **apenas informa**: não altera
configurações, não executa reparos e não faz testes de estresse. O backend escuta somente em `127.0.0.1`.

## Estrutura da resposta

| Campo | Descrição |
|---|---|
| `collectedAt` | Horário da avaliação (UTC, ISO-8601). |
| `source` | Origem dos dados (coleta local via OSHI, sem simulação). |
| `overall` | Maior severidade entre os alertas: `INFO`, `WARNING` ou `CRITICAL`. |
| `infoCount`, `warningCount`, `criticalCount` | Quantidade de alertas por severidade. |
| `thresholds` / `thresholdsSource` | Limites usados e a origem deles. |
| `alerts` | Lista de alertas, do mais grave para o menos grave. |

## Alerta

| Campo | Descrição |
|---|---|
| `id` | Identificador estável (`temperature:cpu`, `disk-space:<volume>`, `memory:usage`, `memory:commit`, `memory:paging`, `smart` (sem leitura) ou `smart:<n>` (disco físico n), `collection:<componente>`). |
| `component` | Componente avaliado. |
| `severity` | `INFO` (Informação), `WARNING` (Atenção) ou `CRITICAL` (Crítico). |
| `kind` | `NORMAL` (dentro do esperado), `PREVENTIVE` (indicador acima do limite; **não prova defeito**) ou `UNAVAILABLE` (dado indisponível; **não é falha física**). |
| `title`, `description` | Resumo e explicação em linguagem simples. |
| `observedValue`, `threshold` | Valor observado e limite aplicado, com unidade (`null` quando não se aplica). |
| `timestamp` | Horário da avaliação. |

## Verificações

| Verificação | Regra |
|---|---|
| Temperatura (CPU) | `>= 95 °C` Crítico; `>= 80 °C` Atenção; senão normal. Sem sensor: Informação / `UNAVAILABLE`. A temperatura nunca é estimada. |
| Espaço em disco | Por volume: espaço livre `< 5%` Crítico; `< 15%` Atenção; senão normal. Mostra espaço livre, total e percentual. Imagens somente leitura (`squashfs`, `iso9660`, `udf`, `cdfs`) são ignoradas. |
| Uso de RAM | `>= 85%` gera **aviso** de uso elevado (Atenção). Nunca é Crítico nem defeito. |
| Memória comprometida (`memory:commit`) | Windows: `>= 90%` do limite (RAM + arquivo de paginação) gera Atenção; perto do limite, alocações podem falhar. Linux: só informativo, porque o kernel permite ultrapassar o limite (overcommit). |
| Paginação (`memory:paging`) | Média das páginas gravadas em disco nas últimas 30 amostras do Live Monitor (~1 min). Atenção somente com média `>= 1 MiB/s` **e** RAM `>= 85%` (pressão de memória). Gravação sem RAM cheia é informativa. Menos de 10 amostras: Informação / `UNAVAILABLE`. |
| Saúde dos discos (`smart:<n>`) | Lida pelo leitor de sensores (exige administrador). Um alerta por disco: vida útil restante `<= 5%` Crítico, `<= 20%` Atenção; temperatura `>= 55 °C` (HDD) ou `>= 70 °C` (SSD ou tipo desconhecido) Atenção; senão "Indicadores do disco normais", que não garante ausência de falhas (setores realocados e erros de leitura não são lidos). Texto e limites citam só o que o disco informou (HDDs não informam vida útil); disco sem vida útil nem temperatura fica `UNAVAILABLE` ("não avaliado"). Valor observado: vida útil, temperatura, horas ligado e dados gravados. Sem leitura: um alerta `smart` Informação / `UNAVAILABLE` com o motivo. |
| Coleta incompleta | Componentes (SO, CPU, GPU, placa-mãe) sem leitura geram Informação / `UNAVAILABLE`. |

Se uma verificação falhar internamente, vira um alerta informativo (`diagnostic-error:<nome>`) e as demais continuam.

## Origem e configuração dos limites

Os valores padrão são **limites genéricos e conservadores definidos pelo projeto**; não são limites oficiais do
fabricante do seu processador ou disco. Ajuste-os em `backend/src/main/resources/application.properties`:

```
nexcore.health.cpu-temp-warning-c=80
nexcore.health.cpu-temp-critical-c=95
nexcore.health.disk-free-warning-percent=15
nexcore.health.disk-free-critical-percent=5
nexcore.health.ram-used-warning-percent=85
nexcore.health.memory-pressure.commit-warning-percent=90
nexcore.health.memory-pressure.page-out-warning-bytes-per-sec=1048576
nexcore.health.memory-pressure.window-samples=30
nexcore.health.memory-pressure.min-samples=10
nexcore.health.disk.life-warning-percent=20
nexcore.health.disk.life-critical-percent=5
nexcore.health.disk.hdd-temp-warning-c=55
nexcore.health.disk.ssd-temp-warning-c=70
```

Limites incoerentes (por exemplo, crítico menor que atenção) impedem o backend de iniciar, com mensagem explicando o erro.
