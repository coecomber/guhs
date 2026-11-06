"""Koepelkit: depth pieces for Saffraanpoort (zone 6, the capital). U-Z6WERELD, Saffraanpoort fork.

The lesson of zone 5 (u-z5 gotiek.py): never a flat wall. Every facade here is built in LAYERS in front of / into the wall plane:
  d < 0  into the wall:  arched niches (2 deep, mosaic or glass at the back), door niches, window reveals
  d = 0  the wall plane
  d = 1  pilasters, corbels, the overhanging upper storey, cornices (muqarnas = rows of upside-down stairs)
  d = 2  oriels (mashrabiya panes), balconies with railings, planters, awnings, lanterns
Game rules (survival): ground floors of the chosen houses are enterable with a REAL floor, a round hanging lamp (brass lanterns on a
gold ring) and furniture; everything else is solid inside (closed). Nothing on the street at foot height except stoops (slabs).

Coordinates: game (x east, z south). 'kant' = (dx, dz) the outward normal of a facade; vlak(W, kant, c, u, y, k) puts a block on
the plane (c = x or z of the plane, u along it); the plane d blocks outward is c + d * (dx + dz).
"""
import math
import numpy as np
import zijbouw as ZB
from zijbouw import zet, doos, leeg, vlak, raakt, rng_van, FACE, LUCHT
from zpalet import Z, WOL, TAPIJT, b, trap, plaat

ML = "guhs:messing_lantaarn"
DEUR = "minecraft:cherry_door"


def ml(hangend=True):
    return f"{ML}[hanging={'true' if hangend else 'false'},waterlogged=false]"


def tegen(kant):
    return (-kant[0], -kant[1])


def V(W, kant, c, d, u, y, k):
    """Block on the facade plane d blocks outward (d < 0 = into the wall)."""
    s = kant[0] + kant[1]
    vlak(W, kant, c + s * d, u, y, k)


def Vget(W, kant, c, d, u, y):
    s = kant[0] + kant[1]
    cc = c + s * d
    return W.wget(cc, y, u) if kant[0] else W.wget(u, y, cc)


# ============================================================================================ light
def ronde_luchter(W, cx, y_plafond, cz, r=3.0, lengte=3, lagen=2, kroon=True):
    """ROUND hanging lamp: a chain from the ceiling to a gold hub, one or two gold rings (slabs, read as a circle from below) with
    brass lanterns hanging under them, and a big lantern in the middle. Never a grid."""
    for dy in range(1, lengte + 1):
        zet(W, cx, y_plafond - dy, cz, ZB.ketting("y"))
    yc = y_plafond - lengte - 1
    zet(W, cx, yc, cz, "goudblok" if r >= 2.5 else "goud")
    zet(W, cx, yc - 1, cz, ml(True))
    for li in range(lagen):
        rr = r - li * 1.4
        if rr < 1.2:
            break
        yy = yc - li
        n = max(6, int(round(rr * 2 * math.pi / 1.2)))
        gezet = set()
        for k in range(n):
            a = k * 2 * math.pi / n
            p = (int(math.floor(cx + 0.5 + math.cos(a) * rr)), int(math.floor(cz + 0.5 + math.sin(a) * rr)))
            if p in gezet:
                continue
            gezet.add(p)
            zet(W, p[0], yy, p[1], plaat("goud", "top"))
        # spokes (chains) from hub to ring, lanterns under every other ring point
        for k in range(4):
            a = k * math.pi / 2 + li * math.pi / 4
            for s in np.arange(1.0, rr, 1.0):
                px, pz = cx + 0.5 + math.cos(a) * s, cz + 0.5 + math.sin(a) * s
                if (int(math.floor(px)), int(math.floor(pz))) != (cx, cz):
                    zet(W, px, yy, pz, ZB.ketting("x" if abs(math.cos(a)) > 0.7 else "z"), keep=True)
        for i, p in enumerate(sorted(gezet)):
            if i % 2 == 0:
                zet(W, p[0], yy - 1, p[1], ml(True))
    if kroon and r >= 2.5:
        zet(W, cx, yc + 1, cz, "goud") if lengte >= 2 else None


def lantaarnpaal(W, x, y, z, h=3):
    """Street lamp: a sandstone wall post with a brass lantern on top (stands on the ground at y)."""
    for dy in range(h):
        zet(W, x, y + dy, z, ZB.muurtje("zs") if dy else "zs_beitel")
    zet(W, x, y + h, z, ml(False))


def wandlamp(W, kant, c, u, y):
    """Brass lantern on a wall bracket (1 out)."""
    V(W, kant, c, 1, u, y + 1, trap("zs", FACE[tegen(kant)], half="top"))
    V(W, kant, c, 1, u, y, ml(True))


# ============================================================================================ facade pieces
def muqarnas(W, kant, c, u1, u2, y, k1="zs", k2="goud", band="mozaiek_ster"):
    """Muqarnas cornice: a mosaic band in the wall, then two rows of upside-down stairs stepping out (1, then 2), gold slab on top."""
    for u in range(int(u1), int(u2) + 1):
        V(W, kant, c, 0, u, y, band)
        V(W, kant, c, 1, u, y + 1, trap(k1 if u % 2 else "zs", FACE[tegen(kant)], half="top"))
        V(W, kant, c, 1, u, y + 2, k1)
        V(W, kant, c, 2, u, y + 2, trap(k2 if (u % 3) else k1, FACE[tegen(kant)], half="top"))
        V(W, kant, c, 2, u, y + 3, plaat("goud"))
        V(W, kant, c, 1, u, y + 3, "goud")


def pilaster(W, kant, c, u, y1, y2, k="zs", kop="zuilkop", voet="zs_beitel"):
    """A pilaster 1 out of the wall with a carved base and a golden capital."""
    for y in range(int(y1), int(y2) + 1):
        V(W, kant, c, 1, u, y, voet if y == y1 else (kop if y == y2 else k))


def nis(W, kant, c, u, y, breed=3, h_recht=2, diep=2, achter="mozaiek_bloem", kader="tegel_turkoois", inhoud=None):
    """Blind arched niche INTO the wall (mosaic at the back), with a tile frame; optionally a pot/statue in it."""
    ZB.boog(W, kant, c, u + 0.5, y, breed, h_recht, diep=diep, kader=kader, achter=achter)
    if inhoud:
        V(W, kant, c, -(diep - 2), u, y, inhoud)


def raamnis(W, kant, c, u, y, rng, soort=None, glas="glas"):
    """A window with REAL depth: a 2-deep arched reveal with glass at the back, a sill (stair) and sometimes a planter or shutters."""
    soort = soort or ["boog", "boog", "luik", "rond", "dubbel", "trali"][rng.randint(6)]
    if soort in ("boog", "dubbel", "trali"):
        n = 2 if soort == "dubbel" else 1
        for i in range(n):
            uu = u + (i * 2 - (n - 1))
            for y_ in (y, y + 1):
                V(W, kant, c, 0, uu, y_, b("mashrabiya") if soort == "trali" else LUCHT)
                V(W, kant, c, -1, uu, y_, glas)
            V(W, kant, c, 0, uu, y + 2, trap("zs", FACE[tegen(kant)], half="top"))
            V(W, kant, c, 1, uu, y - 1, trap("zs", FACE[tegen(kant)], half="top"))
        if rng.rand() < 0.55:
            ZB.bloembak(W, kant, c, u, y - 1)
    elif soort == "luik":
        for y_ in (y, y + 1):
            V(W, kant, c, 0, u, y_, LUCHT)
            V(W, kant, c, -1, u, y_, glas)
            for side in (-1, 1):
                V(W, kant, c, 1, u + side, y_, f"minecraft:cherry_trapdoor[facing={FACE[kant]},half=top,open=true,powered=false,waterlogged=false]")
        ZB.bloembak(W, kant, c, u, y - 1)
    else:   # rond: a round guh window in a mosaic disc
        V(W, kant, c, 0, u, y + 1, "glas_guh")
        V(W, kant, c, 0, u, y, "mozaiek_guh")
        for du in (-1, 1):
            V(W, kant, c, 0, u + du, y + 1, "goud")


def erker(W, kant, c, u, y, breed=3, hoog=3, dak="koepel_tegel"):
    """Mashrabiya oriel: 1-2 out on corbels, lattice panes front and sides, a little tiled roof with a gold knob."""
    hb = breed // 2
    for du in range(-hb, hb + 1):
        V(W, kant, c, 1, u + du, y - 1, trap("palm_planken", FACE[tegen(kant)], half="top"))
        V(W, kant, c, 2, u + du, y - 1, trap("palm_planken", FACE[tegen(kant)], half="top"))
        V(W, kant, c, 1, u + du, y - 2, trap("zs", FACE[tegen(kant)], half="top"))
        for dy in range(hoog):
            V(W, kant, c, 2, u + du, y + dy, "mashrabiya")
            V(W, kant, c, 1, u + du, y + dy, LUCHT)
            V(W, kant, c, 0, u + du, y + dy, "glas")
        V(W, kant, c, 1, u + du, y + hoog, plaat("koepel_tegel") if dak == "koepel_tegel" else plaat("palm_planken"))
        V(W, kant, c, 2, u + du, y + hoog, trap("koepel_tegel", FACE[tegen(kant)]) if dak == "koepel_tegel" else plaat("palm_planken"))
    for du in (-hb - 1, hb + 1):
        for dy in range(hoog):
            V(W, kant, c, 1, u + du, y + dy, "mashrabiya")
            V(W, kant, c, 2, u + du, y + dy, "palm_stam")
        V(W, kant, c, 2, u + du, y - 1, trap("palm_planken", FACE[tegen(kant)], half="top"))
        V(W, kant, c, 2, u + du, y + hoog, "goud")


def balkon(W, kant, c, u1, u2, y, k_rail="palm_hek", bloemen=True, seed=0):
    """Balcony: a slab floor 2 out (on corbels), railing, flower pots, an arched door-window behind it (glass)."""
    rng = rng_van(c, u1, y, seed)
    for u in range(int(u1), int(u2) + 1):
        for d in (1, 2):
            V(W, kant, c, d, u, y - 1, plaat("palm_planken", "top"))
            V(W, kant, c, d, u, y, LUCHT)
            V(W, kant, c, d, u, y + 1, LUCHT)
        V(W, kant, c, 1, u, y - 2, trap("zs", FACE[tegen(kant)], half="top"))
        V(W, kant, c, 3, u, y, b(k_rail))
        if bloemen and rng.rand() < 0.4:
            V(W, kant, c, 3, u, y + 1, ZB.PLANTEN[rng.randint(len(ZB.PLANTEN))])
    for u in (int(u1) - 1, int(u2) + 1):
        for d in (1, 2, 3):
            V(W, kant, c, d, u, y, b(k_rail))
    um = (int(u1) + int(u2)) // 2
    for y_ in (y, y + 1, y + 2):
        V(W, kant, c, 0, um, y_, "glas" if y_ < y + 2 else "glas_hart")


def luifel(W, kant, c, u1, u2, y, diep=2):
    """Silk awning (the thin zijden_luifel plate, needs no support) + a gold rod."""
    for u in range(int(u1), int(u2) + 1):
        for d in range(1, diep + 1):
            V(W, kant, c, d, u, y, "luifel_zijde")
        V(W, kant, c, 0, u, y, "goud")


def deur(W, kant, c, u, y0, open_=True, kader="tegel_turkoois", achter="mozaiek_bloem"):
    """Door in an arched niche 2 deep: the real door at d = -1 (only the middle column), mosaic beside it, a silk awning, two brass
    wall lanterns and a stoop. open_ = the room behind is hollow (enterable); else the door stays closed against the wall."""
    ZB.boog(W, kant, c, u + 0.5, y0, 3, 3, diep=2, kader=kader, achter=achter)
    f = FACE[kant]
    V(W, kant, c, -1, u, y0, f"{DEUR}[facing={f},half=lower,hinge=left,open=false,powered=false]")
    V(W, kant, c, -1, u, y0 + 1, f"{DEUR}[facing={f},half=upper,hinge=left,open=false,powered=false]")
    V(W, kant, c, -1, u, y0 + 2, achter)
    luifel(W, kant, c, u - 1, u + 1, y0 + 5)
    for du in (-2, 2):
        V(W, kant, c, 1, u + du, y0 + 3, ml(False)) if False else wandlamp(W, kant, c, u + du, y0 + 2)
    for du in (-1, 0, 1):
        V(W, kant, c, 1, u + du, y0 - 1, "zs")   # stoop is flush with the street (no step)


# ============================================================================================ interiors
FLOORS = ["palm_planken", "mozaiek_ster", "mozaiek_bloem", "vloer_dambord", "tegel_parel"]


def interieur(W, x1, z1, x2, z2, y0, h, rol="woning", seed=0, deur_p=None):
    """Furnish a hollow room (inside x1..x2, z1..z2 from y0, ceiling at y0+h): a real floor, a carpet, a ROUND lamp in the middle,
    furniture along the walls (kept clear of the door). Returns a free stand spot."""
    rng = rng_van(x1, z1, seed, 17)
    fl = FLOORS[rng.randint(len(FLOORS))] if rol != "herberg" else "palm_planken"
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            zet(W, x, y0 - 1, z, fl if fl != "vloer_dambord" or (x + z) % 2 else "wit_glad")
    cx, cz = (x1 + x2) // 2, (z1 + z2) // 2
    # carpet (rug) in the middle
    kl = ["roze", "magenta", "turkoois", "oranje", "lila"][rng.randint(5)]
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            if x1 < x < x2 and z1 < z < z2:
                zet(W, x, y0, z, TAPIJT[kl] if (x + z) % 2 else TAPIJT["geel"])
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            zet(W, x, y0 + h, z, "palm_planken")
    ronde_luchter(W, cx, y0 + h, cz, r=min(2.6, (min(x2 - x1, z2 - z1)) / 2.0 - 0.2), lengte=max(0, h - 4), lagen=1)
    # furniture along the walls, never next to the door
    def vrij_van_deur(x, z):
        return deur_p is None or abs(x - deur_p[0]) + abs(z - deur_p[2]) > 2
    rand = [(x, z1) for x in range(x1, x2 + 1)] + [(x, z2) for x in range(x1, x2 + 1)] + \
           [(x1, z) for z in range(z1 + 1, z2)] + [(x2, z) for z in range(z1 + 1, z2)]
    meubels = {
        "woning": ["guhs:pink_kussen", "guhs:yellow_kussen", "minecraft:barrel[facing=up,open=false]", "guhs:potted_roze_hibiscus",
                   "minecraft:bookshelf", "guhs:cyan_kussen", "minecraft:decorated_pot[facing=north,waterlogged=false,cracked=false]"],
        "herberg": ["minecraft:barrel[facing=up,open=false]", "guhs:pink_kussen", "guhs:yellow_kussen", "guhs:theepotje",
                    "minecraft:barrel[facing=up,open=false]"],
        "bank": ["minecraft:chest[facing=south,type=single,waterlogged=false]", "minecraft:gold_block", "minecraft:barrel[facing=up,open=false]"],
        "winkel": ["minecraft:barrel[facing=up,open=true]", "minecraft:decorated_pot[facing=north,waterlogged=false,cracked=false]",
                   "minecraft:hay_block[axis=y]", "guhs:theepotje", "minecraft:barrel[facing=up,open=false]"],
        "brouwerij": ["minecraft:brewing_stand[has_bottle_0=true,has_bottle_1=false,has_bottle_2=true]", "minecraft:cauldron",
                      "minecraft:barrel[facing=up,open=false]", "minecraft:barrel[facing=up,open=false]"],
    }.get(rol.split("_")[0], ["minecraft:barrel[facing=up,open=false]"])
    for i, (x, z) in enumerate(rand):
        if not vrij_van_deur(x, z):
            continue
        if rng.rand() < 0.45:
            zet(W, x, y0, z, meubels[rng.randint(len(meubels))])
    # a low table with tea in a free spot
    if x2 - x1 >= 4 and z2 - z1 >= 4:
        zet(W, cx + 1, y0, cz, plaat("palm_planken", "top"))
        zet(W, cx + 1, y0 + 1, cz, "guhs:theepotje")
    return (cx - 1 if cx - 1 > x1 else cx, y0, cz)


# ============================================================================================ the house
GEVELS = ["muur_vanille", "muur_roze", "muur_perzik", "zs", "muur_boter", "muur_roze", "muur_lavendel", "zs"]


def huis2(W, x1, z1, x2, z2, y0, kant, verd=2, seed=0, gevel=None, open_=False, rol=None, hid=None):
    """Town house with depth, its front facing 'kant'. Ground floor (5 high) in the footprint; the upper storeys overhang the street by 1
    on corbels; door niche, window reveals, an oriel or a balcony, muqarnas cornice, crenellation, a set-back roof room with a small
    dome (with guh ears) or a wind tower, roof garden. open_: the ground floor is hollow + furnished (zijbouw.huis_rol records it).
    Returns the door position (stand spot in front of it)."""
    if not raakt(W, x1 - 2, y0 - 3, z1 - 2, x2 + 2, y0 + verd * 5 + 16, z2 + 2):
        return None
    rng = rng_van(x1, z1, seed)
    gevel = gevel or GEVELS[rng.randint(len(GEVELS))]
    dx, dz = kant
    s = dx + dz
    hgt = verd * 5
    # body (solid = closed) + plinth below the street
    doos(W, x1, y0 - 3, z1, x2, y0 - 1, z2, "zs")
    doos(W, x1, y0, z1, x2, y0 + hgt, z2, gevel)
    # front plane + range along it
    if dx == 0:
        c = z2 if dz > 0 else z1
        u1, u2 = x1, x2
    else:
        c = x2 if dx > 0 else x1
        u1, u2 = z1, z2
    um = (u1 + u2) // 2
    # the upper storeys overhang 1 (whole width), carried by a corbel row
    if verd >= 2:
        for u in range(u1, u2 + 1):
            V(W, kant, c, 1, u, y0 + 4, trap("zs", FACE[tegen(kant)], half="top"))
            for y in range(y0 + 5, y0 + hgt + 1):
                V(W, kant, c, 1, u, y, gevel)
            V(W, kant, c, 1, u, y0 + 5, "mozaiek_ster" if u % 2 else "goud")
        for u in (u1, u2):     # side cheeks of the overhang get a carved pilaster look
            for y in range(y0 + 5, y0 + hgt):
                V(W, kant, c, 1, u, y, "zs_beitel" if (y - y0) % 5 == 2 else "zs")
        cf = 1                 # the upper facade plane is 1 out
    else:
        cf = 0
    # ground floor: corner pilasters (in the wall plane: quoins) + door + windows/niches
    for u in (u1, u2):
        for y in range(y0, y0 + 4):
            V(W, kant, c, 0, u, y, "zs" if y % 2 else "zs_beitel")
    deur(W, kant, c, um, y0, open_=open_)
    for u in range(u1 + 2, u2 - 1):
        if abs(u - um) < 3:
            continue
        if (u - um) % 3 == 0:
            if open_ or rng.rand() < 0.6:
                raamnis(W, kant, c, u, y0 + 1, rng, soort=["boog", "trali", "luik"][rng.randint(3)])
            else:
                nis(W, kant, c, u, y0, breed=1, h_recht=2, achter="mozaiek_bloem", kader=None,
                    inhoud=ZB.PLANTEN[rng.randint(len(ZB.PLANTEN))])
    # upper storeys
    cc = c + s * cf
    gedaan = False
    for v in range(1, verd):
        yv = y0 + v * 5 + 1
        L = u2 - u1
        if not gedaan and L >= 7 and rng.rand() < 0.75:
            if rng.rand() < 0.55:
                erker(W, kant, cc, um, yv, breed=3, hoog=3)
            else:
                balkon(W, kant, cc, um - 1, um + 1, yv, seed=seed)
            gedaan = True
            for u in (u1 + 2, u2 - 2):
                if abs(u - um) >= 3:
                    raamnis(W, kant, cc, u, yv + 1, rng)
            continue
        n = max(1, (L - 1) // 3)
        for i in range(n):
            u = u1 + 2 + int((i + 0.5) * (L - 3) / n)
            raamnis(W, kant, cc, u, yv + 1, rng)
    # side facades (if free): small window reveals
    for sk in ((dz, dx), (-dz, -dx)):
        if sk[0]:
            sc = x2 if sk[0] > 0 else x1
            su1, su2 = z1, z2
        else:
            sc = z2 if sk[1] > 0 else z1
            su1, su2 = x1, x2
        if Vget(W, sk, sc, 1, (su1 + su2) // 2, y0 + 6) == LUCHT and su2 - su1 >= 5:
            for v in range(1, verd):
                raamnis(W, sk, sc, (su1 + su2) // 2, y0 + v * 5 + 2, rng, soort="rond" if v % 2 else "luik")
    # cornice + roof
    yd = y0 + hgt
    muqarnas(W, kant, cc, u1, u2, yd - 3, band=["mozaiek_ster", "mozaiek_bloem", "mozaiek_guh"][rng.randint(3)])
    doos(W, x1, yd, z1, x2, yd, z2, "steen_vanille")
    if cf:
        for u in range(u1, u2 + 1):
            V(W, kant, c, 1, u, yd, "steen_vanille")
    rx1, rz1, rx2, rz2 = x1, z1, x2, z2
    if cf:
        if dx == 0:
            rz1, rz2 = (z1, z2 + 1) if dz > 0 else (z1 - 1, z2)
        else:
            rx1, rx2 = (x1, x2 + 1) if dx > 0 else (x1 - 1, x2)
    ZB.dakrand(W, rx1, rz1, rx2, rz2, yd + 1, gevel if gevel not in ("zs",) else "muur_perzik")
    # set-back roof room (with a dome) / wind tower / roof garden
    keus = rng.rand()
    if keus < 0.45 and x2 - x1 >= 6 and z2 - z1 >= 6:
        bx1, bz1 = x1 + 2, z1 + 2
        bx2, bz2 = min(x2 - 2, bx1 + 4), min(z2 - 2, bz1 + 4)
        doos(W, bx1, yd + 1, bz1, bx2, yd + 4, bz2, gevel)
        for (kk, cc2, uu) in (((0, 1), bz2, (bx1 + bx2) // 2), ((1, 0), bx2, (bz1 + bz2) // 2), ((0, -1), bz1, (bx1 + bx2) // 2)):
            ZB.boog(W, kk, cc2, uu + 0.5, yd + 1, 1, 1, diep=1, kader="tegel_turkoois", achter="glas")
        doos(W, bx1, yd + 5, bz1, bx2, yd + 5, bz2, "goud")
        kd = ["koepel_tegel", "koepel_roze", "koepel_goud", "koepel_lavendel", "koepel_tegel"][rng.randint(5)]
        r = max(1.8, min(bx2 - bx1, bz2 - bz1) / 2 + 0.2)
        ZB.koepel(W, (bx1 + bx2) / 2 + 0.5, yd + 6, (bz1 + bz2) / 2 + 0.5, r, k_dak=kd, vorm=["ui", "rond", "spits"][rng.randint(3)],
                  ribben=6, oortjes=rng.rand() < 0.7, dik=1.2)
    elif keus < 0.7:
        wx, wz = (x2 - 3, z1 + 1) if rng.rand() < 0.5 else (x1 + 1, z2 - 3)
        doos(W, wx, yd + 1, wz, wx + 2, yd + 7, wz + 2, gevel)
        for dy in (4, 5, 6):
            for (ax, az) in ((wx + 1, wz), (wx + 1, wz + 2), (wx, wz + 1), (wx + 2, wz + 1)):
                zet(W, ax, yd + dy, az, b("mashrabiya"))
        doos(W, wx, yd + 8, wz, wx + 2, yd + 8, wz + 2, "goud")
        zet(W, wx + 1, yd + 9, wz + 1, plaat("koepel_tegel"))
    # roof garden: pots, a pergola of date palm wood with blossom, or a silk parasol
    ZB.potten(W, x1 + 1, z1 + 1, x2 - 1, z2 - 1, yd + 1, seed=seed, dicht=0.07)
    if rng.rand() < 0.5:
        for (px, pz) in ((x1 + 1, z1 + 1), (x1 + 1, z2 - 1), (x2 - 1, z1 + 1), (x2 - 1, z2 - 1)):
            if W.wget(px, yd + 1, pz) == LUCHT:
                doos(W, px, yd + 1, pz, px, yd + 2, pz, "palm_hek")
                zet(W, px, yd + 3, pz, "palm_stam")
        for x in range(x1 + 1, x2):
            for z in range(z1 + 1, z2):
                if (x + z) % 2 == 0 and W.wget(x, yd + 3, z) == LUCHT and W.wget(x, yd + 2, z) == LUCHT:
                    zet(W, x, yd + 3, z, "minecraft:flowering_azalea_leaves" if (x * 3 + z) % 4 else "guhs:guhbloesem_leaves", keep=True)
    # hanging greenery from the roof edge
    if rng.rand() < 0.35:
        for u in range(u1 + 1, u2, 2):
            if rng.rand() < 0.5:
                ZB.hangende_tuin(W, *((c + s * (cf + 1), yd - 1, u) if dx else (u, yd - 1, c + s * (cf + 1))), lengte=rng.randint(2, 5), seed=u)
    # inside
    deur_vak = (c + s * 1, y0, um) if dx else (um, y0, c + s * 1)
    deur_p = (deur_vak[0], y0, deur_vak[2]) if dx == 0 else (deur_vak[0], y0, deur_vak[2])
    if open_:
        leeg(W, x1 + 1, y0, z1 + 1, x2 - 1, y0 + 3, z2 - 1)
        dpi = (c - s * 2, um) if dx else (um, c - s * 2)
        binnen = interieur(W, x1 + 1, z1 + 1, x2 - 1, z2 - 1, y0, 4, rol=rol or "woning", seed=seed,
                           deur_p=(dpi[0], y0, dpi[1]) if dx else (dpi[0], y0, dpi[1]))
        # keep the 2 cells behind the door free
        for k in (2, 3):
            if dx:
                leeg(W, c - s * k, y0, um, c - s * k, y0 + 1, um)
            else:
                leeg(W, um, y0, c - s * k, um, y0 + 1, um)
        if hid:
            dpos = (c - s * 1, y0, um) if dx else (um, y0, c - s * 1)
            ZB.huis_rol(W, hid, rol or "woning", dpos, binnen)
    return (c + s, y0, um) if dx else (um, y0, c + s)
