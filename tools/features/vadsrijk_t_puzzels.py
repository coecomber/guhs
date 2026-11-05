"""
1.10.0 "Het Vadsrijk", night round T, slice "puzzels" (T_BRIEF.md, VOORSTEL_ROADMAP.md 5.1). Java: feature/vadsrijk/puzzels.

  - the PUZZLE DATA: every puzzle/secret of zone 1-3 as data/guhs/vadsrijk_puzzels/<id>.json (layout in LOCAL coordinates,
    anchored to a POI id of WereldPlekken / Z2WereldPlekken / Z3WereldPlekken + an offset, so a world shift moves them along);
  - validation: every light puzzle is brute-forced (a solution exists and the start state is NOT solved), every push puzzle
    is solved by a BFS (the solution pushes are written into the json for the dev "los" command + the gametests);
  - the 19 puzzle blocks (16x16 pixel art, blockstates, models), the 9 "Gescheurde bladzijde" lore pages (items);
  - every text (Dutch; en_us gets English for the shared lines and the riddles, Dutch for the rest).

Layout legend (one row = one z line, north first; one char = one x cell; y 0 = where you stand, the floor is at y -1):
  . or space  floor + air          #  wall, 2 high            =  low wall (1 high)
  L lantern (deco)                 b  bush (leaves)           f  flower            k  treasure glint (amethyst)
  1 2 3  pillar of that height (parkour)                       E  end plate on a 3-high pillar (tijdrit)
  D  secret door, 2 high (opens when solved)                   X  cracked wall, 2 high (click = the secret)
  other letters: the puzzle's own legend (elements).
A roof ("dak") is a second map: '#' = a roof block at y 2.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_puzzels.py
"""
import itertools
import json
import os
import random
from collections import deque

import numpy as np

from vadsrijk_economie import Doek, _png, _licht

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
D = os.path.join(R, "data", "guhs")
DATA_DIR = os.path.join(D, "vadsrijk_puzzels")
BTEX = "vadsrijk/puzzels"        # textures/block/vadsrijk/puzzels/<name>.png
ITEX = "vadsrijk/puzzels"        # textures/item/vadsrijk/puzzels/<name>.png
P = "vadsrijk.puzzels."


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


# ======================================================================================================================
# palettes per zone
# ======================================================================================================================
PALET = {
    1: dict(vloer="minecraft:moss_block", muur="minecraft:mossy_stone_bricks", platform="minecraft:mushroom_stem",
            struik="minecraft:azalea_leaves[persistent=true]", bloem="minecraft:pink_tulip", dak="minecraft:red_mushroom_block",
            vul="minecraft:dirt", lamp="minecraft:lantern"),
    2: dict(vloer="minecraft:snow_block", muur="minecraft:calcite", platform="minecraft:amethyst_block",
            struik="minecraft:spruce_leaves[persistent=true]", bloem="minecraft:white_tulip", dak="minecraft:smooth_quartz",
            vul="minecraft:snow_block", lamp="minecraft:soul_lantern"),
    3: dict(vloer="minecraft:packed_mud", muur="minecraft:spruce_wood", platform="minecraft:honeycomb_block",
            struik="minecraft:flowering_azalea_leaves[persistent=true]", bloem="minecraft:allium", dak="minecraft:spruce_planks",
            vul="minecraft:dirt", lamp="minecraft:lantern"),
}
# the floor under a flower must be soil: flowers stand on a grass/moss patch
BLOEMGROND = {1: "minecraft:moss_block", 2: "minecraft:moss_block", 3: "minecraft:moss_block"}

# the puzzle blocks: soort -> (shape, interactive)
BLOKKEN = ["drukplaat", "klanksteen", "voorspeelsteen", "symboolsteen", "probeerknop", "herstelknop", "spiegel", "lichtbron",
           "lichtdoel", "schuifblok", "doelvloer", "startlantaarn", "eindplaat", "scheur", "geheimdeur", "hendel",
           "fluistersteen", "sterrensteen", "sterrenkaart"]
VLOERTEGELS = {"doelvloer", "sterrensteen"}   # elements that ARE the floor (y -1)

KLEUREN = ["roze", "geel", "blauw", "groen"]   # drukplaat stand 0..3
SYMBOLEN = ["ster", "hart", "maan", "zon", "druppel", "blad"]   # symboolsteen stand 0..5
RICHTING = {"N": 0, "O": 1, "Z": 2, "W": 3}
DXZ = [(0, -1), (1, 0), (0, 1), (-1, 0)]   # N O Z W


# ======================================================================================================================
# the puzzles
# ======================================================================================================================
PUZZELS = []


def puzzel(pid, zone, soort, anker, kaart, legenda=None, oplossing=None, dak=None, **kw):
    PUZZELS.append(dict(id=pid, zone=zone, soort=soort, anker=anker, kaart=kaart, legenda=legenda or {}, oplossing=oplossing,
                        dak=dak, **kw))


# ---- zone 1: De Knuffelzwammen (lv 1-20) ---------------------------------------------------------------------------------
puzzel("z1_kleurplaten", 1, "volgorde", ("z1", "kampvuur_klodderweide_1", 14, 4), [
    "L.=====.L",
    ".........",
    "..g...r..",
    ".........",
    "..b...y..",
    ".........",
    "L.=====.L",
], {"g": ("drukplaat", 3), "r": ("drukplaat", 0), "b": ("drukplaat", 2), "y": ("drukplaat", 1)}, "grby",
    naam="De Kleurplaten",
    uitleg="Stap op de platen in de goede volgorde. Eerst het gras, dan een blozende wang, dan de lucht, en als laatste de zon.",
    hint1="Gras is groen, een blozende wang is roze...",
    hint2="De volgorde is: groen, roze, blauw, geel.",
    aanwijzing="Bij het eerste kampvuur van de Klodderweide liggen vier gekleurde platen.",
    beloning=dict(munten=400, bladzijde=1))

puzzel("z1_zwammenlied", 1, "volgorde", ("z1", "kampvuur_sporenwoud_1", -14, 6), [
    "L.......L",
    ".........",
    "..a.b.c..",
    "....d....",
    ".........",
    "....V....",
    "L.......L",
], {"a": ("klanksteen", 0), "b": ("klanksteen", 2), "c": ("klanksteen", 4), "d": ("klanksteen", 7), "V": ("voorspeelsteen", 0)},
    "acbcd",
    naam="Het Zwammenlied",
    uitleg="Klik op de voorspeelsteen en luister. Speel het liedje daarna na op de zingende zwamstenen.",
    hint1="Kijk goed welke steen oplicht. Het liedje heeft vijf tonen, en één steen zingt twee keer.",
    hint2="Links, rechts, midden, rechts, en dan de steen vooraan.",
    aanwijzing="In het Sporenwoud zingen de stenen een liedje, vlak bij het kampvuur.",
    beloning=dict(munten=500, items=[["guhs:vr_item_verbetersteentje", 2]]))

puzzel("z1_lichtstraal", 1, "licht", ("z1", "lichtstralen.1", 10, 10), [
    "####.####",
    "#S..m..m#",
    "#.......#",
    "#.......#",
    "#.m.m..m#",
    "#.......#",
    "#......T#",
    "#########",
], {"S": ("lichtbron", RICHTING["O"]), "m": ("spiegel", 0), "T": ("lichtdoel", 0)},
    naam="De Verdwaalde Lichtstraal",
    uitleg="Draai de spiegels (klik erop) tot de lichtstraal het lichtdoel raakt.",
    hint1="Volg de straal vanaf de lichtsteen. Waar hij een spiegel raakt, buigt hij af.",
    hint2="De straal moet twee keer omlaag en één keer naar rechts. De spiegel linksonder en rechtsboven heb je niet nodig.",
    aanwijzing="Tussen de lichtstralen van het Sporenwoud is er één verdwaald.",
    beloning=dict(munten=600, bladzijde=2))

puzzel("z1_gebarsten_muur", 1, "geheim", ("z1", "kampvuur_druipgrotten_3", 12, 0), [
    ".#####.",
    ".#...#.",
    ".#.k.#.",
    ".##X##.",
    ".......",
], dak=[
    ".#####.",
    ".#####.",
    ".#####.",
    ".#####.",
    ".......",
], stil=True, reset=600,
    naam="De Gebarsten Muur",
    uitleg="Een muurtje met een barst erin. Er zit iets achter...",
    hint1="Klik op de barst.", hint2="Klik op de barst.",
    aanwijzing="Bij het onderste kampvuur van de Druipgrotten staat een muurtje met een barst.",
    beloning=dict(munten=350, bladzijde=3))

puzzel("z1_fluistersteen", 1, "wachtwoord", ("z1", "kampvuur_klodderweide_3", 10, 8), [
    "L...L",
    ".f.f.",
    "..F..",
    ".f.f.",
    "L...L",
], {"F": ("fluistersteen", 0)}, woorden=["vads", "de vads", "vadsje"],
    naam="De Fluistersteen",
    uitleg="Klik op de steen en fluister het antwoord in de chat.",
    vraag="Ik word niet minder als je me weggeeft, ik word juist méér. Wat ben ik?",
    vraag_en="I don't get smaller when you give me away, I get BIGGER. What am I? (answer: one Guhs word)",
    hint1="Elke guh weet het: het groeit alleen als je het deelt.",
    hint2="Het woord heeft vier letters en staat als balk op je scherm.",
    hint1_en="Every guh knows: it only grows when you share it.", hint2_en="Four letters, and it's a bar on your screen.",
    aanwijzing="Achter in de Klodderweide fluistert een steen tussen de bloemen.",
    beloning=dict(munten=300, items=[["guhs:vr_item_herrolknabbel", 1]]))

puzzel("z1_sterrenveld", 1, "patroon", ("z1", "hoedhupperwei", 0, -14), [
    "L.....L",
    ".sssss.",
    ".sssss.",
    ".sssss.",
    ".sssss.",
    ".sssss.",
    "L.K.P.L",
], {"s": ("sterrensteen", 0), "K": ("sterrenkaart", 0), "P": ("probeerknop", 0)}, [
    ".X.X.",
    "X.X.X",
    "X...X",
    ".X.X.",
    "..X..",
], nacht=True,
    naam="Het Sterrenveld",
    uitleg="Alleen 's nachts! Lees de sterrenkaart en laat de juiste sterrenstenen oplichten. Druk dan op de knop.",
    hint1="Het sterrenbeeld heet De Knuffelster: het is een hartje.",
    hint2="Klik de sterrenkaart nog eens: elke ster op de kaart is één steen. Bovenste rij: de 2e en 4e steen.",
    aanwijzing="Op de Hoedhupperwei liggen stenen die alleen 's nachts wakker worden.",
    beloning=dict(munten=700, ontgrendel="pz_vleugels_sterrenstof"))

puzzel("z1_vuurtorenschuur", 1, "geheim", ("z1", "kampvuur_vuurtoren", 8, 8), [
    "..bhb..",
    ".#####.",
    ".#.k.#.",
    ".#...#.",
    ".##D##.",
    ".......",
], {"h": ("hendel", 0)}, dak=[
    ".......",
    ".#####.",
    ".#####.",
    ".#####.",
    ".#####.",
    ".......",
], stil=True, reset=600,
    naam="Het Schuurtje bij de Vuurtoren",
    uitleg="Een schuurtje zonder deurklink...",
    hint1="Kijk eens achter het schuurtje, tussen de struikjes.", hint2="Achter het schuurtje zit een hendeltje.",
    aanwijzing="Op Knuffelkamp staat bij de vuurtoren een schuurtje dat nooit opengaat.",
    beloning=dict(munten=200, items=[["guhs:vr_eco_klodderreus_knuffel", 1]]))

puzzel("z1_symboolslot", 1, "symbool", ("z1", "kampvuur_zwamtempel_1", 12, -4), [
    "L.....L",
    ".......",
    ".a.b.c.",
    ".......",
    "L..P..L",
], {"a": ("symboolsteen", 0), "b": ("symboolsteen", 0), "c": ("symboolsteen", 0), "P": ("probeerknop", 0)}, "hart ster maan",
    naam="Het Tempelslot",
    uitleg="Draai de symboolstenen (klik) en druk op de knop. Op de oude tempelmuur stond: wat je voelt, wat je ziet en wat je droomt.",
    hint1="Voelen doe je met je hart. Zien doe je 's nachts aan de hemel.",
    hint2="Hart, ster, maan.",
    aanwijzing="Op weg naar de Zwamtempel staan drie draaistenen.",
    beloning=dict(munten=800, items=[["guhs:vr_item_verbetersteentje", 3]]))

# ---- zone 2: De Splinterpieken (lv 20-30) --------------------------------------------------------------------------------
puzzel("z2_ijsschuif", 2, "schuif", ("z2", "kampvuur_knuffelmeer_1", 12, 6), [
    "########",
    "#......#",
    "#.B..x.#",
    "#..##..#",
    "#.x..B.#",
    "#......#",
    "#...R..#",
    "###..###",
], {"B": ("schuifblok", 0), "x": ("doelvloer", 0), "R": ("herstelknop", 0)},
    naam="De Ijsblokken",
    uitleg="Duw de ijsblokken (klik erop) naar de roze vakjes. Vast? Druk op de herstelknop.",
    hint1="Een blok gaat de kant op waar jij vandaan duwt. Je kunt niet trekken!",
    hint2="Het linkerblok gaat twee keer omlaag, het rechterblok twee keer omhoog.",
    aanwijzing="Bij het Knuffelmeer liggen ijsblokken die naar huis willen.",
    beloning=dict(munten=1800, bladzijde=4))

puzzel("z2_kristallied", 2, "volgorde", ("z2", "kampvuur_galmhellingen_1", 12, 0), [
    "L.......L",
    "..a...e..",
    ".........",
    "..b...d..",
    "....c....",
    "....V....",
    "L.......L",
], {"a": ("klanksteen", 0), "b": ("klanksteen", 2), "c": ("klanksteen", 4), "d": ("klanksteen", 5), "e": ("klanksteen", 7),
    "V": ("voorspeelsteen", 0)}, "cbadec",
    naam="Het Galmlied",
    uitleg="De Galmhellingen zingen terug. Luister naar de voorspeelsteen en speel het lied na.",
    hint1="Het lied heeft zes tonen. Het begint en eindigt bij de steen in het midden.",
    hint2="Midden, linksonder, linksboven, rechtsonder, rechtsboven, midden.",
    aanwijzing="Op de Galmhellingen staan stenen die een lied kennen.",
    beloning=dict(munten=2000, items=[["guhs:vr_item_verbetersteentje", 3]]))

puzzel("z2_kristalspiegels", 2, "licht", ("z2", "kampvuur_splinterkloof_3", -12, 6), [
    "####.#####",
    "#.m...m.T#",
    "#........#",
    "#..m..m..#",
    "#........#",
    "#S.m...m.#",
    "##########",
], {"S": ("lichtbron", RICHTING["O"]), "m": ("spiegel", 0), "T": ("lichtdoel", 0)},
    naam="De Kristalspiegels",
    uitleg="Draai de kristalspiegels tot de lichtstraal het doel rechtsboven raakt.",
    hint1="De straal moet omhoog voordat hij naar rechts kan. Niet elke spiegel doet mee.",
    hint2="Omhoog bij de eerste spiegel, rechts bij de spiegel daarboven, omhoog, en dan helemaal naar rechts.",
    aanwijzing="In de Splinterkloof breekt het licht in kristallen.",
    beloning=dict(munten=2400, bladzijde=5))

puzzel("z2_poollicht", 2, "patroon", ("z2", "kampvuur_knuffelmeer_2", 6, 12), [
    "L.....L",
    ".sssss.",
    ".sssss.",
    ".sssss.",
    ".sssss.",
    ".sssss.",
    "L.K.P.L",
], {"s": ("sterrensteen", 0), "K": ("sterrenkaart", 0), "P": ("probeerknop", 0)}, [
    "..X..",
    ".X.X.",
    "X.X.X",
    ".X.X.",
    "..X..",
], nacht=True,
    naam="Het Poollichtveld",
    uitleg="Alleen 's nachts! Leg het sterrenbeeld van de sterrenkaart na met de sterrenstenen en druk op de knop.",
    hint1="Het sterrenbeeld heet Het Kristal: een ruit met een stip in het midden.",
    hint2="Middelste rij: de 1e, 3e en 5e steen. De hoeken blijven donker.",
    aanwijzing="Bij het Knuffelmeer liggen sterrenstenen die alleen 's nachts wakker worden.",
    beloning=dict(munten=2600, ontgrendel="pz_vleugels_poollicht"))

puzzel("z2_lantaarnsprint", 2, "tijdrit", ("z2", "kampvuur_galmhellingen_2", -12, 8), [
    ".........",
    ".E.3.3.2.",
    ".........",
    ".......2.",
    ".........",
    ".......1.",
    ".........",
    ".A.1.1.1.",
    ".........",
], {"A": ("startlantaarn", 0)}, tijd=25,
    naam="De Lantaarnsprint",
    uitleg="Klik op de lantaarn en spring over de kristalpilaren naar de eindplaat. Je hebt 25 tellen!",
    hint1="Je hoeft niet te rennen: springen van pilaar naar pilaar gaat het snelst.",
    hint2="Rechtdoor, dan bij de hoek omhoog, en bovenaan terug naar links.",
    aanwijzing="Op de Galmhellingen staat een lantaarn met een stopwatch.",
    beloning=dict(munten=2200, ontgrendel="pz_cape_sneeuwvlok"))

puzzel("z2_ijsgrot", 2, "geheim", ("z2", "kampvuur_splinterkloof_1", 10, 8), [
    ".#####.",
    ".#...#.",
    ".#.k.#.",
    ".##X##.",
    ".......",
], dak=[
    ".#####.",
    ".#####.",
    ".#####.",
    ".#####.",
    ".......",
], stil=True, reset=600,
    naam="Het Bevroren Holletje",
    uitleg="Een ijsmuurtje met een barst...",
    hint1="Klik op de barst.", hint2="Klik op de barst.",
    aanwijzing="Vlak bij het westelijke kampvuur van de Splinterkloof zit een barst in het ijs.",
    beloning=dict(munten=1500, bladzijde=6))

puzzel("z2_raadselsteen", 2, "wachtwoord", ("z2", "gehucht_meerkamp", 14, 12), [
    "L...L",
    ".f.f.",
    "..F..",
    ".f.f.",
    "L...L",
], {"F": ("fluistersteen", 0)}, woorden=["ijs", "het ijs", "ice", "ijsje"],
    naam="De Bibberende Steen",
    uitleg="Klik op de steen en fluister het antwoord in de chat.",
    vraag="Ik ben een meer dat stil wil staan. Ik ben glad en koud, en in de zon smelt ik weg. Wat ben ik?",
    vraag_en="I am a lake that wants to stand still. I'm slippery and cold, and in the sun I melt. What am I?",
    hint1="Het Knuffelmeer is er in de winter helemaal van gemaakt.",
    hint2="Drie letters. Je kunt erop schaatsen.",
    hint1_en="The Knuffelmeer is made of it in winter.", hint2_en="Three letters. You can skate on it.",
    aanwijzing="Bij het Meerkampje bibbert een steen van de kou.",
    beloning=dict(munten=1600, items=[["guhs:vr_item_herrolknabbel", 2]]))

# ---- zone 3: De Schemerwortel (lv 30-50) ---------------------------------------------------------------------------------
puzzel("z3_wortelplaten", 3, "volgorde", ("z3", "kampvuur_wortelweg_1", 12, 6), [
    "L.=====.L",
    ".........",
    "..y...b..",
    ".........",
    "..r...g..",
    ".........",
    "L.=====.L",
], {"g": ("drukplaat", 3), "r": ("drukplaat", 0), "b": ("drukplaat", 2), "y": ("drukplaat", 1)}, "ybrg",
    naam="De Wortelplaten",
    uitleg="Stap op de platen in de goede volgorde. De zon wordt wakker, de lucht wordt blauw, de bloesem bloost en het mos groeit gauw.",
    hint1="De zon is geel, de bloesem is roze.",
    hint2="Geel, blauw, roze, groen.",
    aanwijzing="Langs de Wortelweg, in het oosten, liggen vier gekleurde platen.",
    beloning=dict(munten=4500, items=[["guhs:vr_item_verbetersteentje", 4]]))

puzzel("z3_bijenlied", 3, "volgorde", ("z3", "kampvuur_wortelweg_2", 12, 8), [
    "L.......L",
    ".........",
    ".a.b.c.d.",
    ".........",
    "...e.f...",
    ".........",
    "....V....",
    "L.......L",
], {"a": ("klanksteen", 0), "b": ("klanksteen", 2), "c": ("klanksteen", 3), "d": ("klanksteen", 5), "e": ("klanksteen", 6),
    "f": ("klanksteen", 7), "V": ("voorspeelsteen", 0)}, "abfecd",
    naam="Het Bijenlied",
    uitleg="De bijen van de Honingtak neurieden altijd een liedje. Luister naar de voorspeelsteen en neurie mee.",
    hint1="Het lied heeft zes tonen en geen steen zingt twee keer.",
    hint2="Achterste rij de eerste twee, dan voorste rij rechts en links, dan achterste rij de laatste twee.",
    aanwijzing="Onder de Honingtak, langs de Wortelweg, zoemen zingende stenen.",
    beloning=dict(munten=5000, bladzijde=7, ontgrendel="pz_cape_honingraat"))

puzzel("z3_wortelschuif", 3, "schuif", ("z3", "kampvuur_wortelweg_3", -12, 6), [
    "#########",
    "#.......#",
    "#.B...x.#",
    "#...#...#",
    "#.x.B...#",
    "#...#.B.#",
    "#.....x.#",
    "#.R.....#",
    "####.####",
], {"B": ("schuifblok", 0), "x": ("doelvloer", 0), "R": ("herstelknop", 0)},
    naam="De Wortelknoesten",
    uitleg="Duw de wortelknoesten (klik erop) naar de vakjes. Vast? De herstelknop zet alles terug.",
    hint1="Kijk eerst waar je moet STAAN om een knoest de goede kant op te duwen.",
    hint2="Begin met de knoest rechtsonder: één keer omlaag.",
    aanwijzing="Aan de westkant van de Wortelweg liggen knoesten scheef.",
    beloning=dict(munten=5500, items=[["guhs:vr_item_verbetersteentje", 4]]))

puzzel("z3_lantaarnloop", 3, "tijdrit", ("z3", "kampvuur_wortelweg_4", -12, -6), [
    "...........",
    ".E.3.3.3.2.",
    ".........  ",
    ".1.......2.",
    "...........",
    ".1.2.2...1.",
    "...........",
    ".A.......1.",
    "...........",
    ".1.1.1.1.1.",
    "...........",
], {"A": ("startlantaarn", 0)}, tijd=30,
    naam="De Lantaarnloop",
    uitleg="Klik op de lantaarn en ren over de honingpilaren naar de eindplaat. Je hebt 30 tellen!",
    hint1="Begin onderaan en ga tegen de klok in. Sommige pilaren zijn een doodlopend weggetje.",
    hint2="Onderste rij naar rechts, rechterkant omhoog, bovenste rij naar links.",
    aanwijzing="Langs de Wortelweg in het zuidwesten staat een lantaarn tussen honingpilaren.",
    beloning=dict(munten=6000, bladzijde=9, ontgrendel="pz_vleugels_lantaarnmot"))

puzzel("z3_mossymbolen", 3, "symbool", ("z3", "kampvuur_wortelweg_5", 12, -6), [
    "L.......L",
    ".........",
    ".a.b.c.d.",
    ".........",
    "L...P...L",
], {"a": ("symboolsteen", 0), "b": ("symboolsteen", 0), "c": ("symboolsteen", 0), "d": ("symboolsteen", 0),
    "P": ("probeerknop", 0)}, "blad druppel zon blad",
    naam="Het Mosslot",
    uitleg="Draai de symboolstenen en druk op de knop. Het oude mosversje gaat zo: eerst groen, dan nat, dan warm, en weer groen.",
    hint1="Wat is groen aan een boom? Wat is nat? Wat is warm?",
    hint2="Blad, druppel, zon, blad.",
    aanwijzing="Bij het Wortelgewelf staan vier bemoste draaistenen.",
    beloning=dict(munten=5000, ontgrendel="pz_cape_mos"))

puzzel("z3_schorsdeur", 3, "geheim", ("z3", "kampvuur_wortelweg_6", 12, -6), [
    "..bhb..",
    ".#####.",
    ".#.k.#.",
    ".#...#.",
    ".##D##.",
    ".......",
], {"h": ("hendel", 0)}, dak=[
    ".......",
    ".#####.",
    ".#####.",
    ".#####.",
    ".#####.",
    ".......",
], stil=True, reset=600,
    naam="Het Schorshuisje",
    uitleg="Een huisje van schors zonder deurklink...",
    hint1="Kijk eens achter het huisje, tussen de struikjes.", hint2="Achter het huisje zit een hendeltje.",
    aanwijzing="Langs de Wortelweg in het zuidoosten staat een huisje zonder deurklink.",
    beloning=dict(munten=4000, bladzijde=8))

puzzel("z3_honingfluister", 3, "wachtwoord", ("z3", "kampvuur_mosvoet", -6, 16), [
    "L...L",
    ".f.f.",
    "..F..",
    ".f.f.",
    "L...L",
], {"F": ("fluistersteen", 0)}, woorden=["bij", "bijtje", "bijen", "de bij", "een bij", "bee", "honingbij"],
    naam="De Zoemsteen",
    uitleg="Klik op de steen en fluister het antwoord in de chat.",
    vraag="Ik zoem en ik dans, ik maak iets zoets, en ik woon met duizend zusjes op de Honingtak. Wat ben ik?",
    vraag_en="I buzz and I dance, I make something sweet, and I live with a thousand sisters on the Honey Branch. What am I?",
    hint1="Kijk eens naar de korven op de Honingtak.", hint2="Drie letters, en het rijmt op 'wij'.",
    hint1_en="Look at the hives on the Honingtak.", hint2_en="Three letters, it buzzes.",
    aanwijzing="Bij de Mosvoet zoemt een steen.",
    beloning=dict(munten=4000, items=[["guhs:vr_item_herrolknabbel", 3]]))


# ======================================================================================================================
# the lore pages: the legend of the Lege Schaal (WERELDVERHAAL 2.2), in nine torn pieces
# ======================================================================================================================
BLADZIJDES = {
    1: ("Een gewone kleine Mika",
        "Lang geleden woonde aan de rand van de wereld een gewone, kleine Mika. Niet sterker dan de rest. Niet slimmer. Alleen... hongeriger."),
    2: ("De eerste kaasknabbel",
        "Hij stal zijn eerste kaasknabbel van een guh. Toen hij hem opat, voelde hij één tel iets warms in zijn buik. Vads! En toen was het weer weg."),
    3: ("Wat elke guh weet",
        "Dus pakte hij er nog een. En nog een. Wat hij niet begreep, en wat elke guh weet: vads groeit alleen als je hem deelt. Wat je pakt, smelt in je pootjes."),
    4: ("Lange armen",
        "Hij moest steeds meer pakken en steeds verder reiken. Eerst zijn eigen land, toen dat van de buren, eeuw na eeuw. Daarom zeggen Mika's: 'De Keizer heeft lange armen.'"),
    5: ("Een gestolen kroontje",
        "Zijn eigen land haalde hij helemaal leeg: grauw, mager en stil. Toen zette hij een gestolen kroontje op. Het was te klein. Hij noemde zichzelf toch Keizer."),
    6: ("De Lege Schaal",
        "Hij bouwde een burcht om één ding heen: de Lege Schaal, een kom zo groot als een meer. Alle vads van de wereld moet erin. Hij is nog nooit vol geweest."),
    7: ("Nooitgenoeg",
        "Daarom heet zijn burcht Nooitgenoeg. Grauwe Vadsleidingen lopen er als rivieren naartoe, vanuit elk land dat hij al vast heeft."),
    8: ("Het Oog",
        "De tweede maan is geen maan. Het is een grauwe lantaarn die de Keizer boven het westen heeft gehangen, om te zien waar nog vads is. Mika's noemen hem het Oog."),
    9: ("Een eenzaam katje",
        "Mika's maken elkaar 's nachts bang: 'Als je je knabbel deelt, komt de Keizer je halen.' Maar in de kantlijn heeft iemand iets geschreven: 'Misschien heeft hij nooit een koekje gekregen.'"),
}

# the cosmetic unlock ids (t-kleding / the wardrobe picks these up through Puzzels.ontgrendeld)
ONTGRENDEL = {
    "pz_vleugels_sterrenstof": ("Sterrenstofvleugels", "Stardust Wings"),
    "pz_vleugels_poollicht": ("Poollichtvleugels", "Aurora Wings"),
    "pz_vleugels_lantaarnmot": ("Lantaarnmotvleugels", "Lantern Moth Wings"),
    "pz_cape_sneeuwvlok": ("Sneeuwvlokcape", "Snowflake Cape"),
    "pz_cape_honingraat": ("Honingraatcape", "Honeycomb Cape"),
    "pz_cape_mos": ("Moscape", "Moss Cape"),
}

SOORTNAAM = {
    "volgorde": ("Volgordepuzzel", "Sequence puzzle"), "licht": ("Lichtpuzzel", "Light puzzle"), "geheim": ("Geheimpje", "Secret"),
    "wachtwoord": ("Raadsel", "Riddle"), "patroon": ("Sterrenpuzzel", "Star puzzle"), "symbool": ("Symboolslot", "Symbol lock"),
    "schuif": ("Schuifpuzzel", "Push puzzle"), "tijdrit": ("Tijdrit", "Time trial"),
}


# ======================================================================================================================
# building the json
# ======================================================================================================================
def _zet(blokken, x, y, z, b):
    blokken[(x, y, z)] = b


def bouw(p):
    """The json of one puzzle (local coordinates: x = column, z = row, y 0 = standing level)."""
    kaart = p["kaart"]
    d = len(kaart)
    w = max(len(r) for r in kaart)
    pal = PALET[p["zone"]]
    blokken = {}
    vloer = {}
    elementen = []
    deur = []
    schuif = []
    hoogte = 2
    for z, rij in enumerate(kaart):
        for x in range(w):
            c = rij[x] if x < len(rij) else "."
            vloer[(x, z)] = pal["vloer"]
            if c in ". ":
                continue
            if c in p["legenda"]:   # (the puzzle's own legend wins over the shared one)
                soort, stand = p["legenda"][c]
                y = -1 if soort in VLOERTEGELS else 0
                elementen.append(dict(x=x, y=y, z=z, blok=soort, stand=stand, c=c))
                if soort == "schuifblok":
                    schuif.append((x, z))
            elif c == "#":
                _zet(blokken, x, 0, z, pal["muur"])
                _zet(blokken, x, 1, z, pal["muur"])
            elif c == "=":
                _zet(blokken, x, 0, z, pal["muur"])
            elif c == "L":
                _zet(blokken, x, 0, z, pal["lamp"])
            elif c == "b":
                _zet(blokken, x, 0, z, pal["struik"])
            elif c == "f":
                vloer[(x, z)] = BLOEMGROND[p["zone"]]
                _zet(blokken, x, 0, z, pal["bloem"])
            elif c == "k":
                _zet(blokken, x, 0, z, "minecraft:amethyst_cluster")
            elif c in "123":
                for y in range(int(c)):
                    _zet(blokken, x, y, z, pal["platform"])
                hoogte = max(hoogte, int(c) + 2)
            elif c == "E":
                for y in range(3):
                    _zet(blokken, x, y, z, pal["platform"])
                elementen.append(dict(x=x, y=3, z=z, blok="eindplaat", stand=0, c="E"))
                hoogte = max(hoogte, 5)
            elif c == "D":
                deur += [[x, 0, z], [x, 1, z]]
            elif c == "X":
                elementen.append(dict(x=x, y=0, z=z, blok="scheur", stand=0, c="X"))
                elementen.append(dict(x=x, y=1, z=z, blok="scheur", stand=0, c="X"))
            else:
                raise ValueError(f"{p['id']}: unknown char {c!r}")
    if p.get("dak"):
        for z, rij in enumerate(p["dak"]):
            for x, c in enumerate(rij):
                if c == "#":
                    _zet(blokken, x, 2, z, pal["dak"])
        hoogte = max(hoogte, 3)
    uit = dict(id=p["id"], zone=p["zone"], soort=p["soort"],
               anker=dict(bron=p["anker"][0], plek=p["anker"][1], dx=p["anker"][2], dz=p["anker"][3]),
               maat=[w, d], hoogte=hoogte + 2, vloer=pal["vloer"], vul=pal["vul"],
               blokken=[[x, y, z, b] for (x, y, z), b in sorted(blokken.items())],
               vloerAnders=[[x, z, b] for (x, z), b in sorted(vloer.items()) if b != pal["vloer"]],
               elementen=[{k: v for k, v in e.items() if k != "c"} for e in elementen],
               deur=deur, reset=p.get("reset", 400), hintNa=p.get("hintNa", 3), nacht=bool(p.get("nacht")), stil=bool(p.get("stil")),
               beloning=p["beloning"])
    soort = p["soort"]
    if soort == "volgorde":
        idx = {}
        for i, e in enumerate(elementen):
            idx.setdefault(e["c"], i)
        uit["oplossing"] = [idx[c] for c in p["oplossing"]]
        uit["demo"] = any(e["blok"] == "voorspeelsteen" for e in elementen)
    elif soort == "symbool":
        doel = [SYMBOLEN.index(s) for s in p["oplossing"].split()]
        stenen = [i for i, e in enumerate(elementen) if e["blok"] == "symboolsteen"]
        assert len(doel) == len(stenen), p["id"]
        rnd = random.Random(p["id"])
        for i, s in zip(stenen, doel):
            elementen[i]["stand"] = (s + rnd.randint(1, 5)) % 6
            uit["elementen"][i]["stand"] = elementen[i]["stand"]
        uit["oplossing"] = doel
    elif soort == "patroon":
        sterren = [i for i, e in enumerate(elementen) if e["blok"] == "sterrensteen"]
        x0 = min(elementen[i]["x"] for i in sterren)
        z0 = min(elementen[i]["z"] for i in sterren)
        aan = []
        for i in sterren:
            e = elementen[i]
            if p["oplossing"][e["z"] - z0][e["x"] - x0] == "X":
                aan.append(i)
        uit["oplossing"] = aan
        uit["kaart"] = p["oplossing"]
    elif soort == "licht":
        uit["oplossing"] = los_licht(p, uit)
    elif soort == "schuif":
        uit["oplossing"] = los_schuif(p, uit)
    elif soort == "tijdrit":
        uit["tijd"] = p["tijd"]
        assert any(e["blok"] == "eindplaat" for e in elementen) and any(e["blok"] == "startlantaarn" for e in elementen), p["id"]
    elif soort == "wachtwoord":
        uit["woorden"] = p["woorden"]
    elif soort == "geheim":
        assert any(e["blok"] in ("scheur", "hendel") for e in elementen), p["id"]
    return uit


def _vast(uit, x, z):
    """Is (x, z) blocked at y 0 (walls, pillars, decoration, elements standing on the floor)?"""
    w, d = uit["maat"]
    if not (0 <= x < w and 0 <= z < d):
        return True
    for bx, by, bz, _ in uit["blokken"]:
        if bx == x and bz == z and by == 0:
            return True
    for e in uit["elementen"]:
        if e["x"] == x and e["z"] == z and e["y"] == 0:
            return True
    return False


def straal(uit, standen):
    """Trace the beam (same rules as Java Puzzels.straal): returns (hits the target, path)."""
    el = uit["elementen"]
    bron = next(e for e in el if e["blok"] == "lichtbron")
    spiegels = {(e["x"], e["z"]): i for i, e in enumerate(el) if e["blok"] == "spiegel"}
    doel = next(e for e in el if e["blok"] == "lichtdoel")
    x, z, r = bron["x"], bron["z"], bron["stand"]
    pad = []
    for _ in range(64):
        x += DXZ[r][0]
        z += DXZ[r][1]
        if (x, z) == (doel["x"], doel["z"]):
            return True, pad
        if (x, z) in spiegels:
            s = standen[spiegels[(x, z)]]
            r = spiegel(r, s)
            pad.append((x, z))
            continue
        if _vast(uit, x, z):
            return False, pad
        pad.append((x, z))
    return False, pad


def spiegel(r, s):
    """Stand 0 = '/' , 1 = '\\' (seen from above, north up). N0 O1 Z2 W3."""
    if s == 0:
        return {1: 0, 0: 1, 3: 2, 2: 3}[r]
    return {1: 2, 2: 1, 3: 0, 0: 3}[r]


def los_licht(p, uit):
    el = uit["elementen"]
    sp = [i for i, e in enumerate(el) if e["blok"] == "spiegel"]
    oplossingen = []
    for combo in itertools.product((0, 1), repeat=len(sp)):
        st = {i: s for i, s in zip(sp, combo)}
        if straal(uit, st)[0]:
            oplossingen.append(st)
    assert oplossingen, f"{p['id']}: light puzzle has no solution"
    beste = oplossingen[0]
    # the start state: every mirror the "wrong" way round of the first solution, unless that is also a solution
    start = {i: 1 - s for i, s in beste.items()}
    assert not straal(uit, start)[0], f"{p['id']}: start state already solved"
    for i in sp:
        el[i]["stand"] = start[i]
    return [beste[i] for i in sp]


def los_schuif(p, uit):
    el = uit["elementen"]
    blokken = tuple(sorted((e["x"], e["z"]) for e in el if e["blok"] == "schuifblok"))
    doelen = frozenset((e["x"], e["z"]) for e in el if e["blok"] == "doelvloer")
    assert len(blokken) == len(doelen), p["id"]
    w, d = uit["maat"]
    muur = set()
    for bx, by, bz, _ in uit["blokken"]:
        if by == 0:
            muur.add((bx, bz))
    for e in el:
        if e["y"] == 0 and e["blok"] != "schuifblok":
            muur.add((e["x"], e["z"]))

    def vrij(x, z, bl):
        return 0 <= x < w and 0 <= z < d and (x, z) not in muur and (x, z) not in bl

    def bereik(start, bl):
        """Every cell the player can walk to (the entrance gap counts as reachable from outside: start = outside cells)."""
        gezien = set(start)
        q = deque(start)
        while q:
            x, z = q.popleft()
            for dx, dz in DXZ:
                n = (x + dx, z + dz)
                if n not in gezien and vrij(*n, bl):
                    gezien.add(n)
                    q.append(n)
        return gezien

    # the player comes in from any open edge cell
    rand = [(x, z) for x in range(w) for z in range(d) if (x in (0, w - 1) or z in (0, d - 1)) and (x, z) not in muur]
    begin = (blokken, )
    gezien = {blokken: None}
    q = deque([blokken])
    while q:
        bl = q.popleft()
        if set(bl) == doelen:
            zetten = []
            while gezien[bl] is not None:
                vorige, zet = gezien[bl]
                zetten.append(zet)
                bl = vorige
            return list(reversed(zetten))
        blset = set(bl)
        speler = bereik([c for c in rand if c not in blset], blset)
        for i, (bx, bz) in enumerate(bl):
            for r, (dx, dz) in enumerate(DXZ):
                staan = (bx - dx, bz - dz)
                doel = (bx + dx, bz + dz)
                if staan in speler and vrij(*doel, blset):
                    nieuw = tuple(sorted(bl[:i] + ((doel[0], doel[1]),) + bl[i + 1:]))
                    if nieuw not in gezien:
                        gezien[nieuw] = (bl, [bx, bz, r])
                        q.append(nieuw)
    raise AssertionError(f"{p['id']}: push puzzle has no solution")


# ======================================================================================================================
# art: 16x16 block textures + page icon
# ======================================================================================================================
def _ruis(basis, var=14, zaad=1):
    d = Doek()
    rnd = np.random.default_rng(zaad)
    for y in range(16):
        for x in range(16):
            f = 1 + (rnd.random() - 0.5) * var / 100
            d.px(x, y, _licht(basis, f))
    return d


def _rand(d, c):
    d.rect(0, 0, 15, 0, c)
    d.rect(0, 15, 15, 15, c)
    d.rect(0, 0, 0, 15, c)
    d.rect(15, 0, 15, 15, c)


STEEN = (150, 140, 160)
ROZE = (255, 140, 190)
KLEURRGB = [(255, 140, 190), (255, 220, 90), (110, 170, 255), (110, 210, 120)]


def tex_plaat(kleur, aan):
    d = _ruis(_licht(kleur, 1.15 if aan else 0.8), 10, 3)
    _rand(d, _licht(kleur, 0.55))
    if aan:
        d.disc(7.5, 7.5, 3, (255, 255, 235))
    else:
        d.disc(7.5, 7.5, 2, _licht(kleur, 1.05))
    return d


def tex_steen(aan, merk=None, zaad=5):
    d = _ruis((170, 150, 180) if not aan else (230, 190, 230), 16, zaad)
    _rand(d, (90, 80, 100))
    if merk:
        merk(d, (255, 255, 255) if aan else (120, 90, 140))
    return d


def noot(d, c):
    d.rect(9, 3, 10, 11, c)
    d.disc(7, 11, 2.2, c)
    d.rect(10, 3, 12, 4, c)


def oor(d, c):
    d.disc(7.5, 7.5, 4, c)
    d.disc(7.5, 7.5, 2, (90, 80, 100))
    d.rect(6, 1, 9, 2, c)


def symbool(d, naam, c):
    if naam == "ster":
        d.poly([(8, 2), (9.6, 6.4), (14, 6.6), (10.5, 9.3), (11.8, 14), (8, 11.2), (4.2, 14), (5.5, 9.3), (2, 6.6), (6.4, 6.4)], c)
    elif naam == "hart":
        d.disc(5.5, 6, 2.6, c)
        d.disc(10.5, 6, 2.6, c)
        d.poly([(3, 7), (13, 7), (8, 13.5)], c)
    elif naam == "maan":
        d.disc(8, 8, 5, c)
        d.disc(10.5, 6.5, 4.2, (170, 150, 180))
    elif naam == "zon":
        d.disc(8, 8, 3.2, c)
        for dx, dy in ((0, -5), (0, 5), (-5, 0), (5, 0), (-4, -4), (4, 4), (-4, 4), (4, -4)):
            d.px(8 + dx, 8 + dy, c)
    elif naam == "druppel":
        d.disc(8, 10, 3.5, c)
        d.poly([(8, 2), (11, 9), (5, 9)], c)
    elif naam == "blad":
        d.poly([(3, 13), (6, 5), (13, 3), (11, 10)], c)
        d.lijn(3, 13, 12, 4, _licht(c, 0.6))


def tex_symbool(naam):
    d = _ruis((170, 150, 180), 14, 7)
    _rand(d, (90, 80, 100))
    symbool(d, naam, (255, 120, 180))
    return d


def tex_spiegel(s):
    d = _ruis((120, 110, 140), 10, 9)
    _rand(d, (70, 60, 80))
    for i in range(2, 14):
        x = i if s == 1 else 15 - i
        d.px(x, i, (220, 240, 255))
        d.px(x, i + 1 if i < 14 else i, (170, 200, 255))
    return d


def tex_bron(front):
    d = _ruis((140, 120, 150), 10, 11)
    _rand(d, (80, 70, 90))
    if front:
        d.disc(7.5, 7.5, 4, (255, 240, 170))
        d.disc(7.5, 7.5, 2, (255, 255, 255))
    return d


def tex_doel(aan):
    d = _ruis((140, 120, 150), 10, 13)
    for r, c in ((6, (255, 255, 255) if aan else (200, 190, 210)), (4.5, ROZE if aan else (150, 110, 140)), (2.5, (255, 255, 200) if aan else (120, 100, 120))):
        d.disc(7.5, 7.5, r, c)
    return d


def tex_schuif():
    d = _ruis((190, 225, 255), 10, 15)
    _rand(d, (120, 150, 200))
    d.rect(3, 3, 12, 12, (220, 240, 255))
    d.lijn(4, 7, 11, 7, (150, 180, 230))
    d.lijn(7, 4, 7, 11, (150, 180, 230))
    return d


def tex_doelvloer(aan):
    d = _ruis(ROZE if aan else (220, 160, 200), 8, 17)
    _rand(d, (180, 90, 140))
    d.rect(4, 4, 11, 11, (255, 200, 230) if aan else (240, 180, 215))
    return d


def tex_knop(kleur):
    d = _ruis(kleur, 10, 19)
    _rand(d, _licht(kleur, 0.5))
    d.disc(7.5, 7.5, 3, _licht(kleur, 1.3))
    return d


def tex_scheur():
    d = _ruis((120, 130, 110), 18, 21)
    for x, y in ((7, 1), (8, 2), (7, 3), (8, 4), (9, 5), (8, 6), (7, 7), (6, 8), (7, 9), (8, 10), (9, 11), (8, 12), (8, 13), (7, 14)):
        d.px(x, y, (40, 40, 40))
    d.px(10, 5, (255, 220, 255))
    return d


def tex_deur():
    d = _ruis((120, 130, 110), 18, 23)
    for y in (3, 7, 11):
        d.rect(0, y, 15, y, (90, 95, 85))
    d.px(12, 9, (255, 220, 255))
    return d


def tex_fluister():
    d = _ruis((160, 160, 175), 14, 25)
    _rand(d, (90, 90, 105))
    d.px(5, 6, (30, 20, 40))
    d.px(10, 6, (30, 20, 40))
    d.px(4, 8, ROZE)
    d.px(11, 8, ROZE)
    d.rect(7, 9, 8, 10, (30, 20, 40))
    return d


def tex_sterren(aan):
    d = _ruis((40, 40, 90) if not aan else (70, 60, 140), 10, 27)
    _rand(d, (25, 25, 60))
    c = (255, 250, 200) if aan else (120, 120, 170)
    d.poly([(8, 3), (9.2, 6.8), (13, 7), (10, 9.3), (11, 13), (8, 10.8), (5, 13), (6, 9.3), (3, 7), (6.8, 6.8)], c)
    return d


def tex_kaart():
    d = _ruis((200, 170, 120), 10, 29)
    _rand(d, (120, 90, 60))
    for x, y in ((4, 4), (11, 5), (7, 8), (4, 11), (11, 11)):
        d.px(x, y, (60, 40, 100))
    d.lijn(4, 4, 7, 8, (150, 120, 90))
    d.lijn(11, 5, 7, 8, (150, 120, 90))
    return d


def tex_voorspeel():
    d = tex_steen(False, noot, 31)
    d.rect(2, 2, 3, 3, (255, 220, 90))
    d.rect(12, 12, 13, 13, (255, 220, 90))
    return d


def tex_hendel(aan):
    d = _ruis((110, 80, 60), 10, 33)
    return d


def tex_bladzijde():
    d = Doek()
    d.poly([(3, 2), (12, 2), (13, 6), (11, 8), (13, 10), (12, 14), (3, 14)], (240, 225, 190))
    for y in (5, 7, 9, 11):
        d.lijn(5, y, 10, y, (150, 130, 110))
    d.px(9, 3, (200, 80, 120))
    d.schaduw()
    return d.rand()


# ======================================================================================================================
# blockstates + models
# ======================================================================================================================
def _model_cube(naam, tex):
    return naam, {"parent": "minecraft:block/cube_all", "textures": {"all": f"guhs:block/{BTEX}/{tex}"}}


def _model_plaat(naam, tex, hoogte=1):
    t = f"guhs:block/{BTEX}/{tex}"
    return naam, {"parent": "minecraft:block/block", "textures": {"particle": t, "t": t}, "elements": [{
        "from": [1, 0, 1], "to": [15, hoogte, 15],
        "faces": {f: {"texture": "#t", **({"cullface": "down"} if f == "down" else {})}
                  for f in ("down", "up", "north", "south", "west", "east")}}]}


def _model_klein(naam, tex, x0=4, x1=12, h=6):
    t = f"guhs:block/{BTEX}/{tex}"
    return naam, {"parent": "minecraft:block/block", "textures": {"particle": t, "t": t}, "elements": [{
        "from": [x0, 0, x0], "to": [x1, h, x1],
        "faces": {f: {"texture": "#t"} for f in ("down", "up", "north", "south", "west", "east")}}]}


def _model_hendel(naam, aan):
    voet = f"guhs:block/{BTEX}/hendel_voet"
    stok = "minecraft:block/oak_planks"
    knop = f"guhs:block/{BTEX}/knop_roze"
    hoek = 30 if aan else -30
    return naam, {"parent": "minecraft:block/block", "textures": {"particle": voet, "voet": voet, "stok": stok, "knop": knop}, "elements": [
        {"from": [4, 0, 4], "to": [12, 2, 12], "faces": {f: {"texture": "#voet"} for f in ("down", "up", "north", "south", "west", "east")}},
        {"from": [7, 1, 7], "to": [9, 10, 9], "rotation": {"angle": 22.5 if aan else -22.5, "axis": "x", "origin": [8, 1, 8]},
         "faces": {f: {"texture": "#stok"} for f in ("down", "up", "north", "south", "west", "east")}},
        {"from": [6.5, 9, 6.5], "to": [9.5, 12, 9.5], "rotation": {"angle": 22.5 if aan else -22.5, "axis": "x", "origin": [8, 1, 8]},
         "faces": {f: {"texture": "#knop"} for f in ("down", "up", "north", "south", "west", "east")}}]}


def modellen():
    """{model name: json}, {block: blockstate json}, {texture name: Doek}."""
    tex, mod, bs = {}, {}, {}
    B = "guhs:block/vr_pz_"

    def m(pair):
        mod[pair[0]] = pair[1]

    def var(blok, fn):
        v = {}
        for aan in (False, True):
            for stand in range(8):
                v[f"aan={'true' if aan else 'false'},stand={stand}"] = fn(aan, stand)
        bs[blok] = {"variants": v}

    # drukplaat: colour = stand
    for i, k in enumerate(KLEUREN):
        for aan in (0, 1):
            tex[f"plaat_{k}_{aan}"] = tex_plaat(KLEURRGB[i], aan)
            m(_model_plaat(f"vr_pz_drukplaat_{k}_{aan}", f"plaat_{k}_{aan}"))
    var("vr_pz_drukplaat", lambda aan, s: {"model": f"{B}drukplaat_{KLEUREN[s % 4]}_{int(aan)}"})
    # klanksteen
    for aan in (0, 1):
        tex[f"klank_{aan}"] = tex_steen(aan, noot, 41)
        m(_model_cube(f"vr_pz_klanksteen_{aan}", f"klank_{aan}"))
    var("vr_pz_klanksteen", lambda aan, s: {"model": f"{B}klanksteen_{int(aan)}"})
    tex["voorspeel"] = tex_voorspeel()
    m(_model_cube("vr_pz_voorspeelsteen", "voorspeel"))
    var("vr_pz_voorspeelsteen", lambda aan, s: {"model": f"{B}voorspeelsteen"})
    # symboolsteen
    for i, s in enumerate(SYMBOLEN):
        tex[f"symbool_{s}"] = tex_symbool(s)
        m(_model_cube(f"vr_pz_symboolsteen_{s}", f"symbool_{s}"))
    var("vr_pz_symboolsteen", lambda aan, s: {"model": f"{B}symboolsteen_{SYMBOLEN[s % 6]}"})
    # knoppen
    tex["knop_roze"] = tex_knop((255, 140, 190))
    tex["knop_geel"] = tex_knop((255, 210, 90))
    m(_model_klein("vr_pz_probeerknop", "knop_roze", 4, 12, 4))
    m(_model_klein("vr_pz_herstelknop", "knop_geel", 4, 12, 4))
    var("vr_pz_probeerknop", lambda aan, s: {"model": f"{B}probeerknop"})
    var("vr_pz_herstelknop", lambda aan, s: {"model": f"{B}herstelknop"})
    # spiegel
    for s in (0, 1):
        tex[f"spiegel_{s}"] = tex_spiegel(s)
        mod[f"vr_pz_spiegel_{s}"] = {"parent": "minecraft:block/cube_column",
                                     "textures": {"end": f"guhs:block/{BTEX}/spiegel_{s}", "side": f"guhs:block/{BTEX}/spiegel_zij"}}
    tex["spiegel_zij"] = _ruis((120, 110, 140), 10, 43)
    var("vr_pz_spiegel", lambda aan, s: {"model": f"{B}spiegel_{s % 2}"})
    # lichtbron (front = the stand direction; orientable model faces north at y 0)
    tex["bron_voor"] = tex_bron(True)
    tex["bron_zij"] = tex_bron(False)
    mod["vr_pz_lichtbron"] = {"parent": "minecraft:block/orientable", "textures": {
        "front": f"guhs:block/{BTEX}/bron_voor", "side": f"guhs:block/{BTEX}/bron_zij", "top": f"guhs:block/{BTEX}/bron_zij"}}
    var("vr_pz_lichtbron", lambda aan, s: {"model": f"{B}lichtbron", **({"y": 90 * (s % 4)} if s % 4 else {})})
    for aan in (0, 1):
        tex[f"doel_{aan}"] = tex_doel(aan)
        m(_model_cube(f"vr_pz_lichtdoel_{aan}", f"doel_{aan}"))
        tex[f"doelvloer_{aan}"] = tex_doelvloer(aan)
        m(_model_cube(f"vr_pz_doelvloer_{aan}", f"doelvloer_{aan}"))
        tex[f"sterren_{aan}"] = tex_sterren(aan)
        m(_model_cube(f"vr_pz_sterrensteen_{aan}", f"sterren_{aan}"))
    var("vr_pz_lichtdoel", lambda aan, s: {"model": f"{B}lichtdoel_{int(aan)}"})
    var("vr_pz_doelvloer", lambda aan, s: {"model": f"{B}doelvloer_{int(aan)}"})
    var("vr_pz_sterrensteen", lambda aan, s: {"model": f"{B}sterrensteen_{int(aan)}"})
    tex["schuif"] = tex_schuif()
    m(_model_cube("vr_pz_schuifblok", "schuif"))
    var("vr_pz_schuifblok", lambda aan, s: {"model": f"{B}schuifblok"})
    # lanterns: the vanilla lantern template (soul = waiting, gold = running)
    mod["vr_pz_startlantaarn_0"] = {"parent": "minecraft:block/template_lantern", "textures": {"lantern": "minecraft:block/soul_lantern"}}
    mod["vr_pz_startlantaarn_1"] = {"parent": "minecraft:block/template_lantern", "textures": {"lantern": "minecraft:block/lantern"}}
    var("vr_pz_startlantaarn", lambda aan, s: {"model": f"{B}startlantaarn_{int(aan)}"})
    for aan in (0, 1):
        tex[f"eind_{aan}"] = tex_plaat((255, 210, 90), aan)
        m(_model_plaat(f"vr_pz_eindplaat_{aan}", f"eind_{aan}"))
    var("vr_pz_eindplaat", lambda aan, s: {"model": f"{B}eindplaat_{int(aan)}"})
    tex["scheur"] = tex_scheur()
    m(_model_cube("vr_pz_scheur", "scheur"))
    var("vr_pz_scheur", lambda aan, s: {"model": f"{B}scheur"})
    tex["deur"] = tex_deur()
    m(_model_cube("vr_pz_geheimdeur", "deur"))
    var("vr_pz_geheimdeur", lambda aan, s: {"model": f"{B}geheimdeur"})
    tex["hendel_voet"] = tex_hendel(False)
    for aan in (0, 1):
        m(_model_hendel(f"vr_pz_hendel_{aan}", aan))
    var("vr_pz_hendel", lambda aan, s: {"model": f"{B}hendel_{int(aan)}"})
    tex["fluister"] = tex_fluister()
    tex["fluister_zij"] = _ruis((160, 160, 175), 14, 45)
    mod["vr_pz_fluistersteen"] = {"parent": "minecraft:block/orientable", "textures": {
        "front": f"guhs:block/{BTEX}/fluister", "side": f"guhs:block/{BTEX}/fluister_zij", "top": f"guhs:block/{BTEX}/fluister_zij"}}
    var("vr_pz_fluistersteen", lambda aan, s: {"model": f"{B}fluistersteen", **({"y": 90 * (s % 4)} if s % 4 else {})})
    tex["kaart"] = tex_kaart()
    mod["vr_pz_sterrenkaart"] = {"parent": "minecraft:block/cube_top", "textures": {
        "top": f"guhs:block/{BTEX}/kaart", "side": "minecraft:block/spruce_planks"}}
    var("vr_pz_sterrenkaart", lambda aan, s: {"model": f"{B}sterrenkaart"})
    return mod, bs, tex


def _assets():
    mod, bs, tex = modellen()
    for naam, j in mod.items():
        _schrijf(os.path.join(A, "models", "block", naam + ".json"), j)
    for blok, j in bs.items():
        _schrijf(os.path.join(A, "blockstates", blok + ".json"), j)
    for naam, d in tex.items():
        d.png(os.path.join(A, "textures", "block", BTEX, naam + ".png"))
    assert set(bs) == {"vr_pz_" + b for b in BLOKKEN}, set(bs) ^ {"vr_pz_" + b for b in BLOKKEN}
    tex_bladzijde().png(os.path.join(A, "textures", "item", ITEX, "bladzijde.png"))
    for n in BLADZIJDES:
        _schrijf(os.path.join(A, "items", f"vr_pz_bladzijde_{n}.json"), {"model": {"type": "minecraft:model", "model": "guhs:item/vr_pz_bladzijde"}})
    _schrijf(os.path.join(A, "models", "item", "vr_pz_bladzijde.json"),
             {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{ITEX}/bladzijde"}})


def _data():
    os.makedirs(DATA_DIR, exist_ok=True)
    for f in os.listdir(DATA_DIR):
        if f.endswith(".json"):
            os.remove(os.path.join(DATA_DIR, f))
    uit = []
    for p in PUZZELS:
        j = bouw(p)
        _schrijf(os.path.join(DATA_DIR, p["id"] + ".json"), j)
        uit.append(j)
    per = {}
    for p in PUZZELS:
        per[p["zone"]] = per.get(p["zone"], 0) + 1
    assert all(per.get(z, 0) >= 5 for z in (1, 2, 3)), per
    pages = sorted(p["beloning"]["bladzijde"] for p in PUZZELS if "bladzijde" in p["beloning"])
    assert pages == sorted(BLADZIJDES), pages
    return uit


# ======================================================================================================================
# texts
# ======================================================================================================================
BLOKNAAM = {
    "drukplaat": ("Puzzelplaat", "Puzzle Plate"), "klanksteen": ("Zingende steen", "Singing Stone"),
    "voorspeelsteen": ("Voorspeelsteen", "Demo Stone"), "symboolsteen": ("Symboolsteen", "Symbol Stone"),
    "probeerknop": ("Probeerknop", "Try Button"), "herstelknop": ("Herstelknop", "Reset Button"), "spiegel": ("Draaispiegel", "Turning Mirror"),
    "lichtbron": ("Lichtsteen", "Light Stone"), "lichtdoel": ("Lichtdoel", "Light Target"), "schuifblok": ("Schuifblok", "Push Block"),
    "doelvloer": ("Doelvakje", "Target Tile"), "startlantaarn": ("Startlantaarn", "Start Lantern"), "eindplaat": ("Eindplaat", "Finish Plate"),
    "scheur": ("Gebarsten muur", "Cracked Wall"), "geheimdeur": ("Geheime deur", "Secret Door"), "hendel": ("Verstopt hendeltje", "Hidden Lever"),
    "fluistersteen": ("Fluistersteen", "Whisper Stone"), "sterrensteen": ("Sterrensteen", "Star Stone"), "sterrenkaart": ("Sterrenkaart", "Star Map"),
}

ALGEMEEN = {
    # key: (nl, en)
    "opgelost": ("✔ %s opgelost!", "✔ %s solved!"),
    "geheim_gevonden": ("✦ Geheimpje gevonden: %s!", "✦ Secret found: %s!"),
    "al_opgelost": ("Dit heb je al eens opgelost. Lekker puzzelen mag altijd!", "You solved this before. Puzzling is always allowed!"),
    "rust": ("Opgelost! Het puzzeltje zet zichzelf zo terug.", "Solved! The puzzle resets itself in a moment."),
    "fout": ("Hmm, dat klopt niet. Alles gaat terug naar het begin.", "Hmm, that's not right. Everything goes back to the start."),
    "tip": ("Tip: %s", "Tip: %s"),
    "beloning.munten": ("+ %s", "+ %s"),
    "beloning.bladzijde": ("+ een gescheurde bladzijde: \"%s\"", "+ a torn page: \"%s\""),
    "beloning.ontgrendel": ("+ ontgrendeld voor je kledingkast: %s", "+ unlocked for your wardrobe: %s"),
    "beloning.item": ("+ %sx %s", "+ %sx %s"),
    "nacht": ("De sterrenstenen slapen nog. Kom terug als het donker is!", "The star stones are still asleep. Come back when it's dark!"),
    "tijd.start": ("Rennen! Je hebt %s tellen.", "Run! You have %s seconds."),
    "tijd.over": ("⏱ Nog %s", "⏱ %s left"),
    "tijd.op": ("De tijd is om! Klik de lantaarn om opnieuw te beginnen.", "Time's up! Click the lantern to try again."),
    "tijd.niet_bezig": ("Klik eerst op de startlantaarn!", "Click the start lantern first!"),
    "luister": ("De steen luistert... fluister je antwoord in de chat.", "The stone is listening... whisper your answer in chat."),
    "woord.fout": ("De steen giechelt: \"Nee hoor!\"", "The stone giggles: \"Nope!\""),
    "herstel": ("Alles staat weer op zijn plek.", "Everything is back in place."),
    "duw.vast": ("Die kant op kan het blok niet.", "The block can't go that way."),
    "demo": ("Luister goed...", "Listen carefully..."),
    "kaart.kop": ("Sterrenkaart: %s", "Star map: %s"),
    "symbool": ("Symbool: %s", "Symbol: %s"),
    "boek.cat": ("Puzzels en geheimpjes", "Puzzles and secrets"),
    "boek.hint": ("%s", "%s"),
    "bladzijde.kop": ("Gescheurde bladzijde %s/9: %s", "Torn page %s/9: %s"),
    "bladzijde.uitleg": ("Klik met rechts om te lezen. Er zijn er negen.", "Right-click to read. There are nine."),
    "tip.eerste": ("Een puzzel! Onderaan je scherm staat steeds wat je moet doen. Lukt het niet? Na een paar pogingen krijg je een tip.",
                   "A puzzle! The bottom of your screen always says what to do. Stuck? After a few tries you get a tip."),
    "soortkop": ("%s: %s", "%s: %s"),
    "tip.eerste.titel": ("Een puzzel!", "A puzzle!"),
}

SYMBOOLNAAM = {"ster": ("ster", "star"), "hart": ("hart", "heart"), "maan": ("maan", "moon"), "zon": ("zon", "sun"),
               "druppel": ("druppel", "drop"), "blad": ("blad", "leaf")}


def teksten():
    nl, en = {}, {}

    def t(k, n, e=None):
        nl[k] = n
        en[k] = e if e is not None else n

    for k, (n, e) in ALGEMEEN.items():
        t(P + k, n, e)
    for b, (n, e) in BLOKNAAM.items():
        t(f"block.guhs.vr_pz_{b}", n, e)
    for s, (n, e) in SYMBOOLNAAM.items():
        t(P + "symbool." + s, n, e)
    for s, (n, e) in SOORTNAAM.items():
        t(P + "soort." + s, n, e)
    for i, (n, e) in ONTGRENDEL.items():
        t(P + "ontgrendel." + i, n, e)
    for nr, (kop, tekst) in BLADZIJDES.items():
        t(f"item.guhs.vr_pz_bladzijde_{nr}", f"Gescheurde bladzijde ({nr}/9)", f"Torn Page ({nr}/9)")
        t(P + f"bladzijde.{nr}.titel", kop)
        t(P + f"bladzijde.{nr}.tekst", tekst)
    for p in PUZZELS:
        k = P + "p." + p["id"] + "."
        t(k + "naam", p["naam"])
        t(k + "uitleg", p["uitleg"])
        t(k + "hint1", p["hint1"], p.get("hint1_en"))
        t(k + "hint2", p["hint2"], p.get("hint2_en"))
        t(k + "aanwijzing", p["aanwijzing"])
        if "vraag" in p:
            t(k + "vraag", p["vraag"], p.get("vraag_en"))
    return nl, en


def build(h):
    _assets()
    _data()
    nl, en = teksten()
    for k, v in nl.items():
        h.lang(k, en[k], v)   # (the shared helper takes en first)


def _direct():
    _assets()
    uit = _data()
    nl, en = teksten()
    for taal, tk in (("nl_nl", nl), ("en_us", en)):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(tk)
        _schrijf(p, data)
    per = {}
    for j in uit:
        per[j["zone"]] = per.get(j["zone"], 0) + 1
    print(f"vadsrijk_t_puzzels: {len(uit)} puzzles {per}, {len(nl)} texts, {len(BLOKKEN)} blocks, {len(BLADZIJDES)} pages")


if __name__ == "__main__":
    _direct()
