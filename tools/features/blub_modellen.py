"""
1.8.0 (Guhdromenland), the Blub-guh's looks: its GeckoLib model, animations and textures.

The look is the one the user approved in the m06 concept renders (dromen-ontwerp/_bron/mobs/blubmeter.py): the voxel
model below is copied from there UNCHANGED (K = 2 voxels per model pixel, only renamed model -> model_vox), and every
texture is sampled from it, face by face, so the mod looks like the renders. Measured on the real Blub (Minecraft
Dungeons II): one 10 x 10 x 7.5 body block, two flat 3 x 4 x 1 ears, four 3 x 2 x 3 stubby legs, three dark red stripes.

The Blub-Guh-Meter: one model with every part as a bone; BlubClient switches bones on and off (sharp or soft body, Blub
ears, small guh ears, big guh ears, each soort's decoration) and picks the texture
  textures/entity/blubguh/<soort>_<tint>_<stap>.png   stap = p (0), b (1-19), or the meter rounded down to 5 (20..100)
(the colour slides to guh pink in steps of 5, the face changes at 20 / 50 (guh eyes, then the whole face with the :3 mouth), the ears at 60 / 80 / 100; the round ear tips
at 80+ are see-through texture). The strikje is drawn white and tinted in its own pass; the blue Blub glows a little
(blubguh/blauw_gloed.png).

  build(h)    writes geckolib/models/entity/blubguh.geo.json, geckolib/animations/entity/blubguh.animation.json, the
              textures and blockbench/BlubGuh.bbmodel (to look at / tweak by hand; the game files come from here)
  check(h)    every animated bone exists, the bones BlubClient switches exist
"""
import math

import numpy as np
from PIL import Image

# =====================================================================================================================
# the approved voxel model (copied from dromen-ontwerp/_bron/mobs/blubmeter.py; do not change the look here)
# =====================================================================================================================
K = 2                       # voxels per texel
SOORTEN = ["blauw", "weide", "schild", "ravijn", "zwam"]
NAAM = {"blauw": "Blauw", "weide": "Weide", "schild": "Schildje", "ravijn": "Ravijn", "zwam": "Zwam"}
KANS = {"blauw": .50, "weide": .22, "schild": .15, "ravijn": .09, "zwam": .04}

ZW, WIT, CYAAN, BLAUWOOG = (30, 24, 40), (255, 255, 255), (110, 222, 234), (62, 128, 222)
BLOS, NEUS, MOND = (255, 160, 196), (255, 130, 168), (136, 86, 150)
ROZE = (214, 168, 216)       # guh-roze (stijl E)
OORROZE = (255, 176, 206)

GUHOOG = [".KKKK.",
          "KWWKKK",
          "KWKKKK",
          "KKKKWK",
          "CKKKKC",
          ".BBBB."]
GK = {"K": ZW, "W": WIT, "C": CYAAN, "B": BLAUWOOG}

# per soort: kleuren (rollen) + streepkleur + gloed van het lijf
PAL = {
    "blauw": dict(rand=(160, 213, 247), lijf=(138, 201, 247), kern=(132, 196, 246), onder=None,
                  poot=(138, 201, 247), streep=(112, 67, 80), oor=(160, 213, 247), oorb=(118, 178, 232), gloed=1.15),
    "weide": dict(rand=(96, 172, 178), lijf=(92, 178, 140), kern=(86, 168, 132), onder=(206, 182, 160),
                  poot=(200, 160, 132), streep=(46, 92, 112), spriet=(52, 98, 118), tip=(170, 214, 222),
                  stip=(186, 228, 232), lamp=(92, 158, 178), lampoog=(70, 236, 255), gloed=1.0),
    "schild": dict(rand=(226, 182, 164), lijf=(160, 214, 182), kern=(170, 224, 192), ring=(226, 214, 160),
                   hoek=(150, 162, 210), onder=None, poot=(200, 166, 152), streep=(12, 66, 86),
                   vleugel=(186, 58, 70), vleugel2=(206, 104, 104), spriet=(132, 196, 160), tip=(196, 40, 56),
                   stip=(206, 150, 200), gloed=1.0),
    "ravijn": dict(rand=(150, 96, 160), lijf=(160, 108, 172), kern=(150, 100, 162), onder=(206, 196, 170),
                   poot=(196, 186, 160), streep=(58, 46, 46), spriet=(72, 164, 132), oorb=(116, 70, 128),
                   stip=(76, 178, 150), vin=(206, 146, 206), vin2=(186, 116, 190), gloed=1.0),
    "zwam": dict(rand=(200, 212, 220), lijf=(226, 232, 238), kern=(234, 238, 242), onder=None,
                 poot=(214, 222, 228), streep=(48, 102, 128), gewei=(184, 194, 200), tip=(214, 222, 226), gloed=1.0),
}
# tinten: 0 = normaal (meest common), 1 en 2 = pastel (meng naar een licht doel)
TINT = {
    "blauw": [None, ((196, 238, 250), .45, "ijsblauw"), ((206, 206, 255), .42, "babyblauw")],
    "weide": [None, ((214, 246, 200), .42, "muntgroen"), ((236, 242, 190), .40, "lentegroen")],
    "schild": [None, ((250, 226, 214), .42, "perzik"), ((214, 236, 250), .38, "zeepbel")],
    "ravijn": [None, ((236, 206, 246), .42, "lavendel"), ((246, 200, 222), .40, "framboos")],
    "zwam": [None, ((250, 240, 220), .45, "vanille"), ((226, 236, 250), .45, "maanwit")],
}
VERSIER = {"oor", "oorb", "spriet", "tip", "lamp", "vleugel", "vleugel2", "vin", "vin2", "gewei", "stip"}


def _lum(c):
    return .3 * c[0] + .59 * c[1] + .11 * c[2]


def _mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def guh_t(meter):
    if meter >= 100:
        return 1.0
    return max(0.0, (meter - 20) / 80.0) ** 1.6


def palet(soort, tint, meter):
    P = dict(PAL[soort])
    tv = TINT[soort][tint]
    t = guh_t(meter)
    out = {}
    for k, c in P.items():
        if not isinstance(c, tuple) or c is None:
            out[k] = c
            continue
        if k in ("streep", "lampoog"):
            out[k] = c
            continue
        if tv:
            c = _mix(c, tv[0], tv[1])
        # naar guh-roze, met behoud van helderheid (patroon blijft zichtbaar)
        doel = tuple(min(255, int(v * (_lum(c) / _lum(ROZE)) ** .8)) for v in ROZE)
        sterkte = t * (0.55 if k in VERSIER else 1.0)
        out[k] = _mix(c, doel, sterkte)
    return out


def trek(rng):
    """Random Blub zoals bij het spawnen: klokvormige meter rond 35 (normaal mu=35, sigma=18, afgekapt op 0..100)."""
    meter = int(np.clip(round(rng.normal(35, 18)), 0, 100))
    soort = rng.choice(SOORTEN, p=[KANS[s] for s in SOORTEN])
    tint = int(rng.choice([0, 1, 2], p=[.6, .2, .2]))
    grootte = float(np.clip(rng.normal(1.0, .08), .85, 1.15))
    return meter, str(soort), tint, grootte


def model_vox(meter, soort, tint=0):
    P = palet(soort, tint, meter)
    vox = {}
    X0, X1 = -5 * K, 5 * K
    Z0, Z1 = -5 * K, 5 * K
    hoogte = 17 if soort == "zwam" else 15            # zwam is een tikje hoger
    Y0, Y1 = 2 * K, 2 * K + hoogte
    zacht = meter >= 60
    onderband = {"weide": 5, "ravijn": 6}.get(soort, 0)  # zandkleurige onderkant (voxels)

    def lijfkleur(x, y, z):
        dx = min(x - X0, X1 - 1 - x)
        dy = min(y - Y0, Y1 - 1 - y)
        dz = min(z - Z0, Z1 - 1 - z)
        d = sorted((dx, dy, dz))
        if onderband and y < Y0 + onderband:
            return P["onder"]
        if soort == "schild":
            # schild: blauwe hoekjes, perzik rand, gele ring, munt midden
            if d[1] < K and d[2] < K * 2:
                return P["hoek"]
            if d[1] < K:
                return P["rand"]
            if d[1] < 2 * K:
                return P["ring"]
            return P["kern"]
        if d[1] < K:
            return P["rand"]
        if d[1] < 2 * K:
            return P["lijf"]
        return P["kern"]

    for x in range(X0, X1):
        for y in range(Y0, Y1):
            for z in range(Z0, Z1):
                vox[(x, y, z)] = lijfkleur(x, y, z)
    for (px, pz) in ((X0, Z0), (X1 - 3 * K, Z0), (X0, Z1 - 3 * K), (X1 - 3 * K, Z1 - 3 * K)):
        for x in range(px, px + 3 * K):
            for y in range(0, Y0):
                for z in range(pz, pz + 3 * K):
                    vox[(x, y, z)] = P["poot"]

    # stipjes (weide/schild/ravijn) op boven- en zijvlakken
    rng = np.random.default_rng({"weide": 3, "schild": 4, "ravijn": 5}.get(soort, 0))
    if "stip" in P:
        for _ in range({"weide": 9, "schild": 3, "ravijn": 7}[soort]):
            if rng.random() < .55:
                x, z = rng.integers(X0 + 2, X1 - 3), rng.integers(Z0 + 3, Z1 - 3)
                for i in range(2):
                    for j in range(2):
                        vox[(x + i, Y1 - 1, z + j)] = P["stip"]
            else:
                x = X0 if rng.random() < .5 else X1 - 1
                y, z = rng.integers(Y0 + onderband + 2, Y1 - 3), rng.integers(Z0 + 4, Z1 - 3)
                for i in range(2):
                    for j in range(2):
                        vox[(x, y + i, z + j)] = P["stip"]

    if zacht:   # afgeronde randen
        for p in list(vox):
            x, y, z = p
            if y < Y0:
                continue
            if (x in (X0, X1 - 1)) + (y in (Y0, Y1 - 1)) + (z in (Z0, Z1 - 1)) >= 2:
                del vox[p]

    def staafje(pad, kleur, tipkleur=None, dik=1):
        """pad = lijst texel-punten (tx, ty, tz) relatief aan bovenvlak-midden; tekent 1x1 texel blokjes."""
        for i, (tx, ty, tz) in enumerate(pad):
            c = tipkleur if (tipkleur and i >= len(pad) - 1) else kleur
            for a in range(K * dik):
                for b in range(K):
                    for e in range(K * dik):
                        vox[(int(tx * K) + a, Y1 + int(ty * K) + b, int(tz * K) + e)] = c

    def plaat(punten, kleur_fn):
        """dunne plaat: punten = lijst (x,y,z) voxels."""
        for (x, y, z) in punten:
            vox[(x, y, z)] = kleur_fn(x, y, z)

    # --- versiering per soort
    ronde_oren = meter >= 100
    if soort == "blauw" and not ronde_oren:
        for ox in (1, 6):
            xa, za, hoog = X0 + ox * K, Z0 + K, 4 * K
            for x in range(xa, xa + 3 * K):
                for y in range(Y1, Y1 + hoog):
                    for z in range(za, za + K):
                        rx = min(x - xa, xa + 3 * K - 1 - x)
                        ry = Y1 + hoog - 1 - y
                        if meter >= 80 and ry < K and rx < K // 2 + 1:
                            continue
                        binnen = rx >= K and ry >= K
                        vox[(x, y, z)] = (OORROZE if meter >= 60 else P["oorb"]) if binnen else P["oor"]
    elif soort == "weide":
        roze_tip = OORROZE if meter >= 60 else P["tip"]
        for (tx, tz, h) in ((-3.5, -3.5, 4), (-1.5, -2.5, 5), (0.5, -3.5, 3), (-2.5, -0.5, 3)):
            staafje([(tx, i, tz) for i in range(h)], P["spriet"], roze_tip)
        # lampje aan een hengel vanaf de achterkant
        pad = [(2, 0, 4), (2, 1, 4), (2, 2, 4.5), (2, 3, 5), (2, 4, 5.5), (2, 5, 6), (2, 6, 6.5), (2, 7, 7)]
        staafje(pad, P["lamp"])
        lx, ly, lz = 2 * K - 3, Y1 + 8 * K, 7 * K - 3
        for x in range(lx, lx + 4 * K):
            for y in range(ly, ly + 4 * K):
                for z in range(lz, lz + 4 * K):
                    vox[(x, y, z)] = P["lamp"]
        for x in range(lx + 2, lx + 5):
            for y in range(ly + 3, ly + 5):
                vox[(x, y, lz - 1)] = P["lampoog"]
    elif soort == "schild":
        # rode vleugeltjes schuin langs beide zijkanten, en 3 sprietjes achter
        for s in (-1, 1):
            xs = X0 - 1 if s < 0 else X1
            pts = []
            for i in range(13):            # schuin naar beneden-achter, steekt naar buiten uit
                for j in range(5):
                    pts.append((xs + s * (i // 3), Y1 - 1 - i, Z0 + 4 + j + i // 2))
            plaat(pts, lambda x, y, z: P["vleugel"] if (y + z) % 5 else P["vleugel2"])
        tip = OORROZE if meter >= 60 else P["tip"]
        for (tx, tz, h) in ((-2, 3, 3), (0, 4, 4.5), (2, 3, 3)):
            staafje([(tx, i, tz) for i in range(int(h))], P["spriet"], tip)
    elif soort == "ravijn" and not ronde_oren:
        # twee schuine sprieten (achterover), groen met paarse binnenkant
        for ox in (-3, 1):
            for i in range(11):
                for w in range(K + 1):
                    x = ox * K + w
                    y = Y1 + i
                    z = Z0 + 2 * K + i + 1
                    if meter >= 80 and i >= 9 and w == 0:
                        continue
                    binnen = w == 1 and 1 <= i <= 6
                    vox[(x, y, z)] = (OORROZE if meter >= 60 else P["oorb"]) if binnen else P["spriet"]
    if soort == "ravijn":
        # vinnen achter-opzij
        for s in (-1, 1):
            for i in range(7):
                for j in range(2):
                    x = (X1 + i) if s > 0 else (X0 - 1 - i)
                    vox[(x, Y0 + onderband + 2 + j, Z1 - 4 - i // 2)] = P["vin"] if j == 0 else P["vin2"]
                    vox[(x, Y0 + onderband + 4 + j, Z1 - 3 - i // 2)] = P["vin2"]
    elif soort == "zwam":
        tip = OORROZE if meter >= 60 else P["tip"]
        for (tx, tz, h, haak) in ((-4, -3, 5, 1), (-2, -1, 4, -1), (0, -3, 6, 1), (2, 0, 3, 1), (3, -2, 5, -1),
                                  (-3, 2, 3, 1)):
            staafje([(tx, i, tz) for i in range(h)] + [(tx + haak, h - 1, tz), (tx + haak, h - 2, tz)],
                    P["gewei"], tip)

    # kleine ronde guh-oortjes (80-99) voor soorten zonder Blub-oren, grote ronde oren bij 100
    def rond_oor(cx, cy, cz, r):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            for y in range(int(cy - r) - 1, int(cy + r) + 2):
                dd = math.hypot(x + .5 - cx, y + .5 - cy)
                if dd <= r and y >= Y1 - 1:
                    vox[(x, y, cz)] = OORROZE if dd <= r - K else P["rand"]
                    vox[(x, y, cz + 1)] = P["rand"]
    if ronde_oren:
        for s in (-1, 1):
            rond_oor(s * 4.2 * K, Y1 + 3.2 * K, Z0 + 3 * K, 3.6 * K)
    elif meter >= 80 and soort in ("weide", "schild", "zwam"):
        for s in (-1, 1):
            rond_oor(s * 3.8 * K, Y1 + 1.6 * K, Z0 + 2 * K, 2.0 * K)

    # --- gezicht (voorvlak z = Z0)
    fz = Z0

    def pix(tx, tyb, w, h, kleur):
        for i in range(int(w * K)):
            for j in range(int(h * K)):
                vox[(X1 - 1 - int(tx * K) - i, Y1 - 1 - int(tyb * K) - j, fz)] = kleur

    def guhoog(tx, ty):
        for j, rij in enumerate(GUHOOG):
            for i, ch in enumerate(rij):
                if ch != ".":
                    vox[(X1 - 1 - int(tx * K) - i, Y1 - 1 - int(ty * K) - j, fz)] = GK[ch]

    oy = 3.5 if soort != "zwam" else 4.5          # ooghoogte (texels van boven)
    st = P["streep"]
    if meter < 20:
        pix(1, oy, 2, 1.5, st)
        pix(7, oy, 2, 1.5, st)
        pix(4, oy + 1, 2, 1, st)
        if meter >= 1:
            pix(0.5, oy + 1.5, 1, .5, BLOS)
            pix(8.5, oy + 1.5, 1, .5, BLOS)
    elif meter < 50:                                # (ronde 2: the :3 mouth from 50, not 40)
        guhoog(0.5, oy - 1.5)
        guhoog(6.5, oy - 1.5)
        pix(4, oy + 1, 2, 1, st)
        pix(0.5, oy + 1.5, 1, .5, BLOS)
        pix(8.5, oy + 1.5, 1, .5, BLOS)
    else:
        guhoog(0.5, oy - 1.5)
        guhoog(6.5, oy - 1.5)
        pix(0.5, oy + 1.5, 1.5, .5, BLOS)
        pix(8, oy + 1.5, 1.5, .5, BLOS)
        pix(4.5, oy + 1, 1, .5, NEUS)
        for (tx, ty) in ((3.5, oy + 1.5), (4, oy + 2), (4.5, oy + 1.5), (5, oy + 2), (5.5, oy + 1.5)):
            pix(tx, ty, .5, .5, MOND)
    return vox, P




# =====================================================================================================================
# the GeckoLib model: cubes whose faces are sampled from the voxels
# =====================================================================================================================
S = 2                       # texels per model pixel (= voxels per pixel: one texel per voxel)
ATLAS = 80                  # atlas size in model pixels (the texture is ATLAS * S = 160 square)
STAPPEN = ["p", "b"] + [str(m) for m in range(20, 101, 5)]
STAP_METER = {"p": 0, "b": 10, **{str(m): m for m in range(20, 101, 5)}}
FACES = ("north", "south", "east", "west", "up", "down")
WIT = (255, 255, 255)


def stap_meter(stap):
    return STAP_METER[stap]


class Layout:
    """Every cube face gets its own patch (same order every time, so every texture shares one layout)."""

    def __init__(self):
        self.x = self.y = self.row = 0
        self.patches = []          # (u, v, w, h, (origin, size, face, rol, leeg, diep))

    def alloc(self, w, h):
        w, h = int(math.ceil(w)), int(math.ceil(h))
        if self.x + w > ATLAS:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        if self.y + h > ATLAS:
            raise SystemExit("blub_modellen: the texture atlas is full")
        u, v = self.x, self.y
        self.x += w
        self.row = max(self.row, h)
        return u, v


def face_size(face, size):
    sx, sy, sz = size
    return {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}[face]


def _voxel(origin, size, face, col, row):
    """The voxel just inside this cube face under texel (col, row), and the way further in (Bedrock face orientation:
    north seen from the front has +x on its left, side faces have the top of the cube at the top of the patch)."""
    ox, oy, oz = origin
    sx, sy, sz = size
    c, r = (col + 0.5) / S, (row + 0.5) / S
    if face == "north":
        p, n = (ox + sx - c, oy + sy - r, oz + 0.25), (0, 0, 1)
    elif face == "south":
        p, n = (ox + c, oy + sy - r, oz + sz - 0.25), (0, 0, -1)
    elif face == "east":
        p, n = (ox + sx - 0.25, oy + sy - r, oz + sz - c), (-1, 0, 0)
    elif face == "west":
        p, n = (ox + 0.25, oy + sy - r, oz + c), (1, 0, 0)
    elif face == "up":
        p, n = (ox + c, oy + sy - 0.25, oz + r), (0, -1, 0)
    else:
        p, n = (ox + c, oy + 0.25, oz + sz - r), (0, 1, 0)
    return tuple(math.floor(v * K) for v in p), n


def cube(lay, origin, size, rol, leeg="vul", pivot=None, rotation=None, diep=6, alleen=None):
    """A cube; every face samples the voxels of a variant. rol: the palette colour (name) for voxels it can't find, or a
    painter (P, meter, col, row, W, H, face) -> colour or None (None: sample the voxels). leeg='doorzichtig': missing
    voxels are see-through (round ears, cut ear tips). alleen: only these faces are painted (the rest see-through)."""
    uv = {}
    for f in FACES:
        w, h = face_size(f, size)
        w, h = max(w, 0.5), max(h, 0.5)
        u, v = lay.alloc(w, h)
        lay.patches.append((u, v, w, h, (tuple(origin), tuple(size), f, rol, leeg, diep) if alleen is None or f in alleen
                            else (tuple(origin), tuple(size), f, lambda *a: None, leeg, diep)))
        uv[f] = {"uv": [u, v], "uv_size": [w, h]}
    c = {"origin": origin, "size": size, "uv": uv}
    if rotation:
        c["pivot"], c["rotation"] = pivot, rotation
    return c


def bone(name, parent, pivot, cubes, rotation=None):
    b = {"name": name, "pivot": pivot, "cubes": cubes}
    if parent:
        b["parent"] = parent
    if rotation:
        b["rotation"] = rotation
    return b


def _paint_patch(img, patch, vox, P, meter):
    u, v, w, h, (origin, size, face, rol, leeg, diep) = patch
    W, H = int(math.ceil(w)) * S, int(math.ceil(h)) * S
    for row in range(H):
        for col in range(W):
            kleur = rol(P, meter, col, row, W, H, face) if callable(rol) else None
            if kleur is None and not callable(rol):
                (x, y, z), n = _voxel(origin, size, face, col, row)
                for k in range(diep):
                    kleur = vox.get((x + n[0] * k, y + n[1] * k, z + n[2] * k))
                    if kleur is not None:
                        break
                if kleur is None and leeg != "doorzichtig":
                    kleur = P.get(rol) if isinstance(rol, str) else rol
            if kleur is None:
                continue
            img[v * S + row, u * S + col, :3] = kleur[:3]
            img[v * S + row, u * S + col, 3] = 255


# --- painters for the cubes that stand at an angle (no voxels to sample) ---------------------------------------------------
def vleugel(P, meter, col, row, W, H, face):
    return P.get("vleugel", P["rand"]) if (col + row) % 5 else P.get("vleugel2", P["rand"])


def ravijn_spriet(P, meter, col, row, W, H, face):
    if face in ("north", "south") and col == W // 2 and 2 <= row <= H * 0.6:
        return OORROZE if meter >= 60 else P.get("oorb", P["rand"])
    return P.get("spriet", P["rand"])


def vin(P, meter, col, row, W, H, face):
    return P.get("vin", P["rand"]) if row < H / 2 else P.get("vin2", P["rand"])


def strik(P, meter, col, row, W, H, face):
    """White with a soft shade (the colour comes from the render pass)."""
    return (226, 226, 226) if (row == 0 or col == 0) else WIT


BENEN = ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right")


def model():
    """The one model of every Blub-guh. Front = north (-z), left = +x; 1 unit = 1 model pixel, the floor is y = 0."""
    lay = Layout()
    B0, B1 = 2.0, 9.5                      # body bottom and top (the legs are 2 high)
    bones = [bone("root", None, [0, 0, 0], [])]
    bones.append(bone("lijf", "root", [0, B0, 0], [cube(lay, [-5, B0, -5], [10, 7.5, 10], "kern")]))
    # the soft body (meter 60+): three crossing boxes leave the edges half a pixel rounder (like the renders)
    bones.append(bone("lijf_zacht", "root", [0, B0, 0], [
        cube(lay, [-5, B0 + 0.5, -4.5], [10, 6.5, 9], "kern"),
        cube(lay, [-4.5, B0, -4.5], [9, 7.5, 9], "kern"),
        cube(lay, [-4.5, B0 + 0.5, -5], [9, 6.5, 10], "kern")]))
    for name, x, z in zip(BENEN, (2, -5, 2, -5), (-5, -5, 2, 2)):
        bones.append(bone(name, "root", [x + 1.5, B0, z + 1.5], [cube(lay, [x, 0, z], [3, 2, 3], "poot")]))
    # the Blub ears (blauw); the round tips at 80+ are see-through texture
    for name, x in (("oor_l", 1), ("oor_r", -4)):
        bones.append(bone(name, "root", [x + 1.5, B1, -3.5], [cube(lay, [x, B1, -4], [3, 4, 1], "oor", leeg="doorzichtig")]))
    # small round guh ears (80-99: weide, schildje, zwam) and the big round guh ears (100: every soort)
    for name, cx in (("guhoortje_l", 3.8), ("guhoortje_r", -3.8)):
        bones.append(bone(name, "root", [cx, B1, -2.5], [cube(lay, [cx - 2, 9.0, -3], [4, 4.2, 1], "rand", leeg="doorzichtig", diep=2,
                                                                               alleen=("north", "south"))]))
    for name, cx in (("guhoor_l", 4.2), ("guhoor_r", -4.2)):
        bones.append(bone(name, "root", [cx, B1, -1.5], [cube(lay, [cx - 3.6, 9.0, -2], [7.2, 7.4, 1], "rand", leeg="doorzichtig", diep=2,
                                                                                 alleen=("north", "south"))]))
    # weide: four antennae, the fishing rod and its lamp
    bones.append(bone("weide_sprieten", "root", [0, B1, 0], [cube(lay, [tx, B1, tz], [1, hh, 1], "spriet")
                                                              for tx, tz, hh in ((-3.5, -3.5, 4), (-1.5, -2.5, 5), (0.5, -3.5, 3), (-2.5, -0.5, 3))]))
    hengel = [(2, 0, 4), (2, 1, 4), (2, 2, 4.5), (2, 3, 5), (2, 4, 5.5), (2, 5, 6), (2, 6, 6.5), (2, 7, 7)]
    bones.append(bone("weide_hengel", "root", [2.5, B1, 4.5], [cube(lay, [tx, B1 + ty, tz], [1, 1, 1], "lamp") for tx, ty, tz in hengel]
                      + [cube(lay, [0.5, B1 + 8, 5.0], [4, 4, 4.5], "lamp")]))
    # schildje: the red wings (at an angle) and three antennae
    bones.append(bone("schild_vleugel_l", "root", [5.2, B1, -1.5], [cube(lay, [5, 3, -3], [0.5, 6.5, 3], vleugel, pivot=[5.2, B1, -1.5],
                                                                          rotation=[-22, 0, 16])]))
    bones.append(bone("schild_vleugel_r", "root", [-5.2, B1, -1.5], [cube(lay, [-5.5, 3, -3], [0.5, 6.5, 3], vleugel, pivot=[-5.2, B1, -1.5],
                                                                           rotation=[-22, 0, -16])]))
    bones.append(bone("schild_sprieten", "root", [0, B1, 3], [cube(lay, [tx, B1, tz], [1, hh, 1], "spriet")
                                                              for tx, tz, hh in ((-2, 3, 3), (0, 4, 4), (2, 3, 3))]))
    # ravijn: two antennae leaning back, two fins on each side
    for name, ox in (("ravijn_spriet_l", 1), ("ravijn_spriet_r", -3)):
        bones.append(bone(name, "root", [ox + 0.75, B1, -2.5], [cube(lay, [ox, B1, -2.75], [1.5, 7.5, 0.5], ravijn_spriet,
                                                                     pivot=[ox + 0.75, B1, -2.5], rotation=[-45, 0, 0])]))
    for name, sx in (("ravijn_vin_l", 1), ("ravijn_vin_r", -1)):
        x0, x1 = (5, 5) if sx > 0 else (-8.5, -8.0)
        bones.append(bone(name, "root", [5 * sx, 6.5, 2.5], [
            cube(lay, [x0, 6, 1.5], [3.5, 1, 0.5], vin, pivot=[5 * sx, 6.5, 2.5], rotation=[0, -18 * sx, 0]),
            cube(lay, [x1, 7, 2.0], [3.0, 0.75, 0.5], vin, pivot=[5 * sx, 6.5, 2.5], rotation=[0, -18 * sx, 0])]))
    # zwam: the antlers with their little hooks
    gewei = []
    for tx, tz, hh, haak in ((-4, -3, 5, 1), (-2, -1, 4, -1), (0, -3, 6, 1), (2, 0, 3, 1), (3, -2, 5, -1), (-3, 2, 3, 1)):
        gewei.append(cube(lay, [tx, B1, tz], [1, hh, 1], "gewei"))
        gewei.append(cube(lay, [tx + haak, B1 + hh - 2, tz], [1, 2, 1], "gewei"))
    bones.append(bone("zwam_gewei", "root", [0, B1, 0], gewei))
    # the strikje (its own tinted pass)
    bones.append(bone("strik", "root", [0, B1, -4.3], [
        cube(lay, [-2.1, B1, -4.6], [1.6, 1.3, 0.6], strik), cube(lay, [0.5, B1, -4.6], [1.6, 1.3, 0.6], strik),
        cube(lay, [-0.55, B1 + 0.1, -4.7], [1.1, 1.1, 0.8], strik)]))
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.blubguh", "texture_width": ATLAS, "texture_height": ATLAS,
                        "visible_bounds_width": 1.6, "visible_bounds_height": 1.8, "visible_bounds_offset": [0, 0.9, 0]},
        "bones": bones}]}
    return geo, lay


def textuur(lay, soort, tint, stap):
    meter = stap_meter(stap)
    vox, P = model_vox(meter, soort, tint)
    img = np.zeros((ATLAS * S, ATLAS * S, 4), np.uint8)
    for patch in lay.patches:
        _paint_patch(img, patch, vox, P, meter)
    return Image.fromarray(img)


# (the first 6 * 8 patches: lijf, the three lijf_zacht boxes and the four legs)
LIJF_PATCHES = 6 * 8


def gloed(lay):
    """The blue Blub's soft glow: its body and legs at a third of their light (not the face side, ears or strikje)."""
    vox, P = model_vox(20, "blauw", 0)
    img = np.zeros((ATLAS * S, ATLAS * S, 4), np.uint8)
    for patch in lay.patches[:LIJF_PATCHES]:
        if patch[4][2] == "north":
            continue
        _paint_patch(img, patch, vox, P, 20)
    img[..., :3] = (img[..., :3].astype(np.float32) * 0.33).astype(np.uint8)
    return Image.fromarray(img)


def kf(pairs):
    return {str(round(t, 3)): v for t, v in pairs}


def anim(length, bones, loop=True):
    return {"loop": loop, "animation_length": length, "bones": bones}


def animaties():
    idle = {"root": {"scale": kf([(0, [1, 1, 1]), (1.5, [1.02, 0.97, 1.02]), (3, [1, 1, 1])])},
            "oor_l": {"rotation": kf([(0, [0, 0, 0]), (2.2, [0, 0, 0]), (2.3, [-14, 0, 0]), (2.45, [0, 0, 0])])}}
    L = 0.5    # one hop
    walk = {"root": {"position": kf([(0, [0, 0, 0]), (L * 0.4, [0, 2.2, 0]), (L * 0.8, [0, 0.2, 0]), (L, [0, 0, 0])]),
                     "scale": kf([(0, [1.06, 0.92, 1.06]), (L * 0.2, [0.96, 1.06, 0.96]), (L * 0.8, [1, 1, 1]), (L, [1.06, 0.92, 1.06])])}}
    for i, n in enumerate(BENEN):
        s = 30 if i < 2 else -30
        walk[n] = {"rotation": kf([(0, [0, 0, 0]), (L * 0.4, [s, 0, 0]), (L * 0.8, [0, 0, 0])])}
    zit = {"root": {"position": [0, -1.0, 0]}, **{n: {"scale": [1, 0.5, 1]} for n in BENEN}}
    slaap = {"root": {"position": [0, -1.4, 0], "scale": kf([(0, [1.04, 0.92, 1.04]), (1.6, [1.07, 0.88, 1.07]), (3.2, [1.04, 0.92, 1.04])])},
             **{n: {"scale": [1, 0.3, 1]} for n in BENEN},
             "oor_l": {"rotation": [40, 0, 0]}, "oor_r": {"rotation": [40, 0, 0]}}
    blij = {"root": {"position": kf([(0, [0, 0, 0]), (0.15, [0, 2.5, 0]), (0.3, [0, 0, 0]), (0.45, [0, 1.5, 0]), (0.6, [0, 0, 0])])}}
    eet = {"root": {"rotation": kf([(0, [0, 0, 0]), (0.2, [10, 0, 0]), (0.35, [4, 0, 0]), (0.5, [10, 0, 0]), (0.8, [0, 0, 0])])}}
    squish = {"root": {"scale": kf([(0, [1, 1, 1]), (0.12, [1.35, 0.55, 1.35]), (0.3, [1.3, 0.6, 1.3]), (0.45, [0.9, 1.15, 0.9]),
                                    (0.6, [1.05, 0.96, 1.05]), (0.75, [1, 1, 1])])}}
    rondje = {"root": {"rotation": kf([(0, [0, 0, 0]), (0.8, [0, 360, 0])]),
                       "position": kf([(0, [0, 0, 0]), (0.2, [0, 1.5, 0]), (0.4, [0, 0, 0]), (0.6, [0, 1.5, 0]), (0.8, [0, 0, 0])])}}
    dood = {"root": {"rotation": kf([(0, [0, 0, 0]), (0.3, [0, 0, 95]), (0.4, [0, 0, 88]), (2.2, [0, 0, 90]), (2.5, [0, 0, 0])]),
                     "position": kf([(0, [0, 0, 0]), (0.3, [0, 0.5, 0]), (2.2, [0, 0.5, 0]), (2.35, [0, 3, 0]), (2.5, [0, 0, 0])])},
            **{n: {"rotation": kf([(0.4, [0, 0, 0]), (0.6, [0, 0, 25]), (0.8, [0, 0, -10]), (1.0, [0, 0, 0])])} for n in BENEN}}
    sprong = {"root": {"rotation": kf([(0, [0, 0, 0]), (0.25, [-20, 0, 0]), (0.5, [-360, 0, 0]), (0.7, [-360, 0, 0])]),
                       "scale": kf([(0, [1.2, 0.8, 1.2]), (0.15, [0.9, 1.15, 0.9]), (0.55, [1, 1, 1]), (0.62, [1.2, 0.8, 1.2]), (0.7, [1, 1, 1])])}}
    schud = {"root": {"rotation": kf([(0, [0, 0, 0]), (0.1, [0, 0, 14]), (0.2, [0, 0, -14]), (0.3, [0, 0, 12]), (0.4, [0, 0, -12]),
                                      (0.5, [0, 0, 8]), (0.6, [0, 0, -6]), (0.7, [0, 0, 0])])},
             "oor_l": {"rotation": kf([(0, [0, 0, 0]), (0.15, [0, 0, 30]), (0.3, [0, 0, -20]), (0.5, [0, 0, 10]), (0.7, [0, 0, 0])])},
             "oor_r": {"rotation": kf([(0, [0, 0, 0]), (0.15, [0, 0, -30]), (0.3, [0, 0, 20]), (0.5, [0, 0, -10]), (0.7, [0, 0, 0])])}}
    return {"format_version": "1.8.0", "animations": {
        "idle": anim(3.0, idle), "walk": anim(L, walk), "zit": anim(2.0, zit), "slaap": anim(3.2, slaap),
        "blij": anim(0.6, blij, False), "eet": anim(0.8, eet, False), "squish": anim(0.75, squish, False),
        "rondje": anim(0.8, rondje, False), "dood": anim(2.5, dood, False), "sprong": anim(0.7, sprong, False),
        "uitschudden": anim(0.7, schud, False)}}


# the bones BlubClient switches (keep in sync with feature/blub/client/BlubClient.java)
SCHAKEL = ["lijf", "lijf_zacht", "oor_l", "oor_r", "guhoortje_l", "guhoortje_r", "guhoor_l", "guhoor_r", "weide_sprieten", "weide_hengel",
           "schild_vleugel_l", "schild_vleugel_r", "schild_sprieten", "ravijn_spriet_l", "ravijn_spriet_r", "ravijn_vin_l", "ravijn_vin_r",
           "zwam_gewei", "strik"]
ANIMS = ("idle", "walk", "zit", "slaap", "blij", "eet", "squish", "rondje", "dood", "sprong", "uitschudden")


def build(h):
    A = h.A
    geo, lay = model()
    h.w(f"{A}/geckolib/models/entity/blubguh.geo.json", geo)
    h.w(f"{A}/geckolib/animations/entity/blubguh.animation.json", animaties())
    for soort in SOORTEN:
        for tint in range(3):
            for stap in STAPPEN:
                h.save(textuur(lay, soort, tint, stap), "entity", "blubguh", f"{soort}_{tint}_{stap}.png")
    h.save(textuur(lay, "blauw", 0, "40"), "entity", "blubguh.png")          # (the default texture)
    h.save(gloed(lay), "entity", "blubguh", "blauw_gloed.png")
    import json
    import os
    with open(os.path.join("blockbench", "BlubGuh.bbmodel"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(bbmodel(geo, textuur(lay, "blauw", 0, "40"), animaties()), f)
    return geo, lay


def check(h):
    import json
    problems = []
    g = json.load(open(f"{h.A}/geckolib/models/entity/blubguh.geo.json", encoding="utf-8"))["minecraft:geometry"][0]
    names = {b["name"] for b in g["bones"]}
    for b in SCHAKEL:
        if b not in names:
            problems.append(f"blubguh: no bone {b}")
    anims = json.load(open(f"{h.A}/geckolib/animations/entity/blubguh.animation.json", encoding="utf-8"))["animations"]
    for an in ANIMS:
        if an not in anims:
            problems.append(f"blubguh: no animation {an}")
    for an, a in anims.items():
        for bn in a.get("bones", {}):
            if bn not in names:
                problems.append(f"blubguh: animation {an} moves a missing bone {bn}")
    return problems


def bbmodel(geo, textuur_png, anims, naam="BlubGuh"):
    """A Blockbench project (bedrock entity) of the model, one texture and the animations, to look at or tweak by hand.
    (The game files come from this generator, not from the .bbmodel: hand edits have to be copied back here.)"""
    import base64
    import io
    import uuid
    g = geo["minecraft:geometry"][0]
    rnd = __import__("random").Random(1808)

    def uid():
        return str(uuid.UUID(int=rnd.getrandbits(128), version=4))
    buf = io.BytesIO()
    textuur_png.save(buf, "PNG")
    elements, groepen = [], {}
    for b in g["bones"]:
        px, py, pz = b["pivot"]
        groepen[b["name"]] = {"name": b["name"], "origin": [-px, py, pz], "rotation": [-v for v in b.get("rotation", [0, 0, 0])[:2]]
                              + [b.get("rotation", [0, 0, 0])[2]], "uuid": uid(), "export": True, "isOpen": False, "visibility": True,
                              "children": [], "parent": b.get("parent")}
        for c in b.get("cubes", []):
            (ox, oy, oz), (sx, sy, sz) = c["origin"], c["size"]
            rot = c.get("rotation", [0, 0, 0])
            piv = c.get("pivot", [ox + sx / 2, oy + sy / 2, oz + sz / 2])
            e = {"name": b["name"], "type": "cube", "uuid": uid(), "box_uv": False, "rescale": False, "locked": False,
                 "from": [-(ox + sx), oy, oz], "to": [-ox, oy + sy, oz + sz], "origin": [-piv[0], piv[1], piv[2]],
                 "rotation": [-rot[0], -rot[1], rot[2]], "autouv": 0, "color": 0,
                 "faces": {f: {"uv": [d["uv"][0], d["uv"][1], d["uv"][0] + d["uv_size"][0], d["uv"][1] + d["uv_size"][1]], "texture": 0}
                           for f, d in c["uv"].items()}}
            elements.append(e)
            groepen[b["name"]]["children"].append(e["uuid"])
    outliner = []
    for name, grp in groepen.items():
        parent = grp.pop("parent")
        (groepen[parent]["children"] if parent else outliner).append(grp)
    animaties_bb = []
    for an, a in anims["animations"].items():
        animators = {}
        for bn, kanalen in a["bones"].items():
            kfs = []
            for kanaal, waarde in kanalen.items():
                punten = waarde.items() if isinstance(waarde, dict) else [("0", waarde)]
                for t, v in punten:
                    kfs.append({"channel": kanaal, "data_points": [{"x": str(v[0]), "y": str(v[1]), "z": str(v[2])}],
                                "uuid": uid(), "time": float(t), "interpolation": "linear"})
            animators[groepen[bn]["uuid"]] = {"name": bn, "type": "bone", "keyframes": kfs}
        animaties_bb.append({"uuid": uid(), "name": an, "loop": "loop" if a["loop"] else "once", "length": a["animation_length"],
                             "snapping": 24, "animators": animators})
    return {"meta": {"format_version": "4.10", "model_format": "bedrock", "box_uv": False}, "name": naam,
            "model_identifier": "blubguh", "visible_box": [1.6, 1.8, 0.9],
            "resolution": {"width": ATLAS, "height": ATLAS}, "elements": elements, "outliner": outliner,
            "textures": [{"name": "blubguh.png", "id": "0", "width": ATLAS * S, "height": ATLAS * S, "uv_width": ATLAS, "uv_height": ATLAS,
                          "uuid": uid(), "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()}],
            "animations": animaties_bb}
