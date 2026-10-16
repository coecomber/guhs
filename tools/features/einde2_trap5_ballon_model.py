"""
einde2 phase C, slice trap5_ballon: the looks of the Vredesballon (not a feature module: einde2_trap5_ballon.py calls it).

  the actor   guhs:einde2_trap5_ballon_vredesballon (a cutscene actor, never in the world): the finished balloon of the
              building site (laag_vredesballon_5 of einde2_bouw_lucht_ballon.py) as ONE GeckoLib model, so it can rise and fly.
              It is drawn at a third of its size and the renderer scales it by 3 (SCHAAL), so one overlay block = U model
              pixels = one block in the world. The envelope is the same shape as the overlay's cloth (straal()), stacked
              from rounded layers; the orange and yellow gores, the band of flags and the guh face on the front are painted
              on two unwrapped pictures (FRONT, SIDE) that every layer samples at its own height, so the face reads across the
              layers. Pink ears on top, a woven basket with a low rim (the player and the Werktuigguh look out over it), the
              burner with a snoet on vadskracht, its flame (glowing), ropes, ballast bags of knabbels on the sides.
              Animations: zweven (always: a slow sway, ears, flame) and strooi (the bags swing while knabbels rain).
              The model's front (-z) is the face; the overlay's face looks south, the scene stands the actor at yaw 0.
  the statue  block guhs:vredesballon (the statuette, a reward): a plain block model of the same balloon, small.

Dutch only in comments; no text here.
"""
import math

import numpy as np
from PIL import Image, ImageDraw

from features import einde2_bouw_lucht_ballon as overlay
from features import sterrenwacht_hulp as hulp

NAAM = "einde2_trap5_ballon_vredesballon"
SCHAAL = 3                          # (Java: client.E2BallonClient: renderer.withScale(3))
U = 16.0 / SCHAAL                   # model pixels per overlay block
TEX = 256
MID = overlay.MID + 0.5             # the middle of the overlay (x and z), in overlay blocks
VLOER = overlay.MAND_Y              # the overlay row of the basket's floor = model y 0

# regions of the 256 x 256 sheet: name -> (u, v, w, h)
REGIO = {
    "front": (0, 0, 104, 128), "side": (104, 0, 104, 128), "top": (208, 0, 48, 48),
    "oor": (208, 48, 24, 24), "oor_in": (232, 48, 24, 24), "rand": (208, 72, 48, 8),
    "mand": (0, 128, 64, 24), "bodem": (64, 128, 32, 32), "touw": (96, 128, 8, 32), "koper": (104, 128, 24, 24),
    "snoet": (128, 128, 24, 24), "vlam": (152, 128, 16, 32), "knabbel": (168, 128, 24, 24), "zak": (192, 128, 24, 24),
    "staaf": (216, 128, 8, 24),
}
FW, FH = 104, 128
TOP_Y = (overlay.BOL_Y + overlay.BOL_R + 1 - VLOER) * U       # the model y of the top of the FRONT / SIDE pictures

ORANJE, GEEL = (238, 128, 36), (250, 204, 52)
BAND = [(120, 186, 236), (248, 246, 240), (130, 210, 72), (248, 246, 240)]   # light blue, white, lime, white (the overlay's flags)
ROZE, ROZE_IN = (244, 160, 196), (226, 104, 160)


def r(v):
    return round(v, 4)


# =====================================================================================================================
# the geo model
# =====================================================================================================================
def _vlak(name, x0, x1, y0, y1, flip=False):
    """Per-face uv for a face that samples FRONT or SIDE at its own x (or z) range and height."""
    u, v, w, h = REGIO[name]
    links = w / 2 - x1
    return {"uv": [r(u + links), r(v + TOP_Y - y1)], "uv_size": [r(x1 - x0), r(y1 - y0)]}


def _vol(name):
    u, v, w, h = REGIO[name]
    return {"uv": [u, v], "uv_size": [w, h]}


def _kubus(origin, size, faces, inflate=0.0):
    c = {"origin": [r(v) for v in origin], "size": [r(v) for v in size], "uv": faces}
    if inflate:
        c["inflate"] = inflate
    return c


def _effen(origin, size, name, voor=None):
    faces = {f: _vol(name) for f in ("north", "south", "east", "west", "up", "down")}
    if voor:
        faces["north"] = _vol(voor)
    return _kubus(origin, size, faces)


def _laag(hx, hz, y0, y1):
    """One box of a layer of the envelope: the front samples FRONT, the other sides SIDE, the top and bottom TOP."""
    return _kubus([-hx, y0, -hz], [2 * hx, y1 - y0, 2 * hz], {
        "north": _vlak("front", -hx, hx, y0, y1), "south": _vlak("side", -hx, hx, y0, y1),
        "east": _vlak("side", -hz, hz, y0, y1), "west": _vlak("side", -hz, hz, y0, y1),
        "up": _vol("top"), "down": _vol("top")})


def straal_model(rij):
    """The radius (model px) of the layer of overlay row `rij` (as the overlay's cloth: straal() + its skin)."""
    if rij == overlay.MOND_Y - 1:
        return 1.9 * U
    s = overlay.straal(rij)
    return -1.0 if s < 0 else (s + 0.45) * U


def envelop():
    """The cubes of the envelope: per overlay row a square and two flat boxes crossed (a rounded layer)."""
    cubes = []
    for rij in range(overlay.MOND_Y - 1, int(overlay.BOL_Y + overlay.BOL_R) + 1):
        a = straal_model(rij)
        if a <= 0:
            continue
        y0, y1 = (rij - VLOER) * U, (rij + 1 - VLOER) * U
        cubes.append(_laag(0.86 * a, 0.86 * a, y0, y1))
        cubes.append(_laag(a, 0.56 * a, y0, y1))
        cubes.append(_laag(0.56 * a, a, y0, y1))
    return cubes


def oor(cx):
    """An ear on top of the balloon (the overlay's pink ball of radius 2.3 at x MID +- 5, row 35)."""
    cy = (overlay.BOL_Y + overlay.BOL_R - 0.2 - VLOER) * U
    b = 2.3 * U
    return [
        _effen([cx - 0.8 * b, cy - 0.8 * b, -0.5 * b], [1.6 * b, 1.6 * b, b], "oor"),
        _effen([cx - b, cy - 0.45 * b, -0.42 * b], [2 * b, 0.9 * b, 0.84 * b], "oor"),
        _effen([cx - 0.45 * b, cy - b, -0.42 * b], [0.9 * b, 2 * b, 0.84 * b], "oor"),
        _effen([cx - 0.55 * b, cy - 0.55 * b, -0.5 * b - 0.4], [1.1 * b, 1.1 * b, 0.4], "oor_in"),
    ]


def mand():
    """The basket: a floor of 7 x 7, a low woven rim with the little gate open at the front, the burner with its snoet."""
    h = 1.3 * U
    m, d = 3.5 * U, 0.5 * U
    cubes = [_effen([-m, 0, -m], [2 * m, U, 2 * m], "bodem"),
             _effen([-m, U, m - d], [2 * m, h, d], "mand"),                        # back (south)
             _effen([-m, U, -m + d], [d, h, 2 * m - 2 * d], "mand"), _effen([m - d, U, -m + d], [d, h, 2 * m - 2 * d], "mand"),
             _effen([-m, U, -m], [m - 0.5 * U, h, d], "mand"), _effen([0.5 * U, U, -m], [m - 0.5 * U, h, d], "mand"),   # front, the gate open
             _effen([-m - 0.1, U + h, -m - 0.1], [2 * m + 0.2, 0.5, 2 * m + 0.2], "rand")]
    # the burner (3 x 2 x 3 of copper, the snoet looks at the gate), its cap and the rod up to the flame
    cubes += [_effen([-1.5 * U, U, -1.5 * U], [3 * U, 2 * U, 3 * U], "koper", voor="snoet"),
              _effen([-0.5 * U, 3 * U, -0.5 * U], [U, U, U], "koper"),
              _effen([-0.15 * U, 4 * U, -0.15 * U], [0.3 * U, 3 * U, 0.3 * U], "staaf")]
    # knabbels in the basket (the overlay's seven blocks, one high here)
    for dx, dz in ((-2, -2), (2, -2), (-2, 2), (2, 2), (-2, 0), (2, 0), (0, 2)):
        cubes.append(_effen([dx * U - 0.42 * U, U, dz * U - 0.42 * U], [0.84 * U, 0.84 * U, 0.84 * U], "knabbel"))
    # the ropes from the corners of the rim up into the mouth
    for sx in (-1, 1):
        for sz in (-1, 1):
            cubes.append(_effen([sx * 3 * U - 0.12 * U, U + h, sz * 3 * U - 0.12 * U], [0.24 * U, (overlay.MOND_Y + 1 - VLOER) * U - U - h, 0.24 * U], "touw"))
    return cubes


def zakken():
    """The ballast bags of knabbels on the outside of the rim (the overlay's six)."""
    out = []
    for x, z in ((-4, -1), (-4, 1), (4, -1), (4, 1), (-1, 4), (1, 4)):
        px, pz = x * U, z * U
        out += [_effen([px - 0.4 * U, 1.2 * U, pz - 0.4 * U], [0.8 * U, 0.9 * U, 0.8 * U], "knabbel"),
                _effen([px - 0.25 * U, 2.1 * U, pz - 0.25 * U], [0.5 * U, 0.35 * U, 0.5 * U], "zak")]
    return out


def geo():
    vlam_y = 7 * U
    bones = [
        {"name": "root", "pivot": [0, 0, 0]},
        {"name": "mand", "parent": "root", "pivot": [0, 0, 0], "cubes": mand()},
        {"name": "zakken", "parent": "root", "pivot": [0, 2.4 * U, 0], "cubes": zakken()},
        {"name": "vlam", "parent": "root", "pivot": [0, vlam_y, 0], "cubes": [_effen([-0.6 * U, vlam_y, -0.6 * U], [1.2 * U, 4.2 * U, 1.2 * U], "vlam")]},
        {"name": "ballon", "parent": "root", "pivot": [0, (overlay.MOND_Y - VLOER) * U, 0], "cubes": envelop()},
    ]
    oy = (overlay.BOL_Y + overlay.BOL_R - 1.5 - VLOER) * U
    for kant, cx in (("links", 5 * U), ("rechts", -5 * U)):
        bones.append({"name": f"oor_{kant}", "parent": "ballon", "pivot": [r(cx), r(oy), 0], "cubes": oor(cx)})
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{NAAM}", "texture_width": TEX, "texture_height": TEX,
                        "visible_bounds_width": 8, "visible_bounds_height": 14, "visible_bounds_offset": [0, 6.5, 0]},
        "bones": bones}]}


def animatie():
    a = f"animation.{NAAM}"
    return {"format_version": "1.8.0", "animations": {
        f"{a}.zweven": {"loop": True, "animation_length": 6.0, "bones": {
            "ballon": {"rotation": {"0.0": [0.6, 0, -0.8], "3.0": [-0.6, 0, 0.8], "6.0": [0.6, 0, -0.8]}},
            "oor_links": {"rotation": {"0.0": [0, 0, 0], "1.2": [0, 0, 6], "2.0": [0, 0, 0], "4.4": [0, 0, 3], "6.0": [0, 0, 0]}},
            "oor_rechts": {"rotation": {"0.0": [0, 0, 0], "1.6": [0, 0, -4], "2.6": [0, 0, 0], "4.8": [0, 0, -6], "6.0": [0, 0, 0]}},
            "zakken": {"rotation": {"0.0": [0, 0, 0], "3.0": [0, 2, 0], "6.0": [0, 0, 0]}},
            "vlam": {"scale": {"0.0": [1, 1, 1], "0.3": [1.1, 1.3, 1.1], "0.6": [0.9, 0.8, 0.9], "1.0": [1.05, 1.2, 1.05], "1.4": [1, 0.9, 1],
                               "2.0": [1.1, 1.35, 1.1], "2.5": [0.95, 0.85, 0.95], "3.2": [1.05, 1.2, 1.05], "4.0": [1, 1, 1],
                               "4.6": [1.12, 1.3, 1.12], "5.3": [0.92, 0.85, 0.92], "6.0": [1, 1, 1]}}}},
        f"{a}.strooi": {"loop": True, "animation_length": 1.5, "bones": {
            "zakken": {"rotation": {"0.0": [-10, 0, 0], "0.75": [12, 0, 0], "1.5": [-10, 0, 0]}},
            "oor_links": {"rotation": {"0.0": [0, 0, 4], "0.4": [0, 0, 12], "0.75": [0, 0, 4], "1.1": [0, 0, 10], "1.5": [0, 0, 4]}},
            "oor_rechts": {"rotation": {"0.0": [0, 0, -4], "0.5": [0, 0, -12], "0.75": [0, 0, -4], "1.2": [0, 0, -10], "1.5": [0, 0, -4]}}}},
    }}


# =====================================================================================================================
# the textures
# =====================================================================================================================
def _ruis(rng, h, w, kleur, var=7):
    b = np.zeros((h, w, 4), np.uint8)
    b[..., :3] = np.clip(np.array(kleur, float) + rng.normal(0, var, (h, w, 1)), 0, 255)
    b[..., 3] = 255
    return b


def _rij_straal(y):
    """The radius (model px) of the envelope at model height y (for the shading of the pictures)."""
    rij = int(math.floor(y / U + VLOER))
    return max(1.0, straal_model(rij))


def _doek(rng, gezicht):
    """FRONT (gezicht) or SIDE: the unwrapped side of the balloon: gores, seams, the band of flags, light and shadow."""
    a = _ruis(rng, FH, FW, (0, 0, 0), 0)
    for rij in range(FH):
        y = TOP_Y - rij - 0.5
        rad = _rij_straal(y)
        orij = y / U + VLOER                                          # the overlay row of this picture row
        for kol in range(FW):
            x = FW / 2 - kol - 0.5
            t = max(-1.0, min(1.0, x / rad))
            hoek = math.asin(t)
            baan = int((hoek + math.pi / 2) / (math.pi / 8))
            kleur = ORANJE if baan % 2 else GEEL
            if abs((hoek + math.pi / 2) / (math.pi / 8) - round((hoek + math.pi / 2) / (math.pi / 8))) < 0.06:
                kleur = tuple(int(c * 0.8) for c in kleur)          # a seam between two gores
            if int(math.floor(orij)) == overlay.BOL_Y - 3:
                kleur = BAND[int(math.floor((x + FW) / U)) % 4]     # the band of flags (the overlay's row dy = -3)
            licht = 1.0 - 0.22 * abs(t) ** 2.2 + 0.05 * (1 if orij > overlay.BOL_Y + 3 else 0)
            a[rij, kol, :3] = np.clip(np.array(kleur) * licht + rng.normal(0, 5), 0, 255)
    img = Image.fromarray(a)
    if gezicht:
        # the guh face on the front, a bit above the middle of the ball (the overlay's eyes at dy 2..3, nose at 0..1)
        midden = TOP_Y - (overlay.BOL_Y + 1.6 - VLOER) * U
        w, h = 64, 44
        d = ImageDraw.Draw(img)
        d.ellipse((FW / 2 - w / 2 - 6, midden - h / 2 - 5, FW / 2 + w / 2 + 6, midden + h / 2 + 5), fill=(255, 226, 150, 255))
        hulp.paint_face(img, (int(FW / 2 - w / 2), int(midden - h / 2), w, h), 0)
    return np.asarray(img).copy()


def _top(rng):
    w, h = REGIO["top"][2:]
    a = _ruis(rng, h, w, (0, 0, 0), 0)
    for y in range(h):
        for x in range(w):
            hoek = math.atan2(y - h / 2 + 0.5, x - w / 2 + 0.5)
            baan = int((hoek + math.pi) / (math.pi / 8))
            a[y, x, :3] = np.clip(np.array(ORANJE if baan % 2 else GEEL) + rng.normal(0, 5), 0, 255)
            if math.hypot(y - h / 2 + 0.5, x - w / 2 + 0.5) < 5:
                a[y, x, :3] = (232, 186, 70)                           # the golden button on top
    return a


def _vlecht(rng, w, h):
    b = _ruis(rng, h, w, (176, 124, 70), 8)
    for y in range(h):
        for x in range(w):
            if (y // 3 + x // 4) % 2 == 0:
                b[y, x, :3] = np.clip(b[y, x, :3].astype(int) - 32, 0, 255)
    b[:2, :, :3] = (120, 80, 45)
    b[-2:, :, :3] = (120, 80, 45)
    return b


def _snoet(rng, w, h):
    b = _ruis(rng, h, w, (196, 112, 72), 9)
    img = Image.fromarray(b)
    d = ImageDraw.Draw(img)
    d.rectangle((5, 7, 8, 11), fill=(24, 18, 22, 255))               # the eyes
    d.rectangle((w - 9, 7, w - 6, 11), fill=(24, 18, 22, 255))
    d.point([(6, 8), (w - 8, 8)], fill=(255, 255, 255, 255))
    d.rectangle((w // 2 - 2, 13, w // 2 + 1, 15), fill=(236, 120, 160, 255))   # the nose
    d.rectangle((2, 13, 4, 14), fill=(240, 150, 150, 255))           # blush
    d.rectangle((w - 5, 13, w - 3, 14), fill=(240, 150, 150, 255))
    return np.asarray(img).copy()


def _vlam(w, h):
    b = np.zeros((h, w, 4), np.uint8)
    for y in range(h):
        t = y / h
        for x in range(w):
            rand = abs(x - w / 2 + 0.5) / (w / 2)
            b[y, x] = (255, int(150 + 100 * t * (1 - rand)), int(40 + 120 * t * (1 - rand)), 255)
    return b


def _knabbel(rng, w, h):
    b = _ruis(rng, h, w, (246, 184, 64), 8)
    for _ in range(14):
        x, y = rng.integers(1, w - 3), rng.integers(1, h - 3)
        b[y:y + 2, x:x + 2, :3] = (206, 132, 40)
    b[0, :, :3] = b[-1, :, :3] = b[:, 0, :3] = b[:, -1, :3] = (190, 120, 40)
    return b


def textures(seed=27720):
    rng = np.random.default_rng(seed)
    a = np.zeros((TEX, TEX, 4), np.uint8)

    def zet(name, blok):
        u, v, w, h = REGIO[name]
        a[v:v + h, u:u + w] = blok[:h, :w]

    zet("front", _doek(rng, True))
    zet("side", _doek(rng, False))
    zet("top", _top(rng))
    zet("oor", _ruis(rng, 24, 24, ROZE, 6))
    zet("oor_in", _ruis(rng, 24, 24, ROZE_IN, 5))
    zet("rand", _ruis(rng, 8, 48, (120, 80, 45), 6))
    zet("mand", _vlecht(rng, 64, 24))
    zet("bodem", _ruis(rng, 32, 32, (112, 80, 50), 9))
    zet("touw", _ruis(rng, 32, 8, (226, 206, 160), 6))
    zet("koper", _ruis(rng, 24, 24, (196, 112, 72), 10))
    zet("snoet", _snoet(rng, 24, 24))
    zet("vlam", _vlam(16, 32))
    zet("knabbel", _knabbel(rng, 24, 24))
    zet("zak", _ruis(rng, 24, 24, (120, 82, 54), 7))
    zet("staaf", _ruis(rng, 24, 8, (210, 120, 76), 8))
    hoofd = Image.fromarray(a)
    # the glow: only the flame
    g = np.zeros_like(a)
    u, v, w, h = REGIO["vlam"]
    g[v:v + h, u:u + w] = a[v:v + h, u:u + w]
    return hoofd, Image.fromarray(g)


# =====================================================================================================================
# the statuette (a plain block model, its own little 32 x 32 texture)
# =====================================================================================================================
BEELD_REGIO = {"doek": (0, 0, 8, 8), "gezicht": (8, 0, 8, 8), "oor": (0, 8, 4, 4), "mand": (4, 8, 4, 4), "hout": (8, 8, 4, 4),
               "touw": (12, 8, 1, 4), "vlam": (13, 8, 2, 4), "top": (0, 12, 4, 4)}     # in uv units (0..16), 2 px each


def beeld_textuur(seed=27721):
    rng = np.random.default_rng(seed)
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    a = np.zeros((32, 32, 4), np.uint8)

    def vul(naam, fn):
        u, v, w, h = (int(c * 2) for c in BEELD_REGIO[naam])
        a[v:v + h, u:u + w] = fn(w, h)

    def gores(w, h):
        b = _ruis(rng, h, w, (0, 0, 0), 0)
        for x in range(w):
            b[:, x, :3] = np.clip(np.array(ORANJE if (x // 2) % 2 else GEEL) + rng.normal(0, 4, (h, 1)), 0, 255)
        b[h // 2 + 3, :, :3] = [BAND[x % 4] for x in range(w)] if h > 8 else b[h // 2 + 3, :, :3]
        return b

    vul("doek", gores)

    def gezicht(w, h):
        b = gores(w, h)
        b[5:7, 3:5, :3] = (24, 18, 22)
        b[5:7, w - 5:w - 3, :3] = (24, 18, 22)
        b[8:10, w // 2 - 1:w // 2 + 1, :3] = (236, 120, 160)
        b[9, 1:3, :3] = (240, 130, 150)
        b[9, w - 3:w - 1, :3] = (240, 130, 150)
        return b

    vul("gezicht", gezicht)
    vul("oor", lambda w, h: _ruis(rng, h, w, ROZE, 6))
    vul("mand", lambda w, h: _vlecht(rng, w, h))
    vul("hout", lambda w, h: _ruis(rng, h, w, (112, 80, 50), 8))
    vul("touw", lambda w, h: _ruis(rng, h, w, (226, 206, 160), 5))
    vul("vlam", lambda w, h: _vlam(w, h))
    vul("top", lambda w, h: _ruis(rng, h, w, (232, 186, 70), 6))
    img = Image.fromarray(a)
    return img


def _el(frm, to, naam, voor=None, boven=None):
    u, v, w, h = BEELD_REGIO[naam]
    vlak = {"uv": [u, v, u + w, v + h], "texture": "#t"}
    faces = {f: dict(vlak) for f in ("north", "south", "east", "west", "up", "down")}
    if voor:
        uu, vv, ww, hh = BEELD_REGIO[voor]
        faces["north"] = {"uv": [uu, vv, uu + ww, vv + hh], "texture": "#t"}
    if boven:
        uu, vv, ww, hh = BEELD_REGIO[boven]
        faces["up"] = {"uv": [uu, vv, uu + ww, vv + hh], "texture": "#t"}
    return {"from": [r(c) for c in frm], "to": [r(c) for c in to], "faces": faces}


def beeld_model():
    el = [_el([3, 0, 3], [13, 1, 13], "hout"),                       # the little plinth
          _el([6, 1, 6], [10, 3, 10], "mand"),
          _el([7.25, 1.5, 7.25], [8.75, 4, 8.75], "vlam")]
    for x in (6, 9.5):
        for z in (6, 9.5):
            el.append(_el([x, 3, z], [x + 0.5, 6, z + 0.5], "touw"))
    el += [_el([5, 6, 5], [11, 7, 11], "doek"),
           _el([3.5, 7, 3.5], [12.5, 8, 12.5], "doek"),
           _el([2.5, 8, 2.5], [13.5, 13, 13.5], "doek", voor="gezicht"),
           _el([3.5, 13, 3.5], [12.5, 14, 12.5], "doek"),
           _el([5, 14, 5], [11, 15, 11], "doek", boven="top"),
           _el([2, 13, 7], [5, 16, 9], "oor"), _el([11, 13, 7], [14, 16, 9], "oor")]
    return {"parent": "minecraft:block/block", "textures": {"t": "guhs:block/vredesballon", "particle": "guhs:block/vredesballon"},
            "elements": el}


def build(h):
    h.w(f"{h.A}/geckolib/models/entity/{NAAM}.geo.json", geo())
    h.w(f"{h.A}/geckolib/animations/entity/{NAAM}.animation.json", animatie())
    hoofd, gloed = textures()
    h.save(hoofd, "entity", f"{NAAM}.png")
    h.save(gloed, "entity", f"{NAAM}_gloed.png")
    h.save(beeld_textuur(), "block", "vredesballon.png")
    h.w(f"{h.A}/models/block/vredesballon.json", beeld_model())
    h.w(f"{h.A}/blockstates/vredesballon.json", {"variants": h.facing_states("vredesballon")})
    h.w(f"{h.A}/models/item/vredesballon.json", {"parent": "guhs:block/vredesballon", "display": {
        "gui": {"rotation": [20, 205, 0], "translation": [0, -0.5, 0], "scale": [0.72, 0.72, 0.72]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]}}})


def controle():
    """Self-check of the model -> problems."""
    p = []
    g = geo()["minecraft:geometry"][0]
    for bone in g["bones"]:
        for c in bone.get("cubes", []):
            for f, uv in c["uv"].items():
                (u, v), (w, hh) = uv["uv"], uv["uv_size"]
                if u < -0.01 or v < -0.01 or u + w > TEX + 0.01 or v + hh > TEX + 0.01 or w <= 0 or hh <= 0:
                    p.append(f"{bone['name']}: a {f} face samples outside the sheet: {uv}")
    hoogte = max(c["origin"][1] + c["size"][1] for b in g["bones"] for c in b.get("cubes", []))
    if not 33 * U <= hoogte <= 37 * U:
        p.append(f"the balloon is {hoogte / U:.1f} blocks high, the overlay's is about 35")
    return p
