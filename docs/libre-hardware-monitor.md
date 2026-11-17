# GPU e sensores com o LibreHardwareMonitor

O OSHI não fornece utilização da GPU, memória de vídeo em uso nem leitura confiável de temperatura e ventoinhas no
Windows (a integração dele com monitores de hardware só consulta o **Open Hardware Monitor** antigo, não o
LibreHardwareMonitor). Por isso o PCCheck lê esses dados do servidor web local do
[LibreHardwareMonitor](https://github.com/LibreHardwareMonitor/LibreHardwareMonitor).

## O que passa a aparecer

| Onde | Dado |
|---|---|
| Live Monitor → GPU | placa, utilização (%), memória de vídeo em uso e total, temperatura, gráfico |
| Live Monitor → Sensores | temperatura da CPU e da GPU, rotação das ventoinhas, gráfico de temperaturas |
| Health Check | a temperatura da CPU passa a ser avaliada com a leitura real |

## Como ativar (uma vez)

1. Baixe o LibreHardwareMonitor (versão **.NET Framework** ou a mais recente) e extraia a pasta.
2. Abra o `LibreHardwareMonitor.exe` **como administrador** (botão direito → *Executar como administrador*).
   Sem administrador, a maioria dos sensores da CPU e da placa-mãe não é lida.
3. No menu: **Options → Remote Web Server → Run**. A porta padrão é **8085**.
4. Confira no navegador: `http://127.0.0.1:8085/data.json` deve mostrar um JSON. Se o programa pedir usuário e
   senha, deixe a autenticação do servidor desativada (o PCCheck não envia credenciais).
5. Mantenha o LibreHardwareMonitor aberto (opcional: *Options → Start Minimized* e *Run On Windows Startup*).
6. Reinicie o backend. Em poucos segundos a aba Live Monitor mostra GPU e sensores.

## Configuração

Em `backend/src/main/resources/application.properties`:

```
pccheck.lhm.url=http://127.0.0.1:8085/data.json
```

- Mudou a porta no LibreHardwareMonitor? Ajuste aqui.
- Somente `localhost`/`127.0.0.1` são aceitos (o PCCheck não consulta outras máquinas).
- Em branco, a integração fica desativada e GPU/sensores voltam a "Sem leitura".

## Quando não há leitura

O sistema **nunca estima** valores. Cada situação tem uma mensagem própria no cartão da GPU:

| Mensagem | Causa provável |
|---|---|
| "O LibreHardwareMonitor não respondeu em ..." | programa fechado ou *Remote Web Server* desligado, ou porta diferente |
| "respondeu com o código HTTP ..." | autenticação do servidor ativada |
| "não informou nenhuma placa de vídeo" | driver da GPU ausente ou não suportado pelo programa |
| "sem leituras de uso, memória ou temperatura" | a placa foi detectada, mas o driver não expõe sensores |

## Detalhes técnicos

- `agent/.../lhm/LhmClient`: consulta `data.json` (tempo limite curto; leitura reaproveitada por 1 s).
- `agent/.../lhm/LhmParser`: percorre a árvore computador → hardware → grupo → sensor, converte textos como
  `"45,0 °C"` ou `"12.288,0 MB"` e funciona com versões novas (com `HardwareId`/`SensorId`/`Type`) e antigas.
- Com mais de uma GPU (por exemplo, integrada + dedicada), a principal é a de maior memória de vídeo.
- Temperatura da CPU: usa `CPU Package`/`Core (Tctl/Tdie)`; sem eles, a maior temperatura de núcleo válida.
