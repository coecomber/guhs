"""
bbq2 (tech-quests): where Guh-technologie is taught and where it ends. Java: feature/techquest.

  Oude Guhrad-centrale     structure guhs:oude_guhrad_centrale in the Guhbarbecuether (tech_quests_bouw.py): an old power
                           station with five Guhraden that still run, a practice hall with five broken setups, the
                           workshop of the Uitvinder-guh and the bordes of De Grote Knabbelmachine. Protected; the setups
                           are put down and broken again by Centrale.java, so every player finds them broken.
  questline "techniek"     the Uitvinder-guh (kind UITVINDERGUH): mend the five setups, bring him zoutkristal. Reward: the
                           Bodemloos Knabbelmaagje (bank's upgrade item) and three recipe cards.
  the recipe cards         techquest_recept_saus / _machines / _bezorg: this module puts a card into the recipes of the
                           "Saus" tier (KAARTEN): that is the gate of tier 3 (DESIGN_130 2, progression).
  questline "knabbelmachine"  De Grote Knabbelmachine (tech_quests_modellen.py draws it): five stages of big deliveries
                           (LEVERINGEN = Knabbelmachine.FASEN), per player; then a perfect knabbel a day, the statuette, the
                           title Knabbelmachinist.
  the FTB chapter          Guh-technologie, the WHOLE chapter, as projects (tech_quests_ftb.py); the preview of what the
                           Guheinde will add stays at its end (FTB_SLOT).

Dutch only (CONTRACT_130 1); English names are proposed in the slice report.
"""
import json
import os
import re

import numpy as np
from PIL import Image, ImageDraw

from features import bbq2, tech_machines, vadskracht, verhaal_motor, wereld
from features import sterrenwacht_hulp as hulp
from features import tech_quests_bouw as bouw
from features import tech_quests_ftb
from features import tech_quests_modellen as modellen

SALT = 21300801                  # (CONTRACT_130 3, slice 08: 213008NN; the guaranteed set is SALT + 7)
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "techquest")

# The FTB chapter guhs_techniek is written by this module only, as PROJECTS (CONTRACT_130 8): tech_quests_ftb.py.
FTB_SECTIES = tech_quests_ftb.SECTIES
# One picture at the very END of the chapter with no quests under it (make_ftbquests.py FTB_SLOT): the preview that the
# last part of the Guh-technologie is not there yet and belongs to the Guheinde (the Bestelguh, draadloze vadskracht and
# the Guhterminal come with the next update).
FTB_SLOT = ("Wordt vervolgd in het Guheinde...", "guh:ender",
            ["Hier ontbreekt nog een stukje. Njeg!",
             "De laatste uitvindingen liggen diep in het &5Guheinde&r:",
             "de Bestelguh, draadloze vadskracht en de Guhterminal.",
             "Die komen in een volgende update. Bouw vast een mooie fabriek!"])

# recipe card -> the recipes it goes into (its place: the top-left cell). The machines of tech_machines go through its own
# hook; the others are recipes that the modules before this one wrote (FEATURES order), patched in place.
KAARTEN = {
    "techquest_recept_saus": ["sauspomp", "sausvat", "brouwautomaat", "frituurautomaat", "grillkoolpers", "blubkacheltje"],
    "techquest_recept_machines": ["knabbelaar", "neerzetter", "knutselmachine", "tekentafel"],
    "techquest_recept_bezorg": ["stepstation", "haltepaaltje"],
}
LEVERINGEN = tech_quests_ftb.LEVERINGEN      # the five stages of De Grote Knabbelmachine (Knabbelmachine.FASEN)
ZOUT = tech_quests_ftb.ZOUT                  # (UitvinderRol.ZOUT)
ITEMS = ["perfecte_knabbel", "techquest_recept_saus", "techquest_recept_machines", "techquest_recept_bezorg", "techquest_oefenknabbel",
         "techquest_oefenrommel"]


def ftb(fq):
    tech_quests_ftb.ftb(fq)


# =====================================================================================================================
# textures: the item icons
# =====================================================================================================================
def _kaart(band, teken):
    """A recipe card: parchment with a coloured band and a little drawing (teken(draw))."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 1, 13, 14], fill=(238, 222, 184, 255), outline=(120, 92, 56, 255))
    d.rectangle([3, 2, 12, 4], fill=band + (255,))
    d.line([4, 12, 11, 12], fill=(170, 146, 104, 255))
    d.point([(13, 1), (2, 14)], fill=(0, 0, 0, 0))
    teken(d)
    return img


def _knabbel(licht, basis, donker, rand, glans=None):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([2, 4, 13, 13], fill=basis + (255,), outline=rand + (255,))
    d.ellipse([4, 5, 9, 8], fill=licht + (255,))
    d.arc([3, 5, 12, 12], 20, 120, fill=donker + (255,))
    for (x, y) in ((6, 10), (10, 8), (8, 11)):
        d.point((x, y), fill=donker + (255,))
    if glans:
        for (x, y) in glans:
            d.point((x, y), fill=(255, 255, 255, 255))
    return img


def textures(h):
    save = h.save
    # the perfect knabbel: golden, with a sparkle
    perfect = _knabbel((255, 244, 170), (255, 208, 72), (226, 150, 36), (150, 92, 20), glans=[(12, 2), (11, 3), (13, 3), (12, 4), (3, 3), (5, 6)])
    save(perfect, "item", "perfecte_knabbel.png")
    save(_knabbel((214, 180, 132), (188, 150, 100), (140, 104, 62), (96, 70, 40)), "item", "techquest_oefenknabbel.png")
    prop = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(prop)
    d.polygon([(4, 5), (9, 3), (13, 6), (12, 11), (8, 13), (3, 10)], fill=(240, 238, 228, 255), outline=(150, 146, 136, 255))
    d.line([5, 6, 8, 9, 11, 7], fill=(190, 186, 174, 255))
    d.line([6, 11, 8, 9], fill=(200, 196, 184, 255))
    save(prop, "item", "techquest_oefenrommel.png")

    def druppel(d):
        d.polygon([(8, 6), (10, 9), (8, 11), (6, 9)], fill=(242, 150, 30, 255))
        d.point((8, 6), fill=(255, 200, 90, 255))

    def tandwiel(d):
        d.rectangle([6, 7, 10, 10], fill=(110, 106, 116, 255))
        d.point([(8, 6), (8, 11), (5, 8), (11, 9)], fill=(110, 106, 116, 255))
        d.point((8, 8), fill=(238, 222, 184, 255))

    def step(d):
        d.line([5, 10, 10, 10], fill=(90, 86, 96, 255))
        d.line([10, 10, 10, 6], fill=(90, 86, 96, 255))
        d.point([(5, 11), (10, 11)], fill=(40, 36, 44, 255))
        d.point((9, 6), fill=(250, 204, 60, 255))
    save(_kaart((242, 150, 30), druppel), "item", "techquest_recept_saus.png")
    save(_kaart((226, 110, 160), tandwiel), "item", "techquest_recept_machines.png")
    save(_kaart((250, 204, 60), step), "item", "techquest_recept_bezorg.png")


# =====================================================================================================================
# models, loot, tags, the recipe cards in the recipes
# =====================================================================================================================
def blokken_en_items(h):
    D = h.D
    modellen.build(h)
    for i in ITEMS:
        h.item_model(i)
    h.self_drop("knabbelmachine_beeldje")
    # (the kern of the machine is part of its building: it never drops anything)
    h.w(f"{D}/loot_table/blocks/grote_knabbelmachine.json", {"type": "minecraft:block", "pools": []})
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:knabbelmachine_beeldje"])
    # the barrels of the workshop: modest
    kist = [("guhs:guh_wire", 5, 2, 5), ("guhs:kaas_knabbels", 5, 2, 6), ("minecraft:redstone", 4, 1, 4), ("minecraft:iron_nugget", 4, 2, 6),
            ("minecraft:copper_ingot", 3, 1, 3), ("minecraft:paper", 3, 1, 3), ("minecraft:pink_dye", 2, 1, 2), ("guhs:zoutkristal", 1, 1, 1)]
    h.w(f"{D}/loot_table/chests/tech_quests_werkplaats.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
            {"type": "minecraft:item", "name": item, "weight": wgt, "functions": h.count_fn(lo, hi)} for item, wgt, lo, hi in kist]}]})


def kaart_in_recept(h, naam, kaart):
    """Puts a recipe card into the top-left cell of a shaped recipe that an earlier module wrote (it stays in the grid:
    the card is its own crafting remainder)."""
    def patch(r):
        patroon, sleutel = list(r["pattern"]), dict(r["key"])
        if sleutel.get("X") == kaart:
            return
        assert "X" not in sleutel, f"recipe {naam} already uses the key X"
        if len(patroon[0]) == 1:                               # (a recipe one cell wide: the card gets a column of its own)
            patroon = [" " + rij for rij in patroon]
        patroon[0] = "X" + patroon[0][1:]
        sleutel["X"] = kaart
        r["pattern"] = patroon
        r["key"] = {k: v for k, v in sleutel.items() if any(k in rij for rij in patroon)}
    h.patch_json(f"{h.D}/recipe/{naam}.json", patch)


def recepten(h):
    for kaart, namen in KAARTEN.items():
        for naam in namen:
            if naam in tech_machines.RECEPTEN:
                tech_machines.recept(h, naam, kaart=f"guhs:{kaart}")
            else:
                kaart_in_recept(h, naam, f"guhs:{kaart}")


# =====================================================================================================================
# the building
# =====================================================================================================================
def structuur(h):
    problems, b = bouw.build(h)
    if problems:
        print("tech_quests geometry check found problems:\n  " + "\n  ".join(problems[:40]))
        raise SystemExit("tech_quests: fix the template (see the geometry check above)")
    s = h.Structure((13, 8, 33))                               # the test room for a copy that is turned a quarter
    s.fill(0, 0, 0, 12, 1, 32, "minecraft:stone")
    s.save("techquest_test_draai")
    wereld.bbq_structuur(h, bouw.NAAM, soort="grot", titel="Oude Guhrad-centrale",
                         tooltip="Een oude krachtcentrale waar vijf guhs nog altijd hun rondjes rennen, met de Uitvinder-guh en zijn oefenhal (Guhbarbecuether)",
                         biomes=["houtskoolvlakte", "asdal", "worstenwoud", "satebos"], salt=SALT, templates=[(bouw.NAAM, 1)],
                         spacing=44, separation=18, gegarandeerd=dict(sector=0, min=250, max=900), voorrang=280,
                         grootte=36, vlak=10, hoogte=24)

    # the centre jigsaw is in layer 0 and the pool says where the ground really is (as fossiel-mijn does): the floor lands
    # on the cave floor and the terrain is smoothed towards it
    def grond(pool):
        for e in pool["elements"]:
            el = {"element_type": "guhs:grond_single_pool_element"}
            el.update({k: v for k, v in e["element"].items() if k not in ("element_type", "ground_level_delta")})
            el["ground_level_delta"] = bouw.G + 1
            e["element"] = el
    h.patch_json(f"{h.D}/worldgen/template_pool/{bouw.NAAM}/start.json", grond)
    return b


# =====================================================================================================================
# the Uitvinder-guh: the sitting guh with welding goggles on his forehead, a wild white tuft, a pencil, a leather apron
# =====================================================================================================================
def npc(h):
    c = hulp.cube
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_uitvinderguh")
    sw = hulp.swatches(geo, ["band", "bril", "glas", "haar", "potlood", "punt", "schort", "zak", "sleutel"])
    geo["bones"].append({"name": "uitvinder_bril", "parent": "head", "pivot": [0, 24, -7], "cubes": [
        c([-6.8, 23.3, -7.4], [13.6, 1.4, 13.2], sw["band"]),                                          # the strap round his head
        c([-4.8, 22.5, -8.7], [3.8, 3.0, 1.4], sw["bril"]), c([1.0, 22.5, -8.7], [3.8, 3.0, 1.4], sw["bril"]),   # two brass rims, pushed up
        c([-4.2, 23.1, -9.0], [2.6, 1.8, 0.4], sw["glas"]), c([1.6, 23.1, -9.0], [2.6, 1.8, 0.4], sw["glas"]),   # green glass
        c([-1.0, 23.5, -8.5], [2.0, 1.0, 0.8], sw["band"])]})
    geo["bones"].append({"name": "uitvinder_haar", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        c([-3.4, 25.4, -3.6], [2.0, 3.6, 2.0], sw["haar"], rotation=[0, 0, 20], pivot=[-2.4, 25.4, -2.6]),
        c([-1.0, 25.4, -2.0], [2.0, 5.0, 2.0], sw["haar"]),
        c([1.6, 25.4, -3.4], [2.0, 3.2, 2.0], sw["haar"], rotation=[0, 0, -24], pivot=[2.6, 25.4, -2.4]),
        c([-1.8, 25.4, 0.6], [2.0, 3.4, 2.0], sw["haar"], rotation=[-22, 0, 0], pivot=[-0.8, 25.4, 1.6]),
        c([1.0, 25.4, 1.2], [1.8, 2.6, 1.8], sw["haar"], rotation=[-14, 0, -16], pivot=[1.9, 25.4, 2.1]),
        c([-3.6, 25.2, -4.2], [7.2, 0.9, 7.0], sw["haar"]),
        c([-7.6, 19.6, -1.6], [1.2, 3.4, 2.6], sw["haar"], rotation=[0, 0, 12], pivot=[-7.0, 21.0, -0.3]),   # a tuft over each ear
        c([6.4, 19.6, -1.6], [1.2, 3.4, 2.6], sw["haar"], rotation=[0, 0, -12], pivot=[7.0, 21.0, -0.3])]})
    geo["bones"].append({"name": "uitvinder_potlood", "parent": "head", "pivot": [6.4, 21, -2], "cubes": [
        c([6.2, 20.6, -5.0], [0.8, 0.8, 6.0], sw["potlood"], rotation=[14, 0, 0], pivot=[6.6, 21.0, -2.0]),
        c([6.2, 20.6, -6.0], [0.8, 0.8, 1.0], sw["punt"], rotation=[14, 0, 0], pivot=[6.6, 21.0, -2.0])]})
    geo["bones"].append({"name": "uitvinder_schort", "parent": "body", "pivot": [0, 8, 0], "cubes": [
        c([-4.2, 2.6, -5.2], [8.4, 8.4, 0.7], sw["schort"]),                                           # the bib
        c([-5.4, 9.6, -4.8], [10.8, 1.2, 8.8], sw["schort"], inflate=0.1),                             # its strap
        c([-2.2, 3.6, -5.6], [4.4, 3.0, 0.5], sw["zak"]),                                              # a pocket
        c([-1.4, 5.8, -5.9], [0.9, 3.6, 0.5], sw["sleutel"]), c([-1.8, 9.0, -5.9], [1.7, 1.0, 0.5], sw["sleutel"]),   # a wrench in it
        c([0.7, 5.8, -5.9], [0.7, 2.6, 0.5], sw["potlood"])]})
    hulp.save_geo(h, "guh_npc_uitvinderguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.11, sat=0.34, val=1.04)                                         # an old guh: creamy, nearly white
    rng = np.random.default_rng(30811)
    hulp.paint_swatch(a, sw["band"], (88, 60, 40), rng, 6)
    hulp.paint_swatch(a, sw["bril"], (214, 168, 70), rng, 6)

    def glans(block):
        block[4:12, 4:12, :3] = (200, 255, 220)
    hulp.paint_swatch(a, sw["glas"], (96, 200, 150), rng, 4, glans)
    hulp.paint_swatch(a, sw["haar"], (246, 244, 238), rng, 5)
    hulp.paint_swatch(a, sw["potlood"], (250, 204, 60), rng, 4)
    hulp.paint_swatch(a, sw["punt"], (60, 56, 60), rng, 3)
    hulp.paint_swatch(a, sw["schort"], (140, 96, 60), rng, 7)
    hulp.paint_swatch(a, sw["zak"], (112, 74, 44), rng, 6)
    hulp.paint_swatch(a, sw["sleutel"], (176, 180, 190), rng, 5)
    h.save(Image.fromarray(a), "entity", "npc_uitvinderguh.png")


# =====================================================================================================================
# texts
# =====================================================================================================================
def _lang():
    rad, oven, molen = vadskracht.getal("GUHRAD"), vadskracht.getal("GUH_OVEN"), vadskracht.getal("MOLEN")
    Q = "quest.guhs.techquest.uitvinder."
    G = "gui.guhs.techquest."
    t = {
        # blocks and items
        "block.guhs.grote_knabbelmachine": "De Grote Knabbelmachine",
        "block.guhs.grote_knabbelmachine.lore": "Het bakje onder zijn bek. Wie hem afbouwt, krijgt er elke dag één perfecte knabbel uit",
        "block.guhs.techquest_knabbelmachine_deel": "Stukje Grote Knabbelmachine",
        "block.guhs.knabbelmachine_beeldje": "Knabbelmachine-beeldje",
        "block.guhs.knabbelmachine_beeldje.lore": "De Grote Knabbelmachine, maar dan klein. Klik erop en hij knabbelt",
        "block.guhs.knabbelmachine_beeldje.klik": "Knabbel knabbel knabbel!",
        "item.guhs.perfecte_knabbel": "Perfecte knabbel",
        "item.guhs.perfecte_knabbel.lore": "Knapperig, kazig en nog warm. Er komt er maar één per dag uit de Grote Knabbelmachine",
        "item.guhs.techquest_recept_saus": "Receptkaart: saus en slangen",
        "item.guhs.techquest_recept_saus.lore": "Voor de Sauspomp, het Sausvat, de Brouwautomaat, de Frituurautomaat, de Grillkoolpers en het Blubkacheltje",
        "item.guhs.techquest_recept_machines": "Receptkaart: knabbelende machines",
        "item.guhs.techquest_recept_machines.lore": "Voor de Knabbelaar, de Neerzetter, de Knutselmachine en de Tekentafel",
        "item.guhs.techquest_recept_bezorg": "Receptkaart: het Bezorgguhtje",
        "item.guhs.techquest_recept_bezorg.lore": "Voor het Stepstation en de Haltepaaltjes",
        "item.guhs.techquest_recept.blijft": "Blijft in het werkbankrooster liggen: één kaart is genoeg voor altijd",
        "item.guhs.techquest_oefenknabbel": "Oefenknabbel",
        "item.guhs.techquest_oefenknabbel.lore": "Van karton, voor de buizen van de oefenhal. Niet eten: smaakt naar doos",
        "item.guhs.techquest_oefenrommel": "Propje papier",
        "item.guhs.techquest_oefenrommel.lore": "Rommel uit de oefenhal. Een Filterstuk hoort dit tegen te houden",
        "gui.guhs.titels.naam.knabbelmachinist": "Knabbelmachinist",
        "gui.guhs.titels.hint.knabbelmachinist": "Bouw iets heel groots af bij de Uitvinder-guh",
        # the practice hall
        G + "opstelling.draad.gemaakt": "De oven is wakker! Opstelling 1 werkt",
        G + "opstelling.te_zwaar.gemaakt": "Het rad kan het weer aan! Opstelling 2 werkt",
        G + "opstelling.buis.gemaakt": "De knabbels rollen de goede kant op! Opstelling 3 werkt",
        G + "opstelling.filter.gemaakt": "Het filter laat alleen nog knabbels door! Opstelling 4 werkt",
        G + "opstelling.filter.papier": "De propjes papier die er al doorheen waren, veegt de Uitvinder-guh terug in de eerste ton",
        G + "opstelling.saus.gemaakt": "Er zit saus in het vat! Opstelling 5 werkt",
        G + "opstelling.draad.verder": "Opstelling 1 is klaar. Op naar opstelling 2, het vak ernaast!",
        G + "opstelling.te_zwaar.verder": "Opstelling 2 is klaar. De Uitvinder-guh wil je spreken: hij heeft zout nodig",
        G + "opstelling.buis.verder": "Opstelling 3 is klaar. Op naar opstelling 4: het filter",
        G + "opstelling.filter.verder": "Opstelling 4 is klaar. Op naar opstelling 5: de saus (vraag de Uitvinder-guh om een Sausslang)",
        G + "opstelling.saus.verder": "Opstelling 5 is klaar: de hele oefenhal werkt! Vertel het de Uitvinder-guh",
        G + "opstelling.stuk": "(Een opstelling gaat vanzelf weer stuk als iedereen is weggelopen: de volgende speler wil ook oefenen, njeg.)",
        # the machine
        G + "machine.eerst": "Een leeg bakje. De Uitvinder-guh heeft hier vast plannen mee",
        G + "machine.plan": "Hier komt iets heel groots. Vraag het de Uitvinder-guh maar",
        G + "machine.bezig": "Jouw Grote Knabbelmachine is nog niet af (%s van de %s stappen). Breng de Uitvinder-guh wat hij nodig heeft",
        G + "machine.morgen": "Het bakje is leeg. Morgen ligt er weer een perfecte knabbel, njeg",
        G + "machine.knabbel": "Een perfecte knabbel! Nog warm. VAHOEG!",
        G + "beloning.knabbel": "Elke dag één perfecte knabbel uit jouw eigen Grote Knabbelmachine",
        G + "levering.nog": "  nog nodig: %1$s (%2$s/%3$s)",
        G + "levering.binnen": "  binnen: %1$s (%3$s/%3$s)",
        # the Uitvinder-guh
        "entity.guhs.guh_npc.uitvinderguh": "Uitvinder-guh",
        Q + "hallo1": "Njeg! Bezoek! Welkom in de Oude Guhrad-centrale. Vijf oude guhs rennen hier al eeuwen hun rondjes, en ik ben de Uitvinder-guh.",
        Q + "hallo2": "Maar mijn oefenhal... alles kapot! Iemand heeft aan alle knopjes gezeten. (Ik. Het was ik.) Help je me de vijf opstellingen "
                      "maken? Dan leer je meteen hoe vadskracht werkt. Hier, twee stukjes Guhdraad!",
        Q + "hint.draad": "Ga naar opstelling 1 in de oefenhal en leg Guhdraad in het gat (de gele tegel)",
        Q + "draad": "Opstelling 1: het rad rent, de oven slaapt. Daartussen mist een stukje Guhdraad. Leg het op de gele tegel en kijk naar "
                     "de oven: je leest meteen hoeveel vadskracht de opstelling gebruikt.",
        Q + "draad_weer": "Guhdraad kwijt? Njeg. Hier, nog eentje. Niet opeten, het smaakt naar stroom.",
        Q + "te_zwaar": f"Opstelling 2: één rad geeft {rad} vadskracht, maar twee ovens en een Vadsmolen vragen er {2 * oven + molen}. Te zwaar, "
                        "en dan staat ALLES stil. Knip een draad door bij de rode wol: haal er één machine af.",
        Q + "zout": "Voor de buizen heb ik zout nodig: %2$s zoutkristallen. Je hebt er nu %1$s. Zoek de Zoutkristalmijn, of zoutkristalerts in de rotsen, njeg!",
        Q + "zout_dank": "Zout! Vahoeg! Daar maak je buizen, filters en sensoren van. Kom, de zoutafdeling: opstelling 3.",
        Q + "hint.buis": "Opstelling 3: sluip en klik met een lege hand op het Richtingstuk, dan draait het om",
        Q + "buis": "Opstelling 3: de knabbels moeten van de volle ton naar de lege, maar het Richtingstuk wijst achterstevoren. Sluip en klik "
                    "erop met een lege hand: dan draait het om. (Oefenknabbels zijn van karton. Niet eten. Ik heb het geprobeerd.)",
        Q + "filter": "Opstelling 4: in de ton zitten oefenknabbels én propjes papier. Het Filterstuk moet alleen knabbels doorlaten, maar "
                      "het staat verkeerd: op zijn lijstje staat de knabbel, met 'alles behalve' erbij. Dus rolt alleen het papier door! Klik "
                      "erop en zet het op 'alleen deze'.",
        Q + "saus": "Opstelling 5, de lekkerste: de Sauspomp slurpt kaassaus op, maar tussen de pomp en het Sausvat mist een stuk Sausslang. "
                    "Leg het op de gele tegel en wacht tot er een emmer saus in het vat zit.",
        Q + "slang": "Hier is een Sausslang. Mijn laatste. Nou ja, mijn laatste van vandaag.",
        Q + "klaar1": "VAHOEG! De hele oefenhal doet het weer! Jij snapt vadskracht beter dan mijn vorige assistent. (Dat was een knabbel.)",
        Q + "klaar2": "Dit is voor jou: het Bodemloos Knabbelmaagje voor je Bank Guh, en mijn drie receptkaarten. Daarmee knutsel je pompen, "
                      "Knabbelaars, een Bezorgguhtje... Kijk in je questboek bij Guh-technologie!",
        Q + "hint.winkel": "Kaart kwijt? Sluip en klik op de Uitvinder-guh: hij verkoopt ze opnieuw",
        Q + "tip0": "Een blije guh rent harder. Laat hem eerst even op een Knuffelgenerator liggen, njeg!",
        Q + "tip1": "Te zwaar is te zwaar: dan staat alles stil. Kijk naar een draad en je leest wat er mis is.",
        Q + "tip2": "Een Bezorgguhtje haalt alleen op wat hij ook ergens kwijt kan. Slimmer dan ik, eerlijk gezegd.",
        Q + "tip3": "Ik werk aan iets groots. Het heeft oren. Meer zeg ik niet. Njeg.",
        Q + "km.eerst_mika": "Ik heb een plan voor de grootste machine ooit, maar zijn hart moet een gloeister zijn. Die heeft alleen de "
                             "Aangebrande Mika. Versla hem eerst (hij zit in het Mika-grillpaleis), dan praten we verder!",
        Q + "km.plan1": "Jij hebt de Aangebrande Mika verslagen?! Dan is het tijd. Kijk naar het bordes: daar bouwen wij DE GROTE "
                        "KNABBELMACHINE. Een guh van koper, negen blokken hoog, die elke dag één perfecte knabbel maakt.",
        Q + "km.plan2": "Ik heb er bergen spullen voor nodig. Zoveel dat je er een fabriek voor moet bouwen, njeg. Breng het gerust in "
                        "porties: ik tel alles. En alleen jij ziet jouw machine groeien.",
        Q + "km.dank": "Dank je! Dat waren er %s. Ik heb ze geteld. Twee keer.",
        Q + "km.fase1.vraag": "Eerst de fundering. Daar heb ik steen en hout voor nodig, heel veel:",
        Q + "km.fase1.klaar": "De fundering ligt! Recht, stevig en bijna waterpas. Vahoeg!",
        Q + "km.fase2.vraag": "Nu de ketel. Die stook ik met grillkool, en de lasnaden smeer ik in met gefrituurde kaasknabbels:",
        Q + "km.fase2.klaar": "De ketel staat! Hij borrelt al een beetje. Of dat was mijn buik.",
        Q + "km.fase3.vraag": "De maag. Daar gaat meel in, en een hele kluwen Guhdraad als zenuwen:",
        Q + "km.fase3.klaar": "De maag zit erin, met schoorstenen en al! Hij knort. Dat hoort zo.",
        Q + "km.fase4.vraag": "De snoet! Voor zijn ogen heb ik glas nodig, en Guhdrankjes om het email mooi roze te krijgen:",
        Q + "km.fase4.klaar": "Hij heeft een snoet! En oren! Kijk nou, hij slaapt nog. Njeg, wat lief.",
        Q + "km.fase5.vraag": "Het laatste: zijn hart. Een gloeister. En knabbels, voor zijn allereerste hap:",
        Q + "km.fase5.klaar": "Het hart gloeit... zijn ogen gaan open...",
        Q + "km.af1": "HIJ KNABBELT! VAHOEG! De Grote Knabbelmachine leeft! Kijk hem kauwen!",
        Q + "km.af2": "Elke dag ligt er één perfecte knabbel in het bakje, speciaal voor jou. En dit beeldje is voor op je kast. Vanaf nu ben "
                      "jij een echte Knabbelmachinist!",
        Q + "km.knabbel_klaar": "Psst: er ligt een perfecte knabbel voor je in het bakje van de machine. Njeg!",
    }
    for fase in LEVERINGEN:
        for naam, nl, _ in fase:
            t[G + "levering." + naam] = nl
    return t


def teksten(h):
    for key, text in _lang().items():
        h.lang(key, text, text)
    centrale = "De Oude Guhrad-centrale in de Guhbarbecuether"
    hal, werkplaats = "De oefenhal van de Oude Guhrad-centrale", "De werkplaats van de Uitvinder-guh"
    verhaal_motor.verhaallijn(
        h, "techniek", "De oefenhal van de Uitvinder-guh",
        "In de Oude Guhrad-centrale rennen vijf oude guhs nog altijd hun rondjes, maar de oefenhal van de Uitvinder-guh ligt in puin. "
        "Wie de vijf opstellingen maakt, snapt hoe vadskracht werkt.",
        stappen=[("Praat met de Uitvinder-guh", "Zoek de Oude Guhrad-centrale en praat met de Uitvinder-guh in zijn werkplaats.", centrale),
                 ("De losse draad", "Opstelling 1: leg Guhdraad in het gat tussen het rad en de Guhoven (de gele tegel).", hal),
                 ("Te zwaar!", "Opstelling 2: drie machines aan één rad is te veel. Knip een draad door bij de rode wol.", hal),
                 ("Zout voor de buizen", f"Breng de Uitvinder-guh {ZOUT} zoutkristallen (uit de Zoutkristalmijn of uit zoutkristalerts).", werkplaats),
                 ("De buis loopt achterstevoren", "Opstelling 3: sluip en klik met een lege hand op het Richtingstuk, zodat het omdraait.", hal),
                 ("Het filter staat verkeerd", "Opstelling 4: klik op het Filterstuk en zet het van 'alles behalve' op 'alleen deze'.", hal),
                 ("De slang is zoek", "Opstelling 5: leg de Sausslang in het gat tussen de pomp en het Sausvat en wacht op de saus.", hal),
                 ("Terug naar de Uitvinder-guh", "Vertel de Uitvinder-guh dat de hele oefenhal weer werkt.", werkplaats)],
        klaar=("De oefenhal werkt weer. Met de receptkaarten maak je de machines van de saus: kijk in je questboek bij Guh-technologie.", centrale),
        kort={"0": "Praat met de Uitvinder-guh", "1": "Opstelling 1: leg Guhdraad in het gat", "2": "Opstelling 2: knip een draad door",
              "3": f"Breng {ZOUT} zoutkristallen naar de Uitvinder-guh", "4": "Opstelling 3: draai het Richtingstuk om",
              "5": "Opstelling 4: zet het Filterstuk op 'alleen deze'", "6": "Opstelling 5: leg de Sausslang in het gat",
              "7": "Vertel het de Uitvinder-guh"})

    def breng(fase):
        return tech_quests_ftb.breng(fase).replace("Breng", "Breng de Uitvinder-guh", 1) + ". In porties mag: hij telt alles."
    verhaal_motor.verhaallijn(
        h, "knabbelmachine", "De Grote Knabbelmachine",
        "De Uitvinder-guh wil de grootste machine ooit bouwen: een guh van koper die elke dag één perfecte knabbel maakt. Hij heeft er "
        "zoveel spullen voor nodig dat je er een fabriek voor moet bouwen. Iedere speler bouwt zijn eigen machine.",
        stappen=[("Het grote plan", "Versla de Aangebrande Mika (in het Mika-grillpaleis) en praat daarna met de Uitvinder-guh.", centrale),
                 ("De fundering", breng(1), werkplaats), ("De ketel", breng(2), werkplaats), ("De maag", breng(3), werkplaats),
                 ("De snoet", breng(4), werkplaats), ("Het gloeisterhart", breng(5), werkplaats)],
        klaar=("De Grote Knabbelmachine knabbelt! Elke dag ligt er één perfecte knabbel voor je in het bakje onder zijn bek.", centrale),
        kort={"0": "Versla de Aangebrande Mika en praat met de Uitvinder-guh", "1": "Fundering: breng steen en boomstammen",
              "2": "Ketel: breng grillkool en gefrituurde kaasknabbels", "3": "Maag: breng knabbelmeel en Guhdraad",
              "4": "Snoet: breng Guhdrankjes en glas", "5": "Hart: breng een gloeister en kaasknabbels"})


def advancements(h):
    bbq2.zichtbaar(h, "techniek", "tech_quests_centrale", "root", "guhs:bank_upgrade", "goal", "De oefenhal draait weer",
                   "Maak de vijf opstellingen van de Uitvinder-guh in de Oude Guhrad-centrale")
    bbq2.zichtbaar(h, "techniek", "tech_quests_knabbelmachine", "tech_quests_centrale", "guhs:knabbelmachine_beeldje", "challenge", "Knabbelmachinist",
                   "Bouw je eigen Grote Knabbelmachine af bij de Uitvinder-guh")
    for name in ("tech_quests_perfecte_knabbel", "tech_quests_beeldje"):
        bbq2.verborgen(h, name)


# =====================================================================================================================
# build, self-check
# =====================================================================================================================
def _java(naam):
    return open(os.path.join(JAVA, naam), encoding="utf-8").read()


def selfcheck(h):
    A, D = h.A, h.D
    missing = []
    paden = [f"{A}/blockstates/grote_knabbelmachine.json", f"{A}/blockstates/knabbelmachine_beeldje.json", f"{A}/blockstates/techquest_knabbelmachine_deel.json",
             f"{A}/models/item/grote_knabbelmachine.json", f"{A}/models/item/knabbelmachine_beeldje.json", f"{D}/loot_table/blocks/grote_knabbelmachine.json",
             f"{D}/loot_table/blocks/knabbelmachine_beeldje.json", f"{D}/loot_table/chests/tech_quests_werkplaats.json",
             f"{A}/geckolib/models/entity/guh_npc_uitvinderguh.geo.json", f"{D}/structure/{bouw.NAAM}.nbt", f"{D}/structure/techquest_test_kamer.nbt",
             f"{D}/structure/techquest_test_draai.nbt", f"{D}/worldgen/structure/{bouw.NAAM}.json", f"{D}/worldgen/structure_set/{bouw.NAAM}.json",
             f"{D}/worldgen/structure_set/{bouw.NAAM}_gegarandeerd.json"]
    paden += [f"{A}/models/item/{i}.json" for i in ITEMS] + [os.path.join(h.TEX, "item", f"{i}.png") for i in ITEMS]
    missing += [p for p in paden if not os.path.exists(p)]
    pool = json.load(open(f"{D}/worldgen/template_pool/{bouw.NAAM}/start.json", encoding="utf-8"))
    if any(e["element"].get("element_type") != "guhs:grond_single_pool_element" for e in pool["elements"]):
        missing.append(f"{bouw.NAAM}: its start pool has no ground_level_delta")
    # every recipe of the Saus tier asks for its card, and nothing else does
    for kaart, namen in KAARTEN.items():
        for naam in namen:
            r = json.load(open(f"{D}/recipe/{naam}.json", encoding="utf-8"))
            if r.get("key", {}).get("X") != f"guhs:{kaart}" or not any("X" in rij for rij in r["pattern"]):
                missing.append(f"recipe {naam} does not ask for {kaart}")
    # the spots of the practice hall: the same numbers here and in Centrale.java
    java = _java("Centrale.java")
    for naam, plekken in bouw.PLEKKEN.items():
        m = re.search(r"static final BlockPos\[\] " + naam + r" = \{([^}]*)\}", java)
        in_java = [tuple(int(v) for v in g) for g in re.findall(r"pos\((-?\d+), (-?\d+), (-?\d+)\)", m.group(1))] if m else None
        if in_java != [tuple(p) for p in plekken]:
            missing.append(f"Centrale.{naam} = {in_java}, tech_quests_bouw.PLEKKEN = {plekken}")
    schoorstenen = [(bouw.X0 + 4, bouw.MAAT[1] - 3, bouw.Z0 - 4), (bouw.X1 - 4, bouw.MAAT[1] - 3, bouw.Z0 - 4)]
    m = re.search(r"SCHOORSTENEN = \{([^}]*)\}", java)
    if [tuple(int(v) for v in g) for g in re.findall(r"pos\((-?\d+), (-?\d+), (-?\d+)\)", m.group(1))] != schoorstenen:
        missing.append(f"Centrale.SCHOORSTENEN != {schoorstenen}")
    # the parts of the machine and their stages: the same here and in TechquestBlocks.Onderdeel
    m = re.search(r"enum Onderdeel \{\s*([^;]*);", _java("TechquestBlocks.java"))
    in_java = [(n.lower(), int(a), int(b)) for n, a, b in re.findall(r"([A-Z_]+)\((\d+), (\d+)\)", m.group(1))]
    if in_java != [(p, *modellen.FASEN[p]) for p in modellen.PARTS]:
        missing.append(f"TechquestBlocks.Onderdeel {in_java} != tech_quests_modellen.PARTS / FASEN")
    # the deliveries: the same names and amounts here and in Knabbelmachine.FASEN
    in_java = [(n, int(a)) for n, a in re.findall(r'new Levering\("([a-z_]+)",.*?(\d+)\)[,)]', _java("Knabbelmachine.java"))]
    if in_java != [(n, a) for fase in LEVERINGEN for n, _, a in fase]:
        missing.append(f"Knabbelmachine.FASEN {in_java} != tech_quests.LEVERINGEN")
    m = re.search(r"ZOUT = (\d+)", _java("UitvinderRol.java"))
    if int(m.group(1)) != ZOUT:
        missing.append(f"UitvinderRol.ZOUT = {m.group(1)}, tech_quests_ftb.ZOUT = {ZOUT}")
    # every text the Java side asks for exists
    lang = _lang()
    for naam in ("Centrale.java", "Knabbelmachine.java", "UitvinderRol.java", "TechquestBlocks.java", "TechquestFeature.java"):
        src = _java(naam)
        for key in re.findall(r'"((?:gui|quest|item|block)\.guhs\.[a-z0-9_.]+)"', src):
            if key.endswith("."):
                if not any(k.startswith(key) for k in lang):
                    missing.append(f"{naam}: no text starts with {key}")
                continue
            if key not in lang and key not in h.NL:
                missing.append(f"{naam}: text {key}")
        for staart in re.findall(r'Q \+ "([a-z0-9_.]+)"', src):
            if staart.endswith(".") or staart.endswith("tip") or staart.endswith("km.fase"):
                continue
            if "quest.guhs.techquest.uitvinder." + staart not in lang:
                missing.append(f"{naam}: text quest.guhs.techquest.uitvinder.{staart}")
    for o in ("draad", "te_zwaar", "buis", "filter", "saus"):      # (Centrale.Opstelling)
        for soort in ("gemaakt", "verder"):
            if f"gui.guhs.techquest.opstelling.{o}.{soort}" not in lang:
                missing.append(f"text gui.guhs.techquest.opstelling.{o}.{soort}")
    for fase in range(1, len(LEVERINGEN) + 1):
        for soort in ("vraag", "klaar"):
            if f"quest.guhs.techquest.uitvinder.km.fase{fase}.{soort}" not in lang:
                missing.append(f"text quest.guhs.techquest.uitvinder.km.fase{fase}.{soort}")
    if missing:
        raise SystemExit("tech_quests self-check failed:\n  " + "\n  ".join(str(m) for m in missing))


def build(h):
    textures(h)
    blokken_en_items(h)
    recepten(h)
    structuur(h)
    npc(h)
    teksten(h)
    advancements(h)
    selfcheck(h)
