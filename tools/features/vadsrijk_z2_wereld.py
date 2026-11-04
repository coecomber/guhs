"""
1.10 "Het Vadsrijk" zone 2 "De Splinterpieken", the z2-wereld slice (Z2_BRIEF.md). Java: feature/vadsrijk/zone2/wereld.

  - the zone-2 sky textures (pale ice band, mother-of-pearl clouds, aurora curtain, the big moon, the crystal-spire silhouette);
  - every z2-wereld text (Dutch; en_us gets the same Dutch): area names (kern.Gebieden keys), the Spiegelburcht door, the airship
    line Haven <-> Kandijkom and its opening, the gondola, the viewpoints.

The map itself: tools/vadsrijk/maak_kaart.py --zone 2 (tools/vadsrijk/zone2: tiles, Z2WereldPlekken.java, POI_Z2.md).
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_z2_wereld.py
"""
import json
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

A = os.path.join("src", "main", "resources")
AS = os.path.join(A, "assets", "guhs")
W = "vadsrijk.wereld."
Z = W + "z2."

TEKSTEN = {
    # areas (kern.Gebieden: vadsrijk.kern.gebied.<id>)
    "vadsrijk.kern.gebied.z2_kandijkom": "Kandijkom",
    "vadsrijk.kern.gebied.z2_kandijkom_dok": "Kandijkom - Luchtschipsteiger",
    "vadsrijk.kern.gebied.z2_galmhellingen": "De Galmhellingen",
    "vadsrijk.kern.gebied.z2_knuffelmeer": "Het Bevroren Knuffelmeer",
    "vadsrijk.kern.gebied.z2_splinterkloof": "De Splinterkloof",
    "vadsrijk.kern.gebied.z2_spiegelburcht": "De Spiegelburcht",
    "vadsrijk.kern.gebied.z2_ring": "De Splinterpieken",
    # the Spiegelburcht door (WereldDeuren id spiegelburcht)
    W + "deur.titel.spiegelburcht": "De Spiegelburcht",
    W + "deur.onder.spiegelburcht": "Kerker van zone 2 - lv 28-30",
    W + "deur.dicht.spiegelburcht": "De spiegeldeur weerkaatst alleen je eigen gezicht. Er moet een sleutel zijn...",
    # airship line
    Z + "luchtschip.titel": "Kandijkom",
    Z + "luchtschip.onder": "De Splinterpieken - lv 20-30",
    Z + "luchtschip.onder_haven": "Terug met de guhballon-lijn. Lekker warm!",
    Z + "luchtschip.nog_niet": "De kapitein schudt zijn hoofd: eerst moet het Vadsrijk hier geholpen zijn. Njeg.",
    Z + "luchtschip.te_laag": "De kapitein kijkt bezorgd: daarboven is het ijskoud en gevaarlijk. Kom terug op niveau %s.",
    Z + "luchtschip.tip": "Sluip en klik op de bel voor de lijn naar Kandijkom.",
    Z + "opening.titel": "De Splinterpieken",
    Z + "opening.onder": "Waar de vads versteent tot grauw kristal",
    Z + "opening.chocomelk": "Pfoe, dat was op het nippertje! Welkom in Kandijkom. Eerst een beker warme chocomelk.",
    # gondola
    Z + "gondel.titel": "Gondelbaan",
    Z + "gondel.omhoog": "Kandijkom - Galmtop",
    Z + "gondel.omlaag": "Galmtop - Kandijkom",
    Z + "gondel.dicht": "De gondel staat nog stil. De gondelguh drinkt eerst zijn chocomelk op.",
    # viewpoints
    W + "uitkijk.galmtop": "Galmtop",
    W + "uitkijk.meerkijk": "Meerkijk",
    W + "uitkijk.kloofkijk": "Kloofkijk",
}


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _png(img, naam):
    pad = os.path.join(AS, "textures", "vadsrijk", "wereld", naam)
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    img.save(pad)


def tex_band():
    """Vertical fade: opaque white at the bottom (v = 1) to nothing at the top (tinted by the colour modulator)."""
    im = Image.new("RGBA", (4, 128), (255, 255, 255, 0))
    for y in range(128):
        a = int(255 * (y / 127) ** 1.6)
        for x in range(4):
            im.putpixel((x, y), (255, 255, 255, a))
    return im


def tex_parelwolk(i):
    W_, H = 256, 128
    rng = random.Random(70 + i)
    im = Image.new("RGBA", (W_, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for k in range(14):
        x, y = rng.randint(40, 216), rng.randint(44, 84)
        rx, ry = rng.randint(22, 46), rng.randint(12, 22)
        t = rng.random()
        kleur = [(255, 226, 240), (220, 238, 255), (232, 222, 255), (226, 255, 246)][k % 4]
        d.ellipse((x - rx, y - ry, x + rx, y + ry), fill=kleur + (int(120 + 80 * t),))
    im = im.filter(ImageFilter.GaussianBlur(7))
    # a mother-of-pearl sheen: faint rainbow bands across
    glans = Image.new("RGBA", (W_, H), (0, 0, 0, 0))
    g = ImageDraw.Draw(glans)
    for x in range(W_):
        h = (x / W_ * 2 + i * 0.3) % 1.0
        r, gg, b = [int(200 + 55 * (0.5 + 0.5 * math.sin(2 * math.pi * (h + o)))) for o in (0, 1 / 3, 2 / 3)]
        g.line([(x, 0), (x, H)], fill=(r, gg, b, 60))
    alpha = im.split()[3]
    glans.putalpha(Image.eval(alpha, lambda a: a // 3))
    return Image.alpha_composite(im, glans)


def tex_poollicht():
    W_, H = 256, 128
    rng = random.Random(5)
    im = Image.new("RGBA", (W_, H), (0, 0, 0, 0))
    px = im.load()
    stralen = [rng.random() for _ in range(W_)]
    for x in range(W_):
        s = 0.55 + 0.45 * math.sin(x * 0.21) * math.sin(x * 0.047 + 1) + 0.25 * (stralen[x] - 0.5)
        s = max(0.1, min(1.0, s))
        for y in range(H):
            v = y / (H - 1)                 # 0 top, 1 bottom
            if v > 0.92:
                a = (1 - v) / 0.08
            else:
                a = v ** 1.4
            # colour: green at the foot, pink-violet at the top
            r = int(90 + 165 * (1 - v) ** 1.5)
            g = int(255 * v ** 0.6)
            b = int(150 + 70 * (1 - v))
            px[x, y] = (r, g, b, int(255 * a * s))
    return im.filter(ImageFilter.GaussianBlur(1.5))


def tex_maan():
    S = 128
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse((8, 8, 120, 120), fill=(236, 244, 255, 255))
    rng = random.Random(3)
    for _ in range(9):
        x, y, r = rng.randint(30, 98), rng.randint(30, 98), rng.randint(5, 14)
        d.ellipse((x - r, y - r, x + r, y + r), fill=(214, 226, 246, 255))
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse((0, 0, S - 1, S - 1), fill=(200, 226, 255, 90))
    glow = glow.filter(ImageFilter.GaussianBlur(6))
    return Image.alpha_composite(glow, im.filter(ImageFilter.SMOOTH))


def tex_spitsen():
    W_, H = 512, 216
    rng = random.Random(12)
    im = Image.new("RGBA", (W_, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for k in range(26):
        x = rng.randint(0, W_)
        h = rng.randint(50, 200) if abs(x - W_ / 2) > 70 else rng.randint(30, 110)
        w = rng.randint(8, 22)
        lean = rng.randint(-14, 14)
        donker = (96, 108, 138, 235)
        d.polygon([(x - w, H), (x + lean, H - h), (x + w, H)], fill=donker)
        d.line([(x + lean, H - h), (x + w, H)], fill=(190, 214, 245, 230), width=2)
    d.rectangle((0, H - 14, W_, H), fill=(96, 108, 138, 235))
    return im.filter(ImageFilter.GaussianBlur(0.6))


def textures():
    _png(tex_band(), "z2_band.png")
    _png(tex_parelwolk(0), "z2_parelwolk_0.png")
    _png(tex_parelwolk(1), "z2_parelwolk_1.png")
    _png(tex_poollicht(), "z2_poollicht.png")
    _png(tex_maan(), "z2_maan.png")
    _png(tex_spitsen(), "z2_spitsen.png")


def build(h):
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)
    textures()


def _direct():
    textures()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(AS, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update(TEKSTEN)
        _schrijf(p, d)
    print(f"vadsrijk_z2_wereld: {len(TEKSTEN)} texts, sky textures written")


if __name__ == "__main__":
    _direct()
