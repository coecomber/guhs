"""
bbq2 (ring-h4) - the two things of the chapter that are no plain blocks:

  - the elf boat (entity guhs:ringh4_elfenbootje; GeckoLib model, animation, texture): a slender boat of silver-grey wood,
    its prow a long neck that ends in a little guh's head (ears, eyes, a snoet), its stern a curled leaf, two benches, a
    lamp of the elves on the prow and a leaf-shaped paddle on the right side. The model's front (-z) is where it sails.
    Every face samples its own region of a 64 x 64 sheet (per-face uv).
  - the Spiegel van Guhladriel (block guhs:ringh4_spiegel): a silver bowl on a foot of quartz, still water in it that
    shimmers (an animated texture of 4 frames).

  build(h)       writes geckolib/models/entity/ringh4_elfenbootje.geo.json, geckolib/animations/entity/...animation.json,
                 textures/entity/ringh4_elfenbootje.png, the mirror's model, blockstate, item model, textures
  preview(out)   pictures (python tools/features/ring_h4_modellen.py <out>, from the worktree root, after a build)
"""
import math
import os
import sys

import numpy as np
from PIL import Image

BOOT = "ringh4_elfenbootje"
TEX = 64
# regions of the boat's sheet: name -> (u, v, w, h)
REGIO = {
    "hout": (0, 0, 32, 16),          # the hull: pale silver-grey planks
    "binnen": (0, 16, 32, 16),       # the inside and the benches: warmer wood
    "rand": (32, 0, 16, 8),          # the trim: silver
    "kop": (32, 8, 8, 8),            # the guh's face on the prow
    "kop_zij": (40, 8, 8, 8),
    "oor": (48, 0, 4, 4),
    "licht": (48, 4, 4, 4),          # the lamp
    "blad": (32, 16, 16, 8),         # the paddle's blade and the leaf on the stern
    "steel": (52, 0, 4, 8),
}


def _uv(name, override=None):
    out = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        u, v, w, h = REGIO[(override or {}).get(f, name)]
        out[f] = {"uv": [u, v], "uv_size": [w, h]}
    return out


def _cube(origin, size, name, override=None, inflate=0.0):
    c = {"origin": origin, "size": size, "uv": _uv(name, override)}
    if inflate:
        c["inflate"] = inflate
    return c


def geo():
    bones = [{"name": "root", "pivot": [0, 0, 0]}]
    binnen = {"up": "binnen"}
    romp = [
        _cube([-7, -2, -18], [14, 2, 36], "hout", binnen),                     # the bottom
        _cube([-9, -1, -18], [2, 7, 36], "hout", {"up": "rand"}),              # the sides
        _cube([7, -1, -18], [2, 7, 36], "hout", {"up": "rand"}),
        _cube([-7, -2, -23], [14, 8, 5], "hout", {"up": "rand"}),              # the bow narrows in three steps
        _cube([-5, -1.5, -27], [10, 8, 4], "hout", {"up": "rand"}),
        _cube([-3, -1, -30], [6, 8, 3], "hout", {"up": "rand"}),
        _cube([-7, -2, 18], [14, 8, 5], "hout", {"up": "rand"}),               # the stern
        _cube([-5, -1.5, 23], [10, 8, 3], "hout", {"up": "rand"}),
        _cube([-3, -1, 26], [6, 8, 2], "hout", {"up": "rand"}),
        _cube([-7, 2, -9], [14, 1, 4], "binnen"),                              # two benches
        _cube([-7, 2, 5], [14, 1, 4], "binnen"),
    ]
    bones.append({"name": "romp", "parent": "root", "pivot": [0, 0, 0], "cubes": romp})
    # the prow: a neck that leans forward, a guh's head on it
    bones.append({"name": "hals", "parent": "romp", "pivot": [0, 6, -30], "rotation": [-12, 0, 0], "cubes": [
        _cube([-1.5, 6, -31.5], [3, 6, 3], "hout"), _cube([-1.5, 12, -32.5], [3, 5, 3], "hout")]})
    bones.append({"name": "kop", "parent": "hals", "pivot": [0, 17, -32], "rotation": [12, 0, 0], "cubes": [
        _cube([-3.5, 16, -36], [7, 6, 7], "kop_zij", {"north": "kop", "up": "hout", "down": "hout", "south": "hout"}),
        _cube([-4.5, 21, -32.5], [2.5, 2.5, 1.5], "oor"), _cube([2, 21, -32.5], [2.5, 2.5, 1.5], "oor")]})
    # the stern: a stem that curls back into a leaf
    bones.append({"name": "staart", "parent": "romp", "pivot": [0, 6, 27], "rotation": [14, 0, 0], "cubes": [
        _cube([-1, 6, 26.5], [2, 6, 2], "hout"), _cube([-2.5, 11, 25], [5, 1.5, 5], "blad"), _cube([-1.5, 12.5, 26], [3, 1, 3], "blad")]})
    # the lamp of the elves, on a hook behind the head
    bones.append({"name": "lamp", "parent": "romp", "pivot": [0, 9, -26], "cubes": [
        _cube([-0.5, 6, -26.5], [1, 5, 1], "steel"), _cube([-1.5, 11, -27.5], [3, 3.5, 3], "licht"), _cube([-2, 14.5, -28], [4, 0.8, 4], "rand")]})
    # the paddle, lying over the right side
    bones.append({"name": "peddel", "parent": "romp", "pivot": [9.5, 6, 12], "rotation": [-28, 0, 0], "cubes": [
        _cube([9.2, 5.5, 2], [1, 1, 20], "steel"), _cube([8.9, 3, 20], [1.6, 6, 9], "blad")]})
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{BOOT}", "texture_width": TEX, "texture_height": TEX,
                        "visible_bounds_width": 5, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1, 0]},
        "bones": bones}]}


def animation():
    return {"format_version": "1.8.0", "animations": {f"animation.{BOOT}.dobber": {"loop": True, "animation_length": 4.0, "bones": {
        "romp": {"position": {"0.0": [0, 0, 0], "1.0": [0, 0.5, 0], "2.0": [0, 0, 0], "3.0": [0, -0.4, 0], "4.0": [0, 0, 0]}},
        "peddel": {"rotation": {"0.0": [0, 0, 0], "2.0": [3, 0, 0], "4.0": [0, 0, 0]}},
        "lamp": {"rotation": {"0.0": [0, 0, -3], "2.0": [0, 0, 3], "4.0": [0, 0, -3]}}}}}}


def texture():
    rng = np.random.default_rng(21301940)
    a = np.zeros((TEX, TEX, 4), np.uint8)

    def vul(name, painter):
        u, v, w, h = REGIO[name]
        a[v:v + h, u:u + w] = painter(w, h)

    def planken(basis, donker, breed=4):
        def p(w, h):
            b = np.zeros((h, w, 4), np.uint8)
            b[..., :3] = np.clip(np.array(basis) + rng.normal(0, 5, (h, w, 1)), 0, 255)
            b[..., 3] = 255
            for y in range(0, h, breed):
                b[y, :, :3] = donker
            for _ in range(w * h // 40):
                x, y = rng.integers(0, w), rng.integers(0, h)
                b[y, x:x + 3, :3] = np.clip(np.array(basis) * 0.9, 0, 255)
            return b
        return p

    def vlak(kleur, var=4):
        def p(w, h):
            b = np.zeros((h, w, 4), np.uint8)
            b[..., :3] = np.clip(np.array(kleur) + rng.normal(0, var, (h, w, 1)), 0, 255)
            b[..., 3] = 255
            return b
        return p

    def kop(w, h):
        b = vlak((214, 220, 224))(w, h)
        for ex in (1, 5):                                           # two big eyes with a glint
            b[2:5, ex:ex + 2, :3] = (34, 36, 48)
            b[2, ex, :3] = (250, 250, 255)
        b[5, 3:5, :3] = (236, 150, 170)                             # the snoet
        b[6, 3:5, :3] = (150, 156, 166)
        return b

    def blad(w, h):
        b = vlak((150, 190, 160))(w, h)
        b[h // 2 - 1:h // 2 + 1, :, :3] = (108, 150, 120)
        for x in range(0, w, 3):
            b[:, x, :3] = np.clip(b[:, x, :3].astype(int) - 14, 0, 255)
        return b

    def licht(w, h):
        b = vlak((190, 244, 250), 3)(w, h)
        b[1:3, 1:3, :3] = (250, 255, 255)
        return b

    vul("hout", planken((196, 202, 204), (150, 158, 164)))
    vul("binnen", planken((206, 190, 150), (168, 150, 112)))
    vul("rand", vlak((228, 234, 240), 3))
    vul("kop", kop)
    vul("kop_zij", vlak((214, 220, 224)))
    vul("oor", vlak((236, 170, 186), 3))
    vul("licht", licht)
    vul("blad", blad)
    vul("steel", vlak((150, 158, 164), 3))
    return Image.fromarray(a)


def spiegel(h):
    """The mirror: a foot, a stem, the bowl with its rim, the water."""
    from features import uvfix
    rng = np.random.default_rng(21301941)
    frames = 4
    water = np.zeros((16 * frames, 16, 4), np.uint8)
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - 7.5, y - 7.5)
                golf = math.sin(d * 1.1 - f * math.pi / 2) * 0.5 + 0.5
                k = (120 + 60 * golf, 190 + 40 * golf, 235 + 20 * golf)
                water[f * 16 + y, x, :3] = np.clip(np.array(k) + rng.normal(0, 3, 3), 0, 255)
                water[f * 16 + y, x, 3] = 255
        for _ in range(3):                                           # little stars in the water
            x, y = rng.integers(2, 14), rng.integers(2, 14)
            water[f * 16 + y, x, :3] = (255, 255, 255)
    h.save(Image.fromarray(water), "block", "ringh4_spiegel_water.png")
    h.w(os.path.join(h.TEX, "block", "ringh4_spiegel_water.png.mcmeta"), {"animation": {"frametime": 12, "interpolate": True}})
    zilver = np.zeros((16, 16, 4), np.uint8)
    zilver[..., :3] = np.clip(np.array((206, 214, 224)) + rng.normal(0, 5, (16, 16, 1)), 0, 255)
    zilver[..., 3] = 255
    zilver[0, :, :3] = (240, 246, 252)
    zilver[15, :, :3] = (150, 160, 176)
    for x in range(1, 16, 4):                                        # a band of little leaves around the bowl
        zilver[6:9, x:x + 2, :3] = (150, 196, 170)
    h.save(Image.fromarray(zilver), "block", "ringh4_spiegel.png")

    def el(frm, to, tex, faces=None):
        return {"from": frm, "to": to, "faces": {f: {"texture": tex} for f in (faces or ("down", "up", "north", "south", "west", "east"))}}

    z, k, w = "#zilver", "#kwarts", "#water"
    elements = [el([4.5, 0, 4.5], [11.5, 1.5, 11.5], k), el([6.5, 1.5, 6.5], [9.5, 6, 9.5], k), el([5.5, 5, 5.5], [10.5, 6.5, 10.5], z),
                el([2.5, 6.5, 2.5], [13.5, 7.5, 13.5], z),
                el([1.5, 7.5, 1.5], [14.5, 10.5, 3], z), el([1.5, 7.5, 13], [14.5, 10.5, 14.5], z),
                el([1.5, 7.5, 3], [3, 10.5, 13], z), el([13, 7.5, 3], [14.5, 10.5, 13], z),
                el([3, 9.4, 3], [13, 9.6, 13], w, faces=("up",))]
    h.w(f"{h.A}/models/block/ringh4_spiegel.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {
        "particle": "guhs:block/ringh4_spiegel", "zilver": "guhs:block/ringh4_spiegel", "kwarts": "minecraft:block/quartz_block_bottom",
        "water": "guhs:block/ringh4_spiegel_water"}, "elements": uvfix.binnen(elements),
        "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 1.5, 0], "scale": [0.62, 0.62, 0.62]},
                    "ground": {"translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]}, "fixed": {"scale": [0.5, 0.5, 0.5]},
                    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.36, 0.36, 0.36]},
                    "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]}}})
    h.w(f"{h.A}/blockstates/ringh4_spiegel.json", {"variants": {"": {"model": "guhs:block/ringh4_spiegel"}}})
    h.w(f"{h.A}/models/item/ringh4_spiegel.json", {"parent": "guhs:block/ringh4_spiegel"})
    h.self_drop("ringh4_spiegel")
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:ringh4_spiegel"])


def build(h):
    h.w(os.path.join(h.A, "geckolib", "models", "entity", f"{BOOT}.geo.json"), geo())
    h.w(os.path.join(h.A, "geckolib", "animations", "entity", f"{BOOT}.animation.json"), animation())
    h.save(texture(), "entity", f"{BOOT}.png")
    spiegel(h)


def check(h):
    problems = []
    for f in (f"geckolib/models/entity/{BOOT}.geo.json", f"geckolib/animations/entity/{BOOT}.animation.json", f"textures/entity/{BOOT}.png",
              "models/block/ringh4_spiegel.json", "textures/block/ringh4_spiegel.png", "textures/block/ringh4_spiegel_water.png",
              "textures/block/ringh4_spiegel_water.png.mcmeta"):
        if not os.path.exists(os.path.join(h.A, *f.split("/"))):
            problems.append(f"missing {f}")
    return problems


def preview(out):
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    geo_pad = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity", f"{BOOT}.geo.json")
    q = wr.geo_quads(geo_pad, f"guhs:entity/{BOOT}")
    for yaw in (35, 150, 215):
        wr.render(q, yaw=yaw, pitch=-18, size=520, ss=2).save(os.path.join(out, f"{BOOT}_{yaw}.png"))
    for yaw in (35, 215):
        wr.render(wr.model_quads("guhs:block/ringh4_spiegel"), yaw=yaw, pitch=-28, size=360, ss=2).save(os.path.join(out, f"ringh4_spiegel_{yaw}.png"))


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    import make_v2
    if "--build" in sys.argv:
        build(make_v2)
        print(check(make_v2) or "models ok")
    preview(sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith("--") else ".")
