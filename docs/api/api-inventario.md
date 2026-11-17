# API de inventário (Módulo A)

`GET http://127.0.0.1:8080/api/inventory`

O backend escuta apenas em `127.0.0.1` (sem acesso remoto). Cada chamada faz uma nova coleta, portanto a
atualização manual da interface é uma nova requisição. A coleta de CPU leva cerca de 500 ms (medição de uso).

## Estrutura da resposta

| Campo | Descrição |
|---|---|
| `collectedAt` | Horário da coleta (UTC, ISO-8601). |
| `source` | Origem dos dados (coleta local via OSHI). |
| `os`, `cpu`, `memory`, `gpu`, `motherboard`, `storage` | Um objeto por componente, com `status`, `message` e `data`. |

Cada componente tem estado próprio; a falha de um não impede a leitura dos demais:

| `status` | Significado | `data` |
|---|---|---|
| `OK` | Coleta concluída. | Dados do componente. |
| `UNAVAILABLE` | O sistema não forneceu a informação (não indica falha física). | `null` |
| `ERROR` | A coleta lançou uma exceção (detalhe em `message`). | `null` |

Dentro de `data`, **campo `null` significa "não disponível"**. Valores ausentes, inválidos ou sentinelas da biblioteca
(como `-1`) e textos genéricos de fabricantes (como "To be filled by O.E.M.") nunca são apresentados como medição.

## Campos por componente

| Componente | Campos |
|---|---|
| `os` | `name`, `version`, `buildNumber`, `architecture`, `uptimeSeconds` |
| `cpu` | `name`, `vendor`, `physicalCores`, `logicalProcessors`, `nominalFrequencyHz`, `maxFrequencyHz`, `usagePercent` |
| `memory` | `totalBytes`, `availableBytes`, `usedBytes`, `usedPercent` |
| `gpu` | Lista de placas: `name`, `vendor`, `deviceId`, `vramBytes` |
| `motherboard` | `manufacturer`, `model`, `version`, `serialNumber`, `systemManufacturer`, `systemModel`, `notReportedFields` (nomes dos campos que o firmware deixou com texto genérico, como "To be filled by O.E.M."; campo nulo fora dessa lista = não disponível) |
| `storage` | `drives` (`name`, `model`, `sizeBytes`) e `volumes` (`mountPoint`, `label`, `fileSystem`, `totalBytes`, `freeBytes`, `usedBytes`, `usedPercent`); cada lista pode ser `null` independentemente |

## Observações

- `vramBytes` é a memória de vídeo informada pelo sistema; não distingue dedicada de compartilhada.
- `freeBytes` é o espaço disponível ao usuário; `usedBytes = totalBytes - freeBytes`.
- O tipo da unidade (SSD/HDD) não é informado.
- Se a biblioteca OSHI não puder ser inicializada, todos os componentes retornam `ERROR` e a próxima chamada tenta de novo.
