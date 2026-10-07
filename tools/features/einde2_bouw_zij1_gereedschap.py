"""
einde2 (bouw_zij1): the tools the three builds of this slice share (not a feature module: not in features.FEATURES).

  - ruis: a small value noise (rims, undersides, moss);
  - eiland: the floating mass a place stands on (or hangs under): a top, an irregular rim, a tapering underside;
  - kamer / doos / trap / deur / ladder / pilaar / hanglamp / bord: the bricks of every building here;
  - trapvormen: gives every stair the corner shape it would have got when placed by hand (the tiles of a guhs:leegte_bouw
    keep the block states of the template: nothing looks at its neighbours when a copy appears);
  - verlicht: the Guheinde has no skylight; after building, every walkable spot that is still too dark gets a glowing
    block laid into the floor next to it (the palette of the place decides which);
  - glijcheck: rides a post chute from its first block to its last, the way E2 Postgoot pushes a rider (the walker of
    the self-check does not know sliding);
  - plaatje: a top view and a cross-section of a build as PNG, to look at a template without a client.
"""
import math
import os
from collections import deque

import features.einde2_kit as kit
from features import guheinde_bouw as gb

SLICE = "bouw_zij1"
PREFIX = "sign.guhs.einde2_bouw_zij1"
GOOT = "guhs:einde2_bouw_zij1_postgoot"
AIR = gb.AIR
N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))
N6 = ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))
LINKS = {"north": "west", "west": "south", "south": "east", "east": "north"}      # counter-clockwise
RECHTS = {v: k for k, v in LINKS.items()}
YAW = {"south": 0.0, "west": 90.0, "north": 180.0, "east": -90.0}


# =====================================================================================================================
# noise
# =====================================================================================================================
def ruis(naam, schaal=8.0):
    """A smooth value noise n(x, z) in -1..1, seeded from a name."""
    zaad = kit.seed(f"{SLICE}/{naam}")

    def punt(i, j):
        v = (i * 374761393 + j * 668265263 + zaad * 2246822519) & 0xFFFFFFFF
        v = ((v ^ (v >> 13)) * 1274126177) & 0xFFFFFFFF
        return ((v ^ (v >> 16)) & 0xFFFF) / 32767.5 - 1.0

    def n(x, z):
        fx, fz = x / schaal, z / schaal
        i, j = math.floor(fx), math.floor(fz)
        u, v = fx - i, fz - j
        u, v = u * u * (3 - 2 * u), v * v * (3 - 2 * v)
        a = punt(i, j) * (1 - u) + punt(i + 1, j) * u
        c = punt(i, j + 1) * (1 - u) + punt(i + 1, j + 1) * u
        return a * (1 - v) + c * v
    return n


# =====================================================================================================================
# the island
# =====================================================================================================================
def eiland(b, vorm, boven, rots, kern=None, laag=3):
    """The mass of a floating island. vorm(x, z) -> None (no island there) or (top y, bottom y), both inclusive.
    boven = the block of the top `laag` layers (a name, or a function (x, y, z) -> name), rots = the rest (the same).
    kern = a block for the deep inside (cheaper tiles: the heart of the island is one block). Returns {(x, z): top y}."""
    top = {}
    blocks = b.s.blocks
    f_boven = boven if callable(boven) else (lambda x, y, z: boven)
    f_rots = rots if callable(rots) else (lambda x, y, z: rots)
    for x in range(b.W):
        for z in range(b.D):
            v = vorm(x, z)
            if v is None:
                continue
            t, o = v
            if o > t:
                continue
            top[(x, z)] = t
            for y in range(max(0, o), t + 1):
                if y > t - laag:
                    n = f_boven(x, y, z)
                elif kern and o + 3 < y < t - laag - 2:
                    n = kern
                else:
                    n = f_rots(x, y, z)
                blocks[(x, y, z)] = (gb.mc(n), {}, None)
    return top


# =====================================================================================================================
# bricks
# =====================================================================================================================
def doos(b, x0, y0, z0, x1, y1, z1, name, props=None):
    """A hollow box: only its six faces."""
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or y in (y0, y1) or z in (z0, z1):
                    b.set(x, y, z, name, props)


def kamer(b, x0, y0, z0, x1, y1, z1, muur, vloer=None, plafond=None):
    """A room: (x0, y0, z0)-(x1, y1, z1) are its OUTER corners; walls, floor (y0) and ceiling (y1) in one go, air inside."""
    doos(b, x0, y0, z0, x1, y1, z1, muur)
    b.fill(x0, y0, z0, x1, y0, z1, vloer or muur)
    b.fill(x0, y1, z0, x1, y1, z1, plafond or muur)
    b.fill(x0 + 1, y0 + 1, z0 + 1, x1 - 1, y1 - 1, z1 - 1, AIR)


def trap(b, x, y, z, richting, n, name, breed=1, vul=None, vrij=3):
    """A straight stair of n steps going UP in `richting` from the step at (x, y, z); breed = its width (to the right of
    the direction), vul = what stands under the steps (None: nothing), vrij = air cut out above every step."""
    dx, dz = gb.DIRS[richting]
    rx, rz = gb.DIRS[RECHTS[richting]]
    for i in range(n):
        for k in range(breed):
            px, pz, py = x + dx * i + rx * k, z + dz * i + rz * k, y + i
            b.set(px, py, pz, name, gb.stair(richting))
            for d in range(1, vrij + 1):
                b.set(px, py + d, pz, AIR)
            if vul:
                yy = py - 1
                while yy >= 0 and b.s.blocks.get((px, yy, pz), (AIR,))[0] == AIR:
                    b.set(px, yy, pz, vul)
                    yy -= 1


def binnentrap(b, x, z, richting, y_laag, y_hoog, trap_blok, wal, vul=None):
    """A stair inside a building, along a wall: from the floor layer y_laag up to the floor layer y_hoog (the top step lies
    in that floor), with a rail (wal) on the open sides of the stairwell, wherever the upper floor is there.
    (x, z) = its lowest step, richting = the way up, vul = what the steps rest on (None: they hang on the wall beside
    them)."""
    n = y_hoog - y_laag
    trap(b, x, y_laag + 1, z, richting, n, trap_blok, vul=vul, vrij=3)
    dx, dz = gb.DIRS[richting]
    for i in range(max(1, n - 5), n - 1):         # (only beside the hole in the upper floor)
        for sx, sz in ((dz, dx), (-dz, -dx)):
            c = (x + dx * i + sx, y_hoog, z + dz * i + sz)
            onder = b.s.blocks.get(c, (AIR,))[0]
            boven = b.s.blocks.get((c[0], y_hoog + 1, c[2]), (AIR,))[0]
            if onder != AIR and not onder.endswith(("_stairs", "_trap")) and boven == AIR and b.solid_at(c):
                b.set(c[0], y_hoog + 1, c[2], wal)


def deur(b, x, y, z, facing, hout="spruce", scharnier="left"):
    gb.door(b, x, y, z, facing, hout, scharnier)


def ladder(b, x, y0, y1, z, facing):
    """A ladder from y0 to y1 on the block behind it (facing = the side you look at it from)."""
    for y in range(y0, y1 + 1):
        b.set(x, y, z, "ladder", {"facing": facing, "waterlogged": "false"})


def pilaar(b, x, y0, y1, z, name, props=None):
    for y in range(y0, y1 + 1):
        b.set(x, y, z, name, props)


def hanglamp(b, x, y, z, ketting=1, lamp="lantern"):
    """A lantern on a chain under the block at (x, y + 1, z)."""
    for i in range(ketting):
        b.set(x, y - i, z, "chain", {"axis": "y", "waterlogged": "false"})
    b.set(x, y - ketting, z, lamp, gb.lamp(True))


def staaf(b, x, y, z, facing="up"):
    b.set(x, y, z, "guhs:lichtstaafje", {"facing": facing})


def kristal(b, x, y, z, facing="up"):
    b.set(x, y, z, "guhs:kristalkaas_kristal", {"facing": facing, "waterlogged": "false"})


def koorplant(b, x, y, z, hoog, bloem=True):
    """A knabbelkoor on the block (x, y - 1, z) (koorkorst): a stem of `hoog` and a flower that has finished growing."""
    for i in range(hoog):
        b.set(x, y + i, z, "guhs:knabbelkoorplant", {"down": "true", "up": "true" if (i < hoog - 1 or bloem) else "false",
                                                    "north": "false", "south": "false", "east": "false", "west": "false"})
    if bloem:
        b.set(x, y + hoog, z, "guhs:knabbelkoorbloem", {"age": "5"})


def trapvormen(b):
    """Corner shapes for every stair of the build, by the rule of the game itself (StairBlock.getStairsShape)."""
    blocks = b.s.blocks

    def is_trap(bl):
        return bl is not None and (bl[0].endswith("_stairs") or bl[0].endswith("_trap")) and "facing" in bl[1]

    def kan(c, bl, d):
        dx, dz = gb.DIRS[d]
        n = blocks.get((c[0] + dx, c[1], c[2] + dz))
        return not is_trap(n) or n[1]["facing"] != bl[1]["facing"] or n[1].get("half") != bl[1].get("half")

    for c, bl in list(blocks.items()):
        if not is_trap(bl):
            continue
        f = bl[1]["facing"]
        dx, dz = gb.DIRS[f]
        shape = "straight"
        voor = blocks.get((c[0] + dx, c[1], c[2] + dz))
        achter = blocks.get((c[0] - dx, c[1], c[2] - dz))
        if is_trap(voor) and voor[1].get("half") == bl[1].get("half") and voor[1]["facing"] not in (f, gb.OPP[f]) \
                and kan(c, bl, gb.OPP[voor[1]["facing"]]):
            shape = "outer_left" if voor[1]["facing"] == LINKS[f] else "outer_right"
        elif is_trap(achter) and achter[1].get("half") == bl[1].get("half") and achter[1]["facing"] not in (f, gb.OPP[f]) \
                and kan(c, bl, achter[1]["facing"]):
            shape = "inner_left" if achter[1]["facing"] == LINKS[f] else "inner_right"
        if bl[1].get("shape") != shape:
            blocks[c] = (bl[0], dict(bl[1], shape=shape), bl[2])


# =====================================================================================================================
# light
# =====================================================================================================================
class Licht:
    """The block light of a build, kept up to date while lamps are added (the same rules as kit.Bouw.licht)."""

    def __init__(self, b):
        self.b = b
        self.blocks = b.s.blocks
        self.light = b.licht()

    def dicht(self, c):
        bl = self.blocks.get(c)
        if bl is None:
            return False
        n = bl[0]
        return n != AIR and n not in kit.WATER and not self.b.is_dun(n) and not any(t in n for t in gb.TRANSPARENT_SUB) and n not in kit.LICHT

    def zet(self, c, sterkte):
        self.light[c] = max(self.light.get(c, 0), sterkte)
        todo = deque([c])
        while todo:
            c = todo.popleft()
            l = self.light[c] - 1
            if l <= 0:
                continue
            x, y, z = c
            for dx, dy, dz in N6:
                n = (x + dx, y + dy, z + dz)
                if self.b.inside(*n) and self.light.get(n, 0) < l and not self.dicht(n):
                    self.light[n] = l
                    todo.append(n)


def verlicht(b, start, inleg, vervangbaar, min_licht=7, raster=5, niet=(), inleg_props=None):
    """Lays glowing blocks (`inleg`, a name of kit.LICHT) into the floor until every spot a player can walk to from `start`
    has at least min_licht: under a dark spot when the floor there is one of `vervangbaar` (block names), else in a wall
    or floor block right next to it. Spots on the `raster` grid go first, so the lamps make a pattern where they can.
    niet = cells that must keep their block. Returns how many lamps were laid."""
    w = kit.Loper(b)
    reach = w.bfs(tuple(start))
    licht = Licht(b)
    blocks = b.s.blocks
    sterkte = kit.LICHT[gb.mc(inleg)]
    niet = set(niet)
    for e in b.plekken.values():
        if "pos" in e:
            niet.add(tuple(e["pos"]))
            niet.add((e["pos"][0], e["pos"][1] - 1, e["pos"][2]))

    def kan(c):
        bl = blocks.get(c)
        return bl is not None and bl[0] in vervangbaar and c not in niet

    donker = sorted(c for c in reach if licht.light.get(c, 0) < min_licht)
    eerst = [c for c in donker if c[0] % raster == 0 and c[2] % raster == 0]
    gelegd = 0
    for c in eerst + donker:
        if licht.light.get(c, 0) >= min_licht:
            continue
        x, y, z = c
        plek = None
        if kan((x, y - 1, z)):
            plek = (x, y - 1, z)
        else:
            for dx, dy, dz in ((1, -1, 0), (-1, -1, 0), (0, -1, 1), (0, -1, -1), (1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1),
                               (1, 1, 0), (-1, 1, 0), (0, 1, 1), (0, 1, -1), (0, 2, 0), (1, 2, 0), (-1, 2, 0), (0, 2, 1), (0, 2, -1),
                               (1, -1, 1), (-1, -1, -1), (1, -1, -1), (-1, -1, 1), (0, -2, 0)):
                if kan((x + dx, y + dy, z + dz)):
                    plek = (x + dx, y + dy, z + dz)
                    break
        if plek is None:
            continue
        blocks[plek] = (gb.mc(inleg), dict(inleg_props or {}), None)
        licht.zet(plek, sterkte)
        gelegd += 1
    return gelegd


# =====================================================================================================================
# the post chutes
# =====================================================================================================================
def glijcheck(b, start, einde, max_stap=1):
    """Rides a chute: start = the cell a rider stands in on the first chute block, einde = the cell where the ride must
    end (the rider gets off within 2 blocks of it). A chute block pushes whoever stands on it one cell on in its facing;
    the rider may drop at most max_stap per cell (never a fall that hurts), keeps two free cells for its body and cannot
    leave the trough sideways. Returns (the cells of the ride, the problems)."""
    blocks = b.s.blocks
    w = kit.Loper(b)

    def goot(c):
        bl = blocks.get((c[0], c[1] - 1, c[2]))
        return bl[1]["facing"] if bl is not None and bl[0] == GOOT else None

    c, rit, p = tuple(start), [], []
    for _ in range(4000):
        rit.append(c)
        f = goot(c)
        if f is None:
            break
        if not (w.passable(c) and w.passable((c[0], c[1] + 1, c[2]))):
            p.append(f"glijbaan {c}: geen twee vrije blokken voor de glijder")
            break
        for zij in (LINKS[f], RECHTS[f]):
            dx, dz = gb.DIRS[zij]
            n = (c[0] + dx, c[1], c[2] + dz)
            if w.passable(n) and goot(n) is None and len(rit) > 2:
                # (an open side: fine when it is another chute that joins, or the first cells where you step in)
                p.append(f"glijbaan {c}: de zijkant {n} is open (je glijdt eruit)")
        dx, dz = gb.DIRS[f]
        n = (c[0] + dx, c[1], c[2] + dz)
        if not (w.passable(n) and w.passable((n[0], n[1] + 1, n[2]))):
            p.append(f"glijbaan {c} -> {n}: daar zit iets in de weg")
            break
        val = 0
        while not w.support((n[0], n[1] - 1, n[2])):
            n = (n[0], n[1] - 1, n[2])
            val += 1
            if val > max_stap or n[1] <= 0:
                p.append(f"glijbaan {c} -> {n}: een val van meer dan {max_stap}")
                return rit, p
        if n in rit:
            p.append(f"glijbaan {n}: de goot loopt in een rondje")
            break
        c = n
    else:
        p.append("glijbaan: geen einde gevonden")
    if not p and (max(abs(c[0] - einde[0]), abs(c[2] - einde[2])) > 2 or abs(c[1] - einde[1]) > 1):
        p.append(f"glijbaan van {tuple(start)}: eindigt op {c}, niet bij {tuple(einde)}")
    return rit, p


def eerlijk(b):
    """The walker of the kit takes every block whose name holds "minecraft:light" for something thin you walk through
    (it means the light block): a wall of light blue glass or light gray wool would be no wall to it, and the self-check
    would call places reachable that are not. So the builds of this slice use no such block: this says which ones did."""
    fout = sorted({bl[0] for bl in b.s.blocks.values() if "minecraft:light" in bl[0] and bl[0] != "minecraft:light"})
    return [f"blok {n}: de loper van de zelfcontrole loopt hier dwars doorheen (neem een andere kleur)" for n in fout]


def hoeken(rit):
    """The corner points of a ride (for its pad marker): first, last and every cell where the direction changes."""
    out = [rit[0]]
    for a, c, d in zip(rit, rit[1:], rit[2:]):
        if (c[0] - a[0], c[2] - a[2]) != (d[0] - c[0], d[2] - c[2]):
            out.append(c)
    out.append(rit[-1])
    return out


# =====================================================================================================================
# a look at a template without a client
# =====================================================================================================================
KLEUR = [("postgoot", (150, 220, 250)), ("klipkaas_stenen", (214, 150, 60)), ("klipkaas", (196, 128, 52)), ("kristalkaas", (255, 214, 80)),
         ("kruimelrots_stenen", (190, 140, 96)), ("kruimelrots", (170, 120, 84)), ("dichte_kruimelwolk", (236, 236, 244)),
         ("kruimelwolk", (250, 250, 255)), ("koorkorst", (122, 78, 150)), ("knabbelkoorblok", (190, 150, 205)),
         ("knabbelkoorpilaar", (176, 132, 196)), ("knabbelkoor", (150, 90, 190)), ("kaaskorst", (226, 190, 96)), ("korstgras", (200, 190, 80)),
         ("lichtstaafje", (255, 230, 240)), ("guh_kristal", (255, 170, 210)), ("dark_prismarine", (52, 92, 76)), ("prismarine", (100, 160, 150)),
         ("red_nether", (90, 20, 24)), ("nether_brick", (48, 24, 28)), ("blackstone", (44, 40, 48)), ("gold", (250, 220, 70)),
         ("lava", (240, 110, 20)), ("water", (60, 110, 230)), ("glass", (190, 220, 235)), ("calcite", (224, 226, 222)),
         ("orange", (224, 120, 30)), ("yellow", (240, 200, 60)), ("red", (160, 44, 40)), ("white", (234, 234, 234)), ("green", (90, 130, 60)),
         ("cyan", (90, 170, 190)), ("blue", (60, 70, 170)), ("ice", (150, 190, 250)), ("hay", (200, 170, 40)), ("spruce", (110, 80, 48)),
         ("dark_oak", (66, 44, 22)), ("oak", (160, 130, 80)), ("birch", (200, 180, 130)), ("froglight", (250, 240, 200)), ("shroomlight", (250, 170, 90)),
         ("lantern", (250, 200, 100)), ("magma", (150, 70, 30)), ("basalt", (80, 80, 86)), ("tuff", (108, 110, 102)), ("stone", (125, 125, 125)),
         ("deepslate", (72, 72, 78)), ("copper", (180, 110, 80)), ("purpur", (170, 126, 170)), ("moss", (90, 110, 50)), ("leaves", (70, 120, 50))]


def kleur(name):
    for sleutel, rgb in KLEUR:
        if sleutel in name:
            return rgb
    return (150, 150, 150)


def plaatje(b, map_, schaal=4):
    """Writes <map_>/<name>_boven.png (top view, shaded by height), <name>_zuid.png and <name>_oost.png (what you see from
    the south and from the east) and <name>_snede.png (the cut through the middle, z = D / 2)."""
    from PIL import Image
    os.makedirs(map_, exist_ok=True)
    blocks = {c: bl[0] for c, bl in b.s.blocks.items() if bl[0] != AIR}

    def schaduw(rgb, f):
        return tuple(max(0, min(255, int(v * f))) for v in rgb)

    def bewaar(img, naam):
        img.resize((img.width * schaal, img.height * schaal), Image.NEAREST).save(os.path.join(map_, f"{b.name}_{naam}.png"))

    boven, zuid, oost, snede = {}, {}, {}, {}
    for (x, y, z), n in blocks.items():
        if (x, z) not in boven or boven[(x, z)][0] < y:
            boven[(x, z)] = (y, n)
        if (x, y) not in zuid or zuid[(x, y)][0] < z:
            zuid[(x, y)] = (z, n)
        if (z, y) not in oost or oost[(z, y)][0] < x:
            oost[(z, y)] = (x, n)
        if z == b.D // 2:
            snede[(x, y)] = (0, n)
    img = Image.new("RGB", (b.W, b.D), (24, 16, 36))
    for (x, z), (y, n) in boven.items():
        img.putpixel((x, z), schaduw(kleur(n), 0.55 + 0.6 * y / b.H))
    bewaar(img, "boven")
    for naam, vlak, breed, diep in (("zuid", zuid, b.W, b.D), ("oost", oost, b.D, b.W), ("snede", snede, b.W, 1)):
        img = Image.new("RGB", (breed, b.H), (24, 16, 36))
        for (u, y), (d, n) in vlak.items():
            img.putpixel((u if naam != "oost" else breed - 1 - u, b.H - 1 - y), schaduw(kleur(n), 0.6 + 0.5 * d / max(1, diep)))
        bewaar(img, naam)


# =====================================================================================================================
# roofs and little houses
# =====================================================================================================================
def piramidedak(b, x0, z0, x1, z1, y, trap, plaat, top=None, half="bottom", vul=None):
    """A hip roof over the rectangle (x0, z0)-(x1, z1): rings of stairs that step in one block per layer, from y upwards
    (the first ring is the eave: give the rectangle one block wider than the walls and y = their top layer, so the eave
    hangs beside the wall top and the second ring lies on it)
    (half="top" and a falling y: the same roof upside down, for a building that hangs). The last ring is closed with
    slabs; top = a block on the very tip (None: nothing). Under the stairs the roof is solid (vul; default: the block the
    stairs are made of), so every ring rests on the one below it. Returns the y of the tip."""
    stap = 1 if half == "bottom" else -1
    if vul is None:
        vul = trap.replace("brick_stairs", "bricks").replace("_stairs", "").replace("_trap", "")
    eerste = True
    while x1 - x0 >= 2 and z1 - z0 >= 2:
        if not eerste or half == "top":
            b.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, vul)
        eerste = False
        for x in range(x0, x1 + 1):
            b.set(x, y, z0, trap, gb.stair("south", half))
            b.set(x, y, z1, trap, gb.stair("north", half))
        for z in range(z0 + 1, z1):
            b.set(x0, y, z, trap, gb.stair("east", half))
            b.set(x1, y, z, trap, gb.stair("west", half))
        x0, z0, x1, z1, y = x0 + 1, z0 + 1, x1 - 1, z1 - 1, y + stap
    b.fill(x0, y, z0, x1, y, z1, plaat, gb.slab("bottom" if half == "bottom" else "top"))
    if top:
        b.fill((x0 + x1) // 2, y, (z0 + z1) // 2, (x0 + x1 + 1) // 2, y, (z0 + z1 + 1) // 2, top)
    return y


def bed(b, x, y, z, facing, kleur="lime"):
    """A bed: its foot at (x, y, z), its head one block on in `facing`."""
    dx, dz = gb.DIRS[facing]
    b.set(x, y, z, f"{kleur}_bed", {"facing": facing, "part": "foot", "occupied": "false"})
    b.set(x + dx, y, z + dz, f"{kleur}_bed", {"facing": facing, "part": "head", "occupied": "false"})


def huis(b, x0, z0, x1, z1, y, kant, muur, dak, hoog=4, raam="yellow_stained_glass_pane", hout="spruce", binnen=0):
    """A little house on the floor block layer y - 1: walls of `muur` (a list of names: one is picked per house), a door in
    the middle of side `kant`, windows, a hip roof of (trap, plaat) `dak`, a lantern and a few things inside. Returns the
    cell in front of its door (outside)."""
    steen = muur[b.rng.randrange(len(muur))] if isinstance(muur, (list, tuple)) else muur
    kamer(b, x0, y - 1, z0, x1, y + hoog, z1, steen, vloer="spruce_planks", plafond="spruce_planks")
    mx, mz = (x0 + x1) // 2, (z0 + z1) // 2
    dx, dz = gb.DIRS[kant]
    px, pz = {"north": (mx, z0), "south": (mx, z1), "west": (x0, mz), "east": (x1, mz)}[kant]
    deur(b, px, y, pz, kant, hout)
    for zijde in ("north", "south", "east", "west"):
        if zijde == kant:
            continue
        wx, wz = {"north": (mx, z0), "south": (mx, z1), "west": (x0, mz), "east": (x1, mz)}[zijde]
        b.set(wx, y + 1, wz, raam)
    # (windows beside the door when the front is wide enough)
    if kant in ("north", "south") and x1 - x0 >= 6:
        b.set(px - 2, y + 1, pz, raam)
        b.set(px + 2, y + 1, pz, raam)
    if kant in ("east", "west") and z1 - z0 >= 6:
        b.set(px, y + 1, pz - 2, raam)
        b.set(px, y + 1, pz + 2, raam)
    piramidedak(b, x0 - 1, z0 - 1, x1 + 1, z1 + 1, y + hoog, dak[0], dak[1])
    hanglamp(b, mx, y + hoog - 1, mz, ketting=0)
    # inside: a bed in a back corner, a table with a chair, a cupboard
    achter = gb.OPP[kant]
    ax, az = gb.DIRS[achter]
    hoek_x = x0 + 1 if kant != "west" else x1 - 1
    hoek_z = z0 + 1 if kant != "north" else z1 - 1
    if binnen % 2 == 0:
        if kant in ("north", "south"):
            bed(b, x0 + 1, y, hoek_z - az, achter, "lime" if binnen % 4 == 0 else "yellow")
            b.set(x1 - 1, y, hoek_z, "guhs:guh_tafel", {"facing": kant})
            b.set(x1 - 2, y, hoek_z, "guhs:guh_stoel", {"facing": "east"})
        else:
            bed(b, hoek_x - ax, y, z0 + 1, achter, "lime" if binnen % 4 == 0 else "yellow")
            b.set(hoek_x, y, z1 - 1, "guhs:guh_tafel", {"facing": kant})
            b.set(hoek_x, y, z1 - 2, "guhs:guh_stoel", {"facing": "south"})
    else:
        b.set(hoek_x, y, hoek_z, "bookshelf")
        b.set(hoek_x, y + 1, hoek_z, "potted_dandelion" if binnen % 3 else "potted_fern")
        tx, tz = (x1 - 1 if hoek_x == x0 + 1 else x0 + 1), hoek_z
        b.set(tx, y, tz, "guhs:guh_tafel", {"facing": kant})
        b.set(tx, y + 1, tz, "candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
    return (px + dx, y, pz + dz)
