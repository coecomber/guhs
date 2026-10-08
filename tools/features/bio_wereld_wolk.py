"""
biomes3 wereld: the Wolkenweide (guhs:wolkenweide). Not in FEATURES: bio_wereld.py calls biome(), surface() and build().
Java: feature/bio/wereld/WolkTerrein (the meadow, the stacks of floating islands with their ways up and down, the water,
where the clouds go), WolkVulling (clouds, lift columns, water, crystal tips, vines, plants, trees), WolkRoute (the jump
rule), WolkBlokken (the blocks, sounds and particles below), client/WolkClient + WolkLucht (particles, the sky).

What this file writes:
  - the biome file: a pastel sky and haze, its own music and ambience loop, two ambient particles (floating fluff and
    glints), and the sky renderer guhs:wolkenweide (a warmer sunset and more stars, WolkLucht.java). No placed features
    besides the ores: every plant and tree of this biome is placed by WolkVulling from the terrain model, so nothing lands
    in a lift column or on a stepping stone;
  - the surface rule: wolkenweide_gras on top of the meadow and of every island, and under an island's top soft layers
    of rock by depth: rose, a cream seam, lilac, a seam of parelmoer, and on (LAGEN; the same list paints the islands of
    templates, bio_wereld_eiland.py);
  - seven blocks: wolkenweide_gras, wolkenweide_steen (rose), wolkenweide_steen_lila, wolkenweide_steen_room (cream), wolkenweide_kristal (softly glowing),
    wolkenweide_kristalpunt (the little point that hangs under a crystal), wolkenweide_rank (a pale hanging vine); their
    textures, models, loot, tags and names;
  - the particles wolkenweide_pluisje and wolkenweide_glinster; sounds.json for wolkenweide.muziek / .sfeer (the OGGs:
    bio_wereld_wolk_geluid.py).
Keep step 1 of the features exactly ["guhs:bio_wereld_vulling"]. Everything random comes from lib.rng(name).
"""
import os

import numpy as np
from PIL import Image

from features import bio_lib

EILAND_VANAF_Y = 74   # above the meadow's highest ground (WolkTerrein.WEIDE_Y + GLOOIING, and the foot of a stair)

GRAS = "guhs:wolkenweide_gras"
STEEN = "guhs:wolkenweide_steen"
LILA = "guhs:wolkenweide_steen_lila"
ROOM = "guhs:wolkenweide_steen_room"
KRISTAL = "guhs:wolkenweide_kristal"
KRISTALPUNT = "guhs:wolkenweide_kristalpunt"
RANK = "guhs:wolkenweide_rank"
BLOKKEN = ["wolkenweide_gras", "wolkenweide_steen", "wolkenweide_steen_lila", "wolkenweide_steen_room", "wolkenweide_kristal", "wolkenweide_kristalpunt", "wolkenweide_rank"]
# the layers under an island's top block: (how many blocks, block), from the top down; below the last: rose
LAGEN = [(2, STEEN), (1, ROOM), (3, LILA), (1, "guhs:parelmoer"), (3, STEEN), (2, ROOM), (4, LILA), (1, "guhs:parelmoer"), (4, STEEN), (2, ROOM),
         (5, LILA)]

TEKSTEN = {
    "block.guhs.wolkenweide_gras": "Wolkenweidegras",
    "block.guhs.wolkenweide_steen": "Wolkensteen",
    "block.guhs.wolkenweide_steen_lila": "Lila wolkensteen",
    "block.guhs.wolkenweide_steen_room": "Roomwitte wolkensteen",
    "block.guhs.wolkenweide_kristal": "Wolkenkristal",
    "block.guhs.wolkenweide_kristalpunt": "Wolkenkristalpunt",
    "block.guhs.wolkenweide_rank": "Wolkenrank",
    "subtitles.guhs.wolkenweide.sfeer": "Wind en klokjes in de verte",
}


def laag(diepte):
    """The block d blocks under an island's top block (d >= 1): what the surface rule makes there."""
    d = diepte
    for (n, blok) in LAGEN:
        if d <= n:
            return blok
        d -= n
    return STEEN


def biome(h, lib, ores, lege_spawners):
    """The biome file (the 1.21.1 shape; tools/mc26.py converts the effects and keeps the attributes)."""
    return {
        "has_precipitation": False, "temperature": 0.8, "downfall": 0.3, "creature_spawn_probability": 0.3,
        # a soft periwinkle sky over a pale pink-lilac haze; clear light water
        "effects": {"sky_color": 0xB9CCFF, "fog_color": 0xF1E2F6, "water_color": 0x9FE3F2, "water_fog_color": 0x6FB8DA,
                    "grass_color": 0xF3D9E6, "foliage_color": 0xF6CFE0,
                    "ambient_sound": "guhs:wolkenweide.sfeer",
                    "music": {"sound": "guhs:wolkenweide.muziek", "min_delay": 6000, "max_delay": 14000, "replace_current_music": False}},
        "attributes": {
            # floating fluff and glints (WolkClient.java draws them; the game spawns them around the camera)
            "minecraft:visual/ambient_particles": [{"particle": {"type": "guhs:wolkenweide_pluisje"}, "probability": 0.0011},
                                                   {"particle": {"type": "guhs:wolkenweide_glinster"}, "probability": 0.0016}],
            # the Guhmensie's sky with a warmer sunset and more stars (WolkLucht.java)
            "neoforge:custom_skybox": "guhs:wolkenweide"},
        # (wolkenschaapjes and the wild wolkguh of slice dieren come in through its own biome modifiers; no Mika's here)
        "spawners": {**lege_spawners, "creature": [{"type": "guhs:guh", "weight": 100, "minCount": 2, "maxCount": 4}]},
        "spawn_costs": {}, "carvers": {"air": []},
        "features": [[], ["guhs:bio_wereld_vulling"], [], [], [], [], ores, [], [], [], []]}


def surface(h, lib):
    """The surface rule inside the biome: grass on top of the meadow and of every island; under an island's top the soft
    layers of LAGEN, by depth (so they lie level under a flat top and follow a hill), shifted one block where a noise is
    high so the seams wander a little."""
    def diepte(offset):
        return {"type": "minecraft:stone_depth", "offset": offset, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"}

    def blok(name):
        return {"type": "minecraft:block", "result_state": {"Name": name}}

    def lagen(schuif):
        rules, tot = [], schuif
        for (n, b) in LAGEN:
            tot += n
            rules.append({"type": "minecraft:condition", "if_true": diepte(tot), "then_run": blok(b)})
        return {"type": "minecraft:sequence", "sequence": rules + [blok(STEEN)]}

    hoog = {"type": "minecraft:y_above", "anchor": {"absolute": EILAND_VANAF_Y}, "surface_depth_multiplier": 0, "add_stone_depth": False}
    golf = {"type": "minecraft:noise_threshold", "noise": "guhs:klaterdal_detail", "min_threshold": 0.12, "max_threshold": 10.0}
    return {"type": "minecraft:sequence", "sequence": [
        {"type": "minecraft:condition", "if_true": diepte(0), "then_run": blok(GRAS)},
        {"type": "minecraft:condition", "if_true": hoog, "then_run": {"type": "minecraft:sequence", "sequence": [
            {"type": "minecraft:condition", "if_true": golf, "then_run": lagen(1)}, lagen(0)]}}]}


# =====================================================================================================================
# textures
# =====================================================================================================================
def _veld(naam, grof_x=2.2, grof_y=2.2, n=16):
    """A soft field of n x n values 0..1 that tiles on all four sides; grof_x / grof_y: how coarse along x and y."""
    f = bio_lib.rng(naam).random((n, n))
    k = np.fft.fftfreq(n) * n
    g = np.exp(-(k[:, None] ** 2) / (2 * grof_y ** 2) - (k[None, :] ** 2) / (2 * grof_x ** 2))
    v = np.real(np.fft.ifft2(np.fft.fft2(f) * g))
    return (v - v.min()) / (v.max() - v.min() + 1e-9)


def _meng(a, b, t):
    return np.asarray(a, np.float32)[None, None, :] * (1 - t[..., None]) + np.asarray(b, np.float32)[None, None, :] * t[..., None]


def _beeld(rgb, alpha=255):
    a = np.clip(np.rint(rgb), 0, 255).astype(np.uint8)
    al = np.full(a.shape[:2] + (1,), alpha, np.uint8) if np.isscalar(alpha) else np.clip(np.rint(alpha), 0, 255).astype(np.uint8)[..., None]
    return Image.fromarray(np.concatenate([a, al], axis=2), "RGBA")


GRAS_LICHT, GRAS_DONKER = (250, 233, 240), (240, 212, 226)
STEEN_TINT = {"wolkenweide_steen": ((244, 205, 219), (229, 178, 199)), "wolkenweide_steen_lila": ((214, 201, 240), (190, 173, 224)),
              "wolkenweide_steen_room": ((255, 244, 232), (246, 226, 212))}


def _gesteente(naam):
    """Soft rock in fine lying streaks (coarse along x, fine along y), with a few paler flecks; no outline, tiles."""
    licht, donker = STEEN_TINT[naam]
    v = 0.7 * _veld(naam + "_streep", 0.9, 4.5) + 0.3 * _veld(naam + "_vlek", 2.0, 2.0)
    v = np.clip((v - 0.2) / 0.6, 0, 1)
    rgb = _meng(licht, donker, v)
    vlek = _veld(naam + "_glans", 5.0, 5.0) > 0.86
    rgb[vlek] = rgb[vlek] * 0.6 + 255 * 0.4
    return rgb


def textures(h):
    r = bio_lib.rng("wolkenweide_tex")
    # the grass: pale and calm, a breath of darker and a few bright flecks
    v = np.clip((_veld("wolkenweide_gras", 2.4, 2.4) - 0.2) / 0.6, 0, 1)
    top = _meng(GRAS_LICHT, GRAS_DONKER, v)
    for _ in range(7):
        x, y = int(r.integers(16)), int(r.integers(16))
        top[y, x] = (255, 250, 252)
    h.save(_beeld(top), "block", "wolkenweide_gras_top.png")
    for naam in STEEN_TINT:
        h.save(_beeld(_gesteente(naam)), "block", f"{naam}.png")
    # the side of the grass block: rose rock with the grass hanging over it in a ragged edge
    zij = _gesteente("wolkenweide_steen")
    rand = [3, 4, 3, 2, 3, 4, 4, 3, 2, 2, 3, 4, 3, 3, 2, 3]
    for x in range(16):
        for y in range(rand[x]):
            zij[y, x] = top[y, x] * (0.97 if y == rand[x] - 1 else 1.0)
    h.save(_beeld(zij), "block", "wolkenweide_gras_zij.png")
    # the crystal: warm white in the middle of every facet, a blush of rose and lilac towards its edges
    yy, xx = np.mgrid[0:16, 0:16]
    facet = (np.abs(((xx + yy) % 8) - 3.5) + np.abs(((xx - yy) % 8) - 3.5)) / 7.0
    kr = _meng((255, 250, 232), (255, 222, 236), np.clip(facet, 0, 1))
    koel = _veld("wolkenweide_kristal", 2.0, 2.0) > 0.7
    kr[koel] = kr[koel] * 0.75 + np.asarray((226, 214, 250), np.float32) * 0.25
    h.save(_beeld(kr), "block", "wolkenweide_kristal.png")
    # the hanging point: a slim crystal that tapers downward
    punt = ["................", "......oIIo......", "......oIIo......", ".....oIWIIo.....", ".....oIWIIo.....", ".....oIWIio.....", "......oWIo......",
            "......oWIo......", "......oWio......", "......oIIo......", ".......Io.......", ".......Io.......", ".......i........", ".......i........",
            "................", "................"]
    h.save(h.grid(punt, {"W": (255, 253, 240, 255), "I": (255, 236, 226, 255), "i": (252, 216, 232, 255), "o": (238, 200, 226, 255)}), "block",
           "wolkenweide_kristalpunt.png")
    # the vine: two pale strands with small leaves; the tip ends half way
    rank = ["....s......s....", "....s.....bs....", "...bs......s....", "....s......sb...", "....sb.....s....", "....s.....bs....", "...bs......s....",
            "....s......s....", "....s......sb...", "....sb....bs....", "....s......s....", "...bs......s....", "....s......sb...", "....sb.....s....",
            "....s.....bs....", "....s......s...."]
    punt_rank = rank[:5] + ["....s.....bs....", "...bs......s....", "....s......b....", "....sb..........", "....s...........", "....b...........",
                            "................", "................", "................", "................", "................"]
    kleuren = {"s": (236, 188, 212, 255), "b": (255, 232, 242, 255)}
    h.save(h.grid(rank, kleuren), "block", "wolkenweide_rank.png")
    h.save(h.grid(punt_rank, kleuren), "block", "wolkenweide_rank_punt.png")
    # particles: three bits of fluff, two glints
    for i in range(3):
        n = 8
        y8, x8 = np.mgrid[0:n, 0:n]
        cx, cy = 3.5 + float(r.uniform(-0.6, 0.6)), 3.5 + float(r.uniform(-0.6, 0.6))
        d = np.hypot((x8 - cx) / (2.6 + 0.5 * i), (y8 - cy) / 2.2) + 0.25 * (r.random((n, n)) - 0.5)
        al = np.clip(1.25 - d, 0, 1) ** 1.5 * 235
        h.save(_beeld(np.full((n, n, 3), 255, np.float32), al), "particle", f"wolkenweide_pluisje_{i}.png")
    for i, rijen in enumerate((["...I...", "...I...", "..iIi..", "IIIWIII", "..iIi..", "...I...", "...I..."],
                               ["..i.i..", "...I...", ".iIWIi.", "...I...", "..i.i..", ".......", "......."])):
        h.save(h.grid(rijen, {"W": (255, 255, 255, 255), "I": (255, 255, 255, 220), "i": (255, 255, 255, 120)}), "particle", f"wolkenweide_glinster_{i}.png")


# =====================================================================================================================
def models(h):
    A, w = h.A, h.w
    w(f"{A}/models/block/wolkenweide_gras.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/wolkenweide_gras_top", "side": "guhs:block/wolkenweide_gras_zij", "bottom": "guhs:block/wolkenweide_steen"}})
    w(f"{A}/blockstates/wolkenweide_gras.json", {"variants": {"": {"model": "guhs:block/wolkenweide_gras"}}})
    w(f"{A}/models/item/wolkenweide_gras.json", {"parent": "guhs:block/wolkenweide_gras"})
    for b in ("wolkenweide_steen", "wolkenweide_steen_lila", "wolkenweide_steen_room", "wolkenweide_kristal"):
        h.simple_block(b)
    for name in ("wolkenweide_kristalpunt", "wolkenweide_rank", "wolkenweide_rank_punt"):
        w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": f"guhs:block/{name}"}})
    w(f"{A}/blockstates/wolkenweide_kristalpunt.json", {"variants": {"": {"model": "guhs:block/wolkenweide_kristalpunt"}}})
    w(f"{A}/blockstates/wolkenweide_rank.json", {"variants": {"tip=false": {"model": "guhs:block/wolkenweide_rank"},
                                                              "tip=true": {"model": "guhs:block/wolkenweide_rank_punt"}}})
    h.item_model("wolkenweide_kristalpunt", "guhs:block/wolkenweide_kristalpunt")
    h.item_model("wolkenweide_rank", "guhs:block/wolkenweide_rank")
    w(f"{A}/particles/wolkenweide_pluisje.json", {"textures": [f"guhs:wolkenweide_pluisje_{i}" for i in range(3)]})
    w(f"{A}/particles/wolkenweide_glinster.json", {"textures": [f"guhs:wolkenweide_glinster_{i}" for i in range(2)]})


def data(h):
    for b in BLOKKEN:
        h.self_drop(b)
    ids = [f"guhs:{b}" for b in BLOKKEN]
    for kind in ("block", "item"):
        h.add_tag(f"minecraft/tags/{kind}/dirt", [GRAS])
    # plants and trees stand on the grass; animals and guhs may appear on it
    h.add_tag("minecraft/tags/block/animals_spawnable_on", [GRAS])
    h.add_tag("minecraft/tags/block/mineable/shovel", [GRAS])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [STEEN, LILA, ROOM, KRISTAL, KRISTALPUNT])
    h.add_tag("minecraft/tags/block/mineable/hoe", [RANK])
    h.add_tag("minecraft/tags/block/sword_efficient", [RANK])
    h.add_tag("guhs/tags/block/wolkenweide", ids)

    def patch(d):
        d["wolkenweide.muziek"] = {"sounds": [{"name": "guhs:wolkenweide/muziek", "stream": True}]}
        d["wolkenweide.sfeer"] = {"sounds": ["guhs:wolkenweide/sfeer"], "subtitle": "subtitles.guhs.wolkenweide.sfeer"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def build(h, lib):
    """Everything else of the biome: its blocks, particles and sounds."""
    textures(h)
    models(h)
    data(h)
    lib.teksten(h, TEKSTEN)
    selfcheck(h)


def selfcheck(h):
    A, D = h.A, h.D
    mis = []
    for b in BLOKKEN:
        for p in (f"{A}/blockstates/{b}.json", f"{A}/models/item/{b}.json", f"{D}/loot_table/blocks/{b}.json"):
            if not os.path.exists(p):
                mis.append(p)
    for f in ("muziek", "sfeer"):
        p = f"{A}/sounds/wolkenweide/{f}.ogg"
        if not os.path.exists(p):
            mis.append(p + " (python tools/features/bio_wereld_wolk_geluid.py)")
    if mis:
        raise SystemExit("bio_wereld_wolk self-check failed, missing:\n  " + "\n  ".join(mis))
