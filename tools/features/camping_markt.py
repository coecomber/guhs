"""
bbq2 (camping-markt): the Grillcamping and the Nether-Mika-ruilmarkt. Java: feature/campingmarkt.

  Grillcamping          structure guhs:grillcamping, the Kampbaas-guh and the Houthakker-guh, questline "camping": pitch a
                        tent of your own on a free pitch and hammer in its four pegs, split fire wood on the chopping
                        block, light the big camp fire (the residents come and dance), roast three marshmallows golden
                        brown with the Roosterstok. Reward: the recipe card of the Plantagebak and the camping outfit.
  Nether-Mika-ruilmarkt structure guhs:mika_ruilmarkt, the Marktmeester-Mika, questline "ruilmarkt": learn haggling (read his
                        mood, pick the answer that fits), unmask the fake vads (one of five stacks is lighter: weigh them
                        two by two on the scales of the Waag and stamp the fake). Reward: the scales as a deco block and an
                        extra present at every barter with a Nether-Mika; the stall holders barter on the spot.
  tent canvas           guhs:campingmarkt_tentdoek_<kleur> (+ _trap, _plaat) in five colours: what the tents and the awnings
                        are made of, and a building block for players.

Everything is per player and nothing in either building changes for good: a pitched tent is folded up again, the fire burns
down, a split log is replaced, the stacks of vads never change (which one is the fake is drawn per player).

Split: camping_markt_bouw.py (the two buildings, the two pitch templates, their self-check and pictures),
camping_markt_tex.py (textures), camping_markt_modellen.py (block models and the four characters),
camping_markt_wiki.py (the wiki entries). Dutch only (CONTRACT_130 1); English names are proposed in the slice report.
"""
import json
import os

from features import bbq2, verhaal_motor, wereld
from features import camping_markt_bouw as bouw
from features import camping_markt_modellen as modellen
from features import camping_markt_tex as tex

KLEUREN = ["rood", "geel", "groen", "blauw", "creme"]
KLEURNAAM = {"rood": "Rood", "geel": "Geel", "groen": "Groen", "blauw": "Blauw", "creme": "Crèmekleurig"}
WOL = {"rood": "minecraft:red_wool", "geel": "minecraft:yellow_wool", "groen": "minecraft:green_wool", "blauw": "minecraft:light_blue_wool",
       "creme": "minecraft:white_wool"}
DOEK_BLOKKEN = [f"campingmarkt_tentdoek_{k}{s}" for k in KLEUREN for s in ("", "_trap", "_plaat")]
BLOKKEN = DOEK_BLOKKEN + ["campingmarkt_kampeerplek", "campingmarkt_haring", "campingmarkt_hakblok", "campingmarkt_kampvuur",
                          "campingmarkt_weegschaal", "campingmarkt_vadsstapel"]
ITEMS = ["campingmarkt_tentzak", "campingmarkt_brandhout", "campingmarkt_roosterstok", "campingmarkt_recept_plantagebak", "campingmarkt_keurstempel"]
# guh clothes: the camping outfit (the Kampbaas-guh's thank-you); they use bones the guh already has
CLOTHES = ["campingmarkt_hoedje", "campingmarkt_halsdoek", "campingmarkt_rugzak"]
# salts: CONTRACT_130 3, slice 13: 213013NN
SALT_CAMPING, SALT_MARKT = 21301301, 21301311
KAART = "guhs:campingmarkt_recept_plantagebak"

FTB_SECTIES = [("camping_markt_camping", "De Grillcamping", "npc:kampbaasguh",
                ["camping_markt_camping_vind", "camping_markt_camping_1", "camping_markt_camping_2", "camping_markt_camping_3", "camping_markt_camping_4",
                 "camping_markt_camping_5", "camping_markt_buren", "camping_markt_stok", "camping_markt_tentje"]),
               ("camping_markt_markt", "De Nether-Mika-ruilmarkt", "npc:marktmeester_mika", None)]


# =====================================================================================================================
# guh clothes
# =====================================================================================================================
def clothes(rng, v):
    return {
        "campingmarkt_hoedje": {"rain_hat": lambda: v.band((132, 142, 92), (240, 130, 176), rng, (10,))},
        "campingmarkt_halsdoek": {"scarf": lambda: v.band((236, 196, 72), (196, 60, 54), rng, (0, 27))},
        "campingmarkt_rugzak": {"backpack": lambda: v.band((98, 140, 80), (232, 190, 70), rng, (4, 22))},
    }


def icons(ic):
    hoed = ["....aaaaaaa.....", "...abbbbbbba....", "..abbbbbbbbba...", "..acccccccccca..", ".aaaaaaaaaaaaaa.", "abbbbbbbbbbbbbba",
            ".aaaaaaaaaaaaaa."]
    doek = ["aaaaaaaaaaaaaa..", "abbbbbbbbbbbba..", ".abbbbbbbbbba...", "..acbbbbbbca....", "...acbbbbca.....", "....acbbca......",
            ".....acca.......", "......aa........"]
    rugzak = ["...aaaaaaaa.....", "..acccccccca....", "..aaaaaaaaaa....", "..abbbbbbbba....", ".abbbbbbbbbba...", ".abbaccccabba...",
              ".abbacddcabba...", ".abbaccccabba...", ".abbbbbbbbbba...", ".abbbbbbbbbba...", "..aaaaaaaaaa...."]
    return {
        "campingmarkt_hoedje": ic.icon(ic.pad(hoed), {"a": (84, 92, 56), "b": (132, 142, 92), "c": (240, 130, 176)}),
        "campingmarkt_halsdoek": ic.icon(ic.pad(doek), {"a": (150, 110, 30), "b": (236, 196, 72), "c": (196, 60, 54)}),
        "campingmarkt_rugzak": ic.icon(ic.pad(rugzak), {"a": (58, 88, 48), "b": (98, 140, 80), "c": (232, 190, 70), "d": (240, 130, 176)}),
    }


# =====================================================================================================================
# textures of the items; the blocks' textures are written with their models (camping_markt_modellen)
# =====================================================================================================================
def textures(h):
    for name, rows in tex.ICONS.items():
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), f"camping_markt icon {name}: 16 x 16"
        h.save(h.grid(rows, tex.ITEM_PAL), "item", f"{name}.png")
    for i in ITEMS:
        h.item_model(i, parent="minecraft:item/handheld" if i == "campingmarkt_roosterstok" else "minecraft:item/generated")


# =====================================================================================================================
# loot, recipes, tags
# =====================================================================================================================
def recept_plantagebak(h):
    """The recipe of the Plantagebak needs the Kampbaas-guh's recipe card (it stays in the grid). The recipe itself is
    tech-machines': its module offers recept(h, naam, kaart=...) for this; until that slice is merged the same recipe is
    written here (RECEPTEN["plantagebak"] of tools/features/tech_machines.py, the card in its top-left cell)."""
    from features import tech_machines
    if hasattr(tech_machines, "recept"):
        tech_machines.recept(h, "plantagebak", kaart=KAART)
    else:
        h.shaped("plantagebak", ["XEL", "LBL", "LDL"], {"X": KAART, "L": "#minecraft:logs", "E": "minecraft:dirt", "B": "guhs:blubroom",
                                                       "D": "guhs:guh_wire"}, "guhs:plantagebak")


def data(h):
    D = h.D
    # blocks: tent canvas and the deco blocks drop themselves; the pitch sign and the stack of vads belong to their building
    for k in KLEUREN:
        n = f"campingmarkt_tentdoek_{k}"
        h.self_drop(n)
        h.self_drop(n + "_trap")
        h.w(f"{D}/loot_table/blocks/{n}_plaat.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": f"guhs:{n}_plaat", "functions": [
                {"function": "minecraft:set_count", "count": 2, "conditions": [{"condition": "minecraft:block_state_property",
                                                                                 "block": f"guhs:{n}_plaat", "properties": {"type": "double"}}]},
                {"function": "minecraft:explosion_decay"}]}]}]})
    for b in ("campingmarkt_hakblok", "campingmarkt_kampvuur", "campingmarkt_weegschaal", "campingmarkt_haring"):
        h.self_drop(b)
    for b in ("campingmarkt_kampeerplek", "campingmarkt_vadsstapel"):
        h.w(f"{D}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": []})
    # the barrels of the two buildings, and the Marktmeester's bargain of the day: modest
    loot = {
        "chests/campingmarkt_camping": (2, 4, [("guhs:marshmallow_knabbel", 5, 2, 4), ("guhs:kaas_knabbels", 5, 2, 6), ("minecraft:stick", 4, 2, 6),
                                               ("minecraft:string", 3, 1, 3), ("minecraft:lantern", 2, 1, 1), ("minecraft:campfire", 1, 1, 1),
                                               ("guhs:campingmarkt_tentdoek_groen", 2, 2, 4), ("minecraft:charcoal", 3, 1, 4)]),
        "chests/campingmarkt_markt": (2, 4, [("guhs:kaas_knabbels", 6, 2, 8), ("minecraft:paper", 3, 1, 3), ("minecraft:string", 3, 1, 3),
                                             ("minecraft:lantern", 2, 1, 1), ("guhs:pindascheutjes", 3, 1, 3), ("guhs:mosterdscheutjes", 3, 1, 3),
                                             ("guhs:campingmarkt_tentdoek_rood", 2, 2, 4), ("guhs:campingmarkt_tentdoek_creme", 2, 2, 4),
                                             ("guhs:vahoege_vads_ingot", 1, 1, 1)]),
        "gameplay/campingmarkt_koopje": (1, 1, [("guhs:kaas_knabbels", 6, 6, 10), ("guhs:marshmallow_knabbel", 4, 3, 5), ("minecraft:lantern", 3, 1, 2),
                                                ("guhs:campingmarkt_tentdoek_rood", 3, 4, 6), ("guhs:campingmarkt_tentdoek_creme", 3, 4, 6),
                                                ("guhs:gloeikool", 2, 1, 2), ("guhs:vahoege_vads_ingot", 1, 1, 1)]),
    }
    for table, (lo, hi, entries) in loot.items():
        h.w(f"{D}/loot_table/{table}.json", {"type": "minecraft:chest" if table.startswith("chests") else "minecraft:barter", "pools": [
            {"rolls": lo if lo == hi else {"type": "minecraft:uniform", "min": lo, "max": hi}, "entries": [
                {"type": "minecraft:item", "name": item, "weight": wgt, "functions": h.count_fn(a, b)} for item, wgt, a, b in entries]}]})
    # recipes: canvas from wool and string, its stairs and slabs, the camp fire and the chopping block as deco
    for k in KLEUREN:
        n = f"campingmarkt_tentdoek_{k}"
        h.shaped(n, ["WW", "WS"], {"W": WOL[k], "S": "minecraft:string"}, f"guhs:{n}", 4)
        h.shaped(n + "_trap", ["D  ", "DD ", "DDD"], {"D": f"guhs:{n}"}, f"guhs:{n}_trap", 4)
        h.shaped(n + "_plaat", ["DDD"], {"D": f"guhs:{n}"}, f"guhs:{n}_plaat", 6)
    h.shaped("campingmarkt_kampvuur", ["LLL", "LCL", "SSS"], {"L": "#minecraft:logs", "C": "minecraft:campfire", "S": "guhs:houtskoolsteen_stenen"},
             "guhs:campingmarkt_kampvuur")
    h.shaped("campingmarkt_hakblok", ["A", "L"], {"A": "minecraft:iron_axe", "L": "#minecraft:logs"}, "guhs:campingmarkt_hakblok")
    recept_plantagebak(h)
    # tags
    h.add_tag("minecraft/tags/block/stairs", [f"guhs:campingmarkt_tentdoek_{k}_trap" for k in KLEUREN])
    h.add_tag("minecraft/tags/block/slabs", [f"guhs:campingmarkt_tentdoek_{k}_plaat" for k in KLEUREN])
    h.add_tag("minecraft/tags/item/stairs", [f"guhs:campingmarkt_tentdoek_{k}_trap" for k in KLEUREN])
    h.add_tag("minecraft/tags/item/slabs", [f"guhs:campingmarkt_tentdoek_{k}_plaat" for k in KLEUREN])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:campingmarkt_hakblok", "guhs:campingmarkt_kampvuur", "guhs:campingmarkt_kampeerplek"])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:campingmarkt_weegschaal", "guhs:campingmarkt_haring", "guhs:campingmarkt_vadsstapel"])
    h.add_tag("guhs/tags/block/campingmarkt_tentdoek", [f"guhs:{b}" for b in DOEK_BLOKKEN])
    # the tent bag, the bundle of fire wood and the stamp are quest items: they never go into a Bank Guh or a helper's chest
    h.add_tag("guhs/tags/item/loaned", ["guhs:campingmarkt_tentzak", "guhs:campingmarkt_brandhout", "guhs:campingmarkt_keurstempel"])


# =====================================================================================================================
# worldgen: the two structures
# =====================================================================================================================
def structuren(h):
    problems, bouwsels = bouw.build_all(h)
    if problems:
        print("camping_markt geometry check found problems:\n  " + "\n  ".join(problems[:40]))
        raise SystemExit("camping_markt: fix the templates (see the geometry check above)")
    wereld.bbq_structuur(h, bouw.CAMPING, soort="grot", titel="Grillcamping",
                         tooltip="Een tentendorp van guhs rond een groot kampvuur, met de Kampbaas-guh en de Houthakker-guh (Guhbarbecuether)",
                         biomes=["satebos", "worstenwoud", "houtskoolvlakte"], salt=SALT_CAMPING, templates=[(bouw.CAMPING, 1)],
                         spacing=30, separation=12, gegarandeerd=dict(sector=7, min=250, max=900), grootte=30, vlak=8, hoogte=12)
    wereld.bbq_structuur(h, bouw.MARKT, soort="grot", titel="Nether-Mika-ruilmarkt",
                         tooltip="De markt van de Nether-Mika's: kraampjes rond de Waag, met de Marktmeester-Mika (Guhbarbecuether)",
                         biomes=wereld.BBQ, salt=SALT_MARKT, templates=[(bouw.MARKT, 1)],
                         spacing=32, separation=12, gegarandeerd=dict(sector=8, min=250, max=900), grootte=30, vlak=8, hoogte=12)
    # the centre jigsaw is in layer 0 and the pool says where the ground really is (the trick of fossiel_mijn_bouw): the
    # ground layer lands on the cave floor and the terrain is smoothed towards it
    for naam in bouwsels:
        def grond(pool, delta=bouw.G + 1):
            for e in pool["elements"]:
                el = {"element_type": "guhs:grond_single_pool_element"}
                el.update({k: v for k, v in e["element"].items() if k not in ("element_type", "ground_level_delta")})
                el["ground_level_delta"] = delta
                e["element"] = el
        h.patch_json(f"{h.D}/worldgen/template_pool/{naam}/start.json", grond)
    return bouwsels


# =====================================================================================================================
# texts
# =====================================================================================================================
G_ = "gui.guhs.campingmarkt."
Q_ = "quest.guhs.campingmarkt."
LANG = {
    # --- blocks and items ---
    **{f"block.guhs.campingmarkt_tentdoek_{k}": f"{KLEURNAAM[k]} tentdoek" for k in KLEUREN},
    **{f"block.guhs.campingmarkt_tentdoek_{k}_trap": f"{KLEURNAAM[k]} tentdoek (schuin)" for k in KLEUREN},
    **{f"block.guhs.campingmarkt_tentdoek_{k}_plaat": f"{KLEURNAAM[k]} tentdoekplaat" for k in KLEUREN},
    "block.guhs.campingmarkt_kampeerplek": "Kampeerplekbordje",
    "block.guhs.campingmarkt_haring": "Tentharing",
    "block.guhs.campingmarkt_hakblok": "Hakblok",
    "block.guhs.campingmarkt_kampvuur": "Groot kampvuur",
    "block.guhs.campingmarkt_kampvuur.lore": "Steek het aan met een vuursteen, doof het met een schep. Marshmallows erbij, njeg!",
    "block.guhs.campingmarkt_weegschaal": "Weegschaal van de Waag",
    "block.guhs.campingmarkt_weegschaal.lore": "Klik erop: hij weegt wat je in je twee handen hebt. De volste hand is het zwaarst.",
    "block.guhs.campingmarkt_vadsstapel": "Stapel vads",
    "item.guhs.campingmarkt_tentzak": "Tentzak",
    "item.guhs.campingmarkt_tentzak.lore": "Klik ermee op een bordje VRIJ op de Grillcamping: plof, een tent!",
    "item.guhs.campingmarkt_brandhout": "Bos brandhout",
    "item.guhs.campingmarkt_brandhout.lore": "Zelf gehakt. Hoort op het grote kampvuur van de Grillcamping",
    "item.guhs.campingmarkt_roosterstok": "Roosterstok",
    "item.guhs.campingmarkt_roosterstok.lore": "Houd rechtsklik ingedrukt op een brandend kampvuur en laat los als de marshmallow goudbruin is",
    "item.guhs.campingmarkt_roosterstok.lore2": "Te vroeg is koud, te laat is zwart. Je hebt marshmallowknabbels nodig. Njeg!",
    "item.guhs.campingmarkt_recept_plantagebak": "Receptkaart: Plantagebak",
    "item.guhs.campingmarkt_recept_plantagebak.lore": "Van de Kampbaas-guh: zo kweek je je eigen brandhout in een bak",
    "item.guhs.campingmarkt_recept_plantagebak.lore2": "Blijft gewoon in je werkbank liggen als je een Plantagebak maakt. Njeg!",
    "item.guhs.campingmarkt_keurstempel": "Keurstempel van de Marktmeester",
    "item.guhs.campingmarkt_keurstempel.lore": "Klik ermee op de stapel vads die volgens jou nep is. Eerst wegen!",
    "item.guhs.campingmarkt_hoedje": "Kampeerhoedje",
    "item.guhs.campingmarkt_halsdoek": "Padvindersdasje",
    "item.guhs.campingmarkt_rugzak": "Kampeerrugzak",
    "entity.guhs.campingmarkt_kraam_mika": "Kraam-Mika",
    "entity.guhs.campingmarkt_kraam_mika.0": "Saté-Mika",
    "entity.guhs.campingmarkt_kraam_mika.1": "Vads-Mika",
    "entity.guhs.campingmarkt_kraam_mika.2": "Kolen-Mika",
    "entity.guhs.guh_npc.kampbaasguh": "Kampbaas-guh",
    "entity.guhs.guh_npc.houthakkerguh": "Houthakker-guh",
    "entity.guhs.guh_npc.marktmeester_mika": "Marktmeester-Mika",
    # --- the camping: what the blocks say ---
    G_ + "plek.eerst_praten": "Dit plekje is vrij. Vraag de Kampbaas-guh in de receptie om een tentzak, njeg",
    G_ + "plek.geen_zak": "Je hebt geen tentzak bij je. De Kampbaas-guh heeft er nog een",
    G_ + "plek.bezet": "Hier staat net een tent. Zoek een ander bordje VRIJ (of wacht even)",
    G_ + "plek.haringen": "Jouw tent staat al! Sla de haringen vast: klik op de ijzeren pennen",
    G_ + "plek.tent": "Wat een knus tentje, njeg",
    G_ + "plek.klaar": "Hier zet iedereen zijn eigen tent op. De jouwe stond er al. Njeg, wat was hij mooi!",
    G_ + "plek.geen_ruimte": "Er staat iets in de weg op dit plekje",
    G_ + "plek.staat": "Plof! Je tent staat. Sla nu de vier haringen vast (klik erop)",
    G_ + "haring.eerst_tent": "Zet eerst je eigen tent op: klik met de tentzak op een bordje VRIJ",
    G_ + "haring.al": "Deze zit al vast (%s/%s)",
    G_ + "haring.tik": "Tik! Haring erin (%s/%s)",
    G_ + "haring.klaar": "Vier haringen vast: jouw tent staat als een huis. Vahoeg! Nu naar de Houthakker-guh",
    G_ + "hak.eerst_praten": "Vraag eerst de Houthakker-guh of je mag hakken, njeg",
    G_ + "hak.tjak": "Tjak! (%s/%s)",
    G_ + "hak.klaar": "Zes blokken! Je krijgt een bos brandhout: gooi hem op het grote kampvuur",
    G_ + "vuur.hout_later": "Dit hout is voor het kampvuurfeest. Nu nog niet, njeg",
    G_ + "vuur.feest": "WOESJ! Het kampvuur brandt en het feest begint. Vahoeg!",
    G_ + "vuur.opgepookt": "Je pookt het vuur op: het brandt weer lekker",
    G_ + "vuur.hout_in_hand": "Pak het brandhout van de Houthakker-guh in je hand en klik op het vuur",
    G_ + "vuur.uit": "Het kampvuur is uit. Zonder hout geen feest, njeg",
    G_ + "vuur.warm": "Lekker warm bij het vuur. Njeg",
    G_ + "stok.geen_vuur": "Houd de stok boven een brandend kampvuur (rechtsklik ingedrukt houden)",
    G_ + "stok.geen_marshmallow": "Er zit geen marshmallow op je stok. Je hebt marshmallowknabbels nodig",
    G_ + "stok.vuur_weg": "Je bent te ver van het vuur",
    G_ + "stok.bezig.koud": "De marshmallow is nog koud...",
    G_ + "stok.bezig.warm": "Hij wordt warm... lichtbruin...",
    G_ + "stok.bezig.goud": "GOUDBRUIN! Nu loslaten!",
    G_ + "stok.bezig.zwart": "Hij rookt! Te lang...",
    G_ + "stok.koud": "Nog koud. Houd hem langer boven het vuur",
    G_ + "stok.bijna": "Bijna! Nog net niet goudbruin. Iets langer",
    G_ + "stok.verkoold": "Njeg, verkoold! Je veegt de stok schoon: probeer het gewoon opnieuw",
    G_ + "stok.goud": "Mmm, goudbruin en plakkerig. Vahoeg!",
    G_ + "stok.goud_n": "Goudbruin! Mmm (%s/%s)",
    G_ + "stok.goud_klaar": "Drie goudbruine marshmallows! Vertel het de Kampbaas-guh",
    G_ + "nodig.hout": "Blokken gehakt op het hakblok",
    G_ + "nodig.goudbruin": "Goudbruin geroosterde marshmallows",
    G_ + "nodig.afdingen": "Knabbels van de prijs afgedongen",
    G_ + "beloning.pakje": "Het kampeerpakje: hoedje, padvindersdasje en rugzak",
    G_ + "beloning.extraatje": "Een extraatje bij elke ruil met een Nether-Mika",
    # --- the market: what the blocks say ---
    G_ + "weegschaal.leeg": "Leg iets in je handen: je gewone hand is de linkerschaal, je andere hand de rechter",
    G_ + "weegschaal.links": "Links is zwaarder: %s tegen %s",
    G_ + "weegschaal.rechts": "Rechts is zwaarder: %s tegen %s",
    G_ + "weegschaal.midden": "Precies even zwaar: %s tegen %s. Njeg!",
    G_ + "stapel.eerst_afdingen": "Afblijven! Vraag eerst de Marktmeester-Mika wat hier aan de hand is",
    G_ + "stapel.klaar": "Echt vads. Allemaal. De Marktmeester houdt ze nu goed in de gaten",
    G_ + "stapel.eerst_wegen": "Eerst wegen, dan stempelen! Klik (zonder stempel) op twee stapels",
    G_ + "stapel.echt": "Stapel %s is echt vads! De oplichter wisselt gauw de stapels om: weeg opnieuw",
    G_ + "stapel.ontmaskerd": "NEP! Stapel %s is een kaasknabbel in paars papier. Vertel het de Marktmeester!",
    G_ + "stapel.links": "Stapel %s ligt op de linkerschaal. Klik op een andere stapel voor de rechterschaal",
    G_ + "stapel.terug": "Stapel %s gaat weer van de schaal",
    G_ + "stapel.weeg.links": "De schaal zakt naar links: stapel %s is zwaarder dan stapel %s",
    G_ + "stapel.weeg.rechts": "De schaal zakt naar rechts: stapel %s is lichter dan stapel %s",
    G_ + "stapel.weeg.midden": "De schaal blijft recht: stapel %s en stapel %s zijn even zwaar",
    G_ + "schaal.uitleg": "Klik op twee stapels vads op de toonbank om ze te wegen. Stempel daarna de neppe",
    G_ + "ruil.extraatje": "De Marktmeester kent jou: je krijgt een extraatje!",
    G_ + "ruil.kraam": "Verkocht! Daar komt je verrassing. Mjauw!",
    # --- the op command (dev checks, AutoCheck) ---
    G_ + "commando.stand": "Camping-markt van %s: camping stap %s (haringen %s, hout %s, geroosterd %s), ruilmarkt stap %s (nep %s, wegingen %s)",
    G_ + "commando.gezet": "Camping-markt: gezet",
    # --- the Kampbaas-guh ---
    Q_ + "kampbaas.hallo1": "Njeg! Welkom op Grillcamping De Gloeiende Guh! Lekker warm hè? Vloerverwarming van de hele Barbecuether.",
    Q_ + "kampbaas.hallo2": "Wie hier wil blijven, zet zelf een tent op. Hier is een tentzak: zoek een bordje VRIJ op het gele gras bij de poort en klik erop!",
    Q_ + "kampbaas.tentzak_weer": "Tentzak kwijt? Njeg njeg. Ik had er nog eentje onder de balie. Hier!",
    Q_ + "kampbaas.tent": "Zoek een bordje VRIJ (het gele gras bij de poort) en klik erop met de tentzak. Alles bezet? Even wachten: tenten gaan vanzelf weer de zak in.",
    Q_ + "kampbaas.haringen": "Je tent staat! Nu nog %s haringen vastslaan: klik op de ijzeren pennen bij de hoeken van je tent.",
    Q_ + "kampbaas.hout": "Mooie tent! Nu een kampvuurfeest. Maar zonder hout geen vuur: vraag de Houthakker-guh bij de houtschuur om brandhout.",
    Q_ + "kampbaas.vuur": "Heb je het brandhout? Gooi het op het grote kampvuur in het midden. Dan begint het feest, vahoeg!",
    Q_ + "kampbaas.stok": "Wat een feest! Nu het lekkerste: marshmallows. Hier is mijn Roosterstok en een zakje marshmallows. Rooster er %s goudbruin!",
    Q_ + "kampbaas.stok_uitleg": "Houd rechtsklik ingedrukt op het brandende kampvuur en laat los als hij GOUDBRUIN is. Te vroeg is koud, te laat is zwart. Njeg!",
    Q_ + "kampbaas.marshmallows_weer": "Marshmallows op? Hier zijn er nog vier. Nog %s goudbruine te gaan!",
    Q_ + "kampbaas.rooster": "Nog %s goudbruine marshmallows! Is het vuur uit? Klik erop, dan pook je het weer op.",
    Q_ + "kampbaas.klaar1": "Drie goudbruine! Jij bent een echte kampeerguh. VAHOEG!",
    Q_ + "kampbaas.klaar2": "Dit is voor jou: het kampeerpakje voor je guh, en mijn receptkaart van de Plantagebak. Daar kweek ik mijn eigen brandhout in!",
    Q_ + "kampbaas.recept_weer": "Receptkaart kwijt? Gelukkig heb ik hem overgetekend. Hier, njeg!",
    Q_ + "kampbaas.tip0": "Met de Roosterstok rooster je overal marshmallows: boven elk brandend kampvuur, ook overdag.",
    Q_ + "kampbaas.tip1": "Van tentdoek bouw je thuis je eigen tent. De schuine stukken zijn het dak, njeg.",
    Q_ + "kampbaas.tip2": "Een Plantagebak heeft vadskracht nodig. Vraag de Uitvinder-guh in de Oude Guhrad-centrale maar hoe dat zit.",
    Q_ + "kampbaas.tip3": "Regel één van de camping: wie het laatst wakker is, zet koffie. Regel twee: ik ben nooit het laatst wakker.",
    # --- the Houthakker-guh ---
    Q_ + "houthakker.te_vroeg": "Tjak! Tjak! ...O, hallo. Ik heb het druk met hakken. Ga eerst maar langs de Kampbaas-guh, njeg.",
    Q_ + "houthakker.hallo1": "Brandhout voor het kampvuur? Dat heb ik! ...in hele stammen. En mijn pootjes zijn moe van het hakken.",
    Q_ + "houthakker.hallo2": "Hak jij zes blokken op mijn hakblok? Gewoon erop klikken: tjak! Dan bind ik er een mooie bos van.",
    Q_ + "houthakker.nog": "Nog %s blokken! Tjak tjak. Niet in je eigen staart hakken.",
    Q_ + "houthakker.hout_weer": "Bos hout kwijt? Hoe raak je nou een bos hout kwijt... Hier, een nieuwe.",
    Q_ + "houthakker.naar_vuur": "Daar is je bos brandhout! Hup, naar het grote kampvuur ermee.",
    Q_ + "houthakker.tip0": "Satéhout brandt het lekkerst. Worsthout ruikt het lekkerst. Sparrenhout is gewoon hout.",
    Q_ + "houthakker.tip1": "Ik hak al mijn hele leven. Eén keer raak geslagen op mijn duim. Sindsdien draag ik een muts. Helpt niks, maar staat goed.",
    Q_ + "houthakker.tip2": "Wil je thuis ook een hakblok? Een bijl op een stam, klaar. Tjak!",
    Q_ + "houthakker.tip3": "De Kampbaas-guh kweekt zijn hout in een bak. Een BAK! Dat noem ik geen bos, njeg.",
    # --- hints (after "Volgende stap:") ---
    Q_ + "hint.tent": "Klik met de tentzak op een bordje VRIJ bij de poort van de camping",
    Q_ + "hint.hak": "Klik zes keer op een hakblok bij de houtschuur",
    Q_ + "hint.marshmallows": "Haal de Roosterstok bij de Kampbaas-guh",
    Q_ + "hint.rooster": "Rooster drie marshmallows goudbruin boven het grote kampvuur",
    Q_ + "hint.kampbaas": "Vertel het de Kampbaas-guh in de receptie",
    Q_ + "hint.afdingen": "Praat met de Marktmeester-Mika en ding af: compliment bij opscheppen, laag bieden bij zuchten, weglopen bij grommen",
    Q_ + "hint.wegen": "Weeg de vijf stapels vads in de Waag (klik op twee stapels) en stempel de lichtste",
    Q_ + "hint.marktmeester": "Vertel het de Marktmeester-Mika",
    # --- the residents of the camping ---
    Q_ + "kampeerder.0": "Njeg! Kamperen is het leukste wat er is. Vooral het eten.",
    Q_ + "kampeerder.1": "Mijn luchtbed is lek. Gelukkig ben ik zelf zacht.",
    Q_ + "kampeerder.2": "Heb jij ook zo'n zin in marshmallows? Ik heb ALTIJD zin in marshmallows.",
    Q_ + "kampeerder.3": "De Kampbaas-guh fluit elke ochtend om zes uur. Niemand weet waarom.",
    Q_ + "kampeerder.4": "Vannacht hoorde ik geritsel bij mijn tent. Het was mijn eigen buik.",
    Q_ + "kampeerder.5": "Als het kampvuur brandt, gaan we allemaal dansen. Vahoeg!",
    # --- the Marktmeester-Mika ---
    Q_ + "marktmeester.hallo1": "Mjauw! Welkom op de Nether-Mika-ruilmarkt. Ik ben de Marktmeester. Niet aan de staven vads zitten!",
    Q_ + "marktmeester.hallo2": "Er betaalt hier iemand met NEPVADS. Help je mij zoeken? Dan moet je eerst kunnen afdingen, anders lacht de hele markt je uit. Praat nog eens met me!",
    Q_ + "marktmeester.geleerd1": "Zo doe je dat! Schept een Mika op: geef een compliment. Zucht hij: bied laag. Gromt hij: loop weg. Onthouden, mjauw.",
    Q_ + "marktmeester.geleerd2": "Hier is mijn Keurstempel. In de Waag liggen vijf stapels vads: één is nep, en nepvads is LICHTER. Weeg ze en stempel de neppe!",
    Q_ + "marktmeester.weeg": "Klik in de Waag op twee stapels: de schaal laat zien welke lichter is. Weet je het zeker? Klik dan met de Keurstempel op de neppe stapel.",
    Q_ + "marktmeester.stempel_weer": "Stempel kwijt? Mjauw mjauw. Hier is een nieuwe. Niet op je voorhoofd zetten!",
    Q_ + "marktmeester.klaar1": "NEP! Een kaasknabbel in paars papier! Dat is het werk van mijn neefje Smikkel... die heeft de echte staaf opgegeten. Ik regel het wel.",
    Q_ + "marktmeester.klaar2": "Jij krijgt deze weegschaal. En ik vertel het rond: voortaan geeft elke Nether-Mika jou een extraatje bij het ruilen. Mjauw!",
    Q_ + "marktmeester.koopje_vraag": "Zin om af te dingen? Win je, dan is het koopje van de dag voor jou!",
    Q_ + "marktmeester.koopje": "Verkocht! Het koopje van de dag is van jou. Morgen heb ik weer een nieuw, mjauw.",
    Q_ + "marktmeester.tip0": "Op mijn markt ruilen de kraam-Mika's meteen: een staaf vahoege vads erin, een verrassing eruit. Geen gesnuffel.",
    Q_ + "marktmeester.tip1": "Echt vads is zwaar. Zo zwaar als een vadsige guh na het ontbijt. Nepvads weegt niks, mjauw.",
    Q_ + "marktmeester.tip2": "Elke dag heb ik één koopje voor wie goed afdingt. Vandaag heb je het al gehad. Morgen weer!",
    Q_ + "marktmeester.tip3": "Mijn neefje Smikkel moet nu een week lang de markt vegen. Met zijn staart.",
    # --- haggling: begin / goed / fout x his three moods (0 brags, 1 sighs, 2 growls); %s = the price, his patience ---
    Q_ + "afdingen.begin.0": "Kijk dan, wat een markt! Eigen pootjes gebouwd, de mooiste van de hele Barbecuether. Een marktbewijs kost %s knabbels. (Geduld: %s)",
    Q_ + "afdingen.begin.1": "Zucht... zo stil vandaag. Niemand koopt wat. Een marktbewijs kost %s knabbels. Of... ach. (Geduld: %s)",
    Q_ + "afdingen.begin.2": "Grrr! %s knabbels voor een marktbewijs. Dat is mijn laatste bod! Echt! Mijn allerlaatste! (Geduld: %s)",
    Q_ + "afdingen.goed.0": "Hmpf, goed gespeeld. %s knabbels dan. En zie je mijn hoed? De hoogste hoed van de markt, zelf uitgezocht! (Geduld: %s)",
    Q_ + "afdingen.goed.1": "Mjauw, slim. %s knabbels dan. Zucht... en ik moet nog zeven Mika-kindjes eten geven. Zeven! (Geduld: %s)",
    Q_ + "afdingen.goed.2": "Vooruit, %s knabbels. Maar lager ga ik NIET! Nooit! Dit is echt mijn laatste bod! Grrr! (Geduld: %s)",
    Q_ + "afdingen.fout.0": "Nee nee, zo werkt dat niet! Het blijft %s. Ik ben hier de baas van de mooiste markt die er is, hoor! (Geduld: %s)",
    Q_ + "afdingen.fout.1": "Fout gegokt. Het blijft %s. Zucht... wat een dag. En mijn pootjes doen ook al zo'n zeer. (Geduld: %s)",
    Q_ + "afdingen.fout.2": "Mis! Het blijft %s knabbels. Grrr, mijn laatste bod, en daar blijf ik bij! (Geduld: %s)",
    Q_ + "afdingen.gewonnen": "%s knabbels?! Mjauw... verkocht. Weet je wat: voor zo'n goede afdinger is het gratis. Jij kunt het!",
    Q_ + "afdingen.weggestuurd": "WEGWEZEN! Mijn geduld is op! ...Kom straks maar terug, dan beginnen we gewoon opnieuw. Mjauw.",
    Q_ + "afdingen.optie.compliment": "Wat een prachtige markt (en hoed)!",
    Q_ + "afdingen.optie.laag": "Ik bied de helft. Njeg!",
    Q_ + "afdingen.optie.weglopen": "Dan ga ik maar weer...",
    Q_ + "afdingen.optie.deal": "Deal! Poot erop.",
    # --- the stall holders: stall 0 sate, 1 vads, 2 coals ---
    Q_ + "kraam.0.0": "Mjauw! Verse saté, vanochtend nog geplukt! Voor één staaf vahoege vads krijg je een verrassing.",
    Q_ + "kraam.0.1": "Proeven mag niet. Ruiken wel. Ruiken kost niks. Nog niet.",
    Q_ + "kraam.0.2": "Heb je vahoege vads bij je? Houd hem maar omhoog, dan doen we zaken!",
    Q_ + "kraam.1.0": "Vads! Echt vads! Zwaar vads! Bij mij geen nep, vraag maar aan de Marktmeester.",
    Q_ + "kraam.1.1": "Een staaf vads erin, een verrassing eruit. Zo werkt ruilen, mjauw.",
    Q_ + "kraam.1.2": "Niet de kaarsjes uitblazen. Die zijn voor de sfeer.",
    Q_ + "kraam.2.0": "Gloeikooltjes! Grillkool! Alles lekker warm. Niet in je zak stoppen.",
    Q_ + "kraam.2.1": "Voor een staaf vahoege vads mag je in mijn verrassingszak graaien.",
    Q_ + "kraam.2.2": "Mijn kraam is de warmste van de hele markt. Dat is geen reclame, dat is een waarschuwing.",
}


def teksten(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    camping = "De Grillcamping in de Guhbarbecuether"
    verhaal_motor.verhaallijn(
        h, "camping", "Kamperen bij De Gloeiende Guh",
        "Op de Grillcamping wonen guhs in tentjes rond een groot kampvuur. Wie wil blijven, zet zelf een tent op.",
        stappen=[("Praat met de Kampbaas-guh", "Zoek de Grillcamping en praat met de Kampbaas-guh in de receptie.", camping),
                 ("Zet je tent op", "Klik met de tentzak op een bordje VRIJ en sla de vier haringen van je tent vast (klik op de ijzeren pennen).",
                  "De kampeerplekjes bij de poort van de Grillcamping"),
                 ("Hak brandhout", "Vraag de Houthakker-guh om brandhout en hak zes blokken op zijn hakblok.", "De houtschuur van de Grillcamping"),
                 ("Het kampvuurfeest", "Gooi de bos brandhout op het grote kampvuur.", "Het grote kampvuur, midden op de Grillcamping"),
                 ("Rooster marshmallows", "Haal de Roosterstok bij de Kampbaas-guh, rooster drie marshmallows goudbruin boven het kampvuur en vertel het hem.",
                  "Het grote kampvuur van de Grillcamping")],
        klaar=("Je bent een echte kampeerguh! Met de Roosterstok rooster je overal marshmallows, en met de receptkaart maak je een Plantagebak.", camping),
        kort={"0": "Praat met de Kampbaas-guh", "1": "Zet je tent op en sla de vier haringen vast", "2": "Hak zes blokken bij de Houthakker-guh",
              "3": "Gooi het brandhout op het grote kampvuur", "4": "Rooster drie marshmallows goudbruin"})
    markt = "De Nether-Mika-ruilmarkt in de Guhbarbecuether"
    verhaal_motor.verhaallijn(
        h, "ruilmarkt", "De nepvads van de ruilmarkt",
        "Op de Nether-Mika-ruilmarkt betaalt iemand met nepvads. De Marktmeester-Mika zoekt een slimme afdinger om hem te helpen.",
        stappen=[("Praat met de Marktmeester-Mika", "Zoek de Nether-Mika-ruilmarkt en praat met de Marktmeester-Mika voor de Waag.", markt),
                 ("Leer afdingen", "Praat met de Marktmeester en ding zijn prijs af van 30 naar 10 knabbels: een compliment als hij opschept, laag bieden "
                                   "als hij zucht, weglopen als hij gromt.", "De Marktmeester-Mika, voor de Waag"),
                 ("Ontmasker de nepvads", "Weeg de vijf stapels vads in de Waag (klik op twee stapels) en klik met de Keurstempel op de stapel die "
                                          "lichter is.", "De Waag, midden op de ruilmarkt"),
                 ("Vertel het de Marktmeester", "Vertel de Marktmeester-Mika welke stapel nep was.", "De Marktmeester-Mika, voor de Waag")],
        klaar=("De nepvads is ontmaskerd! Elke Nether-Mika geeft je voortaan een extraatje bij het ruilen, en elke dag kun je afdingen om het "
               "koopje van de dag.", markt),
        kort={"0": "Praat met de Marktmeester-Mika", "1": "Ding af bij de Marktmeester-Mika", "2": "Weeg de stapels vads en stempel de neppe",
              "3": "Vertel het de Marktmeester-Mika"})


def advancements(h):
    bbq2.zichtbaar(h, "barbecuether", "camping_markt_camping", "binnen", "guhs:campingmarkt_roosterstok", "goal", "Kampeerguh",
                   "Zet je tent op, vier het kampvuurfeest en rooster marshmallows op de Grillcamping")
    bbq2.zichtbaar(h, "barbecuether", "camping_markt_ruilmarkt", "binnen", "guhs:campingmarkt_weegschaal", "goal", "Meester-afdinger",
                   "Leer afdingen en ontmasker de nepvads op de Nether-Mika-ruilmarkt")
    for name in ("camping_markt_goudbruin", "camping_markt_kampeerder", "camping_markt_afgedongen", "camping_markt_koopje", "camping_markt_extraatje",
                 "camping_markt_kraam"):
        bbq2.verborgen(h, name)


# =====================================================================================================================
# build, self-check
# =====================================================================================================================
def selfcheck(h):
    A, D = h.A, h.D
    missing = []
    for b in BLOKKEN:
        for path in (f"{A}/blockstates/{b}.json", f"{A}/models/item/{b}.json", f"{D}/loot_table/blocks/{b}.json"):
            if not os.path.exists(path):
                missing.append(path)
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in ITEMS:
        if not os.path.exists(f"{A}/models/item/{i}.json") or not os.path.exists(f"{A}/textures/item/{i}.png"):
            missing.append(f"item {i}")
        if f"item.guhs.{i}" not in h.NL:
            missing.append(f"lang item.guhs.{i}")
    for c in CLOTHES:
        if f"item.guhs.{c}" not in h.NL:
            missing.append(f"lang item.guhs.{c}")
    for f in sorted(os.listdir(f"{A}/models/block")):
        if f.startswith("campingmarkt_"):
            model = json.load(open(f"{A}/models/block/{f}", encoding="utf-8"))
            for t in model.get("textures", {}).values():
                if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                    missing.append(f"texture {t} ({f})")
            for e in model.get("elements", []):
                if min(e["from"]) < -16 or max(e["to"]) > 32:
                    missing.append(f"{f}: an element outside -16..32")
    for naam in (bouw.CAMPING, bouw.MARKT):
        pool = json.load(open(f"{D}/worldgen/template_pool/{naam}/start.json", encoding="utf-8"))
        if any(e["element"].get("element_type") != "guhs:grond_single_pool_element" for e in pool["elements"]):
            missing.append(f"{naam}: its start pool has no ground_level_delta")
        for path in (f"{D}/structure/{naam}.nbt", f"{D}/worldgen/structure/{naam}.json", f"{D}/worldgen/structure_set/{naam}.json",
                     f"{D}/worldgen/structure_set/{naam}_gegarandeerd.json"):
            if not os.path.exists(path):
                missing.append(path)
    for naam in ("campingmarkt_plek", "campingmarkt_tent", "campingmarkt_test_kamer"):
        if not os.path.exists(f"{D}/structure/{naam}.nbt"):
            missing.append(f"template {naam}")
    for kind in ("kampbaasguh", "houthakkerguh", "marktmeester_mika"):
        if not os.path.exists(f"{A}/geckolib/models/entity/guh_npc_{kind}.geo.json"):
            missing.append(f"model of {kind}")
    if not os.path.exists(f"{A}/geckolib/models/entity/campingmarkt_kraam_mika.geo.json"):
        missing.append("model of the stall holders")
    recept = f"{D}/recipe/plantagebak.json"
    if not os.path.exists(recept) or KAART not in open(recept, encoding="utf-8").read():
        missing.append("the recipe of the Plantagebak without the Kampbaas-guh's recipe card")
    # every text the Java side sends exists (the keys it builds from numbers)
    for key in ([Q_ + f"kampeerder.{i}" for i in range(6)] + [Q_ + f"kraam.{k}.{i}" for k in range(3) for i in range(3)]
                + [Q_ + f"afdingen.{w}.{s}" for w in ("begin", "goed", "fout") for s in range(3)]
                + [Q_ + f"{wie}.tip{i}" for wie in ("kampbaas", "houthakker", "marktmeester") for i in range(4)]):
        if key not in h.NL:
            missing.append(f"lang {key}")
    if missing:
        raise SystemExit("camping_markt self-check failed:\n  " + "\n  ".join(missing))


def build(h):
    textures(h)
    modellen.build(h)
    data(h)
    structuren(h)
    teksten(h)
    advancements(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (chapter guhs_barbecuether, two sections)
# =====================================================================================================================
def ftb(fq):
    q, item, adv, structure = fq.q, fq.item, fq.adv, fq.structure
    q("camping_markt_camping_vind", "Tentjes in de as", "Ergens in de Guhbarbecuether staat een heel dorp van tentjes rond een groot kampvuur: "
      "de &6Grillcamping&r. Het superkompas (Barbecue > Grillcamping) wijst de weg.",
      "guhs:campingmarkt_tentdoek_groen_trap", [structure("grillcamping")], rewards=(("guhs:kaas_knabbels", 12),), deps=["bbq_dimensie"],
      shape="hexagon", xp=100)
    wereld.ftb_questlijn(fq, "camping_markt", "camping", [
        ("Welkom op de camping", "Praat met de &dKampbaas-guh&r in de receptie (het huisje met het puntdak). Je krijgt een &6tentzak&r van hem.",
         "guhs:campingmarkt_tentzak"),
        ("Plof, een tent!", "Klik met de tentzak op een bordje &6VRIJ&r (op het gele gras bij de poort) en sla daarna de vier &6haringen&r van je tent "
                            "vast: klik op de ijzeren pennen. Staat er overal al een tent? Die gaan na een paar minuten vanzelf weer de zak in.",
         "guhs:campingmarkt_haring"),
        ("Tjak! Tjak!", "Vraag de &dHouthakker-guh&r bij de houtschuur om brandhout en klik zes keer op een &6hakblok&r. Je krijgt een bos brandhout.",
         "guhs:campingmarkt_hakblok"),
        ("Het kampvuurfeest", "Gooi de bos brandhout op het &6grote kampvuur&r midden op de camping. De kampeerguhs komen erbij zitten en gaan dansen!",
         "guhs:campingmarkt_kampvuur"),
        ("Goudbruin!", "Haal de &6Roosterstok&r bij de Kampbaas-guh. Houd rechtsklik ingedrukt op het brandende kampvuur en laat los als de marshmallow "
                       "&6goudbruin&r is (te vroeg is koud, te laat is zwart: dan probeer je het gewoon opnieuw). Drie goudbruine, en vertel het hem: "
                       "je krijgt het &dkampeerpakje&r en de &6receptkaart van de Plantagebak&r.", "guhs:campingmarkt_roosterstok")],
        na=["camping_markt_camping_vind"], eind=(("guhs:marshmallow_knabbel", 8),))
    q("camping_markt_buren", "Kennismaken met de buren", "Op de camping wonen vijf kampeerguhs. Klik er eentje aan voor een praatje. (Ze horen bij de "
      "camping: temmen kan niet.)", "guhs:guh_slaapzak", [adv("camping_markt_kampeerder")], rewards=(("guhs:kaas_knabbels", 6),),
      deps=["camping_markt_camping_vind"], shape="circle")
    q("camping_markt_stok", "Marshmallows voor altijd", "De Roosterstok werkt boven &6elk&r brandend kampvuur, ook thuis en ook overdag. Rooster er een "
      "goudbruin! (Je hebt marshmallowknabbels nodig: stokje + suiker + kaasknabbel.)", "guhs:marshmallow_knabbel", [adv("camping_markt_goudbruin")],
      rewards=(("guhs:marshmallow_knabbel", 4),), deps=["camping_markt_camping_5"], shape="diamond")
    q("camping_markt_tentje", "Een tent voor thuis", "Van &6tentdoek&r (wol + draad) bouw je je eigen tent: de schuine stukken zijn het dak. Maak acht "
      "schuine stukken in een kleur naar keuze.", "guhs:campingmarkt_tentdoek_geel_trap",
      [item("guhs:campingmarkt_tentdoek_geel_trap", 8)], rewards=(("guhs:kaas_knabbels", 8),), deps=["camping_markt_camping_5"], shape="rsquare")

    q("camping_markt_markt_vind", "Kraampjes in het donker", "Gestreepte kraampjes rond een grote tent met een weegschaal: de &6Nether-Mika-ruilmarkt&r. "
      "Het superkompas (Barbecue > Nether-Mika-ruilmarkt) wijst de weg.",
      "guhs:campingmarkt_tentdoek_rood", [structure("mika_ruilmarkt")], rewards=(("guhs:kaas_knabbels", 12),), deps=["bbq_dimensie"],
      shape="hexagon", xp=100)
    wereld.ftb_questlijn(fq, "camping_markt", "ruilmarkt", [
        ("De Marktmeester", "Praat met de &dMarktmeester-Mika&r op zijn spreekgestoelte voor de Waag.", "guhs:vahoege_vads_ingot"),
        ("Leer afdingen", "Praat nog eens met hem en ding af van 30 naar 10 knabbels. Lees goed wat hij zegt: &6schept hij op&r, geef dan een compliment; "
                          "&6zucht hij&r, bied dan laag; &6gromt hij&r over zijn laatste bod, doe dan alsof je wegloopt. Drie keer fout en hij "
                          "stuurt je weg (dan begin je gewoon opnieuw).", "guhs:kaas_knabbels"),
        ("De nepvads", "In de Waag liggen vijf stapels vads. Eén is &6nep&r en nepvads is lichter. Klik op twee stapels om ze te wegen; klik daarna met "
                       "de &6Keurstempel&r op de neppe. (Welke nep is, is voor iedereen anders. Fout gestempeld? Dan wisselt de oplichter ze om.)",
         "guhs:campingmarkt_keurstempel"),
        ("Ontmaskerd!", "Vertel het de Marktmeester-Mika. Je krijgt de &6weegschaal&r en voortaan een extraatje bij elke ruil met een Nether-Mika.",
         "guhs:campingmarkt_weegschaal")],
        na=["camping_markt_markt_vind"], eind=(("guhs:kaas_knabbels", 16),))
    q("camping_markt_kraam", "Vads erin, verrassing eruit", "De &dkraam-Mika's&r van de markt ruilen meteen: geef er een een staaf &6vahoege vads&r "
      "en je krijgt je verrassing. Geen gesnuffel, en ze doen je niks.", "guhs:vahoege_vads_ingot", [adv("camping_markt_kraam")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["camping_markt_markt_vind"], shape="circle")
    q("camping_markt_extraatje", "Een extraatje, mjauw", "Ruil na de questlijn met een Nether-Mika (op de markt of in het wild, door de staaf in zijn "
      "poot te geven): je krijgt er een tweede cadeautje bij.", "minecraft:bundle", [adv("camping_markt_extraatje")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["camping_markt_ruilmarkt_4"], shape="diamond")
    q("camping_markt_koopje", "Het koopje van de dag", "Elke dag kun je bij de Marktmeester-Mika opnieuw afdingen. Win je, dan krijg je het koopje van "
      "de dag.", "guhs:campingmarkt_tentdoek_creme", [adv("camping_markt_koopje")], rewards=(("guhs:kaas_knabbels", 8),),
      deps=["camping_markt_ruilmarkt_4"], shape="rsquare")
