"""
einde2, slice bouw_schelpenstad - the eight decorative blocks of the road trip: textures (painted here, 16 x 16 pixel art),
block models, blockstates, item models, loot, recipes and tags.

  schelpsteen     shell stone: the cream stone full of little shells that Schelpenstad is built of
  drooglamp       the drying lamp of the souvenir shop (hangs, light 15)
  souvenir        a dried souvenir on a little stand; block state soort 0-3 = a star, a block of gatenkaas, a kwal, a fish guh
                  (flat as a pancake, with googly eyes: dried is dried)
  kroonkussen     the velvet cushion on a foot of mother-of-pearl where the king's crown lies
  stopcontact     the wall socket the drying lamp is plugged into
  sproeier        a sprinkler on the ceiling
  neon            the neon sign of De Ruige Kroeg: an anchor with Mika ears (light 10)
  kaartenrek      a rack of postcards

build(h) writes everything; the Java side is feature/einde2/bouwschelpenstad/SchelpenstadBlokken + E2BouwSchelpenstadFeature.
"""
import math
import os
import random

from PIL import Image

from features import einde2_kit as kit

P = "einde2_bouw_schelpenstad_"
BLOKKEN = ("schelpsteen", "drooglamp", "souvenir", "kroonkussen", "stopcontact", "sproeier", "neon", "kaartenrek")
NAMEN = {
    "schelpsteen": ("Schelpsteen", "Steen vol schelpjes. Heel Schelpenstad is ervan gebouwd."),
    "drooglamp": ("Droogtelamp", "Droogt alles tot souvenir. Niet onder gaan liggen, njeg."),
    "souvenir": ("Gedroogd souvenir", "Plat, stijf en met wiebeloogjes. Niet natmaken!"),
    "kroonkussen": ("Kroonkussen", "Hier hoort een kroon op. Een echte, van een koning."),
    "stopcontact": ("Stopcontact", "Zit een droogtelamp in. Niet op huilen."),
    "sproeier": ("Sproeier", "Gaat aan bij rook. En dan wordt alles nat. Alles."),
    "neon": ("Neonbord van De Ruige Kroeg", "Een anker met Mika-oren. Zoemt een beetje."),
    "kaartenrek": ("Ansichtkaartenrek", "Groetjes uit Schelpenstad! Het weer is droog."),
}
CREME, CREME_D, ROZE, WIT = (236, 224, 200), (212, 196, 168), (240, 176, 184), (252, 248, 240)
STAAL, STAAL_D, ROOD, GOUD = (150, 154, 166), (92, 96, 110), (190, 44, 52), (244, 200, 72)


def _img(size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def _rng(naam):
    return random.Random(kit.seed("bouw_schelpenstad/blok/" + naam))


def _vul(img, x0, y0, x1, y1, c):
    for y in range(max(0, y0), min(img.height, y1)):
        for x in range(max(0, x0), min(img.width, x1)):
            img.putpixel((x, y), tuple(c) + (255,) if len(c) == 3 else tuple(c))


def _ruis(img, rng, var=6):
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a:
                v = rng.randint(-var, var)
                px[x, y] = (max(0, min(255, r + v)), max(0, min(255, g + v)), max(0, min(255, b + v)), a)
    return img


def _raster(rows, pal, img=None):
    img = img or _img(len(rows[0]))
    assert len(rows) == img.height and all(len(row) == img.width for row in rows), [len(row) for row in rows]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                c = pal[ch]
                img.putpixel((x, y), tuple(c) + (255,) if len(c) == 3 else tuple(c))
    return img


# =====================================================================================================================
# textures
# =====================================================================================================================
def schelpsteen(rng):
    """Cream stone with shells in it: three little scallops and a few spirals."""
    img = _img()
    _vul(img, 0, 0, 16, 16, CREME)
    for _ in range(26):                                    # sandy grains
        img.putpixel((rng.randint(0, 15), rng.randint(0, 15)), CREME_D + (255,))
    for (cx, cy, kleur) in ((3, 4, ROZE), (11, 3, WIT), (7, 11, ROZE), (13, 12, WIT)):
        for (dx, dy) in ((0, 0), (-1, -1), (0, -1), (1, -1), (-2, -2), (-1, -2), (0, -2), (1, -2), (2, -2), (-1, -3), (0, -3), (1, -3)):
            x, y = (cx + dx) % 16, (cy + dy) % 16
            rib = dx % 2 == 0
            c = kleur if rib else tuple(int(v * 0.88) for v in kleur)
            img.putpixel((x, y), c + (255,))
        img.putpixel((cx % 16, (cy + 1) % 16), tuple(int(v * 0.8) for v in kleur) + (255,))       # the hinge
    for (x, y) in ((1, 10), (9, 7), (5, 14), (14, 7)):       # tiny spirals
        img.putpixel((x, y), WIT + (255,))
        img.putpixel(((x + 1) % 16, y), (226, 168, 150, 255))
    return _ruis(img, rng, 5)


def lampkap(rng):
    img = _img()
    _vul(img, 0, 0, 16, 16, STAAL_D)
    _vul(img, 0, 0, 16, 2, STAAL)
    _vul(img, 0, 13, 16, 16, ROOD)
    for x in range(2, 16, 4):
        _vul(img, x, 3, x + 1, 12, (70, 74, 88))
    return _ruis(img, rng, 5)


def gloed(rng):
    img = _img()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5) / 10.0
            img.putpixel((x, y), (255, int(236 - 110 * d), int(150 - 130 * min(1, d)), 255))
    return _ruis(img, rng, 4)


def snoer():
    img = _img()
    _vul(img, 0, 0, 16, 16, (40, 40, 48))
    return img


def houten_voetje(rng):
    img = _img()
    _vul(img, 0, 0, 16, 16, (176, 132, 84))
    for y in (4, 9, 14):
        _vul(img, 0, y, 16, y + 1, (150, 108, 66))
    return _ruis(img, rng, 5)


OOG = {"W": (255, 255, 255), "K": (20, 20, 28)}
SOUVENIRS = [
    # 0: a dried star (pink, five points, two googly eyes)
    (["................", ".......aa.......", ".......aa.......", "......abba......", "......abba......", ".aaaaabbbbaaaaa.",
      "..abbbWWbWWbbba.", "...abbWKbWKbba..", "....abbbbbbba...", "....abbbccbba...", "...abbbbbbbbba..", "...abbbaabbbba..",
      "..abbba..abbba..", "..abba....abba..", "..aa........aa..", "................"],
     {"a": (196, 110, 118), "b": (236, 150, 160), "c": (150, 70, 80), **OOG}),
    # 1: a dried block of gatenkaas (square, holes, eyes; somebody we know?)
    (["................", "..aaaaaaaaaaaa..", "..abbbbbbbbbba..", "..abhbbbbbbhba..", "..abbWWbbWWbba..", "..abbWKbbWKbba..",
      "..abbbbbbbbbba..", "..ahbbbccbbbba..", "..abbbbbbbbhba..", "..abbhbbbbbbba..", "..abbbbbbhbbba..", "..aaaaaaaaaaaa..",
      "...dddddddddd...", "...deeddddeed...", "....d......d....", "................"],
     {"a": (196, 150, 40), "b": (240, 204, 80), "h": (190, 140, 36), "c": (140, 80, 40), "d": (130, 84, 50), "e": (240, 240, 236), **OOG}),
    # 2: a dried kwal (a dome with stiff tentacles)
    (["................", ".....aaaaaa.....", "...aabbbbbbaa...", "..abbbbbbbbbba..", "..abbWWbbWWbba..", ".abbbWKbbWKbbba.",
      ".abbbbbbbbbbbba.", ".abbcbbbbbbcbba.", ".aaaaaaaaaaaaaa.", "..a.a..aa..a.a..", "..a.a..aa..a.a..", "..a..a.a..a..a..",
      ".a...a.a..a...a.", ".a....a..a....a.", "......a..a......", "................"],
     {"a": (150, 110, 190), "b": (206, 170, 236), "c": (236, 120, 180), **OOG}),
    # 3: a dried fish guh (fins, a tail fin, guh ears)
    (["................", "...aa......aa...", "..abba....abba..", "..abbaaaaaabba..", "..abbbbbbbbbba.a", ".abbWWbbbWWbbbaa",
      ".abbWKbbbWKbbbba", ".abbbbbccbbbbbaa", ".abbbbbbbbbbba.a", "..abbbbbbbbba...", "..aabbbbbbbaa...", ".aa.aabbbaa.aa..",
      ".a....aaa....a..", "................", "................", "................"],
     {"a": (60, 130, 150), "b": (120, 196, 204), "c": (236, 130, 150), **OOG}),
]


def souvenir(i):
    rows, pal = SOUVENIRS[i]
    return _raster(rows, pal)


def souvenir_rand(i):
    """The thin edge of a souvenir: its darkest colour."""
    img = _img()
    _vul(img, 0, 0, 16, 16, SOUVENIRS[i][1]["a"])
    return img


def fluweel(rng):
    """The top of the cushion: red velvet, a golden border, a dent in the middle where the crown goes."""
    img = _img()
    _vul(img, 0, 0, 16, 16, ROOD)
    for i in range(16):
        for (x, y) in ((i, 1), (i, 14), (1, i), (14, i)):
            img.putpixel((x, y), GOUD + (255,))
    for y in range(16):
        for x in range(16):
            if math.hypot(x - 7.5, y - 7.5) < 3.4:
                img.putpixel((x, y), (150, 30, 40, 255))
    for (x, y) in ((0, 0), (15, 0), (0, 15), (15, 15)):
        img.putpixel((x, y), GOUD + (255,))
    return _ruis(img, rng, 5)


def fluweel_zij(rng):
    img = _img()
    _vul(img, 0, 0, 16, 16, ROOD)
    _vul(img, 0, 6, 16, 7, GOUD)
    _vul(img, 0, 9, 16, 10, (150, 30, 40))
    for x in (0, 15):
        _vul(img, x, 5, x + 1, 11, GOUD)
    return _ruis(img, rng, 5)


def parelvoet(rng):
    img = _img()
    _vul(img, 0, 0, 16, 16, (232, 236, 244))
    _vul(img, 0, 0, 16, 1, (255, 255, 255))
    _vul(img, 0, 15, 16, 16, (196, 204, 220))
    for _ in range(10):
        img.putpixel((rng.randint(0, 15), rng.randint(1, 14)), (240, 214, 232, 255))
    return _ruis(img, rng, 4)


def contact():
    """The socket: a white plate with two holes and a little spark of worry."""
    rows = ["wwwwwwwwwwwwwwww"] * 16
    img = _raster(rows, {"w": (240, 240, 236)})
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel((x, y), (190, 190, 188, 255))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 4.4 < d <= 5.6:
                img.putpixel((x, y), (206, 206, 204, 255))
    _vul(img, 5, 6, 7, 9, (40, 40, 48))
    _vul(img, 9, 6, 11, 9, (40, 40, 48))
    _vul(img, 7, 11, 9, 12, (120, 120, 124))
    return img


def metaal(rng, rood=False):
    img = _img()
    _vul(img, 0, 0, 16, 16, ROOD if rood else STAAL)
    _vul(img, 0, 0, 16, 2, (220, 90, 96) if rood else (214, 218, 228))
    _vul(img, 0, 14, 16, 16, (140, 30, 38) if rood else STAAL_D)
    return _ruis(img, rng, 5)


def neon():
    """The neon of De Ruige Kroeg on a dark board: a pink anchor with two cyan Mika ears on its ring."""
    rows = ["dddddddddddddddd", "dddccddddddccddd", "ddcccdppppdcccdd", "dddcdpdddddpdcdd", "dddddpdddddpdddd", "ddddddpppppddddd",
            "ddddpppppppppddd", "dddddddpppdddddd", "dddddddpppdddddd", "dpdddddpppdddddp", "dppddddpppddddpp", "ddppdddpppdddppd",
            "dddpppppppppppdd", "dddddpppppppdddd", "dddddddddddddddd", "dddddddddddddddd"]
    return _raster(rows, {"d": (26, 18, 40), "p": (255, 110, 200), "c": (110, 240, 255)})


def bord_rand():
    img = _img()
    _vul(img, 0, 0, 16, 16, (26, 18, 40))
    _vul(img, 0, 0, 16, 1, (60, 46, 84))
    return img


def kaarten(rng):
    """One side of the postcard rack: three rows of little cards (sea, sand, a shell, a guh, a lighthouse...)."""
    img = _img()
    _vul(img, 0, 0, 16, 16, (70, 74, 88))
    kaart = [((120, 196, 236), (236, 214, 150)), ((250, 170, 196), (255, 255, 255)), ((150, 110, 190), (255, 220, 120)),
             ((236, 214, 150), (190, 44, 52)), ((110, 200, 190), (255, 255, 255)), ((255, 220, 120), (236, 150, 160))]
    for rij, y in enumerate((1, 6, 11)):
        for kol, x in enumerate((1, 6, 11)):
            lucht, ding = kaart[(rij * 3 + kol + rng.randint(0, 5)) % len(kaart)]
            _vul(img, x, y, x + 4, y + 4, lucht)
            _vul(img, x, y + 3, x + 4, y + 4, ding)
            img.putpixel((x + 1 + rng.randint(0, 1), y + 1), ding + (255,))
            img.putpixel((x, y), (255, 255, 255, 255))
    return img


def textures(h):
    s = h.save
    s(schelpsteen(_rng("schelpsteen")), "block", P + "schelpsteen.png")
    s(lampkap(_rng("kap")), "block", P + "drooglamp.png")
    s(gloed(_rng("gloed")), "block", P + "drooglamp_gloed.png")
    s(snoer(), "block", P + "drooglamp_snoer.png")
    s(houten_voetje(_rng("voetje")), "block", P + "souvenir_voet.png")
    for i in range(4):
        s(souvenir(i), "block", f"{P}souvenir_{i}.png")
        s(souvenir_rand(i), "block", f"{P}souvenir_{i}_rand.png")
    s(souvenir(0), "item", P + "souvenir.png")
    s(fluweel(_rng("fluweel")), "block", P + "kroonkussen.png")
    s(fluweel_zij(_rng("fluweel_zij")), "block", P + "kroonkussen_zij.png")
    s(parelvoet(_rng("voet")), "block", P + "kroonkussen_voet.png")
    s(contact(), "block", P + "stopcontact.png")
    s(metaal(_rng("staal")), "block", P + "sproeier.png")
    s(metaal(_rng("rood"), rood=True), "block", P + "sproeier_kop.png")
    s(neon(), "block", P + "neon.png")
    s(bord_rand(), "block", P + "neon_rand.png")
    s(kaarten(_rng("kaarten")), "block", P + "kaartenrek.png")
    s(metaal(_rng("rek")), "block", P + "kaartenrek_paal.png")


# =====================================================================================================================
# models
# =====================================================================================================================
ALLE = ("north", "south", "east", "west", "up", "down")


def el(frm, to, faces):
    """A model element; faces: {side: texture key} or {side: (texture key, [u0, v0, u1, v1])}."""
    out = {}
    for side, f in faces.items():
        key, uv = (f, None) if isinstance(f, str) else f
        out[side] = {"texture": "#" + key, **({"uv": uv} if uv else {})}
    return {"from": list(frm), "to": list(to), "faces": out}


def rondom(key, **anders):
    f = {s: key for s in ALLE}
    f.update(anders)
    return f


def models(h):
    A, w = h.A, h.w
    t = lambda naam: f"guhs:block/{P}{naam}"  # noqa: E731
    h.simple_block(P + "schelpsteen")
    # --- the drying lamp: a cord, a metal shade, the glow under it
    w(f"{A}/models/block/{P}drooglamp.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {
        "particle": t("drooglamp"), "kap": t("drooglamp"), "gloed": t("drooglamp_gloed"), "snoer": t("drooglamp_snoer")}, "elements": [
        el((7.5, 12, 7.5), (8.5, 16, 8.5), rondom("snoer")),
        el((5, 10, 5), (11, 12, 11), rondom("kap", up=("kap", [0, 0, 16, 2]), down=("gloed", [4, 4, 12, 12]))),
        el((3, 7, 3), (13, 10, 13), rondom("kap", up=("kap", [0, 0, 16, 2]), down=("gloed", [0, 0, 16, 16]))),
        dict(el((5.5, 5.5, 5.5), (10.5, 7, 10.5), rondom("gloed")), shade=False)]})
    w(f"{A}/blockstates/{P}drooglamp.json", {"variants": {"": {"model": t("drooglamp")}}})
    w(f"{A}/models/item/{P}drooglamp.json", {"parent": t("drooglamp")})
    # --- the dried souvenirs: a wooden foot and a flat figure, four pictures
    for i in range(4):
        w(f"{A}/models/block/{P}souvenir_{i}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {
            "particle": t("souvenir_voet"), "voet": t("souvenir_voet"), "plaat": t(f"souvenir_{i}"), "rand": t(f"souvenir_{i}_rand")}, "elements": [
            el((4, 0, 5), (12, 1, 11), rondom("voet")),
            {"from": [1, 1, 7.5], "to": [15, 15, 8.5], "faces": {
                "north": {"texture": "#plaat", "uv": [15, 1, 1, 15]}, "south": {"texture": "#plaat", "uv": [1, 1, 15, 15]}}}]})
    w(f"{A}/blockstates/{P}souvenir.json", {"variants": {
        k.replace("facing=", f"soort={i},facing="): dict(v, model=t(f"souvenir_{i}")) for i in range(4)
        for k, v in h.facing_states(P + "souvenir").items()}})
    h.item_model(P + "souvenir")
    # --- the crown cushion
    w(f"{A}/models/block/{P}kroonkussen.json", {"parent": "minecraft:block/block", "textures": {
        "particle": t("kroonkussen"), "boven": t("kroonkussen"), "zij": t("kroonkussen_zij"), "voet": t("kroonkussen_voet")}, "elements": [
        el((4, 0, 4), (12, 5, 12), rondom("voet")),
        el((3, 5, 3), (13, 6, 13), rondom("voet")),
        el((2, 6, 2), (14, 10, 14), rondom("zij", up="boven", down="voet"))]})
    w(f"{A}/blockstates/{P}kroonkussen.json", {"variants": {"": {"model": t("kroonkussen")}}})
    w(f"{A}/models/item/{P}kroonkussen.json", {"parent": t("kroonkussen")})
    # --- the socket: a plate on the wall behind it (north = its front)
    w(f"{A}/models/block/{P}stopcontact.json", {"parent": "minecraft:block/block", "textures": {
        "particle": t("stopcontact"), "plaat": t("stopcontact")}, "elements": [
        el((5, 5, 15), (11, 11, 16), rondom(("plaat", [0, 0, 16, 1]), north=("plaat", [0, 0, 16, 16])))]})
    w(f"{A}/blockstates/{P}stopcontact.json", {"variants": h.facing_states(P + "stopcontact")})
    w(f"{A}/models/item/{P}stopcontact.json", {"parent": t("stopcontact"), "display": {
        "gui": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1.6, 1.6, 1.6]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, -7], "scale": [1.4, 1.4, 1.4]}}})
    # --- the sprinkler under the ceiling
    w(f"{A}/models/block/{P}sproeier.json", {"parent": "minecraft:block/block", "textures": {
        "particle": t("sproeier"), "staal": t("sproeier"), "kop": t("sproeier_kop")}, "elements": [
        el((7, 12, 7), (9, 16, 9), rondom("staal")),
        el((6, 10, 6), (10, 12, 10), rondom("kop")),
        el((5, 9, 5), (11, 9.5, 11), rondom("staal")),
        el((7.5, 9.5, 7.5), (8.5, 10, 8.5), rondom("staal"))]})
    w(f"{A}/blockstates/{P}sproeier.json", {"variants": {"": {"model": t("sproeier")}}})
    w(f"{A}/models/item/{P}sproeier.json", {"parent": t("sproeier")})
    # --- the neon sign: a board on the wall behind it
    w(f"{A}/models/block/{P}neon.json", {"parent": "minecraft:block/block", "textures": {
        "particle": t("neon_rand"), "beeld": t("neon"), "rand": t("neon_rand")}, "elements": [
        dict(el((0, 1, 15), (16, 15, 16), rondom("rand", north=("beeld", [0, 1, 16, 15]))), shade=False)]})
    w(f"{A}/blockstates/{P}neon.json", {"variants": h.facing_states(P + "neon")})
    w(f"{A}/models/item/{P}neon.json", {"parent": "minecraft:item/generated", "textures": {"layer0": t("neon")}})
    # --- the postcard rack
    w(f"{A}/models/block/{P}kaartenrek.json", {"parent": "minecraft:block/block", "textures": {
        "particle": t("kaartenrek_paal"), "kaart": t("kaartenrek"), "paal": t("kaartenrek_paal")}, "elements": [
        el((5, 0, 5), (11, 1, 11), rondom("paal")),
        el((7.5, 1, 7.5), (8.5, 16, 8.5), rondom("paal")),
        el((4.5, 3, 4.5), (11.5, 14, 11.5), rondom(("kaart", [0, 0, 16, 16]), up="paal", down="paal"))]})
    w(f"{A}/blockstates/{P}kaartenrek.json", {"variants": {"": {"model": t("kaartenrek")}}})
    w(f"{A}/models/item/{P}kaartenrek.json", {"parent": t("kaartenrek")})


# =====================================================================================================================
# loot, recipes, tags, names
# =====================================================================================================================
def data(h):
    for blok in BLOKKEN:
        h.self_drop(P + blok)
    g = "guhs:" + P
    h.shaped(P + "schelpsteen", ["PZ", "ZP"], {"P": "guhs:parelmoer", "Z": "guhs:kruimelzand"}, g + "schelpsteen", 4)
    h.shaped(P + "drooglamp", [" K ", "ILI"], {"K": "minecraft:chain", "I": "minecraft:iron_ingot", "L": "guhs:lichtstaafje"}, g + "drooglamp")
    h.shaped(P + "souvenir", ["K", "P"], {"K": "guhs:kaas_knabbels", "P": "#minecraft:wooden_slabs"}, g + "souvenir", 2)
    h.shaped(P + "kroonkussen", ["WGW", " P "], {"W": "minecraft:red_wool", "G": "minecraft:gold_nugget", "P": "guhs:parelmoer"}, g + "kroonkussen")
    h.shaped(P + "stopcontact", ["I", "R"], {"I": "minecraft:iron_nugget", "R": "minecraft:redstone"}, g + "stopcontact", 2)
    h.shaped(P + "sproeier", ["I", "E"], {"I": "minecraft:iron_ingot", "E": "minecraft:water_bucket"}, g + "sproeier", 2)
    h.shaped(P + "neon", ["GLG", "RRR"], {"G": "minecraft:glass_pane", "L": "guhs:lichtstaafje", "R": "minecraft:pink_dye"}, g + "neon", 2)
    h.shaped(P + "kaartenrek", ["PPP", " I ", " I "], {"P": "minecraft:paper", "I": "minecraft:iron_nugget"}, g + "kaartenrek")
    add = h.add_tag
    add("minecraft/tags/block/mineable/pickaxe", [g + n for n in ("schelpsteen", "drooglamp", "stopcontact", "sproeier", "neon", "kaartenrek")])
    add("minecraft/tags/block/mineable/axe", [g + "souvenir"])
    for blok, (naam, lore) in NAMEN.items():
        kit.lang(h, f"block.guhs.{P}{blok}", naam)
        kit.lang(h, f"block.guhs.{P}{blok}.lore", lore)


def build(h):
    textures(h)
    models(h)
    data(h)


def check(h):
    problems = []
    for blok in BLOKKEN:
        for pad in (f"{h.A}/blockstates/{P}{blok}.json", f"{h.D}/loot_table/blocks/{P}{blok}.json", f"{h.A}/models/item/{P}{blok}.json",
                    f"{h.D}/recipe/{P}{blok}.json"):
            if not os.path.exists(pad):
                problems.append(f"missing {pad}")
        if f"block.guhs.{P}{blok}" not in h.NL:
            problems.append(f"no name for {blok}")
    return problems
