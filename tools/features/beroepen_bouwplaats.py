"""
De beroepen (2.9) - Bob de Guhbouwer's bouwplaats: the half-built house in the guh village's third layout
(guh_village/layout_c; make_structures.guh_village(..., bouwplaats=bouwplaats) builds it instead of one home plot).

The house is a village house (9 x 9, pink walls, the stepped cheese roof) whose roof isn't finished: the three lower
steps are on, but the top (y 8) is a dakraam (3 x 3 pink glass) with sixteen see-through beroepen_dakplek ghost tiles
round it: the player lays the dakpannen there (Bob's quest). Round it: scaffolding with a plank walkway, a ladder up to
the roof, a little yellow bouwkraan with a pallet of dakpannen on its hook, a betonmolen, a kruiwagen, piles of planks and
bricks, a bouwbord, and inside a workbench with the bouwtekening. Bob (BOUWVAKKERGUH) stands by the door, facing the path.

The plot's orientation follows its door: (a, b) are local offsets from the house's middle, b towards the door (the path),
a to the door's right when you look out of it.
"""
import json

from make_structures import mc, Byte, floats, NbtList, STEP

DAKPLEK = "guhs:beroepen_dakplek"
DAKPAN = "guhs:beroepen_dakpan"
YAW = {"south": 0.0, "west": 90.0, "north": 180.0, "east": 270.0}
TURN = {"north": "east", "east": "south", "south": "west", "west": "north"}      # clockwise
BACK = {"north": "south", "south": "north", "east": "west", "west": "east"}


def frame(x0, z0, door):
    fx, fz = STEP[door]
    right = TURN[door]
    rx, rz = STEP[right]
    cx, cz = x0 + 4, z0 + 4

    def at(a, b):
        return cx + a * rx + b * fx, cz + a * rz + b * fz
    return at, right


def bouwplaats(s, x0, z0, door, roof, rng):
    at, right = frame(x0, z0, door)
    back = BACK[door]
    left = BACK[right]
    path = {mc("yellow_concrete"), mc("white_concrete"), mc("pink_concrete")}

    def free(x, z):
        return s.get(x, 0, z) not in path and s.get(x, 1, z) is None

    # the floor, the walls (a window on each side, the door towards the path)
    for a in range(-4, 5):
        for b in range(-4, 5):
            x, z = at(a, b)
            s.set(x, 0, z, mc("birch_planks"))
            edge = abs(a) == 4 or abs(b) == 4
            corner = abs(a) == 4 and abs(b) == 4
            for y in range(1, 5):
                if corner:
                    s.set(x, y, z, mc("white_terracotta"))
                elif edge:
                    window = y in (2, 3) and ((abs(a) == 4 and abs(b) == 1) or (abs(b) == 4 and abs(a) == 1 and b < 0))
                    s.set(x, y, z, mc("pink_stained_glass") if window else mc("pink_concrete"))
                else:
                    s.set(x, y, z, mc("air"))
    dx, dz = at(0, 4)
    for y, half in ((1, "lower"), (2, "upper")):
        s.set(dx, y, dz, mc("oak_door"), {"facing": BACK[door], "half": half, "hinge": "left", "open": "false", "powered": "false"})
    px, pz = at(0, 5)
    s.set(px, 0, pz, mc("yellow_concrete"))
    # the roof: three steps done, the top (level 3) is the dakraam with the ghost tiles round it
    for level in range(4):
        n = 5 - level
        for a in range(-n, n + 1):
            for b in range(-n, n + 1):
                ring = max(abs(a), abs(b)) == n
                x, z = at(a, b)
                if level < 3 and ring:
                    hole = (x * 7 + z * 3 + level) % 11 == 0
                    s.set(x, 5 + level, z, mc("orange_terracotta") if hole else mc(roof))
                elif level == 3:
                    s.set(x, 8, z, DAKPLEK if ring else mc("pink_stained_glass"))
    # inside: a workbench with the bouwtekening, sawhorses, a toolbox, a lantern
    for (a, b, block, props) in ((-2, -3, "crafting_table", None), (-1, -3, "cartography_table", None),
                                 (2, -3, "barrel", {"facing": "up", "open": "false"}), (3, -2, "smithing_table", None)):
        x, z = at(a, b)
        s.set(x, 1, z, mc(block), props)
    for (a, b) in ((2, 1), (3, 1)):
        x, z = at(a, b)
        s.set(x, 1, z, mc("spruce_fence"), {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
    x, z = at(2, 1)
    s.set(x, 2, z, mc("spruce_slab"), {"type": "bottom", "waterlogged": "false"})
    x, z = at(3, 1)
    s.set(x, 2, z, mc("spruce_slab"), {"type": "bottom", "waterlogged": "false"})
    x, z = at(0, 0)
    s.set(x, 4, z, mc("lantern"), {"hanging": "true", "waterlogged": "false"})
    # scaffolding along the back wall (two blocks out), a plank walkway on top, a ladder up to the roof on the left side
    for a in range(-5, 6):
        x, z = at(a, -6)
        if free(x, z):
            for y in range(1, 5):
                s.set(x, y, z, mc("scaffolding"), {"bottom": "false", "distance": "0", "waterlogged": "false"})
            s.set(x, 5, z, mc("spruce_slab"), {"type": "bottom", "waterlogged": "false"})
    for y in range(1, 7):
        x, z = at(-7, 0)
        s.set(x, y, z, mc("spruce_planks"))
        x, z = at(-6, 0)
        s.set(x, y, z, mc("ladder"), {"facing": right, "waterlogged": "false"})
    # the little crane at the back right corner: a yellow mast, a jib over the roof, a pallet of dakpannen on its hook
    mx, mz = at(6, -7)
    for y in range(1, 14):
        s.set(mx, y, mz, mc("yellow_concrete") if y % 3 else mc("black_concrete"))
    for b in range(-7, 2):                                                  # the jib: over the side, then over the roof
        x, z = at(6, b)
        s.set(x, 14, z, mc("yellow_concrete"))
    for a in range(-2, 6):
        x, z = at(a, 1)
        s.set(x, 14, z, mc("yellow_concrete"))
    hx, hz = at(-2, 1)
    for y in (12, 13):
        s.set(hx, y, hz, mc("chain"), {"axis": "y", "waterlogged": "false"})
    s.set(hx, 11, hz, DAKPAN)
    x, z = at(6, -8)
    s.set(x, 14, z, mc("gray_concrete"))                                   # the counterweight
    s.set(mx, 15, mz, mc("lantern"), {"hanging": "false", "waterlogged": "false"})
    # the betonmolen, the kruiwagen, planks and bricks, the bouwbord (on the right side)
    for (a, b, block, props) in ((6, 2, "cauldron", None), (6, 3, "stripped_spruce_log", {"axis": "y"}),
                                 (6, 0, "spruce_planks", None), (6, -1, "spruce_planks", None), (7, 0, "spruce_slab", {"type": "bottom", "waterlogged": "false"}),
                                 (6, -3, "bricks", None), (7, -3, "brick_slab", {"type": "bottom", "waterlogged": "false"}),
                                 (6, 5, "composter", {"level": "0"})):
        x, z = at(a, b)
        if free(x, z):
            s.set(x, 1, z, mc(block), props)
    x, z = at(6, 0)
    if s.get(x, 1, z) == mc("spruce_planks"):
        s.set(x, 2, z, mc("spruce_planks"))
    x, z = at(6, 2)
    if s.get(x, 1, z) == mc("cauldron"):
        s.set(x, 2, z, mc("barrel"), {"facing": left, "open": "true"})                   # the drum of the mixer
    x, z = at(5, 5)
    if free(x, z):
        blank = json.dumps("")
        msgs = [json.dumps({"translate": f"sign.guhs.beroepen_{k}"}) for k in ("bouwbord1", "bouwbord2", "bouwbord3")] + [blank]
        text = {"messages": NbtList(8, msgs), "color": "black", "has_glowing_text": Byte(0)}
        empty = {"messages": NbtList(8, [blank] * 4), "color": "black", "has_glowing_text": Byte(0)}
        s.set(x, 1, z, mc("oak_sign"), {"rotation": {"south": "0", "west": "4", "north": "8", "east": "12"}[door], "waterlogged": "false"},
              {"id": "minecraft:sign", "is_waxed": Byte(1), "front_text": text, "back_text": empty})
    # Bob, by the door, looking at the path
    bx, bz = at(1, 5)
    s.entity(bx + 0.5, 1.0, bz + 0.5, {"id": "guhs:guh_npc", "Kind": "bouwvakkerguh", "PersistenceRequired": Byte(1),
                                       "Rotation": floats(YAW[door], 0.0)})


def dakplekken(s):
    return [p for p, (blk, _, _) in s.blocks.items() if blk == DAKPLEK]


def check(s):
    """The bouwplaats in a saved village: 16 ghost tiles round a glass dakraam at y 8, Bob on the ground, a ladder that
    reaches the roof steps."""
    problems = []
    plekken = dakplekken(s)
    if len(plekken) != 16 or any(y != 8 for (_, y, _) in plekken):
        problems.append(f"bouwplaats: {len(plekken)} dakplekken {sorted(plekken)[:4]}")
    bobs = [e for e in s.entities if e[3].get("Kind") == "bouwvakkerguh"]
    if len(bobs) != 1:
        problems.append(f"bouwplaats: {len(bobs)} Bobs")
    else:
        x, y, z = bobs[0][:3]
        below = s.get(int(x), 0, int(z))
        if y != 1.0 or below is None or s.get(int(x), 1, int(z)) not in (None, "minecraft:air"):
            problems.append(f"bouwplaats: Bob doesn't stand on the ground ({below})")
    ladders = [p for p, (blk, _, _) in s.blocks.items() if blk == "minecraft:ladder"]
    if not ladders or max(y for (_, y, _) in ladders) < 6:
        problems.append("bouwplaats: no ladder up to the roof")
    return problems
