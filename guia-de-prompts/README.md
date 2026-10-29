# Guia de prompts — Guerra Ezuberante

Este guia ajuda a transformar uma ideia em uma mudança que possa ser implementada, testada e revisada. Você pode escrever de forma informal: o que faz diferença é explicar o resultado esperado, os limites e como conferir se ficou certo.

**Para entregar o contexto completo a outra IA:** envie o arquivo único [SKILL.md — manual completo do mod](guerra-ezuberante/SKILL.md). Ele reúne regras de negócio, arquitetura, personagens, poderes, testes, skins, áudio, vilas, fluxo de contribuição e as 118 configurações da versão0.014.2. Inclui uma mensagem pronta para acompanhar o anexo e pode ser usado como skill em ferramentas compatíveis.

| Quero… | Onde começar |
| --- | --- |
| Dar todo o contexto do mod à IA de um colaborador | [Manual completo em um único SKILL.md](guerra-ezuberante/SKILL.md) |
| Pedir um personagem, habilidade, correção ou arte | [Modelos de prompts para copiar](MODELOS.md) |
| Instalar e jogar o mod | [Requisitos e instalação](REQUISITOS.md#para-jogar) |
| Preparar outro computador para desenvolver | [Ambiente de desenvolvimento](REQUISITOS.md#para-desenvolver) |
| Entender onde modificar o projeto e como validar | [Como modificar o mod](COMO-MODIFICAR.md) |
| Conhecer o jogo e as mecânicas atuais | [README do projeto](../README.md) |
| Retomar o trabalho com contexto técnico | [CONTEXTO.md](../CONTEXTO.md) e [AGENTS.md](../AGENTS.md) |

## O que colocar em um bom pedido

1. **Objetivo:** o que o jogador deve ver ou conseguir fazer depois da mudança.
2. **Contexto:** personagem, versão, habilidade ou tela envolvidos; se é ideia futura ou implementação agora.
3. **Comportamento:** o que inicia a ação, etapas, condições, alvo, recarga e término.
4. **Números e unidades:** vida em HP, distância em blocos, duração em segundos ou ticks. No ritmo nominal, 20 ticks de servidor equivalem a 1 segundo; lag pode alongar o tempo real.
5. **Regras:** aliados, destruição de blocos, multiplayer, limites de invocação e comportamento ao salvar/reabrir.
6. **Assets:** caminho da skin, sprite, áudio ou referência; diga quais arquivos são recursos finais e quais são só prévias.
7. **Preservação:** mecânicas, mundos, recursos ou versões que precisam continuar funcionando.
8. **Aceite e entrega:** exemplos observáveis do que conta como pronto; se deve abrir o jogo e se o pedido inclui commit/push.

Não precisa definir tudo. Para valores em aberto, escreva: “Escolha um padrão coerente, documente a escolha e me diga o que adotou”. Para uma decisão que você quer tomar pessoalmente, diga isso no pedido.

## Estrutura curta para qualquer mudança

```text
Projeto: Guerra Ezuberante. Leia AGENTS.md e a seção atual de CONTEXTO.md.

Quero implementar: [objetivo concreto].
Hoje acontece: [comportamento atual, se houver].
Depois deve acontecer: [comportamento desejado].

Regras e valores:
- [gatilho, alvo, dano/vida, alcance, recarga, limites]
- [aliados, terreno, multiplayer e persistência relevantes]

Arquivos de entrada: [caminhos e função de cada arquivo].
Preservar: [o que não deve mudar].
Pode decidir por conta própria: [detalhes sem preferência].

Considero pronto quando: [2 a 5 resultados verificáveis].
Entrega: [implementar/testar/documentar; abrir jogo; publicar ou manter local].
```

## Exemplo do próprio projeto

“Coloca a vida no nível do Wither” comunica a intenção. Este complemento torna a implementação mais precisa:

```text
Lula e Jair devem ter 300 HP por padrão, como o Wither Java 1.20.1.
Mostrar barras nativas: Lula em vermelho e Jair Bolsonaro em verde.
Atualizar a barra ao receber dano/curar e removê-la ao morrer ou sair do alcance.
Aplicar também aos personagens antigos de 30 HP mantendo a porcentagem de vida.
Preservar valores personalizados diferentes nas configurações do servidor.
Não mudar os ataques nem a morte especial nesta tarefa.

Aceite: novos personagens com 300 HP; dano de 60 deixa a barra em 80%;
personagem antigo com 15/30 passa para 150/300; salvar/reabrir não migra duas vezes.
```

## Como pedir combate sem deixar casos importantes em aberto

| Pergunta | Exemplo de resposta |
| --- | --- |
| Quem pode iniciar a luta? | Neutro até sofrer uma agressão válida |
| Quando usa o poder? | Sempre que houver alvo visível, alcance e recarga disponível |
| Como alterna ataques? | A habilidade pode interromper o corpo a corpo ao ficar pronta |
| Quantas vezes causa dano? | Uma vez por inimigo por ativação, mesmo com vários segmentos |
| Quem fica protegido? | Dono, aliados, Creative/Spectator e espectadores sem relação com a luta |
| O que acontece com blocos? | Respeitar opção de terreno, mobGriefing e hooks de proteção existentes |
| O que acontece ao salvar? | Salvar a recarga sem duplicar efeitos ou invocações |
| Como funciona online? | Servidor calcula dano/IA; clientes recebem estado e efeitos |

## Como enviar feedback útil

Descreva **o que fez → o que aconteceu → o que esperava**. Acrescente frequência, distância aproximada, mundo/perfil e versões dos mods quando forem relevantes. Para erro ou crash, envie o trecho do log com a causa; para uma aparência errada, uma captura e o ângulo ajudam mais que “ficou estranho”.

Exemplo: “Flávio usa a Rachadinha quando começa a luta, depois só dá socos. Esperei 30 segundos com o inimigo vivo ao lado dele. Quero que repita a habilidade a cada 6 segundos enquanto o alvo continuar válido”.

## Contexto, referência e instrução

- **Só contexto:** “Guarde esta ideia nos documentos, sem implementar Java agora”.
- **Implementação:** “Implemente somente a primeira habilidade descrita neste arquivo”.
- **Referência visual:** “Use esta prévia para entender o personagem; a textura final é o PNG 64×64 separado”.
- **Referência externa:** “Estude o funcionamento, confirme a licença e adapte somente o necessário; mantenha dependências e procedência documentadas”.

Um anexo pode conter um plano grande e instruções antigas. Diga qual parte está autorizando agora. O guia registra a versão **0.014.2**; nas próximas sprints, as versões efetivas continuam em [gradle.properties](../gradle.properties), as dependências em [build.gradle](../build.gradle) e o estado técnico em [CONTEXTO.md](../CONTEXTO.md).

## Preferências deste projeto

Os testes automatizados usam perfis isolados com som mestre zero **antes** de abrir o cliente. Não alterar o volume do Windows nem o perfil normal do autor. Preservar os mundos. Ao entregar uma nova versão de código, abrir o Minecraft normal atualizado e deixá-lo disponível para teste, confirmando o carregamento. Não encerrar à força um cliente com mundo aberto.

Pedidos de publicação podem ser diretos: “Faça commit e push de todas as alterações atuais, incluindo a documentação”. Quando o trabalho deve ficar local, escreva isso. O [modelo de publicação](MODELOS.md#publicar-no-github) permite especificar branch, conteúdo e mensagem.
