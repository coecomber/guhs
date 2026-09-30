"""
3.0 (Guhverhalen), balto_slee - the textures (all painted here, nothing hand-made):

  entity/baltoslee_slee.png          the Nomguh sled: warm wood, iron shoes, a red plaid blanket, golden bells, a lantern, the
                                     white medicine chest with a pink heart, a little pink guh head on the brush bow
  entity/baltoslee_slee_steele.png   Steele-Mika's: dark wood, a purple blanket, silver bells, a grinning little Mika head
  entity/sneeuwslee.png              your own: light birch, a pink blanket with light-blue stripes and white hearts
  entity/baltoslee_sledehondje.png   a guh-sledehondje: grey-and-white husky fur, a guh face with blushes, a red harness
  entity/baltoslee_sledehondje_steele.png   Steele-Mika's dogs: dark fur, a purple harness, a cheeky little grin
  entity/baltoslee_touw.png          the ropes
  block/baltoslee_*.png              the winter deco
  item/sneeuwslee.png, item/sledebelletje.png, particle/baltoslee_snuffel_0..2.png
"""
import numpy as np
from PIL import Image

from features import balto_slee_modellen as modellen

SW = modellen.SW


def _rng(seed):
    return np.random.default_rng(seed)


def vlak(kleur, rng, var=8, size=SW):
    a = np.zeros((size, size, 4), np.float32)
    a[..., :3] = kleur
    a[..., :3] += rng.normal(0, var / 2, (size, size, 1))
    a[..., 3] = 255
    return a


def hout(kleur, rng, size=SW):
    a = vlak(kleur, rng, 6, size)
    for y in range(size):
        if rng.random() < 0.35:
            a[y, :, :3] *= 0.86                                  # the grain
    for _ in range(max(1, size // 8)):
        x, y = rng.integers(0, size, 2)
        a[y, x, :3] *= 0.7                                       # a knot
    return a


def plaid(basis, streep, rng, size=SW, hartjes=None):
    a = vlak(basis, rng, 5, size)
    for i in range(size):
        if i % 4 == 1:
            a[i, :, :3] = a[i, :, :3] * 0.4 + np.array(streep) * 0.6
            a[:, i, :3] = a[:, i, :3] * 0.4 + np.array(streep) * 0.6
    if hartjes:
        for y, x in ((2, 2), (5, 6)) if size == SW else ((3, 3), (11, 9), (6, 12)):
            a[y, x, :3] = hartjes
            a[y, x + 1, :3] = hartjes
            a[y + 1, x:x + 2, :3] = hartjes
    return a


def glans(kleur, rng, size=SW):
    a = vlak(kleur, rng, 4, size)
    for y in range(size):
        for x in range(size):
            d = ((x - size * 0.3) ** 2 + (y - size * 0.3) ** 2) ** 0.5
            a[y, x, :3] *= 1.18 - 0.05 * d
    return a


def gezicht_guh(huid, rng, grijns=False):
    """A tiny guh face (8x8): eyes, blushes, a little mouth (or a cheeky grin)."""
    a = vlak(huid, rng, 4)
    for x in (2, 5):
        a[3:5, x, :3] = (30, 24, 34)
        a[3, x, :3] = (250, 250, 255)
    a[5, 1, :3] = (255, 140, 170)
    a[5, 6, :3] = (255, 140, 170)
    if grijns:
        a[6, 2:6, :3] = (40, 30, 40)
        a[5, 5, :3] = (40, 30, 40)
    else:
        a[6, 3, :3] = (120, 60, 70)
        a[6, 4, :3] = (120, 60, 70)
    return a


def kist(rng, hart=True):
    a = vlak((244, 244, 240), rng, 3)
    a[0, :, :3] = a[-1, :, :3] = (200, 200, 205)
    a[:, 0, :3] = a[:, -1, :3] = (200, 200, 205)
    if hart:
        for y, xs in ((2, (2, 3, 5, 6)), (3, range(2, 7)), (4, range(2, 7)), (5, range(3, 6)), (6, (4,))):
            for x in xs:
                a[y, x, :3] = (246, 120, 170)
        a[2, 2, :3] = (255, 190, 215)
    return a


# =====================================================================================================================
# the sleds
# =====================================================================================================================
SLEDEN = {
    "baltoslee_slee": {"hout": (172, 120, 72), "hout_donker": (116, 76, 44), "ijzer": (150, 166, 188), "deken": (206, 52, 64),
                       "streep": (250, 236, 220), "rand": (246, 236, 220), "goud": (240, 192, 60), "glas": (255, 214, 110),
                       "frame": (62, 56, 60), "riem": (122, 78, 46), "guhkop": (246, 176, 196), "lint": (214, 40, 56), "grijns": False},
    "baltoslee_slee_steele": {"hout": (96, 72, 62), "hout_donker": (58, 44, 40), "ijzer": (122, 122, 138), "deken": (118, 68, 160),
                              "streep": (40, 32, 48), "rand": (70, 58, 84), "goud": (206, 210, 222), "glas": (196, 160, 255),
                              "frame": (40, 36, 44), "riem": (70, 50, 40), "guhkop": (150, 144, 158), "lint": (128, 70, 176), "grijns": True},
    "sneeuwslee": {"hout": (224, 198, 152), "hout_donker": (172, 140, 100), "ijzer": (172, 204, 232), "deken": (246, 150, 190),
                   "streep": (168, 214, 246), "rand": (168, 214, 246), "goud": (240, 192, 60), "glas": (255, 222, 130),
                   "frame": (90, 84, 96), "riem": (150, 100, 70), "guhkop": (246, 176, 196), "lint": (120, 190, 240), "grijns": False,
                   "hartjes": (255, 250, 252)},
}


def slee(naam, p, seed):
    rng = _rng(seed)
    img = np.zeros((64, 64, 4), np.float32)
    sw = modellen.SLEE_SW
    delen = {
        "hout": hout(p["hout"], rng), "hout_donker": hout(p["hout_donker"], rng), "ijzer": glans(p["ijzer"], rng),
        "deken": plaid(p["deken"], p["streep"], rng, hartjes=p.get("hartjes")), "deken_rand": plaid(p["rand"], p["deken"], rng),
        "goud": glans(p["goud"], rng), "glas": glans(p["glas"], rng), "frame": vlak(p["frame"], rng, 4),
        "kist": kist(rng, False), "kist_hart": kist(rng), "riem": vlak(p["riem"], rng, 5), "guhkop": vlak(p["guhkop"], rng, 4),
        "guhgezicht": gezicht_guh(p["guhkop"], rng, p["grijns"]), "oor": vlak(tuple(c * 0.85 for c in p["guhkop"]), rng, 4),
        "lint": vlak(p["lint"], rng, 5), "deksel": kist(rng, False)}
    # the medicine chest's lid gets a little cross-stitch strap
    delen["deksel"][3:5, :, :3] = p["riem"]
    for k, (u, v) in sw.items():
        img[v:v + SW, u:u + SW] = delen[k]
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))


# =====================================================================================================================
# the guh-sledehondjes
# =====================================================================================================================
HONDEN = {
    "baltoslee_sledehondje": {"vacht": (152, 158, 174), "rug": (112, 118, 136), "buik": (242, 242, 246), "tuig": (214, 48, 60),
                              "gesp": (240, 196, 70), "oor_binnen": (246, 170, 190), "grijns": False},
    "baltoslee_sledehondje_steele": {"vacht": (74, 72, 86), "rug": (48, 46, 58), "buik": (160, 158, 170), "tuig": (130, 70, 172),
                                     "gesp": (206, 210, 222), "oor_binnen": (200, 140, 170), "grijns": True},
}


def hond(p, seed):
    rng = _rng(seed)
    img = np.zeros((32, 32, 4), np.float32)
    sw = modellen.HOND_SW
    v, b = p["vacht"], p["buik"]

    def gezicht():
        a = vlak(v, rng, 4)
        a[4:, :, :3] = b                                          # the husky mask: white below
        a[2:4, 3:5, :3] = b                                       # and between the eyes
        a[0:2, 3:5, :3] = p["rug"]
        for x in (1, 6):
            a[3:5, x, :3] = (26, 22, 30)
            a[3, x, :3] = (255, 255, 255)
        a[5, 0, :3] = (255, 150, 176)
        a[5, 7, :3] = (255, 150, 176)
        if p["grijns"]:
            a[2, 0:2, :3] = (26, 22, 30)                          # (cheeky eyebrows)
            a[2, 6:8, :3] = (26, 22, 30)
        return a

    def neus():
        a = vlak(b, rng, 3)
        a[1:3, 3:5, :3] = (30, 26, 34)
        a[1, 3, :3] = (90, 90, 100)
        a[4, 2:6, :3] = (150, 90, 100)
        if p["grijns"]:
            a[5, 5, :3] = (150, 90, 100)
        return a

    def tuig():
        a = vlak(p["tuig"], rng, 5)
        a[3:5, 3:5, :3] = p["gesp"]
        return a

    def staart():
        a = vlak(v, rng, 6)
        a[:2, :, :3] = b
        return a

    delen = {"vacht": vlak(v, rng, 10), "buik": vlak(b, rng, 4), "gezicht": gezicht(), "snoet": vlak(b, rng, 3), "oor": vlak(p["rug"], rng, 5),
             "tuig": tuig(), "tong": vlak((244, 120, 150), rng, 4), "poot": vlak(tuple((np.array(v) + np.array(b)) / 2), rng, 6),
             "goud": glans((240, 196, 70), rng), "staart": staart(), "rug": vlak(p["rug"], rng, 8), "neus": neus(),
             "oor_binnen": vlak(p["oor_binnen"], rng, 4), "masker": vlak(b, rng, 3), "wit": vlak(b, rng, 2), "leeg": vlak(b, rng, 2)}
    for k, (u, vv) in sw.items():
        img[vv:vv + SW, u:u + SW] = delen[k]
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))


def touw(rng):
    a = np.zeros((16, 16, 4), np.float32)
    for y in range(16):
        for x in range(16):
            twist = (x + y * 2) % 6 < 3
            a[y, x, :3] = (170, 120, 70) if twist else (130, 84, 48)
            a[y, x, 3] = 255
    a[..., :3] += rng.normal(0, 4, (16, 16, 1))
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


# =====================================================================================================================
# the deco's block textures
# =====================================================================================================================
def blokken(h):
    rng = _rng(20300801)
    B = 16

    def save(name, a):
        h.save(Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)), "block", name + ".png")

    sneeuw = vlak((242, 246, 255), rng, 6, B)
    for _ in range(14):
        x, y = rng.integers(0, B, 2)
        sneeuw[y, x, :3] = (214, 226, 246)
    save("baltoslee_sneeuw", sneeuw)
    g = sneeuw.copy()
    for x in (4, 10):                                            # coal eyes with a sparkle
        g[5:8, x:x + 2, :3] = (34, 32, 40)
        g[5, x, :3] = (250, 250, 255)
    g[9:11, 2:4, :3] = (255, 170, 196)                           # blushes
    g[9:11, 12:14, :3] = (255, 170, 196)
    for x in range(5, 11):                                       # a coal-dot smile
        if x % 2 == 1:
            g[12 if x in (5, 11) else 13, x, :3] = (34, 32, 40)
    save("baltoslee_sneeuwguh_gezicht", g)
    wortel = vlak((244, 140, 40), rng, 8, B)
    for y in range(0, B, 4):
        wortel[y, :, :3] *= 0.8
    save("baltoslee_wortel", wortel)
    save("baltoslee_sjaal", plaid((214, 40, 56), (250, 236, 220), rng, B))
    save("baltoslee_hout", hout((172, 120, 72), rng, B))
    save("baltoslee_ijzer", glans((150, 166, 188), rng, B))
    save("baltoslee_deken", plaid((206, 52, 64), (250, 236, 220), rng, B))
    save("baltoslee_deken_rand", plaid((246, 236, 220), (206, 52, 64), rng, B))
    lint = vlak((214, 40, 56), rng, 6, B)
    lint[7:9, :, :3] = (250, 200, 210)
    save("baltoslee_lint", lint)
    save("baltoslee_goud", glans((240, 192, 60), rng, B))
    bord = glans((240, 192, 60), rng, B)
    # the cup's plaque: a little sled with a bell, engraved
    for x in range(3, 13):
        bord[10, x, :3] = (150, 100, 30)
    for x in (3, 12):
        bord[8:10, x, :3] = (150, 100, 30)
    bord[6:8, 5:11, :3] = (150, 100, 30)
    bord[3:5, 7:9, :3] = (255, 240, 170)
    save("baltoslee_beker_bord", bord)
    mand = vlak((196, 150, 90), rng, 6, B)
    for y in range(B):
        for x in range(B):
            if (x // 2 + y // 2) % 2 == 0:
                mand[y, x, :3] *= 0.82                           # the wicker weave
    save("baltoslee_mand", mand)
    save("baltoslee_kussen", plaid((120, 170, 230), (250, 250, 255), rng, B, hartjes=(255, 170, 200)))
    knabbel = vlak((250, 204, 70), rng, 6, B)
    for _ in range(6):
        x, y = rng.integers(1, B - 2, 2)
        knabbel[y:y + 2, x:x + 2, :3] = (214, 160, 40)
    save("baltoslee_knabbel", knabbel)
    glas = glans((255, 220, 120), rng, B)
    glas[0, :, :3] = glas[-1, :, :3] = (62, 56, 60)
    glas[:, 0, :3] = glas[:, -1, :3] = (62, 56, 60)
    glas[1:-1, 1:-1, :3] = np.clip(glas[1:-1, 1:-1, :3] * 1.05 + 10, 0, 255)
    glas[5:11, 6:10, :3] = (255, 246, 200)                       # the little flame's glow
    save("baltoslee_glas", glas)


# =====================================================================================================================
# items and the particle
# =====================================================================================================================
BELLETJE = ["................", "......rr.rr.....", ".....rRRrRRr....", "......rrrrr.....", ".......ggg......", "......gGGGg.....",
            ".....gGGWGGg....", ".....gGGWGGg....", "....gGGGGGGGg...", "....gGGGGGGGg...", "...gGGGGGGGGGg..", "...ddddddddddd..",
            ".......dkd......", "........k.......", "................", "................"]
SLEE_ICOON = ["................", "................", ".............yy.", "............yggy", "d...........yggy", "dd...........yy.",
              ".d..pppPpppPp.w.", ".d.pPppppPpppPw.", ".dhhhhhhhhhhhhw.", ".d.h..h..h..h.w.", "iii.i..i..i..iii", ".iiiiiiiiiiiiiii",
              "................", "................", "................", "................"]


def items(h):
    h.save(h.grid(BELLETJE, {"r": (214, 40, 56, 255), "R": (250, 110, 120, 255), "g": (176, 126, 30, 255), "G": (240, 194, 64, 255),
                             "W": (255, 246, 200, 255), "d": (150, 104, 26, 255), "k": (70, 50, 30, 255)}), "item", "sledebelletje.png")
    h.save(h.grid(SLEE_ICOON, {"d": (172, 140, 100, 255), "p": (246, 150, 190, 255), "P": (255, 250, 252, 255), "h": (224, 198, 152, 255),
                               "i": (150, 186, 222, 255), "w": (172, 140, 100, 255), "y": (176, 126, 30, 255), "g": (240, 194, 64, 255)}),
           "item", "sneeuwslee.png")
    for i in range(3):
        a = np.zeros((8, 8, 4), np.uint8)
        kleur = [(255, 236, 150), (190, 230, 255), (255, 250, 230)][i]
        r = 3 - (i == 2)
        for k in range(-r, r + 1):
            a[4, 4 + k] = (*kleur, 255 if abs(k) < r else 150)
            a[4 + k, 4] = (*kleur, 255 if abs(k) < r else 150)
        a[3:6, 3:6] = (*kleur, 255)
        a[4, 4] = (255, 255, 255, 255)
        h.save(Image.fromarray(a), "particle", f"baltoslee_snuffel_{i}.png")


def build(h):
    for i, (naam, p) in enumerate(SLEDEN.items()):
        h.save(slee(naam, p, 20300810 + i), "entity", naam + ".png")
    for i, (naam, p) in enumerate(HONDEN.items()):
        h.save(hond(p, 20300801 + i), "entity", naam + ".png")
    h.save(touw(_rng(20300802)), "entity", "baltoslee_touw.png")
    blokken(h)
    items(h)
