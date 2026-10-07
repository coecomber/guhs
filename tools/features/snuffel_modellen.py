"""
Het Snuffeleiland - the looks (approval round): ONE parametric guh-like DOG, its breeds, coats, puppies and the island's
residents (breed + coat + accessories), three concepts for the invisible companion and its little tree in four steps.

De hond: clearly a dog (four legs, a real snout with a dog nose, dog ears, a tail, a collar) with the mod's guh traits (the
big glossy guh eyes, pink blush, chubby cheeks, soft noisy fur). Everything is ONE builder, `hond(atlas, ras, vacht, ...)`:
  RASSEN   a breed = numbers only (body length/width/height, leg length, head, snout, ear type, tail type, pattern)
  VACHTEN  a coat = a pattern name + colours; patterns are 3D rules ("light below this line", "a saddle patch here"), so
           they wrap around the cubes without seams and fit every body shape
  EXTRA'S  accessories = little add-on cube sets that place themselves from the breed's numbers (glasses, coats, caps...)
  BEWONERS a resident = breed + coat + accessories + expression: one line each, no modelling
Het maatje (three concepts A/B/C, each with a happy and a naughty face) and het boompje (kiem, scheutje, struikje, jong
boompje) are small models of their own.

Textures are painted face by face on an atlas (4 texels per model pixel, like boerderij_dieren), but here every texel knows
its place in the model (x, y, z), which is what lets one pattern serve every breed.

  maak()            -> ({"models/entity/<x>.geo.json": .., "animations/entity/<x>.animation.json": ..}, {"<tex>": RGBA array})
  build(h)          writes those under assets/guhs/geckolib and textures/entity (NOT wired into features/__init__ yet: the
                    looks wait for the author's approval; nothing in the game uses them)
  check(h)          bones/animations sanity (works on maak(), needs no files)
  platen(out)       (python tools/features/snuffel_modellen.py <out>) the approval pictures 01..07
Bone names: root, lijf, kop, snuit, oor_links, oor_rechts, staart, poot_lv, poot_rv, poot_la, poot_ra (+ x_<accessory>).
Animations of every dog: idle, walk, snuffel, zit, kwispel.
"""
import copy
import json
import math
import os
import sys
import tempfile
from types import SimpleNamespace as NS

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

S = 4                       # texels per model pixel
FACES = ("north", "south", "east", "west", "up", "down")


# =====================================================================================================================
# atlas + cubes whose texels know where they are
# =====================================================================================================================
class Atlas:
    def __init__(self, seed, maat=128):
        self.maat = maat
        self.img = np.zeros((maat * S, maat * S, 4), np.uint8)
        self.x = self.y = self.row = 0
        self.rng = np.random.default_rng(seed)

    def alloc(self, w, h):
        w, h = int(math.ceil(w)), int(math.ceil(h))
        if self.x + w > self.maat:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        if self.y + h > self.maat:
            raise SystemExit("snuffel_modellen: the texture atlas is full")
        u, v = self.x, self.y
        self.x += w
        self.row = max(self.row, h)
        return u, v


def _vlak(face, size):
    sx, sy, sz = size
    return {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}[face]


def _plek(face, origin, size, W, H):
    """Model coordinates (X, Y, Z arrays, H x W) of the texels of one cube face (the mapping of wiki_renders.geo_quads)."""
    (x0, y0, z0), (w, h, d) = origin, size
    fw, fh = _vlak(face, size)
    A, B = np.meshgrid(np.clip((np.arange(W) + 0.5) / (max(fw, 1e-6) * S), 0, 1), np.clip((np.arange(H) + 0.5) / (max(fh, 1e-6) * S), 0, 1))
    one = np.ones_like(A)
    if face == "north":
        return x0 + w - A * w, y0 + h - B * h, one * z0
    if face == "south":
        return x0 + A * w, y0 + h - B * h, one * (z0 + d)
    if face == "east":
        return one * (x0 + w), y0 + h - B * h, z0 + d - A * d
    if face == "west":
        return one * x0, y0 + h - B * h, z0 + A * d
    if face == "up":
        return x0 + w - A * w, one * (y0 + h), z0 + B * d
    return x0 + w - A * w, one * y0, z0 + d - B * d


def blok(atlas, origin, size, kleur, var=8, inflate=None, alleen=None):
    """A cube; kleur(X, Y, Z, face) -> rgb (H, W, 3) | rgba (H, W, 4) | (rgb[a], glad-mask) paints every face in model
    space. Fur noise (var) is added except where `glad` is set (eyes, noses). alleen = the faces to keep (default all)."""
    origin, size = [float(v) for v in origin], [float(v) for v in size]
    uv = {}
    for f in FACES:
        if alleen and f not in alleen:
            continue
        w, h = _vlak(f, size)
        w, h = max(w, 0.5), max(h, 0.5)
        u, v = atlas.alloc(w, h)
        W, H = int(math.ceil(w)) * S, int(math.ceil(h)) * S
        X, Y, Z = _plek(f, origin, size, W, H)
        out = kleur(X, Y, Z, f)
        glad = None
        if isinstance(out, tuple):
            out, glad = out
        out = np.asarray(out, np.float32)
        if out.ndim == 1:
            out = np.broadcast_to(out, (H, W, out.shape[0])).copy()
        a = np.zeros((H, W, 4), np.float32)
        a[..., :3] = out[..., :3]
        a[..., 3] = out[..., 3] if out.shape[-1] == 4 else 255
        rng = atlas.rng
        ruis = rng.normal(0, var / 2, (H, W, 1)) + rng.normal(0, var / 3, ((H + 1) // 2, (W + 1) // 2, 1)).repeat(2, 0).repeat(2, 1)[:H, :W]
        if glad is not None:
            ruis = ruis * (1 - np.asarray(glad, np.float32))[..., None]
        a[..., :3] += ruis
        atlas.img[v * S:v * S + H, u * S:u * S + W] = np.clip(a, 0, 255).astype(np.uint8)
        uv[f] = {"uv": [u, v], "uv_size": [round(w, 3), round(h, 3)]}
    c = {"origin": [round(v, 3) for v in origin], "size": [round(v, 3) for v in size], "uv": uv}
    if inflate:
        c["inflate"] = inflate
    return c


def bot(name, parent, pivot, cubes, rotation=None):
    b = {"name": name, "pivot": [round(float(v), 3) for v in pivot], "cubes": cubes}
    if parent:
        b["parent"] = parent
    if rotation:
        b["rotation"] = rotation
    return b


def geo(identifier, bones, width, height, maat=128):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{identifier}", "texture_width": maat, "texture_height": maat,
                        "visible_bounds_width": width, "visible_bounds_height": height, "visible_bounds_offset": [0, height / 2, 0]},
        "bones": bones}]}


def spiegel(c):
    """The cube mirrored to the other side (x -> -x); it keeps its own painted patches (fine for symmetric paint)."""
    m = copy.deepcopy(c)
    m["origin"][0] = round(-(c["origin"][0] + c["size"][0]), 3)
    return m


def effen(kleur):
    k = np.array(kleur[:3], np.float32)
    return lambda X, Y, Z, f: np.broadcast_to(k, X.shape + (3,)).copy()


def leg(rgb, mask, kleur, alpha=1.0):
    """Lay a colour over rgb where mask (bool or 0..1 float)."""
    m = (np.asarray(mask, np.float32) * alpha)[..., None]
    return rgb * (1 - m) + np.array(kleur[:3], np.float32) * m


def tint(kleur, f):
    return tuple(max(0, min(255, int(c * f))) for c in kleur[:3])


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


# =====================================================================================================================
# the guh eye (the mod's: outline, dark pupil, a blue -> teal ring below, two white highlights) and its moods
# =====================================================================================================================
OOG_RAND = (22, 16, 34)


def oog(rgb, glad, X, Y, ex, ey, r, kant, stemming="open", lid=(0, 0, 0), rand=OOG_RAND):
    """Paint one eye at (ex, ey) (model px, on a north face), radius r. kant = +1 / -1 (which side of the face).
    stemming: open | wijs (a calm heavy lid) | ondeugend (a sly slanted lid) | blij (wide, an extra sparkle)."""
    dx, dy = (ex - X) / r, (ey - Y) / (r * 1.08)            # image right = -x, image down = -y (as boerderij_dieren.guh_face)
    d = np.hypot(dx, dy)
    binnen = d <= 1
    omtrek = binnen & ((d > 0.86) | ((dy < -0.55) & (d > 0.78)))
    ring = binnen & ~omtrek & (dy > -0.05) & (d > 0.5)
    t = np.clip((dy + 0.05) / 0.9, 0, 1)[..., None]
    rgb = leg(rgb, binnen, (18, 18, 34))
    rgb = np.where(ring[..., None], np.concatenate([40 + 50 * t, 100 + 110 * t, 200 + 20 * t], -1), rgb)
    rgb = leg(rgb, omtrek, rand)
    rgb = leg(rgb, np.hypot(dx - 0.22, dy + 0.32) <= 0.3, (255, 255, 255))
    rgb = leg(rgb, np.hypot(dx + 0.32, dy - 0.28) <= 0.13, (226, 232, 240))
    if stemming == "blij":
        rgb = leg(rgb, np.hypot(dx + 0.12, dy + 0.5) <= 0.1, (255, 255, 255))
    if stemming in ("wijs", "ondeugend"):
        schuin = dx * kant                                   # > 0 towards the middle of the face (kant: +1 = eye at +x = image left)
        grens = -0.38 + (0.0 if stemming == "wijs" else 0.42 * schuin + 0.18)
        dicht = binnen & (dy < grens)
        rgb = leg(rgb, dicht, lid)
        rgb = leg(rgb, binnen & (dy >= grens) & (dy < grens + 0.2), rand)
    glad = np.maximum(glad, binnen.astype(np.float32))
    return rgb, glad


def blos(rgb, X, Y, ex, ey, r, kant, sterkte=0.55, kleur=(255, 150, 186)):
    m = (((ex + kant * 0.25 * r - X) / (0.66 * r)) ** 2 + ((ey - 1.45 * r - Y) / (0.34 * r)) ** 2) <= 1
    return leg(rgb, m, kleur, sterkte)


# =====================================================================================================================
# DE HOND: breeds (numbers), coats (patterns + colours)
# =====================================================================================================================
BASIS = dict(lijf=(8, 7, 11), poot=(4, 2.6), kop=(11, 9, 8), kop_zak=3.5, kop_in=2.6, snuit=(4.6, 3.4, 2.6), oren=("punt", 3.2, 4.0),
             staart="krul", wang=True, borst=False, rimpel=False, lippen=False, oog=(2.75, 0.60, 1.75), broek=False, schaal=0.8)

RASSEN = {
    "shiba": dict(naam="Shiba", borst=True),
    "jackrussell": dict(naam="Jack russell", lijf=(7, 6.4, 10.5), poot=(5, 2.3), kop=(10, 8.6, 7.6), snuit=(4.2, 3.2, 3.0),
                        oren=("knak", 3.0, 1.7), staart="recht", wang=False, oog=(2.55, 0.60, 1.65)),
    "teckel": dict(naam="Teckel", lijf=(7.6, 6.6, 19), poot=(2.3, 2.6), kop=(10, 8.6, 7.6), kop_zak=3.2, snuit=(4.0, 3.2, 4.4),
                   oren=("hang", 4.4, 8.6), staart="lang", wang=False, oog=(2.5, 0.60, 1.65)),
    "corgi": dict(naam="Corgi", lijf=(8.6, 7.4, 14), poot=(2.5, 2.8), kop=(11.6, 9, 8), snuit=(4.6, 3.4, 3.0),
                  oren=("groot", 4.2, 5.8), staart="stomp", broek=True, borst=True),
    "golden": dict(naam="Golden retriever", lijf=(9.6, 8.6, 14.5), poot=(6, 3.0), kop=(11.6, 9.6, 8.6), snuit=(5.0, 3.8, 3.8),
                   oren=("hang", 4.6, 6.6), staart="pluim", borst=True, oog=(2.9, 0.60, 1.8)),
    "mops": dict(naam="Mopshond", lijf=(9.6, 8, 10), poot=(3.2, 2.9), kop=(12.4, 9.6, 8), kop_zak=4.2, snuit=(5.8, 3.6, 1.3),
                 oren=("roos", 2.8, 1.4), staart="krulletje", rimpel=True, oog=(3.35, 0.58, 1.95)),
    # not for the player: the meadow trainer's breed, to show what the base does with a seventh set of numbers
    "speurhond": dict(naam="Speurhond", lijf=(9.6, 8.6, 15), poot=(5, 3.2), kop=(11.6, 10, 8.6), kop_zak=3.4, snuit=(5.6, 4.2, 4.2),
                      oren=("hang", 4.8, 10.6), staart="lang", wang=False, rimpel=True, lippen=True, oog=(2.95, 0.63, 1.7)),
}

NEUS = (38, 30, 40)
V = lambda patroon, **k: dict(patroon=patroon, **k)
VACHTEN = {
    "shiba": {
        "rood": V("urajiro", naam="rood", basis=(232, 142, 72), licht=(255, 238, 214), oor=(236, 150, 150), wenk=(255, 232, 200)),
        "zwart": V("urajiro", naam="zwart-tan", basis=(58, 52, 62), licht=(250, 236, 216), tan=(214, 142, 80), oor=(214, 142, 80), wenk=(226, 158, 92)),
        "creme": V("urajiro", naam="crème", basis=(250, 226, 190), licht=(255, 248, 236), oor=(244, 170, 170), wenk=(255, 250, 240)),
    },
    "jackrussell": {
        "bruin": V("vlekken", naam="wit met bruin", basis=(252, 248, 240), vlek=(196, 122, 66), oor=(236, 160, 160)),
        "zwart": V("vlekken", naam="wit met zwart", basis=(252, 248, 240), vlek=(60, 54, 62), oor=(236, 160, 160)),
        "driekleur": V("vlekken", naam="driekleur", basis=(252, 248, 240), vlek=(60, 54, 62), tan=(206, 136, 76), oor=(236, 160, 160)),
    },
    "teckel": {
        "rood": V("effen", naam="rood", basis=(190, 104, 58), licht=(214, 134, 80), oor=(160, 82, 46), donker=(160, 82, 46)),
        "zwart": V("tan", naam="zwart-tan", basis=(54, 48, 56), tan=(208, 136, 72), oor=(70, 62, 72), wenk=(208, 136, 72)),
        "choco": V("tan", naam="chocola-tan", basis=(122, 78, 56), tan=(226, 170, 110), oor=(96, 58, 42), wenk=(226, 170, 110), neus=(92, 58, 50)),
    },
    "corgi": {
        "rood": V("corgi", naam="rood-wit", basis=(236, 156, 78), licht=(255, 250, 240), oor=(240, 164, 160)),
        "sable": V("corgi", naam="sable", basis=(206, 140, 84), licht=(255, 248, 236), rug=(128, 92, 70), oor=(236, 160, 160)),
        "driekleur": V("corgi", naam="driekleur", basis=(222, 148, 82), licht=(255, 250, 240), rug=(56, 50, 58), oor=(236, 160, 160)),
    },
    "golden": {
        "goud": V("effen", naam="goud", basis=(236, 178, 96), licht=(252, 220, 156), oor=(214, 152, 74), donker=(214, 152, 74)),
        "licht": V("effen", naam="licht crème", basis=(248, 226, 180), licht=(255, 246, 222), oor=(236, 204, 150), donker=(236, 204, 150)),
        "rood": V("effen", naam="roodgoud", basis=(204, 124, 62), licht=(232, 166, 96), oor=(178, 100, 48), donker=(178, 100, 48)),
    },
    "mops": {
        "beige": V("masker", naam="beige", basis=(240, 214, 168), licht=(250, 232, 198), masker=(60, 50, 56), oor=(60, 50, 56)),
        "abrikoos": V("masker", naam="abrikoos", basis=(238, 180, 118), licht=(250, 210, 160), masker=(64, 50, 52), oor=(64, 50, 52)),
        "zwart": V("masker", naam="zwart", basis=(62, 58, 70), licht=(84, 80, 94), masker=(40, 36, 46), oor=(40, 36, 46), oogrand=(112, 106, 126)),
    },
    "speurhond": {
        "bruin": V("zadel", naam="roodbruin met zwart zadel", basis=(190, 118, 64), licht=(214, 150, 92), rug=(64, 52, 54), oor=(120, 72, 48)),
    },
}


def maten(ras, pup=False):
    """The breed's numbers worked out into the places of everything (model px; the dog looks to -z, stands on y = 0)."""
    r = dict(BASIS)
    r.update(RASSEN[ras] if isinstance(ras, str) else ras)
    B, h, L = r["lijf"]
    pl, pd = r["poot"]
    kb, kh, kd = r["kop"]
    sb, sh, sl = r["snuit"]
    oortype, ow, oh = r["oren"]
    odx, ofr, orr = r["oog"]
    zak, kin = r["kop_zak"], r["kop_in"]
    if pup:      # the little sibling: a small short body on stubby legs under a head that barely shrank; bigger eyes
        B, h, L = B * 0.78, h * 0.76, L * 0.62
        pl, pd = max(1.6, pl * 0.55), pd * 0.84
        kb, kh, kd = kb * 0.9, kh * 0.9, kd * 0.9
        sb, sh, sl = sb * 0.88, sh * 0.86, max(1.0, sl * 0.6)
        ow, oh = ow * 0.86, oh * (0.8 if oortype in ("punt", "groot") else 0.74)
        odx, orr = odx * 0.9, orr * 0.98
        zak, kin = zak * 0.86, kin * 0.8
    d = NS(ras=r, pup=pup, B=B, h=h, L=L, pl=pl, pd=pd, kb=kb, kh=kh, kd=kd, sb=sb, sh=sh, sl=sl, oortype=oortype, ow=ow, oh=oh)
    d.y0, d.y1, d.zf, d.zb = pl, pl + h, -L / 2, L / 2
    d.ky0 = d.y1 - zak
    d.ky1 = d.ky0 + kh
    d.kzf = d.zf - kd + kin
    d.kzb = d.kzf + kd
    d.sy0 = d.ky0 + 0.25
    d.sz = d.kzf - sl
    d.ex, d.ey, d.er = odx, d.ky0 + ofr * kh, orr
    d.px = B / 2 - pd / 2 - 0.15
    d.pzv, d.pza = d.zf + pd / 2 + 0.5, d.zb - pd / 2 - 0.5
    return d


def _golf(X, Y, Z, s=1.0):
    return s * (0.045 * np.sin(0.9 * X + 0.8 * Z) + 0.035 * np.sin(1.1 * Y + 1.3 * Z + 1.0) + 0.02 * np.sin(1.9 * X + 1.7 * Y + 2.3 * Z))


def vachtkleur(d, c):
    """-> kleur(deel)(X, Y, Z, face): the coat as 3D rules. deel: lijf, kop, wang, snuit, oor, poot, teen, staart, staart2."""
    pat = c["patroon"]
    basis, licht = c["basis"], c.get("licht", c["basis"])

    def lagen(deel, X, Y, Z, face):
        hx, hy, hz = X / (d.kb / 2), (Y - d.ky0) / d.kh, (Z - d.kzf) / d.kd
        bx, by, bz = X / (d.B / 2), (Y - d.y0) / d.h, (Z - d.zf) / d.L
        w = _golf(X, Y, Z)
        ja, nee = np.ones(X.shape, bool), np.zeros(X.shape, bool)
        out = []
        if pat == "urajiro":
            tan = c.get("tan")
            if deel == "kop":
                if tan:
                    out.append(((hy < 0.50 + 0.26 * hx ** 2 + w) & (hz < 0.82), tan))
                out.append(((hy < 0.36 + 0.26 * hx ** 2 + w) & (hz < 0.7), licht))
            elif deel == "wang":
                out.append((ja, licht))
            elif deel == "snuit":
                out.append((ja if face != "up" else (Z < d.sz + d.sl * 0.55), licht))
            elif deel == "lijf":
                if tan:
                    out.append((by < 0.46 + w, tan))
                    out.append(((bz < 0.14) & (by < 0.92), tan))
                out.append((by < 0.32 + w, licht))
                out.append(((bz < 0.12) & (by < 0.8) & (np.abs(bx) < 0.72 + w), licht))
            elif deel == "borst":
                out.append((ja, licht))
            elif deel == "poot":
                if tan:
                    out.append((Y < d.pl * 0.75 + w, tan))
            elif deel == "teen":
                out.append((ja, licht))
            elif deel == "staart2":
                out.append((ja, licht))
        elif pat == "vlekken":
            vlek, tan = c["vlek"], c.get("tan")
            if deel == "kop":
                om = lambda f: (((np.abs(X) - (d.kb / 2 - 1.5)) / (3.1 * f)) ** 2 + ((Y - d.ey - 0.9) / (3.0 * f)) ** 2
                                + ((Z - d.kzf - 0.4) / (3.6 * f)) ** 2) < 1 + w * 2
                oogvlek = om(1.0)                                     # a patch around each eye, up to the ear
                if tan:
                    out.append((om(1.22) & (Y < d.ey + 1.0), tan))
                out.append((oogvlek, vlek))
            elif deel == "oor":
                out.append((ja, tint(vlek, 0.84)))
            elif deel == "wang":
                if tan:
                    out.append((ja, tan))
            elif deel == "lijf":
                out.append(((((bz - 0.56) / 0.25) ** 2 + ((by - 1.0) / 0.62) ** 2 + ((bx - 0.25) / 1.5) ** 2 < 1 + w * 3), vlek))
                out.append(((bz > 0.9 + w) & (by > 0.62 + w), vlek))
            elif deel == "staart":
                out.append((Y < d.y1 + 1.2, vlek))
        elif pat == "effen":
            donker = c.get("donker", basis)
            if deel == "lijf":
                out.append(((bz < 0.14) & (by < 0.8) & (np.abs(bx) < 0.75 + w), licht))
                out.append((by < 0.2 + w, licht))
            elif deel == "kop":
                out.append(((hy < 0.3 + 0.1 * hx ** 2 + w) & (hz < 0.5), licht))
            elif deel in ("borst", "staart2", "wang"):
                out.append((ja, licht))
            elif deel == "snuit":
                out.append((ja, licht))
            elif deel == "oor":
                out.append((ja, donker))
            elif deel == "poot":
                out.append((Z > 99, licht))
        elif pat == "tan":
            tan = c["tan"]
            if deel == "snuit":
                out.append((ja if face != "up" else (Z < d.sz + d.sl * 0.5), tan))
            elif deel == "kop":
                out.append(((hy < 0.3 + w) & (np.abs(hx) > 0.3) & (hz < 0.4), tan))
            elif deel == "lijf":
                out.append(((bz < 0.1) & (by < 0.7) & (np.abs(np.abs(bx) - 0.42) < 0.3 + w), tan))
                out.append(((bz > 0.94) & (by < 0.6) & (np.abs(bx) < 0.4), tan))
            elif deel == "poot":
                out.append((Y < d.pl * 0.7 + 0.4 + w, tan))
            elif deel == "teen":
                out.append((ja, tan))
            elif deel == "oor":
                out.append((ja, c["oor"]))
        elif pat == "corgi":
            rug = c.get("rug")
            if deel == "kop":
                if rug:
                    out.append(((hy > 0.9 + w) & (np.abs(hx) < 0.8) | ((hz > 0.55) & (hy > 0.45)), rug))
                out.append(((np.abs(hx) < 0.13 + 0.16 * np.clip(0.9 - hy, 0, 1) + w * 0.6) & (hz < 0.5) & (hy < 0.97), licht))
                out.append(((hy < 0.3 + 0.2 * hx ** 2 + w) & (hz < 0.7), licht))
            elif deel in ("snuit", "wang", "borst", "poot", "teen"):
                out.append((ja, licht))
            elif deel == "lijf":
                if rug:
                    out.append(((by > 0.5 + w) & (bz > 0.3 + w), rug))
                out.append((bz < 0.2 + w, licht))
                out.append((by < 0.3 + w, licht))
                out.append(((bz > 0.97) & (by < 0.75), mix(basis, licht, 0.55)))
            elif deel == "broek":
                out.append((ja, mix(basis, licht, 0.6)))
        elif pat == "masker":
            masker = c["masker"]
            if deel == "snuit":
                out.append((ja, masker))
            elif deel == "kop":
                out.append(((hy < 0.26 + w) & (hz < 0.45), licht))
            elif deel == "oor":
                out.append((ja, masker))
            elif deel == "lijf":
                out.append((by < 0.3 + w, licht))
                out.append(((np.abs(bx) < 0.14) & (by > 0.98), mix(basis, masker, 0.35)))
            elif deel in ("wang", "teen"):
                out.append((ja, licht))
        elif pat == "zadel":
            rug = c["rug"]
            if deel == "lijf":
                out.append(((by > 0.42 + w) & (bz > 0.22 + w) & (bz < 0.95 + w), rug))
                out.append(((bz < 0.12) & (by < 0.8), licht))
            elif deel in ("snuit", "teen", "lip"):
                out.append((ja, licht))
            elif deel == "oor":
                out.append((ja, c["oor"]))
            elif deel == "staart":
                out.append((Y > d.y1 - 1.2 + 0 * X, rug))
        rgb = np.broadcast_to(np.array(basis, np.float32), X.shape + (3,)).copy()
        for mask, kleur in out:
            rgb = leg(rgb, mask, kleur)
        return rgb

    return lambda deel: (lambda X, Y, Z, face: lagen(deel, X, Y, Z, face))


def hond(atlas, ras, vacht, pup=False, extra=(), blik="open", halsband=(220, 60, 70), grijs=False, tong=True, naam="hond"):
    """The dog: -> (geo, d). ras = a key of RASSEN (or a dict of numbers), vacht = a coat dict, extra = accessories
    [(name, {options})], blik = the eyes' mood, grijs = an old dog's grey snout and brows."""
    d = maten(ras, pup)
    c = vacht
    K = vachtkleur(d, c)
    neus = c.get("neus", NEUS)
    binnen = c.get("oor", (236, 150, 160))
    oortype = d.oortype
    grijs_k = (236, 232, 232)

    # ---- the face: the coat, brow dots / mask / wrinkles, then the eyes and the blush --------------------------------
    def gezicht(X, Y, Z, face):
        rgb = K("kop")(X, Y, Z, face)
        glad = np.zeros(X.shape, np.float32)
        if face != "north":
            return rgb, glad
        if c["patroon"] == "masker":                                  # the pug's dark mask around the eyes, fading out
            for s in (1, -1):
                m = np.hypot((X - s * d.ex) / (d.er * 1.7), (Y - d.ey + 0.2) / (d.er * 1.75))
                rgb = leg(rgb, m < 1, c["masker"], 0.6)
                rgb = leg(rgb, m < 0.8, c["masker"], 0.5)
            rgb = leg(rgb, (np.abs(X) < d.sb / 2 + 0.6) & (Y < d.sy0 + d.sh + 0.9), c["masker"], 0.85)
        if d.ras["rimpel"]:                                           # forehead folds
            donker = tint(c["basis"], 0.72)
            for i, (yy, br) in enumerate(((d.ey + d.er + 1.25, 1.9), (d.ey + d.er + 2.15, 1.3))):
                if yy < d.ky1 - 0.3:
                    rgb = leg(rgb, (np.abs(Y - yy + 0.12 * X ** 2 * (0.5 if i else 0.35)) < 0.14) & (np.abs(X) < br), donker, 0.8)
        if c.get("wenk") is not None or grijs:                         # brow dots (tan points, the shiba's, an old dog's)
            wk = grijs_k if grijs else c["wenk"]
            for s in (1, -1):
                rgb = leg(rgb, np.hypot((X - s * (d.ex - 0.5)) / 0.75, (Y - (d.ey + d.er + 0.95)) / 0.5) < 1, wk)
        if grijs:                                                     # the grey creeping up from the snout
            rgb = leg(rgb, (np.abs(X) < d.sb / 2 + 0.9) & (Y < d.sy0 + d.sh + 1.0 + _golf(X, Y, Z, 4)), grijs_k, 0.8)
        lid = tint(c["basis"] if c["patroon"] != "vlekken" else c["vlek"], 0.86)
        for s in (1, -1):
            rgb = blos(rgb, X, Y, s * d.ex, d.ey, d.er, s, 0.6 if not pup else 0.72)
            rgb, glad = oog(rgb, glad, X, Y, s * d.ex, d.ey, d.er, s, blik, lid, c.get("oogrand", OOG_RAND))
        return rgb, glad

    def snuit_k(X, Y, Z, face):
        rgb = K("snuit")(X, Y, Z, face)
        if grijs:
            rgb = leg(rgb, np.ones(X.shape, bool), grijs_k, 0.85)
        glad = np.zeros(X.shape, np.float32)
        if face == "north":                                           # the mouth: a line down from the nose, a soft "w", a tongue
            lijn = tint(rgb.reshape(-1, 3).mean(0), 0.5) if c["patroon"] != "masker" else (20, 16, 22)
            ym = d.sy0 + d.sh * 0.36
            rgb = leg(rgb, (np.abs(X) < 0.16) & (Y > ym) & (Y < d.sy0 + d.sh - 0.9), lijn)
            boog = ym - 0.42 * np.sin(np.clip(np.abs(X) / (d.sb * 0.32), 0, 1) * math.pi)
            rgb = leg(rgb, (np.abs(Y - boog) < 0.17) & (np.abs(X) < d.sb * 0.32), lijn)
            if tong:
                t = (np.abs(X) < 0.62) & (Y < ym - 0.1) & (Y > ym - 1.15) & (np.hypot(X / 0.66, (Y - ym + 0.5) / 0.7) < 1)
                rgb = leg(rgb, t, (244, 120, 150))
                glad = np.maximum(glad, t.astype(np.float32))
        return rgb, glad

    def neus_k(X, Y, Z, face):
        rgb = np.broadcast_to(np.array(neus, np.float32), X.shape + (3,)).copy()
        if face in ("north", "up"):
            rgb = leg(rgb, np.hypot((X - 0.45) / 0.36, ((Y if face == "north" else Z) - (d.sy0 + d.sh + 0.05 if face == "north" else d.sz - 0.15)) / 0.22) < 1, (150, 140, 160), 0.8)
        return rgb, np.ones(X.shape, np.float32)

    B, h, L, y0, y1, zf, zb = d.B, d.h, d.L, d.y0, d.y1, d.zf, d.zb
    kb, kh, kd, ky0, ky1, kzf = d.kb, d.kh, d.kd, d.ky0, d.ky1, d.kzf
    lijf = [blok(atlas, [-B / 2, y0, zf], [B, h, L], K("lijf")),
            blok(atlas, [-B / 2 + 1, y1, zf + 1.5], [B - 2, 0.8, L - 2.6], K("lijf"), alleen=("up", "north", "south", "east", "west")),
            blok(atlas, [-B / 2 + 0.8, y0 + 0.6, zf - 0.9], [B - 1.6, h - 1.4, 0.9], K("lijf"), alleen=("north", "east", "west", "up", "down")),
            blok(atlas, [-B / 2 + 1, y0 + 0.9, zb], [B - 2, h - 2.0, 0.7], K("lijf"), alleen=("south", "east", "west", "up", "down"))]
    for s in (1, -1):
        zij = blok(atlas, [B / 2, y0 + 1.1, zf + 1.6], [0.7, h - 2.4, L - 3.2], K("lijf"), alleen=("east", "north", "south", "up", "down"))
        lijf.append(zij if s > 0 else _spiegel_zij(atlas, zij, K("lijf")))
    if d.ras["borst"]:                                                # a fluffy chest
        hoog = max(1.6, min(h * 0.6, ky0 - 1.9 - y0))
        lijf.append(blok(atlas, [-B / 2 + 1.7, y0 + 0.1, zf - 1.7], [B - 3.4, hoog, 0.8], K("borst")))
    if d.ras["broek"]:                                                # the corgi's fluffy trousers
        lijf.append(blok(atlas, [-B / 2 + 0.5, y0 + 0.3, zb + 0.7], [B - 1.0, h * 0.62, 0.9], K("broek")))
        lijf.append(blok(atlas, [-B / 2 + 1.6, y0 + h * 0.62 + 0.3, zb + 0.7], [B - 3.2, 1.0, 0.7], K("broek")))
    bones = [bot("root", None, [0, 0, 0], []), bot("lijf", "root", [0, y0 + h / 2, 0], lijf)]
    if halsband:
        hb = effen(halsband)
        bones.append(bot("halsband", "lijf", [0, ky0 - 1, zf], [
            blok(atlas, [-B / 2 - 0.35, ky0 - 1.55, zf - 1.2], [B + 0.7, 1.15, 2.5], hb, 6),
            blok(atlas, [-0.65, ky0 - 2.5, zf - 1.5], [1.3, 1.3, 0.4], effen((250, 206, 90)), 5)]))
    kop = [blok(atlas, [-kb / 2, ky0, kzf], [kb, kh, kd], gezicht),
           blok(atlas, [-kb / 2 + 1, ky1, kzf + 1], [kb - 2, 0.8, kd - 2], K("kop"), alleen=("up", "north", "south", "east", "west")),
           blok(atlas, [-kb / 2 + 1, ky0 + 1, kzf + kd], [kb - 2, kh - 2, 0.7], K("kop"), alleen=("south", "east", "west", "up", "down"))]
    for s in (1, -1):
        if d.ras["wang"]:                                             # chubby cheek fluff
            w1 = blok(atlas, [kb / 2, ky0 + 0.3, kzf + 0.6], [0.9, kh * 0.5, kd * 0.5], K("wang"))
            w2 = blok(atlas, [kb / 2 + 0.9, ky0 + 0.9, kzf + 1.3], [0.6, kh * 0.5 - 1.6, kd * 0.5 - 1.6], K("wang"))
            kop += [w1, w2] if s > 0 else [spiegel(w1), spiegel(w2)]
        else:
            w1 = blok(atlas, [kb / 2, ky0 + 0.8, kzf + 1.0], [0.6, kh - 2.2, kd - 2.2], K("kop"))
            kop.append(w1 if s > 0 else spiegel(w1))
    bones.append(bot("kop", "lijf", [0, ky0 + 1.5, d.kzb - 2], kop))
    snuit = [blok(atlas, [-d.sb / 2, d.sy0, d.sz], [d.sb, d.sh, d.sl + 0.3], snuit_k, alleen=("north", "east", "west", "up", "down")),
             blok(atlas, [-1.15, d.sy0 + d.sh - 1.05, d.sz - 0.5], [2.3, 1.35, 1.0], neus_k, 3)]
    if d.ras["lippen"]:                                               # hanging lips
        for s in (1, -1):
            lp = blok(atlas, [d.sb / 2 - 0.5, d.sy0 - 1.0, d.sz + 0.3], [1.0, d.sh * 0.72, d.sl - 0.6], K("lip"))
            snuit.append(lp if s > 0 else spiegel(lp))
    bones.append(bot("snuit", "kop", [0, d.sy0 + d.sh / 2, kzf], snuit))
    bones += _oren(atlas, d, K, binnen, c)
    bones += _staart(atlas, d, K)
    for naam_, sx, z in (("poot_lv", 1, d.pzv), ("poot_rv", -1, d.pzv), ("poot_la", 1, d.pza), ("poot_ra", -1, d.pza)):
        x, pd, pl = sx * d.px, d.pd, d.pl
        bones.append(bot(naam_, "lijf", [x, y0 + 0.5, z], [
            blok(atlas, [x - pd / 2, 0, z - pd / 2], [pd, pl + 0.9, pd], K("poot")),
            blok(atlas, [x - pd / 2 - 0.25, 0, z - pd / 2 - 0.75], [pd + 0.5, 1.25, pd + 1.0], K("teen"))]))
    for nm, opties in extra:
        bones += EXTRAS[nm](atlas, d, **opties)
    hoogte = (ky1 + d.oh + 2) / 16
    return geo(naam, bones, max(1.2, (L + kd + d.sl) / 16 + 0.3), max(1.0, hoogte), atlas.maat), d


def _spiegel_zij(atlas, c, kleur):
    """The west twin of an east side plate, painted for its own place (patterns need not be symmetric)."""
    x0, y, z = c["origin"]
    w, h, dd = c["size"]
    return blok(atlas, [-(x0 + w), y, z], [w, h, dd], kleur, alleen=("west", "north", "south", "up", "down"))


def _oren(atlas, d, K, binnen, c):
    t, w, h = d.oortype, d.ow, d.oh
    kb, ky1, kzf = d.kb, d.ky1, d.kzf
    vacht_oor = K("oor")
    out = []
    for kant, s in (("links", 1), ("rechts", -1)):
        cubes, rot = [], None
        if t in ("punt", "groot"):
            xo, z, dik = kb / 2 - 0.3, kzf + 2.2, 2.0
            stappen = ((0, w, h * 0.42), (0.75, w - 1.1, h * 0.32), (1.45, max(1.0, w - 2.2), h * 0.26))

            def k_oor(X, Y, Z, face, xo=xo, w=w, h=h):
                rgb = vacht_oor(X, Y, Z, face)
                if face == "north":
                    yy = (Y - ky1) / h
                    half = (w / 2 - 0.6) * (1 - yy * 1.0)
                    xc = xo - w / 2 + 0.3 * yy
                    rgb = leg(rgb, (np.abs(np.abs(X) - xc) < half) & (yy > 0.02) & (yy < 0.86), binnen)
                return rgb
            yy = ky1 - 0.3
            for dx, ww, hh in stappen:
                hh2 = hh + (0.3 if dx == 0 else 0)
                cb = blok(atlas, [xo - w / 2 - ww / 2 + dx * 0.2, yy, z], [ww, hh2, dik], k_oor)
                cubes.append(cb if s > 0 else spiegel(cb))
                yy += hh2
            rot = [0, 0, -s * (11 if t == "groot" else 5)]
            pivot = [s * (xo - w / 2), ky1, z + dik / 2]
        elif t in ("knak", "roos"):
            xo = kb / 2 - 0.3 if t == "knak" else kb / 2 + 0.2
            z = kzf + 0.75
            cb = [blok(atlas, [xo - w + 0.3, ky1 - 0.3, z], [w - 0.6, h + 0.3, 1.3], vacht_oor),
                  blok(atlas, [xo - w, ky1 + h - 2.0, z - 1.2], [w, 2.0, 1.2], vacht_oor),
                  blok(atlas, [xo - w + 0.6, ky1 + h - 3.1, z - 1.15], [w - 1.2, 1.1, 1.0], vacht_oor)]
            cubes += cb if s > 0 else [spiegel(q) for q in cb]
            rot = [0, 0, -s * (8 if t == "knak" else 20)]
            pivot = [s * (xo - w / 2), ky1, z + 0.7]
        else:                                                         # hang: a long soft flap down the side of the head
            x, yt, z = kb / 2 + 0.1, ky1 - 0.9, kzf + 1.3
            cb = [blok(atlas, [kb / 2 - 1.0, ky1 - 1.6, z - 0.3], [2.4, 2.1, w + 0.6], vacht_oor),
                  blok(atlas, [x, yt - (h - 1.5), z], [2.0, h - 1.5, w], vacht_oor),
                  blok(atlas, [x + 0.2, yt - h, z + 0.7], [1.8, 1.5, w - 1.4], vacht_oor)]
            cubes += cb if s > 0 else [spiegel(q) for q in cb]
            rot = [0, 0, -s * 14]
            pivot = [s * (kb / 2 + 0.4), ky1 - 0.4, z + w / 2]
        out.append(bot(f"oor_{kant}", "kop", pivot, cubes, rot))
    return out


def _staart(atlas, d, K):
    t = d.ras["staart"]
    y1, zb = d.y1, d.zb
    f = 0.8 if d.pup else 1.0
    st, st2 = K("staart"), K("staart2")
    rot = None
    if t == "krul":
        cubes = [blok(atlas, [-1.25 * f, y1 - 1.5, zb - 1.7 * f], [2.5 * f, 1.5 + 2.9 * f, 2.5 * f], st),
                 blok(atlas, [-1.25 * f, y1 + 0.5 + 1.0 * f, zb - 4.7 * f], [2.5 * f, 2.4 * f, 3.2 * f], st),
                 blok(atlas, [-1.05 * f, y1 + 0.3, zb - 5.0 * f], [2.1 * f, 0.3 + 1.6 * f, 1.7 * f], st2)]
    elif t == "krulletje":
        cubes = [blok(atlas, [-1.0 * f, y1 - 1.2, zb - 1.3 * f], [2.0 * f, 1.2 + 2.4 * f, 2.0 * f], st),
                 blok(atlas, [-1.0 * f, y1 + 0.5 + 0.7 * f, zb - 3.5 * f], [2.0 * f, 1.9 * f, 2.4 * f], st),
                 blok(atlas, [-0.8 * f, y1 + 0.5, zb - 3.7 * f], [1.6 * f, 0.9 * f, 1.3 * f], st)]
    elif t == "recht":
        cubes = [blok(atlas, [-0.85 * f, y1 - 1.2, zb - 1.5 * f], [1.7 * f, 1.2 + 4.2 * f, 1.7 * f], st)]
        rot = [18, 0, 0]
    elif t == "lang":
        cubes = [blok(atlas, [-0.95 * f, y1 - 2.4, zb - 0.6], [1.9 * f, 1.9 * f, 0.6 + 3.8 * f], st),
                 blok(atlas, [-0.7 * f, y1 - 2.4 + 0.25 * f, zb + 3.8 * f], [1.4 * f, 1.4 * f, 3.0 * f], st)]
        rot = [24, 0, 0]
    elif t == "stomp":
        cubes = [blok(atlas, [-1.3 * f, y1 - 2.6, zb + 0.4], [2.6 * f, 2.2 * f, 1.9 * f], st)]
    else:                                                             # pluim: a feathered plume
        cubes = [blok(atlas, [-1.15 * f, y1 - 2.8, zb - 0.6], [2.3 * f, 2.3 * f, 0.6 + 6.2 * f], st),
                 blok(atlas, [-0.75 * f, y1 - 2.8 - 1.9 * f, zb + 0.9 * f], [1.5 * f, 2.0 * f, 5.0 * f], st2),
                 blok(atlas, [-0.9 * f, y1 - 2.8 + 0.25 * f, zb + 6.2 * f], [1.8 * f, 1.8 * f, 1.1 * f], st2)]
        rot = [16, 0, 0]
    return [bot("staart", "lijf", [0, y1 - 1.5, zb - 0.5], cubes, rot)]


EXTRAS = {}


# =====================================================================================================================
# rendering (the repo's quads: wiki_renders.geo_quads; a fast exact rasteriser with the same light as wiki_renders.render)
# =====================================================================================================================
def _wr():
    tools = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
    if tools not in sys.path:
        sys.path.insert(0, tools)
    import wiki_renders
    return wiki_renders


_TMP = None


def quads(geo_, tex, pose=None, verberg=()):
    """wiki_renders quads of a geo dict with an in-memory texture; pose = {bone: [rx, ry, rz]} (added to the bone's own)."""
    global _TMP
    wr = _wr()
    g = copy.deepcopy(geo_)
    for b in g["minecraft:geometry"][0]["bones"]:
        if pose and b["name"] in pose:
            b["rotation"] = [a + c for a, c in zip(b.get("rotation", [0, 0, 0]), pose[b["name"]])]
    _TMP = _TMP or tempfile.mkdtemp()
    pad = os.path.join(_TMP, "m.geo.json")
    json.dump(g, open(pad, "w"))
    return wr.geo_quads(pad, np.asarray(tex).astype(np.float32), hide=verberg)


def teken(qs, yaw=205, pitch=-14, schaal=14.0, ss=2):
    """Render quads -> (RGBA image cropped to the model, (ox, oy) = where the model's origin (0, 0, 0) is in it).
    schaal = screen pixels per model pixel. Orthographic, z-buffered, the light and shade of wiki_renders.render."""
    wr = _wr()
    R = wr.rot_matrix(yaw, pitch)
    k = schaal * 16 * ss
    pts = np.array([R @ (q.origin + a * q.u + b * q.v) for q in qs for a in (0, 1) for b in (0, 1)])
    lo, hi = pts[:, :2].min(0), pts[:, :2].max(0)
    rand = 3 * ss
    W, H = int(math.ceil((hi[0] - lo[0]) * k)) + 2 * rand, int(math.ceil((hi[1] - lo[1]) * k)) + 2 * rand
    zbuf = np.full((H, W), np.inf)
    col = np.zeros((H, W, 4), np.float32)
    light = np.array([-0.35, 0.85, -0.4])
    light /= np.linalg.norm(light)
    for q in qs:
        if (R @ q.normal)[2] > 1e-6:
            continue
        shade = 0.62 + 0.38 * max(0.0, float(np.dot(q.normal, light)))
        tex = wr.tex_array(q.tex) if isinstance(q.tex, str) else q.tex
        th, tw = tex.shape[:2]
        P0, U, Vv = R @ q.origin, R @ q.u, R @ q.v
        det = U[0] * Vv[1] - U[1] * Vv[0]
        if abs(det) < 1e-12:
            continue
        c4 = np.array([P0, P0 + U, P0 + Vv, P0 + U + Vv])
        sx, sy = (c4[:, 0] - lo[0]) * k + rand, (hi[1] - c4[:, 1]) * k + rand
        x0, x1 = max(0, int(sx.min())), min(W, int(sx.max()) + 2)
        y0, y1 = max(0, int(sy.min())), min(H, int(sy.max()) + 2)
        if x1 <= x0 or y1 <= y0:
            continue
        gx, gy = np.meshgrid(np.arange(x0, x1) + 0.5, np.arange(y0, y1) + 0.5)
        rx, ry = lo[0] + (gx - rand) / k - P0[0], hi[1] - (gy - rand) / k - P0[1]
        a, b = (rx * Vv[1] - ry * Vv[0]) / det, (U[0] * ry - U[1] * rx) / det
        m = (a >= 0) & (a < 1) & (b >= 0) & (b < 1)
        if not m.any():
            continue
        z = P0[2] + a * U[2] + b * Vv[2]
        u0, v0, u1, v1 = q.uv
        tu = np.clip(((u0 + a * (u1 - u0)) * tw).astype(int), 0, tw - 1)
        tv = np.clip(((v0 + b * (v1 - v0)) * th).astype(int), 0, th - 1)
        rgba = tex[tv, tu]
        sub_z = zbuf[y0:y1, x0:x1]
        m &= (rgba[..., 3] > 20) & (z < sub_z - 1e-6)
        sub_z[m] = z[m]
        sub = col[y0:y1, x0:x1]
        kleur = rgba[..., :3] * shade
        if q.tint is not None:
            kleur = kleur * (np.array(q.tint[:3]) / 255.0)
        sub[m, :3] = kleur[m]
        sub[m, 3] = 255
    img = Image.fromarray(np.clip(col, 0, 255).astype(np.uint8))
    img = img.convert("RGBa").resize((W // ss, H // ss), Image.LANCZOS).convert("RGBA")
    o = R @ np.zeros(3)
    return img, ((o[0] - lo[0]) * k / ss + rand / ss, (hi[1] - o[1]) * k / ss + rand / ss)


def figuur_hond(ras, kleur, seed=1, **kw):
    atlas = Atlas(seed, kw.pop("maat", 128))
    vacht = VACHTEN[ras][kleur] if isinstance(kleur, str) else kleur
    g, d = hond(atlas, ras, vacht, **kw)
    return g, atlas.img, d


# =====================================================================================================================
# EXTRA'S: the accessories. Each places itself from the breed's numbers (d), so it fits every dog.
# =====================================================================================================================
WIT, GOUD, LEER, MARINE = (250, 250, 246), (246, 204, 92), (150, 96, 58), (44, 62, 110)


def _rgba(X, kleur=None):
    a = np.zeros(X.shape + (4,), np.float32)
    if kleur is not None:
        a[..., :3], a[..., 3] = kleur, 255
    return a


def _zet(a, mask, kleur):
    a[mask, :3] = kleur
    a[mask, 3] = 255
    return a


def _vol(X, kleur):
    return np.broadcast_to(np.array(kleur, np.float32), X.shape + (3,)).copy()


def _stof(kleur, zoom=None, zoom_y=None, extra=None):
    """Cloth: the colour, a hem below zoom_y (colour zoom), extra(rgb, X, Y, Z, face) for buttons and pockets."""
    def k(X, Y, Z, f):
        rgb = _vol(X, kleur)
        if zoom is not None and zoom_y is not None and f not in ("up", "down"):
            rgb = leg(rgb, Y < zoom_y, zoom)
        if extra:
            rgb = extra(rgb, X, Y, Z, f)
        return rgb
    return k


def x_bril(atlas, d, kleur=GOUD):
    r = d.er + 0.5
    x1 = d.kb / 2 + (1.6 if d.ras["wang"] else 0.7)

    def k(X, Y, Z, f):
        a = _rgba(X)
        for s in (1, -1):
            dd = np.hypot((X - s * d.ex) / r, (Y - d.ey) / (r * 1.08))
            _zet(a, (dd <= 1) & (dd > 0.8), kleur)
            _zet(a, (dd > 1) & (np.abs(Y - d.ey - 0.1) < 0.2) & (s * X > d.ex), kleur)
        _zet(a, (np.abs(X) < d.ex - r * 0.85) & (np.abs(Y - d.ey - 0.25) < 0.2), kleur)
        return a, np.ones(X.shape)
    cubes = [blok(atlas, [-x1, d.ey - r * 1.08 - 0.1, d.kzf - 0.32], [2 * x1, 2 * r * 1.08 + 0.2, 0.3], k, 0, alleen=("north",))]
    for s in (1, -1):
        arm = blok(atlas, [x1 - 0.3, d.ey - 0.12, d.kzf - 0.32], [0.3, 0.42, 3.2], effen(kleur), 4)
        cubes.append(arm if s > 0 else spiegel(arm))
    return [bot("x_bril", "kop", [0, d.ey, d.kzf], cubes)]


def x_spiegel(atlas, d, band=(96, 70, 60)):
    """The doctor's forehead mirror: a strap around the head with a round silver disc over one eye."""
    def schijf(X, Y, Z, f):
        a = _rgba(X)
        cx, cy = 2.3, d.ky1 - 0.7
        dd = np.hypot(X - cx, Y - cy) / 1.7
        _zet(a, dd <= 1, (150, 160, 176))
        _zet(a, dd <= 0.8, (214, 224, 236))
        _zet(a, np.hypot(X - cx - 0.45, Y - cy - 0.45) < 0.5, (255, 255, 255))
        _zet(a, dd <= 0.2, (120, 130, 150))
        return a, np.ones(X.shape)
    w = d.kb + (2.2 if d.ras["wang"] else 1.5)
    return [bot("x_spiegel", "kop", [0, d.ky1, d.kzf], [
        blok(atlas, [-w / 2, d.ky1 - 1.25, d.kzf - 0.3], [w, 0.9, d.kd + 1.2], effen(band), 5),
        blok(atlas, [0.6, d.ky1 - 2.4, d.kzf - 1.0], [3.4, 3.4, 0.7], schijf, 0, alleen=("north",))])]


def x_jas(atlas, d, kleur=WIT, rand=(204, 214, 230), knoop=GOUD, kruis=False, lang=1.0):
    """A coat over the back and flanks, a front panel with buttons, a pocket on each flank (kruis: a red cross on it)."""
    B, h, L, y0, y1, zf = d.B, d.h, d.L, d.y0, d.y1, d.zf
    yb = y0 + h * 0.26
    z0, lengte = zf + 0.5, (L - 2.6) * lang

    def romp(rgb, X, Y, Z, f):
        if f in ("east", "west"):
            zc, yc = z0 + lengte * 0.55, yb + (y1 - yb) * 0.42
            zak = (np.abs(Z - zc) < 1.5) & (np.abs(Y - yc) < 1.2)
            rgb = leg(rgb, zak & ~((np.abs(Z - zc) < 1.25) & (np.abs(Y - yc) < 0.95)), rand)
            if kruis:
                rgb = leg(rgb, ((np.abs(Z - zc) < 0.28) & (np.abs(Y - yc) < 0.8)) | ((np.abs(Z - zc) < 0.8) & (np.abs(Y - yc) < 0.28)), (226, 60, 70))
        if f == "up":
            rgb = leg(rgb, np.abs(X) < 0.14, rand)
        if f != "down":
            rgb = leg(rgb, Z > z0 + lengte - 0.5, rand)
        return rgb

    def voor(rgb, X, Y, Z, f):
        if f == "north":
            rgb = leg(rgb, np.abs(X) < 0.13, rand)
            top = d.ky0 - 1.7
            for i in range(2):
                rgb = leg(rgb, np.hypot(X - 0.95, Y - (top - 0.8 - i * 1.4)) < 0.42, knoop)
        return rgb
    hoog = max(1.2, d.ky0 - 1.7 - yb)
    return [bot("x_jas", "lijf", [0, y1, 0], [
        blok(atlas, [-B / 2 - 0.95, yb, z0], [B + 1.9, y1 + 1.25 - yb, lengte], _stof(kleur, rand, yb + 0.5, romp), 5),
        blok(atlas, [-B / 2 + 0.3, yb, zf - 1.45], [B - 0.6, hoog, 2.0], _stof(kleur, rand, yb + 0.5, voor), 5)])]


def x_cape(atlas, d, kleur=(64, 120, 84), rand=GOUD, deel=0.5, gebreid=False):
    """A short cape / shawl over the shoulders."""
    B, h, L, y0, y1, zf = d.B, d.h, d.L, d.y0, d.y1, d.zf
    yb = y0 + h * 0.42
    lengte = max(4.0, L * deel)

    def k(X, Y, Z, f):
        rgb = _vol(X, kleur)
        if gebreid:                                                   # knitted: rows of stitches, a fringe
            u = Z if f in ("east", "west", "up") else X
            v = Y if f != "up" else X
            rgb = leg(rgb, (np.floor(u * 1.5 + np.floor(v * 1.5) * 0.5) % 2 == 0), tint(kleur, 0.86))
        a = _rgba(X, (0, 0, 0))
        a[..., :3] = rgb
        if f not in ("up", "down"):
            a[..., :3] = leg(rgb, Y < yb + 0.55, rand)
            if gebreid:
                u = Z if f in ("east", "west") else X
                a[(Y < yb + 0.55) & (np.floor(u * 1.25) % 2 == 0), 3] = 0
        if f == "up":
            a[..., :3] = leg(a[..., :3], Z > zf + 0.2 + lengte - 0.5, rand)
        return a
    return [bot("x_cape", "lijf", [0, y1, zf], [
        blok(atlas, [-B / 2 - 1.0, yb, zf + 0.2], [B + 2.0, y1 + 1.25 - yb, lengte], k, 5),
        blok(atlas, [-B / 2 - 0.5, d.ky0 - 1.9, zf - 1.35], [B + 1.0, 1.5, 1.6], _stof(kleur, rand, d.ky0 - 1.5), 5)])]


def x_sjaal(atlas, d, kleur=(70, 120, 200), streep=(240, 240, 236)):
    B, zf, ky0 = d.B, d.zf, d.ky0

    def k(X, Y, Z, f):
        u = X + Z if f in ("up", "down") else (X if f in ("north", "south") else Z)
        return leg(_vol(X, kleur), np.floor(u * 0.62 + 50) % 2 == 0, streep)

    def eind(X, Y, Z, f):
        return leg(_vol(X, kleur), np.floor(Y * 0.8 + 50) % 2 == 0, streep)
    lang = min(3.4, ky0 - 2.0 - 0.6)
    return [bot("x_sjaal", "lijf", [0, ky0 - 1, zf], [
        blok(atlas, [-B / 2 - 0.65, ky0 - 2.15, zf - 1.55], [B + 1.3, 1.95, 3.2], k, 5),
        blok(atlas, [B / 2 - 2.6, ky0 - 2.0 - lang, zf - 2.0], [2.0, lang, 0.65], eind, 5)])]


def x_bandana(atlas, d, kleur=(226, 64, 70), stip=(255, 250, 240)):
    B, zf, ky0 = d.B, d.zf, d.ky0
    hoog = min(3.0, ky0 - 1.5 - 0.8)

    def stippen(rgb, Y, u):
        return leg(rgb, np.hypot((u * 0.9) % 1.6 - 0.8, (Y * 0.9 + np.floor(u * 0.9 / 1.6) * 0.8) % 1.6 - 0.8) < 0.3, stip)

    def band(X, Y, Z, f):
        return stippen(_vol(X, kleur), Y, X if f in ("north", "south", "up", "down") else Z)

    def punt(X, Y, Z, f):
        a = _rgba(X, kleur)
        a[..., :3] = stippen(a[..., :3], Y, X)
        a[np.abs(X) > 2.7 * np.clip((Y - (ky0 - 1.5 - hoog)) / hoog, 0, 1) + 0.25, 3] = 0
        return a
    return [bot("x_bandana", "lijf", [0, ky0 - 1, zf], [
        blok(atlas, [-B / 2 - 0.5, ky0 - 1.8, zf - 1.35], [B + 1.0, 1.5, 2.8], band, 5),
        blok(atlas, [-2.9, ky0 - 1.5 - hoog, zf - 1.9], [5.8, hoog, 0.55], punt, 5, alleen=("north", "south"))])]


def x_medaille(atlas, d, lint=(190, 50, 60), goud=GOUD):
    B, zf, ky0 = d.B, d.zf, d.ky0
    cy = ky0 - 3.5

    def schijf(X, Y, Z, f):
        a = _rgba(X)
        dd = np.hypot(X, Y - cy) / 1.45
        _zet(a, dd <= 1, tint(goud, 0.8))
        _zet(a, dd <= 0.78, goud)
        _zet(a, np.hypot(X / 0.6, (Y - cy - 0.1) / 0.42) < 1, (120, 80, 40))            # a little nose: the sniff medal
        _zet(a, (np.abs(X) < 0.12) & (Y < cy - 0.2) & (Y > cy - 0.8), (120, 80, 40))
        _zet(a, np.hypot(X - 0.6, Y - cy - 0.7) < 0.25, (255, 250, 220))
        return a, np.ones(X.shape)
    return [bot("x_medaille", "lijf", [0, ky0 - 1, zf], [
        blok(atlas, [-B / 2 - 0.55, ky0 - 1.95, zf - 1.45], [B + 1.1, 1.3, 2.9], effen(lint), 5),
        blok(atlas, [-0.7, ky0 - 2.6, zf - 2.0], [1.4, 1.0, 0.5], effen(lint), 5),
        blok(atlas, [-1.5, cy - 1.5, zf - 2.3], [3.0, 3.0, 0.5], schijf, 0, alleen=("north",))])]


def x_tas(atlas, d, kleur=LEER, teken=None, kant=1):
    """A satchel on one flank with a strap over the back. teken: kruis | schelp | None."""
    B, h, y0, y1 = d.B, d.h, d.y0, d.y1
    yb, hoog = y0 + h * 0.14, h * 0.6

    def k(X, Y, Z, f):
        rgb = _vol(X, kleur)
        if f in ("east", "west"):
            rgb = leg(rgb, Y > yb + hoog * 0.5, tint(kleur, 0.8))
            rgb = leg(rgb, (np.abs(Z) < 0.4) & (np.abs(Y - yb - hoog * 0.5) < 0.45), GOUD)
            yc = yb + hoog * 0.26
            if teken == "kruis":
                rgb = leg(rgb, np.hypot(Z, Y - yc) < 1.0, WIT)
                rgb = leg(rgb, ((np.abs(Z) < 0.22) & (np.abs(Y - yc) < 0.7)) | ((np.abs(Z) < 0.7) & (np.abs(Y - yc) < 0.22)), (226, 60, 70))
            elif teken == "schelp":
                rgb = leg(rgb, (np.hypot(Z, Y - yc + 0.5) < 1.15) & (Y > yc - 0.5), (255, 214, 200))
        return rgb
    x = B / 2 + 1.05
    tas = blok(atlas, [x, yb, -2.5], [1.9, hoog, 5.0], k, 5)
    return [bot("x_tas", "lijf", [0, y1, 0], [
        tas if kant > 0 else spiegel(tas),
        blok(atlas, [-B / 2 - 1.1, y0 + h * 0.55, -0.7], [B + 2.2, y1 + 1.4 - (y0 + h * 0.55), 1.4], effen(tint(kleur, 0.82)), 5)])]


def x_rugzak(atlas, d, kleur=(110, 136, 84), rol=(214, 96, 70)):
    B, h, y0, y1 = d.B, d.h, d.y0, d.y1

    def k(X, Y, Z, f):
        rgb = _vol(X, kleur)
        if f in ("south", "east", "west"):
            rgb = leg(rgb, Y > y1 + 2.8, tint(kleur, 0.78))
        if f == "south":
            rgb = leg(rgb, (np.abs(X) < 0.5) & (np.abs(Y - y1 - 2.7) < 0.5), GOUD)
        return rgb

    def deken(X, Y, Z, f):
        return leg(_vol(X, rol), np.floor(X * 0.7 + 50) % 3 == 0, (250, 236, 200))
    return [bot("x_rugzak", "lijf", [0, y1, 0], [
        blok(atlas, [-B / 2 + 0.8, y1 + 0.8, -2.2], [B - 1.6, 3.6, 5.4], k, 5),
        blok(atlas, [-B / 2 + 0.2, y1 + 4.4, -0.6], [B - 0.4, 1.9, 2.1], deken, 5),
        blok(atlas, [-B / 2 - 0.85, y0 + 0.5, -1.2], [B + 1.7, h + 0.4, 1.1], effen(tint(kleur, 0.7)), 5)])]


def x_schort(atlas, d, kleur=WIT, rand=(214, 220, 230), zak=True):
    B, h, L, y0, zf, ky0 = d.B, d.h, d.L, d.y0, d.zf, d.ky0
    yb = max(0.9, y0 - 0.6)

    def voor(rgb, X, Y, Z, f):
        if f == "north" and zak:
            yc = yb + (ky0 - 1.7 - yb) * 0.42
            rgb = leg(rgb, (np.abs(X) < 1.6) & (np.abs(Y - yc) < 0.95) & ~((np.abs(X) < 1.35) & (np.abs(Y - yc + 0.1) < 0.75)), rand)
        return rgb
    return [bot("x_schort", "lijf", [0, y0 + h, zf], [
        blok(atlas, [-B / 2 - 0.5, ky0 - 1.8, zf - 1.35], [B + 1.0, 1.4, 2.8], effen(rand), 5),
        blok(atlas, [-B / 2 + 0.4, yb, zf - 2.1], [B - 0.8, ky0 - 1.7 - yb, 0.5], _stof(kleur, rand, yb + 0.45, voor), 5),
        blok(atlas, [-B / 2 - 0.95, yb + 0.5, zf - 0.5], [B + 1.9, h * 0.62, L * 0.4], _stof(kleur, rand, yb + 0.95), 5)])]


def x_pet(atlas, d, kleur=MARINE, top=WIT):
    """The captain's cap: a navy band with a gold anchor, a white top, a black peak."""
    kb, kd, y, zf = d.kb, d.kd, d.ky1 + 0.8, d.kzf
    w = kb - 2.6

    def band(X, Y, Z, f):
        rgb = _vol(X, kleur)
        if f == "north":
            cy = y + 0.9
            anker = ((np.abs(X) < 0.16) & (np.abs(Y - cy) < 0.62)) | ((np.abs(Y - cy - 0.35) < 0.14) & (np.abs(X) < 0.5)) | \
                    ((np.abs(np.hypot(X, Y - cy + 0.1) - 0.55) < 0.15) & (Y < cy - 0.1))
            rgb = leg(rgb, anker, GOUD)
        return leg(rgb, Y < y + 0.25, GOUD) if f != "up" else rgb
    return [bot("x_pet", "kop", [0, y, 0], [
        blok(atlas, [-w / 2, y, zf + 0.5], [w, 1.7, kd - 1.8], band, 5),
        blok(atlas, [-w / 2 - 0.7, y + 1.7, zf - 0.1], [w + 1.4, 1.5, kd - 0.8], effen(top), 5),
        blok(atlas, [-w / 2 + 0.3, y, zf - 1.6], [w - 0.6, 0.55, 2.2], effen((34, 34, 44)), 4)])]


def x_platte_pet(atlas, d, kleur=(120, 132, 96), ruit=(92, 104, 74)):
    """A tweed flat cap."""
    kb, kd, y, zf = d.kb, d.kd, d.ky1 + 0.8, d.kzf

    def tweed(X, Y, Z, f):
        u, v = (X, Z) if f in ("up", "down") else ((X, Y) if f in ("north", "south") else (Z, Y))
        return leg(_vol(X, kleur), (np.floor(u * 1.1 + 40) + np.floor(v * 1.1 + 40)) % 2 == 0, ruit)
    return [bot("x_platte_pet", "kop", [0, y, 0], [
        blok(atlas, [-kb / 2 + 0.9, y, zf + 0.2], [kb - 1.8, 1.5, kd - 1.2], tweed, 5),
        blok(atlas, [-kb / 2 + 1.9, y + 1.5, zf + 1.6], [kb - 3.8, 0.7, kd - 3.4], tweed, 5),
        blok(atlas, [-kb / 2 + 1.7, y, zf - 1.7], [kb - 3.4, 0.6, 2.0], tweed, 5),
        blok(atlas, [-0.5, y + 2.2, zf + kd / 2 - 0.5], [1.0, 0.4, 1.0], effen(ruit), 4)])]


def _stro(kleur=(238, 204, 112), donker=(206, 164, 76)):
    def k(X, Y, Z, f):
        u, v = (X, Z) if f in ("up", "down") else ((X, Y) if f in ("north", "south") else (Z, Y))
        return leg(_vol(X, kleur), ((np.floor(u * 1.0 + 40) + np.floor(v * 2.0 + 40)) % 2 == 0), donker, 0.6)
    return k


def x_strohoed(atlas, d, band=(226, 64, 70)):
    kb, kd, y, zf = d.kb, d.kd, d.ky1 + 0.8, d.kzf
    return [bot("x_strohoed", "kop", [0, y, 0], [
        blok(atlas, [-kb / 2 - 1.6, y, zf - 1.9], [kb + 3.2, 0.6, kd + 3.6], _stro(), 5),
        blok(atlas, [-kb / 2 + 2.0, y + 0.6, zf + 1.2], [kb - 4.0, 2.7, kd - 2.6], _stro(), 5),
        blok(atlas, [-kb / 2 + 1.85, y + 0.6, zf + 1.05], [kb - 3.7, 1.0, kd - 2.3], effen(band), 5)])]


def x_zuidwester(atlas, d, kleur=(250, 206, 60)):
    kb, kd, y, zf = d.kb, d.kd, d.ky1 + 0.8, d.kzf
    donker = tint(kleur, 0.84)
    return [bot("x_zuidwester", "kop", [0, y, 0], [
        blok(atlas, [-kb / 2 + 1.3, y + 0.5, zf + 0.6], [kb - 2.6, 2.3, kd - 1.6], effen(kleur), 5),
        blok(atlas, [-kb / 2 + 2.4, y + 2.8, zf + 1.6], [kb - 4.8, 0.8, kd - 3.6], effen(kleur), 5),
        blok(atlas, [-kb / 2 - 0.2, y, zf - 1.2], [kb + 0.4, 0.55, kd + 1.6], effen(donker), 5),
        blok(atlas, [-kb / 2 + 0.6, y - 0.5, zf + kd + 0.4], [kb - 1.2, 0.55, 2.6], effen(donker), 5)])]


def x_bakkersmuts(atlas, d):
    kd, y, zf = d.kd, d.ky1 + 0.8, d.kzf

    def plooi(X, Y, Z, f):
        m = (np.floor((X if f in ("north", "south") else Z) * 1.0 + 40) % 2 == 0) & (f not in ("up", "down"))
        return leg(_vol(X, WIT), m, (226, 228, 236))
    return [bot("x_bakkersmuts", "kop", [0, y, 0], [
        blok(atlas, [-2.4, y, zf + 1.8], [4.8, 1.6, kd - 3.4], plooi, 4),
        blok(atlas, [-3.3, y + 1.6, zf + 1.1], [6.6, 2.8, kd - 2.0], effen(WIT), 4),
        blok(atlas, [-2.5, y + 4.4, zf + 1.9], [5.0, 0.8, kd - 3.6], effen(WIT), 4)])]


def x_strik(atlas, d, kleur=(240, 110, 150)):
    y, z, x = d.ky1 + 0.6, d.kzf + 0.6, d.kb / 2 - 3.6
    donker = tint(kleur, 0.8)
    return [bot("x_strik", "kop", [x, y, z], [
        blok(atlas, [x - 2.3, y, z], [2.0, 2.4, 1.3], effen(kleur), 5), blok(atlas, [x + 0.7, y, z], [2.0, 2.4, 1.3], effen(kleur), 5),
        blok(atlas, [x - 0.5, y + 0.5, z - 0.2], [1.4, 1.4, 1.6], effen(donker), 5)], [0, 0, -8])]


def x_bloem(atlas, d, kleur=(255, 150, 190), hart=(255, 220, 90)):
    """A flower tucked behind one ear."""
    x, y, z = d.kb / 2 - 0.6, d.ky1 - 0.6, d.kzf + 0.2

    def k(X, Y, Z, f):
        a = _rgba(X)
        u, v = X - x - 1.5, Y - y - 1.5
        _zet(a, np.hypot(u, v) < 1.0 + 0.5 * np.cos(5 * np.arctan2(v, u)), kleur)
        _zet(a, np.hypot(u, v) < 0.5, hart)
        return a
    return [bot("x_bloem", "kop", [x, y, z], [blok(atlas, [x, y, z - 0.5], [3.0, 3.0, 0.6], k, 4, alleen=("north", "south")),
                                              blok(atlas, [x + 1.0, y + 1.0, z - 0.75], [1.0, 1.0, 0.4], effen(hart), 4)])]


def x_botje(atlas, d):
    """A bone in the corner of the mouth, like a pipe."""
    y, z, x = d.sy0 + d.sh * 0.3, d.sz + 0.15, d.sb / 2 - 0.3
    bot_k = effen((250, 244, 228))
    return [bot("x_botje", "snuit", [x, y, z], [
        blok(atlas, [x, y, z], [3.0, 0.8, 0.8], bot_k, 4),
        blok(atlas, [x + 2.6, y - 0.35, z - 0.3], [0.9, 0.85, 0.85], bot_k, 4), blok(atlas, [x + 2.6, y + 0.35, z + 0.3], [0.9, 0.85, 0.85], bot_k, 4)],
                [0, -18, 14])]


EXTRAS.update(bril=x_bril, spiegel=x_spiegel, jas=x_jas, cape=x_cape, sjaal=x_sjaal, bandana=x_bandana, medaille=x_medaille, tas=x_tas,
              rugzak=x_rugzak, schort=x_schort, pet=x_pet, platte_pet=x_platte_pet, strohoed=x_strohoed, zuidwester=x_zuidwester,
              bakkersmuts=x_bakkersmuts, strik=x_strik, bloem=x_bloem, botje=x_botje)

# =====================================================================================================================
# DE BEWONERS: breed + coat + accessories (+ mood, an old dog's grey snout). One line each.
# =====================================================================================================================
B_ = lambda id, naam, rol, ras, kleur, uiterlijk, extra, **k: dict(id=id, naam=naam, rol=rol, ras=ras, kleur=kleur, uiterlijk=uiterlijk, extra=extra, **k)
BEWONERS = [
    B_("dokter", "Dokter Pleisterpoot", "de dorpsdokter", "teckel", "rood",
       "oude teckel met grijze snuit, witte doktersjas met rood kruis, ronde gouden bril, voorhoofdspiegel",
       [("jas", dict(kruis=True)), ("bril", {}), ("spiegel", {})], grijs=True, halsband=None, tong=False),
    B_("trainer", "Meester Truffelneus", "de snuffeltrainer in de wei", "speurhond", "bruin",
       "grote speurhond met lange oren en een wijze blik, groene cape met gouden rand, gouden snuffelmedaille, tweed pet",
       [("cape", dict(deel=0.42)), ("medaille", {}), ("platte_pet", {})], blik="wijs", halsband=None, tong=False),
    B_("kapitein", "Kapitein Zoutsnoet", "de kapitein van de boot", "mops", "beige",
       "stevige mops, marineblauwe jas met gouden knopen, kapiteinspet met anker, een botje als pijp",
       [("jas", dict(kleur=MARINE, rand=GOUD, lang=0.8)), ("pet", {}), ("botje", {})], halsband=None, tong=False),
    B_("redder", "Jutje Kwispel", "de strandjutter die jou op het strand vindt", "golden", "goud",
       "vrolijke golden retriever, rode bandana met stippen, juttertas met schelp, bloem achter het oor",
       [("bandana", {}), ("tas", dict(teken="schelp", kant=-1)), ("bloem", {})], halsband=None),
    B_("vader", "Papa Zwerfpoot", "je vader (krijgt het ras en de kleur van de speler)", "shiba", "rood",
       "zelfde ras als jij, grijze snuit, blauwe gestreepte sjaal, rugzak met dekenrol",
       [("sjaal", {}), ("rugzak", {})], grijs=True, halsband=None, tong=False),
    B_("bakker", "Bakker Kruimelsnuit", "bakker; is zijn deegroller kwijt", "corgi", "rood",
       "corgi met wit schort en bakkersmuts tussen de oren",
       [("schort", {}), ("bakkersmuts", {})], halsband=None),
    B_("visser", "Visser Natneus", "visser; is zijn lievelingsdobber kwijt", "jackrussell", "zwart",
       "jack russell met gele zuidwester en gele regenjas",
       [("jas", dict(kleur=(250, 206, 60), rand=(214, 164, 40), knoop=(60, 60, 70))), ("zuidwester", {})], halsband=None),
    B_("juf", "Juf Blaffetje", "juf van het schooltje; is de schoolbel kwijt", "shiba", "creme",
       "crème shiba met ronde rode bril, roze strik en paarse halsband",
       [("bril", dict(kleur=(214, 70, 90))), ("strik", {})], halsband=(150, 110, 200), tong=False),
    B_("oma", "Oma Wolletje", "breit voor het hele dorp; is haar bol wol kwijt", "golden", "licht",
       "lichte golden met grijze snuit, lila gebreide omslagdoek met franje, klein brilletje",
       [("cape", dict(kleur=(190, 160, 220), rand=(236, 220, 246), deel=0.6, gebreid=True)), ("bril", dict(kleur=(170, 170, 184)))],
       grijs=True, halsband=None, blik="wijs", tong=False),
    B_("tuinder", "Tuinder Knolletje", "moestuin; is zijn gietertje kwijt", "teckel", "zwart",
       "zwart-tan teckel met strohoed en groen tuinschort",
       [("strohoed", dict(band=(90, 150, 90))), ("schort", dict(kleur=(104, 160, 104), rand=(70, 118, 76)))], halsband=None),
    B_("pup", "Kleine Kwijlebal", "puppy van het plein; is zijn bal kwijt", "mops", "abrikoos",
       "mopspuppy met blauwe bandana",
       [("bandana", dict(kleur=(70, 130, 210)))], pup=True, halsband=None),
]


def figuur_bewoner(b, seed=None, **over):
    k = dict(pup=b.get("pup", False), extra=b["extra"], blik=b.get("blik", "open"), halsband=b.get("halsband", (220, 60, 70)),
             grijs=b.get("grijs", False), tong=b.get("tong", True), naam="snuffel_" + b["id"])
    ras, kleur = over.pop("ras", b["ras"]), over.pop("kleur", b["kleur"])
    k.update(over)
    return figuur_hond(ras, kleur, seed or 400 + [x["id"] for x in BEWONERS].index(b["id"]), maat=192, **k)


# =====================================================================================================================
# HET MAATJE: three concepts (built at double size for a fine face; the renderer draws them at half scale)
# =====================================================================================================================
MAATJE_SCHAAL = 0.5


def mond(rgb, glad, X, Y, mx, my, soort, lijn=(96, 50, 60)):
    if soort == "blij":                                               # an open laughing mouth with a tongue
        m = (np.hypot((X - mx) / 1.5, (Y - my) / 1.35) < 1) & (Y < my)
        rgb = leg(rgb, m, (140, 44, 66))
        rgb = leg(rgb, m & (np.hypot((X - mx) / 1.0, (Y - my + 1.45) / 0.75) < 1), (250, 130, 160))
        rgb = leg(rgb, (np.abs(Y - my) < 0.16) & (np.abs(X - mx) < 1.7), lijn)
    else:                                                             # a crooked grin with one little fang
        lijn_y = my - 0.5 + 0.34 * (mx - X) + 0.5 * np.clip((np.abs(X - mx) - 0.9), 0, 9)
        m = (np.abs(Y - lijn_y) < 0.2) & (np.abs(X - mx) < 1.9)
        rgb = leg(rgb, m, lijn)
        tand = (np.abs(X - (mx - 0.9)) < 0.34 * np.clip((Y - (lijn_y - 0.85)) / 0.85, 0, 1)) & (Y < lijn_y) & (Y > lijn_y - 0.85)
        rgb = leg(rgb, tand, (255, 255, 250))
        m = m | tand
    return rgb, np.maximum(glad, m.astype(np.float32))


def _snoet(basis, ex, ey, r, my, gezicht, lid, extra=None):
    """A companion's face on the north side of its body cube."""
    def k(X, Y, Z, f):
        rgb = basis(X, Y, Z, f)
        glad = np.zeros(X.shape, np.float32)
        if f != "north":
            return rgb, glad
        if extra:
            rgb = extra(rgb, X, Y)
        for s in (1, -1):
            rgb = blos(rgb, X, Y, s * ex, ey, r, s, 0.7)
            rgb, glad = oog(rgb, glad, X, Y, s * ex, ey, r, s, "blij" if gezicht == "blij" else "ondeugend", lid)
            if gezicht != "blij":                                     # one raised brow
                wy = ey + r * (1.3 if s < 0 else 1.08) + 0.25 * (X - s * ex) * (1 if s < 0 else -0.6)
                rgb = leg(rgb, (np.abs(Y - wy) < 0.22) & (np.abs(X - s * ex) < r * 0.75), tint(lid, 0.6))
        return mond(rgb, glad, X, Y, 0, my, gezicht)
    return k


def _wolk(kleur, schaduw, n=1.0):
    """Fluff: soft darker dabs."""
    def k(X, Y, Z, f):
        t = np.sin(X * 1.3 * n + Y * 0.9 + 1) * np.sin(Y * 1.4 * n + Z * 1.1) * np.sin(Z * 1.2 * n + X * 0.7 + 2)
        return leg(_vol(X, kleur), t > 0.18, schaduw, 0.75)
    return k


def _blad(u0, u1, v0, v1, kleur=(112, 196, 92), nerf=(70, 150, 70), as_="zy"):
    """A leaf painted on a thin plate: a pointed oval from u0 (stalk) to u1 (tip), a midrib, see-through around it.
    as_ = which model axes are (along, across): "yz", "xz", "zx"; u1 < u0 is fine (the leaf then points the other way)."""
    def k(X, Y, Z, f):
        u, v = {"yz": (Y, Z), "xz": (X, Z), "zx": (Z, X), "yx": (Y, X)}[as_]
        t = np.clip((u - u0) / (u1 - u0), 0, 1)
        half = (v1 - v0) / 2 * np.sin(t * math.pi) ** 0.7 * (1 - 0.25 * t)
        a = _rgba(X)
        m = (np.abs(v - (v0 + v1) / 2) < half) & (t > 0) & (t < 1)
        _zet(a, m, kleur)
        _zet(a, m & (np.abs(v - (v0 + v1) / 2) < 0.2), nerf)
        return a
    return k


def maatje_a(atlas, gezicht="blij"):
    """A. Zweefzaadje: a ball of seed fluff with a little seed dangling under it and a leaf for a sail."""
    pluis, schaduw = (255, 251, 240), (230, 236, 220)
    w = _wolk(pluis, schaduw)
    zaad, zaad_d = effen((170, 118, 76)), effen((132, 88, 60))
    lijf = [blok(atlas, [-6, 6, -5.5], [12, 11, 11], _snoet(w, 2.9, 12.2, 2.5, 8.6, gezicht, (226, 230, 214)), 5),
            blok(atlas, [-4.5, 17, -4], [9, 1.5, 8], w, 5), blok(atlas, [-4.5, 4.7, -4], [9, 1.3, 8], w, 5),
            blok(atlas, [-4.5, 8, 5.5], [9, 7.5, 1.5], w, 5),
            blok(atlas, [-3, 18.5, -2.6], [2.2, 1.5, 2.2], w, 5), blok(atlas, [1.2, 18.5, 0.6], [2.4, 1.2, 2.2], w, 5)]
    for s in (1, -1):
        for cb in (blok(atlas, [6, 7.6, -4], [1.5, 7.6, 8], w, 5), blok(atlas, [7.5, 9.4, -2.2], [1.1, 3.6, 4.4], w, 5)):
            lijf.append(cb if s > 0 else spiegel(cb))
    bones = [bot("root", None, [0, 0, 0], []), bot("lijf", "root", [0, 11, 0], lijf),
             bot("zaadje", "lijf", [0, 5, 0], [blok(atlas, [-0.5, 3.6, -0.5], [1, 1.4, 1], zaad_d, 4),
                                               blok(atlas, [-1.9, 1.5, -1.9], [3.8, 2.4, 3.8], zaad, 5), blok(atlas, [-1.0, 0, -1.0], [2, 1.6, 2], zaad_d, 5)]),
             bot("zeil", "lijf", [0, 17.5, 2], [blok(atlas, [-0.4, 17.4, -1.5], [0.8, 10, 8], _blad(17.4, 27.4, -1.5, 6.5, as_="yz"), 5, alleen=("east", "west")),
                                                blok(atlas, [-0.3, 17.2, 2.0], [0.6, 2.2, 0.9], effen((70, 150, 70)), 4)], [14, 38, 0])]
    for kant, s in (("links", 1), ("rechts", -1)):
        hand = blok(atlas, [6.8 if s > 0 else -10.2, 8.6, -3.2], [3.4, 1.5, 2.8], effen((112, 196, 92)), 6)
        bones.append(bot(f"hand_{kant}", "lijf", [s * 7, 9.4, -1.8], [hand], [0, 0, s * (-24 if gezicht == "blij" else 10)]))
    return geo("snuffel_maatje_a", bones, 1.2, 1.6)


def maatje_b(atlas, gezicht="blij"):
    """B. Mos-eikeltje: an acorn imp with a scaly cap pulled over its brow, a tuft of moss and two twig arms."""
    noot, noot_l, dop, dop_d, mos, tak = (226, 172, 104), (246, 208, 150), (132, 86, 58), (100, 62, 44), (120, 180, 84), (120, 82, 56)

    def k_noot(X, Y, Z, f):
        rgb = leg(_vol(X, noot), np.abs(np.sin((X + Z) * 1.1)) < 0.12, tint(noot, 0.9))
        if f == "north":
            rgb = leg(rgb, np.hypot(X / 4.4, (Y - 5.2) / 3.4) < 1, noot_l, 0.8)
        return rgb

    def k_dop(X, Y, Z, f):
        u, v = (X, Z) if f in ("up", "down") else ((X, Y) if f in ("north", "south") else (Z, Y))
        s1, s2 = (u + v) * 0.72, (u - v) * 0.72
        rgb = leg(_vol(X, dop), (np.abs(s1 - np.round(s1)) < 0.11) | (np.abs(s2 - np.round(s2)) < 0.11), dop_d)
        return leg(rgb, (np.abs(s1 - np.round(s1) - 0.3) < 0.1) & (np.abs(s2 - np.round(s2) - 0.3) < 0.1), tint(dop, 1.25))
    w_mos = _wolk(mos, (84, 146, 66), 1.4)
    lijf = [blok(atlas, [-5.5, 3, -5.5], [11, 10, 11], _snoet(k_noot, 2.75, 8.4, 2.45, 5.0, gezicht, tint(noot, 0.88)), 5),
            blok(atlas, [-4, 1.5, -4], [8, 1.5, 8], k_noot, 5), blok(atlas, [-1.6, 0, -1.6], [3.2, 1.5, 3.2], effen(tint(noot, 0.8)), 5)]
    hoed = [blok(atlas, [-6.9, 12.2, -6.9], [13.8, 4.2, 13.8], k_dop, 5), blok(atlas, [-5, 16.4, -5], [10, 2, 10], k_dop, 5),
            blok(atlas, [-7.5, 13.6, 0.5], [0.9, 3.0, 5.0], w_mos, 6), blok(atlas, [1.4, 18.4, 0.8], [4.0, 1.5, 3.8], w_mos, 6),
            blok(atlas, [3.4, 11.3, -7.4], [3.4, 1.8, 0.9], w_mos, 6)]
    bones = [bot("root", None, [0, 0, 0], []), bot("lijf", "root", [0, 8, 0], lijf),
             bot("dop", "lijf", [0, 12.4, 0], hoed, [0, 0, -6 if gezicht != "blij" else 0]),
             bot("steel", "dop", [-1, 18.4, 0], [blok(atlas, [-2.2, 18.2, -1], [2, 3.4, 2], effen(tak), 5),
                                                 blok(atlas, [-1.6, 20.9, -1.8], [4.6, 0.7, 3.6], _blad(-1.2, 3.0, -1.8, 1.8, as_="xz"), 5, alleen=("up", "down"))], [0, 0, 16])]
    for kant, s in (("links", 1), ("rechts", -1)):
        arm = [blok(atlas, [5.5, 6.2, -0.6], [4.6, 1.2, 1.2], effen(tak), 5), blok(atlas, [8.9, 7.4, -0.6], [1.2, 2.4, 1.2], effen(tak), 5),
               blok(atlas, [9.9, 5.0, -0.6], [1.2, 1.4, 1.2], effen(tak), 5)]
        bones.append(bot(f"arm_{kant}", "lijf", [s * 5.5, 6.8, 0], arm if s > 0 else [spiegel(q) for q in arm],
                         [0, 0, s * (-30 if (gezicht == "blij" or s > 0) else 28)]))
    return geo("snuffel_maatje_b", bones, 1.3, 1.5)


def maatje_c(atlas, gezicht="blij"):
    """C. Zonnepluisje: a glowing dandelion head, petals standing out all around, a wisp of a tail that ends in a curl."""
    geel, licht, oranje, groen = (255, 208, 84), (255, 240, 176), (250, 150, 50), (150, 206, 104)
    cy = 12.4

    def k_bol(X, Y, Z, f):
        rgb = _vol(X, geel)
        if f == "north":
            rgb = leg(rgb, np.hypot(X / 4.9, (Y - cy + 0.4) / 4.6) < 1, licht, 0.85)
        return rgb

    def stralen(as_):
        def k(X, Y, Z, f):
            u, v = {"xy": (X, Y - cy), "zy": (Z, Y - cy), "xz": (X, Z)}[as_]
            rr, hoek = np.hypot(u, v), np.arctan2(v, u)
            a = _rgba(X)
            lang = 8.4 + 1.5 * np.abs(np.sin(hoek * 4 + (0.4 if as_ != "xy" else 0)))
            m = (rr < lang) & (np.abs(np.sin(hoek * 4 + (0.4 if as_ != "xy" else 0))) > 0.22)
            if as_ == "zy":                                           # nothing sticks out in front of the face
                m &= ~((Z < -4.9) & (np.abs(Y - cy) < 5.6))
            elif as_ == "xz":
                m &= ~((Z < -4.9) & (np.abs(X) < 5.8))
            a[m, :3] = np.array(geel, np.float32) + (np.array(oranje, np.float32) - np.array(geel, np.float32)) * np.clip((rr[m][:, None] - 5.5) / 4.5, 0, 1)
            a[m, 3] = 255
            return a
        return k
    lijf = [blok(atlas, [-5.5, cy - 5.3, -5], [11, 10.6, 10], _snoet(k_bol, 2.75, cy + 0.6, 2.45, cy - 2.9, gezicht, tint(geel, 0.9)), 5),
            blok(atlas, [-10.2, cy - 10.2, -0.5], [20.4, 20.4, 1.2], stralen("xy"), 4),
            blok(atlas, [-0.6, cy - 10.2, -10.2], [1.2, 20.4, 20.4], stralen("zy"), 4),
            blok(atlas, [-10.2, cy - 0.6, -10.2], [20.4, 1.2, 20.4], stralen("xz"), 4)]

    def k_staart(X, Y, Z, f):
        t = np.clip((Z - 3) / 9, 0, 1)[..., None]
        return np.array(licht, np.float32) * (1 - t) + np.array(groen, np.float32) * t
    bones = [bot("root", None, [0, 0, 0], []), bot("lijf", "root", [0, cy, 0], lijf),
             bot("staart", "lijf", [0, cy - 4, 4], [blok(atlas, [-1.7, cy - 7.0, 3.6], [3.4, 3.4, 4.6], k_staart, 5),
                                                    blok(atlas, [-1.3, cy - 9.6, 6.6], [2.6, 3.2, 3.0], k_staart, 5),
                                                    blok(atlas, [-1.0, cy - 12.0, 8.2], [2.0, 2.8, 2.6], k_staart, 5),
                                                    blok(atlas, [-0.8, cy - 12.4, 5.4], [1.6, 1.8, 3.0], effen(groen), 5),
                                                    blok(atlas, [-0.7, cy - 11.2, 4.6], [1.4, 1.8, 1.4], effen(groen), 5)])]
    for i, (x, y, z) in enumerate(((9.5, cy + 8, -3), (-11, cy + 4.5, 2), (7, cy - 9, -5))):
        bones.append(bot(f"vonk_{i}", "root", [x, y, z], [blok(atlas, [x - 0.8, y - 0.8, z - 0.8], [1.6, 1.6, 1.6], effen((255, 250, 226)), 3)]))
    return geo("snuffel_maatje_c", bones, 1.6, 1.7)


MAATJES = {"a": ("Zweefzaadje", maatje_a), "b": ("Mos-eikeltje", maatje_b), "c": ("Zonnepluisje", maatje_c)}


# =====================================================================================================================
# HET BOOMPJE in four steps
# =====================================================================================================================
GROEN, GROEN_L, GROEN_D, STAM, AARDE, BLOESEM = (104, 190, 92), (160, 224, 120), (70, 150, 76), (140, 96, 64), (124, 86, 62), (255, 170, 204)


def _loof(X, Y, Z, f):
    t = np.sin(X * 1.9 + Y * 1.3 + 1) * np.sin(Y * 1.7 + Z * 1.5) * np.sin(Z * 1.8 + X * 0.9 + 2)
    return leg(leg(_vol(X, GROEN), t > 0.2, GROEN_L, 0.8), t < -0.25, GROEN_D, 0.7)


def _voet(atlas):
    def aarde(X, Y, Z, f):
        return leg(_vol(X, AARDE), np.sin(X * 2.3 + Z * 1.7) * np.sin(Z * 2.9 + Y) > 0.4, tint(AARDE, 0.8))
    steen = effen((176, 176, 186))
    cubes = [blok(atlas, [-3.5, 0, -3.5], [7, 0.9, 7], aarde, 8), blok(atlas, [-2.5, 0.9, -2.5], [5, 0.6, 5], aarde, 8)]
    for x, z, m in ((-4.6, -2.0, 1.5), (3.3, -4.4, 1.3), (3.6, 2.4, 1.6), (-3.2, 3.6, 1.2), (-0.6, -5.0, 1.1), (0.4, 4.2, 1.3)):
        cubes.append(blok(atlas, [x, 0, z], [m, m * 0.7, m], steen, 8))
    return bot("voet", "root", [0, 0, 0], cubes)


def _blaadje(atlas, naam, x, y, z, lang, breed, hoek, parent="stam"):
    """A chunky leaf growing sideways from (x, y, z) (lang < 0: to the other side); hoek = how far it stands up."""
    s = 1 if lang > 0 else -1

    def k(X, Y, Z, f):
        return leg(_vol(X, GROEN), (np.abs(Z - z) < 0.22) & (f == "up"), GROEN_D) if f != "down" else _vol(X, GROEN_D)
    a, t = abs(lang) * 0.68, abs(lang) * 0.32
    return bot(naam, parent, [x, y, z], [blok(atlas, [min(x, x + s * a), y, z - breed / 2], [a, 0.9, breed], k, 6),
                                         blok(atlas, [min(x + s * a, x + s * (a + t)), y, z - breed * 0.3], [t, 0.9, breed * 0.6], k, 6)], [0, 0, -s * hoek])


def _bloesem(atlas, x, y, z, m=1.5):
    def k(X, Y, Z, f):
        u, v = (X - x, Z - z) if f in ("up", "down") else ((X - x, Y - y) if f in ("north", "south") else (Z - z, Y - y))
        return leg(_vol(X, BLOESEM), np.hypot(u, v) < m * 0.26, (255, 226, 110))
    return blok(atlas, [x - m / 2, y - m / 2, z - m / 2], [m, m, m], k, 4)


def boompje(atlas, stap):
    """stap 1 kiem, 2 scheutje, 3 struikje, 4 jong boompje."""
    stengel = effen((130, 200, 104))

    def stam(X, Y, Z, f):
        return leg(_vol(X, STAM), np.abs(np.sin((X + Z) * 2.2 + Y * 0.3)) < 0.2, tint(STAM, 0.8))
    bones = [bot("root", None, [0, 0, 0], []), _voet(atlas)]
    if stap == 1:
        bones.append(bot("stam", "root", [0, 1.5, 0], [blok(atlas, [-0.5, 1.5, -0.5], [1, 2.6, 1], stengel, 5)]))
        bones += [_blaadje(atlas, "blad_1", 0.3, 3.7, 0, 3.6, 3.2, 38), _blaadje(atlas, "blad_2", -0.3, 3.7, 0, -3.6, 3.2, 38)]
    elif stap == 2:
        bones.append(bot("stam", "root", [0, 1.5, 0], [blok(atlas, [-0.6, 1.5, -0.6], [1.2, 6.5, 1.2], stengel, 5),
                                                      blok(atlas, [-0.9, 7.8, -0.9], [1.8, 1.6, 1.8], effen(GROEN_L), 6)]))
        bones += [_blaadje(atlas, "blad_1", 0.4, 3.6, 0, 4.4, 3.6, 34), _blaadje(atlas, "blad_2", -0.4, 3.6, 0, -4.4, 3.6, 34),
                  _blaadje(atlas, "blad_3", 0.4, 6.4, 0.2, 3.4, 3.0, 44), _blaadje(atlas, "blad_4", -0.4, 6.4, -0.2, -3.4, 3.0, 44)]
    elif stap == 3:
        bones.append(bot("stam", "root", [0, 1.5, 0], [blok(atlas, [-0.8, 1.5, -0.8], [1.6, 4.5, 1.6], stam, 6)]))
        bones.append(bot("kruin", "stam", [0, 6, 0], [blok(atlas, [-3.5, 5.2, -3.5], [7, 5.2, 7], _loof, 6), blok(atlas, [-2.4, 10.4, -2.4], [4.8, 1.6, 4.8], _loof, 6),
                                                      blok(atlas, [3.5, 6.0, -2.2], [1.8, 3.4, 4.2], _loof, 6), blok(atlas, [-5.3, 6.4, -1.8], [1.8, 3.0, 4.0], _loof, 6),
                                                      blok(atlas, [-2.2, 6.0, 3.5], [4.4, 3.4, 1.6], _loof, 6), blok(atlas, [-2.0, 6.2, -5.0], [4.2, 3.0, 1.5], _loof, 6),
                                                      _bloesem(atlas, 1.6, 11.6, -1.4)]))
    else:
        bones.append(bot("stam", "root", [0, 1.5, 0], [blok(atlas, [-1.1, 1.5, -1.1], [2.2, 9.5, 2.2], stam, 6), blok(atlas, [-1.5, 1.5, -1.5], [3.0, 1.4, 3.0], stam, 6)]))
        bones.append(bot("tak", "stam", [1, 7, 0], [blok(atlas, [1.0, 6.6, -0.6], [3.2, 1.2, 1.2], stam, 6), blok(atlas, [3.2, 7.4, -1.6], [3.0, 2.8, 3.0], _loof, 6)], [0, 0, 18]))
        kruin = [blok(atlas, [-5.5, 10.2, -5.5], [11, 7.4, 11], _loof, 6), blok(atlas, [-4, 17.6, -4], [8, 2.2, 8], _loof, 6),
                 blok(atlas, [-2.2, 19.8, -2.2], [4.4, 1.4, 4.4], _loof, 6),
                 blok(atlas, [5.5, 11.4, -3.6], [2.0, 5.0, 7.2], _loof, 6), blok(atlas, [-7.5, 11.4, -3.6], [2.0, 5.0, 7.2], _loof, 6),
                 blok(atlas, [-3.6, 11.4, 5.5], [7.2, 5.0, 2.0], _loof, 6), blok(atlas, [-3.6, 11.4, -7.5], [7.2, 5.0, 2.0], _loof, 6),
                 blok(atlas, [-3.4, 8.9, -3.4], [6.8, 1.3, 6.8], _loof, 6)]
        for x, y, z in ((-2.6, 20.0, -3.6), (4.6, 17.9, -1.0), (-6.6, 15.0, -4.6), (2.4, 13.2, -7.9), (7.9, 13.8, 1.6), (-1.0, 21.6, 0.6), (-4.4, 12.2, -7.9), (6.0, 11.6, -5.9)):
            kruin.append(_bloesem(atlas, x, y, z))
        bones.append(bot("kruin", "stam", [0, 10.5, 0], kruin))
    return geo(f"snuffel_boompje_{stap}", bones, 1.4, 1.6)


STAPPEN = {1: "kiem", 2: "scheutje", 3: "struikje", 4: "jong boompje"}


# =====================================================================================================================
# animations
# =====================================================================================================================
def kf(*pairs):
    return {str(t): list(v) for t, v in pairs}


def anim(length, bones, loop=True):
    return {"loop": loop, "animation_length": length, "bones": bones}


SNUFFEL_HOEK = (8, 34, -26)       # the body tips forward, the head goes down to the ground, the tail goes up
ZIT = (-1, -78)                     # the sign of the body's tip, how far the hind legs fold forward


def zit_hoek(d):
    """How far the body tips back so that the hips reach the ground when the dog sits (degrees)."""
    return math.degrees(math.asin(min(0.5, max(0.05, (d.pl - 0.4) / (2 * max(1.0, d.L / 2 - d.pd))))))


def hond_anims(d):
    poten = ("poot_lv", "poot_rv", "poot_la", "poot_ra")
    idle = {"lijf": {"scale": kf((0, (1, 1, 1)), (1.5, (1.02, 1.03, 1.02)), (3, (1, 1, 1)))},
            "staart": {"rotation": kf((0, (0, 0, 0)), (0.75, (0, 12, 0)), (2.25, (0, -12, 0)), (3, (0, 0, 0)))},
            "oor_links": {"rotation": kf((0, (0, 0, 0)), (2.4, (0, 0, 0)), (2.55, (0, 0, -14)), (2.7, (0, 0, 0)))},
            "oor_rechts": {"rotation": kf((0, (0, 0, 0)), (1.2, (0, 0, 0)), (1.35, (0, 0, 14)), (1.5, (0, 0, 0)))},
            "kop": {"rotation": kf((0, (0, 0, 0)), (1.5, (2, 0, 0)), (3, (0, 0, 0)))}}
    walk = {n: {"rotation": kf((0, (s, 0, 0)), (0.3, (-s, 0, 0)), (0.6, (s, 0, 0)))} for n, s in zip(poten, (30, -30, -30, 30))}
    walk["lijf"] = {"position": kf((0, (0, 0, 0)), (0.15, (0, 0.5, 0)), (0.3, (0, 0, 0)), (0.45, (0, 0.5, 0)), (0.6, (0, 0, 0)))}
    walk["staart"] = {"rotation": kf((0, (0, 16, 0)), (0.3, (0, -16, 0)), (0.6, (0, 16, 0)))}
    walk["kop"] = {"rotation": kf((0, (0, 0, 2)), (0.3, (0, 0, -2)), (0.6, (0, 0, 2)))}
    sn = SNUFFEL_HOEK
    snuffel = {"lijf": {"rotation": kf((0, (sn[0], 0, 0)), (1.2, (sn[0], 0, 0)))},
               "kop": {"rotation": kf((0, (sn[1], 0, 0)), (0.15, (sn[1] + 5, 0, 0)), (0.3, (sn[1], 0, 0)), (0.45, (sn[1] + 5, 0, 0)), (0.6, (sn[1], 6, 0)),
                                      (0.75, (sn[1] + 5, 6, 0)), (0.9, (sn[1], -6, 0)), (1.05, (sn[1] + 5, -6, 0)), (1.2, (sn[1], 0, 0)))},
               "snuit": {"scale": kf((0, (1, 1, 1)), (0.15, (1.06, 1.06, 1.04)), (0.3, (1, 1, 1)), (0.45, (1.06, 1.06, 1.04)), (0.6, (1, 1, 1)))},
               "staart": {"rotation": kf((0, (sn[2], 14, 0)), (0.3, (sn[2], -14, 0)), (0.6, (sn[2], 14, 0)), (0.9, (sn[2], -14, 0)), (1.2, (sn[2], 14, 0)))}}
    for n in poten:
        snuffel[n] = {"rotation": kf((0, (-sn[0], 0, 0)), (1.2, (-sn[0], 0, 0)))}
    h = zit_hoek(d)
    zak = (d.L / 2 - d.pd) * math.sin(math.radians(h))
    zit = {"lijf": {"rotation": kf((0, (ZIT[0] * h, 0, 0))), "position": kf((0, (0, -zak, 0)))},
           "kop": {"rotation": kf((0, (-ZIT[0] * h, 0, 0)))},
           "poot_lv": {"rotation": kf((0, (-ZIT[0] * h, 0, 0)))}, "poot_rv": {"rotation": kf((0, (-ZIT[0] * h, 0, 0)))},
           "poot_la": {"rotation": kf((0, (ZIT[1] - ZIT[0] * h, 0, 0)))}, "poot_ra": {"rotation": kf((0, (ZIT[1] - ZIT[0] * h, 0, 0)))},
           "staart": {"rotation": kf((0, (0, 10, 0)), (0.5, (0, -10, 0)), (1, (0, 10, 0)))}}
    kwispel = {"staart": {"rotation": kf((0, (0, 28, 0)), (0.12, (0, -28, 0)), (0.24, (0, 28, 0)), (0.36, (0, -28, 0)), (0.48, (0, 28, 0)))},
               "lijf": {"rotation": kf((0, (0, 0, 2)), (0.12, (0, 0, -2)), (0.24, (0, 0, 2)), (0.36, (0, 0, -2)), (0.48, (0, 0, 2)))},
               "kop": {"rotation": kf((0, (-6, 0, -3)), (0.24, (-6, 0, 3)), (0.48, (-6, 0, -3)))}}
    return {"format_version": "1.8.0", "animations": {"idle": anim(3.0, idle), "walk": anim(0.6, walk), "snuffel": anim(1.2, snuffel),
                                                      "zit": anim(1.0, zit), "kwispel": anim(0.48, kwispel)}}


def pose_van(anims, naam):
    """The first keyframe of an animation as (rotations per bone, the body's drop in model px)."""
    rot, zak = {}, 0.0
    for b, ch in anims["animations"][naam]["bones"].items():
        if "rotation" in ch:
            rot[b] = ch["rotation"]["0"]
        if "position" in ch and b == "lijf":
            zak = ch["position"]["0"][1]
    return rot, zak


def maatje_anims(model):
    zweef = {"lijf": {"position": kf((0, (0, 0, 0)), (1, (0, 1.6, 0)), (2, (0, 0, 0))), "rotation": kf((0, (0, 0, -3)), (1, (0, 0, 3)), (2, (0, 0, -3)))}}
    blij = {"lijf": {"position": kf((0, (0, 0, 0)), (0.15, (0, 4, 0)), (0.3, (0, 0, 0)), (0.45, (0, 3, 0)), (0.6, (0, 0, 0))),
                     "rotation": kf((0, (0, 0, 0)), (0.3, (0, 180, 0)), (0.6, (0, 360, 0)))}}
    stout = {"lijf": {"rotation": kf((0, (0, 0, 0)), (0.2, (0, 0, 12)), (0.4, (0, 0, -12)), (0.6, (0, 0, 12)), (0.8, (0, 0, 0)))}}
    if model == "a":
        zweef["zeil"] = {"rotation": kf((0, (0, 0, 0)), (1, (6, 0, 0)), (2, (0, 0, 0)))}
    elif model == "b":
        stout["dop"] = {"rotation": kf((0, (0, 0, 0)), (0.4, (0, 0, -10)), (0.8, (0, 0, 0)))}
    else:
        zweef["staart"] = {"rotation": kf((0, (0, -10, 0)), (1, (0, 10, 0)), (2, (0, -10, 0)))}
    return {"format_version": "1.8.0", "animations": {"idle": anim(2.0, zweef), "blij": anim(0.6, blij, False), "ondeugend": anim(0.8, stout, False)}}


def boompje_anims():
    groei = {"root": {"scale": kf((0, (0.7, 0.7, 0.7)), (0.3, (1.15, 1.15, 1.15)), (0.5, (0.95, 0.95, 0.95)), (0.7, (1, 1, 1)))}}
    wieg = {"stam": {"rotation": kf((0, (0, 0, -2)), (2, (0, 0, 2)), (4, (0, 0, -2)))}}
    return {"format_version": "1.8.0", "animations": {"idle": anim(4.0, wieg), "groei": anim(0.7, groei, False)}}


# =====================================================================================================================
# everything as files (for the game, once the looks are approved)
# =====================================================================================================================
def maak():
    """-> ({"models/entity/<x>.geo.json" | "animations/entity/<x>.animation.json": dict}, {"<texture name>": RGBA array}).
    Per breed ONE model + animation file (and one for its puppy), per coat a texture on the same atlas layout."""
    json_uit, tex = {}, {}
    for i, ras in enumerate(RASSEN):
        for pup in (False, True):
            if ras == "speurhond" and pup:
                continue
            naam = f"snuffelhond_{ras}" + ("_pup" if pup else "")
            for kleur in VACHTEN[ras]:
                g, t, d = figuur_hond(ras, kleur, 100 + i * 10 + pup, pup=pup, naam=naam)
                tex[f"{naam}_{kleur}"] = t
            json_uit[f"models/entity/{naam}.geo.json"] = g
            json_uit[f"animations/entity/{naam}.animation.json"] = hond_anims(d)
    for b in BEWONERS:
        g, t, d = figuur_bewoner(b)
        json_uit[f"models/entity/snuffel_{b['id']}.geo.json"] = g
        json_uit[f"animations/entity/snuffel_{b['id']}.animation.json"] = hond_anims(d)
        tex[f"snuffel_{b['id']}"] = t
    for k, (_, maker) in MAATJES.items():
        for gezicht in ("blij", "ondeugend"):
            atlas = Atlas(300 + ord(k))
            g = maker(atlas, gezicht)
            tex[f"snuffel_maatje_{k}_{gezicht}"] = atlas.img
            json_uit[f"models/entity/snuffel_maatje_{k}_{gezicht}.geo.json"] = g
            json_uit[f"animations/entity/snuffel_maatje_{k}_{gezicht}.animation.json"] = maatje_anims(k)
    for stap in STAPPEN:
        atlas = Atlas(350 + stap)
        json_uit[f"models/entity/snuffel_boompje_{stap}.geo.json"] = boompje(atlas, stap)
        json_uit[f"animations/entity/snuffel_boompje_{stap}.animation.json"] = boompje_anims()
        tex[f"snuffel_boompje_{stap}"] = atlas.img
    return json_uit, tex


def build(h):
    json_uit, tex = maak()
    for pad, d in json_uit.items():
        h.w(f"{h.A}/geckolib/{pad}", d)
    for naam, img in tex.items():
        h.save(Image.fromarray(img), "entity", f"{naam}.png")


def check(h=None):
    """Every parent exists, every animated bone exists, every dog has the bones the animations and the code will ask for,
    no bone name starts with a prefix that wiki_renders hides."""
    json_uit, _ = maak()
    problems = []
    verboden = ("outfit_", "neck", "teckel", "ender", "koning", "wolk", "zeemeer", "asguh", "pluis", "pinguh", "balto", "mewtwo", "stitch", "samguh", "guhshi")
    for pad, g in json_uit.items():
        if not pad.startswith("models/"):
            continue
        name = os.path.basename(pad).split(".")[0]
        bones = g["minecraft:geometry"][0]["bones"]
        names = {b["name"] for b in bones}
        for b in bones:
            if b.get("parent") and b["parent"] not in names:
                problems.append(f"{name}: bone {b['name']} has a missing parent {b['parent']}")
            if b["name"].startswith(verboden):
                problems.append(f"{name}: bone {b['name']} would be hidden by wiki_renders")
        if "maatje" not in name and "boompje" not in name:
            for nodig in ("root", "lijf", "kop", "snuit", "oor_links", "oor_rechts", "staart", "poot_lv", "poot_rv", "poot_la", "poot_ra"):
                if nodig not in names:
                    problems.append(f"{name}: no bone {nodig}")
        for an, a in json_uit[f"animations/entity/{name}.animation.json"]["animations"].items():
            for bn in a["bones"]:
                if bn not in names:
                    problems.append(f"{name}: animation {an} moves a missing bone {bn}")
    return problems


# =====================================================================================================================
# the approval pictures
# =====================================================================================================================
BG, PANEEL, LIJN, INKT, ZACHT = (246, 243, 237), (252, 250, 246), (228, 221, 210), (66, 52, 58), (130, 114, 118)
RAS_VOLGORDE = ("shiba", "jackrussell", "teckel", "corgi", "golden", "mops")
BESTAND = {"mops": "mopshond"}


def _font(maat, vet=True):
    for naam in (("segoeuib.ttf", "arialbd.ttf") if vet else ("segoeui.ttf", "arial.ttf")):
        try:
            return ImageFont.truetype(naam, maat)
        except OSError:
            pass
    return ImageFont.load_default()


class Plaat:
    def __init__(self, w, h, titel, onder=None):
        self.img = Image.new("RGBA", (w, h), BG + (255,))
        self.d = ImageDraw.Draw(self.img)
        self.d.text((60, 36), titel, font=_font(56), fill=INKT)
        if onder:
            self.d.text((62, 110), onder, font=_font(27, False), fill=ZACHT)

    def paneel(self, x0, y0, x1, y1):
        self.d.rounded_rectangle((x0, y0, x1, y1), 26, fill=PANEEL, outline=LIJN, width=3)

    def zet(self, beeld_, x, grond, zweef=0, schaduw=True):
        """Put a rendered figure with its feet on the line `grond`, centred on x (zweef: pixels above the ground)."""
        if schaduw:
            rx = beeld_.width * (0.36 if not zweef else 0.24)
            laag = Image.new("RGBA", self.img.size, (0, 0, 0, 0))
            ImageDraw.Draw(laag).ellipse((x - rx, grond - rx * 0.2 - 4, x + rx, grond + rx * 0.2 - 4), fill=(60, 40, 40, 46))
            self.img.alpha_composite(laag.filter(ImageFilter.GaussianBlur(7)))
        self.img.alpha_composite(beeld_, (int(x - beeld_.width / 2), int(grond - beeld_.height - zweef)))
        self.d = ImageDraw.Draw(self.img)

    def tekst(self, x, y, t, maat=28, vet=True, kleur=INKT, anker="ma", breed=None):
        f = _font(maat, vet)
        if breed:
            regels, regel = [], ""
            for woord in t.split():
                if self.d.textlength((regel + " " + woord).strip(), font=f) > breed and regel:
                    regels.append(regel)
                    regel = woord
                else:
                    regel = (regel + " " + woord).strip()
            t = "\n".join(regels + [regel])
        self.d.multiline_text((x, y), t, font=f, fill=kleur, anchor=anker, align="center" if anker[0] == "m" else "left", spacing=6)

    def bewaar(self, pad):
        self.img.convert("RGB").save(pad)
        print("plaat", os.path.basename(pad))


_FIG = {}


def _hond(ras, kleur, **kw):
    sleutel = (ras, kleur if isinstance(kleur, str) else id(kleur), tuple(sorted(kw.items())))
    if sleutel not in _FIG:
        g, t, d = figuur_hond(ras, kleur, 100 + list(RASSEN).index(ras) * 10, **kw)
        _FIG[sleutel] = (g, t, d, hond_anims(d))
    return _FIG[sleutel]


def _bewoner(b, **over):
    sleutel = (b["id"], tuple(sorted(over.items())))
    if sleutel not in _FIG:
        g, t, d = figuur_bewoner(b, **over)
        _FIG[sleutel] = (g, t, d, hond_anims(d))
    return _FIG[sleutel]


def beeld(fig, yaw=25, pitch=-14, schaal=14, pose=None):
    g, t, d, an = fig
    rot = pose_van(an, pose)[0] if pose else None
    return teken(quads(g, t, rot), yaw, pitch, schaal)[0]


def _los(maker, seed, *args):
    atlas = Atlas(seed)
    return maker(atlas, *args), atlas.img


def beeld_los(gt, yaw=25, pitch=-14, schaal=14):
    return teken(quads(gt[0], gt[1]), yaw, pitch, schaal)[0]


def beeld_guh(yaw, pitch, schaal):
    wr = _wr()
    pad = os.path.join(wr.ASSETS, "geckolib", "models", "entity", "guh.geo.json")
    verberg = ("saddle",) + tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    return teken(wr.geo_quads(pad, "guhs:entity/guh", hide=verberg), yaw, pitch, schaal)[0]


def beeld_speler(yaw, pitch, schaal):
    """A player-sized figure (the vanilla player model and skin), for scale only."""
    wr = _wr()
    tex, qs = "minecraft:entity/player/wide/steve", []
    for origin, size, uv, pivot in (((-4, -8, -4), (8, 8, 8), (0, 0), (0, 0, 0)), ((-4, 0, -2), (8, 12, 4), (16, 16), (0, 0, 0)),
                                    ((-3, -2, -2), (4, 12, 4), (40, 16), (-5, 2, 0)), ((-1, -2, -2), (4, 12, 4), (32, 48), (5, 2, 0)),
                                    ((-2, 0, -2), (4, 12, 4), (0, 16), (-1.9, 12, 0)), ((-2, 0, -2), (4, 12, 4), (16, 48), (1.9, 12, 0))):
        qs += wr.java_box_quads(tex, (64, 64), origin, size, uv, pivot)
    return teken(qs, yaw, pitch, schaal)[0]


def plaat_01(out):
    p = Plaat(2700, 1830, "Het Snuffeleiland  ·  de zes rassen", "Eén hondenbasis, zes rassen. Elk ras in drie kleuren (zie het blad per ras). Dit zijn de honden waaruit de speler kiest.")
    for i, ras in enumerate(RAS_VOLGORDE):
        x, y = 80 + (i % 3) * 860, 180 + (i // 3) * 810
        p.paneel(x, y, x + 820, y + 770)
        kleuren = list(VACHTEN[ras])
        p.zet(beeld(_hond(ras, kleuren[0]), 32, -14, 19), x + 410, y + 590)
        p.tekst(x + 410, y + 630, RASSEN[ras]["naam"], 46)
        p.tekst(x + 410, y + 696, "kleuren: " + ", ".join(VACHTEN[ras][k]["naam"] for k in kleuren), 26, False, ZACHT)
    p.bewaar(os.path.join(out, "01_rassen_overzicht.png"))


def plaat_02(out, ras):
    kleuren = list(VACHTEN[ras])
    p = Plaat(2800, 1900, f"{RASSEN[ras]['naam']}", "Boven: de eerste kleur van alle kanten. Onder: de drie kleuren, en twee houdingen uit de animaties (zit, snuffelt).")
    p.paneel(60, 170, 2740, 960)
    fig = _hond(ras, kleuren[0])
    for i, (naam, yaw, pitch) in enumerate((("van voren", 0, -5), ("van opzij", 270, -6), ("driekwart", 25, -14), ("van achteren", 205, -16))):
        x = 400 + i * 670
        p.zet(beeld(fig, yaw, pitch, 19), x, 850)
        p.tekst(x, 880, naam, 30, False, ZACHT)
    p.paneel(60, 1000, 1700, 1840)
    for i, k in enumerate(kleuren):
        x = 340 + i * 540
        p.zet(beeld(_hond(ras, k), 328 if i == 1 else 32, -14, 17), x, 1690)
        p.tekst(x, 1722, VACHTEN[ras][k]["naam"], 34)
    p.paneel(1740, 1000, 2740, 1840)
    p.zet(beeld(fig, 32, -14, 15, "zit"), 1990, 1690)
    p.tekst(1990, 1722, "zit", 30, False, ZACHT)
    p.zet(beeld(fig, 270, -6, 13, "snuffel"), 2470, 1690)
    p.tekst(2470, 1722, "snuffelt", 30, False, ZACHT)
    p.bewaar(os.path.join(out, f"02_{BESTAND.get(ras, ras)}.png"))


def plaat_03(out):
    p = Plaat(2700, 1800, "De pup  ·  het zieke broertje of zusje", "Dezelfde basis met een klein kort lijfje, korte pootjes en een bijna even groot hoofd. Hier voor twee rassen groot, onderaan alle zes.")
    for i, (ras, kleur) in enumerate((("shiba", "rood"), ("golden", "goud"))):
        x = 60 + i * 1320
        p.paneel(x, 170, x + 1260, 1080)
        p.zet(beeld(_hond(ras, kleur), 25, -14, 16), x + 300, 960)
        p.tekst(x + 300, 990, f"{RASSEN[ras]['naam']} (volwassen)", 28, False, ZACHT)
        pup = _hond(ras, kleur, pup=True)
        p.zet(beeld(pup, 25, -14, 16), x + 720, 960)
        p.tekst(x + 720, 990, "pup", 34)
        p.zet(beeld(pup, 270, -6, 12), x + 1060, 620)
        p.zet(beeld(pup, 205, -16, 12), x + 1060, 960)
        p.tekst(x + 1060, 990, "opzij en achter", 26, False, ZACHT)
    p.paneel(60, 1120, 2640, 1740)
    for i, ras in enumerate(RAS_VOLGORDE):
        x = 290 + i * 425
        k = list(VACHTEN[ras])[1 if ras in ("shiba", "golden") else 0]
        p.zet(beeld(_hond(ras, k, pup=True), 25, -14, 13), x, 1620)
        p.tekst(x, 1650, f"{RASSEN[ras]['naam'].lower()} ({VACHTEN[ras][k]['naam']})", 26, False, ZACHT)
    p.bewaar(os.path.join(out, "03_pups.png"))


def plaat_04(out):
    p = Plaat(3200, 2560, "De bewoners van Snuffeldorp", "Elke bewoner is ras + kleur + losse accessoires op dezelfde basis: één regel in de lijst, geen nieuw model.")
    for i, b in enumerate(BEWONERS):
        x, y = 60 + (i % 4) * 775, 170 + (i // 4) * 790
        p.paneel(x, y, x + 745, y + 760)
        p.zet(beeld(_bewoner(b), 25 if i % 2 == 0 else 335, -14, 14), x + 372, y + 500)
        p.tekst(x + 372, y + 530, b["naam"], 38)
        p.tekst(x + 372, y + 584, b["rol"], 25, False, INKT, breed=680)
        p.tekst(x + 372, y + 660, b["uiterlijk"], 21, False, ZACHT, breed=680)
    x, y = 60 + 3 * 775, 170 + 2 * 790
    p.paneel(x, y, x + 745, y + 760)
    p.tekst(x + 372, y + 90, "Zo werkt het", 38)
    p.tekst(x + 372, y + 170, "ras (7 mogelijk) × kleur (3 per ras) × accessoires (18 stuks: bril, voorhoofdspiegel, jas, cape, sjaal, bandana, medaille, tas, "
            "rugzak, schort, kapiteinspet, platte pet, strohoed, zuidwester, bakkersmuts, strik, bloem, botje) × blik (open, wijs) × grijze snuit × pup. "
            "Elk accessoire past zichzelf aan op de maten van het ras.", 25, False, ZACHT, breed=640)
    p.bewaar(os.path.join(out, "04_bewoners.png"))


def plaat_04b(out):
    p = Plaat(3000, 2060, "Dokter, trainer, kapitein en vader van dichtbij", "De trainer is een zevende ras (speurhond) dat alleen bewoners hebben. De vader krijgt het ras en de kleur die de speler kiest.")
    wie = {b["id"]: b for b in BEWONERS}
    for i, id_ in enumerate(("dokter", "trainer", "kapitein", "vader")):
        b = wie[id_]
        x = 60 + i * 730
        p.paneel(x, 170, x + 700, 1560)
        fig = _bewoner(b)
        p.zet(beeld(fig, 25, -14, 17), x + 350, 760)
        p.zet(beeld(fig, 270, -6, 10), x + 350, 1080)
        p.zet(beeld(fig, 200, -18, 8.5), x + 190, 1420)
        p.zet(beeld(fig, 0, -5, 8.5), x + 520, 1420)
        p.tekst(x + 350, 1450, b["naam"], 38)
        p.tekst(x + 350, 1502, b["rol"], 24, False, ZACHT, breed=650)
    p.paneel(60, 1600, 2940, 2010)
    p.tekst(110, 1630, "Papa Zwerfpoot in een ander ras (hij volgt de keuze van de speler):", 30, True, INKT, "la")
    vader = wie["vader"]
    for i, (ras, kleur) in enumerate((("golden", "goud"), ("teckel", "zwart"), ("corgi", "rood"), ("mops", "beige"), ("jackrussell", "bruin"))):
        p.zet(beeld(_bewoner(vader, ras=ras, kleur=kleur), 25, -14, 9), 420 + i * 530, 1960)
    p.bewaar(os.path.join(out, "04b_dokter_trainer_kapitein_vader.png"))


MAATJE_TEKST = {
    "a": ("A  ·  Zweefzaadje", "Een bolletje zaadpluis met een zaadje eronder en een blad als zeil.",
          "Zweeft mee op de wind, stuurt met het blad; licht en luchtig, valt goed op tegen gras."),
    "b": ("B  ·  Mos-eikeltje", "Een eikeltje met de dop diep over de ogen, een pluk mos en twee takarmpjes.",
          "Kan wijzen en gebaren met de takjes; de dop wipt mee als het iets uithaalt."),
    "c": ("C  ·  Zonnepluisje", "Een gloeiend paardenbloemkopje met kroonblaadjes rondom en een krulstaartje.",
          "Geeft licht (handig in het donker), laat pluisvonkjes achter als spoor."),
}


def plaat_05(out):
    p = Plaat(3000, 2640, "Het maatje  ·  drie ontwerpen om uit te kiezen", "Een ondeugend bosgeestje dat alleen jij kunt zien. Elk ontwerp met een blij en een ondeugend gezicht; rechts naast een hond, op ware grootte.")
    hond_fig = _hond("shiba", "rood")
    for i, k in enumerate(MAATJES):
        y = 170 + i * 820
        p.paneel(60, y, 2940, y + 790)
        blij, stout = _los(MAATJES[k][1], 300 + i, "blij"), _los(MAATJES[k][1], 300 + i, "ondeugend")
        titel, r1, r2 = MAATJE_TEKST[k]
        p.tekst(110, y + 30, titel, 44, True, INKT, "la")
        p.tekst(110, y + 92, r1, 26, False, ZACHT, "la")
        p.tekst(110, y + 130, r2, 26, False, ZACHT, "la")
        for j, (gt, naam, yaw) in enumerate(((blij, "blij", 20), (stout, "ondeugend", 340), (blij, "opzij", 285), (blij, "achter", 200))):
            groot = j < 2
            x = (400, 1000, 1500, 1880)[j]
            p.zet(beeld_los(gt, yaw, -12, 16 if groot else 10), x, y + 690, zweef=20)
            p.tekst(x, y + 720, naam, 30 if groot else 26, groot, INKT if groot else ZACHT)
        p.zet(beeld(hond_fig, 62, -10, 10), 2640, y + 690)
        p.zet(beeld_los(blij, 300, -10, 10 * MAATJE_SCHAAL / 0.8), 2330, y + 690, zweef=150)
        p.tekst(2500, y + 720, "naast een hond", 26, False, ZACHT)
    p.bewaar(os.path.join(out, "05_maatje_varianten.png"))


def plaat_06(out):
    p = Plaat(2700, 1040, "Het boompje van het maatje  ·  vier stappen", "Het groeit een stap bij goede daden. De stenenkrans blijft, zodat je de plek herkent. Rechts een pup voor de maat.")
    p.paneel(60, 170, 2640, 980)
    for i, stap in enumerate(STAPPEN):
        x = 330 + i * 520
        p.zet(beeld_los(_los(boompje, 350 + stap, stap), 25, -16, 19), x, 850)
        p.tekst(x, 880, f"{stap}  ·  {STAPPEN[stap]}", 34)
    p.zet(beeld(_hond("corgi", "rood", pup=True), 335, -14, 19 * 0.8), 2420, 850)
    p.bewaar(os.path.join(out, "06_boompje.png"))


def plaat_07(out):
    s = 15
    p = Plaat(3000, 1060, "Naast een guh en een speler", "Alles op dezelfde schaal zoals in het spel (honden op 0,8; het maatje op 0,5). Stijlvergelijking: zelfde ogen, blosjes en vacht, maar duidelijk een hond.")
    p.paneel(60, 170, 2940, 1000)
    grond = 880
    for x, b, naam, zweef in ((330, beeld_speler(25, -10, s), "speler", 0),
                              (840, beeld_guh(25, -14, s), "een gewone guh", 0),
                              (1380, beeld(_hond("golden", "goud"), 25, -14, s * 0.8), "golden retriever", 0),
                              (1800, beeld(_hond("shiba", "rood"), 25, -14, s * 0.8), "shiba", 0),
                              (2160, beeld(_hond("shiba", "rood", pup=True), 25, -14, s * 0.8), "pup", 0),
                              (2450, beeld(_hond("teckel", "zwart"), 335, -14, s * 0.8), "teckel", 0),
                              (2790, beeld_los(_los(maatje_b, 301, "blij"), 25, -12, s * MAATJE_SCHAAL), "maatje", 150)):
        p.zet(b, x, grond, zweef=zweef)
        p.tekst(x, grond + 30, naam, 30, False, ZACHT)
    p.bewaar(os.path.join(out, "07_naast_een_guh.png"))


def lees_mij(out):
    r = ["# Het Snuffeleiland: de modellen (ter goedkeuring)", "",
         "Alles hier is alleen een uiterlijk-ronde: er is nog niets van het verhaal gebouwd en niets zit in het spel.",
         "Bron: `tools/features/snuffel_modellen.py` (branch `bbq2-snuffel-modellen`). Opnieuw maken: `python tools/features/snuffel_modellen.py <map>`.", "",
         "## De plaatjes", "",
         "- `01_rassen_overzicht.png`: de zes rassen naast elkaar (driekwart).",
         "- `02_<ras>.png` (shiba, jackrussell, teckel, corgi, golden, mopshond): voor, opzij, driekwart, achter; de drie kleuren; zit en snuffelt.",
         "- `03_pups.png`: de pup (het zieke broertje of zusje) groot voor shiba en golden, klein voor alle zes.",
         "- `04_bewoners.png`: de elf bewoners met naam en rol. `04b_dokter_trainer_kapitein_vader.png`: die vier van dichtbij, en de vader in andere rassen.",
         "- `05_maatje_varianten.png`: drie ontwerpen voor het maatje (A, B, C), elk blij en ondeugend, met een hond ernaast.",
         "- `06_boompje.png`: het boompje in vier stappen (kiem, scheutje, struikje, jong boompje).",
         "- `07_naast_een_guh.png`: honden, pup en maatje naast een gewone guh en een speler, op spelschaal.",
         "- Er is geen `08_in_het_spel.png`: de plaatjes komen uit de eigen model-renderer van de mod (zelfde modellen en texturen als in het spel).", "",
         "## De rassen en hun kleuren", ""]
    for ras in RAS_VOLGORDE:
        r.append(f"- **{RASSEN[ras]['naam']}**: " + ", ".join(v["naam"] for v in VACHTEN[ras].values()))
    r += ["", "Geen ras gewisseld: alle zes zijn aan hun silhouet te herkennen (oren, staart, pootlengte, snuit).",
          "Extra: de speurhond (lange hangoren, hanglippen, zadelrug), alleen voor de trainer.", "",
          "## De bewoners", "", "| naam | ras | rol | uiterlijk |", "|---|---|---|---|"]
    for b in BEWONERS:
        ras = RASSEN[b["ras"]]["naam"].lower() + (" (pup)" if b.get("pup") else "")
        r.append(f"| {b['naam']} | {ras}, {VACHTEN[b['ras']][b['kleur']]['naam']} | {b['rol']} | {b['uiterlijk']} |")
    r += ["", "## Het maatje: drie ontwerpen", ""]
    for k in MAATJES:
        titel, r1, r2 = MAATJE_TEKST[k]
        r += [f"**{titel.replace('  ·  ', ': ')}**  ", f"{r1}  ", f"{r2}", ""]
    r += ["## Wat ik zou kiezen", "",
          "- **Maatje B, het Mos-eikeltje.** Het heeft het sterkste silhouet (de dop), is in één oogopslag geen guh en geen hond, en de takarmpjes kunnen",
          "  wijzen naar een geur. De dop over de ogen maakt het ondeugende gezicht het grappigst. A is het liefst, maar wit pluis valt weg tegen",
          "  zand en wolken; C is het opvallendst, maar leest meer als bloem dan als geestje.",
          "- **Trainer als speurhond** en **dokter als oude teckel**: allebei op afstand te herkennen aan vorm, niet alleen aan kleding.",
          "- **Vader in het ras van de speler**: kost niets extra en maakt hem meteen familie.", ""]
    open(os.path.join(out, "LEES_MIJ.md"), "w", encoding="utf-8").write("\n".join(r))


def platen(out, welke=None):
    os.makedirs(out, exist_ok=True)
    werk = {"01": [lambda: plaat_01(out)], "02": [(lambda ras=ras: plaat_02(out, ras)) for ras in RAS_VOLGORDE], "03": [lambda: plaat_03(out)],
            "04": [lambda: plaat_04(out), lambda: plaat_04b(out)], "05": [lambda: plaat_05(out)], "06": [lambda: plaat_06(out)],
            "07": [lambda: plaat_07(out)], "lees": [lambda: lees_mij(out)]}
    for naam, taken in werk.items():
        if not welke or naam in welke:
            for taak in taken:
                taak()


if __name__ == "__main__":
    problemen = check()
    if problemen:
        raise SystemExit("\n".join(problemen))
    platen(sys.argv[1] if len(sys.argv) > 1 else ".", sys.argv[2:])
