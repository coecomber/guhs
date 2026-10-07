"""
einde2, slice bouw_vuur: what stands inside the big crater of the Vuurnatie-hoofdstad.

  poort        the tunnel through the crater wall (the road from the harbour) with its two portals
  stad         the avenue, the wall of the Vuurheer's face, eight houses and shops, the market
  school       the Vuurnatie-school ("Vuurheer Mikai is de beste")
  dansgrot     the secret dance cave in the crater wall behind the school
  paleis       the palace on its podium: throne room with the flame curtain, two wings, the forecourt of the coronation
  vijver       the palace garden with the pond of the schildpad-eendguhs
  bunker       the stair behind the iron door in the west wing and the bunker under the forecourt (Dag van de Zwarte Zon)

Every function takes the build (einde2_bouw_vuur_bouw.Vuur) and records its markers itself.
"""
from features import guheinde_bouw as gb
from features.einde2_bouw_vuur_bouw import (BALK, BARST, BEITEL, EMBLEEM, F, GLAD, GLOEI, GOUD, HOUT, HOUT_HEK, HOUT_PLAAT, IJZER, KOOL, LAVA, MUUR, PORTRET, ROOD,
                                            ROOD_PLAAT, ROOD_TRAP, STENEN, STENEN_MUUR, STENEN_TRAP, ZWART, ZWART_MUUR, ZWART_PLAAT, ZWART_TRAP)

V = F + 1                           # where you stand on the crater floor
POORT_X = (125, 131)                # the tunnel: 7 wide
PALEIS_Y = F + 3                    # the palace podium
BUNKER_Y = 29                       # the bunker floor
RUIT = "orange_stained_glass_pane"
BLAD = {"distance": "7", "persistent": "true", "waterlogged": "false"}


# =====================================================================================================================
# the gate tunnel
# =====================================================================================================================
def poort(b, mond_z):
    """The tunnel from the crater floor (z 146) south to its mouth in the flank (mond_z), 7 wide and 6 high with a vaulted
    top, an inner and an outer portal, braziers and lamps in the vault."""
    x0, x1 = POORT_X
    z0 = 144
    b.fill(x0 - 1, F, z0, x1 + 1, F, mond_z, STENEN)
    b.fill(x0 - 1, F + 1, z0, x1 + 1, F + 8, mond_z, STENEN)
    b.lucht(x0, V, z0, x1, V + 5, mond_z)
    for z in range(z0, mond_z + 1):
        # the vault: the top corners closed, a keystone row of chiseled bricks
        for x, hoek in ((x0, "east"), (x1, "west")):
            b.set(x, V + 5, z, STENEN)
            b.trap(x, V + 4, z, STENEN_TRAP, gb.OPP[hoek], "top")
            b.trap(x + (1 if x == x0 else -1), V + 5, z, STENEN_TRAP, gb.OPP[hoek], "top")
        b.set(128, V + 6, z, BEITEL)
        b.set(128, F, z, ROOD if z % 2 else GLAD)
        if z % 6 == 1:
            b.hanglamp(128, V + 6, z, 2)
            b.set(x0, F, z, GLOEI)
            b.set(x1, F, z, GLOEI)
    for z, kant in ((z0, "north"), (mond_z, "south")):
        portaal(b, z, kant)
    b.sign(x0 - 1, V + 2, mond_z + 1, "south", ["Kraterpoort", "Welkom in de", "hoofdstad", "(buigen mag)"], glow=True)
    b.sign(x1 + 1, V + 2, mond_z + 1, "south", ["Verboden:", "watersturen,", "dansen en", "kritiek, njeg"], glow=True)
    b.prop("kraterpoort", 128, F, mond_z, wie="de poort in de kraterwand (de weg van de haven naar de stad)")
    b.prop("rustpunt_2", 128, V, z0 - 3, wie="rustpunt: net binnen de kraterpoort")


def portaal(b, z, kant):
    """A portal front on a tunnel end: two towers with braziers, a lintel with the emblem and a red roof."""
    x0, x1 = POORT_X
    dz = 1 if kant == "south" else -1
    for x in (x0 - 2, x1 + 2):
        b.fill(x - 1, F, z, x + 1, V + 9, z + dz, ZWART)
        b.fill(x - 1, V + 9, z, x + 1, V + 9, z + dz, ROOD)
        b.set(x, V + 3, z + 2 * dz, EMBLEEM)
        b.set(x, V + 2, z + 2 * dz, ZWART)
        b.set(x, V + 1, z + 2 * dz, ZWART)
        b.set(x, F, z + 2 * dz, ZWART)
        b.set(x, V, z + 2 * dz, ZWART)
        b.set(x, V + 4, z + 2 * dz, GLOEI)
        b.vlam(x, V + 5, z + 2 * dz)
    b.fill(x0 - 1, V + 6, z, x1 + 1, V + 8, z + dz, ZWART)
    b.lucht(x0, V, z, x1, V + 5, z + dz)
    b.fill(x0, F, z, x1, F, z + dz, GLAD)
    for x in (x0, x1):
        b.set(x, V + 5, z + dz, ZWART)
    b.fill(127, V + 7, z + dz, 129, V + 7, z + dz, EMBLEEM)
    b.dak(x0 - 4, z + (-1 if dz > 0 else -2), x1 + 4, z + (2 if dz > 0 else 1), V + 10, max_lagen=2)
    for x in (126, 130):
        b.set(x, V + 11, z + (0 if dz > 0 else -1), GLOEI)


# =====================================================================================================================
# houses
# =====================================================================================================================
def huis(b, x0, z0, x1, z1, y, kant, hoog=4, wand=MUUR, bord=None, ramen=True):
    """A Vuurnatie house on the rectangle: a footing of charcoal bricks, red sandstone walls between mangrove posts, paper
    windows, a door in the middle of side `kant`, a hipped red roof with golden corners, a lamp inside and one by the door.
    y = the floor (the ground). Returns (inside x0, z0, x1, z1, the door cell (x, z), the y of the ceiling)."""
    b.fill(x0, y, z0, x1, y, z1, STENEN)
    b.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, HOUT)
    for yy in range(y + 1, y + hoog + 1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    hoek = x in (x0, x1) and z in (z0, z1)
                    b.set(x, yy, z, BALK if hoek else (ZWART if yy == y + 1 else wand), {"axis": "y"} if hoek else None)
                else:
                    b.set(x, yy, z, gb.AIR)
    if ramen and hoog >= 4:
        for x in range(x0 + 2, x1 - 1):
            if (x - x0) % 3 != 1:
                for z in (z0, z1):
                    b.set(x, y + 2, z, RUIT)
                    b.set(x, y + 3, z, RUIT)
        for z in range(z0 + 2, z1 - 1):
            if (z - z0) % 3 != 1:
                for x in (x0, x1):
                    b.set(x, y + 2, z, RUIT)
                    b.set(x, y + 3, z, RUIT)
    xm, zm = (x0 + x1) // 2, (z0 + z1) // 2
    dx, dz = gb.DIRS[kant]
    deur = {"north": (xm, z0), "south": (xm, z1), "west": (x0, zm), "east": (x1, zm)}[kant]
    for yy in range(y + 1, y + min(hoog, 4) + 1):
        for k in (-1, 0, 1):
            cx, cz = deur[0] + (k if dz else 0), deur[1] + (k if dx else 0)
            b.set(cx, yy, cz, BALK if k else wand, {"axis": "y"} if k else None)
    b.deur(deur[0], y + 1, deur[1], gb.OPP[kant])
    b.set(deur[0] + dx, y, deur[1] + dz, GLAD)
    top = b.dak(x0 - 1, z0 - 1, x1 + 1, z1 + 1, y + hoog + 1, plafond=HOUT)
    b.hanglamp(xm, y + hoog + 1, zm, 1)
    if x1 - x0 > 10 or z1 - z0 > 10:
        b.hanglamp(xm + (3 if x1 - x0 > 10 else 0), y + hoog + 1, zm + (3 if z1 - z0 > 10 else 0), 1)
        b.hanglamp(xm - (3 if x1 - x0 > 10 else 0), y + hoog + 1, zm - (3 if z1 - z0 > 10 else 0), 1)
    b.lantaarn(deur[0] + dx + (1 if dz else 0), y + hoog, deur[1] + dz + (1 if dx else 0), True)
    if bord:
        b.sign(deur[0] + dx, y + 3, deur[1] + dz, kant, bord)
    b.geen_licht.add((deur[0], deur[1]))
    return x0 + 1, z0 + 1, x1 - 1, z1 - 1, deur, top


def woning(b, box, y, kant, rijk=False):
    """A home: beds against the back wall, a low table with cushions, a cupboard, a plant."""
    x0, z0, x1, z1 = box
    v = y + 1
    b.tapijt((x0 + x1) // 2 - 1, v, (z0 + z1) // 2 - 1, (x0 + x1) // 2 + 1, (z0 + z1) // 2 + 1, "red" if not rijk else "orange")
    b.set((x0 + x1) // 2, v, (z0 + z1) // 2, "guhs:guh_tafel", {"facing": "north"})
    achter = gb.OPP[kant]
    if achter in ("north", "south"):
        z = z0 if achter == "north" else z1
        zz = z0 + 1 if achter == "north" else z1 - 1
        b.slaapzak(x0, v, z, achter)
        b.slaapzak(x0 + 2, v, z, achter)
        b.boekenkast(x1, v, z, x1, v + 1, z)
        b.ton(x1 - 1, v, z)
        b.set(x1 - 1, v + 1, z, "guhs:potted_knabbelroos")
        b.set(x1, v, zz + (1 if achter == "north" else -1), "guhs:red_kussen")
    else:
        x = x0 if achter == "west" else x1
        b.slaapzak(x, v, z0, achter)
        b.slaapzak(x, v, z0 + 2, achter)
        b.boekenkast(x, v, z1, x, v + 1, z1)
        b.ton(x, v, z1 - 1)
        b.set(x, v + 1, z1 - 1, "guhs:potted_knabbelroos")
        b.set(x + (2 if achter == "west" else -2), v, z1, "guhs:red_kussen")
    if rijk:
        b.set(x0 if achter != "west" else x1, v, (z0 + z1) // 2 + 2, "guhs:guh_kast", {"facing": "east" if achter != "west" else "west", "open": "false"})


# =====================================================================================================================
# the city
# =====================================================================================================================
def stad(b):
    """The avenue from the gate to the forecourt, the wall with the Vuurheer's face, the houses, the shops and the market."""
    laan(b)
    gezicht(b)
    # --- the west row (doors on the avenue) ---
    box = huis(b, 108, 102, 120, 112, F, "east", bord=["Heet Hapje", "Vuurvlokken", "heet, heter,", "njeg"])
    bakkerij(b, box)
    box = huis(b, 108, 116, 120, 126, F, "east", bord=["Familie Mika", "(alle zeven)", "", "Niet aanbellen"])
    woning(b, box[:4], F, "east", rijk=True)
    box = huis(b, 110, 130, 120, 138, F, "east", bord=["Wachthuis", "Wij duwen u", "graag terug", "naar buiten"])
    wachthuis(b, box)
    # --- the east row ---
    box = huis(b, 136, 118, 148, 128, F, "west", bord=["Badhuis", "De Lauwe Plons", "Vandaag: lauw", "(altijd lauw)"])
    badhuis(b, box)
    box = huis(b, 136, 132, 146, 138, F, "west", bord=["Mika & Zonen", "Duwstokken", "en helmen op", "maat"])
    smederij(b, box)
    # --- the houses behind the rows ---
    box = huis(b, 92, 104, 103, 113, F, "south", bord=["Oma Mika", "Thee na vijven", "Geen bezoek", "voor vijven"])
    woning(b, box[:4], F, "south")
    box = huis(b, 96, 119, 104, 128, F, "east", bord=["Verhuur van", "vermommingen", "Vraag niet", "waarom, njeg"])
    kleermaker(b, box)
    box = huis(b, 151, 118, 160, 128, F, "west", bord=["Postkantoor", "Havikpost", "Brieven aan de", "Guhvatar: bak 2"])
    postkantoor(b, box)
    box = huis(b, 146, 86, 158, 96, F, "west", bord=["Eethuis", "De Dikke Draak", "Noedels zo lang", "als je staart"])
    eethuis(b, box)
    markt(b)
    exercitieplein(b)
    # --- the people of the city: where they stand ---
    for k, (x, z, yaw, wie) in enumerate([
            (123, 106, -90.0, "voor de bakkerij, eet vuurvlokken"), (133, 110, 90.0, "op de markt, pingelt"),
            (124, 122, -90.0, "veegt de laan"), (140, 106, 180.0, "marktkoopman achter zijn kraam"),
            (132, 134, 90.0, "voor de smederij, poetst een helm"), (99, 116, 0.0, "oma Mika's buurman, schoffelt as"),
            (146, 114, 180.0, "roddelt bij de markt"), (128, 100, 180.0, "kijkt op naar het gezicht van de Vuurheer")], 1):
        b.plek(f"bewoner_{k}", x, V, z, yaw=yaw, wie=wie, kind="VUURNATIE_BURGER_MIKA")


def laan(b):
    """The avenue: a red runner with a golden middle, braziers every eight blocks."""
    for z in range(93, 146):
        for x in range(125, 132):
            b.set(x, F, z, GLAD if x in (125, 131) else (ROOD if x != 128 else (GOUD if z % 8 == 0 else ROOD)))
        if z % 8 == 4:
            b.vuurpot(123, V, z, 2)
            b.vuurpot(133, V, z, 2)
            b.set(123, F, z, ZWART)
            b.set(133, F, z, ZWART)
    for z in (110, 126, 140):
        for x in (122, 134):
            b.set(x, F, z, ZWART)
            b.fill(x, V, z, x, V + 5, z, HOUT_HEK)
        for x in range(123, 134):
            b.set(x, V + 5, z, "guhs:vlaggetjes", {"axis": "x"})
        b.geen_licht.update(((122, z), (134, z)))
    b.sign(133, V, 143, "north", ["Paleis: vooruit", "Markt: rechts", "School: links", "Feest: nergens"], wall=False)
    # the cross street of the houses behind the rows
    for x in range(98, 159):
        if not 122 < x < 134:
            b.set(x, F, 115, GLAD)
            b.set(x, F, 116 if x > 134 else 114, b.naam(x, F, 116 if x > 134 else 114) or STENEN)


def gezicht(b):
    """In the middle of the avenue stands a wall with the face of the Vuurheer in mosaic (he looks at everybody who comes
    in), a row of flames in front of it; the avenue splits around it."""
    x0, z = 123, 98
    b.fill(x0, F, z - 1, x0 + 10, F, z + 1, ZWART)
    b.fill(x0, V, z, x0 + 10, V + 1, z, ZWART)
    b.fill(x0 + 1, V + 2, z, x0 + 9, V + 9, z, "purple_terracotta")
    rijen = ["pKKpppKKp",
             "pKKpppKKp",
             "ppKKpKKpp",
             "pRRpppRRp",
             "pRRpppRRp",
             "ppppppppp",
             "pKKKKKKKp",
             "ppWpppWpp"]
    pal = {"p": "purple_terracotta", "K": "black_concrete", "R": LAVA, "W": "white_concrete"}
    for j, rij in enumerate(rijen):
        for i, ch in enumerate(rij):
            b.set(x0 + 1 + i, V + 9 - j, z, pal[ch])
    # ears, a golden flame for a crown, the frame
    for x in (x0 + 1, x0 + 2, x0 + 8, x0 + 9):
        b.set(x, V + 10, z, "purple_terracotta")
    for x in (x0 + 1, x0 + 9):
        b.set(x, V + 11, z, "purple_terracotta")
    for x, top in ((x0 + 4, 11), (x0 + 5, 12), (x0 + 6, 11)):
        b.fill(x, V + 10, z, x, V + top, z, GOUD)
    b.vlam(x0 + 5, V + 13, z)
    for x in (x0, x0 + 10):
        b.fill(x, V + 2, z, x, V + 8, z, ZWART_MUUR)
        b.set(x, V + 9, z, GLOEI)
        b.vlam(x, V + 10, z)
    for x in range(x0 + 2, x0 + 9, 2):
        b.set(x, F, z + 1, GLOEI)
        b.vlam(x, V, z + 1)
    b.sign(x0 + 5, V + 1, z + 1, "south", ["Vuurheer Mikai", "kijkt mee", "(ook nu)", ""], glow=True)
    b.sign(x0 + 5, V + 1, z - 1, "north", ["Achterkant van", "de Vuurheer", "Niet naar", "kijken, njeg"], glow=True)
    b.prop("gezicht_mikai", x0 + 5, V + 5, z, wie="de muur met het gezicht van de Vuurheer", los=True)


def bakkerij(b, box):
    x0, z0, x1, z1, deur, _ = box
    v = F + 1
    b.fill(x1 - 3, v, z0 + 2, x1 - 3, v, z1 - 2, HOUT_PLAAT, gb.slab("top"))          # the counter
    b.set(x1 - 3, v, (z0 + z1) // 2, gb.AIR)
    b.set(x0, v, z0, "smoker", {"facing": "east", "lit": "false"})
    b.set(x0, v, z0 + 1, "smoker", {"facing": "east", "lit": "false"})
    for z in range(z0 + 3, z1 + 1):
        b.ton(x0, v, z)
        if z % 2:
            b.ton(x0, v + 1, z)
    b.set(x1 - 3, v + 1, z0 + 2, "guhs:potted_kaasbloem")
    b.set(x1 - 3, v + 1, z1 - 2, "guhs:potted_knabbelroos")
    b.set(x0 + 2, v, z1, "hay_block", {"axis": "y"})
    b.sign(x0, v + 2, z0 + 3, "east", ["Vuurvlokken", "per zak", "Blussen kost", "extra"])
    b.kist(x0 + 3, v, z0, "south", "winkel")
    b.plek("bakker", x0 + 4, v, (z0 + z1) // 2, yaw=-90.0, wie="de bakker van Heet Hapje", kind="VUURNATIE_BURGER_MIKA")


def wachthuis(b, box):
    x0, z0, x1, z1, deur, _ = box
    v = F + 1
    for z in (z0, z0 + 2, z0 + 4):
        b.slaapzak(x0, v, z, "west")
    b.fill(x1, v, z0, x1, v + 1, z0, "bookshelf")
    b.set(x0 + 4, v, z1, "guhs:guh_tafel", {"facing": "north"})
    b.set(x0 + 3, v, z1, "guhs:guh_stoel", {"facing": "east"})
    b.set(x0 + 5, v, z1, "guhs:guh_stoel", {"facing": "west"})
    b.set(x1, v, z1, "grindstone", {"face": "floor", "facing": "north"})
    b.sign(x0 + 6, v + 2, z0, "south", ["Dienstrooster", "ma-zo: duwen", "pauze: nooit", "(soms toch)"])


def badhuis(b, box):
    x0, z0, x1, z1, deur, _ = box
    v = F + 1
    # a sunken bath of lukewarm water in the back half, wooden duckboards around it
    bx0, bx1 = x0 + 4, x1 - 1
    b.fill(bx0, F - 1, z0 + 1, bx1, F - 1, z1 - 1, GLAD)
    b.fill(bx0, F, z0 + 1, bx1, F, z1 - 1, "water", {"level": "0"})
    for x in range(bx0 - 1, bx1 + 2):
        for z in range(z0, z1 + 1):
            if not (bx0 <= x <= bx1 and z0 + 1 <= z <= z1 - 1):
                if x <= x1:
                    b.set(x, F, z, GLAD)
    b.set(bx0, F - 1, z0 + 1, GLOEI)
    b.set(bx1, F - 1, z1 - 1, GLOEI)
    b.fill(bx0 - 1, F - 1, z0, bx1 + 1, F - 1, z1, STENEN)
    b.fill(bx0, F - 2, z0 + 1, bx1, F - 2, z1 - 1, STENEN)
    b.fill(bx0, F - 1, z0 + 1, bx1, F - 1, z1 - 1, GLAD)
    b.set(bx0, F - 1, z0 + 1, GLOEI)
    b.set(bx1, F - 1, z1 - 1, GLOEI)
    b.set(x0, v, z0, "guhs:guh_bank", {"facing": "east"})
    b.set(x0, v, z1, "guhs:guh_bank", {"facing": "east"})
    b.set(x0 + 2, v, z0, "water_cauldron", {"level": "3"})
    b.sign(x0 + 2, v + 2, z0, "south", ["Niet duiken", "Niet spetteren", "Wel vadsen", "njeg"])
    b.geen_licht.update((x, z) for x in range(bx0, bx1 + 1) for z in range(z0 + 1, z1))


def smederij(b, box):
    x0, z0, x1, z1, deur, _ = box
    v = F + 1
    b.set(x1, v, z0, "blast_furnace", {"facing": "west", "lit": "false"})
    b.set(x1, v, z0 + 1, "anvil", {"facing": "north"})
    b.set(x1, v, z1, "smithing_table")
    b.set(x1 - 2, v, z1, "cauldron")
    b.set(x1 - 4, v, z0, "guhs:roosterijzer")
    b.set(x1 - 4, v + 1, z0, "iron_bars")
    b.set(x1 - 5, v, z0, "guhs:roosterijzer")
    b.set(x1 - 5, v + 1, z0, "guhs:verkoolde_mikakop", {"facing": "south"})
    b.sign(x1 - 3, v + 2, z1, "north", ["Duwstok: 3 kn.", "Helm: 5 knabbels", "Echt zwaard:", "hebben we niet"])
    b.plek("smid", x1 - 2, v, z0 + 2, yaw=90.0, wie="de smid van Mika & Zonen", kind="VUURNATIE_BURGER_MIKA")


def kleermaker(b, box):
    x0, z0, x1, z1, deur, _ = box
    v = F + 1
    b.set(x0, v, z0, "loom", {"facing": "east"})
    for z, kleur in ((z0 + 2, "red"), (z0 + 3, "black"), (z0 + 4, "orange")):
        b.set(x0, v, z, f"{kleur}_wool")
        b.set(x0, v + 1, z, f"{kleur}_wool" if kleur != "black" else "red_wool")
    b.set(x0, v, z1, "guhs:guh_kast", {"facing": "east", "open": "true"})
    b.set(x0 + 3, v, z1, "fletching_table")
    b.tapijt(x0 + 2, v, z0 + 2, x0 + 4, z0 + 5, "red")
    b.sign(x0 + 3, v + 2, z0, "south", ["Vuurnatie-pak", "Past over elke", "skin. Ruilen", "kan niet"])
    b.kist(x1, v, z1, "north", "winkel")


def postkantoor(b, box):
    x0, z0, x1, z1, deur, _ = box
    v = F + 1
    b.fill(x0 + 3, v, z0 + 1, x0 + 3, v, z1 - 1, HOUT_PLAAT, gb.slab("top"))
    b.set(x0 + 3, v, (z0 + z1) // 2 + 2, gb.AIR)
    for z in range(z0, z1 + 1, 2):
        b.ton(x1, v, z)
    b.fill(x1, v + 1, z0, x1, v + 2, z0 + 2, "bookshelf")
    b.set(x1 - 2, v, z0, "lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    b.sign(x1 - 1, v + 2, z1, "north", ["Bak 1: gewoon", "Bak 2: Guhvatar", "Bak 3: klachten", "(bak 3 is vol)"])
    b.plek("postmika", x1 - 2, v, (z0 + z1) // 2, yaw=90.0, wie="de postbeambte (havikpost)", kind="VUURNATIE_BURGER_MIKA")


def eethuis(b, box):
    """De Dikke Draak: a counter with the kitchen behind it, three low tables with cushions."""
    x0, z0, x1, z1, deur, _ = box
    v = F + 1
    b.fill(x1 - 3, v, z0 + 1, x1 - 3, v, z1 - 1, HOUT_PLAAT, gb.slab("top"))
    b.set(x1 - 3, v, z1 - 1, gb.AIR)
    b.set(x1, v, z0, "smoker", {"facing": "west", "lit": "false"})
    b.set(x1, v, z0 + 1, "cauldron")
    b.ton(x1, v, z1)
    b.ton(x1, v + 1, z1)
    b.set(x1 - 3, v + 1, z0 + 2, "guhs:theepotje", {"facing": "west"})
    for k, z in enumerate((z0 + 1, z0 + 4, z0 + 7)):
        b.set(x0 + 3, v, z, "guhs:guh_tafel", {"facing": "north"})
        b.set(x0 + 2, v, z, "guhs:red_kussen")
        b.set(x0 + 4, v, z, "guhs:orange_kussen")
    b.sign(x0 + 6, v + 2, z0, "south", ["Vandaag: noedels", "Morgen: noedels", "Overmorgen:", "verrassing"])
    b.plek("kok", x1 - 1, v, (z0 + z1) // 2, yaw=90.0, wie="de kok van De Dikke Draak", kind="VUURNATIE_BURGER_MIKA")


def exercitieplein(b):
    """The drill square north of the school: two rows of practice dummies, a rack of shoving sticks, the order of the day."""
    for x in (93, 97, 101):
        for z in (72, 77):
            b.set(x, F, z, ZWART)
            b.set(x, V, z, HOUT_HEK)
            b.set(x, V + 1, z, "hay_block", {"axis": "y"})
            b.set(x, V + 2, z, "carved_pumpkin", {"facing": "south"})
            b.geen_licht.add((x, z))
    for x in range(92, 103):
        for z in (70, 79):
            b.set(x, F, z, ROOD)
    for x in (95, 96, 97, 98):
        b.set(x, V, 69, HOUT_HEK)
    b.vuurpot(92, V, 69, 1)
    b.vuurpot(102, V, 69, 1)
    b.sign(99, V, 80, "south", ["Exercitieplein", "Duwen: 3x daags", "Poppen duwen", "nooit terug"], wall=False)
    b.zone("exercitieplein", 91, V, 68, 103, V + 5, 80, wie="het exercitieplein naast de school")


def kraam(b, x, z, kleur, waar, facing="south"):
    """A market stall of 5 x 3: four posts, a striped awning and a counter with goods."""
    for dx in (0, 4):
        for dz in (0, 2):
            b.fill(x + dx, V, z + dz, x + dx, V + 2, z + dz, HOUT_HEK)
    for dx in range(-0, 5):
        for dz in range(0, 3):
            b.set(x + dx, V + 3, z + dz, f"{kleur}_wool" if dx % 2 == 0 else "white_wool")
    zc = z + (2 if facing == "south" else 0)
    for dx in range(1, 4):
        b.set(x + dx, V, zc, HOUT_PLAAT, gb.slab("top"))
        b.set(x + dx, V + 1, zc, waar[(dx - 1) % len(waar)])
    b.lantaarn(x + 2, V + 2, z + 1, True)
    b.geen_licht.update((x + dx, z + dz) for dx in range(5) for dz in range(3))


def markt(b):
    """The market square east of the avenue: four stalls around a well of lukewarm lava."""
    for x in range(136, 159):
        for z in range(100, 115):
            if (x + z) % 4 == 0:
                b.set(x, F, z, ROOD)
    kraam(b, 138, 101, "red", ["guhs:block_of_kaasknabbels", "guhs:potted_kaasbloem", "honeycomb_block"])
    kraam(b, 146, 101, "orange", ["melon", "pumpkin", "hay_block"])
    kraam(b, 152, 104, "yellow", ["guhs:potted_knabbelroos", "bookshelf", "guhs:potted_roze_guhbloem"], facing="south")
    kraam(b, 138, 110, "red", ["red_wool", "orange_wool", "black_wool"], facing="north")
    b.sign(140, V, 104, "south", ["Vuurvlokken", "Vuurkoekjes", "Vuurtaart", "(blusser gratis)"], wall=False)
    b.sign(148, V, 104, "south", ["Verse meloen", "uit de krater", "Een beetje", "geroosterd"], wall=False)
    b.sign(140, V, 109, "north", ["Stof per el", "Rood, roder", "en zwart", ""], wall=False)
    # the well
    cx, cz = 148, 109
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            b.set(cx + dx, F, cz + dz, ZWART)
            if dx or dz:
                b.set(cx + dx, V, cz + dz, ZWART_MUUR if (dx and dz) else ZWART_PLAAT, None if (dx and dz) else gb.slab())
    b.set(cx, F, cz, LAVA)
    b.sign(cx, V, cz + 2, "south", ["Lauwe lava", "Niet drinken", "Wel lekker", "warm"], wall=False)
    b.geen_licht.update((cx + dx, cz + dz) for dx in (-2, -1, 0, 1, 2) for dz in (-2, -1, 0, 1, 2))
    b.zone("markt", 136, V, 100, 158, V + 5, 114, wie="de markt van de hoofdstad")


# =====================================================================================================================
# the school
# =====================================================================================================================
def school(b):
    """One classroom: the blackboard and the portrait of the Vuurheer on the west wall, low desks in rows, the teacher's
    desk, a punishment corner; a little yard with a flag in front of the door."""
    x0, z0, x1, z1 = 86, 82, 103, 97
    ix0, iz0, ix1, iz1, deur, top = huis(b, x0, z0, x1, z1, F, "east", hoog=5, bord=["Vuurnatie-", "school", "Wie te laat is", "moet duwen"])
    v = F + 1
    # the blackboard, the portrait above the teacher, the slogan
    b.fill(x0, v + 1, 87, x0, v + 2, 89, "black_concrete")
    b.fill(x0, v + 1, 91, x0, v + 2, 93, "black_concrete")
    b.set(x0, v + 1, 90, PORTRET)
    b.set(x0, v + 2, 90, GOUD)
    b.sign(ix0, v + 3, 86, "east", ["Vuurheer Mikai", "is de beste", "", ""], glow=True)
    b.sign(ix0, v + 3, 94, "east", ["Les 1: buigen", "Les 2: duwen", "Les 3: nooit", "dansen, njeg"])
    b.sign(ix0, v + 1, 86, "east", ["Eed, elke dag:", "Mijn knabbels", "zijn van de", "Vuurheer"])
    b.prop("portret_mikai", x0, v + 1, 90, wie="het portret van Vuurheer Mikai, midden in het bord")
    # the teacher's desk and her spot
    b.set(ix0 + 2, v, 89, HOUT_PLAAT, gb.slab("top"))
    b.set(ix0 + 2, v, 90, HOUT_PLAAT, gb.slab("top"))
    b.set(ix0 + 2, v, 91, HOUT_PLAAT, gb.slab("top"))
    b.set(ix0 + 2, v + 1, 91, "guhs:potted_knabbelroos")
    b.plek("juf", ix0 + 1, v, 90, yaw=-90.0, wie="de juf van de Vuurnatie-school", kind="VUURNATIE_JUF")
    # three rows of low desks (the pupils sit on the floor behind them, looking west at the board)
    plekken = []
    for x in (ix0 + 5, ix0 + 8, ix0 + 11):
        for z in (84, 85, 88, 89, 92, 93):
            b.set(x, v, z, HOUT_PLAAT, gb.slab("top"))
            if z % 4 in (0, 1) and z in (84, 88, 92):
                plekken.append((x + 1, z))
        b.tapijt(x + 1, v, 84, x + 1, 93, "red")
        for z in (86, 87, 90, 91):
            b.set(x + 1, v, z, gb.AIR)
    for k, (x, z) in enumerate(plekken[:5], 1):
        b.plek(f"leerling_{k}", x, v, z, yaw=90.0, wie="een leerling achter zijn bankje", kind="VUURNATIE_LEERLING")
    sx, sz = plekken[7]
    b.prop("schoolbank_speler", sx, v, sz, wie="jouw plek in de klas (vermomd)")
    # the punishment corner, the bookcase, the coat rack
    b.fill(ix1, v, iz0, ix1, v + 2, iz0 + 2, "bookshelf")
    b.set(ix1, v, iz1, "guhs:red_kussen")
    b.sign(ix1, v + 2, iz1, "west", ["Strafhoek", "voor wie danst", "of vragen", "stelt"])
    b.kist(ix0, v, iz1, "north", "school")
    b.zone("school", x0, v, z0, x1, v + 5, z1, wie="het schooltje")
    # the yard
    for z in range(86, 95):
        for x in range(x1 + 1, x1 + 5):
            b.set(x, F, z, GLAD if (x + z) % 2 else ZWART)
    b.fill(x1 + 3, V, 86, x1 + 3, V + 5, 86, HOUT_HEK)
    b.set(x1 + 3, V + 6, 86, EMBLEEM)
    b.set(x1 + 3, F, 86, ZWART)
    b.geen_licht.add((x1 + 3, 86))
    b.prop("rustpunt_3", x1 + 3, V, 92, wie="rustpunt: het schoolplein")
    return x0, z0, x1, z1


# =====================================================================================================================
# the secret dance cave
# =====================================================================================================================
GROT = (59, 94, 75, 110)            # the chamber (inside): south of the school, the alley between the houses leads to it
GROT_Y = 45                         # its floor


def na_talud(g):
    """After the slope pass, before the ground becomes blocks: solid rock where the dance cave is carved and a mound of it
    above the ceiling; a little outcrop on the crater floor hides the way in."""
    x0, z0, x1, z1 = GROT
    cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
    g.massief(x0 - 3, z0 - 3, 83, z1 + 3, GROT_Y - 2)
    g.koepel(cx, cz, 10, GROT_Y + 11)
    g.koepel(78.5, cz, 2.5, F + 6, steil=3.0)


def dansgrot(b):
    """A cave in the crater wall, at the end of the alley south of the school: a passage that goes down, a round chamber
    with a light-up dance floor of nine by nine, a ledge for the band, lampions and a bar of barrels."""
    x0, z0, x1, z1 = GROT
    y, cx, cz = GROT_Y, (x0 + x1) // 2, (z0 + z1) // 2
    # the chamber: rounded, the ceiling higher in the middle
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            d = ((x - cx) / 8.6) ** 2 + ((z - cz) / 8.6) ** 2
            if d <= 1.0:
                hoog = 6 if d < 0.55 else (5 if d < 0.8 else 4)
                b.lucht(x, y + 1, z, x, y + hoog, z)
                b.set(x, y, z, b.kies([(KOOL, 60), (IJZER, 25), ("guhs:as_aarde", 15)]))
    # the passage from the crater floor: three wide, a step down per block until the cave's floor, then level
    for k, x in enumerate(range(82, x0, -1)):
        vloer = max(y, F - k)
        if vloer == y and ((x - cx) / 8.6) ** 2 <= 1.0:
            break
        for z in (cz - 1, cz, cz + 1):
            if vloer > y and k:
                b.trap(x, vloer, z, STENEN_TRAP, "east")
            else:
                b.set(x, vloer, z, KOOL)
        b.lucht(x, vloer + 1, cz - 1, x, vloer + 4, cz + 1)
    b.set(79, F - 3 + 5, cz, GLOEI)
    # the dance floor, the disco ball above it
    for x in range(cx - 4, cx + 5):
        for z in range(cz - 4, cz + 5):
            b.set(x, y, z, "guhs:disco_dansvloer", {"fase": str((x & 1) + 2 * (z & 1))})
    b.set(cx, y + 7, cz, KOOL)
    b.set(cx, y + 6, cz, "chain", {"axis": "y", "waterlogged": "false"})
    b.set(cx, y + 5, cz, "guhs:disco_bal")
    b.zone("dansvloer", cx - 4, y + 1, cz - 4, cx + 4, y + 4, cz + 4, wie="de dansvloer (negen bij negen)")
    b.prop("dans_speler", cx, y + 1, cz + 2, wie="waar jij danst")
    b.geen_licht.update((x, z) for x in range(cx - 4, cx + 5) for z in range(cz - 4, cz + 5))
    # the band on its ledge against the west wall
    for z in range(cz - 3, cz + 4):
        for x in (x0 + 1, x0 + 2):
            if b.naam(x, y + 1, z) == gb.AIR:
                b.set(x, y + 1, z, HOUT)
    b.set(x0 + 1, y + 2, cz - 2, "guhs:guh_xylofoon", {"facing": "east"})
    b.set(x0 + 1, y + 2, cz, "note_block", {"instrument": "harp", "note": "0", "powered": "false"})
    b.set(x0 + 1, y + 2, cz + 2, "jukebox", {"has_record": "false"})
    for k, z in enumerate((cz - 2, cz, cz + 2), 1):
        b.plek(f"muzikant_{k}", x0 + 2, y + 2, z, yaw=-90.0, wie=["de xylofonist", "de trommelaar", "de platendraaier"][k - 1], kind="VUURNATIE_LEERLING")
    # lampions under the ceiling, the bar, the signs
    for x, z, kleur in ((cx - 5, cz - 5, "roze"), (cx + 5, cz - 5, "geel"), (cx - 5, cz + 5, "mint"), (cx + 5, cz + 5, "roze"),
                        (cx, cz - 6, "geel"), (cx, cz + 6, "mint")):
        yy = y + 1
        while b.naam(x, yy + 1, z) == gb.AIR:
            yy += 1
        b.set(x, yy, z, f"guhs:lampion_{kleur}", {"hanging": "true"})
    for x in (cx + 2, cx + 3, cx + 4):
        b.ton(x, y + 1, z0 + 2)
    b.set(cx + 3, y + 2, z0 + 2, "guhs:potted_knabbelroos")
    b.sign(cx + 3, y + 1, z0 + 3, "south", ["Vuurvlokken-", "limonade", "(gewoon water", "met hoop)"], wall=False)
    b.sign(84, V, cz - 2, "east", ["Psst... feestje", "Niks tegen de", "Vuurheer zeggen", "njeg"], wall=False)
    for x, z in ((85, cz + 2), (86, cz + 2), (85, cz + 3)):
        b.ton(x, V, z)
    b.zone("dansgrot", x0, y + 1, z0, x1, y + 6, z1, wie="de geheime dansgrot")
    b.prop("dansgrot_ingang", 82, V, cz, wie="de ingang van de grot, aan het eind van het steegje naast de school")


# =====================================================================================================================
# the palace
# =====================================================================================================================
HAL = (117, 52, 139, 72)            # the throne hall (outside walls)
WEST = (105, 56, 117, 70)           # the west wing (war room, the door to the bunker)
OOST = (139, 56, 151, 70)           # the east wing (the prince's old rooms)


def paleis_grond(g):
    """The podium of the palace and the forecourt, before the ground becomes blocks."""
    from features import einde2_bouw_vuur_grond as gr
    g.vlak(103, 49, 153, 74, PALEIS_Y, gr.PLEIN)
    g.vlak(110, 75, 146, 92, F, gr.PLEIN)


def paleis(b):
    P = PALEIS_Y
    v = P + 1
    # --- the podium's face and the grand stair to the forecourt ---
    for x in range(103, 154):
        for y in range(F + 1, P + 1):
            if b.naam(x, y, 74) is not None:
                b.set(x, y, 74, ZWART)
    for k in range(3):
        for x in range(119, 138):
            b.trap(x, P - k, 75 + k, ZWART_TRAP, "north")
            b.fill(x, F + 1, 75 + k, x, P - k - 1, 75 + k, ZWART)
    for x in (118, 138):
        b.fill(x, F + 1, 75, x, P, 77, ZWART)
        b.vuurpot(x, P + 1, 77, 1)
    b.tapijt(126, v, 73, 130, 74, "red")
    # --- the throne hall ---
    x0, z0, x1, z1 = HAL
    hal_hoog = 9
    b.fill(x0, P, z0, x1, P, z1, ZWART)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            for y in range(v, v + hal_hoog):
                if rand:
                    hoek = (x in (x0, x1) and z in (z0, z1)) or (x - x0) % 4 == 2 and z in (z0, z1) or (z - z0) % 4 == 2 and x in (x0, x1)
                    b.set(x, y, z, ZWART if (y == v or hoek) else ROOD)
                else:
                    b.set(x, y, z, gb.AIR)
            if rand and not (x in (x0, x1) and z in (z0, z1)):
                b.set(x, v + hal_hoog - 2, z, GOUD if (x + z) % 2 == 0 else ROOD)
    # the floor: polished, a red runner with golden edges, two channels of lukewarm lava along the pillars
    b.fill(x0 + 1, P, z0 + 1, x1 - 1, P, z1 - 1, GLAD)
    for z in range(z0 + 7, z1):
        b.set(126, P, z, GOUD)
        b.set(130, P, z, GOUD)
        b.fill(127, P, z, 129, P, z, ROOD)
        for x in (x0 + 3, x1 - 3):
            b.set(x, P, z, LAVA)
    # two rows of pillars with golden heads, lamps between them
    for z in range(z0 + 8, z1 - 1, 4):
        for x in (x0 + 5, x1 - 5):
            b.fill(x, v, z, x, v + hal_hoog - 3, z, ZWART_MUUR)
            b.set(x, v, z, ZWART)
            b.set(x, v + hal_hoog - 2, z, GOUD)
            b.set(x, v + hal_hoog - 1, z, ZWART)
    # the dais (three steps), the throne under a golden flame, the emblem behind it
    for k in range(3):
        b.fill(x0 + 4 + k, v + k, z0 + 1, x1 - 4 - k, v + k, z0 + 5 - k, ROOD)
        for x in range(x0 + 4 + k, x1 - 3 - k):
            b.trap(x, v + k, z0 + 6 - k, ROOD_TRAP, "north")
        for z in range(z0 + 1, z0 + 6 - k):
            b.trap(x0 + 3 + k, v + k, z, ROOD_TRAP, "east")
            b.trap(x1 - 3 - k, v + k, z, ROOD_TRAP, "west")
        b.trap(x0 + 3 + k, v + k, z0 + 6 - k, ROOD_TRAP, "north", shape="outer_right")
        b.trap(x1 - 3 - k, v + k, z0 + 6 - k, ROOD_TRAP, "north", shape="outer_left")
    ty = v + 3
    b.fill(127, ty, z0 + 1, 129, ty, z0 + 1, GOUD)
    b.trap(128, ty, z0 + 2, "polished_blackstone_stairs", "north")
    b.set(127, ty, z0 + 2, GOUD)
    b.set(129, ty, z0 + 2, GOUD)
    b.fill(127, ty + 1, z0 + 1, 129, ty + 3, z0 + 1, GOUD)
    b.set(128, ty + 2, z0 + 1, EMBLEEM)
    b.set(128, ty + 4, z0 + 1, GLOEI)
    b.vlam(128, ty + 5, z0 + 1)
    for x in (125, 131):
        b.vuurpot(x, ty, z0 + 2, 1)
    b.prop("troon", 128, ty, z0 + 2, wie="de troon van de Vuurheer")
    b.plek("mikai", 128, ty, z0 + 3, yaw=0.0, wie="Vuurheer Mikai, voor zijn troon", kind="VUURHEER_MIKAI")
    # the flame curtain: a row of lukewarm flames on glowing coal in front of the dais, from wall to wall
    gz = z0 + 8
    for x in range(x0 + 1, x1):
        b.set(x, P, gz, GLOEI)
        b.vlam(x, v, gz)
        b.vlam(x, v + 1, gz)
        if (x - x0) % 2:
            b.vlam(x, v + 2, gz)
    b.zone("vlamgordijn", x0 + 1, v, gz, x1 - 1, v + 2, gz, wie="de rij vlammen voor de troon")
    b.geen_licht.update((x, gz) for x in range(x0, x1 + 1))
    b.zone("troonzaal", x0 + 1, v, z0 + 1, x1 - 1, v + hal_hoog - 1, z1 - 1, wie="de troonzaal")
    # the great door: five wide and five high, open, golden posts
    for x in range(126, 131):
        b.lucht(x, v, z1, x, v + 4, z1)
    for x in (125, 131):
        b.fill(x, v, z1, x, v + 5, z1, GOUD)
    b.fill(126, v + 5, z1, 130, v + 5, z1, GOUD)
    b.set(128, v + 6, z1, EMBLEEM)
    b.sign(124, v + 1, z1 + 1, "south", ["Troonzaal", "Voeten vegen", "(en buigen)", "njeg"], glow=True)
    b.sign(132, v + 1, z1 + 1, "south", ["Audiëntie:", "nooit", "Klachten:", "ook nooit"], glow=True)
    # lamps high in the hall, emblems on the side walls
    for z in range(z0 + 6, z1 - 1, 4):
        for x in (x0 + 2, x1 - 2, 128):
            b.hanglamp(x, v + hal_hoog, z, 2 if x != 128 else 3)
        b.set(x0, v + 4, z, EMBLEEM)
        b.set(x1, v + 4, z, EMBLEEM)
    # the roof: three tiers, a golden flame on top
    dak_y = v + hal_hoog
    b.fill(x0 + 1, dak_y, z0 + 1, x1 - 1, dak_y, z1 - 1, HOUT)
    top = b.pagode(x0 - 2, z0 - 2, x1 + 2, z1 + 2, dak_y, 3, hoog=4)
    xm, zm = (x0 + x1) // 2, (z0 + z1) // 2
    b.fill(xm, top + 1, zm, xm, top + 2, zm, GOUD)
    b.set(xm, top + 3, zm, GLOEI)
    b.vlam(xm, top + 4, zm)
    vleugel_west(b)
    vleugel_oost(b)
    voorplein(b)
    b.zone("paleis", 103, v, 49, 153, v + 30, 74, wie="het paleis van de Vuurheer")


def vleugel(b, box, deur_kant):
    """A wing of the palace: lower than the hall, the same walls, a door into the hall."""
    x0, z0, x1, z1 = box
    P, v, hoog = PALEIS_Y, PALEIS_Y + 1, 5
    b.fill(x0, P, z0, x1, P, z1, ZWART)
    b.fill(x0 + 1, P, z0 + 1, x1 - 1, P, z1 - 1, HOUT)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            if deur_kant == "east" and x == x1 or deur_kant == "west" and x == x0:
                continue                                    # (the wall it shares with the hall)
            for y in range(v, v + hoog):
                if rand:
                    b.set(x, y, z, ZWART if y == v or (x in (x0, x1) and z in (z0, z1)) else ROOD)
                else:
                    b.set(x, y, z, gb.AIR)
    for z in range(z0 + 2, z1 - 1, 3):
        x = x0 if deur_kant == "east" else x1
        b.set(x, v + 2, z, RUIT)
        b.set(x, v + 3, z, RUIT)
    for x in range(x0 + 3, x1 - 2, 3):
        for z in (z0, z1):
            b.set(x, v + 2, z, RUIT)
            b.set(x, v + 3, z, RUIT)
    xd = x1 if deur_kant == "east" else x0
    zm = (z0 + z1) // 2
    b.lucht(xd, v, zm, xd, v + 1, zm)
    b.deur(xd, v, zm, deur_kant, "mangrove")
    b.fill(x0 + 1, v + hoog, z0 + 1, x1 - 1, v + hoog, z1 - 1, HOUT)
    b.dak(x0 - 1 if deur_kant == "east" else x0 + 1, z0 - 1, x1 - 1 if deur_kant == "east" else x1 + 1, z1 + 1, v + hoog, max_lagen=4)
    for z in (z0 + 4, z1 - 4):
        b.hanglamp((x0 + x1) // 2, v + hoog, z, 1)
    return x0 + 1, z0 + 1, x1 - 1, z1 - 1


def vleugel_west(b):
    """The war room: the map table of the invasion, banners, and in the corner the iron door to the bunker stair."""
    ix0, iz0, ix1, iz1 = vleugel(b, WEST, "east")
    v = PALEIS_Y + 1
    # the map table (east of the stairwell of the bunker, which bunker() builds)
    for x in range(ix0 + 6, ix0 + 10):
        for z in range(iz0 + 5, iz0 + 9):
            b.set(x, v, z, "smooth_stone_slab", gb.slab("top"))
    b.set(ix0 + 7, v + 1, iz0 + 6, "red_candle", {"candles": "1", "lit": "false", "waterlogged": "false"})
    b.set(ix0 + 8, v + 1, iz0 + 7, "guhs:theepotje", {"facing": "south"})
    b.sign(ix0 + 7, v, iz0 + 9, "south", ["Krijgsraad", "Punt 1: trek", "Punt 2: nog", "meer trek"], wall=False)
    for z in (iz0 + 1, iz1 - 1):
        b.set(WEST[0], v + 2, z, EMBLEEM)
    b.boekenkast(ix1, v, iz1 - 3, ix1, v + 2, iz1)
    b.zone("krijgsraad", ix0, v, iz0, ix1, v + 4, iz1, wie="de krijgsraadzaal in de westvleugel")
    return ix0, iz0, ix1, iz1


def vleugel_oost(b):
    """The prince's old rooms: a bed with a dusty guh plush, a desk with a portrait turned to the wall, the door to the
    garden of the pond."""
    ix0, iz0, ix1, iz1 = vleugel(b, OOST, "west")
    v = PALEIS_Y + 1
    b.slaapzak(ix1, v, iz0 + 1, "east")
    b.set(ix1, v, iz0, "guhs:knuffel_normal", {"facing": "south"})
    b.tapijt(ix0 + 3, v, iz0 + 2, ix1 - 2, iz0 + 5, "orange")
    b.set(ix1, v, iz1, HOUT_PLAAT, gb.slab("top"))
    b.set(ix1 - 1, v, iz1, HOUT_PLAAT, gb.slab("top"))
    b.set(ix1, v + 1, iz1, "guhs:theepotje", {"facing": "west"})
    b.sign(ix1 - 2, v, iz1, "north", ["Kamer van", "prins Zuka", "Niets aanraken", "(eer!)"], wall=False)
    b.boekenkast(ix0, v, iz0, ix0 + 1, v + 2, iz0)
    b.kist(ix0 + 3, v, iz0, "south", "paleis")
    # the garden door in the east wall and the steps down to the garden
    x1 = OOST[2]
    zd = iz1 - 3
    b.lucht(x1, v, zd, x1, v + 1, zd)
    b.deur(x1, v, zd, "west", "mangrove")
    for k in range(3):
        for z in (zd - 1, zd, zd + 1):
            b.trap(x1 + 1 + k, PALEIS_Y - k, z, ZWART_TRAP, "west")
            b.fill(x1 + 1 + k, F, z, x1 + 1 + k, PALEIS_Y - k - 1, z, ZWART)
    b.zone("kamer_zuka", ix0, v, iz0, ix1, v + 4, iz1, wie="de oude kamer van prins Zuka in de oostvleugel")


def voorplein(b):
    """The forecourt: where the coronation is held. A sun of red and gold in the paving, braziers, and at its west edge
    the little chequered finish line of the luchtscooter-baan (the end of Sjokkel's long way)."""
    cx, cz = 128, 85
    for x in range(112, 145):
        for z in range(78, 93):
            d = max(abs(x - cx), abs(z - cz))
            hoek = abs(x - cx) == abs(z - cz) or x == cx or z == cz
            if d <= 6:
                b.set(x, F, z, GOUD if d == 0 else (ROOD if hoek or d == 6 else GLAD))
    for x in (114, 142):
        for z in (79, 91):
            b.vuurpot(x, V, z, 2)
            b.set(x, F, z, ZWART)
    for x in (115, 141):
        bloesemboom(b, x, V, 77)
    b.zone("kroning", 112, V, 76, 144, V + 8, 92, wie="het voorplein: de kroning")
    b.prop("kroning_zuka", 128, PALEIS_Y, 74, wie="waar Zuka gekroond wordt, boven aan de paleistrap")
    # the finish line: three wide, two deep, black and white
    for k, z in enumerate((84, 85, 86)):
        b.set(112, F, z, "white_concrete" if k % 2 == 0 else "black_concrete")
        b.set(113, F, z, "black_concrete" if k % 2 == 0 else "white_concrete")
    for z in (83, 87):
        b.fill(112, V, z, 112, V + 2, z, HOUT_HEK)
    b.set(112, V + 3, 83, "white_wool")
    b.set(112, V + 3, 87, "black_wool")
    b.geen_licht.update((x, z) for x in (112, 113) for z in range(83, 88))
    b.prop("knipoog_sjokkel_finish", 112, F, 85, wie="de finish van de luchtscooter-baan (drie breed)")
    b.pad("knipoog_sjokkel", [(118, V, 91), (115, V, 88), (113, V, 85)], wie="Sjokkels laatste stukje, tot op de finish")
    b.camera("knipoog_sjokkel", (121, V + 3, 88), (113, V, 85), zicht=["knipoog_sjokkel_finish", "kroning_zuka"])
    b.sign(111, V, 85, "east", ["FINISH", "luchtscooter-", "baan. Eindelijk", "njeg"], wall=False)


# =====================================================================================================================
# the garden of the pond
# =====================================================================================================================
def bloesemboom(b, x, y, z, hoog=4):
    """A blossom tree: a trunk on the ground at (x, y, z), a round crown, a lantern hanging in it."""
    b.fill(x, y, z, x, y + hoog, z, "cherry_log", {"axis": "y"})
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            for dy in range(hoog - 1, hoog + 3):
                r = abs(dx) + abs(dz) + abs(dy - hoog) * 1.3
                if r <= 4.2 and not (dx == 0 and dz == 0 and dy <= hoog) and b.inside(x + dx, y + dy, z + dz) \
                        and b.naam(x + dx, y + dy, z + dz) in (None, gb.AIR):
                    b.set(x + dx, y + dy, z + dz, "cherry_leaves", BLAD)
    b.lantaarn(x - 1, y + hoog - 2, z, True)
    b.geen_licht.add((x, z))


def vijver(b):
    """The palace garden east of the palace: a pond with lily pads for the schildpad-eendguhs, a blossom tree, a bench."""
    x0, z0, x1, z1 = 153, 64, 165, 80
    cx, cz = 159, 73
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if b.naam(x, F, z) is None:
                continue
            b.set(x, F, z, "guhs:kaasmos" if (x + z) % 5 else "moss_block")
    nat = []
    for x in range(cx - 5, cx + 6):
        for z in range(cz - 5, cz + 6):
            d = ((x - cx) / 4.6) ** 2 + ((z - cz) / 5.4) ** 2
            if d <= 1.0:
                b.set(x, F - 2, z, "sand" if (x * 3 + z) % 4 else GLOEI)
                b.set(x, F - 3, z, KOOL)
                b.set(x, F - 1, z, "water", {"level": "0"})
                b.set(x, F, z, "water", {"level": "0"})
                nat.append((x, z))
            elif d <= 1.5 and b.naam(x, F, z) is not None:
                b.set(x, F, z, "smooth_sandstone" if (x + z) % 3 else STENEN)
                for y in (F - 1, F - 2, F - 3):
                    b.set(x, y, z, KOOL)
    b.geen_licht.update(nat)
    for x, z in ((cx - 2, cz - 1), (cx + 1, cz + 3), (cx + 2, cz - 3)):
        b.set(x, V, z, "guhs:guh_waterlelie")
    b.zone("paleisvijver", cx - 5, F - 1, cz - 6, cx + 5, V + 2, cz + 6, wie="de vijver van de schildpad-eendguhs")
    for k, (x, z) in enumerate(((cx - 1, cz - 2), (cx + 1, cz), (cx - 2, cz + 2), (cx + 2, cz + 3)), 1):
        b.wezen(f"eendguh_{k}", x, V, z, "guhs:schildpad_eendguh", yaw=90.0 * k, honk=6)
    # the feeding spot with its basket, the bench, the sign
    b.set(cx - 6, V, cz, "barrel", {"facing": "up", "open": "true"})
    b.prop("brood", cx - 6, V, cz, wie="de broodmand bij de vijver")
    b.set(cx - 7, V, cz + 2, "guhs:guh_bank", {"facing": "east"})
    b.sign(cx - 6, V, cz - 2, "east", ["Eendguhs voeren", "Alleen brood,", "geen vuurvlokken", "njeg"], wall=False)
    bloesemboom(b, x1 - 2, V, z0 + 3)
    # stone lanterns
    for x, z in ((x0 + 1, z0 + 1), (x0 + 1, z1 - 1), (x1 - 1, z1 - 1)):
        if b.naam(x, F, z) is not None:
            b.vuurpot(x, V, z, 1, paal=STENEN_MUUR)
            b.geen_licht.add((x, z))
    b.zone("paleistuin", x0, V, z0, x1, V + 8, z1, wie="de paleistuin")


# =====================================================================================================================
# the bunker
# =====================================================================================================================
BUNKER = (104, 84, 152, 110)        # the bunker's shell (x0, z0, x1, z1)
TRAP_X = WEST[0] + 3                # the bunker stair runs south along this x
TRAP_Z0 = WEST[1] + 4               # its landing in the war room


def bunker(b):
    """Behind an iron door in the war room a stair goes down under the forecourt: an entry hall, a long corridor of grill
    iron, the map room, the hiding room of the Vuurheer and, at the end, the underground throne."""
    P, B = PALEIS_Y, BUNKER_Y
    sx, z_top, n = TRAP_X, TRAP_Z0, PALEIS_Y - BUNKER_Y
    wand = [(STENEN, 60), (BARST, 15), (IJZER, 25)]
    x0, z0, x1, z1 = BUNKER
    assert z_top + n == z0, (z_top, n, z0)
    b.mix(x0, B - 1, z0, x1, B + 6, z1, wand)
    # --- the stair: its own shell, one step per block, a glowing coal in the wall every fifth step ---
    for k in range(n + 1):
        z, y = z_top + k, P - k
        b.mix(sx - 2, y - 1, z, sx + 2, min(y + 5, P + 4) if k < 7 else y + 5, z, wand)
        b.lucht(sx - 1, y + 1, z, sx + 1, min(y + 4, P + 3) if k < 7 else y + 4, z)
        if k:
            for x in (sx - 1, sx, sx + 1):
                b.trap(x, y, z, STENEN_TRAP, "north")
        if k % 5 == 2:
            b.set(sx - 2, y + 3, z, GLOEI)
            b.set(sx + 2, y + 3, z, GLOEI)
    # --- its head in the war room: a stairwell of grill iron with an iron door on the east side ---
    b.fill(sx - 2, P + 1, z_top - 2, sx + 2, P + 4, z_top - 2, IJZER)
    b.fill(sx - 2, P + 1, z_top - 1, sx - 2, P + 4, z_top - 1, IJZER)
    b.fill(sx + 2, P + 1, z_top - 1, sx + 2, P + 4, z_top - 1, IJZER)
    b.fill(sx - 1, P + 4, z_top - 1, sx + 1, P + 4, z_top - 1, IJZER)
    b.lucht(sx - 1, P + 1, z_top - 1, sx + 1, P + 3, z_top - 1)
    b.fill(sx - 1, P, z_top - 1, sx + 1, P, z_top, GLAD)
    b.deur(sx + 2, P + 1, z_top - 1, "west", "iron")
    b.set(sx + 3, P, z_top - 1, GLAD)
    b.sign(sx + 3, P + 3, z_top - 1, "east", ["Bunker", "Alleen voor de", "Vuurheer (en", "zijn thee)"], glow=True)
    b.prop("bunker_ingang", sx + 2, P + 1, z_top - 1, wie="de ijzeren deur naar de bunkertrap (onderste helft)")
    b.lantaarn(sx, P + 3, z_top, True)
    b.geen_licht.add((sx + 3, z_top - 1))
    # --- the entry hall at the foot of the stair ---
    b.lucht(sx - 1, B + 1, z0 + 1, sx + 4, B + 4, z0 + 6)
    b.fill(sx - 1, B, z0 + 1, sx + 4, B, z0 + 6, GLAD)
    b.lantaarn(sx + 2, B + 4, z0 + 3, True)
    b.sign(sx + 4, B + 2, z0 + 1, "west", ["Dag v.d. Zwarte", "Zon: 8 minuten", "geen vuur. Dus:", "wij zitten hier"], glow=True)
    b.prop("rustpunt_4", sx + 1, B + 1, z0 + 2, wie="rustpunt: de hal van de bunker")
    # --- the corridor east: a red runner, pillars of grill iron, a lamp between every pair ---
    gz0, gz1 = z0 + 3, z0 + 5
    b.lucht(sx + 5, B + 1, gz0, 140, B + 4, gz1)
    for x in range(sx + 5, 141):
        b.set(x, B, gz0, GLAD)
        b.set(x, B, gz0 + 1, ROOD if x % 2 else GLAD)
        b.set(x, B, gz1, GLAD)
        if x % 6 == 0:
            b.fill(x, B + 1, gz0 - 1, x, B + 4, gz0 - 1, "guhs:roosterijzer_pilaar", {"axis": "y"})
            b.fill(x, B + 1, gz1 + 1, x, B + 4, gz1 + 1, "guhs:roosterijzer_pilaar", {"axis": "y"})
        if x % 6 == 3:
            b.lantaarn(x, B + 4, gz0 + 1, True)
    # --- the map room (south of the corridor) ---
    kx0, kx1, kz0, kz1 = 116, 126, gz1 + 2, gz1 + 10
    kamer(b, kx0, kz0, kx1, kz1, B, deur=(121, gz1 + 1), kant="north")
    for x in range(kx0 + 3, kx1 - 2):
        for z in range(kz0 + 3, kz1 - 2):
            b.set(x, B + 1, z, "smooth_stone_slab", gb.slab("top"))
    b.set(121, B + 2, kz0 + 4, "red_candle", {"candles": "3", "lit": "false", "waterlogged": "false"})
    b.sign(121, B + 2, kz1, "north", ["Plan Zwarte Zon", "Stap 1: weg", "Stap 2: thee", "Stap 3: wachten"])
    b.boekenkast(kx0, B + 1, kz0, kx0, B + 3, kz0 + 2)
    b.zone("kaartkamer", kx0, B + 1, kz0, kx1, B + 4, kz1, wie="de kaartkamer van de bunker")
    # --- the hiding room of the Vuurheer (where Zuka faces his father) ---
    vx0, vx1, vz0, vz1 = 129, 139, gz1 + 2, gz1 + 12
    kamer(b, vx0, vz0, vx1, vz1, B, deur=(134, gz1 + 1), kant="north", vloer=ROOD)
    for x in range(vx0 + 2, vx1 - 1):
        b.set(x, B + 1, vz1 - 2, ROOD_PLAAT, gb.slab())
        b.set(x, B + 1, vz1 - 1, ROOD)
        b.set(x, B + 1, vz1, ROOD)
    b.set(135, B + 2, vz1, "guhs:red_kussen")
    b.set(132, B + 2, vz1, "guhs:theepotje", {"facing": "north"})
    for x in (vx0, vx1):
        b.vuurpot(x, B + 1, vz1, 1)
    b.set(134, B + 3, vz1 + 1, EMBLEEM)
    b.tapijt(133, B + 1, vz0 + 1, 135, vz1 - 3, "red")
    b.prop("zuka_vader", 134, B + 1, vz0 + 4, wie="waar Zuka tegenover zijn vader staat")
    b.plek("mikai_bunker", 134, B + 2, vz1 - 1, yaw=180.0, wie="Vuurheer Mikai in zijn schuilkamer", kind="VUURHEER_MIKAI")
    b.sign(133, B + 2, gz1, "north", ["Schuilkamer", "van de Vuurheer", "Niet storen", "(hij schuilt)"], glow=True)
    b.zone("schuilkamer", vx0, B + 1, vz0, vx1, B + 4, vz1, wie="de schuilkamer van de Vuurheer")
    # --- the underground throne at the end of the corridor (where Azula-Mika waits) ---
    tx0, tx1, tz0, tz1 = 142, 151, z0 + 1, z0 + 11
    b.lucht(tx0, B + 1, tz0, tx1, B + 5, tz1)
    b.fill(tx0, B, tz0, tx1, B, tz1, GLAD)
    b.lucht(141, B + 1, gz0 + 1, 141, B + 2, gz0 + 1)
    b.deur(141, B + 1, gz0 + 1, "west", "iron", open_=True)
    b.set(141, B, gz0 + 1, GLAD)
    zt = (tz0 + tz1) // 2
    for k in range(2):
        b.fill(tx1 - 3 + k, B + 1 + k, tz0 + 2 + k, tx1, B + 1 + k, tz1 - 2 - k, ROOD)
        for z in range(tz0 + 2 + k, tz1 - 1 - k):
            b.trap(tx1 - 4 + k, B + 1 + k, z, ROOD_TRAP, "east")
    b.trap(tx1, B + 3, zt, "polished_blackstone_stairs", "east")
    b.set(tx1, B + 3, zt - 1, GOUD)
    b.set(tx1, B + 3, zt + 1, GOUD)
    b.set(tx1 + 1, B + 5, zt, EMBLEEM)
    for z in (tz0, tz1):
        b.vuurpot(tx0 + 1, B + 1, z, 1)
        b.vuurpot(tx1, B + 1, z, 1)
    for x, z in ((tx0 + 3, zt - 3), (tx0 + 3, zt + 3), (tx1 - 2, zt)):
        b.lantaarn(x, B + 5, z, True)
    b.prop("bunker_troon", tx1, B + 3, zt, wie="de troon in de bunker")
    b.plek("azula_bunker", tx1 - 1, B + 3, zt, yaw=90.0, wie="Azula-Mika, die het al wist", kind="AZULA_MIKA")
    b.zone("bunker_troonzaal", tx0, B + 1, tz0, tx1, B + 5, tz1, wie="de troonzaal van de bunker")
    b.zone("bunker", x0, B + 1, z0, x1, B + 5, z1, wie="de bunker onder het voorplein")


def kamer(b, x0, z0, x1, z1, y, deur, kant, vloer=None, hoog=4):
    """A room carved into the bunker's shell: (x0..x1, z0..z1) is the inside, y the floor; an iron door at `deur` in the
    wall on side `kant`, lamps on the ceiling."""
    b.lucht(x0, y + 1, z0, x1, y + hoog, z1)
    b.fill(x0, y, z0, x1, y, z1, vloer or STENEN)
    b.lucht(deur[0], y + 1, deur[1], deur[0], y + 2, deur[1])
    b.deur(deur[0], y + 1, deur[1], kant, "iron", open_=True)
    b.set(deur[0], y, deur[1], GLAD)
    for x in range(x0 + 2, x1 - 1, 4):
        for z in range(z0 + 2, z1 - 1, 4):
            b.lantaarn(x, y + hoog, z, True)
