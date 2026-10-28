# Modelos de prompts

[Voltar ao guia](README.md) · [Requisitos](REQUISITOS.md) · [Como modificar](COMO-MODIFICAR.md)

Substitua os campos entre colchetes e apague os itens que não se aplicam. Números dos exemplos são sugestões para o pedido, não alterações automáticas do balanceamento.

## Planejar sem implementar

```text
Vou enviar uma ideia para o Guerra Ezuberante SOMENTE COMO CONTEXTO.
Leia AGENTS.md, CONTEXTO.md e os documentos relacionados.

Ideia: [descrição ou caminho do anexo].
Organize em Markdown: visão geral, regras, personagens, habilidades,
diagramas úteis, dúvidas e uma primeira etapa pequena de implementação.
Separe o que eu defini de sugestões suas e do que ainda está em aberto.
Atualize os links necessários. Não implemente Java nem altere o jogo nesta etapa.
Publicação: [manter local / commit e push].
```

## Criar um personagem

```text
Implemente [nome] no Guerra Ezuberante usando a arquitetura existente.
Leia AGENTS.md, CONTEXTO.md e entidades semelhantes antes de alterar arquivos.

Identidade:
- Nome exibido: [nome]. ID proposto: [id estável].
- Facção e aliados: [quais]. Agressão: [neutro/retaliação/outra regra].
- Vida: [HP]. Movimento: [valor ou escolher e documentar].
- Skin final: [PNG 64×64]. Prévia: [arquivo apenas para referência].

Combate:
- Ataque básico: [arma, dano e ritmo].
- Primeira habilidade: [gatilho, preparação, impacto e recuperação].
- Dano: [HP e limite de acertos por ativação]. Alcance: [blocos].
- Recarga: [segundos/ticks e quando começa a contar].
- Invocações: [quantidade, limite por dono, duração e limpeza, se houver].
- Terreno/aliados: [regras].
- Salvamento e multiplayer: [o que deve persistir/sincronizar].

Recursos: use [pasta de áudios]; indique a função de cada gravação.
Não invente gravações ausentes. Documente os recursos que faltarem.
Inclua ovo, aba criativa, painel K e traduções pt_br/en_us quando aplicável.
Reutilize os sistemas existentes e preserve os demais personagens.

Aceite:
1. [Como obter e ver o personagem no jogo].
2. [Como provocar e observar o ataque].
3. [Como conferir limites, aliados e persistência].

Valide build e testes relevantes, documente e abra o cliente normal atualizado.
Testes automatizados isolados com som zero; preserve mundos e volume normal.
Publicação: [manter local / commit e push].
```

## Criar ou corrigir uma habilidade

```text
Personagem: [nome]. Habilidade: [nome].
Objetivo: [resultado que o jogador deve perceber].

Gatilho: [o que inicia]. Condições: [alvo/alcance/linha de visão].
Fases: preparação [ticks], impacto no tick [n], recuperação [ticks].
Efeito: [projétil/área/invocação/efeito visual/terreno].
Dano: [HP]. Acertos: [um por alvo/ativação ou outra regra explícita].
Recarga: [ticks], contada a partir de [momento].
Repetição: [usar novamente durante a mesma luta quando a recarga terminar].
Interação com ataque básico: [prioridade e interrupções].
Aliados e espectadores: [proteções]. Terreno: [regras e opções].
Cancelamento: [morte, alvo inválido, falta de chão ou descarregamento].
Persistência: [recarga/vínculo/estado que precisa sobreviver ao save].

Preservar: [mecânicas existentes]. Recursos: [arquivos].
Aceite: [sequência observável, inclusive segunda/terceira ativação].
Teste com a IA normal além de ativações diretas, se alterar decisões de combate.
Não amplie o escopo para outras habilidades.
Entrega/publicação: [definir].
```

## Ajustar balanceamento

```text
Altere somente estes padrões:
- [personagem.parâmetro]: [valor antigo] → [valor novo e unidade].
- [habilidade.parâmetro]: [valor antigo] → [valor novo e unidade].

Aplicar em mundos existentes? [sim/não].
Para personagens já salvos: [preservar porcentagem de vida/vida absoluta/outra regra].
Para configurações personalizadas: [preservar valores diferentes dos padrões antigos].
Mantenha os outros atributos, recursos e mecânicas.
Atualize painel, documentação e migração quando necessários.
Aceite: [contas e resultados concretos].
Valide o comportamento alterado e abra o Minecraft normal se gerar nova versão.
Publicação: [manter local / commit e push].
```

## Relatar um bug

```text
Versão do Guerra Ezuberante: [versão].
Minecraft/Forge/GeckoLib/TaCZ: [versões ou pedir para conferir o ambiente].
Ambiente: [cliente normal/servidor/mundo isolado; outros mods relevantes].

Passos para reproduzir:
1. [ação].
2. [ação].
3. [ação].

Resultado atual: [o que ocorreu].
Resultado esperado: [como deveria funcionar].
Frequência: [sempre/às vezes; após quanto tempo].
Anexos: [log, crash report, captura, vídeo, caminhos].
Última situação que funcionava: [se souber].

Investigue a causa, corrija e reproduza novamente a sequência.
Preserve meus mundos; use uma cópia ou perfil isolado para reproduzir.
Mostre o que foi confirmado, o que mudou e os limites da validação.
Publicação: [manter local / commit e push].
```

## Corrigir skin ou animação

```text
Personagem: [nome]. Arquivo final: [caminho].
Problema: [parte, lado/ângulo, estado ou momento da animação].
Quero: [aparência/movimento esperado]. Referências: [arquivos].

Autorizo alterar: [regiões UV, ossos, clipes ou textura inteira].
Preservar: [rosto, roupa, faixa, proporções, resolução, regiões específicas].
Mantenha a fonte original e documente a origem do recurso novo.
Verifique o resultado no modelo do jogo; uma prévia plana não basta para UV.
Não altere dano, IA ou áudio nesta tarefa.
Se houver gerador/validador afetado, mantenha-o compatível com o recurso final.
Entrega: [capturas/abrir cliente normal]. Publicação: [definir].
```

## Adicionar áudios

```text
Personagem: [nome]. Pasta de entrada: [caminho].
Uso de cada arquivo:
- Interação com botão direito: [arquivos].
- Playlist de combate: [arquivos; indicar se inclui falas e músicas].
- Reação ao dano: [arquivo inteiro ou trecho autorizado].
- Habilidade/invocação/morte: [arquivo e momento do disparo].

Pode cortar os arquivos? [não, usar inteiros / sim, intervalos exatos].
Defina pausas/recargas coerentes para evitar sobreposição e repetição excessiva.
Use os canais de som e sistemas espaciais existentes; inclua legendas/traduções.
Converta para o formato do jogo preservando as fontes e a procedência.
Testes automatizados sem som; deixe o volume normal para meu teste de audição.
Aceite: [evento, quantidade de instâncias e comportamento ao encerrar combate].
Publicação: [definir].
```

## Documentar uma versão

```text
Atualize a documentação do Guerra Ezuberante para refletir o código atual.
Confira gradle.properties, build.gradle, configurações e relatórios existentes.
Inclua comportamento, requisitos, instalação, teste manual, limitações,
exemplos de configuração e diagramas que ajudem a entender os sistemas.
Mantenha histórico com a versão correspondente e destaque o estado atual.
Não declare teste aprovado sem evidência nem apresente planos como implementados.
Crie links no README principal para os documentos novos.
Escopo: somente documentação. Publicação: [manter local / commit e push].
```

## Publicar no GitHub

```text
Faça commit e push de todas as alterações atuais do Guerra Ezuberante,
incluindo código, recursos do mod, documentação e guias.
Destino: origin/main no repositório OmarFLK/A-Guerra-Ezuberante.
Mensagem sugerida: [mensagem, ou peça para escolher uma fiel às mudanças].

Revise o diff e o estado remoto, respeite o .gitignore, confira os links novos
e use os resultados de validação já feitos quando o código não tiver mudado.
Se houver mudança nova de código, rode as verificações pertinentes.
Não inclua mundos, caches, credenciais ou ferramentas locais.
Não use force push. Ao terminar, confirme o commit remoto e a sincronização.
```

## Retomar uma sessão ou continuar em outro computador

```text
Estamos continuando o Guerra Ezuberante neste checkout.
Leia AGENTS.md, a seção mais recente de CONTEXTO.md, README.md e git status.
Confira versões reais e alterações locais antes de mexer nos arquivos.

Objetivo desta sessão: [uma entrega concreta].
Decisões já tomadas: [informações que faltarem nos documentos].
Arquivos de entrada: [caminhos novos deste computador].
Preservar: [mundos, trabalho não publicado, mecânicas].
Prossiga com o que já estiver definido e pergunte apenas por informação
essencial que não estiver no projeto ou no meu pedido.
Entrega e publicação: [definir].
```
