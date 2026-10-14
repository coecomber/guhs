"""
1.4.2 Het Knabbelpark, slice park-plekjes b (Java: feature/park/plek; English: tools/lang/en/c142_park_b.json; design:
guhs_work142/DESIGN_142.md 9, 9a; the Plekjes API: park_bouw.py and reports/slice_park.md). Five more plekjes, each one
at most ONCE per park (a second copy becomes a flower bed: feature/park/plek/Uitkijktoren.schik):

  duivenvoer_oma   Oma-guh on her bench, feeding the Duifguhtjes (pigeon-parody little birds of the Guhmensie). Talk to her
                   and she gives you a handful of broodkruimels (once a day): strooi them and the Duifguhtjes flutter round
                   you and peck at your feet.
  uitkijktoren     a wooden lookout tower: a spiral staircase round a core, a deck with a railing, a pink roof and two
                   verrekijkers (right-click = zoom like a spyglass). NOT in the random pool: the park puts it on its HIGHEST
                   plekje spot (ParkStructure.layout -> plek.Uitkijktoren.schik swaps that plekje for the tower).
  poppenkast       a puppet theatre with benches and three guhkindjes: right-click the stage for the show (a guh, a Mika who
                   steals his knabbel, and the children shouting ACHTER JE!).
  wensboom         a big blossom tree with ribbons: hang a renamed paper on its trunk and your wish appears on a little
                   hanging sign under a branch, for everybody to see.
  mika_bankje      a bench with the sign "Verboden voor Mika's" and always one sulking Mika on it. Give him a kaasknabbel and
                   he leaves happily; the next day he is back.
  plek_b_vulsel    a small flower bed with a bench: what a second copy of one of the above becomes.

  park_plek_b_modellen.py   the Duifguhtje (model, texture, animations), the three NPCs' looks, the blocks
  park_plek_b_wiki.py       the wiki entries (not in FEATURES)
  here                      the templates, every Dutch text, sounds, advancements, FTB quests, the self-check against the Java

The template spots the Java needs are BlockPos constants in feature/park/plek/PlekB.java (read back by plekken()).
"""
# (first: park_bouw imports this module at its end and reads PLEKJES)
PLEKJES = [("duivenvoer_oma", 2), ("poppenkast", 2), ("wensboom", 2), ("mika_bankje", 2)]
TOREN, VULSEL = "uitkijktoren", "plek_b_vulsel"

import math      # noqa: E402
import os        # noqa: E402
import random    # noqa: E402
import re        # noqa: E402

from features import park_bouw as bouw      # noqa: E402

NAME = "park_plek_b"
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "park", "plek")
FTB_CHAPTER = "guhs_minigames"
FTB_SECTIES = [("parkb", "Plekjes in het Knabbelpark", "npc:omaguh", None)]
G, LOOP = bouw.G, bouw.LOOP
AIR = bouw.AIR
ZIT = 0.4375                 # the seat of a park bench (an NPC sits on it)


def plekken():
    """{NAAM: (x, y, z)} of the BlockPos constants in PlekB.java, and {NAAM_YAW: f}."""
    src = open(os.path.join(JAVA, "PlekB.java"), encoding="utf-8").read()
    out = {}
    for m in re.finditer(r"(\w+) = new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)", src):
        out[m.group(1)] = tuple(int(m.group(i)) for i in (2, 3, 4))
    for m in re.finditer(r"(\w+_YAW) = (-?[\d.]+)f", src):
        out[m.group(1)] = float(m.group(2))
    return out


# =====================================================================================================================
# helpers
# =====================================================================================================================
def bord(h, s, x, y, z, kijk, regels):
    """A standing cherry sign with four Dutch lines (keys sign.guhs.parkb.*, chunk c142_park_b)."""
    import sign_text
    B = h.Byte
    voor = {"messages": sign_text.messages("sign.guhs.parkb", regels), "color": "black", "has_glowing_text": B(0)}
    leeg = {"messages": sign_text.messages("sign.guhs.parkb", ["", "", "", ""]), "color": "black", "has_glowing_text": B(0)}
    rot = {"south": "0", "west": "4", "north": "8", "east": "12"}[kijk]
    s.set(x, y, z, "minecraft:cherry_sign", {"rotation": rot, "waterlogged": "false"},
          {"id": "minecraft:sign", "is_waxed": B(1), "front_text": voor, "back_text": leeg})


def wens_bordje(h, s, x, y, z, rot, regels, van):
    """A wish on a little hanging sign (the examples in the template; players' wishes are hung by Wensboom.java)."""
    import sign_text
    B = h.Byte
    voor = {"messages": sign_text.messages("sign.guhs.parkb", regels), "color": "black", "has_glowing_text": B(0)}
    achter = {"messages": sign_text.messages("sign.guhs.parkb", ["Wens van", van, "", ""]), "color": "black", "has_glowing_text": B(0)}
    s.set(x, y, z, "minecraft:cherry_hanging_sign", {"rotation": str(rot), "attached": "false", "waterlogged": "false"},
          {"id": "minecraft:hanging_sign", "is_waxed": B(1), "front_text": voor, "back_text": achter})


def zit_npc(h, s, plek, nbt):
    """An NPC on a park bench (the bench is at the spot; the NPC sits on its seat)."""
    x, y, z = plek
    s.entity(x + 0.5, y + ZIT, z + 0.5, nbt)


def duif_nbt(h, nr):
    rng = random.Random(1420300 + nr)
    return {"id": "guhs:duifguhtje", "PersistenceRequired": h.Byte(1), "Rotation": h.floats(rng.uniform(0, 360), 0.0),
            "NeoForgeData": {"guhs_bezetting": f"parkb_duif_{nr}"}}


def klinker_rond(s, cx, cz, r, rng=None):
    for x in range(s.size[0]):
        for z in range(s.size[2]):
            if math.hypot(x - cx, z - cz) <= r:
                bouw.pad_cel(s, x, z, bouw.KLINKERS)


# =====================================================================================================================
# the plekjes (rigid, G = 1, walk at y = 2, the entrance on the south edge in the middle)
# =====================================================================================================================
def duivenvoer_oma(h, p):
    """Oma-guh on her bench at the north side of a little paved square; four Duifguhtjes peck about; a dovecote, a bird bath."""
    sx, sy, sz = 13, 10, 11
    s, rng, mid = bouw.plekje_begin(h, "duivenvoer_oma", sx, sy, sz, 1420311, roze=0.35)
    klinker_rond(s, 6, 5, 3.4)
    for z in range(8, sz):
        bouw.pad_cel(s, mid, z)
    ox, oy, oz = p["OMA"]
    # her bench (two seats: one for you), a bin, a lantern; the paving in front of it
    bouw.bankje(s, ox, LOOP, oz, "south")
    bouw.bankje(s, ox - 1, LOOP, oz, "south")
    for x in (ox - 1, ox):
        bouw.zet_gras(s, x, G, oz, bouw.GRAS)
    bouw.prullenbak(s, ox - 2, LOOP, oz, "south")
    bouw.lantaarnpaal(s, ox + 2, LOOP, oz)
    zit_npc(h, s, p["OMA"], _npc(h, "omaguh", "parkb_oma", p["OMA_YAW"]))
    # the Duifguhtjes on the paving
    for nr in range(1, 5):
        x, y, z = p[f"DUIF_{nr}"]
        s.set(x, y, z, AIR)
        s.entity(x + 0.5, float(y), z + 0.5, duif_nbt(h, nr))
    # a dovecote on a post (east), a bird bath (west)
    for y in (LOOP, LOOP + 1):
        s.set(10, y, 3, bouw.PAAL, {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
    s.set(10, LOOP + 2, 3, "minecraft:birch_planks")
    s.set(10, LOOP + 3, 3, "minecraft:pink_wool")
    s.set(10, LOOP + 2, 4, "minecraft:birch_trapdoor", {"facing": "south", "half": "top", "open": "true", "powered": "false", "waterlogged": "false"})
    s.set(2, LOOP, 6, "minecraft:water_cauldron", {"level": "3"})
    # a blossom tree behind her bench, bushes, a sign
    bouw.boom(rng, s, 3, LOOP, 1, hoogte=4, r=2.2)
    bouw.boom(rng, s, 10, LOOP, 8, hoogte=3, r=1.6, soort="pluis")
    bouw.struik(rng, s, 1, LOOP, 9)
    bord(h, s, mid - 2, LOOP, sz - 2, "south", ["Duifjes voeren?", "Vraag het oma.", "Niet jagen hoor,", "roekoe!"])
    return bouw.plekje_af(s, rng, sx, sz, heg=0.4)


def poppenkast(h, p):
    """The puppet theatre: a booth with a striped awning at the north, the stage (guhs:park_poppenkast) in its window,
    two rows of benches with three guhkindjes facing it."""
    sx, sy, sz = 13, 10, 13
    s, rng, mid = bouw.plekje_begin(h, "poppenkast", sx, sy, sz, 1420312, roze=0.3)
    px, py, pz = p["PODIUM"]
    # the booth: x px-3..px+3, z pz-2..pz; a closed back and sides, the front with the window (px-1..px+1, py..py+1)
    for x in range(px - 3, px + 4):
        for z in range(pz - 2, pz + 1):
            s.set(x, G, z, "minecraft:spruce_planks")
            for y in range(LOOP, LOOP + 5):
                rand = x in (px - 3, px + 3) or z in (pz - 2, pz)
                if not rand:
                    s.set(x, y, z, AIR)
                    continue
                hoek = x in (px - 3, px + 3) and z in (pz - 2, pz)
                if hoek:
                    s.set(x, y, z, "minecraft:stripped_cherry_log", {"axis": "y"})
                else:
                    s.set(x, y, z, "minecraft:red_wool" if (x + y) % 2 == 0 else "minecraft:white_wool")
    for x in range(px - 1, px + 2):
        for y in (py, py + 1):
            s.set(x, y, pz, "minecraft:red_wool")          # the curtains beside the stage
    s.set(px, py, pz, "guhs:park_poppenkast", {"facing": "south", "open": "false"})
    s.set(px, py + 1, pz, AIR)                             # (room for the puppets)
    s.set(px, py - 1, pz, "minecraft:spruce_planks")       # the counter under the stage
    for x in range(px - 3, px + 4):
        s.set(x, py + 2, pz, "minecraft:yellow_wool")
    # the striped awning
    for x in range(px - 4, px + 5):
        for z in range(pz - 3, pz + 2):
            s.set(x, LOOP + 5, z, "minecraft:red_wool" if (x - px) % 2 == 0 else "minecraft:white_wool")
    s.set(px, LOOP + 6, pz - 1, "minecraft:pink_wool")
    # the audience: two rows of benches facing the stage, an aisle in the middle (x = mid)
    for z in (pz + 3, pz + 5):
        for x in (mid - 2, mid - 1, mid + 1, mid + 2):
            bouw.bankje(s, x, LOOP, z, "north")
    for z in range(pz + 1, sz):
        bouw.pad_cel(s, mid, z)
    for z in range(pz + 1, pz + 3):
        for x in range(mid - 2, mid + 3):
            bouw.pad_cel(s, x, z, bouw.KLINKERS)
    for nr in range(1, 4):
        zit_npc(h, s, p[f"KIND_{nr}"], _npc(h, "guhkindje", f"parkb_kind_{nr}", p["KIND_YAW"]))
    # lanterns, trees behind the booth, a sign
    bouw.lantaarnpaal(s, 1, LOOP, pz + 4)
    bouw.lantaarnpaal(s, sx - 2, LOOP, pz + 4)
    bouw.boom(rng, s, 1, LOOP, 1, hoogte=3, r=1.6, soort="pluis")
    bouw.boom(rng, s, sx - 2, LOOP, 1, hoogte=4, r=1.8)
    bord(h, s, mid + 2, LOOP, sz - 2, "south", ["De Poppenkast", "Klik op het podium", "voor de voorstelling!", "Njeg!"])
    return bouw.plekje_af(s, rng, sx, sz, heg=0.5)


# the wish tree: a 2x2 trunk of guhs:park_wensstam, four branches (horizontal guhbloesem logs) at TAK_Y; the wish signs
# hang under the branches (Wensboom.java finds those spots by itself)
WENS_STAM = (6, 4)            # the north-west column of the trunk
TAK_Y = LOOP + 4
WENS_VOORBEELDEN = [
    (["Meer knabbels", "voor iedereen!", "", ""], "Joostvadsje"),
    (["Dat Mika ook", "eens lief is", "", ""], "Lunde"),
    (["Een eigen", "Hertguhtje!", "", ""], "Trapguh"),
]


def wens_takken():
    """[(x, z, axis)] of the branch logs at TAK_Y (template coordinates)."""
    x0, z0 = WENS_STAM
    out = []
    out += [(x0 + 2 + k, z0 + 1, "x") for k in range(3)]       # east
    out += [(x0 - 1 - k, z0, "x") for k in range(3)]           # west
    out += [(x0, z0 - 1 - k, "z") for k in range(3)]           # north
    out += [(x0 + 1, z0 + 2 + k, "z") for k in range(3)]       # south
    return out


def wensboom(h, p):
    sx, sy, sz = 15, 16, 13
    s, rng, mid = bouw.plekje_begin(h, "wensboom", sx, sy, sz, 1420313, roze=0.5)
    x0, z0 = WENS_STAM
    cx, cz = x0 + 0.5, z0 + 0.5
    # paving round the trunk, the path in
    klinker_rond(s, cx, cz, 3.6)
    for z in range(z0 + 3, sz):
        bouw.pad_cel(s, mid, z)
    # the trunk, the branches, root flares
    for x in (x0, x0 + 1):
        for z in (z0, z0 + 1):
            for y in range(LOOP, TAK_Y + 2):
                s.set(x, y, z, "guhs:park_wensstam")
    for (x, z, axis) in wens_takken():
        s.set(x, TAK_Y, z, bouw.BLOESEM_STAM, {"axis": axis})
        s.set(x, TAK_Y - 1, z, AIR)
        for y in range(LOOP, TAK_Y - 1):
            if s.get(x, y, z) is None:
                s.set(x, y, z, AIR)
    # the crown: a big round of blossom leaves above the branches, with a few tufts at the branch tips
    for x in range(sx):
        for z in range(sz):
            for y in range(TAK_Y + 1, sy):
                d = math.sqrt((x - cx) ** 2 + (z - cz) ** 2 + ((y - (TAK_Y + 4)) * 1.3) ** 2)
                if d <= 5.2 and (d < 4.4 or rng.random() < 0.75) and s.get(x, y, z) is None:
                    s.set(x, y, z, bouw.BLOESEM_BLAD)
    for (x, z, axis) in wens_takken()[2::3]:
        s.set(x, TAK_Y + 1, z, bouw.BLOESEM_BLAD)
    # the ribbons: wool hanging under the edge of the crown (never where a wish hangs)
    takken = {(x, z) for x, z, _ in wens_takken()}
    kleuren = ["minecraft:pink_wool", "minecraft:white_wool", "minecraft:light_blue_wool", "minecraft:yellow_wool", "minecraft:lime_wool",
               "minecraft:magenta_wool"]
    n = 0
    for x in range(sx):
        for z in range(sz):
            if (x, z) in takken or s.get(x, TAK_Y + 1, z) != bouw.BLOESEM_BLAD or s.get(x, TAK_Y, z) is not None:
                continue
            if 2.5 < math.hypot(x - cx, z - cz) < 5 and rng.random() < 0.35:
                s.set(x, TAK_Y, z, kleuren[n % len(kleuren)])
                n += 1
    # three example wishes
    for (regels, van), (x, z, axis) in zip(WENS_VOORBEELDEN, [wens_takken()[1], wens_takken()[4], wens_takken()[10]]):
        wens_bordje(h, s, x, TAK_Y - 1, z, 0 if axis == "x" else 4, regels, van)
    # benches to sit and wish, lanterns, flowers, a sign
    bouw.bankje(s, mid - 3, LOOP, sz - 3, "north")
    bouw.bankje(s, mid + 3, LOOP, sz - 3, "north")
    bouw.lantaarnpaal(s, 1, LOOP, sz - 3)
    bouw.lantaarnpaal(s, sx - 2, LOOP, sz - 3)
    bord(h, s, mid + 1, LOOP, sz - 2, "south", ["De Wensboom", "Schrijf je wens", "op een papiertje", "en hang hem op!"])
    return bouw.plekje_af(s, rng, sx, sz, heg=0.35)


def mika_bankje(h, p):
    """One bench of three seats under a blossom tree, the sulking Mika on the middle one, the sign that says it all."""
    sx, sy, sz = 11, 10, 9
    s, rng, mid = bouw.plekje_begin(h, "mika_bankje", sx, sy, sz, 1420314, roze=0.4)
    mx, my, mz = p["MIKA"]
    for x in range(mx - 2, mx + 3):
        for z in range(mz + 1, mz + 3):
            bouw.pad_cel(s, x, z, bouw.KLINKERS)
    for z in range(mz + 3, sz):
        bouw.pad_cel(s, mid, z)
    for x in (mx - 1, mx, mx + 1):
        bouw.bankje(s, x, LOOP, mz, "south")
    zit_npc(h, s, p["MIKA"], _npc(h, "mokmika", "parkb_mika", p["MIKA_YAW"]))
    bord(h, s, mx + 3, LOOP, mz, "south", ["VERBODEN", "VOOR MIKA'S!", "Get. De", "parkwachter"])
    bouw.prullenbak(s, mx - 2, LOOP, mz, "south")
    bouw.lantaarnpaal(s, mx - 3, LOOP, mz + 1)
    bouw.boom(rng, s, mx, LOOP, mz - 2, hoogte=4, r=2.3)
    for x in range(sx):
        if s.get(x, LOOP, 0) is None:
            s.set(x, LOOP, 0, bouw.HEG)
    return bouw.plekje_af(s, rng, sx, sz, heg=0.3)


def vulsel(h, p):
    """The flower bed a second copy of a once-per-park plekje becomes: a round of flowers round a bush, a bench (7 x 7)."""
    sx, sy, sz = 7, 6, 7
    s, rng, mid = bouw.plekje_begin(h, VULSEL, sx, sy, sz, 1420315, roze=0.5)
    for x in range(sx):
        for z in range(sz - 2):
            if math.hypot(x - 3, z - 2) <= 2.6 and s.get(x, LOOP, z) is None:
                bouw.zet_gras(s, x, G, z, bouw.ROZE_GRAS)
                s.set(x, LOOP, z, bouw.bloem(rng))
    bouw.struik(rng, s, 3, LOOP, 2)
    bouw.bankje(s, mid, LOOP, sz - 3, "north")
    return bouw.plekje_af(s, rng, sx, sz, heg=0.2)


# --- the lookout tower --------------------------------------------------------------------------------------------------
TOREN_MAAT = (9, 20, 9)
TREDEN = 10                   # stairs up; the deck floor is the layer of the last one (y = LOOP + TREDEN - 1)
DEK = LOOP + TREDEN - 1        # the deck's floor layer (you stand on it at DEK + 1)
# the ring of the spiral (the 5 x 5 inside of the tower round a 3 x 3 core), from the door (south) round to the west...
RING = [(3, 6), (2, 6), (2, 5), (2, 4), (2, 3), (2, 2), (3, 2), (4, 2), (5, 2), (6, 2), (6, 3), (6, 4), (6, 5), (6, 6), (5, 6), (4, 6)]


def trap_richting(i):
    """The way you walk up at step i (the facing of its stair block)."""
    (x0, z0), (x1, z1) = RING[i], RING[i + 1]
    return bouw.naar(x1 - x0, z1 - z0)


def uitkijktoren(h, p):
    sx, sy, sz = TOREN_MAAT
    s, rng, mid = bouw.plekje_begin(h, TOREN, sx, sy, sz, 1420316, roze=0.4)
    # the foundation of the tower and its floor
    for x in range(1, 8):
        for z in range(1, 8):
            s.set(x, G, z, "minecraft:spruce_planks")
    # walls (x/z = 1 or 7) up to under the deck, stripped cherry corners, windows; the door in the south wall
    for y in range(LOOP, DEK):
        for x in range(1, 8):
            for z in range(1, 8):
                rand = x in (1, 7) or z in (1, 7)
                if not rand:
                    continue
                hoek = x in (1, 7) and z in (1, 7)
                s.set(x, y, z, "minecraft:stripped_cherry_log" if hoek else "minecraft:spruce_planks", {"axis": "y"} if hoek else None)
    for y in (LOOP + 3, LOOP + 6):
        for (x, z, ns) in ((4, 1, False), (4, 7, False), (1, 4, True), (7, 4, True)):
            s.set(x, y, z, "minecraft:pink_stained_glass_pane", bouw.pane(ns))
    for y in (LOOP, LOOP + 1):
        s.set(mid, y, 7, AIR)
    s.set(mid, G, 7, bouw.PAD)
    # the core (3 x 3) with a warm light every few steps
    for y in range(LOOP, DEK + 1):
        for x in range(3, 6):
            for z in range(3, 6):
                licht = (x, z) != (4, 4) and (x == 4 or z == 4) and y in (LOOP + 2, LOOP + 6)
                s.set(x, y, z, "minecraft:ochre_froglight" if licht else "minecraft:stripped_spruce_log", {"axis": "y"})
    # the spiral: stair i at y = LOOP + i on RING[i]; air above every step
    for x, z in RING:
        for y in range(LOOP, DEK):
            s.set(x, y, z, AIR)
    for i in range(TREDEN):
        x, z = RING[i]
        s.set(x, LOOP + i, z, "minecraft:spruce_stairs", {"facing": trap_richting(i), "half": "bottom", "shape": "straight", "waterlogged": "false"})
        for y in range(LOOP, LOOP + i):
            s.set(x, y, z, "minecraft:spruce_planks")
    s.set(6, LOOP, 6, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    # the deck (the whole 9 x 9, over the walls), open above the last steps; a railing round the edge and round the hole
    gat = {RING[i] for i in range(TREDEN - 4, TREDEN - 1)}
    for x in range(sx):
        for z in range(sz):
            if (x, z) in gat or (x, z) == RING[TREDEN - 1]:
                continue
            s.set(x, DEK, z, "minecraft:spruce_planks")
    hek = {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}
    for x in range(sx):
        for z in range(sz):
            if x in (0, sx - 1) or z in (0, sz - 1):
                s.set(x, DEK + 1, z, bouw.PAAL, dict(hek))
    for (x, z) in gat:
        for dx, dz in bouw.DIRS.values():
            n = (x + dx, z + dz)
            if n not in gat and n != RING[TREDEN - 1] and 0 < n[0] < sx - 1 and 0 < n[1] < sz - 1:
                s.set(n[0], DEK + 1, n[1], bouw.PAAL, dict(hek))
    for x, z in gat:
        for y in (DEK, DEK + 1, DEK + 2):
            s.set(x, y, z, AIR)
    x, z = RING[TREDEN - 1]
    for y in (DEK + 1, DEK + 2, DEK + 3):
        s.set(x, y, z, AIR)
    # lantern posts on two corners of the railing, the roof on four posts, the verrekijkers, a bench
    for (x, z) in ((0, 0), (sx - 1, sz - 1)):
        s.set(x, DEK + 2, z, bouw.PAAL, dict(hek))
        s.set(x, DEK + 3, z, bouw.LAMP)
    for (x, z) in ((1, 1), (7, 1), (1, 7), (7, 7)):
        for y in (DEK + 1, DEK + 2, DEK + 3):
            s.set(x, y, z, "minecraft:stripped_cherry_log", {"axis": "y"})
    for k, y in enumerate(range(DEK + 4, DEK + 8)):
        for x in range(1 + k, 8 - k):
            for z in range(1 + k, 8 - k):
                s.set(x, y, z, "minecraft:pink_terracotta" if k % 2 == 0 else "minecraft:pink_wool")
    for naam in ("KIJKER_OOST", "KIJKER_WEST"):
        x, y, z = p[naam]
        s.set(x, y, z, "guhs:park_verrekijker", {"facing": "east" if naam.endswith("OOST") else "west"})
    bouw.bankje(s, 4, DEK + 1, 6, "south")
    # outside: a sign at the door, flowers and a bush or two round the foot
    bord(h, s, mid - 2, LOOP, sz - 1, "south", ["De Uitkijktoren", "Het hoogste puntje", "van het park!", "Klim maar, njeg"])
    bouw.struik(rng, s, 0, LOOP, 0)
    bouw.struik(rng, s, sx - 1, LOOP, 2, soort="pluis")
    bouw.hekken_vast(s)
    out = bouw.plekje_af(s, rng, sx, sz, heg=0.15)
    return out


def templates(h):
    """{template name (under knabbelpark/): Structure} of this slice (park_bouw.templates() adds them)."""
    p = plekken()
    return {"duivenvoer_oma": duivenvoer_oma(h, p), "poppenkast": poppenkast(h, p), "wensboom": wensboom(h, p),
            "mika_bankje": mika_bankje(h, p), TOREN: uitkijktoren(h, p), VULSEL: vulsel(h, p)}


def _npc(h, kind, id_, yaw):
    from features import wereld
    return wereld.npc(h, kind, id_, yaw=yaw)


# =====================================================================================================================
# texts (Dutch; the English is in tools/lang/en/c142_park_b.json)
# =====================================================================================================================
POPPENKAST = [   # (who: guh / mika / kinderen / verteller, text) - the show, page by page
    ("verteller", "Welkom bij de poppenkast! Vandaag: Guh en de gestolen knabbel. Ssst, het begint!"),
    ("guh", "Hallo kindjes! Kijk eens wat ik heb: een héle grote kaasknabbel. Mmm, njeg!"),
    ("guh", "Ik leg hem even hier neer, op het randje. Ik ga gauw een servetje halen."),
    ("mika", "Hihihi... Een knabbel, zomaar op het randje? Die is nu van MIJ!"),
    ("kinderen", "ACHTER JE! ACHTER JE!"),
    ("guh", "Achter me? Hè? Ik zie helemaal niks..."),
    ("kinderen", "ACHTER JEEEEE!"),
    ("guh", "AHA! Mika! Mijn knabbel! Geef terug, jij kleine smikkelaar!"),
    ("mika", "Oké, oké... Maar mag ik dan één klein hapje? Ik had zo'n trek."),
    ("guh", "Natuurlijk! Delen is lief. Samen knabbelen is twee keer zo lekker. Vahoeg!"),
    ("verteller", "Einde! Klap maar in je pootjes. Njeg!"),
]

TEXTS = {
    # names
    "entity.guhs.guh_npc.omaguh": "Oma-guh",
    "entity.guhs.guh_npc.mokmika": "Mokkende Mika",
    "entity.guhs.guh_npc.guhkindje": "Guhkindje",
    "block.guhs.park_verrekijker": "Verrekijker",
    "block.guhs.park_verrekijker.lore": "Rechtsklik: kijk ver weg, net als door een echte verrekijker. Sluipen = weer gewoon kijken",
    "block.guhs.park_poppenkast": "Poppenkast",
    "block.guhs.park_wensstam": "Wensboomstam",
    "block.guhs.park_wensstam.lore": "Hang een hernoemd papiertje aan de stam: je wens komt in de boom",
    "item.guhs.park_kruimels": "Broodkruimels",
    "item.guhs.park_kruimels.lore": "Rechtsklik: strooi een handje. De Duifguhtjes komen er meteen op af!",
    "item.guhs.duifguhtje_spawn_egg": "Duifguhtje-spawnei",
    "subtitles.guhs.parkb.roekoe": "Duifguhtje: roekoe",
    "subtitles.guhs.parkb.applaus": "Guhkindjes klappen",
    # Oma-guh and the crumbs
    "quest.guhs.parkb.oma.hallo0": "Dag lieverd! Kom je de Duifguhtjes ook voeren? Ze zijn zo gezellig. Roekoe!",
    "quest.guhs.parkb.oma.hallo1": "Vroeger voerde ik de duifjes met Opa Kaasguh. Hij at de helft van de kruimels zelf op, njeg.",
    "quest.guhs.parkb.oma.hallo2": "Die dikke daar heet Kruimeltje. En die daar ook. Ze heten eigenlijk allemaal Kruimeltje.",
    "quest.guhs.parkb.oma.hallo3": "Niet rennen, hoor. Duifjes houden van rustig. En van kruimels. Vooral van kruimels.",
    "quest.guhs.parkb.oma.kruimels": "Hier, een handje broodkruimels voor jou. Strooi maar (rechtsklik), dan komen ze er zo op af!",
    "quest.guhs.parkb.oma.genoeg": "Je hebt nog kruimels, lieverd. Strooi die eerst maar op!",
    "quest.guhs.parkb.oma.morgen": "Morgen krijg je weer een handje. Oma moet ook een beetje zuinig zijn, njeg.",
    "gui.guhs.parkb.kruimels.gestrooid": "Je strooit een handje kruimels. Roekoe!",
    "gui.guhs.parkb.duif.gevoerd": "Roekoe! Het Duifguhtje smult van je kruimels.",
    # the verrekijker
    "gui.guhs.parkb.verrekijker.kijk": "Je kijkt door de verrekijker. Sluip (shift) om te stoppen.",
    "gui.guhs.parkb.verrekijker.toren": "Vanaf het hoogste puntje van het park zie je alles. Vahoeg!",
    # the puppet show
    "gui.guhs.parkb.poppenkast.naam.verteller": "Poppenkast",
    "gui.guhs.parkb.poppenkast.naam.guh": "Guh-pop",
    "gui.guhs.parkb.poppenkast.naam.mika": "Mika-pop",
    "gui.guhs.parkb.poppenkast.naam.kinderen": "De guhkindjes",
    "gui.guhs.parkb.poppenkast.achter_je": "ACHTER JE!",
    "gui.guhs.parkb.poppenkast.applaus": "De guhkindjes klappen in hun pootjes. Njeg njeg!",
    "quest.guhs.parkb.kind.0": "Ik wil de poppenkast zien! Klik jij op het podium?",
    "quest.guhs.parkb.kind.1": "Ssst! Het begint zo. Ik hoop dat Mika weer komt. Mika is zo grappig!",
    "quest.guhs.parkb.kind.2": "Ik heb de voorstelling al tien keer gezien. Ik ga hem nog tien keer zien. Vahoeg!",
    "quest.guhs.parkb.kind.3": "Als je Mika ziet, moet je heel hard roepen. Dat is de regel.",
    # the wish tree
    "gui.guhs.parkb.wens.gehangen": "Je wens hangt in de boom: \"%s\". Die komt vast uit, njeg!",
    "gui.guhs.parkb.wens.vervangen": "Je oudste wens maakt plaats voor je nieuwe (je mag er %s per boom).",
    "gui.guhs.parkb.wens.geen_naam": "Schrijf je wens eerst op het papiertje: hernoem het op een aambeeld.",
    "gui.guhs.parkb.wens.vol": "Er is geen plekje meer vrij aan de takken. Probeer het straks nog eens.",
    "gui.guhs.parkb.wens.info": "In deze boom hangen %s wensen. Hang er zelf een op: rechtsklik de stam met een hernoemd papiertje.",
    "gui.guhs.parkb.wens.weg": "Je hebt je wensen weer uit de boom gehaald.",
    "gui.guhs.parkb.wens.van": "Wens van",
    "gui.guhs.parkb.wens.gewist": "%s wensen uit de boom gehaald.",
    # the sulking Mika
    "quest.guhs.parkb.mika.mok0": "Hmpf. Ik zit hier gewoon. Mag dat soms niet?",
    "quest.guhs.parkb.mika.mok1": "Dat bord is gemeen. Ik ben maar een HEEL klein beetje Mika.",
    "quest.guhs.parkb.mika.mok2": "Iedereen mag op een bankje. Behalve Mika's. Dat is toch niet eerlijk...",
    "quest.guhs.parkb.mika.mok3": "Ga weg. Of... heb je toevallig een kaasknabbel? Nee? Hmpf.",
    "quest.guhs.parkb.mika.duw": "Hé! Dit is MIJN bankje! Hmpf!",
    "quest.guhs.parkb.mika.blij": "Een... een kaasknabbel? Voor MIJ? Njeg! Dankjewel! Weet je wat? Ik zoek een bankje waar Mika's "
                                  "WEL mogen zitten. Doei!",
    "gui.guhs.parkb.mika.weg": "De Mika huppelt blij weg. Morgen zit hij er vast weer.",
    "gui.guhs.parkb.mika.terug": "%s Mika's zitten weer op hun bankje.",
    # the commands
    "gui.guhs.parkb.cmd.toren": "Uitkijktorens in dit park: %s (op hoogte %s, het hoogste plekje %s)",
}

GUHDEX = ("duifguhtje", "Duifguhtje", "vaak (alleen in het Knabbelpark, bij Oma-guh)",
          "Een mollig grijs vogeltje met een glimmend groen-paars nekje en echte guh-oogjes. Duifguhtjes wonen bij Oma-guh in het "
          "Knabbelpark en zijn dol op broodkruimels: houd er een paar vast en ze fladderen om je heen. Roekoe!")

VERBORGEN = ["parkb_duif_gevoerd", "parkb_kruimels", "parkb_verrekijker", "parkb_poppenkast", "parkb_wens", "parkb_mika_blij"]

NPCS = {"omaguh": None, "mokmika": None, "guhkindje": (0.95, 0.75, 1.02)}


def texts(h):
    from features import bbq2
    for k, v in TEXTS.items():
        h.lang(k, v, v)
    for i, (wie, tekst) in enumerate(POPPENKAST, start=1):
        h.lang(f"gui.guhs.parkb.poppenkast.{i}", tekst, tekst)
    for kind, kleur in NPCS.items():
        naam = TEXTS[f"entity.guhs.guh_npc.{kind}"]
        if kleur:
            bbq2.stub_npc(h, kind, naam, hue=kleur[0], sat=kleur[1], val=kleur[2])
        else:
            h.lang(f"entity.guhs.guh_npc.{kind}", naam, naam)
    id_, naam, zeldzaam, info = GUHDEX
    bbq2.pagina(h, id_, naam, zeldzaam, info)
    for name in VERBORGEN:
        bbq2.verborgen(h, name)


def sounds(h):
    def patch(d):
        d["parkb.roekoe"] = {"sounds": [{"name": "minecraft:entity.parrot.ambient", "type": "event", "pitch": 0.55, "volume": 0.7},
                                        {"name": "guhs:vogels.pluisvinkje", "type": "event", "pitch": 0.6, "volume": 0.5}],
                             "subtitle": "subtitles.guhs.parkb.roekoe"}
        d["parkb.applaus"] = {"sounds": [{"name": "guhs:entity.guh.happy", "type": "event", "pitch": 1.4, "volume": 0.8},
                                         {"name": "minecraft:block.note_block.chime", "type": "event", "pitch": 1.2, "volume": 0.5}],
                              "subtitle": "subtitles.guhs.parkb.applaus"}
        return d
    h.patch_json(f"{h.A}/sounds.json", patch)


def recepten_en_loot(h):
    h.shapeless("park_kruimels", ["minecraft:bread"], "guhs:park_kruimels", 6)
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:park_wensstam"])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:park_verrekijker"])


def test_templates(h):
    s = h.Structure((15, 6, 15))
    s.fill(0, 0, 0, 14, 0, 14, "minecraft:grass_block", {"snowy": "false"})
    s.save("parkb_test_kamer")


# =====================================================================================================================
# FTB quests (chapter guhs_minigames, section "Plekjes in het Knabbelpark")
# =====================================================================================================================
def ftb(fq):
    q, adv = fq.q, fq.adv
    kn = "guhs:kaas_knabbels"
    q("parkb_duiven", "Duivenvoeren met oma", "In sommige Knabbelparken zit &dOma-guh&r op haar bankje de &dDuifguhtjes&r te voeren. "
      "Vraag haar om een handje &6broodkruimels&r en strooi mee (rechtsklik). Roekoe!", "guhs:park_kruimels",
      [adv("parkb_kruimels")], rewards=((kn, 6),), deps=["park_gevonden"])
    q("parkb_duifguhtje", "Een Duifguhtje gezien", "Bekijk een &dDuifguhtje&r van dichtbij: zijn bladzijde in de Guhdex vult zich "
      "(een extra bladzijde: hij telt niet mee voor 'alles verzameld').", "guhs:duifguhtje_spawn_egg", [adv("seen_duifguhtje")],
      rewards=((kn, 4),), deps=["parkb_duiven"])
    q("parkb_toren", "Het hoogste puntje", "Een &6uitkijktoren&r staat altijd op het hoogste plekje van zijn park. Klim naar boven "
      "en kijk door de &6verrekijker&r.", "guhs:park_verrekijker", [adv("parkb_verrekijker")], rewards=((kn, 6),), deps=["park_gevonden"])
    q("parkb_poppenkast", "ACHTER JE!", "Klik op het podium van een &6poppenkast&r en kijk de voorstelling. Roep maar mee met de "
      "guhkindjes!", "guhs:park_poppenkast", [adv("parkb_poppenkast")], rewards=((kn, 4),), deps=["park_gevonden"])
    q("parkb_wens", "Doe een wens", "Hernoem een &6papiertje&r op een aambeeld (dat is je wens) en hang het aan de stam van een "
      "&6wensboom&r. Iedereen kan je wens daarna lezen. Lief blijven, hè!", "minecraft:paper", [adv("parkb_wens")],
      rewards=((kn, 6),), deps=["park_gevonden"])
    q("parkb_mika", "Een blije Mika", "Op een bankje met het bordje 'Verboden voor Mika's' zit altijd een &cmokkende Mika&r. Hij duwt "
      "wel als je te dichtbij komt, maar met een &6kaasknabbel&r wordt hij heel blij...", "guhs:kaas_knabbels",
      [adv("parkb_mika_blij")], rewards=((kn, 8),), deps=["park_gevonden"])


# =====================================================================================================================
# self-check + build
# =====================================================================================================================
def _java():
    out = ""
    for root, _dirs, files in os.walk(JAVA):
        for f in files:
            if f.endswith(".java"):
                out += open(os.path.join(root, f), encoding="utf-8").read()
    return out


def selfcheck(h, t, p):
    problems = []
    java = _java()
    for m in re.finditer(r'"((?:gui|quest)\.guhs\.parkb[\w.]*)"', java):
        k = m.group(1)
        if not k.endswith(".") and k not in TEXTS:
            problems.append(f"Java names {k}, no Dutch text")
    for k in ([f"quest.guhs.parkb.oma.hallo{i}" for i in range(4)] + [f"quest.guhs.parkb.kind.{i}" for i in range(4)]
              + [f"quest.guhs.parkb.mika.mok{i}" for i in range(4)]):
        if k not in TEXTS:
            problems.append(f"no text {k}")
    if f"SHOW = {len(POPPENKAST)}" not in java:
        problems.append(f"Poppenkast.SHOW must be {len(POPPENKAST)} (the pages of POPPENKAST)")
    for naam, _ in PLEKJES:
        if f'"knabbelpark/{naam}"' not in java:
            problems.append(f"Uitkijktoren.UNIEK does not name knabbelpark/{naam}")
    for naam in (TOREN, VULSEL):
        if f'"knabbelpark/{naam}"' not in java:
            problems.append(f"the Java does not name knabbelpark/{naam}")
    if f"TOREN_MAAT = {TOREN_MAAT[0]}" not in java:
        problems.append("Uitkijktoren.TOREN_MAAT differs from park_plek_b.TOREN_MAAT")
    # templates: the entrance, spots on a floor with room, the tower walkable up to its verrekijkers
    for naam in [n for n, _ in PLEKJES] + [TOREN, VULSEL]:
        s = t[naam]
        cel = bouw.bereikbaar(s, (s.size[0] // 2, s.size[2] - 1))
        if len(cel) < 4:
            problems.append(f"plekje {naam}: you can hardly walk in")
    for naam in [n for n, _ in PLEKJES]:
        if t[naam].size[0] < t[VULSEL].size[0] or t[naam].size[2] < t[VULSEL].size[2]:
            problems.append(f"{naam} is smaller than the filler that replaces its second copy")
    for naam, plek in (("duivenvoer_oma", "OMA"), ("mika_bankje", "MIKA")) + tuple(("poppenkast", f"KIND_{n}") for n in (1, 2, 3)):
        x, y, z = p[plek]
        if t[naam].get(x, y, z) != bouw.BANK:
            problems.append(f"{naam}: {plek} does not sit on a park bench")
        cel = bouw.bereikbaar(t[naam], (t[naam].size[0] // 2, t[naam].size[2] - 1))
        if not any(math.hypot(cx - x, cz - z) <= 1.6 for cx, cz in cel):
            problems.append(f"{naam}: {plek} can not be reached")
    x, y, z = p["PODIUM"]
    if t["poppenkast"].get(x, y, z) != "guhs:park_poppenkast":
        problems.append("poppenkast: PlekB.PODIUM is not the stage")
    for naam in ("KIJKER_OOST", "KIJKER_WEST"):
        x, y, z = p[naam]
        if t[TOREN].get(x, y, z) != "guhs:park_verrekijker" or y != DEK + 1:
            problems.append(f"uitkijktoren: {naam} is not a verrekijker on the deck")
    problems += toren_loop(t[TOREN], p)
    if problems:
        raise SystemExit("park_plek_b: not right:\n  " + "\n  ".join(problems[:60]))


def toren_loop(s, p):
    """Walk the tower in 3D (a floor, two free blocks; up 1 only over a stair or with room to jump; down at most 3): from the
    entrance you must reach both verrekijkers on the deck."""
    def vrij(x, y, z):
        return bouw._vrij(s, x, y, z)

    def vloer(x, y, z):
        n = s.get(x, y, z)
        return n is not None and n not in (AIR, bouw.VOID) and not bouw._vrij(s, x, y, z) and "fence" not in n and "lantaarn" not in n \
            and "lantern" not in n and "verrekijker" not in n and "bankje" not in n

    def staan(x, y, z):
        return vloer(x, y - 1, z) and vrij(x, y, z) and vrij(x, y + 1, z)

    sx, sy, sz = s.size
    begin = (sx // 2, LOOP, sz - 1)
    gezien, rij = {begin}, [begin]
    while rij:
        x, y, z = rij.pop()
        for dx, dz in bouw.DIRS.values():
            nx, nz = x + dx, z + dz
            if not (0 <= nx < sx and 0 <= nz < sz):
                continue
            for dy in (0, 1, -1, -2, -3):
                q = (nx, y + dy, nz)
                if q in gezien:
                    break
                if dy == 1 and not vrij(x, y + 2, z):
                    continue
                if staan(*q):
                    gezien.add(q)
                    rij.append(q)
                    break
                if dy < 0 and not (vrij(nx, y + dy + 1, nz) and vrij(nx, y + dy + 2, nz)):
                    break
    problems = []
    for naam in ("KIJKER_OOST", "KIJKER_WEST"):
        x, y, z = p[naam]
        if not any(abs(gx - x) + abs(gz - z) <= 1 and gy == y for gx, gy, gz in gezien):
            problems.append(f"uitkijktoren: {naam} can not be reached on foot (reached {len(gezien)} spots, highest "
                            f"{max(g[1] for g in gezien)})")
    return problems


def build(h):
    from features import park_plek_b_modellen as modellen
    # (the templates themselves are saved by park.py: park_bouw.templates() takes ours; the pools from park_bouw.PLEKJES)
    t = templates(h)
    selfcheck(h, t, plekken())
    test_templates(h)
    texts(h)
    sounds(h)
    recepten_en_loot(h)
    modellen.build(h)
