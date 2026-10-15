"""
einde2 (the Guheinde expansion), slice zwevers: the Kruimelkwallen, the Leegtewalvisguh and the four element animals.
Java: nl.juiced.guhs.feature.einde2.zwevers (E2ZweversFeature).

  build(h)   the six creatures' models, animations and textures (einde2_zwevers_modellen), the kruimelkwal_pot (block
             states, two models, textures, loot: the glass bottle back, or the whole pot with silk touch), the leegteparel,
             the spawn eggs, the sounds, the Guhdex pages, the texts (Dutch in both files), the advancements, the game test
             rooms (einde2_zwevers_test_*) and a self-check
  ftb(fq)    the quests of the section "Zwevers & elementdieren" (nothing locked; no whale quests: those are the harbour
             pub's, a later phase)

The creatures: kruimelkwal (swarms in the Kruimelwolken; bounces you up; a glass bottle catches it -> kruimelkwal_pot, a
lamp), leegtewalvisguh (huge, sings, a back to land on, a knabbelkoorvrucht -> a leegteparel a day; only there for players
with the Walviszicht flag), and the element animals vleermuis_lemurguh (lucht), otter_pinguinguh (water; a sled),
das_molguh (aarde; blind, digs), schildpad_eendguh (vuur; swims, bread). Wild ones come by top-ups in Java (the Guheinde
spawns nothing by itself): no biome modifiers here. Nothing drops anything (they are lief).
"""
import math
import os

from PIL import Image

import features.einde2_kit as kit
from features import einde2_zwevers_modellen as modellen

BONES = {}
FTB_CHAPTER = "guhs_guheinde"
FTB_SECTION = "Zwevers & elementdieren"
FTB_PORTRAIT = "geo:kruimelkwal:kruimelkwal"

DIEREN = ["kruimelkwal", "leegtewalvisguh", "vleermuis_lemurguh", "otter_pinguinguh", "das_molguh", "schildpad_eendguh"]
ELEMENTDIEREN = DIEREN[2:]

# =====================================================================================================================
# texts (Dutch in both files)
# =====================================================================================================================
DEX = {  # page -> (name, rarity, info)
    "kruimelkwal": ("Kruimelkwal", "In zwermen in de Kruimelwolken",
                    "Een kwalletje van gloeiende kaaskruimels dat in zwermen door de Kruimelwolken dobbert. Het weegt niks, prikt niet "
                    "en is stiekem een trampoline: laat je erop vallen en BOING, je stuitert zo vijf blokjes omhoog (sluip, dan zak je "
                    "er zachtjes doorheen). Na een stuiter doet landen geen au. Schep er een op met een glazen fles en je hebt een "
                    "kruimelkwalpot: de gezelligste lamp van het Guheinde. Njeg!"),
    "leegtewalvisguh": ("Leegtewalvisguh", "Legendarisch, tussen de buiteneilanden (alleen voor wie de kapitein gelooft)",
                        "Reusachtig, traag en vadsig tevreden zweeft hij tussen de eilanden, en hij zingt zo laag dat je knabbels ervan "
                        "trillen. Op zijn rug groeit knabbelkoor en er is plek zat om te landen en een eindje mee te zweven. Voer hem een "
                        "knabbelkoorvrucht en hij bedankt je met een leegteparel (eentje per dag, hij is zuinig). Een eiland aan de lijn doe je "
                        "niet, maar wie de oude kapitein in de havenkroeg gelooft en een walvis vaak voert, krijgt er een voor zichzelf: "
                        "een vliegend thuis met een huisje op zijn rug."),
    "vleermuis_lemurguh": ("Vleermuis-lemurguh", "Zeldzaam in de Kruimelwolken (en bij de luchttempels)",
                           "Half vleermuis, half lemur, helemaal guh: enorme oren, een ringstaart en vliesvleugeltjes. Hij fladdert in "
                           "korte hupjes van plekje naar plekje en hangt het liefst ondersteboven aan een plafond te dutten, vleugels om "
                           "zich heen als een dekentje. Sluip, dan slaapt hij door. Dol op fruit: een appel, bessen of een "
                           "knabbelkoorvrucht. Let op je knabbels, zeggen de monniken. Njeg."),
    "otter_pinguinguh": ("Otter-pinguïnguh", "Zeldzaam op de Schimmelkaasvlakte (en bij de Waterstam)",
                         "Een pinguïnlijf met een ottersnoet, snorharen en vier flippers. Lopen gaat waggelend, maar op ijs en sneeuw "
                         "laat hij zich op zijn buik vallen en glijdt hij ervandoor. Bij de Waterstam mag je erop zitten: sleeën op een "
                         "otter-pinguïnguh! Sturen doe je door te kijken, afzetten met vooruit, remmen met achteruit. Hij vindt het zelf "
                         "het allerleukst. Lust graag een visje."),
    "das_molguh": ("Das-molguh", "Zeldzaam in de Kristalkaasklippen (en bij Omashguh)",
                   "Een lage bruine graver met een dassenstreep en flinke klauwen. Hij is blind en houdt zijn oogjes lekker dicht, maar "
                   "hij voelt elke stap die je zet: loop langs en hij steekt snuffelend zijn snoet jouw kant op (sluip, en hij merkt "
                   "niks). In zachte grond graaft hij zich in en tunnelt hij rond met alleen een streepje rug boven de grond. Een gat "
                   "maakt hij nooit: alles ligt daarna weer netjes. De eerste aardestuurders, zeggen ze in Omashguh. Dol op kaasknabbels."),
    "schildpad_eendguh": ("Schildpad-eendguh", "Zeldzaam in het Knabbelkoorbos (en op de paleisvijver)",
                          "Een geel eendje in een groen schildpadschild. Hij dobbert op het water, waggelt naar de dichtstbijzijnde "
                          "vijver als hij op het droge staat en kruipt bij schrik even in zijn schild. Hou een stuk brood vast en hij "
                          "volgt je overal; elk hapje is een tevreden kwak-njeg. In de Vuurnatie voert zelfs de chagrijnigste prins ze "
                          "stiekem."),
}

LANG = {
    "block.guhs.kruimelkwal_pot": "Kruimelkwalpot",
    "block.guhs.kruimelkwal_pot.lore": "Een Kruimelkwal in een pot: geeft licht. Breek de pot en hij zweeft weer vrij.",
    "entity.guhs.leegtewalvisguh_rug": "Rug van de Leegtewalvisguh",
    "item.guhs.leegteparel": "Leegteparel",
    "item.guhs.leegteparel.lore": "Een bedankje van de Leegtewalvisguh. Hou hem tegen je oor en je hoort hem zingen.",
    "item.guhs.kruimelkwal_spawn_egg": "Kruimelkwal-spawnei",
    "item.guhs.leegtewalvisguh_spawn_egg": "Leegtewalvisguh-spawnei",
    "item.guhs.vleermuis_lemurguh_spawn_egg": "Vleermuis-lemurguh-spawnei",
    "item.guhs.otter_pinguinguh_spawn_egg": "Otter-pinguïnguh-spawnei",
    "item.guhs.das_molguh_spawn_egg": "Das-molguh-spawnei",
    "item.guhs.schildpad_eendguh_spawn_egg": "Schildpad-eendguh-spawnei",
    # the whale
    "gui.guhs.einde2_zwevers.walvis.parel": "De Leegtewalvisguh zingt dankjewel en laat een leegteparel voor je achter!",
    "gui.guhs.einde2_zwevers.walvis.morgen": "Smakelijk, zingt de walvis. Zijn parel van vandaag heb je al: morgen weer, njeg.",
    # (einde2 phase B, reizen: an own whale steered into something big)
    "gui.guhs.einde2_zwevers.walvis.remt": "Je walvis remt af: er hangt iets groots voor zijn snoet. Kijk een andere kant op.",
    # op commands
    "gui.guhs.einde2_zwevers.commando.walviszicht_aan": "%s ziet nu de Leegtewalvisguh.",
    "gui.guhs.einde2_zwevers.commando.walviszicht_uit": "%s ziet de Leegtewalvisguh niet meer.",
    # sounds
    "subtitles.guhs.einde2_zwevers.kwal": "Kruimelkwal blubt",
    "subtitles.guhs.einde2_zwevers.kwal_stuiter": "Boing! (op een Kruimelkwal)",
    "subtitles.guhs.einde2_zwevers.kwal_pot": "Kruimelkwal glipt een pot in",
    "subtitles.guhs.einde2_zwevers.walvis_zang": "Leegtewalvisguh zingt",
    "subtitles.guhs.einde2_zwevers.walvis_hap": "Leegtewalvisguh hapt",
    "subtitles.guhs.einde2_zwevers.lemur": "Vleermuis-lemurguh piept",
    "subtitles.guhs.einde2_zwevers.lemur_vleugel": "Vleermuis-lemurguh fladdert",
    "subtitles.guhs.einde2_zwevers.otter": "Otter-pinguïnguh snatert",
    "subtitles.guhs.einde2_zwevers.otter_glij": "Otter-pinguïnguh glijdt",
    "subtitles.guhs.einde2_zwevers.das": "Das-molguh snuffelt",
    "subtitles.guhs.einde2_zwevers.das_graaf": "Das-molguh graaft",
    "subtitles.guhs.einde2_zwevers.eendguh": "Schildpad-eendguh kwaakt",
    "subtitles.guhs.einde2_zwevers.eendguh_schelp": "Schildpad-eendguh kruipt in zijn schild",
}

# vanilla sounds, pitched (the whale's song is one long horn note per event: the entity pitches every note itself)
SOUNDS = {
    "einde2_zwevers.kwal": [{"name": "minecraft:block.bubble_column.bubble_pop", "type": "event", "pitch": 1.4, "volume": 0.7},
                            {"name": "minecraft:entity.glow_squid.ambient", "type": "event", "pitch": 1.7, "volume": 0.5}],
    "einde2_zwevers.kwal_stuiter": [{"name": "minecraft:entity.slime.jump", "type": "event", "pitch": 1.3},
                                    {"name": "minecraft:block.slime_block.fall", "type": "event", "pitch": 1.5}],
    "einde2_zwevers.kwal_pot": [{"name": "minecraft:item.bottle.fill", "type": "event", "pitch": 1.2}],
    "einde2_zwevers.walvis_zang": [{"name": "minecraft:item.goat_horn.sound.1", "type": "event", "pitch": 0.8},
                                   {"name": "minecraft:item.goat_horn.sound.6", "type": "event", "pitch": 0.8},
                                   {"name": "minecraft:item.goat_horn.sound.7", "type": "event", "pitch": 0.8}],
    "einde2_zwevers.walvis_hap": [{"name": "minecraft:entity.strider.eat", "type": "event", "pitch": 0.6},
                                  {"name": "minecraft:entity.generic.eat", "type": "event", "pitch": 0.5}],
    "einde2_zwevers.lemur": [{"name": "minecraft:entity.bat.ambient", "type": "event", "pitch": 1.3, "volume": 0.5},
                             {"name": "minecraft:entity.fox.ambient", "type": "event", "pitch": 1.9, "volume": 0.6}],
    "einde2_zwevers.lemur_vleugel": [{"name": "minecraft:entity.bat.takeoff", "type": "event", "pitch": 1.2},
                                     {"name": "minecraft:entity.parrot.fly", "type": "event", "pitch": 1.4}],
    "einde2_zwevers.otter": [{"name": "minecraft:entity.dolphin.ambient", "type": "event", "pitch": 1.5, "volume": 0.6},
                             {"name": "minecraft:entity.fox.sniff", "type": "event", "pitch": 1.2}],
    "einde2_zwevers.otter_glij": [{"name": "minecraft:block.snow.step", "type": "event", "pitch": 0.8},
                                  {"name": "minecraft:block.powder_snow.step", "type": "event", "pitch": 0.9}],
    "einde2_zwevers.das": [{"name": "minecraft:entity.sniffer.sniffing", "type": "event", "pitch": 1.4, "volume": 0.6},
                           {"name": "minecraft:entity.armadillo.ambient", "type": "event", "pitch": 0.8}],
    "einde2_zwevers.das_graaf": [{"name": "minecraft:entity.sniffer.digging", "type": "event", "pitch": 1.3, "volume": 0.7},
                                 {"name": "minecraft:block.rooted_dirt.break", "type": "event", "pitch": 0.8}],
    # (the guh-eendje's own kwak files, a little lower: it has a shell to carry)
    "einde2_zwevers.eendguh": [{"name": f"guhs:waterdiertjes/kwak{i}", "pitch": 0.85} for i in (1, 2, 3)],
    "einde2_zwevers.eendguh_schelp": [{"name": "minecraft:entity.shulker.close", "type": "event", "pitch": 1.6, "volume": 0.5},
                                      {"name": "minecraft:entity.turtle.shamble", "type": "event", "pitch": 1.3}],
}

# spawn egg colours (base, spots): the creature's two main colours
EIEREN = {"kruimelkwal": (0xFFDE8C, 0xDE9C48), "leegtewalvisguh": (0x7668BE, 0xFCEAC6), "vleermuis_lemurguh": (0xEEE8E0, 0x604842),
          "otter_pinguinguh": (0x4A689C, 0xFAFAFC), "das_molguh": (0x846246, 0xF2E6D0), "schildpad_eendguh": (0xFCDA70, 0x6E9E58)}


def texts(h):
    for key, text in LANG.items():
        kit.lang(h, key, text)
    for page, (name, rarity, info) in DEX.items():
        kit.pagina(h, page, name, rarity, info)


# =====================================================================================================================
# textures of the jar and the pearl
# =====================================================================================================================
GLAS = (214, 238, 244)
KURK = (196, 150, 92)


def _pot_glas():
    """The jar's glass (16 x 16, see-through): a pale pane with a lighter frame and one glint."""
    img = Image.new("RGBA", (16, 16), GLAS + (70,))
    px = img.load()
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i), (i, 5), (i, 10), (5, i), (10, i)):
            px[x, y] = (236, 250, 252, 150)
    for (x, y) in ((6, 12), (7, 12), (6, 13), (12, 7), (12, 8)):
        px[x, y] = (255, 255, 255, 210)
    return img


def _pot_deksel(h):
    img = h.noise_tex(KURK, 12, kit.seed("zwevers/pot_deksel")).convert("RGBA")
    px = img.load()
    for i in range(16):
        px[i, 6] = px[6, i] = (150, 108, 60, 255)
        px[i, 9] = px[9, i] = (222, 182, 124, 255)
    return img


def _pot_kwal(h):
    """The kwal in the jar (32 x 32): crumb-cream all over, and where the little cube's sides sample it (u 12-20, v 22-30:
    the model's elements have no uv of their own, so a face takes the patch of its position) a tiny guh face."""
    img = h.noise_tex(modellen.KWAL, 10, kit.seed("zwevers/pot_kwal")).resize((32, 32), Image.NEAREST).convert("RGBA")
    px = img.load()
    rng = kit.rng("zwevers/pot_kwal")
    for _ in range(40):
        x, y = int(rng.integers(32)), int(rng.integers(32))
        px[x, y] = modellen.KRUIM + (255,) if rng.random() < 0.6 else modellen.KWAL_LICHT + (255,)
    for ex in (13, 17):
        for dx in (0, 1):
            for dy in (0, 1, 2):
                px[ex + dx, 24 + dy] = (24, 20, 38, 255)
        px[ex + 1, 24] = (255, 255, 255, 255)
        px[ex, 26] = (70, 150, 214, 255)
    for x in (12, 19):
        px[x, 28] = (255, 160, 190, 255)
    return img


def _pot_icoon():
    """The item: the jar from the front, the kwal looking out."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(4, 15):
        for x in range(3, 13):
            rand = x in (3, 12) or y in (4, 14)
            px[x, y] = (236, 250, 252, 255) if rand else GLAS + (150,)
    for y in (2, 3):
        for x in range(4, 12):
            px[x, y] = KURK + (255,) if y == 2 else (150, 108, 60, 255)
    for y in range(7, 13):
        for x in range(5, 11):
            px[x, y] = modellen.KWAL + (255,) if (x + y) % 5 else modellen.KRUIM + (255,)
    for x in (6, 9):
        px[x, 9] = px[x, 10] = (24, 20, 38, 255)
    px[6, 9] = px[9, 9] = (255, 255, 255, 255)
    px[5, 13] = px[7, 13] = px[9, 13] = modellen.KWAL_LICHT + (255,)
    px[11, 6] = px[11, 7] = (255, 255, 255, 230)
    return img


def _parel():
    """The leegteparel: a void-lilac pearl with a cream sheen and two star glints."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 8.0)
            if d > 5.6:
                continue
            licht = max(0.0, 1.0 - math.hypot(x - 5.5, y - 5.5) / 6.5)
            basis = modellen.WALVIS if d < 4.6 else modellen.WALVIS_DONKER
            px[x, y] = tuple(int(b + (c - b) * licht * 0.8) for b, c in zip(basis, modellen.WALVIS_BUIK)) + (255,)
    for (x, y) in ((5, 5), (6, 5), (5, 6), (10, 10)):
        px[x, y] = modellen.STER + (255,)
    px[4, 5] = px[5, 4] = (255, 255, 255, 255)
    return img


def textures(h):
    h.save(_pot_glas(), "block", "kruimelkwal_pot_glas.png")
    h.save(_pot_deksel(h), "block", "kruimelkwal_pot_deksel.png")
    h.save(_pot_kwal(h), "block", "kruimelkwal_pot_kwal.png")
    h.save(_pot_icoon(), "item", "kruimelkwal_pot.png")
    h.save(_parel(), "item", "leegteparel.png")


# =====================================================================================================================
# blocks, items, loot, sounds
# =====================================================================================================================
def _el(frm, to, tex, faces=("north", "south", "east", "west", "up", "down")):
    return {"from": frm, "to": to, "faces": {f: {"texture": tex} for f in faces}}


def _pot_model(dy, touw):
    """The jar: a glass body with a cork lid, the kwal (a little cube and its dangling bits) inside; hanging it sits a
    little higher on a cord."""
    els = [_el([5, dy, 5], [11, dy + 7, 11], "#glas"),
           _el([6, dy + 7, 6], [10, dy + 8.5, 10], "#deksel"),
           _el([6, dy + 2, 6], [10, dy + 6, 10], "#kwal"),
           _el([6.5, dy + 0.6, 6.5], [7.5, dy + 2, 7.5], "#kwal", ("north", "south", "east", "west", "down")),
           _el([8.5, dy + 0.6, 8.5], [9.5, dy + 2, 9.5], "#kwal", ("north", "south", "east", "west", "down"))]
    if touw:
        els.append(_el([7.5, dy + 8.5, 7.5], [8.5, 16, 8.5], "#deksel", ("north", "south", "east", "west")))
    return {"parent": "minecraft:block/block", "render_type": "minecraft:translucent", "ambientocclusion": False,
            "textures": {"glas": "guhs:block/kruimelkwal_pot_glas", "deksel": "guhs:block/kruimelkwal_pot_deksel",
                         "kwal": "guhs:block/kruimelkwal_pot_kwal", "particle": "guhs:block/kruimelkwal_pot_glas"},
            "elements": els}


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    w(f"{A}/models/block/kruimelkwal_pot.json", _pot_model(0, False))
    w(f"{A}/models/block/kruimelkwal_pot_hangend.json", _pot_model(2, True))
    w(f"{A}/blockstates/kruimelkwal_pot.json", {"variants": {"hanging=false": {"model": "guhs:block/kruimelkwal_pot"},
                                                              "hanging=true": {"model": "guhs:block/kruimelkwal_pot_hangend"}}})
    h.item_model("kruimelkwal_pot")
    h.item_model("leegteparel")
    # breaking the jar lets the kwal go (KruimelkwalPotBlock): you get the bottle back; silk touch keeps kwal and jar together
    silk = {"condition": "minecraft:match_tool", "predicate": {
        "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}
    w(f"{D}/loot_table/blocks/kruimelkwal_pot.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "guhs:kruimelkwal_pot", "conditions": [silk]},
            {"type": "minecraft:item", "name": "minecraft:glass_bottle"}]}]}]})
    for id, (basis, vlek) in EIEREN.items():
        kit.spawn_ei(h, id, LANG[f"item.guhs.{id}_spawn_egg"], basis, vlek)
    # nothing drops anything (they are lief, and nobody should want to hurt one)
    for id in DIEREN:
        w(f"{D}/loot_table/entities/{id}.json", {"type": "minecraft:entity", "pools": []})


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# advancements
# =====================================================================================================================
def advancements(h):
    for naam in ("einde2_zwevers_gestuiterd", "einde2_zwevers_walvisrug", "einde2_zwevers_walvis_gevoerd"):
        kit.verborgen(h, naam)


# =====================================================================================================================
# game test rooms
# =====================================================================================================================
def test_templates(h):
    B = h.ms.Byte
    # the room of most tests: a stone floor with a strip of packed ice, a patch of dirt, a little basin of water, and a
    # beam to hang under
    t = h.Structure((14, 8, 14))
    for x in range(14):
        for z in range(14):
            t.set(x, 0, z, "minecraft:smooth_stone")
    for x in range(1, 13):
        for z in (1, 2):
            t.set(x, 0, z, "minecraft:packed_ice")
    for x in range(1, 5):
        for z in range(9, 13):
            t.set(x, 0, z, "minecraft:dirt")
    for x in range(8, 13):
        for z in range(8, 13):
            rand = x in (8, 12) or z in (8, 12)
            t.set(x, 1, z, "minecraft:smooth_stone" if rand else "minecraft:water", None if rand else {"level": "0"})
    t.set(6, 5, 6, "minecraft:smooth_stone")
    t.save("einde2_zwevers_test_kamer")
    # creatures as a building places them (kit.Bouw.wezen writes exactly this): PersistenceRequired and Honk, the otter a sled
    t = h.Structure((11, 6, 11))
    for x in range(11):
        for z in range(11):
            t.set(x, 0, z, "minecraft:smooth_stone")
    for i, (id, extra) in enumerate((("vleermuis_lemurguh", {}), ("otter_pinguinguh", {"Slee": B(1)}), ("das_molguh", {}),
                                     ("schildpad_eendguh", {}), ("kruimelkwal", {}))):
        nbt = {"id": f"guhs:{id}", "PersistenceRequired": B(1), "Honk": 3, "Rotation": h.ms.floats(0.0, 0.0)}
        nbt.update(extra)
        x, z = (2, 2) if i == 0 else (8, 2) if i == 1 else (2, 8) if i == 2 else (8, 8) if i == 3 else (5, 5)
        t.entity(x + 0.5, 1 if id != "kruimelkwal" else 2, z + 0.5, nbt)
    t.save("einde2_zwevers_test_honk")


# =====================================================================================================================
# self-check
# =====================================================================================================================
def selfcheck(h):
    problems = modellen.check(h)
    for key, text in list(LANG.items()) + [(k, " ".join(v)) for k, v in DEX.items()]:
        low = text.lower()
        if "te vads" in low or "hamster" in low:
            problems.append(f"lore: {key}")
        for woord in ("laatste", "sterkste", "allerlaatste", "het einde van"):   # (build contract 1: open ends)
            if woord in low:
                problems.append(f"open ends: {key} says '{woord}'")
    for e in DIEREN:
        for key in (f"entity.guhs.{e}", f"item.guhs.{e}_spawn_egg", f"gui.guhs.guhdex.info.{e}", f"gui.guhs.guhdex.rarity.{e}"):
            if key not in h.NL:
                problems.append(f"missing lang {key}")
        for p in (f"{h.A}/geckolib/models/entity/{e}.geo.json", f"{h.A}/geckolib/animations/entity/{e}.animation.json",
                  f"{h.A}/models/item/{e}_spawn_egg.json", f"{h.D}/advancement/quest/seen_{e}.json", f"{h.D}/loot_table/entities/{e}.json"):
            if not os.path.exists(p):
                problems.append(f"missing {p}")
    for event in SOUNDS:
        if f"subtitles.guhs.{event}" not in h.NL:
            problems.append(f"missing subtitle {event}")
    for p in (f"{h.A}/models/block/kruimelkwal_pot.json", f"{h.A}/models/block/kruimelkwal_pot_hangend.json",
              f"{h.A}/blockstates/kruimelkwal_pot.json", f"{h.D}/loot_table/blocks/kruimelkwal_pot.json",
              f"{h.D}/structure/einde2_zwevers_test_kamer.nbt", f"{h.D}/structure/einde2_zwevers_test_honk.nbt"):
        if not os.path.exists(p):
            problems.append(f"missing {p}")
    if problems:
        raise SystemExit("einde2_zwevers self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    modellen.build(h)
    textures(h)
    blocks_and_items(h)
    sounds(h)
    texts(h)
    advancements(h)
    test_templates(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests: "Zwevers & elementdieren" (see / find / make; nothing locked; the whale's questline is a later phase)
# =====================================================================================================================
def ftb(fq):
    q, adv, item = fq.q, fq.adv, fq.item
    q("einde2_zwevers_kwal", "Kruimelkwallen", "Boven de &eKruimelwolken&r dobberen zwermen &6Kruimelkwallen&r: gloeiende kwalletjes van "
      "kaaskruimels. Kom er dichtbij voor je Guhdex. En laat je er eens bovenop vallen: &dBOING!&r (Sluip, dan zak je erdoorheen.)",
      "guhs:kruimelkwal_spawn_egg", [adv("seen_kruimelkwal")], rewards=(("minecraft:glass_bottle", 3),), shape="gear", xp=50)
    q("einde2_zwevers_kwal_pot", "Een lamp die terugkijkt", "Schep een Kruimelkwal op met een &bglazen fles&r: je krijgt een "
      "&6kruimelkwalpot&r, een lamp die je neerzet of ophangt. Breek je de pot, dan zweeft de kwal weer vrij rond (en blijft hij bij je).",
      "guhs:kruimelkwal_pot", [item("guhs:kruimelkwal_pot")], rewards=(("guhs:kaas_knabbels", 8),), xp=100)
    q("einde2_zwevers_vleermuis_lemurguh", "Ondersteboven dutten", "In de &eKruimelwolken&r fladdert heel af en toe een "
      "&dvleermuis-lemurguh&r: grote oren, ringstaart, vliesvleugels. Hij hangt het liefst ondersteboven te slapen. Sluip dichterbij "
      "voor je Guhdex en geef hem een appel.", "guhs:vleermuis_lemurguh_spawn_egg", [adv("seen_vleermuis_lemurguh")],
      rewards=(("minecraft:apple", 4),), xp=50)
    q("einde2_zwevers_otter_pinguinguh", "Buikglijden", "Op de &bSchimmelkaasvlakte&r waggelt soms een &dotter-pinguïnguh&r. Op ijs en "
      "sneeuw glijdt hij op zijn buik! Kom er dichtbij voor je Guhdex. Hij lust graag een visje.",
      "guhs:otter_pinguinguh_spawn_egg", [adv("seen_otter_pinguinguh")], rewards=(("minecraft:cod", 4),), xp=50)
    q("einde2_zwevers_das_molguh", "Hij voelt je aankomen", "In de &6Kristalkaasklippen&r graaft een enkele &ddas-molguh&r. Hij is "
      "blind, maar hij voelt je stappen (sluip!). In zachte grond tunnelt hij rond zonder ook maar één blok stuk te maken. Kom er "
      "dichtbij voor je Guhdex.", "guhs:das_molguh_spawn_egg", [adv("seen_das_molguh")], rewards=(("guhs:kaas_knabbels", 8),), xp=50)
    q("einde2_zwevers_schildpad_eendguh", "Kwak-njeg", "In het &5Knabbelkoorbos&r dobbert af en toe een &dschildpad-eendguh&r: een eendje "
      "in een schildpadschild. Hou &6brood&r vast en hij volgt je overal. Kom er dichtbij voor je Guhdex.",
      "guhs:schildpad_eendguh_spawn_egg", [adv("seen_schildpad_eendguh")], rewards=(("minecraft:bread", 4),), xp=50)
