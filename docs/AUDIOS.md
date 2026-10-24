# Áudios — inventário da sprint 0.002.0

Originais preservados em `audios/`; cópias de entrada em `assets-entrada/audio/renan/` e `assets-entrada/audio/arthur/`. Os OGGs usam Vorbis mono a 44.100 Hz, sem cortes, alteração de velocidade ou de duração.

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

## Conversão

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/convert_audio.ps1 -Character renan
powershell -NoProfile -ExecutionPolicy Bypass -File tools/convert_audio.ps1 -Character arthur
```

O script aceita `-InputDirectory` e `-FFmpegPath`. O parâmetro `-Character` escolhe a subpasta de entrada e de saída. Os MP3s soltos da primeira versão continuam preservados, mas a entrada padrão agora é separada por personagem.

Para registrar outra fala, atualize `ModSounds.java`, `sounds.json`, as duas traduções e a lista de interação/reação desejada. A recarga precisa exceder a duração integral da gravação. A lista de reação evita repetir a última opção quando houver mais de uma opção disponível.
