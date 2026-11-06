"""T-MODELS: an offline GeckoLib -> PNG previewer for the Vadsrijk mobs (before/after contact sheets, no client needed).

It uses the shared software renderer of the render pipeline (renders-mounts/_bron/mountkit.py + base-ontwerpen/_bron/mcrender.py,
outside the repo; memory "render-pipeline"). Glowmasks are added as an additive bloom pass, like the in-game GloedLaag.

    python tools/vadsrijk/model_voorbeeld.py <voor-map> <na-map> <uit.png> <zone> [namen...]
      voor-map / na-map: an "assets/guhs" folder (or a copy of it); zone: wezens | zone2/wezens | zone3/wezens
    python tools/vadsrijk/model_voorbeeld.py los <assets> <geo-rel> <png-rel> <uit.png> [anim-rel animatie t]
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, r"C:\Users\Juiced\Documents\MineScapeStory\renders-mounts\_bron")
import mountkit as K  # noqa: E402
import mcrender as R  # noqa: E402

LUCHT = ((214, 228, 248), (250, 236, 246))
INKT = (70, 40, 80)


def pose(anim_pad, naam, t):
    """The pose of an animation at time t (nearest keyframe per channel, like the draak check renders)."""
    if not anim_pad or not os.path.exists(anim_pad):
        return {}
    an = json.load(open(anim_pad, encoding="utf-8"))["animations"].get(naam)
    if not an:
        return {}
    p = {}
    for b, kan in an["bones"].items():
        for ch, pts in kan.items():
            if not isinstance(pts, dict):
                pts = {"0": pts}
            ts = sorted(pts, key=float)
            k = min(ts, key=lambda x: abs(float(x) - t))
            v = pts[k]
            if isinstance(v, dict):
                v = v.get("vector", v.get("post", [0, 0, 0]))
            key = "rot" if ch == "rotation" else "pos" if ch == "position" else "scale"
            if key != "scale":
                p.setdefault(b, {})[key] = v
    return p


def _gloed(im, cam, gq, sterkte=1.0, ss=2):
    r, bk = K.res()
    vol, pal = np.zeros((1, 1, 1), np.int32), ["minecraft:air"]
    gl, _ = R.render(vol, pal, cam, r, bk, extras=gq, margin=40 * ss)
    g = np.asarray(gl, np.float32)[..., :3]
    g = np.where(g.max(axis=2, keepdims=True) > 30, g, 0)
    gi = Image.fromarray(np.clip(g, 0, 255).astype(np.uint8))
    b1 = np.asarray(gi.filter(ImageFilter.GaussianBlur(3 * ss)), np.float32)
    b2 = np.asarray(gi.filter(ImageFilter.GaussianBlur(9 * ss)), np.float32)
    base = np.asarray(im, np.float32)
    rgb = base[..., :3] + g * 0.9 + (b1 * 0.6 + b2 * 0.9) * sterkte
    al = np.maximum(base[..., 3], np.clip(b2.max(axis=2) * 2 * sterkte, 0, 255))
    return Image.fromarray(np.dstack([np.clip(rgb, 0, 255), al]).astype(np.uint8))


def render(geo, png, gloed=None, p=None, yaw=215, pitch=18, schaal=None, px=150, ss=2):
    """One model as an RGBA image (cropped). schaal None = fit: px is then roughly the size of the model in pixels."""
    m = K.laad(geo, png)
    if schaal is None:
        g = m["g"]["description"]
        schaal = px / max(1.0, 16.0 * max(g.get("visible_bounds_width", 2), g.get("visible_bounds_height", 2)) / 16.0) / 16.0
    base = K.base_mat((0, 0, 0), yaw, 1.0)
    q = K.quads(m, base, p or {})
    r, bk = K.res()
    cam = R.Camera(yaw * 0 + 30, pitch, schaal * 16 * ss)
    vol, pal = np.zeros((1, 1, 1), np.int32), ["minecraft:air"]
    im, _ = R.render(vol, pal, cam, r, bk, extras=q, margin=40 * ss)
    if gloed and os.path.exists(gloed):
        gm = K.laad(geo, gloed)
        im = _gloed(im, cam, K.quads(gm, base, p or {}), ss=ss)
    im = im.resize((im.width // ss, im.height // ss), Image.LANCZOS)
    a = np.asarray(im.split()[3])
    ys, xs = np.nonzero(a > 8)
    if len(xs):
        im = im.crop((max(0, xs.min() - 12), max(0, ys.min() - 12), min(im.width, xs.max() + 12), min(im.height, ys.max() + 12)))
    return im


def tegel(ims, labels, titel=None, sky=LUCHT, gat=24):
    lp = 22
    H = max(i.height for i in ims)
    W = sum(i.width for i in ims) + gat * (len(ims) + 1)
    if titel:
        W = max(W, int(K.font(28).getlength(titel)) + 2 * gat)
    boven = 44 if titel else 8
    bg = K.gradient(W, H + boven + 34, *sky)
    d = ImageDraw.Draw(bg)
    x = gat
    for k, i in enumerate(ims):
        sh = Image.new("RGBA", i.size, (60, 40, 80, 0))
        sh.putalpha(i.split()[3].filter(ImageFilter.GaussianBlur(8)).point(lambda v: int(v * 0.25)))
        y = boven + (H - i.height) // 2
        bg.paste(sh, (x + 5, y + 8), sh)
        bg.paste(i, (x, y), i)
        f = K.font(lp)
        w = d.textlength(labels[k], font=f)
        d.text((x + i.width / 2 - w / 2, boven + H + 4), labels[k], font=f, fill=INKT, stroke_width=3, stroke_fill=(255, 255, 255))
        x += i.width + gat
    if titel:
        d.text((gat, 8), titel, font=K.font(28), fill=INKT, stroke_width=3, stroke_fill=(255, 255, 255))
    return bg


def onder_elkaar(rijen, sky=LUCHT):
    W = max(i.width for i in rijen)
    bg = K.gradient(W, sum(i.height for i in rijen), *sky)
    y = 0
    for i in rijen:
        bg.paste(i.convert("RGB"), (0, y))
        y += i.height
    return bg


def paden(assets, zone, naam):
    geo = os.path.join(assets, "geckolib", "models", "entity", "vadsrijk", *zone.split("/"), naam + ".geo.json")
    an = os.path.join(assets, "geckolib", "animations", "entity", "vadsrijk", *zone.split("/"), naam + ".animation.json")
    tex = os.path.join(assets, "textures", "vadsrijk", *zone.split("/"))
    return geo, an, tex


def kubussen(geo):
    g = json.load(open(geo, encoding="utf-8"))["minecraft:geometry"][0]
    return sum(len(b.get("cubes", [])) for b in g["bones"]), len(g["bones"])


def vergelijk(voor, na, uit, zone, namen, tex_van=None, px=170):
    """Per mob: before (grey) | after (grey) | after (blij, if any). Returns {naam: (cubes before, cubes after)}."""
    rijen, tellen = [], {}
    for naam in namen:
        tn = (tex_van or {}).get(naam, naam)
        gv, av, tv = paden(voor, zone, (tex_van or {}).get(naam + "@geo", naam))
        gn, an, tn_ = paden(na, zone, (tex_van or {}).get(naam + "@geo", naam))
        ims, lab = [], []
        if os.path.exists(gv):
            ims.append(render(gv, os.path.join(tv, tn + ".png"), os.path.join(tv, tn + "_gloed.png"), pose(av, "idle", 0), px=px))
            lab.append("voor")
        ims.append(render(gn, os.path.join(tn_, tn + ".png"), os.path.join(tn_, tn + "_gloed.png"), pose(an, "idle", 0), px=px))
        lab.append("na")
        if os.path.exists(os.path.join(tn_, tn + "_blij.png")):
            ims.append(render(gn, os.path.join(tn_, tn + "_blij.png"), None, pose(an, "idle", 0), px=px))
            lab.append("na, opgevrolijkt")
        ims.append(render(gn, os.path.join(tn_, tn + ".png"), os.path.join(tn_, tn + "_gloed.png"), pose(an, "idle", 0), yaw=120, px=px))
        lab.append("na, zij")
        cv = kubussen(gv)[0] if os.path.exists(gv) else 0
        cn = kubussen(gn)[0]
        tellen[naam] = (cv, cn)
        rijen.append(tegel(ims, lab, f"{naam}   ({cv} -> {cn} kubussen)"))
    im = onder_elkaar(rijen)
    os.makedirs(os.path.dirname(uit), exist_ok=True)
    im.convert("RGB").save(uit)
    print("->", uit, im.size)
    return tellen


if __name__ == "__main__":
    a = sys.argv[1:]
    if a[0] == "los":
        assets, geo, png, uit = a[1:5]
        p = pose(os.path.join(assets, a[5]), a[6], float(a[7])) if len(a) > 7 else {}
        render(os.path.join(assets, geo), os.path.join(assets, png), None, p).save(uit)
    else:
        voor, na, uit, zone = a[:4]
        namen = a[4:]
        vergelijk(voor, na, uit, zone, namen)
