"""
einde2 (bouw_water): the ground of the Noordelijke Waterstam, as one numpy block of materials.

The place brings its own floating slab: an iceberg with a tapering underside of packed and blue ice that carries
  - a glacier in the north with ridges down both flanks (the city lies in the bowl between them);
  - the city in three tiers (T0 at the water line with the canals, T1 with the training square, T2 with the palace) and the
    spirit oasis in a hollow of the glacier behind the palace;
  - the great ice wall between the two ridge ends, with its gate, towers, stairs and the Golfkansel above the gate;
  - a sea in front of the wall: a closed basin of source blocks inside a rim of pack ice, with the ice shelf at the wall's
    foot (west: the otter sled run down from the wall top; east: the landing place, the camp and the harbour).

Everything massive is made here, with its hollows already cut (canals, the gate arch, the tunnel to the oasis, the
recessed stairs between the tiers), and then only a SHELL of it goes into the template (emit): the inside of the berg is
hollow, nobody ever sees it. The architecture on top (houses, palace, bridges, ship) is einde2_bouw_water_stad.py and
einde2_bouw_water_schip.py, which never dig deeper than the shell (einde2_bouw_water_bouw.py checks that).
"""
import math

import numpy as np

import features.einde2_kit as kit

W, H, D = 224, 88, 240
SEA = 30                     # y of the top water block of the sea and the canals; the quays lie flush with it
T0, T1, T2 = SEA, SEA + 6, SEA + 12       # the floor blocks of the three tiers (you stand one higher)
MUUR = SEA + 18              # the floor of the walkway on the ice wall
KANSEL = MUUR + 3            # the floor of the Golfkansel above the gate
P0 = T2 + 2                  # the floor of the palace (on its podium)
BODEM = SEA - 4              # the top of the sea bed (the sea is four deep)

LEEG, PAK, BLAUW, SNEEUW, WATER, STEEN, MOS, ROTS = range(8)
IJSSTEEN = "guhs:einde2_bouw_water_ijssteen"
NAAM = {PAK: "minecraft:packed_ice", BLAUW: "minecraft:blue_ice", SNEEUW: "minecraft:snow_block", WATER: "minecraft:water",
        STEEN: IJSSTEEN, MOS: "minecraft:moss_block", ROTS: "minecraft:stone"}
SCHIL = 3                    # how thick the shell of a mass is

# --- the plan (template x: west -> east, z: north -> south; the sea is in the south) ---------------------------------------
MX = 112                     # the axis of the city: gate, great canal, grand stairs, palace, oasis
STAD0 = (40, 184, 100, 139)  # x0, x1, z0, z1 of the lower city (T0)
STAD1 = (46, 178, 76, 99)    # the middle tier (T1)
STAD2 = (64, 160, 34, 75)    # the palace terrace (T2)
MUUR_X = (40, 184)
MUUR_Z = (140, 149)
POORT_X = (108, 116)         # the gate arch (water 110..114, a footway on both sides)
TOREN_W, TOREN_O = (99, 106), (118, 125)      # the two gate towers (x ranges)
PLAAT_Z = (150, 167)         # the ice shelf at the wall's foot
PLAAT_X = (44, 206)
OASE = (112, 21, 9.0)        # centre x, z and radius of the oasis hollow
VIJVER = (112, 20, 4.8)      # the pond in it
PALEIS = (88, 136, 38, 63)   # the palace walls (outer), on a podium one block wider
BASSIN = (142, 168, 79, 95)  # the wave pool on the training tier
EILANDJE = (160, 87, 3.4)    # the islet in it (only an ice bridge gets you there)
PIER = (143, 150, 86, 88)    # the jetty the bridge starts from
# canals (x0, x1, z0, z1), all two deep; the ring and the great canal are five wide, the cross canal three
RING_N, RING_Z = (44, 180, 103, 107), (44, 180, 127, 131)
RING_W, RING_O = (44, 48, 103, 131), (176, 180, 103, 131)
GROOT = (110, 114, 103, 168)             # from the basin at the grand stairs through the gate into the sea
DWARS = (49, 175, 116, 118)
GRACHTEN = [RING_N, RING_Z, RING_W, RING_O, GROOT, DWARS]
TRAP_GROOT = (104, 120)      # x range of the grand stairs (T0 -> T1 at z 94..98, T1 -> T2 at z 70..74)
TRAP_ZIJ0 = [(49, 52), (172, 175)]       # side stairs T0 -> T1
TRAP_ZIJ1 = [(72, 75), (149, 152)]       # side stairs T1 -> T2
MUURTRAP = 18                # steps of the two stairs up the inside of the wall (east: x 127.., west: x 97..)
SCHIP = (177, 187, 163, 204)             # the hull of Zhao's ship (x0, x1, z0, z1); the bow points north, at the shelf
VLOOT = (48, 144, 170, 217)              # open sea in front of the wall: room for the fleet and for the great wave
# h2_water (phase B): the snow plain on the north-east glacier where Zuka drags the Guhvatar into the blizzard
VLAKTE = (188, 52, 15, 15)               # centre x, z and the two radii of the plain (only where the glacier is at least this high)
VLAKTE_Y = SEA + 44                      # its snow floor (you stand one higher)
VLAKTE_START = (178, VLAKTE_Y + 1, 55)   # where you wake up in the storm (Zuka keeps you here)
VLAKTE_VIND = (199, VLAKTE_Y + 1, 46)    # where Katara-guh and Appa find you


def ruis(naam, schaal, shape=(W, D)):
    """Smooth value noise 0..1 over the plan, from a NAME (deterministic)."""
    r = kit.rng("bouw_water/ruis/" + naam)
    g = r.random((shape[0] // schaal + 3, shape[1] // schaal + 3))
    xs, zs = np.arange(shape[0]) / schaal, np.arange(shape[1]) / schaal
    x0, z0 = xs.astype(int), zs.astype(int)
    fx, fz = xs - x0, zs - z0
    fx, fz = (fx * fx * (3 - 2 * fx))[:, None], (fz * fz * (3 - 2 * fz))[None, :]
    a, b, c, d = g[x0][:, z0], g[x0 + 1][:, z0], g[x0][:, z0 + 1], g[x0 + 1][:, z0 + 1]
    return (a * (1 - fx) + b * fx) * (1 - fz) + (c * (1 - fx) + d * fx) * fz


def afstand(buiten, maximum=90):
    """For every cell of the plan: how many steps (4-neighbours) to the nearest cell of `buiten` (0 inside it)."""
    dist = np.where(buiten, 0, maximum).astype(np.int32)
    for _ in range(maximum):
        nb = dist.copy()
        nb[1:, :] = np.minimum(nb[1:, :], dist[:-1, :] + 1)
        nb[:-1, :] = np.minimum(nb[:-1, :], dist[1:, :] + 1)
        nb[:, 1:] = np.minimum(nb[:, 1:], dist[:, :-1] + 1)
        nb[:, :-1] = np.minimum(nb[:, :-1], dist[:, 1:] + 1)
        if (nb == dist).all():
            break
        dist = nb
    return dist


def rechthoek(X, Z, r, groei=0):
    x0, x1, z0, z1 = r
    return (X >= x0 - groei) & (X <= x1 + groei) & (Z >= z0 - groei) & (Z <= z1 + groei)


def sleebaan():
    """The otter sled run: {(x, z): (top y, "baan" | "wal")} and the points of its middle line [(x, y, z)].
    From the Glijkansel on the wall top (west of the gate) west along the wall's outer face, a hairpin at the west end of the
    shelf, and back east along the edge of the shelf to the finish near the gate. It drops one block every five or six."""
    lijn = [(float(x), 153.5) for x in np.arange(93.5, 54.5, -0.25)]
    for a in np.arange(0.0, math.pi + 0.001, 0.04):                    # the hairpin: half a circle of radius 5 around (54.5, 158.5)
        lijn.append((54.5 - 5.0 * math.sin(a), 158.5 - 5.0 * math.cos(a)))
    lijn += [(float(x), 163.5) for x in np.arange(54.75, 90.5, 0.25)]
    lengte = [0.0]
    for (ax, az), (bx, bz) in zip(lijn, lijn[1:]):
        lengte.append(lengte[-1] + math.hypot(bx - ax, bz - az))
    totaal = lengte[-1]
    cellen, midden = {}, []
    for (px, pz), s in zip(lijn, lengte):
        top = MUUR - int(round((MUUR - T0) * min(1.0, s / (totaal - 6.0))))      # (the last six blocks run out flat)
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                x, z = int(math.floor(px)) + dx, int(math.floor(pz)) + dz
                d = math.hypot(x + 0.5 - px, z + 0.5 - pz)
                if d <= 1.55:
                    oud = cellen.get((x, z))
                    if oud is None or oud[1] == "wal" or d < oud[2]:
                        cellen[(x, z)] = (top, "baan", d)
                elif d <= 2.7 and (x, z) not in cellen:
                    cellen[(x, z)] = (top + 1, "wal", d)
        if not midden or math.hypot(px - midden[-1][0], pz - midden[-1][2]) >= 7.5:
            midden.append((int(math.floor(px)), top + 1, int(math.floor(pz))))
    midden.append((int(lijn[-1][0]), T0 + 1, int(lijn[-1][1])))
    return {c: (v[0], v[1]) for c, v in cellen.items()}, midden


class Grond:
    """The massive part of the build. M[x, y, z] = a material; G[x, z] = the top solid y of the natural ground; after emit():
    HOL[x, y, z] = inside the berg, not in the template."""

    def __init__(self):
        self.M = np.zeros((W, H, D), np.uint8)
        self.X, self.Z = np.meshgrid(np.arange(W), np.arange(D), indexing="ij")
        self.Y = np.arange(H)[None, :, None]
        self.baan, self.baan_lijn = sleebaan()
        self.berg()
        self.stad()
        self.muur()
        self.slee()
        self.water()
        self.oase()
        self.vlakte()

    # --- helpers ----------------------------------------------------------------------------------------------------------
    def leeg(self, x, y, z):
        """Is this cell of the ground open (no mass, no water)? What the builders of the city ask before they hang
        something on a face: a cell the template does not hold may also be the hollow inside of the berg."""
        return self.M[x, y, z] == LEEG

    def doos(self, x0, x1, y0, y1, z0, z1, mat):
        self.M[x0:x1 + 1, y0:y1 + 1, z0:z1 + 1] = mat

    def kolommen(self, masker, y0, y1, mat):
        """mat in every column of the 2D mask, from y0 to y1 (each a number or a 2D array)."""
        lo = np.broadcast_to(np.asarray(y0), (W, D))[:, None, :]
        hi = np.broadcast_to(np.asarray(y1), (W, D))[:, None, :]
        self.M[masker[:, None, :] & (self.Y >= lo) & (self.Y <= hi)] = mat

    # --- the berg: outline, underside, glacier, ridges, shelf, rim, sea ---------------------------------------------------------
    def berg(self):
        X, Z, Y = self.X, self.Z, self.Y
        qx, qz = np.abs(X - 111.5) - (103 - 60), np.abs(Z - 118.5) - (116.5 - 60)
        sd = np.hypot(np.maximum(qx, 0), np.maximum(qz, 0)) + np.minimum(np.maximum(qx, qz), 0) - 60
        # (the north coast, behind the oasis, stays almost straight: the glacier needs its full depth there)
        sd += ((ruis("kust", 18) - 0.5) * 9 + (ruis("kust_fijn", 6) - 0.5) * 3) * np.clip((Z - 4) / 40.0, 0.15, 1.0)
        eiland = sd < 0
        eiland[[0, -1], :] = False
        eiland[:, [0, -1]] = False
        rand = afstand(~eiland)
        self.eiland, self.rand = eiland, rand
        kom = rechthoek(X, Z, STAD0) | rechthoek(X, Z, STAD1) | rechthoek(X, Z, STAD2) | rechthoek(X, Z, MUUR_X + MUUR_Z)
        self.kom = kom
        d_kom = afstand(kom)
        # the glacier and the ridges: steep out of the bowl, highest in the north, tapering to the wall ends and out to sea,
        # and falling off to the rim of the berg in cliffs
        kap = np.interp(Z, [0, 40, 100, 150, 162, 174], [50, 50, 40, 25, 9, 1])
        berg = 24 + 2.0 * d_kom + (ruis("berg", 13) - 0.5) * 9 + (ruis("berg_fijn", 5) - 0.5) * 3
        berg = np.minimum(np.minimum(berg, kap + (ruis("kap", 9) - 0.5) * 6), 1 + 5.0 * rand)
        G = (SEA + np.round(berg)).astype(np.int32)
        land = eiland & (((Z < 150) & ~kom) | ((Z >= 150) & ((X < PLAAT_X[0]) | (X > PLAAT_X[1])) & (berg > 2.5) & (Z < 176)))
        # the shelf at the wall's foot: flush with the water, an irregular edge to the sea
        kant = np.where((X < 160) | (X > 200), PLAAT_Z[1] - 1 + np.round(ruis("plaat", 7) * 2), np.where((X >= 170) & (X <= 194), 160, 163))
        plaat = eiland & rechthoek(X, Z, PLAAT_X + PLAAT_Z) & (Z <= kant) & (rand > 3) & ~land
        # the rim of pack ice that keeps the sea in (never lower than the water), with a few bergs on it
        vrij = rechthoek(X, Z, VLOOT) | rechthoek(X, Z, SCHIP, 5)          # (never a berg of the rim in the fleet's water or at the ship)
        rim = eiland & (rand <= np.where(vrij, 3, 3 + np.round(ruis("rim", 9) * 3))) & ~land
        rim_h = SEA + np.round(ruis("rim_h", 6) * 2.2) + np.where((ruis("ijsberg", 15) > 0.7) & (rand <= 7), np.round(ruis("ijsberg_h", 5) * 8), 0)
        zee = eiland & (Z >= 150) & ~land & ~plaat & ~rim
        assert (zee | ~rechthoek(X, Z, VLOOT)).all() and (zee | ~rechthoek(X, Z, SCHIP, 2)).all(), "the fleet's water and the ship lie in the sea"
        G = np.where(kom, T0, G)
        G = np.where(plaat, T0, G)
        G = np.where(rim, rim_h.astype(np.int32), G)
        G = np.where(zee, BODEM - (ruis("bodem", 8) > 0.62), G)
        self.G, self.land, self.plaat, self.rim, self.zee = G, land, plaat, rim, zee
        # the underside: deep in the middle, thin at the rim, with icicles
        diep = 3 + 21 * (1 - np.exp(-rand / 24.0)) * (0.55 + 0.45 * np.clip((176 - Z) / 60.0, 0, 1)) + (ruis("onder", 11) - 0.5) * 6
        diep += np.where(ruis("pegel", 3) > 0.78, ruis("pegel_h", 2) * 5, 0)
        onder = np.maximum(1, np.round(BODEM - 1 - diep)).astype(np.int32)
        self.onder = onder
        vast = eiland[:, None, :] & (Y >= onder[:, None, :]) & (Y <= G[:, None, :])
        M = self.M
        M[vast] = PAK
        # strata of blue ice (they dip and wander a little), blue tips on the underside, snow on top of the natural land
        laag = ((Y + (ruis("laag", 22) * 12)[:, None, :] + (ruis("laag_fijn", 7) * 3)[:, None, :]).astype(np.int32) % 8) < 2
        M[vast & laag] = BLAUW
        M[vast & (Y < onder[:, None, :] + 2)] = BLAUW
        val = (ruis("val", 4) > 0.8)[:, None, :] & land[:, None, :] & (Y > SEA + 4)
        M[vast & val] = BLAUW
        natuur = (land | plaat | rim)[:, None, :]
        M[vast & natuur & (Y >= G[:, None, :] - 1)] = SNEEUW
        M[vast & zee[:, None, :] & (Y == G[:, None, :]) & (ruis("bodem_sneeuw", 5) > 0.55)[:, None, :]] = SNEEUW
        self.kolommen(zee, G + 1, SEA, WATER)
        # floes: flat cakes of snow on the water, never in the fleet's water, at the ship or in the way of a boat from the gate
        schots = zee & (ruis("schots", 6) > 0.77) & ~rechthoek(X, Z, VLOOT, 3) & ~rechthoek(X, Z, SCHIP, 8) & ~rechthoek(X, Z, (150, 180, 150, 196)) \
            & ~((np.abs(X - MX) < 9) & (Z < 180))
        M[:, SEA, :][schots] = SNEEUW
        self.schots = schots

    # --- the three tiers of the city, the podium of the palace, the stairs between the tiers ------------------------------------
    def stad(self):
        X, Z, Y = self.X, self.Z, self.Y
        stoep = ruis("stoep", 5) > 0.58
        for r, top in ((STAD0, T0), (STAD1, T1), (STAD2, T2)):
            m = rechthoek(X, Z, r)
            self.kolommen(m, SEA - 2, top, STEEN)
            self.kolommen(m, top + 1, H - 1, LEEG)
            self.kolommen(m & stoep, top, top, SNEEUW)
        x0, x1, z0, z1 = PALEIS
        self.doos(x0 - 1, x1 + 1, T2 + 1, P0, z0 - 1, z1 + 1, STEEN)
        self.doos(MX - 8, MX + 8, T2 + 1, T2 + 1, z1 + 2, z1 + 2, STEEN)         # (the one step up to the podium)
        # stairs cut back into the edge of the upper tier: z0 is the first step down, five steps, then the lower tier
        for (xa, xb), z, top in [(TRAP_GROOT, 94, T1), (TRAP_GROOT, 70, T2)] + [(t, 94, T1) for t in TRAP_ZIJ0] + [(t, 70, T2) for t in TRAP_ZIJ1]:
            for i in range(6):
                self.doos(xa, xb, top - i, H - 1, z + i, z + i, LEEG)
                self.doos(xa, xb, top - 1 - i, top - 1 - i, z + i, z + i, STEEN)
        self.G = np.where(rechthoek(X, Z, STAD1), T1, np.where(rechthoek(X, Z, STAD2), T2, self.G))

    # --- the ice wall ---------------------------------------------------------------------------------------------------------
    def muur(self):
        x0, x1 = MUUR_X
        z0, z1 = MUUR_Z
        M = self.M

        def massa(xa, xb, ya, yb, za, zb):
            """A block of wall: ijssteen with a course of blue ice every sixth layer."""
            self.doos(xa, xb, ya, yb, za, zb, STEEN)
            for y in range(ya, yb + 1):
                if (y - SEA) % 6 == 5:
                    self.doos(xa, xb, y, y, za, zb, BLAUW)

        massa(x0, x1, SEA - 2, MUUR, z0, z1)
        massa(x0, x1, T0 + 1, SEA + 10, z1 + 1, z1 + 1)           # the outer face leans: two steps thicker at the foot
        massa(x0, x1, T0 + 1, SEA + 4, z1 + 2, z1 + 2)
        for x in range(x0 + 8, x1, 16):                           # buttresses on the outer face
            if x > TOREN_O[1] + 3:                                # (west of the gate the sled run leans on the wall)
                massa(x, x + 1, T0 + 1, SEA + 14, z1 + 1, z1 + 2)
                massa(x, x + 1, T0 + 1, SEA + 7, z1 + 3, z1 + 3)
        # parapets: merlons on the sea side, a low wall on the city side
        self.doos(x0, x1, MUUR + 1, MUUR + 1, z1, z1, STEEN)
        for x in range(x0, x1 + 1):
            if x % 4 < 2:
                M[x, MUUR + 2, z1] = STEEN
        self.doos(x0, x1, MUUR + 1, MUUR + 1, z0, z0, STEEN)
        # the end towers (they stand half in the ridges) and the two gate towers
        for xa, xb in ((x0 - 2, x0 + 6), (x1 - 6, x1 + 2)):
            massa(xa, xb, SEA - 2, MUUR + 6, z0 - 2, z1 + 2)
            self.toren_top(xa, xb, MUUR + 6, z0 - 2, z1 + 2)
        for xa, xb in (TOREN_W, TOREN_O):
            massa(xa, xb, SEA - 2, MUUR + 9, z0 - 3, z1 + 3)
            self.toren_top(xa, xb, MUUR + 9, z0 - 3, z1 + 3)
        # the gatehouse between the gate towers, with the Golfkansel on it
        massa(TOREN_W[1] + 1, TOREN_O[0] - 1, SEA - 2, KANSEL, z0, z1 + 1)
        self.doos(TOREN_W[1] + 1, TOREN_O[0] - 1, KANSEL + 1, KANSEL + 1, z1 + 1, z1 + 1, STEEN)
        self.doos(TOREN_W[1] + 1, TOREN_O[0] - 1, KANSEL + 1, KANSEL + 1, z0, z0, STEEN)
        # the walkway runs through the towers; in the gate towers three steps lead up to the Golfkansel
        for xa, xb in ((x0 - 2, x0 + 6), (x1 - 6, x1 + 2)):
            self.doos(xa + 2, xb - 2, MUUR + 1, MUUR + 3, z0 + 3, z0 + 6, LEEG)
        self.doos(x0 + 5, x0 + 6, MUUR + 1, MUUR + 3, z0 + 3, z0 + 6, LEEG)
        self.doos(x1 - 6, x1 - 5, MUUR + 1, MUUR + 3, z0 + 3, z0 + 6, LEEG)
        for (xa, xb), richting in ((TOREN_W, 1), (TOREN_O, -1)):
            self.doos(xa, xb, MUUR + 1, MUUR + 6, z0 + 3, z0 + 6, LEEG)
            for i in range(3):
                x = (xb - 2 + i) if richting == 1 else (xa + 2 - i)
                self.doos(x, x, MUUR + 1, MUUR + 1 + i, z0 + 3, z0 + 6, STEEN)
        # the gate: an arch through wall and towers' flanks; the great canal runs through it (water())
        gx0, gx1 = POORT_X
        self.doos(gx0, gx1, T0 + 1, SEA + 5, z0 - 1, z1 + 3, LEEG)
        self.doos(gx0 + 1, gx1 - 1, SEA + 6, SEA + 6, z0 - 1, z1 + 3, LEEG)
        self.doos(gx0 + 2, gx1 - 2, SEA + 7, SEA + 7, z0 - 1, z1 + 3, LEEG)
        # the two stairs up the inside of the wall (a solid flight against its inner face) and their landings
        for kant in (1, -1):
            xs = 127 if kant == 1 else 97
            for i in range(MUURTRAP):
                x = xs + kant * i
                massa(x, x, T0 + 1, T0 + 1 + i, z0 - 2, z0 - 1)
                massa(x, x, T0 + 1, T0 + 2 + i, z0 - 3, z0 - 3)       # (a kerb on the open side)
            xa, xb = sorted((xs + kant * MUURTRAP, xs + kant * (MUURTRAP + 2)))
            massa(xa, xb, T0 + 1, MUUR, z0 - 2, z0 - 1)
            massa(xa, xb, T0 + 1, MUUR + 1, z0 - 3, z0 - 3)
            x_eind = xb + 1 if kant == 1 else xa - 1
            massa(x_eind, x_eind, T0 + 1, MUUR + 1, z0 - 3, z0 - 1)       # (and a kerb at the end of the landing)
            self.doos(xa, xb, MUUR + 1, MUUR + 1, z0, z0, LEEG)           # (the gap in the low wall)
        # the Glijkansel: where the sled run starts, outside the parapet west of the gate
        self.doos(92, 98, MUUR + 1, MUUR + 2, z1, z1, LEEG)
        massa(91, 98, T0 + 1, MUUR, z1 + 1, z1 + 6)
        self.doos(91, 98, MUUR + 1, MUUR + 1, z1 + 7, z1 + 7, STEEN)
        massa(91, 98, T0 + 1, MUUR, z1 + 7, z1 + 7)
        self.doos(98, 98, MUUR + 1, MUUR + 1, z1 + 1, z1 + 7, STEEN)

    def toren_top(self, xa, xb, y, za, zb):
        """Merlons around a tower top and a stepped cap of snow in the middle."""
        M = self.M
        for x in range(xa, xb + 1):
            for z in range(za, zb + 1):
                if x in (xa, xb) or z in (za, zb):
                    M[x, y + 1, z] = STEEN
                    if (x + z) % 2 == 0:
                        M[x, y + 2, z] = STEEN
        for i in range(1, 4):
            if xa + 1 + i <= xb - 1 - i:
                self.doos(xa + 1 + i, xb - 1 - i, y + i, y + i, za + 1 + i, zb - 1 - i, SNEEUW)

    # --- the sled run: a ramp of ice against the wall and along the shelf -----------------------------------------------------
    def slee(self):
        M = self.M
        for (x, z), (top, soort) in self.baan.items():
            assert self.plaat[x, z] or self.kom[x, z], f"the sled run leaves the shelf at {(x, z)}"
            if top <= T0:
                continue
            if z >= 152:
                M[x, T0 + 1:top + 1, z] = PAK
                M[x, top, z] = BLAUW if soort == "baan" else SNEEUW
                if not (91 <= x <= 98):
                    M[x, top + 1:min(H, top + 6), z] = LEEG
            if z in (151, 152) and x >= 58:          # (the first leg leans on the wall: no gap behind it)
                gat = M[x, T0 + 1:top + 2, 150:152]
                M[x, T0 + 1:top + 2, 150:152] = np.where(gat == LEEG, PAK, gat)
        # where the run lies flush in the shelf its lane is blue ice too
        for (x, z), (top, soort) in self.baan.items():
            if top == T0 and soort == "baan" and self.plaat[x, z]:
                M[x, T0, z] = BLAUW

    # --- canals, the wave pool -------------------------------------------------------------------------------------------------
    def water(self):
        M = self.M
        for x0, x1, z0, z1 in GRACHTEN:
            self.doos(x0, x1, SEA - 1, SEA, z0, z1, WATER)
            self.doos(x0, x1, SEA - 2, SEA - 2, z0, z1, BLAUW)
        # the basin at the foot of the grand stairs (the gondola starts here): the north arm bulges south
        for x in range(MX - 9, MX + 10):
            for z in range(108, 113):
                if math.hypot(x - MX, (z - 107) * 1.7) <= 9.3:
                    M[x, SEA - 1:SEA + 1, z] = WATER
                    M[x, SEA - 2, z] = BLAUW
        # outside the gate the canal runs through the shelf into the sea: the sea bed takes over where the sea begins
        # the wave pool of the training square: sunk one block, two deep, with a jetty and an islet at the level of the square
        x0, x1, z0, z1 = BASSIN
        self.doos(x0, x1, T1 - 2, T1 - 1, z0, z1, WATER)
        self.doos(x0, x1, T1 - 3, T1 - 3, z0, z1, BLAUW)
        self.doos(x0, x1, T1, T1, z0, z1, LEEG)
        px0, px1, pz0, pz1 = PIER
        self.doos(px0, px1, T1 - 2, T1, pz0, pz1, STEEN)
        ex, ez, er = EILANDJE
        for x in range(int(ex - er - 1), int(ex + er + 2)):
            for z in range(int(ez - er - 1), int(ez + er + 2)):
                if math.hypot(x - ex, z - ez) <= er:
                    M[x, T1 - 2:T1 + 1, z] = STEEN
        self.doos(x0 + 6, x0 + 8, T1 - 1, T1 - 1, z1, z1, STEEN)       # (the steps out of the pool, on its south side)

    # --- the spirit oasis: a hollow in the glacier behind the palace, open to the sky, and the tunnel to it ----------------------
    def oase(self):
        M, X, Z = self.M, self.X, self.Z
        cx, cz, r = OASE
        afst = np.hypot(X - cx, Z - cz)
        for y in range(T2 - 2, H):
            straal = r + max(0, y - T2 - 2) / 14.0
            m = afst <= straal
            if y <= T2 - 1:
                M[:, y, :][m & (M[:, y, :] != LEEG)] = ROTS
            elif y == T2:
                M[:, y, :][m] = MOS
            else:
                M[:, y, :][m] = LEEG
        self.G = np.where(afst <= r, T2, self.G)
        vx, vz, vr = VIJVER
        vijver = (np.hypot(X - vx, Z - vz) <= vr) & (np.hypot(X - vx, Z - vz) > 1.6)
        self.kolommen(vijver, T2 - 1, T2, WATER)
        self.kolommen(vijver, T2 - 2, T2 - 2, ROTS)
        self.doos(MX, MX, T2 - 1, T2, int(cz - r) + 1, int(vz - vr), WATER)      # the brook from the foot of the waterfall
        self.doos(MX, MX, T2 - 2, T2 - 2, int(cz - r) + 1, int(vz - vr), ROTS)
        self.doos(MX - 1, MX + 1, T2 + 1, T2 + 3, int(cz + r) - 1, STAD2[2] - 1, LEEG)      # the tunnel
        self.doos(MX - 1, MX + 1, T2, T2, int(cz + r) - 1, STAD2[2] - 1, STEEN)

    # --- h2_water: the snow plain of the blizzard, cut flat into the north-east glacier ------------------------------------------
    def vlakte(self):
        """A plain of snow on the glacier (flat, the glacier cut off at VLAKTE_Y wherever it stood higher), with low drifts and
        a few needles of packed ice that loom up in the white-out; the spots of the start and of the finding stay clear."""
        X, Z = self.X, self.Z
        cx, cz, rx, rz = VLAKTE
        ellips = ((X - cx) / rx) ** 2 + ((Z - cz) / rz) ** 2 <= 1.0
        m = ellips & self.land & (self.G >= VLAKTE_Y)
        self.kolommen(m, VLAKTE_Y + 1, H - 1, LEEG)
        self.kolommen(m, VLAKTE_Y - 1, VLAKTE_Y, SNEEUW)
        vrij = np.zeros_like(m)
        for sx, _, sz in (VLAKTE_START, VLAKTE_VIND):
            vrij |= np.hypot(X - sx, Z - sz) <= 3.5
        drift = m & ~vrij & (ruis("vlakte_drift", 4) > 0.74)
        self.kolommen(drift, VLAKTE_Y + 1, VLAKTE_Y + 1, SNEEUW)
        naald = m & ~vrij & (ruis("vlakte_naald", 3) > 0.93) & (((X - cx) / rx) ** 2 + ((Z - cz) / rz) ** 2 <= 0.8)
        hoogte = (3 + np.round(ruis("vlakte_naald_h", 2) * 4)).astype(np.int32)
        self.kolommen(naald, VLAKTE_Y + 1, VLAKTE_Y + hoogte, PAK)
        self.G = np.where(m, VLAKTE_Y, self.G)
        self.vlakte_masker = m & ~drift & ~naald
        for sx, sy, sz in (VLAKTE_START, VLAKTE_VIND):
            assert m[sx, sz] and self.M[sx, sy, sz] == LEEG and self.M[sx, sy - 1, sz] == SNEEUW, f"the snow plain does not reach {(sx, sz)}"

    # --- into the template -----------------------------------------------------------------------------------------------------
    def emit(self, b):
        """Puts the shell of the mass (everything within SCHIL cells of air or water) and all water into the build."""
        M = self.M
        open_ = np.pad((M == LEEG) | (M == WATER), 1, constant_values=True)
        dicht = open_.copy()
        for _ in range(SCHIL):
            n = dicht.copy()
            n[1:, :, :] |= dicht[:-1, :, :]
            n[:-1, :, :] |= dicht[1:, :, :]
            n[:, 1:, :] |= dicht[:, :-1, :]
            n[:, :-1, :] |= dicht[:, 1:, :]
            n[:, :, 1:] |= dicht[:, :, :-1]
            n[:, :, :-1] |= dicht[:, :, 1:]
            dicht = n
        dicht = dicht[1:-1, 1:-1, 1:-1]
        self.HOL = (M != LEEG) & ~dicht
        blocks = b.s.blocks
        water = {"level": "0"}
        for x, y, z in np.argwhere((M != LEEG) & dicht):
            m = M[x, y, z]
            blocks[(int(x), int(y), int(z))] = (NAAM[m], water if m == WATER else {}, None)
