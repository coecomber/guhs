"""[U-Z5TUIN] Hoogvads gardens (feedback: "the plateau next to the Raadszaal is a noisy pink/green patchwork").

The city ground (kit/stad.grond) lays a hash mix of wolkenweide_gras and grass_block on every open patch. This pass, run at the
end of bouw5.bouw (after the terrace stairs, before the figurants are published), finds EVERY open grass patch of the city (the
top grass block of a column with air above it, inside the city mask) and:
  1. makes the lawn one calm green (grass_block), no more noise;
  2. turns every patch that is big enough into a designed garden:
       - a winding spine path along the long axis (3 wide on wide patches), cross paths every ~12 blocks (dirt_path);
       - the plots between the paths get a theme: vegetable garden (wheat / carrots / beetroot / potatoes in rows with water
         channels), pumpkin + melon patch, sweet-berry rows, flower bed (rings of tulips, alliums, daisies, cornflowers ... with
         tall flowers and a bird house in the middle), rose garden, pond (water lilies) and, on the biggest plot of a big
         garden, a fountain with a column and a golden candelabrum;
       - kitchen-garden plots get a low flowering-azalea hedge, the others a flower border;
       - along the paths: guh lantern posts (guh_lantaarnpaal), pink guh benches facing the path, park bins, and on long spines
         a leafy pergola over the path;
  3. narrow strips (front gardens) get a flower row along the house wall.
Walkability: paths reach the patch edge on all sides, nothing solid (hedge, post, bench, rim, crop, pumpkin) or wet stands
within 2 of a door / stair / ladder / gate / trapdoor, within 3 of a published spot, on a figurant, or next to a wall, a
balustrade or a 1-block step; water is 1 deep with a step out, and the channels' ends stay open (no hedge).
"""
import math

import numpy as np
from scipy import ndimage

import bouwhulp as BH
from palet import P

GAZON = "minecraft:grass_block[snowy=false]"
PAD = "minecraft:dirt_path"
HAAG = "minecraft:flowering_azalea_leaves[distance=1,persistent=true,waterlogged=false]"
WATER = "minecraft:water[level=0]"
AKKER = "minecraft:farmland[moisture=7]"
LAMP_O, LAMP_B = "guhs:guh_lantaarnpaal[half=lower]", "guhs:guh_lantaarnpaal[half=upper]"
GEWAS = ["minecraft:wheat[age=7]", "minecraft:carrots[age=7]", "minecraft:beetroots[age=3]", "minecraft:potatoes[age=7]"]
BLOEMEN = ["minecraft:pink_tulip", "minecraft:white_tulip", "minecraft:allium", "minecraft:oxeye_daisy", "minecraft:cornflower",
           "minecraft:poppy", "minecraft:azure_bluet", "minecraft:red_tulip", "minecraft:orange_tulip"]
HOOG = ["minecraft:rose_bush", "minecraft:peony", "minecraft:lilac"]
RANDEN = [("minecraft:pink_tulip", "minecraft:white_tulip"), ("minecraft:allium", "minecraft:oxeye_daisy"),
          ("minecraft:cornflower", "minecraft:white_tulip"), ("minecraft:pink_tulip", "minecraft:oxeye_daisy"),
          ("minecraft:azure_bluet", "minecraft:allium")]
THEMAS = ["moestuin", "bloemen", "vijver", "bessen", "pompoen", "rozen", "moestuin", "bloemen", "pompoen", "bloemen"]
NAAST = ((1, 0, "east"), (-1, 0, "west"), (0, 1, "south"), (0, -1, "north"))
SPECIAAL = ("door", "_deur", "stairs", "_trap", "ladder", "fence_gate", "trapdoor", "poortje", "luik")
MIN_TUIN = 40
# (only flowers the shared loopcheck knows as passable: lily_of_the_valley counts as a solid block there and would be a fake
#  stepping stone next to a balustrade)


# ============================================================================================== the surface
class Grond:
    """The open grass surface of the city: mask m (X, Z), top grass voxel y H, and the protection masks."""

    def __init__(self, W, S, plekken):
        v = W.v
        self.W = W
        self.b = W.b
        names = [s.split("[")[0] for s in W.pal]
        lucht = np.array([n in ("minecraft:air", "minecraft:cave_air") for n in names] + [False] * 8)
        gras = np.array([n in ("guhs:wolkenweide_gras", "minecraft:grass_block") for n in names] + [False] * 8)
        spec = np.array([any(t in n.split(":")[-1] for t in SPECIAAL) and "dakpan" not in n and "kroonlijst" not in n
                         for n in names] + [False] * 8)
        self.lucht_lut = lucht
        y_lo, y_hi = W.vy(212), W.vy(266)
        sub = v[:, y_lo:y_hi + 1, :]
        ok = gras[sub[:, :-1, :]] & lucht[sub[:, 1:, :]]
        heeft = ok.any(1)
        idx = ok.shape[1] - 1 - np.argmax(ok[:, ::-1, :], axis=1)
        self.H = np.where(heeft, y_lo + idx, -1)
        self.m = heeft & S.m
        # air above (how many free cells, up to 5)
        Hc = np.clip(self.H, 0, v.shape[1] - 7)
        vrij = np.zeros(self.m.shape, np.int8)
        alle = np.ones(self.m.shape, bool)
        for k in range(1, 6):
            alle &= lucht[np.take_along_axis(v, (Hc + k)[:, None, :], 1)[:, 0, :]]
            vrij += alle
        self.vrij = vrij
        # doors / stairs / ladders ... near the ground (H-1 .. H+3) of every column (also non-grass columns: their own top)
        sp = np.zeros(self.m.shape, bool)
        Hs = np.where(self.H > 0, self.H, 0)
        # for non-grass columns: scan around the neighbouring ground height (dilated H)
        Hn = ndimage.maximum_filter(np.where(self.m, self.H, -1), size=5)
        Hs = np.where(self.m, Hs, Hn)
        Hs = np.clip(Hs, 1, v.shape[1] - 5)
        for k in range(-1, 4):
            sp |= spec[np.take_along_axis(v, (Hs + k)[:, None, :], 1)[:, 0, :]]
        sp &= Hn >= 0
        self.speciaal = ndimage.binary_dilation(sp, structure=np.ones((5, 5), bool))
        # published spots (3 around, any height) and the figurants (1 around)
        bez = np.zeros(self.m.shape, bool)
        for (x, _, z) in plekken:
            i, k = W.vx(x), W.vz(z)
            if 0 <= i < bez.shape[0] and 0 <= k < bez.shape[1]:
                bez[i, k] = True
        self.plek = ndimage.binary_dilation(bez, structure=np.ones((7, 7), bool))
        fig = np.zeros(self.m.shape, bool)
        for g in getattr(W, "guhs", []):
            i, k = int(math.floor(g[0])), int(math.floor(g[2]))
            if 0 <= i < fig.shape[0] and 0 <= k < fig.shape[1]:
                fig[i, k] = True
        self.figuur = ndimage.binary_dilation(fig, structure=np.ones((3, 3), bool))
        # free-standing: every one of the 8 neighbours is level ground (T == H) or a real drop (T <= H - 2). Next to a wall, a
        # balustrade, a building or a 1-block step nothing solid goes: a hedge or a bench there would be a stepping stone over
        # the balustrade, or block the step that is the way up
        vast = ~lucht[v[:, y_lo:y_hi + 1, :]]
        T = np.where(vast.any(1), y_lo + vast.shape[1] - 1 - np.argmax(vast[:, ::-1, :], axis=1), -999)
        T = np.where(self.m, self.H, T)
        ok = np.ones(self.m.shape, bool)
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if dx or dz:
                    Tn = np.roll(np.roll(T, -dx, 0), -dz, 1)
                    ok &= (Tn == self.H) | (Tn <= self.H - 2)
        self.vrijstaand = ok & self.m
        self.bezet = np.zeros(self.m.shape, bool)    # cells already given something by this pass

    # voxel cell helpers ------------------------------------------------------------------------
    def zet(self, i, y, k, s):
        self.W.v[i, y, k] = self.W.id(BH.b(s))

    def lucht(self, i, y, k):
        return bool(self.lucht_lut[self.W.v[i, y, k]])

    def vast_mag(self, i, k, hoogte=2):
        """May something solid (or wet) go on this cell?"""
        return (self.m[i, k] and self.vrijstaand[i, k] and not self.speciaal[i, k] and not self.plek[i, k] and not self.figuur[i, k]
                and not self.bezet[i, k] and self.vrij[i, k] >= hoogte)

    def plant_mag(self, i, k):
        return self.m[i, k] and not self.plek[i, k] and not self.bezet[i, k] and self.vrij[i, k] >= 1


# ============================================================================================== pieces
def bloem(G, i, k, s):
    """A flower (tall flowers: two halves) on the lawn at (i, k)."""
    y = G.H[i, k]
    if s in HOOG:
        if G.vrij[i, k] < 2:
            s = "minecraft:pink_tulip"
        else:
            G.zet(i, y + 1, k, s + "[half=lower]")
            G.zet(i, y + 2, k, s + "[half=upper]")
            G.bezet[i, k] = True
            return
    G.zet(i, y + 1, k, s)
    G.bezet[i, k] = True


def lantaarn(G, i, k):
    y = G.H[i, k]
    G.zet(i, y + 1, k, LAMP_O)
    G.zet(i, y + 2, k, LAMP_B)
    G.bezet[i, k] = True


def bank(G, i, k, facing):
    G.zet(i, G.H[i, k] + 1, k, f"guhs:guh_bankje_roze[facing={facing}]")
    G.bezet[i, k] = True


def prullenbak(G, i, k, facing):
    G.zet(i, G.H[i, k] + 1, k, f"guhs:park_prullenbak[facing={facing}]")
    G.bezet[i, k] = True


# ============================================================================================== one garden
def paden(R, H):
    """Spine + cross paths of a patch R (bool, local window). Returns (path mask, long axis, spine centre per slice)."""
    ii, kk = np.nonzero(R)
    lang = 0 if (ii.max() - ii.min()) >= (kk.max() - kk.min()) else 1
    L = ii if lang == 0 else kk
    Q = kk if lang == 0 else ii
    l0, l1 = L.min(), L.max()
    centra = {}
    for l in range(l0, l1 + 1):
        q = Q[L == l]
        if len(q):
            centra[l] = float(np.median(q))
    ls = sorted(centra)
    glad = {}
    for l in ls:
        w = [centra[x] for x in range(l - 4, l + 5) if x in centra]
        glad[l] = int(round(float(np.median(w))))
    span = max(len(set(Q[L == l])) for l in ls)
    w = 1 if span >= 16 else 0
    pad = np.zeros(R.shape, bool)
    for l in ls:
        for q in range(glad[l] - w, glad[l] + w + 1):
            a, c = (l, q) if lang == 0 else (q, l)
            if 0 <= a < R.shape[0] and 0 <= c < R.shape[1] and R[a, c]:
                pad[a, c] = True
    # the spine may have gaps where the patch is narrow and off-centre: connect consecutive slices (step in q)
    for l in ls[:-1]:
        if l + 1 in glad and glad[l + 1] != glad[l]:
            q1, q2 = sorted((glad[l], glad[l + 1]))
            for q in range(q1, q2 + 1):
                a, c = (l, q) if lang == 0 else (q, l)
                if R[a, c]:
                    pad[a, c] = True
    # side paths parallel to the spine, every 8 cells out from it (plots about 7 wide)
    if span >= 14:
        for (a, c) in np.argwhere(R):
            l, q = (a, c) if lang == 0 else (c, a)
            off = abs(q - glad[l]) - w
            if off > 0 and off % 8 == 0:
                pad[a, c] = True
    # cross paths every 9 (not in the first/last 4)
    lengte = l1 - l0 + 1
    if lengte >= 14:
        P_ = 9 if lengte >= 24 else max(7, lengte // 2)
        for l in range(l0 + P_ // 2 + 1, l1 - 3, P_):
            sel = R[l, :] if lang == 0 else R[:, l]
            if lang == 0:
                pad[l, :] |= sel
            else:
                pad[:, l] |= sel
    return pad, lang, glad, w


def tuin(G, sl, R, nr, log_n):
    """Design one garden on patch R (bool, in the window sl of the whole grid)."""
    i0, k0 = sl[0].start, sl[1].start
    H = G.H[sl]
    pad, lang, glad, w = paden(R, H)
    # lay the paths
    for (a, c) in np.argwhere(pad):
        i, k = i0 + a, k0 + c
        if G.vrij[i, k] >= 3:          # (not under a low branch or an overhang: the path stays lawn there)
            G.zet(i, G.H[i, k], k, PAD)
        G.bezet[i, k] = True
    # leafy pergolas over the spine (long gardens: one per ~30 blocks, the first place from a quarter on where it fits)
    ls = sorted(glad)
    if len(ls) >= 22:
        gedaan = []
        for l in ls[len(ls) // 4: len(ls) - 4]:
            if len(gedaan) >= max(1, len(ls) // 30) or any(abs(l - g) < 14 for g in gedaan):
                continue
            if pergola(G, sl, R, pad, lang, glad, w, l, log_n):
                gedaan.append(l)
    # furniture along the paths: candidate cells = patch cells next to a path
    naast = np.zeros(R.shape, bool)
    richting = {}
    for (dx, dz, f) in NAAST:
        sh = np.zeros(R.shape, bool)
        src = pad[max(0, dx):R.shape[0] + min(0, dx), max(0, dz):R.shape[1] + min(0, dz)]
        sh[max(0, -dx):R.shape[0] + min(0, -dx), max(0, -dz):R.shape[1] + min(0, -dz)] = src
        nieuw = sh & R & ~pad
        for (a, c) in np.argwhere(nieuw):
            richting.setdefault((a, c), f)
        naast |= nieuw
    lampen, banken = [], []
    for (a, c) in sorted(map(tuple, np.argwhere(naast))):
        i, k = i0 + a, k0 + c
        if not G.vast_mag(i, k, 3):
            continue
        if all(max(abs(a - p), abs(c - q)) >= 9 for (p, q) in lampen):
            lantaarn(G, i, k)
            lampen.append((a, c))
            n_ = log_n.setdefault("lantaarnpalen", 0)
            log_n["lantaarnpalen"] = n_ + 1
            continue
        if all(max(abs(a - p), abs(c - q)) >= 8 for (p, q) in banken) and \
                all(max(abs(a - p), abs(c - q)) >= 3 for (p, q) in lampen):
            f = richting[(a, c)]
            bank(G, i, k, f)
            banken.append((a, c))
            log_n["bankjes"] = log_n.get("bankjes", 0) + 1
            # a bin next to it, along the path
            for (dx, dz, _) in NAAST:
                p, q = a + dx, c + dz
                if 0 <= p < R.shape[0] and 0 <= q < R.shape[1] and naast[p, q] and G.vast_mag(i0 + p, k0 + q, 2):
                    prullenbak(G, i0 + p, k0 + q, f)
                    break
    # the plots
    vakken, n = ndimage.label(R & ~pad)
    grootte = ndimage.sum(np.ones(R.shape), vakken, range(1, n + 1)) if n else []
    # the fountain: on the flat plot nearest the garden's middle that is deep enough (big gardens only)
    fontein_vak = None
    if R.sum() >= 250 and n:
        ra, rc = np.argwhere(R).mean(0)
        best = 1e9
        for j in range(1, n + 1):
            V = vakken == j
            hs = H[V]
            if hs.min() != hs.max():
                continue
            D = ndimage.distance_transform_cdt(np.pad(V, 1), metric="taxicab")[1:-1, 1:-1]
            if D.max() < 4:
                continue
            va, vc = np.argwhere(V).mean(0)
            dd = (va - ra) ** 2 + (vc - rc) ** 2
            if dd < best:
                best, fontein_vak = dd, j
    volgorde = sorted(range(1, n + 1), key=lambda j: -grootte[j - 1])
    for t, j in enumerate(volgorde):
        V = vakken == j
        thema = THEMAS[(nr * 3 + t) % len(THEMAS)]
        D = ndimage.distance_transform_cdt(np.pad(V, 1), metric="taxicab")[1:-1, 1:-1]
        cel = np.argwhere(V)
        hs = H[V]
        vlak = hs.min() == hs.max()
        if j == fontein_vak:
            thema = "fontein"
        if thema in ("moestuin", "vijver", "pompoen") and (not vlak or D.max() < 3):
            thema = "bloemen"
        if len(cel) < 10:
            thema = "rand"
        log_n[thema] = log_n.get(thema, 0) + 1
        vak(G, i0, k0, V, D, thema, nr * 7 + t, lang)


def pergola(G, sl, R, pad, lang, glad, w, l_mid, log_n):
    """A leafy pergola over 5 slices of the spine: posts (3 high) on both sides every other slice, a roof of
    flowering azalea at H+4 (3 blocks of headroom over the path); the posts never stand on a path."""
    i0, k0 = sl[0].start, sl[1].start
    sl_ = [l for l in range(l_mid - 2, l_mid + 3)]
    if any(l not in glad or glad[l] != glad[l_mid] for l in sl_):
        return False
    q = glad[l_mid]
    cellen = []
    for l in sl_:
        for qq in range(q - w - 1, q + w + 2):
            a, c = (l, qq) if lang == 0 else (qq, l)
            if not (0 <= a < R.shape[0] and 0 <= c < R.shape[1] and R[a, c]):
                return False
            i, k = i0 + a, k0 + c
            if G.H[i, k] != G.H[i0 + (l_mid if lang == 0 else q), k0 + (q if lang == 0 else l_mid)] or G.vrij[i, k] < 4:
                return False
            zij = abs(qq - q) == w + 1
            if zij and (G.speciaal[i, k] or G.plek[i, k] or G.figuur[i, k] or G.bezet[i, k] or pad[a, c]):
                return False
            cellen.append((i, k, zij, l))
    y = G.H[cellen[0][0], cellen[0][1]]
    for (i, k, zij, l) in cellen:
        if zij and (l - sl_[0]) % 2 == 0:
            G.zet(i, y + 1, k, "minecraft:pale_oak_fence")
            G.zet(i, y + 2, k, "minecraft:pale_oak_fence")
            G.zet(i, y + 3, k, "minecraft:pale_oak_fence")
        elif zij:
            G.zet(i, y + 1, k, "minecraft:pink_tulip")
        G.zet(i, y + 4, k, HAAG)
        if zij:
            G.bezet[i, k] = True
    log_n["pergolas"] = log_n.get("pergolas", 0) + 1
    return True


def vak(G, i0, k0, V, D, thema, seed, lang):
    """Fill one plot V (bool in the garden window) with its theme; D = taxicab distance to the plot edge (1 = edge)."""
    rand_a, rand_b = RANDEN[seed % len(RANDEN)]
    cel = [tuple(c) for c in np.argwhere(V)]
    dmax = int(D.max())
    haag = thema in ("moestuin", "pompoen", "bessen")
    # the centre (deepest cell nearest the centroid)
    ca, cc = np.mean([c[0] for c in cel]), np.mean([c[1] for c in cel])
    diep = [c for c in cel if D[c] == dmax]
    midden = min(diep, key=lambda c: (c[0] - ca) ** 2 + (c[1] - cc) ** 2)
    if thema == "fontein" and not fontein(G, i0, k0, V, midden, min(3, dmax - 1)):
        thema = "bloemen"
    for (a, c) in cel:
        i, k = i0 + a, k0 + c
        if G.bezet[i, k]:
            continue
        d = D[a, c]
        y = G.H[i, k]
        q = c if lang == 0 else a          # across the long axis of the garden: the rows run along it
        if d == 1 or thema == "rand":
            geul = (thema == "moestuin" and q % 3 == 1) or (thema == "pompoen" and q % 3 == 2)
            if haag and not geul and G.vast_mag(i, k, 1) and (a + c) % 7 != 3:   # (the channels' ends stay open)
                G.zet(i, y + 1, k, HAAG)
                G.bezet[i, k] = True
            elif G.plant_mag(i, k) and (a + c) % 2 == 0:
                bloem(G, i, k, rand_a if (a + c) % 4 == 0 else rand_b)
            continue
        if thema == "moestuin":
            if q % 3 == 1:
                if G.vast_mag(i, k, 1):
                    G.zet(i, y, k, WATER)
                    if (a + c) % 5 == 0:
                        G.zet(i, y + 1, k, "minecraft:lily_pad")
                    G.bezet[i, k] = True
            elif G.vast_mag(i, k, 1):
                G.zet(i, y, k, AKKER)
                G.zet(i, y + 1, k, GEWAS[(q // 3 + seed) % len(GEWAS)])
                G.bezet[i, k] = True
            elif G.plant_mag(i, k):
                bloem(G, i, k, rand_b)
        elif thema == "pompoen":
            r = q % 3
            if r == 2:
                if G.vast_mag(i, k, 1):
                    G.zet(i, y, k, WATER)
                    G.bezet[i, k] = True
            elif r == 0 and G.vast_mag(i, k, 1):
                # the fruit lies one row further (q + 1): the stem points at it
                f = ("south" if lang == 0 else "east")
                fa, fc = (a, c + 1) if lang == 0 else (a + 1, c)
                if 0 <= fa < V.shape[0] and 0 <= fc < V.shape[1] and V[fa, fc] and D[fa, fc] > 1 and \
                        G.vast_mag(i0 + fa, k0 + fc, 1):
                    fr, st = (("minecraft:melon", "minecraft:attached_melon_stem") if ((a + c) // 4 + seed) % 3 == 0 else
                              ("minecraft:pumpkin", "minecraft:attached_pumpkin_stem"))
                    G.zet(i, y, k, AKKER)
                    G.zet(i, y + 1, k, f"{st}[facing={f}]")
                    G.zet(i0 + fa, G.H[i0 + fa, k0 + fc] + 1, k0 + fc, fr)
                    G.bezet[i0 + fa, k0 + fc] = True
                else:
                    G.zet(i, y, k, AKKER)
                    G.zet(i, y + 1, k, "minecraft:pumpkin_stem[age=7]")
                G.bezet[i, k] = True
        elif thema == "bessen":
            if q % 2 == 0 and G.vast_mag(i, k, 1):
                G.zet(i, y + 1, k, "minecraft:sweet_berry_bush[age=3]")
                G.bezet[i, k] = True
            elif G.plant_mag(i, k) and (a + c) % 3 == 0:
                bloem(G, i, k, "minecraft:allium")
        elif thema == "vijver":
            if G.vast_mag(i, k, 1):
                G.zet(i, y, k, WATER)
                h = (a * 7 + c * 13 + seed) % 11
                if h == 0:
                    G.zet(i, y + 1, k, "minecraft:lily_pad")
                elif h == 5:
                    G.zet(i, y + 1, k, "guhs:guh_waterlelie")
                G.bezet[i, k] = True
            elif G.plant_mag(i, k):
                G.zet(i, y + 1, k, "minecraft:pink_petals[facing=north,flower_amount=4]")
                G.bezet[i, k] = True
        elif thema == "rozen":
            if G.plant_mag(i, k):
                if (a + c) % 2 == 0:
                    bloem(G, i, k, "minecraft:rose_bush")
                else:
                    G.zet(i, y + 1, k, "minecraft:pink_petals[facing=%s,flower_amount=4]" % ("north", "east", "south", "west")[(a * 3 + c) % 4])
                    G.bezet[i, k] = True
        else:   # bloemen (and the rest of a fountain plot): rings of colour, tall flowers in ring 2, a bird house in the middle
            if (a, c) == midden and dmax >= 3 and thema == "bloemen" and G.vast_mag(i, k, 3):
                G.zet(i, y + 1, k, "minecraft:pale_oak_fence")
                G.zet(i, y + 2, k, "guhs:vogelhuisje")
                G.bezet[i, k] = True
            elif G.plant_mag(i, k):
                if d == 2 and dmax >= 3 and (a + c) % 2 == 0:
                    bloem(G, i, k, HOOG[(seed + d) % len(HOOG)])
                else:
                    bloem(G, i, k, BLOEMEN[(d + seed) % len(BLOEMEN)])


def fontein(G, i0, k0, V, midden, R):
    """A round basin (rim 1 high, water 1 deep inside, so you can climb out), a column with a gold capital and a golden
    candelabrum in the middle, a ring of pink petals around it."""
    a0, c0 = midden
    y = G.H[i0 + a0, k0 + c0]

    def past(R):
        # every basin cell must be in the plot, free and level: a gap in the rim would let the water run out
        for a in range(a0 - R, a0 + R + 1):
            for c in range(c0 - R, c0 + R + 1):
                if math.hypot(a - a0, c - c0) > R + 0.5:
                    continue
                if not (0 <= a < V.shape[0] and 0 <= c < V.shape[1] and V[a, c]) or not G.vast_mag(i0 + a, k0 + c, 5 if
                                                                                                    (a, c) == midden else 2)                         or G.H[i0 + a, k0 + c] != y:
                    return False
        return True
    while R >= 2 and not past(R):
        R -= 1
    if R < 2:
        return False
    for a in range(a0 - R - 1, a0 + R + 2):
        for c in range(c0 - R - 1, c0 + R + 2):
            if not (0 <= a < V.shape[0] and 0 <= c < V.shape[1] and V[a, c]):
                continue
            i, k = i0 + a, k0 + c
            d = math.hypot(a - a0, c - c0)
            if d <= R + 0.5:
                if d > R - 0.5:
                    G.zet(i, y, k, P["steen_bak"])
                    G.zet(i, y + 1, k, P["wit_glad"])
                elif d < 0.5:
                    G.zet(i, y, k, P["wit_glad"])
                    G.zet(i, y + 1, k, P["wit_glad"])
                    G.zet(i, y + 2, k, P["zuil_wit"])
                    G.zet(i, y + 3, k, P["zuilkop"])
                    G.zet(i, y + 4, k, "guhs:gouden_kandelaar")
                else:
                    G.zet(i, y, k, P["wit_glad"])
                    G.zet(i, y + 1, k, WATER)
                    if (a + c) % 4 == 1 and d > 1.2:
                        G.zet(i, y + 2, k, "minecraft:lily_pad")
                G.bezet[i, k] = True
            elif d <= R + 1.5 and G.plant_mag(i, k):
                G.zet(i, y + 1, k, "minecraft:pink_petals[facing=north,flower_amount=4]")
                G.bezet[i, k] = True
    return True


def strook(G, sl, R, log_n):
    """A narrow patch (front garden): a flower row along the house wall, otherwise lawn."""
    i0, k0 = sl[0].start, sl[1].start
    W = G.W
    n = 0
    for (a, c) in np.argwhere(R):
        i, k = i0 + a, k0 + c
        if not G.plant_mag(i, k):
            continue
        y = G.H[i, k]
        tegen = False
        for (dx, dz, _) in NAAST:
            p, q = i + dx, k + dz
            if 0 <= p < W.v.shape[0] and 0 <= q < W.v.shape[2] and not G.m[p, q] and \
                    not G.lucht(p, y + 1, q) and not G.lucht(p, y + 2, q):
                tegen = True
        if tegen:
            s = RANDEN[(i // 6 + k // 6) % len(RANDEN)][(i + k) % 2]
            bloem(G, i, k, s if (i + k) % 3 else "minecraft:rose_bush")
            n += 1
    log_n["voortuin_bloemen"] = log_n.get("voortuin_bloemen", 0) + n


# ============================================================================================== all
def bouw(W, S, info, log=print):
    import plan5 as PL
    plekken = [tuple(v) for v in info.get("plekken", {}).values() if v and len(v) == 3]
    plekken += [(x, 0, z) for (x, _, z) in PL.plekken().values()]
    G = Grond(W, S, plekken)
    # 1. one calm lawn
    gazon = W.id(GAZON)
    ii, kk = np.nonzero(G.m)
    W.v[ii, G.H[ii, kk], kk] = gazon
    # 2. gardens per patch
    lab, n = ndimage.label(G.m)
    vlakken = ndimage.find_objects(lab)
    log_n = {}
    nt = 0
    for j, sl in enumerate(vlakken, start=1):
        if sl is None:
            continue
        R = lab[sl] == j
        size = int(R.sum())
        if size < 12:
            continue
        D = ndimage.distance_transform_cdt(np.pad(R, 1), metric="taxicab")[1:-1, 1:-1]
        if size >= MIN_TUIN and D.max() >= 3:
            tuin(G, sl, R, nt, log_n)
            nt += 1
        else:
            strook(G, sl, R, log_n)
    log(f"tuinen (U-Z5TUIN): {int(G.m.sum())} grasvakjes egaal, {nt} tuinen, " +
        ", ".join(f"{k} {v}" for k, v in sorted(log_n.items())))
    return nt
