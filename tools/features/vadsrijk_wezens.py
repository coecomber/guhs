"""
1.10.0 "Het Vadsrijk", the wezens slice (CONTRACT_VADSRIJK.md). Java: feature/vadsrijk/wezens.

Everything here is generated (no hand-made art):
  * GeckoLib models + animations of the zone-1 creatures (assets/guhs/geckolib/{models,animations}/entity/vadsrijk/wezens/):
      slijm family   klodder (also kloddertje), bubbelklodder, stuiterklodder, moederklodder, druipslijm, kristalklodder, kampklodder
      zwam family    hoedjesbol, sporenpuffer, zwamwachter, glimzwam, tempelridder, sporenpriester
      others         mosgeest, knaag (a grey rodent, NOT a guh), draak (Vahoegnir, the flyover dragon)
  * per creature two textures (textures/vadsrijk/wezens/): <naam>.png = "ontvadst" (grey-purple, grumpy guh face) and
    <naam>_blij.png (its bright colours, a happy guh face with blush: the look after the plof), plus <naam>_elite.png (the
    additive gold glow of elites) and a few glowmasks (glimzwam, sporenpriester, draak).
  * the 14 armour base types (items vr_mob_*, 16x16 icons in textures/item/vadsrijk/wezens/) and all Dutch texts.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_wezens.py
"""
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw

SUB = "vadsrijk/wezens"
FACES = ("north", "south", "east", "west", "up", "down")


# =====================================================================================================================
# model builder: cubes with their own uv regions (one region per face), painted per "rol"
# =====================================================================================================================
def _face_dims(face, size):
    sx, sy, sz = size
    return {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}[face]


class Model:
    def __init__(self, naam, px=4, breedte=64, bounds=(2, 2, 0.5)):
        self.naam, self.px, self.W = naam, px, breedte
        self.bones, self.regios = [], []
        self.x = self.y = self.rij = 0
        self.bounds = bounds

    def _neem(self, w, h):
        w, h = max(1, int(math.ceil(w))), max(1, int(math.ceil(h)))
        if self.x + w > self.W:
            self.x, self.y, self.rij = 0, self.y + self.rij + 1, 0
        r = (self.x, self.y, w, h)
        self.x += w + 1
        self.rij = max(self.rij, h)
        return r

    def bone(self, name, parent=None, pivot=(0, 0, 0), rot=None):
        b = {"name": name, "pivot": [float(v) for v in pivot], "cubes": []}
        if parent:
            b["parent"] = parent
        if rot:
            b["rotation"] = [float(v) for v in rot]
        self.bones.append(b)
        return self

    def cube(self, bone, rol, origin, size, rot=None, pivot=None, inflate=0.0, gezicht=None):
        uv = {}
        for f in FACES:
            w, h = _face_dims(f, size)
            x, y, ww, hh = self._neem(w, h)
            self.regios.append({"rol": rol, "f": f, "r": (x, y, ww, hh), "gezicht": gezicht if f == "north" else None})
            uv[f] = {"uv": [x, y], "uv_size": [w, h]}
        c = {"origin": [float(v) for v in origin], "size": [float(v) for v in size], "uv": uv}
        if inflate:
            c["inflate"] = inflate
        if rot:
            c["rotation"] = [float(v) for v in rot]
            c["pivot"] = [float(v) for v in (pivot or origin)]
        for b in self.bones:
            if b["name"] == bone:
                b["cubes"].append(c)
                return self
        raise KeyError(bone)

    def spiegel(self, bone, rol, origin, size, rot=None, pivot=None, inflate=0.0):
        """The same cube mirrored in x (for the right-hand side): origin x -> -(x + size x), rotations y/z flipped."""
        o = [-(origin[0] + size[0]), origin[1], origin[2]]
        r = None if rot is None else [rot[0], -rot[1], -rot[2]]
        p = None if pivot is None else [-pivot[0], pivot[1], pivot[2]]
        return self.cube(bone, rol, o, size, r, p, inflate)

    def hoogte(self):
        h = self.y + self.rij + 1
        return int(math.ceil(h / 4.0) * 4)

    def geo(self):
        bones = []
        for b in self.bones:
            b2 = dict(b)
            if not b2["cubes"]:
                del b2["cubes"]
            bones.append(b2)
        bw, bh, off = self.bounds
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry.vr_" + self.naam, "texture_width": self.W, "texture_height": self.hoogte(),
                            "visible_bounds_width": bw, "visible_bounds_height": bh, "visible_bounds_offset": [0, off, 0]},
            "bones": bones}]}


# =====================================================================================================================
# painting
# =====================================================================================================================
GRAUW = np.array((126, 116, 136), float)


def _grauw(c, f=0.62):
    """The ontvadst version of a colour: pulled toward a dull grey-purple and a bit darker."""
    c = np.array(c, float)
    return tuple(int(v) for v in np.clip(c * (1 - f) + GRAUW * f, 0, 255) * 0.92)


def _rng(seed):
    return np.random.default_rng(seed)


def _vul(blok, kleur, r, var=6):
    blok[..., :3] = np.clip(np.array(kleur, float) + r.normal(0, var, blok.shape[:2] + (1,)), 0, 255)
    blok[..., 3] = 255


def _patroon(blok, naam, kleur2, r, f):
    h, w = blok.shape[:2]
    if naam == "stippen":                          # white dots on a mushroom cap
        n = max(1, (h * w) // 160)
        for _ in range(n):
            cx, cy, rr = r.integers(0, w), r.integers(0, h), max(1.5, min(h, w) * r.uniform(0.07, 0.14))
            yy, xx = np.mgrid[0:h, 0:w]
            blok[np.hypot(xx - cx, yy - cy) < rr, :3] = kleur2
    elif naam == "glans":                          # a slime's highlight (top-left on the faces you see)
        if f in ("north", "up", "west", "east"):
            yy, xx = np.mgrid[0:h, 0:w]
            m = np.hypot((xx - w * 0.22) / max(1, w * 0.12), (yy - h * 0.2) / max(1, h * 0.1)) < 1
            blok[m, :3] = np.clip(blok[m, :3].astype(int) + 60, 0, 255)
    elif naam == "strepen":
        for x in range(w):
            if (x // 3) % 2:
                blok[:, x, :3] = kleur2
    elif naam == "plekjes":                        # moss / fluff blotches
        for _ in range(max(1, (h * w) // 50)):
            cx, cy = r.integers(0, w), r.integers(0, h)
            blok[max(0, cy - 1):cy + 2, max(0, cx - 1):cx + 2, :3] = kleur2
    elif naam == "veer":                           # a feather: a darker shaft down the middle, lighter tips
        m = w // 2
        blok[:, max(0, m - 1):m + 1, :3] = kleur2
        blok[-max(1, h // 6):, :, :3] = np.clip(blok[-max(1, h // 6):, :, :3].astype(int) + 30, 0, 255)
    elif naam == "facet":                          # crystal facets
        for y in range(h):
            for x in range(w):
                if (x + y) % 6 < 2:
                    blok[y, x, :3] = kleur2
    elif naam == "knetter":                        # black crackling fluff on the dragon
        for _ in range(max(1, (h * w) // 30)):
            cx, cy = r.integers(0, w), r.integers(0, h)
            blok[cy, cx, :3] = kleur2


def _cirkel(blok, cx, cy, r, kleur):
    h, w = blok.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    m = np.hypot(xx - cx, yy - cy) <= r
    blok[m, :3] = kleur
    return m


def _lijn(blok, pts, kleur, dik=1):
    img = Image.fromarray(blok)
    d = ImageDraw.Draw(img)
    d.line(pts, fill=tuple(kleur) + (255,), width=dik)
    blok[...] = np.asarray(img)


def gezicht(blok, mood, ey=0.42, schaal=1.0):
    """A guh face on a front face. mood: blij (round eyes with sparkles, blush, smile), boos (half-lidded grumpy eyes, brows,
    a little frown), o (blij with an "o" mouth: the bubble spitter), knaag (beady rodent eyes, pink nose, buck teeth),
    draak (glowing purple slit eyes)."""
    h, w = blok.shape[:2]
    lijf = blok[h // 2, 1, :3].astype(int).copy()
    s = min(w, h) * schaal
    er = max(1.6, s * 0.11)
    cy = h * ey
    ogen = (w * 0.5 - s * 0.22, w * 0.5 + s * 0.22)
    if mood == "draak":
        for i, ex in enumerate(ogen):
            pts = [(ex - er * 1.4, cy - er * (0.6 if i == 0 else -0.2)), (ex + er * 1.4, cy + er * (0.6 if i == 1 else -0.2))]
            _lijn(blok, pts, (40, 10, 50), max(2, int(er * 0.9)))
            _lijn(blok, pts, (220, 120, 255), max(1, int(er * 0.45)))
        return
    if mood == "knaag":
        for ex in ogen:
            _cirkel(blok, ex, cy, er * 0.7, (24, 20, 26))
            _cirkel(blok, ex - er * 0.25, cy - er * 0.25, max(0.6, er * 0.22), (240, 240, 250))
        _cirkel(blok, w * 0.5, cy + s * 0.2, max(1, er * 0.55), (240, 140, 160))
        tx = w * 0.5
        blok[int(cy + s * 0.3):int(cy + s * 0.44), int(tx - max(1, s * 0.07)):int(tx + max(1, s * 0.07)) + 1, :3] = (250, 248, 236)
        return
    for ex in ogen:
        _cirkel(blok, ex, cy, er, (44, 28, 56))
        _cirkel(blok, ex, cy + er * 0.45, er * 0.55, (96, 66, 124))
        _cirkel(blok, ex, cy + er * 0.1, er * 0.5, (44, 28, 56))
        _cirkel(blok, ex - er * 0.35, cy - er * 0.35, max(0.7, er * 0.32), (255, 255, 255))
        _cirkel(blok, ex + er * 0.4, cy + er * 0.35, max(0.5, er * 0.15), (220, 230, 255))
    mond_y = cy + er * 1.7
    if mood == "boos":
        for i, ex in enumerate(ogen):               # heavy eyelids in the body colour, and brows slanting to the middle
            yy, xx = np.mgrid[0:h, 0:w]
            m = (np.hypot(xx - ex, yy - cy) <= er + 0.5) & (yy < cy - er * 0.15)
            blok[m, :3] = np.clip(lijf - 18, 0, 255)
            binnen = ex + (er if i == 0 else -er)
            buiten = ex - (er if i == 0 else -er)
            _lijn(blok, [(buiten, cy - er * 1.5), (binnen, cy - er * 0.8)], (50, 40, 60), max(1, int(er * 0.45)))
        mx = w * 0.5
        _lijn(blok, [(mx - er * 0.9, mond_y + er * 0.5), (mx, mond_y), (mx + er * 0.9, mond_y + er * 0.5)], (60, 40, 70), max(1, int(er * 0.35)))
        return
    for ex in ogen:                                  # blush
        yy, xx = np.mgrid[0:h, 0:w]
        bx = ex + (-er * 0.9 if ex < w / 2 else er * 0.9)
        m = np.hypot((xx - bx) / (er * 0.9), (yy - (cy + er * 1.35)) / (er * 0.5)) <= 1
        blok[m, :3] = (255, 140, 180)
    mx = w * 0.5
    if mood == "o":
        _cirkel(blok, mx, mond_y + er * 0.3, er * 0.55, (110, 40, 70))
        _cirkel(blok, mx, mond_y + er * 0.3, er * 0.25, (230, 120, 160))
    else:
        _lijn(blok, [(mx - er * 0.8, mond_y), (mx - er * 0.3, mond_y + er * 0.5), (mx, mond_y + er * 0.15),
                     (mx + er * 0.3, mond_y + er * 0.5), (mx + er * 0.8, mond_y)], (110, 40, 70), max(1, int(er * 0.35)))


def schilder(model, palet, mood, seed, grauw):
    """palet: rol -> (kleur, patroon or None, tweede kleur). grauw: the ontvadst version (grey colours, grumpy)."""
    S = model.px
    img = np.zeros((model.hoogte() * S, model.W * S, 4), np.uint8)
    r = _rng(seed)
    for reg in model.regios:
        x, y, w, h = reg["r"]
        blok = img[y * S:(y + h) * S, x * S:(x + w) * S]
        kleur, pat, k2 = palet.get(reg["rol"], ((255, 0, 255), None, None))
        if grauw and reg["rol"] not in ("oog", "tand"):
            kleur, k2 = _grauw(kleur), (_grauw(k2) if k2 else None)
        f = reg["f"]
        donker = 0.82 if f == "down" else 0.92 if f in ("east", "west", "south") else 1.0
        _vul(blok, tuple(np.array(kleur) * donker), r)
        if pat:
            _patroon(blok, pat, k2, r, f)
        if reg["gezicht"]:
            g = reg["gezicht"]
            m = g.get("mood", mood)
            if grauw and m in ("blij", "o"):
                m = "boos"
            gezicht(blok, m, g.get("ey", 0.42), g.get("schaal", 1.0))
    return Image.fromarray(img)


def gloed(model, rollen, seed, kleur_van=None):
    """A glowmask: the listed roles in their glow colour, everything else black (the additive eyes layer)."""
    S = model.px
    img = np.zeros((model.hoogte() * S, model.W * S, 4), np.uint8)
    img[..., 3] = 255
    r = _rng(seed)
    for reg in model.regios:
        x, y, w, h = reg["r"]
        blok = img[y * S:(y + h) * S, x * S:(x + w) * S]
        if reg["rol"] in rollen:
            _vul(blok, rollen[reg["rol"]], r, 10)
        if kleur_van and reg["gezicht"]:
            tmp = np.zeros_like(blok)
            gezicht(tmp, "draak", reg["gezicht"].get("ey", 0.42))
            m = tmp[..., 0] > 150
            blok[m, :3] = (230, 130, 255)
    return Image.fromarray(img)


def elite_gloed(tex):
    a = np.asarray(tex).astype(float)
    out = a.copy()
    out[..., :3] = np.clip(a[..., :3] * np.array([0.55, 0.42, 0.12]) + np.array([40, 26, 0]), 0, 255)
    out[..., 3] = 255
    return Image.fromarray(out.astype(np.uint8))


# =====================================================================================================================
# animations
# =====================================================================================================================
def kf(pairs):
    return {f"{t:g}": [float(v) for v in val] for t, val in pairs}


def anim(lengte, bones, loop=True):
    a = {"animation_length": lengte, "bones": bones}
    a["loop"] = True if loop is True else ("hold_on_last_frame" if loop == "hold" else False)
    return a


def anim_bestand(anims):
    return {"format_version": "1.8.0", "animations": anims}


# =====================================================================================================================
# the slime family
# =====================================================================================================================
def slijm_basis(naam, hoog=10, lift=0.0, px=4):
    m = Model(naam, px=px, breedte=64)
    m.bone("root")
    m.bone("lijf", "root", (0, lift, 0))
    m.cube("lijf", "slijm", (-6, lift, -6), (12, hoog, 12), gezicht={"ey": 0.42})
    m.cube("lijf", "slijm", (-5, lift + hoog, -5), (10, 1.5, 10))
    top = lift + hoog + 1
    m.bone("oor_l", "lijf", (-3.5, top, 0), (0, 0, 18))
    m.cube("oor_l", "slijm", (-5.5, top - 0.5, -1), (3, 3.5, 2))
    m.cube("oor_l", "oor", (-5, top, -1.25), (2, 2.5, 0.5))
    m.bone("oor_r", "lijf", (3.5, top, 0), (0, 0, -18))
    m.spiegel("oor_r", "slijm", (-5.5, top - 0.5, -1), (3, 3.5, 2))
    m.spiegel("oor_r", "oor", (-5, top, -1.25), (2, 2.5, 0.5))
    return m, top


def slijm_anims(extra=None, lift=0.0):
    a = {
        "idle": anim(2.0, {"lijf": {"scale": kf([(0, (1, 1, 1)), (1.0, (1.05, 0.95, 1.05)), (2.0, (1, 1, 1))])},
                           "oor_l": {"rotation": kf([(0, (0, 0, 0)), (1.0, (0, 0, -6)), (2.0, (0, 0, 0))])},
                           "oor_r": {"rotation": kf([(0, (0, 0, 0)), (1.0, (0, 0, 6)), (2.0, (0, 0, 0))])}}),
        "hop": anim(0.7, {"lijf": {"scale": kf([(0, (1.15, 0.8, 1.15)), (0.15, (0.9, 1.18, 0.9)), (0.45, (1, 1, 1)), (0.6, (1.12, 0.85, 1.12)),
                                                (0.7, (1.15, 0.8, 1.15))])},
                          "oor_l": {"rotation": kf([(0, (0, 0, 10)), (0.2, (0, 0, -22)), (0.5, (0, 0, 8)), (0.7, (0, 0, 10))])},
                          "oor_r": {"rotation": kf([(0, (0, 0, -10)), (0.2, (0, 0, 22)), (0.5, (0, 0, -8)), (0.7, (0, 0, -10))])}}),
        "aanval": anim(0.5, {"lijf": {"position": kf([(0, (0, 0, 0)), (0.15, (0, 0, 1.5)), (0.3, (0, 1, -4)), (0.5, (0, 0, 0))]),
                                      "scale": kf([(0, (1, 1, 1)), (0.15, (1.15, 0.85, 1.15)), (0.3, (0.9, 1.1, 1.2)), (0.5, (1, 1, 1))])}},
                       loop=False),
        "blij": anim(0.6, {"lijf": {"scale": kf([(0, (1.1, 0.9, 1.1)), (0.3, (0.95, 1.1, 0.95)), (0.6, (1.1, 0.9, 1.1))]),
                                    "rotation": kf([(0, (0, 0, -8)), (0.3, (0, 0, 8)), (0.6, (0, 0, -8))])},
                           "oor_l": {"rotation": kf([(0, (0, 0, 20)), (0.15, (0, 0, -25)), (0.3, (0, 0, 20)), (0.45, (0, 0, -25)), (0.6, (0, 0, 20))])},
                           "oor_r": {"rotation": kf([(0, (0, 0, -20)), (0.15, (0, 0, 25)), (0.3, (0, 0, -20)), (0.45, (0, 0, 25)), (0.6, (0, 0, -20))])}}),
    }
    if extra:
        a.update(extra)
    return a


SLIJM = {
    # naam: (main colour, ear inside, pattern, second colour)
    "klodder": ((120, 214, 140), (255, 170, 200), "glans", None),
    "kloddertje": ((150, 226, 160), (255, 170, 200), "glans", None),
    "bubbelklodder": ((110, 180, 250), (255, 170, 210), "glans", None),
    "stuiterklodder": ((250, 214, 90), (255, 160, 190), "glans", None),
    "moederklodder": ((196, 140, 236), (255, 170, 210), "glans", None),
    "druipslijm": ((70, 170, 160), (255, 160, 200), "glans", None),
    "kristalklodder": ((200, 180, 255), (255, 180, 220), "glans", None),
    "kampklodder": ((255, 150, 200), (255, 220, 236), "glans", None),
}


def model_slijm(naam):
    lift = 3.0 if naam == "stuiterklodder" else 0.0
    m, top = slijm_basis(naam, hoog=11 if naam == "stuiterklodder" else 10, lift=lift)
    extra = {}
    if naam == "bubbelklodder":
        m.bone("bel", "lijf", (4, top, -2))
        m.cube("bel", "bel", (2.5, top - 1, -4), (3.5, 3.5, 3.5))
        m.cube("bel", "bel", (5.5, top + 2.5, -3), (2, 2, 2))
        extra["spuug"] = anim(0.6, {"lijf": {"scale": kf([(0, (1, 1, 1)), (0.3, (1.2, 1.2, 1.2)), (0.4, (0.9, 0.95, 1.1)), (0.6, (1, 1, 1))])},
                                    "bel": {"scale": kf([(0, (1, 1, 1)), (0.3, (1.6, 1.6, 1.6)), (0.4, (0.2, 0.2, 0.2)), (0.6, (1, 1, 1))])}},
                               loop=False)
    elif naam == "stuiterklodder":
        m.bone("veer", "root", (0, 0, 0))
        for i in range(3):
            m.cube("veer", "veer", (-3.5 + (i % 2) * 0.5, i * 1.0, -3.5 + (i % 2) * 0.5), (7 - (i % 2), 0.8, 7 - (i % 2)))
        extra["stuiter"] = anim(0.9, {"lijf": {"position": kf([(0, (0, -2, 0)), (0.3, (0, 1.5, 0)), (0.9, (0, 0, 0))]),
                                               "scale": kf([(0, (1.2, 0.8, 1.2)), (0.3, (0.85, 1.25, 0.85)), (0.9, (1, 1, 1))])},
                                      "veer": {"scale": kf([(0, (1, 0.4, 1)), (0.3, (1, 2.0, 1)), (0.9, (1, 1, 1))])}}, loop=False)
    elif naam in ("moederklodder", "kampklodder"):
        m.bone("strik", "lijf", (0, top, -1))
        k = "strik" if naam == "moederklodder" else "bloem"
        m.cube("strik", k, (-1, top - 0.2, -1.5), (2, 2, 1.5))
        m.cube("strik", k, (-4, top - 0.6, -1.25), (3, 2.8, 1), rot=(0, 0, 15), pivot=(-1, top + 0.8, -1))
        m.cube("strik", k, (1, top - 0.6, -1.25), (3, 2.8, 1), rot=(0, 0, -15), pivot=(1, top + 0.8, -1))
        if naam == "kampklodder":
            m.cube("strik", "bloemhart", (-0.6, top + 0.3, -1.8), (1.2, 1.2, 0.4))
            extra["aai"] = anim(1.0, {"lijf": {"scale": kf([(0, (1, 1, 1)), (0.2, (1.25, 0.75, 1.25)), (0.5, (0.9, 1.15, 0.9)), (0.75, (1.08, 0.94, 1.08)),
                                                          (1.0, (1, 1, 1))])},
                                      "oor_l": {"rotation": kf([(0, (0, 0, 0)), (0.25, (0, 0, -30)), (0.5, (0, 0, 20)), (0.75, (0, 0, -15)), (1.0, (0, 0, 0))])},
                                      "oor_r": {"rotation": kf([(0, (0, 0, 0)), (0.25, (0, 0, 30)), (0.5, (0, 0, -20)), (0.75, (0, 0, 15)), (1.0, (0, 0, 0))])}},
                                loop=False)
        else:
            extra["speciaal"] = anim(0.8, {"lijf": {"scale": kf([(0, (1, 1, 1)), (0.4, (1.3, 0.7, 1.3)), (0.55, (0.9, 1.2, 0.9)), (0.8, (1, 1, 1))])}},
                                     loop=False)
    elif naam == "druipslijm":
        m.bone("druppels", "lijf", (0, 0, 0))
        for i, (x, z, l) in enumerate(((-5.5, -6.3, 3), (3, -6.3, 2), (-6.3, 1, 2.5), (5.6, -2, 3.5), (0, 5.6, 2.2))):
            m.cube("druppels", "druppel", (x, -l + 1, z), (1.4, l, 0.8))
        extra["plafond"] = anim(1.6, {"lijf": {"scale": kf([(0, (1, 1.05, 1)), (0.8, (1.04, 1.15, 1.04)), (1.6, (1, 1.05, 1))])},
                                      "druppels": {"scale": kf([(0, (1, 1, 1)), (0.8, (1, 1.6, 1)), (1.6, (1, 1, 1))])}})
    elif naam == "kristalklodder":
        m.bone("kristallen", "lijf", (0, top, 0))
        for (x, z, hgt, rz, rx) in ((-1, -1, 6, 8, -6), (2.5, 1.5, 4, -22, 10), (-3.5, 2, 3.5, 25, 12), (0.5, -3.5, 3, -10, -20)):
            m.cube("kristallen", "kristal", (x, top - 1.5, z), (1.8, hgt, 1.8), rot=(rx, 0, rz), pivot=(x + 0.9, top - 1, z + 0.9))
        extra["speciaal"] = anim(0.6, {"kristallen": {"scale": kf([(0, (1, 1, 1)), (0.2, (1.3, 1.3, 1.3)), (0.6, (1, 1, 1))])}}, loop=False)
    return m, slijm_anims(extra)


# =====================================================================================================================
# the mushroom family (guh ears on the cap, the face on the stem)
# =====================================================================================================================
def zwam_oren(m, top, breed=4.0):
    m.bone("oor_l", "hoed", (-breed, top, 0), (0, 0, 16))
    m.cube("oor_l", "hoed", (-breed - 1.5, top - 0.5, -1), (3, 3.5, 2))
    m.cube("oor_l", "oor", (-breed - 1, top, -1.25), (2, 2.5, 0.5))
    m.bone("oor_r", "hoed", (breed, top, 0), (0, 0, -16))
    m.spiegel("oor_r", "hoed", (-breed - 1.5, top - 0.5, -1), (3, 3.5, 2))
    m.spiegel("oor_r", "oor", (-breed - 1, top, -1.25), (2, 2.5, 0.5))


def zwam_voeten(m, breed=2.6, lang=3.0, hoog=1.2):
    m.bone("voet_l", "root", (-1.6, hoog, 0))
    m.cube("voet_l", "voet", (-breed - 0.2, 0, -lang / 2 - 0.4), (breed, hoog, lang))
    m.bone("voet_r", "root", (1.6, hoog, 0))
    m.spiegel("voet_r", "voet", (-breed - 0.2, 0, -lang / 2 - 0.4), (breed, hoog, lang))


def zwam_armen(m, y, x, lang=6.0, dik=2.5):
    m.bone("arm_l", "steel", (-x, y, 0))
    m.cube("arm_l", "steel", (-x - dik, y - lang, -dik / 2), (dik, lang, dik))
    m.bone("arm_r", "steel", (x, y, 0))
    m.spiegel("arm_r", "steel", (-x - dik, y - lang, -dik / 2), (dik, lang, dik))


def zwam_anims(extra=None, armen=False):
    loop = {"voet_l": {"rotation": kf([(0, (-25, 0, 0)), (0.25, (25, 0, 0)), (0.5, (-25, 0, 0))])},
            "voet_r": {"rotation": kf([(0, (25, 0, 0)), (0.25, (-25, 0, 0)), (0.5, (25, 0, 0))])},
            "steel": {"rotation": kf([(0, (0, 0, -4)), (0.25, (0, 0, 4)), (0.5, (0, 0, -4))]),
                      "position": kf([(0, (0, 0, 0)), (0.125, (0, 0.6, 0)), (0.25, (0, 0, 0)), (0.375, (0, 0.6, 0)), (0.5, (0, 0, 0))])},
            "hoed": {"rotation": kf([(0, (0, 0, 3)), (0.25, (0, 0, -3)), (0.5, (0, 0, 3))])}}
    if armen:
        loop["arm_l"] = {"rotation": kf([(0, (25, 0, 0)), (0.25, (-25, 0, 0)), (0.5, (25, 0, 0))])}
        loop["arm_r"] = {"rotation": kf([(0, (-25, 0, 0)), (0.25, (25, 0, 0)), (0.5, (-25, 0, 0))])}
    a = {
        "idle": anim(2.4, {"hoed": {"rotation": kf([(0, (0, 0, 0)), (1.2, (3, 0, 2)), (2.4, (0, 0, 0))]),
                                    "position": kf([(0, (0, 0, 0)), (1.2, (0, 0.4, 0)), (2.4, (0, 0, 0))])},
                           "oor_l": {"rotation": kf([(0, (0, 0, 0)), (1.2, (0, 0, -8)), (2.4, (0, 0, 0))])},
                           "oor_r": {"rotation": kf([(0, (0, 0, 0)), (1.2, (0, 0, 8)), (2.4, (0, 0, 0))])}}),
        "loop": anim(0.5, loop),
        "aanval": anim(0.5, {"steel": {"rotation": kf([(0, (0, 0, 0)), (0.15, (-15, 0, 0)), (0.3, (25, 0, 0)), (0.5, (0, 0, 0))])},
                             "hoed": {"rotation": kf([(0, (0, 0, 0)), (0.3, (15, 0, 0)), (0.5, (0, 0, 0))])}}, loop=False),
        "blij": anim(0.6, {"steel": {"rotation": kf([(0, (0, 0, -10)), (0.3, (0, 0, 10)), (0.6, (0, 0, -10))]),
                                     "position": kf([(0, (0, 0, 0)), (0.15, (0, 2, 0)), (0.3, (0, 0, 0)), (0.45, (0, 2, 0)), (0.6, (0, 0, 0))])},
                           "oor_l": {"rotation": kf([(0, (0, 0, 20)), (0.15, (0, 0, -20)), (0.3, (0, 0, 20)), (0.45, (0, 0, -20)), (0.6, (0, 0, 20))])},
                           "oor_r": {"rotation": kf([(0, (0, 0, -20)), (0.15, (0, 0, 20)), (0.3, (0, 0, -20)), (0.45, (0, 0, 20)), (0.6, (0, 0, -20))])}}),
    }
    if extra:
        a.update(extra)
    return a


ZWAM = {
    # rol -> colour, pattern, second colour
    "hoedjesbol": {"hoed": ((230, 70, 80), "stippen", (255, 246, 236)), "steel": ((250, 236, 214), None, None), "voet": ((236, 214, 190), None, None),
                   "oor": ((255, 170, 200), None, None)},
    "sporenpuffer": {"hoed": ((200, 210, 140), "plekjes", (170, 186, 110)), "steel": ((226, 228, 170), "plekjes", (200, 206, 140)),
                     "voet": ((196, 190, 140), None, None), "oor": ((255, 170, 200), None, None), "spore": ((170, 220, 120), "plekjes", (220, 250, 170))},
    "zwamwachter": {"hoed": ((176, 110, 70), "stippen", (232, 206, 170)), "steel": ((236, 222, 196), None, None), "voet": ((206, 186, 160), None, None),
                    "oor": ((255, 170, 200), None, None), "schild": ((160, 96, 60), "stippen", (236, 210, 176)), "knop": ((236, 196, 90), None, None)},
    "glimzwam": {"hoed": ((90, 200, 240), "stippen", (200, 250, 255)), "steel": ((226, 240, 246), None, None), "voet": ((196, 214, 226), None, None),
                 "oor": ((255, 170, 220), None, None), "glimvlek": ((180, 250, 255), None, None)},
    "tempelridder": {"hoed": ((150, 60, 90), "stippen", (240, 210, 220)), "steel": ((240, 226, 206), None, None), "voet": ((120, 110, 130), None, None),
                     "oor": ((255, 170, 200), None, None), "plaat": ((196, 196, 214), "strepen", (170, 170, 196)), "zwaard": ((220, 224, 236), None, None),
                     "gevest": ((236, 190, 80), None, None), "vizier": ((120, 110, 130), None, None)},
    "sporenpriester": {"hoed": ((120, 80, 170), "stippen", (230, 210, 250)), "steel": ((240, 228, 214), None, None), "voet": ((90, 70, 120), None, None),
                       "oor": ((255, 170, 210), None, None), "gewaad": ((96, 70, 140), "strepen", (130, 100, 180)), "staf": ((150, 110, 70), None, None),
                       "bol": ((190, 255, 160), None, None)},
}


def model_zwam(naam):
    m = Model(naam, px=4, breedte=64)
    m.bone("root")
    extra = {}
    armen = False
    if naam == "hoedjesbol":
        m.bone("steel", "root", (0, 1, 0))
        m.cube("steel", "steel", (-3, 1, -3), (6, 6, 6), gezicht={"ey": 0.45})
        m.bone("hoed", "steel", (0, 7, 0))
        m.cube("hoed", "hoed", (-6, 7, -6), (12, 4, 12))
        m.cube("hoed", "hoed", (-4.5, 11, -4.5), (9, 2, 9))
        zwam_oren(m, 12.5, 3.2)
        zwam_voeten(m)
        extra["ram"] = anim(0.8, {"steel": {"rotation": kf([(0, (0, 0, 0)), (0.6, (-25, 0, 0)), (0.8, (35, 0, 0))]),
                                            "position": kf([(0, (0, 0, 0)), (0.6, (0, -1, 2)), (0.8, (0, 0, -2))])},
                                  "hoed": {"rotation": kf([(0, (0, 0, 0)), (0.6, (-10, 0, 0)), (0.8, (20, 0, 0))])}}, loop="hold")
    elif naam == "sporenpuffer":
        m.bone("steel", "root", (0, 1, 0))
        m.cube("steel", "steel", (-6, 1, -6), (12, 10, 12), gezicht={"ey": 0.5})
        m.bone("hoed", "steel", (0, 11, 0))
        m.cube("hoed", "hoed", (-5, 11, -5), (10, 2, 10))
        m.cube("hoed", "spore", (-2, 13, -2), (4, 1, 4))
        zwam_oren(m, 12.5, 3.6)
        zwam_voeten(m)
        extra["puf"] = anim(0.8, {"steel": {"scale": kf([(0, (1, 1, 1)), (0.35, (1.15, 1.15, 1.15)), (0.45, (0.85, 0.8, 0.85)), (0.8, (1, 1, 1))])},
                                  "hoed": {"position": kf([(0, (0, 0, 0)), (0.45, (0, 2, 0)), (0.8, (0, 0, 0))])}}, loop=False)
    elif naam == "zwamwachter":
        m.bone("steel", "root", (0, 2, 0))
        m.cube("steel", "steel", (-4, 2, -3.5), (8, 12, 7), gezicht={"ey": 0.3, "schaal": 1.0})
        m.bone("hoed", "steel", (0, 14, 0))
        m.cube("hoed", "hoed", (-7.5, 14, -7.5), (15, 4, 15))
        m.cube("hoed", "hoed", (-5, 18, -5), (10, 2, 10))
        zwam_oren(m, 19.5, 3.8)
        zwam_armen(m, 12, 4, 7, 2.8)
        m.bone("schild", "arm_l", (-3, 6, -4))
        m.cube("schild", "schild", (-7.5, 2, -7.5), (11, 11, 1.6))
        m.cube("schild", "knop", (-3.5, 6, -8.5), (3, 3, 1))
        zwam_voeten(m, 3, 3.6, 2)
        armen = True
        extra["blok"] = anim(1.0, {"arm_l": {"rotation": kf([(0, (-40, 0, 0)), (1.0, (-40, 0, 0))])},
                                   "schild": {"rotation": kf([(0, (40, 0, 0)), (1.0, (40, 0, 0))])}})
        extra["klap"] = anim(1.2, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.7, (-160, 0, 0)), (0.85, (-20, 0, 0)), (1.2, (0, 0, 0))])},
                                   "arm_l": {"rotation": kf([(0, (-40, 0, 0)), (0.6, (-10, 0, -30)), (1.2, (0, 0, -20))])},
                                   "steel": {"rotation": kf([(0, (0, 0, 0)), (0.7, (-10, 0, 0)), (0.85, (18, 0, 0)), (1.2, (0, 0, 0))])}}, loop="hold")
    elif naam == "glimzwam":
        m.bone("steel", "root", (0, 1, 0))
        m.cube("steel", "steel", (-2.5, 1, -2.5), (5, 5, 5), gezicht={"ey": 0.42})
        m.bone("hoed", "steel", (0, 6, 0))
        m.cube("hoed", "hoed", (-5, 6, -5), (10, 3.5, 10))
        m.cube("hoed", "glimvlek", (-3.5, 9.5, -3.5), (7, 1.5, 7))
        zwam_oren(m, 10.5, 2.6)
        zwam_voeten(m, 2, 2.4, 1)
        extra["glim"] = anim(1.0, {"hoed": {"scale": kf([(0, (1, 1, 1)), (0.4, (1.25, 1.1, 1.25)), (1.0, (1, 1, 1))])},
                                   "steel": {"position": kf([(0, (0, 0, 0)), (0.4, (0, 1.5, 0)), (1.0, (0, 0, 0))])}}, loop=False)
    elif naam == "tempelridder":
        m.bone("steel", "root", (0, 7, 0))
        m.cube("steel", "steel", (-4.5, 7, -3.5), (9, 13, 7), gezicht={"ey": 0.28, "schaal": 0.95})
        m.cube("steel", "plaat", (-4.8, 8, -3.9), (9.6, 6, 0.6))
        m.bone("hoed", "steel", (0, 20, 0))
        m.cube("hoed", "hoed", (-7.5, 20, -7.5), (15, 3.5, 15))
        m.cube("hoed", "hoed", (-5.5, 23.5, -5.5), (11, 2.5, 11))
        m.cube("hoed", "vizier", (-6, 19, -7.6), (12, 1.2, 1))
        zwam_oren(m, 25.5, 4.0)
        zwam_armen(m, 18, 4.5, 8, 3)
        m.bone("zwaard", "arm_r", (6, 11, 0))
        m.cube("zwaard", "gevest", (5, 9.5, -1), (2, 2, 2))
        m.cube("zwaard", "gevest", (4.5, 9.8, -2.5), (3, 1.2, 1))
        m.cube("zwaard", "zwaard", (5.4, 10.1, -16.5), (1.2, 1.4, 14))
        m.bone("been_l", "root", (-2, 7, 0))
        m.cube("been_l", "voet", (-4, 0, -2), (3.5, 7, 4))
        m.bone("been_r", "root", (2, 7, 0))
        m.spiegel("been_r", "voet", (-4, 0, -2), (3.5, 7, 4))
        armen = True
        extra["uithaal"] = anim(1.4, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.8, (-150, -30, 0)), (0.95, (40, 30, 0)), (1.4, (0, 0, 0))])},
                                      "steel": {"rotation": kf([(0, (0, 0, 0)), (0.8, (-8, 25, 0)), (0.95, (12, -30, 0)), (1.4, (0, 0, 0))])}}, loop="hold")
        extra["loop"] = anim(0.6, {"been_l": {"rotation": kf([(0, (-30, 0, 0)), (0.3, (30, 0, 0)), (0.6, (-30, 0, 0))])},
                                   "been_r": {"rotation": kf([(0, (30, 0, 0)), (0.3, (-30, 0, 0)), (0.6, (30, 0, 0))])},
                                   "arm_l": {"rotation": kf([(0, (20, 0, 0)), (0.3, (-20, 0, 0)), (0.6, (20, 0, 0))])},
                                   "steel": {"rotation": kf([(0, (0, 0, -3)), (0.3, (0, 0, 3)), (0.6, (0, 0, -3))])}})
    elif naam == "sporenpriester":
        m.bone("steel", "root", (0, 0, 0))
        m.cube("steel", "gewaad", (-5, 0, -5), (10, 5, 10))
        m.cube("steel", "steel", (-4, 5, -4), (8, 10, 8), gezicht={"ey": 0.35})
        m.cube("steel", "gewaad", (-4.3, 5, -4.3), (8.6, 4, 8.6))
        m.bone("hoed", "steel", (0, 15, 0))
        for i, (b, hh) in enumerate(((13, 3), (9, 3), (5.5, 3), (2.5, 2.5))):
            m.cube("hoed", "hoed", (-b / 2, 15 + sum(x[1] for x in ((13, 3), (9, 3), (5.5, 3), (2.5, 2.5))[:i]), -b / 2), (b, hh, b))
        zwam_oren(m, 18.5, 3.3)
        zwam_armen(m, 13, 4, 6, 2.4)
        m.bone("staf", "arm_r", (5.5, 8, 0))
        m.cube("staf", "staf", (5, -2, -1.5), (1.2, 20, 1.2))
        m.cube("staf", "bol", (4.1, 18, -2.4), (3, 3, 3))
        armen = True
        extra["spreuk"] = anim(1.0, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.4, (-120, 0, 0)), (0.7, (-120, 0, 0)), (1.0, (0, 0, 0))])},
                                     "arm_l": {"rotation": kf([(0, (0, 0, 0)), (0.4, (-60, 0, 30)), (1.0, (0, 0, 0))])},
                                     "hoed": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-10, 0, 0)), (1.0, (0, 0, 0))])}}, loop=False)
        extra["loop"] = anim(1.0, {"steel": {"position": kf([(0, (0, 0, 0)), (0.5, (0, 0.8, 0)), (1.0, (0, 0, 0))]),
                                             "rotation": kf([(0, (0, 0, -3)), (0.5, (0, 0, 3)), (1.0, (0, 0, -3))])}})
    return m, zwam_anims(extra, armen)


# =====================================================================================================================
# Mosgeest, Grauwe Knaag
# =====================================================================================================================
def model_mosgeest():
    m = Model("mosgeest", px=4, breedte=64)
    m.bone("root")
    m.bone("lijf", "root", (0, 8, 0))
    m.cube("lijf", "geest", (-5, 4, -5), (10, 9, 10), gezicht={"ey": 0.45})
    m.cube("lijf", "mos", (-5.5, 12, -5.5), (11, 2, 11))
    m.cube("lijf", "mos", (-3, 14, -2), (3, 1.5, 3))
    m.bone("staart1", "lijf", (0, 5, 3))
    m.cube("staart1", "geest", (-3.5, 2, 1), (7, 4, 6))
    m.bone("staart2", "staart1", (0, 3, 7))
    m.cube("staart2", "geest", (-2, 1, 6), (4, 3, 5))
    m.bone("staart3", "staart2", (0, 2, 11))
    m.cube("staart3", "mos", (-1, 0.5, 10.5), (2, 2, 4))
    m.bone("oor_l", "lijf", (-3.5, 13.5, 0), (0, 0, 20))
    m.cube("oor_l", "mos", (-5.5, 13, -1), (3, 3.5, 2))
    m.cube("oor_l", "oor", (-5, 13.5, -1.25), (2, 2.5, 0.5))
    m.bone("oor_r", "lijf", (3.5, 13.5, 0), (0, 0, -20))
    m.spiegel("oor_r", "mos", (-5.5, 13, -1), (3, 3.5, 2))
    m.spiegel("oor_r", "oor", (-5, 13.5, -1.25), (2, 2.5, 0.5))
    golf = lambda b, a, l: {"rotation": kf([(0, (0, a, 0)), (l / 2, (0, -a, 0)), (l, (0, a, 0))])}
    anims = {
        "idle": anim(2.0, {"lijf": {"position": kf([(0, (0, 0, 0)), (1.0, (0, 1.5, 0)), (2.0, (0, 0, 0))])},
                           "staart1": golf("staart1", 10, 2.0), "staart2": golf("staart2", 16, 2.0), "staart3": golf("staart3", 24, 2.0)}),
        "loop": anim(1.0, {"lijf": {"rotation": kf([(0, (12, 0, 0)), (1.0, (12, 0, 0))]),
                                    "position": kf([(0, (0, 0, 0)), (0.5, (0, 1, 0)), (1.0, (0, 0, 0))])},
                           "staart1": golf("staart1", 18, 0.5), "staart2": golf("staart2", 26, 0.5), "staart3": golf("staart3", 34, 0.5)}),
        "aanval": anim(0.6, {"lijf": {"scale": kf([(0, (1, 1, 1)), (0.25, (1.2, 0.85, 1.2)), (0.4, (0.9, 1.1, 0.9)), (0.6, (1, 1, 1))])}}, loop=False),
        "ontwijk": anim(0.4, {"lijf": {"scale": kf([(0, (0.6, 1.3, 0.6)), (0.2, (1.2, 0.85, 1.2)), (0.4, (1, 1, 1))]),
                                       "rotation": kf([(0, (0, 0, 30)), (0.4, (0, 0, 0))])}}, loop=False),
        "blij": anim(0.8, {"lijf": {"rotation": kf([(0, (0, 0, 0)), (0.8, (0, 360, 0))]),
                                    "position": kf([(0, (0, 0, 0)), (0.4, (0, 3, 0)), (0.8, (0, 0, 0))])}}),
    }
    pal = {"geest": ((170, 220, 190), "plekjes", (200, 240, 214)), "mos": ((96, 160, 80), "plekjes", (130, 196, 96)), "oor": ((255, 170, 200), None, None)}
    return m, anims, pal


def model_knaag():
    m = Model("knaag", px=4, breedte=64)
    m.bone("root")
    m.bone("lijf", "root", (0, 5, 0))
    m.cube("lijf", "vacht", (-3.5, 3, -4), (7, 6, 10))
    m.cube("lijf", "buik", (-2.5, 2.6, -3), (5, 1, 7))
    m.bone("kop", "lijf", (0, 6, -4))
    m.cube("kop", "vacht", (-3, 3.5, -9.5), (6, 5.5, 6), gezicht={"mood": "knaag", "ey": 0.4})
    m.cube("kop", "vacht", (-1.5, 3.5, -11), (3, 3, 1.6))
    m.cube("kop", "neus", (-0.6, 5.4, -11.4), (1.2, 0.9, 0.5))
    m.cube("kop", "tand", (-1, 2.4, -10.6), (2, 1.4, 0.5))
    m.cube("kop", "vacht", (-3.6, 8.2, -7.5), (2.6, 2.6, 0.8))
    m.cube("kop", "oor", (-3.2, 8.6, -7.8), (1.8, 1.8, 0.4))
    m.spiegel("kop", "vacht", (-3.6, 8.2, -7.5), (2.6, 2.6, 0.8))
    m.spiegel("kop", "oor", (-3.2, 8.6, -7.8), (1.8, 1.8, 0.4))
    m.bone("staart", "lijf", (0, 6, 6), (-25, 0, 0))
    m.cube("staart", "staart", (-0.5, 5.5, 6), (1, 1, 11))
    for naam, x, z in (("poot_lv", -2.5, -2.5), ("poot_rv", 2.5, -2.5), ("poot_la", -2.5, 4.5), ("poot_ra", 2.5, 4.5)):
        m.bone(naam, "root", (x, 3, z))
        m.cube(naam, "poot", (x - 1, 0, z - 1), (2, 3, 2))
    loopkf = lambda a: {"rotation": kf([(0, (a, 0, 0)), (0.15, (-a, 0, 0)), (0.3, (a, 0, 0))])}
    anims = {
        "idle": anim(1.5, {"staart": {"rotation": kf([(0, (0, -10, 0)), (0.75, (0, 10, 0)), (1.5, (0, -10, 0))])},
                           "kop": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-4, 0, 0)), (0.4, (0, 0, 0)), (0.5, (-4, 0, 0)), (0.6, (0, 0, 0)), (1.5, (0, 0, 0))])}}),
        "loop": anim(0.3, {"poot_lv": loopkf(40), "poot_ra": loopkf(40), "poot_rv": loopkf(-40), "poot_la": loopkf(-40),
                           "lijf": {"position": kf([(0, (0, 0, 0)), (0.075, (0, 0.6, 0)), (0.15, (0, 0, 0)), (0.225, (0, 0.6, 0)), (0.3, (0, 0, 0))])},
                           "staart": {"rotation": kf([(0, (0, -18, 0)), (0.15, (0, 18, 0)), (0.3, (0, -18, 0))])}}),
        "aanval": anim(0.4, {"kop": {"rotation": kf([(0, (0, 0, 0)), (0.1, (-20, 0, 0)), (0.2, (25, 0, 0)), (0.4, (0, 0, 0))]),
                                     "position": kf([(0, (0, 0, 0)), (0.2, (0, 0, -2)), (0.4, (0, 0, 0))])}}, loop=False),
        "blij": anim(0.5, {"lijf": {"position": kf([(0, (0, 0, 0)), (0.25, (0, 3, 0)), (0.5, (0, 0, 0))])},
                           "staart": {"rotation": kf([(0, (0, -30, 0)), (0.25, (0, 30, 0)), (0.5, (0, -30, 0))])}}),
    }
    pal = {"vacht": ((150, 146, 150), "plekjes", (132, 128, 134)), "buik": ((200, 196, 196), None, None), "neus": ((240, 140, 160), None, None),
           "tand": ((250, 248, 232), None, None), "oor": ((236, 170, 180), None, None), "staart": ((216, 160, 170), None, None),
           "poot": ((206, 160, 170), None, None)}
    return m, anims, pal


# =====================================================================================================================
# Vahoegnir, de Vadsloze Draak (flyover model, scaled x2.5 in the renderer)
# =====================================================================================================================
def model_draak():
    m = Model("draak", px=2, breedte=256, bounds=(40, 20, 0))
    m.bone("root")
    m.bone("lijf", "root", (0, 10, 0))
    m.cube("lijf", "pluis", (-9, 2, -14), (18, 16, 30))
    m.cube("lijf", "buik", (-7, 1, -12), (14, 3, 24))
    m.cube("lijf", "pluis2", (-10, 4, -17), (20, 13, 6), inflate=0.5)
    for i in range(6):
        m.cube("lijf", "zwart", (-1.5 - (i % 2), 18, -12 + i * 5), (3 + (i % 2) * 2, 2.5 + (i % 3), 3))
    for (x, y, z) in ((-9.6, 8, -6), (8, 12, 4), (-9.4, 13, 10), (7.8, 6, -10), (-4, 1.2, 6)):
        m.cube("lijf", "zwart", (x, y, z), (1.8, 2.2, 2.2))
    # neck + head
    m.bone("nek1", "lijf", (0, 12, -14))
    m.cube("nek1", "pluis", (-5, 8, -24), (10, 10, 11))
    m.cube("nek1", "zwart", (-1.5, 18, -22), (3, 2, 3))
    m.bone("nek2", "nek1", (0, 13, -23))
    m.cube("nek2", "pluis", (-4.5, 10, -32), (9, 9, 10))
    m.cube("nek2", "zwart", (-1.2, 19, -30), (2.4, 2, 3))
    m.bone("kop", "nek2", (0, 15, -31))
    m.cube("kop", "kop", (-7, 10, -44), (14, 11, 14), gezicht={"mood": "draak", "ey": 0.35})
    m.cube("kop", "kop", (-5, 10, -52.5), (10, 7, 9))
    m.cube("kop", "neus", (-3, 15.2, -52.8), (6, 1.4, 0.6))
    m.cube("kop", "pluis2", (-6, 20.5, -42), (12, 3, 10))
    m.bone("kaak", "kop", (0, 10, -44))
    m.cube("kaak", "kop2", (-4.5, 7, -51.5), (9, 3, 9))
    for x in (-3.8, -1.2, 1.2, 3.0):
        m.cube("kaak", "tand", (x, 10, -51.3), (0.8, 1.4, 0.8))
    # the guh ears as horns: long, swept back, faded pink inside
    m.bone("oor_l", "kop", (-5, 20, -38), (-38, 0, -18))
    m.cube("oor_l", "pluis", (-8.5, 19, -39.5), (5, 15, 3.5))
    m.cube("oor_l", "oor", (-7.8, 20.5, -39.9), (3.6, 11, 0.6))
    m.cube("oor_l", "zwart", (-7.5, 33.5, -39), (3, 2.5, 2.5))
    m.bone("oor_r", "kop", (5, 20, -38), (-38, 0, 18))
    m.spiegel("oor_r", "pluis", (-8.5, 19, -39.5), (5, 15, 3.5))
    m.spiegel("oor_r", "oor", (-7.8, 20.5, -39.9), (3.6, 11, 0.6))
    m.spiegel("oor_r", "zwart", (-7.5, 33.5, -39), (3, 2.5, 2.5))
    # wings: an arm bone with feathers hanging backward, an outer segment with longer feathers
    for kant in ("l", "r"):
        sp = m.cube if kant == "l" else m.spiegel
        sx = 1 if kant == "l" else -1
        m.bone(f"vleugel_{kant}", "lijf", (-9 * sx, 16, -8))
        sp(f"vleugel_{kant}", "bot", (-39, 15, -10), (30, 3, 4))
        for i in range(6):
            lengte = 14 + i * 1.6
            sp(f"vleugel_{kant}", "veer" if i % 2 == 0 else "veer2", (-14 - 4.4 * i, 15.4, -7), (5, 1, lengte))
        m.bone(f"vleugel2_{kant}", f"vleugel_{kant}", (-39 * sx, 16, -8))
        sp(f"vleugel2_{kant}", "bot", (-63, 15.3, -9.5), (24, 2.4, 3))
        for i in range(6):
            lengte = 23 - i * 2.2
            sp(f"vleugel2_{kant}", "veer2" if i % 2 == 0 else "veer", (-43.5 - 3.9 * i, 15.6, -7.5), (4.6, 0.9, lengte))
        sp(f"vleugel2_{kant}", "zwart", (-66, 15, -10), (4, 3, 4))
    # tail
    prev, z = "lijf", 15
    for i, (w, hh, l) in enumerate(((12, 10, 12), (9, 8, 12), (6, 6, 12), (4, 4, 10))):
        naam = f"staart{i + 1}"
        m.bone(naam, prev, (0, 10, z))
        m.cube(naam, "pluis", (-w / 2, 10 - hh / 2, z), (w, hh, l))
        prev, z = naam, z + l - 1
    m.cube("staart4", "pluis2", (-4.5, 5.5, z - 1), (9, 9, 7))
    m.cube("staart4", "zwart", (-2, 13, z), (4, 3, 4))
    for naam, x, zz in (("poot_lv", -7, -9), ("poot_rv", 7, -9), ("poot_la", -7, 9), ("poot_ra", 7, 9)):
        m.bone(naam, "lijf", (x, 4, zz), (35, 0, 0))
        m.cube(naam, "pluis", (x - 2.5, -4, zz - 2.5), (5, 8, 5))
        m.cube(naam, "klauw", (x - 2.5, -5, zz - 3.2), (5, 1.4, 1.4))
    staart = lambda a, l, fase: {"rotation": kf([(0, (fase * a, a, 0)), (l / 2, (-fase * a, -a, 0)), (l, (fase * a, a, 0))])}
    vlieg = {
        "vleugel_l": {"rotation": kf([(0, (0, 0, -32)), (0.8, (0, 6, 34)), (1.6, (0, 0, -32))])},
        "vleugel_r": {"rotation": kf([(0, (0, 0, 32)), (0.8, (0, -6, -34)), (1.6, (0, 0, 32))])},
        "vleugel2_l": {"rotation": kf([(0, (0, 0, 10)), (0.4, (0, 0, -18)), (1.0, (0, 0, 28)), (1.6, (0, 0, 10))])},
        "vleugel2_r": {"rotation": kf([(0, (0, 0, -10)), (0.4, (0, 0, 18)), (1.0, (0, 0, -28)), (1.6, (0, 0, -10))])},
        "lijf": {"position": kf([(0, (0, -2, 0)), (0.8, (0, 3, 0)), (1.6, (0, -2, 0))])},
        "nek1": {"rotation": kf([(0, (4, 0, 0)), (0.8, (-4, 0, 0)), (1.6, (4, 0, 0))])},
        "kop": {"rotation": kf([(0, (-4, 0, 0)), (0.8, (5, 0, 0)), (1.6, (-4, 0, 0))])},
        "staart1": staart(6, 1.6, 0.5), "staart2": staart(9, 1.6, 0.5), "staart3": staart(12, 1.6, 0.5), "staart4": staart(15, 1.6, 0.5),
    }
    zweef = {
        "vleugel_l": {"rotation": kf([(0, (0, 0, -6)), (2.0, (0, 0, -10)), (4.0, (0, 0, -6))])},
        "vleugel_r": {"rotation": kf([(0, (0, 0, 6)), (2.0, (0, 0, 10)), (4.0, (0, 0, 6))])},
        "vleugel2_l": {"rotation": kf([(0, (0, 0, 4)), (2.0, (0, 0, -4)), (4.0, (0, 0, 4))])},
        "vleugel2_r": {"rotation": kf([(0, (0, 0, -4)), (2.0, (0, 0, 4)), (4.0, (0, 0, -4))])},
        "lijf": {"position": kf([(0, (0, 0, 0)), (2.0, (0, -2, 0)), (4.0, (0, 0, 0))])},
        "staart1": staart(4, 4.0, 0.3), "staart2": staart(6, 4.0, 0.3), "staart3": staart(9, 4.0, 0.3), "staart4": staart(12, 4.0, 0.3),
    }
    brul = {
        "nek1": {"rotation": kf([(0, (0, 0, 0)), (0.4, (-25, 0, 0)), (1.6, (-25, 0, 0)), (2.0, (0, 0, 0))])},
        "nek2": {"rotation": kf([(0, (0, 0, 0)), (0.4, (-15, 0, 0)), (1.6, (-15, 0, 0)), (2.0, (0, 0, 0))])},
        "kop": {"rotation": kf([(0, (0, 0, 0)), (0.4, (-20, 0, 0)), (0.6, (-16, 0, 4)), (0.8, (-20, 0, -4)), (1.0, (-16, 0, 4)), (1.6, (-20, 0, 0)),
                                (2.0, (0, 0, 0))])},
        "kaak": {"rotation": kf([(0, (0, 0, 0)), (0.4, (38, 0, 0)), (1.6, (38, 0, 0)), (2.0, (0, 0, 0))])},
        "oor_l": {"rotation": kf([(0, (0, 0, 0)), (0.4, (12, 0, -10)), (1.6, (12, 0, -10)), (2.0, (0, 0, 0))])},
        "oor_r": {"rotation": kf([(0, (0, 0, 0)), (0.4, (12, 0, 10)), (1.6, (12, 0, 10)), (2.0, (0, 0, 0))])},
    }
    anims = {"vlieg": anim(1.6, vlieg), "zweef": anim(4.0, zweef), "brul": anim(2.0, brul, loop=False)}
    pal = {"pluis": ((128, 108, 140), "knetter", (34, 26, 40)), "pluis2": ((150, 132, 162), "plekjes", (120, 100, 136)),
           "buik": ((176, 160, 184), None, None), "zwart": ((28, 22, 34), "plekjes", (70, 40, 90)), "kop": ((136, 116, 150), "knetter", (40, 30, 48)),
           "kop2": ((120, 100, 134), None, None), "neus": ((60, 40, 70), None, None), "tand": ((236, 230, 240), None, None),
           "oor": ((196, 150, 186), None, None), "bot": ((110, 92, 124), None, None), "veer": ((164, 146, 178), "veer", (100, 84, 112)),
           "veer2": ((140, 120, 156), "veer", (86, 70, 100)), "klauw": ((50, 40, 56), None, None)}
    return m, anims, pal


# =====================================================================================================================
# armour + set items: 16x16 icons
# =====================================================================================================================
# id: (slot, name, colour, accent, set or None)
HARNAS = {
    "slijmmutsje": ("hoofd", "Slijmmutsje", (120, 214, 140), (255, 170, 200), None),
    "bubbelvest": ("borst", "Bubbelvest", (110, 180, 250), (220, 240, 255), None),
    "plakpantoffels": ("voeten", "Plakpantoffels", (250, 214, 90), (255, 160, 190), None),
    "steelbroek": ("benen", "Steelbroek", (240, 226, 206), (200, 170, 140), None),
    "knaagvachtje": ("borst", "Knaagvachtje", (150, 146, 150), (236, 170, 180), None),
    "kristalhelmpje": ("hoofd", "Kristalhelmpje", (200, 180, 255), (255, 255, 255), None),
    "klodderkapje": ("hoofd", "Klodderkapje", (255, 140, 190), (120, 214, 140), "klodder"),
    "klodderhesje": ("borst", "Klodderhesje", (255, 140, 190), (120, 214, 140), "klodder"),
    "klodderbroek": ("benen", "Klodderbroek", (255, 140, 190), (120, 214, 140), "klodder"),
    "klodderstuiters": ("voeten", "Klodderstuiters", (255, 140, 190), (120, 214, 140), "klodder"),
    "zwamhoed": ("hoofd", "Zwamhoed", (150, 90, 190), (240, 220, 250), "zwam"),
    "sporenmantel": ("borst", "Sporenmantel", (150, 90, 190), (190, 255, 160), "zwam"),
    "zwambroek": ("benen", "Zwambroek", (150, 90, 190), (240, 226, 206), "zwam"),
    "mossloffen": ("voeten", "Mossloffen", (96, 160, 80), (150, 90, 190), "zwam"),
}

VORMEN = {
    "hoofd": ["................", "................", "....########....", "...##########...", "..############..", "..############..",
              "..############..", "..####....####..", "..###......###..", "..###......###..", "................", "................",
              "................", "................", "................", "................"],
    "borst": ["................", "..###......###..", ".#####....#####.", ".##############.", ".##############.", "..############..",
              "...##########...", "...##########...", "...##########...", "...##########...", "...##########...", "...##########...",
              "...##########...", "................", "................", "................"],
    "benen": ["................", "...##########...", "...##########...", "...##########...", "...####..####...", "...####..####...",
              "...####..####...", "...####..####...", "...####..####...", "...####..####...", "...####..####...", "...####..####...",
              "................", "................", "................", "................"],
    "voeten": ["................", "................", "................", "................", "................", "................",
               "..####....####..", "..####....####..", "..####....####..", "..####....####..", ".#####...#####..", "######..######..",
               "######..######..", "................", "................", "................"],
}


def icoon(slot, kleur, accent, set_naam, seed):
    r = _rng(seed)
    a = np.zeros((16, 16, 4), np.uint8)
    vorm = VORMEN[slot]
    for y, rij in enumerate(vorm):
        for x, ch in enumerate(rij):
            if ch == "#":
                licht = 1.12 if (x + y) < 12 else 0.9 if (x + y) > 20 else 1.0
                a[y, x, :3] = np.clip(np.array(kleur) * licht + r.normal(0, 6, 3), 0, 255)
                a[y, x, 3] = 255
    # outline
    m = a[..., 3] > 0
    rand = np.zeros_like(m)
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        rand |= np.roll(np.roll(m, dy, 0), dx, 1)
    rand &= ~m
    a[rand, :3] = np.clip(np.array(kleur) * 0.45, 0, 255)
    a[rand, 3] = 255
    # accents: guh ears on helmets, a stripe or dots on the rest
    if slot == "hoofd":
        for x in (3, 4, 11, 12):
            a[1, x, :3], a[1, x, 3] = accent, 255
        a[2, 4, :3] = a[2, 11, :3] = accent
    elif slot == "borst":
        a[6:11, 7:9, :3] = accent
    elif slot == "benen":
        a[1:3, 3:13, :3] = accent
    else:
        a[11:13, 0:6, :3] = accent
        a[11:13, 8:14, :3] = accent
    if set_naam:
        a[13 if slot != "voeten" else 3, 7:9, :3] = (255, 230, 120) if set_naam == "klodder" else (190, 255, 160)
        a[13 if slot != "voeten" else 3, 7:9, 3] = 255
    return Image.fromarray(a)


# =====================================================================================================================
# texts
# =====================================================================================================================
ENTITEITEN = {
    "klodder": "Klodder", "kloddertje": "Kloddertje", "bubbelklodder": "Bubbelklodder", "stuiterklodder": "Stuiterklodder",
    "moederklodder": "Moederklodder", "hoedjesbol": "Hoedjesbol", "sporenpuffer": "Sporenpuffer", "zwamwachter": "Zwamwachter",
    "mosgeest": "Mosgeest", "druipslijm": "Druipslijm", "kristalklodder": "Kristalklodder", "knaag": "Grauwe Knaag",
    "glimzwam": "Glimzwam", "tempelridder": "Tempelzwam-ridder", "sporenpriester": "Sporenpriester", "kampklodder": "Kampklodder",
    "draak": "Vahoegnir, de Vadsloze Draak",
}

FLAVOUR = {
    "slijmmutsje": ["Zit als gegoten. Gegoten slijm, dat wel.", "Ruikt naar munt en een beetje naar klodder.", "Niet schudden. Echt niet schudden."],
    "bubbelvest": ["Elke knoop is een belletje. Niet prikken.", "Waterdicht, behalve van binnen.", "Plopt zachtjes als je ademt."],
    "plakpantoffels": ["Je loopt nooit meer weg. Letterlijk.", "Plakt aan de vloer, de trap en je hart.", "Voor op zondag en tijdens gevechten."],
    "steelbroek": ["Gemaakt van een zwamsteel. Hij vond het goed.", "Stevig, stijf en een beetje sponzig.", "Past bij elke hoed, vooral bij een paddenstoel."],
    "knaagvachtje": ["Al het knagen is eruit gewassen. Bijna.", "Grijs, warm en verdacht kriebelig.", "Er zit nog een kruimeltje in de zak."],
    "kristalhelmpje": ["Weerkaatst zonlicht, slechte grappen en spreuken.", "Glinstert zo hard dat je eigen schaduw knippert.", "Ook leuk als lamp."],
    "klodderkapje": ["Plakt aan je oortjes. Dat hoort zo.", "Klodder-chic, nu in de kleur vrolijk.", "Vahoeg! zegt het kapje. Kapjes praten niet. Toch?"],
    "klodderhesje": ["Glibberig van buiten, knus van binnen.", "Slaat iemand je? Stuiter gewoon terug.", "Gewassen op 30 graden, gestuiterd op 40."],
    "klodderbroek": ["Wiebelt bij elke stap. Bewust.", "Voor springen, stuiteren en pootjebaden.", "Twee pijpen vol vads."],
    "klodderstuiters": ["Boing. Boing. Boing. Je went eraan.", "Stuiterkracht: ja. Remkracht: nee.", "Wie stuitert, die blijft."],
    "zwamhoed": ["Groeit een klein beetje als het regent.", "Wijsheid zit in de hoed. En wat sporen.", "Hoedje op, wijsneus aan."],
    "sporenmantel": ["Hoesten mag, maar niet in je mouw.", "Elke zoom laat een spoortje achter.", "Gemaakt van mos, moed en mysterie."],
    "zwambroek": ["Stevig als een steel, zacht als een hoedje.", "Ritsje dicht, sporen erin.", "Met een zakje voor geheime paddenstoelen."],
    "mossloffen": ["Loopt stil. De mos doet het werk.", "Voeten groen, hart roze.", "Niet buiten laten staan, ze gaan wortelen."],
}

TEKSTEN = {
    "vadsrijk.wezens.elite.gloeiend": "Gloeiend",
    "vadsrijk.wezens.set.klodder": "Klodderset",
    "vadsrijk.wezens.set.zwam": "Zwamset",
    "vadsrijk.wezens.set.stukken": "%s (%s/4)",
    "vadsrijk.wezens.set.klodder.2": "(2) Glibberig: 15%% kans dat een klap gewoon van je af glijdt",
    "vadsrijk.wezens.set.klodder.4": "(4) Stuiterkracht: +12%% schade en je klappen maken soms Plakkerig",
    "vadsrijk.wezens.set.zwam.2": "(2) Sporenschild: 10%% minder schade",
    "vadsrijk.wezens.set.zwam.4": "(4) Sporenregen: +12%% spreukschade en je spreuken geven Gesporen",
    "vadsrijk.wezens.set.ontweken": "Glibber! Ontweken.",
    "vadsrijk.wezens.weerkaatst": "Weerkaatst!",
    "vadsrijk.wezens.ontwijkt": "Zwiep!",
    "vadsrijk.wezens.geblokt": "Geblokt!",
    "vadsrijk.wezens.kampklodder.aai": "De Kampklodder wiebelt van geluk. Blub!",
    "vadsrijk.wezens.kampklodder.aai2": "Blub blub! De Kampklodder geeft je een kloddernatte knuffel.",
    "vadsrijk.wezens.kampklodder.aai3": "De Kampklodder maakt een tevreden bubbeltje.",
    "vadsrijk.wezens.cmd.gespawnd": "%s gemaakt (level %s%s).",
    "vadsrijk.wezens.cmd.onbekend": "Dat wezen ken ik niet: %s",
    "vadsrijk.wezens.cmd.draak": "Vahoegnir vliegt over! Kijk omhoog.",
    "vadsrijk.wezens.cmd.set": "Set %s gegeven (level %s).",
    "vadsrijk.wezens.cmd.loot": "Loot van %s (level %s): %s items, %s brons.",
    "vadsrijk.wezens.cmd.spawners": "%s wezens-spawners, %s eigen grot/tempel-spawners.",
    "vadsrijk.wezens.cmd.opgeruimd": "%s wezens opgeruimd.",
    "vadsrijk.wezens.tip.elite.titel": "Een elite!",
    "vadsrijk.wezens.tip.elite": "Gouden gloed = elite. Sterker, groter en met een eigenschap (Gloeiend, Snel, Splitsend of Schildend), maar ook betere loot!",
    "vadsrijk.wezens.tip.zwamwachter.titel": "Zwamwachter",
    "vadsrijk.wezens.tip.zwamwachter": "Van voren blokt hij met zijn hoedschild. Loop om hem heen of sla hem net nadat hij zelf heeft uitgehaald!",
    "vadsrijk.wezens.tip.kristalklodder.titel": "Kristalklodder",
    "vadsrijk.wezens.tip.kristalklodder": "Zijn kristallen weerkaatsen soms spreuken. Gewone klappen werken altijd.",
    "vadsrijk.wezens.tip.glimzwam.titel": "Glimzwam",
    "vadsrijk.wezens.tip.glimzwam": "De Glimzwam heelt de wezens om hem heen. Vrolijk hem als eerste op!",
    "vadsrijk.wezens.tip.mosgeest.titel": "Mosgeest",
    "vadsrijk.wezens.tip.mosgeest": "Een Mosgeest zwiept soms opzij. Blijf erbij, hij kan niet altijd ontwijken.",
    "vadsrijk.wezens.tip.sporenpuffer.titel": "Sporenpuffer",
    "vadsrijk.wezens.tip.sporenpuffer": "Blijf uit de groene wolk: die geeft Gesporen (schade en minder heling).",
    "vadsrijk.wezens.tip.tempelridder.titel": "Tempelzwam-ridder",
    "vadsrijk.wezens.tip.tempelridder": "Zie je de rode strook? Dat is zijn zwaai. Stap opzij!",
}


def teksten():
    t = dict(TEKSTEN)
    for k, n in ENTITEITEN.items():
        t[f"entity.guhs.vr_mob_{k}"] = n
    for k, (slot, naam, kleur, accent, s) in HARNAS.items():
        t[f"item.guhs.vr_mob_{k}"] = naam
        t[f"vadsrijk.loot.{k}"] = naam
        for i, regel in enumerate(FLAVOUR[k]):
            t[f"vadsrijk.loot.{k}.flavour.{i}"] = "\"" + regel + "\""
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


def assets(a, w=_schrijf_json):
    geo_dir = os.path.join(a, "geckolib", "models", "entity", *SUB.split("/"))
    anim_dir = os.path.join(a, "geckolib", "animations", "entity", *SUB.split("/"))
    tex_dir = os.path.join(a, "textures", *SUB.split("/"))
    n = 0

    def schrijf(naam, m, anims, pal, seed, glow=None):
        nonlocal n
        w(os.path.join(geo_dir, naam + ".geo.json"), m.geo())
        w(os.path.join(anim_dir, naam + ".animation.json"), anim_bestand(anims))
        grauw = schilder(m, pal, "blij", seed, True)
        _png(os.path.join(tex_dir, naam + ".png"), grauw)
        _png(os.path.join(tex_dir, naam + "_blij.png"), schilder(m, pal, "blij", seed, False))
        _png(os.path.join(tex_dir, naam + "_elite.png"), elite_gloed(grauw))
        if glow:
            _png(os.path.join(tex_dir, naam + "_gloed.png"), glow)
        n += 1

    for i, naam in enumerate(SLIJM):
        if naam == "kloddertje":
            continue
        m, anims = model_slijm(naam)
        kleur, oor, pat, k2 = SLIJM[naam]
        pal = {"slijm": (kleur, pat, k2), "oor": (oor, None, None), "bel": ((210, 236, 255), "glans", None), "veer": ((200, 200, 214), None, None),
               "strik": ((255, 110, 170), None, None), "bloem": ((255, 250, 240), None, None), "bloemhart": ((255, 214, 90), None, None),
               "druppel": (tuple(int(c * 0.85) for c in kleur), None, None), "kristal": ((236, 220, 255), "facet", (190, 160, 250))}
        schrijf(naam, m, anims, pal, 1100 + i, gloed(m, {"kristal": (110, 80, 160)}, 7) if naam == "kristalklodder" else None)
        if naam == "klodder":   # the kloddertje: the same model, its own (lighter) colours
            pal2 = dict(pal)
            pal2["slijm"] = (SLIJM["kloddertje"][0], "glans", None)
            grauw = schilder(m, pal2, "blij", 1199, True)
            _png(os.path.join(tex_dir, "kloddertje.png"), grauw)
            _png(os.path.join(tex_dir, "kloddertje_blij.png"), schilder(m, pal2, "blij", 1199, False))
            _png(os.path.join(tex_dir, "kloddertje_elite.png"), elite_gloed(grauw))
    for i, naam in enumerate(ZWAM):
        m, anims = model_zwam(naam)
        glow = None
        if naam == "glimzwam":
            glow = gloed(m, {"glimvlek": (120, 230, 250), "hoed": (30, 90, 120)}, 9)
        elif naam == "sporenpriester":
            glow = gloed(m, {"bol": (150, 255, 120)}, 9)
        schrijf(naam, m, anims, ZWAM[naam], 1200 + i, glow)
    m, anims, pal = model_mosgeest()
    schrijf("mosgeest", m, anims, pal, 1300)
    m, anims, pal = model_knaag()
    schrijf("knaag", m, anims, pal, 1301)
    # the dragon: no "blij"/elite needed (it is not a mob), one texture + the glowmask (eyes, crackling sparks)
    m, anims, pal = model_draak()
    w(os.path.join(geo_dir, "draak.geo.json"), m.geo())
    w(os.path.join(anim_dir, "draak.animation.json"), anim_bestand(anims))
    _png(os.path.join(tex_dir, "draak.png"), schilder(m, pal, "draak", 1400, False))
    _png(os.path.join(tex_dir, "draak_gloed.png"), gloed(m, {"zwart": (40, 10, 60)}, 1401, kleur_van=True))
    n += 1
    # armour items
    for i, (k, (slot, naam, kleur, accent, s)) in enumerate(HARNAS.items()):
        item = "vr_mob_" + k
        _png(os.path.join(a, "textures", "item", *SUB.split("/"), k + ".png"), icoon(slot, kleur, accent, s, 1500 + i))
        w(os.path.join(a, "items", item + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + item}})
        w(os.path.join(a, "models", "item", item + ".json"), {"parent": "minecraft:item/generated",
                                                              "textures": {"layer0": f"guhs:item/{SUB}/{k}"}})
    return n


def uiterlijken(a, w=_schrijf_json):
    """Integration 1.10.0: a worn look per armour piece (KernRegistry.harnasEigenschappen(slot, "vr_mob_<id>")):
    assets/guhs/equipment/vr_mob_<id>.json + the two layer textures = vanilla iron tinted with the piece's main colour."""
    import zipfile
    from io import BytesIO
    from PIL import Image
    jar = os.path.join("build", "moddev", "artifacts", "minecraft-patched-26.1.2.112.jar")
    bron = {}
    if os.path.exists(jar):
        with zipfile.ZipFile(jar) as z:
            for laag in ("humanoid", "humanoid_leggings"):
                bron[laag] = Image.open(BytesIO(z.read(f"assets/minecraft/textures/entity/equipment/{laag}/iron.png"))).convert("RGBA")
    for hid, (_slot, _naam, kleur, _k2, _set) in HARNAS.items():
        uid = "vr_mob_" + hid
        w(os.path.join(a, "equipment", uid + ".json"), {"layers": {
            "humanoid": [{"texture": "guhs:" + uid}], "humanoid_leggings": [{"texture": "guhs:" + uid}]}})
        for laag in ("humanoid", "humanoid_leggings"):
            doel = os.path.join(a, "textures", "entity", "equipment", laag, uid + ".png")
            if laag not in bron:
                if not os.path.exists(doel):
                    print("vadsrijk_wezens: no minecraft jar (run a gradle build first) for", doel)
                continue
            img = bron[laag].copy()
            px = img.load()
            for y in range(img.height):
                for x in range(img.width):
                    r, g, b, al = px[x, y]
                    l = min(1.0, (0.3 * r + 0.59 * g + 0.11 * b) / 255 * 1.15)
                    px[x, y] = (int(kleur[0] * l), int(kleur[1] * l), int(kleur[2] * l), al)
            os.makedirs(os.path.dirname(doel), exist_ok=True)
            img.save(doel)


def build(h):
    assets(h.A, h.w)
    uiterlijken(h.A, h.w)
    for k, nl in teksten().items():
        h.lang(k, nl, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    n = assets(a)
    uiterlijken(a)
    t = teksten()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(t)
        _schrijf_json(p, data)
    print(f"vadsrijk_wezens: {n} models, {len(HARNAS)} items, {len(t)} texts")


if __name__ == "__main__":
    _direct()
