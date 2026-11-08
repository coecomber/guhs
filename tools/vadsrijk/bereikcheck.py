"""W-BEREIK: GLOBAL survival reachability check of the whole Vadsrijk map (zones 1-6, all generated tiles).

Run from the project root (after maak_kaart.py; ~2-4 min, ~2 GB RAM):
    python tools/vadsrijk/bereikcheck.py                  # BEREIK_RAPPORT.md (+ tools/vadsrijk/_cache/bereik.json)
    python tools/vadsrijk/bereikcheck.py --zone 5         # report only zone 5's targets (the flood is always global)
    python tools/vadsrijk/bereikcheck.py --punt 2650,224,-520   # debug: is this spot reached, nearest reached cell
    python tools/vadsrijk/bereikcheck.py --strikt         # exit code 1 when a main-quest target or an NPC is not reached

What it reads (the way the pipeline ships the map):
  - the tiles src/main/resources/data/guhs/structure/vadsrijk/tegels/t_<tx>_<tz>.nbt: the working format every generator writes
    and that kaartpak.py packs 1:1 (verified tile by tile) into the kaart.gpak the jar ships;
  - the targets tools/vadsrijk/bereik_doelen.json: dumped from the running game code by the gametest BereikDoelenGameTests
    (`gradle_slot.sh <tree> runGameTestServer -Pgt=BereikDoelenGameTests`): every *Plekken field, all NPC definitions, arenas,
    spawners, airship lines, puzzles/secrets;
  - the quests (src/main/resources/data/guhs/vadsrijk/npc/quests*.json): which target belongs to a main quest;
  - the POI tables POI_*.md (house doors, lifts, hatches, ...).

Player model (W_BRIEF walkability standard), in half blocks:
  - a standing cell needs 1.8 blocks of air: ceiling - feet >= 4 half blocks (so 2 air blocks; 1.5 on a slab is too low);
  - walking onto a neighbour (4 directions) at the same height or up a slab/stair (+0.5) needs the body to fit on both sides;
  - a full block step up (+1) is a jump: it needs 2.8 blocks of air above the LOWER floor (3 air blocks) + room on top;
  - dropping down: at most 3 blocks, any depth into water; you must fit through the drop column;
  - water: swim up to the surface, climb out onto a block 1 above it; ladders/vines/scaffolding/cloud streams: climb;
  - fences, walls, iron bars, glass panes, lanterns, chains are BLOCKING (fences/walls count 1.5 high: you can't jump on them);
    wooden doors, gates and trapdoors are passable, iron doors are not (they are the "fake / locked" doors);
  - leaves are solid; plants, carpets, torches, signs, banners are passable; lava is never a floor.
Transport edges: airship lines (pier <-> pier), city lifts (lift_*_voet <-> lift_*_top), the zone-5 hatches/lifts, the moving temple
doors (open), plus the links in bereik_doelen.json "links".

Report (BEREIK_RAPPORT.md): per zone the reach numbers (main quest / side quest / NPC / secrets / other) and every unreached
target with coordinates + the nearest reached stand cell; "one-way" areas (reached, but no way back to a zone entry); boss arenas
without an exit; airship boarding (a reached cell within 4 blocks of the docked ship); doors (into a wall / unreachable interior);
lanterns/chains/decoration on walking lines; blocks that look solid but have no collision on walking edges.
"""
import argparse
import collections
import glob
import json
import os
import re
import sys
import time

import numpy as np
from scipy import sparse
from scipy.sparse import csgraph
from scipy.spatial import cKDTree

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
sys.path.insert(0, os.path.join(HIER, "audit"))
import tegellezer as TL  # noqa: E402

TEGELS = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
DOELEN = os.path.join(HIER, "bereik_doelen.json")
QUESTS = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "vadsrijk", "npc")
JAVA = os.path.join(ROOT, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk")
RAPPORT = os.path.join(ROOT, "BEREIK_RAPPORT.md")
CACHE = os.path.join(HIER, "_cache")
T = 64
NY = 320
MAX_VAL = 6          # max safe drop in half blocks (3 blocks)
GROOT = 4000         # ceiling of an open-sky run

# ------------------------------------------------------------------ block classes
AIR, SOLID, WATER, CLIMB, SLAB, FENCE, STAIR, DOOR, PLANT, LAVA, LOS, SPOOK, IJZERDEUR = range(13)
PASSABLE = (AIR, WATER, CLIMB, DOOR, PLANT, SPOOK)
HALF = (SLAB, STAIR)

_LUCHT = {"air", "cave_air", "void_air", "light", "structure_void", "vr_w_portaal", "nether_portal", "end_portal", "moving_piston"}
_WATER = {"water", "bubble_column", "kelp", "kelp_plant", "seagrass", "tall_seagrass"}
_KLIM = {"ladder", "vine", "scaffolding", "twisting_vines", "twisting_vines_plant", "weeping_vines", "weeping_vines_plant",
         "cave_vines", "cave_vines_plant", "wolkenlift"}
# blocks that LOOK like a wall/rail/post/cloth but have NO collision (W_BRIEF: "no blocks that look solid but have no collision")
_SPOOK = {"gouden_kaarsenrand", "kroonluchter", "kristallen_hanger", "gouden_wandluchter", "hoogvads_vaandel_boven",
          "hoogvads_vaandel_onder", "zijden_luifel", "vr_w_litteken_plant", "mini_luchtballon"}
# small things without collision you walk through
_DOOR_LOS = {"torch", "wall_torch", "soul_torch", "redstone_torch", "lever", "rail", "tripwire", "end_rod", "pale_hanging_moss",
             "brown_mushroom", "red_mushroom", "glimpaddenstoel", "guh_waterlelie", "lily_pad", "cobweb", "hanging_roots",
             "glow_lichen", "sculk_vein", "spore_blossom", "short_grass", "tall_grass", "fern", "large_fern", "dead_bush",
             "sweet_berry_bush", "firefly_bush", "bush", "short_dry_grass", "tall_dry_grass", "leaf_litter", "wildflowers",
             "pink_petals", "sunflower", "lilac", "rose_bush", "peony", "allium", "azure_bluet", "cornflower", "oxeye_daisy",
             "pink_tulip", "red_tulip", "white_tulip", "orange_tulip", "poppy", "dandelion", "lily_of_the_valley", "torchflower",
             "roze_guhbloem", "guhoortjes", "knabbelroos", "roze_hibiscus", "bloesemmeertje_riet", "wheat", "carrots", "potatoes",
             "beetroots", "pumpkin_stem", "melon_stem", "attached_melon_stem", "attached_pumpkin_stem", "sea_pickle",
             "small_amethyst_bud", "fossielmijn_zoutkristalletjes", "glimplant", "guh_ijsbloempje", "redstone_wire",
             "tube_coral_fan", "horn_coral_fan", "brain_coral", "bubble_coral", "vlaggetjes", "feestslingers", "azalea_bush"}
# blocking thin things (W_BRIEF: lanterns/chains/iron bars/panes block; on a path or stair they are a bug)
_LOS_WOORDEN = ("lantern", "lantaarn", "chain", "ketting", "lampion", "vadstouw")


def splits(state):
    naam, _, rest = state.partition("[")
    props = {}
    if rest:
        for kv in rest.rstrip("]").split(","):
            if "=" in kv:
                k, v = kv.split("=", 1)
                props[k] = v
    return naam, props


def klasse(state):
    naam, props = splits(state)
    n = naam.split(":", 1)[-1]
    if n in _LUCHT:
        return AIR
    if n in _WATER or props.get("waterlogged") == "true" and n in ("seagrass",):
        return WATER
    if n == "lava":
        return LAVA
    if n in _KLIM:
        return CLIMB
    if n == "wolkenstroom":
        return AIR if props.get("down") == "true" else CLIMB
    if n in _SPOOK:
        return SPOOK
    if n.endswith("trapdoor") or n.startswith("guh_luik"):
        return DOOR
    if n.endswith("_door") or n.endswith("_deur") or n.endswith("fence_gate") or n.startswith("guh_poortje"):
        return IJZERDEUR if n.startswith("iron") else DOOR
    if n == "sea_lantern" or n == "torenpeper_vuurtorenlamp":
        return SOLID
    if any(w in n for w in _LOS_WOORDEN) or n in ("iron_bars", "bell"):
        return LOS
    if n.endswith("_pane") or n.endswith("_ruit") or n.endswith("_tralies") or n == "glass_pane":
        return FENCE
    if n in _DOOR_LOS or n.endswith("_carpet") or n.endswith("_button") or n.endswith("pressure_plate") or n.endswith("_sign") \
            or n.endswith("_banner") or n.endswith("_sapling") or n.endswith("_torch") or n.startswith("potted_") and False:
        return PLANT
    if n.endswith("_fence") or n.endswith("_wall") or n.endswith("_muur") or n.endswith("_hek") or n.startswith("guh_hekje"):
        return FENCE
    if n.endswith("_stairs") or n.endswith("_trap"):
        return STAIR if props.get("half", "bottom") == "bottom" else SOLID
    if n.endswith("_slab") or n.endswith("_plaat"):
        return SLAB if props.get("type", "bottom") == "bottom" else SOLID
    if n in ("snow",):
        return PLANT if props.get("layers", "1") in ("1", "2") else SLAB
    if n in ("campfire", "soul_campfire") or n.endswith("_bed") or n in ("daylight_detector", "guh_bankje_hout", "guh_bankje_roze"):
        return SLAB
    return SOLID


class Kaart:
    """All runs (vertical stretches of passable cells) of all tiles: the nodes of the movement graph."""

    def __init__(self):
        self.x, self.z, self.b, self.t, self.F, self.H, self.C, self.fc, self.water = ([] for _ in range(9))
        self.deuren = []      # (x, y, z, facing, naam, class)
        self.los = []         # (x, y, z, naam) blocking deco
        self.spook = []       # (x, y, z, naam) no-collision look-alikes
        self.trappen = []     # (x, y, z) bottom stairs
        self.kolom_top = {}   # not used

    def klaar(self):
        for k in ("x", "z", "b", "t", "F", "H", "C", "fc"):
            setattr(self, k, np.concatenate(getattr(self, k)).astype(np.int32))
        self.water = np.concatenate(self.water)
        self.N = len(self.x)
        self.geldig = (self.C - self.F >= 4) & (self.fc != LAVA)
        self.bodem = self.b <= 1
        # sorted by column, then height
        self.col = self.x.astype(np.int64) * 100000 + (self.z + 50000)
        volg = np.lexsort((self.b, self.col))
        for k in ("x", "z", "b", "t", "F", "H", "C", "fc", "water", "geldig", "bodem", "col"):
            setattr(self, k, getattr(self, k)[volg])
        self.ucol, self.start, self.aantal = np.unique(self.col, return_index=True, return_counts=True)


def deur_dozen():
    """Moving temple doors (WereldPlekken.Deur): counted as open."""
    uit = []
    bp = r"new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)"
    for f in glob.glob(os.path.join(JAVA, "**", "*WereldPlekken.java"), recursive=True):
        src = open(f, encoding="utf-8").read()
        for m in re.finditer(r'new (?:WereldPlekken\.)?Deur\("([^"]+)", ' + bp + ", " + bp, src):
            uit.append((m.group(1), tuple(int(v) for v in m.group(2, 3, 4)), tuple(int(v) for v in m.group(5, 6, 7))))
    return uit


def laad(tegel_filter=None):
    t0 = time.time()
    K = Kaart()
    dozen = deur_dozen()
    lut_cache = {}
    for tx, tz, pad in TL.tegels(TEGELS):
        if tegel_filter and not tegel_filter(tx, tz):
            continue
        pal, (xs, ys, zs, st) = TL.lees(pad)
        lut = np.array([lut_cache.setdefault(s, klasse(s)) for s in pal], np.uint8)
        cls = np.zeros((T, T, NY), np.uint8)
        ok = (ys >= 0) & (ys < NY)
        cls[xs[ok], zs[ok], ys[ok]] = lut[st[ok]]
        # a (trap)door right above a ladder/vine is climbed through (an open trapdoor on a ladder counts as a ladder in Minecraft)
        boven_klim = (cls[:, :, 1:] == DOOR) & (cls[:, :, :-1] == CLIMB)
        cls[:, :, 1:][boven_klim] = CLIMB
        bed = cls[:, :, 0] == AIR
        cls[:, :, 0][bed] = SOLID                       # the flat generator's bedrock
        X0, Z0 = tx * T, tz * T
        for _, mn, mx in dozen:                         # moving doors: open
            x0, x1 = max(mn[0] - X0, 0), min(mx[0] - X0, T - 1)
            z0, z1 = max(mn[2] - Z0, 0), min(mx[2] - Z0, T - 1)
            if x0 <= x1 and z0 <= z1:
                cls[x0:x1 + 1, z0:z1 + 1, mn[1]:mx[1] + 1] = AIR
        # sparse lists: doors, blocking deco, no-collision look-alikes, stairs
        for i, s in enumerate(pal):
            c = lut[i]
            if c in (DOOR, IJZERDEUR) and ("_door" in s or "_deur" in s) and "trapdoor" not in s:
                n, props = splits(s)
                if props.get("half", "lower") != "lower":
                    continue
                k = st == i
                for x, y, z in zip(xs[k], ys[k], zs[k]):
                    K.deuren.append((X0 + int(x), int(y), Z0 + int(z), props.get("facing", "north"), n, int(c)))
            elif c == LOS or c == SPOOK:
                k = st == i
                n = splits(s)[0]
                lst = K.los if c == LOS else K.spook
                for x, y, z in zip(xs[k], ys[k], zs[k]):
                    if c == LOS:
                        lst.append((X0 + int(x), int(y), Z0 + int(z), n, int(cls[x, z, max(int(y) - 1, 0)])))
                    else:
                        lst.append((X0 + int(x), int(y), Z0 + int(z), n))
        # runs
        P = np.isin(cls, PASSABLE)
        onder = np.zeros_like(P)
        onder[:, :, 1:] = P[:, :, :-1]
        boven = np.zeros_like(P)
        boven[:, :, :-1] = P[:, :, 1:]
        sx, sz, sy = np.nonzero(P & ~onder)
        ex, ez, ey = np.nonzero(P & ~boven)
        b = sy.astype(np.int32)
        t = ey.astype(np.int32)
        fc = cls[sx, sz, np.maximum(b - 1, 0)]
        F = 2 * b
        F = np.where(np.isin(fc, HALF), F - 1, F)
        F = np.where(fc == FENCE, F + 1, F)
        C = np.where(t == NY - 1, GROOT, 2 * (t + 1))
        # swim / climb chain from the floor up
        X = (cls == WATER) | (cls == CLIMB)
        start = b.copy()
        c0 = X[sx, sz, b]
        c1 = ~c0 & (cls[sx, sz, np.minimum(b + 1, NY - 1)] == CLIMB)
        start[c1] += 1
        actief = np.nonzero(c0 | c1)[0]
        top = start.copy()
        while len(actief):
            nxt = top[actief] + 1
            ok2 = (nxt < NY) & (nxt <= t[actief])
            ok2[ok2] = X[sx[actief[ok2]], sz[actief[ok2]], nxt[ok2]]
            top[actief[ok2]] = nxt[ok2]
            actief = actief[ok2]
        heeft = c0 | c1
        water_top = heeft & (cls[sx, sz, np.minimum(top, NY - 1)] == WATER)
        H = np.where(heeft, 2 * top + 2 * water_top.astype(np.int32), F)
        H = np.minimum(H, C - 4)
        H = np.maximum(H, F)
        K.x.append(X0 + sx.astype(np.int32))
        K.z.append(Z0 + sz.astype(np.int32))
        K.b.append(b)
        K.t.append(t)
        K.F.append(F.astype(np.int32))
        K.H.append(H.astype(np.int32))
        K.C.append(C.astype(np.int32))
        K.fc.append(fc.astype(np.int32))
        K.water.append(cls[sx, sz, b] == WATER)
    K.klaar()
    print(f"  tegels gelezen: {K.N} runs, {len(K.deuren)} deuren, {len(K.los)} lantaarns/kettingen, {len(K.spook)} spookblokken "
          f"({time.time() - t0:.0f} s)", flush=True)
    return K


# ------------------------------------------------------------------ edges
def randen(K, blok=4_000_000):
    """Directed walk/step/drop/swim/climb edges between runs of neighbouring columns (W_BRIEF player model)."""
    src_all, dst_all = [], []
    pos_van = {}
    idx_of_col = np.searchsorted
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        ncol = K.col + dx * 100000 + dz
        j = np.searchsorted(K.ucol, ncol)
        j = np.minimum(j, len(K.ucol) - 1)
        heeft = K.ucol[j] == ncol
        A_all = np.nonzero(heeft & K.geldig)[0]
        for s in range(0, len(A_all), blok):
            A = A_all[s:s + blok]
            jj = j[A]
            n = K.aantal[jj]
            Ar = np.repeat(A, n)
            off = np.arange(len(Ar)) - np.repeat(np.cumsum(n) - n, n)
            B = np.repeat(K.start[jj], n) + off
            FA, HA, CA = K.F[Ar], K.H[Ar], K.C[Ar]
            FB, HB, CB = K.F[B], K.H[B], K.C[B]
            gB = K.geldig[B]
            # level / drop: feet h = max(FA, FB) must be within A's range
            h = np.maximum(FA, FB)
            neer = gB & (FB <= HA) & (CB >= h + 4) & (CA >= h + 4)
            val = h - FB
            geen_val = (h <= HB + MAX_VAL) & (HB > FB)      # you land on the water/ladder/vine column and hold on
            neer &= (val <= MAX_VAL) | K.water[B] | geen_val
            # up: FB above A's highest feet
            dh = FB - HA
            op = gB & (dh > 0) & (dh <= 2) & (CB >= FB + 4) & (CA >= FB + 4)
            op &= (dh <= 1) | (CA >= HA + 6)
            # off the HIGH half of a stair (stairs count as half blocks above): a jump of 1 from there reaches +1.5
            trap = gB & (dh == 3) & (K.fc[Ar] == STAIR) & (HA == FA) & (CB >= FB + 4) & (CA >= HA + 7)
            ok = neer | op | trap
            src_all.append(Ar[ok].astype(np.int32))
            dst_all.append(B[ok].astype(np.int32))
    return np.concatenate(src_all), np.concatenate(dst_all)


def _paren(K, A, j):
    """All (A run, run in column index j[A]) pairs."""
    jj = j[A]
    n = K.aantal[jj]
    Ar = np.repeat(A, n)
    off = np.arange(len(Ar)) - np.repeat(np.cumsum(n) - n, n)
    return Ar, np.repeat(K.start[jj], n) + off


def sprongen(K, blok=3_000_000):
    """Gap jumps over 1 or 2 open columns (straight, 4 directions), landing at the same height or up to 3 blocks lower (any depth
    into water): take-off needs 3 air blocks over the feet, every column in between must be open from the feet up to 3 blocks
    (no fence/wall/roof in the way), the landing needs room for the arc. This is ordinary survival movement (the hat paths, the
    gaps between rooftops); the report counts targets that are ONLY reached thanks to a jump separately."""
    src_all, dst_all = [], []
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for k in (2, 3):
            ncol = K.col + k * (dx * 100000 + dz)
            j = np.minimum(np.searchsorted(K.ucol, ncol), len(K.ucol) - 1)
            heeft = (K.ucol[j] == ncol) & K.geldig & ~K.water & (K.C - K.F >= 6)
            mids = []
            for m in range(1, k):
                mc = K.col + m * (dx * 100000 + dz)
                jm = np.minimum(np.searchsorted(K.ucol, mc), len(K.ucol) - 1)
                heeft &= K.ucol[jm] == mc
                mids.append(jm)
            A_all = np.nonzero(heeft)[0]
            for s in range(0, len(A_all), blok):
                A = A_all[s:s + blok]
                Ar, B = _paren(K, A, j)
                FA, CA = K.F[Ar], K.C[Ar]
                FB, CB = K.F[B], K.C[B]
                ok = K.geldig[B] & (FB <= FA) & ((FA - FB <= MAX_VAL) | K.water[B]) & (CB >= np.maximum(FB + 4, FA + 5))
                Ar, B, FA = Ar[ok], B[ok], FA[ok]
                gat = np.zeros(len(Ar), bool)
                for jm in mids:                       # every column in between open over [FA, FA + 6]
                    if not len(Ar):
                        break
                    idx = np.arange(len(Ar))
                    P_, R_ = _paren(K, idx, jm[Ar])
                    open_ = (2 * K.b[R_] <= FA[P_]) & (K.C[R_] >= FA[P_] + 6)
                    goed = np.zeros(len(Ar), bool)
                    goed[P_[open_]] = True
                    g2 = np.zeros(len(Ar), bool)       # a real gap: no floor near the feet in between
                    g2[P_[open_ & (K.F[R_] < FA[P_] - 2)]] = True
                    gat |= g2
                    Ar, B, FA, gat = Ar[goed], B[goed], FA[goed], gat[goed]
                Ar, B = Ar[gat], B[gat]
                src_all.append(Ar.astype(np.int32))
                dst_all.append(B.astype(np.int32))
    if not src_all:
        return np.zeros(0, np.int32), np.zeros(0, np.int32)
    return np.concatenate(src_all), np.concatenate(dst_all)


def bfs(G, bronnen):
    N = G.shape[0]
    mask = np.zeros(N, bool)
    bronnen = [b for b in bronnen if b >= 0]
    if not bronnen:
        return mask
    s = np.asarray(sorted(set(bronnen)), np.int64)
    extra = sparse.csr_matrix((np.ones(len(s), np.int8), (np.full(len(s), N), s)), shape=(N + 1, N + 1))
    G2 = sparse.bmat([[G, None], [None, sparse.csr_matrix((1, 1), dtype=np.int8)]], format="csr") + extra
    order = csgraph.breadth_first_order(G2, N, directed=True, return_predecessors=False)
    mask[order[order < N]] = True
    return mask


class Zoeker:
    """Finds runs by world position."""

    def __init__(self, K):
        self.K = K

    def kolom(self, x, z):
        c = int(x) * 100000 + (int(z) + 50000)
        j = np.searchsorted(self.K.ucol, c)
        if j >= len(self.K.ucol) or self.K.ucol[j] != c:
            return range(0)
        return range(self.K.start[j], self.K.start[j] + self.K.aantal[j])

    def run(self, x, y, z, marge=3, alleen_geldig=True):
        """The run in column (x, z) that holds feet height y (or the nearest floor within marge)."""
        K = self.K
        beste, bd = -1, 999
        for i in self.kolom(x, z):
            if alleen_geldig and not K.geldig[i]:
                continue
            if K.b[i] - marge <= y <= K.t[i] + 1:
                f, h = K.F[i] / 2, K.H[i] / 2
                d = f - y if y < f else (y - h if y > h else 0)
                if d < bd:
                    beste, bd = i, d
        return beste

    def rond(self, x, y, z, r=2, marge=3):
        """Runs near (x, y, z): the given column first, then a growing square."""
        uit = []
        for d in range(0, r + 1):
            for dx in range(-d, d + 1):
                for dz in range(-d, d + 1):
                    if max(abs(dx), abs(dz)) != d:
                        continue
                    i = self.run(x + dx, y, z + dz, marge)
                    if i >= 0:
                        uit.append(i)
        return uit


# ------------------------------------------------------------------ targets
def lees_doelen():
    with open(DOELEN, encoding="utf-8") as f:
        return json.load(f)


def lees_quests():
    alle = []
    for f in sorted(glob.glob(os.path.join(QUESTS, "quests*.json"))):
        with open(f, encoding="utf-8") as h:
            alle += json.load(h)["quests"]
    return alle


def zone_van_id(qid):
    if len(qid) > 3 and qid[0] == "z" and "2" <= qid[1] <= "9" and qid[2] == "_":
        return int(qid[1])
    return 1


class Zones:
    """Which zone a point lies in: the kern areas with a zN_ id (first match wins, with their y layers), else by x."""

    def __init__(self, D):
        self.g = []
        for g in D.get("gebieden", []):
            m = re.match(r"z(\d)_", g["id"])
            laag = g.get("laag") or {}
            self.g.append((int(m.group(1)) if m else None, g["doos"], laag.get("min_y", -999), laag.get("max_y", 9999)))

    def van(self, x, y, z):
        for zn, (x0, z0, x1, z1), y0, y1 in self.g:
            if zn and x0 <= x <= x1 and z0 <= z <= z1 and y0 <= y <= y1:
                return zn
        if x < 660 and z > -700:
            return 1
        if x < 1150:
            return 2
        if x < 1720:
            return 3
        if x < 3100:
            return 5 if y >= 118 and x > 2420 else 4
        return 6


# spots that are not meant to be walked to (decoration anchors, sky points, boxes, ship anchors, chimneys, ...)
_NIET_LOPEN = ("schoorste", "lichtstra", "bijenkor", "_min", "_max", "doos", "luchtschip", "mast", ".via", "lijn_e_via", "wolkzuil",
               "pluim", "camera", "film", "scene", "baken", "bruggen", "vuurtorenlamp", "plafond", "spits", "torentop", "_water",
               "anker", "hemel", "lichtjes", "regenboog", "vlucht", "dok.", "schaduw", "vliegroute", "route", "cirkel", "wolk_")
_GEHEIM_WOORDEN = ("geheim", "schat", "kist", "aanwijzing", "knuffel", "parel", "schelp", "flessenpost", "mistbrie", "relief",
                   "kuiken", "lichtje", "vliegerspul", "bijtjes", "bloemen", "lampionhak", "uitzicht", "uitkijk")


def poi_bij(qid, poi):
    """The game's 'you are there' window for a quest POI (NpcHaken.bij: horizontal radius, |dy|), per zone (*Plekken.POI_STRAAL /
    POI_HOOGTE / hoogteVan)."""
    z = zone_van_id(qid)
    straal, dy = {1: (6, 10), 2: (6, 10), 3: (6, 8), 4: (6, 8), 5: (5, 6), 6: (5, 6)}.get(z, (5, 6))
    if z == 1 and "uitkijk" in poi:
        dy = 40
    if z == 2 and (poi.startswith("z2_naald") or poi in ("z2_galmnaald", "z2_uitkijk_poollicht", "z2_drakenravijn")):
        dy = 30
    if poi == "z2_galmnaald":
        straal = 9                      # Z2NpcPlekken.straalVan (W-BEREIK)
    return float(straal), dy


def verzamel_doelen(D, quests):
    """-> list of dict(id, soort, zone, pos, straal, dy, hoofd, quests, bron)."""
    velden = D.get("velden", {})
    npc_pos = {n["id"]: tuple(n["pos"]) for n in D.get("npcs", [])}
    npc_rol = {n["id"]: n.get("rol", "") for n in D.get("npcs", [])}
    for veld, kv in velden.items():                     # *NpcPlekken.NPC / Z4VerhaalPlekken.NPC
        if veld.endswith(".NPC"):
            for k, p in kv.items():
                if isinstance(p, list) and len(p) == 3:
                    npc_pos.setdefault(k, tuple(p))

    def kaart(achter):
        uit = {}
        for veld, kv in velden.items():
            if veld.endswith(achter) and not veld.startswith("npc.QuestPlek"):
                for k, p in kv.items():
                    if isinstance(p, list) and len(p) == 3:
                        uit.setdefault(k, tuple(p))
        return uit

    def qp(n):
        return {k: tuple(v) for k, v in velden.get("npc.QuestPlek." + n, {}).items()}
    poi = {**kaart(".POI"), **qp("POI")}
    gebied = {**kaart(".GEBIED_DOEL"), **qp("GEBIED")}
    baas = {**kaart(".BAAS"), **qp("BAAS")}
    kampvuur = kaart(".KAMPVUUR")
    arenas = {a["id"]: a for a in D.get("arenas", [])}

    def arena_bij(p):
        beste, bd = None, 1e9
        for a in arenas.values():
            c = a["centrum"]
            d = (p[0] - c[0]) ** 2 + (p[2] - c[2]) ** 2
            if d <= (a["straal"] + 4) ** 2 and c[1] - a.get("onder", 4) - 4 <= p[1] <= c[1] + a.get("boven", 24) and d < bd:
                beste, bd = a, d
        return beste

    doelen = {}

    def voeg(did, soort, pos, straal=4.0, dy=3, quest=None, hoofd=False, bron="", zone=None):
        d = doelen.get(did)
        if d is None:
            d = doelen[did] = {"id": did, "soort": soort, "pos": None if pos is None else tuple(int(v) for v in pos), "straal": straal,
                               "dy": dy, "quests": [], "hoofd": False, "bron": bron, "zone": zone}
        if quest:
            if quest not in d["quests"]:
                d["quests"].append(quest)
            d["hoofd"] |= hoofd
            if d["zone"] is None and d["pos"] is None:
                d["zone"] = zone_van_id(quest)
        return d

    # 1. NPCs
    for k, p in npc_pos.items():
        voeg("npc:" + k, "npc", p, 3.5, 3.5, bron=npc_rol.get(k, ""))
    # 2. quests (the QlPlek.van logic: where does each step happen)
    for q in quests:
        qid = q["id"]
        hoofd = q.get("soort") == "hoofd"
        for rol in ("gever", "inleveren"):
            n = q.get(rol)
            if n:
                voeg("npc:" + n, "npc", npc_pos.get(n), 3.5, 3.5, qid, hoofd)
        for d in q["doelen"]:
            soort, doel, plek, bron = d["soort"], d.get("doel", ""), d.get("plek", ""), d.get("bron", "")
            tid, pos, straal, dy = None, None, 5.0, 6
            if plek:
                k, _, i = plek.partition(":")
                if k == "npc":
                    tid, pos, straal, dy = plek, npc_pos.get(i), 3.5, 3.5
                elif k == "poi":
                    tid, pos = plek, poi.get(i)
                    straal, dy = poi_bij(qid, i)
                elif k == "gebied":
                    tid, pos, straal, dy = plek, gebied.get(i), 10.0, 10
                elif k == "baas":
                    tid, pos, straal, dy = plek, baas.get(i), 12.0, 10
            if pos is None:
                if soort in ("praat", "escorte"):
                    tid, pos, straal, dy = "npc:" + doel, npc_pos.get(doel), 3.5, 3.5
                elif soort in ("gebied", "geheim") and doel.startswith("poi:"):
                    tid, pos = doel, poi.get(doel[4:])
                    straal, dy = poi_bij(qid, doel[4:])
                elif soort == "gebied":
                    g = doel.split("|")[0]
                    tid, pos, straal, dy = "gebied:" + g, gebied.get(g), 10.0, 10
                elif soort == "kampvuur":
                    tid, pos = "kampvuur:" + doel, kampvuur.get(doel)
                elif soort in ("opvrolijk", "elite") and doel.startswith("gebied:"):
                    g = doel[7:].split("|")[0]
                    tid, pos, straal, dy = "gebied:" + g, gebied.get(g), 10.0, 10
                elif soort == "verzamel" and bron.startswith("gebied:"):
                    g = bron[7:].split("|")[0]
                    tid, pos, straal, dy = "gebied:" + g, gebied.get(g), 10.0, 10
                elif soort == "baas":
                    g = doel[7:].split("|")[0] if doel.startswith("gebied:") else doel.split(":", 1)[-1]
                    tid, pos, straal, dy = "baas:" + g, baas.get(g), 12.0, 10
                elif soort == "meld" and doel.startswith("quiz:"):
                    tid, pos, straal, dy = "npc:" + q["gever"], npc_pos.get(q["gever"]), 3.5, 3.5
            if tid is None:
                continue            # (level, klasse, meld without a spot, ...: no place)
            voeg(tid, "npc" if tid.startswith("npc:") else "quest", pos, straal, dy, qid, hoofd)
            if pos is not None and (tid.startswith("baas:") or soort == "baas"):
                a = arena_bij(pos)
                if a:
                    voeg("arena:" + a["id"], "arena", a["deur"], 3.0, 3, qid, hoofd)
    # 3. arenas (door), spawners
    for a in arenas.values():
        voeg("arena:" + a["id"], "arena", a["deur"], 3.0, 3)
    for s in D.get("spawners", []):
        # (an open-air spawner puts its mobs on the surface inside its radius: its centre y is nominal; cave spawners keep y)
        voeg("mobs:" + s["id"], "mobs", s["centrum"], max(4.0, float(s["straal"])), 10 if s.get("grot") else 400,
             bron=s.get("type", ""))
    # 4. puzzles / secrets
    for p in D.get("puzzels", []):
        if "pos" in p:
            voeg("puzzel:" + p["id"], "geheim" if p.get("stil") or p.get("soort") == "geheim" else "puzzel", p["pos"],
                 6.0 + max(p.get("breed", 0), p.get("diep", 0)) / 2, 6)
    # 5. all other *Plekken spots (secrets / collectables by name, the rest "overig")
    for veld, kv in velden.items():
        if veld.startswith("npc.QuestPlek") or veld.endswith(".NPC"):
            continue
        for k, p in kv.items():
            if not (isinstance(p, list) and len(p) == 3):
                continue
            naam = (veld + "." + k).lower()
            if any(w in naam for w in _NIET_LOPEN):
                continue
            soort = "geheim" if any(w in naam for w in _GEHEIM_WOORDEN) else "overig"
            # secrets/collectables are clicked (block reach ~4.5 from the eye) or found by walking up (VIND_STRAAL 3, VIND_HOOGTE 4)
            voeg("plek:" + veld + ("." + k if k != "_" else ""), soort, p, 4.5 if soort == "geheim" else 4.0,
                 5 if soort == "geheim" else 4, bron=veld)
    zones = Zones(D)
    for d in doelen.values():
        if d["zone"] is None:
            d["zone"] = zones.van(*d["pos"]) if d["pos"] else 1
    return list(doelen.values()), arenas


# ------------------------------------------------------------------ links (transport edges)
# Areas the Java code builds at runtime (not in the map tiles): their targets are listed, not checked.
RUNTIME_GEBIEDEN = [
    ("De Verzonken Sterrenwacht (Z6Bouw.sterrenwacht: rooms under the Ruinedal, entered by the hatch link)", 3930, 15, -995, 4075, 48, -945),
    ("Raid binnentoren (Z6RaidBouw: the template raid_binnentoren, placed at runtime)", 4100, 0, -730, 4190, 66, -645),
    ("Raid binnentoren top (Z6RaidBouw)", 4100, 100, -730, 4190, 320, -645),
]


def runtime_gebied(x, y, z):
    for _, x0, y0, z0, x1, y1, z1 in RUNTIME_GEBIEDEN:
        if x0 <= x <= x1 and y0 <= y <= y1 and z0 <= z <= z1:
            return True
    return False


def poi_tabellen():
    """All `id` | x, y, z rows of the POI_*.md tables."""
    uit = {}
    for f in sorted(glob.glob(os.path.join(ROOT, "POI_*.md"))):
        for m in re.finditer(r"^\| `([^`]+)` \| (-?\d+), (-?\d+), (-?\d+) \|", open(f, encoding="utf-8").read(), re.M):
            uit.setdefault(m.group(1), tuple(int(m.group(i)) for i in (2, 3, 4)))
    return uit


def links(D, poi):
    """(naam, van, naar, beide) transport links: airship lines, lifts, hatches, the dump's own links."""
    uit = []
    for l in D.get("lijnen", []):
        uit.append(("luchtschip " + l["id"], tuple(l["a"]["plek"]), tuple(l["b"]["plek"]), True))
    for k, v in poi.items():                                     # city lifts lift_*_voet <-> lift_*_top, *_onder <-> *_boven
        for a, b in (("_voet", "_top"), ("_onder", "_boven")):
            if k.endswith(a) and "lift" in k and k[:-len(a)] + b in poi:
                uit.append(("lift " + k[:-len(a)], v, poi[k[:-len(a)] + b], True))
    for l in D.get("links", []):
        uit.append((l["naam"], tuple(l["van"]), tuple(l["naar"]), bool(l.get("beide", False))))
    return uit


# ------------------------------------------------------------------ the check
class Uitslag:
    pass


def analyseer(K=None):
    t0 = time.time()
    D = lees_doelen()
    quests = lees_quests()
    poi = poi_tabellen()
    if K is None:
        K = laad()
    src, dst = randen(K)
    js, jd = sprongen(K)
    print(f"  randen: {len(src)} lopen + {len(js)} sprongen ({time.time() - t0:.0f} s)", flush=True)
    n_lopen = len(src)
    src = np.concatenate([src, js])
    dst = np.concatenate([dst, jd])
    del js, jd
    Z = Zoeker(K)
    L = links(D, poi)
    extra_s, extra_d, link_info = [], [], []
    for naam, a, b, beide in L:
        ra = Z.rond(a[0], a[1], a[2], 2)
        rb = Z.rond(b[0], b[1], b[2], 2)
        link_info.append((naam, a, b, beide, ra[0] if ra else -1, rb[0] if rb else -1))
        if ra and rb:
            extra_s.append(ra[0])
            extra_d.append(rb[0])
            if beide:
                extra_s.append(rb[0])
                extra_d.append(ra[0])
    src = np.concatenate([src, np.array(extra_s, np.int32)])
    dst = np.concatenate([dst, np.array(extra_d, np.int32)])
    G = sparse.csr_matrix((np.ones(len(src), np.int8), (src, dst)), shape=(K.N, K.N))
    zonder = np.ones(len(src), bool)
    zonder[n_lopen:n_lopen + (len(src) - n_lopen - len(extra_s))] = False      # walking only (no gap jumps), links kept
    G0 = sparse.csr_matrix((np.ones(int(zonder.sum()), np.int8), (src[zonder], dst[zonder])), shape=(K.N, K.N))
    del src, dst, zonder
    GT = G.T.tocsr()
    # zone entries: the start + every airship pier (+ the dump's extra entries)
    ingangen = {"start (aankomst)": poi.get("aankomst", (-384, 65, 1130))}
    for l in D.get("lijnen", []):
        for h in (l["a"], l["b"]):
            ingangen[f"luchtschip {h['id']}"] = tuple(h["plek"])
    # the travel map (Reiskaart) lands you at these spots (QuestMotor.vliegNaar per destination)
    for veld in ("wereld.WereldPlekken.KAMP_LANDING", "wereld.WereldPlekken.HAVEN_LANDING", "zone2.wereld.Z2WereldPlekken.DOK_LANDING",
                 "zone3.wereld.Z3WereldPlekken.DOK_LANDING", "zone4.verhaal.Z4VerhaalPlekken.LANDING",
                 "zone5.verhaal.Z5VerhaalPlekken.AANKOMST", "zone6.verhaal.Z6VerhaalPlekken.AANKOMST"):
        p = D.get("velden", {}).get(veld, {}).get("_")
        if p:
            ingangen["reiskaart " + veld.split(".")[-2] + "." + veld.split(".")[-1]] = tuple(p)
    zaden = {}
    for naam, p in ingangen.items():
        r = Z.rond(p[0], p[1], p[2], 3)
        zaden[naam] = r[0] if r else -1
    start = zaden["start (aankomst)"]
    FR = bfs(G, [start])
    FR0 = bfs(G0, [start])
    del G0
    BR = bfs(GT, [r for r in zaden.values() if r >= 0])
    print(f"  bereikt vanaf de start: {int(FR.sum())} runs, terug naar een ingang: {int((FR & BR).sum())} ({time.time() - t0:.0f} s)",
          flush=True)
    per_ingang = {}                 # (one BFS per entry costs ~10 s each on the full graph: off; the start reaches everything)
    U = Uitslag()
    U.K, U.Z, U.G, U.GT, U.FR, U.BR, U.zaden, U.per_ingang, U.links, U.D, U.poi = K, Z, G, GT, FR, BR, zaden, per_ingang, link_info, D, poi
    U.doelen, U.arenas = verzamel_doelen(D, quests)
    # stand points of reached runs: feet at F/2, and for water/ladder runs also at the top of the swim/climb (H/2)
    staan = FR & K.geldig & ~K.bodem
    ids = np.nonzero(staan)[0]
    hoog = ids[K.H[ids] > K.F[ids] + 1]
    U.pid = np.concatenate([ids, hoog])
    U.py = np.concatenate([K.F[ids] / 2.0, K.H[hoog] / 2.0])
    U.staan_ids = U.pid
    px, pz = K.x[U.pid] + 0.5, K.z[U.pid] + 0.5
    U.boom = cKDTree(np.c_[px, U.py, pz])
    boom2 = cKDTree(np.c_[px, pz])
    zonder = FR0[U.pid]
    xs0, xs1, zs0, zs1 = int(K.x.min()), int(K.x.max()), int(K.z.min()), int(K.z.max())
    for d in U.doelen:
        d["ok"] = False
        if d["pos"] is None:
            continue
        if not (xs0 <= d["pos"][0] <= xs1 and zs0 <= d["pos"][2] <= zs1) or runtime_gebied(*d["pos"]):
            d["buiten_kaart"] = True        # a runtime-built area (e.g. the guild valley at -12000, 12000): not in the tiles
            continue
        x, y, z = d["pos"]
        kand = np.array(boom2.query_ball_point([x + 0.5, z + 0.5], d["straal"]), np.int64)
        if len(kand):
            kand = kand[np.abs(U.py[kand] - y) <= d["dy"]]
        if len(kand):
            d["ok"] = True
            q = kand[np.argmin((px[kand] - x) ** 2 + (pz[kand] - z) ** 2 + (U.py[kand] - y) ** 2)]
            d["via"] = ["start"]
            d["sprong"] = not bool(zonder[kand].any())
            d["terug"] = bool(BR[U.pid[q]])
        else:
            dist, q = U.boom.query([x + 0.5, y, z + 0.5])
            j = U.pid[q]
            d["dichtst"] = (int(K.x[j]), int(U.py[q]), int(K.z[j]), round(float(dist), 1))
    U.t = time.time() - t0
    return U


def eenrichting(U, min_kolommen=6):
    """Reached areas without a way back to a zone entry (FR & ~BR), grouped."""
    K = U.K
    val = U.FR & ~U.BR & K.geldig
    idx = np.nonzero(val)[0]
    if len(idx) == 0:
        return []
    sub = U.G[idx][:, idx]
    n, lab = csgraph.connected_components(sub, directed=False)
    groepen = collections.defaultdict(list)
    for i, l in zip(idx, lab):
        groepen[l].append(i)
    uit = []
    for g in groepen.values():
        g = np.array(g)
        kol = len(set(zip(K.x[g].tolist(), K.z[g].tolist())))
        if kol < min_kolommen:
            continue
        laag = g[np.argmin(K.b[g])]
        uit.append({"kolommen": kol, "pos": (int(K.x[laag]), int(K.b[laag]), int(K.z[laag])), "water": bool(K.water[g].any()),
                    "bodem": bool(K.bodem[g].any()), "y": (int(K.b[g].min()), int(K.b[g].max()))})
    uit.sort(key=lambda u: -u["kolommen"])
    return uit


def arena_uitgangen(U):
    """Per arena: the reached runs inside; can you get from there back to a zone entry?"""
    K = U.K
    uit = []
    for a in U.arenas.values():
        cx, cy, cz = a["centrum"]
        if runtime_gebied(cx, cy, cz):
            continue
        r = a["straal"]
        x0, x1 = cx - r, cx + r
        sel = (K.x >= x0) & (K.x <= x1) & (K.z >= cz - r) & (K.z <= cz + r) & K.geldig & ~K.bodem
        sel &= ((K.x - cx) ** 2 + (K.z - cz) ** 2 <= r * r) & (K.b >= cy - a.get("onder", 4) - 2) & (K.b <= cy + a.get("boven", 24))
        binnen = np.nonzero(sel)[0]
        fr = binnen[U.FR[binnen]]
        vast = binnen[~U.BR[binnen]]
        # where Questlopen drops you (between door and centre) / the boss spot
        dx, dz = a["deur"][0] - cx, a["deur"][2] - cz
        ql = None
        for f in (0.55, 0.45, 0.65, 0.35, 0.75, 0.25):
            p = (int(cx + dx * f), cy, int(cz + dz * f))
            rr = U.Z.rond(p[0], p[1], p[2], 2, 4)
            if rr:
                ql = (p, rr[0])
                break
        uit.append({"id": a["id"], "centrum": (cx, cy, cz), "deur": tuple(a["deur"]), "binnen": len(binnen), "bereikt": len(fr),
                    "vast": len(vast), "ql": None if ql is None else (ql[0], bool(U.BR[ql[1]]), bool(U.FR[ql[1]])),
                    "vast_pos": None if len(vast) == 0 else (int(K.x[vast[0]]), int(K.b[vast[0]]), int(K.z[vast[0]]))})
    return uit


def instappen(U):
    """Airship boarding: the nearest reached stand cell to the docked ship's hull (eye -> hull box). The hull: ~14 long along
    the pier->ship direction, ~6 wide, deck .. deck + 6 (balloon click boxes higher up don't help a player on the pier)."""
    K = U.K
    uit = []
    for l in U.D.get("lijnen", []):
        for h in (l["a"], l["b"]):
            dx, dy, dz = h["dok"]
            px0, _, pz0 = h["plek"]
            ux, uz = dx - px0 - 0.5, dz - pz0 - 0.5
            n = max((ux * ux + uz * uz) ** 0.5, 1e-6)
            ux, uz = ux / n, uz / n
            kand = U.boom.query_ball_point([dx, dy, dz], 20)
            beste = None
            for i in kand:
                j = U.staan_ids[i]
                px, py, pz = K.x[j] + 0.5 - dx, U.py[i] + 1.6, K.z[j] + 0.5 - dz
                lang = px * ux + pz * uz
                breed = -px * uz + pz * ux
                hl = max(abs(lang) - 7, 0)
                hb = max(abs(breed) - 3, 0)
                hy = max(dy - py, 0, py - (dy + 6))
                d = (hl * hl + hb * hb + hy * hy) ** 0.5
                if beste is None or d < beste[0]:
                    beste = (d, (int(K.x[j]), int(U.py[i]), int(K.z[j])))
            uit.append({"lijn": l["id"], "haven": h["id"], "plek": tuple(h["plek"]), "dok": (dx, dy, dz),
                        "afstand": None if beste is None else round(beste[0], 1), "cel": None if beste is None else beste[1]})
    return uit


def deuren(U):
    """Every door (lower half): reachable? both sides open? interior walkable?"""
    K = U.K
    uit = {"nep": [], "dicht": [], "onbereikt_binnen": [], "ok": 0, "ijzer": 0}
    R = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
    for x, y, z, f, n, c in K.deuren:
        if c == IJZERDEUR:
            uit["ijzer"] += 1
            continue
        dx, dz = R.get(f, (0, -1))
        kanten = []
        for s in (1, -1):
            r = U.Z.run(x + dx * s, y, z + dz * s, 1)
            kanten.append(r)
        hier = U.Z.run(x, y, z, 1)
        if hier < 0:
            continue
        if not U.FR[hier]:
            # not reached at all: only interesting when one side is reached (a door into nowhere next to a reached street)
            if any(r >= 0 and U.FR[r] for r in kanten):
                uit["dicht"].append((x, y, z, n))
            continue
        open_ = [r for r in kanten if r >= 0 and K.F[r] // 2 >= y - 1 and K.F[r] // 2 <= y + 1]
        if len(open_) < 2:
            uit["nep"].append((x, y, z, n))
        elif not all(U.FR[r] for r in open_):
            uit["onbereikt_binnen"].append((x, y, z, n))
        else:
            uit["ok"] += 1
    return uit


def deco_op_pad(U):
    """Lanterns/chains (blocking deco) IN a walked line: standing on a walked floor/stair with walked neighbours at that
    height ("staat"), or hanging so low over a walked floor that only 1 block of air is left under it ("hangt")."""
    K = U.K
    uit = []
    ys = {}
    for x, y, z, n, _ in K.los:
        ys.setdefault((x, z), set()).add(y)
    for x, y, z, n, cl_onder in K.los:
        if y - 1 in ys.get((x, z), ()):
            continue                                   # (a chain/lantern under another one: judge the lowest only)
        onder = None                                   # the run directly under the block (ends at y - 1)
        for i in U.Z.kolom(x, z):
            if K.t[i] == y - 1:
                onder = i
        # standing: the run under it is the floor block itself (no run) -> look at the block class via the neighbours
        voet = None
        if onder is not None and K.b[onder] == y - 1:
            voet = y - 1                               # 1 air under a hanging lantern: you can't pass
            soort = "hangt"
        elif onder is None and cl_onder in (SOLID, SLAB, STAIR):
            voet = y                                   # it stands on a floor (or a stair: feet y - 0.5)
            soort = "staat"
        else:
            continue
        buren = 0
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            r = U.Z.run(x + dx, voet, z + dz, 1)
            if r >= 0 and U.FR[r] and abs(K.F[r] / 2 - voet) <= 1:
                buren += 1
        keten = n.split(":")[-1] in ("iron_chain", "chain")
        if soort == "hangt":
            ok = buren >= 1
        elif cl_onder == STAIR:
            ok = buren >= 1
        elif cl_onder == SLAB:
            ok = buren >= 2
        else:
            ok = buren >= (2 if keten else 4)
        if ok:
            uit.append((x, y, z, n, soort))
    return uit


def spook_op_rand(U):
    """No-collision look-alikes (gold rails, banners, ...) on a walked edge: you think it's a railing but you walk through."""
    K = U.K
    uit = []
    for x, y, z, n in K.spook:
        if n.split(":")[-1] not in ("gouden_kaarsenrand",):
            continue
        r = U.Z.run(x, y, z, 1, alleen_geldig=False)
        if r >= 0 and U.FR[r] and abs(K.F[r] / 2 - y) <= 1:
            uit.append((x, y, z, n))
    return uit


# ------------------------------------------------------------------ report
ZONENAMEN = {1: "Zone 1 - Zwamzoet + Knuffelkamp", 2: "Zone 2 - De Splinterpieken", 3: "Zone 3 - De Schemerwortel",
             4: "Zone 4 - De Lampionkust", 5: "Zone 5 - Hoogvads", 6: "Zone 6 - Het Zijdeland"}
SOORTEN = (("hoofd", "hoofdquest-doelen"), ("zij", "zijquest-doelen"), ("npc", "NPC's"), ("geheim", "geheimen/verzamelplekken"),
           ("arena", "baasarena's (deur)"), ("mobs", "mob-spawners"), ("puzzel", "puzzels"), ("overig", "overige plekken"))


def groep(d):
    if d["quests"]:
        return "hoofd" if d["hoofd"] else ("npc" if d["soort"] == "npc" and not d["quests"] else "zij")
    return d["soort"] if d["soort"] in ("npc", "geheim", "arena", "mobs", "puzzel") else "overig"


def tp(p):
    return f"/tp @s {p[0]} {p[1]} {p[2]}"


def schrijf_rapport(U, extra, zone_filter=None):
    K = U.K
    regels = ["# BEREIK_RAPPORT: kan een survival-speler overal komen? (W-BEREIK)", "",
              f"**GENERATED** by `python tools/vadsrijk/bereikcheck.py` ({time.strftime('%Y-%m-%d %H:%M')}). Global flood over all "
              f"{len(set(zip((K.x // T).tolist()[::997], (K.z // T).tolist()[::997])))}+ tiles from the start spawn, airship lines, lifts "
              "and hatches as edges. Player model and sources: see the docstring of the script.", "",
              "A target counts as **reached** when a reached stand cell lies within its radius (NPC 3.5 blocks, quest POI 5, area 10, "
              "boss 12, spawner = its radius) and height window. \"via\" = which zone entries reach it (the start reaches everything "
              "that is reached).", ""]
    # summary table
    tellers = collections.defaultdict(lambda: [0, 0])
    for d in U.doelen:
        if d["pos"] is None or d.get("buiten_kaart"):
            continue
        g = groep(d)
        tellers[(d["zone"], g)][0] += 1
        tellers[(d["zone"], g)][1] += d["ok"]
        if d["soort"] == "npc" and g != "npc":
            tellers[(d["zone"], "npc")][0] += 1
            tellers[(d["zone"], "npc")][1] += d["ok"]
    regels += ["## Samenvatting per zone (bereikt / totaal)", "",
               "| zone | " + " | ".join(n for _, n in SOORTEN) + " |", "|---|" + "---|" * len(SOORTEN)]
    for zn in sorted(ZONENAMEN):
        cel = []
        for g, _ in SOORTEN:
            n, ok = tellers.get((zn, g), [0, 0])
            cel.append("-" if n == 0 else (f"**{ok}/{n}**" if ok < n else f"{ok}/{n}"))
        regels.append(f"| {zn} | " + " | ".join(cel) + " |")
    sprong = [d for d in U.doelen if d.get("ok") and d.get("sprong") and groep(d) in ("hoofd", "zij", "npc", "geheim")]
    regels += ["", f"Reached ONLY with a gap jump (1-2 blocks, the hat paths / rooftops): {len(sprong)} quest/NPC/secret targets"
               + (": " + ", ".join(f"`{d['id']}`" for d in sprong[:40]) if sprong else "")]
    onbekend = [d for d in U.doelen if d["pos"] is None]
    buiten = [d for d in U.doelen if d.get("buiten_kaart")]
    regels += ["", "Runtime-built areas (not in the tiles, not checked here): the guild valley (-12000, 12000), "
               + "; ".join(g[0] for g in RUNTIME_GEBIEDEN) + "."]
    regels += ["", f"Targets outside the map tiles (areas built at runtime, e.g. the guild valley; not checked): "
               + ", ".join(f"`{d['id']}`" for d in buiten)]
    regels += ["", f"Quest targets the game cannot place (no NPC/POI with that id): {len(onbekend)}"]
    for d in sorted(onbekend, key=lambda d: d["id"]):
        regels.append(f"- `{d['id']}` ({', '.join(d['quests'][:4])})")
    # per zone details
    for zn in sorted(ZONENAMEN):
        if zone_filter and zn != zone_filter:
            continue
        regels += ["", f"## {ZONENAMEN[zn]}", ""]
        mis = [d for d in U.doelen if d["zone"] == zn and d["pos"] is not None and not d["ok"] and not d.get("buiten_kaart")]
        if not mis:
            regels.append("Alles bereikt.")
        for g, gnaam in SOORTEN:
            lijst = [d for d in mis if groep(d) == g]
            if not lijst:
                continue
            regels += [f"### Niet bereikt: {gnaam} ({len(lijst)})", "", "| doel | plek | dichtstbijzijnde bereikte cel (afstand) | quests |",
                       "|---|---|---|---|"]
            for d in sorted(lijst, key=lambda d: d["id"]):
                dd = d.get("dichtst")
                regels.append(f"| `{d['id']}` | {d['pos'][0]}, {d['pos'][1]}, {d['pos'][2]} | "
                              + (f"{dd[0]}, {dd[1]}, {dd[2]} ({dd[3]})" if dd else "-") + f" | {', '.join(d['quests'][:3])} |")
            regels.append("")
        geen_terug = [d for d in U.doelen if d["zone"] == zn and d.get("ok") and not d.get("terug", True) and groep(d) in ("hoofd", "zij", "npc")]
        if geen_terug:
            regels += [f"### Bereikt maar zonder weg terug ({len(geen_terug)})", ""]
            for d in geen_terug[:40]:
                regels.append(f"- `{d['id']}` {d['pos']}")
    # arenas
    regels += ["", "## Baasarena's: binnen bereikt, en is er een uitgang?", "", "| arena | deur | binnen (bereikt) | cellen zonder weg terug | Questlopen-plek terug? |",
               "|---|---|---|---|---|"]
    for a in extra["arenas"]:
        ql = a["ql"]
        regels.append(f"| `{a['id']}` | {a['deur'][0]}, {a['deur'][1]}, {a['deur'][2]} | {a['binnen']} ({a['bereikt']}) | "
                      + (f"**{a['vast']}** bv. {a['vast_pos']}" if a["vast"] else "0") + " | "
                      + ("-" if ql is None else ("ja" if ql[1] else f"**NEE** {ql[0]}")) + " |")
    # boarding
    regels += ["", "## Luchtschepen: kun je instappen? (bereikte cel binnen ~4 blokken van het schip)", "",
               "Boarding geometry belongs to slice w-schepen; this only reports.", "", "| lijn | haven | pier (bereikt?) | dichtste cel bij het schip | afstand oog-romp |", "|---|---|---|---|---|"]
    for b in extra["instappen"]:
        r = U.zaden.get("luchtschip " + b["haven"], -1)
        regels.append(f"| {b['lijn']} | `{b['haven']}` | {b['plek'][0]}, {b['plek'][1]}, {b['plek'][2]} ({'ja' if r >= 0 and U.FR[r] else '**NEE**'}) | "
                      f"{b['cel']} | " + ("-" if b["afstand"] is None else (f"**{b['afstand']}**" if b["afstand"] > 4 else str(b["afstand"]))) + " |")
    # links
    regels += ["", "## Verbindingen (links) in de graaf", ""]
    for naam, a, b, beide, ra, rb in U.links:
        st = "ok" if ra >= 0 and rb >= 0 else "**geen stacel**"
        bereikt = "" if ra < 0 else (" (van-kant bereikt)" if U.FR[ra] else " (van-kant NIET bereikt)")
        regels.append(f"- {naam}: {a} {'<->' if beide else '->'} {b}: {st}{bereikt}")
    # one-way
    regels += ["", "## Eenrichtingsgebieden (bereikt, maar geen weg terug naar een zone-ingang; >= 6 kolommen)", ""]
    ev = extra["eenrichting"]
    regels.append(f"{len(ev)} groepen. De grootste 60:")
    regels += ["", "| plek (laagste) | kolommen | y | water | onder de kaart |", "|---|---|---|---|---|"]
    for e in ev[:60]:
        regels.append(f"| {e['pos'][0]}, {e['pos'][1]}, {e['pos'][2]} | {e['kolommen']} | {e['y'][0]}..{e['y'][1]} | {'ja' if e['water'] else ''} | {'ja' if e['bodem'] else ''} |")
    # doors
    dr = extra["deuren"]
    regels += ["", "## Deuren", "", f"- ok (beide kanten open en bereikt): {dr['ok']}", f"- ijzeren (nep/op slot) deuren: {dr['ijzer']}",
               f"- deur tegen een muur / zonder binnenkant (houten deur, nep): {len(dr['nep'])}",
               f"- deur bereikt maar de binnenkant niet: {len(dr['onbereikt_binnen'])}", f"- deur naast een bereikte straat maar zelf niet bereikbaar: {len(dr['dicht'])}", ""]
    for titel, k in (("Nep-deuren (houten deur tegen een muur)", "nep"), ("Binnenkant niet bereikt", "onbereikt_binnen"), ("Niet bereikbare deuren", "dicht")):
        if dr[k]:
            regels += [f"### {titel} ({len(dr[k])})", ""]
            per_zone = collections.Counter()
            zones = Zones(U.D)
            for x, y, z, n in dr[k]:
                per_zone[zones.van(x, y, z)] += 1
            regels.append("per zone: " + ", ".join(f"zone {a}: {b}" for a, b in sorted(per_zone.items())))
            regels.append("")
            for x, y, z, n in dr[k][:80]:
                regels.append(f"- {x}, {y}, {z} `{n.split(':')[-1]}`")
            regels.append("")
    # deco on paths
    dp = extra["deco"]
    regels += ["", f"## Lantaarns/kettingen op looplijnen ({len(dp)})", "", "Blocking deco standing on a walked floor (2+ walked neighbours at that height) or hanging at head height over a walked floor."]
    per_zone = collections.Counter()
    zones = Zones(U.D)
    for x, y, z, n, s in dp:
        per_zone[zones.van(x, y, z)] += 1
    regels.append("per zone: " + ", ".join(f"zone {a}: {b}" for a, b in sorted(per_zone.items())))
    regels.append("")
    for x, y, z, n, s in dp[:150]:
        regels.append(f"- {x}, {y}, {z} `{n.split(':')[-1]}` {s}")
    sp = extra["spook"]
    regels += ["", f"## Spookrelingen: gouden kaarsenrand (geen botsing) op een looprand ({len(sp)})", ""]
    for x, y, z, n in sp[:80]:
        regels.append(f"- {x}, {y}, {z} `{n.split(':')[-1]}`")
    with open(RAPPORT, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(regels) + "\n")
    return tellers


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--zone", type=int, default=None)
    ap.add_argument("--punt", action="append", default=[], help="x,y,z: debug one spot")
    ap.add_argument("--strikt", action="store_true")
    a = ap.parse_args()
    t0 = time.time()
    print("bereikcheck: tegels lezen ...", flush=True)
    U = analyseer()
    for p in a.punt:
        x, y, z = (int(v) for v in p.split(","))
        r = U.Z.rond(x, y, z, 2)
        if r:
            i = r[0]
            print(f"  punt {p}: run {i} (vloer {U.K.F[i] / 2}, plafond {U.K.C[i] / 2}) bereikt={bool(U.FR[i])} terug={bool(U.BR[i])}")
        else:
            print(f"  punt {p}: geen stacel")
        dist, j = U.boom.query([x + 0.5, y, z + 0.5])
        k = U.staan_ids[j]
        print(f"    dichtste bereikte cel: {U.K.x[k]}, {U.K.F[k] / 2}, {U.K.z[k]} ({dist:.1f})")
    extra = {"arenas": arena_uitgangen(U), "instappen": instappen(U), "eenrichting": eenrichting(U), "deuren": deuren(U),
             "deco": deco_op_pad(U), "spook": spook_op_rand(U)}
    tellers = schrijf_rapport(U, extra, a.zone)
    os.makedirs(CACHE, exist_ok=True)
    with open(os.path.join(CACHE, "bereik.json"), "w", encoding="utf-8") as f:
        json.dump({"doelen": [{k: v for k, v in d.items()} for d in U.doelen]}, f, default=str)
    print(f"  {os.path.relpath(RAPPORT, ROOT)} ({time.time() - t0:.0f} s)")
    for zn in sorted(ZONENAMEN):
        s = []
        for g, n in SOORTEN[:5]:
            tot, ok = tellers.get((zn, g), [0, 0])
            if tot:
                s.append(f"{g} {ok}/{tot}")
        print(f"  zone {zn}: " + ", ".join(s))
    if a.strikt:
        slecht = [d for d in U.doelen if d["pos"] is not None and not d["ok"] and (d["hoofd"] or d["soort"] == "npc")]
        if slecht:
            sys.exit(1)


if __name__ == "__main__":
    main()
