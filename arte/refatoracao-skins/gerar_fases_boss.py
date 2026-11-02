"""Segundas fases dos bosses (0.029) e variedades de anjo.

Rode da raiz do projeto: python arte/refatoracao-skins/gerar_fases_boss.py
Parte das texturas atuais, sem tocar nelas:
  lula_eleitoral.png    Lula sem camisa e musculoso, mesma cabeca e mesma calca
  renan_akatsuki.png    Renan com o manto preto de nuvens vermelhas
  anjo_*.png            o anjo em outras cores (a forma do vex nao muda)
  lula_eleitoral.geo.json  o modelo do Lula sem a barriga, com tronco e bracos largos
"""
import colorsys
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/guerra_ezuberante"
ENTITY = ASSETS / "textures/entity"
ENTRADA = ROOT / "assets-entrada/skins"


def rgb(value):
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def fill(image, box, color):
    for x in range(box[0], box[2]):
        for y in range(box[1], box[3]):
            image.putpixel((x, y), color)


def clear(image, box):
    fill(image, box, (0, 0, 0, 0))


def paint(image, x0, y0, rows, colors):
    for dy, row in enumerate(rows):
        for dx, key in enumerate(row):
            if key != ".":
                image.putpixel((x0 + dx, y0 + dy), colors[key])


TORSO_OVERLAY = (16, 32, 40, 48)
RIGHT_SLEEVE = (40, 32, 56, 48)
LEFT_SLEEVE = (48, 48, 64, 64)


# ------------------------------------------------------------------ Lula eleitoral
def lula_eleitoral():
    image = Image.open(ENTITY / "lula_skin_64x64.png").convert("RGBA")
    # Tons da pele do proprio Lula, mais o pelo grisalho do peito.
    tones = dict(L=rgb("f3c7a0"), M=rgb("e0a77e"), S=rgb("c98f68"), D=rgb("a56f4e"), H=rgb("cfcac2"), B=rgb("2f2622"), G=rgb("8b8378"))
    for box in (TORSO_OVERLAY, RIGHT_SLEEVE, LEFT_SLEEVE):
        clear(image, box)
    paint(image, 20, 20, [  # peito e abdomen, 8x12
        "SLLMMLLS",
        "LLHMMHLL",
        "LMHSSHML",
        "SMMDDMMS",
        "SDDSSDDS",
        "SMLDDLMS",
        "SDDSSDDS",
        "SMLDDLMS",
        "SDDSSDDS",
        "SSMDDMSS",
        "DSSMMSSD",
        "BGBBBBGB"], tones)
    paint(image, 32, 20, [  # costas
        "SMMDDMMS",
        "MLLSSLLM",
        "MLMDDMLM",
        "SMMDDMMS",
        "DSMDDMSD",
        "SMMDDMMS",
        "SSMDDMSS",
        "DSMDDMSD",
        "SSMDDMSS",
        "DSSDDSSD",
        "DSSSSSSD",
        "BGBBBBGB"], tones)
    for x0 in (16, 28):  # lados do tronco, 4x12
        paint(image, x0, 20, ["MMSS", "MLMS", "MMSS", "SMSD", "MMSS", "SMSD", "MMSS", "SMSD", "SSSD", "SSSD", "DSSD", "BGBB"], tones)
    fill(image, (20, 16, 28, 20), tones["L"])  # ombros, visto de cima
    paint(image, 20, 16, ["LLLSSLLL", "LLMHHMLL", "MMSSSSMM", "SSSSSSSS"], tones)
    fill(image, (28, 16, 36, 20), tones["B"])  # cintura, vista de baixo
    arm_front = ["LLML", "LMMS", "SMMD", "MLLS", "MLLS", "SMMD", "SSMD", "MLMS", "MMMS", "SMSD", "MMMS", "SSSD"]
    arm_side = ["MLMS", "MMMS", "SMSD", "MMMS", "MLMS", "SMSD", "SSSD", "MMMS", "MMSS", "SMSD", "MMMS", "SSSD"]
    arm_back = ["SMMS", "MMMS", "SSSD", "SMMS", "SMMS", "SSSD", "DSSD", "SMMS", "SMMS", "SSSD", "SMMS", "DSSD"]
    for x0, y0 in ((40, 20), (32, 52)):  # braco direito e esquerdo: lado, frente, lado, costas
        paint(image, x0, y0, arm_side, tones)
        paint(image, x0 + 4, y0, arm_front, tones)
        paint(image, x0 + 8, y0, arm_side, tones)
        paint(image, x0 + 12, y0, arm_back, tones)
        fill(image, (x0 + 4, y0 - 4, x0 + 8, y0), tones["L"])
        fill(image, (x0 + 8, y0 - 4, x0 + 12, y0), tones["S"])
    return image


def lula_eleitoral_geo():
    source = json.loads((ASSETS / "geo/lula.geo.json").read_text(encoding="utf-8"))
    geometry = source["minecraft:geometry"][0]
    geometry["description"]["identifier"] = "geometry.guerra.lula_eleitoral"
    geometry["bones"] = [bone for bone in geometry["bones"] if bone["name"] != "belly"]
    grow = {"torso": 1.3, "right_arm": .9, "right_forearm": .75, "left_arm": .9, "left_forearm": .75, "right_leg": .35, "right_shin": .3, "left_leg": .35, "left_shin": .3}
    # Os bracos saem para os lados, junto com o pivo, para o tronco largo nao os atravessar.
    shift = {"right_arm": -1.9, "right_forearm": -1.9, "right_hand": -1.9, "left_arm": 1.9, "left_forearm": 1.9, "left_hand": 1.9}
    for bone in geometry["bones"]:
        name = bone["name"]
        if name in shift:
            bone["pivot"][0] += shift[name]
        for each in bone.get("cubes", []):
            if name in shift:
                each["origin"][0] += shift[name]
            if name in grow:
                each["inflate"] = (each.get("inflate") or 0) + grow[name]
    return json.dumps(source, indent=1, ensure_ascii=False) + "\n"


# ------------------------------------------------------------------ Renan Akatsuki
def renan_akatsuki():
    image = Image.open(ENTITY / "renan_santos.png").convert("RGBA")
    k = dict(K=rgb("17161b"), C=rgb("24232a"), L=rgb("33323b"), D=rgb("0c0b0e"), R=rgb("c4161c"), r=rgb("8a0f14"), W=rgb("ece7e2"), F=rgb("3d3a38"), S=image.getpixel((44, 30)))
    for box in (TORSO_OVERLAY, RIGHT_SLEEVE, LEFT_SLEEVE, (0, 32, 16, 48), (0, 48, 16, 64)):
        clear(image, box)
    cloud = [".WW.", "WRRW", "WRrW", ".WW."]
    wide = [".WWW.", "WRRRW", "WRRrW", ".WWW."]

    def cloak(x0, y0, width, tone="C"):
        rows = []
        for row in range(12):
            base = "K" if row in (3, 8) else tone
            rows.append(base * (width - 1) + "D")
        paint(image, x0, y0, rows, k)

    # Tronco: manto fechado com o forro vermelho na gola e o ziper no meio.
    cloak(20, 20, 8, "L")
    paint(image, 20, 20, ["LRRDDRRL", "LLrDDrLL"], k)
    for y in range(22, 32):
        image.putpixel((24, y), k["D"])
    paint(image, 21, 23, wide, k)
    paint(image, 24, 28, cloud, k)
    cloak(32, 20, 8)
    paint(image, 33, 22, wide, k)
    paint(image, 35, 27, wide, k)
    cloak(16, 20, 4)
    cloak(28, 20, 4)
    paint(image, 16, 24, cloud, k)
    paint(image, 28, 27, cloud, k)
    fill(image, (20, 16, 28, 20), k["K"])
    paint(image, 20, 18, ["RRRRRRRR", "rrrrrrrr"], k)
    fill(image, (28, 16, 36, 20), k["D"])
    # Bracos: mangas largas ate o punho e a mao de fora, com a pele do proprio Renan.
    for x0, y0 in ((40, 20), (32, 52)):
        for side in range(4):
            cloak(x0 + side * 4, y0, 4, "L" if side == 1 else "C")
            fill(image, (x0 + side * 4, y0 + 11, x0 + side * 4 + 4, y0 + 12), k["S"])
            fill(image, (x0 + side * 4, y0 + 10, x0 + side * 4 + 4, y0 + 11), k["D"])
        paint(image, x0 + 4, y0 + 2, cloud, k)
        paint(image, x0 + 12, y0 + 5, cloud, k)
        fill(image, (x0 + 4, y0 - 4, x0 + 12, y0), k["K"])
    # Pernas: a barra do manto ate a canela e a sandalia embaixo.
    for x0, y0 in ((0, 20), (16, 52)):
        for side in range(4):
            cloak(x0 + side * 4, y0, 4, "L" if side == 1 else "C")
            fill(image, (x0 + side * 4, y0 + 9, x0 + side * 4 + 4, y0 + 11), k["F"])
            fill(image, (x0 + side * 4, y0 + 11, x0 + side * 4 + 4, y0 + 12), k["D"])
        paint(image, x0 + 4, y0 + 3, cloud, k)
        paint(image, x0 + 12, y0 + 2, cloud, k)
        fill(image, (x0 + 4, y0 - 4, x0 + 8, y0), k["K"])
        fill(image, (x0 + 8, y0 - 4, x0 + 12, y0), k["D"])
    # Gola alta na camada da cabeca: tapa o queixo por todos os lados, sem cobrir a boca.
    for box in ((32, 15, 40, 16), (40, 15, 48, 16), (48, 15, 56, 16), (56, 15, 64, 16)):
        fill(image, box, k["K"])
    for x in range(40, 48):
        image.putpixel((x, 15), k["r"])
    return image


# ------------------------------------------------------------------ Anjos
ANGELS = {
    # id: (rampa da tunica, rampa das asas)
    "querubim": (("ffe3ec", "f7bfd0", "dd94ad"), ("fffbe6", "ffe98f", "e0be57")),
    "serafim": (("ffb25a", "e8602a", "a8331a"), ("ffd24a", "f28a1e", "c4421a")),
    "arcanjo": (("c9d4e6", "7f90b3", "4a5a80"), ("ffe27a", "e0b02a", "9f7a18")),
    "guardiao": (("b7dcff", "5d9de8", "2f62ab"), ("ffffff", "dfe7f0", "aab7c6")),
    "trombeteiro": (("d9c2ff", "9a6fe0", "5f3fa8"), ("fff0b0", "e8c34a", "b08d28")),
}


def ramp_pick(ramp, value):
    return rgb(ramp[0] if value > .9 else ramp[1] if value > .78 else ramp[2])


def recolor_angel(base, robe, wings):
    image = base.copy()
    for x in range(image.width):
        for y in range(image.height):
            r, g, b, a = image.getpixel((x, y))
            if a == 0:
                continue
            hue, saturation, value = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            whitish = saturation < .12 and value > .6
            yellow = .09 < hue < .19 and saturation > .3
            if y >= 20 and x >= 16:  # asas
                image.putpixel((x, y), ramp_pick(wings, value))
            elif whitish:
                image.putpixel((x, y), ramp_pick(robe, value))
            elif yellow and y >= 10:  # detalhes da tunica
                image.putpixel((x, y), rgb(wings[1]))
    return image


def main():
    ENTRADA.mkdir(parents=True, exist_ok=True)
    for name, image in (("lula_eleitoral", lula_eleitoral()), ("renan_akatsuki", renan_akatsuki())):
        assert image.size == (64, 64) and image.mode == "RGBA", name
        for box in ((8, 8, 16, 16), (20, 20, 28, 32), (44, 20, 48, 32), (36, 52, 40, 64), (4, 20, 8, 32), (20, 52, 24, 64)):
            assert image.getchannel("A").crop(box).getextrema() == (255, 255), (name, box)
        image.save(ENTITY / f"{name}.png")
        image.save(ENTRADA / f"{name}_skin_64x64.png")
    (ASSETS / "geo/lula_eleitoral.geo.json").write_text(lula_eleitoral_geo(), encoding="utf-8")
    base = Image.open(ENTITY / "anjo.png").convert("RGBA")
    for name, (robe, wings) in ANGELS.items():
        recolor_angel(base, robe, wings).save(ENTITY / f"anjo_{name}.png")
    print("ok")


if __name__ == "__main__":
    main()
