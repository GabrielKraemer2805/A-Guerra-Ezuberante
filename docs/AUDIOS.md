# Áudios — funcionamento atual 0.014.2 e inventário histórico

**Atual:** um acionamento toca uma gravação completa e termina em silêncio. Botão direito inicia ou pula para outra faixa, com debounce de0,5s. Combate inicia uma gravação por encontro, sem playlist infinita. Todos os áudios existentes de Renan, Arthur e Lula continuam nas bibliotecas compartilhadas. Morte, remoção e saída do mundo encerram o som vinculado àquela entidade.

Jair usa o mesmo canal vinculado: duas falas no botão direito; Canalha apenas na reação a dano, com40% de chance, sem interromper outra fala automaticamente. O jogador pode pular inclusive Canalha com o botão direito. Os intervalos antigos das tabelas abaixo são históricos, não bloqueiam mais o clique por toda a duração da gravação. [Correção e validação](CORRECOES_0.014.2.md).

Originais preservados em `audios/`; cópias de entrada em `assets-entrada/audio/renan/`, `assets-entrada/audio/arthur/` e `assets-entrada/audio/jair/`. Os OGGs usam Vorbis mono a 44.100 Hz, sem cortes, alteração de velocidade ou de duração.

## Efeito novo: invocação

O autor substituiu o efeito por `audios/ucraniana/efeito sonoro spawn ucrania.MP3`. Duração medida: **5,903667 s**. SHA-256: `5162b84a34516322de7dd0307c20fb26cf328d3eb23413c9ff01a2ce3e47f354`. O hash difere do MP3 anterior; a troca foi confirmada pelo conteúdo, além do nome e da data.

A cópia normalizada fica em `assets-entrada/audio/arthur/invocacao.mp3`; o recurso atualizado é `sounds/arthur/invocacao.ogg`. Evento `guerra_ezuberante:arthur.invocacao`, com streaming e legenda. Somente Arthur Invocador e o ID legado disparam o efeito uma vez no início do cast; as cinco combatentes não repetem o mesmo som. Uma trava de **120 ticks** evita sobreposição caso o intervalo configurado fique menor que a duração do arquivo. A conversão preservou a gravação inteira, em Vorbis mono 44.100 Hz.

O efeito é separado das sete falas do botão direito e da reação É vagabundo sim. Os dez OGGs de falas/phonk anteriores mantêm seus bytes; somente invocacao.ogg foi substituído. O mod empacota **11 OGGs**. O original novo na pasta audios permanece intacto. [Mecânica e limites da validação](GECKOLIB_0.004.md).

## Renan

| Original | Duração aproximada | OGG em `sounds/renan/` | Uso |
| --- | --- | --- | --- |
| me respeita normal.MP3 | 13,48 s | me_respeita.ogg | Botão direito; recarga 15 s |
| renan risadinha safada.MP3 | 2,38 s | risadinha.ogg | Botão direito; recarga 4 s |
| Me respeita, creu creu - PHONK.mp3 | 12,59 s | phonk.ogg | Entrada em combate; recarga 14 s |

O phonk foi identificado pelo nome explícito do arquivo, separado da fala normal. Não foi usado Me Respeita normal como substituto da música. O phonk usa streaming e som vinculado à entidade pelo protocolo nativo, acompanhando sua posição no cliente. Só inicia ao entrar em combate; golpes e troca de agressor durante o mesmo combate não o reiniciam.

## Arthur

| Original | Duração aproximada | OGG em `sounds/arthur/` | Recarga de interação |
| --- | --- | --- | --- |
| e_vagabundo_sim.MP3 | 9,98 s | e_vagabundo_sim.ogg | 11 s |
| ficou_ofendidinho.MP3 | 16,59 s | ficou_ofendidinho.ogg | 18 s |
| limpa o cu com a linuga.MP3 | 14,03 s | limpa_o_cu_com_a_linuga.ogg | 16 s |
| parar_de_mamat.MP3 | 13,19 s | parar_de_mamat.ogg | 15 s |
| privatizar_tudo.MP3 | 16,69 s | privatizar_tudo.ogg | 18 s |
| sao_faceis_pq_sao_pobres.MP3 | 7,89 s | sao_faceis_pq_sao_pobres.ogg | 9 s |
| so_deusa.MP3 | 9,43 s | so_deusa.ogg | 11 s |
| privatizar_tudo(1).MP3 | 13,19 s | Mesmos bytes de parar_de_mamat.MP3 | Sem cópia extra |
| privatizar_tudo(2).MP3 | 13,19 s | Mesmos bytes de parar_de_mamat.MP3 | Sem cópia extra |

**São nove arquivos originais e sete gravações únicas.** As duas duplicatas têm o mesmo SHA-256 de `parar_de_mamat.MP3`: `76f2d289a1da6ead3a22635322c55d6af632afc9ff8575d9f5f2cdfdbc01248a`. Todos os originais foram mantidos. O conversor detecta duplicatas por hash e evita empacotar o mesmo áudio várias vezes.

Os sete áudios estão registrados e disponíveis completos no botão direito. O autor escolheu “É vagabundo sim” (9,98 s) como reação automática do Arthur ao entrar em combate. A gravação inteira usa recarga de 11 s e não reinicia a cada golpe. A recarga é preservada no salvamento.

Os nomes e as legendas identificam os arquivos recebidos; não foi feita transcrição integral das gravações nem edição de conteúdo.

## Jair Bolsonaro — 0.013.1

Recebidos em `assets-entrada/audio/jair/` como `.mpeg` (MP3 estéreo 48 kHz com capa embutida; a capa é descartada). Gravações inteiras, sem cortes.

| Original | Duração | SHA-256 | OGG em `sounds/jair/` | Uso |
| --- | --- | --- | --- | --- |
| CANALHA.mpeg | 6,55 s | `53e49f88…a91f191` | canalha.ogg | **Só ao receber dano**, 40% de chance (`[jair] canalha_chance`); trava de 7 s |
| EU NAO TE.mpeg | 53,30 s | `b435f05b…d87d8` | eu_nao_te.ogg | Botão direito; recarga 55,5 s |
| POPCORN.mpeg | 42,38 s | `3ec03c00…6543a` | popcorn.ogg | Botão direito; recarga 44,5 s |

- O "Canalha" é sorteado a cada dano que tira vida do Jair, nunca por cima de outra fala dele e nunca no botão direito. A reação genérica de entrada em combate continua vazia para o Jair.
- Desde a0.014.2, o botão direito interrompe a gravação anterior e inicia outra, sem sobrepor. As durações medidas dos OGGs são131/1066/848ticks para Canalha/Eu não te/Popcorn; os cooldowns acima descrevem a implementação antiga.
- O mod passa a empacotar **3 OGGs a mais** (evento `guerra_ezuberante:jair.*`, streaming, legendas "Jair — gravação: …"). O painel K → Personagens → Jair lista as três.

## Boulos e Haddad — 0.015.4

Trechos escolhidos pelo colaborador Giovanni (Jhovial) de dois vídeos maiores, ambos MP3 estéreo 44.100 Hz. Diferente das outras falas, **aqui há corte, a pedido**; dentro do trecho não há mudança de velocidade, volume ou fade. Originais preservados em `audios/boulos/` e `audios/haddad/` (ignorados pelo Git).

| Original | Duração original | SHA-256 | Trecho | OGG | Uso |
| --- | --- | --- | --- | --- | --- |
| NOVO HIT - BOULOS E O PÓ 🤣🤧 - B006 CANAIS.mp3 | 83,98 s | `79ce30da…add14d8` | 0–7 s | `sounds/boulos/o_po.ogg`, 7,000 s = 140 ticks | Botão direito; **ao receber dano**, 40% de chance (`[boulos] po_chance`), nunca por cima dela mesma. Não toca só por entrar em combate |
| Músicas famosas são adaptadas em meme para Taxad assista - Poder360.mp3 | 105,06 s | `bb874fe2…234058d` | 34–45 s | `sounds/haddad/taxad.ogg`, 11,000 s = 220 ticks | **Só botão direito** (a única faixa de interação do Haddad) |
| Após provocação, Haddad dá tapinhas no rosto de Pavanato - Metrópoles.mp3 | 16,77 s | `5c828251…e278e55` | 5–8 s | `sounds/haddad/tapinhas.ogg`, 3,000 s = 60 ticks | **Ao receber dano**, com vida em 20% ou mais (`[haddad] damage_voice_chance`, padrão 1 = sempre) |
| (mesmo arquivo) | — | — | 8–11 s | `sounds/haddad/perdeu_ne_seu_tonto.ogg`, 3,000 s = 60 ticks | **Ao receber dano com menos de 20% da vida** (`[haddad] low_health_voice_fraction` 0,2), no lugar de "tapinhas" |

- Os dois usam o mesmo canal vinculado de Lula/Jair (`CharacterAudio.BOULOS`/`HADDAD`): clique pula/reinicia, fim natural fica em silêncio, morte e remoção param o som. Com uma única faixa de interação, o clique reinicia a mesma gravação.
- Falas de dano (o pó, tapinhas, perdeu né seu tonto) seguem a regra do Canalha: só quando o dano tira vida, nunca por cima de outra fala do mesmo personagem; o clique pode interrompê-las. A faixa de vida é conferida depois do dano.
- Volume medido (média/pico): o pó −20,3/−4,3 dB, Taxad −22,9/−3,7 dB, tapinhas −12,6/−1,9 dB, perdeu né seu tonto −14,7/−2,0 dB. o pó e Taxad ficam mais baixos que Lula/Jair (−11 a −18 dB de média). Mantidos como recebidos.
- Eventos `guerra_ezuberante:boulos.o_po` e `guerra_ezuberante:haddad.{taxad,tapinhas,perdeu_ne_seu_tonto}`, streaming, atenuação 32 blocos, legendas "Boulos — gravação: o pó" e "Haddad — gravação: …". O painel K → Personagens → Haddad/Boulos lista cada uma.

O corte gera um WAV PCM sem perdas na pasta de entrada, e o conversor do projeto faz o OGG mono:

```powershell
ffmpeg -i "audios/boulos/NOVO HIT - BOULOS E O PÓ 🤣🤧 - B006 CANAIS.mp3" -ss 0 -to 7 -vn -map_metadata -1 -c:a pcm_s16le assets-entrada/audio/boulos/o_po.wav
ffmpeg -i "audios/haddad/Músicas famosas são adaptadas em meme para Taxad assista - Poder360.mp3" -ss 34 -to 45 -vn -map_metadata -1 -c:a pcm_s16le assets-entrada/audio/haddad/taxad.wav
ffmpeg -i "audios/haddad/Após provocação, Haddad dá tapinhas no rosto de Pavanato - Metrópoles.mp3" -ss 5 -to 8 -vn -map_metadata -1 -c:a pcm_s16le assets-entrada/audio/haddad/tapinhas.wav
ffmpeg -i "audios/haddad/Após provocação, Haddad dá tapinhas no rosto de Pavanato - Metrópoles.mp3" -ss 8 -to 11 -vn -map_metadata -1 -c:a pcm_s16le assets-entrada/audio/haddad/perdeu_ne_seu_tonto.wav
```

## Conversão

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/convert_audio.ps1 -Character renan
powershell -NoProfile -ExecutionPolicy Bypass -File tools/convert_audio.ps1 -Character arthur
powershell -NoProfile -ExecutionPolicy Bypass -File tools/convert_audio.ps1 -Character boulos
powershell -NoProfile -ExecutionPolicy Bypass -File tools/convert_audio.ps1 -Character haddad
```

O script aceita `-InputDirectory` e `-FFmpegPath`. O parâmetro `-Character` escolhe a subpasta de entrada e de saída. Os MP3s soltos da primeira versão continuam preservados, mas a entrada padrão agora é separada por personagem.

Para registrar outra fala, atualize `ModSounds.java`, `sounds.json`, as duas traduções e a lista de interação/reação desejada. A recarga precisa exceder a duração integral da gravação. A lista de reação evita repetir a última opção quando houver mais de uma opção disponível.
