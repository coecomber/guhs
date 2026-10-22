"""
Op Safari! (1.6.0), slice S6 - the giftshop's posters and lieve schilderijen of the safari animals (painting variants).

Renders the real animal models (assets/guhs/geckolib/models/entity/safari) as chibi portraits on soft backgrounds into
assets/guhs/textures/painting/safari_<naam>.png (32 px per block). The painting_variant JSONs, the placeable tag and the
names are written by tools/features/safari_blokken.py (make_resources), from SCHILDERIJEN below.

Not part of make_resources.py (slow, and it needs the atelier renderer); the PNGs are committed:
    python tools/features/safari_schilderijen.py        (from the project root)
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

PPB = 32                                      # pixels per painting block
TEX_OUT = os.path.join("src", "main", "resources", "assets", "guhs", "textures", "painting")
GEO = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity", "safari")
DIER_TEX = os.path.join("src", "main", "resources", "assets", "guhs", "textures", "entity", "safari")

# naam: (breedte, hoogte, NL-titel, EN-titel, achtergrond, [(soort, variant, dx, dz)], yaw)
# The format is in the title, as the design asks ("Rode panda (1×1)").
SCHILDERIJEN = {
    "rode_panda":        (1, 1, "Rode panda", "Red panda", "bamboe", [("rode_panda", "", 0, 0)], 30),
    "pinguin":           (1, 1, "Pinguïnkuiken", "Penguin chick", "ijs", [("pinguin", "baby", 0, 0)], 25),
    "koala":             (1, 1, "Slaperige koala", "Sleepy koala", "australie", [("koala", "", 0, 0)], 30),
    "stokstaartje":      (1, 1, "Stokstaartje op wacht", "Meerkat on guard", "savanne", [("stokstaartje", "", 0, 0)], 20),
    "giraffe":           (1, 2, "Giraffe", "Giraffe", "savanne", [("giraffe", "", 0, 0)], 35),
    "flamingo":          (1, 2, "Flamingo", "Flamingo", "delta", [("flamingo", "", 0, 0)], 30),
    "pinguinfamilie":    (2, 1, "Pinguïnfamilie", "Penguin family", "ijs",
                          [("pinguin", "", -0.7, 0), ("pinguin", "baby", 0.15, 0.3), ("pinguin", "", 0.95, 0)], 20),
    "otters":            (2, 1, "Spelende otters", "Playful otters", "europa", [("otter", "", -0.6, 0), ("otter", "baby", 0.6, 0.2)], 35),
    "leeuwenfamilie":    (2, 2, "Leeuwenfamilie", "Lion family", "savanne",
                          [("leeuw", "man", -0.7, 0), ("leeuw", "", 0.5, 0.1), ("leeuw", "baby", 0.0, 0.8)], 25),
    "reuzenpanda":       (2, 2, "Reuzenpanda met bamboe", "Giant panda with bamboo", "bamboe", [("reuzenpanda", "", 0, 0)], 30),
    "savanne":           (3, 2, "Savanne bij zonsondergang", "Savanna at sunset", "zonsondergang",
                          [("olifant", "", -1.4, 0), ("giraffe", "", 0.2, -0.6), ("zebra", "", 1.4, 0.2), ("zebra", "baby", 2.0, 0.6)], 25),
    "regenwoud":         (3, 2, "Regenwoudvriendjes", "Rainforest friends", "regenwoud",
                          [("orang_oetan", "", -1.2, 0), ("luiaard", "", 0.1, -0.3), ("toekan", "", 1.1, 0.2)], 25),
    "olifant":           (3, 3, "Olifant", "Elephant", "savanne", [("olifant", "man", 0, 0), ("olifant", "baby", 1.3, 0.8)], 30),
    "grote_trek":        (4, 2, "De grote trek", "The great migration", "zonsondergang",
                          [("kafferbuffel", "", -2.4, 0), ("zebra", "", -0.9, 0.3), ("zebra", "", 0.5, -0.2), ("kafferbuffel", "man", 1.9, 0.2),
                           ("zebra", "baby", 3.0, 0.6)], 20),
    # G1: the painting of the ninth expert's badge (Het Kriebelhuis)
    "kriebelhuis":       (2, 1, "Kriebelvriendjes", "Creepy-crawly friends", "kriebel",
                          [("kameleon", "", -0.8, 0), ("gifkikker", "", 0.1, 0.3), ("vogelspin", "", 0.9, 0)], 25),
}

ACHTERGROND = {           # top colour, bottom colour, ground colour
    "bamboe": ((206, 236, 198), (240, 248, 226), (150, 196, 110)),
    "ijs": ((196, 224, 246), (238, 246, 252), (236, 244, 250)),
    "australie": ((250, 214, 170), (252, 236, 210), (206, 120, 74)),
    "savanne": ((250, 228, 170), (252, 242, 214), (214, 186, 96)),
    "delta": ((214, 236, 230), (244, 248, 240), (126, 170, 120)),
    "europa": ((214, 236, 214), (242, 248, 238), (110, 164, 90)),
    "regenwoud": ((186, 222, 178), (226, 242, 214), (70, 130, 70)),
    "zonsondergang": ((246, 168, 132), (252, 220, 160), (196, 150, 80)),
    "kriebel": ((210, 236, 200), (246, 238, 214), (120, 150, 86)),
    "oertijd": ((222, 214, 196), (246, 238, 222), (150, 128, 96)),
    "zee": ((170, 214, 236), (220, 240, 248), (230, 214, 170)),
}
# G1: the background of a species' photo painting (safari_foto_<soort>), by its zone (the Kriebelhuis' water animals: zee)
ZONE_THEMA = {"savanne": "savanne", "delta": "delta", "regenwoud": "regenwoud", "bamboe": "bamboe", "ijskust": "ijs",
              "oertijd": "oertijd", "europa": "europa", "australie": "australie", "kriebelhuis": "kriebel", "entree": "europa"}


def achtergrond(w, h, thema):
    top, bot, grond = ACHTERGROND[thema]
    img = Image.new("RGBA", (w, h))
    px = img.load()
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(int(top[i] * (1 - t) + bot[i] * t) for i in range(3))
        for x in range(w):
            px[x, y] = (*c, 255)
    d = ImageDraw.Draw(img)
    if thema in ("savanne", "zonsondergang", "australie"):
        r = int(min(w, h) * 0.18)
        d.ellipse([w * 0.72 - r, h * 0.28 - r, w * 0.72 + r, h * 0.28 + r], fill=(255, 236, 170, 255) if thema != "zonsondergang" else (255, 200, 120, 255))
    gy = int(h * 0.78)
    d.rectangle([0, gy, w, h], fill=(*grond, 255))
    d.ellipse([-w * 0.2, gy - h * 0.06, w * 1.2, gy + h * 0.1], fill=(*grond, 255))
    # a few cute hearts / sparkles
    rng = np.random.default_rng(hash(thema) % 1000)
    for _ in range(max(2, w * h // 6000)):
        x, y = rng.uniform(0.05, 0.95) * w, rng.uniform(0.05, 0.5) * h
        s = max(2, int(min(w, h) * 0.025))
        d.ellipse([x - s, y - s, x + s, y + s], fill=(255, 255, 255, 150))
    return img


def lijst(img):
    """A soft wooden frame of 2 painting pixels."""
    d = ImageDraw.Draw(img)
    w, h = img.size
    for i, c in enumerate(((120, 82, 50), (168, 120, 74))):
        d.rectangle([i, i, w - 1 - i, h - 1 - i], outline=(*c, 255))
    return img


def teken(bw, bh, thema, dieren, yaw):
    """One painting (bw x bh blocks): the animals on the theme's background in a wooden frame."""
    sys.path.insert(0, os.path.join("tools"))
    import wiki_renders as wr
    sys.path.insert(0, os.path.join("tools", "features", "safari_atelier"))
    import blad
    if True:
        W, H = bw * PPB, bh * PPB
        quads = []
        for soort, variant, dx, dz in dieren:
            stam = soort + (f"_{variant}" if variant else "")
            geo = os.path.join(GEO, stam + ".geo.json")
            if not os.path.exists(geo):
                geo, stam = os.path.join(GEO, soort + ".geo.json"), soort
            tex = np.asarray(Image.open(os.path.join(DIER_TEX, stam + ".png")).convert("RGBA")).astype(np.float32)
            quads += wr.offset_quads(wr.geo_quads(geo, tex), dx=dx, dz=dz)
        # scale so the group fills ~80% of the painting
        sx, sy = blad.span(quads, yaw, -10)
        ppb = min(W * 4 * 0.8 / sx, H * 4 * 0.7 / sy)
        dieren_img = blad.render_vast(quads, yaw, -10, ppb, W * 4, H * 4, ss=2)
        bg = achtergrond(W * 4, H * 4, thema)
        schaduw = Image.new("RGBA", bg.size, (0, 0, 0, 0))
        a = np.asarray(dieren_img)[..., 3]
        sh = Image.fromarray(((a > 0) * 70).astype(np.uint8)).filter(ImageFilter.GaussianBlur(6))
        schaduw.putalpha(sh)
        bg.alpha_composite(schaduw, (6, 10))
        bg.alpha_composite(dieren_img, (0, int(H * 4 * 0.06)))
        return lijst(bg.resize((W, H), Image.LANCZOS))


def main(namen=None):
    os.makedirs(TEX_OUT, exist_ok=True)
    for naam, (bw, bh, *_rest) in SCHILDERIJEN.items():
        if namen and naam not in namen:
            continue
        _nl, _en, thema, dieren, yaw = _rest
        out = teken(bw, bh, thema, dieren, yaw)
        out.save(os.path.join(TEX_OUT, f"safari_{naam}.png"))
        print("wrote", naam, out.size)


def fotos(soorten):
    """G1 (design 1.7, a photo printed as a painting): a 1x1 portrait per species, safari_foto_<soort>.png.
    {@code soorten}: soort -> zone (soorten.json)."""
    os.makedirs(TEX_OUT, exist_ok=True)
    for soort, zone in soorten.items():
        if not os.path.exists(os.path.join(GEO, soort + ".geo.json")):
            continue
        thema = ZONE_THEMA.get(zone or "", "europa")
        out = teken(1, 1, thema, [(soort, "", 0, 0)], 30)
        out.save(os.path.join(TEX_OUT, f"safari_foto_{soort}.png"))
    print("safari_schilderijen: photo paintings for", len(soorten), "species")


if __name__ == "__main__":
    main(sys.argv[1:] or None)
