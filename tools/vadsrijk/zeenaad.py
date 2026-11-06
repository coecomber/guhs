"""U-OVERGANG: THE shared open sea between the zones (one model for the tiles and the runtime sea filler).

Mirror of src/main/java/nl/juiced/guhs/feature/vadsrijk/indeling/ZeeVulling.java (change BOTH). Every function is a pure
function of the world x/z, with the same integer hashing and the same double maths in Python and Java, so a tile edge and the
procedural sea chunk next to it get exactly the same floor height, floor block and cloud bank:

  vloer(X, Z)        the open-sea floor (top block y): 47..51, smooth value noise around 49;
  materiaal(X, Z)    0 = light_blue_terracotta, 1 = sand, 2 = gravel (patches);
  wolk(X, Z)         (thickness, kind) of the cloud sea: the zone-3 cloud banks (Wolkenkloof west, Wolkenval east) that used
                     to stop dead at the zone-3 tile box now thin out over ~70 blocks outside it (organic edge, in every
                     direction); inside the zone-3 box this is the old barriere.zee_z3 cloud field (with a wobbly outer edge of the kloof cliffs).

Used by: barriere.zee_z3 (zone-3 tiles), overgang.py (the seam pass over every zone's edge tiles), ZeeVulling (Java).
"""
import numpy as np

ZEE = 62
DIEP = 49
Z3_STAM = (1404, -576)
# the zone-3 tile box (world blocks, inclusive) = VadsrijkZones.Zone.Z3
Z3_BOX = (1024, -1024, 1727, -129)
WOLK = ["minecraft:white_wool", "minecraft:white_wool", "guhs:pluiswolblok", "minecraft:white_concrete_powder"]
MATERIAAL = ["minecraft:light_blue_terracotta", "minecraft:sand", "minecraft:gravel"]


def ruis2(X, Z, cel, seed):
    """Smooth 2D value noise in [0, 1) on world coordinates (deterministic; the same over every tile and in Java)."""
    X = np.asarray(X, np.float64)
    Z = np.asarray(Z, np.float64)
    gx, gz = np.floor(X / cel), np.floor(Z / cel)
    fx, fz = X / cel - gx, Z / cel - gz

    def h(a, b):
        with np.errstate(over="ignore"):
            v = (a.astype(np.int64) * 73856093) ^ (b.astype(np.int64) * 19349663) ^ np.int64(seed * 83492791)
            v = (v ^ (v >> 13)) * np.int64(1274126177)
            return ((v ^ (v >> 16)) & 0xFFFF) / 65536.0
    sx, sz = fx * fx * (3 - 2 * fx), fz * fz * (3 - 2 * fz)
    return (h(gx, gz) * (1 - sx) * (1 - sz) + h(gx + 1, gz) * sx * (1 - sz) + h(gx, gz + 1) * (1 - sx) * sz
            + h(gx + 1, gz + 1) * sx * sz)


def hash01(X, Z, seed):
    """A per-block hash in [0, 1) (dithering)."""
    with np.errstate(over="ignore"):
        a = np.asarray(X, np.float64).astype(np.int64)
        b = np.asarray(Z, np.float64).astype(np.int64)
        v = (a * 73856093) ^ (b * 19349663) ^ np.int64(seed * 83492791)
        v = (v ^ (v >> 13)) * np.int64(1274126177)
        return ((v ^ (v >> 16)) & 0xFFFF) / 65536.0


def vloer(X, Z):
    """Open-sea floor (top block y) at world x/z."""
    n = ruis2(X, Z, 56, 11)
    return (DIEP + np.floor((n - 0.5) * 5.0 + 0.5)).astype(np.int32)


def materiaal(X, Z):
    m = ruis2(X, Z, 23, 12)
    return np.where(m < 0.30, 2, np.where(m > 0.74, 1, 0)).astype(np.int32)


def kloof_x(Z):
    """Centre line of the Wolkenkloof (the open channel between the cloud banks of zone 2 and zone 3), world x at world z."""
    Z = np.asarray(Z, np.float64)
    return 1066 + 20 * np.sin(Z / 70.0) + 9 * np.sin(Z / 23.0 + 1.3)


def buiten_z3(X, Z):
    """Distance (blocks) outside the zone-3 tile box (0 inside)."""
    X = np.asarray(X, np.float64)
    Z = np.asarray(Z, np.float64)
    dx = np.maximum(np.maximum(Z3_BOX[0] - X, X - (Z3_BOX[2] + 1)), 0)
    dz = np.maximum(np.maximum(Z3_BOX[1] - Z, Z - (Z3_BOX[3] + 1)), 0)
    return np.hypot(dx, dz)


def wolk(X, Z):
    """(thickness int32 >= 0, kind int32 0..3) of the cloud sea at world x/z. Thickness t means cloud blocks at y 62..62+t
    over open water (0 = none). Outside the zone-3 tile box the banks thin out and are gone ~70 blocks out."""
    X = np.asarray(X, np.float64)
    Z = np.asarray(Z, np.float64)
    R = np.hypot(X - Z3_STAM[0], Z - Z3_STAM[1])
    n = ruis2(X, Z, 46, 5) * 0.65 + ruis2(X, Z, 17, 6) * 0.35
    drempel = np.full(X.shape, 0.52)
    dk = np.abs(X - kloof_x(Z))
    # U-OVERGANG: the outer edge of the cloud cliffs wobbles (24..56 from the channel) instead of a parallel line at 40
    band = 40 + 16 * (2 * ruis2(X, Z, 23, 7) - 1)
    drempel = np.where(dk < band, 0.30, drempel)                      # dense cloud cliffs along the kloof
    drempel = np.where(dk < 15, 9.0, drempel)                         # the open channel itself
    oost = np.clip((X - 1620) / 90, 0, 1)
    drempel = drempel + 0.22 * oost                                   # the Wolkenval: banks break apart towards zone 4
    # U-OVERGANG: outside the zone-3 box the banks fall apart (a rising threshold, so the edge follows the noise)
    uit = np.clip(buiten_z3(X, Z) / 70.0, 0, 1)
    drempel = drempel + 0.55 * uit
    dik = np.clip((n - drempel) * 26, 0, 9) * (1 - 0.6 * oost)
    dik = np.where(dk < band, dik * 1.6 + 2 * (dik > 0), dik)
    dik = dik * (1 - 0.7 * uit)
    soort = (ruis2(X, Z, 7, 9) * 4).astype(np.int32) % 4
    t = np.where(R >= 262, dik, 0).astype(np.int32)
    return t, soort


# ------------------------------------------------------------------------------------------------ scattered rocks / islets
# Small rock stacks in the open sea near the zones (30..260 blocks from a zone's land box), so the coasts taper out into the sea
# instead of ending at an empty tile edge. Only in the procedural sea (never closer than r + 6 to any tile box).
LAND = [(-488, 886, -258, 1106), (-640, -650, 640, 650), (434, -1436, 974, -806), (1166, -897, 1725, -338),
        (1740, -716, 2540, -266)]
TEGELVAKKEN = [(-704, -704, 767, 1215), (-128, -768, 1023, -705), (384, -1472, 1023, -769), (1024, -1024, 1727, -129),
               (1728, -768, 3199, -257)]
ROTS_CEL = 72
ROTS = ["minecraft:stone", "minecraft:andesite", "minecraft:tuff", "minecraft:calcite"]


def _doos_afstand(x, z, b):
    dx = max(b[0] - x, x - b[2], 0)
    dz = max(b[1] - z, z - b[3], 0)
    return float(np.hypot(dx, dz))


def rots_in_cel(cx, cz):
    """The rock of cell (cx, cz): (x, z, radius, top y) or None."""
    if hash01(cx, cz, 41) >= 0.45:
        return None
    x = cx * ROTS_CEL + 12 + float(hash01(cx, cz, 42)) * 48
    z = cz * ROTS_CEL + 12 + float(hash01(cx, cz, 43)) * 48
    r = 3 + float(hash01(cx, cz, 44)) * 5
    top = 63 + int(float(hash01(cx, cz, 45)) * 8)
    land = min(_doos_afstand(x, z, b) for b in LAND)
    if land < 30 or land > 260:
        return None
    if min(_doos_afstand(x, z, b) for b in TEGELVAKKEN) < r + 6:
        return None
    return x, z, r, top


def rots(X, Z):
    """Top y of the rock at world x/z (-1 = none). X, Z: 2D arrays."""
    X = np.asarray(X, np.float64)
    Z = np.asarray(Z, np.float64)
    uit = np.full(X.shape, -1, np.int32)
    cx0, cx1 = int(np.floor(X.min() / ROTS_CEL)) - 1, int(np.floor(X.max() / ROTS_CEL)) + 1
    cz0, cz1 = int(np.floor(Z.min() / ROTS_CEL)) - 1, int(np.floor(Z.max() / ROTS_CEL)) + 1
    G = vloer(X, Z)
    w = 1 + 0.5 * (ruis2(X, Z, 5, 46) - 0.5)
    for cx in range(cx0, cx1 + 1):
        for cz in range(cz0, cz1 + 1):
            r = rots_in_cel(cx, cz)
            if r is None:
                continue
            x, z, rr, top = r
            d = np.hypot(X - x, Z - z) * w / rr
            m = d < 1
            if not m.any():
                continue
            h = np.floor(G + (top - G) * np.power(np.clip(1 - d * d, 0, 1), 0.6) + 0.5).astype(np.int32)
            uit = np.where(m & (h > G), np.maximum(uit, h), uit)
    return uit
