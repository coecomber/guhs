"""
De Elf-Guhjestocht - the template: the polder ground with its gentle snowy relief, the meandering frozen canal loop with
its banks, start arch, flags, arrows, bridges, lampions and vuurkorven, koek-en-zopie stalls and cheering audience, the
polder between the villages (organic sloten with knotwilg rows, knotwilg groups, reed clumps, molentjes,
sneeuwguh-heuveltjes, hooibergen, skating ponds, ijspegelguh-kristallen: elftocht_land + elftocht_sfeer), Guhwarden with
"De Bonkevads" (village 1: start and finish) and the ten villages (tools/features/elftocht_dorp_<nn>_<slug>.py, see
elftocht_dorp_api.py; a simple placeholder plot while a module is missing). 2.10: every village is built in its own
frame and turned onto its plot beside its straight (dorpen(), draai_props), so the eleven villages lie evenly along the
whole route (elftocht_route).

build(h, echt) returns (structure, info); check(s, info) is the geometry self-check (raises SystemExit).
echt = the Guhpolder blocks exist (guhpolder.py made them); otherwise vanilla stand-ins (STANDIN) are used.
"""
import glob
import importlib
import json
import math
import os
import random
import sys
from collections import deque

from features import elftocht_route as R
from features import sterrenwacht_hulp as hulp

AIR = "minecraft:air"
GY, OY = R.GY, R.OY

# the Guhpolder blocks (guhpolder slice) and their vanilla stand-ins while they don't exist yet (W/elftocht before the merge)
STANDIN = {
    "guhs:rijpgras": ("minecraft:grass_block", {"snowy": "false"}),
    "guhs:rijpsprietjes": ("minecraft:short_grass", {}),
    "guhs:guh_ijsbloempje": ("minecraft:blue_orchid", {}),
    "guhs:polderijs": ("minecraft:packed_ice", {}),
    "guhs:knotwilg_stam": ("minecraft:oak_log", None),
    "guhs:knotwilg_bladeren": ("minecraft:oak_leaves", {"persistent": "true", "distance": "7", "waterlogged": "false"}),
    "guhs:ijspegelguh_kristal": ("minecraft:sea_lantern", {}),
    "guhs:guh_molentje": ("minecraft:oak_fence", {}),
}
POLDER_BLOCKS = tuple(STANDIN)

ICE = "guhs:polderijs"
GRAS = "guhs:rijpgras"

# our own blocks (tools/features/elftocht.py + feature/elftocht)
LAMPION = "guhs:elftocht_lampion"
VUURKORF = "guhs:elftocht_vuurkorf"
KOPJES = "guhs:elftocht_kopjes"
KRUISJE = "guhs:elf_guhjeskruisje"

DORPEN_DIR = os.path.dirname(os.path.abspath(__file__))


def mc(n):
    return "minecraft:" + n


# =====================================================================================================================
# small helpers
# =====================================================================================================================
def sign_nbt(lines, colour="black", glow=False):
    from make_structures import NbtList, Byte
    msgs = [json.dumps({"text": l}) for l in lines] + [json.dumps("")] * (4 - len(lines))
    empty = [json.dumps("")] * 4
    return {"id": "minecraft:sign",
            "front_text": {"messages": NbtList(8, msgs), "color": colour, "has_glowing_text": Byte(1 if glow else 0)},
            "back_text": {"messages": NbtList(8, msgs), "color": colour, "has_glowing_text": Byte(1 if glow else 0)},
            "is_waxed": Byte(1)}


def banner_nbt(patterns):
    from make_structures import NbtList
    return {"id": "minecraft:banner", "patterns": NbtList(10, [{"color": c, "pattern": "minecraft:" + p} for c, p in patterns])}


# the flags: the Dutch tricolour, the Frisian-ish blue/white with red hearts, and pink guh pennants
VLAGGEN = [
    ("red", [("white", "stripe_middle"), ("blue", "stripe_bottom")]),
    ("white", [("blue", "diagonal_left"), ("red", "flower"), ("white", "border")]),
    ("pink", [("white", "circle"), ("magenta", "triangles_bottom"), ("white", "border")]),
    ("orange", [("white", "rhombus"), ("orange", "circle")]),
]


def vlag(s, x, y, z, rng=None, rot=0, kind=None):
    """A standing flag on a little pole (fence) at (x, y, z) on the ground: pole y, y+1, banner y+2."""
    base, pats = VLAGGEN[kind if kind is not None else (rng.randrange(len(VLAGGEN)) if rng else 0)]
    s.set(x, y, z, mc("spruce_fence"))
    s.set(x, y + 1, z, mc("spruce_fence"))
    s.set(x, y + 2, z, mc(f"{base}_banner"), {"rotation": str(rot % 16)}, banner_nbt(pats))


def wandvlag(s, x, y, z, facing, kind=0):
    base, pats = VLAGGEN[kind]
    s.set(x, y, z, mc(f"{base}_wall_banner"), {"facing": facing}, banner_nbt(pats))


def lamppaal(s, x, y, z, hoog=3):
    """A lamp post: dark wood posts with our night lampion on top (it glows at night)."""
    for dy in range(hoog):
        s.set(x, y + dy, z, mc("dark_oak_fence"))
    s.set(x, y + hoog, z, LAMPION, {"hanging": "false", "lit": "false", "waterlogged": "false"})


def vuurkorf(s, x, y, z):
    s.set(x, y, z, VUURKORF, {"lit": "false"})


def knotwilg(s, x, z, rng, y=OY):
    """A pollard willow: a thick short trunk and a round head of thin twigs with a cap of snow."""
    hoog = rng.choice((2, 3, 3))
    for dy in range(hoog):
        s.set(x, y + dy, z, "guhs:knotwilg_stam", {"axis": "y"})
    top = y + hoog
    s.set(x, top, z, "guhs:knotwilg_stam", {"axis": "y"})
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            for dy in range(0, 3):
                d = math.sqrt(dx * dx + dz * dz + (dy - 0.6) ** 2 * 1.4)
                if d <= 2.3 and (dx, dy, dz) != (0, 0, 0) and rng.random() < 0.92:
                    if s.get(x + dx, top + dy, z + dz) is None:
                        s.set(x + dx, top + dy, z + dz, "guhs:knotwilg_bladeren")
    # a snow cap on the highest leaves
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            for dy in range(3, -1, -1):
                if s.get(x + dx, top + dy, z + dz) == "guhs:knotwilg_bladeren":
                    if s.get(x + dx, top + dy + 1, z + dz) is None:
                        s.set(x + dx, top + dy + 1, z + dz, mc("snow"), {"layers": "2"})
                    break


def bankje(s, x, y, z, facing):
    s.set(x, y, z, "guhs:guh_bank", {"facing": facing})


def guh_publiek(s, x, y, z, yaw, scale=None, rng=None):
    """A cheering audience guh (tag guhs_elftocht_publiek: the game makes it cheer when a skater passes)."""
    from make_structures import Byte, floats, compounds, Double
    nbt = {"id": "guhs:guh", "PersistenceRequired": Byte(1), "Rotation": floats(float(yaw), 0.0),
           "Tags": _strings(["guhs_elftocht_publiek"])}
    if scale:
        nbt["attributes"] = compounds([{"id": "minecraft:scale", "base": Double(scale)}])
    s.entity(x + 0.5, y, z + 0.5, nbt)


def _strings(values):
    from make_structures import NbtList
    return NbtList(8, list(values))


def stempelguh(s, x, y, z, yaw, index):
    from make_structures import Byte, floats
    s.entity(x + 0.5, y, z + 0.5, {"id": "guhs:guh_npc", "Kind": "stempelguh", "PersistenceRequired": Byte(1),
                                   "Rotation": floats(float(yaw), 0.0), "Tags": _strings([f"guhs_elftocht_dorp_{index}"])})


def yaw_of(dx, dz):
    """Minecraft yaw (0 = south, 90 = west) of looking along (dx, dz)."""
    return math.degrees(math.atan2(-dx, dz)) % 360


def facing_of(dx, dz):
    return {(0, -1): "north", (0, 1): "south", (1, 0): "east", (-1, 0): "west"}[(dx, dz)]


# =====================================================================================================================
# the ground and the canal
# =====================================================================================================================
def grond(s, ice, rng):
    """Dirt from y 0, rijpgras (or ice) on top: the whole square. Under the ice two layers of ice."""
    for x in range(R.SIZE_X):
        for z in range(R.SIZE_Z):
            is_ice = (x, z) in ice
            for y in range(0, GY - 1):
                s.blocks[(x, y, z)] = (mc("dirt") if y >= GY - 3 else mc("packed_mud") if y else mc("stone"), {}, None)
            if is_ice:
                s.blocks[(x, GY - 1, z)] = (ICE, {}, None)
                s.blocks[(x, GY, z)] = (ICE, {}, None)
            else:
                s.blocks[(x, GY - 1, z)] = (mc("dirt"), {}, None)
                s.blocks[(x, GY, z)] = (GRAS, {}, None)


def is_free(s, x, z, ice, reserved, y=OY):
    return (0 < x < R.SIZE_X - 1 and 0 < z < R.SIZE_Z - 1 and (x, z) not in ice and (x, z) not in reserved
            and not R.in_plot(x, z) and s.get(x, y, z) is None)


def near_ice(x, z, ice, r):
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            if (x + dx, z + dz) in ice:
                return True
    return False


def pijlen(s, ice, keep_off):
    """Direction chevrons of blue ice on the canal every 40 blocks (not near the start line)."""
    pts, length, _ = R.route()
    n = 0
    s_pos = 20.0
    while s_pos < length - 15:
        x, z, tx, tz = R.along(s_pos)
        if abs(x - R.START_X) > 12 or abs(z - R.Z_E1) > 6:
            nx, nz = -tz, tx
            for k in range(-2, 3):
                for thick in (0.0, 0.7):
                    px = x - tx * (abs(k) * 1.1 + thick) + nx * k
                    pz = z - tz * (abs(k) * 1.1 + thick) + nz * k
                    c = (int(round(px)), int(round(pz)))
                    if c in ice and c not in keep_off:
                        s.set(c[0], GY, c[1], mc("blue_ice"))
            n += 1
        s_pos += 40
    return n


def startlijn(s, ice):
    """The start/finish line: a chequered band of blue and packed ice across the canal."""
    cells = []
    for z in range(R.Z_E1 - R.HALF, R.Z_E1 + R.HALF + 1):
        for dx in (0, 1):
            x = R.START_X + dx
            if (x, z) in ice:
                s.set(x, GY, z, mc("blue_ice") if (z + dx) % 2 == 0 else mc("packed_ice"))
                cells.append((x, z))
    return set(cells)


def startboog(s, rng):
    """The start arch over the canal at the start line: two striped towers, a bow with a big guh face on top, flags,
    lampions, "START" and "FINISH" boards and a row of little flags hanging over the ice."""
    x = R.START_X
    zn, zs = R.Z_E1 - R.HALF - 2, R.Z_E1 + R.HALF + 2    # the towers stand on the banks (z 31 and 41)
    top = OY + 8
    for z in (zn, zs):
        for dx in (-1, 0, 1, 2):
            for dz in (-1, 0, 1):
                if z == zn and dz == 1:
                    continue      # (keeps the bank row in front of the plot free)
                if z == zs and dz == -1:
                    continue
                for y in range(OY, top):
                    stripe = ((y - OY) // 2) % 2 == 0
                    edge = dx in (-1, 2) or dz != 0
                    s.set(x + dx, y, z + dz, mc("red_concrete") if stripe and edge else mc("white_concrete") if edge else mc("spruce_planks"))
        # a little roof cap
        for dx in (-1, 0, 1, 2):
            for dz in (-1, 0, 1):
                if (z == zn and dz == 1) or (z == zs and dz == -1):
                    continue
                s.set(x + dx, top, z + dz, "guhs:pluisdak_plaat", {"type": "bottom", "waterlogged": "false"})
        s.set(x, top + 1, z, mc("spruce_fence"))
        s.set(x, top + 2, z, mc("orange_banner"), {"rotation": "0"}, banner_nbt(VLAGGEN[3][1]))
    # the bow over the ice
    for z in range(zn + 1, zs):
        for dx in (0, 1):
            s.set(x + dx, top - 1, z, mc("spruce_planks"))
            s.set(x + dx, top, z, "guhs:pluisdak_plaat", {"type": "bottom", "waterlogged": "false"})
        s.set(x - 1, top - 1, z, mc("red_concrete") if z % 2 else mc("white_concrete"))
        s.set(x + 2, top - 1, z, mc("red_concrete") if z % 2 else mc("white_concrete"))
        # little flags hanging below the bow
        s.set(x, top - 2, z, "guhs:vlaggetjes", {"axis": "z"})
    # a big guh face on the middle of the bow, looking both ways (east for the start, west for the finish)
    mid = R.Z_E1
    for facing, fx in (("west", x - 1), ("east", x + 2)):
        hulp.wall_face(s, mid, top + 3, fx, 3, facing)
    for zz in range(mid - 4, mid + 5):
        for yy in range(top + 1, top + 7):
            if s.get(x - 1, yy, zz) is not None and s.get(x + 2, yy, zz) is not None:
                s.set(x, yy, zz, "minecraft:pink_wool")
                s.set(x + 1, yy, zz, "minecraft:pink_wool")
    # boards: START facing west (skaters start going east... they see it from the west) and FINISH
    s.set(x - 2, top - 1, mid - 1, mc("spruce_wall_sign"), {"facing": "west", "waterlogged": "false"},
          sign_nbt(["", "START", "Elf-Guhjestocht", ""], "white", True))
    s.set(x - 2, top - 1, mid + 1, mc("spruce_wall_sign"), {"facing": "west", "waterlogged": "false"},
          sign_nbt(["", "FINISH", "VAHOEG!", ""], "white", True))
    s.set(x + 3, top - 1, mid, mc("spruce_wall_sign"), {"facing": "east", "waterlogged": "false"},
          sign_nbt(["Nog elf", "dorpjes...", "vahoeg!", ""], "white", True))
    # lampions on the towers
    for z in (zn, zs):
        s.set(x - 2, OY + 5, z, LAMPION, {"hanging": "false", "lit": "false", "waterlogged": "false"}) if s.get(x - 2, OY + 4, z) else None
        s.set(x - 2, OY + 4, z, mc("spruce_fence")) if s.get(x - 2, OY + 4, z) is None else None
        s.set(x - 2, OY + 5, z, LAMPION, {"hanging": "false", "lit": "false", "waterlogged": "false"})
        for y in range(OY, OY + 4):
            if s.get(x - 2, y, z) is None:
                s.set(x - 2, y, z, mc("spruce_fence"))


def brug(s, cx, cz, along_x, ice, reserved, name):
    """A little arched wooden footbridge over the canal; skaters glide underneath (3 blocks of air above the ice).
    along_x: the deck runs along x (over a canal that runs north-south). Returns False if it doesn't fit."""
    # the span: from the ice's edge on one side to the other, plus 4 steps down on each bank
    ax = (1, 0) if along_x else (0, 1)
    side = (0, 1) if along_x else (1, 0)
    lo = hi = 0
    while (cx + ax[0] * (lo - 1), cz + ax[1] * (lo - 1)) in ice:
        lo -= 1
    while (cx + ax[0] * (hi + 1), cz + ax[1] * (hi + 1)) in ice:
        hi += 1
    lo -= 1
    hi += 1
    deck = OY + 3
    cells = []
    for t in range(lo - 4, hi + 5):
        for w in (-1, 0, 1):
            x, z = cx + ax[0] * t + side[0] * w, cz + ax[1] * t + side[1] * w
            if lo <= t <= hi:
                cells.append((x, z, deck, t, w))
            else:
                step = lo - t if t < lo else t - hi     # 1..4 away from the ice: stairs down
                y = deck - step + 1
                if (x, z) in ice or R.in_plot(x, z) or (x, z) in reserved:
                    return False
                cells.append((x, z, y, t, w))
    for (x, z, y, t, w) in cells:
        if y > OY - 1 and s.get(x, y, z) is not None and s.get(x, y, z) != AIR:
            return False
    stair_up = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}
    for (x, z, y, t, w) in cells:
        if lo <= t <= hi:
            s.set(x, y, z, "guhs:vadshout_planken")
            if w != 0:
                s.set(x, y + 1, z, "guhs:vadshout_hek", {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
        else:
            going_up = (ax[0], ax[1]) if t < lo else (-ax[0], -ax[1])
            if y >= OY:
                s.set(x, y, z, "guhs:vadshout_trap", {"facing": stair_up[going_up], "half": "bottom", "shape": "straight", "waterlogged": "false"})
                for yy in range(OY, y):
                    s.set(x, yy, z, "guhs:vadshout_planken")
        reserved.add((x, z))
    # railing posts with lampions at the ends of the deck, and a name board
    for t in (lo, hi):
        for w in (-1, 1):
            x, z = cx + ax[0] * t + side[0] * w, cz + ax[1] * t + side[1] * w
            s.set(x, deck + 1, z, mc("dark_oak_fence"))
            s.set(x, deck + 2, z, LAMPION, {"hanging": "false", "lit": "false", "waterlogged": "false"})
    # guh faces in the middle of the deck's edges, looking along the canal both ways (skaters see them coming)
    mx, mz = cx + ax[0] * ((lo + hi) // 2), cz + ax[1] * ((lo + hi) // 2)
    for w in (-1, 1):
        x, z = mx + side[0] * w, mz + side[1] * w
        s.set(x, deck, z, "guhs:vadshout_gezicht", {"facing": facing_of(side[0] * w, side[1] * w), "stemming": str((cx + cz + w) % 3)})
    return True


def kraam(s, x0, z0, facing, rng, soort="chocovet", titel=("Koek &", "Zopie")):
    """A koek-en-zopie stall on the bank (5 wide, 3 deep, the counter towards `facing`): striped roof, a counter with a
    tray of warm cups (the boost spot: guhs:elftocht_kopjes), a vuurkorf, a sign. Returns the boost spot."""
    dx, dz = {"south": (0, 1), "north": (0, -1), "east": (1, 0), "west": (-1, 0)}[facing]
    # local frame: u along the counter, v towards the ice
    u = (1, 0) if dz != 0 else (0, 1)
    cells = []
    for a in range(5):
        for b in range(3):
            x = x0 + u[0] * a + dx * b
            z = z0 + u[1] * a + dz * b
            cells.append((a, b, x, z))
    for (a, b, x, z) in cells:
        if a in (0, 4) and b in (0, 2):
            for y in range(OY, OY + 3):
                s.set(x, y, z, mc("spruce_fence"))
        stripe = mc("red_wool") if a % 2 == 0 else mc("white_wool")
        s.set(x, OY + 3, z, stripe)
    # the counter at the ice side
    boost = None
    for a in range(1, 4):
        x = x0 + u[0] * a + dx * 2
        z = z0 + u[1] * a + dz * 2
        s.set(x, OY, z, "guhs:vadshout_planken")
        if a == 2:
            s.set(x, OY + 1, z, KOPJES, {"facing": facing, "soort": soort})
            boost = (x, OY + 1, z)
        else:
            s.set(x, OY + 1, z, mc("spruce_slab"), {"type": "bottom", "waterlogged": "false"})
    # the pot and the vuurkorf behind
    x = x0 + u[0] * 1
    z = z0 + u[1] * 1
    s.set(x, OY, z, mc("cauldron"))
    vuurkorf(s, x0 + u[0] * 3, OY, z0 + u[1] * 3)
    # the sign on the roof edge
    sx, sz = x0 + u[0] * 2 + dx * 3, z0 + u[1] * 2 + dz * 3
    s.set(sx, OY + 3, sz, mc("spruce_wall_sign"), {"facing": facing, "waterlogged": "false"}, sign_nbt(["", titel[0], titel[1], ""]))
    return boost


# =====================================================================================================================
# Guhwarden: De Bonkevads, the grandstand, the start
# =====================================================================================================================
def guhwarden(s, rng):
    """Village 1 (start and finish). Returns the village's dict like the modules do, plus the Schaatsmeester's spot.
    (Laid out for a plot starting at x 18; dx shifts it to where R.GW_X0 puts it.)"""
    x0, z0, x1, z1 = R.GUHWARDEN
    dx = x0 - 18
    front = z1 - 1       # the bank row at the ice
    info = {"publiek": [], "boost": [], "lichten": []}
    # klinkers over the whole plot, a bit of snow
    for x in range(x0, x1):
        for z in range(z0, z1):
            s.set(x, GY, z, "guhs:knuffelklinkers")
    bonkevads(s, 20 + dx, 6, rng, info)
    tribune(s, 47 + dx, 15, rng, info)
    # the terrace in front of the Bonkevads: benches, lampions, the koek-en-zopie counter (boost) at the ice
    b = kraam(s, 22 + dx, 26, "south", rng, "chocovet", ("Koek &", "Zopie"))
    info["boost"].append(b)
    b2 = kraam(s, 32 + dx, 26, "south", rng, "snert", ("Erwten-", "soep!"))
    info["boost"].append(b2)
    for x in (19, 28, 37):
        lamppaal(s, x + dx, OY, front)
        info["lichten"].append((x + dx, OY + 3, front))
    for x in (21, 26, 31):
        vlag(s, x + dx, OY, front, rng, 0)
    # the Schaatsmeester next to the start (facing the ice), the finish Stempelguh on the other side of the arch
    schaatsmeester = (40 + dx, OY, front - 1)
    finish = (49 + dx, OY, front)
    s.set(finish[0], GY, finish[2], "guhs:vadshout_planken")
    info["stempelguh"] = finish + (0,)
    info["schaatsmeester"] = schaatsmeester
    # a podium for the stempelguh with flags
    for x in (47, 51):
        vlag(s, x + dx, OY, front, rng, 0, kind=0)
    return info


def bonkevads(s, x0, z0, rng, info):
    """De Bonkevads: a big Frisian-style farmhouse café (22 x 16) with a huge guh face in the south gable, a
    koek-en-zopie inside, round windows like guh eyes, a smoking chimney and lampions."""
    W, D = 22, 16
    x1, z1 = x0 + W - 1, z0 + D - 1
    wall, frame, floor = "guhs:knuffelsteen", "guhs:vadshout_stam", "guhs:vadshout_planken"
    hwall = 6
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            s.set(x, GY, z, floor)
            edge = x in (x0, x1) or z in (z0, z1)
            for y in range(OY, OY + hwall):
                if edge:
                    corner = x in (x0, x1) and z in (z0, z1)
                    s.set(x, y, z, frame if corner or (x - x0) % 7 == 0 and z in (z0, z1) else wall, {"axis": "y"} if corner or (x - x0) % 7 == 0 and z in (z0, z1) else None)
                else:
                    s.set(x, y, z, AIR)
    # the roof: a big gable along z (ridge north-south), gables with the guh face at the south and north ends
    half = W // 2
    for k in range(half + 1):
        y = OY + hwall + k
        for z in range(z0 - 1, z1 + 2):
            for x in (x0 - 1 + k, x1 + 1 - k):
                if x0 - 1 <= x <= x1 + 1:
                    s.set(x, y, z, "guhs:pluisdak_trap", {"facing": "east" if x < x0 + half else "west", "half": "bottom",
                                                          "shape": "straight", "waterlogged": "false"})
                    if k > 0 and x0 - 1 < x < x1 + 1:
                        s.set(x, y - 1, z, "guhs:pluisdak")
        if x0 - 1 + k >= x1 + 1 - k:
            break
    # gables (the triangles at both ends) filled with wall, the big guh face in the south gable
    for z in (z0, z1):
        for k in range(half):
            y = OY + hwall + k
            for x in range(x0 + k + 1, x1 - k):
                if s.get(x, y, z) is None or s.get(x, y, z) == AIR:
                    s.set(x, y, z, wall)
    hulp.wall_face(s, x0 + half, OY + hwall + 2, z1, 4, "south")
    hulp.wall_face(s, x0 + half, OY + hwall + 2, z0, 3, "north")
    # guh-face stones between the windows
    for x in (x0 + 4, x1 - 4):
        s.set(x, OY + 4, z1, "guhs:knuffelsteen_gezicht", {"facing": "south", "stemming": "1"})
    # snow on the roof ridge
    for z in range(z0 - 1, z1 + 2):
        for x in range(x0 - 1, x1 + 2):
            for y in range(OY + hwall + half + 1, OY + hwall - 1, -1):
                b = s.get(x, y, z)
                if b and b.startswith("guhs:pluisdak_trap") and s.get(x, y + 1, z) is None and rng.random() < 0.55:
                    s.set(x, y + 1, z, mc("snow"), {"layers": "1"})
                    break
    # the ceiling (the attic floor): the lampions hang from it
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            s.set(x, OY + hwall, z, "guhs:vadshout_planken")
    # door (double, south) and windows
    dx = x0 + half - 1
    for x in (dx, dx + 1):
        s.set(x, OY, z1, "guhs:vadshout_deur", {"facing": "south", "half": "lower", "hinge": "left" if x == dx else "right", "open": "false", "powered": "false"})
        s.set(x, OY + 1, z1, "guhs:vadshout_deur", {"facing": "south", "half": "upper", "hinge": "left" if x == dx else "right", "open": "false", "powered": "false"})
        s.set(x, OY + 2, z1, frame, {"axis": "x"})
    for x in list(range(x0 + 2, dx - 1, 3)) + list(range(dx + 4, x1 - 1, 3)):
        s.set(x, OY + 2, z1, mc("pink_stained_glass_pane"), {"north": "false", "south": "false", "east": "true", "west": "true", "waterlogged": "false"})
        s.set(x, OY + 2, z0, mc("pink_stained_glass_pane"), {"north": "false", "south": "false", "east": "true", "west": "true", "waterlogged": "false"})
    for z in range(z0 + 3, z1 - 1, 4):
        for x in (x0, x1):
            s.set(x, OY + 2, z, mc("light_blue_stained_glass_pane"), {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})
            s.set(x, OY + 3, z, mc("light_blue_stained_glass_pane"), {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})
    # name board over the door
    s.set(dx, OY + 3, z1 + 1, mc("spruce_wall_sign"), {"facing": "south", "waterlogged": "false"}, sign_nbt(["", "De", "Bonkevads", ""], "brown"))
    s.set(dx + 1, OY + 3, z1 + 1, mc("spruce_wall_sign"), {"facing": "south", "waterlogged": "false"}, sign_nbt(["Koek & zopie", "Erwtensoep", "Chocovet", ""], "brown"))
    # inside: tables and chairs, the counter along the north wall with the snert pots, lampions from the ceiling
    for (tx, tz) in ((x0 + 3, z1 - 4), (x0 + 3, z1 - 9), (x1 - 3, z1 - 4), (x1 - 3, z1 - 9)):
        s.set(tx, OY, tz, "guhs:guh_tafel")
        s.set(tx - 1, OY, tz, "guhs:guh_stoel", {"facing": "east"})
        s.set(tx + 1, OY, tz, "guhs:guh_stoel", {"facing": "west"})
    for x in range(x0 + 3, x1 - 2):
        s.set(x, OY, z0 + 2, "guhs:vadshout_planken")
        s.set(x, OY + 1, z0 + 2, mc("spruce_slab"), {"type": "bottom", "waterlogged": "false"})
    s.set(x0 + 5, OY + 1, z0 + 1, mc("cauldron"))
    s.set(x0 + 8, OY + 1, z0 + 1, mc("cauldron"))
    s.set(x0 + 11, OY + 1, z0 + 2, KOPJES, {"facing": "south", "soort": "snert"})
    info["boost"].append((x0 + 11, OY + 1, z0 + 2))
    s.set(x0 + 14, OY + 1, z0 + 2, KOPJES, {"facing": "south", "soort": "chocovet"})
    info["boost"].append((x0 + 14, OY + 1, z0 + 2))
    for (lx, lz) in ((x0 + 5, z0 + 6), (x1 - 5, z0 + 6), (x0 + 5, z1 - 3), (x1 - 5, z1 - 3), (x0 + half, z0 + 8)):
        s.set(lx, OY + hwall - 1, lz, mc("chain"), {"axis": "y", "waterlogged": "false"})
        s.set(lx, OY + hwall - 2, lz, "guhs:lampion_geel", {"hanging": "true", "waterlogged": "false"})
    s.set(x0 + 1, OY, z0 + 1, mc("campfire"), {"facing": "south", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    # the chimney (with a hidden campfire under it: smoke) on the north side of the roof
    cx, cz = x0 + 4, z0 + 2
    for y in range(OY + hwall, OY + hwall + 9):
        s.set(cx, y, cz, mc("bricks"))
    s.set(cx, OY + hwall + 9, cz, mc("campfire"), {"facing": "south", "lit": "true", "signal_fire": "true", "waterlogged": "false"})
    # the Bonkevads' own audience at the windows outside (sitting on benches)
    for x in (x0 + 2, x1 - 2):
        bankje(s, x, OY, z1 + 2, "north")


def tribune(s, x0, z0, rng, info):
    """A grandstand facing the ice (x0..x0+17, z0..z0+14): four rows going up to the north, striped canopy, flags,
    audience on the rows."""
    W = 18
    rows = [(z0 + 12, OY), (z0 + 9, OY + 1), (z0 + 6, OY + 2), (z0 + 3, OY + 3)]
    for x in range(x0, x0 + W):
        for (rz, ry) in rows:
            for z in range(rz, rz + 3):
                for y in range(OY, ry + 1):
                    if y == ry:
                        s.set(x, y, z, "guhs:vadshout_planken" if z != rz + 2 else "guhs:vadshout_plaat", {"type": "bottom", "waterlogged": "false"} if z == rz + 2 else None)
                    else:
                        s.set(x, y, z, "guhs:vadshout_planken")
        # back wall
        for y in range(OY, OY + 8):
            s.set(x, y, z0 + 2, "guhs:knuffelsteen" if y < OY + 6 else mc("red_wool") if x % 2 else mc("white_wool"))
    # canopy
    for x in range(x0 - 1, x0 + W + 1):
        for z in range(z0 + 2, z0 + 11):
            s.set(x, OY + 8, z, mc("red_wool") if (x - x0) % 4 < 2 else mc("white_wool"))
    for x in (x0 - 1, x0 + W):
        for z in (z0 + 10,):
            for y in range(OY, OY + 8):
                s.set(x, y, z, mc("spruce_fence"))
    for x in range(x0 - 1, x0 + W + 1, 3):
        s.set(x, OY + 9, z0 + 2, mc("spruce_fence"))
        base, pats = VLAGGEN[(x // 3) % len(VLAGGEN)]
        s.set(x, OY + 10, z0 + 2, mc(f"{base}_banner"), {"rotation": "8"}, banner_nbt(pats))
    # the audience on the rows
    for i, (rz, ry) in enumerate(rows):
        for x in range(x0 + 1 + i % 2, x0 + W - 1, 3):
            info["publiek"].append((x, ry + 1, rz + 1, 0))
    # lampions hanging from the canopy
    for x in range(x0 + 2, x0 + W - 1, 5):
        s.set(x, OY + 7, z0 + 9, mc("chain"), {"axis": "y", "waterlogged": "false"})
        s.set(x, OY + 6, z0 + 9, LAMPION, {"hanging": "true", "lit": "false", "waterlogged": "false"})
        info["lichten"].append((x, OY + 6, z0 + 9))


# =====================================================================================================================
# the villages 2..11
# =====================================================================================================================
def dorp_modules():
    """The village modules found: {index: module} (tools/features/elftocht_dorp_<nn>_<slug>.py; for development also
    the directory in the environment variable ELFTOCHT_DORPEN_DIR)."""
    found = {}
    dirs = [DORPEN_DIR]
    extra = os.environ.get("ELFTOCHT_DORPEN_DIR")
    if extra and os.path.isdir(extra):
        dirs.append(extra)
        if extra not in sys.path:
            sys.path.insert(0, extra)
    for d in dirs:
        for path in sorted(glob.glob(os.path.join(d, "elftocht_dorp_[0-9][0-9]_*.py"))):
            name = os.path.basename(path)[:-3]
            mod = importlib.import_module(("features." if d == DORPEN_DIR else "") + name)
            idx = getattr(mod, "INDEX", None)
            if isinstance(idx, int) and 2 <= idx <= 11 and idx not in found:
                found[idx] = mod
    return found


class Wacht:
    """The Structure as a village sees it: every write outside its plot is remembered (the core refuses those)."""

    def __init__(self, s, ox, oy, oz):
        self._s = s
        self.size = s.size
        self.blocks = s.blocks
        self.entities = s.entities
        self.box = (ox, oy, oz)
        self.buiten = []

    def _ok(self, x, y, z):
        ox, oy, oz = self.box
        if not (ox <= x < ox + R.PLOT_X and oy - 3 <= y < oy + R.PLOT_Y and oz <= z < oz + R.PLOT_Z):
            self.buiten.append((x, y, z))
            return False
        return True

    def set(self, x, y, z, name, props=None, nbt=None):
        x, y, z = int(round(x)), int(round(y)), int(round(z))
        if self._ok(x, y, z):
            self._s.set(x, y, z, name, props, nbt)

    def get(self, x, y, z):
        return self._s.get(int(round(x)), int(round(y)), int(round(z)))

    def entity(self, x, y, z, nbt):
        if self._ok(int(math.floor(x)), int(math.floor(y)), int(math.floor(z))):
            self._s.entity(x, y, z, nbt)

    def inside(self, x, y, z):
        return self._s.inside(x, y, z)

    def __getattr__(self, item):
        # fill, sphere, line, ring_xy, tube, clear_above: the Structure's own code, writing through our set()
        from make_structures import Structure
        fn = getattr(Structure, item)
        return lambda *a, **k: fn(self, *a, **k)


def plaatshouder(s, index, ox, oy, oz, rng):
    """A simple village while its module isn't there yet: a jetty, a small guh house, the Stempelguh and some audience."""
    naam = R.NAMEN[index]
    edge = oz + R.PLOT_Z - 1
    for x in range(ox + 8, ox + 20):
        for z in range(edge - 3, edge + 1):
            s.set(x, oy - 1, z, "guhs:vadshout_planken")
    hx0, hz0 = ox + 9, oz + 6
    for x in range(hx0, hx0 + 10):
        for z in range(hz0, hz0 + 8):
            edge_ = x in (hx0, hx0 + 9) or z in (hz0, hz0 + 7)
            for y in range(oy, oy + 5):
                s.set(x, y, z, "guhs:knuffelsteen" if edge_ else AIR)
            s.set(x, oy + 5, z, "guhs:pluisdak")
    hulp.wall_face(s, hx0 + 5, oy + 2, hz0 + 7, 2, "south")
    s.set(hx0 + 1, oy + 2, hz0 + 8, mc("spruce_wall_sign"), {"facing": "south", "waterlogged": "false"}, sign_nbt(["", naam, "(bouwt nog...)", ""]))
    s.set(hx0 + 4, oy, hz0 + 7, "guhs:vadshout_deur", {"facing": "south", "half": "lower", "hinge": "left", "open": "false", "powered": "false"})
    s.set(hx0 + 4, oy + 1, hz0 + 7, "guhs:vadshout_deur", {"facing": "south", "half": "upper", "hinge": "left", "open": "false", "powered": "false"})
    x, z = ox + 14, edge
    stempelguh(s, x, oy, z, 0, index)
    lamppaal(s, ox + 8, oy, edge)
    lamppaal(s, ox + 19, oy, edge)
    publiek = [(ox + 10, oy, edge - 1, 0), (ox + 17, oy, edge - 1, 0), (ox + 4, oy, edge, 0)]
    for (px, py, pz, yaw) in publiek:
        guh_publiek(s, px, py, pz, yaw)
    return {"stempelguh": (x, oy, z, 0), "publiek": publiek, "boost": [], "lichten": [(ox + 8, oy + 3, edge), (ox + 19, oy + 3, edge)]}


RICHTINGEN = ["north", "east", "south", "west"]      # clockwise seen from above


def draai_richting(d, k):
    return RICHTINGEN[(RICHTINGEN.index(d) + k) % 4] if d in RICHTINGEN else d


def draai_props(props, k):
    """Block state properties turned k quarter turns clockwise (seen from above): facing, axis, rotation (signs,
    banners), the side connections of fences/panes/walls/vines, rail shapes and jigsaw orientations."""
    if not props or k % 4 == 0:
        return dict(props or {})
    out = {}
    for key, val in props.items():
        if key in RICHTINGEN:
            out[draai_richting(key, k)] = val
        elif key == "facing":
            out[key] = draai_richting(val, k)
        elif key == "axis":
            out[key] = {"x": "z", "z": "x"}.get(val, val) if k % 2 else val
        elif key == "rotation":
            out[key] = str((int(val) + 4 * k) % 16)
        elif key == "shape" and ("_" in val) and not val.startswith(("inner", "outer", "straight")):
            if val.startswith("ascending_"):
                out[key] = "ascending_" + draai_richting(val[10:], k)
            elif val in ("north_south", "east_west"):
                out[key] = val if k % 2 == 0 else {"north_south": "east_west", "east_west": "north_south"}[val]
            else:
                parts = [draai_richting(p, k) for p in val.split("_")]
                parts.sort(key=lambda d: ("north", "south", "east", "west").index(d))
                out[key] = "_".join(parts)
        elif key == "orientation" and "_" in val:
            a, b = val.split("_", 1)
            out[key] = draai_richting(a, k) + "_" + draai_richting(b, k)
        else:
            out[key] = val
    return out


def lokale_grond(s, ox, oz):
    """The ground of a village's own little building site: the polder layers everywhere, the canal (7 wide, the ice
    at GY - 1 and GY) running west -> east right south of the plot (oz + PLOT_Z .. oz + PLOT_Z + 6)."""
    sx, _, sz = s.size
    for x in range(sx):
        for z in range(sz):
            is_ice = oz + R.PLOT_Z <= z <= oz + R.PLOT_Z + 2 * R.HALF
            for y in range(0, GY - 1):
                s.blocks[(x, y, z)] = (mc("dirt") if y >= GY - 3 else mc("packed_mud") if y else mc("stone"), {}, None)
            s.blocks[(x, GY - 1, z)] = (ICE if is_ice else mc("dirt"), {}, None)
            s.blocks[(x, GY, z)] = (ICE if is_ice else GRAS, {}, None)


def dorpen(s, rng, report):
    """Builds villages 2..11: each in its own frame on a little building site (the ice south of the plot, as the
    village modules expect), checked there, then turned onto its plot beside its straight (R.plot_frame).
    Returns {index: returned dict} in template coordinates."""
    from make_structures import Structure, floats
    mods = dorp_modules()
    out = {}
    ox, oy, oz = 6, OY, 2
    for index in range(2, 12):
        mod = mods.get(index)
        lokaal = Structure((R.PLOT_X + 12, R.H, R.PLOT_Z + 14))
        lokale_grond(lokaal, ox, oz)
        if mod is None:
            res = plaatshouder(lokaal, index, ox, oy, oz, random.Random(2029060 + index))
            report.append(f"village {index} {R.NAMEN[index]}: placeholder (no module)")
        else:
            view = Wacht(lokaal, ox, oy, oz)
            res = mod.bouw(view, ox, oy, oz)
            if view.buiten:
                raise SystemExit(f"elftocht village {index} ({mod.__name__}) builds outside its plot: {sorted(set(view.buiten))[:6]}")
            if not isinstance(res, dict) or "stempelguh" not in res:
                raise SystemExit(f"elftocht village {index} ({mod.__name__}): bouw() must return a dict with 'stempelguh'")
            mod.check(lokaal, ox, oy, oz)
            report.append(f"village {index} {getattr(mod, 'NAAM', R.NAMEN[index])}: {mod.__name__}")
        f, k, yaw = R.plot_frame(index)

        def cel(x, z):
            return f(x - ox, z - oz)

        def punt(x, z):
            # a continuous position (block coordinates, a cell spans [x, x + 1)): through the cell-centre frame
            a, b = f(0, 0), f(1, 0)
            e1 = (b[0] - a[0], b[1] - a[1])
            c = f(0, 1)
            e2 = (c[0] - a[0], c[1] - a[1])
            u, v = x - ox - 0.5, z - oz - 0.5
            return (a[0] + u * e1[0] + v * e2[0] + 0.5, a[1] + u * e1[1] + v * e2[1] + 0.5)

        for (x, y, z), (name, props, nbt) in lokaal.blocks.items():
            if ox <= x < ox + R.PLOT_X and oz <= z < oz + R.PLOT_Z:
                wx, wz = cel(x, z)
                s.blocks[(wx, y, wz)] = (name, draai_props(props, k), nbt)
        for (ex, ey, ez, nbt) in lokaal.entities:
            wx, wz = punt(ex, ez)
            nbt = dict(nbt)
            rot = nbt.get("Rotation")
            if rot is not None:
                nbt["Rotation"] = floats((float(rot[0]) + yaw) % 360.0, float(rot[1]) if len(rot) > 1 else 0.0)
            s.entities.append((wx, ey, wz, nbt))

        def blok(p):
            wx, wz = cel(p[0], p[2])
            return (wx, p[1], wz)

        x, y, z, sy = res["stempelguh"]
        wx, wz = cel(x, z)
        out[index] = {"stempelguh": (wx, y, wz, (sy + yaw) % 360),
                      "publiek": [blok(p) + ((p[3] + yaw) % 360,) for p in res.get("publiek", [])],
                      "boost": [blok(p) for p in res.get("boost", [])],
                      "lichten": [blok(p) for p in res.get("lichten", [])],
                      "draai": k}
    return out


# =====================================================================================================================
# the banks, the polder and the atmosphere between the villages
# =====================================================================================================================
def oever(afstand, kant, extra=0.0):
    """A spot on the bank at this distance along the route: kant +1 = the skaters' left, -1 their right; extra blocks
    beyond the edge of the ice. Returns ((x, z) cell, (nx, nz) the unit normal pointing away from the ice)."""
    x, z, tx, tz = R.along(afstand)
    nx, nz = tz * kant, -tx * kant
    d = R.breed(afstand, kant) + 1 + extra
    return (int(round(x + nx * d)), int(round(z + nz * d))), (nx, nz)


def oevers(s, ice, reserved, rng, info):
    """Along the whole canal: lamp posts with night lampions and flags in a lively rhythm (every 10-18 blocks, mostly
    changing banks but not always), straw bales on the outside of the sharp turns."""
    pts, length, _ = R.route()
    lichten = info["lichten"]
    afstand = rng.uniform(3, 9)
    kant = 1
    k = 0
    while afstand < length - 4:
        if rng.random() < 0.78:
            kant = -kant
        for extra in (0, 1, 2):
            (bx, bz), (nx, nz) = oever(afstand, kant, extra)
            if (is_free(s, bx, bz, ice, reserved) and not R.in_plot(bx, bz, margin=1) and not near_start(bx, bz)
                    and not near_ice(bx, bz, ice, 0)):
                if k % 3 == 2 or rng.random() < 0.12:
                    vlag(s, bx, OY, bz, rng, int(round(yaw_of(-nx, -nz) / 22.5)) % 16)
                else:
                    hoog = rng.choice((3, 3, 3, 4))
                    lamppaal(s, bx, OY, bz, hoog=hoog)
                    lichten.append((bx, OY + hoog, bz))
                reserved.add((bx, bz))
                break
        k += 1
        afstand += rng.uniform(10, 18)
    # straw bales on the outside of the sharp turns
    for i in range(0, len(pts) - 24, 24):
        (x, z, tx, tz, s0), (_, _, tx2, tz2, _) = pts[i], pts[i + 24]
        turn = tx * tz2 - tz * tx2
        if abs(turn) > 0.35:
            kant = 1 if turn > 0 else -1          # (turning right: the outside is on the left)
            (bx, bz), _ = oever(s0, kant, 0)
            if is_free(s, bx, bz, ice, reserved) and not R.in_plot(bx, bz, margin=1) and not near_start(bx, bz):
                s.set(bx, OY, bz, mc("hay_block"), {"axis": "y"})
                reserved.add((bx, bz))


def near_start(x, z):
    return R.START_X - 4 <= x <= R.START_X + 6 and R.Z_E1 - 8 <= z <= R.Z_E1 + 8


def publiek_groep(s, afstand, ice, reserved, rng, info, n=4, kant=None):
    """A group of cheering guhs on the bank at this distance along the route, looking at the ice, with little flags."""
    placed = 0
    kanten = (kant,) if kant else ((1, -1) if rng.random() < 0.5 else (-1, 1))
    for kant in kanten:
        for k in range(-n, n + 1):
            if placed >= n:
                break
            (bx, bz), (nx, nz) = oever(afstand + k * 1.6, kant, 1)
            if is_free(s, bx, bz, ice, reserved) and not R.in_plot(bx, bz, margin=1) and not near_start(bx, bz):
                yaw = yaw_of(-nx, -nz)       # looking at the ice
                guh_publiek(s, bx, OY, bz, yaw, scale=rng.choice((0.8, 0.9, 1.0, 1.1)))
                info["publiek_eigen"].append((bx, OY, bz, yaw))
                reserved.add((bx, bz))
                placed += 1
                # a flag behind every other guh
                fx, fz = int(round(bx + nx * 2)), int(round(bz + nz * 2))
                if placed % 2 == 0 and is_free(s, fx, fz, ice, reserved):
                    vlag(s, fx, OY, fz, rng)
                    reserved.add((fx, fz))
        if placed:
            break
    return placed


def heuveltje(s, cx, cz, r, hoog, rng, facing="south"):
    """A sneeuwguh-heuveltje: a round snow hill with a sweet guh face (and ears) on one side."""
    for x in range(cx - r - 1, cx + r + 2):
        for z in range(cz - r - 1, cz + r + 2):
            d = math.hypot(x - cx, z - cz) / r
            if d > 1:
                continue
            top = OY + int(round(hoog * (1 - d * d)))
            for y in range(OY, top):
                s.set(x, y, z, mc("snow_block"))
            s.set(x, top, z, mc("snow"), {"layers": str(rng.choice((1, 2, 3)))})
    # the face: eyes (black wool with a blue shine), pink cheeks, a small nose, on the side facing `facing`
    fx, fz = {"south": (0, 1), "north": (0, -1), "east": (1, 0), "west": (-1, 0)}[facing]
    ux, uz = (1, 0) if fz != 0 else (0, 1)
    y = OY + max(1, hoog // 2)
    # the surface point in that direction at height y
    rr = r * math.sqrt(max(0.0, 1 - (y - OY + 0.5) / hoog)) if hoog else r
    sx, sz = int(round(cx + fx * rr)), int(round(cz + fz * rr))
    for side in (-1, 1):
        s.set(sx + ux * side * 2, y, sz + uz * side * 2, mc("black_wool"))
        s.set(sx + ux * side * 3, y - 1, sz + uz * side * 3, mc("pink_wool"))
    s.set(sx, y - 1, sz, mc("pink_concrete"))
    # ears on top
    for side in (-1, 1):
        ex, ez = cx + ux * side * max(2, r // 2), cz + uz * side * max(2, r // 2)
        topy = OY + hoog
        for y2 in range(OY, topy + 3):
            if s.get(ex, y2, ez) is None:
                s.set(ex, y2, ez, mc("snow_block") if y2 < topy + 2 else mc("pink_wool"))


def kristallen(s, ice, reserved, rng, n):
    placed = 0
    tries = 0
    while placed < n and tries < n * 30:
        tries += 1
        x, z = rng.randrange(4, R.SIZE_X - 4), rng.randrange(4, R.SIZE_Z - 4)
        if is_free(s, x, z, ice, reserved) and not near_ice(x, z, ice, R.HALF + 2) and not R.in_plot(x, z, margin=2):
            s.set(x, OY, z, "guhs:ijspegelguh_kristal")
            reserved.add((x, z))
            placed += 1
    return placed


def bruggen(s, ice, reserved, info):
    """Little arched footbridges over the canal at the spots elftocht_route picked (where the canal runs along an axis,
    in the middle of the stretches after some villages)."""
    n = 0
    for (afstand, cx, cz, along_x) in R.bruggen():
        if brug(s, cx, cz, along_x, ice, reserved, f"brug{n}"):
            n += 1
        else:
            info["report"].append(f"bridge at {(cx, cz)} didn't fit")
    info["bruggen"] = n


def tussen(fracties):
    """Distances along the route at these fractions of every stretch between two villages (away from the villages'
    straights): [(afstand, stretch index)]."""
    _, length, _ = R.route()
    out = []
    for i in range(1, 12):
        a = R.dorp_s(i) + (R.STRAIGHT + 6 if i != 1 else 34)
        b = R.dorp_s(i % 11 + 1) - (R.STRAIGHT + 6)
        if b < a:
            b += length
        for f in fracties:
            out.append(((a + (b - a) * f) % length, i))
    return out


def _kijk_naar_kanaal(cx, cz):
    d = R.along(R.distance_at(cx, cz))
    dx, dz = d[0] - cx, d[1] - cz
    return ("east" if dx > 0 else "west") if abs(dx) >= abs(dz) else ("south" if dz > 0 else "north")


def polder(s, ice, reserved, rng, info):
    """Everything between the villages, spread over the whole polder."""
    from features import elftocht_land as land
    from features import elftocht_sfeer
    report = info["report"]
    _, length, _ = R.route()
    # koek-en-zopie stalls halfway some stretches (boost spots)
    kramen = [("chocovet", ("Warme", "chocovet!")), ("snert", ("Snert", "& knabbels")), ("chocovet", ("Koek &", "Zopie")),
              ("snert", ("Snert", "& knabbels")), ("chocovet", ("Warme", "chocovet!"))]
    for (afstand, i), (soort, titel) in zip([p for p in tussen((0.5,)) if p[1] in (2, 4, 6, 8, 10)], kramen):
        spot = kraam_langs(s, afstand, soort, titel, ice, reserved, rng)
        if spot:
            info["boost"].append(spot)
        else:
            report.append(f"stall at {afstand:.0f} didn't fit")
    # audience groups all along the way (their own flags): two in every stretch
    for (afstand, i) in tussen((0.22, 0.74)):
        if not publiek_groep(s, afstand + rng.uniform(-6, 6), ice, reserved, rng, info, n=rng.choice((3, 4, 5))):
            report.append(f"audience at {afstand:.0f} didn't fit")
    # vuurkorven on the outside of the sharpest turns
    pts, _, _ = R.route()
    bochten = []
    for i in range(0, len(pts) - 40, 8):
        (x, z, tx, tz, s0), (_, _, tx2, tz2, _) = pts[i], pts[i + 40]
        turn = tx * tz2 - tz * tx2
        if abs(turn) > 0.5:
            bochten.append((abs(turn), s0 + 5, 1 if turn > 0 else -1))
    bochten.sort(reverse=True)
    gezet = []
    for (_, s0, kant) in bochten:
        if any(min(abs(s0 - g), length - abs(s0 - g)) < 50 for g in gezet):
            continue
        for extra in (1, 2, 3):
            (bx, bz), _ = oever(s0, kant, extra)
            if (is_free(s, bx, bz, ice, reserved) and not near_ice(bx, bz, ice, 0) and not R.in_plot(bx, bz, margin=1)
                    and not near_start(bx, bz)):
                vuurkorf(s, bx, OY, bz)
                info["lichten"].append((bx, OY, bz))
                reserved.add((bx, bz))
                gezet.append(s0)
                break
    # sneeuwguh-heuveltjes, the polder skating ponds and the hooibergen, spread over the open polder
    plekken = land.verspreid(s, ice, reserved, 6, afstand_ijs=(9, 30), onderling=40, rng=rng, rand=14)
    for (cx, cz), (r, hoog) in zip(plekken, ((6, 5), (5, 4), (6, 5), (4, 3), (4, 3), (3, 3))):
        spot = elftocht_sfeer._zoek(s, cx, cz, r, r, ice, reserved, 3, straal=12)
        if not spot:
            report.append(f"snow hill near {(cx, cz)} didn't fit")
            continue
        cx, cz = spot[0], spot[1]
        heuveltje(s, cx, cz, r, hoog, rng, _kijk_naar_kanaal(cx, cz))
        for a in range(-r - 1, r + 2):
            for b in range(-r - 1, r + 2):
                reserved.add((cx + a, cz + b))
    elftocht_sfeer.sfeer(s, ice, reserved, rng, info)
    # the ditches with their knotwilgen in the open fields that are left
    land.sloten(s, ice, reserved, rng, info)
    sloot = info.get("sloot_cellen", set())
    water = set(ice) | sloot
    land.knotwilg_groepjes(s, ice, reserved, rng, info)
    # sneeuwpopguhs (two blocks high) here and there near the banks
    placed = 0
    for (cx, cz) in land.verspreid(s, water, reserved, 14, afstand_ijs=(2, 9), onderling=40, rng=rng):
        if is_free(s, cx, cz, water, reserved) and s.get(cx, OY + 1, cz) is None:
            facing = _kijk_naar_kanaal(cx, cz)
            s.set(cx, OY, cz, "guhs:sneeuwpopguh", {"facing": facing, "half": "lower"})
            s.set(cx, OY + 1, cz, "guhs:sneeuwpopguh", {"facing": facing, "half": "upper"})
            reserved.add((cx, cz))
            placed += 1
    info["sneeuwpoppen"] = placed
    # glowing crystals, reed on the banks
    info["kristallen"] = kristallen(s, water, reserved, rng, 26)
    land.riet(s, ice, reserved, rng, info)


def kraam_langs(s, afstand, soort, titel, ice, reserved, rng):
    """A stall on the bank near this distance along the route (tries both banks, a bit before and after), its counter
    towards the ice."""
    for extra in (0, 8, -8, 16, -16, 24, -24, 32, -32):
        for kant in (1, -1):
            (bx, bz), (nx, nz) = oever(afstand + extra, kant, 3)
            # the counter faces the ice: the axis direction closest to -n
            dx, dz = -nx, -nz
            facing = ("east" if dx > 0 else "west") if abs(dx) >= abs(dz) else ("south" if dz > 0 else "north")
            spot = _kraam_bij(s, bx, bz, facing, soort, titel, ice, reserved, rng, shifts=(0, 2, -2))
            if spot:
                return spot
    return None


def naamborden(s, ice, reserved, info):
    """A blue-and-white place-name board (a real Dutch plaatsnaambord, on two posts) on the far bank a little before every
    village, facing the skaters that come towards it."""
    n = 0
    for index in range(2, 12):
        (cx, cz), r, kant = R.dorpen()[index]
        d = R.RICHTING[r]
        f, _, _ = R.plot_frame(index)
        # from the canal away from the plot (the far bank)
        a, b = f(0, 0), f(0, 1)
        weg = (b[0] - a[0], b[1] - a[1])
        facing = facing_of(-d[0], -d[1])
        klaar = False
        for voor in range(R.STRAIGHT + 1, R.STRAIGHT + 8):
            for extra in (0, 1):
                x = int(round(cx - d[0] * voor + weg[0] * (R.HALF + 2 + extra)))
                z = int(round(cz - d[1] * voor + weg[1] * (R.HALF + 2 + extra)))
                cells = [(x, z), (x + weg[0], z + weg[1])]
                front = [(px - d[0], pz - d[1]) for px, pz in cells]
                if (all(is_free(s, px, pz, ice, reserved) and s.get(px, OY + 1, pz) is None for px, pz in cells + front)
                        and not any(near_ice(px, pz, ice, 0) for px, pz in cells + front)):
                    for i, (px, pz) in enumerate(cells):
                        s.set(px, OY, pz, mc("spruce_fence"))
                        s.set(px, OY + 1, pz, mc("blue_concrete"))
                        fx, fz = front[i]
                        lines = ["", R.NAMEN[index], f"dorp {index} van 11", ""] if i == 0 else ["", "Elf-", "Guhjestocht", ""]
                        s.set(fx, OY + 1, fz, mc("spruce_wall_sign"), {"facing": facing, "waterlogged": "false"},
                              sign_nbt(lines, "white", True))
                        reserved.add((px, pz))
                        reserved.add((fx, fz))
                    n += 1
                    klaar = True
                    break
            if klaar:
                break
        if not klaar:
            info["report"].append(f"no room for the name board of village {index}")
    info["naamborden"] = n


def _kraam_bij(s, x0, z0, facing, soort, titel, ice, reserved, rng, shifts=(0, 3, -3, 6, -6, 9, -9)):
    """A stall near (x0, z0) with its counter towards the ice: tries a few spots around."""
    dx, dz = {"south": (0, 1), "north": (0, -1), "east": (1, 0), "west": (-1, 0)}[facing]
    u = (1, 0) if dz != 0 else (0, 1)
    for shift in shifts:
        for back in (0, 1, 2, -1):
            bx, bz = x0 + u[0] * shift - dx * back, z0 + u[1] * shift - dz * back
            cells = [(bx + u[0] * a + dx * b, bz + u[1] * a + dz * b) for a in range(-1, 6) for b in range(-1, 4)]
            if all(is_free(s, cx, cz, ice, reserved) and not R.in_plot(cx, cz, margin=1) for cx, cz in cells):
                # the counter must be 1-2 blocks from the ice
                front = [(bx + u[0] * a + dx * 3, bz + u[1] * a + dz * 3) for a in range(1, 4)]
                if any(near_ice(fx, fz, ice, 2) for fx, fz in front):
                    spot = kraam(s, bx, bz, facing, rng, soort, titel)
                    for c in cells:
                        reserved.add(c)
                    return spot
    return None


# =====================================================================================================================
# markers for what the villages returned, the entities, finishing touches
# =====================================================================================================================
KLEIN = ("candle", "taart", "carpet", "pot", "cake", "flower", "theepotje", "lantern")


def boost_blok(s, x, y, z, soort, facing="south"):
    """Puts the tray of warm cups (the boost spot) at (x, y, z) or on the counter there; when the village put something
    on that counter already: on a free bit of the counter next to it, or in place of a small thing (a cake, a candle)."""
    spot = _boost_hier(s, x, y, z, soort, facing)
    if spot:
        return spot
    for r in (1, 2):
        for dx, dz in ((r, 0), (-r, 0), (0, r), (0, -r)):
            b = s.get(x + dx, y, z + dz)
            if b not in (None, AIR) and solid(b) and s.get(x + dx, y + 1, z + dz) in (None, AIR):
                s.set(x + dx, y + 1, z + dz, KOPJES, {"facing": facing, "soort": soort})
                return (x + dx, y + 1, z + dz)
    boven = s.get(x, y + 1, z) or ""
    if solid(s.get(x, y, z)) and any(k in boven for k in KLEIN):
        s.set(x, y + 1, z, KOPJES, {"facing": facing, "soort": soort})
        return (x, y + 1, z)
    return None


def _boost_hier(s, x, y, z, soort, facing):
    b = s.get(x, y, z)
    if b is None or b == AIR or b.endswith("_carpet"):
        if s.get(x, y - 1, z) not in (None, AIR):
            s.set(x, y, z, KOPJES, {"facing": facing, "soort": soort})
            return (x, y, z)
    if b == KOPJES:
        return (x, y, z)
    if b not in (None, AIR) and s.get(x, y + 1, z) in (None, AIR):
        s.set(x, y + 1, z, KOPJES, {"facing": facing, "soort": soort})
        return (x, y + 1, z)
    return None


def licht_blok(s, x, y, z):
    """Turns a village's lamp spot into one of our night lights (lampion or vuurkorf), keeping how it hangs."""
    b = s.blocks.get((x, y, z))
    name, props = (b[0], b[1]) if b else (None, {})
    if name in (LAMPION, VUURKORF):
        return True
    if name and "campfire" in name:
        s.set(x, y, z, VUURKORF, {"lit": "false"})
        return True
    if name is None or name == AIR or "lantern" in name or "lampion" in name:
        hanging = props.get("hanging") == "true" if props else False
        below, above = s.get(x, y - 1, z), s.get(x, y + 1, z)
        if name is None or name == AIR:
            if below not in (None, AIR) and not hulp.passable(below):
                hanging = False
            elif above not in (None, AIR):
                hanging = True
            else:
                return False
        s.set(x, y, z, LAMPION, {"hanging": "true" if hanging else "false", "lit": "false", "waterlogged": "false"})
        return True
    # a lamp that is always on (sea lantern, crystal lamp, candle...): the village's own choice, it glows anyway
    return True


def schaatsmeester(s, x, y, z):
    from make_structures import Byte, floats, Float
    s.entity(x + 0.5, y, z + 0.5, {"id": "guhs:guh_npc", "Kind": "schaatsmeesterguh", "PersistenceRequired": Byte(1),
                                   "Rotation": floats(0.0, 0.0),
                                   # where the start is, in his own frame (he faces the ice): 5 forward, 1 to his left
                                   "RoleData": {"StartVooruit": Float(float(R.Z_E1 - z)), "StartLinks": Float(float(R.START_X - 3 - x))}})


def lucht(s, ice):
    """Air above the ground where nothing is built, so hills and trees of the world don't poke through the polder
    (higher over the ice and the villages: nobody bumps their head)."""
    for x in range(R.SIZE_X):
        for z in range(R.SIZE_Z):
            top = GY + (12 if (x, z) in ice or R.in_plot(x, z) else 8)
            for y in range(OY, top):
                if (x, y, z) not in s.blocks:
                    s.blocks[(x, y, z)] = (AIR, {}, None)


def standin(s):
    n = 0
    for pos, (name, props, nbt) in list(s.blocks.items()):
        if name in STANDIN:
            new, newprops = STANDIN[name]
            s.blocks[pos] = (new, dict(props) if newprops is None else newprops, nbt)
            n += 1
    return n


def anchor(s):
    x, y, z = R.ANCHOR
    s.set(x, y, z, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": "guhs:elfguhjestocht_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": GRAS, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})


# =====================================================================================================================
# the whole thing
# =====================================================================================================================
def build(h, echt=True):
    rng = random.Random(20290601)
    R.check_route()
    pts, length, ice = R.route()
    s = h.Structure((R.SIZE_X, R.H, R.SIZE_Z))
    report = []
    info = {"report": report, "publiek": [], "publiek_eigen": [], "boost": [], "lichten": [], "length": length}
    grond(s, ice, rng)
    reserved = set()
    # the start
    info["startlijn"] = startlijn(s, ice)
    gw = guhwarden(s, rng)
    startboog(s, rng)
    for x in range(R.START_X - 4, R.START_X + 7):
        for z in range(R.Z_E1 - R.HALF - 3, R.Z_E1 + R.HALF + 4):
            reserved.add((x, z))
    # the villages (their plots only)
    dorpen_res = dorpen(s, rng, report)
    dorpen_res[1] = gw
    # the polder in between
    bruggen(s, ice, reserved, info)
    naamborden(s, ice, reserved, info)
    oevers(s, ice, reserved, rng, info)
    polder(s, ice, reserved, rng, info)
    info["pijlen"] = pijlen(s, ice, info["startlijn"])
    # what the villages returned: the Stempelguhs (placeholders place their own; Guhwarden's here), audience, boosts, lights
    stempels = {}
    for index in range(1, 12):
        res = dorpen_res[index]
        x, y, z, yaw = res["stempelguh"]
        stempels[index] = (x, y, z, yaw)
        if index == 1:
            stempelguh(s, x, y, z, yaw, 1)
        for (px, py, pz, pyaw) in res.get("publiek", []):
            if index == 1:
                guh_publiek(s, px, py, pz, pyaw)
            info["publiek"].append((px, py, pz, pyaw))
        for (bx, by, bz) in res.get("boost", []):
            spot = boost_blok(s, bx, by, bz, "snert" if index in (1, 8) and len(info["boost"]) % 2 else "chocovet",
                              draai_richting("south", res.get("draai", 0)))
            if spot:
                info["boost"].append(spot)
            else:
                report.append(f"village {index}: boost spot {(bx, by, bz)} has no room for the cups")
        for (lx, ly, lz) in res.get("lichten", []):
            if licht_blok(s, lx, ly, lz):
                info["lichten"].append((lx, ly, lz))
            else:
                report.append(f"village {index}: light spot {(lx, ly, lz)} can't hold a lampion")
    info["stempels"] = stempels
    sm = gw["schaatsmeester"]
    schaatsmeester(s, *sm)
    info["schaatsmeester"] = sm
    # 2.10.1: a Reisguh on Guhwarden's terrace, between the two koek-en-zopie stalls, looking at the ice: the tour's waypoint
    from make_structures import Byte, floats
    from features import reisguh_plek
    gdx = R.GUHWARDEN[0] - 18
    info["reisguh"] = reisguh_plek.zet(s, reisguh_plek.rondom(29 + gdx, OY, R.GUHWARDEN[3] - 3), "Guhwarden", 0, Byte, floats,
                                       "(Elf-Guhjestocht)")
    # the gentle snowy relief over the open polder (flat pads under everything), then the plants on the flat bits
    from features import elftocht_land as land
    land.relief(s, ice, info)
    land.plantjes(s, set(ice) | info.get("sloot_cellen", set()), reserved, rng)
    lucht(s, ice)
    anchor(s)
    info["standin"] = 0 if echt else standin(s)
    info["ice"] = ice
    return s, info


# =====================================================================================================================
# the geometry self-check
# =====================================================================================================================
def solid(name):
    return name not in (None, AIR) and not hulp.passable(name)


def check(s, info):
    problems = []
    ice = info["ice"]
    icey = {GY: ice}
    # the canal is free: two blocks of air (or passable) above every ice cell, except the bridges (3 free) and the arch
    for (x, z) in ice:
        for y in (OY, OY + 1, OY + 2):
            b = s.get(x, y, z)
            if b not in (None, AIR) and not hulp.passable(b):
                problems.append(f"something on the ice at {(x, y, z)}: {b}")
                break
        top = s.get(x, GY, z)
        if top not in (ICE, "minecraft:blue_ice", "minecraft:packed_ice", "minecraft:snow_block") and not (top or "").startswith("minecraft:jigsaw"):
            problems.append(f"ice cell without ice at {(x, GY, z)}: {top}")
    # the Stempelguhs: standing on solid ground, near the ice, room for their head
    for index, (x, y, z, yaw) in info["stempels"].items():
        if not solid(s.get(x, y - 1, z)):
            problems.append(f"Stempelguh {index} has no floor at {(x, y - 1, z)}")
        if solid(s.get(x, y, z)) or solid(s.get(x, y + 1, z)):
            problems.append(f"Stempelguh {index} stands in a block at {(x, y, z)}")
        if not near_ice(x, z, ice, 4):
            problems.append(f"Stempelguh {index} is not near the ice at {(x, y, z)}")
        if not _reach(s, x, y, z, ice):
            problems.append(f"Stempelguh {index} can't be reached from the ice")
    # the Stempelguh entities: exactly one per village
    tags = {}
    for (ex, ey, ez, nbt) in s.entities:
        if nbt.get("Kind") == "stempelguh":
            for t in nbt.get("Tags", []):
                tags[t] = tags.get(t, 0) + 1
    for index in range(1, 12):
        if tags.get(f"guhs_elftocht_dorp_{index}", 0) != 1:
            problems.append(f"village {index}: {tags.get(f'guhs_elftocht_dorp_{index}', 0)} Stempelguhs")
    # every guh and NPC stands on something solid with room
    for (ex, ey, ez, nbt) in s.entities:
        x, y, z = int(math.floor(ex)), int(math.floor(ey)), int(math.floor(ez))
        dorp = R.in_plot(x, z)
        waar = problems if dorp < 2 else info["report"]
        pre = "" if dorp < 2 else f"WARNING village {dorp}: "
        if not solid(s.get(x, y - 1, z)) and not (s.get(x, y, z) or "").endswith(("_slab", "_plaat", "_carpet")):
            waar.append(f"{pre}{nbt.get('id')} floats at {(x, y, z)} (under it: {s.get(x, y - 1, z)})")
        if solid(s.get(x, y, z)) and not (s.get(x, y, z) or "").endswith(("_slab", "_plaat", "_carpet", "_trap", "_stairs")):
            waar.append(f"{pre}{nbt.get('id')} is stuck in {s.get(x, y, z)} at {(x, y, z)}")
    # the audience guhs: 2..8 per village and they carry the tag
    for (x, y, z, yaw) in info["publiek"] + info["publiek_eigen"]:
        pass
    # 2.10.1: exactly one Reisguh (in Guhwarden)
    from features import reisguh_plek
    if reisguh_plek.aantal(s) != 1 or R.in_plot(info["reisguh"][0], info["reisguh"][2]) != 1:
        problems.append(f"{reisguh_plek.aantal(s)} Reisguhs, or not in Guhwarden: {info.get('reisguh')}")
    # the Schaatsmeester stands next to the start
    x, y, z = info["schaatsmeester"]
    if not solid(s.get(x, y - 1, z)) or not near_ice(x, z, ice, 2):
        problems.append("the Schaatsmeester doesn't stand at the start")
    # boosts and lights exist
    if len(info["boost"]) < 4:
        problems.append(f"only {len(info['boost'])} boost spots")
    for (x, y, z) in info["boost"]:
        if s.get(x, y, z) != KOPJES or not solid(s.get(x, y - 1, z)):
            problems.append(f"boost spot {(x, y, z)} is not a tray on a counter")
    if len(info["lichten"]) < 40:
        problems.append(f"only {len(info['lichten'])} night lights")
    # nothing floating (everything hangs together with the ground)
    loose = hulp.check_floating(s, GY)
    loose = [p for p in loose if s.get(*p) not in ("minecraft:snow",)]
    if loose:
        problems.append(f"{len(loose)} floating blocks, e.g. {loose[:5]} ({s.get(*loose[0])})")
    # whole doors
    for (x, y, z), (name, props, _) in list(s.blocks.items()):
        if name.endswith("_deur") or name.endswith("_door"):
            other = (x, y + 1, z) if props.get("half") == "lower" else (x, y - 1, z)
            if s.get(*other) != name:
                problems.append(f"half a door at {(x, y, z)}")
        if name == "guhs:sneeuwpopguh" and props.get("half") == "lower" and s.get(x, y + 1, z) != name:
            problems.append(f"half a sneeuwpopguh at {(x, y, z)}")
    # the anchor
    if s.get(*R.ANCHOR) != "minecraft:jigsaw":
        problems.append("no anchor jigsaw")
    # the relief: at most ~2 blocks of snow, nothing of it right on the banks of the canal and the ditches
    sloot = info.get("sloot_cellen", set())
    for (x, z), e in info.get("reliëf", {}).items():
        if e > 8 * 2:
            problems.append(f"relief of {e / 8:.2f} blocks at {(x, z)}")
            break
    for (x, z) in list(ice) + list(sloot):
        for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + a, z + b)
            if n in ice or n in sloot or R.in_plot(n[0], n[1], margin=1):
                continue
            if (s.get(n[0], OY, n[1]) == "minecraft:snow_block"
                    or (s.get(n[0], OY, n[1]) == "minecraft:snow" and int(s.blocks[(n[0], OY, n[1])][1].get("layers", "1")) > 1)):
                problems.append(f"a snow bank right at the ice at {(n[0], OY, n[1])}")
                break
    if not 5 <= len(info.get("sloten", [])) <= 8:
        problems.append(f"{len(info.get('sloten', []))} ditches (5..8)")
    if sum(1 for d in info.get("sloten", []) if d["kanaal"]) < 2:
        problems.append("hardly any ditch runs into the canal")
    if problems:
        raise SystemExit("elftocht geometry: " + "; ".join(problems[:14]) + (f" (+{len(problems) - 14} more)" if len(problems) > 14 else ""))


def _reach(s, x, y, z, ice):
    """A short walk from the spot next to the Stempelguh to the ice (feet level y, steps of one block up/down)."""
    seen = {(x, y, z)}
    q = deque([(x, y, z, 0)])
    while q:
        cx, cy, cz, d = q.popleft()
        if (cx, cz) in ice and cy == OY:
            return True
        if d > 10:
            continue
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, -1, 1):
                n = (cx + dx, cy + dy, cz + dz)
                if n in seen:
                    continue
                if not solid(s.get(n[0], n[1] - 1, n[2])):
                    continue
                if solid(s.get(*n)) or solid(s.get(n[0], n[1] + 1, n[2])):
                    continue
                seen.add(n)
                q.append(n + (d + 1,))
    return False
