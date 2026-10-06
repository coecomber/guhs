"""
bbq2 (toren-peper) - the two guhs: their own models and textures on the sitting guh (client: TorenpeperClient puts them in
SittingGuhRenderers.NPC_MODELEN).

  de Torenwachter-guh   weathered grey-blue, a yellow zuidwester (the rain hat with the long brim at the back) that his ears
                        stick out from under, a white beard and moustache, a navy keeper's coat with gold buttons and a red
                        and white striped collar
  de Peperteler-guh     red-faced from tasting his own peppers, a wide straw hat with a red pepper behind the band, a green
                        apron with a pocket, a red neckerchief

  torenwachter(h) / peperteler(h)   write geo + texture (after bbq2.stub_npc gave the kind its name)
  check(h)                          the files are there
  preview(out)                      (python tools/features/toren_peper_modellen.py <out>, after make_resources) offline renders
"""
import os
import sys

import numpy as np
from PIL import Image

if __name__ == "__main__":
    sys.path.insert(0, "tools")
from features import sterrenwacht_hulp as hulp

TORENWACHTER = "guh_npc_torenwachterguh"
PEPERTELER = "guh_npc_pepertelerguh"
SEED_TORENWACHTER, SEED_PEPERTELER = 21301451, 21301452

GEEL, GEEL_DONKER = (246, 204, 52), (206, 160, 30)
BAARD = (242, 240, 232)
JAS, JAS_DONKER = (40, 56, 96), (28, 40, 72)
GOUD = (250, 208, 84)
STRO, STRO_DONKER = (226, 190, 110), (186, 148, 76)
SCHORT, SCHORT_LICHT = (62, 132, 66), (120, 190, 110)
PEPERROOD, PEPERSTEEL = (214, 44, 36), (70, 150, 60)


def torenwachter(h):
    c = hulp.cube
    geo_file, g = hulp.sitting_geo(h, f"geometry.{TORENWACHTER}")
    sw = hulp.swatches(g, ["hoed", "hoedrand", "baard", "jas", "knoop", "kraag"])
    # the zuidwester: a low crown, a short brim at the front and a long one sloping down over his neck
    g["bones"].append({"name": "torenwachter_hoed", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        c([-6.3, 24.4, -6.9], [12.6, 2.4, 12.6], sw["hoed"]),
        c([-4.8, 26.8, -5.4], [9.6, 1.3, 9.6], sw["hoed"]),
        c([-3.0, 28.1, -3.6], [6.0, 0.6, 6.0], sw["hoed"]),
        c([-6.9, 24.0, -9.4], [13.8, 0.7, 2.8], sw["hoedrand"]),
        c([-7.3, 23.9, 5.4], [14.6, 0.7, 5.2], sw["hoedrand"], rotation=[-24, 0, 0], pivot=[0, 24.2, 5.4]),
        c([-6.9, 24.0, -6.9], [0.7, 0.7, 12.6], sw["hoedrand"]), c([6.2, 24.0, -6.9], [0.7, 0.7, 12.6], sw["hoedrand"])]})
    # beard and moustache
    g["bones"].append({"name": "torenwachter_baard", "parent": "head", "pivot": [0, 14, -7.6], "cubes": [
        c([-4.6, 12.6, -7.9], [9.2, 2.2, 0.9], sw["baard"]),
        c([-3.4, 10.8, -7.8], [6.8, 1.8, 0.8], sw["baard"]),
        c([-1.8, 9.4, -7.7], [3.6, 1.4, 0.7], sw["baard"]),
        c([-3.4, 15.2, -8.0], [3.0, 1.0, 0.6], sw["baard"]), c([0.4, 15.2, -8.0], [3.0, 1.0, 0.6], sw["baard"]),
        c([-7.6, 13.0, -6.6], [0.8, 4.4, 2.4], sw["baard"]), c([6.8, 13.0, -6.6], [0.8, 4.4, 2.4], sw["baard"])]})
    # the coat with three gold buttons, the striped collar
    g["bones"].append({"name": "torenwachter_jas", "parent": "body", "pivot": [0, 7, 0], "cubes": [
        c([-5.2, 2.6, -4.3], [10.4, 7.6, 8.6], sw["jas"], inflate=0.15),
        c([-0.6, 3.6, -4.9], [1.2, 1.2, 0.5], sw["knoop"]), c([-0.6, 5.8, -4.9], [1.2, 1.2, 0.5], sw["knoop"]),
        c([-0.6, 8.0, -4.9], [1.2, 1.2, 0.5], sw["knoop"])]})
    g["bones"].append({"name": "torenwachter_kraag", "parent": "body", "pivot": [0, 11, 0], "cubes": [
        c([-5.4, 10.2, -4.6], [10.8, 1.8, 9.0], sw["kraag"], inflate=0.1)]})
    hulp.save_geo(h, f"{TORENWACHTER}.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.58, sat=0.36, val=0.92)
    rng = np.random.default_rng(SEED_TORENWACHTER)

    def naden(block):
        block[:, 15:17, :3] = GEEL_DONKER
        block[15:17, :, :3] = GEEL_DONKER

    def haren(block):
        for x in range(1, block.shape[1], 4):
            block[2:, x, :3] = (214, 212, 204)

    def revers(block):
        block[:, 14:18, :3] = JAS_DONKER
        block[-3:, :, :3] = JAS_DONKER

    def glim(block):
        block[8:14, 8:14, :3] = (255, 240, 170)

    def strepen(block):
        for x in range(0, block.shape[1], 8):
            block[:, x:x + 4, :3] = (236, 236, 232)

    hulp.paint_swatch(a, sw["hoed"], GEEL, rng, 7, naden)
    hulp.paint_swatch(a, sw["hoedrand"], GEEL_DONKER, rng, 6)
    hulp.paint_swatch(a, sw["baard"], BAARD, rng, 5, haren)
    hulp.paint_swatch(a, sw["jas"], JAS, rng, 6, revers)
    hulp.paint_swatch(a, sw["knoop"], GOUD, rng, 4, glim)
    hulp.paint_swatch(a, sw["kraag"], (206, 50, 46), rng, 5, strepen)
    h.save(Image.fromarray(a), "entity", "npc_torenwachterguh.png")


def peperteler(h):
    c = hulp.cube
    geo_file, g = hulp.sitting_geo(h, f"geometry.{PEPERTELER}")
    sw = hulp.swatches(g, ["stro", "band", "peper", "steel", "schort", "zak", "doek"])
    # the straw hat, a little askew, with a red pepper behind the band
    g["bones"].append({"name": "peperteler_hoed", "parent": "head", "pivot": [0, 26, -1], "rotation": [-5, 0, -4], "cubes": [
        c([-10.0, 25.4, -10.4], [20.0, 0.7, 19.0], sw["stro"]),
        c([-8.6, 25.0, -9.0], [17.2, 0.5, 16.2], sw["stro"]),
        c([-4.3, 26.1, -5.6], [8.6, 1.5, 9.4], sw["band"]),
        c([-4.1, 27.6, -5.4], [8.2, 2.0, 9.0], sw["stro"]),
        c([-3.3, 29.6, -4.6], [6.6, 0.6, 7.4], sw["stro"]),
        c([-4.4, 26.3, -6.5], [1.3, 4.2, 1.3], sw["peper"], rotation=[0, 0, 18], pivot=[-3.8, 26.3, -5.9]),
        c([-5.2, 30.2, -6.3], [0.6, 1.0, 0.6], sw["steel"], rotation=[0, 0, 18], pivot=[-3.8, 26.3, -5.9])]})
    g["bones"].append({"name": "peperteler_doek", "parent": "body", "pivot": [0, 13, 0], "cubes": [
        c([-5.2, 11.3, -4.6], [10.4, 1.9, 8.4], sw["doek"], inflate=0.1),
        c([-1.9, 9.6, -5.0], [3.8, 1.9, 0.7], sw["doek"]), c([-0.9, 8.6, -5.0], [1.8, 1.1, 0.7], sw["doek"])]})
    g["bones"].append({"name": "peperteler_schort", "parent": "body", "pivot": [0, 6, -4], "cubes": [
        c([-3.6, 2.0, -4.7], [7.2, 6.8, 0.7], sw["schort"]),
        c([-1.9, 3.0, -5.1], [3.8, 2.6, 0.5], sw["zak"]),
        c([-0.5, 5.0, -5.5], [1.0, 2.2, 0.5], sw["peper"]),                     # a pepper peeking out of the pocket
        c([-4.3, 6.8, -4.3], [8.6, 0.9, 8.6], sw["schort"], inflate=0.05)]})
    hulp.save_geo(h, f"{PEPERTELER}.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.02, sat=1.7, val=0.98)
    rng = np.random.default_rng(SEED_PEPERTELER)

    def vlechtwerk(block):
        for y in range(0, block.shape[0], 4):
            block[y, :, :3] = STRO_DONKER
            for x in range((y // 4 % 2) * 4, block.shape[1], 8):
                block[y:y + 4, x, :3] = STRO_DONKER

    def glans(block):
        block[4:10, 6:10, :3] = (255, 150, 130)

    def stiksel(block):
        for i in (2, -3):
            block[i, 2:-2, :3] = SCHORT_LICHT
            block[2:-2, i, :3] = SCHORT_LICHT

    def stippen(block):
        for y in range(3, block.shape[0], 8):
            for x in range(3 + (y // 8 % 2) * 4, block.shape[1], 8):
                block[y:y + 2, x:x + 2, :3] = (255, 244, 232)

    hulp.paint_swatch(a, sw["stro"], STRO, rng, 8, vlechtwerk)
    hulp.paint_swatch(a, sw["band"], SCHORT, rng, 6)
    hulp.paint_swatch(a, sw["peper"], PEPERROOD, rng, 5, glans)
    hulp.paint_swatch(a, sw["steel"], PEPERSTEEL, rng, 5)
    hulp.paint_swatch(a, sw["schort"], SCHORT, rng, 7, stiksel)
    hulp.paint_swatch(a, sw["zak"], SCHORT_LICHT, rng, 6, stiksel)
    hulp.paint_swatch(a, sw["doek"], PEPERROOD, rng, 6, stippen)
    h.save(Image.fromarray(a), "entity", "npc_pepertelerguh.png")


def check(h):
    problems = []
    for geo in (TORENWACHTER, PEPERTELER):
        if not os.path.exists(os.path.join(h.A, "geckolib", "models", "entity", f"{geo}.geo.json")):
            problems.append(f"missing model {geo}")
    for kind in ("torenwachterguh", "pepertelerguh"):
        if not os.path.exists(os.path.join(h.TEX, "entity", f"npc_{kind}.png")):
            problems.append(f"missing texture npc_{kind}")
    return problems


def preview(out):
    """Offline renders (front, side, behind) of both guhs as the generators last wrote them."""
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    tiles = []
    for geo, kind in ((TORENWACHTER, "torenwachterguh"), (PEPERTELER, "pepertelerguh")):
        pad = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity", f"{geo}.geo.json")
        q = wr.geo_quads(pad, f"guhs:entity/npc_{kind}")
        for yaw, pitch in ((205, -14), (270, -8), (25, -18)):
            tiles.append(wr.render(q, yaw, pitch, 360, margin=0.06))
    sheet = Image.new("RGBA", (360 * 3, 360 * 2), (62, 50, 54, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, ((i % 3) * 360, (i // 3) * 360))
    sheet.save(os.path.join(out, "toren_peper_guhs.png"))


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
