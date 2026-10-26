# Dependências externas e créditos

O JAR do Guerra Ezuberante contém o código e os recursos próprios do projeto. GeckoLib é obrigatório para os personagens; TaCZ é opcional desde a 0.006.0. Seus JARs permanecem separados.

A pistola experimental GE-P12 usa implementação, geometria, paleta, transformações e sons sintetizados próprios. Não reutiliza código ou assets do TaCZ nem adiciona bibliotecas novas. `tools/GeneratePistolAssets.java` registra o processo determinístico de geração.

## TaCZ — Timeless and Classics Zero

- Autoria informada pelo mod: **Serene Wave Studio | Timeless Squad**.
- Versão fixada: **1.1.8-hotfix2**, Forge 1.20.1.
- Distribuição oficial: [CurseForge, arquivo 9037989](https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero/files/9037989), publicado em 02/10/2026.
- [Projeto e código-fonte oficial](https://github.com/MCModderAnchor/TACZ/tree/1.20.1).
- Licenças declaradas pelo projeto: código **GNU GPL 3.0**; assets **CC BY-NC-ND 4.0**. Essas licenças pertencem à distribuição TaCZ e não devem ser removidas ou substituídas por uma licença do Guerra Ezuberante.
- SHA-256 do JAR original resolvido: `2cbb89f44333d2eb4e825d86d789ff119b272ff75184390b55bafd4f0e7f10c2`.

A integração utiliza chamadas às APIs públicas e IDs do gunpack padrão. Nenhum modelo 3D, textura, animação, música, som de arma ou implementação do TaCZ foi copiado para os recursos ou fontes do mod. As capturas na documentação são imagens de testes executados no Minecraft. Não foi criado um fork do TaCZ.

O gunpack padrão está incluído no JAR oficial dessa versão e é carregado pelo próprio TaCZ. A instalação validada não exige baixar outro pacote nem duplicar seus assets. O TaCZ também carrega bibliotecas internas da própria distribuição: Simple Bedrock Model, LuaJ, Commons Math, BCEL e MixinExtras. Elas permanecem no JAR original, com seus metadados.

O build prepara um conjunto **local de testes** com três JARs separados em `build/distributions/guerra-ezuberante-0.008.0/mods/` e outro com dois, sem TaCZ, em `build/distributions/guerra-ezuberante-0.008.0-sem-tacz/mods/`. São ignorados pelo Git. A distribuição oficial e os termos do TaCZ permanecem nas referências acima; seus assets não passam a ser conteúdo próprio do Guerra Ezuberante.

## GeckoLib

- Versão fixada: **GeckoLib Forge 1.20.1 — 4.8.4**.
- [Projeto oficial](https://github.com/bernie-g/geckolib).
- [Maven oficial](https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/).
- Utilizado para os personagens. As cinco armas externas usam TaCZ; a GE-P12 utiliza o renderizador de itens vanilla/Forge com transformações próprias.

Player Animator é opcional no TaCZ e não foi adicionado como requisito. GE-P12 e armas TaCZ têm caminhos independentes, restritos aos respectivos itens.
