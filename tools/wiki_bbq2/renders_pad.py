"""
The wiki pictures of the second part of bbq2: Het Guhpad and Het Snuffeleiland.

renders(r) is called by tools/wiki_bbq2/renders.py (python tools/wiki_renders.py docs/wiki/img --only-bbq2 --bbq2=verhalenpad;
r is tools/wiki_renders.py, r.OUT the folder docs/wiki/img). Everything is drawn from the mod's own generated files:
  snuffel_hond.png                 the six breeds a player chooses from, side by side at the size the game draws them
  snuffel_hond_<ras>.png           one breed in its three coats
  snuffel_pup.png                  the puppy of every breed (Kleine Wiebel is a puppy of your own breed and coat)
  snuffel_snuffelt.png             a dog with its nose to the ground and its buddy pointing ahead
  snuffel_maatje.png, snuffel_maatje_<a|b|c>.png    the three buddies; one buddy with its happy and its naughty face
  snuffel_boompje.png              the little tree in its four steps
  snuffel_bewoners.png, snuffel_bewoner_<id>.png    the ten dogs of Snuffeldorp (NOT Papa Zwerfpoot: he is missing, that is the story)
  steiger_buurvrouw.png, steiger_boot.png           Buurvrouw Mandje and De Natte Neus with its captain
  structure_steigerhuisje.png      the dock cottage as it stands on flat land, with the sea, the captain and the boat
  snuffel_eiland.png, snuffel_dorp.png              the island (from its 15 tiles and eiland.json) and the village on it
  block_guhstation.png, icon_guhstation.png         the reward block
  gui_guhpad_padkaart.png          the path map of the Guhdex (the texture the game draws its ticks and locks on)

The dogs, buddies and the tree are drawn with the rasteriser of the approved model generator (features/snuffel_modellen.teken:
one scale for a whole row, so a teckel stays smaller than a golden retriever); the buildings with a textured block scene (the
block's own blockstate and models), the way the slices' own check pictures were made.
"""
import json
import math
import os

import numpy as np

BEWONERS = ("redder", "dokter", "trainer", "kapitein", "bakker", "visser", "juf", "oma", "tuinder", "pup")
ROZE_ZEE = (232, 106, 184)          # the Diepe Guhzee of the Guhmensie
BLAUWE_ZEE = (63, 118, 228)         # the sea of the Snuffeleiland
TINT = {"grass_block": (124, 189, 107), "short_grass": (124, 189, 107), "fern": (124, 189, 107), "oak_leaves": (100, 170, 80),
        "birch_leaves": (128, 167, 85), "spruce_leaves": (97, 153, 97), "jungle_leaves": (80, 170, 60), "lily_pad": (32, 128, 48),
        "water": BLAUWE_ZEE}
# blocks that fill their whole cell: one that is surrounded by six of them is never seen and not drawn
VOL = {"sand", "stone", "dirt", "grass_block", "spruce_planks", "dirt_path", "oak_log", "stripped_spruce_log", "barrel", "stone_bricks",
       "mossy_stone_bricks", "cracked_stone_bricks", "calcite", "bricks", "coarse_dirt", "cobblestone", "bookshelf", "andesite",
       "mossy_cobblestone", "sandstone", "polished_andesite", "white_concrete", "red_concrete", "oak_planks", "birch_planks",
       "dark_oak_planks", "smooth_sandstone", "mushroom_stem", "moss_block", "gravel", "clay", "red_terracotta", "white_wool", "red_wool"}


def _sm():
    from features import snuffel_modellen
    return snuffel_modellen


def _pad(r, soort, naam):
    return os.path.join(r.ASSETS, "geckolib", soort, "entity", naam)


def _save(r, im, name):
    box = im.getbbox()
    if box:
        im = im.crop((max(0, box[0] - 8), max(0, box[1] - 8), min(im.width, box[2] + 8), min(im.height, box[3] + 8)))
    im.save(os.path.join(r.OUT, name + ".png"))
    print("rendered", name)


# ---------------------------------------------------------------------------------------------------------------------
# figures (dogs, buddies, the tree)
# ---------------------------------------------------------------------------------------------------------------------
def figuur(r, model, tex=None, yaw=25, pitch=-14, schaal=10.0, pose=None):
    """One model of the game as a cropped picture; schaal = screen pixels per model pixel; pose = the name of one of its animations."""
    sm = _sm()
    with open(_pad(r, "models", model + ".geo.json"), encoding="utf-8") as f:
        geo = json.load(f)
    rot = None
    if pose:
        with open(_pad(r, "animations", model + ".animation.json"), encoding="utf-8") as f:
            rot = sm.pose_van(json.load(f), pose)[0]
    return sm.teken(sm.quads(geo, r.texture(f"guhs:entity/{tex or model}"), rot), yaw, pitch, schaal)[0]


def rij(r, beelden, gat=34, zweef=None):
    """Figures side by side with their feet on one line (zweef[i]: that many pixels above it), each with a soft shadow."""
    from PIL import ImageDraw, ImageFilter
    zweef = zweef or [0] * len(beelden)
    hoog = max(b.height + z for b, z in zip(beelden, zweef))
    grond = hoog + 10
    sheet = r.Image.new("RGBA", (sum(b.width for b in beelden) + gat * (len(beelden) + 1), grond + 30), (0, 0, 0, 0))
    schaduw = r.Image.new("RGBA", sheet.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(schaduw)
    x = gat
    plek = []
    for b, z in zip(beelden, zweef):
        mid, rx = x + b.width / 2, b.width * (0.24 if z else 0.36)
        d.ellipse((mid - rx, grond - rx * 0.2 - 4, mid + rx, grond + rx * 0.2 - 4), fill=(60, 40, 40, 56))
        plek.append((int(x), int(grond - b.height - z)))
        x += b.width + gat
    sheet.alpha_composite(schaduw.filter(ImageFilter.GaussianBlur(6)))
    for b, p in zip(beelden, plek):
        sheet.alpha_composite(b, p)
    return sheet


def rassen(r):
    """{breed: [coat ids]} in the order of the choice screen (the lang file's own order), without the trainer's breed."""
    with open(os.path.join(r.ASSETS, "lang", "nl_nl.json"), encoding="utf-8") as f:
        lang = json.load(f)
    out = {}
    for k in lang:
        if k.startswith("gui.guhs.snuffel.kleur."):
            ras, kleur = k.split(".")[4:6]
            if ras != "speurhond":
                out.setdefault(ras, []).append(kleur)
    return out


def honden(r):
    R = rassen(r)
    S = 0.8                              # the game draws a dog at 0.8
    _save(r, rij(r, [figuur(r, f"snuffelhond_{ras}", f"snuffelhond_{ras}_{k[0]}", 28 if i % 2 == 0 else 332, -14, 11 * S)
                     for i, (ras, k) in enumerate(R.items())]), "snuffel_hond")
    for ras, kleuren in R.items():
        _save(r, rij(r, [figuur(r, f"snuffelhond_{ras}", f"snuffelhond_{ras}_{k}", 328 if i == 1 else 32, -14, 13 * S)
                         for i, k in enumerate(kleuren)]), f"snuffel_hond_{ras}")
    _save(r, rij(r, [figuur(r, f"snuffelhond_{ras}_pup", f"snuffelhond_{ras}_pup_{k[0]}", 28 if i % 2 == 0 else 332, -14, 13 * S)
                     for i, (ras, k) in enumerate(R.items())]), "snuffel_pup")
    # a dog that sniffs, its buddy a little ahead of it
    ras = next(iter(R))
    hond = figuur(r, f"snuffelhond_{ras}", f"snuffelhond_{ras}_{R[ras][0]}", 292, -8, 14 * S, pose="snuffel")
    maatje = figuur(r, "snuffel_maatje_b_blij", None, 300, -10, 14 * 0.5)
    _save(r, rij(r, [hond, maatje], gat=26, zweef=[0, int(hond.height * 0.45)]), "snuffel_snuffelt")


def maatjes(r):
    M = 0.5 * 2.2                        # the game draws a buddy at 0.5; here a little bigger than life, it is small
    _save(r, rij(r, [figuur(r, f"snuffel_maatje_{k}_blij", None, 20 if k != "b" else 340, -12, 12 * M) for k in "abc"],
                 zweef=[24, 24, 24]), "snuffel_maatje")
    for k in "abc":
        _save(r, rij(r, [figuur(r, f"snuffel_maatje_{k}_blij", None, 20, -12, 13 * M),
                         figuur(r, f"snuffel_maatje_{k}_ondeugend", None, 340, -12, 13 * M)], zweef=[20, 20]), f"snuffel_maatje_{k}")


def boompje(r):
    _save(r, rij(r, [figuur(r, f"snuffel_boompje_{stap}", None, 25, -16, 11) for stap in (1, 2, 3, 4)], gat=20), "snuffel_boompje")


def bewoners(r):
    S = 0.8
    beelden = {b: figuur(r, f"snuffel_{b}", None, 25 if i % 2 == 0 else 335, -14, 12 * S) for i, b in enumerate(BEWONERS)}
    for b, im in beelden.items():
        _save(r, rij(r, [im]), f"snuffel_bewoner_{b}")
    # the group picture: two rows of five
    boven = rij(r, [figuur(r, f"snuffel_{b}", None, 25 if i % 2 == 0 else 335, -14, 9 * S) for i, b in enumerate(BEWONERS[:5])], gat=26)
    onder = rij(r, [figuur(r, f"snuffel_{b}", None, 335 if i % 2 == 0 else 25, -14, 9 * S) for i, b in enumerate(BEWONERS[5:])], gat=26)
    groep = r.Image.new("RGBA", (max(boven.width, onder.width), boven.height + onder.height - 10), (0, 0, 0, 0))
    groep.alpha_composite(boven, ((groep.width - boven.width) // 2, 0))
    groep.alpha_composite(onder, ((groep.width - onder.width) // 2, boven.height - 10))
    _save(r, groep, "snuffel_bewoners")
    # the dock: the neighbour (a roodgoud golden retriever), and the boat with its captain
    _save(r, rij(r, [figuur(r, "snuffelhond_golden", "snuffelhond_golden_rood", 25, -14, 12 * S)]), "steiger_buurvrouw")
    q = geo_op(r, "steiger_boot", "steiger_boot", (0, 0, 0), 0, 1.0) + geo_op(r, "snuffel_kapitein", "snuffel_kapitein", (0, 0.45, 1.3), 0, S)
    _save(r, snel(r, q, yaw=35, pitch=-20, size=900), "steiger_boot")


# ---------------------------------------------------------------------------------------------------------------------
# a textured block scene: every block drawn with its own blockstate and models
# ---------------------------------------------------------------------------------------------------------------------
def _blockstate(r, block):
    ns, name = block.split(":")
    if ns == "guhs":
        with open(os.path.join(r.ASSETS, "blockstates", name + ".json"), encoding="utf-8") as f:
            return json.load(f)
    with r._jar.open(f"assets/minecraft/blockstates/{name}.json") as f:
        return json.load(f)


_BS = {}


def _past(when, props):
    if "OR" in when:
        return any(_past(w, props) for w in when["OR"])
    if "AND" in when:
        return all(_past(w, props) for w in when["AND"])
    return all(str(props.get(k, "")) in str(v).split("|") for k, v in when.items())


def _varianten(r, block, props):
    if block not in _BS:
        _BS[block] = _blockstate(r, block)
    bs, out = _BS[block], []
    if "variants" in bs:
        for key, v in bs["variants"].items():
            want = dict(kv.split("=") for kv in key.split(",")) if key else {}
            if all(str(props.get(k, "")) == val for k, val in want.items()):
                return [v[0] if isinstance(v, list) else v]
        # a property the template leaves at its default: the variant that fits what it does give, else the first
        beste = None
        for key, v in bs["variants"].items():
            want = dict(kv.split("=") for kv in key.split(",")) if key else {}
            if all(str(props[k]) == val for k, val in want.items() if k in props) and all(val in ("false", "0", "none") for k, val in want.items() if k not in props):
                beste = v
                break
        beste = beste or next(iter(bs["variants"].values()))
        return [beste[0] if isinstance(beste, list) else beste]
    for part in bs["multipart"]:
        if "when" not in part or _past(part["when"], props):
            v = part["apply"]
            out.append(v[0] if isinstance(v, list) else v)
    return out


def _draai(r, quads, x_deg, y_deg):
    if not x_deg and not y_deg:
        return quads
    c = np.array([0.5, 0.5, 0.5])
    ax, ay = math.radians(-x_deg), math.radians(-y_deg)
    Mx = np.array([[1, 0, 0], [0, math.cos(ax), -math.sin(ax)], [0, math.sin(ax), math.cos(ax)]])
    My = np.array([[math.cos(ay), 0, math.sin(ay)], [0, 1, 0], [-math.sin(ay), 0, math.cos(ay)]])
    M = My @ Mx
    return [r.Quad(M @ (q.origin - c) + c, M @ q.u, M @ q.v, q.tex, q.uv, M @ q.normal, q.tint) for q in quads]


_MODEL = {}


def _blok_quads(r, block, props, pos, tint):
    key = (block, tuple(sorted((props or {}).items())), tint.get(block.split(":")[1]))
    if key not in _MODEL:
        quads = []
        try:
            for v in _varianten(r, block, {k: str(val) for k, val in (props or {}).items()}):
                quads += _draai(r, r.model_quads(v["model"], tint=tint.get(block.split(":")[1])), v.get("x", 0), v.get("y", 0))
        except Exception:  # noqa: BLE001  (a block the renderer has no model for: beds, banners, fluids)
            quads = []
        _MODEL[key] = quads
    p = np.array(pos, float)
    return [r.Quad(q.origin + p, q.u, q.v, q.tex, q.uv, q.normal, q.tint) for q in _MODEL[key]]


def scene(r, blocks, box=None, tint=TINT):
    """blocks: {(x, y, z): (name, properties)} -> quads; box = (x0, y0, z0, x1, y1, z1) keeps a part."""
    names = {p: v[0] for p, v in blocks.items() if v[0] not in ("minecraft:air", "minecraft:water", "minecraft:structure_void")
             and (not box or (box[0] <= p[0] <= box[3] and box[1] <= p[1] <= box[4] and box[2] <= p[2] <= box[5]))}
    vol = {p for p, n in names.items() if n.split(":")[1] in VOL}
    quads = []
    for pos, name in names.items():
        x, y, z = pos
        if pos in vol and all(n in vol for n in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1))):
            continue
        quads += _blok_quads(r, name, blocks[pos][1], pos, tint)
    return quads


def geo_op(r, model, tex, pos, yaw, schaal=1.0):
    """A GeckoLib model of the game standing in a scene."""
    qs = r.geo_quads(_pad(r, "models", model + ".geo.json"), f"guhs:entity/{tex}")
    a = math.radians(yaw)
    M = np.array([[math.cos(a), 0, math.sin(a)], [0, 1, 0], [-math.sin(a), 0, math.cos(a)]]) * schaal
    return [r.Quad(M @ q.origin + np.array(pos, float), M @ q.u, M @ q.v, q.tex, q.uv, M @ q.normal / max(schaal, 1e-6), q.tint) for q in qs]


def snel(r, quads, yaw=35, pitch=-25, size=512, margin=0.03, ss=2):
    """wiki_renders.render's picture (orthographic, z-buffered point splatting, the same light) with the z-test of a quad's
    points done in numpy instead of a python loop (an island has 280,000 quads), on a transparent sheet."""
    R = r.rot_matrix(yaw, pitch)
    O = np.array([q.origin for q in quads])
    U = np.array([q.u for q in quads])
    V = np.array([q.v for q in quads])
    hoeken = np.concatenate([O @ R.T, (O + U) @ R.T, (O + V) @ R.T, (O + U + V) @ R.T])
    lo, hi = hoeken[:, :2].min(0), hoeken[:, :2].max(0)
    span = max(hi - lo) or 1
    W = size * ss
    scale = W * (1 - 2 * margin) / span
    center = (lo + hi) / 2
    zbuf = np.full(W * W, np.inf)
    col = np.zeros((W * W, 4), np.float32)
    light = np.array([-0.35, 0.85, -0.4])
    light /= np.linalg.norm(light)
    for q in quads:
        if (R @ q.normal)[2] > 1e-6:
            continue
        shade = 0.62 + 0.38 * max(0.0, float(np.dot(q.normal, light)))
        tex = r.tex_array(q.tex) if isinstance(q.tex, str) else q.tex
        th, tw = tex.shape[:2]
        u0, v0, u1, v1 = q.uv
        nu = max(2, int(np.linalg.norm(R @ q.u) * scale * 1.6) + 1)
        nv = max(2, int(np.linalg.norm(R @ q.v) * scale * 1.6) + 1)
        A, B = np.meshgrid((np.arange(nu) + 0.5) / nu, (np.arange(nv) + 0.5) / nv)
        S = (q.origin + A[..., None] * q.u + B[..., None] * q.v) @ R.T
        px = ((S[..., 0] - center[0]) * scale + W / 2).astype(int)
        py = (-(S[..., 1] - center[1]) * scale + W / 2).astype(int)
        z = S[..., 2]
        rgba = tex[np.clip((v0 + B * (v1 - v0)) * th, 0, th - 1).astype(int), np.clip((u0 + A * (u1 - u0)) * tw, 0, tw - 1).astype(int)]
        m = (px >= 0) & (px < W) & (py >= 0) & (py < W) & (rgba[..., 3] > 20)
        idx, z, rgb = (py[m] * W + px[m]), z[m], rgba[m][:, :3]
        if q.tint is not None:
            rgb = rgb * (np.array(q.tint[:3], np.float32) / 255.0)
        dichter = z < zbuf[idx]
        idx, z, rgb = idx[dichter], z[dichter], rgb[dichter]
        if not len(idx):
            continue
        order = np.argsort(-z, kind="stable")       # far first: the nearest point of a pixel is written last
        idx, z, rgb = idx[order], z[order], rgb[order]
        zbuf[idx] = z
        col[idx, :3] = rgb * shade
        col[idx, 3] = 255
    img = r.Image.fromarray(np.clip(col.reshape(W, W, 4), 0, 255).astype(np.uint8), "RGBA")
    return img.convert("RGBa").resize((size, size), r.Image.LANCZOS).convert("RGBA")


def _zee(r, cellen, y, kleur):
    """A flat sheet of sea over the given (x, z) cells (the renderer has no fluids)."""
    return [r.Quad((x, y, z), (1, 0, 0), (0, 0, 1), "minecraft:block/white_concrete", (0, 0, 16, 16), (0, 1, 0), kleur) for x, z in cellen]


# ---------------------------------------------------------------------------------------------------------------------
def steigerhuisje(r):
    """The dock cottage as it turns out on flat land (the builder's own example: in the .nbt the outer ring is marker blocks
    that the worldgen turns into a wall or a retaining wall), with the sea, the captain and his boat."""
    import types

    import make_structures as ms
    from features import snuffel_steiger_bouw as bouw
    s, _ = bouw.steigerhuisje(types.SimpleNamespace(Structure=ms.Structure, Byte=ms.Byte), voorbeeld=True)
    blocks = {p: (v[0], v[1] or {}) for p, v in s.blocks.items()}
    G = bouw.G
    q = scene(r, blocks, (0, G - 3, 0, bouw.SX, bouw.SY, bouw.SZ), dict(TINT, grass_block=(240, 150, 190), short_grass=(240, 150, 190)))
    open_ = lambda x, z: blocks.get((x, G - 2, z), ("minecraft:air",))[0] in ("minecraft:air", "minecraft:water", "minecraft:ladder")
    q += _zee(r, [(x, z) for x in range(-4, bouw.SX + 6) for z in range(bouw.PLOT_Z + 1, bouw.SZ + 5) if open_(x, z)], G - 1.12, ROZE_ZEE)
    (x, y, z), yaw = bouw.PLEK_KAPITEIN
    q += geo_op(r, "snuffel_kapitein", "snuffel_kapitein", (x, y, z), 180 - yaw, 0.8)
    (x, y, z), yaw = bouw.PLEK_BOOT
    q += geo_op(r, "steiger_boot", "steiger_boot", (x, y, z), 180 - yaw, 1.0)
    _save(r, snel(r, q, yaw=145, pitch=-28, size=1500), "structure_steigerhuisje")


def eiland(r):
    """The island from the files the game reads: data/guhs/snuffel/eiland.json and its tiles, with the residents and the
    little tree (full-grown) in place."""
    with open(os.path.join("src", "main", "resources", "data", "guhs", "snuffel", "eiland.json"), encoding="utf-8") as f:
        data = json.load(f)
    blocks = {}
    for stuk in data["stukken"]:
        st = r.load_structure(stuk["template"].split(":", 1)[1])
        ox, oy, oz = stuk["plek"]
        for (x, y, z), v in st.blocks.items():
            blocks[(x + ox, y + oy, z + oz)] = (v[0], v[1] or {})
    SX, _, SZ = data["maat"]
    G = int(data["strand"]["plek"][1]) - 1          # the ground block under the feet of whoever washes ashore
    land = {(x, z) for (x, y, z), v in blocks.items() if y == G and v[0] != "minecraft:water"}

    def figuren(box):
        binnen = lambda p: box[0] <= p[0] <= box[3] + 1 and box[2] <= p[2] <= box[5] + 1
        q = []
        for b in data["bewoners"]:
            if binnen(b["plek"]):
                q += geo_op(r, "snuffel_" + b["bewoner"], "snuffel_" + b["bewoner"], b["plek"], 180 - b.get("yaw", 0), 0.8)
        bx, by, bz = data["boom"]
        if binnen((bx, by, bz)):
            q += geo_op(r, "snuffel_boompje_4", "snuffel_boompje_4", (bx + 0.5, by, bz + 0.5), 0)
        return q

    for naam, box, rand, yaw, pitch, size in (("snuffel_eiland", (0, G - 2, 0, SX - 1, G + 30, SZ - 1), 8, 205, -40, 2000),
                                               ("snuffel_dorp", (50, G - 1, 96, 120, G + 14, 142), 0, 205, -36, 1700)):
        cellen = [(x, z) for x in range(box[0] - rand, box[3] + 1 + rand) for z in range(box[2] - rand, box[5] + 1 + rand) if (x, z) not in land]
        q = scene(r, blocks, box) + _zee(r, cellen, G - 0.12, BLAUWE_ZEE) + figuren(box)
        _save(r, snel(r, q, yaw=yaw, pitch=pitch, size=size), naam)


def padkaart(r):
    """The path map of the Guhdex: the texture itself, four times as big (the game draws the ticks, locks and pips on it)."""
    im = r.Image.open(os.path.join(r.ASSETS, "textures", "gui", "guhpad", "padkaart.png")).convert("RGBA")
    im.resize((im.width * 4, im.height * 4), r.Image.NEAREST).save(os.path.join(r.OUT, "gui_guhpad_padkaart.png"))
    print("rendered gui_guhpad_padkaart")


def renders(r, blokken):
    """blokken: renders.blocks (the Guhstation is drawn like every other block of bbq2)."""
    for wat in (padkaart, lambda r_: blokken(r_, only={"guhstation"}), honden, maatjes, boompje, bewoners, steigerhuisje, eiland):
        try:
            wat(r)
        except Exception as e:  # noqa: BLE001
            print("no render:", getattr(wat, "__name__", "guhstation"), repr(e))
