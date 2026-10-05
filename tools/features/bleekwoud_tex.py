"""
Het Bleekwoud (1.2.8) - textures and the two wooden creatures.

Everything is made from Minecraft 1.21.1's own textures (the jar every generator uses), drained of colour: pale, almost
white bark, grey-pink leaves, pale moss, and warm cheese-orange kaashars. The Kraakguh is the plain guh model (the ten
base bones of guh.geo.json) and the Kraak-Mika the Mika model, both painted as pale wood: grain along every face, darker
bark edges, tufts of moss (extra little cubes with their own swatches), a carved face. Their eyes are a glow mask
(<name>_glowmask.png): the renderer only draws it while the creature is awake.
"""
import json
import os
import random

import numpy as np
from PIL import Image

BARK = ((150, 141, 139), (232, 226, 222))
BARK_TOP = ((176, 164, 160), (240, 234, 228))
STRIPPED = ((204, 194, 188), (246, 240, 234))
PLANKS = ((196, 186, 180), (240, 233, 226))
DOOR = ((180, 170, 166), (238, 231, 225))
LEAF = ((126, 112, 118), (226, 208, 214))
MOSS = ((132, 128, 124), (206, 200, 196))
HARS = ((196, 112, 22), (255, 214, 96))
ORANJE = (252, 120, 18)
ZUUR = (180, 210, 60)
UV, SCALE = 128, 4
BASE_BONES = ["root", "body", "tail", "leg_back_left", "leg_back_right", "head", "ear_left", "ear_right", "leg_front_left", "leg_front_right"]


# =====================================================================================================================
# block and item textures
# =====================================================================================================================
def sleepy_face(img):
    """A sleepy little guh face carved in pale bark: closed eyes, a tiny nose and mouth, grey cheeks (no pink left)."""
    px = img.load()
    for x in range(2, 14):
        for y in range(3, 14):
            if ((x - 7.5) / 6.2) ** 2 + ((y - 8.3) / 5.8) ** 2 <= 1:      # a smooth knot of lighter wood behind the face
                c = px[x, y]
                px[x, y] = tuple(min(255, int(v * 0.4 + 240 * 0.6)) for v in c[:3]) + (255,)
    dark, cheek, nose = (70, 62, 66, 255), (206, 190, 194, 255), (150, 136, 140, 255)
    for x in (4, 5, 6, 9, 10, 11):
        px[x, 7] = dark
    px[4, 6] = px[11, 6] = dark                                           # eyelids curving down: fast asleep
    for (x, y) in ((3, 9), (4, 9), (11, 9), (12, 9)):
        px[x, y] = cheek
    px[7, 9] = px[8, 9] = nose
    for (x, y) in ((6, 11), (7, 12), (8, 12), (9, 11)):
        px[x, y] = dark


def heart_tex(h, state, colour, top=False, frame=0):
    """A guh heart in a hollow of the trunk. state 0 uprooted (dull), 1 asleep (grey, eyes closed), 2 awake (glowing,
    beating: frame 1 is the bigger beat). colour: the awake colour (orange, or sour yellow-green)."""
    base = h.ramp(h.vanilla("block/dark_oak_log_top" if top else "block/stripped_dark_oak_log"), *(BARK_TOP if top else STRIPPED)).copy()
    px = base.load()
    for x in range(16):
        for y in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d < 5.6:                                                    # the hollow
                px[x, y] = (62 + (x * 7 + y * 3) % 9, 52 + (x * 5 + y) % 7, 56 + (x + y * 3) % 8, 255)
            elif d < 6.6:
                px[x, y] = tuple(int(v * 0.72) for v in px[x, y][:3]) + (255,)
    if state == 0:
        col, hi = (126, 116, 118), (150, 140, 142)
    elif state == 1:
        col, hi = tuple(int(c * 0.45 + 120 * 0.55) for c in colour), tuple(int(c * 0.5 + 170 * 0.5) for c in colour)
    else:
        col, hi = colour, tuple(min(255, c + 70) for c in colour)
    big = state == 2 and frame == 1
    shape = ["..xx.xx..", ".xxxxxxx.", ".xxxxxxx.", ".xxxxxxx.", "..xxxxx..", "...xxx...", "....x...."]
    if big:
        shape = [".xxx.xxx.", "xxxxxxxxx", "xxxxxxxxx", "xxxxxxxxx", ".xxxxxxx.", "..xxxxx..", "...xxx...", "....x...."]
    x0, y0 = 4 if not big else 4, 5 if not big else 4
    x0 -= 0 if not big else 0
    for dy, row in enumerate(shape):
        for dx, ch in enumerate(row):
            if ch == "x":
                x, y = x0 + dx - (0 if not big else 0), y0 + dy
                if 0 <= x < 16 and 0 <= y < 16:
                    px[x, y] = (hi if dy <= 1 and dx in (2, 3, 1) else col) + (255,)
    if not top:
        eye = (60, 40, 30, 255)
        ey = y0 + 2
        if state == 2:                                                     # two little open eyes and a smile
            px[x0 + 2, ey] = px[x0 + 6, ey] = eye
            px[x0 + 3, ey + 2] = px[x0 + 4, ey + 2] = px[x0 + 5, ey + 2] = eye
        elif state == 1:                                                   # asleep: closed eyes
            px[x0 + 2, ey] = px[x0 + 3, ey] = px[x0 + 5, ey] = px[x0 + 6, ey] = eye
    return base


def flower(open_):
    """The oogbloempje: a thin grey-green stem, grey petals around a closed eye (day) or orange petals around a guh eye."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    stem, leaf = (118, 128, 110, 255), (140, 150, 130, 255)
    for y in range(9, 16):
        px[8 if y % 4 else 7, y] = stem
    px[6, 12] = px[5, 11] = px[10, 13] = px[11, 12] = leaf
    petal = (250, 132, 30, 255) if open_ else (150, 144, 146, 255)
    petal_d = (214, 92, 16, 255) if open_ else (116, 110, 114, 255)
    for (x, y) in ((6, 2), (7, 2), (8, 2), (9, 2), (5, 3), (10, 3), (4, 4), (11, 4), (4, 5), (11, 5), (4, 6), (11, 6), (5, 7), (10, 7),
                   (6, 8), (7, 8), (8, 8), (9, 8), (3, 5), (12, 5), (7, 1), (8, 1), (7, 9), (8, 9)):
        px[x, y] = petal
    for (x, y) in ((5, 2), (10, 2), (3, 4), (12, 4), (3, 6), (12, 6), (5, 8), (10, 8)):
        px[x, y] = petal_d
    for x in range(5, 11):
        for y in range(3, 8):
            if (x, y) not in ((5, 3), (10, 3), (5, 7), (10, 7)):
                px[x, y] = (252, 250, 246, 255) if open_ else (176, 170, 172, 255)
    if open_:                                                               # a round guh eye: lashes, a big pupil, a shine
        for x in range(6, 10):
            px[x, 3] = (70, 44, 30, 255)
        for (x, y) in ((7, 4), (8, 4), (7, 5), (8, 5), (7, 6), (8, 6), (6, 5), (6, 6), (9, 5), (9, 6)):
            px[x, y] = (40, 28, 34, 255)
        px[7, 4] = px[6, 5] = (255, 255, 255, 255)                          # the shine
        px[7, 7] = px[8, 7] = (236, 150, 170, 255)                          # a little blush under it
    else:
        for x in range(6, 10):
            px[x, 5] = (70, 62, 66, 255)                                    # the closed eyelid
        px[5, 4] = px[10, 4] = (70, 62, 66, 255)
    return img


def carpet_side(moss, tall):
    """The tufts of a moss carpet climbing a wall: the moss texture with a ragged top edge (small: only the lower part)."""
    rng = random.Random(1281 + tall)
    img = moss.copy()
    px = img.load()
    for x in range(16):
        top = (1 if tall else 9) + rng.randint(0, 3 if tall else 4)
        for y in range(16):
            if y < top or (y == top and rng.random() < 0.4):
                px[x, y] = (0, 0, 0, 0)
    return img


def textures(h):
    v, ramp, save = h.vanilla, h.ramp, h.save
    bark = ramp(v("block/dark_oak_log"), *BARK).copy()
    save(bark, "block", "bleekhout_stam.png")
    save(ramp(v("block/dark_oak_log_top"), *BARK_TOP), "block", "bleekhout_stam_top.png")
    save(ramp(v("block/stripped_dark_oak_log"), *STRIPPED), "block", "bleekhout_gestript.png")
    save(ramp(v("block/stripped_dark_oak_log_top"), *STRIPPED), "block", "bleekhout_gestript_top.png")
    save(ramp(v("block/oak_planks"), *PLANKS), "block", "bleekhout_planken.png")
    face = bark.copy()
    sleepy_face(face)
    save(face, "block", "bleekhout_gezicht.png")
    top = ramp(v("block/dark_oak_door_top"), *DOOR).copy()
    px = top.load()
    for (x, y) in ((4, 6), (5, 6), (6, 6), (9, 6), (10, 6), (11, 6), (7, 9), (8, 9)):       # a sleepy face in the window
        if px[x, y][3]:
            px[x, y] = (84, 74, 78, 255)
    save(top, "block", "bleekhout_deur_top.png")
    save(ramp(v("block/dark_oak_door_bottom"), *DOOR), "block", "bleekhout_deur_bottom.png")
    save(ramp(v("item/dark_oak_door"), *DOOR), "item", "bleekhout_deur.png")
    save(ramp(v("block/dark_oak_trapdoor"), *DOOR), "block", "bleekhout_luik.png")
    save(ramp(v("entity/signs/dark_oak"), *PLANKS), "entity", "signs", "bleekhout.png")
    save(ramp(v("item/dark_oak_sign"), *PLANKS), "item", "bleekhout_bord.png")
    save(ramp(v("block/dark_oak_leaves"), *LEAF), "block", "bleekhout_bladeren.png")
    sap = ramp(v("block/dark_oak_sapling"), (120, 110, 112), (226, 208, 212))
    save(sap, "block", "bleekhout_zaailing.png")
    # the moss: pale grey with a few faint flecks; the carpet's tufts; hanging moss
    rng = random.Random(12801)
    moss = ramp(v("block/moss_block"), *MOSS).copy()
    px = moss.load()
    for _ in range(10):
        px[rng.randrange(16), rng.randrange(16)] = (222, 206, 210, 255)
    save(moss, "block", "bleekmos.png")
    save(carpet_side(moss, 0), "block", "bleekmos_tapijt_laag.png")
    save(carpet_side(moss, 1), "block", "bleekmos_tapijt_hoog.png")
    save(ramp(v("block/weeping_vines_plant"), *MOSS), "block", "bleek_hangmos.png")
    save(ramp(v("block/weeping_vines"), *MOSS), "block", "bleek_hangmos_punt.png")
    save(ramp(v("block/weeping_vines"), *MOSS), "item", "bleek_hangmos.png")
    # the hearts: uprooted, asleep, awake (two frames: it beats)
    for name, colour in (("krakend_guhhartje", ORANJE), ("verzuurd_guhhartje", ZUUR)):
        for top_ in (False, True):
            suffix = "_top" if top_ else ""
            save(heart_tex(h, 0, colour, top_), "block", f"{name}{suffix}.png")
            save(heart_tex(h, 1, colour, top_), "block", f"{name}_slaapt{suffix}.png")
            strip = Image.new("RGBA", (16, 32))
            strip.paste(heart_tex(h, 2, colour, top_, 0), (0, 0))
            strip.paste(heart_tex(h, 2, colour, top_, 1), (0, 16))
            save(strip, "block", f"{name}_wakker{suffix}.png")
            h.w(os.path.join(h.TEX, "block", f"{name}_wakker{suffix}.png.mcmeta"), {"animation": {"frames": [
                {"index": 0, "time": 14}, {"index": 1, "time": 4}, {"index": 0, "time": 3}, {"index": 1, "time": 4}]}})
    # kaashars: the clump (on the logs), the block, the bricks
    save(ramp(v("block/glow_lichen"), *HARS), "block", "kaashars.png")
    save(ramp(v("item/honeycomb"), *HARS), "item", "kaashars.png")
    save(ramp(v("block/honeycomb_block"), (206, 124, 26), (255, 208, 92)), "block", "kaashars_blok.png")
    save(ramp(v("block/bricks"), (176, 100, 24), (250, 196, 84)), "block", "harsstenen.png")
    chis = ramp(v("block/chiseled_nether_bricks"), (176, 100, 24), (250, 196, 84)).copy()
    save(chis, "block", "gebeitelde_harsstenen.png")
    save(ramp(v("item/brick"), (186, 106, 24), (255, 206, 90)), "item", "harssteen.png")
    # the oogbloempje
    save(flower(False), "block", "oogbloempje.png")
    save(flower(True), "block", "open_oogbloempje.png")
    # spawn eggs (baked like mc26.spawn_eggs does for the eggs it knows)
    import mc26
    for name, (base, spots) in (("kraakguh_spawn_egg", (0xE4DEDA, 0xFC7812)), ("kraak_mika_spawn_egg", (0xD6D0CC, 0xB4D23C))):
        egg = Image.alpha_composite(mc26.tint(mc26.vanilla_121("item/spawn_egg"), base), mc26.tint(mc26.vanilla_121("item/spawn_egg_overlay"), spots))
        save(egg, "item", f"{name}.png")


# =====================================================================================================================
# the creatures: geo models and wood textures
# =====================================================================================================================
def _face_rect(face):
    (u, v), (w, hh) = face["uv"], face["uv_size"]
    x0, x1 = sorted((u, u + w))
    y0, y1 = sorted((v, v + hh))
    return int(round(x0 * SCALE)), int(round(y0 * SCALE)), int(round(x1 * SCALE)), int(round(y1 * SCALE))


def _swatches(geo, names, used_extra=()):
    from features import knuffeldal_npcs as kn
    return kn._swatches(geo, names)


def wood(arr, rect, rng, vertical, base=(226, 220, 216), dark=(170, 160, 157)):
    """Pale wood on one face: grain lines along it, a darker bark rim, a knot now and then."""
    x0, y0, x1, y1 = rect
    if x1 <= x0 or y1 <= y0:
        return
    w, hh = x1 - x0, y1 - y0
    block = np.zeros((hh, w, 3), np.float32)
    n = w if vertical else hh
    lines = np.array([rng.normal(0, 9) + (-16 if rng.random() < 0.22 else 0) for _ in range(n)], np.float32)
    for i in range(n):
        if vertical:
            block[:, i, :] = lines[i]
        else:
            block[i, :, :] = lines[i]
    block += rng.normal(0, 3.5, (hh, w, 1))
    out = np.array(base, np.float32) + block
    rim = np.array(dark, np.float32)
    for k, f in ((0, 0.75), (1, 0.3)):                                     # bark at the edges of the face
        if hh > 2 * k + 1 and w > 2 * k + 1:
            for sl in (np.s_[k, k:w - k], np.s_[hh - 1 - k, k:w - k], np.s_[k:hh - k, k], np.s_[k:hh - k, w - 1 - k]):
                out[sl] = out[sl] * (1 - f) + rim * f
    if w >= 20 and hh >= 20 and rng.random() < 0.5:                        # a knot
        cx, cy = rng.integers(6, w - 6), rng.integers(6, hh - 6)
        for yy in range(hh):
            for xx in range(w):
                d = ((xx - cx) ** 2 + (yy - cy) ** 2) ** 0.5
                if d < 3.2:
                    out[yy, xx] = out[yy, xx] * 0.55 + rim * 0.45 * (1.2 if d > 2 else 0.9)
    arr[y0:y1, x0:x1, :3] = np.clip(out, 0, 255).astype(np.uint8)
    arr[y0:y1, x0:x1, 3] = 255


def creature(h, name, source, mika):
    """The geo model + texture + glow mask of a wooden creature, from the guh's (or the Mika's) own model."""
    from features import knuffeldal_npcs as kn
    geo_file = kn._load(h, source)
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = f"geometry.{name}"
    geo["bones"] = [b for b in geo["bones"] if b["name"] in BASE_BONES]
    assert len(geo["bones"]) == len(BASE_BONES), [b["name"] for b in geo["bones"]]
    sw = kn._swatches(geo, ["mos", "tak"])
    c = kn._cube
    # tufts of moss on its head and back, a twig with a leaf behind one ear
    geo["bones"].append({"name": "kraak_mos_kop", "parent": "head", "pivot": [0, 13, -6], "cubes": [
        c([-5.0, 13.7, -9.0], [5, 1.2, 4], sw["mos"]), c([1.0, 13.6, -6.5], [4, 0.9, 3], sw["mos"]),
        c([-8.5, 6.0, -8.0], [0.8, 3, 3], sw["mos"])]})
    geo["bones"].append({"name": "kraak_mos_rug", "parent": "body", "pivot": [0, 10, 5], "cubes": [
        c([-4.0, 9.4, 3.0], [5, 1.1, 5], sw["mos"]), c([1.0, 9.2, 6.5], [4, 0.9, 3.5], sw["mos"]),
        c([6.2, 3.0, 4.0], [0.8, 3.5, 4], sw["mos"])]})
    geo["bones"].append({"name": "kraak_takje", "parent": "head", "pivot": [5, 14, -6], "cubes": [
        c([4.6, 13.5, -6.4], [0.8, 3.6, 0.8], sw["tak"]), c([5.4, 15.6, -6.4], [2.2, 0.8, 0.8], sw["tak"]),
        c([7.0, 16.0, -6.6], [1.6, 1.6, 0.4], sw["mos"])]})
    kn._save_geo(h, f"{name}.geo.json", geo_file)

    rng = np.random.default_rng(12810 + mika)
    arr = np.zeros((UV * SCALE, UV * SCALE, 4), np.uint8)
    face_rect = None
    for bone in geo["bones"]:
        if bone["name"].startswith("kraak_"):
            continue
        cubes = bone.get("cubes", [])
        for cube in cubes:
            uv = cube["uv"]
            assert isinstance(uv, dict), f"{source}: {bone['name']} has box uv"
            for side, f in uv.items():
                rect = _face_rect(f)
                # grain runs along the body (front to back) and up the legs/ears
                vertical = side in ("north", "south") or bone["name"].startswith(("leg", "ear"))
                wood(arr, rect, rng, vertical)
        if bone["name"] == "head":
            big = [cb for cb in cubes if cb["size"][0] >= 10]
            front = min(big, key=lambda cb: cb["origin"][2])
            face_rect = _face_rect(front["uv"]["north"])
    kn._paint(arr, sw["mos"], (150, 146, 140), rng, 16)
    kn._paint(arr, sw["tak"], (150, 140, 136), rng, 8)
    # the carved face: two round eye hollows, a little nose, a stitched smile (the Mika: slanted brows, a frown, a fang)
    x0, y0, x1, y1 = face_rect
    w, hh = x1 - x0, y1 - y0
    glow = np.zeros_like(arr)
    hollow, carve = (58, 48, 50), (120, 108, 106)
    eye_col = ZUUR if mika else ORANJE
    ey = y0 + int(hh * 0.40)
    for ex in (x0 + int(w * 0.30), x0 + int(w * 0.70)):
        for yy in range(-7, 8):
            for xx in range(-7, 8):
                d = (xx * xx + yy * yy) ** 0.5
                if d <= 7.2:
                    arr[ey + yy, ex + xx, :3] = hollow
                if d <= 5.2:
                    glow[ey + yy, ex + xx, :3] = eye_col
                    glow[ey + yy, ex + xx, 3] = 255
                if d <= 2.2:
                    glow[ey + yy, ex + xx, :3] = (255, 240, 200)
        if mika:                                                           # grumpy brows
            sgn = 1 if ex < (x0 + x1) // 2 else -1
            for i in range(-7, 8):
                yy = ey - 10 + (i * sgn) // 3
                arr[yy:yy + 2, ex + i, :3] = hollow
                glow[yy:yy + 2, ex + i, 3] = 0
    nx, ny = (x0 + x1) // 2, y0 + int(hh * 0.58)
    arr[ny:ny + 3, nx - 2:nx + 2, :3] = carve
    my = y0 + int(hh * 0.74)
    for i in range(-8, 9):
        yy = my + ((abs(i) - 4) // 3 if mika else -(abs(i) - 4) // 3)
        arr[yy:yy + 2, nx + i, :3] = hollow
        if not mika and i % 4 == 0:
            arr[yy - 1:yy + 3, nx + i, :3] = carve                          # little stitches
    if mika:
        arr[my + 1:my + 5, nx + 4:nx + 6, :3] = (246, 242, 236)             # a wooden fang
    h.save(Image.fromarray(arr), "entity", f"{name}.png")
    h.save(Image.fromarray(glow), "entity", f"{name}_glowmask.png")


def animations(h):
    """The wooden creatures move stiffly: the guh's idle and walk with less bounce, a wobble, and the hug."""
    src = json.load(open(os.path.join(h.A, "geckolib", "animations", "entity", "guh.animation.json"), encoding="utf-8"))["animations"]
    walk = json.loads(json.dumps(src["animation.guh.walk"]))
    walk["bones"].pop("body", None)                                          # (wood doesn't squash)
    out = {
        "animation.kraak.idle": {"loop": True, "animation_length": 6.0, "bones": {
            "head": {"rotation": {"0.0": [0, 0, 0], "3.0": [0, 0, 3], "6.0": [0, 0, 0]}},
            "ear_left": {"rotation": {"0.0": [0, 0, 0], "4.0": [0, 0, 0], "4.2": [0, 0, -10], "4.4": [0, 0, 0], "6.0": [0, 0, 0]}}}},
        "animation.kraak.walk": walk,
        "animation.kraak.wiebel": {"loop": False, "animation_length": 0.6, "bones": {
            "root": {"rotation": {"0.0": [0, 0, 0], "0.1": [0, 0, 7], "0.25": [0, 0, -5], "0.4": [0, 0, 3], "0.6": [0, 0, 0]}},
            "head": {"rotation": {"0.0": [0, 0, 0], "0.15": [0, 0, -6], "0.35": [0, 0, 4], "0.6": [0, 0, 0]}}}},
        "animation.kraak.knuffel": {"loop": False, "animation_length": 1.2, "bones": {
            "root": {"position": {"0.0": [0, 0, 0], "0.2": [0, 2.5, -1.5], "0.5": [0, 0.5, -2], "0.9": [0, 0.5, -2], "1.2": [0, 0, 0]},
                     "rotation": {"0.0": [0, 0, 0], "0.3": [-10, 0, 0], "0.9": [-10, 0, 0], "1.2": [0, 0, 0]}},
            "leg_front_left": {"rotation": {"0.0": [0, 0, 0], "0.3": [-50, 0, -20], "0.9": [-50, 0, -20], "1.2": [0, 0, 0]}},
            "leg_front_right": {"rotation": {"0.0": [0, 0, 0], "0.3": [-50, 0, 20], "0.9": [-50, 0, 20], "1.2": [0, 0, 0]}},
            "head": {"rotation": {"0.0": [0, 0, 0], "0.4": [0, 0, 8], "0.7": [0, 0, -8], "1.2": [0, 0, 0]}}}},
    }
    h.w(os.path.join(h.A, "geckolib", "animations", "entity", "kraakguh.animation.json"), {"format_version": "1.8.0", "animations": out})


def build(h):
    textures(h)
    creature(h, "kraakguh", "guh.geo.json", 0)
    creature(h, "kraak_mika", "mika.geo.json", 1)
    animations(h)
