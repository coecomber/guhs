"""
1.10.0 "Het Vadsrijk", round U, U-TAPIJT: the flying carpet (zone 6 Zijdeland; Java: feature/vadsrijk/tapijt).

  * the GeckoLib model vr_eco_tapijt: an old-Middle-East woven carpet (28 x 44 model px, 1 px thick) in five pieces chained front
    and back from the middle ("midden", where the rider sits and the cushion goes), so the animation can ripple it like cloth; tassels
    along both short ends; at the four corners a little guh ear stands up (pink inside);
  * animations idle (a slow ripple + sway), zweef (moving), snel (fast: the back flaps more);
  * the weaves (the VACHT axis of the mount looks): vr_eco_tapijt.png ("naturel": rose + saffron) and vr_eco_tapijt_<weefsel>.png
    (turkoois, granaat, zandroos, nacht, karavaan): a border with a zigzag, a lattice field with tiny guh faces, a medallion in the
    middle;
  * the tassel colours (the PATROON axis): vr_rijpatroon_tapijt_kwast_<kleur>.png, overlays that only paint the tassels;
  * the cushions (the TUIG axis): item models vr_rijtuig_kussen_<kleur> (a small cushion with gold corner tassels and a button);
  * the icon vr_item_zadel_tapijt (a rolled-up carpet) and every text (Dutch; en_us English).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_tapijt.py
"""
import colorsys
import json
import math
import os

import numpy as np
from PIL import Image

try:
    import vadsrijk_wezens as W
    from vadsrijk_economie import Doek
except ImportError:  # (inside make_resources: the package import)
    from features import vadsrijk_wezens as W
    from features.vadsrijk_economie import Doek

ITEM_TEX = "vadsrijk/tapijt"
kf, anim = W.kf, W.anim
PX = 2
BREED, VOOR, ACHTER = 28, -22, 22          # carpet width (x -14..14) and length (z -22..22), model px
SEGMENTEN = [("voor2", "voor1", -22, -16), ("voor1", "midden", -16, -8), ("midden", "root", -8, 8), ("achter1", "midden", 8, 16),
             ("achter2", "achter1", 16, 22)]

# the weaves: field, border, border stripe, motif, medallion, medallion ring, style
WEEFSELS = {
    "naturel": dict(veld=(224, 96, 122), rand=(240, 176, 64), streep=(120, 40, 70), motief=(255, 236, 214), med=(150, 40, 70),
                    ring=(250, 200, 90), stijl="rooster"),
    "turkoois": dict(veld=(63, 184, 176), rand=(40, 64, 140), streep=(250, 214, 110), motief=(250, 250, 240), med=(30, 90, 130),
                     ring=(255, 220, 120), stijl="rooster"),
    "granaat": dict(veld=(154, 42, 58), rand=(34, 44, 96), streep=(240, 200, 120), motief=(246, 226, 180), med=(34, 44, 96),
                    ring=(240, 200, 120), stijl="rooster"),
    "zandroos": dict(veld=(242, 198, 160), rand=(196, 110, 80), streep=(255, 240, 220), motief=(226, 120, 130), med=(196, 110, 80),
                     ring=(255, 236, 200), stijl="ruit"),
    "nacht": dict(veld=(46, 44, 106), rand=(232, 186, 70), streep=(30, 26, 70), motief=(255, 244, 190), med=(26, 24, 66),
                  ring=(232, 186, 70), stijl="sterren"),
    "karavaan": dict(veld=(200, 154, 60), rand=(120, 50, 40), streep=(250, 230, 170), motief=(120, 50, 40), med=(150, 70, 50),
                     ring=(250, 230, 170), stijl="ruit"),
}
KWASTEN = {"kwast_goud": (255, 200, 64), "kwast_roze": (255, 127, 180), "kwast_turkoois": (64, 208, 200), "kwast_wit": (248, 244, 255),
           "kwast_regenboog": None}
KUSSENS = {"kussen_roze": (255, 156, 200), "kussen_saffraan": (255, 176, 48), "kussen_turkoois": (64, 192, 192), "kussen_paars": (138, 80, 200)}
KWAST_BASIS = (255, 232, 170)   # the plain tassels in the base texture (cream gold)


# =====================================================================================================================
# the model (model px; the front is -z like every GeckoLib mob; y 0 = the carpet's underside = the entity's feet)
# =====================================================================================================================
def model_tapijt():
    m = W.Model("eco_tapijt", px=PX, breedte=64, bounds=(3, 1.5, 0.2))
    m.bone("root", None, (0, 0, 0))
    for naam, ouder, z0, z1 in SEGMENTEN:
        scharnier = z1 if z0 < -8 else z0 if z0 >= 8 else 0     # front pieces hinge on their back edge, back pieces on their front edge
        m.bone(naam, ouder, (0, 0.5, scharnier))
        m.cube(naam, "tapijt", (-BREED / 2, 0, z0), (BREED, 1, z1 - z0))
    # tassels along both short ends (they stick out flat, a knot at the carpet's edge)
    n = 8
    for i in range(n):
        x = -BREED / 2 + 1.5 + i * (BREED - 3) / (n - 1) - 0.5
        m.cube("voor2", "knoop", (x - 0.15, 0.1, VOOR - 0.9), (1.3, 0.8, 0.9))
        m.cube("voor2", "kwast", (x, 0.2, VOOR - 3.6), (1, 0.6, 2.7))
        m.cube("achter2", "knoop", (x - 0.15, 0.1, ACHTER), (1.3, 0.8, 0.9))
        m.cube("achter2", "kwast", (x, 0.2, ACHTER + 0.9), (1, 0.6, 2.7))
    # guh ears at the corners: a small upright flap turned diagonally outward, pink inside
    for bot, z, zt in (("voor2", VOOR + 1.6, -1), ("achter2", ACHTER - 1.6, 1)):
        for sx in (-1, 1):
            x = sx * (BREED / 2 - 1.6)
            hoek_y = 45 * sx * -zt
            piv = (x, 1, z)
            m.cube(bot, "oorhoek", (x - 1.75, 1, z - 0.4), (3.5, 3.2, 0.8), rot=(-12 * zt, hoek_y, 0), pivot=piv)
            m.cube(bot, "oorhoek", (x - 1.0, 4.2, z - 0.4), (2.0, 1.2, 0.8), rot=(-12 * zt, hoek_y, 0), pivot=piv)
            m.cube(bot, "oorbinnen", (x - 1.0, 1.6, z - 0.5 * zt - 0.05), (2.0, 2.6, 0.1), rot=(-12 * zt, hoek_y, 0), pivot=piv)
    return m


# =====================================================================================================================
# the weave: one big design (rows = z from the front, columns = x), sliced onto the pieces
# =====================================================================================================================
def _donker(c, f):
    return tuple(int(max(0, min(255, v * f))) for v in c)


def ontwerp(p, seed=0):
    """The carpet's top as an image (44*PX rows x 28*PX columns), RGB float."""
    H, B = (ACHTER - VOOR) * PX, BREED * PX
    a = np.zeros((H, B, 3), np.float32)
    rng = np.random.default_rng(seed)
    a[:, :] = p["veld"]
    for y in range(H):
        for x in range(B):
            r = min(x, B - 1 - x, y, H - 1 - y)     # distance to the edge (texture px)
            if r < 2:
                a[y, x] = _donker(p["rand"], 0.8)
            elif r < 7:
                a[y, x] = p["rand"]
                # a zigzag in the border band
                langs = y if min(x, B - 1 - x) < min(y, H - 1 - y) else x
                zig = abs((langs % 6) - 3)
                if r - 2 == zig + 1 or r - 2 == zig:
                    a[y, x] = p["streep"]
            elif r < 8:
                a[y, x] = p["streep"]
            else:
                a[y, x] = p["veld"]
    cx, cy = B / 2 - 0.5, H / 2 - 0.5
    # the field: a lattice of small diamonds, stars or a big lozenge pattern
    for y in range(8, H - 8):
        for x in range(8, B - 8):
            u, v = x - 8, y - 8
            if p["stijl"] == "rooster":
                du, dv = (u % 8) - 3.5, (v % 8) - 3.5
                if abs(du) + abs(dv) <= 2.0:
                    a[y, x] = p["motief"]
                elif abs(du) + abs(dv) <= 3.0 and (u + v) % 2 == 0:
                    a[y, x] = _donker(p["veld"], 0.78)
            elif p["stijl"] == "ruit":
                d = (abs(((u + 6) % 12) - 6) + abs(((v + 6) % 12) - 6))
                if d in (5, 6):
                    a[y, x] = p["motief"]
                elif d == 0 or d == 1:
                    a[y, x] = p["streep"]
    if p["stijl"] == "sterren":
        for _ in range(40):
            x, y = int(rng.integers(9, B - 9)), int(rng.integers(9, H - 9))
            a[y, x] = p["motief"]
            if rng.random() < 0.35:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    a[y + dy, x + dx] = _donker(p["motief"], 0.85)
    # the medallion in the middle (an eight-pointed lozenge with rings) and a guh face in its heart
    for y in range(H):
        for x in range(B):
            dx, dy = abs(x - cx), abs(y - cy)
            d = dx / 11 + dy / 13
            d8 = max(d, (dx + dy) / 17)
            if d8 <= 1.0:
                a[y, x] = p["ring"] if d8 > 0.86 else p["med"] if d8 > 0.4 else p["ring"] if d8 > 0.3 else p["veld"]
                if 0.55 < d8 < 0.62:
                    a[y, x] = p["motief"]
    # the guh face in the medallion (eyes, a smile, blush)
    oog = (40, 24, 40)
    ox, oy = int(cx), int(cy)
    for ex in (ox - 2, ox + 3):
        a[oy - 1:oy + 1, ex:ex + 1] = oog
    a[oy + 2, ox:ox + 2] = oog
    a[oy + 1, ox - 1] = oog
    a[oy + 1, ox + 2] = oog
    for bx in (ox - 4, ox + 5):
        a[oy + 1, bx] = (255, 140, 180)
    # a woven look: every other thread a touch darker
    a[::2, :] *= 0.95
    a[:, 1::4] *= 0.97
    return np.clip(a, 0, 255)


def _gouden_kleur(i, n):
    r, g, b = colorsys.hsv_to_rgb(i / n, 0.55, 1.0)
    return (int(r * 255), int(g * 255), int(b * 255))


def schilder(m, p, seed):
    S = m.px
    img = np.zeros((m.hoogte() * S, m.W * S, 4), np.uint8)
    d = ontwerp(p, seed)
    rng = np.random.default_rng(seed)
    for reg in m.regios:
        x, y, w, h = reg["r"]
        blok = img[y * S:(y + h) * S, x * S:(x + w) * S]
        rol, f = reg["rol"], reg["f"]
        if rol == "tapijt":
            z0 = int(reg["o"][2] - VOOR) * S
            z1 = z0 + int(reg["s"][2]) * S
            if f in ("up", "down"):
                stuk = d[z0:z1, :, :]
                if f == "down":
                    stuk = stuk * 0.72
                blok[:stuk.shape[0], :stuk.shape[1], :3] = stuk.astype(np.uint8)
                blok[..., 3] = 255
            else:
                blok[..., :3] = _donker(p["rand"], 0.75)
                blok[..., 3] = 255
        elif rol in ("kwast", "knoop"):
            k = KWAST_BASIS if rol == "kwast" else _donker(KWAST_BASIS, 0.85)
            W._vul(blok, tuple(np.array(k) * (0.9 if f in ("east", "west", "down") else 1.0)), rng, 8)
        elif rol == "oorhoek":
            W._vul(blok, p["rand"], rng, 5)
        elif rol == "oorbinnen":
            W._vul(blok, (255, 150, 186), rng, 4)
        else:
            W._vul(blok, (255, 0, 255), rng)
    return Image.fromarray(img)


def kwast_overlay(m, kleur):
    """Only the tassels (and their knots), in this colour; everything else see-through (drawn untinted over the weave)."""
    S = m.px
    img = np.zeros((m.hoogte() * S, m.W * S, 4), np.uint8)
    rng = np.random.default_rng(77)
    i = 0
    for reg in m.regios:
        if reg["rol"] not in ("kwast", "knoop"):
            continue
        x, y, w, h = reg["r"]
        blok = img[y * S:(y + h) * S, x * S:(x + w) * S]
        k = kleur if kleur is not None else _gouden_kleur((i // 12) % 8, 8)   # (rainbow: each tassel its own hue)
        if reg["rol"] == "knoop":
            k = _donker(k, 0.85)
        W._vul(blok, tuple(np.array(k) * (0.9 if reg["f"] in ("east", "west", "down") else 1.0)), rng, 8)
        blok[..., 3] = 255
        i += 1
    return Image.fromarray(img)


# =====================================================================================================================
# animations: a travelling ripple along the chain of pieces
# =====================================================================================================================
def anims_tapijt():
    def golf(amp, lengte, fase, extra=0.0):
        stappen = 8
        pts = []
        for i in range(stappen + 1):
            t = lengte * i / stappen
            hoek = amp * math.sin(2 * math.pi * (i / stappen) + fase) + extra
            pts.append((round(t, 3), (round(hoek, 2), 0, 0)))
        return {"rotation": kf(pts)}

    def zwaai(amp, lengte):
        return {"rotation": kf([(0, (0, 0, amp)), (lengte / 2, (0, 0, -amp)), (lengte, (0, 0, amp))])}

    def oor(amp, lengte):
        return {"rotation": kf([(0, (0, 0, 0)), (lengte * 0.8, (0, 0, 0)), (lengte * 0.88, (amp, 0, 0)), (lengte, (0, 0, 0))])}

    idle = {"root": zwaai(1.2, 4.0),
            "voor1": golf(2.5, 3.0, 0.0), "voor2": golf(3.5, 3.0, 0.8),
            "achter1": golf(2.5, 3.0, 1.6), "achter2": golf(3.5, 3.0, 2.4)}
    zweef = {"root": zwaai(0.8, 2.0),
             "voor1": golf(3.0, 1.4, 0.0, -1.0), "voor2": golf(4.5, 1.4, 0.9, -1.0),
             "achter1": golf(4.0, 1.4, 1.8), "achter2": golf(6.0, 1.4, 2.7)}
    snel = {"voor1": golf(2.0, 0.8, 0.0, -2.5), "voor2": golf(3.0, 0.8, 0.9, -3.0),
            "achter1": golf(6.0, 0.8, 1.8, 1.0), "achter2": golf(9.0, 0.8, 2.7, 2.0)}
    return {"idle": anim(4.0, idle), "zweef": anim(1.4, zweef), "snel": anim(0.8, snel)}


# =====================================================================================================================
# the cushions (tack items: drawn on the carpet's middle; anchor = model point (8, 8, 8))
# =====================================================================================================================
def kussen_textuur(kleur):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cellen = {"stof": kleur, "licht": tuple(min(255, int(v * 1.15 + 20)) for v in kleur), "donker": _donker(kleur, 0.78),
              "goud": (255, 208, 64), "knoop": (250, 236, 200)}
    for i, (k, c) in enumerate(cellen.items()):
        for dx in range(2):
            for dy in range(2):
                img.putpixel(((i % 8) * 2 + dx, (i // 8) * 2 + dy), c + (255,))
    return img


KUSSEN_CEL = {"stof": 0, "licht": 1, "donker": 2, "goud": 3, "knoop": 4}


def _kblok(van, tot, cel, tex):
    u, v = (KUSSEN_CEL[cel] % 8) * 2, (KUSSEN_CEL[cel] // 8) * 2
    faces = {f: {"uv": [u + 0.3, v + 0.3, u + 1.7, v + 1.7], "texture": "#t"} for f in ("north", "south", "east", "west", "up", "down")}
    return {"from": [round(x, 3) for x in van], "to": [round(x, 3) for x in tot], "faces": faces}


def kussen_model(naam):
    t = f"guhs:item/{ITEM_TEX}/{naam}"
    e = [_kblok((3, 8, 3.5), (13, 10, 12.5), "stof", t),           # the cushion
         _kblok((3.6, 10, 4.1), (12.4, 10.8, 11.9), "licht", t),   # puffed top
         _kblok((2.8, 8.6, 3.3), (13.2, 9.2, 12.7), "donker", t),  # the seam round it
         _kblok((7.5, 10.8, 7.5), (8.5, 11.1, 8.5), "knoop", t)]   # the button
    for x, z in ((2.4, 2.9), (12.6, 2.9), (2.4, 12.1), (12.6, 12.1)):   # gold tassels at the corners
        e.append(_kblok((x, 7.4, z), (x + 1, 9.4, z + 1), "goud", t))
    return {
        "textures": {"t": t, "particle": t},
        "elements": e,
        "display": {
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "gui": {"rotation": [25, -135, 0], "translation": [0, -1.5, 0], "scale": [0.85, 0.85, 0.85]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.4] * 3},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375] * 3},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4] * 3},
        },
    }


# =====================================================================================================================
# the icon: a rolled-up carpet with tassels
# =====================================================================================================================
def rol_icoon():
    d = Doek()
    roze, goud, donker = (224, 96, 122), (240, 176, 64), (150, 40, 70)
    d.lijn(3, 11, 12, 4, roze, dik=3)            # the roll, diagonal
    d.lijn(3, 12, 12, 5, donker, dik=1)
    d.lijn(4, 10, 11, 5, goud, dik=1)            # a gold band along it
    d.disc(12.5, 4.5, 2, goud)                   # the end of the roll (the spiral)
    d.px(12, 4, donker)
    d.px(13, 5, roze)
    for x, y in ((1, 13), (2, 14), (3, 14), (1, 12)):   # tassels at the bottom end
        d.px(x, y, (255, 232, 170))
    for x, y in ((14, 2), (15, 3), (14, 1)):
        d.px(x, y, (255, 232, 170))
    d.px(6, 8, (40, 24, 40))                     # a tiny guh face on the roll
    d.px(8, 7, (40, 24, 40))
    d.schaduw()
    return d.rand()


# =====================================================================================================================
# texts (nl, en)
# =====================================================================================================================
T = "vadsrijk.tapijt."
NAMEN_WEEFSEL = {"naturel": ("Rozensaffraan", "Rose saffron"), "turkoois": ("Turkoois", "Turquoise"), "granaat": ("Granaatappel", "Pomegranate"),
                 "zandroos": ("Zandroos", "Desert rose"), "nacht": ("Sterrennacht", "Starry night"),
                 "karavaan": ("Eerste Karavaan", "First Caravan")}
NAMEN_KWAST = {"kwast_goud": ("Gouden kwastjes", "Gold tassels"), "kwast_roze": ("Roze kwastjes", "Pink tassels"),
               "kwast_turkoois": ("Turkooise kwastjes", "Turquoise tassels"), "kwast_wit": ("Witte kwastjes", "White tassels"),
               "kwast_regenboog": ("Regenboogkwastjes", "Rainbow tassels")}
NAMEN_KUSSEN = {"kussen_roze": ("Roze kussentje", "Pink cushion"), "kussen_saffraan": ("Saffraankussentje", "Saffron cushion"),
                "kussen_turkoois": ("Turkoois kussentje", "Turquoise cushion"), "kussen_paars": ("Paars kussentje", "Purple cushion")}


def teksten():
    t = {
        "entity.guhs.vr_eco_tapijt": ("Vliegend tapijt", "Flying carpet"),
        "item.guhs.vr_item_zadel_tapijt": ("Opgerold vliegend tapijt", "Rolled-up flying carpet"),
        "vadsrijk.economie.rijdier.tapijt": ("Vliegend tapijt", "Flying carpet"),
        "vadsrijk.economie.rijdier.eigen.tapijt": (
            "Zweeft boven grond en water en glijdt heerlijk door. Je kunt hem draaien terwijl hij nog de oude kant op zweeft. Spatie = hupje.",
            "Hovers over ground and water and glides on and on. You can turn it while it still drifts the old way. Space = a little hop."),
        "vadsrijk.economie.stal.nog_niet.tapijt": ("Tapijtweefster Knoopje weeft hem voor je in het Zijdeland (zone 6).",
                                                   "Carpet weaver Knoopje weaves it for you in the Silk Land (zone 6)."),
        "vadsrijk.rijdier.as.vacht.tapijt": ("Weefsel", "Weave"),
        "vadsrijk.rijdier.as.patroon.tapijt": ("Kwastjes", "Tassels"),
        "vadsrijk.rijdier.as.tuig.tapijt": ("Kussen", "Cushion"),
        "vadsrijk.rijdier.nieuw.patroon_van": ("Nieuw voor je %2$s: %1$s! Kies het met %3$s, Aankleden.",
                                               "New for your %2$s: %1$s! Pick it with %3$s, Dress up."),
        "vadsrijk.rijdier.nieuw.tuig_van": ("Nieuw voor je %2$s: %1$s! Kies het met %3$s, Aankleden.",
                                            "New for your %2$s: %1$s! Pick it with %3$s, Dress up."),
        "vadsrijk.rijdier.bron.prestatie.lv10_tapijt": ("Prestatie: je vliegend tapijt op level 10.", "Achievement: your flying carpet at level 10."),
        "vadsrijk.rijdier.bron.api.z6_kwastjes": ("Beloning van Knoopje: vind haar verloren tapijtkwastjes (Saffraanpoort).",
                                                  "Knoopje's reward: find her lost carpet tassels (Saffron Gate)."),
        "vadsrijk.rijdier.bron.api.z6_eerste_karavaan": ("In de kist van de Eerste Karavaan, diep in de Zingende Zoutgrot.",
                                                         "In the chest of the First Caravan, deep in the Singing Salt Cave."),
        T + "scherm.zweeft": ("Zweeft %s blok hoog · glijdt ~%s blokken uit", "Hovers %s blocks up · glides ~%s blocks"),
        T + "tip.gekregen.titel": ("Een vliegend tapijt!", "A flying carpet!"),
        T + "tip.gekregen": ("Kies het tapijt met N en roep het met je rijdier-toets (X). Bij N, Aankleden kies je het weefsel, de kwastjes en het kussen.",
                             "Pick the carpet with N and call it with your mount key (X). In N, Dress up you choose the weave, the tassels and the cushion."),
        T + "tip.rijden.titel": ("Zweven!", "Gliding!"),
        T + "tip.rijden": ("W = vaart maken, loslaten = uitglijden (hij remt niet meteen!), S = remmen, A/D = opzij. Hij draait naar waar je kijkt, maar zweeft nog even de oude kant op. Spatie = hupje. Hoger vliegen kan hij niet.",
                           "W = speed up, let go = glide (it doesn't stop at once!), S = brake, A/D = sideways. It turns to where you look, but drifts the old way for a while. Space = a hop. It can't fly any higher."),
    }
    for k, (nl, en) in NAMEN_WEEFSEL.items():
        t[f"vadsrijk.rijdier.vacht.tapijt.{k}"] = (nl, en)
    for k, (nl, en) in NAMEN_KWAST.items():
        t[f"vadsrijk.rijdier.vacht.tapijt.{k}"] = (nl, en)
    for k, (nl, en) in NAMEN_KUSSEN.items():
        t[f"vadsrijk.rijdier.vacht.tapijt.{k}"] = (nl, en)
        t[f"item.guhs.vr_rijtuig_{k}"] = (nl, en)
    return t


# =====================================================================================================================
# writing
# =====================================================================================================================
def _schrijf_json(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _png(pad, img):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    img.save(pad)


def _item(a, w, naam, model):
    w(os.path.join(a, "models", "item", f"{naam}.json"), model)
    w(os.path.join(a, "items", f"{naam}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}})


def assets(a, w=_schrijf_json):
    m = model_tapijt()
    w(os.path.join(a, "geckolib", "models", "entity", "vr_eco_tapijt.geo.json"), m.geo())
    w(os.path.join(a, "geckolib", "animations", "entity", "vr_eco_tapijt.animation.json"), W.anim_bestand(anims_tapijt()))
    tex = os.path.join(a, "textures", "entity")
    for i, (naam, p) in enumerate(WEEFSELS.items()):
        bestand = "vr_eco_tapijt.png" if naam == "naturel" else f"vr_eco_tapijt_{naam}.png"
        _png(os.path.join(tex, bestand), schilder(m, p, 6100 + i))
    for naam, kleur in KWASTEN.items():
        _png(os.path.join(tex, f"vr_rijpatroon_tapijt_{naam}.png"), kwast_overlay(m, kleur))
    it = os.path.join(a, "textures", "item", ITEM_TEX)
    for naam, kleur in KUSSENS.items():
        _png(os.path.join(it, f"{naam}.png"), kussen_textuur(kleur))
        _item(a, w, f"vr_rijtuig_{naam}", kussen_model(naam))
    rol_icoon().png(os.path.join(it, "vr_item_zadel_tapijt.png"))
    w(os.path.join(a, "models", "item", "vr_item_zadel_tapijt.json"),
      {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{ITEM_TEX}/vr_item_zadel_tapijt"}})
    w(os.path.join(a, "items", "vr_item_zadel_tapijt.json"), {"model": {"type": "minecraft:model", "model": "guhs:item/vr_item_zadel_tapijt"}})
    return m


def build(h):
    assets(h.A, h.w)
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    m = assets(a)
    t = teksten()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in t.items()})
        _schrijf_json(p, data)
    print(f"vadsrijk_u_tapijt: model ({m.W * m.px}x{m.hoogte() * m.px} texture), {len(WEEFSELS)} weaves, {len(KWASTEN)} tassel colours, "
          f"{len(KUSSENS)} cushions, {len(t)} texts")


if __name__ == "__main__":
    _direct()
