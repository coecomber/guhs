"""
De Elf-Guhjestocht (2.9) - the characters' looks (the sitting guh with extra bones, see sterrenwacht_hulp):

  Schaatsmeester Guhglij   a frosty-blue guh with an orange-and-white striped pompom hat (bone schaatsmeester_pompon
                           bounces), a big orange scarf, a whistle on a cord and a pair of skates hanging round his neck
                           (bone schaatsmeester_schaatsen swings).
  Stempelguh               a cream guh with a rubber stamp in its right paw (bone stempel_stempel; the arm_right bone does
                           the PLOF). Each village has its own hat and scarf (bones stempel_hoed_<n> / stempel_sjaal_<n>;
                           the client shows only the one of its village).
"""
import numpy as np

from features import sterrenwacht_hulp as hulp

HOEDEN = {   # village: (shape, hat colour, band/scarf colour)
    1: ("muts", (242, 120, 36), (255, 255, 255)),      # Guhwarden: Frisian orange
    2: ("pet", (250, 250, 255), (40, 60, 140)),         # Snuh: sailor cap
    3: ("baret", (120, 70, 40), (250, 200, 120)),       # IJlguh: chocolate beret
    4: ("emmer", (90, 150, 70), (250, 230, 120)),       # Knabbelsloten: green bucket hat (music band)
    5: ("pet", (30, 40, 90), (240, 200, 70)),           # Vadsvoren: captain's cap
    6: ("muts", (220, 50, 60), (255, 255, 255)),        # Guhdeloopen: red-white beanie
    7: ("helm", (250, 210, 50), (40, 40, 40)),          # Vadskum: ice hockey helmet
    8: ("hoge", (40, 36, 48), (200, 60, 70)),           # Knabbelsward: tall black hat (church)
    9: ("baret", (130, 130, 140), (250, 160, 60)),      # Guhlingen: miller's flat cap
    10: ("punt", (40, 50, 120), (250, 220, 90)),        # Franeguh: a starry pointed hat
    11: ("muts", (180, 220, 250), (255, 255, 255)),     # Dokguh: snowy pompom hat
}
H = [0, 13, 0]


def _hoed(shape, sw_h, sw_b):
    c = hulp.cube
    if shape == "muts":
        return [c([-5, 25.2, -5.5], [10, 1.6, 9], sw_b), c([-4.6, 26.8, -5.1], [9.2, 2.8, 8.2], sw_h),
                c([-3.6, 29.6, -4.1], [7.2, 1.2, 6.2], sw_h), c([-1.2, 30.8, -1.8], [2.4, 2.4, 2.4], sw_b)]
    if shape == "pet":
        return [c([-4.8, 25.2, -4.8], [9.6, 2.6, 8.6], sw_h), c([-4.8, 25.2, -7.6], [9.6, 0.6, 2.8], sw_b),
                c([-4.9, 25.8, -4.9], [9.8, 0.8, 8.8], sw_b)]
    if shape == "baret":
        return [c([-5.4, 25.4, -5.4], [10.8, 1.8, 9.8], sw_h), c([-0.5, 27.2, -0.8], [1, 1, 1], sw_b)]
    if shape == "emmer":
        return [c([-5.8, 25.0, -5.8], [11.6, 0.6, 10.6], sw_h), c([-4.2, 25.6, -4.2], [8.4, 3.2, 7.4], sw_h),
                c([-4.3, 25.6, -4.3], [8.6, 0.8, 7.6], sw_b)]
    if shape == "helm":
        return [c([-5.2, 24.8, -5.4], [10.4, 3.4, 9.6], sw_h), c([-5.3, 24.6, -5.5], [10.6, 0.8, 9.8], sw_b),
                c([-0.6, 28.2, -4.6], [1.2, 0.6, 7.8], sw_b)]
    if shape == "hoge":
        return [c([-5.6, 25.6, -5.6], [11.2, 0.8, 10.2], sw_h), c([-3.6, 26.4, -3.6], [7.2, 6.4, 6.2], sw_h),
                c([-3.7, 26.4, -3.7], [7.4, 1.4, 6.4], sw_b)]
    if shape == "punt":
        return [c([-4.6, 25.4, -4.6], [9.2, 2, 8.2], sw_h), c([-3.2, 27.4, -3.2], [6.4, 2.4, 5.4], sw_h),
                c([-1.8, 29.8, -1.8], [3.6, 2.4, 3.2], sw_h), c([-0.6, 32.2, -0.6], [1.2, 1.8, 1.2], sw_b),
                c([-1.8, 27.9, -3.4], [1.0, 1.0, 0.4], sw_b), c([1.2, 29.2, -2.0], [0.8, 0.8, 0.4], sw_b)]
    raise ValueError(shape)


def _sjaal(sw):
    c = hulp.cube
    return [c([-5.4, 11.4, -5.6], [10.8, 2.0, 9.4], sw), c([-4.6, 6.8, -5.9], [2.4, 4.8, 0.8], sw)]


def stempelguh(h):
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_stempelguh")
    names = ["stempel_hout", "stempel_rubber"] + [f"hoed_{n}" for n in HOEDEN] + [f"band_{n}" for n in HOEDEN]
    sw = hulp.swatches(geo, names)
    c = hulp.cube
    geo["bones"].append({"name": "stempel_stempel", "parent": "arm_right", "pivot": [-3, 8.5, -5], "cubes": [
        c([-3.6, 10.6, -5.6], [1.2, 1.2, 1.2], sw["stempel_hout"]),       # the knob
        c([-3.3, 8.4, -5.3], [0.6, 2.4, 0.6], sw["stempel_hout"]),        # the stem
        c([-4.4, 7.0, -6.4], [2.8, 1.4, 2.8], sw["stempel_hout"]),        # the block
        c([-4.3, 6.4, -6.3], [2.6, 0.6, 2.6], sw["stempel_rubber"])]})   # the rubber (inked orange)
    for n, (shape, _, _) in HOEDEN.items():
        geo["bones"].append({"name": f"stempel_hoed_{n}", "parent": "head", "pivot": H, "cubes": _hoed(shape, sw[f"hoed_{n}"], sw[f"band_{n}"])})
        geo["bones"].append({"name": f"stempel_sjaal_{n}", "parent": "body", "pivot": [0, 12, 0], "cubes": _sjaal(sw[f"band_{n}"])})
    hulp.save_geo(h, "guh_npc_stempelguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, 0.10, 0.28, 1.04)
    rng = np.random.default_rng(29062)
    hulp.paint_swatch(a, sw["stempel_hout"], (160, 110, 64), rng, 8)
    hulp.paint_swatch(a, sw["stempel_rubber"], (242, 120, 36), rng, 6)
    for n, (_, hoed, band) in HOEDEN.items():
        pattern = None
        if n == 10:
            def pattern(block):
                for (y, x) in ((5, 6), (12, 20), (22, 9), (26, 25), (16, 14)):
                    block[y:y + 2, x:x + 2, :3] = (250, 230, 120)
        elif n in (1, 6, 11):
            def pattern(block, c2=band):
                for y in range(0, 32, 8):
                    block[y:y + 3, :, :3] = c2
        hulp.paint_swatch(a, sw[f"hoed_{n}"], hoed, rng, 6, pattern)
        hulp.paint_swatch(a, sw[f"band_{n}"], band, rng, 6)
    from PIL import Image
    h.save(Image.fromarray(a), "entity", "npc_stempelguh.png")


def schaatsmeester(h):
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_schaatsmeesterguh")
    sw = hulp.swatches(geo, ["muts", "pompon", "sjaal", "fluit", "koord", "schaats", "mes"])
    c = hulp.cube
    geo["bones"].append({"name": "schaatsmeester_muts", "parent": "head", "pivot": H, "cubes": [
        c([-5.2, 25.0, -5.6], [10.4, 1.8, 9.2], sw["sjaal"]),
        c([-4.8, 26.8, -5.2], [9.6, 3.0, 8.4], sw["muts"]),
        c([-3.8, 29.8, -4.2], [7.6, 1.4, 6.4], sw["muts"])]})
    geo["bones"].append({"name": "schaatsmeester_pompon", "parent": "schaatsmeester_muts", "pivot": [0, 31.2, -1], "cubes": [
        c([-1.6, 31.2, -2.6], [3.2, 3.2, 3.2], sw["pompon"])]})
    geo["bones"].append({"name": "schaatsmeester_sjaal", "parent": "body", "pivot": [0, 12, 0], "cubes": [
        c([-5.6, 11.2, -5.8], [11.2, 2.2, 9.8], sw["sjaal"]),
        c([2.2, 5.2, -6.2], [2.6, 6.2, 0.9], sw["sjaal"])]})
    geo["bones"].append({"name": "schaatsmeester_fluit", "parent": "body", "pivot": [0, 11, -5], "cubes": [
        c([-1.2, 7.6, -6.6], [2.4, 1.2, 1.4], sw["fluit"]),
        c([-2.6, 8.6, -5.9], [0.4, 2.8, 0.4], sw["koord"]),
        c([2.2, 8.6, -5.9], [0.4, 2.8, 0.4], sw["koord"])]})
    geo["bones"].append({"name": "schaatsmeester_schaatsen", "parent": "body", "pivot": [0, 11, -4], "cubes": [
        c([-4.8, 5.4, -6.4], [1.8, 2.2, 3.0], sw["schaats"]), c([-4.6, 4.8, -6.8], [0.4, 0.6, 3.8], sw["mes"]),
        c([-3.0, 5.0, -6.2], [1.8, 2.2, 3.0], sw["schaats"]), c([-2.8, 4.4, -6.6], [0.4, 0.6, 3.8], sw["mes"])]})
    hulp.save_geo(h, "guh_npc_schaatsmeesterguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, 0.56, 0.35, 1.06)
    rng = np.random.default_rng(29061)

    def streep(block):
        for y in range(0, 32, 8):
            block[y:y + 4, :, :3] = (255, 255, 255)
    hulp.paint_swatch(a, sw["muts"], (242, 120, 36), rng, 6, streep)
    hulp.paint_swatch(a, sw["pompon"], (255, 255, 255), rng, 10)
    hulp.paint_swatch(a, sw["sjaal"], (242, 120, 36), rng, 8)
    hulp.paint_swatch(a, sw["fluit"], (220, 220, 230), rng, 8)
    hulp.paint_swatch(a, sw["koord"], (240, 70, 90), rng, 4)
    hulp.paint_swatch(a, sw["schaats"], (250, 150, 196), rng, 6)
    hulp.paint_swatch(a, sw["mes"], (220, 232, 245), rng, 4)
    from PIL import Image
    h.save(Image.fromarray(a), "entity", "npc_schaatsmeesterguh.png")


def build(h):
    schaatsmeester(h)
    stempelguh(h)
