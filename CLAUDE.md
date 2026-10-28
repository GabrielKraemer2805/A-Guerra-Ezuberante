# Instruções para o Claude Code

As regras do autor valem para qualquer agente, inclusive o Claude:

@AGENTS.md

## Contexto técnico

- `CONTEXTO.md` é o diário técnico de cada versão: estado atual, decisões, balanceamento e validações. Ler a seção da versão atual (no topo) antes de mexer em uma mecânica existente.
- `guia-de-prompts/` descreve como pedidos, requisitos e modificações são estruturados neste projeto.
- Stack: Minecraft 1.20.1, Forge 47.4.28, Java 17, GeckoLib 4.8.4, TaCZ 1.1.8-hotfix2 opcional.
- Código em `src/main/java/io/github/omarflk/guerraezuberante/`; testes e fixtures em `src/gameTest` (nunca empacotar no JAR).

## Comandos

- Build: `.\gradlew.bat build`
- Abrir o jogo: `.\gradlew.bat runClient`
- GameTests: `.\gradlew.bat runGameTestServer`
- `tools/gradle.ps1` espera um JDK portátil em `.tools/jdk17`. Se ele não existir, usar o `gradlew.bat` com o Java 17 do sistema.

## Trabalho em dupla

- O projeto tem mais de um colaborador. Trabalhar em branch própria; não fazer push na `main` nem forçar push sem pedido explícito.
- Antes de começar uma tarefa, fazer `git pull` para pegar as mudanças dos outros.
- Ao registrar uma entrega no `CONTEXTO.md`, seguir o formato das seções existentes.
