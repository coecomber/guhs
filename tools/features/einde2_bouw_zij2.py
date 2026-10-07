"""
einde2 (the Guheinde expansion), slice bouw_zij2: the builds of five side places of the outer islands, each a
guhs:leegte_bouw in the high band with its own floating ground, listed in the Superkompas ("einde") and protected:

  - kyoshi_dorp               the Kyoshi-dorp (Schimmelkaasvlakte)            einde2_bouw_zij2_kyoshi.py
  - bibliotheek_wan_shi_guh   the Bibliotheek van Wan Shi Guh (Korstwoestijn)  einde2_bouw_zij2_bieb.py
  - leeuwschildpad_eiland     the island that is a lion-turtle guh             einde2_bouw_zij2_schildpad.py
  - leegtehaven               the floating harbour                             einde2_bouw_zij2_haven.py
  - guhvleugel_ringenrace     the ring race (Kruimelwolken)                    einde2_bouw_zij2_race.py

BUILD only: no quest logic and no characters. Where a character, a prop or a route of a later phase goes, the builders
leave the spot free and record a marker (the sidecars data/guhs/einde2/plekken/<id>.json; Java: feature/einde2/Plekken).
The only entities placed are creatures of this phase: the four Ender-Mika traders of the harbour and the two
otter-pinguinguhs of the village. The shared tools of the five builders: einde2_bouw_zij2_gereedschap.py.

Also here: the block guhs:ringenrace_ring (the glowing hoops of the course; no behaviour), the modest chest of a
vetschip's hold, the names and Superkompas lines, and one "find it" quest per place.
Java: nl.juiced.guhs.feature.einde2.bouwzij2 (E2BouwZij2Feature).
"""
import os

import features.einde2_kit as kit
from features import einde2_bouw_zij2_bieb as bieb
from features import einde2_bouw_zij2_haven as haven
from features import einde2_bouw_zij2_kyoshi as kyoshi
from features import einde2_bouw_zij2_race as race
from features import einde2_bouw_zij2_schildpad as schildpad

FTB_CHAPTER = "guhs_guheinde"
FTB_SECTION = "Haven, dorp en bibliotheek"

# id -> (the builder, its name, its Superkompas line)
PLEKKEN = {
    "kyoshi_dorp": (kyoshi, "Kyoshi-dorp", "Een dorp op de Schimmelkaasvlakte, onder een reuzenbeeld van Kyoshi-guh. Met een dojo vol waaiers"),
    "bibliotheek_wan_shi_guh": (bieb, "Bibliotheek van Wan Shi Guh",
                                "Een toren die tot zijn spits in het woestijnzand is weggezakt. Naar binnen ga je door het raam"),
    "leeuwschildpad_eiland": (schildpad, "Leeuwschildpad-eiland", "Een eiland met een bos erop. En een kop. En poten. Loop zachtjes, njeg"),
    "leegtehaven": (haven, "Leegtehaven", "Een zwevende haven: vetschepen, een markt van Ender-Mika's, de veerpier, de werf en de kroeg"),
    "guhvleugel_ringenrace": (race, "Guhvleugel-ringenrace", "Achttien zwevende ringen in de Kruimelwolken, voor Guhvleugels en Enderguhs"),
}

TEXTS = {
    "block.guhs.ringenrace_ring": "Ringenrace-ring",
}

# the chest in the hold of a moored vetschip: what a Mika leaves lying about, nothing a quest needs
RUIM = [("guhs:kaas_knabbels", 10, 2, 5), ("minecraft:honey_bottle", 6, 1, 2), ("minecraft:honeycomb", 6, 1, 3), ("minecraft:string", 5, 1, 4),
        ("minecraft:paper", 5, 1, 3), ("minecraft:lantern", 2, 1, 1), ("guhs:flitskruimel", 1, 1, 1)]


def textures(h):
    """The ring block: a warm glow with a pale core and two pink chevrons (the way you fly)."""
    from PIL import Image
    img = Image.new("RGBA", (16, 16))
    rand = [(214, 120, 30), (246, 164, 46), (255, 204, 84), (255, 226, 130)]
    for x in range(16):
        for y in range(16):
            e = min(x, y, 15 - x, 15 - y)
            c = rand[e] if e < 4 else (255, 240, 176)
            if e >= 3 and (abs(y - 7.5) + (x - 4)) in (0.5, 1.5) and 4 <= x <= 8:
                c = (255, 158, 196)
            elif e >= 3 and (abs(y - 7.5) + (x - 8)) in (0.5, 1.5) and 8 <= x <= 12:
                c = (255, 190, 214)
            img.putpixel((x, y), c + (255,))
    h.save(img, "block", "ringenrace_ring.png")


def blocks_and_items(h):
    h.simple_block("ringenrace_ring")
    h.self_drop("ringenrace_ring")
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:ringenrace_ring"])


def loot(h):
    h.w(f"{h.D}/loot_table/chests/einde2_bouw_zij2_ruim.json", {"type": "minecraft:chest", "pools": [{
        "rolls": {"type": "minecraft:uniform", "min": 3, "max": 5},
        "entries": [{"type": "minecraft:item", "name": name, "weight": weight,
                     "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
                    for name, weight, lo, hi in RUIM]}]})


def texts(h):
    for key, text in TEXTS.items():
        kit.lang(h, key, text)


def structuren(h):
    """Builds the five places (each builder checks itself and raises SystemExit on a problem), saves their tiles and
    writes their worldgen, sidecar and names."""
    for id, (module, titel, tooltip) in PLEKKEN.items():
        b = module.bouw(h)
        nx, nz = b.save_tiles()
        kit.leegte_structuur(h, b, titel=titel, tooltip=tooltip, y=module.Y)
        print(f"{id}: {b.W} x {b.H} x {b.D} at y {module.Y}, {nx} x {nz} tiles ({len(b.tiles[2])} saved), {len(b.plekken)} plekken, "
              f"{len(b.s.entities)} entities")


def selfcheck(h):
    """Every place has its structure, its guaranteed set, its sidecar and its tiles; every text is there; nothing of the
    stand-in platforms is left."""
    D = h.D
    missing = [k for k in TEXTS if k not in h.NL]
    for id in PLEKKEN:
        for path in (f"{D}/worldgen/structure/{id}.json", f"{D}/worldgen/structure_set/{id}_gegarandeerd.json", f"{D}/einde2/plekken/{id}.json",
                     f"{D}/tags/worldgen/biome/has_structure/{id}.json"):
            if not os.path.exists(path):
                missing.append(path)
        if not any(f.startswith("stuk_") for f in os.listdir(f"{D}/structure/{id}")):
            missing.append(f"tiles of {id}")
        for key in (f"structure.guhs.{id}", f"structure.guhs.{id}.tooltip"):
            if key not in h.NL or "gebouwd" in h.NL[key]:
                missing.append(key)
        if (kit.STRUCTUREN[id]["spreiding"] is not None) != os.path.exists(f"{D}/worldgen/structure_set/{id}.json"):
            missing.append(f"the rare set of {id}")
    for path in (f"{h.A}/blockstates/ringenrace_ring.json", f"{h.A}/textures/block/ringenrace_ring.png", f"{D}/loot_table/blocks/ringenrace_ring.json",
                 f"{D}/loot_table/chests/einde2_bouw_zij2_ruim.json"):
        if not os.path.exists(path):
            missing.append(path)
    if missing:
        raise SystemExit(f"einde2_bouw_zij2: missing: {missing}")


def build(h):
    textures(h)
    blocks_and_items(h)
    loot(h)
    texts(h)
    structuren(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests: a section of its own at the end of the Guheinde chapter (find the place; nothing is locked)
# =====================================================================================================================
def ftb(fq):
    q, structure = fq.q, fq.structure
    q("einde2_bouw_zij2_leegtehaven", "Vind de Leegtehaven", "Ergens tussen de buiteneilanden drijft een hele &6haven&r in het niets: steigers met "
      "vetschepen eraan, een markt waar Ender-Mika's achter de kraam staan, een veerpier, een scheepswerf en een kroeg. Het Superkompas "
      "(tabblad einde) wijst de weg. Land op de vlonder met het roze neusje.", "minecraft:lantern", [structure("leegtehaven")],
      rewards=(("guhs:kaas_knabbels", 10),), shape="circle")
    q("einde2_bouw_zij2_kyoshi_dorp", "Vind het Kyoshi-dorp", "Op de Schimmelkaasvlakte zweeft een dorp met zeegroene daken, en boven alles uit "
      "torent een &6reuzenbeeld van Kyoshi-guh&r: wit geschminkt, gouden waaiers, hele grote voeten. In de dojo ligt een dansvloer. De "
      "krijgsterguhs zijn vast even weg, njeg.", "minecraft:green_banner", [structure("kyoshi_dorp")], rewards=(("guhs:kaas_knabbels", 10),))
    q("einde2_bouw_zij2_bibliotheek_wan_shi_guh", "Vind de Bibliotheek van Wan Shi Guh", "In de Korstwoestijn steekt een groene torenspits uit het "
      "zand. Meer zie je niet. De rest van de &6bibliotheek&r hangt eronder: zes zalen diep, vol boeken. De ingang is het raam. Echt.",
      "minecraft:bookshelf", [structure("bibliotheek_wan_shi_guh")], rewards=(("guhs:kaas_knabbels", 10),))
    q("einde2_bouw_zij2_leeuwschildpad_eiland", "Vind het Leeuwschildpad-eiland", "Een eiland met een bos erop. En een staart. En een kop met "
      "manen en guh-oortjes. Dit eiland &6is iemand&r: een Leeuwschildpad-guh, zo oud dat hij vergeten is hoe oud. Loop over zijn arm naar "
      "zijn handpalm en zeg netjes gedag.", "minecraft:turtle_scute", [structure("leeuwschildpad_eiland")], rewards=(("guhs:kaas_knabbels", 12),),
      shape="diamond")
    q("einde2_bouw_zij2_guhvleugel_ringenrace", "Vind de Guhvleugel-ringenrace", "In de Kruimelwolken hangt een parcours van &6achttien "
      "gloeiende ringen&r: weg langs de noordkant, om de bocht, terug langs de zuidkant. Starttoren, landingsbaan, scorebord: alles staat "
      "klaar. Alleen de racemeester moet nog komen. Oefenen mag alvast.", "guhs:ringenrace_ring", [structure("guhvleugel_ringenrace")],
      rewards=(("guhs:kaas_knabbels", 10),), shape="rsquare")
