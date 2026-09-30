"""
3.0 (Guhverhalen), slice hemel: the look of the wolkenhoeder (GuhNpcEntity.Kind WOLKENHOEDER), made by hemel.build(h).

The sitting guh, but as soft as a cloud: pale cloud-white fur with a sky-blue shimmer, a little cloud puff between its ears,
a tiny golden halo above it, a fluffy cloud collar, and its golden shepherd's crook (herdersstaf) standing next to it.
Geo: assets/guhs/geckolib/models/entity/guh_npc_wolkenhoeder.geo.json (registered in SittingGuhRenderers.NPC_MODELEN by HemelClient),
texture: textures/entity/npc_wolkenhoeder.png. Extra bones sample 8x8 swatches in free parts of the UV sheet
(knuffeldal_npcs helpers).
"""
import numpy as np
from PIL import Image

from features import knuffeldal_npcs as npcs

GEO = "guh_npc_wolkenhoeder.geo.json"


def build(h):
    geo_file = npcs._load(h, "guh_sitting.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.guh_npc_wolkenhoeder"
    sw = npcs._swatches(geo, ["wolk", "wolk_blauw", "goud", "staf"])
    cube = npcs._cube
    # a little cloud between the ears (it bobs: HemelClient's animator)
    geo["bones"].append({"name": "hoeder_wolkje", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        cube([-3.5, 25.6, -4.0], [7, 2.4, 5], sw["wolk"], 0.1), cube([-5.2, 26.0, -3.4], [2.4, 1.8, 3.6], sw["wolk"]),
        cube([2.8, 26.0, -3.4], [2.4, 1.8, 3.6], sw["wolk"]), cube([-2.2, 27.6, -3.2], [3.6, 1.8, 3.2], sw["wolk"]),
        cube([0.6, 27.3, -2.6], [2.4, 1.5, 2.4], sw["wolk_blauw"])]})
    # the tiny golden halo floating above the cloud
    geo["bones"].append({"name": "hoeder_aureool", "parent": "head", "pivot": [0, 31, -1.5], "cubes": [
        cube([-2.6, 30.4, -4.4], [5.2, 0.6, 0.7], sw["goud"]), cube([-2.6, 30.4, 0.8], [5.2, 0.6, 0.7], sw["goud"]),
        cube([-3.3, 30.4, -3.7], [0.7, 0.6, 4.5], sw["goud"]), cube([2.6, 30.4, -3.7], [0.7, 0.6, 4.5], sw["goud"])]})
    # the fluffy cloud collar round the neck
    geo["bones"].append({"name": "hoeder_kraag", "parent": "body", "pivot": [0, 12, 0], "cubes": [
        cube([-5.6, 11.0, -5.2], [11.2, 2.6, 2.2], sw["wolk"], 0.15), cube([-6.2, 11.2, -3.4], [2.4, 2.4, 6.6], sw["wolk"], 0.1),
        cube([3.8, 11.2, -3.4], [2.4, 2.4, 6.6], sw["wolk"], 0.1), cube([-4.6, 11.2, 3.0], [9.2, 2.2, 1.8], sw["wolk"]),
        cube([-2.0, 10.0, -5.8], [4.0, 1.6, 1.4], sw["wolk_blauw"])]})
    # the golden shepherd's crook, standing at its right side (the hook curls towards the guh)
    geo["bones"].append({"name": "hoeder_staf", "parent": "body", "pivot": [-9.5, 2, -5], "cubes": [
        cube([-10.0, 0.0, -5.5], [1.0, 27.0, 1.0], sw["staf"]), cube([-10.2, 26.6, -5.6], [3.4, 1.0, 1.2], sw["staf"]),
        cube([-7.6, 24.0, -5.6], [1.0, 3.2, 1.2], sw["staf"]), cube([-10.3, 12.0, -5.7], [1.4, 1.2, 1.4], sw["goud"]),
        cube([-7.8, 23.2, -5.7], [1.4, 1.0, 1.4], sw["goud"])]})
    npcs._save_geo(h, GEO, geo_file)

    src = Image.open(f"{h.TEX}/entity/guh_sitting.png").convert("RGBA")
    img = h.recolour(src, hue=0.58, sat=0.16, val=1.12, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(3010)

    def wolk(block):
        block[..., :3] = (250, 251, 255)
        for (cx, cy, r) in ((8, 20, 7), (22, 12, 6), (26, 26, 5)):
            for yy in range(32):
                for xx in range(32):
                    if (xx - cx) ** 2 + (yy - cy) ** 2 < r * r:
                        block[yy, xx, :3] = (block[yy, xx, :3] * 0.6 + np.array((200, 226, 250)) * 0.4).astype(np.uint8)
    npcs._paint(a, sw["wolk"], (250, 251, 255), rng, 6, wolk)
    npcs._paint(a, sw["wolk_blauw"], (206, 228, 252), rng, 6)
    npcs._paint(a, sw["goud"], (250, 206, 80), rng, 10)

    def staf(block):
        for yy in range(32):
            t = abs(((yy * 2) % 16) - 8) / 8
            block[yy, :, :3] = (np.array((236, 186, 60)) * (0.8 + 0.2 * t)).astype(np.uint8)
        block[:, 10:13, :3] = (255, 240, 170)
    npcs._paint(a, sw["staf"], (236, 186, 60), rng, 6, staf)
    h.save(Image.fromarray(a), "entity", "npc_wolkenhoeder.png")
