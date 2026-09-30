"""
Zwevende guh-eilandjes (the eilanden feature): a very rare cluster of five floating islands high in the Guhmension sky.

  - the structure zwevende_eilanden (101 x 92 x 101): a guh-face square on the ground with two wolkenlifts (up and
    down), and in the sky the main island (the Wolkguh's nest, a guh-face gate, a guh-face kaassaus pool, guhbloesem
    trees), the Guhrots (a giant guh-head rock drooling two kaassaus waterfalls, with a treasure cave in its mouth),
    the Bloesemeiland (picnic under the blossoms), the Kaaswolkje (a kaasknabbel boulder with a waterfall) and, up a
    trail of cloud stepping stones, the Wolkje (a lookout with a second chest). Bridges connect the rest.
  - the Wolkguh: a unique guh variant (GuhVariant.WOLK) with a little cloud on its head, a cloud tail and cloud socks, wearing the wolkenmuts and
    the wolkenkraag (guh clothes you get by taming and undressing it)
  - blocks: wolkenlift (the pad) and wolkenstroom (its invisible stream); item: wolkensuikerspin
  - advancements, FTB quests, the Guhdex page, lang (English + Dutch)

check(s) is the geometry self-check of the template: no holes in walk floors, everything reachable on foot from the
lift (stepping stones with jumps), the Wolkguh on solid ground, no floating junk, plants and lamps on something solid,
lift columns whole and free, kaassaus kept in its pools. build() runs it and fails loudly if it finds anything.
Keep the layout constants in sync with feature/eilanden/EilandenProtection.java (ISLANDS_FROM, SQUARE_RADIUS).
"""
import math
import random
from collections import deque

NAME = "zwevende_eilanden"
W, H, D = 101, 92, 101
C = 50                      # the middle of the template (x and z): the up lift
S0 = 62                     # main island: top block y
ISLANDS_FROM = 36           # everything from here up is sky (protected); keep in sync with EilandenProtection
SQUARE_RADIUS = 13          # (EilandenProtection.SQUARE_RADIUS: the plaza square around the middle)
PLAZA_R = 13
UP_COL = [(x, z) for x in (49, 50, 51) for z in (49, 50, 51)]
DOWN_COL = [(x, z) for x in (38, 39, 40) for z in (54, 55, 56)]
TOP = S0 + 2                # the highest stream block of both lifts

# island: centre x, centre z, radius, top y, top block
ISLANDS = {
    "main": (50, 75, 17, S0, "pink_wool"),
    "guhrots": (16, 72, 10, 68, "pink_wool"),
    "bloesem": (84, 80, 9, 57, "pink_wool"),
    "kaaswolk": (82, 34, 7, 64, "yellow_wool"),
    "wolkje": (18, 26, 6, 74, "white_wool"),
}

PLANTS = {"guhs:roze_gras", "guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes"}
NEEDS_SUPPORT = PLANTS | {"guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint", "guhs:guh_taart", "minecraft:white_carpet",
                          "minecraft:pink_carpet", "guhs:white_kussen", "guhs:pink_kussen", "guhs:guh_bank", "guhs:guh_stoel",
                          "guhs:guh_tafel", "minecraft:chest"}
FLUID = "guhs:kaas_saus"
STREAM = "guhs:wolkenstroom"
LIFT = "guhs:wolkenlift"
# what you can walk through (feet / head) and what you can't stand on
PASSABLE = {None, "minecraft:air", "minecraft:white_carpet", "minecraft:pink_carpet", STREAM} | PLANTS | {
    "guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint", "guhs:vlaggetjes"}
NOT_FLOOR = {None, "minecraft:air", STREAM, FLUID, "minecraft:cherry_fence"} | PLANTS | {
    "guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint", "guhs:vlaggetjes"}

ADVANCEMENT_QUESTS = ["eilanden_lift", "eilanden_gevoerd", "eilanden_opgevangen", "seen_wolk"]


# =====================================================================================================================
# the Wolkguh (variant), its clothes and their icons (make_guh_variants.py / make_clothes_icons.py)
# =====================================================================================================================
_H = [0, 6, -2]   # head pivot
BONES = {
    # the Wolkguh itself: a little cloud floating on its head (always, also above a hat), a big puffy cloud tail
    # and cloud socks
    "wolk_wolkje": ("head", _H, "wolk_pluis", [([-3, 20.8, -8], [6, 2.2, 4], 0.15), ([-4.6, 21.2, -7.5], [2.4, 1.6, 3], 0.1),
                                              ([2.2, 21.2, -7.5], [2.4, 1.6, 3], 0.1), ([-1.8, 22.6, -7.6], [3.2, 1.8, 3.2], 0.1),
                                              ([0.6, 22.3, -7], [2.2, 1.4, 2.2], 0)]),
    "wolk_staart": ("tail", [0, 1.5, 12], "wolk_pluis", [([-3, 1, 10.5], [6, 4, 4], 0.3), ([-2, 4, 12], [4, 3, 3.5], 0.2),
                                                         ([-4.5, 2, 12.5], [3, 3, 3], 0.1), ([1.5, 2, 12.5], [3, 3, 3], 0.1),
                                                         ([-1.5, 1.5, 14], [3, 3, 2.5], 0.1)]),
    "wolk_sok_fl": ("leg_front_left", [2, 1, 0], "wolk_pluis", [([4.25, 0, -2.5], [3.5, 1.6, 4.5], 0.55)]),
    "wolk_sok_fr": ("leg_front_right", [-6, 1, 0], "wolk_pluis", [([-7.75, 0, -2.5], [3.5, 1.6, 4.5], 0.55)]),
    "wolk_sok_bl": ("leg_back_left", [6, 1, 9], "wolk_pluis", [([4.5, 0, 7], [3.5, 1.6, 4.5], 0.55)]),
    "wolk_sok_br": ("leg_back_right", [-6, 1, 9], "wolk_pluis", [([-8, 0, 7], [3.5, 1.6, 4.5], 0.55)]),
    # its clothes: a cloud hat and a fluffy cloud collar
    "outfit_wolkenmuts": ("head", _H, "wolkenmuts", [([-4.5, 15, -9.5], [9, 2.5, 7], 0.1), ([-3, 17.3, -8.5], [4.5, 2.6, 4.5], 0),
                                                    ([0.5, 17, -7.5], [3.5, 2.2, 3.5], 0), ([-5.8, 15.4, -7.5], [2.6, 2.2, 3.2], 0),
                                                    ([3.4, 15.4, -8], [2.4, 2, 3], 0)]),
    "outfit_wolkenkraag": ("body", [0, 6, 6], "wolkenkraag", [([-7.6, 1, -1.4], [15.2, 10.4, 2.8], 0.2), ([-6.5, 9.8, -2.2], [4, 3, 3.6], 0),
                                                             ([2.5, 9.8, -2.2], [4, 3, 3.6], 0), ([-2, 10.8, -2.4], [4, 2.4, 3.6], 0),
                                                             ([-8.2, 3, -2], [2.6, 5, 3.4], 0), ([5.6, 3, -2], [2.6, 5, 3.4], 0)]),
}
CLOTHES = ["wolkenmuts", "wolkenkraag"]
SKY = (150, 205, 250)


def _cloud(v, rng, base=(248, 250, 255), blue=SKY, sparkle=None):
    """A cloud swatch: fluffy white with sky-blue shadows (and optional sparkles)."""
    a = v.fabric(base, rng, 10)
    px = v.SWATCH * 4
    for _ in range(9):
        x, y = rng.integers(0, px, 2)
        r = rng.integers(3, 7)
        for yy in range(max(0, y - r), min(px, y + r)):
            for xx in range(max(0, x - r), min(px, x + r)):
                if (xx - x) ** 2 + (yy - y) ** 2 < r * r:
                    a[yy, xx] = a[yy, xx] * 0.55 + [c * 0.45 for c in blue]
    if sparkle:
        for _ in range(10):
            x, y = rng.integers(1, px - 1, 2)
            a[y, x] = sparkle
    return a


def variants(rng, v):
    return {"wolk": ((224, 238, 255), {"wolk_pluis": lambda: _cloud(v, rng)})}


def clothes(rng, v):
    return {"wolkenmuts": {"wolkenmuts": lambda: _cloud(v, rng, sparkle=(255, 190, 225))},
            "wolkenkraag": {"wolkenkraag": lambda: _cloud(v, rng, base=(244, 248, 255), blue=(170, 215, 255))}}


def icons(ic):
    col = {"a": (150, 190, 230), "b": (250, 252, 255), "c": (205, 230, 255), "p": (255, 150, 200)}
    muts = ["................", "................", "......aaa.......", ".....abbba......", "..aaabbbbbaaa...",
            ".abbbbbpbbbbba..", "abbcbbbbbbcbbba.", "abbbbbbbbbbbbba.", "abccbbbcbbbccba.", ".aaaaaaaaaaaaa..",
            "................", "................", "................", "................", "................", "................"]
    kraag = ["................", "................", "................", "..aaa.....aaa...", ".abbba...abbba..",
             "abbbbbaaabbbbba.", "abcbbbbbbbbbcba.", "abbbbbbbbbbbbba.", ".abbcbbbbbcbba..", "..abbbbbbbbba...",
             "...aaabbbaaa....", "......aaa.......", "................", "................", "................", "................"]
    return {"wolkenmuts": ic.icon(muts, col), "wolkenkraag": ic.icon(kraag, col)}


# =====================================================================================================================
# resources (make_v2)
# =====================================================================================================================
def build(h):
    blocks_and_items(h)
    wolkenkist(h)
    advancements(h)
    texts(h)
    h.TEMPLATE_SIZES[NAME] = 64      # the flatness check samples the ground this far around the middle
    h.FLATNESS[NAME] = 28
    none = {"bounding_box": "piece", "spawns": []}
    h.structure(NAME, h.GUHMENSION_LAND, spacing=100, separation=40, salt=20240177,
                spawn_overrides={"creature": none, "monster": none, "ambient": none, "water_creature": none})
    s = template(h)
    problems = check(s)
    if problems:
        raise SystemExit("zwevende_eilanden geometry check failed:\n  " + "\n  ".join(problems[:40]))
    print("zwevende_eilanden: geometry check ok")
    s.save(NAME)


def blocks_and_items(h):
    A, D = h.A, h.D
    rng = random.Random(4417)
    # --- the wolkenlift: a cloud pad with a guh face that looks where it puffs you ---
    face = ["................", "..cc........cc..", ".cppc......cppc.", ".cppc......cppc.", "..cccccccccccc..",
            ".cwwwwwwwwwwwwc.", "cwwwwwwwwwwwwwwc", "cwwkkwwwwwwkkwwc", "cwwkkwwwwwwkkwwc", "cwwwwwwppwwwwwwc",
            "cwwwwwwwwwwwwwwc", "cwwwwwkwwkwwwwwc", "cwwwwwwkkwwwwwwc", ".cwwwwwwwwwwwwc.", "..cccccccccccc..", "................"]
    for name, bg, line in (("wolkenlift_top", (246, 249, 255), (130, 190, 240)), ("wolkenlift_down_top", (205, 230, 255), (90, 150, 220))):
        img = h.noise_tex(bg, 5, 31 + len(name))
        px = img.load()
        for y, row in enumerate(face):
            for x, ch in enumerate(row):
                if ch == "c":
                    px[x, y] = line + (255,)
                elif ch == "p":
                    px[x, y] = (255, 160, 205, 255)
                elif ch == "k":
                    px[x, y] = (40, 30, 60, 255)
                elif ch == "w":
                    base = px[x, y]
                    px[x, y] = tuple(min(255, c + 6) for c in base[:3]) + (255,)
        h.save(img, "block", f"{name}.png")
    side = h.noise_tex((244, 248, 255), 6, 77, spots=[((200, 225, 250), 0.18), ((255, 255, 255), 0.12)])
    px = side.load()
    for x in range(16):                                   # a little blue cloud rim
        for y in (0, 15):
            px[x, y] = (170, 210, 245, 255)
    h.save(side, "block", "wolkenlift_side.png")
    for name, top in (("wolkenlift", "wolkenlift_top"), ("wolkenlift_down", "wolkenlift_down_top")):
        h.w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": f"guhs:block/{top}", "bottom": "guhs:block/wolkenlift_side", "side": "guhs:block/wolkenlift_side"}})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    h.w(f"{A}/blockstates/wolkenlift.json", {"variants": {
        f"down={str(down).lower()},facing={f}": {"model": "guhs:block/wolkenlift_down" if down else "guhs:block/wolkenlift", **({"y": r} if r else {})}
        for down in (False, True) for f, r in rot.items()}})
    h.w(f"{A}/models/item/wolkenlift.json", {"parent": "guhs:block/wolkenlift"})
    h.w(f"{A}/models/block/wolkenstroom.json", {"textures": {"particle": "guhs:block/wolkenlift_side"}})
    h.w(f"{A}/blockstates/wolkenstroom.json", {"variants": {"": {"model": "guhs:block/wolkenstroom"}}})
    h.self_drop("wolkenlift")
    h.shaped("wolkenlift", ["WFW", "WKW", "WWW"], {"W": "minecraft:white_wool", "F": "minecraft:feather", "K": "guhs:guh_kristal"},
             "guhs:wolkenlift", 2)

    # --- the wolkensuikerspin: cloud candy floss on a stick ---
    pal = dict(h.ITEM_PAL)
    pal.update({"W": (252, 252, 255, 255), "c": (200, 228, 255, 255), "p": (255, 175, 215, 255), "b": (150, 100, 60, 255)})
    h.save(h.grid(["................", ".....kkkkk......", "...kkWWcWWkk....", "..kWWpWWWcWWk...", ".kWcWWWWpWWWWk..",
                   ".kWWWWcWWWWpWk..", "kWpWWWWWWcWWWk..", "kWWWcWWpWWWWck..", ".kWWWWWWWWpWk...", ".kcWWpWWcWWWk...",
                   "..kkWWWWWWkk....", "....kkbbkk......", "......bb........", ".....bb.........", "....bb..........",
                   "...bb..........."], pal), "item", "wolkensuikerspin.png")
    h.item_model("wolkensuikerspin")
    h.shapeless("wolkensuikerspin", ["minecraft:sugar", "minecraft:white_wool", "minecraft:stick", "guhs:kaas_knabbels"],
                "guhs:wolkensuikerspin", 2)

    # --- the treasure of the islands ---
    h.w(f"{D}/loot_table/chests/{NAME}.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:wolkensuikerspin", "functions": h.count_fn(3, 6)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:wolkenlift", "functions": h.count_fn(2, 4)}]},
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
            {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 4, "functions": h.count_fn(8, 16)},
            {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 2, "functions": h.count_fn(2, 5)},
            {"type": "minecraft:item", "name": "guhs:guh_kristal", "weight": 2, "functions": h.count_fn(2, 6)},
            {"type": "minecraft:item", "name": "guhs:guhbloesem_sapling", "weight": 1, "functions": h.count_fn(1, 3)},
            {"type": "minecraft:item", "name": "minecraft:feather", "weight": 2, "functions": h.count_fn(4, 10)},
            {"type": "minecraft:item", "name": "guhs:vahoege_vads_ingot", "weight": 1, "functions": h.count_fn(1, 2)}]}]})


def wolkenkist(h):
    """(2.9, kleding) the wolkenkist on the Wolkje: the Wolkguh's outfit (one of the two, often both) and cloud treats."""
    h.w(f"{h.D}/loot_table/chests/{NAME}_wolkenkist.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:wolkenmuts"}, {"type": "minecraft:item", "name": "guhs:wolkenkraag"}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:wolkenmuts", "weight": 1},
                                 {"type": "minecraft:item", "name": "guhs:wolkenkraag", "weight": 1}, {"type": "minecraft:empty", "weight": 1}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:wolkensuikerspin", "functions": h.count_fn(2, 5)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:wolkenlift", "functions": h.count_fn(1, 3)}]}]})


def advancements(h):
    D = h.D
    for name in ADVANCEMENT_QUESTS:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    tame = {"trigger": "minecraft:tame_animal", "conditions": {"entity": [{"condition": "minecraft:entity_properties", "entity": "this",
                                                                           "predicate": {"type": "guhs:guh", "nbt": "{Variant:\"wolk\"}"}}]}}
    h.w(f"{D}/advancement/quest/tamed_wolk.json", {"criteria": {"done": tame}})
    for name, parent, icon, frame, crit, (ten, tnl), (den, dnl) in [
        ("find_zwevende_eilanden", "enter_guhmension", "guhs:wolkenlift", "goal",
         {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": [f"guhs:{NAME}"]}}}},
         ("Guhs in the clouds", "Guhs in de wolken"), ("Find the very rare floating guh islands", "Vind de heel zeldzame zwevende guh-eilandjes")),
        ("wolkguh_getemd", "find_zwevende_eilanden", "guhs:wolkenmuts", "challenge", tame,
         ("On cloud nine", "Op wolkje negen"), ("Tame the one and only Wolkguh", "Tem de enige echte Wolkguh")),
        ("wolkenpakje", "wolkguh_getemd", "guhs:wolkenkraag", "goal",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:wolkenmuts"}, {"items": "guhs:wolkenkraag"}]}},
         ("Head in the clouds", "Met je hoofd in de wolken"), ("Get the Wolkguh's cloud hat and cloud collar", "Krijg de wolkenmuts en de wolkenkraag van de Wolkguh")),
    ]:
        h.w(f"{D}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": True},
            "criteria": {"done": crit}})
        h.lang(f"advancements.guhs.guhmension.{name}.title", ten, tnl)
        h.lang(f"advancements.guhs.guhmension.{name}.description", den, dnl)


def texts(h):
    for key, en, nl in [
        ("block.guhs.wolkenlift", "Cloud Lift", "Wolkenlift"),
        ("block.guhs.wolkenstroom", "Cloud Stream", "Wolkenstroom"),
        ("item.guhs.wolkensuikerspin", "Cloud Candy Floss", "Wolkensuikerspin"),
        ("item.guhs.wolkenmuts", "Cloud Hat", "Wolkenmuts"),
        ("item.guhs.wolkenkraag", "Cloud Collar", "Wolkenkraag"),
        ("entity.guhs.guh.wolk", "Cloud Guh", "Wolkguh"),
        ("gui.guhs.guhdex.rarity.wolk", "Rarity: Unique (floating guh islands)", "Zeldzaamheid: Uniek (zwevende guh-eilandjes)"),
        ("gui.guhs.guhdex.info.wolk",
         "Fluffy, with a cloud on its head; it floats when it falls. Only on the floating islands. Trusts you after 8 kaas knabbels, then catches your falls!",
         "Pluizig, met een wolkje op zijn hoofd: valt hij, dan zweeft hij. Alleen op de zwevende eilandjes. Na 8 kaasknabbels vertrouwt hij je en vangt hij jou op!"),
        (f"structure.guhs.{NAME}", "Floating Guh Islands", "Zwevende guh-eilandjes"),
        (f"structure.guhs.{NAME}.tooltip", "Very rare: islands in the sky, a wolkenlift and the unique Wolkguh",
         "Heel zeldzaam: eilandjes in de lucht, een wolkenlift en de unieke Wolkguh"),
        ("quest.guhs.eilanden.feed", "The Wolkguh munches... %s more knabbels and it's vads enough to trust you",
         "De Wolkguh smult ervan... nog %s knabbels en hij is vads genoeg om je te vertrouwen"),
        ("quest.guhs.eilanden.fed", "VAHOEG! The Wolkguh is vads enough now. Keep feeding it!",
         "VAHOEG! De Wolkguh is nu vads genoeg. Blijf hem voeren!"),
        ("quest.guhs.eilanden.tamed", "The Wolkguh floats over to you: it's yours! As long as it's near, it catches you when you fall. Undress it in its wardrobe for the cloud hat and collar.",
         "De Wolkguh zweeft naar je toe: hij is van jou! Zolang hij dichtbij is, vangt hij je op als je valt. Kleed hem uit in zijn kledingkast voor de wolkenmuts en de wolkenkraag."),
        ("quest.guhs.eilanden.caught_by_guh", "Poof! %s caught you like a pillow of cloud", "Poef! %s vangt je op als een kussen van wolk"),
        ("quest.guhs.eilanden.no_hurt", "Njeg! You can't hurt a cloud. Try kaas knabbels instead...",
         "Njeg! Een wolkje kun je geen pijn doen. Probeer het eens met kaasknabbels..."),
        ("quest.guhs.eilanden.caught", "Njeg, careful! The clouds catch you...", "Njeg, pas op! De wolkjes vangen je op..."),
        ("quest.guhs.eilanden.lift_up", "VAHOEG! Up into the clouds!", "VAHOEG! De wolken in!"),
        ("quest.guhs.eilanden.lift_down", "Floating down softly... wheee", "Zachtjes naar beneden zweven... wieee"),
        ("gui.guhs.eilanden.no_build", "Njeg! The floating guh islands are sacred: no breaking or building here.",
         "Njeg! De zwevende guh-eilandjes zijn heilig: hier mag je niks slopen of bouwen."),
    ]:
        h.lang(key, en, nl)


# =====================================================================================================================
# the template
# =====================================================================================================================
def face_role(u, v, R):
    """A guh face (top or front view, v up) of radius R: skin, ear, ear_in, eye, shine, nose, mouth, cheek or None."""
    role = None
    for sx in (-1, 1):
        d = math.dist((u, v), (sx * 0.62 * R, 0.76 * R))
        if d <= 0.3 * R + 0.35:
            role = "ear_in" if d <= 0.15 * R + 0.2 else "ear"
    if (u / (R + 0.4)) ** 2 + (v / (0.86 * R + 0.4)) ** 2 <= 1:
        role = "skin"
        for sx in (-1, 1):
            if math.dist((u, v), (sx * 0.37 * R, 0.12 * R)) <= 0.18 * R + 0.3:
                role = "eye"
                if round(u) == round(sx * 0.37 * R + 0.07 * R) and round(v) == round(0.12 * R + 0.08 * R):
                    role = "shine"
            if math.dist((u, v), (sx * 0.64 * R, -0.3 * R)) <= 0.14 * R + 0.2:
                role = "cheek"
        if math.dist((u, v), (0, -0.2 * R)) <= 0.1 * R + 0.3:
            role = "nose"
        y0 = round(-0.38 * R)
        if (round(u), round(v)) in {(-2, y0), (-1, y0 - 1), (0, y0), (1, y0 - 1), (2, y0)}:
            role = "mouth"
    return role


def lampion(s, x, y, z, colour="roze"):
    """A cherry post with a lampgion on top (y = the ground block)."""
    post = {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}
    s.set(x, y + 1, z, "minecraft:cherry_fence", post)
    s.set(x, y + 2, z, "minecraft:cherry_fence", post)
    s.set(x, y + 3, z, f"guhs:lampion_{colour}", {"hanging": "false", "waterlogged": "false"})


def fence(x_axis):
    return {"north": str(not x_axis).lower(), "south": str(not x_axis).lower(), "east": str(x_axis).lower(),
            "west": str(x_axis).lower(), "waterlogged": "false"}


def island(h, s, rng, name):
    cx, cz, r, S, top = ISLANDS[name]
    mc = h.mc
    bumps = [rng.uniform(-1.0, 1.4) for _ in range(12)]

    def edge(a):
        t = (a / (2 * math.pi)) * 12
        i = int(t) % 12
        f = t - int(t)
        return r + bumps[i] * (1 - f) + bumps[(i + 1) % 12] * f

    cells = {}
    for x in range(cx - r - 3, cx + r + 4):
        for z in range(cz - r - 3, cz + r + 4):
            d = math.dist((x, z), (cx, cz))
            rr = edge(math.atan2(z - cz, x - cx) % (2 * math.pi))
            if d <= rr and 0 <= x < W and 0 <= z < D:
                cells[(x, z)] = d / rr
    for (x, z), f in cells.items():
        depth = int(round((1 - f) ** 0.8 * r * 0.8 + 2 + rng.random() * 1.6))
        s.set(x, S, z, mc(top))
        for k in range(1, depth + 1):
            y = S - k
            if k <= 2:
                block = "guhs:kaasknabbel_dirt"
            elif k == depth and f < 0.35 and rng.random() < 0.3:
                block = "guhs:guh_kristal_blok"
            else:
                block = rng.choice([mc("pink_terracotta"), mc("pink_terracotta"), "guhs:kaasknabbel_stone", mc("magenta_terracotta")])
            s.set(x, y, z, block)
    # a skirt of cloud puffs under the rim
    for i in range(int(r * 1.6)):
        a = i / int(r * 1.6) * 2 * math.pi + rng.uniform(-0.1, 0.1)
        px, pz = cx + math.cos(a) * (r - 0.8), cz + math.sin(a) * (r - 0.8)
        pr = rng.uniform(1.4, 2.3)
        py = S - 1 - pr * 0.6
        for x in range(int(px - pr - 1), int(px + pr + 2)):
            for z in range(int(pz - pr - 1), int(pz + pr + 2)):
                for y in range(int(py - pr - 1), S):
                    if math.dist((x, y * 1.4, z), (px, py * 1.4, pz)) <= pr and s.get(x, y, z) is None:
                        s.set(x, y, z, mc("white_wool"))
    return cells


def decorate(h, s, rng, cells, S, top, density=0.28):
    """Pink grass and guh flowers on the free top blocks."""
    for (x, z) in cells:
        if s.get(x, S, z) == h.mc(top) and s.get(x, S + 1, z) is None and s.get(x, S + 2, z) is None:
            r = rng.random()
            if r < 0.06:
                s.set(x, S + 1, z, rng.choice(["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes"]))
            elif r < density:
                s.set(x, S + 1, z, "guhs:roze_gras")


def bridge(h, s, rng, axis, fixed, a, b, ya, yb):
    """A bridge along the x (axis="x") or z axis at x/z = fixed (walkway fixed-1..fixed+1, railings on fixed+-2),
    from a to b (inclusive), going from height ya to yb; stairs where it goes up or down."""
    mc = h.mc
    n = abs(b - a) + 1
    step = 1 if b > a else -1
    heights = [ya + round((yb - ya) * i / (n - 1)) for i in range(n)]
    go = {("x", 1): "east", ("x", -1): "west", ("z", 1): "south", ("z", -1): "north"}
    back = {"east": "west", "west": "east", "south": "north", "north": "south"}
    for i in range(n):
        t = a + i * step
        y = heights[i]
        stair = None
        if i > 0 and heights[i - 1] < y:
            stair = go[(axis, step)]             # climbing in the walking direction
        if i < n - 1 and heights[i + 1] < y:
            stair = back[go[(axis, step)]]       # climbing when walked the other way
        for o in range(-2, 3):
            x, z = (t, fixed + o) if axis == "x" else (fixed + o, t)
            if stair and abs(o) <= 1:
                s.set(x, y, z, mc("cherry_stairs"), {"facing": stair, "half": "bottom", "shape": "straight", "waterlogged": "false"})
            else:
                s.set(x, y, z, "guhs:guhbloesem_planks" if abs(o) <= 1 else mc("stripped_cherry_wood"), None if abs(o) <= 1 else {"axis": axis})
            if abs(o) == 2:
                lamp = i % 6 == 3
                s.set(x, y + 1, z, mc("cherry_fence"), fence(axis == "x"))
                if lamp:
                    s.set(x, y + 2, z, "guhs:lampion_mint", {"hanging": "false", "waterlogged": "false"})
        if i % 4 == 1:                          # little clouds under the deck
            x, z = (t, fixed) if axis == "x" else (fixed, t)
            s.set(x, y - 1, z, mc("white_wool"))


def waterfall(h, s, x, y, z, out, pool_y, pool):
    """A kaassaus spout at (x, y, z) sticking out towards `out` ((dx, dz)), falling into a pool (cells at pool_y)."""
    mc = h.mc
    dx, dz = out
    side = (dz, dx)
    s.set(x - dx, y, z - dz, mc("pink_terracotta"))           # the wall behind it
    s.set(x + dx, y, z + dz, mc("pink_terracotta"))           # the lip in front
    for sgn in (1, -1):
        s.set(x + side[0] * sgn, y, z + side[1] * sgn, mc("pink_terracotta"))
    s.set(x, y, z, FLUID, {"level": "0"})
    for yy in range(pool_y + 1, y):
        s.set(x, yy, z, FLUID, {"level": "8"})
    for (px, pz) in pool:
        s.set(px, pool_y, pz, FLUID, {"level": "0"})
        s.set(px, pool_y - 1, pz, mc("pink_terracotta"))
    for (px, pz) in pool:
        for ex, ez in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (px + ex, pz + ez)
            if q not in pool and s.get(q[0], pool_y, q[1]) in (None, FLUID):
                s.set(q[0], pool_y, q[1], mc("quartz_block"))


def template(h):
    mc = h.mc
    s = h.Structure((W, H, D))
    rng = random.Random(1717)
    guh_nbt = h.ms.guh_nbt

    # --- the plaza: a guh face on the ground, the two lifts, lampgions ------------------------------------------
    face_mat = {"skin": "pink_concrete", "ear": "pink_concrete", "ear_in": "magenta_concrete", "eye": "black_concrete",
                "shine": "white_concrete", "nose": "magenta_concrete", "mouth": "black_concrete", "cheek": "pink_terracotta"}
    fp = []
    for x in range(C - PLAZA_R - 1, C + PLAZA_R + 2):
        for z in range(C - PLAZA_R - 1, C + PLAZA_R + 2):
            d = math.dist((x, z), (C, C))
            if d > PLAZA_R + 0.4:
                continue
            fp.append((x, z))
            role = face_role(x - C, -(z - 48), 11)
            if role:
                s.set(x, 0, z, mc(face_mat[role]))
            elif d > PLAZA_R - 1.1:
                ang = int(math.degrees(math.atan2(z - C, x - C)) + 360) // 15
                s.set(x, 0, z, mc("magenta_concrete") if ang % 2 else mc("pink_terracotta"))
            else:
                s.set(x, 0, z, mc("white_concrete"))
    for (x, z) in UP_COL:
        s.set(x, 0, z, LIFT, {"facing": "south", "down": "false"})
        for y in range(1, TOP + 1):
            s.set(x, y, z, STREAM, {"facing": "south", "down": "false"})
    for (x, z) in DOWN_COL:
        s.set(x, 0, z, LIFT, {"facing": "north", "down": "true"})
        for y in range(1, TOP + 1):
            s.set(x, y, z, STREAM, {"facing": "north", "down": "true"})
    for k in range(8):
        a = k / 8 * 2 * math.pi
        lampion(s, round(C + math.cos(a) * (PLAZA_R - 0.6)), 0, round(C + math.sin(a) * (PLAZA_R - 0.6)), ("roze", "mint")[k % 2])
    for (x, z, f) in ((C, C + 8, "north"), (C + 8, C + 4, "west"), (C - 5, C + 8, "north")):
        s.set(x, 1, z, "guhs:guh_bank", {"facing": f})
    s.clear_above(fp, 1)

    # --- the islands --------------------------------------------------------------------------------------------
    cells = {name: island(h, s, rng, name) for name in ISLANDS}
    main = cells["main"]

    # the landing pier from the up lift, with a guh-face gate
    for x in range(48, 53):
        for z in range(52, 61):
            s.set(x, S0, z, "guhs:guhbloesem_planks" if 49 <= x <= 51 else mc("stripped_cherry_wood"), None if 49 <= x <= 51 else {"axis": "z"})
        s.set(x, S0 - 1, 54, mc("white_wool"))
        s.set(x, S0 - 1, 57, mc("white_wool"))
    for z in range(52, 58):
        for x in (48, 52):
            s.set(x, S0 + 1, z, mc("cherry_fence"), fence(False))
    for x in (48, 52):
        s.set(x, S0 + 2, 52, "guhs:lampion_roze", {"hanging": "false", "waterlogged": "false"})
    gz, gy, R = 59, S0 + 7, 7
    gate_mat = {"skin": "pink_wool", "ear": "pink_wool", "ear_in": "magenta_wool", "eye": "black_concrete",
                "shine": "white_concrete", "nose": "magenta_concrete", "mouth": "black_concrete", "cheek": "pink_terracotta"}
    for x in range(C - 10, C + 11):
        for y in range(S0 + 1, S0 + 18):
            role = face_role(x - C, y - gy, R)
            if role:
                s.set(x, y, gz, mc(gate_mat[role]))
    for x in range(C - 1, C + 2):                    # the mouth is the door
        for y in range(S0 + 1, S0 + 4):
            s.set(x, y, gz, mc("air"))
    for x in range(C - 5, C + 6):                   # the gate stands on solid ground
        if s.get(x, S0 + 1, gz) not in (None, "minecraft:air"):
            s.set(x, S0, gz, mc("white_wool") if s.get(x, S0, gz) is None else s.get(x, S0, gz))

    # the walkway to the down lift
    for x in range(37, 42):
        for z in range(57, 64):
            if s.get(x, S0, z) is None or abs(x - 39) <= 1:
                s.set(x, S0, z, "guhs:guhbloesem_planks" if abs(x - 39) <= 1 else mc("stripped_cherry_wood"), None if abs(x - 39) <= 1 else {"axis": "z"})
        s.set(x, S0 - 1, 59, mc("white_wool"))
    for z in range(57, 62):
        for x in (37, 41):
            s.set(x, S0 + 1, z, mc("cherry_fence"), fence(False))
    for x in (37, 41):
        s.set(x, S0 + 2, 57, "guhs:lampion_mint", {"hanging": "false", "waterlogged": "false"})

    # paths on the main island: gate -> nest -> the bridges and the fountain
    def path(x0, x1, z0, z1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if (x, z) in main:
                    lamp = (x + z) % 5 == 0 and (x in (x0, x1) or z in (z0, z1))
                    s.set(x, S0, z, "guhs:guh_kristal_lamp" if lamp else mc("white_concrete") if (x + z) % 2 else mc("pink_concrete"))
    path(49, 51, 60, 70)
    path(33, 46, 71, 73)
    path(54, 67, 79, 81)
    path(49, 51, 78, 81)

    # the nest of the Wolkguh (and the Wolkguh itself)
    nx, nz = 50, 74
    for x in range(nx - 4, nx + 5):
        for z in range(nz - 4, nz + 5):
            d = math.dist((x, z), (nx, nz))
            if d <= 2.6:
                s.set(x, S0, z, mc("white_wool"))
                s.set(x, S0 + 1, z, mc("white_carpet"))
            elif d <= 3.6:
                s.set(x, S0, z, mc("white_wool"))
                opening = abs(x - nx) <= 1 or abs(z - nz) <= 1
                if not opening:
                    s.set(x, S0 + 1, z, mc("white_wool"))
    s.set(nx - 2, S0 + 1, nz, "guhs:white_kussen", {"facing": "east"})
    s.set(nx + 2, S0 + 1, nz, "guhs:pink_kussen", {"facing": "west"})
    wolkguh = guh_nbt(1.25, Variant="wolk", ClothesHead="wolkenmuts", ClothesNeck="wolkenkraag", Rotation=h.floats(180.0, 0.0))
    s.entity(nx + 0.5, S0 + 1.07, nz + 0.5, wolkguh)
    for (x, z) in ((44, 68), (56, 68), (44, 80), (56, 80)):
        lampion(s, x, S0, z, "roze")

    # the guh-face pool of kaassaus
    fx, fz, fr = 50, 86, 4
    pool = []
    for x in range(fx - 7, fx + 8):
        for z in range(fz - 7, fz + 8):
            role = face_role(x - fx, -(z - fz), fr)
            d = math.dist((x, z), (fx, fz))
            if role in ("skin", "cheek"):
                pool.append((x, z))
                s.set(x, S0, z, FLUID, {"level": "0"})
                s.set(x, S0 - 1, z, mc("quartz_block"))
            elif role:
                s.set(x, S0, z, mc({"ear": "pink_wool", "ear_in": "magenta_wool", "eye": "black_concrete", "shine": "white_concrete",
                                     "nose": "magenta_concrete", "mouth": "black_concrete"}[role]))
            elif d <= fr + 2.3 and (x, z) in main:
                s.set(x, S0, z, mc("quartz_block"))
    for (x, z) in pool:                               # everything around the saus is solid
        for ex, ez in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if s.get(x + ex, S0, z + ez) in (None,):
                s.set(x + ex, S0, z + ez, mc("quartz_block"))

    # guhbloesem trees and benches
    for (x, z) in ((39, 80), (62, 70), (42, 86), (58, 86)):
        h.blossom_tree(s, rng, x, S0 + 1, z)
    s.set(45, S0 + 1, 86, "guhs:guh_bank", {"facing": "east"})
    s.set(55, S0 + 1, 86, "guhs:guh_bank", {"facing": "west"})

    # --- the Guhrots: a giant guh head drooling kaassaus, with a treasure cave in its mouth ------------------------
    gx, gzc, S1 = 12, 72, ISLANDS["guhrots"][3]
    hx, hy, hz = gx, S1 + 7, gzc
    rx, ry, rzz = 5.2, 7.5, 6.2
    head = set()
    for x in range(hx - 7, hx + 8):
        for y in range(S1 + 1, S1 + 16):
            for z in range(hz - 8, hz + 9):
                # a round head on a wide chin: below the middle it hardly narrows (so it sits firmly on the island)
                squash = 1.0 if y >= hy else 0.3
                if ((x - hx) / rx) ** 2 + ((y - hy) / ry) ** 2 * squash + ((z - hz) / rzz) ** 2 <= 1:
                    head.add((x, y, z))
    for sz in (-1, 1):                                 # ears
        ex, ey, ez = hx - 1, S1 + 14, hz + sz * 4.5
        for x in range(ex - 3, ex + 4):
            for y in range(ey - 3, ey + 4):
                for z in range(int(ez) - 3, int(ez) + 4):
                    if math.dist((x, y, z), (ex, ey, ez)) <= 2.6:
                        head.add((x, y, z))
    front = {}
    for (x, y, z) in head:
        front[(y, z)] = max(front.get((y, z), -99), x)
    for (x, y, z) in head:
        s.set(x, y, z, mc("pink_terracotta") if (x + y + z) % 5 else mc("magenta_terracotta"))
    for (y, z), x in front.items():                   # the face, on the side looking at the main island (+x)
        dy, dz = y - hy, z - hz
        if y >= S1 + 13:
            ear_in = abs(dz) >= 3 and math.dist((dy, abs(dz)), (7, 4.5)) <= 1.3
            if ear_in:
                s.set(x, y, z, mc("magenta_concrete"))
            continue
        if math.dist((dy, abs(dz)), (3, 2.6)) <= 1.35:
            s.set(x, y, z, mc("black_concrete"))
            if (dy, abs(dz)) == (4, 2):
                s.set(x, y, z, mc("white_concrete"))
        elif math.dist((dy, abs(dz)), (-1.5, 4.2)) <= 1.1:
            s.set(x, y, z, mc("magenta_concrete"))      # cheeks
        elif (dy, dz) in ((0, 0), (1, 0)):
            s.set(x, y, z, mc("magenta_concrete"))      # nose
        elif -1 >= dy >= -3 and abs(dz) <= 3.2:
            s.set(x, y, z, mc("pink_concrete"))         # the snout around the mouth
    # the cave inside (a hollow head) with the treasure
    for (x, y, z) in head:
        room = ((x - hx) / (rx - 1.6)) ** 2 + ((z - hz) / (rzz - 1.6)) ** 2 <= 1
        if room and (y <= S1 + 3 or ((y - hy) / (ry - 1.6)) ** 2 + ((x - hx) / (rx - 1.6)) ** 2 + ((z - hz) / (rzz - 1.6)) ** 2 <= 1):
            s.set(x, y, z, mc("air"))
    mouth_x = front[(S1 + 2, hz)]
    for x in range(hx - 1, mouth_x + 2):              # the mouth: a doorway into the cave
        for y in range(S1 + 1, S1 + 4):
            for z in range(hz - 1, hz + 2):
                s.set(x, y, z, mc("air"))
    for x in range(hx - 4, hx + 5):
        for z in range(hz - 5, hz + 6):
            if (x, z) in cells["guhrots"] and s.get(x, S1 + 1, z) in ("minecraft:air",):
                s.set(x, S1, z, mc("cherry_planks"))
    h.ms.chest(s, hx - 3, S1 + 1, hz, "east", f"guhs:chests/{NAME}")
    s.set(hx - 3, S1 + 1, hz - 2, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    s.set(hx - 3, S1 + 1, hz + 2, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    # it drools kaassaus from both corners of its mouth into two pools
    for sz in (-1, 1):
        z = hz + sz * 3
        fx_ = front[(S1 + 5, z)] + 1
        waterfall(h, s, fx_, S1 + 5, z, (1, 0), S1, [(fx_, z), (fx_ + 1, z), (fx_, z + sz), (fx_ + 1, z + sz)])
    lampion(s, 22, S1, 66, "geel")
    lampion(s, 22, S1, 78, "geel")
    # the bridge from the main island
    bridge(h, s, rng, "x", 72, 33, 26, S0, S1)

    # --- the Bloesemeiland: a picnic under guhbloesem trees ---------------------------------------------------------
    bx, bz, _, S2, _ = ISLANDS["bloesem"]
    for (x, z) in ((79, 76), (89, 76), (86, 86)):
        h.blossom_tree(s, rng, x, S2 + 1, z)
    s.set(bx, S2 + 1, bz + 1, "guhs:guh_tafel", {"facing": "north"})
    s.set(bx - 1, S2 + 1, bz + 1, "guhs:guh_stoel", {"facing": "east"})
    s.set(bx + 1, S2 + 1, bz + 1, "guhs:guh_stoel", {"facing": "west"})
    s.set(bx, S2 + 1, bz + 3, "guhs:pink_zitzak", {"facing": "north"})
    lampion(s, bx - 3, S2, bz - 3, "roze")
    lampion(s, bx + 4, S2, bz + 4, "mint")
    bridge(h, s, rng, "x", 80, 67, 75, S0, S2)

    # --- the Kaaswolkje: a kaasknabbel boulder with a waterfall, reached by the long bridge north --------------------
    kx, kz, _, S3, _ = ISLANDS["kaaswolk"]
    boulder = set()
    for x in range(kx - 4, kx + 5):
        for y in range(S3 + 1, S3 + 8):
            for z in range(kz - 5, kz + 3):
                if ((x - kx) / 3.4) ** 2 + ((y - S3 - 1) / 5.5) ** 2 + ((z - kz + 1) / 3.2) ** 2 <= 1:
                    boulder.add((x, y, z))
    for (x, y, z) in boulder:
        s.set(x, y, z, "guhs:block_of_kaasknabbels")
    sx_ = kx
    sz_ = max(z for (x, y, z) in boulder if x == kx and y == S3 + 4) + 1
    waterfall(h, s, sx_, S3 + 4, sz_, (0, 1), S3, [(sx_, sz_), (sx_, sz_ + 1), (sx_ - 1, sz_ + 1), (sx_ + 1, sz_ + 1)])
    s.set(kx + 3, S3 + 1, kz + 3, "guhs:guh_taart", {"bites": "0"})
    lampion(s, kx - 3, S3, kz + 3, "geel")
    bridge(h, s, rng, "z", 84, 71, 40, S2, S3)

    # --- the Wolkje: a lookout up a trail of cloud stepping stones, with a second chest ----------------------------
    wx, wz, _, S4, _ = ISLANDS["wolkje"]
    stones = []
    for i, (zc, y) in enumerate(((59, S1), (55, S1 + 1), (51, S1 + 2), (47, S1 + 2), (43, S1 + 3), (39, S1 + 4), (35, S1 + 5))):
        xc = 16 + round(i * (wx - 16) / 6)
        stones.append((xc, y, zc))
        for x in range(xc - 1, xc + 2):
            for z in range(zc - 1, zc + 2):
                s.set(x, y, z, mc("white_wool"))
        s.set(xc, y - 1, zc, mc("white_wool"))
        s.set(xc, y - 1, zc - 1, mc("light_blue_wool"))
    for x in range(wx - 2, wx + 3):                   # a little pavilion
        for z in range(wz - 3, wz + 2):
            s.set(x, S4 + 4, z, mc("pink_wool") if (x + z) % 2 else mc("white_wool"))
    for (x, z) in ((wx - 2, wz - 3), (wx + 2, wz - 3), (wx - 2, wz + 1), (wx + 2, wz + 1)):
        for y in range(S4 + 1, S4 + 4):
            s.set(x, y, z, mc("cherry_fence"), {"north": "false", "south": "false", "east": "false", "west": "false",
                                                "waterlogged": "false"})
    s.set(wx, S4 + 3, wz - 1, "guhs:lampion_roze", {"hanging": "true", "waterlogged": "false"})
    s.set(wx, S4 + 5, wz - 1, mc("gold_block"))
    h.ms.chest(s, wx, S4 + 1, wz - 2, "south", f"guhs:chests/{NAME}")
    # (2.9, kleding) the wolkenkist: the only place for the Wolkguh's wolkenmuts and wolkenkraag
    h.ms.chest(s, wx + 1, S4 + 1, wz, "west", f"guhs:chests/{NAME}_wolkenkist")
    s.set(wx - 1, S4 + 1, wz, "guhs:white_zitzak", {"facing": "north"})

    # --- plants last, where nothing else stands --------------------------------------------------------------------
    for name, (cx, cz, r, S, top) in ISLANDS.items():
        decorate(h, s, rng, cells[name], S, top, density=0.12 if name == "wolkje" else 0.28)
    prune(s)
    s.stones = stones
    return s


def solid(block):
    return block not in NOT_FLOOR and block not in PASSABLE


def prune(s):
    """Removes floating leftovers: every group of blocks (not touching the ground layer) smaller than 8 blocks."""
    for comp in components(s):
        if len(comp) < 8 and all(y > 0 for (_, y, _) in comp):
            for p in comp:
                del s.blocks[p]


def components(s):
    real = {p for p, (b, _, _) in s.blocks.items() if b not in ("minecraft:air", STREAM)}
    seen, comps = set(), []
    for p in real:
        if p in seen:
            continue
        comp, todo = [], [p]
        seen.add(p)
        while todo:
            q = todo.pop()
            comp.append(q)
            x, y, z = q
            for n in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
                if n in real and n not in seen:
                    seen.add(n)
                    todo.append(n)
        comps.append(comp)
    return comps


# =====================================================================================================================
# the geometry self-check
# =====================================================================================================================
def check(s):
    problems = []
    get = s.get
    # 1. walk floors: every top block of every island is solid (or a kaassaus pool with a floor)
    for name, (cx, cz, r, S, top) in ISLANDS.items():
        for x in range(cx - r, cx + r + 1):
            for z in range(cz - r, cz + r + 1):
                if math.dist((x, z), (cx, cz)) <= r - 1.5:
                    b = get(x, S, z)
                    if b == FLUID:
                        if not solid(get(x, S - 1, z)):
                            problems.append(f"{name}: pool without floor at {(x, S, z)}")
                    elif not solid(b) and b != "minecraft:air":
                        problems.append(f"{name}: hole in the floor at {(x, S, z)} ({b})")
                    elif b in (None, "minecraft:air") and not solid(get(x, S - 1, z)) and get(x, S + 1, z) in (None, "minecraft:air"):
                        problems.append(f"{name}: hole in the floor at {(x, S, z)}")
    # 2. the lifts: whole columns, pads below, free air at the top where you get off
    for col, down in ((UP_COL, False), (DOWN_COL, True)):
        for (x, z) in col:
            if get(x, 0, z) != LIFT:
                problems.append(f"lift pad missing at {(x, 0, z)}")
            for y in range(1, TOP + 1):
                if get(x, y, z) != STREAM or s.blocks[(x, y, z)][1].get("down") != str(down).lower():
                    problems.append(f"lift column broken at {(x, y, z)}")
            if get(x, TOP + 1, z) not in (None, "minecraft:air"):
                problems.append(f"something on top of the lift at {(x, TOP + 1, z)}")
    for x in range(49, 52):                          # the landing: pier right next to the up lift, nothing in the way
        if not solid(get(x, S0, 52)):
            problems.append(f"no pier next to the lift at {(x, S0, 52)}")
        for y in range(S0 + 1, S0 + 5):
            for z in range(52, 56):
                if get(x, y, z) not in (None, "minecraft:air") and get(x, y, z) not in PASSABLE:
                    problems.append(f"blocks the landing: {get(x, y, z)} at {(x, y, z)}")
    for x in range(38, 41):                          # the way down: walkway right next to the down lift
        if not solid(get(x, S0, 57)):
            problems.append(f"no walkway next to the down lift at {(x, S0, 57)}")
    # 3. walking (with jumps over the stepping stones) from the landing to everything that matters
    reach = walk(s, (50, S0 + 1, 54))
    wolk = [e for e in s.entities if e[3].get("Variant") == "wolk"]
    if len(wolk) != 1:
        problems.append(f"{len(wolk)} Wolkguhs (should be exactly 1)")
    for (ex, ey, ez, _) in wolk:
        p = (int(ex), int(ey), int(ez))
        below = get(p[0], p[1] - 1, p[2])
        if not solid(below) or get(*p) not in PASSABLE:
            problems.append(f"the Wolkguh doesn't stand on solid ground at {p} ({below})")
        if p not in reach:
            problems.append("the Wolkguh can't be reached on foot")
    targets = {"down lift walkway": (39, S0 + 1, 58)}
    for name, (cx, cz, r, S, top) in ISLANDS.items():
        spot = next((p for p in sorted(reach) if p[1] == S + 1 and math.dist((p[0], p[2]), (cx, cz)) <= r - 2), None)
        if spot is None:
            problems.append(f"island {name} can't be reached on foot")
    for (x, y, z), (b, _, _) in s.blocks.items():
        if b == "minecraft:chest" and not any((x + dx, y, z + dz) in reach for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            problems.append(f"chest at {(x, y, z)} can't be reached")
    for name, p in targets.items():
        if p not in reach:
            problems.append(f"{name} {p} can't be reached")
    plaza = walk(s, (C, 1, C + 6))
    for p in ((C, 1, C + 2), (39, 1, 57)):
        if p not in plaza:
            problems.append(f"the lift next to {p} can't be reached from the plaza")
    # 4. nothing floating, everything that needs support has it
    for comp in components(s):
        if len(comp) < 8 and all(y > 0 for (_, y, _) in comp):
            problems.append(f"floating blocks: {sorted(comp)[:3]}...")
    for (x, y, z), (b, props, _) in s.blocks.items():
        if b in NEEDS_SUPPORT and not (props.get("hanging") == "true"):
            below = get(x, y - 1, z)
            if not solid(below) and below != "minecraft:cherry_fence":
                problems.append(f"{b} at {(x, y, z)} stands on {below}")
        if b == "guhs:lampion_roze" and props.get("hanging") == "true" and not solid(get(x, y + 1, z)):
            problems.append(f"hanging lampgion at {(x, y, z)} hangs from nothing")
    # 5. kaassaus stays where it is: sources closed in on all sides, falling saus lands in saus
    for (x, y, z), (b, props, _) in s.blocks.items():
        if b != FLUID:
            continue
        if props.get("level") == "0":
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = get(x + dx, y, z + dz)
                if n != FLUID and not solid(n):
                    problems.append(f"kaassaus leaks at {(x, y, z)} towards {(x + dx, y, z + dz)} ({n})")
            below = get(x, y - 1, z)
            if below != FLUID and not solid(below):
                problems.append(f"kaassaus source at {(x, y, z)} over {below}")
        else:
            if get(x, y - 1, z) != FLUID:
                problems.append(f"falling kaassaus at {(x, y, z)} lands on {get(x, y - 1, z)}")
    # 6. the sky part really is above ISLANDS_FROM (the protection counts on it)
    low = min(y for (x, y, z), (b, _, _) in s.blocks.items() if y > 5 and b not in (STREAM, "minecraft:air"))
    if low < ISLANDS_FROM:
        problems.append(f"an island hangs down to y={low}, below ISLANDS_FROM={ISLANDS_FROM}")
    return problems


def walk(s, start):
    """Every feet position you can walk to from start: steps of 1 up / 3 down, and jumps of 2-3 blocks (1 up at most)."""
    get = s.get

    def standable(p):
        x, y, z = p
        if not (0 <= x < W and 0 <= z < D and 1 <= y < H - 1):
            return False
        return get(x, y, z) in PASSABLE and get(x, y + 1, z) in PASSABLE and solid(get(x, y - 1, z)) \
            and get(x, y, z) != STREAM

    seen = {start} if standable(start) else set()
    todo = deque(seen)
    while todo:
        x, y, z = todo.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (1, 0, -1, -2, -3):
                n = (x + dx, y + dy, z + dz)
                if dy == 1 and get(x, y + 2, z) not in PASSABLE:
                    continue
                if dy < 0 and any(get(x + dx, y + k, z + dz) not in PASSABLE for k in range(dy, 2)):
                    continue
                if n not in seen and standable(n):
                    seen.add(n)
                    todo.append(n)
                    break
            for dist in (2, 3, 4):                    # jumps (over air only)
                gap = [(x + dx * k, y, z + dz * k) for k in range(1, dist)]
                if any(get(*g) not in PASSABLE or get(g[0], g[1] + 1, g[2]) not in PASSABLE or get(g[0], g[1] + 2, g[2]) not in PASSABLE
                       for g in gap):
                    break
                if any(solid(get(g[0], g[1] - 1, g[2])) for g in gap):
                    continue                          # (that's just walking)
                for dy in (1, 0, -1, -2):
                    if dist == 4 and dy > 0:
                        continue
                    n = (x + dx * dist, y + dy, z + dz * dist)
                    if n not in seen and standable(n):
                        seen.add(n)
                        todo.append(n)
                        break
    return seen


# =====================================================================================================================
# FTB Quests (row y=40)
# =====================================================================================================================
def ftb(fq):
    fq.q("eilanden_vind", "Zwevende guh-eilandjes",
         "Heel hoog in de lucht van de Guhmensie zweven de &bzwevende guh-eilandjes&r. Heel zeldzaam! Het &dGuhmensie-superkompas&r (Wonderen > Zwevende guh-eilandjes) wijst de weg.",
         "guhs:guhmensie_superkompas", [fq.structure(NAME)], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=-8, y=40, shape="gear", xp=300)
    fq.q("eilanden_lift", "VAHOEG, de lucht in!",
         "Onder de eilandjes ligt een plein met een guhgezicht. Ga op de &bwolkenlift&r (de neus!) staan: hij blaast je omhoog. Naar beneden zweef je met de blauwe wolkenlift aan het eind van de loopplank.",
         "guhs:wolkenlift", [fq.adv("eilanden_lift")], rewards=(("guhs:wolkenlift", 2),), x=-6, y=40, xp=100)
    fq.q("eilanden_zien", "Wat een wolkje!",
         "Op het grote eiland, in een nest van wol, woont de &bWolkguh&r: de enige in de hele Guhmensie. Ga naast hem staan voor zijn Guhdex-pagina.",
         "guhs:guhdex", [fq.adv("seen_wolk")], rewards=(("guhs:kaas_knabbels", 16),), x=-4, y=40, xp=100)
    fq.q("eilanden_gevoerd", "Vadsig wolkje",
         "De Wolkguh vertrouwt je pas als hij vads genoeg is: voer hem minstens &68 kaasknabbels&r.",
         "guhs:kaas_knabbels", [fq.adv("eilanden_gevoerd")], rewards=(("guhs:kaas_knabbels", 16),), x=-2, y=40, xp=150)
    fq.q("eilanden_tem", "Op wolkje negen",
         "Blijf de Wolkguh voeren tot hij van jou is. Een getemde Wolkguh vangt je op als je valt!",
         "guhs:wolkenmuts", [fq.adv("tamed_wolk")], rewards=(("guhs:gefrituurde_kaasknabbels", 16),), x=0, y=40, shape="gear", xp=500)
    fq.q("eilanden_pakje", "Met je hoofd in de wolken",
         "Kleed je getemde Wolkguh uit in zijn kledingkast: de &bwolkenmuts&r en de &bwolkenkraag&r zijn nu van jou (en van je andere guhs).",
         "guhs:wolkenkraag", [fq.item("guhs:wolkenmuts"), fq.item("guhs:wolkenkraag")], rewards=(("guhs:guh_kristal", 8),), x=2, y=40, xp=200)
    fq.q("eilanden_opgevangen", "Zacht geland",
         "Spring ergens vanaf (minstens 6 blokken) terwijl je getemde Wolkguh in de buurt is. Poef: hij vangt je op!",
         "minecraft:feather", [fq.adv("eilanden_opgevangen")], rewards=(("guhs:wolkensuikerspin", 4),), x=4, y=40, xp=150)
    fq.q("eilanden_schat", "Wolkensuikerspin",
         "In de mond van de Guhrots (het reuzenguhhoofd dat kaassaus kwijlt) en op het hoogste wolkje staat een schatkist. Zoek een &bwolkensuikerspin&r: daarmee zweef je overal zachtjes naar beneden.",
         "guhs:wolkensuikerspin", [fq.item("guhs:wolkensuikerspin")], rewards=(("guhs:vahoege_vads_ingot", 2),), x=6, y=40, xp=200)


if __name__ == "__main__":
    # the self-check on its own:  python tools/features/eilanden.py   (from the project root)
    import os
    import sys
    import types
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    import make_structures as ms
    stub = types.SimpleNamespace(mc=ms.mc, Structure=ms.Structure, ms=ms, floats=ms.floats)
    import make_v2
    stub.blossom_tree = make_v2.blossom_tree
    tpl = template(stub)
    found = check(tpl)
    print("\n".join(found) if found else "geometry check ok")
    print(f"{len(tpl.blocks)} blocks, {len(tpl.entities)} entities")
