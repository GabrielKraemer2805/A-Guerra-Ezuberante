# Contexto do projeto — Guerra Ezuberante

Atualizado em 24/10/2026. Documento curto para retomar o trabalho. A documentação principal agora é o [README.md](README.md), com a ideia completa, tecnologias, diagramas e plano de construção. A consolidação inicial permanece em [docs/IDEIA_GERAL.md](docs/IDEIA_GERAL.md).

## Pedido atual do autor

- O nome é **Guerra Ezuberante**, com **z**. “Guerra Política” é o nome antigo do material recebido.
- Nesta etapa, apenas receber, organizar e salvar a ideia em Markdown.
- Em seguida, o autor pediu para reunir toda a documentação no README publicado no GitHub, com badges das tecnologias, explicações de cada escolha e vários diagramas. A publicação da documentação está autorizada; o código Java continua para depois.
- A implementação Java e os testes no Minecraft ficam para a próxima etapa.
- Sprites, modelos, texturas e animações de personagens serão criados depois, com o autor.
- A intenção seguinte é construir a base e um personagem inicial de teste. O documento sugere Renan Santos; a escolha final ainda pode mudar.
- O material anexado contém propostas e um prompt para implementação futura. Esses trechos são referência de planejamento, não uma ordem para implementar agora.

## Ideia em poucas linhas

Mod de Minecraft Java Edition, com foco multiplayer, combate político inteiramente satírico e personagens caricatos inspirados em figuras públicas. Terá habilidades absurdas, invocações, exércitos, alianças, músicas, transformações e batalhas entre facções.

Separar três conceitos: **facção**, **classe de combate** e **personagem/entidade**. Um jogador pode ser Invocador da Missão e usar poderes ligados ao Renan; o Renan também pode existir como mob controlado por IA.

## Conteúdo planejado

| Facção | Núcleo e identidade |
| --- | --- |
| Esquerda Lulista | Lula, Janones, Boulos, burros zumbificados, artistas, feijão e picanha |
| Direita Bolsonarista | Jair, Flávio, Eduardo, Nikolas, Trump e Netanyahu; dinheiro, dança e bombardeios fictícios |
| Direita Missão | Renan, Arthur, Glenda, loiras ucranianas e nerds montados em onças |
| Esquerda Independente | Resistência em grupo, recuperação e controle |
| Direita Independente | Dano individual, velocidade e precisão |
| Centro | Alternância entre defesa, mobilidade e suporte |

Classes propostas: Combatente, Tanque, Invocador, Especialista e Suporte.

Modos propostos: laboratório criativo com spawn eggs, prioritário para desenvolvimento; guerra multiplayer com escolha de facção, habilidades e persistência.

## Caminho de desenvolvimento proposto

1. Base técnica e entidade piloto com ovo de invocação e aparência de teste.
2. IA e combate básico.
3. Primeiro poder com projéteis e cooldown.
4. Lula e primeira batalha automática contra Renan.
5. Seis facções, interface, persistência e poderes genéricos.
6. Expandir Missão, exército de Lula e clã Bolsonaro.
7. Aliados internacionais, integração, balanceamento e alpha.

As versões v0.001 a v0.010 e v0.1.0 são marcos futuros; nenhuma delas foi implementada.

## Base técnica proposta, ainda sem configuração

- Java + NeoForge + Gradle; versões ainda indefinidas.
- Blockbench para modelos; GeckoLib opcional, se necessário.
- JSON para dados configuráveis de personagens e habilidades.
- Servidor valida habilidades, dano e invocações; clientes exibem animações, sons, partículas e interfaces.
- Habilidades reutilizáveis com dano, alcance, duração, recarga e limites de tropas configuráveis.
- Invocações limitadas em duração e quantidade; explosões sem destruição permanente de blocos por padrão.
- Distorção temporal afeta entidades e aparência, sem alterar o relógio global do servidor.
- Evitar buscas de todos os mobs a cada tick; gerenciar sobreposição de áudio.

## Decisões pendentes para a etapa de implementação

- Versão do Minecraft e versões compatíveis de Java, NeoForge e Gradle.
- Confirmação do personagem piloto e escolha da aparência provisória para o teste.
- Identificador técnico do mod e pacote Java; `guerra_ezuberante` e `br.com.guerraezuberante` são apenas propostas no documento completo.
- Formato dos recursos visuais, necessidade de GeckoLib, valores de balanceamento e regras detalhadas de alianças/progressão.

## Estado salvo

- Repositório: https://github.com/OmarFLK/A-Guerra-Ezuberante
- Branch inicial: `main`.
- O primeiro commit enviado ao GitHub foi `088ceda`, com README contendo “oi”.
- A documentação desta etapa é versionada na `main`, incluindo README detalhado com badges e 13 diagramas Mermaid, contexto, design e referências. Isso não é uma implementação do mod nem uma release jogável.
- Referência completa: [ideia geral](docs/IDEIA_GERAL.md).
- Fontes originais e três imagens: [docs/referencias](docs/referencias/).
