"""
1.10.0 "Het Vadsrijk", the kern slice (CONTRACT_VADSRIJK.md). Java: feature/vadsrijk/kern (+ klassen/ridder).

  - the dimension guhs:vadsrijk (type, flat generator with features on, biome with the map feature guhs:vr_kaart);
  - kern's items (3 weapons, 2 armour pieces: vanilla look for now), the boss chest block, the grey klodder texture;
  - every kern text (Dutch; en_us gets the same Dutch texts).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_kern.py
"""
import json
import os

DIM_TYPE = {
    "attributes": {
        "minecraft:gameplay/bed_rule": {"can_set_spawn": "never", "can_sleep": "never"},
        "minecraft:gameplay/can_start_raid": False,
        "minecraft:gameplay/respawn_anchor_works": False,
        "minecraft:visual/ambient_light_color": "#1a0f14",
        "minecraft:visual/cloud_color": "#ccffd6ea",
        "minecraft:visual/cloud_height": 150.33,
        "minecraft:visual/fog_color": "#ffd9c2",
        "minecraft:visual/sky_color": "#ffb3d1",
    },
    "has_skylight": True,
    "has_ceiling": False,
    "has_ender_dragon_fight": False,
    "coordinate_scale": 1.0,
    "ambient_light": 0.1,
    "min_y": 0,
    "height": 320,
    "logical_height": 320,
    "infiniburn": "#minecraft:infiniburn_overworld",
    "monster_spawn_light_level": 0,
    "monster_spawn_block_light_limit": 0,
    "has_fixed_time": False,
    "default_clock": "minecraft:overworld",   # (integration: /time set works in the dimension, like the Guhmension)
    "timelines": "#minecraft:in_overworld",
}

# (fix round 2 "biomen": the generator guhs:vadsrijk_kaart = the old flat generator (one bedrock layer, no structures, the
# biome features = the map) with a biome per area from guhs:vadsrijk_gebieden; the biomes: vadsrijk_biomen.py)
DIM = {
    "type": "guhs:vadsrijk",
    "generator": {"type": "guhs:vadsrijk_kaart", "biome_source": {"type": "guhs:vadsrijk_gebieden", "oud": "guhs:vadsrijk"}},
}

BIOME = {
    "attributes": {
        "minecraft:visual/fog_color": "#ffd9c2",
        "minecraft:visual/sky_color": "#ffb3d1",
        "minecraft:visual/water_fog_color": "#d77ab8",
    },
    "has_precipitation": False,
    "temperature": 0.8,
    "downfall": 0.0,
    "effects": {"water_color": "#ff9ad5", "foliage_color": "#8fd46a", "grass_color": "#a8e07a"},
    "spawners": {},
    "spawn_costs": {},
    "carvers": [],
    "features": [["guhs:vr_kaart"], [], [], [], [], [], [], [], [], [], []],
}

CONFIGURED = {"type": "guhs:vr_kaart", "config": {}}
PLACED = {"feature": "guhs:vr_kaart", "placement": []}

# item id -> vanilla item texture (placeholder look until the economie/art pass)
ITEMS = {
    "vr_kussenzwaard": ("minecraft:item/golden_sword", "minecraft:item/handheld"),
    "vr_knuffelknots": ("minecraft:item/mace", "minecraft:item/handheld"),
    "vr_pluishamer": ("minecraft:item/iron_axe", "minecraft:item/handheld"),
    "vr_pluishelm": ("minecraft:item/golden_helmet", "minecraft:item/generated"),
    "vr_knuffelharnas": ("minecraft:item/golden_chestplate", "minecraft:item/generated"),
    "vr_roze_ring": ("minecraft:item/gold_nugget", "minecraft:item/generated"),
    "vr_knuffelketting": ("minecraft:item/iron_chain", "minecraft:item/generated"),
    "vr_pluisoorbel": ("minecraft:item/amethyst_shard", "minecraft:item/generated"),
}
# the default armour look of every Vadsrijk armour piece (KernRegistry.STANDAARD_UITERLIJK): vanilla iron layers, tinted pink
UITERLIJK = "vr_knuffel"
ROZE_TINT = (255, 150, 205)

K = "vadsrijk.kern."
TEKSTEN = {
    "biome.guhs.vadsrijk": "Het Vadsrijk",
    "key.category.guhs.vadsrijk": "Vadsrijk",
    "key.guhs.vr_r": "Vaardigheid 1 (R)",
    "key.guhs.vr_v": "Vaardigheid 2 (V)",
    "key.guhs.vr_g": "Vaardigheid 3 (G)",
    "key.guhs.vr_z": "Ultieme vaardigheid (Z)",
    "key.guhs.vr_sprong": "Knuffelsprong (C)",
    "item.guhs.vr_kussenzwaard": "Kussenzwaard",
    "item.guhs.vr_knuffelknots": "Knuffelknots",
    "item.guhs.vr_pluishamer": "Pluishamer",
    "item.guhs.vr_pluishelm": "Pluishelm",
    "item.guhs.vr_knuffelharnas": "Knuffelharnas",
    "item.guhs.vr_roze_ring": "Roze Ring",
    "item.guhs.vr_knuffelketting": "Knuffelketting",
    "item.guhs.vr_pluisoorbel": "Pluisoorbel",
    "vadsrijk.loot.roze_ring.flavour.0": "\"Past om elk pootje.\"",
    "vadsrijk.loot.roze_ring.flavour.1": "\"Glimt een beetje als je njeg zegt.\"",
    "vadsrijk.loot.roze_ring.flavour.2": "\"Gevonden in een klodder. Afgespoeld, beloofd.\"",
    "vadsrijk.loot.knuffelketting.flavour.0": "\"Een knuffel om je nek, de hele dag.\"",
    "vadsrijk.loot.knuffelketting.flavour.1": "\"Rammelt vahoeg zacht.\"",
    "vadsrijk.loot.knuffelketting.flavour.2": "\"Oma-guh breide hem. Van ijzer.\"",
    "vadsrijk.loot.pluisoorbel.flavour.0": "\"Voor aan je guh-oortje.\"",
    "vadsrijk.loot.pluisoorbel.flavour.1": "\"Je hoort er de vads mee ruisen.\"",
    "vadsrijk.loot.pluisoorbel.flavour.2": "\"Kietelt. Dat hoort zo.\"",
    K + "slot.wapen": "Wapen",
    K + "slot.hoofd": "Hoofd",
    K + "slot.borst": "Borst",
    K + "slot.benen": "Benen",
    K + "slot.voeten": "Voeten",
    K + "slot.ring": "Ring",
    K + "slot.ketting": "Ketting",
    K + "slot.oorbel": "Oorbel",
    K + "sieraad.om": "Je draagt nu: %s",
    K + "sieraad.te_hoog": "Dit sieraad is voor niveau %s.",
    K + "sieraad.vol": "Je rugzak is vol: het sieraad blijft om.",
    K + "sieraad.leeg": "%s: leeg. Rechtsklik een sieraad of Shift+klik het in je inventaris om het om te doen.",
    K + "sieraad.hoe": "Rechtsklik om te dragen (of Shift+klik in je inventaris).",
    K + "sieraad.klik_af": "Klik om af te doen.",
    K + "item.los_stuk": "Los stuk: hoort bij geen set.",
    "block.guhs.vr_baaskist": "Buitkist",
    "entity.guhs.vr_proefklodder": "Proefklodder",
    "entity.guhs.vr_proefkloddertje": "Kloddertje",
    "entity.guhs.vr_klodderreus": "Grauwe Klodderreus",
    # flavour lines (vadsrijk.loot.<type>.flavour.<n>)
    "vadsrijk.loot.kussenzwaard.flavour.0": "\"Slaat hard, knuffelt harder.\"",
    "vadsrijk.loot.kussenzwaard.flavour.1": "\"Gevuld met echt guhdons. Niet aan de guhs vertellen.\"",
    "vadsrijk.loot.kussenzwaard.flavour.2": "\"Plof! Sorry. Plof!\"",
    "vadsrijk.loot.knuffelknots.flavour.0": "\"Een knuffel met extra overtuigingskracht.\"",
    "vadsrijk.loot.knuffelknots.flavour.1": "\"Mosgroen en een tikje vads.\"",
    "vadsrijk.loot.knuffelknots.flavour.2": "\"Werkt ook als hoofdkussen. Njeg.\"",
    "vadsrijk.loot.pluishamer.flavour.0": "\"Vahoeg zwaar, vahoeg zacht.\"",
    "vadsrijk.loot.pluishamer.flavour.1": "\"Smeedwerk van een smidguh met een lispel.\"",
    "vadsrijk.loot.pluishamer.flavour.2": "\"Ruikt een beetje naar kaasknabbels.\"",
    "vadsrijk.loot.pluishelm.flavour.0": "\"Met gaatjes voor guh-oortjes.\"",
    "vadsrijk.loot.pluishelm.flavour.1": "\"Kriebelt een beetje. Dat hoort zo.\"",
    "vadsrijk.loot.pluishelm.flavour.2": "\"Pluis bovenop, vads van binnen.\"",
    "vadsrijk.loot.knuffelharnas.flavour.0": "\"Voelt als een eeuwige knuffel.\"",
    "vadsrijk.loot.knuffelharnas.flavour.1": "\"Wasbaar op 30 graden. Niet in de droger.\"",
    "vadsrijk.loot.knuffelharnas.flavour.2": "\"Ridders van de Knuffelorde dragen dit met trots.\"",
    # elements, stats, statuses, rarities, affixes
    K + "element.geen": "Geen element",
    K + "element.roze": "Roze",
    K + "element.vuur": "Vuur",
    K + "element.natuur": "Natuur",
    K + "element.water": "Water",
    K + "stat.vads": "Vads",
    K + "stat.kracht": "Kracht",
    K + "stat.behendigheid": "Behendigheid",
    K + "stat.wijsheid": "Wijsheid",
    K + "stat.verdediging": "Verdediging",
    K + "stat.kritiek": "Kritiek",
    K + "stat.kritiekschade": "Kritiekschade",
    K + "stat.aanvalssnelheid": "Aanvalssnelheid",
    K + "stat.loopsnelheid": "Loopsnelheid",
    K + "stat.cooldownreductie": "Cooldownreductie",
    K + "stat.elementschade": "Elementschade",
    K + "stat.elementweerstand": "Elementweerstand",
    K + "stat.levensroof": "Levensroof",
    K + "stat.healkracht": "Heal-kracht",
    K + "status.brandend": "Brandend",
    K + "status.gesporen": "Gesporen",
    K + "status.plakkerig": "Plakkerig",
    K + "status.verdoofd": "Verdoofd",
    K + "status.vahoeg": "Vahoeg",
    K + "status.knuffelschild": "Knuffelschild",
    K + "status.snelpootjes": "Snelpootjes",
    K + "status.duizelig": "Duizelig",
    K + "zeldzaamheid.gewoon": "Gewoon",
    K + "zeldzaamheid.ongewoon": "Ongewoon",
    K + "zeldzaamheid.zeldzaam": "Zeldzaam",
    K + "zeldzaamheid.episch": "Episch",
    K + "zeldzaamheid.legendarisch": "Legendarisch",
    K + "zeldzaamheid.guhddelijk": "Guhddelijk",
    K + "affix.gloeiend": "Gloeiend",
    K + "affix.snel": "Snel",
    K + "affix.splitsend": "Splitsend",
    K + "affix.schildend": "Schildend",
    K + "elite": "Elite",
    # areas
    K + "gebied.zwamzoet": "Zwamzoet",
    K + "gebied.haven": "De Haven",
    K + "gebied.klodderweide": "Klodderweide",
    K + "gebied.sporenwoud": "Sporenwoud",
    K + "gebied.druipgrotten": "Druipgrotten",
    K + "gebied.zwamtempel": "De Zwamtempel",
    K + "gebied.ring": "Het Vadsrijk",
    K + "gebied.knuffelkamp": "Knuffelkamp",
    K + "gebied.proeftuin": "De Proeftuin",
    K + "gebied.veilig": "Veilig gebied",
    K + "gebied.levels": "Niveau %s - %s",
    # the class and its skills
    K + "klasse.knuffelridder": "Knuffelridder",
    K + "skill.kussenklap": "Kussenklap",
    K + "skill.stormsprong": "Stormsprong",
    K + "skill.knuffelschild": "Knuffelschild",
    K + "skill.vahoege_plof": "Vahoege Plof",
    K + "skill.knuffelsprong": "Knuffelsprong",
    K + "skill.cooldown": "Nog even wachten...",
    K + "skill.ult_niet_vol": "Je ultieme vaardigheid is nog niet opgeladen. Meer meppen!",
    K + "skill.geen_energie": "Te weinig Vadsenergie.",
    # moments
    K + "levelop.titel": "NIVEAU OMHOOG!",
    K + "levelop.onder": "%s - niveau %s. Vahoeg!",
    K + "flauw.titel": "Flauwgevallen...",
    K + "flauw.onder": "Je wordt wakker bij je kampvuur. Nog even duizelig.",
    K + "kampvuur.checkpoint": "Kampvuur aangestoken! Hier word je wakker als je flauwvalt.",
    K + "kampvuur.rust": "Je warmt je pootjes. Vads en energie zijn weer vol.",
    K + "baas.klodderreus": "GRAUWE KLODDERREUS",
    K + "baas.klodderreus.onder": "De Plakkerige Plaag van het Kamp",
    K + "baas.te_ver": "Te ver weg! Kom de arena in om de baas te raken.",
    K + "arena.wakker": "is wakker!",
    K + "arena.terug": "terug over %s",
    K + "baaskist.niet_jouw": "Deze buitkist is van %s. Jouw kist staat er vast ook!",
    K + "baaskist.kreeg": "Uit de buitkist: %s",
    K + "cmd.kist": "%s spullen teruggegeven (nog %s in de verloren-voorwerpenkist).",
    # items
    K + "verpluisd": "Verpluisd",
    K + "item.ongerold": "Nog niet gerold (vraag een /guhs vadsrijk loot).",
    K + "item.alle_klassen": "Alle klassen",
    K + "item.level": "Niveau %s",
    K + "item.schade": "%s schade per klap",
    K + "item.aanvalssnelheid": "Aanvalssnelheid: %s s",
    K + "item.gebonden": "Gebonden: niet te ruilen",
    K + "item.verpluisd_uitleg": "Verpluisd! Laat hem repareren bij de smidguh.",
    K + "item.guhddelijk": "Er dwarrelt roze glitter van af...",
    # party
    K + "party.zelf": "Jezelf uitnodigen? Knuffel jezelf maar gewoon.",
    K + "party.vol": "Je party is vol (5 guhvrienden).",
    K + "party.al_in_party": "%s zit al in een party.",
    K + "party.uitgenodigd": "%s is uitgenodigd!",
    K + "party.uitnodiging": "%s nodigt je uit voor een party! Typ /guhs vadsrijk party accepteer",
    K + "party.geen_uitnodiging": "Niemand heeft je uitgenodigd. Nog niet!",
    K + "party.erbij": "Je zit in de party. Samen sterk, samen vads!",
    K + "party.weg": "Je hebt de party verlaten.",
    # HUD
    K + "hud.vads": "Vads",
    K + "hud.energie": "Energie",
    K + "hud.niveau": "Nv %s",
    K + "hud.opgevrolijkt": "Opgevrolijkt: %s",
    K + "hud.fase": "Fase %s",
    K + "hud.ult_klaar": "KLAAR!",
    K + "hud.duizelig": "Duizelig %ss",
    K + "hud.bevroren": "❄ Bevroren %ss",
    # first-time hints
    K + "tip.welkom.titel": "Welkom in Het Vadsrijk!",
    K + "tip.welkom": "Hier heb je een eigen rugzak: je gewone spullen liggen veilig thuis. Linkermuis = meppen, R V G = vaardigheden, Z = je ultieme klap.",
    K + "tip.skill.titel": "Eerste vaardigheid!",
    K + "tip.skill": "Vaardigheden kosten Vadsenergie (blauwe balk) en hebben een afkoeltijd. Z laadt op door schade te doen.",
    K + "tip.loot.titel": "Buit!",
    K + "tip.loot": "De kleur van de lichtstraal is de zeldzaamheid. Alleen jij kunt jouw buit oppakken. Kijk in de tooltip voor de bonussen.",
    K + "tip.levelop.titel": "Niveau omhoog!",
    K + "tip.levelop": "Je hebt twee niveaus. Generiek (alles wat je doet): meer Vads en Verdediging, nieuwe gebieden en quests. Klasse (het wapen waarmee je raakt): je hoofdstat, skillpunten en betere wapens.",
    K + "tip.kampvuur.titel": "Kampvuur",
    K + "tip.knuffelsprong.titel": "Knuffelsprong!",
    K + "tip.knuffelsprong": "Druk op C voor een flinke sprong vooruit (zo'n 20 blokken). Elke klasse kan het, je landt altijd zacht, en na 5 tellen mag het weer.",
    K + "tip.elementen.titel": "Elementen",
    K + "tip.elementen": "Het icoontje voor de naam van een wezen is zijn element:  Vuur wint van  Zwam, Zwam van  Slijm, Slijm van Vuur (x1,5; andersom x0,7).  Roze is extra sterk tegen ontvadste wezens. Geen icoontje = geen element.",
    K + "tip.kampvuur": "Val je flauw, dan word je hier wakker. Je verliest niks, je bent alleen even duizelig.",
    # fix round 1: losknuffelen (kern.veilig)
    "key.guhs.vr_vast": "Losknuffelen (vast? naar een veilig plekje)",
    K + "vast.bezig": "Even stilstaan... de knuffelwind tilt je op!",
    K + "vast.klaar": "Hup, weer los! Je staat op een veilig plekje.",
    K + "vast.kampvuur": "Geen veilig plekje dichtbij: de knuffelwind brengt je naar je kampvuur.",
    K + "vast.gestopt.bewogen": "Losknuffelen gestopt: je bewoog.",
    K + "vast.gestopt.geraakt": "Losknuffelen gestopt: je werd geraakt!",
    K + "vast.wacht": "De knuffelwind rust nog even uit: probeer het over %s s weer.",
    K + "vast.gevecht": "Niet tijdens een bazengevecht! Loop door de deur naar buiten (of val flauw).",
    K + "vast.alleen_rijk": "Losknuffelen werkt alleen in het Vadsrijk.",
    K + "vast.hint": "Vast? Druk op %s",
    K + "vast.gered": "Oei, je viel van de wereld! De knuffelwind ving je op.",
    K + "tip.vast.titel": "Vastgelopen?",
    K + "tip.vast": "Zit je ergens vast? Druk op U (of /guhs vadsrijk vast) en sta 3 tellen stil: de knuffelwind zet je op het dichtstbijzijnde veilige plekje. Kan elke minuut, niet tijdens een bazengevecht.",
    K + "wapen.vereist": "Dit wapen vereist %s nv %s (jij: nv %s). Vasthouden mag, aanvallen nog niet.",
    K + "klasse.actief": "⚔ Nu %s!",
    K + "skill.geen_wapen": "Geen klassewapen in je hand: geen vaardigheden, alleen een zwak klapje.",
    K + "levelop.beide": "Generiek nv %s · %s nv %s. Vahoeg!",
    K + "levelop.generiek": "Generiek niveau %s: meer Vads en Verdediging. Vahoeg!",
    K + "hud.generiek": "Generiek nv %s",
    K + "hud.klasse": "%s nv %s",
    K + "item.vereist_klasse": "Vereist: %s nv %s",
    K + "item.vereist_generiek": "Vereist: Generiek nv %s",
    # fix round 3 (i-ui): the rest of the mod stays outside
    "gui.guhs.vadsrijk.roep.niet_hier": "Je guhs blijven lekker thuis zolang jij in het Vadsrijk op avontuur bent, njeg.",
}


def build(h):
    h.w(f"{h.D}/dimension_type/vadsrijk.json", DIM_TYPE)
    h.w(f"{h.D}/dimension/vadsrijk.json", DIM)
    h.w(f"{h.D}/worldgen/biome/vadsrijk.json", BIOME)
    h.w(f"{h.D}/worldgen/configured_feature/vr_kaart.json", CONFIGURED)
    h.w(f"{h.D}/worldgen/placed_feature/vr_kaart.json", PLACED)
    _assets(h.w, h.A)
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)


# fix gevecht: the element icons (heart, flame, leaf, drop) as glyphs U+F8A1..U+F8A4 of minecraft:default
ICOON_TEKENS = ""
ICONEN = {
    # 16x16 pixel art, '.' = transparent; colours per letter
    "kleuren": {"a": (255, 122, 200), "b": (255, 190, 228), "c": (190, 60, 140),          # roze
                "d": (255, 90, 42), "e": (255, 200, 60), "f": (170, 40, 20),            # vuur
                "g": (91, 209, 74), "h": (170, 240, 130), "i": (40, 120, 40),           # natuur
                "j": (63, 169, 255), "k": (190, 230, 255), "l": (30, 90, 170)},         # water
    "roze": [
        "................",
        "................",
        "...ccc....ccc...",
        "..cbbac..caaac..",
        ".cbbaaaccaaaaac.",
        ".cbaaaaaaaaaaac.",
        ".caaaaaaaaaaaac.",
        ".caaaaaaaaaaaac.",
        "..caaaaaaaaaac..",
        "...caaaaaaaac...",
        "....caaaaaac....",
        ".....caaaac.....",
        "......caac......",
        ".......cc.......",
        "................",
        "................"],
    "vuur": [
        "........f.......",
        ".......fdf......",
        ".......fddf.....",
        "......fddddf....",
        "....f.fdddddf...",
        "...fdffddeddf...",
        "...fddfdeedddf..",
        "..fdddddeeeddf..",
        "..fddddeeeeddf..",
        "..fdddeeeeeddf..",
        "..fdddeeeeeeddf.",
        "..fddeeeeeeeddf.",
        "...fddeeeeeddf..",
        "....fddeeeddf...",
        ".....fffffff....",
        "................"],
    "natuur": [
        "................",
        "...........iii..",
        ".........iigggi.",
        ".......iiggghgi.",
        "......igggghhgi.",
        ".....iggggghggi.",
        "....igggghhgggi.",
        "....iggghggggi..",
        "...igghhggggi...",
        "...ighgggggi....",
        "...ihgggggi.....",
        "..ihiggiii......",
        "..ii.iii........",
        ".ii.............",
        ".i..............",
        "................"],
    "water": [
        ".......l........",
        "......ljl.......",
        "......ljl.......",
        ".....ljjjl......",
        ".....ljjjl......",
        "....ljjjjjl.....",
        "...ljjjjjjjl....",
        "...ljkjjjjjl....",
        "..ljkkjjjjjjl...",
        "..ljkjjjjjjjl...",
        "..ljkjjjjjjjl...",
        "..ljjjjjjjjjl...",
        "...ljjjjjjjl....",
        "....ljjjjjl.....",
        ".....lllll......",
        "................"],
}


def _iconen(w, a):
    """assets/guhs/textures/font/vr_element.png (64x16) + a bitmap provider added to minecraft:default."""
    from PIL import Image
    img = Image.new("RGBA", (64, 16), (0, 0, 0, 0))
    for i, naam in enumerate(("roze", "vuur", "natuur", "water")):
        for y, regel in enumerate(ICONEN[naam]):
            for x, c in enumerate(regel):
                if c != ".":
                    img.putpixel((i * 16 + x, y), ICONEN["kleuren"][c] + (255,))
    doel = os.path.join(a, "textures", "font", "vr_element.png")
    os.makedirs(os.path.dirname(doel), exist_ok=True)
    img.save(doel)
    w(os.path.join(a, "..", "minecraft", "font", "default.json"), {"providers": [
        {"type": "bitmap", "file": "guhs:font/vr_element.png", "ascent": 7, "height": 8, "chars": [ICOON_TEKENS]}]})


def _assets(w, a):
    _iconen(w, a)
    for item, (tex, parent) in ITEMS.items():
        w(os.path.join(a, "items", item + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + item}})
        w(os.path.join(a, "models", "item", item + ".json"), {"parent": parent, "textures": {"layer0": tex}})
    w(os.path.join(a, "blockstates", "vr_baaskist.json"), {"variants": {"": {"model": "guhs:block/vr_baaskist"}}})
    w(os.path.join(a, "models", "block", "vr_baaskist.json"), {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "minecraft:block/gold_block", "bottom": "minecraft:block/gold_block", "side": "minecraft:block/pink_glazed_terracotta"}})
    w(os.path.join(a, "items", "vr_baaskist.json"), {"model": {"type": "minecraft:model", "model": "guhs:block/vr_baaskist"}})
    _grijs(a)
    _uiterlijk(w, a)


def _uiterlijk(w, a):
    """The pink default armour look: assets/guhs/equipment/vr_knuffel.json + the two layer textures (vanilla iron, tinted)."""
    import zipfile
    from io import BytesIO
    from PIL import Image
    w(os.path.join(a, "equipment", UITERLIJK + ".json"), {"layers": {
        "humanoid": [{"texture": "guhs:" + UITERLIJK}], "humanoid_leggings": [{"texture": "guhs:" + UITERLIJK}]}})
    jar = os.path.join("build", "moddev", "artifacts", "minecraft-patched-26.1.2.112.jar")
    for laag in ("humanoid", "humanoid_leggings"):
        doel = os.path.join(a, "textures", "entity", "equipment", laag, UITERLIJK + ".png")
        if not os.path.exists(jar):
            if not os.path.exists(doel):
                print("vadsrijk_kern: no minecraft jar (run a gradle build first) for", doel)
            continue
        with zipfile.ZipFile(jar) as z:
            img = Image.open(BytesIO(z.read(f"assets/minecraft/textures/entity/equipment/{laag}/iron.png"))).convert("RGBA")
        px = img.load()
        for y in range(img.height):
            for x in range(img.width):
                r, g, b, al = px[x, y]
                l = (0.3 * r + 0.59 * g + 0.11 * b) / 255
                px[x, y] = (int(ROZE_TINT[0] * l), int(ROZE_TINT[1] * l), int(ROZE_TINT[2] * l), al)
        os.makedirs(os.path.dirname(doel), exist_ok=True)
        img.save(doel)


def _grijs(a):
    """The grey 'ontvadst' klodder: the guh slime texture, desaturated and a little darker."""
    from PIL import Image
    bron = os.path.join(a, "textures", "entity", "guh_slime.png")
    doel = os.path.join(a, "textures", "vadsrijk", "kern", "grauwe_klodder.png")
    img = Image.open(bron).convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, al = px[x, y]
            l = int((0.3 * r + 0.59 * g + 0.11 * b) * 0.8)
            px[x, y] = (l, l, int(l * 1.05) if l * 1.05 < 255 else 255, al)
    os.makedirs(os.path.dirname(doel), exist_ok=True)
    img.save(doel)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    a = os.path.join("src", "main", "resources")
    d = os.path.join(a, "data", "guhs")
    _schrijf(os.path.join(d, "dimension_type", "vadsrijk.json"), DIM_TYPE)
    _schrijf(os.path.join(d, "dimension", "vadsrijk.json"), DIM)
    _schrijf(os.path.join(d, "worldgen", "biome", "vadsrijk.json"), BIOME)
    _schrijf(os.path.join(d, "worldgen", "configured_feature", "vr_kaart.json"), CONFIGURED)
    _schrijf(os.path.join(d, "worldgen", "placed_feature", "vr_kaart.json"), PLACED)
    _assets(_schrijf, os.path.join(a, "assets", "guhs"))
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(TEKSTEN)
        _schrijf(p, data)
    print(f"vadsrijk_kern: dimension + items + {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
