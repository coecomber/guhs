"""Export van Hoogvads naar SPELcoördinaten: blokkenlijsten per 64x320x64-tegel, voor de kaartbouwer (Vadsrijk-tegels).

Plaatsing (WERELDINDELING.md, zone 5): de walvis hangt boven het oosteinde van zone 4, met de KOP NAAR HET WESTEN.
  generator (lokaal): kop = +x, as z = 0, y = spel-y
  spel:               gx = GX0 - x,  gz = GZ0 - z,  gy = y        (180 graden gedraaid, geen spiegeling)
  GX0 = 2702 -> neus op x ~2440 (boven de Opstijgpier); staartvin tot x ~3015; GZ0 = -520 (as op z -520)
  y: buik ~154, rug ~240, kathedraalspitsen tot ~317. (Het plan zei y 170-320; de buik zakt iets lager.)

Gebruik (pure functies, deterministisch: dezelfde seed geeft dezelfde stad):
  import export as EX
  W = EX.wereld("nieuw")                    # of "guhs" (alleen bestaande blokken, zonder de hv_-conceptblokken)
  for (gx, gy, gz, state) in EX.alle_blokken(W): ...          # alle niet-lucht blokken, spelcoördinaten
  vol, palet = EX.tegel(W, tx, tz)          # uint16 (64, 320, 64) [x, y, z] in de tegel; palet[0] = lucht
  EX.tegels_van(W)                          # lijst (tx, tz) met inhoud
CLI:
  python export.py [nieuw|guhs] [--hol] [--uit DIR]
    -> DIR/t_<tx>_<tz>.npz (xs, ys, zs, ids: int32; palet: str-array; coördinaten binnen de tegel)
    -> DIR/index.json (tegels, plaatsing, aantallen), DIR/figuren.json (guhs/NPC's/ridders voor spawns),
       DIR/plekken.json (POI's in spelcoördinaten)
--hol haalt blokken weg die aan alle 6 kanten door volle blokken zijn ingesloten (zoals de tegelbouwer van zone 3).
"""
import os, sys, json, time
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

GX0, GZ0 = 2702, -520
T, HOOG = 64, 320
NS = {"north": "south", "south": "north", "east": "west", "west": "east"}


def draai180(s):
    """Blockstate 180 graden om de y-as draaien (facing, verbindingen, rotation)."""
    if "[" not in s:
        return s
    naam, _, rest = s.partition("[")
    props = []
    kv = {}
    for p in rest.rstrip("]").split(","):
        if "=" in p:
            k, v = p.split("=", 1)
            kv[k] = v
            props.append(k)
    uit = dict(kv)
    for k in ("facing", "horizontal_facing"):
        if kv.get(k) in NS:
            uit[k] = NS[kv[k]]
    for a, b in (("north", "south"), ("east", "west")):
        if a in kv and b in kv:
            uit[a], uit[b] = kv[b], kv[a]
    if kv.get("rotation", "").isdigit():
        uit["rotation"] = str((int(kv["rotation"]) + 8) % 16)
    return naam + "[" + ",".join(f"{k}={uit[k]}" for k in props) + "]"


def wereld(modus="nieuw", cache=True):
    """De lokale Wereld (bouwt hem zo nodig; anders uit _cache/)."""
    import wereld as WR
    p = os.path.join(WR.CACHE, modus + "_v.npy")
    if cache and os.path.exists(p):
        return WR.laad(modus)
    W, S = WR.bouw(modus)
    WR.bewaar(W, modus)
    return W


def _spel_palet(W):
    import vibekit as V
    W.fix_connections() if hasattr(W, "fix_connections") else None
    pal = [("minecraft:air" if s.split("[")[0] in ("minecraft:air", "minecraft:cave_air") else draai180(s))
           for s in W.pal]
    return pal


def naar_spel(x, y, z):
    return GX0 - x, y, GZ0 - z


def naar_lokaal(gx, gy, gz):
    return GX0 - gx, gy, GZ0 - gz


def tegels_van(W):
    nz = np.argwhere((W.v != 0).any(axis=1))
    x1, z1 = W.b[0], W.b[2]
    xs = x1 + nz[:, 0]
    zs = z1 + nz[:, 1]
    gx = GX0 - xs
    gz = GZ0 - zs
    return sorted(set(zip([int(a) for a in (gx // T)], [int(b) for b in (gz // T)])))


def tegel(W, tx, tz, pal=None, hol=False):
    """(vol, palet) van één tegel in spelcoördinaten; vol[x, y, z] met x/z 0..63 binnen de tegel, y = spel-y."""
    pal = pal or _spel_palet(W)
    vol = np.zeros((T, HOOG, T), np.uint16)
    x1, y1, z1 = W.b[:3]
    X, Y, Z = W.v.shape
    gx = tx * T + np.arange(T)
    gz = tz * T + np.arange(T)
    lx = (GX0 - gx) - x1       # lokale voxel-index (aflopend)
    lz = (GZ0 - gz) - z1
    okx = (lx >= 0) & (lx < X)
    okz = (lz >= 0) & (lz < Z)
    if not okx.any() or not okz.any():
        return vol, pal
    ys = np.arange(Y) + int(y1)
    oky = (ys >= 0) & (ys < HOOG)
    sub = W.v[np.ix_(lx[okx], np.nonzero(oky)[0], lz[okz])]
    ix = np.nonzero(okx)[0]
    iz = np.nonzero(okz)[0]
    vol[np.ix_(ix, ys[oky], iz)] = sub
    if hol:
        vol = _hol(vol, pal)
    return vol, pal


def _hol(vol, pal):
    import vibekit as V
    r, bk = V.res()
    lut = np.array([s != "minecraft:air" and bk.bake(s).full_opaque for s in pal], bool)
    occ = lut[vol]
    inn = occ.copy()
    inn[0, :, :] = inn[-1, :, :] = False
    inn[:, 0, :] = inn[:, -1, :] = False
    inn[:, :, 0] = inn[:, :, -1] = False
    inn[1:-1, 1:-1, 1:-1] &= occ[:-2, 1:-1, 1:-1] & occ[2:, 1:-1, 1:-1] & occ[1:-1, :-2, 1:-1] & \
        occ[1:-1, 2:, 1:-1] & occ[1:-1, 1:-1, :-2] & occ[1:-1, 1:-1, 2:]
    vol = vol.copy()
    vol[inn] = 0
    return vol


def alle_blokken(W, pal=None):
    """Generator: (gx, gy, gz, state) voor elk niet-lucht blok, in spelcoördinaten."""
    pal = pal or _spel_palet(W)
    x1, y1, z1 = W.b[:3]
    for (i, j, k) in np.argwhere(W.v != 0):
        s = pal[W.v[i, j, k]]
        if s == "minecraft:air":
            continue
        yield (GX0 - (x1 + i), int(y1 + j), GZ0 - (z1 + k), s)


def figuren(W):
    """Guh-figuurtjes (render-only) als spawnlijst in spelcoördinaten (W-LEVEN: yaw gespiegeld, zie bouw5.spel_yaw)."""
    out = []
    x1, y1, z1 = W.b[:3]
    for (x, y, z, yaw, skin, zit) in W.guhs:
        gx, gy, gz = naar_spel(x / W.k + x1, y / W.k + y1, z / W.k + z1)
        out.append(dict(x=round(gx, 2), y=round(gy, 2), z=round(gz, 2), yaw=(360 - yaw) % 360, skin=skin, zit=bool(zit)))
    return out


def plekken(W):
    import stad as ST
    import kathedraal as KA
    P = {}
    for naam, (xa, xb, za, zb, niv) in ST.ZONES.items():
        cx, cz = (xa + xb) / 2, (za + zb) / 2
        gx, gy, gz = naar_spel(cx, niv or 0, cz)
        P["zone_" + naam] = dict(x=int(gx), y=None if niv is None else int(niv), z=int(gz), vak=[int(GX0 - xb), int(GZ0 - zb), int(GX0 - xa),
                                                                   int(GZ0 - za)])
    for naam, (x, y, z) in (("kathedraal_poort", (KA.X0 - 3, KA.Y0 + 1, 0)),
                            ("kathedraal_altaar", ((KA.X_KRUIS[0] + KA.X_KRUIS[1]) / 2, KA.Y0 + 4, 0))):
        gx, gy, gz = naar_spel(x, y, z)
        P[naam] = dict(x=int(gx), y=int(gy), z=int(gz))
    for k in ("balkon_oog",):
        if getattr(W, k, None):
            gx, gy, gz = naar_spel(*W.__dict__[k])
            P[k] = dict(x=int(gx), y=int(gy), z=int(gz))
    return P


def main():
    args = sys.argv[1:]
    modus = "guhs" if "guhs" in args else ("vanilla" if "vanilla" in args else "nieuw")
    uit = os.path.join(HERE, "_export_" + modus)
    if "--uit" in args:
        uit = args[args.index("--uit") + 1]
    hol = "--hol" in args
    os.makedirs(uit, exist_ok=True)
    t = time.time()
    W = wereld(modus)
    pal = _spel_palet(W)
    lijst = tegels_van(W)
    index = dict(plaatsing=dict(GX0=GX0, GZ0=GZ0, formule="gx = GX0 - x, gz = GZ0 - z, gy = y (180 graden)"),
                 modus=modus, hol=hol, tegels=[])
    for (tx, tz) in lijst:
        vol, _ = tegel(W, tx, tz, pal, hol)
        xs, ys, zs = np.nonzero(vol)
        if not len(xs):
            continue
        ids = vol[xs, ys, zs]
        geb, inv = np.unique(ids, return_inverse=True)
        np.savez_compressed(os.path.join(uit, f"t_{tx}_{tz}.npz"), xs=xs.astype(np.int32), ys=ys.astype(np.int32),
                            zs=zs.astype(np.int32), ids=inv.astype(np.int32),
                            palet=np.array([pal[i] for i in geb]))
        index["tegels"].append(dict(tx=int(tx), tz=int(tz), blokken=int(len(xs))))
        print("tegel", tx, tz, len(xs), flush=True)
    with open(os.path.join(uit, "index.json"), "w", encoding="utf-8") as fh:
        json.dump(index, fh, indent=1, default=lambda o: o.item())
    with open(os.path.join(uit, "figuren.json"), "w", encoding="utf-8") as fh:
        json.dump(figuren(W), fh, default=lambda o: o.item())
    with open(os.path.join(uit, "plekken.json"), "w", encoding="utf-8") as fh:
        json.dump(plekken(W), fh, indent=1, default=lambda o: o.item())
    print("klaar:", len(index["tegels"]), "tegels in", uit, "%.0fs" % (time.time() - t))


if __name__ == "__main__":
    main()
