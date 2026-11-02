"""Modelos 3D dos itens na mao (0.029): megafone, biblia, foice, martelo e motosserra.

Rode da raiz do projeto: python arte/itens/gerar_modelos_mao.py
Cada item vira um JSON com cubos e uma textura-paleta 16x16. No inventario, no chao e na moldura continua
o sprite plano (forge:separate_transforms); so na mao aparece o modelo. O eixo das ferramentas de cabo e o Y;
megafone e motosserra apontam para -Z, com o punho para baixo.
"""
import json
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/guerra_ezuberante"
MOD = "guerra_ezuberante"

# Material: (claro, medio, escuro). Topo usa o claro, laterais o medio, base e fundo o escuro.
MATERIALS = {
    "preto": ("3a3f47", "2a2d33", "1a1c21"),
    "cinza": ("aab5bf", "8d949c", "6d7883"),
    "branco": ("f1f3f4", "c9ced3", "9aa1a8"),
    "vermelho": ("e0483f", "c8322b", "8f201b"),
    "couro": ("4a3a30", "3c2f27", "241b16"),
    "paginas": ("f7f0dd", "efe7d2", "cfc5a8"),
    "ouro": ("f4d866", "e8c34a", "b08d28"),
    "madeira": ("b5462f", "7f2d1f", "5a1f15"),
    "aco": ("e6edf2", "aab5bf", "6d7883"),
    "laranja": ("f0a02a", "d97c16", "a85c0e"),
    "corrente": ("6d7883", "3a3f47", "20232a"),
}


def rgb(value):
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def build_palette(names):
    """Tres pixels por material, cada material numa linha da textura 16x16."""
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    where = {}
    for row, name in enumerate(names):
        where[name] = row
        for col, tone in enumerate(MATERIALS[name]):
            image.putpixel((col, row), rgb(tone))
    return image, where


def face(row, tone):
    return {"uv": [tone + .25, row + .25, tone + .75, row + .75], "texture": "#t"}


def cube(where, material, x0, y0, z0, x1, y1, z1):
    """Um cubo de cor unica, com o topo claro e a base escura."""
    row = where[material]
    faces = {side: face(row, 1) for side in ("north", "south", "east", "west")}
    faces["north"] = face(row, 0)
    faces["up"] = face(row, 0)
    faces["down"] = face(row, 2)
    faces["east"] = face(row, 2)
    return {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}


# Onde o punho fecha, em 1/16 de bloco, no espaco de cada contexto. Medido a partir dos valores do item/handheld
# vanilla, cujo cabo fica no canto do sprite. Na terceira pessoa, sem giro, o Y do modelo aponta para a frente do
# corpo e o +Z para cima; na primeira pessoa, o Y para cima e o -Z para longe da camera.
FIST_THIRD = (0, -3.1, 1.75)
FIST_FIRST = (1.13, -2.2, -.84)


def rotate(rotation, vector):
    """Mesma ordem do jogo (rotationXYZ): gira em Z, depois em Y, depois em X."""
    x, y, z = vector
    rx, ry, rz = (math.radians(angle) for angle in rotation)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    return x, y, z


def transform(rotation, scale, grip, fist):
    """O giro pedido, com a translacao que leva o ponto de pegada do modelo para dentro do punho."""
    turned = rotate(rotation, [(value - 8) * scale for value in grip])
    return {"rotation": list(rotation), "translation": [round(fist[i] - turned[i], 3) for i in range(3)], "scale": [scale] * 3}


def mirrored(entry):
    """A outra mao: o mesmo giro e a posicao espelhada em X, para a face de cada ferramenta continuar para a frente."""
    return {**entry, "translation": [-entry["translation"][0], *entry["translation"][1:]]}


def hands(grip, third_rotation, third_scale, first_rotation, first_scale, left_third=None, left_first=None):
    third = transform(third_rotation, third_scale, grip, FIST_THIRD)
    first = transform(first_rotation, first_scale, grip, FIST_FIRST)
    return {
        "thirdperson_righthand": third,
        "thirdperson_lefthand": mirrored(transform(left_third, third_scale, grip, FIST_THIRD)) if left_third else mirrored(third),
        "firstperson_righthand": first,
        "firstperson_lefthand": mirrored(transform(left_first, first_scale, grip, FIST_FIRST)) if left_first else mirrored(first),
    }


MODELS = {
    "megafone": dict(
        materials=["preto", "branco", "cinza", "vermelho"],
        display=hands((8, 3, 9.5), (82, 0, 0), .62, (0, 6, 0), .56),
        cubes=[
            ("preto", 7, 0, 8.5, 9, 7, 10.5),      # punho
            ("preto", 7.4, 5, 7, 8.6, 6.4, 8.5),   # gatilho
            ("cinza", 6.5, 6.5, 6, 9.5, 9.5, 12),  # corpo
            ("preto", 6, 6, 12, 10, 10, 13.5),     # tampa traseira
            ("branco", 5.5, 5.5, 3.5, 10.5, 10.5, 6),
            ("branco", 4.5, 4.5, 1, 11.5, 11.5, 3.5),
            ("branco", 3.5, 3.5, -1.5, 12.5, 12.5, 1),
            ("vermelho", 3, 3, -2.5, 13, 13, -1.5),  # boca
            ("preto", 5, 5, -2.7, 11, 11, -2.5),
            ("vermelho", 6.2, 10.5, 4, 9.8, 10.8, 5.5),
        ]),
    "biblia": dict(
        materials=["couro", "paginas", "ouro", "preto"],
        display=hands((8, 4, 8), (28, 90, 0), .6, (-12, -160, 0), .72, left_first=(-12, 160, 0)),
        cubes=[
            ("couro", 3.5, 1, 6.2, 12.5, 13, 6.9),   # capa da frente
            ("couro", 3.5, 1, 9.1, 12.5, 13, 9.8),   # capa de tras
            ("preto", 3.5, 1, 6.9, 4.1, 13, 9.1),    # lombada
            ("paginas", 4.1, 1.4, 6.9, 12.2, 12.6, 9.1),
            ("ouro", 7.4, 3.4, 5.95, 8.6, 11, 6.2),  # cruz
            ("ouro", 5.6, 8, 5.95, 10.4, 9.2, 6.2),
            ("vermelho_fita", 8.6, -1, 7.8, 9.4, 1.4, 8.1),
        ]),
    "foice": dict(
        materials=["madeira", "aco", "preto"],
        display=hands((8, 2, 8), (38, 90, 0), .78, (-22, 90, 0), .8),
        cubes=[
            ("madeira", 7.4, -2, 7.4, 8.6, 7, 8.6),  # cabo
            ("preto", 7.2, 7, 7.2, 8.8, 8, 8.8),     # virola
            ("aco", 6.4, 8, 7.6, 8.6, 10.5, 8.4),
            ("aco", 5.2, 10, 7.6, 7.2, 12.5, 8.4),
            ("aco", 4.8, 12, 7.6, 6.6, 14.2, 8.4),
            ("aco", 5.6, 13.8, 7.6, 8.4, 15.6, 8.4),
            ("aco", 8, 14.4, 7.6, 10.6, 16, 8.4),
            ("aco", 10.2, 13.8, 7.6, 12, 15.2, 8.4),
            ("aco", 11.6, 13, 7.65, 12.8, 14, 8.35), # ponta
        ]),
    "martelo": dict(
        materials=["madeira", "aco", "preto"],
        display=hands((8, 2, 8), (38, -90, 0), .78, (-22, -90, 0), .8),
        cubes=[
            ("madeira", 7.4, -2, 7.4, 8.6, 10, 8.6),  # cabo
            ("preto", 7.2, 9.2, 7.2, 8.8, 10.2, 8.8),
            ("aco", 5.5, 10, 6.5, 10.5, 13.5, 9.5),   # cabeca
            ("aco", 3.5, 10.4, 6.9, 5.5, 13.1, 9.1),  # face
            ("preto", 3.2, 10.6, 7.1, 3.5, 12.9, 8.9),
            ("aco", 10.5, 10.8, 7.2, 12.5, 12.8, 8.8),
        ]),
    "motosserra": dict(
        materials=["laranja", "preto", "aco", "corrente"],
        display=hands((8, 3, 11), (82, 0, 0), .58, (0, 6, 0), .66),
        cubes=[
            ("preto", 7, 0, 10, 9, 6, 12),            # punho traseiro
            ("preto", 7, 5.5, 8.5, 9, 7, 11.5),
            ("laranja", 5.5, 6, 2, 10.5, 12, 10),     # motor
            ("laranja", 6, 12, 4, 10, 13, 9),
            ("preto", 4.8, 7.5, 4, 5.5, 10.5, 8),     # entrada de ar
            ("preto", 10.5, 7.5, 4, 11.2, 10.5, 8),
            ("preto", 7.4, 13, 4, 8.6, 15, 5),        # alca de cima
            ("preto", 7.4, 14.5, 4, 8.6, 15.5, 9.5),
            ("preto", 7.4, 13, 8.5, 8.6, 15, 9.5),
            ("preto", 5, 5.5, 1, 11, 12.5, 2),        # protecao da corrente
            ("aco", 7.5, 7.5, -13, 8.5, 10.5, 1),     # sabre
            ("corrente", 7.3, 10.5, -13.2, 8.7, 11, 1),
            ("corrente", 7.3, 7, -13.2, 8.7, 7.5, 1),
            ("corrente", 7.3, 7, -13.6, 8.7, 11, -13),
        ] + [("corrente", 7.4, 11, z, 8.6, 11.4, z + .8) for z in range(-12, 1, 2)]
          + [("corrente", 7.4, 6.6, z, 8.6, 7, z + .8) for z in range(-12, 1, 2)]),
}

# Fita marcadora da biblia.
MATERIALS["vermelho_fita"] = MATERIALS["vermelho"]
MODELS["biblia"]["materials"].append("vermelho_fita")

# Item no inventario, no chao e na moldura: o sprite plano de sempre.
FLAT_CONTEXTS = ("gui", "ground", "fixed", "head")


def item_json(name, hand_model, flat_parent):
    flat = {"parent": flat_parent, "textures": {"layer0": f"{MOD}:item/{name}"}}
    return {
        "loader": "forge:separate_transforms",
        "gui_light": "front",
        "base": {"parent": f"{MOD}:item/mao/{hand_model}"},
        "perspectives": {context: flat for context in FLAT_CONTEXTS},
    }


def main():
    (ASSETS / "models/item/mao").mkdir(parents=True, exist_ok=True)
    (ASSETS / "textures/item/mao").mkdir(parents=True, exist_ok=True)
    for name, spec in MODELS.items():
        palette, where = build_palette(spec["materials"])
        palette.save(ASSETS / f"textures/item/mao/{name}.png")
        model = {
            "textures": {"t": f"{MOD}:item/mao/{name}", "particle": f"{MOD}:item/mao/{name}"},
            "gui_light": "front",
            "elements": [cube(where, *each) for each in spec["cubes"]],
            "display": spec["display"],
        }
        text = json.dumps(model, indent=1)
        (ASSETS / f"models/item/mao/{name}.json").write_text(text + "\n", encoding="utf-8")
    for name, hand, parent in (("megafone", "megafone", "minecraft:item/generated"), ("biblia", "biblia", "minecraft:item/generated"),
                               ("foice_e_martelo", "foice", "minecraft:item/handheld"), ("motosserra", "motosserra", "minecraft:item/handheld")):
        text = json.dumps(item_json(name, hand, parent), indent=2)
        (ASSETS / f"models/item/{name}.json").write_text(text + "\n", encoding="utf-8")
    print("modelos gerados:", ", ".join(MODELS))


if __name__ == "__main__":
    main()
