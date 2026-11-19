; Instalador do NEXCORE (Inno Setup 6.7+). Gerado por installer\build.ps1, que define:
;   Version   versão do produto (do pom.xml)
;   AppImage  pasta do jpackage (build\app-image\NEXCORE)
;   PawnIO    instalador oficial do driver PawnIO, com SHA-256 já conferido (opcional)
; Compilar à mão: ISCC.exe /DVersion=4.0.0 /DAppImage=... /DPawnIO=... installer\nexcore.iss

#ifndef Version
  #error Defina /DVersion (use installer\build.ps1)
#endif
#ifndef AppImage
  #error Defina /DAppImage (use installer\build.ps1)
#endif

#define Root ".."
#define RepoUrl "https://github.com/not-toledo/Projeto-Integrador-SO"

[Setup]
; Identificador fixo: novas versões atualizam a mesma instalação (preservando os dados do usuário).
AppId={{6E3C2F4A-8B1D-4C7E-9A52-0E4D7B1F3C9A}
AppName=NEXCORE
AppVersion={#Version}
AppVerName=NEXCORE {#Version}
AppPublisher=NEXCORE
AppPublisherURL={#RepoUrl}
AppSupportURL={#RepoUrl}/issues
AppUpdatesURL={#RepoUrl}/releases
AppComments=Monitoramento local de hardware
VersionInfoVersion={#Version}.0
VersionInfoProductName=NEXCORE
VersionInfoProductVersion={#Version}
VersionInfoDescription=Instalador do NEXCORE
VersionInfoCompany=NEXCORE

; "Para todos os usuários" (Program Files, pede administrador) é o padrão; a janela inicial permite escolher
; "Somente para mim" (sem administrador, em %LOCALAPPDATA%\Programs).
PrivilegesRequired=admin
PrivilegesRequiredOverridesAllowed=dialog commandline
DefaultDirName={autopf}\NEXCORE
DefaultGroupName=NEXCORE
DisableProgramGroupPage=yes
; Tela inicial com o logotipo, o nome, a versão e a descrição (o Inno Setup 6 a esconde por padrão).
DisableWelcomePage=no
UsePreviousAppDir=yes

; Windows 10 1903 ou mais novo, 64 bits (o leitor de sensores usa o .NET Framework 4.8, que vem nessas versões).
MinVersion=10.0.18362
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

; Identidade visual: fundo escuro, logotipo oficial (gerado por branding\gerar-recursos.ps1).
WizardStyle=modern dark
WizardImageBackColor=#000000
WizardBackColor=#0b0b0c
WizardImageFile={#Root}\branding\instalador\wizard-100.bmp,{#Root}\branding\instalador\wizard-125.bmp,{#Root}\branding\instalador\wizard-150.bmp,{#Root}\branding\instalador\wizard-175.bmp,{#Root}\branding\instalador\wizard-200.bmp
WizardSmallImageFile={#Root}\branding\instalador\wizard-small-55.bmp,{#Root}\branding\instalador\wizard-small-64.bmp,{#Root}\branding\instalador\wizard-small-83.bmp,{#Root}\branding\instalador\wizard-small-92.bmp,{#Root}\branding\instalador\wizard-small-110.bmp,{#Root}\branding\instalador\wizard-small-119.bmp,{#Root}\branding\instalador\wizard-small-138.bmp
SetupIconFile={#Root}\branding\nexcore.ico
UninstallDisplayIcon={app}\NEXCORE.exe
UninstallDisplayName=NEXCORE

OutputDir={#Root}\build\instalador
OutputBaseFilename=NEXCORE-Setup-{#Version}
Compression=lzma2/ultra64
SolidCompression=yes
; Fecha o NEXCORE aberto antes de substituir arquivos (atualização/reinstalação).
CloseApplications=yes
RestartApplications=no
; Log da instalação em %TEMP%\Setup Log *.txt (útil para diagnosticar falhas).
SetupLogging=yes

[Languages]
Name: "ptbr"; MessagesFile: "compiler:Languages\BrazilianPortuguese.isl"

[Messages]
ptbr.WelcomeLabel1=Bem-vindo ao NEXCORE {#Version}
ptbr.WelcomeLabel2=Monitoramento local de hardware: identifica os componentes, acompanha a saúde e o uso do computador em tempo real, lista os processos e compara o seu hardware com os requisitos dos jogos.%n%nTudo é lido do próprio computador, sem valores simulados. Não é preciso instalar Java, Node.js nem outras ferramentas.
ptbr.FinishedLabel=O NEXCORE foi instalado. Ele abre no seu navegador padrão e fica com um ícone na bandeja do Windows (ao lado do relógio), com as opções Abrir e Sair.

[Tasks]
Name: "desktopicon"; Description: "Criar atalho na Área de Trabalho"; GroupDescription: "Atalhos:"
#ifdef PawnIO
Name: "pawnio"; Description: "Instalar o driver PawnIO (necessário para temperatura da CPU e ventoinhas). Abre o instalador oficial do PawnIO, com os termos dele."; GroupDescription: "Sensores:"; Check: not PawnIOInstalled
#endif

[Files]
Source: "{#AppImage}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#Root}\installer\TERCEIROS.txt"; DestDir: "{app}"; Flags: ignoreversion
#ifdef PawnIO
Source: "{#PawnIO}"; DestDir: "{tmp}"; DestName: "PawnIO_setup.exe"; Flags: deleteafterinstall; Tasks: pawnio
#endif

[Icons]
Name: "{autoprograms}\NEXCORE"; Filename: "{app}\NEXCORE.exe"; Comment: "Monitoramento local de hardware"
Name: "{autodesktop}\NEXCORE"; Filename: "{app}\NEXCORE.exe"; Comment: "Monitoramento local de hardware"; Tasks: desktopicon

[Run]
#ifdef PawnIO
; Instalador oficial do PawnIO, com a tela e os termos dele (os autores esperam que o próprio usuário aceite).
Filename: "{tmp}\PawnIO_setup.exe"; Description: "Driver PawnIO"; StatusMsg: "Abrindo o instalador do PawnIO..."; Flags: shellexec waituntilterminated; Tasks: pawnio
#endif
; Abre o NEXCORE como o usuário que iniciou a instalação (não como administrador): os dados ficam no perfil dele.
Filename: "{app}\NEXCORE.exe"; Description: "Abrir o NEXCORE agora"; Flags: nowait postinstall skipifsilent runasoriginaluser

[Code]
const
  NetFx48Release = 528040;

function PawnIOInstalled: Boolean;
begin
  Result := RegKeyExists(HKLM64, 'SYSTEM\CurrentControlSet\Services\PawnIO')
    or RegKeyExists(HKLM64, 'SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\PawnIO');
end;

function NetFx48Installed: Boolean;
var
  Release: Cardinal;
begin
  Result := RegQueryDWordValue(HKLM64, 'SOFTWARE\Microsoft\NET Framework Setup\NDP\v4\Full', 'Release', Release)
    and (Release >= NetFx48Release);
end;

{ Encerra o NEXCORE aberto (os dois processos do lançador). O leitor de sensores sai junto com ele. }
procedure StopNexcore;
var
  Code: Integer;
begin
  Exec(ExpandConstant('{sys}\taskkill.exe'), '/IM NEXCORE.exe /T /F', '', SW_HIDE, ewWaitUntilTerminated, Code);
end;

function InitializeSetup: Boolean;
begin
  Result := True;
  { Linhas não podem começar com #: o pré-processador do Inno as trata como diretivas. }
  if not NetFx48Installed then
    Result := MsgBox('O .NET Framework 4.8 não foi encontrado. O NEXCORE funciona, mas sem o leitor de sensores ' +
      '(temperaturas, ventoinhas e saúde dos discos).' + #13#10#13#10 + 'O .NET Framework 4.8 vem com o ' +
      'Windows 10 1903 ou mais novo e pode ser instalado pelo Windows Update.' + #13#10#13#10 +
      'Continuar a instalação?', mbConfirmation, MB_YESNO) = IDYES;
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
begin
  StopNexcore;
  Result := '';
end;

function InitializeUninstall: Boolean;
begin
  StopNexcore;
  Result := True;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  DataDir: String;
  Code: Integer;
begin
  if CurUninstallStep = usPostUninstall then
  begin
    { Tarefa da coleta agendada (criada pelos scripts do projeto), se existir: sem o NEXCORE ela não funciona. }
    Exec(ExpandConstant('{sys}\schtasks.exe'), '/Delete /TN "NEXCORE\Coleta" /F', '', SW_HIDE,
      ewWaitUntilTerminated, Code);
    DataDir := ExpandConstant('{localappdata}\NEXCORE');
    if DirExists(DataDir) and not UninstallSilent then
      if MsgBox('Remover também as configurações, os logs e o histórico do NEXCORE deste usuário?' + #13#10 +
        DataDir + #13#10#13#10 + 'Escolha Não para mantê-los (por exemplo, para reinstalar depois).',
        mbConfirmation, MB_YESNO or MB_DEFBUTTON2) = IDYES then
        DelTree(DataDir, True, True, True);
  end;
end;
