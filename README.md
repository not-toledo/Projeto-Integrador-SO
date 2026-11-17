# PCCheck

Aplicação local de identificação, diagnóstico básico e monitoramento de hardware.
Projeto Integrador de Sistemas Operacionais.

**Versão atual: 1.0.0** (versão estável dos módulos A, B e C). Histórico de mudanças em [docs/CHANGELOG.md](docs/CHANGELOG.md).

O PCCheck coleta dados **reais** do próprio computador. Dados indisponíveis aparecem como "Não disponível" ou
"Sem leitura": o sistema nunca simula nem estima valores.

## Módulos

| Módulo | Nome | O que faz |
|---|---|---|
| A | Hardware Overview | Identifica sistema operacional, CPU, memória, GPU, placa-mãe e armazenamento. |
| B | Health Check | Avalia temperatura, espaço em disco e uso de RAM, com alertas explicáveis. |
| C | Live Monitor | Mostra CPU, RAM, GPU, sensores e discos em tempo real, com gráficos. |
| D | Game Compatibility | *Em desenvolvimento* (V1.1: pesquisa de jogos na Steam). Compara requisitos publicados com o hardware local. Ver [plano](docs/plano-modulo-d.md). |

## Documentação

- [Documentação de projeto](docs/documentacao-de-projeto.md): objetivo, escopo, requisitos, arquitetura e limitações.
- [Documentação de APIs](docs/documentacao-api.md): endpoints e convenções de resposta.
- [Processo de desenvolvimento](docs/processo-devops.md): ambiente, versionamento, testes e solução de problemas.
- [Diagramas](docs/diagramas.md): arquitetura, fluxo do Live Monitor e estados das métricas.
- [GPU e sensores com o LibreHardwareMonitor](docs/libre-hardware-monitor.md): como ativar a leitura de GPU e temperaturas.
- [Checklist de revisão](docs/checklist-documentacao.md).
- [Como contribuir](CONTRIBUTING.md) e [Política de segurança](SECURITY.md).

## Tecnologias

- Java 21 e [OSHI](https://github.com/oshi/oshi) 6.6.5 (agente de coleta)
- Spring Boot 3.3.5 (API local)
- React 19 com TypeScript, Vite, Vitest e Testing Library (interface)
- Maven, JUnit 5, Shell Script

## Requisitos

- **JDK 21**
- **Maven 3.9+**
- **Node.js 22.12 ou superior** (o `npm run dev` aceita 20.19+, mas o `npm test` exige 22.12+)

Funciona em Windows e Linux; a primeira versão prioriza o Windows.

## Instalação e execução

### 1. LibreHardwareMonitor (opcional, para GPU e sensores)

Abra o LibreHardwareMonitor **como administrador** e ative **Options → Remote Web Server → Run**.
Passo a passo em [docs/libre-hardware-monitor.md](docs/libre-hardware-monitor.md).

### 2. Backend (terminal 1)

Na raiz do projeto:

```
scripts\run-backend.bat        (Windows)
scripts/run-backend.sh         (Linux)
```

Os scripts instalam o agente no repositório Maven local e iniciam a API em `http://127.0.0.1:8080`.
Teste: `http://127.0.0.1:8080/api/ping` deve responder `{"status":"UP","app":"PCCheck","version":"1.0.0"}`.

### 3. Frontend (terminal 2)

```
cd frontend
npm install
npm run dev
```

Abra o endereço exibido pelo Vite (normalmente `http://localhost:5173`). Em desenvolvimento, o Vite encaminha `/api`
para o backend, sem configuração de CORS. Para outro endereço de API, defina `VITE_API_BASE`.

### Agente isolado

```
mvn -pl agent package
java -jar agent/target/pccheck-agent-1.0.0-all.jar
```

## Configuração

Em `backend/src/main/resources/application.properties`:

| Chave | Padrão | Função |
|---|---|---|
| `pccheck.health.*` | ver arquivo | Limites do Health Check (temperatura, disco, RAM). |
| `pccheck.monitor.interval-ms` | `2000` | Intervalo entre coletas do Live Monitor (mínimo 500). |
| `pccheck.monitor.history-size` | `300` | Amostras mantidas em memória (10 a 3600). |
| `pccheck.lhm.url` | `http://127.0.0.1:8085/data.json` | Servidor do LibreHardwareMonitor. Só endereços locais; em branco desativa. |

## Testes

1. **Agente:** `mvn -pl agent test`.
2. **Backend:** `mvn -pl backend -am test`.
3. **Ponta a ponta do agente:** `mvn -pl agent package` e depois `tests/verificar-agente.sh` (Git Bash ou WSL no Windows).
4. **API, com o backend em execução:** `tests/verificar-api.sh`, `tests/verificar-saude.sh` e `tests/verificar-live.sh`.
5. **Frontend:** `cd frontend && npm test` e `npm run build`.
6. **Documentação:** `scripts/testes-documentacao.sh`.
7. **Conferência manual:** compare os valores com o Gerenciador de Tarefas e o Gerenciador de Dispositivos (Windows)
   ou com `lscpu`, `free -h` e `df -h` (Linux).

Detalhes de cada teste em [docs/processo-devops.md](docs/processo-devops.md).

## Estrutura do repositório

```
PCCheck/
├── README.md
├── CONTRIBUTING.md
├── SECURITY.md
├── agent/        # Agente local (Java 21 + OSHI): collector/, lhm/, model/ e testes
├── backend/      # API Spring Boot (127.0.0.1:8080): controller/, service/, dto/, diagnostic/, monitor/, config/
├── frontend/     # React + TypeScript (Vite): components/, modules/, pages/, services/
├── docs/         # Documentação de projeto, API, processo, diagramas e histórico
│   └── api/      # Documentos de cada endpoint
├── scripts/      # Execução (run-agent, run-backend) e testes de documentação
├── templates/    # Modelo de README para novos projetos
└── tests/        # Verificação ponta a ponta (agente, API, Health Check, Live Monitor)
```

## Solução de problemas

Problemas comuns estão em [docs/processo-devops.md](docs/processo-devops.md#solução-de-problemas).

## Limitações conhecidas

Lista completa em [docs/documentacao-de-projeto.md](docs/documentacao-de-projeto.md#limitações-conhecidas).
Destaques: GPU e sensores dependem do LibreHardwareMonitor; SMART não é suportado; os limites do Health Check são
genéricos; e a leitura com sensores reais depende de teste na sua máquina.

## Integrantes

- Gabriel — *Pendente: sobrenome, RA e demais integrantes do grupo.*

## Licença

Pendente: definir a licença do projeto (o repositório de referência usa MIT).
