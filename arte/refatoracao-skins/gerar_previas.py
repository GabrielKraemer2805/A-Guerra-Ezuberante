"""Previas e comparacoes a partir das texturas reais, renderizadas com os UVs do modelo do mod.

Rode da raiz do projeto: python arte/refatoracao-skins/gerar_previas.py
Nenhum destes arquivos entra no JAR: a textura de producao continua sendo o PNG 64x64.
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from render_skin import render  # noqa: E402

INK, DIM, PANEL, BG, LINE = (238, 242, 248), (160, 174, 192), (32, 41, 58), (18, 24, 36), (64, 78, 100)
CHECK = ((62, 74, 88), (50, 60, 72))
INFO = {
    "marina_silva": ("MARINA SILVA", ["CABELO PRESO COM", "RISCA AO MEIO E COQUE", "BLUSA BRANCA, COLAR", "CALCA AZUL-MARINHO"],
                     ["Camada externa opaca tapava o rosto: agora livre", "Maos que nao existiam", "Coque e nuca nas costas", "Sombras na blusa e colar legivel"]),
    "tabata_amaral": ("TABATA AMARAL", ["CABELO LONGO LISO", "COM FRANJA LATERAL", "BLAZER AZUL-MARINHO", "BLUSA AZUL E GOLA"],
                      ["Lateral da cabeca era pele: agora cabelo", "Pernas sem camada base: corrigido", "Cabelo sobre ombros e costas", "Maos, punhos e sapatos"]),
    "haddad": ("FERNANDO HADDAD", ["GRISALHO CURTO", "TERNO AZUL-MARINHO", "GRAVATA AZUL", "CALCA SOCIAL COM VINCO"],
               ["Tampa cinza no topo removida", "Pele saiu da lateral do braco", "Maos e punhos brancos", "Orelha, nuca e sapatos"]),
    "jones_manoel": ("JONES MANOEL", ["CABELO CRESPO E BARBA", "OCULOS DE ARO", "CAMISETA COM FOICE E MARTELO", "CALCA AZUL, TENIS"],
                     ["Lentes opacas tapavam os olhos", "Desfoque do cabelo eliminado", "Manchas dos bracos limpas", "Alfa parcial eliminado"]),
    "dilma": ("DILMA ROUSSEFF", ["CORTE CURTO CASTANHO", "CONJUNTO VERMELHO", "FAIXA PRESIDENCIAL", "SAPATOS PRETOS"],
              ["641 cores com ruido: agora 80 por material", "307 pixels de lixo removidos do atlas", "Faixa passa pelo ombro e pelas costas", "Cabelo sem ruido, com volume"]),
}


def font(size, bold=False):
    for name in (("segoeuib.ttf", "arialbd.ttf") if bold else ("segoeui.ttf", "arial.ttf")):
        try:
            return ImageFont.truetype(f"C:/Windows/Fonts/{name}", size)
        except OSError:
            continue
    return ImageFont.load_default()


def nearest(im, s):
    return im.resize((im.width * s, im.height * s), Image.NEAREST)


def checker(w, h, cell):
    out = Image.new("RGB", (w, h), CHECK[0])
    d = ImageDraw.Draw(out)
    for y in range(0, h, cell):
        for x in range(0, w, cell):
            if (x // cell + y // cell) % 2:
                d.rectangle((x, y, x + cell - 1, y + cell - 1), fill=CHECK[1])
    return out


def atlas(path, s=8):
    im = Image.open(path).convert("RGBA")
    out = checker(64 * s, 64 * s, 4 * s).convert("RGBA")
    big = nearest(im, s)
    out.paste(big, (0, 0), big)
    return out


def card(sheet, box, title):
    d = ImageDraw.Draw(sheet)
    d.rounded_rectangle(box, 14, fill=PANEL, outline=LINE)
    d.text((box[0] + 18, box[1] + 12), title, font=font(21, True), fill=INK)


def views(path, scale):
    return {"frente": render(path, 0, 0, scale), "costas": render(path, 180, 0, scale),
            "direita": render(path, 90, 0, scale), "esquerda": render(path, -90, 0, scale),
            "rosto": nearest(Image.open(path).convert("RGBA").crop((8, 8, 16, 16)), 24),
            "3d": render(path, 32, 14, scale, shade=True)}


def presentation(name):
    title, details, _ = INFO[name]
    path = HERE / name / f"{name}_skin_64x64.png"
    v = views(path, 11)
    W, H = 1500, 800
    s = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(s)
    d.text((34, 22), f"{title}  |  GUERRA EZUBERANTE", font=font(36, True), fill=INK)
    d.text((36, 72), "SKIN MINECRAFT JAVA  ·  64 × 64 RGBA  ·  UV STEVE CLÁSSICO  ·  RENDERIZADA COM OS UVS DO MODELO DO MOD", font=font(16), fill=DIM)
    for i, (key, label) in enumerate((("frente", "FRENTE"), ("costas", "COSTAS"))):
        x = 34 + i * 300
        card(s, (x, 110, x + 286, 740), label)
        im = v[key]
        s.paste(im, (x + 143 - im.width // 2, 160), im)
    card(s, (634, 110, 1466, 740), "MAPA UV — 64 × 64")
    a = atlas(path, 8)
    s.paste(a, (654, 180))
    d.text((1190, 178), "ROSTO", font=font(20, True), fill=INK)
    f = v["rosto"]
    s.paste(f, (1190, 212), f)
    d.text((1190, 430), "DETALHES", font=font(20, True), fill=INK)
    for i, line in enumerate(details):
        d.text((1190, 464 + i * 30), line, font=font(14, True), fill=INK)
    d.text((36, 752), f"Arquivo de jogo: {name}.png  •  Esta imagem é só a prévia ampliada.", font=font(16), fill=DIM)
    out = HERE / name / f"{name}_preview.png"
    s.save(out)
    return out


def individual(name):
    path = HERE / name / f"{name}_skin_64x64.png"
    v = views(path, 16)
    for key, label in (("frente", "previa_frente"), ("costas", "previa_costas"), ("direita", "previa_lado_direito"),
                       ("esquerda", "previa_lado_esquerdo"), ("rosto", "previa_rosto"), ("3d", "previa_3_4")):
        bg = Image.new("RGB", v[key].size, BG)
        bg.paste(v[key], (0, 0), v[key])
        bg.save(HERE / name / f"{label}.png")
    atlas(path, 12).convert("RGB").save(HERE / name / "previa_atlas_uv.png")


def compare(name):
    title, _, notes = INFO[name]
    old, new = HERE / name / "original.png", HERE / name / f"{name}_skin_64x64.png"
    keys = [("frente", "FRENTE"), ("costas", "COSTAS"), ("direita", "DIREITA"), ("esquerda", "ESQUERDA"), ("3d", "3/4")]
    W, H = 1500, 1020
    s = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(s)
    d.text((30, 18), f"{title}  —  ANTES E DEPOIS", font=font(32, True), fill=INK)
    for row, (path, label) in enumerate(((old, "ANTES"), (new, "DEPOIS"))):
        y0 = 72 + row * 400
        card(s, (24, y0, W - 24, y0 + 388), label)
        v = views(path, 9)
        for i, (k, cap) in enumerate(keys):
            x = 40 + i * 212
            im = v[k]
            s.paste(im, (x, y0 + 34), im)
            d.text((x + 80, y0 + 360), cap, font=font(13), fill=DIM)
        f = nearest(Image.open(path).convert("RGBA").crop((8, 8, 16, 16)), 34)
        s.paste(f, (W - 24 - 300, y0 + 60), f)
    d.text((34, 878), "O QUE MELHOROU", font=font(18, True), fill=INK)
    for i, n in enumerate(notes):
        d.text((34 + (i % 2) * 720, 912 + (i // 2) * 34), "•  " + n, font=font(17), fill=INK)
    out = HERE / "comparacoes" / f"{name}_antes_depois.png"
    s.save(out)
    return out


if __name__ == "__main__":
    only = sys.argv[1:] or list(INFO)
    for n in only:
        individual(n)
        print(presentation(n).relative_to(HERE), compare(n).relative_to(HERE))
