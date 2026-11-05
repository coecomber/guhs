"""Stadshuizen van Hoogvads (ronde 2): veel meer detail en afwisseling.

huis2(W, x1, z1, x2, z2, y0, verd, seed, voor) bouwt één huis op de kavel x1..x2, z1..z2 (inclusief) met de voorkant
naar 'voor' (north/south/east/west). Per huis gekozen (deterministisch uit seed):
  - gevelkleur + rand/accentkleur, plint, hoekpilasters, verdiepingsbanden
  - overkraging (bovenverdiepingen 1 naar voren, met consoles eronder)
  - ramen: hoog met boogje, dubbel, rond; luiken; bloembakken; sommige verlicht
  - erker (uitbouw met eigen dakje) of balkon met hekje en potten
  - winkel op de begane grond met etalage, luifel en uithangbord
  - daktype: zadeldak langs de straat met dakkapellen, trapgevel naar de straat, steil puntdak, schilddak,
    of een hoektorentje met kegeldak; schoorsteen, gouden oortjes op de nok
"""
import math
import numpy as np
import palet
from palet import P, trap, plaat
import bouwhulp as BH

GEVELS = ["pastelbaksteen_roze", "pastelbaksteen_vanille", "pastelbaksteen_perzik", "pastelbaksteen_lavendel",
          "pastelbaksteen_framboos", "pastelbaksteen_orchidee", "pastelbaksteen_wolkje", "pleister_roze",
          "pleister_vanille", "pleister_orchidee", "pleister_framboos", "pastelbaksteen_aqua", "pastelbaksteen_boter",
          "pleister_wolkje", "pastelbaksteen_hemelsblauw", "pastelbaksteen_pistache"]
GEVELS_VAN = ["minecraft:pink_terracotta", "minecraft:white_terracotta", "minecraft:orange_terracotta",
              "minecraft:magenta_terracotta", "minecraft:cherry_planks", "minecraft:pale_oak_planks",
              "minecraft:light_blue_terracotta", "minecraft:pink_concrete", "minecraft:white_concrete"]
DAKEN = ["dak_roze", "dak_roze", "dak_wit", "guhs:dakpan_kersrood", "guhs:dakpan_hemelsblauw", "guhs:dakpan_lavendel",
         "guhs:dakpan_mint", "dak_roze", "dak_goud", "guhs:dakpan_perzik"]
LUIK = ["minecraft:cherry_trapdoor", "minecraft:pale_oak_trapdoor", "minecraft:birch_trapdoor",
        "minecraft:mangrove_trapdoor", "minecraft:warped_trapdoor"]
BLOEM = ["minecraft:flowering_azalea_leaves", "minecraft:flowering_azalea_leaves", "minecraft:azalea_leaves",
         "minecraft:cherry_leaves"]
TEGEN = {"north": "south", "south": "north", "east": "west", "west": "east"}


def dak_sleutel(d):
    if d in P:
        return d
    if palet.MODUS == "vanilla":
        return "dak_roze"
    P[d], P[d + "_trap"], P[d + "_plaat"] = d, d + "_trap", d + "_plaat"
    return d


class Kavel:
    """Lokaal stelsel: u langs de voorgevel (0..w-1), v de diepte (0 = voorgevel, <0 = ervoor), y omhoog."""

    def __init__(self, x1, z1, x2, z2, voor):
        self.voor = voor
        if voor in ("south", "north"):
            self.w, self.d = x2 - x1 + 1, z2 - z1 + 1
        else:
            self.w, self.d = z2 - z1 + 1, x2 - x1 + 1
        self.x1, self.z1, self.x2, self.z2 = x1, z1, x2, z2
        self.uit = voor
        self.binnen = TEGEN[voor]
        # links/rechts kijkend vanaf de straat naar de gevel
        self.zij = {"south": ("west", "east"), "north": ("east", "west"), "east": ("south", "north"),
                    "west": ("north", "south")}[voor]

    def p(self, u, y, v):
        if self.voor == "south":
            return self.x1 + u, y, self.z2 - v
        if self.voor == "north":
            return self.x2 - u, y, self.z1 + v
        if self.voor == "east":
            return self.x2 - v, y, self.z1 + u
        return self.x1 + v, y, self.z2 - u


def zet(W, K, u, y, v, b):
    x, y_, z = K.p(u, y, v)
    BH.zet(W, x, y_, z, b)


def huis2(W, x1, z1, x2, z2, y0, verd=3, seed=0, voor="south", winkel=False, luifel=None):
    rng = np.random.RandomState(abs(int(seed)) % (2 ** 31))
    K = Kavel(x1, z1, x2, z2, voor)
    w, d = K.w, K.d
    if palet.MODUS == "vanilla":
        wand = GEVELS_VAN[rng.randint(len(GEVELS_VAN))]
    else:
        wand = "guhs:" + GEVELS[rng.randint(len(GEVELS))]
    rand = P["wit_glad"] if rng.rand() < 0.7 else P["marmer_glad"]
    hv = int(rng.choice([4, 5, 5]))
    y_top = y0 + verd * hv
    over = rng.rand() < 0.45 and verd >= 2           # overkraging
    raam = P["raam"]
    lamp, donker = "minecraft:shroomlight", ("minecraft:pink_terracotta" if palet.MODUS != "vanilla"
                                             else "minecraft:brown_terracotta")
    luik = LUIK[rng.randint(len(LUIK))]
    # romp
    BH.doos(W, x1, y0, z1, x2, y0, z2, P["steen_bak"])
    BH.doos(W, x1, y0 + 1, z1, x2, y_top - 1, z2, wand)
    for v_ in range(1, verd):
        BH.doos(W, x1, y0 + v_ * hv, z1, x2, y0 + v_ * hv, z2, rand)
    BH.doos(W, x1, y_top, z1, x2, y_top, z2, rand)
    for (x, z) in ((x1, z1), (x1, z2), (x2, z1), (x2, z2)):
        BH.doos(W, x, y0, z, x, y_top, z, rand)
    # overkraging: bovenverdiepingen 1 naar voren op consoles
    if over:
        for u in range(0, w):
            zet(W, K, u, y0 + hv, -1, trap("marmer_trap", K.binnen, "top") if palet.MODUS != "vanilla"
                else trap("wit_trap", K.binnen, "top"))
            for y in range(y0 + hv + 1, y_top):
                zet(W, K, u, y, -1, wand)
            zet(W, K, u, y_top, -1, rand)
        for v_ in range(1, verd):
            for u in range(0, w):
                zet(W, K, u, y0 + v_ * hv, -1, rand)
        for y in range(y0 + hv, y_top + 1):
            zet(W, K, 0, y, -1, rand)
            zet(W, K, w - 1, y, -1, rand)
    # ramen per verdieping
    soort = rng.choice(["boog", "dubbel", "hoog", "rond"])
    for vd in range(verd):
        yb = y0 + vd * hv + 2
        vv = -1 if (over and vd >= 1) else 0
        for (gevel, vpos) in (("voor", vv), ("achter", d - 1)):
            us = list(range(2, w - 2, 3)) if w >= 6 else [w // 2]
            for u in us:
                if gevel == "voor" and vd == 0 and winkel:
                    continue
                licht = rng.rand() < 0.3
                h = 2 if hv >= 5 else 1
                if soort == "rond" and vd == verd - 1:
                    h = 1
                n_raam = []
                for dy in range(h):
                    n_raam.append((u, yb + dy))
                if soort == "dubbel" and u + 1 < w - 1:
                    for dy in range(h):
                        n_raam.append((u + 1, yb + dy))
                inw = 1 if gevel == "voor" else -1
                for (uu, yy) in n_raam:
                    zet(W, K, uu, yy, vpos, raam)
                    zet(W, K, uu, yy, vpos + inw, lamp if licht else donker)
                if gevel == "voor":
                    # boogje of druiplijst erboven
                    if soort in ("boog", "hoog"):
                        zet(W, K, u, yb + h, vpos - 1, trap("wit_trap", K.binnen, "top"))
                    # luiken
                    if rng.rand() < 0.55 and soort != "dubbel":
                        for dy in range(h):
                            for (du, kant) in ((-1, K.zij[0]), (1, K.zij[1])):
                                if 0 < u + du < w - 1:
                                    zet(W, K, u + du, yb + dy, vpos - 1,
                                        f"{luik}[facing={K.uit},half=bottom,open=true,powered=false,waterlogged=false]")
                    # bloembak
                    if rng.rand() < 0.8:
                        zet(W, K, u, yb - 1, vpos - 1, BLOEM[rng.randint(len(BLOEM))])
                        if soort == "dubbel" and u + 1 < w - 1:
                            zet(W, K, u + 1, yb - 1, vpos - 1, BLOEM[rng.randint(len(BLOEM))])
    # zijgevels: een paar raampjes
    for vd in range(1, verd):
        yb = y0 + vd * hv + 2
        for v_ in range(2, d - 2, 4):
            for kant_u in (0, w - 1):
                zet(W, K, kant_u, yb, v_, raam)
    # deur (+ portiekje)
    du = w // 2
    zet(W, K, du, y0 + 1, 0, "minecraft:cherry_planks")
    zet(W, K, du, y0 + 2, 0, "minecraft:cherry_planks")
    zet(W, K, du, y0 + 3, -1, plaat("wit_plaat", "top"))
    zet(W, K, du, y0, -1, trap("wit_trap", K.binnen))
    zet(W, K, du + 1 if du + 1 < w - 1 else du - 1, y0 + 2, -1, "minecraft:lantern[hanging=false,waterlogged=false]")
    # winkel: etalage, luifel, uithangbord
    if winkel:
        for u in range(1, w - 1):
            if u == du:
                continue
            for y in (y0 + 1, y0 + 2):
                zet(W, K, u, y, 0, "minecraft:glass")
                zet(W, K, u, y, 1, lamp)
        kl = luifel or ["minecraft:pink_wool", "minecraft:light_blue_wool", "minecraft:yellow_wool",
                        "minecraft:lime_wool"][rng.randint(4)]
        kc = kl.replace("_wool", "_carpet")
        for u in range(0, w):
            zet(W, K, u, y0 + 4, -1, kl if u % 2 else "minecraft:white_wool")
            zet(W, K, u, y0 + 3, -2, kc if u % 2 else "minecraft:white_carpet")
        zet(W, K, 0, y0 + 3, -2, "minecraft:iron_chain[axis=y,waterlogged=false]")
    # erker of balkon
    keuze = rng.rand()
    vstart = -1 if over else 0
    if keuze < 0.35 and verd >= 2 and w >= 7:
        ue = int(rng.randint(1, w - 4))
        ve = vstart - 2
        for vd in range(1, verd):
            yb = y0 + vd * hv
            for u in range(ue, ue + 3):
                zet(W, K, u, yb, ve, rand)
                zet(W, K, u, yb, ve + 1, rand)
                for y in range(yb + 1, yb + hv):
                    for vv in (ve, ve + 1):
                        zet(W, K, u, y, vv, raam if (y - yb) in (2, 3) else wand)
            for y in range(yb + 1, yb + hv):
                zet(W, K, ue, y, ve, rand)
                zet(W, K, ue + 2, y, ve, rand)
        # consoles + dakje
        zet(W, K, ue + 1, y0 + hv - 1, ve + 1, trap("wit_trap", K.binnen, "top"))
        yr = y0 + verd * hv
        for u in range(ue - 1, ue + 4):
            zet(W, K, u, yr, ve - 1, trap(dak_sleutel("dak_roze") + "_trap", K.binnen))
        for u in range(ue, ue + 3):
            zet(W, K, u, yr + 1, ve, trap(dak_sleutel("dak_roze") + "_trap", K.binnen))
        zet(W, K, ue + 1, yr + 2, ve + 1, "goud")
    elif keuze < 0.65 and verd >= 2:
        vd = int(rng.randint(1, verd))
        yb = y0 + vd * hv
        ub = (int(rng.randint(1, max(2, w - 5))), None)[0]
        bw = min(5, w - 2)
        for u in range(ub, ub + bw):
            zet(W, K, u, yb, vstart - 1, plaat("wit_plaat", "top"))
            zet(W, K, u, yb + 1, vstart - 2, "minecraft:pale_oak_fence")
            if u in (ub, ub + bw - 1):
                zet(W, K, u, yb + 1, vstart - 1, "minecraft:pale_oak_fence")
        zet(W, K, ub + 1, yb + 1, vstart - 1, "minecraft:potted_pink_tulip")
        zet(W, K, ub + bw - 2, yb + 1, vstart - 1, "minecraft:flowering_azalea_leaves")
        zet(W, K, ub + bw // 2, yb + 1, vstart, "minecraft:cherry_planks")
        zet(W, K, ub + bw // 2, yb + 2, vstart, "minecraft:cherry_planks")
    # dak
    dak = dak_sleutel(DAKEN[rng.randint(len(DAKEN))])
    soort_dak = rng.choice(["langs", "langs", "trapgevel", "steil", "schild", "torentje"])
    ox1, oz1, ox2, oz2 = x1, z1, x2, z2
    if over:
        fx, fy, fz = K.p(0, 0, -1)
        gx, gy, gz = K.p(w - 1, 0, -1)
        ox1, ox2 = min(ox1, fx, gx), max(ox2, fx, gx)
        oz1, oz2 = min(oz1, fz, gz), max(oz2, fz, gz)
    straat_as = "x" if voor in ("south", "north") else "z"
    dwars_as = "z" if straat_as == "x" else "x"
    if soort_dak == "langs":
        BH.zadeldak(W, ox1, oz1, ox2, oz2, y_top + 1, nok=straat_as, dak=dak, overstek=1, gevel=wand)
        # dakkapellen op de voorkant
        for u in range(2, w - 2, 5):
            for y in range(y_top + 1, y_top + 4):
                zet(W, K, u, y, vstart + 1, wand)
                zet(W, K, u + 1, y, vstart + 1, wand)
            zet(W, K, u, y_top + 2, vstart + 1, raam)
            zet(W, K, u + 1, y_top + 2, vstart + 1, raam)
            zet(W, K, u, y_top + 4, vstart + 1, trap(dak + "_trap", K.zij[1]))
            zet(W, K, u + 1, y_top + 4, vstart + 1, trap(dak + "_trap", K.zij[0]))
    elif soort_dak in ("trapgevel", "steil"):
        BH.zadeldak(W, ox1, oz1, ox2, oz2, y_top + 1, nok=dwars_as, dak=dak, overstek=0 if soort_dak == "trapgevel"
                    else 1, gevel=wand)
        if soort_dak == "trapgevel":
            n = (w + 1) // 2
            for i in range(n):
                for u in (i, w - 1 - i):
                    zet(W, K, u, y_top + 1 + i, vstart, rand if i % 2 == 0 else wand)
                    zet(W, K, u, y_top + 2 + i, vstart, rand)
            zet(W, K, w // 2, y_top + n + 1, vstart, "goud")
            zet(W, K, w // 2, y_top + max(2, n - 2), vstart - 1 + 0, raam)
        else:
            # steil: tweede, hogere laag erop (extra puntig) + rond raampje in de top
            zet(W, K, w // 2, y_top + 2, vstart, raam)
    elif soort_dak == "schild":
        piramidedak(W, ox1, oz1, ox2, oz2, y_top + 1, dak)
    else:
        BH.zadeldak(W, ox1, oz1, ox2, oz2, y_top + 1, nok=straat_as, dak=dak, overstek=1, gevel=wand)
        cx, _, cz = K.p(0, 0, vstart)
        torentje(W, cx, cz, y0 + hv, y_top + 5, wand, rand, dak)
    # schoorsteen met kapje
    sx, _, sz = K.p(int(rng.randint(1, w - 1)), 0, d - 2)
    BH.doos(W, sx, y_top + 1, sz, sx, y_top + 2 + (w + d) // 3, sz, P["steen_bak"])
    BH.zet(W, sx, y_top + 3 + (w + d) // 3, sz, plaat("steen_plaat"))
    # gouden oortjes op de nok
    if rng.rand() < 0.4:
        for (u, v_) in ((0, d // 2), (w - 1, d // 2)):
            x, _, z = K.p(u, 0, v_)
            BH.zet(W, x, y_top + 1 + (min(w, d) + 1) // 2 + 1, z, "goud")
    return y_top


def piramidedak(W, x1, z1, x2, z2, y0, dak):
    i = 0
    xa, xb, za, zb = x1 - 1, x2 + 1, z1 - 1, z2 + 1
    while xa <= xb and za <= zb:
        y = y0 + i
        if xb - xa <= 1 or zb - za <= 1:
            BH.doos(W, xa, y, za, xb, y, zb, plaat(dak + "_plaat"))
            BH.zet(W, (xa + xb) // 2, y + 1, (za + zb) // 2, "goud")
            break
        for x in range(xa, xb + 1):
            BH.zet(W, x, y, za, trap(dak + "_trap", "south"))
            BH.zet(W, x, y, zb, trap(dak + "_trap", "north"))
        for z in range(za + 1, zb):
            BH.zet(W, xa, y, z, trap(dak + "_trap", "east"))
            BH.zet(W, xb, y, z, trap(dak + "_trap", "west"))
        BH.doos(W, xa + 1, y, za + 1, xb - 1, y, zb - 1, P[dak])
        xa, xb, za, zb = xa + 1, xb - 1, za + 1, zb - 1
        i += 1


def torentje(W, cx, cz, y0, y1, wand, rand, dak):
    """Rond hoektorentje (straal 2) met raampjes en een spits kegeldak met gouden knop."""
    for y in range(y0, y1 + 1):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                dd = math.hypot(dx, dz)
                if dd <= 2.3:
                    b = wand if dd > 1.2 else wand
                    if (y - y0) % 4 == 2 and (dx == 0 or dz == 0) and dd > 1.5:
                        b = P["raam"]
                    if y == y1:
                        b = rand
                    BH.zet(W, cx + dx, y, cz + dz, b)
    BH.spits(W, cx, cz, y1 + 1, 3.0, 8, dak=dak, hoeken=0, rand=None, oortjes=False)
