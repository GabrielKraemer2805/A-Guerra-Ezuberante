# Validação — sprint 0.002.0

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
