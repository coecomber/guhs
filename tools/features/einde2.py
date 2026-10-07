"""
einde2 (the Guheinde expansion): what the whole update shares (Java: nl.juiced.guhs.feature.einde2, see Einde2.java). The
fourteen slices each have a module of their own after this one in features.FEATURES (einde2_biomes ... einde2_bouw_zij2);
the helper library they all use is einde2_kit.py.

  - the post-pass hook of the structure type guhs:leegte_bouw (make_v2.bouwruimte writes its keep_clear and voorrang);
  - the biome tags of the outer islands (guhs:einde2/buiteneilanden, gewoon, zeldzaam and one per element), the block tags
    guhs:einde2/eilandgrond (what creatures spawn on and features grow on; the biomes slice adds its surface blocks) and
    guhs:einde2/wolk, the structure tags guhs:einde2/verhaal and guhs:einde2/zij;
  - the hidden advancements quest/einde2_gevonden_<id> of the twelve big places (Einde2Ontdekt grants them);
  - the texts of the shared Java code (protection, the /guhs einde2 commands);
  - einde2_test_bouw: a small guhs:leegte_bouw of 3 x 2 tiles with every kind of marker and an overlay, in no structure set
    (it never generates in a world): the game tests of the type, of Plekken and of the kit's own self-check.
"""
import features.einde2_kit as kit

TEXTS = {
    "gui.guhs.einde2.beschermd": "Njeg! Deze plek blijft heel, voor iedere guh die na jou komt.",
    "gui.guhs.einde2.commando.geen": "Njeg: van %s ligt er in deze wereld geen vaste plek (nog niet gebouwd, of geen plekje gevonden).",
    "gui.guhs.einde2.commando.waar": "%s: startchunk %s, oorsprong %s, doos %s",
    "gui.guhs.einde2.commando.geen_plek": "Njeg: %s heeft geen plek '%s' (of die ligt in deze wereld nog niet vast).",
    "gui.guhs.einde2.commando.plek": "Tuut! %s/%s (%s) op %s",
    "gui.guhs.einde2.commando.plekken": "%s: %s plekken",
    "gui.guhs.einde2.commando.geen_structuur": "Njeg: je staat niet in (of vlak naast) een plek van de buiteneilanden.",
    # (the Mika-vesting joins the Superkompas with einde2; the vesting slice writes the real line with its new build)
    "structure.guhs.mika_vesting.tooltip": "Een vesting van paarse Mika-steen op de buiteneilanden, vol Mika's en buit",
}

TEST_BOUW = "einde2_test_bouw"
TEST_Y = 160


def tags(h):
    D = h.D
    biome = lambda names: {"values": [f"guhs:{n}" for n in names]}  # noqa: E731
    h.w(f"{D}/tags/worldgen/biome/einde2/buiteneilanden.json", biome(kit.BUITEN))
    h.w(f"{D}/tags/worldgen/biome/einde2/gewoon.json", biome(kit.GEWOON))
    h.w(f"{D}/tags/worldgen/biome/einde2/zeldzaam.json", biome(kit.ZELDZAAM))
    for element, name in kit.ELEMENT.items():
        h.w(f"{D}/tags/worldgen/biome/einde2/{element}.json", biome([name]))
    h.add_tag("guhs/tags/block/einde2/eilandgrond", ["guhs:kaaskorst"])
    h.add_tag("guhs/tags/block/einde2/wolk", [])
    # (the structures arrive slice by slice: not required)
    for tag, names in (("verhaal", kit.VERHAAL), ("zij", kit.ZIJ)):
        h.w(f"{D}/tags/worldgen/structure/einde2/{tag}.json", {"values": [{"id": f"guhs:{n}", "required": False} for n in names]})


def advancements(h):
    for name in kit.VERHAAL + kit.ZIJ:
        kit.verborgen(h, f"einde2_gevonden_{name}")


def texts(h):
    for key, text in TEXTS.items():
        kit.lang(h, key, text)


def test_bouw(h):
    """80 x 12 x 48 (3 x 2 tiles, the last column of tiles 16 wide): a slab of kaaskorststenen with a lit walkway, a little
    hut that is shut (a los spot), a pond, one of every kind of marker and an overlay."""
    b = kit.Bouw(h, TEST_BOUW, (80, 12, 48), 21400050, prefix="sign.guhs.einde2")
    b.fill(0, 0, 0, 79, 1, 47, "guhs:kaaskorst")
    b.fill(0, 2, 0, 79, 2, 47, "guhs:kaaskorst_stenen")
    for x in range(4, 80, 8):
        for z in range(4, 48, 8):
            b.set(x, 2, z, "guhs:guh_kristal_lamp")
    # a shut hut (walls all around) with a spot inside, and a pond you wade through
    b.fill(60, 3, 30, 66, 6, 36, "guhs:mika_steen")
    b.fill(61, 3, 31, 65, 5, 35, "minecraft:air")
    b.set(63, 6, 33, "guhs:guh_kristal_lamp")
    b.fill(20, 2, 30, 26, 2, 36, "minecraft:water", {"level": "0"})
    b.sign(40, 3, 24, "south", ["Proefbouw", "njeg"], wall=False)
    b.zone("aankomst", 2, 3, 2, 12, 6, 12, wie="de landingsplek")
    b.prop("aankomst_speler", 7, 3, 7)
    b.prop("uitzicht", -20, 8, 24)
    b.plek("proefguh", 40, 3, 20, yaw=90.0, wie="een proefguh", kind="PROEFGUH")
    b.plek("opgesloten", 63, 3, 33, wie="zit in het hutje", los=True)
    b.prop("tafel", 44, 2, 20, wie="een blok in de vloer")
    b.pad("route", [(7, 3, 7), (40, 3, 7), (72, 3, 40)])
    b.ring("ring_01", 70, 8, 10, as_="x", straal=4)
    b.zone("vijver", 20, 2, 30, 26, 4, 36)
    laag = h.Structure((3, 3, 3))
    laag.fill(0, 0, 0, 2, 0, 2, "guhs:mika_steen")
    laag.set(1, 1, 1, "guhs:guh_kristal_lamp")
    b.laag("proef", laag, 50, 3, 10)
    problems = b.check((7, 3, 7), ["proefguh", "opgesloten", "tafel", "route", ("ring", 1), "vijver"])
    # the check must also catch what is wrong: a marker in the wall, a missing id, a spot nobody can reach
    fout = kit.Bouw(h, TEST_BOUW + "_fout", (16, 8, 16), 21400051)
    fout.fill(0, 0, 0, 15, 0, 15, "guhs:kaaskorst_stenen")
    fout.fill(8, 1, 0, 8, 4, 15, "guhs:mika_steen")
    fout.set(2, 0, 2, "guhs:guh_kristal_lamp")
    fout.plek("in_de_muur", 8, 1, 4)
    fout.plek("erachter", 12, 1, 4)
    found = " | ".join(fout.check((2, 1, 2), ["bestaat_niet"], standaard=False))
    for expected in ("in_de_muur", "erachter", "bestaat_niet", "te donker"):
        if expected not in found:
            problems.append(f"de zelfcontrole mist '{expected}': {found}")
    if problems:
        raise SystemExit(f"{TEST_BOUW}: self-check found {len(problems)} problem(s):\n  " + "\n  ".join(problems[:40]))
    nx, nz = b.save_tiles()
    anker = kit.standaard_anker(b)
    h.w(f"{h.D}/worldgen/structure/{TEST_BOUW}.json", {
        "type": "guhs:leegte_bouw", "biomes": f"#guhs:has_structure/{TEST_BOUW}", "step": "surface_structures", "terrain_adaptation": "none",
        "spawn_overrides": {"monster": {"bounding_box": "full", "spawns": []}, "creature": {"bounding_box": "full", "spawns": []}},
        "templates": f"guhs:{TEST_BOUW}", "tiles_x": nx, "tiles_z": nz, "tile_size": kit.TILE, "anchor": list(anker), "size": [b.W, b.H, b.D],
        "y": TEST_Y, "min_distance": kit.MIN_AFSTAND, "rand": 48})
    h.w(f"{h.D}/tags/worldgen/biome/has_structure/{TEST_BOUW}.json", {"values": ["guhs:guheinde", kit.ALLE_ZES]})
    h.w(f"{h.D}/einde2/plekken/{TEST_BOUW}.json", b.blad("leegte_bouw", anker))
    print(f"{TEST_BOUW}: self-check ok {b.stats}, {nx} x {nz} tiles")


def selfcheck(h):
    missing = [k for k in TEXTS if k not in h.NL]
    for name, info in kit.STRUCTUREN.items():
        if info["slice"] not in ("bouw_lucht", "bouw_water", "bouw_aarde", "bouw_vuur", "bouw_zij1", "bouw_zij2"):
            missing.append(f"slice of {name}")
    salts = [i["salt"] for i in kit.STRUCTUREN.values()] + [i["salt"] + 7 for i in kit.STRUCTUREN.values()]
    if len(set(salts)) != len(salts):
        missing.append("a salt of STRUCTUREN is used twice")
    if sorted(kit.STRUCTUREN) != sorted(kit.VERHAAL + kit.ZIJ):
        missing.append("STRUCTUREN is not VERHAAL + ZIJ")
    if missing:
        raise SystemExit(f"einde2: {missing}")


def build(h):
    h.RUIMTE_HOOKS["guhs:leegte_bouw"] = kit.leegte_reach
    tags(h)
    advancements(h)
    texts(h)
    test_bouw(h)
    selfcheck(h)
