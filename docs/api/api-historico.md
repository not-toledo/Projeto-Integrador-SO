# API do Histórico (coleta agendada)

O Live Monitor guarda poucos minutos e só enquanto o backend está aberto. O **histórico** registra uma coleta a cada
N minutos **mesmo com o PCCheck fechado**, por meio de uma tarefa no agendador do sistema operacional.

## Como funciona

1. `scripts/agendar-coleta.bat instalar [minutos]` (Windows, `schtasks`) ou `scripts/agendar-coleta.sh instalar
   [minutos]` (Linux, `cron`) copia o agente para `<dados>/bin/` e cria a tarefa (padrão: a cada 15 minutos).
2. A cada execução, o SO inicia um processo `java -jar pccheck-agent.jar --registrar-coleta` (no Windows, `javaw`,
   sem janela). O agente coleta, acrescenta **uma linha JSON** ao arquivo do dia e termina. O código de saída (0 =
   sucesso, 1 = falha) fica registrado no agendador ("Último resultado" no Windows).
3. O backend lê os mesmos arquivos em `GET /api/history`, e a página **Histórico** mostra os gráficos.

| Item | Detalhe |
|---|---|
| Pasta `<dados>` | Variável de ambiente `PCCHECK_DATA_DIR`; senão `%LOCALAPPDATA%\PCCheck` (Windows) ou `${XDG_STATE_HOME:-~/.local/state}/pccheck` (Linux). Consulte com `java -jar pccheck-agent.jar --onde`. |
| Arquivos | `<dados>/coletas/coletas-AAAA-MM-DD.jsonl`: um arquivo por dia (rotação diária), uma coleta por linha. |
| Retenção | Arquivos com mais de 7 dias são apagados a cada coleta (`PCCHECK_RETENCAO_DIAS`, de 1 a 365). Só arquivos com esse padrão de nome são apagados. |
| Concorrência | Cada linha é gravada sob trava exclusiva do arquivo (`FileChannel.lock`); o leitor tenta de novo se o arquivo estiver travado e ignora linhas incompletas. |
| Permissões | No Linux, pastas criadas pelo PCCheck ficam com `700` e arquivos com `600` (só o dono lê). Pastas já existentes não são alteradas. No Windows, a pasta do perfil já é restrita ao usuário. |
| Privilégios | Não exige administrador. Por isso as temperaturas não entram no histórico (o leitor de sensores precisa de elevação). |

No Windows, `PCCHECK_DATA_DIR` e `PCCHECK_RETENCAO_DIAS` só valem para a tarefa se definidas para o usuário com
`setx` (a tarefa não herda variáveis definidas apenas no terminal). No Linux, o script grava as variáveis definidas
na própria linha do cron.

## `GET /api/history?days=N`

`N` de 1 a 30 (padrão 7). Fora disso: HTTP 400 com `{"error": "..."}`.

| Campo | Descrição |
|---|---|
| `directory` | Pasta lida. No backend, pode ser trocada por `pccheck.history.data-dir`. |
| `days`, `count` | Período consultado e quantidade de coletas. |
| `lastCollectedAt` | Última coleta do período (UTC) ou `null`. |
| `scheduler.state` | `INSTALLED`, `NOT_INSTALLED` ou `UNKNOWN` (agendador indisponível). O backend executa `schtasks /Query /TN PCCheck\Coleta /FO CSV /NH` (Windows) ou `crontab -l` (Linux) com tempo limite de 5 s. |
| `scheduler.details` | Próxima execução e estado (Windows) ou a linha do cron (Linux). |
| `records` | Coletas em ordem cronológica: `collectedAt`, `cpuPercent`, `ramUsedPercent`, `commitPercent`, `swapUsedPercent`, `uptimeSeconds` e `volumes` (`mountPoint`, `totalBytes`, `freeBytes`). Campo `null` = sem leitura. |
| `problems` | Arquivos que não puderam ser lidos; as demais coletas continuam na resposta. |

Na interface, intervalos sem coleta (computador desligado, por exemplo) aparecem como lacunas nos gráficos: nenhum
valor é preenchido.
