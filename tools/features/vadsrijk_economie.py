"""
1.10.0 "Het Vadsrijk", the economie slice (CONTRACT_VADSRIJK.md). Java: feature/vadsrijk/economie.

  - pixel art (16x16, drawn from primitives + an automatic dark outline) for the coins (guh face), potions (guh face in the
    liquid), smid materials, the rijfluitje, the saddles and the 16 extra gear pieces;
  - the guh clothes (CLOTHES / clothes() / icons(): existing bones only, so the guh model never changes);
  - the 4 trophy/deco blocks (block models from cuboids, guh faces), their blockstates and loot tables;
  - (the mounts Hoedhupper / Klodderros were removed in I-MOUNTS 2026-11-04; the mount system's assets: vadsrijk_rijdieren.py);
  - every text (Dutch; en_us gets the same Dutch).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_economie.py
"""
import json
import math
import os

import numpy as np
from PIL import Image

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
D = os.path.join(R, "data", "guhs")
ITEM_TEX = "vadsrijk/economie"          # textures/item/vadsrijk/economie/<id>.png
BLOCK_TEX = "vadsrijk/economie"         # textures/block/vadsrijk/economie/<name>.png


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _png(pad, a):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA").save(pad)


# ======================================================================================================================
# a tiny 16x16 painter
# ======================================================================================================================
class Doek:
    def __init__(self, n=16):
        self.n = n
        self.a = np.zeros((n, n, 4), np.float32)

    def px(self, x, y, c):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < self.n and 0 <= y < self.n:
            self.a[y, x] = (*c[:3], 255)

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, c)

    def disc(self, cx, cy, r, c, alleen_boven=None):
        for y in range(self.n):
            for x in range(self.n):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r and (alleen_boven is None or y <= alleen_boven):
                    self.px(x, y, c)

    def lijn(self, x0, y0, x1, y1, c, dik=1):
        n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
        for i in range(n + 1):
            t = i / n
            x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            for dx in range(dik):
                for dy in range(dik):
                    self.px(x + dx, y + dy, c)

    def poly(self, pts, c):
        for y in range(self.n):
            for x in range(self.n):
                if _in_poly(x + 0.5, y + 0.5, pts):
                    self.px(x, y, c)

    def wis(self, x0, y0, x1, y1):
        self.a[y0:y1 + 1, x0:x1 + 1] = 0

    def gezicht(self, cx, cy, oog=(30, 20, 40), blos=(255, 140, 180), breed=2):
        self.px(cx - breed, cy, oog)
        self.px(cx + breed + 1, cy, oog)
        self.px(cx - breed, cy - 1, (255, 255, 255)) if False else None
        self.px(cx - breed - 1, cy + 1, blos)
        self.px(cx + breed + 2, cy + 1, blos)
        self.px(cx, cy + 1, oog)
        self.px(cx + 1, cy + 1, oog)

    def schaduw(self, licht=0.0):
        """Shading: a little lighter top-left, darker bottom-right (on the painted pixels)."""
        for y in range(self.n):
            for x in range(self.n):
                if self.a[y, x, 3] > 0:
                    f = 1 + 0.10 * ((self.n - x - y) / self.n) + licht
                    self.a[y, x, :3] = np.clip(self.a[y, x, :3] * f, 0, 255)

    def rand(self):
        """A dark outline around everything (the darkened colour of the neighbour)."""
        a = self.a.copy()
        for y in range(self.n):
            for x in range(self.n):
                if a[y, x, 3] > 0:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < self.n and 0 <= yy < self.n and a[yy, xx, 3] > 0:
                        self.a[y, x] = (*(a[yy, xx, :3] * 0.42), 255)
                        break
        return self

    def png(self, pad):
        _png(pad, self.a)


def _in_poly(x, y, pts):
    binnen = False
    j = len(pts) - 1
    for i in range(len(pts)):
        xi, yi = pts[i]
        xj, yj = pts[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi + 1e-9) + xi:
            binnen = not binnen
        j = i
    return binnen


def _licht(c, f):
    return tuple(min(255, int(v * f)) for v in c)


# ======================================================================================================================
# item icons
# ======================================================================================================================
def munt(kleur):
    d = Doek()
    d.disc(7.5, 7.5, 6.6, _licht(kleur, 0.8))
    d.disc(7.5, 7.5, 5.4, kleur)
    d.disc(6.5, 6.0, 2.0, _licht(kleur, 1.18))
    # little guh ears on the rim
    d.px(4, 1, _licht(kleur, 0.8))
    d.px(11, 1, _licht(kleur, 0.8))
    d.px(5, 7, (40, 26, 30))
    d.px(10, 7, (40, 26, 30))
    d.px(5, 6, (255, 255, 255))
    d.px(4, 9, (255, 130, 170))
    d.px(11, 9, (255, 130, 170))
    d.px(7, 9, (90, 50, 50))
    d.px(8, 9, (90, 50, 50))
    return d.rand()


def drankje(vloeistof):
    d = Doek()
    d.rect(6, 1, 9, 2, (160, 110, 70))
    d.rect(7, 3, 8, 5, (210, 235, 250))
    d.disc(7.5, 10, 5.2, (215, 238, 252))
    d.disc(7.5, 10.5, 4.4, vloeistof)
    d.rect(4, 7, 11, 7, (215, 238, 252))
    d.px(5, 8, (255, 255, 255))
    d.px(5, 9, (255, 255, 255))
    d.px(6, 10, (40, 26, 40))
    d.px(9, 10, (40, 26, 40))
    d.px(5, 11, _licht(vloeistof, 1.25))
    d.px(10, 11, _licht(vloeistof, 1.25))
    d.px(7, 12, (60, 30, 50))
    d.px(8, 12, (60, 30, 50))
    return d.rand()


def drankje_klein(vloeistof):
    """(balans 2026-11) The Klein Vadsdrankje: the same flask, smaller and lower in the icon."""
    d = Doek()
    d.rect(7, 4, 8, 5, (160, 110, 70))
    d.rect(7, 6, 8, 7, (210, 235, 250))
    d.disc(7.5, 11, 3.8, (215, 238, 252))
    d.disc(7.5, 11.4, 3.1, vloeistof)
    d.px(5, 10, (255, 255, 255))
    d.px(6, 11, (40, 26, 40))
    d.px(9, 11, (40, 26, 40))
    d.px(7, 13, (60, 30, 50))
    d.px(8, 13, (60, 30, 50))
    return d.rand()


def steentje():
    d = Doek()
    d.poly([(8, 1), (14.5, 7), (8, 15), (1.5, 7)], (240, 110, 200))
    d.poly([(8, 1), (8, 7), (1.5, 7)], (255, 170, 230))
    d.poly([(8, 7), (14.5, 7), (8, 15)], (200, 70, 170))
    d.px(6, 4, (255, 255, 255))
    d.px(13, 2, (255, 250, 200))
    d.px(14, 1, (255, 250, 200))
    d.px(2, 12, (255, 250, 200))
    return d.rand()


def knabbel():
    d = Doek()
    d.disc(7.5, 7.5, 6.5, (226, 168, 72))
    d.disc(6.5, 6.5, 3, (240, 196, 110))
    for x, y in ((5, 4), (10, 6), (4, 9), (9, 10), (7, 7), (11, 9)):
        d.px(x, y, (120, 70, 40))
    for x, y in ((6, 11), (10, 3), (3, 6), (12, 11)):
        d.px(x, y, (255, 110, 190))
    # the swirl that rerolls
    for t in range(14):
        hoek = t / 14 * math.pi * 1.6
        d.px(7.5 + math.cos(hoek) * 2.6, 7.5 + math.sin(hoek) * 2.6, (255, 90, 170))
    return d.rand()


def draad():
    d = Doek()
    d.rect(4, 2, 11, 3, (190, 140, 90))
    d.rect(4, 12, 11, 13, (190, 140, 90))
    d.rect(5, 4, 10, 11, (255, 140, 200))
    for y in range(4, 12, 2):
        d.rect(5, y, 10, y, (255, 190, 225))
    d.lijn(10, 9, 14, 14, (255, 140, 200))
    return d.rand()


def fluitje():
    d = Doek()
    d.rect(3, 7, 10, 11, (250, 200, 70))
    d.disc(4.5, 9, 2.6, (250, 200, 70))
    d.rect(11, 8, 14, 9, (230, 170, 50))
    d.px(6, 7, (90, 60, 30))
    d.px(7, 7, (90, 60, 30))
    d.px(4, 8, (255, 240, 180))
    d.lijn(2, 9, 1, 3, (255, 120, 190))
    d.lijn(1, 3, 6, 2, (255, 120, 190))
    return d.rand()


def zadel(kap, stip, rug=(150, 90, 60)):
    d = Doek()
    d.rect(2, 10, 13, 12, rug)
    d.rect(3, 13, 4, 14, _licht(rug, 0.8))
    d.rect(11, 13, 12, 14, _licht(rug, 0.8))
    d.disc(7.5, 9, 5.5, kap, alleen_boven=8)
    for x, y in stip:
        d.px(x, y, (255, 255, 255))
    d.px(6, 7, (40, 26, 40))
    d.px(9, 7, (40, 26, 40))
    return d.rand()


def zwaard(blad, gevest=(240, 190, 60)):
    d = Doek()
    d.lijn(5, 10, 13, 2, blad, dik=2)
    d.lijn(6, 10, 13, 3, _licht(blad, 1.2))
    d.lijn(3, 8, 7, 12, gevest, dik=2)
    d.lijn(1, 14, 4, 11, (130, 80, 60), dik=1)
    d.px(1, 14, gevest)
    return d.rand()


def boog(hout, puf):
    d = Doek()
    for t in range(40):
        hoek = -1.15 + t / 39 * 2.3
        d.px(3 + math.cos(hoek) * 8.5, 7.5 + math.sin(hoek) * 7, hout)
    d.lijn(6.5, 0.8, 6.5, 14.2, (250, 250, 250))
    d.disc(7, 1, 1.4, puf)
    d.disc(7, 14, 1.4, puf)
    d.lijn(12, 3, 4, 12, (200, 160, 120))
    return d.rand()


def katapult():
    d = Doek()
    d.lijn(8, 15, 8, 9, (150, 100, 70), dik=2)
    d.lijn(8, 9, 4, 3, (150, 100, 70), dik=2)
    d.lijn(9, 9, 12, 3, (150, 100, 70), dik=2)
    d.lijn(4, 3, 8, 6, (255, 120, 190))
    d.lijn(12, 3, 8, 6, (255, 120, 190))
    d.disc(8, 6, 1.6, (110, 200, 240))
    return d.rand()


def staf(top):
    d = Doek()
    d.lijn(2, 14, 10, 6, (160, 110, 80), dik=2)
    top(d)
    return d.rand()


def _orb(d):
    d.disc(11.5, 4, 3, (255, 110, 190))
    d.disc(10.8, 3.2, 1, (255, 220, 240))
    d.px(14, 1, (255, 250, 200))
    d.px(8, 1, (255, 250, 200))


def _zwamtop(d):
    d.disc(11, 5, 4, (220, 60, 70), alleen_boven=5)
    d.rect(10, 6, 12, 7, (245, 230, 210))
    for x, y in ((9, 3), (12, 2), (13, 4)):
        d.px(x, y, (255, 255, 255))


def helm(c, extra=None):
    d = Doek()
    d.disc(7.5, 9, 6, c, alleen_boven=9)
    d.rect(1, 9, 14, 11, _licht(c, 0.85))
    d.rect(4, 8, 11, 8, _licht(c, 0.6))
    if extra:
        extra(d)
    return d.rand()


def borst(c, rand=None):
    d = Doek()
    d.rect(3, 3, 12, 14, c)
    d.rect(1, 3, 14, 6, c)
    d.wis(6, 3, 9, 4)
    if rand:
        d.rect(7, 6, 8, 13, rand)
    return d.rand()


def broek(c):
    d = Doek()
    d.rect(3, 2, 12, 5, c)
    d.rect(3, 6, 6, 14, c)
    d.rect(9, 6, 12, 14, c)
    d.rect(3, 2, 12, 2, _licht(c, 0.75))
    return d.rand()


def sloffen(c):
    d = Doek()
    for ox in (0, 7):
        d.rect(2 + ox, 7, 5 + ox, 13, c)
        d.rect(2 + ox, 11, 7 + ox, 13, c)
        d.px(6 + ox, 11, _licht(c, 1.25))
    return d.rand()


def kroon(goud=(250, 200, 60), slijm=(110, 200, 240)):
    d = Doek()
    d.rect(2, 7, 13, 11, goud)
    for x in (2, 7, 12):
        d.poly([(x - 0.5, 7.5), (x + 1, 2.5), (x + 2.5, 7.5)], goud)
    d.px(5, 9, (255, 80, 160))
    d.px(10, 9, (255, 80, 160))
    d.px(7, 9, (120, 220, 255))
    d.rect(3, 12, 3, 13, slijm)
    d.rect(8, 12, 8, 14, slijm)
    d.rect(12, 12, 12, 12, slijm)
    return d.rand()


def _pluim(d):
    d.lijn(7, 3, 9, 0, (255, 100, 180), dik=2)


def _stippen(d):
    for x, y in ((4, 6), (9, 4), (11, 7), (6, 4)):
        d.px(x, y, (255, 255, 255))


def cape():
    d = Doek()
    d.poly([(4, 1), (11, 1), (14, 15), (1, 15)], (255, 110, 180))
    d.rect(4, 1, 11, 2, (250, 200, 60))
    d.disc(7.5, 8, 2, (250, 200, 60))
    d.px(7, 8, (255, 110, 180))
    return d.rand()


def sjaal():
    d = Doek()
    for x in range(2, 14):
        d.rect(x, 4, x, 7, (255, 110, 180) if (x // 2) % 2 == 0 else (250, 210, 80))
    for y in range(8, 15):
        d.rect(9, y, 11, y, (255, 110, 180) if (y // 2) % 2 == 0 else (250, 210, 80))
    return d.rand()


PLUIS, KLODDER, ZWAM = (255, 205, 228), (110, 210, 205), (220, 70, 80)
ICONEN = {
    "vr_item_munt_brons": lambda: munt((205, 128, 60)),
    "vr_item_munt_zilver": lambda: munt((205, 210, 225)),
    "vr_item_munt_goud": lambda: munt((255, 205, 50)),
    "vr_item_vadsdrankje": lambda: drankje((255, 80, 160)),
    "vr_item_klein_vadsdrankje": lambda: drankje_klein((255, 110, 180)),
    "vr_item_energiedrankje": lambda: drankje((80, 190, 255)),
    "vr_item_vahoegdrankje": lambda: drankje((255, 170, 40)),
    "vr_item_snelpootjesdrankje": lambda: drankje((110, 225, 110)),
    "vr_item_verbetersteentje": steentje,
    "vr_item_herrolknabbel": knabbel,
    "vr_item_pluisdraad": draad,
    "vr_item_rijfluitje": fluitje,
}
HANDHELD = set()   # (the gear base types moved to their owners at the 1.10.0 integration: klassen/wezens/bazen)

# ======================================================================================================================
# guh clothes (GuhClothes VR_ITEM_*; existing bones only)
# ======================================================================================================================
CLOTHES = ["vr_item_ridderhelmpje", "vr_item_zwammenhoedje", "vr_item_klodderkroontje", "vr_item_riddercape", "vr_item_vadsrijksjaal"]
_KLEDING_ICOON = {
    "vr_item_ridderhelmpje": lambda: helm((205, 210, 225), _pluim),
    "vr_item_zwammenhoedje": lambda: helm(ZWAM, _stippen),
    "vr_item_klodderkroontje": lambda: kroon((120, 210, 240), (255, 140, 200)),
    "vr_item_riddercape": cape,
    "vr_item_vadsrijksjaal": sjaal,
}
# swatch per piece: {swatch: (painter name, colours)}
_KLEDING_SWATCH = {
    "vr_item_ridderhelmpje": {"helmet": ("metaal", (200, 205, 220), (255, 110, 180))},
    "vr_item_zwammenhoedje": {"rain_hat": ("stippen", (220, 60, 70), (255, 255, 255))},
    "vr_item_klodderkroontje": {"crown": ("slijm", (120, 210, 240), (255, 230, 120))},
    "vr_item_riddercape": {"cape": ("embleem", (255, 110, 180), (250, 200, 60))},
    "vr_item_vadsrijksjaal": {"scarf": ("strepen", (255, 110, 180), (250, 210, 80))},
}


def _swatch(soort, c1, c2, rng, px=32):
    a = np.zeros((px, px, 3), np.float32) + np.array(c1, np.float32)
    a += rng.normal(0, 4, (px, px, 1))
    if soort == "metaal":
        for y in range(px):
            a[y, :] *= 0.9 + 0.2 * (y % 8 < 2)
        a[px // 2 - 2:px // 2 + 2, :] = c2
    elif soort == "stippen":
        for y in range(2, px, 8):
            for x in range((y // 8) % 2 * 4 + 2, px, 8):
                a[y:y + 3, x:x + 3] = c2
    elif soort == "slijm":
        for x in range(0, px, 6):
            a[:, x:x + 2] *= 1.12
        a[0:3, :] = c2
    elif soort == "embleem":
        yy, xx = np.mgrid[0:px, 0:px]
        a[((yy - px / 2) ** 2 + (xx - px / 2) ** 2) < (px / 6) ** 2] = c2
        a[0:2, :] = c2
    elif soort == "strepen":
        for y in range(px):
            if (y // 4) % 2:
                a[y, :] = c2
    return np.clip(a, 0, 255)


def clothes(rng, v):
    """make_guh_variants hook: {piece: {swatch: painter}} (the swatches of the existing outfit bones)."""
    uit = {}
    for naam, sw in _KLEDING_SWATCH.items():
        uit[naam] = {s: (lambda s0=soort, a=c1, b=c2: _swatch(s0, a, b, rng)) for s, (soort, c1, c2) in sw.items()}
    return uit


def icons(ic):
    """make_clothes_icons hook: {piece: PIL image}."""
    return {k: Image.fromarray(np.clip(f().a, 0, 255).astype(np.uint8), "RGBA") for k, f in _KLEDING_ICOON.items()}


def _kleding_texturen():
    """Standalone: paint the clothes sheets on the swatch spots of the existing bones (read from guh.geo.json)."""
    geo = json.load(open(os.path.join(A, "geckolib", "models", "entity", "guh.geo.json"), encoding="utf-8"))["minecraft:geometry"][0]
    plek = {}
    for b in geo["bones"]:
        if b["name"].startswith("outfit_") and b.get("cubes"):
            uv = b["cubes"][0]["uv"]["north"]["uv"]
            plek[b["name"][len("outfit_"):]] = uv
    w, h = geo["description"]["texture_width"] * 4, geo["description"]["texture_height"] * 4
    rng = np.random.default_rng(11000)
    for naam, painters in clothes(rng, None).items():
        a = np.zeros((h, w, 4), np.uint8)
        for swatch, paint in painters.items():
            u, v = plek[swatch]
            px = paint().astype(np.uint8)
            a[v * 4:v * 4 + 32, u * 4:u * 4 + 32, :3] = px
            a[v * 4:v * 4 + 32, u * 4:u * 4 + 32, 3] = 255
        _png(os.path.join(A, "textures", "entity", "guh_clothes", f"{naam}.png"), a)
    for naam, img in icons(None).items():
        img.save(os.path.join(A, "textures", "item", f"{naam}.png"))
        _schrijf(os.path.join(A, "models", "item", f"{naam}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{naam}"}})
        _schrijf(os.path.join(A, "items", f"{naam}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}})


# ======================================================================================================================
# deco blocks
# ======================================================================================================================
def _ruis(c, n=16, sterkte=8, seed=0):
    rng = np.random.default_rng(seed)
    a = np.zeros((n, n, 4), np.float32)
    a[..., :3] = np.array(c, np.float32) + rng.normal(0, sterkte, (n, n, 1))
    a[..., 3] = 255
    return a


def _gezicht_tex(c, seed, oog=(40, 26, 40), blos=(255, 140, 180), kroon=None):
    a = _ruis(c, 16, 5, seed)
    for x, y in ((4, 7), (11, 7)):
        a[y:y + 2, x:x + 1, :3] = oog
        a[y, x, :3] = (255, 255, 255) if False else oog
    a[6, 4, :3] = (255, 255, 255)
    a[6, 11, :3] = (255, 255, 255)
    a[9, 3, :3] = blos
    a[9, 12, :3] = blos
    a[10, 7:9, :3] = (90, 40, 60)
    return a


def _vaandel_tex():
    a = _ruis((255, 120, 185), 16, 6, 5)
    a[0:2, :, :3] = (250, 205, 70)
    a[14:16, :, :3] = (250, 205, 70)
    yy, xx = np.mgrid[0:16, 0:16]
    a[((yy - 8) ** 2 + (xx - 7.5) ** 2) < 12, :3] = (255, 230, 240)
    a[7, 6, :3] = (40, 26, 40)
    a[7, 9, :3] = (40, 26, 40)
    a[9, 7:9, :3] = (200, 60, 120)
    a[4, 5, :3] = (255, 230, 240)
    a[4, 10, :3] = (255, 230, 240)
    return a


def _stip_tex(c, stip, seed):
    a = _ruis(c, 16, 5, seed)
    for x, y in ((2, 2), (9, 3), (5, 8), (12, 10), (3, 12), (10, 13)):
        a[y:y + 2, x:x + 2, :3] = stip
    return a


BLOK_TEX = {
    "slijm": lambda: _ruis((140, 205, 240), seed=1, sterkte=6),
    "slijm_gezicht": lambda: _gezicht_tex((140, 205, 240), 2),
    "goud": lambda: _ruis((250, 200, 60), seed=3, sterkte=10),
    "zwamhoed": lambda: _stip_tex((220, 60, 70), (255, 250, 245), 4),
    "zwamsteel": lambda: _ruis((245, 232, 212), seed=6, sterkte=5),
    "zwamsteel_gezicht": lambda: _gezicht_tex((245, 232, 212), 7),
    "sokkel": lambda: _ruis((240, 180, 205), seed=8, sterkte=6),
    "pluche": lambda: _ruis((165, 160, 170), seed=9, sterkte=7),
    "pluche_gezicht": lambda: _gezicht_tex((165, 160, 170), 10, blos=(255, 170, 200)),
    "vaandel": _vaandel_tex,
    "paal": lambda: _ruis((190, 140, 90), seed=11, sterkte=6),
}


def _el(frm, to, tex, gezicht=None):
    faces = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        t = gezicht if (gezicht and f == "north") else tex
        face = {"texture": "#" + t}
        if gezicht and f == "north":
            face["uv"] = [0, 0, 16, 16]
        faces[f] = face
    return {"from": frm, "to": to, "faces": faces}


BLOKKEN = {
    "vr_eco_mini_koning_klodder": [
        _el([3, 0, 3], [13, 9, 13], "slijm", "slijm_gezicht"),
        _el([4, 9, 4], [12, 10, 12], "slijm"),
        _el([5, 10, 5], [11, 11, 11], "goud"),
        _el([5, 11, 5], [6, 13, 6], "goud"), _el([10, 11, 5], [11, 13, 6], "goud"),
        _el([5, 11, 10], [6, 13, 11], "goud"), _el([10, 11, 10], [11, 13, 11], "goud"),
        _el([7.5, 11, 5], [8.5, 14, 6], "goud"),
    ],
    "vr_eco_opperzwam_beeld": [
        _el([2, 0, 2], [14, 2, 14], "sokkel"),
        _el([5, 2, 5], [11, 9, 11], "zwamsteel", "zwamsteel_gezicht"),
        _el([1, 9, 1], [15, 13, 15], "zwamhoed"),
        _el([3, 13, 3], [13, 15, 13], "zwamhoed"),
        _el([6, 15, 6], [10, 16, 10], "zwamhoed"),
    ],
    "vr_eco_klodderreus_knuffel": [
        _el([2, 0, 2], [14, 10, 14], "pluche", "pluche_gezicht"),
        _el([3, 10, 3], [13, 12, 13], "pluche"),
        _el([3, 12, 6], [5, 14, 8], "pluche"), _el([11, 12, 6], [13, 14, 8], "pluche"),
        _el([6, 3, 1.5], [10, 6, 2], "slijm"),
    ],
    "vr_eco_vadsrijk_vaandel": [
        _el([5, 0, 5], [11, 1, 11], "goud"),
        _el([7, 1, 7], [9, 16, 9], "paal"),
        _el([3, 5, 6], [13, 15, 7], "vaandel", "vaandel"),
        _el([2, 15, 6.5], [14, 16, 7.5], "goud"),
    ],
}
BLOK_TEXTUREN = {
    "vr_eco_mini_koning_klodder": ["slijm", "slijm_gezicht", "goud"],
    "vr_eco_opperzwam_beeld": ["sokkel", "zwamsteel", "zwamsteel_gezicht", "zwamhoed"],
    "vr_eco_klodderreus_knuffel": ["pluche", "pluche_gezicht", "slijm"],
    "vr_eco_vadsrijk_vaandel": ["goud", "paal", "vaandel"],
}
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def _blokken():
    for naam, f in BLOK_TEX.items():
        _png(os.path.join(A, "textures", "block", BLOCK_TEX, f"{naam}.png"), f())
    for blok, els in BLOKKEN.items():
        tex = {t: f"guhs:block/{BLOCK_TEX}/{t}" for t in BLOK_TEXTUREN[blok]}
        tex["particle"] = tex[BLOK_TEXTUREN[blok][0]]
        _schrijf(os.path.join(A, "models", "block", f"{blok}.json"), {"parent": "minecraft:block/block", "textures": tex, "elements": els})
        _schrijf(os.path.join(A, "blockstates", f"{blok}.json"),
                 {"variants": {f"facing={f}": {"model": f"guhs:block/{blok}", **({"y": y} if y else {})} for f, y in FACING_Y.items()}})
        _schrijf(os.path.join(A, "items", f"{blok}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:block/{blok}"}})
        _schrijf(os.path.join(D, "loot_table", "blocks", f"{blok}.json"), {
            "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"guhs:{blok}"}],
                                                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


# ======================================================================================================================
# the mounts (GeckoLib): cuboids with per-face UV on a 128x128 sheet
# ======================================================================================================================
class Model:
    def __init__(self, naam, tw=128, th=128):
        self.naam = naam
        self.tw, self.th = tw, th
        self.bones = []
        self.tex = np.zeros((th, tw, 4), np.float32)
        self.x = self.y = self.rij = 0
        self.rng = np.random.default_rng(abs(hash(naam)) % 10000)

    def _plek(self, w, h):
        w, h = max(1, int(math.ceil(w))), max(1, int(math.ceil(h)))
        if self.x + w > self.tw:
            self.x, self.y, self.rij = 0, self.y + self.rij, 0
        p = (self.x, self.y)
        self.x += w
        self.rij = max(self.rij, h)
        assert self.y + h <= self.th, f"texture full ({self.naam})"
        return p, w, h

    def bone(self, naam, pivot, parent=None, cubes=(), rotation=None):
        b = {"name": naam, "pivot": pivot}
        if parent:
            b["parent"] = parent
        if rotation:
            b["rotation"] = rotation
        b["cubes"] = [self.cube(*c) for c in cubes]
        self.bones.append(b)
        return b

    def cube(self, origin, size, kleur, voor=None, inflate=0):
        """kleur: rgb or a function(face, w, h) -> array; voor: a painter for the north face (the front)."""
        sx, sy, sz = size
        dims = {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}
        uv = {}
        for face, (w, h) in dims.items():
            (u, v), pw, ph = self._plek(w, h)
            blok = np.zeros((ph, pw, 4), np.float32)
            c = kleur
            if face == "down":
                c = _licht(kleur, 0.8)
            elif face == "up":
                c = _licht(kleur, 1.08)
            blok[..., :3] = np.array(c, np.float32) + self.rng.normal(0, 5, (ph, pw, 1))
            blok[..., 3] = 255
            if face == "north" and voor:
                voor(blok)
            self.tex[v:v + ph, u:u + pw] = blok
            uv[face] = {"uv": [u, v], "uv_size": [w, h]}
        c = {"origin": origin, "size": list(size), "uv": uv}
        if inflate:
            c["inflate"] = inflate
        return c

    def schrijf(self):
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": f"geometry.{self.naam}", "texture_width": self.tw, "texture_height": self.th,
                            "visible_bounds_width": 4, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1.2, 0]},
            "bones": self.bones}]}
        _schrijf(os.path.join(A, "geckolib", "models", "entity", f"{self.naam}.geo.json"), geo)
        _png(os.path.join(A, "textures", "entity", f"{self.naam}.png"), self.tex)


# ======================================================================================================================
# texts (Dutch; en_us gets the same)
# ======================================================================================================================
E = "vadsrijk.economie."
TEKSTEN = {
    # items
    "item.guhs.vr_item_munt_brons": "Bronzen Vadsmunt",
    "item.guhs.vr_item_munt_zilver": "Zilveren Vadsmunt",
    "item.guhs.vr_item_munt_goud": "Gouden Vadsmunt",
    # (balans 2026-11) potions are flat numbers in tiers, 2 s to drink, 10 s shared potion cooldown
    "item.guhs.vr_item_klein_vadsdrankje": "Klein Vadsdrankje",
    "item.guhs.vr_item_klein_vadsdrankje.uitleg": "Geeft je 120 Vads terug. Een slokje aardbei voor onderweg.",
    "item.guhs.vr_item_vadsdrankje": "Vadsdrankje",
    "item.guhs.vr_item_vadsdrankje.uitleg": "Geeft je 300 Vads terug. Smaakt naar aardbei en knuffels.",
    "item.guhs.vr_item_energiedrankje": "Energiedrankje",
    "item.guhs.vr_item_energiedrankje.uitleg": "Geeft je 50 energie terug. Bruist een beetje, njeg!",
    E + "drankje.wacht": "Drinken duurt 2 tellen. Daarna moet je 10 tellen wachten voor het volgende drankje.",
    "item.guhs.vr_item_vahoegdrankje": "Vahoeg-drankje",
    "item.guhs.vr_item_vahoegdrankje.uitleg": "Een minuut lang Vahoeg: je voelt je heel vahoeg.",
    "item.guhs.vr_item_snelpootjesdrankje": "Snelpootjes-drankje",
    "item.guhs.vr_item_snelpootjesdrankje.uitleg": "Een minuut lang Snelpootjes: trippel trappel!",
    "item.guhs.vr_item_verbetersteentje": "Verbetersteentje",
    "item.guhs.vr_item_verbetersteentje.uitleg": "De smidguh tikt er een sterretje mee op je uitrusting.",
    "item.guhs.vr_item_herrolknabbel": "Herrolknabbel",
    "item.guhs.vr_item_herrolknabbel.uitleg": "Zeldzaam! De smidguh herrolt er de bonussen van je uitrusting mee.",
    "item.guhs.vr_item_pluisdraad": "Pluisdraad",
    "item.guhs.vr_item_pluisdraad.uitleg": "Om verpluisde uitrusting weer heel te naaien bij de smid.",
    "item.guhs.vr_item_rijfluitje": "Rijfluitje",
    "item.guhs.vr_item_rijfluitje.uitleg": "Fluit je rijdier erbij (nog een keer: het huppelt naar huis).",
    "block.guhs.vr_eco_mini_koning_klodder": "Mini Koning Klodder",
    "block.guhs.vr_eco_opperzwam_beeld": "Opperzwam-beeldje",
    "block.guhs.vr_eco_klodderreus_knuffel": "Klodderreus-knuffel",
    "block.guhs.vr_eco_vadsrijk_vaandel": "Vadsrijk-vaandel",
    # W-INT: the block items register without the block description prefix (26.1), so they need their own item.* name
    "item.guhs.vr_eco_mini_koning_klodder": "Mini Koning Klodder",
    "item.guhs.vr_eco_opperzwam_beeld": "Opperzwam-beeldje",
    "item.guhs.vr_eco_klodderreus_knuffel": "Klodderreus-knuffel",
    "item.guhs.vr_eco_vadsrijk_vaandel": "Vadsrijk-vaandel",
    "item.guhs.vr_item_ridderhelmpje": "Ridderhelmpje",
    "item.guhs.vr_item_zwammenhoedje": "Zwammenhoedje",
    "item.guhs.vr_item_klodderkroontje": "Klodderkroontje",
    "item.guhs.vr_item_riddercape": "Riddercape",
    "item.guhs.vr_item_vadsrijksjaal": "Vadsrijk-sjaal",
    "gui.guhs.kledingbron.vadsrijk": "Het Vadsrijk (Thuisbezorger in Zwamzoet)",
    E + "kledingprijs": "Vadsmunten",
    # shops
    E + "winkel.drankjes": "Drankjes van Juf Bubbelvads",
    E + "winkel.uitrusting": "Uitrusting bij Meester Pluisnaald",
    E + "winkel.smidwaren": "Smidwaren",
    E + "winkel.cosmetica": "De Thuisbezorger",
    E + "winkel.algemeen": "Het Zwamzoet Winkeltje",
    E + "winkel.tab.kopen": "Kopen",
    E + "winkel.tab.verkopen": "Verkopen",
    E + "winkel.koop": "Koop",
    E + "winkel.verkoop": "Verkoop",
    E + "winkel.verkoop_grijs": "Verkoop alle grijze",
    E + "winkel.leeg": "Deze winkel is leeg, njeg.",
    E + "winkel.niks_te_verkopen": "Niks om te verkopen in je tasje.",
    E + "winkel.gear_uitleg": "Gewoon, level %s (bonussen zijn een verrassing)",
    E + "winkel.per_post_kort": "Komt per post naar je huis buiten het Vadsrijk",
    E + "winkel.post_uitleg": "Cosmetica gaat per post naar huis (in de post: %s)",
    E + "winkel.titel": "Titel: %s",
    E + "winkel.in_bezit": "Al van jou",
    E + "winkel.tas_vol": "Je tasje is vol!",
    E + "winkel.te_weinig": "Te weinig Vadsmunten (nodig: %s)",
    E + "winkel.gekocht": "Gekocht! (-%s) Vahoeg!",
    E + "winkel.verkocht": "Verkocht voor %s. Dankjewel, njeg!",
    E + "winkel.geen_grijs": "Geen grijze spulletjes om te verkopen.",
    E + "winkel.per_post": "%s gaat per post naar je huis!",
    E + "winkel.titel_gekocht": "Nieuwe titel! Kies hem in je Guhdex.",
    E + "winkel.al_titel": "Die titel heb je al.",
    E + "tooltip.waarde": "Winkelwaarde: %s",
    E + "alleen_rijk": "Werkt alleen in het Vadsrijk",
    E + "munt.uitleg": "Vadsmunt (%s). Rechtsklik: in je portemonnee.",
    E + "munt.erin": "+%s in je portemonnee",
    E + "munt.buiten": "Vadsmunten zijn alleen in het Vadsrijk wat waard.",
    E + "drankje.buiten": "Dit drankje werkt alleen in het Vadsrijk.",
    E + "drankje.vads.op": "Glug glug... je Vads stroomt terug!",
    E + "drankje.energie.op": "Bruis! Je energie is terug.",
    E + "drankje.vahoeg.op": "Je voelt je helemaal vahoeg!",
    E + "drankje.snelpootjes.op": "Snelpootjes! Trippel trappel!",
    E + "tas_vol_kist": "Tasje vol: het staat klaar in /guhs vadsrijk kist",
    # smid
    E + "smid.titel": "De Smidse van Smidguh Tikkeltje",
    E + "smid.jouw_spullen": "Jouw uitrusting",
    E + "smid.geen_gear": "Geen uitrusting bij je.",
    E + "smid.kies": "Kies iets uit je tasje, njeg!",
    E + "smid.level": "Level %s",
    E + "smid.kans": "Kans op een ster: %s",
    E + "smid.risico": "Mislukt: ster eraf %s · verpluisd %s",
    E + "smid.veilig": "Mislukt: er gebeurt niks ergs",
    E + "smid.ster_kost": "Kost: %s verbetersteentje(s) +",
    E + "smid.knop.ster": "Ster erbij!",
    E + "smid.max_uitleg": "Tien sterren! Dit stuk straalt helemaal.",
    E + "smid.bonussen": "Bonussen (klik = vasthouden)",
    E + "smid.herrol_kost": "%s Herrolknabbel +",
    E + "smid.herrol_kost_slot": "%s Herrolknabbels (1 vast) +",
    E + "smid.knop.herrol": "Herrollen",
    E + "smid.is_verpluisd": "Verpluisd! (doet niks tot je het repareert)",
    E + "smid.reparatie_kost": "Kost: %s pluisdraad +",
    E + "smid.knop.repareer": "Repareren",
    E + "smid.eerst_repareren": "Eerst repareren, njeg!",
    E + "smid.max": "Al tien sterren!",
    E + "smid.geen_steentjes": "Niet genoeg verbetersteentjes.",
    E + "smid.geen_knabbels": "Niet genoeg Herrolknabbels.",
    E + "smid.geen_draad": "Niet genoeg pluisdraad.",
    E + "smid.geen_bonussen": "Dit stuk heeft geen bonussen.",
    E + "smid.te_weinig": "Te weinig Vadsmunten.",
    E + "smid.herrold": "Nieuwe bonussen!",
    E + "smid.gerepareerd": "Weer helemaal heel!",
    E + "smid.uitkomst.gelukt": "Gelukt! Een ster erbij!",
    E + "smid.uitkomst.mislukt": "Plof... niks gebeurd. Nog een keer?",
    E + "smid.uitkomst.ster_kwijt": "Oei, er plofte een sterretje af.",
    E + "smid.uitkomst.verpluisd": "Verpluisd! Repareer het bij de smid.",
    E + "smid.uitkomst.kan_niet": "Dat kan niet.",
    E + "smid.groot.gelukt": "GELUKT!",
    E + "smid.groot.mislukt": "Plof...",
    E + "smid.groot.ster_kwijt": "Sterretje eraf!",
    E + "smid.groot.verpluisd": "Verpluisd!",
    E + "smid.groot.herrold": "Herrold!",
    E + "smid.groot.gerepareerd": "Gerepareerd!",
    # market
    E + "markt.titel": "Het Veilinghuis van Marktmeester Pingel",
    E + "markt.tab.kopen": "Kopen",
    E + "markt.tab.verkopen": "Verkopen",
    E + "markt.tab.mijn": "Mijn spullen (%s)",
    E + "markt.haalop": "Opbrengst ophalen",
    E + "markt.zoek": "Zoeken...",
    E + "markt.prijs_veld": "Vraagprijs",
    E + "markt.f.slot.alle": "Alle slots",
    E + "markt.f.slot.wapen": "Wapens",
    E + "markt.f.slot.hoofd": "Hoofd",
    E + "markt.f.slot.borst": "Borst",
    E + "markt.f.slot.benen": "Benen",
    E + "markt.f.slot.voeten": "Voeten",
    E + "markt.f.slot.overig": "Overig",
    E + "markt.f.klasse": "Alle klassen",
    E + "markt.f.klasse.knuffelridder": "Ridder",
    E + "markt.f.klasse.pluisjager": "Jager",
    E + "markt.f.klasse.vadsmagier": "Magiër",
    E + "markt.f.zeldzaam": "Zeldzaamheid",
    E + "markt.f.level": "Alle levels",
    E + "markt.leeg": "Niks te koop dat hierbij past.",
    E + "markt.niks_eigen": "Je hebt niks te koop staan.",
    E + "markt.verkoper": "van %s",
    E + "markt.trekin": "Intrekken",
    E + "markt.kies_iets": "Kies iets om te verkopen",
    E + "markt.klik_iets": "Klik links op iets uit je tasje.",
    E + "markt.winkelwaarde": "Een winkelguh geeft:",
    E + "markt.vraagprijs": "Vraagprijs (bv. 2z 50b):",
    E + "markt.fee": "Marktgeld (niet terug):",
    E + "markt.zet": "Te koop zetten",
    E + "markt.niks": "Daar ligt niks.",
    E + "markt.gebonden": "Gebonden spullen kun je niet verkopen.",
    E + "markt.prijs": "Die prijs klopt niet.",
    E + "markt.te_veel": "Je hebt al 12 dingen te koop staan.",
    E + "markt.geen_fee": "Te weinig Vadsmunten voor het marktgeld.",
    E + "markt.weg": "Dat is er niet meer, njeg.",
    E + "markt.eigen": "Dat is van jezelf!",
    E + "markt.tas_vol": "Je tasje is vol!",
    E + "markt.kapot": "Dat ding kan nu niet uit de kraam (het blijft veilig staan).",
    E + "markt.te_weinig": "Te weinig Vadsmunten.",
    E + "markt.gezet": "Te koop gezet! (marktgeld %s)",
    E + "markt.gekocht": "Gekocht! Vahoeg!",
    E + "markt.ingetrokken": "Teruggepakt.",
    E + "markt.opgehaald": "Opgehaald: %s",
    E + "markt.niks_op": "Er wacht nog niks op je.",
    E + "markt.verkocht_melding": "Je %s is verkocht voor %s! Haal je munten op bij de marktmeester.",
    E + "markt.wacht": "Er wacht %s op je bij de marktmeester in Zwamzoet!",
    # trade
    E + "handel.titel": "Ruilen met %s",
    E + "handel.jij_geeft": "Jij geeft",
    E + "handel.hij_geeft": "%s geeft",
    E + "handel.munten": "Munten:",
    E + "handel.munten_veld": "Munten",
    E + "handel.zet": "Zet",
    E + "handel.tas": "Jouw tasje (klik = erbij)",
    E + "handel.klik_terug": "klik = terug",
    E + "handel.status.bezig": "Kiest nog...",
    E + "handel.status.klaar": "Klaar!",
    E + "handel.status.bevestigd": "Wil ruilen!",
    E + "handel.klaar": "Klaar",
    E + "handel.niet_klaar": "Toch niet klaar",
    E + "handel.ruilen": "Ruilen!",
    E + "handel.stop": "Stoppen",
    E + "handel.gebonden": "Gebonden: kan niet geruild worden",
    E + "handel.alleen_rijk": "Ruilen kan alleen in het Vadsrijk.",
    E + "handel.bezig": "Iemand is al aan het ruilen.",
    E + "handel.gevraagd": "Je vroeg %s om te ruilen.",
    E + "handel.vraag": "%s wil met je ruilen!",
    E + "handel.ja": "[Ja, ruilen!]",
    E + "handel.geen_verzoek": "Niemand wil nu met je ruilen.",
    E + "handel.te_ver": "Jullie staan te ver uit elkaar.",
    E + "handel.eerst_klaar": "Eerst moeten jullie allebei op Klaar drukken.",
    E + "handel.veranderd": "Er veranderde iets in een tasje: kijk nog eens en bevestig opnieuw.",
    E + "handel.te_weinig": "Iemand heeft niet genoeg munten meer.",
    E + "handel.tas_vol": "Iemands tasje is te vol voor de ruil.",
    E + "handel.gelukt": "Geruild! Vahoeg, njeg!",
    E + "handel.geannuleerd": "Het ruilen is gestopt.",
    E + "handel.weg": "De ander is weg: het ruilen is gestopt.",
    # mounts + stal
    E + "rijdier.nieuw": "Nieuw rijdier: %s! Blaas op je rijfluitje.",
    E + "rijdier.al": "Dat rijdier heb je al.",
    E + "rijdier.level": "%s is nu level %s!",
    E + "rijdier.alleen_rijk": "Je rijdier huppelt alleen in het Vadsrijk.",
    E + "rijdier.leer": 'Je %s wacht op je! Rijden leer je in zone 3 bij de Pluimstal.',   # 1.10.2
    E + "rijdier.leer_stal": 'Je rijdieren wachten op je! Rijden leer je in zone 3 bij de Pluimstal.',
    E + "rijdier.geen": "Nog geen rijdier: dat krijg je in De Schemerwortel.",
    E + "rijdier.geschrokken": "Je rijdier schrok en huppelde weg!",
    E + "zadel.uitleg": "Rechtsklik: %s komt in je stal.",
    E + "stal.titel": "De Stal van Stalmeester Hupsakee",
    E + "stal.level": "Level %s / %s",
    E + "stal.xp": "%s / %s (rijden geeft xp)",
    E + "stal.max": "Maximaal level!",
    E + "stal.snelheid": "Snelheid: %s%%",
    E + "stal.sprong": "Sprong: ~%s blokken",
    E + "stal.kies": "Deze roepen",
    E + "stal.gekozen_label": "Je fluitje roept deze",
    E + "stal.gekozen": "Je fluitje roept nu dit rijdier.",
    E + "stal.fluitje_ja": "Je hebt een rijfluitje.",
    E + "stal.fluitje_nee": "Je hebt geen rijfluitje!",
    E + "stal.koop_fluitje": "Nieuw fluitje (%sb)",
    E + "stal.heb_al": "Je hebt al een fluitje.",
    E + "stal.fluitje": "Hier is een nieuw rijfluitje!",
    # post + titles
    E + "post.aangekomen": "Er kwam een pakketje uit het Vadsrijk! (%s dingen in je tasje)",
    E + "post.vol": "Je tasje is vol: %s pakketje(s) uit het Vadsrijk wachten nog.",
    E + "titel.nieuw": "Nieuwe titel: %s!",
    "gui.guhs.titels.naam.vr_ridder_van_zwamzoet": "Ridder van Zwamzoet",
    "gui.guhs.titels.hint.vr_ridder_van_zwamzoet": "Te koop bij de Thuisbezorger in het Vadsrijk.",
    "gui.guhs.titels.naam.vr_vadsmuntenbaron": "Vadsmuntenbaron",
    "gui.guhs.titels.hint.vr_vadsmuntenbaron": "Heel duur, bij de Thuisbezorger in het Vadsrijk.",
    "gui.guhs.titels.naam.vr_sterrensmid": "Sterrensmid",
    "gui.guhs.titels.hint.vr_sterrensmid": "Tik tien sterren op één stuk uitrusting.",
    "gui.guhs.titels.naam.vr_rijdiermeester": "Rijdiermeester",
    "gui.guhs.titels.hint.vr_rijdiermeester": "Rijd een rijdier naar het hoogste level.",
    "gui.guhs.titels.naam.vr_marktkoopman": "Marktkoopman",
    "gui.guhs.titels.hint.vr_marktkoopman": "Verkoop tien dingen op de markt van Zwamzoet.",
    # tips
    E + "tip.rijdier.titel": "Je eerste rijdier!",
    E + "tip.rijdier": "Blaas op je rijfluitje en je Wolkenschaapje komt eraan. Rijden geeft het xp: dan wordt het sneller en springt het hoger.",
    E + "tip.rijden.titel": "Rijden",
    E + "tip.rijden": "Spatie = een grote hup. Word je geraakt, dan schrikt je rijdier en huppelt hij weg.",
}
FLAVOUR = {
}
for _t, _lijnen in FLAVOUR.items():
    for _i, _l in enumerate(_lijnen):
        TEKSTEN[f"vadsrijk.loot.{_t}.flavour.{_i}"] = _l


# ======================================================================================================================
def _items():
    for naam, f in ICONEN.items():
        f().png(os.path.join(A, "textures", "item", ITEM_TEX, f"{naam}.png"))
        parent = "minecraft:item/handheld" if naam in HANDHELD else "minecraft:item/generated"
        _schrijf(os.path.join(A, "models", "item", f"{naam}.json"), {"parent": parent, "textures": {"layer0": f"guhs:item/{ITEM_TEX}/{naam}"}})
        _schrijf(os.path.join(A, "items", f"{naam}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}})


def _assets():
    _items()
    _blokken()


def build(h):
    _assets()
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)


def _direct():
    _assets()
    _kleding_texturen()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(TEKSTEN)
        _schrijf(p, data)
    print(f"vadsrijk_economie: {len(TEKSTEN)} texts, {len(ICONEN)} item icons, {len(BLOKKEN)} blocks, {len(CLOTHES)} clothes")


if __name__ == "__main__":
    _direct()
