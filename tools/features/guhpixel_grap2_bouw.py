"""
The two arenas of the guhpixel slice "grap2" (Java: feature/guhpixel/grap2/Guhmon.java and Bzg.java):

  guhs:guhpixel/guhmon_gym    the gym of Gymleider Dutjes: a battle field with a knabbelbal in the middle, the challenger's
                              mat, a podium for the gym leader under a big moon emblem, cushion stands for the audience
  guhs:guhpixel/bzg_studio    the TV studio of Boer zoekt Guh (stage, sofa, cameras, the mailbox) with the farm set next to
                              it behind a decor wall (painted sky, a barn front, hay, a fake cow)

Both are floors in the void with walls around (nobody can walk off), no entities (the sessions spawn their own guhs), no
fluids and no falling blocks. GYM and STUDIO are the fixed spots in template coordinates (feet positions); the Java files
keep the same numbers and controleer() compares them, and checks that every spot has a floor and free space.
"""
import os

GYM_NAAM = "guhpixel/guhmon_gym"
GYM_MAAT = (27, 12, 39)
GYM = {
    "START": (13.5, 1, 35.5), "VAK": (13.5, 1, 29.5), "MIJN": (13.5, 1, 25.5), "TEGEN": (13.5, 1, 13.5),
    "LEIDER": (13.5, 2, 7.5), "TEGEN_WACHT": (15.5, 2, 7.5),
}
GYM_PUBLIEK = [(3.5, 2, 14.5), (3.5, 2, 19.5), (3.5, 2, 24.5), (23.5, 2, 14.5), (23.5, 2, 19.5), (23.5, 2, 24.5)]

STUDIO_NAAM = "guhpixel/bzg_studio"
STUDIO_MAAT = (41, 12, 27)
STUDIO = {"START": (10.5, 1, 22.5), "GUHVON": (10.5, 1, 8.5), "BOER": (30.5, 1, 8.5)}
STUDIO_BRIEVENBUS = (5, 1, 14)
STUDIO_BANK = [(13.5, 1, 5.5), (15.5, 1, 5.5), (17.5, 1, 5.5)]
STUDIO_HOOI = [(26.5, 1, 13.5), (30.5, 1, 15.5), (34.5, 1, 13.5)]
STUDIO_SAMEN = [(29.0, 1, 9.8), (30.5, 1, 10.3), (32.0, 1, 9.8)]

LUCHT = (None, "minecraft:air")
VALT = ("minecraft:sand", "minecraft:red_sand", "minecraft:gravel", "minecraft:water", "minecraft:lava")


def _bord(h, prefix, regels):
    import sign_text
    B = h.ms.Byte
    return {"id": "minecraft:sign", "is_waxed": B(1),
            "front_text": {"messages": sign_text.messages(prefix, regels), "color": "black", "has_glowing_text": B(0)},
            "back_text": {"messages": sign_text.messages(prefix, regels), "color": "black", "has_glowing_text": B(0)}}


def _staand(h, s, x, y, z, rotatie, prefix, regels, hout="birch"):
    """A standing sign; rotatie 0 = reads from the south, 4 = from the west, 8 = from the north, 12 = from the east."""
    s.set(x, y, z, f"minecraft:{hout}_sign", {"rotation": str(rotatie), "waterlogged": "false"}, _bord(h, prefix, regels))


def _muur(h, s, x, y, z, facing, prefix, regels, hout="birch"):
    s.set(x, y, z, f"minecraft:{hout}_wall_sign", {"facing": facing, "waterlogged": "false"}, _bord(h, prefix, regels))


# =====================================================================================================================
# the gym
# =====================================================================================================================
MAAN = [
    "......MMM...ZZZ",
    ".....MM.......Z",
    "....MM.......Z.",
    "....MM......ZZZ",
    "....MM....z....",
    ".....MM..M.....",
    "......MMMM.....",
]


def gym(h):
    W, H, D = GYM_MAAT
    s = h.Structure(GYM_MAAT)
    P = "sign.guhs.guhmon"
    # the floor: pink terracotta, a white field with pink lines and a cheese-yellow knabbelbal in the middle
    for x in range(W):
        for z in range(D):
            b = "minecraft:pink_terracotta"
            if 6 <= x <= 20 and 9 <= z <= 31:
                b = "minecraft:white_concrete"
                if x in (6, 20) or z in (9, 31) or z == 20:
                    b = "minecraft:pink_concrete"
                d = ((x - 13) ** 2 + (z - 20) ** 2) ** 0.5
                if d <= 3.3:
                    b = "minecraft:magenta_concrete" if d > 2.3 else "minecraft:yellow_concrete"
            elif 12 <= x <= 14 and z >= 32:
                b = "minecraft:magenta_concrete"          # the walk-in carpet
            s.set(x, 0, z, b)
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            s.set(13 + dx, 0, 29 + dz, "minecraft:lime_concrete")     # the challenger's mat
    s.set(13, 0, 25, "minecraft:magenta_wool")                        # where your guh stands
    s.set(13, 0, 13, "minecraft:purple_wool")                         # where the other guh stands
    for (x, z) in ((1, 1), (25, 1), (1, 37), (25, 37), (6, 20), (20, 20)):
        s.set(x, 0, z, "minecraft:sea_lantern")
    # the walls: nobody walks off; the "door" is pink glass
    for x in range(W):
        for z in range(D):
            rand = x in (0, W - 1) or z in (0, D - 1)
            if not rand:
                continue
            for y in range(1, 5):
                deur = z == D - 1 and 12 <= x <= 14 and y <= 3
                s.set(x, y, z, "minecraft:pink_stained_glass" if deur else "minecraft:magenta_concrete" if y < 4 else "minecraft:pink_concrete")
    for z in range(4, D - 2, 5):                                      # soft lights in the side walls
        for x in (0, W - 1):
            s.set(x, 3, z, "minecraft:pearlescent_froglight", {"axis": "y"})
    # the back wall with the moon emblem of the Dutjesgym
    for x in range(3, 24):
        for y in range(1, 11):
            s.set(x, y, 2, "minecraft:purple_concrete")
            s.set(x, y, 1, "minecraft:purple_concrete")
    for rij, regel in enumerate(MAAN):
        for kol, ch in enumerate(regel):
            if ch != ".":
                s.set(6 + kol, 9 - rij, 2, {"M": "minecraft:yellow_concrete", "Z": "minecraft:white_concrete", "z": "minecraft:white_concrete"}[ch])
    for x in (3, 23):
        for y in range(1, 11):
            s.set(x, y, 2, "minecraft:pink_concrete")
        s.set(x, 10, 2, "minecraft:pearlescent_froglight", {"axis": "y"})
    # the podium of the gym leader (LEIDER and TEGEN_WACHT stand on it)
    for x in range(10, 18):
        for z in range(5, 9):
            s.set(x, 1, z, "minecraft:purple_concrete" if (x + z) % 2 else "minecraft:magenta_concrete")
    for x in range(10, 18):
        s.set(x, 1, 9, "minecraft:quartz_stairs", {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    # the cushion stands of the (sleeping) audience
    kleuren = ["pink", "light_blue", "yellow", "lime", "white", "magenta"]
    for kant, x0 in enumerate((2, 22)):
        for z in range(12, 28):
            for x in range(x0, x0 + 3):
                s.set(x, 1, z, f"minecraft:{kleuren[(z // 5 + kant) % len(kleuren)]}_wool")
    _staand(h, s, 5, 1, 27, 12, P, ["Publiek", "niet wekken", "njeg"])
    _staand(h, s, 21, 1, 27, 4, P, ["Publiek", "niet wekken", "njeg"])
    # two gym statues with their plates at the entrance, and the sign at the mat
    for x in (9, 17):
        s.set(x, 1, 33, "minecraft:chiseled_quartz_block")
        s.set(x, 2, 33, "minecraft:quartz_pillar", {"axis": "y"})
        s.set(x, 3, 33, "minecraft:yellow_glazed_terracotta", {"facing": "south"})
    _muur(h, s, 9, 2, 34, "south", P, ["Gym van", "Dutjes", "", "Sst!"])
    _muur(h, s, 17, 2, 34, "south", P, ["Winnaars:", "iedereen die", "sliep", "njeg"])
    _staand(h, s, 16, 1, 30, 0, P, ["Uitdager", "ga hier staan"])
    _muur(h, s, 16, 2, D - 2, "north", P, ["Er wordt", "gevochten", "(zachtjes)"])
    s.save(GYM_NAAM)
    return s


# =====================================================================================================================
# the studio and the farm set
# =====================================================================================================================
HART = [
    ".XX...XX.",
    "XXXX.XXXX",
    "XXXXXXXXX",
    "XXXXXXXXX",
    ".XXXXXXX.",
    "..XXXXX..",
    "...XXX...",
    "....X....",
]
WOLK = ["..XXX....", ".XXXXXX..", "XXXXXXXXX"]


def studio(h):
    W, H, D = STUDIO_MAAT
    s = h.Structure(STUDIO_MAAT)
    P = "sign.guhs.bzg"
    MUUR_X = 20
    # floors: the studio is grey with a pink stage, the farm set is "grass" (green concrete) with real hay
    for x in range(W):
        for z in range(D):
            if x < MUUR_X:
                b = "minecraft:light_gray_concrete"
                if 4 <= x <= 18 and 3 <= z <= 11:
                    b = "minecraft:pink_concrete" if (x + z) % 2 else "minecraft:magenta_concrete"
            elif x == MUUR_X:
                b = "minecraft:smooth_stone"
            else:
                b = "minecraft:lime_concrete" if (x * 7 + z * 3) % 5 else "minecraft:green_concrete"
            s.set(x, 0, z, b)
    s.set(10, 0, 8, "minecraft:yellow_concrete")                       # Guhvon's star
    s.set(10, 0, 22, "minecraft:white_concrete")                       # where the guest walks in
    for (hx, _, hz) in STUDIO_HOOI:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                s.set(int(hx) + dx, 0, int(hz) + dz, "minecraft:hay_block", {"axis": "y"})
    for x in range(27, 35):
        for z in range(7, 12):
            s.set(x, 0, z, "minecraft:hay_block", {"axis": "y"})          # the big hay bed around the farmer
    # the outer walls
    for x in range(W):
        for z in range(D):
            if x in (0, W - 1) or z in (0, D - 1):
                for y in range(1, 6):
                    s.set(x, y, z, "minecraft:gray_concrete" if x < MUUR_X else "minecraft:spruce_planks")
    # the logo wall of the studio (north): magenta with a big heart, the title on signs under it
    for x in range(1, MUUR_X):
        for y in range(1, 11):
            s.set(x, y, 0, "minecraft:magenta_concrete")
    for rij, regel in enumerate(HART):
        for kol, ch in enumerate(regel):
            if ch == "X":
                s.set(6 + kol, 10 - rij, 0, "minecraft:pink_concrete" if rij else "minecraft:white_concrete")
    _muur(h, s, 9, 2, 1, "south", P, ["", "BOER ZOEKT", "GUH", ""], hout="crimson")
    _muur(h, s, 10, 2, 1, "south", P, ["", "BOER ZOEKT", "GUH", ""], hout="crimson")
    _muur(h, s, 11, 2, 1, "south", P, ["", "BOER ZOEKT", "GUH", ""], hout="crimson")
    # the sofa of the candidates (a back of pink wool behind the three spots) and their sign
    for x in range(12, 19):
        s.set(x, 1, 4, "minecraft:pink_wool")
        s.set(x, 2, 4, "minecraft:pink_wool")
    s.set(12, 1, 5, "minecraft:pink_wool")
    s.set(19, 1, 5, "minecraft:pink_wool")
    s.set(19, 1, 4, "minecraft:pink_wool")
    _staand(h, s, 11, 1, 5, 0, P, ["Kandidaten"])
    # two rows of seats for the studio audience (it is a quiet day), an "applause" lamp above them
    for x in range(2, 18):
        if x in (9, 10, 11):
            continue                                                      # the aisle the guest walks in through
        s.set(x, 1, 24, "minecraft:birch_stairs", {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"})
        s.set(x, 1, 25, "minecraft:birch_planks")
        s.set(x, 2, 25, "minecraft:birch_stairs", {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    # two cameras on tripods and a row of studio lights
    for x in (6, 14):
        s.set(x, 1, 18, "minecraft:spruce_fence", {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
        s.set(x, 2, 18, "minecraft:observer", {"facing": "north", "powered": "false"})
    for x in range(1, MUUR_X):
        s.set(x, 7, 13, "minecraft:smooth_stone_slab", {"type": "top", "waterlogged": "false"})
        if x % 4 == 2:
            s.set(x, 6, 13, "minecraft:pearlescent_froglight", {"axis": "y"})
    for y in range(1, 8):
        s.set(0, y, 13, "minecraft:gray_concrete")
    # "Stilte! Opname" with its red light, the mailbox with its sign
    s.set(1, 4, 20, "minecraft:redstone_block")
    _muur(h, s, 1, 3, 20, "east", P, ["Stilte!", "Opname"])
    s.set(*STUDIO_BRIEVENBUS, "guhs:bzg_brievenbus", {"facing": "east"})
    _staand(h, s, 5, 1, 15, 12, P, ["Brieven voor", "Boer Guhrrit"])
    # the decor wall between the studio and the farm set, with a doorway (z 12..14)
    for z in range(1, D - 1):
        for y in range(1, 8):
            if 12 <= z <= 14 and y <= 3:
                continue
            s.set(MUUR_X, y, z, "minecraft:stripped_birch_wood" if y < 7 else "minecraft:birch_planks", {"axis": "y"} if y < 7 else None)
    _muur(h, s, MUUR_X - 1, 2, 11, "west", P, ["Naar de", "boerderij", "(decor)"])
    # the painted sky of the farm set (north): hills, a sun, two clouds; a sign admits it
    for x in range(MUUR_X + 1, W - 1):
        for y in range(1, 10):
            heuvel = y <= 1 + (2 if (x // 3) % 2 else 1)
            s.set(x, y, 0, "minecraft:lime_terracotta" if heuvel else "minecraft:light_blue_terracotta")
    for (cx, cy) in ((24, 8), (33, 7)):
        for rij, regel in enumerate(WOLK):
            for kol, ch in enumerate(regel):
                if ch == "X":
                    s.set(cx + kol, cy - rij, 0, "minecraft:white_concrete")
    for dx in (0, 1):
        for dy in (0, 1):
            s.set(37 + dx, 8 + dy, 0, "minecraft:yellow_concrete")
    _muur(h, s, 38, 2, 1, "south", P, ["Lucht", "(geverfd)"])
    # the barn front (east): red with white beams and a door that is only paint
    for z in range(4, 22):
        for y in range(1, 8):
            deur = 10 <= z <= 15 and y <= 5
            wit = z in (4, 21) or y == 7 or (deur and (z in (10, 15) or y == 5 or z - 10 == y or 15 - z == y))
            s.set(W - 1, y, z, "minecraft:white_concrete" if wit else "minecraft:red_terracotta")
    for z in range(6, 20):
        s.set(W - 1, 8, z, "minecraft:spruce_planks")
    # farm things: loose bales, a barrel, a fake cow with a warning
    for (x, y, z) in ((23, 1, 3), (24, 1, 3), (23, 2, 3), (37, 1, 4), (38, 1, 4), (38, 1, 5), (22, 1, 22), (23, 1, 22)):
        s.set(x, y, z, "minecraft:hay_block", {"axis": "y"})
    s.set(25, 1, 3, "minecraft:barrel", {"facing": "up", "open": "false"})
    s.set(36, 1, 4, "minecraft:composter", {"level": "3"})
    for (x, y, z, b) in ((36, 1, 21, "white_wool"), (37, 1, 21, "black_wool"), (38, 1, 21, "white_wool"), (36, 2, 21, "black_wool"),
                         (37, 2, 21, "white_wool"), (38, 2, 21, "white_wool"), (39, 2, 21, "pink_wool")):
        s.set(x, y, z, f"minecraft:{b}")
    _staand(h, s, 37, 1, 20, 8, P, ["Nep-koe", "niet melken", "njeg"], hout="spruce")
    _staand(h, s, 27, 1, 17, 4, P, ["Echt hooi"], hout="spruce")
    s.save(STUDIO_NAAM)
    return s


# =====================================================================================================================
# checks
# =====================================================================================================================
def _plek(s, naam, plek, problems):
    x, y, z = int(plek[0]), int(plek[1]), int(plek[2])
    if s.get(x, y - 1, z) in LUCHT:
        problems.append(f"{naam}: no floor under {plek}")
    for dy in (0, 1):
        if s.get(x, y + dy, z) not in LUCHT:
            problems.append(f"{naam}: {s.get(x, y + dy, z)} in the way at {(x, y + dy, z)}")


def _vec(v):
    return ", ".join(str(int(c)) if float(c).is_integer() else str(c) for c in v)


def controleer(gym_s, studio_s):
    problems = []
    for naam, s, maat in (("gym", gym_s, GYM_MAAT), ("studio", studio_s, STUDIO_MAAT)):
        if tuple(s.size) != maat or max(maat) > 160:
            problems.append(f"{naam}: size {s.size}")
        for (x, y, z), (b, _, _) in s.blocks.items():
            if b in VALT or b.endswith("concrete_powder"):
                problems.append(f"{naam}: {b} at {(x, y, z)} (falls or flows)")
        if s.entities:
            problems.append(f"{naam}: the template must not hold entities")
        # a closed rim: nobody walks off the edge
        for x in range(maat[0]):
            for z in range(maat[2]):
                if (x in (0, maat[0] - 1) or z in (0, maat[2] - 1)) and (s.get(x, 1, z) in LUCHT or s.get(x, 2, z) in LUCHT):
                    problems.append(f"{naam}: a gap in the wall at {(x, z)}")
    for naam, plek in list(GYM.items()) + [(f"PUBLIEK{i}", p) for i, p in enumerate(GYM_PUBLIEK)]:
        _plek(gym_s, f"gym {naam}", plek, problems)
    for naam, plek in (list(STUDIO.items()) + [(f"BANK{i}", p) for i, p in enumerate(STUDIO_BANK)]
                       + [(f"HOOI{i}", p) for i, p in enumerate(STUDIO_HOOI)] + [(f"SAMEN{i}", p) for i, p in enumerate(STUDIO_SAMEN)]):
        _plek(studio_s, f"studio {naam}", plek, problems)
    bus = studio_s.blocks.get(STUDIO_BRIEVENBUS)
    if not bus or bus[0] != "guhs:bzg_brievenbus":
        problems.append("studio: no mailbox at STUDIO_BRIEVENBUS")
    if sum(1 for b in studio_s.blocks.values() if b[0] == "guhs:bzg_brievenbus") != 1:
        problems.append("studio: exactly one mailbox, please")
    # the doorway between the studio and the farm set is open, and the player can walk from START to the farmer
    for z in (12, 13, 14):
        for y in (1, 2):
            if studio_s.get(20, y, z) not in LUCHT:
                problems.append(f"studio: the doorway is blocked at {(20, y, z)}")
    # the same numbers as the Java side
    basis = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhpixel", "grap2")
    for bestand, verwacht in (
            ("Guhmon.java", [f"MAAT = new Vec3i({_vec(GYM_MAAT)})"] + [f"{k} = new Vec3({_vec(v)})" for k, v in GYM.items()]
             + [f"new Vec3({_vec(p)})" for p in GYM_PUBLIEK]),
            ("Bzg.java", [f"MAAT = new Vec3i({_vec(STUDIO_MAAT)})", f"BRIEVENBUS = new BlockPos({_vec(STUDIO_BRIEVENBUS)})"]
             + [f"{k} = new Vec3({_vec(v)})" for k, v in STUDIO.items()]
             + [f"new Vec3({_vec(p)})" for p in STUDIO_BANK + STUDIO_HOOI + STUDIO_SAMEN])):
        pad = os.path.join(basis, bestand)
        if not os.path.exists(pad):
            continue
        src = open(pad, encoding="utf-8").read()
        for v in verwacht:
            if v not in src:
                problems.append(f"{bestand} does not have '{v}' (keep it the same as guhpixel_grap2_bouw.py)")
    if problems:
        raise SystemExit("guhpixel grap2 arena check failed:\n  " + "\n  ".join(problems[:40]))


def build(h):
    g = gym(h)
    st = studio(h)
    controleer(g, st)
    return g, st
