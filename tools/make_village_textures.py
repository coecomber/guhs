"""
Textures for guh villages: the guh villager look (type texture + ears/tail), the five guh profession robes, the five
job site blocks, the guh clothing items and the Mika-mepper. Most are vanilla textures recoloured, so they fit in.

Run from the project root (after a Gradle build, it reads the vanilla textures):  python tools/make_village_textures.py
"""
import colorsys
import io
import os
import zipfile

import numpy as np
from PIL import Image

JAR = os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar")
TEX = os.path.join("src", "main", "resources", "assets", "guhs", "textures")
jar = zipfile.ZipFile(JAR)
FUR = (195, 160, 205)
rng = np.random.default_rng(11)


def vanilla(path):
    return Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def save(img, *path):
    full = os.path.join(TEX, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def rehue(img, hue, sat=None, value=1.0, keep_grey=True):
    """Moves every coloured pixel to `hue` (0-1), keeping its shading."""
    a = np.asarray(img).astype(np.float32) / 255
    out = a.copy()
    for y in range(a.shape[0]):
        for x in range(a.shape[1]):
            r, g, b, al = a[y, x]
            if al == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r, g, b)
            if keep_grey and s < 0.12:
                continue
            out[y, x, :3] = colorsys.hsv_to_rgb(hue, s if sat is None else min(1, s * sat), min(1, v * value))
    return Image.fromarray((out * 255).astype(np.uint8))


def fur(w, h):
    a = np.array(FUR, np.float32) + rng.normal(0, 6, (h, w, 1))
    return np.clip(a, 0, 255).astype(np.uint8)


# --- the guh villager ------------------------------------------------------------------------------------------------------
base = np.asarray(vanilla("entity/villager/villager")).copy()
plains = np.asarray(rehue(vanilla("entity/villager/type/plains"), 0.9, 0.8)).copy()     # the plains outfit, made pink
guh_type = plains.copy()
# every bit of skin (head, nose, hands) turns into guh fur
for y in range(64):
    for x in range(64):
        r, g, b, al = base[y, x]
        if al and guh_type[y, x, 3] == 0:
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if 0.02 < h < 0.12 and s > 0.2:          # tan skin
                shade = v / 0.62
                guh_type[y, x] = (*[int(min(255, c * shade)) for c in FUR], 255)
# the whole head box (and the nose) is fur, with big glossy guh eyes on the face
guh_type[0:8, 8:24, :3] = fur(16, 8); guh_type[0:8, 8:24, 3] = 255
guh_type[8:18, 0:32, :3] = fur(32, 10); guh_type[8:18, 0:32, 3] = 255
guh_type[0:6, 24:32, :3] = fur(8, 6); guh_type[0:6, 24:32, 3] = 255
K, B, W, D = (12, 12, 20), (70, 150, 225), (250, 250, 255), (40, 90, 170)
EYE = [[K, K, K], [K, B, W], [D, B, K]]
for (ex, flip) in ((8 + 0, False), (8 + 5, True)):
    for row in range(3):
        for col in range(3):
            c = EYE[row][2 - col if flip else col]
            guh_type[8 + 3 + row, ex + col] = (*c, 255)
guh_type[8 + 7, 8 + 3:8 + 5] = (150, 90, 160, 255)                      # a little mouth under the nose
save(Image.fromarray(guh_type), "entity", "villager", "type", "guh.png")
# zombie guh villagers: the same, a bit green and grim
zombie = rehue(Image.fromarray(guh_type), 0.28, 0.6, 0.85, keep_grey=True)
save(zombie, "entity", "zombie_villager", "type", "guh.png")

# ears + tail (GuhVillagerFeaturesLayer): a 32x32 sheet
feat = np.zeros((32, 32, 4), np.uint8)
feat[:, :, :3] = fur(32, 32); feat[:, :, 3] = 255
feat[1:4, 1:4] = (160, 110, 175, 255)                                    # inner ear
save(Image.fromarray(feat), "entity", "villager", "guh_features.png")

# profession robes: recoloured vanilla ones
PROFESSIONS = {"vads_temmer": ("shepherd", 0.92), "guh_kleermaker": ("leatherworker", 0.8), "vadssmid": ("armorer", 0.85),
               "hamsterbouwer": ("mason", 0.14), "mika_jager": ("weaponsmith", 0.97)}
for name, (src, hue) in PROFESSIONS.items():
    robe = rehue(vanilla(f"entity/villager/profession/{src}"), hue, 1.1, 0.9 if name == "mika_jager" else 1.0)
    save(robe, "entity", "villager", "profession", f"{name}.png")
    save(rehue(robe, hue, 0.8, 0.85), "entity", "zombie_villager", "profession", f"{name}.png")

# --- job site blocks: top / front / side --------------------------------------------------------------------------------------
WORK = {  # block: (vanilla top, front, side, hue)
    "knabbelbak": ("composter_top", "composter_side", "composter_side", 0.92),
    "naaitafel": ("loom_top", "loom_front", "loom_side", 0.9),
    "vadsaambeeld": ("smithing_table_top", "smithing_table_front", "smithing_table_side", 0.8),
    "buizenbank": ("fletching_table_top", "fletching_table_front", "fletching_table_side", 0.14),
    "mikatrofee": ("polished_blackstone", "polished_blackstone", "polished_blackstone", None),
}
for name, (top, front, side, hue) in WORK.items():
    for part, src in (("top", top), ("front", front), ("side", side)):
        img = vanilla(f"block/{src}")
        if hue is not None:
            img = rehue(img, hue, 1.2)
        a = np.asarray(img).copy()
        if name == "knabbelbak" and part == "top":                       # a bowl full of kaas knabbels
            empty = a[..., 3] < 255                                      # (the composter's see-through inside)
            a[empty] = (110, 36, 70, 255)
            for (x, y) in ((4, 5), (7, 4), (10, 6), (5, 9), (9, 10), (11, 9), (7, 7)):
                a[y:y + 2, x:x + 2] = (236, 150, 40, 255)
                a[y, x] = (250, 200, 90, 255)
        if name == "mikatrofee" and part == "front":                     # Mika's evil face
            a[3:13, 3:13] = (140, 40, 100, 255)
            for (x, y) in ((4, 6), (5, 7), (10, 6), (11, 7)):
                a[y, x] = (230, 30, 30, 255)
            a[10, 6:10] = (30, 10, 20, 255); a[9, 9] = (250, 250, 250, 255)
            a[2, 3:5] = a[2, 11:13] = (140, 40, 100, 255)
        save(Image.fromarray(a), "block", f"{name}_{part}.png")

# --- item icons: clothes + the Mika-mepper ------------------------------------------------------------------------------------
def icon(rows, colours):
    img = np.zeros((16, 16, 4), np.uint8)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colours:
                img[y, x] = (*colours[ch], 255)
    return Image.fromarray(img)


SHIRT = ["................", "................", "...aaa....aaa...", "..abbbaaaabbba..", ".abbbbbbbbbbbba.", ".abbbbbbbbbbbba.",
         ".aabbbbbbbbbbaa.", "..aabbbbbbbbaa..", "...abbbbbbbba...", "...abbbbbbbba...", "...abbbbbbbba...", "...abbbbbbbba...",
         "...abbbbbbbba...", "...aaaaaaaaaa...", "................", "................"]
def shirt(main, dark, stripe=None, buttons=None):
    rows = [r for r in SHIRT]
    if stripe:
        rows = [r.replace("b", "s") if i % 2 == 0 else r for i, r in enumerate(rows)]
    if buttons:
        rows = [r[:7] + ("o" if "b" in r[6:9] and i % 2 else r[7]) + r[8:] for i, r in enumerate(rows)]
    return icon(rows, {"a": dark, "b": main, "s": stripe or main, "o": buttons or main})


save(shirt((255, 64, 190), (170, 20, 120)), "item", "pink_onesie.png")
save(shirt((240, 240, 250), (40, 70, 160), stripe=(64, 110, 210)), "item", "striped_sweater.png")
save(shirt((250, 210, 40), (180, 140, 10), buttons=(120, 90, 10)), "item", "raincoat.png")
save(shirt((246, 246, 246), (170, 170, 175), buttons=(40, 40, 45)), "item", "chef_jacket.png")
save(icon(["................"] * 5 + ["......aaaa......", ".....abbbba.....", ".....abbbba.....", "..aaaabbbbaaaa..", ".abbbbbbbbbbbba.",
           "..aaaaaaaaaaaa.."] + ["................"] * 5, {"a": (180, 140, 10), "b": (250, 210, 40)}), "item", "rain_hat.png")
save(icon(["........w.......", ".......www......", ".......aba......", "......abbba.....", "......bcbcb.....", ".....abbbbba....",
           ".....bcbcbcb....", "....abbbbbbba...", "....bcbcbcbcb...", "...abbbbbbbbba..", "...aaaaaaaaaaa..", "................",
           "................", "................", "................", "................"],
          {"w": (250, 250, 250), "a": (150, 30, 110), "b": (230, 60, 170), "c": (250, 220, 60)}), "item", "party_hat.png")
save(icon(["................", "....aaaaaaaa....", "...abbbbbbbba...", "..abbbbbbbbbba..", "..abbbbbbbbbba..", "..abbbbbbbbbba..",
           "...abbbbbbbba...", "....abbbbbba....", "....abbbbbba....", "....aaaaaaaa....", "................", "................",
           "................", "................", "................", "................"],
          {"a": (180, 180, 185), "b": (252, 252, 252)}), "item", "chef_hat.png")
BOW = ["................"] * 5 + ["..aa........aa..", "..abaa....aaba..", "..abbbaccabbba..", "..abbbaccabbba..", "..abaa....aaba..",
                                  "..aa........aa.."] + ["................"] * 5
save(icon(BOW, {"a": (140, 20, 40), "b": (220, 40, 60), "c": (250, 120, 130)}), "item", "red_bowtie.png")
save(icon(BOW, {"a": (10, 10, 12), "b": (40, 40, 46), "c": (90, 90, 100)}), "item", "black_bowtie.png")
save(icon(["..........aaaaa.", ".........abcbca.", "........abcbcba.", "........acbcbca.", "........abcbcba.", "........acbcbca.",
           ".........aaaaa..", "........d.......", ".......d........", "......d.........", ".....d..........", "....d...........",
           "...d............", "..e.............", ".ee.............", "................"],
          {"a": (60, 20, 40), "b": (230, 60, 150), "c": (255, 150, 210), "d": (120, 80, 40), "e": (200, 200, 205)}), "item", "mika_mepper.png")
print("village textures written")
