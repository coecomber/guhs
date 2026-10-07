"""
The Guh-internetcafé "De Trage Verbinding": worldgen structure guhs:internetcafe in the Guhmensie (Java: the Beheerder-guh
and the sleepers in feature/guhpixel/lobby; the portal block itself is the kern's guhs:guhpixel_portaal[soort=in]).

A beige box of a building (21 x 16 x 19 with its plate) with an antenna and a satellite dish on the roof. Inside: two rows
of desks with old beige computers (guhs:internetcafe_computer) and guhs asleep behind them, the helpdesk of the Beheerder-guh
("Heb je hem al uit en weer aan gezet, njeg?"), a broken printer, a dusty server rack, and against the back wall the portal:
a giant CRT monitor whose screen you walk through. Walking through it the first time unlocks Guhpixel (PortaalBlock).

The plate is sunk G blocks into the land (the jigsaw in the middle of the floor is the start; make_v2.grond() lets the land
meet the floor). Everything is deterministic; texts on signs go through tools/sign_text.py (prefix sign.guhs.internetcafe).
"""
NAME = "internetcafe"
G = 4                            # the foundation under the floor
W, H, D = 21, 16, 19
MUUR = "minecraft:smooth_sandstone"
BAND = "minecraft:cut_sandstone"
VLOER_A, VLOER_B = "minecraft:white_terracotta", "minecraft:light_gray_terracotta"
ANKER = "guhs:internetcafe_midden"

X0, X1, Z0, Z1 = 2, 18, 2, 16    # the building's outer walls
DAK = G + 8                      # the roof (the room is 7 high)
# the screen of the giant CRT: guhs:guhpixel_portaal[soort=in], 5 wide and 4 high, you walk into it from the south
SCHERM = [(x, y, 4) for x in range(8, 13) for y in range(G + 1, G + 5)]
# desks: (x of the desk, z, the side the guh sits on: +1 = east of the desk, -1 = west)
BUREAUS = [(3, z, 1) for z in (6, 8, 10, 12, 14)] + [(17, z, -1) for z in (8, 10, 12, 14)]
SLAPERS = [0, 1, 3, 4, 5, 7, 8]  # which desks have a guh asleep behind them (the others are free: "gereserveerd voor dutjes")
BEHEERDER = (15.5, G + 1, 4.5, 0.0)


def bord_nbt(h, regels, kleur="black", gloei=False):
    import sign_text
    B = h.ms.Byte
    leeg = sign_text.messages("sign.guhs.internetcafe", ["", "", "", ""])
    return {"id": "minecraft:sign", "is_waxed": B(1),
            "front_text": {"messages": sign_text.messages("sign.guhs.internetcafe", regels), "color": kleur, "has_glowing_text": B(1 if gloei else 0)},
            "back_text": {"messages": leeg, "color": kleur, "has_glowing_text": B(0)}}


def npc(h, s, x, y, z, kind, yaw, slaapt=False):
    ms = h.ms
    nbt = {"id": "guhs:guh_npc", "Kind": kind, "PersistenceRequired": ms.Byte(1), "Rotation": ms.floats(yaw, 0.0)}
    if slaapt:
        nbt["NoAI"] = ms.Byte(1)          # (a sleeper keeps facing its screen)
    s.entity(x, float(y), z, nbt)


def maak(h):
    s = h.Structure((W, H, D))
    # the plate: a foundation of pink wool, grass around the building and a path to the door
    for x in range(W):
        for z in range(D):
            for y in range(G):
                s.set(x, y, z, "minecraft:pink_wool")
            pad = 9 <= x <= 11 and z > Z1
            s.set(x, G, z, "guhs:knuffelklinkers" if pad else "guhs:knuffelgras")
    # floor, walls, roof
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            s.set(x, G, z, VLOER_A if (x + z) % 2 else VLOER_B)
            rand = x in (X0, X1) or z in (Z0, Z1)
            for y in range(G + 1, DAK):
                if rand:
                    s.set(x, y, z, BAND if y in (G + 1, DAK - 1) else MUUR)
                else:
                    s.set(x, y, z, "minecraft:air")
            s.set(x, DAK, z, MUUR)
    for x in range(X0 - 1, X1 + 2):                            # a little overhang
        for z in (Z0 - 1, Z1 + 1):
            s.set(x, DAK, z, "minecraft:smooth_sandstone_slab", {"type": "bottom", "waterlogged": "false"})
    for z in range(Z0, Z1 + 1):
        for x in (X0 - 1, X1 + 1):
            s.set(x, DAK, z, "minecraft:smooth_sandstone_slab", {"type": "bottom", "waterlogged": "false"})
    # the door (south) and windows with brown "blinds"
    for x in (9, 10, 11):
        for y in (G + 1, G + 2, G + 3):
            s.set(x, y, Z1, "minecraft:air")
    for z in (5, 9, 13):
        for x in (X0, X1):
            for y in (G + 3, G + 4):
                s.set(x, y, z, "minecraft:brown_stained_glass")
    for x in (5, 15):
        for y in (G + 2, G + 3, G + 4):
            s.set(x, y, Z1, "minecraft:brown_stained_glass")
    # lights in the ceiling
    for x in (6, 10, 14):
        for z in (6, 10, 14):
            s.set(x, DAK, z, "minecraft:sea_lantern")
    # --- the giant CRT against the back (north) wall: a beige frame, the screen is the portal ---------------------------
    for x in range(7, 14):
        for y in range(G + 1, G + 7):
            scherm = 8 <= x <= 12 and y <= G + 4
            s.set(x, y, 4, "minecraft:air" if scherm else MUUR)
            s.set(x, y, 3, "minecraft:black_concrete" if scherm else MUUR)      # the tube behind the screen
    for (x, y, z) in SCHERM:
        s.set(x, y, z, "guhs:guhpixel_portaal", {"soort": "in"})
    s.set(12, G + 5, 4, "minecraft:sea_lantern")                                 # the power light
    for x in (8, 9):
        s.set(x, G + 5, 4, "minecraft:birch_button", {"face": "wall", "facing": "south", "powered": "false"})   # (knobs: on the frame's front)
    # (buttons hang on the block behind them: put them one block forward)
    for x in (8, 9):
        s.set(x, G + 5, 4, MUUR)
        s.set(x, G + 5, 5, "minecraft:birch_button", {"face": "wall", "facing": "south", "powered": "false"})
    for x in range(8, 13):                                     # the back of the tube sticks out of the building
        for y in range(G + 1, G + 5):
            s.set(x, y, 1, MUUR)
    for x in range(9, 12):
        for y in range(G + 2, G + 4):
            s.set(x, y, 0, BAND)
    # --- desks with computers and sleepers ----------------------------------------------------------------------------
    for i, (x, z, kant) in enumerate(BUREAUS):
        s.set(x, G + 1, z, "minecraft:smooth_sandstone_slab", {"type": "top", "waterlogged": "false"})
        s.set(x, G + 2, z, "guhs:internetcafe_computer", {"facing": "east" if kant > 0 else "west"})
        if i in SLAPERS:
            npc(h, s, x + kant + 0.5, G + 1, z + 0.5, "internetcafe_slaper", 90.0 if kant > 0 else -90.0, slaapt=True)
        else:
            s.set(x + kant, G + 1, z, "guhs:pink_kussen", {"facing": "west" if kant > 0 else "east"})
    # --- the helpdesk of the Beheerder-guh (east of the CRT) ----------------------------------------------------------
    for x in (14, 15, 16):
        s.set(x, G + 1, 6, "minecraft:smooth_quartz_slab", {"type": "top", "waterlogged": "false"})
    s.set(14, G + 2, 6, "guhs:internetcafe_computer", {"facing": "north"})
    npc(h, s, BEHEERDER[0], BEHEERDER[1], BEHEERDER[2], "internetcafe_beheerder", BEHEERDER[3])
    s.set(16, G + 2, 6, "minecraft:birch_sign", {"rotation": "0", "waterlogged": "false"},
          bord_nbt(h, ["HELPDESK", "Uit en weer", "aan gezet?", "Nee? Doe dat."], gloei=True))
    # --- the rest: a server rack, a broken printer, cables --------------------------------------------------------------
    for y in (G + 1, G + 2, G + 3):
        s.set(3, y, 3, "minecraft:lodestone")
        s.set(4, y, 3, "minecraft:jukebox", {"has_record": "false"})
    s.set(3, G + 4, 3, "minecraft:cobweb")
    s.set(5, G + 1, 3, "minecraft:cobweb")
    s.set(17, G + 1, 3, "minecraft:loom", {"facing": "south"})
    s.set(17, G + 2, 3, "minecraft:birch_wall_sign", {"facing": "south", "waterlogged": "false"},
          bord_nbt(h, ["Printer stuk.", "Al sinds", "de opening.", "Njeg."]))
    for z in range(5, 16):                                     # a blue cable over the floor, from the door to the screen
        s.set(10, G + 1, z, "minecraft:light_blue_carpet")
    for x in (5, 6, 7, 8, 9):
        s.set(x, G + 1, 5 if x < 8 else 5, "minecraft:light_blue_carpet")
    s.set(5, G + 1, 4, "minecraft:light_blue_carpet")
    # --- signs ----------------------------------------------------------------------------------------------------------
    def muur(x, y, z, facing, regels, **kw):
        s.set(x, y, z, "minecraft:birch_wall_sign", {"facing": facing, "waterlogged": "false"}, bord_nbt(h, regels, **kw))

    muur(9, G + 5, Z1 + 1, "south", ["~ Guh- ~", "internetcafé", "", "sinds heel lang"], gloei=True)
    muur(10, G + 5, Z1 + 1, "south", ["De Trage", "Verbinding", "", "56k = snel zat"], gloei=True)
    muur(11, G + 5, Z1 + 1, "south", ["Open: altijd", "(iedereen", "slaapt toch", "al, njeg)"], gloei=True)
    muur(7, G + 2, 5, "south", ["GUHPIXEL", "Loop door het", "beeldscherm!", "(echt waar)"], gloei=True)
    muur(13, G + 2, 5, "south", ["Laadtijd:", "ongeveer", "3 dutjes"])
    muur(X0 + 1, G + 3, 7, "east", ["Wifi:", "njegnjeg123", "(werkt niet,", "wel leuk)"])
    muur(X1 - 1, G + 3, 11, "west", ["Niet aaien:", "deze guh is", "aan het", "bufferen"])
    muur(12, G + 3, Z1 - 1, "north", ["Vergeet je", "kabeltje niet", "(krijg je op", "Guhpixel)"])
    # --- the roof: an antenna and a satellite dish ----------------------------------------------------------------------
    for y in range(DAK + 1, DAK + 5):
        s.set(5, y, 5, "minecraft:iron_bars", {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})
    s.set(5, DAK + 5, 5, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    for (x, y, z) in ((14, DAK + 1, 12), (14, DAK + 2, 12)):
        s.set(x, y, z, "minecraft:iron_bars", {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})
    for (x, z) in ((13, 11), (14, 11), (15, 11), (13, 12), (15, 12), (13, 13), (14, 13), (15, 13)):
        s.set(x, DAK + 3, z, "minecraft:white_concrete")
    s.set(14, DAK + 3, 12, "minecraft:light_gray_concrete")
    s.set(14, DAK + 4, 12, "minecraft:end_rod", {"facing": "up"})
    # --- outside: two planters by the door, a bench ---------------------------------------------------------------------
    for x in (8, 12):
        s.set(x, G + 1, Z1 + 1, "guhs:seizoensbloembak", {"seizoen": "lente"})
    s.set(6, G + 1, Z1 + 1, "guhs:guh_bank", {"facing": "south"})
    # the start jigsaw in the middle of the floor
    s.set(10, G, 9, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": ANKER, "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": VLOER_A,
           "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=H)
    return s


def controleer(s):
    problems = []
    if s.size != (W, H, D):
        problems.append(f"size {s.size}")
    if s.get(10, G, 9) != "minecraft:jigsaw":
        problems.append("no start jigsaw in the middle of the floor")
    for (x, y, z) in SCHERM:
        b = s.blocks.get((x, y, z))
        if not b or b[0] != "guhs:guhpixel_portaal" or b[1].get("soort") != "in":
            problems.append(f"no portal at {(x, y, z)}")
    for x in range(8, 13):                                     # the way to the screen is free (from the door, 3 high)
        for z in range(5, Z1):
            if x in (9, 10, 11) and any(s.get(x, y, z) not in (None, "minecraft:air", "minecraft:light_blue_carpet") for y in (G + 1, G + 2, G + 3)):
                problems.append(f"the aisle is blocked at {(x, z)}")
    for y in (G + 1, G + 2, G + 3):
        if s.get(10, y, Z1) not in (None, "minecraft:air"):
            problems.append("the door is closed")
    kinds = [e[3]["Kind"] for e in s.entities]
    if kinds.count("internetcafe_beheerder") != 1 or kinds.count("internetcafe_slaper") != len(SLAPERS):
        problems.append(f"NPCs: {kinds}")
    for (x, y, z, nbt) in s.entities:
        if s.get(int(x), int(y) - 1, int(z)) in (None, "minecraft:air") or s.get(int(x), int(y), int(z)) not in (None, "minecraft:air"):
            problems.append(f"{nbt['Kind']} at {(x, y, z)} does not stand free on a floor")
    if sum(1 for b in s.blocks.values() if b[0] == "guhs:internetcafe_computer") != len(BUREAUS) + 1:
        problems.append("the computers are not all there")
    for (x, y, z), (b, _, _) in s.blocks.items():
        if b in ("minecraft:water", "minecraft:lava"):
            problems.append(f"{b} in the café")
    if problems:
        raise SystemExit("internetcafe check failed:\n  " + "\n  ".join(problems))


def build(h, spacing, separation, salt):
    s = maak(h)
    controleer(s)
    none = {"bounding_box": "piece", "spawns": []}
    h.TEMPLATE_SIZES[NAME] = max(W, D)
    h.structure(NAME, h.GUHMENSION_LAND, spacing=spacing, separation=separation, salt=salt, start_y=-G, reach=40, centre=ANKER,
                spawn_overrides={"monster": none, "creature": none})
    s.save(NAME)
    return s
