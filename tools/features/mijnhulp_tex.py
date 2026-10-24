"""
1.7.0 "Onder de Wol", the mining helpers (feature/mijnhulp): their textures. Called by mijnhulp.py (textures(h)); on its own

    python tools/features/mijnhulp_tex.py      (from the project root): writes them all and a contact sheet
                                               ../mijnhulp_tex_vel.png (enlarged) to look at

  - super vahoege vads: the vahoege vads items and armour layers, their pink metal turned into a glittery pink-lilac
    (lighter, a little more violet, with white and pale pink glints); the brown handles stay;
  - the Vahoege Vadsknakker: the vadspaxel with a little hoe hook at the top, a tiny pair of scissors below and a guh face
    (two eyes, blushing cheeks) on its pink head; the super one is its glittery version;
  - the Vahoegietstaaf (a pink-lilac ingot full of glints), the Super-vads-sjabloon (a smithing template plate with a guh);
  - the Wolkenliftje (a pink cloud block, an up-and-down arrow on top), the Pluisladder (a soft pink rope ladder, the top
    piece with the rolled-up rest, and a roll as the item);
  - the Mijnlampje: the Mijnguhhelm (kaasmijn_helm) in pink, its lamp stays warm white (item icon and guh clothes texture).

Deterministic (fixed seeds), only reads the existing vads/kaasmijn textures.
"""
import colorsys
import os
import sys

import numpy as np
from PIL import Image

A_STANDAARD = os.path.join("src", "main", "resources", "assets", "guhs")
VADS_ITEMS = ["sword", "pickaxe", "axe", "shovel", "hoe", "paxel", "shears", "helmet", "chestplate", "leggings", "boots"]


def _lees(A, *pad):
    return np.asarray(Image.open(os.path.join(A, "textures", *pad)).convert("RGBA")).astype(np.float32)


def _img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def _grid(rows, kleuren):
    a = np.zeros((len(rows), len(rows[0]), 4), np.float32)
    for y, r in enumerate(rows):
        for x, c in enumerate(r):
            if c != ".":
                k = kleuren[c]
                a[y, x, :3] = k[:3]
                a[y, x, 3] = k[3] if len(k) == 4 else 255
    return a


# ---------------------------------------------------------------------------------------------------------------------
# super: glittery pink-lilac
# ---------------------------------------------------------------------------------------------------------------------
def glitter(a, seed, glints=None):
    """The pink/magenta/lilac metal of a vads texture as glittery pink-lilac; the rest (handles, outlines) as it was."""
    a = a.copy()
    h, w = a.shape[:2]
    metaal = np.zeros((h, w), bool)
    for y in range(h):
        for x in range(w):
            r, g, b, al = a[y, x]
            if al == 0:
                continue
            hh, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s > 0.22 and (hh >= 0.72 or hh <= 0.04) and v > 0.18:
                metaal[y, x] = True
                # light stays light, dark gets deep violet; a bit more violet and lighter overall
                nh = 0.84 + (v - 0.55) * 0.08
                ns = min(1.0, s * 0.62 + 0.08)
                nv = min(1.0, v * 0.92 + 0.14)
                a[y, x, :3] = np.array(colorsys.hsv_to_rgb(nh % 1.0, ns, nv)) * 255
    # glints: white and pale pink sparkles on the metal (per texture: about one in twelve metal pixels, at least two)
    rng = np.random.default_rng(seed)
    plekken = np.argwhere(metaal)
    if len(plekken):
        n = glints if glints is not None else max(2, len(plekken) // 12)
        for i, (y, x) in enumerate(plekken[rng.permutation(len(plekken))[:n]]):
            a[y, x, :3] = (255, 250, 255) if i % 2 == 0 else (255, 214, 240)
    return a


# ---------------------------------------------------------------------------------------------------------------------
# the Vadsknakker
# ---------------------------------------------------------------------------------------------------------------------
def vadsknakker(A):
    a = _lees(A, "item", "vahoege_vads_paxel.png")
    roze, licht, donker, zwart = (235, 32, 129), (255, 150, 200), (120, 20, 70), (40, 8, 24)
    staal, staal2 = (230, 220, 240), (170, 150, 190)

    def zet(x, y, k):
        a[y, x, :3] = k
        a[y, x, 3] = 255
    # the hoe: a little pink hook in the top right corner, on the end of the head
    for x, y, k in ((11, 1, donker), (12, 1, roze), (13, 1, roze), (14, 1, donker), (12, 0, licht), (13, 0, licht),
                    (14, 2, donker), (13, 2, roze), (15, 2, zwart), (15, 1, zwart), (15, 3, zwart)):
        zet(x, y, k)
    # the scissors: two little silver blades crossing below the handle, with pink finger rings
    for x, y, k in ((6, 11, staal), (7, 12, staal), (8, 13, staal2), (8, 11, staal), (7, 12, staal), (6, 13, staal2),
                    (5, 14, roze), (9, 14, roze), (4, 14, donker), (10, 14, donker), (5, 15, donker), (9, 15, donker)):
        zet(x, y, k)
    # the guh face on the pink pick head: two dark eyes, pink cheeks, a little smile
    for x, y, k in ((7, 3, zwart), (10, 3, zwart), (6, 4, licht), (11, 4, licht), (8, 4, donker), (9, 4, donker)):
        zet(x, y, k)
    return a


# ---------------------------------------------------------------------------------------------------------------------
# the material and the template
# ---------------------------------------------------------------------------------------------------------------------
def vahoegietstaaf(A):
    a = _lees(A, "item", "vahoege_vads_ingot.png")
    uit = a.copy()
    donker, licht = np.array((150, 60, 160)), np.array((255, 205, 236))
    for y in range(16):
        for x in range(16):
            if a[y, x, 3] == 0:
                continue
            v = a[y, x, :3].mean() / 255
            f = np.clip((v - 0.2) / 0.7, 0, 1)
            uit[y, x, :3] = donker * (1 - f) + licht * f
    return glitter(uit, 41, glints=7)


def sjabloon():
    k = {"o": (44, 24, 52), "p": (98, 54, 112), "q": (128, 76, 146), "r": (168, 104, 184), "z": (236, 120, 180), "w": (255, 214, 240),
         "e": (40, 8, 24)}
    return _grid([
        "................",
        "..oooooooooooo..",
        ".oqqqqqqqqqqqqo.",
        ".oqrrrrrrrrrrpo.",
        ".oqr.zzzzzz.rpo.",
        ".oqrzzzzzzzzrpo.",
        ".oqrzzezzezzrpo.",
        ".oqrzwzzzzwzrpo.",
        ".oqrzzzeezzzrpo.",
        ".oqrrzzzzzzrrpo.",
        ".oqrrrzzzzrrrpo.",
        ".oqrrrrrrrrrrpo.",
        ".oqpppppwppppqo.",
        ".opppppppppppqo.",
        "..oooooooooooo..",
        "................"], k)


# ---------------------------------------------------------------------------------------------------------------------
# the blocks
# ---------------------------------------------------------------------------------------------------------------------
def _wol(kleur, seed, amp=10):
    r = np.random.default_rng(seed)
    a = np.zeros((16, 16, 4), np.float32)
    a[..., :3] = np.array(kleur) + r.normal(0, amp / 2, (16, 16, 1))
    a[..., 3] = 255
    return a


def wolkenliftje_zij():
    a = _wol((246, 170, 206), 11)
    r = np.random.default_rng(12)
    # white cloud puffs along the top and a soft pink band below
    for x in range(16):
        top = 4 + int(1.5 * np.sin(x / 2.2)) + r.integers(0, 2)
        a[:top, x, :3] = (255, 244, 250)
        a[top, x, :3] = (250, 214, 232)
    a[13:, :, :3] = (226, 140, 186)
    a[15, :, :3] = (200, 110, 160)
    return a


def wolkenliftje_boven():
    a = _wol((255, 236, 246), 13, 8)
    pijl = (236, 96, 160)
    # the arrows: up in the top half, down in the bottom half
    for i, y in enumerate(range(2, 6)):
        a[y, 8 - i - 1:8 + i + 1, :3] = pijl
    a[6:8, 7:9, :3] = pijl
    a[8:10, 7:9, :3] = pijl
    for i, y in enumerate(range(13, 9, -1)):
        a[y, 8 - i - 1:8 + i + 1, :3] = pijl
    a[0, :, :3] = a[15, :, :3] = (230, 160, 200)
    a[:, 0, :3] = a[:, 15, :3] = (230, 160, 200)
    return a


def wolkenliftje_onder():
    return _wol((226, 140, 186), 14)


def pluisladder(top):
    k = {"t": (246, 150, 196), "u": (210, 100, 160), "s": (255, 200, 228), "d": (176, 70, 130), "w": (255, 236, 246)}
    rows = []
    for y in range(16):
        rail = "tu" if y % 2 == 0 else "ut"
        sport = y % 4 == 2
        mid = ("ssssssss" if sport else "........")
        rows.append("." + rail + "." + ("d" if sport else ".") + mid + ("d" if sport else ".") + "." + rail[::-1] + ".")
        rows[-1] = rows[-1][:16]
    a = _grid(rows, k)
    if top:
        # the rolled-up rest of the roll along the top
        rol = _grid([
            "..dddddddddddd..",
            ".dsswsswsswsssd.",
            ".duuttuuttuuttd.",
            "..dddddddddddd..",
        ], k)
        a[:4] = np.where(rol[..., 3:4] > 0, rol, a[:4])
    return a


def pluisladder_item():
    k = {"t": (246, 150, 196), "u": (210, 100, 160), "s": (255, 200, 228), "d": (150, 50, 110), "w": (255, 240, 248)}
    return _grid([
        "................",
        "................",
        "....dddddddd....",
        "...dssssssssd...",
        "..dsswwssttssd..",
        "..dswuuuuuutsd..",
        "..dstudddddutd..",
        "..dstud..dsutd..",
        "..dstud..dsutd..",
        "..dstuddddsutd..",
        "..dsstuuuuutsd..",
        "...dssttttssdt..",
        "....dddddddd.t..",
        ".............u..",
        "..............t.",
        "................"], k)


# ---------------------------------------------------------------------------------------------------------------------
# the Mijnlampje: the miner guh's hard hat in pink
# ---------------------------------------------------------------------------------------------------------------------
def roze_helm(a):
    a = a.copy()
    h, w = a.shape[:2]
    for y in range(h):
        for x in range(w):
            r, g, b, al = a[y, x]
            if al == 0:
                continue
            hh, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s > 0.45 and 0.05 <= hh <= 0.2:          # the yellow of the helmet (the pale lamp stays)
                nh = 0.93
                a[y, x, :3] = np.array(colorsys.hsv_to_rgb(nh, min(1.0, s * 0.72), min(1.0, v * 0.97 + 0.03))) * 255
    return a


# ---------------------------------------------------------------------------------------------------------------------
def textures(h):
    A = h.A

    def zet(a, *pad):
        p = os.path.join(A, "textures", *pad)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        _img(a).save(p)
    for i, n in enumerate(VADS_ITEMS):
        zet(glitter(_lees(A, "item", f"vahoege_vads_{n}.png"), 100 + i), "item", f"super_vahoege_vads_{n}.png")
    for i, laag in enumerate(("humanoid", "humanoid_leggings")):
        zet(glitter(_lees(A, "entity", "equipment", laag, "vahoege_vads.png"), 200 + i), "entity", "equipment", laag, "super_vahoege_vads.png")
    knakker = vadsknakker(A)
    zet(knakker, "item", "vahoege_vadsknakker.png")
    zet(glitter(knakker, 300), "item", "super_vahoege_vadsknakker.png")
    zet(vahoegietstaaf(A), "item", "vahoegietstaaf.png")
    zet(sjabloon(), "item", "super_vads_sjabloon.png")
    zet(wolkenliftje_zij(), "block", "wolkenliftje_zij.png")
    zet(wolkenliftje_boven(), "block", "wolkenliftje_boven.png")
    zet(wolkenliftje_onder(), "block", "wolkenliftje_onder.png")
    zet(pluisladder(False), "block", "pluisladder.png")
    zet(pluisladder(True), "block", "pluisladder_top.png")
    zet(pluisladder_item(), "item", "pluisladder.png")
    zet(roze_helm(_lees(A, "item", "kaasmijn_helm.png")), "item", "mijnlampje.png")
    zet(roze_helm(_lees(A, "entity", "guh_clothes", "kaasmijn_helm.png")), "entity", "guh_clothes", "mijnlampje.png")


VEL = ["item/vahoege_vadsknakker", "item/super_vahoege_vadsknakker", "item/vahoegietstaaf", "item/super_vads_sjabloon",
       "item/mijnlampje", "item/pluisladder", "block/pluisladder", "block/pluisladder_top", "block/wolkenliftje_zij",
       "block/wolkenliftje_boven"] + [f"item/super_vahoege_vads_{n}" for n in VADS_ITEMS]


if __name__ == "__main__":
    import types
    A = A_STANDAARD
    textures(types.SimpleNamespace(A=A))
    s = 8
    vel = Image.new("RGBA", (len(VEL) * (16 * s + 8), 16 * s + 8), (70, 70, 80, 255))
    for i, n in enumerate(VEL):
        vel.alpha_composite(Image.open(os.path.join(A, "textures", n + ".png")).convert("RGBA").resize((16 * s, 16 * s), Image.NEAREST),
                            (i * (16 * s + 8) + 4, 4))
    uit = sys.argv[1] if len(sys.argv) > 1 else os.path.join("..", "mijnhulp_tex_vel.png")
    vel.save(uit)
    print("mijnhulp textures written, sheet:", uit)
