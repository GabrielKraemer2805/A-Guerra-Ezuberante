"""Skins refatoradas de Marina Silva, Tabata Amaral, Haddad, Jones Manoel e Dilma.

Desenho pixel a pixel, deterministico: cada letra e um indice de paleta e cada grade e uma face do atlas Steve 64x64.
Nao ha redimensionamento, desfoque nem foto. Rode da raiz do projeto: python arte/refatoracao-skins/gerar_skins.py
Coluna 0 de uma face de frente = lado direito do personagem (esquerda de quem olha).
Faixas de membro: R (x0 = costas) F L (x0 = frente) B, nessa ordem, contiguas como no atlas.
"""
from pathlib import Path
from PIL import Image

HERE = Path(__file__).resolve().parent
# Luz vinda de cima/frente/esquerda do personagem: multiplicador por face, alem das letras.
MUL = {"top": 1.05, "bottom": .72, "R": .90, "F": 1.0, "L": .97, "B": .90}


def hexrgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def norm(rows):
    return [r.replace(" ", "") for r in rows]


def mirror(rows):
    return [r[::-1] for r in norm(rows)]


def mirror_limb(rows):
    out = []
    for r in norm(rows):
        R, F, L, B = r[:4], r[4:8], r[8:12], r[12:16]
        out.append(L[::-1] + F[::-1] + R[::-1] + B[::-1])
    return out


class Skin:
    def __init__(self, pal):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.pal = {k: hexrgb(v) for k, v in pal.items()}

    def face(self, x, y, w, h, rows, mul=1.0):
        rows = norm(rows)
        assert len(rows) == h, (x, y, len(rows), h)
        for j, row in enumerate(rows):
            assert len(row) == w, (x, y, j, row, w)
            for i, ch in enumerate(row):
                if ch == ".":
                    continue
                c = self.pal[ch]
                self.img.putpixel((x + i, y + j), (*[min(255, round(v * mul)) for v in c], 255))

    def fill(self, x, y, w, h, ch, mul=1.0):
        self.face(x, y, w, h, [ch * w] * h, mul)

    def strip(self, u, v, w, d, h, rows):
        """Faces laterais de uma caixa a partir da faixa R F L B; (u,v) e a origem classica da caixa."""
        rows = norm(rows)
        assert len(rows) == h, (u, v, len(rows))
        for j, r in enumerate(rows):
            assert len(r) == 2 * (w + d), (u, v, j, r)
        y = v + d
        for name, a, n in (("R", 0, d), ("F", d, w), ("L", d + w, d), ("B", d + w + d, w)):
            self.face(u + a, y, n, h, [r[a:a + n] for r in rows], MUL[name])

    def head(self, u, v, front, right, back, top, bottom=None, left=None):
        self.face(u + 8, v + 8, 8, 8, front, MUL["F"])
        self.face(u, v + 8, 8, 8, right, MUL["R"])
        self.face(u + 16, v + 8, 8, 8, left or mirror(right), MUL["L"])
        self.face(u + 24, v + 8, 8, 8, back, MUL["B"])
        self.face(u + 8, v, 8, 8, top, MUL["top"])
        if bottom:
            self.face(u + 16, v, 8, 8, bottom, MUL["bottom"])

    def limb(self, u, v, rows, top, bottom):
        self.strip(u, v, 4, 4, 12, rows)
        self.face(u + 4, v, 4, 4, top, MUL["top"])
        self.fill(u + 8, v, 4, 4, bottom, MUL["bottom"])

    def torso(self, rows, top, bottom):
        self.strip(16, 16, 8, 4, 12, rows)
        self.face(20, 16, 8, 4, top, MUL["top"])
        self.fill(28, 16, 8, 4, bottom, MUL["bottom"])

    def right_arm(self, rows, top, bottom):
        self.limb(40, 16, rows, top, bottom)

    def left_arm(self, rows, top, bottom):
        self.limb(32, 48, mirror_limb(rows), mirror(top), bottom)

    def right_leg(self, rows, top, bottom):
        self.limb(0, 16, rows, top, bottom)

    def left_leg(self, rows, top, bottom):
        self.limb(16, 48, mirror_limb(rows), mirror(top), bottom)


# Moldes de membro. m n o p = tecido do claro ao fundo; x = punho; 1 2 3 4 = pele.
ARM_LONG = [
    "onnm nmmo mnno onno",  # coluna 3 da frente: sombra junto ao tronco
    "onnm nmmo mnno onno",
    "onnm nmmo mnno onno",
    "onnm nmmo mnno onno",
    "onnn nmmo nnno onno",
    "oonn nnno nnoo oono",  # dobra do cotovelo
    "onnm nmmo mnno onno",
    "onnm nmmo mnno onno",
    "onnn nmno nnno onno",
    "pxxx xxxx xxxp xxxx",  # punho
    "3221 2112 1223 3223",  # mao
    "4333 3223 3334 3333",  # dedos
]
ARM_TOP = ["mmnn", "mmnn", "nnno", "nnoo"]
LEG = [
    "vuus sqqs suuv vuuv",
    "vuus sqqs suuv vuuv",
    "vuus sqqs suuv vuuv",
    "vuus sqqs suuv vusv",
    "vuus sqqs suuv vusv",
    "vuus qqqs suuv vusv",  # joelho
    "vuus sqqs suuv vusv",
    "vuus sqqs suuv vuuv",
    "vuus sqqs suuv vuuv",
    "vvuu usuu uuvv vvuv",  # barra
    "7667 6886 7667 7667",  # sapato
    "7777 6666 7777 7777",  # sola
]
LEG_CREASE = [  # vinco de calca social
    "vuus sqsu suuv vuuv",
    "vuus sqsu suuv usuv",
    "vuus sqsu suuv usuv",
    "vuus sqsu suuv usuv",
    "vuus sqsu suuv uusv",
    "vuus qqsu suuv uusv",
    "vuus sqsu suuv usuv",
    "vuss sqsu ssuv usuv",
    "vuus sqsu suuv usuv",
    "vvuu usuu uuvv vuuv",
    "7667 6886 7667 7667",
    "7777 6666 7777 7777",
]
LEG_TOP = ["ssss", "ssus", "suus", "uuuu"]


def palette(skin5, shoes3=None, **rest):
    pal = dict(zip("12345", skin5))
    pal.update(rest)
    if shoes3:
        pal.update(zip("678", shoes3))
    return pal


def marina():
    s = Skin(palette(["cf9a74", "b97f5a", "9c6546", "7a4a33", "c98a62"], ["38363a", "232226", "55535a"],
                     a="43363c", b="2c2328", c="1c171a", e="100d0f", w="d9cdbb", i="2a1a16", k="54362a",
                     r="a56a55", R="7f4437", m="fbfbf5", n="eceee6", o="d4d7d0", p="b4b8b1", x="fbfbf5",
                     z="6c747c", y="a4acb3", q="40506e", s="323f5a", u="27324a", v="1a2234"))
    s.head(0, 0,
           front=["cbbaabbc", "cbb11bbc", "cb2112bc", "3kk12kk3", "3wi21iw3", "32231223", "32rRRr23", "33222233"],
           right=["ccbbaabb", "cbbabbab", "cbabbabb", "cbbabb23", "ccbb3423", "ccbb3423", "ecc33223", "e4332223"],
           back=["cbbaabbc", "cbabbabc", "ccbaabcc", "cbbabbbc", "ecbbbbce", "eccbbcce", "4ecccce4", "43333334"],
           top=["cbbaabbc", "cbabbabc", "bbabbabb", "babcbabb", "babcbabb", "bbacbabb", "bbacbbbb", "cbb33bbc"],
           bottom=["33333333"] * 8)
    # coque baixo: so a camada externa de tras; o rosto fica livre
    s.face(56, 8, 8, 8, ["........", "........", "........", "..cbbc..", ".cbaabc.", ".cbbabc.", "..cccc..", "........"], MUL["B"])
    s.torso([
        "oonn nm2222mo nnoo onmzzmno",
        "oonn nmy22ymo nnoo onmnnmno",
        "oonn nmz21zmo nnoo onmmmmno",
        "oonn nmmzzmmo nnoo onmmmmno",
        "oonn nmmmmmmo nnoo onmmmmno",
        "oonn nmmmmmno nnoo onmmmmno",
        "oonn nmmmmnmo nnoo onmmmmno",
        "oonn nmmmnmmo nnoo onmmmmno",
        "oonn nnmmmmno nnoo onnmmnno",
        "oonn nnnmmnno nnoo onnnnnno",
        "oono onnnnnoo oono oonnnnoo",
        "pppp opoooopo pppp popoopop"], ["nmmmmmmn", "mm3333mm", "nm3223mn", "nnmmmmnn"], "o")
    s.right_arm(ARM_LONG, ARM_TOP, "3")
    s.left_arm(ARM_LONG, ARM_TOP, "3")
    s.right_leg(LEG, LEG_TOP, "7")
    s.left_leg(LEG, LEG_TOP, "7")
    return s.img


def tabata():
    s = Skin(palette(["f6d3b9", "e8bba2", "cf9c83", "ac7a65", "efb49c"], ["2a2630", "16131a", "4a4452"],
                     a="6a5144", b="46372f", c="30251f", e="1e1714", w="efe6db", i="3b2a1f", k="5e4336",
                     r="dc9f9c", R="b05e68", x="f0f2f2", y="d2d7da", f="3f73b8", g="6f9fd6", h="2c5692",
                     m="34507f", n="253b61", o="1a2b48", p="111d33", q="2c3a52", s="232e44", u="1a2335", v="111725"))
    side = ["cbbaabbb", "cbabbaab", "cbabbabb", "cbbabbab", "ccbabbab", "ccbabbac", "ccbbabbc", "eccbbbcc"]
    back = ["cbbaabbc", "cbabbabc", "cbabbabc", "cbbabbac", "ccbabbac", "ccbabbcc", "ecbbabce", "ecbbbbce"]
    top = ["cbbaabbc", "bbabbabb", "babbabab", "babcbaab", "babcbaab", "babcbabb", "bbacbabb", "cbacbbbc"]
    s.head(0, 0,
           front=["cbaaabbc", "cabb12bc", "bb2112bb", "bkk22kkb", "cwi21iwc", "c523125c", "c3rRRr3c", "e322223e"],
           right=side, back=back, top=top, bottom=["c3333333"] * 8)
    # volume dos cabelos na camada externa, com a frente livre
    s.head(32, 0,
           front=["cb....bc", "c......c", "c......c", "c......c", "c......c", "c......c", "c......c", "e......e"],
           right=side, back=back, top=top)
    s.torso([
        "oonn nn2222nn nnoo onnnnnno",
        "oonn mnx33xnm nnoo onmnnmno",
        "oonm mxggffxm mnoo onmnnmno",
        "ponm mxgfgfxm mnop onnmnnno",
        "ponn mxgfgfxn nnop onnnnnno",
        "ponn mnxgfxnn nnop onnnnnoo",
        "ponn nnxfhxnn nnop onnnnnno",
        "ponn nmnxynno nnop onmnnnno",
        "pono nmngnnno onop ooonnooo",
        "pono nnnnnnoo onop oonnnnoo",
        "poop oonnnnoo poop ponnnnop",
        "pppp opoooopo pppp popoopop"], ["nmmmmmmn", "mm3333mm", "nm3223mn", "nnmmmmnn"], "o")
    # cabelo comprido caindo sobre os ombros e as costas: camada externa do tronco
    s.strip(16, 32, 8, 4, 12, [
        "ccbb cb....bc bbcc cbbaabbc",
        "ccbb cb....bc bbcc cbabbabc",
        "...b cb....bc b... cbabbabc",
        ".... c......c .... cbbabbac",
        ".... c......c .... ccbabbcc",
        ".... ........ .... ecbabbce",
        ".... ........ .... .ecbbce.",
        ".... ........ .... ........",
        ".... ........ .... ........",
        ".... ........ .... ........",
        ".... ........ .... ........",
        ".... ........ .... ........"])
    s.right_arm(ARM_LONG, ARM_TOP, "3")
    s.left_arm(ARM_LONG, ARM_TOP, "3")
    s.right_leg(LEG, LEG_TOP, "7")
    s.left_leg(LEG, LEG_TOP, "7")
    return s.img


def haddad():
    s = Skin(palette(["ecbd9b", "daa682", "b87f62", "8c5a45", "e0a98a"], ["2e2a28", "1b1817", "4a433f"],
                     a="7e7e7c", b="5c5b5b", c="3f4145", e="2a2b2f", w="ddd9cf", i="2e2520", k="46413e",
                     r="b5795f", R="7d4437", x="f0f2f2", y="cdd2d5", f="2f67ad", g="5a93d6", h="1f4a82",
                     m="2c4268", n="1f3354", o="15243e", p="0d172b", q="24354f", s="1b2940", u="141f33", v="0d1524"))
    s.head(0, 0,
           front=["cbaabaab", "babbabbc", "bc2112cb", "2kk22kk2", "2wi21iw2", "23231232", "32rRRr23", "33233233"],
           right=["cbbabbab", "cbabaabb", "cbbabbcb", "cbbc3222", "ccc34222", "ccc34322", "e4332223", "e4333334"],
           back=["cbbabbac", "cbabaabc", "cbbabbbc", "ccbabbcc", "ccbbabcc", "eccbbcce", "ecccccce", "43333334"],
           top=["cbabbabc", "babaabab", "abbabbaa", "bababbab", "bacbabab", "bacbabba", "bacabbab", "cbcabaac"],
           bottom=["33333333"] * 8)
    s.torso([
        "oonn nnx22xnn nnoo onnnnnno",
        "oonn mnxffxnm nnoo onmnnmno",
        "oonm mmxgfxmm mnoo onmnnmno",
        "ponm mnxgfxnm mnop onnmnnno",
        "ponn mnnghnnm nnop onnnnnno",
        "ponn nnngfnno nnop onnnnnoo",
        "ponn nnnnfnno nnop onmnnnno",
        "ponn nmnnonno nnop onmnnnno",
        "pono nmnyonno onop ooonoooo",
        "pono nnnoonno onop oonnonoo",
        "poop oonoonoo poop ponnonop",
        "pppp opoooopo pppp popoopop"], ["nmmmmmmn", "mmx33xmm", "nmx22xmn", "nnmmmmnn"], "o")
    s.right_arm(ARM_LONG, ARM_TOP, "3")
    s.left_arm(ARM_LONG, ARM_TOP, "3")
    s.right_leg(LEG_CREASE, LEG_TOP, "7")
    s.left_leg(LEG_CREASE, LEG_TOP, "7")
    return s.img


def jones():
    s = Skin(palette(["bb8159", "a66c48", "855437", "5f3a26", "b07650"], ["e4e4e4", "a6a6ac", "3c3c42"],
                     a="3d322c", b="272019", c="17120f", e="0b0807", w="e6e0d6", i="140e0b", k="2a1a12",
                     r="7a4a38", R="4e2c20", G="8c9186", J="33363b", m="e0281f", n="c41a14", o="9c0f0c", p="700808",
                     y="f5d232", z="c9a21f", q="4e4ec0", s="3d3da6", u="2e2e86", v="212163"))
    s.head(0, 0,
           front=["cbaabbac", "cbbcbabc", "c221122c", "2kk12kk2", "2wi21iw2", "c223122c", "cb3RR3bc", "ccbbbbcc"],
           right=["cbacbabc", "bcabcbac", "cbbacbbc", "bacbcabc", "cbcb34cc", "cbcc34bc", "ec444cbc", "e4444cbc"],
           back=["cbacbabc", "bcabcbac", "cbbacbbc", "bacbcabc", "cbcbacbc", "ccbcbcce", "ecccccce", "43333334"],
           top=["cbacbabc", "bcabcbac", "cbbacbbc", "bacbcabc", "cbcbacbc", "bacbcabc", "cbbacbbc", "bcabcbac"],
           bottom=["cccc4444"] * 8)
    # oculos e volume do cabelo crespo na camada externa; lentes transparentes deixam os olhos a mostra
    s.head(32, 0,
           front=["cbaabbac", "cb....bc", "........", "JJJGGJJJ", "J......J", "........", "........", "........"],
           right=["cbaabcbc", "bcabcbac", "cbb.cbb.", "...JJJJJ", "........", "........", "........", "........"],
           left=["cbcbaabc", "cabcbacb", ".bbc.bbc", "JJJJJ...", "........", "........", "........", "........"],
           back=["cbaabcbc", "bcabcbac", "cbbacbbc", "bacbcabc", "cb.ba.bc", "........", "........", "........"],
           top=["cbaabcbc", "bcabcbac", "cbbacbbc", "bacbcabc", "cbcbacbc", "bacbcabc", "cbbacbbc", "bcabcbac"])
    s.torso([
        "oonn no3223on nnoo onmoomno",
        "oonn nnoooonn nnoo onmnnmno",
        "oonm nmmmmmmn mnoo onnmmnno",
        "ponm nmmmyymn mnop onnmmnno",
        "ponn nmymmyyn nnop onnmnnno",
        "ponn nyyymmyn nnop onnnnnoo",
        "ponn nmyyymyn nnop onmnnmno",
        "ponn nmmyyyyn nnop onmnnmno",
        "pono nmymmyzn onop ooonnooo",
        "pono nymmmmmn onop oonnnnoo",
        "poop nnnnnnoo poop ponnnnop",
        "pppp oooooopo pppp popoopop"], ["nmmmmmmn", "mm3333mm", "nm3223mn", "nnmmmmnn"], "s")
    arm = [
        "onnm nmmn mnno onno",
        "onnm nmmn mnno onno",
        "onmm mmmn mnno oonn",
        "ppoo oooo oopp pooo",
        "3221 2112 1223 3223",
        "3221 2112 1223 3223",
        "4322 3223 2234 3333",
        "3221 2112 1223 3223",
        "3221 2112 1223 3223",
        "3221 2122 1223 3223",
        "3211 1221 1123 2222",
        "4333 3223 3334 3333"]
    s.right_arm(arm, ARM_TOP, "3")
    s.left_arm(arm, ARM_TOP, "3")
    leg = LEG[:10] + ["7667 6666 7667 7667", "8888 8888 8888 8888"]
    s.right_leg(leg, LEG_TOP, "8")
    s.left_leg(leg, LEG_TOP, "8")
    # barra da manga em relevo, como na skin original
    s.face(40, 39, 16, 1, ["ppoo oooo oopp pooo"], .95)
    s.face(48, 55, 16, 1, ["ppoo oooo oopp pooo"], .95)
    return s.img


def dilma():
    s = Skin(palette(["f3cca9", "e6b793", "cb9274", "a87257", "e8a98c"], ["26242a", "131217", "45424c"],
                     a="987347", b="775735", c="573f27", e="3c2a1a", w="e6dfd2", i="3a2c20", k="7d5e40",
                     r="c44a48", R="a82f33", m="ea3a31", n="d02621", o="a9171a", p="7c0f12", x="ea3a31",
                     g="3da34a", h="247a31", y="f4d23c", z="c9a526", f="2b50bd", F="93b4ea",
                     q="d62a24", s="ba1e1c", u="981619", v="700e10"))
    side = ["cbaabaab", "cbabaaab", "cbabbaab", "cbbabbab", "cbbabbab", "ccbabbac", "4ccbbbcc", "44333334"]
    back = ["cbaabaac", "cbabaabc", "cbbabbac", "cbabbabc", "ccbabbcc", "ccbbabcc", "ecccccce", "43333334"]
    top = ["cbabbabc", "baabaaba", "abbaabba", "baabbaab", "abaabbab", "baabbaab", "abaabbaa", "cbaabaac"]
    s.head(0, 0,
           front=["cbaaabbc", "baabbaab", "bab2112b", "ckk12kkc", "cwi21iwc", "b523125b", "c32rr23c", "43222234"],
           right=side, back=back, top=top, bottom=["33333333"] * 8)
    # volume do corte curto: coroa e laterais na camada externa, rosto livre
    s.head(32, 0,
           front=["cbaaabbc", "b......b", "c......c", "c......c", "c......c", "b......b", "........", "........"],
           right=side[:6] + ["........", "........"], back=back[:6] + ["........", "........"], top=top)
    s.torso([
        "oonn yhn22nmn nnoo onnnnnhg",
        "oonn gyhoonmn nnoo onnnnhgy",
        "oonm hgyhmmmn mnoo onnnhgyh",
        "ponm nhgyhmmn mnop nnnhgyhn",
        "ponn nmhgyhmn nnop nnhgyhnn",
        "ponn nmmhgyhn nnop nhgyhnnn",
        "ponn nmmmhgyh hnno hgyhnnnn",
        "ponn nnmmmhgy yhno gyhnnnnn",
        "pono nmmnnfFf gyhn yhnnnnno",
        "pono nnnnnfFf hgyh hnnnnnoo",
        "poop oonnoooo ohgo oonnnnoo",
        "pppp opoooooo pppp opoooopo"], ["yhmmmmmn", "gymm2mmn", "hgn22mnn", "gynnmmnn"], "o")
    s.right_arm(ARM_LONG, ARM_TOP, "3")
    s.left_arm(ARM_LONG, ARM_TOP, "3")
    s.right_leg(LEG_CREASE, LEG_TOP, "7")
    s.left_leg(LEG_CREASE, LEG_TOP, "7")
    return s.img


SKINS = {"marina_silva": marina, "tabata_amaral": tabata, "haddad": haddad, "jones_manoel": jones, "dilma": dilma}

if __name__ == "__main__":
    for name, fn in SKINS.items():
        out = HERE / name / f"{name}_skin_64x64.png"
        fn().save(out)
        print("gerada", out.relative_to(HERE))
