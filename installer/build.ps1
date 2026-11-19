<#
  Gera a versao distribuivel do NEXCORE (Windows):
    1. interface (frontend/dist) e testes do frontend;
    2. leitor de sensores (sensor-host/dist);
    3. backend com a interface embutida e o NexcoreLauncher como entrada (perfil Maven "distribuicao");
    4. build\app-image\NEXCORE: NEXCORE.exe (sem janela de terminal) + Java reduzido (jlink) + leitor de sensores;
    5. build\instalador\NEXCORE-Setup-<versao>.exe (Inno Setup) e o arquivo .sha256 dele.
  Uso, na raiz do projeto:
    powershell -ExecutionPolicy Bypass -File installer\build.ps1 [-PularTestes] [-SemPawnIO]
  Requer (somente nesta maquina de build): JDK 21 (jpackage), Maven, Node.js, .NET SDK e Inno Setup 6.7+.
  O instalador oficial do PawnIO e baixado do GitHub do autor e so e usado se o SHA-256 conferir.
  Tudo o que e gerado fica em build\ (fora do Git).
#>
param([switch]$PularTestes, [switch]$SemPawnIO)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$version = ([xml](Get-Content (Join-Path $root 'pom.xml'))).project.version
$build = Join-Path $root 'build'
$staging = Join-Path $build 'staging'
$image = Join-Path $build 'app-image'
Write-Host "NEXCORE $version"

# O backend de desenvolvimento usa backend\target\classes: recompilar com ele aberto quebra o processo em execucao.
if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) {
  throw 'A porta 8080 esta em uso (backend de desenvolvimento aberto?). Feche-o e rode o build de novo.'
}
foreach ($tool in 'jpackage', 'mvn', 'npm', 'dotnet') {
  if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) { throw "Ferramenta de build ausente: $tool" }
}

function Invoke-Step([string]$name, [scriptblock]$block) {
  Write-Host "==> $name"
  # No PowerShell 5.1, avisos que as ferramentas escrevem no stderr (ex.: Maven/Mockito) viram erro quando a saida e
  # redirecionada. O resultado de cada passo e decidido so pelo codigo de saida.
  $previous = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  try { & $block } finally { $ErrorActionPreference = $previous }
  if ($LASTEXITCODE -ne 0) { throw "$name falhou (codigo $LASTEXITCODE)" }
}

Invoke-Step 'Interface (frontend)' {
  Push-Location frontend
  try {
    if (-not (Test-Path node_modules)) { npm ci --no-audit --no-fund; if ($LASTEXITCODE) { return } }
    if (-not $PularTestes) { npm test; if ($LASTEXITCODE) { return } }
    npm run build
  } finally { Pop-Location }
}

Invoke-Step 'Leitor de sensores' { cmd /c scripts\build-sensor-host.bat }

$skip = if ($PularTestes) { '-DskipTests' } else { '-DskipTests=false' }
Invoke-Step 'Backend (com a interface embutida)' { mvn -B -q -pl backend -am -Pdistribuicao package $skip }

$jar = Join-Path $root "backend\target\nexcore-backend-$version.jar"
if (-not (Test-Path $jar)) { throw "Jar nao encontrado: $jar" }

Write-Host '==> Preparando arquivos'
Remove-Item $staging, $image -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force (Join-Path $staging 'input'), (Join-Path $staging 'sensor-host') | Out-Null
Copy-Item $jar (Join-Path $staging 'input')
Get-ChildItem sensor-host\dist -File | Where-Object { $_.Extension -ne '.pdb' } |
  Copy-Item -Destination (Join-Path $staging 'sensor-host')

# Modulos do Java usados pelo NEXCORE (Spring Boot, OSHI/JNA, HTTPS para a Steam, bandeja/janelas, formatos pt-BR).
$modules = @(
  'java.base', 'java.compiler', 'java.desktop', 'java.instrument', 'java.logging', 'java.management', 'java.naming',
  'java.net.http', 'java.prefs', 'java.rmi', 'java.scripting', 'java.security.jgss', 'java.sql', 'java.xml',
  'jdk.charsets', 'jdk.crypto.ec', 'jdk.localedata', 'jdk.management', 'jdk.net', 'jdk.unsupported', 'jdk.zipfs'
) -join ','

Invoke-Step 'NEXCORE.exe e Java reduzido (jpackage)' {
  jpackage --type app-image `
    --dest $image `
    --name NEXCORE `
    --app-version $version `
    --vendor 'NEXCORE' `
    --copyright 'NEXCORE - Projeto Integrador de Sistemas Operacionais' `
    --description 'Monitoramento local de hardware' `
    --icon (Join-Path $root 'branding\nexcore.ico') `
    --input (Join-Path $staging 'input') `
    --main-jar "nexcore-backend-$version.jar" `
    --app-content (Join-Path $staging 'sensor-host') `
    --add-modules $modules `
    --jlink-options '--strip-debug --no-man-pages --no-header-files --compress=zip-6' `
    --java-options '-Dfile.encoding=UTF-8' `
    --java-options '-Dstdout.encoding=UTF-8'
}

$size = (Get-ChildItem (Join-Path $image 'NEXCORE') -Recurse -File | Measure-Object Length -Sum).Sum / 1MB
Write-Host ("Aplicativo: {0} ({1:N1} MB)" -f (Join-Path $image 'NEXCORE\NEXCORE.exe'), $size)

# ---------------------------------------------------------------------------------------------------------------
# Instalador (Inno Setup 6.7+)
# ---------------------------------------------------------------------------------------------------------------
$iscc = @("$env:LOCALAPPDATA\Programs\Inno Setup 6\ISCC.exe", "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe",
  "$env:ProgramFiles\Inno Setup 6\ISCC.exe") | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $iscc) { throw 'Inno Setup 6 nao encontrado (winget install JRSoftware.InnoSetup).' }

$isccArgs = @("/DVersion=$version", "/DAppImage=$(Join-Path $image 'NEXCORE')")
if (-not $SemPawnIO) {
  # Instalador oficial do PawnIO (GitHub de namazso), baixado so nesta maquina de build e conferido pelo SHA-256
  # publicado no release. O usuario final nao baixa nada: ele vai dentro do instalador do NEXCORE.
  $pawnVersion = '2.2.0'
  $pawnSha = '1f519a22e47187f70a1379a48ca604981c4fcf694f4e65b734aaa74a9fba3032'
  $pawnUrl = "https://github.com/namazso/PawnIO.Setup/releases/download/$pawnVersion/PawnIO_setup.exe"
  $deps = Join-Path $build 'deps'
  $pawn = Join-Path $deps "PawnIO_setup-$pawnVersion.exe"
  New-Item -ItemType Directory -Force $deps | Out-Null
  if (-not (Test-Path $pawn)) {
    Write-Host "==> Baixando PawnIO $pawnVersion (fonte oficial)"
    Invoke-WebRequest -UseBasicParsing $pawnUrl -OutFile "$pawn.part"
    Move-Item "$pawn.part" $pawn -Force
  }
  $actual = (Get-FileHash $pawn -Algorithm SHA256).Hash.ToLowerInvariant()
  if ($actual -ne $pawnSha) {
    Remove-Item $pawn -Force
    throw "SHA-256 do PawnIO diferente do oficial ($actual). Arquivo descartado; instalador nao gerado."
  }
  Write-Host "    PawnIO $pawnVersion conferido (SHA-256 $pawnSha)"
  $isccArgs += "/DPawnIO=$pawn"
}

Invoke-Step 'Instalador (Inno Setup)' { & $iscc /Q @isccArgs (Join-Path $PSScriptRoot 'nexcore.iss') }

$setup = Join-Path $build "instalador\NEXCORE-Setup-$version.exe"
$hash = (Get-FileHash $setup -Algorithm SHA256).Hash.ToLowerInvariant()
"$hash  NEXCORE-Setup-$version.exe" | Set-Content -Encoding ascii "$setup.sha256"
Write-Host ("Instalador: {0} ({1:N1} MB)" -f $setup, ((Get-Item $setup).Length / 1MB))
Write-Host "SHA-256:    $hash"
