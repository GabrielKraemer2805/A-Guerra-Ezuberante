# Requisitos para jogar e desenvolver

[Voltar ao guia](README.md) · [Prompts prontos](MODELOS.md) · [Como modificar](COMO-MODIFICAR.md)

Referência deste documento: **Guerra Ezuberante 0.011.0**. As versões vêm dos arquivos do próprio repositório; não substitua uma dependência pela versão mais nova sem conferir compatibilidade.

## Para jogar

| Componente | Versão usada | Função |
| --- | --- | --- |
| Minecraft **Java Edition** | **1.20.1** | Jogo base; este projeto não é um addon de Bedrock |
| Java de 64 bits | **17** | Runtime usado nesta versão; o launcher deve iniciar o perfil com Java compatível |
| Forge | **47.4.28**, versão validada | Carrega o mod e os eventos de gameplay |
| Guerra Ezuberante | **0.011.0** | Personagens, habilidades, painel, menu e GE-P12 |
| GeckoLib **para Forge 1.20.1** | **4.8.4**, obrigatório | Modelos e animações dos personagens |
| TaCZ **para Forge 1.20.1** | **1.1.8-hotfix2**, opcional | Arsenal externo e armas nativas de Renan/Jair |

A configuração atual aceita Forge a partir de 47.4.28 dentro da linha 47; a versão efetivamente usada nos testes foi 47.4.28. GeckoLib e TaCZ, quando presente, estão fixados nas versões acima em [mods.toml](../src/main/resources/META-INF/mods.toml). Não misture arquivos de Fabric/NeoForge com este conjunto Forge.

Sem TaCZ, a **GE-P12 própria continua funcionando** e Renan/Jair usam o comportamento de reserva desarmado. GeckoLib continua obrigatório. Com TaCZ, ele aparece como outro mod na lista: os três JARs são dependências separadas, não um único arquivo mesclado.

### Instalar no launcher

1. Prepare um perfil Minecraft Java 1.20.1 com Forge 47.4.28 e execute-o uma vez.
2. Feche esse perfil antes de trocar os JARs.
3. Na pasta `mods/` **desse perfil**, coloque o Guerra Ezuberante, GeckoLib e, se quiser o arsenal integrado, TaCZ nas versões acima.
4. Remova o JAR antigo do Guerra Ezuberante dessa pasta para não carregar duas versões do mesmo mod. Guarde os mundos antes de alterar um conjunto de mods.
5. Inicie o perfil e confira o carregamento na lista de mods. No jogo, use a aba criativa do mod ou o painel **K** para conferir os personagens.

O menu ilustrado é parte do mod. Os testes atuais, as mecânicas e os recursos estão descritos no [README principal](../README.md) e na [entrega 0.011.0](../docs/BOSSES_0.011.md).

### Onde obter os arquivos deste projeto

O repositório publica o **código, recursos e documentação**. `build/` é ignorada pelo Git: dar push do código não cria automaticamente um download de JAR ou uma GitHub Release.

Depois de compilar, use um dos conjuntos gerados:

```text
build/libs/guerra_ezuberante-0.011.0.jar

build/distributions/guerra-ezuberante-0.011.0/mods/
  Guerra Ezuberante + GeckoLib + TaCZ, em JARs separados

build/distributions/guerra-ezuberante-0.011.0-sem-tacz/mods/
  Guerra Ezuberante + GeckoLib
```

Copie o conteúdo de **um** dos diretórios `mods/` para seu perfil. Para jogar, não use os JARs de cache com nomes `mapped_official` do ambiente de desenvolvimento. O build prepara as dependências originais para distribuição, com [avisos de terceiros](../docs/DEPENDENCIAS_TERCEROS.md).

### Multiplayer e servidor

Cliente e servidor devem usar o mesmo conjunto de versões e dependências de gameplay. Use Java 17 e Forge 1.20.1 no servidor e instale os JARs de produção em `mods/` também no servidor. Um servidor dedicado não precisa dos renderizadores locais, mas este mod e GeckoLib têm dependências declaradas para ambos os lados.

A configuração SERVER fica dentro do diretório do mundo:

```text
<mundo>/serverconfig/guerra_ezuberante-server.toml
```

Em desenvolvimento, um mundo normal costuma estar em `run/saves/<mundo>/`; no dedicado, veja o diretório de mundo configurado no servidor. Feche/salve o mundo antes de editar o arquivo. Confira a [configuração dos bosses](../docs/BOSSES_0.011.md#mundos-e-configurações-existentes) e os exemplos no README.

Aceitar a EULA é uma decisão de quem opera o servidor. O aceite dado pelo autor ao servidor local deste workspace não configura automaticamente servidores de outra pessoa ou de outro computador. [EULA oficial do Minecraft](https://www.minecraft.net/en-us/eula).

## Para desenvolver

O fluxo documentado e validado neste repositório usa **Windows e PowerShell**. Vários validadores chamam `powershell` e algumas ferramentas usam APIs do Windows; a portabilidade completa para Linux/macOS ainda não foi validada.

| Ferramenta | Obrigatória? | Observação |
| --- | --- | --- |
| **JDK 17 de 64 bits** | Sim | Precisa incluir `java` e `javac`, não apenas um runtime |
| **Git** | Para clonar/publicar | Credenciais são necessárias para enviar alterações ao GitHub |
| **Windows PowerShell** | No fluxo atual | Executa wrappers, preparo e validadores |
| **Gradle Wrapper** | Já incluído | `gradlew.bat` baixa e executa o Gradle **8.8**; não exige instalar Gradle global |
| **ForgeGradle** | Resolvido pelo build | **6.0.54**, já configurado no projeto |
| Internet | No primeiro preparo/novas dependências | Downloads do Gradle, Forge, mappings, GeckoLib e TaCZ |
| Editor ou IDE com Java | Opcional | O terminal basta para compilar; o editor facilita navegação e depuração |
| FFmpeg com Vorbis | Para converter áudio | Não é necessário para um build normal com os OGGs já versionados |

A exigência de JDK 17/VM de 64 bits para esta linha está na [documentação oficial do Forge 1.20.1](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/). As animações usam a API da linha [GeckoLib 4](https://wiki.geckolib.com/docs/geckolib4/); exemplos da GeckoLib 5 não são intercambiáveis com ela.

### Primeiro clone e build

No PowerShell:

```powershell
git clone https://github.com/OmarFLK/A-Guerra-Ezuberante.git
cd A-Guerra-Ezuberante
```

Se seu JDK 17 não estiver definido em `JAVA_HOME`, ajuste o caminho abaixo para a pasta real do JDK, **sem acrescentar `bin`**:

```powershell
$env:JAVA_HOME = 'C:\CAMINHO\DO\JDK17'
& "$env:JAVA_HOME\bin\java.exe" --version
& "$env:JAVA_HOME\bin\javac.exe" --version
```

Os dois comandos devem mostrar Java 17. Essa atribuição vale para a sessão atual do terminal. O wrapper também reconhece um JDK portátil em `.tools/jdk17/<pasta-do-jdk>/`, se você o tiver instalado ali. O diretório `.tools/` não acompanha o clone.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/gradle.ps1 build --console=plain
```

O wrapper usa `.gradle-user/` como cache local e verifica a versão Java. O primeiro build precisa resolver dependências e pode demorar mais. Recursos finais já estão em `src/main/resources`; não é necessário regenerar todos os modelos, converter todos os áudios nem ter as pastas pessoais do autor para compilar.

`-PwithoutTacz` retira TaCZ do runtime de desenvolvimento/teste. O projeto ainda possui integração compilada, validação e bundle opcional de TaCZ; essa opção não promete um preparo sem nenhum download de TaCZ.

### Abrir o cliente para jogar/testar manualmente

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/gradle.ps1 runClient --console=plain
```

Esse comando usa **`run/`**, o perfil normal de desenvolvimento. Ele não é o cenário automatizado e não encerra sozinho. O mod funciona nesse cliente sem copiar manualmente JARs para a instalação do launcher.

Para um cliente de desenvolvimento sem TaCZ:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/gradle.ps1 runClient -PwithoutTacz --console=plain
```

Use um perfil/mundo separado para essa comparação; não abra mundos com itens de TaCZ removendo a dependência sem antes preservar uma cópia. A preferência de som dos testes automatizados é diferente do teste manual: o perfil normal mantém o volume escolhido pelo autor.

### Testes e servidor de desenvolvimento

```powershell
# GameTests no perfil run-gametest/, fora dos mundos normais
powershell -NoProfile -ExecutionPolicy Bypass -File tools/gradle.ps1 runGameTestServer --console=plain

# Compatibilidade do runtime sem TaCZ
powershell -NoProfile -ExecutionPolicy Bypass -File tools/gradle.ps1 runGameTestServer -PwithoutTacz --console=plain

# Servidor dedicado de desenvolvimento; observe a EULA e configuração locais
powershell -NoProfile -ExecutionPolicy Bypass -File tools/gradle.ps1 runServer --console=plain
```

O servidor dedicado usa `run-server/`. A aprovação do servidor GameTest não equivale a testar conexão com um servidor dedicado ou multiplayer pela internet. Consulte o [registro de validação](../docs/VALIDACAO.md) para saber o que já foi exercitado.

O cenário gráfico mais recente usa:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/boss_rift_visual_test.ps1
```

Ele requer a cópia de mundo `run-gametest/world` gerada por uma execução concluída do GameTestServer. Feche o servidor antes de copiar esse mundo. O script prepara `.tools/run-boss-rift-client` com volume mestre **0**, copia o mundo, executa o cenário e encerra o cliente de teste. Não rode dois cenários usando o mesmo perfil ao mesmo tempo. Não use essa flag para o cliente que deve ficar aberto para o autor.

### Memória, vídeo e disco

Não há um mínimo de CPU/GPU/RAM medido especificamente para este mod. O computador precisa executar Minecraft Java 1.20.1 com Forge; a carga aumenta conforme personagens, invocações, efeitos e clientes simultâneos.

O processo Gradle está configurado com teto de heap de **3 GB** em `gradle.properties`; isso não significa que 3 GB de RAM total bastem para Windows, IDE, compilação e jogo juntos. Reserve recursos para os processos simultâneos e espaço livre para caches, dependências, mundos e capturas. Não apresente valores sugeridos de alocação como garantia de FPS ou requisito oficial.

## Problemas comuns

| Sintoma | O que conferir |
| --- | --- |
| `JAVA_HOME` inválido ou Java errado | Pasta do JDK 17 com `bin/java.exe` e `bin/javac.exe`; o wrapper pode priorizar um JDK portátil existente |
| Dependência não baixa | Acesso à internet e aos repositórios configurados em `build.gradle`; use o erro exato do Gradle para identificar qual falhou |
| Falta GeckoLib / erro `GeoEntity` | JAR Forge 1.20.1 da GeckoLib **4.8.4** no perfil de jogo e servidor |
| Mod duplicado | Duas versões do Guerra Ezuberante na mesma pasta `mods/` |
| Configuração antiga não parece mudar | Arquivo SERVER do mundo correto, valores personalizados e migração; parâmetros de uma instância já salva podem estar em NBT |
| Não consegue entrar no servidor | Conjunto/versões dos mods, Forge, log do cliente e do servidor |
| Cliente de teste está sem som | Esperado em perfis `.tools/`; o teste manual normal mantém seu próprio volume |
| Sem áudios originais em `audios/` após clonar | Fontes locais são ignoradas; OGGs usados pelo jogo estão versionados em resources |
| Não encontra JAR na página do GitHub | Clone e compile, ou obtenha uma distribuição publicada separadamente; push de fonte não cria Release |

Para pedir ajuda, use o [modelo de bug](MODELOS.md#relatar-um-bug) com comando executado, versões e erro relevante.
