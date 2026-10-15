"""
einde2 (the Guheinde expansion), slice bouw_lucht: the build of the Zuidelijke Luchttempel (DESIGN 5, chapter 1: lucht).
Java: nl.juiced.guhs.feature.einde2.bouwlucht (E2BouwLuchtFeature).

  - the structure guhs:zuidelijke_luchttempel: a guhs:leegte_bouw that brings its own floating island (the build itself:
    einde2_bouw_lucht_bouw.py), one guaranteed copy per world on the outer ring, protected, in no compass tab. BUILD only:
    no story logic and no characters, every spot a later phase needs is a marker in
    data/guhs/einde2/plekken/zuidelijke_luchttempel.json;
  - the five stages of the Vredesballon as overlay templates on the building site (einde2_bouw_lucht_ballon.py);
  - the block werkplaatsdeur (the Werktuigguh's closed door: solid for everybody until Einde2Haken.WERKPLAATS_OPEN says
    otherwise) and five decorative blocks of the temple (einde2_bouw_lucht_tegel, _pijltegel, _taart, _tandwiel, _blauwdruk;
    textures: einde2_bouw_lucht_tex.py);
  - the chest loot of the monks' pantry, the texts, two little test templates for the game tests.
"""
import json
import os

import features.einde2_kit as kit
from features import einde2_bouw_lucht_ballon as ballon
from features import einde2_bouw_lucht_bouw as bouw
from features import einde2_bouw_lucht_tex as tex

NAAM = bouw.NAAM
S = "einde2_bouw_lucht"
DEUR = "werkplaatsdeur"
TEGEL, PIJL, TAART, TANDWIEL, BLAUWDRUK = f"{S}_tegel", f"{S}_pijltegel", f"{S}_taart", f"{S}_tandwiel", f"{S}_blauwdruk"
BLOKKEN = [DEUR, TEGEL, PIJL, TAART, TANDWIEL, BLAUWDRUK]
VOORRAAD = f"chests/{S}_voorraad"

TEXTS = {
    f"block.guhs.{DEUR}": "Werkplaatsdeur",
    f"block.guhs.{DEUR}.lore": "Dicht tot de Werktuigguh zegt dat het af is. Dus: dicht.",
    f"block.guhs.{TEGEL}": "Luchtnomadentegel",
    f"block.guhs.{TEGEL}.lore": "Een tempeltegel met een windkrul. Tocht niet.",
    f"block.guhs.{PIJL}": "Pijltegel",
    f"block.guhs.{PIJL}.lore": "Wijst de weg. Welke weg? Die kant op, njeg.",
    f"block.guhs.{TAART}": "Taart van Guhatso",
    f"block.guhs.{TAART}.lore": "Niet om op te eten. Om te gooien, njeg.",
    f"block.guhs.{TANDWIEL}": "Tandwielblok",
    f"block.guhs.{TANDWIEL}.lore": "Draait nergens om. Staat wel vads.",
    f"block.guhs.{BLAUWDRUK}": "Blauwdruk van de Werktuigguh",
    f"block.guhs.{BLAUWDRUK}.lore": "Een plan voor een ballon met oortjes. Bijna af.",
}


def textures(h):
    tex.build(h)


def blocks_and_items(h):
    A = h.A
    h.simple_block(DEUR)
    h.simple_block(TEGEL)
    h.simple_block(TANDWIEL)
    # the arrow tile: the arrow lies on top and points to the block's facing
    h.w(f"{A}/models/block/{PIJL}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": f"guhs:block/{PIJL}", "bottom": f"guhs:block/{TEGEL}", "side": f"guhs:block/{TEGEL}"}})
    h.w(f"{A}/blockstates/{PIJL}.json", {"variants": h.facing_states(PIJL)})
    h.w(f"{A}/models/item/{PIJL}.json", {"parent": f"guhs:block/{PIJL}"})
    # the blueprint: its drawing on the front, a plank frame around
    h.w(f"{A}/models/block/{BLAUWDRUK}.json", {"parent": "minecraft:block/orientable", "textures": {
        "top": f"guhs:block/{BLAUWDRUK}_zij", "front": f"guhs:block/{BLAUWDRUK}", "side": f"guhs:block/{BLAUWDRUK}_zij"}})
    h.w(f"{A}/blockstates/{BLAUWDRUK}.json", {"variants": h.facing_states(BLAUWDRUK)})
    h.w(f"{A}/models/item/{BLAUWDRUK}.json", {"parent": f"guhs:block/{BLAUWDRUK}"})
    # the taart: a low round-ish pie (a box of 12 x 5 x 12 with a rim of crust that stands a little higher)
    top, zij, onder = f"guhs:block/{TAART}_top", f"guhs:block/{TAART}_zij", f"guhs:block/{TAART}_onder"

    def doos(x0, y0, z0, x1, y1, z1):
        return {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": {
            "up": {"uv": [x0, z0, x1, z1], "texture": "#top"}, "down": {"uv": [x0, 16 - z1, x1, 16 - z0], "texture": "#onder"},
            "north": {"uv": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "texture": "#zij"}, "south": {"uv": [x0, 16 - y1, x1, 16 - y0], "texture": "#zij"},
            "west": {"uv": [z0, 16 - y1, z1, 16 - y0], "texture": "#zij"}, "east": {"uv": [16 - z1, 16 - y1, 16 - z0, 16 - y0], "texture": "#zij"}}}

    h.w(f"{A}/models/block/{TAART}.json", {"parent": "minecraft:block/block", "textures": {"particle": zij, "top": top, "zij": zij, "onder": onder},
                                            "elements": [doos(2, 0, 2, 14, 4, 14), doos(3, 4, 3, 13, 5, 13),
                                                         doos(1, 3, 3, 2, 5, 13), doos(14, 3, 3, 15, 5, 13), doos(3, 3, 1, 13, 5, 2), doos(3, 3, 14, 13, 5, 15)]})
    h.w(f"{A}/blockstates/{TAART}.json", {"variants": {"": {"model": f"guhs:block/{TAART}"}}})
    h.w(f"{A}/models/item/{TAART}.json", {"parent": f"guhs:block/{TAART}"})
    for blok in (TEGEL, PIJL, TAART, TANDWIEL, BLAUWDRUK):      # (the door is unbreakable and drops nothing)
        h.self_drop(blok)
    g = lambda names: [f"guhs:{n}" for n in names]  # noqa: E731
    h.add_tag("minecraft/tags/block/mineable/pickaxe", g([TEGEL, PIJL, TANDWIEL]))
    h.add_tag("minecraft/tags/block/mineable/axe", g([BLAUWDRUK, TAART]))


def recipes(h):
    h.shaped(TEGEL, ["CC", "CB"], {"C": "minecraft:calcite", "B": "minecraft:light_blue_dye"}, f"guhs:{TEGEL}", 3)
    h.shapeless(PIJL, ["guhs:" + TEGEL, "minecraft:light_blue_dye"], f"guhs:{PIJL}")
    h.shaped(TANDWIEL, [" K ", "KPK", " K "], {"K": "minecraft:copper_ingot", "P": "#minecraft:planks"}, f"guhs:{TANDWIEL}", 2)
    h.shaped(BLAUWDRUK, ["PPP", "PBP", "PPP"], {"P": "minecraft:paper", "B": "minecraft:blue_dye"}, f"guhs:{BLAUWDRUK}")
    h.shaped(TAART, ["BSB", "WWW"], {"B": "minecraft:sweet_berries", "S": "minecraft:sugar", "W": "minecraft:wheat"}, f"guhs:{TAART}")


def loot(h):
    """The monks' pantry: something to eat, a few knabbels. Modest, and never anything a quest needs."""
    def item(name, weight, lo, hi):
        return {"type": "minecraft:item", "name": name, "weight": weight,
                "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
    h.w(f"{h.D}/loot_table/{VOORRAAD}.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
            item("minecraft:apple", 6, 2, 5), item("minecraft:bread", 5, 1, 3), item("minecraft:sweet_berries", 4, 2, 6),
            item("minecraft:pumpkin_pie", 3, 1, 2), item("guhs:kaas_knabbels", 5, 2, 6), item("minecraft:melon_slice", 3, 2, 5)]},
        {"rolls": 1, "entries": [item(f"guhs:{TAART}", 1, 1, 1), item("minecraft:honey_bottle", 1, 1, 1), {"type": "minecraft:empty", "weight": 2}]}]})


def texts(h):
    for key, text in TEXTS.items():
        kit.lang(h, key, text)


def structuur(h):
    """The build, its self-check, its tiles, the overlays and everything of the worldgen."""
    b = bouw.maak(h)
    problems = bouw.controleer(b)
    if problems:
        raise SystemExit(f"{NAAM}: self-check found {len(problems)} problem(s):\n  " + "\n  ".join(problems[:40]))
    bouw.lagen(b)
    bouw.zuka_schip_laag(b)
    nx, nz = b.save_tiles()
    kit.leegte_structuur(h, b, titel="Zuidelijke Luchttempel", tooltip="Een witte tempel met blauwgroene daken op een zwevende rots: hier woont de wind",
                         y=bouw.Y_WERELD, anker=bouw.ANKER)
    print(f"{NAAM}: self-check ok {b.stats}, {nx} x {nz} tiles, {len(b.palen)} palen, {len(bouw.RINGEN)} ringen, "
          f"licht erbij {sum(b.auto_licht.values())} {b.auto_licht}")
    return b


def test_templates(h):
    """einde2_bouw_lucht_test_deur: a floor with a wall of door blocks across it (the game test of the door);
    einde2_bouw_lucht_test_veld: an empty floor big enough to put the five stages of the Vredesballon on."""
    t = h.Structure((5, 5, 5))
    t.fill(0, 0, 0, 4, 0, 4, "guhs:kruimelrots_stenen")
    t.fill(0, 1, 2, 4, 3, 2, f"guhs:{DEUR}")
    t.save(f"{S}_test_deur")
    v = h.Structure((ballon.SX + 2, ballon.SY + 2, ballon.SZ + 2))
    v.fill(0, 0, 0, ballon.SX + 1, 0, ballon.SZ + 1, "guhs:kruimelrots_stenen")
    v.save(f"{S}_test_veld")


def selfcheck(h):
    A, D = h.A, h.D
    missing = [k for k in TEXTS if k not in h.NL]
    for blok in BLOKKEN:
        for path in (f"{A}/blockstates/{blok}.json", f"{A}/models/block/{blok}.json", f"{A}/models/item/{blok}.json"):
            if not os.path.exists(path):
                missing.append(path)
    for t in (f"{DEUR}", TEGEL, PIJL, f"{TAART}_top", f"{TAART}_zij", f"{TAART}_onder", TANDWIEL, BLAUWDRUK, f"{BLAUWDRUK}_zij"):
        if not os.path.exists(f"{A}/textures/block/{t}.png"):
            missing.append(f"texture {t}")
    for path in (f"{D}/worldgen/structure/{NAAM}.json", f"{D}/worldgen/structure_set/{NAAM}_gegarandeerd.json", f"{D}/einde2/plekken/{NAAM}.json",
                 f"{D}/tags/worldgen/biome/has_structure/{NAAM}.json", f"{D}/loot_table/{VOORRAAD}.json", f"{D}/structure/{S}_test_deur.nbt",
                 f"{D}/structure/{S}_test_veld.nbt", f"{D}/structure/{NAAM}/laag_zuka_schip.nbt") + tuple(f"{D}/structure/{NAAM}/laag_vredesballon_{n}.nbt" for n in range(1, 6)):
        if not os.path.exists(path):
            missing.append(path)
    if os.path.exists(f"{D}/worldgen/structure_set/{NAAM}.json"):
        missing.append("a random spread set: the temple exists exactly once per world")
    if not missing:
        blad = json.load(open(f"{D}/einde2/plekken/{NAAM}.json", encoding="utf-8"))
        st = json.load(open(f"{D}/worldgen/structure/{NAAM}.json", encoding="utf-8"))
        if sorted(blad.get("lagen", {})) != sorted([f"vredesballon_{n}" for n in range(1, 6)] + ["zuka_schip"]):
            missing.append("the five overlays of the Vredesballon and Zuka's ship in the sidecar")
        if st["rand"] != 48 or st["size"] != [bouw.W, bouw.H, bouw.D] or st["y"] != bouw.Y_WERELD:
            missing.append(f"structure json: {st}")
        if not os.path.exists(f"{D}/structure/{NAAM}/stuk_{bouw.ANKER[0] // kit.TILE}_{bouw.ANKER[2] // kit.TILE}.nbt"):
            missing.append("the tile of the anchor")
    if missing:
        raise SystemExit(f"{S}: missing {missing}")


def build(h):
    textures(h)
    blocks_and_items(h)
    recipes(h)
    loot(h)
    texts(h)
    structuur(h)
    test_templates(h)
    selfcheck(h)
