"""
Het Snuffeleiland, the KERN (DESIGN_VERHALENPAD C). Java: feature/snuffel.

  - the approved models of snuffel_modellen.py, written for the game (geo, animations, textures), with three animations
    more for every dog: snuffel_loop (sniffing while walking), graaf (digging) and blaf (a bark), and one more for every
    companion: wijs (it points). Nothing of the approved looks is changed: same bones, same textures;
  - data/guhs/snuffel/honden.json: the breeds (coats, how tall the dog and its puppy are), the named residents, for
    feature/snuffel/Honden.java;
  - the dimension guhs:snuffeleiland: a flat sea (stone, sand, water up to y 62), day and night, no weather, no mobs, NO
    music (the biome and the dimension type both say so);
  - data/guhs/snuffel/eiland.json + template guhs:snuffel/eiland: the small test island of snuffel_bouw.py (the island
    slice writes both again), and the game tests' floor snuffel_test_eiland;
  - block guhstation, items snuffel_geheugenkaart and snuffel_bloesemtakje (snuffel_tex.py);
  - the sounds (vanilla and guh sounds, pitched), the texts, the questline "snuffeleiland" (nine steps: the frame that the
    dock and the village slices fill in), its hidden advancements and its FTB quests.

The wiki entries are in snuffel_wiki.py.
"""
import json
import os

from PIL import Image

from . import bbq2
from . import snuffel_bouw as bouw
from . import snuffel_modellen as modellen
from . import snuffel_tex as tex
from . import verhaal_motor
from . import wereld

NAME = "snuffel"
FTB_CHAPTER = "guhs_verhalen"
FTB_SECTION = "Het Snuffeleiland"
FTB_PORTRAIT = "geo:snuffelhond_shiba:snuffelhond_shiba_rood"

# the dog's box in Java is never higher than this (it walks under a block), its eyes never higher than that
MAX_HOOGTE, MAX_OOG = 0.9, 0.78


# =====================================================================================================================
# the models: the approved generator's files + the animations the game needs on top
# =====================================================================================================================
def extra_hond_anims(d):
    """snuffel_loop, graaf, blaf for a dog with the numbers d (snuffel_modellen.maten)."""
    kf, anim = modellen.kf, modellen.anim
    sn = modellen.SNUFFEL_HOEK
    poten = ("poot_lv", "poot_rv", "poot_la", "poot_ra")
    # sniffing while walking: the sniff pose, and the legs swing around the angle that keeps them upright
    loop = {"lijf": {"rotation": kf((0, (sn[0], 0, 0)), (0.6, (sn[0], 0, 0))),
                     "position": kf((0, (0, 0, 0)), (0.15, (0, 0.4, 0)), (0.3, (0, 0, 0)), (0.45, (0, 0.4, 0)), (0.6, (0, 0, 0)))},
            "kop": {"rotation": kf((0, (sn[1], 6, 0)), (0.15, (sn[1] + 5, 6, 0)), (0.3, (sn[1], -6, 0)), (0.45, (sn[1] + 5, -6, 0)), (0.6, (sn[1], 6, 0)))},
            "snuit": {"scale": kf((0, (1, 1, 1)), (0.15, (1.06, 1.06, 1.04)), (0.3, (1, 1, 1)), (0.45, (1.06, 1.06, 1.04)), (0.6, (1, 1, 1)))},
            "staart": {"rotation": kf((0, (sn[2], 14, 0)), (0.3, (sn[2], -14, 0)), (0.6, (sn[2], 14, 0)))}}
    for n, s in zip(poten, (26, -26, -26, 26)):
        loop[n] = {"rotation": kf((0, (s - sn[0], 0, 0)), (0.3, (-s - sn[0], 0, 0)), (0.6, (s - sn[0], 0, 0)))}
    # digging: nose in the hole, the front paws scratch in turn, the tail up and wagging, a little shake of the body
    g = sn[0] + 6
    graaf = {"lijf": {"rotation": kf((0, (g, 0, 1.5)), (0.1, (g, 0, -1.5)), (0.2, (g, 0, 1.5)), (0.3, (g, 0, -1.5)), (0.4, (g, 0, 1.5)))},
             "kop": {"rotation": kf((0, (sn[1] - 4, 0, 0)), (0.2, (sn[1] + 2, 0, 0)), (0.4, (sn[1] - 4, 0, 0)))},
             "poot_lv": {"rotation": kf((0, (-g - 38, 0, 0)), (0.2, (-g + 22, 0, 0)), (0.4, (-g - 38, 0, 0)))},
             "poot_rv": {"rotation": kf((0, (-g + 22, 0, 0)), (0.2, (-g - 38, 0, 0)), (0.4, (-g + 22, 0, 0)))},
             "poot_la": {"rotation": kf((0, (-g, 0, 0)), (0.4, (-g, 0, 0)))},
             "poot_ra": {"rotation": kf((0, (-g, 0, 0)), (0.4, (-g, 0, 0)))},
             "staart": {"rotation": kf((0, (sn[2], 22, 0)), (0.1, (sn[2], -22, 0)), (0.2, (sn[2], 22, 0)), (0.3, (sn[2], -22, 0)), (0.4, (sn[2], 22, 0)))}}
    # a bark: the head snaps up twice, the snout opens, a little hop, the ears bounce
    blaf = {"kop": {"rotation": kf((0, (0, 0, 0)), (0.06, (-22, 0, 0)), (0.16, (-8, 0, 0)), (0.24, (-20, 0, 0)), (0.4, (0, 0, 0)))},
            "snuit": {"scale": kf((0, (1, 1, 1)), (0.06, (1.1, 1.18, 1.05)), (0.16, (1, 1, 1)), (0.24, (1.1, 1.18, 1.05)), (0.4, (1, 1, 1)))},
            "lijf": {"position": kf((0, (0, 0, 0)), (0.08, (0, 0.9, 0)), (0.18, (0, 0, 0)), (0.26, (0, 0.6, 0)), (0.4, (0, 0, 0)))},
            "oor_links": {"rotation": kf((0, (0, 0, 0)), (0.08, (0, 0, -12)), (0.2, (0, 0, 0)), (0.28, (0, 0, -10)), (0.4, (0, 0, 0)))},
            "oor_rechts": {"rotation": kf((0, (0, 0, 0)), (0.08, (0, 0, 12)), (0.2, (0, 0, 0)), (0.28, (0, 0, 10)), (0.4, (0, 0, 0)))},
            "staart": {"rotation": kf((0, (0, 18, 0)), (0.13, (0, -18, 0)), (0.27, (0, 18, 0)), (0.4, (0, -18, 0)))}}
    return {"snuffel_loop": anim(0.6, loop), "graaf": anim(0.4, graaf), "blaf": anim(0.4, blaf)}


def extra_maatje_anims():
    """wijs: it leans towards what the dog smells and nods at it."""
    kf, anim = modellen.kf, modellen.anim
    wijs = {"lijf": {"rotation": kf((0, (16, 0, 0)), (0.3, (28, 0, 0)), (0.6, (16, 0, 0))),
                     "position": kf((0, (0, 0, 0)), (0.3, (0, 0.8, 0)), (0.6, (0, 0, 0)))}}
    return {"wijs": anim(0.6, wijs)}


def modellen_voor_het_spel():
    """-> ({path: json}, {texture name: array}) of snuffel_modellen.maak() with the extra animations added."""
    json_uit, texturen = modellen.maak()
    for ras in modellen.RASSEN:
        for pup in (False, True):
            pad = f"animations/entity/snuffelhond_{ras}" + ("_pup" if pup else "") + ".animation.json"
            if pad in json_uit:
                json_uit[pad]["animations"].update(extra_hond_anims(modellen.maten(ras, pup)))
    for b in modellen.BEWONERS:
        json_uit[f"animations/entity/snuffel_{b['id']}.animation.json"]["animations"].update(extra_hond_anims(modellen.maten(b["ras"], b.get("pup", False))))
    for k in modellen.MAATJES:
        for gezicht in ("blij", "ondeugend"):
            json_uit[f"animations/entity/snuffel_maatje_{k}_{gezicht}.animation.json"]["animations"].update(extra_maatje_anims())
    return json_uit, texturen


def schrijf_modellen(h):
    json_uit, texturen = modellen_voor_het_spel()
    for pad, d in json_uit.items():
        h.w(f"{h.A}/geckolib/{pad}", d)
    for naam, img in texturen.items():
        h.save(Image.fromarray(img), "entity", f"{naam}.png")
    return json_uit, texturen


# =====================================================================================================================
# honden.json: what Java needs to know about the dogs
# =====================================================================================================================
def _maat(ras, pup):
    """(the top of the head, the eyes) in blocks: the model's numbers at the scale the game draws a dog."""
    d = modellen.maten(ras, pup)
    per_px = modellen.BASIS["schaal"] / 16.0
    hoogte = round(min(MAX_HOOGTE, max(0.5, d.ky1 * per_px)), 3)
    oog = round(min(MAX_OOG, hoogte - 0.08, max(0.3, d.ey * per_px)), 3)
    return hoogte, oog


def honden(h):
    rassen = {}
    for ras, r in modellen.RASSEN.items():
        heeft_pup = ras != "speurhond"
        hoogte, oog = _maat(ras, False)
        pup_hoogte, pup_oog = _maat(ras, True) if heeft_pup else (hoogte, oog)
        rassen[ras] = dict(kleuren=list(modellen.VACHTEN[ras]), speelbaar=ras != "speurhond", pup=heeft_pup, hoogte=hoogte, oog=oog,
                           pup_hoogte=pup_hoogte, pup_oog=pup_oog)
        h.lang(f"gui.guhs.snuffel.ras.{ras}", r["naam"], r["naam"])
        for kleur, v in modellen.VACHTEN[ras].items():
            naam = v["naam"][0].upper() + v["naam"][1:]
            h.lang(f"gui.guhs.snuffel.kleur.{ras}.{kleur}", naam, naam)
    bewoners = {}
    for b in modellen.BEWONERS:
        bewoners[b["id"]] = dict(ras=b["ras"], kleur=b["kleur"], pup=bool(b.get("pup", False)))
        h.lang(f"entity.guhs.snuffel_bewoner.{b['id']}", b["naam"], b["naam"])
    h.w(f"{h.D}/snuffel/honden.json", dict(rassen=rassen, bewoners=bewoners, maatjes=list(modellen.MAATJES)))
    for k, (naam, _) in modellen.MAATJES.items():
        h.lang(f"gui.guhs.snuffel.maatje.{k}", naam, naam)
        _, wat, hoe = modellen.MAATJE_TEKST[k]
        tekst = f"{wat} {hoe[0].upper()}{hoe[1:]}"
        h.lang(f"gui.guhs.snuffel.maatje.{k}.tekst", tekst, tekst)
    return rassen, bewoners


# =====================================================================================================================
# the dimension: a flat sea, day and night, no music
# =====================================================================================================================
def dimensie(h):
    D = h.D
    h.w(f"{D}/dimension/snuffeleiland.json", {"type": "guhs:snuffeleiland", "generator": {
        "type": "minecraft:flat", "settings": {"biome": "guhs:snuffeleiland", "lakes": False, "features": False, "structure_overrides": [], "layers": [
            {"block": "minecraft:bedrock", "height": 1},
            {"block": "minecraft:stone", "height": bouw.STEEN},
            {"block": "minecraft:sand", "height": bouw.ZAND - bouw.STEEN},
            {"block": "minecraft:water", "height": bouw.ZEE - bouw.ZAND}]}}})
    # written in the 26.1 shape (like guhpixel): the overworld's day and night and sky, beds do nothing, and NO background
    # music (an empty BackgroundMusic: DESIGN_VERHALENPAD wants sound effects only on the island)
    h.w(f"{D}/dimension_type/snuffeleiland.json", {
        "attributes": {
            "minecraft:audio/background_music": {},
            "minecraft:gameplay/bed_rule": {"can_set_spawn": "never", "can_sleep": "never"},
            "minecraft:gameplay/can_start_raid": False,
            "minecraft:gameplay/respawn_anchor_works": False,
            "minecraft:visual/ambient_light_color": "#0a0a0a",
            "minecraft:visual/cloud_color": "#ccffffff",
            "minecraft:visual/cloud_height": 192.33,
            "minecraft:visual/fog_color": "#c8e6ff",
            "minecraft:visual/sky_color": "#7ec0ff",
        },
        "has_skylight": True, "has_ceiling": False, "has_ender_dragon_fight": False, "coordinate_scale": 1.0,
        "ambient_light": 0.1, "min_y": 0, "height": 256, "logical_height": 256,
        "infiniburn": "#minecraft:infiniburn_overworld", "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0,
        "default_clock": "minecraft:overworld", "timelines": "#minecraft:in_overworld"})
    # the biome too in the 26.1 shape (mc26.py leaves a biome with its own background_music attribute alone)
    h.w(f"{D}/worldgen/biome/snuffeleiland.json", {
        "attributes": {
            "minecraft:audio/background_music": {},
            "minecraft:visual/fog_color": "#c8e6ff",
            "minecraft:visual/sky_color": "#7ec0ff",
            "minecraft:visual/water_fog_color": "#2a8fb8",
        },
        "has_precipitation": False, "temperature": 0.8, "downfall": 0.4,
        "effects": {"water_color": "#3fb4d8", "grass_color": "#7fcb52", "foliage_color": "#62b33b"},
        "spawners": {}, "spawn_costs": {}, "carvers": [], "features": []})


# =====================================================================================================================
# the test island and the game tests' floor
# =====================================================================================================================
def eiland(h):
    s, data = bouw.eiland(h)
    s.save("snuffel/eiland")
    h.w(f"{h.D}/snuffel/eiland.json", data)
    bouw.test_vloer(h).save("snuffel_test_eiland")
    return data


# =====================================================================================================================
# sounds (vanilla and guh sounds, pitched; no new sound files, no music)
# =====================================================================================================================
SOUNDS = {
    "snuffel.snuf": [{"name": "minecraft:entity.fox.sniff", "type": "event", "pitch": 1.25, "volume": 0.8}],
    "snuffel.blaf": [{"name": "minecraft:entity.wolf_cute.ambient", "type": "event", "pitch": 1.15},
                     {"name": "minecraft:entity.wolf.ambient", "type": "event", "pitch": 1.25},
                     {"name": "minecraft:entity.wolf_puglin.ambient", "type": "event", "pitch": 1.2}],
    "snuffel.njeg": [{"name": "guhs:entity.guh.happy", "type": "event", "pitch": 1.5, "volume": 0.45}],
    "snuffel.graaf": [{"name": "minecraft:block.sand.break", "type": "event", "pitch": 0.9},
                      {"name": "minecraft:block.gravel.break", "type": "event", "pitch": 1.1}],
    "snuffel.kwispel": [{"name": "minecraft:entity.wolf.shake", "type": "event", "pitch": 1.5, "volume": 0.4}],
    "snuffel.gevonden": [{"name": "minecraft:block.amethyst_block.chime", "type": "event", "pitch": 1.2},
                         {"name": "minecraft:entity.experience_orb.pickup", "type": "event", "pitch": 0.9, "volume": 0.6}],
    "snuffel.geleerd": [{"name": "minecraft:entity.player.levelup", "type": "event", "pitch": 1.6, "volume": 0.5}],
    "snuffel.groei": [{"name": "minecraft:item.bone_meal.use", "type": "event", "pitch": 0.9},
                      {"name": "minecraft:block.azalea_leaves.place", "type": "event", "pitch": 1.1}],
    "snuffel.reis": [{"name": "minecraft:entity.generic.splash", "type": "event", "pitch": 0.8, "volume": 0.7}],
    "snuffel.guhstation": [{"name": "minecraft:block.note_block.bit", "type": "event", "pitch": 1.5, "volume": 0.6}],
    "snuffel.maatje": [{"name": "minecraft:entity.allay.ambient_without_item", "type": "event", "pitch": 1.5, "volume": 0.7}],
}
SUBTITLES = {
    "snuffel.snuf": "Hond snuffelt", "snuffel.blaf": "Hond blaft", "snuffel.njeg": "Njeg!", "snuffel.graaf": "Hond graaft",
    "snuffel.kwispel": "Hond kwispelt", "snuffel.gevonden": "Iets gevonden", "snuffel.geleerd": "Nieuwe geur geleerd",
    "snuffel.groei": "Boompje groeit", "snuffel.reis": "Plons", "snuffel.guhstation": "Guhstation piept", "snuffel.maatje": "Maatje giechelt",
}


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# Dutch texts
# =====================================================================================================================
G = "gui.guhs.snuffel."
LANG = {
    "dimension.guhs.snuffeleiland": "Het Snuffeleiland",
    "biome.guhs.snuffeleiland": "Het Snuffeleiland",
    "block.guhs.guhstation": "Guhstation",
    "block.guhs.guhstation.lore": "Een grijszwart spelkastje met een guh-snoet erop. Het ruikt een beetje naar zeelucht en natte hond",
    "block.guhs.guhstation.tooltip": "Zet het neer en klik erop: zo ga je terug naar het Snuffeleiland",
    "item.guhs.snuffel_geheugenkaart": "Geheugenkaart",
    "item.guhs.snuffel_geheugenkaart.lore": "Hier staat alles op wat je neus heeft geleerd. Een hond raakt hem nooit kwijt",
    "item.guhs.snuffel_geheugenkaart.tooltip": "Gebruik hem om op te slaan en naar huis te gaan",
    "item.guhs.snuffel_bloesemtakje": "Bloesemtakje van je boompje",
    "item.guhs.snuffel_bloesemtakje.lore": "Het boompje van je maatje gaf het je mee. Het ruikt naar het Snuffeleiland, njeg",
    "entity.guhs.snuffel_bewoner": "Eilandhond",
    "entity.guhs.snuffel_hond": "Snuffelhond",
    "entity.guhs.snuffel_maatje": "Maatje",
    "entity.guhs.snuffel_boompje": "Boompje van je maatje",
    "key.guhs.snuffel.snuffel": "Snuffelen (ingedrukt houden)",
    "key.guhs.snuffel.zit": "Zitten (hond)",
    "key.guhs.snuffel.kwispel": "Kwispelen (hond)",
    "key.guhs.snuffel.blaf": "Blaffen (hond)",
    "key.guhs.snuffel.boekje": "Snuffelboekje",
    # the dog form, travel
    G + "post_wacht": "Er wachten nog %s dingen van het eiland op een vrij plekje in je zakken",
    G + "post_mee": "%s x %s gaat met je mee naar huis. Een hond heeft geen zakken, njeg",
    G + "aangespoeld": "De golven spoelen je terug op het eiland. Njeg, wat een plons",
    G + "stroming": "De stroming is hier te sterk. Zwem maar terug naar het eiland",
    G + "regels.poten": "Met pootjes kun je niet bouwen of breken. Wel snuffelen en graven, njeg",
    G + "reis.geen_eiland": "Het Snuffeleiland is nu niet te bereiken",
    G + "reis.al_daar": "Je bent al op het Snuffeleiland",
    G + "reis.spel_bezig": "Maak eerst je spelletje af, dan mag je naar het eiland",
    G + "reis.even_wachten": "Nog heel even wachten, njeg",
    G + "reis.afstappen": "Stap eerst even af",
    G + "reis.thuis": "Weer thuis, precies waar je was. Al je spulletjes zijn er nog. Vahoeg!",
    # sniffing
    G + "soort.eten": "iets lekkers",
    G + "soort.voorwerp": "een ding",
    G + "soort.dier": "een dier",
    G + "soort.vreemd": "iets vreemds",
    G + "meter.niets": "Je ruikt niets bijzonders",
    G + "meter.ruikt": "Je ruikt %s",
    G + "meter.graaf": "Hier is het! Graaf! (%s)",
    G + "meter.dichtbij": "Heel dichtbij! Snuffel nog even... (%s)",
    G + "graaf_niets": "Hier ligt niets. Alleen zand tussen je tenen, njeg",
    G + "gevonden": "Gevonden: %s",
    G + "geur_geleerd": "Nieuwe geur in je snuffelboekje: %s (%s). Je kent nu %s geuren. Vahoeg!",
    G + "geur.proef_botje": "Een oud botje",
    G + "geur.proef_bal": "Een stuiterbal",
    G + "geur.proef_zeelucht": "Zilte zeelucht",
    # ranks
    G + "rang.snuffelpup": "Snuffelpup",
    G + "rang.snuffelneus": "Snuffelneus",
    G + "rang.snuffelspeurder": "Snuffelspeurder",
    G + "rang.snuffelmeester": "Snuffelmeester",
    G + "rang.opper_snuffelmeester": "Opper-Snuffelmeester",
    G + "rang.regel.laagste": "%s (rang %s (laagste) van %s (hoogste))",
    G + "rang.regel": "%s (rang %s van %s; 1 is de laagste)",
    G + "rang.regel.hoogste": "%s (rang %s (hoogste) van %s)",
    G + "rang.lijst": "%s. %s (%s geuren)",
    G + "rang.lijst_later": "%s. %s (%s geuren, later)",
    G + "rang.jouw": "Jouw rang: %s. Geleerde geuren: %s",
    G + "rang.omhoog": "Je neus is gegroeid! Je bent nu %s. Njeg!",
    # deeds, the tree, exams
    G + "daad_gedaan": "Goede daad: %s. Dat is nummer %s, njeg!",
    G + "nodig.daden": "Goede daden voor de dorpelingen",
    G + "boom.stap.0": "nog een kaal plekje",
    G + "boom.stap.1": "een kiem",
    G + "boom.stap.2": "een scheutje",
    G + "boom.stap.3": "een struikje",
    G + "boom.stap.4": "een jong boompje",
    G + "boom.groeit.1": "Er komt een kiempje op bij de stenenkrans!",
    G + "boom.groeit.2": "Het kiempje is een scheutje geworden!",
    G + "boom.groeit.3": "Het scheutje is een struikje geworden!",
    G + "boom.groeit.4": "Het struikje is een jong boompje geworden. Vahoeg!",
    G + "examen.start": "%s begint: snuffel de %s geuren op. Neem de tijd, zakken bestaat niet, njeg",
    G + "examen.voortgang": "Examen: %s van de %s gevonden",
    G + "examen.geslaagd": "Geslaagd voor %s! VAHOEG!",
    G + "examen.hud": "%s: %s / %s",
    # the Guhstation and the memory card
    G + "guhstation.onbekend": "Het schermpje blijft zwart. Dit kastje hoort bij het Snuffeleiland: zoek eerst een steigerhuisje aan het water, njeg",
    G + "guhstation.titel": "HET SNUFFEL EILAND",
    G + "guhstation.start": "Druk op start",
    G + "guhstation.geuren": "Geleerde geuren: %s",
    G + "guhstation.nee": "Nee ik wil even niet snuffelen, njeg",
    G + "pauze.titel": "Even pauze",
    G + "pauze.naar_huis": "Opslaan en naar huis",
    G + "pauze.verder": "Verder spelen",
    G + "pauze.boekje": "Snuffelboekje",
    G + "pauze.stand": "Geuren: %s   Goede daden: %s",
    G + "optie.naar_huis": "Vaar me maar naar huis",
    G + "optie.blijven": "Ik blijf nog even",
    # the keys on the screen
    G + "toets.snuffel": "[%s] ingedrukt: snuffelen",
    G + "toets.graaf": "[%s] graven",
    G + "toets.zit": "[%s] zitten   [%s] kwispelen",
    G + "toets.blaf": "[%s] blaffen. Njeg!",
    G + "toets.boekje": "[%s] snuffelboekje",
    G + "toets.kaart": "Geheugenkaart: opslaan en naar huis",
    # the choice screen
    G + "keuze.titel": "Kies je hond",
    G + "keuze.kop_hond": "Welke hond ben jij?",
    G + "keuze.kop_maatje": "Welk maatje gaat er met je mee?",
    G + "keuze.stap": "%s van %s",
    G + "keuze.ras": "Ras",
    G + "keuze.kleur": "Vacht",
    G + "keuze.naam": "Naam van je hond",
    G + "keuze.draai": "Sleep om je hond te draaien",
    G + "keuze.verder": "Verder »",
    G + "keuze.terug": "« Terug",
    G + "keuze.klaar": "Klaar, njeg!",
    G + "keuze.alleen_jij": "Je maatje is een bosgeestje dat alleen jij kunt zien.",
    # the snuffelboekje
    G + "boekje.titel": "Snuffelboekje",
    G + "boekje.van": "Snuffelboekje van %s",
    G + "boekje.hond": "%s, %s",
    G + "boekje.aantal": "Geleerde geuren: %s",
    G + "boekje.rangen": "Alle vijf de rangen:",
    G + "boekje.later": "Rang 2 tot en met 5 komen in een later verhaal",
    G + "boekje.daden": "Goede daden: %s",
    G + "boekje.boom": "Je boompje: %s",
    G + "boekje.maatje": "Je maatje: %s",
    G + "boekje.diploma": "Snuffeldiploma gehaald. Vahoeg!",
    G + "boekje.geuren": "Wat je neus al kent:",
    G + "boekje.leeg": "Nog niets. Houd de snuffeltoets ingedrukt en volg je neus, njeg",
    G + "boekje.dicht": "Dicht",
    # residents without a role of their own, and the harbour captain's own offer
    "quest.guhs.snuffel.bewoner.hallo": "Woef! Eh, ik bedoel: njeg! Mooi weer om te snuffelen, hè?",
    "quest.guhs.snuffel.bewoner.hallo_pup": "Kef kef! Njeg njeg njeg!",
    "quest.guhs.snuffel.kapitein.vraag": "Ahoi, landrot! Mijn bootje ligt klaar. Zal ik je terugvaren naar huis? Je komt precies uit waar je vandaan "
                                         "kwam, met al je spulletjes. En het eiland loopt niet weg, njeg.",
    "quest.guhs.snuffel.kapitein.geen_hond": "Ahoi! Jij ziet er niet uit als een hond. Hoe ben jij hier verzeild geraakt, njeg?",
    # the growth scene (its title: the replay text of the engine)
    "scene.guhs.snuffel_groei.titel": "Het boompje groeit",
}

STAPPEN = [  # (stapnaam, nu, waar): the questline "snuffeleiland" in the Guhdex; the dock (0-1) and the village (2-8) fill it in
    ("Vind een steigerhuisje", "Zoek een steigerhuisje aan het water. Je superkompas (Mijn verhaal) wijst de weg.",
     "Een steigerhuisje aan een Diepe Guhzee in de Guhmensie"),
    ("Kies je hond en vaar uit", "Kleine Wiebel is ziek en papa is al weken weg. Ga naar het ziekbed in het huisje. Kies daarna bij Kapitein "
     "Zoutsnoet je hond en je maatje, en vaar met hem het water op.",
     "Het steigerhuisje: het ziekbed in het huisje, de kapitein aan het eind van de steiger"),
    ("Aangespoeld", "Je bent een hond! Jutje Kwispel heeft je op het strand gevonden. Loop met haar mee naar het dorp.",
     "Het strand van het Snuffeleiland"),
    ("Naar de dokter", "Vertel Dokter Pleisterpoot wat er thuis aan de hand is.", "Het dokterspraktijkje in Snuffeldorp"),
    ("Snuffelles", "Meester Truffelneus leert je snuffelen. Houd de snuffeltoets ingedrukt en volg je neus. Op de plek zelf graaf je.",
     "De wei net buiten Snuffeldorp"),
    ("Er rommelt iets", "In het dorp valt van alles om, maar niemand ziet wie het doet. Ga eens kijken bij het pleintje.",
     "Het pleintje van Snuffeldorp"),
    ("Goede daden", "De dorpelingen zijn van alles kwijt. Snuffel het voor ze terug: van elke goede daad groeit het boompje van je maatje.",
     "Snuffeldorp"),
    ("Het snuffelexamen", "Je neus is er klaar voor. Doe je examen bij Meester Truffelneus.", "De wei net buiten Snuffeldorp"),
    ("Een spoor van papa", "Je hebt je diploma! Maar wat ruik je daar bij het boompje? Volg je neus.", "Het boompje bij Snuffeldorp"),
]
KORT = {"0": "Zoek een steigerhuisje aan het water", "1": "Ga naar het ziekbed, kies je hond en vaar uit", "2": "Loop met Jutje Kwispel naar het dorp",
        "3": "Praat met Dokter Pleisterpoot", "4": "Volg de snuffelles van Meester Truffelneus", "5": "Kijk wat er rommelt op het pleintje",
        "6": "Doe goede daden voor de dorpelingen", "7": "Doe het snuffelexamen", "8": "Volg je neus bij het boompje"}
VERBORGEN = ["snuffel_eerste_geur", "snuffel_guhstation", "snuffel_diploma"]

FTB_STAPPEN = [  # (titel, tekst, icon) for the steps 1..9 (quest "snuffel_snuffeleiland_<i>", task quest/snuffeleiland_stap_<i>)
    ("Een steigerhuisje", "Aan de &9Diepe Guhzee&r in de &dGuhmensie&r staat hier en daar een &fsteigerhuisje&r vol lampions. Daar is het "
     "lantaarnfeest... maar je kleine broertje of zusje, &6Kleine Wiebel&r, zakt in elkaar. Je &6superkompas&r (Mijn verhaal) wijst de weg.",
     "minecraft:lantern"),
    ("Welke hond ben jij?", "&6Buurvrouw Mandje&r vertelt het bij het ziekbed: papa is al weken weg om de &dgeneesbloem&r te zoeken. Jij gaat "
     "hem achterna! Kies bij &6Kapitein Zoutsnoet&r je &fhond&r (ras, vacht en naam) en het &amaatje&r dat met je meegaat, en vaar uit met "
     "De Natte Neus. Njeg, wat een golven...", "minecraft:oak_boat"),
    ("Aangespoeld!", "Je wordt wakker op het strand van &bHet Snuffeleiland&r, en je hebt... pootjes?! Op het eiland ben je een hond. "
     "&6Jutje Kwispel&r brengt je naar Snuffeldorp. Je eigen spulletjes liggen veilig thuis op je te wachten.", "minecraft:sand"),
    ("Dokter Pleisterpoot", "De oude dorpsdokter weet het zeker: de geneesbloem vind je alleen met een &fechte snuffelneus&r. "
     "Daarvoor moet je naar de wei.", "minecraft:paper"),
    ("Snuffelles", "&6Meester Truffelneus&r leert je snuffelen: houd de snuffeltoets ingedrukt en je neus gaat naar de grond. De &fgeurmeter&r "
     "slaat harder uit hoe dichter je bij bent, met een kleur per soort geur. Op de plek zelf graaf je het op.", "minecraft:bone"),
    ("Wie doet dat toch?", "Er valt een emmer om en niemand ziet waarom. Alleen jij ziet het: een ondeugend &abosgeestje&r! Het wordt "
     "je maatje, en het wijst je voortaan de weg naar geuren.", "minecraft:bucket"),
    ("Goede daden", "De bakker, de visser, de juf, oma en de tuinder zijn iets kwijt. Snuffel het terug! Van elke goede daad leer je "
     "een geur en groeit het &aboompje&r van je maatje een stukje.", "minecraft:oak_sapling"),
    ("Het snuffelexamen", "Tijd voor je examen bij Meester Truffelneus. Zakken bestaat niet: je neus mag er zo lang over doen als hij "
     "wil. Dan ben je een echte &6Snuffelpup&r (rang 1 van 5).", "minecraft:writable_book"),
    ("Een spoor van papa", "Bij het boompje ruik je iets bekends... papa is hier geweest! Het lost nog niets op, maar je weet nu waar je "
     "moet zoeken. Je krijgt het &8Guhstation&r: daarmee ga je terug naar het eiland wanneer je wilt. Wordt vervolgd, njeg!", "guhs:guhstation"),
]


def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    for event, text in SUBTITLES.items():
        h.lang(f"subtitles.guhs.{event}", text, text)
    for name in VERBORGEN:
        bbq2.verborgen(h, name)
    verhaal_motor.groep(h, "snuffeleiland", "Het Snuffeleiland")
    verhaal_motor.verhaallijn(
        h, "snuffeleiland", "Het Snuffeleiland",
        "Je broertje of zusje is ziek en alleen de geneesbloem kan helpen. Op het Snuffeleiland ben je een hond en leer je snuffelen, njeg.",
        STAPPEN,
        klaar=("Je bent een echte Snuffelpup en je hebt een spoor van papa. Met het Guhstation ga je terug naar het eiland wanneer je wilt. "
               "Wordt vervolgd, njeg!", "Het Snuffeleiland"),
        kort=KORT)


def tags(h):
    # what a dog can use with a right-click on the island (the island slice adds its own blocks to this tag)
    h.add_tag("guhs/tags/block/snuffel_bruikbaar", ["#minecraft:doors", "#minecraft:trapdoors", "#minecraft:fence_gates", "#minecraft:buttons",
                                                    "#minecraft:pressure_plates", "minecraft:bell", "minecraft:lever"])
    h.add_tag("guhs/tags/item/loaned", ["guhs:snuffel_geheugenkaart"])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:guhstation"])


# =====================================================================================================================
# FTB quests
# =====================================================================================================================
def ftb(fq):
    wereld.ftb_questlijn(fq, NAME, "snuffeleiland", FTB_STAPPEN, eind=(("guhs:kaas_knabbels", 16),))


# =====================================================================================================================
# self-check
# =====================================================================================================================
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "snuffel")
DIER_ANIMS = ("idle", "walk", "snuffel", "snuffel_loop", "zit", "kwispel", "graaf", "blaf")


def java_sleutels():
    """Every lang key that the Java of the kern names in full ("gui.guhs.snuffel.x", "quest.guhs.snuffel.x", "key.guhs.snuffel.x")."""
    import re
    keys = set()
    for map_, _, files in os.walk(JAVA):
        for f in files:
            if f.endswith(".java") and not f.endswith("GameTests.java"):
                src = open(os.path.join(map_, f), encoding="utf-8").read()
                for m in re.finditer(r'"((?:gui|quest|key|scene)\.guhs\.snuffel[\w.]*)"', src):
                    if not m.group(1).endswith("."):
                        keys.add(m.group(1))
    return keys


def selfcheck(h, json_uit, texturen, rassen, bewoners, data):
    problems = list(modellen.check(h))
    for pad, d in json_uit.items():
        naam = os.path.basename(pad).split(".")[0]
        if pad.startswith("animations/") and "maatje" not in naam and "boompje" not in naam:
            for a in DIER_ANIMS:
                if a not in d["animations"]:
                    problems.append(f"{naam}: no animation {a}")
        if pad.startswith("animations/") and "maatje" in naam:
            for a in ("idle", "blij", "ondeugend", "wijs"):
                if a not in d["animations"]:
                    problems.append(f"{naam}: no animation {a}")
        if not os.path.exists(f"{h.A}/geckolib/{pad}"):
            problems.append(f"missing file {pad}")
    for naam in texturen:
        if not os.path.exists(os.path.join(h.TEX, "entity", f"{naam}.png")):
            problems.append(f"missing texture {naam}")
    # every dog Java can ask for has its three files
    for ras, r in rassen.items():
        for pup in ((False, True) if r["pup"] else (False,)):
            model = f"snuffelhond_{ras}" + ("_pup" if pup else "")
            if f"models/entity/{model}.geo.json" not in json_uit:
                problems.append(f"no model {model}")
            for kleur in r["kleuren"]:
                if f"{model}_{kleur}" not in texturen:
                    problems.append(f"no texture {model}_{kleur}")
        if not (0.4 <= r["oog"] < r["hoogte"] <= MAX_HOOGTE):
            problems.append(f"{ras}: eyes {r['oog']} / height {r['hoogte']}")
    for id in bewoners:
        if f"models/entity/snuffel_{id}.geo.json" not in json_uit or f"snuffel_{id}" not in texturen:
            problems.append(f"resident {id} has no model or texture")
    # the island's data only names what exists
    geuren = {g["id"] for g in data["geuren"]}
    for b in data["geurbronnen"]:
        if b["geur"] not in geuren:
            problems.append(f"geurbron {b['id']}: unknown scent {b['geur']}")
    for g in data["geuren"]:
        if f"gui.guhs.snuffel.geur.{g['id']}" not in h.NL:
            problems.append(f"scent {g['id']} has no name")
    for b in data["bewoners"]:
        if b.get("bewoner") and b["bewoner"] not in bewoners:
            problems.append(f"resident {b['sleutel']}: unknown name {b['bewoner']}")
        if not b.get("bewoner") and (b.get("ras") not in rassen or b.get("kleur") not in rassen[b["ras"]]["kleuren"]):
            problems.append(f"resident {b['sleutel']}: unknown breed or coat")
    # the sea of the dimension is the sea Java puts back, and the test island stands on its floor
    src = open(os.path.join(JAVA, "Eiland.java"), encoding="utf-8").read()
    if f"ZEE = {bouw.ZEE}, ZAND = {bouw.ZAND}, STEEN = {bouw.STEEN};" not in src:
        problems.append("Eiland.java ZEE / ZAND / STEEN differ from snuffel_bouw")
    for key in sorted(java_sleutels() | set(LANG) | {f"subtitles.guhs.{e}" for e in SOUNDS}):
        if key not in h.NL:
            problems.append(f"missing lang {key}")
    for p in ([f"{h.D}/{f}" for f in ("snuffel/honden.json", "snuffel/eiland.json", "structure/snuffel/eiland.nbt", "structure/snuffel_test_eiland.nbt",
                                      "dimension/snuffeleiland.json", "dimension_type/snuffeleiland.json", "worldgen/biome/snuffeleiland.json",
                                      "advancement/quest/snuffeleiland_stap_9.json", "loot_table/blocks/guhstation.json")]
              + [f"{h.A}/{f}" for f in ("blockstates/guhstation.json", "models/block/guhstation.json", "models/item/guhstation.json",
                                        "models/item/snuffel_geheugenkaart.json", "models/item/snuffel_bloesemtakje.json")]
              + [os.path.join(h.TEX, *f) for f in (("block", "guhstation_boven.png"), ("block", "guhstation_voor.png"), ("block", "guhstation_zij.png"),
                                                   ("item", "snuffel_geheugenkaart.png"), ("item", "snuffel_bloesemtakje.png"),
                                                   ("gui", "snuffel", "guhstation_logo.png"))]):
        if not os.path.exists(p):
            problems.append(f"missing file {p}")
    geluiden = json.load(open(f"{h.A}/sounds.json", encoding="utf-8"))
    for e in SOUNDS:
        if e not in geluiden:
            problems.append(f"sounds.json misses {e}")
    if problems:
        raise SystemExit("snuffel self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    json_uit, texturen = schrijf_modellen(h)
    rassen, bewoners = honden(h)
    dimensie(h)
    data = eiland(h)
    tex.build(h)
    sounds(h)
    texts(h)
    tags(h)
    selfcheck(h, json_uit, texturen, rassen, bewoners, data)
