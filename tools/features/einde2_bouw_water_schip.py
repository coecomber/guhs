"""
einde2 (bouw_water): the ships of the Vuurnatie at the Noordelijke Waterstam.

  - vlaggenschip(b): admiraal Zhao-Mika's flagship "VNS Schroeiplek", moored bow-first at the harbour shelf with its bow
    ramp down. All grill iron and charcoal stone (the Vuurnatie are Mika's, and Mika's come from the Barbecuether). You can
    board it over the ramp or, quietly, up the ladder on its side; on deck a catapult, crates, the bridge tower (guard room,
    Zhao's cabin, the bridge) and the funnel; below deck (down the companionway aft of the tower, or the ladder in the guard
    room) the hold, the bunks, the boiler room and the brig with the cage for the moon fish.
  - vloot(h): the overlay template laag_vloot: three more ships for the siege. Blocks only (no block entities), and they
    lie ON the water (nothing below the water line): a later phase puts the overlay there and takes it away again as air.
"""
from features import guheinde_bouw as gb
from features.einde2_bouw_water_grond import SCHIP, SEA, T0

STAAL, ROOSTER, PILAAR, TRALIES = "guhs:gepolijst_roosterijzer", "guhs:roosterijzer", "guhs:roosterijzer_pilaar", "guhs:roosterijzer_tralies"
STENEN = "guhs:houtskoolsteen_stenen"
S_TRAP, S_PLAAT, S_MUUR = STENEN + "_trap", STENEN + "_plaat", STENEN + "_muur"
ROOD = "red_nether_bricks"
GLOED = "guhs:gloeikool"
TEKEN = "guhs:einde2_bouw_water_vuurnatie_teken"
KIST = "guhs:chests/einde2_bouw_water_schip"
LUCHT = "minecraft:air"

X0, X1, Z0, Z1 = SCHIP
XM = (X0 + X1) // 2
KIEL = SEA - 2               # the bottom plate (and the floor of the hold)
DEK = SEA + 4                # the deck plate (you stand one higher)
TOREN = (XM - 3, XM + 3, 181, 189)       # the bridge tower on deck


def halve_breedte(z, z0=Z0, z1=Z1, breed=5):
    """How far the hull reaches to either side of its middle line at z (a pointed bow in the north, a blunt stern)."""
    boeg = [0, 1, 1, 2, 3, 3, 4]
    if z - z0 < len(boeg):
        return min(breed, boeg[z - z0])
    hek = [breed - 2, breed - 1, breed - 1]
    if z1 - z < len(hek):
        return hek[z1 - z]
    return breed


def romp(zet, xm, z0, z1, onder, dek, breed, binnen_lucht_tot):
    """The hull between y = onder and the deck plate: plates outside, air inside (explicit air up to binnen_lucht_tot, so the
    sea does not stand in the hold), a bulwark of red brick above the deck. zet(x, y, z, block, props=None)."""
    def in_romp(x, z):
        return z0 <= z <= z1 and abs(x - xm) <= halve_breedte(z, z0, z1, breed)

    for z in range(z0, z1 + 1):
        hb = halve_breedte(z, z0, z1, breed)
        for x in range(xm - hb, xm + hb + 1):
            rand = not all(in_romp(x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            for y in range(onder, dek + 1):
                if y == onder or y == dek or rand:
                    naam = ROOSTER if y == dek else (STAAL if (y - onder) % 3 else ROOSTER)
                    zet(x, y, z, naam)
                elif y <= binnen_lucht_tot:
                    zet(x, y, z, LUCHT)
            if rand:
                zet(x, dek + 1, z, ROOD)


def vlaggenschip(b):
    def zet(x, y, z, naam, props=None):
        b.set(x, y, z, naam, props)

    romp(zet, XM, Z0, Z1, KIEL, DEK, 5, DEK - 1)
    y = DEK + 1                                                   # where you stand on deck
    # the bow ramp: four steps of charcoal stone from the deck down to the shelf, three wide, on a solid tongue of iron
    for i in range(4):
        z, top = Z0 + 1 - i, DEK - i
        for x in range(XM - 1, XM + 2):
            for yy in range(T0 + 1, top):
                b.set(x, yy, z, STAAL)
            b.set(x, top, z, S_TRAP, gb.stair("south"))
            for yy in range(top + 1, top + 4):
                b.set(x, yy, z, LUCHT)
    for x in range(XM - 1, XM + 2):
        b.set(x, DEK + 1, Z0 + 2, LUCHT)
    # the emblem of the Vuurnatie on both sides of the bow and on the stern
    for x in (X0, X1):
        b.set(x, DEK - 1, Z0 + 9, TEKEN)
    b.set(XM, DEK - 1, Z1, TEKEN)
    # the ladder up the west side (the quiet way on board): a gap in the bulwark above it
    lz = 186
    for yy in range(T0 + 1, DEK + 1):
        b.set(X0 - 1, yy, lz, "ladder", {"facing": "west", "waterlogged": "false"})
    b.set(X0, DEK + 1, lz, LUCHT)
    # the foredeck: a catapult with a glowing coal in its spoon, a heap of coals, crates to hide behind
    for x in (XM - 1, XM + 1):
        b.set(x, y, 173, "dark_oak_fence")
        b.set(x, y + 1, 173, "dark_oak_fence")
    for x in range(XM - 1, XM + 2):
        b.set(x, y + 2, 173, "dark_oak_log", {"axis": "x"})
    for z in range(171, 176):                                     # the arm: a beam over the axle, a weight in front, the spoon behind
        b.set(XM, y + 3, z, "dark_oak_log", {"axis": "z"})
    b.set(XM, y + 2, 171, STAAL)
    b.set(XM, y + 4, 175, GLOED)
    for x, z in ((X0 + 2, 177), (X0 + 2, 178), (X1 - 2, 176)):
        b.set(x, y, z, GLOED if (x, z) == (X0 + 2, 177) else "coal_block")
    for x, z, n in ((X0 + 1, 191, 2), (X0 + 2, 191, 1), (X0 + 1, 192, 1), (X1 - 1, 179, 2), (X1 - 1, 180, 1), (X1 - 2, 196, 2), (X1 - 1, 196, 1)):
        for i in range(n):
            b.set(x, y + i, z, "barrel", {"facing": "up", "open": "false"})
    for z in (170, 178, 191, 199):
        for x in (X0, X1):
            if b.get(x, DEK + 1, z) == "minecraft:" + ROOD:
                b.set(x, DEK + 2, z, "lantern", gb.lamp(False))
    # the bridge tower: three rooms on top of each other, a ladder inside, a roof with an eave
    tx0, tx1, tz0, tz1 = TOREN
    for v in range(3):
        ya = y + v * 4
        for x in range(tx0, tx1 + 1):
            for z in range(tz0, tz1 + 1):
                if x in (tx0, tx1) or z in (tz0, tz1):
                    hoek = x in (tx0, tx1) and z in (tz0, tz1)
                    for yy in range(ya, ya + 3):
                        raam = yy == ya + 1 and not hoek and (v == 2 or (v == 1 and ((x - tx0) % 2 == 1 or (z - tz0) % 2 == 1)))
                        if (x, z) == (tx1, tz0 + 4):
                            raam = False                             # (the ladder hangs on this wall)
                        b.set(x, yy, z, PILAAR if hoek else ("orange_stained_glass_pane" if raam else STAAL), {"axis": "y"} if hoek else None)
                b.set(x, ya + 3, z, STENEN)
    for z in (tz0, tz1):                                          # doors fore and aft on deck level
        gb.door(b, XM, y, z, "south" if z == tz0 else "north", wood="iron")
        b.set(XM, y + 2, z, TEKEN)
    # (an iron door wants a button: one on each side of each door)
    for z, kant in ((tz0 - 1, "north"), (tz0 + 1, "south"), (tz1 - 1, "north"), (tz1 + 1, "south")):
        b.set(XM + 1, y + 1, z, "polished_blackstone_button", {"face": "wall", "facing": kant, "powered": "false"})
    for yy in range(y, y + 9):                                    # the ladder on the east wall, through both ceilings
        b.set(tx1 - 1, yy, tz0 + 4, "ladder", {"facing": "west", "waterlogged": "false"})
    dak = y + 11
    for x in range(tx0 - 1, tx1 + 2):                             # the roof: an eave of slabs, a cap of red brick, a spike
        for z in range(tz0 - 1, tz1 + 2):
            if x in (tx0 - 1, tx1 + 1) or z in (tz0 - 1, tz1 + 1):
                b.set(x, dak, z, S_PLAAT, gb.slab("bottom"))
    b.fill(tx0 + 1, dak + 1, tz0 + 1, tx1 - 1, dak + 1, tz1 - 1, ROOD)
    b.fill(tx0 + 2, dak + 2, tz0 + 2, tx1 - 2, dak + 2, tz1 - 2, STENEN)
    b.set(XM, dak + 3, (tz0 + tz1) // 2, GLOED)
    b.set(XM, dak + 4, (tz0 + tz1) // 2, "lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    b.set(XM, y + 6, tz0 - 1, TEKEN)
    # room 1: the guard room (a table, a rack of coals, the hatch down)
    b.set(tx0 + 1, y, tz0 + 1, "smithing_table")
    b.set(tx0 + 1, y, tz0 + 2, "barrel", {"facing": "up", "open": "false"})
    b.set(tx1 - 1, y, tz1 - 1, GLOED)
    b.sign(tx0 + 1, y + 1, tz1 - 1, "east", ["Wachtlokaal", "Wie slaapt op", "wacht, schrobt", "het dek. -Zhao"])
    # room 2: Zhao's cabin (a red bed, a map table, his chest, his opinion of himself)
    ya = y + 4
    b.set(tx0 + 1, ya, tz0 + 1, "red_bed", {"facing": "north", "part": "head", "occupied": "false"})
    b.set(tx0 + 1, ya, tz0 + 2, "red_bed", {"facing": "north", "part": "foot", "occupied": "false"})
    b.set(tx0 + 2, ya, tz1 - 1, "cartography_table")
    b.chest(tx0 + 1, ya, tz1 - 1, "east", KIST)
    b.set(XM, ya, tz0 + 1, "red_carpet")
    b.set(XM, ya, tz0 + 2, "red_carpet")
    b.sign(tx0 + 1, ya + 1, tz0 + 4, "east", ["Hut van Zhao", "de Onoverwinbare", "(zegt Zhao", "zelf, njeg)"])
    b.set(XM, ya + 3, tz0 + 4, GLOED)
    # room 3: the bridge (the wheel, a far look over the city)
    ya = y + 8
    b.set(XM, ya, tz0 + 1, "grindstone", {"face": "wall", "facing": "south"})
    b.set(XM, ya + 3, tz0 + 4, GLOED)
    b.sign(tx0 + 1, ya + 1, tz1 - 1, "east", ["Brug", "Plan: de maan", "vangen. Wat kan", "er misgaan?"])
    b.set(XM, y + 3, tz0 + 4, GLOED)
    # the funnel aft of the tower: a pipe of iron with a fire on top
    for x in range(XM - 1, XM + 2):
        for z in range(192, 195):
            for yy in range(y, y + 8):
                if (x, z) != (XM, 193):
                    b.set(x, yy, z, PILAAR, {"axis": "y"})
    for yy in range(y, y + 6):
        b.set(XM, yy, 193, STAAL)
    b.set(XM, y + 6, 193, GLOED)
    b.set(XM, y + 7, 193, "campfire", {"facing": "north", "lit": "true", "signal_fire": "true", "waterlogged": "false"})
    b.sign(XM - 2, y + 1, 193, "west", ["VNS", "Schroeiplek", "Vlaggenschip", "van Zhao-Mika"])
    b.sign(XM + 1, y, Z0 + 6, "north", ["Verboden voor", "guhs, vissen", "en de maan.", "- de admiraal"], wall=False)
    # --- below deck ---
    yh = KIEL + 1                                                 # where you stand in the hold
    b.fill(X0 + 1, KIEL, Z0 + 8, X1 - 1, KIEL, Z1 - 3, STENEN)
    for z in range(Z0 + 10, Z1 - 2, 6):                           # light in the ceiling of the hold
        b.set(XM, DEK, z, GLOED)
    # the hatch in the guard room and its ladder (on a pillar)
    hx, hz = tx0 + 1, tz0 + 5
    for yy in range(yh, DEK):
        b.set(hx - 1, yy, hz, PILAAR, {"axis": "y"})
    for yy in range(yh, DEK + 1):
        b.set(hx, yy, hz, "ladder", {"facing": "east", "waterlogged": "false"})
    # the companionway aft of the tower, on the starboard side: an open well in the deck, five steps down into the boiler room
    ax, az = X1 - 2, tz1 + 2
    for i in range(5):
        z, top = az + i, DEK - 1 - i
        for yy in range(top + 1, DEK + 1):
            b.set(ax, yy, z, LUCHT)
        b.set(ax, top, z, S_TRAP, gb.stair("north"))
        for yy in range(yh, top):
            b.set(ax, yy, z, STENEN)
    for z in range(az, az + 5):                                   # a rail along the well, on the side of the deck
        b.set(ax - 1, DEK + 1, z, S_MUUR)
    # the fore hold: what the Mika's took along (coals, barrels, a suspicious amount of knabbels)
    for x, z, blok, n in ((X0 + 1, 173, "coal_block", 2), (X0 + 2, 173, "coal_block", 1), (X0 + 1, 174, "coal_block", 1), (X1 - 1, 172, "barrel", 2),
                          (X1 - 1, 173, "barrel", 3), (X1 - 2, 173, "barrel", 1), (X1 - 1, 176, "guhs:block_of_kaasknabbels", 2),
                          (X1 - 2, 176, "guhs:block_of_kaasknabbels", 1), (X0 + 1, 178, "barrel", 1)):
        for i in range(n):
            b.set(x, yh + i, z, blok, {"facing": "up", "open": "false"} if blok == "barrel" else None)
    b.sign(X1 - 2, yh, 177, "north", ["Knabbels", "'Gevonden.'", "Niet van guhs", "afgepakt. Echt."], wall=False)
    # the bunks under the tower: hammocks between posts, a wall with a doorway on both ends
    for z in (180, 190):
        for x in range(X0 + 1, X1):
            for yy in range(yh, DEK):
                if abs(x - XM) > 1 or yy == DEK - 1:
                    b.set(x, yy, z, STAAL)
    for z in (183, 187):
        for x in (X1 - 1, X1 - 3):
            b.set(x, yh, z, S_MUUR)
            b.set(x, yh + 1, z, S_MUUR)
        b.set(X1 - 2, yh + 1, z, "red_wool")
        b.set(X1 - 2, yh + 2, z, "red_carpet")
    b.sign(X1 - 1, yh + 1, 185, "west", ["Hier vadsen", "alleen Mika's.", "Hangmat 3 is", "van de kok."])
    # the boiler room: furnaces, glowing coals, pipes
    for x in (X0 + 1, X0 + 2):
        b.set(x, yh, 193, "blast_furnace", {"facing": "south", "lit": "true"})
        b.set(x, yh + 1, 193, PILAAR, {"axis": "y"})
        b.set(x, yh + 2, 193, PILAAR, {"axis": "y"})
    b.set(X0 + 1, yh, 194, GLOED)
    b.set(X1 - 1, yh, 192, "coal_block")
    b.set(X1 - 1, yh, 193, "coal_block")
    b.set(X1 - 1, yh + 1, 193, "coal_block")
    # the brig in the stern: a wall of bars with an iron door, and in it the cage with the tub for the moon fish
    bz = 199
    for x in range(X0 + 1, X1):
        for yy in range(yh, DEK):
            b.set(x, yy, bz, TRALIES if yy < DEK - 1 else STAAL)
    for x in (XM + 1, XM + 3):
        for yy in range(yh, DEK - 1):
            b.set(x, yy, bz, STAAL)
    gb.door(b, XM + 2, yh, bz, "north", wood="iron")
    b.set(XM + 3, yh + 1, bz - 1, "polished_blackstone_button", {"face": "wall", "facing": "north", "powered": "false"})
    b.set(XM + 3, yh + 1, bz + 1, "polished_blackstone_button", {"face": "wall", "facing": "south", "powered": "false"})
    kx, kz = XM - 1, bz + 2
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if (dx, dz) != (0, 0):
                b.set(kx + dx, yh, kz + dz, TRALIES)
                b.set(kx + dx, yh + 1, kz + dz, TRALIES)
            b.set(kx + dx, yh + 2, kz + dz, STAAL)
    b.set(kx, yh, kz, "water_cauldron", {"level": "3"})
    b.set(kx, yh + 2, kz, GLOED)
    b.sign(XM, yh, bz - 1, "north", ["Kooi voor 1", "vis. NIET", "opeten! Ook", "niet proeven."], wall=False)
    return {"ladder": (X0 - 1, T0 + 1, lz), "luik": (ax, DEK + 1, az - 1), "trap_onder": (ax, yh, az + 5), "kooi": (kx, yh, kz), "celdeur": (XM + 2, yh, bz),
            "brug": (XM, y + 8, tz0 + 3),
            "ruim": (XM, yh, 175), "kooien": (XM, yh, 185), "dek_voor": (XM + 2, y, 176), "dek_toren": (XM, y, tz0 - 2),
            "plank": (XM, y, Z0 + 3), "dek_achter": (XM, y, 198)}


# =====================================================================================================================
# the overlay: the rest of the fleet
# =====================================================================================================================
VLOOT_GROOTTE = (92, 18, 44)
VLOOT_SCHEPEN = [(10, 12, 3, 26), (45, 2, 4, 34), (80, 14, 3, 26)]      # (middle x, bow z, half width, length), overlay coordinates


def vloot(h):
    """laag_vloot: three ships of the Vuurnatie, bows to the wall. Their flat bottoms lie on the water (overlay y 0 = the
    first block above the sea)."""
    s = h.Structure(VLOOT_GROOTTE)

    def zet(x, y, z, naam, props=None):
        s.set(x, y, z, gb.mc(naam), dict(props) if props else None)

    for xm, z0, breed, lengte in VLOOT_SCHEPEN:
        z1 = z0 + lengte - 1
        romp(zet, xm, z0, z1, 0, 3, breed, -1)
        y = 4
        mid = z0 + lengte // 2
        tx0, tx1, tz0, tz1 = xm - 2, xm + 2, mid - 1, mid + 4
        for v in range(2):                                        # the tower: two closed storeys (no way in: they are scenery)
            ya = y + v * 4
            for x in range(tx0, tx1 + 1):
                for z in range(tz0, tz1 + 1):
                    rand = x in (tx0, tx1) or z in (tz0, tz1)
                    hoek = x in (tx0, tx1) and z in (tz0, tz1)
                    for yy in range(ya, ya + 3):
                        if rand:
                            raam = v == 1 and yy == ya + 1 and not hoek
                            zet(x, yy, z, PILAAR if hoek else ("orange_stained_glass" if raam else STAAL), {"axis": "y"} if hoek else None)
                    zet(x, ya + 3, z, STENEN)
        zet(xm, y + 5, mid + 1, GLOED)
        for x in range(tx0 - 1, tx1 + 2):
            for z in range(tz0 - 1, tz1 + 2):
                if x in (tx0 - 1, tx1 + 1) or z in (tz0 - 1, tz1 + 1):
                    zet(x, y + 7, z, S_PLAAT, gb.slab("bottom"))
        s.fill(tx0 + 1, y + 8, tz0 + 1, tx1 - 1, y + 8, tz1 - 1, gb.mc(ROOD))
        zet(xm, y + 9, mid + 1, GLOED)
        zet(xm, y + 2, tz0 - 1, TEKEN)
        for yy in range(y, y + 6):                                # the funnel, glowing at its mouth
            zet(xm, yy, tz1 + 3, PILAAR, {"axis": "y"})
        zet(xm, y + 6, tz1 + 3, GLOED)
        for x in (xm - 1, xm + 1):                                # the catapult on the foredeck
            zet(x, y, z0 + 6, "dark_oak_fence", {"east": "false", "north": "false", "south": "false", "west": "false", "waterlogged": "false"})
        for x in range(xm - 1, xm + 2):
            zet(x, y + 1, z0 + 6, "dark_oak_log", {"axis": "x"})
        zet(xm, y + 2, z0 + 5, "dark_oak_slab", gb.slab("bottom"))
        zet(xm, y + 3, z0 + 4, GLOED)
        zet(xm, y + 2, z0 + 4, "dark_oak_slab", gb.slab("top"))
        for x in (xm - breed, xm + breed):
            zet(x, 2, z0 + 9, TEKEN)
            for z in (z0 + 8, z1 - 4):
                zet(x, 5, z, "lantern", gb.lamp(False))
    return s
