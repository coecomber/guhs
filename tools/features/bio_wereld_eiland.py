"""
biomes3 wereld: the builder of floating islands, cloud banks and lift columns for TEMPLATES (not in FEATURES; a library).

The natural islands of the Wolkenweide are made by the terrain model (Java: WolkTerrein). A structure of the Wolkenweide
brings its own island or cloud inside its template (structure type guhs:bio_plek, kind "lucht"); with these three
functions it gets the same shapes: the same outlines (round, elongated, crescent, double, with a hole), the same
drip-shaped underside, plus the finish the approved sketch shows (base-ontwerpen/vibes/_bron/biome_wolkenweide_v2.py):
layered pastel rock, glowing crystal tips, hanging vines.

    from features import bio_wereld_eiland as eiland

    tops = eiland.eiland(h, s, (cx, y_top, cz), straal_x, straal_z, zaad, vorm="rond", ...)
        s          a make_structures.Structure (h.Structure((x, y, z)))
        midden     (x, y, z): the middle column and the y of the island's TOP block there
        straal_x/z half its size along x and z (before `draai`)
        zaad       any hashable: the same seed gives the same island
        vorm       "rond" | "lang" (give a long straal_x yourself) | "maan" (crescent, open to +x) | "dubbel" (a big and a
                   smaller blob joined) | "gat" (a hole in the middle)
        draai      degrees to turn the outline around the middle (default 0)
        diepte     how far the underside hangs under the middle (default: about 1.7 x the smallest radius + 3)
        punten     the number of long drip points (default 2-4 by size)
        relief     None, or a function (dx, dz) -> extra blocks of height on top (a hill); the edge always stays flat
        top        the top block (default "minecraft:pink_wool", the Guhmensie's ground)
        kristal / ranken   glowing tips in the drip points and crystals / vines hanging under it (default True)
      returns {(x, z): y} : the y of the top block of every column of the island (put your building on those).
      Leave room in the template: about straal + 2 to every side, and `diepte` + 8 below y_top.

    eiland.wolk(h, s, (cx, cy, cz), straal_x, straal_y, straal_z, zaad, kleur="wit")
        a cloud bank: a main blob and a few smaller ones, flatter underneath. kleur "wit" | "roze" (the cloud blocks of
        slice blokken-wolk; wool until that slice is merged). Returns {(x, z): y} of its top blocks (you can stand on it).

    eiland.lift(h, s, x, z, y_onder, y_boven, omlaag=False, kijk="south")
        a 3 x 3 wolkenlift column around (x, z), as in the zwevende_eilanden structure: guhs:wolkenlift pads at y_onder,
        guhs:wolkenstroom from y_onder + 1 to y_boven. Up (default): end it two blocks above the floor you arrive on;
        `kijk` is the side you are puffed off to at the top. omlaag=True: a stream that floats you down.

Everything is deterministic (lib.rng on the seed): the same call paints the same blocks.
"""
import math

from features import bio_lib as lib

STEEN = ["guhs:knuffelsteen", "minecraft:calcite", "guhs:guh_kristalsteen", "minecraft:purpur_block",
         "minecraft:calcite", "minecraft:pink_concrete_powder", "guhs:parelmoer", "minecraft:purpur_block"]
GLOED = "guhs:guh_kristal_blok"
AARDE = "minecraft:pink_wool"
VORMEN = ("rond", "lang", "maan", "dubbel", "gat")


def _binnen(vorm, u, v, p):
    """How deep inside the outline (u, v in units of the radii): above 0 inside, 1 in the middle. Java: WolkTerrein.Eiland.binnen."""
    def bol(uu, vv, schaal):
        a = math.atan2(vv, uu)
        golf = 1 + 0.14 * math.sin(2 * a + p[0]) + 0.10 * math.sin(3 * a + p[1])
        return 1 - math.hypot(uu, vv) / (golf * schaal)
    if vorm == "dubbel":
        return max(bol(u - 0.55, v, 0.62), bol(u + 0.55, v, 0.5))
    f = bol(u, v, 1.0)
    if vorm == "maan":
        f = min(f, (math.hypot(u - 0.55, v) / 0.7 - 1) * 0.9)
    elif vorm == "gat":
        f = min(f, (math.hypot(u, v) / 0.32 - 1) * 0.6)
    return f


def eiland(h, s, midden, straal_x, straal_z, zaad, vorm="rond", draai=0.0, diepte=None, punten=None, relief=None,
           top=AARDE, kristal=True, ranken=True):
    assert vorm in VORMEN, vorm
    rnd = lib.rng(f"bio_wereld_eiland/{zaad}")
    cx, cy, cz = midden
    p = [float(rnd.uniform(0, 6.283)) for _ in range(6)]
    c, sn = math.cos(math.radians(draai)), math.sin(math.radians(draai))
    r = min(straal_x, straal_z)
    diepte = diepte or r * 1.7 + 3
    if punten is None:
        punten = 1 if r < 3 else 2 if r < 6 else 3 if r < 10 else 4
    bereik = int(max(straal_x, straal_z) * 1.4) + 2
    cellen = {}
    for dx in range(-bereik, bereik + 1):
        for dz in range(-bereik, bereik + 1):
            u, v = (dx * c + dz * sn) / straal_x, (-dx * sn + dz * c) / straal_z
            f = _binnen(vorm, u, v, p)
            if f > 0:
                cellen[(dx, dz)] = min(1.0, f)
    binnenste = sorted(k for k, f in cellen.items() if f > 0.4) or sorted(cellen)
    tips = []
    for i in range(punten):
        tx, tz = (0, 0) if (i == 0 and vorm in ("rond", "lang") and (0, 0) in cellen) else binnenste[int(rnd.integers(len(binnenste)))]
        tips.append((tx, tz, diepte * (0.70 if i == 0 else float(rnd.uniform(0.3, 0.62))), max(1.0, r * (0.34 if i == 0 else float(rnd.uniform(0.1, 0.17))))))
    tops = {}
    for (dx, dz), f in sorted(cellen.items()):
        x, z = cx + dx, cz + dz
        onder = 1.2 + diepte * 0.32 * f ** 1.3
        for (tx, tz, lang, breed) in tips:
            onder += lang * math.exp(-((dx - tx) ** 2 + (dz - tz) ** 2) / (2 * breed * breed))
        hoog = cy + (int(round(relief(dx, dz) * min(1.0, f * 5))) if relief else 0)
        bodem = int(round(hoog - onder))
        in_punt = any((dx - tx) ** 2 + (dz - tz) ** 2 <= (2.2 if r > 8 else 1.1 if r > 4.4 else 0.3) for (tx, tz, _, _) in tips)
        golf = 1.3 * math.sin(x * 0.33 + p[5]) + 1.0 * math.sin(z * 0.4 + p[3]) + 0.6 * math.sin((x - z) * 0.21 + p[2])
        for y in range(bodem, hoog + 1):
            d = hoog - y
            if d == 0:
                blok = top
            elif d == 1 and onder > 2.2 and r > 2.5:
                blok = AARDE
            elif kristal and in_punt and y - bodem < (3 if r > 8 else 2 if r > 2.5 else 1):
                blok = GLOED
            else:
                blok = STEEN[int(max(0, d - 2 + golf) / 2.0) % len(STEEN)]
            s.set(x, y, z, blok)
        if s.inside(x, hoog, z):
            tops[(x, z)] = hoog
        q = float(rnd.random())
        if ranken and q < 0.11:
            n = int(rnd.integers(2, 8 if r > 6 else 5))
            for i in range(1, n + 1):
                if (x, bodem - i, z) not in s.blocks:
                    s.set(x, bodem - i, z, "guhs:bleek_hangmos", {"tip": "true" if i == n else "false"})
        elif kristal and q < 0.17 and (x, bodem - 1, z) not in s.blocks:
            s.set(x, bodem - 1, z, "guhs:guh_kristal_cluster", {"facing": "down"})
    return tops


def wolk(h, s, midden, straal_x, straal_y, straal_z, zaad, kleur="wit"):
    assert kleur in ("wit", "roze"), kleur
    rnd = lib.rng(f"bio_wereld_wolk/{zaad}")
    blok = lib.blok(h, "wolkenblok_roze", "minecraft:pink_wool") if kleur == "roze" else lib.blok(h, "wolkenblok_wit", "minecraft:white_wool")
    cx, cy, cz = midden
    bollen = [(cx, cy, cz, straal_x, straal_y, straal_z)]
    for _ in range(4):
        a, k = float(rnd.uniform(0, 6.283)), float(rnd.uniform(0.45, 0.75))
        bollen.append((cx + math.cos(a) * straal_x * 0.75, cy + int(rnd.choice((0, 0, 1))), cz + math.sin(a) * straal_z * 0.75,
                       straal_x * k, straal_y * float(rnd.uniform(0.7, 1.15)), straal_z * k))
    tops = {}
    for (bx, by, bz, rx, ry, rz) in bollen:
        for x in range(int(bx - rx - 1), int(bx + rx + 2)):
            for z in range(int(bz - rz - 1), int(bz + rz + 2)):
                dh = ((x - bx) / rx) ** 2 + ((z - bz) / rz) ** 2
                if dh > 1:
                    continue
                for y in range(int(by - ry), int(by + ry + 2)):
                    dy = (y - by) / (ry if y >= by else ry * 0.55)
                    if dh + dy * dy <= 1 and s.inside(x, y, z) and (x, y, z) not in s.blocks:
                        s.set(x, y, z, blok)
                        tops[(x, z)] = max(tops.get((x, z), y), y)
    return tops


def lift(h, s, x, z, y_onder, y_boven, omlaag=False, kijk="south"):
    props = {"facing": kijk, "down": "true" if omlaag else "false"}
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            s.set(x + dx, y_onder, z + dz, "guhs:wolkenlift", dict(props))
            for y in range(y_onder + 1, y_boven + 1):
                s.set(x + dx, y, z + dz, "guhs:wolkenstroom", dict(props))
