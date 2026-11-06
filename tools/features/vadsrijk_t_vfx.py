"""Het Vadsrijk 1.10, T-VFX: the VFX kit v1 + the boss bouwdoos (Java: feature/vadsrijk/vfx).

Writes:
  textures/vadsrijk/vfx/deeltjes.png      the particle sprite sheet (8 x 4 cells of 32 px, white, tinted in code; see Deeltje.java)
  textures/vadsrijk/vfx/decal_vlak.png    decal fill (u = 0 middle .. 1 edge: a soft gradient with a little sparkle)
  textures/vadsrijk/vfx/decal_rand.png    decal rim tile (u = 0 edge .. 1 inside; one chevron per tile)
  textures/vadsrijk/vfx/decal_veilig.png  safe-spot rim tile (one heart per tile)
  textures/vadsrijk/vfx/gloed.png         soft band (u across) for shockwaves, beams, trails, pillars
  textures/vadsrijk/vfx/flits.png         radial glow for impact flashes
  the generic boss animations vfx_windup / vfx_treffer / vfx_geraakt / vfx_fase / vfx_woede / vfx_juich, MERGED into every boss
  animation file (on the bones that model has), so this module must run after the boss slices (end of FEATURES);
  the texts (attack names for the cast bar, messages, the config label).

Run standalone from the project root:  PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_vfx.py
"""
import glob
import json
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

# ---- texts -------------------------------------------------------------------------------------------------------------------

A = "vadsrijk.vfx.aanval."
# attack id: (nl, en)
AANVALLEN = {
    "kloddertjesregen": ("Kloddertjesregen", "Blob Rain"),
    "kloddergolf": ("Kloddergolf", "Blob Wave"),
    "kroonregen": ("Kroonregen", "Crown Rain"),
    "koninklijk_bad": ("Koninklijk Bad", "Royal Bath"),
    "sporenkegel": ("Sporenkegel", "Spore Cone"),
    "sporenkruis": ("Sporenkruis", "Spore Cross"),
    "echobrul": ("Echobrul", "Echo Roar"),
    "galmring": ("Galmring", "Echo Ring"),
    "spiegelscherven": ("Spiegelscherven", "Mirror Shards"),
    "spiegelzaal": ("Spiegelzaal", "Hall of Mirrors"),
    "boorlinie": ("Boorlinie", "Drill Line"),
    "overdruk": ("Overdruk", "Overpressure"),
    "veerregen": ("Veerregen", "Feather Rain"),
    "windkegel": ("Windkegel", "Wind Cone"),
    "touwzwiep": ("Touwzwiep", "Rope Lash"),
    "kaasvaten": ("Kaasvaten", "Cheese Barrels"),
    "kruisvuur": ("Kruisvuur", "Crossfire"),
    "dubbel_kruisvuur": ("Dubbel Kruisvuur", "Double Crossfire"),
    "sabelsnede": ("Sabelsnede", "Sabre Slash"),
    "sabelstorm": ("Sabelstorm", "Sabre Storm"),
    "capezwiep": ("Capezwiep", "Cape Swipe"),
    "piepkoor": ("Piepkoor", "Squeak Choir"),
    "wortelgreep": ("Wortelgreep", "Root Grip"),
    "nestschok": ("Nestschok", "Nest Shock"),
    "degenreeks": ("Degenreeks", "Rapier Combo"),
    "duet": ("Duet", "Duet"),
    "vuurregen": ("Vuurregen", "Fire Rain"),
    "hemelstorm": ("Hemelstorm", "Sky Storm"),
    "vlammenkruis": ("Vlammenkruis", "Flame Cross"),
    "laatste_brul": ("Laatste Brul", "Last Roar"),
    # signature moves of the bosses' own rotations (cast bars added by T-VFX one-liners)
    "plettersprong": ("Plettersprong", "Squash Jump"),
    "grote_plof": ("Grote Plof", "Big Plop"),
    "rolaanval": ("Rolaanval", "Rolling Charge"),
    "sporenstamp": ("Sporenstamp", "Spore Stomp"),
    "grauwe_huil": ("Grauwe Huil", "Grey Howl"),
    "vleugelwind": ("Vleugelwind", "Wing Gust"),
    "grauw_vuur": ("Grauw Vuur", "Grey Fire"),
    "galm": ("De Galm", "The Howl"),
    "boorstoot": ("Boorstoot", "Drill Thrust"),
    "vleugelslag": ("Vleugelslag", "Wing Beat"),
    "duikvlucht": ("Duikvlucht", "Dive"),
    "zinkend_schip": ("Het schip zinkt!", "The ship is sinking!"),
    "demo": ("Proefcast", "Test Cast"),
}
TEKSTEN = {
    "vadsrijk.vfx.cast.laadt": ("%s laadt: %s", "%s is casting: %s"),
    "vadsrijk.vfx.cast.onderbreek": ("Onderbreek! Raak hem hard of verdoof hem", "Interrupt! Hit it hard or stun it"),
    "vadsrijk.vfx.cast.onderbroken": ("ONDERBROKEN!", "INTERRUPTED!"),
    "vadsrijk.vfx.onderbroken": ("%s onderbroken! De baas is even suf.", "%s interrupted! The boss is dazed."),
    "vadsrijk.vfx.geheeld": ("%s heeft zich opgelapt! (niet onderbroken)", "%s patched itself up! (not interrupted)"),
    "vadsrijk.vfx.woede": ("%s wordt woest: zijn aanvallen komen vaker!", "%s is enraged: its attacks come faster!"),
    "guhs.configuration.vadsrijkVfx": ("Vadsrijk: effecten (VOL / ZACHT / UIT)", "Vadsrijk: effects (FULL / SOFT / OFF)"),
    "guhs.configuration.vadsrijkVfx.tooltip": ("Deeltjes, schokgolven, stralen en flitsen van bazen. Telegraphs op de grond blijven altijd zichtbaar.",
                                               "Boss particles, shockwaves, beams and flashes. Ground telegraphs always stay visible."),
}


def build(h):
    for k, (nl, en) in AANVALLEN.items():
        h.lang(A + k, en, nl)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    a_dir = os.path.join("src", "main", "resources", "assets", "guhs")
    textures(a_dir)
    animaties(a_dir)


# ---- textures ----------------------------------------------------------------------------------------------------------------

W = (255, 255, 255)


def _zacht(im, r):
    return im.filter(ImageFilter.GaussianBlur(r))


def _cel(tekenaar, rnd, frame):
    im = Image.new("RGBA", (32, 32), (255, 255, 255, 0))
    tekenaar(ImageDraw.Draw(im), im, rnd, frame)
    return im


def ster(d, im, rnd, f):
    s = [9, 12, 14, 11][f]
    c = 16
    d.polygon([(c, c - s), (c + 2, c - 2), (c + s, c), (c + 2, c + 2), (c, c + s), (c - 2, c + 2), (c - s, c), (c - 2, c - 2)], fill=W + (255,))
    d.ellipse([c - 3, c - 3, c + 3, c + 3], fill=W + (255,))


def pluis(d, im, rnd, f):
    for _ in range(9):
        x, y, r = 16 + rnd.randint(-6, 6), 16 + rnd.randint(-6, 6), rnd.randint(4, 8)
        d.ellipse([x - r, y - r, x + r, y + r], fill=W + (rnd.randint(110, 190),))
    for _ in range(5 + f):
        a = rnd.random() * 6.28
        d.line([(16, 16), (16 + math.cos(a) * 14, 16 + math.sin(a) * 14)], fill=W + (120,), width=1)


def vonk(d, im, rnd, f):
    d.ellipse([12, 12, 20, 20], fill=W + (255,))
    l = 12 if f == 0 else 8
    d.line([(16 - l, 16), (16 + l, 16)], fill=W + (200,), width=2)
    d.line([(16, 16 - l), (16, 16 + l)], fill=W + (200,), width=2)


def slijm(d, im, rnd, f):
    d.ellipse([9, 10, 23, 22], fill=W + (230,))
    for _ in range(4 + f * 2):
        x, y, r = 16 + rnd.randint(-11, 11), 16 + rnd.randint(-11, 11), rnd.randint(2, 4)
        d.ellipse([x - r, y - r, x + r, y + r], fill=W + (210,))
    d.ellipse([12, 12, 16, 15], fill=W + (255,))


def spoor(d, im, rnd, f):
    for _ in range(7):
        x, y, r = 16 + rnd.randint(-7, 7), 16 + rnd.randint(-7, 7), rnd.randint(4, 9)
        d.ellipse([x - r, y - r, x + r, y + r], fill=W + (rnd.randint(60, 120),))
    for _ in range(4):
        x, y = 16 + rnd.randint(-9, 9), 16 + rnd.randint(-9, 9)
        d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=W + (230,))


def kristal(d, im, rnd, f):
    pts = [(16, 3), (22, 14), (18, 29), (10, 24), (9, 11)] if f == 0 else [(14, 4), (25, 12), (20, 27), (8, 22)]
    d.polygon(pts, fill=W + (220,))
    d.line([pts[0], pts[2]], fill=W + (255,), width=1)


def honing(d, im, rnd, f):
    d.ellipse([10, 13, 22, 27], fill=W + (235,))
    d.polygon([(16, 4), (21, 16), (11, 16)], fill=W + (235,))
    d.ellipse([13, 16, 16, 20], fill=W + (255,))


def confetti(d, im, rnd, f):
    if f == 0:
        d.rectangle([11, 13, 21, 19], fill=W + (255,))
    else:
        d.polygon([(10, 12), (22, 16), (12, 21)], fill=W + (255,))


def hart(d, im, rnd, f):
    s = 1.0 if f == 0 else 0.8
    c = 16
    d.ellipse([c - 11 * s, c - 9 * s, c + 1 * s, c + 3 * s], fill=W + (255,))
    d.ellipse([c - 1 * s, c - 9 * s, c + 11 * s, c + 3 * s], fill=W + (255,))
    d.polygon([(c - 10.5 * s, c - 1 * s), (c + 10.5 * s, c - 1 * s), (c, c + 11 * s)], fill=W + (255,))


def rook(d, im, rnd, f):
    for _ in range(6):
        x, y, r = 16 + rnd.randint(-5, 5), 16 + rnd.randint(-5, 5), rnd.randint(6, 10)
        d.ellipse([x - r, y - r, x + r, y + r], fill=W + (rnd.randint(50, 100),))


def veer(d, im, rnd, f):
    d.ellipse([12, 3, 20, 29], fill=W + (220,))
    d.line([(16, 2), (16, 30)], fill=W + (255,), width=1)
    for y in range(6, 28, 4):
        d.line([(16, y), (11 if f == 0 else 21, y + 3)], fill=W + (140,), width=1)


def blad(d, im, rnd, f):
    d.polygon([(16, 3), (25, 14), (16, 29), (7, 14)], fill=W + (225,))
    d.line([(16, 5), (16, 27)], fill=W + (255,), width=1)


def gloed(d, im, rnd, f):
    for r in range(15, 0, -1):
        a = int(255 * (1 - r / 15) ** 1.6)
        d.ellipse([16 - r, 16 - r, 16 + r, 16 + r], fill=W + (a,))


DEELTJES = [  # (painter, frames): the order and cell numbers of Deeltje.java
    (ster, 4), (pluis, 4), (vonk, 2), (slijm, 2), (spoor, 2), (kristal, 2), (honing, 2), (confetti, 2), (hart, 2), (rook, 2), (veer, 2),
    (blad, 2), (gloed, 1),
]


def deeltjes_sheet():
    sheet = Image.new("RGBA", (256, 128), (255, 255, 255, 0))
    rnd = random.Random(1100)
    cel = 0
    for painter, frames in DEELTJES:
        for f in range(frames):
            im = _cel(painter, rnd, f)
            if painter in (pluis, spoor, rook):
                im = _zacht(im, 1.2)
            elif painter is not gloed:
                im = _zacht(im, 0.35)
            sheet.paste(im, ((cel % 8) * 32, (cel // 8) * 32))
            cel += 1
    return sheet


def decal_vlak():
    im = Image.new("RGBA", (64, 64))
    rnd = random.Random(7)
    px = im.load()
    for x in range(64):
        u = x / 63
        for y in range(64):
            a = 0.55 + 0.45 * u ** 2.2 + rnd.uniform(-0.05, 0.05)
            px[x, y] = (255, 255, 255, int(255 * max(0.0, min(1.0, a))))
    return im


def decal_rand(hart_teken=False):
    im = Image.new("RGBA", (64, 64), (255, 255, 255, 0))
    px = im.load()
    for x in range(64):
        u = x / 63
        a = 1.0 if u < 0.28 else max(0.0, 1 - (u - 0.28) / 0.72) ** 1.5 * 0.85
        for y in range(64):
            px[x, y] = (255, 255, 255, int(255 * a))
    d = ImageDraw.Draw(im)
    if hart_teken:  # a small heart inside each tile
        c, s = (40, 32), 0.9
        d.ellipse([c[0] - 9 * s, c[1] - 8 * s, c[0] + 1 * s, c[1] + 2 * s], fill=(255, 255, 255, 255))
        d.ellipse([c[0] - 9 * s, c[1] - 2 * s, c[0] + 1 * s, c[1] + 8 * s], fill=(255, 255, 255, 255))
        d.polygon([(c[0] - 4, c[1] - 9), (c[0] - 4, c[1] + 9), (c[0] + 10, c[1])], fill=(255, 255, 255, 255))
    else:  # a chevron pointing inwards
        d.line([(22, 14), (40, 32), (22, 50)], fill=(255, 255, 255, 235), width=6)
    return im


def gloed_band():
    im = Image.new("RGBA", (64, 16))
    px = im.load()
    for x in range(64):
        u = x / 63
        a = math.exp(-((u - 0.5) / 0.22) ** 2)
        core = math.exp(-((u - 0.5) / 0.07) ** 2)
        for y in range(16):
            px[x, y] = (255, 255, 255, int(255 * min(1.0, a * 0.8 + core * 0.6)))
    return im


def flits():
    im = Image.new("RGBA", (64, 64))
    px = im.load()
    for x in range(64):
        for y in range(64):
            r = math.hypot(x - 31.5, y - 31.5) / 32
            a = max(0.0, 1 - r) ** 2.0
            ster = max(0.0, 1 - abs(x - 31.5) / 2.5) * max(0.0, 1 - r) + max(0.0, 1 - abs(y - 31.5) / 2.5) * max(0.0, 1 - r)
            px[x, y] = (255, 255, 255, int(255 * min(1.0, a + ster * 0.6)))
    return im


def textures(a_dir):
    d = os.path.join(a_dir, "textures", "vadsrijk", "vfx")
    os.makedirs(d, exist_ok=True)
    deeltjes_sheet().save(os.path.join(d, "deeltjes.png"))
    decal_vlak().save(os.path.join(d, "decal_vlak.png"))
    decal_rand().save(os.path.join(d, "decal_rand.png"))
    decal_rand(True).save(os.path.join(d, "decal_veilig.png"))
    gloed_band().save(os.path.join(d, "gloed.png"))
    flits().save(os.path.join(d, "flits.png"))


# ---- the generic boss animations ---------------------------------------------------------------------------------------------

# which model (geo) each boss animation file belongs to when the names differ (the rest: same name)
GEO_VAN = {"vr_z3_baas_vahoegnir": "vadsrijk/wezens/draak"}
# boss models outside the vr_*baas_* naming: the Klodderreus once t-models gives it a GeckoLib model (NOTITIES_T_VFX: bone names)
EXTRA = ["vr_klodderreus", "vadsrijk/klodderreus"]
LINKS = ["arm_left", "arm_l", "leg_front_left", "pootvl", "vleugel_l", "wing_l"]
RECHTS = ["arm_right", "arm_r", "leg_front_right", "pootvr", "vleugel_r", "wing_r"]
NAMEN = ["vfx_windup", "vfx_treffer", "vfx_geraakt", "vfx_fase", "vfx_woede", "vfx_juich"]


def _eerste(bones, kandidaten):
    for k in kandidaten:
        if k in bones:
            return k
    return None


def generiek(bones):
    """The six generic moves on the bones this model has (root always; body/head/arms/wings when present)."""
    body = "body" if "body" in bones else None
    head = _eerste(bones, ["head", "kop", "hoed", "neck_1"])
    links, rechts = _eerste(bones, LINKS), _eerste(bones, RECHTS)
    an = {}

    def anim(naam, lengte, bb, loop=False):
        an[naam] = {"loop": loop, "animation_length": lengte, "bones": {k: v for k, v in bb.items() if k}}

    # wind-up: crouch, lean in, arms back (held: played with thenPlayAndHold)
    bb = {"root": {"scale": {"0.0": [1, 1, 1], "0.35": [1.12, 0.84, 1.12], "0.5": [1.1, 0.86, 1.1]},
                   "position": {"0.0": [0, 0, 0], "0.5": [0, -0.6, 0]}}}
    if body:
        bb[body] = {"rotation": {"0.0": [0, 0, 0], "0.5": [12, 0, 0]}}
    if head:
        bb[head] = {"rotation": {"0.0": [0, 0, 0], "0.5": [-14, 0, 0]}}
    if links:
        bb[links] = {"rotation": {"0.0": [0, 0, 0], "0.5": [-45, 0, -15]}}
    if rechts:
        bb[rechts] = {"rotation": {"0.0": [0, 0, 0], "0.5": [-45, 0, 15]}}
    anim("vfx_windup", 0.5, bb)
    # the hit: pop up, lean through, arms forward
    bb = {"root": {"scale": {"0.0": [1.1, 0.86, 1.1], "0.08": [0.92, 1.15, 0.92], "0.2": [1.05, 0.95, 1.05], "0.35": [1, 1, 1]},
                   "position": {"0.0": [0, -0.6, 0], "0.08": [0, 0.4, 0], "0.35": [0, 0, 0]}}}
    if body:
        bb[body] = {"rotation": {"0.0": [12, 0, 0], "0.08": [-10, 0, 0], "0.35": [0, 0, 0]}}
    if links:
        bb[links] = {"rotation": {"0.0": [-45, 0, -15], "0.08": [35, 0, 0], "0.35": [0, 0, 0]}}
    if rechts:
        bb[rechts] = {"rotation": {"0.0": [-45, 0, 15], "0.08": [35, 0, 0], "0.35": [0, 0, 0]}}
    anim("vfx_treffer", 0.35, bb)
    # a small flinch
    bb = {"root": {"rotation": {"0.0": [0, 0, 0], "0.06": [0, 0, 6], "0.15": [0, 0, -4], "0.3": [0, 0, 0]},
                   "scale": {"0.0": [1, 1, 1], "0.06": [1.05, 0.93, 1.05], "0.3": [1, 1, 1]}}}
    if head:
        bb[head] = {"rotation": {"0.0": [0, 0, 0], "0.06": [12, 0, 0], "0.3": [0, 0, 0]}}
    anim("vfx_geraakt", 0.3, bb)
    # phase change: rear up, shake, arms/wings wide
    bb = {"root": {"position": {"0.0": [0, 0, 0], "0.3": [0, 2, 0], "0.7": [0, 2, 0], "1.0": [0, 0, 0]},
                   "rotation": {"0.0": [0, 0, 0], "0.3": [-15, 0, 0], "0.7": [-15, 0, 0], "1.0": [0, 0, 0]},
                   "scale": {"0.0": [1, 1, 1], "0.3": [1.15, 1.15, 1.15], "1.0": [1, 1, 1]}}}
    if body:
        bb[body] = {"rotation": {"0.0": [0, 0, 0], "0.4": [0, 0, 5], "0.5": [0, 0, -5], "0.6": [0, 0, 5], "0.7": [0, 0, 0]}}
    if head:
        bb[head] = {"rotation": {"0.0": [0, 0, 0], "0.3": [-25, 0, 0], "0.7": [-25, 0, 0], "1.0": [0, 0, 0]}}
    if links:
        bb[links] = {"rotation": {"0.0": [0, 0, 0], "0.3": [0, 0, -70], "0.8": [0, 0, -70], "1.2": [0, 0, 0]}}
    if rechts:
        bb[rechts] = {"rotation": {"0.0": [0, 0, 0], "0.3": [0, 0, 70], "0.8": [0, 0, 70], "1.2": [0, 0, 0]}}
    anim("vfx_fase", 1.2, bb)
    # enrage: a hot tremble
    tr = {f"{t / 10:.1f}": [0, 0, (4 if t % 2 else -4) if 0 < t < 15 else 0] for t in range(0, 16)}
    bb = {"root": {"rotation": tr, "scale": {"0.0": [1, 1, 1], "0.3": [1.06, 1.06, 1.06], "1.2": [1.06, 1.06, 1.06], "1.5": [1, 1, 1]}}}
    if head:
        bb[head] = {"rotation": {"0.0": [0, 0, 0], "0.3": [-20, 0, 0], "1.2": [-20, 0, 0], "1.5": [0, 0, 0]}}
    anim("vfx_woede", 1.5, bb)
    # the cheer-up finale: sway, little hops, and wave
    bb = {"root": {"rotation": {"0.0": [0, 0, 0], "0.5": [0, 0, 10], "1.0": [0, 0, -10], "1.5": [0, 0, 10], "2.0": [0, 0, 0]},
                   "position": {"0.0": [0, 0, 0], "0.25": [0, 1.5, 0], "0.5": [0, 0, 0], "0.75": [0, 1.5, 0], "1.0": [0, 0, 0],
                                "1.25": [0, 1.2, 0], "1.5": [0, 0, 0]}}}
    if rechts:
        bb[rechts] = {"rotation": {"0.0": [0, 0, 0], "0.2": [0, 0, 150], "0.45": [0, 0, 115], "0.7": [0, 0, 150], "0.95": [0, 0, 115],
                                   "1.2": [0, 0, 150], "1.45": [0, 0, 115], "1.8": [0, 0, 0]}}
    elif head:
        bb[head] = {"rotation": {"0.0": [0, 0, 0], "0.3": [0, 0, 12], "0.7": [0, 0, -12], "1.1": [0, 0, 12], "1.5": [0, 0, 0]}}
    anim("vfx_juich", 2.0, bb)
    return an


def _bones(geo_pad):
    with open(geo_pad, encoding="utf-8") as f:
        g = json.load(f)
    return {b["name"] for b in g["minecraft:geometry"][0]["bones"]}


def boss_animatiebestanden(a_dir):
    """(animation file, geo file) of every boss: zone 1 (vr_baas_*), zone 2 (vr_z2_baas_*), zone 3 (vr_z3_baas_*). (Not the shared
    guh file: the Mika spy is skipped by BaasAnims.)"""
    an_dir = os.path.join(a_dir, "geckolib", "animations", "entity")
    geo_dir = os.path.join(a_dir, "geckolib", "models", "entity")
    paren = []
    extra = [os.path.join(an_dir, n + ".animation.json") for n in EXTRA]
    for pad in sorted(glob.glob(os.path.join(an_dir, "vr_*baas_*.animation.json"))) + [e for e in extra if os.path.exists(e)]:
        naam = os.path.relpath(pad, an_dir)[:-len(".animation.json")].replace("\\", "/")
        geo = os.path.join(geo_dir, GEO_VAN.get(naam, naam) + ".geo.json")
        if os.path.exists(geo):
            paren.append((pad, geo))
    return paren


def animaties(a_dir):
    n = 0
    for pad, geo in boss_animatiebestanden(a_dir):
        with open(pad, encoding="utf-8") as f:
            data = json.load(f)
        data.setdefault("animations", {}).update(generiek(_bones(geo)))
        _schrijf(pad, data)
        n += 1
    return n


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    textures(a)
    n = animaties(a)
    teksten = {A + k: v for k, v in AANVALLEN.items()}
    teksten.update(TEKSTEN)
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in teksten.items()})
        _schrijf(p, data)
    print(f"vadsrijk_t_vfx: 6 textures, generic anims in {n} boss animation files, {len(teksten)} texts")


if __name__ == "__main__":
    _direct()
