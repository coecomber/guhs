"""Renderkit voor Guhs 1.10.0 "Het Vadsrijk"-concepten (Knuffelkamp, Portaaltempel).

Bovenop erfkit (kopie in deze map; assets = guhs-assets-1.8.1.zip uit renders-erfenis/_bron/jars).
Extra's:
  - Rot          : lokaal assenstelsel met een 'facing' (gebouwen draaien, trappen draaien mee)
  - pix(...)     : pixel-art op een verticaal vlak (guhgezichten, oortjes, vlaggen)
  - oortjes(...) : twee guh-oortjes op een dak/ballon
  - uitsnede(...): deel van een Scene als nieuwe Scene (close-ups)
  - render_lagen : render met verre laag in nevel + zon/manen/wolken (vergezicht)
Coördinaten: x oost, y omhoog, z zuid. Renders komen in renders-vadsrijk/<naam>.png.
Let op: multipart-blokken zonder variant (vine, glow_lichen, pink_petals, mushroom blocks) renderen als
roze terugvalkubus; die vermijden we.
"""
import os, sys, math, time, random
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import erfkit as K  # noqa: E402

V, R, E = K.V, K.R, K.E
R.GRASS = (132, 200, 96)
R.FOLIAGE = (96, 176, 72)
R.WATER = (96, 178, 236)
OUT = K.OUT
Scene, stairs, slab, has_block = K.Scene, K.stairs, K.slab, K.has_block
V.EMISSIVE |= {"minecraft:campfire", "minecraft:pearlescent_froglight", "minecraft:sea_lantern",
               "guhs:guh_lantaarnpaal", "guhs:lampion_roze"}

DIRS = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}
NAMES = {v: k for k, v in DIRS.items()}

ZONSOPGANG = ((146, 160, 222), (255, 206, 184))  # lucht boven -> onder


class Rot:
    """Lokaal stelsel: lx = naar rechts, lz = naar achteren, voorkant (lz=0) kijkt naar 'facing'."""

    def __init__(self, S, ox, oy, oz, facing="south"):
        self.S, self.o = S, (ox, oy, oz)
        fx, fz = DIRS[facing]
        self.f = (fx, fz)
        self.r = (-fz, fx)
        self.b = (-fx, -fz)

    def w(self, lx, ly, lz):
        ox, oy, oz = self.o
        return (ox + lx * self.r[0] + lz * self.b[0], oy + ly, oz + lx * self.r[1] + lz * self.b[1])

    def dir(self, d):
        """d in front/back/left/right -> wereldrichting."""
        v = {"front": self.f, "back": self.b, "right": self.r, "left": (-self.r[0], -self.r[1])}[d]
        return NAMES[v]

    def _st(self, s):
        # trappen: facing=front/back/left/right in lokale termen
        if "facing=" in s:
            for d in ("front", "back", "left", "right"):
                s = s.replace(f"facing={d}", f"facing={self.dir(d)}")
        return s

    def set(self, lx, ly, lz, s, **kw):
        self.S.set(*self.w(lx, ly, lz), self._st(s), **kw)

    def fill(self, x1, y1, z1, x2, y2, z2, s, **kw):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, s, **kw)

    def guh(self, lx, ly, lz, yaw_rel=0, **kw):
        x, y, z = self.w(lx, ly, lz)
        base = {"south": 180, "north": 0, "east": 90, "west": 270}[NAMES[self.f]]
        self.S.guh(x + 0.5, y, z + 0.5, (base + yaw_rel) % 360, **kw)


def pix(S, x0, y0, z0, u, rows, key, keep=False, roof=False):
    """Pixel-art op een verticaal vlak. (x0,y0,z0) = linksboven, u = (dx,dz) per kolom naar rechts.
    rows = lijst strings van boven naar beneden; key = {teken: blok} (spatie/'.' = niets)."""
    for i, row in enumerate(rows):
        for j, ch in enumerate(row):
            b = key.get(ch)
            if b:
                S.set(x0 + j * u[0], y0 - i, z0 + j * u[1], b, keep=keep, roof=roof)


GEZICHT_KEY = {"#": "minecraft:pink_wool", "o": "minecraft:black_concrete", "w": "minecraft:white_concrete",
               "b": "minecraft:pink_concrete", "m": "minecraft:black_wool", "l": "minecraft:light_blue_concrete",
               "r": "guhs:knuffelblok_roze"}

# guhgezicht 9 breed x 7 hoog: grote ogen met glimmer, blosjes, mondje
GEZICHT_9 = ["#########",
             "#ow###wo#",
             "#ol###lo#",
             "#oo###oo#",
             "#b##m##b#",
             "##mm#mm##",
             "#########"]
GEZICHT_5 = ["#####",
             "ow#wo",
             "oo#oo",
             "b#m#b",
             "#####"]


def oor(S, x, y, z, u, h=6, w=4, buiten="minecraft:pink_wool", binnen="minecraft:pink_concrete", dik=(0, 0)):
    """Eén afgerond guh-oortje op vlak u (dx,dz), voet links-onder op (x,y,z). dik=(dx,dz) extra laag erachter."""
    for i in range(h):
        t = i / max(1, h - 1)
        half = w / 2 * (1.0 - 0.55 * t * t)
        for j in range(w):
            c = j - (w - 1) / 2
            if abs(c) > half:
                continue
            b = binnen if (abs(c) < half - 0.9 and 0 < i < h - 1) else buiten
            S.set(x + j * u[0], y + i, z + j * u[1], b)
            if dik != (0, 0):
                S.set(x + j * u[0] + dik[0], y + i, z + j * u[1] + dik[1], buiten)


def oortjes(S, cx, y, cz, u, gap=4, h=6, w=4, **kw):
    """Twee oortjes symmetrisch rond (cx,cz) op vlak u. gap = afstand tussen de oortjes."""
    off = gap // 2 + w
    oor(S, cx - off * u[0], y, cz - off * u[1], u, h, w, **kw)
    oor(S, cx + (gap // 2 + (gap % 2)) * u[0], y, cz + (gap // 2 + (gap % 2)) * u[1], u, h, w, **kw)


def vlag(S, x, y, z, u, kleur="minecraft:pink_wool", rand="minecraft:white_wool", h=4, w=3, paal_h=7,
         paal="minecraft:cherry_fence", embleem=None):
    """Vlaggenmast met een wollen vaandel (banieren renderen niet)."""
    for i in range(paal_h):
        S.set(x, y + i, z, paal)
    top = y + paal_h - 1
    for i in range(h):
        for j in range(1, w + 1):
            last = i == h - 1
            if last and j == w:  # zwaluwstaartje
                continue
            b = rand if (j == w or i == 0) else kleur
            S.set(x + j * u[0], top - i, z + j * u[1], b)
    if embleem:
        S.set(x + 2 * u[0] if w >= 3 else x + u[0], top - 1 - (h > 3), z + 2 * u[1] if w >= 3 else z + u[1], embleem)


def uitsnede(S, x1, z1, x2, z2, y1=0, y2=None):
    y2 = S.v.shape[1] if y2 is None else y2
    N = Scene(x2 - x1, y2 - y1, z2 - z1)
    N.pal, N.idx = list(S.pal), dict(S.idx)
    N.v = S.v[x1:x2, y1:y2, z1:z2].copy()
    N.guhs = [(gx - x1, gy - y1, gz - z1, a, b, c) for (gx, gy, gz, a, b, c) in S.guhs
              if x1 <= gx < x2 and z1 <= gz < z2]
    N.roof = {(x - x1, y - y1, z - z1) for (x, y, z) in S.roof if x1 <= x < x2 and z1 <= z < z2}
    return N


def _laag(S, vol, cam, bounds, extra=(), ss=2, emissive=()):
    r, bk = V.res()
    extras, hide = E.special_extras(vol, S.pal, bk)
    for (x, y, z, yaw_g, skin, sitting) in S.guhs:
        extras += E.guh(r, x, y, z, yaw_g, skin=skin, scale=0.8, sitting=sitting)
    extras = list(extras) + list(extra) + [K.punt(*p) for p in bounds]
    return R.render(vol, S.pal, cam, r, bk, hide=lambda s: s in hide, extras=extras, margin=30 * ss,
                    emissive=V.EMISSIVE | set(emissive))


def _wolken(bg, rng, n, ybereik, kleur=(255, 222, 232), a=150):
    W, H = bg.size
    lay = Image.new("RGBA", bg.size, kleur + (0,))
    d = ImageDraw.Draw(lay)
    for _ in range(n):
        cx, cy = rng.uniform(0, W), rng.uniform(*ybereik) * H
        for _ in range(6):
            rx, ry = rng.uniform(0.03, 0.09) * W, rng.uniform(0.012, 0.03) * H
            ox, oy = rng.uniform(-0.06, 0.06) * W, rng.uniform(-0.01, 0.01) * H
            d.ellipse((cx + ox - rx, cy + oy - ry, cx + ox + rx, cy + oy + ry), fill=kleur + (a,))
    lay = lay.filter(ImageFilter.GaussianBlur(W / 180))
    bg.paste(lay, (0, 0), lay)


def lucht(W, H, sky=ZONSOPGANG, zon=None, manen=(), wolken=0, seed=3, horizon=0.7):
    """Zonsopgang-lucht: verloop, optioneel zon (x,y,r in fracties), manen [(x,y,r)], roze wolken."""
    bg = V.gradient(W, H, *sky).convert("RGBA")
    rng = random.Random(seed)
    if zon:
        zx, zy, zr = zon[0] * W, zon[1] * H, zon[2] * W
        glow = Image.new("RGBA", bg.size, (255, 214, 160, 0))
        d = ImageDraw.Draw(glow)
        for k, (rr, al) in enumerate(((zr * 7, 60), (zr * 4, 80), (zr * 2.2, 110))):
            d.ellipse((zx - rr, zy - rr * 0.8, zx + rr, zy + rr * 0.8), fill=(255, 214, 160, al))
        glow = glow.filter(ImageFilter.GaussianBlur(zr * 1.5))
        bg.paste(glow, (0, 0), glow)
        d = ImageDraw.Draw(bg)
        d.ellipse((zx - zr, zy - zr, zx + zr, zy + zr), fill=(255, 244, 214))
    for (mx, my, mr) in manen:
        d = ImageDraw.Draw(bg)
        x, y, r_ = mx * W, my * H, mr * W
        d.ellipse((x - r_, y - r_, x + r_, y + r_), fill=(246, 232, 246, 255))
        d.ellipse((x - r_ * 0.35, y - r_ * 0.5, x - r_ * 0.05, y - r_ * 0.2), fill=(232, 214, 236, 255))
        d.ellipse((x + r_ * 0.15, y + r_ * 0.1, x + r_ * 0.45, y + r_ * 0.4), fill=(232, 214, 236, 255))
    if wolken:
        _wolken(bg, rng, wolken, (0.08, horizon - 0.05))
    return bg


def render(S, name, yaw=35, pitch=30, scale=8, ss=2, extra=(), bounds=(), labels=(), titel=None, sky=ZONSOPGANG,
           noroof=False, emissive=(), schaduw=True, wolken=0):
    """Zoals erfkit.render, maar met zonsopgang-lucht en optioneel wolken achter de scène."""
    S.fix_connections()
    vol = S.v.copy()
    if noroof:
        for (x, y, z) in S.roof:
            vol[x, y, z] = 0
    cam = R.Camera(yaw, pitch, scale * ss)
    t = time.time()
    im, (ox, oy) = _laag(S, vol, cam, bounds, extra, ss, emissive)
    im = im.resize((im.width // ss, im.height // ss), Image.LANCZOS)
    bg = lucht(im.width, im.height, sky, wolken=wolken)
    if schaduw:
        sh = Image.new("RGBA", im.size, (70, 50, 90, 0))
        a = im.split()[3].filter(ImageFilter.GaussianBlur(14))
        sh.putalpha(a.point(lambda v: int(v * 0.3)))
        bg.paste(sh, (10, 16), sh)
    bg.paste(im, (0, 0), im)
    d = ImageDraw.Draw(bg)
    for (x, y, z, tekst) in labels:
        sx, sy, _ = cam.project(np.array([x, y, z], float))
        K.ballon(d, (sx + ox) / ss, (sy + oy) / ss, tekst)
    if titel:
        d.text((14, 10), titel, font=K._font(20, True), fill=(120, 40, 90))
    path = os.path.join(OUT, name + ".png")
    bg.convert("RGB").save(path)
    print(name, "%.1fs" % (time.time() - t), bg.size, flush=True)
    return path


def render_lagen(S, name, ver_masker, yaw=0, pitch=5, scale=8, ss=2, nevel=0.55, sky=ZONSOPGANG, zon=None,
                 manen=(), wolken=8, extra=(), nevelkleur=(250, 210, 200), titel=None, crop=None):
    """Vergezicht: blokken waar ver_masker[x,y,z] True is worden apart gerenderd en in nevel gezet.
    Beide lagen krijgen dezelfde beeldgrenzen (hoeken van het volume), dus ze vallen precies over elkaar.
    crop = (l, t, r, b) in fracties om het eindbeeld bij te snijden."""
    S.fix_connections()
    X, Y, Z = S.v.shape
    cam = R.Camera(yaw, pitch, scale * ss)
    bounds = [(x, y, z) for x in (0, X) for y in (0, Y) for z in (0, Z)]
    t = time.time()
    # ver_masker mag ook een lijst [(masker, nevel), ...] zijn (van ver naar dichtbij), voor een zachte overgang
    banden = ver_masker if isinstance(ver_masker, list) else [(ver_masker, nevel)]
    rest = np.ones(S.v.shape, bool)
    lagen = []
    gu = S.guhs
    for (m, nv) in banden:
        m = m & rest
        rest &= ~m
        S.guhs = []
        im_v, _ = _laag(S, np.where(m, S.v, 0).astype(S.v.dtype), cam, bounds, (), ss)
        im_v = im_v.resize((im_v.width // ss, im_v.height // ss), Image.LANCZOS)
        a = np.asarray(im_v).astype(np.float32)
        a[..., :3] = a[..., :3] * (1 - nv) + np.array(nevelkleur, np.float32) * nv
        lagen.append(Image.fromarray(a.astype(np.uint8), "RGBA"))
    S.guhs = gu
    im_d, (ox, oy) = _laag(S, np.where(rest, S.v, 0).astype(S.v.dtype), cam, bounds, extra, ss)
    im_d = im_d.resize((im_d.width // ss, im_d.height // ss), Image.LANCZOS)
    bg = lucht(im_d.width, im_d.height, sky, zon=zon, manen=manen, wolken=wolken)
    for im_v in lagen:
        bg.paste(im_v, (0, 0), im_v)
    bg.paste(im_d, (0, 0), im_d)
    if titel:
        ImageDraw.Draw(bg).text((14, 10), titel, font=K._font(20, True), fill=(120, 40, 90))
    if crop:
        W, H = bg.size
        bg = bg.crop((int(crop[0] * W), int(crop[1] * H), int(crop[2] * W), int(crop[3] * H)))
    path = os.path.join(OUT, name + ".png")
    bg.convert("RGB").save(path)
    print(name, "%.1fs" % (time.time() - t), bg.size, flush=True)
    return path


def ruis(seed=1):
    """Gladde 2D-ruis uit sinussen (genoeg voor kustlijnen en heuvels)."""
    rng = random.Random(seed)
    golven = [(rng.uniform(0.02, 0.09), rng.uniform(0.02, 0.09), rng.uniform(0, 6.28), rng.uniform(0.3, 1.0))
              for _ in range(6)]
    tot = sum(g[3] for g in golven)

    def f(x, z):
        return sum(a * math.sin(x * kx + z * kz + p) for (kx, kz, p, a) in golven) / tot
    return f
