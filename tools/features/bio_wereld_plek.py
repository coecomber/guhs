"""
biomes3 wereld: guhs:bio_plek for the building slices (not in FEATURES; bio_wereld.py calls build()).

A structure of type guhs:bio_plek starts at a named kind of spot of the three biomes' terrain (Java: BioPlekStructure,
BioPlekken). A slice writes it with the generator's own h.structure(...) and then one call:

    from features import bio_wereld_plek as bio_plek

    h.structure("dal_boogbrug", ["klaterdal"], spacing=5, separation=2, salt=21500601, reach=24, centre="guhs:dal_boogbrug_midden")
    bio_plek.plek(h, "dal_boogbrug", "over_rivier")

    plek(h, naam, soort, hoogte=None, ruimte=None, terrain_adaptation="none")
        naam    the structure (written by h.structure just before)
        soort   SOORTEN: "terras", "oever", "over_rivier", "waterval", "rots" (Klaterdal); "meer_oever", "meer_eiland",
                "meer_boom" (Bloesemmeertje; a shore can lie in the Klaterdal's valley floor too: give both biomes);
                "weide", "lucht", "zweefeiland" (Wolkenweide)
        hoogte  kind "lucht": blocks above the meadow where the start jigsaw comes (default 24)
        ruimte  kind "lucht": no natural island or cloud within this many blocks of the start (default 20, at most 64);
                make it the reach of your template from its anchor, plus a few
        terrain_adaptation   "none" (default: the terrain is exact already), or e.g. "beard_thin"

The template, as for every guhs structure, with these rules of bio_plek:
  - the START JIGSAW (the `centre` you gave h.structure: a jigsaw block with that name, pool "minecraft:empty") lands IN
    the top ground block of the spot (its final_state replaces that block). So the template's layer at the jigsaw's y is
    the ground layer; what must go into the ground (foundations, a river bed) lies below it, the building above it.
    For "over_rivier" the spot's y is the top of the BANKS (the water is one below); for "lucht" it is `hoogte` above
    the meadow; for "zweefeiland" the island's top.
  - the template's NORTH side (low z, as you build it) is turned to what the spot looks at: the river ("oever",
    "terras" when a river is near), the open water ("meer_oever", "meer_eiland"), the waterfall ("waterval"), out over
    the fall ("rots"), the big tree ("meer_boom"). For "over_rivier" north points ALONG the river: build the bridge
    from west to east. "weide", "lucht" and "zweefeiland" get a random quarter turn.
  - a spot is searched in the start chunk only, and the structure gives up when that chunk has none. Spots like a
    waterfall are rare per chunk: use a SMALL spacing (2-4) for those and let the kind of spot do the thinning.
  - use structure_void (not air) in the template where the terrain must stay (water under a bridge, the river beside a
    bank), and leave out what would cut into a rock face.
  - no guaranteed copy, no *_gegarandeerd set.

build() below writes the TEST structures: one tiny marker per kind (<biome>_plektest_<kind>: a coloured 5 x 5 plate, a
glowing post on the spot and a red block on the north side), with alleen_test: they only generate on a server started
with the environment variable GUHS_BIO_PLEKTEST set, never in a normal world.
"""
from features import bio_wereld_eiland as eiland

SOORTEN = {  # kind -> (biomes, the colour of its test plate)
    "terras": (["klaterdal"], "lime"), "oever": (["klaterdal"], "yellow"), "over_rivier": (["klaterdal"], "orange"),
    "waterval": (["klaterdal"], "light_blue"), "rots": (["klaterdal"], "gray"),
    "meer_oever": (["bloesemmeertje", "klaterdal"], "cyan"), "meer_eiland": (["bloesemmeertje"], "green"),
    "meer_boom": (["bloesemmeertje"], "brown"),
    "weide": (["wolkenweide"], "magenta"), "lucht": (["wolkenweide"], "purple"), "zweefeiland": (["wolkenweide"], "blue"),
}
TEST = "plektest"
SALT = 21500101   # the wereld slice's structure sets: 21500101, 21500111, ...


def plek(h, naam, soort, hoogte=None, ruimte=None, terrain_adaptation="none", alleen_test=False):
    """Turns the structure `naam` (written by h.structure) into a guhs:bio_plek structure of this kind of spot."""
    assert soort in SOORTEN, f"bio_plek: unknown kind {soort} (one of {', '.join(SOORTEN)})"
    hooks(h)

    def patch(s):
        assert s["type"] in ("guhs:flat_jigsaw", "guhs:bio_plek"), f"bio_plek: {naam} is a {s['type']}"
        s["type"] = "guhs:bio_plek"
        s["plek"] = soort
        s["terrain_adaptation"] = terrain_adaptation
        s["jigsaw"]["terrain_adaptation"] = terrain_adaptation
        for oud in ("check_radius", "max_height_difference"):
            s.pop(oud, None)
        if hoogte is not None:
            assert soort == "lucht", "hoogte only for kind lucht"
            s["hoogte"] = int(hoogte)
        if ruimte is not None:
            assert soort == "lucht" and 0 <= ruimte <= 64, "ruimte only for kind lucht, at most 64"
            s["ruimte"] = int(ruimte)
        if alleen_test:
            s["alleen_test"] = True
    h.patch_json(f"{h.D}/worldgen/structure/{naam}.json", patch)


def hooks(h):
    """make_v2's post-pass bouwruimte() for guhs:bio_plek: keep_clear = the reach of its pieces from its anchor, plus
    half a chunk (the anchor sits on the spot, anywhere in the start chunk)."""
    h.RUIMTE_HOOKS["guhs:bio_plek"] = lambda s, jigsaw_reach: jigsaw_reach(s["jigsaw"], True) + 8


def _anker(h, s, x, y, z, naam, final):
    s.set(x, y, z, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": f"guhs:{naam}_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": final, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})


def test_structuren(h):
    """One marker per kind of spot (see the module text)."""
    for i, (soort, (biomen, kleur)) in enumerate(SOORTEN.items()):
        naam = f"{biomen[0]}_{TEST}_{soort}"
        plaat = f"minecraft:{kleur}_concrete"
        if soort == "lucht":
            # a small island of the builder with the marker on it (so the builder is used by a real template too)
            s = h.Structure((21, 26, 21))
            tops = eiland.eiland(h, s, (10, 18, 10), 6, 5, zaad=naam)
            eiland.wolk(h, s, (4, 21, 15), 4, 2, 3, zaad=naam, kleur="roze")
            for (x, z) in tops:
                if abs(x - 10) <= 2 and abs(z - 10) <= 2:
                    s.set(x, 18, z, plaat)
            cx, y, cz = 10, 18, 10
        else:
            s = h.Structure((5, 5, 5))
            for x in range(5):
                for z in range(5):
                    if soort == "over_rivier" and x not in (0, 4) and (x, z) != (2, 2):
                        continue   # (a bridge: two bank plates and the middle; the water stays)
                    s.set(x, 0, z, plaat)
            if soort == "over_rivier":
                for x in range(5):
                    s.set(x, 1, 2, plaat)
            cx, y, cz = 2, 0, 2
        _anker(h, s, cx, y, cz, naam, plaat)
        for dy in range(1, 4):
            s.set(cx, y + dy, cz, "minecraft:glowstone")
        s.set(cx, y + 1, cz - 2, "minecraft:red_wool")          # the north side: what the spot looks at
        s.set(cx, y + 2, cz - 2, "minecraft:red_wool")
        s.save(naam)
        # (the air test sparse: every start of kind lucht keeps natural islands away, and the island test needs some)
        ruim = soort == "lucht"
        h.structure(naam, biomen, spacing=10 if ruim else 3, separation=4 if ruim else 1, salt=SALT + 10 * i, reach=16, centre=f"guhs:{naam}_midden")
        plek(h, naam, soort, hoogte=30 if soort == "lucht" else None, ruimte=14 if soort == "lucht" else None, alleen_test=True)


def build(h, lib):
    hooks(h)
    test_structuren(h)
