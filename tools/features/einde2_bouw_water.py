"""
einde2 (the Guheinde expansion), slice bouw_water: the build of the Noordelijke Waterstam, the second story structure of
the Guhvatar (water). Java: nl.juiced.guhs.feature.einde2.bouwwater (E2BouwWaterFeature).

BUILD ONLY: no story, no characters. A later phase finds its spots through the markers of the sidecar
data/guhs/einde2/plekken/noordelijke_waterstam.json (Plekken.java).

  - the structure guhs:noordelijke_waterstam (guhs:leegte_bouw: tiles noordelijke_waterstam/stuk_<i>_<j>, one guaranteed
    copy per world, high above the outer islands; protected and not in any compass: the kit and the skeleton see to that).
    The build itself is einde2_bouw_water_bouw.py (the ground: _grond.py, the city: _stad.py, the ships: _schip.py);
  - the overlay noordelijke_waterstam/laag_vloot: the fleet of the Vuurnatie in front of the wall;
  - six decorative blocks (no behaviour): Waterstam-ijsstenen with stairs and slab (ice you do not slip on), the ijslantaarn
    (a light), the Waterstamteken and the Vuurnatieteken. Textures: einde2_bouw_water_tex.py;
  - three modest loot tables for the chests in the houses, the palace and Zhao's cabin;
  - the room of the game tests.

Its water wants to be blue: the Waterstam only starts above the two streken with blue-green water (WATER_STREKEN), not above
all six (build contract 10, Deviations).
"""
import os

import features.einde2_kit as kit
from features import einde2_bouw_water_bouw as bouw
from features import einde2_bouw_water_tex as tex
from features.barbecuether import stair_states

NAAM = bouw.NAAM
IJSSTEEN = "einde2_bouw_water_ijssteen"
SIMPEL = [IJSSTEEN, "einde2_bouw_water_ijslantaarn", "einde2_bouw_water_stamteken", "einde2_bouw_water_vuurnatie_teken"]
BLOKKEN = [IJSSTEEN, IJSSTEEN + "_trap", IJSSTEEN + "_plaat"] + SIMPEL[1:]
# the streken whose water is blue-green (the others have yellow, pink or purple water: no sea for a Waterstam)
WATER_STREKEN = ["korstwoestijn", "schimmelkaasvlakte"]

TEXTS = {
    "block.guhs.einde2_bouw_water_ijssteen": "Waterstam-ijsstenen",
    "block.guhs.einde2_bouw_water_ijssteen_trap": "Trap van Waterstam-ijsstenen",
    "block.guhs.einde2_bouw_water_ijssteen_plaat": "Plaat van Waterstam-ijsstenen",
    "block.guhs.einde2_bouw_water_ijslantaarn": "IJslantaarn",
    "block.guhs.einde2_bouw_water_stamteken": "Waterstamteken",
    "block.guhs.einde2_bouw_water_vuurnatie_teken": "Vuurnatieteken",
    "block.guhs.einde2_bouw_water_ijssteen.lore": "Uit de gletsjer gezaagd. Glijdt niet: daar heeft een guh over nagedacht",
    "block.guhs.einde2_bouw_water_ijslantaarn.lore": "Een lichtje, ingevroren in helder ijs. Smelt nooit, njeg",
    "block.guhs.einde2_bouw_water_stamteken.lore": "Een maan boven drie golven. De oortjes zijn er later bij gekomen",
    "block.guhs.einde2_bouw_water_vuurnatie_teken.lore": "Een vlam die boos kijkt. Typisch Mika",
}


def blocks_and_items(h):
    A, w = h.A, h.w
    for b in SIMPEL:
        h.simple_block(b)
    t = f"guhs:block/{IJSSTEEN}"
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        w(f"{A}/models/block/{IJSSTEEN}_trap{suffix}.json", {"parent": f"minecraft:block/{parent}", "textures": {"bottom": t, "top": t, "side": t}})
    w(f"{A}/blockstates/{IJSSTEEN}_trap.json", {"variants": stair_states(f"guhs:block/{IJSSTEEN}_trap")})
    w(f"{A}/models/item/{IJSSTEEN}_trap.json", {"parent": f"guhs:block/{IJSSTEEN}_trap"})
    w(f"{A}/models/block/{IJSSTEEN}_plaat.json", {"parent": "minecraft:block/slab", "textures": {"bottom": t, "top": t, "side": t}})
    w(f"{A}/models/block/{IJSSTEEN}_plaat_top.json", {"parent": "minecraft:block/slab_top", "textures": {"bottom": t, "top": t, "side": t}})
    w(f"{A}/blockstates/{IJSSTEEN}_plaat.json", {"variants": {"type=bottom": {"model": f"guhs:block/{IJSSTEEN}_plaat"},
                                                              "type=top": {"model": f"guhs:block/{IJSSTEEN}_plaat_top"},
                                                              "type=double": {"model": f"guhs:block/{IJSSTEEN}"}}})
    w(f"{A}/models/item/{IJSSTEEN}_plaat.json", {"parent": f"guhs:block/{IJSSTEEN}_plaat"})


def loot(h):
    D, w = h.D, h.w
    for b in BLOKKEN:
        if b != IJSSTEEN + "_plaat":
            h.self_drop(b)
    w(f"{D}/loot_table/blocks/{IJSSTEEN}_plaat.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"guhs:{IJSSTEEN}_plaat", "functions": [
            {"function": "minecraft:set_count", "count": 2, "conditions": [{"condition": "minecraft:block_state_property",
                                                                             "block": f"guhs:{IJSSTEEN}_plaat", "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}]})

    def kist(naam, rollen, dingen):
        """A modest chest: (item, weight, min, max)."""
        w(f"{D}/loot_table/chests/einde2_bouw_water_{naam}.json", {"type": "minecraft:chest", "pools": [{
            "rolls": {"type": "minecraft:uniform", "min": rollen[0], "max": rollen[1]},
            "entries": [{"type": "minecraft:item", "name": item, "weight": gewicht, "functions": h.count_fn(lo, hi)} for item, gewicht, lo, hi in dingen]}]})
    # what a guh of the Waterstam keeps at home: fish, kelp, a snowball, a few knabbels
    kist("huis", (2, 4), [("minecraft:cod", 8, 1, 3), ("minecraft:salmon", 6, 1, 2), ("minecraft:dried_kelp", 6, 2, 5), ("minecraft:snowball", 5, 2, 6),
                          ("guhs:kaas_knabbels", 6, 1, 3), ("minecraft:string", 3, 1, 3), ("minecraft:light_blue_wool", 3, 1, 2), ("minecraft:bone", 2, 1, 2)])
    # Yue-guh's chest: a little finer, still nothing a quest needs
    kist("paleis", (3, 5), [("guhs:kaas_knabbels", 8, 2, 5), ("minecraft:lapis_lazuli", 5, 1, 4), ("minecraft:prismarine_crystals", 4, 1, 3),
                            ("minecraft:cooked_salmon", 5, 1, 3), ("minecraft:white_wool", 3, 1, 3), ("guhs:einde2_bouw_water_ijslantaarn", 2, 1, 2),
                            ("minecraft:nautilus_shell", 1, 1, 1)])
    # Zhao's sea chest: coals, iron, and the knabbels he "found"
    kist("schip", (3, 5), [("minecraft:coal", 8, 2, 6), ("minecraft:charcoal", 5, 1, 4), ("minecraft:iron_nugget", 6, 2, 6), ("guhs:kaas_knabbels", 6, 2, 4),
                           ("minecraft:cooked_cod", 4, 1, 2), ("minecraft:iron_ingot", 2, 1, 2), ("minecraft:map", 2, 1, 1)])


def tags(h):
    g = lambda names: [f"guhs:{n}" for n in names]  # noqa: E731
    h.add_tag("minecraft/tags/block/mineable/pickaxe", g(BLOKKEN))
    for kind in ("block", "item"):
        h.add_tag(f"minecraft/tags/{kind}/stairs", g([IJSSTEEN + "_trap"]))
        h.add_tag(f"minecraft/tags/{kind}/slabs", g([IJSSTEEN + "_plaat"]))


def recipes(h):
    D = h.D
    n = f"guhs:{IJSSTEEN}"
    h.shaped(IJSSTEEN, ["PP", "PP"], {"P": "minecraft:packed_ice"}, n, 4)
    h.shaped(IJSSTEEN + "_trap", ["S  ", "SS ", "SSS"], {"S": n}, n + "_trap", 4)
    h.shaped(IJSSTEEN + "_plaat", ["SSS"], {"S": n}, n + "_plaat", 6)
    for out, c in ((IJSSTEEN + "_trap", 1), (IJSSTEEN + "_plaat", 2)):
        h.w(f"{D}/recipe/{out}_steenzagen.json", {"type": "minecraft:stonecutting", "ingredient": {"item": n},
                                                  "result": {"id": f"guhs:{out}", "count": c}})
    h.shaped("einde2_bouw_water_ijslantaarn", [" S ", "SKS", " S "], {"S": n, "K": "guhs:guh_kristal"}, "guhs:einde2_bouw_water_ijslantaarn", 4)
    h.shaped("einde2_bouw_water_stamteken", ["SL", "LS"], {"S": n, "L": "minecraft:lapis_lazuli"}, "guhs:einde2_bouw_water_stamteken", 2)
    h.shaped("einde2_bouw_water_vuurnatie_teken", ["HR", "RH"], {"H": "guhs:houtskoolsteen_stenen", "R": "minecraft:red_dye"},
             "guhs:einde2_bouw_water_vuurnatie_teken", 2)


def texts(h):
    for key, text in TEXTS.items():
        kit.lang(h, key, text)


def structuur(h):
    b, _ = bouw.bouw(h)
    kit.leegte_structuur(h, b, titel="Noordelijke Waterstam", tooltip="Een stad van ijs en grachten achter een reusachtige ijsmuur", y=bouw.WERELD_Y)
    # the sea of a Waterstam is blue: only the streken with blue-green water (Deviations: 6.4 says all six)
    h.w(f"{h.D}/tags/worldgen/biome/has_structure/{NAAM}.json", {"values": [f"guhs:{s}" for s in WATER_STREKEN]})
    print(f"{NAAM}: self-check ok {b.stats}, {b.lampen} lampen gelegd, {b.tiles[0]} x {b.tiles[1]} tiles ({len(b.tiles[2])} with blocks)")
    return b


def testkamer(h):
    """The room of E2BouwWaterGameTests: a floor of ice bricks with one of each of our blocks on it and a little canal."""
    s = h.Structure((9, 5, 9))
    s.fill(0, 0, 0, 8, 0, 8, f"guhs:{IJSSTEEN}")
    for i, blok in enumerate(BLOKKEN):
        props = {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"} if blok.endswith("_trap") else (
            {"type": "bottom", "waterlogged": "false"} if blok.endswith("_plaat") else None)
        s.set(1 + i, 1, 1, f"guhs:{blok}", props)
    s.fill(2, 0, 4, 6, 0, 6, "minecraft:water", {"level": "0"})
    s.save("einde2_bouw_water_test_kamer")


def selfcheck(h, b):
    A, D = h.A, h.D
    missing = []
    for blok in BLOKKEN:
        for path in (f"{A}/blockstates/{blok}.json", f"{A}/models/item/{blok}.json", f"{D}/loot_table/blocks/{blok}.json"):
            if not os.path.exists(path):
                missing.append(path)
    for n in tex.BLOCKS:
        if not os.path.exists(os.path.join(h.TEX, "block", f"{n}.png")):
            missing.append(f"texture {n}")
    missing += [k for k in TEXTS if k not in h.NL]
    for naam in ("huis", "paleis", "schip"):
        if not os.path.exists(f"{D}/loot_table/chests/einde2_bouw_water_{naam}.json"):
            missing.append(f"loot {naam}")
    for i, j in b.tiles[2]:
        if not os.path.exists(f"{D}/structure/{NAAM}/stuk_{i}_{j}.nbt"):
            missing.append(f"tile {i},{j}")
    for path in (f"{D}/structure/{NAAM}/laag_vloot.nbt", f"{D}/einde2/plekken/{NAAM}.json", f"{D}/worldgen/structure/{NAAM}.json",
                 f"{D}/worldgen/structure_set/{NAAM}_gegarandeerd.json"):
        if not os.path.exists(path):
            missing.append(path)
    if f"structure.guhs.{NAAM}" not in h.NL:
        missing.append("the name of the structure")
    if missing:
        raise SystemExit(f"einde2_bouw_water: missing: {missing}")


def build(h):
    tex.textures(h)
    blocks_and_items(h)
    loot(h)
    tags(h)
    recipes(h)
    texts(h)
    b = structuur(h)
    testkamer(h)
    selfcheck(h, b)
