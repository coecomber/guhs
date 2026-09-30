"""
De Elf-Guhjestocht (2.9, the showpiece of De Grote Guhspelen; the polder rebuilt in 2.10) - Java: feature/elftocht.

  build(h)      textures and models (elftocht_tex), the characters (elftocht_npcs), sounds (the OGGs are made by
                elftocht_geluid.py, run separately), tags, lang (elftocht_tekst), advancements, the structure: the tour's
                template (elftocht_bouw + elftocht_route + the village modules elftocht_dorp_<nn>_<slug>.py), its structure
                type / placement json (guhs:elfguhjestocht + guhs:elftocht_piek, one per Guhpolder on the noise peak), and
                the gametest templates. Geometry self-checks raise SystemExit.
  ftb(fq)       the FTB quests of the tour.
  BONES / clothes / icons / CLOTHES: the tour's clothes (pompom hat, woollen jumper, orange scarf, fluffy ear warmers).

The Guhpolder (biome, noise, blocks) is the guhpolder module's (it runs before this one). Read from it (all optional):
NOISE, BIOME, ELFTOCHT_PIEK = {"min_value", "dal_value", "flat_value", "flat_radius", optional "vlak" (the dead-flat
polder rules for the spot search, ElftochtPiek.Vlak) and "max_ongelijk" (the real-terrain guard)}. While its biome/noise/blocks are not
generated yet, a stand-in is used (Knuffeldal biome + knuffel noise, vanilla blocks) and a WARNING is printed.
"""
import os

from features import elftocht_bouw as bouw
from features import elftocht_npcs as npcs
from features import elftocht_route as route
from features import elftocht_tekst as tekst
from features import elftocht_tex as tex
from features import spelen
from features import sterrenwacht_hulp as hulp

NAME = "elfguhjestocht"
SALT = 20290601
# the reach of the tour's pieces from its anchor, per axis (make_v2.bouwruimte adds 1 + cell_chunks * 16): the 256 wide
# template with its anchor in the middle
REACH = 128
# the template y of the polder ground (the ice and the rijpgras) at the anchor: make_v2.grond() (the 2.10 terrain fix)
# lets the world's terrain meet the structure there
GROND_Y = route.GY
CELL = 32            # the placement's cells (chunks): one polder peak per cell of 512 x 512 blocks
PIEK = {"min_value": 0.585, "dal_value": 0.47, "flat_value": 0.53, "flat_radius": 100}
STANDIN = {"noise": "guhmension_knuffel", "biome": "knuffeldal", "min_value": 0.585, "dal_value": 0.47, "flat_value": 0.5, "flat_radius": 0}
FTB_Y = 120

# ======================================================================================================================
# the tour's clothes (Schaatsmeester Guhglij's shop)
# ======================================================================================================================
_H = [0, 6, -2]
BONES = {
    # a knitted beanie with a folded rim and a big pompom
    "outfit_elftocht_muts": ("head", _H, "elftocht_muts", [([-4.4, 14.6, -9.4], [8.8, 1.4, 7.8], 0.1),
                                                          ([-4.0, 16.0, -9.0], [8.0, 2.6, 7.0], 0),
                                                          ([-3.2, 18.6, -8.2], [6.4, 0.8, 5.4], 0)]),
    "outfit_elftocht_muts_pompom": ("head", _H, "elftocht_pompom", [([-1.3, 19.2, -6.8], [2.6, 2.6, 2.6], 0)]),
}
BONES.update(spelen.oren("outfit_oren_warmer", "oren_warmer", inflate=0.45))
CLOTHES = ["elftocht_schaatsmuts", "elftocht_truitje", "elftocht_sjaal", "elftocht_oorwarmers"]


def clothes(rng, v):
    def muts():
        return v.band((242, 120, 36), (255, 255, 255), rng, [4, 14, 24])

    def pompom():
        return v.fabric((255, 255, 255), rng, 18)

    def truitje():
        # a woollen jumper: cream with a band of little blue snowflakes and pink guh hearts, knitted rows
        a = v.fabric((246, 240, 226), rng, 8)
        px = a.shape[0]
        for y in range(0, px, 3):
            a[y, :] = a[y, :] * 0.94
        for x in range(1, px, 6):
            a[9:12, x:x + 2] = (70, 110, 200)
            a[10, max(0, x - 1):x + 3] = (70, 110, 200)
        for x in range(4, px, 8):
            a[18:20, x:x + 3] = (240, 120, 170)
            a[20, x + 1] = (240, 120, 170)
        a[24:26, :] = (242, 120, 36)
        return a

    def sjaal():
        a = v.fabric((242, 120, 36), rng, 8)
        a[:, ::7] = a[:, ::7] * 0.9
        a[26:30, :] = (255, 255, 255)
        return a

    def oren():
        return v.fabric((255, 214, 232), rng, 22)

    return {
        "elftocht_schaatsmuts": {"elftocht_muts": muts, "elftocht_pompom": pompom},
        "elftocht_truitje": {"suit": truitje},
        "elftocht_sjaal": {"scarf": sjaal},
        "elftocht_oorwarmers": {"oren_warmer": oren},
    }


def icons(ic):
    muts = ["......bb......", ".....bbbb.....", "......bb......", "....aaaaaa....", "...aOOOOOOa...", "..aOwwwwwwOa..",
            "..aOOOOOOOOa..", "..awwwwwwwwa..", "..aaaaaaaaaa.."]
    trui = ["...aaa....aaa...", "..abbbaaaabbba..", ".abbbbbbbbbbbba.", ".abcbbcbbcbbcba.", ".aabbbbbbbbbbaa.",
            "..aabdbbdbbdaa..", "...abbbbbbbba...", "...aooooooooa...", "...abbbbbbbba...", "...aaaaaaaaaa..."]
    sjaal = ["..aaaaaaaaaaaa..", ".aoooooooooooooa", ".aoooooooooooooa", "..aaaaaaaoooa...", "........aoooa...",
             "........aoooa...", "........awwwa...", "........aoooa...", "........awwwa...", "........aaaaa..."]
    oren = ["..aaa......aaa..", ".abbba....abbba.", ".abpba....abpba.", ".abbbaaaaaabbba.", "..aaa.a..a.aaa..",
            "......a..a......", "......aaaa......"]
    return {
        "elftocht_schaatsmuts": ic.icon(ic.pad(muts), {"a": (150, 70, 20), "O": (242, 120, 36), "w": (255, 255, 255), "b": (250, 250, 255)}),
        "elftocht_truitje": ic.icon(ic.pad(trui), {"a": (150, 130, 110), "b": (246, 240, 226), "c": (70, 110, 200), "d": (240, 120, 170),
                                                    "o": (242, 120, 36)}),
        "elftocht_sjaal": ic.icon(ic.pad(sjaal), {"a": (150, 70, 20), "o": (242, 120, 36), "w": (255, 255, 255)}),
        "elftocht_oorwarmers": ic.icon(ic.pad(oren), {"a": (170, 110, 140), "b": (255, 214, 232), "p": (255, 180, 210)}),
    }


# ======================================================================================================================
# the Guhpolder (the guhpolder module's): real or stand-in
# ======================================================================================================================
def polder(h):
    """(echt, noise id path, biome id path, thresholds) - echt when the guhpolder module has made the biome, noise and blocks."""
    try:
        from features import guhpolder
    except ImportError:
        guhpolder = None
    noise = getattr(guhpolder, "NOISE", "guhmension_polder")
    biome = getattr(guhpolder, "BIOME", "guhpolder")
    echt = (os.path.exists(f"{h.D}/worldgen/noise/{noise}.json") and os.path.exists(f"{h.D}/worldgen/biome/{biome}.json")
            and all(os.path.exists(f"{h.A}/blockstates/{b.split(':')[1]}.json") for b in bouw.POLDER_BLOCKS))
    if not echt:
        print("WARNING elftocht: the Guhpolder (biome/noise/blocks) isn't generated yet: using the stand-in "
              f"(biome {STANDIN['biome']}, noise {STANDIN['noise']}, vanilla blocks) - this is only right before the merge")
        return False, STANDIN["noise"], STANDIN["biome"], dict(STANDIN)
    piek = dict(PIEK)
    piek.update(getattr(guhpolder, "ELFTOCHT_PIEK", {}) or {})
    return True, noise, biome, piek


def structure_json(h, echt, noise, biome, piek):
    none = {"bounding_box": "full", "spawns": []}
    h.w(f"{h.D}/worldgen/structure/{NAME}.json", {
        "type": f"guhs:{NAME}", "biomes": f"#guhs:has_structure/{NAME}", "step": "surface_structures",
        "spawn_overrides": {"monster": none, "ambient": none}, "terrain_adaptation": "beard_box",
        "start_pool": f"guhs:{NAME}/start", "start_jigsaw_name": f"guhs:{NAME}_midden",
        "polder_noise": f"guhs:{noise}", "cell": CELL, "cell_chunks": 1,
        "min_value": piek["min_value"], "dal_value": piek["dal_value"], "flat_value": piek["flat_value"], "flat_radius": piek["flat_radius"],
        "max_distance_from_center": 128, "keep_clear": REACH + 17, "voorrang": 900,
        **({"vlak": piek["vlak"], "max_ongelijk": piek.get("max_ongelijk", 2)} if piek.get("vlak") else {})})
    h.w(f"{h.D}/worldgen/template_pool/{NAME}/start.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:{NAME}",
                                  "projection": "rigid", "processors": "minecraft:empty"}}]})
    h.w(f"{h.D}/worldgen/structure_set/{NAME}.json", {
        "structures": [{"structure": f"guhs:{NAME}", "weight": 1}],
        "placement": {"type": "guhs:elftocht_piek", "salt": SALT, "spacing": CELL, "noise": f"guhs:{noise}",
                      **({"vlak": piek["vlak"]} if piek.get("vlak") else {})}})
    h.w(f"{h.D}/tags/worldgen/biome/has_structure/{NAME}.json", {"values": [f"guhs:{biome}"]})


# ======================================================================================================================
# the rest of the resources
# ======================================================================================================================
def tags(h):
    h.w(f"{h.D}/tags/block/elftocht/schaatsijs.json", {"replace": False, "values": [
        "minecraft:ice", "minecraft:packed_ice", "minecraft:blue_ice", "minecraft:frosted_ice", {"id": "guhs:polderijs", "required": False}]})
    h.add_tag("guhs/tags/item/loaned", ["guhs:guh_schaatsen", "guhs:stempelkaart"])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:elf_guhjeskruisje", "guhs:elftocht_vuurkorf"])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:elftocht_kopjes"])


def sounds(h):
    hulp.sounds(h, {
        "elftocht.glij": [{"name": "guhs:elftocht/glij"}],
        "elftocht.kras": [{"name": f"guhs:elftocht/kras{i}"} for i in (1, 2, 3)],
        "elftocht.plof": [{"name": f"guhs:elftocht/plof{i}"} for i in (1, 2)],
        "elftocht.fluit": [{"name": "guhs:elftocht/fluit"}],
        "elftocht.juich": [{"name": f"guhs:elftocht/juich{i}"} for i in (1, 2, 3)],
        "elftocht.finish": [{"name": "guhs:elftocht/finish"}],
    })


def advancements(h):
    names = tekst.ADVANCEMENTS
    parent = {"elftocht_gevonden": "grote_guhspelen/root", "elftocht_eerste_rit": "grote_guhspelen/elftocht_gevonden",
              "elftocht_uitgereden": "grote_guhspelen/elftocht_eerste_rit", "elftocht_snel": "grote_guhspelen/elftocht_uitgereden",
              "elftocht_kleding": "grote_guhspelen/elftocht_eerste_rit"}
    icon = {"elftocht_gevonden": "guhs:elftocht_lampion", "elftocht_eerste_rit": "guhs:guh_schaatsen",
            "elftocht_uitgereden": "guhs:elf_guhjeskruisje", "elftocht_snel": "guhs:elfstempel", "elftocht_kleding": "guhs:elftocht_schaatsmuts"}
    frame = {"elftocht_uitgereden": "challenge", "elftocht_snel": "challenge", "elftocht_kleding": "goal"}
    for name, (title, desc) in names.items():
        if name == "elftocht_gevonden":
            crit = {"done": {"trigger": "minecraft:location", "conditions": {"player": [{"condition": "minecraft:entity_properties",
                    "entity": "this", "predicate": {"location": {"structures": f"guhs:{NAME}"}}}]}}}
        else:
            crit = {"done": {"trigger": "minecraft:impossible"}}
        h.w(f"{h.D}/advancement/grote_guhspelen/{name}.json", {
            "parent": f"guhs:{parent[name]}",
            "display": {"icon": {"id": icon[name]}, "title": {"translate": f"advancements.guhs.grote_guhspelen.{name}.title"},
                        "description": {"translate": f"advancements.guhs.grote_guhspelen.{name}.description"},
                        "frame": frame.get(name, "task"), "show_toast": True, "announce_to_chat": name in frame, "hidden": False},
            "criteria": crit})
        h.lang(f"advancements.guhs.grote_guhspelen.{name}.title", title, title)
        h.lang(f"advancements.guhs.grote_guhspelen.{name}.description", desc, desc)
    # the Guhdex pages of the two characters
    hulp.quest_advancements(h, ["seen_schaatsmeesterguh", "seen_stempelguh"])


def lang(h):
    for k, v in tekst.TEKSTEN.items():
        h.lang(k, v, v)
    # the villages' own sign texts (modules that use translate keys with a Dutch fallback export them)
    for index, mod in sorted(bouw.dorp_modules().items()):
        if hasattr(mod, "teksten"):
            for k, v in mod.teksten().items():
                h.lang(k, v, v)


def test_templates(h):
    """elftocht_test_baan: an ice rink (24 x 6 x 16, the floor at y 0) with Schaatsmeester Guhglij (the start 4 ahead,
    2 to his left), the Stempelguhs of villages 2, 3 and 1, an audience guh, a tray of cups, a lampion and a vuurkorf;
    a strip of grass (z 0..1) to walk off the ice."""
    from make_structures import Byte, floats, Float
    s = h.Structure((24, 6, 16))
    for x in range(24):
        for z in range(16):
            s.set(x, 0, z, "minecraft:grass_block" if z <= 1 else "minecraft:packed_ice")
    s.entity(3.5, 1, 2.5, {"id": "guhs:guh_npc", "Kind": "schaatsmeesterguh", "PersistenceRequired": Byte(1), "Rotation": floats(0.0, 0.0),
                           "RoleData": {"StartVooruit": Float(4.0), "StartLinks": Float(2.0)}})
    for x, dorp in ((9, 2), (13, 3), (17, 1)):
        bouw.stempelguh(s, x, 1, 2, 0, dorp)
    bouw.guh_publiek(s, 12, 1, 12, 180)
    s.set(20, 1, 12, "guhs:vadshout_planken")
    s.set(20, 2, 12, "guhs:elftocht_kopjes", {"facing": "south", "soort": "chocovet"})
    s.set(22, 1, 12, "guhs:elftocht_lampion", {"hanging": "false", "lit": "false", "waterlogged": "false"})
    s.set(22, 1, 14, "guhs:elftocht_vuurkorf", {"lit": "false"})
    s.save("elftocht_test_baan")


def tocht(h, echt):
    s, info = bouw.build(h, echt=echt)
    bouw.check(s, info)
    s.save(NAME)
    print(f"elftocht: route {info['length']:.0f} blocks, {len(info['boost'])} boost spots, {len(info['lichten'])} night lights, "
          f"{len(info['publiek']) + len(info['publiek_eigen'])} audience guhs, {info.get('bruggen')} bridges"
          + ("" if echt else f", {info['standin']} stand-in blocks"))
    print("  elftocht: villages every " + ", ".join(f"{g:.0f}" for _, _, g in route.gaten()) + " blocks along the route; "
          f"{len(info.get('sloten', []))} ditches, relief up to {max(info.get('reliëf', {0: 0}).values()) / 8:.2f} blocks")
    for line in info["report"]:
        print("  elftocht:", line)
    return info


def build(h):
    echt, noise, biome, piek = polder(h)
    tex.models(h)
    npcs.build(h)
    tags(h)
    sounds(h)
    lang(h)
    advancements(h)
    structure_json(h, echt, noise, biome, piek)
    tocht(h, echt)
    test_templates(h)
    selfcheck(h)


def selfcheck(h):
    problems = []
    for p in (f"{h.A}/sounds/elftocht/glij.ogg", f"{h.A}/sounds/elftocht/plof1.ogg", f"{h.A}/geckolib/models/entity/guh_npc_stempelguh.geo.json",
              f"{h.D}/structure/{NAME}.nbt", f"{h.D}/structure/elftocht_test_baan.nbt"):
        if not os.path.exists(p):
            problems.append(f"missing {p}")
    if REACH + 1 + 16 < 136:
        problems.append("REACH too small for the template")
    if route.SIZE_X // 2 > REACH or route.SIZE_Z // 2 > REACH:
        problems.append("the template is wider than REACH allows")
    if problems:
        raise SystemExit("elftocht selfcheck: " + "; ".join(problems))


# ======================================================================================================================
# FTB
# ======================================================================================================================
def ftb(fq):
    q, y = fq.q, FTB_Y
    q("elftocht_vinden", "Een bevroren kanaal", "Ergens in een ijskoude &bGuhpolder&r ligt een bevroren kanaal langs elf dorpjes: de "
      "&6Elf-Guhjestocht&r! Het Superkompas (tabblad minigames) wijst je de weg.", "guhs:elftocht_lampion",
      [fq.adv("guhs:grote_guhspelen/elftocht_gevonden")], rewards=(("guhs:kaas_knabbels", 8),), x=0, y=y, shape="circle", xp=100)
    q("elftocht_start", "It giet oan!", "Praat in Guhwarden met &6Schaatsmeester Guhglij&r en start de tocht. Houd de guh-schaatsen "
      "vast: op het ijs glij je supervahoeg!", "guhs:guh_schaatsen", [fq.adv("guhs:grote_guhspelen/elftocht_eerste_rit")],
      rewards=(("guhs:warme_chocovet", 2),), deps=["elftocht_vinden"], x=1.5, y=y, xp=100)
    q("elftocht_uit", "Elf-Guhjeskruisje", "Haal bij elke &6Stempelguh&r een stempel, in de goede volgorde (Snuh eerst, Guhwarden "
      "laatst), en schaats zo de hele tocht uit. Elke tocht geeft &612 elfstempels&r (tot 8 extra als je snel bent), en je eerste "
      "keer krijg je het &6Elf-Guhjeskruisje&r en 5 elfstempels extra!", "guhs:elf_guhjeskruisje",
      [fq.adv("guhs:grote_guhspelen/elftocht_uitgereden")], rewards=(("guhs:elfstempel", 5),), deps=["elftocht_start"], x=3, y=y,
      shape="gear", xp=400)
    q("elftocht_snel", "Supervahoeg", "Rij de Elf-Guhjestocht in minder dan 3:40. Tip: warme chocovet en snert geven een boost, en "
      "sprinten op het ijs gaat het hardst! Zo snel krijg je 8 elfstempels extra.", "guhs:elfstempel", [fq.adv("guhs:grote_guhspelen/elftocht_snel")],
      rewards=(("guhs:elfstempel", 6),), deps=["elftocht_uit"], x=4.5, y=y, xp=300)
    q("elftocht_kleding", "Warm ingepakt", "Koop bij Schaatsmeester Guhglij de schaatsmuts, het schaatstruitje, de sjaal en de "
      "oorwarmers, en ontgrendel ze voor je guhs.", "guhs:elftocht_schaatsmuts", [fq.adv("guhs:grote_guhspelen/elftocht_kleding")],
      rewards=(("guhs:elfstempel", 2),), deps=["elftocht_start"], x=3, y=y + 1.5, xp=150)
