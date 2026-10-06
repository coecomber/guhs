"""
bbq2 (ring-sausuman) - the model, texture and animation file of Sausuman van de Vele Sauzen (NPC kind sausuman,
SittingGuhRenderers.NPC_MODELEN, registered in feature/ringsausuman/client/RingSausumanClient).

He is a Mika (the Mika's model, like Boromika of ring-kern) in off-white: a white beard that is longer than he is and lies
on the floor in front of him, long white hair down his back, bushy dark eyebrows, a white robe covered in the stains of
many sauces (ketchup, mustard, curry, sate: "van de Vele Sauzen"), and next to him his staff: a tall black fork with a
glowing blob of cheese sauce between its tines.

build(h) writes geckolib/models/entity/guh_npc_sausuman.geo.json, geckolib/animations/entity/guh_npc_sausuman.animation.json
and textures/entity/npc_sausuman.png. preview(out) renders him from three sides
(python tools/features/ring_sausuman_modellen.py <out> [--build]).
"""
import os
import sys

import numpy as np

from features import ring_modellen as rm

KIND = "sausuman"
HOOFD = [0, 5, -2]          # the Mika's head pivot
LIJF = [0, 5, 5]


def vlekken(block):
    """The robe: off-white cloth with splashes of sauce: ketchup, mustard, curry and sate, and a drip under each."""
    hgt, wid = block.shape[:2]
    rng = np.random.default_rng(21302251)
    sauzen = [(206, 44, 36), (236, 190, 40), (232, 130, 36), (140, 86, 44), (206, 44, 36), (236, 190, 40), (232, 130, 36)]
    for i in range(max(4, (hgt * wid) // 60)):
        kleur = sauzen[i % len(sauzen)]
        cy, cx = int(rng.integers(1, max(2, hgt - 1))), int(rng.integers(1, max(2, wid - 1)))
        r = int(rng.integers(1, 3))
        for y in range(max(0, cy - r), min(hgt, cy + r + 1)):
            for x in range(max(0, cx - r), min(wid, cx + r + 1)):
                if (y - cy) ** 2 + (x - cx) ** 2 <= r * r + 0.5:
                    block[y, x, :3] = kleur
        for d in range(1, int(rng.integers(1, 4))):          # the drip
            if cy + r + d < hgt:
                block[cy + r + d, cx, :3] = kleur
    block[-1:, :, :3] = (block[-1:, :, :3] * 0.82).astype(block.dtype)     # a grubby hem


def gloed(block):
    """The blob of cheese sauce on the fork: bright in the middle."""
    hgt, wid = block.shape[:2]
    for y in range(hgt):
        for x in range(wid):
            d = ((y - (hgt - 1) / 2) ** 2 + (x - (wid - 1) / 2) ** 2) ** 0.5 / (max(hgt, wid) / 2)
            k = max(0.0, 1.0 - d)
            block[y, x, :3] = tuple(int(min(255, c + (255 - c) * k * 0.8)) for c in block[y, x, :3])


def sausuman(h):
    geo_file, geo = rm._mika(h, f"guh_npc_{KIND}")
    geo["description"]["visible_bounds_width"] = 4
    geo["description"]["visible_bounds_height"] = 3.5
    geo["description"]["visible_bounds_offset"] = [0, 1.5, 0]
    sw = rm.kn._swatches(geo, ["baard", "haar", "wenkbrauw", "mantel", "kraag", "staf", "bol"])
    # the beard: a moustache, a full chin, and then it just goes on, out over the floor in front of him
    rm._deel(geo, sw, "sausuman_baard", "head", [0, 3, -12], [
        ([-4.6, 3.4, -13.5], [9.2, 1.5, 1.0], "baard"),
        ([-4.2, 0.9, -13.9], [8.4, 2.6, 1.7], "baard"),
        ([-3.6, 0.0, -17.2], [7.2, 1.5, 4.0], "baard"),
        ([-2.6, 0.0, -20.4], [5.2, 1.1, 3.4], "baard"),
        ([-1.4, 0.0, -22.8], [2.8, 0.8, 2.6], "baard"),
        ([-0.6, 0.0, -24.2], [1.2, 0.6, 1.6], "baard")])
    # long white hair: a cap on the crown, a curtain down the back of the head and out over his back, a lock along each cheek
    locks = [([8.0, 2.6, -9.6], [1.3, 9.6, 5.2], "haar"), ([8.2, 1.4, -8.4], [1.0, 1.4, 3.0], "haar")]
    rm._deel(geo, sw, "sausuman_haar", "head", HOOFD, [
        ([-6.8, 14.0, -11.4], [13.6, 1.3, 9.8], "haar"),
        ([-5.6, 15.1, -10.2], [11.2, 0.7, 7.6], "haar"),
        ([-7.2, 9.6, -2.6], [14.4, 5.4, 1.5], "haar"),
        ([-5.4, 11.0, -1.4], [10.8, 1.1, 5.6], "haar"),
        ([-3.8, 11.0, 4.0], [7.6, 0.9, 3.2], "haar")] + locks + rm._spiegel(locks))
    # bushy dark eyebrows, drawn down in the middle: he is always a little cross
    rm._deel(geo, sw, "sausuman_wenkbrauw_links", "head", [4.4, 11.2, -12.6], [([1.4, 10.5, -13.1], [6.0, 1.7, 1.1], "wenkbrauw")],
             rotation=[0, 0, 14])
    rm._deel(geo, sw, "sausuman_wenkbrauw_rechts", "head", [-4.4, 11.2, -12.6], [([-7.4, 10.5, -13.1], [6.0, 1.7, 1.1], "wenkbrauw")],
             rotation=[0, 0, -14])
    # the robe of many sauces over his back, with a high collar
    rm._deel(geo, sw, "sausuman_mantel", "body", LIJF, [
        ([-7.1, 10.0, -1.4], [14.2, 1.0, 12.8], "mantel"),
        ([-7.5, 1.6, -1.4], [1.0, 8.9, 12.8], "mantel"), ([6.5, 1.6, -1.4], [1.0, 8.9, 12.8], "mantel"),
        ([-7.1, 1.6, 10.9], [14.2, 8.9, 1.0], "mantel"),
        ([-7.8, 8.8, -2.2], [15.6, 2.6, 1.2], "kraag"), ([-7.8, 8.8, -1.2], [1.2, 2.6, 2.2], "kraag"), ([6.6, 8.8, -1.2], [1.2, 2.6, 2.2], "kraag")])
    # the staff: a black fork taller than he is, planted next to him, a glowing blob of sauce between the tines
    rm._deel(geo, sw, "sausuman_staf", "root", [-11.2, 0, -5.0], [
        ([-11.8, 0.0, -5.6], [1.2, 23.6, 1.2], "staf"),
        ([-13.4, 23.2, -5.6], [4.4, 1.1, 1.2], "staf"),
        ([-13.4, 24.3, -5.6], [0.8, 5.0, 1.2], "staf"), ([-12.2, 24.3, -5.6], [0.8, 4.2, 1.2], "staf"),
        ([-11.0, 24.3, -5.6], [0.8, 4.2, 1.2], "staf"), ([-9.8, 24.3, -5.6], [0.8, 5.0, 1.2], "staf"),
        ([-12.4, 0.0, -6.2], [2.4, 0.8, 2.4], "staf")])
    rm._deel(geo, sw, "sausuman_bol", "sausuman_staf", [-11.2, 26.4, -5.0], [([-12.4, 25.0, -6.2], [2.4, 2.4, 2.4], "bol")])
    a = rm._schilder(h, "mika.png", (0.10, 0.16, 1.12), sw, {
        "baard": ((240, 240, 236), 9, rm.haar(0.86)), "haar": ((230, 230, 228), 9, rm.haar(0.84)),
        "wenkbrauw": ((66, 60, 64), 8, rm.haar(0.75)), "mantel": ((238, 234, 224), 5, vlekken), "kraag": ((214, 208, 196), 5, None),
        "staf": ((44, 42, 50), 6, rm.metaal(18)), "bol": ((250, 190, 50), 6, gloed)}, 21302252)
    rm._bewaar(h, KIND, geo_file, a)
    h.w(os.path.join(h.A, *rm.ANIM, f"guh_npc_{KIND}.animation.json"), rm.mika_anims("animation.guh_sitting", kop=-3, staart=8, sneller=0.5))


def build(h):
    sausuman(h)


def check(h):
    problems = []
    for path in (os.path.join(h.A, *rm.GEO, f"guh_npc_{KIND}.geo.json"), os.path.join(h.A, *rm.ANIM, f"guh_npc_{KIND}.animation.json"),
                 os.path.join(h.TEX, "entity", f"npc_{KIND}.png")):
        if not os.path.exists(path):
            problems.append(f"missing file {path}")
    return problems


# =====================================================================================================================
# pictures (not part of the build)
# =====================================================================================================================
def preview(out):
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    geo = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity", f"guh_npc_{KIND}.geo.json")
    q = wr.geo_quads(geo, f"guhs:entity/npc_{KIND}")
    for yaw in (35, 150, -60):
        wr.render(q, yaw=yaw, pitch=-12, size=420, ss=1).save(os.path.join(out, f"sausuman_{yaw}.png"))


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    import make_v2
    if "--build" in sys.argv:
        build(make_v2)
        print(check(make_v2) or "model ok")
    preview(sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith("--") else ".")
