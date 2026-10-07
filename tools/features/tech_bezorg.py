"""
bbq2 (tech-bezorg): the Bezorgguhtje, its Stepstation, the Haltepaaltjes and the whistle. Java: feature/techbezorg.

A Bezorgguhtje is a mini-guh on a step with a far too big backpack. It lives in a Stepstation (a guh machine: it needs
vadskracht) and rides its round along the Haltepaaltjes of that station: at an "ophalen" stop it takes things out of the
chest or machine the pole stands against, at an "afleveren" stop it puts them in; each stop has a filter. This module makes:
  - block stepstation: a little guh-faced depot with a mint roof, ears, and a sign with a step on it; the face is the
    standard machine face (asleep without vadskracht, awake with it, surprised when the backpack does not get empty) and
    the roller door under it shows the same (shut / a step parked inside / stuffed with parcels). Blockstate facing x snoet
  - block haltepaaltje: a bus stop pole made of a grillspies with a guh-eared sign: green with an arrow up (ophalen) or
    orange with an arrow down (afleveren), and a little arm with a tray towards the chest it serves. Blockstate
    facing (down, north, south, west, east: where the chest is) x ophalen
  - item bezorgguhtje_fluitje (the reward of the Rookguh-vuurtoren, slice toren-peper: no recipe), the spawn egg
  - the Bezorgguhtje itself: tech_bezorg_modellen.py (GeckoLib model, animations, two textures)
  - recipes (tier Saus: both hold a grillspies), loot tables, tags, sounds, texts, the Guhdex page, advancements
    (visible techniek/tech_bezorg_*, hidden quest/tech_bezorg_* for the FTB chapter that tech-quests writes), the test room
No ftb(fq): the chapter Guh-technologie is written by tech_quests only (CONTRACT_130 8). Wiki: tech_bezorg_wiki.py.
"""
import os
import re

import numpy as np
from PIL import Image

from features import bbq2, vadskracht
from features import tech_bezorg_modellen as modellen

ROT = {"north": 0, "east": 90, "south": 180, "west": 270}

# --- colours ------------------------------------------------------------------------------------------------------------------
LILA = (205, 170, 214)              # the station's body: guh fur
LILA_DONKER = (148, 112, 160)
MINT = (96, 206, 200)               # its roof, like the step
MINT_DONKER = (52, 150, 150)
GEEL = (250, 200, 62)               # the roller door and the sign: the thermal bag's cheese yellow
GEEL_DONKER = (214, 150, 44)
BINNEN = (52, 26, 42)
ROZE = (240, 120, 170)
PAKJE = (190, 140, 96)
LINT = (240, 110, 160)
STAAL = (196, 198, 206)             # the grillspies pole
STAAL_DONKER = (128, 130, 142)
GROEN = (112, 200, 124)             # ophalen
GROEN_DONKER = (58, 140, 76)
ORANJE = (246, 160, 70)             # afleveren
ORANJE_DONKER = (190, 104, 34)
WIT = (255, 252, 246)
INKT = (58, 28, 60)

_JAVA = None


def getal(naam):
    """A number of Bezorgnet.java (BEREIK, MAX_HALTES, RUGZAK): the texts never type one themselves."""
    global _JAVA
    if _JAVA is None:
        _JAVA = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "techbezorg", "Bezorgnet.java"),
                     encoding="utf-8").read()
    m = re.search(rf"\b{naam}\s*=\s*(\d+)", _JAVA)
    if not m:
        raise SystemExit(f"tech_bezorg: Bezorgnet.java has no number {naam}")
    return int(m.group(1))


# =====================================================================================================================
# textures
# =====================================================================================================================
def _ruis(basis, var, seed, size=16):
    rng = np.random.default_rng(seed)
    a = np.zeros((size, size, 4), np.uint8)
    n = rng.normal(0, var, (size, size))
    for c in range(3):
        a[..., c] = np.clip(basis[c] + n, 0, 255)
    a[..., 3] = 255
    return a


def _station_voor(staat):
    """The front of the body (rows 4..15: the body is 12 high): the face, and under it the roller door."""
    a = _ruis(LILA, 5, 650_1)
    a[4, :, :3] = LILA_DONKER
    a[15, :, :3] = LILA_DONKER
    a[4:, 0, :3] = LILA_DONKER
    a[4:, 15, :3] = LILA_DONKER
    # the door frame (x 3..12, rows 9..15) and what you see in it
    a[9, 3:13, :3] = LILA_DONKER
    a[9:16, 3, :3] = LILA_DONKER
    a[9:16, 12, :3] = LILA_DONKER
    if staat == "slaapt":                                 # shut: yellow slats
        for y in range(10, 16):
            a[y, 4:12, :3] = GEEL if y % 2 == 0 else GEEL_DONKER
        a[13, 7:9, :3] = LILA_DONKER                      # the handle
    else:
        a[10:16, 4:12, :3] = BINNEN
        a[10, 4:12, :3] = GEEL_DONKER                     # the rolled-up door
        if staat == "werkt":                              # a step parked inside: mint deck, pink wheels, the stem
            a[14, 5:11, :3] = MINT
            a[15, 5, :3] = ROZE
            a[15, 10, :3] = ROZE
            a[11:14, 5, :3] = MINT_DONKER
            a[11, 4:7, :3] = ROZE
        else:                                             # stuffed with parcels
            a[11:16, 4:8, :3] = PAKJE
            a[12:16, 8:12, :3] = (170, 120, 80)
            a[13, 4:8, :3] = LINT
            a[11:16, 6, :3] = LINT
            a[14, 8:12, :3] = LINT
            a[12:16, 10, :3] = LINT
    img = Image.fromarray(a)
    return vadskracht.snoet(img, 2, 5, staat)


def _station_zij():
    a = _ruis(LILA, 6, 650_2)
    a[4, :, :3] = LILA_DONKER
    a[15, :, :3] = LILA_DONKER
    a[4:, 0, :3] = LILA_DONKER
    a[4:, 15, :3] = LILA_DONKER
    a[12:14, 1:15, :3] = MINT                              # a mint band
    # the charging socket for the step: a yellow plate with two holes and a little smile (a snoet of its own)
    a[6:11, 5:11, :3] = GEEL
    a[6, 5:11, :3] = GEEL_DONKER
    a[10, 5:11, :3] = GEEL_DONKER
    a[6:11, 5, :3] = GEEL_DONKER
    a[6:11, 10, :3] = GEEL_DONKER
    a[7, 6, :3] = INKT
    a[7, 9, :3] = INKT
    a[9, 7:9, :3] = INKT
    return Image.fromarray(a)


def _station_dak():
    """The roof (top view) and, in its top two rows, the edge of the roof slab."""
    a = _ruis(MINT, 5, 650_3)
    a[0, :, :3] = MINT_DONKER
    a[15, :, :3] = MINT_DONKER
    a[:, 0, :3] = MINT_DONKER
    a[:, 15, :3] = MINT_DONKER
    a[2, 2:14, :3] = MINT_DONKER                          # the raised middle
    a[13, 2:14, :3] = MINT_DONKER
    a[2:14, 2, :3] = MINT_DONKER
    a[2:14, 13, :3] = MINT_DONKER
    return Image.fromarray(a)


def _station_bord():
    """One sheet for the small parts: x 0..11, y 0..5 the sign (a step), x 12..15, y 0..3 an ear, row 7 the sign's edge,
    rows 8..9 its post."""
    a = np.zeros((16, 16, 4), np.uint8)
    a[..., 3] = 255
    a[..., :3] = GEEL
    a[0, 0:12, :3] = GEEL_DONKER
    a[5, 0:12, :3] = GEEL_DONKER
    a[0:6, 0, :3] = GEEL_DONKER
    a[0:6, 11, :3] = GEEL_DONKER
    # the step: the handlebar and stem, the deck, two pink wheels
    for x, y in ((8, 1), (9, 1), (10, 1), (9, 2), (9, 3), (2, 3), (3, 3), (4, 3), (5, 3), (6, 3), (7, 3), (8, 3)):
        a[y, x, :3] = INKT
    a[4, 2, :3] = ROZE
    a[4, 9, :3] = ROZE
    a[0:4, 12:16, :3] = vadskracht.OOR                    # the ear
    a[1:4, 13:15, :3] = vadskracht.OOR_BINNEN
    a[7, :, :3] = GEEL_DONKER
    a[8:10, :, :3] = STAAL                                # the sign's post
    return Image.fromarray(a)


def _paal():
    a = _ruis(STAAL, 5, 650_5)
    a[:, 0::4, :3] = STAAL_DONKER                         # a twisted spit: dark ridges
    return Image.fromarray(a)


def _halte_bord(ophalen):
    """x 0..7, y 0..6: the sign (a fat arrow: up = it takes things along, down = it brings them); x 8..10, y 0..1: an
    ear; (8, 3): the sign's edge; x 0..3, y 8..10: the little tray at the end of the arm."""
    kleur, donker = (GROEN, GROEN_DONKER) if ophalen else (ORANJE, ORANJE_DONKER)
    a = np.zeros((16, 16, 4), np.uint8)
    a[..., 3] = 255
    a[..., :3] = donker
    a[0:7, 0:8, :3] = kleur
    a[0, 0:8, :3] = donker
    a[6, 0:8, :3] = donker
    a[0:7, 0, :3] = donker
    a[0:7, 7, :3] = donker
    for y in range(1, 6):                                 # the shaft of the arrow
        a[y, 3:5, :3] = WIT
    if ophalen:                                           # ...and its head: up or down
        a[1, 3:5, :3] = WIT
        a[2, 2:6, :3] = WIT
        a[3, 1:7, :3] = WIT
    else:
        a[5, 3:5, :3] = WIT
        a[4, 2:6, :3] = WIT
        a[3, 1:7, :3] = WIT
    a[0:2, 8:11, :3] = vadskracht.OOR
    a[1, 9, :3] = vadskracht.OOR_BINNEN
    a[8:11, 0:4, :3] = kleur
    a[8, 0:4, :3] = donker
    return Image.fromarray(a)


def _halte_icoon():
    """The item: the pole with its green sign, flat (the thin block model is hard to see in a slot)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(8, 15):
        px[7, y] = STAAL + (255,)
        px[8, y] = STAAL_DONKER + (255,)
    for x in range(5, 11):
        px[x, 15] = STAAL_DONKER + (255,)
    for y in range(2, 9):
        for x in range(3, 13):
            px[x, y] = (GROEN_DONKER if x in (3, 12) or y in (2, 8) else GROEN) + (255,)
    for y in range(3, 8):                                 # the arrow
        px[7, y] = WIT + (255,)
        px[8, y] = WIT + (255,)
    for x in range(6, 10):
        px[x, 4] = WIT + (255,)
    for x in range(5, 11):
        px[x, 5] = WIT + (255,)
    for x0 in (3, 10):                                    # the ears
        for x in range(x0, x0 + 3):
            px[x, 1] = vadskracht.OOR + (255,)
        px[x0 + 1, 0] = vadskracht.OOR + (255,)
        px[x0 + 1, 1] = vadskracht.OOR_BINNEN + (255,)
    return img


def _fluitje():
    """A cheese-yellow whistle with guh ears, a pink mouthpiece and a cord."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    rand = (150, 96, 34, 255)
    body = [(y, x) for y in range(6, 13) for x in range(3, 11) if (x - 6.5) ** 2 + (y - 9) ** 2 <= 14]
    for y, x in body:
        px[x, y] = GEEL + (255,)
    for y, x in body:
        if any((y + dy, x + dx) not in body for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            px[x, y] = rand
    for x in range(9, 15):                                # the mouthpiece
        for y in range(6, 9):
            px[x, y] = ROZE + (255,) if 6 < y or x < 14 else (0, 0, 0, 0)
        px[x, 5] = (170, 70, 120, 255)
        px[x, 9] = (170, 70, 120, 255)
    px[14, 6] = px[14, 7] = px[14, 8] = (170, 70, 120, 255)
    for y, x in ((8, 5), (8, 6), (9, 5), (9, 6)):          # the air hole
        px[x, y] = INKT + (255,)
    px[5, 8] = (255, 255, 255, 255)
    for x0 in (3, 7):                                     # two little ears
        px[x0, 5] = px[x0 + 1, 5] = vadskracht.OOR + (255,)
        px[x0, 4] = vadskracht.OOR + (255,)
    for x, y in ((3, 12), (2, 13), (2, 14), (3, 15), (4, 15), (5, 14), (5, 13)):   # the cord
        px[x, y] = (236, 110, 160, 255)
    return img


def textures(h):
    for staat in vadskracht.STATEN:
        h.save(_station_voor(staat), "block", f"stepstation_voor_{staat}.png")
    h.save(_station_zij(), "block", "stepstation_zij.png")
    h.save(_station_dak(), "block", "stepstation_dak.png")
    h.save(_station_bord(), "block", "stepstation_bord.png")
    h.save(_paal(), "block", "haltepaaltje_paal.png")
    h.save(_halte_bord(True), "block", "haltepaaltje_bord_ophalen.png")
    h.save(_halte_bord(False), "block", "haltepaaltje_bord_afleveren.png")
    h.save(_halte_icoon(), "item", "haltepaaltje.png")
    h.save(_fluitje(), "item", "bezorgguhtje_fluitje.png")
    import mc26                                           # (the spawn egg, baked like mc26.spawn_eggs does for the eggs it knows)
    egg = Image.alpha_composite(mc26.tint(mc26.vanilla_121("item/spawn_egg"), 0xC3A0CD),
                                mc26.tint(mc26.vanilla_121("item/spawn_egg_overlay"), 0xFAC83E))
    h.save(egg, "item", "bezorgguhtje_spawn_egg.png")


# =====================================================================================================================
# models, blockstates, loot, recipes, tags
# =====================================================================================================================
def _f(tex, uv, cull=None):
    face = {"texture": tex, "uv": uv}
    if cull:
        face["cullface"] = cull
    return face


def _doos(frm, to, faces):
    return {"from": frm, "to": to, "faces": faces}


def _station_model(staat):
    zij = _f("#zij", [0, 4, 16, 16])
    dakrand = _f("#dak", [0, 0, 16, 2])
    bord = _f("#bord", [0, 0, 12, 6])
    rand = _f("#bord", [0, 7, 1, 8])
    oor = _f("#bord", [12, 0, 16, 4])
    post = _f("#bord", [0, 8, 1, 10])
    elements = [
        # the body: 12 high, the face and the roller door on the front
        _doos([0, 0, 0], [16, 12, 16], {"north": _f("#voor", [0, 4, 16, 16], "north"), "south": dict(zij, cullface="south"),
                                         "west": dict(zij, cullface="west"), "east": dict(zij, cullface="east"),
                                         "down": _f("#zij", [0, 4, 16, 16], "down")}),
        # the mint roof: a slab with a raised middle
        _doos([0, 12, 0], [16, 14, 16], {"north": dakrand, "south": dakrand, "west": dakrand, "east": dakrand,
                                          "up": _f("#dak", [0, 0, 16, 16]), "down": _f("#dak", [0, 0, 16, 16])}),
        _doos([2, 14, 2], [14, 15, 14], {"north": _f("#dak", [2, 2, 14, 3]), "south": _f("#dak", [2, 2, 14, 3]),
                                          "west": _f("#dak", [2, 2, 14, 3]), "east": _f("#dak", [2, 2, 14, 3]),
                                          "up": _f("#dak", [2, 2, 14, 14])}),
        # the sign on the roof: a step, so everybody knows who lives here
        _doos([7.5, 15, 8.5], [8.5, 17, 9.5], {f: post for f in ("north", "south", "west", "east")}),
        _doos([2, 17, 8.5], [14, 23, 9.5], {"north": bord, "south": bord, "west": rand, "east": rand, "up": rand, "down": rand}),
    ]
    for x0 in (2, 10):                                    # the ears, at the front edge of the roof
        elements.append(_doos([x0, 14, 0.5], [x0 + 4, 18, 2.5], {f: oor for f in ("north", "south", "west", "east", "up")}))
    return {"parent": "minecraft:block/block",
            "textures": {"particle": "guhs:block/stepstation_zij", "voor": f"guhs:block/stepstation_voor_{staat}",
                         "zij": "guhs:block/stepstation_zij", "dak": "guhs:block/stepstation_dak", "bord": "guhs:block/stepstation_bord"},
            "elements": elements}


def _halte_model(ophalen, neer):
    paal = lambda uv: _f("#paal", uv)
    bord = _f("#bord", [0, 0, 8, 7])
    rand = _f("#bord", [8, 3, 9, 4])
    oor = _f("#bord", [8, 0, 10.5, 2])
    bak = _f("#bord", [0, 8, 4, 11])
    alle = ("north", "south", "west", "east", "up", "down")
    elements = [
        _doos([7.25, 1, 7.25], [8.75, 10, 8.75], {f: paal([0, 0, 1.5, 9]) for f in ("north", "south", "west", "east")}),
        # the guh-eared sign: its two faces look along the street (the arm points at the chest)
        _doos([4, 10, 7.5], [12, 17, 8.5], {"north": bord, "south": bord, "west": rand, "east": rand, "up": rand, "down": rand}),
        _doos([4, 17, 7.5], [6.5, 19, 8.5], {f: oor for f in alle}),
        _doos([9.5, 17, 7.5], [12, 19, 8.5], {f: oor for f in alle}),
    ]
    if neer:                                              # standing on the chest: a wide clamp as its foot
        elements.append(_doos([5, 0, 5], [11, 1, 11], {f: paal([0, 0, 6, 6] if f in ("up", "down") else [0, 0, 6, 1]) for f in alle}))
        elements.append(_doos([6, 0, 4], [10, 0.5, 12], {f: bak for f in alle}))
    else:
        elements.append(_doos([6, 0, 6], [10, 1, 10], {f: paal([0, 0, 4, 4] if f in ("up", "down") else [0, 0, 4, 1]) for f in alle}))
        # the arm towards the chest (north) with a little tray at its end
        elements.append(_doos([7.5, 5, 1], [8.5, 6, 7.25], {f: paal([0, 0, 1, 6.25] if f in ("up", "down") else [0, 0, 6.25, 1])
                                                             for f in ("west", "east", "up", "down")}))
        elements.append(_doos([5.5, 4, 0], [10.5, 7, 1], {f: bak for f in alle}))
    soort = "ophalen" if ophalen else "afleveren"
    return {"parent": "minecraft:block/block",
            "textures": {"particle": "guhs:block/haltepaaltje_paal", "paal": "guhs:block/haltepaaltje_paal",
                         "bord": f"guhs:block/haltepaaltje_bord_{soort}"},
            "elements": elements}


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    # --- the Stepstation: a guh machine with its own model ---
    for staat in vadskracht.STATEN:
        w(f"{A}/models/block/stepstation_{staat}.json", _station_model(staat))
    w(f"{A}/blockstates/stepstation.json", {"variants": {
        f"facing={f},snoet={staat}": {"model": f"guhs:block/stepstation_{staat}", **({"y": r} if r else {})}
        for f, r in ROT.items() for staat in vadskracht.STATEN}})
    w(f"{A}/models/item/stepstation.json", {"parent": "guhs:block/stepstation_werkt"})
    h.self_drop("stepstation")
    vadskracht.toon(h, "stepstation")
    # --- the Haltepaaltje ---
    for ophalen in (True, False):
        soort = "ophalen" if ophalen else "afleveren"
        w(f"{A}/models/block/haltepaaltje_{soort}.json", _halte_model(ophalen, False))
        w(f"{A}/models/block/haltepaaltje_{soort}_neer.json", _halte_model(ophalen, True))
    varianten = {}
    for ophalen in (True, False):
        soort = "ophalen" if ophalen else "afleveren"
        varianten[f"facing=down,ophalen={str(ophalen).lower()}"] = {"model": f"guhs:block/haltepaaltje_{soort}_neer"}
        for f, r in ROT.items():
            varianten[f"facing={f},ophalen={str(ophalen).lower()}"] = {"model": f"guhs:block/haltepaaltje_{soort}", **({"y": r} if r else {})}
    w(f"{A}/blockstates/haltepaaltje.json", {"variants": varianten})
    h.item_model("haltepaaltje")
    h.self_drop("haltepaaltje")
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:stepstation", "guhs:haltepaaltje"])
    # --- items ---
    h.item_model("bezorgguhtje_fluitje")
    h.item_model("bezorgguhtje_spawn_egg")
    w(f"{D}/loot_table/entities/bezorgguhtje.json", {"type": "minecraft:entity", "pools": []})   # (it drops nothing: it is lief)
    # --- recipes (tier Saus: a grillspies in both; the whistle is the torenwachter-guh's reward and has none) ---
    h.shaped("stepstation", ["IKI", "GWG", "IMI"],
             {"I": "minecraft:iron_ingot", "K": "guhs:kaas_knabbels", "G": "guhs:grillspies", "W": "guhs:guh_wire", "M": "minecraft:minecart"},
             "guhs:stepstation")
    h.shaped("haltepaaltje", ["W", "G", "I"], {"W": "minecraft:yellow_wool", "G": "guhs:grillspies", "I": "minecraft:iron_ingot"},
             "guhs:haltepaaltje", 2)


# =====================================================================================================================
# texts
# =====================================================================================================================
K = "gui.guhs.techbezorg."


def _texts():
    bereik, haltes, rugzak, vk = getal("BEREIK"), getal("MAX_HALTES"), getal("RUGZAK"), vadskracht.getal("STEPSTATION")
    return {
        "block.guhs.stepstation": "Stepstation",
        "block.guhs.stepstation.lore": f"Hier woont een Bezorgguhtje. Geef het station {vk} vadskracht en zet Haltepaaltjes neer "
                                       f"(tot {bereik} blokken ver): hij brengt alles rond op zijn stepje. Tuut tuut, njeg!",
        "block.guhs.haltepaaltje": "Haltepaaltje",
        "block.guhs.haltepaaltje.lore": "Zet het tegen een kist of machine en kies: ophalen of afleveren, en wat er mee mag. "
                                        "Het Bezorgguhtje van het dichtstbijzijnde Stepstation komt langs.",
        "item.guhs.bezorgguhtje_fluitje": "Bezorgguhtje-fluitje",
        "item.guhs.bezorgguhtje_fluitje.lore": "Fluit en je Bezorgguhtje komt aanstepperen met zijn rugzak open.",
        "item.guhs.bezorgguhtje_fluitje.lore.sluip": "Sluipend fluiten: al je Bezorgguhtjes gaan naar huis en beginnen opnieuw.",
        "item.guhs.bezorgguhtje_spawn_egg": "Bezorgguhtje-spawnei",
        # --- the two screens ---
        K + "rugzak": "Rugzak",
        K + "rugzak.dicht": "Onderweg: de rugzak zit op zijn rug. Wacht tot hij thuis is, of fluit hem bij je.",
        K + "haltes": "Haltes (%s/%s)",
        K + "geen_haltes": f"Nog geen haltes. Zet een Haltepaaltje tegen een kist, binnen {bereik} blokken van hier.",
        K + "ophalen": "Ophalen",
        K + "afleveren": "Afleveren",
        K + "ophalen.tooltip": "Ophalen: het Bezorgguhtje pakt hier spullen uit de kist, net als een trechter eronder (bij een "
                               "oven dus alleen wat klaar is). Hij neemt alleen mee wat een aflever-halte ook echt kwijt kan.",
        K + "afleveren.tooltip": "Afleveren: het Bezorgguhtje stopt hier spullen uit zijn rugzak in de kist, aan de kant waar "
                                 "het paaltje staat, net als een trechter (zet het paaltje dus OP een oven om hem te vullen).",
        K + "filter": "Wat mag mee?",
        K + "filter.tooltip": "Klik met een item op een vakje. Alles leeg = alles mag mee.",
        K + "filter.alles": "(alles)",
        K + "wis": "Leegmaken",
        K + "omhoog": "Eerder in de ronde",
        K + "omlaag": "Later in de ronde",
        K + "ander_station": "Ander station",
        K + "ander_station.tooltip": f"Koppelt dit paaltje aan het volgende Stepstation binnen {bereik} blokken.",
        K + "naar_huis": "Naar huis!",
        K + "naar_huis.tooltip": "Het Bezorgguhtje stept terug naar het station en begint zijn ronde opnieuw.",
        K + "halte.rij.ophalen": "Halte %s: ophalen",
        K + "halte.rij.afleveren": "Halte %s: afleveren",
        K + "halte.rij.afstand": "%s blokken ver",
        K + "halte.rij.geen_kist": "Geen kist of machine bij dit paaltje!",
        K + "halte.rij.te_ver": "Niet geladen of te ver weg: hij slaat deze halte over.",
        K + "halte.station": "Halte %s van %s",
        K + "halte.station.waar": "Stepstation op %s, %s, %s",
        K + "halte.los": "Hoort nog bij geen Stepstation",
        K + "halte.geen_kist.kort": "Hier staat geen kist of machine!",
        # --- what the Bezorgguhtje is doing (the station's screen, the hover readout, a click on the guhtje) ---
        K + "stand.slaapt": "Het Bezorgguhtje slaapt: het Stepstation heeft geen vadskracht. Zzz, njeg.",
        K + "stand.rust": "Het Bezorgguhtje rust uit bij het station.",
        K + "stand.rijdt": "Het Bezorgguhtje stept naar halte %s van %s.",
        K + "stand.laadt": "Het Bezorgguhtje rommelt in zijn rugzak bij halte %s.",
        K + "stand.naar_huis": "Het Bezorgguhtje stept naar huis.",
        K + "stand.naar_speler": "Het Bezorgguhtje komt eraan. Tuut tuut!",
        K + "stand.bij_speler": "Het Bezorgguhtje wacht met zijn rugzak open.",
        K + "stand.zoek": "Het Bezorgguhtje is even zoek. Hij komt zo terug bij het station, njeg.",
        K + "hover.rugzak": f"Rugzak: %s/{rugzak} stapels",
        K + "hover.geen_haltes": f"Geen haltes: zet een Haltepaaltje tegen een kist (tot {bereik} blokken ver)",
        K + "hover.geen_aflever": "Geen aflever-halte: niemand wil de spullen hebben",
        K + "hover.vol": "De rugzak raakt niet leeg: bij de aflever-halte past het niet. Vahoeg!",
        # --- placing a pole ---
        K + "halte.gekoppeld": "Halte %s van het Stepstation op %s, %s, %s. Tuut tuut!",
        K + "halte.geen_station": f"Geen Stepstation binnen {bereik} blokken. Het paaltje wacht tot er een komt, njeg.",
        K + "halte.vol": f"Elk Stepstation in de buurt heeft al {haltes} haltes. Meer kan zijn rugzakje niet aan!",
        K + "halte.geen_kist": "Hier staat geen kist of machine tegenaan. Het Bezorgguhtje slaat dit paaltje over.",
        K + "halte.mag_niet": "Dit paaltje mag hier niet bij: de kist staat in een beschermd gebouw of in het klusgebied van andermans "
                              "Guhhuisje. Het Bezorgguhtje slaat het over, njeg.",
        K + "station.gekoppeld": "Er komt een Bezorgguhtje wonen! %s Haltepaaltjes in de buurt horen er nu bij.",
        K + "station.nieuw": "Er komt een Bezorgguhtje wonen! Zet nu Haltepaaltjes tegen je kisten.",
        # --- the whistle ---
        K + "fluit.komt": "Tuut tuut! Je Bezorgguhtje komt eraan.",
        K + "fluit.niemand": f"Geen Bezorgguhtje van jou binnen {bereik} blokken. Njeg?",
        K + "fluit.slaapt": "Je Bezorgguhtje slaapt: zijn Stepstation heeft geen vadskracht.",
        K + "fluit.naar_huis": "Naar huis, njeg! (%s)",
        # --- the guhtje itself ---
        K + "guhtje.dakloos": "Njeg? Ik zoek nog een Stepstation om in te wonen.",
        K + "guhtje.lekker": "Vahoeg! Van een kaasknabbel step ik extra hard.",
        K + "guhtje.aai": "Tuut tuut, njeg!",
        "subtitles.guhs.techbezorg.bel": "Bezorgguhtje belt: tuut tuut",
        "subtitles.guhs.techbezorg.fluitje": "Fluitje",
        "subtitles.guhs.techbezorg.hop": "Bezorgguhtje hupt: vahoeg!",
        "subtitles.guhs.techbezorg.rits": "Rugzak ritst",
        "subtitles.guhs.techbezorg.guhtje": "Bezorgguhtje piept",
    }


TEXTS = None


def texts(h):
    global TEXTS
    TEXTS = _texts()
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)
    # the Guhdex page (the skelet wrote a placeholder; this is the real one)
    bbq2.pagina(h, "bezorgguhtje", "Bezorgguhtje", "Ongewoon (woont in een Stepstation)",
                "Een mini-guh op een step met een veel te grote rugzak. Hij woont in een Stepstation en stept zijn rondje langs "
                "de Haltepaaltjes: ophalen hier, afleveren daar. Is de weg dicht, dan hupt hij er gewoon overheen. Hij raakt "
                "nooit iets kwijt en komt altijd weer thuis. Tuut tuut, njeg!")


# =====================================================================================================================
# sounds (vanilla sounds, pitched)
# =====================================================================================================================
SOUNDS = {
    "techbezorg.bel": [{"name": "minecraft:block.note_block.bell", "type": "event", "pitch": 1.7, "volume": 0.5}],
    "techbezorg.fluitje": [{"name": "minecraft:block.note_block.flute", "type": "event", "pitch": 1.9}],
    "techbezorg.hop": [{"name": "minecraft:entity.rabbit.jump", "type": "event", "pitch": 1.5},
                       {"name": "minecraft:entity.slime.jump_small", "type": "event", "pitch": 1.7, "volume": 0.6}],
    "techbezorg.rits": [{"name": "minecraft:item.bundle.insert", "type": "event", "pitch": 1.3, "volume": 0.7}],
    "techbezorg.guhtje": [{"name": "guhs:entity.guh.ambient", "type": "event", "pitch": 1.8, "volume": 0.6}],
}


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# advancements: visible in the tab Guh-technologie, hidden ones for the FTB chapter (tech_quests uses fq.adv("tech_bezorg_*"))
# =====================================================================================================================
VERBORGEN = ("tech_bezorg_station", "tech_bezorg_halte", "tech_bezorg_bezorgd", "tech_bezorg_fluitje")


def advancements(h):
    for name in VERBORGEN:
        bbq2.verborgen(h, name)
    bbq2.zichtbaar(h, "techniek", "tech_bezorg_station", "root", "guhs:stepstation", "task", "Tuut tuut!",
                   "Zet een Stepstation neer en geef het vadskracht: er komt een Bezorgguhtje wonen")
    bbq2.zichtbaar(h, "techniek", "tech_bezorg_bezorgd", "tech_bezorg_station", "guhs:haltepaaltje", "goal", "Bezorgd, njeg!",
                   "Laat je Bezorgguhtje iets van een ophaal-halte naar een aflever-halte brengen")
    bbq2.zichtbaar(h, "techniek", "tech_bezorg_fluitje", "tech_bezorg_station", "guhs:bezorgguhtje_fluitje", "task",
                   "Op je wenken bediend", "Fluit je Bezorgguhtje bij je met het Bezorgguhtje-fluitje van de torenwachter-guh")


# =====================================================================================================================
# test room
# =====================================================================================================================
def test_templates(h):
    """techbezorg_test_kamer: 15 x 6 x 15 with a stone floor (things stand at helper y 2)."""
    s = h.Structure((15, 6, 15))
    s.fill(0, 0, 0, 14, 0, 14, "minecraft:stone")
    s.save("techbezorg_test_kamer")


# =====================================================================================================================
def selfcheck(h):
    A, D = h.A, h.D
    paden = [f"{A}/blockstates/stepstation.json", f"{A}/blockstates/haltepaaltje.json", f"{A}/models/item/stepstation.json",
             f"{A}/models/item/haltepaaltje.json", f"{A}/models/item/bezorgguhtje_fluitje.json",
             f"{A}/models/item/bezorgguhtje_spawn_egg.json", f"{D}/recipe/stepstation.json", f"{D}/recipe/haltepaaltje.json",
             f"{D}/loot_table/blocks/stepstation.json", f"{D}/loot_table/blocks/haltepaaltje.json",
             f"{D}/structure/techbezorg_test_kamer.nbt"]
    paden += [f"{A}/models/block/stepstation_{s}.json" for s in vadskracht.STATEN]
    paden += [f"{D}/advancement/quest/{n}.json" for n in VERBORGEN]
    missing = [p for p in paden if not os.path.exists(p)]
    missing += [k for k in TEXTS if k not in h.NL]
    missing += [f"subtitles.guhs.{e}" for e in SOUNDS if f"subtitles.guhs.{e}" not in h.NL]
    for recipe in ("stepstation", "haltepaaltje"):        # tier Saus: a grillspies or blubroom in it
        import json
        tekst = json.dumps(json.load(open(f"{D}/recipe/{recipe}.json", encoding="utf-8")))
        if "guhs:grillspies" not in tekst and "guhs:blubroom" not in tekst:
            missing.append(f"recipe {recipe}: no Saus tier ingredient")
    missing += modellen.check(h)
    if missing:
        raise SystemExit(f"tech_bezorg: missing {missing}")


def build(h):
    textures(h)
    modellen.build(h)
    blocks_and_items(h)
    texts(h)
    sounds(h)
    advancements(h)
    test_templates(h)
    selfcheck(h)
