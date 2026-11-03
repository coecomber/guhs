"""Bouwdoos voor vibe-schetsen: een leeg volume, bouwhulpjes en een render naar PNG.

Lokale coordinaten: x oost, y omhoog, z zuid. Blokken zonder namespace zijn guhs-blokken.
"""
import os, sys, math, time
import numpy as np
from PIL import Image, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
sys.path.insert(0, HERE)
import mcrender as R, mcextra as E

JARS = [os.path.abspath(os.path.join(HERE, "..", "..", "..", "..", "..", "renders-erfenis", "_bron", "jars", "guhs-assets-1.8.1.zip")),
        os.path.join(os.environ.get("APPDATA", ""),
                     "PrismLauncher/libraries/com/mojang/minecraft/26.1.2/minecraft-26.1.2-client.jar")]
OUT = os.path.abspath(os.path.join(HERE, "..", ".."))  # renders-vadsrijk/
EMISSIVE = {"guhs:kaasmos", "guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint", "guhs:sterrenlantaarn",
            "guhs:guh_kristal_lamp", "guhs:uienlicht", "guhs:tiki_schelpjeslampion", "guhs:glimtegel",
            "minecraft:pearlescent_froglight", "minecraft:ochre_froglight", "minecraft:verdant_froglight",
            "minecraft:sea_lantern", "minecraft:glowstone", "minecraft:shroomlight", "minecraft:lantern",
            "minecraft:end_rod"}
_res = _bk = None


def res():
    global _res, _bk
    if _res is None:
        _res = R.Res(JARS)
        _bk = R.Baker(_res)
    return _res, _bk


def has_block(name):
    """Bestaat dit blok (blockstate-bestand) in de jars?"""
    if ":" not in name:
        name = "guhs:" + name
    ns, p = name.split("[")[0].split(":")
    return res()[0].read(f"assets/{ns}/blockstates/{p}.json") is not None


def state_props(name):
    """Welke blockstate-varianten kent dit blok? (handig om de juiste properties te kiezen)"""
    if ":" not in name:
        name = "guhs:" + name
    bs = res()[0].json(name, "blockstates")
    if bs is None:
        return None
    return list(bs.get("variants", {}).keys())[:40] or ["multipart"]


class Scene:
    def __init__(self, X=64, Y=48, Z=64):
        self.pal = ["minecraft:air"]
        self.idx = {"minecraft:air": 0}
        self.v = np.zeros((X, Y, Z), np.uint16)
        self.guhs = []
        self.roof = set()

    def id(self, s):
        if ":" not in s:
            s = "guhs:" + s
        if s not in self.idx:
            self.idx[s] = len(self.pal)
            self.pal.append(s)
        return self.idx[s]

    def inb(self, x, y, z):
        X, Y, Z = self.v.shape
        return 0 <= x < X and 0 <= y < Y and 0 <= z < Z

    def set(self, x, y, z, s, roof=False, keep=False):
        """keep=True zet alleen in lucht. roof=True verdwijnt in de doorkijk-render."""
        x, y, z = int(x), int(y), int(z)
        if not self.inb(x, y, z) or (keep and self.v[x, y, z] != 0):
            return
        self.v[x, y, z] = self.id(s)
        (self.roof.add if roof else self.roof.discard)((x, y, z))

    def get(self, x, y, z):
        return self.pal[self.v[x, y, z]] if self.inb(x, y, z) else "minecraft:air"

    def is_air(self, x, y, z):
        return self.get(x, y, z) == "minecraft:air"

    def fill(self, x1, y1, z1, x2, y2, z2, s, roof=False, keep=False):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, s, roof, keep)

    def walls(self, x1, y1, z1, x2, y2, z2, s, roof=False):
        """Alleen de vier wanden van een doos."""
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                if x in (x1, x2) or z in (z1, z2):
                    for y in range(min(y1, y2), max(y1, y2) + 1):
                        self.set(x, y, z, s, roof)

    def air(self, x1, y1, z1, x2, y2, z2):
        self.fill(x1, y1, z1, x2, y2, z2, "minecraft:air")

    def shape(self, fn, s, roof=False, keep=False):
        """fn(x, y, z) -> bool over het hele volume."""
        X, Y, Z = self.v.shape
        for x in range(X):
            for y in range(Y):
                for z in range(Z):
                    if fn(x, y, z):
                        self.set(x, y, z, s, roof, keep)

    def ellipsoid(self, cx, cy, cz, rx, ry, rz, s, hollow=0.0, roof=False, keep=False, ymin=None):
        """Bol/ei rond (cx,cy,cz). hollow = wanddikte in blokken (0 = massief). ymin kapt de onderkant af."""
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            for y in range(int(cy - ry - 1), int(cy + ry + 2)):
                if ymin is not None and y < ymin:
                    continue
                for z in range(int(cz - rz - 1), int(cz + rz + 2)):
                    d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
                    if d > 1:
                        continue
                    if hollow:
                        d2 = ((x - cx) / (rx - hollow)) ** 2 + ((y - cy) / (ry - hollow)) ** 2 + \
                             ((z - cz) / (rz - hollow)) ** 2
                        if d2 <= 1:
                            continue
                    self.set(x, y, z, s, roof, keep)

    def cylinder(self, cx, cz, r, y1, y2, s, hollow=0.0, roof=False, keep=False):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            for z in range(int(cz - r - 1), int(cz + r + 2)):
                d = math.hypot(x - cx, z - cz)
                if d <= r and not (hollow and d <= r - hollow):
                    for y in range(y1, y2 + 1):
                        self.set(x, y, z, s, roof, keep)

    def ground(self, height_fn, top="guhs:knuffelgras", under="minecraft:dirt", deep="minecraft:stone", depth=3):
        """Terrein: height_fn(x, z) -> y van het bovenste blok (of None voor niets)."""
        X, Y, Z = self.v.shape
        for x in range(X):
            for z in range(Z):
                h = height_fn(x, z)
                if h is None:
                    continue
                h = int(h)
                for y in range(0, h + 1):
                    self.set(x, y, z, top if y == h else under if y >= h - depth else deep)

    def top_y(self, x, z):
        """Hoogste niet-lucht blok in de kolom (of -1)."""
        col = self.v[x, :, z]
        nz = np.nonzero(col)[0]
        return int(nz.max()) if len(nz) else -1

    def tree(self, x, y, z, h=4, r=2, log="guhs:pluizenboom_stam[axis=y]", leaves="guhs:pluizenboom_bladeren"):
        for yy in range(y, y + h):
            self.set(x, yy, z, log)
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                for dy in range(-1, 2):
                    d = abs(dx) + abs(dz) + abs(dy) * 1.5
                    if d <= r + 0.6 and (dx, dz) != (0, 0):
                        self.set(x + dx, y + h + dy - 1, z + dz, leaves, keep=True)
        self.set(x, y + h, z, leaves)
        self.set(x, y + h + 1, z, leaves, keep=True)

    def guh(self, x, y, z, yaw=180, skin="guh", sitting=False):
        """Guh-figuurtje; x/z mogen .5 zijn, y = bovenkant van de vloer. skin = texture in guhs:entity/."""
        self.guhs.append((x, y, z, yaw, skin, sitting))

    def fix_connections(self):
        """Hekken, muurtjes, glaspanelen en tralies laten aansluiten zoals in het spel."""
        def fam(n):
            n = n.split("[")[0]
            if n.endswith(("_hek", "_fence")) or "_hekje_" in n:
                return "fence"
            if n.endswith(("_muur", "_wall")) and "verkoolde" not in n:
                return "wall"
            if n.endswith(("glass_pane", "iron_bars", "_tralies", "_ruit")):
                return "pane"
            return None
        X, Y, Z = self.v.shape
        dirs = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}
        b = R.Baker(res()[0]) if False else res()[1]
        solid = {}

        def is_solid(s):
            if s not in solid:
                solid[s] = s != "minecraft:air" and b.bake(s).full_opaque
            return solid[s]
        todo = [(x, y, z) for (x, y, z) in np.argwhere(self.v > 0) if fam(self.pal[self.v[x, y, z]])]
        new = []
        for (x, y, z) in todo:
            s = self.pal[self.v[x, y, z]]
            f, name = fam(s), s.split("[")[0]
            con = {}
            for d, (dx, dz) in dirs.items():
                o = self.get(x + dx, y, z + dz)
                fo = fam(o)
                con[d] = is_solid(o) or fo == f or (f == "fence" and o.split("[")[0].endswith(("_poort", "_gate"))) \
                    or (f == "wall" and fo == "pane") or (f == "pane" and fo == "wall")
            if f == "wall":
                above = not self.is_air(x, y + 1, z)
                straight = (con["north"] and con["south"] and not con["east"] and not con["west"]) or \
                           (con["east"] and con["west"] and not con["north"] and not con["south"])
                up = "false" if straight and not (above and fam(self.get(x, y + 1, z)) != "wall") else "true"
                props = ",".join(f"{d}={'low' if con[d] else 'none'}" for d in ("east", "north", "south", "west"))
                st = f"{name}[{props},up={up},waterlogged=false]"
            else:
                props = ",".join(f"{d}={'true' if con[d] else 'false'}" for d in ("east", "north", "south", "west"))
                st = f"{name}[{props},waterlogged=false]"
            new.append((x, y, z, st))
        for (x, y, z, st) in new:
            self.v[x, y, z] = self.id(st)


def stairs(block, facing, half="bottom", shape="straight"):
    """Trap-state. facing = de kant waar de HOGE rug zit (zoals in het spel)."""
    return f"{block}[facing={facing},half={half},shape={shape},waterlogged=false]"


def slab(block, typ="bottom"):
    return f"{block}[type={typ},waterlogged=false]"


def gradient(w, h, top, bottom):
    a = np.linspace(0, 1, h)[:, None, None]
    g = np.array(top)[None, None] * (1 - a) + np.array(bottom)[None, None] * a
    return Image.fromarray(np.repeat(g, w, 1).astype(np.uint8), "RGB")


def render(S, name, yaw=35, pitch=30, scale=22, noroof=False, cut=None, sky=((214, 228, 250), (252, 238, 244)),
           ss=2, emissive=(), extra=()):
    """Rendert de scene naar OUT/<name>.png.

    yaw: 45 = camera in het zuidoosten, -45 = zuidwesten, 135 = noordoosten. pitch 30 = iso, 50+ = vogelvlucht.
    noroof: laat alles met roof=True weg (doorkijk). cut=(as, n): laat alles weg met x/y/z > n (doorsnede),
    bv. cut=("z", 30) snijdt de zuidkant eraf, cut=("y", 12) haalt alles boven y 12 weg.
    """
    r, bk = res()
    S.fix_connections()
    vol = S.v.copy()
    if noroof:
        for (x, y, z) in S.roof:
            vol[x, y, z] = 0
    guhs = list(S.guhs)
    if cut:
        ax, n = cut
        if ax == "x":
            vol[n + 1:, :, :] = 0
        elif ax == "y":
            vol[:, n + 1:, :] = 0
        else:
            vol[:, :, n + 1:] = 0
        i = "xyz".index(ax)
        guhs = [g for g in guhs if g[i] <= n + 1]
    extras, hide = E.special_extras(vol, S.pal, bk)
    for (x, y, z, yaw_g, skin, sitting) in guhs:
        extras += E.guh(r, x, y, z, yaw_g, skin=skin, scale=0.8, sitting=sitting)
    extras = list(extras) + list(extra)
    cam = R.Camera(yaw, pitch, scale * ss)
    t = time.time()
    im, _ = R.render(vol, S.pal, cam, r, bk, hide=lambda s: s in hide, extras=extras, margin=30 * ss,
                     emissive=EMISSIVE | set(emissive))
    im = im.resize((im.width // ss, im.height // ss), Image.LANCZOS)
    bg = gradient(im.width, im.height, *sky)
    sh = Image.new("RGBA", im.size, (70, 50, 90, 0))
    a = im.split()[3].filter(ImageFilter.GaussianBlur(14))
    sh.putalpha(a.point(lambda v: int(v * 0.3)))
    bg.paste(sh, (10, 16), sh)
    bg.paste(im, (0, 0), im)
    path = os.path.join(OUT, name + ".png")
    bg.save(path)
    print(name, "%.1fs" % (time.time() - t), bg.size, path)
    return path
