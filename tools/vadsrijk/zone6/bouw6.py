"""Zone 6 "Het Zijdeland": the GAME parts of the open land (U-Z6WERELD), on top of the kit (land, cities, landmarks).

Order (maak6.bouw_wereld): land -> wegen (walkable roads, before the cities) -> Rozenkloof -> Saffraanpoort -> landmarks()
(beacons, tower, dungeon entrance, the Zingende Zoutgrot, camps, oasis, ruins, waterfalls, viewpoints, secrets, caravans)
-> groen -> figures.

Everything is walkable in survival (T_BRIEF): roads are cut/filled with steps of at most 1 (stairs on every rise) and 1:1
shoulders, every POI has a stand spot, water has a way out, the cave has real floors.
"""
import math

import numpy as np

import zijbouw as ZB
import zijwereld as ZW
from zijbouw import zet, doos, leeg, rng_van, lantaarn, plek
from zpalet import WOL, TAPIJT, b, trap, plaat

LUCHT = "minecraft:air"
KAMPVUUR = "minecraft:campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]"   # unlit = kern checkpoint
_ZACHT = ("leaves", "flower", "tulip", "grass", "bush", "fern", "allium", "peony", "lily", "carpet", "sapling", "rose", "orchid",
          "dandelion", "poppy", "daisy", "cornflower", "bluet", "kristalletjes", "torch", "lantern", "snow")


# ------------------------------------------------------------------------------------------------ ground helpers
class Grond:
    """Solid-block lookup per palette id (for 'what is the real ground here' after buildings)."""

    def __init__(self, W):
        self.W = W
        self.c = {}

    def vast(self, i):
        if i not in self.c:
            s = self.W.pal[i]
            n = s.split("[")[0]
            self.c[i] = not (i == 0 or n in ("minecraft:air", "minecraft:cave_air") or "water" in n or any(k in n for k in _ZACHT))
        return self.c[i]

    def top(self, x, z, ymax=None):
        """y of the highest solid block in column (x, z) (below ymax), or None."""
        W = self.W
        i, k = W.vx(x), W.vz(z)
        if not (0 <= i < W.v.shape[0] and 0 <= k < W.v.shape[2]):
            return None
        col = W.v[i, :, k]
        hi = W.v.shape[1] - 1 if ymax is None else min(W.v.shape[1] - 1, W.vy(ymax))
        for j in range(hi, -1, -1):
            if col[j] and self.vast(int(col[j])):
                return int(W.b[1] + j)
        return None


def _G(W):
    if not hasattr(W, "_grond"):
        W._grond = Grond(W)
    return W._grond


def grond(W, x, z, ymax=None):
    t = _G(W).top(int(math.floor(x)), int(math.floor(z)), ymax)
    return ZW.hoogte(W, x, z) - 1 if t is None else t


def stand(W, x, z, ymax=None):
    return grond(W, x, z, ymax) + 1


def zet_h(W, x, z, top):
    """Keep the land field in step with a changed column (later builders read ZW.hoogte)."""
    i, j = W.vx(x), W.vz(z)
    if 0 <= i < W.H.shape[0] and 0 <= j < W.H.shape[1]:
        W.H[i, j] = top + 0.01


# ------------------------------------------------------------------------------------------------ roads
def _herbemonster(pts, stap=1.0):
    P = np.asarray(pts, float)
    uit = [P[0]]
    for a, c in zip(P[:-1], P[1:]):
        L = float(np.hypot(*(c - a)))
        n = max(1, int(math.ceil(L / stap)))
        for k in range(1, n + 1):
            uit.append(a + (c - a) * k / n)
    return np.array(uit)


def pad(W, pts, breed=2, top_k="vloer_zand", rand_k="zs", trap_k="vloer_zand", schouder=4, y_begin=None, y_eind=None,
        naam="weg", lampen=24, hek=False):
    """A walkable road along the polyline pts [(x, z), ...]: the ground is cut/filled to a smoothed profile with a rise of at
    most 1 per block (stairs on every rise), 4 blocks of air above, 1:1 shoulders (no cliff next to the road), lanterns on
    posts every `lampen` blocks. Returns the profile [(x, z, y_top)]."""
    P = _herbemonster(pts, 1.0)
    N = len(P)
    h = np.array([grond(W, x, z, 200) for x, z in P], float)
    if N > 9:
        k = np.ones(9) / 9
        hs = np.convolve(np.pad(h, 4, mode="edge"), k, mode="valid")
    else:
        hs = h.copy()
    t = np.round(hs)
    t[0] = y_begin - 1 if y_begin is not None else t[0]
    t[-1] = y_eind - 1 if y_eind is not None else t[-1]
    i = np.arange(N)
    lo = np.maximum(t[0] - i, t[-1] - (N - 1 - i))
    hi = np.minimum(t[0] + i, t[-1] + (N - 1 - i))
    t = np.clip(t, lo, hi)
    for a in range(1, N):
        t[a] = np.clip(t[a], t[a - 1] - 1, t[a - 1] + 1)
    for a in range(N - 2, -1, -1):
        t[a] = np.clip(t[a], t[a + 1] - 1, t[a + 1] + 1)
    if abs(t[0] - (y_begin - 1 if y_begin is not None else t[0])) > 0 or abs(t[-1] - (y_eind - 1 if y_eind is not None else t[-1])) > 0:
        print(f"  [weg {naam}] te steil: begin/eind niet haalbaar ({t[0]}, {t[-1]})", flush=True)
    t = t.astype(int)
    cel = {}
    rich = {}
    for a in range(N):
        x, z = P[a]
        d = P[min(a + 1, N - 1)] - P[max(a - 1, 0)]
        d = d / max(1e-6, np.hypot(*d))
        nx, nz = -d[1], d[0]
        for o in np.arange(-breed - schouder, breed + schouder + 0.01, 0.5):
            c = (int(math.floor(x + nx * o)), int(math.floor(z + nz * o)))
            ao = abs(o)
            if c not in cel or ao < cel[c][1]:
                cel[c] = (t[a], ao, a)
        rich[a] = d
    rng = rng_van(P[0][0], P[0][1], 31)
    for (cx, cz), (y, ao, a) in cel.items():
        g = grond(W, cx, cz, 200)
        if ao <= breed + 0.25:
            # the road bed
            onder = min(g, y - 1)
            doos(W, cx, onder, cz, cx, y - 1, cz, "rots")
            k = top_k if ao < breed - 0.6 else rand_k
            zet(W, cx, y, cz, k)
            leeg(W, cx, y + 1, cz, cx, y + 4, cz)
            zet_h(W, cx, cz, y)
        else:
            # shoulder: 1:1 slope from the road edge to the terrain
            dd = int(math.ceil(ao - breed))
            if g > y + dd:
                leeg(W, cx, y + dd + 1, cz, cx, g, cz)
                zet(W, cx, y + dd, cz, "zand")
                zet_h(W, cx, cz, y + dd)
            elif g < y - dd:
                doos(W, cx, g, cz, cx, y - dd, cz, "rots")
                zet(W, cx, y - dd, cz, "zand")
                zet_h(W, cx, cz, y - dd)
    # stairs on every rise (the higher cell becomes a stair facing uphill), done after the bed so they are not overwritten
    for (cx, cz), (y, ao, a) in cel.items():
        if ao > breed + 0.25:
            continue
        for a2 in (a - 1, a + 1):
            if 0 <= a2 < N and t[a2] == y - 1:
                d = P[a] - P[a2]
                if abs(d[0]) >= abs(d[1]):
                    f = "east" if d[0] > 0 else "west"
                else:
                    f = "south" if d[1] > 0 else "north"
                zet(W, cx, y, cz, trap(trap_k, f))
                break
    # lanterns on posts along the side (never on the road)
    for a in range(lampen // 2, N, lampen):
        x, z = P[a]
        d = rich[a]
        nx, nz = -d[1], d[0]
        side = 1 if (a // lampen) % 2 else -1
        px, pz = x + nx * (breed + 1.5) * side, z + nz * (breed + 1.5) * side
        y = grond(W, px, pz, 200) + 1
        if abs(y - 1 - t[a]) <= 2:
            doos(W, px, y, pz, px, y + 2, pz, "palm_hek")
            zet(W, px, y + 3, pz, "messing_lantaarn")
    return [(float(P[a][0]), float(P[a][1]), int(t[a])) for a in range(N)]


def wegen(W):
    """The caravan roads (before the cities, which build over their own ends)."""
    import saffraanpoort as SP
    wp = getattr(SP, "WESTPOORT", (3640, -806))
    zp = getattr(SP, "ZUIDPOORT", (3758, -742))
    op = getattr(SP, "OOSTPOORT", (3882, -884))
    Y = SP.Y
    W.wegen = {}
    # 1. Siq top -> Westpoort (the caravan road of hq02/hq03)
    W.wegen["karavaanweg"] = pad(W, [(3566, -794), (3586, -802), (3600, -806), (3616, -806), (wp[0] - 10, wp[1])], breed=2,
                                  y_eind=Y, naam="karavaanweg")
    # 2. Zuidpoort -> the Duinroute south (Duinbaken, Wormelien)
    W.wegen["duinroute"] = pad(W, [(zp[0], zp[1] + 8), (3750, -712), (3738, -672), (3726, -625), (3716, -580), (3708, -530),
                                   (3700, -478)], breed=2, y_begin=Y, naam="duinroute")
    # 2b. branch to the Duinbaken + the Duinput
    W.wegen["duinbaken"] = pad(W, [(3716, -580), (3690, -570), (3668, -566), (3655, -547)], breed=1, naam="duinbaken")
    W.wegen["duinput"] = pad(W, [(3738, -672), (3700, -676), (3660, -674), (3624, -672)], breed=1, naam="duinput")
    # 3. Zuidpoort -> Zoutvlakte -> Moederoase (the Oostroute)
    W.wegen["zoutweg"] = pad(W, [(zp[0] + 4, zp[1] + 8), (3800, -724), (3850, -708), (3900, -690), (3940, -660), (3990, -640),
                                 (4040, -670), (4076, -706), (4090, -730)], breed=2, y_begin=Y, naam="zoutweg")
    W.wegen["zoutbaken"] = pad(W, [(3940, -660), (3934, -632), (3930, -603)], breed=1, naam="zoutbaken")
    W.wegen["oostroute"] = pad(W, [(4090, -730), (4130, -740), (4160, -736), (4190, -730)], breed=2, naam="oostroute")
    # 4. Oostpoort -> Ruinedal (dungeon) and the oasis from the north
    #    [u-z6afronding] both roads go AROUND the rock mesa east of the gate (x 3896..3956, z -930..-882); they used to climb
    #    over it in a stair tunnel through the rock up to y ~101
    W.wegen["oaseweg"] = pad(W, [(op[0] + 12, op[1] - 4), (3890, -878), (3902, -870), (3940, -868), (4000, -860), (4040, -846),
                                 (4058, -830)], breed=2, y_begin=Y, naam="oaseweg")
    W.wegen["ruineweg"] = pad(W, [(3940, -868), (3966, -878), (3968, -910), (3970, -935), (3985, -946)], breed=2,
                              naam="ruineweg")
    # 5. Westpoort -> along the river into the Palmenkloven; the climb to the plateau is the waterfall stair (palmenkloven())
    W.wegen["rivierpad"] = pad(W, [(wp[0] - 6, wp[1] - 18), (3622, -860), (3600, -900), (3576, -926), (3552, -946),
                                   (3530, -958), (3500, -962), (3470, -966)], breed=1, naam="rivierpad")
    # 6. Siq top -> Palmbaken across the plateau
    W.wegen["plateaupad"] = pad(W, [(3566, -800), (3540, -830), (3510, -860), (3490, -888), (3480, -893)], breed=1,
                                naam="plateaupad")
    # 7. the toren rock + the Ruinebaken mesa ramp
    W.wegen["torenpad"] = pad(W, [(4090, -730), (4100, -712), (4108, -698), (4112, -690)], breed=2, naam="torenpad")
    W.wegen["ruinebaken"] = pad(W, [(3985, -946), (4010, -960), (4030, -975), (4040, -985), (4045, -987)], breed=1,
                                naam="ruinebaken")


# ------------------------------------------------------------------------------------------------ small pieces
def kampvuur(W, pid, x, z, banken=True):
    # [u-z6afronding] never on a tent roof or a palm crown (the karavaankamp fire sat on a tent, the Moederoase fire on a
    #   palm): if the column is not the land itself, take the nearest free ground column within 8
    def op_grond(px, pz):
        yy = stand(W, px, pz)
        return abs(yy - int(math.floor(ZW.hoogte(W, px, pz)))) <= 1 and all(
            W.wget(px, yy + q, pz) == LUCHT for q in range(0, 3))
    if not op_grond(x, z):
        for r in range(1, 9):
            ring = [(x + dx, z + dz) for dx in range(-r, r + 1) for dz in range(-r, r + 1) if max(abs(dx), abs(dz)) == r]
            vrij = [p for p in ring if op_grond(*p)]
            if vrij:
                x, z = vrij[0]
                break
    y = stand(W, x, z)
    zet(W, x, y - 1, z, "vloer_zand")
    zet(W, x, y, z, KAMPVUUR)
    if banken:
        for (dx, dz) in ((2, 0), (-2, 0), (0, 2)):
            if abs(grond(W, x + dx, z + dz) + 1 - y) <= 1:
                zet(W, x + dx, stand(W, x + dx, z + dz), z + dz, plaat("palm_planken"))
    plek(W, pid, x, y, z - 2)


def uitkijk(W, pid, x, z, k="zs"):
    """A viewpoint: a small paved platform with a bench + a brass lantern (and a stair if it stands proud)."""
    y = stand(W, x, z)
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) + abs(dz) <= 3:
                g = grond(W, x + dx, z + dz)
                doos(W, x + dx, min(g, y - 1), z + dz, x + dx, y - 1, z + dz, k)
                leeg(W, x + dx, y, z + dz, x + dx, y + 3, z + dz)
                zet_h(W, x + dx, z + dz, y - 1)    # [u-z6afronding] else groen() put a flower 1 above the platform (floating)
    zet(W, x - 1, y, z - 2, trap("palm_planken", "north"))
    zet(W, x + 1, y, z - 2, trap("palm_planken", "north"))
    doos(W, x + 2, y, z + 2, x + 2, y + 1, z + 2, "palm_hek")
    zet(W, x + 2, y + 2, z + 2, "messing_lantaarn")
    plek(W, pid, x, y, z)


def geheim(W, pid, x, z, seed=0):
    """A small secret nook: a ring of rocks/bushes with a hollow and a flower pot, the spot in the middle."""
    rng = rng_van(x, z, seed)
    y = stand(W, x, z)
    for k in range(10):
        a = k * math.pi / 5
        px, pz = x + round(math.cos(a) * 2.6), z + round(math.sin(a) * 2.6)
        if k == 3:
            continue          # the way in
        h = 1 + int(rng.randint(0, 3))
        doos(W, px, stand(W, px, pz), pz, px, stand(W, px, pz) + h - 1, pz, "zs" if rng.rand() < 0.6 else "rots")
    leeg(W, x - 1, y, z - 1, x + 1, y + 2, z + 1)
    zet(W, x + 1, y, z + 1, "guhs:potted_roze_hibiscus")
    plek(W, pid, x, y, z)


def stal_rijdieren(W, x, z, seed=0, yaw=90):
    """A small caravan stop: the 3 mounts (bultguh, flying carpet, pluimguh) at a hitching rail with a trough."""
    import zijfiguren as ZF
    y = stand(W, x, z)
    for dx in range(-5, 6):
        zet(W, x + dx, stand(W, x + dx, z - 2), z - 2, "palm_hek")
    zet(W, x - 1, stand(W, x - 1, z - 3), z - 3, "minecraft:water_cauldron[level=3]")
    zet(W, x + 1, stand(W, x + 1, z - 3), z - 3, "minecraft:hay_block[axis=y]")
    ZF.zet_bultguh(W, x - 3.5, stand(W, x - 3, z), z + 0.5, yaw, variant=["zandroze", "saffraan", "kaneel", "wolkwit"][seed % 4],
                   deken=seed, ruiter=False)
    ZF.zet_tapijt(W, x + 0.5, stand(W, x, z + 1), z + 1.5, yaw + 20, patroon=seed, ruiter=False, hoogte=0.9)
    ZF.zet_pluimguh(W, x + 3.5, stand(W, x + 3, z), z + 0.5, yaw - 30, ["roze", "mint", "honing", "lavendel", "goud"][seed % 5])


# ------------------------------------------------------------------------------------------------ the landmarks
def bakens(W):
    """The five star beacons, all DARK in the game (the story lights them: u-z6verhaal swaps the star at <baken>_ster)."""
    import landschap as LS
    for naam, (x, z, _aan) in LS.BAKENS.items():
        W.baken = None
        LS.sterrenbaken(W, x, z, aan=False, naam=None)
        if W.baken:
            bx, by, bz = W.baken
            plek(W, naam + "_ster", bx, by, bz)
        y0 = ZW.hoogte(W, x, z)
        # the plinth has 3 steps of 1: the stand spot in front (south) on the second step, the puzzle centre east
        plek(W, naam, x, y0 + 2, z + 8)
        plek(W, naam + "_voet", x, None, z + 14)
        plek(W, naam + "_puzzel", x + 18, None, z)
        # a campfire at every beacon (checkpoint)
        kampvuur(W, "kampvuur_" + naam, x - 13, z + 12)


def toren(W):
    """De Sterrendovende Toren (exterior; the inside is the raid instance of u-z6raid). The gate (iwan, west) is sealed with
    Ringeling's ring lock: a gold ring in front of a dark doorway; the stand spot is in front of it."""
    import landschap as LS
    LS.sterrendovende_toren(W)
    tx, tz = ZW.TOREN
    y0 = ZW.hoogte(W, tx, tz)
    gx = tx - 21
    # stair from the rock to the gate threshold (y0 + 1), the doorway itself stays open air 2 deep, then a dark "seal"
    # [u-z6afronding] real stairs (a tread per block, facing the gate) all the way down to the rock/ground: the old 6 full
    #   blocks were 1-block jumps and stopped ~10 blocks above the ground where the torenpad ends
    for dz in range(-2, 3):
        g = grond(W, gx, tz + dz, y0 + 3)
        doos(W, gx, min(g, y0), tz + dz, gx, y0, tz + dz, "grauw_steen")
        leeg(W, gx, y0 + 1, tz + dz, gx, y0 + 4, tz + dz)
    for i in range(1, 40):
        ys = y0 - i + 1                      # tread block y (its top = the stand level of the cell above)
        laag = True
        for dz in range(-2, 3):
            g = grond(W, gx - i, tz + dz, y0 + 3)
            doos(W, gx - i, min(g, ys - 1), tz + dz, gx - i, ys - 1, tz + dz, "grauw_steen")
            zet(W, gx - i, ys, tz + dz, trap("grauw_steen", "east"))
            leeg(W, gx - i, ys + 1, tz + dz, gx - i, ys + 4, tz + dz)
            laag = laag and g >= ys - 1
        if laag:
            break
    # [u-z6afronding] the doorway between the threshold and the seal had a 1-block hole (x gx+1) down to the rock
    for xx in range(gx + 1, tx - 16):
        for dz in range(-2, 3):
            if W.wget(xx, y0, tz + dz) == "minecraft:air":
                zet(W, xx, y0, tz + dz, "grauw_steen")
    # [u-z6afronding] beside the stair the cut left grass overhangs with a 2-high void under them (loopcheck VAL at
    #   4116 70 -694): fill the voids under the top in the strips next to the stair
    for xx in range(gx - 40, gx + 1):
        for dz in list(range(-8, -2)) + list(range(3, 9)):
            t = None
            for yy in range(y0 + 3, y0 - 30, -1):         # the real top (the cached ground grid is stale here)
                if W.wget(xx, yy, tz + dz) != LUCHT:
                    t = yy
                    break
            if t is None:
                continue
            for yy in range(t - 6, t):
                if W.wget(xx, yy, tz + dz) == "minecraft:air":
                    zet(W, xx, yy, tz + dz, "rots")
    # [x-raid] the raid door. The old "seal" (black glass 5 wide at tx-16 + a gold ring standing edge-on in the walkway) left
    #   2 blocks free on both sides of the 9-wide niche: you walked round it, through the open back-wall door, into the
    #   hollow tower, and nothing happened (the Java door box is only 4 wide). Now: the niche is open and is the door itself
    #   (Java Z6Raid: entering the niche = to De Wachtzaal); the back-wall door is filled with sterrenvlies (purple glass, the
    #   same as the raid's floor gates) inside a gold ring set flush in the wall; a gold circle inlaid in the niche floor.
    # [x-raid] the voet is an octagon with a CORNER on the door axis: that corner stuck out 1 block in front of the iwan's gate
    #   facade (x tx-19, 3 wide, y0+1..y0+14), a pillar right in the middle of the doorway. Cut it back to the facade.
    muur = {W.id(b(k)) for k in ("grauw_steen", "grauw2", "goud")}
    for xx in (tx - 20, tx - 19):
        for dz in range(-8, 9):
            for yy in range(y0 + 1, y0 + 15):
                if W.v[W.vx(xx), W.vy(yy), W.vz(tz + dz)] in muur:
                    zet(W, xx, yy, tz + dz, LUCHT)
    cb = tx - 13
    for dz in range(-5, 6):
        for yy in range(y0 + 1, y0 + 12):
            if W.wget(cb, yy, tz + dz) == LUCHT:
                zet(W, cb, yy, tz + dz, "minecraft:purple_stained_glass")
            elif abs(math.hypot(yy - (y0 + 3.5), dz) - 3.0) < 0.55:
                zet(W, cb, yy, tz + dz, "goudblok")
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if abs(math.hypot(dx, dz) - 2.0) < 0.6:
                zet(W, tx - 15 + dx, y0, tz + dz, "goudblok")
    # (stand spot outside: the threshold at the top of the stair, OUTSIDE the door box, so coming out never bounces you in)
    plek(W, "toren_poort", gx, y0 + 1, tz)
    plek(W, "toren_raid_ingang", tx - 17, y0 + 1, tz)
    plek(W, "toren_voet", gx - 7, None, tz)


def kerker(W):
    """De Verzonken Sterrenwacht (dungeon entrance, Ruinedal): a half-buried observatory dome with a broken copper
    telescope; through the south arch you walk into a lit hall with a real floor and a stair down to the sealed star gate
    (kerker_poort: the instance teleport of the dungeon slice)."""
    x, _, z = ZW.PLEK["verzonken_sterrenwacht"]
    y = ZW.hoogte(W, x, z)
    # dome shell (sunk), solid floor, hall inside
    W.bol((x + 0.5, y - 1, z + 0.5), 12, b("rots_marmer"), ry=10, hollow=1.5)
    doos(W, x - 10, y - 8, z - 10, x + 10, y - 1, z + 10, "rots", keep=False)
    for dx in range(-9, 10):
        for dz in range(-9, 10):
            if math.hypot(dx, dz) < 9.6:
                zet(W, x + dx, y - 1, z + dz, "mozaiek_ster" if (dx + dz) % 4 == 0 else "vloer_dambord")
    # broken top: a gap + the telescope
    W.bol((x + 4.5, y + 9, z - 3.5), 4.5, LUCHT, ry=3)
    for i in range(12):
        zet(W, x + 2 + i * 0.8, y + 4 + i * 0.6, z - 2, "minecraft:copper_block")
        zet(W, x + 2 + i * 0.8, y + 4 + i * 0.6, z - 3, "minecraft:copper_block")
    doos(W, x + 1, y, z - 3, x + 2, y + 3, z - 2, "minecraft:copper_block")
    # the south arch (entrance) + a short sunk ramp
    for i in range(8):
        for dx in range(-2, 3):
            leeg(W, x + dx, y, z + 8 + i, x + dx, y + 5, z + 8 + i)
            zet(W, x + dx, y - 1, z + 8 + i, "vloer_zand")
    for yy in range(y, y + 6):
        zet(W, x - 3, yy, z + 9, "zuil")
        zet(W, x + 3, yy, z + 9, "zuil")
    doos(W, x - 3, y + 6, z + 9, x + 3, y + 6, z + 9, "goud")
    # stair down (north side) to the gate
    # [u-z6afronding] 6 treads y-1..y-6 (was 5 treads y-2..y-6: a 1-block jump at the top and 1.5 at the bottom)
    for i in range(6):
        for dx in range(-1, 2):
            leeg(W, x + dx, y - i, z - 5 - i, x + dx, y + 3 - i, z - 5 - i)
            zet(W, x + dx, y - 1 - i, z - 5 - i, trap("vloer_zand", "south"))
            doos(W, x + dx, y - 6 - i, z - 5 - i, x + dx, y - 2 - i, z - 5 - i, "rots")
    gy = y - 6
    doos(W, x - 3, gy - 1, z - 14, x + 3, gy - 1, z - 10, "vloer_dambord")
    leeg(W, x - 3, gy, z - 14, x + 3, gy + 5, z - 10)
    doos(W, x - 2, gy, z - 15, x + 2, gy + 4, z - 15, "minecraft:purple_stained_glass")
    for k in range(5):
        a = k * 2 * math.pi / 5
        zet(W, x + math.cos(a) * 1.6, gy + 2 + math.sin(a) * 1.6, z - 15, "minecraft:sea_lantern")
    # light: brass lanterns on chains in a ring (round, no grid)
    for k in range(6):
        a = k * math.pi / 3
        lx, lz = x + round(math.cos(a) * 5), z + round(math.sin(a) * 5)
        zet(W, lx, y + 5, lz, "minecraft:chain[axis=y,waterlogged=false]")
        zet(W, lx, y + 4, lz, "messing_lantaarn[hanging=true,waterlogged=false]")
    # [u-z6afronding] the gate room cleared the last tread (z - 10, y gy): put it back, else the way up is a 1.5 step
    for dx in range(-1, 2):
        zet(W, x + dx, gy, z - 10, trap("vloer_zand", "south"))
    zet(W, x - 2, gy + 3, z - 11, "messing_lantaarn[hanging=false,waterlogged=false]")
    plek(W, "kerker_ingang", x, y, z + 12)
    plek(W, "kerker_hal", x, y, z + 3)
    plek(W, "kerker_poort", x, gy, z - 12)


def zoutgrot(W):
    """De Zingende Zoutgrot under the Zoutvlakte (ronde 4 cave decision: hand-built, own theme, craft spots, 1 elite, a
    puzzle and a treasure). Entrance: a salt mound with an arch (south), a straight rock-cut stair down to the main hall
    (glowing salt crystals, the underground lake), the puzzle niche with 5 singing crystals and the crystal wall, the
    treasure room behind it, the elite's round chamber (east)."""
    ex, _, ez = ZW.PLEK["zoutgrot"]
    y0 = stand(W, ex, ez)
    rng = rng_van(ex, ez, 61)
    # entrance mound + arch
    for dx in range(-7, 8):
        for dz in range(-6, 7):
            d = math.hypot(dx, dz * 1.3)
            if d < 7:
                g = grond(W, ex + dx, ez + dz)
                for dy in range(0, int(5 - d * 0.6)):
                    zet(W, ex + dx, g + 1 + dy, ez + dz, "zoutkristal" if (dx * 3 + dz + dy) % 4 else "zout")
    for k in range(7):
        zet(W, ex - 3 + k, y0 + 4, ez + 3, "zoutkristalletjes")
    leeg(W, ex - 1, y0, ez + 5, ex + 1, y0 + 6, ez + 8)   # [u-z6afronding] the mound in front of the stair top made a 1.5 step
    FL = 49                              # hall floor top block y (stand 50)
    # stair: from (ex, y0, ez + 4) north/down to (ex, FL + 1, ez - (y0 - FL))
    n = y0 - FL - 1
    for i in range(n + 6):
        zz = ez + 4 - i
        ys = max(y0 - 1 - i, FL)
        for dx in range(-1, 2):
            doos(W, ex + dx, ys - 3, zz, ex + dx, ys - 1, zz, "zout")
            if i < n:
                zet(W, ex + dx, ys, zz, trap("vloer_zand", "south"))
            else:
                zet(W, ex + dx, ys, zz, "zout")
            leeg(W, ex + dx, ys + 1, zz, ex + dx, ys + 4, zz)
        for dx in (-2, 2):
            for yy in range(ys, ys + 5):
                zet(W, ex + dx, yy, zz, "zoutkristal" if (yy + zz) % 5 else "minecraft:sea_lantern")
        if i % 6 == 3:
            zet(W, ex - 1, ys + 4, zz, "messing_lantaarn[hanging=true,waterlogged=false]")
    hz = ez - n - 18                      # hall centre z
    W.zoutgrot_hal = (ex, FL + 1, hz)
    # the main hall: an ellipsoid cavity with a flat floor
    W.bol((ex + 0.5, FL + 6, hz + 0.5), 22, LUCHT, ry=9, rz=15)
    for dx in range(-23, 24):
        for dz in range(-16, 17):
            q = (dx / 22.0) ** 2 + (dz / 15.0) ** 2
            if q < 1:
                zet(W, ex + dx, FL, hz + dz, "zout" if rng.rand() < 0.7 else "rots_roze")
                doos(W, ex + dx, FL - 3, hz + dz, ex + dx, FL - 1, hz + dz, "zout")
                leeg(W, ex + dx, FL + 1, hz + dz, ex + dx, FL + 1, hz + dz)
    # crystal shell: walls/ceiling of salt + pink crystal clusters + glowing crystals
    for dx in range(-24, 25):
        for dz in range(-17, 18):
            for dy in range(-1, 17):
                q = (dx / 23.0) ** 2 + ((dy - 5) / 10.0) ** 2 + (dz / 16.0) ** 2
                if 0.86 < q < 1.12:
                    p = (ex + dx, FL + dy, hz + dz)
                    if W.wget(*p) == LUCHT or "stairs" in W.wget(*p) or "trap" in W.wget(*p):
                        continue        # [u-z6afronding] the shell used to turn 2 entrance treads into full blocks (1.5 step)
                    r = rng.rand()
                    zet(W, *p, "zoutkristal" if r < 0.45 else ("minecraft:pink_stained_glass" if r < 0.5 else
                                                               ("minecraft:pearlescent_froglight" if r < 0.53 else "zout")))
    for k in range(70):
        a = rng.uniform(0, 2 * math.pi)
        rr = rng.uniform(0.3, 0.95)
        px, pz = ex + math.cos(a) * 21 * rr, hz + math.sin(a) * 14 * rr
        if rng.rand() < 0.5:
            zet(W, px, FL + 1, pz, "zoutkristalletjes", keep=True)
        else:
            top = FL + 6 + int(9 * math.sqrt(max(0, 1 - rr * rr)))
            zet(W, px, top, pz, "zoutkristalletjes", keep=True)
    # [u-z6afronding] the stair corridor stopped 2-3 blocks short of the hall and the shell above turned that rock into
    #   crystal: the whole cave was unreachable. Cut the passage through to the hall floor (3 wide, 4 high).
    z_eind = ez + 4 - (n + 5)
    for zz in range(hz + 6, z_eind + 1):
        for dx in range(-1, 2):
            zet(W, ex + dx, FL, zz, "zout")
            leeg(W, ex + dx, FL + 1, zz, ex + dx, FL + 4, zz)
    # pillars of salt (stalagmites with a glow)
    for k in range(9):
        a = k * 2 * math.pi / 9 + 0.3
        px, pz = ex + math.cos(a) * 15, hz + math.sin(a) * 9
        h = 3 + int(rng.randint(0, 5))
        if abs(px - ex) < 4 and pz > hz + 4:
            continue                      # [u-z6afronding] keep the way in from the stair free
        doos(W, px, FL + 1, pz, px, FL + h, pz, "zoutkristal")
        zet(W, px, FL + h + 1, pz, "minecraft:pearlescent_froglight" if k % 3 == 0 else "zoutkristalletjes")
    # the lake (west-south of the hall centre): glowing floor, shore steps
    lx, lz = ex + 8, hz - 7
    for dx in range(-9, 10):
        for dz in range(-6, 7):
            q = (dx / 9.0) ** 2 + (dz / 6.0) ** 2
            if q < 1:
                dpt = 3 if q < 0.45 else (2 if q < 0.8 else 1)
                for dy in range(dpt):
                    zet(W, lx + dx, FL - dy, lz + dz, "minecraft:water[level=0]")
                zet(W, lx + dx, FL - dpt, lz + dz, "minecraft:sea_lantern" if (dx + dz) % 5 == 0 else "zout")
    W.zoutgrot_meer = (lx, FL + 1, lz)
    plek(W, "zoutgrot", ex, FL + 1, hz + 8)
    plek(W, "zoutgrot_meer", lx, FL + 1, lz + 8)
    plek(W, "zoutgrot_vis", lx - 4, FL + 1, lz + 7)
    plek(W, "zoutgrot_mijn", ex - 14, FL + 1, hz + 6)
    for k in range(4):
        zet(W, ex - 17 + k, FL + 1, hz + 9, "zoutkristal")
        zet(W, ex - 17 + k, FL + 2, hz + 9, "zoutkristalletjes")
    # puzzle niche (west): 5 singing crystal pillars on a raised step + the crystal wall north of it
    px0, pz0 = ex - 20, hz
    leeg(W, px0 - 12, FL + 1, pz0 - 4, px0, FL + 6, pz0 + 4)
    doos(W, px0 - 12, FL, pz0 - 4, px0, FL, pz0 + 4, "zout")
    doos(W, px0 - 13, FL, pz0 - 5, px0 - 13, FL + 7, pz0 + 5, "zoutkristal")
    for i in range(5):
        cx = px0 - 10 + i * 2
        zet(W, cx, FL + 1, pz0 - 3, "zout")
        doos(W, cx, FL + 2, pz0 - 3, cx, FL + 3 + (i % 3), pz0 - 3, "zoutkristal")
        zet(W, cx, FL + 4 + (i % 3), pz0 - 3, "minecraft:pearlescent_froglight")
        plek(W, f"zoutgrot_kristal_{i + 1}", cx, FL + 1, pz0 - 2)
    plek(W, "zoutgrot_puzzel", px0 - 6, FL + 1, pz0 + 1)
    zet(W, px0 - 2, FL + 4, pz0 + 3, "messing_lantaarn")
    # the crystal wall (u-z6verhaal opens it at runtime: zoutgrot_kristalwand = its centre block, 3 wide x 4 high, along x)
    wx, wz = px0 - 6, pz0 + 5
    leeg(W, wx - 1, FL + 1, wz, wx + 1, FL + 4, wz + 8)
    for xx in range(wx - 1, wx + 2):
        for yy in range(FL + 1, FL + 5):
            zet(W, xx, yy, wz, "zoutkristal" if (xx + yy) % 2 else "minecraft:pink_stained_glass")
    plek(W, "zoutgrot_kristalwand", wx, FL + 2, wz)
    # treasure room behind it
    W.bol((wx + 0.5, FL + 3, wz + 9.5), 4.5, LUCHT, ry=3.2)
    doos(W, wx - 4, FL, wz + 5, wx + 4, FL, wz + 13, "zout")
    leeg(W, wx - 1, FL + 1, wz + 1, wx + 1, FL + 3, wz + 5)
    zet(W, wx, FL + 1, wz + 11, "minecraft:chest[facing=north,type=single,waterlogged=false]")
    zet(W, wx - 2, FL + 1, wz + 11, "minecraft:decorated_pot[facing=north,waterlogged=false,cracked=false]")
    zet(W, wx + 2, FL + 3, wz + 9, "messing_lantaarn")
    plek(W, "zoutgrot_schat", wx, FL + 1, wz + 9)
    # the elite's round chamber (east), through a short tunnel
    tx0 = ex + 23
    for i in range(-8, 15):     # [u-z6afronding] ex+15..ex+37: the tunnel used to stop short at both ends (hall + chamber walls)
        leeg(W, tx0 + i, FL + 1, hz - 1, tx0 + i, FL + 4, hz + 1)
        doos(W, tx0 + i, FL - 1, hz - 1, tx0 + i, FL, hz + 1, "zout")
    ec = (tx0 + 20, hz)
    W.bol((ec[0] + 0.5, FL + 5, ec[1] + 0.5), 10, LUCHT, ry=6)
    for dx in range(-10, 11):
        for dz in range(-10, 11):
            if math.hypot(dx, dz) < 10:
                zet(W, ec[0] + dx, FL, ec[1] + dz, "zout")
                doos(W, ec[0] + dx, FL - 3, ec[1] + dz, ec[0] + dx, FL - 1, ec[1] + dz, "zout")
                leeg(W, ec[0] + dx, FL + 1, ec[1] + dz, ec[0] + dx, FL + 1, ec[1] + dz)
    for k in range(12):
        a = k * math.pi / 6
        zet(W, ec[0] + math.cos(a) * 10.5, FL + 3, ec[1] + math.sin(a) * 10.5, "minecraft:sea_lantern")
    plek(W, "zoutgrot_elite", ec[0], FL + 1, ec[1])
    plek(W, "zoutgrot_ingang", ex, y0, ez + 6)
    # some hanging brass lamps in the hall (rings, no grid)
    for k in range(8):
        a = k * math.pi / 4
        hx, hzz = ex + round(math.cos(a) * 9), hz + round(math.sin(a) * 6)
        top = FL + 13
        for yy in range(FL + 9, top):
            if W.wget(hx, yy, hzz) != LUCHT:
                top = yy
                break
        for yy in range(FL + 9, top):
            zet(W, hx, yy, hzz, "minecraft:chain[axis=y,waterlogged=false]")
        zet(W, hx, FL + 8, hzz, "messing_lantaarn[hanging=true,waterlogged=false]")


def zoutvlakte(W):
    import landschap as LS
    LS.kamp(W, 3906, -584)
    plek(W, "karavaankamp", 3906, None, -580)
    kampvuur(W, "kampvuur_karavaankamp", 3906, -578, banken=False)
    stal_rijdieren(W, 3920, -570, seed=2, yaw=200)
    # the salt boiler's hut (side quest 11) + Zoutzieder Korrel
    x, z = 3874, -548
    y = stand(W, x, z)
    doos(W, x - 4, y - 1, z - 3, x + 4, y - 1, z + 3, "palm_planken")
    for xx in range(x - 4, x + 5):
        for zz in range(z - 3, z + 4):
            if xx in (x - 4, x + 4) or zz in (z - 3, z + 3):
                doos(W, xx, y, zz, xx, y + 3, zz, "zs" if (xx + zz) % 3 else "palm_stam")
    leeg(W, x - 3, y, z - 2, x + 3, y + 3, z + 2)
    leeg(W, x, y, z + 3, x, y + 1, z + 3)
    doos(W, x - 5, y + 4, z - 4, x + 5, y + 4, z + 4, "luifel_zijde")
    zet(W, x - 2, y, z - 2, "minecraft:cauldron")
    zet(W, x + 2, y, z - 2, "minecraft:barrel[facing=up,open=false]")
    zet(W, x, y + 3, z, "messing_lantaarn[hanging=true,waterlogged=false]")
    ZB.huis_rol(W, "zoutziedershuisje", "woning", (x, y, z + 3), (x, y, z))
    plek(W, "zoutziedershuisje", x, y, z)
    plek(W, "zoutzieder_korrel", x + 2, None, z + 6)
    # salt pans next to the hut
    for i in range(3):
        doos(W, x + 7 + i * 4, y - 1, z - 2, x + 9 + i * 4, y - 1, z + 2, "zout")
        doos(W, x + 7 + i * 4, y - 1, z - 1, x + 9 + i * 4, y - 1, z + 1, "minecraft:white_concrete_powder")
    plek(W, "zoutkolos_arena", 3995, None, -560)


def duinen(W):
    # the Duinput: a little well-oasis with palms, a tent, a stall with the three mounts and a campfire
    x, z = 3612, -672
    y = stand(W, x, z)
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if math.hypot(dx, dz) < 3.4:
                zet(W, x + dx, y - 1, z + dz, "zs")
    doos(W, x - 1, y - 3, z - 1, x + 1, y - 1, z + 1, "minecraft:water[level=0]")
    for (dx, dz) in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        doos(W, x + dx, y, z + dz, x + dx, y + 3, z + dz, "palm_stam")
    doos(W, x - 2, y + 4, z - 2, x + 2, y + 4, z + 2, "koepel_tegel")
    zet(W, x, y + 3, z, "messing_lantaarn[hanging=true,waterlogged=false]")
    for i, (dx, dz) in enumerate(((8, 6), (-8, 7), (9, -7), (-7, -9))):
        ZB.palm(W, x + dx, stand(W, x + dx, z + dz), z + dz, seed=i + 40)
    import landschap as LS
    LS.tent(W, x + 1, stand(W, x + 1, z + 14), z + 14, r=4, h=6, kl=("roze", "wit"), seed=3)
    stal_rijdieren(W, x + 16, z + 2, seed=1, yaw=270)
    plek(W, "duinput", x, y, z + 4)
    kampvuur(W, "kampvuur_duinput", x + 4, z + 8)
    plek(W, "duinguh_rimpel", 3646, None, -540)
    plek(W, "wormelien_arena", 3700, None, -600)
    plek(W, "bobbeltje_duin", 3684, None, -662)
    plek(W, "duinen_karavaanweg", 3726, None, -625)


def palmenkloven(W):
    """Waterfalls from the plateau into the river canyon, the waterfall stair (rivierpad -> plateau, the Palmbaken climb),
    mulberry trees with silk cocoons, palms, the Palmguh's spot."""
    # the waterfall stair: zigzag rock-cut stair from the canyon floor at (3540, -956) up to the plateau at (3520, -930)
    voet = (3540, -956)
    yv = stand(W, *voet)
    top = (3528, -922)
    yt = stand(W, *top, ymax=200)
    pts, x, z, y, rich = [], voet[0], voet[1], yv, 1
    # build switchbacks along x, climbing north->south (z increasing) one row per leg
    W.wegen = getattr(W, "wegen", {})
    rr = []
    legs = max(1, int(math.ceil((yt - yv) / 14.0)))
    zs = np.linspace(voet[1], top[1], legs + 1)
    for li in range(legs):
        xa, xb = (3520, 3556) if li % 2 == 0 else (3556, 3520)
        rr += [(xa, zs[li]), (xb, zs[li] + 2)]
    rr.append(top)
    W.wegen["watervaltrap"] = pad(W, [voet] + rr, breed=1, schouder=2, y_begin=yv, naam="watervaltrap", lampen=14)
    plek(W, "watervallen_voet", voet[0], None, voet[1] - 3)
    plek(W, "watervallen_top", top[0], None, top[1] + 2)
    # three waterfalls from the rim (static columns of water + a splash pool with a way out)
    for (wx, wz) in ((3560, -944), (3586, -936), (3506, -948)):
        g = grond(W, wx, wz, 200)
        bodem = None
        for k in range(1, 40):
            if grond(W, wx, wz - k, 200) < g - 8:
                bodem = (wz - k, grond(W, wx, wz - k, 200))
                break
        if bodem is None:
            continue
        bz, by = bodem
        for dx in (0, 1):
            doos(W, wx + dx, g - 1, bz + 1, wx + dx, g, wz, "minecraft:water[level=0]")
            doos(W, wx + dx, by + 1, bz, wx + dx, g, bz, "minecraft:water[level=0]")
            for zz in range(bz - 2, bz + 1):
                zet(W, wx + dx, by, zz, "minecraft:water[level=0]")
    # mulberry trees with silk cocoons
    for i in range(3):
        pid = f"moerbei_{i + 1}"
        x, _, z = _plan(pid)
        y = stand(W, x, z)
        doos(W, x, y, z, x, y + 4, z, "minecraft:dark_oak_log[axis=y]")
        W.bol((x + 0.5, y + 6, z + 0.5), 3.6, "minecraft:flowering_azalea_leaves[distance=1,persistent=true,waterlogged=false]",
              ry=2.6, keep=True)
        rng = rng_van(x, z, 9)
        for k in range(8):
            a = k * math.pi / 4
            zet(W, x + math.cos(a) * 3, y + 4, z + math.sin(a) * 3, "minecraft:white_wool")
        plek(W, pid, x + 3, None, z + 3)
    plek(W, "palmguh_dadelpit", 3486, None, -896)
    for i in range(3):
        pid = f"palmbaken_spiegel_{i + 1}"
        x, _, z = _plan(pid)
        y = stand(W, x, z)
        zet(W, x, y, z, "zs_beitel")
        doos(W, x, y + 1, z, x, y + 2, z, "minecraft:glass_pane")
        zet(W, x, y + 3, z, "goud")
        plek(W, pid, x, y, z + 1)
    plek(W, "koerier_spetter", 3536, None, -962)
    for i in range(5):
        x, _, z = _plan(f"waterval_{i + 1}")
        plek(W, f"waterval_{i + 1}", x, None, z)
    kampvuur(W, "kampvuur_palmenkloven", 3500, -966)
    stal_rijdieren(W, 3512, -972, seed=3, yaw=180)


def oase(W):
    """De Moederoase: date palms all round, the Moederpalm (a giant palm), Moeder Bron's resting spot on the shore,
    the refugee camp of the Oostroute (Mager Mientje), the Oasebaken stand."""
    ox, oz, r = ZW.OASE
    rng = rng_van(ox, oz, 77)
    for k in range(34):
        a = k * 2 * math.pi / 34 + rng.uniform(-0.05, 0.05)
        rr = r * rng.uniform(1.15, 1.6)
        px, pz = ox + math.cos(a) * rr, oz + math.sin(a) * rr
        if ZW.is_water(W, px, pz) or math.hypot(px - ZW.TOREN[0], pz - ZW.TOREN[1]) < 32:
            continue
        ZB.palm(W, px, stand(W, px, pz), pz, seed=k + 100)
    # the Moederpalm
    mx, mz = 4080, -812
    y = stand(W, mx, mz)
    for dy in range(0, 22):
        off = math.sin(dy / 7.0) * 1.2
        doos(W, mx + off, y + dy, mz, mx + off + 1, y + dy, mz + 1, "palm_stam")
    W.bol((mx + 1, y + 23, mz + 1), 4, "guhs:guhwaii_palm_blad", ry=2)
    for k in range(10):
        a = k * math.pi / 5
        for s in range(3, 11):
            zet(W, mx + 1 + math.cos(a) * s, y + 23 - (s - 3) * 0.55, mz + 1 + math.sin(a) * s, "guhs:guhwaii_palm_blad")
    plek(W, "moederpalm", mx + 3, None, mz + 3)
    plek(W, "moeder_bron", 4072, None, -806)
    plek(W, "moederoase", 4060, None, -790)
    kampvuur(W, "kampvuur_moederoase", 4054, -822)
    # the refugee camp (Mager Mientje, side quest 15) on the Oostroute
    import landschap as LS
    for i, (dx, dz) in enumerate(((0, 0), (12, 6), (-6, 12))):
        LS.tent(W, 4150 + dx, stand(W, 4150 + dx, -772 + dz), -772 + dz, r=3, h=5, kl=[("wit", "lila"), ("roze", "wit"),
                                                                                          ("geel", "wit")][i], seed=i + 6)
    plek(W, "mager_mientje", 4150, None, -760)
    plek(W, "oostroute", 4170, None, -730)
    stal_rijdieren(W, 4044, -838, seed=4, yaw=0)


def ruines(W):
    """Het Ruinedal: half-buried star temples (column stumps, broken arches, sunk domes), meteorite chunks, the 6 tablets,
    Professor Stof's dig camp."""
    rng = rng_van(3985, -975, 5)
    for k in range(14):
        x = 3870 + rng.randint(0, 260)
        z = -1050 + rng.randint(0, 130)
        if ZW.hoogte(W, x, z) > ZW.LAAG + 12:
            continue                                   # not on the mesas
        y = stand(W, x, z)
        soort = k % 3
        if soort == 0:                                 # a row of column stumps
            for i in range(5):
                h = int(rng.randint(1, 7))
                doos(W, x + i * 3, y - 1, z, x + i * 3, y + h, z, "zuil_wit")
                if rng.rand() < 0.4:
                    zet(W, x + i * 3, y + h + 1, z, "zuilkop")
        elif soort == 1:                               # a broken arch
            for dy in range(0, 7):
                zet(W, x, y + dy, z, "zs")
                zet(W, x + 6, y + dy, z, "zs" if dy < 4 else LUCHT)
            for i in range(7):
                zet(W, x + i, y + 6 + (1 if 1 <= i <= 4 else 0), z, "zs_beitel" if i == 3 else "zs")
        else:                                          # a sunk dome
            W.bol((x + 0.5, y - 2, z + 0.5), 5, b("rots_marmer"), ry=4, hollow=1.0, ymin=y)
            leeg(W, x - 1, y, z + 3, x + 1, y + 2, z + 6)
            doos(W, x - 4, y - 1, z - 4, x + 4, y - 1, z + 4, "vloer_zand")
        if rng.rand() < 0.5:
            zet(W, x + 2, y, z + 2, "minecraft:magma_block" if rng.rand() < 0.2 else "minecraft:blackstone")
    for i in range(6):
        pid = f"tablet_{i + 1}"
        x, _, z = _plan(pid)
        y = stand(W, x, z)
        zet(W, x, y, z, "zs_beitel")
        zet(W, x, y + 1, z, trap("zs", "south"))
        plek(W, pid, x, None, z + 1)
    # the dig camp
    import landschap as LS
    LS.tent(W, 3960, stand(W, 3960, -934), -934, r=3, h=5, kl=("geel", "wit"), seed=8)
    plek(W, "ruinedal_kamp", 3960, None, -940)
    plek(W, "professor_stof", 3970, None, -942)
    kampvuur(W, "kampvuur_ruinedal", 3952, -942)
    stal_rijdieren(W, 3948, -928, seed=5, yaw=90)


def karavanen(W):
    """Caravans on the roads (bultguhs with riders, flying carpets), as figures + recorded mounts for the game."""
    import zijfiguren as ZF
    for naam, start, n_b, n_t, seed in (("duinroute", 40.0, 6, 2, 4), ("zoutweg", 120.0, 5, 2, 7), ("karavaanweg", 20.0, 4, 1, 2)):
        prof = getattr(W, "wegen", {}).get(naam)
        if not prof:
            continue
        pts = [(x, z) for (x, z, _y) in prof[::4]]
        ZF.karavaan(W, pts, lambda x, z: stand(W, x, z), n_bult=n_b, n_tapijt=n_t, seed=seed, start=start, stap=5.5)


def uitkijken_en_geheimen(W):
    import plan6
    for pid in ("uitkijk_duinkam", "uitkijk_palmwaterval", "uitkijk_zoutrots", "uitkijk_mesa", "uitkijk_oase"):
        x, _, z = _plan(pid)
        uitkijk(W, pid, x, z)
    for i in range(1, len(plan6.GEHEIM_KNUFFEL) + 1):
        if i in plan6.GEHEIM_IN_STAD:
            continue                       # the city builders place these
        pid = f"geheim_knuffel_{i}"
        x, _, z = _plan(pid)
        geheim(W, pid, x, z, seed=i)
    # the mesa treasure (geheim_schat_3) + the Tafelmesa lookout: a narrow hidden rock-cut stair up the mesa
    x, _, z = _plan("geheim_schat_3")
    top = grond(W, x, z, 200)
    if top > ZW.LAAG + 12:
        W.wegen["tafelmesa"] = pad(W, [(x - 34, z + 26), (x - 22, z + 16), (x - 12, z + 8), (x - 4, z + 2), (x, z)], breed=0,
                                   schouder=1, y_eind=top + 1, naam="tafelmesa", lampen=99)
    y = stand(W, x, z)
    zet(W, x, y, z - 1, "minecraft:chest[facing=south,type=single,waterlogged=false]")
    plek(W, "geheim_schat_3", x, y, z)
    plek(W, "ruinedal_tafelmesa", x - 2, y, z + 2)


def extra_plekken(W):
    """Small builds for spots the story slice asked for: Duinguh Rimpel's tent, the dune rest stop + ridge, the Mika camp on the
    salt (grey tents, a doused lamp), the river mouth landing, the mirage spot on the Oostroute."""
    import landschap as LS
    x, z = 3640, -620
    LS.tent(W, x, stand(W, x, z - 4), z - 4, r=3, h=5, kl=("oranje", "wit"), seed=12)
    plek(W, "duinen_tent", x, None, z + 1)
    x, z = 3598, -742
    for dx in (-2, 2):
        zet(W, x + dx, stand(W, x + dx, z), z, plaat("palm_planken"))
    doos(W, x, stand(W, x, z), z - 2, x, stand(W, x, z) + 2, z - 2, "palm_hek")
    zet(W, x, stand(W, x, z) + 3, z - 2, "messing_lantaarn")
    plek(W, "duinen_rustplek", x, None, z)
    plek(W, "duinen_kam", 3622, None, -690)
    x, z = 3972, -582
    for i, (dx, dz) in enumerate(((0, 0), (8, 5), (-7, 6))):
        LS.tent(W, x + dx, stand(W, x + dx, z + dz), z + dz, r=3, h=5, kl=("wit", "lila"), seed=i + 30)
    for (dx, dz) in ((4, -4), (-4, -4)):
        doos(W, x + dx, stand(W, x + dx, z + dz), z + dz, x + dx, stand(W, x + dx, z + dz) + 2, z + dz, "minecraft:tuff_brick_wall")
    plek(W, "zoutvlakte_mikakamp", x, None, z - 6)
    x, z = 3306, -1000
    plek(W, "palmenkloven_monding", x, None, z)
    plek(W, "palmenkloven_palmenbos", 3450, None, -972)
    plek(W, "oostroute_luchtspiegeling", 4020, None, -662)
    for i in range(1, 4):
        plek(W, f"oase_tent_{i}", 4140 + 4 * i, None, -762)
    if getattr(W, "toren_top", None):
        tx, ty, tz = W.toren_top
        plek(W, "toren_top", tx, ty + 7, tz)


def _plan(pid):
    import plan6
    return plan6.P[pid]


def landmarks(W, log=print):
    bakens(W)
    log("  bakens")
    toren(W)
    log("  toren")
    kerker(W)
    zoutgrot(W)
    log("  kerker + zoutgrot")
    zoutvlakte(W)
    duinen(W)
    palmenkloven(W)
    oase(W)
    ruines(W)
    log("  gebieden")
    uitkijken_en_geheimen(W)
    extra_plekken(W)
    karavanen(W)
    log("  uitkijken, geheimen, karavanen")


# (x, y_from, z, y_to, block): Saffraanpoort, the 2-wide slot between the zellige house and the mosaic wall under a lintel
# (VAL 3816 72 -800); the 1x1 water shaft beside the canal at 3754 -860 (VAL, 6.5 up)
VASTE_PUTJES = [(3816, 72, -800, 77, "zs"), (3817, 72, -800, 77, "zs"), (3754, 61, -860, 68, "zs")]


def vul_putjes(W, y_lo=40, y_hi=200, log=print):
    """[u-z6afronding] Walkability post-pass over the whole zone-6 window (after the cities, landmarks, palms and figures):
    small sky-open pits of 1 or 2 columns whose rims are all 2+ higher (a gap between two houses, a coconut hole in a palm
    crown, a 1x1 shaft in the canyon rock, a water hole between canal walls) are filled up to 1 below the lowest rim with
    the pit's own floor block. You used to fall in from a roof/cliff and never get out (loopcheck VAL). Stair floors,
    fences and anything that is not air/water in the fill range are left alone (stair openings, doors, wells with decor)."""
    import poi6
    import loopcheck as LC
    K = poi6.Klassen(W)
    vast = np.isin(K.k, [LC.SOLID, LC.HALF, LC.STAIR, LC.DIRT_PATH, LC.FENCE])
    water = np.array([p.startswith("minecraft:water") for p in W.pal], bool)
    X, Y, Z = W.v.shape
    j0, j1 = max(1, W.vy(y_lo)), min(Y - 1, W.vy(y_hi))
    H = np.full((X, Z), -1, np.int32)
    for i0 in range(0, X, 64):
        sl = vast[W.v[i0:i0 + 64, j0:j1, :]]                      # (x, y, z)
        heeft = sl.any(axis=1)
        top = (j1 - j0 - 1) - np.argmax(sl[:, ::-1, :], axis=1)
        H[i0:i0 + 64] = np.where(heeft, top + j0, -1)
    P = np.pad(H, 1, mode="edge")
    nb = np.stack([P[2:, 1:-1], P[:-2, 1:-1], P[1:-1, 2:], P[1:-1, :-2]])     # +x, -x, +z, -z
    hoog = nb >= (H + 2)[None]
    enkel = hoog.all(axis=0)
    # pairs: exactly one rim side open, onto a neighbour at the same height that is itself closed on its other 3 sides
    paar = np.zeros_like(enkel)
    een_open = hoog.sum(axis=0) == 3
    for d, (dx, dz) in enumerate(((1, 0), (-1, 0), (0, 1), (0, -1))):
        kand = een_open & ~hoog[d] & (np.abs(nb[d] - H) <= 1)
        ii, kk = np.nonzero(kand)
        for i, k in zip(ii, kk):
            i2, k2 = i + dx, k + dz
            if 0 <= i2 < X and 0 <= k2 < Z and een_open[i2, k2] and not hoog[d ^ 1, i2, k2]:
                paar[i, k] = True
    gevuld = 0
    plekken = []
    stair_ids = K.k == LC.STAIR
    fence_ids = K.k == LC.FENCE
    def muur(i2, k2, h):
        """Height of the contiguous solid wall in column (i2, k2) right above level h (0 = no wall)."""
        if not (0 <= i2 < X and 0 <= k2 < Z):
            return 0
        n = 0
        while n < 10 and h + 1 + n < Y and vast[W.v[i2, h + 1 + n, k2]]:
            n += 1
        return n

    for i, k in zip(*np.nonzero((enkel | paar) & (H > 0))):
        h = H[i, k]
        # the rim must be real walls touching the pit (not a roof/awning high above an open neighbour)
        wanden = []
        for d, (dx, dz) in enumerate(((1, 0), (-1, 0), (0, 1), (0, -1))):
            if hoog[d, i, k]:
                wanden.append(muur(i + dx, k + dz, h))
        if not wanden or min(wanden) < 2:
            continue
        rim = h + 1 + min(wanden)
        if rim - h > 9:
            continue
        vloer = W.v[i, h, k]
        if stair_ids[vloer] or fence_ids[vloer] or water[W.v[i, h + 1, k]]:
            continue
        kol = W.v[i, h + 1:rim, k]
        if not all(c == 0 for c in kol):
            continue
        W.v[i, h + 1:rim, k] = vloer
        gevuld += 1
        plekken.append((int(W.b[0] + i), int(W.b[1] + h + 1), int(W.b[2] + k)))
    try:
        import json, os
        with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "_cache", "putjes_z6.json"), "w") as fh:
            json.dump(plekken, fh)
    except OSError:
        pass
    # two spots the column rule can't see (under a lintel / a water shaft between canal walls), checked by hand
    for (x, y, z, y2, k) in VASTE_PUTJES:
        for yy in range(y, y2 + 1):
            if W.wget(x, yy, z) == LUCHT or W.wget(x, yy, z).startswith("minecraft:water"):
                zet(W, x, yy, z, k)
                gevuld += 1
    log(f"  putjes gevuld: {gevuld} kolommen" + (f" (bv. {plekken[:6]})" if plekken else ""))
    return plekken
