"""
Guhpixel slice "bioscoop" (Java: feature/guhpixel/bioscoop; namespace guhbioscoop; English: tools/lang/en/c37_px_bioscoop.json).

The Guhbioscoop for at home (DESIGN_PX section 4): the projector, the Bioscoopdoek (a screen of up to 7 x 4 blocks), the
Bioscoopstoeltje (players and guhs), the popcornmachine and the bakje popcorn, and the nine films.
  guhpixel_bioscoop_tex.py     the block and item textures, and the sprite library of the films
  guhpixel_bioscoop_film.py    THE FILM FORMAT and the tools to write a film (read its module comment to add a film)
  guhpixel_bioscoop_films.py   the nine films (films1/2/3.py); also a preview without the game
This module: models, blockstates, loot, the sounds, the hidden advancements, the texts of the screens, and the FTB quests.
Everything is bought in the Guhpixel shop (no crafting recipes): the prices are in BioscoopSlice.java.
"""
import json
import os
import re

from features import guhpixel_bioscoop_film as film
from features import guhpixel_bioscoop_films as films
from features import guhpixel_bioscoop_tex as tex
from features import guhpixel_lib as lib

BLOKKEN = ("guhbioscoop_projector", "guhbioscoop_doek", "guhbioscoop_stoeltje", "guhbioscoop_popcornmachine")

TEXTS = {
    # --- blocks and items ---
    "block.guhs.guhbioscoop_projector": "Guhbioscoop-projector",
    "block.guhs.guhbioscoop_projector.lore": "Zet hem neer met de lens naar een Bioscoopdoek. Rechtsklik: kies een van jouw films. Ratelratel!",
    "block.guhs.guhbioscoop_doek": "Bioscoopdoek",
    "block.guhs.guhbioscoop_doek.lore": "Bouw er een scherm van: tot 7 breed en 4 hoog. Hoe groter, hoe mooier, njeg.",
    "block.guhs.guhbioscoop_stoeltje": "Bioscoopstoeltje",
    "block.guhs.guhbioscoop_stoeltje.lore": "Voor jou en voor je guhs: bij een film komen ze er zelf op zitten. Lekker pluche.",
    "block.guhs.guhbioscoop_popcornmachine": "Popcornmachine",
    "block.guhs.guhbioscoop_popcornmachine.lore": "Pof! Met eentje in de zaal krijgt elke guh een bakje popcorn bij de film. Rechtsklik: ook eentje voor jou.",
    "item.guhs.guhbioscoop_popcorn": "Bakje popcorn",
    "item.guhs.guhbioscoop_popcorn.lore": "Nog warm. De helft is al op voordat de film begint, njeg.",
    # --- sounds ---
    "subtitles.guhs.guhbioscoop.projector_aan": "Projector ratelt",
    "subtitles.guhs.guhbioscoop.projector_uit": "Projector klikt uit",
    "subtitles.guhs.guhbioscoop.pop": "Popcorn poft",
    # --- the projector screen ---
    "gui.guhs.guhbioscoop.scherm.titel": "Guhbioscoop",
    "gui.guhs.guhbioscoop.scherm.aantal": "Films: %1$s/%2$s",
    "gui.guhs.guhbioscoop.scherm.doek": "Doek gevonden: %1$s breed, %2$s hoog. Kies maar een film, njeg!",
    "gui.guhs.guhbioscoop.scherm.geen_doek": "De projector ziet geen Bioscoopdoek. Zet er een voor de lens, binnen 16 blokken, met de witte kant naar de projector.",
    "gui.guhs.guhbioscoop.scherm.speel": "Speel",
    "gui.guhs.guhbioscoop.scherm.stop": "Stop",
    "gui.guhs.guhbioscoop.scherm.op_slot": "Op slot",
    "gui.guhs.guhbioscoop.scherm.duur": "%1$s seconden",
    "gui.guhs.guhbioscoop.scherm.duur_gezien": "%1$s seconden · %2$s keer gezien",
    "gui.guhs.guhbioscoop.melding.speelt": "Nu in de zaal: %s. Pak een stoeltje!",
    "gui.guhs.guhbioscoop.melding.gestopt": "De projector staat uit.",
    "gui.guhs.guhbioscoop.melding.geen_projector": "De projector is weg, njeg.",
    "gui.guhs.guhbioscoop.melding.te_ver": "Je staat te ver van de projector, njeg.",
    "gui.guhs.guhbioscoop.melding.onbekend": "Die film ken ik niet, njeg.",
    "gui.guhs.guhbioscoop.melding.op_slot": "Die film heb je nog niet, njeg.",
    "gui.guhs.guhbioscoop.melding.geen_doek": "Zonder Bioscoopdoek valt er niks te zien: zet er een voor de lens, njeg.",
    "gui.guhs.guhbioscoop.einde": "Einde van %s. Vahoeg, wat een film!",
    "gui.guhs.guhbioscoop.stoel.bezet": "Hier zit al een guh. Die laat je lekker zitten, njeg.",
    "gui.guhs.guhbioscoop.popcorn.pak": "Pof! Een bakje popcorn voor jou.",
    "gui.guhs.guhbioscoop.popcorn.wacht": "De machine poft nog. Eerst je eigen bakje leegeten, njeg!",
    "gui.guhs.guhbioscoop.dagboek.film": "Ik heb de film %s gezien! Ik ben maar een klein beetje in slaap gevallen.",
    "gui.guhs.guhbioscoop.dagboek.film_popcorn": "Ik heb de film %s gezien, met popcorn! De popcorn was het spannendst.",
    # --- the shop ---
    "gui.guhs.guhbioscoop.winkel.set": "Guhbioscoop-set",
    "gui.guhs.guhbioscoop.winkel.set.uitleg": "Alles voor je eigen bioscoop: 1 projector, 28 stukken Bioscoopdoek (een heel scherm van 7 bij 4) en 4 Bioscoopstoeltjes. De films verdien je met spelen.",
    "gui.guhs.guhbioscoop.winkel.stoeltje.uitleg": "Een extra stoeltje: voor nog een guh, of voor bezoek.",
    "gui.guhs.guhbioscoop.winkel.popcornmachine.uitleg": "Elke guh in de zaal krijgt een bakje popcorn bij de film. En jij ook, als je erop klikt.",
    "gui.guhs.guhbioscoop.winkel.doek": "Bioscoopdoek (4 stuks)",
    "gui.guhs.guhbioscoop.winkel.doek.uitleg": "Vier extra stukken doek: voor een tweede scherm of een gaatje.",
    # --- the Guhdex section ---
    "gui.guhs.guhbioscoop.gids.kop": "Guhbioscoop",
    "gui.guhs.guhbioscoop.gids.uitleg": "Koop in de Guhpixel-winkel een Guhbioscoop-set, zet de projector voor een scherm van Bioscoopdoek en kies een film. Je guhs komen er zelf bij zitten!",
    "gui.guhs.guhbioscoop.gids.films": "Jouw films",
    "gui.guhs.guhbioscoop.gids.gekeken": "Uitgekeken",
}

# the cinema's own sounds: reused game sounds (Java: BioscoopSlice.FILM_GELUIDEN, the same names in the same order)
FILM_GELUIDEN = {
    "titel": ([{"name": "minecraft:block.note_block.chime", "type": "event", "pitch": 1.0}], "Filmmuziekje"),
    "guh": ([{"name": "guhs:guh_ambient3"}, {"name": "guhs:guh_ambient7"}, {"name": "guhs:guh_ambient11"}], "Guh op het doek"),
    "njeg": ([{"name": "guhs:guh_ambient5", "pitch": 0.8}, {"name": "guhs:guh_ambient9", "pitch": 0.8}], "Njeg op het doek"),
    "vahoeg": ([{"name": "guhs:guh_ambient4", "pitch": 1.3}, {"name": "guhs:guh_ambient10", "pitch": 1.3}, {"name": "guhs:guh_ambient16", "pitch": 1.2}],
               "Vahoeg op het doek"),
    "snurk": ([{"name": "minecraft:entity.fox.sleep", "type": "event", "pitch": 1.2, "volume": 0.8}], "Gesnurk op het doek"),
    "gaap": ([{"name": "guhs:guh_ambient2", "pitch": 0.6}, {"name": "guhs:guh_ambient14", "pitch": 0.6}], "Guh gaapt op het doek"),
    "fanfare": ([{"name": "minecraft:ui.toast.challenge_complete", "type": "event", "pitch": 1.2, "volume": 0.6}], "Fanfare"),
    "boem": ([{"name": "minecraft:entity.firework_rocket.blast", "type": "event"}], "Boem op het doek"),
    "pling": ([{"name": "minecraft:block.note_block.pling", "type": "event", "pitch": 1.4}], "Pling"),
    "plof": ([{"name": "minecraft:block.wool.place", "type": "event", "pitch": 0.8}], "Plof"),
    "spanning": ([{"name": "minecraft:block.note_block.didgeridoo", "type": "event", "pitch": 0.6}], "Spannende muziek"),
    "piep": ([{"name": "minecraft:block.note_block.bit", "type": "event", "pitch": 1.6}], "Alarm piept"),
    "mika": ([{"name": "guhs:mika_ambient1"}, {"name": "guhs:mika_ambient4"}], "Mika op het doek"),
    "wind": ([{"name": "minecraft:item.elytra.flying", "type": "event", "volume": 0.4}], "Wind suist"),
    "klik": ([{"name": "minecraft:block.wooden_button.click_on", "type": "event"}], "Klik"),
    "lach": ([{"name": "guhs:guh_ambient6", "pitch": 1.4}, {"name": "guhs:guh_ambient12", "pitch": 1.4}], "Gelach op het doek"),
    "oeh": ([{"name": "guhs:guh_ambient8", "pitch": 1.7}, {"name": "guhs:guh_ambient1", "pitch": 1.7}], "Guh schrikt op het doek"),
    "smak": ([{"name": "guhs:guh_eat"}], "Gesmak op het doek"),
}
SOUNDS = {
    "guhbioscoop.projector_aan": [{"name": "minecraft:block.lever.click", "type": "event", "pitch": 0.7},
                                  {"name": "minecraft:block.dispenser.dispense", "type": "event", "pitch": 0.8}],
    "guhbioscoop.projector_uit": [{"name": "minecraft:block.lever.click", "type": "event", "pitch": 0.5}],
    "guhbioscoop.pop": [{"name": "minecraft:entity.chicken.egg", "type": "event", "pitch": 1.6, "volume": 0.7},
                        {"name": "minecraft:block.bubble_column.bubble_pop", "type": "event", "pitch": 1.2}],
}
for _n, (_s, _o) in FILM_GELUIDEN.items():
    SOUNDS[f"guhbioscoop.film.{_n}"] = _s
    TEXTS[f"subtitles.guhs.guhbioscoop.film.{_n}"] = _o

ADVANCEMENTS = ("guhbioscoop_eerste_film", "guhbioscoop_alles")


def build(h):
    tex.textures(h)
    modellen(h)
    lib.teksten(h, TEXTS)
    lib.geluid(h, SOUNDS)
    for a in ADVANCEMENTS:
        lib.quest_adv(h, a)
    h.add_tag("guhs/tags/block/guhpixel_bruikbaar", [f"guhs:{b}" for b in ("guhbioscoop_projector", "guhbioscoop_stoeltje", "guhbioscoop_popcornmachine")])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:guhbioscoop_projector", "guhs:guhbioscoop_popcornmachine"])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:guhbioscoop_stoeltje"])
    build.filmteksten = films.build(h)
    lib.teksten(h, build.filmteksten)
    selfcheck(h)


build.filmteksten = {}


# =====================================================================================================================
def _vlak(tex_, uv=None, **extra):
    f = {"texture": tex_}
    if uv:
        f["uv"] = uv
    f.update(extra)
    return f


def _doos(frm, to, tex_, **per_kant):
    """A box with one texture on every side (per_kant: another face for a side)."""
    faces = {k: _vlak(tex_) for k in ("down", "up", "north", "south", "west", "east")}
    for k, v in per_kant.items():
        faces[k] = v
    return {"from": frm, "to": to, "faces": faces}


def modellen(h):
    A = h.A
    T = "guhs:block/guhbioscoop_"
    # --- the projector (the lens looks north) ---
    for aan in (False, True):
        lens = "#lens"
        elementen = [
            _doos([6, 0, 6], [10, 5, 10], "#kast"),                                   # the stand
            _doos([4, 0, 4], [12, 1, 12], "#kast"),
            _doos([3, 5, 2], [13, 12, 14], "#kast"),                                  # the body
            _doos([5.5, 6.5, 0], [10.5, 11.5, 2], "#kast", north=_vlak(lens, [0, 0, 16, 16])),   # the lens
            _doos([7, 12, 2.5], [9, 16, 7.5], "#kast", west=_vlak("#spoel", [0, 0, 16, 16]), east=_vlak("#spoel", [0, 0, 16, 16])),
            _doos([7, 12, 8.5], [9, 16, 13.5], "#kast", west=_vlak("#spoel", [0, 0, 16, 16]), east=_vlak("#spoel", [0, 0, 16, 16])),
        ]
        if aan:
            elementen[3]["light_emission"] = 15
        naam = "guhbioscoop_projector" + ("_aan" if aan else "")
        h.w(f"{A}/models/block/{naam}.json", {
            "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"particle": T + "projector", "kast": T + "projector", "lens": T + ("projector_lens_aan" if aan else "projector_lens"),
                         "spoel": T + "projector_spoel"},
            "elements": elementen})
    variants = {}
    for f, y in lib.ROT:
        for lit in ("false", "true"):
            v = {"model": "guhs:block/guhbioscoop_projector" + ("_aan" if lit == "true" else "")}
            if y:
                v["y"] = y
            variants[f"facing={f},lit={lit}"] = v
    h.w(f"{A}/blockstates/guhbioscoop_projector.json", {"variants": variants})
    # --- the cloth: a thin sheet at the back of its block (the picture side is north) ---
    h.w(f"{A}/models/block/guhbioscoop_doek.json", {
        "parent": "minecraft:block/block", "textures": {"particle": T + "doek", "doek": T + "doek", "achter": T + "doek_achter"},
        "elements": [_doos([0, 0, 14], [16, 16, 16], "#achter", north=_vlak("#doek", [0, 0, 16, 16]))],
        "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
                    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 7], "scale": [1, 1, 1]}}})
    h.w(f"{A}/blockstates/guhbioscoop_doek.json", {"variants": h.facing_states("guhbioscoop_doek")})
    # --- the seat (the sitter looks north; the back is south) ---
    h.w(f"{A}/models/block/guhbioscoop_stoeltje.json", {
        "parent": "minecraft:block/block", "textures": {"particle": T + "stoeltje", "stof": T + "stoeltje", "frame": T + "stoeltje_frame"},
        "elements": [
            _doos([4, 0, 4], [12, 4, 12], "#frame"),            # the foot
            _doos([2, 4, 2], [14, 7, 13], "#stof"),             # the cushion
            _doos([2, 7, 13], [14, 16, 15], "#stof"),           # the back
            _doos([1, 5, 3], [3, 10, 14], "#frame"),            # the arm rests
            _doos([13, 5, 3], [15, 10, 14], "#frame"),
        ]})
    h.w(f"{A}/blockstates/guhbioscoop_stoeltje.json", {"variants": h.facing_states("guhbioscoop_stoeltje")})
    # --- the popcornmachine (the sign is on the north side) ---
    h.w(f"{A}/models/block/guhbioscoop_popcornmachine.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": T + "popcornmachine_kar", "kar": T + "popcornmachine_kar", "voor": T + "popcornmachine_voor",
                     "glas": T + "popcornmachine_glas", "dak": T + "popcornmachine_dak"},
        "elements": [
            _doos([2, 0, 2], [14, 6, 14], "#kar", north=_vlak("#voor", [0, 4, 16, 12])),
            _doos([2, 6, 2], [14, 14, 14], "#glas", **{k: _vlak("#glas", [0, 0, 16, 16]) for k in ("north", "south", "west", "east")}),
            _doos([1, 14, 1], [15, 16, 15], "#dak"),
        ]})
    h.w(f"{A}/blockstates/guhbioscoop_popcornmachine.json", {"variants": h.facing_states("guhbioscoop_popcornmachine")})
    for b in BLOKKEN:
        h.w(f"{A}/models/item/{b}.json", {"parent": f"guhs:block/{b}"})
        h.self_drop(b)
    h.item_model("guhbioscoop_popcorn")


# =====================================================================================================================
def selfcheck(h):
    lib.controleer(h, "guhpixel_bioscoop", blokken=BLOKKEN, items=BLOKKEN + ("guhbioscoop_popcorn",), keys=list(TEXTS) + list(build.filmteksten))
    problems = []
    for b in BLOKKEN:
        if not os.path.exists(f"{h.D}/loot_table/blocks/{b}.json"):
            problems.append(f"no loot table for {b}")
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhpixel")
    slice_src = open(os.path.join(java, "bioscoop", "BioscoopSlice.java"), encoding="utf-8").read()
    m = re.search(r"FILM_GELUIDEN = List\.of\(([^;]+)\);", slice_src)
    in_java = re.findall(r'"([a-z_]+)"', m.group(1)) if m else []
    if in_java != list(FILM_GELUIDEN) or tuple(in_java) != film.GELUIDEN:
        problems.append(f"the cinema sounds differ: Java {in_java}, guhpixel_bioscoop.py {list(FILM_GELUIDEN)}, film.py {list(film.GELUIDEN)}")
    info = open(os.path.join(java, "bioscoop", "FilmInfo.java"), encoding="utf-8").read()
    m = re.search(r"CUE_SOORTEN = List\.of\(([^;]+)\);", info)
    if not m or tuple(re.findall(r'"([a-z]+)"', m.group(1))) != film.CUES:
        problems.append("the cue kinds of FilmInfo.java and guhpixel_bioscoop_film.py differ")
    ids_src = open(os.path.join(java, "Films.java"), encoding="utf-8").read()
    m = re.search(r"IDS = new ArrayList<>\(List\.of\(([^;]+)\)\);", ids_src, re.S)
    ids = re.findall(r'"([a-z0-9_]+)"', m.group(1)) if m else []
    index = json.load(open(f"{h.D}/guhbioscoop/films.json", encoding="utf-8"))["films"]
    if ids != list(index):
        problems.append(f"Films.IDS {ids} is not the list of films {list(index)}")
    for fid in index:
        for pad in (f"{h.A}/guhbioscoop/films/{fid}.json", os.path.join(h.TEX, "guhbioscoop", f"{fid}.png")):
            if not os.path.exists(pad):
                problems.append(f"missing {pad}")
    for a in ADVANCEMENTS:
        if not os.path.exists(f"{h.D}/advancement/quest/{a}.json"):
            problems.append(f"missing advancement {a}")
    if problems:
        raise SystemExit("guhpixel_bioscoop self-check failed:\n  " + "\n  ".join(problems))


def ftb(fq):
    fq.q("guhbioscoop_projector", "Je eigen Guhbioscoop",
         "Koop bij de &dVerkoper-guh&r in de Guhpixel-lobby een &dGuhbioscoop-set&r: een projector, een heel scherm van Bioscoopdoek "
         "(7 breed, 4 hoog) en vier Bioscoopstoeltjes. Bouw thuis het scherm en zet de projector ervoor, met de lens naar het doek. Ratelratel!",
         "guhs:guhbioscoop_projector", [fq.item("guhs:guhbioscoop_projector")], xp=50)
    fq.q("guhbioscoop_eerste_film", "Filmavond",
         "Rechtsklik op de projector en kies een film: voor elk uitgespeeld Guhpixel-grapspel krijg je er een, en ook voor de verhalen van "
         "Baltoguh, Guhtwo en 626-guh. Kijk hem helemaal uit. Je guhs zoeken zelf een stoeltje en met een &dPopcornmachine&r krijgen ze popcorn. "
         "Stil zijn in de zaal, njeg!",
         "guhs:guhbioscoop_popcorn", [fq.adv("guhbioscoop_eerste_film")], deps=["guhbioscoop_projector"], xp=50)
    fq.q("guhbioscoop_alles", "Filmkenner",
         "Kijk alle &dnegen films&r helemaal uit: Skyblok, Bedwars, Vadsnite, Guhmon, Boer zoekt Guh, Among Guhs, Baltoguh, Guhtwo en 626. "
         "Ze lopen allemaal hetzelfde af. Raad maar hoe, njeg.",
         "guhs:guhbioscoop_stoeltje", [fq.adv("guhbioscoop_alles")], rewards=(("guhs:kaas_knabbels", 16),), deps=["guhbioscoop_eerste_film"], xp=100)
    fq.q("guhbioscoop_popcorn", "Pof!",
         "Koop bij de &dVerkoper-guh&r een &dPopcornmachine&r en zet hem in je bioscoopzaal. Elke guh die komt kijken krijgt een bakje popcorn "
         "bij de film. Rechtsklik erop en jij krijgt er ook een. Niet kruimelen, njeg. (Wel kruimelen.)",
         "guhs:guhbioscoop_popcornmachine", [fq.item("guhs:guhbioscoop_popcornmachine")], deps=["guhbioscoop_projector"])
