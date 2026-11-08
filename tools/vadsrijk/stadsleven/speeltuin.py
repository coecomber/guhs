"""U-LEVEN-STAD: a cute pink playground (speeltuin) in every hub town, stamped into the written map tiles.

WHY a pass over the tiles (like verbind.py) and not inside every zone generator: the seven hub towns come from six different
generators (kamp, stad, zone2, zone3, zone4, zone5, zone 6 in its own branch). One deterministic pass finds a free, flat spot
near the town centre in the REAL tiles and stamps the same playground everywhere, so a regen of any zone gets it back.

  python tools/vadsrijk/stadsleven/speeltuin.py                 every town whose tiles exist
  python tools/vadsrijk/stadsleven/speeltuin.py --stad zwamzoet  one town
  python tools/vadsrijk/stadsleven/speeltuin.py --droog          only search + print (writes nothing)

maak_kaart.py runs it after the zone makers and BEFORE the connection pass (so the fences come out connected). Run on its
own it calls the connection pass itself on the tiles it wrote (unless GUHS_VERBIND_LATER=1).
Output: the tiles + src/main/resources/data/guhs/vadsrijk_stadsleven/speeltuinen.txt (one line per town: the corner of the
footprint and the floor y). The Java side (feature/vadsrijk/stadsleven/Speeltuinen) reads that file; the equipment offsets
below are mirrored in Speeltuinen.java (keep them in sync).

Walkability: the spot is flat (every footprint column the same floor height), the ring around it is within one step, the
four 3-wide openings in the fence lead out at +-1, nothing is built on a published stand spot (POI_*.md), the slide can be
walked up in half steps, the ladder goes up the tower, the sand pit is flush with the floor.

Layout (x 0..12 east, z 0..9 south, F = floor y; standing level F+1):
  ring  cherry fence at the edge, openings x 5..7 (north/south) and z 4..5 (west/east), lanterns on the 4 corner posts
  glijbaan  ladder x1 z1 (F+1..F+2, facing west), tower pink concrete x2 z1 (F+1..F+2), slide cherry x3..5 z1
            (x3 stairs F+2 facing west, x4 top slab F+1, x5 bottom slab F+1), landing x6
  wipwap    cherry fence post x9 z2 (F+1): pivot (9.5, F+2, 2.5), plank along x (the moving part = an entity)
  schommels posts x8 / x11 z7 (F+1..F+3), beam x8..11 z7 (F+4); two swings hang at x 9.5 and 10.5 (entities)
  draaimolen  centre (3.5, F+1, 6.5), r 1.4 (entity)
  zandbak   sand x5..7 z6..8 at F (flush)
  bankjes   cherry stairs facing south at x1, x2 on z8 (F+1)
The spot may be levelled: footprint columns at F-1 or F-2 get soil + the floor on top, natural ground at F+1 is dug away.
"""
import argparse
import os
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
VADSRIJK = os.path.dirname(HIER)
ROOT = os.path.abspath(os.path.join(VADSRIJK, "..", ".."))
sys.path.insert(0, VADSRIJK)
import verbind as VB  # noqa: E402

DATA = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "vadsrijk_stadsleven", "speeltuinen.txt")
T = VB.T
HOOG = VB.HOOG
W, D = 13, 10            # footprint (x, z)
VRIJ = 6                 # the clear box above the floor (plants in it are removed)
HOOFD = 5                # clear cells a floor needs above it (the swing beam is at F+4)

# town: (zone, centre x, y (stand), z, box minx, minz, maxx, maxz)  -- the kern areas / POI_*.md
STEDEN = {
    "knuffelkamp": (1, -384, 87, 1025, -512, 900, -256, 1150),
    "zwamzoet": (1, 0, 64, 59, -160, -160, 160, 160),
    "kandijkom": (2, 704, 91, -1068, 620, -1140, 788, -856),
    "honingtak": (3, 1396, 167, -525, 1300, -680, 1500, -420),
    "lampionhaven": (4, 1848, 67, -467, 1790, -530, 1910, -410),
    "hoogvads": (5, 2860, 212, -515, 2694, -545, 2907, -434),
    "saffraanpoort": (6, 3762, 71, -850, 3634, -950, 3890, -742),
}

PLANT = ("short_grass", "tall_grass", "fern", "dandelion", "poppy", "tulip", "orchid", "allium", "azure_bluet", "oxeye_daisy",
         "cornflower", "lily_of_the_valley", "pink_petals", "wildflowers", "leaf_litter", "bush", "sapling", "dead_bush",
         "sweet_berry", "torchflower", "pitcher", "short_dry_grass", "tall_dry_grass", "firefly_bush")
NIET_VLOER = ("leaves", "fence", "wall", "pane", "_bars", "slab", "stairs", "door", "trapdoor", "carpet", "lantern",
              "torch", "sign", "water", "lava", "glass", "chain", "button", "plate", "rail", "bed", "candle", "flower_pot",
              "campfire", "barrel", "chest", "ladder", "vine", "path", "geharkt", "farmland", "scaffold", "kelp", "seagrass",
              "lily_pad", "cake", "skull", "head", "banner", "anvil", "bell", "vlaggetjes", "slinger", "vadstouw")


def kaal(s):
    return s.partition("[")[0]


def wisbaar(naam):
    """Air, or a small plant the stamp may replace with air."""
    if naam in VB.LUCHT:
        return True
    n = naam.split(":")[-1]
    return any(p in n for p in PLANT) and "grass_block" not in n


def vloer(naam):
    if naam in VB.LUCHT:
        return False
    n = naam.split(":")[-1]
    return n != "snow" and not any(p in n for p in NIET_VLOER)


def verhard(naam):
    """Paving (plazas, streets): allowed, but a lawn is preferred."""
    n = naam.split(":")[-1]
    return any(p in n for p in ("klinker", "tegel", "cobble", "brick", "stone", "terracotta", "concrete", "planks", "marmer", "tile"))


def zacht(naam):
    """Natural ground that may be dug away one block when levelling."""
    n = naam.split(":")[-1]
    return any(p in n for p in ("grass_block", "dirt", "gras", "moss_block", "sand", "snow_block", "podzol", "mud", "clay"))


# ---------------------------------------------------------------------------------------------------------- the stamp
def ontwerp():
    """[(dx, dy, dz, state)] relative to the footprint corner at floor y (dy 0 = the floor)."""
    b = []
    roze, wit = "minecraft:pink_concrete", "minecraft:white_concrete"
    for x in range(W):
        for z in range(D):
            rand = x in (0, W - 1) or z in (0, D - 1)
            b.append((x, 0, z, wit if rand or (x + 2 * z) % 6 == 0 else roze))
    for x in range(5, 8):
        for z in range(6, 9):
            b.append((x, 0, z, "minecraft:sand"))
    # the fence ring with its four openings + lanterns on the corners
    for x in range(W):
        for z in range(D):
            if not (x in (0, W - 1) or z in (0, D - 1)):
                continue
            if z in (0, D - 1) and 5 <= x <= 7:
                continue
            if x in (0, W - 1) and 4 <= z <= 5:
                continue
            b.append((x, 1, z, "minecraft:cherry_fence"))
    for x, z in ((0, 0), (W - 1, 0), (0, D - 1), (W - 1, D - 1)):
        b.append((x, 2, z, "minecraft:lantern[hanging=false,waterlogged=false]"))
    # glijbaan
    for y in (1, 2):
        b.append((1, y, 1, "minecraft:ladder[facing=west,waterlogged=false]"))
        b.append((2, y, 1, "minecraft:pink_concrete"))
    b.append((3, 2, 1, "minecraft:cherry_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]"))
    b.append((4, 1, 1, "minecraft:cherry_slab[type=top,waterlogged=false]"))
    b.append((5, 1, 1, "minecraft:cherry_slab[type=bottom,waterlogged=false]"))
    # wipwap foot
    b.append((9, 1, 2, "minecraft:cherry_fence"))
    # schommels: two posts + the beam
    for y in (1, 2, 3):
        b.append((8, y, 7, "minecraft:cherry_fence"))
        b.append((11, y, 7, "minecraft:cherry_fence"))
    for x in range(8, 12):
        b.append((x, 4, 7, "minecraft:cherry_fence"))
    # bankjes (backs to the south fence, looking at the playground)
    for x in (1, 2):
        b.append((x, 1, 8, "minecraft:cherry_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]"))
    return b


# ---------------------------------------------------------------------------------------------------------- the search
class Gebied:
    """The town box as dense columns: top (highest non-clearable block in the band), its block, clear-above."""

    def __init__(self, TG, stad):
        zone, cx, cy, cz, x0, z0, x1, z1 = STEDEN[stad]
        self.stad, self.c = stad, (cx, cy, cz)
        self.x0, self.z0 = x0, z0
        nx, nz = x1 - x0 + 1, z1 - z0 + 1
        ylo, yhi = max(1, cy - 14), min(HOOG - 1, cy + 30)
        self.top = np.full((nx, nz), -999, np.int32)
        self.naam = np.zeros((nx, nz), object)
        self.ok = np.zeros((nx, nz), bool)
        self.pad = np.zeros((nx, nz), bool)
        self.zacht = np.zeros((nx, nz), bool)
        self.hard = np.zeros((nx, nz), bool)
        self.waarom = {}
        self.tegels = set()
        lut_w = np.zeros(0, bool)
        lut_v = np.zeros(0, bool)
        lut_p = np.zeros(0, bool)
        lut_h = np.zeros(0, bool)
        lut_z = np.zeros(0, bool)
        for tx in range(x0 // T, x1 // T + 1):
            for tz in range(z0 // T, z1 // T + 1):
                v = TG.dicht((tx, tz))
                if v is None:
                    continue
                self.tegels.add((tx, tz))
                if len(lut_w) < len(TG.pal):
                    lut_w = np.array([wisbaar(kaal(s)) for s in TG.pal], bool)
                    lut_v = np.array([vloer(kaal(s)) for s in TG.pal], bool)
                    lut_p = np.array([("path" in s or "gravel" in s) for s in TG.pal], bool)
                    lut_h = np.array([verhard(kaal(s)) for s in TG.pal], bool)
                    lut_z = np.array([zacht(kaal(s)) for s in TG.pal], bool)
                ax0, az0 = max(x0, tx * T), max(z0, tz * T)
                ax1, az1 = min(x1, tx * T + T - 1), min(z1, tz * T + T - 1)
                blok = v[ax0 - tx * T:ax1 - tx * T + 1, ylo:yhi + 1, az0 - tz * T:az1 - tz * T + 1]
                vast = ~lut_w[blok]                                  # (x, y, z)
                # the floor = the highest solid block with HOOFD clear cells above it (garlands, lamps and crowns higher up
                # do not count as the ground)
                run = np.zeros(vast.shape, np.int16)
                n = vast.shape[1]
                run[:, n - 1, :] = ~vast[:, n - 1, :]
                for yy in range(n - 2, -1, -1):
                    run[:, yy, :] = np.where(vast[:, yy, :], 0, run[:, yy + 1, :] + 1)
                staan = np.zeros(vast.shape, bool)
                staan[:, :n - 1, :] = vast[:, :n - 1, :] & (run[:, 1:, :] >= HOOFD)
                heeft = staan.any(axis=1)
                i = n - 1 - np.argmax(staan[:, ::-1, :], axis=1)
                y = np.where(heeft, ylo + i, -999)
                s = np.take_along_axis(blok, i[:, None, :], axis=1)[:, 0, :]
                gx0, gz0 = ax0 - x0, az0 - z0
                sl = (slice(gx0, gx0 + blok.shape[0]), slice(gz0, gz0 + blok.shape[2]))
                self.top[sl] = y
                self.ok[sl] = heeft & lut_v[s] & (np.abs(y + 1 - cy) <= 10) & (y + VRIJ <= yhi)
                self.pad[sl] = heeft & lut_p[s]
                self.hard[sl] = heeft & lut_h[s]
                self.zacht[sl] = heeft & lut_z[s]
                for a in range(blok.shape[0]):
                    for b in range(blok.shape[2]):
                        self.naam[gx0 + a, gz0 + b] = TG.pal[int(s[a, b])]

    def zoek(self, verboden, ruim=False):
        nx, nz = self.top.shape
        beste = None
        cx, cy, cz = self.c
        if nx < W + 2 or nz < D + 2:
            return None
        from numpy.lib.stride_tricks import sliding_window_view as sw
        okv = sw(self.ok, (W, D)).all(axis=(2, 3))
        tv = sw(self.top, (W, D))
        lo, hi = tv.min(axis=(2, 3)), tv.max(axis=(2, 3))
        vlak = okv & (hi - lo <= 3)
        for gx, gz in zip(*np.nonzero(vlak)):
            gx, gz = int(gx), int(gz)
            if gx < 1 or gz < 1 or gx + W >= nx or gz + D >= nz:
                continue
            t = self.top[gx:gx + W, gz:gz + D]
            waarden, aantal = np.unique(t, return_counts=True)
            y0 = int(waarden[np.argmax(aantal)])
            if (t < y0 - 2).any() or (t > y0 + 1).any():
                continue
            if not ruim and not self.zacht[gx:gx + W, gz:gz + D][t == y0 + 1].all():
                self.waarom["graven"] = self.waarom.get("graven", 0) + 1
                continue                         # only natural ground is dug away
            if ruim and self.hard[gx:gx + W, gz:gz + D].mean() > 0.5:
                continue                         # (the fallback may dig a paved step, but never across a whole street)
            if self.pad[gx:gx + W, gz:gz + D].mean() > 0.15:
                self.waarom["pad"] = self.waarom.get("pad", 0) + 1
                continue
            ring = np.concatenate([self.top[gx - 1, gz - 1:gz + D + 1], self.top[gx + W, gz - 1:gz + D + 1],
                                   self.top[gx:gx + W, gz - 1], self.top[gx:gx + W, gz + D]])
            if (np.abs(ring - y0) <= 1).mean() < 0.85:
                self.waarom["rand"] = self.waarom.get("rand", 0) + 1
                continue
            uit = [self.top[gx + 6, gz - 1], self.top[gx + 6, gz + D], self.top[gx - 1, gz + 4], self.top[gx + W, gz + 4]]
            if sum(abs(int(u) - y0) <= 1 for u in uit) < 3:
                self.waarom["ingang"] = self.waarom.get("ingang", 0) + 1
                continue
            wx, wz = self.x0 + gx, self.z0 + gz
            if any(wx - 1 <= px <= wx + W and wz - 1 <= pz <= wz + D and y0 - 1 <= py <= y0 + VRIJ for (px, py, pz) in verboden):
                self.waarom["poi"] = self.waarom.get("poi", 0) + 1
                continue
            d = ((wx + W / 2 - cx) ** 2 + (wz + D / 2 - cz) ** 2) ** 0.5
            # lawns before plazas and streets (a playground is not put in the middle of the market square)
            score = d + 6 * abs(y0 + 1 - cy) + 2 * int((t != y0).sum()) + 80 * float(self.hard[gx:gx + W, gz:gz + D].mean())
            if beste is None or score < beste[0]:
                beste = (score, wx, y0, wz)
        return beste


# ---------------------------------------------------------------------------------------------------------- write
def lees_data():
    uit = {}
    if os.path.exists(DATA):
        with open(DATA, encoding="utf-8") as f:
            for r in f:
                d = r.split()
                if len(d) == 5 and d[0] == "speeltuin":
                    uit[d[1]] = (int(d[2]), int(d[3]), int(d[4]))
    return uit


def schrijf_data(plekken):
    os.makedirs(os.path.dirname(DATA), exist_ok=True)
    with open(DATA, "w", encoding="utf-8", newline="\n") as f:
        f.write("# U-LEVEN-STAD: the playgrounds, GENERATED by tools/vadsrijk/stadsleven/speeltuin.py from the map tiles.\n")
        f.write("# speeltuin <town> <x> <floor y> <z>   (x/z = the north-west corner of the 13x10 footprint)\n")
        for stad in STEDEN:
            if stad in plekken:
                x, y, z = plekken[stad]
                f.write(f"speeltuin {stad} {x} {y} {z}\n")


def er_al(TG, plek):
    """Is our playground already stamped here (a re-run on the same tiles)?"""
    x, y, z = plek
    for (dx, dy, dz, s) in ((2, 1, 1, "minecraft:pink_concrete"), (9, 1, 2, "minecraft:cherry_fence"), (6, 0, 7, "minecraft:sand")):
        wx, wy, wz = x + dx, y + dy, z + dz
        v = TG.dicht((wx // T, wz // T))
        if v is None or kaal(TG.pal[int(v[wx % T, wy, wz % T])]) != s:
            return False
    return True


def stempel(TG, plek, gewijzigd, ruim=False):
    x0, y0, z0 = plek
    blokken = {}
    for x in range(-1, W + 1):          # the clear box (plants on the ring too, so nobody trips over a flower in the opening)
        for z in range(-1, D + 1):
            for y in range(1, VRIJ + 1):
                blokken[(x0 + x, y0 + y, z0 + z)] = None
    graaf = set()
    for x in range(W):
        for z in range(D):
            graaf.add((x0 + x, y0 + 1, z0 + z))
    for (dx, dy, dz, s) in ontwerp():
        blokken[(x0 + dx, y0 + dy, z0 + dz)] = s
    for x in range(W):                  # levelling: a column two below gets one block of soil under the floor
        for z in range(D):
            blokken.setdefault((x0 + x, y0 - 1, z0 + z), "vul")
    lucht = TG.id("minecraft:air")
    for (wx, wy, wz), s in blokken.items():
        t = (wx // T, wz // T)
        v = gewijzigd.get(t)
        if v is None:
            v = TG.dicht(t)
            if v is None:
                continue
            gewijzigd[t] = v = v.copy()
        cel = (wx % T, wy, wz % T)
        if s == "vul":
            if wisbaar(kaal(TG.pal[int(v[cel])])):
                v[cel] = TG.id("minecraft:dirt")
            continue
        if s is None:
            n = kaal(TG.pal[int(v[cel])])
            if wisbaar(n) or ((wx, wy, wz) in graaf and (zacht(n) or (ruim and vloer(n)))):
                v[cel] = lucht
        else:
            v[cel] = TG.id(s)


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--stad", default=None)
    ap.add_argument("--droog", action="store_true")
    ap.add_argument("--map", default=VB.UIT)
    a = ap.parse_args(argv)
    TG = VB._Tegels(a.map)
    oud = lees_data()
    plekken = dict(oud)
    verboden = VB.poi_cellen()
    gewijzigd = {}
    ruim_gebruikt = set()
    for stad in STEDEN:
        if a.stad and stad != a.stad:
            continue
        if stad in oud and er_al(TG, oud[stad]):
            print(f"speeltuin {stad}: staat er al op {oud[stad]}")
            continue
        G = Gebied(TG, stad)
        if not G.tegels:
            print(f"speeltuin {stad}: geen tegels (zone niet gebouwd), overgeslagen")
            continue
        b = G.zoek(verboden)
        if b is None:                       # fallback: a paved step inside the footprint may be dug away too
            b = G.zoek(verboden, ruim=True)
            ruim_gebruikt.add(stad)
        if b is None:
            print(f"speeltuin {stad}: GEEN vlakke vrije plek gevonden {G.waarom}", flush=True)
            plekken.pop(stad, None)
            continue
        _, wx, y0, wz = b
        plekken[stad] = (wx, y0, wz)
        print(f"speeltuin {stad}: {wx},{y0},{wz} (vloer {G.naam[wx - G.x0, wz - G.z0]}, "
              f"{((wx + W / 2 - G.c[0]) ** 2 + (wz + D / 2 - G.c[2]) ** 2) ** 0.5:.0f} blokken van het midden)", flush=True)
        if not a.droog:
            stempel(TG, plekken[stad], gewijzigd, ruim=stad in ruim_gebruikt)
    if a.droog:
        return
    for t, v in gewijzigd.items():
        oud_v = None
        p = TG.pad(t)
        palette, (xs, ys, zs, st) = TG.TL.lees(p)
        extra = {}
        if os.path.exists(p):
            ids = np.array([TG.id(s) for s in palette], np.uint16)
            oud_v = np.zeros((T, HOOG, T), np.uint16)
            ok = (xs >= 0) & (xs < T) & (ys >= 0) & (ys < HOOG) & (zs >= 0) & (zs < T)
            oud_v[xs[ok], ys[ok], zs[ok]] = ids[st[ok]]
            extra = {k: r for k, r in TG.nbt(t).items() if v[k] == oud_v[k] and v[k]}
        TG.schrijf(t, v, extra=extra)
    schrijf_data(plekken)
    print(f"speeltuinen: {len(gewijzigd)} tegels herschreven -> {os.path.relpath(DATA, ROOT)}")
    if gewijzigd and os.environ.get("GUHS_VERBIND_LATER") != "1":
        VB.na(sorted(gewijzigd))


if __name__ == "__main__":
    main()
