# GPU e sensores: leitor de sensores do PCCheck

O OSHI identifica bem o hardware, mas no Windows não fornece temperatura da CPU, ventoinhas nem telemetria da GPU.
Esses dados só podem ser lidos com um driver de kernel e permissão de administrador: é uma restrição do Windows, não
do Java nem do OSHI. Programas como HWiNFO, HWMonitor e Core Temp trazem o próprio driver pelo mesmo motivo.

Por isso o PCCheck tem um **leitor de sensores interno** (`sensor-host/`): um executável pequeno (~3 MB) que usa a
biblioteca [LibreHardwareMonitorLib](https://github.com/LibreHardwareMonitor/LibreHardwareMonitor) (licença MPL-2.0).
O backend inicia, controla e encerra esse leitor sozinho. **O usuário não instala nem abre nenhum outro programa.**

## O que aparece

| Onde | Dado | Precisa de administrador? |
|---|---|---|
| Live Monitor → GPU | placa, utilização (%), memória de vídeo em uso e total, temperatura | não |
| Live Monitor → Sensores | temperatura da GPU, ventoinha da GPU | não |
| Live Monitor → Sensores | temperatura da CPU, ventoinhas da placa-mãe | **sim** (e driver PawnIO) |
| Health Check | temperatura da CPU avaliada com a leitura real | **sim** (e driver PawnIO) |
| Health Check | saúde dos discos (SMART): vida útil restante, temperatura, horas ligado, dados gravados | **sim** (não precisa do PawnIO) |
| Hardware Overview | tipo do disco (SSD/HDD) e barramento | não (vem do WMI `MSFT_PhysicalDisk`, não do leitor) |

## Como funciona

1. Ao subir, o backend inicia `sensor-host/dist/PCCheck.SensorHost.exe` e o Windows mostra o aviso de administrador
   (UAC). Aceite para ter temperatura da CPU e ventoinhas.
2. Se o aviso for recusado, o leitor roda sem elevação: a GPU continua com leitura e CPU/ventoinhas aparecem como
   "Sem leitura" com a explicação. Para tentar de novo, reinicie o backend.
3. A comunicação é por um **pipe nomeado local** com nome aleatório, somente leitura para o PCCheck. Nenhuma porta de
   rede é aberta.
4. O leitor termina sozinho quando o backend termina (inclusive se for fechado à força). Se ele cair, o backend o
   reinicia (até 3 vezes).

## Requisitos

- **Para executar:** Windows 10 (1903+) ou 11 com .NET Framework 4.8, que já vem no sistema.
- **Driver PawnIO** (para CPU e ventoinhas da placa-mãe): driver assinado usado pela LibreHardwareMonitorLib no lugar
  do antigo WinRing0, que o Windows Defender bloqueia por ser vulnerável. Instalação única pelo site
  <https://pawnio.eu> (modo silencioso: `PawnIOSetup.exe -install -silent`). Um instalador do PCCheck deve incluí-lo.
- **Para compilar o leitor:** .NET SDK 8 ou superior. `scripts\run-backend.bat` compila na primeira execução; para
  compilar manualmente use `scripts\build-sensor-host.bat`.

## Configuração

Em `backend/src/main/resources/application.properties`:

| Chave | Padrão | Função |
|---|---|---|
| `pccheck.sensors.enabled` | `true` | Liga/desliga a leitura de sensores. |
| `pccheck.sensors.elevate` | `true` | Pede administrador ao iniciar. Com `false`, só os dados que não exigem elevação. |
| `pccheck.sensors.host-path` | (vazio) | Caminho do `PCCheck.SensorHost.exe`. Vazio: procura em `sensor-host/dist/`. |

## Quando não há leitura

O sistema **nunca estima** valores. Cada situação tem uma mensagem própria:

| Mensagem | Causa |
|---|---|
| "O leitor de sensores (...) não foi encontrado" | leitor não compilado: rode `scripts\build-sensor-host.bat` |
| "...exigem permissão de administrador" | o aviso do Windows foi recusado; reinicie o backend e aceite |
| "O driver PawnIO não está instalado" | instale o PawnIO uma vez e reinicie o backend |
| "Iniciando o leitor de sensores" / "está carregando" | primeiros segundos após iniciar o backend |
| "O leitor de sensores não encontrou discos com dados SMART" | o disco ou o controlador (ex.: alguns adaptadores USB e RAID) não repassa o SMART |
| "Nenhuma placa de vídeo com sensores foi encontrada" | driver da GPU ausente ou não suportado |

## Detalhes técnicos

- `sensor-host/Program.cs`: abre a LibreHardwareMonitorLib e, a cada conexão no pipe, escreve uma linha JSON com os
  sensores (tipos e números em cultura invariável). Também informa se está elevado e se o PawnIO está instalado.
- `agent/.../sensors/SensorHostClient`: inicia o leitor (com `ShellExecuteEx` "runas" via JNA, que já vem com o OSHI),
  trata recusa da elevação, reinício e cache de 1 s.
- `agent/.../sensors/SensorHostParser`: escolhe os sensores. Temperatura da CPU: `CPU Package`/`Core (Tctl/Tdie)`; sem
  eles, a maior temperatura de núcleo válida. Com mais de uma GPU, a principal é a de maior memória de vídeo.
- Testes: `SensorHostParserTest` e `SensorHostClientTest` (sem processos reais).
