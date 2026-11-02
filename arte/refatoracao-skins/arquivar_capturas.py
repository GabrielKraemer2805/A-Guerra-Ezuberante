"""Recorta as capturas do teste no jogo (tools/skins_visual_test.ps1) para validacao/jogo.

Rode da raiz do projeto: python arte/refatoracao-skins/arquivar_capturas.py
"""
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
SRC = HERE.parents[1] / ".tools/run-skins-client/screenshots"
DST = HERE / "validacao/jogo"
NAMES = ["marina_silva", "tabata_amaral", "haddad", "jones_manoel", "dilma"]
VIEWS = ["frente", "costas", "lado_direito", "lado_esquerdo"]
BOX = (500, 200, 780, 570)

DST.mkdir(parents=True, exist_ok=True)
sheet = Image.new("RGB", (280 * 4, 370 * 5), (18, 24, 36))
for i, n in enumerate(NAMES):
    for j, v in enumerate(VIEWS):
        im = Image.open(SRC / f"skin-{n}-{v}.png").convert("RGB").crop(BOX)
        im.save(DST / f"{n}_{v}.png")
        sheet.paste(im, (j * 280, i * 370))
sheet.save(DST / "todas_no_jogo.png")
group = Image.open(SRC / "skin-grupo.png").convert("RGB").crop((320, 240, 980, 420))
group.resize((1320, 360), Image.NEAREST).save(DST / "grupo_com_lula_e_renan.png")
print("capturas arquivadas em", DST.relative_to(HERE))
