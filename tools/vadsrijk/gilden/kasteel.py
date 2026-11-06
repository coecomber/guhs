"""U-GILDEN: the Bondenvallei structures (pink-guh style), written as vanilla structure templates (.nbt) + marker JSON.

  plein.nbt       56 x .. x 56   the valley square: paving, fountain, the walk-in gate back to Hoogvads (north), lamp posts
  kavel.nbt       72 x .. x 72   one plot cell: floating island, cherry boardwalk roads north + west, hedge round the 64x64 plot
  kasteel_1..5    64 x .. x 64   the castle levels (y 0 = the ground layer):
                  1 Wachttoren   tower with the teleport stone (lodestone) + the vault (chest), banner room, roof deck
                  2 Torenhof     + ring wall, gatehouse, Trofeeenzaal (trophy pedestals)
                  3 Burcht       + NW/NE corner towers, west wing: smidse (smithing table, anvil) + keuken (smoker)
                  4 Vesting      + SW/SE corner towers, east wing: naaiatelier (loom) + brouwerij (brewing stand), garden + fountain
                  5 Knuffelslot  the great keep (3 floors, hall with chandeliers, throne, spires) instead of the tower

Markers (data/guhs/vr_gilden/kasteel_<n>.json, plot-relative): "aankomst" [x,y,z] (where the teleport puts you), "trofee"
[[x,y,z]...] (the standing spot on top of each trophy pedestal).

Rules kept (LEESMIJ / brief): every fence/pane/bar/wall/chain is written WITH its connection states (own pass below; the Java side
also places with known shape OFF so vanilla recomputes them), walkable: every floor change is a stair, doors are open arches
reachable from the path, the water is 1 deep, interiors have real floors, nothing on the paths.

Run from the project root:  python tools/vadsrijk/gilden/kasteel.py
"""
import json
import math
import os
import random
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "vadsrijk", "wereld"))
import nbtsnel  # noqa: E402

UIT = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "gilden")
MARK = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "vr_gilden")

# ---- palette ---------------------------------------------------------------------------------------------------------------
FUND = "minecraft:stone_bricks"
FUND2 = "minecraft:chiseled_stone_bricks"
MUUR = "minecraft:pink_terracotta"
BAND = "minecraft:quartz_bricks"
HOEK = "minecraft:quartz_pillar"
VLOER = "minecraft:cherry_planks"
TEGEL = "minecraft:smooth_quartz"
DAK = "minecraft:purpur_block"
DAKTRAP = "minecraft:purpur_stairs"
GLAS = "minecraft:pink_stained_glass_pane"
HEK = "minecraft:cherry_fence"
KETTING = "minecraft:iron_chain"
LAMP = "minecraft:lantern"
GRAS = "minecraft:grass_block"
PAD = "minecraft:dirt_path"

ZIJ = {"north": (0, -1), "east": (1, 0), "south": (0, 1), "west": (-1, 0)}
TEGEN = {"north": "south", "south": "north", "east": "west", "west": "east"}


class Bouw:
    """A sparse volume: (x, y, z) -> (name, props)."""

    def __init__(self, sx, sy, sz):
        self.s = (sx, sy, sz)
        self.b = {}
        self.nbt = {}

    def zet(self, x, y, z, naam, **props):
        if 0 <= x < self.s[0] and 0 <= y < self.s[1] and 0 <= z < self.s[2]:
            if naam is None:
                self.b.pop((x, y, z), None)
            else:
                self.b[(x, y, z)] = (naam, {k: str(v).lower() if isinstance(v, bool) else str(v) for k, v in props.items()})

    def lucht(self, x, y, z):
        self.b[(x, y, z)] = ("minecraft:air", {})

    def get(self, x, y, z):
        return self.b.get((x, y, z), (None, {}))[0]

    def doos(self, x1, y1, z1, x2, y2, z2, naam, **props):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.zet(x, y, z, naam, **props)

    def leeg(self, x1, y1, z1, x2, y2, z2):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.lucht(x, y, z)

    def wis(self, x1, y1, z1, x2, y2, z2):
        """Remove (not air: nothing written there)."""
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.b.pop((x, y, z), None)


# ---- the connection pass ---------------------------------------------------------------------------------------------------
NIET_VOL = ("stairs", "slab", "fence", "pane", "bars", "wall", "chain", "lantern", "banner", "torch", "candle", "carpet", "button",
            "flower", "tulip", "poppy", "dandelion", "orchid", "allium", "bluet", "daisy", "lily", "rose", "_bush", "grass", "fern",
            "sapling", "azalea", "rod", "pot", "door", "trapdoor", "sign", "leaves", "air", "water", "lodestone", "chest", "table",
            "smoker", "loom", "brewing", "anvil", "cauldron", "furnace", "barrel", "end_rod", "dirt_path", "campfire", "bell",
            "pink_petals", "leaf_litter", "head", "skull")


def vol(naam):
    if naam is None:
        return False
    n = naam.split(":")[1]
    return not any(t in n for t in NIET_VOL)


def soort(naam):
    if naam is None:
        return None
    n = naam.split(":")[1]
    if n.endswith("_fence_gate"):
        return "poort"
    if n.endswith("_fence"):
        return "hek"
    if n.endswith("_pane") or n == "iron_bars":
        return "ruit"
    if n.endswith("_wall") and "banner" not in n and "sign" not in n and "torch" not in n:
        return "muur"
    if n.endswith("chain"):
        return "ketting"
    return None


def verbind(v):
    """Writes north/east/south/west (+ up for walls, axis for chains) on every fence, pane, bar, wall and chain."""
    n = 0
    for (x, y, z), (naam, props) in list(v.b.items()):
        s = soort(naam)
        if s in ("hek", "ruit", "muur"):
            p = dict(props)
            for zij, (dx, dz) in ZIJ.items():
                buur = v.get(x + dx, y, z + dz)
                bs = soort(buur)
                if s == "hek":
                    ja = bs in ("hek", "poort") or vol(buur)
                elif s == "ruit":
                    ja = bs in ("ruit", "muur") or vol(buur)
                else:
                    ja = bs in ("muur", "ruit", "poort") or vol(buur)
                if s == "muur":
                    p[zij] = ("tall" if vol(v.get(x, y + 1, z)) else "low") if ja else "none"
                else:
                    p[zij] = "true" if ja else "false"
            if s == "muur":
                p["up"] = "true"
            p.setdefault("waterlogged", "false")
            v.b[(x, y, z)] = (naam, p)
            n += 1
        elif s == "ketting":
            p = dict(props)
            if soort(v.get(x, y + 1, z)) == "ketting" or soort(v.get(x, y - 1, z)) == "ketting" or not p.get("axis"):
                p["axis"] = "y"
            p.setdefault("waterlogged", "false")
            v.b[(x, y, z)] = (naam, p)
            n += 1
    return n


# ---- writing ---------------------------------------------------------------------------------------------------------------

def centraal(v):
    """V-INT: u-hekken's shared connection pass (tools/vadsrijk/verbind.py) over the whole template, after the own pass: the same
    rules as the map tiles (wall heights, chains, garlands, gap fill on diagonal steps). Returns the number of changed cells."""
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    import verbind
    pal, idx = ["minecraft:air"], {"minecraft:air": 0}

    def sid(bn, props):
        return bn + ("[" + ",".join(f"{k}={w}" for k, w in sorted(props.items())) + "]" if props else "")

    def id_fn(st):
        if st not in idx:
            idx[st] = len(pal)
            pal.append(st)
        return idx[st]

    g = np.zeros(v.s, np.int32)
    for (x, y, z), (bn, props) in v.b.items():
        g[x, y, z] = id_fn(sid(bn, props))
    oud = g.copy()
    n = verbind.verbind_vol(g, pal, id_fn)
    for x, y, z in zip(*np.nonzero(g != oud)):
        st = pal[int(g[x, y, z])]
        if "[" in st:
            bn, rest = st[:-1].split("[", 1)
            props = dict(kv.split("=", 1) for kv in rest.split(",") if kv)
        else:
            bn, props = st, {}
        v.b[(int(x), int(y), int(z))] = (bn, props)
    return n


def schrijf(v, naam):
    verbind(v)
    print(f"  {naam}: central verbind pass changed {centraal(v)} cells")   # V-INT
    pal = []
    idx = {}
    xs, ys, zs, st = [], [], [], []
    for (x, y, z), (bn, props) in sorted(v.b.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
        k = (bn, tuple(sorted(props.items())))
        if k not in idx:
            idx[k] = len(pal)
            pal.append((bn, dict(props)))
        xs.append(x)
        ys.append(y)
        zs.append(z)
        st.append(idx[k])
    os.makedirs(UIT, exist_ok=True)
    pad = os.path.join(UIT, naam + ".nbt")
    n = nbtsnel.schrijf(pad, list(v.s), pal, np.array(xs, np.int32), np.array(ys, np.int32), np.array(zs, np.int32),
                        np.array(st, np.int32), extra_nbt=v.nbt)
    print(f"  {naam}.nbt: {n} blocks, {len(pal)} states, size {v.s}")


# ---- small parts -----------------------------------------------------------------------------------------------------------

def lantaarnpaal(v, x, y, z, hoog=3):
    for i in range(hoog):
        v.zet(x, y + i, z, HEK)
    v.zet(x, y + hoog, z, LAMP, hanging=False, waterlogged=False)


def hanglamp(v, x, y_plafond, z, lengte=2):
    """A lantern on a chain under the ceiling block at y_plafond."""
    for i in range(1, lengte + 1):
        v.zet(x, y_plafond - i, z, KETTING, axis="y")
    v.zet(x, y_plafond - lengte - 1, z, LAMP, hanging=True, waterlogged=False)


def kroonluchter(v, x, y_plafond, z, lengte=3):
    """A chandelier: chain, a cross of cherry fence arms, lanterns hanging under the arm ends, candles on top."""
    for i in range(1, lengte + 1):
        v.zet(x, y_plafond - i, z, KETTING, axis="y")
    y = y_plafond - lengte - 1
    v.zet(x, y, z, HEK)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        v.zet(x + dx, y, z + dz, HEK)
        v.zet(x + 2 * dx, y, z + 2 * dz, HEK)
        v.zet(x + 2 * dx, y - 1, z + 2 * dz, LAMP, hanging=True, waterlogged=False)
        v.zet(x + 2 * dx, y + 1, z + 2 * dz, "minecraft:pink_candle", candles=3, lit=True, waterlogged=False)
    v.zet(x, y - 1, z, LAMP, hanging=True, waterlogged=False)


def piramide(v, x1, z1, x2, z2, y, steil=False, top=True):
    """A roof over the box x1..x2 / z1..z2 starting at y: stairs on the bottom ring, purpur inside, steil = spire (2 rows per
    step; an int = that many rows per step: 3 = a tall pointy spire)."""
    i = 0
    yy = y
    while x1 + i <= x2 - i and z1 + i <= z2 - i:
        a1, a2, b1, b2 = x1 + i, x2 - i, z1 + i, z2 - i
        hoog = (steil if steil is not True else 2) if steil and i > 0 else 1
        for h in range(hoog):
            for x in range(a1, a2 + 1):
                for z in range(b1, b2 + 1):
                    rand = x in (a1, a2) or z in (b1, b2)
                    if not rand:
                        continue
                    if h == 0 and (i == 0 or not steil) and not ((x in (a1, a2)) and (z in (b1, b2))):
                        f = "south" if z == b1 else "north" if z == b2 else "east" if x == a1 else "west"
                        v.zet(x, yy, z, DAKTRAP, facing=f, half="bottom", shape="straight", waterlogged=False)
                    else:
                        v.zet(x, yy, z, DAK)
            yy += 1
        i += 1
    if top:
        mx, mz = (x1 + x2) // 2, (z1 + z2) // 2
        v.zet(mx, yy, mz, "minecraft:gold_block")
        v.zet(mx, yy + 1, mz, "minecraft:end_rod", facing="up")
    return yy


def kantelen(v, x1, z1, x2, z2, y, naam=MUUR):
    """Merlons on the edge ring of a box at y (every other block)."""
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            if (x in (x1, x2) or z in (z1, z2)) and (x + z) % 2 == 0:
                v.zet(x, y, z, naam)


def ramen(v, x1, z1, x2, z2, y, hoog=2):
    """A pane window in the middle of each face of a box (y..y+hoog-1)."""
    mx, mz = (x1 + x2) // 2, (z1 + z2) // 2
    for h in range(hoog):
        for (x, z) in ((mx, z1), (mx, z2), (x1, mz), (x2, mz)):
            v.zet(x, y + h, z, GLAS)


def wandbanier(v, x, y, z, facing):
    v.zet(x, y, z, "minecraft:white_wall_banner", facing=facing)


def toren(v, x0, z0, w, verdiepingen, vh=6, deur="north", trap=True, dak="dek", inhoud=None):
    """A square tower: foundation + pink walls with quartz corners and bands, floors every vh, a straight stair along the east
    inside wall to every floor (with a railing), windows, a hanging lantern per floor, roof deck with merlons (dak="dek") or a
    spire ("spits") or closed (no door: dak + deur=None). Returns the top y."""
    x1, z1, x2, z2 = x0, z0, x0 + w - 1, z0 + w - 1
    top = verdiepingen * vh
    # walls
    for y in range(0, top + 1):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                rand = x in (x1, x2) or z in (z1, z2)
                hoek = x in (x1, x2) and z in (z1, z2)
                if y == 0:
                    v.zet(x, y, z, FUND if rand else VLOER)
                elif rand:
                    v.zet(x, y, z, HOEK if hoek else (BAND if y % vh == 0 else (FUND if y == 1 else MUUR)), **({"axis": "y"} if hoek else {}))
                else:
                    v.lucht(x, y, z)
    # floors
    for f in range(1, verdiepingen + 1):
        y = f * vh
        for x in range(x1 + 1, x2):
            for z in range(z1 + 1, z2):
                v.zet(x, y, z, VLOER)
    # stairs (east inside wall, going north), opening + railing above
    if trap and deur:
        tx = x2 - 1
        for f in range(verdiepingen):
            yf = f * vh
            for k in range(vh - 1):
                zz = z2 - 2 - k
                v.zet(tx, yf + 1 + k, zz, "minecraft:cherry_stairs", facing="north", half="bottom", shape="straight", waterlogged=False)
                # (under the stair: solid, so it reads as a staircase)
                for yy in range(yf + 1, yf + 1 + k):
                    v.zet(tx, yy, zz, VLOER)
            for k in range(vh - 1):
                v.lucht(tx, yf + vh, z2 - 2 - k)
            for k in range(vh - 1):
                v.zet(tx - 1, yf + vh + 1, z2 - 2 - k, HEK)
            v.zet(tx, yf + vh + 1, z2 - 1, HEK)
    # windows + lights
    for f in range(verdiepingen):
        ramen(v, x1, z1, x2, z2, f * vh + 2, 2)
        hanglamp(v, (x1 + x2) // 2 - 1, (f + 1) * vh, (z1 + z2) // 2 - 1, 1)
    # door (an open arch, 3 high)
    if deur:
        dx, dz = ZIJ[deur]
        mx, mz = (x1 + x2) // 2, (z1 + z2) // 2
        px, pz = (mx, z1) if deur == "north" else (mx, z2) if deur == "south" else (x1, mz) if deur == "west" else (x2, mz)
        for y in (1, 2, 3):
            v.lucht(px, y, pz)
            if deur in ("north", "south"):
                v.lucht(px - 1, y, pz) if w >= 11 else None
                v.lucht(px + 1, y, pz) if w >= 11 else None
        v.zet(px, 4, pz, FUND2)
        # a wall banner above the door, a lantern each side outside
        bx, bz = px + dx, pz + dz
        wandbanier(v, bx, 5, bz, deur)
    # roof
    if dak == "dek":
        kantelen(v, x1, z1, x2, z2, top + 1)
        # corner turrets with little spires
        for (cx, cz) in ((x1, z1), (x2 - 1, z1), (x1, z2 - 1), (x2 - 1, z2 - 1)):
            v.doos(cx, top + 1, cz, cx + 1, top + 2, cz + 1, BAND)
            piramide(v, cx, cz, cx + 1, cz + 1, top + 3, steil=True)
        # flag in the middle
        mx, mz = (x1 + x2) // 2, (z1 + z2) // 2
        for i in range(1, 5):
            v.zet(mx, top + i, mz, HEK)
        v.zet(mx, top + 5, mz, "minecraft:white_banner", rotation=0)
        top += 6
    elif dak == "spits":
        kantelen(v, x1, z1, x2, z2, top + 1, BAND)
        top = piramide(v, x1 + 1, z1 + 1, x2 - 1, z2 - 1, top + 1, steil=3)
    if inhoud:
        inhoud(v, x1, z1, x2, z2)
    return top


def muurring(v, x1, z1, x2, z2, hoog=7, poort=None):
    """A ring wall with merlons and quartz buttresses; poort = (x_mid, breed) opening in the north wall."""
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            if not (x in (x1, x2) or z in (z1, z2)):
                continue
            for y in range(0, hoog + 1):
                v.zet(x, y, z, FUND if y <= 1 else (BAND if y == hoog else MUUR))
            if (x + z) % 2 == 0:
                v.zet(x, hoog + 1, z, MUUR)
            # buttresses outside every 8
            if (x - x1) % 8 == 0 and z in (z1, z2) or (z - z1) % 8 == 0 and x in (x1, x2):
                ox = -1 if x == x1 else 1 if x == x2 else 0
                oz = -1 if z == z1 else 1 if z == z2 else 0
                for y in range(0, hoog):
                    v.zet(x + ox, y, z + oz, HOEK, axis="y")
    if poort:
        mx, b = poort
        for x in range(mx - b // 2, mx + b // 2 + 1):
            for y in range(1, 6):
                v.lucht(x, y, z1)
            v.zet(x, 0, z1, TEGEL)
            v.zet(x, 6, z1, FUND2)
    # wall lanterns on the inside
    for x in range(x1 + 4, x2 - 2, 6):
        hanglamp(v, x, hoog, z2 - 1, 1)
        if not poort or abs(x - poort[0]) > 4:
            hanglamp(v, x, hoog, z1 + 1, 1)


def poortgebouw(v, mx, z, hoog=11):
    """Two closed flanking towers by the gate (decorative, no door) with spires and banners."""
    for x0 in (mx - 6, mx + 2):
        toren(v, x0, z - 2, 5, 1, vh=hoog - 1, deur=None, trap=False, dak="spits")
        wandbanier(v, x0 + 2, 6, z - 3, "north")


def hal(v, x1, z1, x2, z2, hoog=6, deur_x=None, deur_zij="north", inhoud=None, kroon=1):
    """A hall along x: foundation, cherry floor, pink walls, windows, gable roof (purpur stairs), chandelier(s), an open door."""
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            rand = x in (x1, x2) or z in (z1, z2)
            v.zet(x, 0, z, FUND if rand else VLOER)
            for y in range(1, hoog + 1):
                if rand:
                    hoek = x in (x1, x2) and z in (z1, z2)
                    v.zet(x, y, z, HOEK if hoek else (BAND if y == hoog else (FUND if y == 1 else MUUR)), **({"axis": "y"} if hoek else {}))
                else:
                    v.lucht(x, y, z)
    # ceiling
    for x in range(x1 + 1, x2):
        for z in range(z1 + 1, z2):
            v.zet(x, hoog, z, VLOER)
    # windows along the long sides
    for x in range(x1 + 2, x2 - 1, 3):
        for y in (2, 3):
            v.zet(x, y, z1, GLAS)
            v.zet(x, y, z2, GLAS)
    # gable roof along x
    diep = (z2 - z1) // 2 + 1
    for i in range(diep + 1):
        y = hoog + 1 + i
        za, zb = z1 - 1 + i, z2 + 1 - i
        if za > zb:
            break
        for x in range(x1 - 1, x2 + 2):
            if za == zb:
                v.zet(x, y, za, "minecraft:purpur_slab", type="bottom", waterlogged=False)
            else:
                v.zet(x, y, za, DAKTRAP, facing="south", half="bottom", shape="straight", waterlogged=False)
                v.zet(x, y, zb, DAKTRAP, facing="north", half="bottom", shape="straight", waterlogged=False)
                for z in range(za + 1, zb):
                    if x in (x1 - 1, x2 + 1):
                        v.zet(x, y, z, MUUR) if x in (x1 - 1, x2 + 1) and z in range(z1, z2 + 1) else None
    # gable ends
    for i in range(diep):
        y = hoog + 1 + i
        for z in range(z1 + i, z2 - i + 1):
            v.zet(x1, y, z, MUUR)
            v.zet(x2, y, z, MUUR)
    # door
    if deur_x is not None:
        zd = z1 if deur_zij == "north" else z2
        for x in (deur_x - 1, deur_x, deur_x + 1):
            for y in (1, 2, 3):
                v.lucht(x, y, zd)
        v.zet(deur_x, 4, zd, FUND2)
        wandbanier(v, deur_x, 5, zd + (-1 if deur_zij == "north" else 1), deur_zij)
    # chandeliers
    lengte = x2 - x1
    for k in range(kroon):
        kx = x1 + (k + 1) * lengte // (kroon + 1)
        kroonluchter(v, kx, hoog, (z1 + z2) // 2, 1)
    if inhoud:
        inhoud(v)


# ---- the levels ----------------------------------------------------------------------------------------------------------

def grond_en_pad(v, tot_z, breed=3):
    """The path from the plot entrance (north, z 0) to tot_z, and lamp posts along it."""
    for z in range(0, tot_z + 1):
        for x in range(32 - breed // 2 - 0, 32 + breed // 2 + 1):
            v.zet(x, 0, z, PAD)
    for z in range(3, tot_z, 6):
        lantaarnpaal(v, 32 - breed // 2 - 2, 1, z)
        lantaarnpaal(v, 32 + breed // 2 + 2, 1, z)


def toren_inhoud(v, x1, z1, x2, z2):
    mx, mz = (x1 + x2) // 2, (z1 + z2) // 2
    # ground floor: the teleport stone in the middle (on a quartz plate), the vault chest against the west wall, rugs
    v.zet(mx, 0, mz + 1, "minecraft:chiseled_quartz_block")
    v.zet(mx, 1, mz + 1, "minecraft:lodestone")
    v.zet(x1 + 1, 1, mz, "minecraft:chest", facing="east", type="single", waterlogged=False)
    v.zet(x1 + 1, 1, mz + 1, "minecraft:chest", facing="east", type="single", waterlogged=False)
    for x in range(mx - 1, mx + 2):
        v.zet(x, 1, mz - 1, "minecraft:pink_carpet")
    # banners inside on the floors
    wandbanier(v, x1 + 1, 9, mz, "east")
    wandbanier(v, x2 - 2, 9, z1 + 1, "south") if False else None
    v.zet(mx, 7, z2 - 1, "minecraft:cherry_stairs", facing="north", half="bottom", shape="straight", waterlogged=False)


def niveau1():
    v = Bouw(64, 40, 64)
    grond_en_pad(v, 26)
    toren(v, 27, 27, 11, 2, inhoud=toren_inhoud)
    # flower beds round the tower
    bloemen(v, 22, 24, 26, 42)
    bloemen(v, 38, 24, 42, 42)
    return v, {"aankomst": [32, 1, 22], "trofee": []}


def bloemen(v, x1, z1, x2, z2, kans=0.35, seed=1):
    r = random.Random(seed + x1 * 31 + z1)
    soorten = ["minecraft:pink_tulip", "minecraft:allium", "minecraft:oxeye_daisy", "minecraft:azure_bluet", "minecraft:peony",
               "minecraft:pink_petals"]
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            if (x, 1, z) in v.b or (x, 0, z) in v.b:
                continue
            if r.random() < kans:
                s = r.choice(soorten)
                if s == "minecraft:peony":
                    if (x, 2, z) not in v.b:
                        v.zet(x, 1, z, s, half="lower")
                        v.zet(x, 2, z, s, half="upper")
                elif s == "minecraft:pink_petals":
                    v.zet(x, 1, z, s, facing="north", flower_amount=r.randint(1, 4), segment_amount=r.randint(1, 4)) if False else \
                        v.zet(x, 1, z, s, facing="north", flower_amount=r.randint(1, 4))
                else:
                    v.zet(x, 1, z, s)


TROFEE_PLEKKEN = [(x, z) for z in (41, 45) for x in (22, 26, 30, 34, 38, 42)]


def trofeezaal(v):
    hal(v, 19, 38, 45, 47, hoog=7, deur_x=32, deur_zij="north", kroon=3)
    for (x, z) in TROFEE_PLEKKEN:
        v.zet(x, 0, z, "minecraft:chiseled_quartz_block")
        v.zet(x, 1, z, HOEK, axis="y")
    # a red-pink carpet runner down the middle (x 31..33 kept free: the aisle)
    for x in range(20, 45):
        for z in (43,):
            v.zet(x, 1, z, "minecraft:pink_carpet")
    for z in range(39, 47):
        v.zet(32, 1, z, "minecraft:pink_carpet")


def niveau2(v=None):
    v = v or Bouw(64, 48, 64)
    # courtyard paving inside the ring
    for x in range(17, 48):
        for z in range(17, 48):
            v.zet(x, 0, z, TEGEL if (x + z) % 2 == 0 else "minecraft:quartz_bricks")
    grond_en_pad(v, 16)
    muurring(v, 16, 16, 48, 48, hoog=7, poort=(32, 3))
    poortgebouw(v, 32, 16)
    toren(v, 27, 25, 11, 2, inhoud=toren_inhoud)
    trofeezaal(v)
    # path from the gate to the tower, between flower beds
    for z in range(17, 25):
        for x in (31, 32, 33):
            v.zet(x, 0, z, PAD)
    return v, {"aankomst": [32, 1, 20], "trofee": [[x, 2, z] for (x, z) in TROFEE_PLEKKEN]}


def werkvleugel(v, x1, z1, x2, z2, deur_zij, blokken):
    """A workshop wing (hall along z): open arches on the courtyard side, the workshop blocks inside against the back wall."""
    # a hall along z: use the hall helper rotated by building it along x then... simpler: own box
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            rand = x in (x1, x2) or z in (z1, z2)
            v.zet(x, 0, z, FUND if rand else VLOER)
            for y in range(1, 7):
                if rand:
                    hoek = x in (x1, x2) and z in (z1, z2)
                    v.zet(x, y, z, HOEK if hoek else (BAND if y == 6 else (FUND if y == 1 else MUUR)), **({"axis": "y"} if hoek else {}))
                else:
                    v.lucht(x, y, z)
            v.zet(x, 6, z, VLOER) if not (x in (x1, x2) or z in (z1, z2)) else None
    # lean-to roof (stairs down towards the courtyard side)
    breed = x2 - x1 + 2
    for i in range(breed):
        x = (x1 - 1 + i) if deur_zij == "east" else (x2 + 1 - i)
        y = 7 + (breed - 1 - i) // 2 if False else 7 + min(i, breed - 1 - i) // 2
        for z in range(z1 - 1, z2 + 2):
            v.zet(x, y, z, DAKTRAP, facing="west" if x <= (x1 + x2) // 2 else "east", half="bottom", shape="straight", waterlogged=False)
    # arches on the courtyard side
    xd = x2 if deur_zij == "east" else x1
    for z in range(z1 + 2, z2 - 1, 4):
        for y in (1, 2, 3):
            v.lucht(xd, y, z)
            v.lucht(xd, y, z + 1)
        hanglamp(v, (x1 + x2) // 2, 6, z, 1)
    # the workshop blocks against the back wall
    xb = x1 + 1 if deur_zij == "east" else x2 - 1
    z = z1 + 2
    for naam, props in blokken:
        v.zet(xb, 1, z, naam, **props)
        z += 3


def niveau3(v=None):
    v, m = niveau2(v or Bouw(64, 52, 64))
    for (x0, z0) in ((13, 13), (45, 13)):
        toren(v, x0, z0, 7, 2, vh=7, deur=None, trap=False, dak="spits")
        wandbanier(v, x0 + 3, 8, z0 - 1, "north")
    werkvleugel(v, 18, 19, 24, 36, "east", [("minecraft:smithing_table", {}), ("minecraft:anvil", {"facing": "north"}),
                                            ("minecraft:smoker", {"facing": "east", "lit": "true"}), ("minecraft:furnace", {"facing": "east", "lit": "false"}),
                                            ("minecraft:crafting_table", {})])
    return v, m


def niveau4(v=None):
    v, m = niveau3(v or Bouw(64, 52, 64))
    for (x0, z0) in ((13, 45), (45, 45)):
        toren(v, x0, z0, 7, 2, vh=7, deur=None, trap=False, dak="spits")
    werkvleugel(v, 40, 19, 46, 36, "west", [("minecraft:loom", {"facing": "west"}), ("minecraft:brewing_stand",
                                            {"has_bottle_0": "false", "has_bottle_1": "false", "has_bottle_2": "false"}),
                                            ("minecraft:cauldron", {}), ("minecraft:crafting_table", {})])
    # garden: flower beds in the courtyard corners, a fountain in front of the tower
    for (x1, z1) in ((26, 18), (35, 18)):
        for x in range(x1, x1 + 4):
            for z in range(z1, z1 + 3):
                v.zet(x, 0, z, GRAS)
        bloemen(v, x1, z1, x1 + 3, z1 + 2, kans=0.8, seed=4)
    fontein(v, 32, 21) if False else None
    return v, m


def fontein(v, mx, mz):
    for x in range(mx - 2, mx + 3):
        for z in range(mz - 2, mz + 3):
            rand = x in (mx - 2, mx + 2) or z in (mz - 2, mz + 2)
            v.zet(x, 0, z, BAND)
            v.zet(x, 1, z, "minecraft:quartz_slab" if rand else "minecraft:water", **({"type": "bottom", "waterlogged": "false"} if rand else {"level": "0"}))
    v.zet(mx, 1, mz, HOEK, axis="y")
    v.zet(mx, 2, mz, HOEK, axis="y")
    v.zet(mx, 3, mz, "minecraft:water", level="0")


def keep_inhoud(v, x1, z1, x2, z2):
    mx, mz = (x1 + x2) // 2, (z1 + z2) // 2
    # the great hall (ground floor): throne at the south, teleport stone + vault, two chandeliers, a carpet aisle
    for z in range(z1 + 1, z2):
        v.zet(mx, 1, z, "minecraft:magenta_carpet")
    v.zet(mx, 1, z2 - 2, "minecraft:quartz_stairs", facing="south", half="bottom", shape="straight", waterlogged=False)
    v.zet(mx, 1, z2 - 1, "minecraft:gold_block")
    v.zet(mx - 1, 1, z2 - 1, "minecraft:quartz_pillar", axis="y")
    v.zet(mx + 1, 1, z2 - 1, "minecraft:quartz_pillar", axis="y")
    wandbanier(v, mx, 4, z2 - 1, "north")
    v.zet(mx - 3, 0, mz - 2, "minecraft:chiseled_quartz_block")
    v.zet(mx - 3, 1, mz - 2, "minecraft:lodestone")
    v.zet(x1 + 1, 1, mz, "minecraft:chest", facing="east", type="single", waterlogged=False)
    v.zet(x1 + 1, 1, mz + 1, "minecraft:chest", facing="east", type="single", waterlogged=False)
    v.zet(x1 + 1, 1, mz + 2, "minecraft:chest", facing="east", type="single", waterlogged=False)
    kroonluchter(v, mx, 8, z1 + 4, 2)
    kroonluchter(v, mx, 8, z2 - 4, 2)
    for x in (x1 + 1, x2 - 1):
        for z in range(z1 + 3, z2 - 2, 4):
            wandbanier(v, x, 5, z, "east" if x == x1 + 1 else "west")


def niveau5(v=None):
    v, m = niveau4(v or Bouw(64, 64, 64))
    # the keep replaces the tower: clear the tower's spot and build 15x15, 3 floors of 8, deck + spires
    v.wis(26, 0, 24, 38, 40, 37)
    for x in range(26, 39):
        for z in range(24, 38):
            v.zet(x, 0, z, TEGEL)
    top = toren(v, 25, 23, 15, 3, vh=8, inhoud=keep_inhoud, dak="dek")
    # a tall central spire on the deck instead of the flag pole
    v.wis(32, 25, 30, 32, 30, 30)
    piramide(v, 29, 27, 35, 33, 25, steil=3)
    # banners on the outer ring wall (north face, both sides of the gate)
    for x in (20, 24, 40, 44):
        wandbanier(v, x, 5, 15, "north")
    fontein(v, 32, 20) if False else None
    m = dict(m)
    m["aankomst"] = [32, 1, 19]
    return v, m


# ---- the plot cell + the square -----------------------------------------------------------------------------------------

def eiland(v, x1, z1, x2, z2, grond_y, seed):
    """Floating island underside under the box (top at grond_y): dirt, then strata of stone/calcite/pink terracotta, tapering."""
    r = random.Random(seed)
    cx, cz = (x1 + x2) / 2, (z1 + z2) / 2
    rx, rz = (x2 - x1) / 2 + 1, (z2 - z1) / 2 + 1
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            d = max(abs(x - cx) / rx, abs(z - cz) / rz)
            diep = int(3 + (grond_y - 3) * (1 - d) ** 1.4 + r.random() * 2)
            diep = max(2, min(grond_y, diep))
            for i in range(1, diep + 1):
                y = grond_y - i
                if y < 0:
                    break
                if i <= 2:
                    s = "minecraft:dirt"
                elif (y + (x // 5) + (z // 7)) % 5 == 0:
                    s = "minecraft:pink_terracotta"
                elif (y + x + z) % 7 == 0:
                    s = "minecraft:calcite"
                else:
                    s = "minecraft:stone"
                v.zet(x, y, z, s)


def kavel():
    G = 16
    v = Bouw(72, G + 6, 72)
    eiland(v, 0, 0, 71, 71, G, 7)
    for x in range(72):
        for z in range(72):
            weg = z < 8 or x < 8
            if weg:
                rand = z in (0, 7) and x >= 8 or x in (0, 7) and z >= 8
                if rand:
                    v.zet(x, G, z, "minecraft:stripped_cherry_log", axis="x" if z in (0, 7) else "z")
                else:
                    v.zet(x, G, z, VLOER)
            else:
                v.zet(x, G, z, GRAS)
    # the hedge round the plot (persistent flowering azalea), open at the north middle (plot x 29..35)
    for i in range(64):
        for (x, z) in ((8 + i, 8), (8 + i, 71), (8, 8 + i), (71, 8 + i)):
            if z == 8 and 8 + 29 <= x <= 8 + 35:
                continue
            v.zet(x, G + 1, z, "minecraft:flowering_azalea_leaves", persistent=True, distance=1, waterlogged=False)
    # gate posts at the opening
    for x in (8 + 28, 8 + 36):
        v.zet(x, G + 1, 8, HOEK, axis="y")
        v.zet(x, G + 2, 8, HOEK, axis="y")
        v.zet(x, G + 3, 8, LAMP, hanging=False, waterlogged=False)
    # lamp posts on the road edges (plot side), not in front of the opening
    for x in range(12, 72, 16):
        if not (8 + 26 <= x <= 8 + 38):
            lantaarnpaal(v, x, G + 1, 6)
    for z in range(20, 72, 16):
        lantaarnpaal(v, 6, G + 1, z)
    return v


def plein():
    G = 16
    v = Bouw(56, G + 12, 56)
    eiland(v, 0, 0, 55, 55, G, 3)
    for x in range(56):
        for z in range(56):
            d = math.hypot(x - 27.5, z - 27.5)
            s = BAND if int(d) % 6 == 0 else TEGEL if (x + z) % 2 else "minecraft:pink_glazed_terracotta" if (x * 7 + z * 3) % 23 == 0 else TEGEL
            v.zet(x, G, z, s)
    # railing on the north, east and west edges (the south edge meets the first plots' roads)
    for i in range(56):
        v.zet(i, G + 1, 0, HEK)
        v.zet(0, G + 1, i, HEK)
        v.zet(55, G + 1, i, HEK)
    # the gate (north): quartz pillars + an arch, open in the middle (world x X0-1..X0+1, z Z0-23..Z0-21)
    for x in (26, 30):
        for z in (5, 6, 7):
            for y in range(G + 1, G + 6):
                v.zet(x, y, z, HOEK, axis="y")
    for x in range(26, 31):
        for z in (5, 6, 7):
            v.zet(x, G + 6, z, BAND)
            v.zet(x, G + 7, z, MUUR if x in (26, 30) else BAND)
    v.zet(28, G + 8, 6, "minecraft:gold_block")
    v.zet(28, G + 9, 6, "minecraft:end_rod", facing="up")
    wandbanier(v, 28, G + 5, 8, "south")
    for x in (27, 28, 29):
        for z in (5, 6, 7):
            v.zet(x, G, z, "minecraft:amethyst_block")
    # the fountain in the middle (1 deep, a rim you can step over)
    fx, fz = 28, 30
    for x in range(fx - 3, fx + 4):
        for z in range(fz - 3, fz + 4):
            rand = abs(x - fx) == 3 or abs(z - fz) == 3
            v.zet(x, G, z, BAND)
            v.zet(x, G + 1, z, "minecraft:quartz_slab" if rand else "minecraft:water", **({"type": "bottom", "waterlogged": "false"} if rand else {"level": "0"}))
    for y in range(G + 1, G + 4):
        v.zet(fx, y, fz, HOEK, axis="y")
    v.zet(fx, G + 4, fz, "minecraft:water", level="0")
    # lamp posts + benches + flower pots round the square
    for (x, z) in ((10, 10), (45, 10), (10, 45), (45, 45), (10, 28), (45, 28)):
        lantaarnpaal(v, x, G + 1, z, 3)
    for (x, z, f) in ((20, 30, "east"), (36, 30, "west"), (28, 22, "south"), (28, 38, "north")):
        for d in (-1, 0, 1):
            bx, bz = (x, z + d) if f in ("east", "west") else (x + d, z)
            v.zet(bx, G + 1, bz, "minecraft:cherry_stairs", facing={"east": "west", "west": "east", "south": "north", "north": "south"}[f],
                  half="bottom", shape="straight", waterlogged=False)
    for (x, z) in ((5, 5), (50, 5), (5, 50), (50, 50)):
        v.zet(x, G + 1, z, "minecraft:potted_pink_tulip")
    return v


def main():
    print("U-GILDEN castle templates ->", os.path.relpath(UIT, ROOT))
    schrijf(plein(), "plein")
    schrijf(kavel(), "kavel")
    os.makedirs(MARK, exist_ok=True)
    for n, f in ((1, niveau1), (2, niveau2), (3, niveau3), (4, niveau4), (5, niveau5)):
        v, m = f()
        schrijf(v, f"kasteel_{n}")
        with open(os.path.join(MARK, f"kasteel_{n}.json"), "w", encoding="utf-8", newline="\n") as fh:
            json.dump(m, fh, indent=1)
            fh.write("\n")


if __name__ == "__main__":
    main()
