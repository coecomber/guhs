"""
Het Vadswoud (slice 5 of 2.7.0): a misty mint-green forest of giant guh trees in the Guhmension, and the guh families.

  - the biome guhs:vadswoud (multi_noise point temperature -0.15, humidity 0.55, erosion [-0.25, 0.25]): lavender sky,
    mint mist (thicker fog: feature/vadswoud/client/VadswoudClient.java), drifting glowing vadspluisjes, vadsmos ground
  - reuzenguhbomen (the Java feature guhs:reuzenguhboom): thick round trunks with root flares, branches, huge canopies
    and little guh faces in the bark (the block vadshout_gezicht, four moods); smaller vadshout trees (vanilla tree JSON)
  - the wood set vadshout_*: stam, gestript, planken, trap, plaat, hek, poort, deur, luik (+ bladeren, zaailing,
    gezicht) and vadstouw (rope, for rope bridges; climbable)
  - knabbelbessenstruik (a sweet berry bush that never pricks) and knabbelbessen (food; wild babyguhs love them)
  - guhnestje: guh families sleep in it at night (wild and tame guhs); found in the forest, craftable
  - the structure boomhutdorp: a big tree-house village high in five giant guh trees (a spiral stair, ladders, rope
    bridges, guh-head huts with faces on every side, a guh face in the Moederboom), with the Boswachterguh (wood,
    saplings, nests, the ranger outfit) and the Knabbelplukker (trades knabbelbessen, the picker's outfit)
  - guh clothes: boswachtershoed, boswachtersjas, plukmuts, plukmandje
  - advancements, FTB quests (rows y=68 and y=70), Guhdex pages, lang (Dutch everywhere)

check(s) is the geometry self-check of the village template: everything is reachable on foot (stairs, ladders and
bridges) from the edge, huts have a way in, NPCs/guhs/nests/chests stand on solid floors, ladders hang on something,
nothing floats, lamps hang or stand on something. build() fails loudly (SystemExit) when it finds a problem.
Run it on its own:  python tools/features/vadswoud.py   (from the project root)
"""
import json
import math
import os
import random
import zipfile
import io
from collections import deque

NAME = "boomhutdorp"
BIOME = "vadswoud"
W, H, D = 100, 82, 100
C = 50                      # the middle (x and z): the Moederboom
G = 4                       # the ground's top block (roots go below it)
ANCHOR = "guhs:boomhutdorp_midden"

# the wood set: our block -> its vanilla oak counterpart (blockstates, models and loot are copied from vanilla)
WOOD = {"vadshout_trap": "oak_stairs", "vadshout_plaat": "oak_slab", "vadshout_hek": "oak_fence",
        "vadshout_poort": "oak_fence_gate", "vadshout_deur": "oak_door", "vadshout_luik": "oak_trapdoor"}
LOGS = ["vadshout_stam", "vadshout_gestript", "vadshout_gezicht"]
BLOCKS = ["vadshout_stam", "vadshout_gestript", "vadshout_planken", *WOOD, "vadshout_bladeren", "vadshout_zaailing",
          "vadshout_gezicht", "vadsmos", "guhnestje", "knabbelbessenstruik", "vadstouw"]
CLOTHES = ["boswachtershoed", "boswachtersjas", "plukmuts", "plukmandje"]
PARTICLES = {"vadspluisje": 4, "vadsblaadje": 3, "nestje_zzz": 1}

MINT = (150, 222, 190)
LAVENDER = (190, 170, 235)


# =====================================================================================================================
# guh clothes (make_guh_variants.py / make_clothes_icons.py)
# =====================================================================================================================
_H = [0, 6, -2]   # head pivot
BONES = {
    # the ranger hat: a wide brim, a round crown with a dent and a leafy band
    "outfit_boswachtershoed": ("head", _H, "boswachtershoed", [([-6.5, 15.2, -11.5], [13, 0.6, 11], 0), ([-3.8, 15.8, -9.2], [7.6, 2.6, 6.4], 0),
                                                              ([-3.2, 18.4, -8.6], [6.4, 0.8, 5.2], 0), ([-4, 15.8, -9.4], [8, 0.8, 6.8], 0.05)]),
    # the picker's cap: a round knitted cap with a knabbelbes on a little stalk
    "outfit_plukmuts": ("head", _H, "plukmuts", [([-4.2, 15, -9.4], [8.4, 2.4, 7], 0.05), ([-3.2, 17.4, -8.6], [6.4, 1.2, 5.4], 0),
                                                 ([-0.4, 18.6, -6.6], [0.8, 1.4, 0.8], 0), ([-1, 19.8, -7.2], [2, 2, 2], 0)]),
    # the picker's basket on its back: a woven basket with a handle, full of knabbelbessen
    "outfit_plukmandje": ("body", [0, 6, 6], "plukmandje", [([-3.8, 11.2, 1.2], [7.6, 4.2, 6.4], 0), ([-3.3, 15.4, 1.7], [6.6, 0.6, 5.4], 0),
                                                            ([-4, 15.4, 4.1], [0.6, 2.4, 0.6], 0), ([3.4, 15.4, 4.1], [0.6, 2.4, 0.6], 0),
                                                            ([-4, 17.4, 4.1], [8, 0.6, 0.6], 0)]),
}


def clothes(rng, v):
    np = __import__("numpy")

    def hoed():
        a = v.fabric((110, 150, 90), rng, 8)
        a[12:17, :] = (70, 105, 60)                   # the band
        for x in range(2, 32, 7):
            a[13:16, x:x + 2] = (240, 150, 190)       # little pink blossoms in the band
        return a

    def jas():
        a = v.fabric((96, 132, 84), rng, 8)
        a[:, 14:18] = (70, 100, 60)                   # the zip
        for y in (6, 20):
            a[y:y + 5, 4:10] = (130, 160, 100)        # pockets
            a[y:y + 5, 22:28] = (130, 160, 100)
        a[0:3, :] = (200, 170, 110)                   # a sandy collar
        return a

    def muts():
        a = v.stripes((230, 120, 60), (250, 190, 70), rng, width=3)
        a[26:32, :] = (240, 230, 210)                 # the rim
        return a

    def mand():
        a = v.fabric((190, 140, 80), rng, 6)
        for y in range(0, 32, 4):                     # woven
            for x in range((y // 4 % 2) * 2, 32, 4):
                a[y:y + 2, x:x + 2] = (150, 100, 55)
        for _ in range(12):                            # knabbelbessen peeking out
            x, y = rng.integers(1, 30, 2)
            a[y:y + 2, x:x + 2] = (250, 196, 50)
        return np.clip(a, 0, 255)

    return {"boswachtershoed": {"boswachtershoed": hoed},
            "boswachtersjas": {"suit": jas},
            "plukmuts": {"plukmuts": muts},
            "plukmandje": {"plukmandje": mand}}


def icons(ic):
    hoed = ic.icon(ic.pad(["....aaaaaaa.....", "...abbbbbbba....", "...abbbbbbba....", "..accpcpcpcca...",
                           "aaaaaaaaaaaaaaa.", ".aaaaaaaaaaaaa.."]),
                   {"a": (60, 90, 50), "b": (110, 150, 90), "c": (70, 105, 60), "p": (240, 150, 190)})
    muts = ic.icon(ic.pad([".......g........", "......yy........", "......yy........", "....aaaaaa......", "...abcbcbcba....",
                           "..abcbcbcbcba...", "..abbbbbbbbba...", "..wwwwwwwwwww..."]),
                   {"a": (170, 80, 30), "b": (230, 120, 60), "c": (250, 190, 70), "w": (240, 230, 210), "y": (250, 196, 50),
                    "g": (90, 160, 90)})
    mand = ic.icon(ic.pad(["....aaaaaaa.....", "...a.......a....", "...a.......a....", "..ayyryyryya.....", ".abcbcbcbcbca...",
                           ".acbcbcbcbcba...", ".abcbcbcbcbca...", "..aaaaaaaaaa...."]),
                   {"a": (110, 70, 35), "b": (190, 140, 80), "c": (150, 100, 55), "y": (250, 196, 50), "r": (230, 120, 60)})
    return {"boswachtershoed": hoed,
            "boswachtersjas": ic.shirt((96, 132, 84), (60, 90, 50), (200, 170, 110), "buttons"),
            "plukmuts": muts,
            "plukmandje": mand}


# =====================================================================================================================
# resources (make_v2)
# =====================================================================================================================
def build(h):
    textures(h)
    blocks_and_items(h)
    wood_set(h)
    worldgen(h)
    npcs(h)
    advancements(h)
    texts(h)
    # the village: only in the Vadswoud, anchored in the middle (the Moederboom) at ground level
    h.TEMPLATE_SIZES[NAME] = 32
    h.FLATNESS[NAME] = 20
    h.KEEP_CLEAR[NAME] = 44
    none = {"bounding_box": "piece", "spawns": []}
    h.structure(NAME, [BIOME], spacing=16, separation=6, salt=20270501, start_y=-G, reach=80, centre=ANCHOR,
                spawn_overrides={"monster": none, "ambient": none})
    s = template(h)
    problems = check(s)
    if problems:
        raise SystemExit("boomhutdorp geometry check failed:\n  " + "\n  ".join(problems[:60]))
    print(f"boomhutdorp: geometry check ok ({s.walkable} walkable spots, {len(s.blocks)} blocks, {len(s.entities)} entities)")
    s.save(NAME)
    test_templates(h)
    selfcheck_assets(h)


def test_templates(h):
    """Rooms for the game tests: a fenced meadow (families, nests) and open ground for growing a giant tree."""
    mc = h.mc
    weide = h.Structure((26, 4, 9))
    for x in range(26):
        for z in range(9):
            weide.set(x, 0, z, "guhs:vadsmos")
            if x in (0, 25) or z in (0, 8):
                weide.set(x, 1, z, "guhs:vadshout_hek", {"north": str(z not in (0,) and x in (0, 25)).lower(), "south": str(z != 8 and x in (0, 25)).lower(),
                                                         "east": str(x != 25 and z in (0, 8)).lower(), "west": str(x != 0 and z in (0, 8)).lower(),
                                                         "waterlogged": "false"})
    weide.save("vadswoud_weide")
    grond = h.Structure((33, 48, 33))
    for x in range(33):
        for z in range(33):
            grond.set(x, 0, z, "guhs:vadsmos")
    grond.save("vadswoud_boomgrond")


def _jar():
    return zipfile.ZipFile(os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar"))


def vanilla_json(path):
    with _jar() as z:
        return json.loads(z.read(path))


def paint_face(img, mood, x0=0, y0=0):
    """A little guh face on a 16x16 bark texture: 0 happy, 1 sleepy, 2 surprised, 3 vads (big cheeks, tongue)."""
    px = img.load()
    k, w, p, m, t = (38, 26, 44, 255), (255, 255, 255, 255), (242, 150, 188, 255), (70, 22, 50, 255), (240, 110, 150, 255)
    ring = (226, 190, 172, 255)
    for (x, y) in [(xx, yy) for xx in range(2, 14) for yy in range(3, 14)]:   # a smooth "knot" of lighter bark behind the face
        if ((x - 7.5) / 6.2) ** 2 + ((y - 8.3) / 5.8) ** 2 <= 1:
            px[x0 + x, y0 + y] = tuple(min(255, int(c * 0.35 + r * 0.65)) for c, r in zip(px[x0 + x, y0 + y][:3], ring[:3])) + (255,)
    if mood == 1:                                               # sleepy: closed eyes
        for x in (4, 5, 6, 9, 10, 11):
            px[x0 + x, y0 + 7] = k
    else:
        big = mood == 2
        for ex in (4, 9):
            for dx in range(3):
                for dy in range(3 + big):
                    px[x0 + ex + dx, y0 + 5 + dy] = k
            px[x0 + ex + 1, y0 + 5] = w                          # the shine
    cheeks = [(3, 9), (4, 9), (11, 9), (12, 9)] + ([(3, 10), (4, 10), (11, 10), (12, 10), (2, 9), (13, 9)] if mood == 3 else [])
    for (x, y) in cheeks:
        px[x0 + x, y0 + y] = p
    px[x0 + 7, y0 + 9] = px[x0 + 8, y0 + 9] = (210, 110, 150, 255)   # the nose
    if mood == 2:                                               # "o"
        for (x, y) in ((7, 11), (8, 11), (6, 12), (9, 12), (7, 13), (8, 13)):
            px[x0 + x, y0 + y] = m
    else:
        for (x, y) in ((5, 11), (6, 12), (7, 12), (8, 12), (9, 12), (10, 11)):
            px[x0 + x, y0 + y] = m
        if mood == 3:
            px[x0 + 7, y0 + 13] = px[x0 + 8, y0 + 13] = t


def textures(h):
    Image = h.Image
    rng = random.Random(5150)
    bark_dark, bark_light = (76, 46, 60), (182, 128, 142)
    bark = h.ramp(h.vanilla("block/dark_oak_log"), bark_dark, bark_light).copy()
    h.save(bark, "block", "vadshout_stam.png")
    h.save(h.ramp(h.vanilla("block/dark_oak_log_top"), (96, 58, 70), (226, 176, 176)), "block", "vadshout_stam_top.png")
    h.save(h.ramp(h.vanilla("block/stripped_dark_oak_log"), (156, 96, 104), (240, 186, 180)), "block", "vadshout_gestript.png")
    h.save(h.ramp(h.vanilla("block/stripped_dark_oak_log_top"), (160, 100, 106), (242, 192, 184)), "block", "vadshout_gestript_top.png")
    h.save(h.ramp(h.vanilla("block/dark_oak_planks"), (124, 74, 86), (220, 164, 166)), "block", "vadshout_planken.png")
    for mood in range(4):
        face = bark.copy()
        paint_face(face, mood)
        h.save(face, "block", f"vadshout_gezicht_{mood}.png")
    # the door and trapdoor, each with a little guh face in the window
    top = h.ramp(h.vanilla("block/dark_oak_door_top"), (110, 64, 76), (224, 170, 170)).copy()
    px = top.load()
    for (x, y, c) in ((5, 5, (40, 26, 44)), (10, 5, (40, 26, 44)), (4, 7, (242, 150, 188)), (11, 7, (242, 150, 188)),
                      (7, 8, (80, 30, 60)), (8, 8, (80, 30, 60))):
        if px[x, y][3]:
            px[x, y] = c + (255,)
    h.save(top, "block", "vadshout_deur_top.png")
    h.save(h.ramp(h.vanilla("block/dark_oak_door_bottom"), (110, 64, 76), (224, 170, 170)), "block", "vadshout_deur_bottom.png")
    h.save(h.ramp(h.vanilla("item/dark_oak_door"), (110, 64, 76), (224, 170, 170)), "item", "vadshout_deur.png")
    luik = h.ramp(h.vanilla("block/dark_oak_trapdoor"), (110, 64, 76), (224, 170, 170)).copy()
    px = luik.load()
    for (x, y, c) in ((5, 6, (40, 26, 44)), (10, 6, (40, 26, 44)), (7, 9, (80, 30, 60)), (8, 9, (80, 30, 60))):
        if px[x, y][3]:
            px[x, y] = c + (255,)
    h.save(luik, "block", "vadshout_luik.png")
    # the leaves: flowering azalea leaves in mint, with pink blossoms
    greens = lambda hh, s, v: (hh > 0.12) & (hh < 0.5) & (s > 0.1)
    pinks = lambda hh, s, v: ((hh > 0.75) | (hh < 0.05)) & (s > 0.15)
    leaves = h.recolour(h.vanilla("block/flowering_azalea_leaves"), hue=0.43, sat=0.62, val=1.22, only=greens)
    leaves = h.recolour(leaves, hue=0.93, sat=0.75, val=1.12, only=pinks)
    h.save(leaves, "block", "vadshout_bladeren.png")
    sap = h.recolour(h.vanilla("block/dark_oak_sapling"), hue=0.43, sat=0.7, val=1.25, only=greens).copy()
    px = sap.load()
    for (x, y) in ((6, 4), (9, 3), (4, 7), (11, 6), (8, 6)):
        if px[x, y][3]:
            px[x, y] = (246, 160, 200, 255)
    h.save(sap, "block", "vadshout_zaailing.png")
    # vadsmos: mint moss with pink flecks
    mos = h.recolour(h.vanilla("block/moss_block"), hue=0.42, sat=0.6, val=1.18, only=greens).copy()
    px = mos.load()
    for _ in range(14):
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = (244, 160, 198, 255) if rng.random() < 0.7 else (255, 226, 120, 255)
    h.save(mos, "block", "vadsmos.png")
    # the guh nest: straw with a soft pink inside
    straw = h.ramp(h.vanilla("block/hay_block_side"), (170, 116, 62), (250, 214, 146))
    h.save(straw, "block", "guhnestje_zijkant.png")
    h.save(h.ramp(h.vanilla("block/hay_block_top"), (176, 120, 66), (252, 220, 150)), "block", "guhnestje_rand.png")
    inner = h.ramp(h.vanilla("block/pink_wool"), (226, 130, 170), (255, 206, 226)).copy()
    px = inner.load()
    for (x, y) in ((4, 5), (11, 3), (7, 11), (12, 12), (3, 12)):   # a few little feathers
        px[x, y] = (255, 250, 250, 255)
        px[x + 1, y] = (240, 236, 244, 255)
    h.save(inner, "block", "guhnestje_binnen.png")
    # the knabbelbessen bush: mint leaves, cheese-yellow berries (stages 0-3), the berries and the pie
    berries = lambda hh, s, v: ((hh > 0.9) | (hh < 0.06)) & (s > 0.35)
    for i in range(4):
        img = h.recolour(h.vanilla(f"block/sweet_berry_bush_stage{i}"), hue=0.41, sat=0.7, val=1.2, only=greens)
        img = h.recolour(img, hue=0.12, sat=1.0, val=1.25, only=berries)
        h.save(img, "block", f"knabbelbessenstruik_{i}.png")
    img = h.recolour(h.vanilla("item/sweet_berries"), hue=0.41, sat=0.7, val=1.2, only=greens)
    h.save(h.recolour(img, hue=0.12, sat=1.0, val=1.25, only=berries), "item", "knabbelbessen.png")
    pie = h.recolour(h.vanilla("item/pumpkin_pie"), hue=0.93, sat=0.7, val=1.1, only=lambda hh, s, v: (hh > 0.02) & (hh < 0.12) & (s > 0.3)).copy()
    px = pie.load()
    for (x, y) in ((5, 7), (8, 6), (10, 8), (7, 9), (12, 7)):
        if px[x, y][3]:
            px[x, y] = (255, 205, 60, 255)
    h.save(pie, "item", "knabbelbessentaartje.png")
    # the rope: the chain, twisted from brown string
    h.save(h.ramp(h.vanilla("block/chain"), (110, 76, 44), (228, 194, 140)), "block", "vadstouw.png")
    h.save(h.ramp(h.vanilla("item/chain"), (110, 76, 44), (228, 194, 140)), "item", "vadstouw.png")
    # particles: glowing fluff (4 colours), falling mint leaves (3), and the Zzz of a sleeping guh
    for i, col in enumerate(((255, 236, 150), (255, 190, 222), (190, 255, 220), (220, 200, 255))):
        img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
        for y in range(8):
            for x in range(8):
                d = math.dist((x + 0.5, y + 0.5), (4, 4))
                if d < 3.4:
                    a = int(255 * max(0.0, 1 - (d / 3.4) ** 1.5))
                    c = tuple(min(255, int(ch + (255 - ch) * max(0.0, 1 - d / 1.6))) for ch in col)
                    img.putpixel((x, y), c + (a,))
        h.save(img, "particle", f"vadspluisje_{i}.png")
    for i, (a, b) in enumerate((((120, 210, 170), (70, 160, 120)), ((160, 230, 190), (100, 180, 140)), ((246, 170, 206), (210, 110, 160)))):
        img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
        for (x, y) in ((3, 1), (4, 1), (2, 2), (3, 2), (4, 2), (5, 2), (2, 3), (3, 3), (4, 3), (5, 3), (6, 3), (1, 4), (2, 4), (3, 4),
                       (4, 4), (5, 4), (2, 5), (3, 5), (4, 5), (3, 6)):
            img.putpixel((x, y), (a if (x + y) % 3 else b) + (255,))
        h.save(img, "particle", f"vadsblaadje_{i}.png")
    zzz = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    for (x, y) in ((1, 1), (2, 1), (3, 1), (4, 1), (5, 1), (6, 1), (5, 2), (4, 3), (3, 4), (2, 5), (1, 6), (2, 6), (3, 6), (4, 6), (5, 6), (6, 6)):
        zzz.putpixel((x, y), (206, 186, 255, 255))
    h.save(zzz, "particle", "nestje_zzz_0.png")
    for name, n in PARTICLES.items():
        h.w(f"{h.A}/particles/{name}.json", {"textures": [f"guhs:{name}_{i}" for i in range(n)]})


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    # logs
    for name, tex in (("vadshout_stam", "vadshout_stam"), ("vadshout_gestript", "vadshout_gestript")):
        w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cube_column",
                                            "textures": {"end": f"guhs:block/{tex}_top", "side": f"guhs:block/{tex}"}})
        w(f"{A}/models/block/{name}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal",
                                                       "textures": {"end": f"guhs:block/{tex}_top", "side": f"guhs:block/{tex}"}})
        w(f"{A}/blockstates/{name}.json", {"variants": {
            "axis=y": {"model": f"guhs:block/{name}"},
            "axis=z": {"model": f"guhs:block/{name}_horizontal", "x": 90},
            "axis=x": {"model": f"guhs:block/{name}_horizontal", "x": 90, "y": 90}}})
        w(f"{A}/models/item/{name}.json", {"parent": f"guhs:block/{name}"})
    # the guh faces in the bark: a log with a face on the front, four moods (right-click it to change its mood)
    for mood in range(4):
        w(f"{A}/models/block/vadshout_gezicht_{mood}.json", {"parent": "minecraft:block/orientable", "textures": {
            "top": "guhs:block/vadshout_stam_top", "front": f"guhs:block/vadshout_gezicht_{mood}", "side": "guhs:block/vadshout_stam"}})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    w(f"{A}/blockstates/vadshout_gezicht.json", {"variants": {
        f"facing={f},stemming={m}": {"model": f"guhs:block/vadshout_gezicht_{m}", **({"y": r} if r else {})}
        for f, r in rot.items() for m in range(4)}})
    w(f"{A}/models/item/vadshout_gezicht.json", {"parent": "guhs:block/vadshout_gezicht_0"})
    h.simple_block("vadshout_planken")
    h.simple_block("vadshout_bladeren", render_type="minecraft:cutout_mipped")
    h.simple_block("vadsmos")
    w(f"{A}/models/block/vadshout_zaailing.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                   "textures": {"cross": "guhs:block/vadshout_zaailing"}})
    w(f"{A}/blockstates/vadshout_zaailing.json", {"variants": {"": {"model": "guhs:block/vadshout_zaailing"}}})
    h.item_model("vadshout_zaailing", "guhs:block/vadshout_zaailing")
    # the bush (its item is the berries)
    for i in range(4):
        w(f"{A}/models/block/knabbelbessenstruik_{i}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                             "textures": {"cross": f"guhs:block/knabbelbessenstruik_{i}"}})
    w(f"{A}/blockstates/knabbelbessenstruik.json", {"variants": {f"age={i}": {"model": f"guhs:block/knabbelbessenstruik_{i}"} for i in range(4)}})
    h.item_model("knabbelbessen")
    h.item_model("knabbelbessentaartje")
    # the nest: a round straw rim around a soft pink bowl
    el = h.el
    rim = "#rand"
    w(f"{A}/models/block/guhnestje.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": {"particle": "guhs:block/guhnestje_zijkant", "zijkant": "guhs:block/guhnestje_zijkant",
                     "rand": "guhs:block/guhnestje_rand", "binnen": "guhs:block/guhnestje_binnen"},
        "elements": [el([1, 0, 1], [15, 2, 15], "#zijkant", faces=("down", "north", "south", "west", "east")),
                     el([2, 2, 2], [14, 2.5, 14], "#binnen", faces=("up",)),
                     el([1, 2, 0.5], [15, 6, 3], rim), el([1, 2, 13], [15, 6, 15.5], rim),
                     el([0.5, 2, 2.5], [3, 6, 13.5], rim), el([13, 2, 2.5], [15.5, 6, 13.5], rim),
                     el([1, 1.5, 1], [15, 2, 15], "#zijkant", faces=("up",))]})
    w(f"{A}/blockstates/guhnestje.json", {"variants": {"": {"model": "guhs:block/guhnestje"}}})
    w(f"{A}/models/item/guhnestje.json", {"parent": "guhs:block/guhnestje"})
    # the rope (a chain made of string)
    chain = vanilla_json("assets/minecraft/models/block/chain.json")
    chain["textures"] = {"particle": "guhs:block/vadstouw", "all": "guhs:block/vadstouw"}
    chain["render_type"] = "minecraft:cutout"
    chain["parent"] = "minecraft:block/block"
    w(f"{A}/models/block/vadstouw.json", chain)
    w(f"{A}/blockstates/vadstouw.json", {"variants": {
        "axis=x": {"model": "guhs:block/vadstouw", "x": 90, "y": 90}, "axis=y": {"model": "guhs:block/vadstouw"},
        "axis=z": {"model": "guhs:block/vadstouw", "x": 90}}})
    h.item_model("vadstouw")

    # --- loot ---
    for b in ("vadshout_stam", "vadshout_gestript", "vadshout_planken", "vadshout_zaailing", "vadshout_gezicht", "vadsmos", "guhnestje",
              "vadstouw"):
        h.self_drop(b)
    bush = vanilla_json("data/minecraft/loot_table/blocks/sweet_berry_bush.json")
    txt = json.dumps(bush).replace("minecraft:sweet_berry_bush", "guhs:knabbelbessenstruik").replace("minecraft:sweet_berries", "guhs:knabbelbessen")
    w(f"{D}/loot_table/blocks/knabbelbessenstruik.json", json.loads(txt))
    leaves = vanilla_json("data/minecraft/loot_table/blocks/dark_oak_leaves.json")
    txt = json.dumps(leaves).replace("minecraft:dark_oak_leaves", "guhs:vadshout_bladeren").replace("minecraft:dark_oak_sapling", "guhs:vadshout_zaailing")
    txt = txt.replace("minecraft:apple", "guhs:knabbelbessen")        # (dark oak leaves drop apples: ours drop knabbelbessen)
    w(f"{D}/loot_table/blocks/vadshout_bladeren.json", json.loads(txt))

    # --- recipes ---
    h.add_tag("guhs/tags/item/vadshout_stammen", [f"guhs:{b}" for b in LOGS])
    h.shapeless("vadshout_planken", ["#guhs:vadshout_stammen"], "guhs:vadshout_planken", 4)
    h.shapeless("vadshout_gezicht", ["guhs:vadshout_stam", "guhs:kaas_knabbels"], "guhs:vadshout_gezicht", 1)
    h.shaped("guhnestje", ["S S", "WKW", "SSS"], {"S": "minecraft:stick", "W": "#minecraft:wool", "K": "guhs:kaas_knabbels"}, "guhs:guhnestje", 1)
    h.shaped("vadstouw", ["S", "S", "S"], {"S": "minecraft:string"}, "guhs:vadstouw", 3)
    h.shapeless("knabbelbessentaartje", ["guhs:knabbelbessen", "guhs:knabbelbessen", "guhs:knabbelbessen", "minecraft:sugar", "guhs:kaas_knabbels"],
                "guhs:knabbelbessentaartje", 1)
    h.shapeless("knabbelbessen_gele_kleurstof", ["guhs:knabbelbessen"], "minecraft:yellow_dye", 1)

    # --- tags ---
    add = h.add_tag
    for kind in ("block", "item"):
        add(f"minecraft/tags/{kind}/logs_that_burn", [f"guhs:{b}" for b in LOGS])
        add(f"minecraft/tags/{kind}/planks", ["guhs:vadshout_planken"])
        add(f"minecraft/tags/{kind}/wooden_stairs", ["guhs:vadshout_trap"])
        add(f"minecraft/tags/{kind}/wooden_slabs", ["guhs:vadshout_plaat"])
        add(f"minecraft/tags/{kind}/wooden_fences", ["guhs:vadshout_hek"])
        add(f"minecraft/tags/{kind}/fence_gates", ["guhs:vadshout_poort"])
        add(f"minecraft/tags/{kind}/wooden_doors", ["guhs:vadshout_deur"])
        add(f"minecraft/tags/{kind}/wooden_trapdoors", ["guhs:vadshout_luik"])
        add(f"minecraft/tags/{kind}/leaves", ["guhs:vadshout_bladeren"])
        add(f"minecraft/tags/{kind}/saplings", ["guhs:vadshout_zaailing"])
    add("minecraft/tags/block/mineable/axe", [f"guhs:{b}" for b in LOGS + ["vadshout_planken", *WOOD, "guhnestje"]])
    add("minecraft/tags/block/mineable/hoe", ["guhs:vadshout_bladeren", "guhs:vadsmos"])
    add("minecraft/tags/block/dirt", ["guhs:vadsmos"])
    add("minecraft/tags/block/climbable", ["guhs:vadstouw"])
    add("minecraft/tags/block/sword_efficient", ["guhs:knabbelbessenstruik"])


def wood_set(h):
    """Stairs, slab, fence, gate, door and trapdoor: vanilla oak's blockstates, models, loot and recipes, with our wood."""
    A, D, w = h.A, h.D, h.w
    tex = {"minecraft:block/oak_planks": "guhs:block/vadshout_planken", "minecraft:block/oak_door_top": "guhs:block/vadshout_deur_top",
           "minecraft:block/oak_door_bottom": "guhs:block/vadshout_deur_bottom", "minecraft:block/oak_trapdoor": "guhs:block/vadshout_luik"}
    with _jar() as z:
        names = z.namelist()
        for ours, oak in WOOD.items():
            state = json.loads(z.read(f"assets/minecraft/blockstates/{oak}.json"))
            text = json.dumps(state).replace("minecraft:block/oak_planks", "guhs:block/vadshout_planken")
            text = text.replace(f"minecraft:block/{oak}", f"guhs:block/{ours}")
            w(f"{A}/blockstates/{ours}.json", json.loads(text))
            # (fence models are called oak_fence_*, so the gate's models must not be caught by the fence prefix)
            for path in names:
                if not path.startswith(f"assets/minecraft/models/block/{oak}") or not path.endswith(".json"):
                    continue
                rest = path[len(f"assets/minecraft/models/block/{oak}"):-5]
                if oak == "oak_fence" and rest.startswith("_gate"):
                    continue
                model = json.loads(z.read(path))
                model["textures"] = {k: tex.get(v, v) for k, v in model.get("textures", {}).items()}
                if oak in ("oak_door", "oak_trapdoor"):
                    model["render_type"] = "minecraft:cutout"
                w(f"{A}/models/block/{ours}{rest}.json", model)
            loot = json.loads(z.read(f"data/minecraft/loot_table/blocks/{oak}.json"))
            w(f"{D}/loot_table/blocks/{ours}.json", json.loads(json.dumps(loot).replace(f"minecraft:{oak}", f"guhs:{ours}")))
            recipe = json.loads(z.read(f"data/minecraft/recipe/{oak}.json"))
            text = json.dumps(recipe).replace('"item": "minecraft:oak_planks"', '"item": "guhs:vadshout_planken"')
            text = text.replace(f'"id": "minecraft:{oak}"', f'"id": "guhs:{ours}"').replace('"group": "wooden_', '"group": "vadshout_')
            w(f"{D}/recipe/{ours}.json", json.loads(text))
    w(f"{A}/models/item/vadshout_trap.json", {"parent": "guhs:block/vadshout_trap"})
    w(f"{A}/models/item/vadshout_plaat.json", {"parent": "guhs:block/vadshout_plaat"})
    w(f"{A}/models/item/vadshout_hek.json", {"parent": "guhs:block/vadshout_hek_inventory"})
    w(f"{A}/models/item/vadshout_poort.json", {"parent": "guhs:block/vadshout_poort"})
    w(f"{A}/models/item/vadshout_luik.json", {"parent": "guhs:block/vadshout_luik_bottom"})
    h.item_model("vadshout_deur")


# =====================================================================================================================
# worldgen: the biome (a point in the Guhmension's multi_noise), its trees and plants, the surface rule
# =====================================================================================================================
def worldgen(h):
    D, w = h.D, h.w
    wg = f"{D}/worldgen"
    # the giant guh tree (Java: feature/vadswoud/ReuzenguhboomFeature) and the smaller vadshout tree (vanilla JSON)
    w(f"{wg}/configured_feature/reuzenguhboom.json", {"type": "guhs:reuzenguhboom", "config": {}})
    # (two tries per chunk: the tree itself keeps its distance from other giants and from structures)
    w(f"{wg}/placed_feature/reuzenguhboom.json", {"feature": "guhs:reuzenguhboom", "placement": [
        {"type": "minecraft:count", "count": 2}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
    leaves = {"Name": "guhs:vadshout_bladeren", "Properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}}
    tree = {"type": "minecraft:tree", "config": {
        "trunk_provider": {"type": "minecraft:simple_state_provider", "state": {"Name": "guhs:vadshout_stam", "Properties": {"axis": "y"}}},
        "foliage_provider": {"type": "minecraft:simple_state_provider", "state": leaves},
        "dirt_provider": {"type": "minecraft:simple_state_provider", "state": {"Name": "guhs:vadsmos"}},
        "trunk_placer": {"type": "minecraft:mega_jungle_trunk_placer", "base_height": 11, "height_rand_a": 2, "height_rand_b": 7},
        "foliage_placer": {"type": "minecraft:jungle_foliage_placer", "radius": 3, "offset": 0, "height": 3},
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 1, "upper_size": 2},
        "decorators": [{"type": "minecraft:alter_ground", "provider": {"type": "minecraft:simple_state_provider", "state": {"Name": "guhs:vadsmos"}}}],
        "ignore_vines": True, "force_dirt": False}}
    w(f"{wg}/configured_feature/vadshout_boom.json", tree)
    w(f"{wg}/placed_feature/vadshout_boom.json", {"feature": "guhs:vadshout_boom", "placement": [
        {"type": "minecraft:count", "count": 3}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive",
                                                                     "state": {"Name": "guhs:vadshout_zaailing", "Properties": {"stage": "0"}}}},
        {"type": "minecraft:biome"}]})
    # the smaller tree is also what a single sapling grows into (four saplings in a square: a giant guh tree)
    w(f"{wg}/configured_feature/knabbelbessenstruiken.json", {"type": "minecraft:random_patch", "config": {
        "tries": 28, "xz_spread": 5, "y_spread": 1, "feature": {"feature": {"type": "minecraft:simple_block", "config": {
            "to_place": {"type": "minecraft:weighted_state_provider", "entries": [
                {"weight": 3, "data": {"Name": "guhs:knabbelbessenstruik", "Properties": {"age": "3"}}},
                {"weight": 1, "data": {"Name": "guhs:knabbelbessenstruik", "Properties": {"age": "2"}}}]}}},
            "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
                {"type": "minecraft:would_survive", "state": {"Name": "guhs:knabbelbessenstruik", "Properties": {"age": "0"}}}]}}]}}})
    w(f"{wg}/placed_feature/knabbelbessenstruiken.json", {"feature": "guhs:knabbelbessenstruiken", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 3}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
    w(f"{wg}/configured_feature/guhnestje.json", {"type": "minecraft:simple_block", "config": {
        "to_place": {"type": "minecraft:simple_state_provider", "state": {"Name": "guhs:guhnestje"}}}})
    w(f"{wg}/placed_feature/guhnestje.json", {"feature": "guhs:guhnestje", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 5}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
            {"type": "minecraft:solid", "offset": [0, -1, 0]}]}},
        {"type": "minecraft:biome"}]})
    w(f"{wg}/configured_feature/vadsvaren.json", {"type": "minecraft:random_patch", "config": {
        "tries": 48, "xz_spread": 7, "y_spread": 2, "feature": {"feature": {"type": "minecraft:simple_block", "config": {
            "to_place": {"type": "minecraft:weighted_state_provider", "entries": [
                {"weight": 5, "data": {"Name": "guhs:roze_gras"}}, {"weight": 1, "data": {"Name": "guhs:guhoortjes"}},
                {"weight": 1, "data": {"Name": "guhs:kaasbloem"}}]}}},
            "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
                {"type": "minecraft:matching_blocks", "offset": [0, -1, 0], "blocks": ["guhs:vadsmos", "minecraft:pink_wool"]}]}}]}}})
    w(f"{wg}/placed_feature/vadsvaren.json", {"feature": "guhs:vadsvaren", "placement": [
        {"type": "minecraft:count", "count": 3}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})

    # the biome itself: a copy of the other Guhmension biomes (ores), with its own colours, mist, fluff and trees
    base = json.load(open(f"{wg}/biome/pink_puffs.json", encoding="utf-8"))
    ores = base["features"][6]
    biome = {
        "has_precipitation": False, "temperature": 0.7, "downfall": 0.4, "creature_spawn_probability": 0.3,
        "effects": {"sky_color": 0xBFA8EE, "fog_color": 0xCDEEDD, "water_color": 0x9FE6CB, "water_fog_color": 0x5FB89A,
                    "grass_color": 0x9ED9B5, "foliage_color": 0x9ED9B5,
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
                    "music": {"sound": "minecraft:music.overworld.forest", "min_delay": 12000, "max_delay": 24000, "replace_current_music": False},
                    "particle": {"options": {"type": "guhs:vadspluisje"}, "probability": 0.012}},
        "spawners": {**{k: [] for k in base["spawners"]},
                     "creature": [{"type": "guhs:guh", "weight": 100, "minCount": 3, "maxCount": 5},
                                  {"type": "guhs:guh_bee", "weight": 10, "minCount": 1, "maxCount": 2}]},
        "spawn_costs": {}, "carvers": {"air": []},
        "features": [[], [], [], [], [], [], ores, [], [], ["guhs:reuzenguhboom", "guhs:vadshout_boom", "guhs:knabbelbessenstruiken",
                                                            "guhs:guhnestje", "guhs:vadsvaren"], []]}
    w(f"{wg}/biome/{BIOME}.json", biome)

    def biome_source(d):
        entries = [e for e in d["generator"]["biome_source"]["biomes"] if e["biome"] != f"guhs:{BIOME}"]
        entries.append({"biome": f"guhs:{BIOME}", "parameters": {"temperature": -0.15, "humidity": 0.55, "continentalness": [-1.0, 1.0],
                                                                 "erosion": [-0.25, 0.25], "weirdness": 0.0, "depth": [-1.0, 1.0], "offset": 0.0}})
        d["generator"]["biome_source"]["biomes"] = entries
    h.patch_json(f"{D}/dimension/guhmension.json", biome_source)

    def surface(d):
        rules = d["surface_rule"]["sequence"]
        rule = {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [f"guhs:{BIOME}"]},
                "then_run": {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "offset": 0,
                             "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"},
                             "then_run": {"type": "minecraft:sequence", "sequence": [
                                 {"type": "minecraft:condition", "if_true": {"type": "minecraft:noise_threshold",
                                  "noise": "guhs:guhmension_patches", "min_threshold": 0.55, "max_threshold": 10.0},
                                  "then_run": {"type": "minecraft:block", "result_state": {"Name": "minecraft:pink_wool"}}},
                                 {"type": "minecraft:block", "result_state": {"Name": "guhs:vadsmos"}}]}}}
        d["surface_rule"]["sequence"] = [r for r in rules if r.get("if_true", {}).get("biome_is") != [f"guhs:{BIOME}"]] + [rule]
    h.patch_json(f"{wg}/noise_settings/guhmension.json", surface)
    # guh villagers born here are guh villagers too
    h.patch_json(f"{h.R}/data/neoforge/data_maps/worldgen/biome/villager_types.json",
                 lambda d: d["values"].update({f"guhs:{BIOME}": {"villager_type": "guhs:guh"}}))


def npcs(h):
    """The two tree-village characters: sitting guhs in their own colours, with their outfit painted on."""
    src = h.Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    np = h.np
    # the Boswachterguh: moss green, a ranger hat band over the eyes
    img = h.recolour(src, hue=0.30, sat=0.55, val=0.98, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    a[128:134, 440:497, :3] = (70, 105, 60)
    a[128:130, 440:497, :3] = (110, 150, 90)
    for x in range(446, 494, 12):
        a[130:133, x:x + 4, :3] = (240, 150, 190)
    h.save(h.Image.fromarray(a), "entity", "npc_boswachterguh.png")
    # the Knabbelplukker: warm apricot, an orange-yellow striped cap band, knabbelbes-yellow cheeks
    img = h.recolour(src, hue=0.07, sat=0.9, val=1.04, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    for y in range(128, 134):
        a[y, 440:497, :3] = (230, 120, 60) if (y // 2) % 2 else (250, 190, 70)
    h.save(h.Image.fromarray(a), "entity", "npc_knabbelplukker.png")


# =====================================================================================================================
# advancements and texts
# =====================================================================================================================
QUEST_ADVANCEMENTS = ["seen_boswachterguh", "seen_knabbelplukker", "vadswoud_gezin", "vadswoud_geslapen", "vadswoud_boswachter_praat",
                      "vadswoud_plukker_praat", "vadswoud_reuzenboom"]


def advancements(h):
    D = h.D
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    baby_tame = {"trigger": "minecraft:tame_animal", "conditions": {"entity": [{"condition": "minecraft:entity_properties", "entity": "this",
                                                                               "predicate": {"type": "guhs:guh", "flags": {"is_baby": True}}}]}}
    h.w(f"{D}/advancement/quest/vadswoud_baby_getemd.json", {"criteria": {"done": baby_tame}})
    for name, parent, icon, frame, crit, title, desc in [
        ("vadswoud_bezocht", "enter_guhmension", "guhs:vadshout_zaailing", "task",
         {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"biomes": f"guhs:{BIOME}"}}}}},
         "Tussen de reuzen", "Loop door het mistige Vadswoud, onder de reuzenguhbomen"),
        ("boomhutdorp_gevonden", "vadswoud_bezocht", "guhs:vadstouw", "goal",
         {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": f"guhs:{NAME}"}}}}},
         "Hoog in de bomen", "Vind het boomhutdorp van de guhs in het Vadswoud"),
        ("vadswoud_babyguh", "vadswoud_bezocht", "guhs:knabbelbessen", "goal", {"done": baby_tame},
         "Klein maar vads", "Tem een babyguh (babyguhs zijn dol op knabbelbessen!)"),
        ("vadswoud_nestje", "vadswoud_babyguh", "guhs:guhnestje", "goal", {"done": {"trigger": "minecraft:impossible"}},
         "Welterusten, guh", "Laat je eigen guh 's nachts in een guhnestje slapen"),
        ("vadswoud_boswachter", "boomhutdorp_gevonden", "guhs:boswachtershoed", "goal",
         {c: {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"guhs:{c}"}]}} for c in ("boswachtershoed", "boswachtersjas")},
         "Boswachter in spe", "Koop de boswachtershoed en de boswachtersjas bij de Boswachterguh"),
        ("vadswoud_plukker", "boomhutdorp_gevonden", "guhs:plukmandje", "challenge",
         {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:knabbelbessen", "count": {"min": 64}}]}}},
         "Plukker van het jaar", "Heb 64 knabbelbessen tegelijk bij je. VAHOEG, wat een oogst!"),
        ("vadswoud_reuzenboom", "vadswoud_bezocht", "guhs:vadshout_gezicht", "challenge", {"done": {"trigger": "minecraft:impossible"}},
         "Een boom van een guh", "Laat vier vadshoutzaailingen in een vierkant uitgroeien tot een reuzenguhboom"),
    ]:
        h.w(f"{D}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": crit, **({"requirements": [[c] for c in crit]} if len(crit) > 1 else {})})
        h.lang(f"advancements.guhs.guhmension.{name}.title", title, title)
        h.lang(f"advancements.guhs.guhmension.{name}.description", desc, desc)
    # the treasure of the village
    h.w(f"{D}/loot_table/chests/{NAME}.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:knabbelbessen", "functions": h.count_fn(6, 14)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:vadshout_zaailing", "functions": h.count_fn(1, 4)}]},
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
            {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 5, "functions": h.count_fn(6, 16)},
            {"type": "minecraft:item", "name": "guhs:knabbelbessentaartje", "weight": 3, "functions": h.count_fn(1, 3)},
            {"type": "minecraft:item", "name": "guhs:vadshout_planken", "weight": 4, "functions": h.count_fn(8, 24)},
            {"type": "minecraft:item", "name": "guhs:vadstouw", "weight": 3, "functions": h.count_fn(4, 10)},
            {"type": "minecraft:item", "name": "guhs:guhnestje", "weight": 2},
            {"type": "minecraft:item", "name": "guhs:vadshout_gezicht", "weight": 2, "functions": h.count_fn(1, 3)},
            {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 2, "functions": h.count_fn(2, 5)},
            {"type": "minecraft:item", "name": "guhs:vahoege_vads_ingot", "weight": 1}]}]})
    h.w(f"{D}/loot_table/chests/{NAME}_uitkijk.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:vahoege_vads_ingot", "functions": h.count_fn(1, 3)}]},
        # (2.9, kleding: the plukmuts and the boswachtershoed are only sold in the village now)
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:knabbelbessentaartje", "weight": 2, "functions": h.count_fn(1, 3)},
                                 {"type": "minecraft:item", "name": "guhs:vadshout_gezicht", "weight": 1, "functions": h.count_fn(1, 2)},
                                 {"type": "minecraft:empty", "weight": 2}]},
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
            {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 3, "functions": h.count_fn(4, 10)},
            {"type": "minecraft:item", "name": "guhs:guh_kristal", "weight": 3, "functions": h.count_fn(3, 8)},
            {"type": "minecraft:item", "name": "guhs:knabbelbessentaartje", "weight": 3, "functions": h.count_fn(2, 4)},
            {"type": "minecraft:item", "name": "guhs:vadshout_zaailing", "weight": 2, "functions": h.count_fn(4, 4)},
            {"type": "minecraft:item", "name": "minecraft:golden_apple", "weight": 1}]}]})


LANG = {
    "biome.guhs.vadswoud": "Vadswoud",
    "block.guhs.vadshout_stam": "Vadshoutstam", "block.guhs.vadshout_gestript": "Gestripte vadshoutstam",
    "block.guhs.vadshout_planken": "Vadshoutplanken", "block.guhs.vadshout_trap": "Vadshouttrap", "block.guhs.vadshout_plaat": "Vadshoutplaat",
    "block.guhs.vadshout_hek": "Vadshouthek", "block.guhs.vadshout_poort": "Vadshoutpoort", "block.guhs.vadshout_deur": "Vadshoutdeur",
    "block.guhs.vadshout_luik": "Vadshoutluik", "block.guhs.vadshout_bladeren": "Vadshoutbladeren",
    "block.guhs.vadshout_zaailing": "Vadshoutzaailing", "block.guhs.vadshout_gezicht": "Guhgezichtje in de schors",
    "block.guhs.vadsmos": "Vadsmos", "block.guhs.guhnestje": "Guhnestje", "block.guhs.knabbelbessenstruik": "Knabbelbessenstruik",
    "block.guhs.vadstouw": "Vadstouw",
    "item.guhs.knabbelbessen": "Knabbelbessen", "item.guhs.knabbelbessentaartje": "Knabbelbessentaartje",
    "item.guhs.boswachtershoed": "Boswachtershoed", "item.guhs.boswachtersjas": "Boswachtersjas",
    "item.guhs.plukmuts": "Plukmuts", "item.guhs.plukmandje": "Plukmandje",
    "block.guhs.guhnestje.lore": "Guhs slapen er 's nachts in, met het hele gezin. Ook jouw tamme guhs!",
    "item.guhs.knabbelbessen.lore": "Babyguhs zijn er dol op: wilde babyguhs tem je er makkelijk mee",
    "block.guhs.vadshout_gezicht.lore": "Rechtsklik: verander zijn humeur",
    "block.guhs.vadshout_zaailing.lore": "Vier in een vierkant: een reuzenguhboom!",
    "entity.guhs.guh_npc.boswachterguh": "Boswachterguh", "entity.guhs.guh_npc.knabbelplukker": "Knabbelplukker",
    "structure.guhs.boomhutdorp": "Boomhutdorp",
    "structure.guhs.boomhutdorp.tooltip": "Guh-boomhutten hoog in de reuzenguhbomen van het Vadswoud, met de Boswachterguh en de Knabbelplukker",
    "gui.guhs.guhdex.rarity.boswachterguh": "Zeldzaamheid: Zeldzaam (boomhutdorp, Vadswoud)",
    "gui.guhs.guhdex.info.boswachterguh": "Zorgt voor het Vadswoud en alle guhfamilies. Verkoopt vadshout, zaailingen, guhnestjes en het boswachterspakje.",
    "gui.guhs.guhdex.rarity.knabbelplukker": "Zeldzaamheid: Zeldzaam (boomhutdorp, Vadswoud)",
    "gui.guhs.guhdex.info.knabbelplukker": "Plukt de hele dag knabbelbessen en ruilt ze tegen van alles. Pas op: soms eet ze de oogst zelf op.",
    # the Boswachterguh
    "quest.guhs.vadswoud.boswachter.hello": "Njeg, een bezoeker in het Vadswoud! Ik ben de Boswachterguh. Ik let op de reuzenguhbomen en op alle guhfamilies. Kijk maar eens naar de babyguhs: die lopen in een rijtje achter hun ouder aan. VAHOEG, zo schattig!",
    "quest.guhs.vadswoud.boswachter.tip0": "'s Nachts kruipen de guhfamilies samen in een guhnestje. Zet er een neer bij je huis, dan gaan jouw tamme guhs er ook in slapen. Vads!",
    "quest.guhs.vadswoud.boswachter.tip1": "Een wilde babyguh is veel makkelijker te temmen dan een grote. Met knabbelbessen lukt het bijna altijd, njeg!",
    "quest.guhs.vadswoud.boswachter.tip2": "Plant vier vadshoutzaailingen in een vierkant, dan groeit er een reuzenguhboom. Luister goed: die bomen giechelen soms.",
    "quest.guhs.vadswoud.boswachter.tip3": "Die guhgezichtjes in de schors? Dat zijn de bomen zelf. Tik er eens op, dan krijgen ze een ander humeur. Vahoeg!",
    "quest.guhs.vadswoud.boswachter.tip4": "De Mika's willen onze knabbelbessen inpikken, want dan worden de guhs niet vahoeg genoeg. Njeg! Daarom pluk ik ze liever zelf.",
    "quest.guhs.vadswoud.boswachter.tip5": "Mijn boswachtershoed en boswachtersjas? Die heb ik ook in guhmaat. Staat jouw guh vast vadsig!",
    # the Knabbelplukker
    "quest.guhs.vadswoud.plukker.hello": "Hoi hoi! Ik ben de Knabbelplukker. Ik pluk knabbelbessen van de struikjes. Wil je ruilen? Voor knabbelbessen heb ik taartjes, nestjes en mijn plukpakje. Njeg, niet stiekem snoepen hoor... *smak*",
    "quest.guhs.vadswoud.plukker.tip0": "Knabbelbessen prikken niet, hoor. Je loopt er gewoon doorheen. Pluk ze als ze geel zijn, dan groeien ze weer aan.",
    "quest.guhs.vadswoud.plukker.tip1": "Babyguhs lusten niks liever dan knabbelbessen. Geef een wilde babyguh er een, dan wil hij vaak meteen met je mee!",
    "quest.guhs.vadswoud.plukker.tip2": "Drie knabbelbessen, suiker en een kaasknabbel: een knabbelbessentaartje. VAHOEG, dat is pas vads!",
    "quest.guhs.vadswoud.plukker.tip3": "Ik had er vanochtend nog honderd, maar... *hik*... ik heb er maar een paar opgegeten. Echt. Njeg.",
    "quest.guhs.vadswoud.plukker.tip4": "Geef je tamme babyguh knabbelbessen, dan wordt hij sneller groot. Van vads naar vahoeg!",
    # families, nests, taming babies
    "gui.guhs.vadswoud.baby_getemd": "VAHOEG! De babyguh wil met je mee!",
    "gui.guhs.vadswoud.baby_bessen": "Mjam, knabbelbessen! De babyguh groeit er een beetje van.",
    "gui.guhs.vadswoud.alleen_babys": "Njeg, knabbelbessen zijn voor babyguhs. Grote guhs willen kaasknabbels!",
    "gui.guhs.vadswoud.slaapt": "%s slaapt lekker in het guhnestje. Zzz...",
    "gui.guhs.vadswoud.reuzenboom": "VAHOEG! Er is een reuzenguhboom uit je zaailingen gegroeid!",
}


def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)


def selfcheck_assets(h):
    """check_assets.py only reads the registry classes: this checks our own blocks and items."""
    A = h.A
    missing = []
    for b in BLOCKS:
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
        if b != "knabbelbessenstruik" and not os.path.exists(f"{A}/models/item/{b}.json"):
            missing.append(f"item model {b}")
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in ("knabbelbessen", "knabbelbessentaartje"):
        if not os.path.exists(f"{A}/models/item/{i}.json") or not os.path.exists(f"{A}/textures/item/{i}.png"):
            missing.append(f"item {i}")
    for root, _dirs, files in os.walk(f"{A}/models/block"):
        for f in files:
            if f.startswith(("vadshout", "vadsmos", "guhnestje", "knabbelbessen", "vadstouw")):
                model = json.load(open(os.path.join(root, f), encoding="utf-8"))
                for t in model.get("textures", {}).values():
                    if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                        missing.append(f"texture {t} ({f})")
    if missing:
        raise SystemExit(f"vadswoud assets missing: {missing}")


# =====================================================================================================================
# the village template
# =====================================================================================================================
def face_role(u, v, R):
    """A guh face (front view, v up) of radius R: skin, ear, ear_in, eye, shine, nose, mouth, cheek or None."""
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
        y0 = round(-0.42 * R)
        if (round(u), round(v)) in {(-2, y0 + 1), (-1, y0), (0, y0), (1, y0), (2, y0 + 1)}:
            role = "mouth"
    return role


FACE_WOOL = {"skin": "minecraft:pink_wool", "ear": "minecraft:pink_wool", "ear_in": "minecraft:magenta_wool", "eye": "minecraft:black_wool",
             "shine": "minecraft:white_wool", "nose": "minecraft:magenta_concrete", "mouth": "minecraft:black_wool", "cheek": "minecraft:pink_terracotta"}
STEP = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}

# the five trees: centre, trunk radius, top of the trunk, canopy (horizontal, vertical radius), lowest branch
TREES = {
    "moeder": {"c": (50, 50), "r": 4.2, "top": G + 60, "canopy": (13.5, 7.5), "branch": G + 55},
    "oost": {"c": (86, 50), "r": 3.0, "top": G + 45, "canopy": (11.5, 6.0), "branch": G + 31},
    "west": {"c": (14, 50), "r": 3.0, "top": G + 44, "canopy": (11.5, 6.0), "branch": G + 31},
    "noord": {"c": (50, 14), "r": 2.9, "top": G + 50, "canopy": (11.0, 6.0), "branch": G + 38},
    "zuid": {"c": (50, 86), "r": 3.1, "top": G + 50, "canopy": (11.5, 6.0), "branch": G + 38},
}
P0, P1, P2, LOW, UP = G + 16, G + 30, G + 44, G + 18, G + 32   # deck levels (the block y of the floor)

LEAVES = "guhs:vadshout_bladeren"
LOG = "guhs:vadshout_stam"
PLANK = "guhs:vadshout_planken"
HEK = "guhs:vadshout_hek"
TOUW = "guhs:vadstouw"
LADDER = "minecraft:ladder"
DOOR = "guhs:vadshout_deur"
NEST = "guhs:guhnestje"
BUSH = "guhs:knabbelbessenstruik"
LAMPS = {"guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint"}
PLANTS = {"guhs:roze_gras", "guhs:guhoortjes", "guhs:kaasbloem", "guhs:roze_guhbloem", "guhs:knabbelroos", BUSH}
PASSABLE = {None, "minecraft:air", LADDER, DOOR, NEST, "guhs:white_kussen", "guhs:pink_kussen", "minecraft:pink_carpet",
            "minecraft:white_carpet", "minecraft:lime_carpet"} | PLANTS | LAMPS
NOT_FLOOR = {None, "minecraft:air", LADDER, DOOR, LEAVES} | PLANTS | LAMPS | {"minecraft:pink_carpet", "minecraft:white_carpet", "minecraft:lime_carpet",
                                                                             "guhs:white_kussen", "guhs:pink_kussen"}
NEEDS_FLOOR = PLANTS | {NEST, "minecraft:chest", "guhs:guh_bank", "guhs:guh_tafel", "guhs:guh_stoel", "guhs:white_kussen", "guhs:pink_kussen",
                        "guhs:guh_taart", "minecraft:pink_carpet", "minecraft:white_carpet", "minecraft:lime_carpet", "guhs:guh_kast"}


def solid(b):
    return b not in NOT_FLOOR and b not in PASSABLE


def fence_props():
    return {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}


def lamp(hanging, colour="geel"):
    return f"guhs:lampion_{colour}", {"hanging": "true" if hanging else "false", "waterlogged": "false"}


class Village:
    def __init__(self, h):
        self.h = h
        self.s = h.Structure((W, H, D))
        self.rng = random.Random(27050)
        self.reserved = set()          # cells that must stay free of leaves (walk space, huts)
        self.trunk_cells = {}          # tree name -> set of (x, z) of the trunk at deck height
        self.targets = {}              # name -> feet position that must be reachable
        self.ladders = []

    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, name, props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def reserve(self, x, y, z, up=3):
        for k in range(up + 1):
            self.reserved.add((x, y + k, z))


def template(h):
    v = Village(h)
    ground(v)
    for name in TREES:
        giant_tree(v, name)
    moeder_face_and_hollow(v)
    spiral_stair(v)
    decks(v)
    huts(v)
    bridges(v)
    ladders(v)
    garden(v)
    families(v)
    finish(v)
    return v.s


# --- the ground: a clearing of vadsmos with paths -----------------------------------------------------------------------
def ground(v):
    mc = v.h.mc
    rng = v.rng
    v.footprint = []
    for x in range(W):
        for z in range(D):
            if ((x - 49.5) / 50) ** 4 + ((z - 49.5) / 50) ** 4 <= 1:
                v.footprint.append((x, z))
                v.set(x, G, z, "guhs:vadsmos" if (x * 7 + z * 3) % 11 else mc("pink_wool"))
                for y in range(1, G):
                    v.set(x, y, z, mc("pink_wool"))
    # paths from the four sides to the trees: stepping stones of pink terracotta and white concrete
    for i in range(W):
        for o in (-1, 0, 1):
            for (x, z) in ((C + o, i), (i, C + o)):
                if 0 <= x < W and 0 <= z < D and v.get(x, G, z):
                    v.set(x, G, z, mc("pink_terracotta") if (i + o) % 4 else mc("white_concrete"))
    v.fp = set(v.footprint)


# --- the trees ------------------------------------------------------------------------------------------------------------
def giant_tree(v, name):
    t = TREES[name]
    cx, cz = t["c"]
    r, top = t["r"], t["top"]
    rng = random.Random(sum(map(ord, name)) * 31)
    decks_top = {"moeder": P2 + 9, "oost": LOW + 9, "west": LOW + 9, "noord": UP + 6, "zuid": UP + 6}[name]
    no_flare = {("moeder", "south"), ("oost", "south"), ("west", "north"), ("noord", "east"), ("zuid", "west")}
    logs = set()
    cells_at = {}
    for y in range(1, top + 1):
        rr = r
        if y <= G + 5:
            rr = r + 2.6 * math.exp(-(y - G) / 1.9) if y >= G else r + 2.6
        if y > decks_top:
            rr = r * (1 - 0.38 * (y - decks_top) / max(1, top - decks_top))
        for x in range(int(cx - rr - 2), int(cx + rr + 3)):
            for z in range(int(cz - rr - 2), int(cz + rr + 3)):
                d = math.dist((x, z), (cx, cz))
                if d > rr + 0.35:
                    continue
                if d > r + 0.35:          # the flare: not on the side with the ladder / door
                    side = ("east" if x > cx else "west") if abs(x - cx) > abs(z - cz) else ("south" if z > cz else "north")
                    if (name, side) in no_flare:
                        continue
                v.set(x, y, z, LOG, {"axis": "y"})
                logs.add((x, y, z))
                cells_at.setdefault(y, set()).add((x, z))
    v.trunk_cells[name] = cells_at
    # roots creeping over the ground
    for i in range(7):
        a = i / 7 * 2 * math.pi + rng.uniform(-0.2, 0.2)
        dx, dz = math.cos(a), math.sin(a)
        side = ("east" if dx > 0 else "west") if abs(dx) > abs(dz) else ("south" if dz > 0 else "north")
        if (name, side) in no_flare:
            continue
        length = rng.uniform(4, 7.5)
        for k in range(int(r * 10), int((r + length) * 10), 5):
            s = k / 10
            x, z = round(cx + dx * s), round(cz + dz * s)
            y = G + 1 if s < r + length * 0.55 else G
            if (x, z) in v.fp:
                v.set(x, y, z, LOG, {"axis": "x" if abs(dx) > abs(dz) else "z"})
                logs.add((x, y, z))
    # branches with leaf blobs, and the big crown
    blobs = [((cx, top + 1.5, cz), t["canopy"][0], t["canopy"][1])]
    n = 6 if name == "moeder" else 5
    for i in range(n):
        a = i / n * 2 * math.pi + rng.uniform(-0.3, 0.3)
        y0 = rng.uniform(t["branch"], top - 3)
        length = rng.uniform(6, 9 if name == "moeder" else 7.5)
        dx, dz = math.cos(a), math.sin(a)
        pts = [(round(cx + dx * (r * 0.6 + k / 4)), round(y0 + (r * 0.6 + k / 4) * 0.45), round(cz + dz * (r * 0.6 + k / 4)))
               for k in range(0, int(length * 4) + 1)]
        for (x, y, z) in line6(pts):
            if (x, y, z) not in logs:
                v.set(x, y, z, LOG, {"axis": "x" if abs(dx) > abs(dz) else "z"})
                logs.add((x, y, z))
        end = pts[-1]
        blobs.append((end, rng.uniform(4.2, 5.4), rng.uniform(2.6, 3.4)))
    for k in range(5):
        a = k / 5 * 2 * math.pi + 0.5
        blobs.append(((cx + math.cos(a) * t["canopy"][0] * 0.55, top - 0.5, cz + math.sin(a) * t["canopy"][0] * 0.55),
                      t["canopy"][0] * 0.55, t["canopy"][1] * 0.7))
    for (bx, by, bz), rh, rv in blobs:
        for x in range(int(bx - rh - 1), int(bx + rh + 2)):
            for z in range(int(bz - rh - 1), int(bz + rh + 2)):
                for y in range(int(by - rv - 1), int(by + rv + 2)):
                    q = ((x - bx) / rh) ** 2 + ((z - bz) / rh) ** 2 + ((y - by) / rv) ** 2
                    wobble = 0.82 + 0.25 * math.sin(x * 1.7 + z * 2.3 + y * 0.9)
                    if q <= wobble and v.get(x, y, z) is None and 0 <= x < W and 0 <= z < D and y < H:
                        v.set(x, y, z, LEAVES, {"distance": "7", "persistent": "true", "waterlogged": "false"})
    # little guh faces in the bark, looking out on every side
    faces = 0
    for y in range(G + 3, decks_top, 3):
        for (x, z) in sorted(cells_at.get(y, ())):
            for f, (dx, dz) in STEP.items():
                out = (x + dx, z + dz)
                if out in cells_at.get(y, ()) or v.get(x + dx, y, z + dz) is not None:
                    continue
                # only where the bark is flat enough that the face really looks out that way
                if (x - dz, z - dx) in cells_at[y] and (x + dz, z + dx) in cells_at[y] and rng.random() < 0.09:
                    v.set(x, y, z, "guhs:vadshout_gezicht", {"facing": f, "stemming": str(rng.randrange(4))})
                    faces += 1
                break
    v.faces = getattr(v, "faces", 0) + faces


def moeder_face_and_hollow(v):
    """A big guh face in the Moederboom (south side), its mouth the door to a hollow room with a nest and a chest;
    and big bark faces looking east, west and north a bit higher up."""
    mc = v.h.mc
    cx, cz = TREES["moeder"]["c"]
    cells = v.trunk_cells["moeder"]
    # the hollow
    for y in range(G + 1, G + 5):
        for (x, z) in cells[y]:
            if math.dist((x, z), (cx, cz)) <= 2.9:
                v.set(x, y, z, mc("air"))
    for (x, z) in cells[G + 1]:
        if math.dist((x, z), (cx, cz)) <= 2.9:
            v.set(x, G, z, PLANK)
    # the faces: on the front of the trunk for every (horizontal, y) line
    for f, R, fy in (("south", 4.2, G + 5), ("east", 3.2, G + 23), ("south", 3.2, G + 23), ("north", 3.2, G + 23), ("west", 3.0, G + 37)):
        dx, dz = STEP[f]
        for y in range(fy - 6, fy + 7):
            for u in range(-6, 7):
                # the outermost trunk block of this line
                line = [(x, z) for (x, z) in cells.get(y, ()) if (x - cx if dz else z - cz) == u]
                if not line:
                    continue
                x, z = max(line, key=lambda p: p[0] * dx + p[1] * dz)
                role = face_role(u * (1 if dz else -1) * (1 if (dz > 0 or dx < 0) else -1), y - fy, R)
                if role in ("eye", "shine", "nose", "mouth", "cheek", "ear_in"):
                    v.set(x, y, z, {"eye": mc("black_concrete"), "shine": mc("white_concrete"), "nose": mc("magenta_concrete"),
                                    "mouth": mc("black_concrete"), "cheek": mc("pink_wool"), "ear_in": mc("magenta_wool")}[role])
    # the mouth of the south face is the door (3 wide, 3 high)
    front = max(z for (x, z) in cells[G + 1] if x == cx)
    for x in range(cx - 1, cx + 2):
        for y in range(G + 1, G + 4):
            for z in range(cz + 2, front + 1):
                v.set(x, y, z, mc("air"))
    v.targets["de holte in de Moederboom"] = (cx, G + 1, cz)
    v.set(cx - 2, G + 1, cz - 1, mc("chest"), {"facing": "east", "type": "single", "waterlogged": "false"},
          {"id": "minecraft:chest", "LootTable": f"guhs:chests/{NAME}"})
    v.set(cx + 1, G + 1, cz - 1, NEST)
    v.set(cx, G + 4, cz, *lamp(True))
    v.nests = [(cx + 1, G + 1, cz - 1)]


# --- the spiral stair around the Moederboom, from the ground to the plaza -------------------------------------------------
def spiral_stair(v):
    mc = v.h.mc
    cx, cz = TREES["moeder"]["c"]
    a0, turn = math.radians(118), math.radians(328)       # it starts south-west of the face door and winds counterclockwise
    steps = P0 - G - 1                                     # step blocks G+1 .. P0-1, then the plaza
    cells = []
    for k in range(0, 900):
        a = a0 + turn * k / 899
        for rad in (5.3, 6.3):
            p = (round(cx + math.cos(a) * rad), round(cz + math.sin(a) * rad))
            if p not in [c[0] for c in cells]:
                cells.append((p, a))
    stair = {}
    for (x, z), a in cells:
        t = (a - a0) / turn
        y = G + 1 + min(steps - 1, int(t * steps))
        if (x, z) in stair:
            continue
        stair[(x, z)] = (y, a)
    for (x, z), (y, a) in stair.items():
        tx, tz = -math.sin(a), math.cos(a)             # the way up (counterclockwise in x/z)
        facing = ("east" if tx > 0 else "west") if abs(tx) > abs(tz) else ("south" if tz > 0 else "north")
        v.set(x, y, z, "guhs:vadshout_trap", {"facing": facing, "half": "bottom", "shape": "straight", "waterlogged": "false"})
        for yy in range(y + 1, y + 4):
            v.set(x, yy, z, mc("air"))
            v.reserved.add((x, yy, z))
        if (x + z) % 3 == 0 and not (abs(x - cx) <= 2 and z > cz):   # posts under the stair (not in front of the door)
            for yy in range(G + 1, y):
                if v.get(x, yy, z) is None:
                    v.set(x, yy, z, HEK, fence_props())
    v.spiral = stair
    # a railing on the outside
    for (x, z), (y, a) in stair.items():
        for dx, dz in STEP.values():
            q = (x + dx, z + dz)
            if q not in stair and math.dist(q, (cx, cz)) > 6.6 and v.get(q[0], y, q[1]) is None and y > G + 2:
                v.set(q[0], y, q[1], HEK, fence_props())


# --- decks ------------------------------------------------------------------------------------------------------------------
# name: (tree, floor y, outer radius, [(extra disc centre, radius)], [openings: direction or (x, z)])
DECKS = {
    "plein": ("moeder", P0, 14.4, [], ["north", "south", "east", "west"]),
    "boswachterspost": ("moeder", P1, 10.4, [((60, 50), 6.4)], ["north", "south"]),
    "uitkijk": ("moeder", P2, 8.4, [((41, 50), 5.4)], []),
    "oost": ("oost", LOW, 9.4, [((86, 40), 6.4)], ["west"]),
    "west": ("west", LOW, 9.4, [((14, 60), 6.4)], ["east"]),
    "noord": ("noord", LOW, 9.4, [((60, 14), 6.4)], ["south"]),
    "zuid": ("zuid", LOW, 10.4, [((40, 86), 6.4)], ["north"]),
    "noord_boven": ("noord", UP, 7.4, [], ["south"]),
    "zuid_boven": ("zuid", UP, 7.4, [], ["north"]),
}


def decks(v):
    mc = v.h.mc
    v.deck_cells = {}
    for name, (tree, y, ro, discs, openings) in DECKS.items():
        cx, cz = TREES[tree]["c"]
        cells = set()
        for x in range(W):
            for z in range(D):
                inside = math.dist((x, z), (cx, cz)) <= ro or any(math.dist((x, z), c) <= rr for c, rr in discs)
                if inside and (x, z) not in v.trunk_cells[tree].get(y, ()):
                    cells.add((x, z))
        if name == "plein":   # the stairwell of the spiral stair
            cells -= {p for p, (sy, _) in v.spiral.items() if sy + 3 >= P0}
        v.deck_cells[name] = cells
        for (x, z) in cells:
            v.set(x, y, z, PLANK)
            for k in range(1, 4):
                v.set(x, y + k, z, mc("air"))
                v.reserved.add((x, y + k, z))
        # a pink wool ring inlaid in the floor, and little guh faces in the planks
        for (x, z) in cells:
            d = math.dist((x, z), (cx, cz))
            if abs(d - (TREES[tree]["r"] + 2.2)) < 0.5:
                v.set(x, y, z, mc("pink_wool"))
        # railing on the rim, except at the openings (3 wide) towards the bridges
        trunk = v.trunk_cells[tree].get(y, set())
        well = set(v.spiral) if name == "plein" else set()
        rim = [(x, z) for (x, z) in cells if any((x + dx, z + dz) not in cells and (x + dx, z + dz) not in trunk
                                                 and (x + dx, z + dz) not in well for dx, dz in STEP.values())]
        v.deck_rim = getattr(v, "deck_rim", {})
        v.deck_rim[name] = rim
        for (x, z) in rim:
            open_ = False
            for o in openings:
                ox, oz = STEP[o]
                along = (x - cx) if ox == 0 else (z - cz)
                if abs(along) <= 1 and ((x - cx) * ox + (z - cz) * oz) > 0:
                    open_ = True
            if not open_:
                v.set(x, y + 1, z, HEK, fence_props())
        # lampions: hanging under the rim, and on some posts
        for i, (x, z) in enumerate(sorted(rim, key=lambda p: math.atan2(p[1] - cz, p[0] - cx))):
            if i % 7 == 3:
                v.set(x, y - 1, z, *lamp(True, ("geel", "roze", "mint")[i % 3]))
            if i % 9 == 5 and v.get(x, y + 1, z) == HEK:
                v.set(x, y + 2, z, *lamp(False, "roze"))
        # struts under the deck: from the trunk out to the rim
        tr = TREES[tree]["r"]
        for k in range(4):
            a = k * math.pi / 2 + math.pi / 4
            pts = []
            for s10 in range(int((tr - 0.4) * 10), int((ro - 1.2) * 10), 3):
                s_ = s10 / 10
                pts.append((round(cx + math.cos(a) * s_), round(y - 1 - (ro - 1.2 - s_) * 0.8), round(cz + math.sin(a) * s_)))
            pts.append((pts[-1][0], y - 1, pts[-1][2]))
            for (x, yy, z) in line6(pts):
                if v.get(x, yy, z) in (None, LEAVES, "minecraft:air"):
                    v.set(x, yy, z, LOG, {"axis": "y"})



# --- huts: guh heads with faces on several sides, the mouth is the door ---------------------------------------------------
# name: (centre, floor y, radius, door direction, face directions, what's inside)
HUTS = {
    "boswachterspost": ((61, 50), P1, 4, "north", ["north", "east", "south"], "boswachter"),
    "uitkijkhut": ((40, 50), P2, 3, "north", ["north", "west", "south"], "uitkijk"),
    "oosthut": ((86, 39), LOW, 4, "west", ["west", "north", "east"], "nest"),
    "westhut": ((14, 61), LOW, 4, "east", ["east", "south", "west"], "nest"),
    "noordhut": ((61, 14), LOW, 4, "south", ["south", "east", "north"], "bank"),
    "plukkershut": ((39, 86), LOW, 4, "north", ["north", "west", "south"], "plukker"),
}


def huts(v):
    mc = v.h.mc
    v.hut_cells = set()
    for name, ((hx, hz), y0, R, door, face_dirs, what) in HUTS.items():
        wall_top = y0 + 3
        # the head: a round wall with a dome on it; its skin is every block with a side to the outside (not the floor)
        outer = set()
        for x in range(hx - R - 1, hx + R + 2):
            for z in range(hz - R - 1, hz + R + 2):
                d = math.dist((x, z), (hx, hz))
                for y in range(y0 + 1, wall_top + R + 2):
                    rr = R + 0.45 if y <= wall_top else math.sqrt(max(0.0, (R + 0.45) ** 2 - (y - wall_top) ** 2))
                    if d <= rr:
                        outer.add((x, y, z))
        around = [(dx, dy, dz) for dx in (-1, 0, 1) for dy in (-1, 0, 1) for dz in (-1, 0, 1) if 0 < abs(dx) + abs(dy) + abs(dz) <= 2]
        shell = {p for p in outer if any((p[0] + dx, p[1] + dy, p[2] + dz) not in outer and p[1] + dy > y0 for dx, dy, dz in around)}
        inside = outer - shell
        for p in shell:
            v.set(*p, mc("pink_wool"), None)
            v.hut_cells.add(p)
        for p in inside:
            v.set(*p, mc("air"))
            v.hut_cells.add(p)
            v.reserved.add(p)
        for x in range(hx - R - 1, hx + R + 2):
            for z in range(hz - R - 1, hz + R + 2):
                if math.dist((x, z), (hx, hz)) <= R + 0.45:
                    v.set(x, y0, z, PLANK)
        # ears on top (on the line across the door direction)
        dx, dz = STEP[door]
        for sgn in (-1, 1):
            ex, ez = hx + dz * sgn * (R * 0.62), hz + dx * sgn * (R * 0.62)
            ey = wall_top + R * 0.72
            for x in range(int(ex) - 2, int(ex) + 3):
                for y in range(int(ey) - 2, int(ey) + 3):
                    for z in range(int(ez) - 2, int(ez) + 3):
                        if math.dist((x, y, z), (ex, ey, ez)) <= 1.55 and (x, y, z) not in inside:
                            v.set(x, y, z, mc("pink_wool"))
                            v.hut_cells.add((x, y, z))
            for y in range(int(ey), y0, -1):                   # (joined to the dome)
                if (round(ex), y, round(ez)) in shell or (round(ex), y, round(ez)) in inside:
                    break
                v.set(round(ex), y, round(ez), mc("pink_wool"))
                v.hut_cells.add((round(ex), y, round(ez)))
            v.set(round(ex + dx * 1.0), round(ey), round(ez + dz * 1.0), mc("magenta_wool"))
            v.set(round(ex - dx * 1.0), round(ey), round(ez - dz * 1.0), mc("magenta_wool"))
        # the faces
        fy = y0 + 3.2
        for f in face_dirs:
            fx, fz = STEP[f]
            for u in range(-R - 1, R + 2):
                for y in range(y0 + 1, wall_top + R + 1):
                    col = [p for p in shell if p[1] == y and ((p[0] - hx) if fz else (p[2] - hz)) == u]
                    if not col:
                        continue
                    p = max(col, key=lambda q: q[0] * fx + q[2] * fz)
                    # u from the viewer's left to right: looking at the face from outside
                    uu = u * (1 if (fz > 0 or fx < 0) else -1) * (1 if fz else 1)
                    uu = (p[0] - hx) * (-1 if fz < 0 else 1) if fz else (p[2] - hz) * (1 if fx < 0 else -1)
                    role = face_role(uu, y - fy, R * 0.95)
                    if role and role not in ("skin", "ear"):
                        v.set(*p, FACE_WOOL[role])
        # the door: the middle of the mouth, 2 high, a vadshout door
        front = max((p for p in shell if p[1] == y0 + 1 and ((p[0] - hx) if dz else (p[2] - hz)) == 0),
                    key=lambda q: q[0] * dx + q[2] * dz)
        fxd, fzd = front[0], front[2]
        v.set(fxd, y0 + 1, fzd, DOOR, {"facing": door, "half": "lower", "hinge": "left", "open": "false", "powered": "false"})
        v.set(fxd, y0 + 2, fzd, DOOR, {"facing": door, "half": "upper", "hinge": "left", "open": "false", "powered": "false"})
        v.set(fxd, y0 + 3, fzd, mc("black_wool"))         # (the top lip of the mouth)
        v.set(fxd + dx, y0, fzd + dz, PLANK)               # a doorstep
        # windows: round pink glass on the sides without a face
        for f in STEP:
            if f in face_dirs:
                continue
            wx, wz = STEP[f]
            col = [p for p in shell if p[1] == y0 + 2 and ((p[0] - hx) if wz else (p[2] - hz)) == 0]
            if col:
                p = max(col, key=lambda q: q[0] * wx + q[2] * wz)
                if v.get(p[0] + wx, p[1], p[2] + wz) in (None, "minecraft:air") and v.get(*p) == mc("pink_wool"):
                    v.set(*p, mc("pink_stained_glass"))
        v.set(hx, wall_top + R - 1, hz, *lamp(True, "roze"))
        inside_floor = (hx - dx * (R - 2), y0 + 1, hz - dz * (R - 2))
        v.targets[f"binnen in de {name}"] = (hx, y0 + 1, hz)
        furnish(v, name, what, hx, hz, y0, R, door)


def furnish(v, name, what, hx, hz, y0, R, door):
    mc = v.h.mc
    dx, dz = STEP[door]
    back = (hx - dx * (R - 1), hz - dz * (R - 1))
    side = (dz, dx)
    b1 = (back[0] + side[0], back[1] + side[1])
    b2 = (back[0] - side[0], back[1] - side[1])
    c1 = (hx + side[0] * 2, hz + side[1] * 2)
    c2 = (hx - side[0] * 2, hz - side[1] * 2)
    chest = lambda p, loot=NAME: v.set(p[0], y0 + 1, p[1], mc("chest"), {"facing": door, "type": "single", "waterlogged": "false"},
                                       {"id": "minecraft:chest", "LootTable": f"guhs:chests/{loot}"})
    if what == "boswachter":
        v.npc_boswachter = (back[0] + 0.5, y0 + 1, back[1] + 0.5, door)
        chest(b1)
        v.set(b2[0], y0 + 1, b2[1], NEST)
        v.nests.append((b2[0], y0 + 1, b2[1]))
        v.set(c1[0], y0 + 1, c1[1], "guhs:guh_tafel", {"facing": door})
    elif what == "plukker":
        v.npc_plukker = (back[0] + 0.5, y0 + 1, back[1] + 0.5, door)
        chest(b1)
        v.set(b2[0], y0 + 1, b2[1], mc("composter"), {"level": "7"})
        v.set(c1[0], y0 + 1, c1[1], "guhs:guh_tafel", {"facing": door})
        v.set(c2[0], y0 + 1, c2[1], "guhs:guh_taart", {"bites": "0"})
    elif what == "uitkijk":
        chest(back, f"{NAME}_uitkijk")
        v.set(hx + side[0], y0 + 1, hz + side[1], "guhs:pink_kussen", {"facing": door})
    elif what == "nest":
        for p in (back, b1):
            v.set(p[0], y0 + 1, p[1], NEST)
            v.nests.append((p[0], y0 + 1, p[1]))
        v.set(c2[0], y0 + 1, c2[1], "guhs:white_kussen", {"facing": door})
    elif what == "bank":
        v.set(back[0], y0 + 1, back[1], "guhs:guh_bank", {"facing": door})
        v.set(c1[0], y0 + 1, c1[1], "guhs:guh_tafel", {"facing": door})
        chest(c2)


# --- rope bridges ------------------------------------------------------------------------------------------------------------
# (axis, fixed coordinate, from, to (inclusive), height at from, height at to)
BRIDGES = [
    ("x", 50, 65, 76, P0, LOW),        # plaza -> oost
    ("x", 50, 35, 24, P0, LOW),        # plaza -> west
    ("z", 50, 35, 24, P0, LOW),        # plaza -> noord
    ("z", 50, 65, 75, P0, LOW),        # plaza -> zuid
    ("z", 50, 39, 22, P1, UP),         # boswachterspost -> noord boven
    ("z", 50, 61, 78, P1, UP),         # boswachterspost -> zuid boven
]


def bridges(v):
    mc = v.h.mc
    go = {("x", 1): "east", ("x", -1): "west", ("z", 1): "south", ("z", -1): "north"}
    for axis, fixed, a, b, ya, yb in BRIDGES:
        n = abs(b - a) + 1
        step = 1 if b > a else -1
        heights = [ya + round((yb - ya) * (i + 0.5) / n) if 0 < i < n - 1 else (ya if i == 0 else yb) for i in range(n)]
        # (the first and last blocks sit at the deck heights, the rest climbs evenly)
        heights = [ya] + [ya + round((yb - ya) * i / (n - 1)) for i in range(1, n - 1)] + [yb]
        for i in range(n):
            t = a + i * step
            y = heights[i]
            up = heights[i + 1] > y if i < n - 1 else False
            down_before = heights[i - 1] > y if i > 0 else False
            for o in range(-2, 3):
                x, z = (t, fixed + o) if axis == "x" else (fixed + o, t)
                if abs(o) <= 1:
                    if up:
                        v.set(x, y + 1, z, "guhs:vadshout_trap", {"facing": go[(axis, step)], "half": "bottom", "shape": "straight", "waterlogged": "false"})
                        v.set(x, y, z, PLANK)
                    else:
                        v.set(x, y, z, PLANK if (i % 3) else "guhs:vadshout_plaat" if False else PLANK)
                    for k in range(1, 4):
                        if v.get(x, y + k, z) != "guhs:vadshout_trap":
                            v.set(x, y + k, z, mc("air"))
                        v.reserved.add((x, y + k, z))
                else:
                    # the rope rail on posts
                    if i % 4 == 0 or i in (0, n - 1):
                        v.set(x, y + 1, z, HEK, fence_props())
                        v.set(x, y + 2, z, HEK, fence_props())
                        if i % 8 == 4:
                            v.set(x, y + 3, z, *lamp(False, "geel"))
                    else:
                        v.set(x, y + 2, z, TOUW, {"axis": axis, "waterlogged": "false"})
                        v.set(x, y + 1, z, TOUW, {"axis": axis, "waterlogged": "false"})
                    v.set(x, y, z, "guhs:vadshout_plaat", {"type": "top", "waterlogged": "false"})
            # a rope hanging down under the middle of the deck
            if i % 5 == 2:
                x, z = (t, fixed) if axis == "x" else (fixed, t)
                v.set(x, y - 1, z, TOUW, {"axis": "y", "waterlogged": "false"})


# --- ladders on the trunks ---------------------------------------------------------------------------------------------------
# (tree, direction the ladder looks out to, from y (feet), to y (the deck floor it goes through))
LADDERS = [
    ("oost", "south", G + 1, LOW), ("west", "north", G + 1, LOW), ("noord", "east", G + 1, LOW), ("zuid", "west", G + 1, LOW),
    ("moeder", "west", P0 + 1, P1), ("moeder", "north", P1 + 1, P2),
    ("noord", "west", LOW + 1, UP), ("zuid", "east", LOW + 1, UP),
]


def ladders(v):
    mc = v.h.mc
    for tree, f, y0, y1 in LADDERS:
        cx, cz = TREES[tree]["c"]
        dx, dz = STEP[f]
        cells = v.trunk_cells[tree]
        # the ladder stands just outside the trunk on that side, the trunk behind it at every height
        for k in range(1, 9):
            x, z = cx + dx * k, cz + dz * k
            if all((x, z) in cells.get(y, ()) for y in range(y0, y1 + 1)) and not all((x + dx, z + dz) in cells.get(y, ()) for y in range(y0, y1 + 1)):
                break
        lx, lz = x + dx, z + dz
        for y in range(y0, y1 + 1):
            v.set(lx, y, lz, LADDER, {"facing": f, "waterlogged": "false"})
            v.reserved.add((lx, y, lz))
            v.reserved.add((lx, y + 1, lz))
            # room to climb: nothing in front of the ladder
            if v.get(lx + dx, y, lz + dz) in (LEAVES, LOG) and y > y0 + 1:
                v.set(lx + dx, y, lz + dz, mc("air"))
        v.ladders.append((lx, lz, y0, y1, f))
        if y0 == G + 1:
            v.set(lx, G, lz, "minecraft:pink_terracotta")


# --- the knabbelbessen garden (on the zuid deck and around the foot of the trees) --------------------------------------------
def garden(v):
    mc = v.h.mc
    rng = v.rng
    cells = v.deck_cells["zuid"]
    cx, cz = TREES["zuid"]["c"]
    beds = [(x, z) for (x, z) in cells if 55 <= x <= 58 and 82 <= z <= 90]
    for (x, z) in beds:
        v.set(x, LOW, z, "guhs:vadsmos")
        v.set(x, LOW + 1, z, BUSH, {"age": str(2 + (x + z) % 2)})
    for (x, z) in beds:                                     # a little plank border around the beds
        for dx, dz in STEP.values():
            q = (x + dx, z + dz)
            if q in cells and q not in beds and v.get(q[0], LOW + 1, q[1]) in (None, "minecraft:air"):
                pass
    # bushes, grass and flowers on the ground
    for (x, z) in v.footprint:
        if v.get(x, G, z) != "guhs:vadsmos" or v.get(x, G + 1, z) is not None:
            continue
        if any(math.dist((x, z), TREES[t]["c"]) < TREES[t]["r"] + 4 for t in TREES):
            continue
        r = rng.random()
        if r < 0.025:
            v.set(x, G + 1, z, BUSH, {"age": "3"})
        elif r < 0.12:
            v.set(x, G + 1, z, "guhs:roze_gras")
        elif r < 0.14:
            v.set(x, G + 1, z, rng.choice(["guhs:guhoortjes", "guhs:kaasbloem", "guhs:roze_guhbloem"]))
    # lampion posts along the paths
    for i in range(6, W - 6, 9):
        for (x, z) in ((C + 3, i), (i, C + 3), (C - 3, i), (i, C - 3)):
            if (x, z) in v.fp and v.get(x, G + 1, z) in (None, "guhs:roze_gras") and all(math.dist((x, z), TREES[t]["c"]) > TREES[t]["r"] + 3.5 for t in TREES):
                v.set(x, G + 1, z, HEK, fence_props())
                v.set(x, G + 2, z, HEK, fence_props())
                v.set(x, G + 3, z, *lamp(False, "mint"))


# --- guh families and their nests ---------------------------------------------------------------------------------------------
FAMILY_SPOTS = [
    # (deck or ground, nest x, z, variant, parents, babies)
    ("plein", (57, 43), "normal", 2, 3),
    ("oost", (91, 51), "choco", 1, 2),
    ("west", (9, 53), "mint", 2, 2),
    ("noord_boven", (46, 10), "snow", 1, 1),
    ("grond", (70, 64), "normal", 1, 3),
    ("grond", (30, 34), "mint", 2, 1),
]


def families(v):
    ms = v.h.ms
    rng = random.Random(4040)
    v.family_guhs = []
    for fam_id, (where, (nx, nz), variant, parents, babies) in enumerate(FAMILY_SPOTS, start=1):
        y = G + 1 if where == "grond" else DECKS[where][1] + 1
        v.set(nx, y, nz, NEST)
        v.nests.append((nx, y, nz))
        members = [("ouder", i) for i in range(parents)] + [("baby", i + 1) for i in range(babies)]
        for k, (rol, plek) in enumerate(members):
            ox, oz = [(1, 0), (-1, 1), (1, 2), (2, 1), (-1, -1)][k]
            gx, gz = nx + ox, nz + oz
            scale = rng.uniform(0.85, 1.25) if rol == "ouder" else rng.uniform(0.8, 1.0)
            nbt = ms.guh_nbt(scale, Variant=variant, Rotation=ms.floats(rng.uniform(0, 360), 0.0),
                             NeoForgeData={"GuhsGezin": {"Id": ms.Long(900000 + fam_id), "Rol": rol, "Plek": plek}})
            if rol == "baby":
                nbt["Age"] = -24000
            v.s.entity(gx + 0.5, float(y), gz + 0.5, nbt)
            v.family_guhs.append((gx, y, gz))
            v.reserve(gx, y, gz, 1)


def finish(v):
    mc = v.h.mc
    s = v.s
    # the NPCs
    for kind, spot in (("boswachterguh", v.npc_boswachter), ("knabbelplukker", v.npc_plukker)):
        x, y, z, door = spot
        yaw = {"south": 0.0, "west": 90.0, "north": 180.0, "east": 270.0}[door]
        s.entity(x, float(y), z, {"id": "guhs:guh_npc", "Kind": kind, "PersistenceRequired": v.h.ms.Byte(1),
                                  "Rotation": v.h.ms.floats(yaw, 0.0)})
        v.targets[kind] = (int(x), y, int(z))
    # no leaves in the walk space, the huts, above the bridges; leaves that lost their tree go too
    for p in list(v.reserved):
        if s.get(*p) == LEAVES:
            del s.blocks[p]
    logs = {p for p, (b, _, _) in s.blocks.items() if b in (LOG, "guhs:vadshout_gezicht")}
    keep, todo = set(), deque()
    for p in logs:
        for q in neighbours(p):
            if s.get(*q) == LEAVES and q not in keep:
                keep.add(q)
                todo.append((q, 1))
    while todo:
        p, dist = todo.popleft()
        if dist >= 9:
            continue
        for q in neighbours(p):
            if s.get(*q) == LEAVES and q not in keep:
                keep.add(q)
                todo.append((q, dist + 1))
    for p, (b, _, _) in list(s.blocks.items()):
        if b == LEAVES and p not in keep:
            del s.blocks[p]
    connect(v)
    # the anchor: the whole village is placed around it (it becomes the trunk again)
    floor = s.blocks.get((C, G, C))
    final = floor[0] + ("[" + ",".join(f"{k}={val}" for k, val in floor[1].items()) + "]" if floor[1] else "")
    s.set(C, G, C, mc("jigsaw"), {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": final, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    s.anchor_final = final
    s.clear_above(v.footprint, G + 1, top=H)
    s.targets, s.ladders, s.nests, s.family_guhs = v.targets, v.ladders, v.nests, v.family_guhs
    s.deck_free = {}
    for name, (tree, y, ro, discs, openings) in DECKS.items():
        s.deck_free[name] = [(x, y + 1, z) for (x, z) in v.deck_cells[name]
                             if s.get(x, y + 1, z) in PASSABLE and s.get(x, y + 2, z) in PASSABLE and solid(s.get(x, y, z))]
    s.faces = v.faces


def line6(points):
    """The points of a path made 6-connected: between two points that differ in more than one coordinate, the
    in-between blocks are added (x first, then z, then y)."""
    out = []
    for p in points:
        if out:
            x, y, z = out[-1]
            while (x, y, z) != p:
                if x != p[0]:
                    x += 1 if p[0] > x else -1
                elif z != p[2]:
                    z += 1 if p[2] > z else -1
                else:
                    y += 1 if p[1] > y else -1
                out.append((x, y, z))
        else:
            out.append(p)
    return out


def neighbours(p):
    x, y, z = p
    return ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1))


def connect(v):
    """Fences connect to fences, gates and full blocks; like the game does when you place them."""
    s = v.s
    for (x, y, z), (name, props, nbt) in list(s.blocks.items()):
        if name != HEK:
            continue
        p = {"waterlogged": "false"}
        for d, (dx, dz) in STEP.items():
            n = s.get(x + dx, y, z + dz)
            p[d] = "true" if n and (n == HEK or n == "guhs:vadshout_poort" or (solid(n) and n not in (TOUW, "guhs:vadshout_trap", "guhs:vadshout_plaat")
                                                                                and n not in LAMPS and n != "minecraft:chest")) else "false"
        s.blocks[(x, y, z)] = (name, p, nbt)


# =====================================================================================================================
# the geometry self-check
# =====================================================================================================================
def check(s):
    problems = []
    get = s.get
    reach = walk(s, (C, G + 1, 1))
    s.walkable = len(reach)
    if (C, G + 1, 1) not in reach:
        problems.append("the path at the edge of the village is not walkable")
    for name, p in s.targets.items():
        if p not in reach and not any((p[0] + dx, p[1], p[2] + dz) in reach for dx, dz in STEP.values()):
            problems.append(f"{name} {p} can't be reached on foot")
    for name, free in s.deck_free.items():
        got = sum(1 for p in free if p in reach)
        if got < 0.85 * len(free):
            lost = [p for p in free if p not in reach]
            problems.append(f"deck {name}: only {got} of {len(free)} free spots reachable (e.g. {lost[:3]})")
    # the NPCs, guhs and nests stand on something
    for (ex, ey, ez, nbt) in s.entities:
        p = (int(math.floor(ex)), int(round(ey)), int(math.floor(ez)))
        below = get(p[0], p[1] - 1, p[2])
        if not solid(below) or get(*p) not in PASSABLE:
            problems.append(f"{nbt['id']} at {p} doesn't stand on solid ground ({below}, {get(*p)})")
    npcs = [e for e in s.entities if e[3]["id"] == "guhs:guh_npc"]
    if sorted(e[3]["Kind"] for e in npcs) != ["boswachterguh", "knabbelplukker"]:
        problems.append(f"the NPCs: {[e[3]['Kind'] for e in npcs]}")
    babies = [e for e in s.entities if e[3].get("Age") == -24000]
    if len(babies) < 8:
        problems.append(f"only {len(babies)} babyguhs")
    for (x, y, z) in s.nests:
        if get(x, y, z) != NEST or not solid(get(x, y - 1, z)):
            problems.append(f"nest at {(x, y, z)}: {get(x, y, z)} on {get(x, y - 1, z)}")
    # blocks that need a floor, lamps hang or stand on something, ladders hang on something solid
    for (x, y, z), (b, props, _) in s.blocks.items():
        if b in NEEDS_FLOOR and not solid(get(x, y - 1, z)) and get(x, y - 1, z) != "guhs:vadsmos":
            problems.append(f"{b} at {(x, y, z)} stands on {get(x, y - 1, z)}")
        if b in LAMPS:
            support = get(x, y + 1, z) if props.get("hanging") == "true" else get(x, y - 1, z)
            if not solid(support) and support not in (HEK, TOUW):
                problems.append(f"{b} at {(x, y, z)} hangs/stands on {support}")
        if b == LADDER:
            dx, dz = STEP[props["facing"]]
            if not solid(get(x - dx, y, z - dz)):
                problems.append(f"ladder at {(x, y, z)} hangs on {get(x - dx, y, z - dz)}")
        if b == DOOR and props["half"] == "lower" and (get(x, y + 1, z) != DOOR or not solid(get(x, y - 1, z))):
            problems.append(f"door at {(x, y, z)} is broken")
    # nothing floats (every group of blocks is held up by the ground)
    real = {p for p, (b, _, _) in s.blocks.items() if b != "minecraft:air"}
    seen = set()
    for p in real:
        if p in seen:
            continue
        comp, todo, grounded = [], [p], False
        seen.add(p)
        while todo:
            q = todo.pop()
            comp.append(q)
            if q[1] <= G:
                grounded = True
            for n in neighbours(q):
                if n in real and n not in seen:
                    seen.add(n)
                    todo.append(n)
        if not grounded:
            problems.append(f"floating blocks: {len(comp)} from {sorted(comp)[:2]} ({s.get(*sorted(comp)[0])})")
    # every deck is used: guh faces enough, trees tall enough
    if s.faces < 25:
        problems.append(f"only {s.faces} little faces in the bark")
    if get(C, G, C) != "minecraft:jigsaw" or not s.anchor_final.startswith((LOG, PLANK)):
        problems.append(f"the anchor: {get(C, G, C)} -> {s.anchor_final}")
    return problems


def walk(s, start):
    """Every feet position you can reach from start: steps of 1 up / 3 down, climbing ladders (no jumps over gaps)."""
    get = s.get

    def passable(p):
        return get(*p) in PASSABLE

    def standable(p):
        x, y, z = p
        if not (0 <= x < W and 0 <= z < D and 1 <= y < H - 1):
            return False
        if not passable(p) or not passable((x, y + 1, z)):
            return False
        below = get(x, y - 1, z)
        return solid(below) or get(x, y, z) == LADDER or below == LADDER

    seen = {start} if standable(start) else set()
    todo = deque(seen)
    while todo:
        x, y, z = todo.popleft()
        here = get(x, y, z)
        nexts = []
        for dx, dz in STEP.values():
            for dy in (1, 0, -1, -2, -3):
                n = (x + dx, y + dy, z + dz)
                if dy == 1 and not passable((x, y + 2, z)):
                    continue
                if dy < 0 and any(not passable((x + dx, y + k, z + dz)) for k in range(dy + 1, 2)):
                    continue
                if standable(n):
                    nexts.append(n)
                    break
        if here == LADDER or get(x, y - 1, z) == LADDER:
            if get(x, y, z) == LADDER and standable((x, y + 1, z)):
                nexts.append((x, y + 1, z))
            if get(x, y - 1, z) == LADDER and standable((x, y - 1, z)):
                nexts.append((x, y - 1, z))
        for n in nexts:
            if n not in seen:
                seen.add(n)
                todo.append(n)
    return seen


# =====================================================================================================================
# FTB Quests (rows y = 68 and 70), no dependencies
# =====================================================================================================================
def ftb(fq):
    q = fq.q
    q("vadswoud_bezoek", "Het Vadswoud", "Ergens in de Guhmensie staat een mistig, mintgroen woud vol &areuzenguhbomen&r: het &dVadswoud&r. Loop er eens doorheen en kijk goed naar de schors... zie je de guhgezichtjes?",
      "guhs:vadshout_zaailing", [fq.biome(BIOME)], rewards=(("guhs:kaas_knabbels", 8),), x=-8, y=68, shape="circle", xp=50)
    q("vadswoud_dorp", "Hoog in de bomen", "In het Vadswoud woont een dorp guhs in &aboomhutten&r, hoog in de reuzenguhbomen. Klim de wenteltrap op en loop over de touwbruggen! Het superkompas (Wonen) wijst de weg.",
      "guhs:vadstouw", [fq.structure(NAME)], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=-6, y=68, shape="gear", xp=150)
    q("vadswoud_boswachter", "De Boswachterguh", "In de boswachterspost, hoog in de Moederboom, zit de &2Boswachterguh&r. Hij weet alles over guhfamilies en verkoopt vadshout, zaailingen en guhnestjes.",
      "guhs:guhdex", [fq.adv("seen_boswachterguh")], rewards=(("guhs:kaas_knabbels", 16),), x=-4, y=68, xp=100)
    q("vadswoud_plukker", "De Knabbelplukker", "Naast de bessentuin in het boomhutdorp woont de &6Knabbelplukker&r. Ze ruilt knabbelbessen tegen taartjes, nestjes en haar plukpakje.",
      "guhs:guhdex", [fq.adv("seen_knabbelplukker")], rewards=(("guhs:knabbelbessen", 8),), x=-2, y=68, xp=100)
    q("vadswoud_bessen", "Knabbelbessen plukken", "Knabbelbessenstruiken prikken niet! Rechtsklik op een struik met gele bessen om ze te plukken. Verzamel er 32.",
      "guhs:knabbelbessen", [fq.item("guhs:knabbelbessen", 32)], rewards=(("guhs:knabbelbessentaartje", 2),), x=0, y=68, xp=100)
    q("vadswoud_taartje", "Vadsig taartje", "Drie knabbelbessen, suiker en een kaasknabbel: bak een &6knabbelbessentaartje&r.",
      "guhs:knabbelbessentaartje", [fq.item("guhs:knabbelbessentaartje")], rewards=(("guhs:kaas_knabbels", 12),), x=2, y=68, xp=50)
    q("vadswoud_houtset", "Vadshout", "Hak een reuzenguhboom om (of koop hout bij de Boswachterguh) en maak vadshoutplanken, een vadshoutdeur en een vadshouthek.",
      "guhs:vadshout_planken", [fq.item("guhs:vadshout_planken", 32), fq.item("guhs:vadshout_deur"), fq.item("guhs:vadshout_hek", 4)],
      rewards=(("guhs:vadshout_zaailing", 4),), x=4, y=68, xp=100)
    q("vadswoud_reuzenboom", "Een boom van een guh", "Plant vier vadshoutzaailingen in een vierkant en geef ze wat tijd (of beendermeel). Dan groeit er een echte &areuzenguhboom&r!",
      "guhs:vadshout_gezicht", [fq.adv("vadswoud_reuzenboom")], rewards=(("guhs:vahoege_vads_ingot", 1),), x=6, y=68, shape="gear", xp=250)
    q("vadswoud_gezin", "Guhfamilie", "Wilde guhs leven in gezinnetjes: een of twee ouders met babyguhs die in een rijtje achter hen aan lopen. Zoek een guhfamilie en loop met ze mee.",
      "guhs:guh_spawn_egg", [fq.adv("vadswoud_gezin")], rewards=(("guhs:kaas_knabbels", 16),), x=-8, y=70, shape="circle", xp=100)
    q("vadswoud_baby", "Klein maar vads", "Babyguhs zijn veel makkelijker te temmen dan grote guhs. Geef een wilde babyguh &eknabbelbessen&r of kaasknabbels.",
      "guhs:knabbelbessen", [fq.adv("vadswoud_baby_getemd")], rewards=(("guhs:knabbelbessen", 16),), x=-6, y=70, xp=150)
    q("vadswoud_nestje", "Een eigen guhnestje", "Maak een &6guhnestje&r (stokjes, wol en een kaasknabbel) of koop er een bij de Boswachterguh.",
      "guhs:guhnestje", [fq.item("guhs:guhnestje")], rewards=(("guhs:kaas_knabbels", 8),), x=-4, y=70, xp=50)
    q("vadswoud_slapen", "Welterusten, guh", "Zet je guhnestje neer bij je tamme guhs. Als het nacht wordt, kruipen ze erin om te slapen. Zzz... VAHOEG!",
      "minecraft:clock", [fq.adv("vadswoud_geslapen")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=-2, y=70, xp=150)
    q("vadswoud_boswachterspakje", "Boswachter in spe", "Koop de &2boswachtershoed&r en de &2boswachtersjas&r bij de Boswachterguh en trek ze je guh aan.",
      "guhs:boswachtershoed", [fq.item("guhs:boswachtershoed"), fq.item("guhs:boswachtersjas")], rewards=(("guhs:vadshout_zaailing", 4),),
      x=0, y=70, shape="rsquare", xp=150)
    q("vadswoud_plukpakje", "Plukker van het jaar", "Ruil de &6plukmuts&r en het &6plukmandje&r bij de Knabbelplukker. Met het mandje op zijn rug ziet je guh er echt vads uit!",
      "guhs:plukmandje", [fq.item("guhs:plukmuts"), fq.item("guhs:plukmandje")], rewards=(("guhs:knabbelbessentaartje", 4),),
      x=2, y=70, shape="rsquare", xp=150)


if __name__ == "__main__":
    # the self-check on its own:  python tools/features/vadswoud.py   (from the project root)
    import sys
    import types
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    import make_structures as ms
    stub = types.SimpleNamespace(mc=ms.mc, Structure=ms.Structure, ms=ms, floats=ms.floats)
    tpl = template(stub)
    found = check(tpl)
    print("\n".join(found[:80]) if found else "geometry check ok")
    print(f"{len(tpl.blocks)} blocks, {len(tpl.entities)} entities, {tpl.walkable} walkable, {tpl.faces} faces")
