"""
biomes3 wereld, the Bloesemmeertje: measures and draws the GENERATED blocks of a box, read from the dev server's region
files (dimension guhs:guhmension). Not in FEATURES. Called through bio_wereld_meer_meet.py:

    python tools/features/bio_wereld_meer_meet.py regio <world dir> <x0> <z0> <x1> <z1> <out prefix> [alles | x0,z0,x1,z1]

Prints: the depth histogram and the mean depth by distance to land (shore to middle), the bed per depth, the islands in
the box with their sizes, the tree cover and the tree-free flat ground of the large ones, the share of open water, land
plants that stand in water or on nothing, lilies and petals that are not on a water source, wood and crowns that float.
Draws <prefix>_boven.png (from above) and <prefix>_diepte.png, and with the last argument an offline 3D picture of that
part of the box (<prefix>_3d.png, <prefix>_3d_laag.png; base-ontwerpen's renderer on this worktree's resources).
"""
import io
import os
import struct
import sys
import zlib

import numpy as np
from PIL import Image

WY = 49   # MeerTerrein.WATER
BED = {1: (224, 212, 170)}
WATER = (79, 214, 210)
LAND_PLANT = ("roze_guhbloem", "knabbelroos", "roze_hibiscus", "roze_gras", "bloesemmeertje_riet", "guhbloesem_sapling", "plumeria")
KLEUR = {"minecraft:sand": (228, 214, 166), "minecraft:pink_wool": (242, 160, 196), "guhs:gladde_knuffelsteen": (238, 234, 230),
         "guhs:knuffelsteen": (232, 226, 222), "minecraft:calcite": (226, 226, 220), "minecraft:light_blue_wool": (74, 176, 214),
         "minecraft:cyan_wool": (30, 140, 148), "minecraft:cyan_concrete": (21, 119, 136), "guhs:guhbloesem_leaves": (236, 128, 184),
         "guhs:guhbloesem_log": (112, 72, 84), "guhs:drijvende_bloesemblaadjes": (255, 222, 236), "guhs:guh_waterlelie": (240, 120, 170),
         "guhs:bloesemmeertje_riet": (206, 190, 150), "minecraft:seagrass": (60, 150, 110), "guhs:kaaskoraal": (240, 200, 80),
         "guhs:reuzenschelp": (250, 240, 250)}


def onder_water(bed, diepte):
    """A bed colour seen through d blocks of the lake's water (a rough picture of the game's look)."""
    t = min(0.75, 0.30 + 0.06 * diepte)
    return tuple(int(b * (1 - t) + w * t * 0.9) for b, w in zip(bed, WATER))


def _nbt(buf):
    f = io.BytesIO(buf)

    def naam():
        n = struct.unpack(">H", f.read(2))[0]
        return f.read(n).decode("utf-8", "replace")

    def waarde(t):
        if t == 1:
            return struct.unpack(">b", f.read(1))[0]
        if t == 2:
            return struct.unpack(">h", f.read(2))[0]
        if t == 3:
            return struct.unpack(">i", f.read(4))[0]
        if t == 4:
            return struct.unpack(">q", f.read(8))[0]
        if t == 5:
            return struct.unpack(">f", f.read(4))[0]
        if t == 6:
            return struct.unpack(">d", f.read(8))[0]
        if t == 7:
            return f.read(struct.unpack(">i", f.read(4))[0])
        if t == 8:
            return naam()
        if t == 9:
            et = f.read(1)[0]
            return [waarde(et) for _ in range(struct.unpack(">i", f.read(4))[0])]
        if t == 10:
            d = {}
            while True:
                tt = f.read(1)[0]
                if tt == 0:
                    return d
                k = naam()
                d[k] = waarde(tt)
        if t == 11:
            n = struct.unpack(">i", f.read(4))[0]
            return np.frombuffer(f.read(4 * n), ">i4")
        if t == 12:
            n = struct.unpack(">i", f.read(4))[0]
            return np.frombuffer(f.read(8 * n), ">u8")
        raise ValueError(t)
    t = f.read(1)[0]
    naam()
    return waarde(t)


def lees(wereld, x0, z0, x1, z1, y0=36, y1=84):
    """The blocks of [x0, x1) x [y0, y1) x [z0, z1): (vol[x, y, z] of palette indexes, palette of state strings)."""
    map_ = os.path.join(wereld, "dimensions", "guhs", "guhmension", "region")
    pal, idx = ["minecraft:air"], {"minecraft:air": 0}
    vol = np.zeros((x1 - x0, y1 - y0, z1 - z0), np.uint16)
    regios, mist = {}, 0
    for cx in range(x0 >> 4, ((x1 - 1) >> 4) + 1):
        for cz in range(z0 >> 4, ((z1 - 1) >> 4) + 1):
            rk = (cx >> 5, cz >> 5)
            if rk not in regios:
                pad = os.path.join(map_, f"r.{rk[0]}.{rk[1]}.mca")
                regios[rk] = open(pad, "rb").read() if os.path.exists(pad) else None
            data = regios[rk]
            c = None
            if data:
                i = 4 * ((cx & 31) + (cz & 31) * 32)
                off = struct.unpack(">I", b"\0" + data[i:i + 3])[0]
                if off:
                    q = off * 4096
                    ln, comp = struct.unpack(">IB", data[q:q + 5])
                    raw = data[q + 5:q + 4 + ln]
                    c = _nbt(zlib.decompress(raw) if comp == 2 else raw)
            if c is None or c.get("Status") not in ("minecraft:full", "full"):
                mist += 1
                continue
            for sec in c.get("sections", []):
                sy = sec["Y"] * 16
                bs = sec.get("block_states")
                if not bs or sy + 16 <= y0 or sy >= y1:
                    continue
                ids = []
                for e in bs["palette"]:
                    n = e["Name"]
                    if e.get("Properties"):
                        n += "[" + ",".join(f"{k}={v}" for k, v in sorted(e["Properties"].items())) + "]"
                    if n not in idx:
                        idx[n] = len(pal)
                        pal.append(n)
                    ids.append(idx[n])
                ids = np.array(ids, np.uint16)
                if len(ids) == 1:
                    arr = np.full(4096, ids[0], np.uint16)
                else:
                    bits = max(4, (len(ids) - 1).bit_length())
                    per = 64 // bits
                    w = np.asarray(bs["data"], np.uint64)
                    v = (w[:, None] >> (np.arange(per, dtype=np.uint64) * np.uint64(bits))) & np.uint64((1 << bits) - 1)
                    arr = ids[v.reshape(-1)[:4096].astype(np.int64)]
                arr = arr.reshape(16, 16, 16)   # y, z, x
                ax0, az0 = max(x0, cx * 16), max(z0, cz * 16)
                ax1, az1 = min(x1, cx * 16 + 16), min(z1, cz * 16 + 16)
                ay0, ay1 = max(y0, sy), min(y1, sy + 16)
                blok = arr[ay0 - sy:ay1 - sy, az0 - cz * 16:az1 - cz * 16, ax0 - cx * 16:ax1 - cx * 16]
                vol[ax0 - x0:ax1 - x0, ay0 - y0:ay1 - y0, az0 - z0:az1 - z0] = blok.transpose(2, 0, 1)
    if mist:
        print(f"  ({mist} chunks of the box are not generated)")
    return vol, pal


def regio(wereld, x0, z0, x1, z1, uit, render=None):
    from scipy import ndimage
    y0 = 36
    vol, pal = lees(wereld, x0, z0, x1, z1, y0, 84)
    kaal = [p.split("[")[0] for p in pal]
    X, Y, Z = vol.shape
    kubus = np.ones((3, 3, 3))

    def is_(*namen):
        return np.isin(vol, [i for i, n in enumerate(kaal) if n.split(":")[1] in namen])

    lucht = vol == 0
    waterbron = np.isin(vol, [i for i, p in enumerate(pal) if p in ("minecraft:water[level=0]", "minecraft:water")])
    nat = np.isin(vol, [i for i, p in enumerate(pal) if kaal[i] == "minecraft:water" or "waterlogged=true" in p
                        or kaal[i] in ("minecraft:seagrass", "minecraft:tall_seagrass")])
    blad, stam = is_("guhbloesem_leaves"), is_("guhbloesem_log")
    plant = np.isin(vol, [i for i, n in enumerate(kaal) if any(k in n for k in LAND_PLANT)])
    drijf = is_("drijvende_bloesemblaadjes", "guh_waterlelie", "lily_pad")
    # ground: everything that is not air, water, a tree or a plant
    grond = ~(lucht | nat | blad | stam | plant | drijf)
    ys = np.arange(Y)[None, :, None]
    top = np.where(grond, ys, -1).max(axis=1) + y0            # the top ground block per column
    w = WY - y0
    meer = nat[:, w, :]                                        # water at the lake's level
    diepte = np.zeros((X, Z), int)
    for d in range(1, 12):
        diepte += (nat[:, w - d + 1, :] & (diepte == d - 1))
    print(f"box {x0} {z0} .. {x1} {z1}: {X * Z} columns, water at y {WY} in {int(meer.sum())}")
    print("  depth histogram 1..9:", " ".join(str(int((diepte[meer] == d).sum())) for d in range(1, 10)), "  deepest", int(diepte.max()))
    afst = ndimage.distance_transform_edt(meer)
    rij = []
    for a, b in ((1, 1), (2, 2), (3, 4), (5, 7), (8, 11), (12, 17), (18, 25), (26, 35), (36, 999)):
        sel = meer & (afst >= a) & (afst <= b)
        if sel.any():
            rij.append(f"{a}-{b if b < 999 else ''}: {diepte[sel].mean():.1f}")
    print("  mean depth by distance to land (blocks):", ", ".join(rij))
    print("  bed per depth:")
    bodem = np.take_along_axis(vol, np.clip(top - y0, 0, Y - 1)[:, None, :], axis=1)[:, 0, :]
    for d in range(1, 9):
        sel = meer & (diepte == d)
        if sel.any():
            n = np.bincount(bodem[sel], minlength=len(pal))
            print(f"    {d}: " + ", ".join(f"{kaal[i].split(':')[1]} {100 * n[i] // sel.sum()}%" for i in np.argsort(-n)[:4] if n[i]))
    # islands: land that does not reach the box's edge
    land = ~meer & (top >= WY)
    lab, n = ndimage.label(land)
    rand = set(np.unique(np.concatenate([lab[0, :], lab[-1, :], lab[:, 0], lab[:, -1]])))
    kruin = blad.any(axis=1) | stam.any(axis=1)
    stenen = [j for j, k in enumerate(kaal) if "knuffelsteen" in k]
    eiland_kolommen, groot, klein, keien = 0, [], 0, 0
    for i in range(1, n + 1):
        if i in rand:
            continue
        sel = lab == i
        grootte = int(sel.sum())
        eiland_kolommen += grootte
        xs, zs = np.nonzero(sel)
        if np.isin(bodem[sel], stenen).mean() > 0.6:
            keien += 1
        elif grootte >= 150:
            vlak = sel & (top == WY + 2)
            vrij = vlak & ~kruin
            best, dp = 0, np.zeros((X + 1, Z + 1), int)
            for x in range(xs.min(), xs.max() + 1):
                for z in range(zs.min(), zs.max() + 1):
                    if vrij[x, z]:
                        dp[x + 1, z + 1] = 1 + min(dp[x, z], dp[x, z + 1], dp[x + 1, z])
                        best = max(best, int(dp[x + 1, z + 1]))
            groot.append(f"[{x0 + int(xs.mean())} {z0 + int(zs.mean())}: {xs.max() - xs.min() + 1}x{zs.max() - zs.min() + 1}, {grootte} columns, flat {int(vlak.sum())}, "
                         f"under a crown {100 * int((sel & kruin).sum()) // grootte}%, flat and free of crown {int(vrij.sum())}, free square {best}, "
                         f"top y {int(top[sel].max())}]")
        else:
            klein += 1
    binnen = int(meer.sum()) + eiland_kolommen
    print(f"  islands inside the box: large {len(groot)}, small {klein}, boulders and stepping stones {keien}; open water "
          f"{100.0 * meer.sum() / max(1, binnen):.1f}% of water + islands")
    for g in groot:
        print("    " + g)
    # things in the wrong place
    onder = np.zeros_like(plant)
    onder[:, 1:, :] = grond[:, :-1, :]
    eigen_onder = np.zeros_like(plant)
    eigen_onder[:, 1:, :] = plant[:, :-1, :]
    fout_plant = plant & ~onder & ~eigen_onder                 # a land plant not on ground (nor on its own lower half)
    fout_plant[:, 0, :] = False                                # (the box's lowest layer: what is under it is not read)
    drijf = drijf & (ys + y0 == WY + 1) & meer[:, None, :]      # (only what floats on the lake; the valley's ponds are not ours)
    nat_plant = plant & (ys + y0 <= WY) & meer[:, None, :]
    op_bron = np.zeros_like(drijf)
    op_bron[:, 1:, :] = waterbron[:, :-1, :]
    bodemplant = is_("seagrass", "tall_seagrass", "kaaskoraal", "reuzenschelp")
    print(f"  land plants: {int(plant.sum())} blocks; not on ground {int(fout_plant.sum())}; in the lake's water {int(nat_plant.sum())}")
    print(f"  on the water: petals {int(is_('drijvende_bloesemblaadjes').sum())}, lilies {int(is_('guh_waterlelie', 'lily_pad').sum())}; "
          f"not on a water source {int((drijf & ~op_bron).sum())}")
    print(f"  under water: seagrass {int(is_('seagrass').sum())}, kaaskoraal {int(is_('kaaskoraal').sum())}, reuzenschelp {int(is_('reuzenschelp').sum())}; "
          f"not on the bed {int((bodemplant & ~onder).sum())}")
    print(f"  bloesemriet {int(is_('bloesemmeertje_riet').sum()) // 2} plants, flowers {int(is_('roze_guhbloem', 'knabbelroos', 'roze_hibiscus').sum())}, "
          f"roze gras {int(is_('roze_gras').sum())}")
    # trees: wood hangs together with wood that stands on ground (or is a knot inside a crown); every crown hangs on a grounded trunk
    slab, sn = ndimage.label(stam, structure=kubus)
    opgrond = np.zeros_like(stam)
    opgrond[:, 1:, :] = stam[:, 1:, :] & grond[:, :-1, :]
    vast = set(np.unique(slab[opgrond])) - {0}
    blab, bn = ndimage.label(blad | stam, structure=kubus)
    met_grond = set(np.unique(blab[opgrond])) - {0}
    los_hout, losse_kruin = 0, 0

    def binnen_box(sel):
        # (a tree cut by the box's edge can look loose: only count what lies well inside)
        xs, _, zs = np.nonzero(sel)
        return xs.min() > 12 and xs.max() < X - 12 and zs.min() > 12 and zs.max() < Z - 12
    for i in range(1, sn + 1):
        if i not in vast:
            sel = slab == i
            if binnen_box(sel) and not (set(np.unique(blab[sel])) & met_grond):
                los_hout += 1
    for i in range(1, bn + 1):
        if i not in met_grond and binnen_box(blab == i):
            losse_kruin += 1
    verval = sum(int((vol == i).sum()) for i, p in enumerate(pal) if "guhbloesem_leaves" in p and "distance=7" in p and "persistent=false" in p)
    blijvend = sum(int((vol == i).sum()) for i, p in enumerate(pal) if "guhbloesem_leaves" in p and "persistent=true" in p)
    print(f"  trees: {len(vast)} trunks on the ground, {int(stam.sum())} logs, {int(blad.sum())} leaves; wood not joined to a grounded tree {los_hout}; "
          f"crowns not joined to a grounded trunk {losse_kruin}; leaves that would decay {verval}; persistent leaves {blijvend}")
    if blad.any():
        laag = np.where(blad, ys + y0, 999).min(axis=1)
        print(f"  lowest leaf: {int(laag.min()) - WY} blocks above the water level; leaves per crown column: {blad.sum() / blad.any(axis=1).sum():.1f}")

    # --- pictures ---
    S = 3
    boven, dm = Image.new("RGB", (X, Z)), Image.new("RGB", (X, Z))
    hoogste = np.where(~lucht, ys, -1).max(axis=1)
    for x in range(X):
        for z in range(Z):
            d = int(diepte[x, z])
            if meer[x, z]:
                k = onder_water(KLEUR.get(kaal[bodem[x, z]], (120, 120, 120)), d)
                dm.putpixel((x, z), (255 - d * 28, 255 - d * 22, 255 - d * 8))
            else:
                k = KLEUR.get(kaal[bodem[x, z]], (200, 150, 170))
                t = int(top[x, z]) - WY
                k = tuple(min(255, c + 5 * max(0, min(t, 6))) for c in k)
                dm.putpixel((x, z), (70, 70, 70))
            h = int(hoogste[x, z])
            if h >= 0:
                bn_ = kaal[vol[x, h, z]]
                if bn_ in KLEUR and bn_ not in ("minecraft:sand", "minecraft:pink_wool") and (h + y0 > WY or not meer[x, z]) and h + y0 > top[x, z]:
                    kk = KLEUR[bn_]
                    if "leaves" in bn_:
                        sch = 0.8 + 0.035 * min(8, max(0, h + y0 - WY - 4))
                        kk = tuple(int(min(255, c * sch)) for c in kk)
                    k = kk
                elif any(p in bn_ for p in ("guhbloem", "knabbelroos", "hibiscus")):
                    k = (250, 90, 150)
                elif "roze_gras" in bn_:
                    k = (150, 70, 110)
            boven.putpixel((x, z), k)
    boven.resize((X * S, Z * S), Image.NEAREST).save(uit + "_boven.png")
    dm.resize((X * S, Z * S), Image.NEAREST).save(uit + "_diepte.png")
    print("  wrote", uit + "_boven.png", uit + "_diepte.png")
    if render:
        teken3d(vol, pal, y0, uit, render)


def teken3d(vol, pal, y0, uit, deel):
    """deel: "x0,z0,x1,z1" (offsets inside the box) or "alles"."""
    import zipfile
    hier = os.path.dirname(os.path.abspath(__file__))
    wt = os.path.abspath(os.path.join(hier, "..", ".."))
    root = os.path.abspath(os.path.join(wt, ".."))
    sys.path.insert(0, os.path.join(root, "base-ontwerpen", "_bron"))
    sys.path.insert(0, os.path.join(root, "base-ontwerpen", "vibes", "_bron"))
    import mcrender
    import vibekit
    map_uit = os.path.dirname(os.path.abspath(uit))
    pak = os.path.join(map_uit, "_assets_meer.zip")
    assets = os.path.join(wt, "src", "main", "resources", "assets")
    with zipfile.ZipFile(pak, "w") as z:
        for sub in ("blockstates", "models/block", "textures/block"):
            d = os.path.join(assets, "guhs", sub)
            for f in sorted(os.listdir(d)):
                if f.startswith(("bloesemmeertje", "drijvende_bloesemblaadjes", "gladde_knuffelsteen")):
                    z.write(os.path.join(d, f), f"assets/guhs/{sub}/{f}")
    vibekit.JARS.insert(0, pak)
    vibekit.OUT = map_uit
    mcrender.WATER = WATER   # the biome's water colour
    orig = mcrender.Baker.bake

    def bake(self, state):
        # (as in the first-round sketch: only the water's surface is drawn, so the bed shows through; the cut edge is whole)
        if state in self.cache:
            return self.cache[state]
        F = orig(self, state)
        if state.startswith("minecraft:water"):
            if "top=1" in state:
                F.quads = [q for q in F.quads if np.allclose(np.asarray(q[0])[:, 1], 14 / 16.0)]
            elif "in=1" in state:
                F.quads = []
        return F
    mcrender.Baker.bake = bake
    if deel != "alles":
        a, b, c, d = (int(v) for v in deel.split(","))
        vol = vol[a:c, :, b:d]
    lo = max(0, 41 - y0)
    vol = vol[:, lo:lo + 34, :].copy()
    S = vibekit.Scene(1, 1, 1)
    S.pal = ["minecraft:water[level=0]" if p.startswith("minecraft:water") else p for p in pal]
    S.idx = {}
    for i, p in enumerate(S.pal):
        S.idx.setdefault(p, i)
    S.v = vol
    isw = np.isin(vol, [i for i, p in enumerate(S.pal) if p == "minecraft:water[level=0]"])
    boven, binnen = S.id("minecraft:water[level=0,top=1]"), S.id("minecraft:water[level=0,in=1]")
    surf = isw.copy()
    surf[:, :-1, :] &= ~isw[:, 1:, :]
    edge = np.zeros_like(isw)
    edge[-1, :, :] = True
    edge[:, :, -1] = True
    vol[isw & ~edge] = binnen
    vol[surf & ~edge] = boven
    naam = os.path.basename(uit)
    vibekit.render(S, naam + "_3d", yaw=35, pitch=30, scale=14)
    vibekit.render(S, naam + "_3d_laag", yaw=35, pitch=20, scale=14)
