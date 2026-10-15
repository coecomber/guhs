"""
Het Snuffeleiland, the dock: the two things that are no plain blocks.

  - Kapitein Zoutsnoet's boat "De Natte Neus" (entity guhs:steiger_boot; GeckoLib model, animations, texture): a stubby
    wooden sloop, five blocks long: a thick hull with raised sides (whoever stands in it is hidden to the knees), a bow
    that narrows in three steps with a bowsprit and a lantern, a transom with a rudder and a tiller, a mast with a
    cream fore-and-aft sail (a paw print on it) and a red pennant, a crate in the bow. The model's front (-z) is the
    bow; y 0 is the keel, the deck is at DEK px. Every face samples its own region of a 64 x 64 sheet.
    Animations: "dobber" (moored and in calm weather: the sail breathes, the pennant flaps) and "storm" (the sail
    slams from side to side); the rolling of the hull itself is the renderer's (client.SnuffelsteigerClient).
  - one animation more for every PUPPY of the kern's dogs: "lig" (lying flat, the legs out to the front and the back,
    the head down, breathing): the sickbed and the collapse in the feast's cutscene (SteigerBewoner.java). Written into
    the animation files that snuffel.py made (this module runs after it); nothing else of the approved dogs is touched.

  build(h)   writes geckolib/models/entity/steiger_boot.geo.json, geckolib/animations/entity/steiger_boot.animation.json,
             textures/entity/steiger_boot.png, and adds "lig" to geckolib/animations/entity/snuffelhond_<ras>_pup.animation.json
"""
import numpy as np
from PIL import Image

from . import snuffel_modellen as modellen

BOOT = "steiger_boot"
TEX = 64
DEK = 7.0                    # model px of the deck over the keel (Steiger.BOOT_DEK = DEK / 16)
DIEPGANG = 5.2               # how deep the keel lies under the water's surface (px)
# regions of the sheet: name -> (u, v, w, h)
REGIO = {
    "hout": (0, 0, 32, 16),          # the hull: warm brown planks
    "dek": (0, 16, 32, 16),          # the deck: paler planks
    "rand": (32, 0, 16, 8),          # the trim along the top of the sides: dark
    "mast": (32, 8, 8, 8),
    "wimpel": (40, 8, 8, 8),
    "licht": (48, 8, 8, 8),
    "krat": (32, 16, 16, 16),
    "zeil": (0, 32, 32, 32),
    "zeilrand": (32, 32, 8, 8),
}


def _uv(name, override=None):
    out = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        u, v, w, h = REGIO[(override or {}).get(f, name)]
        out[f] = {"uv": [u, v], "uv_size": [w, h]}
    return out


def _cube(origin, size, name, override=None):
    return {"origin": origin, "size": size, "uv": _uv(name, override)}


def geo():
    boven = {"up": "rand"}
    romp = [
        _cube([-14, 0, -24], [28, DEK, 52], "hout", {"up": "dek"}),             # the bottom, thick: its top is the deck
        _cube([-17, 3, -24], [3, 13, 52], "hout", boven),                        # the sides
        _cube([14, 3, -24], [3, 13, 52], "hout", boven),
        _cube([-14, 1, -30], [28, 15, 6], "hout", boven),                        # the bow narrows in three steps
        _cube([-10, 2, -35], [20, 14, 5], "hout", boven),
        _cube([-6, 3, -39], [12, 13, 4], "hout", boven),
        _cube([-1, 13, -47], [2, 2, 9], "mast"),                                 # the bowsprit
        _cube([-14, 1, 28], [28, 15, 4], "hout", boven),                         # the transom
        _cube([-1, -1, 32], [2, 13, 4], "rand"),                                 # the rudder
        _cube([-0.5, 16, 21], [1, 1, 12], "mast"),                               # the tiller
        _cube([-14, DEK, 17], [28, 3.5, 4], "dek", {"north": "rand", "south": "rand"}),   # the helmsman's bench
        _cube([5, DEK, -22], [7, 6, 7], "krat"),                                 # a crate in the bow
        _cube([-12, DEK, -21], [5, 3, 5], "mast"),                               # a coil of rope
    ]
    bones = [{"name": "root", "pivot": [0, 0, 0]},
             {"name": "romp", "parent": "root", "pivot": [0, 0, 0], "cubes": romp},
             {"name": "mast", "parent": "romp", "pivot": [0, DEK, 2], "cubes": [
                 _cube([-1.5, DEK, 0.5], [3, 60, 3], "mast"),
                 _cube([-0.75, 26, 3.5], [1.5, 1.5, 28], "mast"),                # the boom
             ]},
             # the sail: three thin panes on the centre line that step in towards the top (a fore-and-aft sail)
             {"name": "zeil", "parent": "mast", "pivot": [0, 44, 3.5], "cubes": [
                 _cube([-0.3, 27.5, 3.5], [0.6, 12, 27], "zeilrand", {"east": "zeil", "west": "zeil"}),
                 _cube([-0.3, 39.5, 3.5], [0.6, 12, 21], "zeilrand", {"east": "zeil", "west": "zeil"}),
                 _cube([-0.3, 51.5, 3.5], [0.6, 12, 13], "zeilrand", {"east": "zeil", "west": "zeil"}),
             ]},
             {"name": "wimpel", "parent": "mast", "pivot": [0, 65, 0.5], "cubes": [
                 _cube([-0.3, 62, -7], [0.6, 4.5, 7.5], "wimpel")]},
             {"name": "lantaarn", "parent": "romp", "pivot": [0, 21, -37], "cubes": [
                 _cube([-0.5, 16, -37.5], [1, 5, 1], "mast"),
                 _cube([-1.5, 21, -38.5], [3, 4, 3], "licht"),
                 _cube([-2, 25, -39], [4, 1, 4], "rand")]}]
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{BOOT}", "texture_width": TEX, "texture_height": TEX,
                        "visible_bounds_width": 7, "visible_bounds_height": 6, "visible_bounds_offset": [0, 2, 0]},
        "bones": bones}]}


def animation():
    kf = modellen.kf
    dobber = {"zeil": {"rotation": kf((0, (0, -3, 0)), (2, (0, 3, 0)), (4, (0, -3, 0)))},
              "wimpel": {"rotation": kf((0, (0, -14, 0)), (0.5, (0, 12, 0)), (1, (0, -8, 0)), (1.5, (0, 16, 0)), (2, (0, -14, 0)))},
              "lantaarn": {"rotation": kf((0, (0, 0, -4)), (2, (0, 0, 4)), (4, (0, 0, -4)))}}
    storm = {"zeil": {"rotation": kf((0, (0, -16, 0)), (0.35, (0, 14, 0)), (0.7, (0, -10, 0)), (1.05, (0, 18, 0)), (1.4, (0, -16, 0)))},
             "mast": {"rotation": kf((0, (0, 0, -2)), (0.7, (0, 0, 2)), (1.4, (0, 0, -2)))},
             "wimpel": {"rotation": kf((0, (0, -30, 0)), (0.2, (0, 26, 0)), (0.4, (0, -22, 0)), (0.6, (0, 30, 0)), (0.8, (0, -30, 0)))},
             "lantaarn": {"rotation": kf((0, (0, 0, -14)), (0.5, (0, 0, 14)), (1, (0, 0, -14)))}}
    return {"format_version": "1.8.0", "animations": {"dobber": modellen.anim(4.0, dobber), "storm": modellen.anim(1.4, storm)}}


def texture():
    rng = np.random.default_rng(2130_5203)
    a = np.zeros((TEX, TEX, 4), np.uint8)

    def vlak(naam, kleur, ruis=7):
        u, v, w, h = REGIO[naam]
        blok = np.clip(np.array(kleur, float)[None, None, :] + rng.normal(0, ruis, (h, w, 1)), 0, 255)
        a[v:v + h, u:u + w, :3] = blok.astype(np.uint8)
        a[v:v + h, u:u + w, 3] = 255
        return u, v, w, h

    def planken(naam, kleur, naad, stap=4, ruis=6):
        u, v, w, h = vlak(naam, kleur, ruis)
        for y in range(stap - 1, h, stap):
            a[v + y, u:u + w, :3] = naad
        for i, y0 in enumerate(range(0, h, stap)):                      # butt joints, staggered per plank
            x = (5 + i * 11) % w
            a[v + y0:v + min(h, y0 + stap - 1), u + x, :3] = naad

    planken("hout", (150, 98, 58), (104, 66, 40))
    planken("dek", (196, 154, 104), (150, 112, 74))
    vlak("rand", (92, 60, 40), 5)
    vlak("mast", (116, 80, 52), 5)
    planken("krat", (176, 132, 82), (112, 78, 48), stap=5)
    u, v, w, h = REGIO["krat"]
    a[v, u:u + w, :3] = a[v + h - 1, u:u + w, :3] = (112, 78, 48)
    a[v:v + h, u, :3] = a[v:v + h, u + w - 1, :3] = (112, 78, 48)
    # the pennant: red with a white paw
    u, v, w, h = vlak("wimpel", (214, 60, 70), 4)
    for (x, y) in ((3, 4), (4, 4), (3, 5), (4, 5), (2, 2), (4, 1), (5, 2)):
        a[v + y, u + x, :3] = (255, 244, 236)
    # the lantern: warm glass in a dark frame
    u, v, w, h = vlak("licht", (255, 214, 120), 6)
    a[v, u:u + w, :3] = a[v + h - 1, u:u + w, :3] = (70, 52, 44)
    a[v:v + h, u, :3] = a[v:v + h, u + w - 1, :3] = (70, 52, 44)
    a[v + 3:v + 5, u + 3:u + 5, :3] = (255, 248, 200)
    # the sail: cream canvas with seams, a big paw print in guh pink
    u, v, w, h = vlak("zeil", (246, 236, 212), 4)
    for x in range(7, w, 8):
        a[v:v + h, u + x, :3] = (226, 212, 184)
    for y in (0, h - 1):
        a[v + y, u:u + w, :3] = (212, 196, 164)
    poot = (238, 141, 173)
    cx, cy = 14, 17
    yy, xx = np.mgrid[0:h, 0:w]
    kussen = ((xx - cx) / 5.2) ** 2 + ((yy - cy - 2.5) / 4.2) ** 2 <= 1
    for dx, dy, r in ((-6.2, -3.6, 2.1), (-2.2, -6.6, 2.2), (2.4, -6.6, 2.2), (6.2, -3.6, 2.1)):
        kussen |= (xx - cx - dx) ** 2 + (yy - cy - dy) ** 2 <= r * r
    regio = a[v:v + h, u:u + w]
    regio[kussen, :3] = poot
    vlak("zeilrand", (226, 212, 184), 4)
    return a


# =====================================================================================================================
# the puppies lie down
# =====================================================================================================================
def lig_anim(d):
    """Lying flat for a dog with the numbers d (snuffel_modellen.maten): the body sinks until the legs, turned out to the
    front and to the back, lie on the ground; the head rests low and a little askew; slow breathing."""
    kf, anim = modellen.kf, modellen.anim
    zak = max(0.0, d.pl - d.pd * 0.5)
    lig = {"lijf": {"position": kf((0, (0, -zak, 0)), (2.4, (0, -zak, 0))),
                    "scale": kf((0, (1, 1, 1)), (1.2, (1.03, 1.06, 1.02)), (2.4, (1, 1, 1)))},
           "kop": {"rotation": kf((0, (16, 0, 12)), (1.2, (18, 0, 12)), (2.4, (16, 0, 12)))},
           "poot_lv": {"rotation": kf((0, (-84, 0, -8)))}, "poot_rv": {"rotation": kf((0, (-84, 0, 8)))},
           "poot_la": {"rotation": kf((0, (84, 0, -10)))}, "poot_ra": {"rotation": kf((0, (84, 0, 10)))},
           "oor_links": {"rotation": kf((0, (0, 0, -10)))}, "oor_rechts": {"rotation": kf((0, (0, 0, 10)))},
           "staart": {"rotation": kf((0, (0, 0, 0)), (1.2, (0, 5, 0)), (2.4, (0, 0, 0)))}}
    return anim(2.4, lig)


def pups():
    return [ras for ras, r in modellen.RASSEN.items() if ras != "speurhond"]


def build(h):
    h.w(f"{h.A}/geckolib/models/entity/{BOOT}.geo.json", geo())
    h.w(f"{h.A}/geckolib/animations/entity/{BOOT}.animation.json", animation())
    h.save(Image.fromarray(texture()), "entity", f"{BOOT}.png")
    for ras in pups():
        d = modellen.maten(ras, True)

        def met_lig(data, d=d):
            data["animations"]["lig"] = lig_anim(d)
        h.patch_json(f"{h.A}/geckolib/animations/entity/snuffelhond_{ras}_pup.animation.json", met_lig)


def check(h):
    import json
    import os
    problems = []
    for pad in (f"geckolib/models/entity/{BOOT}.geo.json", f"geckolib/animations/entity/{BOOT}.animation.json"):
        if not os.path.exists(f"{h.A}/{pad}"):
            problems.append(f"missing {pad}")
    if not os.path.exists(os.path.join(h.TEX, "entity", f"{BOOT}.png")):
        problems.append(f"missing texture {BOOT}")
    for ras in pups():
        pad = f"{h.A}/geckolib/animations/entity/snuffelhond_{ras}_pup.animation.json"
        if "lig" not in json.load(open(pad, encoding="utf-8"))["animations"]:
            problems.append(f"the {ras} puppy cannot lie down")
    return problems
