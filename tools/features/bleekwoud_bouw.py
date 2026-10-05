"""
Het Bleekwoud (1.2.8) - its two small structures and the game test rooms.

  bleke_open_plek   (35 x 27 x 35) De Bleke Open Plek: a round clearing of bleekmos full of oogbloempjes, ringed by eight
                    big bleekhout trees, with one extra-large tree in the middle that holds a Krakend Guhhartje high in
                    its trunk (on the south side, so you see it glow at night), and a half-buried chest.
  houthakkershutje  (17 x 14 x 17) Het Houthakkershutje: the abandoned cabin of the Houthakkerguh, of bleekhout: a bed, a
                    chest, a crafting table, his diary on a lectern, a note on the door; outside a woodpile, a tree he
                    was half done chopping, a felled trunk and a stump with his axe still in it.

Both are sunk G deep (their ground is the top of the plate at y = G; make_v2.grond() makes the land meet it), anchored in
the middle, and round: the corners of the template stay empty so the forest floor runs on.
check(): the chest/lectern/heart stand where the game tests look for them, the heart has a log above and below, nothing
of the furniture floats, the door can be walked to.
"""
import json
import math
import random

G = 4
LOG, STRIPPED, PLANK, LEAVES = "guhs:bleekhout_stam", "guhs:bleekhout_gestript", "guhs:bleekhout_planken", "guhs:bleekhout_bladeren"
MOSS, CARPET, HANG, FLOWER = "guhs:bleekmos", "guhs:bleekmos_tapijt", "guhs:bleek_hangmos", "guhs:oogbloempje"
HEART = "guhs:krakend_guhhartje"
AIR = "minecraft:air"

PLEK, PLEK_W, PLEK_H, PLEK_C = "bleke_open_plek", 35, 27, 17
PLEK_ANCHOR = "guhs:bleke_open_plek_midden"
PLEK_HART = (PLEK_C, G + 10, PLEK_C + 1)
PLEK_KIST = (PLEK_C + 5, G, PLEK_C - 4)

HUT, HUT_W, HUT_H, HUT_C = "houthakkershutje", 17, 14, 8
HUT_ANCHOR = "guhs:houthakkershutje_midden"
HUT_KIST = (9, G + 1, 4)
HUT_LESSENAAR = (9, G + 1, 6)
HUT_BED = (5, G + 1, 4)
HUT_BORD = (8, G + 2, 9)
HUT_DEUR = (7, G + 1, 8)

DAGBOEK_TITEL = "book.guhs.bleekwoud.dagboek.title"
DAGBOEK_PAGINAS = 7


def log(axis="y"):
    return {"axis": axis}


def leaves():
    return {"distance": "7", "persistent": "true", "waterlogged": "false"}


def carpet():
    return {"bottom": "true", "north": "none", "east": "none", "south": "none", "west": "none"}


def plate(s, c, radius):
    """A round plate: pink wool with bleekmos on top. Returns its columns."""
    fp = []
    for x in range(s.size[0]):
        for z in range(s.size[2]):
            if math.dist((x, z), (c, c)) <= radius:
                for y in range(G):
                    s.set(x, y, z, "minecraft:pink_wool")
                s.set(x, G, z, MOSS)
                fp.append((x, z))
    return fp


def canopy(s, rng, cx, cz, top, r, hang=0.2):
    """A dark-oak-like crown: a wide flat blob of leaves around the top of the trunk, with moss hanging under it."""
    for dy, rr in ((-2, r - 1.5), (-1, r), (0, r), (1, r - 1.2), (2, r - 2.6)):
        if rr <= 0:
            continue
        for x in range(int(cx - rr - 1), int(cx + rr + 2)):
            for z in range(int(cz - rr - 1), int(cz + rr + 2)):
                d = math.dist((x, z), (cx, cz))
                if d <= rr + (rng.random() - 0.5) * 0.9 and s.get(x, top + dy, z) is None and s.inside(x, top + dy, z):
                    s.set(x, top + dy, z, LEAVES, leaves())
    for x in range(int(cx - r - 1), int(cx + r + 2)):
        for z in range(int(cz - r - 1), int(cz + r + 2)):
            for y in range(top - 2, top + 1):
                if s.get(x, y, z) == LEAVES and s.get(x, y - 1, z) is None and rng.random() < hang:
                    n = 1 + (rng.random() < 0.4) + (rng.random() < 0.25)
                    placed = []
                    for k in range(1, n + 1):
                        if s.get(x, y - k, z) is not None or y - k <= G + 2:
                            break
                        placed.append(y - k)
                    for i, yy in enumerate(placed):
                        s.set(x, yy, z, HANG, {"tip": "true" if i == len(placed) - 1 else "false"})


def tree(s, rng, x, z, height, r=4.2):
    """A big bleekhout tree: a 2x2 trunk (x..x+1, z..z+1) and its crown."""
    for dx in (0, 1):
        for dz in (0, 1):
            for y in range(G + 1, G + 1 + height):
                s.set(x + dx, y, z + dz, LOG, log())
    canopy(s, rng, x + 0.5, z + 0.5, G + height, r)


def jigsaw(s, x, y, z, name, final):
    s.set(x, y, z, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": name, "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": final,
           "joint": "rollable", "placement_priority": 0, "selection_priority": 0})


# =====================================================================================================================
# De Bleke Open Plek
# =====================================================================================================================
def open_plek(h):
    s = h.Structure((PLEK_W, PLEK_H, PLEK_W))
    rng = random.Random(128001)
    C = PLEK_C
    fp = plate(s, C, 17.4)
    # the eight trees of the ring
    for i in range(8):
        a = math.tau * (i + 0.5) / 8
        tx, tz = int(round(C + math.cos(a) * 13 - 0.5)), int(round(C + math.sin(a) * 13 - 0.5))
        tree(s, rng, tx, tz, 7 + rng.randrange(3))
    # the old tree in the middle: a 3x3 trunk with root flares and a huge crown; the heart sits high in its south side
    top = G + 16
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            for y in range(G + 1, top + 1):
                s.set(C + dx, y, C + dz, LOG, log())
    for (dx, dz, hh) in ((-2, 0, 3), (2, 0, 2), (0, -2, 3), (0, 2, 2), (-2, -1, 1), (2, 1, 1), (1, -2, 1), (-1, 2, 1)):
        for y in range(G + 1, G + 1 + hh):
            s.set(C + dx, y, C + dz, LOG, log())
    for (dx, dz, axis) in ((-2, 0, "x"), (-3, 0, "x"), (2, 0, "x"), (3, 0, "x"), (0, -2, "z"), (0, -3, "z"), (0, 2, "z"), (0, 3, "z")):
        s.set(C + dx, top - 3, C + dz, LOG, log(axis))              # branches under the crown
    canopy(s, rng, C, C, top, 7.2, hang=0.3)
    s.set(*PLEK_HART, HEART, {"axis": "y", "creaking_heart_state": "dormant", "natural": "true"}, {"id": "guhs:krakend_guhhartje"})
    # oogbloempjes all over, a few tufts of carpet
    spots = [(x, z) for (x, z) in fp if 4 <= math.dist((x, z), (C, C)) <= 10.5 and s.get(x, G + 1, z) is None]
    rng.shuffle(spots)
    for (x, z) in spots[:44]:
        s.set(x, G + 1, z, FLOWER)
    for (x, z) in spots[44:62]:
        s.set(x, G + 1, z, CARPET, carpet())
    # the half-buried chest (its lid just above the moss), a little heap of moss against it
    kx, ky, kz = PLEK_KIST
    h.ms.chest(s, kx, ky, kz, "south", "guhs:chests/bleke_open_plek")
    s.set(kx, ky + 1, kz, AIR)
    s.set(kx + 1, ky + 1, kz, CARPET, carpet())
    s.set(kx - 1, ky + 1, kz - 1, CARPET, carpet())
    jigsaw(s, C, G, C, PLEK_ANCHOR, "minecraft:pink_wool")
    s.clear_above(fp, G + 1)
    return s


# =====================================================================================================================
# Het Houthakkershutje
# =====================================================================================================================
def dagboek_lessenaar(h):
    """The lectern's block entity with the diary lying open on it (the same book as Dagboek.stack() in Java)."""
    B = h.ms.Byte
    return {"id": "minecraft:lectern", "Page": 0, "Book": {"id": "minecraft:written_book", "count": 1, "components": {
        "minecraft:written_book_content": {"title": {"raw": "Houthakkersdagboek"}, "author": "", "generation": 0,
                                           "pages": h.ms.NbtList(8, [json.dumps({"translate": f"book.guhs.bleekwoud.dagboek.{n}"})
                                                                    for n in range(DAGBOEK_PAGINAS)]),
                                           "resolved": B(1)},
        "minecraft:custom_name": json.dumps({"translate": DAGBOEK_TITEL, "italic": False, "color": "#D8D2CC"}),
        "minecraft:lore": h.ms.NbtList(8, [json.dumps({"translate": "book.guhs.bleekwoud.dagboek.door", "color": "gray", "italic": False})]),
        "minecraft:enchantment_glint_override": B(0),
        "minecraft:custom_data": {"GuhsBleekwoudDagboek": B(1)}}}}


def sign_nbt(h, lines):
    import sign_text
    B = h.ms.Byte
    msgs = sign_text.messages("sign.guhs.bleekwoud", lines)
    return {"id": "minecraft:sign", "is_waxed": B(1),
            "front_text": {"messages": msgs, "color": "black", "has_glowing_text": B(0)},
            "back_text": {"messages": sign_text.messages("sign.guhs.bleekwoud", ["", "", "", ""]), "color": "black", "has_glowing_text": B(0)}}


def hutje(h):
    s = h.Structure((HUT_W, HUT_H, HUT_W))
    rng = random.Random(128002)
    C = HUT_C
    B = h.ms.Byte
    fp = plate(s, C, 8.4)
    x0, x1, z0, z1 = 4, 10, 3, 8
    # the floor and the walls (logs on the corners)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            s.set(x, G, z, PLANK)
            edge = x in (x0, x1) or z in (z0, z1)
            for y in range(G + 1, G + 4):
                if edge:
                    corner = x in (x0, x1) and z in (z0, z1)
                    s.set(x, y, z, LOG if corner else PLANK, log() if corner else None)
                else:
                    s.set(x, y, z, AIR)
    # the gable roof: stairs up from both long sides, planks in the gable ends
    for i in range(4):
        for x in range(x0 - 1, x1 + 2):
            stair = {"half": "bottom", "shape": "straight", "waterlogged": "false"}
            s.set(x, G + 4 + i, z0 - 1 + i, "guhs:bleekhout_trap", {**stair, "facing": "south"})
            s.set(x, G + 4 + i, z1 + 1 - i, "guhs:bleekhout_trap", {**stair, "facing": "north"})
    for z in range(z0, z1 + 1):
        i = min(z - (z0 - 1), (z1 + 1) - z)
        for y in range(G + 4, G + 4 + i):
            for x in range(x0, x1 + 1):
                s.set(x, y, z, PLANK if x in (x0, x1) or z in (z0, z1) else AIR)
    # the door (south), two windows, his note next to the door
    dx, dy, dz = HUT_DEUR
    for half, y in (("lower", dy), ("upper", dy + 1)):
        s.set(dx, y, dz, "guhs:bleekhout_deur", {"facing": "north", "half": half, "hinge": "left", "open": "false", "powered": "false"})
    pane_z = {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"}
    s.set(x0, G + 2, 5, "minecraft:glass_pane", pane_z)
    s.set(x0, G + 2, 6, "minecraft:glass_pane", pane_z)
    s.set(x1, G + 2, 5, "minecraft:glass_pane", pane_z)
    s.set(*HUT_BORD, "guhs:bleekhout_wandbord", {"facing": "south", "waterlogged": "false"},
          sign_nbt(h, ["Verhuisd naar", "Knuffeldal.", "Slaap lekker!", "- Houthakkerguh"]))
    # inside: his bed, the chest, a crafting table, the diary on the lectern, a lantern under the ridge
    bx, by, bz = HUT_BED
    s.set(bx, by, bz, "minecraft:light_gray_bed", {"facing": "north", "part": "head", "occupied": "false"})
    s.set(bx, by, bz + 1, "minecraft:light_gray_bed", {"facing": "north", "part": "foot", "occupied": "false"})
    h.ms.chest(s, *HUT_KIST, "south", "guhs:chests/houthakkershutje")
    s.set(8, G + 1, 4, "minecraft:crafting_table")
    s.set(*HUT_LESSENAAR, "minecraft:lectern", {"facing": "west", "has_book": "true", "powered": "false"}, dagboek_lessenaar(h))
    s.set(7, G + 6, 5, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    s.set(6, G + 1, 7, CARPET, carpet())
    # outside: the woodpile against the west wall
    for z in (4, 5, 6):
        s.set(3, G + 1, z, LOG, log("z"))
    for z in (4, 5):
        s.set(3, G + 2, z, LOG, log("z"))
    # the tree he was half done with (a chop mark: one stripped log), the felled trunk next to it
    tx, tz = 13, 5
    for y in range(G + 1, G + 7):
        s.set(tx, y, tz, STRIPPED if y == G + 2 else LOG, log())
    canopy(s, rng, tx, tz, G + 7, 2.6, hang=0.25)
    for x in (11, 12, 13):
        s.set(x, G + 1, 9, LOG, log("x"))
    s.set(14, G + 1, 9, STRIPPED, log("x"))
    # the stump with his axe still in it (a fixed, invisible item frame lying on top)
    sx, sz = 12, 12
    s.set(sx, G + 1, sz, STRIPPED, log())
    s.entity(sx + 0.5, G + 2.03, sz + 0.5, {"id": "minecraft:item_frame", "Facing": B(1), "Fixed": B(1), "Invisible": B(1), "ItemRotation": B(3),
                                            "TileX": sx, "TileY": G + 2, "TileZ": sz, "Invulnerable": B(1),
                                            "Item": {"id": "minecraft:iron_axe", "count": 1}})
    # a lantern post by the door, flowers and tufts around
    s.set(5, G + 1, 10, "guhs:bleekhout_hek", {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})
    s.set(5, G + 2, 10, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    spots = [(x, z) for (x, z) in fp if s.get(x, G + 1, z) is None and s.get(x, G, z) == MOSS and not (6 <= x <= 8 and z >= 9)
             and math.dist((x, z), (C, C)) < 7.6]
    rng.shuffle(spots)
    for (x, z) in spots[:9]:
        s.set(x, G + 1, z, FLOWER)
    for (x, z) in spots[9:16]:
        s.set(x, G + 1, z, CARPET, carpet())
    jigsaw(s, C, G, z1, HUT_ANCHOR, PLANK)
    s.clear_above(fp, G + 1)
    return s


# =====================================================================================================================
# checks
# =====================================================================================================================
def check(plek, hut):
    problems = []
    if plek.get(*PLEK_HART) != HEART:
        problems.append("open plek: no heart")
    hx, hy, hz = PLEK_HART
    if plek.get(hx, hy + 1, hz) != LOG or plek.get(hx, hy - 1, hz) != LOG:
        problems.append("open plek: the heart needs a log above and below")
    if plek.get(hx, hy, hz + 1) not in (None, AIR):
        problems.append("open plek: the heart is not visible from the south")
    if plek.get(*PLEK_KIST) != "minecraft:chest" or plek.get(PLEK_KIST[0], PLEK_KIST[1] + 1, PLEK_KIST[2]) != AIR:
        problems.append("open plek: the chest is gone or can't open")
    if sum(1 for b in plek.blocks.values() if b[0] == FLOWER) < 30:
        problems.append("open plek: too few oogbloempjes")
    for name, pos in (("chest", HUT_KIST), ("lectern", HUT_LESSENAAR), ("light_gray_bed", HUT_BED)):
        if hut.get(*pos) != f"minecraft:{name}":
            problems.append(f"hutje: no {name} at {pos}")
        elif hut.get(pos[0], pos[1] - 1, pos[2]) != PLANK:
            problems.append(f"hutje: the {name} floats")
    if hut.get(*HUT_BORD) != "guhs:bleekhout_wandbord" or hut.get(HUT_BORD[0], HUT_BORD[1], HUT_BORD[2] - 1) != PLANK:
        problems.append("hutje: the note hangs on nothing")
    dx, dy, dz = HUT_DEUR
    if hut.get(dx, dy, dz) != "guhs:bleekhout_deur" or hut.get(dx, dy, dz + 1) not in (None, AIR) or hut.get(dx, dy, dz - 1) != AIR:
        problems.append("hutje: the door is blocked")
    for s, name in ((plek, "open plek"), (hut, "hutje")):
        for (x, y, z), (block, _p, _n) in s.blocks.items():
            if block in (FLOWER, CARPET) and s.get(x, y - 1, z) in (None, AIR):
                problems.append(f"{name}: {block} floats at {(x, y, z)}")
            if block == HANG and s.get(x, y + 1, z) not in (LEAVES, HANG, LOG):
                problems.append(f"{name}: hanging moss hangs on nothing at {(x, y, z)}")
    return problems


def test_templates(h):
    """A mossy field for the game tests (in a test the floor is y = 1: stand on y = 2). High enough for a big tree: the game
    test puts a cage of barriers around the template, a ceiling too."""
    veld = h.Structure((15, 22, 15))
    for x in range(15):
        for z in range(15):
            veld.set(x, 0, z, MOSS)
    veld.save("bleekwoud_test_veld")
    klein = h.Structure((7, 8, 7))
    for x in range(7):
        for z in range(7):
            klein.set(x, 0, z, MOSS)
    klein.save("bleekwoud_test_klein")


def build(h):
    plek, hut = open_plek(h), hutje(h)
    problems = check(plek, hut)
    if problems:
        raise SystemExit("bleekwoud structures check failed:\n  " + "\n  ".join(problems[:40]))
    plek.save(PLEK)
    hut.save(HUT)
    test_templates(h)
    print(f"bleekwoud: structures ok ({len(plek.blocks)} + {len(hut.blocks)} blocks)")
