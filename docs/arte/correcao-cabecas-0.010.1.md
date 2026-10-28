# Correção das cabeças — 0.010.1

Pedido do autor: diminuir a aparência das orelhas de Jair e Flávio e preencher o cabelo nas laterais do Jair. Edição feita com o skill **imagegen**, ferramenta integrada, seguida de extração mecânica das faces geradas e integração no atlas original.

Somente duas faces8×8 de cada PNG foram substituídas: UVx0–7/y8–15 e UVx16–23/y8–15. Comparação pixel a pixel confirmou **128 pixels alterados nas laterais e zero alterações fora delas**, para cada personagem. Rosto frontal, topo, nuca, camadas externas, roupas, faixa presidencial, braços, mãos, calças e sapatos permanecem iguais. Nenhum modelo, atributo ou mecânica foi modificado.

Os originais permanecem em `assets-entrada/skins/*_skin_64x64.png`. Fontes corrigidas: `assets-entrada/skins/jair_bolsonaro_skin_64x64_corrigida.png` e `flavio_bolsonaro_skin_64x64_corrigida.png`. Texturas utilizadas pelo mod: `src/main/resources/assets/guerra_ezuberante/textures/entity/{jair_bolsonaro,flavio_bolsonaro}.png`, ainda64×64RGBA.

As quatro faces selecionadas estão em `assets-entrada/skins/correcoes/`. `tools/prepare_bolsonaro_skin_heads.ps1` recompõe os PNGs a partir dos originais e dessas faces, sem repintar o restante. O validador passou a comparar com as fontes corrigidas autorizadas pelo autor, preservando os originais anteriores.

## Geração e preparação

Modo: ferramenta integrada de edição, fundo transparente. Saídas originais geradas preservadas na pasta gerenciada do Codex `generated_images/01a16e2e-eaa4-72a2-9bc3-9968148939ff/`. Foram selecionadas apenas as faces laterais; os atlas completos gerados não substituíram os arquivos originais.

| Personagem | Arquivo gerado | Recorte direita | Recorte esquerda |
| --- | --- | --- | --- |
| Jair | `exec-619afb93-eefa-4209-8723-206b0aac6d37.png`,1254×1254 | x21/y228/l189/a210 | x426/y228/l201/a210 |
| Flávio | `exec-4361f10d-4d47-4446-ae5f-a63f9ad50163.png`,1261×1247 | x55/y204/l156/a226 | x422/y204/l175/a226 |

Recortes reduzidos para8×8 com nearest-neighbor via FFmpeg. Composição por cópia direta dos pixels dessas faces, sem interpolar ou reamostrar o corpo original. PNGs finais salvos com alpha.

## Prompt Jair

```text
Edit the attached Minecraft Java Steve classic 64x64 RGBA skin atlas. This is a technical game texture, NOT a character render or preview. Output only the flat texture atlas. Preserve its exact 64x64 logical pixel grid and UV layout, crisp square pixels, transparent unused regions. If generating at 1024x1024, every logical texture pixel must be a uniform 16x16 square, no antialiasing, no shadows, no checkerboard baked into transparency. Very localized correction: Jair Bolsonaro's hair on BOTH SIDE faces of the head is patchy and the ears are oversized. Give him full short dark brown hair with restrained gray temples continuously covering the upper half of each side, connecting naturally to the crown and back. Small natural ears, only 1x2 logical pixels midway down each side; no huge flesh-colored patches, no bald strip, no protruding ear shapes on the outer hat layer. Head base RIGHT SIDE UV x0..7 y8..15, LEFT SIDE x16..23 y8..15; matching hat overlay sides x32..39 y8..15 and x48..55 y8..15 should be transparent, apart from tiny upper hair details if needed. Keep face, top of head, back, suit, green/yellow Brazilian sash, arms, hands, pants, shoes and ALL remaining UV pixels exactly as the input. Keep adult character identity and original pixel palette. Do not add text, labels, grid lines or extra anatomy.
```

## Prompt Flávio

```text
Edit the attached Minecraft Java classic Steve 64x64 RGBA skin TEXTURE ATLAS. Return only the flat square UV atlas, not a character render or presentation. Preserve the exact 64x64 logical pixel layout, transparent unused parts and hard pixel-art edges. If upscaled, each of the 64 logical pixels per side must be a consistent square, no antialiasing or baked checkerboard. Only correct BOTH SIDE FACES of the head: Flavio Bolsonaro's ears look much too big. Replace broad skin-colored side patches with neatly continuous short dark brown hair covering the upper half and the rear of each side. Draw restrained normal ears only 1x2 logical pixels around the middle of each side, no large protrusions or inflated ear layer. Head base side UV regions: x0..7/y8..15, x16..23/y8..15. Matching outer hat side regions x32..39/y8..15 and x48..55/y8..15 must have no opaque skin/ear protrusions; transparent sides apart from small upper hair details. Keep the front face, face expression, crown, back, blue shirt, rolled sleeves, wristwatch, hands, jeans, shoes and EVERY other original part unchanged. Original character identity and palette. No text, guides, labels, grid borders, extra limbs or redesigned costume.
```

## Validação

- PNGs64×64, fontes corrigidas e runtime iguais por SHA256.
- Comparação de todos os pixels com os originais: nenhuma mudança fora das duas laterais.
- Laterais ampliadas inspecionadas em `.tools/skin-heads/laterais-corrigidas.png`.
- Build/check/reobf0.010.1 aprovados em `.tools/skins-0.010.1-build.log`.
- Não houve alteração Java de gameplay; os GameTests da0.010.0 não foram reexecutados para esta correção visual.
- JAR: `build/libs/guerra_ezuberante-0.010.1.jar`.
- SHA256: `a341295029f779d7497fdd4233b217551a082a60857fc031b914cae3b72e386f`.
- Cliente normal0.010.1 carregado e deixado aberto para o autor, com seu som preservado. O cliente anterior encerrou salvando o mundo; não houve fechamento forçado.
