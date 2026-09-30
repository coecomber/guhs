"""
De structuren van het Guheinde (2.6), zie GUHEINDE_CONTRACT.md en guheinde.py:

  knabbelkelder          de "stronghold": een kaaskelder van kaaskorststenen diep onder de Guhmensie. Een centrale
                         trappenhal met een guhgezicht-mozaiek, de portaalkamer (12 knabbelportaalframes rond een poel
                         kaassaus, trap op, Mika-larfjes-spawner), de Knabbelbibliotheek met galerij, de leeggeroofde
                         knabbelvoorraad, Mika's feestzaal en een trap omlaag naar het cellenblok vol magere guhs.
  guheinde_knabbelberg   het "exitportaal" op het centrale eiland (Java plaatst hem): een reusachtige, half opgegeten
                         kaasknabbel met een pad eromheen naar het plateau (terugportaal, zuil, 4 knabbelsokkels) en
                         Opper-Mika's schatkamer achter het knabbelslot.
  mika_vesting(_schip)   de "End City": een toren van Mika-steen met een wenteltrap eromheen, troonzaal en dakterras,
                         en in de schip-variant het vetschip met de Guhvleugels in het boegkamertje (2.8: de helft
                         van de vestingen heeft een schip, gewicht 1:1).
  guheinde_terugpoort    (2.8) een klein Knabbelpoort-heiligdom met een guhkop, overal op de buiteneilanden: stap erin
                         en je zweeft terug naar het grote eiland (GuheindeReis.terugpoortDestination).

build(h) maakt de templates, hun worldgen-JSON en de contract-sidecars. Elke template heeft een strenge
self-check (bereikbaarheid met een flood fill als een speler, contractposities, hangende/zwevende blokken,
licht in de kelder): bij problemen SystemExit.
"""
import json
import math
import os
import random
from collections import deque

AIR = "minecraft:air"
SAUS = "guhs:kaas_saus"
FRAME = "guhs:knabbelportaalframe"
SLOT = "guhs:knabbelslot"
SOKKEL = "guhs:knabbelsokkel"
STEEN = "guhs:kaaskorst_stenen"
MSTEEN = "guhs:mika_steen"

# blocks you can walk through (or stand in), and blocks you can't jump onto
THIN_EXACT = {"minecraft:lantern", "minecraft:soul_lantern", "minecraft:chain", "minecraft:cobweb", "minecraft:vine"}
THIN_SUB = ("lampion", "_sign", "carpet", "torch", "pressure_plate", "_button", "kristal_cluster", "vlaggetjes")
FENCY_SUB = ("_fence", "_wall", "_pane", "iron_bars", "_muur")
TRANSPARENT_SUB = ("glass", "_slab", "_plaat", "_trap", "_stairs", "chest", "_fence", "_wall", "_muur", "_pane", "iron_bars",
                   "lectern", "_bed", "portaalframe", "sokkel", "spawner", "knabbelbak", "guh_tafel", "guh_stoel", "mikatrofee",
                   "honey_block", "_door", "cake", "leaves")
LIGHT = {"guhs:guh_kristal_lamp": 15, "guhs:lampion_roze": 15, "guhs:lampion_geel": 15, "guhs:lampion_mint": 15,
         "minecraft:lantern": 15, "minecraft:sea_lantern": 15, "minecraft:glowstone": 15, "minecraft:shroomlight": 15,
         "minecraft:ochre_froglight": 15, "minecraft:pearlescent_froglight": 15}
DIRS = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}
FACING_BYTE = {"down": 0, "up": 1, "north": 2, "south": 3, "west": 4, "east": 5}

# Mika's face (angry brows, red eyes, a fang): graffiti, sails and flags
MIKA_FACE = ["pKpppKp",
             "ppKpKpp",
             "pRpppRp",
             "ppppppp",
             "pKKKKKp",
             "ppWpppp"]
GRAFFITI = {"p": "purple_wool", "K": "black_wool", "R": "red_wool", "W": "white_wool"}


def mc(n):
    return n if ":" in n else f"minecraft:{n}"


def stair(facing, half="bottom"):
    return {"facing": facing, "half": half, "shape": "straight", "waterlogged": "false"}


def slab(t="bottom"):
    return {"type": t, "waterlogged": "false"}


def lamp(hanging):
    return {"hanging": "true" if hanging else "false", "waterlogged": "false"}


def pick(rng, table):
    total = sum(w for _, w in table)
    r = rng.uniform(0, total)
    for name, w in table:
        r -= w
        if r <= 0:
            return name
    return table[-1][0]


def is_thin(n):
    return n in THIN_EXACT or any(t in n for t in THIN_SUB)


def is_fency(n):
    return any(t in n for t in FENCY_SUB) and not is_thin(n)


# =====================================================================================================================
# a template in the making
# =====================================================================================================================
class Bouw:
    def __init__(self, h, name, size, unset_solid, seed):
        self.h = h
        self.name = name
        self.s = h.Structure(size)
        self.W, self.H, self.D = size
        self.unset_solid = unset_solid          # c -> is an unset cell solid for the walker (natural rock / the island)
        self.rng = random.Random(seed)
        self.oob = []                           # blocks we tried to put outside the template
        self.ents = []                          # (x, y, z, kind, nbt)

    def inside(self, x, y, z):
        return 0 <= x < self.W and 0 <= y < self.H and 0 <= z < self.D

    def set(self, x, y, z, name, props=None, nbt=None):
        if not self.inside(x, y, z):
            self.oob.append((x, y, z, name))
            return
        self.s.set(x, y, z, mc(name), dict(props) if props else None, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)

    def ent(self, x, y, z, nbt, kind):
        if not self.inside(int(math.floor(x)), int(math.floor(y)), int(math.floor(z))):
            self.oob.append((x, y, z, kind))
            return
        self.s.entity(x, y, z, nbt)
        self.ents.append((x, y, z, kind, nbt))

    def chest(self, x, y, z, facing, loot):
        self.h.chest(self.s, x, y, z, facing, loot)
        if not self.inside(x, y, z):
            self.oob.append((x, y, z, "chest"))

    def sign(self, x, y, z, facing, key, nl, en, wall=True, wood="birch"):
        """A waxed, glowing sign with lang keys (Dutch and English). Wall signs hang on the block behind them."""
        h = self.h
        msgs = []
        for line in list(nl) + list(en):
            if len(line) > 16:
                self.oob.append(("te lang bordje", key, line))
        for i in range(4):
            if i < len(nl) and nl[i]:
                k = f"sign.guhs.guheinde.{key}{i + 1}"
                h.lang(k, en[i], nl[i])
                msgs.append(json.dumps({"translate": k}))
            else:
                msgs.append('""')
        text = {"messages": h.ms.NbtList(8, msgs), "color": "black", "has_glowing_text": h.Byte(1)}
        empty = {"messages": h.ms.NbtList(8, ['""'] * 4), "color": "black", "has_glowing_text": h.Byte(0)}
        nbt = {"id": "minecraft:sign", "is_waxed": h.Byte(1), "front_text": text, "back_text": empty}
        if wall:
            self.set(x, y, z, f"{wood}_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
        else:
            rot = {"south": "0", "west": "4", "north": "8", "east": "12"}[facing]
            self.set(x, y, z, f"{wood}_sign", {"rotation": rot, "waterlogged": "false"}, nbt)

    def anchor(self, x, y, z, name):
        floor = self.s.blocks[(x, y, z)]
        final = floor[0] + ("[" + ",".join(f"{k}={v}" for k, v in sorted(floor[1].items())) + "]" if floor[1] else "")
        self.set(x, y, z, "jigsaw", {"orientation": "up_north"},
                 {"id": "minecraft:jigsaw", "name": name, "target": "minecraft:empty", "pool": "minecraft:empty",
                  "final_state": final, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
        self.anchor_at = (x, y, z, name, final)

    def art(self, rows, palette, plane, fixed, u0, ytop):
        """A picture on a wall: plane 'x' = the wall x = fixed (columns along z), 'z' = the wall z = fixed (along x)."""
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if palette.get(ch):
                    if plane == "x":
                        self.set(fixed, ytop - j, u0 + i, palette[ch])
                    else:
                        self.set(u0 + i, ytop - j, fixed, palette[ch])

    def solid_at(self, c):
        b = self.s.blocks.get(c)
        if b is None:
            return self.unset_solid(c)
        n = b[0]
        return n != AIR and n != SAUS and not is_thin(n) and not n.endswith("_door")

    def connect(self):
        """Fences, panes, bars and walls connect to their neighbours, like when you place them."""
        blocks = self.s.blocks
        for (x, y, z), (name, props, nbt) in list(blocks.items()):
            fence = name.endswith("_fence") or name.endswith("_pane") or name == "minecraft:iron_bars"
            wall = name.endswith("_wall") and not name.endswith("_wall_sign") or name.endswith("_muur")
            if not (fence or wall):
                continue
            p = {"waterlogged": "false"}
            for d, (dx, dz) in DIRS.items():
                c = (x + dx, y, z + dz)
                nb = blocks.get(c)
                nn = nb[0] if nb else None
                join = nn is not None and (is_fency(nn) or (self.solid_at(c) and "sign" not in nn and "chest" not in nn
                                                            and "_trap" not in nn and "_stairs" not in nn))
                if fence:
                    p[d] = "true" if join else "false"
                else:
                    p[d] = "low" if join else "none"
            if wall:
                above = blocks.get((x, y + 1, z))
                p["up"] = "true" if (above and above[0] != AIR) or sum(p[d] != "none" for d in DIRS) != 2 or \
                    not ((p["north"] != "none") == (p["south"] != "none")) else "false"
            blocks[(x, y, z)] = (name, p, nbt)

    def save(self):
        self.s.save(self.name)


# =====================================================================================================================
# walking like a player
# =====================================================================================================================
class Walker:
    def __init__(self, b, doors=True, slot=False, max_fall=3):
        self.b = b
        self.doors = doors
        self.slot = slot
        self.max_fall = max_fall
        self.holes = set()

    def name(self, c):
        bl = self.b.s.blocks.get(c)
        if bl is None:
            return "natural" if self.b.unset_solid(c) else None
        return bl[0]

    def passable(self, c):
        n = self.name(c)
        if n is None or n == AIR or n == SAUS:
            return True
        if n == "natural":
            return False
        if n == SLOT:
            return self.slot
        if n.endswith("_door"):
            return self.doors
        return is_thin(n)

    def fluid(self, c):
        return self.name(c) == SAUS

    def support(self, c):
        n = self.name(c)
        return n is not None and not self.passable(c) and (n == "natural" or not is_fency(n))

    def inside(self, c):
        return 0 <= c[0] < self.b.W and 0 <= c[2] < self.b.D and 0 <= c[1] < self.b.H

    def standable(self, c):
        x, y, z = c
        return self.inside(c) and self.passable(c) and self.passable((x, y + 1, z)) and \
            (self.support((x, y - 1, z)) or self.fluid(c))

    def moves(self, c):
        x, y, z = c
        out = []
        if self.fluid(c):
            for dy in (1, -1):
                n = (x, y + dy, z)
                if self.inside(n) and self.passable(n) and self.standable(n):
                    out.append(n)
        for dx, dz in DIRS.values():
            nx, nz = x + dx, z + dz
            up = (nx, y + 1, nz)
            if self.inside(up) and self.passable(up) and self.passable((nx, y + 2, nz)) and self.passable((x, y + 2, z)) \
                    and self.support((nx, y, nz)):
                out.append(up)
            n = (nx, y, nz)
            if not (self.inside(n) and self.passable(n) and self.passable((nx, y + 1, nz))):
                continue
            yy = y
            while yy > 0 and not self.support((nx, yy - 1, nz)) and not self.fluid((nx, yy, nz)):
                yy -= 1
            if yy <= 0 and not self.support((nx, yy - 1, nz)):
                continue
            if y - yy > self.max_fall:
                self.holes.add((n, y - yy))
                continue
            out.append((nx, yy, nz))
        return out

    def bfs(self, start, limit=None):
        seen = {start} if self.standable(start) else set()
        todo = deque(seen)
        while todo:
            c = todo.popleft()
            for n in self.moves(c):
                if n not in seen:
                    seen.add(n)
                    todo.append(n)
                    if limit and len(seen) > limit:
                        return seen
        return seen


def near(reach, c, dy=(0,)):
    """Can you stand right next to block c (to open a chest, use a frame, ...)?"""
    x, y, z = c
    return any((x + dx, y + d, z + dz) in reach for dx, dz in DIRS.values() for d in dy)


def attach_problems(b, reach=None):
    """Things that need something to hang on or stand on."""
    w = Walker(b)
    out = []
    for (x, y, z), (n, props, _) in b.s.blocks.items():
        if "lampion" in n or n in ("minecraft:lantern", "minecraft:soul_lantern"):
            other = (x, y + 1, z) if props.get("hanging") == "true" else (x, y - 1, z)
            on = w.name(other) or ""
            if not (w.support(other) or on.endswith("chain") or is_fency(on)):
                out.append(f"{n} op {(x, y, z)} hangt/staat nergens aan")
        elif n.endswith("_wall_sign"):
            dx, dz = DIRS[props["facing"]]
            if not w.support((x - dx, y, z - dz)) and not is_fency(w.name((x - dx, y, z - dz)) or ""):
                out.append(f"bordje op {(x, y, z)} hangt aan niks")
        elif n.endswith("_sign") or n.endswith("_door") and props.get("half") == "lower" or n.endswith("_carpet") \
                or n == "minecraft:chest" or n == SOKKEL or n.endswith("_bed"):
            if not (w.support((x, y - 1, z)) or is_fency(w.name((x, y - 1, z)) or "")):
                out.append(f"{n} op {(x, y, z)} staat nergens op")
        elif n == "minecraft:chain":
            if not (w.support((x, y + 1, z)) or w.name((x, y + 1, z)) == "minecraft:chain"):
                out.append(f"ketting op {(x, y, z)} hangt aan niks")
    return out


def floating(b, grounded):
    """Blocks that don't connect (through blocks) to a grounded one."""
    solid = {c for c, bl in b.s.blocks.items() if bl[0] != AIR}
    todo = [c for c in solid if grounded(c)]
    seen = set(todo)
    while todo:
        x, y, z = todo.pop()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if n in solid and n not in seen:
                seen.add(n)
                todo.append(n)
    return solid - seen


def light_map(b):
    blocks = b.s.blocks

    def opaque(c):
        bl = blocks.get(c)
        if bl is None:
            return b.unset_solid(c)
        n = bl[0]
        return n not in (AIR, SAUS) and not is_thin(n) and not any(t in n for t in TRANSPARENT_SUB)

    light, todo = {}, deque()
    for c, bl in blocks.items():
        if LIGHT.get(bl[0]):
            light[c] = LIGHT[bl[0]]
            todo.append(c)
    while todo:
        c = todo.popleft()
        l = light[c] - 1
        if l <= 0:
            continue
        x, y, z = c
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if b.inside(*n) and light.get(n, 0) < l and not opaque(n):
                light[n] = l
                todo.append(n)
    return light


def guh_mager(b, x, y, z, yaw=0.0):
    nbt = {"id": "guhs:guh", "Variant": "mager", "PersistenceRequired": b.h.Byte(1), "Age": 0,
           "Rotation": b.h.floats(float(yaw), 0.0)}
    b.ent(x + 0.5, float(y), z + 0.5, nbt, "mager")


def mika(b, x, y, z, yaw=0.0):
    nbt = {"id": "guhs:mika", "PersistenceRequired": b.h.Byte(1), "Rotation": b.h.floats(float(yaw), 0.0)}
    b.ent(x + 0.5, float(y), z + 0.5, nbt, "mika")


def door(b, x, y, z, facing, wood="oak", hinge="left"):
    b.set(x, y, z, f"{wood}_door", {"facing": facing, "half": "lower", "hinge": hinge, "open": "false", "powered": "false"})
    b.set(x, y + 1, z, f"{wood}_door", {"facing": facing, "half": "upper", "hinge": hinge, "open": "false", "powered": "false"})


# =====================================================================================================================
# 1. de knabbelkelder
# =====================================================================================================================
KW, KH, KD = 80, 26, 80
KF = 11                  # the main floor
KLO = 3                  # the floor of the cell block
KP = KF + 4              # the top of the portal plateau: the frames are at this height
PORTAL = (40, KP, 10)    # portal_center (template coordinates)
KC = (40, KF, 40)        # the anchor (the hall floor, in the middle of the guh mosaic)
KELDER_Y = 21            # start_height (absolute): the world y of the anchor
KORST_MIX = [("guhs:kaaskorst_stenen", 64), ("guhs:gebarsten_kaaskorst_stenen", 24), ("guhs:aangevreten_kaaskorst_stenen", 8),
             ("guhs:kaaskorst", 4)]

ROOMS = {  # zone: (x0, x1, z0, z1, floor, height of the air)
    "hal": (32, 48, 32, 48, KF, 9),
    "portaal": (31, 49, 4, 25, KF, 13),
    "bieb": (54, 74, 30, 50, KF, 9),
    "voorraad": (6, 19, 32, 48, KF, 6),
    "feest": (30, 50, 56, 72, KF, 7),
    "gang_n": (39, 41, 26, 31, KF, 4),
    "gang_o": (49, 53, 39, 41, KF, 4),
    "gang_w": (20, 31, 33, 35, KF, 4),
    "gang_z": (39, 41, 49, 55, KF, 4),
    "kerker": (32, 34, 45, 69, KLO, 4),
    "wacht": (29, 37, 70, 76, KLO, 4),
}
CELL_Z = (49, 53, 57, 61, 65)
CELLS = [("w", 28, z0) for z0 in CELL_Z] + [("o", 36, z0) for z0 in CELL_Z]     # (side, x0, z0): 3x3 inside


class Kelder(Bouw):
    def __init__(self, h):
        super().__init__(h, "knabbelkelder", (KW, KH, KD), lambda c: True, 20260925)
        self.hollow = {}
        self.shell_zone = {}
        self.guhs = []

    def dig(self, x, y, z, zone):
        if self.inside(x, y, z):
            self.hollow[(x, y, z)] = zone
            self.set(x, y, z, AIR)

    def box(self, x0, x1, y0, y1, z0, z1, zone):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.dig(x, y, z, zone)


def kelder_dig(k):
    for zone, (x0, x1, z0, z1, f, hgt) in ROOMS.items():
        k.box(x0, x1, f + 1, f + hgt, z0, z1, zone)
    for i, (side, x0, z0) in enumerate(CELLS):
        k.box(x0, x0 + 2, KLO + 1, KLO + 3, z0, z0 + 2, f"cel{i}")
    # the stairwell from the hall down to the cell block (x 32..34, top step at z = 37)
    for s in range(1, 8):
        k.box(32, 34, KF - s + 1, KF, 37 + s, 37 + s, "trap")


def kelder_shell(k):
    rng = k.rng
    for (x, y, z), zone in sorted(k.hollow.items()):
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    c = (x + dx, y + dy, z + dz)
                    if c not in k.hollow and c not in k.shell_zone and k.inside(*c):
                        k.shell_zone[c] = zone
    for c in sorted(k.shell_zone):
        k.set(*c, pick(rng, KORST_MIX))
    # floors
    for (x, y, z), zone in sorted(k.hollow.items()):
        if (x, y - 1, z) in k.hollow:
            continue
        c = (x, y - 1, z)
        if zone == "bieb":
            name = "dark_oak_planks" if (x + z) % 2 else "spruce_planks"
        elif zone == "feest":
            name = MSTEEN if (x // 2 + z // 2) % 2 else STEEN
        elif zone == "voorraad":
            name = pick(rng, [("guhs:kaaskorst", 40), ("guhs:gebarsten_kaaskorst_stenen", 30), ("cobblestone", 20), (STEEN, 10)])
        elif zone.startswith("cel") or zone in ("kerker", "wacht"):
            name = pick(rng, [("guhs:gebarsten_kaaskorst_stenen", 45), ("cobblestone", 25), (STEEN, 20), ("guhs:kaaskorst", 10)])
        elif zone == "hal":
            name = STEEN if (x + z) % 2 else "guhs:gebarsten_kaaskorst_stenen"
        else:
            name = STEEN if rng.random() < 0.8 else "guhs:gebarsten_kaaskorst_stenen"
        k.set(*c, name)


def kelder_hal(k):
    from features import kaasmijn
    # the guh mosaic in the middle of the floor, with a yellow border
    for x in range(34, 47):
        for z in range(35, 46):
            if not (x <= 34 and 37 <= z <= 44):          # (not over the stairs)
                k.set(x, KF, z, "guhs:guh_kristal_lamp" if (x in (36, 40, 44) and z in (35, 45)) or (x == 46 and z in (38, 42))
                      else "yellow_terracotta")
    face = kaasmijn.face_pattern(11, 9)
    pal = {"fur": "pink_concrete", "edge": "magenta_concrete", "ear": "pink_terracotta", "eye": "black_concrete",
           "shine": "white_concrete", "blush": "magenta_terracotta", "muzzle": "white_concrete", "nose": "magenta_concrete",
           "mouth": "black_concrete"}
    for x in range(35, 46):
        for z in range(36, 45):
            k.set(x, KF, z, "guhs:kaaskorst")
    for (i, j), part in face.items():
        k.set(35 + i, KF, 36 + j, pal[part])
    # the stairs down (x 32..34), with a wall around the hole
    for s in range(0, 8):
        for x in (32, 33, 34):
            k.set(x, KF - s, 37 + s, "guhs:kaaskorst_stenen_trap", stair("north"))
            for y in range(KLO + 1, KF - s):
                if (x, y, 37 + s) not in k.hollow:
                    k.set(x, y, 37 + s, STEEN)
    for z in range(38, 46):
        k.set(35, KF + 1, z, "guhs:kaaskorst_stenen_muur")
    for (y, z) in ((KF - 2, 40), (KF - 5, 43)):
        k.set(31, y, z, "guhs:guh_kristal_lamp")
    for x in (32, 33, 34):
        k.set(x, KF + 1, 45, "guhs:kaaskorst_stenen_muur")
    # pillars with crystal lamps in the corners
    for (x, z) in ((32, 32), (48, 32), (32, 48), (48, 48)):
        for y in range(KF + 1, KF + 10):
            k.set(x, y, z, "guhs:guh_kristal_lamp" if y in (KF + 4, KF + 8) else STEEN)
    # the chandelier
    for y in range(KF + 7, KF + 10):
        k.set(40, y, 40, "chain", {"axis": "y", "waterlogged": "false"})
    k.set(40, KF + 6, 40, "guhs:guh_kristal_lamp")
    for (x, z) in ((37, 37), (43, 37), (37, 43), (43, 43)):
        k.set(x, KF + 9, z, "guhs:lampion_geel", lamp(True))
    # the signs at the doorways
    k.sign(38, KF + 3, 32, "south", "hal_n", ["Het", "Knabbelportaal", "(naar het", "Guheinde!)"],
           ["The", "Nibble Portal", "(to the", "Guh End!)"])
    k.sign(48, KF + 3, 38, "west", "hal_o", ["De Knabbel-", "bibliotheek", "Stil zijn,", "njeg!"],
           ["The Nibble", "Library", "Be quiet,", "njeg!"])
    k.sign(32, KF + 3, 36, "east", "hal_w", ["De knabbel-", "voorraad", "(was vol,", "nu leeg)"],
           ["The nibble", "stores", "(were full,", "now empty)"])
    k.sign(42, KF + 3, 48, "north", "hal_z", ["Mika's", "feestzaal", "Guhs", "VERBODEN"],
           ["Mika's", "party hall", "Guhs", "FORBIDDEN"])
    k.sign(32, KF + 3, 42, "east", "hal_trap", ["Trap omlaag:", "het cellen-", "blok. Help de", "magere guhs!"],
           ["Stairs down:", "the cell", "block. Help the", "skinny guhs!"])
    k.sign(46, KF + 1, 35, "north", "hal_mid", ["Hier woonden", "vroeger", "VADSIGE guhs.", "Nu: njeg..."],
           ["Once, CHUBBY", "guhs lived", "here.", "Now: njeg..."], wall=False)


def kelder_portaal(k):
    cx, cy, cz = PORTAL
    # the plateau, with a wall around it and lamps on the corners
    for x in range(33, 48):
        for z in range(4, 18):
            for y in range(KF + 1, KP + 1):
                k.set(x, y, z, STEEN if y == KP and (x + z) % 3 else ("guhs:gebarsten_kaaskorst_stenen" if y == KP else "guhs:kaaskorst"))
    for z in range(4, 18):
        for x in (33, 47):
            k.set(x, KP + 1, z, "guhs:kaaskorst_stenen_muur")
    for x in list(range(33, 37)) + list(range(44, 48)):
        k.set(x, KP + 1, 17, "guhs:kaaskorst_stenen_muur")
    for (x, z) in ((33, 4), (47, 4), (33, 17), (47, 17)):
        k.set(x, KP + 1, z, STEEN)
        k.set(x, KP + 2, z, "guhs:guh_kristal_lamp")
    for (x, z) in ((33, 10), (47, 10)):
        k.set(x, KP + 2, z, "guhs:lampion_roze", lamp(False))
    # the kaassaus pool under the 3x3, the frames around it (they point to the middle)
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            k.set(x, cy, z, AIR)
            k.set(x, cy - 1, z, SAUS, {"level": "0"})
            k.set(x, cy - 2, z, SAUS, {"level": "0"})
            k.set(x, cy - 3, z, "guhs:kaaskorst")
    for d in (-1, 0, 1):
        k.set(cx + d, cy, cz - 2, FRAME, {"facing": "south", "oog": "false"})
        k.set(cx + d, cy, cz + 2, FRAME, {"facing": "north", "oog": "false"})
        k.set(cx - 2, cy, cz + d, FRAME, {"facing": "east", "oog": "false"})
        k.set(cx + 2, cy, cz + d, FRAME, {"facing": "west", "oog": "false"})
    for (x, z) in ((cx - 2, cz - 2), (cx + 2, cz - 2), (cx - 2, cz + 2), (cx + 2, cz + 2)):
        k.set(x, cy, z, "guhs:block_of_kaasknabbels")
    # the stairs up (x 37..43), the Mika larva spawner in the middle of them
    for i, z in enumerate((21, 20, 19, 18)):
        for x in range(37, 44):
            k.set(x, KF + 1 + i, z, "guhs:kaaskorst_stenen_trap", stair("north"))
            for y in range(KF + 1, KF + 1 + i):
                k.set(x, y, z, STEEN)
        for x in (36, 44):                                  # a balustrade on both sides
            for y in range(KF + 1, KF + 2 + i):
                k.set(x, y, z, STEEN)
            k.set(x, KF + 2 + i, z, "guhs:kaaskorst_stenen_muur")
    k.h.ms.spawner(k.s, 40, KF + 2, 20, "guhs:mika_larfje")
    # lamps: hanging from the ceiling, posts in the aisles next to the plateau
    for x in (35, 40, 45):
        for z in (7, 14, 22):
            k.set(x, KF + 13, z, "guhs:lampion_" + ("roze" if (x + z) % 2 else "geel"), lamp(True))
    for (x, z) in ((31, 6), (31, 12), (31, 18), (49, 6), (49, 12), (49, 18), (31, 24), (49, 24)):
        k.set(x, KF + 1, z, "guhs:kaaskorst_stenen_muur")
        k.set(x, KF + 2, z, "guhs:lampion_mint", lamp(False))
    # Mika's graffiti above the portal: a big angry Mika face on the north wall
    k.art(MIKA_FACE, GRAFFITI, "z", 3, 37, KP + 9)
    k.sign(42, KP + 1, 16, "south", "portaal", ["KNABBELPORTAAL", "12 Ogen van", "Vadsig erin =", "VAHOEG! weg"],
           ["NIBBLE PORTAL", "12 Eyes of", "Vadsig in =", "VAHOEG! away"], wall=False)
    k.sign(36, KF + 1, 22, "south", "saus", ["Pas op:", "kaassaus is", "HEET.", "(en lekker)"],
           ["Careful:", "cheese sauce", "is HOT.", "(and yummy)"], wall=False)
    k.sign(38, KF + 3, 25, "north", "portaal_mika", ["Dit portaal is", "van MIJ.", "Wegwezen!", "-Opper-Mika"],
           ["This portal is", "MINE.", "Get lost!", "-Top Mika"])


def kelder_bieb(k):
    x0, x1, z0, z1 = 54, 74, 30, 50
    # every wall is a bookshelf
    for c, zone in list(k.shell_zone.items()):
        x, y, z = c
        if zone in ("bieb", "gang_o") and KF + 1 <= y <= KF + 9 and x0 - 1 <= x <= x1 + 1 and z0 - 1 <= z <= z1 + 1 \
                and any(k.hollow.get((x + dx, y, z + dz)) == "bieb" for dx, dz in DIRS.values()):
            k.set(x, y, z, "bookshelf")
    # the gallery along the north wall (floor at KF + 5), posts under it, a railing
    G = KF + 5
    for x in range(x0, x1 + 1):
        for z in range(z0, z0 + 3):
            k.set(x, G, z, "dark_oak_planks")
    for x in range(57, x1 + 1, 4):
        for y in range(KF + 1, G):
            k.set(x, y, z0 + 2, "dark_oak_log", {"axis": "y"})
    for x in range(56, x1 + 1):
        k.set(x, G + 1, z0 + 2, "dark_oak_fence")
    # the stairs up to it (x 54..55), a bookshelf wall on their open side
    for i, z in enumerate((37, 36, 35, 34, 33)):
        y = KF + 1 + i
        for x in (54, 55):
            k.set(x, y, z, "dark_oak_stairs", stair("north"))
            for yy in range(KF + 1, y):
                k.set(x, yy, z, "dark_oak_planks")
        for yy in range(KF + 1, y + 3):
            k.set(56, yy, z, "bookshelf")
    # rows of shelves on the floor (with an aisle in the middle)
    for z in (38, 39, 43, 44):
        for x in range(59, 72):
            if x in (64, 65, 66):
                continue
            for y in range(KF + 1, KF + 4):
                k.set(x, y, z, "bookshelf")
    for x in range(59, 72):
        for z in (38, 39, 43, 44):
            if x not in (64, 65, 66):
                k.set(x, KF + 4, z, "dark_oak_slab", slab())
    # the reading corner: a lectern, a guh table and chairs, a purple runner
    k.set(65, KF + 1, 47, "lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    k.set(62, KF + 1, 48, "guhs:guh_tafel", {"facing": "south"})
    k.set(61, KF + 1, 48, "guhs:guh_stoel", {"facing": "east"})
    k.set(63, KF + 1, 48, "guhs:guh_stoel", {"facing": "west"})
    k.set(69, KF + 1, 48, "guhs:guh_tafel", {"facing": "south"})
    k.set(68, KF + 1, 48, "guhs:guh_stoel", {"facing": "east"})
    k.set(70, KF + 1, 48, "guhs:guh_stoel", {"facing": "west"})
    for x in range(54, 58):
        for z in (40,):
            k.set(x, KF + 1, z, "purple_carpet")
    for z in range(40, 47):
        if k.get(65, KF + 1, z) == AIR:
            k.set(65, KF + 1, z, "purple_carpet")
    k.chest(74, G + 1, 30, "west", "guhs:chests/knabbelkelder_bieb")
    k.chest(55, KF + 1, 50, "north", "guhs:chests/knabbelkelder_bieb")
    k.sign(65, KF + 2, 50, "north", "bieb", ["KNABBELBIEB", "Lenen mag.", "Opeten NIET.", "(hoor je Mika?)"],
           ["NIBBLE LIBRARY", "Borrowing: ok.", "Eating: NOT!", "(yes, Mika!)"])
    k.sign(70, G + 2, 29, "south", "bieb_galerij", ["Knabbel-", "recepten,", "deel 1 t/m 99", "(98 opgegeten)"],
           ["Nibble", "recipes,", "vol. 1 to 99", "(98 eaten)"])
    # lamps
    for x in range(57, 74, 4):
        for z in (36, 41, 46):
            if k.get(x, KF + 9, z) == AIR:
                k.set(x, KF + 9, z, "guhs:lampion_geel", lamp(True))
    for x in range(58, 75, 5):
        k.set(x, KF + 9, 31, "guhs:lampion_roze", lamp(True))
    for (x, z) in ((73, 49), (73, 35), (58, 49), (57, 41), (73, 41)):
        k.set(x, KF + 1, z, "dark_oak_fence")
        k.set(x, KF + 2, z, "guhs:lampion_geel", lamp(False))
    for x in range(56, 75, 4):                               # under the gallery
        k.set(x, G - 1, z0 + 1, "guhs:lampion_roze", lamp(True))
    for (x, z) in ((65, 36), (65, 41), (61, 41), (69, 41), (61, 46), (69, 46), (65, 49)):
        k.set(x, KF, z, "guhs:guh_kristal_lamp")


def kelder_voorraad(k):
    rng = random.Random(44)
    x0, x1, z0, z1 = 6, 19, 32, 48
    # empty barrels along the walls (some knocked over), cobwebs, a last few crumbs
    for z in range(z0, z1 + 1):
        if z % 3 == 0 or 32 <= z <= 36:
            continue
        for x in (x0, x1):
            k.set(x, KF + 1, z, "barrel", {"facing": "up" if rng.random() < 0.6 else rng.choice(["north", "east", "south", "west"]),
                                           "open": "true"})
            if rng.random() < 0.4:
                k.set(x, KF + 2, z, "barrel", {"facing": "up", "open": "true"})
    for x in range(x0 + 2, x1 - 1, 4):
        k.set(x, KF + 1, z0, "barrel", {"facing": "south", "open": "true"})
    for (x, z) in ((x0 + 1, z0 + 1), (x1 - 1, z1 - 1), (x0 + 1, z1 - 1), (x1 - 1, z0 + 1), (12, 40)):
        k.set(x, KF + 6, z, "cobweb")
    k.set(12, KF + 1, 44, "guhs:block_of_kaasknabbels")
    for (x, z) in ((11, 44), (13, 43), (12, 45), (14, 45), (10, 36)):
        k.set(x, KF + 1, z, "yellow_carpet")
    k.chest(x0 + 1, KF + 1, 40, "east", "guhs:chests/knabbelkelder_gang")
    # Mika was here
    k.art(MIKA_FACE, GRAFFITI, "x", x0 - 1, 37, KF + 6)
    k.art(MIKA_FACE, GRAFFITI, "z", z1 + 1, 9, KF + 6)
    k.sign(19, KF + 2, 38, "west", "voorraad", ["Hier lagen", "1000 knabbels.", "Nu: 0. NJEG!", "-Mika was hier"],
           ["Here lay", "1000 nibbles.", "Now: 0. NJEG!", "-Mika was here"])
    for x in (8, 13, 17):
        for z in (34, 41, 46):
            k.set(x, KF + 6, z, "guhs:lampion_geel", lamp(True))


def kelder_feest(k):
    x0, x1, z0, z1 = 30, 50, 56, 72
    top = KF + 7
    # Mika's long table: slabs with stolen nibbles, cake and candles; Mika stairs as chairs
    for x in range(34, 47):
        for z in (63, 64, 65):
            k.set(x, KF + 1, z, "guhs:mika_steen_plaat", slab("top"))
    for x in range(34, 47, 3):
        k.set(x, KF + 2, 64, "guhs:block_of_kaasknabbels")
    for x in (35, 41, 45):
        k.set(x, KF + 2, 63, "cake", {"bites": "3"})
    for x in (37, 43):
        k.set(x, KF + 2, 65, "guhs:lampion_roze", lamp(False))
    for x in range(34, 47, 2):
        k.set(x, KF + 1, 62, "guhs:mika_steen_trap", stair("north"))
        k.set(x, KF + 1, 66, "guhs:mika_steen_trap", stair("south"))
    # the head of the table: Mika's big chair
    k.set(32, KF + 1, 64, "guhs:mika_steen_trap", stair("west"))
    k.set(31, KF + 1, 64, "guhs:mika_steen_pilaar", {"axis": "y"})
    k.set(31, KF + 2, 64, "guhs:mika_steen_pilaar", {"axis": "y"})
    k.set(31, KF + 3, 64, "guhs:guh_kristal_lamp")
    # flags, graffiti, lamps
    for x in range(x0 + 1, x1, 3):
        k.set(x, top, 59, "guhs:vlaggetjes", {"axis": "x"})
        k.set(x, top, 69, "guhs:vlaggetjes", {"axis": "x"})
    k.art(MIKA_FACE, GRAFFITI, "z", z0 - 1, 33, KF + 6)
    k.art(MIKA_FACE, GRAFFITI, "z", z0 - 1, 43, KF + 6)
    k.art(MIKA_FACE, GRAFFITI, "z", z1 + 1, 37, KF + 6)
    for (x, z) in ((33, 58), (47, 58), (33, 70), (47, 70), (40, 60), (40, 68)):
        k.set(x, top, z, "guhs:lampion_roze", lamp(True))
    for x in (34, 40, 46):
        for z in (58, 70):
            k.set(x, KF, z, "guhs:guh_kristal_lamp")
    for x in (32, 48):
        for z in (61, 67):
            k.set(x, KF, z, "guhs:guh_kristal_lamp")
    for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        for y in range(KF + 1, top + 1):
            k.set(x, y, z, "guhs:mika_steen_pilaar" if y != KF + 4 else "guhs:guh_kristal_lamp", {"axis": "y"} if y != KF + 4 else None)
    k.chest(49, KF + 1, 64, "west", "guhs:chests/knabbelkelder_gang")
    k.sign(42, KF + 3, 56, "south", "feest", ["Mika's", "feestmaal!", "Guhs verboden.", "NJEG!"],
           ["Mika's", "feast!", "No guhs here.", "NJEG!"])
    k.sign(40, KF + 2, 64, "south", "feest_tafel", ["Knabbels:", "OP.", "Buikpijn:", "een beetje."],
           ["Nibbles:", "GONE.", "Tummy ache:", "a little."], wall=False)


def kelder_kerker(k):
    rng = random.Random(55)
    # the cells: an oak door, bars next to it, an empty food bowl, a lamp and a skinny guh
    for i, (side, x0, z0) in enumerate(CELLS):
        zc = z0 + 1
        wx = 31 if side == "w" else 35
        inner = x0 + 2 if side == "w" else x0        # the column next to the door
        far = x0 if side == "w" else x0 + 2
        door(k, wx, KLO + 1, zc, "east" if side == "w" else "west", hinge="left" if i % 2 else "right")
        for z in (zc - 1, zc + 1):
            k.set(wx, KLO + 2, z, "iron_bars")
        k.set(far, KLO + 1, z0, "guhs:knabbelbak", {"facing": "east" if side == "w" else "west"})
        k.set(far, KLO + 3, z0 + 2, "guhs:lampion_geel", lamp(True))
        k.set(far, KLO + 1, z0 + 2, "yellow_carpet")
        if i % 3 == 0:
            k.chest(far, KLO + 1, zc, "east" if side == "w" else "west", "guhs:chests/knabbelkelder_cel")
            gx = inner
        else:
            gx = x0 + 1
        guh_mager(k, gx, KLO + 1, zc, yaw=90 if side == "w" else -90)
        k.guhs.append((gx, KLO + 1, zc, i))
    # the corridor: lamps, a sign
    for z in range(46, 70, 4):
        k.set(33, KLO + 4, z, "guhs:lampion_geel", lamp(True))
    k.sign(34, KLO + 2, 46, "west", "cel", ["NJEG! Geen", "knabbels voor", "jullie.", "-Mika"],
           ["NJEG! No", "nibbles for", "you lot.", "-Mika"])
    k.sign(34, KLO + 2, 47, "west", "cel2", ["CELLENBLOK", "Magere guhs?", "Een knabbel", "= VAHOEG!"],
           ["CELL BLOCK", "Skinny guhs?", "One nibble", "= VAHOEG!"])
    # the guard room at the end
    k.set(33, KLO + 1, 74, "guhs:guh_tafel", {"facing": "south"})
    k.set(32, KLO + 1, 74, "guhs:mika_steen_trap", stair("west"))
    k.set(34, KLO + 1, 74, "guhs:mika_steen_trap", stair("east"))
    k.set(33, KLO + 2, 74, "cake", {"bites": "5"})
    for x in (29, 30):
        k.set(x, KLO + 1, 76, "barrel", {"facing": "up", "open": "false"})
    k.chest(37, KLO + 1, 76, "west", "guhs:chests/knabbelkelder_gang")
    k.sign(33, KLO + 2, 76, "north", "wacht", ["Sleutels van", "de cellen?", "Opgegeten.", "-Mika"],
           ["The cell", "keys?", "Eaten.", "-Mika"])
    for (x, z) in ((30, 71), (36, 71), (30, 75), (36, 75)):
        k.set(x, KLO + 4, z, "guhs:lampion_roze", lamp(True))
    k.art(MIKA_FACE, GRAFFITI, "x", 28, 71, KLO + 4)


def kelder_gangen(k):
    """Lamps in the corridors, crystal lamps in their walls."""
    for zone in ("gang_n", "gang_o", "gang_w", "gang_z"):
        x0, x1, z0, z1, f, hgt = ROOMS[zone]
        along_x = x1 - x0 > z1 - z0
        a0, a1 = (x0, x1) if along_x else (z0, z1)
        for a in range(a0 + 1, a1, 4):
            c = (a, f + hgt, (z0 + z1) // 2) if along_x else ((x0 + x1) // 2, f + hgt, a)
            if k.get(*c) == AIR:
                k.set(*c, "guhs:lampion_geel", lamp(True))


def kelder_worldgen(h):
    D = h.D
    # (2.7: also under the kaasmoeras and the vadswoud, and in the gatenkaasgrotten: the start is checked deep underground)
    lands = [f"guhs:{b}" for b in h.GUHMENSION_LAND + ["guh_peaks", "vads_cliffs", "mikas_biome", "guh_kristalmijn",
                                                        "kaasmoeras", "vadswoud", "gatenkaasgrotten"]]
    h.w(f"{D}/tags/worldgen/biome/has_structure/knabbelkelder.json", {"values": lands})
    h.w(f"{D}/tags/worldgen/structure/oog_van_vadsig_located.json", {"values": ["guhs:knabbelkelder"]})
    h.w(f"{D}/worldgen/structure/knabbelkelder.json", {
        "type": "minecraft:jigsaw", "biomes": "#guhs:has_structure/knabbelkelder", "step": "underground_structures",
        "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": [
            {"type": "guhs:mika_larfje", "weight": 1, "minCount": 1, "maxCount": 2}]}},
        "terrain_adaptation": "bury", "start_pool": "guhs:knabbelkelder/start", "size": 1,
        "start_height": {"absolute": KELDER_Y}, "start_jigsaw_name": "guhs:knabbelkelder_midden",
        "max_distance_from_center": 80, "use_expansion_hack": False})
    rules = [{"input_predicate": {"predicate_type": "minecraft:random_blockstate_match",
                                  "block_state": {"Name": FRAME, "Properties": {"facing": f, "oog": "false"}}, "probability": 0.1},
              "location_predicate": {"predicate_type": "minecraft:always_true"},
              "output_state": {"Name": FRAME, "Properties": {"facing": f, "oog": "true"}}}
             for f in ("north", "east", "south", "west")]
    h.w(f"{D}/worldgen/template_pool/knabbelkelder/start.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": "guhs:knabbelkelder",
                                  "projection": "rigid", "processors": {"processors": [
                                      {"processor_type": "minecraft:rule", "rules": rules}]}}}]})
    h.w(f"{D}/worldgen/structure_set/knabbelkelder.json", {
        "structures": [{"structure": "guhs:knabbelkelder", "weight": 1}],
        "placement": {"type": "minecraft:concentric_rings", "distance": 12, "spread": 4, "count": 4,
                      "preferred_biomes": "#guhs:has_structure/knabbelkelder", "salt": 20260925}})
    h.w(f"{D}/guheinde/knabbelkelder.json", {"portal_center": list(PORTAL)})


def kelder_check(k):
    p = []
    blocks = k.s.blocks
    if k.oob:
        p.append(f"{len(k.oob)} blokken buiten de template, bv. {k.oob[:4]}")
    # the anchor
    a = blocks.get(KC)
    if not a or a[0] != "minecraft:jigsaw" or a[2].get("name") != "guhs:knabbelkelder_midden":
        p.append(f"geen anker guhs:knabbelkelder_midden op {KC}")
    elif "jigsaw" in a[2]["final_state"] or "air" in a[2]["final_state"]:
        p.append(f"anker: final_state {a[2]['final_state']} is geen vloer")
    # between y = 8 and 40 in the world (either way the jigsaw ground delta works out)
    for bottom in (KELDER_Y - KF - 1, KELDER_Y - KF):
        if bottom < 8 or bottom + KH - 1 > 40:
            p.append(f"kelder komt buiten y 8..40 (onderkant {bottom})")
    # the portal: exactly 12 frames in the vanilla pattern, facing the middle, 3x3 open with kaassaus under it
    cx, cy, cz = PORTAL
    want = {}
    for d in (-1, 0, 1):
        want[(cx + d, cy, cz - 2)] = "south"
        want[(cx + d, cy, cz + 2)] = "north"
        want[(cx - 2, cy, cz + d)] = "east"
        want[(cx + 2, cy, cz + d)] = "west"
    frames = {c: bl for c, bl in blocks.items() if bl[0] == FRAME}
    if set(frames) != set(want):
        p.append(f"portaalframes kloppen niet: {len(frames)} frames, verwacht de 12 rond {PORTAL}")
    for c, f in want.items():
        bl = blocks.get(c)
        if bl and bl[0] == FRAME and (bl[1].get("facing") != f or bl[1].get("oog") != "false"):
            p.append(f"frame op {c}: {bl[1]} (moet facing={f}, oog=false)")
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            if k.get(x, cy, z) != AIR:
                p.append(f"portaalruimte {(x, cy, z)} is niet leeg")
            if k.get(x, cy - 1, z) != SAUS:
                p.append(f"geen kaassaus onder het portaal op {(x, cy - 1, z)}")
    spawners = [c for c, bl in blocks.items() if bl[0] == "minecraft:spawner"]
    if len(spawners) != 1 or blocks[spawners[0]][2]["SpawnData"]["entity"]["id"] != "guhs:mika_larfje":
        p.append(f"verwacht 1 Mika-larfjes-spawner, gevonden {len(spawners)}")
    # walking: from the hall to everything
    w = Walker(k)
    start = (40, KF + 1, 38)
    if not w.standable(start):
        p.append(f"start {start} is niet beloopbaar")
    reach = w.bfs(start)
    targets = {"portaalplateau": (40, KP + 1, 15), "naast de portaalframes": (40, KP + 1, 13), "voet van de trap": (40, KF + 1, 23),
               "bibliotheek": (65, KF + 1, 45), "galerij": (70, KF + 6, 31), "voorraad": (12, KF + 1, 40),
               "feestzaal": (40, KF + 1, 58), "cellengang": (33, KLO + 1, 67), "wachtkamer": (33, KLO + 1, 72),
               "achter de tafel": (38, KF + 1, 60)}
    for label, c in targets.items():
        if c not in reach:
            p.append(f"{label} op {c} is niet bereikbaar vanaf de hal")
    back = w.bfs((33, KLO + 1, 72))
    if start not in back:
        p.append("vanuit de wachtkamer kom je niet terug naar de hal")
    if spawners and not near(reach, spawners[0], dy=(0, 1, -1)):
        p.append("de spawner is niet bereikbaar")
    for (n, fall) in sorted(w.holes):
        p.append(f"gat: bij {n} val je {fall} blokken")
    # chests
    for c, bl in blocks.items():
        if bl[0] == "minecraft:chest" and not near(reach, c, dy=(0, -1, 1)):
            p.append(f"kist op {c} is niet bereikbaar")
    counts = {}
    for bl in blocks.values():
        if bl[0] == "minecraft:chest":
            counts[bl[2]["LootTable"]] = counts.get(bl[2]["LootTable"], 0) + 1
    for loot, lo in (("guhs:chests/knabbelkelder_gang", 2), ("guhs:chests/knabbelkelder_bieb", 1), ("guhs:chests/knabbelkelder_cel", 1)):
        if counts.get(loot, 0) < lo:
            p.append(f"te weinig kisten {loot}: {counts.get(loot, 0)}")
    if counts.get("guhs:chests/knabbelkelder_bieb", 0) > 2:
        p.append("meer dan 2 bieb-kisten")
    # the cells: each skinny guh stands inside a small closed room with an oak door, which opens onto the corridor
    closed = Walker(k, doors=False)
    cells_ok = 0
    for (x, y, z, i) in k.guhs:
        c = (x, y, z)
        if not w.standable(c):
            p.append(f"magere guh in cel {i} staat niet op een vloer ({c})")
            continue
        room = closed.bfs(c, limit=40)
        if len(room) > 9:
            p.append(f"cel {i}: de guh kan weglopen zonder deur ({len(room)} plekken)")
            continue
        doors = {(rx + dx, ry, rz + dz) for (rx, ry, rz) in room for dx, dz in DIRS.values()
                 if (k.get(rx + dx, ry, rz + dz) or "") == "minecraft:oak_door"}
        if not doors:
            p.append(f"cel {i}: geen eikenhouten deur")
            continue
        if c not in reach:
            p.append(f"cel {i}: niet bereikbaar door de deur")
            continue
        cells_ok += 1
    mager = [e for e in k.ents if e[3] == "mager"]
    if cells_ok < 6 or len(mager) != len(k.guhs):
        p.append(f"maar {cells_ok} goede cellen met een magere guh (minstens 6)")
    p += attach_problems(k)
    # light: every place you can walk has some light
    light = light_map(k)
    pit = {(x, y, z) for x in range(cx - 1, cx + 2) for z in range(cz - 1, cz + 2) for y in range(cy - 2, cy + 2)}
    dark = [c for c in reach if light.get(c, 0) < 5 and c not in pit]
    if dark:
        p.append(f"{len(dark)} donkere plekken, bv. {sorted(dark)[:12]}")
    k.stats = {"beloopbaar": len(reach), "blokken": len(blocks), "kisten": counts, "cellen": cells_ok,
               "frames": len(frames), "entities": len(k.ents)}
    return p


def build_kelder(h):
    k = Kelder(h)
    kelder_dig(k)
    kelder_shell(k)
    kelder_hal(k)
    kelder_portaal(k)
    kelder_bieb(k)
    kelder_voorraad(k)
    kelder_feest(k)
    kelder_kerker(k)
    kelder_gangen(k)
    k.connect()
    k.anchor(*KC, "guhs:knabbelkelder_midden")
    kelder_worldgen(h)
    problems = kelder_check(k)
    k.save()
    print(f"knabbelkelder: {k.stats}")
    return k, problems


# =====================================================================================================================
# 2. de knabbelberg (Java places it on the central island: template (CX, G, CZ) -> world (0, island height, 0))
# =====================================================================================================================
BW, BH, BD = 49, 48, 49
BCX = BCZ = 24
BG = 12
BTOP = BG + 20
PLATEAU_R = 12.5
PATH_T0 = 0.0            # the path starts east (0 degrees) at the foot and winds 300 degrees counterclockwise up
CRUST = [("guhs:kaaskorst", 62), ("yellow_terracotta", 14), ("yellow_concrete", 10), ("guhs:block_of_kaasknabbels", 14)]
POWDER = [("orange_concrete", 45), ("orange_terracotta", 35), ("guhs:block_of_kaasknabbels", 20)]
BITTEN = [("yellow_concrete", 30), ("honeycomb_block", 22), ("smooth_sandstone", 20), ("sandstone", 10), ("yellow_terracotta", 8),
          ("guhs:block_of_kaasknabbels", 10)]
ROOM = (20, 28, 20, 28)  # the treasure room (inside), floor at BG
GANG = (23, 25, 29, 48)  # the corridor to the south, floor at BG
SLOT_Z = 31


def bpolar(x, z):
    return math.hypot(x - BCX, z - BCZ), math.atan2(-(z - BCZ), x - BCX)


BLOB_YC, BLOB_RY, BLOB_R, BLOB_P = BG + 8, 15.0, 21.0, 2.0     # the puff: a squashed superellipsoid, its top bitten off at TOP


def blob_radius(y, th=None, waves=()):
    """How far the kaasknabbel reaches out at height y (and angle th, with its lumps)."""
    dy = abs(y - BLOB_YC) / BLOB_RY
    if dy >= 1:
        return 0.0
    R = BLOB_R + sum(amp * math.sin(n * th + k * y + ph) for ph, n, k, amp in waves) if th is not None else BLOB_R
    return R * (1 - dy ** BLOB_P) ** (1 / BLOB_P)


def powder(x, y, z):
    """Orange cheese powder comes in patches."""
    return math.sin(0.71 * x + 0.3 * y) + math.sin(0.63 * z - 0.4 * y + 1.3) + math.sin(0.5 * (x + z) + 0.9 * y + 0.4) > 1.35


def build_berg(h):
    b = Bouw(h, "guheinde_knabbelberg", (BW, BH, BD), lambda c: c[1] <= BG, 20260927)
    rng = b.rng
    waves = [(rng.uniform(0, 6.283), 3, 0.23, 1.5), (rng.uniform(0, 6.283), 5, -0.31, 0.9), (rng.uniform(0, 6.283), 8, 0.0, 0.5)]
    solid = set()
    # the root below G (it disappears into the island) and the apron at G
    for y in range(0, BG):
        rr = 9 + 13 * ((y + 1) / BG) ** 0.7
        for x in range(BW):
            for z in range(BD):
                r, th = bpolar(x, z)
                if r <= rr + 0.8 * math.sin(3 * th + y):
                    solid.add((x, y, z))
    for x in range(BW):
        for z in range(BD):
            r, th = bpolar(x, z)
            if r <= 24.5:
                solid.add((x, BG, z))
            for y in range(BG, BTOP + 1):
                if r <= blob_radius(y, th, waves) or (y == BTOP and r <= PLATEAU_R):
                    solid.add((x, y, z))
    # the crust lip around the bitten top (a bit higher than the bite)
    for x in range(BW):
        for z in range(BD):
            r, th = bpolar(x, z)
            if r > 10.8 and (x, BTOP, z) in solid and any((x + dx, BTOP, z + dz) not in solid for dx, dz in DIRS.values()):
                solid.add((x, BTOP + 1, z))
                if math.sin(5 * th) + math.sin(3 * th + 1) > 0.6:
                    solid.add((x, BTOP + 2, z))
    # bumps (a kaasknabbel is lumpy)
    for _ in range(34):
        th = rng.uniform(0, 6.283)
        y = rng.randint(BG + 3, BTOP - 4)
        r = blob_radius(y, th, waves)
        x, z = BCX + r * math.cos(th), BCZ - r * math.sin(th)
        rad = rng.uniform(2.2, 3.8)
        for xx in range(int(x - 4), int(x + 5)):
            for yy in range(y - 4, y + 5):
                for zz in range(int(z - 4), int(z + 5)):
                    if math.dist((xx, yy, zz), (x, y, z)) <= rad and BG + 3 < yy < BTOP:
                        solid.add((xx, yy, zz))
    # no overhang near the ground: the puff sits flat on the island
    for x in range(BW):
        for z in range(BD):
            ys = [y for y in range(BG + 1, BG + 9) if (x, y, z) in solid]
            for y in range(BG + 1, min(ys) if ys else BG + 1):
                solid.add((x, y, z))
    # the bites: one big scalloped bite in the side (tooth marks!), smaller ones in the rim
    bitten = set()
    for (deg, y, dist, rad, teeth) in ((205, BG + 11, 21, 8.0, 7), (35, BTOP + 1, 16.5, 4.2, 3), (110, BTOP + 1, 16.5, 3.6, 3),
                                       (290, BTOP + 1, 17.0, 3.8, 3), (160, BTOP, 17.5, 3.0, 0), (75, BG + 6, 21, 4.5, 4)):
        th = math.radians(deg)
        c = (BCX + dist * math.cos(th), y, BCZ - dist * math.sin(th))
        spheres = [(c, rad)]
        for i in range(teeth):
            a = th + math.pi + (i - (teeth - 1) / 2) * (1.9 / max(1, teeth - 1))
            spheres.append(((c[0] + rad * 0.92 * math.cos(a), y + rng.uniform(-1.2, 1.2), c[2] - rad * 0.92 * math.sin(a)), rad * 0.45))
        for (sc, sr) in spheres:
            for xx in range(int(sc[0] - sr - 1), int(sc[0] + sr + 2)):
                for yy in range(int(sc[1] - sr - 1), int(sc[1] + sr + 2)):
                    for zz in range(int(sc[2] - sr - 1), int(sc[2] + sr + 2)):
                        if math.dist((xx, yy, zz), sc) <= sr and yy > BG and (xx, yy, zz) in solid:
                            if math.hypot(xx - BCX, zz - BCZ) <= PLATEAU_R + 0.6 and yy <= BTOP:
                                continue          # the plateau stays whole
                            solid.discard((xx, yy, zz))
                            bitten.add((xx, yy, zz))
    # the treasure room and its corridor (and the protected zone around them)
    x0, x1, z0, z1 = ROOM
    room_air = {(x, y, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1) for y in range(BG + 1, BG + 6)}
    gx0, gx1, gz0, gz1 = GANG
    gang_air = {(x, y, z) for x in range(gx0, gx1 + 1) for z in range(gz0, gz1 + 1) for y in range(BG + 1, BG + 4)}
    for c in room_air | gang_air:
        solid.add((c[0], BG, c[2]))
    protect = {(x, z) for x in range(x0 - 2, x1 + 3) for z in range(z0 - 2, gz1 + 1)}
    for c in room_air | gang_air:
        solid.discard(c)
    # the path: a ledge winding 300 degrees up around the berg, from the foot (east) to the plateau
    N = 1500
    samples = []
    for i in range(N + 1):
        t = i / N
        th = math.radians(PATH_T0 + 300 * t)
        fl = BG + min(20, max(0, int(round(20 * (t - 0.03) / 0.92))))
        r = max(PLATEAU_R + 0.3, blob_radius(fl + 1) - 2.5) if t < 0.97 else PLATEAU_R
        samples.append((BCX + r * math.cos(th), BCZ - r * math.sin(th), fl, t, r))
    band, outside = {}, {}
    for x in range(BW):
        for z in range(BD):
            best = None
            for (sx, sz, fl, t, r) in samples[::3]:
                d = (x - sx) ** 2 + (z - sz) ** 2
                if best is None or d < best[0]:
                    best = (d, fl, t, r)
            rc = math.hypot(x - BCX, z - BCZ)
            if best[0] <= 2.3 ** 2:
                band[(x, z)] = (best[1], best[2], rc > best[3] + 1.3 and best[0] > 1.5 ** 2)
            elif best[0] <= 7 ** 2 and rc > best[3]:
                outside[(x, z)] = best[1]                # outward of the ledge: nothing above its floor
    for (x, z), fl in outside.items():
        for y in range(fl + 1, BH):
            solid.discard((x, y, z))
    for (x, z), (fl, t, outer) in band.items():
        for y in range(fl + 1, BH):
            solid.discard((x, y, z))
        solid.add((x, fl, z))
        low = BG + 7 if (x, z) in protect else BG
        y = fl - 1
        while y > low and (x, y, z) not in solid:
            solid.add((x, y, z))
            y -= 1
    # nothing may float (leftovers of the bites)
    grounded = {c for c in solid if c[1] <= BG}
    todo = list(grounded)
    while todo:
        x, y, z = todo.pop()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if n in solid and n not in grounded:
                grounded.add(n)
                todo.append(n)
    solid &= grounded
    # the plateau: flat at TOP, clear above it
    plateau_air = set()
    for x in range(BW):
        for z in range(BD):
            r = math.hypot(x - BCX, z - BCZ)
            if r <= PLATEAU_R:
                solid.add((x, BTOP, z))
                for y in range(BTOP + 1, BTOP + 13):
                    solid.discard((x, y, z))
                    plateau_air.add((x, y, z))

    # --- materials ---
    def exposed(c):
        x, y, z = c
        return any((x + dx, y + dy, z + dz) not in solid for dx, dy, dz in
                   ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)) if y + dy >= BG)

    for c in sorted(solid):
        x, y, z = c
        if y < BG:
            b.set(*c, "guhs:kaaskorst")
        elif exposed(c):
            near_bite = any((x + dx, y + dy, z + dz) in bitten for dx in (-1, 0, 1) for dy in (-1, 0, 1) for dz in (-1, 0, 1))
            cut = y == BTOP and (x, y + 1, z) not in solid          # the bitten-off top: the airy inside shows
            b.set(*c, pick(rng, BITTEN if near_bite or cut else POWDER if powder(x, y, z) else CRUST))
        else:
            b.set(*c, "guhs:kaaskorst")
    # the apron around the foot: plain kaaskorst
    for x in range(BW):
        for z in range(BD):
            r = math.hypot(x - BCX, z - BCZ)
            if r <= 24.5 and (x, BG + 1, z) not in solid:
                b.set(x, BG, z, "guhs:kaaskorst")
    # the path: kaaskorst bricks, stairs where it goes up, a low wall on the outside with lamps
    floors = {xz: v[0] for xz, v in band.items()}
    rail_n = 0
    for (x, z), (fl, t, outer) in sorted(band.items(), key=lambda kv: kv[1][1]):
        name = STEEN if rng.random() < 0.8 else "guhs:gebarsten_kaaskorst_stenen"
        props = None
        ups = [d for d, (dx, dz) in DIRS.items() if floors.get((x - dx, z - dz)) == fl - 1 and floors.get((x + dx, z + dz), fl) >= fl]
        if len(ups) == 1 and fl > BG:
            name, props = "guhs:kaaskorst_stenen_trap", stair(ups[0])
        if fl == BTOP and math.hypot(x - BCX, z - BCZ) <= PLATEAU_R:
            continue                                  # (the plateau floor is laid below)
        b.set(x, fl, z, name, props)
        if outer and 0.05 < t < 0.95 and props is None and not any(floors.get((x + dx, z + dz), -9) > fl for dx, dz in DIRS.values()):
            rail_n += 1
            b.set(x, fl + 1, z, "guhs:kaaskorst_stenen_muur")
            if rail_n % 9 == 4:
                b.set(x, fl + 2, z, "guhs:lampion_geel", lamp(False))

    # --- the plateau floor: a knabbel sun of kaaskorst bricks ---
    for x in range(BW):
        for z in range(BD):
            r, th = bpolar(x, z)
            if r > PLATEAU_R:
                continue
            ray = abs(((math.degrees(th) + 22.5) % 45) - 22.5) < 5.5 and r > 4
            if 8.2 <= r <= 9.2 or ray:
                name = STEEN
            elif r > 11.2:
                name = "guhs:gebarsten_kaaskorst_stenen"
            else:
                name = pick(rng, [("yellow_concrete", 40), ("smooth_sandstone", 25), ("honeycomb_block", 20), ("guhs:kaaskorst", 15)])
            b.set(x, BTOP, z, name)
            for y in range(BTOP + 1, BTOP + 13):
                b.set(x, y, z, AIR)
    # the return portal: 5x5 ring without corners, 8 portal cells (air), the pillar with the crystal lamp on top
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) == 2 and abs(dz) == 2:
                continue
            if max(abs(dx), abs(dz)) == 2:
                b.set(BCX + dx, BTOP, BCZ + dz, STEEN)
            elif (dx, dz) != (0, 0):
                b.set(BCX + dx, BTOP, BCZ + dz, AIR)
                b.set(BCX + dx, BTOP - 1, BCZ + dz, STEEN)
    for y in range(BTOP, BTOP + 5):
        b.set(BCX, y, BCZ, STEEN)
    b.set(BCX, BTOP + 5, BCZ, "guhs:guh_kristal_lamp")
    for (dx, dz) in ((3, 0), (-3, 0), (0, 3), (0, -3)):
        b.set(BCX + dx, BTOP, BCZ + dz, SOKKEL)
    # lamp posts on the rim (outside radius 10), away from where the path arrives
    arrive = PATH_T0 + 300
    for deg in range(0, 360, 45):
        if abs(((deg - arrive + 180) % 360) - 180) < 30:
            continue
        th = math.radians(deg + 22.5)
        x, z = int(round(BCX + 11.6 * math.cos(th))), int(round(BCZ - 11.6 * math.sin(th)))
        b.set(x, BTOP + 1, z, "guhs:kaaskorst_stenen_muur")
        b.set(x, BTOP + 2, z, "guhs:kaaskorst_stenen_muur")
        b.set(x, BTOP + 3, z, "guhs:lampion_geel", lamp(False))
    th = math.radians(arrive - 28)
    sx, sz = int(round(BCX + 11.6 * math.cos(th))), int(round(BCZ - 11.6 * math.sin(th)))
    b.sign(sx, BTOP + 1, sz, "north", "berg_top", ["DE KNABBELBERG", "Top gehaald!", "Pas op voor de", "Enderguh. Njeg."],
           ["NIBBLE MOUNTAIN", "Summit reached!", "Beware the", "Enderguh. Njeg."], wall=False)
    b.sign(int(round(BCX + 21.5)), BG + 1, BCZ + 2, "east", "berg_voet", ["Naar boven:", "het Knabbel-", "pad. Niet", "knabbelen!"],
           ["Up this way:", "the Nibble", "path. No", "nibbling!"], wall=False)

    # --- the treasure room ---
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            for y in range(BG, BG + 7):
                c = (x, y, z)
                if c in room_air:
                    b.set(*c, AIR)
                elif y == BG:
                    b.set(*c, "gold_block" if (x + z) % 2 else "yellow_concrete")
                elif y == BG + 6:
                    b.set(*c, STEEN)
                else:
                    corner = x in (x0 - 1, x1 + 1) and z in (z0 - 1, z1 + 1)
                    b.set(*c, "gold_block" if corner else ("guhs:block_of_kaasknabbels" if y == BG + 3 else STEEN))
    for (x, z) in ((24, 24), (22, 22), (26, 22), (22, 26), (26, 26)):
        b.set(x, BG + 6, z, "guhs:guh_kristal_lamp")
    b.set(24, BG, 24, "guhs:guh_kristal_blok")
    for (x, z, f) in ((20, 22, "east"), (20, 26, "east"), (28, 22, "west"), (28, 26, "west"), (22, 20, "south"), (26, 20, "south")):
        b.chest(x, BG + 1, z, f, "guhs:chests/knabbelschat")
    for (x, z, n) in ((20, 20, 3), (28, 20, 3), (20, 28, 2), (28, 28, 2), (21, 20, 1), (27, 20, 1), (20, 24, 2), (28, 24, 2)):
        for y in range(BG + 1, BG + 1 + n):
            b.set(x, y, z, "guhs:block_of_kaasknabbels")
    for (x, z, n) in ((24, 20, "guhs:guh_kristal_blok"), (23, 20, "gold_block"), (25, 20, "gold_block"), (21, 21, "raw_gold_block"),
                      (27, 21, "gold_block"), (21, 27, "guhs:guh_kristal_blok"), (27, 27, "raw_gold_block")):
        b.set(x, BG + 1, z, n)
    b.set(24, BG + 2, 20, "guhs:mikatrofee", {"facing": "south"})
    for (x, z) in ((21, 23), (27, 25)):
        b.set(x, BG + 1, z, "guhs:lampion_geel", lamp(False))
    # --- the corridor, the knabbelslot door, the sign ---
    for z in range(gz0, gz1 + 1):
        for x in range(gx0 - 1, gx1 + 2):
            for y in range(BG, BG + 5):
                c = (x, y, z)
                if c in gang_air:
                    b.set(*c, AIR)
                elif (c in solid or y == BG) and z <= 44:
                    b.set(*c, "gold_block" if y == BG + 4 and z in (gz0, 44) else STEEN)
    for z in range(33, 45, 4):
        for x in (gx0 - 1, gx1 + 1):
            if (x, BG + 2, z) in solid:
                b.set(x, BG + 2, z, "guhs:guh_kristal_lamp")
    for x in range(gx0, gx1 + 1):
        for y in range(BG + 1, BG + 4):
            b.set(x, y, SLOT_Z, SLOT)
    b.sign(gx0, BG + 2, SLOT_Z + 2, "east", "schat", ["Opper-Mika's", "knabbelvoorraad", "NIET AANKOMEN.", "NJEG."],
           ["Top Mika's", "nibble stash.", "DO NOT TOUCH.", "NJEG."])
    b.sign(gx1, BG + 2, SLOT_Z + 2, "west", "schat2", ["Sleutel:", "versla de", "Enderguh.", "(haha, succes)"],
           ["Key:", "defeat the", "Enderguh.", "(haha, luck!)"])
    b.connect()
    b.solid = solid
    b.band = band

    h.w(f"{h.D}/guheinde/knabbelberg.json", {"size": [BW, BH, BD], "cx": BCX, "cz": BCZ, "g": BG, "top": BTOP})
    problems = berg_check(b, room_air)
    b.save()
    print(f"guheinde_knabbelberg: {b.stats}")
    return b, problems


def berg_check(b, room_air):
    p = []
    blocks = b.s.blocks
    if b.oob:
        p.append(f"{len(b.oob)} blokken buiten de template, bv. {b.oob[:4]}")
    if (b.W, b.H, b.D) != (49, 48, 49):
        p.append("grootte is niet (49, 48, 49)")
    g = lambda x, y, z: b.get(x, y, z)
    air = lambda n: n in (None, AIR)
    # the portal: ring, 8 portal cells (air, with something solid under them), the pillar and the lamp
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            c = (BCX + dx, BTOP, BCZ + dz)
            if abs(dx) == 2 and abs(dz) == 2:
                continue
            if max(abs(dx), abs(dz)) == 2 and g(*c) != STEEN:
                p.append(f"portaalring: {c} is {g(*c)}")
            if max(abs(dx), abs(dz)) == 1:
                if g(*c) != AIR:
                    p.append(f"portaalplek {c} is geen lucht maar {g(*c)}")
                if not b.solid_at((c[0], BTOP - 1, c[2])):
                    p.append(f"onder portaalplek {c} zit geen vast blok")
    for y in range(BTOP, BTOP + 5):
        if g(BCX, y, BCZ) != STEEN:
            p.append(f"zuil: {(BCX, y, BCZ)} is {g(BCX, y, BCZ)}")
    if g(BCX, BTOP + 5, BCZ) != "guhs:guh_kristal_lamp":
        p.append("geen guh_kristal_lamp op de zuil")
    for (dx, dz) in ((3, 0), (-3, 0), (0, 3), (0, -3)):
        if g(BCX + dx, BTOP, BCZ + dz) != SOKKEL or not air(g(BCX + dx, BTOP + 1, BCZ + dz)):
            p.append(f"knabbelsokkel op {(BCX + dx, BTOP, BCZ + dz)} ontbreekt of is niet vrij")
    # the plateau: solid floor and air above it within radius 10 (except the pillar)
    for x in range(BW):
        for z in range(BD):
            if math.hypot(x - BCX, z - BCZ) > 10:
                continue
            portal = abs(x - BCX) <= 1 and abs(z - BCZ) <= 1 and (x, z) != (BCX, BCZ)
            if not portal and not b.solid_at((x, BTOP, z)):
                p.append(f"plateau heeft een gat op {(x, BTOP, z)}")
            for y in range(BTOP + 1, BTOP + 13):
                if (x, z) == (BCX, BCZ) and y <= BTOP + 5:
                    continue
                if not air(g(x, y, z)):
                    p.append(f"plateau niet vrij: {g(x, y, z)} op {(x, y, z)}")
    for x in range(BCX - 2, BCX + 2):
        for z in range(BCZ + 5, BCZ + 9):
            for y in range(BTOP + 1, BTOP + 9):
                if not air(g(x, y, z)):
                    p.append(f"landingsplek niet vrij op {(x, y, z)}")
    # the knabbelslot door: exactly a 3x3 plane
    slots = sorted(c for c, bl in blocks.items() if bl[0] == SLOT)
    want = sorted((x, y, SLOT_Z) for x in range(GANG[0], GANG[1] + 1) for y in range(BG + 1, BG + 4))
    if slots != want:
        p.append(f"knabbelslot-deur is geen 3x3 vlak: {slots[:12]}")
    # walking: up the path to the plateau and back down; the treasure room only through the knabbelslot
    w = Walker(b)
    start = (46, BG + 1, BCZ)
    if not w.standable(start):
        p.append(f"start {start} aan de voet is niet beloopbaar")
    reach = w.bfs(start)
    tops = [(BCX + 5, BTOP + 1, BCZ + 1), (BCX, BTOP + 1, BCZ + 7), (BCX - 6, BTOP + 1, BCZ - 3)]
    for c in tops:
        if c not in reach:
            p.append(f"plateau {c} is niet te voet bereikbaar vanaf de voet")
    for (dx, dz) in ((3, 0), (-3, 0), (0, 3), (0, -3)):
        if not near(reach, (BCX + dx, BTOP + 1, BCZ + dz)):
            p.append(f"sokkel {(BCX + dx, BCZ + dz)} is niet bereikbaar")
    down = w.bfs(tops[0])
    if start not in down:
        p.append("vanaf het plateau kom je niet te voet terug naar de voet")
    mouth = (BCX, BG + 1, 48)
    if not w.standable(mouth) or mouth not in reach:
        p.append(f"de ingang van de schatgang {mouth} is niet bereikbaar")
    inside = (24, BG + 1, 25)
    closed = w.bfs(mouth)
    if inside in closed or any(c in closed for c in room_air):
        p.append("de schatkamer is bereikbaar zonder door het knabbelslot")
    opened = Walker(b, slot=True).bfs(mouth)
    if inside not in opened:
        p.append("de schatkamer is ook met open knabbelslot niet bereikbaar")
    chests = [c for c, bl in blocks.items() if bl[0] == "minecraft:chest"]
    if len(chests) < 5 or any(blocks[c][2]["LootTable"] != "guhs:chests/knabbelschat" for c in chests):
        p.append(f"verwacht ~6 knabbelschat-kisten, gevonden {len(chests)}")
    for c in chests:
        if not near(opened, c):
            p.append(f"schatkist op {c} is niet bereikbaar")
    light = light_map(b)
    dark = [c for c in opened if c in room_air and light.get(c, 0) < 5]
    if dark:
        p.append(f"{len(dark)} donkere plekken in de schatkamer")
    # nothing floats; nothing below G sticks out of the root
    fl = floating(b, lambda c: c[1] <= BG)
    if fl:
        p.append(f"{len(fl)} zwevende blokken, bv. {sorted(fl)[:5]}")
    p += attach_problems(b)
    b.stats = {"blokken": len(blocks), "beloopbaar": len(reach), "pad": len(b.band), "kisten": len(chests)}
    return p


# =====================================================================================================================
# 3. de Mika-vesting (and the vetschip)
# =====================================================================================================================
VW, VH, VD = 33, 48, 33          # the tower alone; its middle (16, 16) is the anchor
SW, SH, SD = 80, 56, 64          # with the ship: the tower in the middle, the ship to the east
TOX, TOZ = 24, 16                # where the tower goes in the ship template
SX, DK = 64, 36                  # the ship: its middle line and its deck
FLOORS = (0, 9, 18, 27, 36)      # floor levels of the tower (36 = the roof)


def lane_steps():
    """The stairs around the tower: [(x, z, floor, facing or None)] (tower coordinates). A 2 wide lane, 1 up per block,
    a landing (and a door) at every floor."""
    out = []
    for i, z in enumerate(range(21, 10, -1)):                 # east side, north: 0 -> 9
        for x in (22, 23):
            out.append((x, z, min(9, i + 1), "north" if i + 1 <= 9 else None))
    for x in (22, 23):
        for z in (9, 10):
            out.append((x, z, 9, None))
    for i, x in enumerate(range(21, 10, -1)):                 # north side, west: 9 -> 18
        for z in (9, 10):
            out.append((x, z, min(18, 10 + i), "west" if i + 1 <= 9 else None))
    for x in (9, 10):
        for z in (9, 10):
            out.append((x, z, 18, None))
    for i, z in enumerate(range(11, 22)):                     # west side, south: 18 -> 27
        for x in (9, 10):
            out.append((x, z, min(27, 19 + i), "south" if i + 1 <= 9 else None))
    for x in (9, 10):
        for z in (22, 23):
            out.append((x, z, 27, None))
    for i, x in enumerate(range(11, 22)):                     # south side, east: 27 -> 36 (the roof)
        for z in (22, 23):
            out.append((x, z, min(36, 28 + i), "east" if i + 1 <= 9 else None))
    return out


class Shift:
    """Tower coordinates -> template coordinates."""

    def __init__(self, b, ox, oz):
        self.b, self.ox, self.oz = b, ox, oz
        self.h = b.h

    def set(self, x, y, z, *a, **k):
        self.b.set(x + self.ox, y, z + self.oz, *a, **k)

    def get(self, x, y, z):
        return self.b.get(x + self.ox, y, z + self.oz)

    def chest(self, x, y, z, *a):
        self.b.chest(x + self.ox, y, z + self.oz, *a)

    def sign(self, x, y, z, *a, **k):
        self.b.sign(x + self.ox, y, z + self.oz, *a, **k)

    def art(self, rows, pal, plane, fixed, u0, ytop):
        if plane == "x":
            self.b.art(rows, pal, "x", fixed + self.ox, u0 + self.oz, ytop)
        else:
            self.b.art(rows, pal, "z", fixed + self.oz, u0 + self.ox, ytop)


def tower(b, ox, oz, roof_gap_east=()):
    t = Shift(b, ox, oz)
    rng = random.Random(61)
    # the plaza
    for x in range(6, 27):
        for z in range(6, 27):
            if abs(x - 16) + abs(z - 16) > 17:
                continue
            ring = max(abs(x - 16), abs(z - 16))
            t.set(x, 0, z, "guhs:mika_steen_pilaar" if ring in (7, 10) else MSTEEN, {"axis": "y"} if ring in (7, 10) else None)
    for j, row in enumerate(MIKA_FACE):                          # Mika's face in the floor of the guard room
        for i, ch in enumerate(row):
            t.set(13 + i, 0, 14 + j, GRAFFITI[ch])
    # the walls, floors, corner pillars and trim bands
    for y in range(1, 37):
        for x in range(11, 22):
            for z in range(11, 22):
                edge = x in (11, 21) or z in (11, 21)
                corner = x in (11, 21) and z in (11, 21)
                if y in FLOORS:
                    if corner:
                        t.set(x, y, z, "guhs:mika_steen_pilaar", {"axis": "y"})
                    elif edge:
                        t.set(x, y, z, "guhs:mika_steen_pilaar", {"axis": "x" if z in (11, 21) else "z"})
                    else:
                        t.set(x, y, z, MSTEEN if (x + z) % 4 else "guhs:guh_kristal_lamp" if y < 36 else MSTEEN)
                elif corner:
                    t.set(x, y, z, "guhs:mika_steen_pilaar", {"axis": "y"})
                elif edge:
                    t.set(x, y, z, MSTEEN)
                else:
                    t.set(x, y, z, AIR)
    # the entrance (south) and the doors onto the stairs
    for x in (15, 16, 17):
        for y in (1, 2, 3):
            t.set(x, y, 21, AIR)
    t.set(15, 4, 21, "guhs:mika_steen_trap", stair("east", "top"))
    t.set(17, 4, 21, "guhs:mika_steen_trap", stair("west", "top"))
    for (x, y, z) in ((21, 10, 12), (12, 19, 11), (11, 28, 20)):
        t.set(x, y, z, AIR)
        t.set(x, y + 1, z, AIR)
    # graffiti outside (and so inside too), windows where the wall is still plain
    t.art(MIKA_FACE, GRAFFITI, "z", 21, 13, 17)
    t.art(MIKA_FACE, GRAFFITI, "x", 21, 13, 26)
    t.art(MIKA_FACE, GRAFFITI, "z", 11, 13, 34)
    t.art(MIKA_FACE, GRAFFITI, "x", 11, 13, 16)
    for fy in FLOORS[:4]:
        for (x, z) in ((16, 11), (16, 21), (11, 16), (21, 16), (13, 11), (19, 21), (11, 19), (21, 13)):
            for y in (fy + 3, fy + 4):
                if t.get(x, y, z) == MSTEEN:
                    t.set(x, y, z, "purple_stained_glass_pane")
    # the stairs around the tower
    for (x, z, fl, facing) in lane_steps():
        if facing:
            t.set(x, fl, z, "guhs:mika_steen_trap", stair(facing))
            if fl >= 3:
                t.set(x, fl - 1, z, "guhs:mika_steen_trap", stair(OPP[facing], "top"))
        else:
            t.set(x, fl, z, MSTEEN)
            if fl >= 3:
                t.set(x, fl - 1, z, "guhs:mika_steen_plaat", slab("top"))
        for y in range(1, fl - (1 if fl >= 3 else 0)):
            if fl <= 3:
                t.set(x, y, z, MSTEEN)
    # the roof: battlements, lamps on the corners, the flag
    for x in range(11, 22):
        for z in range(11, 22):
            if x in (11, 21) or z in (11, 21):
                corner = x in (11, 21) and z in (11, 21)
                gap = (z == 21 and x in (19, 20)) or (x == 21 and z in roof_gap_east)
                if corner:
                    t.set(x, 37, z, "guhs:mika_steen_pilaar", {"axis": "y"})
                    t.set(x, 38, z, "guhs:lampion_roze", lamp(False))
                elif not gap and (x + z) % 2 == 0:
                    t.set(x, 37, z, MSTEEN)
    for y in range(37, 46):
        t.set(16, y, 16, "crimson_fence")
    t.art(MIKA_FACE, GRAFFITI, "z", 16, 17, 45)
    t.sign(16, 37, 17, "south", "vlag", ["Vesting van", "de Mika's.", "Guhs? NJEG!", ""],
           ["Fortress of", "the Mikas.", "Guhs? NJEG!", ""], wall=False, wood="crimson")
    # --- the rooms ---
    # ground floor: the guard room
    for z in (20,):
        for x in (15, 16, 17):
            t.set(x, 1, z, "purple_carpet")
    t.chest(12, 1, 13, "east", "guhs:chests/mika_vesting")
    for (x, z) in ((20, 12), (20, 13), (12, 20)):
        t.set(x, 1, z, "barrel", {"facing": "up", "open": "false"})
    t.set(16, 8, 16, "guhs:lampion_roze", lamp(True))
    t.set(13, 8, 18, "guhs:lampion_roze", lamp(True))
    t.sign(16, 3, 12, "south", "gezocht", ["GEZOCHT:", "vadsige guhs.", "Beloning:", "0 knabbels."],
           ["WANTED:", "chubby guhs.", "Reward:", "0 nibbles."], wood="crimson")
    mika(b, 16 + ox, 1, 15 + oz, 180)
    # floor 2: the dormitory
    for x in (14, 18):
        t.set(x, 10, 12, "purple_bed", {"facing": "north", "part": "head", "occupied": "false"})
        t.set(x, 10, 13, "purple_bed", {"facing": "north", "part": "foot", "occupied": "false"})
    t.chest(20, 10, 18, "west", "guhs:chests/mika_vesting")
    t.set(16, 17, 16, "guhs:lampion_roze", lamp(True))
    t.set(13, 10, 19, "purple_carpet")
    t.sign(16, 12, 20, "north", "slaap", ["Stil! Mika", "slaapt.", "(of doet", "alsof)"],
           ["Shh! Mika", "is asleep.", "(or pretends", "to be)"], wood="crimson")
    # floor 3: the stolen nibbles
    for (x, z, n) in ((13, 18, 3), (13, 19, 2), (14, 19, 2), (13, 20, 1), (19, 19, 2), (19, 20, 3), (20, 20, 2), (18, 20, 1)):
        for y in range(19, 19 + n):
            t.set(x, y, z, "guhs:block_of_kaasknabbels")
    for (x, z) in ((20, 12), (20, 13), (19, 12)):
        t.set(x, 19, z, "barrel", {"facing": "up", "open": "false"})
    t.chest(20, 19, 16, "west", "guhs:chests/mika_vesting")
    t.set(16, 26, 16, "guhs:lampion_geel", lamp(True))
    t.sign(16, 21, 20, "north", "depot", ["Gestolen? NEE.", "Geleend.", "Voor altijd.", "-Mika"],
           ["Stolen? NO.", "Borrowed.", "Forever.", "-Mika"], wood="crimson")
    # floor 4: the throne room
    t.set(16, 28, 13, "guhs:mika_steen_trap", stair("north"))
    for y in (28, 29, 30):
        t.set(16, y, 12, "guhs:mika_steen_pilaar", {"axis": "y"})
    t.set(16, 31, 12, "guhs:guh_kristal_lamp")
    for x in (15, 17):
        t.set(x, 28, 13, "guhs:mika_steen_plaat", slab())
        t.set(x, 28, 12, "guhs:mika_steen_pilaar", {"axis": "y"})
        t.set(x, 29, 12, "guhs:mika_steen_pilaar", {"axis": "y"})
    for z in range(14, 21):
        t.set(16, 28, z, "purple_carpet")
    t.chest(13, 28, 13, "east", "guhs:chests/mika_vesting")
    t.set(14, 35, 16, "guhs:lampion_roze", lamp(True))
    t.set(18, 35, 16, "guhs:lampion_roze", lamp(True))
    t.sign(19, 30, 12, "south", "troon", ["TROON VAN", "MIKA", "Niet zitten!", "(ook niet even)"],
           ["THRONE OF", "MIKA", "No sitting!", "(not even once)"], wood="crimson")
    mika(b, 16 + ox, 28, 15 + oz, 0)


def mika_face_rows(w, hgt, bg):
    """MIKA_FACE centred on a w x hgt sail/flag of colour bg."""
    rows = []
    fy, fx = (hgt - len(MIKA_FACE)) // 2, (w - len(MIKA_FACE[0])) // 2
    for j in range(hgt):
        row = ""
        for i in range(w):
            if 0 <= j - fy < len(MIKA_FACE) and 0 <= i - fx < len(MIKA_FACE[0]):
                row += MIKA_FACE[j - fy][i - fx]
            else:
                row += bg
        rows.append(row)
    return rows


def ship_half(z):
    if z < 14 or z > 50:
        return None
    if z <= 22:
        return 5.0 * (z - 13) / 9
    if z <= 44:
        return 5.0
    return 5.0 - (z - 44) * 0.5


def ship(b):
    rng = random.Random(71)
    hull = set()
    for z in range(14, 51):
        hw = ship_half(z)
        for x in range(SX - 6, SX + 7):
            u = x - SX
            if abs(u) > hw + 0.3:
                continue
            bottom = DK - 6 + max(0, abs(u) - 2) + (max(0, 22 - z) // 2)
            for y in range(bottom, DK):
                hull.add((x, y, z))
    for c in sorted(hull):
        x, y, z = c
        out = any((x + dx, y + dy, z + dz) not in hull for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)))
        if not out:
            b.set(*c, "brown_terracotta")
        elif y == DK - 1:
            b.set(*c, "honey_block")
        elif abs(x - SX) <= 1 and (x, y - 1, z) not in hull:
            b.set(*c, "honeycomb_block")
        else:
            b.set(*c, pick(rng, [("orange_terracotta", 40), ("yellow_terracotta", 30), ("brown_terracotta", 30)]))
    # the deck and the bulwark
    for z in range(14, 51):
        hw = ship_half(z)
        for x in range(SX - 6, SX + 7):
            u = x - SX
            if abs(u) > hw + 0.3:
                continue
            rim = abs(u) > hw - 0.7 or z in (14, 50)
            b.set(x, DK, z, "brown_terracotta" if rim else ("yellow_terracotta" if u % 2 else "orange_terracotta"))
            if rim and not (x == SX - 5 and 30 <= z <= 34):
                b.set(x, DK + 1, z, "honeycomb_block" if z % 5 else "brown_terracotta")
    for (x, z) in ((SX - 2, 50), (SX + 2, 50), (SX - 4, 45), (SX + 4, 45)):
        b.set(x, DK + 2, z, "guhs:lampion_geel", lamp(False))
    # the hold: 2 chests of vet, reached by stairs through a hatch
    for x in range(SX - 3, SX + 4):
        for z in range(30, 47):
            for y in range(DK - 4, DK):
                b.set(x, y, z, AIR)
    for i, z in enumerate(range(37, 42)):
        y = DK - i
        for x in (SX - 2, SX - 1):
            b.set(x, y, z, "spruce_stairs", stair("north"))
            for yy in range(DK - 4, y):
                b.set(x, yy, z, "brown_terracotta")
            if i >= 1:
                b.set(x, DK, z, AIR)
    for z in range(38, 42):
        b.set(SX - 3, DK + 1, z, "spruce_fence")
        b.set(SX, DK + 1, z, "spruce_fence")
    for x in (SX - 3, SX - 2, SX - 1, SX):
        b.set(x, DK + 1, 42, "spruce_fence")
    b.chest(SX - 3, DK - 4, 32, "east", "guhs:chests/vetschip")
    b.chest(SX + 3, DK - 4, 34, "west", "guhs:chests/vetschip")
    for (x, z) in ((SX + 3, 31), (SX + 3, 44), (SX - 3, 45), (SX + 2, 45)):
        b.set(x, DK - 4, z, "barrel", {"facing": "up", "open": "false"})
    b.set(SX + 3, DK - 4, 40, "honey_block")
    b.set(SX + 3, DK - 3, 40, "honey_block")
    for (x, z) in ((SX, 33), (SX + 1, 44), (SX - 2, 31)):
        b.set(x, DK - 1, z, "guhs:lampion_geel", lamp(True))
    b.sign(SX + 3, DK - 3, 36, "west", "vet", ["VETVOORRAAD", "Niet likken!", "(Mika likt", "wel stiekem)"],
           ["GREASE STORE", "No licking!", "(Mika licks", "it secretly)"], wood="spruce")
    # masts, yards and brown sails with Mika's face
    for (mz, top, sail_w, sail_h, face) in ((30, 14, 9, 9, True), (36, 12, 7, 7, False)):
        for y in range(DK + 1, DK + top + 1):
            b.set(SX, y, mz, "stripped_dark_oak_log", {"axis": "y"})
        for x in range(SX - sail_w // 2 - 1, SX + sail_w // 2 + 2):
            if x != SX:
                b.set(x, DK + top - 1, mz, "stripped_dark_oak_log", {"axis": "x"})
        rows = mika_face_rows(sail_w, sail_h, "b") if face else ["b" * sail_w if j % 3 else "o" * sail_w for j in range(sail_h)]
        pal = {"b": "brown_wool", "o": "orange_wool", "p": "pink_wool", "K": "black_wool", "R": "red_wool", "W": "white_wool"}
        ytop = DK + top - 2
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                x = SX - sail_w // 2 + i
                if x != SX:
                    b.set(x, ytop - j, mz, pal[ch])
    b.set(SX, DK + 15, 30, "purple_wool")
    # the figurehead: a Mika head at the bow
    for x in (SX - 1, SX, SX + 1):
        for y in (DK - 1, DK, DK + 1):
            for z in (11, 12, 13):
                b.set(x, y, z, "pink_wool")
    for (x, y, n) in ((SX - 1, DK + 1, "black_wool"), (SX + 1, DK + 1, "black_wool"), (SX - 1, DK, "red_wool"),
                      (SX + 1, DK, "red_wool"), (SX, DK - 1, "white_wool")):
        b.set(x, y, 11, n)
    # the bow cabin: the Guhvleugels in an item frame, a Mika trophy
    for x in range(SX - 3, SX + 4):
        for z in range(21, 28):
            for y in range(DK + 1, DK + 5):
                wall = x in (SX - 3, SX + 3) or z in (21, 27)
                if y == DK + 4:
                    b.set(x, y, z, "orange_terracotta" if wall else "brown_terracotta")
                elif wall:
                    b.set(x, y, z, "yellow_terracotta" if y != DK + 1 else "brown_terracotta")
                else:
                    b.set(x, y, z, AIR)
    b.set(SX, DK + 1, 27, AIR)
    b.set(SX, DK + 2, 27, AIR)
    for (x, z) in ((SX - 3, 24), (SX + 3, 24)):
        b.set(x, DK + 2, z, "yellow_stained_glass_pane")
    b.set(SX, DK + 1, 22, "guhs:mikatrofee", {"facing": "south"})
    b.ent(SX + 0.5, DK + 2.5, 22.03125, {"id": "minecraft:item_frame", "Facing": b.h.Byte(FACING_BYTE["south"]),
                                          "Item": {"id": "guhs:guhvleugels", "count": 1}, "Invulnerable": b.h.Byte(1),
                                          "Fixed": b.h.Byte(0)}, "frame")
    b.frame_cell = (SX, DK + 2, 22)
    b.set(SX, DK + 3, 25, "guhs:lampion_roze", lamp(True))
    b.sign(SX + 2, DK + 2, 23, "west", "vleugels", ["Guhvleugels!", "Van Mika.", "AFBLIJVEN.", "(njeg!)"],
           ["Guh Wings!", "Mika's.", "HANDS OFF.", "(njeg!)"], wood="spruce")
    mika(b, SX + 2, DK + 1, 33, -90)
    # the bridge from the roof of the tower to the ship
    for x in range(TOX + 22, SX - 5):
        for z in range(30, 35):
            b.set(x, DK, z, MSTEEN if z in (31, 32, 33) else "guhs:mika_steen_pilaar", {"axis": "x"} if z in (30, 34) else None)
        for z in (30, 34):
            b.set(x, DK + 1, z, "crimson_fence")
            if (x - TOX) % 4 == 0:
                b.set(x, DK + 2, z, "guhs:lampion_roze", lamp(False))


def vesting_worldgen(h):
    D = h.D
    h.w(f"{D}/worldgen/structure/mika_vesting.json", {
        "type": "guhs:guheinde_eiland", "biomes": "#guhs:has_structure/mika_vesting", "step": "surface_structures",
        "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": [
            {"type": "guhs:mika", "weight": 1, "minCount": 1, "maxCount": 2}]}},
        "terrain_adaptation": "none", "min_distance": 1100, "min_surface_y": 50, "check_radius": 10,
        "jigsaw": {"type": "minecraft:jigsaw", "biomes": "#guhs:has_structure/mika_vesting", "step": "surface_structures",
                   "spawn_overrides": {}, "terrain_adaptation": "none", "start_pool": "guhs:mika_vesting/start", "size": 1,
                   "start_height": {"absolute": 0}, "project_start_to_heightmap": "WORLD_SURFACE_WG",
                   "start_jigsaw_name": "guhs:mika_vesting_midden", "max_distance_from_center": 64, "use_expansion_hack": False}})
    h.w(f"{D}/worldgen/template_pool/mika_vesting/start.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": "guhs:mika_vesting",
                                  "projection": "rigid", "processors": "minecraft:empty"}},
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": "guhs:mika_vesting_schip",
                                  "projection": "rigid", "processors": "minecraft:empty"}}]})
    h.w(f"{D}/worldgen/structure_set/mika_vesting.json", {
        "structures": [{"structure": "guhs:mika_vesting", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 20, "separation": 8, "salt": 20260926}})
    h.w(f"{D}/tags/worldgen/biome/has_structure/mika_vesting.json", {"values": ["guhs:guheinde"]})


def vesting_check(b, ox, oz, with_ship):
    p = []
    blocks = b.s.blocks
    if b.oob:
        p.append(f"{len(b.oob)} blokken buiten de template, bv. {b.oob[:4]}")
    ax, az = ox + 16, oz + 16
    a = blocks.get((ax, 0, az))
    if not a or a[0] != "minecraft:jigsaw" or a[2].get("name") != "guhs:mika_vesting_midden":
        p.append(f"geen anker guhs:mika_vesting_midden op {(ax, 0, az)}")
    if (ax, az) != (b.W // 2, b.D // 2):
        p.append(f"anker {(ax, az)} staat niet in het midden van de template {(b.W // 2, b.D // 2)}")
    if b.H > 64 or b.D > 64 or b.W > (80 if with_ship else 64):
        p.append(f"template te groot: {(b.W, b.H, b.D)}")
    w = Walker(b)
    start = (ax, 1, oz + 25)
    if not w.standable(start):
        p.append(f"start {start} op het plein is niet beloopbaar")
    reach = w.bfs(start)
    targets = {"begane grond": (ax, 1, az), "slaapzaal": (ax, 10, az), "knabbeldepot": (ax, 19, az), "troonzaal": (ax, 28, az + 2),
               "dak": (ax, 37, az - 2), "trap bovenaan": (ox + 20, 37, oz + 22)}
    if with_ship:
        targets.update({"brug": (SX - 8, DK + 1, 32), "dek": (SX + 2, DK + 1, 32), "ruim": (SX + 1, DK - 4, 36),
                        "boegkamertje": (SX, DK + 1, 24), "achterdek": (SX, DK + 1, 48)})
    for label, c in targets.items():
        if c not in reach:
            p.append(f"{label} op {c} is niet bereikbaar vanaf het plein")
    back = w.bfs(targets["dak"])
    if start not in back:
        p.append("vanaf het dak kom je niet terug naar beneden")
    chests = [c for c, bl in blocks.items() if bl[0] == "minecraft:chest"]
    for c in chests:
        if not near(reach, c, dy=(0, -1, 1)):
            p.append(f"kist op {c} is niet bereikbaar")
    counts = {}
    for c in chests:
        counts[blocks[c][2]["LootTable"]] = counts.get(blocks[c][2]["LootTable"], 0) + 1
    if counts.get("guhs:chests/mika_vesting", 0) < 3:
        p.append(f"te weinig vesting-kisten: {counts}")
    mikas = [e for e in b.ents if e[3] == "mika"]
    if not 1 <= len(mikas) <= 3:
        p.append(f"{len(mikas)} Mika's (1-3 mag)")
    for (x, y, z, kind, nbt) in b.ents:
        c = (int(math.floor(x)), int(math.floor(y)), int(math.floor(z)))
        if kind == "mika" and (not w.standable(c) or c not in reach):
            p.append(f"Mika op {c} staat niet op een bereikbare vloer")
    frames = [e for e in b.ents if e[3] == "frame"]
    if with_ship:
        if counts.get("guhs:chests/vetschip", 0) != 2:
            p.append(f"het vetschip moet 2 vetschip-kisten hebben: {counts}")
        good = [e for e in frames if e[4]["Item"]["id"] == "guhs:guhvleugels" and e[4]["id"] == "minecraft:item_frame"]
        if len(frames) != 1 or len(good) != 1:
            p.append(f"verwacht precies 1 item frame met guhvleugels, gevonden {len(frames)}")
        else:
            fx, fy, fz = b.frame_cell
            if b.get(fx, fy, fz) not in (None, AIR) or not b.solid_at((fx, fy, fz - 1)):
                p.append("het item frame hangt niet aan een muur")
            if not near(reach, (fx, fy - 1, fz), dy=(0,)) and (fx, fy - 1, fz + 1) not in reach:
                p.append("het item frame is niet bereikbaar")
        if not any(bl[0] == "guhs:mikatrofee" for bl in blocks.values()):
            p.append("geen mikatrofee op het schip")
    elif frames or counts.get("guhs:chests/vetschip"):
        p.append("de losse vesting heeft geen schip")
    fl = floating(b, lambda c: c[1] == 0)
    if fl:
        p.append(f"{len(fl)} zwevende blokken, bv. {sorted(fl)[:5]}")
    p += attach_problems(b)
    b.stats = {"blokken": len(blocks), "beloopbaar": len(reach), "kisten": counts, "mikas": len(mikas), "frames": len(frames)}
    return p


def build_vesting(h, with_ship):
    if with_ship:
        b = Bouw(h, "mika_vesting_schip", (SW, SH, SD), lambda c: False, 20260926)
        ox, oz = TOX, TOZ
        tower(b, ox, oz, roof_gap_east=range(14, 19))
        ship(b)
    else:
        b = Bouw(h, "mika_vesting", (VW, VH, VD), lambda c: False, 20260926)
        ox, oz = 0, 0
        tower(b, ox, oz)
    b.connect()
    b.anchor(ox + 16, 0, oz + 16, "guhs:mika_vesting_midden")
    problems = vesting_check(b, ox, oz, with_ship)
    b.save()
    print(f"{b.name}: {b.stats}")
    return b, problems


# =====================================================================================================================
# 4. de Terugpoort (2.8): een klein Knabbelpoort-heiligdom op de buiteneilanden, terug naar het grote eiland
# =====================================================================================================================
TP_W, TP_H = 11, 13                    # template 11 x 13 x 11
TP_C = 5                               # the middle (x and z)
TP_G = 4                               # the floor (the anchor sits in it, right under the poort); below it a plinth
TP_SALT = 20280901
TP_POORT = [(x, y, TP_C) for x in range(TP_C - 1, TP_C + 2) for y in range(TP_G + 1, TP_G + 5)]   # 3 wide, 4 high
TERUG_NBT = {"id": "guhs:knabbelpoort", "Terug": 1}
# a guh head on top of the gate (both sides): ears, eyes with a shine, blush and a snoet
TP_GUH = ["E...E",
          "HHHHH",
          "HOHOH",
          "BHNHB"]
TP_GUH_BLOCKS = {"E": "pink_terracotta", "H": "pink_wool", "O": "black_concrete", "B": "magenta_terracotta", "N": "pink_concrete"}


def terugpoort_dist(x, z):
    return math.hypot(x - TP_C, z - TP_C)


def build_terugpoort(h):
    """The template: a round kaaskorst floor (a slab rim to step up on), two Mika-steen pillars with a lintel and a guh
    head on top, the Knabbelpoort blocks in between (block entity Terug: they lead back to the main island, see
    GuheindeReis.terugpoortDestination), four pink lampions on little walls and a sign on each pillar."""
    b = Bouw(h, "guheinde_terugpoort", (TP_W, TP_H, TP_W), lambda c: c[1] < TP_G, TP_SALT)
    rng = random.Random(TP_SALT)
    for x in range(TP_W):
        for z in range(TP_W):
            d = terugpoort_dist(x, z)
            if d > 5.3:
                continue
            for y in range(0, TP_G):                                     # a plinth down into the island (no overhang)
                if d <= 5.3 - (TP_G - 1 - y) * 0.9:
                    b.set(x, y, z, "guhs:kaaskorst" if y < TP_G - 1 else STEEN)
            if d > 4.4:
                b.set(x, TP_G, z, "guhs:kaaskorst_stenen_plaat", slab())     # a rim to step up on
            elif abs(x - TP_C) <= 1 and abs(z - TP_C) <= 3:
                b.set(x, TP_G, z, MSTEEN)                                 # a Mika-steen path through the gate
            else:
                b.set(x, TP_G, z, rng.choice([STEEN, STEEN, STEEN, "guhs:gebarsten_kaaskorst_stenen"]))
            for y in range(TP_G + 1, TP_H):
                b.set(x, y, z, AIR)                                       # (nothing of the island in the way)
    # the gate: two pillars, a lintel, the guh head
    for x in (TP_C - 2, TP_C + 2):
        for y in range(TP_G + 1, TP_G + 5):
            b.set(x, y, TP_C, "guhs:mika_steen_pilaar", {"axis": "y"})
    for x in range(TP_C - 2, TP_C + 3):
        b.set(x, TP_G + 5, TP_C, STEEN)
    for j, row in enumerate(TP_GUH):
        for i, ch in enumerate(row):
            if ch != ".":
                b.set(TP_C - 2 + i, TP_G + 8 - j, TP_C, TP_GUH_BLOCKS[ch])
    for (x, y, z) in TP_POORT:
        b.set(x, y, z, "guhs:knabbelpoort", None, dict(TERUG_NBT))
    # light: four lampions on little walls, on the diagonals
    for dx, dz in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
        x, z = TP_C + dx, TP_C + dz
        b.set(x, TP_G + 1, z, "guhs:kaaskorst_stenen_muur")
        b.set(x, TP_G + 2, z, "guhs:lampion_roze", lamp(False))
    # the signs, on the outside of the pillars
    b.sign(TP_C - 3, TP_G + 3, TP_C, "west", "terugpoort_w", ["Terugpoort", "Stap erin en", "zweef vahoeg", "naar het midden!"],
           ["Terugpoort", "Stap erin en", "zweef vahoeg", "naar het midden!"])
    b.sign(TP_C + 3, TP_G + 3, TP_C, "east", "terugpoort_o", ["Terugpoort", "Naar het grote", "eiland. Tuut!", "(njeg Mika's)"],
           ["Terugpoort", "Naar het grote", "eiland. Tuut!", "(njeg Mika's)"])
    b.connect()
    b.anchor(TP_C, TP_G, TP_C, "guhs:guheinde_terugpoort_midden")
    problems = terugpoort_check(b)
    b.save()
    print(f"{b.name}: {b.stats}")
    return b, problems


def terugpoort_check(b):
    """The geometry self-check: the anchor in the middle of the floor right under the poort, every poort block leads
    back (Terug) and sits in the gate's frame, you can walk up to the poort from both sides (from the rim), lampions and
    signs hang on something, nothing floats, nothing outside the template."""
    p = []
    blocks = b.s.blocks
    if b.oob:
        p.append(f"{len(b.oob)} blokken buiten de template, bv. {b.oob[:4]}")
    a = blocks.get((TP_C, TP_G, TP_C))
    if not a or a[0] != "minecraft:jigsaw" or a[2].get("name") != "guhs:guheinde_terugpoort_midden":
        p.append("geen anker guhs:guheinde_terugpoort_midden midden in de vloer")
    if (TP_C, TP_C) != (b.W // 2, b.D // 2):
        p.append("het anker staat niet in het midden")
    poort = [c for c, bl in blocks.items() if bl[0] == "guhs:knabbelpoort"]
    if sorted(poort) != sorted(TP_POORT):
        p.append(f"poortblokken op de verkeerde plek: {sorted(poort)}")
    for c in poort:
        if blocks[c][2] != TERUG_NBT:
            p.append(f"poort {c} leidt niet terug: {blocks[c][2]}")
    xs, ys = [c[0] for c in poort], [c[1] for c in poort]
    for y in range(min(ys), max(ys) + 1):
        for x in (min(xs) - 1, max(xs) + 1):
            if blocks.get((x, y, TP_C), (AIR,))[0] != "guhs:mika_steen_pilaar":
                p.append(f"geen pilaar naast de poort op {(x, y, TP_C)}")
    for x in range(min(xs) - 1, max(xs) + 2):
        if blocks.get((x, max(ys) + 1, TP_C), (AIR,))[0] in (AIR, None):
            p.append(f"de latei boven de poort mist op {(x, max(ys) + 1, TP_C)}")
    if not any(bl[0] == "minecraft:black_concrete" for bl in blocks.values()):
        p.append("geen guhgezicht op de poort")
    w = Walker(b)
    start = (TP_C, TP_G + 1, 0)
    if not w.standable(start):
        p.append(f"start {start} op de rand is niet beloopbaar")
    reach = w.bfs(start)
    for side in (TP_C - 1, TP_C + 1):
        if (TP_C, TP_G + 1, side) not in reach:
            p.append(f"je kunt niet voor de poort staan op {(TP_C, TP_G + 1, side)}")
    lamps = [c for c, bl in blocks.items() if "lampion" in bl[0]]
    if len(lamps) < 4:
        p.append(f"te weinig licht: {len(lamps)} lampions")
    fl = floating(b, lambda c: c[1] == 0)
    if fl:
        p.append(f"{len(fl)} zwevende blokken, bv. {sorted(fl)[:5]}")
    p += attach_problems(b)
    if b.W > 16 or b.D > 16 or b.H > 16:
        p.append(f"te groot: {(b.W, b.H, b.D)}")
    b.stats = {"blokken": len(blocks), "poort": len(poort), "lampions": len(lamps), "beloopbaar": len(reach)}
    return p


def terugpoort_worldgen(h):
    """With make_v2's structure(): its start pool, structure_set and biome tag; then the structure itself becomes a
    guhs:guheinde_eiland (only on the outer islands: far from 0,0 and where the island is really there) and the set
    keeps away from the Mika-vestingen (exclusion_zone; the room around it is claimed by bouwruimte())."""
    D = h.D
    h.structure("guheinde_terugpoort", ["guhs:guheinde"], spacing=24, separation=10, salt=TP_SALT,
                centre="guhs:guheinde_terugpoort_midden", reach=16)
    s = json.load(open(f"{D}/worldgen/structure/guheinde_terugpoort.json", encoding="utf-8"))
    jig = s["jigsaw"]
    jig.update({"terrain_adaptation": "none", "spawn_overrides": {}})
    h.w(f"{D}/worldgen/structure/guheinde_terugpoort.json", {
        "type": "guhs:guheinde_eiland", "biomes": jig["biomes"], "step": "surface_structures", "spawn_overrides": {},
        "terrain_adaptation": "none", "min_distance": 1000, "min_surface_y": 56, "check_radius": 7, "jigsaw": jig})
    ss = json.load(open(f"{D}/worldgen/structure_set/guheinde_terugpoort.json", encoding="utf-8"))
    ss["placement"]["exclusion_zone"] = {"other_set": "guhs:mika_vesting", "chunk_count": 6}
    h.w(f"{D}/worldgen/structure_set/guheinde_terugpoort.json", ss)


# =====================================================================================================================
def build(h):
    if not hasattr(h, "ms"):
        h.ms = __import__("make_structures")
    all_problems = {}
    _, all_problems["knabbelkelder"] = build_kelder(h)
    _, all_problems["guheinde_knabbelberg"] = build_berg(h)
    vesting_worldgen(h)
    _, all_problems["mika_vesting"] = build_vesting(h, False)
    _, all_problems["mika_vesting_schip"] = build_vesting(h, True)
    terugpoort_worldgen(h)
    _, all_problems["guheinde_terugpoort"] = build_terugpoort(h)
    bad = {k: v for k, v in all_problems.items() if v}
    for name, probs in all_problems.items():
        if probs:
            print(f"{name}: self-check vond {len(probs)} probleem/problemen:\n  " + "\n  ".join(probs[:40]))
        else:
            print(f"{name}: self-check ok")
    if bad:
        raise SystemExit(f"guheinde structuren: repareer de templates ({', '.join(bad)}), zie hierboven")
