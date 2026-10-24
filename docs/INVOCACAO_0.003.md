# Exército Ucraniano e animações — 0.003.0

Implementação sobre a base local 0.002.0, mantendo Minecraft 1.20.1, Forge 47.4.28 e Java 17. Os atributos, a skin, os registros e os áudios do Renan foram preservados. Não foram adicionados poderes novos ao Renan.

## Invocação de Arthur

Arthur continua pacífico até uma agressão válida. Quando entra legitimamente em combate e tem capacidade disponível, inicia uma preparação de 10 ticks e uma conjuração de mais 10 ticks. Ele para brevemente, encara o alvo e ergue o braço. Partículas azuis e amarelas acompanham a conjuração.

Ao concluir os 20 ticks, cria até cinco combatentes em posições seguras ao redor dele. Cada combatente passa 25 ticks emergindo visualmente do chão, sem atacar durante esse período. Depois começa a defender o invocador.

O cooldown de 700 ticks é individual por Arthur e começa no início da conjuração. Em combate contínuo, isso mantém 700 ticks entre levas concluídas. A primeira leva não espera 35 segundos. Se o combate acabar antes do fim do cast, a invocação é cancelada; o cooldown já iniciado permanece.

Enquanto as tropas acertam o inimigo legítimo de Arthur, isso conta como progresso de combate. Assim, Arthur não desiste apenas porque suas próprias aliadas estão entre ele e o alvo. Se ninguém conseguir avançar nem acertar, a perseguição bloqueada termina normalmente.

```mermaid
sequenceDiagram
    participant E as Agressor
    participant A as Arthur no servidor
    participant L as Registro de invocações
    participant U as Combatentes
    participant C as Clientes
    E->>A: Causa dano válido
    A->>L: Verifica quantidade ativa deste dono
    A-->>C: Estado de conjuração e um som principal
    Note over A: 10 ticks preparando + 10 conjurando
    A->>U: Cria até cinco em terreno seguro
    A->>L: Registra UUIDs e reserva vagas
    A-->>C: Posições físicas e início da emergência
    Note over U,C: 25 ticks de emergência visual; ataques bloqueados
    U->>A: Recupera dono e alvo legítimo
    U->>E: Persegue e ataca com espada
    Note over A: Nova leva só após cooldown e com combate/capacidade
```

## Combatente Ucraniana

Identificador: `guerra_ezuberante:combatente_ucraniana`.

Usa o atlas PNG 64×64 fornecido em `skin-loira.png`, preservado sem edição ou redimensionamento. O modelo é o humanoide clássico com a textura enviada. Possui 15 HP por padrão, movimento 0,25 e espada na mão principal.

Quando está sem alvo, acompanha Arthur por navegação nativa: começa a segui-lo acima de cinco blocos de distância e se aproxima até aproximadamente 2,5 blocos. O caminho é recalculado em intervalos, sem teletransporte e sem busca global de entidades a cada tick.

Compartilha o alvo atual e legítimo do dono e pode revidar a agressões. As verificações de alvo e de dano impedem atacar o próprio invocador, outras combatentes do mesmo Arthur e Renan. As alianças explícitas por equipes vanilla também são respeitadas. Isso não cria um sistema político completo de facções.

### Emergência e segurança do terreno

As posições físicas já nascem sobre terreno sólido e livre. Uma busca local e limitada procura posições em direções distribuídas, com ajuste vertical de até três blocos. Verifica colisão, sobreposição, borda do mundo, disponibilidade do chunk, água/lava e perigos como fogo, cactos e magma.

Se só houver duas posições seguras, aparecem duas unidades. Se não houver nenhuma, não aparece nenhuma; o resultado reduzido é registrado em log de depuração. Não há destruição de blocos.

O renderer desloca apenas o modelo para baixo e interpola sua subida com easing cúbico. A hitbox permanece em posição válida. A camada da espada recebe a mesma transformação, acompanhando a mão. Ao concluir a emergência, ocorre um pequeno efeito azul e amarelo, e os ataques são liberados.

### Dono, limite e ciclo de vida

- Cada combatente salva o UUID do Arthur, tempo de vida e estado restante da emergência.
- O `SummonLedger` usa SavedData por dimensão para guardar as vagas ocupadas por dono; unidades descarregadas continuam contando para o limite.
- O limite inicial é 30 por Arthur. Se restarem duas vagas, a próxima leva cria apenas duas.
- A morte ou remoção de uma unidade libera sua vaga. Morrer, ser descartado ou mudar de dimensão encerra as invocações do Arthur original.
- Uma unidade com dono morto é dissipada com partículas simples. Se o dono estiver ausente/descarregado, há tolerância de 200 ticks antes da remoção, sem forçar carregamento de chunks.
- A duração inicial é de 6.000 ticks carregados, aproximadamente cinco minutos a 20 TPS. Isso evita tropas permanentes; ambos os limites de ciclo de vida são configuráveis.
- Unidades invocadas não fornecem loot nem espadas gratuitas ao morrer. Não existe spawn egg avulso das combatentes nesta sprint; a entrada normal é a habilidade de Arthur.

Ao salvar Arthur, o cooldown permanece. A base de retaliação limpa referências antigas de alvo no carregamento; um cast que perder seu alvo é cancelado, sem conceder uma leva extra. O vínculo das tropas pode ser recuperado pelo UUID, sem depender de referências Java antigas.

## Espada Ucraniana

Identificador: `guerra_ezuberante:espada_ucraniana`.

Registrada como `SwordItem(Tiers.IRON, 3, -2.4F, ...)`, igual à espada de ferro da versão alvo. O modificador de dano do item é +5; com o ataque base 1 da combatente, resulta em **6 pontos nominais**, sem somar outros seis nem usar parte do dano de Arthur.

O item conserva a durabilidade de ferro, os modificadores de velocidade, a categoria de encantamentos e o comportamento de desgaste. Um golpe bem-sucedido da combatente utiliza o método do próprio item para desgaste. Efeitos vanilla, resistência, armadura e encantamentos podem modificar o dano efetivamente recebido.

A textura original de 16×16 está em `textures/item/espada_ucraniana.png`, com modelo `minecraft:item/handheld`. A textura das espadas de ferro vanilla não é alterada. A espada própria também aparece na aba Guerra Ezuberante para testes.

## Animações e sincronização

A solução escolhida foi **PlayerModel nativo + poses originais em Java + SynchedEntityData**. Isso preserva os três atlas existentes e dispensa dependências ou conversões para modelos GeckoLib.

Os ataques têm uma sequência de 12 ticks. O servidor valida alcance e linha de visão ao iniciar e novamente no impacto, no tick 5. Cada sequência pode aplicar dano uma única vez, e o intervalo da IA nativa continua controlando o próximo ataque. Se o alvo mudar, morrer ou ficar inválido antes do impacto, o golpe é cancelado.

Renan e Arthur têm preparação, soco, leve rotação do tronco e recuperação; ocasionalmente usam a esquerda. A combatente faz uma preparação e um golpe de espada com recuperação. Caminhada e idle utilizam o sistema humanoide nativo; a reação a dano combina o feedback vanilla com uma pequena inclinação visual do tronco.

A fase de ataque, a mão escolhida, a conjuração e a emergência são sincronizadas pelo sistema nativo de dados da entidade. O cliente interpola as poses e não calcula dano, não concede tropas e não decide cooldowns. O equipamento e os sons usam a sincronização vanilla/Forge existente. Não foram criados pacotes personalizados.

## Áudio de invocação

O autor apontou a pasta `audios/ucraniana/`. Ela contém uma gravação: `🔊Som e Efeito Anime, gemido de laser descendente, lento #para jogos (HD) - AGE Efeitos Sonoros.mp3`, com aproximadamente **8,65 segundos**.

O arquivo inteiro foi preservado. A cópia de entrada é `assets-entrada/audio/arthur/invocacao.mp3`; o OGG fica em `sounds/arthur/invocacao.ogg`, Vorbis mono a 44.100 Hz. Evento: `guerra_ezuberante:arthur.invocacao`, com legenda traduzível.

Arthur inicia uma única instância principal no início do cast, vinculada à sua entidade pelo protocolo nativo. As cinco unidades não repetem o som. Não houve corte, aceleração ou substituição por um som inventado. Se o administrador reduzir muito o intervalo das levas, uma proteção de áudio impede sobrepor outra cópia antes do término da anterior.

As falas existentes de Arthur e o phonk do Renan permanecem nos eventos anteriores. Os OGGs antigos também foram preservados com seus bytes originais do commit anterior.

## Configuração Forge

Arquivo por mundo: `serverconfig/guerra_ezuberante-server.toml`. No ambiente de desenvolvimento, fica dentro da pasta do mundo em `run/saves/<mundo>/serverconfig/`. Para um servidor normal de desenvolvimento, em `run-server/world/serverconfig/`.

```toml
[arthur]
arthur_summon_enabled = true
arthur_summon_count = 5
arthur_summon_cooldown_ticks = 700
arthur_max_active_summons = 30
ukrainian_fighter_max_health = 15.0
ukrainian_fighter_emergence_ticks = 25
ukrainian_fighter_lifetime_ticks = 6000
ukrainian_fighter_orphan_grace_ticks = 200
```

As mudanças de vida aplicam-se às novas invocações. Os tempos são ticks de servidor; os segundos indicados pressupõem 20 TPS. As unidades já ativas não são removidas só porque o administrador reduziu o limite, mas Arthur não cria novas acima da capacidade configurada.

## Testes e resultado

**22 GameTests obrigatórios passaram**, incluindo as 12 regressões da versão anterior e dez cenários da sprint. O teste de dano confirmou seis pontos com a espada sem encantamentos e um ponto de desgaste por golpe. O teste de animação observou dano do Renan no frame 5, sem duplicação. O teste de levas percorreu mais de 700 ticks de servidor e verificou cinco unidades antes do intervalo e dez após a segunda conclusão.

O build final concluiu com sucesso. O cliente gráfico iniciou em uma cópia isolada do mundo, capturou preparação, emergência, combate e painel, e foi encerrado normalmente. As capturas permitiram inspecionar os modelos, a espada na mão e a direção da subida; isso não equivale a uma avaliação humana de toda a fluidez da animação nem a uma partida com dois jogadores reais.

Capturas: [emergência](preview/ucranianas-emergindo.png), [combate](preview/ucranianas-combate.png) e [painel](preview/painel-0.003.png).

Não foi executada uma sessão com dois clientes conectados a um servidor dedicado normal. A audição e a mixagem dos sons por dois jogadores ainda precisam desse teste. Há avisos de depreciação da stack Forge/Gradle, sem erro de compilação ou carregamento.

## Arquivos da sprint

| Área | Arquivos principais |
| --- | --- |
| Configuração | `config/ModConfig.java` e registro na classe principal |
| Invocação | `summon/ArthurSummoning.java`, `summon/SummonLedger.java`, alterações em `ArthurDoValEntity.java` |
| Combatente | `entity/UkrainianFighterEntity.java` e registro em `ModEntities.java` |
| Ataques | `RetaliatingCharacterEntity.java`, preservando os parâmetros existentes de Renan/Arthur |
| Visual | `client/model/CombatHumanoidModel.java`, `client/renderer/UkrainianFighterRenderer.java`, renderizadores existentes e registro cliente |
| Itens | `ModItems.java`, `ModCreativeTabs.java`, modelo JSON e textura da espada |
| Som | `ModSounds.java`, `sounds.json`, OGG de invocação e legendas |
| Interface | `ModPanelScreen.java` e traduções pt/en, com terceira opção de personagem |
| Testes | `SummonGameTests.java`, ajustes de regressão, template de mundo ampliado e `ClientVisualChecks.java` somente para validação |
| Ferramentas/build | Validador de assets, versão 0.003.0 e opção de execução visual isolada |
| Documentação | README, contexto, inventário de áudio, validação e este documento |

Nenhum mod de referência foi adicionado como dependência de gameplay. Não foram copiados código, skins, modelos, músicas ou animações desses projetos. Não foi realizado push ou merge nesta sprint.

## Executar

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/validate_assets.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File tools/gradle.ps1 runGameTestServer build
powershell -NoProfile -ExecutionPolicy Bypass -File tools/gradle.ps1 runClient
```

JAR: **`build/libs/guerra_ezuberante-0.003.0.jar`**. Use somente um JAR do mod por instalação.

Para testar manualmente: invoque Arthur e Renan pelos ovos da aba, pegue a espada nova e confirme que continuam pacíficos. Em Survival ou Adventure, ataque Arthur e observe o cast, as cinco unidades, o som e a emergência. Mantenha o combate por mais de 35 segundos para a segunda leva. Ataque Renan separadamente para conferir seus socos e phonk; ele não recebeu a habilidade de invocar.

## Referências consultadas

As referências orientaram a separação entre preparação/impacto, vínculo por UUID, acompanhamento do dono e ciclo de vida. A implementação usa a API da versão local e código original.

- Ars Nouveau: [AnimatedAttackGoal](https://github.com/baileyholl/Ars-Nouveau/blob/1.20/src/main/java/com/hollingsworth/arsnouveau/common/entity/goal/AnimatedAttackGoal.java), [FollowSummonerGoal](https://github.com/baileyholl/Ars-Nouveau/blob/1.20/src/main/java/com/hollingsworth/arsnouveau/common/entity/goal/FollowSummonerGoal.java), [IFollowingSummon](https://github.com/baileyholl/Ars-Nouveau/blob/1.20/src/main/java/com/hollingsworth/arsnouveau/common/entity/IFollowingSummon.java), [SummonSkeleton](https://github.com/baileyholl/Ars-Nouveau/blob/1.20/src/main/java/com/hollingsworth/arsnouveau/common/entity/SummonSkeleton.java), [SummonWolf](https://github.com/baileyholl/Ars-Nouveau/blob/1.20/src/main/java/com/hollingsworth/arsnouveau/common/entity/SummonWolf.java).
- Iron's Spells: [SummonVexSpell](https://github.com/iron431/irons-spells-n-spellbooks/blob/1.20.1-legacy/src/main/java/io/redspace/ironsspellbooks/spells/evocation/SummonVexSpell.java), [SummonHorseSpell](https://github.com/iron431/irons-spells-n-spellbooks/blob/1.20.1-legacy/src/main/java/io/redspace/ironsspellbooks/spells/evocation/SummonHorseSpell.java).
- GeckoLib: [animações disparadas por eventos](https://github.com/bernie-g/geckolib/wiki/Triggerable-Animations-(Geckolib4)). A biblioteca foi avaliada; os modelos nativos atendem esta sprint.
- Forge: [dados sincronizados de entidades](https://docs.minecraftforge.net/en/1.20.1/networking/entities/) e [configuração](https://docs.minecraftforge.net/en/1.20.1/misc/config/).
