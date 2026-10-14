"""
einde2, slice cast_schurk - the nine creatures of the Guhvatar story (CONTRACT_E2 11.6): entity types of their own on the
Java class nl.juiced.guhs.feature.einde2.castschurk.CastWezen (table: Wezens), with python-written GeckoLib models.

  draakguh            Ran (red, draakguh.png) and Shaw (blue, draakguh_1.png): a serpentine dragon in a coil, the guh's
                      face and ears; `vlieg_rond` flies the whole coil in a ring of radius 9 around its spot (bone `vlucht`)
  bosco               beer Bosco: an ordinary brown bear, guh ears and the guh's pink nose, a little green hat and a vest
  wan_shi_guh         the huge owl of the library: black, a white heart face that is the guh face, the guh ears as tufts
  vosguh              a small fox with the guh face and ears; `vosguh_rol` (the scroll in its mouth) shows with Draagt
  leeuwschildpad_guh  the MOVING parts of the Leeuwschildpad-guh laid over the block head (lids, brows, lip and beard, ear
                      tips, the claws of the raised paw): look 0 on the island's head, look 1 on the overlay's head
  flopsie             the goat-gorilla: shaggy white on its knuckles, goat horns, floppy guh ears, a silly tongue
  luchtbizon_guh      Appa-guh's model without saddle, its animations without jipjip, five re-tinted textures (looks 0-4)
  maanvis, oceaanvis  Tui and La: koi with veil fins (one model written twice, two textures)

WEZENS: id -> (hitbox width, height, looks, [(animation besides idle, ticks; 0 = a pose that lasts)]) - the same table as
the Java enum Wezens (java_rij gives a row's arguments; einde2_cast_schurk_modellen.check compares them).
Every creature's file holds idle and praat and every name of its row (plain names, as the Sponsguh cast); the lion turtle
also soort_0 / soort_1 (the poses that show one head's parts).
"""
import json
import math
import os

import numpy as np
from PIL import Image

from features import boerderij_dieren as bd
from features import einde2_kit as kit

S = bd.S
plain, guh_face, cube, bone, ears = bd.plain, bd.guh_face, bd.cube, bd.bone, bd.ears
WEG = [0.001, 0.001, 0.001]

WEZENS = {
    "draakguh": (2.4, 2.4, 2, [("praat", 24), ("vlieg_rond", 0), ("vuur", 40), ("buig", 40), ("slaap", 0)]),
    "bosco": (1.3, 1.4, 0, [("praat", 24), ("loop", 0), ("zit", 0), ("eet", 60), ("brom", 30)]),
    "wan_shi_guh": (2.2, 3.6, 0, [("praat", 24), ("draai_kop", 40), ("spreid", 0), ("buig", 40)]),
    "vosguh": (0.6, 0.7, 0, [("praat", 24), ("loop", 0), ("snuffel", 40), ("breng", 30)]),
    "leeuwschildpad_guh": (1.0, 1.0, 2, [("praat", 0), ("ontwaak", 60), ("zegen", 50)]),
    "flopsie": (1.6, 2.4, 0, [("praat", 24), ("loop", 0), ("knuffel", 40), ("spring", 30), ("lik", 30)]),
    "luchtbizon_guh": (2.9, 2.5, 5, [("praat", 24), ("walk", 0), ("vlieg", 0), ("brul", 40)]),
    "maanvis": (0.5, 0.3, 0, [("praat", 24), ("zwem", 0), ("gloei", 40), ("gevangen", 0)]),
    "oceaanvis": (0.5, 0.3, 0, [("praat", 24), ("zwem", 0), ("gloei", 40)]),
}
# textures of a creature's looks: <id>.png for look 0, <id>_<n>.png for the others (the lion turtle's looks are poses)
TEXTUUR_PER_SOORT = ("draakguh", "luchtbizon_guh")
BIZON_SCHAAL = (1.0, 0.9, 1.1, 0.8, 0.55)      # (the vanilla scale attribute per look; Java: CastWezen.BIZON_SCHAAL)


def java_rij(id):
    b, h, soorten, anims = WEZENS[id]
    return f"{b}f, {h}f, {soorten}, " + ", ".join(f'"{n}:{t}"' for n, t in anims)


# =====================================================================================================================
# the atlas (boerderij_dieren's, with a texel density and the slice's own named seed)
# =====================================================================================================================
class Vel(bd.Atlas):
    def __init__(self, naam, k=1.0):
        super().__init__(0)
        self.rng = kit.rng(f"cast_schurk/{naam}")
        self.k = k

    def alloc(self, w, h):
        try:
            return super().alloc(w, h)
        except SystemExit:
            raise SystemExit("einde2_cast_schurk_wezens: the texture atlas is full (lower this model's k)")

    def patch(self, w, h, painter):
        return super().patch(max(w * self.k, 0.25), max(h * self.k, 0.25), painter)


def geo(identifier, bones, width, height, offset=None):
    g = bd.geo(identifier, bones, width, height)
    if offset is not None:
        g["minecraft:geometry"][0]["description"]["visible_bounds_offset"] = offset
    return g


def disc(a, cx, cy, r, colour, alpha=1.0, ry=None):
    H, W = a.shape[:2]
    ry = ry or r
    y0, y1 = max(0, int(cy - ry - 1)), min(H, int(cy + ry + 2))
    x0, x1 = max(0, int(cx - r - 1)), min(W, int(cx + r + 2))
    if y0 >= y1 or x0 >= x1:
        return
    yy, xx = np.mgrid[y0:y1, x0:x1]
    m = ((xx + 0.5 - cx) / r) ** 2 + ((yy + 0.5 - cy) / ry) ** 2 <= 1
    blok = a[y0:y1, x0:x1]
    blok[m, :3] = blok[m, :3] * (1 - alpha) + np.array(colour, np.float32)[:3] * alpha
    blok[m, 3] = 255


def met(painter, extra):
    def paint(W, H, rng):
        a = painter(W, H, rng).astype(np.float32)
        extra(W, H, rng, a)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def vacht(colour, licht, n=24):
    """Shaggy fur: soft hanging locks."""
    def extra(W, H, rng, a):
        for _ in range(max(2, W * H // n)):
            x, y, lang = int(rng.uniform(0, W)), int(rng.uniform(0, H)), int(rng.uniform(3, 9))
            a[y:y + lang, x:x + 1, :3] = licht if rng.random() < 0.5 else np.array(colour, np.float32) * 0.86
    return plain(colour, 7, extra)


def schubben(base, licht, rij=6):
    def extra(W, H, rng, a):
        for j, y in enumerate(range(0, H + rij, rij)):
            for x in range((rij // 2) * (j % 2) - rij, W + rij, rij):
                disc(a, x + rij / 2, y, rij * 0.55, np.array(base) * 0.8, 1.0)
                disc(a, x + rij / 2, y - 1.0, rij * 0.5, licht if (x + j) % 3 == 0 else base, 1.0)
    return plain(base, 6, extra)


def alpha(painter, a_):
    def paint(W, H, rng):
        arr = painter(W, H, rng).copy()
        arr[..., 3] = (arr[..., 3].astype(np.float32) * a_ / 255).astype(np.uint8)
        return arr
    return paint


def kraal_ogen(fur, eye_y=0.4, eye_dx=0.24, r=0.08, neus=None, mond=True):
    """A bear's face: two small black bead eyes with a glint, and a soft smile (the nose is a cube of its own)."""
    def paint(W, H, rng):
        a = plain(fur, 8)(W, H, rng).astype(np.float32)
        for sx in (-1, 1):
            cx, cy = W / 2 + sx * eye_dx * W, H * eye_y
            disc(a, cx, cy, r * W, (20, 16, 18))
            disc(a, cx + 0.3 * r * W, cy - 0.3 * r * W, 0.3 * r * W, (250, 250, 250))
        if neus:
            disc(a, W / 2, H * 0.72, W * 0.1, neus, ry=H * 0.07)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


# =====================================================================================================================
# draakguh: Ran and Shaw
# =====================================================================================================================
DRAAK = {0: dict(huid=(196, 44, 40), licht=(236, 96, 66), buik=(250, 206, 96), stekel=(250, 222, 120), manen=(250, 180, 70)),
         1: dict(huid=(44, 86, 196), licht=(96, 150, 236), buik=(226, 236, 246), stekel=(220, 236, 255), manen=(150, 210, 250))}
DRAAK_N = 14                    # segments; each 16 px long: about 14 blocks of body


def _draak_pad():
    """The coil: the centres of the segments along a rising spiral (tail outside, low; the neck climbs to the middle)."""
    pts = []
    for i in range(4000):
        t = i / 3999
        th = 0.35 + t * 1.62 * math.pi
        r = 60 - 26 * t
        pts.append((math.cos(th) * r, 9 + 38 * t, math.sin(th) * r))
    uit, acc, vorige = [pts[0]], 0.0, pts[0]
    for p in pts[1:]:
        acc += math.dist(p, vorige)
        vorige = p
        if acc >= 15.0:
            uit.append(p)
            acc = 0.0
    return uit[:DRAAK_N]


def draakguh(a, soort=0):
    c = DRAAK[soort]
    huid, buik = schubben(c["huid"], c["licht"]), plain(c["buik"], 6)
    stekel, manen = plain(c["stekel"], 5), vacht(c["manen"], (255, 240, 200), 16)
    bones = [bone("root", None, [0, 0, 0], []), bone("vlucht", "root", [0, 0, 0], []), bone("body", "vlucht", [0, 0, 0], [])]
    pad = _draak_pad()
    for i, (x, y, z) in enumerate(pad):
        nxt = pad[min(i + 1, len(pad) - 1)]
        prv = pad[max(i - 1, 0)]
        dx, dz = nxt[0] - prv[0], nxt[2] - prv[2]
        yaw = math.degrees(math.atan2(dz, dx))
        dik = 10 + 12 * (i / (DRAAK_N - 1)) ** 0.6
        cubes = [cube(a, [x - 9, y - dik / 2, z - dik / 2], [18, dik, dik], huid, overrides={"down": buik}),
                 cube(a, [x - 2, y + dik / 2, z - 1], [4, 3 + dik * 0.15, 2], stekel)]
        if i in (4, 9):                                              # four short legs
            for sz in (-1, 1):
                cubes.append(cube(a, [x - 2.5, y - dik / 2 - 7, z + sz * (dik / 2 - 1) - 2.5], [5, 8, 5], huid))
                cubes.append(cube(a, [x - 3, y - dik / 2 - 8, z + sz * (dik / 2 - 1) - 3], [6, 2, 6], stekel))
        if i == 0:                                                   # the tail tuft
            cubes.append(cube(a, [x - 16, y - 5, z - 5], [8, 10, 10], manen))
        bones.append(bone(f"seg_{i}", "body", [x, y, z], cubes, rotation=[0, -yaw, 0]))
    hx, hy, hz = pad[-1]
    hy += 14
    bones.append(bone("hals", "body", [hx, hy - 14, hz], [cube(a, [hx - 8, hy - 16, hz - 8], [16, 16, 16], huid, overrides={"north": buik})]))
    face = guh_face(c["licht"], eye_y=0.42, eye_dx=0.24, eye_r=0.15)
    bones.append(bone("head", "hals", [hx, hy, hz], [
        cube(a, [hx - 11, hy - 2, hz - 10], [22, 18, 18], huid, overrides={"north": face}),
        cube(a, [hx - 6, hy - 1, hz - 17], [12, 8, 7], huid, overrides={"north": met(plain(c["licht"], 6), lambda W, H, rng, ar: (
            disc(ar, W * 0.32, H * 0.35, W * 0.07, (60, 20, 30)), disc(ar, W * 0.68, H * 0.35, W * 0.07, (60, 20, 30))))}),
        cube(a, [hx - 2, hy + 4, hz - 17.8], [4, 2.5, 1], plain((236, 128, 168), 4)),     # the guh's pink nose
        cube(a, [hx - 13, hy + 12, hz - 2], [26, 6, 14], manen)]))                       # the mane behind the head
    for b in ears(a, c["licht"], (236, 128, 168), 6, hy + 15, hz - 4, 6, 6):     # (made round x = 0: moved onto the head)
        b["pivot"][0] += hx
        for c_ in b["cubes"]:
            c_["origin"][0] += hx
        bones.append(b)
    for side, sx in (("left", 1), ("right", -1)):                  # the long whiskers
        ox = hx + 6 if sx > 0 else hx - 6 - 24
        bones.append(bone(f"snor_{side}", "head", [hx + sx * 6, hy + 2, hz - 15],
                          [cube(a, [ox, hy + 1.5, hz - 15.5], [24, 1, 1], stekel)], rotation=[0, 0, -20 * sx]))
    bones.append(bone("draak_vuur", "head", [hx, hy + 2, hz - 17], [
        cube(a, [hx - 4, hy - 2, hz - 41], [8, 8, 24], plain((255, 170, 60), 30))]))   # (the breath: shown only in vuur)
    return geo("draakguh", bones, 20, 8, [0, 3, 0])


def draak_animaties():
    L = 10.0
    idle = {"body": {"position": kf([(0, [0, 0, 0]), (L / 2, [0, 1.2, 0]), (L, [0, 0, 0])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (L * 0.3, [-6, 10, 0]), (L * 0.6, [4, -8, 0]), (L, [0, 0, 0])])},
            "snor_left": {"rotation": golf(2, 8, L / 2, om=(0, 0, -20))}, "snor_right": {"rotation": golf(2, -8, L / 2, om=(0, 0, 20))},
            "draak_vuur": {"scale": WEG}}
    for i in range(DRAAK_N):
        idle[f"seg_{i}"] = {"position": golf(1, 1.6, L, fase=i * 0.5)}
    praat = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.2, [-10, 0, 0]), (0.5, [6, 0, 0]), (0.8, [-8, 0, 0]), (1.2, [0, 0, 0])])},
             "draak_vuur": {"scale": WEG}}
    V = 6.0
    vlieg = {"vlucht": {"rotation": kf([(V * i / 8, [0, -45 * i, 0]) for i in range(9)])},
             "body": {"position": kf([(V * i / 8, [144, 18 + 22 * math.sin(i / 8 * 2 * math.tau), 0]) for i in range(9)])},
             "draak_vuur": {"scale": WEG}}
    for i in range(DRAAK_N):
        vlieg[f"seg_{i}"] = {"position": golf(1, 7, V / 2, n=6, fase=i * 0.7)}
    vuur = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.3, [-18, 0, 0]), (0.5, [8, 0, 0]), (1.6, [8, 0, 0]), (2.0, [0, 0, 0])])},
            "draak_vuur": {"scale": kf([(0, WEG), (0.45, WEG), (0.55, [1, 1, 1]), (1.6, [1.2, 1.2, 1.3]), (1.75, WEG), (2.0, WEG)])}}
    buig = {"hals": {"rotation": kf([(0, [0, 0, 0]), (0.6, [30, 0, 0]), (1.4, [30, 0, 0]), (2.0, [0, 0, 0])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (0.6, [25, 0, 0]), (1.4, [25, 0, 0]), (2.0, [0, 0, 0])])}, "draak_vuur": {"scale": WEG}}
    slaap = {"hals": {"rotation": [40, 0, 0]}, "head": {"rotation": kf([(0, [30, 0, 0]), (2, [32, 0, 0]), (4, [30, 0, 0])])},
             "body": {"position": kf([(0, [0, 0, 0]), (2, [0, 0.8, 0]), (4, [0, 0, 0])])}, "draak_vuur": {"scale": WEG}}
    return {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "vlieg_rond": anim(V, vlieg), "vuur": anim(2.0, vuur, False),
            "buig": anim(2.0, buig, False), "slaap": anim(4.0, slaap)}


# =====================================================================================================================
# bosco: "gewoon... een beer?"
# =====================================================================================================================
BEER, BEER_LICHT, BEER_DONKER = (122, 82, 50), (160, 116, 76), (90, 58, 36)


def bosco(a):
    fur = vacht(BEER, BEER_LICHT, 30)
    gezicht = kraal_ogen(BEER_LICHT, eye_y=0.38, eye_dx=0.26, r=0.07)
    snuit = plain((196, 156, 112), 6)
    hoed, rand = plain((50, 130, 70), 6), plain((30, 92, 50), 5)
    vest = met(plain((150, 40, 44), 6), lambda W, H, rng, ar: ar.__setitem__((slice(None), slice(0, max(1, W // 10)), slice(0, 3)), (232, 190, 80)))
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 12, 0], [cube(a, [-10, 10, -14], [20, 16, 28], fur),
                                                cube(a, [-10.5, 13, -14.6], [21, 13.5, 13], vest, overrides={"south": vest})]),
             bone("head", "body", [0, 21, -14], [
                 cube(a, [-7, 15, -26], [14, 12, 12], fur, overrides={"north": gezicht}),
                 cube(a, [-4, 15.5, -30], [8, 6, 4], snuit, overrides={"north": met(snuit, lambda W, H, rng, ar: disc(ar, W / 2, H * 0.62, W * 0.16, BEER_DONKER, ry=H * 0.05))}),
                 cube(a, [-1.6, 19.3, -30.6], [3.2, 2.2, 1], plain((236, 128, 168), 4)),          # the guh's pink nose
                 cube(a, [-4, 27, -24], [8, 1.2, 8], rand), cube(a, [-3, 28, -23], [6, 3.5, 6], hoed)]),
             bone("bosco_kwast", "head", [0, 31.5, -20], [cube(a, [-0.5, 31.5, -20.5], [1, 1, 1], rand),
                                                           cube(a, [-0.6, 28.6, -17.8], [1.2, 3, 1.2], plain((250, 210, 90), 5))]),
             bone("bosco_honing", "head", [0, 16, -30], [cube(a, [-3, 13, -34], [6, 5, 4], met(plain((246, 182, 46), 6), lambda W, H, rng, ar: [
                 disc(ar, x, y, 1.6, (200, 130, 20)) for x in range(2, W, 5) for y in range(2, H, 5)]))]),
             bone("tail", "body", [0, 22, 14], [cube(a, [-2, 20, 13.5], [4, 4, 3], fur)])]
    bones += ears(a, BEER, (236, 128, 168), 4.2, 26, -21, 4, 4)
    for side, sx in (("left", 1), ("right", -1)):
        for voor, z in (("front", -12), ("back", 5)):
            x = 3 if sx > 0 else -10
            bones.append(bone(f"leg_{voor}_{side}", "body", [x + 3.5, 12, z + 3.5], [cube(a, [x, 0, z], [7, 12, 7], fur, overrides={"down": plain(BEER_DONKER, 5)})]))
    return geo("bosco", bones, 2, 2.2)


def bosco_animaties():
    L = 9.0
    idle = {"body": {"scale": kf([(0, [1, 1, 1]), (L / 2, [1.02, 1.03, 1.02]), (L, [1, 1, 1])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (L * 0.4, [0, 14, 0]), (L * 0.6, [6, -10, 0]), (L, [0, 0, 0])])},
            "bosco_kwast": {"rotation": golf(2, 10, L / 3)}, "bosco_honing": {"scale": WEG}}
    idle.update(oor_idle(L))
    praat = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.2, [-8, 0, 4]), (0.5, [6, 0, -4]), (0.8, [-6, 0, 2]), (1.2, [0, 0, 0])])},
             "bosco_honing": {"scale": WEG}}
    loop = {"body": {"position": kf([(0, [0, 0, 0]), (0.25, [0, 0.6, 0]), (0.5, [0, 0, 0]), (0.75, [0, 0.6, 0]), (1.0, [0, 0, 0])])},
            "bosco_honing": {"scale": WEG}}
    for side, sx in (("left", 1), ("right", -1)):
        loop[f"leg_front_{side}"] = {"rotation": golf(0, 24 * sx, 1.0)}
        loop[f"leg_back_{side}"] = {"rotation": golf(0, -24 * sx, 1.0)}
    zit = {"body": {"rotation": [-28, 0, 0], "position": [0, -3, 2]}, "head": {"rotation": [24, 0, 0]},
           "leg_front_left": {"rotation": [28, 0, 0]}, "leg_front_right": {"rotation": [28, 0, 0]},
           "leg_back_left": {"rotation": [-60, 0, 0]}, "leg_back_right": {"rotation": [-60, 0, 0]}, "bosco_honing": {"scale": WEG}}
    eet = {"bosco_honing": {"scale": kf([(0, WEG), (0.2, [1, 1, 1]), (2.6, [1, 1, 1]), (2.8, WEG), (3.0, WEG)])},
           "head": {"rotation": kf([(0, [0, 0, 0]), (0.3, [-14, 0, 0]), (0.6, [6, 0, 0]), (0.9, [-14, 0, 0]), (1.2, [6, 0, 0]), (1.5, [-14, 0, 0]),
                                    (1.8, [6, 0, 0]), (2.6, [0, 0, 0]), (3.0, [0, 0, 0])])}}
    brom = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.3, [-22, 0, 0]), (1.1, [-22, 0, 0]), (1.5, [0, 0, 0])])},
            "body": {"scale": kf([(0, [1, 1, 1]), (0.3, [1.06, 1.08, 1.04]), (1.1, [1.06, 1.08, 1.04]), (1.5, [1, 1, 1])])}, "bosco_honing": {"scale": WEG}}
    return {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "loop": anim(1.0, loop), "zit": anim(2.0, zit), "eet": anim(3.0, eet, False),
            "brom": anim(1.5, brom, False)}


# =====================================================================================================================
# wan_shi_guh: the owl spirit
# =====================================================================================================================
UIL, UIL_LICHT = (36, 32, 40), (70, 64, 78)


def uil_gezicht(W, H, rng):
    """The white heart-shaped face on black feathers, with the guh's eyes, blush and snoet in it."""
    a = vacht(UIL, UIL_LICHT, 30)(W, H, rng).astype(np.float32)
    wit = guh_face((246, 244, 240), eye_y=0.44, eye_dx=0.22, eye_r=0.15)(W, H, rng).astype(np.float32)
    yy, xx = np.mgrid[0:H, 0:W]
    u, v = (xx + 0.5) / W * 2 - 1, (yy + 0.5) / H * 2 - 1
    hart = (u ** 2 + (-(v + 0.1) - np.abs(u) ** 0.6 * 0.55) ** 2 <= 0.9) & (v < 0.95)
    a[hart] = wit[hart]
    rand = hart & ~((u * 1.08) ** 2 + (-(v + 0.1) * 1.06 - np.abs(u) ** 0.6 * 0.55) ** 2 <= 0.9)
    a[rand, :3] = (210, 196, 180)
    return np.clip(a, 0, 255).astype(np.uint8)


def wan_shi_guh(a):
    veren = vacht(UIL, UIL_LICHT, 20)
    buik = met(plain((60, 54, 64), 6), lambda W, H, rng, ar: [disc(ar, x + (y // 5 % 2) * 2.5, y, 1.2, (110, 100, 116))
                                                               for y in range(3, H, 5) for x in range(2, W, 5)])
    poot = plain((200, 170, 90), 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], [cube(a, [-14, 6, -10], [28, 26, 20], veren, overrides={"north": buik}),
                                               cube(a, [-12, 4, -8], [24, 3, 16], veren),
                                               cube(a, [-6, 6, 9], [12, 8, 8], veren)]),                    # the tail
             bone("hals", "body", [0, 32, -1], [cube(a, [-6, 31, -6], [12, 10, 10], veren)]),
             bone("head", "hals", [0, 40, -1], [cube(a, [-12, 40, -11], [24, 18, 18], veren, overrides={"north": uil_gezicht}),
                                                 cube(a, [-1.5, 46, -12.2], [3, 3.5, 1.4], plain((150, 140, 130), 5))])]
    bones += ears(a, UIL, (236, 128, 168), 6.5, 57, -4, 6, 6)
    for side, sx in (("left", 1), ("right", -1)):
        x = 13 if sx > 0 else -17
        bones.append(bone(f"wing_{side}", "body", [sx * 14, 31, 0], [
            cube(a, [x, 9, -9], [4, 23, 19], veren), cube(a, [x + (0.5 if sx > 0 else 0), 4, -6], [3.5, 6, 14], veren)]))
        bones.append(bone(f"leg_{side}", "body", [sx * 6, 6, 0], [cube(a, [sx * 6 - 2.5, 0, -4], [5, 6, 5], poot),
                                                                   cube(a, [sx * 6 - 3.5, 0, -7], [7, 1.5, 4], plain((60, 50, 40), 5))]))
    return geo("wan_shi_guh", bones, 4, 4.5)


def uil_animaties():
    L = 12.0
    idle = {"body": {"scale": kf([(0, [1, 1, 1]), (L / 2, [1.02, 1.03, 1.02]), (L, [1, 1, 1])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (L * 0.25, [0, 40, 0]), (L * 0.35, [0, 40, 0]), (L * 0.5, [0, -40, 0]), (L * 0.6, [0, -40, 0]),
                                     (L * 0.75, [0, 0, 0]), (L, [0, 0, 0])])}}
    idle.update(oor_idle(L, 18))
    praat = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.2, [-6, 0, 10]), (0.6, [4, 0, -10]), (1.2, [0, 0, 0])])}}
    draai = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.5, [0, 90, 0]), (1.0, [0, 180, 0]), (1.5, [0, 270, 0]), (2.0, [0, 360, 0])])}}
    spreid = {"root": {"scale": [1.15, 1.15, 1.15]}, "wing_left": {"rotation": kf([(0, [0, 0, -85]), (1.0, [0, 0, -78]), (2.0, [0, 0, -85])])},
              "wing_right": {"rotation": kf([(0, [0, 0, 85]), (1.0, [0, 0, 78]), (2.0, [0, 0, 85])])}, "head": {"rotation": [-10, 0, 0]}}
    buig = {"body": {"rotation": kf([(0, [0, 0, 0]), (0.6, [18, 0, 0]), (1.4, [18, 0, 0]), (2.0, [0, 0, 0])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (0.6, [22, 0, 0]), (1.4, [22, 0, 0]), (2.0, [0, 0, 0])])},
            "wing_left": {"rotation": kf([(0, [0, 0, 0]), (0.6, [0, 0, -20]), (1.4, [0, 0, -20]), (2.0, [0, 0, 0])])},
            "wing_right": {"rotation": kf([(0, [0, 0, 0]), (0.6, [0, 0, 20]), (1.4, [0, 0, 20]), (2.0, [0, 0, 0])])}}
    return {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "draai_kop": anim(2.0, draai, False), "spreid": anim(2.0, spreid),
            "buig": anim(2.0, buig, False)}


# =====================================================================================================================
# vosguh: the knowledge seekers
# =====================================================================================================================
VOS, VOS_WIT = (206, 104, 44), (248, 240, 230)


def vosguh(a):
    fur = plain(VOS, 7)
    wit = plain(VOS_WIT, 5)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], [cube(a, [-3, 4, -6], [6, 5, 11], fur, overrides={"down": wit})]),
             bone("head", "body", [0, 8, -6], [cube(a, [-3.5, 6, -11], [7, 6, 5], fur, overrides={"north": guh_face(VOS, eye_y=0.38, eye_dx=0.24, eye_r=0.16)}),
                                                cube(a, [-1.5, 6, -13], [3, 2.5, 2], wit), cube(a, [-0.6, 7.6, -13.4], [1.2, 1, 0.6], plain((40, 30, 34), 3))]),
             bone("vosguh_rol", "head", [0, 7, -12], [cube(a, [-4.5, 6.6, -12.6], [9, 1.6, 1.6], plain((240, 228, 196), 4)),
                                                       cube(a, [-0.6, 6.4, -12.8], [1.2, 2, 2], plain((200, 50, 50), 4))]),
             bone("tail", "body", [0, 8, 5], [cube(a, [-2, 6, 5], [4, 4, 7], fur), cube(a, [-1.6, 6.4, 12], [3.2, 3.2, 2.5], wit)], rotation=[-25, 0, 0])]
    bones += ears(a, VOS, (236, 128, 168), 1.4, 11.5, -9, 3, 3)
    for side, sx in (("left", 1), ("right", -1)):
        for voor, z in (("front", -5), ("back", 2)):
            x = 1 if sx > 0 else -2.6
            bones.append(bone(f"leg_{voor}_{side}", "body", [x + 0.8, 4, z + 0.8], [cube(a, [x, 0, z], [1.6, 4, 1.6], fur, overrides={"down": plain((50, 34, 30), 4)})]))
    return geo("vosguh", bones, 1, 1)


def vos_animaties():
    L = 8.0
    idle = {"tail": {"rotation": golf(1, 14, L / 2, om=(-25, 0, 0))},
            "head": {"rotation": kf([(0, [0, 0, 0]), (L * 0.3, [0, 18, 6]), (L * 0.45, [0, 18, 6]), (L * 0.6, [0, 0, 0]), (L, [0, 0, 0])])}}
    idle.update(oor_idle(L))
    praat = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.2, [-10, 0, 8]), (0.6, [6, 0, -8]), (1.2, [0, 0, 0])])}}
    loop = {"body": {"position": kf([(0, [0, 0, 0]), (0.15, [0, 0.5, 0]), (0.3, [0, 0, 0]), (0.45, [0, 0.5, 0]), (0.6, [0, 0, 0])])},
            "tail": {"rotation": golf(1, 10, 0.6, om=(-10, 0, 0))}}
    for side, sx in (("left", 1), ("right", -1)):
        loop[f"leg_front_{side}"] = {"rotation": golf(0, 35 * sx, 0.6)}
        loop[f"leg_back_{side}"] = {"rotation": golf(0, -35 * sx, 0.6)}
    snuffel = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.3, [30, 0, 0])] + [(0.3 + 0.15 * k, [30 + (4 if k % 2 else 0), (k % 5 - 2) * 8, 0]) for k in range(1, 12)]
                                       + [(2.0, [0, 0, 0])])},
               "tail": {"rotation": golf(1, 25, 0.5, om=(-35, 0, 0))}}
    breng = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.4, [40, 0, 0]), (1.0, [40, 0, 0]), (1.5, [0, 0, 0])])},
             "body": {"rotation": kf([(0, [0, 0, 0]), (0.4, [8, 0, 0]), (1.0, [8, 0, 0]), (1.5, [0, 0, 0])])}}
    return {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "loop": anim(0.6, loop), "snuffel": anim(2.0, snuffel, False),
            "breng": anim(1.5, breng, False)}


# =====================================================================================================================
# leeuwschildpad_guh: the moving parts over the two block heads
# =====================================================================================================================
# Look 0, the head of leeuwschildpad_eiland. Measured (read-only) in tools/features/einde2_bouw_zij2_schildpad.py, kop(b):
# the guh model's head + ears voxelised at KOP_SCHAAL 1.7 with its low corner at template (CX - 17, R0 - 8, 113) = (39, 22, 113),
# facing south (naar_zuid); the voxel box is 22 deep (make_structures.voxel_guh of the bones head/ear_left/ear_right) and
# the guh model's low corner of those bones is (-11.5, 1, -12.75). The marker `kop` (the entity's spot) is the nose,
# naar(0, 6.2, -12.4) = block (58, 30, 134); the eyes are the GUH_OOG voxels around guh (+-3.8, 9.5, -11.9) (radius about 2.2
# x 2.4 guh pixels, measured by voxelising), the ears' tops at guh y 16-17; the raised paw: PALM = (57, 141) with its top
# at R0 = 30 and four claws of bone blocks at 55, 85, 115 and 145 degrees, 6 to 9.5 blocks out (arm(b)).
KOP0_SCHAAL, KOP0_HOEK, KOP0_DIEP, KOP0_LO = 1.7, (39, 22, 113), 22, (-11.5, 1, -12.75)
KOP0_NEUS = (58, 30, 134)
KOP0_PALM, KOP0_KLAUWEN = (57, 30, 141), (55, 85, 115, 145)
# Look 1, the head of the overlay laag_leeuwschildpad: tools/features/einde2_bouw_vuur_buiten.py, leeuwschildpad(b), in the
# overlay's own coordinates: cx = 22, the water line wy = 14, the face plane hz1 = 56; the nose is the magenta block
# (22, wy + 2, 56), the eyes the black blocks (22 -+ 3, wy + 3 .. wy + 4, 56), the mouth the row (20 .. 24, wy - 1 .. wy, 56), the
# ears the 3 x 3 blocks at (22 -+ 5 (+-1), wy + 8 .. wy + 10, 48). It has no raised paw (its flippers lie in the water).
KOP1_NEUS = (22, 16, 56)
KOP1_OGEN, KOP1_OREN = ((19, 17, 56), (25, 17, 56)), ((17, 22, 48), (27, 22, 48))
SCHILDPAD_GROEN, SCHILDPAD_LICHT = (104, 140, 64), (150, 182, 86)


def _w0(px, py, pz):
    """A point of the guh model on the island's head -> the entity model (bedrock px, the entity at the nose block's middle,
    facing south: world +x = model +x, world +z (south, his front) = model -z)."""
    sx = KOP0_HOEK[0] + (px - KOP0_LO[0]) * KOP0_SCHAAL
    sy = KOP0_HOEK[1] + (py - KOP0_LO[1]) * KOP0_SCHAAL
    sz = KOP0_HOEK[2] + KOP0_DIEP - (pz - KOP0_LO[2]) * KOP0_SCHAAL
    return _blok(sx, sy, sz, KOP0_NEUS)


def _blok(x, y, z, neus):
    """A world point (in blocks, template coordinates) -> the entity model, relative to the middle bottom of the nose block."""
    return [round((x - neus[0] - 0.5) * 16, 2), round((y - neus[1]) * 16, 2), round(-(z - neus[2] - 0.5) * 16, 2)]


def _doos(lo, hi):
    """origin + size of the box between two model points."""
    o = [min(lo[i], hi[i]) for i in range(3)]
    return o, [round(abs(hi[i] - lo[i]), 2) for i in range(3)]


def leeuwschildpad_guh(a):
    lid, brauw = plain(SCHILDPAD_GROEN, 6), vacht((214, 120, 40), (250, 190, 80), 20)
    lip, baard = plain((236, 140, 160), 6), vacht((248, 246, 240), (214, 210, 204), 12)
    oor, klauw = plain((236, 128, 168), 6), plain((236, 226, 196), 6)
    bones = [bone("root", None, [0, 0, 0], []), bone("body", "root", [0, 0, 0], []), bone("head", "body", [0, 0, 0], []),
             bone("kop_0", "head", [0, 0, 0], []), bone("kop_1", "head", [0, 0, 0], [])]
    # look 0: the island's head, in guh model pixels (x 1.7 blocks)
    for side, sx in (("left", 1), ("right", -1)):
        lo, hi = _w0(sx * 1.4, 7.0, -12.15), _w0(sx * 6.2, 12.2, -12.45)
        o, s = _doos(lo, hi)
        bones.append(bone(f"lid0_{side}", "kop_0", [o[0] + s[0] / 2, o[1] + s[1], o[2] + s[2] / 2], [cube(a, o, s, lid)]))
        o, s = _doos(_w0(sx * 1.2, 12.3, -12.2), _w0(sx * 6.4, 13.2, -12.6))
        bones.append(bone(f"brauw0_{side}", "kop_0", [o[0] + s[0] / 2, o[1], o[2]], [cube(a, o, s, brauw)]))
        o, s = _doos(_w0(sx * 5.2, 15.4, -6.6), _w0(sx * 10.8, 17.3, -4.6))
        bones.append(bone(f"oor0_{side}", "kop_0", [o[0] + s[0] / 2, o[1], o[2] + s[2] / 2], [cube(a, o, s, oor)]))
    o, s = _doos(_w0(-3.0, 3.6, -12.3), _w0(3.0, 4.6, -12.7))
    bones.append(bone("lip0", "kop_0", [0, o[1] + s[1], o[2]], [cube(a, o, s, lip)]))
    o, s = _doos(_w0(-2.4, 0.2, -12.2), _w0(2.4, 3.7, -12.9))
    bones.append(bone("baard0", "lip0", [0, o[1] + s[1], o[2]], [cube(a, o, s, baard)]))
    px, py, pz = KOP0_PALM
    for i, graden in enumerate(KOP0_KLAUWEN):
        h = math.radians(graden)
        mx, mz = px + 0.5 + math.cos(h) * 7.8, pz + 0.5 + math.sin(h) * 7.8
        c = _blok(mx, py + 0.05, mz, KOP0_NEUS)
        bones.append(bone(f"klauw0_{i + 1}", "kop_0", c, [cube(a, [c[0] - 9, c[1], c[2] - 9], [18, 16, 18], klauw)]))
    # look 1: the overlay's head, in blocks
    for (side, sx), (ex, ey, ez) in zip((("left", 1), ("right", -1)), sorted(KOP1_OGEN, key=lambda e: -e[0])):
        o, s = _doos(_blok(ex - 0.6, ey, ez + 1.05, KOP1_NEUS), _blok(ex + 1.6, ey + 2.1, ez + 1.25, KOP1_NEUS))
        bones.append(bone(f"lid1_{side}", "kop_1", [o[0] + s[0] / 2, o[1] + s[1], o[2] + s[2] / 2], [cube(a, o, s, lid)]))
        o, s = _doos(_blok(ex - 0.8, ey + 2.25, ez + 1.05, KOP1_NEUS), _blok(ex + 1.8, ey + 2.75, ez + 1.35, KOP1_NEUS))
        bones.append(bone(f"brauw1_{side}", "kop_1", [o[0] + s[0] / 2, o[1], o[2]], [cube(a, o, s, brauw)]))
    for (side, sx), (ox_, oy_, oz_) in zip((("left", 1), ("right", -1)), sorted(KOP1_OREN, key=lambda e: -e[0])):
        o, s = _doos(_blok(ox_ - 0.2, oy_ + 2.0, oz_ - 0.2, KOP1_NEUS), _blok(ox_ + 3.2, oy_ + 3.3, oz_ + 1.2, KOP1_NEUS))
        bones.append(bone(f"oor1_{side}", "kop_1", [o[0] + s[0] / 2, o[1], o[2] + s[2] / 2], [cube(a, o, s, oor)]))
    n0, n1, n2 = KOP1_NEUS
    o, s = _doos(_blok(n0 - 2.5, n1 - 2.6, n2 + 1.05, KOP1_NEUS), _blok(n0 + 3.5, n1 - 2.0, n2 + 1.3, KOP1_NEUS))
    bones.append(bone("lip1", "kop_1", [0, o[1] + s[1], o[2]], [cube(a, o, s, lip)]))
    o, s = _doos(_blok(n0 - 1.8, n1 - 5.2, n2 + 1.05, KOP1_NEUS), _blok(n0 + 2.8, n1 - 2.6, n2 + 1.5, KOP1_NEUS))
    bones.append(bone("baard1", "lip1", [0, o[1] + s[1], o[2]], [cube(a, o, s, baard)]))
    return geo("leeuwschildpad_guh", bones, 34, 24, [0, 4, 0])


def schildpad_animaties():
    L = 9.0
    sets = ("0", "1")
    lids = [f"lid{k}_{s}" for k in sets for s in ("left", "right")]
    klauwen = [f"klauw0_{i}" for i in range(1, 5)]
    LID = 0.42                                                       # (heavy: the lids rest over the top of the eyes)
    idle = {n: {"scale": kf([(0, [1, LID, 1]), (L * 0.55, [1, LID, 1]), (L * 0.6, [1, 1, 1]), (L * 0.66, [1, 1, 1]), (L * 0.72, [1, LID, 1]), (L, [1, LID, 1])])}
            for n in lids}
    for k in sets:
        idle[f"baard{k}"] = {"rotation": golf(2, 3, L / 2)}
    praat = {n: {"scale": [1, LID, 1]} for n in lids}
    for k in sets:
        praat[f"lip{k}"] = {"rotation": kf([(0, [0, 0, 0]), (0.6, [-14, 0, 0]), (1.2, [0, 0, 0]), (1.9, [-10, 0, 0]), (2.6, [0, 0, 0]), (3.0, [0, 0, 0])])}
        praat[f"baard{k}"] = {"rotation": golf(2, 5, 1.5)}
    ontwaak = {n: {"scale": kf([(0, [1, 1, 1]), (1.0, [1, 1, 1]), (2.2, [1, 0.6, 1]), (2.4, [1, 0.8, 1]), (3.0, [1, LID, 1])])} for n in lids}
    for k in sets:
        for s, sx in (("left", 1), ("right", -1)):
            ontwaak[f"brauw{k}_{s}"] = {"position": kf([(0, [0, -6, 0]), (1.6, [0, -6, 0]), (2.6, [0, 4, 0]), (3.0, [0, 0, 0])])}
            ontwaak[f"oor{k}_{s}"] = {"rotation": kf([(0, [0, 0, 0]), (2.4, [0, 0, -12 * sx]), (3.0, [0, 0, 0])])}
    zegen = {n: {"scale": [1, LID, 1]} for n in lids}
    neus_palm = _blok(KOP0_PALM[0] + 0.5, KOP0_PALM[1] + 1.0, KOP0_PALM[2] + 0.5, KOP0_NEUS)
    for i, graden in enumerate(KOP0_KLAUWEN):
        h = math.radians(graden)
        tip = _blok(KOP0_PALM[0] + 0.5 + math.cos(h) * 7.8, KOP0_PALM[1], KOP0_PALM[2] + 0.5 + math.sin(h) * 7.8, KOP0_NEUS)
        naar = [round((neus_palm[j] - tip[j]) * 0.55 + (40 if j == 1 else 0), 2) for j in range(3)]
        zegen[f"klauw0_{i + 1}"] = {"position": kf([(0, [0, 0, 0]), (0.8, [n * 0.5 + (24 if j == 1 else 0) for j, n in enumerate(naar)]),
                                                     (1.3, naar), (1.9, naar), (2.5, [0, 0, 0])])}
    s0 = {"kop_1": {"scale": WEG}}
    s1 = {"kop_0": {"scale": WEG}}
    return {"idle": anim(L, idle), "praat": anim(3.0, praat), "ontwaak": anim(3.0, ontwaak, False), "zegen": anim(2.5, zegen, False),
            "soort_0": anim(1.0, s0), "soort_1": anim(1.0, s1)}


# =====================================================================================================================
# flopsie: the goat-gorilla
# =====================================================================================================================
WOL, WOL_LICHT = (240, 238, 232), (255, 255, 252)


def flopsie(a):
    fur = vacht(WOL, (206, 204, 198), 18)
    huid = plain((212, 196, 196), 6)
    hoorn = met(plain((176, 160, 130), 6), lambda W, H, rng, ar: [ar.__setitem__((slice(y, y + 1), slice(None), slice(0, 3)), (130, 116, 92)) for y in range(1, H, 4)])
    gezicht = guh_face((236, 226, 220), eye_y=0.36, eye_dx=0.25, eye_r=0.17)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 14, 2], [cube(a, [-12, 12, -10], [24, 24, 22], fur, overrides={"north": huid}),
                                                cube(a, [-10, 10, 4], [20, 10, 12], fur)]),
             bone("head", "body", [0, 34, -8], [cube(a, [-8, 31, -18], [16, 15, 12], fur, overrides={"north": gezicht}),
                                                 cube(a, [-1.5, 33.2, -18.6], [3, 1.4, 1], plain((236, 128, 168), 4))]),
             bone("flopsie_tong", "head", [0, 32.4, -18], [cube(a, [-1.6, 29.6, -18.8], [3.2, 3, 1], plain((240, 110, 140), 5))])]
    for side, sx in (("left", 1), ("right", -1)):
        x = 8 if sx > 0 else -11
        bones.append(bone(f"hoorn_{side}", "head", [sx * 8, 44, -12], [
            cube(a, [x, 43, -14], [3, 4, 4], hoorn), cube(a, [x + sx * 1.5, 45.5, -12], [3, 3, 6], hoorn),
            cube(a, [x + sx * 2.5, 42, -8], [3, 5, 3], hoorn), cube(a, [x + sx * 2.5, 39, -11], [3, 3, 3], hoorn)]))
        ox = 8 if sx > 0 else -10.5
        bones.append(bone(f"ear_{side}", "head", [sx * 9, 41, -11], [cube(a, [ox, 27, -12.5], [2.5, 14, 6], fur, overrides={
            "north": met(plain(WOL, 6), lambda W, H, rng, ar: disc(ar, W / 2, H * 0.55, W * 0.35, (236, 140, 170), ry=H * 0.38))})],
            rotation=[0, 0, 12 * sx]))
        ax = 12 if sx > 0 else -19
        bones.append(bone(f"arm_{side}", "body", [sx * 15.5, 34, -5], [cube(a, [ax, 2, -9], [7, 32, 8], fur),
                                                                        cube(a, [ax - 0.5, 0, -10], [8, 4, 9], huid)]))
        lx = 4 if sx > 0 else -11
        bones.append(bone(f"leg_{side}", "body", [sx * 7.5, 14, 10], [cube(a, [lx, 0, 6], [7, 14, 8], fur, overrides={"down": huid})]))
    return geo("flopsie", bones, 3, 3.4)


def flopsie_animaties():
    L = 10.0
    idle = {"body": {"scale": kf([(0, [1, 1, 1]), (L / 2, [1.02, 1.03, 1.02]), (L, [1, 1, 1])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (L * 0.3, [0, 0, 12]), (L * 0.5, [0, 0, -10]), (L, [0, 0, 0])])},
            "ear_left": {"rotation": golf(2, 8, L / 2, om=(0, 0, 12))}, "ear_right": {"rotation": golf(2, -8, L / 2, om=(0, 0, -12))},
            "flopsie_tong": {"scale": kf([(0, [1, 1, 1]), (L * 0.6, [1, 1, 1]), (L * 0.65, [1, 1.6, 1]), (L * 0.75, [1, 1, 1])])}}
    praat = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.2, [-8, 0, 8]), (0.6, [6, 0, -8]), (1.2, [0, 0, 0])])}}
    loop = {"body": {"position": kf([(0, [0, 0, 0]), (0.3, [0, 1, 0]), (0.6, [0, 0, 0]), (0.9, [0, 1, 0]), (1.2, [0, 0, 0])])}}
    for side, sx in (("left", 1), ("right", -1)):
        loop[f"arm_{side}"] = {"rotation": golf(0, 26 * sx, 1.2)}
        loop[f"leg_{side}"] = {"rotation": golf(0, -26 * sx, 1.2)}
    knuffel = {"arm_left": {"rotation": kf([(0, [0, 0, 0]), (0.5, [-80, 0, -70]), (1.0, [-80, 0, -70]), (1.4, [-80, 0, 10]), (1.8, [-80, 0, 10]), (2.0, [0, 0, 0])])},
               "arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.5, [-80, 0, 70]), (1.0, [-80, 0, 70]), (1.4, [-80, 0, -10]), (1.8, [-80, 0, -10]), (2.0, [0, 0, 0])])},
               "body": {"rotation": kf([(0, [0, 0, 0]), (0.5, [-12, 0, 0]), (1.8, [-12, 0, 0]), (2.0, [0, 0, 0])])}}
    spring = {"root": {"position": kf([(0, [0, 0, 0]), (0.25, [0, -2, 0]), (0.6, [0, 14, 0]), (1.0, [0, 0, 0]), (1.2, [0, -1, 0]), (1.5, [0, 0, 0])])},
              "arm_left": {"rotation": kf([(0, [0, 0, 0]), (0.6, [0, 0, -60]), (1.0, [0, 0, 0])])},
              "arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.6, [0, 0, 60]), (1.0, [0, 0, 0])])}}
    lik = {"flopsie_tong": {"scale": kf([(0, [1, 1, 1]), (0.2, [1.2, 2.6, 2.4]), (0.6, [1.2, 2.6, 2.4]), (0.8, [1, 1, 1]), (1.0, [1.2, 2.6, 2.4]), (1.3, [1, 1, 1])]),
                            "rotation": kf([(0, [0, 0, 0]), (0.3, [-20, 0, 15]), (0.5, [-20, 0, -15]), (0.8, [0, 0, 0])])},
           "head": {"rotation": kf([(0, [0, 0, 0]), (0.3, [10, 0, 0]), (1.3, [0, 0, 0])])}}
    return {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "loop": anim(1.2, loop), "knuffel": anim(2.0, knuffel, False),
            "spring": anim(1.5, spring, False), "lik": anim(1.5, lik, False)}


# =====================================================================================================================
# luchtbizon_guh: Appa-guh's model and animations, without the saddle (read-only from einde2_appa's generated files)
# =====================================================================================================================
BIZON_WEG = ("zadel", "kist", "bagage")
# per look: (fur, arrow) as the colour of Appa's cream fur and of his brown arrow become
BIZON_KLEUR = [((250, 242, 226), (150, 102, 72)), ((226, 194, 140), (130, 88, 58)), ((178, 176, 182), (96, 92, 100)),
               ((104, 74, 54), (244, 232, 206)), ((255, 248, 236), (176, 128, 96))]
APPA_VACHT, APPA_PIJL = (248, 240, 224), (146, 100, 72)          # einde2_appa_modellen.VACHT / PIJL


def _appa(h):
    A = h.A
    g = json.load(open(f"{A}/geckolib/models/entity/appa_guh.geo.json", encoding="utf-8"))
    an = json.load(open(f"{A}/geckolib/animations/entity/appa_guh.animation.json", encoding="utf-8"))
    tex = Image.open(f"{h.TEX}/entity/appa_guh.png").convert("RGBA")
    return g, an, tex


def luchtbizon_geo(h):
    g, _, _ = _appa(h)
    geo_ = g["minecraft:geometry"][0]
    weg = set()
    for b in geo_["bones"]:                                         # the saddle and everything hanging from it
        if b["name"] in BIZON_WEG or b.get("parent") in weg:
            weg.add(b["name"])
    geo_["bones"] = [b for b in geo_["bones"] if b["name"] not in weg]
    geo_["description"]["identifier"] = "geometry.luchtbizon_guh"
    return g, weg


def luchtbizon_animaties(h):
    _, an, _ = _appa(h)
    _, weg = luchtbizon_geo(h)
    uit = {}
    for naam, a_ in an["animations"].items():
        if naam == "jipjip":
            continue
        a_ = json.loads(json.dumps(a_))
        a_["bones"] = {b: v for b, v in a_.get("bones", {}).items() if b not in weg}
        uit[naam] = a_
    uit["praat"] = anim(1.2, {"head": {"rotation": kf([(0, [0, 0, 0]), (0.2, [-10, 0, 4]), (0.5, [6, 0, -4]), (0.8, [-8, 0, 2]), (1.2, [0, 0, 0])])}}, False)
    return uit


def luchtbizon_textuur(h, soort):
    """Appa's texture re-tinted: the cream fur -> this look's fur, and on the head and body the brown arrow -> its arrow
    (the horns and the toenails, also brown, keep their colour: they are other bones)."""
    g, _, tex = _appa(h)
    a = np.asarray(tex).astype(np.float32)
    geo_ = g["minecraft:geometry"][0]
    tw = geo_["description"]["texture_width"]
    f = a.shape[1] / tw
    pijlvlak = np.zeros(a.shape[:2], bool)
    for b in geo_["bones"]:
        if b["name"] not in ("head", "body"):
            continue
        for c in b.get("cubes", []):
            for face in c["uv"].values():
                (u, v), (w, hh) = face["uv"], face["uv_size"]
                pijlvlak[int(min(v, v + hh) * f):int(math.ceil(max(v, v + hh) * f)), int(min(u, u + w) * f):int(math.ceil(max(u, u + w) * f))] = True
    rgb = a[..., :3]
    vacht_k, pijl_k = BIZON_KLEUR[soort]
    licht = rgb.mean(-1)
    verz = rgb.max(-1) - rgb.min(-1)
    is_vacht = (licht > 170) & (verz < 60) & (a[..., 3] > 0)
    d_pijl = np.abs(rgb - np.array(APPA_PIJL, np.float32)).sum(-1)
    is_pijl = pijlvlak & (d_pijl < 90) & (a[..., 3] > 0) & ~is_vacht
    uit = a.copy()
    uit[is_vacht, :3] = np.clip(rgb[is_vacht] / np.array(APPA_VACHT, np.float32) * np.array(vacht_k, np.float32), 0, 255)
    uit[is_pijl, :3] = np.clip(rgb[is_pijl] / np.array(APPA_PIJL, np.float32) * np.array(pijl_k, np.float32), 0, 255)
    return Image.fromarray(uit.astype(np.uint8))


# =====================================================================================================================
# maanvis, oceaanvis: Tui and La
# =====================================================================================================================
VIS = {"maanvis": ((246, 244, 240), (30, 28, 34)), "oceaanvis": ((30, 28, 34), (246, 244, 240))}


def vis(a, id="maanvis"):
    kleur, vlek = VIS[id]
    huid = schubben(kleur, tuple(min(255, c + 30) for c in kleur), 4)
    kop = met(plain(kleur, 5), lambda W, H, rng, ar: (disc(ar, W * 0.5, H * 0.3, W * 0.2, vlek, ry=H * 0.2)))
    gezicht = guh_face(kleur, eye_y=0.42, eye_dx=0.28, eye_r=0.16, blush=False)
    vin = alpha(plain(tuple(int(c * 0.85 + 30) for c in kleur), 6), 190)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 2.5, 0], [cube(a, [-1.8, 1, -2], [3.6, 3.2, 6], huid)]),
             bone("head", "body", [0, 2.6, -2], [cube(a, [-1.6, 1.2, -5], [3.2, 2.8, 3], kop, overrides={"north": gezicht})]),
             bone("tail", "body", [0, 2.6, 4], [cube(a, [-0.2, -0.6, 4], [0.4, 6.4, 7], vin)]),
             bone("rug", "body", [0, 4.2, 0], [cube(a, [-0.15, 4.2, -1], [0.3, 2.4, 4.5], vin)])]
    for side, sx in (("left", 1), ("right", -1)):
        x = 1.8 if sx > 0 else -4.8
        bones.append(bone(f"fin_{side}", "body", [sx * 1.8, 1.6, -0.5], [cube(a, [x, 1.4, -1], [3, 0.3, 3.5], vin)]))
    return geo(id, bones, 1, 1)


def vis_animaties(gevangen):
    L = 4.0
    idle = {"tail": {"rotation": golf(1, 16, L / 2)}, "fin_left": {"rotation": golf(2, 14, L / 2)}, "fin_right": {"rotation": golf(2, -14, L / 2)},
            "body": {"position": golf(1, 0.3, L)}}
    praat = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.2, [-10, 0, 0]), (0.5, [6, 0, 0]), (0.8, [-8, 0, 0]), (1.2, [0, 0, 0])])}}
    zwem = {"tail": {"rotation": golf(1, 32, 0.8)}, "body": {"rotation": golf(1, 8, 0.8)}, "fin_left": {"rotation": golf(2, 24, 0.8)},
            "fin_right": {"rotation": golf(2, -24, 0.8)}}
    gloei = {"root": {"scale": kf([(0, [1, 1, 1]), (0.5, [1.25, 1.25, 1.25]), (1.0, [1, 1, 1]), (1.5, [1.25, 1.25, 1.25]), (2.0, [1, 1, 1])])},
             "rug": {"scale": kf([(0, [1, 1, 1]), (1.0, [1, 1.6, 1.2]), (2.0, [1, 1, 1])])}}
    uit = {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "zwem": anim(0.8, zwem), "gloei": anim(2.0, gloei, False)}
    if gevangen:
        uit["gevangen"] = anim(2.0, {"body": {"rotation": [0, 0, 80], "position": [0, -1.5, 0]}, "tail": {"rotation": [0, 30, 0]},
                                     "fin_left": {"rotation": [0, 0, 20]}, "fin_right": {"rotation": [0, 0, -20]}})
    return uit


# =====================================================================================================================
# animation helpers
# =====================================================================================================================
def kf(pairs):
    return {str(round(t, 3)): [round(x, 3) for x in v] for t, v in pairs}


def anim(length, bones, loop=True):
    return {"loop": loop, "animation_length": round(length, 3), "bones": bones}


def golf(as_, amp, length, n=4, fase=0.0, om=(0, 0, 0)):
    out = []
    for i in range(n + 1):
        v = list(om)
        v[as_] += amp * math.sin(2 * math.pi * (i / n) + fase)
        out.append((length * i / n, v))
    return kf(out)


def oor_idle(L, hoek=16):
    return {"ear_left": {"rotation": kf([(0, [0, 0, 0]), (L * 0.62, [0, 0, 0]), (L * 0.64, [0, 0, -hoek]), (L * 0.67, [0, 0, 0])])},
            "ear_right": {"rotation": kf([(0, [0, 0, 0]), (L * 0.3, [0, 0, 0]), (L * 0.32, [0, 0, hoek]), (L * 0.35, [0, 0, 0])])}}


# =====================================================================================================================
# writing, checking, the offline renders
# =====================================================================================================================
# id -> (maker(atlas), texel density, animations()) for the models drawn here
MODELLEN = {
    "draakguh": (draakguh, 0.5, draak_animaties),
    "bosco": (bosco, 1.0, bosco_animaties),
    "wan_shi_guh": (wan_shi_guh, 0.75, uil_animaties),
    "vosguh": (vosguh, 1.5, vos_animaties),
    "leeuwschildpad_guh": (leeuwschildpad_guh, 0.07, schildpad_animaties),
    "flopsie": (flopsie, 0.75, flopsie_animaties),
    "maanvis": ((lambda a: vis(a, "maanvis")), 2.0, (lambda: vis_animaties(True))),
    "oceaanvis": ((lambda a: vis(a, "oceaanvis")), 2.0, (lambda: vis_animaties(False))),
}


def _maak(id, soort=0):
    maker, k, _ = MODELLEN[id]
    atlas = Vel(id, k)
    model = draakguh(atlas, soort) if id == "draakguh" else maker(atlas)
    return atlas, model


def build(h):
    A = h.A
    for id, (_, _, animaties) in MODELLEN.items():
        atlas, model = _maak(id)
        h.w(f"{A}/geckolib/models/entity/{id}.geo.json", model)
        h.w(f"{A}/geckolib/animations/entity/{id}.animation.json", {"format_version": "1.8.0", "animations": animaties()})
        h.save(Image.fromarray(atlas.img), "entity", f"{id}.png")
    atlas, _ = _maak("draakguh", 1)                                  # (the same seed and sizes: the same layout)
    h.save(Image.fromarray(atlas.img), "entity", "draakguh_1.png")
    g, _ = luchtbizon_geo(h)
    h.w(f"{A}/geckolib/models/entity/luchtbizon_guh.geo.json", g)
    h.w(f"{A}/geckolib/animations/entity/luchtbizon_guh.animation.json", {"format_version": "1.8.0", "animations": luchtbizon_animaties(h)})
    for soort in range(len(BIZON_KLEUR)):
        h.save(luchtbizon_textuur(h, soort), "entity", "luchtbizon_guh.png" if soort == 0 else f"luchtbizon_guh_{soort}.png")


def texturen(id):
    soorten = WEZENS[id][2]
    if id not in TEXTUUR_PER_SOORT:
        return [id]
    return [id] + [f"{id}_{n}" for n in range(1, soorten)]


def check(h, model_check):
    problems = []
    for id, (_, _, soorten, anims) in WEZENS.items():
        wil = ["idle"] + [n for n, _ in anims] + ([f"soort_{n}" for n in range(soorten)] if id == "leeuwschildpad_guh" else [])
        model_check(id, f"{h.A}/geckolib/models/entity/{id}.geo.json", f"{h.A}/geckolib/animations/entity/{id}.animation.json", wil,
                    ("root", "body", "head"))
        if anims[0][0] != "praat":
            problems.append(f"{id}: the first animation of the row must be praat")
        for tex in texturen(id):
            if not os.path.exists(f"{h.A}/textures/entity/{tex}.png"):
                problems.append(f"{id}: no texture {tex}.png")
    if len(BIZON_SCHAAL) != WEZENS["luchtbizon_guh"][2] or len(BIZON_KLEUR) != len(BIZON_SCHAAL):
        problems.append("luchtbizon_guh: a scale and a colour per look")
    return problems


def voorbeelden(vang, alleen=None):
    """(name, geo, texture, hidden bones, pose) per creature and look, for einde2_cast_schurk_modellen.preview."""
    uit = []
    for id in MODELLEN:
        if alleen and id not in alleen:
            continue
        soorten = (0, 1) if id == "draakguh" else (0,)
        for soort in soorten:
            atlas, model = _maak(id, soort)
            tex = Image.fromarray(atlas.img)
            if id == "leeuwschildpad_guh":
                for k in (0, 1):
                    hide = tuple(b["name"] for b in model["minecraft:geometry"][0]["bones"]
                                 if b.get("parent") == f"kop_{1 - k}" or b.get("parent", "").startswith(("lip" + str(1 - k))))
                    uit.append((f"{id}_{k}", model, tex, hide, None))
                continue
            hide = ("draak_vuur",) if id == "draakguh" else ("bosco_honing",) if id == "bosco" else ()
            uit.append((f"{id}_{soort}" if id == "draakguh" else id, model, tex, hide, None))
    if not alleen or "luchtbizon_guh" in alleen:
        g, _ = luchtbizon_geo(vang)
        for soort in range(len(BIZON_KLEUR)):
            uit.append((f"luchtbizon_guh_{soort}", g, luchtbizon_textuur(vang, soort), (), None))
    return uit
