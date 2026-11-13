# PCCheck

Aplicação local de identificação, diagnóstico básico e monitoramento de hardware.

- **Módulo A — Hardware Overview**: identifica componentes e propriedades.
- **Módulo B — Health Check**: avalia indicadores disponíveis e gera alertas explicáveis.
- **Módulo C — Live Monitor**: métricas em tempo real com gráficos.

Arquitetura: agente local Java 21 + OSHI, API Spring Boot, interface React + TypeScript.
Seguem a especificação técnica (docs/). Prioridade: Windows.

**Versão atual: V0.1 — estrutura e execução mínima.** Nenhuma coleta de hardware ainda (começa na V0.2).

## Estrutura

```
pccheck/
├── agent/      # Agente local (Java 21)
├── backend/    # API Spring Boot (escuta só em 127.0.0.1:8080)
├── frontend/   # React + TypeScript (Vite)
├── scripts/    # Build/execução auxiliar
└── docs/       # Documentação e histórico
```

## Requisitos

- JDK 21
- Maven 3.9+ (para agent e backend)
- Node.js 20+ e npm (para o frontend)

## Como executar

### Agente

Com Maven (a partir da raiz):

```
mvn -pl agent package
java -jar agent/target/pccheck-agent-0.1.0.jar
```

Sem Maven (apenas JDK 21): `scripts/build-agent.sh` (Linux/macOS) ou `scripts\build-agent.bat` (Windows).

### Backend

```
cd backend
mvn spring-boot:run
```

Teste: abrir http://127.0.0.1:8080/api/ping — resposta esperada: `{"status":"UP","app":"PCCheck","version":"0.1.0"}`.

### Frontend

```
cd frontend
npm install
npm run dev
```

Abrir o endereço exibido pelo Vite (normalmente http://localhost:5173).

## Regras do projeto

Uma versão por vez, commit por versão, relatório ao final de cada etapa e parada para autorização. Veja docs/.
