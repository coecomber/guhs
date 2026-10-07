"""
einde2, slice bouw_zij2: the Kyoshi-dorp (Schimmelkaasvlakte). Build only: the fan dance, the make-up and the villagers'
talk are a later phase, which reads the markers. Two otter-pinguinguhs live at the pond (placed for real).

An island of blue cheese under giant glowing mushrooms. You land in the south, walk through the gate and up the village
street between four houses with sea-green roofs, to the square. North of the square stands the statue: a giant guh in
Kyoshi's green robe, white face paint and red eye shadow, the golden headdress on her head and a golden fan on each
flank. West of the square the house of the dorpsoudste, east the dojo with the fan-dance floor (11 x 11, flat) and the
schminktafel. South-west the pond, where something long and dark is definitely not looking at you.

    maak(h) -> (the kit.Bouw, its problems); bouw(h) raises SystemExit on a problem
"""
import math
import random

import features.einde2_kit as kit
from features import einde2_bouw_zij2_gereedschap as g
from features import guheinde_bouw as gb

NAAM = "kyoshi_dorp"
W, H, D = 128, 72, 128
Y = 152                                   # the world y of template y 0
G = 30                                    # the ground: the layer of the surface blocks (you stand one higher)
CX, CZ = 64, 64
KAAS, STENEN, TRAP, PLAAT, MUUR = "guhs:schimmelkaas", "guhs:schimmelkaas_stenen", "guhs:schimmelkaas_stenen_trap", \
    "guhs:schimmelkaas_stenen_plaat", "guhs:schimmelkaas_stenen_muur"
PADDO, HOED, STEEL = "guhs:schimmelpaddenstoel", "guhs:schimmelhoed", "guhs:schimmelsteel"
DAK, DAKPLANK, DAKPLAAT = "warped_stairs", "warped_planks", "warped_slab"
PAAL, PLANK, WAND = "dark_oak_log", "spruce_planks", "white_terracotta"
STRAAT = (62, 66)                         # the village street: x from .. to
PLEIN = (64, 68, 8)                       # the square: centre x, z and radius
DOJO = (84, 60, 108, 80)
DANSVLOER = (91, 65, 101, 75)
OUDSTE = (34, 61, 48, 71)
HUIZEN = [(47, 79, 57, 87, "east", "Huize Natte Sok"), (71, 79, 81, 87, "west", "Het Waaierhuis"),
          (47, 91, 57, 99, "east", "Vis & Vads"), (71, 91, 81, 99, "west", "Bij Tante Koi")]
VIJVER = (36, 90, 8.5, 6.5)


def eiland(b):
    grond = g.eiland(b, CX, CZ, 54, 54, G, 22, KAAS, [(KAAS, 80), (STENEN, 8), ("guhs:kaaskorst", 12)], NAAM, rafel=0.13, onder=KAAS)
    # the landing terrace in the south is masonry down to the rock: it holds, whatever the frayed rim does there
    for x in range(58, 71):
        for z in range(102, 117):
            for y in range(G - 5, G + 1):
                if (x, z) not in grond or y == G:
                    b.set(x, y, z, STENEN if y < G else KAAS)
            grond[(x, z)] = G
    return grond


def bestrating(b, grond):
    """The street, the square and the paths to the doors: blue-cheese bricks with a cobble of plain cheese in them."""
    rng = random.Random(kit.seed(NAAM + "/straat"))
    steen = lambda: gb.pick(rng, [(STENEN, 70), ("guhs:kaaskorst_stenen", 12), (KAAS, 18)])   # noqa: E731
    for z in range(76, 105):
        for x in range(STRAAT[0], STRAAT[1] + 1):
            b.set(x, G, z, steen())
    px, pz, r = PLEIN
    for x in range(px - r - 1, px + r + 2):
        for z in range(pz - r - 1, pz + r + 2):
            d = math.hypot(x - px, z - pz)
            if d <= r + 0.3:
                b.set(x, G, z, "guhs:kaaskorst_stenen" if abs(d - r) < 0.8 or d < 1.2 else steen())
    for x0, z0, x1, z1, kant, _naam in HUIZEN:                 # from every door to the street
        z = (z0 + z1) // 2
        for x in (range(x1 + 1, STRAAT[0]) if kant == "east" else range(STRAAT[1] + 1, x0)):
            for dz in (-1, 0, 1):
                b.set(x, G, z + dz, steen())
    for x in range(OUDSTE[2] + 1, px - r):                     # the elder's door to the square
        for dz in (-1, 0, 1):
            b.set(x, G, 66 + dz, steen())
    for x in range(px + r, DOJO[0]):                           # the square to the dojo doors
        for z in (68, 69, 70, 71):
            b.set(x, G, z, steen())
    for z in range(58, pz - r + 1):                            # the square to the foot of the statue
        for x in range(61, 68):
            b.set(x, G, z, steen())
    # the landing pad
    for x in range(59, 70):
        for z in range(105, 116):
            rand = x in (59, 69) or z in (105, 115)
            b.set(x, G, z, "green_terracotta" if rand else STENEN)
    for x, z in ((64, 110), (63, 110), (65, 110), (64, 109), (64, 111)):
        b.set(x, G, z, "pink_concrete")
    b.zone("aankomst", 59, G + 1, 105, 69, G + 9, 115, wie="het landingsterras van het Kyoshi-dorp")
    b.prop("aankomst_speler", 64, G + 1, 110)
    b.prop("uitzicht", 64, G + 34, 152, wie="van het zuiden, hoog: over de straat heen recht in het gezicht van het beeld")


def poort(b):
    """The village gate: two red posts, two beams, a lampion under each end."""
    for x in (60, 68):
        for y in range(G + 1, G + 7):
            b.set(x, y, 103, "red_concrete")
        b.set(x, G + 7, 103, "dark_oak_log", {"axis": "x"})
    for x in range(58, 71):
        b.set(x, G + 7, 103, "dark_oak_log", {"axis": "x"})
        if 59 <= x <= 69:
            b.set(x, G + 8, 103, "red_concrete")
    for x in range(57, 72):
        g.plaat(b, x, G + 9, 103, "dark_oak_slab")
    for x in (58, 70):
        g.hanglamp(b, x, G + 6, 103, ketting=0, lamp="guhs:lampion_roze")
    b.sign(64, G + 6, 104, "south", ["Kyoshi-dorp", "Guhvatar-vrij", "sinds vorige", "week. Njeg."])
    b.set(64, G + 6, 103, "dark_oak_planks")


def huis(b, x0, z0, x1, z1, deur, naam, groot=False):
    """A village house: posts of dark oak, a plank skirting, white panels with paper windows, a sea-green gable roof,
    one room inside (bed, table, chest of drawers, a rug, a lantern under the ridge). deur = the side the door is on."""
    hoog = 4 if groot else 3
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            wand = x in (x0, x1) or z in (z0, z1)
            hoek = x in (x0, x1) and z in (z0, z1)
            b.set(x, G, z, STENEN if wand else PLANK)
            for y in range(G + 1, G + 1 + hoog):
                if hoek:
                    b.set(x, y, z, PAAL, {"axis": "y"})
                elif wand:
                    langs = (x - x0) if z in (z0, z1) else (z - z0)
                    if y == G + 1:
                        b.set(x, y, z, PLANK)
                    elif langs % 3 == 2 and y == G + 2:
                        b.set(x, y, z, "white_stained_glass_pane")
                    else:
                        b.set(x, y, z, WAND)
    as_ = "x" if (x1 - x0) >= (z1 - z0) else "z"
    nok = g.zadeldak(b, x0, z0, x1, z1, G + hoog, as_, DAK, DAKPLANK)
    # the door, a lampion next to it, the name on the other side
    dx, dz = gb.DIRS[deur]
    mx, mz = (x0 + x1) // 2, (z0 + z1) // 2
    px, pz = (x1 if deur == "east" else x0 if deur == "west" else mx), (z1 if deur == "south" else z0 if deur == "north" else mz)
    g.deur(b, px, G + 1, pz, deur)
    b.set(px, G + 3, pz, WAND)
    zx, zz = (0, 1) if dx else (1, 0)
    b.set(px + dx + zx * 2, G + 1, pz + dz + zz * 2, "spruce_fence")
    b.set(px + dx + zx * 2, G + 2, pz + dz + zz * 2, "guhs:lampion_geel", gb.lamp(False))
    b.sign(px + dx - zx, G + 2, pz + dz - zz, deur, [naam, "", "", ""])
    # inside
    for x in range(x0 + 2, x1 - 1):
        for z in range(z0 + 2, z1 - 1):
            b.set(x, G + 1, z, "green_carpet" if (x + z) % 2 else "lime_carpet")
    b.set(px - dx, G + 1, pz - dz, gb.AIR)
    binnen = lambda u, v: (x0 + 1 + u, z0 + 1 + v) if deur in ("east", "south") else (x1 - 1 - u, z1 - 1 - v)   # noqa: E731
    bx, bz = binnen(0, 0)
    g.bed(b, bx, G + 1, bz, "east" if deur in ("east", "south") else "west", "green")
    tx, tz = binnen(0, (z1 - z0 - 2))
    b.set(tx, G + 1, tz, "guhs:guh_tafel", {"facing": "north"})
    sx, sz = binnen(1, (z1 - z0 - 2))
    b.set(sx, G + 1, sz, "guhs:guh_stoel", {"facing": "west" if deur in ("east", "south") else "east"})
    kx, kz = binnen(3, 0)
    b.set(kx, G + 1, kz, "barrel", {"facing": "up", "open": "false"})
    b.set(kx, G + 2, kz, "flower_pot")
    g.hanglamp(b, mx, nok - 1, mz, ketting=max(1, nok - 1 - (G + hoog)))
    return px + dx, pz + dz


def huizen(b):
    teksten = ["een dorpsguh voor Huize Natte Sok", "een dorpsguh voor Het Waaierhuis", "een dorpsguh voor Vis & Vads",
               "een dorpsguh voor Bij Tante Koi"]
    for i, (x0, z0, x1, z1, kant, naam) in enumerate(HUIZEN, 1):
        dx, dz = huis(b, x0, z0, x1, z1, kant, naam)
        stap = 2 if kant == "east" else -2
        b.plek(f"bewoner_{i}", dx + stap, G + 1, dz + 2, yaw=-90.0 if kant == "east" else 90.0, wie=teksten[i - 1])
    # the house of the dorpsoudste: bigger, a porch to the square, a bookcase and his chair inside
    x0, z0, x1, z1 = OUDSTE
    huis(b, x0, z0, x1, z1, "east", "Dorpsoudste", groot=True)
    for z in range(z0 + 1, z1):
        if z not in (65, 66, 67):
            b.set(x1 + 1, G + 1, z, "spruce_fence")
    for z in (z0, z1):
        for y in range(G + 1, G + 4):
            b.set(x1 + 2, y, z, "spruce_fence")
        b.set(x1 + 2, G + 4, z, "guhs:lampion_roze", gb.lamp(False))
        b.set(x1 + 1, G + 1, z, "spruce_fence")
        b.set(x1 + 2, G + 1, z, "spruce_fence")
    for x in range(x0 + 1, x0 + 5):
        b.set(x, G + 1, z1 - 1, "bookshelf")
        b.set(x, G + 2, z1 - 1, "bookshelf")
    b.set(x0 + 6, G + 1, z0 + 5, "guhs:guh_bank", {"facing": "east"})
    b.plek("dorpsoudste", x1 + 4, G + 1, 68, yaw=-90.0, wie="de dorpsoudste-guh, voor zijn veranda aan het plein", kind="KYOSHI_DORPSOUDSTE")
    b.sign(x1 + 2, G + 1, z1 + 1, "east", ["Spreekuur:", "na het dutje.", "Het dutje:", "nu."], wall=False)


def beeld(b):
    """The giant statue of Kyoshi-guh on its plinth, looking south down the village street."""
    px0, pz0, px1, pz1 = 46, 14, 82, 58
    for x in range(px0, px1 + 1):
        for z in range(pz0, pz1 + 1):
            rand = x in (px0, px1) or z in (pz0, pz1)
            b.set(x, G + 1, z, STENEN if not rand else "guhs:kaaskorst_stenen")
            if not rand and not (x in (px0 + 1, px1 - 1) or z in (pz0 + 1, pz1 - 1)):
                b.set(x, G + 2, z, "guhs:kaaskorst_stenen" if (x + z) % 7 == 0 else STENEN)
            for y in range(G - 3, G + 1):                     # (the plinth goes down into the rock: the island's rim is thin here)
                if b.get(x, y, z) is None:
                    b.set(x, y, z, KAAS)
    # steps up the front, in the middle
    for x in range(61, 68):
        g.trap(b, x, G + 1, pz1 + 1, TRAP, "north")
        g.trap(b, x, G + 2, pz1 - 1, TRAP, "north")
        b.set(x, G + 1, pz1, STENEN)
        b.set(x, G + 2, pz1 - 1 - 1, STENEN)

    def kleed(deel, pm, blok):
        xm, ym, zm = pm
        if deel == "kop":
            if blok in g.GUH_OOG:
                return blok
            if zm < -12:                                     # (the little nose: lipstick)
                return "red_concrete"
            if zm < -10.9 and 9.4 <= ym <= 12.2 and 1.5 <= abs(xm) <= 6.6:
                return "red_concrete"                        # (the eye shadow, in a wing above each eye)
            return "white_concrete"
        if deel == "oor":
            return "white_concrete" if blok != "minecraft:purpur_block" else "pink_concrete"
        if deel == "poot":
            return "brown_terracotta"                        # (the boots: the biggest of the four nations)
        if deel == "staart":
            return "white_wool"
        if 3.0 <= zm <= 5.0:
            return "yellow_concrete"                         # (the sash)
        if ym >= 9.6:
            return "green_concrete"                          # (the armour over the shoulders)
        return "green_terracotta" if (int(zm) + int(ym)) % 5 else "lime_terracotta"

    schaal = 1.2
    (nx, ny, nz), naar = g.voxel_guh_in(b, 64 - 14, G + 3, 18, schaal, naar_zuid=True, kleur=kleed)
    # the headdress: a golden band over the head with the fan-shaped crest standing on it
    for xm10 in range(-60, 61, 4):
        for zm10 in (-90, -82, -74):
            x, y, z = naar(xm10 / 10, 15.2, zm10 / 10)
            b.set(x, y, z, "gold_block")
    cx, cy, cz = naar(0, 16.0, -8.2)
    for dx in range(-6, 7):
        for dy in range(0, 7):
            d = math.hypot(dx, dy)
            if d <= 6.3:
                b.set(cx + dx, cy + dy, cz, "red_concrete" if d < 2.2 else ("gold_block" if int(math.degrees(math.atan2(dy, dx)) // 22) % 2 == 0
                                                                           or d > 5.2 else "yellow_concrete"))
    # a golden fan on each flank, open, standing up
    for kant in (-1, 1):
        fx, fy, fz = naar(kant * 7.6, 6.0, 4.5)
        for dz in range(-7, 8):
            for dy in range(0, 8):
                d = math.hypot(dz, dy)
                if d <= 7.3:
                    b.set(fx, fy + dy, fz + dz, "gold_block" if d > 6.2 or abs(dz) == abs(dy) or dz == 0 else "yellow_concrete")
    b.prop("kyoshi_beeld", 64, G + 1, pz1 + 1, wie="de voet van het reuzenbeeld van Kyoshi-guh: de onderste trede, midden voor")
    b.sign(60, G + 1, pz1 + 1, "south", ["KYOSHI-GUH", "Grootste voeten", "van de vier", "volkeren. Njeg."])
    b.sign(68, G + 1, pz1 + 1, "south", ["Niet op het", "beeld klimmen.", "(Zij klimt", "wel op jou.)"])
    for x in (px0 + 3, px1 - 3):
        for z in (pz0 + 3, pz1 - 3, (pz0 + pz1) // 2):
            b.set(x, G + 2, z, "guhs:guh_kristal_lamp")
    for x in (58, 70):
        b.set(x, G + 2, pz1 - 2, "guhs:guh_kristal_lamp")
    return nx, ny, nz


def dojo(b):
    """The dojo of the Kyoshi-krijgsterguhs: one hall, the fan-dance floor in the middle, the schminktafel in a corner."""
    x0, z0, x1, z1 = DOJO
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            wand = x in (x0, x1) or z in (z0, z1)
            b.set(x, G, z, STENEN if wand else "bamboo_planks")
            for y in range(G + 1, G + 6):
                langs = (x - x0) if z in (z0, z1) else (z - z0)
                if wand and (langs % 4 == 0 or (x in (x0, x1) and z in (z0, z1))):
                    b.set(x, y, z, PAAL, {"axis": "y"})
                elif wand:
                    b.set(x, y, z, PLANK if y == G + 1 else ("white_stained_glass_pane" if y in (G + 3, G + 4) and langs % 4 == 2 else WAND))
    nok = g.zadeldak(b, x0, z0, x1, z1, G + 5, "x", DAK, DAKPLANK, top=6, plat=DAKPLAAT)
    for x in range(x0 - 1, x1 + 2):                           # the ridge: a row of gold on the flat top
        if (x - x0) % 3 == 0:
            b.set(x, nok + 1, (z0 + z1) // 2, "gold_block")
        else:
            g.plaat(b, x, nok + 1, (z0 + z1) // 2, DAKPLAAT)
    # the dance floor: bamboo mosaic in a dark frame, flat, nothing on it
    fx0, fz0, fx1, fz1 = DANSVLOER
    for x in range(fx0 - 1, fx1 + 2):
        for z in range(fz0 - 1, fz1 + 2):
            rand = x in (fx0 - 1, fx1 + 1) or z in (fz0 - 1, fz1 + 1)
            b.set(x, G, z, "dark_oak_planks" if rand else ("bamboo_mosaic" if (x + z) % 2 else "bamboo_planks"))
    b.set((fx0 + fx1) // 2, G, (fz0 + fz1) // 2, "gold_block")
    b.zone("dojo", x0 + 1, G + 1, z0 + 1, x1 - 1, G + 11, z1 - 1, wie="de dojo van de Kyoshi-krijgsterguhs")
    b.zone("dansvloer", fx0, G + 1, fz0, fx1, G + 4, fz1, wie="de waaierdansvloer: 11 x 11, vlak")
    b.prop("dans_speler", (fx0 + fx1) // 2, G + 1, (fz0 + fz1) // 2, wie="hier staat de speler bij de waaierdans (op het gouden blok)")
    b.plek("krijgster_leider", (fx0 + fx1) // 2, G + 1, z0 + 3, yaw=0.0, wie="de leidster van de Kyoshi-krijgsterguhs, voor de waaierwand",
           kind="KYOSHI_KRIJGSTERGUH")
    for i, z in enumerate((fz0 + 1, (fz0 + fz1) // 2, fz1 - 1), 1):
        b.plek(f"krijgster_{i}", fx1 + 4, G + 1, z, yaw=90.0, wie="een Kyoshi-krijgsterguh, langs de oostkant van de dansvloer", kind="KYOSHI_KRIJGSTERGUH")
    # the doors (west, two wide) and the sign
    for z, scharnier in ((69, "left"), (70, "right")):
        g.deur(b, x0, G + 1, z, "west", scharnier=scharnier)
        b.set(x0, G + 3, z, "dark_oak_planks")
        for y in (G + 4, G + 5):
            b.set(x0, y, z, WAND)
    b.sign(x0 - 1, G + 2, 68, "west", ["DOJO", "Schoenen uit.", "Waaier mee.", "Njeg giechelen"])
    for z in (67, 72):
        b.set(x0 - 1, G + 1, z, "spruce_fence")
        b.set(x0 - 1, G + 2, z, "guhs:lampion_roze", gb.lamp(False))
    # the fan wall behind the leader: a golden fan on green
    waaier = ["ggggYggggYgggg",
              "gYgggYgYgggYgg",
              "ggYggYYYggYggg",
              "gggYYYRYYYgggg",
              "ggggggRggggggg"]
    for j, rij in enumerate(waaier):
        for i, ch in enumerate(rij):
            b.set(x0 + 6 + i, G + 5 - j, z0, {"g": "green_terracotta", "Y": "gold_block", "R": "red_concrete"}[ch])
    # along the north wall: cushions to watch from; in the north-west corner two practice dummies
    for x in range(x0 + 14, x0 + 22, 2):
        b.set(x + 1, G + 1, z0 + 1, "guhs:green_kussen", {"facing": "south"})
    for x in (x0 + 2, x0 + 4):
        b.set(x, G + 1, z0 + 2, "spruce_fence")
        b.set(x, G + 2, z0 + 2, "hay_block", {"axis": "y"})
        b.set(x, G + 3, z0 + 2, "carved_pumpkin", {"facing": "south"})
    b.sign(x0 + 3, G + 2, z0 + 1, "south", ["Oefenpop", "Mikt terug.", "(dat is", "een grapje)"])
    # the schminktafel in the south-west corner: a dressing table under a mirror, pots of white and red
    for x in (x0 + 2, x0 + 3, x0 + 4):
        g.plaat(b, x, G + 1, z1 - 1, "spruce_slab", "top")
        for y in (G + 2, G + 3):
            b.set(x, y, z1, "light_blue_stained_glass")
    b.set(x0 + 2, G + 2, z1 - 1, "potted_white_tulip")
    b.set(x0 + 4, G + 2, z1 - 1, "potted_poppy")
    b.set(x0 + 3, G + 1, z1 - 2, "guhs:guh_stoel", {"facing": "south"})
    b.prop("schminktafel", x0 + 3, G + 1, z1 - 1, wie="de schminktafel onder de spiegel (hier krijg je de Kyoshi-schmink)")
    b.sign(x0 + 1, G + 2, z1 - 2, "east", ["Schminktafel", "Eerst wit,", "dan rood.", "Njeg opeten."])
    # beams with lanterns, and fan banners along the south wall
    for x in (x0 + 5, x0 + 13, x0 + 21):
        for z in range(z0 + 1, z1):
            b.set(x, G + 6, z, "stripped_dark_oak_log", {"axis": "z"})
        for z in (z0 + 4, z0 + 10, z0 + 16):
            b.set(x, G + 5, z, "lantern", gb.lamp(True))       # (right under the beam: four blocks free above the dance floor)
    for x in range(x0 + 9, x1 - 2, 4):
        g.vlag(b, x, G + 3, z1 - 1, "green" if (x // 4) % 2 else "yellow", facing="north")


def fanclub(b):
    """Just inside the gate, on the west side of the street: the stand of the fan who foams at the mouth."""
    for x in (58, 59, 60):
        b.set(x, G + 1, 99, "spruce_planks" if x != 59 else "barrel", None if x != 59 else {"facing": "up", "open": "false"})
        b.set(x, G + 4, 99, "green_wool" if x % 2 else "yellow_wool")
        b.set(x, G + 4, 100, "green_wool" if x % 2 == 0 else "yellow_wool")
    for x, z in ((58, 100), (60, 100)):
        for y in range(G + 1, G + 4):
            b.set(x, y, z, "spruce_fence")
    for x in (58, 60):
        b.set(x, G + 2, 99, "spruce_fence")
        b.set(x, G + 3, 99, "spruce_fence")
    b.set(59, G + 2, 99, "cake", {"bites": "3"})
    b.sign(59, G + 1, 100, "south", ["KYOSHI", "FANCLUB", "Lid worden?", "Eerst gillen."])
    b.sign(60, G + 1, 98, "east", ["Schuim op de", "bek = normaal", "bij echte fans", ""], wall=False)
    b.plek("fan_guh", 61, G + 1, 100, yaw=-90.0, wie="de schuimbekkende fan-guh, naast zijn fanclubkraam bij de poort", kind="FANGUH")


def vijver(b, grond):
    """The pond in the south-west: a closed basin two deep, a jetty, a rowing boat, and Unaguh."""
    cx, cz, rx, rz = VIJVER
    nat = set()
    for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
        for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
            d = math.hypot((x - cx) / rx, (z - cz) / rz)
            if d <= 1.0 and (x, z) in grond:
                nat.add((x, z))
    for (x, z) in nat:
        diep = 2 if all((x + dx, z + dz) in nat for dx, dz in gb.DIRS.values()) else 1
        for k in range(diep):
            b.set(x, G - k, z, "water", {"level": "0"})
        b.set(x, G - diep, z, "guhs:kaaskorst" if (x + z) % 3 else KAAS)
        b.set(x, G - diep - 1, z, KAAS)
    for (x, z) in nat:                                       # (a closed basin: rock around every water block)
        for dx, dz in gb.DIRS.values():
            n = (x + dx, z + dz)
            if n not in nat:
                for y in (G - 1, G):
                    if b.get(n[0], y, n[1]) is None:
                        b.set(n[0], y, n[1], KAAS)
    # the jetty from the east shore, a lampion at its end, and the rowing boat beside it
    for x in range(int(cx) + 2, int(cx + rx) + 2):
        b.set(x, G + 1, int(cz), "spruce_slab", gb.slab())
    g.lantaarnpaal(b, int(cx + rx) + 2, G + 1, int(cz) + 1, hoogte=3)
    for y in (G - 1, G, G + 1, G + 2):                      # a post in the water at the jetty's end, a lampion on it
        b.set(int(cx) + 1, y, int(cz), "spruce_fence")
    b.set(int(cx) + 1, G + 3, int(cz), "guhs:lampion_geel", gb.lamp(False))
    # Unaguh: three dark humps and a head with two pale eyes, half under water
    for hx, hz, r in ((cx - 4, cz - 2, 1.6), (cx - 1, cz - 3, 1.9), (cx + 2, cz - 3.5, 1.4)):
        for x in range(int(hx - r) - 1, int(hx + r) + 2):
            for dy in range(0, 3):
                if math.hypot(x - hx, dy) <= r + 0.2 and (x, int(hz)) in nat:
                    b.set(x, G + dy, int(hz), "dark_prismarine")
    kx, kz = int(cx - 6), int(cz + 1)
    for dx in (0, 1):
        for dy in (0, 1):
            b.set(kx + dx, G + dy, kz, "dark_prismarine")
    b.set(kx, G + 1, kz + 1, "sea_lantern")
    b.set(kx + 1, G + 1, kz + 1, "dark_prismarine")
    b.set(kx, G, kz + 1, "dark_prismarine")
    b.set(kx + 1, G, kz + 1, "dark_prismarine")
    b.sign(int(cx + rx) + 2, G + 1, int(cz) - 2, "east", ["Unaguh", "niet voeren.", "Hij voert", "zichzelf."], wall=False)
    b.wezen("otter_1", int(cx) + 4, G + 1, int(cz - rz) - 3, "guhs:otter_pinguinguh", yaw=180.0, honk=8)
    b.wezen("otter_2", int(cx) - 3, G + 1, int(cz + rz) + 3, "guhs:otter_pinguinguh", yaw=0.0, honk=8)
    return nat


def paddenstoel(b, x, z, hoog, r):
    """A giant blue-cheese mushroom: a stem and a domed cap whose rim droops."""
    for y in range(G + 1, G + 1 + hoog):
        b.set(x, y, z, STEEL, {"axis": "y"})
    top = G + hoog
    hoogte = {}
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            d = math.hypot(dx, dz)
            if d <= r + 0.3:
                hoogte[(dx, dz)] = top + (1 if d < r - 1.2 else 0) - (1 if d > r - 0.5 else 0)
    for (dx, dz), y in hoogte.items():
        # (down to its lowest neighbour: the cap is one piece, no rim that hangs loose by a corner)
        laag = min([y] + [hoogte[n] for n in ((dx + 1, dz), (dx - 1, dz), (dx, dz + 1), (dx, dz - 1)) if n in hoogte])
        for yy in range(laag, y + 1):
            b.set(x + dx, yy, z + dz, HOED)


def natuur(b, grond, nat):
    """Giant mushrooms around the village, small ones in the grass, a few boulders."""
    rng = random.Random(kit.seed(NAAM + "/natuur"))
    plekken = [(22, 50, 7, 4), (30, 34, 6, 3), (100, 30, 8, 4), (112, 52, 6, 3), (110, 92, 7, 4), (92, 108, 5, 3), (40, 110, 6, 3),
               (16, 72, 5, 3), (90, 88, 5, 3), (38, 50, 4, 2), (96, 46, 5, 3), (26, 104, 4, 2)]
    for x, z, hoog, r in plekken:
        if (x, z) in grond and b.get(x, G + 1, z) is None and b.get(x, G, z) == KAAS:
            paddenstoel(b, x, z, hoog, r)
    for (x, z), t in sorted(grond.items()):
        if b.get(x, t, z) == KAAS and b.get(x, t + 1, z) is None and (x, z) not in nat:
            kans = rng.random()
            if kans < 0.035:
                b.set(x, t + 1, z, PADDO)
            elif kans < 0.04:
                b.set(x, t + 1, z, STENEN)
            elif kans < 0.043:
                b.set(x, t + 1, z, "guhs:kaaskorst")


def straatmeubilair(b):
    px, pz, r = PLEIN
    for hoek in range(4):
        a = math.radians(45 + hoek * 90)
        g.lantaarnpaal(b, int(round(px + math.cos(a) * (r - 1))), G + 1, int(round(pz + math.sin(a) * (r - 1))), hoogte=3, lamp="guhs:lampion_roze")
    for z in (80, 89, 97):
        for x in (STRAAT[0] - 1, STRAAT[1] + 1):
            if b.get(x, G + 1, z) is None and b.get(x, G, z) in (KAAS, None):
                g.lantaarnpaal(b, x, G + 1, z, hoogte=3)
    # the well in the middle of the square? No: a bench ring around the square's centre stone, to look at the statue
    for dx, f in ((-3, "east"), (3, "west")):
        b.set(px + dx, G + 1, pz + 3, "guhs:guh_bank", {"facing": f})
    b.sign(px, G + 1, pz + 5, "south", ["Dorpsplein", "Vandaag:", "waaierdans", "(als altijd)"], wall=False)
    # fish racks by the pond path
    for x in (46, 48, 50):
        b.set(x, G + 1, 76, "spruce_fence")
        b.set(x, G + 2, 76, "spruce_fence")
    for x in (47, 49):
        b.set(x, G + 2, 76, "spruce_fence")


def _bouw(h):
    b = kit.Bouw(h, NAAM, (W, H, D), kit.STRUCTUREN[NAAM]["salt"], prefix=g.PREFIX)
    grond = eiland(b)
    bestrating(b, grond)
    poort(b)
    beeld(b)
    huizen(b)
    dojo(b)
    fanclub(b)
    nat = vijver(b, grond)
    straatmeubilair(b)
    natuur(b, grond, nat)
    b.connect()
    start = (64, G + 1, 110)
    n, over = g.verlicht(b, start, [g.op_grond(PADDO, (KAAS,)), g.in_vloer("guhs:guh_kristal_lamp"), g.op_vloer("lantern", gb.lamp(False)),
                                    g.in_wand("guhs:guh_kristal_lamp")])
    b.laatste_licht = n
    return b, start, over


VERWACHT = ["kyoshi_beeld", "dojo", "dansvloer", "dans_speler", "krijgster_leider", ("krijgster", 3), "schminktafel", "fan_guh", "dorpsoudste",
            ("bewoner", 4), ("otter", 2)]


def eigen_check(b):
    """The dance floor: at least 9 x 9, one level, nothing standing on it."""
    p = []
    x0, z0, x1, z1 = DANSVLOER
    if x1 - x0 + 1 < 9 or z1 - z0 + 1 < 9:
        p.append("de dansvloer is kleiner dan 9 x 9")
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if b.get(x, G, z) is None or any(b.get(x, G + k, z) is not None for k in (1, 2, 3, 4)):
                p.append(f"dansvloer {(x, z)}: niet vlak of niet vrij")
    return p[:6]


def maak(h):
    b, start, over = _bouw(h)
    problemen = b.check(start, VERWACHT)
    problemen += [f"{len(over)} donkere plekken kregen geen licht, bv. {over[:4]}"] if over else []
    problemen += eigen_check(b)
    return b, problemen


def bouw(h):
    b, problemen = maak(h)
    if problemen:
        raise SystemExit(f"{NAAM}: self-check found {len(problemen)} problem(s):\n  " + "\n  ".join(problemen[:40]))
    print(f"{NAAM}: self-check ok {b.stats}, {b.laatste_licht} lights added")
    return b
