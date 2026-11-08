"""Bouwhulpjes voor Hoogvads (wereldcoördinaten, op een schemerkit-Wereld met k=1).

Alles leest blokken uit palet.P, zodat dezelfde gebouwen in 'vanilla', 'guhs' en 'nieuw' gebouwd kunnen worden.
Richtingen: x oost, z zuid. 'facing' van trappen = de kant van de hoge rug (zoals in het spel).
"""
import math
import numpy as np
import palet
from palet import P, trap, plaat

LUCHT = "minecraft:air"


def b(k):
    """Blok uit het palet (of een letterlijke naam)."""
    return P.get(k, k) if isinstance(k, str) else k


def doos(W, x1, y1, z1, x2, y2, z2, blok, **kw):
    if blok is None:
        return
    W.wfill(min(x1, x2), min(y1, y2), min(z1, z2), max(x1, x2), max(y1, y2), max(z1, z2), b(blok), **kw)


def zet(W, x, y, z, blok):
    if blok is None:
        return
    W.wset(int(x), int(y), int(z), b(blok))


def muren(W, x1, y1, z1, x2, y2, z2, blok):
    doos(W, x1, y1, z1, x2, y2, z1, blok)
    doos(W, x1, y1, z2, x2, y2, z2, blok)
    doos(W, x1, y1, z1, x1, y2, z2, blok)
    doos(W, x2, y1, z1, x2, y2, z2, blok)


def leeg(W, x1, y1, z1, x2, y2, z2):
    W.wfill(min(x1, x2), min(y1, y2), min(z1, z2), max(x1, x2), max(y1, y2), max(z1, z2), LUCHT)


# ------------------------------------------------------------------ zuilen
def zuil(W, x, z, y1, y2, dik=1, schacht="zuil", kop="zuilkop", voet="marmer_glad", kap="goud_plaat"):
    """Zuil met voet (breder), schacht en gouden kapiteel. dik=1 -> 1x1 schacht, dik=3 -> 3x3."""
    h = dik // 2
    doos(W, x - h - 1, y1, z - h - 1, x + h + 1, y1, z + h + 1, voet)
    doos(W, x - h, y1 + 1, z - h, x + h, y2 - 1, z + h, palet.P[schacht] if schacht in P else schacht)
    doos(W, x - h, y2, z - h, x + h, y2, z + h, kop)
    if kap:
        k = plaat(kap) if kap in P and "_plaat" not in P[kap] else (P[kap] + "[type=bottom,waterlogged=false]"
                                                                   if kap in P else kap)
        doos(W, x - h - 1, y2 + 1, z - h - 1, x + h + 1, y2 + 1, z + h + 1, k)


def kap_plaat():
    return P["goud_plaat"] + "[type=bottom,waterlogged=false]"


# ------------------------------------------------------------------ ramen
def boog_vorm(du, breed, h_recht, spits=1.0):
    """Hoogte van de spitsboog boven het rechte stuk op horizontale afstand du van het midden."""
    hw = breed / 2.0
    q = min(1.0, abs(du) / hw)
    return h_recht + hw * spits * (1 - q ** 1.6) ** 0.62


def boograam(W, vlak, c, u_mid, y1, breed, h_recht, glas="glas", kader="goud", spits=1.2, diep=1, kader_dik=1,
             midstijl=None):
    """Spitsboogvenster in een muurvlak. vlak='x': muur op x=c (u = z); vlak='z': muur op z=c (u = x)."""
    hw = breed / 2.0
    for du in np.arange(-hw - kader_dik, hw + kader_dik + 0.01, 1.0):
        u = int(math.floor(u_mid + du))
        top_g = boog_vorm(du, breed, h_recht, spits) if abs(du) <= hw else -1
        top_k = boog_vorm(min(abs(du), hw), breed + 2 * kader_dik, h_recht, spits) + kader_dik
        for y in range(int(y1) - kader_dik, int(y1 + top_k) + 1):
            dy = y - y1
            binnen = abs(du) < hw and 0 <= dy <= top_g
            blok = None
            if binnen:
                blok = glas
                if midstijl and abs(u + 0.5 - u_mid) < 0.6 and dy < h_recht:
                    blok = midstijl
            else:
                blok = kader
            for k in range(diep):
                if vlak == "x":
                    zet(W, c + k, y, u, blok)
                else:
                    zet(W, u, y, c + k, blok)


def rond_raam(W, vlak, c, u_mid, y_mid, r, glas="glas_roos", lood="goud", spaken=12, diep=1, kader="wit_glad",
              ringen=(0.42, 0.7)):
    """Roosvenster: cirkel glas met gouden spaken en ringen, wit kader eromheen."""
    for du in range(-int(r) - 2, int(r) + 3):
        for dy in range(-int(r) - 2, int(r) + 3):
            d = math.hypot(du + 0.5, dy + 0.5)
            if d > r + 1.6:
                continue
            a = math.atan2(dy + 0.5, du + 0.5)
            if d > r:
                blok = kader
            else:
                blok = glas
                if d < r * 0.18:
                    blok = lood
                elif any(abs(d - rr * r) < 0.55 for rr in ringen):
                    blok = lood
                elif d > r * 0.2 and abs(math.sin(a * spaken / 2)) < 0.5 / max(d, 1) * 1.6:
                    blok = lood
            u, y = int(math.floor(u_mid + du)), int(math.floor(y_mid + dy))
            for k in range(diep):
                if vlak == "x":
                    zet(W, c + k, y, u, blok)
                else:
                    zet(W, u, y, c + k, blok)


def guh_roos(W, vlak, c, u_mid, y_mid, r, diep=2, kader="wit_glad"):
    """Het guh-roosvenster: een rond raam met twee kleinere ronde 'oortjes' erboven = een guh-hoofd."""
    rond_raam(W, vlak, c, u_mid, y_mid, r, diep=diep, kader=kader)
    for s in (-1, 1):
        rond_raam(W, vlak, c, u_mid + s * r * 0.78, y_mid + r * 0.92, r * 0.36, glas="glas_hart", spaken=6,
                  diep=diep, kader=kader, ringen=())


# ------------------------------------------------------------------ daken, spitsen, koepels
def zadeldak(W, x1, z1, x2, z2, y0, nok="x", dak="dak_roze", overstek=1, gevel=None, vol=None):
    """Zadeldak met trappen. nok='x': nok loopt langs x (hellingen naar noord/zuid)."""
    vol = vol or dak
    if nok == "x":
        breed = z2 - z1 + 1 + 2 * overstek
        n = (breed + 1) // 2
        for i in range(n):
            y = y0 + i
            za, zb = z1 - overstek + i, z2 + overstek - i
            if za > zb:
                break
            if za == zb:
                doos(W, x1 - overstek, y, za, x2 + overstek, y, za, plaat(dak + "_plaat") if dak + "_plaat" in P
                     else b(vol))
                break
            doos(W, x1 - overstek, y, za, x2 + overstek, y, za, trap(dak + "_trap", "south"))
            doos(W, x1 - overstek, y, zb, x2 + overstek, y, zb, trap(dak + "_trap", "north"))
            if zb - za > 1:
                doos(W, x1, y, za + 1, x1, y, zb - 1, gevel or vol)
                doos(W, x2, y, za + 1, x2, y, zb - 1, gevel or vol)
                if zb - za == 2:
                    doos(W, x1 - overstek, y + 1, za + 1, x2 + overstek, y + 1, za + 1,
                         plaat(dak + "_plaat") if dak + "_plaat" in P else b(vol))
    else:
        breed = x2 - x1 + 1 + 2 * overstek
        n = (breed + 1) // 2
        for i in range(n):
            y = y0 + i
            xa, xb = x1 - overstek + i, x2 + overstek - i
            if xa > xb:
                break
            if xa == xb:
                doos(W, xa, y, z1 - overstek, xa, y, z2 + overstek, plaat(dak + "_plaat") if dak + "_plaat" in P
                     else b(vol))
                break
            doos(W, xa, y, z1 - overstek, xa, y, z2 + overstek, trap(dak + "_trap", "east"))
            doos(W, xb, y, z1 - overstek, xb, y, z2 + overstek, trap(dak + "_trap", "west"))
            if xb - xa > 1:
                doos(W, xa + 1, y, z1, xb - 1, y, z1, gevel or vol)
                doos(W, xa + 1, y, z2, xb - 1, y, z2, gevel or vol)
                if xb - xa == 2:
                    doos(W, xa + 1, y + 1, z1 - overstek, xa + 1, y + 1, z2 + overstek,
                         plaat(dak + "_plaat") if dak + "_plaat" in P else b(vol))


def spits(W, cx, cz, y0, r0, h, dak="dak_roze", hoeken=8, top="goudblok", oortjes=True, rand="goud", krul=0.0):
    """(Achthoekige) spits van straal r0 tot een punt op y0+h, met een gouden knop en guh-oortjes."""
    for i in range(int(h)):
        t = i / float(h)
        r = r0 * (1 - t) ** 1.15 + 0.35
        y = y0 + i
        for dx in range(-int(r0) - 1, int(r0) + 2):
            for dz in range(-int(r0) - 1, int(r0) + 2):
                if hoeken:
                    a = math.atan2(dz, dx)
                    sec = math.pi / hoeken
                    rr = math.hypot(dx, dz) * math.cos((a % (2 * sec)) - sec)
                else:
                    rr = math.hypot(dx, dz)
                if rr <= r and rr > r - 1.6:
                    blok = dak
                    if i == 0 and rand:
                        blok = rand
                    zet(W, cx + dx, y, cz + dz, blok)
    yt = y0 + int(h)
    zet(W, cx, yt, cz, top)
    zet(W, cx, yt + 1, cz, top)
    if oortjes:
        for s in (-1, 1):
            zet(W, cx + s, yt + 2, cz, top)
            zet(W, cx + s, yt + 3, cz, top)
        zet(W, cx, yt + 2, cz, "goudblok")


def koepel(W, cx, cy, cz, r, ry=None, dak="dak_goud", rib="wit_glad", ribben=12, trommel=0, trommel_blok="marmer_glad",
           ramen="glas", lantaarn=True, binnen_leeg=True):
    """Koepel (halve ellipsoïde) op een trommel met ramen, ribben en een lantaarntje bovenop."""
    ry = ry or r
    if trommel:
        for dy in range(trommel):
            for dx in range(-int(r) - 1, int(r) + 2):
                for dz in range(-int(r) - 1, int(r) + 2):
                    d = math.hypot(dx, dz)
                    if r - 1.5 < d <= r:
                        a = math.atan2(dz, dx)
                        blok = trommel_blok
                        if 2 <= dy < trommel - 2 and abs(math.sin(a * ribben / 2)) > 0.55:
                            blok = ramen
                        zet(W, cx + dx, cy + dy, cz + dz, blok)
        cy += trommel
        doos(W, cx - int(r), cy, cz - int(r), cx + int(r), cy, cz + int(r), None)
    for dy in range(int(ry) + 1):
        for dx in range(-int(r) - 1, int(r) + 2):
            for dz in range(-int(r) - 1, int(r) + 2):
                d = (dx / r) ** 2 + (dz / r) ** 2 + (dy / ry) ** 2
                dbin = (dx / (r - 1.6)) ** 2 + (dz / (r - 1.6)) ** 2 + (dy / (ry - 1.6)) ** 2
                if d <= 1 and dbin > 1:
                    a = math.atan2(dz, dx)
                    blok = dak
                    if abs(math.sin(a * ribben / 2)) < 0.12 * (1 + dy / ry) or dy == 0:
                        blok = rib
                    zet(W, cx + dx, cy + dy, cz + dz, blok)
    if lantaarn:
        yt = cy + int(ry)
        for dy in range(5):
            for dx in range(-2, 3):
                for dz in range(-2, 3):
                    if max(abs(dx), abs(dz)) == 2 and (abs(dx) == 2) != (abs(dz) == 2):
                        zet(W, cx + dx, yt + dy, cz + dz, rib if dy in (0, 4) else ("glas" if dy < 4 else rib))
                    elif max(abs(dx), abs(dz)) < 2 and dy == 4:
                        zet(W, cx + dx, yt + dy, cz + dz, "dak_goud")
        spits(W, cx, cz, yt + 5, 2, 6, dak="dak_goud", hoeken=4, rand=None)


def ronde_toren(W, cx, cz, y0, h, r, wand="marmer", rand="wit_glad", dak="dak_roze", dak_h=None, ramen="raam",
                gezicht=False, vaandel=True, kantelen=False):
    """Ronde toren met raampjes, een witte rand bovenaan en een kegeldak (spits) met oortjes."""
    for dy in range(h):
        for dx in range(-r - 1, r + 2):
            for dz in range(-r - 1, r + 2):
                d = math.hypot(dx, dz)
                if r - 1.5 < d <= r:
                    a = math.atan2(dz, dx)
                    blok = wand
                    if dy % 7 in (3, 4) and abs(math.sin(a * 3)) < 0.22 and dy > 2:
                        blok = ramen
                    if dy == h - 1 or dy == 0:
                        blok = rand
                    zet(W, cx + dx, y0 + dy, cz + dz, blok)
    if kantelen:
        for dx in range(-r - 1, r + 2):
            for dz in range(-r - 1, r + 2):
                d = math.hypot(dx, dz)
                if r - 0.5 < d <= r + 1:
                    a = math.atan2(dz, dx)
                    zet(W, cx + dx, y0 + h, cz + dz, rand)
                    if math.sin(a * (r + 2)) > 0:
                        zet(W, cx + dx, y0 + h + 1, cz + dz, rand)
    if gezicht:
        guhgezicht_rond(W, cx, cz, y0 + h - 9, r)
    if dak:
        spits(W, cx, cz, y0 + h, r + 1.5, dak_h or int(r * 2.6), dak=dak, hoeken=0)


def guhgezicht_rond(W, cx, cz, y, r, kant=(1, 0)):
    """Een eenvoudig guhgezicht (ogen + blos + mondje) op de buitenkant van een ronde toren."""
    ux, uz = kant
    for (du, dy, blok) in ((-2, 2, "minecraft:black_concrete"), (2, 2, "minecraft:black_concrete"),
                           (-2, 3, "minecraft:black_concrete"), (2, 3, "minecraft:black_concrete"),
                           (-3, 0, "minecraft:pink_concrete"), (3, 0, "minecraft:pink_concrete"),
                           (0, 0, "minecraft:purple_terracotta")):
        x = cx + ux * r - uz * du
        z = cz + uz * r + ux * du
        zet(W, x, y + dy, z, blok)


def vaandel(W, x, y, z, facing="south"):
    """Hoogvads-vaandel (2 hoog) tegen een muur; facing = kant waar het doek naar kijkt."""
    if P.get("vaandel_onder") is None:
        doos(W, x, y, z, x, y + 1, z, "minecraft:pink_wool")
        return
    if P.get("vaandel_boven"):
        zet(W, x, y + 1, z, P["vaandel_boven"] + f"[facing={facing}]")
        zet(W, x, y, z, P["vaandel_onder"] + f"[facing={facing}]")
    else:
        zet(W, x, y, z, P["vaandel_onder"])


def balustrade(W, x1, z1, x2, z2, y, blok="wit_muur", palen="wit_glad", elke=6):
    """Rechte balustrade (muurtjes) met paaltjes."""
    n = max(abs(x2 - x1), abs(z2 - z1))
    for i in range(n + 1):
        t = i / max(n, 1)
        x = round(x1 + (x2 - x1) * t)
        z = round(z1 + (z2 - z1) * t)
        zet(W, x, y, z, palen if i % elke == 0 else blok)
        if i % elke == 0:
            zet(W, x, y + 1, z, "minecraft:lantern" if i % (elke * 2) == 0 else None)


def lantaarnpaal(W, x, y, z, h=4):
    for dy in range(h):
        zet(W, x, y + dy, z, "minecraft:pale_oak_fence")
    zet(W, x, y + h, z, "goud")
    zet(W, x, y + h + 1, z, "minecraft:lantern")


def bloesemboom(W, x, y, z, h=6, r=4, seed=0, blad="guhs:guhbloesem_leaves", stam="guhs:guhbloesem_log[axis=y]"):
    rng = np.random.RandomState(abs(int(seed)) % (2 ** 31))
    for dy in range(h):
        zet(W, x, y + dy, z, stam)
    W.bol((x + 0.5, y + h + 0.5, z + 0.5), r, blad, ry=r * 0.7, keep=True)
    for k in range(3):
        a = rng.uniform(0, 2 * math.pi)
        W.bol((x + 0.5 + math.cos(a) * r * 0.7, y + h - 1 + rng.uniform(-1, 1), z + 0.5 + math.sin(a) * r * 0.7),
              r * 0.6, blad, ry=r * 0.45, keep=True)


def hekje(W, x1, z1, x2, z2, y, blok="minecraft:pale_oak_fence"):
    n = max(abs(x2 - x1), abs(z2 - z1))
    for i in range(n + 1):
        t = i / max(n, 1)
        zet(W, round(x1 + (x2 - x1) * t), y, round(z1 + (z2 - z1) * t), blok)


def label(W, x, y, z, tekst):
    W.label(x, y, z, tekst)


# ------------------------------------------------------------------ figuren
def guh(W, x, y, z, yaw=180, skin="guh", zit=False):
    W.wguh(x, y, z, yaw, skin=skin, sitting=zit)


def npc(W, soort, x, y, z, yaw=180, schaal=1.0):
    import erfkit as K
    vx, vy, vz = W.naar_voxel((x, y, z))
    try:
        W.extra.extend(K.dier(soort, vx, vy, vz, yaw=yaw, scale=schaal))
    except Exception as e:  # noqa
        print("npc fout", soort, e)


def keermuur(W, x1, z1, x2, z2, ytop, kanten=("n", "z", "w", "o"), stap=9, bodem_fn=None):
    """Sierlijke keermuur rond een plateau (rechthoek x1..x2, z1..z2, bovenkant ytop): bakstenen, witte pilasters,
    blinde spitsbogen met goud, een gouden band en een balustrade. bodem_fn(x, z) -> y van de grond buiten."""
    import wolkwal as WW_

    def bodem(x, z):
        if bodem_fn:
            return bodem_fn(x, z)
        ex, ez = W.echt(x, z) if hasattr(W, "echt") else (x, z)
        dy = W._d[1] if hasattr(W, "_d") else 0
        r = WW_.rug(ex + 0.5, ez + 0.5)
        return int(r) - 2 - dy if not np.isnan(r) else ytop - 30
    vlakken = []
    if "n" in kanten:
        vlakken.append([(x, z1) for x in range(x1, x2 + 1)])
    if "z" in kanten:
        vlakken.append([(x, z2) for x in range(x1, x2 + 1)])
    if "w" in kanten:
        vlakken.append([(x1, z) for z in range(z1, z2 + 1)])
    if "o" in kanten:
        vlakken.append([(x2, z) for z in range(z1, z2 + 1)])
    for pts in vlakken:
        for i, (x, z) in enumerate(pts):
            yb = bodem(x, z)
            k = i % stap
            for y in range(yb, ytop):
                blok = "steen_bak"
                hoogte = ytop - yb
                if k in (0, 1):
                    blok = "wit_glad"
                elif hoogte > 8:
                    du = k - (stap + 1) / 2.0
                    top = yb + 2 + max(0, hoogte - 9) * 0.55 + (stap / 2.0 - 1) * (1 - (abs(du) / (stap / 2.0 - 1)) ** 1.6) ** 0.62
                    if y < top - 1 and y > yb + 1:
                        blok = "marmer"
                    elif y < top + 0.5 and y > yb + 1:
                        blok = "goud"
                if y == ytop - 3:
                    blok = "goudrand"
                zet(W, x, y, z, blok)
            zet(W, x, ytop, z, "wit_glad")
            zet(W, x, ytop + 1, z, "wit_glad" if k == 0 else "wit_muur")
            if k == 0:
                zet(W, x, ytop + 2, z, "minecraft:lantern")


class Schuif:
    """Verschoven blik op een Wereld: alle bouwcalls krijgen (dx, dy, dz) erbij. Zo blijven de gebouwscripts in hun
    eigen coördinaten (uit de eerste ronde) en zetten we ze op de nieuwe plek op de (kortere) walvis."""

    def __init__(self, W, dx=0, dy=0, dz=0):
        object.__setattr__(self, "_W", W)
        object.__setattr__(self, "_d", (dx, dy, dz))

    def __getattr__(self, k):
        return getattr(self._W, k)

    def echt(self, x, z):
        return x + self._d[0], z + self._d[2]

    def wset(self, x, y, z, s, **kw):
        dx, dy, dz = self._d
        self._W.wset(x + dx, y + dy, z + dz, s, **kw)

    def wget(self, x, y, z):
        dx, dy, dz = self._d
        return self._W.wget(x + dx, y + dy, z + dz)

    def wfill(self, x1, y1, z1, x2, y2, z2, s, **kw):
        dx, dy, dz = self._d
        self._W.wfill(x1 + dx, y1 + dy, z1 + dz, x2 + dx, y2 + dy, z2 + dz, s, **kw)

    def bol(self, c, r, *a, **kw):
        dx, dy, dz = self._d
        if "ymin" in kw and kw["ymin"] is not None:
            kw["ymin"] += dy
        if "ymax" in kw and kw["ymax"] is not None:
            kw["ymax"] += dy
        return self._W.bol((c[0] + dx, c[1] + dy, c[2] + dz), r, *a, **kw)

    def buis(self, pts, *a, **kw):
        dx, dy, dz = self._d
        return self._W.buis([(p[0] + dx, p[1] + dy, p[2] + dz) for p in pts], *a, **kw)

    def cilinder(self, cx, cz, r, y1, y2, *a, **kw):
        dx, dy, dz = self._d
        return self._W.cilinder(cx + dx, cz + dz, r, y1 + dy, y2 + dy, *a, **kw)

    def label(self, x, y, z, t):
        dx, dy, dz = self._d
        self._W.label(x + dx, y + dy, z + dz, t)

    def wguh(self, x, y, z, *a, **kw):
        dx, dy, dz = self._d
        self._W.wguh(x + dx, y + dy, z + dz, *a, **kw)

    def naar_voxel(self, p):
        dx, dy, dz = self._d
        return self._W.naar_voxel((p[0] + dx, p[1] + dy, p[2] + dz))
