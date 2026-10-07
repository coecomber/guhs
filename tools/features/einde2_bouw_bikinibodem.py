"""
einde2 (the Guheinde expansion), slice bouw_bikinibodem: the build of Bikinibodem onder de leegte (the town under its
soap-bubble dome, deep between the islands), where the Sponsguh story of a later phase plays. BUILD only: no story logic and
no characters; where the design puts one, the build leaves the spot free and records a marker (build contract 6.6, 6.7).
Java: nl.juiced.guhs.feature.einde2.bouwbikinibodem (E2BouwBikinibodemFeature).

  - the build itself: einde2_bouw_bikinibodem_bouw.py (the dish, the dome, the lift, the roads, the fields) and
    einde2_bouw_bikinibodem_stad.py (the houses), put together by maak() below and saved in tiles as the guhs:leegte_bouw
    structure "bikinibodem" (deep band, one guaranteed copy, Superkompas tab "einde"), with its sidecar of markers and the
    overlay laag_vliegende_guhlander;
  - the three blocks of the town (zeepbel, bubbelstroom, bloemwolk) and eight decorative ones: models, textures
    (einde2_bouw_bikinibodem_tex.py), loot, tags, texts;
  - the modest loot of its chests, the FTB quest, the test room of the lift. The wiki texts: einde2_bouw_bikinibodem_wiki.py.

build(h) makes everything (h = make_v2).
"""
import json
import os

import features.einde2_kit as kit
from features import einde2_bouw_bikinibodem_bouw as bouw
from features import einde2_bouw_bikinibodem_stad as stad
from features import einde2_bouw_bikinibodem_tex as tex

FTB_CHAPTER = "guhs_guheinde"
FTB_SECTION = "Bikinibodem onder de leegte"

P = "einde2_bouw_bikinibodem_"
SIER = ["koraal", "pylon", "zelfportret", "klarinet", "kluis", "kassa", "camera", "archiefkast"]     # each is guhs:einde2_bouw_bikinibodem_<name>
ALL_BLOCKS = ["zeepbel", "bubbelstroom", "bloemwolk"] + [P + s for s in SIER]
ZONDER_BUIT = ["zeepbel", "bubbelstroom"]                                                            # (no drops: they are the town itself)
BUIT = ["keuken", "huis", "lab"]                                                                     # chests/einde2_bouw_bikinibodem_<name>

TEXTS = {
    "block.guhs.zeepbel": "Zeepbel",
    "block.guhs.bubbelstroom": "Bubbelstroom",
    "block.guhs.bloemwolk": "Bloemwolk",
    f"block.guhs.{P}koraal": "Gloeikoraal",
    f"block.guhs.{P}pylon": "Pylon",
    f"block.guhs.{P}zelfportret": "Zelfportret van Octo Tentakel",
    f"block.guhs.{P}klarinet": "Klarinet op standaard",
    f"block.guhs.{P}kluis": "Kluis",
    f"block.guhs.{P}kassa": "Kassa",
    f"block.guhs.{P}camera": "Bewakingscamera",
    f"block.guhs.{P}archiefkast": "Archiefkast",
    # --- what the lift says in the action bar, the dev command ---
    "gui.guhs.einde2_bouw_bikinibodem.omlaag": "Blub... je zakt naar Bikinibodem, njeg",
    "gui.guhs.einde2_bouw_bikinibodem.omhoog": "Blub... je stijgt naar het eilandje",
    "gui.guhs.einde2_bouw_bikinibodem.mist": "Paarse leegtemist. Het is geen water: gewoon doorademen, njeg",
}

# every marker of build contract 6.7 for this structure (the standard ones are added by the kit's check)
VERWACHT = [("rustpunt", 3), "bubbel_boven", "bubbel_route", "bubbel_onder", "bubbel_omhoog", "koepel",
            "plein", ("weg", 3), "stadspoort", "weg_naar_schelpenstad", ("bewoner", 10),
            "krokante_knabbel", "krabguh", "kantoor", "kluis", "kassa", "octo_kassa", "grill", "grill_speler", "sponsguh_werk", ("besteltafel", 3),
            ("klant", 4), "parel", "mobiel_start",
            "sponsguh_huis", "sponsguh", "sponsguh_bed", "gerrit", "patguh_rots", "patguh", "octo_huis", "octo", "klarinet_standaard",
            "vetemmer", "vetemmer_ingang", "lab", "plankmika", "karen", "plan_z_kast", "plan_z_plankmika", "sluip_start", "sluip_route", ("camera", 4),
            ("laser", 4), "receptkluis",
            "bootschool", "pufguh", "examen_start", "examen_route", ("pylon", 12), "examen_brug", "examen_finish",
            ("kwallenveld", 3), "kwallen_patguh", "kwallen_start",
            "bellenkom", "podium", "podium_octo", "podium_sponsguh", "podium_trommel", ("band", 6), "tribune", "octoliam", ("rook", 4), ("licht", 4),
            "ijssalon", "ijs_toonbank", "ijsverkoper", "ijs_sponsguh", "ijs_patguh", "ijs_speler",
            "boomkoepel", "koepel_sluis", "sandy", "karate_mat", "raket", ("raket_onderdeel", 3),
            "rusthuis", "zeemeerman", "mosselguh", "onzichtbare_boot", "riem",
            "guhlander_anker", "guhlander_dek", "spookkapitein", "guhlander_trap",
            "neptuguh_troon", "neptuguh", "bevroren_krabguh", "hasselguh_landing",
            "knipoog_guhdalf_brug", "knipoog_guhdalf_omweg", "knipoog_guhdalf_camera", "knipoog_kloon_professor", "knipoog_kloon_camera",
            "knipoog_rookguh_zweef", "knipoog_rookguh_patguh", "knipoog_rookguh_camera", "knipoog_ijs_balto", "knipoog_ijs_coupe",
            "knipoog_ijs_kistje", "knipoog_ijs_camera"]
# free room a marker asks for (build contract 6.7): id -> (x, y, z) of the box around it (x and z centred on the spot)
VRIJ = {"parel": (5, 5, 5), "mobiel_start": (7, 4, 5), "neptuguh_troon": (7, 7, 7), "examen_start": (5, 4, 7), "hasselguh_landing": (13, 6, 9),
        "knipoog_ijs_coupe": (1, 4, 1)}


# =====================================================================================================================
# the build
# =====================================================================================================================
def maak(h):
    """Builds the whole town (nothing is saved but the overlay): returns (the Stad, the problems its self-check found)."""
    b = bouw.Stad(h)
    bouw.schotel(b)
    bouw.koepel(b)
    bouw.wegen(b)
    bouw.baan(b)
    for huis in (stad.krokante_knabbel, stad.vetemmer, stad.ijssalon, stad.bootschool, stad.bellenkom, stad.sponsguh_huis, stad.octo_huis,
                 stad.patguh_rots, stad.boomkoepel, stad.rusthuis, stad.huisjes):
        huis(b)
    bouw.eiland(b)
    bouw.lift(b)
    bouw.poort(b)
    stad.guhlander(b, h)
    stad.raket_onderdelen(b)
    bouw.velden(b)
    bouw.stoep(b)
    bouw.straatlampen(b)
    bouw.begroeiing(b)
    wolken = bouw.bloemwolken(b)
    b.connect()
    start = (bouw.CX, bouw.TI + 1, bouw.CZ)
    b.koraal = bouw.verlicht(b, start)
    problemen = b.check(start, VERWACHT, zwevend_ok=lambda c: c in wolken)
    problemen += kit.check_naad(b, "oost")
    problemen += eigen_check(b, h)
    return b, problemen


def eigen_check(b, h):
    """What the kit's check does not look at: the room the markers ask for, the fields, the dome, the roads, the overlay."""
    p = []
    blocks = b.s.blocks
    for id, (sx, sy, sz) in VRIJ.items():
        x, y, z = b.plekken[id]["pos"]
        y += 1 if id == "knipoog_ijs_coupe" else 0
        vol = [c for c in ((x + dx, y + dy, z + dz) for dx in range(-(sx // 2), sx // 2 + 1) for dy in range(sy) for dz in range(-(sz // 2), sz // 2 + 1))
               if c in blocks and b.solid_at(c)]
        if vol:
            p.append(f"plek {id}: niet {sx} x {sy} x {sz} vrij, bv. {vol[:3]}")
    for id, e in b.plekken.items():
        if id.startswith("kwallenveld_"):
            (x0, y0, z0), (x1, y1, z1) = e["pos"], e["tot"]
            if x1 - x0 + 1 < 24 or z1 - z0 + 1 < 24 or y1 - y0 + 1 < 10:
                p.append(f"{id} is kleiner dan 24 x 10 x 24")
            dicht = sum(1 for x in range(x0, x1 + 1) for z in range(z0, z1 + 1) for y in range(b.grond[(x, z)] + 3, y1 + 1) if (x, y, z) in blocks)
            if dicht:
                p.append(f"{id}: {dicht} blokken in de lucht boven het veld")
        if id.startswith("laser_"):
            (x0, _, z0), (x1, _, z1) = e["pos"], e["tot"]
            if x0 != x1 and z0 != z1:
                p.append(f"{id} is dikker dan een blok")
    # the dome is closed: from the plein you cannot fly out except through the gate (the lift is no hole: it is full of stream)
    start = (bouw.CX + 12, bouw.V + 3, bouw.CZ)
    gezien, todo, lek = {start}, [start], None
    while todo and lek is None:
        x, y, z = todo.pop()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if n in gezien or n in blocks and blocks[n][0] not in ("guhs:korstgras", "guhs:schimmelpaddenstoel", "guhs:dorre_korststruik"):
                continue
            if n[0] >= 204 and bouw.CZ - 3 <= n[2] <= bouw.CZ + 3 and n[1] <= bouw.V + 5:
                continue                         # (the gate)
            if not b.inside(*n) or bouw.straal(n[0], n[2]) > bouw.RK + 2:
                lek = n
                break
            gezien.add(n)
            todo.append(n)
    if lek:
        p.append(f"de zeepbel is lek: lucht van het plein komt buiten bij {lek}")
    # the three roads: at least 5 wide and 5 high free, no step
    for id in ("weg_1", "weg_2", "weg_3"):
        punten = b.plekken[id]["punten"]
        for (ax, ay, az), (bx, by, bz) in zip(punten, punten[1:]):
            n = max(abs(bx - ax), abs(bz - az))
            for i in range(n + 1):
                x, z = ax + (bx - ax) * i // n, az + (bz - az) * i // n
                langs_x = abs(bx - ax) >= abs(bz - az)
                for d in range(-2, 3):
                    cx, cz = (x, z + d) if langs_x else (x + d, z)
                    if not b.solid_at((cx, bouw.G, cz)) or any((cx, y, cz) in blocks and b.solid_at((cx, y, cz)) for y in range(bouw.V, bouw.V + 5)):
                        p.append(f"{id}: niet 5 breed en 5 hoog vrij bij {(cx, bouw.V, cz)}")
                        break
    # the overlay must not lie where the build has blocks (taking it away again would leave holes)
    laag = b.lagen["vliegende_guhlander"]
    ox, oy, oz = laag["pos"]
    botst = [c for c in ((ox + x, oy + y, oz + z) for (x, y, z) in stad.guhlander_schip(h).blocks) if c in blocks]
    if botst:
        p.append(f"de laag vliegende_guhlander ligt in {len(botst)} blokken van de bouw, bv. {botst[:3]}")
    if max(c[1] for c in blocks) > bouw.TI + 6:
        p.append("iets steekt boven het eilandje uit")
    if min(c[1] for c in blocks) < 1:
        p.append("de schotel komt lager dan wereld-y 5")
    return p


def structuur(h):
    b, problemen = maak(h)
    if problemen:
        raise SystemExit(f"bikinibodem: self-check found {len(problemen)} problem(s):\n  " + "\n  ".join(problemen[:40]))
    nx, nz = b.save_tiles()
    kit.leegte_structuur(h, b, titel="Bikinibodem onder de leegte",
                         tooltip="Een stadje onder een reusachtige zeepbel, diep tussen de eilanden. Neem de bubbelstroom, njeg",
                         y=bouw.Y_WERELD, anker=bouw.ANKER)
    print(f"bikinibodem: self-check ok {b.stats}, {nx} x {nz} tiles, {b.koraal} lumps of gloeikoraal for light")
    return b


def testkamer(h):
    """The room of E2BouwBikinibodemGameTests: a floor, a deck ten blocks above it, and through the deck two little lifts
    built exactly like the town's (bouw.buis, one stream wide here): one going down (west), one going up (east)."""
    s = h.Structure((13, 14, 7))
    s.fill(0, 0, 0, 12, 0, 6, bouw.KSTEEN)
    s.fill(0, 10, 0, 12, 10, 6, bouw.KSTEEN)
    for (cx, richting, open_onder, open_boven) in ((3, "omlaag", "east", "east"), (9, "omhoog", "west", "west")):
        for y in range(1, 13):
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    c = (cx + dx, y, 3 + dz)
                    if (dx, dz) == (0, 0):
                        s.set(*c, bouw.STROOM, {"richting": richting})
                    elif dx and dz:
                        if y <= 10:
                            s.set(*c, bouw.KSTEEN)
                    else:
                        kant = "north" if dz == -1 else "south" if dz == 1 else "west" if dx == -1 else "east"
                        if kant == open_onder and y <= 2 or y > 10 and kant == open_boven or y > 11:
                            s.blocks.pop(c, None)
                        else:
                            s.set(*c, bouw.BEL)
    s.set(6, 0, 3, bouw.KORAAL)
    s.set(6, 10, 3, bouw.KORAAL)
    s.save("einde2_bouw_bikinibodem_test_lift")


# =====================================================================================================================
# blocks: models, loot, tags
# =====================================================================================================================
def doos(van, tot, uv, noord=None):
    """A box of a block model with the same piece of the texture on every side (noord: another piece on its north face)."""
    faces = {f: {"uv": list(uv), "texture": "#t"} for f in ("north", "south", "east", "west", "up", "down")}
    if noord:
        faces["north"] = {"uv": list(noord), "texture": "#t"}
    return {"from": list(van), "to": list(tot), "faces": faces}


def blocks_and_items(h):
    A, w = h.A, h.w
    h.simple_block("zeepbel", render_type="minecraft:translucent")
    for richting in ("omhoog", "omlaag"):
        w(f"{A}/models/block/bubbelstroom_{richting}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:translucent",
                                                             "textures": {"cross": f"guhs:block/bubbelstroom_{richting}"}})
    w(f"{A}/blockstates/bubbelstroom.json", {"variants": {f"richting={r}": {"model": f"guhs:block/bubbelstroom_{r}"} for r in ("omhoog", "omlaag")}})
    h.item_model("bubbelstroom", "guhs:block/bubbelstroom_omhoog")
    for i in range(4):
        w(f"{A}/models/block/bloemwolk_{i}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"guhs:block/bloemwolk_{i}"}})
    w(f"{A}/blockstates/bloemwolk.json", {"variants": {f"kleur={i}": {"model": f"guhs:block/bloemwolk_{i}"} for i in range(4)}})
    w(f"{A}/models/item/bloemwolk.json", {"parent": "guhs:block/bloemwolk_0"})
    h.simple_block(f"{P}koraal")

    def model(name, elements, tex_name=None, extra=None):
        t = f"guhs:block/{tex_name or name}"
        w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/block", "textures": {"particle": t, "t": t, **(extra or {})}, "elements": elements})

    # the pylon: a cone of stacked boxes, orange and white
    oranje, wit, voet = (1, 1, 7, 7), (9, 1, 15, 7), (1, 9, 7, 15)
    model(f"{P}pylon", [doos((2, 0, 2), (14, 1, 14), voet), doos((4, 1, 4), (12, 4, 12), oranje), doos((5, 4, 5), (11, 7, 11), wit),
                        doos((6, 7, 6), (10, 10, 10), oranje), doos((7, 10, 7), (9, 13, 9), wit), doos((7.5, 13, 7.5), (8.5, 14, 8.5), oranje)])
    w(f"{A}/blockstates/{P}pylon.json", {"variants": {"": {"model": f"guhs:block/{P}pylon"}}})
    # the self-portraits: a thin panel against the wall behind it, four pictures
    for i in range(4):
        model(f"{P}zelfportret_{i}", [doos((0, 0, 15), (16, 16, 16), (0, 0, 1, 1), noord=(0, 0, 16, 16))])
    w(f"{A}/blockstates/{P}zelfportret.json", {"variants": {k: v for i in range(4)
                                                            for k, v in h.facing_states(f"{P}zelfportret_{i}", extra=f",soort={i}").items()}})
    w(f"{A}/models/item/{P}zelfportret.json", {"parent": f"guhs:block/{P}zelfportret_0"})
    # the clarinet on its stand
    h_ = {"uv": [9, 0, 15, 4], "texture": "#t"}
    model(f"{P}klarinet", [{"from": [5, 0, 5], "to": [11, 1, 11], "faces": {f: h_ for f in ("north", "south", "east", "west", "up", "down")}},
                           doos((6, 1, 6), (10, 3, 10), (0, 12, 4, 14)), doos((7, 3, 7), (9, 13, 9), (0, 2, 2, 12)),
                           doos((7.5, 13, 7.5), (8.5, 15, 8.5), (4, 0, 5, 2))])
    # the till (on a counter) and the camera (under a ceiling)
    model(f"{P}kassa", [doos((2, 0, 4), (14, 6, 13), (1, 1, 7, 7), noord=(0, 8, 8, 16)), doos((4, 6, 9), (12, 11, 12), (8, 0, 16, 8)),
                        doos((5, 6, 4), (11, 7, 8), (8, 8, 16, 12))])
    model(f"{P}camera", [doos((7, 12, 7), (9, 16, 9), (0, 8, 4, 12)), doos((5, 8, 4), (11, 12, 13), (5, 1, 15, 7)),
                         doos((6.5, 9, 2), (9.5, 11, 4), (0, 0, 4, 4))])
    for name in ("klarinet", "kassa", "camera"):
        w(f"{A}/blockstates/{P}{name}.json", {"variants": h.facing_states(f"{P}{name}")})
    # the safe and the filing cabinet: a steel box with a front
    for name in ("kluis", "archiefkast"):
        zij = f"guhs:block/{P}{name}"
        w(f"{A}/models/block/{P}{name}.json", {"parent": "minecraft:block/orientable", "textures": {"top": zij, "front": f"{zij}_voor", "side": zij}})
        w(f"{A}/blockstates/{P}{name}.json", {"variants": h.facing_states(f"{P}{name}")})
    for name in ("pylon", "klarinet", "kassa", "camera", "kluis", "archiefkast"):
        w(f"{A}/models/item/{P}{name}.json", {"parent": f"guhs:block/{P}{name}"})
    for b in ALL_BLOCKS:
        if b not in ZONDER_BUIT:
            h.self_drop(b)
    g = lambda names: [f"guhs:{n}" for n in names]  # noqa: E731
    h.add_tag("minecraft/tags/block/mineable/pickaxe", g([P + n for n in ("koraal", "pylon", "kluis", "kassa", "camera", "archiefkast")]))
    h.add_tag("minecraft/tags/block/mineable/axe", g([P + "zelfportret", P + "klarinet"]))
    h.add_tag("minecraft/tags/block/mineable/hoe", g(["bloemwolk"]))
    h.add_tag("minecraft/tags/block/impermeable", g(["zeepbel"]))


def loot(h):
    """The chests of the town: modest, and nothing a quest needs."""
    def item(name, gewicht, lo=1, hi=1):
        e = {"type": "minecraft:item", "name": name, "weight": gewicht}
        if hi > 1:
            e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
        return e
    tabellen = {
        "keuken": [item("minecraft:bread", 6, 1, 3), item("guhs:kaas_knabbels", 6, 2, 5), item("minecraft:baked_potato", 3, 1, 3),
                   item("minecraft:bowl", 2), item("minecraft:dried_kelp", 4, 2, 6)],
        "huis": [item("guhs:kaas_knabbels", 6, 1, 4), item("minecraft:dried_kelp", 4, 1, 4), item("minecraft:paper", 3, 1, 3),
                 item("minecraft:book", 2), item("guhs:knabbelkoorvrucht", 2, 1, 2), item("minecraft:nautilus_shell", 1)],
        "lab": [item("minecraft:redstone", 6, 1, 4), item("minecraft:glass_bottle", 4, 1, 2), item("minecraft:paper", 4, 1, 3),
                item("guhs:kaas_knabbels", 3, 1, 3), item("minecraft:iron_nugget", 4, 2, 6)],
    }
    for name, entries in tabellen.items():
        h.w(f"{h.D}/loot_table/chests/{P}{name}.json", {"type": "minecraft:chest", "pools": [
            {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": entries}]})


def opruimen(h):
    """What the stand-ins of the skeleton left behind and the real blocks do not have: their flat textures and cube models,
    and the loot of the two blocks that drop nothing."""
    for f in [f"{h.A}/textures/block/bubbelstroom.png", f"{h.A}/textures/block/bloemwolk.png", f"{h.A}/models/block/bubbelstroom.json",
              f"{h.A}/models/block/bloemwolk.json"] + [f"{h.D}/loot_table/blocks/{b}.json" for b in ZONDER_BUIT]:
        if os.path.exists(f):
            os.remove(f)


def texts(h):
    for k, v in TEXTS.items():
        kit.lang(h, k, v)


def selfcheck(h):
    A, D = h.A, h.D
    missing = []
    for b in ALL_BLOCKS:
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
        if not os.path.exists(f"{A}/models/item/{b}.json"):
            missing.append(f"item model {b}")
        if (b not in ZONDER_BUIT) != os.path.exists(f"{D}/loot_table/blocks/{b}.json"):
            missing.append(f"loot {b}")
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for n in tex.BLOCKS:
        if not os.path.exists(os.path.join(h.TEX, "block", f"{n}.png")):
            missing.append(f"texture {n}")
    for f in sorted(os.listdir(f"{A}/models/block")):
        if f.startswith((P, "zeepbel", "bubbelstroom", "bloemwolk")):
            for t in json.load(open(f"{A}/models/block/{f}", encoding="utf-8")).get("textures", {}).values():
                if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                    missing.append(f"texture {t} ({f})")
    for name in BUIT:
        if not os.path.exists(f"{D}/loot_table/chests/{P}{name}.json"):
            missing.append(f"loot table {name}")
    for f in ("worldgen/structure/bikinibodem.json", "worldgen/structure_set/bikinibodem_gegarandeerd.json", "einde2/plekken/bikinibodem.json",
              "structure/bikinibodem/stuk_0_0.nbt", "structure/bikinibodem/stuk_6_6.nbt", "structure/bikinibodem/laag_vliegende_guhlander.nbt",
              "structure/einde2_bouw_bikinibodem_test_lift.nbt"):
        if not os.path.exists(f"{D}/{f}"):
            missing.append(f)
    if os.path.exists(f"{D}/worldgen/structure_set/bikinibodem.json"):
        missing.append("bikinibodem has a random set: it exists exactly once")
    st = json.load(open(f"{D}/worldgen/structure/bikinibodem.json", encoding="utf-8"))
    if st.get("leeg") != [480, 224] or not st.get("ruim") or st["size"] != [224, bouw.H, 224] or st["anchor"] != list(bouw.ANKER) or st["y"] != 4:
        missing.append(f"structure json: {st}")
    if missing:
        raise SystemExit(f"einde2_bouw_bikinibodem: missing: {missing}")


def build(h):
    tex.textures(h)
    blocks_and_items(h)
    opruimen(h)
    loot(h)
    texts(h)
    testkamer(h)
    structuur(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests: a section of its own at the end of the Guheinde chapter (find it; the story is a later phase)
# =====================================================================================================================
def ftb(fq):
    fq.q("einde2_bouw_bikinibodem_vind", "Bikinibodem onder de leegte", "Diep tussen de buiteneilanden van het Guheinde ligt een schotel van "
         "kaaskorst onder een reusachtige &dzeepbel&r: &6Bikinibodem&r. Het Superkompas (tabblad einde) wijst de weg. Land op het eilandje "
         "erboven en stap in de &bbubbelstroom&r: die zet je netjes op het plein. De paarse mist is geen water, dus adem maar gewoon door. "
         "Wie wonen er? Dat merk je vanzelf. Iemand lacht er heel hard, njeg.", "guhs:zeepbel", [fq.structure("bikinibodem")],
         rewards=(("guhs:kaas_knabbels", 12),), shape="circle")
