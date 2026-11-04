"""Zone 3 "De Schemerwortel": every fixed coordinate of the zone-3 map (the single source for the builder, POI_Z3.md and
Z3WereldPlekken.java).

Coordinates are written RELATIVE TO THE TRUNK AXIS ("rel": xr, y, zr), like the render generators in kit/ (copied from
renders-schemerwortel/_bron). The zone lies in the EAST (ZONE3_PLAN, decision after WERELDVERHAAL): box x +1600..+3000,
z -700..700, trunk at (+2300, 0), MIRRORED in x relative to the renders (the renders were made for the old west box, with
Honingtak on the branch that points to zone 1; mirrored, Honingtak still points west to zone 1/2 and the far east, with
Hoogvads, lies behind the tree). So:
    world X = 2299 - floor(xr),  world Z = floor(zr),  y = y.
A block at rel x cell [a, a+1) is world block 2299 - a. All "stand" points are the air block a player/NPC stands in.
"""
import math
import os
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HIER, "kit"))
import boom  # noqa: E402

TX_STAM = 2300
X0, X1, Z0, Z1 = 1600, 3000, -700, 700        # the zone box (plan)
TX0, TX1, TZ0, TZ1 = 25, 46, -11, 10           # the tile grid (64x320x64 tiles), x 1600..3007, z -704..703


def wx(xr):
    return int(2299 - math.floor(xr))


def W(xr, y, zr):
    """rel point -> world block [X, Y, Z]."""
    return [wx(xr), int(math.floor(y)), int(math.floor(zr))]


def rel_x(X):
    """world block X -> rel block x (its cell start)."""
    return 2299 - X


def grond(xr, zr):
    return float(boom.grondhoogte(np.array([float(xr)]), np.array([float(zr)]))[0])


def kruin(xr, zr):
    return float(boom.kruin_top(np.array([float(xr)]), np.array([float(zr)]))[0])


def polar(a_deg, r):
    a = math.radians(a_deg)
    return r * math.cos(a), r * math.sin(a)


# ================================================================================================ Honingtak (hub)
DEK_Y = 238                       # deck planks; stand 239
PLEIN = (470, -2)
MAST = (622, 0)
KAPEL = (505, 0)
CAFE = (428, 0)
WONEN = (568, 0)
STAL = (296, 236, 26)             # corner of the stable plateau (planks at y 235, stand 236)
# korfhuisjes: (x, side, floor-y, kind, function)
KORVEN = [(388, 1, 216, 0, "woning_1"), (412, -1, 212, 1, "smid"), (440, 1, 222, 2, "juwelier"),
          (458, -1, 218, 0, "drankjes"), (492, 1, 214, 1, "uitrusting"), (520, -1, 224, 2, "algemeen"),
          (546, 1, 220, 0, "bank"), (584, -1, 214, 1, "jagershut"), (600, 1, 226, 2, "burgemeester"),
          (402, 1, 228, 1, "woning_2"), (430, -1, 228, 2, "woning_3"), (536, -1, 210, 0, "woning_4"),
          (566, 1, 230, 2, "woning_5"), (476, -1, 230, 1, "woning_6"), (510, 1, 230, 0, "woning_7"),
          (594, -1, 230, 2, "woning_8")]
KORF_R = 6.0


def tak_oost(x):
    L, rr = boom.tak_lijn("oost")
    i = int(np.argmin(np.abs(L[:, 0] - x)))
    return float(L[i, 1]), float(rr[i]), float(L[i, 2])


def korf_midden(x, kant, fy):
    yc, r, zc = tak_oost(x)
    return zc + kant * (r + 6.5 + (2 if fy >= 228 else 0))


def tak_onder(x):
    yc, r, zc = tak_oost(x)
    return yc - r * 0.72


# raatcel bottoms (the cafe, the Nestkapel and the woontros lowest cell): floor y, centre, R
CAFE_CEL = (CAFE[0], int(tak_onder(CAFE[0]) - 19 - 10), CAFE[1], 9)
KAPEL_CEL = (KAPEL[0], int(tak_onder(KAPEL[0]) - 19 - 13), KAPEL[1], 10)
WOON_CEL = (WONEN[0], int(tak_onder(WONEN[0]) - 17 - 8), WONEN[1] - 1, 6)
# small honey lifts: (id, x, z of the shaft, bottom stop y (stand), the cell it serves)
LIFTJES = [("cafe", 428, -22, CAFE_CEL[1] + 1), ("kapel", 505, 23, KAPEL_CEL[1] + 1), ("raattros", 568, -20, WOON_CEL[1] + 1)]
# the big Honinglift (Honingtak <-> Mosvoet), opens after z3_hq09
HONINGLIFT_BOVEN = (392, DEK_Y + 1, 26)
MOSVOET = (250, 420)
HONINGLIFT_ONDER = (262, None, 392)      # y from the terrain

# ================================================================================================ Kruinzee
NEST = (0, -40)
NEST_Y = int(round(kruin(*NEST)))          # floor of the nest bowl (stand)
KALE_KRUIN = (170, -170)                   # the dead crown top: world boss Kruinroofvogel
KALE_Y = int(round(kruin(*KALE_KRUIN))) + 1
ROOFVOGEL_ZIT = [(262, 42), (-40, -292)]   # the two other perches (phase 3, ~200 away)
MIKA_STEIGER = (-190, -200)
VIJVERS = boom.VIJVERS
GRAUWE_VLEKKEN = [(150, 120), (-110, 205), (60, -250)]   # z3_hq04: 3 sprayed leaf patches
ZWEEFPLATFORMS = [(-300, 70), (-235, 215)]               # Pluimzweef down to the Takkenbanen (crown edge, west/south-west)
KROON_UITKIJK = (-150, 150)
WATERVAL_A = 108


def kruinring_punt(a_deg):
    a = math.radians(a_deg)
    rr = 225 + 75 * math.cos(a) + 5
    return rr * math.cos(a), rr * math.sin(a)


# ================================================================================================ Takkenbanen
HANGNEST = (-488, 140, 33)
HANGNEST_HUISJES = [(-20, -6, 4), (-14, 14, -3), (2, 21, 6), (18, 12, -6), (22, -6, 3), (8, -20, -1),
                    (-8, -22, 8), (-30, 6, 11), (30, 4, 10), (-2, 32, 12)]
HANGNEST_FUNCTIE = {0: "lusje", 1: "herberg", 2: "winkeltje", 3: "stalpost", 4: "woning_1", 5: "woning_2",
                    6: "woning_3", 7: "woning_4", 8: "woning_5", 9: "woning_6"}
WERF = (-600, 140, -570)
TAKPUNT = "zuidwest"
KAASSCHUTTER_POSTEN = [("west", 0.42), ("noordwest", 0.45), ("zuidwest", 0.55)]


def tak_zij_punt(naam, t, frac):
    """Point on top of branch 'naam' at fraction t, moved sideways by frac * radius (off the riding path): (x, stand-y, z)."""
    L, rr = boom.tak_lijn(naam)
    i = max(1, min(len(L) - 2, int(round(t * (len(L) - 1)))))
    d = L[i + 1] - L[i - 1]
    n = np.array([-d[2], 0.0, d[0]])
    n /= np.linalg.norm(n) + 1e-9
    c = L[i] + n * frac * rr[i]
    y = L[i, 1] + math.sqrt(max(0.0, 1 - frac * frac)) * rr[i] * 0.72
    return float(c[0]), int(math.floor(y)) + 1, float(c[2])


def tak_punt(naam, t):
    """Point on the riding path on top of branch 'naam' at fraction t (0 = trunk, 1 = tip): (x, stand-y, z)."""
    L, rr = boom.tak_lijn(naam)
    i = int(round(t * (len(L) - 1)))
    return float(L[i, 0]), float(L[i, 1] + rr[i] * 0.72) + 1, float(L[i, 2])


# the Kaaszeppelinwerf (dungeon 1): three deck levels under the NW branch tip (rel; y = stand)
WERF_A, WERF_B, WERF_C = 140, 125, 110       # deck floors (planks at y, stand y+1)


def werf_plekken():
    cx, cy, cz = WERF
    return {"werf_kamer_touwwerk": (cx + 18, WERF_A + 1, cz + 16), "arena_taleggio": (cx - 22, WERF_A + 1, cz - 14),
            "werf_kamer_gieterij": (cx + 26, WERF_B + 1, cz - 16), "werf_kamer_bewapening": (cx - 28, WERF_B + 1, cz + 16),
            "arena_brokkel": (cx + 24, WERF_B + 1, cz + 16), "arena_brokkel_kanon_1": (cx + 16, WERF_B + 1, cz + 24),
            "arena_brokkel_kanon_2": (cx + 32, WERF_B + 1, cz + 24), "arena_roquefort": (cx, WERF_C + 1, cz),
            "roquefort_kajuit": (cx + 12, WERF_C + 1, cz), "hartsplinter_3": (cx + 13, WERF_C + 1, cz + 2),
            "grauwe_brief_1": (cx + 13, WERF_C + 1, cz - 2), "werf_trap_ab": (cx + 4, WERF_A + 1, cz - 26),
            "werf_trap_bc": (cx - 6, WERF_B + 1, cz + 26)}


# ================================================================================================ Schorsstad (hollow trunk)
R_BINNEN = boom.R_BINNEN
RINGEN = [80 + 16 * i for i in range(7)]          # terrace floors (planks at y, stand y+1)
TOREN_RING_Y = [y + 3 for y in RINGEN]            # tower ring platforms (stand +1)
POORT_Y = (RINGEN[6], RINGEN[3], 75)               # the 3 gates from the Stamspiraal in (= a ring floor / the floor)
PELSZAAL_Y = 183                                  # floor of the Pelszaal (stand 184)


def spiraal_poort(yp):
    p = boom.SPIRAAL
    a = (p["y_top"] - yp) / (p["y_top"] - p["y_bot"]) * 2 * math.pi * p["slagen"] + p["phi0"]
    return a


# ================================================================================================ Wortelvelden
POSTEN = [(270, 520), (225, 520), (135, 500), (45, 540)]          # Pluimposten (deg, r)
WORTELWEG_R = 470
WORTELGEWELF = (340, -150)
WORTELPOORT = (0, 268)
SCHIMMEL_KETELS = [(-320, -280), (-280, -330), (-345, -335), (-260, -270)]

# ================================================================================================ Wortelduister + Hart
HARTSLAG = boom.HARTSLAG                # (cx, floor y, cz, r, h)
WORTELGANG = boom.WORTELGANG
EI = (0, 56, 0)
HART_RING_R = 95                        # the dungeon rooms of the Hart lie in a ring at r 95, floor y 50
HART_VLOER = 50
HART_KAMERS = [("aderloop", -135, 12), ("schimmelkwekerij", 180, 16), ("ringenzaal", 135, 14),
               ("wortelwurger", 90, 20), ("nestwachter", 45, 20), ("baron_bleu", 0, 22)]

# ================================================================================================ wind columns
def _tak_zij(naam, t, zij, extra=6.0):
    L, rr = boom.tak_lijn(naam)
    i = max(1, min(len(L) - 2, int(round(t * (len(L) - 1)))))
    d = L[i + 1] - L[i - 1]
    n = np.array([-d[2], 0.0, d[0]])
    n /= np.linalg.norm(n) + 1e-9
    c = L[i] + n * zij * (rr[i] + extra)
    lp = L[i] + n * zij * (rr[i] * 0.45)
    return float(c[0]), float(c[2]), int(L[i, 1] + rr[i] * 0.72) + 1, float(lp[0]), float(lp[2])


def windkolommen():
    """(id, x, z, y0 stand at the bottom, y1 = stand y of the landing, radius, landing x, landing z), rel coords.
    Upward blossom wind: from the ground (a petal ring) straight up; at the top the wind sets you down on the landing
    next to it (a branch path / the crown top / the Honingtak balcony)."""
    out = []
    for wid, naam, t, zij in (("hangnest", "west", 0.70, -1), ("paddenbloesem_tak", "zuidwest", 0.62, 1),
                              ("werf_tak", "noordwest", 0.62, -1)):
        x, z, y1, lx, lz = _tak_zij(naam, t, zij)
        out.append((wid, x, z, stand_y_grond(x, z), y1, 3.0, lx, lz))
    for wid, (x, z) in (("mosvoet_kruin", (205, 195)), ("lichtkever_kruin", (40, -300))):
        a = math.atan2(z, x)
        lx, lz = x - 9 * math.cos(a), z - 9 * math.sin(a)
        out.append((wid, x, z, stand_y_grond(x, z), int(round(kruin(lx, lz))) + 1, 3.5, lx, lz))
    x, z = HONINGLIFT_BOVEN[0] + 10, HONINGLIFT_BOVEN[2] + 12
    out.append(("honingtak", x, z, stand_y_grond(x, z), DEK_Y + 1, 3.0, HONINGLIFT_BOVEN[0] + 4, HONINGLIFT_BOVEN[2] + 4))
    return out


WINDKOLOMMEN = None   # filled lazily (needs stand_y_grond below)


def stand_y_grond(xr, zr):
    return int(math.floor(grond(xr, zr))) + 1


def plekken():
    """All POIs (world coordinates) as a nested dict; ids are flattened with '_' / '.'."""
    P = {}

    def put(k, xr, y, zr):
        P[k] = W(xr, y, zr)

    # ---- airship dock + travel
    put("dok_landing", MAST[0] - 12, DEK_Y + 1, MAST[1] + 2)
    put("dok_bel", MAST[0] - 4, DEK_Y + 1, MAST[1] + 3)
    put("dok_mast", MAST[0], DEK_Y + 1, MAST[1])
    put("dok_luchtschip", MAST[0] - 18, DEK_Y - 1, MAST[1] + 24)
    put("dok_kapitein", MAST[0] - 8, DEK_Y + 1, MAST[1] - 3)
    # ---- Honingtak hub
    put("plein", PLEIN[0] - 10, DEK_Y + 1, PLEIN[1] - 8)
    put("respawn", PLEIN[0] - 10, DEK_Y + 1, PLEIN[1] + 6)
    put("knuffelbeeld", PLEIN[0], DEK_Y + 1, PLEIN[1])
    put("npc_raatje_plein", PLEIN[0] + 6, DEK_Y + 1, PLEIN[1] - 6)
    put("questbord", PLEIN[0] - 12, DEK_Y + 1, PLEIN[1] - 3)
    put("markt", 420, DEK_Y + 1, 0)
    for i, x in enumerate(range(398, 448, 12)):
        for s in (-1, 1):
            yc, r, zc = tak_oost(x)
            put(f"npc_kraam_{i + 1}{'n' if s < 0 else 'z'}", x, DEK_Y + 1, zc + s * (r * 0.62 + 1 + 3) + (0.5 if s > 0 else -0.5) - s)
    put("bloesemboompje", 548, DEK_Y + 1, 9)
    gebouw = {}
    for (x, kant, fy, soort, fn) in KORVEN:
        cz = korf_midden(x, kant, fy)
        gebouw[fn] = {"npc": W(x, fy, cz - kant * 2.5), "binnen": W(x, fy, cz), "deur": W(x, fy, cz + kant * (KORF_R + 0.9)),
                      "doos_min": W(x + 7, fy - 1, min(cz - 7, cz + 7)), "doos_max": W(x - 7, fy + 11, max(cz - 7, cz + 7))}
    cx, yb, cz, R = CAFE_CEL
    gebouw["cafe"] = {"npc": W(cx, yb + 1, cz - 6), "binnen": W(cx, yb + 1, cz + 1), "deur": W(cx, yb + 1, cz - R - 3),
                      "podium": W(cx + 1, yb + 1, cz + 2)}
    cx, yb, cz, R = KAPEL_CEL
    gebouw["nestkapel"] = {"npc": W(cx, yb + 1, cz - 6), "binnen": W(cx, yb + 1, cz + 3), "deur": W(cx, yb + 1, cz + R + 3),
                           "eiersokkel": W(cx, yb + 1, cz - 2), "droomsteen": W(cx - 5, yb + 1, cz + 2),
                           "advancement": W(cx + 4, yb + 1, cz + 2)}
    cx, yb, cz, R = WOON_CEL
    gebouw["raattros"] = {"binnen": W(cx, yb + 1, cz), "deur": W(cx, yb + 1, cz - R - 3)}
    x0, y, z0 = STAL
    gebouw["pluimstal"] = {"npc": W(x0 + 30, y, z0 + 17), "binnen": W(x0 + 30, y + 1, z0 + 8), "deur": W(x0 + 30, y, z0 + 18),
                           "voer": W(x0 + 20, y, z0 + 17)}
    jx, jk, jfy = KORVEN[7][0], KORVEN[7][1], KORVEN[7][2]
    jcz = korf_midden(jx, jk, jfy)
    gebouw["jagershut"]["premiebord"] = W(jx - 3, jfy, jcz - jk * 2)
    gebouw["jagershut"]["jachtbord"] = W(jx + 3, jfy, jcz - jk * 2)
    P["gebouw"] = gebouw
    put("pluimstal_keuze", x0 + 34, y, z0 + 32)                      # the starter choice: player stands here
    for i, dx in enumerate((-8, 0, 8)):
        put(f"pluimstal_keuze_pluimguh_{i + 1}", x0 + 34 + dx, y, z0 + 42)
    put("pluimstal_oefenbaan", x0 + 34 + 26, y, z0 + 40)
    put("pluimstal_zweefplatform", x0 + 6, y, z0 + 76)
    put("pluimstal_kruinring_start", x0 + 2, y, z0 - 6)
    # small honey lifts (stand spots at the top and bottom stop)
    for (lid, x, z, yo) in LIFTJES:
        put(f"lift_{lid}_boven", x, DEK_Y + 1, z + (2 if z < 0 else -2))
        put(f"lift_{lid}_onder", x, yo, z + (2 if z < 0 else -2) * -1)
    put("honinglift_boven", HONINGLIFT_BOVEN[0], HONINGLIFT_BOVEN[1], HONINGLIFT_BOVEN[2] - 2)
    put("honinglift_onder", 255 + 2, int(math.floor(grond(255, 400))) + 1, 400)
    put("mika_imker_luik", WONEN[0] + 3, WOON_CEL[1] + 1, WONEN[1] + 2)        # secret hatch in the Raattrossen
    put("mika_imker_raatzaal", WONEN[0] + 2, WOON_CEL[1] - 12, WONEN[1])       # secret honeycomb hall (arena, lv 45)

    # ---- Kruinzee
    nx, nz = NEST
    put("arena_vahoegnir", nx, NEST_Y, nz)
    put("nest_eierkuil", nx, NEST_Y, nz)
    put("nest_rand", nx, NEST_Y + 3, nz + 22)
    put("hartsplinter_2", nx - 14, NEST_Y, nz - 9)
    kx, kz = KALE_KRUIN
    put("arena_kruinroofvogel", kx, KALE_Y, kz)
    put("arena_kruinroofvogel_bord", kx + 20, KALE_Y, kz + 20)
    for i, (x, z) in enumerate(ROOFVOGEL_ZIT):
        put(f"kruinroofvogel_zit_{i + 1}", x, int(round(kruin(x, z))) + 1, z)
    mx, mz = MIKA_STEIGER
    put("mika_steiger", mx + 6, int(kruin(mx, mz)) + 2, mz + 4)
    put("mika_bevel", mx - 6, int(kruin(mx, mz)) + 2, mz - 4)
    for i, (x, z) in enumerate(GRAUWE_VLEKKEN):
        put(f"grauwe_vlek_{i + 1}", x, int(round(kruin(x, z))) + 1, z)
    for i, (x, z, r) in enumerate(VIJVERS):
        put(f"honingvijver_{i + 1}", x + r + 4, int(round(kruin(x + r + 4, z))) + 1, z)
    for i, (x, z) in enumerate(ZWEEFPLATFORMS):
        put(f"zweefplatform_{i + 1}", x, int(round(kruin(x, z))) + 1, z)
    put("uitkijk_kroon", KROON_UITKIJK[0], int(round(kruin(*KROON_UITKIJK))) + 15, KROON_UITKIJK[1])
    for k in range(3):
        a = k * 120 + 30
        x, z = kruinring_punt(a)
        put(f"kampvuur_kruin_{k + 1}", x, int(round(kruin(x, z))) + 1, z)
    put("kampvuur_kruin_nest", nx + 34, int(round(kruin(nx + 34, nz + 30))) + 1, nz + 30)
    a = math.radians(WATERVAL_A)
    rr = float(boom.kruinrand(np.array([a]))[0]) - 12
    put("bloesemwaterval_top", rr * math.cos(a), int(round(kruin(rr * math.cos(a), rr * math.sin(a)))) + 1, rr * math.sin(a))

    # ---- Takkenbanen
    hx, hy, hz = HANGNEST
    put("hangnest_plein", hx + 4, hy, hz + 4)
    put("kampvuur_hangnest", hx, hy, hz)
    for i, (dx, dz, dy) in enumerate(HANGNEST_HUISJES):
        f = HANGNEST_FUNCTIE[i]
        put(f"hangnest_{f}", hx + dx, hy + dy, hz + dz)
    put("hangnest_ladder_top", hx + 10, int(boom.tak_top("west", hx + 10, hz)) + 1, hz)
    for naam, t in (("west", 0.3), ("west", 0.62), ("noordwest", 0.6), ("zuidwest", 0.45)):
        x, y, z = tak_zij_punt(naam, t, 0.5)
        put(f"kampvuur_tak_{naam}_{int(t * 100)}", x, y, z)
    x, y, z = tak_zij_punt("west", 0.5, -0.5)
    put("pluimpost_westtak", x, y, z)
    for i, (naam, t) in enumerate(KAASSCHUTTER_POSTEN):
        x, y, z = tak_punt(naam, t)
        put(f"kaasschutterpost_{i + 1}", x, y, z)
    x, y, z = tak_punt("zuidwest", 1.0)
    put("uitkijk_takpunt", x, y, z)
    put("valbrug", -455, 186, 160)
    put("lianenbrug", -380, 196, -40)
    # ---- Kaaszeppelinwerf (dungeon 1)
    cx, cy, cz = WERF
    x, y, z = tak_punt("noordwest", 0.9)
    put("werf_ingang", x, y, z)
    put("werf_bord", x + 3, y, z + 3)
    put("werf_vahoeg_altaar", x - 3, y, z - 3)
    for k, v in werf_plekken().items():
        put(k, *v)

    # ---- Stamspiraal + Schorsstad
    for yp in POORT_Y:
        a = spiraal_poort(yp)
        put(f"spiraal_poort_{yp}", 104 * math.cos(a), yp + 1, 104 * math.sin(a))
        put(f"schorsstad_poort_{yp}", 60 * math.cos(a), yp + 1, 60 * math.sin(a))
    for k, yb in enumerate((190, 150, 110)):
        p = boom.SPIRAAL
        a = (p["y_top"] - yb) / (p["y_top"] - p["y_bot"]) * 2 * math.pi * p["slagen"] + p["phi0"]
        r = float(boom.r_buiten(np.array([float(yb)]), np.array([a]))[0]) + 6
        put(f"spiraal_balkon_{k + 1}", r * math.cos(a), yb + 1, r * math.sin(a))
        put(f"spiraal_zweefsprong_{k + 1}", (r + 4) * math.cos(a + 0.25), yb + 1, (r + 4) * math.sin(a + 0.25))
    put("spiraal_top", 0, 201, -100)
    put("spiraal_voet", 116 * math.cos(math.radians(55)), 71, 116 * math.sin(math.radians(55)))
    put("schorsstad_vloer", 20, 76, 30)
    put("kampvuur_schorsstad_vloer", 26, 76, 34)
    put("kampvuur_schorsstad_ring4", (R_BINNEN - 4) * math.cos(1.2), RINGEN[3] + 1, (R_BINNEN - 4) * math.sin(1.2))
    a = spiraal_poort(POORT_Y[1])
    put("npc_oude_knoest", 56 * math.cos(a - 0.12), RINGEN[3] + 1, 56 * math.sin(a - 0.12))
    for i, y in enumerate(RINGEN):
        put(f"lantaarnring_{i + 1}", 0, TOREN_RING_Y[i] + 1, 5)
        put(f"lantaarnring_{i + 1}_kooi", -4, TOREN_RING_Y[i] + 1, -3)
        put(f"schorsstad_ring_{i + 1}", (R_BINNEN - 4) * math.cos(0.3 + i), y + 1, (R_BINNEN - 4) * math.sin(0.3 + i))
    put("nestlantaarn_knoest", 0, TOREN_RING_Y[3] + 1, -4)
    put("arena_piepvel", 0, PELSZAAL_Y + 1, 0)
    put("piepvel_troon", 0, PELSZAAL_Y + 1, -12)
    put("piepvel_grote_kooi", 5, PELSZAAL_Y + 1, -12)
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        put(f"piepvel_kooi_{k + 1}", 11 * math.cos(a), PELSZAAL_Y + 3 + 3 * k, 11 * math.sin(a))
    for k in range(6):
        a = k * math.pi / 3
        put(f"piepvel_lantaarnpaal_{k + 1}", 14 * math.cos(a), PELSZAAL_Y + 1, 14 * math.sin(a))
    put("hartsplinter_4", 2, PELSZAAL_Y + 1, 3)
    a = math.radians(20)
    put("flash_f1_kwastgat", (R_BINNEN + 2) * math.cos(a), 178, (R_BINNEN + 2) * math.sin(a))
    put("vingerafdruk", (R_BINNEN - 1) * math.cos(a + 0.12), 172, (R_BINNEN - 1) * math.sin(a + 0.12))
    for k in range(8):
        a = k * 2 * math.pi / 8 + 0.15
        y = RINGEN[k % 7] + 1
        put(f"muurverhaal_{k + 1}", (R_BINNEN - 2.5) * math.cos(a), y, (R_BINNEN - 2.5) * math.sin(a))
    for k in range(4):
        a = k * 1.7 + 0.6
        put(f"boomvolk_lichtje_{k + 1}", (R_BINNEN - 2.5) * math.cos(a), RINGEN[(k * 2 + 1) % 7] + 4, (R_BINNEN - 2.5) * math.sin(a))
    for k, (a, y0, y1) in enumerate(((2.6, 76, 177), (5.2, 76, 145))):
        x, z = (R_BINNEN - 10) * math.cos(a), (R_BINNEN - 10) * math.sin(a)
        put(f"schorslift_{k + 1}_onder", x, y0, z)
        put(f"schorslift_{k + 1}_boven", x, y1 + 1, z)

    # ---- Wortelvelden
    mx, mz = MOSVOET
    g = stand_y_grond(mx, mz)
    put("mosvoet_plein", mx, g, mz + 4)
    put("kampvuur_mosvoet", mx - 6, g, mz - 6)
    for i, f in enumerate(("burgemeester", "theehuis", "winkel", "stalpost", "waterrad", "woning")):
        dx, dz = ((-38, 10), (-30, -26), (34, -22), (40, 18), (20, 26), (-18, 38))[i]
        put(f"mosvoet_{f}", mx + dx, stand_y_grond(mx + dx, mz + dz), mz + dz)
    for i, (a, r) in enumerate(POSTEN):
        x, z = polar(a, r)
        put(f"pluimpost_{i + 1}", x, stand_y_grond(x, z), z + 6)
    for k in range(6):
        a = k * 60 + 10
        x, z = polar(a, WORTELWEG_R + 10)
        put(f"kampvuur_wortelweg_{k + 1}", x, stand_y_grond(x, z), z)
    x, z = WORTELGEWELF
    put("wortelgewelf", x, stand_y_grond(x, z), z)
    put("kampvuur_wortelgewelf", x + 3, stand_y_grond(x + 3, z + 3), z + 3)
    put("uitkijk_wortelgewelf", x + 2, 114, z + 2)
    x, z = WORTELPOORT
    put("wortelpoort", x, stand_y_grond(x, z + 14), z + 14)
    for i, (x, z) in enumerate(SCHIMMEL_KETELS):
        put(f"schimmelketel_{i + 1}", x + 9, stand_y_grond(x + 9, z), z)
    put("lichtkeverbos", 0, stand_y_grond(0, -460), -460)
    put("nachtgeheim_zadel", 60, stand_y_grond(60, -520), -520)
    put("paddenbloesemdal", -360, stand_y_grond(-360, 360), 360)
    put("honingmoeras", 380, stand_y_grond(380, 300), 300)
    put("schimmelboerderij", -300, stand_y_grond(-300, -300), -300)
    put("tijdrit_start", *(lambda p: (p[0], stand_y_grond(*p), p[1] + 3))(polar(270, 520)))

    # ---- Wortelduister + Hart van de Boom (dungeon 2)
    L = boom._spline(WORTELGANG, 10)
    for k, i in enumerate((len(L) // 3, 2 * len(L) // 3)):
        x, y, z = L[i]
        put(f"kampvuur_wortelgang_{k + 1}", x, y - 8, z)
    for k, i in enumerate(range(0, len(L), max(1, len(L) // 8))):
        x, y, z = L[i]
        put(f"grotlantaarn_{k + 1}", x + 6, y - 8, z)
    cx, fy, cz, r, h = HARTSLAG
    put("hartslag_zaal", 0, fy + 1, 30)
    put("kampvuur_hartslag", 30, fy + 1, 20)
    put("hart_poort", 0, fy + 1, -50)
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4 - math.pi / 2
        put(f"hart_grotlantaarn_{k + 1}", 8 * math.cos(a), fy + 1, -50 + 6 * math.sin(a))
    put("hart_bord", 6, fy + 1, -46)
    put("hart_vahoeg_altaar", -6, fy + 1, -46)
    for (naam, a_deg, kr) in HART_KAMERS:
        x, z = polar(a_deg, HART_RING_R)
        key = f"arena_{naam}" if naam in ("wortelwurger", "nestwachter", "baron_bleu") else f"hart_kamer_{naam}"
        put(key, x, HART_VLOER + 1, z)
    put("hart_nestkamer", EI[0] + 6, EI[1] - 6, EI[2])
    put("ei", EI[0], EI[1] - 6, EI[2])
    put("hartsplinter_5", 95 * math.cos(math.radians(45)) + 4, HART_VLOER + 1, 95 * math.sin(math.radians(45)))

    # ---- wind columns (bottom stand spot; the top landing)
    for (wid, x, z, y0, y1, rad, lx, lz) in windkolommen():
        P[f"wind_{wid}_onder"] = W(x, y0, z)
        P[f"wind_{wid}_boven"] = W(lx, y1, lz)

    # ---- secrets
    knuffels = [(-300, 70, "zweef"), (200, -120, "kruin"), (-540, 470, "tak_onder"), (-610, -540, "werf"), (120, 300, "kruin"),
                (300, 360, "moeras"), (0, -540, "lichtkever"), (-430, 420, "padden"), (60, 80, "schorsstad"), (-60, 120, "duister")]
    for i, (x, z, waar) in enumerate(knuffels):
        if waar in ("kruin", "zweef"):
            y = int(round(kruin(x, z))) + 1
        elif waar == "tak_onder":
            y = int(boom.tak_top("zuidwest", x, z)) - 30
        elif waar == "werf":
            y = WERF[1] + 1
        elif waar == "schorsstad":
            x, z, y = (R_BINNEN - 4) * math.cos(2.2), (R_BINNEN - 4) * math.sin(2.2), RINGEN[5] + 1
        elif waar == "duister":
            y = 24
        else:
            y = stand_y_grond(x, z)
        put(f"geheim_knuffel_{i + 1}", x, y, z)
    put("geheim_schat_1", -515, int(boom.tak_top("zuidwest", -515, 465)) - 30, 465)      # under the ZW branch, glide only
    put("geheim_schat_2", 0, NEST_Y + 6, -78)
    put("geheim_schat_3", (R_BINNEN - 3) * math.cos(4.0), RINGEN[6] + 1, (R_BINNEN - 3) * math.sin(4.0))
    put("geheim_schat_4", -140, 22, 180)
    put("geheim_schat_5", 410, stand_y_grond(410, 270), 270)
    for i, (x, y, z) in enumerate(((452, DEK_Y + 1, -12), (600, 226, 30), (560, 196, 8), (446, 239, 12), (WONEN[0] + 3, WOON_CEL[1] + 1, 2))):
        put(f"imker_aanwijzing_{i + 1}", x, y, z)

    # ---- Keizer flash + landmarks
    put("flash_f2_kijkpunt", KAPEL_CEL[0], KAPEL_CEL[1] + 1, KAPEL_CEL[2] + 4)
    put("landmark_hoogvads_richting", -700, 260, 0)
    return P


# ================================================================================================ areas (y-ranges)
# (id, shape, params, y0, y1, minLv, maxLv, town). shape "doos": (x1, z1, x2, z2) world; "cilinder": (cx, cz, r0, r1) world,
# r0 = inner radius (ring) . First match wins (order = priority).
def gebieden():
    T = (TX_STAM, 0)
    hx0, hx1 = sorted((wx(665), wx(285)))
    werf = sorted((wx(-680), wx(-520)))
    return [
        ("z3_honingtak", "doos", (hx0, -100, hx1, 110), 150, 290, 30, 50, True),
        ("z3_zeppelinwerf", "doos", (werf[0], -660, werf[1], -480), 95, 185, 38, 40, False),
        ("z3_hart", "cilinder", (T[0], T[1], 0, 120), 40, 74, 49, 50, False),
        ("z3_schorsstad", "cilinder", (T[0], T[1], 0, 76), 75, 199, 40, 43, False),
        ("z3_stamspiraal", "cilinder", (T[0], T[1], 76, 132), 66, 205, 40, 43, False),
        ("z3_kruinzee", "cilinder", (T[0], T[1], 0, 400), 200, 320, 30, 34, False),
        ("z3_takkenbanen", "cilinder", (T[0], T[1], 0, 820), 120, 320, 34, 38, False),
        ("z3_wortelduister", "cilinder", (T[0], T[1], 0, 470), 0, 61, 46, 49, False),
        ("z3_wortelvelden", "cilinder", (T[0], T[1], 0, 900), 0, 320, 43, 46, False),
    ]


# mob zones (wezens slice): (id, shape, params, y0, y1, minLv, maxLv)
def mobzones():
    T = (TX_STAM, 0)
    return [
        ("z3_kruinzee_zeppelin", "cilinder", (wx(-190), -200, 0, 60), 230, 300, 31, 34),
        ("z3_kruinzee_noord", "cilinder", (T[0], -150, 0, 160), 205, 300, 30, 33),
        ("z3_kruinzee_zuid", "cilinder", (T[0], 140, 0, 170), 205, 300, 30, 34),
        ("z3_takkenbanen_west", "doos", (wx(-420), -40, wx(-700), 110), 120, 230, 34, 37),
        ("z3_takkenbanen_noordwest", "doos", (wx(-260), -560, wx(-560), -250), 120, 230, 35, 38),
        ("z3_takkenbanen_zuidwest", "doos", (wx(-260), 240, wx(-560), 520), 120, 230, 34, 37),
        ("z3_schorsstad_ringen", "cilinder", (T[0], T[1], 8, 66), 76, 182, 40, 43),
        ("z3_wortelvelden_lichtkeverbos", "doos", (wx(160), -640, wx(-160), -330), 60, 140, 43, 45),
        ("z3_wortelvelden_schimmelboerderij", "doos", (wx(-180), -420, wx(-420), -180), 60, 140, 44, 46),
        ("z3_wortelvelden_paddenbloesemdal", "doos", (wx(-180), 200, wx(-520), 520), 60, 140, 43, 45),
        ("z3_wortelvelden_honingmoeras", "doos", (wx(520), 150, wx(160), 520), 60, 140, 44, 46),
        ("z3_wortelduister_gang", "cilinder", (T[0], 160, 0, 140), 10, 62, 46, 47),
        ("z3_wortelduister_grotten", "cilinder", (T[0], T[1], 70, 450), 6, 60, 47, 49),
    ]


# the riding routes (polylines of world x, z): no wild spawns within ~5 blocks
def paden():
    out = {}
    pts = [kruinring_punt(a) for a in range(0, 361, 15)]
    out["kruinring"] = [(wx(x), int(z)) for x, z in pts]
    for naam in ("west", "noordwest", "zuidwest"):
        L, rr = boom.tak_lijn(naam)
        out[f"takbaan_{naam}"] = [(wx(L[i, 0]), int(L[i, 2])) for i in range(0, len(L), 6)]
    out["wortelweg"] = [(wx(x), int(z)) for x, z in (polar(a, WORTELWEG_R) for a in range(0, 361, 10))]
    L = boom._spline(WORTELGANG, 10)
    out["wortelgang"] = [(wx(L[i, 0]), int(L[i, 2])) for i in range(0, len(L), 4)]
    return out
