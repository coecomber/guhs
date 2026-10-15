"""
1.4.2 Het Knabbelpark - the looks:

  hertguhtje       a guh variant (the normal guh model and texture): warm caramel fur with cream fawn spots on its back
                   and sides, a cream belly, chin and tail tip, darker ear rims, and two small branched antlers (variant
                   bones hert_gewei_*; they add no swatch, like biomes3: they sample the koning_manen swatch, which only
                   our texture paints in antler colours)
  statues          textures/entity/beeld_<soort>.png: the guh texture turned into stone or bronze (the shading of the fur
                   stays), with the look of the character painted on in the same material:
                     joostvadsje  bronze, a bow tie (he is a vadsje: the renderer draws him extra round)
                     trapguh      bronze, small (he sits beside Joostvadsje, on the step of the pedestal)
                     liekguh      white stone, with her little golden nose ring
                     suusvadsje   bronze, a bow on her head (a vadsje: drawn extra round, trapguh between her and Joostvadsje)
                     lunde        white stone, a heart on the forehead
                     opa_kaasguh  old bronze with green patina, round glasses and a moustache (on his kaasje)
  blocks           park_bankje, park_lantaarn, park_prullenbak, park_plaquette, park_beeld (the plinth; the cheese plinth
                   for Opa Kaasguh), park_eigen_beeld: models (vanilla textures where they fit) and three small textures
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw

from features import guhpolder_tex as gt

# --- the Hertguhtje ---------------------------------------------------------------------------------------------------------
_H = [0, 6, -2]
SWATCH_GEWEI = "koning_manen"
HERT_VACHT = (196, 136, 92)
HERT_ROOM = (246, 230, 206)
HERT_DONKER = (122, 80, 54)
GEWEI = (120, 86, 60)


def _gewei(links):
    """One small antler: a stem, a tine to the front, a branch out and up (mirrored for the other side)."""
    s = 1 if links else -1

    def c(x, y, z, w, hh, d):
        x0 = x if s > 0 else -x - w
        return ([round(x0, 2), y, z], [w, hh, d], 0)
    return [c(2.0, 15.0, -6.4, 0.8, 2.6, 0.8), c(2.0, 16.4, -7.6, 0.8, 0.7, 1.3), c(2.6, 17.3, -6.4, 1.4, 0.7, 0.8),
            c(3.4, 17.3, -6.4, 0.7, 1.7, 0.8), c(1.6, 17.6, -6.4, 0.6, 1.2, 0.8)]


BONES = {
    "hert_gewei_links": ("head", _H, SWATCH_GEWEI, _gewei(True)),
    "hert_gewei_rechts": ("head", _H, SWATCH_GEWEI, _gewei(False)),
}


def _gewei_swatch(v, rng):
    a = v.fabric(GEWEI, rng, 8)
    px = a.shape[0]
    for y in range(px):
        a[y, :] = a[y, :] * (0.85 + 0.25 * y / px)          # lighter towards the tips
    for _ in range(18):
        x, y = rng.integers(0, px - 1, 2)
        a[y, x] = (168, 130, 96)
    return np.clip(a, 0, 255)


def variants(v, rng):
    """The first texture (tools/make_guh_variants.py); hertguhtje(...) below paints the real one."""
    return {"hertguhtje": (HERT_VACHT, {SWATCH_GEWEI: lambda: _gewei_swatch(v, rng)})}


def _guh_geo(h):
    import json
    return json.load(open(os.path.join(h.A, "geckolib", "models", "entity", "guh.geo.json"), encoding="utf-8"))["minecraft:geometry"][0]


def hertguhtje(base, g, v, rng):
    p = gt.Painter(base, HERT_VACHT, fluff=0.03)
    body = gt._faces(g, "body")
    for c in body:
        p.rect(c["down"], HERT_ROOM)
        p.rect(c["north"], HERT_ROOM, rows=(0.45, 1.0))
        for side in ("east", "west"):
            p.rect(c[side], HERT_ROOM, rows=(0.75, 1.0))
    # the fawn spots: little cream ovals on the back and the upper sides, in rows
    for c in body:
        for face, rij in (("up", (0.1, 0.9)), ("east", (0.1, 0.55)), ("west", (0.1, 0.55))):
            r = c[face]
            for i in range(14 if face == "up" else 7):
                cx, cy = rng.uniform(0.1, 0.9), rng.uniform(*rij)
                p.ellipse(r, cx, cy, rng.uniform(0.035, 0.06), rng.uniform(0.04, 0.07), HERT_ROOM)
    head = gt._faces(g, "head")
    front = min((c for c in head if c["_size"][0] >= 10), key=lambda c: c["_origin"][2])
    p.ellipse(front["north"], 0.5, 0.92, 0.32, 0.16, HERT_ROOM)          # the cream chin
    p.ellipse(front["north"], 0.5, 0.04, 0.1, 0.12, HERT_ROOM)           # a little blaze
    for ear in ("ear_left", "ear_right"):
        for c in gt._faces(g, ear):
            for k, fr in c.items():
                if not k.startswith("_") and k in ("east", "west", "up"):
                    p.rect(fr, HERT_DONKER, rows=(0.0, 0.35))
    for c in gt._faces(g, "tail"):
        for k, fr in c.items():
            if not k.startswith("_"):
                p.rect(fr, HERT_ROOM, anything=True)
    a = p.a
    rect = _swatch_rect(g, "hert_gewei_links")
    if rect:
        _plak(a, rect, _gewei_swatch(v, rng))
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def _swatch_rect(g, bone):
    cubes = gt._faces(g, bone)
    return cubes[0]["north"] if cubes else None


def _plak(a, rect, swatch):
    x0, y0, x1, y1 = rect
    s = np.asarray(Image.fromarray(np.clip(swatch, 0, 255).astype(np.uint8)).resize((x1 - x0, y1 - y0), Image.NEAREST)).astype(np.int32)
    a[y0:y1, x0:x1, :3] = s[..., :3]
    a[y0:y1, x0:x1, 3] = 255


# --- the statues ------------------------------------------------------------------------------------------------------------
MATERIAAL = {
    "brons": [(70, 44, 24), (148, 98, 52), (214, 156, 92), (246, 206, 140)],
    "oud_brons": [(54, 46, 30), (120, 96, 58), (176, 150, 96), (214, 200, 150)],
    "steen": [(118, 116, 122), (176, 174, 180), (222, 220, 224), (246, 244, 246)],
}
BEELDEN = {"joostvadsje": "brons", "trapguh": "brons", "liekguh": "steen", "lunde": "steen", "opa_kaasguh": "oud_brons", "suusvadsje": "brons"}
GOUD = (250, 210, 90)


def _ramp(lum, kleuren):
    """lum 0..1 -> a colour on the ramp (array)."""
    k = np.array(kleuren, np.float32)
    t = np.clip(lum, 0, 1) * (len(k) - 1)
    i = np.clip(t.astype(int), 0, len(k) - 2)
    f = (t - i)[..., None]
    return k[i] * (1 - f) + k[i + 1] * f


def beeld_textuur(base, g, soort, rng):
    a = np.asarray(base.convert("RGBA")).astype(np.float32)
    lum = (a[..., 0] * 0.3 + a[..., 1] * 0.55 + a[..., 2] * 0.15) / 255
    lum = 0.15 + 0.85 * (lum - lum[a[..., 3] > 0].min()) / max(1e-3, np.ptp(lum[a[..., 3] > 0]))
    kleur = _ramp(lum, MATERIAAL[BEELDEN[soort]])
    kleur += rng.normal(0, 4, kleur.shape)                               # a little grain of the stone/metal
    out = np.concatenate([np.clip(kleur, 0, 255), a[..., 3:4]], axis=-1)
    img = Image.fromarray(out.astype(np.uint8))
    d = ImageDraw.Draw(img)
    donker = tuple(int(c) for c in MATERIAAL[BEELDEN[soort]][0])
    licht = tuple(int(c) for c in MATERIAAL[BEELDEN[soort]][3])
    head = gt._faces(g, "head")
    front = min((c for c in head if c["_size"][0] >= 10), key=lambda c: c["_origin"][2])["north"]
    fx0, fy0, fx1, fy1 = front
    fw, fh = fx1 - fx0, fy1 - fy0
    snoet = min(head, key=lambda c: c["_size"][0] * c["_size"][1])["north"]
    if soort == "joostvadsje":
        b = gt._faces(g, "body")[0]["north"]
        cx, cy = (b[0] + b[2]) // 2, b[1] + (b[3] - b[1]) // 5
        d.polygon([(cx, cy), (cx - 12, cy - 7), (cx - 12, cy + 7)], fill=donker)
        d.polygon([(cx, cy), (cx + 12, cy - 7), (cx + 12, cy + 7)], fill=donker)
        d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=licht)
    elif soort == "suusvadsje":
        cx, cy = fx0 + fw // 2, fy0 + 2                                         # (a bow on her head)
        d.polygon([(cx, cy), (cx - 9, cy - 5), (cx - 9, cy + 5)], fill=donker)
        d.polygon([(cx, cy), (cx + 9, cy - 5), (cx + 9, cy + 5)], fill=donker)
        d.ellipse([cx - 2, cy - 2, cx + 2, cy + 2], fill=licht)
    elif soort == "liekguh":
        sx0, sy0, sx1, sy1 = snoet
        cx = (sx0 + sx1) // 2
        d.ellipse([cx - 4, sy1 - 5, cx + 4, sy1 + 3], outline=GOUD, width=2)  # (the nose ring)
    elif soort == "lunde":
        cx, cy = fx0 + fw // 2, fy0 + fh // 9
        d.polygon([(cx, cy + 6), (cx - 6, cy), (cx - 3, cy - 3), (cx, cy - 1), (cx + 3, cy - 3), (cx + 6, cy)], fill=licht)
    elif soort == "opa_kaasguh":
        import make_sleep_eyes as mse
        ogen = mse.eye_boxes(base.convert("RGBA"), mse.fur_colour(base.convert("RGBA")))
        for (x0, y0, x1, y1) in ogen:
            d.ellipse([x0 - 4, y0 - 4, x1 + 4, y1 + 4], outline=donker, width=3)
        if len(ogen) == 2:
            d.line([(ogen[0][2] + 4, (ogen[0][1] + ogen[0][3]) // 2), (ogen[1][0] - 4, (ogen[1][1] + ogen[1][3]) // 2)], fill=donker, width=3)
        cx, cy = fx0 + fw // 2, fy0 + int(fh * 0.78)
        for k in (-1, 1):
            d.ellipse([cx + k * 2 - (12 if k < 0 else 0), cy - 3, cx + k * 2 + (12 if k > 0 else 0), cy + 4], fill=donker)
        # patina: soft green spots
        arr = np.asarray(img).astype(np.int32)
        for _ in range(60):
            y, x = rng.integers(0, arr.shape[0]), rng.integers(0, arr.shape[1])
            if arr[y, x, 3] > 0:
                arr[max(0, y - 2):y + 3, max(0, x - 2):x + 3, :3] = (arr[max(0, y - 2):y + 3, max(0, x - 2):x + 3, :3] * 0.6
                                                                    + np.array((96, 150, 120)) * 0.4).astype(np.int32)
        img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))
    return img


# --- the block textures and models ---------------------------------------------------------------------------------------
def _tex(h, naam, rijen, pal):
    h.save(h.grid(rijen, pal), "block", f"{naam}.png")


def textures(h):
    pal = {".": (0, 0, 0, 0), "p": (246, 176, 204, 255), "P": (226, 132, 172, 255), "w": (255, 238, 244, 255), "k": (46, 30, 44, 255),
           "b": (255, 150, 170, 255), "y": (255, 236, 190, 255), "Y": (255, 214, 140, 255), "m": (194, 96, 136, 255), "o": (150, 70, 104, 255)}
    # the lantern's glowing body with a guh face (front), and plain glow (sides)
    _tex(h, "park_lantaarn_gezicht", [
        "yyyyyyyyyyyyyyyy", "yYYYYYYYYYYYYYYy", "yYyyyyyyyyyyyyYy", "yYyykkyyyykkyyYy", "yYyykwyyyykwyyYy", "yYyykkyyyykkyyYy",
        "yYybbyyyyyybbyYy", "yYyyyyykkyyyyyYy", "yYyyyyyykyyyyyYy", "yYyyyyyyyyyyyyYy", "yYyyyyyyyyyyyyYy", "yYyyyyyyyyyyyyYy",
        "yYyyyyyyyyyyyyYy", "yYyyyyyyyyyyyyYy", "yYYYYYYYYYYYYYYy", "yyyyyyyyyyyyyyyy"], pal)
    _tex(h, "park_lantaarn_licht", ["yYyyyyyyyyyyyyYy" if i % 5 else "yYYYYYYYYYYYYYYy" for i in range(16)], pal)
    _tex(h, "park_lantaarn_kap", ["mmmmmmmmmmmmmmmm", "mPPPPPPPPPPPPPPm"] + ["mPppppppppppppPm"] * 12 + ["mPPPPPPPPPPPPPPm", "mmmmmmmmmmmmmmmm"], pal)
    # the litter bin: pink ribbed metal, a guh face with an open mouth on the front
    rib = ["pPppPppPppPppPpp"] * 16
    _tex(h, "park_prullenbak", rib, pal)
    _tex(h, "park_prullenbak_gezicht", [
        "pPppPppPppPppPpp", "pPppPppPppPppPpp", "pPkkPppPppPkkPpp", "pPkwkppPppkwkPpp", "pPkkPppPppPkkPpp", "pbbpPppPppPpbbpp",
        "pPppPppPppPppPpp", "pPppPokkkkoPpPpp", "pPppPkooookPpPpp", "pPppPkobbokPpPpp", "pPppPokkkkoPpPpp", "pPppPppPppPppPpp",
        "pPppPppPppPppPpp", "pPppPppPppPppPpp", "mmmmmmmmmmmmmmmm", "oooooooooooooooo"], pal)
    _tex(h, "park_prullenbak_top", ["mmmmmmmmmmmmmmmm", "mPPPPPPPPPPPPPPm"] + ["mPkkkkkkkkkkkkPm"] * 12 + ["mPPPPPPPPPPPPPPm", "mmmmmmmmmmmmmmmm"], pal)
    # the plaque: bronze with engraved lines
    br = {".": (0, 0, 0, 0), "a": (120, 78, 40, 255), "b": (176, 120, 64, 255), "c": (214, 160, 94, 255), "d": (98, 62, 32, 255)}
    _tex(h, "park_plaquette", ["dddddddddddddddd", "dccccccccccccccd", "dcbbbbbbbbbbbbcd", "dcbaaaaaaaaaabcd", "dcbbbbbbbbbbbbcd",
                               "dcbaaaaaaaaabbcd", "dcbbbbbbbbbbbbcd", "dcbaaaaaaaaaabcd", "dcbbbbbbbbbbbbcd", "dcbaaaaaabbbbbcd",
                               "dcbbbbbbbbbbbbcd", "dcbbbbbaabbbbbcd", "dcbbbbaaaabbbbcd", "dcbbbbbbbbbbbbcd", "dccccccccccccccd",
                               "dddddddddddddddd"], br)


def _el(frm, to, tex, rot=None, uv=None):
    e = {"from": frm, "to": to, "faces": {f: {"texture": tex} for f in ("down", "up", "north", "south", "west", "east")}}
    if rot:
        e["rotation"] = rot
    if uv:
        for f in e["faces"]:
            e["faces"][f]["uv"] = uv
    return e


DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
           "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
           "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
           "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
           "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]}}


def models(h):
    A, w = h.A, h.w

    def model(naam, elementen, tex, item=True, facing=True, render=None):
        m = {"parent": "minecraft:block/block", "textures": {"particle": next(iter(tex.values())), **tex}, "elements": elementen, "display": DISPLAY}
        if render:
            m["render_type"] = render
        w(f"{A}/models/block/{naam}.json", m)
        if facing:
            w(f"{A}/blockstates/{naam}.json", {"variants": h.facing_states(naam)})
        else:
            w(f"{A}/blockstates/{naam}.json", {"variants": {"": {"model": f"guhs:block/{naam}"}}})
        if item:
            w(f"{A}/models/item/{naam}.json", {"parent": f"guhs:block/{naam}"})

    # the bench (facing north = you look north when you sit: the backrest at the south side), two round ears on the back
    model("park_bankje", [
        _el([1, 5, 3], [15, 7, 12], "#zit"), _el([1, 7, 11], [15, 14, 13], "#zit"),
        _el([2, 0, 4], [4, 5, 11], "#poot"), _el([12, 0, 4], [14, 5, 11], "#poot"),
        _el([2, 13, 11.5], [5, 16, 12.5], "#oor"), _el([11, 13, 11.5], [14, 16, 12.5], "#oor")],
        {"zit": "minecraft:block/cherry_planks", "poot": "minecraft:block/stripped_cherry_log", "oor": "minecraft:block/pink_wool"})
    # the lantern: a glowing round body with a face, a cap and two ears
    lamp = [_el([4, 0, 4], [12, 1, 12], "#kap"), _el([4.5, 1, 4.5], [11.5, 8, 11.5], "#licht"), _el([4, 8, 4], [12, 9, 12], "#kap"),
            _el([3.5, 9, 6.5], [6, 11.5, 9.5], "#kap"), _el([10, 9, 6.5], [12.5, 11.5, 9.5], "#kap")]
    for e in lamp[1:2]:
        e["faces"]["north"]["texture"] = "#gezicht"
        e["faces"]["south"]["texture"] = "#gezicht"
        e["light_emission"] = 15
    model("park_lantaarn", lamp, {"licht": "guhs:block/park_lantaarn_licht", "gezicht": "guhs:block/park_lantaarn_gezicht",
                                  "kap": "guhs:block/park_lantaarn_kap"}, facing=False)
    # the litter bin: the body with its face to the front (north), the rim, two ears
    bak = [_el([3, 0, 3], [13, 13, 13], "#zij"), _el([2, 13, 2], [14, 15, 14], "#top"),
           _el([3, 15, 7], [5.5, 17, 9], "#zij"), _el([10.5, 15, 7], [13, 17, 9], "#zij")]
    bak[0]["faces"]["north"]["texture"] = "#gezicht"
    bak[1]["faces"]["up"]["texture"] = "#top"
    model("park_prullenbak", bak, {"zij": "guhs:block/park_prullenbak", "gezicht": "guhs:block/park_prullenbak_gezicht",
                                   "top": "guhs:block/park_prullenbak_top"})
    # the plaque on its little post, tilted back
    schuin = {"origin": [8, 11.5, 7], "axis": "x", "angle": -22.5}
    plaat = [_el([7, 0, 7], [9, 10, 9], "#paal"), _el([2, 8, 6], [14, 15, 8], "#brons", rot=schuin)]
    w(f"{A}/models/block/park_plaquette.json", {"parent": "minecraft:block/block", "textures": {
        "particle": "guhs:block/park_plaquette", "paal": "minecraft:block/dark_oak_log", "brons": "guhs:block/park_plaquette"},
        "elements": plaat})
    w(f"{A}/blockstates/park_plaquette.json", {"variants": {
        f"facing={f},nr={nr}": ({"model": "guhs:block/park_plaquette", **({"y": y} if y else {})})
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for nr in (1, 2, 3)}})
    # the plinths of the statues: stone, a moulded top; Opa Kaasguh's is a big kaasje
    sokkel = [_el([0, 0, 0], [16, 3, 16], "#voet"), _el([2, 3, 2], [14, 8, 14], "#zuil"), _el([1, 8, 1], [15, 10, 15], "#voet")]
    kaas = [_el([0, 0, 0], [16, 3, 16], "#voet"), _el([1, 3, 1], [15, 11, 15], "#kaas")]
    for naam, el in (("park_beeld", sokkel), ("park_beeld_kaas", kaas)):
        w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/block", "textures": {
            "particle": "minecraft:block/polished_andesite", "voet": "minecraft:block/polished_andesite", "zuil": "minecraft:block/smooth_stone",
            "kaas": "guhs:block/gatenkaas"}, "elements": el})
    w(f"{A}/blockstates/park_beeld.json", {"variants": {
        f"beeld={b},facing={f}": ({"model": "guhs:block/" + ("park_beeld_kaas" if b == "opa_kaasguh" else "park_beeld"), **({"y": y} if y else {})})
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for b in BEELDEN}})
    # the own little statue: a small bronze-edged plinth (the guh is drawn on it)
    model("park_eigen_beeld", [_el([3, 0, 3], [13, 2, 13], "#voet"), _el([4, 2, 4], [12, 5, 12], "#rand")],
          {"voet": "minecraft:block/polished_andesite", "rand": "minecraft:block/cut_copper"}, item=False)
    w(f"{A}/models/item/park_eigen_beeld.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/park_eigen_beeld"}})


def eigen_beeld_icoon(h):
    """The item: a little bronze guh on a plinth."""
    pal = {".": (0, 0, 0, 0), "k": (60, 38, 20, 255), "a": (150, 98, 52, 255), "b": (206, 148, 86, 255), "c": (240, 196, 130, 255),
           "s": (120, 122, 128, 255), "S": (170, 172, 178, 255)}
    rows = ["................", "................", "...kk......kk...", "..kbbk....kbbk..", "..kbak....kabk..", "..kbbbkkkkbbbk..",
            "..kbbbbbbbbbbk..", ".kbbkbbbbbbkbbk.", ".kbbbbbbbbbbbbk.", ".kbbbbccccbbbbk.", ".kabbbbbbbbbbak.", "..kaabbbbbbaak..",
            "...kkkkkkkkkk...", "..sSSSSSSSSSSs..", "..ssssssssssss..", "................"]
    h.save(h.grid(rows, pal), "item", "park_eigen_beeld.png")


def build(h):
    """Block textures and models, the Hertguhtje's texture (+ sleeping), the statues' textures. Returns False while the
    Hertguhtje's bones are not in guh.geo.json yet (run tools/make_guh_variants.py first)."""
    import make_guh_variants as v
    from features import bio_dieren_modellen as bm
    textures(h)
    models(h)
    eigen_beeld_icoon(h)
    g = _guh_geo(h)
    names = {b["name"] for b in g["bones"]}
    klaar = all(b in names for b in BONES)
    if not klaar:
        print("park: run tools/make_guh_variants.py first (the Hertguhtje's antlers are not in guh.geo.json yet)")
    base = Image.open(os.path.join(h.TEX, "entity", "guh.png")).convert("RGBA")
    rng = np.random.default_rng(1420201)
    hert = hertguhtje(base, g, v, rng)
    h.save(hert, "entity", "guh_hertguhtje.png")
    h.save(bm.slaap(hert, base, (110, 70, 48, 255)), "entity", "guh_slaap", "guh_hertguhtje.png")
    for soort in BEELDEN:
        h.save(beeld_textuur(base, g, soort, np.random.default_rng(1420210 + list(BEELDEN).index(soort))), "entity", f"beeld_{soort}.png")
    return klaar
