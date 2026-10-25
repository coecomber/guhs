"""
1.7.0 "Onder de Wol", structures part 3 (design: MineScapeStory/ontwerp-guhs170-mijnen.md chapter 2.4 and rounds 5-20; Java:
feature/onderwol/structuren3; framework: features/onderwol_bouw.py, worked example: onderwol_structuren1.py): seven more buildings
under the ground, made from the design renders r17-r24 (frozen scenes in features/onderwol_bouw_bron):

    grotdisco             r17  af en toe: a secret disco in a crystal geode (the existing disco blocks), DJ Njeg
    oude_wolspinnerij     r18  af en toe: De Wolclub, a cosy spinning club with a water wheel (vadskracht); also a side
                               building of the Mijnguhdorp (pool guhs:onderwol/mijnguhdorp/zij, its west door)
    sterrengrot           r19  af en toe: a quiet dome with a star ceiling, a picnic and a star pond
    knuffelkuil           r20  af en toe: a ball pit of knuffelblokken; six guhs hid in it (and fell asleep: pet them awake)
    verrassingsfeestje    r21  af en toe: a surprise party with a giant cake; clothing loot: feesthoedje, slinger-sjaaltje
    oude_guhtempel        r23  zeldzaam: the huge half buried temple complex with the Grote Guh and its Gouden Kaasknabbel
                               (round 20: the statues are pinker and built after the real (sitting) guh model)
    oude_bankkluis        r24  zeldzaam: the old Bank Guh vault and the traces of a failed Mika break-in

    Loot extras: the super vads template (guhs:mijnhulp/super_vads_sjabloon) in the temple and the vault, Spreukjesboekjes
    (enchant_randomly over #guhs:spreukjes/boekje or /alle, dressed with set_components) in the temple, the vault and the party.

    PYTHONPATH=tools python tools/features/onderwol_structuren3.py      (from the project root; standalone, idempotent)

The scenes come from the render scripts (python tools/features/onderwol_bouw_scene.py r17_grotdisco ... r24_oude_bankkluis).
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
from features import onderwol_structuren1 as s1

PREFIX = "sign.guhs.onderwol3"
SALT = 17030001   # (this slice: 17030001, 17030011, ...)
LUCHT = "minecraft:air"

# the talking guhs: plek -> (GuhNpcEntity.Kind id, [Dutch lines]); Java OnderWolStructuren3.PRAAT has the same pleks and counts
PRAAT = {
    "onderwol_disco": ("kroegbaasguh", [
        "Handjes in de lucht! Elke danser geeft vadskracht aan de Disco-dynamo!",
        "Ik ben DJ Njeg. Mijn lievelingsplaat? 'Vahoeg, vahoeg', de remix.",
        "Boven ligt gewoon een berg wol. Hier beneden is het feest. Niet verder vertellen!",
        "Een milkshake bij de bar? Aardbei, munt of choco. Gratis, als je danst.",
        "Moe gedanst? Even op een kussentje. Vijf minuten. Dan weer vadsen!"]),
    "onderwol_spinnerij": ("knuffelmakerguh", [
        "Welkom bij De Wolclub! Lang geleden was alles grijs. Toen pakten wij onze spinnewielen... en nu? Roze! Vahoeg!",
        "Hier hoeft niemand te werken. We spinnen omdat het gezellig is. En om de vijf minuten is er pauze.",
        "Het waterrad geeft vadskracht. Die draait de spinnewielen, dan hoeven onze pootjes het niet te doen.",
        "Neem gerust een wolbaal mee. Wol is er genoeg, de hele Guhmension ligt er vol mee!",
        "In de weefhoek maken we tapijtjes, dekentjes en stofjes voor guhkleding."]),
    "onderwol_sterren": ("sterrenwacht_ruine_sterrenkijker", [
        "Kijk, daar! Dat sterrenbeeld heet de Grote Kaasknabbel.",
        "Onder de grond zie je de sterren het best. Geen wolkjes in de weg!",
        "Ik heb ze allemaal geteld. Het zijn er... eh... heel veel. Vahoeg.",
        "Ga op een kussentje liggen en kijk omhoog. Dan word je vanzelf een beetje vads."]),
    "onderwol_kuil": ("guhkindje", [
        "...8, 9, 10! Ik kom! Heb jij de anderen gezien? Ze zitten ergens in de knuffels.",
        "Verstoppertje is het leukste spel. Behalve als je de zoeker bent. Dan is het het op-een-na-leukste.",
        "Spring maar in de kuil! Je zakt een beetje weg in de knuffels.",
        "Schoenen uit hoor! Guhs hebben geen schoenen, dus dat is makkelijk.",
        "Gevonden guhtjes zijn vaak in slaap gevallen. Aai ze even wakker!"]),
    "onderwol_feestje": ("guhkindje", [
        "VERRASSING! Oh, ben jij het pas. We oefenen nog even.",
        "Dit feestje is voor jou! Of voor wie er maar binnenkomt. Dus voor jou!",
        "Er zit een guh in een van de cadeautjes. Raad maar welke. Niet schudden!",
        "De taart is van knuffelblokken. Niet happen, wel knuffelen. Njeg!",
        "Pak gerust een feesthoedje uit de kist. Een feestje zonder hoedje telt niet."]),
    "onderwol_tempel": ("archeoloogguh", [
        "Deze tempel is ouder dan de kaas. En de kaas is heel oud.",
        "De oude guhs bouwden alles van zachte steen. Dan deed niemand zich pijn als hij ertegenaan liep.",
        "Achterin staat De Grote Guh met de Gouden Kaasknabbel. Niet likken, ik heb het geprobeerd.",
        "Er zit een gat in het voorplein. Dat weet ik, want ik lag erin. Njeg!",
        "De glijbaan in de oostvleugel is de snelste uitgang. Wieee!"]),
    "onderwol_bank": ("wachterguh", [
        "Ik bewaak deze kluis al honderd jaar. Er is pas een keer ingebroken. Door een Mika. Het mislukte. Njeg!",
        "Zie je die pootafdrukken? Mika kwam door de muur, maar de kluis hield het. Alleen een deukje.",
        "Rente? Nul komma nul knabbels. Maar je spaargeld is wel heel gezellig samen.",
        "De code van de deur? Die ben ik vergeten. Gelukkig staat hij open.",
        "Neem gerust iets mee. De oude bankguhs zijn allang met pensioen."]),
}
PRAAT_EN = {
    "onderwol_disco": ["Hands in the air! Every dancer gives vads power to the Disco Dynamo!",
                       "I'm DJ Njeg. My favourite record? 'Vahoeg, vahoeg', the remix.",
                       "Up there it's just a mountain of wool. Down here it's a party. Don't tell anyone!",
                       "A milkshake at the bar? Strawberry, mint or choco. Free, if you dance.",
                       "Danced out? Sit on a cushion for a bit. Five minutes. Then back to vadsing!"],
    "onderwol_spinnerij": ["Welcome to De Wolclub! Long ago everything was grey. Then we picked up our spinning wheels... and now? Pink! Vahoeg!",
                           "Nobody has to work here. We spin because it's cosy. And every five minutes there's a break.",
                           "The water wheel makes vads power. It turns the spinning wheels, so our paws don't have to.",
                           "Take a bale of wool, go on. There's plenty of wool, the whole Guhmension is full of it!",
                           "In the weaving corner we make little rugs, blankets and fabrics for guh clothes."],
    "onderwol_sterren": ["Look, there! That constellation is called the Great Cheese Nibble.",
                         "You see the stars best under the ground. No little clouds in the way!",
                         "I counted them all. There are... uh... a lot. Vahoeg.",
                         "Lie down on a cushion and look up. You'll turn a little vads all by yourself."],
    "onderwol_kuil": ["...8, 9, 10! Ready or not! Have you seen the others? They're somewhere in the plushies.",
                      "Hide and seek is the best game. Unless you're the seeker. Then it's the second best.",
                      "Jump into the pit! You sink a little into the plushies.",
                      "Shoes off, please! Guhs don't have shoes, so that's easy.",
                      "Found guhs have often fallen asleep. Give them a pet to wake them up!"],
    "onderwol_feestje": ["SURPRISE! Oh, it's only you. We're still practising.",
                         "This party is for you! Or for whoever walks in. So, for you!",
                         "There's a guh inside one of the presents. Guess which one. No shaking!",
                         "The cake is made of plush blocks. Don't bite, do cuddle. Njeg!",
                         "Grab a party hat from the chest. A party without a hat doesn't count."],
    "onderwol_tempel": ["This temple is older than the cheese. And the cheese is very old.",
                        "The old guhs built everything from soft stone. So nobody got hurt walking into it.",
                        "At the back stands the Great Guh with the Golden Cheese Nibble. Don't lick it, I tried.",
                        "There's a hole in the forecourt. I know, because I was in it. Njeg!",
                        "The slide in the east wing is the fastest way out. Wheee!"],
    "onderwol_bank": ["I've guarded this vault for a hundred years. There was only one break-in. By a Mika. It failed. Njeg!",
                      "See those paw prints? Mika came through the wall, but the vault held. Just a little dent.",
                      "Interest? Zero point zero nibbles. But your savings are very cosy together.",
                      "The door code? I forgot it. Luckily the door is open.",
                      "Take something, go on. The old bank guhs retired long ago."],
}

# the English of every sign line (sign_text keys come from the Dutch); a line without English stays Dutch
EN_REGELS = {
    # disco
    "Grotdisco:": "Cave disco:", "dansen tot de": "dance till the", "kaas op is!": "cheese runs out!",
    "Pootjes vegen,": "Wipe your paws,", "njeg!": "njeg!", "DJ Njeg draait": "DJ Njeg spins", "vanavond (en": "tonight (and",
    "morgen, en": "tomorrow, and", "altijd)": "always)", "Shakes gratis": "Free shakes", "voor wie danst": "for dancers",
    "Uitrustkussens:": "Rest cushions:", "max 5 minuten,": "5 minutes max,", "dan weer vadsen": "then vads again",
    "Geheime disco.": "Secret disco.", "Niet verder": "Don't tell", "vertellen, njeg!": "anyone, njeg!",
    "Dansvloer.": "Dance floor.", "Hier dansen de": "Here the guhs", "guhs vadskracht": "dance vads power", "bij elkaar.": "together.",
    # spinnerij
    "De Wolclub,": "De Wolclub,", "sinds altijd.": "since forever.", "Iedereen": "Everyone", "welkom!": "welcome!",
    "Hier werd de": "Here the whole", "hele Guhmension": "Guhmension was", "roze gesponnen!": "spun pink!",
    "Waterrad =": "Water wheel =", "vadskracht.": "vads power.", "Niet aaien,": "Don't pet it,", "hij draait!": "it spins!",
    "Pauze is om": "Break every", "de 5 minuten.": "5 minutes.", "Werken is om": "Work every", "de 6 minuten.": "6 minutes.",
    "Thee en koekjes:": "Tea and cookies:", "pak maar, njeg!": "help yourself!",
    "Weefhoek:": "Weaving corner:", "tapijtjes en": "little rugs,", "dekentjes en": "blankets and", "guhkleding-stof": "guh fabrics",
    "Wolbalen: pak er": "Wool bales: take", "gerust een": "one, go on", "(of twee).": "(or two).",
    "Spinnen is geen": "Spinning is not", "werk, het is": "work, it's", "knuffelen met": "cuddling with", "wol.": "wool.",
    # sterrengrot
    "Sterrengrot": "Star Cave", "Stil kijken,": "Watch quietly,", "zachtjes vadsen.": "vads softly.",
    "Wens iets bij": "Make a wish on", "een vallende": "a falling", "ster. (Liefst": "star. (Ideally", "kaasknabbels.)": "cheese nibbles.)",
    "Sterrenvijver:": "Star pond:", "niet in duiken,": "no diving,", "de sterren": "the stars", "schrikken, njeg.": "get scared, njeg.",
    # knuffelkuil
    "Knuffelkuil": "Plush Pit", "Schoenen uit!": "Shoes off!", "Vind alle 6": "Find all 6", "verstopte": "hidden", "guhtjes!": "guhs!",
    "Alles gevonden?": "Found them all?", "Pak een knuffel!": "Take a plushie!", "Niet spieken!": "No peeking!",
    "Glijbaan: een": "Slide: one", "guh tegelijk,": "guh at a time,", "of drie. Njeg.": "or three. Njeg.",
    "Slapend guhtje?": "Sleeping guh?", "Aai hem wakker,": "Pet it awake,", "dan is hij": "then it's", "gevonden!": "found!",
    # feestje
    "Sssst!": "Shhh!", "Verrassings-": "Surprise", "feestje! Niet": "party! Don't", "verklappen!": "tell!",
    "Jarig? Dan mag": "Birthday? Then", "je de eerste": "you get the", "hap. (Van de": "first bite. (Of", "taart, njeg.)": "cake, njeg.)",
    "Cadeautjes:": "Presents:", "niet schudden,": "no shaking,", "er zit een": "there's a", "guh in.": "guh inside.",
    "Buffet: max": "Buffet: max", "3 taartjes": "3 cakes", "per guh. Of 4.": "per guh. Or 4.",
    "VERRASSING!": "SURPRISE!", "(Doe maar of": "(Act like", "je schrikt.)": "you're shocked.)",
    # tempel
    "Oude Guhtempel.": "Old Guh Temple.", "Zachtjes lopen,": "Walk softly,", "de oude guhs": "the old guhs", "slapen. Njeg!": "are asleep. Njeg!",
    "Pas op voor": "Mind the", "gaten in de": "holes in the", "vloer!": "floor!",
    "Guhbert": "Guhbert", "de Eerste:": "the First:", "vond de wol uit": "invented wool",
    "Njegrid": "Njegrid", "de Wijze: telde": "the Wise: counted", "alle knabbels": "all the nibbles", "(tot 3)": "(up to 3)",
    "Vadsimir:": "Vadsimir:", "tilde ooit een": "once lifted a", "heel kaaswiel": "whole cheese wheel",
    "Pluisje": "Pluisje", "de Zachte:": "the Soft:", "maakte het": "made the", "eerste kussen": "first cushion",
    "Kaasper: at de": "Kaasper: ate the", "eerste knabbel": "first nibble", "(per ongeluk)": "(by accident)",
    "Oma Guh: zei": "Granny Guh: always", "altijd: 'eerst": "said: 'nibble", "knabbelen'": "first'",
    "Njeg, gefopt!": "Njeg, got you!", "Trapje is daar.": "Ladder's there.",
    "Tempel van de": "Temple of the", "Gouden": "Golden", "Kaasknabbel": "Cheese Nibble",
    "Oude puzzel:": "Old puzzle:", "niemand weet": "nobody knows", "meer hoe hij": "how it works", "werkt. Kist mag!": "now. Open it!",
    "De Grote Guh": "The Great Guh", "met de Gouden": "with the Golden", "Kaasknabbel.": "Cheese Nibble.",
    "Niet aan de": "Don't lick", "knabbel likken!": "the nibble!",
    "Het lichtpad": "The light path", "brandt al half.": "is half lit.", "De rest? Njeg,": "The rest? Njeg,", "te donker.": "too dark.",
    "Geheime kamer!": "Secret room!", "Sssst, niet": "Shhh, don't", "doorvertellen!": "tell anyone.",
    "Uitgang?": "Exit?", "Glijbaan!": "Slide!", "(pootjes in het": "(paws inside the", "voertuig)": "vehicle)",
    "Wieee! Einde": "Wheee! End of", "glijbaan. Nog": "the slide. Again?", "een keer? Eerst": "Then walk back", "weer naar boven.": "up first.",
    "Wieee!": "Wheee!", "begint hier": "starts here",
    # bankkluis
    "Bank Guh,": "Bank Guh,", "sinds 1 njeg.": "since 1 njeg.", "Geen Mika's": "No Mikas", "toegestaan.": "allowed.",
    "Ook niet met": "Not even with", "een snor.": "a moustache.", "Rente:": "Interest:", "0,0 knabbels.": "0.0 nibbles.",
    "Wel gezellig.": "But cosy.", "Code vergeten?": "Forgot the code?", "Niet zo vads": "Don't be so", "doen.": "vads.",
    "Volgende keer": "Better luck", "beter. -M.": "next time. -M.", "Mika-proof!": "Mika-proof!", "(alleen een": "(just a",
    "deukje)": "little dent)", "Kluis open?": "Vault open?", "Pak er gerust": "Take one,", "een. Of twee.": "go on. Or two.",
}


# =====================================================================================================================
# the scene conversion (onderwol_structuren1.omzet, plus: which layer rock of the render is a real block here)
# =====================================================================================================================
def omzet(scene, naam, crop, echt=None, aanpas=None, prefix=None):
    """A frozen render scene -> a Bouw. crop = (x0, x1, y0, y1, z0, z1) inclusive, scene coordinates. Every air voxel of the
    crop is carved. Layer rock is SCHIL next to air (else left out), unless echt(x, y, z, gezet) says it is a real block of
    the build (a geode wall, a pond bed, a floor the script laid itself). Returns (Bouw, guhs, meta, P) with P(x, y, z) the
    scene -> template coordinates."""
    v, gezet, pal, guhs, meta = sc.laad(scene)
    v, gezet = v.copy(), gezet.copy()
    if aanpas:
        aanpas(v, gezet, pal)
    x0, x1, y0, y1, z0, z1 = crop
    x1, y1, z1 = min(x1, v.shape[0] - 1), min(y1, v.shape[1] - 1), min(z1, v.shape[2] - 1)
    buren = s1.lucht_buren(v, [i for i, s in enumerate(pal) if i and s.split("[")[0] not in s1.LAAGBLOK
                               and not any(s.startswith("guhs:" + c) for c in s1.ERTS)])
    b = ob.Bouw((x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1), naam, prefix=prefix or f"{PREFIX}.{naam}")
    gekend = {}
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                i = int(v[x, y, z])
                lx, ly, lz = x - x0, y - y0, z - z0
                if i == 0:
                    b.set(lx, ly, lz, LUCHT)
                    continue
                s = pal[i]
                basis = s.split("[")[0]
                if basis in s1.LAAGBLOK:
                    if echt and echt(x, y, z, bool(gezet[x, y, z])):
                        b.zet(lx, ly, lz, s1.kaart(s))
                    elif buren[x, y, z]:
                        b.schil(lx, ly, lz)
                    continue
                if any(basis.startswith("guhs:" + c) for c in s1.ERTS):
                    if buren[x, y, z]:
                        b.zet(lx, ly, lz, s1.kaart(s))
                    continue
                if s not in gekend:
                    gekend[s] = s1.kaart(s)
                if gekend[s]:
                    b.zet(lx, ly, lz, gekend[s])
                else:
                    b.set(lx, ly, lz, LUCHT)       # (a stand-in of the render, e.g. its infobord for a sign: room again)
    gs =[(g[0] - x0, g[1] - y0, g[2] - z0, g[3], g[4], g[5]) for g in guhs
          if x0 <= g[0] <= x1 + 1 and y0 <= g[1] <= y1 + 1 and z0 <= g[2] <= z1 + 1]
    return b, gs, meta, (lambda x, y, z: (x - x0, y - y0, z - z0))


def vrij(b, x, y, z):
    """Is this template spot open (air) with something solid under it? (for signs, chests and guhs)"""
    return b.get(x, y, z) == LUCHT and b.get(x, y - 1, z) not in (None, LUCHT)


def plaats(b, x, y, z, wat):
    """Puts something on a spot that must be open (fails loudly, so a later render change can't hide a sign in a wall)."""
    assert vrij(b, x, y, z), f"{b.naam}: {(x, y, z)} is not free for {wat}: {b.get(x, y, z)} on {b.get(x, y - 1, z)}"


def bord(b, p, facing, lines, wall=False):
    x, y, z = p
    if not wall:
        plaats(b, x, y, z, lines[0])
    b.sign(x, y, z, facing, lines, wall=wall)


def kist(b, p, facing, loot):
    plaats(b, *p, "a chest")
    b.kist(*p, facing, loot)


def npc(b, p, plek, yaw):
    x, y, z = p
    b.entity(x + 0.5, y, z + 0.5, {"id": "guhs:guh_npc", "Kind": PRAAT[plek][0], "PersistenceRequired": ms.Byte(1),
                                   "Invulnerable": ms.Byte(1), "Rotation": ms.floats(float(yaw), 0.0), "RoleData": {"guhs_plek": plek}})


def anker_op(b, p):
    """The start anchor in the floor block at p (the block stays what it was)."""
    assert b.get(*p) not in (None, LUCHT, ob.SCHIL), f"{b.naam}: the anchor {p} needs a floor block, not {b.get(*p)}"
    b.anker(*p)


def ingang_op(b, p):
    x, y, z = p
    assert b.get(x, y + 1, z) not in (None, ob.SCHIL), f"{b.naam}: the shaft marker {p} needs room above it"
    b.ingang(*p, final=None if b.get(*p) not in (None, LUCHT) else ob.SCHIL)


# =====================================================================================================================
# the guh statues (round 20: more like guhs, pinker): voxels of the sitting guh model (guh_sitting.geo.json)
# =====================================================================================================================
# (origin, size) in model pixels; x across, y up, z: the face looks to -z
BEELD_DELEN = {
    "body": [((-5, 2.5, -2.5), (10, 9, 5)), ((-3.5, 1, -3), (7, 12, 6)), ((-4, 2, -4), (8, 10, 8)), ((-4.5, 1.5, -3.5), (9, 11, 7)),
             ((4.5, 1, -3), (2, 5, 6)), ((-6.5, 1, -3), (2, 5, 6))],
    "tail": [((-0.75, 0, 4), (1.5, 1, 8))],
    "foot": [((2.5, 0, -7), (3.5, 2, 5)), ((-6, 0, -7), (3.5, 2, 5))],
    "arm": [((1.5, 7, -6.5), (3, 3.5, 3)), ((-4.5, 7, -6.5), (3, 3.5, 3))],
    "head": [((-8, 14.5, -4.5), (16, 9, 8)), ((-5.5, 12, -5), (11, 14, 9)), ((-7, 13.5, -7), (14, 11, 13)), ((-6.5, 13, -6), (13, 12, 11))],
    "ear": [((4.5, 24, -1.5), (7, 5, 1)), ((5.5, 23, -1.75), (5, 7, 1.5)), ((-11.5, 24, -1.5), (7, 5, 1)), ((-10.5, 23, -1.75), (5, 7, 1.5))],
}
BEELD_VOLGORDE = ["head", "ear", "arm", "foot", "body", "tail"]
BEELD_STEEN = {"head": "roze_knuffelsteen", "body": "roze_knuffelsteen", "tail": "roze_knuffelsteen", "ear": "roze_knuffelsteen",
               "arm": "gepolijste_viltsteen", "foot": "gepolijste_viltsteen"}
VOOR = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}


def guhbeeld(b, cx, y0, cz, s, facing="south", oud=None):
    """A sitting guh of blocks, s blocks per model pixel, its feet on y0, its middle on the column (cx, cz), looking to facing.
    Pink stone, deeper pink paws and arms, pink inner ears, big dark eyes (a light spot from s 0.4), a pink nose and blush.
    oud: a random.Random to wear it a little (a few cracked pieces). Returns the set of its cells."""
    fx, fz = VOOR[facing]
    rx, rz = -fz, fx                          # model +x in the world

    def model(dx, dz):
        """A block offset -> model pixels (across, along: -z is the face)."""
        return (dx * rx + dz * rz) / s, -(dx * fx + dz * fz) / s

    half = 0.5 / s
    cellen = {}
    r = int(math.ceil(12.5 * s)) + 1
    hoog = int(math.ceil(31 * s)) + 1
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            mx, mz = model(dx, dz)
            for dy in range(hoog):
                my = (dy + 0.5) / s
                for deel in BEELD_VOLGORDE:
                    raak = False
                    for (o, sz) in BEELD_DELEN[deel]:
                        t = [half if sz[k] * s < 1.0 else 0.0 for k in range(3)]
                        if (o[0] - t[0] <= mx <= o[0] + sz[0] + t[0] and o[1] - t[1] <= my <= o[1] + sz[1] + t[1]
                                and o[2] - t[2] <= mz <= o[2] + sz[2] + t[2]):
                            raak = True
                            break
                    if raak:
                        cellen[(dx, dy, dz)] = deel
                        break
    for (dx, dy, dz), deel in cellen.items():
        blok = BEELD_STEEN[deel]
        if oud and deel in ("body", "head") and oud.random() < 0.04:
            blok = "knuffelsteen"
        b.zet(cx + dx, y0 + dy, cz + dz, blok)

    def voorste(ml, my):
        """The frontmost cell of the head at model x ml, height my: (dx, dy, dz) or None."""
        dy = int(math.floor(my * s))
        beste = None
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                mx, mz = model(dx, dz)
                if abs(mx - ml) * s > 0.5 or cellen.get((dx, dy, dz)) != "head":
                    continue
                if beste is None or mz < beste[0]:
                    beste = (mz, (dx, dy, dz))
        return beste[1] if beste else None

    def verf(cel, blok, uit=0):
        if cel:
            dx, dy, dz = cel
            b.zet(cx + dx + fx * uit, y0 + dy, cz + dz + fz * uit, blok)

    # the eyes: an oval each (3 x 4 model pixels), at least one block
    for kant in (-1, 1):
        ex, ey = kant * 3.6, 20.0
        stappen = max(1, int(round(3.2 * s)))
        gezien = set()
        for i in range(-stappen, stappen + 1):
            for j in range(-stappen - 1, stappen + 2):
                ml, my = ex + i / s * 0.5, ey + j / s * 0.5
                if ((ml - ex) / 1.7) ** 2 + ((my - ey) / 2.2) ** 2 > 1.0 and (i, j) != (0, 0):
                    continue
                cel = voorste(ml, my)
                if cel and cel not in gezien:
                    gezien.add(cel)
                    verf(cel, "minecraft:black_concrete")
        if s >= 0.4:
            verf(voorste(ex - kant * 0.8, ey + 1.6), "minecraft:white_concrete")
        if s >= 0.35:
            verf(voorste(kant * 6.0, 16.6), "minecraft:pink_concrete")       # blush
    # the nose (the snout cube), standing out a little on the big ones
    verf(voorste(0.0, 17.2), "minecraft:magenta_concrete" if s >= 0.4 else "minecraft:pink_concrete")
    # the inside of the ears: pink, on their front side
    for (dx, dy, dz), deel in list(cellen.items()):
        if deel == "ear" and (dx + fx, dy, dz + fz) not in cellen:
            mx, _ = model(dx, dz)
            my = (dy + 0.5) / s
            if 6.0 <= abs(mx) <= 10.5 and 24.0 <= my <= 28.5:
                b.zet(cx + dx, y0 + dy, cz + dz, "minecraft:pink_concrete")
    return {(cx + dx, y0 + dy, cz + dz) for (dx, dy, dz) in cellen}


# =====================================================================================================================
# r17 the Grotdisco
# =====================================================================================================================
def grotdisco(h):
    scene = "r17_grotdisco"
    v, _, _, _, m = sc.laad(scene)
    VL, CX, CZ, R = m["VL"], m["CX"], m["CZ"], 16.0

    def echt(x, y, z, gezet):
        """The geode: its crystal stone shell is real (not the world's rock), and so is the corridor's crystal floor."""
        d = math.sqrt((x - CX) ** 2 + ((y - VL - 3) * 1.25) ** 2 + (z - CZ) ** 2)
        return d <= R - 0.8 or (y == VL and z >= CZ + 11 and abs(x - CX) <= 3)
    b, gs, _, P = omzet(scene, "grotdisco", (4, 38, VL - 1, 21, 4, v.shape[2] - 1), echt=echt)
    anker_op(b, P(CX, VL, CZ + 2))
    ingang_op(b, P(CX, VL, CZ + 18))
    # the signs of the render (its legend), and a few more
    bord(b, P(CX + 2, VL + 1, CZ + 19), "south", ["Grotdisco:", "dansen tot de", "kaas op is!", ""])
    bord(b, P(CX - 2, VL + 1, CZ + 13), "south", ["Pootjes vegen,", "njeg!", "", ""])
    bord(b, P(CX - 2, VL + 1, CZ + 21), "south", ["Geheime disco.", "Niet verder", "vertellen, njeg!", ""])
    bord(b, P(CX - 2, VL + 1, CZ - 9), "south", ["DJ Njeg draait", "vanavond (en", "morgen, en", "altijd)"])
    bord(b, P(CX - 11, VL + 1, CZ + 5), "east", ["Shakes gratis", "voor wie danst", "", ""])
    bord(b, P(CX + 10, VL + 1, CZ - 5), "west", ["Uitrustkussens:", "max 5 minuten,", "dan weer vadsen", ""])
    bord(b, P(CX + 7, VL + 1, CZ + 7), "south", ["Dansvloer.", "Hier dansen de", "guhs vadskracht", "bij elkaar."])
    # DJ Njeg in front of the dynamo (the render's guh there), the dancers stay wild guhs
    for (gx, gy, gz, yaw, _, zit) in gs:
        if abs(gx - P(CX, 0, 0)[0] - .5) < .1 and abs(gz - P(0, 0, CZ - 10)[2] - .5) < .1:
            npc(b, (int(gx), int(round(gy)), int(gz)), "onderwol_disco", 0)
        else:
            b.guh(gx, gy, gz, yaw=yaw)
    kist(b, P(CX - 11, VL + 1, CZ - 5), "east", LOOT["grotdisco"])
    b.save("onderwol/grotdisco/start")
    return b


# =====================================================================================================================
# r18 the Oude wolspinnerij (De Wolclub)
# =====================================================================================================================
def wolspinnerij(h):
    scene = "r18_oude_wolspinnerij"
    v, _, pal, _, m = sc.laad(scene)
    VL, X, Z = m["VL"], v.shape[0], v.shape[2]

    def dicht(v, gezet, pal):
        """The brook comes in through holes in the walls, but the template's outer columns stay rock (no water into the world)."""
        rots = pal.index("guhs:c_viltsteen")
        for x in (0, X - 1):
            for z in range(4, 11):
                for y in range(VL - 1, VL + 5):
                    v[x, y, z] = rots
    b, gs, _, P = omzet(scene, "oude_wolspinnerij", (0, X - 1, VL - 2, VL + 14, 2, Z - 3), aanpas=dicht)
    anker_op(b, P(25, VL, 22))
    ingang_op(b, P(6, VL, 19))
    # the west door: the jigsaw for the Mijnguhdorp's side tunnel (at foot level, facing out); alone it is just air
    b.verbinding(*P(0, VL + 1, 19), "west", "guhs:mijnguhdorp_zij_in")
    bord(b, P(4, VL + 1, 23), "west", ["De Wolclub,", "sinds altijd.", "Iedereen", "welkom!"])
    bord(b, P(15, VL + 1, 15), "south", ["Hier werd de", "hele Guhmension", "roze gesponnen!", ""])
    bord(b, P(22, VL + 1, 12), "south", ["Waterrad =", "vadskracht.", "Niet aaien,", "hij draait!"])
    bord(b, P(29, VL + 1, 22), "south", ["Pauze is om", "de 5 minuten.", "Werken is om", "de 6 minuten."])
    bord(b, P(13, VL + 1, 22), "north", ["Thee en koekjes:", "pak maar, njeg!", "", ""])
    bord(b, P(42, VL + 1, 23), "north", ["Weefhoek:", "tapijtjes en", "dekentjes en", "guhkleding-stof"])
    bord(b, P(36, VL + 1, 33), "north", ["Wolbalen: pak er", "gerust een", "(of twee).", ""])
    bord(b, P(21, VL + 1, 30), "north", ["Spinnen is geen", "werk, het is", "knuffelen met", "wol."])
    # the Spinmeester at the lectern; the others (in the spinning circles, at tea, by the looms) are wild guhs
    for (gx, gy, gz, yaw, _, zit) in gs:
        if abs(gx - P(13, 0, 0)[0] - .5) < .1 and abs(gz - P(0, 0, 15)[2] - .5) < .1:
            npc(b, (int(gx), int(round(gy)), int(gz)), "onderwol_spinnerij", 180)
        else:
            b.guh(gx, round(gy) + (0.5 if zit else 0), gz, yaw=yaw)
    kist(b, P(31, VL + 1, 34), "north", LOOT["oude_wolspinnerij"])
    b.ton(*P(46, VL + 1, 16), LOOT["oude_wolspinnerij"]) if vrij(b, *P(46, VL + 1, 16)) else None
    b.save("onderwol/oude_wolspinnerij/start")
    return b


# =====================================================================================================================
# r19 the Sterrengrot
# =====================================================================================================================
def sterrengrot(h):
    scene = "r19_sterrengrot"
    v, _, _, _, m = sc.laad(scene)
    VL, CX, CZ = m["VL"], m["CX"], m["CZ"]

    def echt(x, y, z, gezet):
        """The star pond's bed of crystal stone."""
        return y == VL - 1 and CX + 4 <= x <= CX + 10 and CZ - 8 <= z <= CZ - 2
    b, gs, _, P = omzet(scene, "sterrengrot", (2, 37, VL - 1, 21, 2, 33), echt=echt)
    anker_op(b, P(CX + 2, VL, CZ + 6))
    ingang_op(b, P(CX - 9, VL, CZ + 7))
    bord(b, P(CX + 1, VL + 1, CZ + 4), "south", ["Sterrengrot", "Stil kijken,", "zachtjes vadsen.", ""])
    bord(b, P(CX - 4, VL + 1, CZ - 4), "south", ["Wens iets bij", "een vallende", "ster. (Liefst", "kaasknabbels.)"])
    bord(b, P(CX + 4, VL + 1, CZ - 1), "south", ["Sterrenvijver:", "niet in duiken,", "de sterren", "schrikken, njeg."])
    for (gx, gy, gz, yaw, _, zit) in gs:
        if abs(gx - P(CX, 0, 0)[0] - .5) < .1 and abs(gz - P(0, 0, CZ)[2] - .5) < .1:
            npc(b, (int(gx), int(round(gy)), int(gz)), "onderwol_sterren", 210)
        else:
            b.guh(gx, round(gy), gz, yaw=yaw)
    kist(b, P(CX - 8, VL + 1, CZ + 5), "east", LOOT["sterrengrot"])
    b.save("onderwol/sterrengrot/start")
    return b


# =====================================================================================================================
# r20 the Knuffelkuil
# =====================================================================================================================
def knuffelkuil(h):
    scene = "r20_knuffelkuil"
    v, _, _, guhs, m = sc.laad(scene)
    VL, CX, CZ = m["VL"], m["CX"], m["CZ"]
    KR = 9.5
    b, gs, _, P = omzet(scene, "knuffelkuil", (1, 38, VL - 6, VL + 10, 2, 34))
    anker_op(b, P(CX + 12, VL, CZ - 1))
    ingang_op(b, P(CX + 13, VL, CZ + 8))
    ox = int(round(CX + KR + .6))
    bord(b, P(ox + 1, VL + 1, CZ - 2), "east", ["Knuffelkuil", "Schoenen uit!", "", ""])
    bord(b, P(ox + 1, VL + 1, CZ + 2), "east", ["Vind alle 6", "verstopte", "guhtjes!", ""])
    bord(b, P(ox + 4, VL + 1, CZ + 4), "west", ["Alles gevonden?", "Pak een knuffel!", "", ""])
    bord(b, P(CX + 15, VL + 1, CZ + 4), "west", ["Niet spieken!", "", "", ""])
    bord(b, P(ox + 1, VL + 1, CZ - 4), "east", ["Slapend guhtje?", "Aai hem wakker,", "dan is hij", "gevonden!"])
    bord(b, P(CX - int(KR) - 6, VL + 1, CZ + 2), "west", ["Glijbaan: een", "guh tegelijk,", "of drie. Njeg.", ""])
    # the reward chest where the render had a knabbelkist (the plushie moves next to it: a chest needs room to open)
    b.set(*P(ox + 3, VL + 2, CZ + 4), LUCHT)
    b.set(*P(ox + 3, VL + 1, CZ + 4), LUCHT)
    kist(b, P(ox + 3, VL + 1, CZ + 4), "west", LOOT["knuffelkuil"])
    if vrij(b, *P(ox + 3, VL + 1, CZ + 5)):
        b.zet(*P(ox + 3, VL + 1, CZ + 5), "knuffel_normal[facing=west]")
    # the seeker counts by the wall; the six hiders in the pit fell asleep (pet them awake: found!); the slider is wild
    for (gx, gy, gz, yaw, _, zit) in gs:
        if abs(gx - P(CX + 14, 0, 0)[0] - .5) < .1 and abs(gz - P(0, 0, CZ + 2)[2] - .5) < .1:
            npc(b, (int(gx), int(round(gy)), int(gz)), "onderwol_kuil", 90)
        elif gy < P(0, VL + 1, 0)[1] - 0.2:
            b.guh(gx, gy, gz, yaw=yaw, slaapt=True)
        else:
            b.guh(gx, gy, gz, yaw=yaw)
    b.save("onderwol/knuffelkuil/start")
    return b


# =====================================================================================================================
# r21 the Verrassingsfeestje
# =====================================================================================================================
def verrassingsfeestje(h):
    scene = "r21_verrassingsfeestje"
    v, _, pal, _, m = sc.laad(scene)
    VL = m["VL"]                                 # (the first air layer: the floor lies at VL - 1)

    def lager(v, gezet, pal):
        """The dome stops at VL + 11 (the render's cave touched the top of its box)."""
        rots = pal.index("guhs:knuffelsteen")
        for y in range(VL + 12, v.shape[1]):
            v[:, y, :][v[:, y, :] == 0] = rots
    b, gs, _, P = omzet(scene, "verrassingsfeestje", (2, v.shape[0] - 1, VL - 2, VL + 13, 2, 32), aanpas=lager,
                        echt=lambda x, y, z, gezet: gezet and y == VL - 1)
    anker_op(b, P(20, VL - 1, 25))
    ingang_op(b, P(31, VL - 1, 24))
    bord(b, P(37, VL, 18), "east", ["Sssst!", "Verrassings-", "feestje! Niet", "verklappen!"])
    bord(b, P(17, VL, 23), "south", ["Jarig? Dan mag", "je de eerste", "hap. (Van de", "taart, njeg.)"])
    bord(b, P(9, VL, 19), "south", ["Cadeautjes:", "niet schudden,", "er zit een", "guh in."])
    bord(b, P(10, VL, 10), "south", ["Buffet: max", "3 taartjes", "per guh. Of 4.", ""])
    bord(b, P(23, VL, 23), "south", ["VERRASSING!", "(Doe maar of", "je schrikt.)", ""])
    # the birthday guh by the cake (the render's guh at 20, 11), the rest hide behind the presents
    for (gx, gy, gz, yaw, _, zit) in gs:
        if abs(gx - P(20, 0, 0)[0] - .5) < .1 and abs(gz - P(0, 0, 11)[2] - .5) < .1:
            npc(b, (int(gx), int(round(gy)), int(gz)), "onderwol_feestje", 180)
        else:
            b.guh(gx, gy, gz, yaw=yaw)
    # the render's knabbelkist: the party chest (hats and scarves)
    b.set(*P(26, VL, 26), LUCHT)
    kist(b, P(26, VL, 26), "north", LOOT["verrassingsfeestje"])
    b.save("onderwol/verrassingsfeestje/start")
    return b


# =====================================================================================================================
# r23 the Oude guhtempel
# =====================================================================================================================
TEMPEL_NAMEN = [["Guhbert", "de Eerste:", "vond de wol uit", ""], ["Njegrid", "de Wijze: telde", "alle knabbels", "(tot 3)"],
                ["Vadsimir:", "tilde ooit een", "heel kaaswiel", ""], ["Pluisje", "de Zachte:", "maakte het", "eerste kussen"],
                ["Kaasper: at de", "eerste knabbel", "(per ongeluk)", ""], ["Oma Guh: zei", "altijd: 'eerst", "knabbelen'", ""]]
BEELD_Z = (97, 88, 79)   # (the old guhs on the forecourt: 9 apart, the render had them 7 apart at a smaller size)
BEELD_WEG = {"minecraft:calcite", "minecraft:black_concrete", "minecraft:white_concrete", "minecraft:pink_concrete", "minecraft:smooth_quartz",
             "guhs:pleister_lavendel", "minecraft:gold_block", "guhs:block_of_kaasknabbels"}


def oude_guhtempel(h):
    scene = "r23_oude_guhtempel"
    v, _, pal, _, m = sc.laad(scene)
    VL = m["VL"]                                 # (the first air layer of the cave floor; the floor lies at VL - 1)
    PL = VL + 6                                  # (the temple platform: you stand on PL)
    H = 16
    TOP = VL + 36                                # (the dome is cut flat here)

    def aanpas(v, gezet, pal):
        def idx(s):
            if s not in pal:
                pal.append(s)
            return pal.index(s)
        rots = idx("guhs:guh_kristalsteen")
        # the dome: flat at TOP (a 40 high hall is plenty, and it keeps the rock above it)
        for y in range(TOP + 1, v.shape[1]):
            laag = v[:, y, :]
            laag[laag == 0] = rots
        # round 20: the render's white statues go (new, pinker ones are built after the guh model below)
        weg = [idx(s) for s in BEELD_WEG if s in pal]
        for (x0, x1, y0, y1, z0, z1) in [(36, 44, VL + 2, VL + 14, 78, 100), (66, 74, VL + 2, VL + 14, 78, 100),
                                         (44, 66, PL + 3, PL + 30, 21, 33)]:
            sub = v[x0:x1 + 1, y0:y1 + 1, z0:z1 + 1]
            sub[np.isin(sub, weg)] = 0
        # (the render's big statue poked through the shrine's roof: close it again)
        steen = idx("guhs:pastelbaksteen_muisgrijs")
        dak = v[35:76, PL + H + 2, 15:62]
        dak[dak == 0] = steen
        # (and their small sokkels: the new statues are bigger, further apart, on sokkels of their own)
        glad = idx("guhs:gladde_knuffelsteen")
        for z in (96, 89, 82):
            for x in (40, 70):
                sub = v[x - 1:x + 2, VL:VL + 2, z - 1:z + 2]
                sub[sub == glad] = 0
        # the pitfall: 3 deep (the render dug past the bottom of its box), with a soft floor and a ladder out
        wol, kussen = idx("guhs:pluiswolblok"), idx("guhs:c_pluiskussen")
        ladder = idx("minecraft:ladder[facing=north]")
        for x in range(53, 57):
            for z in range(84, 88):
                for y in range(VL - 3, VL - 1):
                    v[x, y, z] = 0
                v[x, VL - 4, z] = wol
                v[x, VL - 3, z] = kussen if (x + z) % 2 == 0 else 0
                gezet[x, VL - 4, z] = gezet[x, VL - 3, z] = True
        for y in range(VL - 3, VL):
            v[56, y, 88] = ladder
            gezet[56, y, 88] = True
        for y in range(0, VL - 5):
            v[:, y, :][v[:, y, :] == 0] = rots
        # the oak leaves (the laurel wreath in the secret room) must not decay
        if "minecraft:oak_leaves" in pal:
            pal[pal.index("minecraft:oak_leaves")] = "minecraft:oak_leaves[persistent=true,distance=7,waterlogged=false]"
        # the east wing: two chests, two blocks of nibbles (the render's four were all chests)
        kaas = idx("guhs:block_of_kaasknabbels")
        for (x, z) in ((79, 44), (87, 36)):
            v[x, PL, z] = kaas
    x0, z0 = 1, 0
    b, gs, _, P = omzet(scene, "oude_guhtempel", (x0, 109, 0, TOP + 2, z0, 108), aanpas=aanpas, echt=lambda x, y, z, gezet: gezet)
    rng = random.Random(23)
    anker_op(b, P(55, VL - 1, 80))
    ingang_op(b, P(60, VL - 1, 92))
    # the old guhs along the forecourt: pink, after the sitting guh model, on wider sokkels, looking at the path
    for k, zs in enumerate(BEELD_Z):
        for j, (xs, kijk) in enumerate(((40, "east"), (70, "west"))):
            for dx in (-2, -1, 0, 1, 2):
                for dz in (-2, -1, 0, 1, 2):
                    for y in (VL, VL + 1):
                        b.zet(*P(xs + dx, y, zs + dz), "gladde_knuffelsteen")
            guhbeeld(b, *P(xs, VL + 2, zs), 0.33, facing=kijk, oud=rng)
            nx = xs + (3 if xs < 55 else -3)
            bord(b, P(nx, VL, zs + 2), kijk, TEMPEL_NAMEN[2 * k + j])
    # the Grote Guh in the shrine, holding the Gouden Kaasknabbel (round 18; round 20: pinker, guh-shaped)
    dak = PL + H + 2
    cellen = guhbeeld(b, *P(55, PL + 3, 27), 0.45, facing="south", oud=rng)
    assert max(c[1] for c in cellen) < P(0, dak, 0)[1], "the Grote Guh fits under the shrine's roof"
    borst = PL + 4
    for k in range(5):
        for x in range(55 - (4 - k), 55 + (4 - k) + 1):
            for z in (30, 31):
                b.zet(*P(x, borst + k, z), "minecraft:gold_block" if (x * 3 + k) % 4 else "block_of_kaasknabbels")
    # signs
    bord(b, P(50, VL, 97), "south", ["Oude Guhtempel.", "Zachtjes lopen,", "de oude guhs", "slapen. Njeg!"])
    bord(b, P(48, VL, 97), "south", ["Pas op voor", "gaten in de", "vloer!", ""])
    bord(b, P(53, VL - 2, 86), "east", ["Njeg, gefopt!", "Trapje is daar.", "", ""], wall=True)
    bord(b, P(55, PL + H, 62), "south", ["Tempel van de", "Gouden", "Kaasknabbel", ""], wall=True)
    bord(b, P(57, PL, 51), "south", ["Oude puzzel:", "niemand weet", "meer hoe hij", "werkt. Kist mag!"])
    bord(b, P(58, PL, 33), "south", ["De Grote Guh", "met de Gouden", "Kaasknabbel.", ""])
    bord(b, P(50, PL, 32), "south", ["Niet aan de", "knabbel likken!", "", ""])
    bord(b, P(30, PL, 53), "south", ["Het lichtpad", "brandt al half.", "De rest? Njeg,", "te donker."])
    bord(b, P(25, PL, 28), "east", ["Geheime kamer!", "Sssst, niet", "doorvertellen!", ""])
    bord(b, P(78, PL, 32), "south", ["Uitgang?", "Glijbaan!", "(pootjes in het", "voertuig)"])
    bord(b, P(96, VL, 78), "south", ["Wieee! Einde", "glijbaan. Nog", "een keer? Eerst", "weer naar boven."])
    bord(b, P(92, PL + 9, 63), "south", ["Wieee!", "begint hier", "", ""])
    # the guide on the forecourt, the other visitors wild
    for (gx, gy, gz, yaw, _, zit) in gs:
        if abs(gx - P(55, 0, 0)[0] - .5) < .1 and abs(gz - P(0, 0, 90)[2] - .5) < .1:
            npc(b, (int(gx), int(round(gy)), int(gz)), "onderwol_tempel", 180)
        else:
            b.guh(gx, round(gy), gz, yaw=yaw)
    # chests: the courtyard (the puzzle's), the secret room (the best), the east wing
    for (pos, blok) in list(b.blocks.items()):
        if blok[0] == "minecraft:chest":
            x, y, z = pos
            sx, sz = x + x0, z + z0
            loot = LOOT["oude_guhtempel_geheim"] if sx < 27 else LOOT["oude_guhtempel"]
            b.kist(x, y, z, "west" if sx > 70 else "south", loot)
    b.save("onderwol/oude_guhtempel/start")
    return b


# =====================================================================================================================
# r24 the Oude bankkluis
# =====================================================================================================================
def oude_bankkluis(h):
    scene = "r24_oude_bankkluis"
    v, _, pal, _, m = sc.laad(scene)
    VL, X = m["VL"], v.shape[0]                  # (the floor lies at VL - 1)
    K1, K2, KZ1, KZ2 = 9, 37, 3, 16

    def aanpas(v, gezet, pal):
        def idx(s):
            if s not in pal:
                pal.append(s)
            return pal.index(s)
        # the gold of the facade becomes yellow (the round door keeps its gold); the stacks of vads are only the colour of vads
        goud, geel = idx("minecraft:gold_block"), idx("minecraft:yellow_concrete")
        gevel = v[:, VL + 6:VL + 8, KZ2]
        gevel[gevel == goud] = geel
        for oud, nieuw in (("guhs:compressed_super_vahoege_vads", "minecraft:purple_concrete"), ("guhs:block_of_vahoege_vads", "minecraft:purpur_block")):
            if oud in pal:
                v[v == pal.index(oud)] = idx(nieuw)
    b, gs, _, P = omzet(scene, "oude_bankkluis", (1, X - 1, VL - 2, VL + 11, 1, 37), aanpas=aanpas, echt=lambda x, y, z, gezet: gezet)
    anker_op(b, P(23, VL - 1, 27))
    ingang_op(b, P(10, VL - 1, 30))
    bord(b, P(31, VL, KZ2 + 2), "south", ["Bank Guh,", "sinds 1 njeg.", "", ""])
    bord(b, P(20, VL, 33), "north", ["Geen Mika's", "toegestaan.", "Ook niet met", "een snor."])
    bord(b, P(10, VL, 25), "south", ["Rente:", "0,0 knabbels.", "Wel gezellig.", ""])
    bord(b, P(13, VL, KZ2 + 2), "south", ["Code vergeten?", "Niet zo vads", "doen.", ""])
    bord(b, P(40, VL, 12), "south", ["Volgende keer", "beter. -M.", "", ""])
    bord(b, P(36, VL, 9), "west", ["Mika-proof!", "(alleen een", "deukje)", ""])
    bord(b, P(23, VL, 12), "south", ["Kluis open?", "Pak er gerust", "een. Of twee.", ""])
    for (gx, gy, gz, yaw, _, zit) in gs:
        if abs(gx - P(23, 0, 0)[0] - .5) < .1 and abs(gz - P(0, 0, 20)[2] - .5) < .1:
            npc(b, (int(gx), int(round(gy)), int(gz)), "onderwol_bank", 180)
        else:
            b.guh(gx, gy, gz, yaw=yaw)
    # the vault's knabbelkisten by the walls: two real chests; the hall's barrels and kist: loot
    for (x, z, f) in ((12, 11, "east"), (34, 13, "west")):
        b.set(*P(x, VL, z), LUCHT)
        kist(b, P(x, VL, z), f, LOOT["oude_bankkluis"])
    b.set(*P(40, VL, 28), LUCHT)
    kist(b, P(40, VL, 28), "north", LOOT["oude_bankkluis_hal"])
    for (x, z) in ((6, 30), (41, 21)):
        b.ton(*P(x, VL, z), LOOT["oude_bankkluis_hal"])
    b.save("onderwol/oude_bankkluis/start")
    return b


# =====================================================================================================================
# loot
# =====================================================================================================================
BOUWSTOF = s1.BOUWSTOF
SNACK = s1.SNACK
SJABLOON = {"rolls": 1, "entries": [{"type": "minecraft:loot_table", "value": "guhs:mijnhulp/super_vads_sjabloon"}]}
BOEKJE = [{"function": "minecraft:set_components", "components": {"minecraft:item_model": "guhs:spreukjesboekje",
                                                                   "minecraft:item_name": {"translate": "item.guhs.spreukjesboekje"}}}]


def boekjes(boekje, alle, leeg):
    """A pool of one roll: a Spreukjesboekje of a book-only spell (weight boekje), of any guh spell (alle), or nothing."""
    es = []
    for opties, w in (("#guhs:spreukjes/boekje", boekje), ("#guhs:spreukjes/alle", alle)):
        if w:
            es.append({"type": "minecraft:item", "name": "minecraft:book", "weight": w,
                       "functions": [{"function": "minecraft:enchant_randomly", "options": opties}] + BOEKJE})
    if leeg:
        es.append({"type": "minecraft:empty", "weight": leeg})
    return {"rolls": 1, "entries": es}


LOOT_DEF = {
    "grotdisco": [((2, 4), [("disco_tegel_roze", 3, 2, 6), ("disco_tegel_blauw", 3, 2, 6), ("disco_tegel_geel", 3, 2, 6), ("disco_tegel_groen", 3, 2, 6),
                            ("disco_dansvloer", 3, 2, 6), ("glimtegel", 3, 2, 6), ("guh_kristal_blok", 2, 2, 5)]), ((1, 3), SNACK),
                  (1, [("disco_bal", 2, 1, 1), ("disco_milkshake", 3, 1, 2), ("guh_kristal_lamp", 2, 1, 2), ("leeg", 4, 1, 1)])],
    "oude_wolspinnerij": [((2, 4), [("minecraft:pink_wool", 5, 4, 12), ("minecraft:magenta_wool", 3, 4, 10), ("minecraft:light_blue_wool", 3, 4, 10),
                                    ("minecraft:white_wool", 3, 4, 10), ("pluiswolblok", 4, 2, 8), ("minecraft:pink_carpet", 2, 4, 8),
                                    ("minecraft:string", 3, 3, 9)]), ((1, 2), SNACK),
                          (1, [("pluiskussen", 3, 1, 2), ("vadstouw", 2, 2, 6), ("knuffelblok_roze", 2, 2, 4), ("knuffel_normal", 1, 1, 1), ("leeg", 4, 1, 1)])],
    "sterrengrot": [((2, 3), [("sterrenplafond", 4, 4, 10), ("sterrenlantaarn", 3, 1, 2), ("minecraft:candle", 3, 1, 3), ("gladde_knuffelsteen", 3, 4, 8),
                              ("lichtmos", 2, 2, 6)]), ((1, 3), SNACK),
                    (1, [("knuffel_starry", 2, 1, 1), ("park_picknickkleed", 2, 2, 4), ("pluiskussen", 2, 1, 2), ("leeg", 4, 1, 1)])],
    "knuffelkuil": [((3, 5), [("knuffelblok_roze", 3, 2, 6), ("knuffelblok_aqua", 3, 2, 6), ("knuffelblok_boter", 3, 2, 6), ("knuffelblok_lavendel", 3, 2, 6),
                              ("knuffelblok_panda", 2, 2, 4), ("knuffelblok_tijger", 2, 2, 4), ("knuffelblok_zebra", 2, 2, 4), ("knuffelblok_giraffe", 2, 2, 4)]),
                    ((1, 2), [("knuffel_normal", 4, 1, 1), ("knuffel_starry", 2, 1, 1), ("knuffel_glitter", 2, 1, 1), ("knuffel_wolk", 2, 1, 1),
                              ("knuffel_rainbow", 1, 1, 1)]),
                    ((1, 2), SNACK), (1, [("safari_speelbal", 3, 1, 2), ("pluiskussen", 3, 1, 2), ("leeg", 3, 1, 1)])],
    "verrassingsfeestje": [((2, 4), [("feestslingers", 4, 2, 6), ("vlaggetjes", 4, 2, 6), ("knuffelblok_roze", 3, 2, 5), ("knuffelblok_vanille", 3, 2, 5),
                                     ("minecraft:pink_candle", 3, 1, 3), ("tegel_hartjes", 3, 4, 8)]),
                           ((1, 3), SNACK + [("guh_taart", 3, 1, 1)]),
                           (1, [("feesthoedje", 3, 1, 1), ("slinger_sjaaltje", 3, 1, 1), ("mini_luchtballon", 2, 1, 1), ("leeg", 3, 1, 1)])],
    "oude_guhtempel": [((2, 4), [("pastelbaksteen_muisgrijs", 4, 4, 12), ("pastelbaksteen_vanille", 4, 4, 12), ("gladde_knuffelsteen", 3, 4, 10),
                                 ("tegel_guhgezicht", 3, 2, 6), ("glasinlood_guh", 2, 1, 3), ("guh_kristal_blok", 2, 2, 6)]),
                       ((1, 3), SNACK + [("minecraft:gold_nugget", 3, 3, 9)]),
                       (1, [("vahoegiet", 2, 1, 2), ("guh_kristal_lamp", 2, 1, 2), ("minecraft:gold_ingot", 2, 1, 3), ("leeg", 3, 1, 1)])],
    "oude_guhtempel_geheim": [((2, 3), [("vahoegiet", 3, 1, 3), ("guh_kristal_blok", 3, 2, 6), ("minecraft:gold_ingot", 3, 2, 5),
                                        ("glasinlood_guh", 2, 1, 3)]), ((1, 2), SNACK),
                              (1, [("knabbelmachine_beeldje", 2, 1, 1), ("knuffel_golden", 1, 1, 1), ("block_of_kaasknabbels", 2, 1, 2)])],
    "oude_bankkluis": [((2, 4), [("guhrio_vadsmunt", 4, 1, 3), ("block_of_kaasknabbels", 4, 1, 3), ("minecraft:gold_ingot", 3, 2, 6),
                                 ("minecraft:gold_nugget", 3, 4, 12), ("pastelbaksteen_boter", 3, 4, 10), ("tegel_dambord", 3, 4, 10)]),
                       ((2, 3), SNACK), (1, [("vahoegiet", 2, 1, 2), ("vahoege_vads_ingot", 2, 1, 3), ("campingmarkt_vadsstapel", 2, 1, 1), ("leeg", 3, 1, 1)])],
    "oude_bankkluis_hal": [((2, 3), [("tegel_dambord", 4, 4, 10), ("pastelbaksteen_boter", 3, 4, 8), ("pluisbolletje", 3, 2, 5), ("minecraft:paper", 3, 2, 6),
                                     ("minecraft:gold_nugget", 2, 2, 6)]), ((1, 2), SNACK)],
}
# the extra pools: the super vads template (its own table: 1 in 4) and the Spreukjesboekjes
LOOT_EXTRA = {
    "verrassingsfeestje": [boekjes(1, 0, 5)],
    "oude_guhtempel": [SJABLOON, boekjes(2, 1, 3)],
    "oude_guhtempel_geheim": [SJABLOON, SJABLOON, boekjes(2, 2, 0)],
    "oude_bankkluis": [SJABLOON, boekjes(2, 1, 3)],
}
LOOT = {n: f"guhs:chests/onderwol/{n}" for n in LOOT_DEF}


def loot(h, naam):
    ob.loot(h, naam, LOOT_DEF[naam])
    if naam in LOOT_EXTRA:
        pad = f"{h.D}/loot_table/chests/onderwol/{naam}.json"
        data = json.load(open(pad, encoding="utf-8"))
        data["pools"] += LOOT_EXTRA[naam]
        h.w(pad, data)


# =====================================================================================================================
# the clothes of the party: feesthoedje + slinger-sjaaltje (GuhClothes FEESTHOEDJE, SLINGER_SJAALTJE: the existing party
# hat and scarf bones, our own textures)
# =====================================================================================================================
FEEST = [(246, 130, 180), (255, 214, 90), (140, 220, 200), (190, 160, 240), (130, 190, 250)]
KLEDING = {"feesthoedje": ("Feesthoedje", "Party Hat", "party_hat"), "slinger_sjaaltje": ("Slinger-sjaaltje", "Bunting Scarf", "winter_scarf")}


def _tex(h):
    return getattr(h, "TEX", os.path.join(h.A, "textures"))


def _bewaar(h, img, *pad):
    if hasattr(h, "save"):
        h.save(img, *pad)
    else:
        full = os.path.join(_tex(h), *pad)
        os.makedirs(os.path.dirname(full), exist_ok=True)
        img.save(full)


def kleding(h):
    from PIL import Image
    for naam, (_, _, bron) in KLEDING.items():
        img = Image.open(os.path.join(_tex(h), "entity", "guh_clothes", f"{bron}.png")).convert("RGBA")
        a = np.asarray(img).astype(np.int32).copy()
        hgt, wdt = a.shape[:2]
        licht = a[..., :3].mean(axis=2, keepdims=True) / 200.0
        ys, xs = np.mgrid[0:hgt, 0:wdt]
        if naam == "feesthoedje":
            band = ((xs // 8) + (ys // 8)) % len(FEEST)            # diagonal party stripes
        else:
            band = (xs // 4 + ((ys // 4) % 2) * 2) % len(FEEST)    # little flags in a row
        kleur = np.array(FEEST)[band]
        a[..., :3] = np.clip(kleur * np.clip(licht, 0.75, 1.15), 0, 255)
        _bewaar(h, Image.fromarray(a.astype(np.uint8)), "entity", "guh_clothes", f"{naam}.png")
    # the item icons (16 x 16)
    hoed = ["................", ".......y........", "......aab.......", "......abb.......", ".....abbca......", ".....bbcaa......",
            "....bbcaabb.....", "....bcaabbc.....", "...caabbccaa....", "...aabbccaabb...", "..abbccaabbcca..", "..dddddddddddd..",
            "................"]
    sjaal = ["................", "..dddddddddddd..", "..a.b.c.a.b.c.d.", "..aabbccaabbcc..", "...a..b..c..a...", "................",
             "..........dd....", "..........ad....", "..........bd....", "..........cd....", "..........ad....", "................"]
    kl = {"a": FEEST[0], "b": FEEST[1], "c": FEEST[2], "d": (150, 90, 160), "y": (255, 240, 150)}
    for naam, rijen in (("feesthoedje", hoed), ("slinger_sjaaltje", sjaal)):
        ic = np.zeros((16, 16, 4), np.uint8)
        top = (16 - len(rijen)) // 2
        for y, rij in enumerate(rijen):
            for x, ch in enumerate(rij[:16]):
                if ch in kl:
                    ic[top + y, x, :3] = kl[ch]
                    ic[top + y, x, 3] = 255
        _bewaar(h, Image.fromarray(ic), "item", f"{naam}.png")
        h.w(f"{h.A}/models/item/{naam}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{naam}"}})
        h.w(f"{h.A}/items/{naam}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}})
    for naam, (nl, en, _) in KLEDING.items():
        h.lang(f"item.guhs.{naam}", en, nl)
    h.lang("gui.guhs.kleding.prijs.hint.verrassingsfeestje_onder_de_wol", "in the chest of a Surprise Party under the wool",
           "in de kist van een Verrassingsfeestje onder de wol")


# =====================================================================================================================
# the structures
# =====================================================================================================================
STRUCTUREN = ["grotdisco", "oude_wolspinnerij", "sterrengrot", "knuffelkuil", "verrassingsfeestje", "oude_guhtempel", "oude_bankkluis"]


def aan_dorp(h, spin):
    """The Wolspinnerij also comes at the end of a side tunnel of the Mijnguhdorp (onderwol_structuren1 writes the pool)."""
    pad = f"{h.D}/worldgen/template_pool/onderwol/mijnguhdorp/zij.json"
    if not os.path.exists(pad):
        return
    data = json.load(open(pad, encoding="utf-8"))
    if not any(e["element"].get("location") == spin.locatie for e in data["elements"]):
        data["elements"].append({"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": spin.locatie,
                                                          "projection": "rigid", "processors": ob.PROCESSORS}})
        h.w(pad, data)


def bouw_alles(h):
    for naam in LOOT_DEF:
        loot(h, naam)
    biomes = ob.guhmension_biomes(h)
    disco = grotdisco(h)
    ob.grot_bouw(h, "grotdisco", templates=[(disco, 1)], titel="Grotdisco",
                 tooltip="Een geheime disco in een kristalgeode. DJ Njeg draait, de guhs dansen vadskracht bij elkaar.",
                 titel_en="Cave Disco", tooltip_en="A secret disco in a crystal geode. DJ Njeg spins, the guhs dance up vads power.",
                 salt=SALT, band=(14, 20), spacing=40, separation=14, ingang=dict(kans=0.5), biomes=biomes)
    spin = wolspinnerij(h)
    ob.grot_bouw(h, "oude_wolspinnerij", templates=[(spin, 1)], titel="Oude wolspinnerij",
                 tooltip="De Wolclub: guhs spinnen gezellig wol rond theetafeltjes. Een waterrad geeft vadskracht.",
                 titel_en="Old Wool Spinnery", tooltip_en="De Wolclub: guhs spin wool together around tea tables. A water wheel makes vads power.",
                 salt=SALT + 10, band=(44, 54), spacing=40, separation=14, ingang=dict(kans=0.5), biomes=biomes)
    aan_dorp(h, spin)
    ster = sterrengrot(h)
    ob.grot_bouw(h, "sterrengrot", templates=[(ster, 1)], titel="Sterrengrot",
                 tooltip="Een stille koepelgrot met een sterrenplafond, een picknick en een sterrenvijvertje.",
                 titel_en="Star Cave", tooltip_en="A quiet dome cave with a star ceiling, a picnic and a little star pond.",
                 salt=SALT + 20, band=(26, 38), spacing=36, separation=12, ingang=dict(kans=0.6), biomes=biomes)
    kuil = knuffelkuil(h)
    ob.grot_bouw(h, "knuffelkuil", templates=[(kuil, 1)], titel="Knuffelkuil",
                 tooltip="Een ballenbak vol knuffelblokken. Zes guhtjes spelen verstoppertje. Vind ze allemaal!",
                 titel_en="Plush Pit", tooltip_en="A ball pit full of plush blocks. Six little guhs play hide and seek. Find them all!",
                 salt=SALT + 30, band=(46, 56), spacing=40, separation=14, ingang=dict(kans=0.5), biomes=biomes)
    feest = verrassingsfeestje(h)
    ob.grot_bouw(h, "verrassingsfeestje", templates=[(feest, 1)], titel="Verrassingsfeestje",
                 tooltip="Een verborgen grot waar guhs een verjaardagsfeestje voorbereiden. Met een reuzentaart. Sssst!",
                 titel_en="Surprise Party", tooltip_en="A hidden cave where guhs prepare a birthday party. With a giant cake. Shhh!",
                 salt=SALT + 40, band=(26, 38), spacing=44, separation=16, ingang=dict(kans=0.5), biomes=biomes)
    tempel = oude_guhtempel(h)
    ob.grot_bouw(h, "oude_guhtempel", templates=[(tempel, 1)], titel="Oude guhtempel",
                 tooltip="Het enorme tempelcomplex van de oude guhs, half ingegraven. Met De Grote Guh en de Gouden Kaasknabbel.",
                 titel_en="Old Guh Temple", tooltip_en="The huge, half buried temple complex of the old guhs. With the Great Guh and the Golden Cheese Nibble.",
                 salt=SALT + 50, band=(15, 18), spacing=120, separation=48, ingang=dict(kans=1.0), voorrang=80, biomes=biomes)
    kluis = oude_bankkluis(h)
    ob.grot_bouw(h, "oude_bankkluis", templates=[(kluis, 1)], titel="Oude bankkluis",
                 tooltip="De oude kluis van de Bank Guh. Iemand heeft geprobeerd in te breken. Iemand met een snor.",
                 titel_en="Old Bank Vault", tooltip_en="The old vault of the Bank Guh. Someone tried to break in. Someone with a moustache.",
                 salt=SALT + 60, band=(22, 34), spacing=72, separation=28, ingang=dict(kans=0.6), biomes=biomes)
    kleding(h)
    for plek, (_, regels) in PRAAT.items():
        for i, (nl, en) in enumerate(zip(regels, PRAAT_EN[plek]), start=1):
            h.lang(f"quest.guhs.onderwol_bouw.{plek}.{i}", en, nl)


def selfcheck(h):
    """Every block of every template exists with known properties; every loot entry is an item; the texts line up."""
    fouten = []
    seen = {}
    for b in BOUWSELS:
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
                extra = {k for k, _ in props} - seen[naam] - {"waterlogged"}
                if extra and seen[naam]:
                    fouten.append(f"{b.naam}: {naam} has no property {sorted(extra)}")
    for naam, pools in LOOT_DEF.items():
        for _, entries in pools:
            for item, *_ in entries:
                if item != "leeg" and ":" not in item and not os.path.exists(f"{h.A}/items/{item}.json") and item not in KLEDING:
                    fouten.append(f"loot {naam}: item guhs:{item} does not exist")
    for plek, (_, regels) in PRAAT.items():
        if len(regels) != len(PRAAT_EN[plek]):
            fouten.append(f"{plek}: {len(regels)} Dutch lines, {len(PRAAT_EN[plek])} English")
    return sorted(set(fouten))


BOUWSELS = []


def _bewaar_bouw(b):
    if b not in BOUWSELS:
        BOUWSELS.append(b)


_save = ob.Bouw.save


def _save3(self, name):
    _save(self, name)
    if name.split("/")[1:2] and name.split("/")[1] in STRUCTUREN:
        _bewaar_bouw(self)


ob.Bouw.save = _save3


def build(h):
    bouw_alles(h)
    for f in selfcheck(h):
        print("onderwol_structuren3:", f)


# =====================================================================================================================
# standalone: python tools/features/onderwol_structuren3.py from the project root
# =====================================================================================================================
EN_OVERLAY = os.path.join("tools", "lang", "en", "c196_onderwol_structuren3.json")


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
        if k.startswith(PREFIX + "."):
            NL[k] = nl
            EN[k] = EN_REGELS.get(nl, nl)
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        w(p, data)
    w(EN_OVERLAY, dict(sorted(EN.items())))
    print(f"onderwol_structuren3: {len(BOUWSELS)} templates, {len(NL)} texts")


if __name__ == "__main__":
    import sys
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    _direct()
