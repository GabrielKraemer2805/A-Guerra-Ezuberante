"""Sprites dos itens craftaveis da 0.024: PNG RGBA 16x16 desenhado pixel a pixel, sem redimensionamento.

Rode da raiz do projeto: python arte/itens/gerar_itens.py
Valida cada sprite, grava na pasta de texturas do mod e gera a previa em arte/itens/previa_itens.png.
"""
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
TEXTURES = HERE.parents[1] / "src/main/resources/assets/guerra_ezuberante/textures/item"

SPRITES = {
    "picanha": (dict(o="4a1f18", r="b5423a", R="8f2f2a", l="d9675a", w="f2e3c8", W="d9c6a0"), [
        "................", "................", "................",
        "...oooooooooo...",
        "..owwwwwwwwwWo..",
        ".owwWWWWWWWWWWo.",
        ".orllrrrrrrrrRo.",
        ".orlrrRrrrRrrRo.",
        ".orrrRrrrRrrrRo.",
        ".orrRrrrRrrrRRo.",
        "..orrrrRrrrRRo..",
        "...orrRrrrRRo...",
        "....orrrrRRo....",
        ".....oooooo.....",
        "................", "................"]),
    "cerveja": (dict(o="6b4310", y="e8a91f", Y="c4821a", l="ffd35a", f="fff8e6", F="d9d2bd", g="c8e2ea"), [
        "................",
        "....ffffff......",
        "...ffffFffff....",
        "...oFffffFFo....",
        "...olyyyyYYo....",
        "...olyyyyYYoooo.",
        "...olyyyyYYo..o.",
        "...olyyyyYYo..o.",
        "...olyyyyYYo..o.",
        "...olyyyyYYoooo.",
        "...olyyyyYYo....",
        "...olyyyyYYo....",
        "...oYYYYYYYo....",
        "...ogggggggo....",
        "...ooooooooo....",
        "................"]),
    "feijoada": (dict(o="3a2416", w="8f5b30", W="6b4222", l="c98a55", b="2a1b22", B="46303a", p="a8604a", g="4f8f3a"), [
        "................", "................", "................", "................",
        "....oooooooo....",
        "..ooBbBpbBgBoo..",
        ".obBbpBbBbgBbBo.",
        ".oooooooooooooo.",
        ".olwwwwwwwwwWWo.",
        ".olwwwwwwwwWWWo.",
        "..olwwwwwwWWWo..",
        "...owwwwwWWWo...",
        "....oooooooo....",
        "................", "................", "................"]),
    "bolo_de_mandioca": (dict(o="6a4318", c="b97a2a", C="8f5a1c", y="d8b45a", Y="e9cf7f", s="fff3d0"), [
        "................", "................", "................",
        ".....oooooo.....",
        "...ooccccccoo...",
        "..ocYYYYYYYYco..",
        ".ocYYsYYYYsYYco.",
        ".ocYYYYYsYYYYco.",
        ".occYYYYYYYYcco.",
        ".oyccccccccccyo.",
        ".oYyyyyyyyyyyCo.",
        ".oYyyyyyyyyyCCo.",
        "..oCCCCCCCCCCo..",
        "...oooooooooo...",
        "................", "................"]),
    "cloroquina": (dict(o="40454f", w="f2f4f5", W="c9cfd4", r="c8322b", R="96221f", b="3f7fc4"), [
        "................", "................",
        "..oooooooooooo..",
        "..owwwwwwwwwWo..",
        "..owwwwwwwwwWo..",
        "..owrrrrrrrrWo..",
        "..owrRRRRRRrWo..",
        "..owrrrrrrrrWo..",
        "..owwwwwwwwwWo..",
        "..owwbbbbbwwWo..",
        "..owwwwwwwwwWo..",
        "..owbbwbbwwwWo..",
        "..owwwwwwwwwWo..",
        "..oWWWWWWWWWWo..",
        "..oooooooooooo..",
        "................"]),
    "megafone": (dict(o="2a2d33", w="f1f3f4", g="c9ced3", G="8d949c", r="c8322b", h="3a3f47"), [
        "................", "................", "................",
        "...........oo...",
        ".........oogo...",
        ".......oowgGo...",
        "..oo.oowwggGo...",
        ".orroowwgggGo...",
        ".orrwwwggggGo...",
        ".orrwwggggGGo...",
        ".orooggggGGGo...",
        "..oo.ooGGGGGo...",
        ".....ohooGGGo...",
        ".....ohh.ooGo...",
        ".....ooo...oo...",
        "................"]),
    "foice_e_martelo": (dict(o="262a31", l="e6edf2", m="aab5bf", M="6d7883", h="b5462f", H="7f2d1f"), [
        "......ooo.......",
        "....oollMo......",
        "...olmMooo..ooo.",
        "..olmMo....olmMo",
        "..omMo....olmMMo",
        ".olmMo...oolmMo.",
        ".omMo...ohooMo..",
        ".omMo..ohHo.oo..",
        ".olmMoohHo......",
        "..olmMoHo.......",
        "...oohHoMoo.....",
        "..ohHooohHo.....",
        ".ohHo...ohHo....",
        "ohHo.....ohHo...",
        "oHo.......oHo...",
        ".o.........o...."]),
    "biblia": (dict(o="17120f", b="2b211c", B="3c2f27", g="e8c34a", G="b08d28", p="efe7d2"), [
        "................", "................",
        "...ooooooooooo..",
        "..obbbbBBBBBBBo.",
        "..obBBBBgBBBBBo.",
        "..obBBBBgBBBBBo.",
        "..obBBggggBBBBo.",
        "..obBBBBgBBBBBo.",
        "..obBBBBgBBBBBo.",
        "..obBBBBgBBBBBo.",
        "..obBBBBGBBBBBo.",
        "..obBBBBBBBBBBo.",
        "..obbbbbbbbbbbo.",
        "..oppppppppppo..",
        "...oooooooooo...",
        "................"]),
    "motosserra": (dict(o="20232a", l="e6edf2", m="aab5bf", M="6d7883", d="3a3f47", y="f0a02a", Y="d97c16", k="26282d"), [
        "............oooo",
        "...........omlmo",
        "..........omlMdo",
        ".........omlMdo.",
        "........omlMdo..",
        ".......omlMdo...",
        "......omlMdo....",
        ".....ommMdo.....",
        "..oooyyyyo......",
        ".oyyYYYYYYo.....",
        ".oyYYYYYYYYo....",
        ".oyYYkkYYYYo....",
        "..oYYkkYYYo.....",
        "...ooYYYoo......",
        "...okkko........",
        "...ooo.........."]),
}


def draw(palette, rows):
    assert len(rows) == 16, len(rows)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row, len(row))
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), tuple(int(palette[ch][i:i + 2], 16) for i in (0, 2, 4)) + (255,))
    return img


def check(name, img):
    assert img.size == (16, 16) and img.mode == "RGBA", name
    alpha = list(img.getchannel("A").getdata())
    assert 0 in alpha and 255 in alpha and set(alpha) <= {0, 255}, name


def main():
    sprites = {name: draw(*spec) for name, spec in SPRITES.items()}
    sheet = Image.new("RGB", (len(sprites) * 136, 280), (70, 70, 80))
    for index, (name, img) in enumerate(sprites.items()):
        check(name, img)
        img.save(TEXTURES / f"{name}.png")
        big = img.resize((128, 128), Image.NEAREST)
        x = index * 136 + 4
        sheet.paste((198, 198, 198), (x, 4, x + 128, 132))
        sheet.paste(big, (x, 4), big)
        sheet.paste((40, 44, 52), (x, 136, x + 128, 264))
        sheet.paste(big, (x, 136), big)
        sheet.paste((139, 139, 139), (x, 266, x + 18, 280))
        sheet.paste(img.crop((0, 2, 16, 16)), (x + 1, 266), img.crop((0, 2, 16, 16)))
    sheet.save(HERE / "previa_itens.png")
    print(len(sprites), "sprites validados")


if __name__ == "__main__":
    main()
