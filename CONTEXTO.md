# Contexto do projeto — Guerra Ezuberante

Atualizado em 24/10/2026. Leia o [README](README.md) para design, tecnologias, comandos e roadmap, e [docs/VALIDACAO.md](docs/VALIDACAO.md) para os resultados reais dos testes.

## Pedido atual e decisões confirmadas

- Nome: **Guerra Ezuberante**, com z.
- Sprint atual: adicionar Arthur, retaliação compartilhada e reações de áudio, preservando a base existente. O autor forneceu a skin 64×64 do Arthur e novos MP3s.
- Stack definida: **Minecraft Java 1.20.1, Forge 47.4.28, Java 17, Gradle Wrapper 8.8 e ForgeGradle 6.0.54**. A sugestão histórica de NeoForge foi substituída.
- Mod ID: `guerra_ezuberante`; pacote: `io.github.omarflk.guerraezuberante`; versão: `0.002.0`.
- Ambiente principal: Windows e IntelliJ IDEA.
- Renan e Arthur usam skins clássicas 64×64 do autor e PlayerModel nativo, sem GeckoLib. Arthur possui escala visual 0,88, hitbox 0,528 × 1,584 e olhos a 1,4256.
- Após os testes, o autor autorizou explicitamente o commit e o push das alterações atuais para a main. Publicações futuras continuam dependendo do pedido vigente.

## Estado implementado

- Projeto Forge baseado no MDK oficial, com Wrapper, metadados, recursos e .gitignore.
- Renan: 30 HP, dano 4, velocidade 0,25, hitbox 0,6 × 1,8. Arthur: 32 HP, dano 5, velocidade 0,24, hitbox proporcional à escala 0,88. Ambos perseguem até 20 blocos.
- Base compartilhada: `RetaliatingCharacterEntity`; socos com IA nativa e linha de visão, sem ataques através de paredes. Não há seleção espontânea de alvos. Dano com agressor válido inicia retaliação; novo agressor substitui o alvo. Sem progresso recente por 200 ticks, a perseguição termina.
- Persistência e recargas salvas; sem despawn por distância. Ao carregar, referências antigas de agressor são descartadas. Dano ambiental e jogadores Creative/Spectator/invulneráveis não iniciam retaliação; Survival e Adventure são válidos.
- Ovos `guerra_ezuberante:renan_spawn_egg` e `guerra_ezuberante:arthur_do_val_spawn_egg` na mesma aba criativa; ambos passam pelo caminho de spawn testado no servidor.
- Skin do autor copiada sem alterações para `assets-entrada/skins/` e `textures/entity/renan_santos.png` nos recursos.
- Áudios reais: **Me Respeita**, aproximadamente 13,48 s; **risadinha**, aproximadamente 2,38 s. Ambos Vorbis mono, 44.100 Hz.
- Botão direito por mão principal escolhe uma fala aleatória disponível. O servidor inicia o som, com recarga por entidade de 15 s ou 4 s. A recarga é salva. Sem pacotes de rede personalizados.
- Phonk separado encontrado: “Me respeita, creu creu - PHONK.mp3”, 12,59 s. Toca na entrada em combate com recarga de 14 s e áudio nativo vinculado à entidade. Não reinicia a cada golpe.
- Painel configurável na tecla K com cinco páginas: Início, Personagens, Facções, Habilidades e Sobre. As duas últimas seções de gameplay são informativas, sem executar sistemas futuros.
- Painel preto/cinza/amarelo preservado: seleção de Renan e Arthur com atributos, prévias e estado de combate implementado. Português e inglês acompanham o idioma do jogo.
- Código cliente isolado em subscribers `Dist.CLIENT`, incluindo renderer, tecla e telas.

## Recursos e ferramentas

- MP3s originais preservados em `audios/`; cópias padronizadas em `assets-entrada/audio/`. Ambas as pastas de áudio original são ignoradas pelo Git.
- OGGs distribuíveis: `sounds/renan/` e `sounds/arthur/` em assets/guerra_ezuberante. Entradas separadas por personagem em assets-entrada/audio/; nove MP3s do Arthur equivalem a sete gravações únicas. As duas cópias privatizar_tudo(1)/(2) são idênticas a parar_de_mamat.
- `tools/convert_audio.ps1`: FFmpeg do PATH ou portátil, conversão sem cortes, normalização de nomes e detecção de colisões.
- `tools/validate_assets.ps1`: verifica JSON, idiomas, legendas, OGGs e skin PNG 64×64 idêntica à fonte.
- `tools/gradle.ps1`: seleciona o JDK 17 portátil, quando presente, e usa `.gradle-user/` como cache local.
- JDK e FFmpeg portáteis em `.tools/` são ferramentas locais ignoradas pelo Git; não são dependências empacotadas do mod.
- Source set `src/gameTest/` contém 12 testes de integração e template ampliado; permanece fora do JAR distribuído.

## Validação realizada

- Build da sprint concluído; JAR atual: `build/libs/guerra_ezuberante-0.002.0.jar`. O JAR 0.001.0 é o protótipo anterior; usar apenas uma versão por instalação.
- Os 12 GameTests passaram: ovos/criação, atributos, escala/olhos, paz inicial, dano/morte, persistência, Survival/Adventure/Creative, perseguição, socos, paredes, troca de alvo, fim da perseguição e recargas.
- Cliente Minecraft iniciou, carregou recursos e abriu o painel pela tecla K; página inicial inspecionada em janela 854×480.
- **O autor confirmou no jogo que a skin deu certo.**
- **O autor confirmou que os áudios tocaram ao clicar com o botão direito no Renan.**
- Navegação completa em todas as páginas, várias resoluções, reconexão real e sessão multiplayer com dois clientes permanecem como testes manuais adicionais. Não apresentar esses testes como já executados.

## Ideia geral preservada

Combate político satírico e multiplayer, com personagens, poderes absurdos, tropas, transformações e batalhas entre facções. Separar **facção**, **classe de combate** e **personagem/entidade**.

| Facção planejada | Núcleo |
| --- | --- |
| Esquerda Lulista | Lula, Janones, Boulos, burros zumbificados, artistas, feijão e picanha |
| Direita Bolsonarista | Jair, Flávio, Eduardo, Nikolas, Trump e Netanyahu |
| Direita Missão | Renan, Arthur, Glenda, loiras ucranianas e cavaleiros de onças |
| Esquerda Independente | Defesa, recuperação e suporte |
| Direita Independente | Dano individual, mobilidade e precisão |
| Centro | Defesa, mobilidade e suporte adaptativos |

Classes futuras: Combatente, Tanque, Invocador, Especialista e Suporte. Modos futuros: laboratório criativo e guerra de facções. A estrutura de conteúdo futura está no README; não implementar todos os sistemas de uma vez.

## Próximo marco

A sprint 0.002.0 implementa Arthur e retaliação básica. Falta confirmar visualmente Arthur e testar dois jogadores reais. Depois vem v0.003, primeiro poder com projéteis e recarga, mediante pedido do autor.

Facções jogáveis, poderes especiais, exércitos, diplomacia, progressão e personagens além de Renan/Arthur continuam pendentes. Novas artes serão feitas com o autor.

Repositório: https://github.com/OmarFLK/A-Guerra-Ezuberante — branch `main`. O último estado remoto anterior à implementação é o commit de documentação `a45140f`. Consulte `git status` antes de versionar a implementação.

## Reação de áudio do Arthur confirmada

O autor escolheu explicitamente “É vagabundo sim”, gravação inteira de 9,98 s, como reação do Arthur na entrada em combate. ARTHUR_REACTIONS utiliza essa gravação com recarga de 220 ticks (11 s). Golpes durante o mesmo combate não reiniciam o som. As sete falas permanecem no botão direito.

Inventário: [docs/AUDIOS.md](docs/AUDIOS.md). Entrega e arquivos: [docs/COMBATE_0.002.md](docs/COMBATE_0.002.md). Uma instância cliente isolada foi iniciada em `.tools/run-combat-client/` e carregou um mundo; isso não constitui confirmação visual do Arthur nem teste de dois clientes em rede.
