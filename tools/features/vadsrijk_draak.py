"""Vahoegnir (redesign 2026-11, approved by the user): the majestic feather dragon with a guh face, plus Vahoegje (his chibi baby).

One generator for every dragon asset (it replaces model_draak() in vadsrijk_wezens.py and draak_texturen()/vahoegnir_anims()
in vadsrijk_z3_bazen_modellen.py; that generator calls alles() here):
  geckolib/models/entity/vadsrijk/wezens/draak.geo.json          one geometry for every form (flyover, passes, zone 2, the boss)
  geckolib/animations/entity/vadsrijk/wezens/draak.animation.json  flyover: vlieg, zweef, brul
  geckolib/animations/entity/vr_z3_baas_vahoegnir.animation.json   boss: idle, walk, vlieg, brul, adem, duik, land, buig
  textures/vadsrijk/wezens/draak.png + draak_gloed.png           grey flyover + its glowing purple eyes
  textures/vadsrijk/z3bazen/vahoegnir_0..5.png (+ _glowmask)     the boss cheering up: 0 grey -> 5 pink, per bone group
  geckolib/models/entity/vadsrijk/wezens/vahoegje.geo.json + vr_huisdier_vahoegje.animation.json + z3bazen/vahoegje.png

Build: the model is built with 4 texture px per model px (the painters are tuned for that) and the PNGs are then halved to
2 px per model px (D_UIT): the UV's are in model units, so the geo does not change. Vahoegje stays at 4 px (small model, eyes).
Design renders + kit: renders-draak/ (outside the repo).

Bones (GeckoLib: front = -z, y up, feet at y=0, left = +x):
  root > body > neck_1 > neck_2 > neck_3 > head > jaw, ear_l, ear_r
         body > wing_l > wing_l_2 > wing_l_3 (and wing_r..)  rest pose = wings spread
         body > tail_1 > .. > tail_5 (+ tail fan);  body > leg_fl/fr/bl/br > foot_..
  Rotation signs: rot x + = front down / back up; rot z - = left wing up; rot y + = a point on +z turns to +x.
The grey-only bits (black fluff, loose feathers) and some face parts are in the shared geometry but transparent in pink.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_draak.py
"""
import json
import math
import os
import zlib

import numpy as np
from PIL import Image

D = 4       # texture px per model px while painting
D_UIT = 2   # in the jar


NAAM = "vr_draak"


# =====================================================================================================================
# model kit (from the mount kit: Model, blob, staaf, painters)
# =====================================================================================================================
def mix(a, b, t):
    return tuple(float(x) * (1 - t) + float(y) * t for x, y in zip(a, b))


def licht(c, f):
    return tuple(min(255.0, v * f) for v in c)


# ======================================================================================================================
# model bouwen
# ======================================================================================================================
class Model:
    def __init__(self, naam, atlas_w=None, d=None):
        self.naam = naam
        self.D = d or D
        self.bones, self.bmap = [], {}
        self.aw = atlas_w or 512 * self.D
        self.x = self.y = self.rij = 0
        self.regions = []
        self.rng = np.random.default_rng(zlib.crc32(naam.encode()))

    def bone(self, naam, pivot, parent=None, rotation=None):
        b = {"name": naam, "pivot": [float(v) for v in pivot]}
        if parent:
            b["parent"] = parent
        if rotation:
            b["rotation"] = [float(v) for v in rotation]
        b["cubes"] = []
        self.bones.append(b)
        self.bmap[naam] = b
        return naam

    def _plek(self, pw, ph):
        pw, ph = pw + pw % 2, ph + ph % 2   # even offsets: halving to D_UIT never mixes two regions
        if self.x + pw > self.aw:
            self.x, self.y, self.rij = 0, self.y + self.rij, 0
        p = (self.x, self.y)
        self.x += pw
        self.rij = max(self.rij, ph)
        return p

    def _vlak(self, pw, ph, kleur, fur, rand):
        a = np.zeros((ph, pw, 4), np.float32)
        a[..., :3] = kleur
        a[..., 3] = 255
        a[..., :3] += self.rng.normal(0, fur, (ph, pw, 1))
        if rand:
            yy, xx = np.mgrid[0:ph, 0:pw]
            d = np.minimum(np.minimum(xx + 0.5, pw - xx - 0.5), np.minimum(yy + 0.5, ph - yy - 0.5)) / (self.D * 1.2)
            a[..., :3] *= (1 - rand * (1 - np.clip(d, 0, 1)))[..., None]
        return a

    def cube(self, bone, origin, size, kleur, verf=None, inflate=0, rotation=None, pivot=None, fur=5, rand=0.10):
        """kleur: rgb, of dict vlak->rgb. verf: functie(arr, vlak) die het vlak beschildert (arr = HxWx4, 0..255),
        of dict vlak->functie(arr). Vlakken: north = voorkant (-z)."""
        sx, sy, sz = size
        dims = {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz),
                "down": (sx, sz)}
        uv = {}
        for face, (w, h) in dims.items():
            pw, ph = max(1, int(round(w * self.D))), max(1, int(round(h * self.D)))
            u, v = self._plek(pw, ph)
            c = kleur.get(face, kleur.get("*")) if isinstance(kleur, dict) else kleur
            a = self._vlak(pw, ph, c, fur, rand)
            if verf:
                f = verf.get(face) if isinstance(verf, dict) else (lambda arr, fc, _v=verf: _v(arr, fc))
                if f:
                    try:
                        f(a, face)
                    except TypeError:
                        f(a)
            self.regions.append((u, v, a))
            uv[face] = {"uv": [u / self.D, v / self.D], "uv_size": [pw / self.D, ph / self.D]}
        c = {"origin": [float(o) for o in origin], "size": [float(s) for s in size], "uv": uv}
        if inflate:
            c["inflate"] = inflate
        if rotation is not None:
            c["rotation"] = [float(r) for r in rotation]
            c["pivot"] = [float(p) for p in (pivot if pivot is not None else origin)]
        self.bmap[bone]["cubes"].append(c)
        return c

    def box(self, bone, center, size, kleur, **kw):
        cx, cy, cz = center
        sx, sy, sz = size
        return self.cube(bone, (cx - sx / 2, cy - sy / 2, cz - sz / 2), size, kleur, **kw)

    def blob(self, bone, center, size, kleur, r=2.0, voor=None, verf=None, fur=5, rand=0.10, hoek=True):
        """Afgeronde vorm: drie dozen met ingesprongen randen (+ een hoekvuller). voor = schilder voor de voorkant
        (het voorste vlak, van de doos met de volle diepte)."""
        sx, sy, sz = size
        cx, cy, cz = center
        sets = [((sx, sy - 2 * r, sz - 2 * r), None), ((sx - 2 * r, sy, sz - 2 * r), None),
                ((sx - 2 * r, sy - 2 * r, sz), voor)]
        if hoek:
            sets.append(((sx - 0.75 * r, sy - 0.75 * r, sz - 0.75 * r), None))
        for (a, b, c), v in sets:
            vv = verf
            if v:
                vv = dict(verf) if isinstance(verf, dict) else ({f: verf for f in ("north", "south", "east", "west", "up", "down")} if verf else {})
                vv["north"] = v
            self.box(bone, (cx, cy, cz), (a, b, c), kleur, verf=vv, fur=fur, rand=rand)

    def staaf(self, bone, p0, p1, w, kleur, verf=None, fur=4, rand=0.08, d=None):
        """Balkje van p0 naar p1 (GeckoLib-coordinaten), dikte w (of (w, d))."""
        p0, p1 = np.array(p0, float), np.array(p1, float)
        v = p1 - p0
        L = float(np.linalg.norm(v))
        dx, dy, dz = v / L
        dd = d if d is not None else w
        rz = math.degrees(math.asin(max(-1, min(1, -dx))))
        cz = math.cos(math.radians(rz))
        a = math.degrees(math.atan2(dz / max(cz, 1e-6), dy / max(cz, 1e-6)))
        return self.cube(bone, (p0[0] - w / 2, p0[1], p0[2] - dd / 2), (w, L, dd), kleur, verf=verf,
                         rotation=(-a, 0, rz), pivot=p0, fur=fur, rand=rand)

    def textuur(self):
        H = self.y + self.rij
        H = int(math.ceil(H / (4 * self.D)) * 4 * self.D)
        t = np.zeros((H, self.aw, 4), np.float32)
        for u, v, a in self.regions:
            t[v:v + a.shape[0], u:u + a.shape[1]] = a
        return t

    def geo(self):
        t = self.textuur()
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": f"geometry.{self.naam}", "texture_width": self.aw // self.D,
                            "texture_height": t.shape[0] // self.D,
                            "visible_bounds_width": 5, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]},
            "bones": self.bones}]}


# ======================================================================================================================
# schilderhulpjes (arr = HxWx4 float 0..255, pixelcoordinaten)
# ======================================================================================================================
def ellips(arr, cx, cy, rx, ry, kleur, alpha=1.0):
    h, w = arr.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    m = ((xx + 0.5 - cx) / rx) ** 2 + ((yy + 0.5 - cy) / ry) ** 2 <= 1
    arr[m, :3] = arr[m, :3] * (1 - alpha) + np.array(kleur, np.float32) * alpha
    return m


def zacht(arr, cx, cy, rx, ry, kleur, alpha=0.6):
    h, w = arr.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    d = ((xx + 0.5 - cx) / rx) ** 2 + ((yy + 0.5 - cy) / ry) ** 2
    t = np.clip(1 - d, 0, 1)[..., None] * alpha
    arr[..., :3] = arr[..., :3] * (1 - t) + np.array(kleur, np.float32) * t


def guh_oog(arr, cx, cy, r, iris=(80, 160, 235)):
    """Een LPS-guh-oog: donkere rand, blauwe iris, zwarte pupil, twee glimmers, wimpertje."""
    ellips(arr, cx, cy, r * 1.0, r * 1.12, (22, 14, 30))
    ellips(arr, cx, cy + r * 0.12, r * 0.80, r * 0.88, iris)
    ellips(arr, cx, cy + r * 0.25, r * 0.80, r * 0.62, licht(iris, 0.75), 0.6)
    ellips(arr, cx, cy - r * 0.05, r * 0.62, r * 0.70, (18, 12, 26))
    ellips(arr, cx - r * 0.30, cy - r * 0.38, r * 0.26, r * 0.26, (255, 255, 255))
    ellips(arr, cx + r * 0.32, cy + r * 0.28, r * 0.13, r * 0.13, (255, 255, 255))


def guh_gezicht(arr, oog_y=0.40, afstand=0.25, r=None, blos=(255, 130, 175), mond=True, iris=(80, 160, 235)):
    h, w = arr.shape[:2]
    r = r or max(3, w * 0.12)
    cy = h * oog_y
    for s in (-1, 1):
        cx = w / 2 + s * w * afstand
        zacht(arr, cx + s * r * 0.9, cy + r * 1.7, r * 1.0, r * 0.6, blos, 0.75)
        guh_oog(arr, cx, cy, r, iris)
        # wimper
        x0 = int(cx + s * r * 0.85)
        y0 = int(cy - r * 0.9)
        for k in range(max(2, int(r * 0.45))):
            xx = x0 + s * k
            if 0 <= xx < w and 0 <= y0 - k < h:
                arr[max(0, y0 - k):y0 - k + 2, xx, :3] = (22, 14, 30)
    if mond:
        my = int(cy + r * 1.9)
        for dx in range(-int(r * 0.6), int(r * 0.6) + 1):
            yy = my + int(round(abs(math.sin(dx / max(1, r * 0.6) * math.pi)) * 1.2))
            xx = int(w / 2 + dx)
            if 0 <= xx < w and 0 <= yy < h:
                arr[yy, xx, :3] = arr[yy, xx, :3] * 0.55


def schubben(kleur_f=0.86, stap=8, ofs=0):
    """Veren-patroon: rijtjes zachte boogjes (donkerder)."""
    def f(arr, face=None):
        h, w = arr.shape[:2]
        for ri, y0 in enumerate(range(ofs, h + stap, stap)):
            off = (stap // 2) * (ri % 2)
            for x in range(w):
                t = ((x + off) % stap) / stap
                yy = y0 + int(round(stap * 0.45 * (1 - (2 * t - 1) ** 2)))
                for k in (0, 1):
                    if 0 <= yy - k < h:
                        arr[yy - k, x, :3] *= kleur_f if k == 0 else (1 + kleur_f) / 2
    return f


def sterretjes(n=12, kleur=(255, 245, 200), seed=1):
    def f(arr, face=None):
        h, w = arr.shape[:2]
        rng = np.random.default_rng(seed + h * 31 + w)
        for _ in range(max(1, int(n * h * w / 1600))):
            x, y = int(rng.integers(1, max(2, w - 1))), int(rng.integers(1, max(2, h - 1)))
            arr[y, x, :3] = kleur
            if rng.random() < 0.4:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if 0 <= x + dx < w and 0 <= y + dy < h:
                        arr[y + dy, x + dx, :3] = mix(arr[y + dy, x + dx, :3], kleur, 0.7)
    return f


def combi(*fs):
    fs = [f for f in fs if f]

    def f(arr, face=None):
        for g in fs:
            g(arr, face)
    return f



# =====================================================================================================================
# Vahoegnir
# =====================================================================================================================
THEMAS = {
    "grijs": dict(basis=(128, 120, 142), licht=(158, 150, 170), diep=(92, 84, 108), buik=(166, 158, 176),
                  manen=(74, 64, 92), manen_tip=(26, 20, 34), veer=(122, 114, 138), veer_tip=(70, 62, 86),
                  oor_binnen=(150, 126, 150), neus=(84, 74, 96), klauw=(70, 64, 78), schub=(104, 98, 112),
                  ruig=0.55, fur=9, glans=False),
    "roze": dict(basis=(255, 150, 202), licht=(255, 204, 228), diep=(236, 104, 172), buik=(255, 220, 236),
                 manen=(238, 92, 168), manen_tip=(255, 240, 250), veer=(255, 164, 212), veer_tip=(255, 228, 242),
                 oor_binnen=(255, 196, 222), neus=(230, 90, 150), klauw=(255, 236, 244), schub=(255, 182, 210),
                 ruig=0.0, fur=4, glans=True),
}
THEMAS["gloed"] = dict(THEMAS["grijs"])

PAARS_GLOED = (196, 110, 255)


KOP = 1.22


class Schaal:
    """Bouwt een stuk model geschaald rond punt c (zelfde API als Model voor bone/blob/box/cube/staaf)."""

    def __init__(self, m, c, k):
        self.m, self.c, self.k = m, np.array(c, float), k

    def P(self, p):
        return tuple(float(v) for v in self.c + (np.array(p, float) - self.c) * self.k)

    def S(self, s):
        return tuple(float(v) * self.k for v in s)

    def bone(self, naam, pivot, parent=None, rotation=None):
        return self.m.bone(naam, list(self.P(pivot)), parent, rotation)

    def blob(self, bone, center, size, kleur, r=2.0, **kw):
        return self.m.blob(bone, self.P(center), self.S(size), kleur, r=r * self.k, **kw)

    def box(self, bone, center, size, kleur, **kw):
        if kw.get("pivot") is not None:
            kw["pivot"] = self.P(kw["pivot"])
        return self.m.box(bone, self.P(center), self.S(size), kleur, **kw)

    def cube(self, bone, origin, size, kleur, **kw):
        if kw.get("pivot") is not None:
            kw["pivot"] = self.P(kw["pivot"])
        return self.m.cube(bone, self.P(origin), self.S(size), kleur, **kw)

    def staaf(self, bone, p0, p1, w, kleur, d=None, **kw):
        return self.m.staaf(bone, self.P(p0), self.P(p1), w * self.k, kleur, d=(d if d is None else d * self.k), **kw)


class GloedModel(Model):
    """Zelfde indeling, maar elk vlak begint doorzichtig: alleen wat de oog-schilder expliciet aanzet blijft over."""

    def _vlak(self, pw, ph, kleur, fur, rand):
        a = np.zeros((ph, pw, 4), np.float32)
        return a


# ======================================================================================================================
# schilders
# ======================================================================================================================
def _rng(arr, extra=0):
    h, w = arr.shape[:2]
    return np.random.default_rng(h * 7919 + w * 104729 + extra)


def glans(arr, face=None):
    """Glimmende veren: lichter naar boven + een paar schuine glansstrepen (alleen op zij-/bovenvlakken)."""
    h, w = arr.shape[:2]
    if face == "down":
        return
    t = np.linspace(0.18, 0.0, h)[:, None, None] if face not in ("up",) else 0.12
    arr[..., :3] = arr[..., :3] * (1 - t) + 255 * t
    yy, xx = np.mgrid[0:h, 0:w]
    for x0 in range(-h, w, 46):
        band = np.abs((xx - yy * 0.7) - x0) < 2.2
        arr[band, :3] = arr[band, :3] * 0.78 + np.array((255, 246, 252)) * 0.22


def pluisjes(P, n=2.2):
    """Grijs: zwarte pluisvlokjes en rafelige lichte puntjes (Vadsloosheid)."""
    def f(arr, face=None):
        if P["ruig"] <= 0:
            return
        h, w = arr.shape[:2]
        r = _rng(arr, 3)
        for _ in range(max(1, int(n * h * w / 6000))):
            cx, cy = r.uniform(0, w), r.uniform(0, h)
            rr = r.uniform(1.2, 2.6)
            for _k in range(4):
                ellips(arr, cx + r.uniform(-rr, rr), cy + r.uniform(-rr, rr), rr * 0.7, rr * 0.6, (24, 18, 30), 0.85)
        m = r.random((h, w)) < 0.012
        arr[m, :3] = arr[m, :3] * 0.6 + np.array((200, 194, 206)) * 0.4
    return f


def veren_lijf(P):
    return combi(schubben(0.86, 9), glans if P["glans"] else None, pluisjes(P))


def _veer_vorm(arr, face, P, kleur, tip, ruig, seed, as_lengte="v"):
    """Platte veer (dun doosje). Basis aan de voorkant (-z), punt achteraan.
    Vlakken: up = rijen 0..h van voor naar achter, down = omgekeerd, west/east = kolommen."""
    h, w = arr.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    if face in ("up", "down"):
        t = (yy + 0.5) / h
        if face == "down":
            t = 1 - t
        s = (xx + 0.5) / w * 2 - 1
        breed = True
    elif face in ("west", "east"):
        t = (xx + 0.5) / w
        if face == "east":
            t = 1 - t
        s = np.zeros_like(t)
        breed = False
    else:
        return
    # vorm: recht tot 70%, dan rond toelopende punt
    hw = np.where(t < 0.70, 1.0, np.sqrt(np.clip(1 - ((t - 0.70) / 0.30) ** 2, 0, 1)))
    r = np.random.default_rng(seed)
    if breed:
        uit = np.abs(s) > hw
        if ruig > 0:  # rafels: schuine kerfjes van de rand naar binnen
            for _ in range(int(3 + ruig * 8)):
                t0 = r.uniform(0.25, 0.95)
                kant = r.choice([-1, 1])
                diep = r.uniform(0.25, 0.65) * ruig + 0.15
                k = (np.abs(t - t0 - (1 - np.abs(s)) * 0.05) < 0.012 + 0.01 * ruig) & (s * kant > 1 - diep)
                uit |= k
        arr[uit, 3] = 0
        # kleur: basis -> tip, schacht licht, randje donker, baardjes
        g = np.clip((t - 0.35) / 0.65, 0, 1)[..., None]
        arr[..., :3] = arr[..., :3] * (1 - g) + np.array(tip, np.float32) * g
        schacht = np.abs(s) < 0.09
        arr[schacht, :3] = arr[schacht, :3] * 0.55 + np.array(licht(kleur, 1.25)) * 0.45
        rand = (np.abs(s) > hw - 0.16) & ~uit
        arr[rand, :3] *= 0.80
        baard = ((t * h * 0.9 + np.abs(s) * w * 0.6) % 5) < 1.0
        arr[baard & ~schacht, :3] *= 0.93
        if P["glans"] and face == "up":
            gl = (s > 0.15) & (s < 0.45) & (t > 0.1) & (t < 0.8)
            arr[gl, :3] = arr[gl, :3] * 0.6 + np.array((255, 250, 253)) * 0.4
        if ruig > 0:
            m = r.random((h, w)) < 0.02 * ruig
            arr[m, :3] *= 0.45
    else:
        arr[t > 0.86, 3] = 0
        g = np.clip((t - 0.35) / 0.65, 0, 1)[..., None]
        arr[..., :3] = arr[..., :3] * (1 - g) + np.array(tip, np.float32) * g


def veer_verf(P, kleur, tip, seed, ruig=None):
    ruig = P["ruig"] if ruig is None else ruig

    def f(arr, face=None):
        _veer_vorm(arr, face, P, kleur, tip, ruig, seed)
    return f


def pluim_verf(P, seed):
    """Manen-pluim (staaf: lengte langs de lokale y, rij 0 = punt). Toelopend, punt in manen_tip-kleur."""
    def f(arr, face=None):
        h, w = arr.shape[:2]
        if face in ("up", "down"):
            return
        yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
        t = 1 - (yy + 0.5) / h  # 1 = punt
        s = (xx + 0.5) / w * 2 - 1
        if face in ("east", "west"):
            hw = np.where(t < 0.45, 1.0, np.clip(1 - np.clip((t - 0.45) / 0.55, 0, 1) ** 1.6, 0, 1))
            uit = np.abs(s) > hw
            r = np.random.default_rng(seed)
            if P["ruig"] > 0:
                for _ in range(4):
                    t0 = r.uniform(0.3, 0.9)
                    uit |= (np.abs(t - t0) < 0.03) & (np.abs(s) > 0.35)
            arr[uit, 3] = 0
            schacht = np.abs(s) < 0.1
            arr[schacht, :3] = arr[schacht, :3] * 0.6 + np.array(licht(P["manen"], 1.3)) * 0.4
        else:
            arr[t > 0.75, 3] = 0
        g = np.clip((t - 0.45) / 0.5, 0, 1)[..., None]
        arr[..., :3] = arr[..., :3] * (1 - g) + np.array(P["manen_tip"], np.float32) * g
        if P["glans"]:
            glans(arr, "west")
    return f


def alleen_grijs(P, verf):
    """Pluisjes/losse veren: in de roze vorm onzichtbaar."""
    def f(arr, face=None):
        if P["ruig"] <= 0:
            arr[..., 3] = 0
            return
        verf(arr, face)
    return f


def zwart_pluis(arr, face=None):
    h, w = arr.shape[:2]
    r = _rng(arr, 11)
    arr[..., :3] = np.array((30, 24, 38)) + r.normal(0, 8, (h, w, 1))
    yy, xx = np.mgrid[0:h, 0:w]
    rand = np.minimum(np.minimum(xx, w - 1 - xx), np.minimum(yy, h - 1 - yy))
    arr[(rand < 2) & (r.random((h, w)) < 0.55), 3] = 0
    m = r.random((h, w)) < 0.05
    arr[m, :3] = (70, 60, 86)


# ---------------------------------------------------------------------------------------------------------------------
# ogen
# ---------------------------------------------------------------------------------------------------------------------
def oog_verf(thema, s):
    """Groot oog op het voorvlak van het oogplaatje. s = +1 linkeroog (+x), -1 rechteroog.
    Kolom 0 = kleinste GeckoLib-x, dus de buitenkant van het linkeroog is rechts in het plaatje."""
    P = THEMAS[thema]

    def f(arr, face=None):
        h, w = arr.shape[:2]
        cx, cy = w / 2, h * 0.54
        rx, ry = w * 0.46, h * 0.44
        buiten = 1 if s > 0 else -1  # richting buitenkant in kolommen
        if thema == "roze":
            # guh-pluche-oog (zoals guh.png): groot, rond, donker en glanzend, blauwe onderrand, twee witte glimmers
            r = min(w, h) * 0.43
            cy = h * 0.55
            zacht(arr, cx, cy, r * 1.25, r * 1.25, licht(P["basis"], 1.08), 0.5)
            ellips(arr, cx, cy, r * 1.04, r * 1.06, (20, 12, 28))                         # rand
            ellips(arr, cx, cy + r * 0.10, r * 0.92, r * 0.90, (60, 150, 235))           # blauwe onderrand
            ellips(arr, cx, cy + r * 0.20, r * 0.86, r * 0.70, (110, 196, 250), 0.7)
            ellips(arr, cx, cy - r * 0.06, r * 0.88, r * 0.86, (16, 10, 24))              # donker glanzend oog
            zacht(arr, cx, cy + r * 0.25, r * 0.60, r * 0.40, (60, 40, 90), 0.35)
            ellips(arr, cx - r * 0.34, cy - r * 0.36, r * 0.30, r * 0.30, (255, 255, 255))  # grote glimmer
            ellips(arr, cx + r * 0.36, cy + r * 0.26, r * 0.14, r * 0.14, (255, 255, 255))  # kleine glimmer
            # dun wimper-boogje bovenop + krulletje naar buiten
            yy, xx = np.mgrid[0:h, 0:w]
            d = np.sqrt(((xx + 0.5 - cx) / (r * 1.10)) ** 2 + ((yy + 0.5 - cy) / (r * 1.12)) ** 2)
            boog = (np.abs(d - 1) < 0.07) & (yy < cy - r * 0.25)
            arr[boog, :3] = (20, 12, 28)
            for j in range(4):
                xx_ = int(cx + buiten * (r * 0.95 + j))
                yy_ = int(cy - r * 0.62 - j)
                if 0 <= xx_ < w and 0 <= yy_ < h:
                    arr[yy_:yy_ + 2, xx_, :3] = (20, 12, 28)
        else:
            gloed = thema == "gloed"
            yy, xx = np.mgrid[0:h, 0:w]
            if not gloed:
                ellips(arr, cx, cy, rx, ry, (34, 26, 46))
            d = np.sqrt(((xx + 0.5 - cx) / (rx * 0.80)) ** 2 + ((yy + 0.5 - cy) / (ry * 0.82)) ** 2)
            irisk = d <= 1
            t = np.clip(d, 0, 1)[..., None]
            kl = np.array((236, 196, 255), np.float32) * (1 - t) + np.array((150, 70, 230), np.float32) * t
            arr[irisk, :3] = kl[irisk]
            pup = (((xx + 0.5 - cx) / (rx * 0.13)) ** 2 + ((yy + 0.5 - cy) / (ry * 0.62)) ** 2) <= 1  # spleetpupil
            arr[pup, :3] = (30, 8, 46)
            # zwaar, vermoeid ooglid: bovenste deel schuin dicht (binnenkant lager = treurig)
            lijn = cy - ry * 0.30 + (xx - cx) * (-buiten) * 0.18
            lid = yy < lijn
            if gloed:
                arr[irisk & ~pup & ~lid, 3] = 255
                arr[~(irisk & ~pup & ~lid), 3] = 0
                return
            arr[lid, :3] = np.array(P["basis"]) * 0.82
            rand = np.abs(yy - lijn) < 1.3
            arr[rand & (((xx + 0.5 - cx) / rx) ** 2 + ((yy + 0.5 - cy) / ry) ** 2 <= 1.15), :3] = (30, 22, 40)
            ellips(arr, cx - buiten * rx * 0.28, cy + ry * 0.05, rx * 0.10, ry * 0.08, (240, 220, 255), 0.7)
    return f


# ======================================================================================================================
# model
# ======================================================================================================================
def bouw(thema="grijs"):
    P = THEMAS[thema]
    m = (GloedModel if thema == "gloed" else Model)(NAAM)
    B, L, Dp, BU = P["basis"], P["licht"], P["diep"], P["buik"]
    fur = P["fur"]
    vl = veren_lijf(P)
    zacht_v = combi(schubben(0.92, 7), glans if P["glans"] else None)
    rng = np.random.default_rng(1100)
    pluis_n = [0]

    def pluisje(bone, c, sz=(2, 2, 2), mm=None):
        pluis_n[0] += 1
        (mm or m).box(bone, c, sz, (30, 24, 38), verf=alleen_grijs(P, zwart_pluis), fur=0, rand=0)

    def pluim(bone, p0, lengte, hoek=48, breed=4.0, dik=0.9, jitter=0.0, mm=None):
        a = math.radians(hoek)
        p1 = (p0[0] + jitter, p0[1] + lengte * math.sin(a), p0[2] + lengte * math.cos(a))
        (mm or m).staaf(bone, p0, p1, dik, P["manen"], verf=pluim_verf(P, int(abs(p0[2]) * 10 + lengte)), d=breed,
                fur=3, rand=0.04)

    # ---------------------------------------------------------------- lijf
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 17, 0], "root")
    m.blob("body", (0, 18, -6), (20, 18, 21), B, r=4.5, verf=vl, fur=fur)               # borst
    m.blob("body", (0, 17, 9), (17, 15.5, 17), B, r=4, verf=vl, fur=fur)               # heup
    m.blob("body", (0, 14.5, -15.5), (14, 11, 6), BU, r=2.2, verf=zacht_v, fur=fur)      # borstpluis
    m.blob("body", (0, 9.6, 1), (13, 2.4, 24), BU, r=1.0, verf=zacht_v, fur=fur)         # buik
    for i, z in enumerate((-12, -6.5, -1, 4.5, 10, 15)):                                  # rugmanen
        top = 27.2 if z < 2 else 24.9
        pluim("body", (0, top - 1.2, z), 9.5 - i * 0.7, hoek=50 + i * 2, breed=4.6 - i * 0.25,
              jitter=(rng.uniform(-0.6, 0.6)))
    for c in ((9.8, 15, -4), (-9.6, 20, 2), (7.2, 10.5, 10), (-6, 22.5, 12), (3, 27.6, -2)):
        pluisje("body", c, (2.2, 2.4, 2.6))
    # losse rafelveren (alleen grijs)
    for (x, y, z, ry, rz) in ((10, 19, 0, 20, -30), (-10.4, 16, 6, -15, 28), (8.5, 22, 12, 30, -45)):
        m.cube("body", (x - 1, y, z), (2, 0.5, 6), P["veer"], verf=alleen_grijs(P, veer_verf(P, P["veer"], P["veer_tip"], 7, 1.0)),
               rotation=(10, ry, rz), pivot=(x, y, z), fur=3, rand=0)

    # ---------------------------------------------------------------- nek (S-boog)
    nek = [((0, 23.5, -13.5), (0, 28.5, -22.5), 11.5), ((0, 28.5, -22.5), (0, 35.5, -26.5), 10.2),
           ((0, 35.5, -26.5), (0, 40.5, -31.0), 9.4)]
    prev = "body"
    for i, (p0, p1, w) in enumerate(nek):
        bn = f"neck_{i + 1}"
        m.bone(bn, list(p0), prev)
        m.staaf(bn, p0, p1, w, B, verf=vl, d=w, fur=fur)
        m.blob(bn, p0, (w + 0.6, w + 0.6, w + 0.6), B, r=w * 0.28, verf=vl, fur=fur)  # gewricht-vuller
        # lichte keel/voorkant
        v = np.subtract(p1, p0)
        n = np.array((0, -v[2], v[1])) / np.linalg.norm(v)  # loodrecht naar voren-onder
        q0 = np.add(p0, n * w * 0.18)
        q1 = np.add(p1, n * w * 0.18)
        m.staaf(bn, tuple(q0 - (0, 0, 0)), tuple(q1), w * 0.72, BU, verf=zacht_v, d=w * 0.72, fur=fur)
        for k in range(2):
            pp = np.add(p0, np.multiply(v, 0.25 + 0.5 * k)) + np.array((0, w * 0.40, w * 0.18))
            pluim(bn, tuple(pp), 8.5 - i * 0.6, hoek=40, breed=4.2, jitter=rng.uniform(-0.6, 0.6))
        if i == 1:
            pluisje(bn, tuple(np.add(p0, (4.6, 2, -2))), (2, 2.2, 2.2))
        prev = bn
    m.blob("neck_3", (0, 40.5, -31.0), (10, 10, 10), B, r=2.8, verf=vl, fur=fur)

    # ---------------------------------------------------------------- kop
    h = Schaal(m, (0, 40.5, -31.0), KOP)  # kop + oren 22% groter (grote ogen, majestueus)
    h.bone("head", [0, 40.5, -31.0], "neck_3")

    def voorhoofd(arr, face=None):
        h, w = arr.shape[:2]
        zacht(arr, w / 2, h * 0.30, w * 0.18, h * 0.16, L, 0.5)
        if thema == "roze":
            bol(arr, face)

    def bol(arr, face=None):
        """Roze: zacht, rond pluche-gezicht: licht midden, zachte randen, geen schubben."""
        h, w = arr.shape[:2]
        zacht(arr, w / 2, h * 0.45, w * 0.62, h * 0.62, licht(B, 1.10), 0.55)
        yy, xx = np.mgrid[0:h, 0:w]
        d = np.minimum(np.minimum(xx + 0.5, w - xx - 0.5), np.minimum(yy + 0.5, h - yy - 0.5)) / max(4.0, min(h, w) * 0.22)
        arr[..., :3] *= (0.90 + 0.10 * np.clip(d, 0, 1))[..., None]
    kopverf = bol if thema == "roze" else combi(schubben(0.9, 7), glans if P["glans"] else None)
    h.blob("head", (0, 43, -36.5), (14, 13, 13), B, r=3.0, verf=kopverf, voor=voorhoofd, fur=fur)
    # wangen (roze: blosjes)
    def wang(s):
        def f(arr, face=None):
            h, w = arr.shape[:2]
            if thema == "roze" and face in ("east", "west", "north"):
                bol(arr, face)
                zacht(arr, w / 2, h * 0.5, w * 0.46, h * 0.40, (255, 92, 146), 0.85)
                for k in range(0):
                    x = int(w * (0.32 + 0.16 * k))
                    for j in range(3):
                        yy_ = int(h * 0.38) + j
                        xx_ = x + j
                        if 0 <= xx_ < w and 0 <= yy_ < h:
                            arr[yy_, xx_, :3] = (255, 236, 244)
        return f
    for s in (1, -1):
        h.blob("head", (s * 6.0, 40.0, -39.0), (3.6, 5.2, 6.0), B, r=1.2, verf=wang(s), fur=fur)

    # snuit: rond guh-snuitje met neusgaatjes en een lachje
    def snuit_voor(arr, face=None):
        h, w = arr.shape[:2]
        if thema == "roze":
            bol(arr, face)
            ellips(arr, w / 2, h * 0.30, w * 0.17, h * 0.12, (236, 104, 160))          # rond neusje
            ellips(arr, w / 2 - w * 0.05, h * 0.26, w * 0.05, h * 0.04, (255, 214, 232))
            my = h * 0.62                                                              # klein w-lachje
            for x in range(int(w * 0.30), int(w * 0.70) + 1):
                u = (x - w * 0.30) / (w * 0.40)
                y = int(round(my + abs(math.sin(u * 2 * math.pi)) * h * 0.07))
                arr[y:y + 2, x, :3] = (150, 60, 100)
            return
        zacht(arr, w / 2, h * 0.30, w * 0.26, h * 0.18, P["neus"], 0.45)
        for s in (-1, 1):
            ellips(arr, w / 2 + s * w * 0.20, h * 0.36, w * 0.075, h * 0.08, licht(P["neus"], 0.45))
        if P["glans"]:
            ellips(arr, w / 2 - w * 0.12, h * 0.20, w * 0.07, h * 0.05, (255, 240, 248), 0.8)

    def snuit_zij(arr, face=None):
        h, w = arr.shape[:2]
        # mondlijn langs de onderkant: roze lacht (krult omhoog achteraan), grijs hangt omlaag
        achter = 1 if face == "west" else 0  # west: kolom w = achter (z groter); east: kolom 0 = achter
        if thema == "roze":  # geen lange draken-mondlijn, wel blosjes
            bol(arr, face)
            zacht(arr, w * (0.72 if achter else 0.28), h * 0.42, w * 0.24, h * 0.30, (255, 96, 150), 0.7)
            return
        for x in range(w):
            t = x / max(1, w - 1)
            ta = t if achter else 1 - t  # 0 = voor, 1 = achter
            if ta > 0.92:
                continue
            if thema == "roze":
                y = h - 3 - int(round(max(0, ta - 0.55) * h * 0.9))
            else:
                y = h - 3 + int(round(max(0, ta - 0.6) * h * 0.25))
            y = min(h - 1, y)
            arr[y:y + 2, x, :3] = arr[y:y + 2, x, :3] * 0.45
        if thema == "roze":
            zacht(arr, w * (0.78 if achter else 0.22), h * 0.40, w * 0.20, h * 0.26, (255, 100, 156), 0.55)
        if P["glans"]:
            glans(arr, face)
    h.blob("head", (0, 40.0, -46.5), (10.5, 7.2, 9.5), B, r=2.6,
           verf={"north": snuit_voor, "east": snuit_zij, "west": snuit_zij, "up": bol if thema == "roze" else zacht_v,
                 "down": None, "south": None},
           voor=snuit_voor, fur=fur)
    h.box("head", (0, 43.2, -46.5), (5.0, 1.0, 7.0), L, verf=bol if thema == "roze" else zacht_v, fur=fur, rand=0.06)  # neusrug
    for s in (-1, 1):                                                                                # tandjes
        h.box("head", (s * 2.6, 36.0, -50.2), (1.2, 1.4, 1.2), (250, 246, 240), fur=2, rand=0,
              verf=(lambda a, f=None: a.__setitem__((Ellipsis, 3), 0)) if thema == "roze" else None)  # roze: geen tandjes
    h.bone("jaw", [0, 37.5, -41.0], "head")
    h.blob("jaw", (0, 35.6, -45.6), (8.6, 2.8, 8.6), L, r=1.0, verf=zacht_v, fur=fur)

    # ogen: grote bolle oogplaatjes op de voorhoeken, schuin naar buiten
    for s in (1, -1):
        c = (s * 5.7, 46.0, -41.5)
        h.cube("head", (c[0] - 3.2, c[1] - 3.5, c[2] - 0.7), (6.4, 7.0, 1.4), B if thema == "roze" else Dp,
               verf={"north": oog_verf(thema, s)}, rotation=(0, -36 * s, 0), pivot=c, fur=2, rand=0.0)
        # wenkbrauw-richel
        h.cube("head", (c[0] - 3.6, c[1] + 3.6, c[2] - 0.4), (7.2, 1.6, 2.2), Dp,
               verf=(lambda a, f=None: a.__setitem__((Ellipsis, 3), 0)) if thema == "roze" else zacht_v,
               rotation=(0, -36 * s, -10 * s), pivot=c, fur=fur, rand=0.06)
    # kuif op de kop
    for i, z in enumerate((-40.5, -37.0, -33.5)):
        pluim("head", (0, 48.8, z), (7.0, 8.5, 7.5)[i], hoek=58 - i * 6, breed=4.0, mm=h)
    pluisje("head", (4.5, 47.5, -32.0), (1.8, 2.0, 2.0), mm=h)

    # oren als horens: lang, naar achter geveegd, twee stukken
    def oor_binnen(arr, face=None):
        h, w = arr.shape[:2]
        ellips(arr, w / 2, h * 0.55, w * 0.30, h * 0.45, P["oor_binnen"])
        if P["glans"]:
            glans(arr, face)

    def oor_schijf(arr, face=None):
        """Roze: rond guh-schijfoortje (staaf: rij 0 = bovenkant), zachte roze binnenkant."""
        h, w = arr.shape[:2]
        if face == "up":
            arr[..., 3] = 0
            return
        yy, xx = np.mgrid[0:h, 0:w]
        if face in ("north", "south"):
            d = ((xx + 0.5 - w / 2) / (w / 2)) ** 2 + ((yy + 0.5 - h * 0.58) / (h * 0.58)) ** 2
            arr[d > 1, 3] = 0
            bol(arr, face)
            if face == "north":
                zacht(arr, w / 2, h * 0.58, w * 0.40, h * 0.42, (255, 150, 196), 0.95)
                ellips(arr, w / 2, h * 0.58, w * 0.30, h * 0.32, (255, 168, 206), 0.8)
        else:
            arr[yy < h * 0.16, 3] = 0

    def oor_tip(arr, face=None):
        h, w = arr.shape[:2]
        k = int(h * 0.45)
        arr[:k, :, :3] = arr[:k, :, :3] * 0.3 + np.array(P["manen_tip"] if thema == "roze" else (34, 28, 44)) * 0.7
        if face == "north":
            ellips(arr, w / 2, h * 0.75, w * 0.26, h * 0.30, P["oor_binnen"])
    for s, n in ((1, "l"), (-1, "r")):
        bn = f"ear_{n}"
        b0 = (s * 4.8, 48.0, -37.0)
        h.bone(bn, list(b0), "head")
        mid = (s * 7.8, 59.0, -31.5)    # groter (goedkeuring 2026-11-05): ronde oren boven de kuif uit
        mid2 = (s * 9.2, 63.5, -27.0)
        tip = (s * 10.2, 67.0, -22.5)
        def terug(a, b, k=1.4):  # segment iets laten overlappen met het vorige (geen naad)
            v = np.subtract(b, a)
            return tuple(np.subtract(a, v / np.linalg.norm(v) * k))
        if thema == "roze":  # rond guh-schijfoortje; de bovenste stukken zijn in roze doorzichtig
            weg = (lambda a, f=None: a.__setitem__((Ellipsis, 3), 0))
            schijf = {fc: oor_schijf for fc in ("north", "south", "east", "west", "up")}
            h.staaf(bn, b0, mid, 9.0, B, verf=schijf, d=2.6, fur=fur)
            h.staaf(bn, terug(mid, mid2), mid2, 6.2, B, verf=weg, d=2.2, fur=fur)
            h.staaf(bn, terug(mid2, tip), tip, 3.0, B, verf=weg, d=1.6, fur=fur)
        else:
            h.staaf(bn, b0, mid, 9.0, B, verf={"north": oor_binnen, "south": vl, "east": vl, "west": vl}, d=2.6, fur=fur)
            h.staaf(bn, terug(mid, mid2), mid2, 6.2, B, verf={"north": oor_binnen, "south": vl, "east": vl, "west": vl}, d=2.2, fur=fur)
            h.staaf(bn, terug(mid2, tip), tip, 3.0, B, verf={"north": oor_tip, "south": oor_tip, "east": oor_tip, "west": oor_tip}, d=1.6, fur=fur)
        h.blob(bn, b0, (5.6, 4.2, 4.2), B, r=1.2, verf=vl, fur=fur)

    # ---------------------------------------------------------------- vleugels (rustpose: gespreid)
    segs = [  # (x0, x1, barbreedte, barhoogte, dekveren A, dekveren B, slagpennen)
        (8.5, 27.0, 5.0, 4.0, 8.0, 13.0, 21.0),
        (27.0, 45.0, 4.2, 3.4, 8.5, 14.5, 24.0),
    ]
    Y, Z0 = 25.5, -9.0
    for s, n in ((1, "l"), (-1, "r")):
        parent = "body"
        for k, (x0, x1, bw, bh, la, lb, lc) in enumerate(segs):
            bn = f"wing_{n}" if k == 0 else f"wing_{n}_{k + 1}"
            m.bone(bn, [s * x0, Y, Z0], parent)
            m.blob(bn, (s * (x0 + x1) / 2, Y + 0.3, Z0 + bw / 2 - 1.0), (x1 - x0 + 2, bh, bw), B, r=1.2, verf=vl, fur=fur)
            stap = 3.3
            xs = np.arange(x0 + 1.6, x1 - 0.5, stap)
            for j, x in enumerate(xs):
                jit = rng.uniform(-3, 3)
                droop = rng.uniform(0, 4)
                # slagpennen (lang, onderste laag)
                m.cube(bn, (s * x - 2.5, Y - 0.3, Z0 + 0.5), (5.0, 0.6, lc + (j % 2) * 1.5), P["veer"],
                       verf=veer_verf(P, P["veer"], P["veer_tip"], 100 + k * 50 + j),
                       rotation=(droop, s * (3 + jit), 0), pivot=(s * x, Y, Z0 + 0.5), fur=3, rand=0)
                # grote dekveren
                m.cube(bn, (s * (x + stap / 2) - 2.3, Y + 0.5, Z0 + 1.0), (4.6, 0.6, lb), L,
                       verf=veer_verf(P, L, P["veer"], 200 + k * 50 + j),
                       rotation=(droop * 0.6, s * (2 + jit), 0), pivot=(s * (x + stap / 2), Y + 0.5, Z0 + 1.0),
                       fur=3, rand=0)
                # kleine dekveren (bovenste laag, manen-kleur)
                m.cube(bn, (s * x - 2.1, Y + 1.2, Z0 + 1.5), (4.2, 0.6, la), B,
                       verf=veer_verf(P, B, Dp, 300 + k * 50 + j),
                       rotation=(droop * 0.3, s * jit, 0), pivot=(s * x, Y + 1.2, Z0 + 1.5), fur=3, rand=0)
            pluisje(bn, (s * (x0 + 5), Y + 2.0, Z0 + 2.5), (2.0, 1.6, 2.4))
            if k == 1:  # losse rafelveer (grijs)
                m.cube(bn, (s * (x0 + 9) - 1.0, Y + 1.4, Z0 + 4), (2.0, 0.5, 9), P["veer"],
                       verf=alleen_grijs(P, veer_verf(P, P["veer"], P["veer_tip"], 77, 1.0)),
                       rotation=(-14, s * 28, -s * 12), pivot=(s * (x0 + 9), Y + 1.4, Z0 + 4), fur=3, rand=0)
            parent = bn
        # hand: handpennen in een waaier
        bn = f"wing_{n}_3"
        x0, x1 = 45.0, 56.0
        m.bone(bn, [s * x0, Y, Z0], parent)
        m.blob(bn, (s * (x0 + x1) / 2, Y + 0.3, Z0 + 1.4), (x1 - x0 + 2, 2.8, 3.6), B, r=1.0, verf=vl, fur=fur)
        m.blob(bn, (s * (x1 + 0.5), Y + 0.4, Z0 + 1.0), (3.2, 3.0, 3.2), Dp, r=1.0, verf=vl, fur=fur)  # polsje
        n_pen = 8
        for j in range(n_pen):
            t = j / (n_pen - 1)
            x = x0 + 1.5 + t * (x1 - x0 - 1.0)
            ang = 4 + t * 66 + rng.uniform(-2, 2)
            lengte = 26 + 9 * math.sin(t * math.pi * 0.85)
            m.cube(bn, (s * x - 2.6, Y - 0.2 - t * 0.3, Z0 + 0.6), (5.2, 0.6, lengte), P["veer"],
                   verf=veer_verf(P, P["veer"], P["veer_tip"] if thema != "roze" else (255, 236, 246), 400 + j),
                   rotation=(rng.uniform(0, 3), s * ang, 0), pivot=(s * x, Y, Z0 + 0.6), fur=3, rand=0)
            if j < n_pen - 1:
                m.cube(bn, (s * x - 2.2, Y + 0.6, Z0 + 1.0), (4.4, 0.6, 10.5), L,
                       verf=veer_verf(P, L, P["veer"], 450 + j),
                       rotation=(0, s * (ang + 3), 0), pivot=(s * x, Y + 0.6, Z0 + 1.0), fur=3, rand=0)

    # ---------------------------------------------------------------- staart
    staart = [(17.5, 11, 10, 12), (16.8, 8.6, 8, 12), (16.2, 6.6, 6.2, 12), (15.7, 5.0, 4.8, 11), (15.3, 3.8, 3.6, 7)]
    prev, z = "body", 16.0
    for i, (y, w, hh, l) in enumerate(staart):
        bn = f"tail_{i + 1}"
        m.bone(bn, [0, y, z], prev)
        m.blob(bn, (0, y, z + l / 2), (w, hh, l + 1.5), B, r=min(w, hh) * 0.28, verf=vl, fur=fur)
        m.box(bn, (0, y - hh / 2 + 0.4, z + l / 2), (w * 0.7, 0.8, l), BU, verf=zacht_v, fur=fur, rand=0.06)
        if i < 4:
            pluim(bn, (0, y + hh / 2 - 0.8, z + l * 0.45), 7.5 - i * 1.2, hoek=40, breed=3.8 - i * 0.4,
                  jitter=rng.uniform(-0.5, 0.5))
        if i == 2:
            pluisje(bn, (2.6, y + 1.5, z + 4), (1.8, 1.8, 2.2))
        prev, z = bn, z + l
    # staartwaaier
    for j in range(7):
        t = j / 6 - 0.5
        lengte = 20 - abs(t) * 10
        kl = P["manen"] if j % 2 == 0 else P["veer"]
        m.cube("tail_5", (-2.4, 15.0 + abs(t) * 0.4, z - 2), (4.8, 0.6, lengte), kl,
               verf=veer_verf(P, kl, P["manen_tip"], 600 + j), rotation=(-4, t * 100, 0), pivot=(0, 15.0, z - 2),
               fur=3, rand=0)

    # ---------------------------------------------------------------- poten
    for n, x, zc, voor in (("fl", 6.8, -9.5, True), ("fr", -6.8, -9.5, True), ("bl", 6.6, 10.0, False), ("br", -6.6, 10.0, False)):
        bn = f"leg_{n}"
        m.bone(bn, [x, 13.0, zc], "body")
        m.blob(bn, (x * 1.04, 10.0, zc), (6.6, 9, 7.2) if not voor else (6.0, 8.4, 6.6), B, r=2.0, verf=vl, fur=fur)
        m.box(bn, (x * 1.04, 4.8, zc + 0.3), (4.2, 4.6, 4.4), P["schub"], verf=schubben(0.85, 5), fur=fur, rand=0.08)
        fb = f"foot_{n}"
        m.bone(fb, [x * 1.04, 2.6, zc], bn)
        m.blob(fb, (x * 1.04, 1.3, zc - 1.0), (6.2, 2.6, 7.4), P["schub"], r=0.9, verf=schubben(0.88, 5), fur=fur)
        for t in (-1, 0, 1):
            m.box(fb, (x * 1.04 + t * 2.0, 0.6, zc - 5.1 + abs(t) * 0.6), (1.2, 1.2, 1.6), P["klauw"], fur=2, rand=0)
    return m




# =====================================================================================================================
# textures for the jar
# =====================================================================================================================
def _halveer(t):
    """HxWx4 (0..255) -> half size, alpha-aware box filter, cut-out alpha (GeckoLib cutout)."""
    h, w = t.shape[0] // 2 * 2, t.shape[1] // 2 * 2
    t = t[:h, :w]
    a = t[..., 3:4] / 255.0
    pm = t[..., :3] * a
    s = lambda x: (x[0::2, 0::2] + x[1::2, 0::2] + x[0::2, 1::2] + x[1::2, 1::2]) / 4.0
    aa = s(a)
    rgb = s(pm) / np.maximum(aa, 1e-6)
    uit = np.zeros((h // 2, w // 2, 4), np.float32)
    uit[..., :3] = rgb
    uit[..., 3] = np.where(aa[..., 0] >= 0.5, 255, 0)
    return uit


def _beeld(t, halveer=True):
    t = np.clip(t, 0, 255).astype(np.float32)
    if halveer and D_UIT != D:
        t = _halveer(t)
    return Image.fromarray(np.clip(t, 0, 255).astype(np.uint8), "RGBA")


def _bone_maskers(geo, shape):
    """Per bone, the texture pixels its faces use (shape = the painted texture, D px per unit)."""
    k = shape[1] / geo["description"]["texture_width"]
    uit = {}
    for b in geo["bones"]:
        m = np.zeros(shape[:2], bool)
        for c in b.get("cubes", []):
            for f in c["uv"].values():
                u, v = f["uv"]
                du, dv = f["uv_size"]
                m[int(round(v * k)):int(round((v + dv) * k)), int(round(u * k)):int(round((u + du) * k))] = True
        uit[b["name"]] = m
    return uit


# cheering-up steps (Vahoegnir.splinters() 0..5): which bone groups have turned pink
STAPPEN = [[], ["wing_l_3", "wing_r_3"], ["wing_l", "wing_r", "wing_l_2", "wing_r_2"], ["tail_1", "tail_2", "tail_3", "tail_4", "tail_5"],
           ["body", "neck_1", "neck_2", "neck_3", "leg_fl", "leg_fr", "leg_bl", "leg_br", "foot_fl", "foot_fr", "foot_bl", "foot_br"],
           ["head", "jaw", "ear_l", "ear_r"]]


def texturen():
    """-> (geo, {naam: PIL image}) for the big dragon."""
    mg, mr, mgl = bouw("grijs"), bouw("roze"), bouw("gloed")
    geo = mg.geo()["minecraft:geometry"][0]
    grijs, roze, gloed = mg.textuur(), mr.textuur(), mgl.textuur()
    assert grijs.shape == roze.shape == gloed.shape
    uit = {"draak": _beeld(grijs), "draak_gloed": _beeld(gloed)}
    maskers = _bone_maskers(geo, grijs.shape)
    leeg = np.zeros_like(gloed)
    m = np.zeros(grijs.shape[:2], bool)
    for i, groep in enumerate(STAPPEN):
        for b in groep:
            m |= maskers[b]
        a = grijs.copy()
        a[m] = roze[m]
        uit[f"vahoegnir_{i}"] = _beeld(a)
        g = gloed.copy()
        g[m] = leeg[m]          # pink parts stop glowing (the head turns last: step 5 has no glow)
        uit[f"vahoegnir_{i}_glowmask"] = _beeld(g)
    return geo, uit


def geo_bestand(geo):
    g = json.loads(json.dumps(geo))
    g["description"].update({"identifier": "geometry.vr_draak", "visible_bounds_width": 40, "visible_bounds_height": 20,
                             "visible_bounds_offset": [0, 4, 0]})
    return {"format_version": "1.12.0", "minecraft:geometry": [g]}


# =====================================================================================================================
# animations
# =====================================================================================================================
def kf(paren, loop=True, lengte=None):
    tijden = [float(t) for b in paren.values() for ch in b.values() for t in ch]
    return {"loop": loop, "animation_length": lengte or (max(tijden) if tijden else 1.0),
            "bones": {b: {ch: {str(float(t)): [float(x) for x in v] for t, v in pts.items()} for ch, pts in kanalen.items()}
                      for b, kanalen in paren.items()}}


def _spiegel(paren):
    """Adds the right-hand bones (wing_r.., ear_r) from the left-hand ones: rot y and z flip."""
    uit = dict(paren)
    for b, kan in paren.items():
        if "_l" in b and (b.startswith("wing_l") or b == "ear_l"):
            r = b.replace("_l", "_r", 1)
            uit[r] = {ch: {t: ([v[0], -v[1], -v[2]] if ch == "rotation" else [-v[0], v[1], v[2]]) for t, v in pts.items()}
                      for ch, pts in kan.items()}
    return uit


def _vast(paren, t):
    """Constant pose over [0, t]."""
    return {b: {"rotation": {0: v, t: v}} for b, v in paren.items()}


POTEN_IN = {"leg_fl": [55, 0, 0], "leg_fr": [55, 0, 0], "foot_fl": [40, 0, 0], "foot_fr": [40, 0, 0],
            "leg_bl": [70, 0, 0], "leg_br": [70, 0, 0], "foot_bl": [50, 0, 0], "foot_br": [50, 0, 0]}
NEK_VLIEG = {"neck_1": [10, 0, 0], "neck_2": [8, 0, 0], "neck_3": [-4, 0, 0], "head": [-6, 0, 0]}
STAAN_VLEUGEL = {"wing_l": [-4, 12, -40], "wing_l_2": [0, 0, 12], "wing_l_3": [0, 0, 18]}


def _staartgolf(t, a=6.0, fase=0.0):
    return {f"tail_{i}": {"rotation": {0: [2, a * math.sin(fase + i * 0.7), 0], t / 2: [2, -a * math.sin(fase + i * 0.7), 0],
                                       t: [2, a * math.sin(fase + i * 0.7), 0]}} for i in range(1, 6)}


def anim_vlieg(t=1.6):
    p = {"wing_l": {"rotation": {0: [0, 4, -38], t / 2: [0, -2, 26], t: [0, 4, -38]}},
         "wing_l_2": {"rotation": {0: [0, 0, 12], t / 4: [0, 0, -4], t / 2: [0, 0, -14], 3 * t / 4: [0, 0, 8], t: [0, 0, 12]}},
         "wing_l_3": {"rotation": {0: [0, 0, 20], t / 4: [0, 0, -2], t / 2: [0, 0, -18], 3 * t / 4: [0, 0, 10], t: [0, 0, 20]}},
         "root": {"position": {0: [0, -2, 0], t / 2: [0, 3, 0], t: [0, -2, 0]}},
         "body": {"rotation": {0: [-4, 0, 0], t / 2: [-1, 0, 0], t: [-4, 0, 0]}},
         **_vast({**POTEN_IN, **NEK_VLIEG}, t), **_staartgolf(t, 5)}
    p["head"] = {"rotation": {0: [-6, 0, 0], t / 2: [-2, 0, 0], t: [-6, 0, 0]}}
    return kf(_spiegel(p))


def anim_zweef(t=4.0):
    p = {"wing_l": {"rotation": {0: [0, 6, -12], t / 2: [0, 6, -6], t: [0, 6, -12]}},
         "wing_l_2": {"rotation": {0: [0, 0, 4], t / 2: [0, 0, -2], t: [0, 0, 4]}},
         "wing_l_3": {"rotation": {0: [0, 0, -8], t / 2: [0, 0, -14], t: [0, 0, -8]}},
         "body": {"rotation": {0: [-2, 0, 0], t / 2: [0, 0, 0], t: [-2, 0, 0]}},
         **_vast({**POTEN_IN, **NEK_VLIEG}, t), **_staartgolf(t, 8)}
    return kf(_spiegel(p))


def anim_brul(t=2.0):
    p = {"neck_1": {"rotation": {0: [0, 0, 0], 0.4: [-16, 0, 0], 1.4: [-16, 0, 0], t: [0, 0, 0]}},
         "neck_2": {"rotation": {0: [0, 0, 0], 0.4: [-10, 0, 0], 1.4: [-10, 0, 0], t: [0, 0, 0]}},
         "head": {"rotation": {0: [0, 0, 0], 0.4: [-22, 0, 0], 0.9: [-18, 6, 0], 1.4: [-22, -6, 0], t: [0, 0, 0]}},
         "jaw": {"rotation": {0: [0, 0, 0], 0.4: [30, 0, 0], 1.4: [30, 0, 0], t: [0, 0, 0]}},
         "ear_l": {"rotation": {0: [0, 0, 0], 0.4: [-28, 0, 10], 1.4: [-28, 0, 10], t: [0, 0, 0]}}}
    return kf(_spiegel(p), loop=False)


def boss_anims():
    st = 2.0
    staan = _vast({**STAAN_VLEUGEL}, 3.0)
    idle = {**staan,
            "wing_l": {"rotation": {0: [-4, 12, -40], 1.5: [-4, 12, -46], 3.0: [-4, 12, -40]}},
            "body": {"rotation": {0: [-6, 0, 0], 1.5: [-7, 0, 0], 3.0: [-6, 0, 0]}},
            "neck_1": {"rotation": {0: [-6, 0, 0], 1.5: [-9, 0, 0], 3.0: [-6, 0, 0]}},
            "neck_3": {"rotation": {0: [6, 0, 0], 3.0: [6, 0, 0]}},
            "head": {"rotation": {0: [10, 0, 0], 1.5: [8, 10, 0], 3.0: [10, 0, 0]}},
            "ear_l": {"rotation": {0: [0, 0, 0], 2.3: [0, 0, 0], 2.45: [0, 0, 12], 2.6: [0, 0, 0], 3.0: [0, 0, 0]}},
            **_staartgolf(3.0, 9)}
    walk = {**_vast({**STAAN_VLEUGEL}, st),
            "wing_l": {"rotation": {0: [-4, 12, -30], 1.0: [-4, 12, -36], st: [-4, 12, -30]}},
            "root": {"position": {0: [0, 0, 0], 0.5: [0, 1.5, 0], 1.0: [0, 0, 0], 1.5: [0, 1.5, 0], st: [0, 0, 0]}},
            "leg_fl": {"rotation": {0: [-25, 0, 0], 1.0: [25, 0, 0], st: [-25, 0, 0]}},
            "leg_br": {"rotation": {0: [-25, 0, 0], 1.0: [25, 0, 0], st: [-25, 0, 0]}},
            "leg_fr": {"rotation": {0: [25, 0, 0], 1.0: [-25, 0, 0], st: [25, 0, 0]}},
            "leg_bl": {"rotation": {0: [25, 0, 0], 1.0: [-25, 0, 0], st: [25, 0, 0]}},
            "neck_1": {"rotation": {0: [-4, 0, 0], 0.5: [-1, 0, 0], 1.0: [-4, 0, 0], 1.5: [-1, 0, 0], st: [-4, 0, 0]}},
            "head": {"rotation": {0: [6, 0, 0], 1.0: [4, 0, 0], st: [6, 0, 0]}},
            **_staartgolf(st, 10)}
    adem = {"neck_1": {"rotation": {0: [0, 0, 0], 0.4: [-15, 0, 0], 0.6: [12, 0, 0], 1.4: [12, 0, 0], 1.6: [0, 0, 0]}},
            "neck_2": {"rotation": {0: [0, 0, 0], 0.4: [-8, 0, 0], 0.6: [6, 0, 0], 1.4: [6, 0, 0], 1.6: [0, 0, 0]}},
            "head": {"rotation": {0: [0, 0, 0], 0.4: [-12, 0, 0], 0.6: [10, 0, 0], 1.4: [10, 0, 0], 1.6: [0, 0, 0]}},
            "jaw": {"rotation": {0: [0, 0, 0], 0.5: [32, 0, 0], 1.4: [32, 0, 0], 1.6: [0, 0, 0]}},
            "ear_l": {"rotation": {0: [0, 0, 0], 0.5: [-25, 0, 0], 1.4: [-25, 0, 0], 1.6: [0, 0, 0]}}}
    duik = {"body": {"rotation": {0: [0, 0, 0], 0.4: [-15, 0, 0], 0.8: [35, 0, 0], 1.4: [0, 0, 0]}},
            "wing_l": {"rotation": {0: [0, 0, -30], 0.4: [0, 0, -45], 0.8: [0, -50, 10], 1.4: [0, 0, 0]}},
            "wing_l_3": {"rotation": {0: [0, 0, 0], 0.8: [0, -30, 0], 1.4: [0, 0, 0]}},
            "neck_1": {"rotation": {0: [0, 0, 0], 0.8: [20, 0, 0], 1.4: [0, 0, 0]}},
            **{k: {"rotation": {0: v, 1.4: v}} for k, v in POTEN_IN.items()}}
    land = {"root": {"position": {0: [0, 14, 0], 0.6: [0, 6, 0], 1.0: [0, 0, 0]}},
            "wing_l": {"rotation": {0: [0, 0, -35], 0.3: [0, 0, 28], 0.6: [0, 0, -30], 1.0: [-4, 12, -40]}},
            "wing_l_2": {"rotation": {0: [0, 0, 10], 0.3: [0, 0, -12], 0.6: [0, 0, 10], 1.0: [0, 0, 12]}},
            "leg_fl": {"rotation": {0: [-30, 0, 0], 1.0: [0, 0, 0]}}, "leg_fr": {"rotation": {0: [-30, 0, 0], 1.0: [0, 0, 0]}},
            "leg_bl": {"rotation": {0: [20, 0, 0], 1.0: [0, 0, 0]}}, "leg_br": {"rotation": {0: [20, 0, 0], 1.0: [0, 0, 0]}}}
    buig = {**_vast({**STAAN_VLEUGEL}, 2.4),
            "neck_1": {"rotation": {0: [0, 0, 0], 0.8: [30, 0, 0], 2.0: [30, 0, 0], 2.4: [0, 0, 0]}},
            "neck_2": {"rotation": {0: [0, 0, 0], 0.8: [20, 0, 0], 2.0: [20, 0, 0], 2.4: [0, 0, 0]}},
            "head": {"rotation": {0: [0, 0, 0], 0.8: [18, 0, 0], 2.0: [18, 0, 0], 2.4: [0, 0, 0]}},
            "ear_l": {"rotation": {0: [0, 0, 0], 0.8: [20, 0, -10], 2.0: [20, 0, -10], 2.4: [0, 0, 0]}}}
    return {"animations": {
        "idle": kf(_spiegel(idle)), "walk": kf(_spiegel(walk)), "vlieg": anim_vlieg(),
        "brul": anim_brul(), "adem": kf(_spiegel(adem), loop=False), "duik": kf(_spiegel(duik), loop=False),
        "land": kf(_spiegel(land), loop=False), "buig": kf(_spiegel(buig), loop=False)}}


def vlucht_anims():
    return {"animations": {"vlieg": anim_vlieg(), "zweef": anim_zweef(), "brul": anim_brul()}}


# =====================================================================================================================
# Vahoegje: the chibi baby (pink form, guh head)
# =====================================================================================================================
def _bol(B):
    def f(arr, face=None):
        h, w = arr.shape[:2]
        zacht(arr, w / 2, h * 0.45, w * 0.62, h * 0.62, licht(B, 1.10), 0.55)
        yy, xx = np.mgrid[0:h, 0:w]
        d = np.minimum(np.minimum(xx + 0.5, w - xx - 0.5), np.minimum(yy + 0.5, h - yy - 0.5)) / max(4.0, min(h, w) * 0.22)
        arr[..., :3] *= (0.90 + 0.10 * np.clip(d, 0, 1))[..., None]
    return f


def vahoegje():
    P = dict(THEMAS["roze"])
    P["basis"], P["licht"] = (255, 176, 218), (255, 214, 236)
    B, L, BU = P["basis"], P["licht"], P["buik"]
    bol = _bol(B)
    m = Model("vr_vahoegje", atlas_w=1024, d=4)
    vl = veren_lijf(P)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 5, 0], "root")
    m.blob("body", (0, 5.6, 0.8), (8, 7, 9), B, r=2.5, verf=vl, fur=4)
    m.blob("body", (0, 4.6, -2.6), (6, 4.6, 4), BU, r=1.5, verf=bol, fur=3)
    for i, z in enumerate((-1.5, 1.5, 4.0)):
        m.staaf("body", (0, 8.6, z), (0, 11.0 - i * 0.6, z + 2.2), 0.7, P["manen"], verf=pluim_verf(P, 30 + i), d=2.4)
    # head: big round guh head with plate eyes, small snout, disc ears
    m.bone("head", [0, 8.5, -2.5], "body")
    m.blob("head", (0, 13.0, -3.5), (11, 9.5, 9.5), B, r=3.0, verf=bol, fur=3)
    for s in (1, -1):
        c = (s * 2.6, 13.0, -8.35)
        m.cube("head", (c[0] - 1.9, c[1] - 2.1, c[2] - 0.4), (3.8, 4.2, 0.8), B, verf={"north": oog_verf("roze", s)},
               rotation=(0, -18 * s, 0), pivot=c, fur=1, rand=0.0)
        m.blob("head", (s * 4.2, 10.6, -7.4), (2.2, 2.0, 2.0), B, r=0.6,
               verf=lambda a, f=None: (bol(a, f), zacht(a, a.shape[1] / 2, a.shape[0] / 2, a.shape[1] * 0.5, a.shape[0] * 0.5, (255, 96, 150), 0.8)), fur=2)

    def snuit(arr, face=None):
        h, w = arr.shape[:2]
        bol(arr, face)
        if face == "north":
            ellips(arr, w / 2, h * 0.32, w * 0.16, h * 0.14, (236, 104, 160))
            for x in range(int(w * 0.32), int(w * 0.68) + 1):
                u = (x - w * 0.32) / (w * 0.36)
                y = int(round(h * 0.66 + abs(math.sin(u * 2 * math.pi)) * h * 0.08))
                arr[y:y + 1, x, :3] = (150, 60, 100)
    m.blob("head", (0, 10.3, -9.0), (4.8, 3.0, 2.6), B, r=0.9, verf=snuit, voor=snuit, fur=2)

    def oor(arr, face=None):
        h, w = arr.shape[:2]
        yy, xx = np.mgrid[0:h, 0:w]
        if face in ("north", "south"):
            d = ((xx + 0.5 - w / 2) / (w / 2)) ** 2 + ((yy + 0.5 - h / 2) / (h / 2)) ** 2
            arr[d > 1, 3] = 0
            bol(arr, face)
            if face == "north":
                zacht(arr, w / 2, h * 0.52, w * 0.38, h * 0.38, (255, 150, 196), 0.95)
        elif face in ("east", "west"):
            arr[(yy < h * 0.15) | (yy > h * 0.85), 3] = 0
        else:
            arr[..., 3] = 0
    for s, n in ((1, "l"), (-1, "r")):
        m.bone(f"ear_{n}", [s * 3.4, 17.0, -3.5], "head")
        m.cube(f"ear_{n}", (s * 4.9 - 2.6, 16.2, -4.0), (5.2, 5.6, 0.9), B, verf={f: oor for f in ("north", "south", "east", "west", "up", "down")},
               rotation=(0, 0, -16 * s), pivot=(s * 3.4, 17.0, -3.5), fur=2, rand=0)
    for i, z in enumerate((-6.0, -3.6)):
        m.staaf("head", (0, 17.6, z), (0, 20.0 - i * 0.4, z + 1.8), 0.7, P["manen"], verf=pluim_verf(P, 40 + i), d=2.2)
    # little wings: a bar + 4 feathers
    for s, n in ((1, "l"), (-1, "r")):
        m.bone(f"wing_{n}", [s * 3.6, 8.0, -0.5], "body")
        m.blob(f"wing_{n}", (s * 5.4, 8.2, 0.0), (4.4, 1.6, 2.2), B, r=0.5, verf=vl, fur=3)
        for j in range(4):
            x = 4.0 + j * 1.3
            m.cube(f"wing_{n}", (s * x - 1.0, 7.9, 0.4), (2.0, 0.4, 4.2 + j * 0.8), P["veer"],
                   verf=veer_verf(P, P["veer"], P["veer_tip"], 700 + j), rotation=(0, s * (6 + j * 14), 0), pivot=(s * x, 7.9, 0.4),
                   fur=2, rand=0)
    # tail with a little fan
    m.bone("tail", [0, 5.2, 4.8], "body")
    m.blob("tail", (0, 5.0, 7.6), (3.2, 2.8, 6.0), B, r=0.9, verf=vl, fur=3)
    for j in range(5):
        t = j / 4 - 0.5
        m.cube("tail", (-1.0, 5.0, 9.6), (2.0, 0.4, 4.6 - abs(t) * 2), P["manen"] if j % 2 == 0 else P["veer"],
               verf=veer_verf(P, P["manen"], P["manen_tip"], 720 + j), rotation=(-6, t * 90, 0), pivot=(0, 5.0, 9.6), fur=2, rand=0)
    for n, x, z in (("leg_fl", 2.4, -2.0), ("leg_fr", -2.4, -2.0), ("leg_bl", 2.4, 3.2), ("leg_br", -2.4, 3.2)):
        m.bone(n, [x, 2.6, z], "body")
        m.blob(n, (x, 1.4, z - 0.2), (2.6, 2.8, 3.0), B, r=0.7, verf=bol, fur=2)
        for t in (-1, 1):
            m.box(n, (x + t * 0.6, 0.3, z - 1.75), (0.6, 0.6, 0.5), P["klauw"], fur=1, rand=0)
    return m


def vahoegje_anims():
    st = [0, 0, 0]
    return {"animations": {
        "idle": kf(_spiegel({"body": {"scale": {0: [1, 1, 1], 1.0: [1.04, 1.05, 1.03], 2.0: [1, 1, 1]}},
                             "wing_l": {"rotation": {0: [0, 0, -10], 1.0: [0, 0, -20], 2.0: [0, 0, -10]}},
                             "head": {"rotation": {0: [0, 0, 0], 1.2: [0, 0, 8], 1.6: [0, 0, 0], 2.0: [0, 0, 0]}},
                             "ear_l": {"rotation": {0: [0, 0, 0], 1.4: [0, 0, 0], 1.5: [0, 0, -14], 1.6: [0, 0, 0], 2.0: [0, 0, 0]}},
                             "tail": {"rotation": {0: [0, 12, 0], 1.0: [0, -12, 0], 2.0: [0, 12, 0]}}})),
        "hop": kf(_spiegel({"root": {"position": {0: st, 0.25: [0, 3, 0], 0.5: st}},
                            "wing_l": {"rotation": {0: [0, 0, 0], 0.25: [0, 0, -45], 0.5: [0, 0, 0]}},
                            "ear_l": {"rotation": {0: [0, 0, 0], 0.25: [-15, 0, 0], 0.5: [0, 0, 0]}}})),
        "sprong": kf(_spiegel({"root": {"position": {0: st, 0.4: [0, 8, 0], 0.9: st}},
                               "wing_l": {"rotation": {0: [0, 0, 0], 0.15: [0, 0, -45], 0.3: [0, 0, 30], 0.45: [0, 0, -45],
                                                       0.6: [0, 0, 30], 0.9: [0, 0, 0]}}}), loop=False),
    }}


# =====================================================================================================================
# writing
# =====================================================================================================================
def alles(w, a_dir, png):
    """Writes every dragon asset; returns {blockbench-id: (geo json, image, anims)}."""
    geo_dir = os.path.join(a_dir, "geckolib", "models", "entity")
    anim_dir = os.path.join(a_dir, "geckolib", "animations", "entity")
    wez = os.path.join(a_dir, "textures", "vadsrijk", "wezens")
    z3 = os.path.join(a_dir, "textures", "vadsrijk", "z3bazen")
    geo, tex = texturen()
    gb = geo_bestand(geo)
    w(os.path.join(geo_dir, "vadsrijk", "wezens", "draak.geo.json"), gb)
    w(os.path.join(anim_dir, "vadsrijk", "wezens", "draak.animation.json"), {"format_version": "1.8.0", **vlucht_anims()})
    ba = boss_anims()
    w(os.path.join(anim_dir, "vr_z3_baas_vahoegnir.animation.json"), {"format_version": "1.8.0", **ba})
    png(os.path.join(wez, "draak.png"), tex["draak"])
    png(os.path.join(wez, "draak_gloed.png"), tex["draak_gloed"])
    for i in range(6):
        png(os.path.join(z3, f"vahoegnir_{i}.png"), tex[f"vahoegnir_{i}"])
        png(os.path.join(z3, f"vahoegnir_{i}_glowmask.png"), tex[f"vahoegnir_{i}_glowmask"])
    vj = vahoegje()
    vg = vj.geo()
    vg["minecraft:geometry"][0]["description"].update({"identifier": "geometry.vr_vahoegje", "visible_bounds_width": 2,
                                                        "visible_bounds_height": 2, "visible_bounds_offset": [0, 0.6, 0]})
    w(os.path.join(geo_dir, "vadsrijk", "wezens", "vahoegje.geo.json"), vg)
    pa = vahoegje_anims()
    w(os.path.join(anim_dir, "vr_huisdier_vahoegje.animation.json"), {"format_version": "1.8.0", **pa})
    vim = _beeld(vj.textuur(), halveer=False)
    png(os.path.join(z3, "vahoegje.png"), vim)
    return {"vr_z3_baas_vahoegnir": (gb, tex["vahoegnir_5"], ba), "vr_huisdier_vahoegje": (vg, vim, pa)}


def _direct():
    def _w(pad, obj):
        os.makedirs(os.path.dirname(pad), exist_ok=True)
        with open(pad, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write("\n")

    def _p(pad, im):
        os.makedirs(os.path.dirname(pad), exist_ok=True)
        im.save(pad)
    bb = alles(_w, os.path.join("src", "main", "resources", "assets", "guhs"), _p)
    print(f"vadsrijk_draak: dragon + Vahoegje written ({len(bb)} models)")


if __name__ == "__main__":
    _direct()
