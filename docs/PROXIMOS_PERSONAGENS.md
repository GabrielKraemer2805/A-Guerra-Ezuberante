# Próximos personagens — Lula

Lula é o próximo personagem solicitado para planejamento técnico. **Não foi implementado nesta sprint**: não existe novo registro, spawn egg, skin, som ou habilidade dele. O autor fornecerá essas decisões.

| Requisito | A definir pelo autor | Ponto existente para implementar |
| --- | --- | --- |
| Skin e modelo | Arquivo 64×64 ou outro formato aprovado, proporções | `CharacterGeoModel`, modelo humanoide articulado com UV preservado |
| Atributos | Vida, movimento, dano e alcance | Factory de atributos e listener do registro atual |
| Facção e alianças | Filiação e regras entre personagens | `isAlliedTo`, `isValidAggressor`, regras de dono já utilizadas pelas tropas |
| Comportamento | Pacificidade, gatilhos de retaliação e movimentação | `RetaliatingCharacterEntity` e goals Forge |
| Equipamentos | Itens iniciais e possibilidade de loot | Construtor/finalizeSpawn e `setItemSlot`, sem mudar itens globais |
| Habilidades | Nenhuma inventada; aguardar descrição | Goals e controlador específico do personagem, como o módulo de invocação existente |
| Animações | Poses e ações necessárias | `CharacterAnimationController`, gerador programático e arquivos GeckoLib |
| Sons | Arquivos, biblioteca e eventuais efeitos específicos | `CharacterAudio`, `MusicTrack`, `EntityMusicSession` |
| Cooldowns | Durações e condições de cancelamento | Relógio de servidor, seguindo ataque/invocação e sessão musical atuais |
| Registro e spawn egg | ID estável, cores e nome | `ModEntities`, atributos e `ModItems` |
| Interface | Informações e ações disponíveis | Painel K existente, traduções pt_br/en_us e prévia |

## Extensões realmente preparadas

- `musicTracks()` permite fornecer uma biblioteca sem duplicar o gerenciador. Combate e interação passam pelo mesmo `EntityMusicSession`; o cliente prende o áudio à entidade e limpa a instância quando ela desaparece.
- `allowsMeleeCombat()` permite manter o combate básico como fallback quando um goal específico assume a ação. Nenhum ataque paralelo precisa ser criado.
- A validação de agressor permanece compartilhada. As armas nativas são isoladas no adaptador opcional `TaczNpcWeapon` / `TaczIntegration`; tipos TaCZ não vazam para a classe base dos personagens.
- Registro de entidades, renderers, skins e eggs segue os mecanismos Forge já utilizados. Não foi criado um segundo registro nem um framework de personagens vazio.

Não há escolha de atributos, arma, poder, animação ou áudio para Lula neste documento. Os exemplos citam infraestrutura existente, não requisitos de gameplay aprovados para ele.
