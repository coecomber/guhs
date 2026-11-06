"""U-HEKKEN: the central connection pass for every Vadsrijk map tile (zones 1-5, also the zone-4/5 kits).

WHY: the tiles are placed with StructurePlaceSettings.setKnownShape(true) (KaartFeature), so Minecraft takes every written
blockstate as it is and never recomputes north/east/south/west. A fence written as `minecraft:spruce_fence` (all sides
false), a garland written with the wrong axis, or a fence ring rasterised with diagonal steps therefore shows up in the
game as loose posts. This pass fixes that AFTER all blocks are placed, over the written tiles, with a halo from the
neighbour tiles, so tile borders are correct too.

What it does (vanilla rules, see LEESMIJ_BOUWREGELS.md):
  * fences      connect to fences of the same family (#wooden_fences vs the other #fences), to fence gates in line and
                to sturdy full faces (not leaves/barrier/pumpkin/melon/shulker);
  * walls       connect to walls, panes/bars, gates in line and sturdy faces; side low/tall (tall under a covering block)
                and `up` like WallBlock.shouldRaisePost;
  * panes/bars  connect to panes/bars, walls and sturdy faces (mod: *_ruit, *_tralies);
  * chains      (iron/copper chain, guhs:vadstouw) get the axis along their chain neighbours;
  * line blocks (guhs:vlaggetjes, guhs:feestslingers) get the horizontal axis along their line / their posts;
  * gaps        a fence/wall/pane/garland LINE (>= 3 pieces) with a diagonal step gets the corner filled (outside of the
                curve, preferably over the void), and a step of 1 up gets the lower post raised by one, so lines read as
                lines. Two lone posts are never joined.

Usage
  python tools/vadsrijk/verbind.py                     all tiles in src/main/resources/data/guhs/structure/vadsrijk/tegels
  python tools/vadsrijk/verbind.py --alleen 30,-2;31,-2  these tiles (+ the neighbour tiles whose shared edge changes)
  from a generator:   import verbind; verbind.na(tegels)   (runs the pass in its own process; tegels = [(tx, tz), ...]
                      or None = all). Every zone maker + maak_kaart call this after writing their tiles.
  on a volume:        verbind.verbind_vol(vol, pal, id_fn, rand=0)  (vol[x,y,z] = ids into pal, a list of state strings;
                      id_fn(state) -> id, may grow pal). Returns the number of changed cells.
"""
import argparse
import importlib.util
import json
import os
import subprocess
import sys
import time

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
UIT = os.path.join(RES, "data", "guhs", "structure", "vadsrijk", "tegels")
T = 64
HOOG = 320
HALO = 4

# directions: 0 north (-z), 1 east (+x), 2 south (+z), 3 west (-x), 4 up, 5 down
D = [(0, 0, -1), (1, 0, 0), (0, 0, 1), (-1, 0, 0), (0, 1, 0), (0, -1, 0)]
NAAM = ["north", "east", "south", "west"]
TEGEN = [2, 3, 0, 1, 5, 4]

GEEN, HEK, MUUR, RUIT, KETTING, LIJN, POORT = range(7)
LUCHT = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:structure_void"}
UITZONDERING = ("leaves", "bladeren", "_blad", "barrier", "carved_pumpkin", "jack_o_lantern", "melon", "pumpkin",
                "shulker_box")


def _tag(pad):
    p = os.path.join(RES, "data", "minecraft", "tags", "block", pad)
    try:
        with open(p, encoding="utf-8") as f:
            return {v for v in json.load(f).get("values", []) if not v.startswith("#")}
    except OSError:
        return set()


def splits(s):
    naam, _, rest = s.partition("[")
    props = {}
    if rest:
        for kv in rest.rstrip("]").split(","):
            k, v = kv.split("=")
            props[k] = v
    return naam, props


def maak(naam, props):
    return naam + ("[" + ",".join(f"{k}={props[k]}" for k in sorted(props)) + "]" if props else "")


class Regels:
    """Per state string: what kind of connecting block it is, and which of its faces are sturdy (full)."""

    def __init__(self):
        self.mod_hek = _tag("fences.json")
        self.mod_hout = _tag("wooden_fences.json")
        self.mod_muur = _tag("walls.json")
        self.mod_poort = _tag("fence_gates.json")
        self._bk = None
        self._vol = {}

    # ------------------------------------------------------------ kinds
    def soort(self, s):
        n = s.split("[")[0]
        kort = n.split(":")[-1]
        if n in LUCHT:
            return GEEN, None
        if n.startswith("minecraft:"):
            if kort.endswith("_fence_gate"):
                return POORT, None
            if kort.endswith("_fence"):
                return HEK, ("steen" if kort == "nether_brick_fence" else "hout")
            if kort.endswith("_wall") and not kort.endswith(("_hanging_wall",)):
                return MUUR, None
            if kort.endswith("glass_pane") or kort == "iron_bars":
                return RUIT, None
            if kort.endswith("chain"):
                return KETTING, None
            return GEEN, None
        if n in self.mod_hout:
            return HEK, "hout"
        if n in self.mod_hek:
            return HEK, "steen"
        if n in self.mod_muur:
            return MUUR, None
        if kort.endswith(("_ruit", "_tralies")):
            return RUIT, None
        if n in self.mod_poort:
            return POORT, None
        if kort == "vadstouw":
            return KETTING, None
        if kort in ("vlaggetjes", "feestslingers"):
            return LIJN, None
        return GEEN, None

    # ------------------------------------------------------------ sturdy faces
    def baker(self):
        if self._bk is None:
            try:
                spec = importlib.util.spec_from_file_location("_verbind_mcrender", os.path.join(HIER, "stad", "mcrender.py"))
                R = importlib.util.module_from_spec(spec)
                spec.loader.exec_module(R)
                jars = [os.path.join(ROOT, "..", "..", "renders-erfenis", "_bron", "jars", "guhs-assets-1.8.1.zip"),
                        os.path.join(os.environ.get("APPDATA", ""),
                                     "PrismLauncher/libraries/com/mojang/minecraft/26.1.2/minecraft-26.1.2-client.jar")]
                jars = [j for j in jars if os.path.exists(j)]

                class Res(R.Res):
                    """The worktree's own assets first (new Vadsrijk blocks), then the jars."""
                    def read(self, path):
                        p = os.path.join(RES, *path.split("/"))
                        if os.path.exists(p):
                            with open(p, "rb") as f:
                                return f.read()
                        return R.Res.read(self, path)
                self._bk = R.Baker(Res(jars))
            except Exception as e:      # no assets: name rules only
                print("verbind: geen baker (alleen naamregels):", e)
                self._bk = False
        return self._bk

    def vlakken(self, s):
        """6 bools: is face d (0 N, 1 E, 2 S, 3 W, 4 up, 5 down) a full sturdy face?"""
        if s in self._vol:
            return self._vol[s]
        n, props = splits(s)
        kort = n.split(":")[-1]
        uit = [False] * 6
        if n in LUCHT or n in ("minecraft:water", "minecraft:lava", "minecraft:light") or "bubble_column" in n:
            pass
        elif any(u in kort for u in UITZONDERING):
            pass      # isExceptionForConnection (and leaves are never a post)
        elif self.soort(s)[0] != GEEN:
            pass
        else:
            bk = self.baker()
            if bk:
                try:
                    F = bk.bake(s)
                    for q in F.quads:
                        c = np.asarray(q[0], float)
                        if c.shape[0] == 3:
                            c = np.vstack([c, c[1] + c[2] - c[0]])
                        lo, hi = c.min(0), c.max(0)
                        for d, (dx, dy, dz) in enumerate(D):
                            a = 0 if dx else (1 if dy else 2)
                            v = (dx, dy, dz)[a]
                            if v > 0 and abs(lo[a] - 1) > 1e-3 or v < 0 and abs(hi[a]) > 1e-3:
                                continue
                            ander = [i for i in range(3) if i != a]
                            if all(lo[i] < 1e-3 and hi[i] > 1 - 1e-3 for i in ander):
                                uit[d] = True
                except Exception:
                    pass
            else:
                vol = not any(w in kort for w in ("slab", "stairs", "door", "sign", "torch", "lantern", "flower", "grass",
                                                   "carpet", "button", "lever", "rail", "vine", "ladder", "pot", "bed",
                                                   "chest", "campfire", "candle", "head", "skull", "banner", "plate"))
                uit = [vol] * 6
            if "slab" in kort and props.get("type") == "double":
                uit = [True] * 6
        self._vol[s] = uit
        return uit


_REGELS = None


def regels():
    global _REGELS
    if _REGELS is None:
        _REGELS = Regels()
    return _REGELS


# ------------------------------------------------------------------------------------------------ the pass on a volume
class _Lut:
    def __init__(self, R, pal):
        self.R, self.pal = R, pal
        self.n = 0
        self.soort = np.zeros(0, np.int8)
        self.groep = []
        self.vlak = np.zeros((0, 6), bool)
        self.basis = []
        self.bij()

    def bij(self):
        n = len(self.pal)
        if n <= self.n:
            return
        s = np.zeros(n, np.int8)
        s[:self.n] = self.soort
        v = np.zeros((n, 6), bool)
        v[:self.n] = self.vlak
        for i in range(self.n, n):
            st = self.pal[i]
            k, g = self.R.soort(st)
            s[i] = k
            self.groep.append(g)
            v[i] = self.R.vlakken(st)
            self.basis.append(st.split("[")[0])
        self.soort, self.vlak, self.n = s, v, n


def _past(L, a, b, d):
    """Does block id a (a connecting kind) connect toward direction d to block id b? (vanilla connectsTo)"""
    ka, kb = L.soort[a], L.soort[b]
    if kb == POORT:
        sb = splits(L.pal[b])[1]
        f = sb.get("facing", "north")
        as_poort = "x" if f in ("east", "west") else "z"
        as_d = "z" if d in (1, 3) else "x"     # direction east/west: clockwise axis is z
        return ka in (HEK, MUUR) and as_poort == as_d
    if ka == HEK:
        if kb == HEK:
            return L.groep[a] == L.groep[b]
        return bool(L.vlak[b, TEGEN[d]])
    if ka == MUUR:
        return kb in (MUUR, RUIT) or bool(L.vlak[b, TEGEN[d]])
    if ka == RUIT:
        return kb in (MUUR, RUIT) or bool(L.vlak[b, TEGEN[d]])
    return False


def _familie(L, a, b):
    """Same line family for the gap filling (a line of fences, walls, panes or garlands)."""
    ka, kb = L.soort[a], L.soort[b]
    if ka == HEK:
        return kb == HEK and L.groep[a] == L.groep[b]
    if ka in (MUUR, RUIT):
        return kb in (MUUR, RUIT)
    if ka == LIJN:
        return kb == LIJN
    return False


def verbind_vol(vol, pal, id_fn, rand=0, vullen=True, R=None, log=None, lijst=None):
    """Fix all connection states in vol (in place). Only cells with rand <= x < X-rand and rand <= z < Z-rand change
    (the rest is halo). Returns the number of changed cells. lijst: collects (x, y, z, old state, new state, why)."""
    R = R or regels()
    L = _Lut(R, pal)
    X, Y, Z = vol.shape
    veranderd = 0

    def binnen(x, z):
        return rand <= x < X - rand and rand <= z < Z - rand

    def get(x, y, z):
        if 0 <= x < X and 0 <= y < Y and 0 <= z < Z:
            return int(vol[x, y, z])
        return 0

    def is_lucht(i):
        return i == 0 or L.basis[i] in LUCHT

    def zet(x, y, z, s, waarom):
        nonlocal veranderd
        i = id_fn(s)
        L.bij()
        if vol[x, y, z] != i:
            if lijst is not None:
                lijst.append((x, y, z, pal[int(vol[x, y, z])], s, waarom))
            vol[x, y, z] = i
            veranderd += 1

    kandidaat = np.nonzero(L.soort > 0)[0]
    if not len(kandidaat):
        return 0
    m = np.isin(vol, kandidaat)
    if not m.any():
        return 0

    # ---------------------------------------------------------------- 1. gaps (lines only, >= 3 pieces)
    if vullen:
        for ronde in range(2):
            m = np.isin(vol, np.nonzero((L.soort == HEK) | (L.soort == MUUR) | (L.soort == RUIT) | (L.soort == LIJN))[0])
            pos = np.argwhere(m)
            if not len(pos):
                break

            def buren(x, y, z, a, zonder):
                """Line neighbours of a (8 around, y-1..y+1), not counting the partner's column `zonder` (x, z): two
                lone posts (lamp posts!) diagonal to each other have 0 each and are never joined."""
                n = 0
                for dy in (-1, 0, 1):
                    for dx in (-1, 0, 1):
                        for dz in (-1, 0, 1):
                            if dx == dy == dz == 0 or (dx == 0 and dz == 0) or (x + dx, z + dz) == zonder:
                                continue
                            b = get(x + dx, y + dy, z + dz)
                            if b and _familie(L, a, b):
                                n += 1
                return n
            vul = []
            for (x, y, z) in pos:
                x, y, z = int(x), int(y), int(z)
                a = int(vol[x, y, z])
                # (a) diagonal step at the same height or one up: fill a corner at the lower height
                for dx in (-1, 1):
                    for dz in (-1, 1):
                        for dy in (0, 1):
                            b = get(x + dx, y + dy, z + dz)
                            if not b or not _familie(L, a, b):
                                continue
                            if dy == 0 and (x + dx, z + dz) < (x, z):
                                continue        # each pair once
                            c1, c2 = (x + dx, y, z), (x, y, z + dz)
                            hoog1, hoog2 = (x + dx, y + dy, z), (x, y + dy, z + dz)
                            if any(get(*c) and _familie(L, a, get(*c)) for c in (c1, c2, hoog1, hoog2)):
                                continue        # already joined via a corner
                            if buren(x, y, z, a, (x + dx, z + dz)) + buren(x + dx, y + dy, z + dz, b, (x, z)) < 1:
                                continue        # two lone posts: leave them (a line has >= 3 pieces)
                            keus = []
                            for c in (c1, c2):
                                if not is_lucht(get(*c)):
                                    continue
                                vloer = bool(L.vlak[get(c[0], c[1] - 1, c[2]), 4])
                                rond = sum(1 for ex in (-1, 0, 1) for ez in (-1, 0, 1)
                                           if (ex or ez) and get(c[0] + ex, c[1], c[2] + ez)
                                           and _familie(L, a, get(c[0] + ex, c[1], c[2] + ez)))
                                vast = sum(1 for e in range(4) if not is_lucht(get(c[0] + D[e][0], c[1], c[2] + D[e][2])))
                                keus.append(((vloer, rond, vast, c), c))
                            if keus:
                                c = min(keus)[1]
                                vul.append((c, a))
                # (b) one step up next to it: raise this (lower) post by one
                for d in range(4):
                    nx, nz = x + D[d][0], z + D[d][2]
                    n0 = get(nx, y, nz)
                    if n0 and _familie(L, a, n0):
                        continue
                    b = get(nx, y + 1, nz)
                    if not b or not _familie(L, a, b) or L.soort[a] == LIJN:
                        continue
                    boven = get(x, y + 1, z)
                    if not is_lucht(boven):
                        continue
                    if buren(x, y, z, a, (nx, nz)) + buren(nx, y + 1, nz, b, (x, z)) < 1:
                        continue
                    vul.append(((x, y + 1, z), a))
            gedaan = False
            for (c, a) in sorted(set(vul)):
                x, y, z = c
                if not (0 <= y < Y) or not binnen(x, z) or not is_lucht(get(x, y, z)):
                    continue
                naam, props = splits(pal[a])
                if "waterlogged" in props:
                    props["waterlogged"] = "false"
                zet(x, y, z, maak(naam, props), "gat")
                gedaan = True
            if log is not None:
                log.append(f"ronde {ronde}: {len(vul)} gaten")
            if not gedaan:
                break

    # ---------------------------------------------------------------- 2. states
    m = np.isin(vol, np.nonzero(L.soort > 0)[0])
    pos = [tuple(int(v) for v in p) for p in np.argwhere(m)]
    con = {}
    for (x, y, z) in pos:
        a = int(vol[x, y, z])
        k = L.soort[a]
        if k in (HEK, MUUR, RUIT):
            con[(x, y, z)] = [_past(L, a, get(x + D[d][0], y, z + D[d][2]), d) if get(x + D[d][0], y, z + D[d][2]) else False
                              for d in range(4)]
    nieuw = []
    for (x, y, z) in pos:
        if not binnen(x, z):
            continue
        a = int(vol[x, y, z])
        k = L.soort[a]
        naam, props = splits(pal[a])
        p = dict(props)
        if k in (HEK, RUIT):
            c = con[(x, y, z)]
            for d in range(4):
                p[NAAM[d]] = "true" if c[d] else "false"
            p.setdefault("waterlogged", "false")
        elif k == MUUR:
            c = con[(x, y, z)]
            boven = get(x, y + 1, z)
            cb = con.get((x, y + 1, z))
            hoogte = []
            for d in range(4):
                if not c[d]:
                    hoogte.append("none")
                elif boven and (L.vlak[boven, 5] or (cb is not None and L.soort[boven] == MUUR and cb[d])):
                    hoogte.append("tall")
                else:
                    hoogte.append("low")
                p[NAAM[d]] = hoogte[d]
            n_, e_, s_, w_ = [h == "none" for h in hoogte]
            if boven and L.soort[boven] == MUUR and splits(pal[boven])[1].get("up") == "true":
                up = True
            elif (n_ and e_ and s_ and w_) or n_ != s_ or e_ != w_:
                up = True
            elif (hoogte[0] == "tall" and hoogte[2] == "tall") or (hoogte[1] == "tall" and hoogte[3] == "tall"):
                up = False
            else:
                up = bool(boven) and not is_lucht(boven)     # anything standing on it covers the post (lantern, torch, ...)
            p["up"] = "true" if up else "false"
            p.setdefault("waterlogged", "false")
        elif k == KETTING:
            tel = {}
            for as_, ds in (("x", (1, 3)), ("y", (4, 5)), ("z", (0, 2))):
                tel[as_] = sum(1 for d in ds if L.soort[get(x + D[d][0], y + D[d][1], z + D[d][2])] == KETTING)
            best = max(tel.values())
            if best:
                nu = p.get("axis", "y")
                if tel.get(nu, 0) < best:
                    p["axis"] = "y" if tel["y"] == best else ("x" if tel["x"] == best else "z")
            p.setdefault("axis", "y")
            p.setdefault("waterlogged", "false")
        elif k == LIJN:
            tel = {"x": 0, "z": 0}
            for as_, ds in (("x", (1, 3)), ("z", (0, 2))):
                tel[as_] = sum(1 for d in ds if L.soort[get(x + D[d][0], y, z + D[d][2])] == LIJN)
            if tel["x"] == tel["z"] == 0:
                # no garland neighbours: hang it between posts / against walls
                for as_, ds in (("x", (1, 3)), ("z", (0, 2))):
                    tel[as_] = sum(1 for d in ds if not is_lucht(get(x + D[d][0], y, z + D[d][2])))
            nu = p.get("axis", "x")
            if tel["x"] != tel["z"]:
                p["axis"] = "x" if tel["x"] > tel["z"] else "z"
            else:
                p["axis"] = nu
        else:
            continue
        s = maak(naam, p)
        if s != pal[a]:
            nieuw.append((x, y, z, s))
    for (x, y, z, s) in nieuw:
        zet(x, y, z, s, "staat")
    return veranderd


# ------------------------------------------------------------------------------------------------ the pass over tiles
class _Tegels:
    """Dense tiles (global palette) with an LRU cache."""

    def __init__(self, map_):
        sys.path.insert(0, os.path.join(HIER, "audit"))
        import tegellezer as TL
        self.TL = TL
        self.map = map_
        self.pal = ["minecraft:air"]
        self.idx = {"minecraft:air": 0}
        self.cache = {}

    def id(self, s):
        i = self.idx.get(s)
        if i is None:
            i = self.idx[s] = len(self.pal)
            self.pal.append(s)
        return i

    def pad(self, t):
        return os.path.join(self.map, f"t_{t[0]}_{t[1]}.nbt")

    def dicht(self, t):
        if t in self.cache:
            return self.cache[t]
        p = self.pad(t)
        if not os.path.exists(p):
            v = None
        else:
            palette, (xs, ys, zs, st) = self.TL.lees(p)
            ids = np.array([self.id(s) for s in palette], np.uint16)
            v = np.zeros((T, HOOG, T), np.uint16)
            ok = (xs >= 0) & (xs < T) & (ys >= 0) & (ys < HOOG) & (zs >= 0) & (zs < T)
            v[xs[ok], ys[ok], zs[ok]] = ids[st[ok]]
        self.cache[t] = v
        if len(self.cache) > 40:
            del self.cache[next(iter(self.cache))]
        return v

    def heeft_nbt(self, t):
        """True when a tile has block-entity nbt (we never rewrite those: the reader drops the nbt)."""
        p = self.pad(t)
        import gzip
        return b"\x0a\x00\x03nbt" in gzip.decompress(open(p, "rb").read())

    def schrijf(self, t, v, achtervoegsel=""):
        sys.path.insert(0, os.path.join(HIER, "wereld"))
        import nbtsnel
        xs, ys, zs = np.nonzero(v)
        ids = v[xs, ys, zs]
        gebruikt, inv = np.unique(ids, return_inverse=True)
        palette = [splits(self.pal[i]) for i in gebruikt]
        nbtsnel.schrijf(self.pad(t) + achtervoegsel, (T, HOOG, T), palette, xs.astype(np.int32), ys.astype(np.int32), zs.astype(np.int32),
                        inv.astype(np.int32), None)


def venster(TG, t, H=HALO):
    """Tile t as a dense window with an H-wide border from its 8 neighbours (missing tiles = air)."""
    tx, tz = t
    w = np.zeros((T + 2 * H, HOOG, T + 2 * H), np.uint16)
    for ex in (-1, 0, 1):
        for ez in (-1, 0, 1):
            b = TG.dicht((tx + ex, tz + ez))
            if b is None:
                continue
            # window coords: tile (tx+ex) local x in [0,T) -> window x = H + ex*T + lx
            lx0, lx1 = max(0, -H - ex * T), min(T, T + H - ex * T)
            lz0, lz1 = max(0, -H - ez * T), min(T, T + H - ez * T)
            if lx0 >= lx1 or lz0 >= lz1:
                continue
            w[H + ex * T + lx0:H + ex * T + lx1, :, H + ez * T + lz0:H + ez * T + lz1] = b[lx0:lx1, :, lz0:lz1]
    return w


_TG = None


def _werk(arg):
    """One tile in a worker: read it with its halo, fix it, write it to <tile>.verbind (the main process renames after the
    round, so no worker ever reads a half-written neighbour). -> (tile, changed cells, edge flags, samples)"""
    global _TG
    map_, t, schrijf, monsters = arg
    if _TG is None or _TG.map != map_:
        _TG = _Tegels(map_)
    TG = _TG
    if TG.dicht(t) is None:
        return t, 0, (), []
    H = HALO
    w = venster(TG, t, H)
    oud = w[H:H + T, :, H:H + T].copy()
    lijst = [] if monsters else None
    n = verbind_vol(w, TG.pal, TG.id, rand=H, lijst=lijst)
    proef = []
    if lijst:
        for (x, y, z, o, nw, why) in lijst:
            proef.append((t[0] * T + x - H, y, t[1] * T + z - H, o, nw, why))
    if not n:
        return t, 0, (), proef
    nieuw = w[H:H + T, :, H:H + T]
    diff = nieuw != oud
    if not diff.any():
        return t, 0, (), proef
    rand = []
    xs, _, zs = np.nonzero(diff)
    for ex in (-1, 0, 1):
        for ez in (-1, 0, 1):
            if (ex, ez) == (0, 0):
                continue
            okx = (xs < H) if ex < 0 else (xs >= T - H) if ex > 0 else np.ones_like(xs, bool)
            okz = (zs < H) if ez < 0 else (zs >= T - H) if ez > 0 else np.ones_like(zs, bool)
            if (okx & okz).any():
                rand.append((t[0] + ex, t[1] + ez))
    if schrijf:
        if TG.heeft_nbt(t):
            print(f"verbind: tegel {t} heeft blok-nbt: overgeslagen", flush=True)
            return t, 0, (), proef
        TG.schrijf(t, nieuw.copy(), achtervoegsel=".verbind")
    return t, int(diff.sum()), tuple(rand), proef


def _ronde(map_, tegels, schrijf, werkers, monsters=False):
    global _TG
    _TG = None      # never reuse tiles cached in an earlier round
    arg = [(map_, t, schrijf, monsters) for t in tegels]
    if werkers > 1 and len(arg) > 8:
        import multiprocessing as mp
        with mp.get_context("spawn").Pool(werkers) as pool:
            uit = list(pool.imap_unordered(_werk, arg, chunksize=4))
    else:
        uit = [_werk(a) for a in arg]
    return uit


def naloop(tegels=None, map_=UIT, praat=True, werkers=6):
    """The pass over written tiles: each tile with a HALO-wide border from its 8 neighbours. Tiles whose shared edge with a
    neighbour changed get that neighbour (re)done too (next round). Returns (tiles written, cells changed)."""
    t0 = time.time()
    sys.path.insert(0, os.path.join(HIER, "audit"))
    import tegellezer as TL
    alle = {(tx, tz) for tx, tz, _ in TL.tegels(map_)}
    doel = sorted(alle if tegels is None else {tuple(t) for t in tegels} & alle)
    rij = list(doel)
    geschreven, cellen, rondes = set(), 0, 0
    while rij and rondes < 4:
        rondes += 1
        uit = _ronde(map_, rij, True, werkers)
        volgende = set()
        for (t, n, rand, _) in uit:
            if not n:
                continue
            p = os.path.join(map_, f"t_{t[0]}_{t[1]}.nbt")
            os.replace(p + ".verbind", p)
            geschreven.add(t)
            cellen += n
            volgende |= {r for r in rand if r in alle}
        rij = sorted(volgende)
    if praat:
        print(f"verbind: {len(doel)} tegels bekeken, {len(geschreven)} herschreven, {cellen} blokken aangepast, "
              f"{rondes} ronde(s) ({time.time() - t0:.0f} s)", flush=True)
    return geschreven, cellen


def na(tegels=None, map_=UIT):
    """Run naloop in its own process (the zone makers have kit modules with clashing names on sys.path)."""
    cmd = [sys.executable, os.path.abspath(__file__), "--map", map_]
    if tegels is not None:
        tegels = list(tegels)
        if not tegels:
            return
        cmd += ["--alleen", ";".join(f"{a},{b}" for a, b in tegels)]
    subprocess.run(cmd, cwd=ROOT, check=True)


def na_sinds(t0, map_=UIT):
    """Run the pass on every tile written since time t0 (+ the neighbours whose shared edge changes). The hook for the
    generators: they note t0 = time.time() before writing and call this afterwards. Skipped when GUHS_VERBIND_LATER=1
    (maak_kaart sets that for the zone makers it starts, and runs the pass itself once at the end)."""
    if os.environ.get("GUHS_VERBIND_LATER") == "1":
        return
    sys.path.insert(0, os.path.join(HIER, "audit"))
    import tegellezer as TL
    tegels = [(tx, tz) for tx, tz, p in TL.tegels(map_) if os.path.getmtime(p) >= t0 - 1]
    if tegels:
        na(tegels, map_)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--alleen", default=None, help="tx,tz;tx,tz;...")
    ap.add_argument("--map", default=UIT)
    ap.add_argument("--werkers", type=int, default=6)
    a = ap.parse_args()
    tegels = None
    if a.alleen:
        tegels = [tuple(int(v) for v in s.split(",")) for s in a.alleen.split(";") if s.strip()]
    naloop(tegels, a.map, werkers=a.werkers)


if __name__ == "__main__":
    main()
