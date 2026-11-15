"""1.10.4: the Ambachtseiland (+ its bridge) and the new Knuffelkamp path into the zone-1 map tiles, plus the two live patches.

    python tools/vadsrijk/kamp/ambacht_kaart.py            stamp the tiles (idempotent) and write the POIs
    python tools/vadsrijk/kamp/ambacht_kaart.py --patch    + (re)write the two live patches (against the LIVE tiles, git tag BASIS)
    python tools/vadsrijk/kamp/ambacht_kaart.py --droog    only report what would change (no files written)

maak_kaart.py runs it after terugportalen.py (a full regeneration keeps the islet). Like terugportalen/speeltuin it is ONE pass over
the written tiles, so every tile it does not touch stays byte-identical and the touched ones only change where the islet/path is.

How (all in the approved design frame of ambacht_render.py: big scene column (0, 0) = world (-704, 865), scene y + 52 = world y):
  * B0 = the Knuffelkamp exactly as 1.10.3 built it (kamp.bouw(ambacht=False)), B1 = the camp WITH the new path
    (kamp.ambachtpad: path, lamp posts, signs, trees out) and the islet (ambachtseiland.bouw). The cells where B0 and B1 differ
    are what changes; the design's own sea (water / sea floor at y <= 62 in sea columns) is left to the real zone-1 sea.
  * islet land columns are rewritten whole: from the real sea floor up (stone below the design shell, then the design blocks,
    air above); bridge piles / jetty posts go down to the sea floor.
  * the new cells are hollowed like the map writer does (only inside the touched cells), then the connection pass (verbind.py)
    and the walking-line pass (vrijpad.py) run over the touched tiles, as in maak_kaart.py.
  * the live patches (data/guhs/vadsrijk/patch/ambachtseiland.json + kamp_pad.json, each with a .stempel.nbt) are the cell
    differences against the LIVE tiles (git tag BASIS, the release the server runs), split at world x -476 (bridge end).
    Each cell carries the old (generated) state, so the live patch can tell a player-changed block from the generated one.
"""
import argparse
import gzip
import json
import math
import os
import struct
import subprocess
import sys
import time

import numpy as np

try:
    import psutil
    psutil.Process().nice(psutil.BELOW_NORMAL_PRIORITY_CLASS if os.name == "nt" else 10)
except Exception:
    pass

HIER = os.path.dirname(os.path.abspath(__file__))
VADSRIJK = os.path.dirname(HIER)
ROOT = os.path.abspath(os.path.join(VADSRIJK, "..", ".."))
sys.path.insert(0, HIER)
import kamp as KP  # noqa: E402
import vrkit as K  # noqa: E402
import ambachtseiland as A  # noqa: E402
sys.path.insert(0, VADSRIJK)
sys.path.insert(0, os.path.join(VADSRIJK, "wereld"))
import verbind as VB  # noqa: E402
import blokken as BL  # noqa: E402
import nbtsnel  # noqa: E402

BASIS = "v1.10.3"                                 # the tiles the live server generated its world from
KAMP_OX, KAMP_OZ = -534, 865                      # kamp scene (0, 0) in the world (kamp_wereld.OX/OZ)
PAD = 170                                         # ambacht_render.PAD: the big scene extends this far west of the camp scene
OX, OY, OZ = KAMP_OX - PAD, 52, KAMP_OZ
SPLITS_X = -476                                   # x <= this: patch "ambachtseiland" (islet + bridge), else "kamp_pad"
T, HOOG = VB.T, VB.HOOG
RES = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "vadsrijk")
PATCHES = os.path.join(RES, "patch")
PLEKKEN = os.path.join(HIER, "ambacht_plekken.json")
ZEE_BLOK = {"minecraft:water", "minecraft:sand", "minecraft:prismarine", "minecraft:light_blue_terracotta"}
STEEN = ["minecraft:stone", "minecraft:andesite", "minecraft:stone", "minecraft:tuff", "minecraft:stone",
         "minecraft:calcite", "minecraft:stone", "minecraft:diorite"]


# ------------------------------------------------------------------------------------------------------------ the scenes
def kamp_scene(met_pad):
    S0 = K.Scene(*KP.KAMP_MAAT)
    I0 = KP.bouw(S0, ambacht=met_pad)
    X0, Y0, Z0 = S0.v.shape
    S = K.Scene(X0 + PAD, Y0, Z0)
    S.pal, S.idx = list(S0.pal), dict(S0.idx)
    S.v[PAD:, :, :] = S0.v
    S.v[:PAD, KP.ZEE - 1, :] = S.id("minecraft:light_blue_terracotta")      # (as ambacht_render: the design's open sea)
    S.v[:PAD, KP.ZEE, :] = S.id("minecraft:water")
    kH = np.full((X0 + PAD, Z0), -1, int)
    kH[PAD:, :] = I0.H
    return S, I0, kH


def scenes():
    t = time.time()
    B0, _, _ = kamp_scene(False)
    B1, I1, kH = kamp_scene(True)
    C = A.bouw(B1, OX, OZ, kH)
    B0.fix_connections()
    B1.fix_connections()
    print(f"scenes klaar ({time.time() - t:.0f} s): {len(I1.ambacht_weg)} bomen weg van het pad, lampen {len(I1.ambacht_lampen)}, "
          f"borden {len(I1.ambacht_borden)}, ertsen {C.n_ertsen}", flush=True)
    return B0, B1, I1, kH, C


def namen(S):
    return np.array([BL.norm(s) if s != "minecraft:air" else "minecraft:air" for s in S.pal], object)


# ------------------------------------------------------------------------------------------------------------ the changes
def wijzigingen(B0, B1, I1, kH, C):
    """{(wx, wy, wz): new state name ('minecraft:air' = clear)} + the islet land columns {(wx, wz): top y} + the sign nbt."""
    n0, n1 = namen(B0), namen(B1)
    G = {}
    g0 = np.array([G.setdefault(n, len(G)) for n in n0], np.int32)[B0.v]
    g1 = np.array([G.setdefault(n, len(G)) for n in n1], np.int32)[B1.v]
    gn = [None] * len(G)
    for n, i in G.items():
        gn[i] = n
    verschil = g0 != g1
    land = C.I.H >= 0
    verschil &= ~land[:, None, :]                               # islet columns: rewritten whole below
    zeekol = (~land) & (kH < 0)
    zee_ids = [i for n, i in G.items() if n.split("[")[0] in ZEE_BLOK]
    laag = np.zeros(verschil.shape, bool)
    laag[:, :KP.ZEE + 1, :] = True
    verschil &= ~(zeekol[:, None, :] & laag & np.isin(g1, zee_ids))
    cellen = {}
    for (x, y, z) in np.argwhere(verschil):
        cellen[(int(x) + OX, int(y) + OY, int(z) + OZ)] = gn[g1[x, y, z]]
    # piles/posts that stop at the design sea floor (scene ZEE - 1): down to the real sea floor (filled per tile later)
    palen = {}
    for (x, y, z) in np.argwhere(zeekol[:, None, :] & (g0 != g1) & (B1.v != 0) & (np.arange(B1.v.shape[1]) == KP.ZEE - 1)[None, :, None]):
        n = n1[B1.v[x, y, z]]
        if "_log" in n or "fence" in n:
            palen[(int(x) + OX, int(z) + OZ)] = (int(y) + OY, n)
    kolommen = {}
    for (x, z) in np.argwhere(land):
        kolommen[(int(x) + OX, int(z) + OZ)] = (int(x), int(z), int(C.I.H[x, z]) + OY)
    borden = {}
    for (x, y, z) in I1.ambacht_borden:
        regels = KP.BORD_AMBACHT
        msgs = [{"translate": f"{KP.BORD_AMBACHT_SLEUTEL}.{i + 1}", "fallback": t} if t else {"text": ""} for i, t in enumerate(regels)]
        kant = {"messages": msgs, "color": "black", "has_glowing_text": False}
        leeg = {"messages": [{"text": ""}] * 4, "color": "black", "has_glowing_text": False}
        borden[(x + KAMP_OX, y + OY, z + KAMP_OZ)] = {"id": "minecraft:sign", "is_waxed": True, "front_text": kant, "back_text": leeg}
    return cellen, kolommen, palen, borden, n1


def steen(x, y, z):
    sy = y - OY
    if sy <= KP.ZEE + 1:
        return "minecraft:mossy_cobblestone" if (x + z + y) % 3 else "minecraft:cobblestone"
    k = (sy + int(2 * math.sin(x / 7.0) + 2 * math.cos(z / 9.0))) // 3
    return STEEN[k % len(STEEN)]


def zeebodem(pal, d, lx, lz):
    """Highest solid (non-water, non-air) cell below the sea surface in this tile column. Always asked of the LIVE (BASIS) tile,
    so a second run finds the same sea floor under an islet column it already filled (idempotent)."""
    for y in range(61, 0, -1):
        n = pal[d[lx, y, lz]]
        if d[lx, y, lz] and not n.startswith(("minecraft:water", "minecraft:air", "minecraft:seagrass", "minecraft:kelp",
                                               "minecraft:tall_seagrass")):
            return y
    return 40


# ------------------------------------------------------------------------------------------------------------ hollowing
_LUT = {}


def opaak(TG, i):
    if i in _LUT:
        return _LUT[i]
    import vibekit as V
    s = TG.pal[i]
    naam = s.split("[")[0]
    v = False
    if i and not (naam in BL.EIGEN or naam.endswith(("_leaves", "barrier")) or "glass" in naam or "slime" in naam or "honey" in naam):
        try:
            v = bool(V.res()[1].bake(s).full_opaque)
        except Exception:
            v = False
    _LUT[i] = v
    return v


def hol(TG, d, mag):
    """tegels.Wereld.hol, but only cells in `mag` (the cells this pass wrote) may be emptied. Returns the emptied mask."""
    lut = np.array([opaak(TG, i) for i in range(len(TG.pal))], bool)
    occ = lut[d]
    inn = occ.copy()
    inn[0, :, :] = inn[-1, :, :] = False
    inn[:, 0, :] = inn[:, -1, :] = False
    inn[:, :, 0] = inn[:, :, -1] = False
    inn[1:-1, 1:-1, 1:-1] &= occ[:-2, 1:-1, 1:-1] & occ[2:, 1:-1, 1:-1] & occ[1:-1, :-2, 1:-1] & \
        occ[1:-1, 2:, 1:-1] & occ[1:-1, 1:-1, :-2] & occ[1:-1, 1:-1, 2:]
    inn &= mag
    d[inn] = 0
    return inn


# ------------------------------------------------------------------------------------------------------------ the tiles
VERBONDEN = ("fence", "_wall", "pane", "iron_bars", "vlaggetjes", "slinger")


def zet(TG, d, lx, y, lz, n):
    """Writes name n; a connecting block (fence, wall, pane) that is already there keeps the connection states the
    connection pass gave it, so a second run changes nothing."""
    if n == "minecraft:air":
        d[lx, y, lz] = 0
        return
    oud = TG.pal[d[lx, y, lz]] if d[lx, y, lz] else ""
    if oud.split("[")[0] == n.split("[")[0] and any(k in n for k in VERBONDEN):
        return
    d[lx, y, lz] = TG.id(n)


def stempel(TG, cellen, kolommen, palen, borden, droog):
    per = {}
    for (x, y, z), n in cellen.items():
        per.setdefault((x // T, z // T), {"cel": [], "kol": [], "paal": []})["cel"].append((x, y, z, n))
    for (wx, wz), v in kolommen.items():
        per.setdefault((wx // T, wz // T), {"cel": [], "kol": [], "paal": []})["kol"].append((wx, wz, v))
    for (wx, wz), v in palen.items():
        per.setdefault((wx // T, wz // T), {"cel": [], "kol": [], "paal": []})["paal"].append((wx, wz, v))
    return per


def schrijf_tegels(TG, B1, n1, per, borden, droog):
    gewijzigd, binnen, stats = {}, {}, {"cellen": 0, "kolommen": 0, "boomcel_anders": 0}
    for t in sorted(per):
        d0 = TG.dicht(t)
        if d0 is None:
            print(f"  tegel {t}: BESTAAT NIET (de zee hoort een tegel te hebben)")
            continue
        d = d0.copy()
        mag = np.zeros(d.shape, bool)
        onder = np.zeros(d.shape, bool)                         # islet column cells at or below the surface (interior if empty)
        tx, tz = t
        pal_b, v_b = basis_tegel(t)
        if v_b is None:                                         # (a tile that is not in the live release: its own floor)
            pal_b, v_b = TG.pal, d0
        for (x, y, z, n) in per[t]["cel"]:
            lx, lz = x - tx * T, z - tz * T
            oud = TG.pal[d[lx, y, lz]]
            if n == "minecraft:air" and d[lx, y, lz] and not any(k in oud for k in ("_log", "_leaves", "grass", "flower", "tulip",
                                                                                      "allium", "daisy", "guhbloem", "guhoortjes",
                                                                                      "cornflower", "fence", "lantern", "sign")):
                stats["boomcel_anders"] += 1          # a cell the pass would clear that holds something unexpected: keep it
                continue
            zet(TG, d, lx, y, lz, n)
            mag[lx, y, lz] = True
            stats["cellen"] += 1
        for (wx, wz, (sx, sz, top)) in per[t]["kol"]:
            lx, lz = wx - tx * T, wz - tz * T
            bodem = zeebodem(pal_b, v_b, lx, lz)
            for y in range(bodem + 1, HOOG):
                sy = y - OY
                n = n1[B1.v[sx, sy, sz]] if 0 <= sy < B1.v.shape[1] else "minecraft:air"
                if sy <= KP.ZEE and n.split("[")[0] in ("minecraft:water", "minecraft:light_blue_terracotta"):
                    n = "minecraft:air"                         # (the design's open sea under the shell: rock, not a sea layer)
                if n == "minecraft:air" and y <= top:
                    n = steen(wx, y, wz)
                zet(TG, d, lx, y, lz, n)
                mag[lx, y, lz] = True
            onder[lx, bodem + 1:top + 1, lz] = True
            stats["kolommen"] += 1
        for (wx, wz, (y0, n)) in per[t]["paal"]:
            lx, lz = wx - tx * T, wz - tz * T
            for y in range(zeebodem(pal_b, v_b, lx, lz) + 1, y0):
                d[lx, y, lz] = TG.id(n)
                mag[lx, y, lz] = True
        hol(TG, d, mag)
        binnen[t] = onder & (d == 0)                            # the hollowed islet interior (stable over reruns)
        if np.array_equal(d, d0):
            continue
        gewijzigd[t] = d
    print(f"  {stats['cellen']} losse cellen, {stats['kolommen']} eilandkolommen, {stats['boomcel_anders']} cellen overgeslagen "
          f"(stonden anders in de tegel dan verwacht)", flush=True)
    if droog:
        return gewijzigd, binnen
    for t, d in gewijzigd.items():
        tx, tz = t
        extra = {k: r for k, r in TG.nbt(t).items() if d[k] and d[k] == TG.dicht(t)[k]}
        for (x, y, z), nb in borden.items():
            if (x // T, z // T) == t:
                extra[(x - tx * T, y, z - tz * T)] = nb
        TG.schrijf(t, d, extra=extra)
    return gewijzigd, binnen


# ------------------------------------------------------------------------------------------------------------ live patches
def basis_tegel(t):
    """The LIVE tile (git BASIS) as a dense array in its own palette, or None."""
    import tempfile
    sys.path.insert(0, os.path.join(VADSRIJK, "audit"))
    import tegellezer as TL
    pad = f"src/main/resources/data/guhs/structure/vadsrijk/tegels/t_{t[0]}_{t[1]}.nbt"
    r = subprocess.run(["git", "show", f"{BASIS}:{pad}"], cwd=ROOT, capture_output=True)
    if r.returncode:
        return None, None
    with tempfile.NamedTemporaryFile(suffix=".nbt", delete=False) as f:
        f.write(r.stdout)
        tmp = f.name
    try:
        pal, (xs, ys, zs, st) = TL.lees(tmp)
    finally:
        os.unlink(tmp)
    v = np.full((T, HOOG, T), -1, np.int32)
    v[xs, ys, zs] = st
    return ["minecraft:air"] + list(pal), v + 1


def nbt_bestand(pad, palet, pos, oud, nieuw, be):
    s, named = nbtsnel._str, nbtsnel._named
    body = b"\x0a" + s("")
    body += named(3, "versie") + struct.pack(">i", 1)
    body += named(9, "palet") + bytes([8]) + struct.pack(">i", len(palet)) + b"".join(s(p) for p in palet)
    for naam, arr in (("pos", pos), ("oud", oud), ("nieuw", nieuw)):
        body += named(11, naam) + struct.pack(">i", len(arr)) + np.asarray(arr, ">i4").tobytes()
    body += named(9, "nbt") + bytes([10]) + struct.pack(">i", len(be))
    for (x, y, z), nb in be:
        body += named(11, "pos") + struct.pack(">i", 3) + struct.pack(">iii", x, y, z) + named(10, "nbt") + nbtsnel._nbt_lijf(nb) + b"\x00" + b"\x00"
    body += b"\x00"
    with open(pad, "wb") as f:
        f.write(gzip.compress(body, compresslevel=9, mtime=0))


PATCH_INFO = {
    "ambachtseiland": ("zee", "het Ambachtseiland met zijn brug, ten westen van het Knuffelkamp",
                       "1.10.4: the Ambachtseiland + its bridge (GENERATED by tools/vadsrijk/kamp/ambacht_kaart.py: the cell differences "
                       "between the live tiles and the new ones). Beleid 'zee': a cell is only changed when it still holds the "
                       "generated block (the 'oud' state), or air/water/plants; never a block entity."),
    "kamp_pad": ("kamp_pad", "het pad van het kampvuur in het Knuffelkamp naar de brug (lantaarns, bordjes, bomen weg)",
                 "1.10.4: the Knuffelkamp path to the Ambachtseiland bridge (GENERATED by tools/vadsrijk/kamp/ambacht_kaart.py). "
                 "Beleid 'kamp_pad': trees (logs/leaves where the generator put a tree), grass/flowers and the generated ground may "
                 "be replaced; anything else only when it still holds the generated block; never a block entity."),
}


def patches(TG, tegels, binnen, borden, droog):
    groepen = {"ambachtseiland": [], "kamp_pad": []}
    for t in tegels:
        pal0, v0 = basis_tegel(t)
        d = TG.dicht(t)                                         # (after the passes: re-read from disk)
        if v0 is None:
            print(f"  tegel {t}: niet in {BASIS}")
            continue
        n0 = np.array(pal0, object)[v0]
        n1 = np.array(TG.pal, object)[d]
        inn = binnen.get(t)
        anders = n0 != n1
        if inn is not None:
            anders &= ~inn                                      # hollowed islet interior: stays as it is live (sealed water/air)
        tx, tz = t
        for (lx, y, lz) in np.argwhere(anders):
            x, z = tx * T + int(lx), tz * T + int(lz)
            groepen["ambachtseiland" if x <= SPLITS_X else "kamp_pad"].append((x, int(y), z, n0[lx, y, lz], n1[lx, y, lz]))
    uit = {}
    for naam, cel in groepen.items():
        cel.sort(key=lambda c: (c[0] >> 4, c[2] >> 4, c[1], c[0], c[2]))   # chunk by chunk, bottom up
        palet = {}
        pos, oud, nieuw = [], [], []
        for (x, y, z, a, b) in cel:
            pos += [x, y, z]
            oud.append(palet.setdefault(a, len(palet)))
            nieuw.append(palet.setdefault(b, len(palet)))
        be = sorted((p, nb) for p, nb in borden.items() if (p[0] <= SPLITS_X) == (naam == "ambachtseiland"))
        chunks = {(c[0] >> 4, c[2] >> 4) for c in cel}
        xs = [c[0] for c in cel] or [0]
        zs = [c[2] for c in cel] or [0]
        uit[naam] = (len(cel), len(chunks), (min(xs), min(zs), max(xs), max(zs)))
        print(f"  patch {naam}: {len(cel)} cellen in {len(chunks)} chunks, doos x {min(xs)}..{max(xs)} z {min(zs)}..{max(zs)}, "
              f"palet {len(palet)}, {len(be)} bordjes", flush=True)
        if droog:
            continue
        beleid, beschrijving, uitleg = PATCH_INFO[naam]
        nbt_bestand(os.path.join(PATCHES, naam + ".stempel.nbt"), list(palet), pos, oud, nieuw, be)
        js = {"_uitleg": uitleg, "soort": "stempel", "dimensie": "guhs:vadsrijk", "beschrijving": beschrijving,
              "gebied": "ambachtseiland" if naam == "ambachtseiland" else "knuffelkamp", "beleid": beleid,
              "stempel": naam + ".stempel.nbt", "cellen": len(cel)}
        with open(os.path.join(PATCHES, naam + ".json"), "w", encoding="utf-8", newline="\n") as f:
            f.write(json.dumps(js, indent=2, ensure_ascii=False) + "\n")
    return uit


# ------------------------------------------------------------------------------------------------------------ POIs
def plekken(TG, B1, n1, I1, C):
    """POIs of the islet (stand positions, world coords), for maak_kaart.java_plekken (WereldPlekken / POI_VADSRIJK.md)."""
    def w(x, y, z):
        return [int(round(x)) + OX, int(round(y)) + OY, int(round(z)) + OZ]

    def staan(sx, sz):
        """Stand y (scene) on the column: the first walkable air above the highest solid block (not plants/carpet)."""
        col = B1.v[sx, :, sz]
        for y in range(len(col) - 1, -1, -1):
            n = n1[col[y]]
            if n != "minecraft:air" and not any(k in n for k in ("grass[", "short_grass", "tulip", "allium", "daisy", "guhbloem",
                                                                "guhoortjes", "cornflower", "carpet", "_bud", "fern", "bush",
                                                                "azalea", "peony", "rose", "lilac", "bluet", "dandelion",
                                                                "knabbelkoor", "lily_pad", "waterlelie", "riet", "cluster")):
                return y + 1
        return KP.VLOER + 1

    p = {}
    px, pz = C.s(*A.PLEIN)
    p["ambacht_plein"] = w(px, KP.VLOER + 1, pz + 6)
    kx, ky, kz = C.knutsel
    p["ambacht_knutsel"] = w(kx, ky, kz)
    for naam, (voor, deur, binnen) in C.hutten.items():
        p["ambacht_" + naam.lower()] = {"voor": w(*voor), "deur": w(*deur), "binnen": w(*binnen)}
    zones = {"Kiezelgroeve": "groeve", "Knuffelbosje": "bosje", "Bloemenwei": "bloemenwei", "Bijenhofje": "bijenhofje",
             "Visvijver": "visvijver"}
    npc_zone = {"guh_npc_mijnwerkerguh": "groeve", "guh_npc_timmerguh": "bosje", "guh_npc_boerinneguh": "bloemenwei",
                "guh_npc_verzorgerguh": "bijenhofje", "guh_npc_veerguh": "visvijver", "guh_npc_kapiteinguh": "vissteiger"}
    for zn, kort in zones.items():
        x1, z1, x2, z2 = C.zone[zn]
        mx, mz = (x1 + x2) // 2, (z1 + z2) // 2
        if kort == "visvijver":                     # (the middle is water: the stand spot on the little jetty)
            vx, vz = C.s(*A.VIJVER)
            mx, mz = vx - 4, vz - A.VIJVER_R[1] - 1
        p["ambacht_" + kort] = {"midden": w(mx, staan(mx, mz), mz), "min": w(x1, KP.VLOER - 14, z1), "max": w(x2, KP.VLOER + 14, z2)}
    sx, sz, se = C.steiger
    p["ambacht_vissteiger"] = {"midden": w(se - 2, KP.ZEE + 3, sz), "begin": w(sx, staan(sx, sz), sz)}
    for (x, y, z, yaw, soort) in C.npcs:
        if soort in npc_zone:
            p["ambacht_" + npc_zone[soort]]["npc"] = w(math.floor(x), math.ceil(y), math.floor(z))   # (y + 0.5 = on a slab)
    xi, xk, yd = C.brug_eind
    _, bz = C.s(0, A.BRUG_Z)
    p["ambacht_brug_eiland"] = w(xi - 5, staan(xi - 5, bz), bz)
    p["ambacht_brug_kamp"] = w(xk + 2, staan(xk + 2, bz), bz)
    p["ambacht_brug_midden"] = w((xi + xk) // 2, yd + 2, bz)
    with open(PLEKKEN, "w", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(p, indent=1) + "\n")
    print(f"  {PLEKKEN}: {len(p)} POI-groepen")
    return p


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--droog", action="store_true")
    ap.add_argument("--map", default=VB.UIT)
    ap.add_argument("--patch", action="store_true", help="(re)write the live patch files (only while BASIS is the live release)")
    a = ap.parse_args(argv)
    t0 = time.time()
    B0, B1, I1, kH, C = scenes()
    cellen, kolommen, palen, borden, n1 = wijzigingen(B0, B1, I1, kH, C)
    print(f"wijzigingen: {len(cellen)} cellen, {len(kolommen)} eilandkolommen, {len(palen)} palen, {len(borden)} bordjes", flush=True)
    TG = VB._Tegels(a.map)
    per = stempel(TG, cellen, kolommen, palen, borden, a.droog)
    print(f"tegels geraakt: {len(per)}: {sorted(per)}", flush=True)
    gewijzigd, binnen = schrijf_tegels(TG, B1, n1, per, borden, a.droog)
    print(f"tegels herschreven: {len(gewijzigd)}: {sorted(gewijzigd)} ({time.time() - t0:.0f} s)", flush=True)
    if not a.droog:
        plekken(TG, B1, n1, I1, C)
    if gewijzigd and not a.droog and os.environ.get("GUHS_VERBIND_LATER") != "1":
        lijst = ";".join(f"{x},{z}" for x, z in sorted(gewijzigd))
        subprocess.run([sys.executable, os.path.join(VADSRIJK, "verbind.py"), "--map", a.map, "--werkers", "1", "--alleen=" + lijst],
                       cwd=ROOT, check=True)
        subprocess.run([sys.executable, os.path.join(VADSRIJK, "vrijpad.py"), "--alleen=" + lijst], cwd=ROOT, check=True)
        subprocess.run([sys.executable, os.path.join(VADSRIJK, "verbind.py"), "--map", a.map, "--werkers", "1", "--alleen=" + lijst],
                       cwd=ROOT, check=True)
    if a.patch or a.droog:
        TG2 = VB._Tegels(a.map)
        patches(TG2, sorted(per), binnen, borden, a.droog)
    print(f"klaar ({time.time() - t0:.0f} s)")


if __name__ == "__main__":
    main()
