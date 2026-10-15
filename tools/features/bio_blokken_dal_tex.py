"""
biomes3 slice "blokken-dal": the textures of the Klaterdal block sets (called from bio_blokken_dal.build).

Everything is drawn here, pixel by pixel (16 x 16), with fixed seeds (bio_lib.rng). The looks, and why:
  roze lakhout     deep red-pink with hard white-pink gloss lines and almost no grain: it must read as LACQUER next to the
                   mod's soft, pale, fluffy pinks (those are pastel and noisy; this is saturated, dark-seamed and smooth)
  guh-dakpannen    glazed pantiles: wide scallops in half-brick bond with a shine on every tile, in roze, wit and grijs
  shoji            cream paper with fibres behind a thin wooden grid; tatami: woven rush with a rose cloth border
  toro             pale speckled garden stone; geharkt zand: light gravel with grooves; gladde knuffelsteen: rounded rock
  guh-bamboe       jade stalk with pink knots; esdoorn: grey-brown bark, red and orange leaves
"""
import math

import numpy as np
from PIL import Image

from features import bio_lib as lib

# --- palettes ----------------------------------------------------------------------------------------------------------
LAK = {"naad": (104, 14, 44), "donker": (158, 26, 62), "basis": (200, 42, 80), "licht": (226, 72, 106), "glans": (255, 168, 186),
       "wit": (255, 226, 232)}
DAKPAN = {  # colour: (gap, shade, base, light, shine)
    "roze": ((150, 62, 96), (206, 104, 138), (236, 138, 168), (248, 172, 194), (255, 224, 232)),
    "wit": ((150, 142, 136), (206, 198, 190), (238, 232, 224), (250, 246, 240), (255, 255, 255)),
    "grijs": ((82, 86, 96), (122, 128, 140), (154, 160, 172), (182, 188, 198), (226, 230, 236)),
}
PAPIER = (252, 248, 238)
LAT = ((142, 98, 62), (184, 138, 92), (222, 182, 134))        # the shoji's wood: dark, base, light
BIES = ((178, 186, 108), (196, 204, 126), (160, 168, 94))       # tatami rush: base, light, dark
RAND = ((104, 40, 62), (136, 58, 84), (226, 150, 172))          # tatami cloth border: dark, base, dot
STRO = ((182, 156, 100), (204, 180, 122))
STEEN = ((204, 196, 198), (226, 220, 220), (242, 238, 236))     # the toro's stone
ZAND = {"kam": (244, 238, 222), "basis": (230, 222, 202), "laag": (216, 207, 186), "groef": (192, 182, 158)}
ESDOORN = {"rood": ((132, 24, 30), (217, 67, 47), (246, 120, 70)), "oranje": ((176, 70, 16), (242, 140, 40), (255, 204, 96))}


def _c(v):
    return tuple(int(max(0, min(255, round(x)))) for x in v)


def _mix(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def _nieuw(w=16, h=16):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def _ruis(img, rng, sterkte):
    """A little per-pixel noise on the opaque pixels."""
    a = np.asarray(img).astype(np.int16)
    n = rng.integers(-sterkte, sterkte + 1, a.shape[:2])
    for i in range(3):
        a[..., i] = np.clip(a[..., i] + n, 0, 255)
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def _golf(rng, cellen=4, n=16):
    """Smooth tileable noise 0..1 (value noise on a wrapped grid): the soft blobs of a rounded rock."""
    g = rng.random((cellen, cellen))
    out = np.zeros((n, n))
    for y in range(n):
        for x in range(n):
            fx, fy = x * cellen / n, y * cellen / n
            x0, y0 = int(fx), int(fy)
            tx, ty = fx - x0, fy - y0
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            a, b = g[y0 % cellen, x0 % cellen], g[y0 % cellen, (x0 + 1) % cellen]
            c, d = g[(y0 + 1) % cellen, x0 % cellen], g[(y0 + 1) % cellen, (x0 + 1) % cellen]
            out[y, x] = (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty
    return out


# --- roze lakhout ------------------------------------------------------------------------------------------------------
def lak_planken():
    """Four lacquered boards: a dark seam under each, a hard gloss line along its top and a sheen sweeping across the block."""
    rng = lib.rng("roze_lakhout_planken")
    img = _nieuw()
    for y in range(16):
        rij, ty = y // 4, y % 4
        naad_x = (3 + 7 * rij) % 16
        for x in range(16):
            c = (LAK["licht"], LAK["basis"], LAK["donker"], LAK["naad"])[ty]
            if ty == 0:                                   # the gloss line: bright, with a white-hot stretch that shifts per board
                d = min((x - (4 + 5 * rij)) % 16, (4 + 5 * rij - x) % 16)
                c = LAK["wit"] if d == 0 else (LAK["glans"] if d <= 2 else LAK["licht"])
            if x == naad_x and ty != 3 and rij % 2 == 0:
                c = LAK["donker"]                         # a faint joint in every other board (long boards: no brick bond)
            img.putpixel((x, y), _c(c) + (255,))
    return _ruis(img, rng, 2)


def lak_balk():
    """The beam's side: a round lacquered post (dark edges, one bright vertical reflection), smooth along its length."""
    rng = lib.rng("roze_lakhout_balk")
    img = _nieuw()
    kolom = [LAK["naad"], LAK["donker"], LAK["basis"], LAK["licht"], LAK["glans"], LAK["wit"], LAK["glans"], LAK["licht"],
             LAK["basis"], LAK["basis"], LAK["basis"], LAK["donker"], LAK["basis"], LAK["donker"], LAK["donker"], LAK["naad"]]
    for y in range(16):
        for x in range(16):
            c = kolom[x]
            if x == 5 and y % 8 in (6, 7):
                c = LAK["glans"]                          # the reflection breaks now and then: it is a brushed-on gloss
            img.putpixel((x, y), _c(c) + (255,))
    return _ruis(img, rng, 2)


def lak_balk_kop():
    """The beam's end: a dark lacquer cap with a gold rim line and a little guh face pressed into it."""
    rng = lib.rng("roze_lakhout_balk_kop")
    img = _nieuw()
    zwart, goud = (58, 16, 32), (236, 190, 96)
    for y in range(16):
        for x in range(16):
            rand = min(x, y, 15 - x, 15 - y)
            c = zwart if rand == 0 else goud if rand == 1 else LAK["donker"] if rand == 2 else LAK["basis"]
            if rand >= 3 and (x + y) in (9, 10):
                c = LAK["licht"]
            img.putpixel((x, y), _c(c) + (255,))
    for (x, y) in ((6, 7), (9, 7)):                       # eyes
        img.putpixel((x, y), zwart + (255,))
    for (x, y) in ((7, 9), (8, 9)):                       # mouth
        img.putpixel((x, y), zwart + (255,))
    return _ruis(img, rng, 2)


def _lak_vlak(img, x0, y0, x1, y1):
    """A lacquered panel: base with a gloss line under its top edge and a dark line at its foot."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            c = LAK["basis"]
            if y == y0:
                c = LAK["glans"] if (x - x0) in range(1, max(2, (x1 - x0) // 2)) else LAK["licht"]
            elif y == y1:
                c = LAK["donker"]
            elif x == x0:
                c = LAK["licht"]
            elif x == x1:
                c = LAK["donker"]
            img.putpixel((x, y), _c(c) + (255,))


def lak_deur():
    """The door (top, bottom): a lacquered frame; above a round paper moon window with a lattice, below two sunken panels."""
    rng = lib.rng("roze_lakhout_deur")
    top, bottom = _nieuw(), _nieuw()
    for img in (top, bottom):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y), _c(LAK["basis"]) + (255,))
        for i in range(16):                               # the frame's edges
            img.putpixel((0, i), _c(LAK["naad"]) + (255,))
            img.putpixel((15, i), _c(LAK["naad"]) + (255,))
            img.putpixel((1, i), _c(LAK["licht"]) + (255,))
    for x in range(16):
        top.putpixel((x, 0), _c(LAK["naad"]) + (255,))
        top.putpixel((x, 1), _c(LAK["glans"] if 2 < x < 9 else LAK["licht"]) + (255,))
        bottom.putpixel((x, 15), _c(LAK["naad"]) + (255,))
    for y in range(16):                                   # the moon window: paper behind a cross of lacquer
        for x in range(16):
            d = math.hypot(x - 7.5, y - 8.5)
            if d < 5.2:
                c = PAPIER if d < 4.3 else LAK["naad"]
                if d < 4.3 and (x in (7, 8) or y in (8, 9)):
                    c = LAK["donker"] if (x == 8 or y == 9) else LAK["licht"]
                top.putpixel((x, y), _c(c) + (255,))
    for (x, y) in ((6, 2), (9, 2)):                       # two little ears on the window: a guh looks out
        top.putpixel((x, y), _c(LAK["naad"]) + (255,))
        top.putpixel((x, y + 1), _c(LAK["naad"]) + (255,))
    for (x0, y0, x1, y1) in ((3, 1, 12, 6), (3, 9, 12, 13)):
        for y in range(y0 - 1, y1 + 2):
            for x in range(x0 - 1, x1 + 2):
                if not (x0 <= x <= x1 and y0 <= y <= y1):
                    bottom.putpixel((x, y), _c(LAK["naad"]) + (255,))
        _lak_vlak(bottom, x0, y0, x1, y1)
    for img, y in ((top, 14), (bottom, 0)):               # the brass handle, on the seam
        img.putpixel((13, y), (236, 190, 96, 255))
    return _ruis(top, rng, 2), _ruis(bottom, rng, 2)


def deur_icoon(top, bottom):
    """The door as an item: the whole door, half size."""
    heel = _nieuw(16, 32)
    heel.paste(top, (0, 0))
    heel.paste(bottom, (0, 16))
    klein = heel.resize((8, 16), Image.NEAREST)
    img = _nieuw()
    img.paste(klein, (4, 0))
    return img


def lak_luik():
    """The trapdoor: a lacquered lattice (kumiko), open between the bars."""
    rng = lib.rng("roze_lakhout_luik")
    img = _nieuw()
    for y in range(16):
        for x in range(16):
            rand = min(x, y, 15 - x, 15 - y)
            lat = x in (5, 10) or y in (5, 10)
            if rand <= 1:
                c = LAK["naad"] if rand == 0 else (LAK["glans"] if y == 1 and 2 < x < 9 else LAK["basis"])
            elif lat:
                c = LAK["licht"] if (x in (5, 10) and y % 5 == 2) else LAK["basis"]
            else:
                continue
            img.putpixel((x, y), _c(c) + (255,))
    return _ruis(img, rng, 2)


# --- guh-dakpannen -----------------------------------------------------------------------------------------------------
def dakpan(kleur):
    """Glazed pantiles: scallops 8 wide and 4 high in half-brick bond; each has a shine, a rounded foot and a shadow above."""
    rng = lib.rng("guh_dakpan_" + kleur)
    gat, schaduw, basis, licht, glim = DAKPAN[kleur]
    img = _nieuw()
    for y in range(16):
        rij, ty = y // 4, y % 4
        for x in range(16):
            tx = (x + 4 * (rij % 2)) % 8
            if ty == 0:
                c = schaduw                                # under the row above
            elif ty == 1:
                c = glim if tx in (2, 3) else licht if tx in (1, 4, 5) else basis
            elif ty == 2:
                c = basis if 0 < tx < 7 else schaduw
            else:
                c = gat if tx in (0, 7) else schaduw if tx in (1, 6) else basis      # the rounded foot of the tile
            img.putpixel((x, y), _c(c) + (255,))
    return _ruis(img, rng, 3)


# --- shoji and tatami --------------------------------------------------------------------------------------------------
def shoji():
    """Paper behind a thin wooden grid: mostly paper (tall panes, two bars up and one across), a stile at each side and a
    rail at top and foot, so stacked panels read as one door."""
    rng = lib.rng("shoji")
    img = _nieuw()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = LAT[0] if (x == 15 or y == 15) else LAT[1]
            elif x in (5, 10) or y == 8:
                c = LAT[1]
            else:
                v = int(rng.integers(-3, 4))
                c = (PAPIER[0] + v, PAPIER[1] + v, PAPIER[2] + v - (4 if (x + 3 * y) % 7 == 0 else 0))    # paper fibres
            img.putpixel((x, y), _c(c) + (255,))
    for (x, y) in ((12, 11), (13, 12), (12, 13), (11, 12)):     # one tiny blossom printed on the paper
        img.putpixel((x, y), (246, 196, 212, 255))
    img.putpixel((12, 12), (240, 150, 180, 255))
    return img


def shoji_rand():
    """The panel's thin edge: wood."""
    rng = lib.rng("shoji_rand")
    img = _nieuw()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), _c(LAT[1 if (x + y // 4) % 5 else 2]) + (255,))
    return _ruis(img, rng, 4)


def tatami_boven(gekoppeld):
    """The mat from above (north = up in the image): rush woven along north-south, the cloth border on the long sides
    (west and east), a seam at the mat's ends. A paired block has no seam at its north end: there the mat goes on."""
    rng = lib.rng("tatami_" + ("kop" if gekoppeld else "los"))
    img = _nieuw()
    for y in range(16):
        for x in range(16):
            if x < 2 or x > 13:
                c = RAND[2] if (x in (0, 15) and y % 4 == 1) or (x in (1, 14) and y % 4 == 3) else RAND[1] if x in (1, 14) else RAND[0]
            else:
                c = BIES[1] if x % 2 else BIES[0]
                if (y + 2 * (x // 2)) % 8 == 0:
                    c = BIES[2]                             # the binding threads, staggered
                if y == 15 or (y == 0 and not gekoppeld):
                    c = BIES[2]
            img.putpixel((x, y), _c(c) + (255,))
    return _ruis(img, rng, 4)


def tatami_zij(rand):
    """The mat's side: the cloth border (long sides) or the cut rush (ends) on top of a straw core."""
    rng = lib.rng("tatami_zij_" + ("rand" if rand else "kop"))
    img = _nieuw()
    for y in range(16):
        for x in range(16):
            if y < 3:
                if rand:
                    c = RAND[2] if (y == 1 and x % 4 == 1) else RAND[1] if y < 2 else RAND[0]
                else:
                    c = BIES[2] if y == 2 else (BIES[1] if x % 2 else BIES[0])
            else:
                c = STRO[1] if (y % 3 == 0 and (x + y) % 5) else STRO[0]
            img.putpixel((x, y), _c(c) + (255,))
    return _ruis(img, rng, 5)


def stro():
    rng = lib.rng("tatami_onder")
    img = _nieuw()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), _c(STRO[1] if (y % 3 == 0 and (x + y) % 5) else STRO[0]) + (255,))
    return _ruis(img, rng, 5)


# --- tuinspul ----------------------------------------------------------------------------------------------------------
def toro_steen():
    """Pale garden stone: soft blotches and a few darker and pinker specks."""
    rng = lib.rng("toro")
    g = _golf(rng)
    img = _nieuw()
    for y in range(16):
        for x in range(16):
            c = _mix(STEEN[0], STEEN[2], g[y, x])
            r = rng.random()
            if r < 0.06:
                c = (176, 168, 174)
            elif r < 0.10:
                c = (238, 208, 216)
            img.putpixel((x, y), _c(c) + (255,))
    return _ruis(img, rng, 3)


def egaal(kleur, naam, sterkte=3):
    img = _nieuw()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), _c(kleur) + (255,))
    return _ruis(img, lib.rng(naam), sterkte)


def _groef(d):
    """The colour of raked sand at distance d (pixels) across the grooves: a crest, a slope, the groove, a slope; period 4."""
    return (ZAND["kam"], ZAND["basis"], ZAND["groef"], ZAND["laag"])[int(math.floor(d)) % 4]


def _zand(afstand, naam):
    rng = lib.rng(naam)
    img = _nieuw()
    for y in range(16):
        for x in range(16):
            c = _groef(afstand(x + 0.5, y + 0.5))
            if rng.random() < 0.05:
                c = _mix(c, (170, 160, 140), 0.5)          # a grain of gravel
            img.putpixel((x, y), _c(c) + (255,))
    return _ruis(img, rng, 3)


def zand_recht():
    """Grooves running east-west (north = up in the image)."""
    return _zand(lambda x, y: y, "geharkt_zand")


def zand_rand():
    """A ring's side piece north of its centre: the same grooves, counted from the centre's side (the south edge)."""
    return _zand(lambda x, y: 16 - y, "geharkt_zand_ring_rand")


def zand_hoek():
    """A ring's corner piece north-east of its centre: quarter circles around the corner that touches the centre (south-west)."""
    return _zand(lambda x, y: math.hypot(x, 16 - y), "geharkt_zand_ring_hoek")


def zand_rond():
    """A round of circles around the middle of the block; at the edge it runs on into the pieces around it."""
    return _zand(lambda x, y: math.hypot(x - 8, y - 8) + 8, "geharkt_zand_ring_rond")


def zand_zij():
    rng = lib.rng("geharkt_zand_zij")
    img = _nieuw()
    for y in range(16):
        for x in range(16):
            c = ZAND["kam"] if y == 0 else ZAND["laag"] if y == 1 else ZAND["basis"]
            if rng.random() < 0.07:
                c = _mix(c, (170, 160, 140), 0.5)
            img.putpixel((x, y), _c(c) + (255,))
    return _ruis(img, rng, 4)


def gladde_knuffelsteen():
    """Natural rounded rock: a few big smooth swellings pressed against each other, each lit from the upper left and
    blushing pink in its shadow, with a soft crease where two meet. No bricks, no mortar, no straight line: this is the
    cliff and the boulder, not the wall."""
    rng = lib.rng("gladde_knuffelsteen")
    kernen = [(2.5, 3.0), (10.5, 1.5), (6.5, 9.0), (13.5, 8.5), (1.5, 13.0), (9.5, 14.0)]
    img = _nieuw()
    for y in range(16):
        for x in range(16):
            best = []
            for (kx, ky) in kernen:
                dx = (x + 0.5 - kx + 8) % 16 - 8             # (wrapped: the texture tiles)
                dy = (y + 0.5 - ky + 8) % 16 - 8
                best.append((math.hypot(dx, dy * 1.15), dx, dy))
            best.sort()
            (d1, dx, dy), d2 = best[0], best[1][0]
            if d2 - d1 < 1.0:
                c = (214, 194, 202)                         # the crease between two swellings
            else:
                licht = max(-1.0, min(1.0, -(dx + dy) / 6.0))      # lit from the upper left
                c = _mix((238, 224, 228), (255, 252, 248), (licht + 1) / 2)
                if d2 - d1 < 2.0 and licht < 0:
                    c = _mix(c, (226, 204, 212), 0.6)       # the shadow side rolls into the crease
            img.putpixel((x, y), _c(c) + (255,))
    return _ruis(img, rng, 2)


def bamboe_stengel():
    """The stalk (the model uses the three left columns): jade with a light side, and a pink knot with a dark line under it."""
    rng = lib.rng("guh_bamboe")
    img = _nieuw()
    kolom = [(176, 226, 172), (140, 200, 146), (102, 168, 120)]
    for y in range(16):
        for x in range(16):
            c = kolom[x % 3]
            if y % 8 == 3:
                c = (250, 170, 196) if x % 3 < 2 else (226, 130, 164)        # the knot
            elif y % 8 == 4:
                c = (78, 138, 100)
            img.putpixel((x, y), _c(c) + (255,))
    return _ruis(img, rng, 3)


def _blad(img, x, y, dx, dy, lengte, licht):
    """One slender leaf from (x, y) in direction (dx, dy): green with a pink tip."""
    for i in range(lengte):
        px, py = int(round(x + dx * i)), int(round(y + dy * i))
        if 0 <= px < 16 and 0 <= py < 16:
            c = (250, 170, 196) if i >= lengte - 1 else (188, 232, 176) if licht and i % 2 == 0 else (126, 196, 138)
            img.putpixel((px, py), c + (255,))
            if i in range(1, lengte - 2) and 0 <= py + 1 < 16:
                img.putpixel((px, py + 1), (92, 160, 112, 255))


def bamboe_blad(groot):
    """The leaves as a cross: a few (small) or a whole tuft (large) of slender leaves fanning out from the stalk."""
    img = _nieuw()
    takken = [(8, 9, 0.9, -0.45, 7, True), (7, 9, -0.9, -0.45, 7, False), (8, 12, 0.9, 0.2, 6, False), (7, 12, -0.9, 0.2, 6, True)]
    if groot:
        takken += [(8, 5, 0.75, -0.6, 7, False), (7, 5, -0.75, -0.6, 7, True), (8, 3, 0.4, -0.9, 4, True), (7, 3, -0.4, -0.9, 4, False),
                   (8, 14, 0.95, 0.1, 5, True), (7, 14, -0.95, 0.1, 5, False)]
    for t in takken:
        _blad(img, *t)
    return img


def bamboe_icoon():
    img = _nieuw()
    stengel = bamboe_stengel()
    for y in range(16):
        for x in range(3):
            img.putpixel((7 + x, y), stengel.getpixel((x, y)))
    _blad(img, 10, 6, 0.9, -0.5, 5, True)
    _blad(img, 6, 10, -0.9, -0.4, 5, False)
    _blad(img, 10, 12, 0.9, 0.1, 4, False)
    return img


def bonsai_pot():
    """The pot's glaze: cream with a pink band and a small guh face on the band."""
    rng = lib.rng("bonsai_pot")
    img = _nieuw()
    for y in range(16):
        for x in range(16):
            c = (244, 236, 226) if y % 4 != 1 else (236, 150, 178)
            if y % 4 == 0:
                c = (255, 250, 244)
            if y % 4 == 3:
                c = (214, 202, 192)
            img.putpixel((x, y), _c(c) + (255,))
    return _ruis(img, rng, 2)


def bonsai_blad(bloesem):
    rng = lib.rng("bonsai_blad_" + str(bloesem))
    img = _nieuw()
    for y in range(16):
        for x in range(16):
            r = rng.random()
            if bloesem:
                c = (252, 186, 210) if r < 0.6 else (255, 226, 236) if r < 0.85 else (236, 140, 176)
            else:
                c = (84, 150, 96) if r < 0.6 else (118, 180, 116) if r < 0.85 else (58, 118, 82)
            img.putpixel((x, y), c + (255,))
    return img


# --- esdoorn -----------------------------------------------------------------------------------------------------------
def esdoorn_bladeren(h, kleur):
    """Vanilla's oak leaf pattern, painted in autumn: dark in the depth, the colour in the middle, a glowing edge."""
    rng = lib.rng("esdoorn_bladeren_" + kleur)
    donker, midden, licht = ESDOORN[kleur]
    bron = np.asarray(h.vanilla("block/oak_leaves")).astype(np.float32)
    lum = bron[..., 0] * .3 + bron[..., 1] * .59 + bron[..., 2] * .11
    vol = bron[..., 3] > 0
    lo, hi = np.percentile(lum[vol], 3), np.percentile(lum[vol], 97)
    t = np.clip((lum - lo) / (hi - lo), 0, 1)
    img = _nieuw()
    for y in range(16):
        for x in range(16):
            if not vol[y, x]:
                continue
            c = _mix(donker, midden, t[y, x] * 2) if t[y, x] < 0.5 else _mix(midden, licht, t[y, x] * 2 - 1)
            if rng.random() < 0.06:
                c = licht
            img.putpixel((x, y), _c(c) + (255,))
    return img


def esdoorn_zaailing(h):
    """Vanilla's oak sapling with red leaves on a grey-brown stem."""
    bron = h.vanilla("block/oak_sapling")
    donker, midden, licht = ESDOORN["rood"]
    img = _nieuw()
    for y in range(16):
        for x in range(16):
            r, g, b, a = bron.getpixel((x, y))
            if not a:
                continue
            if g > r + 8:                                   # a leaf
                t = min(1.0, g / 170.0)
                c = _mix(donker, licht, t)
            else:
                c = _mix((96, 70, 62), (158, 126, 110), (r + g + b) / 520.0)
            img.putpixel((x, y), _c(c) + (255,))
    return img


def build(h):
    save, ramp, v = h.save, h.ramp, h.vanilla
    # roze lakhout
    save(lak_planken(), "block", "roze_lakhout_planken.png")
    save(lak_balk(), "block", "roze_lakhout_balk.png")
    save(lak_balk_kop(), "block", "roze_lakhout_balk_top.png")
    top, bottom = lak_deur()
    save(top, "block", "roze_lakhout_deur_top.png")
    save(bottom, "block", "roze_lakhout_deur_bottom.png")
    save(deur_icoon(top, bottom), "item", "roze_lakhout_deur.png")
    save(lak_luik(), "block", "roze_lakhout_luik.png")
    # guh-dakpannen
    for kleur in DAKPAN:
        save(dakpan(kleur), "block", f"guh_dakpan_{kleur}.png")
    # shoji, tatami
    save(shoji(), "block", "shoji.png")
    save(shoji_rand(), "block", "shoji_rand.png")
    save(tatami_boven(False), "block", "tatami_los.png")
    save(tatami_boven(True), "block", "tatami_kop.png")
    save(tatami_zij(True), "block", "tatami_zij_rand.png")
    save(tatami_zij(False), "block", "tatami_zij_kop.png")
    save(stro(), "block", "tatami_onder.png")
    # tuinspul
    save(toro_steen(), "block", "toro.png")
    save(egaal((60, 52, 62), "toro_donker", 4), "block", "toro_donker.png")
    save(egaal((255, 204, 96), "toro_licht", 5), "block", "toro_licht.png")
    save(zand_recht(), "block", "geharkt_zand.png")
    save(zand_zij(), "block", "geharkt_zand_zij.png")
    save(zand_rand(), "block", "geharkt_zand_ring_rand.png")
    save(zand_hoek(), "block", "geharkt_zand_ring_hoek.png")
    save(zand_rond(), "block", "geharkt_zand_ring_rond.png")
    save(gladde_knuffelsteen(), "block", "gladde_knuffelsteen.png")
    save(bamboe_stengel(), "block", "guh_bamboe.png")
    save(bamboe_blad(False), "block", "guh_bamboe_blad_klein.png")
    save(bamboe_blad(True), "block", "guh_bamboe_blad_groot.png")
    save(bamboe_icoon(), "item", "guh_bamboe.png")
    save(bonsai_pot(), "block", "bonsai_pot.png")
    save(egaal((92, 66, 50), "bonsai_aarde", 8), "block", "bonsai_pot_aarde.png")
    save(bonsai_blad(False), "block", "bonsai_pot_blad.png")
    save(bonsai_blad(True), "block", "bonsai_pot_bloesem.png")
    # esdoorn
    save(ramp(v("block/dark_oak_log"), (92, 68, 62), (176, 146, 130)), "block", "esdoorn_stam.png")
    save(ramp(v("block/oak_log_top"), (120, 86, 70), (244, 214, 190)), "block", "esdoorn_stam_top.png")
    save(esdoorn_bladeren(h, "rood"), "block", "esdoorn_bladeren_rood.png")
    save(esdoorn_bladeren(h, "oranje"), "block", "esdoorn_bladeren_oranje.png")
    save(esdoorn_zaailing(h), "block", "esdoorn_zaailing.png")
