"""
biomes3 fix-plaatsing: draws the placement maps the game test BioPlaatsingGameTests.bioPlaatsingMaten writes when the
environment variable GUHS_BIO_PLAATSING_KAART names a directory (plaatsing_voor_<seed>.bin: the regions as they were,
two noise tops; plaatsing_na_<seed>.bin: as they are). Not in FEATURES; run by hand:

    python tools/features/bio_wereld_plaatsing_kaart.py <directory> [crop_blocks]

For every pair it writes plaatsing_<seed>.png (before on the left, after on the right; crop_blocks: only the middle
square of that many blocks, default everything) from the samples: one byte each, 0 other land, 1 Klaterdal, 2 the
lake's shore strip (biome Bloesemmeertje), 3 lake water, 4 Wolkenweide, 5 an older region our regions give way to.
"""
import struct
import sys
from pathlib import Path

from PIL import Image, ImageDraw

KLEUR = {0: (226, 230, 214), 1: (214, 118, 62), 2: (245, 170, 200), 3: (52, 160, 196), 4: (150, 110, 220), 5: (176, 184, 172)}


def lees(pad, crop):
    data = Path(pad).read_bytes()
    n, stap, x0, z0 = struct.unpack(">iiii", data[:16])
    img = Image.frombytes("P", (n, n), data[16:16 + n * n])
    pal = [0] * 768
    for k, (r, g, b) in KLEUR.items():
        pal[k * 3:k * 3 + 3] = [r, g, b]
    img.putpalette(pal)
    if crop:
        m = n // 2
        h = min(m, crop // stap // 2)
        img = img.crop((m - h, m - h, m + h, m + h))
        x0 += (m - h) * stap
        z0 += (m - h) * stap
    return img.convert("RGB"), stap, x0, z0


def teken(img, stap, x0, z0, titel, maat):
    n = img.size[0]
    f = maat / n
    img = img.resize((maat, maat), Image.NEAREST if f >= 1 else Image.BOX)
    d = ImageDraw.Draw(img)
    km = 1000 if n * stap <= 9000 else 4000
    eerste = -(-x0 // km) * km
    for g in range(eerste, x0 + n * stap, km):
        p = (g - x0) / stap * f
        d.line([(p, 0), (p, maat)], fill=(120, 126, 118), width=1)
        d.text((p + 3, maat - 14), str(g), fill=(60, 60, 60))
    eerste = -(-z0 // km) * km
    for g in range(eerste, z0 + n * stap, km):
        p = (g - z0) / stap * f
        d.line([(0, p), (maat, p)], fill=(120, 126, 118), width=1)
        d.text((3, p + 2), str(g), fill=(60, 60, 60))
    d.rectangle([0, 0, 13 + 7 * len(titel), 18], fill=(255, 255, 255))
    d.text((6, 3), titel, fill=(0, 0, 0))
    return img


def main():
    map_ = Path(sys.argv[1])
    crop = int(sys.argv[2]) if len(sys.argv) > 2 else 0
    for voor in sorted(map_.glob("plaatsing_voor_*.bin")):
        seed = voor.stem.split("_")[-1]
        na = map_ / f"plaatsing_na_{seed}.bin"
        if not na.exists():
            continue
        maat = 1100
        a = teken(*lees(voor, crop), f"BEFORE (seed {seed}): regions = two noise tops", maat)
        b = teken(*lees(na, crop), f"AFTER (seed {seed}): regions with a size of their own", maat)
        uit = Image.new("RGB", (2 * maat + 12, maat + 26), (255, 255, 255))
        uit.paste(a, (0, 0))
        uit.paste(b, (maat + 12, 0))
        d = ImageDraw.Draw(uit)
        x = 6
        for k, naam in ((1, "Klaterdal"), (2, "lake shore (biome Bloesemmeertje)"), (3, "lake water"), (4, "Wolkenweide"), (5, "older region (sea, Knuffeldal, polder, tundra, Guhwai'i, Bleekwoud)"), (0, "other land")):
            d.rectangle([x, maat + 7, x + 12, maat + 19], fill=KLEUR[k], outline=(80, 80, 80))
            d.text((x + 17, maat + 7), naam, fill=(0, 0, 0))
            x += 30 + 7 * len(naam)
        naam = map_.parent / f"plaatsing_{seed}{'_' + str(crop) if crop else ''}.png"
        uit.save(naam)
        print(naam)


if __name__ == "__main__":
    main()
