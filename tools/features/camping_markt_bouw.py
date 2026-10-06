"""
bbq2 (camping-markt): the two buildings of features/camping_markt.py, built block by block.

  grillcamping     a real guh village on a cave floor of the Guhbarbecuether: a lawn of pindasaus-nylium in the dark rock, a
                   gate with the name board, a paved fire circle with the big camp fire and log benches, the reception
                   chalet of the Kampbaas-guh (a steep shingle roof, a porch with his desk and bell, a flag pole), tents of
                   real tent canvas in four colours (stairs and slabs of guhs:campingmarkt_tentdoek_*: sloping roofs, not
                   wool boxes), the Houthakker-guh's wood shed with the chopping blocks, a washing line, a picnic table, a
                   sate tree, and four free pitches where every player pitches a tent of their own.
  mika_ruilmarkt   the barter market of the Nether-Mika's: a paved square with the Waag in the middle (four iron pillars
                   under a striped tent roof, the big scales and the five stacks of vads on its counter), seven stalls with
                   striped awnings in a ring around it (three with a Kraam-Mika behind the counter), the Marktmeester-Mika
                   on his rostrum, lamp posts, hand carts, crates, a notice board and the gate.

Both are ONE template for the type guhs:barbecueput (wereld.bbq_structuur soort "grot"); the centre jigsaw is in layer 0 and
the start pool gets ground_level_delta = G + 1 (the trick of features/fossiel_mijn_bouw.py), so the ground you walk on lands
on the cave floor.

Small templates: campingmarkt_plek (an empty pitch: the sign, air where a tent can stand) and campingmarkt_tent (the same
box with the tent a player pitches there, its four pegs still loose). Java places the tent over the pitch and Herstel puts
the empty pitch back (Kamperen.java); PLEK_MAAT / PLEK_BORD are shared with it.

Numbers the Java side shares (CampingmarktFeature; a game test compares): KAMPBAAS, HOUTHAKKER, MARKTMEESTER (the NPCs),
KAMPEERDERS (the resident guhs), KRAAM_MIKAS (the stall holders), PLEK_BORD.
check(...) is the geometry self-check that build_all() runs. preview(out) draws both from four sides (run this file).
"""
import math
import os
import random
import sys

CAMPING = "grillcamping"
MARKT = "mika_ruilmarkt"
CAMPING_MAAT = (45, 18, 45)
MARKT_MAAT = (45, 18, 45)
G = 3                    # template y of the top block of the ground (you walk at G + 1)
C = 22                   # the middle of both templates (x and z)

HOUTSKOOL = "guhs:houtskoolsteen"
STENEN = "guhs:houtskoolsteen_stenen"
GEBARSTEN = "guhs:gebarsten_houtskoolsteen_stenen"
GEBEITELD = "guhs:gebeitelde_houtskoolsteen_stenen"
MUUR = "guhs:houtskoolsteen_stenen_muur"
PLAAT = "guhs:houtskoolsteen_stenen_plaat"
TRAP = "guhs:houtskoolsteen_stenen_trap"
ROOSTER = "guhs:roosterijzer"
GEPOLIJST = "guhs:gepolijst_roosterijzer"
PILAAR = "guhs:roosterijzer_pilaar"
AS = "guhs:as_blok"
AS_AARDE = "guhs:as_aarde"
PINDA = "guhs:pindasaus_nylium"
MOSTERD = "guhs:mosterd_nylium"
SATE_STAM = "guhs:sate_stam"
WORST_STAM = "guhs:worst_stam"
SATE_VLEES = "guhs:sate_vlees"
MOSTERD_BLOK = "guhs:mosterd_blok"
UIENLICHT = "guhs:uienlicht"
SMEUL = "guhs:smeulkooltjes"
SLAAPZAK = "guhs:guh_slaapzak"
STAM = "minecraft:spruce_log"
KAAL = "minecraft:stripped_spruce_log"
PLANK = "minecraft:spruce_planks"
DONKER = "minecraft:dark_oak_planks"
AIR = "minecraft:air"
# this slice's own blocks
KAMPVUUR = "guhs:campingmarkt_kampvuur"
HAKBLOK = "guhs:campingmarkt_hakblok"
BORD = "guhs:campingmarkt_kampeerplek"
HARING = "guhs:campingmarkt_haring"
WEEGSCHAAL = "guhs:campingmarkt_weegschaal"
VADSSTAPEL = "guhs:campingmarkt_vadsstapel"
KLEUREN = ("rood", "geel", "groen", "blauw", "creme")


def doek(kleur):
    return f"guhs:campingmarkt_tentdoek_{kleur}"


def doektrap(kleur):
    return f"guhs:campingmarkt_tentdoek_{kleur}_trap"


def doekplaat(kleur):
    return f"guhs:campingmarkt_tentdoek_{kleur}_plaat"


# --- shared with Java (CampingmarktFeature / Kamperen / Ruilmarkt): template block positions, feet level --------------
KAMPBAAS = (22, G + 2, 9)             # on his stool behind the desk of the reception
KAMPBAAS_YAW = 0.0                    # (looks south: at the fire and whoever comes in)
HOUTHAKKER = (33, G + 2, 21)          # on a stump by his wood shed
HOUTHAKKER_YAW = 90.0                 # (looks west: at the chopping blocks and the fire)
MARKTMEESTER = (22, G + 2, 28)        # on his rostrum in front of the Waag
MARKTMEESTER_YAW = 0.0                # (looks south: at the gate)
# the resident guhs of the camping: (x, y, z, variant, scale, sits, yaw, head piece, neck piece, back piece)
KAMPEERDERS = [
    (22, G + 2, 18, "asguh", 1.0, True, 0.0, "campingmarkt_hoedje", "", ""),
    (18, G + 2, 22, "choco", 0.95, True, -90.0, "", "campingmarkt_halsdoek", ""),
    (12, G + 1, 23, "mint", 1.0, False, -90.0, "campingmarkt_hoedje", "", "campingmarkt_rugzak"),
    (30, G + 1, 17, "normal", 1.05, False, 135.0, "", "campingmarkt_halsdoek", "campingmarkt_rugzak"),
    (13, G + 1, 17, "asguh", 0.6, False, 20.0, "", "campingmarkt_halsdoek", ""),
]
# the stall holders of the market: (x, y, z, yaw): Nether-Mika's that stand behind their counter (the stalls 0, 3 and 6 of KRAMEN)
KRAAM_MIKAS = [(22, G + 1, 7, 0.0), (8, G + 1, 22, -90.0), (30, G + 1, 33, 180.0)]
# a pitch: the box of both small templates, and where its sign stands in it (the tent's door is to the south, at z 3)
PLEK_MAAT = (7, 3, 5)
PLEK_BORD = (3, 0, 4)
# the pitches of the camping: (the sign's x, z in the template, quarter turns clockwise: 0 = the door to the south)
PLEKKEN = [(14, 31, 3), (30, 31, 1), (17, 34, 2), (27, 34, 2)]

FENCE = {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}
WALL = {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none", "waterlogged": "false"}
RICHTING = ["north", "east", "south", "west"]
STAP = {"north": (0, -1), "east": (1, 0), "south": (0, 1), "west": (-1, 0)}
TEGEN = {"north": "south", "south": "north", "east": "west", "west": "east"}


def mc(n):
    return n if ":" in n else f"minecraft:{n}"


def ruis(seed):
    """Smooth 2D value noise in 0..1 (a function of x, z), for patchy ground."""
    rng = random.Random(seed)
    grid = [[rng.random() for _ in range(64)] for _ in range(64)]

    def f(x, z, schaal=6.0):
        x, z = x / schaal, z / schaal
        x0, z0 = int(math.floor(x)), int(math.floor(z))
        fx, fz = x - x0, z - z0
        fx, fz = fx * fx * (3 - 2 * fx), fz * fz * (3 - 2 * fz)
        a, b = grid[z0 % 64][x0 % 64], grid[z0 % 64][(x0 + 1) % 64]
        c, d = grid[(z0 + 1) % 64][x0 % 64], grid[(z0 + 1) % 64][(x0 + 1) % 64]
        return (a + (b - a) * fx) * (1 - fz) + (c + (d - c) * fx) * fz
    return f


def draai(x, z, k):
    """(x, z) turned k quarter turns clockwise seen from above (what Rotation.CLOCKWISE_90 does around 0, 0)."""
    for _ in range(k % 4):
        x, z = -z, x
    return x, z


def draai_props(props, k):
    """The block state properties of a block that is turned k quarter turns clockwise."""
    if not props or k % 4 == 0:
        return dict(props or {})
    p = dict(props)
    if p.get("facing") in RICHTING:
        p["facing"] = RICHTING[(RICHTING.index(p["facing"]) + k) % 4]
    if p.get("axis") in ("x", "z") and k % 2:
        p["axis"] = "z" if p["axis"] == "x" else "x"
    if all(r in p for r in RICHTING):                         # fences, walls, panes
        for i, r in enumerate(RICHTING):
            p[RICHTING[(i + k) % 4]] = props[r]
    if "rotation" in p:
        p["rotation"] = str((int(p["rotation"]) + 4 * k) % 16)
    return p


class Sub:
    """A small build in its own coordinates (its front to the south), to stamp into a Bouw turned any way."""

    def __init__(self):
        self.blocks = {}

    def set(self, x, y, z, name, props=None, nbt=None):
        self.blocks[(x, y, z)] = (mc(name), dict(props or {}), nbt)

    def trap(self, x, y, z, facing, name, om=False):
        self.set(x, y, z, name, {"facing": facing, "half": "top" if om else "bottom", "shape": "straight", "waterlogged": "false"})

    def plaat(self, x, y, z, name, boven=False):
        self.set(x, y, z, name, {"type": "top" if boven else "bottom", "waterlogged": "false"})


class Bouw:
    """A template under construction: the Structure, its ground level and what the self-check wants to know."""

    def __init__(self, h, naam, maat, g, seed):
        self.h = h
        self.naam = naam
        self.s = h.Structure(maat)
        self.W, self.H, self.D = maat
        self.G = g
        self.rng = random.Random(seed)
        self.npcs = []          # (x, y, z): where an NPC or a sitting guh sits
        self.vaten = []
        self.vrij = []          # (x, y, z, wat): spots that must stay free and reachable (a block a player clicks)

    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, mc(name), props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def leeg(self, x, y, z):
        return self.get(x, y, z) in (None, AIR)

    def grond(self, cx, cz, straal, top, onder):
        """The ground: a frayed disc, `top(x, z, d)` on layer G, `onder` below it down to layer 0, air above (the cave is
        cleared over the whole site)."""
        for x in range(self.W):
            for z in range(self.D):
                d = math.hypot(x - cx, z - cz)
                if d > straal + self.rng.uniform(-1.3, 1.0):
                    continue
                self.set(x, self.G, z, top(x, z, d))
                for y in range(0, self.G):
                    self.set(x, y, z, onder)
                for y in range(self.G + 1, self.H):
                    self.set(x, y, z, AIR)

    def paal(self, x, z, y0, y1, name, props=None):
        for y in range(y0, y1 + 1):
            self.set(x, y, z, name, props)

    def hek(self, x, y, z, name="spruce_fence"):
        self.set(x, y, z, name, FENCE)

    def lamp(self, x, y, z, hangend=False):
        self.set(x, y, z, "lantern", {"hanging": "true" if hangend else "false", "waterlogged": "false"})

    def lantaarnpaal(self, x, z, hoog=3, name=MUUR):
        y0 = self.G + 1
        self.paal(x, z, y0, y0 + hoog - 1, name, WALL if name == MUUR else FENCE)
        self.lamp(x, y0 + hoog, z)

    def plaat(self, x, y, z, boven=False, name="spruce_slab"):
        self.set(x, y, z, name, {"type": "top" if boven else "bottom", "waterlogged": "false"})

    def trap(self, x, y, z, facing, name="spruce_stairs", om=False):
        self.set(x, y, z, name, {"facing": facing, "half": "top" if om else "bottom", "shape": "straight", "waterlogged": "false"})

    def luik(self, x, y, z, facing="north", boven=False, name="spruce_trapdoor"):
        self.set(x, y, z, name, {"facing": facing, "half": "top" if boven else "bottom", "open": "false", "powered": "false",
                                 "waterlogged": "false"})

    def vat(self, x, y, z, loot=None, facing="up"):
        nbt = {"id": "minecraft:barrel"}
        if loot:
            nbt["LootTable"] = loot
            self.vaten.append((x, y, z))
        self.set(x, y, z, "barrel", {"facing": facing, "open": "false"}, nbt)

    def pot(self, x, y, z, facing="south"):
        self.set(x, y, z, "decorated_pot", {"facing": facing, "cracked": "false", "waterlogged": "false"}, {"id": "minecraft:decorated_pot"})

    def bord(self, x, y, z, regels, rotation=None, facing=None):
        """A waxed spruce sign: standing (rotation 0..15, 0 = its text faces south) or on a wall (facing = the side its text
        faces)."""
        import sign_text
        B = self.h.ms.Byte
        prefix = "sign.guhs.camping_markt"
        nbt = {"id": "minecraft:sign", "is_waxed": B(1),
               "front_text": {"messages": sign_text.messages(prefix, regels), "color": "black", "has_glowing_text": B(0)},
               "back_text": {"messages": sign_text.messages(prefix, ["", "", "", ""]), "color": "black", "has_glowing_text": B(0)}}
        if facing:
            self.set(x, y, z, "spruce_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
        else:
            self.set(x, y, z, "spruce_sign", {"rotation": str(rotation or 0), "waterlogged": "false"}, nbt)

    def stempel(self, sub, ox, oy, oz, k=0):
        """Puts a Sub in: its (0, 0, 0) at (ox, oy, oz), turned k quarter turns clockwise around that corner."""
        for (x, y, z), (name, props, nbt) in sub.blocks.items():
            dx, dz = draai(x, z, k)
            self.set(ox + dx, oy + y, oz + dz, name, draai_props(props, k), nbt)

    def midden(self, x, z):
        """The centre jigsaw, in layer 0 (see the module text): what the structure type puts on the cave floor."""
        self.s.set(x, 0, z, "minecraft:jigsaw", {"orientation": "up_north"},
                   {"id": "minecraft:jigsaw", "name": f"guhs:{self.naam}_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
                    "final_state": HOUTSKOOL, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})


# =====================================================================================================================
# tents: real canvas (stairs and slabs), the door to the south
# =====================================================================================================================
def tent(kleur, h=2, lang=4, rand=None, inhoud=True, luifel=False):
    """An A-frame tent, 2h + 1 wide (x) and `lang` deep: z 0 is the closed back, the door is at z lang - 1 (south). The
    two slopes are stairs of tent canvas, the ridge a slab; `rand` = a second colour for the first and the last hoop and
    the back. inhoud: sleeping bags and a lantern. luifel: an awning on two poles in front of the door (two more rows)."""
    t = Sub()
    rand = rand or kleur
    for z in range(lang):
        k = rand if z in (0, lang - 1) else kleur
        for y in range(h):
            t.trap(y, y, z, "east", doektrap(k))
            t.trap(2 * h - y, y, z, "west", doektrap(k))
            for x in range(y + 1, 2 * h - y):
                t.set(x, y, z, AIR)
        t.plaat(h, h, z, doekplaat(k))
    for y in range(h):                                        # the back: closed
        for x in range(y + 1, 2 * h - y):
            t.set(x, y, 0, doek(rand))
    if h >= 3:                                                # a big tent: only the middle of the front is the door
        zf = lang - 1
        t.set(h, h - 1, zf, doek(rand))
        for y in range(h - 1):
            for x in range(y + 1, 2 * h - y):
                if abs(x - h) > 1:
                    t.set(x, y, zf, doek(rand))
    if inhoud:
        binnen = [x for x in range(1, 2 * h)]
        for z in range(1, lang - 1):                          # a ground sheet
            for x in binnen:
                t.set(x, 0, z, "minecraft:white_carpet" if (x + z) % 2 else "minecraft:pink_carpet")
        t.set(2, 0, 1, SLAAPZAK, {"facing": "north", "occupied": "false"})    # (under the ridge: room to sit up)
        if h >= 3:
            t.set(4, 0, 1, SLAAPZAK, {"facing": "north", "occupied": "false"})
            t.set(3, 0, 1, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
        else:
            t.set(3, 0, 1, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    if luifel:
        for x in range(1, 2 * h):
            t.plaat(x, h - 1, lang, doekplaat(rand), boven=True)
            t.plaat(x, h - 1, lang + 1, doekplaat(kleur), boven=True)
        for x in (1, 2 * h - 1):
            for y in range(h - 1):
                t.set(x, y, lang + 1, "minecraft:spruce_fence", FENCE)
    return t


def plek_leeg():
    """The empty pitch: its sign; air wherever a tent can stand (Herstel puts this back, air and all)."""
    t = Sub()
    for x in range(PLEK_MAAT[0]):
        for y in range(PLEK_MAAT[1]):
            for z in range(PLEK_MAAT[2]):
                t.set(x, y, z, AIR)
    t.set(*PLEK_BORD, BORD, {"facing": "south", "bezet": "false"})
    return t


def plek_tent():
    """The same box with a pitched tent: a little yellow tent with a creme trim, its four pegs still loose, the sign turned."""
    t = plek_leeg()
    for (x, y, z), b in tent("geel", 2, 4, rand="creme").blocks.items():
        t.blocks[(x + 1, y, z)] = b
    for x in (0, 6):
        for z in (0, 3):
            t.set(x, 0, z, HARING, {"vast": "false"})
    t.set(*PLEK_BORD, BORD, {"facing": "south", "bezet": "true"})
    return t


def plek_hoek(bord_x, bord_z, k):
    """The corner (template 0, 0, 0 of the small templates) of the pitch whose sign stands at (bord_x, bord_z)."""
    dx, dz = draai(PLEK_BORD[0], PLEK_BORD[2], k)
    return bord_x - dx, bord_z - dz


def plek_cellen(bord_x, bord_z, k):
    """Every (x, z) of a pitch."""
    ox, oz = plek_hoek(bord_x, bord_z, k)
    return [(ox + draai(x, z, k)[0], oz + draai(x, z, k)[1]) for x in range(PLEK_MAAT[0]) for z in range(PLEK_MAAT[2])]


def sateboom(b, x, z, hoog=5):
    """A little sate tree: a pale stem, a cap of meat with glowing onion lights under it."""
    y0 = b.G + 1
    b.paal(x, z, y0, y0 + hoog - 1, SATE_STAM, {"axis": "y"})
    top = y0 + hoog
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) == 2 and abs(dz) == 2:
                continue
            b.set(x + dx, top, z + dz, SATE_VLEES)
            if abs(dx) <= 1 and abs(dz) <= 1:
                b.set(x + dx, top + 1, z + dz, SATE_VLEES)
    b.set(x, top + 2, z, SATE_VLEES)
    for dx, dz in ((2, 0), (-2, 0), (0, 2), (0, -2)):
        b.set(x + dx, top - 1, z + dz, UIENLICHT)


def worstboom(b, x, z, hoog=4):
    """A worst tree: a dark red stem with a round cap of mustard."""
    y0 = b.G + 1
    b.paal(x, z, y0, y0 + hoog - 1, WORST_STAM, {"axis": "y"})
    top = y0 + hoog
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            b.set(x + dx, top, z + dz, MOSTERD_BLOK)
    b.set(x, top + 1, z, MOSTERD_BLOK)
    b.set(x + 1, top - 1, z, UIENLICHT)
    b.set(x - 1, top - 1, z, UIENLICHT)


# =====================================================================================================================
# the Grillcamping
# =====================================================================================================================
VUUR = (C, G + 1, C)                                          # the big camp fire
HAKBLOKKEN = [(31, G + 1, 19), (31, G + 1, 23)]
BANKEN = [(21, 18, "x"), (22, 18, "x"), (23, 18, "x"), (18, 21, "z"), (18, 22, "z"), (18, 23, "z"), (26, 21, "z"), (26, 22, "z"), (26, 23, "z")]


def camping(h):
    b = Bouw(h, CAMPING, CAMPING_MAAT, G, 31301)
    W, H, D = CAMPING_MAAT
    vlek = ruis(31302)
    plekcel, gazon = set(), set()
    for bx, bz, k in PLEKKEN:
        plekcel.update(plek_cellen(bx, bz, k))
        ox, oz = plek_hoek(bx, bz, k)
        gazon.update((ox + draai(x, z, k)[0], oz + draai(x, z, k)[1]) for x in range(PLEK_MAAT[0]) for z in range(PLEK_MAAT[2] - 1))

    def pad(x, z):
        """The paved path: from the gate to the fire circle, on to the reception, and a branch to the wood shed."""
        return (21 <= x <= 23 and 27 <= z <= 41) or (21 <= x <= 23 and 12 <= z <= 17) or (z in (21, 22) and 27 <= x <= 31)

    def top(x, z, d):
        n = vlek(x, z)
        if d <= 5.4:                                          # the fire circle
            return GEBEITELD if d <= 1.6 else (GEBARSTEN if (x * 3 + z * 5) % 5 == 0 else STENEN)
        if pad(x, z):
            return GEBARSTEN if (x * 5 + z * 3) % 4 == 0 else STENEN
        if (x, z) in gazon:
            return MOSTERD                                    # a pitch: a neat yellow lawn
        if (x, z) in plekcel:
            return PINDA
        if d > 19.4:
            return HOUTSKOOL if n > 0.5 else AS_AARDE         # the frayed rim: the cave floor
        if d > 17.8:
            return AS_AARDE if n > 0.5 else PINDA
        return MOSTERD if n > 0.8 else PINDA
    b.grond(C, C, 21.4, top, HOUTSKOOL)

    # --- the fire circle: the big camp fire in a ring of stones, log benches around it ---
    b.set(*VUUR, KAMPVUUR, {"brandt": "false"})
    b.vrij.append((*VUUR, "the camp fire"))
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if (dx or dz) and (dx == 0 or dz == 0):
                b.plaat(C + dx, G + 1, C + dz, name=PLAAT)
    for (x, z, as_) in BANKEN:
        b.set(x, G + 1, z, KAAL, {"axis": as_})
    for k in KAMPEERDERS:
        if k[5]:
            b.npcs.append(k[:3])
    for (x, z) in ((19, 19), (25, 19), (19, 25), (25, 25)):   # stumps on the diagonals
        b.set(x, G + 1, z, SATE_STAM, {"axis": "y"})
    for (x, z) in ((20, 26), (24, 26)):                       # two lamps where the path meets the circle
        b.lantaarnpaal(x, z, 2)

    # --- the gate (south): two log posts, a beam, the name board, pennants ---
    gz = 41
    for x in (19, 25):
        b.paal(x, gz, G + 1, G + 5, STAM, {"axis": "y"})
        b.lamp(x, G + 6, gz)
    for x in range(18, 27):
        if x not in (19, 25):
            b.set(x, G + 5, gz, STAM, {"axis": "x"})
    for x in (21, 22, 23):
        b.set(x, G + 4, gz, PLANK)
    b.hek(20, G + 4, gz)
    b.hek(24, G + 4, gz)
    b.bord(21, G + 4, gz + 1, ["~~~~~~~~", "Grill-", "camping", "~~~~~~~~"], facing="south")
    b.bord(22, G + 4, gz + 1, ["De", "Gloeiende", "Guh", "njeg!"], facing="south")
    b.bord(23, G + 4, gz + 1, ["Tentje mee?", "Kom erbij!", "(vuur is", "er al)"], facing="south")
    for i, x in enumerate((18, 20, 22, 24, 26)):              # pennants on the beam
        b.set(x, G + 6, gz, "minecraft:pink_carpet" if i % 2 else "minecraft:yellow_carpet")
    b.hek(18, G + 4, gz)
    b.hek(26, G + 4, gz)
    b.bord(20, G + 1, 39, ["Kampregels:", "1. Njeg zeggen", "2. Hout delen", "3. Vads slapen"], rotation=14)

    # --- the reception (north): a chalet with a steep roof, an open front with the desk, a porch ---
    x0, x1, z0, z1 = 18, 26, 5, 10                            # the room: x 18..26, z 5..10; the front (south) is open
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 2):
            b.set(x, G, z, PLANK)                             # the floor, and the porch at z1 + 1
    for z in range(z0, z1 + 1):                               # the side walls: logs with a window each
        for x in (x0, x1):
            for y in range(G + 1, G + 4):
                b.set(x, y, z, STAM if z in (z0, z1) else PLANK, {"axis": "y"} if z in (z0, z1) else None)
            b.set(x, G + 4, z, STAM, {"axis": "z"})           # the wall plate, under the roof
        for x in (x0, x1):
            b.set(x, G + 2, z0 + 2, "glass_pane", {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})
            b.set(x, G + 2, z0 + 3, "glass_pane", {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})
    for x in range(x0 + 1, x1):                               # the back wall
        for y in range(G + 1, G + 4):
            b.set(x, y, z0, PLANK)
    # the roof: the ridge runs north-south, the slopes are dark stairs that reach over the walls and the porch
    half = (x1 - x0) // 2 + 1                                 # 5: x0 - 1 .. x1 + 1 is 11 wide
    for z in range(z0 - 1, z1 + 3):
        dak = "dark_oak_stairs" if z in (z0 - 1, z1 + 2) else "spruce_stairs"       # (a dark trim on both ends)
        for i in range(half):
            y = G + 4 + i
            b.trap(x0 - 1 + i, y, z, "east", dak)
            b.trap(x1 + 1 - i, y, z, "west", dak)
        b.plaat(C, G + 4 + half, z, name="dark_oak_slab")
    for y in (G + 9, G + 10):                                 # a chimney on the ridge, with a glowing cap
        b.set(C, y, z0 + 1, STENEN)
    b.set(C, G + 11, z0 + 1, "guhs:gloeikool")
    for z in (z0, z1):                                        # the gables: planks up to the roof, a round window in front
        for i in range(1, half + 1):
            for x in range(x0 + i, x1 - i + 1):
                b.set(x, G + 3 + i, z, PLANK if z == z0 else DONKER)
    b.set(C, G + 6, z1, "glass_pane", {"north": "false", "south": "false", "east": "true", "west": "true", "waterlogged": "false"})
    b.bord(C, G + 5, z1 + 1, ["Receptie", "Kampbaas-guh", "(bel 1x,", "niet 10x)"], facing="south")
    for x in (x0, x1):                                        # the porch posts
        b.paal(x, z1 + 2, G + 1, G + 3, "spruce_fence", FENCE)
    # the desk across the open front: a counter with a gap to walk in, the bell on it
    for x in range(x0 + 1, x1):
        if x != x1 - 1:
            b.trap(x, G + 1, z1, "north", om=True)
    b.set(x0 + 2, G + 2, z1, "bell", {"attachment": "floor", "facing": "north", "powered": "false"}, {"id": "minecraft:bell"})
    b.pot(x1 - 3, G + 2, z1, "south")
    # inside: his bed, a shelf of keys, a map table, a barrel of marshmallows
    b.set(x0 + 1, G + 1, z0 + 1, "pink_bed", {"facing": "west", "part": "head", "occupied": "false"}, {"id": "minecraft:bed"})
    b.set(x0 + 2, G + 1, z0 + 1, "pink_bed", {"facing": "west", "part": "foot", "occupied": "false"}, {"id": "minecraft:bed"})
    b.set(x1 - 1, G + 1, z0 + 1, "cartography_table")
    b.vat(x1 - 1, G + 1, z0 + 2, "guhs:chests/campingmarkt_camping")
    b.vat(x1 - 1, G + 2, z0 + 2, facing="west")
    b.set(x1 - 2, G + 1, z0 + 1, "bookshelf")
    b.set(C, G + 1, z0 + 1, "smoker", {"facing": "south", "lit": "false"}, {"id": "minecraft:smoker"})
    b.lamp(C, G + 4, z0 + 3, hangend=True)
    b.set(C, G + 5, z0 + 3, "chain", {"axis": "y", "waterlogged": "false"})
    b.set(C, G + 6, z0 + 3, "chain", {"axis": "y", "waterlogged": "false"})
    b.set(C, G + 7, z0 + 3, "chain", {"axis": "y", "waterlogged": "false"})
    b.set(C, G + 8, z0 + 3, "chain", {"axis": "y", "waterlogged": "false"})
    b.set(x0 + 4, G + 1, z0 + 1, "barrel", {"facing": "up", "open": "false"}, {"id": "minecraft:barrel"})
    # the Kampbaas-guh's stool behind the desk
    kx, ky, kz = KAMPBAAS
    b.set(kx, ky - 1, kz, KAAL, {"axis": "y"})
    b.npcs.append(KAMPBAAS)
    b.set(x0 + 1, G + 1, z1 + 1, "minecraft:potted_crimson_fungus")
    b.set(x1 - 1, G + 1, z1 + 1, "minecraft:potted_warped_fungus")
    # the flag pole east of the reception: a tall pole with a pink and white flag
    fx, fz = 29, 9
    b.set(fx, G, fz, GEBEITELD)
    b.paal(fx, fz, G + 1, G + 9, "spruce_fence", FENCE)
    for dy, breed in ((9, 3), (8, 3), (7, 2)):
        for i in range(1, breed + 1):
            b.set(fx + i, G + dy, fz, "minecraft:pink_wool" if (dy + i) % 2 else "minecraft:white_wool")

    # --- the tents of the residents ---
    b.stempel(tent("blauw", 2, 4, rand="creme"), 9, G + 1, 11)                 # north-west: x 9..13, z 11..14, the door south
    b.stempel(tent("rood", 2, 4, rand="creme"), 31, G + 1, 11)                 # north-east: x 31..35, z 11..14
    b.stempel(tent("groen", 3, 5, rand="geel", luifel=True), 5, G + 1, 25, 3)  # west: the family tent, its door (and awning) east
    for (x, z) in ((11, 16), (33, 16)):                       # a little cooking fire in front of the two small tents
        b.set(x, G + 1, z, "campfire", {"lit": "true", "facing": "north", "signal_fire": "false", "waterlogged": "false"},
              {"id": "minecraft:campfire"})
    b.set(13, G + 1, 16, KAAL, {"axis": "x"})
    b.set(35, G + 1, 16, KAAL, {"axis": "x"})
    b.vat(36, G + 1, 13)
    b.lamp(36, G + 2, 13)
    b.vat(8, G + 1, 13)
    b.set(8, G + 2, 13, "minecraft:potted_crimson_fungus")
    # the washing line between the blue tent and the family tent: carpets of every colour on a line of fences
    for x in (6, 10):
        b.paal(x, 17, G + 1, G + 2, "spruce_fence", FENCE)
    for i, x in enumerate(range(6, 11)):
        b.hek(x, G + 3, 17)
        if x not in (6, 10):
            b.set(x, G + 4, 17, ("minecraft:pink_carpet", "minecraft:white_carpet", "minecraft:light_blue_carpet")[i % 3])
    # the picnic table between the family tent and the fire
    for x in (13, 14, 15):
        b.plaat(x, G + 1, 27, boven=True)
        b.trap(x, G + 1, 26, "south")
        b.trap(x, G + 1, 28, "north")
    b.set(14, G + 2, 27, "minecraft:cake", {"bites": "2"})
    b.lamp(13, G + 2, 27)

    # --- the Houthakker-guh's corner (east): the wood shed, the chopping blocks, a saw horse ---
    sx0, sx1, sz0, sz1 = 35, 39, 18, 24
    for (x, z) in ((sx0, sz0), (sx0, sz1), (sx1, sz0), (sx1, sz1)):
        b.paal(x, z, G + 1, G + 3, STAM, {"axis": "y"})
    for z in range(sz0 - 1, sz1 + 2):                         # a lean-to roof: high in front (west), low at the back
        dak = "dark_oak_slab" if z in (sz0 - 1, sz1 + 1) else "spruce_slab"
        b.plaat(sx0 - 1, G + 4, z, boven=True, name=dak)
        b.plaat(sx0, G + 4, z, boven=True, name=dak)
        b.plaat(sx0 + 1, G + 4, z, name=dak)
        b.plaat(sx0 + 2, G + 4, z, name=dak)
        b.plaat(sx0 + 3, G + 3, z, boven=True, name=dak)
        b.plaat(sx1, G + 3, z, boven=True, name=dak)
        b.plaat(sx1 + 1, G + 3, z, boven=True, name=dak)
    for z in (sz0, sz1):                                      # (the posts carry it)
        b.set(sx0, G + 4, z, STAM, {"axis": "y"})
        b.set(sx1, G + 3, z, STAM, {"axis": "y"})
        b.set(sx0 + 2, G + 3, z, STAM, {"axis": "x"})
        b.set(sx0 + 1, G + 3, z, STAM, {"axis": "x"})
        b.set(sx0 + 3, G + 3, z, STAM, {"axis": "x"})
    for z in range(sz0 + 1, sz1):                             # the stacks: three kinds of wood, end grain to the front
        soort = (STAM, SATE_STAM, WORST_STAM)[(z - sz0) % 3]
        for x in range(sx0 + 1, sx1):
            hoog = 3 if x >= sx0 + 2 else 2
            for y in range(G + 1, G + 1 + hoog):
                b.set(x, y, z, soort, {"axis": "x"})
    for (x, y, z) in HAKBLOKKEN:
        b.set(x, y, z, HAKBLOK, {"stam": "true"})
        b.vrij.append((x, y, z, "a chopping block"))
    hx, hy, hz = HOUTHAKKER
    b.set(hx, hy - 1, hz, WORST_STAM, {"axis": "y"})
    b.npcs.append(HOUTHAKKER)
    b.hek(33, G + 1, 25)                                      # the saw horse: two trestles and a log on them
    b.hek(31, G + 1, 25)
    b.set(32, G + 2, 25, SATE_STAM, {"axis": "x"})
    b.set(33, G + 2, 25, SATE_STAM, {"axis": "x"})
    b.set(31, G + 2, 25, SATE_STAM, {"axis": "x"})
    b.set(33, G + 1, 17, STAM, {"axis": "x"})                 # loose logs and split wood lying about
    b.set(34, G + 1, 17, STAM, {"axis": "x"})
    b.set(30, G + 1, 25, "minecraft:spruce_button", {"face": "floor", "facing": "north", "powered": "false"})
    b.bord(34, G + 1, 26, ["Houthakker-guh", "Hakken? Graag!", "(vingers", "tellen na afloop)"], rotation=3)
    b.lantaarnpaal(34, 27, 2)
    worstboom(b, 38, 27, 4)

    # --- the four free pitches: a yellow lawn each, a stone under every peg, the sign ---
    leeg = plek_leeg()
    for bx, bz, k in PLEKKEN:
        ox, oz = plek_hoek(bx, bz, k)
        b.stempel(leeg, ox, G + 1, oz, k)
        for px in (0, 6):
            for pz in (0, 3):
                dx, dz = draai(px, pz, k)
                b.set(ox + dx, G, oz + dz, GEBEITELD)
        b.vrij.append((bx, G + 1, bz, "the sign of a pitch"))

    # --- trees, plants, lamps ---
    sateboom(b, 7, 28, 5)
    worstboom(b, 15, 8, 4)
    for (x, z) in ((6, 14), (38, 15), (9, 33), (35, 33), (15, 38), (29, 38), (28, 14), (16, 14)):
        if b.get(x, G, z) not in (None, AIR, STENEN, GEBARSTEN) and b.leeg(x, G + 1, z) and (x, z) not in plekcel:
            b.lantaarnpaal(x, z, 2, name="spruce_fence")
    planten = ("guhs:sate_zwammetje", "guhs:worst_zwammetje", "guhs:pindascheutjes", "guhs:mosterdscheutjes")
    for _ in range(70):
        x, z = b.rng.randrange(2, W - 2), b.rng.randrange(2, D - 2)
        if b.get(x, G, z) in (PINDA, MOSTERD) and b.leeg(x, G + 1, z) and (x, z) not in plekcel and math.hypot(x - C, z - C) > 7 \
                and all(b.leeg(x + dx, G + 1, z + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1)):
            soort = planten[b.rng.randrange(4)]
            if (soort.endswith("pindascheutjes") and b.get(x, G, z) != PINDA) or (soort.endswith("mosterdscheutjes") and b.get(x, G, z) != MOSTERD):
                soort = planten[0]
            b.set(x, G + 1, z, soort)
    b.midden(C, C)
    return b


# =====================================================================================================================
# the Nether-Mika-ruilmarkt
# =====================================================================================================================
SCHAAL = (C, G + 2, C)                                        # the big scales, on a plinth in the middle of the Waag
STAPELS = [(20 + i, G + 2, 19) for i in range(5)]             # the five stacks of vads, on the counter at the back (north)
# a stall: (x, z of the middle of its counter, quarter turns (0 = the counter to the south), awning colours, what lies on it)
KRAMEN = [
    (22, 8, 0, ("rood", "creme"), "sate"), (14, 12, 0, ("geel", "creme"), "potten"), (30, 12, 0, ("groen", "creme"), "lampen"),
    (9, 22, 3, ("blauw", "creme"), "vads"), (35, 22, 1, ("rood", "geel"), "kleden"),
    (14, 32, 2, ("groen", "geel"), "mosterd"), (30, 32, 2, ("blauw", "creme"), "kolen"),
]
KRAAM_BALIE = (3, 2)                                          # the middle of the counter in the stall's own coordinates
KRAAM_STAAT = (3, 1)                                          # where the stall holder stands


def kraam_hoek(cx, cz, k):
    dx, dz = draai(KRAAM_BALIE[0], KRAAM_BALIE[1], k)
    return cx - dx, cz - dz


def kraam(b_rng, kleuren, waar):
    """A market stall, 7 wide and 3 deep, its counter to the south (z 2): four posts, a sloping striped awning, a counter
    of barrels and boards with the goods on it, crates at the back."""
    t = Sub()
    a, c = kleuren
    for (x, z) in ((0, 0), (6, 0), (0, 2), (6, 2)):
        for y in range(3):
            t.set(x, y, z, "minecraft:spruce_fence", FENCE)
    for x in range(7):                                        # the awning: high at the back, a valance in front
        k = a if x % 2 == 0 else c
        t.set(x, 3, 0, doek(k))
        t.trap(x, 3, 1, "north", doektrap(k))
        t.plaat(x, 3, 2, doekplaat(k))
        t.plaat(x, 2, 3, doekplaat(k), boven=True)
    for x in range(1, 6):                                     # the counter
        if x in (1, 5):
            t.set(x, 0, 2, "minecraft:barrel", {"facing": "up", "open": "false"}, {"id": "minecraft:barrel"})
        else:
            t.trap(x, 0, 2, "north", "minecraft:spruce_stairs", om=True)
    goederen = {
        "sate": [SATE_VLEES, "minecraft:potted_crimson_fungus", SATE_VLEES, "guhs:sate_zwammetje", UIENLICHT],
        "potten": ["minecraft:decorated_pot", "minecraft:flower_pot", "minecraft:decorated_pot", "minecraft:flower_pot", "minecraft:decorated_pot"],
        "lampen": ["minecraft:lantern", UIENLICHT, "minecraft:soul_lantern", "minecraft:lantern", UIENLICHT],
        "vads": ["minecraft:purple_candle", "minecraft:amethyst_block", "minecraft:purple_candle", "minecraft:amethyst_block", "minecraft:purple_candle"],
        "kleden": ["minecraft:pink_wool", "minecraft:yellow_carpet", "minecraft:light_blue_wool", "minecraft:pink_carpet", "minecraft:white_wool"],
        "mosterd": [MOSTERD_BLOK, "guhs:worst_zwammetje", MOSTERD_BLOK, "minecraft:potted_warped_fungus", MOSTERD_BLOK],
        "kolen": ["guhs:gloeikool", "guhs:grillkool", "guhs:gloeikool", "guhs:grillkool", "guhs:gloeikool"],
    }[waar]
    for i, x in enumerate(range(1, 6)):
        g = goederen[i]
        if g == "minecraft:decorated_pot":
            t.set(x, 1, 2, g, {"facing": "south", "cracked": "false", "waterlogged": "false"}, {"id": "minecraft:decorated_pot"})
        elif g.endswith("lantern"):
            t.set(x, 1, 2, g, {"hanging": "false", "waterlogged": "false"})
        elif g.endswith("candle"):
            t.set(x, 1, 2, g, {"candles": "3", "lit": "true", "waterlogged": "false"})
        elif x in (1, 5) or not g.endswith(("zwammetje",)):
            t.set(x, 1, 2, g)
        else:
            t.set(x, 1, 2, "minecraft:flower_pot")
    # the back: crates and a second shelf
    t.set(1, 0, 0, "minecraft:barrel", {"facing": "south", "open": "false"}, {"id": "minecraft:barrel"})
    t.set(5, 0, 0, "minecraft:barrel", {"facing": "up", "open": "false"}, {"id": "minecraft:barrel"})
    t.set(5, 1, 0, "minecraft:barrel", {"facing": "south", "open": "false"}, {"id": "minecraft:barrel"})
    t.set(2, 0, 0, goederen[0] if not goederen[0].endswith(("pot", "lantern", "candle")) else "minecraft:spruce_planks")
    t.set(3, 2, 1, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    return t


def markt(h):
    b = Bouw(h, MARKT, MARKT_MAAT, G, 31311)
    W, H, D = MARKT_MAAT
    vlek = ruis(31312)

    def top(x, z, d):
        n = vlek(x, z)
        if d <= 17.2:                                         # the paved square: rings and spokes of lighter and iron stone
            if abs(x - C) <= 4 and abs(z - C) <= 4:
                return GEPOLIJST if (x + z) % 2 == 0 else GEBEITELD      # the floor of the Waag: a chequer board
            if abs(d - 8.0) < 0.6 or abs(d - 13.0) < 0.6:
                return GEBEITELD
            if (x == C or z == C) and d > 5:
                return ROOSTER
            return GEBARSTEN if (x * 5 + z * 3) % 6 == 0 or n > 0.82 else STENEN
        if 20 <= x <= 24 and z > C:
            return GEBARSTEN if (x + z) % 3 == 0 else STENEN  # the way in from the gate
        if d > 19.4:
            return HOUTSKOOL if n > 0.5 else AS_AARDE
        return AS_AARDE if n > 0.4 else AS
    b.grond(C, C, 21.4, top, HOUTSKOOL)

    # --- the Waag: four iron pillars, a striped tent roof, the scales, the stacks of vads ---
    for (x, z) in ((18, 18), (26, 18), (18, 26), (26, 26)):
        b.set(x, G + 1, z, GEBEITELD)
        b.paal(x, z, G + 2, G + 5, PILAAR, {"axis": "y"})
    for i in range(-4, 5):                                    # the beams from pillar to pillar
        for (x, z) in ((C + i, C - 4), (C + i, C + 4), (C - 4, C + i), (C + 4, C + i)):
            b.set(x, G + 6, z, ROOSTER)
    for ring in range(5):                                     # the roof: a pyramid of canvas stairs, red and creme by turns
        y = G + 6 + ring
        r = 5 - ring
        k = "rood" if ring % 2 == 0 else "creme"
        for i in range(-r, r + 1):
            b.trap(C + i, y, C - r, "south", doektrap(k))
            b.trap(C + i, y, C + r, "north", doektrap(k))
            b.trap(C - r, y, C + i, "east", doektrap(k))
            b.trap(C + r, y, C + i, "west", doektrap(k))
    b.set(C, G + 10, C, doek("creme"))
    b.set(C, G + 11, C, doek("rood"))
    b.hek(C, G + 12, C, "guhs:houtskoolsteen_stenen_hek")
    b.set(C, G + 13, C, UIENLICHT)
    for (x, z) in ((19, 18), (25, 18), (19, 26), (25, 26)):   # lamps under the beams
        b.lamp(x, G + 5, z, hangend=True)
    sx, sy, sz = SCHAAL
    b.set(sx, sy - 1, sz, GEBEITELD)
    b.set(sx, sy, sz, WEEGSCHAAL, {"facing": "south", "stand": "midden"})
    b.vrij.append((sx, sy, sz, "the scales"))
    for i, (x, y, z) in enumerate(STAPELS):                   # the counter with the five stacks
        b.set(x, y - 1, z, GEPOLIJST)
        b.set(x, y, z, VADSSTAPEL, {"facing": "south", "nummer": str(i + 1)})
        b.vrij.append((x, y, z, "a stack of vads"))
    b.set(19, G + 1, 19, GEPOLIJST)
    b.set(25, G + 1, 19, GEPOLIJST)
    b.bord(19, G + 2, 19, ["De Waag", "Echt vads", "is zwaar.", "Nep niet!"], rotation=0)
    b.vat(25, G + 2, 19, "guhs:chests/campingmarkt_markt")
    for z in (21, 22, 23):                                    # low iron rails at the sides
        for x in (18, 26):
            b.set(x, G + 1, z, "guhs:roosterijzer_tralies", {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})

    # --- the Marktmeester-Mika's rostrum, south of the Waag: three steps up, a bell ---
    mx, my, mz = MARKTMEESTER
    for x in (mx - 1, mx, mx + 1):
        b.set(x, my - 1, mz, GEBEITELD)
    b.trap(mx - 2, my - 1, mz, "east", TRAP)
    b.trap(mx + 2, my - 1, mz, "west", TRAP)
    b.npcs.append(MARKTMEESTER)
    b.hek(mx + 1, my, mz, "guhs:houtskoolsteen_stenen_hek")
    b.set(mx + 1, my + 1, mz, "bell", {"attachment": "floor", "facing": "south", "powered": "false"}, {"id": "minecraft:bell"})
    b.hek(mx - 1, my, mz, "guhs:houtskoolsteen_stenen_hek")
    b.lamp(mx - 1, my + 1, mz)
    b.bord(mx, my - 1, mz + 1, ["Marktmeester", "Afdingen mag.", "Nepvads", "mag NIET."], facing="south")

    # --- the stalls ---
    bezet = set()
    for (kx, kz, k, kleuren, waar) in KRAMEN:
        ox, oz = kraam_hoek(kx, kz, k)
        b.stempel(kraam(b.rng, kleuren, waar), ox, G + 1, oz, k)
        for x in range(7):
            for z in range(4):
                dx, dz = draai(x, z, k)
                bezet.add((ox + dx, oz + dz))
    # --- the gate (south): two iron pillars, a canvas banner between them ---
    gz = 41
    for x in (19, 25):
        b.set(x, G + 1, gz, GEBEITELD)
        b.paal(x, gz, G + 2, G + 5, PILAAR, {"axis": "y"})
        b.set(x, G + 6, gz, UIENLICHT)
    for x in range(20, 25):
        b.set(x, G + 5, gz, doek("rood" if x % 2 == 0 else "creme"))
        b.plaat(x, G + 6, gz, name=doekplaat("rood" if x % 2 == 0 else "creme"))
    b.bord(21, G + 5, gz + 1, ["~~~~~~~~", "Nether-Mika", "RUIL-", "~~~~~~~~"], facing="south")
    b.bord(22, G + 5, gz + 1, ["~~~~~~~~", "ruilmarkt", "MARKT", "~~~~~~~~"], facing="south")
    b.bord(23, G + 5, gz + 1, ["Vads erin,", "verrassing", "eruit.", "Mjauw!"], facing="south")
    # --- the notice board by the way in, lamp posts on the outer ring, hand carts and crates ---
    for x in (16, 17, 18):
        b.set(x, G + 2, 37, PLANK)
        b.set(x, G + 3, 37, PLANK)
    b.hek(16, G + 1, 37)
    b.hek(18, G + 1, 37)
    b.bord(16, G + 3, 38, ["GEZOCHT:", "wie betaalt", "er met", "nepvads?"], facing="south")
    b.bord(17, G + 3, 38, ["1 vads =", "1 verrassing", "(geen ruilen", "terug)"], facing="south")
    b.bord(18, G + 3, 38, ["Guhs welkom!", "Wel eerst", "afdingen", "leren, njeg"], facing="south")
    b.bord(17, G + 2, 38, ["Kraam huren?", "Vraag de", "Marktmeester", ""], facing="south")
    for hoek in range(8):
        a = math.radians(22.5 + hoek * 45)
        x, z = int(round(C + 13 * math.cos(a))), int(round(C + 13 * math.sin(a)))
        if (x, z) not in bezet and b.leeg(x, G + 1, z) and not (20 <= x <= 24):
            b.lantaarnpaal(x, z, 3)
    def kar(x, z, lading):
        """A hand cart: a tray on two wheels with two handles."""
        b.plaat(x, G + 1, z, boven=True)
        b.plaat(x + 1, G + 1, z, boven=True)
        b.set(x, G + 2, z, lading)
        b.luik(x + 2, G + 1, z, "west", boven=True)
        b.set(x, G + 1, z + 1, "minecraft:spruce_button", {"face": "wall", "facing": "south", "powered": "false"})
        b.set(x + 1, G + 1, z + 1, "minecraft:spruce_button", {"face": "wall", "facing": "south", "powered": "false"})
    kar(27, 15, MOSTERD_BLOK)
    kar(13, 27, SATE_VLEES)
    for (x, z, hoog) in ((14, 15, 2), (15, 15, 1), (30, 28, 2), (30, 29, 1), (29, 16, 1), (12, 26, 1), (10, 16, 1), (34, 17, 2)):
        if (x, z) in bezet:
            continue
        for y in range(hoog):
            b.vat(x, G + 1 + y, z, facing="up" if y == 0 else "south")
    for (x, z) in ((16, 16), (28, 28), (16, 28), (28, 16)):   # sacks of ash and coal between the stalls
        if (x, z) not in bezet and b.leeg(x, G + 1, z):
            b.set(x, G + 1, z, "guhs:grillkool" if (x + z) % 3 else AS)
    for _ in range(26):                                       # embers in the ash of the rim
        x, z = b.rng.randrange(2, W - 2), b.rng.randrange(2, D - 2)
        if b.get(x, G, z) in (AS, AS_AARDE, HOUTSKOOL) and b.leeg(x, G + 1, z) and math.hypot(x - C, z - C) > 18.5:
            b.set(x, G + 1, z, SMEUL)
    b.midden(C, C)
    return b


# =====================================================================================================================
# the geometry self-check
# =====================================================================================================================
OP_DE_GROND = ("minecraft:barrel", "minecraft:campfire", "minecraft:spruce_sign", "minecraft:cartography_table", "minecraft:pink_bed",
               "minecraft:decorated_pot", "minecraft:flower_pot", "minecraft:potted_crimson_fungus", "minecraft:potted_warped_fungus",
               "minecraft:cake", "minecraft:bell", "minecraft:smoker", "minecraft:bookshelf", "minecraft:purple_candle", SLAAPZAK, SMEUL,
               KAMPVUUR, HAKBLOK, BORD, HARING, WEEGSCHAAL, VADSSTAPEL, "guhs:sate_zwammetje", "guhs:worst_zwammetje", "guhs:pindascheutjes",
               "guhs:mosterdscheutjes", "minecraft:spruce_button")
GEEN_VLOER = (None, AIR, SMEUL, "minecraft:lantern", "minecraft:chain", "minecraft:spruce_sign", "minecraft:spruce_wall_sign",
              "guhs:sate_zwammetje", "guhs:worst_zwammetje", "guhs:pindascheutjes", "guhs:mosterdscheutjes", "minecraft:spruce_button")
ACHTER = {"north": (0, 1), "south": (0, -1), "east": (-1, 0), "west": (1, 0)}


def _is_tapijt(name):
    return name is not None and name.endswith("_carpet")


def check(b, naam):
    blocks = b.s.blocks
    problems = []

    def nm(c):
        v = blocks.get(c)
        return v[0] if v else None

    def vrij(c):
        return nm(c) in (None, AIR)

    vast = {c for c, v in blocks.items() if v[0] != AIR}
    # nothing floats: everything above the ground hangs together with the ground
    gezien = {c for c in vast if c[1] <= b.G}
    todo = list(gezien)
    zes = ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))
    schuin = tuple((dx, dy, dz) for dx in (-1, 0, 1) for dy in (-1, 1) for dz in (-1, 0, 1) if abs(dx) + abs(dz) == 1)
    while todo:
        x, y, z = todo.pop()
        # (a roof of stairs hangs together along its edges: every hoop rests half on the one below it)
        for d in zes + (schuin if nm((x, y, z)).endswith(("_trap", "_stairs")) else ()):
            n = (x + d[0], y + d[1], z + d[2])
            if n in vast and n not in gezien and (d in zes or nm(n).endswith(("_trap", "_stairs", "_plaat", "_slab"))):
                gezien.add(n)
                todo.append(n)
    for c in sorted(vast - gezien):
        problems.append(f"{naam}: floating {nm(c)} at {c}")
    for c, v in blocks.items():
        onder = nm((c[0], c[1] - 1, c[2]))
        wall_button = v[0] == "minecraft:spruce_button" and v[1].get("face") == "wall"
        staat = (v[0] in OP_DE_GROND and not wall_button) or (v[0] == "minecraft:lantern" and v[1].get("hanging") == "false") or _is_tapijt(v[0])
        if staat and (onder in GEEN_VLOER or _is_tapijt(onder)):
            problems.append(f"{naam}: {v[0]} at {c} stands on {onder}")
        if v[0] == "minecraft:lantern" and v[1].get("hanging") == "true" and vrij((c[0], c[1] + 1, c[2])):
            problems.append(f"{naam}: a hanging lantern at {c} hangs from nothing")
        if v[0] == "minecraft:spruce_wall_sign" or wall_button:
            dx, dz = ACHTER[v[1]["facing"]]
            if vrij((c[0] + dx, c[1], c[2] + dz)):
                problems.append(f"{naam}: the {v[0]} at {c} has no wall behind it")
        if v[0] == SLAAPZAK:
            dx, dz = STAP[TEGEN[v[1]["facing"]]]
            if not vrij((c[0], c[1] + 1, c[2])):
                problems.append(f"{naam}: no room above the sleeping bag at {c}")
            _ = dx, dz
    # every NPC and sitting guh sits on something, with room for its head
    for (x, y, z) in b.npcs:
        if vrij((x, y - 1, z)) or not vrij((x, y, z)) or not vrij((x, y + 1, z)):
            problems.append(f"{naam}: no place to sit at {(x, y, z)}")
    if not b.vaten:
        problems.append(f"{naam}: no loot barrel")
    if sum(1 for v in blocks.values() if v[0] == "minecraft:jigsaw" and v[2] and v[2].get("name") == f"guhs:{naam}_midden") != 1:
        problems.append(f"{naam}: exactly one centre jigsaw")
    if any(c[1] != 0 for c, v in blocks.items() if v[0] == "minecraft:jigsaw"):
        problems.append(f"{naam}: the centre jigsaw belongs in layer 0")
    # what a player has to click can be walked to from the gate (a flood over the walkable spots)
    def loopbaar(c):
        onder = nm((c[0], c[1] - 1, c[2]))
        return _door(nm(c)) and _door(nm((c[0], c[1] + 1, c[2]))) and onder not in GEEN_VLOER
    start = (C, b.G + 1, 42)
    bereik = {start} if loopbaar(start) else set()
    todo = list(bereik)
    while todo:
        x, y, z = todo.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1):
                n = (x + dx, y + dy, z + dz)
                if n not in bereik and b.s.inside(*n) and loopbaar(n):
                    bereik.add(n)
                    todo.append(n)
    if len(bereik) < 300:
        problems.append(f"{naam}: only {len(bereik)} walkable spots from the gate")
    for (x, y, z, wat) in b.vrij:
        if not any((x + dx, y + dy, z + dz) in bereik for dx in (-2, -1, 0, 1, 2) for dz in (-2, -1, 0, 1, 2) for dy in (-1, 0)
                   if abs(dx) + abs(dz) <= 2):
            problems.append(f"{naam}: {wat} at {(x, y, z)} can't be reached from the gate")
    for (x, y, z) in b.npcs:
        if not any((x + dx, y + dy, z + dz) in bereik for dx in (-2, -1, 0, 1, 2) for dz in (-2, -1, 0, 1, 2) for dy in (-1, 0)):
            problems.append(f"{naam}: who sits at {(x, y, z)} can't be reached from the gate")

    def tel(name):
        return sum(1 for v in blocks.values() if v[0] == name)
    if naam == CAMPING:
        if tel(KAMPVUUR) != 1:
            problems.append(f"{naam}: exactly one big camp fire")
        if tel(HAKBLOK) < 2:
            problems.append(f"{naam}: two chopping blocks, so nobody waits")
        if tel(BORD) != len(PLEKKEN) or len(PLEKKEN) < 3:
            problems.append(f"{naam}: {tel(BORD)} pitch signs for {len(PLEKKEN)} pitches (three or more)")
        # every pitch is free: nothing but its sign in the box of the small templates, ground under all of it
        gezien_cel = {}
        for i, (bx, bz, k) in enumerate(PLEKKEN):
            ox, oz = plek_hoek(bx, bz, k)
            for px in range(PLEK_MAAT[0]):
                for pz in range(PLEK_MAAT[2]):
                    dx, dz = draai(px, pz, k)
                    x, z = ox + dx, oz + dz
                    if (px, pz) not in ((0, 4), (6, 4), (1, 4), (5, 4)) and (x, z) in gezien_cel and gezien_cel[(x, z)] != i:
                        problems.append(f"{naam}: pitch {i} and pitch {gezien_cel[(x, z)]} overlap at {(x, z)}")
                    gezien_cel[(x, z)] = i
                    if vrij((x, b.G, z)):
                        problems.append(f"{naam}: pitch {i} has no ground at {(x, z)}")
                    for py in range(PLEK_MAAT[1]):
                        if (px, py, pz) != PLEK_BORD and not vrij((x, b.G + 1 + py, z)):
                            problems.append(f"{naam}: pitch {i} is not free at {(x, b.G + 1 + py, z)}: {nm((x, b.G + 1 + py, z))}")
        if tel(SLAAPZAK) < 3:
            problems.append(f"{naam}: the residents' tents have sleeping bags")
    else:
        if tel(WEEGSCHAAL) != 1:
            problems.append(f"{naam}: exactly one scales")
        nummers = sorted(v[1].get("nummer") for v in blocks.values() if v[0] == VADSSTAPEL)
        if nummers != ["1", "2", "3", "4", "5"]:
            problems.append(f"{naam}: the stacks of vads are numbered 1..5, not {nummers}")
        for (x, y, z, _) in KRAAM_MIKAS:
            if vrij((x, y - 1, z)) or not vrij((x, y, z)) or not vrij((x, y + 1, z)):
                problems.append(f"{naam}: the stall holder at {(x, y, z)} has no room")
    return problems


def _door(name):
    """Can a player stand in this block?"""
    return name in (None, AIR, SMEUL, BORD, HARING, "minecraft:spruce_sign", "minecraft:spruce_wall_sign", "minecraft:spruce_button",
                    "guhs:sate_zwammetje", "guhs:worst_zwammetje", "guhs:pindascheutjes", "guhs:mosterdscheutjes") or _is_tapijt(name)


# =====================================================================================================================
# saving
# =====================================================================================================================
def sub_structure(h, sub, maat):
    s = h.Structure(maat)
    for (x, y, z), (name, props, nbt) in sub.blocks.items():
        s.set(x, y, z, name, props, nbt)
    return s


def test_kamer(h):
    """CampingmarktGameTests: a floor of houtskoolsteen (helper y 1), 24 x 24, ten high."""
    kamer = h.Structure((24, 10, 24))
    for x in range(24):
        for z in range(24):
            kamer.set(x, 0, z, HOUTSKOOL)
    kamer.save("campingmarkt_test_kamer")


def kampeerder_nbt(h, i):
    """The entity data of resident i of the camping: a guh in camping clothes, with its Bezetting tag."""
    x, y, z, variant, schaal, zit, yaw, hoofd, nek, rug = KAMPEERDERS[i]
    ms = h.ms
    extra = {"Variant": variant, "Rotation": ms.floats(float(yaw), 0.0), "PersistenceRequired": ms.Byte(1), "Invulnerable": ms.Byte(1),
             "NeoForgeData": {"guhs_bezetting": f"campingmarkt_kampeerder_{i}"}}
    if zit:
        extra["Sitting"] = ms.Byte(1)
    if hoofd:
        extra["ClothesHead"] = hoofd
    if nek:
        extra["ClothesNeck"] = nek
    if rug:
        extra["ClothesBack"] = rug
    return ms.guh_nbt(schaal, **extra)


def kraam_mika_nbt(h, i):
    """The entity data of stall holder i of the market: a Nether-Mika that stays behind its counter."""
    x, y, z, yaw = KRAAM_MIKAS[i]
    ms = h.ms
    return {"id": "guhs:nether_mika", "Rotation": ms.floats(float(yaw), 0.0), "PersistenceRequired": ms.Byte(1), "Invulnerable": ms.Byte(1),
            "NeoForgeData": {"guhs_bezetting": f"campingmarkt_kraam_{i}"}}


def build_all(h, bewaar=True):
    """Builds, checks and saves both templates (with their NPCs and residents), the two pitch templates and the test room;
    returns (problems, {name: Bouw})."""
    from features import wereld
    bouwsels = {CAMPING: camping(h), MARKT: markt(h)}
    problems = []
    for naam, b in bouwsels.items():
        problems += check(b, naam)
    c, m = bouwsels[CAMPING], bouwsels[MARKT]
    x, y, z = KAMPBAAS
    c.s.entity(x + 0.5, y, z + 0.5, wereld.npc(h, "kampbaasguh", "campingmarkt_kampbaas", yaw=KAMPBAAS_YAW))
    x, y, z = HOUTHAKKER
    c.s.entity(x + 0.5, y, z + 0.5, wereld.npc(h, "houthakkerguh", "campingmarkt_houthakker", yaw=HOUTHAKKER_YAW))
    for i, k in enumerate(KAMPEERDERS):
        if k[5] and (c.leeg(k[0], k[1] - 1, k[2]) or not c.leeg(k[0], k[1], k[2])):
            problems.append(f"{CAMPING}: resident {i} has nothing to sit on at {k[:3]}")
        if not k[5] and (c.leeg(k[0], k[1] - 1, k[2]) or not c.leeg(k[0], k[1], k[2]) or not c.leeg(k[0], k[1] + 1, k[2])):
            problems.append(f"{CAMPING}: resident {i} has no room at {k[:3]}")
        c.s.entity(k[0] + 0.5, float(k[1]), k[2] + 0.5, kampeerder_nbt(h, i))
    x, y, z = MARKTMEESTER
    m.s.entity(x + 0.5, y, z + 0.5, wereld.npc(h, "marktmeester_mika", "campingmarkt_marktmeester", yaw=MARKTMEESTER_YAW))
    for i, k in enumerate(KRAAM_MIKAS):
        m.s.entity(k[0] + 0.5, float(k[1]), k[2] + 0.5, kraam_mika_nbt(h, i))
    if not problems and bewaar:
        for naam, b in bouwsels.items():
            b.s.save(naam)
        sub_structure(h, plek_leeg(), PLEK_MAAT).save("campingmarkt_plek")
        sub_structure(h, plek_tent(), PLEK_MAAT).save("campingmarkt_tent")
        test_kamer(h)
    return problems, bouwsels


# =====================================================================================================================
# pictures without the game:  python tools/features/camping_markt_bouw.py <out dir>
# =====================================================================================================================
EIGEN_KLEUR = {
    KAMPVUUR: (120, 84, 50), HAKBLOK: (150, 108, 66), BORD: (210, 180, 120), HARING: (170, 170, 176), WEEGSCHAAL: (214, 176, 80),
    VADSSTAPEL: (150, 90, 176), SLAAPZAK: (240, 150, 190),
    **{doek(k): c for k, c in (("rood", (196, 64, 56)), ("geel", (232, 190, 70)), ("groen", (98, 140, 80)), ("blauw", (78, 126, 176)),
                               ("creme", (236, 226, 200)))},
}
for _k in KLEUREN:
    EIGEN_KLEUR[doektrap(_k)] = EIGEN_KLEUR[doek(_k)]
    EIGEN_KLEUR[doekplaat(_k)] = EIGEN_KLEUR[doek(_k)]


def _dozen(name, props):
    """The boxes (x0, y0, z0, x1, y1, z1 in 0..1) a block is drawn as."""
    n = name.split(":")[1]
    if n.endswith(("_trap", "_stairs")):
        om = props.get("half") == "top"
        laag = (0, 0.5, 0, 1, 1, 1) if om else (0, 0, 0, 1, 0.5, 1)
        y0, y1 = (0, 0.5) if om else (0.5, 1)
        f = props.get("facing", "north")
        hoog = {"north": (0, y0, 0, 1, y1, 0.5), "south": (0, y0, 0.5, 1, y1, 1), "east": (0.5, y0, 0, 1, y1, 1), "west": (0, y0, 0, 0.5, y1, 1)}[f]
        return [laag, hoog]
    if n.endswith(("_plaat", "_slab")):
        t = props.get("type", "bottom")
        return [(0, 0.5, 0, 1, 1, 1)] if t == "top" else [(0, 0, 0, 1, 1, 1)] if t == "double" else [(0, 0, 0, 1, 0.5, 1)]
    if n.endswith(("_fence", "_hek", "_muur", "_wall")):
        return [(0.36, 0, 0.36, 0.64, 1, 0.64)]
    if n.endswith(("_carpet",)) or n == "guh_slaapzak":
        return [(0, 0, 0, 1, 0.12 if n.endswith("_carpet") else 0.3, 1)]
    if n.endswith("lantern"):
        return [(0.32, 0.4, 0.32, 0.68, 1, 0.68)] if props.get("hanging") == "true" else [(0.32, 0, 0.32, 0.68, 0.6, 0.68)]
    if n in ("chain", "roosterijzer_tralies", "glass_pane"):
        return [(0.44, 0, 0.44, 0.56, 1, 0.56)] if n == "chain" else [(0.44, 0, 0, 0.56, 1, 1)] if props.get("north") == "true" else [(0, 0, 0.44, 1, 1, 0.56)]
    if n.endswith("_trapdoor"):
        return [(0, 0.8, 0, 1, 1, 1)] if props.get("half") == "top" else [(0, 0, 0, 1, 0.2, 1)]
    if n.endswith(("_sign", "_button")) or n == "jigsaw":
        return []
    if n.endswith(("zwammetje", "scheutjes", "_fungus", "candle")) or n in ("smeulkooltjes", "flower_pot", "cake", "bell"):
        return [(0.3, 0, 0.3, 0.7, 0.55, 0.7)]
    if n == "campingmarkt_kampvuur":
        return [(0.05, 0, 0.05, 0.95, 0.45, 0.95)]
    if n == "campingmarkt_hakblok":
        return [(0.1, 0, 0.1, 0.9, 0.55, 0.9), (0.3, 0.55, 0.3, 0.7, 1, 0.7)]
    if n == "campingmarkt_kampeerplek":
        return [(0.44, 0, 0.44, 0.56, 0.8, 0.56), (0.2, 0.5, 0.42, 0.8, 0.95, 0.58)]
    if n == "campingmarkt_haring":
        return [(0.42, 0, 0.42, 0.58, 0.4, 0.58)]
    if n == "campingmarkt_weegschaal":
        return [(0.42, 0, 0.42, 0.58, 0.9, 0.58), (0.0, 0.8, 0.44, 1, 0.9, 0.56), (0.0, 0.3, 0.3, 0.3, 0.4, 0.7), (0.7, 0.3, 0.3, 1, 0.4, 0.7)]
    if n == "campingmarkt_vadsstapel":
        return [(0.1, 0, 0.2, 0.9, 0.3, 0.8), (0.2, 0.3, 0.25, 0.8, 0.55, 0.75)]
    if n in ("decorated_pot", "barrel"):
        return [(0.06, 0, 0.06, 0.94, 1, 0.94)]
    if n.endswith("_bed"):
        return [(0, 0, 0, 1, 0.56, 1)]
    return [(0, 0, 0, 1, 1, 1)]


def teken(struct, px=16, guhs=True):
    """An isometric picture seen from the north-west corner (turn the structure first for another side); stairs, slabs,
    fences and the slice's own blocks are drawn in their real shape, entities as a coloured peg."""
    import wiki_renders as wr
    from PIL import Image, ImageDraw
    W, H, D = struct.size
    a, bb, c = px, px * 0.5, px
    w = int((W + D) * a + 2 * px)
    hh = int((W + D) * bb + H * c + 2 * px)
    img = Image.new("RGBA", (w, hh), (24, 18, 20, 255))
    draw = ImageDraw.Draw(img, "RGBA")
    ox, oy = D * a + px, (W + D) * bb + H * c + px

    def P(x, y, z):
        return (ox + (x - z) * a, oy - (x + z) * bb - y * c)

    items = []
    for (x, y, z), (name, props, _) in struct.blocks.items():
        if name in (AIR, "minecraft:jigsaw"):
            continue
        kleur = EIGEN_KLEUR.get(name) or wr.block_colour(name)
        if kleur is None:
            continue
        for d in _dozen(name, props):
            items.append((x + z + d[0] + d[2], y + d[1], (x, y, z), d, kleur))
    for (x, y, z, nbt) in struct.entities:
        soort = nbt["id"]
        kleur = (240, 140, 180) if soort == "guhs:guh" else (150, 100, 190) if soort == "guhs:nether_mika" else (250, 220, 120)
        if not guhs:
            continue
        items.append((x - 0.5 + z - 0.5 + 0.3 + 0.3, y, (x - 0.5, y, z - 0.5), (0.25, 0, 0.25, 0.75, 0.9, 0.75), kleur))
    items.sort(key=lambda t: (-t[0], t[1]))
    for _, _, (x, y, z), (x0, y0, z0, x1, y1, z1), kleur in items:
        top = [P(x + x0, y + y1, z + z0), P(x + x1, y + y1, z + z0), P(x + x1, y + y1, z + z1), P(x + x0, y + y1, z + z1)]
        left = [P(x + x0, y + y0, z + z0), P(x + x0, y + y1, z + z0), P(x + x0, y + y1, z + z1), P(x + x0, y + y0, z + z1)]
        front = [P(x + x0, y + y0, z + z0), P(x + x1, y + y0, z + z0), P(x + x1, y + y1, z + z0), P(x + x0, y + y1, z + z0)]
        draw.polygon(top, fill=kleur + (255,))
        draw.polygon(left, fill=tuple(int(v * 0.70) for v in kleur) + (255,))
        draw.polygon(front, fill=tuple(int(v * 0.85) for v in kleur) + (255,))
    return img


def gedraaid(struct, kwart, ms):
    """The structure turned `kwart` quarter turns clockwise (blocks, their properties and the entities)."""
    for _ in range(kwart % 4):
        W, H, D = struct.size
        t = ms.Structure((D, H, W))
        for (x, y, z), (name, props, nbt) in struct.blocks.items():
            t.blocks[(D - 1 - z, y, x)] = (name, draai_props(props, 1), nbt)
        t.entities = [(D - z, y, x, nbt) for x, y, z, nbt in struct.entities]
        struct = t
    return struct


def plattegrond(struct, lagen=3, px=16):
    """A floor plan: the ground layer and what stands on it (the first `lagen` layers above the ground, smaller and smaller)."""
    import wiki_renders as wr
    from PIL import Image
    W, H, D = struct.size
    plan = Image.new("RGB", (W * px, D * px), (20, 16, 18))
    for (x, y, z), b in sorted(struct.blocks.items(), key=lambda t: t[0][1]):
        if b[0] in (AIR, "minecraft:jigsaw") or y < G or y > G + lagen:
            continue
        c = EIGEN_KLEUR.get(b[0]) or wr.block_colour(b[0])
        if c:
            k = 1.0 if y == G else 0.85
            i = (y - G) * 2
            plan.paste(tuple(int(v * k) for v in c), (x * px + i, z * px + i, x * px + px - i, z * px + px - i))
    return plan


def preview(out):
    import types
    import make_structures as ms
    import wiki_renders as wr
    wr.SPECIAL_COLOURS["minecraft:lantern"] = (255, 214, 120)
    wr.SPECIAL_COLOURS["minecraft:campfire"] = (240, 150, 50)
    wr.SPECIAL_COLOURS["minecraft:glass_pane"] = (190, 220, 235)
    wr.SPECIAL_COLOURS["minecraft:bell"] = (240, 200, 70)
    wr.SPECIAL_COLOURS["minecraft:chain"] = (70, 74, 90)
    wr.SPECIAL_COLOURS["guhs:houtskoolsteen_stenen_hek"] = (57, 47, 47)
    wr.SPECIAL_COLOURS["guhs:smeulkooltjes"] = (230, 120, 40)
    for naam, kleur in (("potted_crimson_fungus", (150, 40, 50)), ("potted_warped_fungus", (30, 150, 140)), ("flower_pot", (124, 68, 54)),
                        ("smoker", (86, 80, 72)), ("cake", (250, 240, 230)), ("decorated_pot", (150, 80, 60)), ("purple_candle", (150, 90, 190)),
                        ("soul_lantern", (120, 220, 230)), ("amethyst_block", (134, 98, 190)), ("pink_bed", (238, 141, 173))):
        wr.SPECIAL_COLOURS["minecraft:" + naam] = kleur
    wr.block_colour.cache_clear()
    os.makedirs(out, exist_ok=True)
    h = types.SimpleNamespace(Structure=ms.Structure, ms=ms, Byte=ms.Byte, floats=ms.floats)
    problems, bouwsels = build_all(h, bewaar=False)
    for p in problems:
        print("PROBLEM:", p)
    for naam, b in bouwsels.items():
        for k in range(4):
            teken(gedraaid(b.s, k, ms), 16).save(os.path.join(out, f"{naam}_{k}.png"))
        plattegrond(b.s).save(os.path.join(out, f"{naam}_plan.png"))
        print(naam, "size", b.s.size, "blocks", sum(1 for v in b.s.blocks.values() if v[0] != AIR), "entities", len(b.s.entities))
    for naam, sub in (("campingmarkt_plek", plek_leeg()), ("campingmarkt_tent", plek_tent()), ("tent_groot", tent("groen", 3, 5, rand="geel", luifel=True)),
                      ("kraam", kraam(None, ("rood", "creme"), "sate"))):
        maat = tuple(max(c[i] for c in sub.blocks) + 1 for i in range(3))
        s = sub_structure(h, sub, maat)
        for k in (0, 2):
            teken(gedraaid(s, k, ms), 40).save(os.path.join(out, f"{naam}_{k}.png"))


if __name__ == "__main__":
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
