"""
einde2, slice biomes: draws what /guhs einde2 biomes kaart <biome> <straal> wrote (E2BiomesCommando.java, a dev run only):
a look at a generated biome without a client. Not part of make_resources.py.

    python tools/features/einde2_biomes_kaart.py <game dir>/einde2_biomes/<biome>.txt <out.png>      (from the project root)

Left: the island from above, every column in the mean colour of its top block's texture, lighter where it is higher (the
void is dark; a red dot = the biome of that column is another one). Right: the cross-section through the middle (west to
east, y 0 to 127). Under it the count of every guhs block and fluid in the volume.
"""
import os
import sys

from PIL import Image, ImageDraw

TEX = os.path.join("src", "main", "resources", "assets", "guhs", "textures", "block")
VOID = (26, 12, 30)
EXTRA = {"minecraft:water": (64, 150, 214), "guhs:kaas_saus": (250, 190, 40), "guhs:kruimelzandlaag": None, "guhs:knabbelkoorbloem": None}
_cache = {}


def colour(block):
    if block in _cache:
        return _cache[block]
    c = EXTRA.get(block)
    if c is None:
        name = {"guhs:kruimelzandlaag": "kruimelzand"}.get(block, block.split(":")[1])
        path = os.path.join(TEX, name + ".png")
        if block.startswith("guhs:") and os.path.exists(path):
            im = Image.open(path).convert("RGBA").crop((0, 0, 16, 16))
            px = [p for p in im.getdata() if p[3] > 40]
            c = tuple(sum(p[i] for p in px) // len(px) for i in range(3))
        else:
            c = (200, 60, 200)
    _cache[block] = c
    return c


def draw(path, out):
    tops, section, counts, head = {}, {}, [], None
    for line in open(path, encoding="utf-8"):
        f = line.split()
        if f[0] == "M":
            head = f
        elif f[0] == "T":
            tops[(int(f[1]), int(f[2]))] = (int(f[3]), f[4], f[5])
        elif f[0] == "S":
            section[(int(f[1]), int(f[2]))] = f[3]
        elif f[0] == "C":
            counts.append((f[1], int(f[2])))
    r, biome = int(head[3]), head[4]
    n, s = 2 * r + 1, 4
    img = Image.new("RGB", (n * s * 2 + 24, max(n * s, 128 * s) + 16 + 14 * ((len(counts) + 2) // 3 + 1)), (16, 8, 18))
    d = ImageDraw.Draw(img)
    ys = [t[0] for t in tops.values() if t[1] != "-"]
    lo, hi = (min(ys), max(ys)) if ys else (0, 1)
    for (dx, dz), (y, block, b) in tops.items():
        if block == "-":
            c = VOID
        else:
            base = colour(block)
            f = 0.55 + 0.6 * (y - lo) / max(1, hi - lo)
            c = tuple(max(0, min(255, int(v * f))) for v in base)
        x0, y0 = (dx + r) * s, (dz + r) * s
        d.rectangle((x0, y0, x0 + s - 1, y0 + s - 1), fill=c)
        if b != biome and block != "-":
            d.point((x0 + 1, y0 + 1), fill=(255, 40, 40))
    ox = n * s + 24
    d.rectangle((ox, 0, ox + n * s - 1, 128 * s - 1), fill=VOID)
    for (dx, y), block in section.items():
        x0, y0 = ox + (dx + r) * s, (127 - y) * s
        d.rectangle((x0, y0, x0 + s - 1, y0 + s - 1), fill=colour(block))
    ty = max(n * s, 128 * s) + 4
    d.text((4, ty), f"{biome} at {head[1]}, {head[2]} (radius {r}); top y {lo}..{hi}", fill=(255, 255, 255))
    for i, (block, cnt) in enumerate(counts):
        d.rectangle((4 + (i % 3) * 330, ty + 16 + (i // 3) * 14, 14 + (i % 3) * 330, ty + 26 + (i // 3) * 14), fill=colour(block))
        d.text((20 + (i % 3) * 330, ty + 14 + (i // 3) * 14), f"{block} {cnt}", fill=(230, 230, 230))
    img.save(out)


if __name__ == "__main__":
    draw(sys.argv[1], sys.argv[2])
