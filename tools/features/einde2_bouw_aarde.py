"""
einde2 (the Guheinde expansion), slice bouw_aarde: the build of Ba Sing Guh, the walled city of the earth chapter of the
Guhvatar story (DESIGN 5, chapter 3). BUILD ONLY: no story logic, no characters; where the story needs one there is a marker
in data/guhs/einde2/plekken/ba_sing_guh.json (build contract 6.6, 6.7).
Java: nl.juiced.guhs.feature.einde2.bouwaarde (E2BouwAardeFeature: the six decorative blocks). The build itself:
einde2_bouw_aarde_bouw.py (the kit), einde2_bouw_aarde_stad.py (above the ground), einde2_bouw_aarde_onder.py (the base under
Meer Laogai, the crystal catacombs); textures: einde2_bouw_aarde_tex.py; wiki texts: einde2_bouw_aarde_wiki.py.

  - ba_sing_guh: one guhs:leegte_bouw of 256 x 110 x 256 in 8 x 8 tiles on its own floating slab, one guaranteed copy per
    world (kit.STRUCTUREN), protected and not in the compass (the skeleton does both);
  - the overlays laag_koolkraam_heel / laag_koolkraam_kapot (the koolman's stall, whole and wrecked: 5 x 4 x 4);
  - six decorative blocks (no behaviour): the wall stone, the aardemunt, the kool, the monorail beam, the green crystal
    of the catacombs and the tea crate, with recipes;
  - five modest chest loot tables, the texts, the test room of the game tests.

stad(h) builds the whole city and returns it unchecked (the preview script of the slice uses it too); build(h) checks it
(every marker of the contract, every floor lit and reachable, nothing floating) and stops the chain on any problem.
"""
import features.einde2_kit as kit
from features import einde2_bouw_aarde_bouw as bouw
from features import einde2_bouw_aarde_onder as onder
from features import einde2_bouw_aarde_stad as boven
from features import einde2_bouw_aarde_tex as tex
from features import einde2_bouw_aarde_verhaal as verhaal

NAAM = bouw.NAAM
WERELD_Y = 138                   # the world y of template y 0: the streets of the lower ring lie at y 184
START = (11, bouw.ONDER + 1, 128)

BLOKKEN = ["einde2_bouw_aarde_stadssteen", "einde2_bouw_aarde_aardemunt", "einde2_bouw_aarde_kool", "einde2_bouw_aarde_spoor",
           "einde2_bouw_aarde_groen_kristal", "einde2_bouw_aarde_theekist"]

TEXTS = {
    "block.guhs.einde2_bouw_aarde_stadssteen": "Stadssteen van Ba Sing Guh",
    "block.guhs.einde2_bouw_aarde_stadssteen.lore": "Hier komt geen Mika doorheen. Zegt de muur.",
    "block.guhs.einde2_bouw_aarde_aardemunt": "Aardemuntsteen",
    "block.guhs.einde2_bouw_aarde_aardemunt.lore": "Het teken van het Aarderijk: rond, met een vierkant hapje eruit",
    "block.guhs.einde2_bouw_aarde_kool": "Kool van de koolman",
    "block.guhs.einde2_bouw_aarde_kool.lore": "MIJN KOLEN!",
    "block.guhs.einde2_bouw_aarde_spoor": "Monorailbalk",
    "block.guhs.einde2_bouw_aarde_spoor.lore": "Zelf duwen, njeg",
    "block.guhs.einde2_bouw_aarde_groen_kristal": "Catacombenkristal",
    "block.guhs.einde2_bouw_aarde_groen_kristal.lore": "Gloeit groen onder Oud Ba Sing Guh",
    "block.guhs.einde2_bouw_aarde_theekist": "Theekist",
    "block.guhs.einde2_bouw_aarde_theekist.lore": "Jasmijn. Ruikt naar Oom Miroh.",
}

# every marker of build contract 6.7 for ba_sing_guh (the standard and story markers are added by kit.Bouw.check)
VERWACHT = ["buitenpoort", "loket", "loketguh", "ring_onder", "ring_midden", "ring_boven", ("bewoner", 10), "monorail_route", "station_buiten",
            "station_onder", "station_midden", "station_boven", "monorail_wagen", "aardrommel", "toph", "kei", "omroeper", "arena_speler",
            "arena_tegenstander", "blinddoek_baan", ("tril_doel", 6), "theehuis", "miroh", "zuka_ober", "theeketel", "pai_sho", "keuken", ("klant", 3),
            "koolman", ("koolkraam", 3), "joo_dee", "joo_dee_huis", "gasthuis", ("dai_li", 6), "paleis", "aardekoning", "bosco", "bosco_kamer",
            "long_feng", "meer_laogai", "basis_ingang", "basis", "sluip_start", "sluip_route", ("sluip_wacht", 6), ("cel", 4), "lampkamer",
            "appa_kamer", "appa_gevangen", "catacomben_ingang", "catacomben", "kristalkamer", "azula", "bliksem_speler", "bliksem_azula",
            "zuka_keuze", "katara_redt", "miroh_gevangen", "vlucht_gat", "knipoog_sjokkel", "knipoog_sjokkel_camera", "knipoog_mewtwo_tank",
            "knipoog_mewtwo_dai_li", "knipoog_mewtwo_mew", "knipoog_mewtwo_camera", ("das_molguh", 2), ("rustpunt", 4)] + verhaal.MARKERS


# =====================================================================================================================
# the city
# =====================================================================================================================
def koolkraam(h, heel):
    s = h.Structure((5, 4, 4))
    bouw.kraam(lambda x, y, z, n, p: s.set(x, y, z, n if ":" in n else "minecraft:" + n, p), 0, 0, 0, heel=heel)
    return s


def stad(h):
    """Builds Ba Sing Guh from the ground up (nothing is saved or checked here)."""
    b = bouw.Stad(h)
    bouw.eiland(b)
    verhaal.heuvel(b)            # (phase B, h3_aarde: the hill of Miroh's song; before the city takes its columns)
    boven.wegen(b)
    boven.muren(b)
    boven.plein(b)
    boven.poortgebouw(b)
    boven.monorail(b)
    boven.aardrommel(b)
    boven.meer(b)
    schuur = boven.botenschuur(b)
    boven.theehuis(b)
    boven.joo_dee(b)
    boven.gasthuis(b)
    boven.paleis(b)
    boven.paleistuin(b)
    oude_poort = boven.oude_poort(b)
    boven.kramen(b)
    boven.lampen(b)
    boven.pleintjes(b)
    boven.wijken(b)
    boven.rommel(b)
    boven.groen(b)
    onder.basis(b, schuur)
    onder.catacomben(b, oude_poort)
    boven.volk(b)
    verhaal.plekken(b)           # (phase B, h3_aarde: the cameras of the story scenes)
    x, z = boven.KRAMEN[0]
    b.laag("koolkraam_heel", koolkraam(h, True), x, bouw.ONDER + 1, z)
    b.laag("koolkraam_kapot", koolkraam(h, False), x, bouw.ONDER + 1, z)
    b.connect()
    bouw.vangnet(b, START)
    return b


def vrij(b, x0, y0, z0, x1, y1, z1):
    """The cells of the box that are not free (a thin thing on the floor does not count)."""
    return [(x, y, z) for x in range(x0, x1 + 1) for y in range(y0, y1 + 1) for z in range(z0, z1 + 1)
            if b.naam(x, y, z) is not None and not b.is_dun(b.naam(x, y, z))]


# properties a block has without its blockstate file naming them (they do not change its model)
STILLE_PROPS = {"waterlogged", "powered", "occupied", "distance", "persistent", "level", "signal_fire", "has_book", "type", "rotation", "age", "part",
                "facing", "lit", "in_wall", "up", "attachment", "hinge", "open", "half", "shape", "axis"}


def staten(h, b):
    """Every block of the build exists and has the properties it is given (as far as the blockstate files of vanilla 1.21
    and of the mod say): a typo here would only show as a hole or a default state in the world."""
    import json
    import os
    import zipfile
    jar = zipfile.ZipFile(os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar"))
    bekend, p = {}, []

    def props_van(name):
        ns, pad = name.split(":")
        try:
            data = json.loads(jar.read(f"assets/minecraft/blockstates/{pad}.json")) if ns == "minecraft" else json.load(open(f"{h.A}/blockstates/{pad}.json", encoding="utf-8"))
        except (KeyError, FileNotFoundError):
            return None
        out = {}
        for key in data.get("variants", {}):
            for kv in key.split(","):
                if "=" in kv:
                    out.setdefault(kv.split("=")[0], set()).add(kv.split("=")[1])
        for part in data.get("multipart", []):
            when = part.get("when", {})
            for c in when.get("OR", when.get("AND", [when])):
                for k in c:
                    out.setdefault(k, None)
        return out

    gezien = set()
    for (x, y, z), (name, props, _) in b.s.blocks.items():
        sleutel = (name, tuple(sorted(props.items())))
        if sleutel in gezien:
            continue
        gezien.add(sleutel)
        if name not in bekend:
            bekend[name] = props_van(name)
        if bekend[name] is None:
            p.append(f"{name} op {(x, y, z)}: dit blok bestaat niet")
            continue
        for k, v in props.items():
            if k in bekend[name]:
                if bekend[name][k] is not None and v not in bekend[name][k]:
                    p.append(f"{name}[{k}={v}] op {(x, y, z)}: die waarde bestaat niet ({sorted(bekend[name][k])})")
            elif k not in STILLE_PROPS:
                p.append(f"{name}[{k}={v}] op {(x, y, z)}: die eigenschap bestaat niet")
    return p


def controle(b):
    """What the contract asks of this build on top of kit.Bouw.check: the free room of the spots that need it, the track,
    a modest number of creatures, and a size that fits the tiles."""
    p = b.check(START, VERWACHT) + staten(b.h, b)
    plek = lambda id: b.plekken[id]["pos"]  # noqa: E731
    if all(k in b.plekken for k in ("appa_kamer", "appa_kamp", "pai_sho", "vlucht_schacht", "monorail_route")):
        (x0, y0, z0), (x1, y1, z1) = b.plekken["appa_kamer"]["pos"], b.plekken["appa_kamer"]["tot"]
        if x1 - x0 + 1 < 11 or y1 - y0 + 1 < 7 or z1 - z0 + 1 < 11 or vrij(b, x0, y0 + 1, z0, x1, y1, z1):
            p.append(f"appa_kamer is geen 11 x 7 x 11 vrij: {vrij(b, x0, y0 + 1, z0, x1, y1, z1)[:4]}")
        x, y, z = plek("appa_kamp")
        if vrij(b, x - 2, y, z - 2, x + 2, y + 3, z + 2):
            p.append(f"appa_kamp: geen 5 x 5 vrij: {vrij(b, x - 2, y, z - 2, x + 2, y + 3, z + 2)[:4]}")
        x, y, z = plek("pai_sho")
        if vrij(b, x - 1, y, z - 1, x + 1, y + 1, z + 1):
            p.append("pai_sho: geen 3 x 3 vrij")
        for i in (1, 2, 3):
            x, y, z = plek(f"koolkraam_{i}")
            if vrij(b, x, y, z, x + 4, y + 3, z + 3):
                p.append(f"koolkraam_{i}: geen 5 x 4 x 4 vrij: {vrij(b, x, y, z, x + 4, y + 3, z + 3)[:4]}")
            if any(b.naam(xx, y - 1, zz) is None for xx in range(x, x + 5) for zz in range(z, z + 4)):
                p.append(f"koolkraam_{i}: geen vloer onder de kraam")
        (x0, y0, z0), (x1, y1, z1) = b.plekken["vlucht_schacht"]["pos"], b.plekken["vlucht_schacht"]["tot"]
        dicht = [c for c in vrij(b, x0, y0, z0, x1, y1, z1) if b.naam(*c) != "minecraft:water"]
        if x1 - x0 + 1 < 9 or z1 - z0 + 1 < 7 or dicht:
            p.append(f"vlucht_gat: de schacht is geen 9 x 7 vrij: {dicht[:4]}")
        punten = b.plekken["monorail_route"]["punten"]
        for (ax, ay, az), (bx, by, bz) in zip(punten, punten[1:]):
            gat = [x for x in range(ax, bx + 1) if b.naam(x, ay - 1, az) != bouw.SPOOR]
            if gat or ay != by or az != bz:
                p.append(f"monorail_route: de balk ligt er niet overal tussen {(ax, ay, az)} en {(bx, by, bz)}: x {gat[:6]}")
    for id, e in b.plekken.items():
        if e["soort"] == "npc" and b.naam(*e["pos"]) is not None:
            p.append(f"plek {id}: er staat {b.naam(*e['pos'])} op een npc-plek")
    if len(b.s.entities) > 60:
        p.append(f"{len(b.s.entities)} wezens: meer dan 60")
    for naam, laag in b.lagen.items():
        if laag["grootte"] != [5, 4, 4]:
            p.append(f"laag {naam} is geen 5 x 4 x 4")
    return p


def structuur(h):
    b = stad(h)
    problems = controle(b)
    if problems:
        raise SystemExit(f"{NAAM}: self-check found {len(problems)} problem(s):\n  " + "\n  ".join(problems[:60]))
    nx, nz = b.save_tiles()
    kit.leegte_structuur(h, b, titel="Ba Sing Guh", tooltip="De ommuurde stad van de drie ringen, op haar eigen plak klipkaas", y=WERELD_Y,
                         anker=(120, 0, 120))
    print(f"{NAAM}: self-check ok {b.stats}, {nx} x {nz} tiles ({len(b.tiles[2])} with blocks), houses {b.huizen}, safety net {b.vangnet}, world y {WERELD_Y}..{WERELD_Y + bouw.H - 1}")
    return b


def test_kamer(h):
    """einde2_bouw_aarde_test_plein: a little paved floor for the game test of the two overlays."""
    s = h.Structure((9, 6, 8))
    s.fill(0, 0, 0, 8, 0, 7, "guhs:klipkaas_stenen")
    s.set(0, 0, 0, "guhs:guh_kristal_lamp")
    s.save("einde2_bouw_aarde_test_plein")


# =====================================================================================================================
# blocks, loot, recipes, texts
# =====================================================================================================================
def blocks_and_items(h):
    A, w = h.A, h.w
    for name in ("einde2_bouw_aarde_stadssteen", "einde2_bouw_aarde_aardemunt", "einde2_bouw_aarde_groen_kristal"):
        h.simple_block(name)
    for name in ("einde2_bouw_aarde_spoor", "einde2_bouw_aarde_theekist"):
        t = f"guhs:block/{name}"
        w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": t + "_top", "bottom": t + "_top", "side": t}})
        w(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": f"guhs:block/{name}"}}})
        w(f"{A}/models/item/{name}.json", {"parent": f"guhs:block/{name}"})
    # the kool: a round head of 12 x 11 x 12 with its own top
    name = "einde2_bouw_aarde_kool"
    t = f"guhs:block/{name}"
    zij = {"uv": [2, 5, 14, 16], "texture": "#side"}
    w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                        "textures": {"side": t, "top": t + "_top", "particle": t},
                                        "elements": [{"from": [2, 0, 2], "to": [14, 11, 14], "faces": {
                                            "north": zij, "south": zij, "west": zij, "east": zij,
                                            "up": {"uv": [2, 2, 14, 14], "texture": "#top"},
                                            "down": {"uv": [2, 2, 14, 14], "texture": "#top", "cullface": "down"}}}]})
    w(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": f"guhs:block/{name}"}}})
    w(f"{A}/models/item/{name}.json", {"parent": f"guhs:block/{name}"})
    for name in BLOKKEN:
        h.self_drop(name)
    g = lambda names: [f"guhs:{n}" for n in names]  # noqa: E731
    h.add_tag("minecraft/tags/block/mineable/pickaxe", g(["einde2_bouw_aarde_stadssteen", "einde2_bouw_aarde_aardemunt", "einde2_bouw_aarde_spoor",
                                                          "einde2_bouw_aarde_groen_kristal"]))
    h.add_tag("minecraft/tags/block/mineable/axe", g(["einde2_bouw_aarde_theekist", "einde2_bouw_aarde_kool"]))


def recipes(h):
    """Decorative blocks a builder can make: nothing here is needed for anything."""
    h.shaped("einde2_bouw_aarde_stadssteen", ["KK", "KK"], {"K": "guhs:klipkaas_stenen"}, "guhs:einde2_bouw_aarde_stadssteen", 4)
    h.shaped("einde2_bouw_aarde_aardemunt", [" M ", "MGM", " M "], {"M": "guhs:einde2_bouw_aarde_stadssteen", "G": "minecraft:green_dye"},
             "guhs:einde2_bouw_aarde_aardemunt", 4)
    h.shaped("einde2_bouw_aarde_spoor", ["MMM", "III"], {"M": "guhs:einde2_bouw_aarde_stadssteen", "I": "minecraft:iron_nugget"},
             "guhs:einde2_bouw_aarde_spoor", 6)
    h.shapeless("einde2_bouw_aarde_groen_kristal", ["guhs:kristalkaas", "minecraft:green_dye", "minecraft:glass"], "guhs:einde2_bouw_aarde_groen_kristal", 2)
    h.shaped("einde2_bouw_aarde_theekist", ["PPP", "PBP", "PPP"], {"P": "#minecraft:planks", "B": "#minecraft:leaves"}, "guhs:einde2_bouw_aarde_theekist", 4)
    h.shapeless("einde2_bouw_aarde_kool", ["minecraft:moss_block", "minecraft:lime_dye"], "guhs:einde2_bouw_aarde_kool", 2)


def loot(h):
    """Five modest chests (build contract 3.6 E12: nothing a quest needs)."""
    D, w, c = h.D, h.w, h.count_fn
    item = lambda name, weight=1, n=None: {"type": "minecraft:item", "name": name, "weight": weight, **({"functions": c(*n)} if n else {})}  # noqa: E731
    rol = lambda lo, hi: {"type": "minecraft:uniform", "min": lo, "max": hi}  # noqa: E731
    tafels = {
        "huis": [{"rolls": rol(2, 3), "entries": [item("guhs:kaas_knabbels", 5, (2, 5)), item("minecraft:bread", 3, (1, 2)), item("minecraft:white_tulip", 2),
                                                 item("guhs:einde2_bouw_aarde_kool", 2, (1, 2)), item("minecraft:paper", 2, (1, 3))]}],
        "theehuis": [{"rolls": 1, "entries": [item("guhs:theepotje")]},
                     {"rolls": rol(2, 3), "entries": [item("guhs:kaas_knabbels", 4, (3, 6)), item("minecraft:sugar", 3, (2, 4)),
                                                      item("minecraft:white_tulip", 3, (1, 3)), item("guhs:einde2_bouw_aarde_theekist", 2)]}],
        "paleis": [{"rolls": 1, "entries": [item("minecraft:gold_nugget", 1, (3, 7))]},
                   {"rolls": rol(2, 3), "entries": [item("guhs:kaas_knabbels", 4, (4, 8)), item("guhs:kristalkaas_brokje", 2, (1, 3)),
                                                    item("minecraft:honeycomb", 2, (1, 2)), item("guhs:einde2_bouw_aarde_aardemunt", 2, (1, 2))]}],
        "basis": [{"rolls": rol(2, 3), "entries": [item("guhs:kaas_knabbels", 4, (2, 6)), item("minecraft:green_dye", 3, (1, 3)),
                                                   item("guhs:lampion_mint", 2), item("guhs:einde2_bouw_aarde_theekist", 2), item("minecraft:chain", 1, (1, 2))]}],
        "catacomben": [{"rolls": 1, "entries": [item("guhs:einde2_bouw_aarde_groen_kristal", 1, (1, 3))]},
                       {"rolls": rol(1, 2), "entries": [item("guhs:kristalkaas_brokje", 3, (1, 3)), item("guhs:kaas_knabbels", 3, (2, 5)),
                                                        item("minecraft:clay_ball", 2, (1, 4))]}],
    }
    for naam, pools in tafels.items():
        w(f"{D}/loot_table/chests/einde2_bouw_aarde_{naam}.json", {"type": "minecraft:chest", "pools": pools})
    return list(tafels)


def texts(h):
    for key, text in TEXTS.items():
        kit.lang(h, key, text)


def selfcheck(h, b):
    import os
    missing = [k for k in TEXTS if k not in h.NL]
    for name in BLOKKEN:
        for path in (f"{h.A}/blockstates/{name}.json", f"{h.A}/models/block/{name}.json", f"{h.A}/models/item/{name}.json",
                     f"{h.A}/textures/block/{name}.png", f"{h.D}/loot_table/blocks/{name}.json", f"{h.D}/recipe/{name}.json"):
            if not os.path.exists(path):
                missing.append(path)
    for path in [f"{h.D}/worldgen/structure/{NAAM}.json", f"{h.D}/worldgen/structure_set/{NAAM}_gegarandeerd.json", f"{h.D}/einde2/plekken/{NAAM}.json",
                 f"{h.D}/structure/{NAAM}/laag_koolkraam_heel.nbt", f"{h.D}/structure/{NAAM}/laag_koolkraam_kapot.nbt",
                 f"{h.D}/structure/einde2_bouw_aarde_test_plein.nbt"] + [f"{h.D}/structure/{NAAM}/stuk_{i}_{j}.nbt" for i, j in b.tiles[2]]:
        if not os.path.exists(path):
            missing.append(path)
    if os.path.exists(f"{h.D}/worldgen/structure_set/{NAAM}.json"):
        missing.append("ba_sing_guh has a random spread set: it exists once per world")
    if missing:
        raise SystemExit(f"einde2_bouw_aarde: {missing}")


def build(h):
    tex.textures(h)
    blocks_and_items(h)
    recipes(h)
    loot(h)
    texts(h)
    b = structuur(h)
    test_kamer(h)
    selfcheck(h, b)
