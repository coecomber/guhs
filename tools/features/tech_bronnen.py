"""
bbq2 (tech-bronnen): the sources of vadskracht. Java: feature/techbron (+ block/GuhWheel* for the Guhrad).

  Guhrad            exists (features/vadskracht.py makes its table data/guhs/vadskracht/guhrad.json from GUHRAD_VARIANTEN, which
                    this slice extended with Sam-guh and Guhshi); here: the lines about how each story guh does its rounds
  Knuffelgenerator  a pink cushion of 2 x 2 blocks; every tamed guh that lies on it gives vadskracht; they come by themselves
  Disco-dynamo      a dance floor of 3 x 3 blocks with a looping turntable; every dancing guh gives vadskracht; every disc
                    has its own light show; the mod's own disc (item tag guhs:techbron/zeldzame_plaat) gives a little more
  Blubkacheltje     a Sausblubje in a jar on a little stove; put it in with an item of the tag guhs:techbron/blubje_in_pot
                    (guhs:sausblubje_potje of the sausdieren slice), feed it from the tag guhs:techbron/blubvoer (kaasknabbels
                    and, optional, #guhs:sausdieren/sausblubje_voer)
  Gloeisterkern     200 vadskracht for ever; the recipe costs one gloeister
  Knabbelbatterij   the battery (vadskracht.batterij); its loot table copies the charge onto the item (guhs:techbron_lading)

This module writes: the textures, models, blockstates and item models (features/tech_bronnen_modellen.py), the loot tables,
the five recipes (tiers of CONTRACT_130 7: Knutselen = vanilla + kaasknabbels, Zout = zoutkristal, Saus = grillspies,
Gloeister = gloeister), the item and block tags, the texts, the advancements (hidden quest/tech_bronnen_* for the FTB quests
of tech-quests, visible techniek/tech_bronnen_*), and the test room techbron_test_kamer. It has no FTB quests: the chapter
Guh-technologie is written by tech_quests.py alone.
"""
import os
import re

from features import bbq2, vadskracht
from features import tech_bronnen_modellen as modellen

BLOKKEN = ("knuffelgenerator", "disco_dynamo", "blubkacheltje", "gloeisterkern", "knabbelbatterij")
DELEN = ("techbron_kussen_deel", "techbron_vloer_deel")
STIJLEN = ("hard", "zweeft", "dubbel", "sjouwt", "fladdert")      # GuhradStijl.java (without GEWOON)
ADVANCEMENTS = ("knuffel", "disco", "blub", "kern")

_GETALLEN = None


def getal(naam):
    """A number of feature/techbron/TechbronGetallen.java (what a source gives is in VadsGetallen: vadskracht.getal)."""
    global _GETALLEN
    if _GETALLEN is None:
        src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "techbron", "TechbronGetallen.java"),
                   encoding="utf-8").read()
        _GETALLEN = {m.group(1): eval(m.group(2), {"__builtins__": {}}) for m in re.finditer(r"\b([A-Z][A-Z0-9_]*)\s*=\s*([0-9][0-9 *]*);", src)}
    if naam not in _GETALLEN:
        raise SystemExit(f"tech_bronnen: TechbronGetallen heeft geen {naam}")
    return _GETALLEN[naam]


# =====================================================================================================================
# blocks, items, loot, recipes, tags
# =====================================================================================================================
def blocks_and_items(h):
    modellen.knuffelgenerator(h)
    modellen.disco_dynamo(h)
    modellen.blubkacheltje(h)
    modellen.gloeisterkern(h)
    modellen.knabbelbatterij(h)
    for name in BLOKKEN:
        h.self_drop(name)
    # the Knabbelbatterij keeps its charge: the loot table copies it from the block entity onto the item
    h.w(f"{h.D}/loot_table/blocks/knabbelbatterij.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0,
        "entries": [{"type": "minecraft:item", "name": "guhs:knabbelbatterij", "functions": [
            {"function": "minecraft:copy_components", "source": "block_entity", "include": ["guhs:techbron_lading"]}]}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    # the hover readout works on every block of a source, its parts too
    vadskracht.toon(h, *BLOKKEN, *DELEN)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{b}" for b in ("disco_dynamo", "blubkacheltje", "gloeisterkern", "techbron_vloer_deel")])
    # (the cushion breaks like wool: by hand)
    h.add_tag("guhs/tags/item/techbron/zeldzame_plaat", ["guhs:music_disc_ze_hangen"])
    h.add_tag("guhs/tags/item/techbron/blubje_in_pot", ["guhs:sausblubje_potje"])
    # (kaasknabbels, and whatever a Sausblubje eats once the sausdieren slice is merged: its tag, optional so this one loads without it)
    h.add_tag("guhs/tags/item/techbron/blubvoer", ["guhs:kaas_knabbels", {"id": "#guhs:sausdieren/sausblubje_voer", "required": False}])


def recipes(h):
    # Knutselen (direct): vanilla + kaasknabbels
    h.shaped("knuffelgenerator", ["WWW", "WKW", "VVV"],
             {"W": "minecraft:pink_wool", "K": "guhs:kaas_knabbels", "V": "minecraft:feather"}, "guhs:knuffelgenerator")
    h.shaped("disco_dynamo", ["GGG", "RJR", "KKK"],
             {"G": "minecraft:pink_stained_glass", "R": "minecraft:redstone", "J": "minecraft:jukebox", "K": "guhs:kaas_knabbels"},
             "guhs:disco_dynamo")
    # Zout: zoutkristal from the Barbecuether
    h.shaped("knabbelbatterij", ["IZI", "ZKZ", "IRI"],
             {"I": "minecraft:iron_ingot", "Z": "guhs:zoutkristal", "K": "guhs:kaas_knabbels", "R": "minecraft:redstone_block"},
             "guhs:knabbelbatterij")
    # Saus: a grillspies (the Sausblubje itself goes in later, in its jar)
    h.shaped("blubkacheltje", ["GGG", "G G", "ISI"],
             {"G": "minecraft:glass", "I": "minecraft:iron_ingot", "S": "guhs:grillspies"}, "guhs:blubkacheltje")
    # Gloeister: the gloeister of the Aangebrande Mika (one, and it never runs out)
    h.shaped("gloeisterkern", ["ZKZ", "KGK", "ZKZ"],
             {"Z": "guhs:zoutkristal", "K": "guhs:grillkool", "G": "guhs:gloeister"}, "guhs:gloeisterkern")


def advancements(h):
    for name in ADVANCEMENTS:
        bbq2.verborgen(h, f"tech_bronnen_{name}")
    v = vadskracht.getal
    bbq2.zichtbaar(h, "techniek", "tech_bronnen_knuffel", "root", "guhs:knuffelgenerator", "task", "Knuffelkracht",
                   "Laat een tamme guh op je Knuffelgenerator liggen. Njeg, wat ligt dat lekker!")
    bbq2.zichtbaar(h, "techniek", "tech_bronnen_disco", "tech_bronnen_knuffel", "guhs:disco_dynamo", "task", "Dansen tot de lampjes branden",
                   "Leg een plaat op je Disco-dynamo en laat een guh de vadskracht bij elkaar dansen")
    bbq2.zichtbaar(h, "techniek", "tech_bronnen_blub", "tech_bronnen_disco", "guhs:blubkacheltje", "task", "Blub blub, lekker warm",
                   "Houd een Sausblubje warm in een Blubkacheltje (het lust graag een kaasknabbel)")
    bbq2.zichtbaar(h, "techniek", "tech_bronnen_kern", "tech_bronnen_blub", "guhs:gloeisterkern", "goal", "Een ster in een kooitje",
                   f"Zet een Gloeisterkern neer: {v('GLOEISTERKERN')} vadskracht, voor altijd")


def test_templates(h):
    """techbron_test_kamer: 15 x 6 x 15 with a stone floor (things stand at helper y 2)."""
    s = h.Structure((15, 6, 15))
    s.fill(0, 0, 0, 14, 0, 14, "minecraft:stone")
    s.save("techbron_test_kamer")


# =====================================================================================================================
# texts
# =====================================================================================================================
K = "gui.guhs.techbron."


def _teksten():
    v = vadskracht.getal
    bereik, minuten = getal("BEREIK"), getal("BLUB_SECONDEN") // 60
    knuffel_max, disco_max = v("KNUFFEL_MAX_GUHS") * v("KNUFFEL_PER_GUH"), v("DISCO_MAX_GUHS") * v("DISCO_PER_GUH")
    half_uur = v("BATTERIJ") // v("GUHRAD") // 60       # minutes of one Guhrad that fit in a battery
    return {
        # names
        "block.guhs.knuffelgenerator": "Knuffelgenerator",
        "block.guhs.disco_dynamo": "Disco-dynamo",
        "block.guhs.blubkacheltje": "Blubkacheltje",
        "block.guhs.gloeisterkern": "Gloeisterkern",
        "block.guhs.knabbelbatterij": "Knabbelbatterij",
        "block.guhs.techbron_kussen_deel": "Knuffelgenerator",
        "block.guhs.techbron_vloer_deel": "Disco-dynamo",
        # the line of text on the items
        "block.guhs.knuffelgenerator.lore":
            f"Een groot roze kussen van 2 bij 2 blokken. Tamme guhs binnen {bereik} blokken komen er vanzelf op liggen, en elke guh die "
            f"ligt te knuffelen geeft {v('KNUFFEL_PER_GUH')} vadskracht (hooguit {v('KNUFFEL_MAX_GUHS')} guhs: {knuffel_max} vadskracht). "
            "Zet Rondvadsen uit en je guh blijft liggen als jij wegloopt. Guhs met een eigen Guhhuisje hebben het te druk met hun klusjes.",
        "block.guhs.disco_dynamo.lore":
            f"Een dansvloer van 3 bij 3 blokken met een draaitafel. Leg er een muziekplaat op en tamme guhs binnen {bereik} blokken "
            f"komen dansen: elke danser geeft {v('DISCO_PER_GUH')} vadskracht (hooguit {v('DISCO_MAX_GUHS')} guhs: {disco_max} "
            "vadskracht). Elke plaat heeft zijn eigen lichtshow, en een lege hand haalt de plaat er weer af. Vahoeg!",
        "block.guhs.blubkacheltje.lore":
            f"Stop er een Sausblubje in een potje in en geef het af en toe een kaasknabbel: het blubt {v('BLUBKACHELTJE')} vadskracht "
            f"bij elkaar. Eén knabbel houdt het {minuten} minuten warm. Sluipen + lege hand: het blubje mag er weer uit.",
        "block.guhs.gloeisterkern.lore":
            f"De gloeister van de Aangebrande Mika in een kooitje van zoutkristal: {v('GLOEISTERKERN')} vadskracht, voor altijd. Hij "
            "raakt nooit op en wil geen knabbels. Er telt er maar één per opstelling.",
        "block.guhs.knabbelbatterij.lore":
            f"Bewaart de vadskracht die je opstelling over heeft, tot {v('BATTERIJ')} (zo'n {half_uur} minuten van één Guhrad), en past "
            "bij als je machines even meer vragen. Hij houdt zijn lading als je hem afbreekt en meeneemt.",
        K + "batterij.lading": "Lading: %s/%s vadskracht",
        # Knuffelgenerator
        K + "knuffel.leeg": "Nog geen guh op het kussen: tamme guhs binnen %s blokken komen vanzelf liggen",
        K + "knuffel.guhs": "%s/%s guhs liggen te knuffelen, njeg!",
        # Disco-dynamo
        K + "disco.geen_plaat": "Geen plaat: leg een muziekplaat op de draaitafel",
        K + "disco.draait": "Draait: %s",
        K + "disco.leeg": "De vloer is nog leeg: tamme guhs binnen %s blokken komen vanzelf dansen",
        K + "disco.dansers": "%s/%s guhs dansen de vadskracht bij elkaar, vahoeg!",
        K + "disco.zeldzaam": "Zeldzame plaat: elke danser geeft %s vadskracht extra",
        # Blubkacheltje
        K + "blub.leeg": "Het potje is leeg: stop er een Sausblubje in een potje in",
        K + "blub.trek": "Het Sausblubje heeft trek: geef het een kaasknabbel, njeg!",
        K + "blub.warm": "Het Sausblubje blubt lekker warm (%s/%s knabbels in het bakje)",
        K + "blub.bezet": "Hier woont al een Sausblubje",
        K + "blub.erin": "Blub! Het Sausblubje zit er warmpjes bij",
        K + "blub.erin_trek": "Blub? Het Sausblubje heeft trek: geef het een kaasknabbel",
        K + "blub.vol": "Het bakje zit vol knabbels",
        K + "blub.gevoerd": "Smak smak! %s/%s knabbels in het bakje",
        K + "blub.voer_zonder_blubje": "De knabbel ligt klaar. Nu nog een Sausblubje, njeg",
        # the Guhrad per variant, and the happy guh
        K + "guhrad.hard": "De Baltoguh rent harder dan wie ook: een echte sledeguh",
        K + "guhrad.zweeft": "Guhtwo rent niet. Hij zweeft, en het rad draait vanzelf mee",
        K + "guhrad.dubbel": "De 626-guh telt dubbel: zes pootjes rennen harder dan vier",
        K + "guhrad.sjouwt": "Sam-guh sjouwt het hele rad rond, met rugzak en al",
        K + "guhrad.fladdert": "Guhshi fladdertrappelt het rad in het rond",
        K + "guhrad.tip": "Tip: een guh die net op een Knuffelgenerator lag is blij, en rent harder",
        K + "guhrad.rent": "Je guh rent: %s vadskracht!",
        K + "guhrad.rent_blij": "Je blije guh rent extra hard: %s vadskracht, vahoeg!",
    }


def texts(h):
    for key, nl in _teksten().items():
        h.lang(key, nl, nl)


# =====================================================================================================================
# self-check
# =====================================================================================================================
def selfcheck(h):
    A, D = h.A, h.D
    paden = [f"{A}/blockstates/{b}.json" for b in BLOKKEN + DELEN]
    paden += [f"{A}/models/item/{b}.json" for b in BLOKKEN]
    paden += [f"{D}/loot_table/blocks/{b}.json" for b in BLOKKEN]
    paden += [f"{D}/recipe/{b}.json" for b in BLOKKEN]
    paden += [f"{A}/models/block/{b}_{s}.json" for b in BLOKKEN[:4] for s in vadskracht.STATEN]
    paden += [f"{A}/models/block/{m}.json" for m in ("disco_dynamo_tegel", "disco_dynamo_lampen", "disco_dynamo_lampen_kern",
                                                      "blubkacheltje_blubje", "gloeisterkern_ster")]
    paden += [f"{D}/tags/item/techbron/{t}.json" for t in ("zeldzame_plaat", "blubje_in_pot", "blubvoer")]
    paden += [f"{D}/advancement/quest/tech_bronnen_{a}.json" for a in ADVANCEMENTS]
    paden += [f"{D}/advancement/techniek/tech_bronnen_{a}.json" for a in ADVANCEMENTS]
    paden += [f"{D}/structure/techbron_test_kamer.nbt"]
    missing = [p for p in paden if not os.path.exists(p)]
    missing += [k for k in _teksten() if k not in h.NL]
    missing += [K + "guhrad." + s for s in STIJLEN if K + "guhrad." + s not in h.NL]
    # every style of GuhradStijl.java has its line, and every story guh of the Guhrad table is a variant with a style
    stijl = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "techbron", "GuhradStijl.java"), encoding="utf-8").read()
    for variant in vadskracht.GUHRAD_VARIANTEN:
        if f'case "{variant}"' not in stijl:
            missing.append(f"GuhradStijl for {variant}")
    for naam in ("BEREIK", "BLUB_SECONDEN", "BLUB_VOER_MAX", "DISCO_BONUS"):
        getal(naam)
    if missing:
        raise SystemExit(f"tech_bronnen: missing {missing}")


def build(h):
    blocks_and_items(h)
    recipes(h)
    advancements(h)
    test_templates(h)
    texts(h)
    selfcheck(h)
