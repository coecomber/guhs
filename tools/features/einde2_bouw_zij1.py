"""
einde2 (the Guheinde expansion), slice bouw_zij1: three big side places of the outer islands, as buildings with markers.
Java: nl.juiced.guhs.feature.einde2.bouwzij1 (E2BouwZij1Feature, PostgootBlock, E2BouwZij1GameTests).

  - Omashguh (Kristalkaasklippen): a walled mountain town of three tiers, the palace of koning Bumi-guh on top, three trial
    rooms inside the mountain, and the mail chutes: three slides of postgoot blocks you can really ride, from the palace
    down to the post office (einde2_bouw_zij1_omashguh.py);
  - the Westelijke Luchttempel (Kruimelwolken): it hangs upside down under its island: a hall, four wind corridors to four
    hanging pagodas, the big middle pagoda with the organ pipe, and the stable of the luchtbizon-guhs inside the island
    (einde2_bouw_zij1_luchttempel.py);
  - the Vuurtempel van Roku-guh (Knabbelkoorbos): a crescent island with a pagoda of five storeys; the top one is cut in
    two by five fire doors, with the sanctum behind them (einde2_bouw_zij1_vuurtempel.py).

Each is a guhs:leegte_bouw that brings its own floating island: one guaranteed copy per world plus a very rare random
spread, bound to its biome, in the Superkompas tab "einde", protected (all of that: einde2_kit.leegte_structuur and the
skeleton). No resident and no quest is placed: where the design puts one there is a marker in
data/guhs/einde2/plekken/<id>.json (build contract 6.7), and the side quests come in a later phase.
Every build checks itself while it is generated (reachable, lit, nothing floating, every marker there, every chute ridden
from top to bottom) and stops the generator when something is wrong. The shared tools: einde2_bouw_zij1_gereedschap.py.

Of its own this slice has one block, the postgoot (the chute block: it carries whoever stands on it), and one modest
loot chest (the lost mail of the post office of Omashguh).
"""
import os

import features.einde2_kit as kit
from features import einde2_bouw_zij1_gereedschap as g
from features import einde2_bouw_zij1_luchttempel as luchttempel
from features import einde2_bouw_zij1_omashguh as omashguh
from features import einde2_bouw_zij1_vuurtempel as vuurtempel

FTB_CHAPTER = "guhs_guheinde"
FTB_SECTION = "Grote plekken van de buiteneilanden"

GOOT = "einde2_bouw_zij1_postgoot"
PLEKKEN = {"omashguh": omashguh, "westelijke_luchttempel": luchttempel, "vuurtempel_roku": vuurtempel}
TEST_GOOT = "einde2_bouw_zij1_test_goot"

TEXTS = {
    f"block.guhs.{GOOT}": "Postgoot",
    f"block.guhs.{GOOT}.lore": "Wie erop staat glijdt mee, njeg. Bukken is remmen.",
}


def textures(h):
    """The postgoot: smooth blue-white ice, on top two chevrons that point the way it slides (north on the texture)."""
    ijs = (168, 208, 244)
    boven = h.noise_tex(ijs, 7, kit.seed("bouw_zij1/postgoot_boven"), spots=[((208, 232, 252), 0.12)]).convert("RGBA")
    px = boven.load()
    for rij, kleur in ((3, (255, 255, 255, 255)), (9, (238, 246, 255, 255))):
        for i in range(5):
            for x in (7 - i, 8 + i):
                px[x, rij + i] = kleur
                px[x, rij + i + 1] = (120, 170, 226, 255) if rij + i + 1 < 16 else px[x, 15]
    for i in range(16):                              # (a darker kerb left and right: the trough)
        px[0, i] = px[15, i] = (104, 150, 212, 255)
    h.save(boven, "block", f"{GOOT}_boven.png")
    zij = h.noise_tex((150, 192, 236), 7, kit.seed("bouw_zij1/postgoot_zij"), spots=[((196, 224, 250), 0.1)]).convert("RGBA")
    px = zij.load()
    for x in range(16):
        px[x, 0] = (228, 242, 255, 255)
        px[x, 1] = (120, 170, 226, 255)
        px[x, 15] = (104, 150, 212, 255)
    h.save(zij, "block", f"{GOOT}.png")


def blocks_and_items(h):
    A = h.A
    h.w(f"{A}/models/block/{GOOT}.json", {"parent": "minecraft:block/cube_bottom_top",
                                         "textures": {"top": f"guhs:block/{GOOT}_boven", "bottom": f"guhs:block/{GOOT}", "side": f"guhs:block/{GOOT}"}})
    h.w(f"{A}/blockstates/{GOOT}.json", {"variants": h.facing_states(GOOT)})
    h.w(f"{A}/models/item/{GOOT}.json", {"parent": f"guhs:block/{GOOT}"})
    h.self_drop(GOOT)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{GOOT}"])


def loot(h):
    """The lost mail of Omashguh: a few knabbels, a crumb of kristalkaas, paper and string. Modest; nothing a quest needs."""
    def item(name, lo, hi, weight):
        e = {"type": "minecraft:item", "name": name, "weight": weight}
        if (lo, hi) != (1, 1):
            e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
        return e
    h.w(f"{h.D}/loot_table/chests/einde2_bouw_zij1_omashguh.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
            item("guhs:kaas_knabbels", 2, 5, 6), item("minecraft:paper", 1, 4, 5), item("minecraft:string", 1, 3, 3),
            item("guhs:kristalkaas_brokje", 1, 3, 3), item("minecraft:feather", 1, 2, 2), item("minecraft:name_tag", 1, 1, 1)]}]})


def test_goot(h):
    """The room of the postgoot game test: a little chute with a step down, a corner and another step, laid by the same
    function as the chutes of Omashguh (and ridden by the same check)."""
    b = kit.Bouw(h, TEST_GOOT, (13, 8, 11), kit.seed(TEST_GOOT), prefix=g.PREFIX)
    b.fill(0, 0, 0, 12, 0, 10, "guhs:klipkaas_stenen")
    b.fill(9, 1, 6, 12, 1, 8, "guhs:klipkaas_stenen")
    cellen = omashguh.goot(b, [(2, 1, 3), (2, 7, 2), (9, 7, 1)])
    b.connect()
    x, y, z = cellen[0]
    rit, problems = g.glijcheck(b, (x, y + 1, z), (9, 2, 7))
    if problems or len(rit) != len(cellen) or b.oob:
        raise SystemExit(f"{TEST_GOOT}: {problems or (len(rit), len(cellen), b.oob[:3])}")
    b.s.save(TEST_GOOT)


def texts(h):
    for key, text in TEXTS.items():
        kit.lang(h, key, text)


def selfcheck(h):
    A, D = h.A, h.D
    missing = [k for k in TEXTS if k not in h.NL]
    for f in (f"{A}/blockstates/{GOOT}.json", f"{A}/models/block/{GOOT}.json", f"{A}/models/item/{GOOT}.json", f"{A}/textures/block/{GOOT}.png",
              f"{A}/textures/block/{GOOT}_boven.png", f"{D}/loot_table/blocks/{GOOT}.json", f"{D}/loot_table/chests/einde2_bouw_zij1_omashguh.json",
              f"{D}/structure/{TEST_GOOT}.nbt"):
        if not os.path.exists(f):
            missing.append(f)
    for name in PLEKKEN:
        for f in (f"{D}/worldgen/structure/{name}.json", f"{D}/worldgen/structure_set/{name}.json", f"{D}/worldgen/structure_set/{name}_gegarandeerd.json",
                  f"{D}/tags/worldgen/biome/has_structure/{name}.json", f"{D}/einde2/plekken/{name}.json", f"{D}/structure/{name}/stuk_1_1.nbt"):
            if not os.path.exists(f):
                missing.append(f)
        for key in (f"structure.guhs.{name}", f"structure.guhs.{name}.tooltip"):
            if key not in h.NL:
                missing.append(key)
    if missing:
        raise SystemExit(f"einde2_bouw_zij1: missing: {missing}")


def build(h):
    textures(h)
    blocks_and_items(h)
    loot(h)
    for module in PLEKKEN.values():
        module.build(h)
    test_goot(h)
    texts(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests: a section of its own at the end of the Guheinde chapter ("find": nothing is locked; the side quests of the
# three places come with the story phase and get their own keys)
# =====================================================================================================================
def ftb(fq):
    q, structure = fq.q, fq.structure
    q("einde2_bouw_zij1_omashguh", "Vind Omashguh", "Boven de &6Kristalkaasklippen&r zweeft een bergstadje van klipkaas: &6Omashguh&r. "
      "De post loopt er niet, de post &oglijdt&r: van het paleis van koning Bumi-guh helemaal naar het postkantoor. Jij mag ook, njeg. "
      "(Het Superkompas kent de weg: tabblad Einde.)", "guhs:einde2_bouw_zij1_postgoot", [structure("omashguh")],
      rewards=(("guhs:kaas_knabbels", 12),), shape="octagon")
    q("einde2_bouw_zij1_westelijke_luchttempel", "Vind de Westelijke Luchttempel", "Boven de &6Kruimelwolken&r zweeft een eiland met alleen wat "
      "gras en een fontein. Kijk eens &oonder&r het eiland: daar hangt de &6Westelijke Luchttempel&r, ondersteboven. De ingang is de "
      "keldertrap. De bizonstal is leeg: de kudde is al een tijdje zoek.", "guhs:kruimelwolk", [structure("westelijke_luchttempel")],
      rewards=(("guhs:kaas_knabbels", 12),), shape="octagon")
    q("einde2_bouw_zij1_vuurtempel_roku", "Vind de Vuurtempel van Roku-guh", "Boven het &6Knabbelkoorbos&r zweeft een eiland in de vorm van "
      "een halve maan, met een rode pagode erop: de &6Vuurtempel van Roku-guh&r. Helemaal bovenin zitten vijf vuurdeuren naast elkaar. "
      "Die zijn dicht. Kloppen helpt niet, maar het is wel beleefd.", "guhs:lichtstaafje", [structure("vuurtempel_roku")],
      rewards=(("guhs:kaas_knabbels", 12),), shape="octagon")
