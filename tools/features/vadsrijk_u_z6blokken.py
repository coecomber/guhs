"""
1.10 round U, slice U-Z6WERELD: the new Zijdeland building blocks (zone 6 "Het Zijdeland", ZONE6_PLAN decision Z6-6: a small set).

20 blockstates: rose sandstone (+ stairs, slab, wall, the carved block with the guh motif), 3 mosaic tiles, the mashrabiya lattice
(a pane that connects like iron bars), turquoise dome tiles (+ stairs), silk cloth + a floating silk awning, the brass lantern
(standing/hanging) and date-palm wood (log, planks, stairs, slab, fence). Textures are generated here (16x16 Guhs-style pixel art,
textures/block/zijdeland/); the shape models use the vanilla templates (stairs, slab, wall, fence, glass pane, lantern, log). The
vanilla blockstate layouts (stairs/fence/wall/pane/log) and the lantern texture are read from the vanilla 26.1.2 client jar when it
is there; otherwise the committed resources are kept.

Java: feature/vadsrijk/zone6/wereld/Z6Blokken.java (registers BLOKKEN in the same order, "Guhs: Bouwen" tab).
Standalone (from the project root): python tools/features/vadsrijk_u_z6blokken.py
"""
import json
import os
import random
import sys
import zipfile

RES = os.path.join("src", "main", "resources")
A = os.path.join(RES, "assets", "guhs")
D = os.path.join(RES, "data", "guhs")
TEXDIR = os.path.join(A, "textures", "block", "zijdeland")
JAR = os.path.join(os.environ.get("APPDATA", ""), "PrismLauncher/libraries/com/mojang/minecraft/26.1.2/minecraft-26.1.2-client.jar")

# id -> (soort, NL, EN). soort: blok | trap | plaat | muur | stam | hek | ruit | luifel | lantaarn
BLOKKEN = {
    "rozenzandsteen": ("blok", "Rozenzandsteen", "Rose Sandstone"),
    "rozenzandsteen_trap": ("trap", "Rozenzandstenen trap", "Rose Sandstone Stairs"),
    "rozenzandsteen_plaat": ("plaat", "Rozenzandstenen plaat", "Rose Sandstone Slab"),
    "rozenzandsteen_muur": ("muur", "Rozenzandstenen muurtje", "Rose Sandstone Wall"),
    "rozenzandsteen_gebeiteld": ("blok", "Gebeitelde rozenzandsteen", "Carved Rose Sandstone"),
    "mozaiek_ster": ("blok", "Mozaïektegels (ster)", "Mosaic Tiles (Star)"),
    "mozaiek_guh": ("blok", "Mozaïektegels (guh)", "Mosaic Tiles (Guh)"),
    "mozaiek_bloem": ("blok", "Mozaïektegels (bloem)", "Mosaic Tiles (Flower)"),
    "mashrabiya_tralies": ("ruit", "Mashrabiya-traliewerk", "Mashrabiya Lattice"),
    "koepeltegel_turkoois": ("blok", "Turkoois koepeltegels", "Turquoise Dome Tiles"),
    "koepeltegel_turkoois_trap": ("trap", "Turkoois koepeltegeltrap", "Turquoise Dome Tile Stairs"),
    "zijden_doek": ("blok", "Zijden doek", "Silk Cloth"),
    "zijden_luifel": ("luifel", "Zijden luifel", "Silk Awning"),
    "messing_lantaarn": ("lantaarn", "Messing lantaarn", "Brass Lantern"),
    "dadelpalm_stam": ("stam", "Dadelpalmstam", "Date Palm Log"),
    "dadelpalm_planken": ("blok", "Dadelpalmplanken", "Date Palm Planks"),
    "dadelpalm_trap": ("trap", "Dadelpalmhouten trap", "Date Palm Stairs"),
    "dadelpalm_plaat": ("plaat", "Dadelpalmhouten plaat", "Date Palm Slab"),
    "dadelpalm_hek": ("hek", "Dadelpalmhek", "Date Palm Fence"),
}
# which texture / full block a shape uses
BASIS_VAN = {"rozenzandsteen_trap": "rozenzandsteen", "rozenzandsteen_plaat": "rozenzandsteen", "rozenzandsteen_muur": "rozenzandsteen",
             "koepeltegel_turkoois_trap": "koepeltegel_turkoois", "dadelpalm_trap": "dadelpalm_planken",
             "dadelpalm_plaat": "dadelpalm_planken", "dadelpalm_hek": "dadelpalm_planken"}
HOUT = {"dadelpalm_stam", "dadelpalm_planken", "dadelpalm_trap", "dadelpalm_plaat", "dadelpalm_hek", "mashrabiya_tralies"}
ZACHT = {"zijden_doek", "zijden_luifel"}
CUTOUT = {"mashrabiya_tralies", "zijden_luifel", "messing_lantaarn"}


def _w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _t(n):
    return f"guhs:block/zijdeland/{n}"


# ================================================================================================ textures (16x16 pixel art)
def _textures():
    from PIL import Image
    os.makedirs(TEXDIR, exist_ok=True)
    R = random.Random(6)

    def img():
        return Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    def mix(a, b, t):
        return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)

    def jit(c, d, r=R):
        k = r.randint(-d, d)
        return tuple(max(0, min(255, v + k)) for v in c[:3]) + (255,)

    def save(im, n):
        im.save(os.path.join(TEXDIR, n + ".png"))

    # --- rose sandstone: soft pink-beige with faint strata, speckles and a bevelled 1px edge
    ZS = (232, 178, 168)
    BAND = [(236, 186, 174), (228, 170, 162), (240, 196, 180), (226, 166, 160)]

    def zandsteen(seed):
        r = random.Random(seed)
        im = img()
        for y in range(16):
            band = BAND[(y // 4) % 4]
            for x in range(16):
                c = jit(band, 5, r)
                if r.random() < 0.06:
                    c = jit((214, 150, 146), 4, r)
                im.putpixel((x, y), c)
        for i in range(16):
            im.putpixel((i, 0), jit((246, 210, 196), 3, r))
            im.putpixel((0, i), jit((244, 204, 192), 3, r))
            im.putpixel((i, 15), jit((200, 138, 136), 3, r))
            im.putpixel((15, i), jit((206, 144, 140), 3, r))
        return im
    save(zandsteen(1), "rozenzandsteen")

    # --- carved: a guh face in relief (round ears, eyes, blush, smile) in a recessed panel
    im = zandsteen(2)
    donker, licht, diep = (186, 122, 124, 255), (250, 218, 206, 255), (210, 148, 148, 255)
    for x in range(2, 14):
        for y in range(2, 14):
            im.putpixel((x, y), jit((222, 162, 156), 3))
    for x in range(2, 14):
        im.putpixel((x, 2), donker)
        im.putpixel((x, 13), licht)
    for y in range(2, 14):
        im.putpixel((2, y), donker)
        im.putpixel((13, y), licht)
    # ears
    for (ex, ey) in ((4, 4), (11, 4)):
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if abs(dx) + abs(dy) < 2:
                    im.putpixel((ex + dx, ey + dy), (240, 200, 190, 255))
        im.putpixel((ex, ey), (228, 150, 160, 255))
    # head outline (circle)
    import math
    for a in range(0, 360, 8):
        x = 7.5 + math.cos(math.radians(a)) * 4.6
        y = 8.6 + math.sin(math.radians(a)) * 4.0
        im.putpixel((int(round(x)), int(round(y))), donker if y < 8.6 else diep)
    for (x, y) in ((6, 8), (9, 8)):
        im.putpixel((x, y), (90, 60, 70, 255))
    im.putpixel((5, 10), (236, 140, 156, 255))
    im.putpixel((10, 10), (236, 140, 156, 255))
    for x in (7, 8):
        im.putpixel((x, 11), (150, 96, 104, 255))
    im.putpixel((6, 10), (150, 96, 104, 255)) if False else None
    save(im, "rozenzandsteen_gebeiteld")

    # --- mosaics
    TURK, TURK2, ROZE, GOUD, WIT, BLAUW = (64, 190, 196), (40, 150, 168), (240, 132, 170), (240, 200, 90), (250, 246, 238), (70, 110, 190)

    def tegelrand(im, kl=GOUD):
        for i in range(16):
            for p in ((i, 0), (0, i), (i, 15), (15, i)):
                im.putpixel(p, kl + (255,))

    # star: 8-point star on white, turquoise rays, pink heart, gold grout
    im = img()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = max(abs(dx), abs(dy))
            r2 = (abs(dx) + abs(dy)) / 1.414
            ster = min(r, r2)
            if ster < 2.2:
                c = ROZE
            elif ster < 5.0:
                c = TURK if (x + y) % 2 else TURK2
            elif ster < 5.8:
                c = GOUD
            else:
                c = WIT if (x // 2 + y // 2) % 3 else (236, 230, 222)
            im.putpixel((x, y), c + (255,))
    for (x, y) in ((0, 0), (15, 0), (0, 15), (15, 15)):
        im.putpixel((x, y), BLAUW + (255,))
    tegelrand(im)
    save(im, "mozaiek_ster")

    # guh: a little guh face in a turquoise frame on pink
    im = img()
    for y in range(16):
        for x in range(16):
            c = (250, 214, 226) if (x + y) % 4 else (246, 200, 216)
            if x in (1, 14) or y in (1, 14):
                c = TURK
            im.putpixel((x, y), c + (255,))
    for (ex, ey) in ((5, 4), (10, 4)):
        for dx in (-1, 0, 1):
            im.putpixel((ex + dx, ey), WIT + (255,))
        im.putpixel((ex, ey - 1), WIT + (255,))
    for y in range(5, 12):
        for x in range(4, 12):
            if ((x - 7.5) / 4.2) ** 2 + ((y - 8.2) / 3.6) ** 2 <= 1:
                im.putpixel((x, y), WIT + (255,))
    im.putpixel((6, 8), (70, 50, 60, 255))
    im.putpixel((9, 8), (70, 50, 60, 255))
    im.putpixel((5, 9), ROZE + (255,))
    im.putpixel((10, 9), ROZE + (255,))
    im.putpixel((7, 10), (180, 90, 110, 255))
    im.putpixel((8, 10), (180, 90, 110, 255))
    tegelrand(im)
    save(im, "mozaiek_guh")

    # flower: rosette (gold heart, pink petals, turquoise leaves) on deep blue
    im = img()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            blad = 4.2 + 1.6 * math.cos(6 * a)
            if d < 1.8:
                c = GOUD
            elif d < blad:
                c = ROZE if d < blad - 1.0 else (220, 100, 146)
            elif d < 7.2 and abs(dx) > 5 and abs(dy) > 5:
                c = TURK
            else:
                c = BLAUW if (x + y) % 2 else (60, 98, 176)
            im.putpixel((x, y), c + (255,))
    tegelrand(im, WIT)
    save(im, "mozaiek_bloem")

    # --- mashrabiya: cherry-pink wooden lattice (diamonds + a star knot), transparent holes; and the pane edge
    im = img()
    HOUTR, HOUTD = (232, 160, 168), (196, 120, 134)
    for y in range(16):
        for x in range(16):
            u, v = (x + y) % 8, (x - y) % 8
            lat = u in (0, 7) or v in (0, 7)
            rand = x in (0, 15) or y in (0, 15)
            knoop = abs(x - 7.5) + abs(y - 7.5) < 2.5
            if rand or lat or knoop:
                c = HOUTD if (rand or (u == 7 or v == 7)) else HOUTR
                if knoop:
                    c = (246, 196, 120)
                im.putpixel((x, y), c + (255,))
    save(im, "mashrabiya_tralies")
    im = img()
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), (HOUTD if x in (7, 8) else HOUTR) + (255,))
    save(im, "mashrabiya_tralies_rand")

    # --- turquoise dome tiles: overlapping fish scales (rows of 4, cells of 8, every other row shifted) with gold rims
    im = img()
    for y in range(16):
        for x in range(16):
            rij = y // 4
            ox = 4 if rij % 2 else 0
            cx = ((x + ox) // 8) * 8 + 3.5 - ox
            dy = y - rij * 4
            d = math.hypot((x - cx) / 4.2, (dy + 1.5) / 4.6)
            if d < 0.78:
                c = mix((150, 232, 228), TURK, min(1, dy / 3.0))[:3]
            elif d < 1.0:
                c = GOUD
            else:
                c = TURK2
            im.putpixel((x, y), jit(c, 3))
    save(im, "koepeltegel_turkoois")

    # --- silk: pink with gold stripes + a sheen
    im = img()
    for y in range(16):
        for x in range(16):
            c = (238, 120, 168)
            if x % 8 in (3, 4):
                c = (246, 206, 110)
            elif x % 8 == 5:
                c = (255, 232, 160)
            if (x + y) % 11 == 0:
                c = mix(c, (255, 255, 255), 0.25)[:3]
            im.putpixel((x, y), c + (255,))
    save(im, "zijden_doek")
    im = img()
    for y in range(16):
        for x in range(16):
            c = (240, 136, 176) if (x // 3) % 2 else (252, 244, 238)
            if y in (0, 15):
                c = (246, 200, 100)
            im.putpixel((x, y), c + (255,))
    save(im, "zijden_luifel")

    # --- date palm: bark with diamond scales (side), rings (end), honey planks
    im = img()
    for y in range(16):
        for x in range(16):
            u, v = (x + y) % 6, (x - y) % 6
            c = (176, 128, 84)
            if u == 0 or v == 0:
                c = (128, 88, 58)
            elif u == 1 or v == 1:
                c = (198, 152, 104)
            im.putpixel((x, y), jit(c, 4))
    save(im, "dadelpalm_stam")
    im = img()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            c = (214, 172, 112) if int(d) % 3 else (184, 140, 88)
            if d > 6.8:
                c = (150, 104, 66)
            im.putpixel((x, y), jit(c, 3))
    save(im, "dadelpalm_stam_top")
    im = img()
    for y in range(16):
        plank = y // 4
        for x in range(16):
            c = [(222, 176, 116), (214, 166, 108), (228, 184, 124), (218, 170, 112)][plank]
            if y % 4 == 3:
                c = (164, 118, 74)
            if (x + plank * 5) % 16 == 0:
                c = (176, 128, 82)
            if R.random() < 0.08:
                c = (204, 156, 100)
            im.putpixel((x, y), jit(c, 3))
    save(im, "dadelpalm_planken")

    # --- brass lantern: the vanilla lantern texture recoloured (metal -> brass, flame -> pink/turquoise glow)
    if os.path.exists(JAR):
        import io
        z = zipfile.ZipFile(JAR)
        van = Image.open(io.BytesIO(z.read("assets/minecraft/textures/block/lantern.png"))).convert("RGBA")
        uit = Image.new("RGBA", van.size, (0, 0, 0, 0))
        for y in range(van.size[1]):
            for x in range(van.size[0]):
                r, g, b, a = van.getpixel((x, y))
                if a == 0:
                    continue
                warm = r - b
                if warm > 60 and r > 150:      # the glow
                    t = (g / 255.0)
                    c = mix((236, 110, 170), (255, 220, 236), t) if (x + y // 16) % 5 else mix((70, 200, 210), (210, 250, 250), t)
                else:                            # metal -> brass
                    l = (r + g + b) / 3 / 255.0
                    c = mix((110, 74, 30), (250, 214, 120), min(1, l * 1.4))
                uit.putpixel((x, y), c[:3] + (a,))
        uit.save(os.path.join(TEXDIR, "messing_lantaarn.png"))
        meta = z.read("assets/minecraft/textures/block/lantern.png.mcmeta")
        with open(os.path.join(TEXDIR, "messing_lantaarn.png.mcmeta"), "wb") as f:
            f.write(meta)


# ================================================================================================ models + blockstates
def _vanilla_bs(naam):
    z = zipfile.ZipFile(JAR)
    return json.loads(z.read(f"assets/minecraft/blockstates/{naam}.json"))


def _herschrijf(bs, oud, nieuw):
    s = json.dumps(bs).replace(f'"minecraft:block/{oud}', f'"guhs:block/{nieuw}')
    return json.loads(s)


def _cut(m, k):
    if k in CUTOUT:
        m["render_type"] = "minecraft:cutout"
    return m


def modellen():
    M = os.path.join(A, "models", "block")
    BS = os.path.join(A, "blockstates")
    jar = os.path.exists(JAR)
    for k, (soort, _, _) in BLOKKEN.items():
        basis = BASIS_VAN.get(k, k)
        tex = _t(basis)
        if soort == "blok":
            _w(os.path.join(M, k + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": tex}})
            _w(os.path.join(BS, k + ".json"), {"variants": {"": {"model": f"guhs:block/{k}"}}})
        elif soort == "trap":
            for suf, par in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
                _w(os.path.join(M, k + suf + ".json"), {"parent": f"minecraft:block/{par}",
                                                         "textures": {"bottom": tex, "top": tex, "side": tex}})
            if jar:
                _w(os.path.join(BS, k + ".json"), _herschrijf(_vanilla_bs("oak_stairs"), "oak_stairs", k))
        elif soort == "plaat":
            for suf, par in (("", "slab"), ("_top", "slab_top")):
                _w(os.path.join(M, k + suf + ".json"), {"parent": f"minecraft:block/{par}",
                                                         "textures": {"bottom": tex, "top": tex, "side": tex}})
            _w(os.path.join(BS, k + ".json"), {"variants": {"type=bottom": {"model": f"guhs:block/{k}"},
                                                            "type=double": {"model": f"guhs:block/{basis}"},
                                                            "type=top": {"model": f"guhs:block/{k}_top"}}})
        elif soort == "muur":
            for suf, par in (("_post", "template_wall_post"), ("_side", "template_wall_side"), ("_side_tall", "template_wall_side_tall"),
                             ("_inventory", "wall_inventory")):
                _w(os.path.join(M, k + suf + ".json"), {"parent": f"minecraft:block/{par}", "textures": {"wall": tex}})
            if jar:
                _w(os.path.join(BS, k + ".json"), _herschrijf(_vanilla_bs("cobblestone_wall"), "cobblestone_wall", k))
        elif soort == "hek":
            for suf, par in (("_post", "fence_post"), ("_side", "fence_side"), ("_inventory", "fence_inventory")):
                _w(os.path.join(M, k + suf + ".json"), {"parent": f"minecraft:block/{par}", "textures": {"texture": tex}})
            if jar:
                _w(os.path.join(BS, k + ".json"), _herschrijf(_vanilla_bs("oak_fence"), "oak_fence", k))
        elif soort == "stam":
            _w(os.path.join(M, k + ".json"), {"parent": "minecraft:block/cube_column", "textures": {"end": _t(k + "_top"), "side": tex}})
            _w(os.path.join(M, k + "_horizontal.json"), {"parent": "minecraft:block/cube_column_horizontal",
                                                         "textures": {"end": _t(k + "_top"), "side": tex}})
            _w(os.path.join(BS, k + ".json"), _herschrijf(_vanilla_bs("oak_log"), "oak_log", k) if jar else {"variants": {
                "axis=x": {"model": f"guhs:block/{k}_horizontal", "x": 90, "y": 90}, "axis=y": {"model": f"guhs:block/{k}"},
                "axis=z": {"model": f"guhs:block/{k}_horizontal", "x": 90}}})
        elif soort == "ruit":
            t = {"pane": tex, "edge": _t(k + "_rand")}
            for suf in ("post", "side", "side_alt", "noside", "noside_alt"):
                _w(os.path.join(M, f"{k}_{suf}.json"), _cut({"parent": f"minecraft:block/template_glass_pane_{suf}", "textures": dict(t)}, k))
            if jar:
                _w(os.path.join(BS, k + ".json"), _herschrijf(_vanilla_bs("glass_pane"), "glass_pane", k))
        elif soort == "luifel":
            # a 1px silk plate at the bottom of the block, no support needed (it may float under a window)
            _w(os.path.join(M, k + ".json"), _cut({
                "parent": "minecraft:block/block", "textures": {"particle": tex, "doek": tex},
                "elements": [{"from": [0, 0, 0], "to": [16, 1, 16], "faces": {
                    "down": {"uv": [0, 0, 16, 16], "texture": "#doek"}, "up": {"uv": [0, 0, 16, 16], "texture": "#doek"},
                    "north": {"uv": [0, 15, 16, 16], "texture": "#doek"}, "south": {"uv": [0, 15, 16, 16], "texture": "#doek"},
                    "west": {"uv": [0, 15, 16, 16], "texture": "#doek"}, "east": {"uv": [0, 15, 16, 16], "texture": "#doek"}}}]}, k))
            _w(os.path.join(BS, k + ".json"), {"variants": {"": {"model": f"guhs:block/{k}"}}})
        elif soort == "lantaarn":
            _w(os.path.join(M, k + ".json"), _cut({"parent": "minecraft:block/template_lantern", "textures": {"lantern": tex}}, k))
            _w(os.path.join(M, k + "_hanging.json"), _cut({"parent": "minecraft:block/template_hanging_lantern", "textures": {"lantern": tex}}, k))
            _w(os.path.join(BS, k + ".json"), {"variants": {"hanging=false": {"model": f"guhs:block/{k}"},
                                                            "hanging=true": {"model": f"guhs:block/{k}_hanging"}}})


def _tag(pad, namen, item=False):
    full = os.path.join(RES, "data", "minecraft", "tags", "item" if item else "block", *pad.split("/")) + ".json"
    d = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
    for n in namen:
        if f"guhs:{n}" not in d["values"]:
            d["values"].append(f"guhs:{n}")
    _w(full, d)


def extra():
    """Item models, client items, loot tables, tags, recipes."""
    for k, (soort, _, _) in BLOKKEN.items():
        if soort in ("muur", "hek"):
            model = f"guhs:block/{k}_inventory"
        elif soort in ("ruit", "lantaarn"):
            _w(os.path.join(A, "models", "item", k + ".json"), {"parent": "minecraft:item/generated",
                                                                "textures": {"layer0": _t(k) if soort == "ruit" else f"guhs:item/{k}"}})
            model = None
        else:
            model = f"guhs:block/{k}"
        if model:
            _w(os.path.join(A, "models", "item", k + ".json"), {"parent": model})
        _w(os.path.join(A, "items", k + ".json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{k}"}})
        entry = {"type": "minecraft:item", "name": f"guhs:{k}"}
        if soort == "plaat":
            entry["functions"] = [{"function": "minecraft:set_count", "count": 2, "add": False,
                                   "conditions": [{"condition": "minecraft:block_state_property", "block": f"guhs:{k}",
                                                   "properties": {"type": "double"}}]},
                                  {"function": "minecraft:explosion_decay"}]
        pool = {"rolls": 1, "bonus_rolls": 0, "entries": [entry], "conditions": [{"condition": "minecraft:survives_explosion"}]}
        _w(os.path.join(D, "loot_table", "blocks", k + ".json"), {"type": "minecraft:block", "pools": [pool]})
    # the lantern's inventory icon: a small brass lantern sprite (item texture)
    try:
        from PIL import Image
        src = os.path.join(TEXDIR, "messing_lantaarn.png")
        if os.path.exists(src):
            lt = Image.open(src).convert("RGBA")
            ic = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
            body = lt.crop((0, 2, 6, 9)).resize((8, 10), Image.NEAREST)
            top = lt.crop((0, 0, 4, 2)).resize((6, 2), Image.NEAREST)
            ic.paste(top, (5, 2))
            ic.paste(body, (4, 4))
            os.makedirs(os.path.join(A, "textures", "item"), exist_ok=True)
            ic.save(os.path.join(A, "textures", "item", "messing_lantaarn.png"))
    except Exception as e:  # noqa
        print("vadsrijk_u_z6blokken: lantern icon skipped:", e)
    soorten = {k: v[0] for k, v in BLOKKEN.items()}
    trap = [k for k, s in soorten.items() if s == "trap"]
    plaat = [k for k, s in soorten.items() if s == "plaat"]
    for item in (False, True):
        _tag("stairs", trap, item)
        _tag("slabs", plaat, item)
        _tag("walls", ["rozenzandsteen_muur"], item)
        _tag("fences", ["dadelpalm_hek"], item)
        _tag("wooden_fences", ["dadelpalm_hek"], item)
        _tag("wooden_stairs", ["dadelpalm_trap"], item)
        _tag("wooden_slabs", ["dadelpalm_plaat"], item)
        _tag("planks", ["dadelpalm_planken"], item)
        _tag("logs", ["dadelpalm_stam"], item)
    _tag("mineable/pickaxe", [k for k in BLOKKEN if k not in HOUT and k not in ZACHT])
    _tag("mineable/axe", [k for k in BLOKKEN if k in HOUT])
    rec = os.path.join(D, "recipe")

    def r(naam, obj):
        _w(os.path.join(rec, f"zijdeland_{naam}.json"), obj)

    def los(k, ingr, n):
        r(k, {"type": "minecraft:crafting_shapeless", "category": "building", "ingredients": ingr, "result": {"id": f"guhs:{k}", "count": n}})

    def vorm(k, pat, key, n, naam=None):
        r(naam or k, {"type": "minecraft:crafting_shaped", "category": "building", "pattern": pat, "key": key,
                      "result": {"id": f"guhs:{k}", "count": n}})

    def zaag(van, naar, n=1):
        r(f"{naar}_uit_{van}", {"type": "minecraft:stonecutting", "ingredient": f"guhs:{van}", "result": {"id": f"guhs:{naar}", "count": n}})
    los("rozenzandsteen", ["minecraft:sandstone", "minecraft:pink_terracotta"], 2)
    for k in ("rozenzandsteen_trap", "rozenzandsteen_muur", "rozenzandsteen_gebeiteld"):
        zaag("rozenzandsteen", k)
    zaag("rozenzandsteen", "rozenzandsteen_plaat", 2)
    vorm("rozenzandsteen_trap", ["S  ", "SS ", "SSS"], {"S": "guhs:rozenzandsteen"}, 4)
    vorm("rozenzandsteen_plaat", ["SSS"], {"S": "guhs:rozenzandsteen"}, 6)
    vorm("rozenzandsteen_muur", ["SSS", "SSS"], {"S": "guhs:rozenzandsteen"}, 6)
    los("mozaiek_ster", ["minecraft:cyan_terracotta", "minecraft:white_terracotta", "minecraft:gold_nugget"], 4)
    for a in ("mozaiek_ster", "mozaiek_guh", "mozaiek_bloem"):
        for b in ("mozaiek_ster", "mozaiek_guh", "mozaiek_bloem"):
            if a != b:
                zaag(a, b)
    vorm("mashrabiya_tralies", ["SPS", "SPS"], {"S": "minecraft:stick", "P": "minecraft:cherry_planks"}, 8)
    los("koepeltegel_turkoois", ["minecraft:cyan_glazed_terracotta", "minecraft:gold_nugget"], 2)
    zaag("koepeltegel_turkoois", "koepeltegel_turkoois_trap")
    los("zijden_doek", ["minecraft:pink_wool", "minecraft:string", "minecraft:gold_nugget"], 2)
    vorm("zijden_luifel", ["DD"], {"D": "guhs:zijden_doek"}, 6)
    vorm("messing_lantaarn", ["NNN", "NTN", "NNN"], {"N": "minecraft:gold_nugget", "T": "minecraft:torch"}, 1)
    los("dadelpalm_stam", ["minecraft:jungle_log", "minecraft:sand"], 1)
    los("dadelpalm_planken", ["guhs:dadelpalm_stam"], 4)
    vorm("dadelpalm_trap", ["P  ", "PP ", "PPP"], {"P": "guhs:dadelpalm_planken"}, 4)
    vorm("dadelpalm_plaat", ["PPP"], {"P": "guhs:dadelpalm_planken"}, 6)
    vorm("dadelpalm_hek", ["PSP", "PSP"], {"P": "guhs:dadelpalm_planken", "S": "minecraft:stick"}, 3)


def teksten():
    return {f"block.guhs.{k}": (nl, en) for k, (_, nl, en) in BLOKKEN.items()}


def bouw():
    try:
        _textures()
    except ImportError:
        if not os.path.exists(os.path.join(TEXDIR, "rozenzandsteen.png")):
            raise
    modellen()


def build(h):
    bouw()
    extra()
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)    # make_v2.lang(key, en, nl)


def _direct():
    bouw()
    extra()
    for i, taal in enumerate(("nl_nl", "en_us")):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in teksten().items()})
        _w(p, data)
    print(f"vadsrijk_u_z6blokken: {len(BLOKKEN)} blocks, {len(teksten())} texts")


if __name__ == "__main__":
    _direct()
