"""
3.0 (Guhverhalen), slice timmerguh: De bouwplaats van de Timmerguh (DESIGN_30 par. 1) - the template
knuffeldal_stadje/bouwplaats (22 x 26 x 33, ground top at y = G = 4). Every Knuffeldal town has one: it hangs on the west end
of hoek_noordwest's street (knuffeldal_stadje.BOUWPLAATS_*: jigsaw guhs:knuffeldal_bouwplaats -> pool
guhs:knuffeldal_stadje/bouwplaats); its own jigsaw guhs:bouwplaats_ingang sits at (21, G, 16) east_up, where the street comes in.

What is on it (the street comes in from the east, z 15..17):
  - a half-built GUHHUISJE, a big guh head (11 x 11, its face looks east at the street: the eyes are windows, the door is its
    mouth): the walls stand, the lower rim of the fluffy dome roof is on, but the top of the dome and the two round ears are
    still see-through ghost tiles (guhs:timmerguh_dakplek, deel = dak / oor / binnenoor). The player lays them in the
    Timmerguh's quest with the loaned dakpluisjes (feature.timmerguh.DakpluisjeItem): dak -> guhs:pluisdak, oor ->
    pink wool, binnenoor -> magenta wool. A steigertje (scaffolding with a plank walkway and a ladder) along its south
    wall to climb up, dakbalken over the open roof, a spot for the flag on the top (de vlag in top!);
  - the Timmerguh (NPC timmerguh) in the yard by the gate, looking at the street;
  - a bouwkeet (a little site hut with a guh face: the bouwtekening on the table), a zaagbok with a plank on it, stacks of
    planks and bales of pink wool, a pink bouwkraan with guh ears on its top and a pallet of pink wool on its hook, a
    kruiwagen, a gereedschapskist, a lunch table (kaasknabbeltaart!) where a helper guh with a timmermanshelmpje sits,
    a bouwbord, lamps, pluizenbomen and flowers, a low fence round the yard.

check() (knuffeldal_stadje.check + these rules) raises SystemExit on problems: everything walkable from the street, the ghost
tiles are there (dak + two ears) and reachable from the scaffolding, the Timmerguh stands on a floor, the jigsaw is right.
"""
import json
import math

from features import knuffeldal_stadje as st

G = st.G
W, H, D = 22, 26, 33
JIGSAW = (W - 1, G, 16)
NAAM = st.BOUWPLAATS_STUK                    # knuffeldal_stadje/bouwplaats
DAKPLEK = "guhs:timmerguh_dakplek"
DELEN = {"dak": st.DAK, "oor": "minecraft:pink_wool", "binnenoor": "minecraft:magenta_wool"}   # what a ghost tile becomes
NPC = "timmerguh"
FENCE = "minecraft:spruce_fence"
# the house: a guh head, 11 x 11 (x 1..11, z 11..21), its face (and door) on the east wall
HX0, HX1, HZ0, HZ1 = 1, 11, 11, 21
WALL_TOP = G + 5
ROOF = G + 6                                 # the dome starts here
STAP = 1.35                                  # how much each level of the dome shrinks
KLAAR = 2                                    # the lowest rows of the dome are on already (the rest are ghost tiles)
HELPER_KLEDING = "timmer_helmpje"
OOR = ((-1, 0), (0, 0), (1, 0), (-1, 1), (0, 1), (1, 1), (0, 2))   # an ear: (sideways, up) from its foot on the dome
BINNENOOR = ((0, 0), (0, 1))


def fence_props():
    return {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}


def sign_nbt(h, keys, colour="black"):
    ms = h.ms
    blank = json.dumps("")
    msgs = [json.dumps({"translate": k}) if k else blank for k in keys] + [blank] * (4 - len(keys))
    return {"id": "minecraft:sign", "is_waxed": ms.Byte(1),
            "front_text": {"messages": ms.NbtList(8, msgs), "color": colour, "has_glowing_text": ms.Byte(0)},
            "back_text": {"messages": ms.NbtList(8, [blank] * 4), "color": "black", "has_glowing_text": ms.Byte(0)}}


# =====================================================================================================================
# the half-built guhhuisje
# =====================================================================================================================
def huis(b):
    """The walls, the face, the dome (rim done, top ghost) and the ears (ghost). Returns the ghost tiles {pos: deel}."""
    mc = b.h.mc
    cx, cz = (HX0 + HX1) / 2, (HZ0 + HZ1) / 2
    rx, rz = (HX1 - HX0 + 1) / 2, (HZ1 - HZ0 + 1) / 2

    def inside(x, z, shrink=0.0):
        return ((x - cx) / (rx - shrink)) ** 4 + ((z - cz) / (rz - shrink)) ** 4 <= 1.0

    cells = [(x, z) for x in range(HX0, HX1 + 1) for z in range(HZ0, HZ1 + 1) if inside(x, z)]
    inner = {(x, z) for (x, z) in cells if inside(x, z, 1.0)}
    for (x, z) in cells:
        b.set(x, G, z, mc("cherry_planks") if (x, z) in inner else st.KS)
        for y in range(G + 1, WALL_TOP + 1):
            if (x, z) in inner:
                b.air(x, y, z, x, y, z)
            else:
                b.set(x, y, z, "minecraft:pink_wool" if y == WALL_TOP else st.KS)
    # the dome: stepped rings (each level two blocks wide, resting on the one below), a little cap on top; its lowest
    # KLAAR levels are fluffy roof already, the upper levels are still ghost tiles
    niveaus = {}
    k = 0
    while k * STAP < min(rx, rz) - 0.5:
        regio = {(x, z) for (x, z) in cells if inside(x, z, k * STAP)}
        if not regio:
            break
        niveaus[k] = regio
        k += 1
    hoogste = max(niveaus)
    tiles = {}
    top = {}
    for k, regio in niveaus.items():
        binnen = niveaus.get(k + 2, set()) if k + 2 <= hoogste else set()
        y = ROOF + k
        for (x, z) in regio - binnen:
            top[(x, z)] = y
            if k < KLAAR:
                b.set(x, y, z, st.DAK)
            else:
                b.set(x, y, z, DAKPLEK, {"deel": "dak"})
                tiles[(x, y, z)] = "dak"
    # the ears: two round ears on the dome (left and right of the face), their inside pink
    ex = int(round(cx))
    for sz in (-1, 1):
        ez = int(round(cz + sz * (rz - 3.0)))
        ey = top[(ex, ez)] + 1
        for (du, dv) in OOR:
            deel = "binnenoor" if (du, dv) in BINNENOOR else "oor"
            b.set(ex, ey + dv, ez + du, DAKPLEK, {"deel": deel})
            tiles[(ex, ey + dv, ez + du)] = deel
    # the dakbalken over the open room, under the dome (the roof's frame)
    for z in range(HZ0 + 2, HZ1 - 1, 3):
        for x in range(HX0, HX1 + 1):
            if (x, z) in inner and b.get(x, WALL_TOP, z) is None:
                b.set(x, WALL_TOP, z, mc("stripped_spruce_log"), {"axis": "x"})
    # the face on the front wall (east, x = HX1): eye windows, blush, a nose; the door is the mouth
    fz = int(round(cz))
    blocks = dict(st.FACE_BLOCKS)
    blocks.update({"eye": "minecraft:black_stained_glass", "ring": "minecraft:light_blue_stained_glass",
                   "shine": "minecraft:white_stained_glass", "skin": st.KS})
    st.face_on_wall(b, cells, inner, "east", fz, G + 3, 3.9, blocks, skip=lambda x, y, z, role: y <= G + 2 and abs(z - fz) <= 1)
    front = max(x for (x, z) in cells if z == fz)
    b.set(front, G + 1, fz, st.DOOR, {"facing": "west", "half": "lower", "hinge": "left", "open": "false", "powered": "false"})
    b.set(front, G + 2, fz, st.DOOR, {"facing": "west", "half": "upper", "hinge": "left", "open": "false", "powered": "false"})
    b.set(front + 1, G + 1, fz - 1, mc("potted_pink_tulip"))
    b.set(front + 1, G + 1, fz + 1, mc("potted_allium"))
    # the back and the sides: little knuffelsteen faces and a window each side
    for zz, facing in ((min(z for x, z in cells), "north"), (max(z for x, z in cells), "south")):
        for dx in (-1, 0, 1):
            b.set(ex + dx, G + 3, zz, mc("pink_stained_glass_pane"), st.pane(False))
        b.set(ex - 3, G + 4, zz, st.KS_FACE, {"facing": facing, "stemming": "1"})
        b.gezichten += 1
    back = min(x for (x, z) in cells if z == fz)
    for (u, v) in ((0, 1), (0, 2), (1, 3), (2, 3), (3, 2), (3, 1), (2, 0)):   # the curly guh tail on the back wall
        b.set(back - 1, G + v, fz + u - 1, mc("pink_wool"))
    # inside: a workbench, a toolbox, a rug, a lantern hanging from a dakbalk, a sawhorse with a plank
    b.set(HX0 + 2, G + 1, HZ0 + 2, mc("crafting_table"))
    b.set(HX0 + 3, G + 1, HZ0 + 2, mc("barrel"), {"facing": "up", "open": "false"})
    for x in range(int(cx) - 1, int(cx) + 2):
        for z in range(fz - 1, fz + 2):
            if (x, z) in inner and b.get(x, G + 1, z) is None:
                b.set(x, G + 1, z, mc("pink_carpet"))
    lamp_z = [z for z in range(HZ0 + 2, HZ1 - 1, 3)][1]
    b.set(int(cx), WALL_TOP - 1, lamp_z, st.LAMP, {"hanging": "true", "waterlogged": "false"})
    return tiles, cells, top


def steiger(b, cells):
    """The steigertje along the south wall: scaffolding with a plank walkway on top, a ladder up at its east end."""
    mc = b.h.mc
    zs = max(z for x, z in cells) + 1          # just outside the south wall
    xs = [x for x, z in cells if z == zs - 1]
    x0, x1 = min(xs) - 1, max(xs) + 1
    for x in range(x0, x1 + 1):
        for y in range(G + 1, WALL_TOP):
            if x in (x0, x1) or (x - x0) % 3 == 0:
                b.set(x, y, zs, mc("scaffolding"), {"bottom": "false", "distance": "0", "waterlogged": "false"})
        b.set(x, WALL_TOP, zs, mc("spruce_slab"), {"type": "bottom", "waterlogged": "false"})
    # the ladder, on a post of planks at the east end, up to the walkway
    lx = x1 + 2
    for y in range(G + 1, WALL_TOP + 1):
        b.set(lx, y, zs, mc("spruce_planks"))
        b.set(lx - 1, y, zs, st.LADDER, {"facing": "west", "waterlogged": "false"})
    b.set(lx - 1, WALL_TOP, zs, st.LADDER, {"facing": "west", "waterlogged": "false"})
    b.set(x1, WALL_TOP, zs, mc("spruce_slab"), {"type": "bottom", "waterlogged": "false"})
    b.targets["de steiger"] = (x0 + 2, WALL_TOP + 1, zs)
    return zs


# =====================================================================================================================
# the yard
# =====================================================================================================================
def bouwkeet(b, x0, z0):
    """A little site hut (5 x 5) with a pluisdak roof, a guh face on its front (looking east), the bouwtekening inside."""
    mc = b.h.mc
    x1, z1 = x0 + 4, z0 + 4
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = x in (x0, x1) or z in (z0, z1)
            b.set(x, G, z, mc("cherry_planks"))
            for y in range(G + 1, G + 4):
                if edge:
                    corner = x in (x0, x1) and z in (z0, z1)
                    b.set(x, y, z, mc("stripped_cherry_log") if corner else mc("cherry_planks"), {"axis": "y"} if corner else None)
                else:
                    b.air(x, y, z, x, y, z)
    for x in range(x0 - 1, x1 + 2):                                    # the roof: a flat fluffy roof with a rim
        for z in range(z0 - 1, z1 + 2):
            b.set(x, G + 4, z, st.DAK_PLAAT if x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1) else st.DAK,
                  {"type": "bottom"} if x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1) else None)
    zm = z0 + 2
    b.set(x1, G + 1, zm, st.DOOR, {"facing": "west", "half": "lower", "hinge": "left", "open": "false", "powered": "false"})
    b.set(x1, G + 2, zm, st.DOOR, {"facing": "west", "half": "upper", "hinge": "left", "open": "false", "powered": "false"})
    b.set(x1, G + 3, zm - 1, mc("black_stained_glass"))                   # its little face over the door: eye windows
    b.set(x1, G + 3, zm + 1, mc("black_stained_glass"))
    b.set(x1 + 1, G + 3, zm, st.KS_FACE, {"facing": "east", "stemming": "0"})
    b.gezichten += 1
    for z in (z0 + 1, z1 - 1):
        b.set(x0, G + 2, z, mc("pink_stained_glass_pane"), st.pane(True))
    b.set(x0 + 1, G + 1, z0 + 1, mc("cartography_table"))                 # the bouwtekening
    b.set(x0 + 1, G + 1, z1 - 1, "guhs:guh_stoel", {"facing": "north"})
    b.set(x0 + 2, G + 3, zm, st.LAMP, {"hanging": "true", "waterlogged": "false"})
    b.set(x1 + 1, G + 2, zm + 1, mc("oak_wall_sign"), {"facing": "east", "waterlogged": "false"},
          sign_nbt(b.h, ["sign.guhs.timmerguh_keet1", "sign.guhs.timmerguh_keet2"], "brown"))
    b.targets["in de bouwkeet"] = (x0 + 2, G + 1, zm)


def kraan(b, x, z):
    """A pink bouwkraan with two guh ears on its top, its jib over the house, a pallet of pink wool on the hook."""
    mc = b.h.mc
    top = G + 15
    for y in range(G + 1, top + 1):
        b.set(x, y, z, mc("pink_concrete") if (y - G) % 3 else mc("white_concrete"))
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        b.set(x + dx, G + 1, z + dz, mc("pink_concrete"))                   # its foot
    for dx in range(-7, 3):                                                # the jib (west over the house) and the counterweight
        b.set(x + dx, top + 1, z, mc("pink_concrete") if dx % 2 else mc("white_concrete"))
    b.set(x + 3, top + 1, z, mc("gray_concrete"))
    b.set(x + 3, top + 2, z, mc("gray_concrete"))
    for dz in (-1, 1):                                                     # the ears on its cab
        b.set(x, top + 2, z + dz, mc("pink_wool"))
        b.set(x, top + 3, z + dz, mc("pink_wool"))
    b.set(x, top + 2, z, mc("pink_wool"))
    b.set(x + 1, top + 2, z, st.KS_FACE, {"facing": "east", "stemming": "2"})
    b.gezichten += 1
    hx = x - 5                                                             # the hook with a pallet of pink wool
    for y in range(top - 3, top + 1):
        b.set(hx, y, z, mc("chain"), {"axis": "y", "waterlogged": "false"})
    b.set(hx, top - 4, z, mc("pink_wool"))
    b.set(hx, top - 5, z, mc("spruce_slab"), {"type": "top", "waterlogged": "false"})
    b.set(x + 2, top + 2, z, st.LAMP, {"hanging": "false", "waterlogged": "false"})


def zaagbok(b, x, z):
    """A sawhorse: two little fence legs with a slab, a plank lying across it (and a saw? the axe is in the toolbox)."""
    mc = b.h.mc
    for dx in (0, 2):
        b.set(x + dx, G + 1, z, FENCE, fence_props())
    for dx in range(0, 3):
        b.set(x + dx, G + 2, z, mc("oak_slab"), {"type": "bottom", "waterlogged": "false"})


def stapels(b, x, z):
    """Stacks of planks (oak, birch, cherry) and bales of pink wool with a ribbon (the roof material)."""
    mc = b.h.mc
    for i, (hout, n) in enumerate((("oak_planks", 3), ("birch_planks", 2), ("cherry_planks", 2))):
        for y in range(G + 1, G + 1 + n):
            b.set(x + i, y, z, mc(hout))
        b.set(x + i, G + 1 + n, z, mc(hout.replace("planks", "slab")), {"type": "bottom", "waterlogged": "false"})
    for (dx, dz, n) in ((0, 2, 2), (1, 2, 1), (0, 3, 1)):
        for y in range(G + 1, G + 1 + n):
            b.set(x + dx, y, z + dz, mc("pink_wool"))
    b.set(x + 1, G + 2, z + 2, mc("white_carpet"))


def lunch(b, x, z):
    """The lunch table: a guh table with a kaasknabbeltaart, two chairs, a helper guh with a timmermanshelmpje."""
    b.set(x, G + 1, z, "guhs:guh_tafel", {"facing": "south"})
    b.set(x - 1, G + 1, z, "guhs:guh_stoel", {"facing": "east"})
    b.set(x + 1, G + 1, z, "guhs:guh_stoel", {"facing": "west"})
    b.set(x, G + 2, z, st.LAMP, {"hanging": "false", "waterlogged": "false"})
    b.bewoner(x, G + 1, z + 1, "timmertje", "normal", 0.7, "north")
    x_, y_, z_, nbt = b.s.entities[-1]
    nbt["ClothesHead"] = HELPER_KLEDING


def build(h):
    """The template (checked); returns (Bouw, ghost tiles)."""
    b = st.Bouw(h, (W, H, D), NAAM)
    mc = h.mc
    for x in range(W):
        for z in range(D):
            for y in range(G):
                b.set(x, y, z, mc("pink_wool"))
            b.set(x, G, z, st.GRAS)
    # the street in from the east, a yard of klinkers and a bit of sandy building ground round the house
    for x in range(12, W):
        for z in range(15, 18):
            b.set(x, G, z, st.KLINK)
    for x in range(0, 13):
        for z in range(8, 25):
            if b.get(x, G, z) == st.GRAS:
                b.set(x, G, z, mc("packed_mud") if (x * 3 + z * 5) % 7 == 0 else mc("coarse_dirt") if (x + z) % 5 == 0 else mc("dirt_path"))
    tiles, cells, top = huis(b)
    zs = steiger(b, cells)
    # the low fence round the yard (gaps: the street in the east, a gate to the meadow in the south)
    for x in range(0, W - 3):
        for z in (6, 28):
            if not (z == 28 and 10 <= x <= 11):
                b.set(x, G + 1, z, FENCE, fence_props())
    for z in range(6, 29):
        if not (14 <= z <= 18):
            b.set(W - 4, G + 1, z, FENCE, fence_props())
        b.set(0, G + 1, z, FENCE, fence_props())
    for z in (14, 18):                                                     # the gate posts: lamps
        b.lantaarnpaal(W - 4, z, height=3)
    # the yard
    bouwkeet(b, 14, 24)
    kraan(b, 13, 10)
    zaagbok(b, 5, 25)
    stapels(b, 1, 23)
    lunch(b, 16, 8)
    # the kruiwagen and the gereedschapskist by the house
    b.set(16, G + 1, 20, mc("composter"), {"level": "3"})
    b.set(14, G + 1, 19, mc("chest"), {"facing": "north", "type": "single", "waterlogged": "false"})
    b.set(15, G + 1, 19, mc("smithing_table"))
    # the bouwbord by the gate
    b.set(W - 3, G + 1, 19, FENCE, fence_props())
    b.set(W - 3, G + 2, 19, mc("oak_sign"), {"rotation": "12", "waterlogged": "false"},
          sign_nbt(h, ["sign.guhs.timmerguh_bord1", "sign.guhs.timmerguh_bord2", "sign.guhs.timmerguh_bord3", "sign.guhs.timmerguh_bord4"]))
    # the Timmerguh, in the yard by the gate, looking at the street
    b.npc(15, G + 1, 16, NPC, "east")
    # the meadow round it: pluizenbomen, lamps, flower boxes, benches, leaf piles
    for (x, z) in ((3, 2), (10, 2), (18, 2), (13, 31), (19, 30)):
        b.boompje(x, z, height=4, r=2.2)
    for x in (6, 14):
        b.lantaarnpaal(x, 4)
    b.lantaarnpaal(8, 30)
    b.bloembak(W - 3, 13)
    b.bloembak(W - 3, 21)
    b.bank(4, 30, "north")
    for (x, z) in ((7, 3), (16, 31), (2, 28)):
        if b.get(x, G + 1, z) is None:
            b.set(x, G + 1, z, st.HOOP, {"vol": "false"})
    b.grasveld(0, 0, W - 1, 7, 0.14)
    b.grasveld(0, 28, W - 1, D - 1, 0.14)
    b.grasveld(W - 3, 0, W - 1, D - 1, 0.1)
    b.jigsaw(*JIGSAW, "east_up", "guhs:bouwplaats_ingang", "minecraft:empty", "minecraft:empty", st.KLINK)
    b.starts = [(W - 2, G + 1, 16)]
    b.targets["de voordeur"] = (HX1 + 1, G + 1, 16)
    b.s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=H)
    b.tiles = tiles
    b.zs = zs
    return b


def check_bouwplaats(b):
    problems = []
    tiles = b.tiles
    dak = [p for p, d in tiles.items() if d == "dak"]
    oren = [p for p, d in tiles.items() if d != "dak"]
    if not 16 <= len(dak) <= 40 or len(oren) != 2 * len(OOR):
        problems.append(f"bouwplaats: {len(dak)} dak tiles and {len(oren)} ear tiles")
    for p, d in tiles.items():
        if b.get(*p) != DAKPLEK:
            problems.append(f"bouwplaats: the ghost tile at {p} is {b.get(*p)}")
    # the ghost tiles can be reached from the scaffolding walkway (within 5 blocks of someone standing on it or on the rim)
    stand = [p for p in b.reach if p[1] >= WALL_TOP]
    far = [p for p in tiles if not any(math.dist((p[0] + 0.5, p[1] + 0.5, p[2] + 0.5), (s[0] + 0.5, s[1] + 1.6, s[2] + 0.5)) <= 4.5 for s in stand)]
    if far:
        problems.append(f"bouwplaats: {len(far)} ghost tiles can't be reached from the roof/scaffolding, e.g. {far[:3]}")
    js = b.jigsaws
    if len(js) != 1 or js[0][0] != JIGSAW or js[0][1] != "guhs:bouwplaats_ingang" or js[0][4] != "east_up":
        problems.append(f"bouwplaats: jigsaws {js}")
    npcs = [e for e in b.s.entities if e[3]["id"] == "guhs:guh_npc"]
    if [e[3]["Kind"] for e in npcs] != [NPC]:
        problems.append(f"bouwplaats: NPCs {[e[3]['Kind'] for e in npcs]}")
    # the flag spot on the top of the dome is free
    return problems


def make(h):
    b = build(h)
    b.reach = st.walk(b, b.starts)
    problems = st.check(b, check_bouwplaats)
    if problems:
        raise SystemExit("timmerguh bouwplaats geometry check failed:\n  " + "\n  ".join(problems[:60]))
    b.s.save(NAAM)
    print(f"timmerguh: bouwplaats ok ({len(b.tiles)} ghost tiles, {b.walkable} walkable, {b.gezichten} faces)")
    return b


if __name__ == "__main__":
    # the self-check on its own:  python tools/features/timmerguh_bouw.py  (from the project root; saves nothing)
    import os
    import sys
    import types
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    import make_structures as ms
    stub = types.SimpleNamespace(mc=ms.mc, Structure=ms.Structure, ms=ms)
    bb = build(stub)
    bb.reach = st.walk(bb, bb.starts)
    found = st.check(bb, check_bouwplaats)
    print(len(bb.s.blocks), "blocks,", bb.walkable, "walkable,", len(bb.tiles), "tiles:",
          {d: sum(1 for v in bb.tiles.values() if v == d) for d in ("dak", "oor", "binnenoor")})
    print("\n".join(found[:80]) if found else "geometry check ok")
    if "--png" in sys.argv:
        import wiki_renders as wr
        img = wr.render_structure(bb.s, {}, px=12)
        out = sys.argv[sys.argv.index("--png") + 1]
        img.save(out)
        print("wrote", out)
