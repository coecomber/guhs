"""
Het Guhdoolhof (2.9, De Grote Guhspelen; Java: nl.juiced.guhs.feature.doolhof).

  - the building guhdoolhof (hedge field, the lookout guh on its bridge, the plaza with Meneer Vadskronkel's kiosk,
    gardens, the finish): tools/features/doolhof_bouw.py (geometry self-check)
  - the pixel art, Meneer Vadskronkel's and the Heg-Mika's looks: tools/features/doolhof_tex.py
  - here: block models, sounds, tags, the explorer's outfit (BONES / clothes / icons / CLOTHES), advancements (tab
    De Grote Guhspelen), lang (Dutch in both files), FTB quests (section "doolhof") and a self-check of the assets.
"""
import json
import os

import numpy as np

from features import doolhof_bouw as bouw
from features import doolhof_tex as tex
from features import sterrenwacht_hulp as hulp

NAME = bouw.NAME
BLOKKEN = ["doolhofheg", "doolhofheg_gezicht", "doolhof_lantaarn"]
ITEMS = ["doolhofknabbel", "gestolen_knabbel"]

# ======================================================================================================================
# the explorer's outfit (Meneer Vadskronkel's shop)
# ======================================================================================================================
_H = [0, 6, -2]
_B = [0, 6, 6]
BONES = {
    # a khaki explorer's hat: a wide brim, a round crown, a green band with pink dots
    "outfit_doolhof_hoedje": ("head", _H, "doolhof_hoedje", [([-6.5, 14.6, -11.2], [13, 0.5, 10.4], 0), ([-4.5, 15.1, -9.4], [9, 2.4, 6.8], 0),
                                                           ([-3.5, 17.5, -8.4], [7, 0.9, 4.8], 0)]),
    "outfit_doolhof_hoedje_band": ("head", _H, "doolhof_band", [([-4.6, 15.1, -9.5], [9.2, 0.8, 7.0], 0)]),
    # a little canvas backpack with a rolled-up blanket on top (just for show: no storage)
    "outfit_doolhof_rugzakje": ("body", _B, "doolhof_rugzakje", [([-3.2, 11.2, 2.0], [6.4, 3.6, 5.2], 0), ([-3.4, 14.4, 1.8], [6.8, 0.6, 3.0], 0),
                                                               ([-2.0, 11.6, 7.1], [4.0, 2.4, 0.6], 0)]),
    "outfit_doolhof_rugzakje_rol": ("body", _B, "doolhof_rol", [([-3.8, 14.8, 5.0], [7.6, 1.5, 1.5], 0)]),
    # a compass on a cord
    "outfit_doolhof_kompas": ("head", _H, "doolhof_koord", [([-4.5, 1.6, -11.5], [9, 0.5, 0.5], 0), ([-4.5, 0.6, -11.5], [0.5, 1, 0.5], 0),
                                                          ([4, 0.6, -11.5], [0.5, 1, 0.5], 0)]),
    "outfit_doolhof_kompas_schijf": ("head", _H, "doolhof_kompas", [([-1.6, -1.8, -12.0], [3.2, 3.2, 0.7], 0)]),
}
CLOTHES = ["doolhof_hoedje", "doolhof_rugzakje", "doolhof_kompas"]


def clothes(rng, v):
    def hoed():
        a = v.fabric((218, 198, 140), rng, 7)
        for y in range(0, 32, 6):
            a[y, :] = a[y, :] * 0.94
        return a

    def band():
        return v.dots((70, 134, 72), (250, 150, 196), rng, every=5, size=1)

    def rugzak():
        a = v.fabric((150, 108, 70), rng, 7)
        for x in range(2, 32, 6):                                  # stitches
            a[::3, x] = (218, 190, 140)
        a[12:20, 12:20] = (120, 84, 54)                            # a pocket with a guh-flower
        a[15:17, 15:17] = (250, 150, 196)
        return a

    def rol():
        return v.stripes((70, 134, 72), (250, 196, 222), rng, 3)

    def koord():
        return v.fabric((120, 84, 54), rng, 6)

    def kompas():
        a = np.zeros((32, 32, 3), np.float32)
        for y in range(32):
            for x in range(32):
                d = ((x - 15.5) ** 2 + (y - 15.5) ** 2) ** 0.5
                a[y, x] = (240, 196, 70) if d > 11 else (250, 248, 238)
        for i in range(10):                                         # the needle: pink to the north, grey to the south
            a[5 + i, 15:17] = (236, 104, 164)
            a[16 + i, 15:17] = (120, 120, 132)
        a[14:18, 14:18] = (60, 50, 70)
        return a

    return {
        "doolhof_hoedje": {"doolhof_hoedje": hoed, "doolhof_band": band},
        "doolhof_rugzakje": {"doolhof_rugzakje": rugzak, "doolhof_rol": rol},
        "doolhof_kompas": {"doolhof_koord": koord, "doolhof_kompas": kompas},
    }


def icons(ic):
    hoed = ["....aaaaaa......", "...abbbbbba.....", "..abbbbbbbba....", "..acccccccca....", "aaaaaaaaaaaaaa..",
            "abbbbbbbbbbbba..", "aaaaaaaaaaaaaa.."]
    rugzak = ["...addddda......", "..addddddda.....", "..abbbbbbba.....", ".abbbbbbbbba....", ".abbacccabba....", ".abbacecabba....",
              ".abbacccabba....", ".abbbbbbbbba....", ".abbbbbbbbba....", "..aaaaaaaaa....."]
    kompas = ["..a........a....", "...a......a.....", "....a....a......", ".....aaaa.......", "....abbbba......", "...abccbbba.....",
              "...abbcbbba.....", "...abbbdbba.....", "....abbdba......", ".....aaaa......."]
    return {
        "doolhof_hoedje": ic.icon(ic.pad(hoed), {"a": (120, 96, 60), "b": (218, 198, 140), "c": (70, 134, 72)}),
        "doolhof_rugzakje": ic.icon(ic.pad(rugzak), {"a": (90, 60, 36), "b": (150, 108, 70), "c": (120, 84, 54), "d": (70, 134, 72),
                                                     "e": (250, 150, 196)}),
        "doolhof_kompas": ic.icon(ic.pad(kompas), {"a": (150, 110, 40), "b": (250, 248, 238), "c": (236, 104, 164), "d": (120, 120, 132)}),
    }


# ======================================================================================================================
# blocks, items, sounds, tags
# ======================================================================================================================
SOUNDS = {  # event: (sound, pitch, volume)
    "doolhof.giechel": ("guhs:entity.mika.ambient", 1.6, 0.9),
    "doolhof.knabbel": ("minecraft:entity.item.pickup", 1.4, 0.9),
    "doolhof.groei": ("minecraft:block.azalea_leaves.place", 0.8, 1.0),
}


def blocks_and_items(h):
    A = h.A
    # the hedge (two textures, turned at random so the field doesn't look tiled)
    for name in ("doolhofheg", "doolhofheg_b"):
        h.w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"guhs:block/{name}"}})
    h.w(f"{A}/blockstates/doolhofheg.json", {"variants": {"": [
        {"model": "guhs:block/doolhofheg"}, {"model": "guhs:block/doolhofheg_b"}, {"model": "guhs:block/doolhofheg", "y": 90},
        {"model": "guhs:block/doolhofheg_b", "y": 180}, {"model": "guhs:block/doolhofheg", "x": 180}]}})
    h.w(f"{A}/models/item/doolhofheg.json", {"parent": "guhs:block/doolhofheg"})
    # the hedge with a face (the face looks north in the model)
    h.w(f"{A}/models/block/doolhofheg_gezicht.json", {"parent": "minecraft:block/orientable", "textures": {
        "front": "guhs:block/doolhofheg_gezicht", "side": "guhs:block/doolhofheg", "top": "guhs:block/doolhofheg_b"}})
    h.w(f"{A}/blockstates/doolhofheg_gezicht.json", {"variants": hulp.facing_variants("guhs:block/doolhofheg_gezicht")})
    h.w(f"{A}/models/item/doolhofheg_gezicht.json", {"parent": "guhs:block/doolhofheg_gezicht"})
    # the guh-ear lantern (off / lit)
    for lit in (False, True):
        t = "guhs:block/doolhof_lantaarn_aan" if lit else "guhs:block/doolhof_lantaarn"
        h.w(f"{A}/models/block/doolhof_lantaarn{'_aan' if lit else ''}.json", {
            "parent": "minecraft:block/block", "textures": {"particle": t, "l": t},
            "elements": [hulp.el([5, 0, 5], [11, 1, 11], "#l", uv=[0, 0, 6, 1]),
                         hulp.el([5, 1, 5], [11, 7, 11], "#l", uv=[3, 3, 13, 13]),
                         hulp.el([4, 7, 4], [12, 8, 12], "#l", uv=[0, 0, 8, 1]),
                         hulp.el([5, 8, 7], [7, 10, 9], "#l", uv=[0, 0, 2, 2]),
                         hulp.el([9, 8, 7], [11, 10, 9], "#l", uv=[0, 0, 2, 2])]})
    h.w(f"{A}/blockstates/doolhof_lantaarn.json", {"variants": {
        "lit=false": {"model": "guhs:block/doolhof_lantaarn"}, "lit=true": {"model": "guhs:block/doolhof_lantaarn_aan"}}})
    h.w(f"{A}/models/item/doolhof_lantaarn.json", {"parent": "guhs:block/doolhof_lantaarn_aan"})
    # the invisible anchor
    h.w(f"{A}/models/block/doolhof_anker.json", {"textures": {"particle": "minecraft:block/green_stained_glass"}})
    h.w(f"{A}/blockstates/doolhof_anker.json", {"variants": {f"facing={f}": {"model": "guhs:block/doolhof_anker"}
                                                            for f in ("north", "east", "south", "west")}})
    for i in ITEMS:
        h.item_model(i)
    for b in BLOKKEN:
        h.self_drop(b)
    h.shaped("doolhofheg", ["LL", "LL"], {"L": "#minecraft:leaves"}, "guhs:doolhofheg", 2)
    h.shaped("doolhof_lantaarn", [" I ", "ITI", " I "], {"I": "minecraft:pink_dye", "T": "minecraft:lantern"}, "guhs:doolhof_lantaarn", 2)
    h.add_tag("minecraft/tags/block/mineable/hoe", ["guhs:doolhofheg", "guhs:doolhofheg_gezicht"])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:doolhof_lantaarn"])
    h.add_tag("guhs/tags/item/loaned", ["guhs:gestolen_knabbel"])
    geluiden(h, SOUNDS)


def geluiden(h, sounds):
    """Our sound events in sounds.json (idempotent): each plays an existing sound with its own pitch."""
    def patch(d):
        for event, (sound, pitch, volume) in sounds.items():
            d[event] = {"sounds": [{"name": sound, "type": "event", "pitch": pitch, "volume": volume}], "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# ======================================================================================================================
# advancements: the tab De Grote Guhspelen + the hidden ones for FTB
# ======================================================================================================================
QUEST = ["doolhof_gevonden", "doolhof_gespeeld", "doolhof_makkelijk", "doolhof_medium", "doolhof_lastig", "doolhof_ongepikt"]
TAB = [  # name, parent, icon, frame, title, description
    ("doolhof_gevonden", "root", "guhs:doolhofheg_gezicht", "task", "Verdwaald in de heg",
     "Vind Het Guhdoolhof in de Guhvelden (en zeg hallo tegen Meneer Vadskronkel)"),
    ("doolhof_gespeeld", "doolhof_gevonden", "guhs:gestolen_knabbel", "task", "Alle knabbels terug!",
     "Vind alle gestolen kaasknabbels in het doolhof en ren naar de uitgang"),
    ("doolhof_lastig", "doolhof_gespeeld", "guhs:doolhof_lantaarn", "goal", "Doolhofmeester",
     "Haal het lastige doolhof: zestien knabbels, drie snelle Mika's en nepknabbels"),
    ("doolhof_ongepikt", "doolhof_lastig", "guhs:doolhofknabbel", "challenge", "Niet te pakken!",
     "Haal een medium of lastig doolhof zonder dat een Mika ook maar één knabbel pikt"),
    ("doolhof_kleding", "doolhof_gespeeld", "guhs:doolhof_hoedje", "task", "Op ontdekkingsreis",
     "Koop het hele ontdekkingspakje bij Meneer Vadskronkel: hoedje, kompas en rugzakje"),
]


def advancements(h, name, quest, tab, clothes_ids):
    """(shared with knabbelspelen.py) hidden quest advancements + the visible ones in the tab De Grote Guhspelen."""
    D = h.D
    for q in quest:
        h.w(f"{D}/advancement/quest/{q}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for adv, parent, icon, frame, title, desc in tab:
        if adv.endswith("_gevonden"):
            crit = {"gevonden": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": f"guhs:{name}"}}}}}
        elif adv.endswith("_kleding"):
            crit = {c: {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"guhs:{c}"}]}} for c in clothes_ids}
        else:
            crit = {"done": {"trigger": "minecraft:impossible"}}
        h.w(f"{D}/advancement/grote_guhspelen/{adv}.json", {
            "parent": f"guhs:grote_guhspelen/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.grote_guhspelen.{adv}.title"},
                        "description": {"translate": f"advancements.guhs.grote_guhspelen.{adv}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": crit})
        h.lang(f"advancements.guhs.grote_guhspelen.{adv}.title", title, title)
        h.lang(f"advancements.guhs.grote_guhspelen.{adv}.description", desc, desc)


# ======================================================================================================================
# lang (Dutch in both files)
# ======================================================================================================================
LANG = {
    # names
    "entity.guhs.guh_npc.doolhofguh": "Meneer Vadskronkel",
    "gui.guhs.guhdex.rarity.doolhofguh": "Zeldzaamheid: Uniek (Het Guhdoolhof, Guhvelden)",
    "gui.guhs.guhdex.info.doolhofguh": "Meneer Vadskronkel knipt al zijn hele leven heggen, en zijn snor krult net zo mooi als zijn doolhof. "
                                       "De Mika's verstoppen er steeds weer gestolen kaasknabbels in, en dan zijn de guhs niet VAHOEG genoeg meer. "
                                       "Voor elk spelletje laat hij een gloednieuw doolhof groeien, en de knabbels laat hij glinsterend boven de "
                                       "heggen zweven: zo zie je ze van ver. Verdwalen mag, vads!",
    "entity.guhs.doolhof_mika": "Heg-Mika",
    "block.guhs.doolhofheg": "Doolhofheg",
    "block.guhs.doolhofheg_gezicht": "Doolhofheg met guhgezicht",
    "block.guhs.doolhof_lantaarn": "Guhoor-lantaarntje",
    "block.guhs.doolhof_anker": "Doolhof-anker",
    "item.guhs.doolhofknabbel": "Doolhofknabbel",
    "item.guhs.gestolen_knabbel": "Gestolen kaasknabbel",
    "item.guhs.gestolen_knabbel.lore": "Teruggevonden in het doolhof! Breng hem naar de uitgang (Meneer Vadskronkel zorgt dat hij terugkomt bij de guhs).",
    "item.guhs.doolhof_hoedje": "Ontdekkingshoedje",
    "item.guhs.doolhof_rugzakje": "Ontdekkingsrugzakje",
    "item.guhs.doolhof_kompas": "Doolhofkompasje",
    "structure.guhs.guhdoolhof": "Het Guhdoolhof",
    "structure.guhs.guhdoolhof.tooltip": "Een reusachtig heggendoolhof met een uitkijkguh in het midden (Guhvelden)",
    "subtitles.guhs.doolhof.giechel": "Heg-Mika giechelt",
    "subtitles.guhs.doolhof.knabbel": "Kaasknabbel gevonden",
    "subtitles.guhs.doolhof.groei": "Heggen groeien",
    "gui.guhs.scorebord.doolhof": "Het Guhdoolhof",
    # the screen
    "gui.guhs.doolhof.rules": "De Mika's hebben kaasknabbels gestolen en ze in mijn doolhof verstopt! Kies een niveau: ik laat een gloednieuw "
                              "doolhof groeien. Zoek alle knabbels en ren dan naar de uitgang in het noorden. Heg-Mika's pikken een knabbel terug "
                              "als ze je aantikken (au? nee hoor, ze giechelen alleen). De echte knabbels zweven glinsterend boven de heggen: "
                              "je ziet ze van ver, maar de weg ernaartoe moet je zelf vinden. Hoe sneller, hoe vahoeger!",
    "gui.guhs.doolhof.niveau.tooltip": "%s kaasknabbels, %s Heg-Mika('s). %s",
    "gui.guhs.doolhof.niveau.makkelijk": "Een klein doolhofje met één trage Mika, en onderin zie je hoeveel knabbels er nog verstopt zijn. Lekker rustig, vads.",
    "gui.guhs.doolhof.niveau.medium": "Een flink doolhof met twee Mika's.",
    "gui.guhs.doolhof.niveau.lastig": "Het hele heggenveld, drie snelle Mika's en nepknabbels in de doodlopende gangetjes. Njeg!",
    "gui.guhs.doolhof.shop": "Winkeltje",
    "gui.guhs.doolhof.shop.tooltip": "Het ontdekkingspakje en heggenspulletjes voor doolhofknabbels",
    "gui.guhs.doolhof.busy": "%s zit nu in het doolhof (%s, %s/%s knabbels, %s). Kijk mee vanaf de uitkijkguh op de brug!",
    "gui.guhs.doolhof.kapot": "Njeg... mijn heggenveld is zoek. Hier kan ik geen doolhof laten groeien.",
    "gui.guhs.doolhof.first": "Je eerste keer? Begin met makkelijk, dan leer je de Mika's kennen.",
    "gui.guhs.doolhof.free": "Het doolhof is vrij. Welk niveau durf jij aan?",
    "gui.guhs.doolhof.geen_tijd": "nog geen tijd",
    "gui.guhs.doolhof.jouw_tijd": "jouw tijd: %s",
    "gui.guhs.doolhof.niet_meppen": "Hihi, niet meppen! Een Heg-Mika is veel te snel.",
    "gui.guhs.doolhof.no_build": "Dit is het doolhof van Meneer Vadskronkel: alleen hij knipt hier, njeg!",
    # Meneer Vadskronkel talks
    "quest.guhs.doolhof.hello": "Goedemiddag! Ik ben Meneer Vadskronkel, heggenknipper. De Mika's hebben weer kaasknabbels in mijn doolhof verstopt, "
                                "en nu zijn de guhs niet VAHOEG genoeg... Help je me ze terug te vinden?",
    "quest.guhs.doolhof.hello_again": "Daar ben je weer! Zin in een nieuw doolhof? Ik knip er zo eentje voor je.",
    "quest.guhs.doolhof.busy": "Sssst, %s zit in het doolhof! Kijk maar mee vanaf de uitkijkguh.",
    "quest.guhs.doolhof.busy_you": "Jij zit in het doolhof! Zoek die knabbels, vads!",
    "quest.guhs.doolhof.broken": "Njeg... ik kan mijn heggenveld niet vinden.",
    "quest.guhs.doolhof.shop": "Mijn winkeltje! Met het ontdekkingspakje verdwaal je in stijl.",
    "quest.guhs.doolhof.go.makkelijk": "Een klein doolhofje! %s kaasknabbels en %s trage Mika. Ik laat het groeien... ssst, luister maar!",
    "quest.guhs.doolhof.go.medium": "Een flink doolhof! %s kaasknabbels en %s Mika's. Ik laat het groeien...",
    "quest.guhs.doolhof.go.lastig": "Het HELE heggenveld! %s kaasknabbels, %s snelle Mika's en pas op voor nepknabbels. Daar gaan we...",
    "quest.guhs.doolhof.opzij": "Even opzij, de heggen groeien! Meneer Vadskronkel zet je op het plein.",
    "quest.guhs.doolhof.klaar_staan": "Het doolhof staat! Zoek %s kaasknabbels (ze zweven glinsterend boven de heggen) en ren naar de uitgang in het noorden.",
    "quest.guhs.doolhof.ready": "Het doolhof ruikt naar kaas...",
    "quest.guhs.doolhof.title.go": "ZOEKEN!",
    "quest.guhs.doolhof.title.go.sub": "Vind %s kaasknabbels",
    "quest.guhs.doolhof.bar": "Tijd %s · %s/%s kaasknabbels",
    "quest.guhs.doolhof.bar_makkelijk": "Tijd %s · %s/%s kaasknabbels · nog %s verstopt, njeg!",
    "quest.guhs.doolhof.gevonden": "Kaasknabbel! %s/%s",
    "quest.guhs.doolhof.alles": "Alle knabbels! Nu naar de uitgang, VAHOEG!",
    "quest.guhs.doolhof.nep": "Nep! Een Mika-lokaasje van plastic... +%s seconden, njeg!",
    "quest.guhs.doolhof.gepikt": "Hihi! Een Heg-Mika pikte een knabbel en verstopte hem ergens anders! (%s/%s)",
    "quest.guhs.doolhof.tong": "De Heg-Mika steekt zijn tong uit: je had nog niks, hihi!",
    "quest.guhs.doolhof.nog_niet": "Wacht even! Je mist nog %s kaasknabbels. Terug het doolhof in, vads!",
    "quest.guhs.doolhof.niet_klimmen": "Niet op de heggen klimmen, njeg! Terug naar beneden.",
    "quest.guhs.doolhof.weggelopen": "Je liep het doolhof uit. De Mika's houden de knabbels... voor nu.",
    "quest.guhs.doolhof.te_lang": "Het is al zo laat! Meneer Vadskronkel roept je terug. Volgende keer sneller, vads.",
    "quest.guhs.doolhof.stopped": "Doolhof gestopt. De Mika's giechelen nog na.",
    "quest.guhs.doolhof.done": "Uit het doolhof (%s)! Tijd %s, %s keer gepikt, %s nepknabbels.",
    "quest.guhs.doolhof.munten": "Je verdient %s doolhofknabbels!",
    "quest.guhs.doolhof.record": "Nieuw record: %s!",
    "quest.guhs.doolhof.best": "Je record is %s.",
    "quest.guhs.doolhof.title.record": "NIEUW RECORD!",
    "quest.guhs.doolhof.title.end": "Eruit!",
    "quest.guhs.doolhof.end.snel": "Wat snel! Alle knabbels terug, de guhs zijn weer helemaal VAHOEG.",
    "quest.guhs.doolhof.end.ok": "Gevonden, allemaal! Volgende keer nog sneller, vads.",
    "quest.guhs.doolhof.invite1": "Wie durft mijn doolhof in? Ik laat er zo eentje groeien!",
    "quest.guhs.doolhof.invite2": "Psst... er liggen kaasknabbels in de heggen. Kom je zoeken?",
    "quest.guhs.doolhof.invite3": "Mijn snor kriebelt: er zitten Mika's in het doolhof! Help je me?",
}


def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)


# ======================================================================================================================
# the self-check of our own assets
# ======================================================================================================================
def selfcheck(h, prefix, blokken, items, clothes_ids, textures, geos, lang):
    """(shared with knabbelspelen.py) block states, item models, textures of our models, lang keys."""
    A = h.A
    missing = []
    for b in blokken:
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block {b}")
    for i in items + clothes_ids:
        if i not in blokken and f"item.guhs.{i}" not in h.NL:
            missing.append(f"lang item {i}")
    for i in items:
        if not os.path.exists(f"{A}/models/item/{i}.json"):
            missing.append(f"item model {i}")
    for root, _dirs, files in os.walk(f"{A}/models"):
        for f in files:
            if f.startswith(prefix) or f[:-5] in items or f[:-5] in blokken:
                model = json.load(open(os.path.join(root, f), encoding="utf-8"))
                for t in model.get("textures", {}).values():
                    if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                        missing.append(f"texture {t} ({f})")
    for t in textures:
        if not os.path.exists(f"{A}/textures/{t}.png"):
            missing.append(f"texture {t}")
    for g in geos:
        if not os.path.exists(f"{A}/geo/entity/{g}.geo.json"):
            missing.append(f"geo {g}")
    for key in lang:
        if key not in h.NL or key not in h.EN:
            missing.append(f"lang {key}")
    if missing:
        raise SystemExit(f"{prefix} assets missing: {missing}")


def build(h):
    tex.build(h)
    blocks_and_items(h)
    advancements(h, NAME, QUEST, TAB, CLOTHES)
    texts(h)
    h.w(f"{h.D}/advancement/quest/seen_doolhofguh.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    b = bouw.Bouw(h).bouw()
    problems, walkable = bouw.check(b)
    if problems:
        raise SystemExit("guhdoolhof geometry check failed:\n  " + "\n  ".join(problems[:40]))
    none = {"bounding_box": "piece", "spawns": []}
    h.TEMPLATE_SIZES[NAME] = 48
    h.FLATNESS[NAME] = 14
    h.structure(NAME, ["guh_fields"], spacing=44, separation=16, salt=20290201, start_y=-bouw.G, reach=80, centre=bouw.ANCHOR,
                spawn_overrides={"monster": none, "ambient": none})
    b.s.save(NAME)
    selfcheck(h, "doolhof", BLOKKEN + ["doolhof_anker"], ITEMS + BLOKKEN, CLOTHES,
              ["entity/npc_doolhofguh", "entity/doolhof_mika", "item/doolhofknabbel", "item/gestolen_knabbel"],
              ["guh_npc_doolhofguh", "doolhof_mika"], LANG)
    print(f"doolhof: building ok ({len(b.s.blocks)} blocks, {walkable} walkable, {b.gezichten} guh faces)")


# ======================================================================================================================
# FTB quests (section "doolhof" of the chapter Minigames & bijzondere plekken)
# ======================================================================================================================
def ftb(fq):
    q, y = fq.q, 0
    q("doolhof_vinden", "Het Guhdoolhof", "In de &dGuhvelden&r staat een reusachtig heggendoolhof met een uitkijkguh in het midden. "
      "Het superkompas (Minigames) wijst de weg. Zeg hallo tegen &aMeneer Vadskronkel&r bij zijn kiosk op het plein.",
      "guhs:doolhofheg_gezicht", [fq.structure(NAME)], rewards=(("guhs:kaas_knabbels", 8),), x=0, y=y, shape="circle", xp=100)
    q("doolhof_makkelijk", "Een klein doolhofje", "Kies &amakkelijk&r: Meneer Vadskronkel laat een nieuw doolhof groeien. Vind de acht "
      "gestolen kaasknabbels (ze zweven glinsterend boven de heggen, en onderin zie je hoeveel er nog verstopt zijn) en ren naar de "
      "uitgang in het noorden. Pas op voor de trage Heg-Mika!",
      "guhs:gestolen_knabbel", [fq.adv("doolhof_makkelijk")], rewards=(("guhs:doolhofknabbel", 2),), deps=["doolhof_vinden"], x=1.5, y=y, xp=100)
    q("doolhof_medium", "Twaalf knabbels", "Haal het &emedium&r doolhof: twaalf knabbels en twee Heg-Mika's die er één terugpikken als ze je aantikken.",
      "guhs:doolhofheg", [fq.adv("doolhof_medium")], rewards=(("guhs:doolhofknabbel", 3),), deps=["doolhof_makkelijk"], x=3, y=y, xp=150)
    q("doolhof_lastig", "Doolhofmeester", "Het &clastige&r doolhof: het hele heggenveld, zestien knabbels, drie snelle Mika's en nepknabbels "
      "in de doodlopende gangetjes (die kosten tijd!). Echte knabbels glinsteren een beetje...",
      "guhs:doolhof_lantaarn", [fq.adv("doolhof_lastig")], rewards=(("guhs:doolhofknabbel", 5),), deps=["doolhof_medium"], x=4.5, y=y,
      shape="gear", xp=300)
    q("doolhof_ongepikt", "Niet te pakken!", "Haal een medium of lastig doolhof zonder dat een Mika ook maar één knabbel pikt.",
      "guhs:doolhofknabbel", [fq.adv("doolhof_ongepikt")], rewards=(("guhs:vahoege_vads_ingot", 1),), deps=["doolhof_medium"], x=4.5, y=y + 1.5,
      shape="hexagon", xp=300)
    q("doolhof_pakje", "Op ontdekkingsreis", "Koop het ontdekkingshoedje, het doolhofkompasje en het ontdekkingsrugzakje bij Meneer Vadskronkel "
      "(doolhofknabbels) en ontgrendel ze voor je guhs.", "guhs:doolhof_hoedje",
      [fq.item("guhs:doolhof_hoedje"), fq.item("guhs:doolhof_kompas"), fq.item("guhs:doolhof_rugzakje")],
      rewards=(("guhs:doolhofknabbel", 2),), deps=["doolhof_makkelijk"], x=1.5, y=y + 1.5, xp=150)
