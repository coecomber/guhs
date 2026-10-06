"""
bbq2 (ring-h5) - De Zwarte Roosterpoort (structure guhs:zwarte_roosterpoort, a guhs:burcht in tiles): one closed valley in the
rock of the Asdal, cut off by a black wall with a gate of grill grates, and behind it the tower of the Eye: a giant
one-eyed Mika head on a spire, staring down the valley for his lost snack.

The valley is the whole course of chapter 5, from the mouth you come in by (z 0) to the mouth behind the wall (z 95):

    z  0 - 10   het Kamp        the camp at the mouth: the first Rustvuurtje, Boromika, the provisions Smikagol sniffs at;
                                besides the mouth a tunnel through the cliff on either side (the valley may stand in solid
                                rock: three ways in, in three directions)
    z 11 - 18   de Uitkijkrug   a ridge of rock with a worn stair over it: on top you see the gate and the Eye for the first time
    z 20 - 41   het Asveld      ruins and toppled grates to hide behind while the gaze of the Eye sweeps the field (zone A)
    z 42 - 45   de Slakkenhut   a ruined hut with the third Rustvuurtje
    z 46 - 57   de Kale Vlakte  nothing to hide behind: stand still under the Elfenmanteltje when the gaze comes by (zone B)
    z 56 - 60   de Holte        a notch in the east cliff with the fourth Rustvuurtje, out of the Eye's sight
    z 62 - 69   de Schaduwlaan  the lane in the shadow of the wall: a curtain of smoke (the Lichtflesje blows it away), two
                                riders of the Nine on patrol, then het Wachthek with three Mika guards (only the ring gets you
                                past them) and behind it the fifth Rustvuurtje at het Roosterpoortje, the little side door
    z 70 - 77   de Muur         the wall with the Zwarte Roosterpoort between its two gate towers; the tunnel of the side door
    z 78 - 95   Achter de muur  the foot of the tower of the Eye, the last Rustvuurtje, the mouth towards the Frituurberg

PLEKKEN is the single source of every spot the Java side works with (feature/ringh5/Plekken.java has the same numbers; check()
fails the generator run when they differ, like features/bestaand_bouw.py). All coordinates count in the whole build.

  bouw(h)        -> (Bouw, problems)     everything; problems = what check() found
  check(b)       walkable from the camp to the far mouth, every rest fire reachable, every hiding place out of the Eye's
                 sight and the open stretches in it (rays through the blocks, like the game's own line of sight), no ladder
                 or sign on a seam, Plekken.java
  java_plekken() the text of the constants for Plekken.java (python tools/features/ring_h5_bouw.py --java)
  preview(out)   pictures (python tools/features/ring_h5_bouw.py <out>)
"""
import math
import os
import random
import re
import sys

try:
    from features import spiesburcht_burcht as sb
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import spiesburcht_burcht as sb

NAAM = "zwarte_roosterpoort"
SX, SY, SZ = 96, 64, 96
G = 6                          # the ground layer (template y): you stand at F
F = G + 1
# the anchor of the burcht (it stands on block 8 of its chunk): the middle of the valley floor. Seams: check()
ANKER = (48, G, 48)
SEED = 21302050

AIR = sb.AIR
ROTS = sb.HOUTSKOOL
STENEN, GEBARSTEN, GEBEITELD = sb.STENEN, sb.GEBARSTEN, sb.GEBEITELD
TRAP, PLAAT, MUUR, HEK = sb.TRAP, sb.PLAAT, sb.MUUR, sb.HEK
IJZER, PILAAR, GEPOLIJST, TRALIES = sb.ROOSTER, sb.PILAAR, sb.GEPOLIJST, sb.TRALIES
GLOEIKOOL, AS, AS_AARDE, SMEUL, UIENLICHT, ROOKGAT = sb.GLOEIKOOL, sb.AS, sb.AS_AARDE, sb.SMEUL, sb.UIENLICHT, sb.ROOKGAT
GRILLKOOL, MIKAKOP = sb.GRILLKOOL, sb.MIKAKOP
BOT = "guhs:verkoold_guhbot"
ROOSTER = "guhs:ringh5_poortrooster"           # the grate of the gate (a full block you look through)
VUUR = "guhs:ring_rustvuur"
ZWART = "minecraft:polished_blackstone_bricks"
ZWART_GLAD = "minecraft:polished_blackstone"
ZWART_TRAP = "minecraft:polished_blackstone_brick_stairs"
ZWART_PLAAT = "minecraft:polished_blackstone_brick_slab"
ZWART_MUUR = "minecraft:polished_blackstone_brick_wall"
BASALT = "minecraft:polished_basalt"
ROOD = "minecraft:red_nether_bricks"
TAND = "minecraft:quartz_block"
KETTING = "minecraft:chain"

# the wall and what stands on it
MUUR_Z = (70, 77)
MUUR_TOP = 22                  # the walkway on the wall (block y); a parapet of two on top
POORT_X = (40, 55)             # the opening of the gate
POORT_TOP = 20
TOREN_W, TOREN_O = (32, 39), (56, 63)          # the two gate towers ("de Tanden"), x from .. to
TOREN_Z = (67, 80)
TOREN_TOP = 25
# the tower of the Eye
OOG_C = (48, 85)               # its middle (x, z)
KOP_Y = (44, 54)               # the Mika head on top (y from .. to)
OOG_BLOK = (48, 48, 83)        # where the Eye floats (the entity's feet), in the socket of the head
OOG_KIJK = (48.5, 49.6, 82.1)  # where its sight starts (the front of the pupil)

# =====================================================================================================================
# PLEKKEN: every spot the Java side uses (feature/ringh5/Plekken.java must say the same; boxes are (min, max) inclusive)
# =====================================================================================================================
PLEKKEN = {
    "OOG": [OOG_BLOK],
    # the six Rustvuurtjes (the block itself), in the order you pass them
    "VUUR_KAMP": [(48, F, 6)], "VUUR_UITKIJK": [(54, F, 20)], "VUUR_SLAKKENHUT": [(13, F, 44)], "VUUR_HOLTE": [(85, F, 59)],
    "VUUR_POORTJE": [(19, F, 63)], "VUUR_ACHTER": [(20, F, 85)],
    # the camp
    "BOROMIKA": [(52, F, 5)], "PROVIAND": [(41, F, 9)], "SMIKAGOL_KAMP": [(42, F, 10)], "KAMP": [(40, F, 2), (58, F + 4, 10)],
    # the lookout on the ridge: the crest of the stair
    "UITKIJK": [(48, F + 3, 14)],
    # zone A: where a player counts as "in the field", and the line the gaze of the Eye runs up and down
    "ZONE_A": [(10, F - 1, 21), (60, F + 6, 41)],
    "BLIK_A": [(48, F, 24), (41, F, 26), (34, F, 30), (27, F, 34), (20, F, 38), (18, F, 39)],
    # its hiding places (a standing cell each): Smikagol runs from one to the next
    "SCHUIL_A": [(43, F, 23), (36, F, 27), (29, F, 31), (22, F, 35), (16, F, 39)],
    # zone B
    "ZONE_B": [(16, F - 1, 46), (83, F + 6, 56)],
    "BLIK_B": [(24, F, 49), (38, F, 50), (60, F, 51), (80, F, 54)],
    # where the Eye looks when nobody is in a zone (a slow round over the whole valley floor)
    "BLIK_RUST": [(30, F, 26), (66, F, 30), (70, F, 50), (26, F, 52)],
    # the valley in front of the wall: here a worn ring draws the Eye
    "DOMEIN": [(6, F - 2, 11), (89, F + 24, 69)],
    # the lane: the smoke (a box you are pushed out of until your Lichtflesje blew it away) and which way is "back"
    "ROOK": [(71, F, 62), (76, F + 4, 68)], "ROOK_TERUG": [(79, F, 65)],
    # the two riders: each the two ends of its round
    "RUITER_1": [(66, F, 63), (50, F, 63)], "RUITER_2": [(50, F, 64), (66, F, 64)],
    # het Wachthek: the three guards (they look east, down the lane) and the post up to which the ring had better stay off
    "WACHTER_1": [(26, F, 63)], "WACHTER_2": [(26, F, 68)], "WACHTER_3": [(23, F, 67)], "SCHEDELPAAL": [(33, F, 62)],
    # het Roosterpoortje: the bars a player sees until Guhdalf opened it (really air), the tunnel behind it, where the scene starts
    "DEUR": [(12, F, 70), (14, F + 2, 70)], "TUNNEL": [(12, F, 70), (14, F + 3, 77)], "VOOR_DEUR": [(10, F, 66), (16, F + 3, 69)],
    "DEUR_SCENE": [(13, F, 67)],
    # behind the wall: the end of the chapter
    "ACHTER": [(10, F, 79), (30, F + 4, 92)],
    # Smikagol's routes (standing cells): to the lookout, through the lane, to the door, to the far fire
    "ROUTE_UITKIJK": [(48, F, 10), (48, F + 3, 14)],
    "ROUTE_B": [(18, F, 47), (38, F, 49), (60, F, 50), (80, F, 55), (84, F, 58)],
    "ROUTE_LAAN": [(80, F, 62), (78, F, 65), (68, F, 66), (48, F, 66), (36, F, 64)],
    "ROUTE_HEK": [(33, F, 65), (24, F, 65), (20, F, 65)],
    "ROUTE_DEUR": [(16, F, 66), (13, F, 68)],
    "ROUTE_ACHTER": [(13, F, 72), (13, F, 79), (18, F, 83)],
    # the three ways in: the mouth and a tunnel through the cliff on either side of the camp (where they meet the edge of the build)
    "INGANGEN": [(48, F, 0), (0, F, 6), (95, F, 6)],
    # the whole build (for the test copy)
    "MAAT": [(SX, SY, SZ)], "ANKER": [ANKER],
}


def java_plekken():
    """The constants of feature/ringh5/Plekken.java, in the shape check() reads back."""
    regels = []
    for naam, punten in PLEKKEN.items():
        ps = ", ".join(f"new BlockPos({x}, {y}, {z})" for x, y, z in punten)
        if len(punten) == 1:
            regels.append(f"    public static final BlockPos {naam} = {ps};")
        else:
            regels.append(f"    public static final List<BlockPos> {naam} = List.of({ps});")
    regels.append(f"    public static final Vec3 OOG_KIJK = new Vec3({OOG_KIJK[0]}, {OOG_KIJK[1]}, {OOG_KIJK[2]});")
    return "\n".join(regels)


# =====================================================================================================================
# the rock: the valley, its cliffs and its roof
# =====================================================================================================================
def _ruis(seed, schaal):
    """Smooth value noise 0..1."""
    def g(i, j):
        return random.Random(seed * 7919 + i * 131 + j * 31337).random()

    def f(x, z=0.0):
        fx, fz = x / schaal, z / schaal
        i, j = math.floor(fx), math.floor(fz)
        tx, tz = fx - i, fz - j
        tx, tz = tx * tx * (3 - 2 * tx), tz * tz * (3 - 2 * tz)
        a = g(i, j) * (1 - tx) + g(i + 1, j) * tx
        c = g(i, j + 1) * (1 - tx) + g(i + 1, j + 1) * tx
        return a * (1 - tz) + c * tz
    return f


_RL, _RR, _RD, _RG, _RK = _ruis(SEED + 1, 9.0), _ruis(SEED + 2, 9.0), _ruis(SEED + 3, 11.0), _ruis(SEED + 4, 6.0), _ruis(SEED + 5, 3.0)


def _interp(v, punten):
    for (a, ya), (c, yc) in zip(punten, punten[1:]):
        if v <= c:
            t = max(0.0, (v - a) / (c - a))
            return ya + (yc - ya) * t
    return punten[-1][1]


def links(z):
    return 8 + int(round(3 * _RL(z)))


def rechts(z):
    return 87 - int(round(3 * _RR(z)))


INGANG = (36, 60)              # the mouth you come in by (x from .. to, z 0 .. 2)
UITGANG = (12, 28)             # the mouth behind the wall (z 93 .. 95)


def binnen(x, z):
    """Is this column part of the valley (open air above its floor)?"""
    if 3 <= z <= 92:
        return links(z) <= x <= rechts(z)
    if 0 <= z < 3:
        return INGANG[0] <= x <= INGANG[1]
    if 92 < z < SZ:
        return UITGANG[0] <= x <= UITGANG[1]
    return False


def dak(x, z):
    """The first block of the roof above this column (everything under it, down to the floor, is air)."""
    if z < 3:
        return int(round(17 - 6 * ((x - 48) / 12.0) ** 2))
    if z > 92:
        return int(round(15 - 5 * ((x - 20) / 8.0) ** 2))
    prof = _interp(z, [(3, 24), (10, 31), (20, 42), (40, 53), (60, 59), (92, 60)])
    d = prof - 13 * ((x - 48) / 40.0) ** 2 + (_RD(x, z) - 0.5) * 3
    if z > 88:
        d = min(d, 60 - (z - 88) * 6 + abs(x - 48) * 0.0) if abs(x - OOG_C[0]) > 12 else d   # (it comes down behind the tower)
    d = int(round(d))
    if abs(x - OOG_C[0]) <= 11 and 78 <= z <= 91:
        d = max(d, SY - 3)                              # (room for the ears of the Mika head)
    return max(F + 4, min(SY - 3, d))


def rug(x, z):
    """The lookout ridge: how much higher the ground is here (0: not on the ridge)."""
    if not 11 <= z <= 17:
        return 0
    vorm = (1, 2, 3, 4, 3, 2, 1)[z - 11]
    f = 0.8 + 0.7 * _RK(x * 1.7, 3.0)
    return max(1, int(round(vorm * f)))


def grond(x, z):
    """The y of the top block of the valley floor in this column."""
    h = G + rug(x, z)
    if 3 <= z <= 92:
        d = min(x - links(z), rechts(z) - x)
        if d < 2 and not (MUUR_Z[0] - 1 <= z <= MUUR_Z[1] + 1):
            h += 2 - d                                   # (a little scree along the cliffs)
    return h


class Poort(sb.Bouw):
    def __init__(self, h):
        super().__init__(h, (SX, SY, SZ), SEED)
        self.naam = NAAM
        self.schuil = []            # the cells of every hiding place (for the sight check)
        self.open = []              # cells that must be IN the Eye's sight

    def grondblok(self, x, z):
        r = _RG(x, z)
        if r < 0.3:
            return ROTS
        if r > 0.82 and not (20 <= z <= 69 and 14 <= x <= 84):
            return AS                # (ash you sink into: only off the course)
        return AS_AARDE

    def vlak(self, x0, z0, x1, z1, blok=None, y=G):
        """Flat ground here (and air above it up to the roof)."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                for yy in range(0, y + 1):
                    self.set(x, yy, z, ROTS)
                self.set(x, y, z, blok or self.grondblok(x, z))
                for yy in range(y + 1, min(dak(x, z), y + 12)):
                    self.set(x, yy, z, AIR)


def terrein(b):
    """The rock around the valley (a shell: what lies further out stays what the cave was), its floor, its air, its roof."""
    mantel = {}
    for x in range(SX):
        for z in range(SZ):
            if binnen(x, z):
                continue
            # how far is the valley? (the shell is 4-7 thick; further out nothing is set)
            beste = 99
            for dx in range(-7, 8):
                for dz in range(-7, 8):
                    if binnen(x + dx, z + dz):
                        beste = min(beste, max(abs(dx), abs(dz)))
            if beste <= 4 + int(3 * _RD(x * 2.0, z * 2.0)):
                mantel[(x, z)] = beste
    for x in range(SX):
        for z in range(SZ):
            if binnen(x, z):
                top, d = grond(x, z), dak(x, z)
                for y in range(0, top):
                    b.set(x, y, z, ROTS)
                b.set(x, top, z, b.grondblok(x, z) if top == G else ROTS)
                for y in range(top + 1, d):
                    b.set(x, y, z, AIR)
                for y in range(d, min(SY, d + 3 + int(2 * _RD(x * 3.0, z * 3.0)))):
                    b.set(x, y, z, ROTS)
            elif (x, z) in mantel:
                # the cliff: as high as the roof of the valley next to it, rounded off on the outside
                hoog = 0
                for dx in range(-7, 8):
                    for dz in range(-7, 8):
                        if binnen(x + dx, z + dz):
                            hoog = max(hoog, dak(x + dx, z + dz))
                hoog = min(SY - 1, hoog + 3 - mantel[(x, z)] // 2)
                for y in range(0, hoog + 1):
                    b.set(x, y, z, ROTS)
    # jagged teeth of rock on the crest of the ridge (the camp can't see the valley, the valley can't see the camp)
    for x in range(8, 88):
        if not binnen(x, 14) or 44 <= x <= 52:
            continue
        top = grond(x, 14)
        extra = int(1 + 4 * _RK(x * 0.9, 9.0) ** 2 + (2 if x % 7 == 3 else 0))
        for z, e in ((13, extra - 1), (14, extra), (15, extra - 1)):
            for y in range(grond(x, z) + 1, grond(x, z) + 1 + max(0, e)):
                b.set(x, y, z, ROTS)
        _ = top


# =====================================================================================================================
# the camp and the ridge
# =====================================================================================================================
def _bord(b, x, y, z, rotatie, regels, wand=None):
    import sign_text
    B = b.h.Byte
    leeg = sign_text.messages("sign.guhs.ring_h5", ["", "", "", ""])
    nbt = {"id": "minecraft:sign", "is_waxed": B(1),
           "front_text": {"messages": sign_text.messages("sign.guhs.ring_h5", regels), "color": "orange", "has_glowing_text": B(1)},
           "back_text": {"messages": leeg, "color": "black", "has_glowing_text": B(0)}}
    if wand:
        b.set(x, y, z, "minecraft:crimson_wall_sign", {"facing": wand, "waterlogged": "false"}, nbt)
    else:
        b.set(x, y, z, "minecraft:crimson_sign", {"rotation": str(rotatie), "waterlogged": "false"}, nbt)


def _vuurplek(b, x, z, y=F):
    """A Rustvuurtje on a ring of cracked stones."""
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            b.set(x + dx, y - 1, z + dz, GEBARSTEN if (dx + dz) % 2 else STENEN)
    b.set(x, y, z, VUUR)


def _stam(b, x, y, z, as_, lengte, soort="guhs:sate_stam"):
    for i in range(lengte):
        b.set(x + (i if as_ == "x" else 0), y, z + (i if as_ == "z" else 0), soort, {"axis": as_})


def kamp(b, h):
    vx, _, vz = PLEKKEN["VUUR_KAMP"][0]
    _vuurplek(b, vx, vz)
    _stam(b, vx - 4, F, vz - 1, "z", 3)                    # (the space south and east of the fire stays free: the scene plays there)
    # Boromika's corner: his round shield against a rock, a horn on a post
    bx, _, bz = PLEKKEN["BOROMIKA"][0]
    for dx, dy in ((1, 0), (1, 1), (2, 0), (2, 1), (3, 0)):
        b.set(bx + dx + 1, F + dy, bz - 2, ROTS)
    b.set(bx + 2, F + 1, bz - 1, "minecraft:polished_blackstone_button", {"face": "wall", "facing": "south", "powered": "false"})
    # Sam-guh's kitchen: a cauldron on the stones and a crate of knabbels
    b.set(vx + 5, F, vz + 3, "minecraft:cauldron")
    b.set(vx + 6, F, vz + 3, "guhs:block_of_kaasknabbels")
    # a lean-to with two sleeping places
    for x in (43, 46):
        for y in (F, F + 1):
            b.set(x, y, 3, "guhs:sate_stam", {"axis": "y"})
    for x in range(42, 48):
        b.slab(x, F + 2, 3, "bottom")
        b.slab(x, F + 2, 4, "bottom")
        b.slab(x, F + 1, 5, "top")
    b.set(44, F, 4, "minecraft:green_carpet")
    b.set(45, F, 4, "minecraft:red_carpet")
    # the provisions Smikagol can't keep his paws off: sacks and a barrel behind a rock
    px, _, pz = PLEKKEN["PROVIAND"][0]
    b.set(px, F, pz, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.set(px - 1, F, pz, "guhs:block_of_kaasknabbels")
    b.set(px, F + 1, pz, "guhs:sate_vlees")
    b.set(px - 1, F, pz - 1, "minecraft:hay_block", {"axis": "y"})
    for dy in range(2):
        b.set(px - 2, F + dy, pz, ROTS)
    b.set(px - 2, F, pz + 1, ROTS)
    # the signs at the mouth
    _bord(b, 50, F, 2, 8, ["Zwarte Roosterpoort", "400 guhstappen", "Verboden voor guhs", "(en voor ringen)"])
    _bord(b, 46, F, 2, 8, ["Het Oog ziet alles.", "Behalve rotsen.", "Rotsen vindt hij", "saai. - Araguh"])
    b.fence(56, F, 8)
    b.fence(56, F + 1, 8)
    b.lantern(56, F + 2, 8)
    b.must_reach["the camp fire"] = (vx + 1, F, vz + 1)
    # Boromika himself (a cast character of chapter 5: there from the moment the chapter begins)
    from features import ring
    b.s.entity(bx + 0.5, F, bz + 0.5, ring.cast(h, "boromika", "ringh5_boromika", "ring_h5", 0, 99, plek="ringh5_kamp", yaw=120.0))


def zijgangen(b):
    """A tunnel through the cliff on either side of the camp, out to the edge of the build: the valley may come to stand in
    solid rock with its mouth against a wall, and then one of these two may be the side a cave comes by (outside the build
    a player digs; inside it nobody can)."""
    for west in (True, False):
        xs = range(0, links(6) + 3) if west else range(rechts(6) - 2, SX)
        for x in xs:
            for z in (5, 6, 7):
                for y in range(0, G):
                    b.set(x, y, z, ROTS)
                b.set(x, G, z, STENEN if z == 6 else b.grondblok(x, z))
                for y in range(F, F + 4 - (1 if z != 6 and x % 5 == 2 else 0)):
                    b.set(x, y, z, AIR)
            # rock around it where the cliff is thin
            for z in (4, 8):
                for y in range(0, F + 5):
                    if b.get(x, y, z) is None:
                        b.set(x, y, z, ROTS)
            for z in (5, 6, 7):
                for y in (F + 4, F + 5):
                    if b.get(x, y, z) is None:
                        b.set(x, y, z, ROTS)
        binnen_x = links(6) + 3 if west else rechts(6) - 3
        b.fence(binnen_x, F, 4)
        b.fence(binnen_x, F + 1, 4)
        b.lantern(binnen_x, F + 2, 4)
        _bord(b, binnen_x + (1 if west else -1), F, 4, 0, ["Kamp van de", "Reisgenoten.", "Vuurtje brandt.", "Kom binnen, njeg!"])
        b.must_reach["the way in on the " + ("west" if west else "east")] = (0 if west else SX - 1, F, 6)


def uitkijk(b):
    """The worn stair over the ridge (three wide, x 47..49) and the Rustvuurtje at its foot on the valley side."""
    for x in range(45, 52):
        for z in range(11, 19):
            trede = (1, 2, 3, 3, 3, 3, 2, 1)[z - 11]
            rand = x in (45, 51)
            for y in range(0, G + trede + (2 if rand else 0)):
                b.set(x, y, z, ROTS)
            for y in range(G + trede + (2 if rand else 0), G + 12):
                if b.get(x, y, z) != AIR and y < dak(x, z):
                    b.set(x, y, z, AIR)
            if rand:
                continue
            y = G + trede
            if z in (11, 12, 13):
                b.stair(x, y, z, "south")
            elif z in (16, 17, 18):
                b.stair(x, y, z, "north")
            else:
                b.set(x, y, z, GEBARSTEN if (x + z) % 3 == 0 else STENEN)
    # two crooked posts with a soul-less lantern on the crest: the frame of the first look at the Eye
    for x in (46, 50):
        for y in range(G + 4, G + 7):
            b.wall(x, y, 14)
    b.lantern(46, G + 7, 14)
    b.set(50, G + 7, 14, MIKAKOP, {"facing": "north"})
    ux, uy, uz = PLEKKEN["UITKIJK"][0]
    b.must_reach["the lookout"] = (ux, uy, uz)
    # the second fire: under a lean-to against the ridge, on the valley side (its roof reaches out towards the Eye)
    vx, _, vz = PLEKKEN["VUUR_UITKIJK"][0]
    b.vlak(vx - 2, vz - 1, vx + 3, vz + 3)
    _vuurplek(b, vx, vz)
    for x in range(vx - 2, vx + 4):
        for z in range(vz - 2, vz + 4):
            if z == vz - 2:
                b.set(x, F + 3, z, ROTS)
                b.set(x, F + 4, z, ROTS)
            else:
                b.slab(x, F + 3, z, "top" if z < vz + 3 else "bottom")
    for x in (vx - 2, vx + 3):
        for y in range(F, F + 3):
            b.set(x, y, vz + 3, "guhs:sate_stam", {"axis": "y"})
            b.set(x, y, vz - 2, ROTS)
    _stam(b, vx + 2, F, vz + 1, "z", 2)
    _bord(b, vx - 2, F, vz + 2, 14, ["Hier begint", "het Asveld.", "Zie je de gloed?", "Wegduiken, njeg!"])
    b.must_reach["the lookout fire"] = (vx + 1, F, vz)


# =====================================================================================================================
# the old road to the gate
# =====================================================================================================================
def rooster_in_vloer(b, x0, z0, x1, z1):
    """A grate in the floor with glowing coal under it."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G, z, ROOSTER)
            b.set(x, G - 1, z, GLOEIKOOL)


def weg(b):
    """The road the Mika's march along: cracked paving from the foot of the ridge straight to the gate, stumps of pillars
    beside it (low: they hide nobody), grates in the floor that glow."""
    for z in range(19, MUUR_Z[0] - 1):
        for x in range(46, 51):
            r = b.rng.random()
            if x in (46, 50) and r < 0.35:
                continue                                          # (the edges crumbled away)
            b.set(x, G, z, GEBARSTEN if r < 0.4 else ZWART_GLAD if (x == 48 and z % 4 == 0) else STENEN)
            for y in range(G + 1, G + 5):
                if b.get(x, y, z) not in (AIR, None):
                    b.set(x, y, z, AIR)
    for z in (24, 34, 44, 58):
        rooster_in_vloer(b, 47, z, 49, z + 1)
    # watch fires of the Mika's along the road (low: one block of blackstone, a glowing coal in an iron basket)
    for z in (28, 40, 52, 64):
        for x in (45, 51):
            b.set(x, F, z, ZWART_GLAD)
            b.set(x, F + 1, z, GLOEIKOOL if (x + z) % 2 else ROOSTER)
            if (x + z) % 2 == 0:
                b.set(x, F, z, GLOEIKOOL)
    for i, z in enumerate(range(22, MUUR_Z[0] - 6, 6)):
        for x in (44, 52):
            hoog = 1 if 44 <= z <= 58 else (2, 1, 3, 1, 2)[(i + (x == 52)) % 5]
            for y in range(F, F + hoog):
                b.set(x, y, z, ZWART if y == F else b.brick(0.5))
            if hoog == 3:
                b.set(x, F + 3, z, MIKAKOP, {"facing": "north"})


# =====================================================================================================================
# zone A: het Asveld, with its hiding places
# =====================================================================================================================
def schuilplek(b, x, z, soort):
    """A hiding place for the standing cells (x, z) and (x + 1, z): a wall on the Eye's side (z + 1) and a roof over it.
    Three looks: 0 a ruined wall with a slab roof, 1 a toppled grate of the gate on two iron posts, 2 a split boulder."""
    b.vlak(x - 2, z - 2, x + 3, z + 2)
    if soort == 0:
        for xx in range(x - 1, x + 4):
            hoog = 4 if x <= xx <= x + 2 else 3 if xx == x + 3 else 2
            for y in range(F, F + hoog):
                b.set(xx, y, z + 1, b.brick(0.3))
        for xx in range(x - 1, x + 3):
            b.set(xx, F + 3, z, STENEN if xx != x - 1 else GEBARSTEN)
            b.slab(xx, F + 3, z - 1, "top")
        b.set(x + 3, F, z, GEBARSTEN)
        b.stair(x + 3, F + 1, z, "west")
        b.set(x - 2, F, z + 1, GEBARSTEN)
    elif soort == 1:
        for xx in (x - 1, x + 2):
            for y in range(F, F + 3):
                b.set(xx, y, z + 1, PILAAR, {"axis": "y"})
        for xx in range(x - 1, x + 3):
            for y in range(F, F + 4):
                if b.get(xx, y, z + 1) in (AIR, None) or y == F + 3:
                    b.set(xx, y, z + 1, ROOSTER)
            b.set(xx, F + 3, z, ROOSTER)
            b.set(xx, F + 3, z - 1, ROOSTER)
        b.set(x + 3, F, z + 1, GEPOLIJST)
        b.set(x + 3, F + 1, z + 1, ROOSTER)
        b.set(x - 1, F, z - 1, PILAAR, {"axis": "y"})
        b.set(x - 1, F + 1, z - 1, PILAAR, {"axis": "y"})
        b.set(x - 1, F + 2, z - 1, PILAAR, {"axis": "y"})
    else:
        for xx in range(x - 2, x + 4):
            hoog = (2, 4, 5, 5, 4, 2)[xx - x + 2]
            for y in range(F, F + hoog):
                b.set(xx, y, z + 1, ROTS)
                if y >= F + 3 or xx in (x - 2, x + 3):
                    b.set(xx, y, z + 2, ROTS)
            if hoog >= 4:
                b.set(xx, F + 3, z, ROTS)
                if hoog == 5:
                    b.set(xx, F + 3, z - 1, ROTS)
                    b.set(xx, F + 4, z, ROTS)
        b.set(x + 3, F, z, ROTS)
    b.schuil += [(x, F, z), (x + 1, F, z)]


def asveld(b):
    for i, (x, _, z) in enumerate(PLEKKEN["SCHUIL_A"]):
        schuilplek(b, x, z, (0, 1, 2, 0, 1)[i])
        b.must_reach[f"hiding place {i + 1}"] = (x, F, z)
    # what is left of an older wall, and bones of whoever came here before: things that look like cover and are not
    for x, z, lengte in ((52, 30, 4), (44, 36, 3), (32, 38, 2), (25, 25, 3)):
        for i in range(lengte):
            b.set(x + i, F, z, b.brick(0.5))
    for x, z in ((38, 22), (30, 26), (53, 31), (19, 30), (26, 39), (54, 36), (35, 34)):
        b.set(x, F, z, SMEUL)
    for x, z, as_ in ((50, 25, "x"), (23, 29, "z"), (40, 33, "x")):
        b.set(x, F, z, BOT, {"axis": as_})
    # in between the hiding places you stand in the open
    for (x0, _, z0), (x1, _, z1) in zip(PLEKKEN["SCHUIL_A"], PLEKKEN["SCHUIL_A"][1:]):
        b.open.append(((x0 + x1) // 2, F, (z0 + z1) // 2 - 1))
    b.open.append((46, F, 21))


def aankleding(b):
    """What makes the valley a place: dunes of ash against the cliffs, rubble, a broken cart, cages, all off the course."""
    rng = random.Random(SEED + 31)
    # dunes: low mounds of ash in the corners of the fields (never on a line of the gaze or a route)
    bezet = set()
    for naam, punten in PLEKKEN.items():
        if naam.startswith(("BLIK_A", "BLIK_B", "ROUTE", "SCHUIL", "RUITER", "WACHTER")):
            for (x0, _, z0), (x1, _, z1) in zip(punten, punten[1:] + punten[-1:]):
                n = max(abs(x1 - x0), abs(z1 - z0), 1)
                for i in range(n + 1):
                    bezet.add((round(x0 + (x1 - x0) * i / n), round(z0 + (z1 - z0) * i / n)))

    def vrij(x, z, r):
        return all((x + dx, z + dz) not in bezet for dx in range(-r - 3, r + 4) for dz in range(-r - 3, r + 4))

    for cx, cz, r in ((66, 24, 4), (74, 34, 5), (64, 40, 3), (14, 26, 3), (72, 22, 3), (58, 44, 2), (30, 43, 2), (12, 54, 3), (40, 58, 2), (76, 44, 3)):
        if not vrij(cx, cz, r) or not all(binnen(cx + d, cz) and binnen(cx, cz + d) for d in (-r, r)):
            continue
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                hoog = int(round(1.6 - math.hypot(dx, dz) * 1.6 / r + rng.random() * 0.6))
                for y in range(F, F + max(0, min(2, hoog))):
                    if b.get(cx + dx, y, cz + dz) == AIR:
                        b.set(cx + dx, y, cz + dz, AS if y > F or rng.random() < 0.5 else AS_AARDE)
    # a broken cart of the Mika's beside the road (two wheels of grate, a bed of planks, a spilled load of grillkool)
    for x in range(54, 58):
        b.slab(x, F, 27, "top", block="minecraft:crimson_slab")
        b.slab(x, F, 28, "top", block="minecraft:crimson_slab")
    for x, z in ((54, 26), (57, 26), (54, 29)):
        b.set(x, F, z, ROOSTER)
    b.set(58, F, 28, GRILLKOOL)
    b.set(59, F, 27, GRILLKOOL)
    b.set(58, F, 26, "minecraft:crimson_fence", {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})
    # cages on posts along the east cliff: whoever came to the wrong door (empty: the guhs got away)
    for x, z in ((80, 30), (82, 38), (79, 46)):
        if not binnen(x + 2, z):
            continue
        for y in range(F, F + 3):
            b.set(x, y, z, PILAAR, {"axis": "y"})
        b.set(x, F + 3, z, ROOSTER)
        b.set(x, F + 4, z, MIKAKOP, {"facing": "west"})
    # rubble of the old wall, strewn over the far side of the plain
    for x, z in ((62, 24), (70, 28), (67, 37), (20, 24), (15, 31), (74, 40), (36, 44), (63, 57), (22, 56), (30, 58)):
        if vrij(x, z, 0) and b.get(x, F, z) == AIR:
            b.set(x, F, z, b.brick(0.6))


def slakkenhut(b):
    """A ruined hut against the west cliff with the third Rustvuurtje: three walls, half a roof, the Eye can't look in."""
    vx, _, vz = PLEKKEN["VUUR_SLAKKENHUT"][0]
    b.vlak(vx - 3, vz - 3, vx + 4, vz + 3, STENEN)
    for x in range(vx - 3, vx + 5):
        for y in range(F, F + 4):
            b.set(x, y, vz + 3, b.brick(0.3))                     # the wall towards the Eye
            if x >= vx + 3 and y >= F + 1 + (vx + 4 - x) * 2:
                b.set(x, y, vz + 3, AIR)                          # (its far end crumbled)
    for z in range(vz - 2, vz + 3):
        for y in range(F, F + 4):
            b.set(vx - 3, y, z, b.brick(0.3))
    for x in range(vx - 3, vx + 3):
        for z in range(vz - 1, vz + 4):
            b.set(x, F + 4, z, PLAAT, {"type": "bottom", "waterlogged": "false"})
    for x in range(vx - 3, vx + 3):
        b.set(x, F + 3, vz + 2, STENEN)
    _vuurplek(b, vx, vz)
    b.set(vx - 2, F, vz + 2, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.set(vx - 2, F, vz + 1, "guhs:orange_kussen", {"facing": "east"})
    b.hang_lantern(vx - 1, F + 4, vz, 1)
    _bord(b, vx + 3, F, vz - 2, 4, ["De Kale Vlakte.", "Niks om achter", "te zitten. Word", "zelf maar een rots."])
    b.must_reach["the fire of the Slakkenhut"] = (vx + 1, F, vz)
    b.schuil += [(vx - 1, F, vz + 1), (vx, F, vz + 1)]


# =====================================================================================================================
# zone B: de Kale Vlakte
# =====================================================================================================================
def vlakte(b):
    # bones in the ash (low: nothing here hides you) and embers
    for x, z, as_, n in ((28, 46, "x", 3), (47, 54, "z", 2), (66, 47, "x", 4), (74, 56, "x", 2), (35, 55, "x", 2)):
        for i in range(n):
            b.set(x + (i if as_ == "x" else 0), F, z + (i if as_ == "z" else 0), BOT, {"axis": as_})
    for x, z in ((24, 52), (33, 47), (44, 51), (52, 47), (58, 55), (69, 52), (77, 49), (41, 56)):
        b.set(x, F, z, SMEUL)
    b.set(54, F, 54, MIKAKOP, {"facing": "north"})
    for i, x in enumerate(range(60, 72, 3)):
        for z, y in ((46, F), (46, F + 1), (47, F + 2), (48, F + 2), (49, F + 1), (49, F)):
            b.set(x, y, z, BOT, {"axis": "y" if z in (46, 49) and y < F + 2 else "z"})
    for x, _, z in PLEKKEN["BLIK_B"]:
        b.open.append((x, F, z))
    b.open += [(28, F, 49), (49, F, 50), (70, F, 52)]


def holte(b):
    """A notch in the east cliff, out of the Eye's sight (the wall's shadow begins here): the fourth Rustvuurtje."""
    vx, _, vz = PLEKKEN["VUUR_HOLTE"][0]
    b.vlak(vx - 3, vz - 2, vx + 2, vz + 2, None)
    for x in range(vx - 6, vx + 5):
        for z in range(vz - 3, vz + 5):
            if not binnen(x, z):
                continue
            ver = (vx + 4 - x) + max(0, z - (vz + 2)) * 2
            if ver > 9:
                continue                                          # (the shelf is widest at the cliff)
            b.set(x, F + 4, z, ROTS)                              # an overhang of rock
            b.set(x, F + 5, z, ROTS)
            if ver < 6:
                b.set(x, F + 6, z, ROTS)
    for y in range(F, F + 4):
        b.set(vx - 3, y, vz - 3, ROTS)
        b.set(vx + 1, y, vz - 3, ROTS)
    _vuurplek(b, vx, vz)
    _stam(b, vx - 2, F, vz + 1, "x", 2)
    _bord(b, vx - 2, F, vz - 2, 2, ["De Schaduwlaan.", "Hier kijkt het Oog", "niet. De Negen", "kijken wel. Njeg."])
    b.must_reach["the fire of the Holte"] = (vx - 1, F, vz)
    b.schuil += [(vx - 1, F, vz), (vx, F, vz + 1)]


# =====================================================================================================================
# the wall, the gate and the two gate towers
# =====================================================================================================================
def muur(b):
    z0, z1 = MUUR_Z
    for x in range(4, 92):
        if not any(binnen(xx, 73) for xx in range(x - 4, x + 5)):
            continue
        for z in range(z0, z1 + 1):
            for y in range(0, MUUR_TOP + 1):
                kern = z0 < z < z1
                blok = ROTS if y < G else ZWART if y <= F + 1 else (STENEN if kern else b.brick(0.18))
                if y in (F + 8, MUUR_TOP - 1) and not kern:
                    blok = GEBEITELD                              # two bands of chiselled stone
                b.set(x, y, z, blok)
        # the parapet (solid, two high: the lane behind it lies in the wall's shadow) and merlons on top
        for z in (z0, z1):
            b.set(x, MUUR_TOP + 1, z, STENEN)
            b.set(x, MUUR_TOP + 2, z, STENEN if z == z0 else AIR)
            if x % 3 == 0:
                b.set(x, MUUR_TOP + 3, z0, GEBARSTEN if x % 9 == 0 else STENEN)
        for z in range(z0 + 1, z1):
            for y in range(MUUR_TOP + 1, MUUR_TOP + 6):
                if b.get(x, y, z) is not None and y < dak(x, z):
                    b.set(x, y, z, AIR)
        # buttresses on the valley side, a glowing slit between each pair
        if x % 8 == 4 and not (POORT_X[0] - 9 <= x <= POORT_X[1] + 9) and not (10 <= x <= 16):
            for y in range(F, MUUR_TOP - 3):
                b.set(x, y, z0 - 1, ZWART if y < F + 6 else STENEN)
            b.stair(x, MUUR_TOP - 3, z0 - 1, "south", block=TRAP)
        if x % 8 == 0 and not (POORT_X[0] - 9 <= x <= POORT_X[1] + 9) and not (10 <= x <= 16):
            b.set(x, F + 11, z0, GLOEIKOOL)
            b.set(x, F + 12, z0, GLOEIKOOL)
        # a hanging cage here and there, and grill forks on the parapet
        if x % 16 == 12 and not (POORT_X[0] - 9 <= x <= POORT_X[1] + 9) and not (8 <= x <= 18):
            b.set(x, MUUR_TOP - 1, z0 - 1, GEPOLIJST)
            for y in range(MUUR_TOP - 4, MUUR_TOP - 1):
                b.chain(x, y, z0 - 1)
            b.set(x, MUUR_TOP - 5, z0 - 1, ROOSTER)
        if x % 3 == 0:
            b.bars(x, MUUR_TOP + 4, z0)


def poort(b):
    """De Zwarte Roosterpoort: an arch full of grate, two thick, with the glow of the land behind it shining through."""
    z0 = MUUR_Z[0]
    x0, x1 = POORT_X
    def breedte(y):
        # the arch narrows in its top three rows
        return {POORT_TOP - 2: 1, POORT_TOP - 1: 2, POORT_TOP: 4}.get(y, 0)
    for y in range(F, POORT_TOP + 1):
        smal = breedte(y)
        for x in range(x0 + smal, x1 + 1 - smal):
            for z in range(z0, MUUR_Z[1] + 1):
                b.set(x, y, z, AIR)
            midden = x in ((x0 + x1) // 2, (x0 + x1) // 2 + 1)
            band = y in (F + 4, F + 9)
            for z in (z0 + 1, z0 + 2):
                b.set(x, y, z, PILAAR if midden else GEPOLIJST if band else ROOSTER, {"axis": "y"} if midden else None)
            # the glow behind it
            b.set(x, y, z0 + 5, GLOEIKOOL if (x + y) % 3 else UIENLICHT)
            b.set(x, y, z0 + 6, STENEN)
            b.set(x, y, z0 + 7, STENEN)
    # the frame: polished iron around the arch, studs of charcoal on the bands
    for y in range(F, POORT_TOP + 2):
        smal = breedte(y) if y <= POORT_TOP else 6
        for x in (x0 + smal - 1, x1 + 1 - smal):
            b.set(x, y, z0, GEPOLIJST)
            b.set(x, y, z0 - 1, GEPOLIJST if y % 4 == 3 else IJZER)
        if y > POORT_TOP - 3:
            for x in range(x0 + smal, x1 + 1 - smal):
                if b.get(x, y, z0) != AIR:
                    b.set(x, y, z0, GEPOLIJST)
    for x in range(x0 + 5, x1 - 4):
        b.set(x, POORT_TOP + 1, z0, GEPOLIJST)
        b.set(x, POORT_TOP + 1, z0 - 1, IJZER)
    # iron spikes over the arch and a row of charred Mika heads: who came to the wrong door
    for x in range(x0 + 1, x1, 2):
        b.set(x, MUUR_TOP + 3, z0, TRALIES, {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})
        b.set(x, MUUR_TOP + 2, z0, STENEN)
    # the threshold and the three signs next to the gate
    for x in range(x0 - 1, x1 + 2):
        b.set(x, G, z0 - 1, ZWART_GLAD)
        b.set(x, G, z0 - 2, ZWART_GLAD if x % 2 else ZWART)
    _bord(b, x0 - 2, F + 1, z0 - 1, 0, ["ZWARTE", "ROOSTERPOORT", "Gesloten wegens", "trek. - Sausron"], wand="north")
    _bord(b, x1 + 2, F + 1, z0 - 1, 0, ["Bezorgers:", "achterom.", "Ringen: hier", "afgeven a.u.b."], wand="north")


def poorttoren(b, x0, x1, spiegel):
    """One of "de Tanden": a square gate tower with a grumpy glowing face, a sloping foot, a pointed cap with a spike and two
    round Mika ears that stick out at its corners."""
    z0, z1 = TOREN_Z
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            hoek = x in (x0, x1) and z in (z0, z1)
            for y in range(0, TOREN_TOP + 1):
                if y < G:
                    blok = ROTS
                elif hoek:
                    blok = ZWART
                elif rand:
                    blok = GEBEITELD if y in (F + 8, F + 14, TOREN_TOP) else b.brick(0.15)
                else:
                    blok = STENEN
                b.set(x, y, z, blok)
            for y in range(TOREN_TOP + 1, TOREN_TOP + 7):
                b.set(x, y, z, AIR)
    # the cap: three steps in, an iron spike on top
    for i in range(3):
        for x in range(x0 + i, x1 + 1 - i):
            for z in range(z0 + i * 2, z1 + 1 - i * 2):
                rand = x in (x0 + i, x1 - i) or z in (z0 + i * 2, z1 - i * 2)
                b.set(x, TOREN_TOP + 1 + i, z, (ZWART if i < 2 else GEPOLIJST) if rand else STENEN)
    xm = (x0 + x1) // 2
    for x in (xm, xm + 1):
        b.set(x, TOREN_TOP + 4, z0 + 7, GEPOLIJST)
    b.bars(xm, TOREN_TOP + 5, z0 + 7)
    b.bars(xm + 1, TOREN_TOP + 5, z0 + 7)
    # a heavy plinth
    for x in range(x0, x1 + 1):
        for y in (F, F + 1):
            b.set(x, y, z0, ZWART_GLAD if y == F + 1 else ZWART)
    # the grumpy face: two slanted eye slits of glowing coal under angry brows, a row of bars for teeth
    for dx, dy in ((-2, 1), (-1, 0), (2, 0), (3, 1)):
        b.set(xm + dx, F + 13 + dy, z0, GLOEIKOOL)
    for dx, dy in ((-2, 2), (-1, 1), (2, 1), (3, 2)):
        b.set(xm + dx, F + 13 + dy, z0, GRILLKOOL)
        b.set(xm + dx, F + 13 + dy, z0 - 1, GRILLKOOL)
    for dx in range(-1, 3):
        b.set(xm + dx, F + 9, z0, AIR)
        b.set(xm + dx, F + 9, z0 + 1, GLOEIKOOL)
        b.bars(xm + dx, F + 9, z0)
    for dx in (-1, 2):
        b.set(xm + dx, F + 8, z0 - 1, TAND)                           # two little fangs
    # one big round ear on the outer side of each tower, low enough not to stand in the Eye's light: together the two
    # towers and the gate between them are one wide Mika head with the grate for a grin
    ex = x1 + 2 if spiegel else x0 - 2
    for dx in range(-3, 4):
        for dy in range(-3, 4):
            d = dx * dx + dy * dy
            if d <= 10 and not (x0 <= ex + dx <= x1):
                b.set(ex + dx, TOREN_TOP - 3 + dy, z0, ROOD if d <= 3 else ZWART_GLAD)
                b.set(ex + dx, TOREN_TOP - 3 + dy, z0 + 1, ZWART_GLAD)
    _ = spiegel


# =====================================================================================================================
# the tower of the Eye: a spire with a giant one-eyed Mika head on top
# =====================================================================================================================
def oogtoren(b):
    cx, cz = OOG_C
    lagen = [(7, F, F + 13), (5, F + 14, F + 27), (3, F + 28, KOP_Y[0] - 1)]      # (half width, y from, y to)
    for half, y0, y1 in lagen:
        for x in range(cx - half, cx + half + 1):
            for z in range(cz - half, cz + half + 1):
                rand = abs(x - cx) == half or abs(z - cz) == half
                hoek = abs(x - cx) == half and abs(z - cz) == half
                for y in range(y0 if y0 > F else 0, y1 + 1):
                    if y < G:
                        blok = ROTS
                    elif hoek:
                        blok = ZWART_GLAD
                    elif rand:
                        blok = GEBEITELD if (y - y0) % 7 == 6 else ZWART if (y - y0) % 7 == 0 else b.brick(0.12)
                    else:
                        blok = STENEN
                    b.set(x, y, z, blok)
        # where the tower steps in: a crown of teeth all round (little walls)
        if y0 > F:
            for x in range(cx - half - 1, cx + half + 2):
                for z in range(cz - half - 1, cz + half + 2):
                    if abs(x - cx) == half + 1 or abs(z - cz) == half + 1:
                        if (x + z) % 2 == 0:
                            b.set(x, y0, z, ZWART_MUUR, {"up": "true", "north": "none", "east": "none", "south": "none", "west": "none",
                                                           "waterlogged": "false"})
        # fins: a buttress on the middle of every side
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for i, lengte in enumerate((7, 4, 2)):
                fx, fz = cx + dx * (half + 1 + i), cz + dz * (half + 1 + i)
                for y in range(y0, y0 + lengte):
                    if b.get(fx, y, fz) in (AIR, None) and y < y1:
                        b.set(fx, y, fz, ZWART)
                if b.get(fx, y0 + lengte, fz) in (AIR, None):
                    b.stair(fx, y0 + lengte, fz, {(1, 0): "west", (-1, 0): "east", (0, 1): "north", (0, -1): "south"}[(dx, dz)], block=ZWART_TRAP)
        # glowing slits on every side, in pairs
        for y in range(y0 + 3, y1 - 1, 5):
            for d in ((-1, 1) if half > 3 else (0,)):
                for yy in (y, y + 1):
                    b.set(cx + d * 2, yy, cz - half, GLOEIKOOL)
                    b.set(cx + d * 2, yy, cz + half, GLOEIKOOL)
                    b.set(cx - half, yy, cz + d * 2, GLOEIKOOL)
                    b.set(cx + half, yy, cz + d * 2, GLOEIKOOL)
    # the sealed door at its foot (you never go in) under a little arch
    for y in range(F, F + 3):
        for x in (cx - 1, cx, cx + 1):
            b.set(x, y, cz - 7, GEPOLIJST if x != cx else ROOSTER)
    b.set(cx, F + 3, cz - 7, GLOEIKOOL)
    # corbels: the spire widens under the head
    for i, y in enumerate(range(KOP_Y[0] - 3, KOP_Y[0])):
        half = 4 + i
        for x in range(cx - half, cx + half + 1):
            for z in range(cz - 3, cz + 4):
                if b.get(x, y, z) in (AIR, None):
                    b.set(x, y, z, ZWART if abs(x - cx) == half else STENEN)
    # the head: 15 wide, 11 high, 7 deep; its face looks down the valley (-z); every edge is rounded off
    hx0, hx1, hz0, hz1 = cx - 7, cx + 7, cz - 3, cz + 3
    for x in range(hx0, hx1 + 1):
        for z in range(hz0, hz1 + 1):
            for y in range(KOP_Y[0], KOP_Y[1] + 1):
                op = (x in (hx0, hx1)) + (y in (KOP_Y[0], KOP_Y[1])) + (z in (hz0, hz1))
                if op >= 2:
                    continue
                b.set(x, y, z, ZWART_GLAD if op else STENEN)
    # the socket of the Eye: 9 wide, 5 high, 3 deep, lined with glowing coal
    ox, oy, oz = OOG_BLOK
    for x in range(ox - 4, ox + 5):
        for y in range(oy - 1, oy + 4):
            # an almond: the corners of the socket stay stone
            if abs(x - ox) == 4 and y in (oy - 1, oy + 3):
                continue
            for z in range(hz0, hz0 + 3):
                b.set(x, y, z, AIR)
            b.set(x, y, hz0 + 3, GLOEIKOOL if (x + y) % 2 else UIENLICHT)
    # the angry brow: a slanted ridge of charcoal that sticks out over the socket
    for dx in range(-6, 7):
        y = oy + 4 + (1 if abs(dx) >= 3 else 0) + (1 if abs(dx) >= 5 else 0)
        b.set(ox + dx, y, hz0 - 1, GRILLKOOL)
        b.set(ox + dx, y, hz0, GRILLKOOL)
        if abs(dx) in (3, 5):
            b.set(ox + dx, y - 1, hz0 - 1, GRILLKOOL)
    # the snout (a pink nose that sticks out) and two fangs under it
    for x in (ox - 1, ox, ox + 1):
        b.set(x, oy - 3, hz0 - 1, ROOD)
        b.set(x, oy - 3, hz0, ROOD)
    b.set(ox, oy - 4, hz0 - 1, ROOD)
    b.set(ox, oy - 4, hz0, ROOD)
    for x in (ox - 3, ox + 3):
        for y in (KOP_Y[0] - 2, KOP_Y[0] - 1, KOP_Y[0]):
            b.set(x, y, hz0, TAND)
    # the two big round ears (two thick, a red inside towards the valley)
    for ex in (cx - 6, cx + 6):
        for dx in range(-3, 4):
            for dy in range(-3, 4):
                d = dx * dx + dy * dy
                if d <= 10:
                    y = KOP_Y[1] + 3 + dy
                    b.set(ex + dx, y, cz - 1, ROOD if d <= 3 else ZWART_GLAD)
                    b.set(ex + dx, y, cz, ZWART_GLAD)
    # a curl of a tail at the back, and smoke from the top of its head (he is always cooking something up)
    for dy, dz in ((0, 1), (1, 1), (2, 1), (2, 2), (3, 2), (3, 1)):
        b.set(cx, KOP_Y[0] + 2 + dy, hz1 + dz, ROOD)
    b.set(cx, KOP_Y[1] + 1, cz + 2, ROOKGAT)
    b.set(cx - 2, KOP_Y[1] + 1, cz + 1, ROOKGAT)


# =====================================================================================================================
# de Schaduwlaan: the lane under the wall, the smoke, het Wachthek, het Roosterpoortje
# =====================================================================================================================
def laan(b):
    # the spiked wall that makes it a lane (it also keeps the riders out of the plain)
    for x in range(22, 79):
        for y in range(F, F + 3):
            b.set(x, y, 60, b.brick(0.25))
        b.set(x, F + 3, 60, STENEN if x % 2 else AIR)
        if x % 4 == 1:
            b.set(x, F, 61, STENEN)
            b.set(x, F + 3, 60, TRALIES, {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})
    for x in range(16, 23):
        for y in range(F, F + 4):
            b.set(x, y, 60, b.brick(0.25))                        # (closed at the west end: het Wachthek is the only way)
    for z in range(58, 61):
        for y in range(F, F + 4):
            b.set(16, y, z, b.brick(0.2)) if binnen(16, z) else None
    # braziers along the lane
    for x in (68, 44, 30):
        b.set(x, F, 61, ZWART)
        b.set(x, F + 1, 61, GLOEIKOOL)
    b.set(78, F + 3, 60, GLOEIKOOL)
    # the curtain of smoke: vents in the floor
    (rx0, _, rz0), (rx1, _, rz1) = PLEKKEN["ROOK"]
    for x in range(rx0, rx1 + 1):
        for z in range(rz0, rz1 + 1):
            if (x + z) % 2 == 0 and x not in (rx0, rx1):
                b.set(x, G, z, ROOKGAT)
            else:
                b.set(x, G, z, ZWART_GLAD)
    _bord(b, rx1 + 2, F, rz0, 12, ["Pas op: rook.", "Je ziet hier geen", "poot voor ogen.", "Licht helpt, njeg."])
    # niches in the foot of the wall: a place to hold your breath while a rider passes
    for x in (67, 46):
        for xx in (x, x + 1):
            for y in range(F, F + 3):
                b.set(xx, y, MUUR_Z[0], AIR)
        b.set(x, F + 2, MUUR_Z[0], AIR)
        b.schuil += [(x, F, MUUR_Z[0])]
    # the post of skulls: up to here the ring had better stay off
    sx, _, sz = PLEKKEN["SCHEDELPAAL"][0]
    b.fence(sx, F, sz)
    b.fence(sx, F + 1, sz)
    b.set(sx, F + 2, sz, MIKAKOP, {"facing": "east"})
    b.must_reach["the lane"] = (60, F, 65)


def wachthek(b):
    """A barricade across the lane with one gap, watched by three Mika guards; behind it the yard of the side door."""
    hx = 25
    for z in range(62, 70):
        if z in (65, 66):
            continue
        for y in range(F, F + 4):
            b.set(hx, y, z, PILAAR if z in (64, 67) else ROOSTER, {"axis": "y"} if z in (64, 67) else None)
    for z in (64, 67):
        b.set(hx, F + 4, z, GLOEIKOOL)
    b.set(hx, F + 3, 65, ROOSTER)
    b.set(hx, F + 3, 66, ROOSTER)
    # two little guard huts on the lane side (a roof on two posts) and a brazier
    for z0 in (62, 68):
        for x in (26, 28):
            for y in range(F, F + 3):
                b.fence(x, y, z0 if z0 == 62 else z0 + 1)
        for x in range(26, 29):
            for z in (z0, z0 + 1):
                b.slab(x, F + 3, z, "bottom")
    b.set(29, F, 65, AIR)
    # the yard behind it: the fifth Rustvuurtje, benches of the guards, the sign of the side door
    vx, _, vz = PLEKKEN["VUUR_POORTJE"][0]
    _vuurplek(b, vx, vz)
    _stam(b, vx + 2, F, vz - 1, "z", 2, soort="guhs:worst_stam")
    _bord(b, 17, F + 1, MUUR_Z[0] - 1, 0, ["Roosterpoortje", "Alleen personeel.", "Kloppen heeft", "geen zin. Njeg."], wand="north")
    b.must_reach["the fire at the side door"] = (vx + 1, F, vz + 1)
    b.must_reach["the side door"] = PLEKKEN["DEUR_SCENE"][0]


def poortje(b):
    """Het Roosterpoortje: a low arch in the wall (its bars are only shown to who may not pass yet) and the tunnel behind."""
    (x0, _, z0), (x1, y1, z1) = PLEKKEN["TUNNEL"]
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(F, y1 + 1):
                if y == y1 and x != (x0 + x1) // 2:
                    b.set(x, y, z, ZWART)
                    continue
                b.set(x, y, z, AIR)
            b.set(x, G, z, ZWART_GLAD)
    for z in (z0 + 2, z0 + 5):
        b.set(x0 - 1, F + 1, z, GLOEIKOOL)
        b.set(x1 + 1, F + 1, z, GLOEIKOOL)
    # the frame on both faces of the wall
    for z in (z0, z1):
        for y in range(F, y1 + 2):
            b.set(x0 - 1, y, z, GEPOLIJST)
            b.set(x1 + 1, y, z, GEPOLIJST)
        for x in range(x0 - 1, x1 + 2):
            b.set(x, y1 + 1, z, GEPOLIJST)
    b.must_reach["the tunnel"] = ((x0 + x1) // 2, F, z1)


def achter(b):
    """Behind the wall: the last Rustvuurtje, the road to the mountain."""
    vx, _, vz = PLEKKEN["VUUR_ACHTER"][0]
    _vuurplek(b, vx, vz)
    _stam(b, vx - 1, F, vz + 3, "x", 3)
    _stam(b, vx + 3, F, vz - 1, "z", 2)
    b.fence(26, F, 88)
    b.fence(26, F + 1, 88)
    b.lantern(26, F + 2, 88)
    _bord(b, 22, F, 90, 0, ["Frituurberg", "die kant op.", "Het ruikt al", "naar frituur. Njeg!"])
    for x, z in ((14, 82), (27, 81), (16, 89), (24, 86)):
        b.set(x, F, z, SMEUL)
    b.must_reach["the last fire"] = (vx + 1, F, vz + 1)
    b.must_reach["the far mouth"] = (20, F, 94)


# =====================================================================================================================
# the whole build
# =====================================================================================================================
def bouw(h):
    b = Poort(h)
    terrein(b)
    # the flat stretches of the course first (a plate also clears the air above it: nothing may be built before it)
    for plaat in ((40, 2, 58, 10), (12, 21, 60, 41), (16, 45, 84, 57), (18, 62, 82, 69), (10, 78, 30, 92)):
        b.vlak(*plaat)
    kamp(b, h)
    zijgangen(b)
    weg(b)
    asveld(b)
    uitkijk(b)
    slakkenhut(b)
    vlakte(b)
    holte(b)
    muur(b)
    poorttoren(b, *TOREN_W, False)
    poorttoren(b, *TOREN_O, True)
    poort(b)
    oogtoren(b)
    laan(b)
    wachthek(b)
    poortje(b)
    achter(b)
    aankleding(b)
    b.connect()
    return b, check(b)


# =====================================================================================================================
# checks
# =====================================================================================================================
DOORKIJK = {AIR, SMEUL, "minecraft:crimson_sign", "minecraft:crimson_wall_sign", "minecraft:lantern", KETTING, "minecraft:green_carpet",
            "minecraft:red_carpet", "minecraft:polished_blackstone_button", TRALIES, HEK}


def ziet(b, van, naar):
    """Is there a free line from `van` to `naar` (points in block coordinates)? Steps of a tenth of a block; bars, fences,
    signs and embers don't stop a look (as in the game: their shapes are thin), everything else does."""
    dx, dy, dz = naar[0] - van[0], naar[1] - van[1], naar[2] - van[2]
    n = int(math.sqrt(dx * dx + dy * dy + dz * dz) * 10) + 1
    for i in range(1, n):
        t = i / n
        c = (int(math.floor(van[0] + dx * t)), int(math.floor(van[1] + dy * t)), int(math.floor(van[2] + dz * t)))
        v = b.s.blocks.get(c)
        if v is not None and v[0] not in DOORKIJK:
            return False
    return True


def oog_ziet(b, cel, gebukt=False):
    """Does the Eye see a player standing in this cell (their head or their middle)?"""
    x, y, z = cel
    for hoog in ((1.27, 0.75) if gebukt else (1.62, 0.9)):
        if ziet(b, OOG_KIJK, (x + 0.5, y + hoog, z + 0.5)):
            return True
    return False


def _java(h):
    pad = os.path.join(os.path.dirname(h.R) if hasattr(h, "R") else os.path.join("src", "main"), "java", "nl", "juiced", "guhs", "feature", "ringh5",
                       "Plekken.java")
    if not os.path.exists(pad):
        return None
    tekst = open(pad, encoding="utf-8").read()
    uit = {}
    for m in re.finditer(r"\b([A-Z][A-Z0-9_]*)\s*=\s*((?:List\.of\()?\s*new BlockPos\([^;]*);", tekst):
        uit[m.group(1)] = [tuple(int(v) for v in p) for p in re.findall(r"new BlockPos\((-?\d+),\s*(-?\d+),\s*(-?\d+)\)", m.group(2))]
    m = re.search(r"OOG_KIJK\s*=\s*new Vec3\(([-\d.]+),\s*([-\d.]+),\s*([-\d.]+)\)", tekst)
    if m:
        uit["OOG_KIJK"] = [tuple(float(v) for v in m.groups())]
    return uit


def check(b):
    problems = []
    blocks = b.s.blocks

    def nm(c):
        v = blocks.get(c)
        return v[0] if v else None

    def staat(c):
        return sb.solid(nm((c[0], c[1] - 1, c[2]))) and not sb.solid(nm(c)) and not sb.solid(nm((c[0], c[1] + 1, c[2])))

    # every spot is a place to stand
    for naam in ("BOROMIKA", "SMIKAGOL_KAMP", "UITKIJK", "DEUR_SCENE", "WACHTER_1", "WACHTER_2", "WACHTER_3", "ROOK_TERUG"):
        if not staat(PLEKKEN[naam][0]):
            problems.append(f"{NAAM}: {naam} {PLEKKEN[naam][0]} is not a place to stand")
    for naam in ("BLIK_A", "BLIK_B", "BLIK_RUST", "SCHUIL_A", "RUITER_1", "RUITER_2", "ROUTE_UITKIJK", "ROUTE_B", "ROUTE_LAAN", "ROUTE_HEK", "ROUTE_DEUR",
                 "ROUTE_ACHTER"):
        for c in PLEKKEN[naam]:
            if not staat(c):
                problems.append(f"{NAAM}: {naam} {c} is not a place to stand")
    for naam in ("VUUR_KAMP", "VUUR_UITKIJK", "VUUR_SLAKKENHUT", "VUUR_HOLTE", "VUUR_POORTJE", "VUUR_ACHTER"):
        if nm(PLEKKEN[naam][0]) != VUUR:
            problems.append(f"{NAAM}: {naam} {PLEKKEN[naam][0]} is {nm(PLEKKEN[naam][0])}, not a Rustvuurtje")
    # the bars of the side door are really air (Schijn shows them), the Eye's socket is free
    (x0, y0, z0), (x1, y1, z1) = PLEKKEN["DEUR"]
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            if nm((x, y, z0)) != AIR:
                problems.append(f"{NAAM}: the side door has {nm((x, y, z0))} at {(x, y, z0)}")
    ox, oy, oz = OOG_BLOK
    for dx in range(-3, 4):
        for dy in range(0, 3):
            if nm((ox + dx, oy + dy, oz)) != AIR:
                problems.append(f"{NAAM}: the socket of the Eye has {nm((ox + dx, oy + dy, oz))} at {(ox + dx, oy + dy, oz)}")
    # the walk: from the mouth to the far mouth, past every fire
    start = (48, F, 1)
    seen, _cells = sb.reach(b, start)
    if not seen:
        problems.append(f"{NAAM}: the mouth {start} is not a place to stand")
    for wat, cel in b.must_reach.items():
        if seen and cel not in seen:
            problems.append(f"{NAAM}: can't walk from the mouth to {wat} at {cel}")
    # what the Eye sees
    for cel in b.schuil:
        if oog_ziet(b, cel) or oog_ziet(b, cel, True):
            problems.append(f"{NAAM}: the Eye sees the hiding place {cel}")
    for cel in b.open:
        if not oog_ziet(b, cel) or not oog_ziet(b, cel, True):
            problems.append(f"{NAAM}: the Eye does not see the open cell {cel}")
    for x in range(30, 80):
        for z in (63, 65, 67):
            if staat((x, F, z)) and oog_ziet(b, (x, F, z)):
                problems.append(f"{NAAM}: the Eye sees into the lane at {(x, F, z)}")
                break
    for naam in ("VUUR_UITKIJK", "VUUR_SLAKKENHUT", "VUUR_HOLTE", "VUUR_POORTJE"):
        vx, vy, vz = PLEKKEN[naam][0]
        if all(oog_ziet(b, (vx + dx, vy, vz + dz)) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)) if staat((vx + dx, vy, vz + dz))):
            problems.append(f"{NAAM}: the Eye sees every side of the fire {naam}")
    # nothing hangs on a seam
    try:
        from features import paleizen_bouw
        problems += paleizen_bouw.check_steun(b, ANKER)
    except ImportError:
        pass
    # Java has the same numbers
    java = _java(b.h)
    if java is None:
        problems.append(f"{NAAM}: feature/ringh5/Plekken.java is missing")
    else:
        for naam, punten in list(PLEKKEN.items()) + [("OOG_KIJK", [OOG_KIJK])]:
            if java.get(naam) != [tuple(p) for p in punten]:
                problems.append(f"{NAAM}: Plekken.java says {naam} = {java.get(naam)}, the build says {punten}")
    return problems


# =====================================================================================================================
# the game test room
# =====================================================================================================================
def test_template(h):
    """ringh5_test_kamer (33 x 14 x 33): a floor, a Rustvuurtje at (3, 1, 3), a wall with a roof to hide behind at x 14..17,
    z 20 (the test's Eye hangs at the far end, high up)."""
    t = h.Structure((33, 14, 33))
    for x in range(33):
        for z in range(33):
            t.set(x, 0, z, STENEN)
    t.set(3, 1, 3, VUUR)
    for x in range(14, 18):
        for y in range(1, 5):
            t.set(x, y, 21, STENEN)
        t.set(x, 4, 20, STENEN)
        t.set(x, 4, 19, STENEN)
    return t


# =====================================================================================================================
# pictures (not part of the build)
# =====================================================================================================================
class _H:
    """What bouw() needs of the make_v2 namespace, for the preview."""
    def __init__(self):
        import make_structures as ms
        self.ms, self.Structure, self.Byte, self.floats = ms, ms.Structure, ms.Byte, ms.floats
        self.R = os.path.join("src", "main", "resources")


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    if "--java" in sys.argv:
        print(java_plekken())
    else:
        b, problems = bouw(_H())
        for p in problems:
            print("PROBLEM:", p)
        print(len(b.s.blocks), "blocks")
