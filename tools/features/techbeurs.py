"""
1.7.0 (design: MineScapeStory/ontwerp-guhs170-mijnen.md ronde 45-47 "Vadskracht in de structuren"; Java:
feature/techbeurs/Beurs + BeursBeheer): de Guhtechniek-beurs, a protected exhibition hall with a glass roof on the surface of
the Guhmension. Every part of the Guh-technologie has a stand that really works: a sign with its vadskracht, a
Vadsschakelaar to switch it off and on, a beursguh with a joke and a tip (which quest teaches it) and a Vadslamp. The middle
holds the Droomfabriek (kaasknabbelplantjes -> Oogster -> buizen + Filterstuk -> Frituurautomaat -> Brouwautomaat ->
drankjes in a kist, on its own Gloeisterkern).

THE HALL (one template, 74 x 14 x 42; x east, z south; y 0 foundation, y 1 floor, you walk at y 2, roof at y 11):

    z  0      wall
    z  2..10  north row (front to the south): Guhrad, Knuffelgenerator, Disco-dynamo, Blubkacheltje + Knabbelbatterij,
              Gloeisterkern (the kern that powers the hall's bus), Vadsmolen + Guhoven, Oogster
    z 11..13  aisle
    z 14..27  foyer + entrance (west wall), the Droomfabriek, the Stepstation with its Bezorgguhtje (east)
    z 28..30  aisle
    z 31..39  south row (front to the north): Knabbelbuizen + Filterstuk + Voorraadmeter, Opzuiger + Guhklok, Knabbelaar +
              Neerzetter, two Knutselmachines + Tekentafel, Plantagebak, Sauspomp + Sausvat, Brouw-/Frituurautomaat + Grillkoolpers
    z 41      wall
  The bus (Guhdraad on the floor): the Gloeisterkern of the kern stand -> along z 1 to x 72 -> down x 72 -> along z 40 behind
  the south row. Every machine stand hangs on it through its own Vadsschakelaar (front left corner); the source stands have
  their own net (source -> schakelaar -> lampen). check_netten() proves in python that every schakelaar really cuts.

Everything keeps running for ever through the hidden BeursBeheer under the floor (NeoForgeData.guhs_beurs on the block
entities it looks after: Vul / Leeg / LeegSaus / Station / Hak / Guhs) and through closed loops (chest ping-pong, the
Knabbelaar + Neerzetter, the two Knutselmachines, the Opzuiger + dropper).

Writes: the template techbeurs/guhtechniek_beurs, the structure guhs:guhtechniek_beurs (guhs:flat_jigsaw), its pool, its two
structure sets (gegarandeerd 800-1500, sector 7 of 16, alleen_nieuw; and rare random copies), its biome tag, the textures and
model of the hidden beurs_beheer, and every text (signs, beursguhs, Superkompas; English in tools/lang/en/c205_techbeurs.json).

Standalone and idempotent:   python tools/features/techbeurs.py     (from the project root)
"""
import json
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
import make_structures as ms  # noqa: E402
import sign_text  # noqa: E402
from features import onderwol_bouw as ob  # noqa: E402
from features import onderwol_vadskracht as ov  # noqa: E402
from features import vadskracht as vk  # noqa: E402

NAAM = "guhtechniek_beurs"
TEMPLATE = f"techbeurs/{NAAM}"
PREFIX = "sign.guhs.techbeurs"
X, Y, Z = 74, 14, 42
VL = 1          # the floor
Y0 = 2          # where you walk
DAK = 11        # the glass roof
BEHEER = (37, 0, 21)
SALT = 21705001
EN_OVERLAY = os.path.join("tools", "lang", "en", "c205_techbeurs.json")

DRAAD, SCHAKELAAR, LAMP = "guhs:guh_wire", "guhs:vadsschakelaar", "guhs:vadslamp"
BUIS, RICHTING, FILTER = "guhs:knabbelbuis", "guhs:knabbelbuis_richting", "guhs:knabbelbuis_filter"
VLOER = "minecraft:white_concrete"
VLOER_PAD = "minecraft:pink_glazed_terracotta"
STAND_VLOER = {"bron": "minecraft:pink_concrete", "machine": "minecraft:light_blue_concrete", "saus": "minecraft:yellow_concrete",
               "droom": "minecraft:magenta_concrete", "bezorg": "minecraft:lime_concrete"}

# =====================================================================================================================
# rotation of the south row (the stands are written in a local frame: lx 0..8 left -> right as the visitor sees it, lz 0..8
# back -> front, the visitor stands south of lz 8; the north row is that frame as it is, the south row turned 180 degrees)
# =====================================================================================================================
DRAAI = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "up", "down": "down"}
CW = {"north": "east", "east": "south", "south": "west", "west": "north"}
TEGEN = {"north": "south", "south": "north", "east": "west", "west": "east"}
VEC = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}


def _blokstr(naam, props):
    return naam + ("[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]" if props else "")


def _draai_props(props):
    out = {}
    for k, v in props.items():
        if k in ("facing",) and v in DRAAI:
            out[k] = DRAAI[v]
        elif k in ("north", "south", "east", "west"):
            out[DRAAI[k]] = v
        elif k in ("dx", "dz"):
            out[k] = str(4 - int(v))
        else:
            out[k] = v
    if "stuk" in props:
        i = ["nw", "n", "no", "w", "midden", "o", "zw", "z", "zo"].index(props["stuk"])
        dx, dz = -(i % 3 - 1), -(i // 3 - 1)
        out["stuk"] = ["nw", "n", "no", "w", "midden", "o", "zw", "z", "zo"][(dz + 1) * 3 + dx + 1]
    return out


class Stand:
    """A stand of 9 x 9 in its local frame (see above); everything it sets goes through the turn of its row."""

    def __init__(self, b, x0, z0, rij, soort, naam):
        self.b, self.x0, self.z0, self.rij, self.soort, self.naam = b, x0, z0, rij, soort, naam
        for lx in range(9):
            for lz in range(9):
                self.zet(lx, -1, lz, STAND_VLOER[soort])

    def g(self, lx, ly, lz):
        """Local -> template coordinates."""
        if self.rij == "N":
            return self.x0 + lx, Y0 + ly, self.z0 + lz
        return self.x0 + 8 - lx, Y0 + ly, self.z0 + 8 - lz

    def f(self, facing):
        return facing if self.rij == "N" else DRAAI[facing]

    def zet(self, lx, ly, lz, s, nbt=None):
        naam, props = ob._blok(s)
        if self.rij == "S":
            props = _draai_props(props)
        self.b.set(*self.g(lx, ly, lz), naam, props, nbt)

    def draad(self, *punten):
        for (lx, lz) in punten:
            self.zet(lx, 0, lz, DRAAD)

    def lijn(self, a, b_):
        """Guhdraad on a straight line between two local spots (inclusive)."""
        (x1, z1), (x2, z2) = a, b_
        for lx in range(min(x1, x2), max(x1, x2) + 1):
            for lz in range(min(z1, z2), max(z1, z2) + 1):
                self.zet(lx, 0, lz, DRAAD)

    def schakelaar(self, lx=0, lz=7, facing="south"):
        self.zet(lx, 0, lz, f"{SCHAKELAAR}[facing={facing},powered=true]")
        self.b.schakelaars.append((self.naam, self.g(lx, 0, lz)))

    def lamp(self, lx, lz, ly=0, facing="south"):
        self.zet(lx, ly, lz, f"{LAMP}[facing={facing},snoet=slaapt]", {"id": LAMP})

    def machine(self, lx, lz, naam, facing="south", nbt=None, ly=0, extra=None):
        props = {"facing": facing, **(extra or {})}
        if naam not in ("tekentafel",):
            props.setdefault("snoet", "slaapt")
        self.zet(lx, ly, lz, _blokstr(f"guhs:{naam}", props), dict({"id": f"guhs:{naam}"}, **(nbt or {})) if naam != "tekentafel" else None)

    def meer(self, lx, lz, naam, facing, breed, hoog, diep, deel, nbt=None, extra=None, deel_props=None):
        """A machine bigger than one block: the kern here and every part (MachineDeelBlock: dx, dy, dz = offset + 2 in
        template axes), worked out in template coordinates after the turn."""
        self.machine(lx, lz, naam, facing, nbt, extra=extra)
        kx, ky, kz = self.g(lx, 0, lz)
        gf = self.f(facing)
        zx, zz = VEC[CW[gf]]
        ax, az = VEC[TEGEN[gf]]
        for s in range(-(breed - 1) // 2, breed // 2 + 1):
            for h in range(hoog):
                for d in range(diep):
                    if s == 0 and h == 0 and d == 0:
                        continue
                    dx, dz = zx * s + ax * d, zz * s + az * d
                    props = {"dx": str(dx + 2), "dy": str(h), "dz": str(dz + 2)}
                    if deel_props:
                        props.update(deel_props(dx, dz, gf))
                    self.b.set(kx + dx, ky + h, kz + dz, deel, props)

    def bord(self, lx, lz, regels, facing="south"):
        """A standing sign (the visitor reads it from the front)."""
        self.b.sign(*self.g(lx, 0, lz), self.f(facing), regels, wall=False, wood="cherry")

    def guh_npc(self, lx, lz, yaw=0.0):
        x, y, z = self.g(lx, 0, lz)
        kind = KINDEN[self.naam]
        if self.rij == "S":
            yaw += 180.0
        self.b.npc(x, y, z, f"techbeurs_{self.naam}", yaw, kind=kind)

    def entity(self, lx, ly, lz, nbt):
        if self.rij == "N":
            x, z = self.x0 + lx, self.z0 + lz
        else:
            x, z = self.x0 + 9 - lx, self.z0 + 9 - lz
        self.b.entity(x, Y0 + ly, z, nbt)

    def merk(self, lx, lz, ly=0, **regels):
        """The BeursBeheer mark on the block entity here (NeoForgeData.guhs_beurs)."""
        p = self.g(lx, ly, lz)
        naam, props, nbt = self.b.blocks[p]
        nbt = dict(nbt or {})
        nbt["NeoForgeData"] = {"guhs_beurs": _merk(**regels)}
        self.b.blocks[p] = (naam, props, nbt)


def _merk(**regels):
    out = {}
    for k, v in regels.items():
        if k == "Vul":
            out["Vul"] = ms.compounds([{"id": i, "count": n} for (i, n) in v])
        elif isinstance(v, bool):
            out[k] = ms.Byte(1 if v else 0)
        else:
            out[k] = int(v)
    return out


def _vakken(stapels, slots):
    return {"stacks": ov._items(stapels, slots)}


def tekening(rooster, resultaat, n):
    """A Bouwtekening (26.1 component form: the guhs block entity NBT is not upgraded by the DataFixer)."""
    return {"id": "guhs:bouwtekening", "count": 1, "components": {"guhs:bouwtekening": {
        "rooster": ms.compounds([{"id": r, "count": 1} if r else {} for r in rooster]),
        "resultaat": {"id": resultaat, "count": n}}}}


# =====================================================================================================================
# the beursguhs (Java Beurs.PRAAT has the same kinds and line counts)
# =====================================================================================================================
KINDEN = {"welkom": "loketguh", "guhrad": "uitvinderguh", "knuffel": "kraamguh", "disco": "kraamguh", "blub": "werktuigguh",
          "kern": "uitvinderguh", "molen": "werktuigguh", "oogster": "kraamguh", "buizen": "werktuigguh", "opzuiger": "kraamguh",
          "knabbelaar": "werktuigguh", "knutsel": "uitvinderguh", "plantagebak": "kraamguh", "pomp": "werktuigguh", "sauzen": "kraamguh",
          "bezorg": "loketguh", "droom": "uitvinderguh"}

PRAAT = {  # stand -> [(Dutch, English), ...]: line 1 a joke, line 2 the tip (which quest teaches it)
    "welkom": [
        ("Welkom op de Guhtechniek-beurs! Alles hier draait echt. Zet de schakelaartjes gerust uit en weer aan, njeg!",
         "Welcome to the Guh Technology Fair! Everything here really runs. Go ahead and flip the little switches off and on again, nyeg!"),
        ("Kijken mag, pakken niet: wat in de stands zit, is van de beurs. De beursguhs vertellen je hoe je het zelf bouwt.",
         "Looking is fine, taking is not: what's in the stands belongs to the fair. The fair guhs tell you how to build it yourself."),
        ("In het midden staat de Droomfabriek: daar werkt alles samen. Mijn lievelingsstand!",
         "In the middle is the Dream Factory: everything works together there. My favourite stand!")],
    "guhrad": [
        ("Mijn guh rent al de hele dag. Hij denkt dat er aan het eind een knabbel ligt. Niet verklappen!",
         "My guh has been running all day. He thinks there's a nibble at the end. Don't tell him!"),
        ("Een Guhrad bouw je zelf: begin bij het quest 'Een guh aan het werk' in Guh-technologie. Een blij guhtje geeft 15 in plaats van 10.",
         "You build a Guh Wheel yourself: start at the quest 'A Guh at Work' in Guh Technology. A happy guh gives 15 instead of 10.")],
    "knuffel": [
        ("Dit is de enige generator die je mag knuffelen. Bij de andere zou ik het niet doen.",
         "This is the only generator you're allowed to cuddle. I wouldn't try it with the others."),
        ("Quest 'Blije guhs rennen harder' (Project 1): tamme guhs komen vanzelf op het kussen liggen, elk geeft 3 vadskracht.",
         "Quest 'Happy Guhs Run Faster' (Project 1): tame guhs come and lie on the cushion by themselves, each gives 3 chonk power.")],
    "disco": [
        ("Deze guhs dansen al sinds de opening. De plaat is nog steeds niet afgelopen.",
         "These guhs have been dancing since the opening. The record still hasn't ended."),
        ("Quest 'Dansen voor de fabriek' (Project 1): klik met een muziekplaat op je Disco-dynamo en tot 4 guhs komen dansen.",
         "Quest 'Dancing for the Factory' (Project 1): click your Disco Dynamo with a music disc and up to 4 guhs come dancing.")],
    "blub": [
        ("Het kacheltje eet kaasknabbels. Ik ook. We delen niet.",
         "The little stove eats cheese nibbles. So do I. We don't share."),
        ("Zet de schakelaar uit: dan houdt de Knabbelbatterij de lampjes aan! 'Het Blubkacheltje' staat in Project 3, de batterij in Project 2.",
         "Turn the switch off: the Nibble Battery keeps the lamps on! 'The Blub Stove' is in Project 3, the battery in Project 2.")],
    "kern": [
        ("Een echte ster, in een kooitje. 's Avonds mag hij even naar buiten om te twinkelen.",
         "A real star, in a little cage. In the evening he's allowed out for a bit to twinkle."),
        ("Deze kern laat de halve beurs draaien: 200 vadskracht! Je bouwt hem aan het eind van De Grote Knabbelmachine, met een gloeister.",
         "This core runs half the fair: 200 chonk power! You build it at the end of The Great Nibble Machine, with a glowstar.")],
    "molen": [
        ("Malen zonder wind! De molenaar van hiernaast is een beetje jaloers.",
         "Milling without wind! The miller next door is a little jealous."),
        ("Quest 'Malen zonder wind' voor de Vadsmolen en 'De oven met een snoetje' voor de Guhoven, allebei in Project 1.",
         "Quest 'Milling Without Wind' for the Chonk Mill and 'The Oven with a Little Snoot' for the Guh Oven, both in Project 1.")],
    "oogster": [
        ("Hij oogst, hij plant opnieuw en hij klaagt nooit. Ik wou dat ik zo was.",
         "He harvests, he plants again and he never complains. I wish I were like that."),
        ("Quest 'De akker die zichzelf oogst' (Project 2). Zet hem met zijn snoetje naar je akker, met water en licht erbij.",
         "Quest 'The Field That Harvests Itself' (Project 2). Point his snoot at your field, with water and light nearby.")],
    "buizen": [
        ("De knabbels gaan de hele dag heen en weer. Ze hebben het reuze naar hun zin.",
         "The nibbles go back and forth all day. They're having a great time."),
        ("Begin met 'Van kist naar kist' en 'Het Filterstuk' in Project 2; de Voorraadmeter staat bij 'Genoeg is genoeg'.",
         "Start with 'From Chest to Chest' and 'The Filter Piece' in Project 2; the Stock Meter is at 'Enough Is Enough'.")],
    "opzuiger": [
        ("Laat hier niks vallen. Echt niet. Hij is sneller dan jij.",
         "Don't drop anything here. Really. He's faster than you."),
        ("Quest 'De Opzuiger' (Project 2). De Guhklok die het droppertje aantikt is een sensor uit 'Genoeg is genoeg'.",
         "Quest 'The Slurper' (Project 2). The Guh Clock that taps the dropper is a sensor from 'Enough Is Enough'.")],
    "knabbelaar": [
        ("De een zet een steen neer, de ander knabbelt hem op. Echte vriendschap!",
         "One puts a stone down, the other nibbles it up. True friendship!"),
        ("Haal eerst de receptkaart bij 'Knabbelende machines' (Project 4), dan 'Steen zonder houweel' en 'De Neerzetter'.",
         "First get the recipe card at 'Nibbling Machines' (Project 4), then 'Stone Without a Pickaxe' and 'The Placer'.")],
    "knutsel": [
        ("Negen knabbels erin, één blok eruit, en weer terug. Ze zijn er erg goed in.",
         "Nine nibbles in, one block out, and back again. They're very good at it."),
        ("Teken een recept op de Tekentafel ('De Tekentafel') en stop de tekening in de Knutselmachine ('Knutselen zonder handen').",
         "Draw a recipe on the Drawing Table ('The Drawing Table') and put the drawing in the Tinker Machine ('Tinkering Without Hands').")],
    "plantagebak": [
        ("Een heel bos in een bak. Nou ja, één boom. Maar wel een hele mooie.",
         "A whole forest in a box. Well, one tree. But a very nice one."),
        ("De receptkaart verdien je op de Grillcamping; quest 'Een bos in een bak' (Project 4). Hak de boom om en hij plant een nieuwe.",
         "You earn the recipe card at the Grill Camping; quest 'A Forest in a Box' (Project 4). Chop the tree down and it plants a new one.")],
    "pomp": [
        ("De bron raakt nooit op. Daar zijn we hier heel trots op, njeg.",
         "The source never runs dry. We're very proud of that here, nyeg."),
        ("Pompen, slangen en vaten staan in Project 3, 'Saus uit de kraan'. Begin bij de receptkaart 'Saus en slangen'.",
         "Pumps, hoses and vats are in Project 3, 'Sauce on Tap'. Start at the recipe card 'Sauce and Hoses'.")],
    "sauzen": [
        ("Drankjes, frituur en grillkool uit één en dezelfde slang. Dat noem ik nou een sausstraat!",
         "Potions, fried snacks and grill coal from one and the same hose. Now that's what I call a sauce street!"),
        ("Zie 'Frituur aan de lopende band', 'Drankjes die zichzelf brouwen' en 'De grillkoollijn' in Project 3.",
         "See 'Fry-Up on the Assembly Line', 'Potions That Brew Themselves' and 'The Grill Coal Line' in Project 3.")],
    "bezorg": [
        ("Ons Bezorgguhtje brengt alles rond. Behalve het weekend, dat houdt hij zelf.",
         "Our Delivery Guhling delivers everything. Except the weekend, he keeps that for himself."),
        ("Receptkaart bij 'Het Bezorgguhtje' (Project 4), dan 'Het Stepstation' en 'Haltepaaltjes': groen haalt op, oranje brengt.",
         "Recipe card at 'The Delivery Guhling' (Project 4), then 'The Scooter Station' and 'Stop Posts': green picks up, orange delivers.")],
    "droom": [
        ("Dit is de Droomfabriek: kaasknabbelplantjes erin, gefrituurde knabbels en vahoege drankjes eruit!",
         "This is the Dream Factory: cheese nibble plants in, fried nibbles and vahoege potions out!"),
        ("Elke machine hier heeft ook een eigen stand op de beurs. Bouw ze allemaal en maak thuis je eigen droomfabriek.",
         "Every machine here also has its own stand at the fair. Build them all and make your own dream factory at home."),
        ("Zet de grote schakelaar bij de kern uit en kijk hoe alles in slaap valt. Wel weer aanzetten, hoor!",
         "Turn off the big switch by the core and watch everything fall asleep. Do switch it back on, though!")],
}

# the Dutch sign lines -> English
EN_REGELS = {
    "Dit is een": "This is a", "Dit zijn": "These are", "vadskracht": "chonk power", "Guhrad": "Guh Wheel", "Geeft 15": "Gives 15",
    "Knuffelgenerator": "Cuddle Generator", "Geeft 3 per guh": "Gives 3 per guh", "Disco-dynamo": "Disco Dynamo",
    "Geeft 5 per": "Gives 5 per", "dansende guh": "dancing guh", "Blubkacheltje": "Blub Stove", "Geeft 6": "Gives 6",
    "Knabbelbatterij": "Nibble Battery", "Bewaart 18000": "Stores 18000", "Gloeisterkern": "Glowstar Core", "Geeft 200": "Gives 200",
    "Vadsmolen": "Chonk Mill", "Guhoven": "Guh Oven", "Oogster": "Harvester", "Knabbelbuizen": "Nibble Tubes",
    "+ Filterstuk": "+ Filter Piece", "Voorraadmeter": "Stock Meter", "Opzuiger": "Slurper", "Guhklok": "Guh Clock",
    "Knabbelaar": "Nibbler", "Neerzetter": "Placer", "Twee": "Two", "Knutselmachines": "Tinker Machines",
    "vadskracht elk": "chonk power each", "Tekentafel": "Drawing Table", "Tekent je": "Draws your", "recepten": "recipes",
    "Plantagebak": "Plantation Box", "Sauspomp": "Sauce Pump", "Sausvat": "Sauce Vat", "Geen vadskracht": "Needs no",
    "nodig": "chonk power", "Brouwautomaat": "Auto Brewer", "Frituurautomaat": "Auto Fryer", "Grillkoolpers": "Grill Coal Press",
    "Stepstation": "Scooter Station", "De": "The", "Droomfabriek": "Dream Factory", "Alles werkt": "Everything works",
    "samen!": "together!", "Welkom op de": "Welcome to the", "Guhtechniek-": "Guh Technology", "beurs!": "Fair!",
    "Kijken mag,": "Looking: yes,", "pakken niet!": "taking: no!", "Knopje = aan/uit": "Switch = on/off", "Uitgang": "Exit",
    "Ingang": "Entrance", "Haalt op": "Picks up", "Brengt": "Delivers", "Kaasknabbels": "Cheese nibbles",
    "Gefrituurd!": "Fried!", "Drankjes": "Potions", "Zaadjes": "Seeds", "Klaar!": "Done!", "Hij laat de": "It runs half",
    "halve beurs": "the fair:", "draaien!": "look at the bus!", "op 1": "on 1", "op 2": "on 2", "op 3": "on 3", "op 4": "on 4",
    "op 5": "on 5", "op 6": "on 6", "op 8": "on 8", "op 10": "on 10", "Grote schakelaar": "Big switch", "van de fabriek": "of the factory",
    "Guhtechniek-beurs": "Guh Tech Fair", "Beurscafé": "Fair Café", "Kaasshakes:": "Cheese shakes:", "helaas op": "sold out, sorry",
}


def op(naam):
    return f"op {vk.getal(naam)}"


# =====================================================================================================================
# the hall
# =====================================================================================================================
def hal(b):
    """Foundation, floor, walls with windows and pillars, the glass roof with beams, the entrance in the west wall."""
    b.fill(0, 0, 0, X - 1, 0, Z - 1, "minecraft:smooth_stone")
    b.fill(0, VL, 0, X - 1, VL, Z - 1, VLOER)
    for x in range(1, X - 1):           # the aisles: a pink path
        for z in list(range(11, 14)) + list(range(28, 31)):
            b.set(x, VL, z, VLOER_PAD, {"facing": "north" if (x + z) % 2 else "east"})
    for x in range(X):
        for z in range(Z):
            rand = x in (0, X - 1) or z in (0, Z - 1)
            if not rand:
                continue
            pilaar = (x % 10 == 1 and z in (0, Z - 1)) or (z % 10 == 0 and x in (0, X - 1)) or (x in (0, X - 1) and z in (0, Z - 1))
            for y in range(Y0, DAK):
                if pilaar:
                    b.set(x, y, z, "minecraft:quartz_pillar", {"axis": "y"})
                elif y == Y0 or y == DAK - 1:
                    b.set(x, y, z, "minecraft:pink_terracotta")
                elif 4 <= y <= 8:
                    b.set(x, y, z, "minecraft:pink_stained_glass")
                else:
                    b.set(x, y, z, "minecraft:pink_concrete")
    # the roof: glass in white and pink stripes, quartz beams over the pillars, a raised glass ridge in the middle
    for x in range(X):
        for z in range(Z):
            balk = x % 10 == 1 or x in (0, X - 1) or z in (0, Z - 1, 11, 30)
            if balk:
                b.set(x, DAK, z, "minecraft:smooth_quartz")
            else:
                b.set(x, DAK, z, "minecraft:white_stained_glass" if (z // 3) % 2 else "minecraft:pink_stained_glass")
    for x in range(X):
        for z in range(12, 30):
            b.set(x, DAK, z, "minecraft:air")
            b.set(x, DAK + 1, z, "minecraft:smooth_quartz" if x % 10 == 1 or z in (12, 29) else "minecraft:light_blue_stained_glass")
        for z in (12, 29):
            b.set(x, DAK, z, "minecraft:smooth_quartz")
    for z in range(12, 30):
        for x in (0, X - 1):
            b.set(x, DAK, z, "minecraft:pink_concrete")
    # the entrance: an arch in the west wall, a mat, a sign above it
    for z in range(19, 23):
        for y in range(Y0, Y0 + 4):
            b.set(0, y, z, "minecraft:air")
        b.set(0, Y0 + 4, z, "minecraft:quartz_stairs", {"facing": "east", "half": "top", "shape": "straight", "waterlogged": "false"})
        b.set(0, VL, z, "minecraft:pink_glazed_terracotta", {"facing": "east"})
    b.set(0, Y0 + 5, 20, "minecraft:smooth_quartz")
    b.set(0, Y0 + 5, 21, "minecraft:smooth_quartz")
    # air everywhere inside that nothing else claims (a structure in hilly ground must clear the hall)
    for x in range(1, X - 1):
        for z in range(1, Z - 1):
            for y in range(Y0, DAK):
                if (x, y, z) not in b.blocks:
                    b.set(x, y, z, "minecraft:air")
    for x in range(1, X - 1):
        for z in range(12, 30):
            if (x, DAK, z) not in b.blocks:
                b.set(x, DAK, z, "minecraft:air")


def gevel(b):
    """The name over the entrance, on the inside of the arch."""
    b.sign(1, Y0 + 4, 21, "east", ["Welkom op de", "Guhtechniek-", "beurs!", ""], wall=True, wood="cherry")


# =====================================================================================================================
# the stands
# =====================================================================================================================
def bus_aansluiting(s):
    """A machine stand on the bus: the bus lies behind the stand (lz -1); upstream along lx 0 to the switch at (0, 7)."""
    s.lijn((0, 0), (0, 6))
    s.schakelaar(0, 7)
    s.draad((1, 7), (2, 7))


def stand_guhrad(s):
    ov.guhrad(s.b, *s.g(4, 0, 3), s.f("south"))
    s.b.guhraden.append(s.g(4, 0, 3))
    s.draad((2, 3), (1, 3), (0, 3), (0, 4), (0, 5), (0, 6))
    s.schakelaar(0, 7)
    s.lijn((1, 7), (7, 7))
    for lx in (3, 5, 7):
        s.lamp(lx, 6)
    s.bord(1, 8, ["Dit is een", "Guhrad", f"Geeft {vk.getal('GUHRAD_BLIJ')}", "vadskracht"])
    s.guh_npc(8, 5)


def stand_knuffel(s):
    s.meer(5, 4, "knuffelgenerator", "south", 2, 1, 2, "guhs:techbron_kussen_deel")
    s.merk(5, 4, Guhs=4)
    for (ex, ez) in ((4.5, 3.5), (5.5, 3.5), (4.5, 4.5), (5.5, 4.5)):
        nbt = ms.guh_nbt(1.0, NoAI=ms.Byte(1))
        s.entity(ex, 0.375, ez, nbt)
    # (the kern is at 5,4; its parts at 4,4 / 5,3 / 4,3 (width to the west))
    s.draad((6, 4), (7, 4), (7, 5), (7, 6))
    s.lijn((7, 6), (7, 6))
    s.draad((8, 6))
    s.zet(8, 0, 7, f"{SCHAKELAAR}[facing=south,powered=true]")
    s.b.schakelaars.append((s.naam, s.g(8, 0, 7)))
    s.lijn((1, 8), (7, 8))
    s.draad((8, 8))
    for lx in (2, 4, 6):
        s.lamp(lx, 7)
    s.bord(1, 6, ["Dit is een", "Knuffelgenerator", "Geeft 3 per guh", "vadskracht"])
    s.guh_npc(1, 4, yaw=-90)


def stand_disco(s):
    s.meer(4, 5, "disco_dynamo", "south", 3, 1, 3, "guhs:techbron_vloer_deel",
           nbt={"Plaat": {"id": "minecraft:music_disc_cat", "count": 1}})
    s.merk(4, 5, Guhs=4)
    for (ex, ez) in ((3.5, 3.5), (5.5, 3.5), (3.5, 4.5), (5.5, 4.5)):
        s.entity(ex, 0.25, ez, ms.guh_nbt(1.0, NoAI=ms.Byte(1)))
    s.draad((2, 5), (1, 5), (0, 5), (0, 6))
    s.schakelaar(0, 7)
    s.lijn((1, 7), (7, 7))
    for lx in (6, 7):
        s.lamp(lx, 6)
    s.lamp(2, 8)
    s.bord(1, 8, ["Dit is een", "Disco-dynamo", "Geeft 5 per", "dansende guh"])
    s.guh_npc(8, 5)


def stand_blub(s):
    s.machine(3, 3, "blubkacheltje", nbt={"Blubje": {"id": "guhs:sausblubje_potje", "count": 1}, "Warm": 2147483647,
                                          "Bakje": _vakken([(0, "guhs:kaas_knabbels", 16)], 1)})
    s.draad((2, 3), (1, 3), (0, 3), (0, 4), (0, 5), (0, 6))
    s.schakelaar(0, 7)
    s.lijn((1, 7), (5, 7))
    s.lijn((5, 4), (5, 6))
    s.zet(5, 0, 3, "guhs:knabbelbatterij[facing=south,lading=4]", {"id": "guhs:knabbelbatterij", "Inhoud": ms.Long(int(vk.getal("BATTERIJ")))})
    s.lamp(4, 6)
    s.lamp(6, 6)
    s.lamp(6, 4)
    s.bord(1, 8, ["Dit is een", "Blubkacheltje", f"Geeft {vk.getal('BLUBKACHELTJE')}", "vadskracht"])
    s.bord(7, 8, ["Dit is een", "Knabbelbatterij", "Bewaart 18000", "vadskracht"])
    s.guh_npc(8, 5)


def stand_kern(s):
    s.machine(4, 2, "gloeisterkern")
    s.b.kernen.append(s.g(4, 0, 2))
    s.draad((4, 1), (4, 0))
    for (lx, lz) in ((3, 1), (5, 1), (3, 2), (5, 2), (3, 3), (4, 3), (5, 3)):
        for ly in (0, 1):
            s.zet(lx, ly, lz, "minecraft:iron_bars")
    s.zet(4, 1, 1, "minecraft:iron_bars")
    s.zet(4, 1, 2, "minecraft:iron_bars")
    for lx in (3, 4, 5):
        for lz in (1, 2, 3):
            s.zet(lx, 2, lz, "minecraft:gold_block" if (lx, lz) == (4, 2) else "minecraft:smooth_quartz_slab[type=bottom]")
    bus_aansluiting(s)
    s.lijn((2, 7), (7, 7))
    for lx in (3, 5, 7):
        s.lamp(lx, 6)
    s.bord(1, 8, ["Dit is een", "Gloeisterkern", f"Geeft {vk.getal('GLOEISTERKERN')}", "vadskracht"])
    s.bord(6, 4, ["Hij laat de", "halve beurs", "draaien!", ""])
    s.guh_npc(8, 5)


def stand_molen(s):
    bus_aansluiting(s)
    s.machine(3, 3, "vadsmolen")
    s.zet(3, 1, 3, "guhs:machine_deel[dx=2,dy=1,dz=2]")
    s.merk(3, 3, Vul=[("guhs:knabbelgraan", 32)])
    s.zet(4, 0, 3, f"{RICHTING}[facing={'east'}]", {"id": RICHTING})
    s.zet(5, 0, 3, BUIS)
    s.zet(6, 0, 3, "minecraft:chest[facing=south,type=single,waterlogged=false]", {"id": "minecraft:chest"})
    s.merk(6, 3, Leeg=256)
    s.machine(3, 5, "guh_oven", extra={"lit": "false"})
    s.merk(3, 5, Vul=[("minecraft:cobblestone", 16)], Leeg=16)
    s.lijn((2, 3), (2, 7))
    s.lamp(2, 2)
    s.bord(5, 8, ["Dit is een", "Vadsmolen", op("MOLEN"), "vadskracht"])
    s.bord(4, 6, ["Dit is een", "Guhoven", op("GUH_OVEN"), "vadskracht"])
    s.guh_npc(8, 5)


def stand_oogster(s):
    bus_aansluiting(s)
    s.machine(4, 1, "oogster")
    s.zet(3, 0, 1, f"{RICHTING}[facing=west]", {"id": RICHTING})
    s.zet(2, 0, 1, BUIS)
    s.zet(1, 0, 1, "minecraft:chest[facing=south,type=single,waterlogged=false]", {"id": "minecraft:chest"})
    s.merk(1, 1, Leeg=128)
    for lx in range(2, 7):
        for lz in range(2, 7):
            if (lx, lz) == (4, 4):
                s.zet(lx, -1, lz, "minecraft:water[level=0]")
            else:
                s.zet(lx, -1, lz, "minecraft:farmland[moisture=7]")
                s.zet(lx, 0, lz, "guhs:kaasknabbelplant[age=7]")
    for lz in range(3, 7):
        s.zet(1, 0, lz, "minecraft:cherry_fence")
        s.zet(7, 0, lz, "minecraft:cherry_fence")
    s.lijn((2, 7), (8, 7))
    s.lijn((8, 1), (8, 6))
    s.draad((7, 1), (6, 1), (5, 1))
    s.lamp(7, 2)
    s.bord(3, 8, ["Dit is een", "Oogster", op("OOGSTER"), "vadskracht"])
    s.guh_npc(5, 8)


def stand_buizen(s):
    bus_aansluiting(s)
    kist = "minecraft:chest[facing=south,type=single,waterlogged=false]"
    s.zet(2, 0, 2, kist, {"id": "minecraft:chest", "Items": ms.compounds([
        {"Slot": ms.Byte(0), "id": "guhs:kaas_knabbels", "count": 32}, {"Slot": ms.Byte(1), "id": "minecraft:cobblestone", "count": 32}])})
    s.zet(3, 0, 2, f"{RICHTING}[facing=east]", {"id": RICHTING})
    s.zet(4, 0, 2, BUIS)
    s.zet(5, 0, 2, f"{FILTER}[facing=east,snoet=slaapt]", {"id": FILTER, "Filter": {
        "Items": ms.compounds([{"Slot": ms.Byte(0), "id": "guhs:kaas_knabbels", "count": 1}]), "Behalve": ms.Byte(0),
        "Precies": ms.Byte(0), "Getal": 0}})
    s.zet(6, 0, 2, kist, {"id": "minecraft:chest"})                     # B: the knabbels
    s.zet(4, 0, 1, kist, {"id": "minecraft:chest"})                     # C: the rest
    s.zet(6, 0, 3, f"{RICHTING}[facing=south]", {"id": RICHTING})       # B -> back to A
    for p in ((6, 4), (6, 5), (5, 5), (4, 5), (3, 5), (2, 5), (2, 4), (2, 3)):
        s.zet(p[0], 0, p[1], BUIS)
    s.zet(3, 0, 1, f"{RICHTING}[facing=west]", {"id": RICHTING})        # C -> back to A
    s.zet(2, 0, 1, BUIS)
    s.zet(7, 0, 2, "guhs:voorraadmeter[facing=east,signaal=false,snoet=slaapt]", {"id": "guhs:voorraadmeter", "Filter": {
        "Items": ms.compounds([{"Slot": ms.Byte(0), "id": "guhs:kaas_knabbels", "count": 1}]), "Behalve": ms.Byte(0),
        "Precies": ms.Byte(0), "Getal": 8}})
    s.zet(7, 1, 2, "minecraft:redstone_lamp[lit=false]")
    s.lijn((2, 7), (8, 7))
    s.lijn((8, 1), (8, 6))
    s.draad((7, 1), (6, 1), (5, 1))
    s.lamp(7, 6)
    s.bord(3, 8, ["Knabbelbuizen", "+ Filterstuk", op("BUISFILTER"), "vadskracht"])
    s.bord(5, 8, ["Dit is een", "Voorraadmeter", op("SENSOR"), "vadskracht"])
    s.guh_npc(1, 5, yaw=-90)


def stand_opzuiger(s):
    bus_aansluiting(s)
    s.zet(3, 0, 3, "minecraft:dropper[facing=east,triggered=false]", {"id": "minecraft:dropper", "Items": ms.compounds([
        {"Slot": ms.Byte(0), "id": "guhs:kaas_knabbels", "count": 4}])})
    s.zet(3, 0, 4, "guhs:guhklok[facing=south,signaal=false,snoet=slaapt]", {"id": "guhs:guhklok", "Stand": 1})
    s.zet(6, 0, 3, "guhs:opzuiger[facing=west,snoet=slaapt]", {"id": "guhs:opzuiger"})
    s.zet(7, 0, 3, f"{RICHTING}[facing=east]", {"id": RICHTING})
    for p in ((8, 3), (8, 2), (7, 2), (6, 2), (5, 2), (4, 2), (3, 2)):
        s.zet(p[0], 0, p[1], BUIS)
    s.lijn((2, 4), (2, 7))
    s.lijn((3, 7), (6, 7))
    s.lijn((6, 4), (6, 6))
    s.lamp(7, 6)
    s.bord(1, 8, ["Dit is een", "Opzuiger", op("OPZUIGER"), "vadskracht"])
    s.bord(4, 8, ["Dit is een", "Guhklok", op("SENSOR"), "vadskracht"])
    s.guh_npc(8, 5)


def stand_knabbelaar(s):
    bus_aansluiting(s)
    s.machine(4, 2, "knabbelaar", "south")
    s.machine(4, 4, "neerzetter", "north", nbt={"Vakken": _vakken([(0, "minecraft:cobblestone", 8)], 9)})
    s.zet(4, 0, 1, f"{RICHTING}[facing=north]", {"id": RICHTING})
    for p in ((4, 0), (3, 0), (2, 0), (2, 1), (2, 2), (2, 3), (2, 4), (3, 4)):
        s.zet(p[0], 0, p[1], BUIS)
    s.lijn((2, 7), (5, 7))
    s.lijn((5, 2), (5, 6))
    s.lamp(6, 6)
    s.bord(1, 8, ["Dit is een", "Knabbelaar", op("KNABBELAAR"), "vadskracht"])
    s.bord(7, 8, ["Dit is een", "Neerzetter", op("NEERZETTER"), "vadskracht"])
    s.guh_npc(8, 5)


def stand_knutsel(s):
    bus_aansluiting(s)
    naar_blok = tekening(["guhs:kaas_knabbels"] * 9, "guhs:block_of_kaasknabbels", 1)
    naar_knabbels = tekening(["guhs:block_of_kaasknabbels"] + [None] * 8, "guhs:kaas_knabbels", 9)
    s.machine(3, 3, "knutselmachine", nbt={"Vakken": {"stacks": ov._items([(1, "guhs:kaas_knabbels", 63)], 13)}})
    s.machine(6, 3, "knutselmachine", nbt={"Vakken": {"stacks": ov._items([(1, "guhs:block_of_kaasknabbels", 2)], 13)}})
    for (lx, t) in ((3, naar_blok), (6, naar_knabbels)):
        p = s.g(lx, 0, 3)
        naam, props, nbt = s.b.blocks[p]
        nbt["Vakken"]["stacks"][0] = t
        s.b.blocks[p] = (naam, props, nbt)
    s.zet(4, 0, 3, f"{RICHTING}[facing=east]", {"id": RICHTING})
    s.zet(5, 0, 3, BUIS)
    s.zet(6, 0, 2, f"{RICHTING}[facing=north]", {"id": RICHTING})
    for p in ((6, 1), (5, 1), (4, 1), (3, 1), (3, 2)):
        s.zet(p[0], 0, p[1], BUIS)
    s.machine(8, 3, "tekentafel")
    s.lijn((2, 3), (2, 7))
    s.lijn((3, 7), (7, 7))
    s.lijn((7, 3), (7, 6))
    s.lamp(4, 6)
    s.bord(1, 8, ["Twee", "Knutselmachines", op("KNUTSELMACHINE"), "vadskracht elk"])
    s.bord(8, 8, ["Dit is een", "Tekentafel", "Tekent je", "recepten"])
    s.guh_npc(8, 5)


def _stuk(dx, dz, gf):
    # the piece of the bed, by the compass, seen from the middle of the bed (kern.relative(facing.opposite))
    mx, mz = VEC[TEGEN[gf]]
    rx, rz = dx - mx, dz - mz
    return {"stuk": ["nw", "n", "no", "w", "midden", "o", "zw", "z", "zo"][(rz + 1) * 3 + rx + 1]}


def stand_plantagebak(s):
    bus_aansluiting(s)
    s.meer(4, 5, "plantagebak", "south", 3, 1, 3, "guhs:techmachine_plantagebak_deel",
           nbt={"Vakken": _vakken([(0, "minecraft:oak_sapling", 8)], 1)}, deel_props=_stuk)
    s.merk(4, 5, Hak=400, Vul=[("minecraft:oak_sapling", 4)])
    s.lijn((2, 7), (4, 7))
    s.draad((4, 6))
    s.lamp(5, 6)
    s.lamp(2, 2)
    s.lijn((2, 3), (2, 6))
    s.bord(6, 8, ["Dit is een", "Plantagebak", op("PLANTAGEBAK"), "vadskracht"])
    s.guh_npc(8, 5)


def stand_pomp(s):
    bus_aansluiting(s)
    s.zet(3, -1, 3, "minecraft:water[level=0]")
    s.machine(3, 3, "sauspomp")
    for lx in (4, 5):
        ov.slang(s.b, *s.g(lx, 0, 3))
    s.zet(6, 0, 3, "guhs:sausvat[facing=south,niveau=0,saus=leeg]", {"id": "guhs:sausvat"})
    s.merk(6, 3, LeegSaus=12000)
    s.lijn((2, 3), (2, 7))
    s.lamp(2, 2)
    s.bord(3, 8, ["Dit is een", "Sauspomp", op("POMP"), "vadskracht"])
    s.bord(6, 8, ["Dit is een", "Sausvat", "Geen vadskracht", "nodig"])
    s.guh_npc(8, 5)


def stand_sauzen(s):
    bus_aansluiting(s)
    for (lx, saus) in ((2, "guhs:kaas_saus"), (4, "guhs:kaasfrituursaus"), (6, "minecraft:water")):
        s.zet(lx, -1, 2, f"{saus}[level=0]")
        s.machine(lx, 2, "sauspomp")
    for lx in range(2, 7):
        ov.slang(s.b, *s.g(lx, 0, 3))
    s.machine(2, 4, "brouwautomaat")
    s.merk(2, 4, Vul=[("minecraft:glass_bottle", 32), ("guhs:kaas_knabbels", 16)], Leeg=3)
    s.machine(4, 4, "frituurautomaat")
    s.merk(4, 4, Vul=[("guhs:kaas_knabbels", 32)], Leeg=16)
    s.machine(6, 4, "grillkoolpers", extra={"perst": "false"})
    s.zet(6, 1, 4, "guhs:machine_deel[dx=2,dy=1,dz=2]")
    s.merk(6, 4, Leeg=4)
    s.lijn((2, 5), (7, 5))
    s.lijn((7, 1), (7, 4))
    s.lijn((2, 1), (6, 1))
    s.draad((2, 6))
    s.lamp(3, 6)
    s.bord(1, 8, ["Dit is een", "Brouwautomaat", op("BROUWKETEL"), "vadskracht"])
    s.bord(4, 8, ["Dit is een", "Frituurautomaat", op("FRITUUR"), "vadskracht"])
    s.bord(6, 8, ["Dit is een", "Grillkoolpers", op("FRITUUR"), "vadskracht"])
    s.guh_npc(8, 6)


# =====================================================================================================================
# the middle: foyer, Droomfabriek, Stepstation
# =====================================================================================================================
def foyer(b):
    for x in range(1, 8):
        for z in range(15, 27):
            b.set(x, VL, z, "minecraft:pink_concrete" if (x + z) % 2 else "minecraft:white_concrete")
    b.sign(3, Y0, 17, "west", ["Welkom op de", "Guhtechniek-", "beurs!", ""], wall=False, wood="cherry")
    b.sign(3, Y0, 24, "west", ["Kijken mag,", "pakken niet!", "Knopje = aan/uit", ""], wall=False, wood="cherry")
    b.npc(5, Y0, 21, "techbeurs_welkom", -90.0, kind=KINDEN["welkom"])
    for z in (16, 25):
        b.set(6, Y0, z, "minecraft:potted_cherry_sapling")


def droom(b):
    """The Droomfabriek, x 9..33, z 14..27, on its own Gloeisterkern (local: dx = x - 9, dz = z - 14)."""
    o = (9, 14)

    def zet(dx, dy, dz, s, nbt=None):
        naam, props = ob._blok(s)
        b.set(o[0] + dx, Y0 + dy, o[1] + dz, naam, props, nbt)

    def merk(dx, dz, dy=0, **regels):
        p = (o[0] + dx, Y0 + dy, o[1] + dz)
        naam, props, nbt = b.blocks[p]
        nbt = dict(nbt or {})
        nbt["NeoForgeData"] = {"guhs_beurs": _merk(**regels)}
        b.blocks[p] = (naam, props, nbt)

    for dx in range(0, 25):
        for dz in range(0, 14):
            b.set(o[0] + dx, VL, o[1] + dz, STAND_VLOER["droom"] if dz < 10 else VLOER)
    # the kern in its cage, the big switch, the spine of Guhdraad along dz 0
    zet(1, 0, 2, "guhs:gloeisterkern[facing=south,snoet=slaapt]", {"id": "guhs:gloeisterkern"})
    b.kernen.append((o[0] + 1, Y0, o[1] + 2))
    for (dx, dz) in ((0, 1), (2, 1), (0, 2), (2, 2), (0, 3), (1, 3), (2, 3)):
        zet(dx, 0, dz, "minecraft:iron_bars")
        zet(dx, 1, dz, "minecraft:iron_bars")
    zet(1, 1, 2, "minecraft:iron_bars")
    zet(1, 1, 1, "minecraft:iron_bars")
    for dx in range(3):
        for dz in range(1, 4):
            zet(dx, 2, dz, "minecraft:gold_block" if (dx, dz) == (1, 2) else "minecraft:smooth_quartz_slab[type=bottom]")
    zet(1, 0, 1, DRAAD)
    zet(1, 0, 0, f"{SCHAKELAAR}[facing=north,powered=true]")
    b.schakelaars.append(("droom", (o[0] + 1, Y0, o[1])))
    for dx in range(2, 24):
        zet(dx, 0, 0, DRAAD)
    # the field: 5 x 5 kaasknabbelplantjes in front of the Oogster, water in the middle, a fence around
    zet(6, 0, 3, "guhs:oogster[facing=south,snoet=slaapt]", {"id": "guhs:oogster"})
    zet(6, 0, 1, DRAAD)
    zet(6, 0, 2, DRAAD)
    for dx in range(4, 9):
        for dz in range(4, 9):
            if (dx, dz) == (6, 6):
                zet(dx, -1, dz, "minecraft:water[level=0]")
            else:
                zet(dx, -1, dz, "minecraft:farmland[moisture=7]")
                zet(dx, 0, dz, "guhs:kaasknabbelplant[age=7]")
    for dz in range(4, 10):
        zet(3, 0, dz, "minecraft:cherry_fence")
        zet(9, 0, dz, "minecraft:cherry_fence")
    for dx in range(4, 9):
        zet(dx, 0, 9, "minecraft:cherry_fence")
    zet(3, 0, 1, f"{LAMP}[facing=south,snoet=slaapt]", {"id": LAMP})
    zet(8, 0, 1, f"{LAMP}[facing=south,snoet=slaapt]", {"id": LAMP})
    # Oogster -> Richtingstuk -> buizen -> Filterstuk (knabbels) -> Frituurautomaat; the seeds go into the seed chest
    zet(7, 0, 3, f"{RICHTING}[facing=east]", {"id": RICHTING})
    for dx in range(8, 13):
        zet(dx, 0, 3, BUIS)
    zet(10, 0, 2, "minecraft:chest[facing=south,type=single,waterlogged=false]", {"id": "minecraft:chest"})
    merk(10, 2, Leeg=256)
    zet(13, 0, 3, f"{FILTER}[facing=east,snoet=slaapt]", {"id": FILTER, "Filter": {
        "Items": ms.compounds([{"Slot": ms.Byte(0), "id": "guhs:kaas_knabbels", "count": 1}]), "Behalve": ms.Byte(0),
        "Precies": ms.Byte(0), "Getal": 0}})
    zet(13, 0, 2, DRAAD)
    zet(13, 0, 1, DRAAD)
    zet(14, 0, 3, "guhs:frituurautomaat[facing=south,snoet=slaapt]", {"id": "guhs:frituurautomaat"})
    zet(14, -1, 1, "guhs:kaasfrituursaus[level=0]")
    zet(14, 0, 1, "guhs:sauspomp[facing=south,snoet=slaapt]", {"id": "guhs:sauspomp"})
    ov.slang(b, o[0] + 14, Y0, o[1] + 2)
    # Frituurautomaat -> Richtingstuk -> Filterstuk (gefrituurde knabbels) -> Brouwautomaat; the rest into a chest
    zet(15, 0, 3, f"{RICHTING}[facing=east]", {"id": RICHTING})
    zet(16, 0, 3, BUIS)
    zet(17, 0, 3, BUIS)
    zet(17, 0, 4, "minecraft:chest[facing=south,type=single,waterlogged=false]", {"id": "minecraft:chest"})
    merk(17, 4, Leeg=1024)
    zet(18, 0, 3, f"{FILTER}[facing=east,snoet=slaapt]", {"id": FILTER, "Filter": {
        "Items": ms.compounds([{"Slot": ms.Byte(0), "id": "guhs:gefrituurde_kaasknabbels", "count": 1}]), "Behalve": ms.Byte(0),
        "Precies": ms.Byte(0), "Getal": 0}})
    zet(18, 0, 2, DRAAD)
    zet(18, 0, 1, DRAAD)
    zet(19, 0, 3, "guhs:brouwautomaat[facing=south,snoet=slaapt]", {"id": "guhs:brouwautomaat"})
    merk(19, 3, Vul=[("minecraft:glass_bottle", 32)])
    zet(19, -1, 1, "guhs:kaas_saus[level=0]")
    zet(19, 0, 1, "guhs:sauspomp[facing=south,snoet=slaapt]", {"id": "guhs:sauspomp"})
    ov.slang(b, o[0] + 19, Y0, o[1] + 2)
    # Brouwautomaat -> Richtingstuk -> the potion chest, with a Voorraadmeter and its lamp: "Klaar!"
    zet(20, 0, 3, f"{RICHTING}[facing=east]", {"id": RICHTING})
    zet(21, 0, 3, BUIS)
    zet(22, 0, 3, "minecraft:chest[facing=south,type=single,waterlogged=false]", {"id": "minecraft:chest"})
    merk(22, 3, Leeg=48)
    zet(23, 0, 3, "guhs:voorraadmeter[facing=east,signaal=false,snoet=slaapt]", {"id": "guhs:voorraadmeter", "Filter": {
        "Items": ms.compounds([]), "Behalve": ms.Byte(0), "Precies": ms.Byte(0), "Getal": 1}})
    zet(23, 1, 3, "minecraft:redstone_lamp[lit=false]")
    zet(23, 0, 2, DRAAD)
    zet(23, 0, 1, DRAAD)
    # signs along the line (front: dz 10..13 is the walkway to the south aisle)
    b.sign(o[0] + 2, Y0, o[1] + 11, "south", ["De", "Droomfabriek", "Alles werkt", "samen!"], wall=False, wood="cherry")
    b.sign(o[0] + 1, Y0, o[1] - 0 + 4, "south", ["Grote schakelaar", "van de fabriek", "", ""], wall=False, wood="cherry")
    b.sign(o[0] + 10, Y0, o[1] + 4, "south", ["Zaadjes", "", "", ""], wall=False, wood="cherry")
    b.sign(o[0] + 14, Y0, o[1] + 5, "south", ["Gefrituurd!", "", "", ""], wall=False, wood="cherry")
    b.sign(o[0] + 17, Y0, o[1] + 6, "south", ["Kaasknabbels", "Gefrituurd!", "", ""], wall=False, wood="cherry")
    b.sign(o[0] + 22, Y0, o[1] + 5, "south", ["Drankjes", "Klaar!", "", ""], wall=False, wood="cherry")
    b.npc(o[0] + 12, Y0, o[1] + 11, "techbeurs_droom", 0.0, kind=KINDEN["droom"])


def bezorg(b):
    """The Stepstation and its Bezorgguhtje, x 47..70, z 14..27: from the green post (chest A) to the orange one (chest B)."""
    for x in range(47, 71):
        for z in range(14, 28):
            b.set(x, VL, z, STAND_VLOER["bezorg"] if 15 <= z <= 26 and 48 <= x <= 69 else VLOER)
    kist = "minecraft:chest[facing=south,type=single,waterlogged=false]"
    b.set(58, Y0, 20, "guhs:stepstation", {"facing": "south", "snoet": "slaapt"}, {"id": "guhs:stepstation",
                                                                                    "NeoForgeData": {"guhs_beurs": _merk(Station=True)}})
    b.set(51, Y0, 17, *ob._blok(kist), {"id": "minecraft:chest", "NeoForgeData": {"guhs_beurs": _merk(Vul=[("guhs:kaas_knabbels", 32)])}})
    b.set(51, Y0 + 1, 17, "guhs:haltepaaltje", {"facing": "down", "ophalen": "true"}, {"id": "guhs:haltepaaltje"})
    b.set(66, Y0, 24, *ob._blok(kist), {"id": "minecraft:chest", "NeoForgeData": {"guhs_beurs": _merk(Leeg=32)}})
    b.set(66, Y0 + 1, 24, "guhs:haltepaaltje", {"facing": "down", "ophalen": "false"}, {"id": "guhs:haltepaaltje"})
    # power from the bus (x 72) through the switch at the south aisle
    for x in (71, 70):
        b.set(x, Y0, 27, DRAAD)
    b.set(69, Y0, 27, SCHAKELAAR, {"facing": "south", "powered": "true"})
    b.schakelaars.append(("bezorg", (69, Y0, 27)))
    for x in range(59, 69):
        b.set(x, Y0, 27, DRAAD)
    for z in range(20, 27):
        b.set(59, Y0, z, DRAAD)
    b.set(60, Y0, 20, LAMP, {"facing": "south", "snoet": "slaapt"}, {"id": LAMP})
    b.set(60, Y0, 26, LAMP, {"facing": "south", "snoet": "slaapt"}, {"id": LAMP})
    b.sign(56, Y0, 26, "south", ["Dit is een", "Stepstation", op("STEPSTATION"), "vadskracht"], wall=False, wood="cherry")
    b.sign(52, Y0, 18, "south", ["Haalt op", "", "", ""], wall=False, wood="cherry")
    b.sign(65, Y0, 25, "south", ["Brengt", "", "", ""], wall=False, wood="cherry")
    b.npc(63, Y0, 26, "techbeurs_bezorg", 0.0, kind=KINDEN["bezorg"])


def lounge(b):
    """Between the Droomfabriek and the Stepstation: the beurs café, tables with benches, plants (x 35..45, z 15..26)."""
    for x in range(35, 46):
        for z in range(15, 27):
            b.set(x, VL, z, "minecraft:white_concrete" if (x + z) % 2 else "minecraft:pink_concrete")
    for (tx, tz) in ((37, 17), (43, 17), (37, 24), (43, 24), (40, 20)):
        b.set(tx, Y0, tz, "minecraft:cherry_fence")
        b.set(tx, Y0 + 1, tz, "minecraft:cherry_pressure_plate", {"powered": "false"})
        for (dx, dz, f) in ((-1, 0, "east"), (1, 0, "west")):
            b.set(tx + dx, Y0, tz + dz, "minecraft:cherry_stairs", {"facing": f, "half": "bottom", "shape": "straight", "waterlogged": "false"})
    for (x, z) in ((35, 15), (45, 15), (35, 26), (45, 26), (40, 15), (40, 26)):
        b.set(x, Y0, z, "minecraft:potted_pink_tulip" if (x + z) % 2 else "minecraft:potted_allium")
    b.sign(40, Y0, 22, "south", ["Beurscafé", "Kaasshakes:", "helaas op", ""], wall=False, wood="cherry")


def bus(b):
    """The bus of the kern stand: z 1 from x 42 to 72, x 72 from z 1 to 40, z 40 from x 72 back to 2."""
    for x in range(42, 73):
        b.set(x, Y0, 1, DRAAD)
    for z in range(1, 41):
        b.set(72, Y0, z, DRAAD)
    for x in range(2, 73):
        b.set(x, Y0, 40, DRAAD)


NOORD = [("guhrad", "bron", stand_guhrad), ("knuffel", "bron", stand_knuffel), ("disco", "bron", stand_disco),
         ("blub", "bron", stand_blub), ("kern", "bron", stand_kern), ("molen", "machine", stand_molen),
         ("oogster", "machine", stand_oogster)]
ZUID = [("buizen", "machine", stand_buizen), ("opzuiger", "machine", stand_opzuiger), ("knabbelaar", "machine", stand_knabbelaar),
        ("knutsel", "machine", stand_knutsel), ("plantagebak", "machine", stand_plantagebak), ("pomp", "saus", stand_pomp),
        ("sauzen", "saus", stand_sauzen)]


def bouw():
    b = ob.Bouw((X, Y, Z), NAAM, prefix=PREFIX)
    b.schakelaars, b.kernen, b.guhraden = [], [], []
    hal(b)
    for i, (naam, soort, f) in enumerate(NOORD):
        f(Stand(b, 2 + 10 * i, 2, "N", soort, naam))
    for i, (naam, soort, f) in enumerate(ZUID):
        f(Stand(b, 2 + 10 * i, 31, "S", soort, naam))
    foyer(b)
    droom(b)
    bezorg(b)
    lounge(b)
    bus(b)
    gevel(b)
    b.set(*BEHEER, "guhs:beurs_beheer", {}, {"id": "guhs:beurs_beheer"})
    b._jigsaw(BEHEER[0], 0, BEHEER[2] + 2, "up_north", f"guhs:{NAAM}_midden", final="minecraft:smooth_stone")
    ov.verbind(b)
    verbind_buizen(b)
    return b


# =====================================================================================================================
# connections in the template (no block updates in new chunks)
# =====================================================================================================================
INVENTARIS = {"minecraft:chest", "minecraft:barrel", "minecraft:hopper", "minecraft:dropper", "guhs:guh_oven", "guhs:machine_deel",
              "guhs:vadsmolen", "guhs:oogster", "guhs:knabbelaar", "guhs:neerzetter", "guhs:knutselmachine", "guhs:opzuiger",
              "guhs:brouwautomaat", "guhs:frituurautomaat", "guhs:grillkoolpers", "guhs:stepstation", "guhs:plantagebak",
              "guhs:techmachine_plantagebak_deel"}
AS = {"north": "z", "south": "z", "east": "x", "west": "x", "up": "y", "down": "y"}


def verbind_buizen(b):
    """The arms of every Knabbelbuis (Buizen.verbindt): towards another tube, a piece whose facing lies on that axis, or
    anything that holds items."""
    for p, (naam, props, nbt) in list(b.blocks.items()):
        if naam != BUIS:
            continue
        arms = {}
        for kant, (dx, dy, dz) in ov.RICHTING.items():
            q = (p[0] + dx, p[1] + dy, p[2] + dz)
            bq = b.blocks.get(q)
            n = bq[0] if bq else None
            if n == BUIS or n in INVENTARIS:
                arms[kant] = "true"
            elif n in (RICHTING, FILTER):
                arms[kant] = "true" if AS[bq[1].get("facing", "north")] == AS[kant] else "false"
            else:
                arms[kant] = "false"
        b.blocks[p] = (naam, arms, nbt)


# =====================================================================================================================
# the proof in python: the nets of Guhdraad + knopen, every switch cuts
# =====================================================================================================================
def _knoop_namen():
    return set(ov.VADS) - {"guhs:guh_wire"}


def netten(b, open_schakelaars=True):
    """{pos: net id} of every Guhdraad and knoop block (face neighbours; a switch joins when open_schakelaars)."""
    knopen = _knoop_namen()
    plekken = {p for p, (n, _, _) in b.blocks.items() if n == DRAAD or n in knopen}
    if not open_schakelaars:
        plekken = {p for p in plekken if b.blocks[p][0] != SCHAKELAAR}
    net, i = {}, 0
    for start in sorted(plekken):
        if start in net:
            continue
        i += 1
        todo = [start]
        net[start] = i
        while todo:
            p = todo.pop()
            for (dx, dy, dz) in ov.RICHTING.values():
                q = (p[0] + dx, p[1] + dy, p[2] + dz)
                if q in plekken and q not in net:
                    if b.blocks[q][0] == SCHAKELAAR and dy == -1:
                        continue
                    net[q] = i
                    todo.append(q)
    return net


def check_netten(b):
    fouten = []
    alles = netten(b, True)
    zonder = netten(b, False)
    for (naam, p) in b.schakelaars:
        buren = [(p[0] + dx, p[1], p[2] + dz) for (dx, _, dz) in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1))]
        aan = {zonder[q] for q in buren if q in zonder}
        if len(aan) < 2:
            fouten.append(f"de schakelaar van {naam} op {p} knipt niets door (kanten: {aan})")
    # every machine / lamp is in a net with a source
    bron_netten = {alles[k] for k in b.kernen + b.guhraden if k in alles}
    for p, (n, _, _) in b.blocks.items():
        if n in ("guhs:knuffelgenerator", "guhs:disco_dynamo", "guhs:blubkacheltje", "guhs:knabbelbatterij"):
            bron_netten.add(alles[p])
    for p, (n, _, _) in b.blocks.items():
        if n in _knoop_namen() and n not in ("guhs:machine_deel", "guhs:techbron_kussen_deel", "guhs:techbron_vloer_deel",
                                              "guhs:techmachine_plantagebak_deel", "guhs:guh_wheel_part") and alles.get(p) not in bron_netten:
            fouten.append(f"{n} op {p} hangt aan geen enkele bron")
    # the bus and the droom kern are separate; each source stand is its own
    kn = [alles[k] for k in b.kernen]
    if len(set(kn)) != len(kn):
        fouten.append("de twee kernen zitten in één net")
    return fouten


# =====================================================================================================================
# worldgen
# =====================================================================================================================
BIOMEN = ["guhs:guh_fields", "guhs:knabbel_crumbs", "guhs:pink_puffs", "guhs:kaas_flats", "guhs:guh_meadows", "guhs:bloemenvallei"]


def worldgen(h):
    D, w = h.D, h.w
    spawn = {"monster": {"bounding_box": "piece", "spawns": []}, "ambient": {"bounding_box": "piece", "spawns": []}}
    keep = max(BEHEER[0], X - BEHEER[0], BEHEER[2] + 2, Z - BEHEER[2] - 2) + 9
    w(f"{D}/worldgen/structure/{NAAM}.json", {
        "type": "guhs:flat_jigsaw", "biomes": f"#guhs:has_structure/{NAAM}", "step": "fluid_springs", "spawn_overrides": spawn,
        "terrain_adaptation": "beard_box", "check_radius": 34, "max_height_difference": 10, "keep_clear": keep,
        "jigsaw": {"type": "minecraft:jigsaw", "biomes": f"#guhs:has_structure/{NAAM}", "step": "fluid_springs",
                   "spawn_overrides": spawn, "terrain_adaptation": "beard_box", "start_pool": f"guhs:{NAAM}/start", "size": 1,
                   "start_height": {"absolute": 0}, "project_start_to_heightmap": "WORLD_SURFACE_WG", "max_distance_from_center": 80,
                   "use_expansion_hack": False, "start_jigsaw_name": f"guhs:{NAAM}_midden"}})
    w(f"{D}/worldgen/template_pool/{NAAM}/start.json", {"fallback": "minecraft:empty", "elements": [{"weight": 1, "element": {
        "element_type": "guhs:grond_single_pool_element", "location": f"guhs:{TEMPLATE}", "projection": "rigid",
        "processors": "minecraft:empty", "ground_level_delta": VL + 1}}]})
    w(f"{D}/worldgen/structure_set/{NAAM}_gegarandeerd.json", {"structures": [{"structure": f"guhs:{NAAM}", "weight": 1}], "placement": {
        "type": "guhs:gegarandeerd", "salt": SALT + 7, "min_afstand": 800, "max_afstand": 1500, "alleen_nieuw": True, "sector": 7,
        "sectoren": 16}})
    w(f"{D}/worldgen/structure_set/{NAAM}.json", {"structures": [{"structure": f"guhs:{NAAM}", "weight": 1}], "placement": {
        "type": "minecraft:random_spread", "spacing": 150, "separation": 110, "salt": SALT}})
    w(f"{D}/tags/worldgen/biome/has_structure/{NAAM}.json", {"replace": False, "values": BIOMEN})


TEKSTEN = {  # key: (English, Dutch)
    f"structure.guhs.{NAAM}": ("Guh Technology Fair", "Guhtechniek-beurs"),
    f"structure.guhs.{NAAM}.tooltip": ("A glass hall where every guh machine really works, with the Dream Factory in the middle",
                                       "Een glazen hal waar elke guhmachine echt werkt, met de Droomfabriek in het midden"),
    "gui.guhs.techbeurs.kijken": ("Looking is fine, taking is not: this belongs to the fair!", "Kijken mag, pakken niet: dit is van de beurs!"),
    "block.guhs.beurs_beheer": ("Fair Caretaker", "Beursbeheer"),
}


def texts(h):
    for k, (en, nl) in TEKSTEN.items():
        h.lang(k, en, nl)
    for stand, regels in PRAAT.items():
        for i, (nl, en) in enumerate(regels, 1):
            h.lang(f"quest.guhs.techbeurs.{stand}.{i}", en, nl)


def beheer_blok(h):
    A, w = h.A, h.w
    w(f"{A}/models/block/beurs_beheer.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "minecraft:block/smooth_stone"}})
    w(f"{A}/blockstates/beurs_beheer.json", {"variants": {"": {"model": "guhs:block/beurs_beheer"}}})


def build(h):
    b = bouw()
    fouten = check_netten(b) + b.fouten
    for f in fouten:
        print("techbeurs:", f)
    b.save(TEMPLATE)
    worldgen(h)
    beheer_blok(h)
    texts(h)
    return b, fouten


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
    _, fouten = build(h)
    for k, nl in sign_text.TEXTS.items():
        if k.startswith(PREFIX + "."):
            NL[k] = nl
            EN[k] = EN_REGELS.get(nl, nl)
            if nl not in EN_REGELS:
                print("techbeurs: no English for the sign line", repr(nl))
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        w(p, data)
    overlay = json.load(open(EN_OVERLAY, encoding="utf-8")) if os.path.exists(EN_OVERLAY) else {}
    overlay.update(EN)
    w(EN_OVERLAY, dict(sorted(overlay.items())))
    print(f"techbeurs: {len(NL)} texts, {len(fouten)} problems")
    if fouten:
        raise SystemExit(1)


if __name__ == "__main__":
    _direct()
