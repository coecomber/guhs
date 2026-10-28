"""
1.8.0 (Guhdromenland, afronding "droomsfeer"): the dream's feel. Java: feature/droomsfeer (DroomsfeerFeature, DroomGeluid,
client/DroomsfeerClient, client/DroomLucht).
  build(h)    the sky textures (textures/environment/droom_lucht.png: the twilight gradient; droom_maan.png: the sleepy guh
              moon with its pink nightcap) and the texts: the sweet lines of the fade (gui.guhs.droomsfeer.*) and the
              subtitles of the dream sounds (subtitles.guhs.dromen.*). Dutch in nl_nl, English in
              tools/lang/en/c214_droomsfeer.json.
The sounds themselves (sounds.json + assets/guhs/sounds/dromen/) come from tools/dromen_audio_import.py.
Standalone (from the project root): python tools/features/droomsfeer.py
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

# key -> (Dutch, English). The number of lines per fade must match DroomsfeerFeature.REGELS_INSLAPEN / REGELS_WAKKER.
TEKSTEN = {
    "gui.guhs.droomsfeer.inslapen.0": ("Snoes gaapt... jij ook...", "Snoes yawns... so do you..."),
    "gui.guhs.droomsfeer.inslapen.1": ("De maan stopt je zachtjes in...", "The moon tucks you in softly..."),
    "gui.guhs.droomsfeer.inslapen.2": ("Je dommelt weg op een wolkje...", "You drift off on a little cloud..."),
    "gui.guhs.droomsfeer.inslapen.3": ("Een Blub piept welterusten...", "A Blub squeaks goodnight..."),
    "gui.guhs.droomsfeer.inslapen.4": ("Je oogjes worden zo zwaar als knabbels...", "Your little eyes get as heavy as knabbels..."),
    "gui.guhs.droomsfeer.inslapen.5": ("Zzz... njeg... zzz...", "Zzz... njeg... zzz..."),
    "gui.guhs.droomsfeer.wakker.0": ("Goedemorgen, vadsje!", "Good morning, little vads!"),
    "gui.guhs.droomsfeer.wakker.1": ("Je rekt je uit... njeg!", "You have a big stretch... njeg!"),
    "gui.guhs.droomsfeer.wakker.2": ("De droom zwaait je nog even na...", "The dream waves you goodbye..."),
    "gui.guhs.droomsfeer.wakker.3": ("Uitgeslapen en vahoeg!", "Well rested and vahoeg!"),
    # the subtitles of the dream sounds (tools/dromen_audio_import.py: subtitles.guhs.dromen.<id>)
    "subtitles.guhs.dromen.muziek.plein": ("Droommuziek speelt", "Dream music plays"),
    "subtitles.guhs.dromen.muziek.liedjes": ("Slaapliedjesmuziek speelt", "Lullaby music plays"),
    "subtitles.guhs.dromen.muziek.sterren": ("Sterrenmuziek speelt", "Star music plays"),
    "subtitles.guhs.dromen.muziek.herinnering": ("Herinneringsmuziek speelt", "Memory music plays"),
    "subtitles.guhs.dromen.muziek.badje": ("Badmuziekje speelt", "Bath music plays"),
    "subtitles.guhs.dromen.muziek.molentje": ("Molenmuziek speelt", "Mill music plays"),
    "subtitles.guhs.dromen.snoes_praat": ("Snoes praat", "Snoes talks"),
    "subtitles.guhs.dromen.snoes_geeuw": ("Snoes gaapt", "Snoes yawns"),
    "subtitles.guhs.dromen.snoes_blij": ("Snoes piept blij", "Snoes squeaks happily"),
    "subtitles.guhs.dromen.zandguh": ("Zandguh praat", "Sand Guh talks"),
    "subtitles.guhs.dromen.wekkerguh": ("Wekkerguh praat", "Alarm Guh talks"),
    "subtitles.guhs.dromen.rommelguh": ("Rommelguh praat", "Clutter Guh talks"),
    "subtitles.guhs.dromen.oma": ("Oma-guh praat", "Granny Guh talks"),
    "subtitles.guhs.dromen.kussengevecht": ("Kussengevecht-guh lacht", "Pillow-fight guh giggles"),
    "subtitles.guhs.dromen.badmeester": ("Badmeester Bubbels praat", "Bath Master Bubbels talks"),
    "subtitles.guhs.dromen.sterrenvisje": ("Sterrenvisje twinkelt", "Star fish twinkles"),
}

TEX = os.path.join("src", "main", "resources", "assets", "guhs", "textures", "environment")


# ------------------------------------------------------------------------------------------------ the sky gradient
def lucht():
    """64 x 256: v 0 = straight up (deep violet), the horizon (v 0.5) a warm pink, below a soft lavender."""
    stops = [(0.00, (40, 22, 74)), (0.22, (78, 46, 128)), (0.40, (152, 96, 178)), (0.50, (238, 164, 204)),
             (0.56, (246, 196, 214)), (0.70, (184, 154, 230)), (1.00, (120, 100, 190))]
    h, w = 256, 64
    img = np.zeros((h, w, 4), np.uint8)
    for y in range(h):
        v = y / (h - 1)
        for (v0, c0), (v1, c1) in zip(stops, stops[1:]):
            if v0 <= v <= v1:
                f = (v - v0) / (v1 - v0)
                f = f * f * (3 - 2 * f)
                img[y, :, :3] = [round(a + (b - a) * f) for a, b in zip(c0, c1)]
                break
    img[..., 3] = 255
    return Image.fromarray(img)


# ------------------------------------------------------------------------------------------------ the sleepy guh moon
def maan():
    """128 x 128 RGBA: a round cream moon with two little guh ears, closed sleepy eyes, blushes, a tiny :3 mouth and a
    pink nightcap with a pompom (drawn big at 4x and scaled down for soft edges)."""
    S = 4
    n = 128 * S
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy, r = n * 0.5, n * 0.56, n * 0.33
    room = (255, 246, 222, 255)
    rand = (240, 214, 186, 255)
    # the ears (behind the head), cream with a pink inside
    for kant in (-1, 1):
        ox, oy, er = cx + kant * r * 0.68, cy - r * 0.78, r * 0.34
        d.ellipse([ox - er, oy - er, ox + er, oy + er], fill=rand)
        d.ellipse([ox - er * 0.78, oy - er * 0.78, ox + er * 0.78, oy + er * 0.78], fill=room)
        d.ellipse([ox - er * 0.45, oy - er * 0.4, ox + er * 0.45, oy + er * 0.5], fill=(250, 186, 206, 255))
    d.ellipse([cx - r - S * 2, cy - r - S * 2, cx + r + S * 2, cy + r + S * 2], fill=rand)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=room)
    # soft craters (very pale)
    for (fx, fy, fr) in [(-0.45, 0.35, 0.12), (0.5, 0.42, 0.09), (0.2, 0.62, 0.07), (-0.15, 0.7, 0.05)]:
        x, y, rr = cx + fx * r, cy + fy * r, fr * r
        d.ellipse([x - rr, y - rr, x + rr, y + rr], fill=(246, 230, 204, 255))
    # sleepy closed eyes: two soft arcs
    for kant in (-1, 1):
        ex, ey = cx + kant * r * 0.36, cy + r * 0.02
        d.arc([ex - r * 0.17, ey - r * 0.12, ex + r * 0.17, ey + r * 0.12], 15, 165, fill=(86, 54, 82, 255), width=int(S * 3.2))
    # blushes
    for kant in (-1, 1):
        bx, by = cx + kant * r * 0.56, cy + r * 0.26
        d.ellipse([bx - r * 0.14, by - r * 0.08, bx + r * 0.14, by + r * 0.08], fill=(250, 170, 196, 200))
    # the little :3 mouth
    my = cy + r * 0.3
    for kant in (-1, 1):
        mx = cx + kant * r * 0.07
        d.arc([mx - r * 0.07, my - r * 0.06, mx + r * 0.07, my + r * 0.07], 0, 180, fill=(120, 70, 100, 255), width=int(S * 2.4))
    # the pink nightcap, flopping to the right, with stripes and a pompom
    kap = [(cx - r * 0.95, cy - r * 0.45), (cx + r * 0.75, cy - r * 0.62), (cx + r * 1.25, cy - r * 0.05),
           (cx + r * 0.55, cy - r * 1.25), (cx - r * 0.25, cy - r * 1.05)]
    d.polygon([kap[0], kap[4], kap[3], kap[2], kap[1]], fill=(244, 150, 192, 255))
    d.line([kap[3], kap[2]], fill=(255, 196, 220, 255), width=int(S * 6))
    d.line([(cx - r * 0.55, cy - r * 0.78), (cx + r * 0.45, cy - r * 0.95)], fill=(255, 210, 228, 255), width=int(S * 5))
    d.rounded_rectangle([cx - r * 1.02, cy - r * 0.62, cx + r * 0.82, cy - r * 0.4], radius=int(S * 6), fill=(255, 238, 246, 255))
    px, py, pr = cx + r * 1.25, cy - r * 0.02, r * 0.17
    d.ellipse([px - pr, py - pr, px + pr, py + pr], fill=(255, 250, 252, 255))
    # a tiny zzz
    for i, zs in enumerate([0.13, 0.10, 0.08]):
        x, y, s = cx - r * 1.12 + i * r * 0.2, cy - r * 0.95 - i * r * 0.22, zs * r
        d.line([(x, y), (x + s, y), (x, y + s), (x + s, y + s)], fill=(255, 240, 252, 230), width=int(S * 2.2))
    img = img.filter(ImageFilter.GaussianBlur(S * 0.4))
    return img.resize((128, 128), Image.LANCZOS)


def build(h):
    os.makedirs(TEX, exist_ok=True)
    lucht().save(os.path.join(TEX, "droom_lucht.png"))
    maan().save(os.path.join(TEX, "droom_maan.png"))
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def _direct():
    """Standalone: only its own files (nl_nl gets the Dutch texts, en_us the English ones; the English also goes into
    tools/lang/en/c214_droomsfeer.json so a full make_resources run keeps it)."""
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c214_droomsfeer.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"droomsfeer: sky + moon textures, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
