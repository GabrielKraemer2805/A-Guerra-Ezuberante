# Validação — 0.014.1

Nova contribuição de Gabriel integrada com progressão, poderes, vozes e vilas, preservando os commits e as proteções anteriores. [Recursos, correções e limites](INTEGRACAO_0.014.1.md).

- **120 GameTests com TaCZ** e **56 sem TaCZ** aprovados, incluindo seis regressões novas de proteção de terreno, cancelamento de explosão/dano, PvP no impacto e crédito de XP. Logs `.tools/integration-014-final-tests.log` e `.tools/integration-014-no-tacz.log`.
- **Build, validadores e reobf aprovados** em `.tools/integration-014-package.log`. JAR0.014.1 inspecionado sem classes de teste nem TaCZ embutido. SHA256: `c28c4b1a0e14a822e593098233bd172b1e9f7d7f3139da85cc43c95686011181`.
- **Dois clientes reais aprovados** em perfis isolados e silenciosos. Além dos fluxos anteriores, perfil J/pontos/seleção de poderes via pacotes reais,20 meteoros,6 livros e10 Manifestantes com os mesmos IDs nos dois clientes; Manifestantes PvP OFF sem hits/ON com hit; Faz o L derruba duas pilhas e sincroniza inventário; seis bandeiras e Jair renderizados. [Servidor](validacao/integracao-0.014.1/server.json), [host](validacao/integracao-0.014.1/host.json), [observador](validacao/integracao-0.014.1/observer.json).
- **Vilas:** posições em cinco sementes e geração real de vilas/castelos de três facções representativas nos GameTests. Mundos antigos não recebem retrogeneração; usar mundo novo para ver as estruturas completas.
- **Cliente normal aberto**, menu pronto às14:17:48 em `run/`, sem flags de teste, com volume normal preservado. Log `.tools/integration-014-normal-client.log`.

Limites: rede local, sem latência artificial, todas as sementes ou todos os mods de proteção. Recursos/timers de áudio verificados; testes automatizados sem audição, conforme preferência do autor. Capturas em `docs/preview/integracao-0.014.1/`.

## Histórico — 0.012.1

Contribuição de GabrielKraemer2805 integrada preservando os commits originais e corrigindo PvP, ciclo de vida das tropas e regeneração após relog. [Entrega completa](INTEGRACAO_FACCOES_0.012.1.md).

- **95 GameTests com TaCZ passaram**, incluindo cinco regressões novas das falhas da revisão. `.tools/faction-final-tests.log`.
- **31 GameTests sem TaCZ passaram**. `.tools/faction-final-no-tacz.log`.
- **Build, validadores e reobf passaram**. `.tools/faction-release-package.log`. JAR inspecionado sem fixtures/classes TaCZ embutidas.
- **Dois clientes reais passaram** em perfis isolados, master0 antes do launch, mundo copiado: escolhas C2S, nomes/TAB remotos, mesmos três IDs de feijão283/284/285, cura de dois jogadores10→14/15, K30→27s e mudança de facção com painel aberto, tropas PvP OFF sem dano/ON com dano, morte/respawn/NBT antigo rejeitado e nova invocação, reconexão53ticks com espera de recuperação preservada, Rachadinha PvP OFF20→20/ON20→12 e investida1,3 medida em9,4833blocos nesse cenário.

Relatórios [servidor](validacao/faccoes-0.012.1/servidor.json), [host](validacao/faccoes-0.012.1/host.json), [observador](validacao/faccoes-0.012.1/observer.json). Capturas em `docs/preview/faccoes-0.012.1/`. Logs `.tools/faction-{host,observer}-release.log`. A execução inicial de PvP foi descartada por fome na fixture; a final mantém comida e desliga regeneração natural. Dados de TAB foram verificados no cliente, não deduzidos só de screenshots. A investida mantém seu impulso original e deixa de prometer distância fixa.

Limites: loopback local, sem latência artificial/internet pública, sem outros mods de proteção e sem cobrir todos os terrenos. Áudio automatizado silenciado por preferência do autor; audição manual permanece a cargo do teste normal.

## Histórico — 0.011.0

**76 GameTests obrigatórios passaram**, incluindo cinco novos casos de recasts reais da IA, ponto próximo/área final maior, bossbars, migração de vida salva e configurações personalizadas. Intervalos observados entre quatro casts: 121/120/120 ticks. Log `.tools/boss-rift-final-tests.log`. **12 testes sem TaCZ passaram** em `.tools/boss-rift-no-tacz.log`. Build/check/reobf aprovado em `.tools/boss-rift-package.log`; JAR inspecionado sem fixtures ou classes do TaCZ embutidas. [Mudanças e configurações](BOSSES_0.011.md).

**Teste gráfico final aprovado** em perfil isolado e silencioso: duas barras com os nomes/cores pedidos, 300 HP, progressos recebidos de 0,7/0,8, quatro Rachadinhas naturais e retirada das barras na morte/saída de alcance. [Relatório](validacao/boss-0.011/cliente.json); capturas em `docs/preview/boss-0.011/`. Arena mantida carregada e referências resolvidas por UUID corrigiram a fixture inicial. Log final `.tools/boss-rift-visual-loaded.log`. Não houve nova validação de internet pública ou latência.

Os registros abaixo são históricos e preservam o balanceamento e os resultados de cada sprint.
# Validação — sprint 0.010.0

## Jair, Flávio e Rachadinha

Em 27/10/2026, build/reobf e **71 GameTests obrigatórios passaram**, incluindo os 60 anteriores. Log `.tools/bolsonaro-continuous-release.log`. **12 testes sem TaCZ passaram**, `.tools/bolsonaro-no-tacz-tests.log`. [Entrega, configurações e limites](JAIR_FLAVIO_0.010.md).

Validados: Glock 17 original e munição infinita apenas no NPC, dano/projéteis nativos sem rajadas, linha de tiro/aliados, invocação inicial e cooldown700/limite3, UUID/ledger/remoção dos vinculados, seguimento e defesa reais, ovo independente, fissura com dano6 uma vez, cooldown persistido, windup/parede/declive, terreno estreito e contínuo com profundidade limitada, bedrock, modos ON/OFF, mobGriefing, cancelamento de LivingDestroyBlockEvent e BlockEvent.BreakEvent e morte verde compartilhada com Lula/persistência/explosão única de potência7.

Uma fixture antiga de morte estava sendo inundada por água externa. Auditoria mostrou fonte em waterflow, interação nativa DESTROY_WITH_DECAY e0blocos atingidos, inclusive numa explosão nativa de controle sem entidade. A arena foi movida para altura seca e passou; nenhuma resistência vanilla foi desativada para contornar o teste.

Resultados gráficos, capturas e relatórios da0.010 constam na [documentação da sprint](JAIR_FLAVIO_0.010.md). Os históricos abaixo preservam os resultados anteriores.

**Dois clientes finais aprovados** após a conexão dos cotovelos da fissura:21 tiros medidos com intervalo 20/20;24 eventos cliente no host e24 remotos contando o início da cena; carregador17→17. ModoOFF:12células,6dano,0blocos; modoON:11células,6dano,11blocos. Mesmos IDs de ativação nos clientes, progressão visível, invocação, morte verde e vermelha, subida/ossos e um som de explosão por entidade em ambos. [Host](validacao/bolsonaro-0.010/host.json), [observador](validacao/bolsonaro-0.010/observer.json). Capturas em `docs/preview/bolsonaro-0.010/`.

## Lula

Build/reobf e **60 GameTests obrigatórios passaram** em `.tools/lula-final-build-tests.log`, incluindo os 55 testes anteriores. Cinco novos casos cobrem ovo/neutralidade/playlist, voo/dano6/cadência40/windup8, parede/TTL, dono/aliados e a sequência completa de morte. Verificadas persistência DYING/EXPLODED, subida2, explosão única com potência7 observada no motor, dano/knockback, flags de blocos, mobGriefing, desativação, cadeia limitada e cancelamento de proteção Forge. [Detalhes e limites](LULA_0.009.md).

Os testes históricos abaixo mantêm o contexto das entregas anteriores.

Cenário gráfico final **aprovado com dois clientes reais** em127.0.0.1:25583: mesmos cinco projéteis, quatro gravações com uma instância por entidade, tint/ossos/subida e um som nativo de explosão em ambos. [Host](validacao/lula-0.009/host.json), [observador](validacao/lula-0.009/observer.json), [capturas e limitações](LULA_0.009.md). Uma tentativa anterior teve timeout de login; não declarar rede pública ou reconexões sob latência testadas. Build final passou em `.tools/lula-final-package.log`; artefato0.009.0.

## Tela inicial ilustrada

Em **26/10/2026**, com a stack anterior preservada, o cenário gráfico real passou em `.tools/run-title-client`, com volume mestre zero antes do launch. [Relatório](validacao/menu-0.008/cliente.json), [detalhes da entrega](MENU_0.008.md).

| Verificação | Resultado |
| --- | --- |
| Arte | Recurso RGB 1920×1080 carregado e renderizado no menu |
| Widgets | Nove controles originais encontrados; sem recriar botões |
| Options / Multiplayer / Mods | Telas vanilla/Forge abriram e retornaram corretamente |
| Singleplayer | Lista abriu, mundo isolado carregou e salvar/sair voltou ao menu ilustrado |
| Resoluções de renderização | 1280×720, 1920×1080, 1024×768 e 2560×1080 capturados e inspecionados |
| Loop | Cinco aberturas adicionais; dez substituições ao longo do cenário, sem recursão |
| Regressão | **55 GameTests obrigatórios passaram** com o AT ativo |
| Build | build/reobf e validações de assets concluídos; JAR 0.008.0 |

Limite do ambiente: a janela física é limitada pelo desktop a1444×881; tamanhos alternativos foram renderizados no framebuffer real do Minecraft, não apresentados como monitores físicos diferentes. Não foi feito login Realms ou conexão a servidor externo; a navegação multiplayer local foi verificada. A cópia de mundo e o encerramento automático ficam somente no perfil de teste. [Captura 1920×1080](preview/menu-0.008/1920x1080.png).

## Histórico — 0.007.0

## Renan com AK nativa e playlists compartilhadas

Stack mantida: Forge 47.4.28, Minecraft 1.20.1, Java 17, GeckoLib 4.8.4 e TaCZ 1.1.8-hotfix2. Data: 24/10/2026. Os resultados anteriores permanecem abaixo como histórico.

| Verificação | Resultado executado | Evidência |
| --- | --- | --- |
| Regressão / build | **54 GameTests passaram**, build/reobf concluído | `.tools/renan-final-tests.log` |
| API e arma | Renan equipado com AK nativa; consumo/check desativados só no operador do NPC; Survival permanece normal | `RenanArmedGameTests` |
| Cadência / munição prolongadas | 6500 ticks acelerados, **108 tiros / 108 acertos**, carregador em 30 preservado | Mesmo log, `RENAN_SUSTAINED` |
| Paredes e aliados | Linha bloqueada impede pedido; aliado não é agressor válido | GameTests |
| Playlist / shuffle | Combate e interação compartilham estado; clique tem debounce, duração avança e morte encerra; cem rodadas sem repetição | GameTests |
| Cliente gráfico em tempo real | **124 tiros / 124 projéteis / 124 acertos**, carregador em 30, mais de 2 min de combate | [Relatório](validacao/renan-0.007/singleplayer.json) |
| Áudios no cliente | Três gravações do Renan,14 inícios, uma instância por entidade e limpeza na morte | Mesmo relatório |
| Dois clientes reais | **Passou**: host: 124 tiros/projéteis/acertos e Observer: 124 eventos remotos; mira/recuo observados | [Host](validacao/renan-0.007/multiplayer-host.json), [Observer](validacao/renan-0.007/multiplayer-observer.json) |
| Playlist em rede | Host confirmou combate sem reiniciar interação; os dois receberam 3 gravações e mantiveram uma instância | Mesmos relatórios |
| Sem TaCZ | **11 GameTests passaram**, incluindo criação do Renan e playlist sem dependência opcional | `.tools/renan-no-tacz-build.log` |
| Armas dos jogadores na 0.007 | **Cinco aprovadas no cliente real**, incluindo AKM: ADS/recoil/dano, consumo normal, recarga, conservação e vazio | [Cliente](validacao/renan-0.007/arsenal-jogador-cliente.json), [Servidor](validacao/renan-0.007/arsenal-jogador-servidor.json) |
| Animações / skins | 14 clipes válidos, modelo e UV 64×64 preservados; captura real inspecionada | `validateGeckoAssets`, capturas |
| Som dos testes | Lançamentos posteriores ao pedido do autor forçam master 0.0 em perfis `.tools/`; perfil normal preservado | `runClient.doFirst`, `options.txt` dos dois clientes |

GameTest acelera o relógio de ticks; os 6500 ticks não foram declarados minutos de relógio. O cenário gráfico prolongado usa servidor integrado normal. Os testes antigos de áudio foram atualizados porque o autor pediu falas e músicas na mesma playlist para cada personagem; o teste do soco desarma Renan para exercitar o fallback preservado.

Uma primeira tentativa com dois clientes conectou, mas foi encerrada sem relatórios completos e não vale como aprovação. A repetição **com os dois perfis silenciosos completou e produziu os dois relatórios aprovados**. Usou servidor integrado normal com TCP limitado a 127.0.0.1:25582; não foi declarada uma sessão gráfica dedicada separada. Os GameTests também executaram em processo de servidor sem cliente. Não se usa o teste multiplayer antigo das cinco armas como prova desta nova IA.

Limites: não há seek de OGG para ouvintes que chegam no meio da gravação; eles respeitam a próxima transição do servidor. TPS baixo pode produzir intervalo entre fim real do arquivo e timer lógico. Os áudios das instâncias posteriores foram silenciados conforme preferência; recursos/eventos e estados continuam auditados. Aprovação humana do som e do acabamento das poses permanece feedback do autor. [Implementação e fontes](RENAN_0.007.md), [próximos personagens — Lula](PROXIMOS_PERSONAGENS.md).

## Histórico — 0.006.0

## Pistola própria GE-P12

Ambiente mantido: Minecraft 1.20.1, Forge 47.4.28, Java 17, GeckoLib 4.8.4, Windows, 24/10/2026. TaCZ agora é opcional. A validação desta pistola é separada dos resultados anteriores das armas TaCZ.

| Verificação | Resultado executado | Evidência |
| --- | --- | --- |
| Regressão completa | **47 GameTests passaram**: 37 anteriores + 10 próprios; build/reobf aprovado | `.tools/pistol-final-build.log` |
| Servidor sem TaCZ | **Dez testes próprios passaram**, incluindo segundo acerto dentro da janela vanilla de dano | `.tools/pistol-final-no-tacz-tests.log` |
| Cliente sem TaCZ | Modelo 3D, ADS, tiro/dano, um som principal, recuo, conservação, recarga e vazio aprovados | [Relatório](validacao/pistola-0.006/cliente-sem-tacz.json) |
| Aba criativa sem TaCZ | Seis itens: quatro anteriores, GE-P12 e cartucho; tela real aberta | Mesmo relatório, [captura](preview/pistola-0.006/creative.png) |
| Convivência com TaCZ | Mesmo cenário aprovado; aba real com 15 itens (os seis próprios + nove nativos) | [Relatório](validacao/pistola-0.006/cliente-com-tacz.json), [captura](preview/pistola-0.006/creative-com-tacz.png) |
| Gatilho segurado | Continua semiautomático; nenhum segundo tiro automático | Relatório gráfico sem TaCZ |
| Recursos próprios | Geometria/paleta e três sons sintetizados, sem assets de arma externos | `tools/GeneratePistolAssets.java`, validadores do build |
| Pacotes | Mod próprio reobfuscado, alternativa com GeckoLib e outra com GeckoLib + TaCZ | `build/distributions/` |

O cenário gráfico usa Survival, input vanilla/remapeável e o canal de rede de produção. Medidas: FOV 70 → 54,6; alvo recebeu 6 HP; carregador/reserva 12+48 → 11+48 → 12+47; vazio recusou tiro/recarga e não gerou outro som de tiro. A ausência de TaCZ é verificada por `ModList`, não presumida pelo nome de um perfil.

Os testes iniciais detectaram um bloqueio do ataque vanilla enquanto usando a mira; o cliente próprio passou a observar a borda da tecla de ataque também durante ADS. Capturas só são feitas após frames reais para não registrar a tela anterior ao teleporte. Os scripts exigem relatório com `passed=true`.

Não foi executada uma sessão gráfica entre dois clientes da **GE-P12**; os resultados de dois clientes abaixo são históricos das armas externas da 0.005. O protótipo tem animação por transformação do conjunto e não mãos/slide/carregador articulados. Feedback humano sobre sensação e áudio permanece pendente. [Implementação, capturas e limites](PISTOLA_0.006.md).

## Histórico — 0.005.0

### Arsenal TaCZ: resultados da sprint anterior

Ambiente: Minecraft 1.20.1, Forge 47.4.28, Java 17, GeckoLib 4.8.4 e **TaCZ 1.1.8-hotfix2**, em Windows, 24/10/2026. Não houve push/merge desta sprint.

| Verificação | Resultado real | Evidência |
| --- | --- | --- |
| Build/reobf | **Passou**; JAR 0.005.0 e conjunto de três mods externos separados | `.tools/arsenal-final-build.log` |
| GameTests | **37 testes obrigatórios passaram**, incluindo os 31 da base anterior | Mesmo log, `All 37 required tests passed` |
| Dependência | Manifesto hotfix2, SHA-256, cinco IDs e quatro calibres conferidos | Tarefa `validateTacz` |
| Assets/Gecko | Validadores anteriores passaram; skins, sons e código de entidades preservados | `validateAssets`, `validateGeckoAssets`, diff Git |
| AKM primeiro | Cenário completo aprovado antes de acrescentar as outras quatro armas | `.tools/akm-initial-validation/` |
| Cinco armas em Survival | ADS/FOV, recoil, um som/tiro, dano, recarga e rejeição sem munição passaram | [Relatório cliente](validacao/arsenal-0.005/singleplayer-cliente.json) |
| Servidor dedicado normal | Inicializou após aceite explícito da EULA pelo autor; gameplay real executado | `.tools/arsenal-dedicated.log`, relatórios preservados |
| Dois clientes reais | Uma execução completa com Shooter e Observer: maxPlayers=2, cinco armas, sem acertos duplicados | [Servidor](validacao/arsenal-0.005/dedicado-servidor.json), [Shooter](validacao/arsenal-0.005/dedicado-shooter.json) |
| Sincronização remota | Observer recebeu exatamente um disparo e uma recarga de cada arma | [Observer](validacao/arsenal-0.005/dedicado-observer.json) |
| Calibres e acessórios | Munição incorreta rejeitada; óptica nativa instalada pela API | GameTests |
| Permissões | Comando recusado sem OP, aceito com nível 2, kit completo | GameTests |
| Repetição de login | **Não aprovada como consistente**: tentativas adicionais tiveram timeout no handshake | `.tools/arsenal-network-final.log`, `.tools/arsenal-network-sequential.log` |

Os relatórios de sucesso foram copiados da execução completa antes de novas tentativas. O script exige marcadores/resultados e não considera apenas o exit code zero do Gradle. Tiros e recargas usam a API cliente original, com pacotes reais; o servidor observa eventos e contagens efetivas. Não foi usado um mock de projétil para declarar dano em rede.

A tentativa sem munição é feita depois de esvaziar a arma/reserva somente na fixture isolada. Rejeição e conservação foram medidas de verdade. GunFinishReloadEvent não disparou nessa versão; o estado nativo e a munição comprovam a conclusão. Ver [detalhes e medidas](ARSENAL_0.005.md).

Os modelos, animações FPS e a convivência visual com os personagens GeckoLib foram vistos em capturas reais. A instrumentação de som confirma um disparo no sistema de áudio; não substitui audição ou aprovação humana. O TaCZ emite avisos de sons auxiliares ausentes em alguns keyframes, preservados como limitação da distribuição externa.

Pendentes: causa dos timeouts intermitentes de login nas repetições, latência/reconexão em rede externa, sessões maiores, outros gunpacks, shaders e matriz completa de acessórios. Uma execução local completa com dois clientes não prova todos esses casos. Nenhuma arma foi automaticamente equipada nos personagens políticos.

Conferência gráfica final: a aba criativa foi aberta no Minecraft, selecionada e inspecionada, com cinco armas, quatro munições e os itens anteriores. [Imagem](preview/arsenal-0.005/aba-criativa.png), [cliente final](validacao/arsenal-0.005/singleplayer-final-cliente.json), [servidor integrado final](validacao/arsenal-0.005/singleplayer-final-servidor.json); log `.tools/arsenal-final-visual.log`, `BUILD SUCCESSFUL`. Os timeouts também ocorreram na tentativa sequencial; não declarar essa estratégia como uma correção comprovada. Os avisos de áudio incluem uma referência agregada de recarga da MP5; sons principais de tiro foram verificados, mas não se afirmou que cada keyframe tem áudio disponível.

## Histórico — sprint 0.004.0

Executada em Windows 11, JDK 17.0.20.1, Minecraft 1.20.1, Forge 47.4.28 e GeckoLib 4.8.4, em 24/10/2026. Os resultados abaixo são desta sprint; os registros da 0.003.0 foram preservados depois desta seção como histórico.

| Verificação | Resultado real | Evidência local |
| --- | --- | --- |
| Build e reobf | **Passaram**; JAR 0.004.0 e diretório com as duas dependências prontos | `.tools/gecko-final-tests.log` |
| Servidor GameTest sem GUI | **31 testes obrigatórios passaram**, zero falhas | Mesmo log, linha `All 31 required tests passed` |
| GeckoLib no cliente | Carregou e renderizou todos os modelos/clipes usados no cenário | `.tools/gecko-final-visual.log`, `BUILD SUCCESSFUL` |
| Recursos | JSONs, idiomas, legendas, OGGs, PNGs e hashes passaram | Tarefas `validateAssets` e `validateGeckoAssets` |
| UV/articulação | 14 ossos, 20 cubos, oito clipes; continuidade de 6+6 linhas das extremidades | Validador e skins vistas nas capturas |
| Geração reproduzível | SHA-256 dos dois JSONs idênticos antes/depois de regenerar | Comparação efetivamente executada |
| Alcance | Aproximadamente 2,5 blocos; rejeita alvos distantes, verticais e através de parede | GeckoSprintGameTests |
| Dano/knockback | Renan 4, Arthur 5, tropa 6; durabilidade e impulso moderado verificados | GameTests e capturas de impacto |
| Variantes | Equipamentos, ovos, atributos, ID legado e bloqueio de invocação no Combatente passaram | GameTests |
| Speed I | Só após cast bem-sucedido, infinito, amplificador 0, save/load e sem empilhamento | GameTests |
| Emergência | Cinco unidades, escalonamento, nenhum ataque antes de acabar, fase preservada ao salvar | GameTests + capturas reais |
| Novo áudio | Fonte mudou de hash; MP3 5,903667 s convertido inteiro e recurso disponível | FFprobe, hashes e validador; audição humana não presumida |
| Servidor dedicado convencional | Forge/Gecko userdev chegaram ao bootstrap; servidor parou solicitando aceite da EULA | `.tools/gecko-dedicated.log`; `run-server/eula.txt` mantém `eula=false` |
| Dois clientes reais | **Pendente**: segunda JVM abriu, alcançou handshake Forge em localhost e não concluiu login | `.tools/gecko-multiplayer-host3.log`, `.tools/gecko-multiplayer-observer3.log` |
| JAR distribuível | Não contém classes GameTests, cenários visuais ou estrutura de teste | ZIP do JAR inspecionado |
| Publicação | Commit e push autorizados pelo autor após testar a versão; nenhum merge solicitado | Consultar o histórico Git |

O conjunto possui 22 testes anteriores adaptados ao tempo de emergência e nove novos: variantes/ovos/legado, Combatente sem poder, Speed persistente após sucesso, cast falho sem Speed, alcance/AABB, parede, dano/durabilidade de Arthur, knockback e escalonamento/save da fase. O caso de 700 ticks continua verificando uma e depois duas levas.

As capturas do framebuffer do cliente foram abertas e inspecionadas: [skins](preview/gecko-0.004/skins-variantes.png), [mãos](preview/gecko-0.004/maos.png), [apoio](preview/gecko-0.004/apoio.png), [cotovelos](preview/gecko-0.004/cotovelos.png), [joelhos](preview/gecko-0.004/joelhos.png), [soco de Renan](preview/gecko-0.004/soco-renan.png), [espada de Arthur](preview/gecko-0.004/espada-arthur.png) e [painel](preview/gecko-0.004/painel.png). São imagens do Minecraft em 1280×720; não são mockups. O teste cria uma cópia de mundo isolada e não usa o mundo do jogador em run/.

Durante a implementação, o primeiro cliente falhou no refmap do TextureManagerMixin. A configuração SRG → official dos runs foi corrigida e as execuções gráficas posteriores completaram. Dois testes antigos esperavam emergência em 25 ticks; foram adaptados à regra de 44 ticks e ao escalonamento, depois passaram com o conjunto completo. Na tentativa multiplayer, o primeiro listener foi IPv6; foi corrigido para IPv4 local, mas o handshake continuou sem completar. Esse resultado não constitui aprovação de combate em rede.

Pendentes: login/combate com dois clientes, rede com latência/reconexão, partida no servidor dedicado convencional após aceite pessoal da EULA, audição do efeito novo e avaliação de fluidez/estética pelo autor. O ambiente não permite declarar esses itens aprovados por inferência a partir do build. O funcionamento de skins e falas do protótipo anterior já havia sido confirmado pelo autor; essa confirmação não foi estendida automaticamente à nova animação.

Arquivos e implementação: [GECKOLIB_0.004.md](GECKOLIB_0.004.md).

Verificação adicional do painel: a [prévia 3D completa](preview/gecko-0.004/previa-3d.png) foi capturada após rolar a página e inspecionada. O recorte parcial no fim da página sem rolagem é o scissor do conteúdo; o corpo renderiza inteiro quando a prévia entra na área visível. Log: `.tools/gecko-preview-check.log`, execução concluída. SHA-256 do JAR: `67379a30e7a48738719a20a467b7084be0d70866136de287b84adc62067ccbca` (1.115.886 bytes). O pacote também contém o GeckoLib original, de 1.038.208 bytes.

## Histórico — sprint 0.003.0

24/10/2026. Minecraft 1.20.1, Forge 47.4.28, JDK 17, Gradle 8.8 e ForgeGradle 6.0.54. Os relatórios anteriores estão preservados abaixo.

## Resultados da invocação e animações

| Verificação | Resultado real |
| --- | --- |
| Build | `BUILD SUCCESSFUL`; artefato `build/libs/guerra_ezuberante-0.003.0.jar` |
| Servidor Forge | **22 GameTests obrigatórios passaram**, incluindo as 12 regressões anteriores e dez testes novos |
| Primeira leva | Agressão válida iniciou cast; nenhuma unidade antes da conclusão; cinco unidades após o cast |
| Terreno | Posições físicas sem colisão/overlap; terreno sólido bloqueado e lava rejeitados |
| Emergência | Ataques bloqueados durante a fase de subida, liberados ao concluir os 25 ticks |
| Espada | Modificadores e durabilidade iguais à espada de ferro; dano efetivo seis no alvo sem resistência/armadura; um ponto de desgaste |
| Limite por dono | Leva parcial respeitou as duas vagas restantes; limite de um Arthur não impediu a leva de outro |
| Intervalo | Percorridos mais de 700 ticks; cinco unidades antes da segunda leva e dez após a segunda conclusão |
| Aliados e alvos | Sem agressão espontânea; invocador, irmãs e Renan protegidos; alvo legítimo do dono compartilhado |
| Acompanhamento | Unidade ociosa reduziu distância até Arthur usando navegação nativa |
| Persistência | UUID de dono, emergência, contagem persistente e cooldown de Arthur preservados em save/load |
| Morte do dono | Unidade dissipada e vaga liberada |
| Fim de combate | Alvo removido cancelou o cast sem conceder tropas |
| Animação/impacto | Dano do Renan observado no frame 5 e com valor original quatro, sem duplicação |
| Áudios anteriores | Regressões do phonk e da fala selecionada de Arthur passaram; eventos e OGGs anteriores preservados |
| Recursos | Três skins 64×64, espada 16×16, JSONs, traduções, legendas e 11 OGGs verificados |
| Cliente gráfico | Execução isolada carregou um mundo, capturou conjuração, emergência, combate e painel e encerrou normalmente |
| Inspeção visual | PNGs do framebuffer foram vistos: atlas correto, subida de baixo do terreno, espada na mão e três opções no painel |

Logs locais: `.tools/summon-final.log`, `.tools/summon-build-final.log` e `.tools/summon-client.log`. Capturas inspecionadas estão em [docs/preview](preview/). O arquivo real `serverconfig/guerra_ezuberante-server.toml` gerado pelo Forge também foi lido e conferido.

Durante a implementação, a inspeção visual identificou inversão do deslocamento vertical no espaço de modelo. A direção foi corrigida e o cliente foi executado novamente em cópia nova do mundo para conferir a subida. Os testes também identificaram a necessidade de considerar os acertos das tropas como progresso de combate do Arthur; a correção foi incluída antes do resultado final aprovado.

## Limitações que permanecem

- Não foi realizada uma partida com dois jogadores reais conectados a um servidor dedicado normal. As verificações de servidor e cliente isolado não equivalem a esse teste.
- A audição e a mixagem dos sons por jogadores reais ainda precisam ser confirmadas. Foi verificado o recurso real de invocação e o acionamento pelo servidor; não se declara teste auditivo automatizado.
- Capturas estáticas comprovam estados visuais inspecionados, mas não substituem avaliação humana de toda a fluidez das transições. As animações podem ser refinadas após o autor jogar.
- A navegação completa das telas, todas as resoluções e uma sessão longa com o limite de tropas cheio não foram avaliadas manualmente.
- Há avisos de depreciação na stack fixada, sem falhas de compilação ou carregamento.

O harness visual fica em `src/gameTest/`, só é habilitado por `-PvisualTest` e recusa executar fora de `.tools/run-summon-client/`. Ele não entra no JAR distribuído nem modifica o mundo normal do usuário.

Comandos e detalhes: [INVOCACAO_0.003.md](INVOCACAO_0.003.md).

---
## Histórico — sprint 0.002.0

24/10/2026. Minecraft 1.20.1, Forge 47.4.28, Java 17, Gradle 8.8 e ForgeGradle 6.0.54. Os resultados da primeira versão estão preservados no histórico abaixo.

## Resultado atual

| Verificação | Resultado real |
| --- | --- |
| Build final | `BUILD SUCCESSFUL`; JAR `build/libs/guerra_ezuberante-0.002.0.jar` |
| Servidor Forge GameTest | **12 testes obrigatórios passaram**, incluindo as duas regressões do Renan |
| Ovos | Os dois tipos e o caminho de spawn por `MobSpawnType.SPAWN_EGG` foram executados no servidor |
| Arthur | Vida, dano recebido, persistência, hitbox 0,528 × 1,584 e olhos a 1,4256 verificados |
| Paz inicial | Renan e Arthur convivem próximos de outro mob sem selecionar alvos nem causar dano |
| Retaliação | Cada personagem identificou um agressor e o alcançou com navegação nativa, causando dano |
| Jogadores | FakePlayers em Survival e Adventure aceitos como agressores; Creative ignorado. Isso não é uma sessão com jogadores reais |
| Dano ambiental | Queda não iniciou perseguição |
| Troca de alvo e alcance | Novo agressor substituiu o anterior; alvo além do limite foi abandonado |
| Paredes e alvo inacessível | Sem dano através da barreira; perseguição sem progresso encerrada |
| Combate entre personagens | Arthur revidou contra Renan; Renan reagiu ao dano recebido. A morte de Arthur encerrou o alvo do Renan |
| Phonk | Entrada em combate iniciou a recarga; outro golpe e troca de agressor não reiniciaram a música |
| Reação selecionada de Arthur | Teste dedicado confirmou É vagabundo sim, recarga de 220 ticks, ausência de reinício por novo golpe e persistência da recarga |
| Vozes de Arthur | Sete recursos presentes; botão direito acionou uma gravação e a recarga impediu repetição imediata |
| Arquivos de áudio | MP3s preservados, convertidos sem cortes; as duas duplicatas idênticas não geraram OGGs extras |
| Recursos | Validador confere as duas skins 64×64 e suas fontes, traduções, JSONs, legendas e os dez OGGs |
| Cliente 0.002.0 | `runClient` iniciou e carregou um mundo local em `.tools/run-combat-client/`, separado do mundo do usuário |

Logs locais: `.tools/arthur-reaction-validation.log` (12 testes e build atuais), `.tools/combat-final.log` e `.tools/combat-client.log`. As confirmações do autor sobre a skin e o áudio do Renan na primeira versão continuam registradas no histórico. Não houve confirmação visual do Arthur pelo autor nesta sprint.

## Limitações atuais

- A seleção do áudio foi resolvida pelo autor: “É vagabundo sim”, com recarga de 11 s. A audição dessa reação no cliente ainda precisa de confirmação manual; o teste de servidor verifica sua seleção e a proteção contra reinício.
- A aparência do Arthur, a animação de socos e a navegação das telas atualizadas ainda precisam de confirmação visual manual. A inicialização do cliente não substitui essa confirmação.
- A instância de validação usou uma cópia isolada do mundo GameTest e foi encerrada após o carregamento. O mundo da primeira versão não foi modificado por essa validação.
- Não houve sessão com dois jogadores reais conectados. Alcance audível, sincronização percebida entre clientes e volume durante perseguição ainda precisam desse teste.
- Os testes de recarga e interação verificam o comportamento de servidor, não a audição das gravações.
- A renderização utiliza o PlayerModel nativo; não existem animações especiais, poderes, tropas ou facções jogáveis.

## Repetir

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/validate_assets.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File tools/gradle.ps1 runGameTestServer build
powershell -NoProfile -ExecutionPolicy Bypass -File tools/gradle.ps1 runClient
```

Arquivos alterados e teste manual da sprint: [COMBATE_0.002.md](COMBATE_0.002.md). Inventário de áudio: [AUDIOS.md](AUDIOS.md).

---
## Histórico — protótipo 0.001.0

Data: 24/10/2026. Plataforma: Windows, Minecraft 1.20.1, Forge 47.4.28, JDK 17, ForgeGradle 6.0.54 e Gradle Wrapper 8.8.

## Resultados observados

| Verificação | Resultado | Evidência e limite |
| --- | --- | --- |
| Origem do MDK | Conferida | MDK oficial 1.20.1-47.4.28; SHA-1 `eee9145e892bb5ca5fe72323bd81c8522c53417f`, igual ao publicado pelo Forge |
| Build | Aprovado | `build` concluiu com `BUILD SUCCESSFUL`; JAR gerado em `build/libs/guerra_ezuberante-0.001.0.jar` |
| Recursos | Aprovado | `tools/validate_assets.ps1`: JSONs, tradução pt/en, legendas, OGGs, PNG 64×64 e igualdade com a skin fonte |
| Áudio convertido | Aprovado | FFprobe identificou Vorbis mono, 44.100 Hz, nos dois OGGs; originais preservados sem cortes |
| Servidor Forge | Aprovado em ambiente GameTest | `runGameTestServer` iniciou no lado servidor, carregou o mod e executou dois testes; ambos passaram |
| Ovo e entidade | Aprovado no teste de servidor | Registro do ovo aponta para Renan e a entidade foi criada e adicionada a um mundo real do servidor |
| Vida, dano e morte | Aprovado no teste de servidor | Vida inicial 20, dano de 4 reduz para 16, dano fatal conclui a morte |
| Salvamento | Aprovado no teste de servidor | Dados serializados e carregados preservam vida e persistência da entidade |
| Interação e recarga de voz | Aprovado no teste de servidor | Interação com FakePlayer inicia recarga, segundo clique não reinicia fala, recarga é salva e diminui com os ticks |
| Cliente gráfico | Inicialização aprovada | `runClient` carregou o Forge, recursos e engine de som; mundo local foi aberto |
| Tecla K | Aprovada | Painel inicial capturado e inspecionado no cliente, em janela de 854×480 |
| Skin do Renan | Confirmada pelo autor | O autor informou que testou e a skin deu certo dentro do jogo |
| Falas por botão direito | Confirmadas pelo autor | O autor informou que os áudios tocaram ao clicar com o botão direito no personagem |

Os logs completos locais estão em `.tools/build.log`, `.tools/gametest.log` e `.tools/client.log`. Eles são arquivos temporários ignorados pelo Git. Não há alegação de teste automatizado de audição: o teste de servidor verifica interação, recursos e recarga; a reprodução audível foi confirmada pelo autor no cliente.

## O que os GameTests verificam

`entityRegistrationDamageAndPersistence` valida o vínculo do spawn egg, a criação da entidade no servidor, os atributos, o recebimento de dano, o salvamento/carregamento e a morte.

`voiceInteractionAndCooldown` valida a presença dos recursos de voz no classpath do servidor, a interação por mão principal, a proteção contra cliques repetidos, a persistência da recarga e sua redução ao longo dos ticks.

Os testes e o template de mundo ficam em `src/gameTest/`, separados do código distribuído. O servidor GameTest é uma execução dedicada de testes; não substitui a validação de uma sessão normal com vários jogadores.

## Limites e verificações ainda manuais

- A página inicial do painel foi inspecionada. A navegação completa pelas outras quatro páginas, o link externo e a rolagem em múltiplas resoluções ainda precisam de uma rodada manual completa.
- A persistência foi verificada por serialização/carregamento no servidor; fechar e reabrir um mundo de jogador é uma verificação manual adicional.
- Não foi realizada uma partida com dois clientes conectados a um servidor dedicado normal. Alcance e volume percebidos por vários jogadores ainda precisam desse teste.
- Não foram medidos desempenho de batalhas ou limites de tropas; esses sistemas ainda não existem.
- A IA atual é de caminhada e observação. Nenhuma IA de combate, facção jogável, invocação de tropas ou poder especial foi implementado.

Há avisos de depreciação em APIs do Forge e no plugin Gradle da stack fixada. Eles não impediram o build nem os testes. Não foi feita migração para APIs de outra versão do Minecraft.

## Repetir as verificações

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/validate_assets.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File tools/gradle.ps1 build
powershell -NoProfile -ExecutionPolicy Bypass -File tools/gradle.ps1 runGameTestServer
powershell -NoProfile -ExecutionPolicy Bypass -File tools/gradle.ps1 runClient
```

Para uma sessão de servidor normal, execute `runServer` e siga os requisitos de configuração e EULA descritos pelo Minecraft. O agente não alterou a aceitação da EULA do servidor normal.
