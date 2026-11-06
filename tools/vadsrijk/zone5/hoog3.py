"""[U-Z5] Round U: depth for the rest of Hoogvads (after bouw5's game parts, so doors/stairs/rooms stay).

  universiteit   stepped buttresses with pinnacles on the eaves, gothic dormer gables, string-course mouldings, a deep
                 portal + corbelled corner turrets on the gate tower, crockets on the corner spires; the library gets a
                 gallery (with ladders) on both long walls
  paleis         pinnacles on the balustrade, string-course mouldings, gargoyles, crockets on the corner towers
  raadszaal      pilasters with gold capitals inside, a coffered gold grid under the ceiling
  academie       machicolations (corbelled parapets) on the outer walls and the keep
  station        corbelled buttresses with pinnacles between the arches, frames round the rose windows
  vensterkappen  city-wide: a hood moulding over every window and a sill under every upper window (houses too)
Local kit coordinates (like bouw5). Only builds in air unless noted.
"""
import math

import numpy as np

import palet
from palet import P, trap, plaat
import bouwhulp as BH
import gotiek as G

LUCHT = "minecraft:air"


def _l(W, x, y, z):
    return G.lucht(W, x, y, z)


# =============================================================================================== university
def universiteit(W, S, info):
    y0, xa, xb, za, zb, gx = 234, -76, 6, -80, -30, -35
    vrij = [gx, -62, -14, -10]          # gate + oriels (bouw5.erker)
    gevels = [(G.Vlak("z", za, -1), range(xa + 8, xb - 3, 8), "noord"), (G.Vlak("z", zb, 1), range(xa + 8, xb - 3, 8), "zuid"),
              (G.Vlak("x", xa, -1), range(za + 8, zb - 3, 8), "west"), (G.Vlak("x", xb, 1), range(za + 8, zb - 3, 8), "oost")]
    def sterrenwacht(vl, u):
        return vl.as_ == "z" and vl.o < 0 and u >= -8      # the observatory tower (x -3..7) stands in the north-east corner
    for (vl, us, kant) in gevels:
        lo, hi = (xa, xb) if vl.as_ == "z" else (za, zb)
        # bouw5's flat pilasters every 4: the ones between the buttresses (every 8) go, so each bay reads as one bay
        for u in range(lo + 4, hi - 1, 8):
            if vl.as_ == "z" and (abs(u - gx) <= 8 or any(abs(u - e) <= 3 for e in vrij[1:])):
                continue
            if kant == "oost" and u <= -70 or sterrenwacht(vl, u):
                continue
            for y in range(y0 + 1, y0 + 21):
                if vl.get(W, u, y, 1).startswith(("guhs:wolkmarmer", "guhs:pastelbaksteen_wolkje")):
                    vl.zet(W, u, y, 1, LUCHT)
            if vl.get(W, u, y0 + 1, 2).startswith("guhs:pastelbaksteen_wolkje"):
                vl.zet(W, u, y0 + 1, 2, LUCHT)
        for u in us:
            if vl.as_ == "z" and any(abs(u - v) <= (10 if v == gx else 4) for v in vrij):
                continue
            if kant == "oost" and u <= -70 or sterrenwacht(vl, u):
                continue              # the observatory corner
            # stepped buttress (over bouw5's flat pilaster), pinnacle on the eaves
            G.steunbeer(W, vl, u, y0 + 1, y0 + 20, diep=2, breed=0, pinakel_h=7)
        # tall gothic lancets (two storeys) in the bays between the buttresses: the small upper windows, flat pilasters,
        # sills and hoods of that bay go; a gold-framed pointed window with tracery and a projecting frame comes
        bays = []
        for uc in range(lo + 12, hi - 3, 8):
            if vl.as_ == "z" and any(abs(uc - v) <= (8 if v == gx else 5) for v in vrij):
                continue
            if kant == "oost" and uc <= -66 or sterrenwacht(vl, uc):
                continue
            bays.append(uc)
            muur = "guhs:pastelbaksteen_roze"
            for u in range(uc - 3, uc + 4):
                for y in range(y0 + 8, y0 + 20):
                    vl.zet(W, u, y, 0, muur)
                    vl.zet(W, u, y, 1, LUCHT)
            BH.boograam(W, vl.as_, vl.c, uc + 0.5, y0 + 9, 5, 6, glas="glas", kader="goud", spits=1.3, diep=1)
            G.omlijsting(W, vl, uc + 0.5, y0 + 9, 5, 6, lagen=1, spits=1.3)
            G.maaswerk(W, vl, uc + 0.5, y0 + 9, 5, 6, spits=1.3)
        # string-course mouldings on the white bands (the floors), outside, where there is air (not across the lancets)
        for y in (y0 + 8, y0 + 15):
            for u in range(lo, hi + 1):
                if y > y0 + 8 and any(abs(u + 0.5 - (b + 1.0)) <= 3.6 for b in bays):
                    continue
                vl.zet_l(W, u, y, 1, G.trede("wit_trap", vl.naar_muur, "top"))
        # gothic dormer gables between the buttresses (long facades)
        if vl.as_ == "z":
            for u in range(xa + 4, xb - 3, 8):
                if any(abs(u - v) <= 5 for v in vrij) or sterrenwacht(vl, u):
                    continue
                dakkapel(W, vl, u, y0 + 21)
    # crockets on the corner spires (BH.ronde_toren r 4 -> spits r0 5.5, 14 high, on y0 + 29)
    for (cx, cz) in ((xa, za), (xa, zb)):
        G.hogels(W, cx, cz, y0 + 29, 5.5, 14, hoeken=8, elke=3, start=2)
    # gate tower: corbelled corner turrets (bartizans) with slender spires
    for s in (-1, 1):
        tx, tz = gx + s * 9, zb + 2
        for y in range(y0 + 20, y0 + 36):
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    G.zet_lucht(W, tx + dx, y, tz + dz, P["marmer_glad"] if (y - y0) % 5 else P["goud"])
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                G.zet_lucht(W, tx + dx, y0 + 19, tz + dz, G.trede("wit_trap", "north", "top"))
        G.zet_lucht(W, tx, y0 + 18, tz, G.trede("wit_trap", "north", "top"))
        G.zet_lucht(W, tx, y0 + 28, tz + 1, P["raam"])
        G.slanke_spits(W, tx, tz, y0 + 36, 10, r0=1.8, dak="dak_roze", hoeken=8)
    # a deep portal in front of the gate (2 layers, the stair of bouw5.poorttrap goes through it)
    vg = G.Vlak("z", zb + 2, 1)
    G.portaal(W, vg, gx + 0.5, y0 + 1, 7, 9, lagen=2, gevel=False, pinakels=True)
    # ---- library gallery (north wing x -62..-36, z -79..-69, two storeys): walkways at y0+6 along both shelf walls, railings,
    #      a ladder up at the west end of each, lanterns
    yg = y0 + 6
    for (planken, rail, ladder_z, f) in (((-78, -77), -76, -78, "south"), ((-70, -71), -72, -70, "north")):
        for x in range(-59, -36):
            for zz in planken:
                BH.zet(W, x, yg, zz, "minecraft:dark_oak_planks")
            BH.zet(W, x, yg + 1, rail, "minecraft:dark_oak_fence")
            G.zet_lucht(W, x, yg - 1, rail, "minecraft:dark_oak_slab[type=top,waterlogged=false]")
        for y in range(y0 + 1, yg + 2):
            BH.zet(W, -60, y, ladder_z, f"minecraft:ladder[facing={f},waterlogged=false]")
        for x in range(-57, -37, 6):
            BH.zet(W, x, yg + 1, planken[0], "minecraft:lantern[hanging=false,waterlogged=false]")
    info["plekken"]["universiteit_bibliotheek_galerij"] = (-50, yg + 1, -77)


def dakkapel(W, vl, u, y):
    """A gothic dormer gable standing on the eaves of a facade (2 thick, gold coping with crockets, a little round window)."""
    for k in range(6):
        hw = 3 - k * 0.5
        for du in np.arange(-hw, hw + 0.01, 1.0):
            for d in (0, -1):
                vl.zet(W, u + du, y + k, d, P["marmer_glad"])
            for d in (-2, -3):
                vl.zet_l(W, u + du, y + k, d, P["marmer"])
        for s in (-1, 1):
            vl.zet(W, u + s * hw, y + k, 0, P["goud"])
            if k % 2 == 1:
                vl.zet_l(W, u + s * (hw + 1), y + k, 0, G.trede("goud_trap", vl.langs[1] if s > 0 else vl.langs[0]))
    vl.zet(W, u, y + 1, 0, P["glas_hart"])
    vl.zet(W, u, y + 2, 0, P["glas_hart"])
    x, z = vl.p(u, 0)
    G.pinakel(W, x, y + 6, z, h=3, staaf=False)


# =============================================================================================== palace + council hall
def paleis(W, S, info):
    y0, ma, mb = 252, -152, -122
    vo = G.Vlak("x", mb, 1)
    # pinnacles on the balustrade above every pilaster of the front, gargoyles under the cornice
    for z in range(-20, 21, 5):
        G.pinakel(W, mb + 1, y0 + 37, z, h=6)
        G.gargouille(W, G.Vlak("x", mb + 1, 1), z, y0 + 33)
    # string-course mouldings under the white bands, front + sides
    for y in (y0 + 11, y0 + 22, y0 + 33):
        for z in range(-23, 24):
            G.zet_lucht(W, mb + 1, y, z, G.trede("wit_trap", "west", "top"))
    # crockets on the four round corner towers (ronde_toren r 6 -> spits r0 7.5, 22 high on y0+37) + pinnacles on the wing roofs
    for s in (-1, 1):
        for xt in (ma + 2, mb - 2):
            G.hogels(W, xt, s * 44, y0 + 37, 7.5, 22, hoeken=8, elke=3, start=2)
        for x in range(ma + 5, mb - 4, 5):
            G.pinakel(W, x, y0 + 27, s * 45, h=5, staaf=False)


def raadszaal(W, S, info):
    y = 256
    # pilasters with gold capitals along the long inner walls (z +-16), a gold coffered grid under the ceiling
    for x in range(-105, -89, 4):
        for s in (-1, 1):
            if s < 0 and x <= -107:
                continue
            for yy in range(y + 1, y + 17):
                G.zet_lucht(W, x, yy, s * 16, G.muurtje("wit_muur"))
            G.zet_lucht(W, x, y + 17, s * 16, G.trede("goud_trap", "south" if s > 0 else "north", "top"))
    for x in range(-112, -89):
        for z in range(-16, 17):
            if (x % 4 == 1 or z % 4 == 0) and _l(W, x, y + 19, z) and not _l(W, x, y + 20, z):
                BH.zet(W, x, y + 19, z, plaat("goud_plaat", "top"))


# =============================================================================================== academy
def machicolatie(W, vl, u1, u2, y, vrij=()):
    """A corbelled parapet: a row of upside-down stairs at d=1 under y, the parapet (white) at y and y+1 every other block."""
    for u in range(int(u1), int(u2) + 1):
        if any(abs(u - v) <= 7 for v in vrij):
            continue
        vl.zet_l(W, u, y - 1, 1, G.trede("steen_trap", vl.naar_muur, "top"))
        vl.zet_l(W, u, y, 1, P["wit_glad"])
        if u % 2 == 0:
            vl.zet_l(W, u, y + 1, 1, P["wit_glad"])


def academie(W, S, info):
    y0, xa, xb, za, zb, gx = 234, -76, 6, 30, 80, -35
    machicolatie(W, G.Vlak("z", za, -1), xa, xb, y0 + 13, vrij=(gx,))
    machicolatie(W, G.Vlak("z", zb, 1), xa, xb, y0 + 13)
    machicolatie(W, G.Vlak("x", xa, -1), za, zb, y0 + 13)
    machicolatie(W, G.Vlak("x", xb, 1), za, zb, y0 + 13)
    # the keep (x gx-14..gx+14, z zb-14..zb-2, to y0+30): machicolations on the yard side, pinnacles on its corners
    machicolatie(W, G.Vlak("z", zb - 15, -1), gx - 14, gx + 10, y0 + 30)
    G.pinakel(W, gx - 14, y0 + 31, zb - 15, h=6)      # (the east corner is the ladder tower: academie_toren)


# =============================================================================================== station
def station(W, S, info):
    import kathedraal3 as K3
    y0, ha, hb, zn, zz = 236, 40, 94, -78, -58
    for (vl, xs) in ((G.Vlak("z", zn, -1), range(ha + 3, hb, 6)), (G.Vlak("z", zz, 1), range(ha + 3, hb, 6))):
        for x in xs:
            # corbelled buttress (starts above head height: the walk along the hall stays free)
            for d in (1, 2):
                vl.zet_l(W, x, y0 + 3, d, G.trede("wit_trap", vl.naar_muur, "top"))
            for y in range(y0 + 4, y0 + 10):
                vl.zet_l(W, x, y, 1, P["wit_glad"])
                if y < y0 + 7:
                    vl.zet_l(W, x, y, 2, P["wit_glad"])
            vl.zet_l(W, x, y0 + 7, 2, G.trede("wit_trap", vl.naar_muur))
            xx, zz_ = vl.p(x, 1)
            top = y0 + 10
            while not _l(W, xx, top, zz_) and top < y0 + 16:
                top += 1
            G.pinakel(W, xx, top, zz_, h=5)
    cz = (zn - 2 + zz + 2) / 2.0
    for (x, o) in ((ha, -1), (hb, 1)):
        K3.roos_omlijsting(W, G.Vlak("x", x, o), cz + 0.5, y0 + 12, 5)


# =============================================================================================== window hoods (city-wide)
def _schuif(a, dx, dy, dz, vul=False):
    """out[x, y, z] = a[x + dx, y + dy, z + dz] (vul outside)."""
    out = np.full_like(a, vul)
    X, Y, Z = a.shape

    def sl(d, n):
        return (slice(max(0, -d), n - max(0, d)), slice(max(0, d), n - max(0, -d)))
    (dxa, sxa), (dya, sya), (dza, sza) = sl(dx, X), sl(dy, Y), sl(dz, Z)
    out[dxa, dya, dza] = a[sxa, sya, sza]
    return out


def _in_huis(x, y, z):
    import bouw5
    for h in bouw5.HUIZEN:
        if h["x1"] - 0 <= x <= h["x2"] and h["z1"] <= z <= h["z2"] and h["y0"] <= y <= h["y_top"] + 2:
            return True
    return False


def vensterkappen(W, log=print):
    """A hood moulding (upside-down white stair, against the wall) over the top of every window that looks out into the open
    (the window's outside cell is air and the wall block above the window is solid), widened one block to each side where the
    wall goes on; a sill (top slab) under the lowest glass when it hangs (air under it). Works on the whole world (houses too)."""
    import vibekit as V
    bk = V.res()[1]
    v = W.v
    pal = W.pal
    n = len(pal)
    glas = np.zeros(n, bool)
    vast = np.zeros(n, bool)
    lu = np.zeros(n, bool)
    for i, s in enumerate(pal):
        nm = s.split("[")[0]
        if nm in (LUCHT, "minecraft:cave_air"):
            lu[i] = True
            continue
        if ("glas" in nm or "glass" in nm) and "pane" not in nm:
            glas[i] = True
            continue
        if "leaves" in nm:
            continue
        try:
            vast[i] = bool(bk.bake(s).full_opaque)
        except Exception:
            vast[i] = False
    lu[0] = True
    g, vb, la = glas[v], vast[v], lu[v]
    x1, y1, z1 = (int(c) for c in W.b[:3])
    X, Y, Z = v.shape
    n_kap = n_bank = 0
    boven_vast = _schuif(vb, 0, 1, 0)
    for (dx, dz, f) in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
        kap = BH.b(G.trede("wit_trap", f, "top"))
        bank = BH.b(plaat("wit_plaat", "top"))
        m = g & boven_vast & _schuif(la, dx, 0, dz) & _schuif(la, dx, 1, dz)
        zij = [(dz, dx), (-dz, -dx)]
        for (x, y, z) in np.argwhere(m):
            ox, oz = x + dx, z + dz
            if _in_huis(ox + x1, y + 1 + y1, oz + z1):
                continue      # a room of a house (its stair to the upper floor runs along the walls)
            W.wset(ox + x1, y + 1 + y1, oz + z1, kap)
            n_kap += 1
            for (sx, sz) in zij:
                px, pz = ox + sx, oz + sz
                if 0 <= px < X and 0 <= pz < Z and la[px, y + 1, pz] and vb[px - dx, y + 1, pz - dz]:
                    W.wset(px + x1, y + 1 + y1, pz + z1, kap)
                    n_kap += 1
        # sills: under the lowest glass of a window, when the spot under the window outside hangs in the air
        mb = g & ~_schuif(g, 0, -1, 0) & _schuif(vb, 0, -1, 0) & _schuif(la, dx, 0, dz) & _schuif(la, dx, -1, dz) &             _schuif(la, dx, -2, dz) & _schuif(la, dx, -3, dz)
        for (x, y, z) in np.argwhere(mb):
            if _in_huis(x + dx + x1, y - 1 + y1, z + dz + z1):
                continue
            W.wset(x + dx + x1, y - 1 + y1, z + dz + z1, bank)
            n_bank += 1
    log(f"vensterkappen {n_kap}, vensterbanken {n_bank}")


# =============================================================================================== all
def bouw(W, S, info, log=print):
    universiteit(W, S, info)
    paleis(W, S, info)
    raadszaal(W, S, info)
    academie(W, S, info)
    station(W, S, info)
    vensterkappen(W, log=log)
    log("hoog3 (U-Z5): diepte op universiteit, paleis, raadszaal, academie, station + vensterkappen")
