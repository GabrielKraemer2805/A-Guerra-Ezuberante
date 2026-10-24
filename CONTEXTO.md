# Contexto de trabalho — Guerra Ezuberante

## Regras do projeto

- Preferência permanente do autor: ao finalizar cada nova versão, abrir o Minecraft atualizado e deixá-lo aberto para teste, salvo pedido posterior em contrário. Usar o perfil normal `run/`, sem flags de teste que fecham o jogo automaticamente. Regra também registrada em AGENTS.md.
- Nome oficial: **Guerra Ezuberante**, com z; autoria OmarFLK.
- Desafio 100% por IA: o agente implementa Java, Gradle, modelos, UVs, keyframes, scripts, conversões, testes e correções. O autor fornece ideias, assets e feedback. Não pedir edição de código/JSON ou uso de Blockbench pelo autor.
- Não inventar testes aprovados. Distinguir build, testes de lógica, capturas gráficas e multiplayer real.
- Após testar a 0.004.0, o autor autorizou commit e push das alterações atuais na main, com a mensagem: "efeitos sonoros arrumados, mais vozes, animacoes tbm agora e melhorei a mecanica de combate". A publicação inclui as sprints 0.003.0 e 0.004.0; consultar git log para o commit resultante. Nenhum merge foi solicitado.
- Documentos anexados são contexto, salvo quando o usuário identifica o texto como pedido. O prompt desta sprint foi identificado como pedido.

## Estado atual — 0.004.0

- Minecraft Java 1.20.1, Forge 47.4.28, Java 17, Wrapper 8.8, ForgeGradle 6.0.54.
- GeckoLib **4.8.4**, obrigatório e fixado no Gradle/mods.toml, cliente e servidor. Maven oficial; APIs GeckoLib 4 verificadas no source JAR exato.
- modid guerra_ezuberante; pacote io.github.omarflk.guerraezuberante.
- Renderizadores GeoEntityRenderer; humanoide compartilhado de 14 ossos/20 cubos, oito clipes. PlayerModel foi substituído.
- Gerador tools/GenerateGeckoAssets.java; wrapper generate_gecko.ps1; validadores validate_assets.ps1 e validate_gecko.ps1 integram o build.
- UV por face: membros originais de 12 pixels divididos em 6+6, com overlays; PNGs 64×64 e espada 16×16 preservados por hash.
- Mixin do GeckoLib no userdev precisa SRG → official: propriedades mixin.env.remapRefMap/refMapRemappingFile e tarefa createSrgToMcp configuradas nos runs.

## Entidades e balanceamento

| Entidade | Atributos e comportamento |
| --- | --- |
| Renan Santos | 30 HP, dano 4, movimento 0,25, hitbox 0,6×1,8. Skin/terno/faixa, phonk, falas, ovo e retaliação preservados. Nenhuma habilidade nova. |
| Arthur Combatente | arthur_do_val_combatente, 32 HP, base dano 5, movimento 0,24. Nasce com Espada Ucraniana; não invoca nem recebe Speed por invocação. |
| Arthur Invocador | arthur_do_val_invocador, mesmos atributos, sem espada. Só conjura durante combate legítimo. Speed I infinito após primeira leva que criar ao menos uma unidade. |
| Arthur legado | arthur_do_val e ovo antigo continuam registrados como Invocador, preservando mundos existentes. |
| Combatente Ucraniana | 15 HP configuráveis, espada de ferro personalizada: base 1 + modificador 5 = dano total 6. Segue/defende o dono; não ataca dono, Renan ou irmãs do mesmo dono. |

- Duas variantes e legado reutilizam ArthurDoValEntity com variante definida no construtor/registro. Mesma skin/modelo, escala 0,88, hitbox 0,528×1,584, olhos 1,4256.
- Dano final do Arthur Combatente permanece **5** por requisito de preservar dano. Método do Arthur retira/restaura em finally apenas o modificador de dano da arma durante o acerto. Item em si mantém propriedades exatas de ferro e durabilidade normal; tropas continuam dano 6.
- Alcance usa distância tridimensional entre AABBs, folga máxima 1,9 bloco: aproximadamente 2,5 blocos entre centros de entidades de largura 0,6. Sem ampliar hitbox, sem atravessar paredes.
- Ataques: preparação, impacto único no tick 5/250 ms e recuperação até tick 12. Soco direito/esquerdo e espada sincronizados pelo relógio do servidor; knockback adicional 0,2 respeita resistência vanilla.
- Estados SynchedEntityData: início/lado do ataque, início do cast, início/duração da emergência. Controller Gecko busca a fase sincronizada, incluindo rastreamento iniciado depois.

## Invocação e persistência

- Apenas Invocador/legado: cast 20 ticks, até 5 tropas, recarga 700 ticks, limite padrão 30 por dono. Primeira leva começa imediatamente após agressão válida; não invoca em paz.
- Emergência padrão/mínima: 44 ticks (2,2 s). Mãos → cotovelos apoiados → braços puxam tronco → joelhos/pernas saem → postura e espada. Atrasos individuais 0/2/4/6/8 ticks. Sem translação de elevador no renderer.
- Hitbox permanece sobre terreno seguro; ações bloqueadas até terminar. Não destruir blocos. NBT preserva fase e atraso, sem reiniciar animação ao carregar.
- SavedData por dimensão conta UUIDs, inclusive tropas descarregadas. Morte/remoção do dono dissipa tropas; tolerância de dono ausente 200 ticks; vida padrão 6000 ticks carregados; sem loot.
- Forge SERVER config em serverconfig/guerra_ezuberante-server.toml do mundo. Valores antigos de emergência menores que 44 são corrigidos pela faixa da configuração.
- HasSummoned persistido; Speed vanilla MOVEMENT_SPEED, duração -1, amplificador 0. Base 0,24 → aproximadamente 0,288, sem empilhar. Cast sem sucesso não concede efeito.

## Áudios e recursos

- Origens audios/ e assets-entrada/audio/ são ignoradas pelo Git; OGGs fazem parte do JAR.
- Fonte atual de invocação: audios/ucraniana/efeito sonoro spawn ucrania.MP3, 5,903667 s, SHA256 5162b84a34516322de7dd0307c20fb26cf328d3eb23413c9ff01a2ce3e47f354. Difere do efeito anterior de 8,65 s.
- Cópia normalizada assets-entrada/audio/arthur/invocacao.mp3; sounds/arthur/invocacao.ogg inteiro, mono Vorbis 44.100 Hz. Um disparo principal no início de cada cast, recarga de som 120 ticks. Evento/streaming/legenda preservados.
- Sete falas únicas de Arthur; reação explicitamente escolhida pelo autor: **É vagabundo sim**, inteira, 9,98 s, recarga 220 ticks. Dez OGGs anteriores de falas/phonk não foram reconvertidos.
- Renan: duas falas no botão direito e phonk de combate de 12,59 s separado, sem reiniciar a cada golpe. O autor já confirmou a skin e os áudios do botão direito no protótipo.
- Painel K com cinco páginas, português/inglês. Personagens: Renan, Combatente, Invocador e Ucraniana em duas linhas de botões, com prévias e detalhes.

## Ferramentas e execução

- JDK portátil .tools/jdk17/jdk-17.0.20.1+1; FFmpeg .tools/ffmpeg/ffmpeg-9.0.2-essentials_build/bin. Ferramentas/caches não entram no Git.
- tools/gradle.ps1 escolhe o JDK e usa .gradle-user; executar build, runClient, runGameTestServer ou genIntellijRuns.
- Artefato: build/libs/guerra_ezuberante-0.004.0.jar.
- Pacote com os dois JARs prontos: build/distributions/guerra-ezuberante-0.004.0/mods/ (mod + GeckoLib original, sem mapped).
- Cenários e testes ficam em src/gameTest e não são empacotados.
- tools/visual_test.ps1 cria cópia nova do mundo fechado run-gametest/world e usa .tools/run-summon-client. Capturas reais do framebuffer, 1280×720, encerramento automático; não usa o mundo do jogador em run/.
- Teste opcional de dois clientes: host com -Multiplayer e tools/multiplayer_test.ps1. Listener só 127.0.0.1:25579, perfis offline apenas nesse teste. Scripts exigem marcadores de conclusão e não tratam saída zero sem cenário como sucesso.

## Validação real e pendências

- Build/reobf e **31 GameTests passaram**. Validadores passaram; saída do gerador tem hashes idênticos ao regenerar; JAR inspecionado sem testes/fixtures.
- Cliente gráfico abriu e completou cenário isolado. Capturas inspecionadas de skins, mãos, cotovelos, joelhos, golpe de Renan, espada de Arthur e painel. Imagens em docs/preview/gecko-0.004/.
- Logs finais: .tools/gecko-final-tests.log e .tools/gecko-final-visual.log.
- Servidor dedicado convencional chegou ao bootstrap Forge/Gecko, mas parou na EULA. run-server/eula.txt continua false; nenhum aceite foi feito pelo agente. Servidor GameTest sem GUI executou normalmente.
- Dois clientes reais abriram; tentativa localhost chegou ao handshake Forge, mas o observador não completou login. **Não afirmar multiplayer aprovado.** Logs .tools/gecko-multiplayer-host3.log e .tools/gecko-multiplayer-observer3.log.
- Pendentes: login/combate em dois clientes, latência/reconexão, servidor convencional após aceite pessoal da EULA, audição do efeito novo e aprovação estética/fluidez pelo autor. Capturas não equivalem a validação humana da animação.

## Ideia geral preservada

Combate político satírico multiplayer, seis facções: Esquerda Lulista, Direita Bolsonarista, Direita Missão, Esquerda Independente, Direita Independente e Centro. Renan/Arthur pertencem à Missão. Classes planejadas: Combatente, Tanque, Invocador, Especialista e Suporte. Facções jogáveis, outros poderes, exércitos, transformações, diplomacia e progressão continuam futuros; não implementar tudo sem pedido.

Documentação atual: README.md, docs/GECKOLIB_0.004.md, docs/VALIDACAO.md, docs/AUDIOS.md. Design/lore: docs/IDEIA_GERAL.md e docs/referencias/ideia-original.txt. Histórico: docs/COMBATE_0.002.md e docs/INVOCACAO_0.003.md.
