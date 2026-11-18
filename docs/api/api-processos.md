# API de Processos

Lista os processos em execução, como o sistema operacional os informa, com o uso de CPU medido num intervalo curto.
É **somente leitura**: não existe rota para encerrar, suspender ou mudar a prioridade de um processo.

## Como funciona

1. O backend lê a lista de processos pelo OSHI (`OperatingSystem.getProcesses()`), espera 1 s e lê de novo.
2. Para cada PID presente nas duas leituras, o uso de CPU é o tempo de CPU ganho (modo usuário + núcleo) dividido
   pelo tempo decorrido e pelo número de processadores lógicos: **% da CPU inteira** no último segundo.
3. Um PID que aparece só na segunda leitura, ou que foi reaproveitado por outro processo (início diferente), fica
   com `cpuPercent: null`: não há medição, e o valor não é preenchido com zero.
4. O processo ocioso do sistema (PID 0) é omitido: no Windows ele representa o tempo livre da CPU.
5. Pedidos que chegam até 2 s depois de uma leitura recebem a mesma leitura (várias abas não multiplicam o custo).

| Item | Fonte no Windows | Fonte no Linux |
|---|---|---|
| Lista, nome, PID, PID pai, threads | Contadores de desempenho do objeto *Process* (`HKEY_PERFORMANCE_DATA`) e `WTSEnumerateProcessesEx` (classes `ProcessPerformanceData` e `ProcessWtsData` do OSHI) | `/proc/<pid>/stat` e `/proc/<pid>/status` |
| Tempo de CPU | Tempos de usuário e de kernel das mesmas fontes | Campos `utime` e `stime` de `/proc/<pid>/stat` |
| Memória (`memoryBytes`) | Conjunto de trabalho (*working set*) | RSS |
| Prioridade | Prioridade base (8 = normal) | Prioridade do kernel (20 + nice) |
| Estado | Sempre informado como `RUNNING` | `R`, `S`, `D`, `Z`, `T`... de `/proc/<pid>/stat` |
| Usuário | Dono pelo token do processo (`OpenProcessToken`); processos do sistema só com administrador | Dono do processo (UID) |

## `GET /api/processes`

```json
{
  "collectedAt": "2026-11-18T22:51:42.545Z",
  "sampleMillis": 1000,
  "logicalProcessors": 8,
  "totalMemoryBytes": 16035831808,
  "processCount": 262,
  "threadCount": 3397,
  "processes": [
    {
      "pid": 3480, "parentPid": 6472, "name": "brave", "user": "aluno", "state": "RUNNING", "priority": 8,
      "threads": 66, "cpuPercent": 12.03, "memoryBytes": 250011648, "virtualBytes": 1791385600,
      "startTime": "2026-11-18T20:31:44.729Z", "self": false
    }
  ],
  "error": null,
  "osFamily": "windows"
}
```

| Campo | Significado |
|---|---|
| `processCount`, `threadCount` | Totais informados pelo SO; `null` se não informou. |
| `cpuPercent` | % da CPU inteira no intervalo (0 a 100); `null` sem medição. |
| `user` | `null` quando o SO não informa o dono. |
| `startTime` | `null` quando o SO não informa o início. |
| `self` | `true` no processo do próprio backend do PCCheck. |
| `error` | Motivo quando a leitura falhou; a lista vem vazia e a interface mantém a última leitura boa. |
| `osFamily` | `windows` ou `linux`: define a escala de prioridade e se o estado tem significado. |

A lista vem ordenada por CPU e depois por memória. A interface agrupa por nome (como o Gerenciador de Tarefas),
filtra, ordena por coluna e atualiza a cada 5 s, sempre esperando a leitura anterior terminar.

## Limitações

- No Windows, o estado de todos os processos é `RUNNING`: o sistema não expõe o estado por processo como o Linux.
  Por isso a interface não mostra a coluna Estado no Windows.
- Sem permissão de administrador, o dono dos processos de sistema não é informado (`user: null`).
- A soma da memória dos processos pode passar da memória em uso do sistema: páginas compartilhadas entre processos
  entram no conjunto de trabalho de cada um.
- O uso de CPU é uma medição de 1 s; picos mais curtos podem não aparecer.

Verificação com o backend aberto: `tests/verificar-processos.sh`.
