"""
Dierenatelier (safari-voorwerk, fase 0): gedeelde stijlhulpjes voor de safaridieren.

De dieren zijn guh-GEINSPIREERD, geen guh-varianten: echt diersilhouet en echte kleuren, chibi-proporties (te groot
hoofd, korte pootjes), het guh-gezicht (grote glanzende ogen, roze blosje, klein neusje) maar GEEN guh-oortjes.
Hergebruikt de schilders van guhs/tools/features/boerderij_dieren.py (alleen geimporteerd, niet aangepast) en voegt toe:
een atlas van instelbare grootte, patronen (vlekken, ringen, schubben, verloop), oortjes in diervorm, en een
geo -> .bbmodel export (het omgekeerde van piep_bbexport) zodat de modellen in Blockbench open kunnen.

Formaat (gelijk aan de guhs-entity's): <soort>.geo.json (bedrock 1.12.0, texture_width = atlasmaat in modelpixels,
4 texels per pixel), <soort>.png, <soort>.animation.json (1.8.0, idle + walk + blij + eet).
"""
import base64
import io
import json
import math
import os
import sys
import uuid

import numpy as np
from PIL import Image

HIER = os.path.dirname(os.path.abspath(__file__))
GUHS = os.path.abspath(os.path.join(HIER, "..", "..", ".."))          # the repo root (tools/features/safari_atelier/../../..)
sys.path.insert(0, os.path.join(GUHS, "tools"))
from features import boerderij_dieren as bd                            # noqa: E402

S = bd.S                      # 4 texels per modelpixel
plain, disc, guh_face, transparent, bone, face_size = bd.plain, bd.disc, bd.guh_face, bd.transparent, bd.bone, bd.face_size
FACES = bd.FACES
BLOS = (255, 150, 186)


# ---------------------------------------------------------------------------------------------------------------------
# atlas + cube + geo met instelbare atlasmaat
# ---------------------------------------------------------------------------------------------------------------------
class Atlas:
    def __init__(self, seed, size=128):
        self.size = size
        self.img = np.zeros((size * S, size * S, 4), np.uint8)
        self.x = self.y = self.row = 0
        self.rng = np.random.default_rng(seed)

    def alloc(self, w, h):
        w, h = int(math.ceil(w)), int(math.ceil(h))
        if self.x + w > self.size:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        if self.y + h > self.size:
            raise SystemExit("atelier: de atlas is vol, kies een grotere maat")
        u, v = self.x, self.y
        self.x += w
        self.row = max(self.row, h)
        return u, v

    def patch(self, w, h, painter):
        u, v = self.alloc(w, h)
        W, H = int(math.ceil(w)) * S, int(math.ceil(h)) * S
        self.img[v * S:v * S + H, u * S:u * S + W] = painter(W, H, self.rng)
        return {"uv": [u, v], "uv_size": [w, h]}

    def image(self):
        return Image.fromarray(self.img)


def cube(atlas, origin, size, paint, overrides=None, inflate=None):
    uv = {}
    for f in FACES:
        w, h = face_size(f, size)
        uv[f] = atlas.patch(max(w, 0.5), max(h, 0.5), (overrides or {}).get(f, paint))
    c = {"origin": [round(v, 3) for v in origin], "size": [round(v, 3) for v in size], "uv": uv}
    if inflate:
        c["inflate"] = inflate
    return c


def geo(identifier, bones, atlas, width, height):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{identifier}", "texture_width": atlas.size, "texture_height": atlas.size,
                        "visible_bounds_width": width, "visible_bounds_height": height, "visible_bounds_offset": [0, height / 2, 0]},
        "bones": bones}]}


def mirror_x(origin, size):
    """De gespiegelde oorsprong (x -> -x) van een kubus."""
    return [-origin[0] - size[0], origin[1], origin[2]]


# ---------------------------------------------------------------------------------------------------------------------
# schilders
# ---------------------------------------------------------------------------------------------------------------------
def fluffy(colour, var=14):
    """Pluizig: meer ruis en kleine lichte plukjes."""
    def spots(W, H, rng, a):
        for _ in range(W * H // 30):
            x, y = int(rng.uniform(0, W)), int(rng.uniform(0, H))
            a[y, x, :3] = np.clip(np.array(colour) + rng.uniform(10, 26), 0, 255)
    return plain(colour, var, spots)


def lijn_vlekken(base, vlek, cel=9.0, lijn=1.6, seed_off=0):
    """Giraffevlekken: voronoi-cellen in vlekkleur met lichte lijnen ertussen."""
    def paint(W, H, rng):
        a = plain(base, 6)(W, H, rng).astype(np.float32)
        n = max(2, int(W * H / (cel * S) ** 2 * 1.2) + 2)
        pts = np.stack([rng.uniform(-cel, W + cel, n), rng.uniform(-cel, H + cel, n)], 1)
        yy, xx = np.mgrid[0:H, 0:W] + 0.5
        d = np.sqrt((xx[..., None] - pts[:, 0]) ** 2 + (yy[..., None] - pts[:, 1]) ** 2)
        d.sort(axis=2)
        edge = (d[..., 1] - d[..., 0]) / 2
        m = edge > lijn * S / 2
        shade = rng.normal(0, 6, (H, W, 1))
        a[m, :3] = (np.array(vlek) + shade[m])
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def ringen(base, ring, langs="W", n=4, var=8):
    """Ringstaart: banden dwars op de lengterichting (langs = 'W' of 'H' van het vlak)."""
    def paint(W, H, rng):
        a = plain(base, var)(W, H, rng).astype(np.float32)
        L = W if langs == "W" else H
        for i in range(n):
            lo, hi = int(L * (i + 0.55) / n), int(L * (i + 0.95) / n)
            if langs == "W":
                a[:, lo:hi, :3] = np.array(ring) + rng.normal(0, var / 2, (H, hi - lo, 1))
            else:
                a[lo:hi, :, :3] = np.array(ring) + rng.normal(0, var / 2, (hi - lo, W, 1))
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def schubben(base, lijnkleur):
    """Beverstaart: een ruitjespatroon van schubben."""
    def paint(W, H, rng):
        a = plain(base, 8)(W, H, rng).astype(np.float32)
        step = 2 * S
        for y in range(H):
            for x in range(W):
                if (x + y) % step == 0 or (x - y) % step == 0:
                    a[y, x, :3] = lijnkleur
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def verloop(top, onder, var=6):
    """Kleurverloop van boven naar onder."""
    def paint(W, H, rng):
        t = (np.arange(H) / max(1, H - 1))[:, None, None]
        a = np.zeros((H, W, 4), np.float32)
        a[..., :3] = np.array(top) * (1 - t) + np.array(onder) * t
        a[..., :3] += rng.normal(0, var / 2, (H, W, 1))
        a[..., 3] = 255
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def buik(fur, licht, cx=0.5, cy=0.62, rx=0.38, ry=0.36, var=8):
    """Vacht met een lichte buik- of borstvlek."""
    def spots(W, H, rng, a):
        disc(a, W * cx, H * cy, W * rx, licht, 1.0, ry=H * ry)
    return plain(fur, var, spots)


def met(painter, *extra):
    """Een schilder plus extra spot-functies (W, H, rng, a) eroverheen."""
    def paint(W, H, rng):
        a = painter(W, H, rng).astype(np.float32)
        for f in extra:
            f(W, H, rng, a)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def vorm_oor(fur, inner, punt=0.0, rand=None):
    """Een dierenoor (geen guh-oor): ovaal tot puntig (punt 0..1), doorzichtige hoeken, binnenkant in inner."""
    def paint(W, H, rng):
        a = plain(fur, 8)(W, H, rng).astype(np.float32)
        yy, xx = np.mgrid[0:H, 0:W] + 0.5
        nx, ny = (xx - W / 2) / (W / 2), (yy - H / 2) / (H / 2)
        breed = 1 - punt * np.clip(-ny, 0, 1)                    # smaller naar boven toe
        inside = (nx / np.maximum(breed, 0.15)) ** 2 + ny ** 2 <= 1.02
        a[~inside, 3] = 0
        inn = (nx / np.maximum(breed * 0.62, 0.1)) ** 2 + ((ny - 0.18) / 0.7) ** 2 <= 1
        a[inn & inside, :3] = inner
        if rand is not None:
            ring = inside & ~((nx / np.maximum(breed * 0.82, 0.1)) ** 2 + (ny / 0.86) ** 2 <= 1)
            a[ring, :3] = rand
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def neusje(fur, neus, mond=True):
    """Voorkant van een snuitje: een donker neusje en een lieve mondlijn."""
    def paint(W, H, rng):
        a = plain(fur, 6)(W, H, rng).astype(np.float32)
        disc(a, W / 2, H * 0.32, W * 0.2, neus, 1.0, ry=H * 0.2)
        disc(a, W / 2 - W * 0.06, H * 0.27, W * 0.05, (255, 255, 255), 0.6)
        if mond:
            my = int(H * 0.72)
            for x in range(int(W * 0.3), int(W * 0.7)):
                dy = int(round(abs(x - W / 2) / (W * 0.2)))
                if 0 <= my - dy < H:
                    a[my - dy, x, :3] = np.array(fur) * 0.6
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def gezicht(fur, eye_y=0.42, eye_dx=0.26, eye_r=0.17, snoet=False, blush=True, spots=None):
    """Het guh-gezicht (de grote glimoogjes) op een dierenkop."""
    return guh_face(fur, eye_y=eye_y, eye_dx=eye_dx, eye_r=eye_r, snoet=snoet, blush=blush, spots=spots)


# ---------------------------------------------------------------------------------------------------------------------
# animaties
# ---------------------------------------------------------------------------------------------------------------------
def kf(*pairs):
    return {str(round(t, 3)): v for t, v in pairs}


def animaties(legs4=None, legs2=None, oren=False, staart=None, waggel=False, extra_idle=None, extra_walk=None):
    idle = {"body": {"scale": kf((0, [1, 1, 1]), (1.5, [1.02, 1.03, 1.02]), (3.0, [1, 1, 1]))},
            "head": {"rotation": kf((0, [0, 0, 0]), (1.0, [0, 6, 0]), (2.0, [0, -6, 0]), (3.0, [0, 0, 0]))}}
    if oren:
        idle["ear_left"] = {"rotation": kf((0, [0, 0, 0]), (2.4, [0, 0, 0]), (2.55, [0, 0, -15]), (2.7, [0, 0, 0]))}
        idle["ear_right"] = {"rotation": kf((0, [0, 0, 0]), (1.2, [0, 0, 0]), (1.35, [0, 0, 15]), (1.5, [0, 0, 0]))}
    if staart:
        idle[staart] = {"rotation": kf((0, [0, 0, 0]), (1.0, [0, 12, 0]), (2.0, [0, -12, 0]), (3.0, [0, 0, 0]))}
    idle.update(extra_idle or {})
    L = 0.8
    walk = {"body": {"position": kf((0, [0, 0, 0]), (L / 4, [0, 0.5, 0]), (L / 2, [0, 0, 0]), (3 * L / 4, [0, 0.5, 0]), (L, [0, 0, 0]))}}
    if legs4:
        for i, n in enumerate(legs4):
            s = 26 if i in (0, 3) else -26
            walk[n] = {"rotation": kf((0, [s, 0, 0]), (L / 2, [-s, 0, 0]), (L, [s, 0, 0]))}
    if legs2:
        walk[legs2[0]] = {"rotation": kf((0, [28, 0, 0]), (L / 2, [-28, 0, 0]), (L, [28, 0, 0]))}
        walk[legs2[1]] = {"rotation": kf((0, [-28, 0, 0]), (L / 2, [28, 0, 0]), (L, [-28, 0, 0]))}
    if waggel:
        walk["body"]["rotation"] = kf((0, [0, 0, 7]), (L / 2, [0, 0, -7]), (L, [0, 0, 7]))
    if staart:
        walk[staart] = {"rotation": kf((0, [0, 10, 0]), (L / 2, [0, -10, 0]), (L, [0, 10, 0]))}
    walk.update(extra_walk or {})
    blij = {"body": {"position": kf((0, [0, 0, 0]), (0.15, [0, 2.5, 0]), (0.3, [0, 0, 0]), (0.45, [0, 1.5, 0]), (0.6, [0, 0, 0]))},
            "head": {"rotation": kf((0, [0, 0, 0]), (0.3, [-10, 0, 8]), (0.6, [0, 0, 0]))}}
    eet = {"head": {"rotation": kf((0, [0, 0, 0]), (0.2, [35, 0, 0]), (0.35, [28, 0, 0]), (0.5, [35, 0, 0]), (0.65, [28, 0, 0]), (1.0, [0, 0, 0]))}}
    return {"format_version": "1.8.0", "animations": {
        "idle": {"loop": True, "animation_length": 3.0, "bones": idle},
        "walk": {"loop": True, "animation_length": L, "bones": walk},
        "blij": {"loop": False, "animation_length": 0.6, "bones": blij},
        "eet": {"loop": False, "animation_length": 1.0, "bones": eet}}}


# ---------------------------------------------------------------------------------------------------------------------
# geo -> .bbmodel (Blockbench, bedrock-formaat; het omgekeerde van piep_bbexport.export_entity)
# ---------------------------------------------------------------------------------------------------------------------
def _uid(*parts):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, "safari/" + "/".join(str(p) for p in parts)))


def bbmodel(name, geo_, anims, img):
    g = geo_["minecraft:geometry"][0]
    tw, th = g["description"]["texture_width"], g["description"]["texture_height"]
    elements, groups, nodes = [], [], {}
    for b in g["bones"]:
        gid = _uid(name, "bone", b["name"])
        p = b.get("pivot", [0, 0, 0])
        r = b.get("rotation", [0, 0, 0])
        groups.append({"name": b["name"], "uuid": gid, "export": True, "locked": False, "scope": 0, "selected": False,
                       "visibility": True, "origin": [-p[0], p[1], p[2]], "rotation": [-r[0], -r[1], r[2]],
                       "bedrock_binding": "", "color": 0, "children": [], "reset": False, "shade": True, "mirror_uv": False,
                       "autouv": 0, "isOpen": True})
        kids = []
        for i, c in enumerate(b.get("cubes", [])):
            o, s = c["origin"], c["size"]
            frm = [-(o[0] + s[0]), o[1], o[2]]
            to = [-o[0], o[1] + s[1], o[2] + s[2]]
            faces = {}
            for d, f in c["uv"].items():
                (u, v), (w, h) = f["uv"], f["uv_size"]
                faces[d] = {"uv": [u, v, u + w, v + h], "texture": 0}
            eid = _uid(name, "cube", b["name"], i)
            el = {"name": f"{b['name']}_{i}", "box_uv": False, "render_order": "default", "locked": False, "export": True,
                  "scope": 0, "allow_mirror_modeling": True, "from": frm, "to": to, "autouv": 0, "color": 1,
                  "origin": [-p[0], p[1], p[2]], "type": "cube", "uuid": eid, "faces": faces}
            if c.get("inflate"):
                el["inflate"] = c["inflate"]
            elements.append(el)
            kids.append(eid)
        nodes[b["name"]] = {"uuid": gid, "isOpen": True, "children": kids, "_parent": b.get("parent")}
    outliner = []
    for nm, n in nodes.items():
        par = n.pop("_parent")
        (nodes[par]["children"] if par in nodes else outliner).append(n)
    buf = io.BytesIO()
    img.save(buf, "PNG")
    tex = {"name": f"{name}.png", "relative_path": f"{name}.png", "folder": "", "namespace": "", "id": "0", "scope": 0,
           "width": img.width, "height": img.height, "uv_width": tw, "uv_height": th, "particle": True,
           "render_mode": "default", "render_sides": "auto", "visible": True, "internal": True, "saved": True,
           "uuid": _uid(name, "tex"), "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()}
    banims = []
    bones_by = {b["name"]: _uid(name, "bone", b["name"]) for b in g["bones"]}
    for an, a in anims["animations"].items():
        animators = {}
        for bn, chans in a["bones"].items():
            kfs = []
            for ch, keys in chans.items():
                for t, val in keys.items():
                    kfs.append({"channel": ch, "data_points": [{"x": val[0], "y": val[1], "z": val[2]}],
                                "uuid": _uid(name, an, bn, ch, t), "time": float(t), "color": -1, "interpolation": "linear"})
            animators[bones_by[bn]] = {"name": bn, "type": "bone", "keyframes": kfs}
        banims.append({"uuid": _uid(name, "anim", an), "name": an, "loop": "loop" if a["loop"] else "once", "override": False,
                       "length": a["animation_length"], "snapping": 24, "selected": False, "animators": animators})
    vb = [g["description"]["visible_bounds_width"], g["description"]["visible_bounds_height"], 0]
    return {"meta": {"format_version": "5.0", "model_format": "bedrock", "box_uv": False}, "name": name,
            "model_identifier": name, "visible_box": vb, "resolution": {"width": tw, "height": th},
            "elements": elements, "groups": groups, "outliner": outliner, "textures": [tex], "animations": banims}


# ---------------------------------------------------------------------------------------------------------------------
# wegschrijven
# ---------------------------------------------------------------------------------------------------------------------
def schrijf(map_, naam, geo_, atlas, anims=None):
    os.makedirs(map_, exist_ok=True)
    json.dump(geo_, open(os.path.join(map_, f"{naam}.geo.json"), "w", encoding="utf-8"), indent=1)
    img = atlas.image()
    img.save(os.path.join(map_, f"{naam}.png"))
    if anims is not None:
        json.dump(anims, open(os.path.join(map_, f"{naam}.animation.json"), "w", encoding="utf-8"), indent=1)
    return img
