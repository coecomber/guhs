"""
Guhpixel slice "kantoor" (Java: feature/guhpixel/kantoor; namespaces guhkantoor; English: tools/lang/en/c36_px_kantoor.json).

The Guhkantoor for home (DESIGN_PX section 4): from the Verkoper-guh's shop, no recipes.
  - block + item guhkantoor_prikklok: the Prikklok (the screen with up to 4 desks)
  - block + item guhkantoor_bureautje (facing, bezet): desk, old computer, stool; a guh sleeps on the keyboard (drawn by the
    block entity renderer from the stored looks); bezet switches the monitor from the screensaver to "jjjjjjjj"
  - three papers, each a readable item AND a thin wall block that keeps the paper's data (custom data, copied back by the
    loot table): guhkantoor_loonstrookje, guhkantoor_kwartaalrapport, guhkantoor_oorkonde
  - sounds guhkantoor.inklokken / .uitklokken / .printer / .toetsenbord, hidden advancements quest/guhkantoor_loonstrookje and
    quest/guhkantoor_kwartaal, the test room guhkantoor_test_kantoor
  - every text: tools/features/guhpixel_kantoor_tekst.py (the pools of the papers too)
Helpers: guhpixel_kantoor_tex.py (textures), guhpixel_kantoor_tekst.py (texts). Every text is Dutch here.
"""
import os
import re

from features import guhpixel_lib as lib
from features import guhpixel_kantoor_tekst as tekst
from features import guhpixel_kantoor_tex as tex

PAPIEREN = ("guhkantoor_loonstrookje", "guhkantoor_kwartaalrapport", "guhkantoor_oorkonde")
BLOKKEN = ("guhkantoor_prikklok", "guhkantoor_bureautje") + PAPIEREN
TEXTS = tekst.alle()


def build(h):
    tex.alles(h)
    blokken(h)
    papieren(h)
    lib.teksten(h, TEXTS)
    lib.geluid(h, {
        "guhkantoor.inklokken": [{"name": "minecraft:block.note_block.bell", "type": "event", "pitch": 1.6, "volume": 0.8}],
        "guhkantoor.uitklokken": [{"name": "minecraft:block.note_block.bell", "type": "event", "pitch": 1.1, "volume": 0.8}],
        "guhkantoor.printer": [{"name": "minecraft:ui.cartography_table.take_result", "type": "event", "pitch": 0.9}],
        "guhkantoor.toetsenbord": [{"name": "minecraft:block.wooden_button.click_on", "type": "event", "pitch": 1.8, "volume": 0.5},
                                   {"name": "minecraft:block.wooden_button.click_off", "type": "event", "pitch": 1.6, "volume": 0.5}],
    })
    lib.quest_adv(h, "guhkantoor_loonstrookje")
    lib.quest_adv(h, "guhkantoor_kwartaal")
    lib.test_kamer(h, "guhkantoor_test_kantoor", (16, 8, 16))
    selfcheck(h)


def _f(tex_naam, uv=None):
    f = {"texture": tex_naam}
    if uv:
        f["uv"] = uv
    return f


def blokken(h):
    A = h.A
    # --- the Prikklok: a foot, a post and the clock itself; the dial is on the north side (towards whoever placed it) ---
    h.w(f"{A}/models/block/guhkantoor_prikklok.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": "guhs:block/guhkantoor_metaal", "m": "guhs:block/guhkantoor_metaal", "v": "guhs:block/guhkantoor_prikklok_voor"},
        "elements": [
            h.el([4, 0, 4], [12, 1, 12], "#m"),
            h.el([7, 1, 7], [9, 8, 9], "#m", faces=("north", "south", "west", "east")),
            {"from": [3, 8, 5], "to": [13, 16, 11], "faces": {
                "north": _f("#v", [0, 0, 16, 16]), "south": _f("#m"), "west": _f("#m"), "east": _f("#m"), "up": _f("#m"), "down": _f("#m")}},
        ]})
    h.w(f"{A}/blockstates/guhkantoor_prikklok.json", {"variants": h.facing_states("guhkantoor_prikklok")})
    h.w(f"{A}/models/item/guhkantoor_prikklok.json", {"parent": "guhs:block/guhkantoor_prikklok"})
    h.self_drop("guhkantoor_prikklok")
    # --- the Bureautje: the stool on the north side (where the guh "works"), the monitor at the back looking north ---
    for bezet in (False, True):
        naam = "guhkantoor_bureautje" + ("_bezet" if bezet else "")
        h.w(f"{A}/models/block/{naam}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": "guhs:block/guhkantoor_hout", "h": "guhs:block/guhkantoor_hout", "c": "guhs:block/guhkantoor_computer",
                         "s": "guhs:block/guhkantoor_scherm" + ("_bezet" if bezet else ""), "t": "guhs:block/guhkantoor_toetsenbord",
                         "k": "guhs:block/guhkantoor_kruk"},
            "elements": [
                h.el([0, 7, 5], [16, 8, 16], "#h"),                                                    # the desk top
                h.el([0, 0, 5], [2, 7, 7], "#h", faces=("north", "south", "west", "east", "down")),     # four legs
                h.el([14, 0, 5], [16, 7, 7], "#h", faces=("north", "south", "west", "east", "down")),
                h.el([0, 0, 14], [2, 7, 16], "#h", faces=("north", "south", "west", "east", "down")),
                h.el([14, 0, 14], [16, 7, 16], "#h", faces=("north", "south", "west", "east", "down")),
                h.el([10, 3, 8], [14, 7, 14], "#h", faces=("north", "south", "west", "east", "down")),  # a drawer
                {"from": [4, 8, 10], "to": [12, 15, 16], "faces": {                                    # the old beige monitor
                    "north": _f("#s", [0, 0, 16, 16]), "south": _f("#c"), "west": _f("#c"), "east": _f("#c"), "up": _f("#c")}},
                {"from": [3, 8, 6], "to": [11, 9, 9], "faces": {                                       # the keyboard
                    "up": _f("#t", [0, 0, 16, 16]), "north": _f("#c"), "south": _f("#c"), "west": _f("#c"), "east": _f("#c")}},
                h.el([12, 8, 7], [14, 9, 9], "#c", faces=("north", "south", "west", "east", "up")),     # the mouse (no relation)
                h.el([5, 4, 0], [11, 5, 4], "#k"),                                                     # the stool: a cushion
                h.el([7, 0, 1], [9, 4, 3], "#h", faces=("north", "south", "west", "east", "down")),     # and its leg
            ]})
    states = {}
    states.update(h.facing_states("guhkantoor_bureautje", extra=",bezet=false"))
    states.update(h.facing_states("guhkantoor_bureautje", model="guhs:block/guhkantoor_bureautje_bezet", extra=",bezet=true"))
    h.w(f"{A}/blockstates/guhkantoor_bureautje.json", {"variants": states})
    h.w(f"{A}/models/item/guhkantoor_bureautje.json", {"parent": "guhs:block/guhkantoor_bureautje"})
    h.self_drop("guhkantoor_bureautje")
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:guhkantoor_prikklok"])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:guhkantoor_bureautje"])


# name -> the sheet on the wall (x0, y0, x1, y1), hanging against the south side of its block (facing north)
VELLEN = {"guhkantoor_loonstrookje": (5, 2, 11, 14), "guhkantoor_kwartaalrapport": (3, 1, 13, 15), "guhkantoor_oorkonde": (1, 3, 15, 13)}


def papieren(h):
    A, D = h.A, h.D
    for naam, (x0, y0, x1, y1) in VELLEN.items():
        h.w(f"{A}/models/block/{naam}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": f"guhs:block/{naam}", "p": f"guhs:block/{naam}", "r": "guhs:block/guhkantoor_papierrand"},
            "elements": [{"from": [x0, y0, 15.5], "to": [x1, y1, 16], "faces": {
                "north": _f("#p", [0, 0, 16, 16]), "south": _f("#r"), "west": _f("#r"), "east": _f("#r"), "up": _f("#r"), "down": _f("#r")}}]})
        h.w(f"{A}/blockstates/{naam}.json", {"variants": h.facing_states(naam)})
        h.item_model(naam)
        # the paper keeps what is written on it: the block entity hands its custom data back to the item
        h.w(f"{D}/loot_table/blocks/{naam}.json", {"type": "minecraft:block", "pools": [{
            "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"guhs:{naam}", "functions": [
                {"function": "minecraft:copy_components", "source": "block_entity", "include": ["minecraft:custom_data"]}]}]}]})


def selfcheck(h):
    lib.controleer(h, "guhpixel_kantoor", blokken=BLOKKEN, items=BLOKKEN, keys=TEXTS, templates=("guhkantoor_test_kantoor",))
    problems = []
    for b in BLOKKEN:
        if not os.path.exists(f"{h.D}/loot_table/blocks/{b}.json"):
            problems.append(f"no loot table for {b}")
    for a in ("guhkantoor_loonstrookje", "guhkantoor_kwartaal"):
        if not os.path.exists(f"{h.D}/advancement/quest/{a}.json"):
            problems.append(f"missing advancement quest/{a}")
    # the pools of the papers: the same names and sizes as KantoorTeksten.Pool
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhpixel", "kantoor", "KantoorTeksten.java"),
               encoding="utf-8").read()
    java = {m.group(1): int(m.group(2)) for m in re.finditer(r'[A-Z_]+\("([a-z_.]+)", (\d+)\)', src)}
    python = {pool: len(regels) for pool, regels in tekst.POOLS.items()}
    if java != python:
        problems.append(f"KantoorTeksten.Pool {java} differs from guhpixel_kantoor_tekst.POOLS {python}")
    for pool, regels in tekst.POOLS.items():
        if len(set(regels)) != len(regels):
            problems.append(f"pool {pool} has the same line twice")
    if problems:
        raise SystemExit("guhpixel_kantoor self-check failed:\n  " + "\n  ".join(problems))


def ftb(fq):
    fq.q("guhkantoor_prikklok", "Het Guhkantoor",
         "Koop bij de &dVerkoper-guh&r in de Guhpixel-lobby de &6Guhkantoor-set&r: een &dPrikklok&r en een &dBureautje&r. Zet het Bureautje "
         "binnen 8 blokken van de Prikklok (er passen er 4 op één klok) en klik op de klok om een guh aan het werk te zetten. "
         "Werken betekent hier: slapen op het toetsenbord. Njeg!",
         "guhs:guhkantoor_prikklok", [fq.item("guhs:guhkantoor_prikklok")], x=0, y=4, shape="rsquare", xp=50)
    fq.q("guhkantoor_loonstrookje", "Loon naar slapen",
         "Na &68 echte uren&r slapen ligt er bij de Prikklok een &dloonstrookje&r voor je guh. Bruto: 0 knabbels. Ingehouden: een paar "
         "dutjes. Lees het (rechtsklik in de lucht) of hang het aan de muur. De tijd loopt gewoon door als je er niet bent.",
         "guhs:guhkantoor_loonstrookje", [fq.adv("guhkantoor_loonstrookje")], deps=["guhkantoor_prikklok"], x=2, y=4, xp=100)
    fq.q("guhkantoor_kwartaal", "De lijn is vlak",
         "Bij elk derde loonstrookje krijg je een &dkwartaalrapport&r: grafieken (allemaal vlak), hoogtepunten (\"Vergadering uitgesteld "
         "wegens vadsen\") en de &6Werknemer van de maand&r: de guh die het langst sliep. Het levert helemaal niks op. Vahoeg!",
         "guhs:guhkantoor_kwartaalrapport", [fq.adv("guhkantoor_kwartaal")], deps=["guhkantoor_loonstrookje"], x=4, y=4, shape="gear", xp=150)
    fq.q("guhkantoor_oorkonde", "Werknemer van de maand",
         "Aan het eind van elke echte maand krijgt de guh die de meeste uren aan een bureau sliep de &6Oorkonde Werknemer van de maand&r. "
         "Hij komt vanzelf met de post van het Guhkantoor. Hang hem op: zo hard heeft nog nooit iemand niks gedaan.",
         "guhs:guhkantoor_oorkonde", [fq.item("guhs:guhkantoor_oorkonde")], rewards=(("guhs:kaas_knabbels", 12),),
         deps=["guhkantoor_prikklok"], x=6, y=4, xp=100)
