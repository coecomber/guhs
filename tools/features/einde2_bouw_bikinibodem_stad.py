"""
einde2, slice bouw_bikinibodem: the build of Bikinibodem onder de leegte, part 2: the houses of the town (called by
einde2_bouw_bikinibodem.maak after the ground, the roads and the dome of einde2_bouw_bikinibodem_bouw.py).

Every house is written in its own corner (bouw.Hoek: x and z local, y the y of the build), is closed, lit and furnished,
and records the markers of build contract 6.7: where its characters stand (nothing is placed: phase B puts the cast
there), the props a scene needs and its zones. In street order:

  krokante_knabbel   De Krokante Knabbel of Meneer Krabguh: dining hall, till, kitchen with the grill, office with the safe
  vetemmer           De Vetemmer across the street: the bucket (an empty restaurant) and Plankmika's lab behind it, with
                     Karen, the Plan Z cabinet and the sneak route past cameras and lasers to the stolen recipe
  ijssalon           the Guhfy Guhber ice parlour, with a sundae on its roof
  bootschool         juf Pufguh's boating school inside the loop of her course (a lighthouse on a classroom)
  bellenkom          De Bellenkom: the stadium with its stage
  sponsguh_huis      Sponsguh's knabbelkoorvrucht            } the Schelpstraat, as in the show:
  octo_huis          Octo Tentakel's moai full of himself    } fruit, grumpy head, rock
  patguh_rots        Patguh's kruimelrots, a room under it   }
  boomkoepel         Sandy-guh's air dome with a Guhmensie tree
  rusthuis           the rest home of Zeemeerman and Mosselguh, and the parking spot of the invisible boat
  huisjes            six round little houses of the townsfolk, the bus stop
  guhlander          the mooring of De Vliegende Guhlander and the ship itself (an overlay: it is not always there)
"""
import math

from features import guheinde_bouw as gb
from features.einde2_bouw_bikinibodem_bouw import (ARCHIEF, CAMERA, G, Hoek, KASSA, KLARINET, KLUIS, KORAAL, KSTEEN, LAMP, PORTRET, ROTS, STEEN, STEEN_MUUR,
                                                   STROOM, V)

GLAS = "minecraft:glass"
RUIT = "minecraft:glass_pane"
BEWONER = "guhs:bikinibodem_guh"
PAREL, TEGEL = "guhs:parelmoer", "guhs:parelmoer_tegels"


def schijf(cx, cz, r, r_in=-1.0):
    """The cells (x, z) of a disc (or a ring: further than r_in) around a centre."""
    for x in range(int(math.floor(cx - r)), int(math.ceil(cx + r)) + 1):
        for z in range(int(math.floor(cz - r)), int(math.ceil(cz + r)) + 1):
            d = math.hypot(x - cx, z - cz)
            if r_in < d <= r:
                yield x, z


def log(axis="y"):
    return {"axis": axis}


# =====================================================================================================================
# De Krokante Knabbel
# =====================================================================================================================
def krokante_knabbel(b):
    k = Hoek(b, 127, 82)
    BX, BZ = 25, 24
    k.bouwgrond(-1, -1, BX + 1, BZ + 2)

    def dak(x):                                  # the arched roof (a lobster pot): its height above this x
        return V + 5 + int(round(4.6 * math.sqrt(max(0.0, 1.0 - ((x - 12.5) / 13.6) ** 2))))

    for x in range(BX + 1):
        for z in range(BZ + 1):
            keuken, kantoor = z <= 7 and x <= 14, z <= 7 and x >= 16
            k.set(x, G, z, "smooth_stone" if keuken else "dark_oak_planks" if kantoor else "spruce_planks")
    # walls up to the roof, the roof with a rib every fourth block, corner posts
    for x in range(BX + 1):
        for z in range(BZ + 1):
            if x in (0, BX) or z in (0, BZ):
                for y in range(V, dak(x)):
                    paal = (x in (0, BX) and z in (0, BZ)) or (z in (0, BZ) and x in (6, 19)) and y <= V + 4
                    k.set(x, y, z, "stripped_spruce_log" if paal else "oak_planks", log() if paal else None)
        for z in range(-1, BZ + 2):
            rib = z % 4 == 0
            k.set(x, dak(x), z, "stripped_spruce_log" if rib else "spruce_planks", log("x") if rib else None)
            if 0 < x < BX and dak(x) - dak(x + (1 if x < 12 else -1)) > 1:      # (close a step of two in the arch)
                k.set(x, dak(x) - 1, z, "spruce_planks")
    # the front: big windows, the double door, the name over it
    for x in list(range(2, 11)) + list(range(15, 24)):
        if x in (6, 19):
            continue
        for y in range(V + 1, V + 4):
            k.set(x, y, BZ, RUIT)
    k.deur(12, V, BZ, "south", "spruce", "right")
    k.deur(13, V, BZ, "south", "spruce", "left")
    k.sign(12, V + 3, BZ + 1, "south", ["De Krokante", "Knabbel", "", ""])
    k.sign(13, V + 3, BZ + 1, "south", ["Sinds altijd", "Geld terug:", "nooit", "- M. Krabguh"])
    for z in (6, 12, 18):                        # (side windows)
        for y in (V + 1, V + 2):
            k.set(0, y, z + 3, RUIT)
            k.set(BX, y, z + 3, RUIT)
    # the back part has a ceiling of its own (the kitchen and the office are rooms); the wall between it and the hall
    for x in range(1, BX):
        for z in range(1, 9):
            k.set(x, V + 4, z, "spruce_slab", gb.slab("top"))
        for y in range(V, V + 4):
            k.set(x, y, 8, "oak_planks")
    for y in range(V, V + 4):
        for z in range(1, 8):
            k.set(15, y, z, "oak_planks")
    k.deur(3, V, 8, "south", "spruce")                                         # the kitchen door
    for x in range(6, 10):                                                     # the serving hatch
        k.hol(x, V + 1, 8, x, V + 2, 8)
        k.set(x, V, 8, "smooth_stone")
    k.deur(20, V, 8, "south", "dark_oak")                                      # the office door
    k.sign(21, V + 1, 9, "south", ["Kantoor", "M. Krabguh", "Kloppen kost", "1 knabbel"])
    # --- the kitchen ---
    k.set(7, V, 1, "guhs:krokante_grill", {"facing": "south", "bakt": "false"})
    k.prop("grill", 7, V, 1, wie="de grill van De Krokante Knabbel")
    k.prop("grill_speler", 7, V, 2, wie="hier sta je te bakken")
    k.plek("sponsguh_werk", 9, V, 2, yaw=90.0, wie="Sponsguh aan de grill", wezen="guhs:sponsguh")
    for x in (2, 3, 4, 5, 10, 11, 12):
        k.plaat(x, V, 1, "smooth_stone_slab", "top")
    k.set(13, V, 1, "cauldron")
    k.set(14, V, 1, "barrel", {"facing": "up", "open": "false"})
    k.set(14, V + 1, 1, "barrel", {"facing": "south", "open": "false"})
    k.kist(1, V, 1, "east", "keuken")
    k.set(1, V, 6, "guhs:block_of_kaasknabbels")
    k.set(1, V, 5, "hay_block", log())
    k.set(4, V + 1, 1, "flower_pot")
    k.luik(11, V + 2, 1, "south", "iron_trapdoor", "top", "true")
    k.luik(12, V + 2, 1, "south", "iron_trapdoor", "top", "true")
    k.sign(5, V + 2, 1, "south", ["Het recept is", "GEHEIM.", "Ook voor jou,", "Octo. Njeg."])
    for (x, z) in ((4, 4), (11, 4)):
        k.lantaarn(x, V + 3, z)
    # --- the office: the desk, the safe of the secret recipe, Krabguh's first knabbel ---
    for x in (19, 20, 21):
        k.plaat(x, V, 3, "dark_oak_slab", "top")
    k.stoel(20, V, 2, "south", "dark_oak_stairs")
    k.sier(23, V, 1, KLUIS, "south")
    k.prop("kluis", 23, V, 1, wie="de kluis met het geheime recept")
    k.set(17, V, 1, "guhs:block_of_kaasknabbels")
    k.set(16, V, 1, "guhs:block_of_kaasknabbels")
    k.set(16, V + 1, 1, "guhs:block_of_kaasknabbels")
    k.sign(18, V + 1, 1, "south", ["Mijn eerste", "knabbel.", "Niet aankomen", "(ik tel ze)"])
    for x in range(17, 24):
        for z in range(4, 7):
            k.tapijt(x, V, z, "red")
    k.plek("krabguh", 20, V, 5, yaw=0.0, wie="Meneer Krabguh", wezen="guhs:meneer_krabguh")
    k.lantaarn(20, V + 3, 4)
    k.set(24, V, 7, "flower_pot")
    k.zone("kantoor", 16, V, 1, 24, V + 3, 7, wie="het kantoor van Meneer Krabguh")
    # --- the hall: the till on the counter, the order tables, room for Parel ---
    for x in range(10, 17):
        k.set(x, V, 10, "spruce_planks")
    k.sier(13, V + 1, 10, KASSA, "south")
    k.prop("kassa", 13, V + 1, 10, wie="de kassa, op de toonbank")
    k.plek("octo_kassa", 13, V, 9, yaw=0.0, wie="Octo Tentakel achter de kassa (hij zucht)", wezen="guhs:octo_mika")
    k.sign(11, V + 1, 10, "south", ["Bestel hier", "Glimlach:", "niet bij de", "prijs inbegrepen"], wall=False)
    tafels = [(4, 13), (4, 19), (21, 21)]
    for i, (x, z) in enumerate(tafels, 1):
        k.set(x, V, z, "guhs:besteltafel", {"facing": "south"})
        k.prop(f"besteltafel_{i}", x, V, z, wie="een besteltafel met een bel")
        k.stoel(x - 1, V, z, "east")
        k.stoel(x, V, z - 1, "south")
    for (x, z) in ((8, 16), (9, 21), (22, 13)):
        k.tafel(x, V, z)
        k.stoel(x - 1, V, z, "east")
        k.stoel(x + 1, V, z, "west")
    for i, (x, z, yaw) in enumerate(((5, 13, 90.0), (5, 19, 90.0), (22, 21, 90.0), (8, 17, 180.0)), 1):
        k.plek(f"klant_{i}", x, V, z, yaw=yaw, wie="een klant aan tafel", wezen=BEWONER)
    k.plek("parel", 17, V, 16, yaw=0.0, wie="Parel, de dochter van Meneer Krabguh (5 x 5 x 5 vrij)", wezen="guhs:parel_guh")
    for z in (12, 21):                           # two beams with lanterns
        for x in range(1, BX):
            k.set(x, V + 5, z, "stripped_spruce_log", log("x"))
        for x in (4, 9, 16, 21):
            k.lantaarn(x, V + 4, z)
    k.sign(1, V + 2, 16, "east", ["Menu", "Knabbelburger", "Knabbelburger", "met knabbel"])
    k.sign(1, V + 1, 16, "east", ["Ook lekker:", "nog een", "Knabbelburger", ""])
    k.set(24, V, 23, "flower_pot")
    k.zone("krokante_knabbel", 0, V, 0, BX, V + 9, BZ, wie="De Krokante Knabbel")
    # --- outside: the sign on its pole, where the Knabbelburgermobiel stands at the start of deel 2 ---
    for y in range(V, V + 4):
        k.set(3, y, BZ + 2, STEEN_MUUR)
    k.set(3, V + 4, BZ + 2, KORAAL)
    k.sign(3, V + 3, BZ + 3, "south", ["Vandaag:", "Knabbelburger", "Morgen:", "Knabbelburger"])
    b.prop("mobiel_start", 139, V, 112, wie="voor de deur: hier staat de Knabbelburgermobiel bij het begin van deel 2 (5 x 4 x 7 vrij)")


# =====================================================================================================================
# De Vetemmer: the bucket and the lab
# =====================================================================================================================
def vetemmer(b):
    k = Hoek(b, 126, 118)
    k.bouwgrond(0, -1, 26, 31)
    ex, ez, r = 13, 9, 8.5
    # --- the bucket: a round wall with two hoops, a rim, a handle over it ---
    for (x, z) in schijf(ex, ez, r):
        binnen = math.hypot(x - ex, z - ez) <= r - 1
        k.set(x, G, z, "polished_andesite" if (x + z) % 2 else "stone")
        k.set(x, V + 8, z, "light_gray_concrete")
        if not binnen:
            for y in range(V, V + 8):
                k.set(x, y, z, "iron_block" if y in (V + 2, V + 6) else "light_gray_concrete")
    for (x, z) in schijf(ex, ez, r + 1, r):
        k.set(x, V + 8, z, "iron_block")
    def hengsel(x):
        return int(round(math.sqrt(max(0.0, 81.0 - (x - ex) ** 2)) * 0.75))

    for x in range(ex - 9, ex + 10):                                           # the handle
        laag = min(hengsel(x - 1), hengsel(x + 1), hengsel(x)) if abs(x - ex) < 9 else 0
        for dy in range(laag, hengsel(x) + 1):
            k.set(x, V + 8 + dy, ez, "gray_concrete")
    k.deur(ex, V, 1, "north", "iron")                                          # (an iron door: it opens with the button)
    k.set(ex + 1, V + 1, 0, "stone_button", {"face": "wall", "facing": "north", "powered": "false"})
    k.set(ex - 1, V + 1, 2, "stone_button", {"face": "wall", "facing": "south", "powered": "false"})
    k.sign(ex, V + 2, 0, "north", ["De Vetemmer", "Vet lekker!", "(zegt Plankmika)", ""])
    k.sign(ex - 1, V + 1, 0, "north", ["Vandaag open", "Morgen ook", "Klanten: 0", "Njeg."])
    k.prop("vetemmer_ingang", ex, V, 0, wie="de voordeur van De Vetemmer")
    # the empty restaurant: tables nobody sits at, cobwebs, a sad menu
    for (x, z) in ((9, 6), (17, 6), (9, 12), (17, 12)):
        k.tafel(x, V, z, "warped")
        k.stoel(x - 1, V, z, "east", "warped_stairs")
        k.stoel(x + 1, V, z, "west", "warped_stairs")
    for (x, y, z) in ((7, V + 7, 5), (19, V + 7, 12), (8, V, 14), (18, V + 7, 5), (12, V + 7, 15)):
        k.set(x, y, z, "cobweb")
    for x in range(8, 13):
        k.set(x, V, 14, "polished_andesite")
    k.set(8, V + 1, 14, "cauldron")
    k.sign(10, V + 1, 14, "north", ["Menu", "Vet", "Vetballetjes", "Vet (koud)"], wall=False)
    k.sign(12, V + 1, 14, "north", ["Vetshake:", "uitverkocht", "(nooit", "ingekocht)"], wall=False)
    for (x, z) in ((ex, 4), (8, 9), (18, 9), (ex, 11), (9, 15), (17, 14), (6, 12), (13, 15)):
        for y in range(V + 5, V + 8):
            k.set(x, y, z, "chain", {"axis": "y", "waterlogged": "false"})
        k.lantaarn(x, V + 4, z, True)
    k.zone("vetemmer", ex - 9, V, ez - 9, ex + 9, V + 8, ez + 9, wie="De Vetemmer: het restaurant, zonder gasten")
    # --- the lab behind it: a box of deepslate and iron ---
    x0, x1, z0, z1 = 1, 25, 18, 30
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            k.set(x, G, z, "polished_deepslate" if (x + z) % 2 else "deepslate_tiles")
            k.set(x, V + 4, z, "polished_deepslate")
            if x in (x0, x1) or z in (z0, z1):
                for y in range(V, V + 4):
                    k.set(x, y, z, "iron_block" if y == V + 2 and (x + z) % 3 == 0 else "polished_deepslate")
    # the way in from the restaurant: through the back of the bucket
    for z in (16, 17, 18):
        k.hol(ex, V, z, ex, V + 1, z)
        k.set(ex, G, z, "deepslate_tiles")
        for dx in (-1, 1):
            for y in range(V, V + 3):
                if k.naam(ex + dx, y, z) is None:
                    k.set(ex + dx, y, z, "polished_deepslate")
        k.set(ex, V + 2, z, "polished_deepslate")
    k.deur(ex, V, 17, "north", "iron")
    k.set(ex + 1, V + 1, 16, "stone_button", {"face": "wall", "facing": "north", "powered": "false"})
    k.set(ex - 1, V + 1, 19, "stone_button", {"face": "wall", "facing": "south", "powered": "false"})
    k.sign(ex - 1, V + 1, 15, "north", ["Geheim lab", "Verboden voor", "Krabguhs", "(en klanten)"])
    # the wall between the lab room (west) and the sneak route (east), a door in it
    for z in range(z0 + 1, z1):
        for y in range(V, V + 4):
            k.set(12, y, z, "polished_deepslate")
    k.deur(12, V, 20, "west", "iron")
    k.set(13, V + 1, 21, "stone_button", {"face": "wall", "facing": "east", "powered": "false"})
    k.set(11, V + 1, 19, "stone_button", {"face": "wall", "facing": "west", "powered": "false"})
    for x in (9, 17):                                                          # (light in the nooks between the bucket and the lab)
        k.set(x, G, 17, KORAAL)
    k.prop("rustpunt_2", ex + 1, V, 19, wie="voor de sluiproute van De Vetemmer")
    # --- the lab room: Karen, the cabinet with the plans, Plankmika's little things ---
    k.set(3, V, 24, "guhs:karen", {"facing": "east", "gezicht": "0"})
    k.prop("karen", 3, V, 24, wie="Karen, Plankmika's computervrouw")
    k.plek("plankmika", 7, V, 24, yaw=90.0, wie="Plankmika", wezen="guhs:plankmika")
    k.plek("knipoog_kloon_professor", 5, V, 25, yaw=180.0, wie="Professor Knabbelkloon: als u het recept kloont, heeft u er twee", kind="KNABBELKLOON")
    for x in range(4, 10):
        k.sier(x, V, 19, ARCHIEF, "south")
    for x, tekst in ((4, ["Plan A", "t/m F:", "mislukt", ""]), (6, ["Plan G", "t/m Y:", "ook mislukt", ""]), (9, ["Plan Z", "", "(nog", "niet geprobeerd)"])):
        k.sign(x, V + 1, 19, "south", tekst, wall=False)
    k.prop("plan_z_kast", 9, V, 19, wie="de archiefkast: de la van Plan Z")
    k.plek("plan_z_plankmika", 9, V, 20, yaw=180.0, wie="hier staat Plankmika als hij Plan Z vindt", wezen="guhs:plankmika")
    for x in range(3, 11):                                                     # the workbench along the south wall
        k.plaat(x, V, 29, "smooth_stone_slab", "top")
    for x, ding in ((3, "brewing_stand"), (5, "flower_pot"), (7, "lightning_rod"), (9, "end_rod")):
        props = {"facing": "up", "powered": "false", "waterlogged": "false"} if ding == "lightning_rod" else {"facing": "up"} if ding == "end_rod" else \
            {"has_bottle_0": "true", "has_bottle_1": "false", "has_bottle_2": "true"} if ding == "brewing_stand" else None
        k.set(x, V + 1, 29, ding, props)
    k.set(2, V, 27, "observer", {"facing": "east", "powered": "false"})
    k.set(2, V + 1, 27, "redstone_lamp", {"lit": "false"})
    k.set(2, V, 21, "smooth_stone_slab", gb.slab("bottom"))                    # Plankmika's bed: he is very small
    k.tapijt(2, V, 22, "green")
    k.sign(2, V + 1, 21, "east", ["Bed van", "Plankmika", "(maat: kruimel)", ""])
    k.kist(10, V, 27, "west", "lab")
    for (x, z) in ((4, 22), (9, 23), (6, 27)):
        k.set(x, V + 4, z, "sea_lantern")
    k.camera("knipoog_kloon", (9, V + 2, 27), (5, V + 1, 24), zicht=["knipoog_kloon_professor", "plankmika", "karen"])
    # --- the sneak route: three lanes behind each other, a laser across each, cameras on the ceiling, the recipe at the end ---
    for x in range(13, 22):
        for y in range(V, V + 4):
            k.set(x, y, 22, "polished_deepslate")
    for x in range(16, x1):
        for y in range(V, V + 4):
            k.set(x, y, 26, "polished_deepslate")
    k.prop("sluip_start", 14, V, 20, wie="hier begint de sluiproute")
    k.pad("sluip_route", [(14, V, 20), (23, V, 20), (23, V, 24), (14, V, 24), (14, V, 28), (22, V, 28)], wie="langs camera's en lasers naar het recept")
    lasers = [(18, 19, 18, 21), (22, 22, 24, 22), (17, 23, 17, 25), (19, 27, 19, 29)]
    for i, (ax, az, bx, bz) in enumerate(lasers, 1):
        k.zone(f"laser_{i}", ax, V, az, bx, V + 2, bz, wie="een laserstraal dwars over de gang")
        # (where the beam leaves the wall and where it lands: a red eye in the wall on both sides)
        einden = ((ax, az - 1), (bx, bz + 1)) if ax == bx else ((ax - 1, az), (bx + 1, bz))
        for (x, z) in einden:
            k.set(x, V + 1, z, "red_stained_glass")
    for i, (x, z, kant) in enumerate(((24, 19, "west"), (13, 25, "east"), (24, 27, "west"), (20, 23, "west")), 1):
        k.sier(x, V + 3, z, CAMERA, kant)
        k.prop(f"camera_{i}", x, V + 3, z, wie="een bewakingscamera aan het plafond", los=True)
    for (x, z) in ((16, 20), (22, 20), (20, 24), (14, 24), (15, 28), (21, 28)):
        k.set(x, V + 4, z, "redstone_lamp", {"lit": "false"})
        k.lantaarn(x, V + 3, z, True)
    k.set(24, V, 28, "polished_deepslate")
    k.sier(24, V + 1, 28, KLUIS, "west")
    k.prop("receptkluis", 24, V + 1, 28, wie="hier ligt het gestolen recept (achter de lasers)", los=True)
    k.sign(23, V + 2, 29, "north", ["Recept van", "een ander.", "Eerlijk", "gevonden."])
    k.sign(13, V + 2, 19, "east", ["Bewaking:", "Karen kijkt.", "Altijd.", "Zucht."])
    k.zone("lab", x0, V, z0, x1, V + 3, z1, wie="het lab van Plankmika, met de sluiproute")
    # the roof: an antenna and a dish
    for y in range(V + 5, V + 9):
        k.set(4, y, 26, "iron_bars")
    k.set(4, V + 9, 26, "lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    for (x, z) in schijf(20, 25, 2.2):
        k.set(x, V + 5, z, "smooth_stone_slab", gb.slab("bottom"))
    k.set(20, V + 5, 25, "iron_block")
    k.set(20, V + 6, 25, "end_rod", {"facing": "up"})


# =====================================================================================================================
# the Guhfy Guhber ice parlour
# =====================================================================================================================
def ijssalon(b):
    k = Hoek(b, 66, 84)
    BX, BZ = 20, 22
    k.bouwgrond(-1, -1, BX + 1, BZ + 1)
    for x in range(BX + 1):
        for z in range(BZ + 1):
            k.set(x, G, z, "pink_concrete" if (x + z) % 2 else "white_concrete")
            k.set(x, V + 5, z, "white_concrete")
            if x in (0, BX) or z in (0, BZ):
                for y in range(V, V + 5):
                    k.set(x, y, z, "pink_concrete" if ((x + z) // 2) % 2 else "white_concrete")
    for (x, z) in ((0, 0), (BX, 0), (0, BZ), (BX, BZ)):
        k.set(x, V + 6, z, "pink_concrete")
    for x in range(BX + 1):                      # a scalloped rim, like a paper cup
        for z in (0, BZ):
            if x % 2 == 0:
                k.plaat(x, V + 6, z, "smooth_quartz_slab")
    for z in range(BZ + 1):
        for x in (0, BX):
            if z % 2 == 0:
                k.plaat(x, V + 6, z, "smooth_quartz_slab")
    # the front
    for x in list(range(2, 9)) + list(range(12, 19)):
        for y in range(V + 1, V + 4):
            k.set(x, y, BZ, RUIT)
    k.deur(10, V, BZ, "south", "birch")
    k.sign(10, V + 2, BZ + 1, "south", ["Guhfy Guhber", "IJssalon", "", ""])
    k.sign(11, V + 1, BZ + 1, "south", ["Elke coupe", "met poppetje", "(bijna altijd)", ""])
    for z in (5, 11, 17):
        for y in (V + 1, V + 2, V + 3):
            k.set(0, y, z, RUIT)
            k.set(BX, y, z, RUIT)
    # the sundae on the roof: a glass coupe, three scoops, a cherry and a spoon
    cx, cz = 10, 11
    for (x, z) in schijf(cx, cz, 2.3):
        k.set(x, V + 6, z, "smooth_quartz")
    for y in range(V + 7, V + 9):
        k.set(cx, y, cz, "white_stained_glass")
    for dy, rr in ((9, 2.3), (10, 3.3), (11, 4.3), (12, 4.6)):
        for (x, z) in schijf(cx, cz, rr, rr - 1.3 if dy > 9 else -1):
            k.set(x, V + dy, z, "white_stained_glass")
    for (x, z) in schijf(cx, cz, 3.2):
        k.set(x, V + 11, z, "pink_wool")
        k.set(x, V + 12, z, "pink_wool")
    k.bol(cx - 2, V + 14, cz, 2.6, 2.6, 2.6, "pink_wool")
    k.bol(cx + 2, V + 14, cz - 1, 2.6, 2.6, 2.6, "brown_wool")
    k.bol(cx, V + 14.5, cz + 2, 2.6, 2.6, 2.6, "white_wool")
    k.bol(cx, V + 17, cz, 2.2, 2.0, 2.2, "lime_wool")
    k.set(cx, V + 19, cz, "red_concrete")
    k.set(cx, V + 20, cz, "red_concrete")
    k.set(cx, V + 21, cz, "green_wool")
    k.set(cx + 1, V + 21, cz, "green_wool")
    for (dx, dy) in ((4, 12), (4, 13), (5, 13), (5, 14), (5, 15), (6, 15), (6, 16), (6, 17)):      # the spoon, leaning in the coupe
        k.set(cx + dx, V + dy, cz + 3, "iron_block")
    for (x, z) in ((2, 2), (BX - 2, 2), (2, BZ - 2), (BX - 2, BZ - 2)):
        k.set(x, V + 6, z, KORAAL)
    # --- inside: the bar along the back, tables, the little stage ---
    for x in range(4, 17):
        k.set(x, V, 4, "smooth_quartz")
    k.prop("ijs_toonbank", 10, V, 4, wie="de toonbank van de ijssalon")
    for x, smaak in ((5, "pink_wool"), (7, "brown_wool"), (9, "white_wool"), (11, "lime_wool"), (13, "yellow_wool"), (15, "magenta_wool")):
        k.set(x, V, 1, "smooth_quartz")
        k.set(x, V + 1, 1, smaak)
    k.sign(6, V + 2, 1, "south", ["Smaken:", "knabbel, kaas,", "knabbelkaas,", "verrassing"])
    k.sign(14, V + 2, 1, "south", ["Verrassing", "is ook", "knabbel.", "Njeg!"])
    k.plek("ijsverkoper", 10, V, 2, yaw=0.0, wie="de ijsverkoper", wezen=BEWONER)
    for x in (7, 9, 11, 13):                     # the stools at the bar: round cushions
        k.set(x, V, 6, "pink_wool" if x != 13 else "light_blue_wool")
    k.plek("ijs_sponsguh", 9, V + 1, 6, yaw=180.0, wie="Sponsguh op zijn kruk aan de bar", wezen="guhs:sponsguh")
    k.plek("ijs_patguh", 11, V + 1, 6, yaw=180.0, wie="Patguh naast hem", wezen="guhs:patguh")
    k.prop("ijs_speler", 13, V, 6, wie="jouw kruk")
    for (x, z) in ((4, 10), (4, 18), (9, 18)):
        k.set(x, V, z, "smooth_quartz")
        k.set(x - 1, V, z, "pink_wool")
        k.set(x + 1, V, z, "light_blue_wool")
    # wink 8: Baltoguh behind a towering sundae, the medicine chest next to him
    k.set(5, V, 14, "smooth_quartz")
    k.set(4, V, 14, "light_blue_wool")
    k.plek("knipoog_ijs_balto", 4, V + 1, 14, yaw=-90.0, wie="Baltoguh (de guh-variant BALTOGUH) achter een torenhoge ijscoupe", wezen="guhs:guh")
    k.prop("knipoog_ijs_coupe", 5, V, 14, wie="het tafeltje voor hem: hier komt de torenhoge ijscoupe (3 vrij erboven)")
    k.prop("knipoog_ijs_kistje", 4, V, 15, wie="op de vloer naast zijn kruk: het medicijnkistje dat hij niet ziet")
    k.camera("knipoog_ijs", (11, V + 2, 18), (5, V + 2, 14), zicht=["knipoog_ijs_balto", "knipoog_ijs_kistje"])
    for x in range(13, 20):                      # the stage where the Guhfy Guhber song is sung
        for z in range(11, 20):
            k.set(x, V, z, "spruce_planks")
    for z in range(11, 20):
        for y in range(V + 1, V + 5):
            if z in (11, 19) or y == V + 4:
                k.set(19, y, z, "red_wool")
    k.set(18, V + 1, 12, "note_block", {"instrument": "harp", "note": "0", "powered": "false"})
    k.set(18, V + 1, 18, "jukebox", {"has_record": "false"})
    k.sign(12, V, 15, "west", ["Vanavond:", "Ik ben een", "Guhfy Guhber", "(rock!)"], wall=False)
    for (x, z) in ((4, 8), (10, 9), (16, 8), (4, 16), (10, 16), (16, 15), (10, 20), (5, 2), (15, 2), (3, 20), (17, 20)):
        k.lantaarn(x, V + 4, z, True, "guhs:lampion_roze")
    k.zone("ijssalon", 0, V, 0, BX, V + 4, BZ, wie="de Guhfy Guhber-ijssalon")


# =====================================================================================================================
# juf Pufguh's boating school (inside the loop of the course)
# =====================================================================================================================
def bootschool(b):
    k = Hoek(b, 31, 125)
    BX, BZ = 14, 9
    k.bouwgrond(-1, -1, BX + 3, BZ + 1)
    for x in range(BX + 1):
        for z in range(BZ + 1):
            k.set(x, G, z, "birch_planks")
            k.set(x, V + 4, z, "red_concrete" if x in (0, BX) or z in (0, BZ) else "white_concrete")
            if x in (0, BX) or z in (0, BZ):
                for y in range(V, V + 4):
                    k.set(x, y, z, "red_concrete" if y == V else "white_concrete")
    for x in (3, 4, 5, 9, 10, 11):
        for y in (V + 1, V + 2):
            k.set(x, y, BZ, RUIT)
            k.set(x, y, 0, RUIT)
    k.deur(BX, V, 5, "east", "birch")
    k.sign(BX + 1, V + 2, 5, "east", ["Vaarschool", "juf Pufguh", "", ""])
    k.sign(BX + 1, V + 1, 4, "east", ["Geslaagd", "deze eeuw: 0", "Gezakt:", "Sponsguh (ja)"])
    # the lighthouse on the north-west corner
    tx, tz = 3, 3
    for y in range(V + 5, V + 15):
        for (x, z) in schijf(tx, tz, 2.6, 1.5):
            k.set(x, y, z, "red_concrete" if ((y - V) // 2) % 2 else "white_concrete")
    for (x, z) in schijf(tx, tz, 3.4):
        k.set(x, V + 15, z, "smooth_stone")
    for (x, z) in schijf(tx, tz, 2.6, 1.5):
        k.set(x, V + 16, z, GLAS)
        k.set(x, V + 17, z, GLAS)
    k.set(tx, V + 16, tz, "sea_lantern")
    k.set(tx, V + 17, tz, "sea_lantern")
    for dy, rr in ((18, 3.0), (19, 2.0), (20, 1.0)):
        for (x, z) in schijf(tx, tz, rr):
            k.set(x, V + dy, z, "red_concrete")
    k.set(tx, V + 21, tz, "lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    # the classroom: desks, the teacher's desk, the blackboard
    for z in (2, 4, 6):
        for x in (6, 8, 10):
            k.plaat(x, V, z, "birch_slab", "top")
            if z != 6:
                k.stoel(x, V, z + 1, "north", "birch_stairs")
    for x in (4, 5):
        k.plaat(x, V, 7, "spruce_slab", "top")
    for z in range(2, 7):
        for y in (V + 1, V + 2):
            k.set(1, y, z, "black_concrete")
    k.hol(1, V + 1, 2, 1, V + 2, 6)
    for z in range(2, 7):
        for y in (V + 1, V + 2):
            k.set(0, y, z, "black_concrete")
    k.sign(1, V + 2, 3, "east", ["Regel 1:", "niet botsen", "Regel 2:", "zie regel 1"])
    k.sign(1, V + 2, 5, "east", ["Wat doe je", "bij paniek?", "Juist: NIET", "gillen, njeg"])
    k.sign(1, V + 1, 4, "east", ["Examen:", "1 rondje,", "0 pylonen om,", "1 brug over"])
    k.kist(13, V, 1, "west", "huis")
    k.set(12, V, 8, "flower_pot")
    for (x, z) in ((4, 4), (10, 4)):
        k.lantaarn(x, V + 3, z)
    k.zone("bootschool", 0, V, 0, BX, V + 4, BZ, wie="de vaarschool van juf Pufguh")
    b.plek("pufguh", 46, V, 122, yaw=-90.0, wie="juf Pufguh, bij de startstreep", wezen="guhs:juf_pufguh")
    # wink 5 (the camera stands between the bridge and the start line)
    b.camera("knipoog_guhdalf", (38, V + 5, 119), (23, V + 3, 112), zicht=["knipoog_guhdalf_brug", "pufguh"])
    # a path from the door to the start line
    for x in range(BX + 1, BX + 3):
        for z in range(3, 8):
            k.set(x, G, z, KSTEEN)
    for z in range(122, 128):
        for x in (46, 47):
            b.set(x, G, z, KSTEEN)
            b.bebouwd.add((x, z))


# =====================================================================================================================
# De Bellenkom
# =====================================================================================================================
BK = (106, 53, 26.0, 15.0)                       # the stadium: its middle and its two radii


def bellenkom(b):
    mx, mz, ra, rb = BK
    b.bouwgrond(int(mx - ra) - 1, int(mz - rb) - 1, int(mx + ra) + 1, int(mz + rb) + 2)

    def rho(x, z):
        return math.hypot((x - mx) / ra, (z - mz) / rb)

    def rang(x, z):                              # 0 = the field; 1..3 = the rows of the stands; 5 = the outer wall
        r = rho(x, z)
        return None if r > 1.0 else 0 if r < 0.55 else 1 if r < 0.66 else 2 if r < 0.77 else 3 if r < 0.88 else 5

    for x in range(int(mx - ra), int(mx + ra) + 1):
        for z in range(int(mz - rb), int(mz + rb) + 1):
            t = rang(x, z)
            if t is None:
                continue
            ingang = abs(x - mx) <= 3 and z > mz           # the way in from the Bellenlaan: a cut through the south stand
            if t == 0 or ingang:
                lijn = (x - int(mx)) % 5 == 0 and t == 0
                lamp = lijn and (z - int(mz)) % 4 == 0
                b.set(x, G, z, LAMP if lamp else "white_concrete" if lijn else "lime_concrete" if t == 0 else STEEN)
                continue
            b.set(x, G, z, KSTEEN)
            for y in range(V, V + t):
                top = y == V + t - 1
                b.set(x, y, z, (TEGEL if t < 5 else "light_blue_concrete") if top else KSTEEN)
            if t == 5 and (x + z) % 4 == 0:
                b.set(x, V + 4, z, LAMP)
            if t == 1 and (x + z) % 5 == 0:
                b.set(x, V, z, LAMP)
    for z in range(int(mz), int(mz + rb) + 1):     # the walls of the cut
        for x in (int(mx) - 4, int(mx) + 4):
            t = rang(x, z)
            if t:
                b.set(x, V + t, z, STEEN_MUUR)
    for (x, z) in ((int(mx) - 4, int(mz + rb) + 1), (int(mx) + 4, int(mz + rb) + 1)):
        for y in range(V, V + 4):
            b.set(x, y, z, STEEN_MUUR)
        b.set(x, V + 4, z, KORAAL)
    b.sign(int(mx) - 4, V + 2, int(mz + rb) + 2, "south", ["De Bellenkom", "Vanavond:", "Octo en zijn", "fanfare. Njeg"])
    b.sign(int(mx) + 4, V + 2, int(mz + rb) + 2, "south", ["Kaartjes:", "uitverkocht", "(aan Octo's", "moeder)"])
    b.zone("bellenkom", int(mx - ra), V, int(mz - rb), int(mx + ra), V + 14, int(mz + rb), wie="De Bellenkom, het stadion")
    # --- the stage at the north end: two high, steps at both sides, a shell behind it ---
    sx0, sx1, sz0, sz1 = 97, 115, 40, 46
    for x in range(sx0, sx1 + 1):
        for z in range(sz0, sz1 + 1):
            b.set(x, G, z, KSTEEN)
            b.set(x, V, z, "spruce_planks")
            b.set(x, V + 1, z, "sea_lantern" if (x - sx0) % 6 == 0 and z in (sz0 + 1, sz1 - 1) else "spruce_planks")
            for y in range(V + 2, V + 9):
                b.s.blocks.pop((x, y, z), None)
    for x in (sx0 - 1, sx1 + 1):
        for z in range(sz0 + 2, sz1 + 2):
            b.set(x, G, z, KSTEEN)
            b.set(x, V, z, "spruce_planks")
            for y in range(V + 1, V + 6):
                b.s.blocks.pop((x, y, z), None)
    for x in (sx0 - 2, sx1 + 2):
        for z in range(sz0 + 3, sz1 + 2):
            for y in range(V, V + 5):
                b.s.blocks.pop((x, y, z), None)
    for x in range(sx0, sx1 + 1):                  # the front edge drops to the field
        for y in range(V, V + 4):
            for z in (sz1 + 1, sz1 + 2):
                if rang(x, z) == 0:
                    b.s.blocks.pop((x, y, z), None)
    for x in range(sx0 - 3, sx1 + 4):              # the shell: a fan of parelmoer ribs behind the stage
        hoog = int(round(11.0 * math.sqrt(max(0.0, 1.0 - ((x - mx) / 12.5) ** 2))))
        for y in range(V, V + 2 + hoog):
            rib = (x - int(mx)) % 4 == 0
            b.set(x, y, sz0 - 1, "pink_concrete" if rib else PAREL)
            if y >= V + 2 and hoog > 3 and y < V + 2 + hoog - 2:
                b.set(x, y, sz0 - 2, PAREL if rib else TEGEL)
    b.zone("podium", sx0, V + 2, sz0, sx1, V + 8, sz1, wie="het podium van de Bellenkom")
    b.plek("podium_octo", 106, V + 2, 43, yaw=0.0, wie="Octo Tentakel, de dirigent", wezen="guhs:octo_mika")
    b.plek("podium_sponsguh", 102, V + 2, 45, yaw=0.0, wie="Sponsguh zingt", wezen="guhs:sponsguh")
    b.set(110, V + 2, 45, "note_block", {"instrument": "basedrum", "note": "0", "powered": "false"})
    b.prop("podium_trommel", 110, V + 2, 45, wie="jouw trommel")
    for i, (x, z) in enumerate(((99, 42), (101, 43), (104, 41), (108, 41), (111, 43), (113, 42)), 1):
        b.plek(f"band_{i}", x, V + 2, z, yaw=0.0, wie="de fanfare: een Bikinibodemguh met een instrument", wezen=BEWONER)
    for i, x in enumerate((98, 103, 109, 114), 1):
        b.set(x, V + 2, sz1, "cauldron")
        b.prop(f"rook_{i}", x, V + 2, sz1, wie="rookmachine aan de rand van het podium")
    for x in (sx0 - 1, sx1 + 1):                   # the light rig over the stage
        for y in range(V + 1, V + 10):
            b.set(x, y, sz0 + 4, "iron_bars")
    for x in range(sx0 - 1, sx1 + 2):
        b.set(x, V + 10, sz0 + 4, "iron_block")
    for i, x in enumerate((99, 104, 108, 113), 1):
        b.set(x, V + 9, sz0 + 4, "pearlescent_froglight", log())
        b.prop(f"licht_{i}", x, V + 9, sz0 + 4, wie="een schijnwerper boven het podium", los=True)
    for x in (sx0, sx1):                           # speakers
        b.set(x, V + 2, sz0, "black_wool")
        b.set(x, V + 3, sz0, "black_wool")
        b.set(x, V + 4, sz0, "note_block", {"instrument": "harp", "note": "0", "powered": "false"})
    # the stands: Octoliam sits in the east stand
    ox, oz = 125, 53
    oy = max(y for y in range(G, V + 6) if b.naam(ox, y, oz)) + 1
    b.plek("octoliam", ox, oy, oz, yaw=90.0, wie="Octoliam, op de tribune", wezen="guhs:octoliam")
    b.zone("tribune", 121, V, 44, 131, V + 8, 62, wie="de oosttribune")
    for (x, z) in ((84, 53), (128, 53), (92, 44), (120, 44), (92, 62), (120, 62)):
        y = max(yy for yy in range(G, V + 6) if b.naam(x, yy, z))
        if b.naam(x, y, z) != LAMP:
            b.set(x, y, z, LAMP)


# =====================================================================================================================
# the Schelpstraat: Sponsguh's fruit, Octo's moai, Patguh's rock
# =====================================================================================================================
def sponsguh_huis(b):
    cx, cz = 82, 167
    WAND, DAK, RAAM, DEUR = "guhs:vruchthuis_wand", "guhs:vruchthuis_dak", "guhs:vruchthuis_raam", "guhs:vruchthuis_deur"
    b.bouwgrond(cx - 8, cz - 10, cx + 8, cz + 8)
    my, rx, ry = V + 4, 6.5, 9.0
    for (x, z) in schijf(cx, cz, 6):
        b.set(x, G, z, "orange_terracotta")
    b.bol(cx, my, cz, rx, ry, rx, WAND, hol=True, onder=V)
    for (x, z) in schijf(cx, cz, 5.6):             # the upper floor
        if b.naam(x, V + 5, z) is None:
            b.set(x, V + 5, z, "birch_planks")
    # the crown of leaves
    top = max(y for y in range(V, V + 20) if b.naam(cx, y, cz))
    for dy in range(1, 6):
        b.set(cx, top + dy, cz, DAK)
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            ver = max(abs(dx), abs(dz))
            if ver <= 1 or dx == 0 or dz == 0:
                b.set(cx + dx, top + 2, cz + dz, DAK)
            if ver == 1 and (dx == 0 or dz == 0):
                b.set(cx + dx, top + 1, cz + dz, DAK)
            if ver == 3 and (dx == 0 or dz == 0) or ver == 1 and dx and dz:
                b.set(cx + dx, top + 3, cz + dz, DAK)
    # the door (north, on the street) in a little porch, the round windows
    for y in (V, V + 1, V + 2):
        for x in (cx - 1, cx, cx + 1):
            b.set(x, y, cz - 6, WAND)
        if y < V + 2:
            b.hol(cx, y, cz - 5, cx, y, cz - 4)
    b.hol(cx, V, cz - 6, cx, V + 1, cz - 6)
    b.deur(cx, V, cz - 6, "north", DEUR)
    b.set(cx, G, cz - 6, "orange_terracotta")
    b.set(cx, G, cz - 5, "orange_terracotta")

    def voorste(x, y, stap=1):
        zs = [z for z in range(cz - 8, cz + 9) if b.naam(x, y, z) == WAND]
        return (min(zs) if stap == 1 else max(zs)) if zs else None

    for (x, y) in ((cx - 3, V + 2), (cx + 3, V + 2), (cx - 2, V + 8), (cx + 2, V + 8)):
        z = voorste(x, y)
        if z is not None:
            b.set(x, y, z, RAAM)
    for (x, y) in ((cx, V + 3), (cx, V + 8)):
        z = voorste(x, y, -1)
        if z is not None:
            b.set(x, y, z, RAAM)
    for i, z in enumerate(range(cz - 9, cz - 6)):    # the garden path, flowers
        b.set(cx, G, z, TEGEL)
    for (x, z) in ((cx - 3, cz - 8), (cx + 3, cz - 8)):
        b.set(x, V, z, "moss_block")
        b.set(x, V + 1, z, "pink_tulip" if x < cx else "allium")
    b.sign(cx + 1, V, cz - 8, "north", ["Sponsguh", "Kaasbroek", "Ik ben er", "klaar voor!"], wall=False)
    b.plek("sponsguh", cx, V, cz - 8, yaw=180.0, wie="Sponsguh Kaasbroek, bij zijn deur", wezen="guhs:sponsguh")
    # downstairs: the chair, the shell phone, Gerrit's bowl
    b.stoel(cx - 3, V, cz + 1, "east", "red_nether_brick_stairs")
    b.set(cx - 3, V, cz + 2, "red_wool")
    b.tafel(cx + 3, V, cz - 2, "birch")
    b.set(cx + 3, V, cz + 2, "cauldron")
    b.sign(cx + 3, V + 1, cz + 2, "west", ["Gerrits bakje", "(njeg = leeg)", "", ""], wall=False)
    b.plek("gerrit", cx + 2, V, cz + 1, yaw=90.0, wie="slak Gerrit", wezen="guhs:gerrit")
    b.set(cx - 2, V, cz + 4, "bookshelf")
    b.set(cx - 2, V + 1, cz + 4, "bookshelf")
    b.set(cx + 1, V, cz + 4, "stripped_birch_log", log())
    for y in range(V + 1, V + 5):
        b.set(cx + 1, y, cz + 4, "stripped_birch_log", log())
    b.hol(cx, V + 5, cz + 4, cx, V + 5, cz + 4)
    b.ladder(cx, V, V + 5, cz + 4, "west")
    b.lantaarn(cx, V + 4, cz, True)
    b.kist(cx - 4, V, cz - 1, "east", "huis")
    # upstairs: the bed, the foghorn alarm clock
    b.bed(cx - 2, V + 6, cz - 2, "west", "yellow")
    b.prop("sponsguh_bed", cx - 2, V + 6, cz - 2, wie="het bed van Sponsguh")
    b.set(cx - 2, V + 6, cz, "note_block", {"instrument": "harp", "note": "0", "powered": "false"})
    b.sign(cx - 2, V + 7, cz, "east", ["Wekker", "TOEEET", "(elke ochtend,", "heel vroeg)"], wall=False)
    b.set(cx + 3, V + 6, cz - 1, "bookshelf")
    b.tapijt(cx, V + 6, cz, "light_blue")
    b.tapijt(cx + 1, V + 6, cz, "light_blue")
    b.set(cx, V + 6, cz + 2, "birch_fence")
    b.set(cx, V + 7, cz + 2, "lantern", gb.lamp(False))
    b.zone("sponsguh_huis", cx - 7, V, cz - 7, cx + 7, V + 14, cz + 7, wie="het knabbelkoorvruchthuis van Sponsguh")


PORTRETTEN = 0


def portret(k, x, y, z, facing):
    """One more self-portrait of Octo Tentakel on a wall (four different ones, in turn)."""
    global PORTRETTEN
    k.sier(x, y, z, PORTRET, facing, soort=str(PORTRETTEN % 4))
    PORTRETTEN += 1


def octo_huis(b):
    global PORTRETTEN
    PORTRETTEN = 0
    k = Hoek(b, 100, 159)
    BX, BZ, TOP = 12, 10, V + 15
    k.bouwgrond(-1, -2, BX + 1, BZ + 1)
    HUID, DONKER = "cyan_terracotta", "gray_concrete"
    # the head: a tall block, a little narrower at the top
    for y in range(V, TOP + 1):
        smal = 1 if y >= V + 12 else 0
        for x in range(1 + smal, BX - smal):
            for z in range(1, BZ):
                rand = x in (1 + smal, BX - 1 - smal) or z in (1, BZ - 1)
                if rand or y == TOP:
                    k.set(x, y, z, HUID)
    for x in range(1, BX):
        for z in range(1, BZ):
            k.set(x, G, z, "polished_blackstone" if (x + z) % 2 else "gray_concrete")
            if 1 < x < BX - 1 and 1 < z < BZ - 1 and (x, z) != (9, 8):
                k.set(x, V + 5, z, "dark_oak_planks")
                k.set(x, V + 10, z, "dark_oak_planks")
    # the face on the street side: the brow, the eyes, the long nose, the mouth is the door
    for x in range(2, BX - 1):
        k.set(x, V + 12, 0, DONKER)
    for x in (2, 3, 4, 8, 9, 10):
        k.set(x, V + 11, 0, DONKER)
    for x in (3, 4, 8, 9):
        for y in (V + 9, V + 10):
            k.set(x, y, 1, "gray_stained_glass")
    for y in range(V + 4, V + 11):
        k.set(6, y, 0, HUID)
    for y in (V + 4, V + 5):
        k.set(6, y, -1, HUID)
        k.set(5, y, 0, HUID)
        k.set(7, y, 0, HUID)
    for x in range(4, 9):
        k.set(x, V + 3, 0, DONKER)
    k.deur(6, V, 1, "north", "dark_oak")
    for z in (4, 5, 6):                          # the ears
        for y in range(V + 5, V + 11):
            k.set(0, y, z, HUID)
            k.set(BX, y, z, HUID)
    k.sign(7, V + 1, 0, "north", ["O. Tentakel", "Niet storen.", "Ik oefen.", "Ja, nu ook."])
    b.plek("bewoner_10", 108, V, 157, yaw=180.0, wie="een buurguh die naar de klarinet luistert (met oren dicht)", wezen=BEWONER)
    # the ladder up the back wall, through both floors
    k.ladder(9, V, V + 11, 8, "north")
    # ground floor: the music room
    k.sier(9, V, 4, KLARINET, "west")
    k.prop("klarinet_standaard", 9, V, 4, wie="de klarinet op zijn standaard")
    k.plek("octo", 6, V, 5, yaw=180.0, wie="Octo Tentakel, thuis", wezen="guhs:octo_mika")
    k.stoel(3, V, 7, "north", "dark_oak_stairs")
    k.set(2, V, 7, "bookshelf")
    k.set(2, V + 1, 7, "bookshelf")
    k.set(7, V, 7, "jukebox", {"has_record": "false"})
    k.sign(7, V + 1, 7, "north", ["Mijn platen:", "ik, solo", "ik, live", "ik, nog eens"], wall=False)
    for (x, y, z, f) in ((2, V + 2, 3, "east"), (2, V + 2, 5, "east"), (10, V + 2, 3, "west"), (10, V + 2, 6, "west"), (4, V + 2, 8, "north"),
                         (7, V + 2, 8, "north"), (4, V + 2, 2, "south"), (8, V + 2, 2, "south")):
        portret(k, x, y, z, f)
    k.lantaarn(6, V + 4, 6, True)
    k.lantaarn(4, V + 4, 3, True)
    k.lantaarn(9, V + 4, 3, True)
    k.tapijt(5, V, 4, "purple")
    k.tapijt(6, V, 4, "purple")
    k.tapijt(7, V, 4, "purple")
    # first floor: the gallery of himself, a bust on a pedestal, the bath
    for (x, y, z, f) in ((2, V + 7, 3, "east"), (2, V + 8, 5, "east"), (2, V + 7, 7, "east"), (10, V + 7, 3, "west"), (10, V + 8, 5, "west"),
                         (5, V + 7, 8, "north"), (7, V + 8, 8, "north"), (5, V + 8, 2, "south"), (7, V + 7, 2, "south")):
        portret(k, x, y, z, f)
    k.set(6, V + 6, 5, "polished_blackstone")
    k.set(6, V + 7, 5, HUID)
    k.set(6, V + 8, 5, HUID)
    k.sign(6, V + 6, 4, "north", ["Ik", "(in klei)", "", ""])
    k.set(3, V + 6, 7, "cauldron")
    k.sign(3, V + 7, 7, "east", ["Bad", "(alleen ik)", "", ""], wall=False)
    k.lantaarn(6, V + 9, 3, True)
    k.lantaarn(6, V + 9, 7, True)
    # top floor: the bedroom
    k.bed(4, V + 11, 4, "north", "cyan")
    k.set(3, V + 11, 6, "barrel", {"facing": "up", "open": "false"})
    k.sign(3, V + 12, 6, "east", ["Klarinetkoffer", "nr. 14", "", ""], wall=False)
    for (x, y, z, f) in ((3, V + 12, 3, "east"), (9, V + 12, 4, "west"), (5, V + 12, 8, "north"), (7, V + 12, 8, "north"), (8, V + 12, 2, "south")):
        portret(k, x, y, z, f)
    k.lantaarn(6, V + 14, 5, True)
    k.lantaarn(4, V + 14, 7, True)
    k.lantaarn(9, V + 14, 3, True)
    k.zone("octo_huis", 0, V, -1, BX, TOP, BZ, wie="het moai-huis van Octo Tentakel: vol zelfportretten en klarinetdingen")


def patguh_rots(b):
    cx, cz, r = 130, 167, 5.6
    b.bouwgrond(cx - 7, cz - 8, cx + 7, cz + 7)
    b.bol(cx, V - 1, cz, r, r, r, ROTS, hol=True, onder=V)
    for (x, z) in schijf(cx, cz, r - 1):
        b.set(x, G, z, "smooth_sandstone")
    # the way in (north): a low doorway in the rock
    for y in (V, V + 1):
        for z in range(cz - 6, cz - 3):
            b.hol(cx, y, z, cx, y, z)
    for x in (cx - 1, cx + 1):
        for y in (V, V + 1, V + 2):
            if b.naam(x, y, cz - 5) is None:
                b.set(x, y, cz - 5, ROTS)
    b.set(cx, V + 2, cz - 5, ROTS)
    b.set(cx, G, cz - 5, ROTS)
    top = max(y for y in range(V, V + 9) if b.naam(cx, y, cz))
    b.set(cx, top, cz, KORAAL)                   # (a glowing barnacle under the stick)
    b.set(cx, top + 1, cz, "spruce_fence")       # the stick on top
    b.set(cx, top + 2, cz, "spruce_fence")
    b.set(cx + 1, top + 2, cz, "spruce_fence")
    b.sign(cx - 1, V + 1, cz - 6, "north", ["Dit is geen", "kruimelrots.", "Dit is mijn", "huis. -Patguh"])
    b.plek("patguh", cx - 3, V, cz - 7, yaw=180.0, wie="Patguh Ster, naast zijn rots", wezen="guhs:patguh")
    # the room under the rock: down the ladder; furniture of sand
    x0, x1, z0, z1, vloer = cx - 4, cx + 4, cz - 4, cz + 4, 9
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            for y in range(vloer, V - 1):
                rand = x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1) or y == vloer
                if rand:
                    b.set(x, y, z, "smooth_sandstone" if y == vloer else "sandstone")
                else:
                    b.hol(x, y, z, x, y, z)
    b.set(cx, V - 1, cz + 3, "sandstone")
    b.hol(cx, vloer + 1, cz + 2, cx, G, cz + 2)
    b.set(cx, V, cz + 3, ROTS)
    b.ladder(cx, vloer + 1, V, cz + 2, "north")
    for (x, z) in ((x0, z0), (x0 + 1, z0), (x0 + 2, z0)):
        b.trap(x, vloer + 1, z, "north", "sandstone_stairs")
    b.set(x0 + 1, vloer + 1, z0 + 3, "sandstone_slab", gb.slab("bottom"))
    b.set(x0 + 1, vloer + 1, z0 + 5, "smooth_sandstone")
    b.sign(x0 + 1, vloer + 2, z0 + 5, "north", ["Tv van zand", "Zender 1:", "zand", ""], wall=False)
    b.bed(x1, vloer + 1, z0 + 1, "north", "pink")
    b.set(x1, vloer + 1, z1, "sand")
    b.set(x1 - 1, vloer + 1, z1, "sand")
    b.set(x1, vloer + 2, z1, "sand")
    b.sign(x1 - 2, vloer + 1, z1, "west", ["Kast", "(ook zand)", "", ""], wall=False)
    b.kist(x0, vloer + 1, z1, "east", "huis")
    for (x, z) in ((cx - 2, cz - 2), (cx + 2, cz + 1)):
        b.lantaarn(x, vloer + 5, z, True)
    b.set(cx - 2, G, cz + 1, KORAAL)
    b.set(cx + 2, G, cz - 1, KORAAL)
    b.zone("patguh_rots", cx - 6, vloer, cz - 6, cx + 6, V + 6, cz + 6, wie="de kruimelrots van Patguh, met de kamer eronder")


# =====================================================================================================================
# Sandy-guh's air dome
# =====================================================================================================================
def boomkoepel(b):
    cx, cz, r = 175, 134, 12.0
    b.bouwgrond(cx - 14, cz - 13, cx + 13, cz + 13)
    for (x, z) in schijf(cx, cz, r):
        b.set(x, G, z, "grass_block", {"snowy": "false"} if math.hypot(x - cx, z - cz) <= r - 1 else None)
        if math.hypot(x - cx, z - cz) > r - 1:
            b.set(x, G, z, "white_concrete")
    b.bol(cx, V - 0.5, cz, r, r, r, GLAS, hol=True, onder=V)
    for (x, z) in schijf(cx, cz, r, r - 1):        # ribs of white over the glass, a ring at its foot
        b.set(x, V, z, "white_concrete")
    for y in range(V, V + 13):
        for u in range(-13, 14):
            for c in ((cx + u, y, cz), (cx, y, cz + u)):
                if b.naam(*c) == GLAS:
                    b.set(*c, "white_concrete")
    # the airlock on the street side (west): two doors behind each other
    sx = cx - 12
    for x in range(sx - 2, sx + 1):
        for z in (cz - 1, cz, cz + 1):
            for y in range(V, V + 3):
                binnen = z == cz and y < V + 2
                if binnen:
                    b.hol(x, y, z, x, y, z)
                else:
                    b.set(x, y, z, "iron_block")
            b.set(x, G, z, "smooth_stone")
    b.deur(sx - 2, V, cz, "west", "birch")
    b.deur(sx, V, cz, "west", "birch")
    b.hol(sx + 1, V, cz, sx + 1, V + 1, cz)
    b.prop("koepel_sluis", sx - 2, V, cz, wie="de luchtsluis van de boomkoepel")
    b.sign(sx - 3, V + 2, cz, "west", ["Boomkoepel", "van Sandy-guh", "Lucht binnen!", "Deur dicht"])
    # the Guhmensie tree
    tx, tz = cx + 5, cz - 3
    for y in range(V, V + 5):
        b.set(tx, y, tz, "guhs:guhbloesem_log", log())
    for x in range(tx - 5, tx + 6):
        for z in range(tz - 5, tz + 6):
            for y in range(V + 3, V + 10):
                d = ((x - tx) / 4.4) ** 2 + ((y - (V + 5.5)) / 2.6) ** 2 + ((z - tz) / 4.4) ** 2
                binnen = (x - cx) ** 2 + (y - V) ** 2 + (z - cz) ** 2 < (r - 1.6) ** 2
                if d <= 1.0 and binnen and b.naam(x, y, z) is None:
                    # (persistent: a tile gets no neighbour updates, so a leaf keeps the distance of the template, and a
                    # leaf at distance 7 that is not persistent falls off at its first random tick)
                    b.set(x, y, z, "guhs:guhbloesem_leaves", {"distance": "7", "persistent": "true", "waterlogged": "false"})
    b.set(tx, V + 5, tz, "shroomlight")
    b.set(tx - 2, V + 4, tz + 1, "shroomlight")
    b.set(tx + 1, V + 4, tz - 2, "shroomlight")
    b.sign(tx - 1, V + 1, tz, "west", ["Echte boom", "uit de", "Guhmensie.", "Ik mis de lucht"])
    b.plek("sandy", cx - 3, V, cz - 1, yaw=90.0, wie="Sandy-guh", wezen="guhs:sandy_guh")
    # the karate mat (7 x 7, flat), the broken rocket (3 x 6 x 3: one side open, the nose is missing)
    mx0, mz0 = cx - 5, cz + 2
    for x in range(mx0, mx0 + 7):
        for z in range(mz0, mz0 + 7):
            rand = x in (mx0, mx0 + 6) or z in (mz0, mz0 + 6)
            b.set(x, G, z, "red_wool" if rand else "white_wool")
    b.zone("karate_mat", mx0, V, mz0, mx0 + 6, V + 3, mz0 + 6, wie="de karatemat")
    b.sign(mx0 + 7, V, mz0 + 1, "west", ["Karate!", "Zij duwt,", "jij bukt.", "Hai-ja, njeg"], wall=False)
    rx, rz = cx - 4, cz - 6
    for x in range(rx - 1, rx + 2):
        for z in range(rz - 1, rz + 2):
            b.set(x, G, z, "smooth_stone")
            if (x, z) != (rx, rz) and (x, z) != (rx, rz + 1):
                b.set(x, V, z, "white_concrete")
                if (x - rx) * (z - rz) == 0:
                    b.set(x, V + 1, z, "white_concrete")
    b.set(rx - 1, V + 1, rz - 1, "red_concrete")
    b.set(rx + 1, V + 1, rz - 1, "red_concrete")
    b.prop("raket", rx, V, rz, wie="de kapotte raket (3 x 6 x 3): hier bouw je hem af")
    b.sign(rx + 2, V, rz + 1, "south", ["Raket", "(bijna af)", "Mist: 3 dingen", "en een neus"], wall=False)
    # a picnic table, flowers, light
    b.tafel(cx + 4, V, cz + 5, "oak")
    b.stoel(cx + 3, V, cz + 5, "east", "oak_stairs")
    b.stoel(cx + 5, V, cz + 5, "west", "oak_stairs")
    for (x, z, bloem) in ((cx + 2, cz - 7, "poppy"), (cx - 7, cz + 1, "dandelion"), (cx + 7, cz + 3, "oxeye_daisy"), (cx + 1, cz + 9, "cornflower"),
                          (cx - 8, cz - 3, "poppy"), (cx + 8, cz - 6, "azure_bluet"), (cx - 1, cz - 9, "dandelion")):
        if b.naam(x, G, z) == "minecraft:grass_block" and b.naam(x, V, z) is None:
            b.set(x, V, z, bloem)
    for (x, z) in ((cx - 7, cz - 4), (cx + 2, cz + 2), (cx - 8, cz + 3), (cx + 8, cz + 1), (cx, cz - 8), (cx + 3, cz + 9), (cx - 1, cz - 2)):
        if b.naam(x, G, z) in ("minecraft:grass_block",):
            b.set(x, G, z, "shroomlight")
    b.zone("boomkoepel", int(cx - r), V, int(cz - r), int(cx + r), V + 12, int(cz + r), wie="de luchtkoepel van Sandy-guh")


def raket_onderdelen(b):
    """Where the three lost parts of Sandy's rocket lie around town."""
    for i, (x, z, wie) in enumerate(((151, 66, "bij het Kwallenveld in het noordoosten"), (72, 145, "achter de huisjes aan de Westring"),
                                     (96, 172, "tussen de huizen van de Schelpstraat")), 1):
        y = b.grond[(x, z)] + 1
        b.s.blocks.pop((x, y, z), None)
        b.prop(f"raket_onderdeel_{i}", x, y, z, wie=f"een kwijtgeraakt raketonderdeel: {wie}")


# =====================================================================================================================
# the rest home of Zeemeerman and Mosselguh
# =====================================================================================================================
def rusthuis(b):
    k = Hoek(b, 163, 80)
    BX, BZ = 18, 16
    k.bouwgrond(-1, -1, BX + 1, BZ + 12)
    for x in range(BX + 1):
        for z in range(BZ + 1):
            k.set(x, G, z, "birch_planks")
            k.set(x, V + 4, z, "light_blue_concrete" if x in (0, BX) or z in (0, BZ) else "white_terracotta")
            if x in (0, BX) or z in (0, BZ):
                for y in range(V, V + 4):
                    k.set(x, y, z, "light_blue_concrete" if y == V else "white_terracotta")
    for z in (3, 4, 12, 13):
        for y in (V + 1, V + 2):
            k.set(0, y, z, RUIT)
    for x in (4, 5, 13, 14):
        for y in (V + 1, V + 2):
            k.set(x, y, 0, RUIT)
            k.set(x, y, BZ, RUIT)
    k.deur(0, V, 8, "west", "birch")
    k.sign(-1, V + 2, 8, "west", ["Rusthuis", "De Luie Schelp", "Rust. Vadsen.", "Bingo: dinsdag"])
    k.sign(-1, V + 1, 7, "west", ["Bezoekuur:", "na het dutje", "(het dutje", "duurt lang)"])
    # the wall between the lounge and the two rooms east of it
    for z in range(1, BZ):
        for y in range(V, V + 4):
            k.set(11, y, z, "white_terracotta")
    for x in range(12, BX):
        for y in range(V, V + 4):
            k.set(x, y, 8, "white_terracotta")
    k.deur(11, V, 4, "west", "birch")
    k.deur(11, V, 12, "west", "spruce")
    k.sign(10, V + 1, 13, "west", ["Bezemkast", "(echt waar)", "Niet: geheime", "Zeemeergrot"])
    # the lounge: the tv with their own cartoon, two chairs, a bingo table
    for z in (7, 8, 9):
        for y in (V + 1, V + 2):
            k.set(10, y, z, "black_concrete")
    k.sign(9, V + 3, 8, "west", ["Zeemeerman en", "Mosselguh:", "de tekenfilm", "(herhaling 812)"])
    k.stoel(5, V, 7, "east", "birch_stairs")
    k.stoel(5, V, 9, "east", "birch_stairs")
    k.plek("zeemeerman", 6, V, 7, yaw=-90.0, wie="Zeemeerman", wezen="guhs:zeemeerman_guh")
    k.plek("mosselguh", 6, V, 9, yaw=-90.0, wie="Mosselguh", wezen="guhs:mosselguh")
    k.tafel(4, V, 13, "birch")
    k.stoel(3, V, 13, "east", "birch_stairs")
    k.stoel(5, V, 13, "west", "birch_stairs")
    k.sign(4, V, 14, "north", ["Bingo!", "(wie wakker is", "wint)", ""], wall=False)
    k.set(2, V, 2, "bookshelf")
    k.set(3, V, 2, "bookshelf")
    k.set(2, V + 1, 2, "flower_pot")
    for x in range(4, 8):
        for z in range(6, 11):
            if k.naam(x, V, z) is None:
                k.tapijt(x, V, z, "light_blue")
    # the bedroom: two beds, slippers
    k.bed(14, V, 2, "north", "orange")
    k.bed(16, V, 2, "north", "light_blue")
    k.set(15, V, 1, "birch_fence")
    k.set(15, V + 1, 1, "lantern", gb.lamp(False))
    k.kist(17, V, 6, "west", "huis")
    k.tapijt(14, V, 4, "pink")
    # the "broom cupboard": what is left of the Zeemeergrot
    k.set(17, V, 14, "polished_andesite")
    k.set(17, V + 1, 14, "blue_wool")
    k.sign(16, V + 1, 14, "west", ["Heldencape", "(in de was)", "", ""])
    k.set(13, V, 14, "observer", {"facing": "north", "powered": "false"})
    k.set(14, V, 14, "smooth_stone_slab", gb.slab("top"))
    k.set(14, V + 1, 14, "lever", {"face": "floor", "facing": "north", "powered": "false"})
    k.sign(13, V + 1, 14, "north", ["Heldencomputer", "Wachtwoord:", "KWAAAD", ""], wall=False)
    for (x, z) in ((5, 4), (5, 12), (14, 5), (14, 11), (9, 8)):
        k.lantaarn(x, V + 3, z)
    k.zone("rusthuis", 0, V, 0, BX, V + 4, BZ, wie="het rusthuis van Zeemeerman en Mosselguh")
    # the car park south of it: the place of the invisible bootmobiel (painted lines, nothing to see)
    for x in range(1, 14):
        for z in range(BZ + 2, BZ + 12):
            k.set(x, G, z, "gray_concrete")
    px0, pz0 = 3, BZ + 3
    for x in range(px0 - 1, px0 + 6):
        for z in range(pz0 - 1, pz0 + 8):
            if x in (px0 - 1, px0 + 5) or z == pz0 - 1:
                k.set(x, G, z, "white_concrete")
    k.zone("onzichtbare_boot", px0, V, pz0, px0 + 4, V + 3, pz0 + 6, wie="de parkeerplek van de onzichtbare bootmobiel (er is niets te zien)")
    k.sign(px0 - 2, V, pz0 + 3, "east", ["Gereserveerd:", "onzichtbare", "bootmobiel", "(hij staat er)"], wall=False)
    k.prop("riem", px0 + 7, V, pz0 + 5, wie="hier duikt de verloren riem van Zeemeerman op")
    k.set(px0 + 9, V, pz0 + 7, "birch_fence")
    k.set(px0 + 9, V + 1, pz0 + 7, "lantern", gb.lamp(False))
    k.straatlamp(12, BZ + 3)


# =====================================================================================================================
# the townsfolk: six round little houses, the bus stop, where they stand
# =====================================================================================================================
def huisje(b, cx, cz, r, hoog, kleur, dak, kant, nr, tekst):
    """A round house like the ones of the show: a painted drum with portholes, a dome of another colour, a pipe on top.
    kant = the side its door is on; nr = the bewoner who stands at its door."""
    b.bouwgrond(cx - int(r) - 1, cz - int(r) - 1, cx + int(r) + 1, cz + int(r) + 1)
    for (x, z) in schijf(cx, cz, r):
        b.set(x, G, z, "smooth_stone" if (x + z) % 2 else "polished_andesite")
    for y in range(V, V + hoog):
        for (x, z) in schijf(cx, cz, r, r - 1):
            b.set(x, y, z, f"{kleur}_concrete" if y != V + hoog - 1 else f"{dak}_concrete")
    b.bol(cx, V + hoog - 0.5, cz, r, r * 0.7, r, f"{dak}_concrete", hol=True, onder=V + hoog)
    top = max(y for y in range(V, V + 20) if b.naam(cx, y, cz))
    b.set(cx + 1, top, cz, "iron_block")
    b.set(cx + 1, top + 1, cz, "iron_bars")
    dx, dz = gb.DIRS[kant]
    R = int(round(r))
    deur = (cx + dx * R, cz + dz * R)
    while b.naam(deur[0], V, deur[1]) is None:      # (the wall, seen from outside)
        deur = (deur[0] - dx, deur[1] - dz)
    b.hol(deur[0], V, deur[1], deur[0], V + 1, deur[1])
    b.deur(deur[0], V, deur[1], kant, "birch")
    zij = (deur[0] + (1 if dz else 0), deur[1] + (1 if dx else 0))
    if b.naam(zij[0], V + 1, zij[1]):
        b.sign(zij[0] + dx, V + 1, zij[1] + dz, kant, tekst)
    for (ox, oz) in ((-dz, dx), (dz, -dx), (-dx, -dz)):   # portholes on the three other sides
        px, pz = cx + ox * R, cz + oz * R
        while b.naam(px, V + 1, pz) is None:
            px, pz = px - ox, pz - oz
        b.set(px, V + 2, pz, GLAS)
    # inside: a bed, a table, a chest, light
    b.tafel(cx - dx, V, cz - dz, "birch")
    b.stoel(cx - dx + (1 if dz else 0), V, cz - dz + (1 if dx else 0), "west" if dz else "north", "birch_stairs")
    b.set(cx - dx * (R - 2), V, cz - dz * (R - 2), f"{dak}_wool")
    b.set(cx - dx * (R - 2) + (1 if dz else 0), V, cz - dz * (R - 2) + (1 if dx else 0), f"{dak}_wool")
    b.set(cx - dx * (R - 2) - (1 if dz else 0), V, cz - dz * (R - 2) - (1 if dx else 0), "bookshelf")
    b.set(cx, G, cz, KORAAL)
    b.plek(f"bewoner_{nr}", deur[0] + dx * 2, V, deur[1] + dz * 2, yaw={"south": 0.0, "west": 90.0, "north": 180.0, "east": -90.0}[kant],
           wie="een Bikinibodemguh voor zijn huis", wezen=BEWONER)


def huisjes(b):
    huisje(b, 95, 85, 5.3, 5, "light_blue", "orange", "south", 1, ["Fam. Vin", "Bel kapot:", "roep maar", "njeg"])
    huisje(b, 117, 85, 5.3, 6, "lime", "purple", "south", 2, ["Hier woont", "een vis-guh.", "Of twee.", "Het wisselt."])
    huisje(b, 76, 124, 5.3, 5, "yellow", "red", "east", 3, ["Te koop", "(al jaren)", "Vraagprijs:", "3 knabbels"])
    huisje(b, 76, 141, 5.3, 6, "pink", "cyan", "east", 4, ["Nancy Guh", "Geen reclame", "Wel", "knabbels"])
    huisje(b, 95, 141, 5.3, 5, "orange", "blue", "north", 5, ["Kapper", "Vinnen knippen", "Ook: niks doen", "(populair)"])
    huisje(b, 117, 141, 5.3, 6, "magenta", "lime", "north", 6, ["Tom-guh", "CHOCOLA?!", "(ja graag)", ""])
    # the bus stop on the Noordstraat: the bus to Rotsbodem never comes
    x, z = 131, 78
    for dx in range(4):
        b.set(x + dx, G, z, KSTEEN)
        b.bebouwd.add((x + dx, z))
    b.stoel(x + 1, V, z, "north", "birch_stairs")
    b.stoel(x + 2, V, z, "north", "birch_stairs")
    b.set(x, V, z, STEEN_MUUR)
    b.set(x, V + 1, z, STEEN_MUUR)
    b.set(x, V + 2, z, KORAAL)
    b.sign(x, V + 1, z - 1, "north", ["Bushalte", "Bus naar", "Rotsbodem:", "net weg. Altijd"])
    b.plek("bewoner_7", x + 3, V, z - 1, yaw=180.0, wie="een Bikinibodemguh die op de bus wacht", wezen=BEWONER)
    b.plek("bewoner_8", 99, V, 116, yaw=-45.0, wie="een Bikinibodemguh op het plein", wezen=BEWONER)
    b.plek("bewoner_9", 118, V, 122, yaw=135.0, wie="een Bikinibodemguh op het plein", wezen=BEWONER)


# =====================================================================================================================
# De Vliegende Guhlander
# =====================================================================================================================
GUHLANDER = (126, V, 160)                        # where the overlay's origin lies in the build
GUHLANDER_DEK = 30                               # the deck, above the overlay's origin


def guhlander_schip(h):
    """The ghost ship as a template of its own (blocks only): a small vetschip in ghost green, with a column of bubbelstroom
    from the ground up to its deck. It hangs over the east end of the Schelpstraat when a later phase lets it appear."""
    L, B_, HH = 21, 9, 46
    s = h.Structure((L, HH, B_))
    mz, d = B_ // 2, GUHLANDER_DEK
    for x in range(1, L - 1):
        breed = 3 if 4 <= x <= 15 else 2 if x in (2, 3, 16, 17) else 1 if x in (1, 18) else 0
        for z in range(mz - breed, mz + breed + 1):
            rand = abs(z - mz) == breed
            for y in range(d - 3, d + 1):
                diep = d - 3 + (1 if rand else 0) + (1 if x in (1, 18) else 0)
                if y < diep:
                    continue
                if y == d:
                    s.set(x, y, z, "minecraft:warped_planks")
                elif rand or y == diep or x in (1, 19):
                    s.set(x, y, z, "minecraft:stripped_warped_hyphae" if y == d - 1 else "minecraft:warped_hyphae", {"axis": "x"})
            if rand and 2 <= x <= 17:
                s.set(x, d + 1, z, "minecraft:green_stained_glass")
    s.set(19, d, mz, "minecraft:warped_planks")
    s.set(19, d + 1, mz, "minecraft:warped_planks")
    s.set(20, d + 2, mz, "minecraft:warped_planks")
    # the stern cabin with the ghost captain's lantern, the mast with torn sails
    for x in range(2, 6):
        for z in range(mz - 2, mz + 3):
            rand = x in (2, 5) or abs(z - mz) == 2
            if rand:
                for y in range(d + 1, d + 4):
                    s.set(x, y, z, "minecraft:green_stained_glass" if y == d + 2 and not (x in (2, 5) and abs(z - mz) == 2) else "minecraft:warped_planks")
            s.set(x, d + 4, z, "minecraft:warped_slab", {"type": "bottom", "waterlogged": "false"})
    for y in (d + 1, d + 2):
        s.blocks.pop((5, y, mz), None)
    s.set(3, d + 3, mz, "guhs:spooklantaarn", {"hanging": "true", "waterlogged": "false"})
    for y in range(d + 1, d + 14):
        s.set(10, y, mz, "minecraft:stripped_warped_stem", {"axis": "y"})
    for y, breed in ((d + 12, 3), (d + 7, 4)):
        for z in range(mz - breed, mz + breed + 1):
            s.set(10, y, z, "minecraft:stripped_warped_stem", {"axis": "z"})
    for y in range(d + 3, d + 12):
        for z in range(mz - 4, mz + 5):
            gat = (y * 7 + z * 13) % 11 in (0, 3) or (y < d + 7 and abs(z - mz) == 4)
            if y not in (d + 7,) and not gat and abs(z - mz) <= (3 if y > d + 7 else 4):
                s.set(11, y, z, "minecraft:lime_stained_glass" if (y + z) % 5 else "minecraft:green_stained_glass")
    s.set(10, d + 14, mz, "guhs:spooklantaarn", {"hanging": "false", "waterlogged": "false"})
    s.set(16, d + 1, mz, "minecraft:warped_fence", {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
    s.set(16, d + 2, mz, "guhs:spooklantaarn", {"hanging": "false", "waterlogged": "false"})
    s.set(18, d + 1, mz, "guhs:spooklantaarn", {"hanging": "false", "waterlogged": "false"})
    # the way up: a column of bubbelstroom through the hull, from the ground to the deck
    for y in range(0, d + 2):
        s.set(13, y, mz, STROOM, {"richting": "omhoog"})
    for (x, z) in ((12, mz), (14, mz), (13, mz - 1), (13, mz + 1)):
        for y in range(d - 3, d):
            if s.get(x, y, z) is None:
                s.set(x, y, z, "minecraft:warped_planks")
    return s


def guhlander(b, h):
    ox, oy, oz = GUHLANDER
    schip = guhlander_schip(h)
    b.laag("vliegende_guhlander", schip, ox, oy, oz)
    d = oy + GUHLANDER_DEK
    b.prop("guhlander_anker", ox, oy, oz, los=True,
           wie="de oorsprong van de laag vliegende_guhlander: het spookschip hangt boven het oosten van de Schelpstraat")
    b.prop("guhlander_dek", ox + 8, d + 1, oz + 4, wie="op het dek van De Vliegende Guhlander", los=True)
    b.plek("spookkapitein", ox + 7, d + 1, oz + 4, yaw=-90.0, wie="de spookkapitein, op zijn dek", wezen="guhs:spookkapitein", los=True)
    # on the ground: the mooring, a ring of stones around the spot where the column comes down
    tx, tz = ox + 13, oz + 4
    for x in range(tx - 2, tx + 3):
        for z in range(tz - 2, tz + 3):
            if abs(x - tx) == 2 or abs(z - tz) == 2:
                b.set(x, G, z, KSTEEN)
            else:
                b.set(x, G, z, "minecraft:dark_prismarine")
            b.bebouwd.add((x, z))
            for y in range(V, V + 3):
                b.s.blocks.pop((x, y, z), None)
    b.set(tx - 2, V, tz - 2, STEEN_MUUR)
    b.set(tx - 2, V + 1, tz - 2, "guhs:spooklantaarn", {"hanging": "false", "waterlogged": "false"})
    b.sign(tx + 2, V, tz - 2, "north", ["Aanlegplaats", "De Vliegende", "Guhlander", "(soms. Boe.)"], wall=False)
    b.prop("guhlander_trap", tx, V, tz + 1, wie="op de grond: hier begint de weg omhoog naar het spookschip")
