"""
einde2, slice bouw_schelpenstad - Schelpenstad: a little seaside town of shell stone and mother-of-pearl on the rim of a beach
of kruimelzand, where the void is the sea.

    the gate and the street        the road comes down the hill and ends on the square (the aankomst)
    the souvenir shop              "Schelpenstad Souvenirs & Zo" of the Cycloop: shelves of dried souvenirs, the crown under
                                   glass lamps, and in the back the drying table under the drying lamp
    the kiosk                      Kaartjes & Kruimels (postcards)
    the promenade and the beach    changing huts, parasols, a sand castle, beach bar De Zoute Snoet
    the shore                      the lighthouse De Lange Njeg, the Guhwatch tower, the pier where David Hasselguh waits
"""
import math

from features import guheinde_bouw as gb
from features.einde2_bouw_schelpenstad_basis import *  # noqa: F401,F403

G = STAD_Y                 # 13: the ground block of town (you walk at 14)
V = G + 1
ROZE, TURK, WITB = "minecraft:pink_terracotta", "minecraft:cyan_terracotta", "minecraft:white_concrete"
BERK, BERKPLAAT, HEK = "minecraft:birch_planks", "minecraft:birch_slab", "minecraft:birch_fence"
ROZEDAK = "guhs:pluisdak"
WX0, WX1, WZ0, WZ1 = 171, 197, 13, 31          # the souvenir shop (outer walls); its front is the south wall
WH = 5                                         # the shop is five high inside
VT = (217, 23)                                 # the lighthouse
PIER_Z = 50


def bestrating(b):
    """The square (a shell in its paving), the street in from the road, the yard of the shop, the promenade."""
    px, pz = PLEIN
    for x in range(STAD_X + 1, 212):
        for z in range(33, 57):
            straat = 38 <= z <= 46 and x <= px - 6
            plein = math.hypot(x - px, z - pz) <= 8.6
            erf = WX0 - 1 <= x <= WX1 + 1 and 32 <= z <= 36
            laan = px - 3 <= x <= px + 3 and 44 <= z <= 52
            boulevard = 52 <= z <= 55 and x >= 166
            if not (straat or plein or erf or laan or boulevard) or b.naam(x, G, z) is None:
                continue
            if plein:
                d, hoek = math.hypot(x - px, z - pz), math.atan2(z - pz - 0.01, x - px)
                rib = int((hoek + math.pi) / (2 * math.pi) * 14) % 2
                blok = KLINKER if (d > 7.4 or (rib and 2.2 < d <= 7.4)) else PARELTEGEL
                if d <= 1.2:
                    blok = LAMP
            elif boulevard:
                blok = KLINKER if z in (52, 55) else PARELTEGEL
            elif straat and x <= X_OOST_EIND and abs(z - wegz(x)) <= 3:
                continue                                          # (the road itself lies there)
            else:
                blok = PARELTEGEL if (x + z) % 2 else KLINKER
            b.set(x, G, z, blok)
            b.wis(x, V, z, x, V + 7, z)
            b.vast.add((x, V, z))


def poort(b):
    """The gate over the road where town begins."""
    x = STAD_X + 4
    c = wegz(x)
    for z in (c - 5, c + 5):
        for y in range(V, V + 7):
            b.set(x, y, z, PAREL)
        b.set(x, V + 7, z, ROZEDAK)
        b.set(x, G, z, PAREL)
    for z in range(c - 5, c + 6):
        b.set(x, V + 6, z, SCHELPSTEEN if z % 2 else ROZE)
        if abs(z - c) <= 3:
            b.set(x, V + 7, z, PAREL if abs(z - c) <= 2 else ROZEDAK)
    b.set(x, V + 8, c, ROZEDAK)
    b.bord(x - 1, V + 1, c - 5, "west", ["Welkom in", "Schelpenstad", "aan de Leegtezee"])
    b.bord(x - 1, V + 1, c + 5, "west", ["Parels poetsen", "Knabbels kauwen", "Zand in je oren"])
    b.set(x, V + 5, c - 4, "guhs:lampion_roze", gb.lamp(True))
    b.set(x, V + 5, c + 4, "guhs:lampion_mint", gb.lamp(True))


def schelp_beeld(b, cx, y, z, r=6):
    """A giant scallop standing upright (in the x-y plane at z and z + 1): the sign of the shop, seen from the whole hill."""
    for dz in (0, 1):
        for dx in range(-r, r + 1):
            for dy in range(0, r + 1):
                d = math.hypot(dx, dy)
                if d > r + 0.3:
                    continue
                hoek = math.atan2(dy, dx)
                rib = int(hoek / math.pi * 9)
                rand = d > r - 0.7
                b.set(cx + dx, y + dy, z + dz, "minecraft:pink_concrete" if (rib % 2 and d > 1.6) else PAREL if not rand else "minecraft:pink_concrete" if rib % 2 else WITB)
    for dx in (-2, -1, 0, 1, 2):
        b.set(cx + dx, y - 1, z, PAREL)
        b.set(cx + dx, y - 1, z + 1, PAREL)


def plank(b, cellen, kijkt, hoog=True, start=0):
    """A shelf of dried souvenirs: a cabinet with one on it, and (hoog) a board above with another."""
    for i, (x, z) in enumerate(cellen):
        b.set(x, V, z, BERK)
        b.set(x, V + 1, z, SOUVENIR, {"facing": kijkt, "soort": str((start + i) % 4)})
        if hoog:
            b.set(x, V + 2, z, BERKPLAAT, gb.slab("top"))
            b.set(x, V + 3, z, SOUVENIR, {"facing": kijkt, "soort": str((start + i + 2) % 4)})


def winkel(b):
    """The souvenir shop of the Cycloop."""
    x0, x1, z0, z1 = WX0, WX1, WZ0, WZ1
    top = V + WH                                               # the ceiling
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.wis(x, V, z, x, top + 12, z)
            b.set(x, G, z, PARELTEGEL if (x // 2 + z // 2) % 2 else ROZE)
            b.set(x, top, z, SCHELPSTEEN)
            b.vast.add((x, V, z))
            for y in range(G - 3, G):                          # (the hill may have left a hollow under the floor)
                if b.naam(x, y, z) is None:
                    b.set(x, y, z, KORST)
    b.kader(x0, V, z0, x1, top - 1, z1, SCHELPSTEEN)
    b.kader(x0, V, z0, x1, V, z1, ROZE)                         # a pink plinth
    b.kader(x0, top + 1, z0, x1, top + 1, z1, PARELTEGEL)       # the parapet
    for x in (x0, x1):
        for z in (z0, z1):
            for y in range(V, top + 2):
                b.set(x, y, z, PAREL)
    # the front: the double door, two shop windows, a striped awning
    mx = (x0 + x1) // 2                                         # 184: the door is mx and mx + 1
    for x in list(range(x0 + 3, mx - 2)) + list(range(mx + 4, x1 - 2)):
        for y in range(V + 1, V + 4):
            b.set(x, y, z1, "minecraft:glass")
    for x in (mx, mx + 1):
        b.wis(x, V, z1, x, V + 2, z1)
        b.set(x, V + 2, z1, TURK)
    deur(b, mx, V, z1, "north", "birch", "right")
    deur(b, mx + 1, V, z1, "north", "birch", "left")
    for x in range(x0, x1 + 1):
        b.set(x, top - 1, z1 + 1, "guhs:pluisdak_plaat" if (x // 2) % 2 else "minecraft:smooth_quartz_slab", gb.slab("top"))
    b.bord(mx - 1, V + 1, z1 + 1, "south", ["Schelpenstad", "Souvenirs & Zo", "van de Cycloop"])
    b.bord(mx + 2, V + 1, z1 + 1, "south", ["Alles gedroogd", "Alles te koop", "Niets te eten"])
    for x in (x0 + 4, x0 + 7, x1 - 7, x1 - 4):                  # the window displays
        b.set(x, V, z1 - 1, BERK)
        b.set(x, V + 1, z1 - 1, SOUVENIR, {"facing": "south", "soort": str(x % 4)})
    # side windows
    for z in range(z0 + 9, z1 - 2, 3):
        for x in (x0, x1):
            b.set(x, V + 2, z, "minecraft:glass")
    # the roof: the giant shell
    schelp_beeld(b, mx, top + 2, (z0 + z1) // 2)
    for x in (x0 + 2, x1 - 2):
        b.set(x, top + 1, z1 - 1, "guhs:lampion_roze", gb.lamp(False))
    # --- inside: the partition between the shop and the drying room ---------------------------------------------------------
    zp = z0 + 7                                                 # 20
    for x in range(x0 + 1, x1):
        if not (mx - 4 <= x <= mx + 5):
            for y in range(V, top):
                b.set(x, y, zp, SCHELPSTEEN)
        else:
            b.set(x, top - 1, zp, TURK)
    for x in (mx - 5, mx + 6):
        for y in range(V, top):
            b.set(x, y, zp, PAREL)
    # --- the shop: six shelves, the postcard racks, the till, the crown ----------------------------------------------------
    plank(b, [(x0 + 1, z) for z in range(22, 27)], "east", start=0)
    plank(b, [(x1 - 1, z) for z in range(22, 27)], "west", start=1)
    plank(b, [(x, 25) for x in range(x0 + 5, x0 + 9)], "south", hoog=False, start=2)
    plank(b, [(x, 25) for x in range(x1 - 8, x1 - 4)], "south", hoog=False, start=3)
    plank(b, [(x, zp + 1) for x in range(x0 + 2, x0 + 8)], "south", start=1)
    plank(b, [(x, zp + 1) for x in range(x1 - 7, x1 - 1)], "south", start=2)
    b.set(mx - 3, V, z1 - 2, KAARTENREK)
    b.set(mx + 4, V, z1 - 2, KAARTENREK)
    for x in range(x1 - 6, x1 - 2):                             # the till
        b.set(x, V, z1 - 3, BERK)
    b.set(x1 - 3, V + 1, z1 - 3, "guhs:kaaskluis", {"facing": "south"})
    b.set(x1 - 5, V + 1, z1 - 3, "minecraft:flower_pot")
    b.bord(x1 - 4, V, z1 - 2, "south", ["Kassa", "Ruilen mag niet", "Huilen wel"])
    b.set(mx, V, 25, KUSSEN)                                    # the crown display
    for x in (mx - 1, mx + 1):
        b.set(x, V, 25, STAAFJE, {"facing": "up"})
    b.bord(mx, V, 24, "north", ["Koningskroon", "NIET te koop", "(Plankmika", "betaalde al)"], wall=False)
    for (x, z) in ((x0 + 5, 28), (x1 - 5, 23), (mx, 28), (x0 + 5, 23)):
        b.set(x, top - 1, z, "guhs:lampion_roze" if (x + z) % 2 else "guhs:lampion_mint", gb.lamp(True))
    b.bord(x0 + 1, V + 1, 28, "east", ["Gedroogd is", "voor altijd", "leuk, njeg!"])
    b.bord(x1 - 1, V + 1, 28, "west", ["Niet aaien", "Niet voeren", "Niet natmaken!"])
    # --- the drying room ---------------------------------------------------------------------------------------------------
    zt = z0 + 3                                                 # 16: the row of the table
    for x in (mx - 1, mx, mx + 1):
        b.set(x, V, zt, "minecraft:smooth_stone_slab", gb.slab("top"))
    b.set(mx, top - 1, zt, DROOGLAMP)
    for (x, z, as_) in ((mx + 1, zt, "x"), (mx + 2, zt, "x"), (mx + 3, zt, "x"), (mx + 3, zt - 1, "z")):   # the cord, along the ceiling
        b.set(x, top - 1, z, "minecraft:chain", {"axis": as_, "waterlogged": "false"})
    for y in range(V + 2, top):
        b.set(mx + 3, y, z0 + 1, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    b.set(mx + 3, V + 1, z0 + 1, STOPCONTACT, {"facing": "south"})
    for (x, z) in ((mx, zt + 2), (x0 + 5, zt), (x1 - 5, zt), (x0 + 5, 26), (x1 - 5, 26), (mx + 1, 23)):
        b.set(x, top - 1, z, SPROEIER)
    b.bord(mx - 2, V + 1, z0 + 1, "south", ["Droogtafel", "Niet aankomen", "Is heet, njeg"])
    for x in range(x0 + 2, x0 + 6):                             # the work bench: glue and googly eyes
        b.set(x, V, z0 + 1, BERK)
    b.set(x0 + 2, V + 1, z0 + 1, "minecraft:flower_pot")
    b.set(x0 + 3, V + 1, z0 + 1, SOUVENIR, {"facing": "south", "soort": "1"})
    b.set(x0 + 5, V + 1, z0 + 1, "minecraft:candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
    b.bord(x0 + 4, V + 2, z0 + 1, "south", ["Wiebeloogjes", "3 knabbels", "per paar"])
    b.chest(x1 - 1, V, z0 + 1, "west", "guhs:chests/einde2_bouw_schelpenstad_winkel")
    for z in range(z0 + 3, z0 + 6):                             # fresh stock, waiting for its turn
        b.set(x1 - 1, V, z, BERK)
        b.set(x1 - 1, V + 1, z, SOUVENIR, {"facing": "west", "soort": str(z % 4)})
    b.set(x0 + 3, top - 1, zt + 1, "guhs:lampion_mint", gb.lamp(True))
    b.set(x1 - 3, top - 1, zt + 1, "guhs:lampion_roze", gb.lamp(True))
    # in front of the shop: two racks and a flower box
    b.set(mx - 4, V, z1 + 2, KAARTENREK)
    b.set(mx + 5, V, z1 + 2, KAARTENREK)
    b.paal(x0 + 1, V, z1 + 3, 2, HEK)
    b.paal(x1 - 1, V, z1 + 3, 2, HEK)

    mid = (x0 + x1) // 2
    b.zone("souvenirwinkel", x0, G, z0, x1, top + 1, z1, wie="Schelpenstad Souvenirs & Zo, de winkel van de Cycloop")
    b.plek("cycloop", mid - 3, V, zt + 1, yaw=-90.0, wie="de Cycloop, bij zijn droogtafel", wezen="guhs:cycloop_mika")
    b.prop("droogtafel", mid, V, zt, wie="de tafel onder de droogtelamp")
    b.prop("droogtafel_sponsguh", mid - 1, V + 1, zt, wie="hier ligt Sponsguh te drogen", los=True)
    b.prop("droogtafel_patguh", mid + 1, V + 1, zt, wie="hier ligt Patguh te drogen", los=True)
    b.prop("droogtafel_speler", mid, V, zt + 2, wie="hier sta jij (vastgehouden)")
    b.prop("drooglamp", mid, top - 1, zt, wie="de droogtelamp boven de tafel", los=True)
    b.prop("stopcontact", mid + 3, V + 1, z0 + 1, wie="het stopcontact waar de traan in loopt")
    b.prop("sproeier", mid, top - 1, zt + 2, wie="de sproeier aan het plafond", los=True)
    for i, (x, z) in enumerate(((x0 + 1, 24), (x1 - 1, 24), (x0 + 6, 25), (x1 - 6, 25), (x0 + 4, zp + 1), (x1 - 4, zp + 1)), 1):
        b.prop(f"souvenirplank_{i}", x, V, z, wie="een plank vol gedroogde souvenirs")
    b.prop("kroon", mid, V + 1, 25, wie="op het kussen: de kroon van koning Neptuguh (1 x 2 x 1 vrij)")
    b.prop("rustpunt_3", mid, V, z1 + 3, wie="het rustpunt voor de souvenirwinkel")
    for id in ("cycloop", "droogtafel_speler", "rustpunt_3"):
        b.vast.add(tuple(b.plekken[id]["pos"]))


def kiosk(b, x0, z0):
    """Kaartjes & Kruimels: a round little kiosk under a dome of mother-of-pearl."""
    cx, cz = x0 + 2, z0 + 2
    for x in range(x0, x0 + 5):
        for z in range(z0, z0 + 5):
            hoek = (x in (x0, x0 + 4)) and (z in (z0, z0 + 4))
            b.wis(x, V, z, x, V + 8, z)
            if hoek:
                continue
            b.set(x, G, z, ROZE)
            b.vast.add((x, V, z))
            rand = x in (x0, x0 + 4) or z in (z0, z0 + 4)
            if rand:
                b.set(x, V, z, SCHELPSTEEN)
                b.set(x, V + 2, z, TURK)
                b.set(x, V + 3, z, PAREL)
            else:
                b.set(x, V + 3, z, PAREL)
                b.set(x, V + 4, z, PAREL)
    b.set(cx, V + 5, cz, ROZEDAK)
    b.set(cx, V + 3, cz, "guhs:lampion_roze", gb.lamp(True))
    b.wis(cx, V, z0, cx, V + 1, z0)                              # the door, at the back
    deur(b, cx, V, z0, "south", "birch")
    for (x, z) in ((x0 + 1, z0), (x0 + 3, z0), (x0, z0 + 1), (x0, z0 + 3), (x0 + 4, z0 + 1), (x0 + 4, z0 + 3)):
        b.set(x, V + 1, z, SCHELPSTEEN)                          # (closed where there is no counter)
    b.set(cx, V + 1, z0 + 4, KAARTENREK)                          # on the counter
    b.bord(x0 + 1, V, z0 + 5, "south", ["Kaartjes &", "Kruimels", "Groetjes uit", "Schelpenstad"])
    b.bord(x0 + 3, V, z0 + 5, "south", ["Postzegel: 1", "knabbel. Likken", "op eigen risico"])
    b.set(x0 - 1, V, z0 + 3, KAARTENREK)


def schelphuisje(b, x0, z0, kleur, deur_x=2):
    """A little house of shell stone under a dome of mother-of-pearl: 5 x 5, the door in its south wall."""
    for x in range(x0, x0 + 5):
        for z in range(z0, z0 + 5):
            b.wis(x, V, z, x, V + 8, z)
            b.set(x, G, z, BERK)
            b.vast.add((x, V, z))
            hoek = x in (x0, x0 + 4) and z in (z0, z0 + 4)
            if x in (x0, x0 + 4) or z in (z0, z0 + 4):
                b.set(x, V, z, kleur)
                b.set(x, V + 1, z, PAREL if hoek else SCHELPSTEEN)
                b.set(x, V + 2, z, PAREL if hoek else SCHELPSTEEN)
            if not hoek:
                b.set(x, V + 3, z, PAREL)
            if x0 < x < x0 + 4 and z0 < z < z0 + 4:
                b.set(x, V + 4, z, PAREL)
    b.set(x0 + 2, V + 5, z0 + 2, ROZEDAK)
    b.wis(x0 + 2, V + 3, z0 + 2)
    b.set(x0 + 2, V + 3, z0 + 2, "guhs:lampion_geel", gb.lamp(True))
    b.wis(x0 + deur_x, V, z0 + 4, x0 + deur_x, V + 1, z0 + 4)
    deur(b, x0 + deur_x, V, z0 + 4, "north", "birch")
    b.set(x0, V + 1, z0 + 2, "minecraft:glass")
    b.set(x0 + 4, V + 1, z0 + 2, "minecraft:glass")
    b.set(x0 + 1, V, z0 + 1, "guhs:pink_zitzak", {"facing": "south"})
    tafel(b, x0 + 3, V, z0 + 1)
    b.set(x0 + 3, V + 1, z0 + 1, SOUVENIR, {"facing": "south", "soort": str((x0 + z0) % 4)})
    for z in range(z0 + 5, 38):                                  # a path of clinkers to the street
        if b.naam(x0 + deur_x, G, z) in (KORST, ROTS, BARST):
            b.set(x0 + deur_x, G, z, KLINKER)
            b.vast.add((x0 + deur_x, V, z))


def boulevard(b):
    """Lanterns, benches and a rail along the promenade."""
    for x in range(168, 211, 6):
        b.paal(x, V, 56, 3, HEK)
        b.set(x, G, 56, PAREL)
    for x in range(166, 211):
        if b.naam(x, V, 56) is None and x % 12 not in (4, 5, 6) and b.naam(x, G, 56) is not None:
            b.set(x, V, 56, HEK)
            b.set(x, G, 56, PAREL)
    for x in (171, 177, 195, 201, 207):
        b.set(x, V, 52, "guhs:guh_bank", {"facing": "south"})
    px, pz = PLEIN
    for (x, z) in ((px - 7, pz - 6), (px + 7, pz - 6), (px - 7, pz + 6), (px + 7, pz + 6)):
        b.paal(x, V, z, 3, HEK)
    b.set(px - 6, V, pz + 7, "guhs:guh_bank", {"facing": "north"})
    b.set(px + 6, V, pz + 7, "guhs:guh_bank", {"facing": "north"})


def hokje(b, x0, z0, kleur):
    """A changing hut on the beach: 3 x 3, striped."""
    for x in range(x0, x0 + 3):
        for z in range(z0, z0 + 3):
            g = b.grond(x, z, van=V)
            for y in range((g or G) + 1, V):
                b.set(x, y, z, BERK)
            b.wis(x, V, z, x, V + 6, z)
            b.set(x, V - 1, z, BERK)
            b.vast.add((x, V, z))
            if (x, z) != (x0 + 1, z0 + 1):
                for y in range(V, V + 3):
                    b.set(x, y, z, f"minecraft:{kleur}_wool" if (x + z) % 2 else "minecraft:white_wool")
            b.set(x, V + 3, z, f"minecraft:{kleur}_wool" if x == x0 + 1 else "minecraft:white_wool")
    b.set(x0 + 1, V + 4, z0 + 1, f"minecraft:{kleur}_wool")
    b.set(x0 + 1, V + 2, z0 + 1, "minecraft:lantern", gb.lamp(True))
    b.wis(x0 + 1, V, z0 + 2, x0 + 1, V + 1, z0 + 2)
    deur(b, x0 + 1, V, z0 + 2, "north", "birch")


def parasol(b, x, z, kleur):
    g = b.grond(x, z)
    if g is None:
        return
    for y in range(g + 1, g + 4):
        b.set(x, y, z, HEK)
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            b.set(x + dx, g + 4, z + dz, f"minecraft:{kleur}_wool" if (dx + dz) % 2 == 0 else "minecraft:white_wool")
    b.set(x, g + 5, z, f"minecraft:{kleur}_wool")
    for (dx, dz, c) in ((1, 1, kleur), (-1, 2, "white")):
        gg = b.grond(x + dx, z + dz, van=g + 3)             # (the sand under the parasol, not its cloth)
        if gg is not None and b.naam(x + dx, gg, z + dz) == ZAND and b.naam(x + dx, gg + 1, z + dz) is None:
            b.set(x + dx, gg + 1, z + dz, f"minecraft:{c}_carpet")       # a towel


def strandtent(b, x0, z0):
    """De Zoute Snoet: a deck on posts over the sand, a reed roof, a bar."""
    x1, z1 = x0 + 9, z0 + 6
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            g = b.grond(x, z, van=V)
            b.wis(x, V, z, x, V + 7, z)
            b.set(x, G, z, PALM)
            b.vast.add((x, V, z))
            hoek = x in (x0, x1) and z in (z0, z1)
            for y in range((g if g is not None else G - 3) + 1, G):
                if hoek or b.naam(x, y, z) is None and (x in (x0, x1) or z in (z0, z1)):
                    b.set(x, y, z, PALMSTAM if hoek else PALM, {"axis": "y"} if hoek else None)
            if hoek:
                for y in range(V, V + 3):
                    b.set(x, y, z, PALMSTAM, {"axis": "y"})
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            buiten = x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1)
            if buiten:
                b.set(x, V + 3, z, "guhs:tiki_rietdak_plaat", gb.slab("bottom"))
            else:
                b.set(x, V + 3, z, "guhs:tiki_rietdak")
                if x0 + 2 <= x <= x1 - 2 and z0 + 2 <= z <= z1 - 2:
                    b.set(x, V + 4, z, "guhs:tiki_rietdak_plaat", gb.slab("bottom"))
    for x in range(x0 + 3, x0 + 7):                              # the bar
        b.set(x, V, z0 + 2, PALM)
    b.set(x0 + 3, V + 1, z0 + 2, "guhs:tiki_radiootje", {"facing": "south"})
    b.set(x0 + 6, V + 1, z0 + 2, "guhs:guh_taart", {"bites": "1"})
    for x in (x0 + 3, x0 + 5, x0 + 6):
        b.set(x, V, z0 + 3, "guhs:tiki_kruk", {"facing": "north"})
    b.set(x0 + 1, V, z0 + 1, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.set(x0 + 2, V + 2, z0 + 3, "guhs:lampion_geel", gb.lamp(True))
    b.set(x0 + 7, V + 2, z0 + 3, "guhs:lampion_geel", gb.lamp(True))
    b.set(x0 + 8, V, z0 + 5, "guhs:tiki_fakkel")
    b.set(x0 + 1, V, z0 + 5, "guhs:tiki_fakkel")
    b.bord(x0 + 4, V + 2, z0 - 1, "north", ["De Zoute Snoet", "Koud drinken", "Warm zand", "Njeg erbij"])
    b.set(x0 + 4, V + 2, z0, PALM)
    tafel(b, x0 + 7, V, z0 + 5)
    stoel(b, x0 + 6, V, z0 + 5, "east")


def zandkasteel(b, x, z):
    g = b.grond(x, z)
    if g is None:
        return
    for dx in range(0, 4):
        for dz in range(0, 4):
            gg = b.grond(x + dx, z + dz)
            if gg is None:
                return
    for dx in range(0, 4):
        for dz in range(0, 4):
            gg = b.grond(x + dx, z + dz, van=g + 1)
            hoek = dx in (0, 3) and dz in (0, 3)
            rand = dx in (0, 3) or dz in (0, 3)
            for y in range(gg + 1, g + 2):
                b.set(x + dx, y, z + dz, "minecraft:smooth_sandstone")
            if hoek:
                b.set(x + dx, g + 2, z + dz, "minecraft:smooth_sandstone")
                b.set(x + dx, g + 3, z + dz, "minecraft:sandstone_wall")
            elif rand and (dx + dz) % 2:
                b.set(x + dx, g + 2, z + dz, "minecraft:sandstone_wall")
    b.set(x + 1, g + 2, z + 1, "minecraft:smooth_sandstone")
    b.set(x + 1, g + 3, z + 1, "minecraft:candle", {"candles": "1", "lit": "true", "waterlogged": "false"})


def vuurtoren(b):
    """De Lange Njeg: red and white, a ladder inside, a lamp room with a gallery around it."""
    cx, cz = VT
    for x in range(cx - 6, cx + 7):                              # a foot of rock in the sand
        for z in range(cz - 6, cz + 7):
            d = math.hypot(x - cx, z - cz)
            g = b.grond(x, z)
            if g is None or d > 5.6:
                continue
            for y in range(g, G + 1):
                b.set(x, y, z, ROTS if d > 4.3 else KORSTSTENEN)
            b.wis(x, V, z, x, V + 30, z)
            b.vast.add((x, V, z))
    hoog = 18
    for y in range(V, V + hoog):
        kleur = "minecraft:red_concrete" if ((y - V) // 3) % 2 == 0 else WITB
        b.schijf(cx, y, cz, 3.6, kleur, vanaf=2.6)
        b.set(cx, y, cz - 2, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
        if (y - V) % 6 == 4:
            b.set(cx + 2, y, cz + 1, "minecraft:lantern", gb.lamp(True))
            b.set(cx + 2, y + 1, cz + 1, WITB)
    b.wis(cx, V, cz + 3, cx, V + 1, cz + 3)                       # the door, to the south
    deur(b, cx, V, cz + 3, "north", "birch")
    b.chest(cx - 1, V, cz + 1, "east", "guhs:chests/einde2_bouw_schelpenstad_vuurtoren")
    b.bord(cx + 1, V + 1, cz + 4, "south", ["De Lange Njeg", "vuurtoren", "Licht aan sinds", "altijd"])
    gy = V + hoog                                               # the gallery floor
    b.schijf(cx, gy, cz, 5.2, KORSTSTENEN)
    b.wis(cx, gy, cz - 2)
    b.set(cx, gy, cz - 2, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    b.set(cx, gy + 1, cz - 2, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    b.set(cx, gy + 1, cz - 3, WITB)
    b.set(cx, gy + 2, cz - 3, WITB)
    b.schijf(cx, gy + 1, cz, 5.2, HEK, vanaf=4.4)                # the rail
    for y in range(gy + 1, gy + 4):                             # the lamp room: glass around the light
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                d = math.hypot(x - cx, z - cz)
                if 1.5 < d <= 2.6 and not (z == cz - 2 and x == cx) and not (z == cz + 2 and x == cx and y < gy + 3):
                    b.set(x, y, z, "minecraft:glass")
        b.set(cx, y, cz, LAMP)
    b.schijf(cx, gy + 4, cz, 3.2, "minecraft:red_concrete")
    b.schijf(cx, gy + 5, cz, 2.2, "minecraft:red_concrete")
    b.schijf(cx, gy + 6, cz, 1.0, "minecraft:red_concrete")
    b.set(cx, gy + 7, cz, STAAFJE, {"facing": "up"})


def guhwatch(b, x0, z0):
    """The lifeguard tower: Guhwatch. A hut on four legs, red and yellow, with a ladder."""
    pl = V + 3                                                  # the platform
    for x in range(x0, x0 + 4):
        for z in range(z0, z0 + 4):
            hoek = x in (x0, x0 + 3) and z in (z0, z0 + 3)
            g = b.grond(x, z, van=V + 1)
            if hoek:
                for y in range((g if g is not None else G - 4) + 1, pl):
                    b.set(x, y, z, HEK)
            else:
                b.wis(x, (g or G) + 1, z, x, pl - 1, z)
            b.wis(x, pl, z, x, pl + 6, z)
            b.set(x, pl, z, BERK)
    for x in range(x0, x0 + 4):                                  # the hut: open to the sea (east)
        for z in range(z0, z0 + 4):
            if x == x0 or z in (z0, z0 + 3):
                if not (x == x0 + 3):
                    for y in range(pl + 1, pl + 3):
                        b.set(x, y, z, "minecraft:red_wool" if (x + z + y) % 2 else "minecraft:yellow_wool")
            b.set(x, pl + 3, z, "minecraft:red_wool" if z in (z0, z0 + 3) else "minecraft:yellow_wool")
    b.set(x0 + 1, pl + 2, z0 + 1, "minecraft:lantern", gb.lamp(True))
    b.set(x0 + 2, pl + 1, z0 + 2, "guhs:guh_stoel", {"facing": "east"})
    for x in (x0 + 3,):
        for z in (z0, z0 + 3):
            b.set(x, pl + 1, z, HEK)
            b.set(x, pl + 2, z, HEK)
    lx, lz = x0 + 4, z0 + 1                                       # the ladder, on the sea side
    g = b.grond(lx, lz, van=V + 1)
    for y in range((g if g is not None else G - 4) + 1, pl + 2):
        b.wis(lx, y, lz)
        b.set(lx, y, lz, "minecraft:ladder", {"facing": "east", "waterlogged": "false"})
    b.set(x0 + 3, pl + 4, z0 + 1, HEK)
    b.set(x0 + 3, pl + 5, z0 + 1, "minecraft:red_wool")
    b.bord(x0 + 4, pl, z0 + 2, "east", ["GUHWATCH", "Strandwacht", "D. Hasselguh", "(even zwemmen)"])


def pier(b):
    """The pier into the void: planks on posts, lanterns, a wide head with a rail where you get on David Hasselguh."""
    z = PIER_Z
    x0, x1 = 208, 246
    for x in range(x0, x1 + 1):
        kop = x >= x1 - 5
        for dz in (range(-4, 5) if kop else range(-1, 2)):
            b.wis(x, V, z + dz, x, V + 6, z + dz)
            b.set(x, G, z + dz, PALM)
            b.vast.add((x, V, z + dz))
        if x % 5 == 0 or x == x1:                                # the posts, down to the sand (or four deep into the void)
            for dz in ((-4, 4) if kop else (-1, 1)):
                g = b.grond(x, z + dz, van=G - 1)
                for y in range((g if g is not None else G - 5) + 1, G):
                    b.set(x, y, z + dz, PALMSTAM, {"axis": "y"})
    for x in range(x0 + 8, x1 - 5, 8):                           # lanterns on the north side
        b.paal(x, V, z - 1, 2, HEK)
    for x in range(x1 - 5, x1 + 1):                              # the head: a rail on the north and the east, open to the south (David)
        b.set(x, V, z - 4, HEK)
    for dz in range(-4, 1):
        b.set(x1, V, z + dz, HEK)
    for dz in (-4, -3, -2, 2, 3, 4):
        b.set(x1 - 5, V, z + dz, HEK)
    b.paal(x1, V, z - 4, 2, HEK)
    b.paal(x1 - 5, V, z + 4, 2, HEK)
    b.set(x1 - 1, V, z - 3, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.bord(x1 - 3, V, z - 3, "south", ["Veer naar", "Bikinibodem", "Vraag David", "(hij is groot)"], wall=False)


def strandleven(b):
    for i, kleur in enumerate(("red", "light_blue", "yellow")):
        hokje(b, 168 + 5 * i, 58, kleur)
    b.bord(183, V, 59, "south", ["Omkleedhokjes", "Bezet", "(al jaren)"], wall=False)
    strandtent(b, 190, 60)
    for (x, z, kleur) in ((172, 68, "pink"), (181, 72, "cyan"), (186, 66, "orange"), (205, 70, "magenta"), (176, 79, "lime"),
                          (197, 76, "red"), (219, 58, "light_blue"), (222, 38, "yellow")):
        parasol(b, x, z, kleur)
    zandkasteel(b, 205, 62)
    zandkasteel(b, 166, 74)
    for (x, z) in ((180, 64), (188, 78), (201, 82), (171, 84), (212, 64), (224, 52), (220, 32)):
        g = b.grond(x, z)
        if g is not None and b.naam(x, g, z) == ZAND and b.naam(x, g + 1, z) is None:
            b.set(x, g + 1, z, "guhs:tiki_fakkel")


def plekken(b):
    """The markers of Schelpenstad and its shore (build contract 6.7)."""
    px, pz = PLEIN
    guh = "guhs:bikinibodem_guh"
    b.zone("aankomst", px - 6, V, pz - 6, px + 6, V + 20, pz + 6, wie="het plein van Schelpenstad: hier land je")
    b.prop("aankomst_speler", px, V, pz + 3)
    b.prop("uitzicht", LAND_OOST + 44, 34, 84, wie="boven de Leegtezee: de stad, het strand en daarachter de heuvel met de loopgraaf")
    b.zone("schelpenstad", STAD_X, G - 4, 8, LAND_OOST, V + 30, 90, wie="Schelpenstad, op de rand van het strand")
    b.zone("strand", STAD_X + 2, G - 4, STRAND_Z, LAND_OOST, V + 4, 90, wie="het strand van kruimelzand")
    b.zone("kust", STRAND_X, G - 6, 12, W - 1, V + 4, 84, wie="de rand van de plaat: daarachter is de leegte de zee")
    b.plek("bewoner_1", px + 5, V, pz + 4, yaw=90.0, wie="een bewoner op het plein", wezen=guh)
    b.plek("bewoner_2", 174, V, 54, yaw=0.0, wie="een bewoner op de boulevard", wezen=guh)
    b.plek("bewoner_3", 194, V, 61, yaw=0.0, wie="de barguh van De Zoute Snoet, achter de bar", wezen=guh)
    b.plek("bewoner_4", 200, V, 57, yaw=180.0, wie="een bewoner op het strand", wezen=guh)
    kz = PIER_Z
    b.prop("hasselguh_opstap", 244, V, kz + 2, wie="de kop van de pier: hier stap je op")
    b.plek("hasselguh", 239, G - 3, kz + 12, yaw=90.0, wie="David Hasselguh, dobberend naast de pier (13 x 6 x 13 vrij)",
           wezen="guhs:david_hasselguh", los=True)
    b.plek("mindy_strand", 214, V, kz + 3, yaw=-90.0, wie="prinses Mindy-guh op het strand", wezen="guhs:mindy_guh")
    for id in ("aankomst_speler", "bewoner_1", "bewoner_2", "bewoner_3", "bewoner_4", "mindy_strand", "hasselguh_opstap"):
        b.vast.add(tuple(b.plekken[id]["pos"]))


def stad(b):
    bestrating(b)
    poort(b)
    winkel(b)
    kiosk(b, PLEIN[0] + 11, PLEIN[1] - 3)
    schelphuisje(b, 164, 24, ROZE)
    schelphuisje(b, 200, 16, TURK)
    schelphuisje(b, 204, 27, "minecraft:yellow_terracotta", deur_x=1)
    boulevard(b)
    strandleven(b)
    vuurtoren(b)
    guhwatch(b, 224, 42)
    pier(b)
    plekken(b)
