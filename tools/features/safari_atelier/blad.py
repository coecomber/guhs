"""
Voorstelvellen van de safaridieren: per soort een vel (voor, zij, schuin, en schuin naast een guh) per variant
(volwassen / mannetje / jong), plus een overzichtsvel van de hele batch naast een guh. Alles op vaste schaal per vel.

    python safari-ontwerp/dieren/_bron/blad.py [soort ...]
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HIER = os.path.dirname(os.path.abspath(__file__))
UIT = os.path.abspath(os.path.join(HIER, "..", "..", "..", "build", "safari_atelier"))   # staging (models + proposal sheets; not shipped)
GUHS = os.path.abspath(os.path.join(HIER, "..", "..", ".."))
sys.path.insert(0, os.path.join(GUHS, "tools"))
os.chdir(GUHS)                                       # wiki_renders zoekt de guhs-assets relatief
import wiki_renders as wr                            # noqa: E402

GUH_GEO = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity", "guh.geo.json")
BG = (238, 234, 242, 255)
NAMEN = {"rode_panda": "Rode panda", "olifant": "Olifant", "pinguin": "Pinguïn", "giraffe": "Giraffe", "bever": "Bever",
         "flamingo": "Flamingo"}


def font(n, bold=False):
    try:
        return ImageFont.truetype("arialbd.ttf" if bold else "arial.ttf", n)
    except OSError:
        return ImageFont.load_default()


def dier_quads(soort, variant=""):
    naam = soort + (f"_{variant}" if variant else "")
    tex = np.asarray(Image.open(os.path.join(UIT, soort, f"{naam}.png")).convert("RGBA")).astype(np.float32)
    return wr.geo_quads(os.path.join(UIT, soort, f"{naam}.geo.json"), tex)


KERN = {"root", "body", "tail", "head", "ear_left", "ear_right", "leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"}


def guh_quads():
    import json
    namen = {b["name"] for b in json.load(open(GUH_GEO))["minecraft:geometry"][0]["bones"]}
    return wr.geo_quads(GUH_GEO, "guhs:entity/guh", hide=namen - KERN)


def render_vast(quads, yaw, pitch, ppb, W, H, ss=2):
    """Zoals wiki_renders.render, maar met vaste schaal (ppb = pixels per blok), gecentreerd op het model."""
    R = wr.rot_matrix(yaw, pitch)
    pts = np.array([R @ (q.origin + a * q.u + b * q.v) for q in quads for a in (0, 1) for b in (0, 1)])
    center = (pts[:, :2].min(0) + pts[:, :2].max(0)) / 2
    scale = ppb * ss
    Ws, Hs = W * ss, H * ss
    zbuf = np.full((Hs, Ws), np.inf)
    col = np.zeros((Hs, Ws, 4), np.float32)
    light = np.array([-0.35, 0.85, -0.4]); light /= np.linalg.norm(light)
    for q in quads:
        n = R @ q.normal
        if n[2] > 1e-6:
            continue
        shade = 0.62 + 0.38 * max(0.0, float(np.dot(q.normal, light)))
        tex = wr.tex_array(q.tex) if isinstance(q.tex, str) else q.tex
        th, tw = tex.shape[:2]
        u0, v0, u1, v1 = q.uv
        lu, lv = np.linalg.norm(R @ q.u) * scale, np.linalg.norm(R @ q.v) * scale
        nu, nv = max(2, int(lu * 1.6) + 1), max(2, int(lv * 1.6) + 1)
        A, B = np.meshgrid((np.arange(nu) + 0.5) / nu, (np.arange(nv) + 0.5) / nv)
        P = q.origin + A[..., None] * q.u + B[..., None] * q.v
        Sx = P @ R.T
        px = ((Sx[..., 0] - center[0]) * scale + Ws / 2).astype(int)
        py = (-(Sx[..., 1] - center[1]) * scale + Hs / 2).astype(int)
        z = Sx[..., 2]
        tu = np.clip((u0 + A * (u1 - u0)) * tw, 0, tw - 1).astype(int)
        tv = np.clip((v0 + B * (v1 - v0)) * th, 0, th - 1).astype(int)
        rgba = tex[tv, tu]
        m = (px >= 0) & (px < Ws) & (py >= 0) & (py < Hs) & (rgba[..., 3] > 20)
        px, py, z, rgba = px[m], py[m], z[m], rgba[m]
        order = np.argsort(-z)
        px, py, z, rgba = px[order], py[order], z[order], rgba[order]
        for X, Y, Z, C in zip(px, py, z, rgba):
            if Z < zbuf[Y, X]:
                zbuf[Y, X] = Z
                col[Y, X, :3] = C[:3] * shade
                col[Y, X, 3] = 255
    img = Image.fromarray(np.clip(col, 0, 255).astype(np.uint8))
    return img.resize((W, H), Image.LANCZOS)


def hoogte(quads):
    ys = [p[1] for q in quads for p in (q.origin, q.origin + q.u, q.origin + q.v, q.origin + q.u + q.v)]
    xs = [p[0] for q in quads for p in (q.origin, q.origin + q.u, q.origin + q.v, q.origin + q.u + q.v)]
    zs = [p[2] for q in quads for p in (q.origin, q.origin + q.u, q.origin + q.v, q.origin + q.u + q.v)]
    return max(ys) - min(ys), max(max(xs) - min(xs), max(zs) - min(zs))


def xbreed(quads):
    xs = [p[0] for q in quads for p in (q.origin, q.origin + q.u, q.origin + q.v, q.origin + q.u + q.v)]
    return max(xs) - min(xs)


def span(quads, yaw, pitch):
    R = wr.rot_matrix(yaw, pitch)
    pts = np.array([R @ (q.origin + a * q.u + b * q.v) for q in quads for a in (0, 1) for b in (0, 1)])
    return tuple(pts[:, :2].max(0) - pts[:, :2].min(0))


def naast(a, b, dx):
    return a + wr.offset_quads(b, dx=dx)


VIEWS = [("voor", 0, -6), ("zij", -90, -6), ("schuin", 35, -22)]


def vel(soort):
    varianten = [("", "volwassen" + (" (vrouwtje)" if os.path.exists(os.path.join(UIT, soort, f"{soort}_man.geo.json")) else ""))]
    if os.path.exists(os.path.join(UIT, soort, f"{soort}_man.geo.json")):
        varianten.append(("man", "mannetje"))
    varianten.append(("baby", "jong"))
    guh = guh_quads()
    gh, gw = hoogte(guh)
    maxh, maxw = gh, gw
    qs = {}
    for v, _ in varianten:
        qs[v] = dier_quads(soort, v)
        h, w = hoogte(qs[v])
        maxh, maxw = max(maxh, h), max(maxw, w)
    CW, CH = 360, 380
    ppb = min(170, (CH - 40) / maxh, (CW - 30) / (maxw * 1.05))
    GW = 560
    W, H = 200 + CW * 3 + GW, 90 + CH * len(varianten) + 40
    img = Image.new("RGBA", (W, H), BG)
    d = ImageDraw.Draw(img)
    d.text((20, 18), f"{NAMEN[soort]}  (safari-stijlproef, batch 1)", font=font(30, True), fill=(40, 30, 40))
    d.text((20, 56), "voor / zij / schuin, en schuin naast een guh (zelfde schaal). Placeholders: animaties volgen in de game.",
           font=font(16), fill=(80, 70, 80))
    for c, (lab, _, _) in enumerate(VIEWS + [("naast een guh", 0, 0)]):
        d.text((200 + c * CW + 10, 90 - 2), lab, font=font(16, True), fill=(90, 80, 90))
    combs = {}
    ppb_g = ppb * 0.8
    for v, _ in varianten:
        dier = dier_quads(soort, v)                                  # verse quads: offset_quads verschuift ter plekke
        combs[v] = naast(dier, guh_quads(), dx=-(xbreed(dier) / 2 + xbreed(guh) / 2 + 0.3))
        sx, sy = span(combs[v], 35, -18)
        ppb_g = min(ppb_g, (GW - 30) / sx, (CH - 50) / sy)
    for r, (v, lab) in enumerate(varianten):
        y = 110 + r * CH
        d.text((20, y + CH / 2 - 12), lab, font=font(20, True), fill=(50, 40, 50))
        for c, (_, yaw, pitch) in enumerate(VIEWS):
            t = render_vast(qs[v], yaw, pitch, ppb, CW, CH - 20)
            img.alpha_composite(t, (200 + c * CW, y))
        t = render_vast(combs[v], 35, -18, ppb_g, GW, CH - 20)
        img.alpha_composite(t, (200 + 3 * CW, y))
    pad = os.path.join(UIT, soort, f"{soort}_voorstel.png")
    img.convert("RGB").save(pad)
    print(pad)


def overzicht(soorten):
    guh = guh_quads()
    gh, gw = hoogte(guh)
    rij_v, rij_b = [], []
    x = 0.0
    items = [("guh", guh, None)] + [(s, dier_quads(s), dier_quads(s, "baby")) for s in soorten]
    quads = []
    labels = []
    for naam, q, qb in items:
        h, w = hoogte(q)
        quads += wr.offset_quads(q, dx=(x + w / 2))
        if qb is not None:
            quads += wr.offset_quads(qb, dx=(x + w / 2), dz=-(w * 0.7 + 0.6))
        labels.append((naam, x + w / 2))
        x += w + 0.7
    ppb = 95
    W, H = int(x * ppb * 0.95) + 200, 700
    t = render_vast(quads, 18, -16, ppb, W, H - 100)
    img = Image.new("RGBA", (W, H), BG)
    img.alpha_composite(t, (0, 90))
    d = ImageDraw.Draw(img)
    d.text((20, 18), "Safari-stijlproef batch 1: alle zes naast een guh (volwassen achter, jong ervoor)", font=font(28, True), fill=(40, 30, 40))
    d.text((20, 56), "Rode panda · Olifant · Pinguïn · Giraffe · Bever · Flamingo (van links naar rechts, na de guh)", font=font(17), fill=(80, 70, 80))
    pad = os.path.join(UIT, "batch1_overzicht.png")
    img.convert("RGB").save(pad)
    print(pad)


if __name__ == "__main__":
    soorten = [a for a in sys.argv[1:] if a != "overzicht"] or ["rode_panda", "olifant", "pinguin", "giraffe", "bever", "flamingo"]
    for s in soorten:
        vel(s)
    if len(sys.argv) == 1 or "overzicht" in sys.argv:
        overzicht(["rode_panda", "olifant", "pinguin", "giraffe", "bever", "flamingo"])
