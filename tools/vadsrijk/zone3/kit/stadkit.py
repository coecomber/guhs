"""Renderkit voor de Zwamzoet-renders (Guhs 1.10.0 "Het Vadsrijk").

Kopieën van de 1.9-kit (erfkit/mijnkit/vibekit/mcrender/mcextra) staan in deze map; de assets komen uit
renders-erfenis/_bron/jars/guhs-assets-1.8.1.zip (alleen gelezen). Renders gaan naar renders-vadsrijk/.
Coördinaten: x oost, y omhoog, z zuid (zoals in het spel).
"""
import os, sys, time, math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import erfkit as K  # noqa: E402

V, R, E = K.V, K.R, K.E
OUT = os.path.abspath(os.path.join(HERE, ".."))
K.OUT = OUT
V.OUT = OUT
V.EMISSIVE |= {"guhs:paddenstoellampje", "guhs:glimpaddenstoel", "guhs:schimmelguh_licht", "guhs:lichtmosblok",
               "guhs:glimtegel", "guhs:vadslamp_licht", "guhs:wolkenlamp", "minecraft:sea_lantern",
               "minecraft:shroomlight", "minecraft:glowstone", "minecraft:lantern", "minecraft:end_rod",
               "minecraft:pearlescent_froglight", "minecraft:redstone_lamp", "guhs:glasinlood_guh",
               "guhs:glasinlood_hartje"}
Scene, has_block, stairs, slab = K.Scene, K.has_block, K.stairs, K.slab

MUSH = "[down=true,east=true,north=true,south=true,up=true,west=true]"
ROOD = "minecraft:red_mushroom_block" + MUSH
BRUIN = "minecraft:brown_mushroom_block" + MUSH
STEEL = "minecraft:mushroom_stem" + MUSH


def hol_maken(S):
    """Zet blokken die aan alle 6 kanten door volle blokken zijn ingesloten op lucht. Onzichtbaar, scheelt
    rendertijd (de renderer loopt in Python over elk blok). Alleen voor de render, niet voor de bouw."""
    r, bk = V.res()
    full = np.array([s != "minecraft:air" and bk.bake(s).full_opaque if i else False
                     for i, s in enumerate(S.pal)])
    occ = full[S.v]
    inn = occ.copy()
    inn[0, :, :] = inn[-1, :, :] = False
    inn[:, 0, :] = inn[:, -1, :] = False
    inn[:, :, 0] = inn[:, :, -1] = False
    inn[1:-1, 1:-1, 1:-1] &= occ[:-2, 1:-1, 1:-1] & occ[2:, 1:-1, 1:-1] & occ[1:-1, :-2, 1:-1] & \
        occ[1:-1, 2:, 1:-1] & occ[1:-1, 1:-1, :-2] & occ[1:-1, 1:-1, 2:]
    # bovenste laag van een kolom niet weghalen (AO kijkt naar 'iets boven')
    S.v[inn] = 0
    return int(inn.sum())


def deel(S, x1, y1, z1, x2, y2, z2):
    """Nieuwe Scene met een uitsnede (voor close-ups)."""
    T = Scene(1, 1, 1)
    T.pal = list(S.pal)
    T.idx = dict(S.idx)
    T.v = S.v[x1:x2, y1:y2, z1:z2].copy()
    T.guhs = [(x - x1, y - y1, z - z1, a, b, c) for (x, y, z, a, b, c) in S.guhs
              if x1 <= x < x2 and y1 <= y < y2 and z1 <= z < z2]
    return T


def render(S, name, **kw):
    return K.render(S, name, **kw)


# ------------------------------------------------------------------ eigen render: lucht, manen, schemer
def lucht(w, h, boven, onder, manen=True, sterren=False, seed=3):
    """Zone-1-lucht: zacht verloop, twee manen, optioneel sterren met een guh-sterrenbeeld."""
    import random
    bg = V.gradient(w, h, boven, onder).convert("RGBA")
    d = ImageDraw.Draw(bg, "RGBA")
    rng = random.Random(seed)
    if sterren:
        for _ in range(int(w * h / 2500)):
            x, y = rng.random() * w, rng.random() * h * 0.6
            a = int(120 + 135 * rng.random())
            s = 1 if rng.random() < 0.8 else 2
            d.ellipse((x - s, y - s, x + s, y + s), fill=(255, 245, 250, a))
        # guh-sterrenbeeld: hoofdje met oortjes
        gx, gy, gs = w * 0.16, h * 0.15, min(w, h) * 0.06
        pts = [(-1, 0.2), (-1, -0.6), (-0.75, -1.15), (-0.45, -0.7), (0.45, -0.7), (0.75, -1.15), (1, -0.6),
               (1, 0.2), (0.5, 0.55), (-0.5, 0.55)]
        P = [(gx + u * gs, gy + v * gs) for (u, v) in pts]
        d.line(P + [P[0]], fill=(255, 220, 240, 110), width=max(1, int(gs / 30)))
        for (x, y) in P + [(gx - 0.4 * gs, gy - 0.1 * gs), (gx + 0.4 * gs, gy - 0.1 * gs)]:
            d.ellipse((x - 3, y - 3, x + 3, y + 3), fill=(255, 250, 255, 255))
    if manen:
        for (mx, my, mr, kl) in ((0.80, 0.10, 0.04, (255, 236, 244)), (0.90, 0.19, 0.022, (226, 214, 255))):
            cx, cy, r = w * mx, h * my, min(w, h) * mr
            for k in range(4, 0, -1):
                rr = r * (1 + k * 0.12)
                d.ellipse((cx - rr, cy - rr, cx + rr, cy + rr), fill=kl + (10,))
            d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=kl + (255,))
            for (u, v, s) in ((-0.3, -0.2, 0.18), (0.25, 0.3, 0.12), (0.35, -0.35, 0.08)):
                d.ellipse((cx + u * r - s * r, cy + v * r - s * r, cx + u * r + s * r, cy + v * r + s * r),
                          fill=tuple(int(c * 0.93) for c in kl) + (255,))
    return bg


def render2(S, name, yaw=35, pitch=30, scale=6, ss=1, extra=(), labels=(), titel=None,
            sky=((246, 214, 226), (255, 236, 214)), schemer=False, sterren=False, manen=True, hol=True,
            onderschrift=None):
    """Zoals erfkit.render, maar met de zone-1-lucht (twee manen) en optioneel een schemerversie waarin
    alleen lichtblokken gloeien (tweede pass met shade = 0 als lichtmasker). Schrijft name.png en,
    met labels, ook name_namen.png."""
    r, bk = V.res()
    S.fix_connections()
    vol = S.v.copy()
    if hol:
        T = Scene(1, 1, 1)
        T.pal, T.v = S.pal, vol
        n = hol_maken(T)
        print("binnenblokken weg:", n, flush=True)
    extras, hide = E.special_extras(vol, S.pal, bk)
    for (x, y, z, yaw_g, skin, sitting) in S.guhs:
        extras += E.guh(r, x, y, z, yaw_g, skin=skin, scale=0.8, sitting=sitting)
    extras = list(extras) + list(extra)
    cam = R.Camera(yaw, pitch, scale * ss)
    t = time.time()
    em = V.EMISSIVE
    im, (ox, oy) = R.render(vol, S.pal, cam, r, bk, hide=lambda s: s in hide, extras=extras, margin=30 * ss,
                            emissive=em)
    print(name, "pass 1 %.1fs" % (time.time() - t), im.size, flush=True)
    glow = None
    if schemer:
        oud = R.shade
        R.shade = lambda n_: 0.0
        try:
            glow, _ = R.render(vol, S.pal, cam, r, bk, hide=lambda s: s in hide, extras=extras, margin=30 * ss,
                               emissive=em)
        finally:
            R.shade = oud
        print(name, "pass 2 %.1fs" % (time.time() - t), flush=True)
    W, H = im.width // ss, im.height // ss
    im = im.resize((W, H), Image.LANCZOS)
    bg = lucht(W, H, *sky, manen=manen, sterren=sterren)
    if schemer:
        glow = glow.resize((W, H), Image.LANCZOS)
        a = np.asarray(im, np.float32) / 255.0
        g = np.asarray(glow, np.float32)[..., :3] / 255.0
        rgb = a[..., :3] * np.array([0.40, 0.34, 0.52]) + np.array([0.05, 0.03, 0.08])
        rgb = np.maximum(rgb, g * 1.05)
        gl = Image.fromarray((np.clip(g, 0, 1) * 255).astype(np.uint8))
        b1 = np.asarray(gl.filter(ImageFilter.GaussianBlur(3)), np.float32) / 255.0
        b2 = np.asarray(gl.filter(ImageFilter.GaussianBlur(14)), np.float32) / 255.0
        bloom = b1 * 0.9 + b2 * 2.2 * np.array([1.0, 0.8, 0.75])
        al = a[..., 3:4]
        city = np.clip(rgb, 0, 1)
        im = Image.fromarray((np.concatenate([city, al], 2) * 255).astype(np.uint8), "RGBA")
        out = bg.copy()
        out.paste(im, (0, 0), im)
        o = np.asarray(out, np.float32)[..., :3] / 255.0
        o = 1 - (1 - o) * (1 - np.clip(bloom, 0, 1) * 0.8)
        out = Image.fromarray((np.clip(o, 0, 1) * 255).astype(np.uint8)).convert("RGBA")
    else:
        out = bg.copy()
        sh = Image.new("RGBA", im.size, (90, 50, 90, 0))
        a = im.split()[3].filter(ImageFilter.GaussianBlur(14))
        sh.putalpha(a.point(lambda v: int(v * 0.25)))
        out.paste(sh, (10, 16), sh)
        out.paste(im, (0, 0), im)
    out = out.convert("RGB")
    d = ImageDraw.Draw(out)
    kl1, kl2 = ((255, 230, 245), (240, 210, 230)) if schemer else ((120, 40, 90), (140, 80, 110))
    if titel:
        d.text((16, 12), titel, font=K._font(26, True), fill=kl1)
    if onderschrift:
        d.text((16, 46), onderschrift, font=K._font(16), fill=kl2)
    path = os.path.join(OUT, name + ".png")
    out.save(path)
    if labels:
        o2 = out.copy()
        d2 = ImageDraw.Draw(o2)
        for (x, y, z, tekst) in labels:
            sx, sy, _ = cam.project(np.array([x, y, z], float))
            K.ballon(d2, (sx + ox) / ss, (sy + oy) / ss, tekst, sz=17)
        o2.save(os.path.join(OUT, name + "_namen.png"))
    print(name, "klaar %.1fs" % (time.time() - t), out.size, flush=True)
    return path
