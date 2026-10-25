"""
1.7.0 "Onder de Wol", structures part 2 (design: MineScapeStory/ontwerp-guhs170-mijnen.md chapter 2.4 and rounds 4-20; Java:
feature/onderwol/bouw/OnderWolStructuren2; framework: features/onderwol_bouw.py): eight "af en toe" buildings under the
ground, made from the design renders (frozen scenes in features/onderwol_bouw_bron, see onderwol_bouw_scene.py):

    verlaten_sledestation  r09  a vaulted station hall with two platforms, old rails and sleds, the ticket office
    kaasmakerij            r10  Kaasmakerij De Gouden Saus (v2): saus lake, pumps, vats, giant cheese wheels, the mascot
                                (also a side building of the Mijnguhdorp: pool guhs:onderwol/mijnguhdorp/zij)
    verzonken_guhhuisje    r11  Huize Pluis (v2): a crooked house that sank through the wool, with its old resident
    reuzenguh_fossiel      r12  the giant guh skeleton (v2, the real guh proportions) in an excavation hall
    kristalkwekerij        r13  crystal beds in four growth stages, growth meters, a glass house (v2)
                                (also a side building of the Mijnguhdorp)
    verdwaalde_bouwploeg   r14  the lost building crew: a half-built tunnel, a little crane, the bouwkeet, stacks of stuff
    grotguh_holen          r15  the Grotguhs' sleeping holes around a campfire
    badhuis                r16  Badhuis De Warme Knabbel (v2): three bath halls, a portico, the relax corner, and (round 20)
                                the small overflowing baths stepping down along the east wall

    PYTHONPATH=tools python tools/features/onderwol_structuren2.py      (from the project root; standalone, idempotent;
                                                                         after onderwol_structuren1, whose dorp pool it extends)

The scenes of the open "diorama" renders (r13-r16: only a floor and two back walls) are closed into a cave first (sluit():
rock on the open sides and a dome above), so every template carves its own room (no Java hall needed).
"""
import json
import os
import re

import numpy as np
from PIL import Image

import make_structures as ms
import sign_text
from features import onderwol_bouw as ob
from features import onderwol_bouw_scene as sc
from features import onderwol_structuren1 as s1
from features import onderwol_vadskracht as ov

PREFIX = "sign.guhs.onderwol_bouw"
SALT = 17020001   # (this slice: 17020001, 17020011, ...)
ROTS = "guhs:knuffelsteen"

# the talking guhs (Java OnderWolStructuren2.PRAAT: the same pleks, kinds and counts)
PRAAT_KIND = {"onderwol_station": "wisselguh", "onderwol_kaas": "kraamguh", "onderwol_huisje": "omaguh", "onderwol_fossiel": "archeoloogguh",
              "onderwol_kweker": "pepertelerguh", "onderwol_bouwploeg": "timmerguh", "onderwol_grotguh": "kampbaasguh",
              "onderwol_badmeester": "wachterguh", "onderwol_barguh": "kraamguh"}
PRAAT = {
    "onderwol_station": ["Welkom op Station Diepewol! De volgende slee vertrekt... ooit.",
                         "Een kaartje? Eén knabbel. Waarheen? Nergens, het spoor is ingestort. Njeg.",
                         "Vroeger reden hier sleetjes vol kaasknabbels. Nu rijdt alleen het stof nog.",
                         "Ik verkoop al dertig jaar kaartjes. Nog nooit eentje verkocht. Morgen misschien!"],
    "onderwol_kaas": ["Njeg! Hier maken we ALLE saus.",
                      "Pomp, roer, vads... en klaar!",
                      "Oud? Welnee. De kaas is oud. De makerij is gloednieuw.",
                      "Proeven mag, maar alleen met een knabbel. Zonder knabbel is het gewoon slurpen."],
    "onderwol_huisje": ["Ik deed alleen een dutje... en toen woonde ik ondergronds.",
                        "Mijn buren wonen nog boven. Soms zwaai ik naar het plafond.",
                        "Het huisje zakt nog een beetje. Elke dag een pootje. Gezellig toch?",
                        "De post komt hier nooit. Wel lekker rustig, njeg."],
    "onderwol_fossiel": ["Kijk die oortjes! Een echte oer-guh.",
                         "In zijn buikje zat nog een kaasknabbel! Een fossiele knabbel. Niet opeten.",
                         "Ik graaf hier al twintig jaar. Met een lepeltje. Het gaat goed.",
                         "Volgens mij lag hij gewoon heel lang te vadsen. Heel, heel lang."],
    "onderwol_kweker": ["Elke ochtend een slokje en een liedje. Dan groeien ze het snelst. Njeg!",
                        "Niet aankomen hoor, ze groeien nog. En ze zijn heel verlegen.",
                        "Deze hier is pas vier weken oud. Kijk hem glimmen!",
                        "Als je heel stil bent, hoor je ze groeien. Pling. Pling."],
    "onderwol_bouwploeg": ["We waren op weg naar een klus. Drie jaar geleden. Linksaf was toch rechtsaf?",
                           "Die gang maken we nog af hoor. Eerst even pauze. Al drie jaar.",
                           "Pak gerust wat stenen mee. Dan hoeven wij ze niet terug te sjouwen.",
                           "De kraan doet het prima. Hij doet alleen een dutje, net als Henk."],
    "onderwol_grotguh": ["Welkom in onze holen! Wij zijn Grotguhs. Wij glinsteren in het donker, njeg.",
                         "Elk holletje heeft een nestje, een kussen en een lampje. Meer heeft een guh niet nodig.",
                         "De zon? Nooit van gehoord. Wij hebben kristallen. Die zijn ook warm. Een beetje.",
                         "Neem gerust een knabbel uit de voorraad. Eentje. Ik tel ze, hoor."],
    "onderwol_badmeester": ["Njeg! Niet duiken in het koude bad!",
                            "Eerst afspoelen, dan pas vadsen. Zo hoort het in De Warme Knabbel.",
                            "Bubbels zijn gratis. Schuim ook. Mijn fluitje niet.",
                            "Niet rennen bij het bad! Wel vadsen. Heel veel vadsen."],
    "onderwol_barguh": ["Aardbei, munt of choco? Vahoeg!",
                        "Een kaasshake kost drie knabbels. Het rietje is gratis.",
                        "Na het warme bad een koude shake. Zo deden de oude Romeinguhs dat ook.",
                        "Kaas in je shake? Natuurlijk. Alles is lekkerder met kaas."],
}
PRAAT_EN = {
    "onderwol_station": ["Welcome to Diepewol Station! The next sled leaves... some day.",
                         "A ticket? One nibble. Where to? Nowhere, the track caved in. Njeg.",
                         "Sleds full of cheese nibbles used to ride here. Now only the dust rides along.",
                         "I've been selling tickets for thirty years. Never sold a single one. Maybe tomorrow!"],
    "onderwol_kaas": ["Njeg! We make ALL the sauce here.",
                      "Pump, stir, vads... and done!",
                      "Old? Not at all. The cheese is old. The dairy is brand new.",
                      "Tasting is allowed, but only with a nibble. Without a nibble it's just slurping."],
    "onderwol_huisje": ["I was only taking a nap... and then I lived underground.",
                        "My neighbours still live upstairs. Sometimes I wave at the ceiling.",
                        "The house is still sinking a little. One paw a day. Cosy, right?",
                        "The post never comes down here. Nice and quiet though, njeg."],
    "onderwol_fossiel": ["Look at those little ears! A real ancient guh.",
                         "There was still a cheese nibble in its tummy! A fossil nibble. Don't eat it.",
                         "I've been digging here for twenty years. With a teaspoon. It's going well.",
                         "I think it just lay there vadsing for a very long time. A very, very long time."],
    "onderwol_kweker": ["A sip and a song every morning. That's how they grow fastest. Njeg!",
                        "Don't touch, please, they're still growing. And they're very shy.",
                        "This one is only four weeks old. Look at it sparkle!",
                        "If you're very quiet, you can hear them grow. Pling. Pling."],
    "onderwol_bouwploeg": ["We were on our way to a job. Three years ago. Left was right, wasn't it?",
                           "We'll finish that tunnel, don't worry. A little break first. Three years now.",
                           "Take some stones with you. Then we don't have to lug them back.",
                           "The crane works fine. It's just having a nap, like Henk."],
    "onderwol_grotguh": ["Welcome to our burrows! We're Cave Guhs. We sparkle in the dark, njeg.",
                         "Every little burrow has a nest, a cushion and a lamp. A guh needs nothing more.",
                         "The sun? Never heard of it. We have crystals. They're warm too. A little.",
                         "Take a nibble from the stash. Just one. I'm counting them, mind you."],
    "onderwol_badmeester": ["Njeg! No diving into the cold bath!",
                            "Rinse first, then vads. That's how we do it at The Warm Nibble.",
                            "Bubbles are free. Foam too. My whistle isn't.",
                            "No running by the bath! Vadsing is fine. Lots of vadsing."],
    "onderwol_barguh": ["Strawberry, mint or choco? Vahoeg!",
                        "A cheese shake costs three nibbles. The straw is free.",
                        "A cold shake after the warm bath. That's how the old Roman guhs did it.",
                        "Cheese in your shake? Of course. Everything is better with cheese."],
}

# the English of every sign line (sign_text keys are made from the Dutch); a line without English stays Dutch
EN_REGELS = {
    # r09
    "~ Station ~": "~ Station ~", "Diepewol": "Diepewol", "Vertrek: nooit": "Departs: never",
    "Kaartjes:": "Tickets:", "1 knabbel.": "1 nibble.", "Retourtje:": "Return:", "2 knabbels.": "2 nibbles.",
    "Niet op het": "No vadsing", "spoor vadsen!": "on the track!", "Ook niet even.": "Not even a bit.",
    "Vertrek:": "Departures:", "Wolkendorp 9:00": "Cloud town 9:00", "(vertraagd sinds": "(delayed since", "de ijstijd)": "the ice age)",
    "Gevonden": "Lost and", "voorwerpen:": "found:", "3 sokken,": "3 socks,", "1 slee.": "1 sled.",
    "Naar Wolkendorp:": "To Cloud town:", "ingestort.": "caved in.", "Neem de lift,": "Take the lift,", "njeg!": "njeg!",
    "Naar de": "To the", "Kaasvlakte:": "Cheese plain:", "ook ingestort.": "caved in too.", "Sorry, guh.": "Sorry, guh.",
    # r10
    "Kaasmakerij": "Cheese dairy", "De Gouden Saus": "The Golden Sauce", "Welkom, vadser!": "Welcome, vadser!",
    "Saus van de dag:": "Sauce of the day:", "kaas.": "cheese.", "Morgen ook:": "Tomorrow too:",
    "Niet in het vat": "No diving into", "duiken.": "the vat.", "Njeg.": "Njeg.",
    "Vissen verboden,": "No fishing,", "saus is geen": "sauce is not", "water.": "water.",
    "Dit is Kaasje,": "This is Kaasje,", "onze mascotte.": "our mascot.", "Niet likken!": "No licking!",
    # r10, the pomphuis (1.7.0 playtest)
    "Pomphuis!": "Pump house!", "Guhs rennen,": "Guhs run,", "pompen pompen,": "pumps pump,", "vaten vullen.": "vats fill up.",
    "Kaassaus-vaten:": "Cheese sauce vats:", "zie ze vollopen!": "watch them fill!", "Guhoven:": "Guh oven:",
    "grilt saté op": "grills satay on", "vadskracht.": "vads power.", "Geen kolen!": "No coal!", "Roze draad =": "Pink wire =",
    "vadskracht!": "vads power!", "Lampjes aan?": "Lamps on?", "Dan rennen ze!": "Then they run!",
    # r11
    "Hier stond ooit": "Here once stood", "huize Pluis": "house Pluis", "(voor het": "(before it", "zakte)": "sank)",
    "Bovengronds:": "Upstairs:", "1 verdieping": "1 floor", "hoger.": "higher.",
    "Pas op: zakt": "Careful: still", "nog een beetje.": "sinking a bit.",
    "Verhuisd? Nee,": "Moved? No,", "verzakt.": "sunk.",
    # r12
    "Guhosaurus": "Guhosaurus", "Vadsus": "Vadsus", "65 miljoen": "65 million", "njeg oud": "njeg old",
    "Niet aan de": "Don't pull", "staart trekken.": "the tail.", "Hij is": "He is", "gevoelig.": "sensitive.",
    "Ja, de oren": "Yes, the ears", "waren echt": "really were", "zo groot.": "that big.",
    "In zijn buikje": "In its tummy", "zat nog een": "there was still", "kaasknabbel!": "a cheese nibble!", "(Fossiel. Njeg.)": "(Fossil. Njeg.)",
    # r13
    "Kristalkwekerij": "Crystal nursery", "Niet aankomen,": "Don't touch,", "groeit nog!": "still growing!",
    "Zachtjes praten,": "Speak softly,", "kristallen zijn": "crystals are", "verlegen.": "shy.",
    "Week 1": "Week 1", "Nog heel": "Still very", "klein. Ahh.": "small. Aww.",
    "Week 4": "Week 4", "Groeit al": "Growing a", "een beetje!": "little already!",
    "Week 9": "Week 9", "Bijna zo groot": "Almost as big", "als een guh.": "as a guh.",
    "Bijna vahoeg!": "Almost vahoeg!", "Oogsten mag": "Harvest only", "pas morgen.": "tomorrow.", "(Njeg.)": "(Njeg.)",
    "Kas: alleen": "Glasshouse: only", "torenpepers.": "tower peppers.", "Kristallen": "Crystals stay", "buiten, njeg!": "outside, njeg!",
    # r14
    "Hier komt een": "A tunnel goes", "gang. Ooit.": "here. Some day.", "Na de pauze.": "After the break.",
    "Bouwstoffen.": "Building stuff.", "Pak maar, wij": "Help yourself,", "zijn toch": "we're lost", "verdwaald.": "anyway.",
    "Bouwkeet": "Site hut", "Pauze: altijd.": "Break: always.", "Werk: soms.": "Work: sometimes.",
    "Kraan in": "Crane under", "onderhoud.": "maintenance.", "(Hij doet een": "(It's having", "dutje.)": "a nap.)",
    "Wie weet de weg": "Who knows the way", "naar huis?": "back home?", "Wij niet. Njeg.": "Not us. Njeg.",
    # r15
    "Grotguh-holen": "Cave Guh burrows", "Kloppen mag,": "Knocking is fine,", "knuffelen ook.": "cuddling too.",
    "Knabbelvoorraad": "Nibble stash", "Max 1 per guh.": "Max 1 per guh.", "Of 2. Of 3.": "Or 2. Or 3.",
    "Kampvuur.": "Campfire.", "Niet je staartje": "Don't roast", "roosteren!": "your tail!",
    "Slaapkamer met": "Bedroom with", "uitzicht.": "a view.", "Pas op het": "Mind the", "trapje!": "steps!",
    # r16
    "Badhuis": "Bathhouse", "De Warme Knabbel": "The Warm Nibble",
    "Eerst afspoelen,": "Rinse first,", "Lauw sausbad": "Lukewarm sauce", "voor beginners": "bath: beginners",
    "Bubbels zijn": "Bubbles are", "gratis!": "free!",
    "Koud bad:": "Cold bath:", "brrr-guh!": "brrr-guh!", "(ijsblokjes niet": "(don't eat the", "opeten)": "ice cubes)",
    "Niet rennen,": "No running,", "wel vadsen.": "vadsing is fine.",
    "Kaasshakes:": "Cheese shakes:", "3 knabbels.": "3 nibbles.", "Rietje gratis!": "Free straw!",
    "Massage: nu met": "Massage: now with", "extra knuffel!": "an extra cuddle!",
    "Knabbelbadjes": "Nibble tubs", "Van warm naar": "From warm to", "lauw. Overlopen": "lukewarm. Spilling", "mag, njeg!": "is fine, njeg!",
    "Bovenste badje:": "Top tub:", "voor baasjes.": "for the bosses.", "Onderste: voor": "Bottom one: for", "luie vadsjes.": "lazy vadsers.",
}

# =====================================================================================================================
# helpers
# =====================================================================================================================
BOUWSELS = []


def bewaar(b, naam):
    b.save(naam)
    BOUWSELS.append(b)
    return b


def schoon(v, gezet, pal):
    """The renders' infobord stand-ins become air (real signs go there); explicit wool and knuffelsteen of the scene stay
    real blocks (omzet() would make them rock: they are layer blocks) - see wol_terug()."""
    ids = [i for i, s in enumerate(pal) if s.split("[")[0] in s1.BORDJES]
    m = np.isin(v, ids)
    v[m] = 0
    gezet[m] = True


def bordjes(naam):
    """{(x, z): (x, y, z, facing)} of the render's infobord stand-ins (scene coordinates)."""
    v, _, pal, _, _ = sc.laad(naam)
    out = {}
    for i, s in enumerate(pal):
        if s.split("[")[0] in s1.BORDJES:
            f = re.search(r"facing=(\w+)", s)
            for (x, y, z) in np.argwhere(v == i):
                out[(int(x), int(z))] = (int(x), int(y), int(z), f.group(1) if f else "south")
    return out


def wol_terug(b, naam, crop, blokken=("minecraft:pink_wool", "guhs:knuffelsteen")):
    """Blocks of these (layer) kinds the render script SET itself (puin, a tent, towels, stacks) come back as real blocks."""
    v, gezet, pal, _, _ = sc.laad(naam)
    x0, x1, y0, y1, z0, z1 = crop
    for i, s in enumerate(pal):
        if s not in blokken:
            continue
        for (x, y, z) in np.argwhere((v == i) & gezet):
            if x0 <= x <= x1 and y0 <= y <= y1 and z0 <= z <= z1:
                b.zet(int(x - x0), int(y - y0), int(z - z0), s)


def sluit(v, pal, box, F, top_rand, top_mid, open_=("x1", "z1"), n=4, rand=None, zaad=1):
    """Closes a scene into a cave (in place): air on the open sides of the box (x0, x1, z0, z1) becomes rock from the floor
    F up, air above the back walls too, a dome above the box (top_mid in the middle, top_rand at its edge), and the outer
    columns of rand (x0, x1, z0, z1: the crop) are rock all the way, so the room never opens into the world."""
    if ROTS not in pal:
        pal.append(ROTS)
    r = pal.index(ROTS)
    X, Y, Z = v.shape
    bx0, bx1, bz0, bz1 = box
    cx, cz, rx, rz = (bx0 + bx1) / 2, (bz0 + bz1) / 2, (bx1 - bx0) / 2 + .5, (bz1 - bz0) / 2 + .5
    ruis = np.random.default_rng(zaad).random((X, Z))
    ex0, ex1, ez0, ez1 = rand or (0, X - 1, 0, Z - 1)
    for x in range(X):
        for z in range(Z):
            kol = v[x, :, z]
            if x in (ex0, ex1) or z in (ez0, ez1):
                kol[F:][kol[F:] == 0] = r
                continue
            if bx0 <= x <= bx1 and bz0 <= z <= bz1:
                d = min(1.0, (abs((x - cx) / rx) ** n + abs((z - cz) / rz) ** n) ** (1 / n))
                top = int(top_mid - (top_mid - top_rand) * d * d + ruis[x, z] * 1.3)
                kol[top + 1:][kol[top + 1:] == 0] = r
            elif (x > bx1 and "x1" in open_) or (x < bx0 and "x0" in open_) or (z > bz1 and "z1" in open_) or (z < bz0 and "z0" in open_):
                kol[F:][kol[F:] == 0] = r
            else:
                vast = np.nonzero(kol)[0]
                hoog = int(vast.max()) if len(vast) else F
                kol[hoog + 1:][kol[hoog + 1:] == 0] = r
            if kol[Y - 1] == 0:
                kol[Y - 1] = r


def dicht(v, pal, crop, open_=lambda x, y, z: False):
    """Air on the faces of the crop becomes rock (the room never opens into the world), except where open_(x, y, z)."""
    if ROTS not in pal:
        pal.append(ROTS)
    r = pal.index(ROTS)
    x0, x1, y0, y1, z0, z1 = crop
    x1, y1, z1 = min(x1, v.shape[0] - 1), min(y1, v.shape[1] - 1), min(z1, v.shape[2] - 1)
    for (x, y, z) in np.argwhere(v[x0:x1 + 1, y0:y1 + 1, z0:z1 + 1] == 0):
        x, y, z = int(x) + x0, int(y) + y0, int(z) + z0
        if x in (x0, x1) or y in (y0, y1) or z in (z0, z1):
            if not open_(x, y, z):
                v[x, y, z] = r


def koepel(v, pal, ellips, F, top_rand, top_mid, zaad=1):
    """Only a dome (a scene whose hall has a flat ceiling): rock above top(d) inside the ellipse (cx, cz, rx, rz)."""
    if ROTS not in pal:
        pal.append(ROTS)
    r = pal.index(ROTS)
    X, Y, Z = v.shape
    cx, cz, rx, rz = ellips
    ruis = np.random.default_rng(zaad).random((X, Z))
    for x in range(X):
        for z in range(Z):
            d = min(1.0, ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2)
            top = int(top_mid - (top_mid - top_rand) * d + ruis[x, z] * 1.3)
            kol = v[x, :, z]
            kol[top + 1:][kol[top + 1:] == 0] = r


def vloer_y(v, x, z, van=0):
    """The floor (solid) y under the first air from below (scene coordinates)."""
    for y in range(max(1, van), v.shape[1]):
        if v[x, y, z] == 0 and v[x, y - 1, z] != 0:
            return y - 1
    return None


def grotguh(b, x, y, z, yaw=0.0, slaapt=False):
    """A wild Grotguh (GuhVariant.GROTGUH); slaapt: curled up asleep until petted."""
    nbt = ms.guh_nbt(1.0, Variant="grotguh")
    nbt["Rotation"] = ms.floats(float(yaw), 0.0)
    if slaapt:
        nbt["NeoForgeData"] = {"guhs_onderwol_slaapt": ms.Byte(1)}
    b.entity(float(x), float(y), float(z), nbt)


def praat(b, x, y, z, plek, yaw):
    b.npc(int(x), int(round(y)), int(z), plek, yaw=yaw, kind=PRAAT_KIND[plek])


def is_guh(g, x, z):
    return abs(g[0] - x) < .1 and abs(g[2] - z) < .1


def kisten_en_tonnen(b, loot):
    """Every chest and closed barrel of the render gets this loot."""
    for (pos, blok) in list(b.blocks.items()):
        if blok[0] == "minecraft:chest" and blok[2] is None:
            b.kist(*pos, blok[1].get("facing", "north"), loot)
        elif blok[0] == "minecraft:barrel" and blok[1].get("open") == "false" and blok[2] is None:
            b.ton(*pos, loot, facing=blok[1].get("facing", "up"))


# =====================================================================================================================
# r09 the Verlaten sledestation
# =====================================================================================================================
def sledestation(h):
    naam = "r09_verlaten_sledestation"
    crop = (0, 59, 6, 23, 2, 32)
    x0, _, y0, _, z0, _ = crop
    P = lambda x, y, z: (x - x0, y - y0, z - z0)
    def aanpas(v, gezet, pal):
        schoon(v, gezet, pal)
        dicht(v, pal, crop)
    b, gs, _ = s1.omzet(naam, "verlaten_sledestation", crop, aanpas=aanpas)
    b.prefix = f"{PREFIX}.station"
    bord = bordjes(naam)
    # the caved-in tunnel ends: rubble and rock at both ends of the track
    for (xe, xp) in ((0, 1), (59, 58)):
        for z in range(14, 20):
            for y in range(8, 14):
                if b.get(*P(xe, y, z)) == "minecraft:air":
                    b.schil(*P(xe, y, z))
                if y < 10 and b.get(*P(xp, y, z)) in ("minecraft:air", "minecraft:rail"):
                    b.zet(*P(xp, y, z), "fossielmijn_puin[axis=z]")
    b.anker(*P(20, 8, 9))
    b.ingang(*P(44, 8, 8))
    # signs
    x, y, z, f = bord[(25, 11)]
    b.sign(*P(x, y, z), f, ["Kaartjes:", "1 knabbel.", "Retourtje:", "2 knabbels."], wall=False)
    b.sign(*P(29, 14, 5), "south", ["~ Station ~", "Diepewol", "Vertrek: nooit", ""], wall=True)
    b.sign(*P(16, 9, 12), "south", ["Niet op het", "spoor vadsen!", "Ook niet even.", ""], wall=False)
    b.sign(*P(15, 9, 5), "south", ["Vertrek:", "Wolkendorp 9:00", "(vertraagd sinds", "de ijstijd)"], wall=False)
    b.sign(*P(30, 9, 27), "north", ["Gevonden", "voorwerpen:", "3 sokken,", "1 slee."], wall=False)
    b.sign(*P(3, 8, 16), "east", ["Naar Wolkendorp:", "ingestort.", "Neem de lift,", "njeg!"], wall=False)
    b.sign(*P(56, 8, 16), "west", ["Naar de", "Kaasvlakte:", "ook ingestort.", "Sorry, guh."], wall=False)
    # loot: the closed barrels (one on a sled), a chest in the ticket office
    kisten_en_tonnen(b, LOOT["verlaten_sledestation"])
    b.kist(*P(27, 9, 6), "east", LOOT["verlaten_sledestation"])
    # the ticket guh in the office talks; the others are wild guhs (the one on the sled sits on its load)
    for g in gs:
        gx, gy, gz, yaw = g[0], g[1], g[2], g[3]
        if is_guh(g, 30.5 - x0, 7.5 - z0):
            praat(b, gx, gy, gz, "onderwol_station", 180)
        elif is_guh(g, 25.5 - x0, 15.5 - z0):
            b.guh(gx, gy + 1, gz, yaw=yaw)
        else:
            b.guh(gx, gy, gz, yaw=yaw)
    return bewaar(b, "onderwol/verlaten_sledestation/start")


# =====================================================================================================================
# r10 Kaasmakerij De Gouden Saus
# =====================================================================================================================
def pomphuis(b):
    """(1.7.0 playtest) The pomphuis on the jetty, with WORKING vadskracht (features/onderwol_vadskracht.py): two Guhraden
    stand in the jetty's inner edge, each with a happy guh (2 x 15 VK); right against each wheel a Sauspomp in the jetty,
    standing on the saus lake (a pump lifts from the source block under it). Their hoses climb over the wheels and run down
    to the row of eight Sausvaten, which fill up while you watch. Guhdraad joins both wheels (over redstone lamps that burn
    on the guhs' power) and leads to a Guhoven grilling kaasknabbelsate. One net: 2 pumps (12) + the oven (5) = 17 of 30 VK.
    Template coordinates (floor y 3, the jetty's planks y 4)."""
    # the render's pumps stood on the planks (no source under them) and its hoses ran into the air: gone
    for p, blok in list(b.blocks.items()):
        if blok[0] in ("guhs:sausslang", "guhs:sauspomp"):
            b.set(*p, "minecraft:air")
    for x in range(24, 32):
        assert b.get(x, 4, 27) == "guhs:sausvat", (x, b.get(x, 4, 27))
        b.zet(x, 4, 27, "sausvat[facing=north,niveau=0,saus=leeg]", {"id": "guhs:sausvat"})
    # the wheels: in the jetty's inner edge (x 20), facing the hall; the pumps right against them, on the lake
    for zw in (22, 26):
        for z in (zw - 1, zw, zw + 1):
            assert b.get(20, 4, z) == "minecraft:spruce_planks"
            b.set(20, 4, z, "minecraft:air")
        ov.guhrad(b, 20, 4, zw, "east")
        assert b.get(19, 4, zw) == "minecraft:spruce_planks" and b.get(19, 3, zw) == "guhs:kaas_saus"
        ov.machine(b, 19, 4, zw, "sauspomp", "west")
        # the hose: up out of the pump, over the wheel, along x 23 to the vats
        for y in (5, 6, 7):
            ov.slang(b, 19, y, zw)
        for x in range(20, 24):
            ov.slang(b, x, 7, zw)
    for z in range(23, 28):
        ov.slang(b, 23, 7, z)
    for y in (6, 5):
        ov.slang(b, 23, y, 27)
    for x in range(24, 32):
        ov.slang(b, x, 5, 27)
    # Guhdraad over lamps: along both wheels (x 21), and to the Guhoven
    for z in range(21, 28):
        ov.draad(b, 21, 4, z)
    for x in (22, 23):
        ov.draad(b, x, 4, 24)
    ov.oven(b, 24, 4, 24, "east", "guhs:kaasknabbelsate", 16)
    b.sign(22, 4, 20, "north", ["Pomphuis!", "Guhs rennen,", "pompen pompen,", "vaten vullen."], wall=False)
    b.sign(26, 4, 26, "east", ["Kaassaus-vaten:", "zie ze vollopen!", "", ""], wall=False)
    b.sign(24, 4, 23, "east", ["Guhoven:", "grilt saté op", "vadskracht.", "Geen kolen!"], wall=False)
    b.sign(22, 4, 26, "east", ["Roze draad =", "vadskracht!", "Lampjes aan?", "Dan rennen ze!"], wall=False)


def kaasmakerij(h):
    naam = "r10_oude_kaasmakerij"
    crop = (2, 55, 4, 25, 2, 38)
    x0, _, y0, _, z0, _ = crop
    P = lambda x, y, z: (x - x0, y - y0, z - z0)

    def aanpas(v, gezet, pal):
        schoon(v, gezet, pal)
        koepel(v, pal, (28, 20, 25.0, 17.5), 8, 18, 23, zaad=10)
        dicht(v, pal, crop, lambda x, y, z: x == 55 and 18 <= z <= 22)
        # the sausval: the render's column sat inside the rock; one source high in the wall face falls into the lake
        saus = pal.index("guhs:kaas_saus")
        rots = pal.index("guhs:knuffelsteen")
        for y in range(8, 17):
            for z in (25, 26):
                if v[3, y, z] == saus:
                    v[3, y, z] = rots
        for z in (25, 26):
            x = next(x for x in range(3, 20) if v[x, 15, z] == 0)
            v[x - 1, 15, z] = saus
            gezet[x - 1, 15, z] = True
    b, gs, _ = s1.omzet(naam, "kaasmakerij", crop, aanpas=aanpas)
    b.prefix = f"{PREFIX}.kaas"
    bord = bordjes(naam)
    pomphuis(b)
    b.anker(*P(30, 7, 20))
    b.ingang(*P(40, 7, 34))
    # the east entrance tunnel ends at the template's edge: the connector for the Mijnguhdorp's side tunnels
    b.verbinding(*P(55, 8, 20), "east", "guhs:mijnguhdorp_zij_in")
    # signs (the render's infobords; the one by the lake moves onto the jetty)
    b.sign(*P(49, 8, 24), "east", ["Kaasmakerij", "De Gouden Saus", "Welkom, vadser!", ""], wall=False)   # (its infobord: under the wheel)
    x, y, z, f = bord[(25, 31)]
    b.sign(*P(x, y, z), f, ["Saus van de dag:", "kaas.", "Morgen ook:", "kaas."], wall=False)
    x, y, z, f = bord[(31, 21)]
    b.sign(*P(x, y, z), f, ["Niet in het vat", "duiken.", "Ook niet even.", "Njeg."], wall=False)
    x, y, z, f = bord[(17, 31)]
    b.set(*P(x, y, z), "minecraft:air")
    b.sign(*P(21, 9, 30), "south", ["Vissen verboden,", "saus is geen", "water.", ""], wall=False)
    b.sign(*P(15, 8, 11), "south", ["Dit is Kaasje,", "onze mascotte.", "Niet likken!", ""], wall=False)
    # the mascot: a big still guh on the plinth, under its cheese hat (the render drew it as a model)
    b.entity(15.5 - x0, 10 - y0, 8.5 - z0, ms.guh_nbt(4.2, NoAI=ms.Byte(1), Invulnerable=ms.Byte(1), Silent=ms.Byte(1),
                                                      Rotation=ms.floats(160.0, 0.0)))
    # loot
    kisten_en_tonnen(b, LOOT["kaasmakerij"])
    b.kist(*P(41, 8, 24), "west", LOOT["kaasmakerij"])
    for g in gs:
        gx, gy, gz, yaw = g[0], g[1], g[2], g[3]
        if is_guh(g, 47.5 - x0, 21.5 - z0):
            praat(b, 49 - x0, gy, gz, "onderwol_kaas", 270)     # (the render had Goudje inside the cheese table)
        else:
            b.guh(gx, gy, gz, yaw=yaw)
    ov.verbind(b)
    return bewaar(b, "onderwol/kaasmakerij/start")


# =====================================================================================================================
# r11 the Verzonken guhhuisje (Huize Pluis)
# =====================================================================================================================
def verzonken_guhhuisje(h):
    naam = "r11_verzonken_guhhuisje"
    crop = (1, 43, 4, 31, 1, 32)
    x0, x1, y0, y1, z0, z1 = crop
    P = lambda x, y, z: (x - x0, y - y0, z - z0)
    def aanpas(v, gezet, pal):
        schoon(v, gezet, pal)
        dicht(v, pal, crop)
    b, gs, _ = s1.omzet(naam, "verzonken_guhhuisje", crop, aanpas=aanpas)
    b.prefix = f"{PREFIX}.huisje"
    wol_terug(b, naam, crop)
    v, gezet, pal, _, _ = sc.laad(naam)
    schoon(v, gezet, pal)
    bord = bordjes(naam)
    # the shaft it sank through: a plug of pink wool at the top (the wool it came from)
    for x in range(22, 35):
        for z in range(9, 22):
            if (x - 28) ** 2 + (z - 15) ** 2 <= 20:
                for y in range(y1 - 2, y1 + 1):
                    b.zet(*P(x, y, z), "minecraft:pink_wool")
    b.anker(*P(12, vloer_y(v, 12, 24), 24))
    # signs at the render's infobords
    teksten = {(29, 13): ["Hier stond ooit", "huize Pluis", "(voor het", "zakte)"],
               (11, 13): ["Bovengronds:", "1 verdieping", "hoger.", ""],
               (34, 15): ["Pas op: zakt", "nog een beetje.", "Njeg.", ""]}
    for k, t in teksten.items():
        x, y, z, f = bord[k]
        b.sign(*P(x, y, z), f, t, wall=False)
    yv = vloer_y(v, 16, 22)
    b.sign(*P(16, yv + 1, 22), "south", ["Verhuisd? Nee,", "verzakt.", "", ""], wall=False)
    # loot: the barrel in the house, and a chest by the crooked bed
    kisten_en_tonnen(b, LOOT["verzonken_guhhuisje"])
    if v[20, 8, 13] == 0 and v[20, 7, 13] != 0:
        b.kist(*P(20, 8, 13), "south", LOOT["verzonken_guhhuisje"])
    for g in gs:
        gx, gy, gz, yaw = g[0], g[1], g[2], g[3]
        if is_guh(g, 28.5 - x0, 15.5 - z0):
            praat(b, gx, gy, gz, "onderwol_huisje", 250)
        else:
            b.guh(gx, gy, gz, yaw=yaw)
    return bewaar(b, "onderwol/verzonken_guhhuisje/start")


# =====================================================================================================================
# r12 the Reuzenguh-fossiel
# =====================================================================================================================
def reuzenguh_fossiel(h):
    naam = "r12_reuzenguh_fossiel"
    crop = (2, 58, 4, 27, 2, 39)
    x0, _, y0, _, z0, _ = crop
    P = lambda x, y, z: (x - x0, y - y0, z - z0)

    def aanpas(v, gezet, pal):
        schoon(v, gezet, pal)
        koepel(v, pal, (30, 21, 27.0, 17.5), 6, 21, 25, zaad=12)
        # a high alcove above the head, so the big ears stand free (they reach above the render's flat ceiling)
        laag = [i for i, s in enumerate(pal) if s.split("[")[0] in s1.LAAGBLOK or s == ROTS]
        for x in range(10, 22):
            for z in range(3, 39):
                if ((x - 30) / 27.0) ** 2 + ((z - 21) / 17.5) ** 2 <= 1.15:
                    for y in range(6, 27):
                        if v[x, y, z] in laag:
                            v[x, y, z] = 0
        dicht(v, pal, crop)
    b, gs, _ = s1.omzet(naam, "reuzenguh_fossiel", crop, aanpas=aanpas)
    b.prefix = f"{PREFIX}.fossiel"
    wol_terug(b, naam, crop, ("minecraft:pink_wool",))
    bord = bordjes(naam)
    b.anker(*P(30, 5, 10))
    b.ingang(*P(38, 5, 12))
    teksten = {(18, 9): ["Guhosaurus", "Vadsus", "65 miljoen", "njeg oud"],
               (50, 20): ["Niet aan de", "staart trekken.", "Hij is", "gevoelig."],
               (5, 24): ["Ja, de oren", "waren echt", "zo groot.", ""]}
    for k, t in teksten.items():
        x, y, z, f = bord[k]
        b.sign(*P(x, y, z), f, t, wall=False)
    b.sign(*P(33, 6, 13), "south", ["In zijn buikje", "zat nog een", "kaasknabbel!", "(Fossiel. Njeg.)"], wall=False)
    kisten_en_tonnen(b, LOOT["reuzenguh_fossiel"])
    b.kist(*P(51, 6, 13), "west", LOOT["reuzenguh_fossiel"])
    for g in gs:
        gx, gy, gz, yaw = g[0], g[1], g[2], g[3]
        if is_guh(g, 24.5 - x0, 11.5 - z0):
            praat(b, gx, gy, gz, "onderwol_fossiel", 200)
        else:
            b.guh(gx, gy, gz, yaw=yaw)
    return bewaar(b, "onderwol/reuzenguh_fossiel/start")


# =====================================================================================================================
# r13 the Kristalkwekerij
# =====================================================================================================================
def kristalkwekerij(h):
    naam = "r13_kristalkwekerij"
    crop = (2, 57, 4, 27, 3, 45)
    x0, x1, y0, y1, z0, z1 = crop
    P = lambda x, y, z: (x - x0, y - y0, z - z0)
    F = 6

    def aanpas(v, gezet, pal):
        schoon(v, gezet, pal)
        sluit(v, pal, (3, 56, 4, 44), F, 20, 24, open_=("x1", "z1"), n=8, rand=(x0, x1, z0, z1), zaad=13)
        # the way in: a 3 wide opening in the front wall behind the gate (the Mijnguhdorp's side tunnels connect here)
        for x in range(27, 30):
            for y in range(F, F + 4):
                v[x, y, z1] = 0
                gezet[x, y, z1] = True
    b, gs, _ = s1.omzet(naam, "kristalkwekerij", crop, aanpas=aanpas)
    b.prefix = f"{PREFIX}.kweker"
    bord = bordjes(naam)
    b.anker(*P(29, 5, 20))
    b.ingang(*P(20, 5, 38))
    b.verbinding(*P(28, F, z1), "south", "guhs:mijnguhdorp_zij_in")
    # the growth signs by the beds (north row: stage i, south row: 3 - i), the gate signs
    STADIUM = [["Week 1", "Nog heel", "klein. Ahh.", ""], ["Week 4", "Groeit al", "een beetje!", ""],
               ["Week 9", "Bijna zo groot", "als een guh.", ""], ["Bijna vahoeg!", "Oogsten mag", "pas morgen.", "(Njeg.)"]]
    for i, bx in enumerate((8, 17, 33, 43)):
        x, y, z, _ = bord[(bx + 1, 15)]
        b.sign(*P(x, y, z), "south", STADIUM[i], wall=False)
        x, y, z, _ = bord[(bx + 1, 26)]
        b.sign(*P(x, y, z), "north", STADIUM[3 - i], wall=False)
    x, y, z, f = bord[(32, 41)]
    b.sign(*P(x, y, z), f, ["Kristalkwekerij", "Niet aankomen,", "groeit nog!", ""], wall=False)
    x, y, z, f = bord[(25, 41)]
    b.sign(*P(x, y, z), f, ["Zachtjes praten,", "kristallen zijn", "verlegen.", ""], wall=False)
    b.sign(*P(45, F, 37), "north", ["Kas: alleen", "torenpepers.", "Kristallen", "buiten, njeg!"], wall=False)
    kisten_en_tonnen(b, LOOT["kristalkwekerij"])
    b.kist(*P(7, F, 42), "east", LOOT["kristalkwekerij"])
    for g in gs:
        gx, gy, gz, yaw = g[0], g[1], g[2], g[3]
        if is_guh(g, 30.5 - x0, 18.5 - z0):
            praat(b, gx, gy, gz, "onderwol_kweker", 200)
        else:
            b.guh(gx, gy, gz, yaw=yaw)
    return bewaar(b, "onderwol/kristalkwekerij/start")


# =====================================================================================================================
# r14 the Verdwaalde bouwploeg
# =====================================================================================================================
def verdwaalde_bouwploeg(h):
    naam = "r14_verdwaalde_bouwploeg"
    crop = (2, 55, 3, 29, 0, 43)
    x0, x1, y0, y1, z0, z1 = crop
    P = lambda x, y, z: (x - x0, y - y0, z - z0)
    F = 5

    def aanpas(v, gezet, pal):
        schoon(v, gezet, pal)
        sluit(v, pal, (3, 54, 8, 42), F, 23, 26, open_=("x1", "z1"), n=8, rand=(x0, x1, z0, z1), zaad=14)
    b, gs, _ = s1.omzet(naam, "verdwaalde_bouwploeg", crop, aanpas=aanpas)
    b.prefix = f"{PREFIX}.bouwploeg"
    wol_terug(b, naam, crop, ("guhs:knuffelsteen",))
    b.anker(*P(30, 4, 22))
    b.ingang(*P(48, 4, 38))
    b.sign(*P(20, F, 13), "south", ["Hier komt een", "gang. Ooit.", "Na de pauze.", ""], wall=False)
    b.sign(*P(34, F, 39), "south", ["Bouwstoffen.", "Pak maar, wij", "zijn toch", "verdwaald."], wall=False)
    b.sign(*P(11, F, 30), "north", ["Bouwkeet", "Pauze: altijd.", "Werk: soms.", ""], wall=False)
    b.sign(*P(42, F, 18), "south", ["Kraan in", "onderhoud.", "(Hij doet een", "dutje.)"], wall=False)
    b.sign(*P(26, F, 24), "south", ["Wie weet de weg", "naar huis?", "Wij niet. Njeg.", ""], wall=False)
    kisten_en_tonnen(b, LOOT["verdwaalde_bouwploeg"])
    for g in gs:
        gx, gy, gz, yaw = g[0], g[1], g[2], g[3]
        if is_guh(g, 20.5 - x0, 15.5 - z0):
            praat(b, gx, gy, gz, "onderwol_bouwploeg", 10)
        else:
            b.guh(gx, gy, gz, yaw=yaw, slaapt=True)       # (Henk, napping by the site hut)
    return bewaar(b, "onderwol/verdwaalde_bouwploeg/start")


# =====================================================================================================================
# r15 the Grotguh-holen
# =====================================================================================================================
def grotguh_holen(h):
    naam = "r15_grotguh_holen"
    crop = (0, 45, 2, 23, 1, 39)
    x0, x1, y0, y1, z0, z1 = crop
    P = lambda x, y, z: (x - x0, y - y0, z - z0)

    def aanpas(v, gezet, pal):
        schoon(v, gezet, pal)
        sluit(v, pal, (7, 44, 9, 38), 4, 17, 20, open_=("x1", "z1"), n=6, rand=(x0, x1, z0, z1), zaad=15)
    b, gs, _ = s1.omzet(naam, "grotguh_holen", crop, aanpas=aanpas)
    b.prefix = f"{PREFIX}.grotguh"
    wol_terug(b, naam, crop, ("guhs:knuffelsteen",))
    v, gezet, pal, _, _ = sc.laad(naam)
    Y = lambda x, z: vloer_y(v, x, z, 2) + 1
    b.anker(*P(22, Y(22, 19) - 1, 19))
    b.ingang(*P(34, Y(34, 30) - 1, 30))
    b.sign(*P(26, Y(26, 37), 37), "south", ["Grotguh-holen", "Kloppen mag,", "knuffelen ook.", ""], wall=False)
    b.sign(*P(37, Y(37, 15), 15), "west", ["Knabbelvoorraad", "Max 1 per guh.", "Of 2. Of 3.", "Njeg."], wall=False)
    b.sign(*P(26, Y(26, 19), 19), "north", ["Kampvuur.", "Niet je staartje", "roosteren!", ""], wall=False)
    b.sign(*P(14, Y(14, 11), 11), "south", ["Slaapkamer met", "uitzicht.", "Pas op het", "trapje!"], wall=False)
    kisten_en_tonnen(b, LOOT["grotguh_holen"])
    b.kist(*P(12, Y(12, 34), 34), "east", LOOT["grotguh_holen"])
    # the Grotguhs: asleep in their holes, awake around the fire; the eldest talks
    for g in gs:
        gx, gy, gz, yaw = g[0], g[1], g[2], g[3]
        if is_guh(g, 23.5 - x0, 27.5 - z0):
            praat(b, gx, gy, gz, "onderwol_grotguh", 140)
        else:
            in_hol = (gz + z0) < 9 or (gx + x0) < 7
            grotguh(b, gx, gy, gz, yaw=yaw, slaapt=in_hol)
    return bewaar(b, "onderwol/grotguh_holen/start")


# =====================================================================================================================
# r16 Badhuis De Warme Knabbel (+ round 20: the small overflowing baths)
# =====================================================================================================================
KNABBELBADJES = [   # (z0, z1, rim top y): stepping down to the south, each spills into the next
    (40, 44, 8), (44, 48, 7), (48, 52, 6), (52, 57, 5)]
BX0, BX1 = 74, 80


def badhuis(h):
    naam = "r16_saus_kuuroord"
    crop = (0, 83, 0, 23, 0, 61)
    x0, x1, y0, y1, z0, z1 = crop
    P = lambda x, y, z: (x - x0, y - y0, z - z0)
    F = 5

    def aanpas(v, gezet, pal):
        schoon(v, gezet, pal)
        sluit(v, pal, (2, 81, 2, 60), F, 18, 22, open_=("z1",), n=4, rand=(x0, x1, z0, z1), zaad=16)
    b, gs, _ = s1.omzet(naam, "badhuis", crop, aanpas=aanpas)
    b.prefix = f"{PREFIX}.badhuis"
    wol_terug(b, naam, crop, ("minecraft:pink_wool",))
    bord = bordjes(naam)
    # the spouting statues: only the top source stays (a stream into the bath; lower sources would flood the hall)
    for (mid, pz1, hoog) in ((14, 13, 3), (68, 13, 3)):
        for y in range(F, F + hoog - 1):
            b.set(*P(mid, y, pz1), "minecraft:air")
    # the showers: a little water basin in the floor under the shower head (no water in the air)
    for (x, z) in ((36, 57), (47, 57)):
        b.set(*P(x, F + 2, z), "minecraft:air")
        b.zet(*P(x, F - 1, z), "minecraft:water")
        b.zet(*P(x, F - 2, z), "knuffelbad_badtegel")
    # round 20: the Knabbelbadjes, four small tubs stepping down along the east wall, each overflowing into the next
    for i, (za, zb, top) in enumerate(KNABBELBADJES):
        for x in range(BX0, BX1 + 1):
            for z in range(za, zb + 1):
                rand = x in (BX0, BX1) or z in (za, zb)
                for y in range(F - 2, top + 1):
                    if rand:
                        b.zet(*P(x, y, z), "knuffelbad_badtegel" if y == top else "pastelbaksteen_vanille")
                    elif y <= top - 2:
                        b.zet(*P(x, y, z), "knuffelbad_badtegel")
                    else:
                        b.zet(*P(x, y, z), "kaas_saus")
                for y in range(top + 1, F + 4):
                    if not rand or z != za or i == 0:
                        b.set(*P(x, y, z), "minecraft:air")
        if i:   # the spill: a notch in the shared rim, the upper tub's sauce runs over into this one
            zr = za
            b.zet(*P(77, top + 1, zr), "kaas_saus")
            b.zet(*P(77, top, zr), "kaas_saus")
    for (x, z) in ((76, 42), (78, 41), (75, 46), (79, 50)):
        b.zet(*P(x, KNABBELBADJES[0][2] + 1 if z < 44 else KNABBELBADJES[1][2] + 1 if z < 48 else KNABBELBADJES[2][2] + 1, z), "knuffelbad_schuim")
    # steps up to the top tub (west side)
    for k, x in enumerate((71, 72, 73)):
        for y in range(F, F + k):
            b.zet(*P(x, y, 42), "pastelbaksteen_vanille")
        b.zet(*P(x, F + k, 42), "minecraft:quartz_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]")
    b.sign(*P(73, F, 39), "north", ["Knabbelbadjes", "Van warm naar", "lauw. Overlopen", "mag, njeg!"], wall=False)
    b.sign(*P(73, F, 58), "south", ["Bovenste badje:", "voor baasjes.", "Onderste: voor", "luie vadsjes."], wall=False)
    b.anker(*P(41, 4, 42))
    b.ingang(*P(41, 4, 56))
    teksten = {(41, 59): ["Badhuis", "De Warme Knabbel", "Welkom, vadser!", ""],
               (44, 56): ["Eerst afspoelen,", "njeg!", "", ""],
               (14, 34): ["Lauw sausbad", "voor beginners", "", ""],
               (44, 33): ["Bubbels zijn", "gratis!", "", ""],
               (68, 34): ["Koud bad:", "brrr-guh!", "(ijsblokjes niet", "opeten)"],
               (32, 42): ["Niet rennen,", "wel vadsen.", "", ""],
               (64, 47): ["Kaasshakes:", "3 knabbels.", "Rietje gratis!", ""],
               (38, 51): ["Massage: nu met", "extra knuffel!", "", ""]}
    for k, t in teksten.items():
        x, y, z, f = bord[k]
        b.sign(*P(x, y, z), f, t, wall=False)
    # loot: the bathrobe rack and the towel rack get a chest, the bar a barrel
    b.kist(*P(32, F, 55), "north", LOOT["badhuis"])
    b.kist(*P(19, F, 52), "west", LOOT["badhuis"])
    b.ton(*P(56, F, 50), LOOT["badhuis"])
    # the guests: in the baths, on the loungers, at the bar; the lifeguard by the warm bath and the bar guh talk
    praat(b, *P(39, F, 33), "onderwol_badmeester", 180)
    for g in gs:
        gx, gy, gz, yaw = g[0], g[1], g[2], g[3]
        if is_guh(g, 65.5 - x0, 51.5 - z0):
            praat(b, gx, gy, gz, "onderwol_barguh", 180)
        elif is_guh(g, 41.5 - x0, 34.5 - z0):
            continue    # (the render's lifeguard on his chair: the talking one stands by the bath)
        elif abs(gy - 4.6) < .01 and 33 <= gx + x0 <= 49:
            b.guh(gx, 4.75, gz, yaw=yaw)                   # (on the bubbling sauce, 12 pixels high)
        elif abs(gz + z0 - 46.5) < .01 or is_guh(g, 62.5 - x0, 48.5 - z0):
            b.guh(gx, gy, gz - 1 if gz + z0 > 48 else gz + 1, yaw=yaw)   # (next to the lounger / bar stool, not in it)
        else:
            b.guh(gx, gy, gz, yaw=yaw)
    for (x, z, yaw) in ((77.5, 42.5, 200), (76.5, 54.5, 160)):     # two guests in the little tubs
        b.guh(x - x0, (KNABBELBADJES[0][2] if z < 44 else KNABBELBADJES[3][2]) - .4, z - z0, yaw=yaw)
    return bewaar(b, "onderwol/badhuis/start")


# =====================================================================================================================
# the towel turban (the only new clothes: GuhClothes.HANDDOEK_TULBAND, on the theemutsje bones)
# =====================================================================================================================
TULBAND = [("outfit_theemutsje", "doek"), ("outfit_theemutsje_rand", "rand"), ("outfit_theemutsje_pom", "knoop")]


def _swatch(geo, bone):
    for bn in geo["minecraft:geometry"][0]["bones"]:
        if bn["name"] == bone:
            for c in bn.get("cubes", []):
                for f in c["uv"].values():
                    return tuple(f["uv"])
    raise KeyError(bone)


def _verf(soort, rng):
    img = Image.new("RGBA", (32, 32))
    p = img.load()
    for y in range(32):
        for x in range(32):
            if soort == "doek":      # terry cloth: white with soft pink stripes, wrapped
                roze = ((y + x // 6) // 4) % 3 == 0
                base = (248, 196, 222) if roze else (252, 248, 250)
            elif soort == "rand":
                base = (236, 140, 186)
            else:
                base = (255, 252, 254)
            a = int(rng.integers(-6, 7))
            p[x, y] = tuple(max(0, min(255, c + a)) for c in base) + (255,)
    return img


ICOON = ["................", "......aaaa......", "....aabbbbaa....", "...abbbccbbba...", "..abccbbbbccba..", "..abbbbccbbbba..",
         ".abccbbbbbbccba.", ".abbbbccccbbbba.", ".adddddddddddda.", "..aaaaaaaaaaaa..", "................"]
ICOON_KLEUR = {"a": (200, 110, 160), "b": (252, 248, 250), "c": (248, 190, 220), "d": (236, 140, 186)}


def kleding(A):
    geo = json.load(open(os.path.join(A, "geckolib", "models", "entity", "guh.geo.json"), encoding="utf-8"))
    rng = np.random.default_rng(1774)
    img = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    for bone, soort in TULBAND:
        u, v = _swatch(geo, bone)
        img.paste(_verf(soort, rng), (u * 4, v * 4))
    img.save(os.path.join(A, "textures", "entity", "guh_clothes", "handdoek_tulband.png"))
    a = np.zeros((16, 16, 4), np.uint8)
    top = (16 - len(ICOON)) // 2
    for y, rij in enumerate(ICOON):
        for x, ch in enumerate(rij):
            if ch in ICOON_KLEUR:
                a[top + y, x, :3] = ICOON_KLEUR[ch]
                a[top + y, x, 3] = 255
    Image.fromarray(a).save(os.path.join(A, "textures", "item", "handdoek_tulband.png"))


# =====================================================================================================================
# loot, texts, the structures
# =====================================================================================================================
BOUWSTOF, SNACK = s1.BOUWSTOF, s1.SNACK
LOOT_DEF = {
    "verlaten_sledestation": [((2, 4), BOUWSTOF + [("minecraft:rail", 3, 4, 12), ("lampion_roze", 2, 1, 2)]), ((1, 3), SNACK),
                              (1, [("baltoslee_minislee", 2, 1, 1), ("guhklok", 1, 1, 1), ("guh_lantaarnpaal", 2, 1, 1), ("leeg", 4, 1, 1)])],
    "kaasmakerij": [((2, 4), BOUWSTOF[:4] + [("belegen_kaas_tegels", 3, 4, 10), ("gatenkaas", 3, 2, 6), ("klipkaas", 2, 2, 6),
                                            ("schimmelkaas", 2, 1, 4)]), ((2, 3), SNACK),
                    (1, [("sausvat", 2, 1, 1), ("sauspomp", 1, 1, 1), ("block_of_kaasknabbels", 2, 1, 2), ("leeg", 3, 1, 1)])],
    "verzonken_guhhuisje": [((2, 4), BOUWSTOF[:4] + [("pastelbaksteen_perzik", 3, 4, 10), ("pleister_vanille", 3, 4, 10),
                                                    ("dakpan_roze", 3, 4, 10), ("pluiswolblok", 2, 2, 6)]), ((1, 2), SNACK),
                            (1, [("behang_guhtjes_roze", 2, 2, 6), ("guh_kozijn_roze", 2, 1, 2), ("guhnestje", 1, 1, 1), ("leeg", 3, 1, 1)])],
    "reuzenguh_fossiel": [((2, 4), BOUWSTOF + [("minecraft:bone_block", 3, 2, 6), ("guh_kristalsteen", 3, 4, 10)]), ((1, 2), SNACK),
                          (1, [("fossielmijn_fossielbeeldje", 2, 1, 1), ("vahoegiet", 1, 1, 1), ("guh_kristal_cluster", 2, 1, 3), ("leeg", 3, 1, 1)])],
    "kristalkwekerij": [((2, 4), [("guh_kristalsteen", 4, 4, 12), ("guh_kristal_blok", 3, 1, 4), ("guh_kristal_cluster", 3, 1, 4),
                                  ("viltsteen_tegels", 3, 4, 10), ("gladde_knuffelsteen", 2, 2, 8), ("kasglas", 2, 2, 6)]), ((1, 2), SNACK),
                        (1, [("guh_kristal_lamp", 2, 1, 2), ("vahoegiet", 1, 1, 2), ("warmtelamp", 1, 1, 1), ("leeg", 3, 1, 1)])],
    "verdwaalde_bouwploeg": [((3, 6), BOUWSTOF + [("pastelbaksteen_roze", 3, 4, 12), ("pastelbaksteen_lavendel", 3, 4, 12),
                                                  ("pastelbaksteen_aqua", 2, 4, 12), ("dakpan_mint", 2, 4, 10), ("pleister_perzik", 2, 4, 10)]),
                             ((1, 2), SNACK), (1, [("guh_hekje_roze", 2, 2, 6), ("minecraft:scaffolding", 2, 4, 8), ("guh_lantaarnpaal", 2, 1, 1),
                                                   ("leeg", 3, 1, 1)])],
    "grotguh_holen": [((2, 3), [("pluiswolblok", 3, 2, 6), ("pink_kussen", 2, 1, 2), ("magenta_kussen", 2, 1, 2), ("guhnestje", 2, 1, 1),
                                ("knuffelsteen", 3, 4, 10)]), ((2, 3), SNACK),
                      (1, [("klusjes_guhlampje", 2, 1, 1), ("knabbelbak", 2, 1, 1), ("theetafel", 1, 1, 1), ("leeg", 3, 1, 1)])],
    "badhuis": [((2, 4), [("knuffelbad_badtegel", 3, 4, 10), ("tegel_hartjes", 3, 4, 10), ("tegel_dambord", 2, 4, 10),
                          ("pastelbaksteen_vanille", 3, 4, 12), ("minecraft:quartz_pillar", 2, 2, 6), ("glasinlood_guh", 1, 1, 3)]),
                ((1, 3), SNACK + [("disco_milkshake", 2, 1, 2)]),
                (1, [("badjasje", 3, 1, 1), ("handdoek_tulband", 3, 1, 1), ("knuffelbad_badeendje", 2, 1, 1), ("guhshampoo", 2, 1, 2),
                     ("leeg", 4, 1, 1)])],
}
LOOT = {n: f"guhs:chests/onderwol/{n}" for n in LOOT_DEF}

# naam: (builder, titel, tooltip, titel_en, tooltip_en, band, spacing, separation, ingang kans)
STRUCTUREN = {
    "verlaten_sledestation": (sledestation, "Verlaten sledestation", "Een oud ondergronds sledestation met perrons, sleetjes en een loket. Vertrek: nooit.",
                              "Abandoned Sled Station", "An old underground sled station with platforms, sleds and a ticket office. Departs: never.",
                              (26, 38), 44, 16, 0.5),
    "kaasmakerij": (kaasmakerij, "Kaasmakerij De Gouden Saus", "Sauspompen, kaasvaten en reuzenkaaswielen aan een warm sausmeer. Niet in het vat duiken!",
                    "The Golden Sauce Dairy", "Sauce pumps, cheese vats and giant cheese wheels by a warm sauce lake. No diving into the vat!",
                    (24, 36), 40, 14, 0.3),
    "verzonken_guhhuisje": (verzonken_guhhuisje, "Verzonken guhhuisje", "Huize Pluis zakte ooit door de wol. Scheef, bemost, en er woont nog steeds iemand.",
                            "Sunken Guh House", "House Pluis once sank through the wool. Crooked, mossy, and someone still lives there.",
                            (40, 50), 36, 12, 0.0),
    "reuzenguh_fossiel": (reuzenguh_fossiel, "Reuzenguh-fossiel", "Het skelet van een oer-guh, diep in de kristalsteen. Ja, de oren waren echt zo groot.",
                          "Giant Guh Fossil", "The skeleton of an ancient guh, deep in the crystal stone. Yes, the ears really were that big.",
                          (13, 20), 44, 16, 0.3),
    "kristalkwekerij": (kristalkwekerij, "Kristalkwekerij", "Kweekbedden vol guhkristallen in vier groeistadia. Niet aankomen, ze groeien nog!",
                        "Crystal Nursery", "Beds full of guh crystals in four growth stages. Don't touch, they're still growing!",
                        (13, 20), 40, 14, 0.3),
    "verdwaalde_bouwploeg": (verdwaalde_bouwploeg, "Verdwaalde bouwploeg", "Een achtergelaten bouwplaats: een halve gang, een kraantje en stapels bouwstoffen.",
                             "Lost Building Crew", "An abandoned building site: half a tunnel, a little crane and stacks of building stuff.",
                             (28, 40), 36, 12, 0.4),
    "grotguh_holen": (grotguh_holen, "Grotguh-holen", "Het knusse thuis van de Grotguhs: slaapholletjes, een kampvuur en een knabbelvoorraad.",
                      "Cave Guh Burrows", "The cosy home of the Cave Guhs: sleeping burrows, a campfire and a nibble stash.",
                      (28, 40), 36, 12, 0.3),
    "badhuis": (badhuis, "Badhuis De Warme Knabbel", "Een Romeins badhuis onder de wol: drie badzalen, knabbelbadjes en kaasshakes.",
                "The Warm Nibble Bathhouse", "A Roman bathhouse under the wool: three bath halls, nibble tubs and cheese shakes.",
                (26, 38), 48, 18, 0.6),
}
# the side buildings of the Mijnguhdorp (its pool guhs:onderwol/mijnguhdorp/zij, made by onderwol_structuren1)
DORP_ZIJ = {"kaasmakerij": 2, "kristalkwekerij": 2}

TEKSTEN = {   # key: (en, nl)
    "item.guhs.handdoek_tulband": ("Towel Turban", "Handdoek-tulband"),
    "gui.guhs.kledingbron.loot_badhuis": ("The Warm Nibble Bathhouse", "Badhuis De Warme Knabbel"),
    "gui.guhs.kleding.prijs.hint.in_badhuis_de_warme_knabbel": ("In the chests of the Warm Nibble bathhouse, under the wool",
                                                                "In de kisten van Badhuis De Warme Knabbel, onder de wol"),
}


def koppel_dorp(h, templates):
    """Adds our side buildings to the Mijnguhdorp's pool zij (idempotent: our own elements are replaced)."""
    pad = f"{h.D}/worldgen/template_pool/onderwol/mijnguhdorp/zij.json"
    if not os.path.exists(pad):
        print("onderwol_structuren2: no", pad, "(run onderwol_structuren1 first): the side buildings are not attached")
        return
    pool = json.load(open(pad, encoding="utf-8"))
    eigen = {templates[n].locatie for n in DORP_ZIJ}
    pool["elements"] = [e for e in pool["elements"] if e["element"].get("location") not in eigen]
    for n, wgt in DORP_ZIJ.items():
        pool["elements"].append({"weight": wgt, "element": {"element_type": "minecraft:single_pool_element", "location": templates[n].locatie,
                                                            "projection": "rigid", "processors": ob.PROCESSORS}})
    h.w(pad, pool)


def bouw_alles(h):
    for naam, pools in LOOT_DEF.items():
        ob.loot(h, naam, pools)
    biomes = ob.guhmension_biomes(h)
    gemaakt = {}
    for i, (naam, (maak, titel, tip, titel_en, tip_en, band, spacing, separation, kans)) in enumerate(STRUCTUREN.items()):
        b = maak(h)
        gemaakt[naam] = b
        ob.grot_bouw(h, naam, templates=[(b, 1)], titel=titel, tooltip=tip, titel_en=titel_en, tooltip_en=tip_en,
                     salt=SALT + 10 * i, band=band, spacing=spacing, separation=separation,
                     ingang=dict(kans=kans) if kans else None, biomes=biomes)
    koppel_dorp(h, gemaakt)
    for plek, regels in PRAAT.items():
        for i, (nl, en) in enumerate(zip(regels, PRAAT_EN[plek]), start=1):
            h.lang(f"quest.guhs.onderwol_bouw.{plek}.{i}", en, nl)
    for k, (en, nl) in TEKSTEN.items():
        h.lang(k, en, nl)
    # the towel turban: its clothes texture, icon and item model
    kleding(h.A)
    h.w(f"{h.A}/items/handdoek_tulband.json", {"model": {"type": "minecraft:model", "model": "guhs:item/handdoek_tulband"}})
    h.w(f"{h.A}/models/item/handdoek_tulband.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/handdoek_tulband"}})
    return gemaakt


def selfcheck(h):
    """Every block exists with known properties; every loot item exists; every template has its anchor."""
    fouten = []
    seen = {}
    for b in BOUWSELS:
        if f"guhs:{b.naam}_midden" not in b.ankers:
            fouten.append(f"{b.naam}: no anchor")
        for (naam, props) in set((v[0], tuple(sorted(v[1].items()))) for v in b.blocks.values()):
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
                extra = {k for k, _ in props} - seen[naam] - {"waterlogged", "level"}
                if extra and seen[naam]:
                    fouten.append(f"{b.naam}: {naam} has no property {sorted(extra)}")
    for naam, pools in LOOT_DEF.items():
        for _, entries in pools:
            for item, *_ in entries:
                if item == "leeg" or item.startswith("minecraft:"):
                    continue
                if not os.path.exists(f"{h.A}/items/{item}.json"):
                    fouten.append(f"loot {naam}: item guhs:{item} does not exist")
    return sorted(set(fouten))


def build(h):
    bouw_alles(h)
    for f in selfcheck(h):
        print("onderwol_structuren2:", f)


# =====================================================================================================================
# standalone: python tools/features/onderwol_structuren2.py from the project root
# =====================================================================================================================
EN_OVERLAY = os.path.join("tools", "lang", "en", "c195_onderwol_structuren2.json")
SIGN_PREFIXES = tuple(f"{PREFIX}.{p}." for p in ("station", "kaas", "huisje", "fossiel", "kweker", "bouwploeg", "grotguh", "badhuis"))


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
    for k, nl in sign_text.TEXTS.items():
        if k.startswith(SIGN_PREFIXES):
            NL[k] = nl
            EN[k] = EN_REGELS.get(nl, nl)
            if nl not in EN_REGELS:
                print("onderwol_structuren2: no English for the sign line", repr(nl))
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        w(p, data)
    w(EN_OVERLAY, dict(sorted(EN.items())))
    print(f"onderwol_structuren2: {len(BOUWSELS)} templates, {len(NL)} texts")


if __name__ == "__main__":
    import sys
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    _direct()
