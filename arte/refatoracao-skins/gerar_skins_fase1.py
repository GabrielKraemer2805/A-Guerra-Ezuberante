"""Skins da 0.023: Bolsonaristas novos, Missao (Kim e Guto), Janones e a refatoracao do Boulos.

Mesmo metodo de gerar_skins.py: uma grade de paleta por face, sem redimensionamento.
Rode da raiz do projeto: python arte/refatoracao-skins/gerar_skins_fase1.py
"""
from pathlib import Path

from gerar_skins import ARM_LONG, ARM_TOP, HERE, LEG, LEG_CREASE, LEG_TOP, MUL, Skin, mirror, palette

TOP = ["nmmmmmmn", "mm3333mm", "nm3223mn", "nnmmmmnn"]
# Barra de manga curta com pele abaixo, usada por camisetas e polos.
ARM_SHORT = [
    "onnm nmmo mnno onno",
    "onnm nmmo mnno onno",
    "onnm nmmo mnno onno",
    "ppoo oooo oopp pooo",
    "3221 2113 1223 3223",
    "3221 2113 1223 3223",
    "4322 3223 2234 3333",
    "3221 2113 1223 3223",
    "3221 2113 1223 3223",
    "3221 2113 1223 3223",
    "3211 1223 1123 2222",
    "4333 3223 3334 3333"]
SUIT_BODY = [
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
    "pppp opoooopo pppp popoopop"]


def limbs(s, arm=ARM_LONG, leg=LEG, sole="7"):
    s.right_arm(arm, ARM_TOP, "3")
    s.left_arm(arm, ARM_TOP, "3")
    s.right_leg(leg, LEG_TOP, sole)
    s.left_leg(leg, LEG_TOP, sole)


def tarcisio():
    """Calvicie nas temporas, grisalho curto, rosto quadrado, terno cinza e gravata azul."""
    s = Skin(palette(["eeb992", "dca47e", "bb8061", "8f5c45", "e2a988"], ["2d2926", "1a1716", "4a433e"],
                     a="77706a", b="514b47", c="38332f", e="24201e", w="e0dbd0", i="2c2118", k="3e3530",
                     r="b5795f", R="864a3a", x="eef0f0", y="cdd2d5", f="2f5fa0", g="5588c9", h="1f457d",
                     m="5a6068", n="464b53", o="33373e", p="23262c", q="3d4148", s="30343a", u="25282e", v="191b20"))
    s.head(0, 0,
           front=["cbaabbac", "bb1111bb", "c211112c", "2kk12kk2", "2wi21iw2", "23231232", "32rRRr23", "44233244"],
           right=["cbbabbab", "cbabbabb", "cbbabbc2", "cbbc3222", "ccc34222", "ccc34322", "e4332223", "e4333344"],
           back=["cbbabbac", "cbabbabc", "cbbabbbc", "ccbabbcc", "ccbbabcc", "eccbbcce", "ecccccce", "43333334"],
           top=["cbabbabc", "babbabab", "bbabbabb", "babbabab", "bbabbabb", "babb11ab", "bb1111bb", "c111111c"],
           bottom=["33333333"] * 8)
    s.torso(SUIT_BODY, ["nmmmmmmn", "mmx33xmm", "nmx22xmn", "nnmmmmnn"], "o")
    limbs(s, leg=LEG_CREASE)
    return s.img


def jair_renan():
    """Jovem de pele clara, sem barba, degrade com topete, camiseta preta, corrente dourada, jeans e tenis branco."""
    s = Skin(palette(["f8d9c0", "efc6ab", "d6a68e", "b08570", "f3bfa8"], ["e8e8e8", "a8a8ae", "3c3c42"],
                     a="4a3528", b="33241b", c="221812", e="15100c", w="e6e0d4", i="2a1c12", k="3a281d",
                     r="ae7158", R="7d4436", m="35353c", n="26262c", o="1a1a20", p="101014", z="e8b93a", y="b78d24",
                     q="3f5d8c", s="314b75", u="26395d", v="1a2843"))
    s.head(0, 0,
           front=["baaaabba", "baabbaab", "cc2112cc", "3kk12kk3", "2wi21iw2", "23231232", "32rRRr23", "33222233"],
           right=["ebbaabaa", "ecbbaabb", "eecbbbbc", "eee32222", "eee34222", "eec34322", "ee432223", "e4432223"],
           back=["baaabaab", "bbabbabb", "cbbabbbc", "ccbbbbcc", "eeccccee", "eeeeeeee", "e333333e", "43333334"],
           top=["baabaaab", "aabaabaa", "abaaabaa", "baabaaba", "aabaaaba", "abaabaab", "baaabaab", "baaaabba"],
           bottom=["33333333"] * 8)
    s.torso([
        "oonn nn3223nn nnoo onnnnnno",
        "oonn nnz22znn nnoo onnnnnno",
        "oonm nmnzznmn mnoo onnmmnno",
        "ponm nmmyymmn mnop onnmmnno",
        "ponn nmmmmmmn nnop onnmnnno",
        "ponn nmmmmmno nnop onnnnnoo",
        "ponn nmmmmnmn nnop onmnnmno",
        "ponn nmmmnmmn nnop onmnnmno",
        "pono nnmmmmnn onop ooonnooo",
        "pono nnnmmnno onop oonnnnoo",
        "poop oonnnnoo poop ponnnnop",
        "pppp oooooooo pppp popoopop"], TOP, "o")
    limbs(s, arm=ARM_SHORT, leg=LEG[:10] + ["7667 6666 7667 7667", "8888 8888 8888 8888"], sole="8")
    return s.img


def eduardo():
    """Barba cheia, cabelo raspado escuro, bone vermelho sem texto, blazer azul, camisa branca e jeans."""
    s = Skin(palette(["ecb891", "daa27c", "b87d5d", "8c5a43", "e0a785"], ["3a2c22", "241a13", "57453a"],
                     a="4f3b2c", b="382a1f", c="261c14", e="170f0a", w="e2dccf", i="2a1c12", k="33251a",
                     r="9c6048", R="6e3a2a", m="33507c", n="263e63", o="1a2c49", p="111e33", x="f0f2f2", y="d0d5d8",
                     C="cc2a2a", D="a11c1f", E="7c1216", F="e2e2e2",
                     q="3f5d8c", s="314b75", u="26395d", v="1a2843"))
    s.head(0, 0,
           front=["DDCCCCDD", "cbbbbbbc", "c211112c", "2kk12kk2", "2wi21iw2", "b223122b", "cb3RR3bc", "ccbbbbcc"],
           right=["EEDDDDCC", "cbbaabbb", "cbbabbab", "cbbc3222", "ccc34222", "cbc34bbb", "e43bcbbc", "e44cbbcc"],
           back=["EDDDDDDE", "cbbaabbc", "cbabbabc", "ccbabbcc", "ccbbbbcc", "eccbbcce", "ecccccce", "43333334"],
           top=["DDDCCDDD", "DCCCCCCD", "DCCCCCCD", "DCCCCCCD", "DCCCCCCD", "DCCCCCCD", "DCCCCCCD", "DDCCCCDD"],
           bottom=["33333333"] * 8)
    # aba do bone na camada externa, sem esconder os olhos
    s.face(40, 8, 8, 8, ["DDCCCCDD", "EEDDDDEE", "........", "........", "........", "........", "........", "........"], MUL["F"])
    s.torso([
        "oonn nxx22xxn nnoo onnnnnno",
        "oonn nxy33yxn nnoo onmnnmno",
        "oonm mnxyyxnm mnoo onmnnmno",
        "ponm mnxyyxnm mnop onnmnnno",
        "ponn mmnxxnnm nnop onnnnnno",
        "ponn nmnxxnno nnop onnnnnoo",
        "ponn nmnyynno nnop onmnnnno",
        "ponn nmnooono nnop onmnnnno",
        "pono nmnyonno onop ooonoooo",
        "pono nnnoonno onop oonnonoo",
        "poop oonoonoo poop ponnonop",
        "pppp opoooopo pppp popoopop"], ["nmmmmmmn", "mmx33xmm", "nmx22xmn", "nnmmmmnn"], "o")
    arm = list(ARM_LONG)
    limbs(s, arm=arm)
    return s.img


def michelle():
    """Cabelo longo castanho-claro com luzes, vestido rosa-claro ate o joelho, sapato nude."""
    s = Skin(palette(["f7d6bd", "ebbfa4", "d2a087", "ae7c66", "f0b7a0"], ["d9b090", "b18d70", "ecc8a8"],
                     a="b08a58", b="8a6a40", c="6b4f2d", e="4d381f", w="efe6da", i="4a3424", k="7a5a3a",
                     r="d68a8a", R="b0525c", m="f6dbe0", n="eac6cf", o="d3a6b3", p="b08494", x="ebbfa4",
                     z="e8c96a", y="c9a94a", q="ebbfa4", s="dcac90", u="cb9b80", v="b08068"))
    side = ["cbaabaab", "cbabaaab", "cbabbaab", "cbbabbab", "cbbabaab", "ccbabbac", "ccbbabbc", "eccbbbcc"]
    back = ["cbaabaac", "cbabbabc", "cbabbabc", "cbbabbac", "ccbabbac", "ccbabbcc", "ecbbabce", "ecbbbbce"]
    top = ["cbaabaac", "baabaaba", "abbaabba", "baa4aaab", "aba4baab", "baa4aaab", "abb4abba", "cba4baac"]
    s.head(0, 0,
           front=["cbaa4aac", "baa12aab", "ba1112ab", "bkk12kkb", "cwi21iwc", "c523125c", "c3rRRr3c", "e322223e"],
           right=side, back=back, top=top, bottom=["c3333333"] * 8)
    s.head(32, 0,
           front=["cb....bc", "c......c", "c......c", "c......c", "c......c", "c......c", "c......c", "e......e"],
           right=side, back=back, top=top)
    s.torso([
        "oonn nn3223nn nnoo onmmmmno",
        "oonn nmz22zmn nnoo onmmmmno",
        "oonn nmnyynmn nnoo onmmmmno",
        "oonn nmmmmmmo nnoo onmmmmno",
        "oonn nmmmmmmo nnoo onmmmmno",
        "oonn nmmmmmno nnoo onmmmmno",
        "oonn nmmmmnmo nnoo onmmmmno",
        "oonn nmmmnmmo nnoo onmmmmno",
        "oonn nnmmmmno nnoo onnmmnno",
        "oonn nnnmmnno nnoo onnnnnno",
        "oono onnnnnoo oono oonnnnoo",
        "pppp opoooopo pppp popoopop"], TOP, "o")
    s.strip(16, 32, 8, 4, 12, [
        "ccbb cb....bc bbcc cbbaabbc",
        "ccbb cb....bc bbcc cbabbabc",
        "...b cb....bc b... cbabbabc",
        ".... c......c .... cbbabbac",
        ".... ........ .... ccbabbcc",
        ".... ........ .... .ecbbce.",
        ".... ........ .... ........",
        ".... ........ .... ........",
        ".... ........ .... ........",
        ".... ........ .... ........",
        ".... ........ .... ........",
        ".... ........ .... ........"])
    # saia do vestido ate o joelho, pele e sapato abaixo; manga curta com pele
    skirt = [
        "oonm nmmo mnno onno",
        "oonm nmmo mnno onno",
        "oonm nmmo mnno onno",
        "ppoo oooo oopp pooo",
        "3221 2112 1223 3223",
        "3221 2112 1223 3223",
        "3221 2112 1223 3223",
        "3221 2112 1223 3223",
        "3221 2112 1223 3223",
        "4322 3223 2234 3333",
        "7667 6886 7667 7667",
        "7777 6666 7777 7777"]
    limbs(s, arm=ARM_SHORT, leg=skirt)
    return s.img


def campopiano():
    """Jovem, dentes da frente aparentes, cabelo castanho longo e reto, blazer preto, blusa branca, cruz dourada."""
    s = Skin(palette(["f6d2b6", "e9bb9f", "cf9c81", "aa7963", "eeb39b"], ["1f1c20", "111014", "3c3840"],
                     a="5e4332", b="402d21", c="2c1f17", e="1b130e", w="f0e7db", i="33241a", k="4a3427",
                     r="c86d72", R="a14650", t="f3ecdf", T="d8cdbc", m="3c3c44", n="2b2b32", o="1d1d23", p="121216", x="f4f4f4", y="d6d8da",
                     z="e8c34a", q="2d2d36", s="23232b", u="191920", v="101014"))
    side = ["cbbaabbb", "cbabbaab", "cbabbabb", "cbbabbab", "ccbabbab", "ccbabbac", "ccbbabbc", "eccbbbcc"]
    back = ["cbbaabbc", "cbabbabc", "cbabbabc", "cbbabbac", "ccbabbac", "ccbabbcc", "ecbbabce", "ecbbbbce"]
    top = ["cbbaabbc", "bbabbabb", "babbabab", "baa4baab", "aba4bbab", "baa4baab", "abb4abba", "cba4bbbc"]
    s.head(0, 0,
           front=["cbaa4aac", "bab11bab", "bb1112bb", "bkk12kkb", "cwi21iwc", "c223122c", "c3RttR3c", "e32TT23e"],
           right=side, back=back, top=top, bottom=["c3333333"] * 8)
    s.head(32, 0,
           front=["cb....bc", "c......c", "c......c", "c......c", "c......c", "c......c", "c......c", "e......e"],
           right=side, back=back, top=top)
    s.torso([
        "oonn nnx22xnn nnoo onnnnnno",
        "oonn mnxx3xnm nnoo onmnnmno",
        "oonm mnxzzxnm mnoo onmnnmno",
        "ponm mnxzxxnm mnop onnmnnno",
        "ponn mnxxxxnn nnop onnnnnno",
        "ponn nnxxxxno nnop onnnnnoo",
        "ponn nnnxxnnn nnop onnnnnno",
        "ponn nmnnonno nnop onmnnnno",
        "pono nmnnonno onop ooonnooo",
        "pono nnnoonoo onop oonnnnoo",
        "poop oonnnnoo poop ponnnnop",
        "pppp opoooopo pppp popoopop"], TOP, "o")
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
    limbs(s)
    return s.img


def nikolas():
    """Muito jovem, franja penteada de lado, sem barba, terno azul-escuro e gravata verde."""
    s = Skin(palette(["f2c6a4", "e3b08d", "c28b6d", "966349", "e8b295"], ["2e2a28", "1b1817", "4a433f"],
                     a="5a4231", b="3e2d21", c="2a1e16", e="19120d", w="ece4d8", i="2b1d13", k="4a3527",
                     r="c17a68", R="8f5040", x="f2f4f4", y="d4d8da", f="2e8b47", g="4fb568", h="1f6b34",
                     m="2b3f6b", n="1f2f54", o="15213d", p="0d152a", q="20304f", s="182540", u="111b31", v="0b1222"))
    s.head(0, 0,
           front=["baaaaabb", "aabbaa1b", "bbab1112", "2kk12kk2", "2wi21iw2", "23231232", "32rRRr23", "33222233"],
           right=["cbbaabaa", "cbabbaab", "cbbabbbb", "ccbb3222", "ccb34222", "ccc34322", "e4332223", "e4332223"],
           back=["cbbaabbc", "cbabbabc", "cbbabbbc", "ccbabbcc", "ccbbbbcc", "eccbbcce", "e3cccc3e", "43333334"],
           top=["cbbaabbc", "babbabab", "bbabbabb", "babaabab", "aabaabaa", "baabaaba", "aabaaaba", "baaaaabb"],
           bottom=["33333333"] * 8)
    s.torso(SUIT_BODY, ["nmmmmmmn", "mmx33xmm", "nmx22xmn", "nnmmmmnn"], "o")
    limbs(s, leg=LEG_CREASE)
    return s.img


def pavanato():
    """Cabelo castanho curto com lateral baixa, sem barba, camisa polo azul-marinho e calca bege."""
    s = Skin(palette(["efbd9a", "dea884", "bc8365", "90604a", "e4ad8d"], ["56402e", "3a2a1e", "75593f"],
                     a="7a5a3c", b="5b412a", c="3e2c1c", e="271b11", w="e8e0d2", i="2d1f14", k="4a3423",
                     r="b5795f", R="82483a", m="2e4a7a", n="223a63", o="172a4b", p="0f1d36", x="f0f2f2", y="d2d7da",
                     q="c8b28c", s="b19c76", u="978463", v="7a6a4e"))
    s.head(0, 0,
           front=["bbaaaabb", "baabbaab", "bc1112cb", "3kk12kk3", "2wi21iw2", "23231232", "32rRRr23", "33222233"],
           right=["cbbaabaa", "ccbbaabb", "cccbbbbc", "ccc32222", "ccc34222", "ccc34322", "e4332223", "e4333334"],
           back=["bbaabaab", "babbabab", "cbbabbbc", "ccbbbbcc", "ccccc cc c".replace(" ", ""), "eccccc ce".replace(" ", ""), "e333333e", "43333334"],
           top=["bbaabaab", "aabaabaa", "abaaabaa", "baabaaba", "aabaaaba", "abaabaab", "baaabaab", "bbaaaabb"],
           bottom=["33333333"] * 8)
    s.torso([
        "oonn nx3223xn nnoo onnxxnno",
        "oonn nnx22xnn nnoo onmnnmno",
        "oonm nmnyynmn mnoo onnmmnno",
        "ponm nmmyymmn mnop onnmmnno",
        "ponn nmmmmmmn nnop onnmnnno",
        "ponn nmmmmmno nnop onnnnnoo",
        "ponn nmmmmnmn nnop onmnnmno",
        "ponn nmmmnmmn nnop onmnnmno",
        "pono nnmmmmnn onop ooonnooo",
        "pono nnnmmnno onop oonnnnoo",
        "poop oonnnnoo poop ponnnnop",
        "pppp oooooooo pppp popoopop"], TOP, "o")
    limbs(s, arm=ARM_SHORT, leg=LEG_CREASE)
    return s.img


def kim():
    """Cabelo preto liso repartido, sem barba, terno preto do Missao, gravata amarela."""
    s = Skin(palette(["eec4a0", "dcae8a", "bb8a6b", "8e6249", "e2b093"], ["232026", "131216", "3f3b44"],
                     a="2e2a30", b="1b181d", c="110f13", e="09080a", w="e6dfd2", i="1a1310", k="2a2220",
                     r="b57a66", R="86503f", x="f2f4f4", y="d4d8da", f="f0c21a", g="ffdf55", h="b8920f",
                     m="34343c", n="25252c", o="191920", p="0f0f14", q="26262e", s="1d1d24", u="15151b", v="0e0e12"))
    s.head(0, 0,
           front=["bbbaaabb", "baab1aab", "ba11112b", "2kk12kk2", "2wi21iw2", "23231232", "32rRRr23", "33222233"],
           right=["cbbaabaa", "cbabbaab", "cbbabbbb", "ccbb3222", "ccb34222", "ccc34322", "e4332223", "e4332223"],
           back=["cbbaabbc", "cbabbabc", "cbbabbbc", "ccbabbcc", "ccbbbbcc", "eccbbcce", "e3cccc3e", "43333334"],
           top=["cbbaabbc", "babbabab", "bbabbabb", "bab4abab", "aab4aaba", "bab4abba", "aab4abba", "bbb4aabb"],
           bottom=["33333333"] * 8)
    s.torso(SUIT_BODY, ["nmmmmmmn", "mmx33xmm", "nmx22xmn", "nnmmmmnn"], "o")
    limbs(s, leg=LEG_CREASE)
    return s.img


def guto():
    """Homem negro, barba curta, cabelo preto curto, camisa branca de manga dobrada, broche amarelo do Missao, calca preta."""
    s = Skin(palette(["8f5f40", "784c31", "5e3a25", "442819", "835538"], ["2a262a", "161418", "47424a"],
                     a="2f2622", b="1f1815", c="130f0d", e="0a0706", w="e8e2d6", i="120c09", k="191210",
                     r="66392a", R="3f2018", m="fbfbf8", n="eceeea", o="d2d5d0", p="b0b4ae", x="fbfbf8", z="f0c21a", y="b8920f",
                     q="2c2c34", s="222229", u="18181e", v="0f0f13"))
    s.head(0, 0,
           front=["baaabbab", "bbabbaab", "cc2112cc", "2kk12kk2", "2wi21iw2", "b323123b", "cb3RR3bc", "ccbbbbcc"],
           right=["cbbaabab", "cbabbabb", "cbbabbcb", "ccbc3222", "ccc34222", "ccb34b22", "e43bcbbc", "e44cbbcc"],
           back=["cbbabbac", "cbabbabc", "cbbabbbc", "ccbabbcc", "ccbbbbcc", "eccbbcce", "ecccccce", "43333334"],
           top=["cbabbabc", "babbabab", "bbabbabb", "babaabab", "aabaabaa", "baabbaba", "aabaabba", "baaabbab"],
           bottom=["33333333"] * 8)
    s.torso([
        "oonn nm2222mo nnoo onmoomno",
        "oonn nmo33omo nnoo onmmmmno",
        "oonn nmmooozo nnoo onmmmmno",
        "oonn nmmommmo nnoo onmmmmno",
        "oonn nmmommmo nnoo onmmmmno",
        "oonn nmmomnmo nnoo onmmmmno",
        "oonn nmmommmo nnoo onmmmmno",
        "oonn nmmommmo nnoo onmmmmno",
        "oonn nnmomnno nnoo onnmmnno",
        "oonn nnnonnno nnoo onnnnnno",
        "ssss ssssssss ssss ssssssss",
        "uuuu uuuuuuuu uuuu uuuuuuuu"], ["nmmmmmmn", "mm3333mm", "nm3223mn", "nnmmmmnn"], "u")
    rolled = [
        "onnm nmmo mnno onno",
        "onnm nmmo mnno onno",
        "onnm nmmo mnno onno",
        "onnm nmmo mnno onno",
        "ppoo oooo oopp pooo",
        "xxxx xxxx xxxx xxxx",  # manga dobrada
        "3221 2113 1223 3223",
        "4322 3223 2234 3333",
        "3221 2113 1223 3223",
        "3221 2113 1223 3223",
        "3211 1223 1123 2222",
        "4333 3223 3334 3333"]
    limbs(s, arm=rolled, leg=LEG_CREASE)
    return s.img


def janones():
    """Cabelo escuro curto penteado para tras, cavanhaque, camisa polo vermelha com estrela, jeans."""
    s = Skin(palette(["e8b18c", "d69c78", "b47a5b", "875640", "dca285"], ["e4e4e4", "a6a6ac", "3c3c42"],
                     a="3a2c24", b="271c15", c="1a120d", e="0f0a07", w="e6dfd2", i="20140d", k="2c1f16",
                     r="a86a54", R="6e3a2b", m="dc2c26", n="c11d18", o="9b100f", p="6f0809", z="f7dd4a", y="c9a820",
                     q="3f5d8c", s="314b75", u="26395d", v="1a2843"))
    s.head(0, 0,
           front=["bbaaaabb", "baabbaab", "cc2112cc", "3kk12kk3", "2wi21iw2", "23231232", "22bRRb22", "23cbbc32"],
           right=["bbbaaaaa", "bbbaabaa", "cbbbaabc", "ccbc3222", "ccc34222", "ccc34322", "e4332223", "e4333322"],
           back=["baaabaab", "bbaabaab", "cbbaabbc", "cbbbabbc", "ccbbbbcc", "eccbbcce", "ecccccce", "43333334"],
           top=["baaabaab", "aabaabaa", "abaaabaa", "baabaaba", "aabaaaba", "abaabaab", "baaabaab", "bbaaaabb"],
           bottom=["33333333"] * 8)
    s.torso([
        "oonn nn3223nn nnoo onnoonno",
        "oonn nno22onn nnoo onmnnmno",
        "oonm nmmooomn mnoo onnmmnno",
        "ponm nmmmmzmn mnop onnmmnno",
        "ponn nmmmzzzn nnop onnmnnno",
        "ponn nmmmmzmo nnop onnnnnoo",
        "ponn nmmmmnmn nnop onmnnmno",
        "ponn nmmmnmmn nnop onmnnmno",
        "pono nnmmmmnn onop ooonnooo",
        "pono nnnmmnno onop oonnnnoo",
        "poop oonnnnoo poop ponnnnop",
        "pppp oooooooo pppp popoopop"], TOP, "o")
    limbs(s, arm=ARM_SHORT, leg=LEG[:10] + ["7667 6666 7667 7667", "8888 8888 8888 8888"], sole="8")
    return s.img


def boulos():
    """Refatoracao: mantem barba escura, camiseta vermelha e calca do original, com rosto e sombras novos."""
    s = Skin(palette(["ffc9a8", "f2b292", "d4927a", "a86c58", "f4b39a"], ["3a3a3e", "212124", "57575c"],
                     a="3d2b20", b="271a12", c="17100b", e="0c0806", w="ece6da", i="261509", k="1f140d",
                     r="b5604f", R="7c3c33", m="c91e24", n="b31419", o="991115", p="6d0b0f",
                     q="3f4a60", s="323c50", u="262e40", v="1a2130"))
    s.head(0, 0,
           front=["cbaaabbc", "cb1111bc", "22111122", "2kk12kk2", "2wi21iw2", "b223122b", "bb3RR3bb", "ccbbbbcc"],
           right=["cbbaabab", "cbabbabb", "cbbabbc2", "ccbc3222", "ccc34222", "cbb34bb2", "e4bbcbbb", "e4ccbbcc"],
           back=["cbbabbac", "cbabbabc", "cbbabbbc", "ccbabbcc", "ccbbbbcc", "eccbbcce", "ecccccce", "43333334"],
           top=["cbabbabc", "babbabab", "bbabbabb", "babaabab", "aabaabaa", "baabbaba", "aabaabba", "cbaaabbc"],
           bottom=["cccc4444"] * 8)
    s.torso([
        "oonn oo2222oo nnoo onnoonno",
        "oonn noo22oon nnoo onmnnmno",
        "oonm nmmoommn mnoo onnmmnno",
        "ponm nmmoommn mnop onnmmnno",
        "ponn nmmoommn nnop onnmnnno",
        "ponn nmmmmmno nnop onnnnnoo",
        "ponn nmmmmnmn nnop onmnnmno",
        "ponn nmmmnmmn nnop onmnnmno",
        "pono nnmmmmnn onop ooonnooo",
        "pono nnnmmnno onop oonnnnoo",
        "poop oonnnnoo poop ponnnnop",
        "pppp oooooooo pppp popoopop"], TOP, "o")
    limbs(s, arm=ARM_SHORT)
    return s.img


SKINS = {"tarcisio_freitas": tarcisio, "jair_renan": jair_renan, "eduardo_bolsonaro": eduardo, "michelle_bolsonaro": michelle,
         "eduarda_campopiano": campopiano, "nikolas_ferreira": nikolas, "lucas_pavanato": pavanato,
         "kim_kataguiri": kim, "guto_zacarias": guto, "andre_janones": janones, "guilherme_boulos": boulos}

if __name__ == "__main__":
    for name, fn in SKINS.items():
        folder = HERE / name
        folder.mkdir(exist_ok=True)
        fn().save(folder / f"{name}_skin_64x64.png")
    print(len(SKINS), "skins geradas")
