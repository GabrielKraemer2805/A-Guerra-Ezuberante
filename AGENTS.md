# Preferências do autor

- Todos os Minecrafts abertos pelo agente para testes automatizados devem iniciar com o som zerado. O autor assiste a séries enquanto o agente trabalha. Usar perfis isolados com master volume 0 ou silenciar só a sessão de áudio desses processos; nunca silenciar o volume do Windows ou o aplicativo da série. Registrar essa opção antes de iniciar novos clientes de teste. Não modificar a preferência de volume do perfil normal do autor.
- Reforço explícito do autor em 29/10/2026: **todo Minecraft aberto pelo agente para testes deve ficar sem NENHUM SOM**, inclusive o cliente deixado para conferência. No perfil normal, usar `tools/gradle.ps1 runClient -PsilentClient`: a opção silencia só essa JVM antes da reprodução e preserva o volume salvo em `options.txt`. Se precisar usar o mixer, verificar o processo específico aberto pelo agente; nunca selecionar indiscriminadamente outros Minecrafts.

- Ao finalizar uma nova versão do mod, iniciar o Minecraft com a versão atualizada e deixá-lo aberto para o autor testar, salvo orientação posterior em contrário.
- Para essa entrega, usar o perfil normal em `run/`, sem os modos `visualTest` ou `observerTest`, que encerram o cliente automaticamente. Confirmar que o jogo carregou antes de informar que está aberto.
- Preservar os mundos do autor. Cenários automatizados continuam em perfis isolados; não fechar à força um cliente que possa estar com um mundo aberto.
- Consultar `CONTEXTO.md` para o estado técnico e as demais preferências do projeto.
