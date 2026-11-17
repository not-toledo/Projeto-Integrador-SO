# Política de segurança

O PCCheck é uma aplicação local. O backend escuta somente em `127.0.0.1` e não oferece controle remoto. O leitor de
sensores (`sensor-host`) é o único componente que roda como administrador: ele não abre portas de rede, só escreve
leituras de sensores em um pipe nomeado local com nome aleatório e termina junto com o backend.

## Como relatar um problema

Não publique detalhes de vulnerabilidades em Issues públicas.

Pendente: definir o canal de contato do responsável pelo projeto para relatos de segurança.

## Escopo

- Acesso remoto ao backend ou ao agente.
- Execução de comandos ou alteração de configurações do computador pela aplicação.
- Exposição de dados de inventário fora do computador local.
