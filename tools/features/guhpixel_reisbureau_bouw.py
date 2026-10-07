"""
Reisbureau "De Vadsvakantie": the building in the Guhmensie (template data/guhs/structure/reisbureau.nbt, 17 x 16 x 17).

A little white-and-pink travel agency with a striped awning and a giant suitcase on its roof. Inside: the counter (three
Reisbalie blocks with vast=true: they cannot be broken, and open the same screen as a home-made Reisbalie), the
Reisagent-guh behind it, posters, suitcases and a waiting bench. Outside: a signpost to far-away places and a beach chair
under a parasol. The front (the door) looks north.
"""
import sign_text

NAME = "reisbureau"
G = 4                      # the ground row of the template (below it: the foundation, sunk into the land)
W, H = 17, 16
MIDDEN = (8, 8)            # the anchor jigsaw (in the floor, in front of the counter)
ANCHOR = f"guhs:{NAME}_midden"
BALIES = ((7, G + 1, 9), (8, G + 1, 9), (9, G + 1, 9))
AGENT = (8, G + 1, 11)
SIGN = f"sign.guhs.{NAME}"


def mc(n):
    return "minecraft:" + n


def _bord(h, s, x, y, z, facing, regels, hout="birch", kleur="black"):
    ms = h.ms
    tekst = {"messages": sign_text.messages(SIGN, regels), "color": kleur, "has_glowing_text": ms.Byte(0)}
    s.set(x, y, z, mc(f"{hout}_wall_sign"), {"facing": facing, "waterlogged": "false"},
          {"id": "minecraft:sign", "is_waxed": ms.Byte(1), "front_text": tekst, "back_text": tekst})


def build(h):
    s = h.Structure((W, H, W))
    # ---- the plate: foundation, grass, a path to the door ---------------------------------------------------------------
    for x in range(W):
        for z in range(W):
            for y in range(G):
                s.set(x, y, z, mc("pink_wool"))
            pad = 7 <= x <= 9 and z <= 5
            s.set(x, G, z, "guhs:knuffelklinkers" if pad or (2 <= x <= 14 and z in (3, 4)) else "guhs:knuffelgras")
    s.clear_above([(x, z) for x in range(W) for z in range(W)], G + 1, top=H)
    # ---- the building: x 3..13, z 5..13 ---------------------------------------------------------------------------------
    x0, x1, z0, z1 = 3, 13, 5, 13
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            s.set(x, G, z, mc("pink_concrete") if (x + z) % 2 else mc("white_concrete"))
            for y in range(G + 1, G + 5):
                if rand:
                    hoek = x in (x0, x1) and z in (z0, z1)
                    s.set(x, y, z, mc("pink_terracotta") if hoek or y == G + 1 else mc("white_concrete"))
                else:
                    s.set(x, y, z, mc("air"))
            s.set(x, G + 5, z, mc("pink_terracotta") if rand else mc("white_concrete"))
    # the door (open, two high) and the windows
    for y in (G + 1, G + 2):
        s.set(8, y, z0, mc("air"))
    for y in (G + 2, G + 3):
        for x in (5, 6, 10, 11):
            s.set(x, y, z0, mc("pink_stained_glass"))
        for z in (8, 9, 10):
            s.set(x0, y, z, mc("pink_stained_glass"))
            s.set(x1, y, z, mc("pink_stained_glass"))
    # the awning over the front: pink and white stripes, two deep
    for x in range(4, 13):
        for z in (3, 4):
            s.set(x, G + 4, z, mc("pink_wool") if x % 2 == 0 else mc("white_wool"))
    for x in (4, 12):
        for y in range(G + 1, G + 4):
            s.set(x, y, 3, mc("birch_fence"), {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
    _bord(h, s, 8, G + 3, 4, "north", ["~ Reisbureau ~", "De", "Vadsvakantie", "~ njeg ~"])
    # ---- the giant suitcase on the roof ---------------------------------------------------------------------------------
    for x in range(6, 11):
        for y in range(G + 6, G + 9):
            for z in range(7, 12):
                s.set(x, y, z, mc("dark_oak_planks") if x in (7, 9) else mc("brown_terracotta"))
    for (x, y, z, blok) in ((6, G + 7, 6, None), (8, G + 7, 7, "pink_concrete"), (10, G + 6, 7, "yellow_concrete"), (6, G + 8, 7, "light_blue_concrete"),
                            (8, G + 6, 11, "lime_concrete"), (10, G + 8, 11, "pink_concrete")):
        if blok:
            s.set(x, y, z, mc(blok))
    fence = {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}
    s.set(7, G + 9, 9, mc("dark_oak_fence"), dict(fence))
    s.set(9, G + 9, 9, mc("dark_oak_fence"), dict(fence))
    for x in (7, 8, 9):
        s.set(x, G + 10, 9, mc("dark_oak_slab"), {"type": "bottom", "waterlogged": "false"})
    # ---- inside ---------------------------------------------------------------------------------------------------------
    for (x, y, z) in BALIES:
        s.set(x, y, z, f"guhs:{NAME}_balie", {"facing": "north", "vast": "true"})
    for x in (4, 5, 11, 12):
        s.set(x, G + 1, 9, mc("birch_slab"), {"type": "top", "waterlogged": "false"})
    s.set(6, G + 1, 9, mc("birch_planks"))
    s.set(10, G + 1, 9, mc("birch_planks"))
    for x in range(7, 10):
        for z in range(6, 9):
            s.set(x, G + 1, z, mc("pink_carpet"))
    for (x, z) in ((6, 7), (10, 7), (8, 11)):
        s.set(x, G + 4, z, mc("lantern"), {"hanging": "true", "waterlogged": "false"})
    s.set(4, G + 1, 6, f"guhs:{NAME}_koffertje", {"facing": "east"})
    s.set(4, G + 1, 7, mc("barrel"), {"facing": "up", "open": "false"})
    s.set(4, G + 2, 7, f"guhs:{NAME}_koffertje", {"facing": "north"})
    s.set(12, G + 1, 6, mc("potted_pink_tulip"))
    s.set(12, G + 1, 7, mc("birch_stairs"), {"facing": "east", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    s.set(12, G + 1, 8, mc("birch_stairs"), {"facing": "east", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    s.set(4, G + 1, 12, mc("bookshelf"))
    s.set(5, G + 1, 12, mc("bookshelf"))
    s.set(4, G + 2, 12, mc("potted_azalea_bush"))
    s.set(12, G + 1, 12, f"guhs:{NAME}_koffertje", {"facing": "west"})
    s.set(11, G + 1, 12, mc("chest"), {"facing": "north", "type": "single", "waterlogged": "false"})
    # the posters on the back wall
    _bord(h, s, 6, G + 3, 12, "north", ["Vandaag:", "4 reizen!", "Morgen:", "4 andere!"])
    _bord(h, s, 8, G + 3, 12, "north", ["Eén guh", "tegelijk.", "Hij komt", "altijd terug!"])
    _bord(h, s, 10, G + 3, 12, "north", ["Tip:", "Balkonië.", "Lekker", "dichtbij!"])
    # the Reisagent-guh behind the counter, looking at the door
    s.entity(AGENT[0] + 0.5, float(AGENT[1]), AGENT[2] + 0.5, {"id": "guhs:guh_npc", "Kind": f"{NAME}_agent", "PersistenceRequired": h.ms.Byte(1),
                                                              "Rotation": h.ms.floats(180.0, 0.0)})
    # ---- outside: the signpost and the beach chair ----------------------------------------------------------------------
    for y in range(G + 1, G + 4):
        s.set(4, y, 1, mc("stripped_birch_log"), {"axis": "y"})
    _bord(h, s, 4, G + 3, 0, "north", ["Guhwai'i", "2 uur", "die kant op", ""])
    _bord(h, s, 3, G + 2, 1, "west", ["Kaasmaan", "24 uur", "omhoog", ""])
    _bord(h, s, 5, G + 2, 1, "east", ["Lingsesdijk 86", "1 uur", "vlakbij", ""])
    s.set(13, G + 1, 1, mc("birch_stairs"), {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    s.set(13, G + 1, 2, mc("birch_slab"), {"type": "bottom", "waterlogged": "false"})
    for y in range(G + 1, G + 4):
        s.set(14, y, 1, mc("birch_fence"), dict(fence))
    for x in range(13, 16):
        for z in range(0, 3):
            s.set(x, G + 4, z, mc("yellow_wool") if (x + z) % 2 == 0 else mc("white_wool"))
    for (x, z) in ((1, 6), (1, 12), (15, 6), (15, 12)):
        s.set(x, G + 1, z, mc("flowering_azalea_leaves"), {"persistent": "true", "distance": "7", "waterlogged": "false"})
    # the anchor (in the floor in front of the counter)
    s.set(MIDDEN[0], G, MIDDEN[1], mc("jigsaw"), {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": "minecraft:pink_concrete",
           "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    check(s)
    return s


def check(s):
    """The things the game relies on: the three fixed counters, exactly one agent behind them, the open door, the anchor."""
    problems = []
    for (x, y, z) in BALIES:
        blok = s.blocks.get((x, y, z))
        if not blok or blok[0] != f"guhs:{NAME}_balie" or blok[1].get("vast") != "true":
            problems.append(f"no fixed Reisbalie at {(x, y, z)}")
        if s.get(x, y, z - 1) not in (mc("pink_carpet"), mc("air"), None):
            problems.append(f"the front of the counter at {(x, y, z)} is not free")
    agents = [e for e in s.entities if e[3].get("Kind") == f"{NAME}_agent"]
    if len(agents) != 1:
        problems.append(f"{len(agents)} agents")
    for y in (G + 1, G + 2):
        if s.get(8, y, 5) != mc("air"):
            problems.append("the door is closed")
    if s.get(MIDDEN[0], G, MIDDEN[1]) != mc("jigsaw"):
        problems.append("no anchor jigsaw")
    if problems:
        raise SystemExit("reisbureau template check failed:\n  " + "\n  ".join(problems))
