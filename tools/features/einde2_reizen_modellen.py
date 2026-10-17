"""
einde2 phase B, slice reizen - the looks of its own things: Zuka's flying warship (GeckoLib model, animations, texture,
glow mask) and the item picture of the old captain's horn.

  einde2_reizen_zuka_schip   a grey Mika warship that flies through the void: a riveted steel hull with a pointed ram bow
                             and a red stripe, a bridge with warm lit windows, ONE big smokestack with a red band and the
                             flame mark of the Mika's, a bow cannon (the fireballs), two stubby side fins and a spinning
                             propeller at the stern, and a red pennant. Drawn at ZukaSchipEntity.SCHAAL (4): one model pixel
                             is a quarter block, so the ship is about 12 blocks long. Its bow is -z (the Java side puts Zuka
                             on the bow, ZukaSchipEntity.BOEG, and fires from the cannon, KANON: keep those in step).
                             Bones: root, body, romp, brug, schoorsteen, kanon, vin_links, vin_rechts, propeller, wimpel.
                             Animations: vaar (propeller spins, a gentle roll, the pennant flutters), sputter (the propeller
                             coughs, the nose dips, it lists to one side).
  walvishoorn (item)         the captain's old horn: a curled shell horn with a brass mouthpiece and a leather strap.
  einde2_reizen_appa_platform (block, fix4-appa) the middle of an own Appa landing place: on top a cream fur pad in a
                             brown wooden frame with the blue arrow of the luchtnomaden (like Appa's forehead); the sides
                             hay-brown planks with a blue band and two little hoof prints; the bottom plain planks.

  build(h)       writes geo/animations/textures + the item picture
  check(h)       every bone the animations move exists; the animations the Java side plays exist
  preview(out)   (python tools/features/einde2_reizen_modellen.py <out>) offline renders
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

try:
    from features import boerderij_dieren as bd
    from features import landdiertjes_modellen as lm
    from features import einde2_zwevers_modellen as zm
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import boerderij_dieren as bd
    from features import landdiertjes_modellen as lm
    from features import einde2_zwevers_modellen as zm

plain, disc, cube, bone, geo = bd.plain, bd.disc, bd.cube, bd.bone, bd.geo
kf, anim = lm.kf, lm.anim

SCHIP = "einde2_reizen_zuka_schip"

STAAL = (112, 112, 124)
STAAL_LICHT = (150, 150, 162)
STAAL_DONKER = (72, 72, 84)
ROOD = (186, 38, 40)
ROOD_DONKER = (128, 22, 28)
ZWART = (38, 34, 40)
RAAM = (255, 196, 104)                 # exactly this colour glows
VLAM = (255, 142, 40)                  # the flame mark (glows)
GOUD = (226, 176, 60)


def _klinknagels(kleur, donker, stap=6):
    """Riveted steel plates: seams every few texels and rivets along them."""
    def extra(W, H, rng, a):
        for y in range(0, H, max(4, stap * 2)):
            a[y:y + 1, :, :3] = donker
            for x in range(2, W, stap):
                a[min(H - 1, y + 2), x, :3] = STAAL_LICHT
        for x in range(0, W, max(8, stap * 4)):
            a[:, x:x + 1, :3] = donker
    return plain(kleur, 7, extra)


def _romp_zij(W, H, rng):
    """The hull's side: riveted steel with a red stripe along it and portholes."""
    a = _klinknagels(STAAL, STAAL_DONKER)(W, H, rng).astype(np.float32)
    a[int(H * 0.18):int(H * 0.32), :, :3] = ROOD
    for x in range(int(W * 0.08), W - 4, max(10, W // 8)):
        disc(a, x, H * 0.58, max(1.5, H * 0.08), ZWART)
        disc(a, x, H * 0.58, max(1.0, H * 0.05), RAAM)
    return np.clip(a, 0, 255).astype(np.uint8)


def _brug(W, H, rng):
    """The bridge: steel with a row of lit windows."""
    a = _klinknagels(STAAL_LICHT, STAAL_DONKER, 5)(W, H, rng).astype(np.float32)
    y0, y1 = int(H * 0.3), int(H * 0.55)
    for x in range(2, W - 3, max(5, W // 5)):
        a[y0:y1, x:x + max(2, W // 10), :3] = RAAM
    return np.clip(a, 0, 255).astype(np.uint8)


def _schoorsteen(W, H, rng):
    """The smokestack: dark steel, a red band with the Mika flame mark, soot at the top."""
    a = plain(STAAL_DONKER, 6)(W, H, rng).astype(np.float32)
    y0, y1 = int(H * 0.35), int(H * 0.62)
    a[y0:y1, :, :3] = ROOD
    cx, cy = W / 2, (y0 + y1) / 2
    disc(a, cx, cy + (y1 - y0) * 0.12, (y1 - y0) * 0.28, VLAM)
    disc(a, cx, cy - (y1 - y0) * 0.12, (y1 - y0) * 0.17, VLAM)
    a[:max(1, H // 10), :, :3] = ZWART
    return np.clip(a, 0, 255).astype(np.uint8)


def _wimpel(W, H, rng):
    a = plain(ROOD, 6)(W, H, rng).astype(np.float32)
    disc(a, W * 0.35, H * 0.5, min(W, H) * 0.22, VLAM)
    return np.clip(a, 0, 255).astype(np.uint8)


def _propeller(W, H, rng):
    a = plain(STAAL_DONKER, 6)(W, H, rng).astype(np.float32)
    a[:, :max(1, W // 6), :3] = GOUD
    return np.clip(a, 0, 255).astype(np.uint8)


def _naden(W, H, rng, a):
    """The deck's planks: a dark seam every four texels."""
    for x in range(0, W, 4):
        a[:, x:x + 1, :3] = (70, 56, 46)


def zuka_schip(atlas):
    staal, donker, rood = _klinknagels(STAAL, STAAL_DONKER), plain(STAAL_DONKER, 6), plain(ROOD, 6)
    dek = plain((96, 78, 64), 8, _naden)
    gloei_zij = zm.gloeit_kleur(_romp_zij, RAAM)
    gloei_brug = zm.gloeit_kleur(_brug, RAAM)
    gloei_schoorsteen = zm.gloeit_kleur(_schoorsteen, VLAM)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], []),
             bone("romp", "body", [0, 6, 0], [
                 # the hull: its deck is at y 8 (= ZukaSchipEntity.BOEG 2.1 blocks at scale 4)
                 cube(atlas, [-6, 2, -16], [12, 6, 34], staal, overrides={"east": gloei_zij, "west": gloei_zij, "up": dek, "down": donker}),
                 cube(atlas, [-4, 3, -21], [8, 5, 5], staal, overrides={"east": gloei_zij, "west": gloei_zij, "up": dek}),
                 cube(atlas, [-2, 4, -24], [4, 4, 3], donker, overrides={"up": dek}),
                 cube(atlas, [-4, 0, -12], [8, 2, 26], donker),
                 cube(atlas, [-6.5, 8, -15], [0.5, 1.5, 32], donker), cube(atlas, [6, 8, -15], [0.5, 1.5, 32], donker),
                 cube(atlas, [-5.5, 7.6, 17.5], [11, 1.8, 1], rood)]),
             bone("brug", "romp", [0, 8.5, 8], [
                 cube(atlas, [-4, 8.5, 4], [8, 7.5, 8], gloei_brug, overrides={"up": donker, "down": donker}),
                 cube(atlas, [-4.5, 16, 3.5], [9, 1, 9], rood)]),
             bone("schoorsteen", "brug", [0, 17, 5], [
                 cube(atlas, [-2, 17, 3], [4, 9, 4], gloei_schoorsteen, overrides={"up": plain(ZWART, 4), "down": donker})]),
             bone("kanon", "romp", [0, 10, -18], [
                 cube(atlas, [-1.5, 8.5, -19], [3, 1.5, 3], donker),
                 cube(atlas, [-1, 9.4, -23], [2, 2, 5], plain(ZWART, 5))]),
             bone("vin_links", "romp", [6, 5, 1], [cube(atlas, [6, 4.4, -4], [5, 1, 10], staal, overrides={"up": rood})], rotation=[0, 0, -8]),
             bone("vin_rechts", "romp", [-6, 5, 1], [cube(atlas, [-11, 4.4, -4], [5, 1, 10], staal, overrides={"up": rood})], rotation=[0, 0, 8]),
             bone("propeller", "romp", [0, 5, 19.2], [
                 cube(atlas, [-0.6, 4.4, 18], [1.2, 1.2, 2], donker),
                 cube(atlas, [-4.5, 4.6, 19.6], [9, 0.8, 0.4], _propeller),
                 cube(atlas, [-0.4, 0.5, 19.6], [0.8, 9, 0.4], _propeller)]),
             bone("wimpel", "brug", [0, 17, 11], [
                 cube(atlas, [-0.25, 17, 10.75], [0.5, 8, 0.5], donker),
                 cube(atlas, [-0.1, 21.5, 11.25], [0.2, 3, 5], _wimpel)])]
    return geo(SCHIP, bones, 26, 28)


def zuka_schip_anims():
    L = 1.0
    draai = {"rotation": kf([(0, [0, 0, 0]), (L / 2, [0, 0, 180]), (L, [0, 0, 360])])}
    vaar = {"propeller": draai,
            "body": {"rotation": kf([(0, [0, 0, -1.5]), (1.5, [0, 0, 1.5]), (3.0, [0, 0, -1.5])]),
                     "position": kf([(0, [0, 0, 0]), (1.5, [0, 0.6, 0]), (3.0, [0, 0, 0])])},
            "wimpel": {"rotation": kf([(0, [0, -8, 0]), (0.5, [0, 10, 0]), (1.0, [0, -6, 0]), (1.5, [0, 9, 0]), (3.0, [0, -8, 0])])},
            "vin_links": {"rotation": kf([(0, [0, 0, 0]), (1.5, [0, 0, -4]), (3.0, [0, 0, 0])])},
            "vin_rechts": {"rotation": kf([(0, [0, 0, 0]), (1.5, [0, 0, 4]), (3.0, [0, 0, 0])])}}
    sputter = {"propeller": {"rotation": kf([(0, [0, 0, 0]), (0.6, [0, 0, 120]), (0.8, [0, 0, 110]), (1.6, [0, 0, 250]), (2.0, [0, 0, 240]),
                                             (3.0, [0, 0, 360])])},
               "body": {"rotation": kf([(0, [-8, 0, 6]), (0.7, [-11, 0, 9]), (1.4, [-7, 0, 5]), (2.2, [-12, 0, 10]), (3.0, [-8, 0, 6])])},
               "schoorsteen": {"rotation": kf([(0, [0, 0, 0]), (0.3, [3, 0, 0]), (0.6, [0, 0, 0]), (1.9, [0, 0, 0]), (2.1, [4, 0, 0]), (2.4, [0, 0, 0]),
                                                (3.0, [0, 0, 0])])},
               "wimpel": {"rotation": kf([(0, [0, 0, 0]), (3.0, [0, 0, 0])])}}
    return {"format_version": "1.8.0", "animations": {"vaar": anim(3.0, vaar), "sputter": anim(3.0, sputter)}}


# =====================================================================================================================
# the horn (16 x 16 item)
# =====================================================================================================================
def walvishoorn():
    """A curled shell horn (cream with lilac bands, the whale's colours), a brass mouthpiece and a leather strap."""
    img = np.zeros((16, 16, 4), np.uint8)
    schelp, band, rand, koper, leer = (246, 230, 200), (150, 128, 210), (110, 86, 70), (226, 176, 60), (140, 92, 56)
    # the curve of the horn: from the wide bell (top right) curling down to the mouthpiece (bottom left)
    for i in range(14):
        t = i / 13.0
        cx = 12.5 - 9.5 * t + 1.6 * math.sin(t * math.pi)
        cy = 3.0 + 8.5 * t + 1.6 * math.sin(t * math.pi)
        r = 3.3 * (1 - t) + 0.9
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d <= r:
                    c = band if i % 4 == 1 else schelp
                    if d > r - 0.8:
                        c = rand
                    img[y, x, :3] = c
                    img[y, x, 3] = 255
    # the bell's dark opening
    for y in range(16):
        for x in range(16):
            if math.hypot(x + 0.5 - 13.0, y + 0.5 - 2.6) <= 1.6:
                img[y, x] = (60, 44, 70, 255)
    # the brass mouthpiece and the strap
    for x, y in ((1, 13), (2, 13), (1, 14), (2, 12)):
        img[y, x] = (*koper, 255)
    for x, y in ((5, 6), (6, 5), (7, 4), (8, 4), (9, 5)):
        if img[y, x, 3] == 0:
            img[y, x] = (*leer, 255)
    # a soft highlight
    for x, y in ((11, 4), (10, 5)):
        img[y, x] = (255, 252, 240, 255)
    return Image.fromarray(img)


# =====================================================================================================================
NODIG = {SCHIP: ["vaar", "sputter"]}


def maak():
    atlas = zm.Vel("reizen_zuka_schip", bd.S)
    model = zuka_schip(atlas)
    return model, zuka_schip_anims(), Image.fromarray(atlas.img), Image.fromarray(atlas.glow)


def build(h):
    model, anims, tex, glow = maak()
    h.w(f"{h.A}/geckolib/models/entity/{SCHIP}.geo.json", model)
    h.w(f"{h.A}/geckolib/animations/entity/{SCHIP}.animation.json", anims)
    h.save(tex, "entity", f"{SCHIP}.png")
    h.save(glow, "entity", f"{SCHIP}_glowmask.png")
    h.save(walvishoorn(), "item", "einde2_reizen_walvishoorn.png")
    for kant, img in appa_platform().items():
        h.save(img, "block", f"einde2_reizen_appa_platform_{kant}.png")


PLATFORM_HOUT, PLATFORM_HOUT_DONKER = (150, 104, 62), (112, 74, 42)
PLATFORM_VACHT, PLATFORM_BLAUW, PLATFORM_HOOI = (238, 230, 214), (64, 140, 214), (196, 160, 84)


def appa_platform():
    """fix4-appa: the three faces of the Appa-platform block (16 x 16, fixed seed)."""
    import random
    rng = random.Random(4815)

    def ruis(kleur, var):
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                v = rng.randint(-var, var)
                img.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in kleur) + (255,))
        return img

    top = ruis(PLATFORM_VACHT, 6)
    for i in range(16):                                   # the wooden frame
        for c in ((i, 0), (i, 15), (0, i), (15, i)):
            top.putpixel(c, PLATFORM_HOUT_DONKER + (255,))
        for c in ((i, 1), (i, 14), (1, i), (14, i)):
            if 1 <= i <= 14:
                top.putpixel(c, PLATFORM_HOUT + (255,))
    # the arrow of the luchtnomaden: a shaft with a head pointing up (north)
    for y in range(7, 13):
        for x in (7, 8):
            top.putpixel((x, y), PLATFORM_BLAUW + (255,))
    for i, y in enumerate(range(3, 8)):
        for x in range(7 - i, 9 + i):
            if 3 <= x <= 12:
                top.putpixel((x, y), PLATFORM_BLAUW + (255,))
    zij = ruis(PLATFORM_HOOI, 8)
    for y in (4, 9, 15):                                  # plank seams
        for x in range(16):
            zij.putpixel((x, y), PLATFORM_HOUT_DONKER + (255,))
    for x in range(16):                                   # the blue band
        for y in (1, 2):
            zij.putpixel((x, y), PLATFORM_BLAUW + (255,))
    for hx in (4, 11):                                    # two little hoof prints
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, -1), (2, -1)):
            zij.putpixel((hx + dx, 11 + dy), PLATFORM_HOUT_DONKER + (255,))
    onder = ruis(PLATFORM_HOUT, 7)
    for y in (5, 10):
        for x in range(16):
            onder.putpixel((x, y), PLATFORM_HOUT_DONKER + (255,))
    return {"top": top, "zij": zij, "onder": onder}


def check(h):
    problems = []
    for name, wanted in NODIG.items():
        g = json.load(open(f"{h.A}/geckolib/models/entity/{name}.geo.json", encoding="utf-8"))["minecraft:geometry"][0]
        names = {b["name"] for b in g["bones"]}
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names:
                problems.append(f"{name}: bone {b['name']} has a missing parent {b['parent']}")
        anims = json.load(open(f"{h.A}/geckolib/animations/entity/{name}.animation.json", encoding="utf-8"))["animations"]
        for an in wanted:
            if an not in anims:
                problems.append(f"{name}: no animation {an}")
        for an, a in anims.items():
            for bn in a.get("bones", {}):
                if bn not in names:
                    problems.append(f"{name}: animation {an} moves a missing bone {bn}")
        for f in (f"{name}.png", f"{name}_glowmask.png"):
            if not os.path.exists(f"{h.A}/textures/entity/{f}"):
                problems.append(f"{name}: no texture {f}")
    return problems


def preview(out):
    """Offline renders into out/: the ship from three sides, the furnished whale, the horn (8x)."""
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)

    def teken(naam, model, tex, hoeken):
        pad = os.path.join(out, f"_{naam}.geo.json")
        json.dump(model, open(pad, "w"))
        q = wr.geo_quads(pad, np.asarray(tex).astype(np.float32))
        os.remove(pad)
        for yaw, pitch in hoeken:
            wr.render(q, yaw, pitch, 420, margin=0.06).save(os.path.join(out, f"{naam}_{yaw}_{pitch}.png"))
    model, anims, tex, glow = maak()
    tex.save(os.path.join(out, f"{SCHIP}_tex.png"))
    teken(SCHIP, model, tex, ((205, -18), (25, -22), (90, -10)))
    w_model, _, w_tex, _ = zm.maak("leegtewalvisguh")
    teken("walvis_ingericht", w_model, w_tex, ((205, -40), (25, -35), (180, -70)))
    walvishoorn().resize((128, 128), Image.NEAREST).save(os.path.join(out, "walvishoorn.png"))


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
