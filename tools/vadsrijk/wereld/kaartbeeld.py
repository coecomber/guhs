"""Top-down check image of the generated tiles: the highest block per column, coloured by its texture, with hill shading."""
import io

import numpy as np
from PIL import Image

import kaart as KA

VAST = {"minecraft:water": (64, 118, 220), "minecraft:grass_block": (120, 190, 90), "minecraft:moss_block": (90, 140, 60),
        "guhs:vr_w_litteken_gras": (140, 136, 146), "guhs:vr_w_litteken_plant": (150, 146, 156),
        "minecraft:short_grass": (120, 190, 90), "minecraft:barrier": (0, 0, 0), "minecraft:oak_leaves": (90, 160, 60),
        "minecraft:azalea_leaves": (100, 160, 70), "minecraft:light": (255, 255, 255)}


def kleur_van(naam, cache={}):
    if naam in cache:
        return cache[naam]
    k = VAST.get(naam)
    if k is None:
        try:
            import vibekit as V
            import mijnkit as M
            r, _ = V.res()
            t = M._textuur_van(r, naam)
            if t:
                ns, p = (t.split(":") + [""])[:2] if ":" in t else ("minecraft", t)
                data = r.read(f"assets/{ns}/textures/{p}.png")
                im = Image.open(io.BytesIO(data)).convert("RGBA")
                a = np.asarray(im, np.float32)
                w = a[..., 3:4] / 255
                k = tuple(int(c) for c in (a[..., :3] * w).sum((0, 1)) / max(1, w.sum()))
        except Exception:
            k = None
        if k is None:
            h = hash(naam) & 0xFFFFFF
            k = (h >> 16 & 255, h >> 8 & 255, h & 255)
        if naam.endswith(("grass_block", "_leaves")) and k[0] < 150 and abs(k[0] - k[1]) < 20:
            k = (110, 180, 80)
    cache[naam] = k
    return k


def teken(W, bovenaan, pad):
    img = np.zeros((KA.NZ, KA.NX, 3), np.float32)
    hgt = np.zeros((KA.NZ, KA.NX), np.float32)
    lut = np.array([kleur_van(s.split("[")[0]) for s in W.palet.lijst], np.float32)
    for (tx, tz), (hy, hid) in bovenaan.items():
        i0, j0 = tx * 64 - KA.X0, tz * 64 - KA.Z0
        img[j0:j0 + 64, i0:i0 + 64] = lut[hid].transpose(1, 0, 2)
        hgt[j0:j0 + 64, i0:i0 + 64] = hy.T
    gz, gx = np.gradient(hgt)
    licht = np.clip(1 + (-gx * 0.5 - gz * 0.7) * 0.12, 0.6, 1.35)
    out = np.clip(img * licht[..., None], 0, 255).astype(np.uint8)
    Image.fromarray(out).save(pad)
    klein = Image.fromarray(out).resize((KA.NX // 2, KA.NZ // 2), Image.LANCZOS)
    klein.save(pad.replace(".png", "_klein.png"))
    print("kaartbeeld:", pad)
