# Áudios de entrada

Originais são preservados e ignorados pelo Git. As entradas padrão são separadas por personagem:

- `renan/`: Me Respeita, risadinha e phonk.
- `arthur/`: nove MP3s recebidos, sete gravações únicas.

```powershell
powershell -ExecutionPolicy Bypass -File tools/convert_audio.ps1 -Character renan
powershell -ExecutionPolicy Bypass -File tools/convert_audio.ps1 -Character arthur
```

O conversor preserva as gravações completas e detecta duplicatas por SHA-256. Não há cortes nem mudança de velocidade. Os MP3s soltos da primeira versão continuam preservados; o padrão atual usa as subpastas.

Novas falas também precisam de registro Java, sounds.json e legendas. O autor escolheu “É vagabundo sim” como reação automática de Arthur, inteira, com recarga de 11 s. Os sete áudios completos continuam no botão direito. Inventário e durações: [docs/AUDIOS.md](../../docs/AUDIOS.md).
