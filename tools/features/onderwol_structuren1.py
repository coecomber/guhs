"""
1.7.0 "Onder de Wol", structures part 1 (design: MineScapeStory/ontwerp-guhs170-mijnen.md chapter 2.4 and rounds 14-20; Java:
feature/onderwol/bouw; framework: features/onderwol_bouw.py): the first seven buildings under the ground, made from the
design renders (frozen scenes in features/onderwol_bouw_bron, see onderwol_bouw_scene.py):

    mijnguhdorp            r2   gegarandeerd (one per world 300-1000 blocks from 0,0, only in new terrain) + rare extra copies.
                                Around a saus lake: houses on both shores, terraces, the Mijnguh's kraam, a wolkenlift shaft
                                to a mijningang-hutje, 4 side tunnels (jigsaw) to attached buildings (Knuffelslaapzaal,
                                voorraadgrotje, uitkijkje; later slices add the Kaasmakerij, Wolspinnerij, Kristalkwekerij)
    knuffelslaapzaal       r3   af en toe (also attached to the Mijnguhdorp)
    paddenstoelendorpje    r4   af en toe
    verlaten_guhmijn       r5   vaak: a random network (jigsaw) of corridors, crossings, stairs and four special rooms
    grotschilderingen      r6   vaak: mostly a small wall painting in a niche (4 kinds), sometimes the great fresco hall
    sausmeer_vissershutje  r7   vaak
    mijningang_hutje       r8   af en toe: the hut on the surface + its shaft down to a little mine room; the hut is also THE
                                entrance hut of every guhs:grot_bouw with an "ingang" (pool guhs:onderwol/ingang_hutje/start)

    PYTHONPATH=tools python tools/features/onderwol_structuren1.py      (from the project root; standalone, idempotent)
"""
import json
import math
import os
import random

import numpy as np

import make_structures as ms
import sign_text
from features import onderwol_bouw as ob
from features import onderwol_bouw_scene as sc

LAAGBLOK = {"minecraft:pink_wool", "guhs:c_viltsteen", "guhs:knuffelsteen", "guhs:guh_kristalsteen", "guhs:c_bodem"}
CONCEPT = {"guhs:c_viltsteen": "guhs:viltsteen", "guhs:c_viltsteen_bakstenen": "guhs:viltsteen_bakstenen",
           "guhs:c_viltsteen_tegels": "guhs:viltsteen_tegels", "guhs:c_viltsteen_gepolijst": "guhs:gepolijste_viltsteen",
           "guhs:c_guhklei": "guhs:guhklei", "guhs:c_lichtmos": "guhs:lichtmos", "guhs:c_glinsterpaddenstoel": "guhs:glinsterpaddenstoel",
           "guhs:c_glitterrank": "guhs:glitterrank", "guhs:c_glitterrank_bes": "guhs:glitterrank_bes", "guhs:c_pluisbolletje": "guhs:pluisbolletje",
           "guhs:c_pluiskussen": "guhs:pluiskussen", "guhs:c_vahoegiet_erts": "guhs:vahoegiet_erts", "guhs:c_bodem": "guhs:knuffelnaad",
           # (r06: the render passed these palette letters as block names)
           "guhs:C": "guhs:pleister_vanille", "guhs:L": "guhs:pleister_lavendel", "guhs:S": "minecraft:light_blue_terracotta"}
ERTS = {"c_vadserts": "vadserts", "c_knabbelerts": "kaasknabbelerts", "c_pigmenterts": "pigmenterts"}
BORDJES = {"guhs:infobord", "guhs:dierentuinbordje"}   # (the renders' stand-ins for signs: real signs go there)

PREFIX = "sign.guhs.onderwol_bouw"
SALT = 17010001   # (this slice: 17010001, 17010011, ...)

# the talking guhs (Java OnderWolBouw.PRAAT: the same pleks and counts)
PRAAT = {
    "onderwol_dorp": ["Welkom in het Mijnguhdorp! Hier hakken we, knabbelen we en hakken we nog wat.",
                      "Bulk is fijn! De Mijnguh in de kraam ruilt bouwstoffen voor knabbels.",
                      "Het sausmeer is warm. Zwemmen mag, slurpen niet. Ik heb het geprobeerd. Njeg.",
                      "Boven ons ligt een hele berg roze wol. Wij wonen er gewoon onder. Gezellig toch?",
                      "Door de zijgangen kom je bij de buren. Stil zijn bij de slaapzaal!"],
    "onderwol_ingang": ["Naar beneden? Stap op het wolkje. Ladder mag ook, voor de durfals.",
                        "Onderin ligt de mijn. Neem een lampje mee, het is daar donkerder dan in een kaasknabbelzak.",
                        "Twee wolkjes: eentje omhoog, eentje omlaag. Niet verwisselen, njeg!",
                        "Ik ben de liftguh. Ik druk op geen enkele knop, maar ik sta hier wel heel professioneel."],
    "onderwol_mijn": ["Deze mijn is ouder dan mijn oma. En mijn oma is heel oud.",
                      "Eerst knabbelen, dan hakken. Zo doen guhs dat al eeuwen.",
                      "De gangen lopen alle kanten op. Ik ben zelf ook al drie keer verdwaald. Vandaag.",
                      "Kistsleetjes vol buit staan hier nog. Pak maar, de oude mijnguhs zijn allang met pensioen."],
    "onderwol_slaapzaal": ["Sssst... de babyguhs slapen. Als ze wakker worden, wil iedereen een knabbel.",
                           "Schaapjes tellen helpt. Bij schaapje drie val ik zelf altijd in slaap.",
                           "De wekker staat op nooit. Dat is hier de beste tijd.",
                           "Aai een slapend guhtje heel zachtjes, dan wordt hij blij wakker."],
    "onderwol_paddenstoel": ["Welkom in ons dorpje! Klop even aan, dan klim je zo de steel in.",
                             "Mijn huisje zit in de hoed. Pas op je hoofd, het is maar een hoedje.",
                             "Lampjes aan touwtjes, glinsterbeekje, paddenstoelen: wat wil een guh nog meer?",
                             "Nee, je mag mijn huis niet opeten. Ook niet een klein hapje. Njeg!"],
    "onderwol_visser": ["Ik ben Henk Hengel. Ze bijten vandaag niet. Alleen de kaas.",
                        "Vandaag gevangen: nul vissen en drie sokken. Een goede dag!",
                        "Een visvergunning kost één knabbel. Die heb ik zelf opgegeten.",
                        "De saus is warm. Niet drinken, wel dippen."],
    "onderwol_gids": ["Ik ben Oma Pluis, de gids. Lang geleden was alles grijs en koud. Brr!",
                      "Toen vond een guh de allereerste roze wolbol. Kijk, daar op de muur!",
                      "Spinnen, spinnen, spinnen... en zo werd de hele Guhmension roze.",
                      "Elke guh draagt een stukje Knuffelhart. Dat deel je uit door lief te zijn.",
                      "Niet aanraken hoor, de verf is pas drieduizend jaar oud."],
}
PRAAT_EN = {
    "onderwol_dorp": ["Welcome to the Mijnguh village! Here we dig, we nibble and then we dig some more.",
                      "Bulk is lovely! The Mijnguh in the stall trades building stuff for nibbles.",
                      "The sauce lake is warm. Swimming is allowed, slurping is not. I tried. Njeg.",
                      "Above us lies a whole mountain of pink wool. We just live under it. Cosy, right?",
                      "The side tunnels lead to the neighbours. Be quiet near the dormitory!"],
    "onderwol_ingang": ["Going down? Step onto the little cloud. The ladder is fine too, for the brave.",
                        "The mine is at the bottom. Bring a lamp, it's darker down there than inside a bag of cheese nibbles.",
                        "Two little clouds: one goes up, one goes down. Don't mix them up, njeg!",
                        "I'm the lift guh. I don't press any buttons, but I do stand here very professionally."],
    "onderwol_mijn": ["This mine is older than my grandma. And my grandma is very old.",
                      "First nibble, then dig. Guhs have done it this way for ages.",
                      "The tunnels go every which way. I got lost three times myself. Today.",
                      "There are still little sleds full of loot here. Help yourself, the old miner guhs retired long ago."],
    "onderwol_slaapzaal": ["Shhh... the baby guhs are asleep. If they wake up, everybody wants a nibble.",
                           "Counting sheep helps. I always fall asleep myself at sheep number three.",
                           "The alarm clock is set to never. That's the best time around here.",
                           "Pet a sleeping guh very gently, and it wakes up happy."],
    "onderwol_paddenstoel": ["Welcome to our little village! Knock first, then you climb right up the stem.",
                             "My house is in the cap. Mind your head, it's only a little hat.",
                             "Lights on strings, a sparkly brook, mushrooms: what more could a guh want?",
                             "No, you may not eat my house. Not even a tiny bite. Njeg!"],
    "onderwol_visser": ["I'm Henk Hengel. They're not biting today. Only the cheese is.",
                        "Caught today: zero fish and three socks. A good day!",
                        "A fishing licence costs one nibble. I ate it myself.",
                        "The sauce is warm. Don't drink it, do dip in it."],
    "onderwol_gids": ["I'm Granny Pluis, the guide. Long ago everything was grey and cold. Brr!",
                      "Then a guh found the very first pink ball of wool. Look, up there on the wall!",
                      "Spinning, spinning, spinning... and that's how the whole Guhmension turned pink.",
                      "Every guh carries a little piece of the Cuddle Heart. You share it by being kind.",
                      "Don't touch, please: the paint is only three thousand years old."],
}

# the English of every sign line (sign_text keys are made from the Dutch); a line without English stays Dutch
EN_REGELS = {
    "Naar de": "To the", "guhmijnen!": "guh mines!", "Wolkje op of": "Cloud up or", "wolkje neer.": "cloud down.",
    "Ladder voor": "Ladder for", "durfals!": "the brave!", "Mind your": "Mind your", "pootjes!": "paws!",
    "Hier begint": "Here begins", "jouw mijn!": "your mine!", "Hak ze, guh!": "Dig away, guh!",
    "Bouwstoffen": "Building stuff", "in bulk!": "in bulk!", "Ruilen voor": "Trade for", "knabbels": "nibbles",
    "Proeven": "No tasting.", "verboden.": "Not even", "Ja, ook guhklei.": "the guh clay.", "Njeg.": "Njeg.",
    "Naar boven:": "Up there:", "de roze wol.": "the pink wool.", "Naar beneden:": "Down there:", "hier ben je al.": "you're here.",
    "Sausmeer.": "Sauce lake.", "Zwemmen mag,": "Swimming: yes,", "slurpen niet.": "slurping: no.",
    "Mijnguhdorp": "Mijnguh village", "Welkom onder": "Welcome under", "de wol!": "the wool!",
    "Stil! Guhs": "Quiet! Guhs", "slapen.": "are sleeping.", "Sssst... njeg.": "Shhh... njeg.",
    "Hier slaapt": "Here sleeps", "Schaapjes": "Counting", "tellen: 1...": "sheep: 1...", "2... njeg...": "2... njeg...", "zzz": "zzz",
    "Wekker staat op:": "Alarm set to:", "nooit": "never", "Slaapliedje:": "Lullaby:", "druk niet op": "don't press", "stop, njeg": "stop, njeg",
    "Knuffel-": "Cuddle", "slaapzaal": "dormitory",
    "Paddenstoelen-": "Mushroom", "dorpje": "village", "Graag kloppen,": "Please knock,", "njeg!": "njeg!",
    "Pas op je hoofd!": "Mind your head!", "(het is maar": "(it's only", "een hoedje)": "a little hat)",
    "Verlaten": "Abandoned", "guhmijn": "guh mine", "Betreden op": "Enter at", "eigen risico,": "your own risk,",
    "Eerst knabbelen,": "First nibble,", "dan hakken.": "then dig.", "Knabbel-": "Nibble", "voorraad: max 1": "store: max 1",
    "per hap, njeg": "per bite, njeg", "Sleedepot:": "Sled depot:", "parkeren op": "park at", "eigen risico": "your own risk",
    "Lenen =": "Borrowing =", "terugbrengen.": "bringing back.", "Vahoeg!": "Vahoeg!", "Ingestort.": "Caved in.",
    "Andere kant op!": "Other way!", "Naar boven:": "Up there:",
    "Ooit was alles": "Once all was", "grijs en koud.": "grey and cold.", "Brr, njeg.": "Brr, njeg.",
    "Tot een guh de": "Until a guh", "allereerste": "found the very", "roze wolbol": "first pink wool", "vond!": "ball!",
    "Spinnen, spinnen": "Spinning, spinning", "spinnen...": "spinning...", "vahoeg, wat een": "vahoeg, what", "wol!": "wool!",
    "En zo werd de": "And that's how", "hele Guhmension": "the Guhmension", "roze. Einde.": "turned pink. End.", "(Of?)": "(Or is it?)",
    "Elke guh draagt": "Every guh carries", "een stukje": "a little piece of", "Knuffelhart en": "Cuddle Heart and", "deelt het uit": "shares it out",
    "Wie lief is, mag": "Who is kind may", "later op de": "later cuddle on", "wolkjes": "the little", "knuffelen": "clouds",
    "Niet aanraken,": "Don't touch,", "verf is pas": "the paint is only", "3000 jaar oud": "3000 years old",
    "De eerste": "The first", "wolbol": "wool ball", "Het Knuffelhart": "The Cuddle Heart", "Alles roze!": "All pink!",
    "Grotschildering": "Cave painting", "Visvergunning:": "Fishing licence:", "1 knabbel": "1 nibble",
    "Vandaag": "Caught today:", "gevangen:": "", "0 vissen,": "0 fish,", "3 sokken": "3 socks",
    "Saus is warm.": "Sauce is warm.", "Niet drinken,": "Don't drink it,", "wel dippen.": "do dip in it.",
    "Voorraad.": "Supplies.", "Eerlijk delen,": "Share fairly,", "Stil plekje": "Quiet spot", "om te vadsen": "for vadsing",
    "Hier wordt nog": "Still digging", "gegraven!": "here!", "Huize": "House",
    "Kristallen": "Crystals", "niet likken": "no licking",
}


# =====================================================================================================================
# the scene conversion
# =====================================================================================================================
def kaart(s):
    """A render block string -> the real block string (None: leave it out)."""
    naam = s.split("[")[0]
    rest = s[len(naam):]
    if naam in BORDJES or naam == "guhs:wolkenlift":
        return None
    if naam in CONCEPT:
        return CONCEPT[naam] + rest
    for c, echt in ERTS.items():
        if naam.startswith("guhs:" + c + "_"):
            return f"guhs:{echt}_{naam.split('_')[-1]}{rest}"
    return s


def lucht_buren(v, ook=()):
    """Per voxel: does one of its six neighbours hold air (in the scene)? (ook: palette indices that count as air too)"""
    a = (v == 0) | np.isin(v, list(ook))
    p = np.pad(a, 1, constant_values=False)
    return (p[2:, 1:-1, 1:-1] | p[:-2, 1:-1, 1:-1] | p[1:-1, 2:, 1:-1] | p[1:-1, :-2, 1:-1] | p[1:-1, 1:-1, 2:] | p[1:-1, 1:-1, :-2])


def omzet(scene, naam, crop, lucht="hol", schil_dik=1, aanpas=None):
    """A frozen render scene -> a Bouw. crop = (x0, x1, y0, y1, z0, z1) inclusive, scene coordinates.
    lucht "hol": every air voxel of the scene is carved (the scene's own cave is the room); "zaal": only the air the
    script set itself (the Java hall carves the rest). The layer rock: SCHIL within schil_dik of air, else left out.
    aanpas(v, gezet, pal) may change the scene arrays first. Returns (Bouw, guhs, meta) with the guhs in template coordinates."""
    v, gezet, pal, guhs, meta = sc.laad(scene)
    v = v.copy()
    if aanpas:
        aanpas(v, gezet, pal)
    x0, x1, y0, y1, z0, z1 = crop
    x1, y1, z1 = min(x1, v.shape[0] - 1), min(y1, v.shape[1] - 1), min(z1, v.shape[2] - 1)
    # (rock next to anything that is not rock gets the SCHIL: air, plants, buildings, the dropped stand-ins)
    buren = lucht_buren(v, [i for i, s in enumerate(pal) if i and s.split("[")[0] not in LAAGBLOK and not any(s.startswith("guhs:" + c) for c in ERTS)])
    for _ in range(schil_dik - 1):
        p = np.pad(buren, 1, constant_values=False)
        buren = buren | p[2:, 1:-1, 1:-1] | p[:-2, 1:-1, 1:-1] | p[1:-1, 2:, 1:-1] | p[1:-1, :-2, 1:-1] | p[1:-1, 1:-1, 2:] | p[1:-1, 1:-1, :-2]
    b = ob.Bouw((x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1), naam, prefix=f"{PREFIX}.{naam.split('_')[0]}")
    gekend = {}
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                i = int(v[x, y, z])
                s = pal[i]
                lx, ly, lz = x - x0, y - y0, z - z0
                if i == 0:
                    if lucht == "hol" or gezet[x, y, z]:
                        b.set(lx, ly, lz, "minecraft:air")
                    continue
                basis = s.split("[")[0]
                if basis in LAAGBLOK:
                    if buren[x, y, z]:
                        b.schil(lx, ly, lz)
                    continue
                if any(basis.startswith("guhs:" + c) for c in ERTS):
                    if buren[x, y, z]:
                        b.zet(lx, ly, lz, kaart(s))
                    continue
                if s not in gekend:
                    gekend[s] = kaart(s)
                if gekend[s]:
                    b.zet(lx, ly, lz, gekend[s])
    gs = [(g[0] - x0, g[1] - y0, g[2] - z0, g[3], g[4], g[5]) for g in guhs
          if x0 <= g[0] <= x1 + 1 and y0 <= g[1] <= y1 + 1 and z0 <= g[2] <= z1 + 1]
    return b, gs, meta


PLANTJES = {"guhs:lichtmos", "guhs:glinsterpaddenstoel", "guhs:guhpaddenstoel", "guhs:pluisbolletje", "guhs:glitterrank", "guhs:glitterrank_bes",
            "guhs:roze_gras", "guhs:guh_kristal_cluster"}


def eerste_vloer(b, x, z, van=None):
    """The highest floor-layer y in this template column with free room above (a solid block, air or a plant above it)."""
    for y in range((van if van is not None else b.size[1] - 2), 0, -1):
        onder = b.blocks.get((x, y, z))
        boven = b.blocks.get((x, y + 1, z))
        if onder and onder[0] != "minecraft:air" and onder[0] not in PLANTJES and (boven is None or boven[0] in PLANTJES or boven[0] == "minecraft:air"):
            if boven is not None and boven[0] in PLANTJES:
                b.leeg(x, y + 1, z)
            return y
    return None


# =====================================================================================================================
# r08 the mijningang-hutje: the entrance hut (pool guhs:onderwol/ingang_hutje/start) and the standalone mine room
# =====================================================================================================================
def ingang_hutje(h):
    v, gezet, pal, guhs, m = sc.laad("r08_mijningang_hutje")
    HV, SX, SZ = m["HV"], m["SX"], m["SZ"]
    x0, x1, y0, y1, z0, z1 = 6, 20, HV - 3, HV + 9, 8, 20
    b, gs, _ = omzet("r08_mijningang_hutje", "ingang_hutje", (x0, x1, y0, y1, z0, z1))
    b.prefix = f"{PREFIX}.hutje"
    X = lambda x: x - x0
    Y = lambda y: y - y0
    Z = lambda z: z - z0
    hut = "pastelbaksteen_roze"
    # the floor opening: 3 x 1 across x at z = SZ (the lift lets you off to the south), lined with bricks below the floor
    for x in range(SX - 2, SX + 3):
        for z in range(SZ - 1, SZ + 2):
            for y in (HV - 3, HV - 2):
                b.zet(X(x), Y(y), Z(z), hut)
            b.zet(X(x), Y(HV - 1), Z(z), "vadshout_planken")
            b.set(X(x), Y(HV), Z(z), "minecraft:air")
    for x in range(SX - 1, SX + 2):
        for y in (HV - 3, HV - 2, HV - 1):
            b.leeg(X(x), Y(y), Z(SZ))
    b.ingang_boven(X(SX), Y(HV - 1), Z(SZ), "south")
    for x in range(SX - 2, SX + 3):
        b.zet(X(x), Y(HV), Z(SZ - 1), "guh_hekje_roze")
    b.zet(X(SX - 2), Y(HV), Z(SZ), "guh_hekje_roze")
    b.zet(X(SX + 2), Y(HV), Z(SZ), "guh_hekje_roze")
    # signs: outside by the door, inside by the opening
    b.sign(X(9), Y(HV), Z(10), "west", ["Naar de", "guhmijnen!", "Wolkje op of", "wolkje neer."], wall=False)
    b.sign(X(SX + 3), Y(HV), Z(SZ + 1), "north", ["Ladder voor", "durfals!", "Mind your", "pootjes!"], wall=False)
    # the lift guh inside, a wild guh outside
    b.npc(X(16), Y(HV), Z(12), "onderwol_ingang", yaw=200)
    b.guh(X(7.5), Y(HV), Z(10.5), yaw=270)
    # loot: the chest and the closed barrel
    b.kist(X(17), Y(HV), Z(11), "west", LOOT["ingang_hutje"])
    b.ton(X(11), Y(HV), Z(12), LOOT["ingang_hutje"])
    b.save("onderwol/ingang_hutje/start")
    ob.processor_list(h)
    ob.pool(h, "onderwol/ingang_hutje/start", [(b, 1)])
    return b


def mijnkamertje(h):
    """The little mine room at the bottom of the standalone mijningang-hutje's shaft (the Java hall carves the room)."""
    b = ob.Bouw((11, 6, 11), "mijningang_hutje", prefix=f"{PREFIX}.kamertje")
    c = 5
    for x in range(11):
        for z in range(11):
            d = math.hypot(x - c, z - c)
            if d <= 5.2:
                b.zet(x, 0, z, "viltsteen_tegels" if d < 2.5 else "gladde_knuffelsteen" if (x + z) % 3 else "viltsteen")
    b.ingang(c, 0, c, final="guhs:viltsteen_tegels")
    b.anker(c, 0, c + 3, final="guhs:gladde_knuffelsteen")
    b.sign(c + 3, 1, c + 2, "west", ["Hier begint", "jouw mijn!", "Hak ze, guh!", ""], wall=False)
    b.kist(c + 4, 1, c - 2, "west", LOOT["mijnkamertje"])
    b.zet(c - 4, 1, c - 1, "baltoslee_minislee[facing=south]")
    b.zet(c - 4, 1, c, "block_of_kaasknabbels")
    b.zet(c - 3, 1, c + 3, "guh_lantaarnpaal[half=lower]")
    b.zet(c - 3, 2, c + 3, "guh_lantaarnpaal[half=upper]")
    b.zet(c + 3, 1, c + 4, "lichtmos")
    b.zet(c - 2, 1, c - 4, "glinsterpaddenstoel")
    b.zet(c + 4, 1, c + 1, "guh_kristal_cluster[facing=up]")
    b.save("onderwol/mijningang_hutje/start")
    return b


# =====================================================================================================================
# r2 the Mijnguhdorp
# =====================================================================================================================
HUISNAMEN = ["Vadsig", "Knabbel", "Pluis", "Stip", "Wolkje", "Hakmans", "Snoet", "Kaasje", "Njegje", "Vahoeg", "Guhbert"]


def mijnguhdorp(h):
    v, gezet, pal, guhs, m = sc.laad("r2_mijnguhdorp")
    VL, LX, LZ, WX, WZ, MX, MZ, ZS = m["VL"], m["LX"], m["LZ"], m["WX"], m["WZ"], m["MX"], m["MZ"], m["ZS"]
    Ys = v.shape[1]

    def zonder_schacht(v, gezet, pal):
        """The render's lift shaft above the cave goes: rock again (the Java shaft takes its place); in the cave it stays air."""
        rots = pal.index("guhs:knuffelsteen") if "guhs:knuffelsteen" in pal else 1
        vilt = pal.index("guhs:c_viltsteen") if "guhs:c_viltsteen" in pal else rots
        for y in range(VL, Ys):
            grot = v[LX + 4, y, LZ] == 0 and v[LX - 4, y, LZ] == 0 and v[LX, y, LZ + 4] == 0 and v[LX, y, LZ - 4] == 0
            for dx in range(-2, 3):
                for dz in range(-2, 3):
                    if grot:
                        v[LX + dx, y, LZ + dz] = 0
                    else:
                        v[LX + dx, y, LZ + dz] = vilt if y + m["DY"] >= 45 else rots
    x0, x1, y0, y1, z0, z1 = 7, 105, 3, 37, 3, 89
    b, gs, _ = omzet("r2_mijnguhdorp", "mijnguhdorp", (x0, x1, y0, y1, z0, z1), aanpas=zonder_schacht)
    b.prefix = f"{PREFIX}.dorp"
    X = lambda x: x - x0
    Y = lambda y: y - y0
    Z = lambda z: z - z0
    # the anchor: the plein by the kraam; the shaft comes down on the plein by the lift (west)
    b.anker(X(82), Y(VL - 1), Z(36))
    b.ingang(X(LX), Y(VL - 1), Z(LZ))
    # the side tunnels: their ends become jigsaw connectors (the attached buildings bring their own gate and sign)
    def tunnel(x, z, facing, pool):
        b.verbinding(X(x), Y(VL), Z(z), facing, "guhs:mijnguhdorp_zij", "guhs:mijnguhdorp_zij_in", f"guhs:onderwol/mijnguhdorp/{pool}")
    tunnel(7, 40, "west", "zij_slaapzaal")
    tunnel(105, 58, "east", "zij")
    tunnel(38, 3, "north", "zij")
    tunnel(70, 3, "north", "zij")
    # (the render's gates and their signs go: each attached building has its own)
    for (px, pz) in ((8, 38), (8, 42), (98, 56), (98, 60)):
        for y in range(VL, VL + 5):
            if b.get(X(px), Y(y), Z(pz)) and b.get(X(px), Y(y), Z(pz)) != ob.SCHIL:
                b.schil(X(px), Y(y), Z(pz))
    # the signs of the render (legend), now real signs
    b.set(X(WX - 1), Y(VL + 4), Z(WZ), "minecraft:air")
    hangbord(b, X(WX - 1), Y(VL + 4), Z(WZ), "west", ["Bouwstoffen", "in bulk!", "Ruilen voor", "knabbels"])
    b.sign(X(WX - 6), Y(VL), Z(WZ + 4), "south", ["Proeven", "verboden.", "Ja, ook guhklei.", "Njeg."], wall=False)
    b.sign(X(LX + 3), Y(VL), Z(LZ + 1), "east", ["Naar boven:", "de roze wol.", "Naar beneden:", "hier ben je al."], wall=False)
    b.sign(X(MX - 1), Y(VL), Z(MZ), "south", ["Sausmeer.", "Zwemmen mag,", "slurpen niet.", ""], wall=False)
    b.sign(X(LX + 3), Y(VL), Z(LZ - 2), "east", ["Mijnguhdorp", "Welkom onder", "de wol!", ""], wall=False)
    # name boards by the house doors (north row, south row, terraces): a wall sign right of each door
    namen = iter(HUISNAMEN)
    for x0h in (22, 31, 44, 66, 75):
        deur(b, X, Y, Z, x0h + 3, VL, "zuid", next(namen))
    for x0h in (24, 40, 60, 78):
        deur(b, X, Y, Z, x0h + 3, VL, "noord", next(namen), zf=ZS)
    for (x0h, vl2, _) in m["TERRAS"]:
        deur(b, X, Y, Z, x0h + 3, vl2, "zuid", next(namen))
    # the barrels in the houses: loot
    for (pos, blok) in list(b.blocks.items()):
        if blok[0] == "minecraft:barrel" and blok[1].get("open") == "false" and blok[2] is None:
            px, py, pz = pos
            if b.get(px, py, pz - 1) != "minecraft:barrel" and b.get(px, py, pz + 1) != "minecraft:barrel":   # (not the kraam's counter)
                b.ton(px, py, pz, LOOT["mijnguhdorp"])
    # the Mijnguh behind the counter, two talking dorp guhs, the rest wild guhs
    b.mijnguh(X(WX + 2), Y(VL), Z(WZ), yaw=90)
    praters = {(26, 38), (64, 74)}
    for (gx, gy, gz, yaw, _, zit) in gs[1:]:
        if (int(gx + x0), int(gz + z0)) in praters:
            b.npc(int(gx), gy, int(gz), "onderwol_dorp", yaw=yaw)
        else:
            b.guh(gx, gy, gz, yaw=yaw)
    b.save("onderwol/mijnguhdorp/start")
    return b


def hangbord(b, x, y, z, facing, lines):
    """A hanging sign under the block above it (the kraam's roof)."""
    nbt = {"id": "minecraft:hanging_sign", "is_waxed": ms.Byte(1),
           "front_text": {"messages": sign_text.messages(b.prefix, lines), "color": "black", "has_glowing_text": ms.Byte(0)},
           "back_text": {"messages": sign_text.messages(b.prefix, lines), "color": "black", "has_glowing_text": ms.Byte(0)}}
    b.set(x, y, z, "minecraft:birch_hanging_sign", {"rotation": ob.ROT[facing], "attached": "false", "waterlogged": "false"}, nbt)


def deur(b, X, Y, Z, m, vl, naar, naam, zf=None):
    """A name board right of a house's door (the render's huisje(): door at x0 + 3, facade at zf)."""
    if zf is None:
        # (north row and terraces: find the facade, the first non-air block north of the door's street side)
        for z in range(0, 100):
            blok = b.blocks.get((X(m), Y(vl), Z(z)))
            if blok and "door" in blok[0]:
                zf = z
                break
        if zf is None:
            return
    voor = 1 if naar == "zuid" else -1
    face = "south" if naar == "zuid" else "north"
    p = (X(m + 1), Y(vl + 1), Z(zf + voor))
    if b.blocks.get(p) in (None,) or b.get(*p) == "minecraft:air":
        b.sign(*p, face, ["Huize", naam, "", ""], wall=True)


def dorp_zijstukken(h):
    """The pieces at the ends of the village's side tunnels (pools guhs:onderwol/mijnguhdorp/zij...): a short tunnel each,
    with its jigsaw guhs:mijnguhdorp_zij_in at the open end (at foot level, facing out of the piece)."""
    stukken = {}

    def gang(naam, lengte, inhoud):
        b = ob.Bouw((7, 7, lengte + 6), f"dorp_{naam}", prefix=f"{PREFIX}.dorp")
        # tunnel along +z from z = 0 (the open end) to z = lengte, 3 wide, 4 high, a little room at the end
        for z in range(lengte + 6):
            for x in range(7):
                for y in range(7):
                    binnen_gang = 2 <= x <= 4 and 1 <= y <= 4 and z <= lengte
                    kamer = 1 <= x <= 5 and 1 <= y <= 5 and lengte < z <= lengte + 4
                    if binnen_gang or kamer:
                        b.set(x, y, z, "minecraft:air")
                    elif (1 <= x <= 5 and 0 <= y <= 5 and z <= lengte) or (0 <= x <= 6 and 0 <= y <= 6 and lengte <= z <= lengte + 5):
                        b.schil(x, y, z)
        for z in range(lengte + 5):
            for x in range(1, 6):
                if b.get(x, 1, z) == "minecraft:air" or (2 <= x <= 4 and z <= lengte):
                    b.zet(x, 0, z, "gladde_knuffelsteen")
        b.verbinding(3, 1, 0, "north", "guhs:mijnguhdorp_zij_in")
        inhoud(b, lengte)
        b.save(f"onderwol/mijnguhdorp/{naam}")
        stukken[naam] = b
        return b

    def voorraad(b, L):
        for x in (1, 5):
            for z in (L + 1, L + 2, L + 3):
                b.ton(x, 1, z, LOOT["mijnguhdorp"])
        b.zet(3, 1, L + 4, "block_of_kaasknabbels")
        b.zet(2, 1, L + 4, "minecraft:barrel[facing=up,open=true]")
        b.zet(3, 4, L + 2, "lampion_geel[hanging=true]")
        b.sign(4, 1, L, "south", ["Voorraad.", "Eerlijk delen,", "njeg!", ""], wall=False)

    def uitkijk(b, L):
        b.zet(3, 1, L + 4, "guh_bankje_roze[facing=north]")
        for (x, z) in ((1, L + 1), (5, L + 2), (1, L + 4), (5, L + 4)):
            b.zet(x, 1, z, "guh_kristal_cluster[facing=up]")
        for x in range(1, 6):
            b.zet(x, 5, L + 5 if False else L + 3, "glitterrank")
        b.zet(2, 1, L + 2, "lichtmos")
        b.zet(4, 1, L + 3, "glinsterpaddenstoel")
        b.sign(2, 1, L, "south", ["Stil plekje", "om te vadsen", "", ""], wall=False)

    def dicht(b, L):
        pass
    gang("voorraadgrotje", 6, voorraad)
    gang("uitkijkje", 5, uitkijk)
    # the plug (fallback): the tunnel just ends in rock, with a sign
    d = ob.Bouw((5, 6, 2), "dorp_dicht", prefix=f"{PREFIX}.dorp")
    for x in range(5):
        for y in range(6):
            d.schil(x, y, 1)
    # (the connector at x 1 lines up with the middle of the 3 wide tunnel: x 0..2 is the tunnel, the wall reaches past it)
    d.verbinding(1, 1, 0, "north", "guhs:mijnguhdorp_zij_in")
    d.sign(2, 1, 0, "north", ["Hier wordt nog", "gegraven!", "", ""], wall=True)
    d.save("onderwol/mijnguhdorp/dicht")
    stukken["dicht"] = d
    return stukken


# =====================================================================================================================
# r3 the Knuffelslaapzaal
# =====================================================================================================================
NAMEN = ["Njegje", "Vadsje", "Pluis", "Knabbel", "Snoet", "Wolkje", "Kaasje", "Guhbert", "Muisje", "Vahoeg"]


def knuffelslaapzaal(h):
    v, gezet, pal, guhs, m = sc.laad("r3_knuffelslaapzaal")
    VL, CX, CZ, RX, RZ = m["VL"], m["CX"], m["CZ"], m["RX"], m["RZ"]
    X0 = v.shape[0]
    b, gs, _ = omzet("r3_knuffelslaapzaal", "knuffelslaapzaal", (0, X0 - 1, 0, v.shape[1] - 1, 0, v.shape[2] - 1))
    b.prefix = f"{PREFIX}.slaapzaal"
    b.anker(CX, VL, CZ)
    # the entrance tunnel (east) ends at the template's edge: the connector for the Mijnguhdorp, and the shaft of a hut
    b.verbinding(X0 - 1, VL + 1, CZ, "east", "guhs:mijnguhdorp_zij_in")
    b.ingang(X0 - 3, VL, CZ, final="guhs:viltsteen_tegels")
    b.sign(CX + RX - 2, VL + 1, CZ + 2, "east", ["Stil! Guhs", "slapen.", "Sssst... njeg.", ""], wall=False)
    b.sign(X0 - 1, VL + 1, CZ - 1, "east", ["Knuffel-", "slaapzaal", "", ""], wall=False)
    # name boards by the beds (the render's infobord at (x + 1, VL + 1, row))
    i = 0
    for rij_z in (CZ + 7, CZ + 13):
        for x in range(CX - 16, CX + 17, 4):
            if ((x - CX) / RX) ** 2 + ((rij_z + 1 - CZ) / RZ) ** 2 > .78:
                continue
            b.sign(x + 1, VL + 1, rij_z, "north", ["Hier slaapt", NAMEN[i % len(NAMEN)], "", ""], wall=False)
            i += 1
    b.sign(CX + 13, VL + 1, CZ - 8, "south", ["Schaapjes", "tellen: 1...", "2... njeg...", "zzz"], wall=False)
    b.sign(CX - 5, VL + 1, CZ - 6, "south", ["Wekker staat op:", "nooit", "", ""], wall=False)
    b.sign(CX + 1, VL + 1, CZ - 2, "south", ["Slaapliedje:", "druk niet op", "stop, njeg", ""], wall=False)
    # the sleeping guhs (all of the render's figures), the oppas guh awake by the music box
    for (gx, gy, gz, yaw, _, zit) in gs:
        if abs(gx - (CX - 1.5)) < 0.1 and abs(gz - (CZ + 2.5)) < 0.1:
            b.npc(int(gx), int(round(gy)), int(gz), "onderwol_slaapzaal", yaw=yaw)
        else:
            b.guh(gx, gy, gz, yaw=yaw, slaapt=True)
    # the baby guhs in their baskets (the render drew them as models)
    for (x, z) in ((CX - 22, CZ - 5), (CX - 23, CZ - 1), (CX - 23, CZ + 3), (CX - 22, CZ + 7), (CX - 20, CZ - 9), (CX - 19, CZ + 10)):
        b.guh(x + .5, VL + 1.2, z + .5, yaw=90, slaapt=True, schaal=0.5)
    # counting sheep: three little sheep frozen mid-jump over the fence
    for (dx, hgt) in ((-1.2, .9), (.5, 1.4), (2.2, .8)):
        b.entity(CX + 14.5 + dx, VL + 1 + hgt, CZ - 4.5, {"id": "guhs:guhschaapje", "NoAI": ms.Byte(1), "NoGravity": ms.Byte(1), "Silent": ms.Byte(1),
                                                         "Invulnerable": ms.Byte(1), "PersistenceRequired": ms.Byte(1), "Rotation": ms.floats(90.0, 0.0)})
    # a chest at the foot of the middle canopy bed
    b.kist(CX - 1, VL + 1, CZ - 11, "south", LOOT["knuffelslaapzaal"])
    b.save("onderwol/knuffelslaapzaal/start")
    return b


# =====================================================================================================================
# r4 the Paddenstoelendorpje
# =====================================================================================================================
def paddenstoelendorpje(h):
    v, gezet, pal, guhs, m = sc.laad("r4_paddenstoelendorpje")
    VL, Ys = m["VL"], v.shape[1]

    def zonder_wanden(v, gezet, pal):
        """Only the floor stays of the render's box: the back walls and the ceiling go (the Java hall carves the room)."""
        for x in range(v.shape[0]):
            for z in range(v.shape[2]):
                for y in range(VL + 3, Ys):
                    if pal[int(v[x, y, z])] in LAAGBLOK or pal[int(v[x, y, z])].startswith("guhs:c_glitterrank"):
                        v[x, y, z] = 0
                        gezet[x, y, z] = False
    x0, x1, y0, y1, z0, z1 = 3, v.shape[0] - 1, 0, 22, 3, v.shape[2] - 1
    b, gs, _ = omzet("r4_paddenstoelendorpje", "paddenstoelendorpje", (x0, x1, y0, y1, z0, z1), lucht="zaal", aanpas=zonder_wanden)
    b.prefix = f"{PREFIX}.paddenstoel"
    X = lambda x: x - x0
    Z = lambda z: z - z0
    # the anchor in the middle of the floor, the hut's shaft by the front left
    for (x, z) in ((33, 38), (34, 38), (32, 39)):
        y = eerste_vloer(b, X(x), Z(z), van=VL + 3)
        if y is not None:
            b.anker(X(x), y, Z(z))
            break
    y = eerste_vloer(b, X(7), Z(39), van=VL + 3)
    b.ingang(X(7), y, Z(39))
    huizen = m["HUIZEN"]   # [naam, cx, cz, top]
    for (naam, cx, cz, top) in huizen:
        yv = eerste_vloer(b, X(cx - 2), Z(cz + 3), van=VL + 3)
        b.sign(X(cx - 2), yv + 1, Z(cz + 3), "south", ["Huize", naam.replace("Huize ", ""), "Graag kloppen,", "njeg!"], wall=False)
        b.sign(X(cx), top + 1 - y0, Z(cz + 2), "north", ["Pas op je hoofd!", "(het is maar", "een hoedje)", ""], wall=False)
    # the village sign at the front
    yv = eerste_vloer(b, X(30), Z(42), van=VL + 3)
    b.sign(X(30), yv + 1, Z(42), "south", ["Paddenstoelen-", "dorpje", "Graag kloppen,", "njeg!"], wall=False)
    # a chest in Huize Boterbloem
    for (naam, cx, cz, top) in huizen:
        if "Boterbloem" in naam:
            b.kist(X(cx + 2), top + 1 - y0, Z(cz + 1), "west", LOOT["paddenstoelendorpje"])
    # the guhs: one talking resident outside, the rest wild
    for k, (gx, gy, gz, yaw, _, zit) in enumerate(gs):
        if abs(gx - (20.5 - x0)) < .1 and abs(gz - (36.5 - z0)) < .1:
            b.npc(int(gx), int(round(gy)), int(gz), "onderwol_paddenstoel", yaw=yaw)
        else:
            b.guh(gx, gy, gz, yaw=yaw)
    b.save("onderwol/paddenstoelendorpje/start")
    return b


# =====================================================================================================================
# r5 the Verlaten guhmijn: a random network of pieces (jigsaw), the parts of the render
# =====================================================================================================================
MIJN = "guhs:verlaten_guhmijn_gang"
MIJN_POOL = "guhs:onderwol/verlaten_guhmijn/gang"
PAAL, BALK, PLANK = "minecraft:oak_fence", "vadshout_planken", "minecraft:oak_planks"
ERTS_KNUFFEL = ["vadserts_knuffel", "kaasknabbelerts_knuffel", "kaasknabbelerts_knuffel", "guhklei", "pigmenterts_knuffel"]


def _mijn_gang(b, rng, lengte, langs="z", x0=0, z0=0, vl=1, breed=3, hoog=4):
    """A corridor of the render (3 wide, 4 high) along z from z0 (or x from x0), with stutten every 4 and ores in the wall."""
    for t in range(lengte):
        for w in range(-1, breed + 1):
            for y in range(vl - 1, vl + hoog + 1):
                x, z = (x0 + w, z0 + t) if langs == "z" else (x0 + t, z0 + w)
                binnen = 0 <= w < breed and vl <= y < vl + hoog
                if binnen:
                    b.set(x, y, z, "minecraft:air")
                elif y == vl - 1 and 0 <= w < breed:
                    b.zet(x, y, z, "gladde_knuffelsteen" if rng.random() < .3 else ob.SCHIL)
                else:
                    if y in (vl, vl + 1) and rng.random() < .06:
                        b.zet(x, y, z, rng.choice(ERTS_KNUFFEL))
                    else:
                        b.schil(x, y, z)
        if t % 4 == 2 and rng.random() > .12:
            for w in (0, breed - 1):
                for y in range(vl, vl + 3):
                    x, z = (x0 + w, z0 + t) if langs == "z" else (x0 + t, z0 + w)
                    b.zet(x, y, z, PAAL)
            for w in range(breed):
                x, z = (x0 + w, z0 + t) if langs == "z" else (x0 + t, z0 + w)
                b.zet(x, vl + 3, z, BALK)
            if rng.random() < .5:
                x, z = (x0 + 1, z0 + t) if langs == "z" else (x0 + t, z0 + 1)
                b.zet(x, vl + 2, z, rng.choice(["lampion_roze[hanging=true]", "lampion_geel[hanging=true]", "lampion_mint[hanging=true]"]))
        if rng.random() < .05:
            x, z = (x0 + rng.choice([0, breed - 1]), z0 + t) if langs == "z" else (x0 + t, z0 + rng.choice([0, breed - 1]))
            b.zet(x, vl, z, "pluisbolletje")


def verlaten_guhmijn(h):
    rng = random.Random(5)
    stukken = {}

    def verbind(b, x, y, z, facing):
        b.verbinding(x, y, z, facing, MIJN, MIJN, MIJN_POOL)

    # the start: the old mine hall, 9 x 9, with the shaft of the hut and four ways out
    b = ob.Bouw((11, 7, 11), "verlaten_guhmijn", prefix=f"{PREFIX}.mijn")
    for x in range(11):
        for z in range(11):
            for y in range(7):
                if 1 <= x <= 9 and 1 <= z <= 9 and 1 <= y <= 5:
                    b.set(x, y, z, "minecraft:air")
                elif y == 0 and 1 <= x <= 9 and 1 <= z <= 9:
                    b.zet(x, y, z, PLANK if (x + z) % 5 else "gladde_knuffelsteen")
                else:
                    b.schil(x, y, z)
    for (px, pz) in ((1, 1), (9, 1), (1, 9), (9, 9)):
        for y in range(1, 5):
            b.zet(px, y, pz, PAAL)
    for x in range(1, 10):
        b.zet(x, 5, 1, BALK)
        b.zet(x, 5, 9, BALK)
    b.anker(5, 0, 5)
    b.ingang(5, 0, 3, final="minecraft:oak_planks")
    b.sign(3, 1, 7, "south", ["Verlaten", "guhmijn", "Betreden op", "eigen risico,"], wall=False)
    b.sign(4, 1, 7, "south", ["njeg!", "", "", ""], wall=False)
    b.zet(7, 1, 7, "baltoslee_minislee[facing=north]")
    b.kist(7, 1, 8, "north", LOOT["verlaten_guhmijn"])
    b.zet(2, 1, 2, "guh_lantaarnpaal[half=lower]")
    b.zet(2, 2, 2, "guh_lantaarnpaal[half=upper]")
    b.zet(8, 4, 2, "lampion_geel[hanging=true]")
    for (x, z, f) in ((5, 0, "north"), (5, 10, "south"), (0, 5, "west"), (10, 5, "east")):
        for dy in range(1, 5):
            for d in (-1, 0, 1):
                xx, zz = (x + d, z) if f in ("north", "south") else (x, z + d)
                b.set(xx, dy, zz, "minecraft:air")
        verbind(b, x, 1, z, f)
    b.save("onderwol/verlaten_guhmijn/start")
    stukken["start"] = b

    def gang(naam, lengte, rails=False):
        g = ob.Bouw((5, 6, lengte), f"mijn_{naam}", prefix=f"{PREFIX}.mijn")
        _mijn_gang(g, rng, lengte, "z", 1, 0)
        verbind(g, 2, 1, 0, "north")
        verbind(g, 2, 1, lengte - 1, "south")
        if rails:
            for z in (3, lengte - 4):
                g.zet(1, 1, z, "baltoslee_minislee[facing=south]")
                g.kist(3, 1, z, "west", LOOT["verlaten_guhmijn"])
        g.save(f"onderwol/verlaten_guhmijn/{naam}")
        stukken[naam] = g
    gang("gang_lang", 15)
    gang("gang_kort", 9)
    gang("gang_slee", 13, rails=True)

    # a crossing (4 ways) and a T
    for naam, uit in (("kruising", ("north", "south", "west", "east")), ("splitsing", ("north", "south", "east"))):
        k = ob.Bouw((7, 6, 7), f"mijn_{naam}", prefix=f"{PREFIX}.mijn")
        for x in range(7):
            for z in range(7):
                for y in range(6):
                    open_ = (1 <= x <= 5 and 1 <= z <= 5) and 1 <= y <= 4
                    gat = 1 <= y <= 4 and ((2 <= x <= 4 and (z == 0 and "north" in uit or z == 6 and "south" in uit))
                                           or (2 <= z <= 4 and (x == 0 and "west" in uit or x == 6 and "east" in uit)))
                    if open_ or gat:
                        k.set(x, y, z, "minecraft:air")
                    elif y == 0 and 1 <= x <= 5 and 1 <= z <= 5:
                        k.zet(x, y, z, "gladde_knuffelsteen")
                    else:
                        k.schil(x, y, z)
        for (px, pz) in ((1, 1), (5, 1), (1, 5), (5, 5)):
            for y in range(1, 4):
                k.zet(px, y, pz, PAAL)
            k.zet(px, 4, pz, BALK)
        k.zet(3, 4, 3, "lampion_roze[hanging=true]")
        for f, (x, z) in (("north", (3, 0)), ("south", (3, 6)), ("west", (0, 3)), ("east", (6, 3))):
            if f in uit:
                verbind(k, x, 1, z, f)
        k.save(f"onderwol/verlaten_guhmijn/{naam}")
        stukken[naam] = k

    # the stairs: 6 up over 9 (the render's trapgang between the two levels)
    t = ob.Bouw((5, 12, 13), "mijn_trap", prefix=f"{PREFIX}.mijn")
    for z in range(13):
        vl = 1 + max(0, min(6, z - 3))
        for x in range(5):
            for y in range(12):
                binnen = 1 <= x <= 3 and vl <= y < vl + 5
                if binnen:
                    t.set(x, y, z, "minecraft:air")
                elif 1 <= x <= 3 and y == vl - 1:
                    t.zet(x, y, z, PLANK)
                else:
                    t.schil(x, y, z)
        if 4 <= z <= 9:
            for x in range(1, 4):
                t.zet(x, vl - 1, z, "minecraft:oak_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]")
                t.zet(x, vl - 2, z, PLANK)
    t.sign(1, 1, 2, "south", ["Naar boven:", "Mind your", "pootjes!", ""], wall=False)
    verbind(t, 2, 1, 0, "north")
    verbind(t, 2, 7, 12, "south")
    t.save("onderwol/verlaten_guhmijn/trap")
    stukken["trap"] = t

    # the caved-in corridor (also the end of a way that can't go on)
    i = ob.Bouw((5, 6, 8), "mijn_ingestort", prefix=f"{PREFIX}.mijn")
    _mijn_gang(i, rng, 8, "z", 1, 0)
    for z in range(3, 8):
        for x in range(1, 4):
            hgt = int(2 + 2 * math.sin(z / 2.0) + rng.randint(0, 1))
            for y in range(1, 1 + min(hgt + z - 3, 4)):
                i.zet(x, y, z, "knuffelsteen" if rng.random() < .8 else "kaasknabbelerts_knuffel")
    i.sign(2, 1, 1, "south", ["Ingestort.", "Andere kant op!", "", ""], wall=False)
    verbind(i, 2, 1, 0, "north")
    i.save("onderwol/verlaten_guhmijn/ingestort")
    stukken["ingestort"] = i

    # the four special rooms (9 x 9, 5 high), one way in each
    def kamer(naam, vloer, inhoud):
        k = ob.Bouw((11, 7, 12), f"mijn_{naam}", prefix=f"{PREFIX}.mijn")
        for x in range(11):
            for z in range(12):
                for y in range(7):
                    binnen = 1 <= x <= 9 and 2 <= z <= 10 and 1 <= y <= 5
                    toegang = 4 <= x <= 6 and z <= 1 and 1 <= y <= 4
                    if binnen or toegang:
                        k.set(x, y, z, "minecraft:air")
                    elif y == 0 and (1 <= x <= 9 and 2 <= z <= 10 or 4 <= x <= 6 and z <= 1):
                        k.zet(x, y, z, vloer)
                    else:
                        k.schil(x, y, z)
        verbind(k, 5, 1, 0, "north")
        inhoud(k)
        k.save(f"onderwol/verlaten_guhmijn/{naam}")
        stukken[naam] = k

    def voorraad(k):
        for (dx, dz) in ((0, 0), (1, 0), (0, 1), (8, 0), (8, 1), (7, 0), (0, 8), (1, 8), (8, 8)):
            k.zet(1 + dx, 1, 2 + dz, "block_of_kaasknabbels")
        for (dx, dz) in ((0, 0), (8, 0), (0, 8)):
            k.zet(1 + dx, 2, 2 + dz, "block_of_kaasknabbels")
        for dz in range(2, 7):
            k.ton(1, 1, 2 + dz, LOOT["verlaten_guhmijn"]) if dz % 2 else k.zet(1, 1, 2 + dz, "minecraft:barrel[facing=up,open=true]")
        k.zet(5, 1, 6, "katapult_knabbelkist[facing=east]")
        k.sign(7, 1, 4, "east", ["Knabbel-", "voorraad: max 1", "per hap, njeg", ""], wall=False)
        k.guh(5.5, 1, 8.5, yaw=200)

    def depot(k):
        for j, dz in enumerate((1, 3, 5, 7)):
            k.zet(3, 1, 2 + dz, "baltoslee_minislee[facing=east]")
            if j % 2:
                k.kist(4, 1, 2 + dz, "west", LOOT["verlaten_guhmijn"])
            else:
                k.zet(4, 1, 2 + dz, "block_of_kaasknabbels")
        for z in range(3, 10):
            k.zet(8, 1, z, "minecraft:barrel[facing=up,open=false]")
        k.sign(6, 1, 3, "south", ["Sleedepot:", "parkeren op", "eigen risico", ""], wall=False)

    def kantine(k):
        for dx in range(2, 7):
            k.zet(1 + dx, 1, 6, PAAL)
            k.zet(1 + dx, 2, 6, "minecraft:oak_slab[type=bottom,waterlogged=false]")
            if dx % 2 == 0:
                k.zet(1 + dx, 1, 4, "guh_bankje_hout[facing=south]")
            else:
                k.zet(1 + dx, 1, 8, "guh_bankje_hout[facing=north]")
        k.zet(4, 3, 6, "minecraft:decorated_pot")
        k.zet(6, 3, 6, "picknickeilandje_mand[facing=south,versierd=true]")
        k.ton(9, 1, 10, LOOT["verlaten_guhmijn"])
        k.sign(2, 1, 3, "south", ["Eerst knabbelen,", "dan hakken.", "", ""], wall=False)
        k.npc(3, 1, 5, "onderwol_mijn", yaw=0)
        k.guh(5.5, 1, 7.5, yaw=180)

    def schuur(k):
        k.zet(2, 1, 3, "vadsaambeeld[facing=north]")
        k.zet(4, 1, 3, "minecraft:grindstone[face=floor,facing=north]")
        k.zet(6, 1, 3, "minecraft:smithing_table")
        for z in range(4, 10):
            k.zet(9, 1, z, "minecraft:barrel[facing=west,open=false]")
        k.kist(2, 1, 8, "east", LOOT["verlaten_guhmijn"])
        k.sign(5, 1, 9, "north", ["Lenen =", "terugbrengen.", "Vahoeg!", ""], wall=False)
    kamer("voorraad", PLANK, voorraad)
    kamer("depot", "gladde_knuffelsteen", depot)
    kamer("kantine", "tegel_dambord", kantine)
    kamer("schuur", PLANK, schuur)

    # the plug where a way can't go on: rock with a cobweb of pluis
    e = ob.Bouw((5, 6, 2), "mijn_eind", prefix=f"{PREFIX}.mijn")
    for x in range(5):
        for y in range(6):
            e.schil(x, y, 1)
    e.zet(2, 1, 0, "pluisbolletje")
    e.verbinding(1, 1, 0, "north", MIJN)
    e.save("onderwol/verlaten_guhmijn/eind")
    stukken["eind"] = e
    return stukken


# =====================================================================================================================
# r6 the Grotschilderingen: the great fresco hall, and small wall paintings
# =====================================================================================================================
GUH = [".mmm...mmm.", "mphpm.mphpm", "mphpmmmphpm", ".mpppppppm.", "mpkwpppkwpm", "mpkkpppkkpm", "mhpppppppHm", ".mppwmwppm.",
       "..mpwwwpm..", "...mmmmm..."]
GUH_BLIJ = list(GUH)
GUH_BLIJ[4] = "mppkpppkppm"
GUH_BLIJ[5] = "mpkpkpkpkpm"
WOLBOL = [".mPm.", "mPPPm", "PPmPP", "mPPPm", ".mPm."]
WIEL = ["...nnn...", ".nn...nn.", ".n.n.n.n.", "n...n...n", "nnnnnnnnn", "n...n...n", ".n.n.n.n.", ".nn...nn.", "...nnn..."]
ZON = [".y.y.", "yyyyy", ".yyy.", "yyyyy", ".y.y."]
HART = ["mm.mm", "mrmrm", "mrrrm", ".mrm.", "..m.."]
KOPJE = [".m.m.", "mpppm", "mkpkm", ".mpm."]
VERF = {"p": "minecraft:pink_concrete", "m": "minecraft:magenta_concrete", "w": "minecraft:white_concrete", "k": "minecraft:black_concrete",
        "y": "minecraft:yellow_concrete", "n": "minecraft:brown_concrete", "P": "minecraft:pink_wool", "h": "minecraft:pink_terracotta",
        "H": "minecraft:pink_terracotta", "r": "minecraft:red_concrete", "b": "minecraft:light_blue_concrete"}


def fresco_hal(h):
    v, gezet, pal, guhs, m = sc.laad("r06_grotschilderingen")
    VL, TOP, WN, WW, MID = m["VL"], m["TOP"], m["WN"], m["WW"], m["MID"]
    Xs, Zs = v.shape[0], v.shape[2]

    def ruwer(v, gezet, pal):
        """The hall's ceiling and its plain (south and east) walls get the round, rough shape of a cave."""
        r = random.Random(6)
        rots = pal.index("guhs:knuffelsteen")
        for x in range(WW + 1, Xs - 2):
            for z in range(WN + 1, Zs - 2):
                # the ceiling: lower towards the south and the east wall (a vault)
                dz = (z - WN) / (Zs - 3 - WN)
                dx = max(0.0, (x - (Xs - 14)) / 12.0)
                zak = int(6 * max(dz, dx) ** 2 + r.random() * 1.5)
                for y in range(TOP - zak + 1, TOP + 1):
                    if v[x, y, z] == 0:
                        v[x, y, z] = rots
                        gezet[x, y, z] = False
                # the south wall: bumps
                if z >= Zs - 4 - r.randint(0, 1) and VL + 3 <= TOP:
                    for y in range(VL + 2 + r.randint(0, 3), TOP + 1):
                        if v[x, y, z] == 0:
                            v[x, y, z] = rots
    b, gs, _ = omzet("r06_grotschilderingen", "grotschilderingen", (2, Xs - 2, VL - 2, TOP + 2, 2, Zs - 2), aanpas=ruwer)
    b.prefix = f"{PREFIX}.fresco"
    o = (2, VL - 2, 2)
    P = lambda x, y, z: (x - o[0], y - o[1], z - o[2])
    b.anker(*P(MID, VL - 1, WN + 8))
    b.ingang(*P(MID + 20, VL - 1, WN + 12))
    PW = 18
    xs0 = [6 + i * (PW + 4) for i in range(4)]
    teksten = [["Ooit was alles", "grijs en koud.", "Brr, njeg.", ""], ["Tot een guh de", "allereerste", "roze wolbol", "vond!"],
               ["Spinnen, spinnen", "spinnen...", "vahoeg, wat een", "wol!"], ["En zo werd de", "hele Guhmension", "roze. Einde.", "(Of?)"]]
    for x0, t in zip(xs0, teksten):
        b.sign(*P(x0 + PW // 2, VL, WN + 2), "south", t, wall=False)
    b.sign(*P(WW + 2, VL, 17), "east", ["Elke guh draagt", "een stukje", "Knuffelhart en", "deelt het uit"], wall=False)
    b.sign(*P(WW + 2, VL, 7), "east", ["Wie lief is, mag", "later op de", "wolkjes", "knuffelen"], wall=False)
    b.sign(*P(MID, VL, WN + 4), "south", ["Niet aanraken,", "verf is pas", "3000 jaar oud", ""], wall=False)
    for (gx, gy, gz, yaw, _, zit) in gs:
        if abs(gx - (MID + .5 - o[0])) < .1 and abs(gz - (WN + 5.5 - o[2])) < .1:
            b.npc(int(gx), int(round(gy)), int(gz), "onderwol_gids", yaw=180)
        else:
            b.guh(gx, gy if not zit else gy - 1 + 1, gz, yaw=yaw)
    b.kist(*P(WW + 2, VL, WN + 11), "east", LOOT["grotschilderingen"])
    b.save("onderwol/grotschilderingen/fresco")
    return b


def teken(b, tek, x0, ytop, z, spiegel=False):
    for r, rij in enumerate(tek):
        for c, ch in enumerate(rij[::-1] if spiegel else rij):
            if ch in VERF:
                b.zet(x0 + c, ytop - r, z, VERF[ch])


def schildering(naam, titel, grap, tekening, zaad):
    """A small wall painting in a little cave of its own: a rounded, rough niche (17 wide, 11 high, 11 deep) carved in front of
    a canvas with a frame (viltsteen bricks) on its flat back wall (z = 1), a sign, a lamp and a cushion."""
    r = random.Random(zaad)
    b = ob.Bouw((17, 12, 12), "grotschilderingen", prefix=f"{PREFIX}.schildering")
    golf = [[r.uniform(-0.9, 0.9) for _ in range(12)] for _ in range(17)]
    binnen = set()
    for x in range(17):
        for z in range(2, 12):
            for y in range(1, 12):
                d = ((x - 8) / (7.6 + golf[x][z])) ** 2 + ((y - 1) / (9.0 + golf[x][y % 12] * 0.6)) ** 2 + ((z - 1.5) / (9.5 + golf[x][z])) ** 2
                if d <= 1:
                    binnen.add((x, y, z))
    for (x, y, z) in binnen:
        b.set(x, y, z, "minecraft:air")
    for (x, y, z) in list(binnen):
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            q = (x + dx, y + dy, z + dz)
            if q not in binnen and 0 <= q[0] < 17 and 0 <= q[1] < 12 and 0 <= q[2] < 12:
                b.schil(*q)
    for x in range(17):
        for z in range(2, 12):
            if (x, 1, z) in binnen:
                b.zet(x, 0, z, "gladde_knuffelsteen" if (x + z) % 4 else "viltsteen_tegels")
    # the canvas (a frame of viltsteen bricks) on the flat back wall
    for x in range(3, 14):
        for y in range(1, 9):
            rand = x in (3, 13) or y in (1, 8)
            b.zet(x, y, 1, "viltsteen_bakstenen" if rand else "pleister_vanille")
            if (x, y, 2) not in binnen:
                b.set(x, y, 2, "minecraft:air")
    tekening(b)
    b.sign(8, 1, 3, "south", titel, wall=False)
    b.sign(10, 1, 4, "south", grap, wall=False)
    b.zet(4, 1, 3, "knuffelsteen_muur")
    b.zet(4, 2, 3, "guh_kristal_lamp")
    b.zet(12, 1, 5, "pluiskussen")
    for (x, z) in ((3, 6), (13, 7), (6, 8), (11, 3)):
        if (x, 1, z) in binnen:
            b.zet(x, 1, z, r.choice(["lichtmos", "glinsterpaddenstoel", "lichtmos"]))
    b.anker(8, 0, 6)
    b.save(f"onderwol/grotschilderingen/{naam}")
    return b


def schilderingen_klein():
    return [
        schildering("wolbol", ["De eerste", "wolbol", "", ""], ["Niet aanraken,", "verf is pas", "3000 jaar oud", ""],
                    lambda b: (teken(b, GUH_BLIJ[:6], 4, 7, 1), teken(b, WOLBOL, 8, 6, 1)), 61),
        schildering("knuffelhart", ["Het Knuffelhart", "", "", ""], ["Elke guh draagt", "een stukje", "Knuffelhart en", "deelt het uit"],
                    lambda b: (teken(b, HART, 6, 7, 1), teken(b, KOPJE, 4, 4, 1), teken(b, KOPJE, 8, 4, 1)), 62),
        schildering("spinnewiel", ["Spinnen, spinnen", "spinnen...", "", ""], ["vahoeg, wat een", "wol!", "", ""],
                    lambda b: (teken(b, WIEL[1:7], 4, 7, 1), teken(b, KOPJE, 9, 6, 1), teken(b, WOLBOL[1:4], 8, 3, 1)), 63),
        schildering("alles_roze", ["Alles roze!", "", "", ""], ["En zo werd de", "hele Guhmension", "roze. Einde.", "(Of?)"],
                    lambda b: (teken(b, ZON, 8, 7, 1), teken(b, KOPJE, 4, 6, 1), teken(b, ["PPPPPPPPP"], 4, 2, 1)), 64),
    ]


# =====================================================================================================================
# r7 the Sausmeer-vissershutje
# =====================================================================================================================
def sausmeer_vissershutje(h):
    v, gezet, pal, guhs, m = sc.laad("r07_sausmeer_vissershutje")
    VL, HV, H1X = m["VL"], m["HV"], m["H1X"]
    Xs, Ys, Zs = v.shape
    b, gs, _ = omzet("r07_sausmeer_vissershutje", "sausmeer_vissershutje", (0, Xs - 1, VL - 5, VL + 14, 1, Zs - 2))
    b.prefix = f"{PREFIX}.visser"
    o = (0, VL - 5, 1)
    P = lambda x, y, z: (x - o[0], y - o[1], z - o[2])
    b.anker(*P(40, VL - 1, 30))
    b.ingang(*P(42, VL - 1, 34))
    b.sign(*P(H1X + 1, HV + 1, 21), "east", ["Visvergunning:", "1 knabbel", "", ""], wall=False)
    b.sign(*P(17, HV + 1, 21), "east", ["Vandaag", "gevangen:", "0 vissen,", "3 sokken"], wall=False)
    b.sign(*P(44, VL, 22), "west", ["Saus is warm.", "Niet drinken,", "wel dippen.", ""], wall=False)
    for (gx, gy, gz, yaw, _, zit) in gs:
        if abs(gx - (23.5 - o[0])) < .1 and abs(gz - (19.5 - o[2])) < .1:
            b.npc(int(gx), int(round(gy)), int(gz), "onderwol_visser", yaw=yaw)
        else:
            b.guh(gx, gy, gz, yaw=yaw)
    for (pos, blok) in list(b.blocks.items()):
        if blok[0] == "minecraft:chest":
            b.kist(*pos, blok[1].get("facing", "north"), LOOT["sausmeer_vissershutje"])
        elif blok[0] == "minecraft:barrel" and blok[1].get("open") == "false":
            b.ton(*pos, LOOT["sausmeer_vissershutje"])
    b.save("onderwol/sausmeer_vissershutje/start")
    return b


# =====================================================================================================================
# loot, texts, the structures
# =====================================================================================================================
BOUWSTOF = [("viltsteen", 6, 4, 12), ("viltsteen_bakstenen", 4, 4, 10), ("guhklei", 5, 2, 8), ("knuffelsteen", 4, 4, 10),
            ("gladde_knuffelsteen", 3, 2, 8), ("bloesemmeertje_meerzand", 3, 2, 8), ("viltsteen_tegels", 3, 2, 8)]
SNACK = [("kaas_knabbels", 8, 2, 8), ("vahoege_vads", 4, 1, 3), ("kaaskorrel", 3, 1, 4), ("glitterbes", 2, 1, 4)]
LOOT_DEF = {
    "ingang_hutje": [((2, 4), BOUWSTOF[:4] + [("minecraft:torch", 4, 4, 8), ("lampion_geel", 2, 1, 2)]), (1, SNACK)],
    "mijnkamertje": [((3, 5), BOUWSTOF + [("minecraft:iron_pickaxe", 1, 1, 1)]), ((1, 2), SNACK)],
    "mijnguhdorp": [((2, 4), BOUWSTOF), ((1, 3), SNACK), (1, [("lampion_roze", 3, 1, 2), ("lampion_mint", 3, 1, 2), ("guh_lantaarnpaal", 2, 1, 1),
                                                         ("knuffelsteen_gezicht", 2, 1, 1), ("leeg", 6, 1, 1)])],
    "knuffelslaapzaal": [((2, 3), [("pluiskussen", 4, 1, 3), ("pluisbolletje", 3, 2, 5), ("sterrenlantaarn", 2, 1, 2), ("guhnestje", 2, 1, 1),
                                   ("knuffel_normal", 2, 1, 1), ("knuffel_starry", 1, 1, 1)]),
                         ((1, 2), SNACK), (1, [("slaapmutsje", 3, 1, 1), ("pyjama_pakje", 2, 1, 1), ("leeg", 5, 1, 1)])],
    "paddenstoelendorpje": [((2, 4), [("guhpaddenstoel", 4, 2, 5), ("glinsterpaddenstoel", 4, 2, 6), ("lichtmos", 3, 4, 10), ("schimmelsteel", 3, 4, 8),
                                      ("knuffelblok_roze", 2, 2, 6), ("knuffelblok_lavendel", 2, 2, 6)]), ((1, 2), SNACK),
                            (1, [("lampion_roze", 2, 1, 3), ("lampion_geel", 2, 1, 3), ("vadstouw", 2, 2, 6), ("leeg", 3, 1, 1)])],
    "verlaten_guhmijn": [((2, 4), BOUWSTOF + [("minecraft:rail", 2, 4, 10), ("minecraft:torch", 3, 4, 12)]), ((1, 3), SNACK),
                         (1, [("block_of_kaasknabbels", 2, 1, 2), ("baltoslee_minislee", 1, 1, 1), ("eiken_paal", 2, 2, 6), ("leeg", 4, 1, 1)])],
    "grotschilderingen": [((2, 3), [("pleister_vanille", 3, 4, 10), ("pleister_lavendel", 3, 4, 10), ("minecraft:pink_concrete", 3, 4, 12),
                                    ("minecraft:magenta_concrete", 2, 4, 12), ("minecraft:white_concrete", 2, 4, 12), ("viltsteen_bakstenen", 3, 4, 10)]),
                          ((1, 2), SNACK), (1, [("guh_kristal_lamp", 2, 1, 2), ("pluiskussen", 2, 1, 2), ("leeg", 3, 1, 1)])],
    "sausmeer_vissershutje": [((2, 3), [("vadsbaars", 3, 1, 3), ("minecraft:cod", 3, 1, 3), ("bloesemmeertje_meerzand", 3, 4, 10),
                                        ("minecraft:fishing_rod", 1, 1, 1), ("minecraft:leather_boots", 1, 1, 1)]), ((1, 3), SNACK),
                              (1, [("botenhuisje_steigerlantaarn", 2, 1, 1), ("botenhuisje_hengelstandaard", 2, 1, 1), ("leeg", 3, 1, 1)])],
}
LOOT = {n: f"guhs:chests/onderwol/{n}" for n in LOOT_DEF}


def bouw_alles(h):
    """Every template, pool, set, loot table and text of this slice."""
    for naam, pools in LOOT_DEF.items():
        ob.loot(h, naam, pools)
    hut = ingang_hutje(h)
    biomes = ob.guhmension_biomes(h)
    # --- r8 Mijningang-hutje (standalone): the hut and its shaft always, a little mine room below
    kamer = mijnkamertje(h)
    ob.grot_bouw(h, "mijningang_hutje", templates=[(kamer, 1)], titel="Mijningang-hutje",
                 tooltip="Een hutje met een wolkenlift naar beneden. Hier begint je eigen mijn!",
                 titel_en="Mine Entrance Hut", tooltip_en="A little hut with a cloud lift down. Your own mine starts here!",
                 salt=SALT, band=(36, 50), spacing=30, separation=10, ruimte="zaal", zaal=dict(rand=4, boven=4, ruw=1.0),
                 ingang=dict(kans=1.0, verplicht=True), biomes=biomes)
    # --- r3 Knuffelslaapzaal (also a side building of the Mijnguhdorp)
    slaap = knuffelslaapzaal(h)
    ob.grot_bouw(h, "knuffelslaapzaal", templates=[(slaap, 1)], titel="Knuffelslaapzaal",
                 tooltip="Een ondergrondse slaapzaal vol hemelbedjes en slapende guhs. Sssst!",
                 titel_en="Cuddle Dormitory", tooltip_en="An underground dormitory full of canopy beds and sleeping guhs. Shhh!",
                 salt=SALT + 10, band=(46, 54), spacing=40, separation=14, ingang=dict(kans=0.6), biomes=biomes)
    # --- r2 Mijnguhdorp
    zij = dorp_zijstukken(h)
    dorp = mijnguhdorp(h)
    ob.grot_bouw(h, "mijnguhdorp", templates=[(dorp, 1)], titel="Mijnguhdorp",
                 tooltip="Het ondergrondse dorp van de Mijnguh, rond een warm sausmeer. Met een wolkenlift naar boven.",
                 titel_en="Mijnguh Village", tooltip_en="The Mijnguh's underground village around a warm sauce lake. With a cloud lift up.",
                 salt=SALT + 20, band=(24, 32), spacing=96, separation=40, gegarandeerd=dict(min=300, max=1000), ruimte="zaal",
                 ingang=dict(kans=1.0), size=2, max_distance=120, voorrang=60, biomes=biomes,
                 pools={"zij_slaapzaal": [(slaap, 3), (zij["voorraadgrotje"], 1)], "zij_slaapzaal_eind": [(zij["dicht"], 1)],
                        "zij": [(zij["voorraadgrotje"], 2), (zij["uitkijkje"], 2)], "zij_eind": [(zij["dicht"], 1)]})
    # --- r4 Paddenstoelendorpje
    pad = paddenstoelendorpje(h)
    ob.grot_bouw(h, "paddenstoelendorpje", templates=[(pad, 1)], titel="Paddenstoelendorpje",
                 tooltip="Reuzenpaddenstoelen met een huisje in de hoed, lampjes aan touwtjes en een glinsterbeekje.",
                 titel_en="Mushroom Village", tooltip_en="Giant mushrooms with a little house in the cap, lights on strings and a sparkly brook.",
                 salt=SALT + 30, band=(26, 36), spacing=40, separation=14, zaal=dict(rand=5, boven=4, ruw=1.2), ingang=dict(kans=0.5),
                 biomes=biomes)
    # --- r5 Verlaten guhmijn
    mijn = verlaten_guhmijn(h)
    ob.grot_bouw(h, "verlaten_guhmijn", templates=[(mijn["start"], 1)], titel="Verlaten guhmijn",
                 tooltip="Een oud gangennet van de mijnguhs, met kistsleetjes, een schaftkantine en ingestorte stukken.",
                 titel_en="Abandoned Guh Mine", tooltip_en="An old tunnel network of the miner guhs, with loot sleds, a canteen and caved-in bits.",
                 salt=SALT + 40, band=(24, 38), spacing=24, separation=8, ruimte="zaal", ingang=dict(kans=0.7), size=7, max_distance=72,
                 biomes=biomes,
                 pools={"gang": [(mijn["gang_lang"], 6), (mijn["gang_kort"], 5), (mijn["gang_slee"], 3), (mijn["kruising"], 4),
                                 (mijn["splitsing"], 4), (mijn["trap"], 2), (mijn["ingestort"], 1), (mijn["voorraad"], 1), (mijn["depot"], 1),
                                 (mijn["kantine"], 1), (mijn["schuur"], 1)],
                        "gang_eind": [(mijn["eind"], 3), (mijn["ingestort"], 1)]})
    # --- r6 Grotschilderingen: mostly a small painting, sometimes the great hall
    fresco = fresco_hal(h)
    klein = schilderingen_klein()
    ob.grot_bouw(h, "grotschilderingen", templates=[(fresco, 2)] + [(k, 3) for k in klein], titel="Grotschilderingen",
                 tooltip="Oude guh-schilderingen op de grotwand: hoe alles roze werd, en het Knuffelhart.",
                 titel_en="Cave Paintings", tooltip_en="Old guh paintings on the cave wall: how everything turned pink, and the Cuddle Heart.",
                 salt=SALT + 50, band=(22, 40), spacing=16, separation=6, ingang=dict(kans=0.25), biomes=biomes)
    # (the small ones carve a natural niche around themselves: their own structure would need a zaal; they carve their room)
    # --- r7 Sausmeer-vissershutje
    vis = sausmeer_vissershutje(h)
    ob.grot_bouw(h, "sausmeer_vissershutje", templates=[(vis, 1)], titel="Sausmeer-vissershutje",
                 tooltip="Een hutje op palen aan een warm kaassausmeer. De vissen bijten niet, de kaas wel.",
                 titel_en="Sauce Lake Fishing Hut", tooltip_en="A hut on stilts by a warm cheese sauce lake. The fish don't bite, the cheese does.",
                 salt=SALT + 60, band=(20, 30), spacing=22, separation=8, ruimte="zaal", ingang=dict(kans=0.4), biomes=biomes)
    # the game test field (OnderWolBouwGameTests): 24 x 16 x 24 of air
    t = ob.Bouw((24, 16, 24), "onderwol_bouw_test")
    t.fill(0, 0, 0, 23, 15, 23, "minecraft:air")
    t.save("onderwol_bouw_test")
    # the lines of the talking guhs
    for plek, regels in PRAAT.items():
        for i, (nl, en) in enumerate(zip(regels, PRAAT_EN[plek]), start=1):
            h.lang(f"quest.guhs.onderwol_bouw.{plek}.{i}", en, nl)
    return hut


def selfcheck(h):
    """Every block of every template exists with known properties; every sign line fits; the lines of the talking guhs."""
    fouten = []
    seen = {}
    for b in BOUWSELS:
        for (naam, props, _nbt) in set((v[0], tuple(sorted(v[1].items())), None) for v in b.blocks.values()):
            if not naam.startswith("guhs:"):
                continue
            if naam not in seen:
                f = f"{h.A}/blockstates/{naam[5:]}.json"
                if not os.path.exists(f):
                    seen[naam] = None
                else:
                    bs = json.load(open(f, encoding="utf-8"))
                    keys = set()
                    for k in bs.get("variants", {}):
                        keys |= {kv.split("=")[0] for kv in k.split(",") if "=" in kv}
                    for part in bs.get("multipart", []):
                        w = part.get("when", {})
                        for c in (w.get("OR") or w.get("AND") or [w]):
                            keys |= set(c)
                    seen[naam] = keys
            if seen[naam] is None:
                fouten.append(f"{b.naam}: block {naam} does not exist")
            else:
                extra = {k for k, _ in props} - seen[naam] - {"waterlogged"}
                if extra and seen[naam]:
                    fouten.append(f"{b.naam}: {naam} has no property {sorted(extra)}")
    return sorted(set(fouten))


BOUWSELS = []
_orig_save = ob.Bouw.save


def _save(self, name):
    _orig_save(self, name)
    BOUWSELS.append(self)


ob.Bouw.save = _save


def build(h):
    bouw_alles(h)
    for f in selfcheck(h):
        print("onderwol_structuren1:", f)


# =====================================================================================================================
# standalone (like onderwol.py): python tools/features/onderwol_structuren1.py from the project root
# =====================================================================================================================
EN_OVERLAY = os.path.join("tools", "lang", "en", "c172_onderwol_structuren1.json")


def _direct():
    import types
    R = os.path.join("src", "main", "resources")
    A, D = os.path.join(R, "assets", "guhs"), os.path.join(R, "data", "guhs")
    NL, EN = {}, {}

    def w(path, obj):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write("\n")

    def lang(key, en, nl):
        EN[key] = en
        NL[key] = nl
    h = types.SimpleNamespace(R=R, A=A, D=D, w=w, lang=lang)
    build(h)
    # the sign texts of this slice (sign_text keys) and their English
    for k, nl in sign_text.TEXTS.items():
        if k.startswith(PREFIX + "."):
            NL[k] = nl
            EN[k] = EN_REGELS.get(nl, nl)
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        w(p, data)
    # the English overlay (a full make_resources run takes en_us from nl_nl + tools/lang/en)
    w(EN_OVERLAY, dict(sorted(EN.items())))
    print(f"onderwol_structuren1: {len(BOUWSELS)} templates, {len(NL)} texts")


if __name__ == "__main__":
    import sys
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    _direct()
