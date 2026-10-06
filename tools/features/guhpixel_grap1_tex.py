"""
Textures and models of the guhpixel slice "grap1": the keepsake block Eilandje-in-een-fles (skyblok_fles) and the three
lobby NPCs with their own models (the sitting guh plus a headset / pillow armour / a parachute pack and goggles).
Everything is drawn here with fixed seeds.
"""
import numpy as np
from PIL import Image

from features import guhpixel_lib as lib
from features import sterrenwacht_hulp as hulp


# =====================================================================================================================
# Eilandje-in-een-fles
# =====================================================================================================================
def _glas():
    a = np.zeros((16, 16, 4), np.uint8)
    a[..., :3] = (196, 230, 244)
    a[..., 3] = 58
    a[0, :, 3] = a[15, :, 3] = 150
    a[:, 0, 3] = a[:, 15, 3] = 150
    a[0, :, :3] = a[:, 0, :3] = (232, 248, 255)
    for i in range(5):                                  # a highlight
        a[3 + i, 3, :] = (255, 255, 255, 170)
    a[3, 4, :] = (255, 255, 255, 150)
    a[11, 12, :] = (255, 255, 255, 120)
    return Image.fromarray(a)


def _eiland():
    """One little atlas: grass (0,0)-(8,8), dirt (8,0)-(16,8), leaves (0,8)-(8,16), log (8,8)-(12,16), cork (12,8)-(16,12),
    bed (12,12)-(16,16)."""
    rng = np.random.default_rng(32101)
    a = np.zeros((16, 16, 4), np.uint8)
    a[..., 3] = 255

    def vul(x0, y0, x1, y1, kleur, var):
        n = rng.normal(0, var, (y1 - y0, x1 - x0, 1))
        a[y0:y1, x0:x1, :3] = np.clip(np.array(kleur) + n, 0, 255)
    vul(0, 0, 8, 8, (106, 178, 74), 9)
    vul(8, 0, 16, 8, (134, 96, 67), 8)
    a[0, 8:16, :3] = (98, 168, 70)                       # a grassy edge on the dirt
    vul(0, 8, 8, 16, (62, 132, 54), 12)
    vul(8, 8, 12, 16, (104, 82, 50), 5)
    a[8:16, 9, :3] = (84, 64, 38)
    vul(12, 8, 16, 12, (196, 156, 104), 6)
    vul(12, 12, 16, 16, (196, 52, 52), 4)
    a[12:16, 12, :3] = (240, 240, 240)                   # the pillow
    return Image.fromarray(a)


def _el(frm, to, tex, uv, faces=("down", "up", "north", "south", "west", "east")):
    return {"from": frm, "to": to, "faces": {f: {"texture": tex, "uv": uv} for f in faces}}


def fles(h, naam, lore):
    h.save(_glas(), "block", "skyblok_fles_glas.png")
    h.save(_eiland(), "block", "skyblok_fles_eiland.png")
    zij = ("north", "south", "west", "east")
    elements = [
        # the island: dirt with a grass top
        _el([5.5, 1, 5.5], [10.5, 3, 10.5], "#eiland", [8, 0, 13, 2], zij),
        _el([5.5, 1, 5.5], [10.5, 3, 10.5], "#eiland", [0, 0, 5, 5], ("up",)),
        _el([5.5, 1, 5.5], [10.5, 3, 10.5], "#eiland", [9, 2, 14, 7], ("down",)),
        # the tree, the bed
        _el([7, 3, 8], [8, 5.5, 9], "#eiland", [8, 8, 9, 10.5]),
        _el([6, 5.5, 7], [9, 8, 10], "#eiland", [0, 8, 3, 10.5]),
        _el([8.5, 3, 6], [10, 3.6, 7.5], "#eiland", [12, 12, 13.5, 13.5]),
        # the bottle: body, neck, cork
        _el([4, 0, 4], [12, 10, 12], "#glas", [0, 0, 16, 16]),
        _el([6.5, 10, 6.5], [9.5, 12, 9.5], "#glas", [4, 0, 12, 6]),
        _el([6.25, 12, 6.25], [9.75, 13.5, 9.75], "#eiland", [12, 8, 15.5, 9.5]),
    ]
    lib.deco(h, "skyblok_fles", elements, {"particle": "guhs:block/skyblok_fles_glas", "glas": "guhs:block/skyblok_fles_glas",
                                           "eiland": "guhs:block/skyblok_fles_eiland"}, naam, lore,
             display={"gui": {"rotation": [20, 35, 0], "translation": [0, 1.5, 0], "scale": [1.1, 1.1, 1.1]},
                      "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
                      "fixed": {"rotation": [0, 0, 0], "translation": [0, 1, 0], "scale": [1.0, 1.0, 1.0]},
                      "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2.5, 0], "scale": [0.55, 0.55, 0.55]},
                      "firstperson_righthand": {"rotation": [0, 35, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]}})
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:skyblok_fles"])


# =====================================================================================================================
# the three lobby NPCs
# =====================================================================================================================
def _skyblok_guh(h):
    """Sky blue, deadly serious: a headset with a microphone."""
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_skyblok_guh")
    sw = hulp.swatches(geo, ["beugel", "oorschelp", "microfoon"])
    c = hulp.cube
    geo["bones"].append({"name": "skyblok_headset", "parent": "head", "pivot": [0, 26, 1], "cubes": [
        c([-8.6, 25.9, 0.6], [17.2, 1.0, 2.4], sw["beugel"]),
        c([-9.0, 21.0, 0.6], [0.9, 5.4, 2.4], sw["beugel"]), c([8.1, 21.0, 0.6], [0.9, 5.4, 2.4], sw["beugel"]),
        c([-9.6, 16.4, -2.4], [1.6, 5.2, 5.6], sw["oorschelp"]), c([8.0, 16.4, -2.4], [1.6, 5.2, 5.6], sw["oorschelp"]),
        c([-9.3, 16.2, -8.6], [0.7, 0.8, 6.4], sw["beugel"]),
        c([-9.6, 15.6, -9.8], [1.3, 1.8, 1.4], sw["microfoon"])]})
    hulp.save_geo(h, "guh_npc_skyblok_guh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.56, sat=1.8, val=1.0)
    rng = np.random.default_rng(32111)
    hulp.paint_swatch(a, sw["beugel"], (52, 56, 70), rng, 5)

    def schelp(block):
        block[8:24, 8:24, :3] = (120, 220, 255)          # a glowing gamer ring
        block[12:20, 12:20, :3] = (34, 36, 48)
    hulp.paint_swatch(a, sw["oorschelp"], (34, 36, 48), rng, 4, schelp)
    hulp.paint_swatch(a, sw["microfoon"], (236, 80, 96), rng, 5)
    h.save(Image.fromarray(a), "entity", "npc_skyblok_guh.png")


def _bedwars_guh(h):
    """Warm red, in an armour of pillows: one on the chest, one on the back, one on the head, tied on with a rope."""
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_bedwars_guh")
    sw = hulp.swatches(geo, ["kussen", "kussen_roze", "touw"])
    c = hulp.cube
    geo["bones"].append({"name": "bedwars_harnas", "parent": "body", "pivot": [0, 6, 0], "cubes": [
        c([-4.8, 3.0, -6.4], [9.6, 7.2, 2.4], sw["kussen"], inflate=0.1),
        c([-4.8, 3.0, 4.0], [9.6, 7.2, 2.4], sw["kussen_roze"], inflate=0.1),
        c([-5.4, 6.0, -6.7], [10.8, 1.0, 13.4], sw["touw"])]})
    geo["bones"].append({"name": "bedwars_helm", "parent": "head", "pivot": [0, 26, 0], "cubes": [
        c([-4.4, 25.8, -5.6], [8.8, 2.8, 10.4], sw["kussen"], inflate=0.1),
        c([-0.5, 25.6, -6.0], [1.0, 3.2, 11.2], sw["touw"])]})
    hulp.save_geo(h, "guh_npc_bedwars_guh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.0, sat=2.2, val=1.0)
    rng = np.random.default_rng(32112)

    def naden(block):
        block[0:2, :, :3] = block[30:32, :, :3] = (214, 214, 226)
        block[:, 0:2, :3] = block[:, 30:32, :3] = (214, 214, 226)
        block[14:18, 14:18, :3] = (200, 200, 214)        # the button in the middle
    hulp.paint_swatch(a, sw["kussen"], (248, 248, 252), rng, 4, naden)

    def streepjes(block):
        for x in range(32):
            if (x // 4) % 2:
                block[:, x, :3] = (255, 255, 255)
    hulp.paint_swatch(a, sw["kussen_roze"], (255, 170, 204), rng, 4, streepjes)
    hulp.paint_swatch(a, sw["touw"], (170, 128, 78), rng, 8)
    h.save(Image.fromarray(a), "entity", "npc_bedwars_guh.png")


def _vadsnite_guh(h):
    """Purple, ready to jump: a parachute pack with a rolled-up pink parachute, goggles on the forehead."""
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_vadsnite_guh")
    sw = hulp.swatches(geo, ["rugzak", "scherm", "bril", "band"])
    c = hulp.cube
    geo["bones"].append({"name": "vadsnite_rugzak", "parent": "body", "pivot": [0, 6, 4], "cubes": [
        c([-4.2, 3.2, 4.0], [8.4, 7.6, 3.6], sw["rugzak"]),
        c([-3.6, 10.8, 4.2], [7.2, 2.2, 3.0], sw["scherm"]),
        c([-5.3, 9.0, -4.4], [10.6, 0.9, 8.6], sw["band"]),
        c([-5.3, 4.6, -4.4], [10.6, 0.9, 8.6], sw["band"])]})
    geo["bones"].append({"name": "vadsnite_bril", "parent": "head", "pivot": [0, 24, -7], "cubes": [
        c([-6.0, 23.3, -7.9], [5.2, 3.0, 1.0], sw["bril"]), c([0.8, 23.3, -7.9], [5.2, 3.0, 1.0], sw["bril"]),
        c([-7.3, 24.2, -7.2], [14.6, 1.2, 13.4], sw["band"])]})
    hulp.save_geo(h, "guh_npc_vadsnite_guh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.76, sat=1.8, val=1.0)
    rng = np.random.default_rng(32113)

    def gespen(block):
        block[6:10, 4:28, :3] = (60, 70, 96)
        block[20:24, 4:28, :3] = (60, 70, 96)
        block[12:18, 13:19, :3] = (250, 214, 90)
    hulp.paint_swatch(a, sw["rugzak"], (84, 110, 168), rng, 7, gespen)

    def banen(block):
        for x in range(32):
            if (x // 5) % 2:
                block[:, x, :3] = (255, 255, 255)
    hulp.paint_swatch(a, sw["scherm"], (255, 140, 196), rng, 5, banen)

    def glas(block):
        block[6:26, 6:26, :3] = (150, 214, 246)
        block[8:12, 9:13, :3] = (255, 255, 255)
    hulp.paint_swatch(a, sw["bril"], (250, 214, 90), rng, 5, glas)
    hulp.paint_swatch(a, sw["band"], (52, 56, 70), rng, 5)
    h.save(Image.fromarray(a), "entity", "npc_vadsnite_guh.png")


def npcs(h):
    _skyblok_guh(h)
    _bedwars_guh(h)
    _vadsnite_guh(h)
