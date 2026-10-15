"""
bbq2 (tech-machines): the looks of the guh machines (textures, block models, blockstates, item models).

Every machine is a little guh: a face on the front (the three faces of vadskracht.snoet: asleep, awake, surprised), two
ears, and a part that moves while it works. The part that moves is a block model of its own (<machine>_<part>.json),
drawn by feature/techmachine/client/MachineRenderer.java: THE PIVOTS AND DISTANCES BELOW ARE THE SAME NUMBERS AS IN ITS
TABLE (change them in both places). All models are drawn facing north (the snoet at z = 0); pixels, a block is 16.

  oogster         a green block under a straw hat (the ears stand on the brim), a reel of four blades on two arms in front
                  part oogster_haspel: turns around the line y 5, z -4.5
  knabbelaar      a pink head with an open mouth; two big teeth hang from its upper lip, a jaw with three little teeth below
                  parts knabbelaar_tanden (bite down 1.5), knabbelaar_kaak (comes up 0.5)
  neerzetter      a blue block with a framed hatch; part neerzetter_arm: a yellow pusher on a rod, slides out 7
  vadsmolen       two blocks high: a cream body, a tower, a red roof in three steps with the ears on it
                  part vadsmolen_wieken: four sails around the hub at x 8, y 22
  knutselmachine  a workbench: a tan body, a worktop with a crafting grid, a little workpiece
                  parts knutselmachine_hamer_links / _rechts: two hammers hinged at y 17, z 14
  tekentafel      a low cabinet with the face, a drawing board that slopes towards you with a blue sheet and a pencil;
                  no moving part (it has no block entity): while somebody draws, the drawing on its sheet draws itself
                  (an animated texture, tekentafel_blad_tekent)
  plantagebak     3 x 3 blocks: the kern (the middle of the front edge, with the face, the ears and the watering can), four
                  corners with a post, three more edges, and the soil in the middle (block techmachine_plantagebak_deel,
                  property stuk); part plantagebak_gieter: tips around y 17, z 2.7

The item models hold the machine WITH its moving parts at rest (an item has no renderer of its own).
"""
import os

import numpy as np
from PIL import Image

from features import vadskracht

ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
STATEN = vadskracht.STATEN
VLAKKEN = ("north", "south", "west", "east", "up", "down")

# name -> (basis colour, accent colour)
KLEUR = {
    "oogster": ((150, 200, 110), (244, 204, 84)),
    "knabbelaar": ((240, 156, 160), (206, 76, 104)),
    "neerzetter": ((150, 186, 236), (250, 212, 92)),
    "vadsmolen": ((238, 218, 186), (196, 92, 84)),
    "knutselmachine": ((228, 172, 112), (110, 160, 228)),
    "tekentafel": ((198, 152, 102), (92, 142, 216)),
    "plantagebak": ((172, 122, 78), (118, 182, 92)),
}
# the Tekentafel's sheet: in how many steps the drawing appears
TEKEN_STAPPEN = 8
OOR = (255, 186, 214)
OOR_BINNEN = (255, 140, 182)
IJZER = (150, 154, 166)
HOUT = (150, 104, 62)
STRO = (236, 204, 116)
WIT = (252, 250, 240)
AARDE = (94, 64, 42)
BLAUW = (92, 142, 216)


# =====================================================================================================================
# little painters
# =====================================================================================================================
def _zaad(name):
    return sum((i + 1) * ord(ch) for i, ch in enumerate(name)) % 100000 + 7700


def _ruis(basis, var, seed, size=16):
    rng = np.random.default_rng(seed)
    a = np.zeros((size, size, 4), np.uint8)
    n = rng.normal(0, var, (size, size))
    for c in range(3):
        a[..., c] = np.clip(basis[c] + n, 0, 255)
    a[..., 3] = 255
    return a


def _tint(kleur, f):
    return tuple(int(max(0, min(255, c * f))) for c in kleur[:3])


def _vul(a, x0, y0, x1, y1, kleur):
    """Fills columns x0..x1 and rows y0..y1 (inclusive)."""
    a[y0:y1 + 1, x0:x1 + 1, :3] = kleur[:3]
    a[y0:y1 + 1, x0:x1 + 1, 3] = 255


def _rand(a, kleur):
    a[0, :, :3] = kleur
    a[-1, :, :3] = kleur
    a[:, 0, :3] = kleur
    a[:, -1, :3] = kleur


def _planken(basis, seed, lagen=(5, 11)):
    """Horizontal boards: a seam above the rows in `lagen`, a darker bottom edge on each board, a nail at both ends."""
    a = _ruis(basis, 5, seed)
    for y in range(16):
        if y in lagen:
            a[y, :, :3] = _tint(basis, 0.66)
        elif y + 1 in lagen or y == 15:
            a[y, :, :3] = np.clip(a[y, :, :3].astype(int) - 14, 0, 255)
    rijen = [0] + [y + 1 for y in lagen]
    for y in rijen:
        for x in (1, 14):
            if y + 1 < 16:
                a[y + 1, x, :3] = _tint(basis, 0.55)
    return a


def _png(a):
    return Image.fromarray(a)


# =====================================================================================================================
# model helpers
# =====================================================================================================================
def _uv(vlak, f, t):
    """The uv a face gets by default (the block's own projection), moved or squeezed into 0..16 where the box sticks out."""
    (fx, fy, fz), (tx, ty, tz) = f, t
    u0, v0, u1, v1 = {
        "north": (16 - tx, 16 - ty, 16 - fx, 16 - fy), "south": (fx, 16 - ty, tx, 16 - fy),
        "east": (16 - tz, 16 - ty, 16 - fz, 16 - fy), "west": (fz, 16 - ty, tz, 16 - fy),
        "up": (fx, fz, tx, tz), "down": (fx, 16 - tz, tx, 16 - fz)}[vlak]

    def past(a, b):
        if b - a > 16:
            return 0.0, 16.0
        if a < 0:
            return 0.0, b - a
        if b > 16:
            return 16.0 - (b - a), 16.0
        return a, b
    u0, u1 = past(u0, u1)
    v0, v1 = past(v0, v1)
    return [round(u0, 3), round(v0, 3), round(u1, 3), round(v1, 3)]


def doos(f, t, tex, uv=None, zonder=(), rot=None, cull=None):
    """
    A box. tex: one texture variable for all faces ("#x") or {face: "#x"} (a face that is left out is not drawn).
    uv: [u0, v0, u1, v1] for all faces or {face: [...]}; a face without one gets the default projection. Every face gets an
    explicit uv (models that stick out of their block need that). cull: faces that lie on the block's own side ("north"...).
    """
    faces = {}
    for vlak in VLAKKEN:
        if vlak in zonder:
            continue
        texture = tex.get(vlak) if isinstance(tex, dict) else tex
        if texture is None:
            continue
        eigen = uv.get(vlak) if isinstance(uv, dict) else uv
        face = {"texture": texture, "uv": list(eigen) if eigen else _uv(vlak, f, t)}
        if cull and vlak in cull:
            face["cullface"] = vlak
        faces[vlak] = face
    el = {"from": list(f), "to": list(t), "faces": faces}
    if rot:
        el["rotation"] = rot
    return el


def oren(plekken, y, z, tex="#oor", hoog=3):
    """Two (or more) ears: 4 wide, `hoog` high, 2 deep, their left edge at each x of `plekken`, standing on y, front at z."""
    return [doos([x, y, z], [x + 4, y + hoog, z + 2], tex, uv=[0, 0, 4, 3]) for x in plekken]


def lijf(hoog=16, voor="#voor", zij="#zij", boven="#boven"):
    """The machine's body: a box as wide and deep as the block, its face on the north side."""
    cull = ("north", "south", "west", "east", "down") + (("up",) if hoog >= 16 else ())
    return doos([0, 0, 0], [16, hoog, 16], {"north": voor, "south": zij, "west": zij, "east": zij, "up": boven, "down": zij}, cull=cull)


def model(textures, elements, display=None):
    m = {"parent": "minecraft:block/block", "textures": textures, "elements": elements}
    if display:
        m["display"] = display
    return m


def _tex(name, **meer):
    t = {"particle": f"guhs:block/{name}_zij", "zij": f"guhs:block/{name}_zij", "boven": f"guhs:block/{name}_boven",
         "oor": "guhs:block/techmachine_oor"}
    t.update({k: f"guhs:block/{v}" for k, v in meer.items()})
    return t


# =====================================================================================================================
# the shared ear texture
# =====================================================================================================================
def oor_textuur(h):
    a = np.zeros((16, 16, 4), np.uint8)
    _vul(a, 0, 0, 15, 15, OOR)
    _vul(a, 1, 1, 2, 2, OOR_BINNEN)          # the pink inside, on the 4 x 3 corner the ears use
    a[0, 0, :3] = _tint(OOR, 0.9)
    a[0, 3, :3] = _tint(OOR, 0.9)
    h.save(_png(a), "block", "techmachine_oor.png")


# =====================================================================================================================
# Oogster
# =====================================================================================================================
def _oogster_voor(img, staat):
    """The hatch becomes a grille (where the cut plants go in)."""
    basis, accent = KLEUR["oogster"]
    px = img.load()
    for y in range(9, 14):
        for x in range(3, 13):
            px[x, y] = _tint(basis, 0.45) + (255,)
    for y in (10, 12):
        for x in range(4, 12):
            px[x, y] = (_tint(accent, 0.6) if staat == "slaapt" else accent) + (255,)


def oogster(h):
    basis, accent = KLEUR["oogster"]
    vadskracht.machine(h, "oogster", basis, accent, voor=_oogster_voor)
    # the straw hat: woven straw on rows 0..12, a red band on rows 13..15
    a = _ruis(STRO, 5, _zaad("hoed"))
    for y in range(13):
        for x in range(16):
            if (x + (y // 2) * 2) % 4 == 0:
                a[y, x, :3] = _tint(STRO, 0.84)
            elif y % 2 == 1:
                a[y, x, :3] = _tint(STRO, 0.94)
    _vul(a, 0, 13, 15, 15, (204, 72, 76))
    a[13, :, :3] = (232, 110, 110)
    h.save(_png(a), "block", "oogster_hoed.png")
    # the reel: rows 0..3 the axle (iron), rows 4..11 a blade (accent, a bright cutting edge), rows 12..15 the arms
    a = _ruis(IJZER, 4, _zaad("haspel"))
    a[0, :, :3] = _tint(IJZER, 1.2)
    a[3, :, :3] = _tint(IJZER, 0.7)
    _vul(a, 0, 4, 15, 11, accent)
    a[4, :, :3] = (255, 244, 190)
    a[11, :, :3] = _tint(accent, 0.72)
    for x in range(1, 16, 3):
        a[5:11, x, :3] = _tint(accent, 0.86)
    _vul(a, 0, 12, 15, 15, _tint(IJZER, 0.62))
    a[12, :, :3] = _tint(IJZER, 0.8)
    h.save(_png(a), "block", "oogster_haspel.png")

    def vast(staat):
        return [
            lijf(13, boven="#hoed"),
            doos([-1, 13, -1], [17, 14, 17], "#hoed", uv={"up": [0, 0, 16, 12], "down": [0, 0, 16, 12], "north": [0, 5, 16, 6], "south": [0, 5, 16, 6],
                                                           "west": [0, 5, 16, 6], "east": [0, 5, 16, 6]}),
            doos([3, 14, 3], [13, 17, 13], "#hoed", uv={"up": [3, 1, 13, 11], "north": [3, 13, 13, 16], "south": [3, 13, 13, 16],
                                                         "west": [3, 13, 13, 16], "east": [3, 13, 13, 16]}, zonder=("down",)),
            *oren((2, 10), 14, 0.5),
            doos([-1, 4, -5.5], [0, 6, 3], "#haspel", uv=[0, 12, 9, 14]),
            doos([16, 4, -5.5], [17, 6, 3], "#haspel", uv=[0, 12, 9, 14]),
        ]

    blad = [0, 4, 15, 12]
    haspel = [
        doos([0, 4, -5.5], [16, 6, -3.5], "#haspel", uv=[0, 0, 16, 2], zonder=("west", "east")),
        doos([0.5, 6, -4.75], [15.5, 9, -4.25], "#haspel", uv=blad),
        doos([0.5, 1, -4.75], [15.5, 4, -4.25], "#haspel", uv=blad),
        doos([0.5, 4.75, -8.5], [15.5, 5.25, -5.5], "#haspel", uv=blad),
        doos([0.5, 4.75, -3.5], [15.5, 5.25, -0.5], "#haspel", uv=blad),
    ]
    tex = lambda staat: _tex("oogster", voor=f"oogster_voor_{staat}", hoed="oogster_hoed", haspel="oogster_haspel")
    for staat in STATEN:
        h.w(f"{h.A}/models/block/oogster_{staat}.json", model(tex(staat), vast(staat)))
    h.w(f"{h.A}/models/block/oogster_haspel.json", model({"particle": "guhs:block/oogster_haspel", "haspel": "guhs:block/oogster_haspel"}, haspel))
    h.w(f"{h.A}/models/item/oogster.json", model(tex("werkt"), vast("werkt") + haspel))


# =====================================================================================================================
# Knabbelaar
# =====================================================================================================================
def _knabbelaar_voor(img, staat):
    """A big open mouth instead of the hatch (dark, with a tongue); asleep: the mouth is nearly shut."""
    basis, accent = KLEUR["knabbelaar"]
    px = img.load()
    for y in range(8, 15):
        for x in range(3, 13):
            px[x, y] = tuple(img.getpixel((1, 2))[:3]) + (255,)      # (wipe the hatch away)
    donker, tong = (62, 22, 40), (246, 128, 156)
    rijen = range(10, 12) if staat == "slaapt" else range(8, 15)
    for y in rijen:
        for x in range(3, 13):
            if (x in (3, 12)) and (y in (8, 14)):
                continue
            px[x, y] = donker + (255,)
    if staat != "slaapt":
        for y in (12, 13):
            for x in range(5, 11):
                px[x, y] = tong + (255,)
        px[7, 12] = px[8, 12] = _tint(tong, 0.82) + (255,)


def knabbelaar(h):
    basis, accent = KLEUR["knabbelaar"]
    vadskracht.machine(h, "knabbelaar", basis, accent, voor=_knabbelaar_voor)
    # rows 0..7: tooth enamel (a shine on the left, a shadow on the right and at the tip); rows 8..15: the gum / lip
    a = np.zeros((16, 16, 4), np.uint8)
    _vul(a, 0, 0, 15, 7, WIT)
    for x0 in (0, 4, 8, 12):
        a[0:8, x0, :3] = (255, 255, 255)
        a[0:8, x0 + 3, :3] = (214, 210, 196)
    a[7, :, :3] = (226, 222, 206)
    _vul(a, 0, 8, 15, 15, accent)
    a[8, :, :3] = _tint(accent, 1.18)
    a[15, :, :3] = _tint(accent, 0.74)
    h.save(_png(a), "block", "knabbelaar_tand.png")

    lip, glazuur = [0, 8, 10, 10], [0, 0, 3, 4]
    vast = [lijf(), *vadskracht.oren("#boven"),
            doos([3, 7, -1.5], [13, 8.5, 0], "#tand", uv=lip, zonder=("south",))]
    tanden = [doos([4.5, 3.25, -1.25], [7.5, 7, -0.25], "#tand", uv=glazuur),
              doos([8.5, 3.25, -1.25], [11.5, 7, -0.25], "#tand", uv=[4, 0, 7, 4])]
    kaak = [doos([3, 1, -1.5], [13, 2.5, 0], "#tand", uv=[0, 12, 10, 14], zonder=("south",)),
            doos([3.4, 2.5, -1.25], [4.4, 3.75, -0.25], "#tand", uv=[8, 4, 9, 6]),
            doos([7.6, 2.5, -1.25], [8.4, 3.75, -0.25], "#tand", uv=[9, 4, 10, 6]),
            doos([11.6, 2.5, -1.25], [12.6, 3.75, -0.25], "#tand", uv=[10, 4, 11, 6])]
    tex = lambda staat: _tex("knabbelaar", voor=f"knabbelaar_voor_{staat}", tand="knabbelaar_tand")
    deel = {"particle": "guhs:block/knabbelaar_tand", "tand": "guhs:block/knabbelaar_tand"}
    for staat in STATEN:
        h.w(f"{h.A}/models/block/knabbelaar_{staat}.json", model(tex(staat), vast))
    h.w(f"{h.A}/models/block/knabbelaar_tanden.json", model(deel, tanden))
    h.w(f"{h.A}/models/block/knabbelaar_kaak.json", model(deel, kaak))
    h.w(f"{h.A}/models/item/knabbelaar.json", model(tex("werkt"), vast + tanden + kaak))


# =====================================================================================================================
# Neerzetter
# =====================================================================================================================
def _neerzetter_voor(img, staat):
    """The hatch is an open hole (the arm comes out of it)."""
    px = img.load()
    for y in range(9, 14):
        for x in range(4, 12):
            px[x, y] = (34, 44, 78, 255)
    for x in range(4, 12):
        px[x, 9] = (22, 30, 56, 255)


def neerzetter(h):
    basis, accent = KLEUR["neerzetter"]
    vadskracht.machine(h, "neerzetter", basis, accent, voor=_neerzetter_voor)
    # rows 0..7: the pusher plate (accent, a darker rim, a plus sign); rows 8..15: iron (the rod and the frame)
    a = np.zeros((16, 16, 4), np.uint8)
    _vul(a, 0, 0, 15, 7, accent)
    a[0, :, :3] = _tint(accent, 1.12)
    a[7, :, :3] = _tint(accent, 0.7)
    a[0:8, 0, :3] = _tint(accent, 0.8)
    a[0:8, 6, :3] = _tint(accent, 0.8)
    _vul(a, 3, 2, 3, 5, _tint(accent, 0.62))
    _vul(a, 2, 3, 4, 4, _tint(accent, 0.62))
    b = _ruis(IJZER, 4, _zaad("arm"))
    a[8:16] = b[8:16]
    a[8, :, :3] = _tint(IJZER, 1.2)
    a[15, :, :3] = _tint(IJZER, 0.7)
    h.save(_png(a), "block", "neerzetter_arm.png")

    ijzer = [0, 9, 9, 10]
    vast = [lijf(), *vadskracht.oren("#boven"),
            doos([3, 7, -1], [13, 8, 0], "#arm", uv=[0, 9, 10, 10], zonder=("south",)),
            doos([3, 1, -1], [13, 2, 0], "#arm", uv=[0, 13, 10, 14], zonder=("south",)),
            doos([3, 2, -1], [4, 7, 0], "#arm", uv=[0, 9, 1, 14], zonder=("south",)),
            doos([12, 2, -1], [13, 7, 0], "#arm", uv=[9, 9, 10, 14], zonder=("south",))]
    arm = [doos([4.5, 2.5, -0.75], [11.5, 6.5, -0.25], "#arm", uv=[0, 0, 7, 4]),
           doos([7, 3.5, -0.25], [9, 5.5, 7], "#arm", uv=ijzer, zonder=("north", "south"))]
    tex = lambda staat: _tex("neerzetter", voor=f"neerzetter_voor_{staat}", arm="neerzetter_arm")
    for staat in STATEN:
        h.w(f"{h.A}/models/block/neerzetter_{staat}.json", model(tex(staat), vast))
    h.w(f"{h.A}/models/block/neerzetter_arm.json", model({"particle": "guhs:block/neerzetter_arm", "arm": "guhs:block/neerzetter_arm"}, arm))
    h.w(f"{h.A}/models/item/neerzetter.json", model(tex("werkt"), vast + arm))


# =====================================================================================================================
# Vadsmolen (two blocks high)
# =====================================================================================================================
def _vadsmolen_voor(img, staat):
    """The hatch becomes the spout the meel falls out of."""
    basis, accent = KLEUR["vadsmolen"]
    px = img.load()
    for y in range(9, 15):
        for x in range(4, 12):
            px[x, y] = tuple(img.getpixel((1, 2))[:3]) + (255,)
    for i, y in enumerate(range(10, 14)):
        for x in range(5 + i // 2, 11 - i // 2):
            px[x, y] = (_tint(HOUT, 0.8) if y < 13 else (250, 244, 226)) + (255,)
    for x in range(6, 10):
        px[x, 14] = (250, 244, 226, 255)        # a little heap of meel


def vadsmolen(h):
    basis, accent = KLEUR["vadsmolen"]
    vadskracht.machine(h, "vadsmolen", basis, accent, voor=_vadsmolen_voor)
    # the tower: cream bricks, a little round window
    a = _ruis(_tint(basis, 1.04), 4, _zaad("toren"))
    for y in (3, 7, 11, 15):
        a[y, :, :3] = _tint(basis, 0.8)
    for rij, y0 in enumerate((0, 4, 8, 12)):
        for x in ((4, 12) if rij % 2 == 0 else (0, 8)):
            a[y0:y0 + 3, x, :3] = _tint(basis, 0.8)
    _vul(a, 6, 5, 9, 9, _tint(basis, 0.62))
    _vul(a, 7, 6, 8, 8, (70, 50, 70))
    a[6, 7, :3] = (150, 190, 230)
    h.save(_png(a), "block", "vadsmolen_toren.png")
    # the roof: shingles
    a = _ruis(accent, 5, _zaad("dak"))
    for y in range(16):
        if y % 4 == 3:
            a[y, :, :3] = _tint(accent, 0.68)
        for x in range(16):
            if y % 4 != 3 and (x + (y // 4) * 2) % 4 == 0:
                a[y, x, :3] = _tint(accent, 0.8)
    h.save(_png(a), "block", "vadsmolen_dak.png")
    # the sails: rows 0..3 the wooden spar, rows 4..15 the cloth on its lattice
    a = np.zeros((16, 16, 4), np.uint8)
    a[0:4] = _ruis(HOUT, 5, _zaad("wiek"))[0:4]
    a[0, :, :3] = _tint(HOUT, 1.2)
    a[3, :, :3] = _tint(HOUT, 0.7)
    _vul(a, 0, 4, 15, 15, (246, 240, 226))
    for y in range(4, 16):
        for x in range(16):
            if x % 3 == 0 or (y - 4) % 3 == 0:
                a[y, x, :3] = _tint(HOUT, 1.1)
    h.save(_png(a), "block", "vadsmolen_wiek.png")

    muur = {v: [2, 2, 14, 14] for v in ("north", "south", "west", "east")}
    vast = [
        lijf(14),
        doos([2, 14, 2], [14, 26, 14], "#toren", uv=muur, zonder=("up", "down")),
        doos([1, 26, 1], [15, 28, 15], "#dak", uv={"up": [1, 1, 15, 15], "down": [1, 1, 15, 15], "north": [1, 0, 15, 2], "south": [1, 0, 15, 2],
                                                    "west": [1, 0, 15, 2], "east": [1, 0, 15, 2]}),
        doos([3, 28, 3], [13, 30, 13], "#dak", uv={"up": [3, 3, 13, 13], "north": [3, 4, 13, 6], "south": [3, 4, 13, 6], "west": [3, 4, 13, 6],
                                                    "east": [3, 4, 13, 6]}, zonder=("down",)),
        doos([5, 30, 5], [11, 32, 11], "#dak", uv={"up": [5, 5, 11, 11], "north": [5, 8, 11, 10], "south": [5, 8, 11, 10], "west": [5, 8, 11, 10],
                                                    "east": [5, 8, 11, 10]}, zonder=("down",)),
        doos([3.5, 28, 1.5], [6.5, 31, 3], "#oor", uv=[0, 0, 4, 3]),
        doos([9.5, 28, 1.5], [12.5, 31, 3], "#oor", uv=[0, 0, 4, 3]),
        doos([7, 21, 0], [9, 23, 2], "#wiek", uv=[0, 0, 2, 2], zonder=("south",)),
    ]
    spar, doek = [0, 0, 8, 1], [0, 4, 6, 7]
    wieken = [
        doos([6.5, 20.5, -1.75], [9.5, 23.5, -0.25], "#wiek", uv=[0, 0, 3, 3]),
        doos([7.5, 23.5, -1.25], [8.5, 31, -0.75], "#wiek", uv=[0, 0, 1, 8]),
        doos([8.5, 25, -1.2], [11.5, 31, -0.8], "#wiek", uv=[0, 4, 3, 10]),
        doos([9.5, 21.5, -1.25], [17, 22.5, -0.75], "#wiek", uv=spar),
        doos([11, 18.5, -1.2], [17, 21.5, -0.8], "#wiek", uv=doek),
        doos([7.5, 13, -1.25], [8.5, 20.5, -0.75], "#wiek", uv=[0, 0, 1, 8]),
        doos([4.5, 13, -1.2], [7.5, 19, -0.8], "#wiek", uv=[0, 4, 3, 10]),
        doos([-1, 21.5, -1.25], [6.5, 22.5, -0.75], "#wiek", uv=spar),
        doos([-1, 22.5, -1.2], [5, 25.5, -0.8], "#wiek", uv=doek),
    ]
    tex = lambda staat: _tex("vadsmolen", voor=f"vadsmolen_voor_{staat}", toren="vadsmolen_toren", dak="vadsmolen_dak", wiek="vadsmolen_wiek")
    for staat in STATEN:
        h.w(f"{h.A}/models/block/vadsmolen_{staat}.json", model(tex(staat), vast))
    h.w(f"{h.A}/models/block/vadsmolen_wieken.json", model({"particle": "guhs:block/vadsmolen_wiek", "wiek": "guhs:block/vadsmolen_wiek"}, wieken))
    # (two blocks high: smaller in the hand and in a slot)
    klein = {"gui": {"rotation": [30, 225, 0], "translation": [0, -3.25, 0], "scale": [0.42, 0.42, 0.42]},
             "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.2, 0.2, 0.2]},
             "fixed": {"rotation": [0, 180, 0], "translation": [0, -4, 0], "scale": [0.4, 0.4, 0.4]},
             "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 1.5, 0], "scale": [0.28, 0.28, 0.28]},
             "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
             "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]}}
    h.w(f"{h.A}/models/item/vadsmolen.json", model(tex("werkt"), vast + wieken, display=klein))


# =====================================================================================================================
# Knutselmachine
# =====================================================================================================================
def _knutselmachine_voor(img, staat):
    """The hatch becomes a drawer with a knob."""
    basis, accent = KLEUR["knutselmachine"]
    px = img.load()
    for y in range(9, 14):
        for x in range(3, 13):
            px[x, y] = _tint(basis, 1.12) + (255,)
    for x in range(3, 13):
        px[x, 9] = px[x, 13] = _tint(basis, 0.66) + (255,)
    for y in range(9, 14):
        px[3, y] = px[12, y] = _tint(basis, 0.66) + (255,)
    px[7, 11] = px[8, 11] = (accent if staat != "slaapt" else _tint(accent, 0.6)) + (255,)


def knutselmachine(h):
    basis, accent = KLEUR["knutselmachine"]
    vadskracht.machine(h, "knutselmachine", basis, accent, voor=_knutselmachine_voor)
    # the worktop: light wood, a dark rim, the 3 x 3 grid of a crafting table
    licht = _tint(basis, 1.14)
    a = _ruis(licht, 4, _zaad("blad"))
    _rand(a, _tint(basis, 0.62))
    for k in (5, 10):
        a[2:14, k, :3] = _tint(basis, 0.7)
        a[k, 2:14, :3] = _tint(basis, 0.7)
    h.save(_png(a), "block", "knutselmachine_blad.png")
    # rows 0..5: the hammer's handle (wood); rows 6..11: its head (iron); rows 12..15: the workpiece (accent)
    a = np.zeros((16, 16, 4), np.uint8)
    a[0:6] = _ruis(HOUT, 5, _zaad("hamer"))[0:6]
    a[0, :, :3] = _tint(HOUT, 1.2)
    a[5, :, :3] = _tint(HOUT, 0.7)
    a[6:12] = _ruis(IJZER, 4, _zaad("kop"))[6:12]
    a[6, :, :3] = _tint(IJZER, 1.25)
    a[11, :, :3] = _tint(IJZER, 0.66)
    _vul(a, 0, 12, 15, 15, accent)
    a[12, :, :3] = _tint(accent, 1.2)
    a[15, :, :3] = _tint(accent, 0.7)
    h.save(_png(a), "block", "knutselmachine_hamer.png")

    rand = [0, 0, 16, 2]
    vast = [
        lijf(13, boven="#blad"),
        doos([0, 13, 0], [16, 15, 16], "#blad", uv={"up": [0, 0, 16, 16], "north": rand, "south": rand, "west": rand, "east": rand}, zonder=("down",)),
        *oren((2, 10), 15, 0.5),
        doos([6.5, 15, 6.5], [9.5, 17.5, 9.5], "#hamer", uv=[0, 12, 3, 15], zonder=("down",)),
        doos([3, 15, 13.5], [4, 17.5, 14.5], "#hamer", uv=[0, 1, 1, 4], zonder=("down",)),
        doos([12, 15, 13.5], [13, 17.5, 14.5], "#hamer", uv=[0, 1, 1, 4], zonder=("down",)),
    ]

    def hamer(x):
        return [doos([x, 16.5, 7.5], [x + 1, 17.5, 14], "#hamer", uv=[0, 1, 7, 2]),
                doos([x - 0.5, 15.25, 6], [x + 1.5, 18.25, 8], "#hamer", uv=[0, 7, 2, 10])]
    tex = lambda staat: _tex("knutselmachine", voor=f"knutselmachine_voor_{staat}", blad="knutselmachine_blad", hamer="knutselmachine_hamer")
    deel = {"particle": "guhs:block/knutselmachine_hamer", "hamer": "guhs:block/knutselmachine_hamer"}
    for staat in STATEN:
        h.w(f"{h.A}/models/block/knutselmachine_{staat}.json", model(tex(staat), vast))
    h.w(f"{h.A}/models/block/knutselmachine_hamer_links.json", model(deel, hamer(3)))
    h.w(f"{h.A}/models/block/knutselmachine_hamer_rechts.json", model(deel, hamer(12)))
    h.w(f"{h.A}/models/item/knutselmachine.json", model(tex("werkt"), vast + hamer(3) + hamer(12)))


# =====================================================================================================================
# Tekentafel (no vadskracht: its own blockstate, no vadskracht tag)
# =====================================================================================================================
def tekentafel(h):
    basis, accent = KLEUR["tekentafel"]
    for staat in STATEN:
        a = _planken(basis, _zaad("tafel"), lagen=(10,))
        _rand(a, _tint(basis, 0.62))
        img = _png(a)
        vadskracht.snoet(img, 2, 4, staat)
        h.save(img, "block", f"tekentafel_voor_{staat}.png")
    a = _planken(basis, _zaad("tafelzij"), lagen=(5, 10))
    _rand(a, _tint(basis, 0.62))
    h.save(_png(a), "block", "tekentafel_zij.png")
    # the board: a wooden rim around a blue sheet with a drawing in white lines (a little guh: two ears, a round head)
    a = _ruis(_tint(basis, 1.12), 4, _zaad("bord"))
    _rand(a, _tint(basis, 0.62))
    _vul(a, 2, 2, 13, 12, accent)
    wit = (232, 242, 255)
    for x in range(3, 13, 3):                       # the faint grid of drawing paper
        a[2:13, x, :3] = _tint(accent, 1.16)
    for y in range(3, 13, 3):
        a[y, 2:14, :3] = _tint(accent, 1.16)
    # the pencil's colours on row 14 and 15 of the rim: yellow with a pink rubber and a dark point
    for x in range(1, 15):
        a[14, x, :3] = (250, 214, 80)
    a[14, 1, :3] = (240, 130, 160)
    a[14, 13, :3] = (228, 196, 150)
    a[14, 14, :3] = (60, 50, 56)
    # the drawing, in the order the table draws it: the ears, the head from the top down, the eyes last
    punten = ((6, 4), (9, 4), (5, 5), (10, 5), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (4, 7), (11, 7), (4, 8), (11, 8), (4, 9), (11, 9),
              (5, 10), (6, 10), (7, 10), (8, 10), (9, 10), (10, 10), (6, 8), (9, 8))

    def bord(n, punt=False):
        """The board with the first n points drawn (punt: the newest one is the pencil's point, still yellow)."""
        b = a.copy()
        for x, y in punten[:n]:
            b[y, x, :3] = wit
        if punt and n:
            x, y = punten[n - 1]
            b[y, x, :3] = (255, 226, 110)
        return b
    # asleep: an empty sheet. Somebody at the table: the drawing draws itself, over and over (an animated texture: frame
    # 0 is the finished drawing, frames 1..STAPPEN build it up). A recipe it knows: the finished drawing (also the item).
    h.save(_png(bord(len(punten))), "block", "tekentafel_blad.png")
    h.save(_png(bord(0)), "block", "tekentafel_blad_leeg.png")
    strook = np.zeros((16 * (TEKEN_STAPPEN + 1), 16, 4), np.uint8)
    strook[0:16] = bord(len(punten))
    for i in range(1, TEKEN_STAPPEN + 1):
        strook[16 * i:16 * i + 16] = bord(len(punten) * i // TEKEN_STAPPEN, punt=True)
    h.save(_png(strook), "block", "tekentafel_blad_tekent.png")
    h.w(os.path.join(h.TEX, "block", "tekentafel_blad_tekent.png.mcmeta"), {"animation": {"frames": [
        *({"index": i, "time": 4} for i in range(1, TEKEN_STAPPEN + 1)), {"index": 0, "time": 24}]}})

    schuin = {"origin": [8, 12.25, 8], "axis": "x", "angle": -22.5}
    hout = [0, 0, 16, 2]

    def elementen():
        return [
            doos([1, 0, 2], [15, 9, 15], {"north": "#voor", "south": "#zij", "west": "#zij", "east": "#zij", "up": "#zij", "down": "#zij"},
                 uv={"north": [1, 2, 15, 11]}),
            # the wedge under the sloping board, in four steps (so the desk is closed from the side and from behind)
            doos([1, 9, 5], [15, 10.3, 15], "#zij", zonder=("down",)),
            doos([1, 10.3, 8], [15, 11.4, 15], "#zij", zonder=("down",)),
            doos([1, 11.4, 11], [15, 12.6, 15], "#zij", zonder=("down",)),
            doos([1, 12.6, 13.5], [15, 13.7, 15], "#zij", zonder=("down",)),
            doos([0, 11.5, 0], [16, 13, 16], {"up": "#blad", "down": "#zij", "north": "#blad", "south": "#blad", "west": "#blad", "east": "#blad"},
                 uv={"up": [0, 0, 16, 16], "north": [0, 0, 16, 1], "south": [0, 15, 16, 16], "west": [0, 0, 1, 16], "east": [15, 0, 16, 16]}, rot=schuin),
            doos([3, 13, 12], [11, 13.7, 12.8], "#blad", uv={"up": [1, 14, 15, 15], "north": [1, 14, 15, 15], "south": [1, 14, 15, 15],
                                                             "west": [1, 14, 2, 15], "east": [14, 14, 15, 15]}, zonder=("down",), rot=schuin),
            *oren((2, 10), 14.5, 13.5, hoog=3.5),
        ]
    blad = {"slaapt": "tekentafel_blad_leeg", "werkt": "tekentafel_blad_tekent", "vol": "tekentafel_blad"}
    tex = lambda staat: {"particle": "guhs:block/tekentafel_zij", "voor": f"guhs:block/tekentafel_voor_{staat}", "zij": "guhs:block/tekentafel_zij",
                         "blad": f"guhs:block/{blad[staat]}", "oor": "guhs:block/techmachine_oor"}
    for staat in STATEN:
        h.w(f"{h.A}/models/block/tekentafel_{staat}.json", model(tex(staat), elementen()))
    h.w(f"{h.A}/blockstates/tekentafel.json", {"variants": {
        f"facing={f},snoet={staat}": {"model": f"guhs:block/tekentafel_{staat}", **({"y": r} if r else {})}
        for f, r in ROT.items() for staat in STATEN}})
    h.w(f"{h.A}/models/item/tekentafel.json", {"parent": "guhs:block/tekentafel_vol"})
    h.self_drop("tekentafel")
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:tekentafel"])


# =====================================================================================================================
# Plantagebak (3 x 3: the kern + block techmachine_plantagebak_deel)
# =====================================================================================================================
def plantagebak(h):
    basis, accent = KLEUR["plantagebak"]
    plank = _planken(basis, _zaad("bak"))
    h.save(_png(plank), "block", "plantagebak_plank.png")
    for staat in STATEN:
        a = plank.copy()
        a[12:14, 1:15, :3] = _tint(accent, 0.6 if staat == "slaapt" else 1.0)      # a green band: it is a guh machine all the same
        img = _png(a)
        vadskracht.snoet(img, 2, 5, staat)
        h.save(img, "block", f"plantagebak_voor_{staat}.png")
    # the soil: dark earth, a few crumbs and pebbles, some little green sprouts
    rng = np.random.default_rng(_zaad("aarde"))
    a = _ruis(AARDE, 7, _zaad("aarde"))
    for _ in range(14):
        x, y = rng.integers(0, 16, 2)
        a[y, x, :3] = _tint(AARDE, 1.35 if rng.random() < 0.6 else 0.7)
    for x, y in ((3, 4), (11, 2), (7, 12), (13, 10)):
        a[y, x, :3] = (120, 190, 96)
    aarde = a
    h.save(_png(aarde), "block", "plantagebak_aarde.png")
    # the rim (seen from above): light wood with a dark line on both sides
    rand = _ruis(_tint(basis, 1.16), 4, _zaad("rand"))
    h.save(_png(rand), "block", "plantagebak_rand.png")
    # the top of an edge piece and of a corner piece: the soil with the rim's shadow along the planks
    for naam, hoek in (("rand", False), ("hoek", True)):
        b = aarde.copy()
        b[0:3, :, :3] = rand[0:3, :, :3]
        b[3, :, :3] = _tint(AARDE, 0.6)
        if hoek:
            b[:, 0:3, :3] = rand[:, 0:3, :3]
            b[3:, 3, :3] = _tint(AARDE, 0.6)
        h.save(_png(b), "block", f"plantagebak_boven_{naam}.png")
    # the watering can: rows 0..7 its body (blue-grey with a band in the accent colour), rows 8..15 spout, rose and handle
    a = _ruis((140, 168, 196), 4, _zaad("gieter"))
    a[0, :, :3] = (186, 208, 228)
    a[7, :, :3] = (98, 122, 150)
    a[3, :, :3] = accent
    a[8:16] = _ruis((116, 142, 172), 4, _zaad("tuit"))[8:16]
    a[8, :, :3] = (160, 186, 210)
    for x in range(0, 16, 2):
        a[12, x, :3] = (70, 90, 116)         # the holes of the rose
    h.save(_png(a), "block", "plantagebak_gieter.png")

    t = lambda **meer: {"particle": "guhs:block/plantagebak_plank", "plank": "guhs:block/plantagebak_plank", "aarde": "guhs:block/plantagebak_aarde",
                        "rand": "guhs:block/plantagebak_rand", "oor": "guhs:block/techmachine_oor", **{k: f"guhs:block/{v}" for k, v in meer.items()}}
    alles = ("north", "south", "west", "east", "down", "up")
    richel = [0, 0, 16, 3]

    def rand_stuk(voor):
        """An edge piece, its planks on the north side: the block, and a rim that stands a pixel above the soil."""
        return [doos([0, 0, 0], [16, 16, 16], {"north": voor, "south": "#aarde", "west": "#aarde", "east": "#aarde", "up": "#boven", "down": "#plank"},
                     cull=alles),
                doos([0, 16, 0], [16, 17, 3], "#rand", uv={"up": richel, "north": [0, 0, 16, 1], "south": [0, 2, 16, 3], "west": [0, 0, 3, 1],
                                                           "east": [0, 0, 3, 1]}, zonder=("down",))]

    hoek = [doos([0, 0, 0], [16, 16, 16], {"north": "#plank", "west": "#plank", "south": "#aarde", "east": "#aarde", "up": "#boven", "down": "#plank"},
                 cull=alles),
            doos([3, 16, 0], [16, 17, 3], "#rand", uv={"up": [3, 0, 16, 3], "north": [0, 0, 13, 1], "south": [3, 2, 16, 3], "east": [0, 0, 3, 1]},
                 zonder=("down", "west")),
            doos([0, 16, 3], [3, 17, 16], "#rand", uv={"up": [0, 3, 3, 16], "west": [3, 0, 16, 1], "east": [0, 2, 13, 3], "south": [0, 0, 3, 1]},
                 zonder=("down", "north")),
            # the corner post with its cap
            doos([-0.5, 0, -0.5], [3.5, 18, 3.5], "#plank", uv={"north": [0, 0, 4, 16], "south": [0, 0, 4, 16], "west": [0, 0, 4, 16],
                                                                "east": [0, 0, 4, 16], "down": [0, 0, 4, 4]}, zonder=("up",)),
            doos([-1, 18, -1], [4, 19, 4], "#rand", uv={"up": [0, 0, 5, 5], "down": [0, 0, 5, 5], "north": [0, 0, 5, 1], "south": [0, 0, 5, 1],
                                                         "west": [0, 0, 5, 1], "east": [0, 0, 5, 1]})]
    midden = [doos([0, 0, 0], [16, 16, 16], {"north": "#aarde", "south": "#aarde", "west": "#aarde", "east": "#aarde", "up": "#aarde",
                                             "down": "#plank"}, cull=alles)]
    gieter = [doos([6.5, 17, 0.3], [9.5, 20, 2.7], "#gieter", uv=[0, 1, 3, 4]),
              doos([7.5, 18.5, 2.7], [8.5, 19.5, 5], "#gieter", uv=[0, 9, 2, 10], zonder=("north",)),
              doos([7, 18, 5], [9, 20, 5.6], "#gieter", uv=[0, 11, 2, 13]),
              doos([7.5, 18, -0.7], [8.5, 19.8, 0.3], "#gieter", uv=[4, 9, 5, 11], zonder=("south",))]

    def kern(staat):
        return rand_stuk("#voor") + oren((1.5, 10.5), 17, 0.5)

    for staat in STATEN:
        h.w(f"{h.A}/models/block/plantagebak_{staat}.json",
            model(t(voor=f"plantagebak_voor_{staat}", boven="plantagebak_boven_rand"), kern(staat)))
    h.w(f"{h.A}/models/block/plantagebak_rand.json", model(t(boven="plantagebak_boven_rand"), rand_stuk("#plank")))
    h.w(f"{h.A}/models/block/plantagebak_hoek.json", model(t(boven="plantagebak_boven_hoek"), hoek))
    h.w(f"{h.A}/models/block/plantagebak_midden.json", model(t(), midden))
    h.w(f"{h.A}/models/block/plantagebak_gieter.json",
        model({"particle": "guhs:block/plantagebak_gieter", "gieter": "guhs:block/plantagebak_gieter"}, gieter))
    h.w(f"{h.A}/blockstates/plantagebak.json", {"variants": {
        f"facing={f},snoet={staat}": {"model": f"guhs:block/plantagebak_{staat}", **({"y": r} if r else {})}
        for f, r in ROT.items() for staat in STATEN}})
    # the eight other blocks: which piece of the bed (PlantagebakDeelBlock.Stuk), by the compass. The corner model is the
    # north-west corner, the edge model the north edge.
    draai = lambda m, y: {"model": f"guhs:block/plantagebak_{m}", **({"y": y} if y else {})}
    h.w(f"{h.A}/blockstates/techmachine_plantagebak_deel.json", {"variants": {
        "stuk=nw": draai("hoek", 0), "stuk=no": draai("hoek", 90), "stuk=zo": draai("hoek", 180), "stuk=zw": draai("hoek", 270),
        "stuk=n": draai("rand", 0), "stuk=o": draai("rand", 90), "stuk=z": draai("rand", 180), "stuk=w": draai("rand", 270),
        "stuk=midden": draai("midden", 0)}})
    # the item: the kern as you see it from the front, with the watering can
    h.w(f"{h.A}/models/item/plantagebak.json",
        model(t(voor="plantagebak_voor_werkt", boven="plantagebak_boven_rand", gieter="plantagebak_gieter"), kern("werkt") + gieter))
    h.self_drop("plantagebak")
    vadskracht.toon(h, "plantagebak", "techmachine_plantagebak_deel")
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:plantagebak", "guhs:techmachine_plantagebak_deel"])


# =====================================================================================================================
# the Bouwtekening (an item: an empty sheet, and one with a drawing: custom_model_data 1, set by Bouwtekeningen.metTekening)
# =====================================================================================================================
def bouwtekening(h):
    def vel(getekend):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        px = img.load()
        papier, rand, licht = (92, 142, 216), (56, 96, 170), (132, 176, 236)
        for y in range(2, 15):
            for x in range(2, 14):
                px[x, y] = papier + (255,)
        for x in range(2, 14):
            px[x, 2] = licht + (255,)
            px[x, 14] = rand + (255,)
        for y in range(2, 15):
            px[2, y] = licht + (255,)
            px[13, y] = rand + (255,)
        # a curled corner at the bottom right
        px[13, 14] = (0, 0, 0, 0)
        px[12, 14] = px[13, 13] = px[12, 13] = (196, 216, 244, 255)
        if getekend:
            wit = (236, 244, 255, 255)
            for x, y in ((5, 4), (9, 4), (4, 5), (5, 5), (9, 5), (10, 5), (4, 6), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6),
                         (3, 7), (11, 7), (3, 8), (11, 8), (3, 9), (11, 9), (4, 10), (5, 10), (6, 10), (7, 10), (8, 10), (9, 10), (10, 10),
                         (5, 8), (9, 8), (7, 9)):
                px[x, y] = wit                       # a little guh, drawn in white lines
            for x in range(4, 11, 2):
                px[x, 12] = (176, 204, 244, 255)     # a row of measurements
        else:
            for x in range(4, 12, 3):
                for y in range(4, 13, 3):
                    px[x, y] = licht + (255,)        # the dots of empty drawing paper
        return img
    h.save(vel(False), "item", "bouwtekening.png")
    h.save(vel(True), "item", "bouwtekening_getekend.png")
    h.item_model("bouwtekening_getekend")
    h.w(f"{h.A}/models/item/bouwtekening.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/bouwtekening"},
                                                  "overrides": [{"predicate": {"custom_model_data": 1}, "model": "guhs:item/bouwtekening_getekend"}]})


DELEN = ("oogster_haspel", "knabbelaar_tanden", "knabbelaar_kaak", "neerzetter_arm", "vadsmolen_wieken", "knutselmachine_hamer_links",
         "knutselmachine_hamer_rechts", "plantagebak_gieter")
MACHINES = ("oogster", "knabbelaar", "neerzetter", "vadsmolen", "knutselmachine", "tekentafel", "plantagebak")


def build(h):
    oor_textuur(h)
    oogster(h)
    knabbelaar(h)
    neerzetter(h)
    vadsmolen(h)
    knutselmachine(h)
    tekentafel(h)
    plantagebak(h)
    bouwtekening(h)
