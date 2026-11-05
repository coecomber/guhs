"""T-WERELD: the airship harbours at the zone transitions (WERELDINDELING §4, approved W-3): a departure pier with a platform,
railings, lanterns, a mooring mast with a guh flag and a departure board. The FLIGHT itself is t-luchtschip's; this only builds
the place and publishes the shared POI ids:

    haven_<van>_<naar>_vertrek   stand spot on the departure platform (where the airship docks / you board)
    haven_<van>_<naar>_bord      stand spot in front of the departure board (t-luchtschip puts the timetable there)
    haven_<van>_<naar>_mast      top of the mooring mast (rope anchor for the ship)

Works on any scene with world-coordinate set/fill/get (bouwsels.WS), so zone 1 and zone 2 (local frame) share it.
"""
import math

PLANK = "minecraft:spruce_planks"
LOG = "minecraft:spruce_log[axis=y]"
HEK = "minecraft:spruce_fence"
BORD = "minecraft:dark_oak_planks"
LANT = "minecraft:lantern[hanging=false,waterlogged=false]"
CARVE = "minecraft:cave_air"


def pier(S, cx, y, cz, ux, uz, lengte, hoogste, prefix, P, breed=3, kop=6, bodem=40, wol="minecraft:pink_wool"):
    """A pier from (cx, cz) (on the plaza edge, deck top y) out along (ux, uz) (one of the 4 axes), `lengte` blocks, ending in
    a square platform (half size `kop`). hoogste(x, z) = the highest solid block below (for the posts).
    Writes P[prefix + "_vertrek" / "_bord" / "_mast" / "_eind"]."""
    vx, vz = -uz, ux                       # sideways

    def pt(s, p):
        return cx + ux * s + vx * p, cz + uz * s + vz * p
    # deck + head room + rails + posts
    for s in range(0, lengte + 1):
        for p in range(-breed, breed + 1):
            x, z = pt(s, p)
            S.set(x, y, z, PLANK)
            for yy in range(y + 1, y + 5):
                S.set(x, yy, z, CARVE)
            if abs(p) == breed and s > 1:
                S.set(x, y + 1, z, HEK)
                if s % 6 == 0:
                    S.set(x, y + 2, z, LANT)
            if abs(p) == breed and s % 6 == 0 or (p == 0 and s % 6 == 3):
                g = hoogste(x, z)
                for yy in range(max(g + 1, bodem), y):
                    S.set(x, yy, z, LOG)
    # the platform at the end
    mx, mz = pt(lengte + kop, 0)
    for a in range(-kop, kop + 1):
        for b in range(-kop, kop + 1):
            x, z = mx + a, mz + b
            S.set(x, y, z, PLANK if (a + b) % 5 else "minecraft:stripped_spruce_log[axis=y]")
            for yy in range(y + 1, y + 6):
                S.set(x, yy, z, CARVE)
            rand = max(abs(a), abs(b)) == kop
            terug = pt(lengte, 0)
            aansluiting = abs((x - terug[0]) * vx + (z - terug[1]) * vz) <= breed and ((x - mx) * ux + (z - mz) * uz) <= -kop
            if rand and not aansluiting:
                S.set(x, y + 1, z, HEK)
            if rand and (a + b) % 4 == 0:
                g = hoogste(x, z)
                for yy in range(max(g + 1, bodem), y):
                    S.set(x, yy, z, LOG)
    for (a, b) in ((-kop, -kop), (-kop, kop), (kop, -kop), (kop, kop)):
        S.set(mx + a, y + 2, mz + b, LANT)
    # the mooring mast (on the far edge, to the left) with a guh flag
    gx, gz = mx + ux * (kop - 1) + vx * (kop - 1), mz + uz * (kop - 1) + vz * (kop - 1)
    for yy in range(y + 1, y + 13):
        S.set(gx, yy, gz, LOG)
    S.set(gx, y + 13, gz, "minecraft:shroomlight")
    for i in range(4):
        for j in range(3 - i // 2):
            S.set(gx - vx * (i + 1), y + 11 - j, gz - vz * (i + 1), wol)
    # the departure board at the right-hand front corner of the platform
    bx, bz = mx - ux * (kop - 2) - vx * (kop - 1), mz - uz * (kop - 2) - vz * (kop - 1)
    P[prefix + "_bord"] = bord(S, bx, y, bz, ux, uz, vx, vz)
    P[prefix + "_vertrek"] = [int(mx + ux * 2), y + 1, int(mz + uz * 2)]
    P[prefix + "_mast"] = [int(gx), y + 13, int(gz)]
    P[prefix + "_eind"] = [int(mx + ux * (kop - 1)), y + 1, int(mz + uz * (kop - 1))]
    return mx, mz


def bord(S, bx, y, bz, ux, uz, vx, vz):
    """A departure board standing on deck top y: two posts and a dark 3x3 board in the line (ux, uz), its face towards
    (vx, vz). Returns the stand spot in front of it (where t-luchtschip shows the timetable)."""
    for w in (-2, 2):
        x, z = bx + ux * w, bz + uz * w
        for yy in range(y + 1, y + 5):
            S.set(x, yy, z, "minecraft:stripped_dark_oak_log[axis=y]")
    for w in (-1, 0, 1):
        x, z = bx + ux * w, bz + uz * w
        for yy in range(y + 2, y + 5):
            S.set(x, yy, z, BORD)
        S.set(x, y + 5, z, "minecraft:dark_oak_slab[type=bottom,waterlogged=false]")
    S.set(bx, y + 6, bz, LANT)
    return [int(bx + vx * 2), y + 1, int(bz + vz * 2)]


def doos(cx, cz, ux, uz, lengte, kop=6, marge=10):
    """World box (x0, z0, x1, z1) the pier needs (for the scene size)."""
    xs = [cx, cx + ux * (lengte + 2 * kop)]
    zs = [cz, cz + uz * (lengte + 2 * kop)]
    return min(xs) - kop - marge, min(zs) - kop - marge, max(xs) + kop + marge, max(zs) + kop + marge
