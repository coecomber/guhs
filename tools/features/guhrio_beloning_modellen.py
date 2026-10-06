"""
bbq2 (guhrio-beloning) - the two guhs of the castle: their own models and textures on the sitting guh (client:
GuhrioBeloningClient puts them in SittingGuhRenderers.NPC_MODELEN).

  Pad-guh            the toadstool guh: cream fur, a big white toadstool cap with red spots (his ears stick out from
                     under it), a little blue vest with a golden hem over a cream belly
  Prinses Perzikguh  peach-pink fur, golden locks beside her face and a fringe, a little golden crown with a red and two
                     blue jewels between her ears, a pink dress with a wide hem, a white collar and a blue brooch

  padguh(h) / perzikguh(h)   write geo + texture (after bbq2.stub_npc gave the kind its name)
  check(h)                   the files are there
  preview(out)               (python tools/features/guhrio_beloning_modellen.py <out>, after make_resources) offline renders
"""
import os
import sys

import numpy as np
from PIL import Image

if __name__ == "__main__":
    sys.path.insert(0, "tools")
from features import sterrenwacht_hulp as hulp

PADGUH = "guh_npc_padguh"
PERZIKGUH = "guh_npc_perzikguh"
SEED_PADGUH, SEED_PERZIKGUH = 21302751, 21302752

HOED, STIP = (250, 248, 240), (222, 48, 44)
VEST, VEST_ZOOM = (52, 96, 196), (250, 204, 64)
BUIK = (250, 240, 220)
GOUD, GOUD_DONKER = (250, 200, 60), (190, 128, 24)
HAAR, HAAR_DONKER = (252, 214, 84), (214, 164, 40)
JURK, JURK_DONKER, KRAAG = (244, 136, 178), (214, 96, 146), (252, 250, 246)
ROBIJN, SAFFIER = (214, 40, 60), (50, 110, 226)


def padguh(h):
    c = hulp.cube
    geo_file, g = hulp.sitting_geo(h, f"geometry.{PADGUH}")
    sw = hulp.swatches(g, ["hoed", "stip", "vest", "zoom", "buik"])
    # the toadstool cap: a wide dome of three layers, a red spot on top, in front, behind and on both sides
    g["bones"].append({"name": "padguh_hoed", "parent": "head", "pivot": [0, 24, -1], "cubes": [
        c([-9.0, 23.4, -8.6], [18.0, 3.2, 16.2], sw["hoed"]),
        c([-8.0, 26.6, -7.6], [16.0, 2.6, 14.2], sw["hoed"]),
        c([-6.0, 29.2, -5.6], [12.0, 1.8, 10.2], sw["hoed"]),
        c([-3.0, 30.9, -2.8], [6.0, 0.5, 5.6], sw["stip"]),
        c([-3.0, 24.4, -9.0], [6.0, 3.6, 0.5], sw["stip"]), c([-2.2, 28.0, -8.0], [4.4, 1.0, 0.5], sw["stip"]),
        c([-3.0, 24.4, 7.3], [6.0, 3.6, 0.5], sw["stip"]),
        c([-9.4, 24.4, -3.4], [0.5, 3.6, 6.0], sw["stip"]), c([8.9, 24.4, -3.4], [0.5, 3.6, 6.0], sw["stip"]),
        c([-8.4, 27.4, 2.6], [0.5, 1.6, 3.0], sw["stip"]), c([7.9, 27.4, -6.0], [0.5, 1.6, 3.0], sw["stip"])]})
    # the vest: open at the front over a cream belly, a golden hem
    g["bones"].append({"name": "padguh_vest", "parent": "body", "pivot": [0, 7, 0], "cubes": [
        c([-5.3, 3.4, -4.2], [3.3, 7.0, 8.6], sw["vest"], inflate=0.15), c([2.0, 3.4, -4.2], [3.3, 7.0, 8.6], sw["vest"], inflate=0.15),
        c([-5.3, 3.4, 2.4], [10.6, 7.0, 2.0], sw["vest"], inflate=0.15),
        c([-5.5, 2.8, -4.4], [3.5, 0.8, 9.0], sw["zoom"], inflate=0.1), c([2.0, 2.8, -4.4], [3.5, 0.8, 9.0], sw["zoom"], inflate=0.1),
        c([-2.0, 2.2, -4.5], [4.0, 8.4, 0.5], sw["buik"])]})
    hulp.save_geo(h, f"{PADGUH}.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.09, sat=0.3, val=1.06)
    rng = np.random.default_rng(SEED_PADGUH)

    def rand(block):
        block[-4:, :, :3] = (226, 222, 208)

    def stiksel(block):
        block[:, 15:17, :3] = (36, 70, 150)

    hulp.paint_swatch(a, sw["hoed"], HOED, rng, 5, rand)
    hulp.paint_swatch(a, sw["stip"], STIP, rng, 7)
    hulp.paint_swatch(a, sw["vest"], VEST, rng, 7, stiksel)
    hulp.paint_swatch(a, sw["zoom"], VEST_ZOOM, rng, 5)
    hulp.paint_swatch(a, sw["buik"], BUIK, rng, 5)
    h.save(Image.fromarray(a), "entity", "npc_padguh.png")


def perzikguh(h):
    c = hulp.cube
    geo_file, g = hulp.sitting_geo(h, f"geometry.{PERZIKGUH}")
    sw = hulp.swatches(g, ["goud", "robijn", "saffier", "haar", "jurk", "rok", "kraag"])
    # the crown between her ears: a band, four points and a taller one in front, three jewels
    g["bones"].append({"name": "perzikguh_kroon", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        c([-3.2, 26.0, -3.6], [6.4, 1.5, 5.2], sw["goud"]),
        c([-3.2, 27.5, -3.6], [1.2, 1.4, 1.2], sw["goud"]), c([2.0, 27.5, -3.6], [1.2, 1.4, 1.2], sw["goud"]),
        c([-3.2, 27.5, 0.4], [1.2, 1.4, 1.2], sw["goud"]), c([2.0, 27.5, 0.4], [1.2, 1.4, 1.2], sw["goud"]),
        c([-0.7, 27.5, -3.6], [1.4, 2.4, 1.2], sw["goud"]),
        c([-0.6, 26.3, -3.9], [1.2, 1.0, 0.4], sw["robijn"]),
        c([-3.5, 26.3, -1.6], [0.4, 1.0, 1.2], sw["saffier"]), c([3.1, 26.3, -1.6], [0.4, 1.0, 1.2], sw["saffier"])]})
    # golden hair: a cap of hair on top, a fringe, a long lock at each side and down her back
    g["bones"].append({"name": "perzikguh_haar", "parent": "head", "pivot": [0, 24, -1], "cubes": [
        c([-6.0, 24.3, -6.6], [12.0, 1.9, 12.8], sw["haar"]),
        c([-7.3, 21.6, -7.6], [14.6, 2.9, 0.9], sw["haar"]),
        c([-8.7, 13.6, -6.2], [1.3, 10.6, 9.8], sw["haar"]), c([7.4, 13.6, -6.2], [1.3, 10.6, 9.8], sw["haar"]),
        c([-7.4, 12.0, 5.7], [14.8, 12.6, 1.2], sw["haar"]),
        c([-9.2, 11.6, -4.4], [1.6, 2.6, 3.0], sw["haar"]), c([7.6, 11.6, -4.4], [1.6, 2.6, 3.0], sw["haar"])]})
    # the dress: a bodice, a wide hem, a white collar and a blue brooch
    g["bones"].append({"name": "perzikguh_jurk", "parent": "body", "pivot": [0, 7, 0], "cubes": [
        c([-5.3, 3.0, -4.3], [10.6, 8.2, 8.6], sw["jurk"], inflate=0.2),
        c([-6.6, 0.4, -5.8], [13.2, 3.4, 11.4], sw["rok"]),
        c([-5.5, 10.6, -4.7], [11.0, 1.4, 9.2], sw["kraag"], inflate=0.1),
        c([-1.0, 9.0, -5.2], [2.0, 1.8, 0.6], sw["saffier"])]})
    hulp.save_geo(h, f"{PERZIKGUH}.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.035, sat=0.62, val=1.04)
    rng = np.random.default_rng(SEED_PERZIKGUH)

    def glans(block):
        block[:5, :, :3] = (255, 236, 150)
        block[-5:, :, :3] = GOUD_DONKER

    def fonkel(block):
        block[6:14, 6:14, :3] = np.clip(block[6:14, 6:14, :3].astype(np.int32) + 80, 0, 255).astype(np.uint8)

    def lokken(block):
        for x in range(0, block.shape[1], 4):
            block[:, x, :3] = HAAR_DONKER

    def ruches(block):
        for y in range(0, block.shape[0], 8):
            block[y:y + 2, :, :3] = JURK_DONKER
        block[-4:, :, :3] = KRAAG

    def lijfje(block):
        block[:, 14:18, :3] = JURK_DONKER

    hulp.paint_swatch(a, sw["goud"], GOUD, rng, 5, glans)
    hulp.paint_swatch(a, sw["robijn"], ROBIJN, rng, 5, fonkel)
    hulp.paint_swatch(a, sw["saffier"], SAFFIER, rng, 5, fonkel)
    hulp.paint_swatch(a, sw["haar"], HAAR, rng, 7, lokken)
    hulp.paint_swatch(a, sw["jurk"], JURK, rng, 6, lijfje)
    hulp.paint_swatch(a, sw["rok"], JURK, rng, 6, ruches)
    hulp.paint_swatch(a, sw["kraag"], KRAAG, rng, 4)
    h.save(Image.fromarray(a), "entity", "npc_perzikguh.png")


def check(h):
    problems = []
    for geo in (PADGUH, PERZIKGUH):
        if not os.path.exists(os.path.join(h.A, "geckolib", "models", "entity", f"{geo}.geo.json")):
            problems.append(f"missing model {geo}")
    for kind in ("padguh", "perzikguh"):
        if not os.path.exists(os.path.join(h.TEX, "entity", f"npc_{kind}.png")):
            problems.append(f"missing texture npc_{kind}")
    return problems


def preview(out):
    """Offline renders (front, side, behind) of both guhs as the generators last wrote them."""
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    tiles = []
    for geo, kind in ((PADGUH, "padguh"), (PERZIKGUH, "perzikguh")):
        pad = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity", f"{geo}.geo.json")
        q = wr.geo_quads(pad, f"guhs:entity/npc_{kind}")
        for yaw, pitch in ((205, -14), (270, -8), (25, -18)):
            tiles.append(wr.render(q, yaw, pitch, 360, margin=0.06))
    sheet = Image.new("RGBA", (360 * 3, 360 * 2), (62, 50, 54, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, ((i % 3) * 360, (i // 3) * 360))
    sheet.save(os.path.join(out, "guhrio_beloning_guhs.png"))


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
