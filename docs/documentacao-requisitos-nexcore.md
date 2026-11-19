# DOCUMENTAÇÃO DE REQUISITOS — NEXCORE

**Projeto:** NEXCORE — Identificação, Diagnóstico e Monitoramento de Hardware  
**Versão:** 1.0.0  
**Disciplina:** Projeto Integrador de Sistemas Operacionais  
**Plataformas:** Windows e Linux  
**Tipo de aplicação:** Aplicação local

---

## 1. Introdução

O NEXCORE é uma aplicação local desenvolvida para identificar componentes de hardware, realizar diagnósticos básicos e monitorar o desempenho do computador em tempo real.

O sistema coleta informações diretamente do computador onde está sendo executado, utilizando ferramentas e bibliotecas de acesso ao hardware, apresentando os resultados por meio de uma interface gráfica intuitiva.

Esta documentação descreve os requisitos funcionais e não funcionais necessários para o funcionamento do sistema.

## 2. Objetivo do sistema

Disponibilizar uma ferramenta que permita ao usuário:

- Identificar os componentes de hardware do computador.
- Consultar informações sobre CPU, memória RAM, GPU, placa-mãe e armazenamento.
- Identificar possíveis situações preocupantes relacionadas à temperatura, memória e espaço em disco.
- Monitorar o desempenho do computador em tempo real.
- Visualizar informações técnicas de maneira compreensível.

## 3. Requisitos funcionais (RF)

Os requisitos funcionais descrevem as funcionalidades e operações que o sistema deve executar.

### 3.1. Módulo A — Hardware Overview

| ID | Requisito funcional |
|---|---|
| RF01 | O sistema deve identificar e apresentar informações sobre o sistema operacional instalado, incluindo nome, versão, arquitetura, build e tempo de atividade, quando disponíveis. |
| RF02 | O sistema deve identificar o processador (CPU), apresentando informações como fabricante, modelo, frequência e quantidade de núcleos físicos e lógicos, quando disponíveis. |
| RF03 | O sistema deve identificar a memória RAM, apresentando capacidade total e demais informações disponibilizadas pelo sistema operacional. |
| RF04 | O sistema deve identificar a placa de vídeo (GPU), apresentando fabricante, modelo e memória de vídeo informada pelo sistema. |
| RF05 | O sistema deve identificar a placa-mãe, apresentando fabricante, modelo e informações disponíveis. |
| RF06 | O sistema deve identificar os dispositivos de armazenamento e apresentar suas unidades e volumes. |
| RF07 | O sistema deve apresentar as informações coletadas em cartões individuais para cada componente de hardware. |
| RF08 | O sistema deve disponibilizar um botão de atualização para realizar uma nova coleta das informações de hardware. |
| RF09 | O sistema deve apresentar o horário e a origem da última coleta realizada para cada componente. |
| RF10 | O sistema deve apresentar a indicação "Não disponível" quando uma informação não puder ser obtida. |
| RF11 | O sistema deve permitir que a falha na coleta de um componente não impeça a identificação dos demais componentes. |

### 3.2. Módulo B — Health Check

| ID | Requisito funcional |
|---|---|
| RF12 | O sistema deve coletar informações disponíveis sobre a temperatura da CPU. |
| RF13 | O sistema deve comparar a temperatura da CPU com limites configuráveis para determinar seu estado. |
| RF14 | O sistema deve classificar a temperatura em três estados: Normal, Atenção e Crítico. |
| RF15 | O sistema deve identificar o espaço livre e o percentual de utilização de cada volume de armazenamento. |
| RF16 | O sistema deve monitorar o percentual de utilização da memória RAM. |
| RF17 | O sistema deve gerar alertas quando identificar situações que ultrapassem os limites configurados. |
| RF18 | O sistema deve apresentar os alertas com identificador, componente, severidade, título, descrição, valor observado, limite e horário. |
| RF19 | O sistema deve classificar os alertas nas severidades Informação, Atenção e Crítico. |
| RF20 | O sistema deve apresentar a saúde dos discos (SMART: vida útil restante, temperatura, horas ligado e dados gravados) quando disponível, e informar explicitamente quando não puder lê-la e por quê. |
| RF21 | O sistema deve informar quando uma coleta estiver incompleta ou quando determinado dado estiver indisponível. |
| RF22 | O sistema deve disponibilizar a funcionalidade de reavaliação do estado dos componentes por meio de uma ação do usuário. |
| RF23 | O sistema deve informar quando houver utilização elevada de memória RAM, sem classificar automaticamente essa condição como defeito físico. |

### 3.3. Módulo C — Live Monitor

| ID | Requisito funcional |
|---|---|
| RF24 | O sistema deve monitorar a utilização total da CPU em tempo real. |
| RF25 | O sistema deve apresentar a utilização individual dos processadores lógicos. |
| RF26 | O sistema deve monitorar a utilização da memória RAM. |
| RF27 | O sistema deve monitorar a utilização da GPU quando houver dados disponíveis. |
| RF28 | O sistema deve monitorar sensores de hardware, incluindo temperaturas e velocidades das ventoinhas, quando disponíveis. |
| RF29 | O sistema deve monitorar a atividade dos dispositivos de armazenamento. |
| RF30 | O sistema deve apresentar gráficos temporais para representar a variação das métricas monitoradas. |
| RF31 | O sistema deve apresentar unidades de medida explícitas nos indicadores e gráficos, incluindo %, °C, MiB/s e RPM. |
| RF32 | O sistema deve atualizar automaticamente as métricas em intervalos configuráveis. |
| RF33 | O sistema deve apresentar o horário da última atualização das métricas. |
| RF34 | O sistema deve informar o estado da conexão com o serviço responsável pela coleta. |
| RF35 | O sistema deve manter um histórico temporário das amostras coletadas para alimentar os gráficos de monitoramento. |
| RF36 | O sistema deve permitir a visualização das métricas sem bloquear a interface durante as coletas em segundo plano. |

## 4. Requisitos não funcionais (RNF)

Os requisitos não funcionais estabelecem as condições de qualidade, segurança, desempenho e confiabilidade que o sistema deve atender.

### 4.1. Desempenho

| ID | Requisito não funcional |
|---|---|
| RNF01 | O sistema deve apresentar baixo consumo de processamento e memória durante sua execução. |
| RNF02 | O monitoramento deve utilizar intervalo padrão de coleta de 2 segundos. |
| RNF03 | O intervalo de coleta deve ser configurável, respeitando o mínimo de 500 milissegundos. |
| RNF04 | As coletas devem ocorrer em segundo plano, sem bloquear a interface gráfica. |
| RNF05 | O sistema deve evitar o acúmulo de tarefas de coleta, permitindo apenas uma coleta por vez. |

### 4.2. Segurança

| ID | Requisito não funcional |
|---|---|
| RNF06 | O backend deve aceitar conexões exclusivamente pelo endereço local 127.0.0.1. |
| RNF07 | O sistema não deve disponibilizar endpoints destinados ao controle remoto do computador. |
| RNF08 | A integração com o LibreHardwareMonitor deve aceitar exclusivamente endereços locais. |
| RNF09 | A aplicação não deve executar ações que modifiquem configurações ou componentes do computador do usuário. |

### 4.3. Privacidade

| ID | Requisito não funcional |
|---|---|
| RNF10 | As informações de inventário e diagnóstico devem permanecer armazenadas e processadas localmente. |
| RNF11 | O sistema não deve exigir o envio das informações de hardware para servidores externos ou serviços em nuvem. |

### 4.4. Confiabilidade e robustez

| ID | Requisito não funcional |
|---|---|
| RNF12 | A falha na identificação de um componente não deve comprometer a coleta dos demais componentes. |
| RNF13 | O sistema deve tratar exceções e erros provenientes dos coletores e das integrações externas. |
| RNF14 | Valores inválidos ou indisponíveis não devem ser apresentados como medições reais. |
| RNF15 | O sistema deve diferenciar informações indisponíveis de possíveis falhas físicas de hardware. |
| RNF16 | Os dados apresentados devem corresponder às informações efetivamente disponibilizadas pelo sistema operacional e pelos sensores. |

### 4.5. Compatibilidade

| ID | Requisito não funcional |
|---|---|
| RNF17 | O sistema deve funcionar nos sistemas operacionais Windows e Linux. |
| RNF18 | A primeira versão deve priorizar a compatibilidade e a validação no Windows. |
| RNF19 | O código específico de cada sistema operacional deve permanecer isolado, facilitando a manutenção e a compatibilidade entre plataformas. |
| RNF20 | O sistema deve utilizar Java 21, Maven 3.9 ou superior e Node.js 22.12 ou superior para o ambiente de desenvolvimento e testes completos. |

### 4.6. Manutenibilidade e testabilidade

| ID | Requisito não funcional |
|---|---|
| RNF21 | O projeto deve manter uma arquitetura organizada em agente de coleta, backend e frontend. |
| RNF22 | O sistema deve possuir testes automatizados para conversões, percentuais, regras de diagnóstico e funcionalidades dos módulos. |
| RNF23 | O projeto deve possuir documentação técnica sobre arquitetura, APIs, execução, testes e limitações. |
| RNF24 | As alterações de comportamento devem ser registradas no histórico de versões (CHANGELOG). |
| RNF25 | Os componentes devem possuir responsabilidades separadas, facilitando futuras alterações e manutenção. |

### 4.7. Usabilidade

| ID | Requisito não funcional |
|---|---|
| RNF26 | A interface deve apresentar informações técnicas de maneira organizada e compreensível para usuários sem conhecimento avançado de hardware. |
| RNF27 | Os alertas devem apresentar descrições explicativas sobre as condições identificadas. |
| RNF28 | As métricas e os gráficos devem apresentar unidades de medida para facilitar a interpretação dos resultados. |
| RNF29 | As informações indisponíveis devem ser identificadas claramente, evitando interpretações equivocadas pelo usuário. |

## 5. Regras de negócio (RN)

As regras de negócio estabelecem critérios que devem ser respeitados durante o funcionamento do sistema.

| ID | Regra de negócio |
|---|---|
| RN01 | O sistema deve apresentar somente dados reais obtidos por meio dos coletores e das fontes disponíveis. |
| RN02 | O sistema nunca deve simular, estimar ou inventar valores para preencher informações indisponíveis. |
| RN03 | Quando não houver leitura de determinado componente, o sistema deve informar a indisponibilidade do dado. |
| RN04 | A ausência de uma leitura de temperatura não deve ser interpretada automaticamente como superaquecimento ou defeito. |
| RN05 | Os limites de temperatura e demais indicadores do Health Check devem seguir as configurações definidas no projeto. |
| RN06 | Os limites utilizados no diagnóstico são genéricos e não representam necessariamente os limites oficiais estabelecidos pelos fabricantes. |
| RN07 | O uso elevado de memória RAM deve ser tratado como uma condição de atenção, não como comprovação de defeito físico. |
| RN08 | A análise SMART considera apenas os indicadores informados pelo disco (vida útil e temperatura); "indicadores normais" não deve ser apresentado como garantia de ausência de falhas. |
| RN09 | O histórico de monitoramento deve permanecer temporariamente em memória, sem persistência em banco de dados. |
| RN10 | A coleta de GPU, temperaturas e ventoinhas pode depender da execução do LibreHardwareMonitor com permissões adequadas. |
| RN11 | O Hardware Overview e o Health Check devem ser atualizados mediante solicitação do usuário. |
| RN12 | Somente o Live Monitor deve realizar atualizações automáticas periódicas nesta versão. |

## 6. Requisitos fora do escopo

As funcionalidades abaixo não fazem parte da versão 1.0.0:

- Análise e monitoramento individual de processos.
- Compatibilidade automática com jogos.
- Exportação de relatórios.
- Persistência de histórico em banco de dados.
- Testes de estresse de CPU, GPU ou memória.
- Alteração automática de configurações do computador.
- Controle remoto do hardware.
- Envio de informações para servidores externos.

**Observação:** O módulo D — Game Compatibility está previsto para desenvolvimento em versões futuras, mas não integra o escopo funcional da versão 1.0.0.

## 7. Tecnologias relacionadas aos requisitos

| Camada | Tecnologia | Responsabilidade |
|---|---|---|
| Agente de coleta | Java 21 e OSHI 6.6.5 | Identificação e coleta de informações do hardware. |
| Backend | Spring Boot 3.3.5 | Disponibilização da API local e processamento das regras de diagnóstico. |
| Frontend | React 19 e TypeScript | Apresentação das informações e interação com o usuário. |
| Monitoramento adicional | LibreHardwareMonitor | Obtenção de dados de GPU e sensores. |
| Gerenciamento | Maven e npm | Gerenciamento de dependências e execução do projeto. |
| Testes | JUnit 5, Vitest e Testing Library | Validação das funcionalidades e regras do sistema. |

## 8. Critérios gerais de aceitação

O projeto será considerado funcional quando:

1. Conseguir identificar e apresentar os componentes de hardware disponíveis.
2. Apresentar corretamente informações indisponíveis sem inventar valores.
3. Executar o diagnóstico de temperatura, memória RAM e armazenamento.
4. Gerar alertas de acordo com os limites configurados.
5. Realizar o monitoramento periódico das métricas disponíveis.
6. Apresentar gráficos e indicadores com unidades de medida.
7. Manter a aplicação funcional mesmo quando um dos coletores apresentar falhas.
8. Manter o backend exclusivamente em endereço local.
9. Passar pelos testes automatizados dos módulos implementados.
10. Permitir a execução no Windows e Linux, respeitando as limitações de cada plataforma.

## 9. Considerações finais

A documentação estabelece os requisitos funcionais e não funcionais da versão 1.0.0 do NEXCORE, servindo como referência para desenvolvimento, testes, manutenção e avaliação acadêmica.

O projeto prioriza a coleta de informações reais, a segurança da execução local, a confiabilidade dos diagnósticos e a apresentação compreensível dos dados de hardware.

As funcionalidades futuras deverão respeitar a arquitetura e os princípios definidos nesta documentação.
