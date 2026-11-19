# Distribuição do NEXCORE (Windows)

Esta pasta gera a versão instalável do NEXCORE. O usuário final **não** precisa de Java, Maven, Node.js nem .NET SDK:
tudo o que o NEXCORE usa vai dentro da instalação.

## Como gerar

Na raiz do projeto, com o backend de desenvolvimento **fechado** (o build recompila `backend/target`):

```
powershell -ExecutionPolicy Bypass -File installer\build.ps1            (com todos os testes)
powershell -ExecutionPolicy Bypass -File installer\build.ps1 -PularTestes
```

Ferramentas necessárias só na máquina de build: JDK 21 (inclui `jpackage` e `jlink`), Maven 3.9+, Node.js 22.22.2+,
.NET SDK 8+ e Inno Setup 6.7+ (`winget install JRSoftware.InnoSetup`). Tudo o que é gerado fica em `build\` (ignorado
pelo Git). Resultado: `build\instalador\NEXCORE-Setup-<versão>.exe` e `NEXCORE-Setup-<versão>.exe.sha256`.

| Passo | O que faz | Saída |
|---|---|---|
| Interface | `npm test` e `npm run build` | `frontend\dist` |
| Leitor de sensores | `scripts\build-sensor-host.bat` | `sensor-host\dist\NEXCORE.SensorHost.exe` |
| Backend | `mvn -pl backend -am -Pdistribuicao package`: a interface entra no jar como recurso estático e a entrada passa a ser o `NexcoreLauncher` | `backend\target\nexcore-backend-<versão>.jar` |
| Executável | `jpackage --type app-image`: `NEXCORE.exe` sem janela de terminal, com o ícone oficial, e um Java reduzido pelo `jlink` (só os módulos usados) | `build\app-image\NEXCORE` |
| PawnIO (opcional) | Baixa o instalador oficial 2.2.0 do GitHub do autor e confere o SHA-256 publicado; se não conferir, o build para. `-SemPawnIO` gera sem ele | `build\deps` |
| Instalador | `ISCC installer\nexcore.iss` (Inno Setup) | `build\instalador` |

## O instalador (`nexcore.iss`)

- **Visual:** modo escuro, logotipo oficial na tela inicial e no canto das páginas (imagens de
  `branding\instalador`, em várias escalas de tela), ícone oficial no instalador, no programa e nos atalhos.
- **Onde instalar:** a primeira janela pergunta **"Instalar para todos os usuários"** (`C:\Program Files\NEXCORE`, pede
  administrador; recomendado) ou **"Instalar só para mim"** (`%LOCALAPPDATA%\Programs\NEXCORE`, sem administrador). A
  pasta pode ser alterada.
- **Requisitos:** Windows 10 1903 ou mais novo, 64 bits; espaço em disco (calculado e mostrado pelo instalador);
  .NET Framework 4.8 para o leitor de sensores (se faltar, avisa e permite continuar sem sensores).
- **Atalhos:** Menu Iniciar e, se marcado, Área de Trabalho. Registro em "Aplicativos instalados" (nome, versão,
  ícone, links do projeto).
- **PawnIO (opcional):** só aparece se o driver ainda não estiver instalado. Abre o instalador **oficial** do PawnIO,
  com a tela e os termos dele (não é instalado em silêncio).
- **Atualização:** o mesmo identificador de aplicativo (`AppId`) faz um instalador novo atualizar a instalação
  existente, na mesma pasta. O NEXCORE aberto é fechado antes de trocar os arquivos. A configuração e os dados em
  `%LOCALAPPDATA%\NEXCORE` não são tocados.
- **Desinstalação:** fecha o NEXCORE, remove arquivos, atalhos e o registro, remove a tarefa `NEXCORE\Coleta` se
  existir e **pergunta** se deve apagar configurações, logs e histórico (padrão: manter). O PawnIO não é removido.
- **Log da instalação:** `%TEMP%\Setup Log *.txt`.
- **Instalação silenciosa** (testes, implantação): `NEXCORE-Setup-<versão>.exe /VERYSILENT /CURRENTUSER` (ou
  `/ALLUSERS`), `/TASKS="desktopicon"`. Desinstalação: `unins000.exe /VERYSILENT` (mantém os dados).
- **Aviso do SmartScreen:** sem certificado de assinatura de código, o Windows mostra "Editor desconhecido" ao abrir
  o instalador. Para conferir que o arquivo é o oficial, compare o SHA-256 com o do arquivo `.sha256` publicado.

## O que vai na instalação

```
NEXCORE\
├── NEXCORE.exe          lançador (ícone oficial, sem terminal)
├── app\                 nexcore-backend-<versão>.jar (API + interface) e NEXCORE.cfg
├── runtime\             Java 21 reduzido (cerca de 60 MB)
└── sensor-host\         NEXCORE.SensorHost.exe + LibreHardwareMonitorLib (usa o .NET Framework 4.8 do Windows)
```

Dados de cada usuário, que nunca são apagados nem sobrescritos por atualizações:

```
%LOCALAPPDATA%\NEXCORE\
├── config\nexcore.properties   configuração (porta, aviso de administrador, limites); criado comentado na 1ª vez
├── logs\nexcore.log            log do NEXCORE (até 5 arquivos de 5 MB); logs\inicializacao.log se falhar ao abrir
└── coletas\                    histórico da coleta agendada (opcional)
```

## Como o NEXCORE.exe inicia

1. Garante uma instância por usuário (trava `nexcore.lock`). Se já estiver aberto, só abre o navegador.
2. Usa a porta **47820** em `127.0.0.1` (a de desenvolvimento é 8080). Se outro programa usar a porta, mostra uma
   janela explicando como trocar (`server.port` em `config\nexcore.properties`).
3. Inicia a API e a interface no mesmo processo, e o leitor de sensores da pasta `sensor-host` (o Windows pede
   permissão de administrador; recusando, o NEXCORE funciona sem temperaturas).
4. Abre o navegador padrão e mostra o ícone na bandeja: **Abrir NEXCORE**, **Abrir pasta de dados** e **Sair**.

No Windows, o lançador do `jpackage` aparece como dois processos `NEXCORE.exe` (o lançador e o Java). Ao sair, o
leitor de sensores também é encerrado (ele acompanha o processo do NEXCORE).
