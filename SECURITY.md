# Política de segurança

O NEXCORE é uma aplicação local. O backend escuta somente em `127.0.0.1` e não oferece controle remoto. O leitor de
sensores (`sensor-host`) é o único componente que roda como administrador: ele não abre portas de rede, só escreve
leituras de sensores em um pipe nomeado local com nome aleatório e termina junto com o backend.

## Como relatar um problema

Não publique detalhes de vulnerabilidades em Issues públicas.

Use o relato privado do GitHub: na página do repositório, abra **Security → Report a vulnerability**
(<https://github.com/not-toledo/Projeto-Integrador-SO/security/advisories/new>). O relato fica visível só para o
mantenedor até a correção ser publicada.

Inclua a versão do NEXCORE (Aplicativos instalados do Windows ou `/api/ping`), o sistema operacional, os passos para
reproduzir e o impacto esperado.

## Versões com suporte

| Versão | Suporte |
|---|---|
| 4.0.x (NEXCORE) | Sim |
| Anteriores (PCCheck) | Não |

## Componentes com privilégio

- **Leitor de sensores** (`NEXCORE.SensorHost.exe`): roda como administrador para ler temperaturas e SMART. Não abre
  portas de rede; recusar o aviso do Windows desativa só essas leituras.
- **Driver PawnIO** (opcional): instalado pelo instalador oficial do PawnIO, não pelo NEXCORE. Problemas no driver
  devem ser relatados em <https://github.com/namazso/PawnIO>.
- **Instalador:** confira o SHA-256 publicado em cada Release antes de executar.

## Escopo

- Acesso remoto ao backend ou ao agente.
- Execução de comandos ou alteração de configurações do computador pela aplicação.
- Exposição de dados de inventário fora do computador local.
