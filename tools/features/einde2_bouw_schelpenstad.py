"""
einde2 (the Guheinde expansion), slice bouw_schelpenstad: the road trip of the second half of the Sponsguh story as a BUILD
(no story logic, no cast: every character and every prop of the story is a marker in the sidecar).
Java: nl.juiced.guhs.feature.einde2.bouwschelpenstad (E2BouwSchelpenstadFeature).

  - guhs:schelpenstad: a guhs:leegte_bouw of 256 x 114 x 96 (the build itself is 48 high) in the deep band that stands next to Bikinibodem (its "bij",
    build contract 6.11): the road out of Bikinibodem, De Ruige Kroeg (a tugboat on the dry with the bar in its hull), the
    loopgraaf (a trench right across the slab, a path down and a path up, the perches of the monsters) and Schelpenstad (a
    seaside town of shell stone on a beach of kruimelzand: the souvenir shop of the Cycloop, a lighthouse, a pier into
    the void where David Hasselguh waits). The build: einde2_bouw_schelpenstad_bouw.py (+ _basis, _land, _kroeg, _stad).
  - eight decorative blocks einde2_bouw_schelpenstad_* (einde2_bouw_schelpenstad_blokken.py);
  - three modest loot chests, the FTB quest "find it", the test room of the game tests.

hook fase B: the markers of data/guhs/einde2/plekken/schelpenstad.json (Plekken.plek / wereld): the cast, the props, the
routes and the scene spot of the wink "Aang-guh en Appa-guh boven de loopgraaf".
"""
import os

import features.einde2_kit as kit
from features import einde2_bouw_schelpenstad_basis as basis
from features import einde2_bouw_schelpenstad_blokken as blokken
from features import einde2_bouw_schelpenstad_bouw as bouw

FTB_CHAPTER = "guhs_guheinde"
FTB_SECTION = "De weg naar Schelpenstad"

TITEL = "Schelpenstad"
TOOLTIP = "Een badplaatsje aan de Leegtezee, aan het eind van de weg uit Bikinibodem. Voorbij de Ruige Kroeg en de loopgraaf, njeg"
TEST_WINKEL = "einde2_bouw_schelpenstad_test_winkel"
TEST_DOOS = (180, 13, 13, 189, 19, 20)          # the drying corner of the souvenir shop: x0, y0, z0, x1, y1, z1 (template)
KISTEN = ("kroeg", "winkel", "vuurtoren")

TEXTS = {
    "advancements.guhs.guheinde.einde2_bouw_schelpenstad_zee.title": "Groetjes uit Schelpenstad",
    "advancements.guhs.guheinde.einde2_bouw_schelpenstad_zee.description": "Loop de hele weg uit: langs de Ruige Kroeg, door de loopgraaf, tot op de kop van de pier in de Leegtezee",
}


def loot(h):
    c, D = h.count_fn, h.D
    tabel = {
        # the barman's office: what a rough bar has lying about
        "kroeg": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:kaas_knabbels", "functions": c(4, 8)}]},
                  {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 3}, "entries": [
                      {"type": "minecraft:item", "name": "minecraft:bread", "weight": 4, "functions": c(1, 3)},
                      {"type": "minecraft:item", "name": "minecraft:glass_bottle", "weight": 4, "functions": c(1, 3)},
                      {"type": "minecraft:item", "name": "minecraft:iron_nugget", "weight": 3, "functions": c(2, 6)},
                      {"type": "minecraft:item", "name": "minecraft:music_disc_13", "weight": 1}]}],
        # the back of the souvenir shop: stock
        "winkel": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:einde2_bouw_schelpenstad_souvenir", "functions": c(1, 2)}]},
                   {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 3}, "entries": [
                       {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 5, "functions": c(3, 6)},
                       {"type": "minecraft:item", "name": "minecraft:paper", "weight": 4, "functions": c(2, 5)},
                       {"type": "minecraft:item", "name": "guhs:parelmoer", "weight": 3, "functions": c(1, 3)},
                       {"type": "minecraft:item", "name": "guhs:einde2_bouw_schelpenstad_kaartenrek", "weight": 1}]}],
        # the foot of the lighthouse: the keeper's things
        "vuurtoren": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:lichtstaafje", "functions": c(2, 4)}]},
                      {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 3}, "entries": [
                          {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 5, "functions": c(3, 6)},
                          {"type": "minecraft:item", "name": "minecraft:lantern", "weight": 3},
                          {"type": "minecraft:item", "name": "minecraft:spyglass", "weight": 1},
                          {"type": "minecraft:item", "name": "minecraft:string", "weight": 3, "functions": c(2, 4)}]}],
    }
    for naam, pools in tabel.items():
        h.w(f"{D}/loot_table/chests/einde2_bouw_schelpenstad_{naam}.json", {"type": "minecraft:chest", "pools": pools})


def test_winkel(h, b):
    """The drying corner of the souvenir shop as a little test room (the table, the lamp, the socket, a sprinkler)."""
    x0, y0, z0, x1, y1, z1 = TEST_DOOS
    s = h.Structure((x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1))
    for (x, y, z), bl in b.s.blocks.items():
        if x0 <= x <= x1 and y0 <= y <= y1 and z0 <= z <= z1:
            s.blocks[(x - x0, y - y0, z - z0)] = bl
    s.save(TEST_WINKEL)


def structuur(h):
    b, start, fouten = bouw.bouw(h)
    if fouten:
        raise SystemExit(f"schelpenstad: self-check found {len(fouten)} problem(s):\n  " + "\n  ".join(fouten[:40]))
    nx, nz = b.save_tiles()
    kit.leegte_structuur(h, b, titel=TITEL, tooltip=TOOLTIP, y=basis.Y, anker=basis.ANKER)
    test_winkel(h, b)
    print(f"schelpenstad: self-check ok {b.stats}, {nx} x {nz} tiles, lights {b.verlichting}")
    return b


def texts(h):
    for key, text in TEXTS.items():
        kit.lang(h, key, text)


def advancements(h):
    # (granted by Schelpenstad.java to whoever walks out to the head of the pier)
    kit.zichtbaar(h, "einde2_bouw_schelpenstad_zee", "guheinde_poort", "guhs:einde2_bouw_schelpenstad_schelpsteen", "goal",
                  TEXTS["advancements.guhs.guheinde.einde2_bouw_schelpenstad_zee.title"],
                  TEXTS["advancements.guhs.guheinde.einde2_bouw_schelpenstad_zee.description"])


def ftb(fq):
    fq.q("einde2_bouw_schelpenstad_vind", "De weg naar Schelpenstad",
         "Uit de poort van &dBikinibodem&r loopt een weg de leegte in. Volg hem: langs &6De Ruige Kroeg&r (een sleepboot vol stoere Mika's, "
         "en waag het niet om er bellen te blazen), dwars door &8de loopgraaf&r (diep, donker, vol monsters, veel plezier) en dan de "
         "heuvel af naar &bSchelpenstad&r aan de Leegtezee. Daar staat de souvenirwinkel van de Cycloop. Alles is er gedroogd, njeg.",
         "guhs:einde2_bouw_schelpenstad_schelpsteen", [fq.structure("schelpenstad")], rewards=(("guhs:kaas_knabbels", 10),), shape="circle")


def selfcheck(h, b):
    problems = blokken.check(h)
    D = h.D
    for naam in KISTEN:
        if not os.path.exists(f"{D}/loot_table/chests/einde2_bouw_schelpenstad_{naam}.json"):
            problems.append(f"no loot table for the chest of {naam}")
    kisten = {bl[2]["LootTable"] for bl in b.s.blocks.values() if bl[2] and "LootTable" in bl[2]}
    if kisten != {f"guhs:chests/einde2_bouw_schelpenstad_{n}" for n in KISTEN}:
        problems.append(f"the chests of the build: {sorted(kisten)}")
    for pad in (f"{D}/worldgen/structure/schelpenstad.json", f"{D}/worldgen/structure_set/schelpenstad.json", f"{D}/einde2/plekken/schelpenstad.json",
                f"{D}/structure/{TEST_WINKEL}.nbt", f"{D}/structure/schelpenstad/stuk_0_1.nbt", f"{D}/structure/schelpenstad/stuk_7_2.nbt"):
        if not os.path.exists(pad):
            problems.append(f"missing {pad}")
    if os.path.exists(f"{D}/worldgen/structure_set/schelpenstad_gegarandeerd.json"):
        problems.append("schelpenstad has no guaranteed set of its own (it stands next to Bikinibodem)")
    gebruikt = {bl[0] for bl in b.s.blocks.values()}
    for blok in basis.EIGEN_BLOKKEN:
        if blok not in gebruikt:
            problems.append(f"{blok} is not used in the build")
    for key in list(TEXTS) + ["structure.guhs.schelpenstad", "structure.guhs.schelpenstad.tooltip"]:
        if key not in h.NL:
            problems.append(f"no text {key}")
    # the Java side knows the same numbers (the game tests walk the road through the tiles)
    java = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "bouwschelpenstad", "E2BouwSchelpenstadGameTests.java"),
                encoding="utf-8").read()
    x0, y0, z0, x1, y1, z1 = TEST_DOOS
    for wil in (f"WEST_EIND = {basis.X_WEST_EIND}, OOST_BEGIN = {basis.X_OOST_BEGIN}, OOST_EIND = {basis.X_OOST_EIND}",
                f"TEST_X = {x0}, TEST_Y = {y0}, TEST_Z = {z0}", f"GROOTTE_X = {basis.W}, GROOTTE_Y = {basis.DOOS_H}, GROOTTE_Z = {basis.D}"):
        if wil not in java:
            problems.append(f"E2BouwSchelpenstadGameTests does not say '{wil}' (the build changed?)")
    if problems:
        raise SystemExit("einde2_bouw_schelpenstad self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    blokken.build(h)
    loot(h)
    texts(h)
    advancements(h)
    b = structuur(h)
    selfcheck(h, b)
