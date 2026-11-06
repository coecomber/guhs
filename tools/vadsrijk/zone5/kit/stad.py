"""Hoogvads: de stad op de rug van Moeder Wolkwal (indeling, grond, terrassen, stadsmuur, straten, huizen).

Wereldcoördinaten (k=1): x langs de walvis (kop = +x/oost), z dwars (noord = -z), y omhoog.
  bouw(W)       -> walvis + stad + alle gebouwen + figuren (W = schemerkit.Wereld)
  ZONES         -> de grote bouwplaatsen (voor labels, camera's en het LEESMIJ)
"""
import math
import numpy as np
import palet
from palet import P, trap, plaat
import bouwhulp as BH
import wolkwal as WW

# ------------------------------------------------------------------ indeling
NIV = {"acro": 252, "markt": 238, "uni": 234, "kaz": 234, "plein": 240, "kath": 244, "haven": 236}
ZONES = {
    "paleis":      (-152, -76, -48, 48, NIV["acro"]),
    "markt":       (-74, -6, -24, 24, NIV["markt"]),
    "uni":         (-76, 6, -84, -26, NIV["uni"]),
    "kazerne":     (-76, 6, 26, 84, NIV["kaz"]),
    "plein":       (-6, 18, -36, 36, NIV["plein"]),
    "kathedraal":  (16, 126, -36, 36, NIV["kath"]),
    "transept":    (68, 98, -50, 50, NIV["kath"]),
    "haven":       (22, 120, -98, -54, NIV["haven"]),
    "staartpark":  (-199, -153, -60, 60, None),
    "ooggaarde":   (130, 172, 30, 90, None),
}
X_MIN, X_MAX = -199, 172
RAND = 0.80  # stad tot 80% van de halve breedte


def stadmasker(X, Z):
    a = WW.breedte(X)
    return (X >= X_MIN) & (X <= X_MAX) & (np.abs(Z) < RAND * a) & (a > 20)


def grondhoogte(X, Z):
    """Grondniveau (bovenkant van de bovenste grondlaag) per kolom; nan buiten de stad."""
    r = WW.rug(X, Z)
    # [zone 5 kaart] 1-block terraces (was 4): the open city ground follows the whale's back in steps of 1, so every street and
    # garden is walkable in survival; the monumental zones below stay plateaus with their ornamental retaining walls
    g = np.floor(r - 1) + 1
    for naam, (x1, x2, z1, z2, niv) in ZONES.items():
        if niv is None:
            continue
        m = (X >= x1) & (X <= x2) & (Z >= z1) & (Z <= z2)
        g = np.where(m, niv, g)
    # paleisheuvel rond (afgeronde hoeken)
    m = stadmasker(X, Z)
    return np.where(m, g, np.nan)


class Stad:
    def __init__(self, W):
        self.W = W
        self.xs = np.arange(int(W.b[0]), int(W.b[3]))
        self.zs = np.arange(int(W.b[2]), int(W.b[5]))
        self.X, self.Z = np.meshgrid(self.xs + 0.5, self.zs + 0.5, indexing="ij")
        self.m = stadmasker(self.X, self.Z)
        self.G = grondhoogte(self.X, self.Z)
        self.bezet = np.zeros(self.X.shape, bool)   # gereserveerd (gebouwen, straten)
        self.straat = np.zeros(self.X.shape, bool)
        self.rand = np.zeros(self.X.shape, bool)

    def ix(self, x):
        return int(x - self.W.b[0])

    def iz(self, z):
        return int(z - self.W.b[2])

    def g(self, x, z):
        i, j = self.ix(x), self.iz(z)
        if 0 <= i < self.G.shape[0] and 0 <= j < self.G.shape[1]:
            v = self.G[i, j]
            return None if np.isnan(v) else int(v)
        return None

    def reserveer(self, x1, z1, x2, z2):
        a, b_ = self.ix(min(x1, x2)), self.ix(max(x1, x2)) + 1
        c, d = self.iz(min(z1, z2)), self.iz(max(z1, z2)) + 1
        self.bezet[max(a, 0):b_, max(c, 0):d] = True

    def vrij(self, x1, z1, x2, z2):
        a, b_ = self.ix(x1), self.ix(x2) + 1
        c, d = self.iz(z1), self.iz(z2) + 1
        if a < 0 or c < 0 or b_ > self.m.shape[0] or d > self.m.shape[1]:
            return False
        return bool(self.m[a:b_, c:d].all() and not self.bezet[a:b_, c:d].any() and
                    not self.straat[a:b_, c:d].any() and not self.rand[a:b_, c:d].any())


# ------------------------------------------------------------------ grond
def grond(S):
    """Grond op de rug: fundering (wolkensteen) waar het plateau boven de huid ligt, aarde + gras/plaveisel bovenop,
    huid weg boven het plateau. Keermuren (wolkensteen-bakstenen) waar terrassen verspringen."""
    W = S.W
    G = np.where(S.m, S.G, np.nan)
    rugv = WW.rug(S.X, S.Z)
    huid_top = np.where(S.m, rugv, np.nan)
    # huid boven het grondniveau weghalen
    W.kolom(np.where(S.m, G + 1, np.inf), np.where(S.m, np.fmax(huid_top, G) + 2, -np.inf), "minecraft:air",
            mask=S.m)
    # fundering
    W.kolom(np.where(S.m, np.fmin(huid_top, G) - 3, np.inf), np.where(S.m, G - 3, -np.inf), P["steen"], mask=S.m)
    W.kolom(np.where(S.m, G - 2, np.inf), np.where(S.m, G - 1, -np.inf), "minecraft:dirt", mask=S.m)
    # bovenlaag: gras met wat bloemen-gras, plaveisel waar straat
    gras = W.id("guhs:wolkenweide_gras")
    gras2 = W.id("minecraft:grass_block[snowy=false]")
    import schemerkit as SK
    h = SK.hash3(S.X, np.zeros_like(S.X), S.Z, 5)
    top = np.where(h > 0.55, gras2, gras).astype(np.uint16)
    W.kolom(np.where(S.m, G, np.inf), np.where(S.m, G, -np.inf), top, mask=S.m)
    # keermuren: buren lager -> zijvlak bekleden
    Gf = np.nan_to_num(G, nan=-1e9)
    lager = np.zeros_like(Gf)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nb = np.roll(np.roll(Gf, dx, 0), dz, 1)
        lager = np.maximum(lager, Gf - nb)
    wand = S.m & (lager >= 2) & (lager < 1e8)
    nb_min = Gf.copy()
    Rf = np.where(np.isnan(rugv), Gf - 30, rugv)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nb = np.roll(np.roll(Gf, dx, 0), dz, 1)
        nr = np.roll(np.roll(Rf, dx, 0), dz, 1)
        nb_min = np.minimum(nb_min, np.where(nb < -1e8, nr - 2, nb))
    W.kolom(np.where(wand, nb_min, np.inf), np.where(wand, Gf - 1, -np.inf), P["steen_bak"], mask=wand)
    W.kolom(np.where(wand, Gf, np.inf), np.where(wand, Gf, -np.inf), P["wit_glad"], mask=wand)
    W.kolom(np.where(wand & (lager >= 3), Gf + 1, np.inf), np.where(wand & (lager >= 3), Gf + 1, -np.inf),
            P["wit_muur"], mask=wand & (lager >= 3))
    S.wand = wand


def plaveisel(S, mask, blok=None, blok2=None):
    W = S.W
    blok = blok or P["vloer2"]
    blok2 = blok2 or P["steen"]
    import schemerkit as SK
    h = SK.hash3(S.X, np.ones_like(S.X), S.Z, 9)
    ids = np.where(h > 0.5, W.id(blok), W.id(blok2)).astype(np.uint16)
    G = np.where(mask, S.G, np.nan)
    W.kolom(np.where(mask, G, np.inf), np.where(mask, G, -np.inf), ids, mask=mask & S.m)


# ------------------------------------------------------------------ stadsmuur
def stadsmuur(S, seed=1):
    """Muur langs de rand (bovenop de terrasrand), met ronde torens, vaandels en lantaarns."""
    W = S.W
    m = S.m
    rand = m & ~(np.roll(m, 1, 0) & np.roll(m, -1, 0) & np.roll(m, 1, 1) & np.roll(m, -1, 1))
    # 2 breed
    rand2 = rand | (m & (np.roll(rand, 1, 0) | np.roll(rand, -1, 0) | np.roll(rand, 1, 1) | np.roll(rand, -1, 1)))
    S.rand = rand2.copy()
    G = np.where(rand2, S.G, np.nan)
    W.kolom(np.where(rand2, G - 6, np.inf), np.where(rand2, G + 4, -np.inf), P["steen_bak"], mask=rand2)
    W.kolom(np.where(rand2, G + 5, np.inf), np.where(rand2, G + 5, -np.inf), P["wit_glad"], mask=rand2)
    W.kolom(np.where(rand, G + 6, np.inf), np.where(rand, G + 6, -np.inf), P["wit_muur"], mask=rand)
    # gouden band
    W.kolom(np.where(rand2, G + 2, np.inf), np.where(rand2, G + 2, -np.inf), P["goudrand"], mask=rand2)
    # torens om de ~44 blokken langs de rand
    pts = np.argwhere(rand)
    rng = np.random.RandomState(seed)
    gekozen = []
    for (i, j) in pts[rng.permutation(len(pts))]:
        x, z = S.xs[i], S.zs[j]
        if all((x - a) ** 2 + (z - c) ** 2 > 44 ** 2 for (a, c) in gekozen):
            gekozen.append((x, z))
    S.torens = gekozen
    for (x, z) in gekozen:
        g = S.g(x, z)
        if g is None:
            continue
        bodem = int(WW.rug(x + 0.5, z + 0.5) - 14) if not np.isnan(WW.rug(x + 0.5, z + 0.5)) else g - 14
        BH.ronde_toren(W, x, z, min(bodem, g - 10), (g - min(bodem, g - 10)) + 16, 5, wand=P["marmer"],
                       rand=P["wit_glad"], dak="dak_roze", dak_h=16, kantelen=False)
        kant = "south" if z > 0 else "north"
        BH.vaandel(W, x, g + 9, z + (6 if z > 0 else -6), kant)
        BH.zet(W, x, g + 17, z, "minecraft:lantern")


# ------------------------------------------------------------------ straten
def straten(S):
    X, Z = S.X, S.Z
    st = np.zeros(X.shape, bool)
    st |= (np.abs(Z) <= 5) & (X >= -78) & (X <= 18)                       # Zachtmoedlaan
    for x0 in (-178, 150):
        st |= (np.abs(X - x0) <= 2)
    for z0 in (-60, 60):
        st |= (np.abs(Z - z0) <= 2) & ((X < -76) | (X > 18))
    st |= (np.abs(Z) <= 3) & ((X < -152) | (X > 126))
    # ringstraat net binnen de muur
    m = S.m
    d = m.copy()
    binnen = m.copy()
    for k in range(7):
        binnen = binnen & np.roll(binnen, 1, 0) & np.roll(binnen, -1, 0) & np.roll(binnen, 1, 1) & np.roll(binnen, -1, 1)
        if k == 2:
            ring_buiten = binnen.copy()
    ring = ring_buiten & ~binnen
    st |= ring
    st &= m
    S.straat |= st
    plaveisel(S, st & ~S.bezet)


# ------------------------------------------------------------------ huizen
KLEUREN = ["pastelbaksteen_roze", "pastelbaksteen_vanille", "pastelbaksteen_perzik", "pastelbaksteen_lavendel",
           "pastelbaksteen_framboos", "pastelbaksteen_orchidee", "pastelbaksteen_wolkje", "pleister_roze",
           "pleister_vanille", "pleister_orchidee", "pleister_framboos", "pastelbaksteen_aqua", "pastelbaksteen_boter"]
KLEUREN_VAN = ["minecraft:pink_terracotta", "minecraft:white_terracotta", "minecraft:orange_terracotta",
               "minecraft:magenta_terracotta", "minecraft:cherry_planks", "minecraft:pale_oak_planks",
               "minecraft:light_blue_terracotta", "minecraft:pink_concrete", "minecraft:white_concrete"]
DAKEN = ["dak_roze", "dak_roze", "dak_wit", "guhs:dakpan_kersrood", "guhs:dakpan_hemelsblauw", "guhs:dakpan_lavendel",
         "guhs:dakpan_mint", "dak_roze"]


def _dak_sleutel(d):
    """Zorg dat een dakpan-naam als paletsleutel bestaat (P[naam], P[naam_trap], P[naam_plaat])."""
    if d in P:
        return d
    if palet.MODUS == "vanilla":
        return "dak_roze"
    P[d], P[d + "_trap"], P[d + "_plaat"] = d, d + "_trap", d + "_plaat"
    return d


def huis(W, x1, z1, x2, z2, y0, verd=3, seed=0, voor="south", winkel=False, luifel=None):
    """Smal stadshuis: plint van wolkensteen, gevel in pastel, witte banden, ramen (sommige verlicht),
    deur aan de 'voor'-kant, zadeldak met de nok evenwijdig aan de straat, schoorsteentje."""
    rng = np.random.RandomState(seed)
    if palet.MODUS == "vanilla":
        wand = KLEUREN_VAN[rng.randint(len(KLEUREN_VAN))]
    else:
        wand = "guhs:" + KLEUREN[rng.randint(len(KLEUREN))]
    rand = P["wit_glad"]
    hv = 5
    y_top = y0 + verd * hv
    BH.doos(W, x1, y0, z1, x2, y0, z2, P["steen_bak"])
    BH.doos(W, x1, y0 + 1, z1, x2, y_top - 1, z2, wand)
    for v in range(1, verd):
        BH.doos(W, x1, y0 + v * hv, z1, x2, y0 + v * hv, z2, rand)
    BH.doos(W, x1, y_top, z1, x2, y_top, z2, rand)
    # hoekpilasters
    for (x, z) in ((x1, z1), (x1, z2), (x2, z1), (x2, z2)):
        BH.doos(W, x, y0, z, x, y_top, z, rand)
    lamp = "minecraft:shroomlight"
    donker = "minecraft:pink_terracotta" if palet.MODUS != "vanilla" else "minecraft:brown_terracotta"
    raam = P["raam"]
    # ramen op alle gevels
    for v in range(verd):
        yb = y0 + v * hv + 2
        for (vlak, c, u1, u2, nb) in (("z", z1, x1, x2, -1), ("z", z2, x1, x2, 1), ("x", x1, z1, z2, -1),
                                      ("x", x2, z1, z2, 1)):
            for u in range(u1 + 2, u2 - 1, 3):
                licht = rng.rand() < 0.28
                for dy in (0, 1):
                    if vlak == "z":
                        BH.zet(W, u, yb + dy, c, raam)
                        BH.zet(W, u, yb + dy, c - nb, lamp if licht else donker)
                    else:
                        BH.zet(W, c, yb + dy, u, raam)
                        BH.zet(W, c - nb, yb + dy, u, lamp if licht else donker)
                if v == 0 and rng.rand() < 0.5:  # bloembak
                    if vlak == "z":
                        BH.zet(W, u, yb - 1, c + nb, "minecraft:flowering_azalea_leaves")
                    else:
                        BH.zet(W, c + nb, yb - 1, u, "minecraft:flowering_azalea_leaves")
    # deur
    if voor in ("south", "north"):
        zc = z2 if voor == "south" else z1
        xm = (x1 + x2) // 2
        BH.doos(W, xm, y0 + 1, zc, xm, y0 + 2, zc, "minecraft:cherry_planks")
        BH.zet(W, xm, y0 + 3, zc, rand)
        if winkel:
            for x in range(x1 + 1, x2):
                if x != xm:
                    BH.doos(W, x, y0 + 1, zc, x, y0 + 3, zc, "minecraft:glass")
                    BH.zet(W, x, y0 + 1, zc - (1 if voor == "south" else -1), lamp)
        if luifel:
            dz = 1 if voor == "south" else -1
            for x in range(x1, x2 + 1):
                BH.zet(W, x, y0 + 4, zc + dz, luifel)
    else:
        xc = x2 if voor == "east" else x1
        zm = (z1 + z2) // 2
        BH.doos(W, xc, y0 + 1, zm, xc, y0 + 2, zm, "minecraft:cherry_planks")
        if luifel:
            dx = 1 if voor == "east" else -1
            for z in range(z1, z2 + 1):
                BH.zet(W, xc + dx, y0 + 4, z, luifel)
    dak = _dak_sleutel(DAKEN[rng.randint(len(DAKEN))])
    nok = "x" if voor in ("south", "north") else "z"
    if (x2 - x1) < (z2 - z1) and voor in ("south", "north"):
        nok = "z" if rng.rand() < 0.5 else "x"
    BH.zadeldak(W, x1, z1, x2, z2, y_top + 1, nok=nok, dak=dak, overstek=1, gevel=wand)
    # schoorsteen
    sx, sz = (x1 + 2, z1 + 2) if rng.rand() < 0.5 else (x2 - 2, z2 - 2)
    BH.doos(W, sx, y_top + 1, sz, sx, y_top + 7, sz, P["steen_bak"])
    # dakkapel-oortjes: twee kleine gouden puntjes op de nok van sommige huizen
    return y_top


def huizen(S, gebieden, seed=7, verd=(2, 4)):
    """Pakt de vrije stukken in de gebieden dicht met huizen (gretig: grote kavels eerst, 1 blok steegje ertussen).
    De voorkant kijkt naar de dichtstbijzijnde straat."""
    W = S.W
    rng = np.random.RandomState(seed)
    n = 0
    for (x1, z1, x2, z2) in gebieden:
        kand = [(x, z) for x in range(x1, x2 - 4, 2) for z in range(z1, z2 - 4, 2)]
        rng.shuffle(kand)
        for (x, z) in kand:
            for _ in range(4):
                bw, bd = rng.randint(6, 12), rng.randint(7, 13)
                xa, xb, za, zb = x, x + bw - 1, z, z + bd - 1
                if xb > x2 or zb > z2:
                    continue
                if not S.vrij(xa - 2, za - 2, xb + 2, zb + 2):
                    continue
                gs = S.G[S.ix(xa):S.ix(xb) + 1, S.iz(za):S.iz(zb) + 1]
                y0 = int(np.nanmax(gs))
                ylo = int(np.nanmin(gs))
                if y0 - ylo > 8:
                    continue
                if ylo < y0:
                    BH.doos(W, xa, ylo, za, xb, y0, zb, P["steen_bak"])
                # voorkant: naar de straat die het dichtst bij is
                best, voor = 1e9, "south"
                for (dx, dz, f) in ((0, 1, "south"), (0, -1, "north"), (1, 0, "east"), (-1, 0, "west")):
                    for k in range(1, 12):
                        px = (xa + xb) // 2 + dx * (k + (bw // 2 if dx else 0))
                        pz = (za + zb) // 2 + dz * (k + (bd // 2 if dz else 0))
                        i, j = S.ix(px), S.iz(pz)
                        if 0 <= i < S.m.shape[0] and 0 <= j < S.m.shape[1] and S.straat[i, j]:
                            if k < best:
                                best, voor = k, f
                            break
                import huizen2 as H2
                H2.huis2(W, xa, za, xb, zb, y0, verd=rng.randint(verd[0], verd[1] + 1), seed=rng.randint(1 << 30),
                     voor=voor, winkel=rng.rand() < 0.25,
                     luifel=["minecraft:pink_wool", "minecraft:white_wool", "minecraft:light_blue_wool"][
                         rng.randint(3)] if rng.rand() < 0.3 else None)
                S.reserveer(xa, za, xb, zb)
                n += 1
                break
    return n


def waterval(S, x, kant, breed=4):
    """Waterval van de stadsrand langs de flank van de walvis naar beneden, onderaan in nevel (wolkjes)."""
    W = S.W
    g = None
    for z in range(0, kant * 120, kant):
        gg = S.g(x, z)
        if gg is not None:
            g, z0 = gg, z
    if g is None:
        return
    # goot door de muur
    for dx in range(-breed // 2, breed // 2 + 1):
        for z in range(z0 - kant * 4, z0 + kant * 2, kant):
            BH.zet(W, x + dx, g + 1, z, "minecraft:water")
            BH.zet(W, x + dx, g + 2, z, "minecraft:air")
    y = g
    zlaatst = z0 + kant
    while y > 140:   # [zone 5 kaart] was 70: zone 4 lies below (the game clips zone 5 under y 118); the fall ends in mist
        for dx in range(-breed // 2, breed // 2 + 1):
            zs = WW._zijvlak_z(x + dx + 0.5, y + 0.5, kant)
            if np.isnan(zs):
                # vrije val onder de buik
                zs = zlaatst
                if y < WW.buik(x + 0.5, 0.5) - 12:   # [zone 5 kaart] was -30
                    break
            zlaatst = zs
            BH.zet(W, x + dx, y, int(math.floor(zs + kant * 1.0)), "minecraft:water")
        y -= 1
    for i in range(6):
        W.bol((x + i % 3 - 1, y + 2 + i * 2, zlaatst + kant * 2), 4 - i * 0.4, "guhs:wolkenblok_wit", ry=2.5)


def bloembakken(W, kans=0.6, ymin=226, seed=3):
    """Bloembak onder elk raam (P['raam']) dat aan de buitenkant lucht heeft: één numpy-pass over de hele stad."""
    v = W.v
    ids = [W.idx[s] for s in W.pal if s.split("[")[0] == P["raam"].split("[")[0]]
    if not ids:
        return 0
    glas = np.isin(v, ids)
    j0 = max(1, W.vy(ymin))
    glas[:, :j0, :] = False
    onder_glas = np.zeros_like(glas)
    onder_glas[:, 1:, :] = glas[:, :-1, :]
    onderste = glas & ~onder_glas            # onderste ruit van een raam
    rng = np.random.RandomState(seed)
    bloem = [W.id("minecraft:flowering_azalea_leaves"), W.id("minecraft:azalea_leaves"),
             W.id("minecraft:flowering_azalea_leaves"), W.id("minecraft:cherry_leaves")]
    n = 0
    X, Y, Z = v.shape
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        pts = np.argwhere(onderste)
        if not len(pts):
            continue
        x, y, z = pts[:, 0], pts[:, 1], pts[:, 2]
        bx, by, bz = x + dx, y - 1, z + dz
        ok = (bx >= 0) & (bx < X) & (bz >= 0) & (bz < Z) & (by >= 0)
        x, y, z, bx, by, bz = x[ok], y[ok], z[ok], bx[ok], by[ok], bz[ok]
        ix, iz = np.clip(x - dx, 0, X - 1), np.clip(z - dz, 0, Z - 1)
        buiten = (v[bx, y, bz] == 0) & (v[bx, by, bz] == 0) & (v[ix, y, iz] != 0)
        sel = buiten & (rng.rand(len(x)) < kans)
        v[bx[sel], by[sel], bz[sel]] = np.array(bloem, np.uint16)[rng.randint(0, len(bloem), sel.sum())]
        n += int(sel.sum())
    return n


def bomen_en_lampen(S, seed=11, dicht=0.010):
    W = S.W
    rng = np.random.RandomState(seed)
    vrij = S.m & ~S.bezet & ~S.straat & ~S.rand
    pts = np.argwhere(vrij)
    for (i, j) in pts[rng.rand(len(pts)) < dicht]:
        x, z = int(S.xs[i]), int(S.zs[j])
        g = S.g(x, z)
        if g is None or not S.vrij(x - 3, z - 3, x + 3, z + 3):
            continue
        BH.bloesemboom(W, x, g + 1, z, h=rng.randint(4, 7), r=rng.uniform(3, 4.8), seed=rng.randint(9999),
                       blad="guhs:guhbloesem_leaves" if rng.rand() < 0.7 else "minecraft:cherry_leaves")
        S.reserveer(x - 2, z - 2, x + 2, z + 2)
    # lantaarns langs de laan
    for x in range(-74, 18, 9):
        if -36 < x < -6:
            continue
        for z in (-6, 6):
            g = S.g(x, z)
            if g is not None:
                BH.lantaarnpaal(W, x, g + 1, z, h=4)


# ------------------------------------------------------------------ alles
def bouw(W, delen=None, figuren=True, log=print):
    import gebouwen as GB
    import time
    t = time.time()
    WW.bouw(W)
    log("walvis %.0fs" % (time.time() - t))
    S = Stad(W)
    W.stad = S
    grond(S)
    log("grond %.0fs" % (time.time() - t))
    for naam, (x1, x2, z1, z2, niv) in ZONES.items():
        if naam in ("staartpark", "ooggaarde"):   # [zone 5 kaart] "haven" reserved too: houses were built inside the station
            continue
        S.reserveer(x1, z1, x2, z2)
    stadsmuur(S)
    straten(S)
    log("muur+straten %.0fs" % (time.time() - t))
    GB.alles(W, S, figuren=figuren, log=log)
    n = huizen(S, [(-153, -95, -76, -49), (-153, 49, -76, 95), (16, 37, 128, 100), (16, -100, 128, -37),
                   (124, -95, 172, 95), (-6, 37, 20, 100), (-6, -100, 20, -37), (-74, -30, -6, -25),
                   (-74, 25, -6, 30), (-199, -70, -153, 70)])
    log("huizen: %d, %.0fs" % (n, time.time() - t))
    bomen_en_lampen(S)
    for (x, kant) in ((-175, 1), (-126, -1), (60, 1), (-60, -1)):
        waterval(S, x, kant)
    nb = bloembakken(W)
    log("bloembakken: %d" % nb)
    log("stad klaar %.0fs, palet %d" % (time.time() - t, len(W.pal)))
    return S
