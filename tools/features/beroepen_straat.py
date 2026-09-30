"""
De beroepen (2.9) - the Beroepenstraat: the Knuffeldal town's 2.9 street piece (23 x 44 x 133) on the town's one free
street jigsaw (guhs:knuffeldal_straat_vrij, on this piece's WEST side at z = JIG_Z; it hangs at the east end of
hoek_noordoost's street, so the piece runs north-south along the town's east edge, world box (80, -48)..(102, 84) from
the plein's corner). Ground top at y = G = 4, like the rest of the town.

  x 0          a low fluffy hedge (open where the town's street comes in), lamp posts with seasonal garlands over the street
  x 1..5       the street (knuffelklinkers)
  x 6          the pavement (lamp posts, flower boxes, benches)
  x 7..20      the buildings, their fronts to the street (west)
  x 21         the achterpad (a little back path) all along, x 22 a hedge

  z 1..16      the brandweer-oefenterrein: five marshmallow-kampvuurkuilen (they flare up during Blusguh's quest), benches,
               a brandkraan with a face, the slangenschuurtje, and a tall pluizenboom with a ladder (the guhtje gets stuck up there)
  z 18..38     the Brandweerkazerne: red brick, two garages with guh fire trucks (a face on the front, blue lights, a ladder),
               a slangentoren with a guh head on top wearing a helmet and a blue zwaailicht, upstairs the dormitory and a
               brandweerpaal; the whole roof is a giant red fire helmet with a comb and a golden badge. Blusguh by the door.
  z 39..47     the Beroepenpleintje where the town's street comes in: a guh statue with three hats stacked on its head
               ("veel petten op"), a signpost, benches, flower boxes, a bush
  z 49..74     the Politiebureautje: white with blue-orange politiestriping, a blue lamp by the door, a guh face upstairs;
               inside the balie, the (empty!) knabbelkluis, the knuffelcel, the recherchekamer upstairs; the roof is a giant
               police cap with a checkered band, a visor over the street and a golden badge, two zwaailichten on top
  z 75..79     the steegje (bins, a hiding spot)
  z 80..104    the Apotheekje: white with green, a glowing green apotheekkruis over the street, the round white guh-head roof
               with ears and a big vijzel on top; inside the balie with medicine shelves, the mengketel for customers, and
               the ziekenhoekje where Snotje (a snotterig guhtje) sits by his bed
  z 106..122   the kruidentuin: snotkruid beds, a little greenhouse, the tuinhuisje
  z 123..131   a little park with a pond; the street's end

Invisible markers: beroepen_guhtjeplek (the spot in the tree), beroepen_kluisplek (where the Knabbeldief's trail starts),
beroepen_verstopplek (six hiding places the Knabbeldief picks from). check() (the town's geometry self-check) runs on it.
"""
import json
import math

from features import knuffeldal_stadje as st

W, H, D = 23, 44, 133
G = st.G
JIG_Z = 40                                   # world z -8: the end of hoek_noordoost's street
NAAM = "knuffeldal_stadje/beroepenstraat"

KS, KS_PLAAT, KS_MUUR, KS_FACE, KS_TRAP = st.KS, st.KS_PLAAT, st.KS_MUUR, st.KS_FACE, st.KS_TRAP
KLINK, GRAS, BLAD, STAM, LAMP = st.KLINK, st.GRAS, st.BLAD, st.STAM, st.LAMP
DAK, DAK_PLAAT = st.DAK, st.DAK_PLAAT

VUUR = "guhs:beroepen_marshmallowvuur"
GUHTJEPLEK = "guhs:beroepen_guhtjeplek"
KLUISPLEK = "guhs:beroepen_kluisplek"
VERSTOPPLEK = "guhs:beroepen_verstopplek"
SNOTKRUID = "guhs:beroepen_snotkruid"
MENGKETEL = "guhs:beroepen_mengketel"
MARKERS = (GUHTJEPLEK, KLUISPLEK, VERSTOPPLEK)

# the geometry check of the town knows plants and thin things; our markers and plant count as walk-through
st.PLANTS.update({SNOTKRUID, GUHTJEPLEK, KLUISPLEK, VERSTOPPLEK, "guhs:beroepen_pootafdruk"})

PITS = [(9, 4), (14, 4), (11, 8), (9, 12), (14, 12)]      # the marshmallow campfire pits on the oefenterrein
BOOM = (19, 11)                                           # the tall tree; the guhtje's spot is west of the trunk, up high
GUHTJE = (17, G + 11, 11)
KLUIS = (17, G + 1, 70)
VERSTOP = [(19, G + 1, 3), (20, G + 1, 46), (20, G + 1, 77), (18, G + 1, 118), (20, G + 1, 129), (8, G + 1, 1)]
NPCS = {"brandweerguh": ((6, G + 1, 26), "west"), "politieguh": ((9, G + 1, 64), "west"), "apothekerguh": ((15, G + 1, 92), "west")}
SNOTJE = (11, G + 1, 101)
KETEL = (10, G + 1, 84)


def mc(n):
    return "minecraft:" + n


def door(b, x, z, facing, wood="cherry", hinge="left", y=None, open_=False):
    y = G + 1 if y is None else y
    for dy, half in ((0, "lower"), (1, "upper")):
        b.set(x, y + dy, z, mc(f"{wood}_door"), {"facing": facing, "half": half, "hinge": hinge, "open": str(open_).lower(), "powered": "false"})


def pane(b, x, y, z, glass="glass_pane", ns=True):
    b.set(x, y, z, mc(glass), st.pane(ns))


def sign(b, x, y, z, facing, keys, wall=True, colour="black", wood="spruce"):
    blank = json.dumps("")
    msgs = [json.dumps({"translate": f"sign.guhs.beroepen_{k}"}) if k else blank for k in keys] + [blank] * (4 - len(keys))
    ms = b.h.ms
    text = {"messages": ms.NbtList(8, msgs), "color": colour, "has_glowing_text": ms.Byte(1)}
    empty = {"messages": ms.NbtList(8, [blank] * 4), "color": "black", "has_glowing_text": ms.Byte(0)}
    nbt = {"id": "minecraft:sign", "is_waxed": ms.Byte(1), "front_text": text, "back_text": empty}
    if wall:
        b.set(x, y, z, mc(f"{wood}_wall_sign"), {"facing": facing, "waterlogged": "false"}, nbt)
    else:
        b.set(x, y, z, mc(f"{wood}_sign"), {"rotation": {"south": "0", "west": "4", "north": "8", "east": "12"}[facing], "waterlogged": "false"}, nbt)


def lantern(b, x, y, z, hanging=True, soul=False):
    b.set(x, y, z, mc("soul_lantern" if soul else "lantern"), {"hanging": str(hanging).lower(), "waterlogged": "false"})


def face_x(b, x, cy, cz, facing, R, blocks=None, only_features=True, skip=None):
    """A guh face on a wall in the plane x = const (facing east or west)."""
    st.face_on_plane(b, x, cy, cz, facing, R, blocks=blocks, only_features=only_features, skip=skip)


def window_face_blocks(skin, ring="minecraft:light_blue_stained_glass"):
    blocks = dict(st.FACE_BLOCKS)
    blocks.update({"eye": mc("black_stained_glass"), "ring": ring, "shine": mc("white_stained_glass"), "skin": skin})
    return blocks


def face_on_dome(b, cells, front, cz, cy, R, blocks):
    """Paints a guh face on the front (west, the lowest x) of a dome: every (y, z) of the face lands on the dome's
    outermost block there."""
    n = int(R + 1)
    for du in range(-n, n + 1):
        for dv in range(-n, n + 1):
            role = st.face_role(du, dv, R)
            if role is None or role == "skin":
                continue
            z, y = cz - du, cy + dv                         # (looking west: its right hand is north, -z)
            xs = [x for (x, yy, zz) in cells if yy == y and zz == z]
            if xs:
                b.set(min(xs) if front == "west" else max(xs), y, z, blocks[role])
    b.grote_gezichten += 1


# =====================================================================================================================
# ground, street, hedges, lamps
# =====================================================================================================================
def grond(b):
    for x in range(W):
        for z in range(D):
            for y in range(G):
                b.set(x, y, z, mc("pink_wool"))
            b.set(x, G, z, GRAS)
    for z in range(1, D - 1):
        for x in range(1, 6):
            b.set(x, G, z, KLINK)
        b.set(6, G, z, KS if z % 3 == 0 else KLINK)          # the pavement
        b.set(21, G, z, KLINK)                                # the achterpad
    for x in range(0, 6):                                     # where the town's street comes in
        for z in range(JIG_Z - 2, JIG_Z + 3):
            b.set(x, G, z, KLINK)
    # the paths from the street to the achterpad between the buildings
    for (z0, z1) in ((17, 17), (48, 48), (75, 79), (105, 105)):
        for x in range(6, 22):
            for z in range(z0, z1 + 1):
                b.set(x, G, z, KLINK)


def heggen(b):
    for z in range(D):
        if not (JIG_Z - 2 <= z <= JIG_Z + 2):
            b.set(0, G + 1, z, BLAD, {"distance": "1", "persistent": "true", "waterlogged": "false"})
        b.set(22, G + 1, z, BLAD, {"distance": "1", "persistent": "true", "waterlogged": "false"})
    for x in range(1, 22):
        for z in (0, D - 1):
            b.set(x, G + 1, z, BLAD, {"distance": "1", "persistent": "true", "waterlogged": "false"})


def lampen(b):
    for z in (4, 14, 36, 51, 70, 83, 101, 111, 125):
        for x in (0, 6):
            b.air(x, G + 1, z, x, G + 1, z)
            b.set(x, G, z, KS)
            b.lantaarnpaal(x, z, height=4)
        b.slinger(1, z, 5, z, G + 5)
    for z in (JIG_Z - 3, JIG_Z + 3):                          # the corners where the town's street comes in
        b.air(0, G + 1, z, 0, G + 1, z)
        b.set(0, G, z, KS)
        b.lantaarnpaal(0, z, height=3)


# =====================================================================================================================
# the brandweer-oefenterrein and the kazerne
# =====================================================================================================================
def oefenterrein(b):
    # a paved ring round the pits, grass elsewhere
    for x in range(7, 17):
        for z in range(1, 16):
            if math.dist((x, z), (11.5, 8)) <= 6.2:
                b.set(x, G, z, KS if (x + z) % 5 == 0 else KLINK)
    for (x, z) in PITS:
        b.set(x, G, z, KS)
        b.set(x, G + 1, z, VUUR, {"vuur": "0"})
    for (x, z, f) in ((7, 7, "east"), (7, 9, "east"), (16, 7, "west"), (11, 14, "north"), (12, 2, "south")):
        b.bank(x, z, f)
    # the brandkraan: a red post with a little face and a cap
    b.set(8, G + 1, 15, mc("red_concrete"))
    b.set(8, G + 2, 15, KS_FACE, {"facing": "north", "stemming": "1"})
    b.gezichten += 1
    b.set(8, G + 3, 15, mc("red_nether_brick_slab"), {"type": "bottom", "waterlogged": "false"})
    b.set(7, G + 1, 15, mc("red_nether_brick_wall"), {"up": "true", "north": "none", "south": "none", "east": "low", "west": "none",
                                                       "waterlogged": "false"})
    # the slangenschuurtje (x 17..21, z 1..5): open to the south, hoses and helmets inside
    for x in range(17, 22):
        for z in range(1, 6):
            b.set(x, G, z, mc("spruce_planks"))
            edge = x in (17, 21) or z == 1
            for y in range(G + 1, G + 4):
                if edge and not (z == 5):
                    b.set(x, y, z, mc("red_terracotta") if y < G + 3 else mc("white_concrete"))
            b.set(x, G + 4, z, mc("red_nether_brick_slab"), {"type": "bottom", "waterlogged": "false"})
    for (x, z) in ((18, 2), (20, 2)):
        b.set(x, G + 1, z, mc("red_wool"))                      # rolled-up hoses
        b.set(x, G + 2, z, mc("red_carpet"))
    b.set(20, G + 3, 2, LAMP, {"hanging": "true", "waterlogged": "false"})
    b.set(17, G + 3, 3, KS_FACE, {"facing": "west", "stemming": "2"})
    b.gezichten += 1
    # the tall tree with the ladder; the guhtje's spot high up on the west side of the crown
    tx, tz = BOOM
    for y in range(G + 1, G + 11):
        b.set(tx, y, tz, STAM, {"axis": "y"})
    cy = G + 11.5
    for dx in range(-4, 5):
        for dz in range(-4, 5):
            for dy in range(-3, 4):
                if (dx * dx + dz * dz) / 3.4 ** 2 + dy * dy / 2.6 ** 2 <= 1.0 and 0 <= tx + dx < 22 and 1 <= tz + dz < D - 1:
                    if b.get(tx + dx, int(cy + dy), tz + dz) is None:
                        b.set(tx + dx, int(cy + dy), tz + dz, BLAD, {"distance": "1", "persistent": "true", "waterlogged": "false"})
    gx, gy, gz = GUHTJE
    b.air(gx, gy, gz, gx, gy + 2, gz)
    b.air(gx + 1, G + 1, gz, gx + 1, gy + 2, gz)                # the ladder's shaft through the crown
    for y in range(G + 1, gy):
        b.set(gx + 1, y, gz, st.LADDER, {"facing": "west", "waterlogged": "false"})
    b.set(gx, gy - 1, gz, STAM, {"axis": "x"})                   # a branch to sit on
    b.set(gx, gy, gz, GUHTJEPLEK)
    b.targets["het guhtje in de boom"] = (gx, gy, gz)
    b.targets["de vuurkuilen"] = (11, G + 1, 10)


def truck(b, xf, zc):
    """A guh fire truck facing west (its face at x = xf), 3 wide, 8 long: red with white stripes, a cab with windows, a
    blue light, a ladder on top, black wheels."""
    for x in range(xf, xf + 7):
        for z in range(zc - 1, zc + 2):
            for y in (G + 1, G + 2):
                b.set(x, y, z, mc("red_concrete") if not (y == G + 2 and z != zc and x > xf + 2) else mc("white_concrete"))
    for x in (xf + 1, xf + 5):
        for z in (zc - 1, zc + 1):
            b.set(x, G + 1, z, mc("black_concrete"))
    for x in range(xf, xf + 3):                                   # the cab
        for z in range(zc - 1, zc + 2):
            b.set(x, G + 3, z, mc("light_blue_stained_glass") if x == xf or z != zc else mc("red_concrete"))
    b.set(xf + 1, G + 4, zc, mc("blue_stained_glass"))           # the zwaailicht
    # its face: eyes, blush, a smile on the grille
    b.set(xf, G + 2, zc - 1, mc("black_concrete"))
    b.set(xf, G + 2, zc + 1, mc("black_concrete"))
    b.set(xf, G + 1, zc, mc("pink_concrete"))
    b.set(xf - 1, G + 1, zc - 1, mc("red_nether_brick_slab"), {"type": "bottom", "waterlogged": "false"})    # a bumper
    b.set(xf - 1, G + 1, zc + 1, mc("red_nether_brick_slab"), {"type": "bottom", "waterlogged": "false"})
    for x in range(xf + 3, xf + 7):                              # the ladder on top
        b.set(x, G + 3, zc, mc("birch_slab") if x % 2 else mc("iron_block"), {"type": "bottom", "waterlogged": "false"} if x % 2 else None)


def kazerne(b):
    x0, x1, z0, z1 = 7, 20, 18, 38
    tx0, tz1 = 16, 22                                           # the slangentoren: x 16..20, z 18..22
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G, z, KS if (x + z) % 2 else mc("polished_andesite"))
    top = G + 12
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = x in (x0, x1) or z in (z0, z1)
            corner = x in (x0, x1) and z in (z0, z1)
            for y in range(G + 1, top + 1):
                if edge:
                    band = y in (G + 6, G + 7, top)
                    b.set(x, y, z, mc("white_concrete") if corner or band else mc("bricks"))
            if not edge:
                b.set(x, G + 7, z, mc("spruce_planks"))                 # the first floor
    # the garages (5 wide, 5 high) and the door between them; the red-white awnings
    for zc in (23, 33):
        for z in range(zc - 2, zc + 3):
            for y in range(G + 1, G + 6):
                b.air(x0, y, z, x0, y, z)
            b.set(x0 - 1, G + 6, z, mc("red_concrete") if z % 2 else mc("white_concrete"))
        truck(b, x0 + 2, zc)
        b.set(x0, G + 6, zc, KS_FACE, {"facing": "west", "stemming": "0"})
        b.gezichten += 1
    door(b, x0, 28, "east")
    b.set(x0 - 1, G + 4, 28, mc("red_nether_brick_slab"), {"type": "top", "waterlogged": "false"})
    lantern(b, x0 - 1, G + 3, 28)
    # windows upstairs on the sides, a big guh face on the front (its eyes are windows)
    blocks = window_face_blocks(mc("bricks"))
    face_x(b, x0, G + 9, 28, "west", 3.3, blocks=blocks, skip=lambda x, y, z, role: y <= G + 7)
    # (the windows, faces, balconies... on the sides and the back: achterkanten())
    # inside, the garage: lockers, hoses on the back wall, helmets
    for z in range(25, 32):
        if z != 28:
            b.set(x1 - 1, G + 1, z, mc("barrel"), {"facing": "west", "open": "false"})
    for z in (26, 30):
        b.set(x1 - 1, G + 2, z, mc("red_concrete"))                   # helmets on the lockers
    for z in range(24, 33, 2):
        b.set(x1 - 1, G + 4, z, mc("red_wool"))                       # hose rolls on the wall
    for z in (23, 33):
        lantern(b, 13, G + 6, z)
    # the ladder up (back corner) and the hole in the floor
    for y in range(G + 1, G + 8):
        b.set(x1 - 1, y, z1 - 1, st.LADDER, {"facing": "west", "waterlogged": "false"})
    # the brandweerpaal: a pole from the dormitory down into the garage
    for y in range(G + 1, G + 12):
        b.set(x0 + 2, y, 28, mc("end_rod"), {"facing": "up"})
    # upstairs: bunk beds, a table with kaasknabbels, chairs, a sofa, lanterns
    for (bx, bz) in ((10, 36), (13, 36), (16, 36)):
        b.set(bx, G + 8, bz, mc("red_bed"), {"facing": "south", "part": "foot", "occupied": "false"})
        b.set(bx, G + 8, bz + 1, mc("red_bed"), {"facing": "south", "part": "head", "occupied": "false"})
    b.set(12, G + 8, 26, "guhs:guh_tafel", {"facing": "south"})
    b.set(13, G + 8, 26, "guhs:guh_tafel", {"facing": "south"})
    b.set(12, G + 9, 26, "guhs:block_of_kaasknabbels")
    for (x, z, f) in ((12, 25, "south"), (13, 25, "south"), (12, 27, "north"), (13, 27, "north")):
        b.set(x, G + 8, z, "guhs:guh_stoel", {"facing": f})
    b.set(16, G + 8, 30, "guhs:red_zitzak", {"facing": "west"})
    b.set(16, G + 8, 32, "guhs:red_zitzak", {"facing": "west"})
    for z in (24, 32):
        lantern(b, 13, G + 11, z)
    b.targets["boven in de kazerne"] = (14, G + 8, 30)
    # the slangentoren: up to G + 20, a guh head wearing a helmet on top, a blue zwaailicht
    for x in range(tx0, x1 + 1):
        for z in range(z0, tz1 + 1):
            edge = x in (tx0, x1) or z in (z0, tz1)
            for y in range(G + 1, G + 20):
                if edge:
                    b.set(x, y, z, mc("white_concrete") if (y - G) % 5 == 0 else mc("bricks"))
                else:
                    b.air(x, y, z, x, y, z)
    b.air(tx0, G + 1, 20, tx0, G + 2, 20)                                  # the way in from the garage
    for y in range(G + 8, G + 19):
        b.set(18, y, 20, mc("red_wool"))                                   # hoses hanging to dry
    b.set(18, G + 19, 20, mc("iron_block"))
    for y in (G + 10, G + 14):
        pane(b, tx0, y, 20, "glass_pane", ns=True)
        pane(b, x1, y, 20, "glass_pane", ns=True)
    hy = G + 20
    for x in range(tx0, x1 + 1):
        for z in range(z0, tz1 + 1):
            for y in range(hy, hy + 5):
                if not (x in (tx0, x1) and z in (z0, tz1) and y in (hy, hy + 4)):
                    b.set(x, y, z, mc("pink_wool"))
    for facing, (fx, fz) in (("west", (tx0, 20)), ("east", (x1, 20)), ("north", (18, z0)), ("south", (18, tz1))):
        st.face_on_plane(b, fx, hy + 2, fz, facing, 2.2, only_features=True)
    for x in range(tx0 - 1, x1 + 2):                                       # the helmet: brim, crown, comb
        for z in range(z0 - 1, tz1 + 2):
            b.set(x, hy + 5, z, mc("red_concrete"))
    for x in range(tx0, x1 + 1):
        for z in range(z0, tz1 + 1):
            b.set(x, hy + 6, z, mc("red_concrete"))
    for z in range(z0, tz1 + 1):
        b.set(18, hy + 7, z, mc("red_terracotta"))
    b.set(tx0 - 1, hy + 6, 20, mc("gold_block"))
    b.set(18, hy + 8, 20, mc("sea_lantern"))
    b.set(18, hy + 9, 20, mc("blue_stained_glass"))
    for sz in (z0 - 1, tz1 + 1):                                            # its ears, poking out under the helmet
        for y in (hy + 2, hy + 3):
            b.set(18, y, sz, mc("pink_wool"))
            b.set(17, y, sz, mc("magenta_terracotta"))
            b.set(19, y, sz, mc("pink_wool"))
    # the roof: a giant red fire helmet (brim, dome, comb, a golden badge on its front)
    cx, cz, rx, rz = (x0 + x1) / 2, (z0 + z1) / 2, (x1 - x0) / 2 + 0.6, (z1 - z0) / 2 + 0.6
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            if tx0 <= x <= x1 and z0 <= z <= tz1:
                continue
            b.set(x, top, z, mc("red_concrete"))                           # the brim
    dome = []
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if tx0 <= x <= x1 and z0 <= z <= tz1:
                continue
            d = math.hypot((x - cx) / rx, (z - cz) / rz)
            hgt = int(round(6.5 * math.sqrt(max(0.0, 1 - min(1.0, d) ** 2))))
            for y in range(top + 1, top + 1 + max(1, hgt)):
                b.set(x, y, z, mc("red_terracotta") if y == top + 1 else mc("red_concrete"))
                dome.append((x, y, z))
    for z in range(z0 + 5, z1 - 1):                                           # the comb along the top
        ys = [y for (x, y, zz) in dome if zz == z and x in (13, 14)]
        if ys:
            b.set(13, max(ys) + 1, z, mc("red_nether_bricks"))
            b.set(14, max(ys) + 1, z, mc("red_nether_bricks"))
    # the badge: a golden star on the dome's front
    for (dz, dy) in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, 1), (1, -1), (-1, -1), (0, 2), (2, 0), (-2, 0), (0, -2)):
        z, y = 28 + dz, top + 3 + dy
        xs = [x for (x, yy, zz) in dome if yy == y and zz == z]
        if xs:
            b.set(min(xs), y, z, mc("gold_block") if abs(dz) + abs(dy) < 2 else mc("yellow_concrete"))
    b.set(x0 - 1, top + 1, 28, mc("sea_lantern"))
    b.set(x0 - 2, top, 28, mc("gold_block"))
    b.targets["de garage"] = (x0 + 1, G + 1, 23)


# =====================================================================================================================
# the Beroepenpleintje
# =====================================================================================================================
def pleintje(b):
    z0, z1 = 39, 47
    for x in range(6, 21):
        for z in range(z0, z1 + 1):
            d = math.dist((x, z), (14, 43))
            b.set(x, G, z, KS if int(d) % 3 == 0 else KLINK)
    # the statue: a guh head on a pedestal with three hats on top of each other (a helmet, a cap, a builder's helmet)
    for x in range(13, 16):
        for z in range(42, 45):
            b.set(x, G + 1, z, KS)
    b.set(14, G + 2, 43, KS)
    hy = G + 3
    for x in range(12, 17):
        for z in range(41, 46):
            for y in range(hy, hy + 5):
                if not (x in (12, 16) and z in (41, 45) and y in (hy, hy + 4)):
                    b.set(x, y, z, mc("pink_wool"))
    for facing, (fx, fz) in (("west", (12, 43)), ("east", (16, 43)), ("north", (14, 41)), ("south", (14, 45))):
        st.face_on_plane(b, fx, hy + 2, fz, facing, 2.2, only_features=True)
    for x in (11, 17):
        for y in (hy + 3, hy + 4):
            b.set(x, y, 43, mc("pink_wool"))
            b.set(x, y, 42, mc("pink_wool"))
            b.set(x, y, 44, mc("magenta_terracotta"))
    for x in range(12, 17):                                               # the fire helmet
        for z in range(41, 46):
            if not (x in (12, 16) and z in (41, 45)):
                b.set(x, hy + 5, z, mc("red_concrete"))
    for x in range(13, 16):
        for z in range(42, 45):
            b.set(x, hy + 6, z, mc("red_concrete"))
    b.set(14, hy + 7, 43, mc("blue_concrete"))                            # the police cap
    b.set(13, hy + 7, 43, mc("blue_concrete"))
    b.set(12, hy + 7, 43, mc("black_concrete"))                           # (its visor)
    b.set(14, hy + 8, 43, mc("yellow_concrete"))                          # Bob's helmet
    b.set(14, hy + 9, 43, mc("yellow_concrete"))
    b.set(15, hy + 8, 43, mc("yellow_concrete"))
    # the signpost where the street comes in
    for y in range(G + 1, G + 4):
        b.set(8, y, 43, mc("stripped_spruce_log"), {"axis": "y"})
    b.set(8, G + 4, 43, mc("spruce_planks"))
    sign(b, 7, G + 4, 43, "west", ["straat", "straat2"])
    sign(b, 8, G + 3, 42, "north", ["naar_brandweer"])
    sign(b, 8, G + 3, 44, "south", ["naar_politie", "naar_apotheek"])
    for (x, z, f) in ((18, 40, "south"), (10, 40, "south"), (10, 46, "north")):
        b.bank(x, z, f)
    for (x, z) in ((7, 39), (7, 47), (20, 39)):
        b.bloembak(x, z)
    # a bush in the corner (the Knabbeldief likes it behind there)
    for (x, z) in ((18, 45), (19, 45), (18, 46), (19, 46), (19, 47)):
        b.set(x, G + 1, z, BLAD, {"distance": "1", "persistent": "true", "waterlogged": "false"})
        if (x, z) != (19, 47):
            b.set(x, G + 2, z, BLAD, {"distance": "1", "persistent": "true", "waterlogged": "false"})
    b.set(9, G + 1, 41, st.HOOP, {"vol": "false"})
    b.targets["het beeld"] = (14, G + 1, 40)


# =====================================================================================================================
# the Politiebureautje
# =====================================================================================================================
def politie(b):
    x0, x1, z0, z1 = 7, 20, 49, 74
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G, z, mc("white_concrete") if (x + z) % 2 else mc("gray_concrete"))
    top = G + 10
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = x in (x0, x1) or z in (z0, z1)
            corner = x in (x0, x1) and z in (z0, z1)
            if not edge:
                b.set(x, G + 6, z, mc("birch_planks"))
                continue
            along = z if x in (x0, x1) else x
            for y in range(G + 1, top + 1):
                if corner or y == G + 1:
                    blk = mc("blue_concrete")
                elif y in (G + 4, G + 5):
                    blk = mc("orange_concrete") if (along + y) % 4 == 0 else mc("blue_concrete")    # the politiestriping
                else:
                    blk = mc("white_concrete")
                b.set(x, y, z, blk)
    # the front: a double door, a blue lamp, big windows, a guh face upstairs (its eyes are windows)
    door(b, x0, 61, "east", wood="dark_oak", hinge="left")
    door(b, x0, 62, "east", wood="dark_oak", hinge="right")
    b.set(x0 - 1, G + 5, 60, KS_MUUR, {"up": "true"})
    lantern(b, x0 - 1, G + 4, 60, soul=True)
    b.set(x0 - 1, G + 5, 63, KS_MUUR, {"up": "true"})
    lantern(b, x0 - 1, G + 4, 63, soul=True)
    for z in list(range(51, 57)) + list(range(66, 72)):
        for y in (G + 2, G + 3):
            pane(b, x0, y, z, "glass_pane", ns=True)
    face_x(b, x0, G + 8, 61, "west", 2.9, blocks=window_face_blocks(mc("white_concrete")), skip=lambda x, y, z, role: y <= G + 6)
    for z in (54, 68):
        for y in (G + 8, G + 9):
            pane(b, x0, y, z, "glass_pane", ns=True)
    # (the windows, faces, balconies... on the sides and the back: achterkanten())
    # the knuffelcel (x 14..19, z 50..55): bars to the hall, a pink bed, cushions ("Mika's worden hier alleen geknuffeld")
    for z in range(50, 56):
        for y in range(G + 1, G + 6):
            b.set(14, y, z, mc("iron_bars") if 51 <= z <= 54 and y <= G + 3 else mc("white_concrete"))
    for x in range(15, 20):
        for y in range(G + 1, G + 6):
            b.set(x, y, 56, mc("white_concrete"))
    b.air(14, G + 1, 53, 14, G + 2, 53)
    b.set(14, G + 1, 53, mc("iron_door"), {"facing": "east", "half": "lower", "hinge": "left", "open": "true", "powered": "false"})
    b.set(14, G + 2, 53, mc("iron_door"), {"facing": "east", "half": "upper", "hinge": "left", "open": "true", "powered": "false"})
    b.set(18, G + 1, 50, mc("pink_bed"), {"facing": "north", "part": "head", "occupied": "false"})
    b.set(18, G + 1, 51, mc("pink_bed"), {"facing": "north", "part": "foot", "occupied": "false"})
    b.set(16, G + 1, 50, "guhs:pink_kussen", {"facing": "south"})
    b.set(19, G + 1, 54, "guhs:magenta_kussen", {"facing": "west"})
    b.set(15, G + 1, 55, mc("potted_pink_tulip"))
    lantern(b, 17, G + 5, 52)
    # the office (x 15..19, z 57..66): a desk, a map table, bookshelves, the GEZOCHT poster in the back wall
    b.set(17, G + 1, 61, "guhs:guh_tafel", {"facing": "west"})
    b.set(18, G + 1, 61, "guhs:guh_stoel", {"facing": "west"})
    b.set(17, G + 2, 61, LAMP, {"hanging": "false", "waterlogged": "false"})
    b.set(19, G + 1, 58, mc("cartography_table"))
    for z in (63, 64, 65):
        b.set(19, G + 1, z, mc("bookshelf"))
        b.set(19, G + 2, z, mc("bookshelf"))
    poster = ["kkkkk", "kPPPk", "PbPbP", "PPmPP", ".PPP."]
    colours = {"k": "white_wool", "P": "pink_wool", "b": "black_wool", "m": "magenta_wool", ".": "white_wool"}
    for r, row in enumerate(poster):
        for c, ch in enumerate(row):
            b.set(x1, G + 5 - r, 58 + c, mc(colours[ch]))
    # the knabbelkluis (x 15..19, z 67..73): iron walls, the round door wide open, empty shelves, a few crumbs...
    for x in range(15, 20):
        for y in range(G + 1, G + 6):
            b.set(x, y, 67, mc("iron_block"))
    for z in range(67, 74):
        for y in range(G + 1, G + 6):
            b.set(15, y, z, mc("iron_block"))
    for x in range(16, 20):
        for z in range(68, 74):
            b.set(x, G + 5, z, mc("iron_block"))
    b.air(15, G + 1, 70, 15, G + 3, 71)
    for (z, y) in ((69, G + 1), (69, G + 2), (69, G + 3), (72, G + 1), (72, G + 2), (72, G + 3)):
        b.set(14, y, z, mc("iron_block"))                               # the heavy door, swung open
    for z in range(68, 74):
        b.set(19, G + 1, z, mc("barrel"), {"facing": "west", "open": "true"})
    for (x, z) in ((17, 68), (16, 72), (18, 73)):
        b.set(x, G + 1, z, mc("yellow_carpet"))                        # knabbelkruimels
    lantern(b, 17, G + 4, 70)
    b.set(*KLUIS, KLUISPLEK)
    b.targets["de knabbelkluis"] = KLUIS
    # the hall: a balie with a bell, a waiting bench
    for z in range(57, 67):
        if z not in (62,):
            b.set(13, G + 1, z, mc("blue_concrete"))
            b.set(13, G + 2, z, mc("white_carpet"))
    b.set(13, G + 2, 59, mc("bell"), {"attachment": "floor", "facing": "west", "powered": "false"})
    b.bank(8, 52, "east")
    b.bank(8, 53, "east")
    b.set(8, G + 1, 56, mc("potted_allium"))
    for z in (55, 66):
        lantern(b, 10, G + 5, z)
    # up: the ladder by the front wall, the recherchekamer
    for y in range(G + 1, G + 7):
        b.set(x0 + 1, y, 73, st.LADDER, {"facing": "east", "waterlogged": "false"})
    b.set(12, G + 7, 60, "guhs:guh_tafel", {"facing": "south"})
    b.set(12, G + 7, 61, "guhs:guh_tafel", {"facing": "south"})
    b.set(12, G + 8, 60, mc("lantern"), {"hanging": "false", "waterlogged": "false"})
    for (x, z, f) in ((11, 60, "east"), (11, 61, "east"), (13, 60, "west"), (13, 61, "west")):
        b.set(x, G + 7, z, "guhs:guh_stoel", {"facing": f})
    for z in range(55, 67):                                               # the prikbord with red string
        b.set(x1 - 1, G + 8, z, mc("red_wool") if z % 3 == 0 else mc("white_wool"))
    b.set(16, G + 7, 70, "guhs:blue_zitzak", {"facing": "west"})
    for z in (55, 67):
        lantern(b, 13, G + 10, z)
    b.targets["de recherchekamer"] = (14, G + 7, 64)
    # the roof: a giant police cap (a checkered band, a flat navy top that sticks out, a visor over the street, a golden
    # badge, two zwaailichten)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                along = z if x in (x0, x1) else x
                for y in (top + 1, top + 2):
                    b.set(x, y, z, mc("white_concrete") if (along + y) % 2 else mc("blue_concrete"))
            else:
                b.set(x, top + 1, z, mc("blue_concrete"))
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            cornr = x in (x0 - 1, x1 + 1) and z in (z0 - 1, z1 + 1)
            if not cornr:
                b.set(x, top + 3, z, mc("blue_terracotta") if x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1) else mc("blue_concrete"))
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            b.set(x, top + 4, z, mc("blue_concrete"))
    for x in range(x0 + 3, x1 - 2):
        for z in range(z0 + 3, z1 - 2):
            b.set(x, top + 5, z, mc("blue_concrete"))
    for x in range(x0 - 3, x0):                                          # the visor over the street
        for z in range(55, 68):
            b.set(x, top + 2, z, mc("black_concrete") if x == x0 - 1 else mc("blackstone_slab"),
                  {"type": "bottom", "waterlogged": "false"} if x != x0 - 1 else None)
    for (dz, dy) in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (2, 0), (0, -1), (1, -1)):
        b.set(x0 - 2 if dy < 1 and dz in (0, 1) else x0 - 1, top + 3 + dy, 61 + dz, mc("gold_block"))
    b.set(x0 - 1, top + 4, 60, mc("yellow_concrete"))
    b.set(x0 - 1, top + 4, 63, mc("yellow_concrete"))
    for z in (55, 68):
        b.set(13, top + 6, z, mc("sea_lantern"))
        b.set(13, top + 7, z, mc("blue_stained_glass"))
    b.targets["de hal"] = (10, G + 1, 60)


def steeg(b):
    # the bins and a crate in the steegje between the politie and the apotheek
    b.set(18, G + 1, 76, mc("composter"), {"level": "0"})
    b.set(19, G + 1, 76, mc("barrel"), {"facing": "up", "open": "false"})
    b.set(18, G + 1, 78, mc("barrel"), {"facing": "up", "open": "false"})
    b.set(19, G + 2, 76, mc("green_carpet"))
    b.set(8, G + 1, 77, st.HOOP, {"vol": "true"})
    b.bloembak(7, 75)
    b.bloembak(7, 79)


# =====================================================================================================================
# the Apotheekje
# =====================================================================================================================
def apotheek(b):
    x0, x1, z0, z1 = 7, 20, 80, 104
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G, z, mc("white_concrete") if (x + z) % 2 else mc("lime_terracotta"))
    top = G + 8
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = x in (x0, x1) or z in (z0, z1)
            corner = x in (x0, x1) and z in (z0, z1)
            if edge:
                for y in range(G + 1, top + 1):
                    b.set(x, y, z, mc("lime_concrete") if corner or y in (G + 1, top) else mc("white_terracotta"))
    door(b, x0, 92, "east")
    b.set(x0 - 1, G + 4, 91, mc("lime_concrete"))                       # a little canopy over the door
    b.set(x0 - 1, G + 4, 92, mc("lime_concrete"))
    b.set(x0 - 1, G + 4, 93, mc("lime_concrete"))
    for z in list(range(83, 90)) + list(range(95, 102)):
        for y in (G + 2, G + 3, G + 4):
            pane(b, x0, y, z, "glass_pane", ns=True)
        if z in (84, 88, 96, 100):
            b.bloembak(x0 - 1, z)
    # (the windows, faces, balconies... on the sides and the back: achterkanten())
    # the green apotheekkruis over the street: glowing, on a bracket
    for x in range(x0 - 4, x0):
        b.set(x, G + 9, 86, KS_MUUR if x == x0 - 1 else mc("verdant_froglight"), {"up": "true"} if x == x0 - 1 else {"axis": "y"})
    for dx in range(-2, 3):
        for dy in range(-2, 3):
            if dx == 0 or dy == 0:
                b.set(x0 - 3 + dx, G + 7 + dy, 86, mc("verdant_froglight"), {"axis": "y"})
    b.set(x0 - 3, G + 7, 86, mc("white_concrete"))
    # inside: the balie with the medicine shelves behind it, the doctor
    for z in range(85, 101):
        if z != 97:
            b.set(13, G + 1, z, mc("smooth_quartz"))
            b.set(13, G + 2, z, mc("lime_carpet"))
    b.set(13, G + 2, 90, mc("potted_blue_orchid"))
    b.set(13, G + 2, 94, mc("brewing_stand"), {"has_bottle_0": "true", "has_bottle_1": "true", "has_bottle_2": "false"})
    for z in range(82, 103):
        b.set(x1 - 1, G + 1, z, mc("bookshelf") if z % 4 == 0 else mc("barrel"), None if z % 4 == 0 else {"facing": "west", "open": "false"})
        if z % 2:
            b.set(x1 - 1, G + 2, z, mc("brewing_stand"), {"has_bottle_0": "true", "has_bottle_1": str(z % 3 == 0).lower(), "has_bottle_2": "true"})
        else:
            b.set(x1 - 1, G + 2, z, mc("decorated_pot"), {"facing": "west", "cracked": "false", "waterlogged": "false"})
        b.set(x1 - 1, G + 3, z, mc("bookshelf"))
        b.set(x1 - 1, G + 4, z, mc("flower_pot") if z % 3 else mc("potted_fern"))
    # the mengketel for the customers, by the window
    b.set(*KETEL, MENGKETEL, {"facing": "west"})
    b.set(KETEL[0], G + 1, KETEL[2] - 1, mc("potted_fern"))
    b.targets["de mengketel"] = (KETEL[0] - 1, G + 1, KETEL[2] + 1)
    # the ziekenhoekje: a pink bed, tissues, a flower, a little rug; Snotje (a snotterig guhtje) sits by it
    b.set(9, G + 1, 102, mc("pink_bed"), {"facing": "south", "part": "foot", "occupied": "false"})
    b.set(9, G + 1, 103, mc("pink_bed"), {"facing": "south", "part": "head", "occupied": "false"})
    b.set(8, G + 1, 103, mc("white_wool"))
    b.set(8, G + 2, 103, mc("white_carpet"))
    b.set(10, G + 1, 103, mc("potted_poppy"))
    for x in range(10, 13):
        for z in range(100, 103):
            if b.get(x, G + 1, z) is None:
                b.set(x, G + 1, z, mc("pink_carpet"))
    ms = b.h.ms
    sx, sy, sz = SNOTJE
    b.s.entity(sx + 0.5, float(sy), sz + 0.5, ms.guh_nbt(0.5, Variant="normal", NoAI=ms.Byte(1), Invulnerable=ms.Byte(1),
                                                         Rotation=ms.floats(90.0, 0.0),
                                                         CustomName=json.dumps({"translate": "entity.guhs.beroepen_snotje"}),
                                                         NeoForgeData={"guhs_beroepen_snotterig": ms.Byte(1), "guhs_knuffeldal_checked": ms.Byte(1)}))
    b.floors.append(SNOTJE)
    b.targets["Snotje"] = SNOTJE
    for z in (86, 98):
        lantern(b, 10, G + 8, z)
        lantern(b, 16, G + 8, z)
    b.npc(*NPCS["apothekerguh"][0], "apothekerguh", NPCS["apothekerguh"][1])
    # the roof: a round white guh head with ears (green inside), a face on its front and a big vijzel on top
    cx, cz, rx, rz = (x0 + x1) / 2, (z0 + z1) / 2, (x1 - x0) / 2 + 0.5, (z1 - z0) / 2 + 0.5
    dome = []
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            d = math.hypot((x - cx) / rx, (z - cz) / rz)
            hgt = int(round(8.0 * math.sqrt(max(0.0, 1 - min(1.0, d) ** 2))))
            for y in range(top + 1, top + 2 + hgt):
                b.set(x, y, z, mc("white_wool") if (x * 3 + z + y) % 7 else mc("white_concrete_powder"))
                dome.append((x, y, z))
    face_on_dome(b, dome, "west", 92, top + 4, 4.2, window_face_blocks(mc("white_wool"), ring=mc("lime_stained_glass")))
    tops = {}
    for (x, y, z) in dome:
        tops[(x, z)] = max(tops.get((x, z), 0), y)
    for ez in (85, 99):                                                  # the ears
        ey = tops[(13, ez)] + 3
        for dz in range(-2, 3):
            for dy in range(-3, 3):
                if dz * dz + dy * dy <= 6:
                    b.set(13, ey + dy, ez + dz, mc("lime_wool") if dz * dz + dy * dy <= 2 else mc("white_wool"))
                    b.set(14, ey + dy, ez + dz, mc("white_wool"))
        for y in range(tops[(13, ez)] + 1, ey - 2):
            b.set(13, y, ez, mc("white_wool"))
            b.set(14, y, ez, mc("white_wool"))
    vy = tops[(13, 92)] + 1                                               # the vijzel (mortar) and its stamper
    for x in range(12, 16):
        for z in range(90, 94):
            b.set(x, vy, z, mc("smooth_quartz"))
            if x in (12, 15) or z in (90, 93):
                b.set(x, vy + 1, z, mc("quartz_block"))
    b.set(13, vy + 1, 91, mc("lime_concrete_powder"))
    b.set(14, vy + 1, 92, mc("lime_concrete_powder"))
    b.set(14, vy + 1, 91, mc("lime_concrete_powder"))
    for (x, y) in ((14, vy + 2), (14, vy + 3), (15, vy + 3), (15, vy + 4)):
        b.set(x, y, 91, mc("birch_log"), {"axis": "y"})
    b.targets["de balie"] = (11, G + 1, 92)


def kruidentuin(b):
    z0, z1 = 106, 122
    for x in range(7, 21):
        for z in range(z0, z1 + 1):
            b.set(x, G, z, GRAS)
    for z in range(z0, z1 + 1):
        b.set(7, G, z, KLINK)
    beds = []
    for zb in (108, 111, 114):
        for x in range(9, 15):
            b.set(x, G, zb, mc("coarse_dirt"))
            b.set(x, G + 1, zb, SNOTKRUID, {"age": "3"})
            beds.append((x, G + 1, zb))
        b.set(8, G, zb, mc("stripped_spruce_log"), {"axis": "x"})
        b.set(15, G, zb, mc("stripped_spruce_log"), {"axis": "x"})
    for x in range(8, 16):                                                # little paths between the beds
        for z in (109, 110, 112, 113, 115):
            b.set(x, G, z, KLINK if x in (8, 15) or z in (109, 112, 115) else GRAS)
    b.targets["de snotkruidjes"] = (9, G + 1, 109)
    # the greenhouse (x 16..20, z 107..112) with two more snotkruidjes
    for x in range(16, 21):
        for z in range(107, 113):
            edge = x in (16, 20) or z in (107, 112)
            b.set(x, G, z, mc("coarse_dirt") if not edge else KS)
            if edge:
                for y in (G + 1, G + 2, G + 3):
                    b.set(x, y, z, mc("glass"))
            b.set(x, G + 4, z, mc("glass"))
    b.air(16, G + 1, 109, 16, G + 2, 110)
    b.set(16, G, 109, KLINK)
    b.set(16, G, 110, KLINK)
    for x in range(17, 20):
        b.set(x, G + 5, 109, mc("glass"))
        b.set(x, G + 5, 110, mc("glass"))
    for (x, z) in ((18, 108), (18, 111), (19, 108), (19, 111)):
        b.set(x, G + 1, z, SNOTKRUID, {"age": "3" if x == 18 else "1"})
    lantern(b, 18, G + 3, 109)
    # the tuinhuisje (x 16..20, z 115..120): cherry planks, a pluisdak, an open doorway, inside barrels and a seat
    for x in range(16, 21):
        for z in range(115, 121):
            edge = x in (16, 20) or z in (115, 120)
            b.set(x, G, z, mc("cherry_planks"))
            if edge:
                for y in range(G + 1, G + 4):
                    b.set(x, y, z, mc("cherry_planks") if y < G + 3 else mc("pink_terracotta"))
            b.set(x, G + 4, z, DAK_PLAAT, {"type": "bottom"})
    for x in range(17, 20):
        for z in range(116, 120):
            b.set(x, G + 5, z, DAK_PLAAT, {"type": "bottom"})
    b.air(16, G + 1, 117, 16, G + 2, 118)
    b.set(19, G + 1, 116, mc("barrel"), {"facing": "up", "open": "false"})
    b.set(19, G + 1, 119, "guhs:pink_zitzak", {"facing": "west"})
    b.set(17, G + 3, 116, LAMP, {"hanging": "true", "waterlogged": "false"})
    pane(b, 18, G + 2, 115, ns=False)
    b.set(15, G + 1, 117, KS_FACE, {"facing": "west", "stemming": "3"})
    b.gezichten += 1
    b.bank(10, 118, "north")
    b.bank(12, 118, "north")
    b.set(9, G + 1, 121, mc("bee_nest"), {"facing": "north", "honey_level": "0"})
    b.grasveld(8, 116, 15, 122, 0.3)
    return beds


def park(b):
    for x in range(7, 21):
        for z in range(123, 132):
            b.set(x, G, z, GRAS)
    for x in range(8, 15):
        for z in range(124, 131):
            d = math.hypot((x - 11) / 3.4, (z - 127) / 3.0)
            if d <= 1.0:
                b.set(x, G, z, mc("water") if d < 0.72 else KS)
                if d < 0.72:
                    b.set(x, G - 1, z, mc("clay"))
    b.set(11, G, 127, KS)
    b.boompje(18, 127, height=5, r=2.6)
    b.bank(16, 124, "south")
    sign(b, 3, G + 1, 131, "north", ["einde", "einde2"], wall=False)
    b.grasveld(7, 123, 20, 131, 0.25)
    b.targets["het parkje"] = (16, G + 1, 130)


# =====================================================================================================================
# 2.9 polish: every side of the three buildings is nice too (the sides along the paths and the backs on the achterpad)
# =====================================================================================================================
AP = 21                                       # the achterpad: nothing below G + 4 there, it stays walkable


def ramen(b, wall, at, alongs, ys, glass="glass_pane"):
    """Windows in a wall: wall "x" (the plane x = at, along z) or "z" (the plane z = at, along x)."""
    for a in alongs:
        for y in ys:
            if wall == "x":
                b.set(at, y, a, mc(glass), st.pane(True))
            else:
                b.set(a, y, at, mc(glass), st.pane(False))


def gezichtje(b, x, y, z, facing, stemming):
    b.set(x, y, z, KS_FACE, {"facing": facing, "stemming": str(stemming)})
    b.gezichten += 1


def fence(b, x, y, z, wood, **sides):
    props = {k: str(sides.get(k, False)).lower() for k in ("north", "south", "east", "west")}
    props["waterlogged"] = "false"
    b.set(x, y, z, mc(f"{wood}_fence"), props)


def balkon(b, za, zb, yv, deur_z, wood, deur_hout, soul=False):
    """A balcony on a back wall (x = 20) over the achterpad: a floor of slabs at yv (x 21..22), a railing, flower boxes,
    lanterns hanging under it, and a door out of the upstairs room."""
    for z in range(za, zb + 1):
        for x in (AP, AP + 1):
            b.set(x, yv, z, mc(f"{wood}_slab"), {"type": "top", "waterlogged": "false"})
        fence(b, AP + 1, yv + 1, z, wood, north=z > za, south=z < zb, west=z in (za, zb))
    for z in (za, zb):
        fence(b, AP, yv + 1, z, wood, east=True)
    for z in (za + 1, zb - 1):
        if z != deur_z:
            b.bloembak(AP, z, y=yv + 1)
    door(b, 20, deur_z, "west", wood=deur_hout, hinge="left", y=yv + 1)
    for z in (za + 1, zb - 1):
        lantern(b, AP, yv - 1, z, soul=soul)


def regenketting(b, x, z, y_top):
    """A rain chain from the gutter down into a water butt (a cauldron full of rain)."""
    b.set(x, G + 1, z, mc("water_cauldron"), {"level": "3"})
    for y in range(G + 2, y_top + 1):
        b.set(x, y, z, mc("chain"), {"axis": "y", "waterlogged": "false"})


def achterkant_kazerne(b):
    top = G + 12
    bricks = window_face_blocks(mc("bricks"))
    skip = lambda x, y, z, role: y <= G + 7 or y >= top
    # the back (the achterpad): a balcony off the dormitory, a big guh face with window eyes, windows by the garage
    face_x(b, 20, G + 10, 27, "east", 3.1, blocks=bricks, skip=skip)
    balkon(b, 31, 36, G + 7, 34, "spruce", "spruce")
    ramen(b, "x", 20, (24, 25, 33, 34, 35), (G + 2, G + 3))
    ramen(b, "x", 20, (32,), (G + 9, G + 10))
    gezichtje(b, 20, G + 1, 29, "east", 0)
    for z in range(23, 27):                                          # a red-white awning over the garage windows
        b.set(AP, G + 4, z, mc("red_concrete") if z % 2 else mc("white_concrete"))
    # the north side (the path to the oefenterrein) and the south side (the pleintje): a face with window eyes upstairs
    face_on = lambda cx, z, facing, R: st.face_on_plane(b, cx, G + 10, z, facing, R, blocks=bricks, only_features=True, skip=skip)
    face_on(11, 18, "north", 2.4)
    ramen(b, "z", 18, (9, 10, 13, 14), (G + 2, G + 3))
    face_on(13, 38, "south", 2.6)
    ramen(b, "z", 38, (9, 17), (G + 9, G + 10))
    ramen(b, "z", 38, (10, 11, 15, 16), (G + 2, G + 3))
    for x in (12, 15):
        b.bloembak(x, 39)
    for y in range(G + 2, top):                                      # a red drainpipe into a water butt
        b.set(9, y, 39, mc("red_nether_brick_wall"), {"up": "true", "north": "none", "south": "none", "east": "none",
                                                     "west": "none", "waterlogged": "false"})
    b.set(9, G + 1, 39, mc("water_cauldron"), {"level": "3"})


def achterkant_politie(b):
    top = G + 10
    win = window_face_blocks(mc("white_concrete"))
    skip = lambda x, y, z, role: y <= G + 6 or y > top
    # the back: barred windows of the knuffelcel, the office windows, a guh face upstairs, a balcony with blue lamps
    ramen(b, "x", 20, (51, 52, 53), (G + 2, G + 3), glass="iron_bars")
    ramen(b, "x", 20, (57, 66), (G + 2, G + 3))
    ramen(b, "x", 20, (52, 53), (G + 8, G + 9))
    face_x(b, 20, G + 8, 61, "east", 2.9, blocks=win, skip=skip)
    balkon(b, 67, 72, G + 6, 69, "dark_oak", "dark_oak", soul=True)
    gezichtje(b, 20, G + 1, 56, "east", 1)
    # the north side (the path) and the south side (the steegje): faces with window eyes, windows, cell bars
    for (z, facing) in ((49, "north"), (74, "south")):
        st.face_on_plane(b, 13, G + 8, z, facing, 2.6, blocks=win, only_features=True, skip=skip)
        ramen(b, "z", z, (9, 18), (G + 8, G + 9))
    ramen(b, "z", 49, (9, 10, 11), (G + 2, G + 3))
    ramen(b, "z", 49, (16, 17), (G + 2, G + 3), glass="iron_bars")
    ramen(b, "z", 74, (10, 11), (G + 2, G + 3))
    regenketting(b, 20, 75, top + 2)
    for x in (11, 14):
        b.bloembak(x, 75)


def achterkant_apotheek(b):
    x0, x1, z0, z1, top = 7, 20, 80, 104, G + 8
    win = window_face_blocks(mc("white_terracotta"), ring=mc("lime_stained_glass"))
    skip = lambda x, y, z, role: y <= G + 1 or y >= top
    # the back: green pilasters, a big glowing apotheekkruis, high windows over the shelves with flower boxes on sills
    for z in (86, 98):
        for y in range(G + 1, top):
            b.set(x1, y, z, mc("lime_concrete"))
    for z in range(89, 96):
        for y in range(G + 2, G + 9):
            if y < top:
                b.set(x1, y, z, mc("white_concrete"))
    for dz in range(-2, 3):
        for dy in range(-2, 3):
            if dz == 0 or dy == 0:
                if dz == 0 and dy == 0:
                    b.set(x1, G + 5, 92, mc("verdant_froglight"), {"axis": "y"})
                else:
                    b.set(x1, G + 5 + dy, 92 + dz, mc("lime_concrete"))
    for zs in ((83, 84), (100, 101)):
        ramen(b, "x", x1, zs, (G + 6, G + 7), glass="lime_stained_glass_pane")
        for z in zs:
            b.set(AP, G + 4, z, mc("smooth_quartz_slab"), {"type": "top", "waterlogged": "false"})
            b.bloembak(AP, z, y=G + 5)
    for z in (88, 96):
        gezichtje(b, x1, G + 1, z, "east", 2)
    # the north side (the steegje) and the south side (Snotje's corner): faces with window eyes, windows, flowers
    for (cx, z, facing) in ((13, z0, "north"), (14, z1, "south")):
        st.face_on_plane(b, cx, G + 5, z, facing, 2.6, blocks=win, only_features=True, skip=skip)
    ramen(b, "z", z0, (9, 17), (G + 3, G + 4))
    ramen(b, "z", z1, (9, 10, 18), (G + 3, G + 4))
    for x in (10, 16):
        b.bloembak(x, z0 - 1)
    regenketting(b, 20, 79, top)
    # a cornice all round under the round roof (a green band and a white ledge)
    ring = [(x, z) for x in range(x0 - 1, x1 + 2) for z in (z0 - 1, z1 + 1)] + \
           [(x, z) for x in (x0 - 1, x1 + 1) for z in range(z0, z1 + 1)]
    for (x, z) in ring:
        if b.get(x, top, z) is None:
            b.set(x, top, z, mc("lime_concrete"))
        if b.get(x, top + 1, z) is None:
            b.set(x, top + 1, z, mc("smooth_quartz_slab"), {"type": "bottom", "waterlogged": "false"})
    # the round roof's back: a green cross too
    dome = [p for p, (blk, _, _) in b.s.blocks.items() if x0 <= p[0] <= x1 and z0 <= p[2] <= z1 and p[1] > top
            and blk in (mc("white_wool"), mc("white_concrete_powder"))]
    for dz in range(-2, 3):
        for dy in range(-2, 3):
            if dz == 0 or dy == 0:
                y, z = top + 3 + dy, 92 + dz
                xs = [x for (x, yy, zz) in dome if yy == y and zz == z]
                if xs:
                    if dz == 0 and dy == 0:
                        b.set(max(xs), y, z, mc("verdant_froglight"), {"axis": "y"})
                    else:
                        b.set(max(xs), y, z, mc("lime_concrete"))


def achterkanten(b):
    achterkant_kazerne(b)
    achterkant_politie(b)
    achterkant_apotheek(b)


# =====================================================================================================================
def bouw(h):
    b = st.Bouw(h, (W, H, D), "beroepenstraat")
    grond(b)
    heggen(b)
    oefenterrein(b)
    kazerne(b)
    pleintje(b)
    politie(b)
    steeg(b)
    apotheek(b)
    b.plants = kruidentuin(b)
    park(b)
    achterkanten(b)
    lampen(b)
    for (x, y, z) in VERSTOP:
        b.set(x, y, z, VERSTOPPLEK)
    for i, p in enumerate(VERSTOP):
        b.targets[f"verstopplek {i}"] = p
    for kind in ("brandweerguh", "politieguh"):
        (x, y, z), facing = NPCS[kind]
        b.npc(x, y, z, kind, facing)
    # the jigsaw on the west side where the town's street hangs on (name = target = the town's free connector)
    b.jigsaw(0, G, JIG_Z, "west_up", st.VRIJ_NAAM, st.VRIJ_NAAM, "minecraft:empty", KLINK)
    b.starts = [(1, G + 1, JIG_Z)]
    b.s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=H)
    return b


def check_straat(b):
    problems = []
    js = b.jigsaws
    if len(js) != 1 or js[0][0] != (0, G, JIG_Z) or js[0][1] != st.VRIJ_NAAM or js[0][4] != "west_up":
        problems.append(f"beroepenstraat: jigsaw {js}")
    for z in range(JIG_Z - 1, JIG_Z + 2):                         # the doorway where the town's street comes in is open
        for y in range(G + 1, G + 5):
            if b.get(0, y, z) not in (None, "minecraft:air"):
                problems.append(f"beroepenstraat: {b.get(0, y, z)} blocks the way in at {(0, y, z)}")
    kinds = sorted(e[3]["Kind"] for e in b.s.entities if e[3]["id"] == "guhs:guh_npc")
    if kinds != ["apothekerguh", "brandweerguh", "politieguh"]:
        problems.append(f"beroepenstraat: NPCs {kinds}")
    snotjes = [e for e in b.s.entities if e[3]["id"] == "guhs:guh" and "guhs_beroepen_snotterig" in e[3].get("NeoForgeData", {})]
    if len(snotjes) != 1:
        problems.append("beroepenstraat: Snotje is missing")
    counts = {}
    for (blk, props, _) in b.s.blocks.values():
        counts[blk] = counts.get(blk, 0) + 1
    for blk, want in ((VUUR, len(PITS)), (GUHTJEPLEK, 1), (KLUISPLEK, 1), (VERSTOPPLEK, len(VERSTOP)), (MENGKETEL, 1)):
        if counts.get(blk, 0) != want:
            problems.append(f"beroepenstraat: {counts.get(blk, 0)} x {blk}, want {want}")
    if counts.get(SNOTKRUID, 0) < 12:
        problems.append(f"beroepenstraat: only {counts.get(SNOTKRUID, 0)} snotkruidjes")
    if b.gezichten + b.grote_gezichten < 14:
        problems.append(f"beroepenstraat: only {b.gezichten} + {b.grote_gezichten} guh faces")
    # the Knabbeldief's hiding places stand free on a floor, far enough apart
    for p in VERSTOP:
        x, y, z = p
        if not st.solid(b.get(x, y - 1, z)) or not st.passable(b.get(x, y + 1, z)):
            problems.append(f"beroepenstraat: hiding place {p} is not free on a floor")
    # the guhtje's spot in the tree: on the branch, air above, the ladder next to it
    gx, gy, gz = GUHTJE
    if b.get(gx, gy - 1, gz) != STAM or b.get(gx, gy + 1, gz) not in (None, "minecraft:air") or b.get(gx + 1, gy - 1, gz) != st.LADDER:
        problems.append("beroepenstraat: the guhtje's spot in the tree is wrong")
    for (x, z) in PITS:
        if b.get(x, G + 1, z) != VUUR or b.get(x, G + 2, z) not in (None, "minecraft:air"):
            problems.append(f"beroepenstraat: the pit at {(x, z)} has no room for its flames")
    return problems


def build(h):
    b = bouw(h)
    problems = st.check(b, check_straat)
    if problems:
        raise SystemExit("beroepenstraat geometry check failed:\n  " + "\n  ".join(problems[:80]))
    b.s.save(NAAM)
    return b


if __name__ == "__main__":
    import os
    import sys
    import types
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    import make_structures as ms
    stub = types.SimpleNamespace(mc=ms.mc, Structure=ms.Structure, ms=ms)
    bb = bouw(stub)
    found = st.check(bb, check_straat)
    print("beroepenstraat:", len(bb.s.blocks), "blocks,", bb.walkable, "walkable,", bb.gezichten, "faces,", bb.grote_gezichten, "big faces")
    print("\n".join(found[:120]) if found else "geometry check ok")
