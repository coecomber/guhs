"""Renderkit voor de 1.9 "De Erfenis"-concepten (dorp/boerderij).

Bovenop vibekit + mijnkit (kopieën in deze map): assets uit guhs_erfenis (Guhs 1.8.1, gezipt in jars/),
guh-figuurtjes met alleen de basisbotten, plus:
  - dier(...)        : GeckoLib-boerderijdieren (schaapje, kippetje, koe, eendje, konijntje) en NPC-guhs
  - vlak(...)        : platte gekleurde overlay-quads (perceelrand, rooster)
  - render(...)      : zoals vibekit.render, maar met vaste beeldgrenzen (bounds) en tekstballonnetjes (labels)
Coördinaten: x oost, y omhoog, z zuid. Renders komen in renders-erfenis/<naam>.png.
"""
import os, sys, time, math
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mijnkit as M  # noqa: E402  (zet de jars en de GUH_BASIS-patch)

V = M.V
R, E = V.R, V.E
V.OUT = os.path.abspath(os.path.join(HERE, ".."))
R.GRASS = (178, 232, 122)
R.FOLIAGE = (110, 186, 80)
OUT = V.OUT
Scene, stairs, slab, has_block = V.Scene, V.stairs, V.slab, V.has_block
V.EMISSIVE |= {"guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint", "guhs:guh_kristal_lamp",
               "guhs:sterrenlantaarn", "guhs:weidekristal_roze", "guhs:weidekristal_lila", "guhs:weidekristal_cyaan",
               "guhs:guh_lantaarnpaal", "guhs:vadslamp", "guhs:lampjestegel", "guhs:glimtegel"}

GEO = "assets/guhs/geckolib/models/entity/"


def dier(soort, x, y, z, yaw=0, scale=1.0, tex=None, skip=None):
    """Quads voor een GeckoLib-wezen. soort = geo-naam (guhschaapje, knabbelkippetje, guhkoe, guh_eendje,
    guh_konijntje, guh_npc_boerinneguh ...). yaw 0 = kijkt naar het noorden, 180 = zuid."""
    r = V.res()[0]
    if tex is None:
        tex = soort.replace("guh_npc_", "npc_")
    if skip is None:
        skip = {"guhschaapje": ("kaal",), "wolkenschaapje": ("kaal",)}.get(soort, ())
    Mt = E.affine(scale, yaw, (x, y, z))
    if soort.startswith("guh_npc_"):  # NPC-guhs: basisbotten + hun eigen kledingbotten
        import json
        bones = json.loads(r.read(GEO + soort + ".geo.json"))["minecraft:geometry"][0]["bones"]
        skip = tuple(b["name"] for b in bones if b["name"].startswith(("tail_",)))
    return M._geo_orig(r, GEO + soort + ".geo.json", "guhs:entity/" + tex, Mt, (1, 1, 1), tuple(skip))


def kleur(rgb, a=1.0):
    t = np.ones((2, 2, 4), np.float32)
    t[..., :3] = np.array(rgb, np.float32) / 255.0
    t[..., 3] = a
    return t


def vlak(x1, z1, x2, z2, y, rgb, a=1.0):
    """Plat vlak (bovenkant zichtbaar) van (x1,z1) tot (x2,z2) op hoogte y."""
    P = np.array([(x1, y, z1), (x2, y, z1), (x1, y, z2)], float)
    cu = np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float)
    return [(P, cu, kleur(rgb, a), (1, 1, 1), a < 0.999)]


def rand(x1, z1, x2, z2, y, rgb=(255, 120, 190), a=0.85, w=0.14, streep=0.0):
    """Omtreklijn rond een rechthoek (blokranden x1..x2, z1..z2) net boven de grond. streep>0 = gestippeld."""
    out = []
    y = y + 0.02

    def seg(ax1, az1, ax2, az2):
        out.extend(vlak(ax1, az1, ax2, az2, y, rgb, a))

    if streep:
        L = streep
        x = x1
        while x < x2:
            seg(x, z1 - w / 2, min(x + L, x2), z1 + w / 2)
            seg(x, z2 - w / 2, min(x + L, x2), z2 + w / 2)
            x += 2 * L
        z = z1
        while z < z2:
            seg(x1 - w / 2, z, x1 + w / 2, min(z + L, z2))
            seg(x2 - w / 2, z, x2 + w / 2, min(z + L, z2))
            z += 2 * L
    else:
        seg(x1 - w / 2, z1 - w / 2, x2 + w / 2, z1 + w / 2)
        seg(x1 - w / 2, z2 - w / 2, x2 + w / 2, z2 + w / 2)
        seg(x1 - w / 2, z1 + w / 2, x1 + w / 2, z2 - w / 2)
        seg(x2 - w / 2, z1 + w / 2, x2 + w / 2, z2 - w / 2)
    return out


def rooster(x1, z1, x2, z2, y, stap=1, rgb=(255, 255, 255), a=0.18, w=0.05):
    out = []
    for x in range(x1, x2 + 1, stap):
        out += vlak(x - w / 2, z1, x + w / 2, z2, y + 0.015, rgb, a)
    for z in range(z1, z2 + 1, stap):
        out += vlak(x1, z - w / 2, x2, z + w / 2, y + 0.015, rgb, a)
    return out


def punt(x, y, z):
    """Onzichtbaar punt dat alleen de beeldgrenzen oprekt (zelfde camera = zelfde uitsnede)."""
    P = np.array([(x, y, z)] * 3, float)
    return (P, np.zeros((4, 2)), kleur((0, 0, 0)), (1, 1, 1), False)


def _font(sz, bold=False):
    try:
        return ImageFont.truetype("segoeuib.ttf" if bold else "segoeui.ttf", sz)
    except OSError:
        return ImageFont.load_default()


def ballon(d, x, y, tekst, sz=15):
    """Tekstballonnetje met puntje naar (x, y)."""
    f = _font(sz, True)
    l, t, r_, b = d.textbbox((0, 0), tekst, font=f)
    w, h = r_ - l + 16, b - t + 10
    bx, by = x - w / 2, y - h - 14
    d.rounded_rectangle((bx, by, bx + w, by + h), 8, fill=(255, 255, 255), outline=(200, 90, 150), width=2)
    d.polygon([(x - 5, by + h - 1), (x + 5, by + h - 1), (x, y - 2)], fill=(255, 255, 255))
    d.line([(x - 5, by + h), (x, y - 2), (x + 5, by + h)], fill=(200, 90, 150), width=2)
    d.text((bx + 8 - l, by + 5 - t), tekst, font=f, fill=(120, 40, 90))


def render(S, name, yaw=35, pitch=30, scale=22, ss=2, extra=(), bounds=(), labels=(), titel=None,
           sky=((214, 228, 250), (252, 238, 244)), noroof=False, emissive=()):
    """Rendert naar OUT/<name>.png. bounds = punten (x,y,z) die altijd in beeld moeten (vaste uitsnede).
    labels = [(x, y, z, tekst)] -> tekstballonnetje boven dat punt. titel = tekst linksboven."""
    r, bk = V.res()
    S.fix_connections()
    vol = S.v.copy()
    if noroof:
        for (x, y, z) in S.roof:
            vol[x, y, z] = 0
    extras, hide = E.special_extras(vol, S.pal, bk)
    for (x, y, z, yaw_g, skin, sitting) in S.guhs:
        extras += E.guh(r, x, y, z, yaw_g, skin=skin, scale=0.8, sitting=sitting)
    extras = list(extras) + list(extra) + [punt(*p) for p in bounds]
    cam = R.Camera(yaw, pitch, scale * ss)
    t = time.time()
    im, (ox, oy) = R.render(vol, S.pal, cam, r, bk, hide=lambda s: s in hide, extras=extras, margin=30 * ss,
                            emissive=V.EMISSIVE | set(emissive))
    im = im.resize((im.width // ss, im.height // ss), Image.LANCZOS)
    bg = V.gradient(im.width, im.height, *sky)
    sh = Image.new("RGBA", im.size, (70, 50, 90, 0))
    a = im.split()[3].filter(ImageFilter.GaussianBlur(14))
    sh.putalpha(a.point(lambda v: int(v * 0.3)))
    bg.paste(sh, (10, 16), sh)
    bg.paste(im, (0, 0), im)
    d = ImageDraw.Draw(bg)
    for (x, y, z, tekst) in labels:
        sx, sy, _ = cam.project(np.array([x, y, z], float))
        ballon(d, (sx + ox) / ss, (sy + oy) / ss, tekst)
    if titel:
        f = _font(20, True)
        d.text((14, 10), titel, font=f, fill=(120, 40, 90))
    path = os.path.join(OUT, name + ".png")
    bg.save(path)
    print(name, "%.1fs" % (time.time() - t), bg.size, flush=True)
    return path
