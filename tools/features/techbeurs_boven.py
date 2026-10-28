"""
1.8.0 thema 2 "Dromen en vadskracht" (design: MineScapeStory/ontwerp-guhs180-vadskracht.md V1-V7; renders
dromen-ontwerp/beurs; Java: feature/techbeurs/BeursUitbouw + BeursBeheer): the renewed Guhtechniek-beurs. The hall of 1.7.0
(techbeurs.py) stays as it is; two more templates go on and next to it, stamped by the BeursBeheer of every copy (also of the
beurs that already stands in a world: it keeps a version, like the DroomEilanden):

  techbeurs/guhtechniek_beurs_aanbouw   37 x 14 x 30, at hall (73, 0, 6): the annex east of the hall (its x 0 is the hall's
                                        east wall: only a doorway is cut in it). Simple ground-floor tech: Hapluikje,
                                        Snuffelsensor + Guhteller, Knuffelsteen-generator, Mika-rad, Weer vadser with the three
                                        Wolkenantennes, a Guhrad + Knabbelketel that power the annex. A stair and a wolkenliftje
                                        up to its roof terrace, which leads into the Fabriekshal.
  techbeurs/guhtechniek_beurs_boven     74 x 28 x 42, at hall (0, 11, 0): on the hall's glass roof.
      y 0        the Fabriekshal floor (frog lights in it light the hall below as well)
      y 1..9     De Fabriekshal: north row = the fabriek (Bouwblokken-fabriek, Wolfabriek, Knuffelrecycler, Scheerstal with
                 Wolkenschaapjes, Kleurmolen, Regenboogpers, the ketel stand: Gloeisterkern + 3 Knabbelketels on the bus);
                 south row = tier 5 logistics (Vadszender/-ontvanger, Guhterminal + Bestelbalie, Knabbelpoort-buis +
                 Thuispoort, Bouwkeet, Luchtblazer + Waterdrager, Korstduwer + Vuurkorf, Hamsterbuis-stations); the middle:
                 stair + liftje up, the Kaaskas on its own Knabbelketel (with the Kassier), the Sorteerband (with the
                 Sorteerder), Saustank -> Sausbuis -> Saustank, the raderen-hoek (afnemende opbrengst), the east door.
      y 10       the deck of De Koepel
      y 11..     De Koepel: north row = Mega-oven, Knabbel-weerstation, Draaimolen, Wolkenvanger, Fossiel-broedmachine,
                 Hamsterknooppunt, Mega-knuffelcentrale (on the bus with the Koepel's Gloeisterkern); south row = the
                 Kaasraket (Lanceerplatform at stage 2 of 4: the BeursUitbouw builds the half rocket on it), Enderguhrad
                 and Komeetkern (showcase sources), the Gloeisterkern, the fossil showcases, the view; in the middle a glass
                 dome over the Sterrenkoepel.

Every machine works for ever like downstairs: NeoForgeData.guhs_beurs marks for the BeursBeheer (Vul / Leeg / VulSaus /
LeegSaus / Guhs), switches that cut, check_netten() proves the nets in python.

Standalone and idempotent:   python tools/features/techbeurs_boven.py     (from the project root)
"""
import json
import math
import os
import re
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
import make_structures as ms  # noqa: E402
import sign_text  # noqa: E402
from features import onderwol_bouw as ob  # noqa: E402
from features import onderwol_vadskracht as ov  # noqa: E402
from features import techbeurs as tb  # noqa: E402

NAAM_A, NAAM_B = "guhtechniek_beurs_aanbouw", "guhtechniek_beurs_boven"
TEMPLATE_A, TEMPLATE_B = f"techbeurs/{NAAM_A}", f"techbeurs/{NAAM_B}"
PLEK_A, PLEK_B = (73, 0, 6), (0, 11, 0)          # their min corner in the hall's template frame (Java: BeursUitbouw)
XA, YA, ZA = 37, 14, 30
XB, YB, ZB = 74, 28, 42
PREFIX = tb.PREFIX
DRAAD, SCHAKELAAR, LAMP = tb.DRAAD, tb.SCHAKELAAR, tb.LAMP
BUIS, RICHTING = tb.BUIS, tb.RICHTING
KIST = "minecraft:chest[facing=south,type=single,waterlogged=false]"
VLOER, PAD = "minecraft:white_concrete", "minecraft:pink_glazed_terracotta"
LICHT = "minecraft:pearlescent_froglight[axis=y]"
HEK = "minecraft:cherry_fence"
# the Mega-knuffelcentrale is a vadskracht knoop too (the tag misses it: tools/features/techbeurs_boven.py adds it)
CENTRALE = ("guhs:mega_knuffelcentrale", "guhs:mega_knuffelcentrale_deel")

KINDEN = {"aanbouw": "loketguh", "fabriek": "uitvinderguh", "kaaskas": "kraamguh", "sorteer": "werktuigguh",
          "koepel": "loketguh", "raket": "uitvinderguh"}

PRAAT = {  # stand -> [(Dutch, English), ...]: line 1 a joke, line 2 the tip
    "aanbouw": [
        ("Welkom in de aanbouw! Hier staan de simpele dingetjes. Simpel, maar wel heel schattig.",
         "Welcome to the annex! The simple little things live here. Simple, but very cute."),
        ("Boven is de Fabriekshal en helemaal bovenop de Koepel. Neem de trap, of het wolkenliftje als je lui bent. Ik ben lui.",
         "Upstairs is the Factory Hall and right at the top the Dome. Take the stairs, or the cloud lift if you're lazy. I'm lazy.")],
    "fabriek": [
        ("Welkom in de Fabriekshal! Hier maken machines alles wat jij te lui bent om zelf te maken. Dus alles.",
         "Welcome to the Factory Hall! Machines here make everything you're too lazy to make yourself. So everything."),
        ("Op elk bordje staat hoeveel vadskracht een machine eet. De ketelstand achterin houdt alles hier aan de gang.",
         "Every sign says how much chonk power a machine eats. The boiler stand at the back keeps everything here going.")],
    "kaaskas": [
        ("Ik ben de Kassier! Ik pluk de hele dag. Dat mag je niet verwarren met plukken voor jezelf.",
         "I'm the Cashier! I pick all day long. Not to be confused with picking for myself."),
        ("Kaassaus en vadskracht in de Kaaskas, en alles binnen 9 bij 9 groeit als kool. Zet een guh met het klusje Kassier erbij.",
         "Cheese sauce and chonk power in the Cheese Greenhouse, and everything within 9 by 9 grows like crazy. Add a guh with the chore Cashier.")],
    "sorteer": [
        ("Ik ben de Sorteerder. Knabbels bij knabbels, wol bij wol, en sokken... die zoek ik nog.",
         "I'm the Sorter. Nibbles with nibbles, wool with wool, and socks... I'm still looking for those."),
        ("Leg Sorteerbanden achter elkaar. Een guh met het klusje Sorteerder legt alles in de kist waar al zo'n ding in ligt.",
         "Lay Sorting Belts one after another. A guh with the chore Sorter puts everything in the chest that already holds one.")],
    "koepel": [
        ("Welkom op de Koepel! Hier staan de allergrootste machines. En het mooiste uitzicht. En ik.",
         "Welcome to the Dome! The very biggest machines live here. And the nicest view. And me."),
        ("Onder de glazen koepel staat de Sterrenkoepel: voer hem knabbels en er licht een guh-sterrenbeeld op.",
         "Under the glass dome stands the Star Dome: feed it nibbles and a guh constellation lights up.")],
    "raket": [
        ("De raket is half af. Net als mijn huiswerk. Maar de raket gaat ooit echt naar de Kaasmaan!",
         "The rocket is half done. Just like my homework. But the rocket really is going to the Cheese Moon one day!"),
        ("Zet je eigen Lanceerplatform neer en bouw in vier stappen: romp, motor, brandstof en bemanning. Dan: aftellen!",
         "Place your own Launch Pad and build in four steps: hull, engine, fuel and crew. Then: countdown!")],
}

# the Dutch sign lines -> English ("op N", "Geeft N", "straal N" are worked out below)
EN_REGELS = {
    "Dit is een": "This is a", "Dit zijn": "These are", "vadskracht": "chonk power", "vadskracht elk": "chonk power each",
    "Hapluikje": "Snap Hatch", "Stop erin: het": "Put it in: it", "gaat naar je": "goes to your", "Bank Guh!": "Bank Guh!",
    "Snuffelsensor": "Sniff Sensor", "Guhteller": "Guh Counter", "Ruikt jou en": "Smells you and", "zet het lampje": "switches the",
    "aan!": "lamp on!", "Telt de guhs": "Counts the guhs", "voor zijn neus": "under its nose",
    "Knuffelsteen-": "Cuddlestone", "generator": "Generator", "Kaassaus + water": "Cheese sauce +", "= knuffelsteen!": "water = stone!",
    "Mika-rad": "Mika Wheel", "Een Mika rent": "A Mika doesn't", "niet. Hij": "run. He", "schuifelt.": "shuffles.",
    "Weer vadser": "Weather Chonker", "+ Antenne I": "+ Antenna I", "+ Antenne II": "+ Antenna II", "+ Antenne III": "+ Antenna III",
    "Wolkenantennes": "Cloud Antennas", "maken de zone": "make the zone", "groter!": "bigger!",
    "Guhrad": "Guh Wheel", "Knabbelketel": "Nibble Boiler", "Deze twee geven": "These two power", "de aanbouw": "the whole annex",
    "vadskracht!": "with chonk power!", "Deze kern geeft": "This core powers", "de hele Koepel": "the whole Dome", "Naar boven!": "Upstairs!", "Trap of lift": "Stairs or lift", "De aanbouw": "The Annex",
    "Simpele basis": "Simple basics", "Fabriekshal": "Factory Hall", "De Fabriekshal": "The Factory Hall", "De Koepel": "The Dome",
    "Bouwblokken-": "Building Block", "fabriek": "Factory", "Kies een blok,": "Pick a block,", "hij maakt er": "it makes a",
    "een berg van!": "mountain of it!", "Spinmachine": "Spinning Wheel", "Verfbad": "Dye Bath", "Wolfabriek:": "Wool factory:",
    "pluis > wol >": "fluff > wool >", "roze wol!": "pink wool!", "Knuffelrecycler": "Cuddle Recycler", "Oude knuffels": "Old cuddlies",
    "worden pluis!": "become fluff!", "Scheerstal": "Shearing Stable", "Wolkenschaapjes:": "Cloud sheep:", "pluis die": "fluff that",
    "terug groeit!": "grows back!", "Kleurmolen": "Colour Mill", "Bloem erin,": "Flower in,", "2-3 kleurstof": "2-3 dyes",
    "eruit!": "out!", "Regenboogpers": "Rainbow Press", "Pluis + kaassaus": "Fluff + sauce", "= elke kleur!": "= any colour!",
    "Gloeisterkern": "Glowstar Core", "Max 4 ketels": "Max 4 boilers", "per net!": "per net!", "De ketels laten": "The boilers",
    "de hal draaien!": "run the hall!", "Vadszender": "Chonk Sender", "Vadsontvanger": "Chonk Receiver", "Vadskracht door": "Power through",
    "de lucht!": "the air!", "Guhterminal": "Guh Terminal", "Bestelbalie": "Order Desk", "Knabbelpoort-": "Nibble Gate",
    "buis": "Tube", "Thuispoort": "Home Gate", "Bouwkeet": "Builder's Hut", "Bouwt wat jij": "Builds what you",
    "getekend hebt!": "have drawn!", "Luchtblazer": "Air Blower", "Waterdrager": "Water Carrier", "Ga erop staan:": "Stand on it:",
    "woesj!": "whoosh!", "Korstduwer": "Crust Pusher", "Vuurkorf": "Fire Basket", "Hamsterbuis-": "Hamster Tube",
    "station": "Station", "Hamsterbuis-net:": "Hamster tubes:", "reis door alle": "travel through", "dimensies!": "all dimensions!",
    "Geen vadskracht": "Needs no", "nodig": "chonk power", "Kaaskas": "Cheese Greenhouse", "Op eigen ketel": "On its own boiler",
    "Oogster": "Harvester", "Sorteerband": "Sorting Belt", "Saustank": "Sauce Tank", "Sausbuis": "Sauce Pipe", "Van tank": "From tank",
    "naar tank!": "to tank!", "Afnemende": "Diminishing", "opbrengst:": "returns:", "4 raderen vol,": "4 wheels full,",
    "dan steeds": "then less", "minder!": "and less!", "6 raderen,": "6 wheels,", "1 net": "1 net", "Naar de Koepel": "To the Dome",
    "Naar beneden": "Downstairs", "Mega-oven": "Mega Oven", "Knabbel-": "Nibble", "weerstation": "Weather Station",
    "Vadser binnen 16": "Chonker within", "krijgt straal": "16 gets radius", "150!": "150!", "Draaimolen": "Carousel",
    "Een knabbel": "One nibble", "= een ritje!": "= one ride!", "Wolkenvanger": "Cloud Catcher", "Vangt wolkjes,": "Catches clouds,",
    "maakt pluis!": "makes fluff!", "Fossiel-": "Fossil", "broedmachine": "Incubator", "Fossiel + voer": "Fossil + food",
    "= een oerguh!": "= an ancient guh!", "Hamsterknooppunt": "Hamster Hub", "Het hart van het": "The heart of the",
    "Hamsterbuis-net": "hamster tubes", "Mega-knuffel-": "Mega Cuddle", "centrale": "Power Plant", "Geeft 22 per guh": "Gives 22 per guh",
    "Voer + dekentjes": "Food + blankets", "= blije guhs!": "= happy guhs!", "Kaasraket": "Cheese Rocket", "Stap 2 van 4:": "Step 2 of 4:",
    "romp + motor": "hull + engine", "Enderguhrad": "Ender Guh Wheel", "Komeetkern": "Comet Core", "Geeft 50": "Gives 50",
    "met een guh erin": "with a guh in it", "Geeft 500": "Gives 500", "Altijd!": "Always!", "Fossielen": "Fossils", "Mammoet-guh": "Mammoth guh",
    "T-Guh": "T-Guh", "Pteroguh": "Pteroguh", "Oer-stokstaartje": "Ancient meerkat", "Uitzicht": "View", "Mooi he?": "Pretty, huh?",
    "Sterrenkoepel": "Star Dome", "Knabbels erin,": "Nibbles in,", "sterren aan!": "stars on!", "Grote schakelaar": "Big switch", "Kassier": "Cashier", "+ Kassier": "+ Cashier", "Sorteerder": "Sorter",
    "Ga erop staan!": "Stand on it!", "Welkom op de": "Welcome to the", "Koepel!": "Dome!", "Welkom in de": "Welcome to the",
}


def en(nl):
    """The English of a Dutch sign line (None: missing)."""
    if nl in EN_REGELS:
        return EN_REGELS[nl]
    m = re.fullmatch(r"(op|Geeft|straal) (\d+)", nl)
    if m:
        return {"op": "on", "Geeft": "Gives", "straal": "radius"}[m.group(1)] + " " + m.group(2)
    return None


# =====================================================================================================================
# a stand on another floor (techbeurs.Stand with its own walking height)
# =====================================================================================================================
class Stand(tb.Stand):
    def __init__(self, b, x0, z0, rij, soort, naam, y0):
        self.y0 = y0
        super().__init__(b, x0, z0, rij, soort, naam)

    def g(self, lx, ly, lz):
        if self.rij == "N":
            return self.x0 + lx, self.y0 + ly, self.z0 + lz
        return self.x0 + 8 - lx, self.y0 + ly, self.z0 + 8 - lz

    def entity(self, lx, ly, lz, nbt):
        if self.rij == "N":
            x, z = self.x0 + lx, self.z0 + lz
        else:
            x, z = self.x0 + 9 - lx, self.z0 + 9 - lz
        self.b.entity(x, self.y0 + ly, z, nbt)

    def guh_npc(self, lx, lz, yaw=0.0, plek=None):
        x, y, z = self.g(lx, 0, lz)
        if self.rij == "S":
            yaw += 180.0
        plek = plek or self.naam
        self.b.npc(x, y, z, f"techbeurs_{plek}", yaw, kind=KINDEN[plek])

    def kist(self, lx, lz, ly=0, **merk):
        self.zet(lx, ly, lz, KIST, {"id": "minecraft:chest"})
        if merk:
            self.merk(lx, lz, ly, **merk)

    def guh(self, lx, lz, ly=0.0, **extra):
        self.entity(lx + 0.5, ly, lz + 0.5, ms.guh_nbt(1.0, NoAI=ms.Byte(1), **extra))


def aansluiting(s):
    """A machine stand on the bus (techbeurs.bus_aansluiting): the bus lies behind the stand (lz -1); upstream along lx 0
    to the switch at (0, 7), then (1, 7) and (2, 7). Never put anything of the machine side on lx 1 (it would touch lx 0)."""
    tb.bus_aansluiting(s)


def twee_machines(s, a, b_, borden=None):
    """Two single machines at (3, 3) and (6, 3) on the bus: wire down lx 2, along lz 7, up lx 7."""
    aansluiting(s)
    s.machine(3, 3, a)
    s.machine(6, 3, b_)
    s.lijn((2, 3), (2, 7))
    s.lijn((3, 7), (7, 7))
    s.lijn((7, 3), (7, 6))
    s.lamp(2, 2)
    s.lamp(7, 2)


# =====================================================================================================================
# the shell of a floor: walls with windows and pillars like the hall
# =====================================================================================================================
def muren(b, x0, x1, z0, z1, y0, y1, ramen=(4, 8), zonder=()):
    """Walls round x0..x1 x z0..z1 from y0 to y1, like the hall's (a side in `zonder` - 'w', 'o', 'n', 'z' - is left out
    altogether, corners included: the annex leans on the hall's east wall)."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if not (x in (x0, x1) or z in (z0, z1)):
                continue
            if (x == x0 and "w" in zonder) or (x == x1 and "o" in zonder) or (z == z0 and "n" in zonder) or (z == z1 and "z" in zonder):
                continue
            pilaar = (x % 10 == 1 and z in (z0, z1)) or (z % 10 == 0 and x in (x0, x1)) or (x in (x0, x1) and z in (z0, z1))
            for y in range(y0, y1 + 1):
                if pilaar:
                    b.set(x, y, z, "minecraft:quartz_pillar", {"axis": "y"})
                elif y in (y0, y1):
                    b.set(x, y, z, "minecraft:pink_terracotta")
                elif ramen[0] <= y <= ramen[1]:
                    b.set(x, y, z, "minecraft:pink_stained_glass")
                else:
                    b.set(x, y, z, "minecraft:pink_concrete")


def lucht(b, x0, x1, z0, z1, y0, y1):
    """Air in every spot of the box nothing else claims (a stamp clears what stood there)."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(y0, y1 + 1):
                if (x, y, z) not in b.blocks:
                    b.set(x, y, z, "minecraft:air")


def lift(b, x, y, z, boven, kijk):
    """A wolkenliftje: the pad at (x, y, z) (you step on it), the stream up to `boven` (two above the floor up there); it
    puffs you off towards `kijk`."""
    props = {"facing": kijk, "down": "false"}
    b.set(x, y, z, "guhs:wolkenlift", dict(props))
    for yy in range(y + 1, boven + 1):
        b.set(x, yy, z, "guhs:wolkenstroom", dict(props))


def ring(b, x, y, z, open_kant):
    """A fence round a lift hole at (x, z) on the floor y - 1, open on one side."""
    dx, dz = tb.VEC[open_kant]
    for ax in (-1, 0, 1):
        for az in (-1, 0, 1):
            if (ax, az) in ((0, 0), (dx, dz)):
                continue
            b.set(x + ax, y, z + az, HEK)


def bord(b, x, y, z, facing, regels):
    b.sign(x, y, z, facing, regels, wall=False, wood="cherry")


def item_lijst(paren):
    return ms.compounds([{"Slot": ms.Byte(i), "id": item, "count": n} for i, (item, n) in enumerate(paren)])


# =====================================================================================================================
# THE ANNEX (aanbouw frame: x 0 = the hall's east wall, z 0 = hall z 6; y like the hall)
# =====================================================================================================================
A_VL, A_Y0, A_DAK = 1, 2, 11


def aanbouw_romp(b):
    b.fill(1, 0, 0, XA - 1, 0, ZA - 1, "minecraft:smooth_stone")
    b.fill(1, A_VL, 0, XA - 1, A_VL, ZA - 1, VLOER)
    for x in range(1, XA - 1):
        for z in range(13, 17):
            b.set(x, A_VL, z, PAD, {"facing": "north" if (x + z) % 2 else "east"})
    muren(b, 0, XA - 1, 0, ZA - 1, A_Y0, A_DAK - 1, zonder=("w",))
    # the roof = the terrace: quartz edge, pink and white tiles, frog lights for the annex below
    for x in range(1, XA):
        for z in range(ZA):
            rand = x == XA - 1 or z in (0, ZA - 1)
            if rand:
                b.set(x, A_DAK, z, "minecraft:smooth_quartz")
            elif x % 6 == 3 and z % 6 == 3:
                b.zet(x, A_DAK, z, LICHT)
            else:
                b.set(x, A_DAK, z, "minecraft:pink_concrete" if (x // 2 + z // 2) % 2 else "minecraft:white_concrete")
    for x in range(1, XA):
        for z in range(ZA):
            if x == XA - 1 or z in (0, ZA - 1):
                b.set(x, A_DAK + 1, z, HEK)
    # the doorway into the hall (its east wall at x 0, z 13..16 = hall z 19..22)
    for z in range(13, 17):
        for y in range(A_Y0, A_Y0 + 4):
            b.set(0, y, z, "minecraft:air")
        b.set(0, A_Y0 + 4, z, "minecraft:quartz_stairs", {"facing": "west", "half": "top", "shape": "straight", "waterlogged": "false"})
        b.zet(0, A_VL, z, "minecraft:pink_glazed_terracotta[facing=east]")


def aanbouw_trap(b):
    """The stair up to the terrace along the east wall (x 33..34): ten steps south from z 18 (y 2) to z 27 (y 11), a hole
    in the roof over it with a fence round, the landing at z 28. And the wolkenliftje at (28, 15), puffing you west."""
    for i in range(10):
        for x in (33, 34):
            b.set(x, A_Y0 + i, 18 + i, "minecraft:quartz_stairs", {"facing": "south", "half": "bottom", "shape": "straight",
                                                                     "waterlogged": "false"})
            for y in range(A_Y0, A_Y0 + i):
                b.set(x, y, 18 + i, "minecraft:smooth_quartz")
    for x in (33, 34):
        for z in range(18, 27):
            b.set(x, A_DAK, z, "minecraft:air")
    for z in range(17, 27):
        b.set(32, A_DAK + 1, z, HEK)
        b.set(35, A_DAK + 1, z, HEK)
    for x in (33, 34):
        b.set(x, A_DAK + 1, 17, HEK)
    lift(b, 28, A_Y0, 15, A_DAK + 2, "west")
    b.set(28, A_DAK, 15, "minecraft:air")
    ring(b, 28, A_DAK + 1, 15, "west")
    bord(b, 30, A_Y0, 13, "west", ["Naar boven!", "Trap of lift", "", ""])
    # on the terrace: a sign by the door of the Fabriekshal
    bord(b, 2, A_DAK + 1, 12, "east", ["De Fabriekshal", "", "", ""])


def a_hapluik(s):
    aansluiting(s)
    s.machine(3, 3, "hapluikje")
    s.kist(4, 3, Vul=[("guhs:kaas_knabbels", 16)])
    s.lijn((2, 3), (2, 7))
    s.lamp(2, 2)
    s.bord(3, 8, ["Dit is een", "Hapluikje", "op 2", "vadskracht"])
    s.bord(6, 6, ["Stop erin: het", "gaat naar je", "Bank Guh!", ""])


def a_sensor(s):
    twee_machines(s, "snuffelsensor", "guhteller")
    s.zet(3, 1, 3, "minecraft:redstone_lamp[lit=false]")
    s.zet(6, 1, 3, "minecraft:redstone_lamp[lit=false]")
    for lx in (5, 6):
        s.guh(lx, 5)
    s.bord(3, 8, ["Snuffelsensor", "op 1", "vadskracht", ""])
    s.bord(6, 8, ["Guhteller", "op 1", "vadskracht", ""])
    s.bord(4, 6, ["Ruikt jou en", "zet het lampje", "aan!", ""])


def a_generator(s):
    aansluiting(s)
    s.zet(4, -1, 3, "minecraft:water[level=0]")
    s.machine(4, 3, "knuffelsteen_generator")
    s.merk(4, 3, VulSaus=2000, Leeg=16)
    s.lijn((2, 3), (2, 7))
    s.draad((3, 3))
    s.lamp(2, 2)
    s.zet(5, 0, 3, "guhs:saustank[facing=south,niveau=0,saus=leeg]", {"id": "guhs:saustank"})
    s.merk(5, 3, VulSaus=4000)
    s.bord(3, 8, ["Knuffelsteen-", "generator", "op 4", "vadskracht"])
    s.bord(6, 6, ["Kaassaus + water", "= knuffelsteen!", "", ""])


def a_mikarad(s):
    s.zet(4, 0, 3, "guhs:ringsausuman_mikarad[facing=south]", {"id": "guhs:ringsausuman_mikarad"})
    s.b.bronnen.append(s.g(4, 0, 3))
    s.draad((4, 4), (4, 5), (4, 6))
    s.schakelaar(4, 7)
    s.lijn((5, 7), (7, 7))
    s.lamp(6, 6)
    s.lamp(7, 6)
    s.bord(2, 8, ["Mika-rad", "Geeft 10", "vadskracht", ""])
    s.bord(2, 5, ["Een Mika rent", "niet. Hij", "schuifelt.", ""])


def a_weer(s):
    aansluiting(s)
    s.lijn((2, 5), (2, 7))
    s.lijn((3, 5), (8, 5))
    for i, lx in enumerate((2, 4, 6, 8)):
        s.machine(lx, 4, "weer_vadser")
        if i:
            s.zet(lx, 1, 4, f"guhs:wolkenantenne_{i}")
    s.bord(2, 8, ["Weer vadser", "straal 10", "", ""])
    s.bord(4, 8, ["+ Antenne I", "straal 20", "", ""])
    s.bord(6, 8, ["+ Antenne II", "straal 35", "", ""])
    s.bord(8, 8, ["+ Antenne III", "straal 60", "", ""])
    s.bord(5, 2, ["Wolkenantennes", "maken de zone", "groter!", ""])


def a_bron(s):
    """The Guhrad and the Knabbelketel that power the annex: straight onto the bus through the big switch at (4, 0)."""
    ov.guhrad(s.b, *s.g(4, 0, 3), s.f("south"))
    s.b.guhraden.append(s.g(4, 0, 3))
    s.zet(4, 0, 0, f"{SCHAKELAAR}[facing=south,powered=true]")
    s.b.schakelaars.append((s.naam, s.g(4, 0, 0)))
    s.draad((4, 1), (4, 2))
    s.lijn((5, 2), (7, 2))
    s.machine(7, 3, "knabbelketel")
    s.merk(7, 3, Vul=[("guhs:block_of_kaasknabbels", 8)])
    s.lamp(3, 2)
    s.bord(2, 8, ["Guhrad", "Geeft 15", "vadskracht", ""])
    s.bord(7, 8, ["Knabbelketel", "Geeft 80", "vadskracht", ""])
    s.bord(5, 6, ["Deze twee geven", "de aanbouw", "vadskracht!", ""])


A_NOORD = [("hapluik", "machine", a_hapluik), ("sensor", "machine", a_sensor), ("generator", "saus", a_generator)]
A_ZUID = [("mikarad", "bron", a_mikarad), ("weer", "machine", a_weer), ("abron", "bron", a_bron)]


def bouw_aanbouw():
    b = ob.Bouw((XA, YA, ZA), NAAM_A, prefix=PREFIX)
    b.schakelaars, b.kernen, b.guhraden, b.bronnen = [], [], [], []
    aanbouw_romp(b)
    for i, (naam, soort, f) in enumerate(A_NOORD):
        f(Stand(b, 2 + 10 * i, 2, "N", soort, naam, A_Y0))
    for i, (naam, soort, f) in enumerate(A_ZUID):
        f(Stand(b, 2 + 10 * i, 19, "S", soort, naam, A_Y0))
    for x in range(2, 32):
        b.set(x, A_Y0, 1, DRAAD)
        b.set(x, A_Y0, 28, DRAAD)
    for z in range(1, 29):
        b.set(31, A_Y0, z, DRAAD)
    aanbouw_trap(b)
    bord(b, 3, A_Y0, 12, "east", ["De aanbouw", "Simpele basis", "", ""])
    b.npc(4, A_Y0, 17, "techbeurs_aanbouw", -90.0, kind=KINDEN["aanbouw"])
    for (x, z) in ((2, 11), (2, 18), (30, 11)):
        b.set(x, A_Y0, z, "minecraft:potted_cherry_sapling")
    lucht(b, 1, XA - 2, 1, ZA - 2, A_Y0, A_DAK - 1)
    lucht(b, 1, XA - 2, 1, ZA - 2, A_DAK + 1, YA - 1)
    ov.verbind(b)
    tb.verbind_buizen(b)
    return b


# =====================================================================================================================
# THE FABRIEKSHAL (boven frame: hall x, z; y 0 = hall y 11)
# =====================================================================================================================
F_Y0, DEK, K_Y0 = 1, 10, 11


def hal_romp(b):
    for x in range(XB):
        for z in range(ZB):
            if x % 10 == 6 and z % 10 == 5:
                b.zet(x, 0, z, LICHT)
            else:
                b.set(x, 0, z, "minecraft:pink_concrete" if (x + z) % 2 else VLOER)
    for x in range(1, XB - 1):
        for z in list(range(11, 14)) + list(range(28, 31)):
            b.set(x, 0, z, PAD, {"facing": "north" if (x + z) % 2 else "east"})
    muren(b, 0, XB - 1, 0, ZB - 1, F_Y0, DEK - 1, ramen=(3, 7))
    # the door to the terrace of the annex (east wall, z 19..22)
    for z in range(19, 23):
        for y in range(F_Y0, F_Y0 + 3):
            b.set(XB - 1, y, z, "minecraft:air")
        b.set(XB - 1, F_Y0 + 3, z, "minecraft:quartz_stairs", {"facing": "east", "half": "top", "shape": "straight", "waterlogged": "false"})
    # the deck of the Koepel (= the ceiling): quartz beams, tiles, frog lights
    for x in range(XB):
        for z in range(ZB):
            balk = x in (0, XB - 1) or z in (0, ZB - 1) or x % 10 == 1
            if not balk and x % 10 == 6 and z % 6 == 3:
                b.zet(x, DEK, z, LICHT)
            else:
                b.set(x, DEK, z, "minecraft:smooth_quartz" if balk else
                      ("minecraft:white_concrete" if (x // 2 + z // 2) % 2 else "minecraft:pink_concrete"))


def hal_trap(b):
    """Up to the Koepel (west, x 3..4): ten steps north from z 24 (y 1) to z 15 (y 10, in the deck), a hole in the deck over
    z 16..24; and the wolkenliftje at (7, 20) puffing you east."""
    for i in range(10):
        for x in (3, 4):
            b.set(x, F_Y0 + i, 24 - i, "minecraft:quartz_stairs", {"facing": "north", "half": "bottom", "shape": "straight",
                                                                     "waterlogged": "false"})
            for y in range(F_Y0, F_Y0 + i):
                b.set(x, y, 24 - i, "minecraft:smooth_quartz")
    for x in (3, 4):
        for z in range(16, 25):
            b.set(x, DEK, z, "minecraft:air")
    for z in range(16, 26):
        b.set(2, K_Y0, z, HEK)
        b.set(5, K_Y0, z, HEK)
    for x in (3, 4):
        b.set(x, K_Y0, 25, HEK)
    lift(b, 7, F_Y0, 20, DEK + 2, "east")
    b.set(7, DEK, 20, "minecraft:air")
    ring(b, 7, K_Y0, 20, "east")
    bord(b, 6, F_Y0, 25, "south", ["Naar de Koepel", "Trap of lift", "", ""])
    bord(b, 6, K_Y0, 15, "north", ["Naar beneden", "", "", ""])


def f_bouwblokken(s):
    aansluiting(s)
    s.machine(3, 3, "bouwblokken_fabriek", nbt={"Vakken": tb._vakken([(2, "guhs:knuffelsteen_trap", 1)], 12)})
    s.merk(3, 3, Vul=[("guhs:knuffelsteen", 32)], Leeg=32)
    s.lijn((2, 3), (2, 7))
    s.lamp(2, 2)
    for (lx, lz) in ((5, 2), (6, 2), (5, 3)):
        s.zet(lx, 0, lz, "guhs:knuffelsteen_trap[facing=south,half=bottom,shape=straight,waterlogged=false]")
    s.zet(6, 0, 3, "guhs:knuffelsteen")
    s.bord(3, 8, ["Bouwblokken-", "fabriek", "op 12", "vadskracht"])
    s.bord(6, 6, ["Kies een blok,", "hij maakt er", "een berg van!", ""])


def f_wol(s):
    """The Wolfabriek: Spinmachine -> Richtingstuk -> buis -> Verfbad (pink)."""
    aansluiting(s)
    s.machine(3, 3, "spinmachine")
    s.merk(3, 3, Vul=[("guhs:pluis", 32)])
    s.zet(4, 0, 3, f"{RICHTING}[facing=east]", {"id": RICHTING})
    s.zet(5, 0, 3, BUIS)
    s.machine(6, 3, "verfbad")
    s.merk(6, 3, Vul=[("minecraft:pink_dye", 16), ("minecraft:white_wool", 8)], Leeg=16)
    s.lijn((2, 3), (2, 7))
    s.lijn((3, 7), (7, 7))
    s.lijn((7, 3), (7, 6))
    s.lamp(2, 2)
    s.bord(3, 8, ["Spinmachine", "op 6", "vadskracht", ""])
    s.bord(6, 8, ["Verfbad", "op 6", "vadskracht", ""])
    s.bord(4, 5, ["Wolfabriek:", "pluis > wol >", "roze wol!", ""])


def f_recycler(s):
    aansluiting(s)
    s.machine(3, 3, "knuffelrecycler")
    s.merk(3, 3, Vul=[("minecraft:white_carpet", 32)])
    s.zet(4, 0, 3, f"{RICHTING}[facing=east]", {"id": RICHTING})
    s.zet(5, 0, 3, BUIS)
    s.kist(6, 3, Leeg=32)
    s.lijn((2, 3), (2, 7))
    s.lamp(2, 2)
    s.bord(3, 8, ["Knuffelrecycler", "op 6", "vadskracht", ""])
    s.bord(6, 6, ["Oude knuffels", "worden pluis!", "", ""])


def f_scheer(s):
    aansluiting(s)
    s.machine(3, 2, "scheerstal", "east")
    s.merk(3, 2, Leeg=18)
    s.lijn((2, 2), (2, 7))
    s.lamp(2, 1)
    for lx in range(4, 9):
        s.zet(lx, 0, 0, HEK)
        s.zet(lx, 0, 6, HEK)
    for lz in range(1, 6):
        s.zet(8, 0, lz, HEK)
        s.zet(4, 0, lz, HEK)
    for lx in range(5, 8):
        for lz in range(1, 6):
            s.zet(lx, -1, lz, "minecraft:moss_block")
    for (lx, lz) in ((5.5, 2.5), (6.5, 4.5), (7.2, 1.8)):
        s.entity(lx, 0, lz, {"id": "guhs:wolkenschaapje", "Gehouden": ms.Byte(1), "PersistenceRequired": ms.Byte(1),
                             "Invulnerable": ms.Byte(1)})
    s.bord(3, 8, ["Scheerstal", "op 8", "vadskracht", ""])
    s.bord(6, 8, ["Wolkenschaapjes:", "pluis die", "terug groeit!", ""])


def f_kleur(s):
    aansluiting(s)
    s.machine(3, 3, "kleurmolen")
    s.merk(3, 3, Vul=[("minecraft:cornflower", 16)], Leeg=16)
    s.lijn((2, 3), (2, 7))
    s.lamp(2, 2)
    for (lx, bloem) in ((5, "minecraft:potted_cornflower"), (6, "minecraft:potted_poppy"), (7, "minecraft:potted_allium")):
        s.zet(lx, 0, 2, bloem)
    s.bord(3, 8, ["Kleurmolen", "op 8", "vadskracht", ""])
    s.bord(6, 6, ["Bloem erin,", "2-3 kleurstof", "eruit!", ""])


def f_pers(s):
    aansluiting(s)
    s.machine(3, 3, "regenboogpers", nbt={"Vakken": tb._vakken([(2, "minecraft:light_blue_dye", 1)], 5)})
    s.merk(3, 3, Vul=[("guhs:pluis", 16)], Leeg=16)
    s.zet(4, 0, 3, "guhs:saustank[facing=south,niveau=0,saus=leeg]", {"id": "guhs:saustank"})
    s.merk(4, 3, VulSaus=4000)
    s.lijn((2, 3), (2, 7))
    s.lamp(2, 2)
    s.bord(3, 8, ["Regenboogpers", "op 20", "vadskracht", ""])
    s.bord(6, 6, ["Pluis + kaassaus", "= elke kleur!", "", ""])


def f_ketel(s):
    """The source of the Fabriekshal bus: a Gloeisterkern and three Knabbelketels, onto the bus through the switch at (4, 0)."""
    s.zet(4, 0, 0, f"{SCHAKELAAR}[facing=south,powered=true]")
    s.b.schakelaars.append((s.naam, s.g(4, 0, 0)))
    s.draad((4, 1))
    s.machine(4, 2, "gloeisterkern")
    s.b.kernen.append(s.g(4, 0, 2))
    s.lijn((2, 3), (6, 3))
    for lx in (2, 4, 6):
        s.machine(lx, 4, "knabbelketel")
        s.merk(lx, 4, Vul=[("guhs:block_of_kaasknabbels", 8)])
    s.lamp(7, 3)
    s.lamp(1, 3)
    s.bord(1, 8, ["Knabbelketel", "Geeft 80", "vadskracht", ""])
    s.bord(7, 8, ["Gloeisterkern", "Geeft 200", "vadskracht", ""])
    s.bord(4, 6, ["Max 4 ketels", "per net!", "", ""])
    s.bord(4, 8, ["De ketels laten", "de hal draaien!", "", ""])


def f_zender(s):
    twee_machines(s, "vadszender", "vadsontvanger")
    s.bord(3, 8, ["Vadszender", "op 4", "vadskracht", ""])
    s.bord(6, 8, ["Vadsontvanger", "", "", ""])
    s.bord(4, 5, ["Vadskracht door", "de lucht!", "", ""])


def f_terminal(s):
    twee_machines(s, "guhterminal", "bestelbalie")
    s.bord(3, 8, ["Guhterminal", "op 6", "vadskracht", ""])
    s.bord(6, 8, ["Bestelbalie", "op 8", "vadskracht", ""])


def f_poort(s):
    twee_machines(s, "knabbelpoort_buis", "thuispoort")
    s.bord(3, 8, ["Knabbelpoort-", "buis", "op 6", "vadskracht"])
    s.bord(6, 8, ["Thuispoort", "op 200", "vadskracht", ""])


def f_keet(s):
    aansluiting(s)
    s.meer(4, 3, "bouwkeet", "south", 3, 3, 3, "guhs:machine_deel")
    s.lijn((2, 7), (4, 7))
    s.lijn((4, 4), (4, 6))
    s.lamp(5, 6)
    s.bord(2, 8, ["Bouwkeet", "Bouwt wat jij", "getekend hebt!", ""])


def f_lucht(s):
    aansluiting(s)
    s.machine(3, 3, "luchtblazer", "south", extra={"omhoog": "true"})
    s.lijn((2, 3), (2, 7))
    s.machine(6, 2, "waterdrager")
    s.lijn((3, 7), (8, 7))
    s.lijn((8, 2), (8, 6))
    s.draad((7, 2))
    for lx in range(4, 7):
        for lz in range(4, 7):
            s.zet(lx, -1, lz, "minecraft:farmland[moisture=7]")
            s.zet(lx, 0, lz, "minecraft:wheat[age=7]")
    s.lamp(2, 2)
    s.bord(3, 8, ["Luchtblazer", "Ga erop staan:", "woesj!", ""])
    s.bord(6, 8, ["Waterdrager", "op 6", "vadskracht", ""])


def f_korst(s):
    twee_machines(s, "korstduwer", "vuurkorf")
    s.bord(3, 8, ["Korstduwer", "op 10", "vadskracht", ""])
    s.bord(6, 8, ["Vuurkorf", "op 12", "vadskracht", ""])


def f_hamster(s):
    for lx in (2, 6):
        s.zet(lx, 0, 3, "guhs:hamsterbuis_station[facing=south]", {"id": "guhs:hamsterbuis_station"})
    for lx in (3, 4, 5):
        s.zet(lx, 1, 3, "minecraft:light_blue_stained_glass")
    s.zet(2, 1, 3, "minecraft:light_blue_stained_glass")
    s.zet(6, 1, 3, "minecraft:light_blue_stained_glass")
    s.bord(2, 8, ["Hamsterbuis-", "station", "Geen vadskracht", "nodig"])
    s.bord(6, 8, ["Hamsterbuis-net:", "reis door alle", "dimensies!", ""])


F_NOORD = [("bouwblokken", "machine", f_bouwblokken), ("wol", "machine", f_wol), ("recycler", "machine", f_recycler),
           ("scheer", "machine", f_scheer), ("kleur", "machine", f_kleur), ("pers", "saus", f_pers), ("ketel", "bron", f_ketel)]
F_ZUID = [("zender", "bezorg", f_zender), ("terminal", "bezorg", f_terminal), ("poort", "bezorg", f_poort),
          ("keet", "bezorg", f_keet), ("lucht", "bezorg", f_lucht), ("korst", "bezorg", f_korst), ("hamster", "bezorg", f_hamster)]


def f_kaaskas(b):
    """The Kaaskas (x 12..22, z 16..26): a raised bed (y 1) with crops (y 2) round the kern at (17, 2, 21), glass round it,
    on its own Knabbelketel through a switch; the Oogster harvests the front 5 x 5 into a chest."""
    y = F_Y0
    for x in range(12, 23):
        for z in range(16, 27):
            rand = x in (12, 22) or z in (16, 26)
            if rand:
                for yy in range(y, y + 4):
                    b.set(x, yy, z, "minecraft:glass_pane" if yy > y else "minecraft:pink_terracotta")
                b.set(x, y + 4, z, "minecraft:smooth_quartz_slab", {"type": "bottom", "waterlogged": "false"})
            else:
                b.set(x, y + 4, z, "minecraft:glass")
    for x in range(13, 22):
        for z in range(17, 26):
            b.set(x, y, z, VLOER)
    for x in range(13, 22):
        for z in range(18, 26):
            if (x, z) in ((14, 22), (20, 22), (17, 24)):
                b.set(x, y, z, "minecraft:water", {"level": "0"})
            elif x == 17 and z <= 21:
                b.set(x, y, z, VLOER)
            else:
                b.set(x, y, z, "minecraft:farmland", {"moisture": "7"})
                if x % 2:
                    b.set(x, y + 1, z, "guhs:pluisbolletje_plant", {"age": "3"})
                else:
                    b.set(x, y + 1, z, "guhs:kaasknabbelplant", {"age": "7"})
    b.zet(17, y + 1, 21, "guhs:kaaskas[facing=south,snoet=slaapt]", {"id": "guhs:kaaskas", "NeoForgeData": {"guhs_beurs": tb._merk(VulSaus=4000)}})
    for z in range(17, 21):
        b.set(17, y + 1, z, DRAAD)
    b.set(18, y + 1, 17, SCHAKELAAR, {"facing": "south", "powered": "true"})
    b.schakelaars.append(("kaaskas", (18, y + 1, 17)))
    b.zet(19, y + 1, 17, "guhs:knabbelketel[facing=south,snoet=slaapt]",
          {"id": "guhs:knabbelketel", "NeoForgeData": {"guhs_beurs": tb._merk(Vul=[("guhs:block_of_kaasknabbels", 8)])}})
    b.zet(20, y + 1, 17, f"{LAMP}[facing=south,snoet=slaapt]", {"id": LAMP})
    b.set(16, y + 1, 17, DRAAD)
    b.zet(15, y + 1, 17, "guhs:oogster[facing=south,snoet=slaapt]", {"id": "guhs:oogster"})
    b.zet(14, y + 1, 17, f"{RICHTING}[facing=west]", {"id": RICHTING})
    b.zet(13, y + 1, 17, KIST, {"id": "minecraft:chest", "NeoForgeData": {"guhs_beurs": tb._merk(Leeg=64)}})
    bord(b, 17, y, 27, "south", ["Kaaskas", "op 12", "vadskracht", ""])
    bord(b, 20, y, 27, "south", ["Knabbelketel", "Op eigen ketel", "", ""])
    bord(b, 14, y, 27, "south", ["Oogster", "+ Kassier", "", ""])
    b.npc(11, y, 24, "techbeurs_kaaskas", -90.0, kind=KINDEN["kaaskas"])


def f_sorteer(b):
    y = F_Y0
    for x in range(25, 32):
        b.set(x, y, 20, "guhs:sorteerband", {"facing": "east"})
    for (z, inhoud) in ((19, [("guhs:kaas_knabbels", 32)]), (21, [("minecraft:white_wool", 16)])):
        b.zet(32, y, z, KIST, {"id": "minecraft:chest", "Items": item_lijst(inhoud)})
    b.zet(32, y, 20, KIST, {"id": "minecraft:chest", "Items": item_lijst([("guhs:pluis", 24)])})
    bord(b, 28, y, 22, "south", ["Sorteerband", "Geen vadskracht", "nodig", ""])
    b.npc(29, y, 18, "techbeurs_sorteer", 180.0, kind=KINDEN["sorteer"])
    bord(b, 31, y, 22, "south", ["Sorteerder", "", "", ""])


def f_saus(b):
    """Saustank -> Sausbuis -> Saustank (x 37..44, z 20)."""
    y = F_Y0
    b.zet(37, y, 20, "guhs:saustank[facing=south,niveau=0,saus=leeg]",
          {"id": "guhs:saustank", "NeoForgeData": {"guhs_beurs": tb._merk(VulSaus=8000)}})
    for x in range(38, 44):
        b.set(x, y, 20, "guhs:sausbuis", {"north": "false", "south": "false", "up": "false", "down": "false", "east": "true", "west": "true"})
    b.zet(44, y, 20, "guhs:saustank[facing=south,niveau=0,saus=leeg]",
          {"id": "guhs:saustank", "NeoForgeData": {"guhs_beurs": tb._merk(LeegSaus=6000)}})
    bord(b, 37, y, 22, "south", ["Saustank", "Geen vadskracht", "nodig", ""])
    bord(b, 40, y, 22, "south", ["Sausbuis", "Van tank", "naar tank!", ""])


def f_raderen(b):
    """The raderen-hoek (x 49..61, z 17..26): six Guhraden on ONE net with three lamps; the sign tells about afnemende
    opbrengst. Its own net: it touches nothing else."""
    y = F_Y0
    for (rz, wz) in ((19, 18), (24, 23)):
        for x in (50, 54, 58):
            ov.guhrad(b, x, y, rz, "south")
            b.guhraden.append((x, y, rz))
        for x in range(49, 61):
            b.set(x, y, wz, DRAAD)
    for z in range(18, 24):
        b.set(60, y, z, DRAAD)
    for z in (18, 21, 23):
        b.zet(61, y, z, f"{LAMP}[facing=east,snoet=slaapt]", {"id": LAMP})
    bord(b, 52, y, 26, "south", ["Afnemende", "opbrengst:", "4 raderen vol,", "dan steeds"])
    bord(b, 56, y, 26, "south", ["minder!", "6 raderen,", "1 net", ""])


def f_landing(b):
    y = F_Y0
    for x in range(64, 72):
        for z in range(15, 27):
            b.set(x, 0, z, "minecraft:pink_concrete" if (x + z) % 2 else VLOER)
    bord(b, 68, y, 17, "west", ["Welkom in de", "Fabriekshal", "", ""])
    b.npc(68, y, 24, "techbeurs_fabriek", 90.0, kind=KINDEN["fabriek"])
    for (x, z) in ((65, 15), (65, 26)):
        b.set(x, y, z, "minecraft:potted_pink_tulip")


# =====================================================================================================================
# THE KOEPEL (boven frame, walking at y 11)
# =====================================================================================================================
def koepel_romp(b):
    for x in range(XB):
        for z in range(ZB):
            if x in (0, XB - 1) or z in (0, ZB - 1):
                b.set(x, K_Y0, z, HEK)
    for x in range(1, XB - 1):
        for z in list(range(11, 14)) + list(range(28, 31)):
            b.set(x, DEK, z, PAD, {"facing": "north" if (x + z) % 2 else "east"})


def k_bus_machine(s, lx=4, lz=3):
    """A Koepel machine kern at (lx, lz) on the bus: lx 2 down, along lz 7, up the kern's front column."""
    aansluiting(s)
    s.lijn((2, 7), (lx, 7))
    s.lijn((lx, lz + 1), (lx, 6))


def k_oven(s):
    s.meer(4, 3, "mega_oven", "south", 3, 2, 2, "guhs:machine_deel")
    s.merk(4, 3, Vul=[("guhs:roze_graan", 48)], Leeg=12)
    k_bus_machine(s)
    s.lamp(5, 6)
    s.bord(2, 8, ["Mega-oven", "op 24", "vadskracht", ""])


def k_weer(s):
    s.meer(4, 3, "knabbel_weerstation", "south", 1, 2, 1, "guhs:machine_deel")
    s.merk(4, 3, Vul=[("guhs:kaas_knabbels", 16)])
    k_bus_machine(s)
    s.lamp(5, 6)
    s.bord(2, 8, ["Knabbel-", "weerstation", "op 16", "vadskracht"])
    s.bord(6, 8, ["Vadser binnen 16", "krijgt straal", "150!", ""])


def k_draai(s):
    s.meer(4, 3, "draaimolen", "south", 3, 2, 2, "guhs:machine_deel")
    k_bus_machine(s)
    s.lamp(5, 6)
    s.guh(6, 5)
    s.guh(2, 4)
    s.bord(2, 8, ["Draaimolen", "op 10", "vadskracht", ""])
    s.bord(6, 8, ["Een knabbel", "= een ritje!", "", ""])


def k_vanger(s):
    s.machine(4, 3, "wolkenvanger")
    s.kist(4, 3, ly=-1, Leeg=16)
    k_bus_machine(s)
    s.lamp(5, 6)
    s.bord(2, 8, ["Wolkenvanger", "op 12", "vadskracht", ""])
    s.bord(6, 8, ["Vangt wolkjes,", "maakt pluis!", "", ""])


def k_broed(s):
    s.machine(4, 3, "fossiel_broedmachine", nbt={"Vakken": tb._vakken([(0, "guhs:fossiel_mammoet", 1)], 2)})
    k_bus_machine(s)
    s.lamp(5, 6)
    s.bord(2, 8, ["Fossiel-", "broedmachine", "op 20", "vadskracht"])
    s.bord(6, 8, ["Fossiel + voer", "= een oerguh!", "", ""])


def k_knooppunt(s):
    s.machine(4, 3, "hamsterknooppunt")
    k_bus_machine(s)
    s.lamp(5, 6)
    s.bord(2, 8, ["Hamsterknooppunt", "Het hart van het", "Hamsterbuis-net", ""])


def k_centrale(s):
    """The Mega-knuffelcentrale (5 x 1 x 3) with its 16 beursguhs, onto the bus through the switch at (7, 0)."""
    s.meer(4, 5, "mega_knuffelcentrale", "south", 5, 1, 3, "guhs:mega_knuffelcentrale_deel")
    s.merk(4, 5, Guhs=16, Vul=[("guhs:block_of_kaasknabbels", 16), ("minecraft:white_wool", 8)])
    s.b.bronnen.append(s.g(4, 0, 5))
    for i in range(16):
        lx, lz = 2 + i % 5, 3 + (i // 5) % 3
        s.guh(lx, lz, 0.5)
    s.draad((4, 6), (5, 6), (6, 6), (7, 6))
    s.lijn((7, 1), (7, 5))
    s.zet(7, 0, 0, f"{SCHAKELAAR}[facing=south,powered=true]")
    s.b.schakelaars.append((s.naam, s.g(7, 0, 0)))
    s.lamp(8, 6)
    s.bord(2, 8, ["Mega-knuffel-", "centrale", "Geeft 22 per guh", "vadskracht"])
    s.bord(5, 8, ["Voer + dekentjes", "= blije guhs!", "", ""])


def k_raket(s):
    """The Lanceerplatform at stage 2 of 4 (romp + motor done): the BeursUitbouw builds those stages on it."""
    s.zet(4, 0, 4, "guhs:lanceerplatform", {"id": "guhs:lanceerplatform", "Klaar": 2})
    s.bord(0, 8, ["Kaasraket", "Stap 2 van 4:", "romp + motor", ""])
    s.guh_npc(8, 8, plek="raket")


def eigen_net(s, lx, lz, naam):
    """A showcase source with its own net: wire from its front to the switch at (lx, 7), lamps after it."""
    s.draad(*[(lx, z) for z in range(lz + 1, 7)])
    s.schakelaar(lx, 7)
    s.lijn((lx + 1, 7), (7, 7))
    s.lamp(7, 6)
    s.lamp(6, 6)


def k_ender(s):
    guh = ms.guh_nbt(1.0)
    s.meer(4, 4, "enderguhrad", "south", 5, 5, 1, "guhs:machine_deel", nbt={"HeeftGuh": ms.Byte(1), "Guh": guh})
    s.b.bronnen.append(s.g(4, 0, 4))
    eigen_net(s, 4, 4, "ender")
    s.bord(1, 8, ["Enderguhrad", "Geeft 50", "met een guh erin", ""])


def k_komeet(s):
    s.machine(4, 3, "komeetkern")
    s.b.bronnen.append(s.g(4, 0, 3))
    eigen_net(s, 4, 3, "komeet")
    s.bord(1, 8, ["Komeetkern", "Geeft 500", "Altijd!", ""])


def k_kern(s):
    s.machine(4, 2, "gloeisterkern")
    s.b.kernen.append(s.g(4, 0, 2))
    s.zet(4, 0, 1, f"{SCHAKELAAR}[facing=south,powered=true]")
    s.b.schakelaars.append((s.naam, s.g(4, 0, 1)))
    s.draad((4, 0))
    for (lx, lz) in ((3, 3), (4, 3), (5, 3)):
        for ly in (0, 1):
            s.zet(lx, ly, lz, "minecraft:iron_bars")
    for lx in (3, 5):
        s.lamp(lx, 2)
    s.bord(2, 8, ["Gloeisterkern", "Geeft 200", "vadskracht", ""])
    s.bord(6, 8, ["Deze kern geeft", "de hele Koepel", "vadskracht!", ""])


def k_vitrines(s):
    for (lx, fossiel) in ((1, "mammoet"), (3, "tguh"), (5, "pteroguh"), (7, "stokstaartje")):
        s.zet(lx, 0, 3, "minecraft:quartz_pillar[axis=y]")
        s.zet(lx, 2, 3, "minecraft:glass")
        x, y, z = s.g(lx, 1, 3)
        s.b.entity(x + 0.5, y + 0.03, z + 0.5, {"id": "minecraft:item_frame", "Facing": ms.Byte(1), "Fixed": ms.Byte(1),
                                                 "Invulnerable": ms.Byte(1), "TileX": x, "TileY": y, "TileZ": z,
                                                 "Item": {"id": f"guhs:fossiel_{fossiel}", "count": 1}})
    s.bord(1, 6, ["Mammoet-guh", "", "", ""])
    s.bord(3, 6, ["T-Guh", "", "", ""])
    s.bord(5, 6, ["Pteroguh", "", "", ""])
    s.bord(7, 6, ["Oer-stokstaartje", "", "", ""])
    s.bord(4, 8, ["Fossielen", "", "", ""])


def k_uitzicht(s):
    for lx in (2, 4, 6):
        s.zet(lx, 0, 2, "minecraft:cherry_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]")
    for (lx, lz) in ((1, 1), (7, 1), (1, 5), (7, 5)):
        s.zet(lx, 0, lz, "minecraft:potted_pink_tulip")
    s.bord(4, 6, ["Uitzicht", "Mooi he?", "", ""])


K_NOORD = [("oven", "droom", k_oven), ("kweer", "droom", k_weer), ("draai", "droom", k_draai), ("vanger", "droom", k_vanger),
           ("broed", "droom", k_broed), ("knooppunt", "droom", k_knooppunt), ("centrale", "bron", k_centrale)]
K_ZUID = [("raket", "droom", k_raket), ("ender", "bron", k_ender), ("komeet", "bron", k_komeet), ("kkern", "bron", k_kern),
          ("vitrines", "droom", k_vitrines), ("uitzicht", "droom", k_uitzicht)]


def k_koepel(b):
    """The glass dome (x 24..48, z 15..27) over the Sterrenkoepel at (36, 11, 21), fed from the bus along z 17 through the
    switch at (70, 17)."""
    y = K_Y0
    cx, cz = 36, 21
    for x in range(71, 72):
        b.set(x, y, 17, DRAAD)
    b.set(70, y, 17, SCHAKELAAR, {"facing": "east", "powered": "true"})
    b.schakelaars.append(("sterrenkoepel", (70, y, 17)))
    for x in range(38, 70):
        b.set(x, y, 17, DRAAD)
    for z in range(18, 23):
        b.set(38, y, z, DRAAD)
    b.set(37, y, 22, DRAAD)
    b.set(36, y, 22, DRAAD)
    nbt = {"Niveau": 6}
    s = Stand(b, cx - 4, cz - 4, "N", "droom", "sterren", y)     # (a frame for meer(): kern at local (4, 4))
    s.meer(4, 4, "sterrenkoepel", "south", 3, 2, 2, "guhs:machine_deel", nbt=nbt)
    s.lamp(5, 6)
    s.bord(6, 7, ["Sterrenkoepel", "op 6", "vadskracht", ""])
    s.bord(5, 8, ["Knabbels erin,", "sterren aan!", "", ""])
    # the dome: a glass shell, open at the east and west ends
    rx, ry, rz = 12.5, 8.0, 6.6
    for x in range(cx - 13, cx + 14):
        for z in range(cz - 7, cz + 8):
            for yy in range(y, y + 9):
                d = math.sqrt(((x - cx) / rx) ** 2 + ((yy - y + 0.5) / ry) ** 2 + ((z - cz) / rz) ** 2)
                if 0.86 <= d <= 1.0 and (x, yy, z) not in b.blocks:
                    deur = abs(z - cz) <= 1 and yy <= y + 2
                    b.set(x, yy, z, "minecraft:air" if deur else
                          ("minecraft:light_blue_stained_glass" if (x + yy) % 4 else "minecraft:white_stained_glass"))
    # where the wire comes through the shell: no glass on it
    b.npc(31, y, 24, "techbeurs_koepel", 0.0, kind=KINDEN["koepel"])
    bord(b, 30, y, 21, "east", ["Welkom op de", "Koepel!", "", ""])


def bouw_boven():
    b = ob.Bouw((XB, YB, ZB), NAAM_B, prefix=PREFIX)
    b.schakelaars, b.kernen, b.guhraden, b.bronnen = [], [], [], []
    hal_romp(b)
    hal_trap(b)
    for i, (naam, soort, f) in enumerate(F_NOORD):
        f(Stand(b, 2 + 10 * i, 2, "N", soort, naam, F_Y0))
    for i, (naam, soort, f) in enumerate(F_ZUID):
        f(Stand(b, 2 + 10 * i, 31, "S", soort, naam, F_Y0))
    for (x0, y) in ((2, F_Y0), (2, K_Y0)):
        for x in range(x0, XB - 1):
            b.set(x, y, 1, DRAAD)
            b.set(x, y, ZB - 2, DRAAD)
        for z in range(1, ZB - 1):
            b.set(XB - 2, y, z, DRAAD)
    f_kaaskas(b)
    f_sorteer(b)
    f_saus(b)
    f_raderen(b)
    f_landing(b)
    lucht(b, 1, XB - 2, 1, ZB - 2, F_Y0, DEK - 1)
    koepel_romp(b)
    for i, (naam, soort, f) in enumerate(K_NOORD):
        f(Stand(b, 2 + 10 * i, 2, "N", soort, naam, K_Y0))
    for i, (naam, soort, f) in enumerate(K_ZUID):
        f(Stand(b, 2 + 10 * i, 31, "S", soort, naam, K_Y0))
    k_koepel(b)
    lucht(b, 0, XB - 1, 0, ZB - 1, K_Y0, YB - 1)
    ov.verbind(b)
    tb.verbind_buizen(b)
    return b


# =====================================================================================================================
# the proof: every switch cuts, every machine and lamp hangs on a source
# =====================================================================================================================
BRONNEN = {"guhs:knabbelketel", "guhs:komeetkern", "guhs:enderguhrad", "guhs:mega_knuffelcentrale", "guhs:ringsausuman_mikarad",
           "guhs:gloeisterkern"}
DELEN = {"guhs:machine_deel", "guhs:techbron_kussen_deel", "guhs:techbron_vloer_deel", "guhs:techmachine_plantagebak_deel",
         "guhs:guh_wheel_part", "guhs:mega_knuffelcentrale_deel"}
# knopen that need no source of their own here: the Hamsterbuis-stations are no vadskracht blocks
LOS = set()


def check_netten(b):
    fouten = []
    alles = tb.netten(b, True)
    zonder = tb.netten(b, False)
    for (naam, p) in b.schakelaars:
        buren = [(p[0] + dx, p[1], p[2] + dz) for (dx, _, dz) in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1))]
        aan = {zonder[q] for q in buren if q in zonder}
        if len(aan) < 2:
            fouten.append(f"{b.naam}: de schakelaar van {naam} op {p} knipt niets door (kanten: {aan})")
    bron_netten = {alles[k] for k in b.kernen + b.guhraden + b.bronnen if k in alles}
    for p, (n, _, _) in b.blocks.items():
        if n in BRONNEN and p in alles:
            bron_netten.add(alles[p])
    for p, (n, _, _) in b.blocks.items():
        if n in tb._knoop_namen() and n not in DELEN and n not in LOS and alles.get(p) not in bron_netten:
            fouten.append(f"{b.naam}: {n} op {p} hangt aan geen enkele bron")
    for k in b.kernen:
        if k not in alles:
            fouten.append(f"{b.naam}: de kern op {k} ligt niet in een net")
    kn = [alles[k] for k in b.kernen if k in alles]
    if len(set(kn)) != len(kn):
        fouten.append(f"{b.naam}: twee Gloeisterkernen in één net")
    return fouten


def zorg_tag():
    """The Mega-knuffelcentrale (and its parts) in #guhs:vadskracht: Guhdraad bends towards it and the hover readout works."""
    p = ov.TAG
    data = json.load(open(p, encoding="utf-8"))
    for c in CENTRALE:
        if c not in data["values"]:
            data["values"].append(c)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")
    ov.VADS.update(CENTRALE)


def build():
    zorg_tag()
    uit = []
    for maak, template in ((bouw_aanbouw, TEMPLATE_A), (bouw_boven, TEMPLATE_B)):
        b = maak()
        fouten = check_netten(b) + b.fouten
        for f in fouten:
            print("techbeurs_boven:", f)
        if fouten:
            raise SystemExit(1)
        b.save(template)
        uit.append(b)
    return uit


def _direct():
    R = os.path.join("src", "main", "resources")
    A = os.path.join(R, "assets", "guhs")
    bouwsels = build()
    NL, EN = {}, {}
    for stand, regels in PRAAT.items():
        for i, (nl, en_) in enumerate(regels, 1):
            NL[f"quest.guhs.techbeurs.{stand}.{i}"] = nl
            EN[f"quest.guhs.techbeurs.{stand}.{i}"] = en_
    mist = []
    for k, nl in sign_text.TEXTS.items():
        if k.startswith(PREFIX + "."):
            e = tb.EN_REGELS.get(nl) or en(nl)   # (a line the hall has too keeps the hall's English)
            if e is None:
                mist.append(nl)
            NL[k] = nl
            EN[k] = e or nl
    if mist:
        raise SystemExit("techbeurs_boven: no English for the sign lines " + repr(sorted(set(mist))))
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
    overlay = json.load(open(tb.EN_OVERLAY, encoding="utf-8")) if os.path.exists(tb.EN_OVERLAY) else {}
    overlay.update(EN)
    with open(tb.EN_OVERLAY, "w", encoding="utf-8", newline="\n") as f:
        json.dump(dict(sorted(overlay.items())), f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"techbeurs_boven: {len(NL)} texts, templates {', '.join(b.naam for b in bouwsels)}")


if __name__ == "__main__":
    _direct()
