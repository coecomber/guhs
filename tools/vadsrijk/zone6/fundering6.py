"""X-KAART: foundations for zone 6 outside the towns (user feedback round X: "In zone 6 sluiten veel gebouwen niet goed aan op de
grond of laten ze dingen zweven ... voornamelijk in de zones buiten de stadjes").

maak6 calls:
    terrein = terrein_top(W)       right after the land + the roads: the ground height of every column (incl. water)
    bouw(W, terrein, log)          at the very end of the world build (after landmarks, green, figures, vul_putjes)
and saves the ground heights next to the world cache (_cache/z6/terrein.npz) for tools/vadsrijk/funderingcheck.py.

The rule (the same as funderingcheck's GAT): per column, b = the lowest BUILT block above the ground T (anything solid that is
not terrain-land, not a plant/leaf/carpet/lantern/sign); it is the bottom of its structure when no column in the 5x5 around it
has a lower b (so roof eaves, arches over passages and balconies never count). When 1 <= b - T - 1 <= 6 the gap is filled:
  - under a plain full block (a wall, a tent cloth, a trunk): the same block, so the wall goes down into the ground;
  - under anything else (a stair, slab, fence, door, gold trim, glass): the ground's own block (sand, rock, grass), so the ground
    rises to meet it (slope blending).
Never in the town boxes (plan6.GEBIEDEN stad=True), never over water, never where the ground was dug away later (a stairwell).
Also: no flowers/grass left inside the filled cells (they are overwritten), and an extra ground cell under a door that would
otherwise have a step of 2+ in front of it.
"""
import numpy as np
from scipy import ndimage

import plan6 as PL

MAX_GAT = 8          # (the checker's GAT is <= 6; 7..8 are ZWEEFT there and get a foundation too)
_ZACHT = ("air", "water", "lava", "leaves", "grass", "fern", "flower", "tulip", "allium", "daisy", "bluet", "poppy", "dandelion",
          "cornflower", "orchid", "lily", "petals", "rose", "lilac", "peony", "sapling", "carpet", "lantern", "lampion", "torch",
          "sign", "banner", "vine", "chain", "candle", "button", "lever", "rail", "pressure_plate", "mushroom", "bush", "kelp",
          "seagrass", "pickle", "web", "snow[", "_head", "skull", "hibiscus", "guhbloem", "guhoortjes", "riet", "glimplant",
          "glimpaddenstoel", "dripleaf", "spore", "azalea", "berries", "wildflowers", "leaf_litter", "light[", "structure_void",
          "barrier", "_rod", "bell", "pot", "vadstouw", "vlaggetjes", "slingers", "tripwire", "_blad", "palm_kroon", "lantaarn",
          "lampje", "cactus", "sugar_cane", "bamboo", "cocoa", "dead_bush", "scaffolding")
_NIET_KOPIE = ("half=", "type=", "facing=", "_fence", "_wall", "door", "pane", "glass", "glas", "lamp", "lantaarn", "froglight",
               "gold_block", "goud", "kroonlijst", "kussen", "_bed", "chest", "barrel", "slab", "stairs", "trap", "plaat",
               "hek", "tralie", "ruit", "kaars", "mozaiek", "spiegel", "kristal")


def _zacht(s):
    return any(w in s for w in _ZACHT)


def terrein_top(W):
    """(X, Z) int16: the y (game) of the highest non-air block per column now (= the land + roads + water), -1 = none."""
    X, Y, Z = W.v.shape
    T = np.full((X, Z), -1, np.int16)
    for i0 in range(0, X, 64):
        nz = W.v[i0:i0 + 64] != 0
        heeft = nz.any(axis=1)
        top = (Y - 1) - np.argmax(nz[:, ::-1, :], axis=1)
        T[i0:i0 + 64] = np.where(heeft, top + int(W.b[1]), -1)
    return T


def _stad_masker(W):
    X, _, Z = W.v.shape
    xs = np.arange(X) + int(W.b[0])
    zs = np.arange(Z) + int(W.b[2])
    m = np.zeros((X, Z), bool)
    for g in PL.GEBIEDEN:
        if g[8]:
            m |= ((xs[:, None] >= g[1]) & (xs[:, None] <= g[3])) & ((zs[None, :] >= g[2]) & (zs[None, :] <= g[4]))
    return m


def bodem(W, T):
    """Per column: b (game y of the lowest built block above T, 10**6 = none), the class luts."""
    X, Y, Z = W.v.shape
    gebouwd = np.array([not _zacht(p) for p in W.pal], bool)
    gebouwd[0] = False
    b = np.full((X, Z), 10 ** 6, np.int32)
    y1 = int(W.b[1])
    Yr = np.arange(Y)[None, :, None] + y1
    for i0 in range(0, X, 64):
        sl = gebouwd[W.v[i0:i0 + 64]] & (Yr > T[i0:i0 + 64, None, :])
        heeft = sl.any(axis=1)
        b[i0:i0 + 64] = np.where(heeft, np.argmax(sl, axis=1) + y1, 10 ** 6)
    return b


def bouw(W, T, log=print):
    X, Y, Z = W.v.shape
    y1 = int(W.b[1])
    water = np.array([p.startswith("minecraft:water") for p in W.pal], bool)
    b = bodem(W, T)
    base = ndimage.minimum_filter(np.where(T >= 0, b, 10 ** 6), size=5, mode="nearest")
    gap = b - T.astype(np.int32) - 1
    stad = _stad_masker(W)
    weg = np.zeros((X, Z), bool)                       # road columns (+-1): an arch/bridge over a road keeps its passage
    for prof in getattr(W, "wegen", {}).values():
        for (x, z, _y) in prof:
            i, k = int(x) - int(W.b[0]), int(z) - int(W.b[2])
            weg[max(0, i - 2):i + 3, max(0, k - 2):k + 3] = True
    stam = np.array([("_stam" in p or "_log" in p) for p in W.pal], bool)   # a floating palm/tree trunk: down to the ground
    X0, Z0 = int(W.b[0]), int(W.b[2])
    bi = np.clip(b - y1, 0, Y - 1)
    is_stam = stam[np.take_along_axis(W.v, bi[:, None, :].astype(np.intp), axis=1)[:, 0, :]]
    toren = np.zeros((X, Z), bool)                     # the Sterrendovende Toren's rings and lens float on purpose
    for g in PL.GEBIEDEN:
        if g[0] == "z6_toren":
            toren[max(0, g[1] - X0):g[3] - X0 + 1, max(0, g[2] - Z0):g[4] - Z0 + 1] = True
    grens = np.where(is_stam, 16, MAX_GAT)
    kand = ((T >= 0) & (b < 10 ** 6) & (gap >= 1) & (gap <= grens) & (b <= base) & ~stad & ~(weg & (gap >= 2))
            & ~(toren & (gap > MAX_GAT)))
    ii, kk = np.nonzero(kand)
    n_kol = n_blok = n_water = n_gegraven = 0
    for i, k in zip(ii.tolist(), kk.tolist()):
        t, bb = int(T[i, k]), int(b[i, k])
        grond_id = int(W.v[i, t - y1, k])
        if water[grond_id]:
            n_water += 1
            continue
        if grond_id == 0:
            n_gegraven += 1                            # the ground cell itself was dug away later: leave it
            continue
        top_s = W.pal[int(W.v[i, bb - y1, k])]
        if not any(w in top_s for w in _NIET_KOPIE):
            vul = int(W.v[i, bb - y1, k])
        else:
            g_s = W.pal[grond_id]
            vul = W.id("guhs:roze_zand") if any(w in g_s for w in ("stairs", "slab", "trap", "plaat")) else grond_id
        cel = W.v[i, t + 1 - y1:bb - y1, k]
        vrij = np.array([not (c and not _zacht(W.pal[int(c)])) for c in cel.tolist()], bool)
        cel[vrij] = vul
        n_kol += 1
        n_blok += int(vrij.sum())
    n_deur = deur_stoepjes(W, stad)
    n_los = losse_brokjes(W, stad)
    log(f"fundering: {n_los} losse zwevende blokjes weg")
    log(f"fundering: {n_kol} kolommen, {n_blok} blokken bijgevuld (over water overgeslagen {n_water}, gegraven {n_gegraven}), "
        f"{n_deur} deurstoepjes")
    return n_kol


def _grond_y(W, i, y, k, diep=8):
    """The y of the first non-soft block at or below y in column (i, k) (local index), or None within diep."""
    y1 = int(W.b[1])
    for yy in range(y, y - diep - 1, -1):
        c = int(W.v[i, yy - y1, k])
        if c and not _zacht(W.pal[c]):
            return yy
    return None


def deur_stoepjes(W, stad):
    """Every door outside the towns: on both sides, the cell in front gets ground up to the threshold (y - 1) when it hangs
    (a landing), and the cell after it ground up to y - 2 when that is lower still (one step), so no door opens onto a 2+ drop."""
    X, Y, Z = W.v.shape
    y1 = int(W.b[1])
    deur = np.array([("door" in p and "half=lower" in p and "trapdoor" not in p) for p in W.pal], bool)
    n = 0
    for i0 in range(0, X, 64):
        ii, yy, kk = np.nonzero(deur[W.v[i0:i0 + 64]])
        for i, j, k in zip((ii + i0).tolist(), yy.tolist(), kk.tolist()):
            if stad[i, k]:
                continue
            s = W.pal[int(W.v[i, j, k])]
            f = s.split("facing=")[1].split(",")[0].rstrip("]") if "facing=" in s else "north"
            dx, dz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[f]
            y = j + y1
            for sgn in (1, -1):
                for stap, doel in ((1, y - 1), (2, y - 2)):
                    i2, k2 = i + sgn * dx * stap, k + sgn * dz * stap
                    if not (0 <= i2 < X and 0 <= k2 < Z):
                        break
                    if any(int(W.v[i2, yy2 - y1, k2]) and not _zacht(W.pal[int(W.v[i2, yy2 - y1, k2])])
                           for yy2 in (y, y + 1) if stap == 1):
                        break                          # a wall / furniture in front: an inside door, leave it
                    g = _grond_y(W, i2, doel, k2)
                    if g is None or g >= doel:
                        continue
                    vul = int(W.v[i2, g - y1, k2])
                    for yy2 in range(g + 1, doel + 1):
                        W.v[i2, yy2 - y1, k2] = vul
                    n += 1
    return n


def losse_brokjes(W, stad):
    """A lone block with nothing at all around it (26 neighbours air), outside the towns: a crumb of rock or trim left in the
    air by an earlier cut. Removed."""
    X, Y, Z = W.v.shape
    zacht = np.array([_zacht(p) for p in W.pal], bool)
    n = 0
    for i0 in range(0, X, 64):
        a, b_ = max(0, i0 - 1), min(X, i0 + 65)
        sub = W.v[a:b_]
        vast = (sub != 0) & ~zacht[sub]
        bezet = ndimage.convolve((sub != 0).astype(np.uint8), np.ones((3, 3, 3), np.uint8), mode="constant") - (sub != 0)
        los = vast & (bezet == 0)
        ii, jj, kk = np.nonzero(los)
        for i, j, k in zip(ii.tolist(), jj.tolist(), kk.tolist()):
            gi = i + a
            if not (i0 <= gi < i0 + 64) or stad[gi, k] or j == 0:
                continue
            W.v[gi, j, k] = 0
            n += 1
    return n

