# Validação — sprint 0.004.0

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
