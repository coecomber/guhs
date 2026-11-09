"""Zone 3 "De Schemerwortel" (REVAMP, round 20): every fixed coordinate of the zone-3 map (the single source for the builder,
POI_Z3.md and Z3WereldPlekken.java).

The map is the approved revamp design (renders-schemerwortel2/_bron, copied to kit/: boom2, plekken2, werf2, schemerkit) with
the user's adjustments (narrower crown 130/95/60, one inner stair floor -> nest with half steps, a broad riding spiral with half
steps, ...). Coordinates in the kit are RELATIVE TO THE TRUNK AXIS ("rel": x, y, z), NOT mirrored:
    world X = 2300 + floor(x),  world Z = floor(z),  y = y.
Zone box x 1900..2700, z -400..400 (800 x 800); tiles tx 29..42, tz -7..6 (196 tiles). All "stand" points are the air block a
player/NPC stands in. Ids are kept from v1 wherever the place still exists (see NOTITIES_P_WERELD.md for the mapping).
"""
import math
import os
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HIER, "kit"))
import boom2 as B  # noqa: E402
import plekken2 as P  # noqa: E402
import werf2 as WF  # noqa: E402

# [wereldindeling 2026-11-05] the connected world: zone 3 moved by (-896, -576). THE one offset: world = rel + (TX_STAM, TZ_STAM).
TX_STAM, TZ_STAM = 1404, -576
X0, X1, Z0, Z1 = TX_STAM - 380, TX_STAM + 323, TZ_STAM - 448, TZ_STAM + 447   # the zone box (x 1024..1727, z -1024..-129)
TX0, TX1, TZ0, TZ1 = 16, 26, -16, -3           # the tile grid (64x320x64 tiles), x 1024..1727, z -1024..-129
# earlier zone-3 grids (v1: trunk 2300,0, 484 tiles; v2 revamp at the old place): their tiles outside the new grid are deleted
OUDE_RASTERS = [(25, 46, -11, 10), (29, 42, -7, 6)]
HY = B.HONING_Y


def W(x, y, z):
    """rel point -> world block [X, Y, Z]."""
    return [TX_STAM + int(math.floor(x)), int(math.floor(y)), TZ_STAM + int(math.floor(z))]


def rel_x(X):
    return X - TX_STAM


def rel_z(Z):
    return Z - TZ_STAM


def pol(a, r):
    return r * math.cos(a), r * math.sin(a)


def grond(x, z):
    return float(B.grondhoogte(np.array([float(x)]), np.array([float(z)]))[0])


def gstand(x, z):
    """Stand y on the ground (top block = floor(g))."""
    return int(math.floor(grond(x, z))) + 1


def kstand(s, x, z):
    """Stand y on crown storey s."""
    return int(math.floor(B.kruin_top(s, x, z))) + 1


def tak_punt(tak, s_, zij=0.0):
    """(x, stand y, z) on top of big branch 'tak' at distance s_ from the trunk, moved sideways by zij * radius."""
    L, rr = B.tak_van(tak)
    d = np.hypot(L[:, 0], L[:, 2])
    i = max(1, min(len(L) - 2, int(np.argmin(np.abs(d - s_)))))
    dv = L[i + 1] - L[i - 1]
    n = np.array([-dv[2], 0.0, dv[0]])
    n /= np.linalg.norm(n) + 1e-9
    c = L[i] + n * zij * rr[i]
    y = L[i, 1] + math.sqrt(max(0.0, 1 - zij * zij)) * rr[i] * 0.7
    return float(c[0]), int(math.floor(y)) + 1, float(c[2])


def htak_punt(tak, s_):
    """(x, stand, z) on a Honingtak boardwalk (level HY)."""
    L, rr = P.htak_lijn(tak)
    d = np.hypot(L[:, 0], L[:, 2])
    i = int(np.argmin(np.abs(d - s_)))
    return float(L[i, 0]), HY + 1, float(L[i, 2])


def trap_punt(lev, r=20.0):
    """(x, stand, z) on the inner stair at level lev (continuous)."""
    a = B.trap_hoek(lev)
    return r * math.cos(a), float(B.stand(lev)), r * math.sin(a)


def rij_punt_lev(k, th, frac=0.5):
    """(x, stand, z) on the riding spiral turn k at angle th, frac across the width (0 inner .. 1 outer)."""
    lev = B.rij_lev(th, k)
    Rr = float(B.r_stam(lev, np.array([th - lev / 160.0]))[0])
    r = Rr - 2.5 + 0.5 + (B.RIJ["breed"] - 1.5) * frac
    return r * math.cos(th), float(B.stand(lev)), r * math.sin(th)


# ================================================================================================ fixed places
RIJ_START_TH = B.RIJ["th0"]
BALKONS = ((0, 200), (1, 30), (2, 250))      # riding-spiral balconies (turn, deg)
WORTELWEG_R = 172                    # the ground ring path
NEST_STAND = B.NEST_Y + 1            # nest floor (top block NEST_Y)
PELSZAAL_VLOER = 60                  # the Pelszaal under the Schorsstad floor: floor block y 60 (stand 61), r 16
PELSZAAL_R = 16
KALE_KRUIN = pol(math.radians(162), 152)
KALE_R = 23
KALE_Y = 226                         # stand on the dead mesa
MOSVOET = pol(math.radians(36), 158)
PLUIMSTAL = pol(RIJ_START_TH - 0.45, 104)    # the stable near the foot of the riding spiral
VIJVERS = [(1.05, 108, 8), (1.95, 104, 7), (3.5, 110, 8), (4.85, 106, 7), (5.75, 110, 7)]   # honey ponds on kruin 1
KRAMEN = [(i, kant, B.TH_M + da, r) for i, da in enumerate((-0.42, -0.21, 0.0, 0.21, 0.42))
          for kant, r in (("n", 45.5), ("z", 64.0))]
# Hart van de Boom: the dungeon ring round the Wortelduister hall (floor block y 28, stand 29), r 112
HART_VLOER = 28
HART_R = 112
HART_KAMERS = [("schimmelkwekerij", 152, 15), ("ringenzaal", 120, 15), ("wortelwurger", 86, 20),
               ("nestwachter", 46, 20), ("baron_bleu", 6, 22)]
HART_POORT = pol(math.radians(180), 80)      # the gate in the west wall of the hall
WERF_C = WF.C


def ring_functies():
    """The Honingtak buildings: ring houses then branch houses, by function."""
    fn = ["smid", "juwelier", "drankjes", "uitrusting", "algemeen", "bank", "jagershut", "burgemeester",
          "woning_1", "woning_2", "woning_3", "woning_4", "woning_5", "woning_6", "woning_7", "woning_8", "woning_9",
          "woning_10", "woning_11"]
    huizen = []
    ring = sorted(P.ring_huizen(), key=lambda h: B.wrap(h[1] - B.TH_M) % (2 * math.pi))
    for (k, a, cx, cz, soort) in ring:
        R = [5.2, 4.8, 3.5][soort]
        huizen.append((cx, cz, a + math.pi, R, "ring", soort))
    for (naam, j, cx, cz, deur, b, soort) in P.tak_huizen():
        huizen.append((cx, cz, deur, [5.0, 4.6][soort], naam, soort))
    return list(zip(fn, huizen))


def steiger(naam):
    """((x, z) of the mast at the tip of a pier branch, (ux, uz) outward)."""
    L, rr = P.htak_lijn(naam)
    tip = L[-1]
    ua = math.atan2(tip[2], tip[0])
    ux, uz = math.cos(ua), math.sin(ua)
    return (tip[0] - ux * 4, tip[2] - uz * 4), (ux, uz)


def steiger_tip(naam):
    """W-SCHEPEN: ((x, z) of the open tip cell of the round pier platform (r 7.5 round the mast) on the cardinal side that faces
    out, (ax, az) that cardinal). The airship docks flush beyond it (Lijnen.Haven: richting 90 = west, 270 = east). A cell is
    on the deck when its centre lies within 7.5 of the mast (schemerkit.cilinder)."""
    (mx, mz), (ux, uz) = steiger(naam)
    if abs(ux) >= abs(uz):
        rij = math.floor(mz)
        bereik = math.sqrt(7.5 ** 2 - (rij + 0.5 - mz) ** 2)
        tx = math.floor(mx - 0.5 + bereik) if ux > 0 else math.ceil(mx - 0.5 - bereik)
        return (tx, rij), (1 if ux > 0 else -1, 0)
    kol = math.floor(mx)
    bereik = math.sqrt(7.5 ** 2 - (kol + 0.5 - mx) ** 2)
    tz = math.floor(mz - 0.5 + bereik) if uz > 0 else math.ceil(mz - 0.5 - bereik)
    return (kol, tz), (0, 1 if uz > 0 else -1)


def windkolommen():
    """(id, x, z, y0 = stand at the foot, y1 = stand on the landing, radius, landing x, landing z), rel coords."""
    out = []
    for (wid, x, z, y0, y1, naam) in P.windkolommen_plan():
        if wid == "honingtak":
            lx, lz = pol(math.atan2(z, x), 63)
        else:
            a = math.atan2(z, x)
            lx, lz = x - 7 * math.cos(a), z - 7 * math.sin(a)
        out.append((wid, x, z, y0 + 1, y1 + 1, 3.0, lx, lz))
    return out


def plekken():
    """All POIs (world coordinates) as a nested dict; ids are flattened with '_' / '.'."""
    PP = {}

    def put(k, x, y, z):
        PP[k] = W(x, y, z)

    # ---------------------------------------------------------------- the airship piers on two long branch tips (Honingtak level)
    #   Weststeiger = the dock (dok_*, line B from zone 2's Galmdok), Oosttaksteiger (line C to Lampionhaven, zone 4)
    for naam, pre in (("Weststeiger", "dok"), ("Oosttaksteiger", "oosttaksteiger")):
        (mx, mz), (ux, uz) = steiger(naam)
        (tx, tz), (ax, az) = steiger_tip(naam)
        # W-SCHEPEN: the mast POI is a stand spot at the foot of the mast (land side; it was inside the mast log: NPCs and the
        # quest texts send you "to the mast")
        put(f"{pre}_mast", math.floor(mx) - ax * 2, HY + 1, math.floor(mz) - az * 2)
        put(f"{pre}_landing", mx - ux * 8, HY + 1, mz - uz * 8)
        put(f"{pre}_bel", mx - ux * 4 - uz * 2, HY + 1, mz - uz * 4 + ux * 2)
        put(f"{pre}_kapitein", mx - ux * 6 + uz * 2, HY + 1, mz - uz * 6 - ux * 2)
        put(f"{pre}_instap", tx, HY + 1, tz)   # W-SCHEPEN: the open tip where the airship lies flush
        put(f"{pre}_luchtschip", tx + ax * 10, HY - 1, tz + az * 10)
        # U-OVERGANG: the viewpoints at the two piers are gone (feedback ronde 4: "ze liggen allemaal bij de ballonpunten");
        # zone 3's viewpoints are the Kruinnest (top of the tree), the Kroon, the Takpunt and the Wortelgewelf
    put("uitkijk_nest", 0, NEST_STAND, 17)
    # T-WERELD: the shared harbour ids of the world layout (t-luchtschip): line B (Galmdok <-> Weststeiger), line C
    # (Oosttaksteiger <-> Lampionhaven); the bell stands where the departure board goes
    for pre, hid in (("dok", "haven_z3_z2"), ("oosttaksteiger", "haven_z3_z4")):
        PP[hid + "_vertrek"] = list(PP[pre + "_instap"])   # W-SCHEPEN: the tip (was the landing, 8 inland: the ship lay on the mast)
        PP[hid + "_bord"] = list(PP[pre + "_bel"])
        PP[hid + "_mast"] = list(PP[pre + "_mast"])
    # ---------------------------------------------------------------- Honingtak: the Honingplein + the ring
    cx, cz = pol(B.TH_M, 52)
    put("knuffelbeeld", cx, HY + 1, cz)
    put("plein", cx - 8, HY + 1, cz - 1)
    put("respawn", cx + 8, HY + 1, cz - 1)
    put("npc_raatje_plein", cx + 5, HY + 1, cz + 5)
    put("questbord", cx - 6, HY + 1, cz + 6)
    put("markt", cx, HY + 1, cz + 8)
    for (i, kant, a, r) in KRAMEN:
        x, z = pol(a, r + (2.2 if kant == "n" else -2.2))
        put(f"npc_kraam_{i + 1}{kant}", x, HY + 1, z)
    x, z = pol(B.TH_M + 0.62, 50)
    put("bloesemboompje", x, HY + 1, z)
    gebouw = {}
    for fn, (hx, hz, deur, R, waar, soort) in ring_functies():
        dx, dz = math.cos(deur), math.sin(deur)
        nd = 2.2 if soort != 2 else 1.0
        gebouw[fn] = {"npc": W(hx - dx * nd, HY + 1, hz - dz * nd), "binnen": W(hx + dx * 1.6, HY + 1, hz + dz * 1.6),
                      "deur": W(hx + dx * (R + 1.8), HY + 1, hz + dz * (R + 1.8)),
                      "doos_min": W(hx - 7, HY, hz - 7), "doos_max": W(hx + 7, HY + 12, hz + 7)}
        if fn == "jagershut":       # the boards stand against the side walls (the hunter behind his counter stays free)
            gebouw[fn]["premiebord"] = W(hx + dx * 0.5 - dz * 3.4, HY + 1, hz + dz * 0.5 + dx * 3.4)
            gebouw[fn]["jachtbord"] = W(hx + dx * 0.5 + dz * 3.4, HY + 1, hz + dz * 0.5 - dx * 3.4)
    for naam, (ad, Rc, hc) in P.CELLEN.items():
        a = math.radians(ad)
        ccx, ccz = pol(a, 55)
        yb = HY - 2 - hc                    # cell floor block (honeycomb) -> stand yb + 1
        ux_, uz_ = math.cos(a), math.sin(a)
        g = {"binnen": W(ccx, yb + 1, ccz), "npc": W(ccx - ux_ * 3, yb + 1, ccz - uz_ * 3),
             "deur": W(ccx + ux_ * (Rc - 1.5), yb + 1, ccz + uz_ * (Rc - 1.5))}
        if naam == "cafe":
            g["podium"] = W(ccx - ux_ * 5 + uz_ * 3, yb + 1, ccz - uz_ * 5 - ux_ * 3)
        if naam == "nestkapel":
            g["eiersokkel"] = W(ccx - ux_ * 6, yb + 1, ccz - uz_ * 6)
            g["droomsteen"] = W(ccx - ux_ * 2 + uz_ * 5, yb + 1, ccz - uz_ * 2 - ux_ * 5)
            g["advancement"] = W(ccx - ux_ * 2 - uz_ * 5, yb + 1, ccz - uz_ * 2 + ux_ * 5)
        gebouw[naam] = g
        lid = {"cafe": "cafe", "nestkapel": "kapel", "raattros": "raattros"}[naam]
        rs = 55 + Rc + 3.5                   # the lift shaft just outside the cell, a hole in the deck
        put(f"lift_{lid}_boven", (rs - 3) * ux_, HY + 1, (rs - 3) * uz_)
        put(f"lift_{lid}_onder", (55 + Rc - 2.5) * ux_, yb + 1, (55 + Rc - 2.5) * uz_)
        if naam == "raattros":
            put("mika_imker_luik", ccx - uz_ * 2, yb + 1, ccz + ux_ * 2)
            put("mika_imker_raatzaal", ccx + uz_ * 6, yb - 11, ccz - ux_ * 6)   # T-IMKER: the boss room, ladder at its south edge
        if naam == "nestkapel":
            put("flash_f2_kijkpunt", ccx - ux_ * 1, yb + 1, ccz - uz_ * 1)
    # the Pluimstal (ground, near the foot of the riding spiral)
    sx, sz = PLUIMSTAL
    sa = math.atan2(sz, sx)
    ox, oz = math.cos(sa), math.sin(sa)       # outward
    tx_, tz_ = -oz, ox                        # along the building
    gs = gstand(sx, sz)
    gebouw["pluimstal"] = {"npc": W(sx - ox * 5, gs, sz - oz * 5), "binnen": W(sx, gs, sz),
                           "deur": W(sx - ox * 9, gs, sz - oz * 9), "voer": W(sx + tx_ * 7, gs, sz + tz_ * 7)}
    PP["gebouw"] = gebouw
    kx, kz = sx + ox * 16, sz + oz * 16
    put("pluimstal_keuze", kx, gstand(kx, kz), kz)
    for i, d in enumerate((-6, 0, 6)):
        put(f"pluimstal_keuze_pluimguh_{i + 1}", kx + ox * 6 + tx_ * d, gstand(kx + ox * 6 + tx_ * d, kz + oz * 6 + tz_ * d),
            kz + oz * 6 + tz_ * d)
    ex, ez = sx + ox * 36 + tx_ * 20, sz + oz * 36 + tz_ * 20
    put("pluimstal_oefenbaan", ex, gstand(ex, ez), ez)
    x, y, z = rij_punt_lev(0, RIJ_START_TH + 0.25, 0.5)
    put("pluimstal_kruinring_start", x, y, z)
    put("rijpad_start", x, y, z)
    put("spiraal_voet", x, y, z)
    put("tijdrit_start", x + ox * 2, y, z + oz * 2)
    # ---------------------------------------------------------------- lifts
    lx, lz, y0, y1, aan = P.lift_a()
    put("honinglift_boven", lx, y1 + 1, lz - 3)
    put("honinglift_onder", lx, y0, lz + 3)
    lx, lz, y0, y1, aan = P.lift_b()
    put("honinglift_kruin_boven", lx, y1 + 1, lz - 3)
    put("honinglift_kruin_onder", lx, y0 + 1, lz + 3)
    # ---------------------------------------------------------------- the inner stair + Schorsstad (hollow trunk)
    for i, (yb, naam) in enumerate(B.BORDES + [(B.NEST_Y + 1, "nest")]):
        a, L_ = B.trap_landing(i)
        put(f"trap_bordes_{i + 1}", 20 * math.cos(a), L_ + 1, 20 * math.sin(a))
    put("schorsstad_vloer", 9, B.VOET + 1, -8)
    put("kampvuur_schorsstad_vloer", -10, B.VOET + 1, 7)
    for i, y in enumerate(P.TOREN_RINGEN):
        put(f"lantaarnring_{i + 1}", 4.0, y + 1, 0.5)
        put(f"lantaarnring_{i + 1}_kooi", -4.0, y + 1, 0.5)
        x, yy, z = trap_punt(y + 2, 20.5)
        put(f"schorsstad_ring_{i + 1}", x, yy, z)
    put("nestlantaarn_knoest", 0.5, P.TOREN_RINGEN[3] + 1, 4.0)
    huizen = P.schors_huizen()
    for k, (y0_, a, h) in enumerate(huizen):
        ca, sa_ = math.cos(a), math.sin(a)
        PP.setdefault("schorshuis", {})[str(k + 1)] = W((B.R_IN + 3.5) * ca, h + 1, (B.R_IN + 3.5) * sa_)
    y0_, a, h = huizen[len(huizen) // 2]
    put("npc_oude_knoest", (B.R_IN + 3.5) * math.cos(a), h + 1, (B.R_IN + 3.5) * math.sin(a))
    y0_, a, h = huizen[len(huizen) // 2 + 2]
    put("kampvuur_schorsstad_ring4", (B.R_IN + 4) * math.cos(a), h + 1, (B.R_IN + 4) * math.sin(a))
    for k in range(8):
        lev = 100 + k * 9 + (40 if k >= 6 else 0)
        a = B.trap_hoek(lev)
        put(f"muurverhaal_{k + 1}", 21.5 * math.cos(a), float(B.stand(lev)), 21.5 * math.sin(a))
    for k in range(4):
        lev = 172 + k * 13
        x, yy, z = trap_punt(lev, 19)
        put(f"boomvolk_lichtje_{k + 1}", x, yy, z)
    a = B.KWASTGATEN[0][0]
    lev_k = None
    for hs in B.trap_hoogtes(np.array([a])):
        if not np.isnan(hs[0]) and abs(hs[0] - 120) < 10:
            lev_k = float(hs[0])
    lev_k = lev_k if lev_k is not None else 120.0
    put("flash_f1_kwastgat", 21 * math.cos(a), float(B.stand(lev_k)), 21 * math.sin(a))
    put("vingerafdruk", 23.5 * math.cos(a + 0.18), float(B.stand(lev_k)) + 2, 23.5 * math.sin(a + 0.18))
    # the two lifts in the hollow trunk: floor -> the Honingtak landing (binnenhof deck), binnenhof -> the kruin-1 landing
    a1 = B.TH_M + math.pi
    put("schorslift_1_onder", 9 * math.cos(a1), B.VOET + 1, 9 * math.sin(a1))
    put("schorslift_1_boven", 9 * math.cos(a1), HY + 1, 9 * math.sin(a1))
    a2, L2 = B.trap_landing(2)
    put("schorslift_2_onder", 9 * math.cos(a2), HY + 1, 9 * math.sin(a2))
    put("schorslift_2_boven", 9 * math.cos(a2), L2 + 1, 9 * math.sin(a2))
    put("binnenhof", 0, HY + 1, 6)
    # gates: the two ground gates (v1 ids 75 / 128) and the Honingtak door (v1 id 176)
    for yid, a in zip((75, 128), B.POORT_HOEKEN):
        put(f"schorsstad_poort_{yid}", 19 * math.cos(a), B.VOET + 1, 19 * math.sin(a))
        r_uit = poort_uit_r(a)
        put(f"spiraal_poort_{yid}", r_uit * math.cos(a), gstand(r_uit * math.cos(a), r_uit * math.sin(a)), r_uit * math.sin(a))
    a, _ = B.trap_landing(1)
    put("schorsstad_poort_176", 21 * math.cos(a), HY + 1, 21 * math.sin(a))
    put("spiraal_poort_176", 45 * math.cos(a), HY + 1, 45 * math.sin(a))
    put("rijpad_honingplein", 37 * math.cos(B.TH_M), HY + 1, 37 * math.sin(B.TH_M))
    # balconies on the riding spiral (glide jumps) + its end on kruin 1
    for k, (turn, deg) in enumerate(BALKONS):
        th = math.radians(deg)
        x, yy, z = rij_punt_lev(turn, th, 1.0)
        a = math.atan2(z, x)
        r = math.hypot(x, z)
        put(f"spiraal_balkon_{k + 1}", (r + 4) * math.cos(a), yy, (r + 4) * math.sin(a))
        put(f"spiraal_zweefsprong_{k + 1}", (r + 8) * math.cos(a), yy, (r + 8) * math.sin(a))
        if k == 0:
            put("pluimstal_zweefplatform", (r + 8) * math.cos(a), yy, (r + 8) * math.sin(a))
    th_end = B.RIJ["th0"] + 2 * math.pi * (B.RIJ["eind"] - B.RIJ["y0"]) / B.RIJ["stijg"]
    x, yy, z = rij_punt_lev(int((B.RIJ["eind"] - B.RIJ["y0"]) // B.RIJ["stijg"]), th_end - 0.08, 0.5)
    put("spiraal_top", x, yy, z)
    put("rijpad_eind", x, yy, z)
    # the Pelszaal (Graaf Piepvel) under the Schorsstad floor
    fy = PELSZAAL_VLOER
    put("arena_piepvel", 0, fy + 1, 0)
    put("piepvel_troon", 0, fy + 1, -12)
    put("piepvel_grote_kooi", 6, fy + 1, -11)
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        put(f"piepvel_kooi_{k + 1}", 10 * math.cos(a), fy + 3 + 2 * k, 10 * math.sin(a))
    for k in range(6):
        a = k * math.pi / 3
        put(f"piepvel_lantaarnpaal_{k + 1}", 13.5 * math.cos(a), fy + 1, 13.5 * math.sin(a))
    put("hartsplinter_4", 3, fy + 1, 4)
    pa = pelszaal_trap_hoek()
    put("arena_piepvel_deur", 13 * math.cos(pa), fy + 1, 13 * math.sin(pa))
    put("pelszaal_trap_boven", 21 * math.cos(pa - 44.0 / 21.0 - 0.06), B.VOET + 1, 21 * math.sin(pa - 44.0 / 21.0 - 0.06))
    # ---------------------------------------------------------------- Kruinzee (the 3 crown storeys + the nest)
    put("arena_vahoegnir", 0, NEST_STAND, 0)
    put("nest_eierkuil", 0, NEST_STAND, 0)
    a5, _ = B.trap_landing(5)
    put("nest_rand", 13 * math.cos(a5), NEST_STAND, 13 * math.sin(a5))
    put("hartsplinter_2", -7, NEST_STAND, 6)
    for i, (dx, dz) in enumerate(((11, 5), (-11, 5), (0, 13), (11, -7), (-11, -7), (0, -14))):
        put(f"nest_tak_{i + 1}", dx, NEST_STAND, dz)
    kx, kz = KALE_KRUIN
    put("arena_kruinroofvogel", kx, KALE_Y, kz)
    ka = math.atan2(kz, kx)
    bx_, bz_ = pol(ka, 120)
    put("arena_kruinroofvogel_bord", bx_, kstand(0, bx_, bz_), bz_)
    bx_, bz_ = pol(ka + 0.08, 119)
    put("arena_kruinroofvogel_deur", bx_, kstand(0, bx_, bz_), bz_)
    for i, (tak, s_) in enumerate((("Bijentak", 140), ("Noordtak", 128))):
        x, y, z = tak_punt(tak, s_)
        put(f"kruinroofvogel_zit_{i + 1}", x, y + 1, z)
    mx_, mz_ = pol(math.radians(300), 110)
    put("mika_steiger", mx_, kstand(0, mx_, mz_), mz_)
    put("mika_bevel", mx_ - 6, kstand(0, mx_ - 6, mz_ + 4), mz_ + 4)
    for i, (s, deg, r) in enumerate(((0, 30, 104), (1, 140, 70), (0, 250, 112))):
        x, z = pol(math.radians(deg), r)
        put(f"grauwe_vlek_{i + 1}", x, kstand(s, x, z), z)
    for i, (a, r, R) in enumerate(VIJVERS):
        x, z = pol(a, r)
        ex_, ez_ = x + (R + 3) * math.cos(a + 1.6), z + (R + 3) * math.sin(a + 1.6)
        put(f"honingvijver_{i + 1}", ex_, kstand(0, ex_, ez_), ez_)
        kx_, kz_ = x + (R + 3) * math.cos(a - 1.6), z + (R + 3) * math.sin(a - 1.6)
        if i == 2:   # W-INT: that side of pond 3 is the crown's steep leaf edge (snapped to y 213, unreachable): the walkable rim
            kx_, kz_ = x + (R + 3) * math.cos(a + 1.25), z + (R + 3) * math.sin(a + 1.25)   # next to honingvijver_3
        put(f"kuiken_{i + 1}", kx_, kstand(0, kx_, kz_), kz_)
    for i, deg in enumerate((75, 235)):
        x, z = pol(math.radians(deg), 116)
        put(f"zweefplatform_{i + 1}", x, kstand(0, x, z), z)
    zx, zz = pol(math.radians(235), 100)
    put("kuiken_6", zx, kstand(0, zx, zz), zz)
    ux_, uz_ = pol(0.62, 80)
    put("uitkijk_kroon", ux_, kstand(1, ux_, uz_) + 14, uz_)
    kampen = P.kampen()
    x, z, y, seed, lab = kampen[3]
    put("kampvuur_kruin_1", x, y, z)
    x, z = pol(4.0, 66)
    put("kampvuur_kruin_2", x, kstand(1, x, z), z)
    x, z = pol(2.0, 44)
    put("kampvuur_kruin_3", x, kstand(2, x, z), z)
    a4, L4 = B.trap_landing(4)
    x, z = pol(a4 + 0.25, 38)
    put("kampvuur_kruin_nest", x, kstand(2, x, z), z)
    x, z = pol(1.9, 118)
    put("bloesemwaterval_top", x, kstand(0, x, z), z)
    # ---------------------------------------------------------------- Takkenbanen (the big branches + 3 beekeeper villages)
    dh = P.dorp_huizen()
    zoem = [h for h in dh if h[0] == "Bijentak"]
    rest = [h for h in dh if h[0] != "Bijentak"]
    for (fn, h) in zip(("lusje", "herberg", "winkeltje"), zoem):
        put(f"hangnest_{fn}", h[3], h[5] + 1, h[4])
    for i, h in enumerate(rest[:6]):
        put(f"hangnest_woning_{i + 1}", h[3], h[5] + 1, h[4])
    for (tak, dorp) in P.DORPEN:
        x, y, z = tak_punt(tak, 88)
        key = {"Bijentak": "hangnest_plein", "Zuidwesttak": "raatweide_plein", "Molentak": "molenhoek_plein"}[tak]
        put(key, x, y, z)
    x, y, z = tak_punt("Bijentak", 75, 0.45)
    put("kampvuur_hangnest", x, y, z)
    x, y, z = tak_punt("Bijentak", 100, -0.4)
    put("hangnest_stalpost", x, y, z)
    x, y, z = tak_punt("Bijentak", 46)
    put("hangnest_ladder_top", x, y, z)
    for key, tak, s_, zij in (("kampvuur_tak_west_30", "Westtak", 56, 0.45), ("kampvuur_tak_west_62", "Westtak", 96, -0.45),
                              ("kampvuur_tak_noordwest_60", "Molentak", 76, 0.5),
                              ("kampvuur_tak_zuidwest_45", "Zuidwesttak", 76, 0.5)):
        x, y, z = tak_punt(tak, s_, zij)
        put(key, x, y, z)
    x, y, z = tak_punt("Westtak", 72, 0.4)
    put("pluimpost_westtak", x, y, z)
    for i, (tak, s_) in enumerate((("Westtak", 108), ("Noordtak", 96), ("Zuidwesttak", 106))):
        x, y, z = tak_punt(tak, s_)
        put(f"kaasschutterpost_{i + 1}", x, y, z)
    x, y, z = tak_punt("Westtak", 118)
    put("uitkijk_takpunt", x, y, z)
    for key, tak in (("valbrug", "Westtak"), ("lianenbrug", "Noordtak")):
        x, y, z = tak_punt(tak, 50)
        put(key, x, y, z)
    # ---------------------------------------------------------------- Kaaszeppelinwerf (dungeon 1): its own island
    px, pz = WF.poort_pos()
    put("werf_ingang", px, 161, pz)
    pa_ = WF.POORT_HOEK
    put("werf_bord", px - math.sin(pa_) * 4, 161, pz + math.cos(pa_) * 4)
    put("werf_vahoeg_altaar", px + math.sin(pa_) * 4, 161, pz - math.cos(pa_) * 4)
    b0 = WF.brug_begin()
    put("kabelbrug_begin", b0[0], HY + 1, b0[2])
    put("kabelbrug_eind", px + math.cos(pa_) * 9, 161, pz + math.sin(pa_) * 9)
    for d, aid in zip(WF.DOKKEN, ("arena_taleggio", "arena_brokkel", "arena_roquefort")):
        x, y, z = WF.dok_pos(d)
        put(aid, x, y, z)
    x, y, z = WF.dok_pos(WF.DOKKEN[1])
    put("arena_brokkel_kanon_1", x - 8, y, z + 3)
    put("arena_brokkel_kanon_2", x + 8, y, z + 3)
    x, y, z = WF.dok_pos(WF.DOKKEN[2])
    a3 = math.atan2(WF.C[1] - z, WF.C[0] - x)     # towards the island centre (the boss gate side)
    put("roquefort_kajuit", x + 7 * math.cos(a3 + 1.9), y, z + 7 * math.sin(a3 + 1.9))
    put("hartsplinter_3", x + 8 * math.cos(a3 + 2.1), y, z + 8 * math.sin(a3 + 2.1))
    put("grauwe_brief_1", x + 8 * math.cos(a3 + 1.7), y, z + 8 * math.sin(a3 + 1.7))
    put("roquefort_wrak", x + 17 * math.cos(a3 - 1.4), y - 3, z + 17 * math.sin(a3 - 1.4))
    for key, (x, z) in werf_kamers().items():
        put(key, x, werf_stand(x, z), z)
    bx_, by_, bz_ = PP["werf_kamer_bewapening"]
    PP["werf_loods_poort"] = [bx_, by_, bz_ + 9]
    for key, d in (("werf_trap_ab", WF.DOKKEN[1]), ("werf_trap_bc", WF.DOKKEN[2])):
        px_, pz_, sh_, nx_, nz_ = WF.toren_trap(d)[0]
        put(key, px_, sh_, pz_)
    # ---------------------------------------------------------------- Wortelvelden (the ground of the island)
    mx, mz = MOSVOET
    put("mosvoet_plein", mx, gstand(mx, mz), mz)
    put("kampvuur_mosvoet", mx - 5, gstand(mx - 5, mz + 5), mz + 5)
    for i, f in enumerate(("burgemeester", "theehuis", "winkel", "stalpost", "waterrad", "woning")):
        a = i * math.pi / 3 + 0.3
        x, z = mx + 17 * math.cos(a), mz + 17 * math.sin(a)
        put(f"mosvoet_{f}", x - 5 * math.cos(a), gstand(x - 5 * math.cos(a), z - 5 * math.sin(a)), z - 5 * math.sin(a))
    for i, deg in enumerate((300, 210, 125, 60)):
        x, z = pol(math.radians(deg), WORTELWEG_R + 4)
        put(f"pluimpost_{i + 1}", x, gstand(x, z), z)
    for k in range(6):
        x, z = pol(math.radians(k * 60 + 30), WORTELWEG_R + 5)
        put(f"kampvuur_wortelweg_{k + 1}", x, gstand(x, z), z)
    for i, (x, z, y, seed, lab) in enumerate(kampen[:3]):
        PP.setdefault("kamp", {})[str(i + 1)] = W(x + 3, gstand(x + 3, z + 3), z + 3)
    ru = P.ruines()
    x, z, y, R, seed = ru[0]
    put("wortelgewelf", x + R + 3, gstand(x + R + 3, z), z)
    put("kampvuur_wortelgewelf", x + R + 6, gstand(x + R + 6, z + 4), z + 4)
    put("uitkijk_wortelgewelf", x, gstand(x, z) + 16, z)
    for i, (deg, r) in enumerate(((205, 150), (195, 168), (215, 172), (185, 155))):
        x, z = pol(math.radians(deg), r)
        put(f"schimmelketel_{i + 1}", x + 4, gstand(x + 4, z), z)
    for key, (deg, r) in (("lichtkeverbos", (270, 150)), ("nachtgeheim_zadel", (250, 190)), ("paddenbloesemdal", (95, 160)),
                          ("honingmoeras", (340, 165)), ("schimmelboerderij", (200, 160))):
        x, z = pol(math.radians(deg), r)
        put(key, x, gstand(x, z), z)
    # ---------------------------------------------------------------- Wortelduister + Hart van de Boom (dungeon 2)
    x, z = B.POORT
    put("wortelpoort", x + 14, gstand(x + 14, z), z)
    Lg = B.gang_lijn()
    for k, i in enumerate((len(Lg) // 3, 2 * len(Lg) // 3)):
        x, y, z = Lg[i]
        put(f"kampvuur_wortelgang_{k + 1}", x, gang_stand(y), z + 3)
    for k, i in enumerate(range(2, len(Lg), max(1, len(Lg) // 9))):
        if k >= 9:
            break
        x, y, z = Lg[i]
        put(f"grotlantaarn_{k + 1}", x, gang_stand(y), z - 3.5)
    put("hartslag_zaal", 52, hal_stand(52, 0), 0)
    put("kampvuur_hartslag", 60, hal_stand(60, -14), -14)
    hx, hz = HART_POORT
    put("hart_poort", hx + 3, hal_stand(hx + 3, hz), hz)
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        x, z = hx + 4 + 5 * math.cos(a), hz + 5 * math.sin(a)
        put(f"hart_grotlantaarn_{k + 1}", x, hal_stand(x, z), z)
    put("hart_bord", hx + 6, hal_stand(hx + 6, hz + 5), hz + 5)
    put("hart_vahoeg_altaar", hx + 6, hal_stand(hx + 6, hz - 5), hz - 5)
    ax, az = pol(math.radians(HART_KAMERS[0][1]), HART_R)
    put("hart_kamer_aderloop", ax, HART_VLOER + 1, az * 0.45)
    for (naam, deg, kr) in HART_KAMERS:
        x, z = pol(math.radians(deg), HART_R)
        key = f"arena_{naam}" if naam in ("wortelwurger", "nestwachter", "baron_bleu") else f"hart_kamer_{naam}"
        put(key, x, HART_VLOER + 1, z)
    for naam in ("schimmelkwekerij", "ringenzaal"):
        p, _ = hart_poort_van(naam)
        put(f"hart_{'kwekerij' if naam == 'schimmelkwekerij' else naam}_poort", p[0], HART_VLOER + 1, p[1])
    x, z = pol(math.radians(46), HART_R)
    put("hartsplinter_5", x + 6, HART_VLOER + 1, z - 6)
    put("ei", 0, B.HART[1] - B.HART[3] * 0.72 + 6, 0)
    put("hart_nestkamer", 9, int(B.HART[1] - B.HART[3] * 0.72) + 1, 0)
    # ---------------------------------------------------------------- wind columns
    for (wid, x, z, y0, y1, rad, lx, lz) in windkolommen():
        PP[f"wind_{wid}_onder"] = W(x, y0, z)
        PP[f"wind_{wid}_boven"] = W(lx, y1, lz)
    # ---------------------------------------------------------------- secrets
    sec = [("kruin1", pol(math.radians(120), 100)), ("kruin2", pol(B.trap_landing(3)[0] + 0.5, 40)), ("tak", ("Molentak", 104)),
           ("grond", pol(math.radians(140), 200)), ("kruin1", pol(math.radians(330), 104)), ("grond", pol(math.radians(10), 205)),
           ("grond", pol(math.radians(265), 200)), ("hal", (30, -40)), ("schors", 3), ("duister", (-40, 30))]
    for i, (waar, v) in enumerate(sec):
        if waar.startswith("kruin"):
            s = int(waar[-1]) - 1
            x, z = v
            y = kstand(s, x, z)
        elif waar == "tak":
            x, y, z = tak_punt(v[0], v[1], 0.3)
        elif waar == "schors":
            y0_, a, h = huizen[v]
            x, y, z = (B.R_IN + 5.5) * math.cos(a), h + 1, (B.R_IN + 5.5) * math.sin(a)
        elif waar in ("hal", "duister"):
            x, z = v
            y = hal_stand(x, z)
        else:
            x, z = v
            y = gstand(x, z)
        put(f"geheim_knuffel_{i + 1}", x, y, z)
    x, y, z = tak_punt("Zuidwesttak", 100)
    put("geheim_schat_1", x, y, z)
    put("geheim_schat_2", 4, NEST_STAND, -16)
    y0_, a, h = huizen[-2]
    put("geheim_schat_3", (B.R_IN + 5.5) * math.cos(a), h + 1, (B.R_IN + 5.5) * math.sin(a))
    put("geheim_schat_4", -60, hal_stand(-60, 10), 10)
    x, z = pol(math.radians(355), 195)
    put("geheim_schat_5", x, gstand(x, z), z)
    rt = P.CELLEN["raattros"]
    rc = pol(math.radians(rt[0]), 55)
    pts = [(cx + 10, HY + 1, cz - 4), htak_punt("Kruinlifttak", 104), htak_punt("Lifttak", 70),
           (pol(B.TH_M + 0.9, 50)[0], HY + 1, pol(B.TH_M + 0.9, 50)[1]), (rc[0] + 2, HY - 2 - rt[2] + 1, rc[1] + 2)]
    for i, (x, y, z) in enumerate(pts):
        put(f"imker_aanwijzing_{i + 1}", x, y, z)
    put("landmark_hoogvads_richting", 700, 260, 0)
    return PP


# ================================================================================================ helpers for spots
def poort_uit_r(a):
    """The radius where a ground gate tunnel comes out (the trunk foot ends; the ground is open)."""
    for r in np.arange(30, 90, 1.0):
        ro = float(B.r_stam(B.VOET - 2, np.array([a - (B.VOET - 2) / 160.0]))[0])
        if r > ro + 4:
            return float(r)
    return 70.0


def pelszaal_trap_hoek():
    """The angle of the Pelszaal stair (away from the riding-spiral gates and the stair foot)."""
    a0, _ = B.trap_landing(0)
    return a0 + math.pi * 0.8


def werf_kamers():
    """The werf halls (rel x, z of the centre): the bewapeningsloods north-west of the tower-3 stair start (its +z gate, 3 x 4,
    opens onto a short path to the stair), the gieterij west of the tower-2 stair start, the touwwerk south of Dok 1."""
    s3 = WF.toren_trap(WF.DOKKEN[2])[0]
    s2 = WF.toren_trap(WF.DOKKEN[1])[0]
    x1, y1, z1 = WF.dok_pos(WF.DOKKEN[0])
    return {"werf_kamer_bewapening": (s3[0] - 11, s3[1] - 14), "werf_kamer_gieterij": (s2[0] - 11, s2[1]),
            "werf_kamer_touwwerk": (x1 + 6, z1 + 26)}


def werf_stand(x, z):
    return int(math.floor(WF.werf_top(x, z))) + 1


def gang_stand(yc):
    """Stand y on the floor of the Wortelpoort tunnel (centre line at yc)."""
    return int(math.floor(yc - 5.5 + 1.0)) + 1


def hal_stand(x, z):
    """Stand y on the floor of the Wortelduister hall (koepel without noise; the map snaps it)."""
    r = math.hypot(x, z)
    q = 0.95 - (r / 86.0) ** 2
    if q <= 0:
        return 40
    return int(math.floor(37 - 20 * math.sqrt(q))) + 2


def hart_poort_van(naam):
    """The gate on the z-running corridor stub after the room 'naam' (bazen fills a 3x4 plane along x there)."""
    i = [k[0] for k in HART_KAMERS].index(naam)
    deg, kr = HART_KAMERS[i][1], HART_KAMERS[i][2]
    x, z = pol(math.radians(deg), HART_R)
    zs = -1 if math.sin(math.radians(deg)) > 0 else 1        # towards the next room (clockwise -> smaller angle)
    return (x, z + zs * (kr + 3)), zs


# ================================================================================================ areas (y-ranges)
def _schuif(lijst):
    """The area tables are written with z relative to the trunk (x already world): add TZ_STAM to every z."""
    out = []
    for g in lijst:
        p = list(g[2])
        if g[1] == "doos":
            p[1] += TZ_STAM
            p[3] += TZ_STAM
        else:
            p[1] += TZ_STAM
        out.append((g[0], g[1], tuple(p)) + tuple(g[3:]))
    return out


# (id, shape, params, y0, y1, minLv, maxLv, town). shape "doos": (x1, z1, x2, z2) world; "cilinder": (cx, cz, r0, r1) world.
def gebieden():
    return _schuif(_gebieden())


def _gebieden():
    T = (TX_STAM, 0)
    wc = (TX_STAM + int(WF.C[0]), int(WF.C[1]))
    return [
        ("z3_zeppelinwerf", "cilinder", (wc[0], wc[1], 0, 95), 60, 290, 38, 40, False),
        ("z3_hart", "cilinder", (T[0], T[1], 76, 150), 14, 52, 49, 50, False),
        ("z3_wortelduister", "cilinder", (T[0], T[1], 0, 160), 0, 64, 46, 49, False),
        ("z3_schorsstad", "cilinder", (T[0], T[1], 0, 26), 55, 165, 40, 43, False),
        ("z3_honingtak", "cilinder", (T[0], T[1], 0, 160), 145, 200, 30, 50, True),
        ("z3_stamspiraal", "cilinder", (T[0], T[1], 26, 64), 75, 232, 40, 43, False),
        ("z3_kruinzee", "cilinder", (T[0], T[1], 0, 200), 212, 320, 30, 34, False),
        ("z3_takkenbanen", "cilinder", (T[0], T[1], 0, 260), 140, 320, 34, 38, False),
        ("z3_wortelvelden", "cilinder", (T[0], T[1], 0, 600), 0, 320, 43, 46, False),
    ]


def mobzones():
    return _schuif(_mobzones())


def _mobzones():
    T = (TX_STAM, 0)
    return [
        ("z3_kruinzee_zeppelin", "cilinder", (TX_STAM + 55, -95, 0, 34), 220, 250, 31, 34),
        ("z3_kruinzee_noord", "doos", (TX_STAM - 130, -130, TX_STAM + 130, -10), 222, 300, 30, 33),
        ("z3_kruinzee_zuid", "doos", (TX_STAM - 130, 10, TX_STAM + 130, 130), 222, 300, 30, 34),
        ("z3_takkenbanen_west", "doos", (TX_STAM - 160, -50, TX_STAM - 50, 50), 170, 222, 34, 37),
        ("z3_takkenbanen_noordwest", "doos", (TX_STAM - 60, -160, TX_STAM + 150, -50), 170, 222, 35, 38),
        ("z3_takkenbanen_zuidwest", "doos", (TX_STAM - 140, 50, TX_STAM + 40, 160), 170, 222, 34, 37),
        ("z3_schorsstad_ringen", "cilinder", (T[0], T[1], 0, 24), 172, 228, 40, 43),
        ("z3_wortelvelden_lichtkeverbos", "doos", (TX_STAM - 80, -215, TX_STAM + 80, -110), 50, 110, 43, 45),
        ("z3_wortelvelden_schimmelboerderij", "doos", (TX_STAM - 215, -80, TX_STAM - 110, 80), 50, 110, 44, 46),
        ("z3_wortelvelden_paddenbloesemdal", "doos", (TX_STAM - 80, 110, TX_STAM + 80, 215), 50, 110, 43, 45),
        ("z3_wortelvelden_honingmoeras", "doos", (TX_STAM + 110, -80, TX_STAM + 215, 40), 50, 110, 44, 46),
        ("z3_wortelduister_gang", "doos", (TX_STAM + 40, -12, TX_STAM + 150, 16), 18, 72, 46, 47),
        ("z3_wortelduister_grotten", "cilinder", (T[0], T[1], 24, 86), 14, 58, 47, 49),
    ]


def paden():
    return {k: [(x, z + TZ_STAM) for (x, z) in v] for k, v in _paden().items()}


def _paden():
    """The routes (polylines of world x, z): no wild spawns within ~5 blocks of them."""
    out = {}
    pts = []
    for k in range(6):
        for deg in range(0, 360, 20):
            th = math.radians(deg)
            lev = B.rij_lev(th, k)
            if lev <= B.RIJ["eind"]:
                x, _, z = rij_punt_lev(k, th, 0.5)
                pts.append((TX_STAM + int(x), int(z)))
    out["rijpad"] = pts[::2]
    for tak in ("Bijentak", "Zuidwesttak", "Westtak", "Molentak", "Noordtak"):
        L, rr = B.tak_van(tak)
        out[f"takbaan_{tak.lower()}"] = [(TX_STAM + int(L[i, 0]), int(L[i, 2])) for i in range(0, len(L), 6)]
    out["wortelweg"] = [(TX_STAM + int(x), int(z)) for x, z in (pol(math.radians(a), WORTELWEG_R) for a in range(0, 361, 10))]
    Lg = B.gang_lijn()
    out["wortelgang"] = [(TX_STAM + int(Lg[i, 0]), int(Lg[i, 2])) for i in range(0, len(Lg), 4)]
    b0 = WF.brug_begin()
    px, pz = WF.poort_pos()
    out["kabelbrug"] = [(TX_STAM + int(b0[0]), int(b0[2])), (TX_STAM + int(px), int(pz))]
    return out


def liften(alle):
    """(id, top stand spot, bottom stand spot) for Z3Liften."""
    l = [(f"lift_{k}", alle[f"lift_{k}_boven"], alle[f"lift_{k}_onder"]) for k in ("cafe", "kapel", "raattros")]
    l.append(("honinglift", alle["honinglift_boven"], alle["honinglift_onder"]))
    l.append(("honinglift_kruin", alle["honinglift_kruin_boven"], alle["honinglift_kruin_onder"]))
    for k in (1, 2):
        l.append((f"schorslift_{k}", alle[f"schorslift_{k}_boven"], alle[f"schorslift_{k}_onder"]))
    return l


# ================================================================================================ life lists (code side)
def huis_daken():
    """(x, z, top y of the roof, chimney x, chimney z) for every house with a chimney (rel)."""
    out = []
    for fn, (hx, hz, deur, R, waar, soort) in ring_functies():
        top = HY + 1 + {0: int(5.2 * 1.9), 1: 7 + 6, 2: 5 + 5}[soort]
        dx, dz = math.cos(deur), math.sin(deur)
        out.append((hx, hz, top, hx - dx * 2, hz - dz * 2))
    for h in P.dorp_huizen():
        cx, cz, yt, deur = h[3], h[4], h[5], h[6]
        out.append((cx, cz, yt + 1 + (9 if h[2] != 1 else 13), cx - math.cos(deur) * 2, cz - math.sin(deur) * 2))
    mx, mz = MOSVOET
    for i in range(6):
        a = i * math.pi / 3 + 0.3
        x, z = mx + 17 * math.cos(a), mz + 17 * math.sin(a)
        out.append((x, z, gstand(x, z) + 8, x + 1.5 * math.cos(a), z + 1.5 * math.sin(a)))
    return out


def leven_lijsten():
    out = {"SCHOORSTENEN": [], "BIJENKORVEN": [], "LICHTJES": [], "BRUGGEN": []}
    for (x, z, top, sx, sz) in huis_daken():
        out["SCHOORSTENEN"].append(W(sx, top + 2, sz))
    for h in P.dorp_huizen():
        x, z = h[10]
        n, zij, d = h[8], h[9], h[11]
        out["BIJENKORVEN"].append(W(x - n[0] * zij * 4, h[5] + 2, z - n[2] * zij * 4))
        out["BRUGGEN"].append(W((h[7][0] + h[3]) / 2, h[5] + 1, (h[7][1] + h[4]) / 2))
    for (naam, ad, lengte) in P.HTAKKEN:
        x, y, z = htak_punt(naam, 70)
        out["BRUGGEN"].append(W(x, y, z))
    for k in range(0, 40, 2):
        a = 2 * math.pi * k / 40
        ra = float(P.dek_rand(np.array([a]))[0]) - 0.6
        out["LICHTJES"].append(W(ra * math.cos(a), HY + 5, ra * math.sin(a)))
    for i, y in enumerate(P.TOREN_RINGEN):
        out["LICHTJES"].append(W(4.8, y - 1, 0))
    b0 = WF.brug_begin()
    px, pz = WF.poort_pos()
    out["BRUGGEN"].append(W((b0[0] + px) / 2, 161, (b0[2] + pz) / 2))
    for (x, z, y, seed, lab) in P.kampen():
        out["LICHTJES"].append(W(x + 7, (y or gstand(x, z)) + 4, z - 6))
    return out
