"""Validacao tecnica das skins contra o modelo real (geo/humanoid.geo.json).

Uso, da raiz do projeto:
  python arte/refatoracao-skins/validar_skins.py          # skins refatoradas e originais; grava validacao/relatorio.md
Cada skin integrada tambem e conferida byte a byte com a textura usada pelo jogo.
Sai com codigo 1 se alguma skin refatorada reprovar. Originais podem reprovar: servem de linha de base.
"""
import hashlib
import struct
import sys
import warnings
from pathlib import Path

from PIL import Image

warnings.simplefilter("ignore", DeprecationWarning)
HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from render_skin import CUBES, ROOT  # noqa: E402

TEXTURES = ROOT / "src/main/resources/assets/guerra_ezuberante/textures/entity"
NAMES = ["marina_silva", "tabata_amaral", "haddad", "jones_manoel", "dilma"]
# Atlas: caixa = (u, v, largura, altura, profundidade)
BASE = {"cabeca": (0, 0, 8, 8, 8), "tronco": (16, 16, 8, 12, 4), "braco_d": (40, 16, 4, 12, 4),
        "braco_e": (32, 48, 4, 12, 4), "perna_d": (0, 16, 4, 12, 4), "perna_e": (16, 48, 4, 12, 4)}


def faces(u, v, w, h, d):
    return {"topo": (u + d, v, u + d + w, v + d), "base": (u + d + w, v, u + d + 2 * w, v + d),
            "direita": (u, v + d, u + d, v + d + h), "frente": (u + d, v + d, u + d + w, v + d + h),
            "esquerda": (u + d + w, v + d, u + 2 * d + w, v + d + h), "costas": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)}


def geo_regions():
    """Todos os retangulos que o modelo realmente amostra."""
    regions = []
    for _, _, uv, _ in CUBES:
        for f in uv.values():
            (a, b), (us, vs) = f["uv"], f["uv_size"]
            x0, x1 = sorted((a, a + us))
            y0, y1 = sorted((b, b + vs))
            regions.append((int(x0), int(y0), int(x1), int(y1)))
    return regions


def check(path):
    fails, notes = [], []
    raw = Path(path).read_bytes()
    if raw[:8] != b"\x89PNG\r\n\x1a\n":
        return ["nao e PNG"], notes
    w, h, depth, ctype = struct.unpack(">IIBB", raw[16:26])
    if (w, h) != (64, 64):
        fails.append(f"dimensao {w}x{h}")
    if ctype != 6 or depth != 8:
        fails.append(f"PNG nao RGBA de 8 bits (tipo {ctype}, profundidade {depth})")
    Image.open(path).verify()
    im = Image.open(path).convert("RGBA")
    alpha = im.getchannel("A")
    px = im.load()
    if set(alpha.getdata()) - {0, 255}:
        fails.append("alfa parcial (antialiasing ou desfoque nas bordas)")

    # Corpo base completo: as 6 faces das 6 partes, sem buraco.
    for part, box in BASE.items():
        for fname, rect in faces(*box).items():
            if alpha.crop(rect).getextrema() != (255, 255):
                fails.append(f"{part}/{fname}: base com buraco {rect}")

    # Nada fora do que o modelo amostra (lixo de pixels no atlas).
    used = [[False] * 64 for _ in range(64)]
    for x0, y0, x1, y1 in geo_regions():
        for y in range(y0, y1):
            for x in range(x0, x1):
                used[y][x] = True
    stray = sum(1 for y in range(64) for x in range(64) if px[x, y][3] and not used[y][x])
    if stray:
        fails.append(f"{stray} pixels fora de regioes usadas pelo modelo")

    # Olhos visiveis: a camada externa nao pode tapar os olhos com pixels opacos.
    hat_face = [(x, y) for y in range(12, 13) for x in range(41, 47)]
    covered = sum(1 for p in hat_face if px[p][3])
    if covered:
        fails.append(f"camada externa cobre os olhos ({covered} de {len(hat_face)} pixels da linha dos olhos)")
    mid_hat = sum(1 for y in range(10, 16) for x in range(42, 46) if px[x, y][3])
    if mid_hat > 4:
        fails.append(f"camada externa cobre o centro do rosto ({mid_hat} pixels)")

    # Rosto em lugar certo: olhos nao vao para costas ou lados.
    front = im.crop((8, 8, 16, 16))
    back = im.crop((24, 8, 32, 16))
    if list(front.getdata()) == list(back.getdata()):
        fails.append("costas da cabeca iguais a frente")
    light = {px[x, 12][:3] for x in (9, 14)}
    for name, rect in (("costas", (24, 8, 32, 16)), ("direita", (0, 8, 8, 16)), ("esquerda", (16, 8, 24, 16))):
        crop = im.crop(rect)
        if light & set(p[:3] for p in crop.getdata() if p[3]):
            fails.append(f"branco do olho aparece em {name} da cabeca")
    if len({px[x, y][:3] for x in range(8, 16) for y in range(8, 16)}) < 8:
        fails.append("rosto com menos de 8 tons (chapado)")

    # Tronco: costas com desenho proprio; mesma coisa nos membros.
    if list(im.crop((20, 20, 28, 32)).getdata()) == list(im.crop((32, 20, 40, 32)).getdata()):
        fails.append("costas do tronco iguais a frente")

    # Maos: a ponta de cada braco precisa ser diferente do meio da manga, nas 4 faces.
    for part in ("braco_d", "braco_e"):
        f = faces(*BASE[part])
        for fname in ("direita", "frente", "esquerda", "costas"):
            x0, y0, x1, y1 = f[fname]
            sleeve = {im.getpixel((x, y0 + 3))[:3] for x in range(x0, x1)}
            hand = {im.getpixel((x, y1 - 1))[:3] for x in range(x0, x1)}
            if not hand or sleeve & hand:
                notes.append(f"{part}/{fname}: mao sem distincao da manga")

    # Interpolacao: rampas continuas deixam muitos tons quase iguais vizinhos.
    colors = {p[:3] for p in im.getdata() if p[3]}
    if len(colors) > 256:
        fails.append(f"{len(colors)} cores: provavel imagem reduzida")
    notes.append(f"{len(colors)} cores")
    return fails, notes


def face_distance(a, b):
    x = Image.open(a).convert("RGB").crop((8, 8, 16, 16)).getdata()
    y = Image.open(b).convert("RGB").crop((8, 8, 16, 16)).getdata()
    return sum(abs(p[i] - q[i]) for p, q in zip(x, y) for i in range(3)) / (64 * 3)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main():
    lines = ["# Validacao das skins refatoradas", ""]
    bad = False
    for label, finder in (("Refatoradas", lambda n: HERE / n / f"{n}_skin_64x64.png"), ("Originais (linha de base)", lambda n: HERE / n / "original.png")):
        lines += [f"## {label}", ""]
        for n in NAMES:
            f = finder(n)
            fails, notes = check(f)
            hand_notes = [x for x in notes if "mao" in x]
            status = "APROVADA" if not fails and not hand_notes else "REPROVADA"
            if label == "Refatoradas" and status != "APROVADA":
                bad = True
            lines.append(f"- **{n}**: {status} ({notes[-1]})")
            for x in fails + hand_notes:
                lines.append(f"  - {x}")
        lines.append("")
    lines += ["## Rostos distintos (diferenca media por canal, 0-255)", ""]
    new = [HERE / n / f"{n}_skin_64x64.png" for n in NAMES]
    for i, a in enumerate(new):
        for b in new[i + 1:]:
            d = face_distance(a, b)
            if d < 12:
                bad = True
            lines.append(f"- {a.stem.replace('_skin_64x64', '')} x {b.stem.replace('_skin_64x64', '')}: {d:.1f}")
    lines += ["", "## Integracao: textura do jogo igual a skin refatorada", ""]
    for n in NAMES:
        same = sha(TEXTURES / f"{n}.png") == sha(HERE / n / f"{n}_skin_64x64.png")
        src = ROOT / "assets-entrada/skins" / f"{n}_skin_64x64_refatorada.png"
        src_ok = src.exists() and sha(src) == sha(TEXTURES / f"{n}.png")
        lines.append(f"- {n}: textura {'igual' if same else 'DIFERENTE'}; fonte em assets-entrada {'igual' if src_ok else 'DIFERENTE/AUSENTE'}")
        if "--integrada" in sys.argv and not (same and src_ok):
            bad = True
    text = "\n".join(lines) + "\n"
    (HERE / "validacao" / "relatorio.md").write_text(text, encoding="utf-8")
    print(text)
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
