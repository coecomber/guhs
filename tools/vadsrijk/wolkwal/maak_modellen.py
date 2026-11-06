"""T-WOLKWAL: the GeckoLib models + textures + animations of the zone-5 leftovers (no Blockbench: made here, like the kit renders).

  1. "Wolkwacht-ridder": a real guh (the core bones of guh.geo.json, so every guh animation works) in cloud-blue/white armour with
     gold trim: helmet with a gold rim + nose guard + cheek plates, a white-and-blue plume, breastplate + back plate + gold belt,
     a cloud collar, a short cape, a little cloud shield on the left flank, a sword in its scabbard on the right, gold-banded boots.
     One texture per fur (guh, snow, golden, mint, choco, wolk): the left half of the texture IS that fur's guh texture (same UVs),
     the right half is the armour (UV space 256 x 256, 4 texels per unit).
       -> geckolib/models/entity/vadsrijk/zone5/wolkwal/wolkwacht_ridder.geo.json
       -> geckolib/animations/entity/vadsrijk/zone5/wolkwal/wolkwacht_ridder.animation.json (guh idle/walk/sit + plume sway)
       -> textures/vadsrijk/zone5/wolkwal/wolkwacht_ridder_<fur>.png
  2. The same knight for t-z5wezens' Sparringridder (keeps ITS animation file: bones root/lijf/helm/arm_l/schild/arm_r/wapen):
     a pillow sword in the right paw, a big cloud shield in the left.
       -> geckolib/models/entity/vadsrijk/zone5/wolkwal/sparringridder.geo.json + textures/.../sparringridder{,_elite,_gloed}.png
       (integration: `--sparring-over` copies them over t-z5wezens' vadsrijk/zone5/wezens/sparringridder.* files)
  3. Moeder Wolkwal's moving fin tips and fluke tips (1 model pixel = 1 block at scale 16; textures tiled from the real
     walvishuid/walvisbuik block textures, so they match the static blocks):
       -> geckolib/models/entity/vadsrijk/zone5/wolkwal/{vintip,staarttip}.geo.json, animation wolkwal.animation.json (wuif, slag)
       -> textures/vadsrijk/zone5/wolkwal/{vintip,staarttip}.png

Run from the project root:  python tools/vadsrijk/wolkwal/maak_modellen.py [--sparring-over]
"""
import json
import math
import os
import shutil
import sys

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", ".."))
A = os.path.join(ROOT, "src", "main", "resources", "assets", "guhs")
GEO = os.path.join(A, "geckolib", "models", "entity", "vadsrijk", "zone5", "wolkwal")
ANI = os.path.join(A, "geckolib", "animations", "entity", "vadsrijk", "zone5", "wolkwal")
TEX = os.path.join(A, "textures", "vadsrijk", "zone5", "wolkwal")
FURS = ["guh", "guh_snow", "guh_golden", "guh_mint", "guh_choco", "guh_wolk"]
CORE = ["root", "body", "tail", "leg_back_left", "leg_back_right", "head", "ear_left", "ear_right", "leg_front_left",
        "leg_front_right"]

# ------------------------------------------------------------------------------------------------ colours
BLAUW = (138, 190, 238)
BLAUW_D = (96, 146, 204)
BLAUW_L = (186, 220, 250)
WIT = (248, 250, 255)
WIT_S = (214, 230, 250)
GOUD = (242, 196, 72)
GOUD_L = (255, 232, 140)
GOUD_D = (196, 146, 40)
LEER = (70, 88, 140)
ROZE = (255, 190, 214)


def _mix(a, b, t):
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3))


class Atlas:
    """Shelf packer over a region of the UV space (units), painting at S texels per unit."""

    def __init__(self, img, u0, v0, u1, v1, s):
        self.img, self.u0, self.v0, self.u1, self.v1, self.s = img, u0, v0, u1, v1, s
        self.x, self.y, self.rij = u0, v0, 0

    def plek(self, w, h):
        w, h = max(1, math.ceil(w - 1e-6)), max(1, math.ceil(h - 1e-6))
        if self.x + w > self.u1:
            self.x, self.y, self.rij = self.u0, self.y + self.rij + 1, 0
        if self.y + h > self.v1:
            raise RuntimeError("atlas vol")
        p = (self.x, self.y)
        self.x += w + 1
        self.rij = max(self.rij, h)
        return p, w, h


def schilder(img, s, p, w, h, mat, face, rand=None):
    """Paint one face (UV rect p, w x h units) with a material."""
    x0, y0 = p[0] * s, p[1] * s
    W, H = w * s, h * s
    px = img.load()
    for j in range(H):
        for i in range(W):
            t = j / max(1, H - 1)
            if mat in ("plaat", "plaat_glad"):
                c = _mix(BLAUW_L, BLAUW_D, t * 0.8) if face not in ("up", "down") else (BLAUW_L if face == "up" else BLAUW_D)
                trim = mat == "plaat" and min(w, h) >= 2
                b = s // 2
                if trim and (i < b or j < b or i >= W - b or j >= H - b):
                    c = GOUD_L if (j < b or i < b) else GOUD_D
                elif (i * 7 + j * 3) % 23 == 0:
                    c = _mix(c, WIT, 0.35)
            elif mat == "wit":
                c = _mix(WIT, WIT_S, t * 0.7)
                if ((i // max(1, s)) + (j // max(1, s))) % 3 == 0 and (i + j) % 5 == 0:
                    c = WIT_S
            elif mat == "goud":
                c = _mix(GOUD_L, GOUD_D, t)
                if j == 0 or i == 0:
                    c = GOUD_L
            elif mat == "pluim":
                # feather barbs: diagonal stripes white / cloud blue
                c = WIT if ((i + j // 2) // max(1, s // 2)) % 3 else BLAUW_L
                c = _mix(c, BLAUW, t * 0.3)
            elif mat == "leer":
                c = _mix(LEER, (50, 64, 110), t)
            elif mat == "kussen":
                c = WIT if (i // max(1, s)) % 2 else ROZE
                c = _mix(c, WIT_S, t * 0.4)
            elif mat == "vacht":
                c = rand
            else:
                raise ValueError(mat)
            px[x0 + i, y0 + j] = c + (255,)


FACES = ("north", "east", "south", "west", "up", "down")


def maten(size, face):
    sx, sy, sz = size
    return {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}[face]


class Model:
    def __init__(self, ident, tw, th, img, atlas, s):
        self.ident, self.tw, self.th, self.img, self.atlas, self.s = ident, tw, th, img, atlas, s
        self.bones = []
        self.idx = {}

    def bone(self, name, parent, pivot, rotation=None, cubes=None):
        b = {"name": name, "pivot": list(pivot)}
        if parent:
            b["parent"] = parent
        if rotation:
            b["rotation"] = list(rotation)
        b["cubes"] = list(cubes or [])
        self.bones.append(b)
        self.idx[name] = b
        return b

    def kopie(self, b, parent=None):
        b = json.loads(json.dumps(b))
        if parent is not None:
            b["parent"] = parent
        self.bones.append(b)
        self.idx[b["name"]] = b
        return b

    def cube(self, bone, origin, size, mat, kleur=None, faces=None, tegel=None):
        """A cube with every face packed into the atlas and painted with mat (or tiled with a block texture: tegel=(top, side, onder))."""
        uv = {}
        for f in FACES:
            w, h = maten(size, f)
            p, wa, ha = self.atlas.plek(w, h)
            if tegel:
                t = tegel[0] if f == "up" else tegel[2] if f == "down" else tegel[1]
                for j in range(ha):
                    for i in range(wa):
                        self.img.paste(t, ((p[0] + i) * self.s, (p[1] + j) * self.s))
            else:
                schilder(self.img, self.s, p, wa, ha, (faces or {}).get(f, mat), f, kleur)
            uv[f] = {"uv": [p[0], p[1]], "uv_size": [w, h]}
        self.idx[bone]["cubes"].append({"origin": list(origin), "size": list(size), "uv": uv})

    def json(self):
        bones = []
        for b in self.bones:
            b = dict(b)
            if not b.get("cubes"):
                b.pop("cubes", None)
            bones.append(b)
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": self.ident, "texture_width": self.tw, "texture_height": self.th,
                            "visible_bounds_width": 4, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1, 0]},
            "bones": bones}]}


def _schrijf(pad, data):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=1)
        f.write("\n")


# ------------------------------------------------------------------------------------------------ the knight
def guh_bones():
    d = json.load(open(os.path.join(A, "geckolib", "models", "entity", "guh.geo.json"), encoding="utf-8"))
    return {b["name"]: b for b in d["minecraft:geometry"][0]["bones"] if b["name"] in CORE}


def harnas(m, sparring):
    """The armour on the guh's own bones (head, body, legs); sparring = the training knight (arms instead of flank gear)."""
    # helmet: cap, crown, gold rim, nose guard, cheek plates (not on the sparring knight: its paws sit there)
    m.bone("helm", "head", [0, 6, -2])
    m.cube("helm", [-7.5, 12.5, -12.5], [15, 3, 12.5], "plaat")
    m.cube("helm", [-5, 15.5, -10.5], [10, 1.5, 8], "plaat_glad")
    m.cube("helm", [-7.75, 12.25, -12.75], [15.5, 1, 13], "goud")
    m.cube("helm", [-0.75, 8.75, -13.25], [1.5, 3.75, 0.75], "goud")
    if not sparring:
        m.cube("helm", [8, 6.5, -10], [0.75, 6, 5], "plaat_glad")
        m.cube("helm", [-8.75, 6.5, -10], [0.75, 6, 5], "plaat_glad")
    # plume (own bone: it sways)
    m.bone("pluim", "helm", [0, 17, -6])
    m.cube("pluim", [-1, 17, -7], [2, 1, 2], "goud")
    m.cube("pluim", [-1, 18, -9], [2, 3.5, 6], "pluim")
    m.cube("pluim", [-0.75, 19.5, -4], [1.5, 3.5, 4], "pluim")
    m.cube("pluim", [-0.5, 17.5, -0.5], [1, 3, 3.5], "pluim")
    # body: breastplate, back plate, gold belt, cloud collar, short cape
    m.bone("kuras", "body", [0, 6, 6])
    m.cube("kuras", [-6.75, 2, -3], [13.5, 8.5, 5], "plaat")
    m.cube("kuras", [-6.25, 10.6, -2.75], [12.5, 1, 12.5], "plaat")
    m.cube("kuras", [-6.85, 4, -3.1], [13.7, 1.25, 15.4], "goud")
    m.cube("kuras", [-2, 6, -3.4], [4, 3, 0.5], "goud")                  # the gold cloud badge on the chest
    m.bone("kraag", "body", [0, 9, -3])
    m.cube("kraag", [-6, 8.5, -4.25], [12, 2.5, 2.5], "wit")
    m.cube("kraag", [-7, 9, -3.5], [3, 2.5, 3], "wit")
    m.cube("kraag", [4, 9, -3.5], [3, 2.5, 3], "wit")
    m.bone("cape", "body", [0, 11, 12])
    m.cube("cape", [-5.5, 3.5, 12.4], [11, 7.5, 0.6], "plaat")
    # boots with a gold band on the four feet
    for leg, o in (("leg_front_left", [4.0, 0, -2.75]), ("leg_front_right", [-8.0, 0, -2.75]),
                   ("leg_back_left", [4.25, 0, 6.75]), ("leg_back_right", [-8.25, 0, 6.75])):
        nb = "laars_" + leg[4:]
        m.bone(nb, leg, m.idx[leg]["pivot"])
        m.cube(nb, o, [4, 1.6, 5], "plaat_glad")
        m.cube(nb, [o[0] - 0.1, 1.6, o[2] - 0.1], [4.2, 0.5, 5.2], "goud")
    if not sparring:
        # little cloud shield on the left flank, sword in its scabbard on the right
        m.bone("schild", "body", [7, 6, 4])
        m.cube("schild", [6.95, 2.5, 0.5], [1, 6, 8], "wit")
        m.cube("schild", [6.95, 3.5, -0.5], [1, 4, 10], "wit")
        m.cube("schild", [6.95, 1.5, 1.5], [1, 8, 6], "wit")
        m.cube("schild", [7.7, 4.25, 3.25], [0.75, 2.5, 2.5], "goud")
        m.bone("zwaard", "body", [-7, 4, 4])
        m.cube("zwaard", [-7.75, 3.5, -0.5], [0.9, 1.5, 10], "plaat_glad")
        m.cube("zwaard", [-7.85, 3.25, 9.25], [1.1, 2, 0.75], "goud")
        m.cube("zwaard", [-7.85, 2.5, -1.25], [1.1, 3.5, 0.75], "goud")
        m.cube("zwaard", [-7.65, 3.6, -3.25], [0.7, 1.2, 2], "leer")
        m.cube("zwaard", [-7.8, 3.45, -4], [1, 1.5, 0.75], "goud")


def vachtkleur(img):
    """The fur's main colour (the body's north face of the guh texture)."""
    crop = img.crop((0, 0, 13 * 4, 7 * 4)).convert("RGB")
    p = list(crop.getdata())
    return tuple(sum(c[i] for c in p) // len(p) for i in range(3))


def ridder(fur, sparring=False):
    s = 4
    img = Image.new("RGBA", (256 * s, 256 * s), (0, 0, 0, 0))
    vacht = Image.open(os.path.join(A, "textures", "entity", fur + ".png")).convert("RGBA")
    img.paste(vacht.resize((128 * s, 128 * s)), (0, 0))
    atlas = Atlas(img, 130, 2, 254, 254, s)
    m = Model("geometry.vr_z5_" + ("sparringridder" if sparring else "wolkwacht_ridder"), 256, 256, img, atlas, s)
    g = guh_bones()
    m.kopie(g["root"])
    if sparring:
        m.bone("lijf", "root", [0, 2, 0])
    for n in CORE[1:]:
        par = g[n].get("parent")
        m.kopie(g[n], "lijf" if sparring and par == "root" else None)
    harnas(m, sparring)
    if sparring:
        vk = vachtkleur(vacht)
        m.bone("arm_r", "lijf", [8.5, 7, -6])
        m.cube("arm_r", [8.2, 3, -7.5], [2.5, 4.5, 3], "vacht", kleur=vk)
        m.cube("arm_r", [8.0, 5.5, -7.7], [2.9, 1.5, 3.4], "plaat_glad")
        m.bone("wapen", "arm_r", [9.5, 4, -7])
        m.cube("wapen", [9.0, 3.5, -10], [1, 1, 3], "leer")
        m.cube("wapen", [8.0, 2.75, -10.75], [3, 2.5, 0.75], "goud")
        m.cube("wapen", [8.25, 2.85, -21], [2.5, 2.3, 10.25], "kussen")
        m.bone("arm_l", "lijf", [-8.5, 7, -6])
        m.cube("arm_l", [-10.7, 3, -7.5], [2.5, 4.5, 3], "vacht", kleur=vk)
        m.cube("arm_l", [-10.9, 5.5, -7.7], [2.9, 1.5, 3.4], "plaat_glad")
        m.bone("schild", "arm_l", [-11, 5, -8])
        m.cube("schild", [-12.4, 0.5, -12], [1.2, 9, 9], "wit")
        m.cube("schild", [-12.4, 1.5, -13], [1.2, 7, 11], "wit")
        m.cube("schild", [-12.4, -0.5, -11], [1.2, 11, 7], "wit")
        m.cube("schild", [-13.1, 3.5, -8.5], [0.75, 3, 2.5], "goud")
        m.cube("schild", [-13.0, 7.5, -11.5], [0.6, 1, 4], "plaat")
    return m, img


def anim_ridder():
    g = json.load(open(os.path.join(A, "geckolib", "animations", "entity", "guh.animation.json"), encoding="utf-8"))
    out = {}
    for naam in ("animation.guh.idle", "animation.guh.walk", "animation.guh.sit"):
        a = json.loads(json.dumps(g["animations"][naam]))
        lengte = float(a.get("animation_length", 4.0))
        rot = {}
        n = 16
        for i in range(n + 1):
            t = lengte * i / n
            rot[f"{t:.3f}".rstrip("0").rstrip(".") if t else "0.0"] = [round(-4 * math.sin(2 * math.pi * i / n), 2), 0,
                                                                      round(3 * math.sin(2 * math.pi * i / n + 1.2), 2)]
        a.setdefault("bones", {})["pluim"] = {"rotation": rot}
        out[naam] = a
    return {"format_version": "1.8.0", "animations": out}


# ------------------------------------------------------------------------------------------------ the whale pieces
def blok(naam):
    return Image.open(os.path.join(A, "textures", "block", "hoogvads", naam + ".png")).convert("RGBA").resize((16, 16))


def walvisstuk(naam, segmenten, top, zij):
    s = 16
    img = Image.new("RGBA", (64 * s, 64 * s), (0, 0, 0, 0))
    atlas = Atlas(img, 0, 0, 64, 64, s)
    m = Model("geometry.vr_z5_" + naam, 64, 64, img, atlas, s)
    m.bone("root", None, [0, 0, 0])
    teg = (blok(top), blok(zij), blok("walvisbuik"))
    m.bone("stuk", "root", [0, 0, 0])
    m.bone("punt", "stuk", [0, 0, segmenten[0][2]])
    for i, (o, sz, _) in enumerate(segmenten):
        m.cube("stuk" if i == 0 else "punt", o, sz, None, tegel=teg)
    return m, img


def anim_wolkwal():
    def golf(periode, amp_stuk, amp_punt, vertraging):
        n = 16
        stuk, punt = {}, {}
        for i in range(n + 1):
            t = periode * i / n
            k = f"{t:.4f}".rstrip("0").rstrip(".") if t else "0.0"
            stuk[k] = [round(amp_stuk * math.sin(2 * math.pi * i / n), 2), 0, 0]
            punt[k] = [round(amp_punt * math.sin(2 * math.pi * i / n - vertraging), 2), 0, 0]
        return {"loop": True, "animation_length": periode, "bones": {"stuk": {"rotation": stuk}, "punt": {"rotation": punt}}}
    return {"format_version": "1.8.0", "animations": {"wuif": golf(4.0, 5, 9, 0.9), "slag": golf(5.0, 6, 12, 1.1)}}


def main():
    os.makedirs(TEX, exist_ok=True)
    for fur in FURS:
        m, img = ridder(fur)
        img.save(os.path.join(TEX, f"wolkwacht_ridder_{fur}.png"))
    _schrijf(os.path.join(GEO, "wolkwacht_ridder.geo.json"), m.json())
    _schrijf(os.path.join(ANI, "wolkwacht_ridder.animation.json"), anim_ridder())
    m, img = ridder("guh_snow", sparring=True)
    _schrijf(os.path.join(GEO, "sparringridder.geo.json"), m.json())
    img.save(os.path.join(TEX, "sparringridder.png"))
    # elite: the gold trim glows (additive layer); gloed: nothing glows
    px = img.load()
    elite = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ep = elite.load()
    for y in range(img.size[1]):
        for x in range(img.size[0] // 2, img.size[0]):
            r, g_, b, a = px[x, y]
            if a and r > 180 and g_ > 130 and b < 150:
                ep[x, y] = (r // 2, g_ // 2, b // 4, 255)
    elite.save(os.path.join(TEX, "sparringridder_elite.png"))
    Image.new("RGBA", img.size, (0, 0, 0, 0)).save(os.path.join(TEX, "sparringridder_gloed.png"))
    # fin tip: continues the static fin outward (model -z), a little lower with every segment
    m, img = walvisstuk("vintip", [([-5, -1.5, -6], [10, 3, 9], 3), ([-4, -2.25, -11], [8, 2.5, 5], 0),
                                   ([-2.5, -3, -15], [5, 2, 4], 0), ([-1.25, -3.5, -18], [2.5, 1.5, 3], 0)],
                        "walvishuid_licht", "walvishuid_licht")
    m.idx["punt"]["pivot"] = [0, 0, -6]
    _schrijf(os.path.join(GEO, "vintip.geo.json"), m.json())
    img.save(os.path.join(TEX, "vintip.png"))
    m, img = walvisstuk("staarttip", [([-3.5, -1.5, -3], [7, 3, 7], 4), ([-2.5, -1, -8], [5, 2.5, 5], 0),
                                      ([-1.5, -0.5, -11], [3, 2, 3], 0), ([-0.75, 0, -13], [1.5, 1.5, 2], 0)],
                        "walvishuid", "walvishuid")
    m.idx["punt"]["pivot"] = [0, 0, -3]
    _schrijf(os.path.join(GEO, "staarttip.geo.json"), m.json())
    img.save(os.path.join(TEX, "staarttip.png"))
    _schrijf(os.path.join(ANI, "wolkwal.animation.json"), anim_wolkwal())
    print("maak_modellen: wolkwacht_ridder x%d, sparringridder, vintip, staarttip" % len(FURS))
    if "--sparring-over" in sys.argv:
        geo_w = os.path.join(A, "geckolib", "models", "entity", "vadsrijk", "zone5", "wezens")
        tex_w = os.path.join(A, "textures", "vadsrijk", "zone5", "wezens")
        shutil.copy(os.path.join(GEO, "sparringridder.geo.json"), os.path.join(geo_w, "sparringridder.geo.json"))
        for n in ("sparringridder", "sparringridder_elite", "sparringridder_gloed"):
            shutil.copy(os.path.join(TEX, n + ".png"), os.path.join(tex_w, n + ".png"))
        print("  sparringridder copied over t-z5wezens' files")


if __name__ == "__main__":
    main()
