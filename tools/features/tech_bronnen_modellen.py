"""
bbq2 (tech-bronnen): the textures and block models of the vadskracht sources (called by features/tech_bronnen.py).

  knuffelgenerator(h)   the pink cushion of 2 x 2 blocks: ONE model drawn by the kern (it reaches from 0 to 32 in x and z),
                        three faces embroidered on top (and a small one on the front); its three part blocks are invisible
  disco_dynamo(h)       the dance floor of 3 x 3: every block draws its own tile (a block model cannot reach three blocks);
                        the kern's tile carries the DJ desk with the face, the ears and the turntable. The lit lamps are two
                        loose models (disco_dynamo_lampen / _lampen_kern: one tint index per lamp) that the block entity
                        renderer draws per tile in the colours of the disc's light show
  blubkacheltje(h)      a little iron stove with a glass jar on top; the Sausblubje in the jar is the loose model
                        blubkacheltje_blubje (drawn bobbing by the renderer)
  gloeisterkern(h)      a charcoal foot with a face and a cage of zoutkristal; the star is the loose model gloeisterkern_ster
                        (drawn turning by the renderer; the item model has it standing still inside)
  knabbelbatterij(h)    vadskracht.batterij in kaasknabbel colours, with a bite out of every corner

Every model has its front on the north side, like every guh machine; the blockstates turn them (facing x snoet).
Textures that are atlases are 32 x 32: a texel is half a uv unit (uv32).
"""
import os

import numpy as np
from PIL import Image

from features import vadskracht

ROT = vadskracht.ROT
STATEN = vadskracht.STATEN
OOR, OOR_BINNEN = vadskracht.OOR, vadskracht.OOR_BINNEN

ROZE = (244, 150, 190)
ROZE_L = (255, 196, 220)
ROZE_D = (214, 108, 156)
NAAD = (196, 88, 138)
DONKER = (48, 36, 74)          # the frame of the dance floor
GLAS_UIT = (88, 72, 124)       # a lamp that is off
NEON = (255, 84, 204)
DESK = (136, 98, 214)
IJZER = (76, 72, 86)
IJZER_L = (112, 108, 124)
IJZER_D = (48, 45, 56)
GLOED = (255, 150, 50)
GLOED_L = (255, 216, 96)
SAUS = (246, 176, 60)
SAUS_L = (255, 220, 120)
SAUS_D = (214, 132, 38)
KURK = (176, 132, 84)
STEEN = (64, 58, 70)
STEEN_L = (92, 84, 98)
GOUD = (240, 190, 80)
GOUD_D = (188, 136, 44)
ZOUT = (236, 240, 250)
ZOUT_D = (232, 190, 216)
KNABBEL = (238, 180, 84)
KNABBEL_D = (196, 132, 50)


# =====================================================================================================================
# small helpers
# =====================================================================================================================
def _leeg(size=32):
    return np.zeros((size, size, 4), np.uint8)


def _vul(a, x0, y0, x1, y1, kleur, var=0, seed=0):
    """Fills texels x0..x1-1, y0..y1-1 with a colour (and a little noise)."""
    rng = np.random.default_rng(seed)
    h, w = y1 - y0, x1 - x0
    n = rng.normal(0, var, (h, w)) if var else np.zeros((h, w))
    for c in range(3):
        a[y0:y1, x0:x1, c] = np.clip(kleur[c] + n, 0, 255)
    a[y0:y1, x0:x1, 3] = kleur[3] if len(kleur) > 3 else 255


def _px(a, x, y, kleur):
    if 0 <= y < a.shape[0] and 0 <= x < a.shape[1]:
        a[y, x, :3] = kleur[:3]
        a[y, x, 3] = kleur[3] if len(kleur) > 3 else 255


def _tint(kleur, f):
    return tuple(int(max(0, min(255, c * f))) for c in kleur[:3])


def _oor(a, x, y, w=3, h=3):
    """An ear's front (pink with a darker inside) at x, y and its plain other sides right next to it."""
    _vul(a, x, y, x + w, y + h, OOR)
    _vul(a, x + 1, y + 1, x + w - 1, y + h, OOR_BINNEN)
    _vul(a, x + w, y, x + 2 * w, y + h, OOR)


def uv32(x0, y0, x1, y1):
    """Texels of a 32 x 32 texture -> a uv."""
    return [x0 / 2, y0 / 2, x1 / 2, y1 / 2]


def box(frm, to, faces):
    """An element; faces = {face: (texture, uv)}."""
    return {"from": list(frm), "to": list(to), "faces": {f: {"texture": t, "uv": list(uv)} for f, (t, uv) in faces.items()}}


def _oren(x0s, y, z, tex, uv_voor, uv_rest, w=3, h=3, d=2):
    out = []
    for x0 in x0s:
        out.append(box((x0, y, z), (x0 + w, y + h, z + d), {
            "north": (tex, uv_voor), "south": (tex, uv_rest), "west": (tex, uv_rest), "east": (tex, uv_rest), "up": (tex, uv_rest)}))
    return out


def _staten(name, model=None):
    return {"variants": {f"facing={f},snoet={s}": {"model": (model or f"guhs:block/{name}") + f"_{s}", **({"y": r} if r else {})}
                         for f, r in ROT.items() for s in STATEN}}


def _deel(h, name, particle):
    """An invisible part block: only a particle texture (like machine_deel)."""
    h.w(f"{h.A}/models/block/{name}.json", {"textures": {"particle": particle}})
    h.w(f"{h.A}/blockstates/{name}.json", {"multipart": [{"apply": {"model": f"guhs:block/{name}"}}]})


# =====================================================================================================================
# Knuffelgenerator
# =====================================================================================================================
def _kussen_boven(staat):
    a = _leeg()
    _vul(a, 0, 0, 32, 32, ROZE, 4, 4101)
    for y in range(32):                                   # the quilting: stitched diamonds
        for x in range(32):
            if (x + y) % 8 == 0 or (x - y) % 8 == 0:
                _px(a, x, y, _tint(ROZE, 0.9) if (x + y) % 2 else NAAD)
    _vul(a, 3, 9, 29, 23, ROZE_L, 3, 4102)                # a smooth patch for the face
    for x, y in ((3, 9), (28, 9), (3, 22), (28, 22)):     # (rounded corners)
        _px(a, x, y, ROZE)
    for x in range(4, 28, 2):                             # stitches around the patch
        _px(a, x, 9, NAAD)
        _px(a, x + 1, 22, NAAD)
    for x, y in ((8, 4), (24, 4), (16, 4), (8, 27), (24, 27), (16, 27)):   # tufted buttons
        _vul(a, x - 1, y - 1, x + 1, y + 1, ROZE_D)
        _px(a, x - 1, y - 1, ROZE_L)
    # the piping: the outer ring (the top of the cushion's widest layer) and a seam where the crown starts
    for i in range(32):
        for r in (0, 1):
            for x, y in ((i, r), (i, 31 - r), (r, i), (31 - r, i)):
                _px(a, x, y, ROZE_L if r == 0 else _tint(ROZE, 1.06))
    for i in range(2, 30):
        for x, y in ((i, 2), (i, 29), (2, i), (29, i)):
            _px(a, x, y, ROZE_D)
    img = Image.fromarray(a)
    vadskracht.snoet(img, 4, 12, staat, schaal=2)
    return img


def _kussen_zij(staat):
    a = _leeg()
    for y0 in (0, 8):                                     # rows 0..7: a plain side; rows 8..15: the front (with the face)
        _vul(a, 0, y0, 32, y0 + 2, _tint(ROZE, 1.05), 3, 4110 + y0)        # the crown
        _vul(a, 0, y0 + 2, 32, y0 + 6, ROZE, 4, 4111 + y0)                 # the widest layer
        _vul(a, 0, y0 + 6, 32, y0 + 8, ROZE_D, 3, 4112 + y0)               # tucked under
        for x in range(32):
            _px(a, x, y0 + 2, ROZE_L)                                      # piping
            if x % 3 == 1:
                _px(a, x, y0 + 5, NAAD)                                    # a row of stitches
    img = Image.fromarray(a)
    vadskracht.snoet(img, 10, 10, staat)                                   # the small face on the front
    a = np.asarray(img).copy()
    _oor(a, 0, 16, 4, 3)
    return Image.fromarray(a)


def knuffelgenerator_elementen(schaal=1.0, dy=0.0):
    """The cushion (front = north; it reaches from 0 to 32 in x and z: the kern is its north-west quarter)."""
    Z, B = "#zij", "#boven"

    def strip(rij, x0, x1, voor):
        y0 = (8 if voor else 0) + rij[0]
        return uv32(x0, y0, x1, (8 if voor else 0) + rij[1])

    def laag(x0, y0, x1, y1, rij, boven=None):
        faces = {"north": (Z, strip(rij, x0, x1, True)), "south": (Z, strip(rij, x0, x1, False)),
                 "west": (Z, strip(rij, x0, x1, False)), "east": (Z, strip(rij, x0, x1, False)),
                 "down": (Z, uv32(0, 6, 2, 8))}
        if boven:
            faces["up"] = (B, boven)
        return box((x0, y0, x0), (x1, y1, x1), faces)

    els = [laag(1, 0, 31, 2, (6, 8)),
           laag(0, 2, 32, 6, (2, 6), boven=[16, 16, 0, 0]),                # (flipped: the face stands upright seen from the front)
           laag(2, 6, 30, 8, (0, 2), boven=[15, 15, 1, 1])]
    # the ears stand up at the back edge, above the embroidered face
    els += _oren((6, 22), 8, 26, Z, uv32(0, 16, 4, 19), uv32(4, 16, 8, 19), w=4, h=3, d=2)
    if schaal != 1.0 or dy:
        for e in els:
            e["from"] = [round(v * schaal + (dy if i == 1 else 0), 3) for i, v in enumerate(e["from"])]
            e["to"] = [round(v * schaal + (dy if i == 1 else 0), 3) for i, v in enumerate(e["to"])]
    return els


def knuffelgenerator(h):
    name = "knuffelgenerator"
    for staat in STATEN:
        h.save(_kussen_boven(staat), "block", f"{name}_boven_{staat}.png")
        h.save(_kussen_zij(staat), "block", f"{name}_zij_{staat}.png")
        tex = {"particle": f"guhs:block/{name}_zij_{staat}", "boven": f"guhs:block/{name}_boven_{staat}", "zij": f"guhs:block/{name}_zij_{staat}"}
        h.w(f"{h.A}/models/block/{name}_{staat}.json", {"parent": "minecraft:block/block", "textures": tex,
                                                        "elements": knuffelgenerator_elementen()})
    h.w(f"{h.A}/blockstates/{name}.json", _staten(name))
    # the item: the whole cushion at half size, in the middle of the slot
    h.w(f"{h.A}/models/item/{name}.json", {"parent": "minecraft:block/block",
                                           "textures": {"particle": f"guhs:block/{name}_zij_werkt", "boven": f"guhs:block/{name}_boven_werkt",
                                                        "zij": f"guhs:block/{name}_zij_werkt"},
                                           "elements": knuffelgenerator_elementen(0.5, 5.0)})
    _deel(h, "techbron_kussen_deel", f"guhs:block/{name}_zij_werkt")


# =====================================================================================================================
# Disco-dynamo
# =====================================================================================================================
LAMPEN = ((9, 9), (1, 9), (9, 1), (1, 1))     # x, z of the four lamps of a tile: back left, back right, front left, front right
                                               # (seen from the front, looking south: left is east = high x, the back is high z)
REGENBOOG = ((255, 80, 80), (255, 170, 60), (255, 236, 90), (110, 230, 110), (90, 200, 255), (190, 120, 255))


def _tegel(kern):
    a = _leeg(16)
    _vul(a, 0, 0, 16, 16, DONKER, 3, 4201)
    for x0, z0 in LAMPEN:
        if kern and z0 < 8:
            continue                                      # (under the desk)
        _vul(a, x0, z0, x0 + 6, z0 + 6, GLAS_UIT, 2, 4202 + x0 + z0)
        _vul(a, x0, z0, x0 + 6, z0 + 1, _tint(GLAS_UIT, 1.25))
        _vul(a, x0, z0, x0 + 1, z0 + 6, _tint(GLAS_UIT, 1.25))
        _px(a, x0 + 1, z0 + 1, _tint(GLAS_UIT, 1.6))
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15), (7, 7), (8, 8), (7, 8), (8, 7)):   # rivets
        _px(a, x, y, _tint(DONKER, 1.7))
    if kern:
        for x in range(3, 13):                            # a cable duct under the desk
            _px(a, x, 7, _tint(DONKER, 0.6))
    return Image.fromarray(a)


def _rand():
    a = _leeg(16)
    _vul(a, 0, 0, 16, 16, DONKER, 3, 4210)
    for x in range(16):
        _px(a, x, 0, _tint(DONKER, 1.5))
        _px(a, x, 1, NEON if x % 4 != 3 else _tint(NEON, 0.5))
        _px(a, x, 3, _tint(DONKER, 0.7))
    return Image.fromarray(a)


def _lamp():
    a = _leeg(16)
    _vul(a, 0, 0, 16, 16, (255, 255, 255))
    for i in range(16):                                   # a darker rim and a soft shine, so a tinted lamp still looks like glass
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            _px(a, x, y, (205, 205, 205))
        for x, y in ((i, 1), (i, 14), (1, i), (14, i)):
            if 0 < i < 15:
                _px(a, x, y, (232, 232, 232))
    return Image.fromarray(a)


def _desk(staat):
    a = _leeg()
    # front (0,0)-(12,8): the face over the whole width, a speaker grille below it
    _vul(a, 0, 0, 12, 8, DESK, 4, 4220)
    _vul(a, 0, 0, 12, 1, _tint(DESK, 1.25))
    _vul(a, 0, 7, 12, 8, _tint(DESK, 0.7))
    for x in range(2, 10, 2):
        _px(a, x, 6, _tint(DESK, 0.6))
    # back (12,0)-(24,8): the buttons the DJ would press
    _vul(a, 12, 0, 24, 8, _tint(DESK, 0.8), 3, 4221)
    _vul(a, 13, 1, 23, 7, IJZER_D)
    for i, kleur in enumerate(((255, 90, 90), (110, 230, 110), (255, 226, 90), (90, 200, 255))):
        _px(a, 14 + 2 * i, 2, kleur if staat != "slaapt" else _tint(kleur, 0.45))
    for x in (14, 17, 20):                                # sliders
        _vul(a, x, 4, x + 1, 7, IJZER)
        _px(a, x, 4 + (x % 3), (230, 230, 240))
    # top (0,8)-(12,14): the turntable's deck with the tonearm
    _vul(a, 0, 8, 12, 14, IJZER_D, 3, 4222)
    _vul(a, 0, 8, 12, 9, _tint(DESK, 1.1))
    for x, y in ((10, 9), (10, 10), (9, 11), (9, 12)):
        _px(a, x, y, (214, 214, 226))                     # the tonearm
    _px(a, 10, 9, (255, 226, 90))
    # the platter (0,14)-(6,19)
    _vul(a, 0, 14, 6, 19, (28, 26, 34))
    for x, y in ((0, 14), (5, 14), (0, 18), (5, 18)):
        _px(a, x, y, IJZER_D)
    _px(a, 2, 16, (120, 120, 132))
    _px(a, 3, 16, (120, 120, 132))
    # side (12,8)-(18,16)
    _vul(a, 12, 8, 18, 16, _tint(DESK, 0.9), 4, 4223)
    _vul(a, 12, 8, 18, 9, _tint(DESK, 1.25))
    _vul(a, 12, 13, 18, 14, NEON if staat != "slaapt" else _tint(NEON, 0.4))
    _vul(a, 12, 15, 18, 16, _tint(DESK, 0.7))
    img = Image.fromarray(a)
    vadskracht.snoet(img, 0, 2, staat)
    a = np.asarray(img).copy()
    _oor(a, 24, 0, 3, 2)
    return Image.fromarray(a)


def _vloer_item():
    """The top of the little dance floor of the item model: 6 x 6 lamps, all on."""
    a = _leeg(24)
    _vul(a, 0, 0, 24, 24, DONKER)
    for rij in range(6):
        for kolom in range(6):
            kleur = REGENBOOG[(rij + kolom) % len(REGENBOOG)]
            _vul(a, kolom * 4 + 1, rij * 4 + 1, kolom * 4 + 4, rij * 4 + 4, kleur)
            _px(a, kolom * 4 + 1, rij * 4 + 1, _tint(kleur, 1.3))
    return Image.fromarray(a)


def _desk_elementen(schaal=1.0, plek=(0.0, 0.0, 0.0)):
    D = "#desk"
    els = [box((2, 4, 0), (14, 12, 6), {"north": (D, uv32(0, 0, 12, 8)), "south": (D, uv32(12, 0, 24, 8)), "west": (D, uv32(12, 8, 18, 16)),
                                        "east": (D, uv32(12, 8, 18, 16)), "up": (D, uv32(0, 8, 12, 14))}),
           box((5, 12, 0.5), (11, 12.25, 5.5), {"up": (D, uv32(0, 14, 6, 19)), "north": (D, uv32(0, 18, 6, 19)), "south": (D, uv32(0, 18, 6, 19)),
                                                "west": (D, uv32(0, 18, 5, 19)), "east": (D, uv32(0, 18, 5, 19))})]
    els += _oren((2, 11), 12, 0, D, uv32(24, 0, 27, 2), uv32(27, 0, 30, 2), w=3, h=2, d=2)
    if schaal != 1.0 or any(plek):
        for e in els:
            e["from"] = [round((v - (8 if i == 0 else 4 if i == 1 else 0)) * schaal + (8 if i == 0 else 0) + plek[i], 3) for i, v in enumerate(e["from"])]
            e["to"] = [round((v - (8 if i == 0 else 4 if i == 1 else 0)) * schaal + (8 if i == 0 else 0) + plek[i], 3) for i, v in enumerate(e["to"])]
    return els


def disco_dynamo(h):
    name = "disco_dynamo"
    h.save(_tegel(False), "block", f"{name}_tegel.png")
    h.save(_tegel(True), "block", f"{name}_tegel_kern.png")
    h.save(_rand(), "block", f"{name}_rand.png")
    h.save(_lamp(), "block", f"{name}_lamp.png")
    h.save(_vloer_item(), "block", f"{name}_vloer_item.png")
    R = "#rand"

    def tegel(boven):
        zij = (R, [0, 0, 16, 4])
        return box((0, 0, 0), (16, 4, 16), {"up": (boven, [0, 0, 16, 16]), "down": (R, [0, 4, 16, 16]), "north": zij, "south": zij,
                                            "west": zij, "east": zij})

    # a floor part: one tile
    h.w(f"{h.A}/models/block/{name}_tegel.json", {"parent": "minecraft:block/block",
                                                  "textures": {"particle": f"guhs:block/{name}_tegel", "tegel": f"guhs:block/{name}_tegel",
                                                               "rand": f"guhs:block/{name}_rand"},
                                                  "elements": [tegel("#tegel")]})
    h.w(f"{h.A}/blockstates/techbron_vloer_deel.json", {"multipart": [{"apply": {"model": f"guhs:block/{name}_tegel"}}]})
    # the kern: its tile with the DJ desk on the front edge
    for staat in STATEN:
        h.save(_desk(staat), "block", f"{name}_desk_{staat}.png")
        h.w(f"{h.A}/models/block/{name}_{staat}.json", {"parent": "minecraft:block/block",
                                                        "textures": {"particle": f"guhs:block/{name}_tegel", "tegel": f"guhs:block/{name}_tegel_kern",
                                                                     "rand": f"guhs:block/{name}_rand", "desk": f"guhs:block/{name}_desk_{staat}"},
                                                        "elements": [tegel("#tegel")] + _desk_elementen()})
    h.w(f"{h.A}/blockstates/{name}.json", _staten(name))

    # the lit lamps (drawn by client/DiscoDynamoRenderer, full bright, one tint index per lamp)
    def lampen(welke):
        return {"textures": {"particle": f"guhs:block/{name}_lamp", "lamp": f"guhs:block/{name}_lamp"},
                "elements": [{"from": [x0, 4.02, z0], "to": [x0 + 6, 4.02, z0 + 6], "shade": False,
                              "faces": {"up": {"texture": "#lamp", "uv": [0, 0, 16, 16], "tintindex": i}}}
                             for i, (x0, z0) in enumerate(LAMPEN) if i in welke]}

    h.w(f"{h.A}/models/block/{name}_lampen.json", lampen((0, 1, 2, 3)))
    h.w(f"{h.A}/models/block/{name}_lampen_kern.json", lampen((0, 1)))
    # the item: a little 6 x 6 floor with every lamp on and the desk in front
    vloer = box((0, 3, 0), (16, 4.5, 16), {"up": ("#vloer", [0, 0, 16, 16]), "down": (R, [0, 4, 16, 16]),
                                           **{f: (R, [0, 0, 16, 2]) for f in ("north", "south", "west", "east")}})
    h.w(f"{h.A}/models/item/{name}.json", {"parent": "minecraft:block/block",
                                           "textures": {"particle": f"guhs:block/{name}_tegel", "vloer": f"guhs:block/{name}_vloer_item",
                                                        "rand": f"guhs:block/{name}_rand", "desk": f"guhs:block/{name}_desk_werkt"},
                                           "elements": [vloer] + _desk_elementen(0.6, (0, 4.5, 0))})


# =====================================================================================================================
# Blubkacheltje
# =====================================================================================================================
def _kachel(staat):
    aan = staat != "slaapt"
    a = _leeg()
    # front (0,0)-(14,8): a pink enamel plate with the face, the glowing vents below it
    _vul(a, 0, 0, 14, 8, IJZER, 4, 4301)
    _vul(a, 1, 0, 13, 6, (244, 176, 204), 3, 4302)
    for x in (1, 12):
        _px(a, x, 0, IJZER_L)                             # (rivets in the plate's top corners)
    _vul(a, 0, 6, 14, 8, IJZER_D)
    for x in range(2, 12, 2):
        _vul(a, x, 6, x + 1, 8, (GLOED if x % 4 else GLOED_L) if aan else (38, 34, 40))
    # side (14,0)-(28,8)
    _vul(a, 14, 0, 28, 8, IJZER, 5, 4303)
    _vul(a, 14, 0, 28, 1, IJZER_L)
    _vul(a, 14, 7, 28, 8, IJZER_D)
    for x in (15, 26):
        for y in (2, 5):
            _px(a, x, y, IJZER_L)                         # rivets
    for x in range(18, 24, 2):
        _vul(a, x, 3, x + 1, 6, GLOED if aan else (38, 34, 40))
    # top (0,8)-(14,22): an iron ring around the hole the jar stands in
    _vul(a, 0, 8, 14, 22, IJZER_L, 4, 4304)
    _vul(a, 2, 10, 12, 20, IJZER_D)
    for i in range(14):
        for x, y in ((i, 8), (i, 21), (0, 8 + i), (13, 8 + i)):
            _px(a, x, y, IJZER)
    # the jar's side (14,8)-(24,18): glass you look through, only its rim and a shine are drawn
    glas, glans = (206, 232, 242, 255), (255, 255, 255, 255)
    for x in range(14, 24):
        _px(a, x, 8, glas)
        _px(a, x, 17, glas)
    for y in range(8, 18):
        _px(a, 14, y, glas)
        _px(a, 23, y, glas)
    for x, y in ((16, 10), (16, 11), (16, 12), (17, 10), (21, 15), (21, 14)):
        _px(a, x, y, glans)
    # the jar's top (14,18)-(24,28): the glass rim around the cork
    for i in range(10):
        for x, y in ((14 + i, 18), (14 + i, 27), (14, 18 + i), (23, 18 + i)):
            _px(a, x, y, glas)
    # the cork (24,8)-(32,16) and its side (24,16)-(32,17)
    _vul(a, 24, 8, 32, 17, KURK, 8, 4305)
    for x, y in ((25, 9), (29, 11), (27, 14), (30, 15)):
        _px(a, x, y, _tint(KURK, 0.75))
    # the stove pipe (0,22)-(3,28), its top (3,22)-(6,24)
    _vul(a, 0, 22, 3, 28, IJZER_D)
    _vul(a, 0, 22, 1, 28, IJZER)
    _vul(a, 0, 22, 3, 23, IJZER_L)
    _vul(a, 3, 22, 6, 24, (24, 22, 28))
    img = Image.fromarray(a)
    vadskracht.snoet(img, 1, 1, staat)
    a = np.asarray(img).copy()
    _oor(a, 26, 20, 3, 3)
    return Image.fromarray(a)


def _blubje():
    a = _leeg(16)
    # front (0,0)-(6,5), side (6,0)-(12,5), top (0,5)-(6,11), the bump's side (6,5)-(10,6)
    _vul(a, 0, 0, 12, 5, SAUS, 5, 4310)
    _vul(a, 0, 0, 12, 1, SAUS_L)
    _vul(a, 0, 4, 12, 5, SAUS_D)
    for x in (1, 4):                                      # two big eyes with a shine
        _vul(a, x, 1, x + 1, 3, (84, 44, 22))
        _px(a, x, 1, (255, 255, 255))
    _px(a, 0, 3, (255, 140, 120))                         # blushing cheeks
    _px(a, 5, 3, (255, 140, 120))
    _px(a, 2, 3, (150, 70, 30))                           # a little smile
    _px(a, 3, 3, (150, 70, 30))
    for x, y in ((7, 2), (10, 1), (9, 3)):
        _px(a, x, y, SAUS_L)                              # glistening drips
    _vul(a, 0, 5, 6, 11, SAUS_L, 4, 4311)
    _vul(a, 1, 6, 3, 8, (255, 244, 190))                  # the shine on top
    _vul(a, 6, 5, 10, 6, SAUS_L)
    return Image.fromarray(a)


def blubkacheltje(h):
    name = "blubkacheltje"
    K = "#kachel"
    glas = (K, uv32(14, 8, 24, 18))
    kurk_zij = (K, uv32(24, 16, 32, 17))
    pijp = (K, uv32(0, 22, 3, 28))
    elementen = [
        # the stove
        box((1, 0, 1), (15, 8, 15), {"north": (K, uv32(0, 0, 14, 8)), "south": (K, uv32(14, 0, 28, 8)), "west": (K, uv32(14, 0, 28, 8)),
                                     "east": (K, uv32(14, 0, 28, 8)), "up": (K, uv32(0, 8, 14, 22)), "down": (K, uv32(2, 10, 12, 20))}),
        # the jar (a little higher than the block, so the blubje has room to hop) and its cork
        box((3, 8, 3), (13, 18, 13), {"north": glas, "south": glas, "west": glas, "east": glas, "up": (K, uv32(14, 18, 24, 28))}),
        box((4, 18, 4), (12, 19, 12), {"north": kurk_zij, "south": kurk_zij, "west": kurk_zij, "east": kurk_zij, "up": (K, uv32(24, 8, 32, 16))}),
        # the little stove pipe behind the jar
        box((9, 8, 13), (12, 14, 15), {"north": pijp, "south": pijp, "west": (K, uv32(0, 22, 2, 28)), "east": (K, uv32(0, 22, 2, 28)),
                                       "up": (K, uv32(3, 22, 6, 24))}),
    ] + _oren((1, 12), 8, 1, K, uv32(26, 20, 29, 23), uv32(29, 20, 32, 23))
    for staat in STATEN:
        h.save(_kachel(staat), "block", f"{name}_{staat}.png")
        h.w(f"{h.A}/models/block/{name}_{staat}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                        "textures": {"particle": f"guhs:block/{name}_{staat}", "kachel": f"guhs:block/{name}_{staat}"},
                                                        "elements": elementen})
    h.w(f"{h.A}/blockstates/{name}.json", _staten(name))
    h.w(f"{h.A}/models/item/{name}.json", {"parent": f"guhs:block/{name}_slaapt"})
    # the Sausblubje (drawn in the jar by client/BlubkacheltjeRenderer; it stands on y 0 around the middle of the block)
    h.save(_blubje(), "block", f"{name}_blubje.png")
    B = "#blubje"
    zij = (B, [6, 0, 12, 5])
    h.w(f"{h.A}/models/block/{name}_blubje.json", {
        "textures": {"particle": f"guhs:block/{name}_blubje", "blubje": f"guhs:block/{name}_blubje"},
        "elements": [box((5, 0, 5), (11, 5, 11), {"north": (B, [0, 0, 6, 5]), "south": zij, "west": zij, "east": zij, "up": (B, [0, 5, 6, 11]),
                                                  "down": (B, [0, 5, 6, 11])}),
                     box((6, 5, 6), (10, 6, 10), {"north": (B, [6, 5, 10, 6]), "south": (B, [6, 5, 10, 6]), "west": (B, [6, 5, 10, 6]),
                                                  "east": (B, [6, 5, 10, 6]), "up": (B, [1, 6, 5, 10])})]})


# =====================================================================================================================
# Gloeisterkern
# =====================================================================================================================
def _kern(staat):
    aan = staat != "slaapt"
    vonk = GLOED if aan else (92, 70, 62)
    a = _leeg()
    # the foot's front (0,0)-(14,6): a golden plate with the face
    _vul(a, 0, 0, 14, 6, STEEN, 4, 4401)
    _vul(a, 0, 0, 14, 1, GOUD)
    _vul(a, 0, 5, 14, 6, GOUD_D)
    _vul(a, 1, 1, 13, 5, (252, 228, 156), 3, 4402)
    # its side (14,0)-(28,6): charcoal with gold trim and glowing cracks
    _vul(a, 14, 0, 28, 6, STEEN, 5, 4403)
    _vul(a, 14, 0, 28, 1, GOUD)
    _vul(a, 14, 5, 28, 6, GOUD_D)
    for x, y in ((16, 2), (17, 3), (18, 3), (22, 2), (23, 2), (24, 3), (25, 4), (20, 4)):
        _px(a, x, y, vonk)
    # its top (0,6)-(14,20): a gold ring around a glowing well
    _vul(a, 0, 6, 14, 20, STEEN_L, 4, 4404)
    for i in range(1, 13):
        for x, y in ((i, 7), (i, 18), (1, 6 + i), (12, 6 + i)):
            _px(a, x, y, GOUD)
    _vul(a, 4, 10, 10, 16, STEEN)
    _vul(a, 5, 11, 9, 15, vonk)
    _vul(a, 6, 12, 8, 14, GLOED_L if aan else (120, 96, 84))
    # a post of zoutkristal (28,0)-(30,9)
    _vul(a, 28, 0, 30, 9, ZOUT)
    for y in range(9):
        _px(a, 29, y, ZOUT_D if y % 3 else (255, 255, 255))
    # the cap: top (0,20)-(10,30) a gold frame you look through, side (10,20)-(20,21)
    for i in range(10):
        for r in (0, 1):
            for x, y in ((i, 20 + r), (i, 29 - r), (r, 20 + i), (9 - r, 20 + i)):
                _px(a, x, y, GOUD if r == 0 else GOUD_D)
    _vul(a, 10, 20, 20, 21, GOUD)
    img = Image.fromarray(a)
    vadskracht.snoet(img, 1, 1, staat)
    a = np.asarray(img).copy()
    _oor(a, 26, 20, 3, 3)
    return Image.fromarray(a)


def _ster():
    a = _leeg(16)
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5)) / 7.5
            _px(a, x, y, tuple(int(v) for v in (255, 250 - 80 * d, 200 - 160 * d)))
    for x, y in ((3, 3), (12, 4), (5, 11), (10, 9)):
        _px(a, x, y, (255, 255, 255))
    return Image.fromarray(a)


def _ster_elementen(dy=0.0):
    """A star around the middle of the block: a little cube with six points."""
    S = "#ster"
    hart, punt = (S, [4, 4, 12, 12]), (S, [0, 0, 4, 4])
    alle = ("north", "south", "west", "east", "up", "down")
    els = [box((6, 6, 6), (10, 10, 10), {f: hart for f in alle})]
    for frm, to in (((7, 10, 7), (9, 12, 9)), ((7, 4, 7), (9, 6, 9)), ((10, 7, 7), (12, 9, 9)), ((4, 7, 7), (6, 9, 9)),
                    ((7, 7, 10), (9, 9, 12)), ((7, 7, 4), (9, 9, 6))):
        els.append(box(frm, to, {f: punt for f in alle}))
    for e in els:
        e["shade"] = False
        e["from"][1] += dy
        e["to"][1] += dy
    return els


def gloeisterkern(h):
    name = "gloeisterkern"
    K = "#kern"
    paal = (K, uv32(28, 0, 30, 9))
    kap = (K, uv32(10, 20, 20, 21))
    zij = (K, uv32(14, 0, 28, 6))
    elementen = [box((1, 0, 1), (15, 6, 15), {"north": (K, uv32(0, 0, 14, 6)), "south": zij, "west": zij, "east": zij,
                                              "up": (K, uv32(0, 6, 14, 20)), "down": (K, uv32(4, 10, 10, 16))})]
    for x0 in (3, 11):
        for z0 in (3, 11):
            elementen.append(box((x0, 6, z0), (x0 + 2, 15, z0 + 2), {f: paal for f in ("north", "south", "west", "east")}))
    elementen.append(box((3, 15, 3), (13, 16, 13), {"north": kap, "south": kap, "west": kap, "east": kap, "up": (K, uv32(0, 20, 10, 30)),
                                                    "down": (K, uv32(0, 20, 10, 30))}))
    elementen += _oren((1, 12), 6, 1, K, uv32(26, 20, 29, 23), uv32(29, 20, 32, 23))
    for staat in STATEN:
        h.save(_kern(staat), "block", f"{name}_{staat}.png")
        h.w(f"{h.A}/models/block/{name}_{staat}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                        "textures": {"particle": f"guhs:block/{name}_{staat}", "kern": f"guhs:block/{name}_{staat}"},
                                                        "elements": elementen})
    h.w(f"{h.A}/blockstates/{name}.json", _staten(name))
    h.save(_ster(), "block", f"{name}_ster.png")
    h.w(f"{h.A}/models/block/{name}_ster.json", {"textures": {"particle": f"guhs:block/{name}_ster", "ster": f"guhs:block/{name}_ster"},
                                                 "elements": _ster_elementen()})
    # the item: the kern with its star standing still in the cage
    h.w(f"{h.A}/models/item/{name}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                           "textures": {"particle": f"guhs:block/{name}_werkt", "kern": f"guhs:block/{name}_werkt",
                                                        "ster": f"guhs:block/{name}_ster"},
                                           "elements": elementen + _ster_elementen(3.0)})


# =====================================================================================================================
# Knabbelbatterij
# =====================================================================================================================
def knabbelbatterij(h):
    """vadskracht.batterij in kaasknabbel colours; every side and the top get a bite out of a corner (it IS a knabbel)."""
    name = "knabbelbatterij"
    vadskracht.batterij(h, name, KNABBEL, (255, 112, 170))
    for deel in ("zij", "boven"):
        a = np.asarray(Image.open(os.path.join(h.TEX, "block", f"{name}_{deel}.png")).convert("RGBA")).copy()
        # the bite: a quarter circle out of a corner (the top right of a side; on top the corner away from the ears)
        hoek_y = 1 if deel == "zij" else 11
        hap = ((1, 0), (2, 0), (3, 0), (2, 1), (3, 1), (3, 2)) if deel == "zij" else ((3, 1), (2, 2), (3, 2), (1, 3), (2, 3), (3, 3))
        rand = ((0, 0), (1, 1), (2, 2), (3, 3)) if deel == "zij" else ((3, 0), (2, 1), (1, 2), (0, 3))
        for dx, dy in hap:
            _px(a, 11 + dx, hoek_y + dy, _tint(KNABBEL_D, 0.62))
        for dx, dy in rand:
            _px(a, 11 + dx, hoek_y + dy, _tint(KNABBEL, 1.12))
        for x, y in ((4, 6), (9, 4), (6, 10), (10, 9), (3, 12)):                     # cheese holes
            _px(a, x, y, _tint(KNABBEL, 0.82))
        h.save(Image.fromarray(a), "block", f"{name}_{deel}.png")
