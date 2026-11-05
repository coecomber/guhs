"""
The map tiles of guhs.nl/vadsrijk/kaart: a clean, technical, satellite-like top-down of the real map tiles (the 64x320x64 .nbt
tiles of tools/vadsrijk/maak_kaart.py), cut into Leaflet zoom tiles. No labels in the picture: the page draws names, borders
and markers itself (sharp at every zoom, NL/EN).

    python tools/wiki_site/vadsrijk/kaart.py [--tegels <folder with t_<tx>_<tz>.nbt>] [--uit docs/wiki/vadsrijk_kaart]

Re-runnable: when the world layout moves (guhs1100-avontuur/wereldindeling), regenerate the map (maak_kaart.py) and run this
again; the page reads the bounds from kaart.json. The tiles are committed (like docs/wiki/img): CI has no Minecraft jar.

Leaflet frame: CRS.Simple, 1 map unit = 1 block, lat = -z, lng = x. Zoom 0 = 1 px per block (native), -1..-4 halve it; the page
zooms in to +3 with pixelated upscaling. Tile {z}/{x}/{y}.webp covers pixels [x*256, (x+1)*256) at that zoom.
"""
import argparse
import hashlib
import io
import json
import os
import sys
import time
import zipfile

import numpy as np
from PIL import Image

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "vadsrijk", "audit"))
import tegellezer as TL  # noqa: E402

TEGELS = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
CACHE = os.path.join(ROOT, "tools", "vadsrijk", "_cache")
MC_JAR = os.path.join(os.environ.get("APPDATA", ""), "PrismLauncher", "libraries", "com", "mojang", "minecraft", "26.1.2",
                      "minecraft-26.1.2-client.jar")
T = 256
ZOOMS = (0, -1, -2, -3, -4)

DOORZICHTIG = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:barrier", "minecraft:light",
               "minecraft:structure_void", "minecraft:structure_block", "minecraft:jigsaw"}
WATER = {"minecraft:water", "minecraft:bubble_column", "minecraft:kelp", "minecraft:kelp_plant", "minecraft:seagrass",
         "minecraft:tall_seagrass"}
VAST = {"minecraft:water": (46, 98, 186), "minecraft:lava": (230, 110, 30), "minecraft:snow": (240, 244, 250),
        "minecraft:powder_snow": (236, 240, 248), "minecraft:ice": (150, 190, 240), "minecraft:packed_ice": (140, 180, 235),
        "minecraft:blue_ice": (120, 170, 235)}
GRAS = (124, 182, 84)
BLAD = (88, 150, 52)
TINT_GRAS = ("grass_block", "short_grass", "tall_grass", "fern", "large_fern", "sugar_cane", "vine")
TINT_BLAD = ("oak_leaves", "jungle_leaves", "acacia_leaves", "dark_oak_leaves", "mangrove_leaves", "birch_leaves",
             "spruce_leaves", "lily_pad", "attached_melon_stem", "melon_stem", "pumpkin_stem")


class Bronnen:
    """Textures from the mod's own assets folder, then the Minecraft client jar."""

    def __init__(self):
        self.mapen = [os.path.join(ROOT, "src", "main", "resources")]
        self.zips = []
        if os.path.exists(MC_JAR):
            z = zipfile.ZipFile(MC_JAR)
            self.zips.append((z, set(z.namelist())))
        else:
            print("  (no Minecraft jar at", MC_JAR, "- vanilla blocks get a fallback colour)")

    def lees(self, pad):
        for m in self.mapen:
            f = os.path.join(m, pad)
            if os.path.exists(f):
                return open(f, "rb").read()
        for z, namen in self.zips:
            if pad in namen:
                return z.read(pad)
        return None

    def json(self, ns, soort, p):
        d = self.lees(f"assets/{ns}/{soort}/{p}.json")
        try:
            return json.loads(d) if d else None
        except ValueError:
            return None

    def textuur(self, naam):
        ns, p = naam.split(":", 1)
        bs = self.json(ns, "blockstates", p)
        model = None
        if bs:
            if bs.get("variants"):
                v = next(iter(bs["variants"].values()))
                model = (v[0] if isinstance(v, list) else v).get("model")
            elif bs.get("multipart"):
                a = bs["multipart"][0]["apply"]
                model = (a[0] if isinstance(a, list) else a).get("model")
        gezien = {}
        for _ in range(8):
            if not model:
                break
            mns, mp = model.split(":", 1) if ":" in model else ("minecraft", model)
            m = self.json(mns, "models", mp)
            if not m:
                break
            for k, v in (m.get("textures") or {}).items():
                gezien.setdefault(k, v)
            model = m.get("parent")
        def los(t):
            for _ in range(8):
                if isinstance(t, dict):
                    t = t.get("sprite")
                if not isinstance(t, str) or not t.startswith("#"):
                    break
                t = gezien.get(t[1:])
            return t if isinstance(t, str) else None
        for k in ("top", "up", "end", "all", "texture", "cross", "plant", "wool", "particle", "side"):
            t = los(gezien.get(k))
            if t:
                return t
        return None

    def kleur(self, naam):
        t = self.textuur(naam)
        if not t:
            return None
        ns, p = t.split(":", 1) if ":" in t else ("minecraft", t)
        d = self.lees(f"assets/{ns}/textures/{p}.png")
        if not d:
            return None
        im = Image.open(io.BytesIO(d)).convert("RGBA")
        if im.height > im.width:
            im = im.crop((0, 0, im.width, im.width))
        a = np.asarray(im, np.float32)
        w = a[..., 3:4] / 255
        if w.sum() < 1:
            return None
        return tuple(float(c) for c in (a[..., :3] * w).sum((0, 1)) / w.sum())


def kleur_lut(palet, bron, cache={}):
    out = np.zeros((len(palet), 3), np.float32)
    for i, s in enumerate(palet):
        naam = s.split("[")[0]
        if naam not in cache:
            k = VAST.get(naam)
            if k is None:
                k = bron.kleur(naam)
                kort = naam.split(":")[1]
                if k is not None and naam.startswith("minecraft:"):
                    grijs = max(k) - min(k) < 18
                    if kort in TINT_GRAS and grijs:
                        k = tuple(c * g / 255 * 1.25 for c, g in zip(k, GRAS))
                    elif kort in TINT_BLAD and grijs:
                        k = tuple(c * g / 255 * 1.3 for c, g in zip(k, BLAD))
            if k is None:
                h = int(hashlib.md5(naam.encode()).hexdigest()[:6], 16)
                k = (120 + (h >> 16 & 63), 110 + (h >> 8 & 63), 110 + (h & 63))
            cache[naam] = k
        out[i] = cache[naam]
    return out


def lees_tegel(pad, bron):
    """-> (rgb 64x64x3 float, height 64x64, water depth 64x64, filled mask) for one tile (local x = column, z = row)."""
    palet, blokken = TL.lees(pad)
    if not palet or blokken is None or len(blokken[0]) == 0:
        z = np.zeros((64, 64), np.float32)
        return np.zeros((64, 64, 3), np.float32), z, z.copy(), np.zeros((64, 64), bool)
    xs, ys, zs, st = (np.asarray(a).astype(np.int64) for a in blokken)
    namen = [p.split("[")[0] for p in palet]
    vast = np.array([n not in DOORZICHTIG for n in namen])
    nat = np.array([n in WATER for n in namen])
    lut = kleur_lut(palet, bron)
    rgb = np.zeros((64, 64, 3), np.float32)
    hoog = np.zeros((64, 64), np.float32)
    diep = np.zeros((64, 64), np.float32)
    vol = np.zeros((64, 64), bool)
    k = vast[st]
    xs, ys, zs, st = xs[k], ys[k], zs[k], st[k]
    if len(xs) == 0:
        return rgb, hoog, diep, vol
    # highest block per column
    idx = zs.astype(np.int64) * 64 + xs
    orde = np.lexsort((ys, idx))
    laatste = np.r_[idx[orde][1:] != idx[orde][:-1], True]
    top = orde[laatste]
    ti = idx[top]
    rgb.reshape(-1, 3)[ti] = lut[st[top]]
    hoog.reshape(-1)[ti] = ys[top]
    vol.reshape(-1)[ti] = True
    # water: the depth to the first non-water block below the surface
    wtop = nat[st[top]]
    if wtop.any():
        bodem = ~nat[st]
        bi = idx[bodem]
        by = ys[bodem]
        o2 = np.lexsort((by, bi))
        l2 = np.r_[bi[o2][1:] != bi[o2][:-1], True]
        bod = np.full(64 * 64, -1, np.float32)
        bod[bi[o2][l2]] = by[o2][l2]
        wi = ti[wtop]
        d = hoog.reshape(-1)[wi] - bod[wi]
        diep.reshape(-1)[wi] = np.where(bod[wi] < 0, 12, np.clip(d, 0, 30))
    return rgb, hoog, diep, vol


def tegel_lijst(map_):
    return sorted(TL.tegels(map_))


def sleutel(lijst):
    h = hashlib.sha1()
    for tx, tz, p in lijst:
        st = os.stat(p)
        h.update(f"{tx},{tz},{st.st_size},{int(st.st_mtime)}".encode())
    return h.hexdigest()[:16]


def bovenaanzicht(map_, opnieuw=False):
    lijst = tegel_lijst(map_)
    if not lijst:
        raise SystemExit(f"no tiles in {map_}")
    key = sleutel(lijst)
    os.makedirs(CACHE, exist_ok=True)
    npz = os.path.join(CACHE, f"site_kaart_{key}.npz")
    if os.path.exists(npz) and not opnieuw:
        d = np.load(npz)
        return d["rgb"], d["hoog"], d["diep"], d["vol"], int(d["x0"]), int(d["z0"])
    t0 = time.time()
    bron = Bronnen()
    txs = [t[0] for t in lijst]
    tzs = [t[1] for t in lijst]
    tx0, tz0 = min(txs), min(tzs)
    W, H = (max(txs) - tx0 + 1) * 64, (max(tzs) - tz0 + 1) * 64
    rgb = np.zeros((H, W, 3), np.float32)
    hoog = np.zeros((H, W), np.float32)
    diep = np.zeros((H, W), np.float32)
    vol = np.zeros((H, W), bool)
    for n, (tx, tz, p) in enumerate(lijst):
        r, h, d, v = lees_tegel(p, bron)
        j, i = (tz - tz0) * 64, (tx - tx0) * 64
        rgb[j:j + 64, i:i + 64], hoog[j:j + 64, i:i + 64], diep[j:j + 64, i:i + 64], vol[j:j + 64, i:i + 64] = r, h, d, v
        if n % 200 == 0:
            print(f"  tiles {n}/{len(lijst)} ({time.time() - t0:.0f} s)")
    for f in os.listdir(CACHE):
        if f.startswith("site_kaart_") and f.endswith(".npz"):
            os.remove(os.path.join(CACHE, f))
    np.savez_compressed(npz, rgb=rgb, hoog=hoog, diep=diep, vol=vol, x0=tx0 * 64, z0=tz0 * 64)
    print(f"  top-down {W}x{H} from {len(lijst)} tiles ({time.time() - t0:.0f} s)")
    return rgb, hoog, diep, vol, tx0 * 64, tz0 * 64


def kleuren(rgb, hoog, diep, vol):
    """The technical look: texture colours, soft hill shading, water by depth, a thin dark edge round every height step."""
    gz, gx = np.gradient(hoog)
    licht = np.clip(1 + (-gx * 0.55 - gz * 0.75) * 0.09, 0.62, 1.32)
    stap = np.maximum(np.abs(gx), np.abs(gz))
    rand = np.where(stap > 2.5, 0.86, 1.0)
    out = rgb * licht[..., None] * rand[..., None]
    nat = diep > 0
    if nat.any():
        f = np.clip(diep / 14.0, 0, 1)[..., None]
        ondiep = np.array((86, 160, 214), np.float32)
        diepe = np.array((30, 72, 150), np.float32)
        out[nat] = (ondiep * (1 - f) + diepe * f)[nat]
    # a gentle lift in height: higher = a little lighter (reads as relief on a flat colour field)
    hn = np.clip((hoog - 60) / 220.0, -0.2, 1)
    out = out * (0.92 + 0.16 * hn[..., None])
    out = np.clip(out, 0, 255).astype(np.uint8)
    a = np.where(vol, 255, 0).astype(np.uint8)
    return np.dstack([out, a])


def knip(img, x0, z0, uit, kwaliteit=82):
    """Leaflet tiles for every zoom in ZOOMS: tile (ix, iy) at zoom z = pixels [ix*256..) of the world scaled by 2^z."""
    n = 0
    H, W = img.shape[:2]
    basis = Image.fromarray(img, "RGBA")
    grenzen = {}
    for z in ZOOMS:
        s = 2.0 ** z
        # world pixel bounds at this zoom
        px0, pz0 = int(np.floor(x0 * s)), int(np.floor(z0 * s))
        bw, bh = max(1, int(round(W * s))), max(1, int(round(H * s)))
        im = basis if z == 0 else basis.resize((bw, bh), Image.LANCZOS if z < 0 else Image.NEAREST)
        ix0, iy0 = px0 // T, pz0 // T
        ix1, iy1 = (px0 + bw - 1) // T, (pz0 + bh - 1) // T
        grenzen[z] = [ix0, iy0, ix1, iy1]
        for iy in range(iy0, iy1 + 1):
            for ix in range(ix0, ix1 + 1):
                tegel = Image.new("RGBA", (T, T), (0, 0, 0, 0))
                ox, oy = ix * T - px0, iy * T - pz0
                tegel.paste(im.crop((ox, oy, ox + T, oy + T)), (0, 0))
                if tegel.getextrema()[3][1] == 0:
                    continue
                d = os.path.join(uit, str(z), str(ix))
                os.makedirs(d, exist_ok=True)
                tegel.save(os.path.join(d, f"{iy}.webp"), "WEBP", quality=kwaliteit, method=5)
                n += 1
    return n, grenzen


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--tegels", default=TEGELS)
    ap.add_argument("--uit", default=os.path.join(ROOT, "docs", "wiki", "vadsrijk_kaart"))
    ap.add_argument("--opnieuw", action="store_true", help="ignore the top-down cache")
    a = ap.parse_args(argv)
    t0 = time.time()
    rgb, hoog, diep, vol, x0, z0 = bovenaanzicht(a.tegels, a.opnieuw)
    img = kleuren(rgb, hoog, diep, vol)
    if os.path.isdir(a.uit):
        import shutil
        shutil.rmtree(a.uit)
    os.makedirs(a.uit)
    n, grenzen = knip(img, x0, z0, os.path.join(a.uit, "tegels"))
    H, W = img.shape[:2]
    klein = Image.fromarray(img, "RGBA").resize((max(1, W // 8), max(1, H // 8)), Image.LANCZOS)
    klein.save(os.path.join(a.uit, "overzicht.webp"), "WEBP", quality=80)
    info = dict(x0=x0, z0=z0, x1=x0 + W, z1=z0 + H, zooms=list(ZOOMS), tegel=T, grenzen=grenzen,
                tegels=len(tegel_lijst(a.tegels)), gemaakt=time.strftime("%Y-%m-%d %H:%M"))
    with open(os.path.join(a.uit, "kaart.json"), "w", encoding="utf-8") as f:
        json.dump(info, f, indent=1)
    size = sum(os.path.getsize(os.path.join(d, x)) for d, _, fs in os.walk(a.uit) for x in fs)
    print(f"map tiles -> {a.uit}: {n} tiles, {size / 1e6:.1f} MB, world x {x0}..{x0 + W}, z {z0}..{z0 + H} ({time.time() - t0:.0f} s)")


if __name__ == "__main__":
    main()
