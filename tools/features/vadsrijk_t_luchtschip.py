"""
1.10 "Het Vadsrijk", night round T, slice T-LUCHTSCHIP (Java: feature/vadsrijk/luchtschip):

  - the big guh airship of the scheduled lines (GeckoLib vr_luchtschip: a wooden deck with railing and a stern cabin under a
    pink guh-faced balloon with ears and a tail fin, two propellers; animations vaar / ligt; glowmask for windows + lanterns);
  - the silhouettes ("verre schimmen", WERELDINDELING §5.1): billboard art for De Boom, the Splinterpieken, the Kaaszeppelinwerf
    and Moeder Wolkwal with Hoogvads (side + front) in textures/vadsrijk/luchtschip/schim_*.png;
  - every text of the slice (Dutch, en_us real English): harbour names, departure boards, on-board messages, gates, luxury ticket.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_luchtschip.py
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

try:
    from features.vadsrijk_bazen import Geo, vlak, disc, lijn, kf
except ImportError:  # standalone
    from vadsrijk_bazen import Geo, vlak, disc, lijn, kf

L = "vadsrijk.luchtschip."

HAVENS = {
    "haven_kamp_z1_vertrek": ("Kampsteiger", "Camp Jetty"),
    "haven_z1_kamp_vertrek": ("Haven Zwamzoet", "Zwamzoet Harbour"),
    "haven_z1_z2_vertrek": ("Noordklifhaven", "North Cliff Harbour"),
    "haven_z2_z1_vertrek": ("Kandijkom-dok", "Kandijkom Dock"),
    "haven_z2_z3_vertrek": ("Galmdok", "Echo Dock"),
    "haven_z3_z2_vertrek": ("Weststeiger", "West Jetty"),
    "haven_z3_z4_vertrek": ("Oosttaksteiger", "East Branch Jetty"),
    "haven_z4_z3_vertrek": ("Lampionhaven", "Lantern Harbour"),
    "haven_z4_z5_vertrek": ("De Opstijgpier", "The Ascent Pier"),
    "haven_z5_z4_vertrek": ("Walvisrughaven", "Whaleback Harbour"),
}
LIJNEN = {
    "kamp_z1": ("Lijn 0: Knuffelkamp - Zwamzoet", "Line 0: Knuffelkamp - Zwamzoet"),
    "z1_z2": ("Lijn A: Zwamzoet - Splinterpieken", "Line A: Zwamzoet - Splinter Peaks"),
    "z2_z3": ("Lijn B: Splinterpieken - Schemerwortel", "Line B: Splinter Peaks - Schemerwortel"),
    "z3_z4": ("Lijn C: Schemerwortel - Lampionkust", "Line C: Schemerwortel - Lantern Coast"),
    "z4_z5": ("Lijn D: Lampionkust - Hoogvads", "Line D: Lantern Coast - Hoogvads"),
}

TEKSTEN = {
    "entity.guhs.vr_luchtschip": ("Luchtschip", "Airship"),
    "entity.guhs.vr_luchtschip_bord": ("Vertrekbord", "Departure board"),
    L + "bord.lijn": ("Lijn %s → %s", "Line %s → %s"),
    L + "bord.over": ("Vertrekt over %s", "Departs in %s"),
    L + "bord.nu": ("Vertrekt nu! Snel aan boord!", "Departing now! Hop on!"),
    L + "bord.instappen": ("Klik op het schip om in te stappen (vlucht %s)", "Click the ship to board (flight %s)"),
    L + "bord.onderweg": ("Het schip is onderweg hierheen (vlucht %s)", "The ship is on its way here (flight %s)"),
    L + "pier": ("Lijn %s naar %s: vertrek over %s. Klik op het schip om in te stappen.",
                 "Line %s to %s: departs in %s. Click the ship to board."),
    L + "aan_boord.vlucht": ("Onderweg naar %s - landing over %s", "On the way to %s - landing in %s"),
    L + "aan_boord.wacht": ("Aan boord naar %s - vertrek over %s (sneak om uit te stappen)",
                            "On board to %s - departs in %s (sneak to get off)"),
    L + "al_aan_boord": ("Je bent al aan boord! Vertrek over %s.", "You are on board already! Departs in %s."),
    L + "al_aan_boord.vlucht": ("Je bent al aan boord! Landing over %s.", "You are on board already! Landing in %s."),
    L + "vliegt_al": ("Dit schip is al vertrokken. Wacht op het volgende (kijk op het vertrekbord).",
                      "This ship has left already. Wait for the next one (see the departure board)."),
    L + "vol": ("Het schip is vol! Neem het volgende.", "The ship is full! Take the next one."),
    L + "welkom": ("Welkom aan boord! Naar %s, vertrek over %s. Zolang we aan de pier liggen kun je nog uitstappen (sneak).",
                   "Welcome aboard! To %s, departing in %s. While we are at the pier you can still get off (sneak)."),
    L + "vertrek": ("Daar gaan we! Op naar %s (vlucht %s). Uitstappen kan pas na de landing.",
                    "Off we go! To %s (flight %s). You can get off after the landing."),
    L + "aangekomen": ("Geland met het luchtschip", "Landed by airship"),
    L + "blijf": ("Njeg! Blijf lekker aan boord tot we geland zijn.", "Njeg! Stay on board until we have landed."),
    L + "alsnog_geland": ("Je vlucht is netjes afgemaakt: je staat nu in %s.", "Your flight was finished for you: you are now at %s."),
    L + "slot.kamp": ("De kapitein schudt zijn hoofd: help eerst nog even in het kamp!", "The captain shakes his head: help out in the camp first!"),
    L + "slot.z4": ("Deze lijn vliegt pas als De Schemerwortel weer veilig is. Maak eerst het verhaal van de Schemerwortel af.",
                    "This line only flies once De Schemerwortel is safe again. Finish the Schemerwortel story first."),
    L + "slot.z5": ("Hoogvads hangt nog te hoog. Pas na het Knuffeltij-feest zakt Moeder Wolkwal laag genoeg.",
                    "Hoogvads still floats too high. Only after the Knuffeltij party does Mother Cloudwhale come down low enough."),
    L + "luxe.gratis": ("Luxe vlucht! Deze eerste keer trakteert de kapitein. (Daarna kost een luxe vlucht geld; het grote luchtschip in de haven is gratis.)",
                        "Luxury flight! The captain treats you this first time. (After that a luxury flight costs money; the big airship in the harbour is free.)"),
    L + "luxe.te_duur": ("Een luxe vlucht kost %s brons, en zoveel heb je niet bij je. Het grote luchtschip vliegt gratis: kijk op het vertrekbord in de haven!",
                         "A luxury flight costs %s bronze, and you do not have that much. The big airship flies for free: see the departure board in the harbour!"),
    L + "luxe.betaald": ("Luxe vlucht: %s brons betaald. Instappen maar!", "Luxury flight: %s bronze paid. All aboard!"),
}
for k, (nl, en) in HAVENS.items():
    TEKSTEN[L + "haven." + k] = (nl, en)
for k, (nl, en) in LIJNEN.items():
    TEKSTEN[L + "lijn." + k] = (nl, en)

TEX = "textures/vadsrijk/luchtschip"

# ======================================================================================================== the airship model
HOUT = (164, 112, 74)
HOUT_DONKER = (112, 74, 50)
ROZE = (255, 150, 200)
ROZE_DONKER = (220, 104, 162)
ROOM = (255, 238, 214)
GOUD = (250, 206, 72)
LAMP = (255, 214, 120)


def _planken(a, W, H, f, r):
    for y in range(0, H, 6):
        a[y, :, :3] *= 0.8
    for _ in range(max(1, W * H // 300)):
        disc(a, r.uniform(0, W), r.uniform(0, H), 0.7, HOUT_DONKER, alpha=0.5)


def _strepen(a, W, H, f, r):
    """Cream stripes along the balloon."""
    if f in ("east", "west"):
        for y in range(int(H * 0.2), H, max(6, H // 5)):
            a[y:y + max(2, H // 16), :, :3] = ROOM
    elif f in ("up", "down"):
        for x in range(int(W * 0.2), W, max(6, W // 5)):
            a[:, x:x + max(2, W // 16), :3] = ROOM


def _gezicht(a, W, H, f, r):
    """The guh face on the balloon's nose: big eyes with sparkles, blush, a little w-mouth."""
    for cx in (W * 0.3, W * 0.7):
        disc(a, cx, H * 0.42, W * 0.12, (30, 18, 34), ry=H * 0.16)
        disc(a, cx - W * 0.04, H * 0.36, W * 0.04, (255, 255, 255))
    for cx in (W * 0.16, W * 0.84):
        disc(a, cx, H * 0.62, W * 0.08, (255, 110, 160), ry=H * 0.06, alpha=0.8)
    lijn(a, W * 0.42, H * 0.66, W * 0.47, H * 0.72, (60, 30, 50), 1.6)
    lijn(a, W * 0.47, H * 0.72, W * 0.5, H * 0.67, (60, 30, 50), 1.6)
    lijn(a, W * 0.5, H * 0.67, W * 0.53, H * 0.72, (60, 30, 50), 1.6)
    lijn(a, W * 0.53, H * 0.72, W * 0.58, H * 0.66, (60, 30, 50), 1.6)


def _ramen(glow):
    def extra(a, W, H, f, r):
        if f in ("east", "west", "south"):
            for x in range(max(3, W // 8), W - 3, max(8, W // 4)):
                y0, y1 = int(H * 0.3), int(H * 0.62)
                a[y0:y1, x:x + max(3, W // 10), :3] = LAMP
                a[y0:y1, x:x + max(3, W // 10), 3] = 255
    return extra


class Geo2(Geo):
    """Geo with shelf packing (faces of very different heights share the atlas without wasting whole rows)."""

    def __init__(self, *a, **k):
        super().__init__(*a, **k)
        self.planken = []   # [y, height, x_free]

    def _alloc(self, w, h):
        w, h = int(math.ceil(w)), int(math.ceil(h))
        for pl in self.planken:
            if pl[1] >= h and pl[1] <= max(h * 2, h + 4) and pl[2] + w <= self.tw:
                u = pl[2]
                pl[2] += w
                return u, pl[0]
        y = sum(pl[1] for pl in self.planken)
        if y + h > self.th or w > self.tw:
            raise SystemExit(f"vadsrijk_t_luchtschip: atlas of {self.naam} is full")
        self.planken.append([y, h, w])
        return 0, y


def _leeg(W, H, f):
    return np.zeros((H, W, 4), np.uint8)


def airship(glow=False):
    g = Geo2("vr_luchtschip", tw=320, th=320, s=2)

    def p(kleur, *a, **k):
        return _leeg if glow else vlak(kleur, *a, **k)

    hout = p(HOUT, 8, 1, extra=_planken)
    donker = p(HOUT_DONKER, 6, 2)
    roze = p(ROZE, 8, 3, extra=_strepen, verloop=ROZE_DONKER)
    room = p(ROOM, 6, 4)
    goud = p(GOUD, 6, 5)
    lamp = vlak(LAMP, 4, 6)
    g.bone("root")
    # --- the hull: deck top at y 0, bow at -z
    g.bone("romp", "root")
    g.cube("romp", (-14, -4, -36), (28, 4, 72), hout)
    g.cube("romp", (-12, -9, -32), (24, 5, 66), hout)
    g.cube("romp", (-8, -13, -26), (16, 4, 56), donker)
    g.cube("romp", (-9, -9, -44), (18, 9, 8), hout)
    g.cube("romp", (-4, -6, -50), (8, 6, 6), goud)
    # railing
    g.cube("romp", (-14, 0, -36), (1, 4, 54), donker)
    g.cube("romp", (13, 0, -36), (1, 4, 54), donker)
    g.cube("romp", (-14, 0, -37), (28, 4, 1), donker)
    # the stern cabin with windows (glow), roof
    ramen = vlak(HOUT, 8, 7, extra=_ramen(False)) if not glow else (lambda W, H, f: _ramen_glow(W, H, f))
    g.cube("romp", (-11, 0, 18), (22, 14, 16), ramen)
    g.cube("romp", (-12, 14, 17), (24, 2, 18), p(ROZE_DONKER, 6, 8))
    # lanterns on the bow and the cabin corners
    for (x, y, z) in ((-13, 4, -36), (12, 4, -36), (-12, 14, 33), (11, 14, 33)):
        g.cube("romp", (x, y, z), (1.5, 2, 1.5), lamp)
    # the four ropes / posts up to the balloon
    for (x, z) in ((-12, -24), (11, -24), (-12, 14), (11, 14)):
        g.cube("romp", (x, 0, z), (1, 36, 1), room)
    # --- the balloon
    g.bone("ballon", "root", (0, 52, 0))
    g.cube("ballon", (-18, 36, -40), (36, 32, 80), roze)
    g.cube("ballon", (-14, 40, -46), (28, 24, 6), roze)
    g.cube("ballon", (-14, 40, 40), (28, 24, 6), roze)
    g.cube("ballon", (-14, 68, -36), (28, 2, 72), roze)
    g.cube("ballon", (-14, 34, -36), (28, 2, 72), roze)
    gezicht = p(ROZE, 6, 9, extra=_gezicht)
    g.cube("ballon", (-9, 45, -50), (18, 14, 4), roze, faces={"north": gezicht})
    # ears
    oor = p(ROZE_DONKER, 6, 10, extra=lambda a, W, H, f, r: disc(a, W / 2, H * 0.55, W * 0.3, (255, 200, 225), ry=H * 0.3) if f == "north" else None)
    g.bone("oor_l", "ballon", (10, 70, -26))
    g.cube("oor_l", (6, 70, -30), (8, 10, 6), oor)
    g.bone("oor_r", "ballon", (-10, 70, -26))
    g.cube("oor_r", (-14, 70, -30), (8, 10, 6), oor)
    # tail fins
    g.cube("ballon", (-1, 44, 44), (2, 20, 14), room)
    g.cube("ballon", (-14, 53, 44), (28, 2, 12), room)
    # a little pink flag on top
    g.bone("vlag", "ballon", (0, 70, 20))
    g.cube("vlag", (-0.5, 70, 20), (1, 12, 1), donker)
    g.cube("vlag", (0.5, 76, 20), (0.5, 5, 8), p((255, 90, 160), 6, 11))
    # propellers at the stern
    for naam, x in (("prop_l", 17), ("prop_r", -17)):
        g.bone(naam, "root", (x, 6, 36))
        g.cube(naam, (x - 1.5, 4.5, 35), (3, 3, 3), goud)
        g.cube(naam, (x - 0.75, -2, 36), (1.5, 16, 0.8), room)
        g.cube(naam, (x - 8, 5.25, 36), (16, 1.5, 0.8), room)
        g.cube("romp", (x - 1 if x > 0 else x - 1, 2, 30), (2, 2, 6), donker)
    anims = {"animations": {
        "vaar": kf({"prop_l": {"rotation": {0: [0, 0, 0], 0.25: [0, 0, 180], 0.5: [0, 0, 360]}},
                    "prop_r": {"rotation": {0: [0, 0, 0], 0.25: [0, 0, -180], 0.5: [0, 0, -360]}},
                    "ballon": {"position": {0: [0, 0, 0], 2: [0, 1.2, 0], 4: [0, 0, 0]}},
                    "vlag": {"rotation": {0: [0, -8, 0], 0.5: [0, 8, 0], 1: [0, -8, 0]}},
                    "oor_l": {"rotation": {0: [0, 0, 0], 2: [0, 0, -6], 4: [0, 0, 0]}},
                    "oor_r": {"rotation": {0: [0, 0, 0], 2: [0, 0, 6], 4: [0, 0, 0]}}}, lengte=4.0),
        "ligt": kf({"prop_l": {"rotation": {0: [0, 0, 0], 1: [0, 0, 180], 2: [0, 0, 360]}},
                    "prop_r": {"rotation": {0: [0, 0, 0], 1: [0, 0, -180], 2: [0, 0, -360]}},
                    "ballon": {"position": {0: [0, 0, 0], 3: [0, 0.6, 0], 6: [0, 0, 0]}},
                    "vlag": {"rotation": {0: [0, -3, 0], 1.5: [0, 3, 0], 3: [0, -3, 0]}}}, lengte=6.0),
    }}
    return g, anims


def _ramen_glow(W, H, f):
    a = np.zeros((H, W, 4), np.float32)
    _ramen(True)(a, W, H, f, None)
    return np.clip(a, 0, 255).astype(np.uint8)


# ======================================================================================================== the silhouettes
NEVEL = (206, 196, 226)


def _waas(im, sterkte=0.35):
    """Haze: pull the colours towards the far-away lilac, soften the edge."""
    a = np.asarray(im).astype(np.float32)
    a[..., :3] = a[..., :3] * (1 - sterkte) + np.array(NEVEL) * sterkte
    out = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))
    return out.filter(ImageFilter.GaussianBlur(0.7))


def schim_boom():
    W, H = 256, 160
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    rng = random.Random(1404)
    # the root island (a hanging cone) with roots dangling into the cloud gap
    d.polygon([(40, 128), (W // 2, H - 2), (W - 40, 128)], fill=(150, 120, 140, 255))
    d.ellipse((30, 116, W - 30, 140), fill=(176, 150, 150, 255))
    for _ in range(14):
        x = rng.randint(50, W - 50)
        d.line([(x, 132), (x + rng.randint(-8, 8), 132 + rng.randint(10, 26))], fill=(130, 100, 110, 255), width=2)
    # the trunk, flaring at the foot
    d.polygon([(W // 2 - 16, 124), (W // 2 - 9, 60), (W // 2 + 9, 60), (W // 2 + 16, 124)], fill=(136, 98, 96, 255))
    d.polygon([(W // 2 - 30, 126), (W // 2 - 12, 110), (W // 2 + 12, 110), (W // 2 + 30, 126)], fill=(136, 98, 96, 255))
    # the crown: overlapping blossom clouds, golden-pink
    for _ in range(46):
        r = rng.randint(12, 26)
        cx = W // 2 + max(-(W // 2 - r - 2), min(W // 2 - r - 2, rng.gauss(0, 46)))
        cy = max(r * 0.8 + 2, 46 + rng.gauss(0, 15))
        kl = rng.choice([(250, 196, 160), (246, 170, 190), (252, 214, 150), (236, 160, 186)])
        d.ellipse((cx - r, cy - r * 0.8, cx + r, cy + r * 0.8), fill=kl + (255,))
    # Het Nest on top
    d.ellipse((W // 2 - 7, 10, W // 2 + 7, 20), fill=(200, 150, 110, 255))
    return _waas(im)


def schim_pieken():
    W, H = 384, 128
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    rng = random.Random(704)
    # cloud sea at the foot
    for x in range(0, W, 14):
        d.ellipse((x - 14, H - 22 + rng.randint(-3, 3), x + 18, H), fill=(246, 238, 248, 255))
    spitsen = sorted(((rng.uniform(10, W - 10), rng.uniform(0.25, 1.0)) for _ in range(30)), key=lambda s: s[1])
    for (x, h) in spitsen:
        top = H - 18 - h * (H - 26)
        b = 6 + h * 16
        kl = (int(200 + 40 * h), int(150 + 30 * h), int(214 + 20 * h), 255)
        d.polygon([(x - b, H - 14), (x, top), (x + b, H - 14)], fill=kl)
        d.line([(x, top), (x + b * 0.4, H - 30)], fill=(255, 236, 250, 255), width=1)
    # Kandijkom: towers in the middle
    for i, (x, h) in enumerate(((W / 2 - 22, 46), (W / 2, 62), (W / 2 + 20, 40))):
        d.rectangle((x - 5, H - 14 - h, x + 5, H - 14), fill=(236, 214, 236, 255))
        d.polygon([(x - 7, H - 14 - h), (x, H - 26 - h), (x + 7, H - 14 - h)], fill=(240, 140, 190, 255))
    return _waas(im, 0.3)


def schim_werf():
    W, H = 128, 80
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.polygon([(20, 58), (64, H - 2), (108, 58)], fill=(150, 124, 140, 255))
    d.ellipse((14, 50, 114, 64), fill=(180, 160, 150, 255))
    for x in range(26, 104, 12):
        d.line([(x, 54), (x, 22)], fill=(130, 100, 90, 255), width=2)
    d.line([(26, 22), (102, 22)], fill=(130, 100, 90, 255), width=2)
    d.line([(26, 38), (102, 38)], fill=(130, 100, 90, 255), width=1)
    d.ellipse((34, 6, 104, 30), fill=(250, 210, 100, 255))
    for x in (50, 70, 88):
        d.ellipse((x - 3, 14, x + 3, 20), fill=(228, 180, 70, 255))
    return _waas(im, 0.3)


def _stad(d, x0, x1, ybasis, rng, schaal=1.0, kathedraal=None):
    x = x0
    while x < x1:
        w = rng.randint(4, 8) * schaal
        h = rng.randint(8, 22) * schaal
        if kathedraal is not None and abs(x - kathedraal) < 14 * schaal:
            h = 40 * schaal
            w = 12 * schaal
        d.rectangle((x, ybasis - h, x + w, ybasis), fill=(240, 228, 240, 255))
        d.polygon([(x - 1, ybasis - h), (x + w / 2, ybasis - h - 9 * schaal), (x + w + 1, ybasis - h)], fill=(244, 140, 186, 255))
        for j in range(int(ybasis - h + 4), int(ybasis - 2), 6):
            d.rectangle((x + w / 2 - 1, j, x + w / 2 + 1, j + 2), fill=(250, 210, 140, 255))
        x += w + rng.randint(1, 4)


def schim_hoogvads_zij():
    W, H = 512, 128
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    rng = random.Random(2790)
    # clouds under the belly
    for x in range(30, W - 40, 16):
        d.ellipse((x - 16, 104 + rng.randint(-4, 4), x + 22, 126), fill=(250, 246, 252, 255))
    # the whale: head on the left (west), tail flukes on the right
    d.ellipse((6, 52, W - 60, 120), fill=(196, 206, 236, 255))
    d.polygon([(W - 90, 70), (W - 6, 40), (W - 30, 86), (W - 6, 116), (W - 90, 100)], fill=(190, 200, 232, 255))
    d.ellipse((40, 84, 54, 96), fill=(60, 50, 80, 255))      # the eye
    d.ellipse((44, 86, 48, 90), fill=(255, 255, 255, 255))
    d.ellipse((30, 98, 54, 106), fill=(255, 150, 190, 200))  # blush
    d.arc((14, 90, 120, 118), 20, 80, fill=(150, 160, 200, 255), width=2)
    # Hoogvads on her back, the cathedral at 43%
    _stad(d, 70, W - 110, 62, rng, 1.0, kathedraal=0.43 * W)
    return _waas(im, 0.28)


def schim_hoogvads_voor():
    W, H = 192, 128
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    rng = random.Random(2440)
    for x in range(10, W - 10, 14):
        d.ellipse((x - 14, 104 + rng.randint(-4, 4), x + 18, 126), fill=(250, 246, 252, 255))
    d.ellipse((18, 50, W - 18, 122), fill=(196, 206, 236, 255))
    for cx in (62, W - 62):
        d.ellipse((cx - 7, 82, cx + 7, 94), fill=(60, 50, 80, 255))
        d.ellipse((cx - 3, 84, cx + 1, 88), fill=(255, 255, 255, 255))
    _stad(d, 34, W - 40, 60, rng, 1.0, kathedraal=W / 2)
    return _waas(im, 0.28)


# ======================================================================================================== writing


def alles(w, a_dir, png):
    g, an = airship()
    w(os.path.join(a_dir, "geckolib", "models", "entity", "vr_luchtschip.geo.json"), g.json((22, 24, 4)))
    w(os.path.join(a_dir, "geckolib", "animations", "entity", "vr_luchtschip.animation.json"), {"format_version": "1.8.0", **an})
    png(os.path.join(a_dir, "textures", "entity", "vr_luchtschip.png"), g.png())
    png(os.path.join(a_dir, "textures", "entity", "vr_luchtschip_glowmask.png"), airship(glow=True)[0].png())
    t = os.path.join(a_dir, *TEX.split("/"))
    png(os.path.join(t, "schim_boom.png"), schim_boom())
    png(os.path.join(t, "schim_pieken.png"), schim_pieken())
    png(os.path.join(t, "schim_werf.png"), schim_werf())
    png(os.path.join(t, "schim_hoogvads_zij.png"), schim_hoogvads_zij())
    png(os.path.join(t, "schim_hoogvads_voor.png"), schim_hoogvads_voor())


def build(h):
    alles(h.w, h.A, _png)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)   # (T-INT: make_v2.lang(key, en, nl))


def _png(pad, im):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    im.save(pad)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    alles(_schrijf, a, _png)
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf(p, d)
    print(f"vadsrijk_t_luchtschip: airship model + 5 silhouettes, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
