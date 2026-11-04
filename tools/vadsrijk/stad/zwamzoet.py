"""Zwamzoet: hoofdstad van zone 1 van Het Vadsrijk (Guhs 1.10.0). Generator: bouw(S) zet de hele stad.

Elk onderdeel is een losse functie (zwam, huis, brug, lift, wenteltrap, tempel, plein, markt, haven,
luchtschip ...), zodat ze later als echte stempels gebruikt kunnen worden.
Coördinaten: x oost, y omhoog, z zuid. Scene = 256 x 200 x 256, maaiveld G = 8 (bovenste grasblok).
"""
import math, random
import numpy as np
import stadkit as SK

G = 8
MAAT = (256, 200, 256)
MUSH = SK.MUSH
ROOD, BRUIN, STEEL = SK.ROOD, SK.BRUIN, SK.STEEL
HOED = {"roze": "guhpaddenstoel_hoed", "rood": ROOD, "bruin": BRUIN}
VLEK = {"roze": "knuffelblok_vanille", "rood": "minecraft:white_wool", "bruin": "knuffelblok_roze"}
GLIM = "minecraft:pearlescent_froglight[axis=y]"
LANT = "minecraft:lantern[hanging=false,waterlogged=false]"
LANT_H = "minecraft:lantern[hanging=true,waterlogged=false]"
HEK = "minecraft:cherry_fence"
DEK = "minecraft:cherry_slab[type=bottom,waterlogged=false]"
PLANK = "minecraft:cherry_planks"
KLINK = "knuffelklinkers"
RAAM = "glasinlood_guh"
LUCHT = "minecraft:cave_air"     # air that also clears the map terrain under a stamp (the tile writer's CARVE)
LAMP_O, LAMP_B = "guh_lantaarnpaal[half=lower]", "guh_lantaarnpaal[half=upper]"
PADEN = []                       # (x1, z1, x2, z2, b) of every ground path, for the lamp posts along them
DEURVOOR = []                    # cells in front of stem doors that must stay free
WENTEL_UIT = {}                  # naam -> angle where the spiral stair comes out on the cap


def lantaarnpaal(S, x, y, z):
    """A whole guh lamp post: BOTH halves (the block name alone is only the bottom half, the lamp top goes missing)."""
    S.set(x, y, z, LAMP_O)
    S.set(x, y + 1, z, LAMP_B)

# naam: (cx, cz, T = vloer van het hoedplateau, r = hoedstraal, rs = steelstraal, kleur, plateau-fractie, kromming)
ZWAMMEN = {
    "tempel":  (128, 86, 118, 28, 9.0, "roze", 0.74, 0.0),
    "west":    (62, 98, 88, 19, 6.5, "rood", 0.66, -3.0),
    "oost":    (194, 92, 98, 20, 7.0, "roze", 0.66, 3.0),
    "zuidw":   (50, 160, 66, 16, 5.5, "bruin", 0.64, -2.0),
    "zuido":   (206, 160, 74, 17, 6.0, "rood", 0.64, 2.5),
    "noordw":  (94, 36, 72, 16, 5.5, "bruin", 0.64, -2.0),
    "noordo":  (168, 34, 64, 15, 5.0, "roze", 0.64, 2.0),
    "hoekw":   (24, 52, 60, 13, 4.5, "rood", 0.62, -1.5),
    "hoeko":   (232, 40, 62, 13, 4.5, "bruin", 0.62, 1.5),
}
# kleine lampzwammetjes (cx, cz, hoogte, r, kleur)
KLEIN = [(98, 140, 30, 8, "rood"), (162, 134, 36, 9, "roze"), (26, 112, 40, 9, "roze"), (232, 112, 34, 8, "rood"),
         (82, 200, 22, 7, "roze"), (178, 196, 26, 7, "bruin"), (16, 196, 28, 7, "rood"), (240, 200, 24, 7, "roze"),
         (60, 60, 26, 7, "roze"), (204, 54, 24, 6, "rood")]
BRUGGEN = [("tempel", "west"), ("tempel", "oost"), ("tempel", "noordw"), ("tempel", "noordo"), ("west", "zuidw"),
           ("oost", "zuido"), ("west", "noordw"), ("oost", "hoeko"), ("noordo", "hoeko")]
SPORENPADEN = [("hoekw", "west"), ("noordw", "hoekw")]
LIFTEN = [("tempel", 90), ("oost", 200), ("west", -10)]   # hoek in graden: 0 = oost, 90 = zuid
WENTEL = ["zuidw", "zuido", "noordw", "noordo", "hoekw", "hoeko"]
PLEIN = (128, 178, 22)


def ruis(x, y, z):
    return math.sin(x * 0.47 + z * 0.13) + math.sin(z * 0.51 - y * 0.21) + math.sin(y * 0.63 + x * 0.29)


# ---------------------------------------------------------------- terrein
def terrein(S):
    X, Y, Z = S.v.shape
    S.v[:, 0:G - 3, :] = S.id("minecraft:stone")
    S.v[:, G - 3:G, :] = S.id("minecraft:dirt")
    S.v[:, G, :] = S.id("knuffelgras")
    # haven: water vanaf z ~ 216 (golvende kustlijn)
    for x in range(X):
        kust = 216 + int(3 * math.sin(x * 0.05))
        for z in range(kust, Z):
            S.v[x, 0:3, z] = S.id("minecraft:sand")
            S.v[x, 3:G + 1, z] = S.id("minecraft:water")
    # kade
    for x in range(X):
        kust = 216 + int(3 * math.sin(x * 0.05))
        if 30 < x < 236:
            for z in range(208, kust + 1):
                for y in range(1, G + 1):
                    S.set(x, y, z, "knuffelsteen" if y < G else KLINK)
            S.set(x, G + 1, kust, HEK if x % 6 else LANT)


def pad(S, x1, z1, x2, z2, b=3, blok=KLINK, rand="tegel_hartjes"):
    PADEN.append((x1, z1, x2, z2, b))
    n = int(max(abs(x2 - x1), abs(z2 - z1))) + 1
    for i in range(n + 1):
        t = i / n
        x, z = x1 + (x2 - x1) * t, z1 + (z2 - z1) * t
        for dx in range(-b, b + 1):
            for dz in range(-b, b + 1):
                if S.get(int(x + dx), G, int(z + dz)) == "guhs:knuffelgras":
                    rr = max(abs(dx), abs(dz))
                    S.set(x + dx, G, z + dz, rand if rr == b else blok)


# ---------------------------------------------------------------- paddenstoelen
def hoed_profiel(d, T, r, p):
    drop = 0.42 * r
    if d <= p * r:
        top = T
    else:
        f = (d - p * r) / ((1 - p) * r)
        top = T - drop * f ** 1.6
    under = T - drop - 2 - 0.16 * r * (1 - min(1.0, d / r) ** 2)
    return int(round(top)), int(round(under))


def steel_straal(rs, t):
    return rs * (1 + 0.75 * math.exp(-t * 9)) * (1 - 0.12 * t)


def zwam(S, cx, cz, T, r, rs, kleur, p=0.65, krom=0.0, ramen=True):
    """Reuzenpaddenstoel: steel van G+1 tot onder de hoed, hoed met plat plateau (vloer = T) en lamellen."""
    hoed, vlek = HOED[kleur], VLEK[kleur]
    _, onder_mid = hoed_profiel(0, T, r, p)
    h = onder_mid + 2 - G
    # steel
    for y in range(G + 1, onder_mid + 3):
        t = (y - G) / h
        rr = steel_straal(rs, t)
        ox = krom * math.sin(math.pi * t)
        sx = cx + ox
        for x in range(int(sx - rr - 1), int(sx + rr + 2)):
            for z in range(int(cz - rr - 1), int(cz + rr + 2)):
                d = math.hypot(x - sx, z - cz)
                if d <= rr:
                    blok = STEEL
                    if d > rr - 1.2 and ruis(x, y, z) > 2.3:
                        blok = "schimmelsteel[axis=y]"
                    S.set(x, y, z, blok)
        # raampjes en deurtje in de steel
        if ramen and y > G + 6 and (y - G) % 9 in (0, 1) and y < onder_mid - 3:
            for k in range(5):
                a = k * 2 * math.pi / 5 + (y - G) // 9 * 0.7
                wx, wz = sx + (rr - 0.3) * math.cos(a), cz + (rr - 0.3) * math.sin(a)
                S.set(wx, y, wz, RAAM)
    if ramen:
        steeldeur(S, cx, cz)
    # hoed
    scx = cx + krom * 0  # bovenkant staat recht boven de voet
    for x in range(int(scx - r - 1), int(scx + r + 2)):
        for z in range(int(cz - r - 1), int(cz + r + 2)):
            d = math.hypot(x - scx, z - cz)
            if d > r + 0.4:
                continue
            top, under = hoed_profiel(d, T, r, p)
            for y in range(under, top + 1):
                blok = hoed
                if y == under and d > rs + 1:
                    a = math.atan2(z - cz, x - scx)
                    streep = int((a + math.pi) * r / 1.7)
                    blok = "minecraft:pink_terracotta" if streep % 2 else STEEL
                    if streep % 6 == 0 and int(d) % 5 == 0:
                        blok = "minecraft:shroomlight"
                elif d > p * r + 0.5 and y >= top - 1 and ruis(x * 1.3, y, z * 1.3) > 2.05:
                    blok = GLIM if (x + z) % 3 == 0 else vlek
                elif d > r - 1.5 and y <= under + 1:
                    blok = hoed
                S.set(x, y, z, blok)
            if d > r - 1.2 and (round(math.atan2(z - cz, x - scx) * r) % 6 == 0):
                S.set(x, under - 1, z, LANT_H)
    # plateaurand: lantaarns op paaltjes
    pr = p * r
    n = int(2 * math.pi * pr / 9)
    for k in range(n):
        a = 2 * math.pi * k / n
        x, z = scx + pr * math.cos(a), cz + pr * math.sin(a)
        S.set(x, T + 1, z, HEK)
        S.set(x, T + 2, z, LANT)
    # ringpad op het plateau
    for x in range(int(scx - pr), int(scx + pr) + 1):
        for z in range(int(cz - pr), int(cz + pr) + 1):
            d = math.hypot(x - scx, z - cz)
            if pr * 0.55 <= d <= pr * 0.55 + 1.6:
                S.set(x, T, z, KLINK)


def steeldeur(S, cx, cz):
    """Door on the south side of a big stem, with a real little hall behind it (the stem is solid) and a recess in
    front of it (the stem's foot flares out wider than the door). Small lamp mushrooms get no door at all."""
    # outermost stem block in the door column at the foot
    dz = cz
    while S.get(cx, G + 1, dz + 1) in (STEEL, "minecraft:mushroom_stem" + MUSH) or \
            S.get(cx, G + 1, dz + 1).startswith("guhs:schimmelsteel"):
        dz += 1
    # hall: 5 wide, 5 deep, 3 high, plank floor with a carpet, lights, a bench and a barrel
    S.fill(cx - 2, G + 1, dz - 5, cx + 2, G + 3, dz - 1, LUCHT)
    S.fill(cx - 2, G, dz - 5, cx + 2, G, dz - 1, "roze_lakhout_planken")
    S.fill(cx, G + 1, dz - 4, cx, G + 1, dz - 1, "minecraft:pink_carpet")
    S.set(cx - 2, G + 3, dz - 5, LANT_H)
    S.set(cx + 2, G + 3, dz - 5, LANT_H)
    S.set(cx - 2, G + 1, dz - 5, "minecraft:barrel[facing=up,open=false]")
    S.set(cx + 2, G + 1, dz - 5, "minecraft:flower_pot")
    S.set(cx - 2, G + 1, dz - 3, "minecraft:cherry_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]")
    S.set(cx + 2, G + 1, dz - 3, "minecraft:cherry_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]")
    S.set(cx, G + 3, dz - 3, "minecraft:shroomlight")
    S.set(cx, G + 1, dz, "roze_lakhout_deur[facing=south,half=lower,hinge=left,open=false]")
    S.set(cx, G + 2, dz, "roze_lakhout_deur[facing=south,half=upper,hinge=left,open=false]")
    S.set(cx, G + 3, dz, "pastelbaksteen_framboos")
    for x in (cx - 1, cx + 1):
        if S.is_air(x, G + 1, dz + 1):
            S.set(x, G + 1, dz + 1, LANT)
    for z in range(dz + 1, dz + 4):
        S.set(cx, G, z, KLINK)
        DEURVOOR.append((cx, z))


def deuren_vrij(S):
    """Clear the cells in front of the stem doors (spiral stairs / trees must not end up there)."""
    for (x, z) in DEURVOOR:
        for y in (G + 1, G + 2):
            if not S.get(x, y, z).startswith("minecraft:lantern"):
                S.set(x, y, z, "minecraft:air")


def lampzwam(S, cx, cz, h, r, kleur):
    """Klein lampzwammetje (20-40 hoog) met een lichtgevende hoed: puur skyline."""
    T = G + h
    zwam(S, cx, cz, T, r, max(2.0, r * 0.3), kleur, p=0.35, ramen=False)
    S.set(cx, T + 1, cz, GLIM)
    S.set(cx, T + 2, cz, "minecraft:end_rod[facing=up]")


# ---------------------------------------------------------------- guh-dingen
def vaandel(S, x, y, z, as_="x", kleuren=("minecraft:pink_wool", "minecraft:white_wool")):
    """Guh banner: a cherry pole (7 high) with a 2 x 3 wool cloth and a little ear tip on top. as_ = cloth direction."""
    for k in range(7):
        S.set(x, y + k, z, "minecraft:stripped_cherry_log[axis=y]" if k == 0 else HEK)
    S.set(x, y + 7, z, "minecraft:pink_concrete")
    for u in (1, 2):
        for v in (3, 4, 5):
            b = kleuren[(u + v) % 2]
            S.set(x + (u if as_ == "x" else 0), y + v, z + (u if as_ == "z" else 0), b)


def oortje(S, x, y, z, as_="x", groot=1):
    """Guh-oortje (rechtop plaatje). as_ = langs welke as het plaatje loopt."""
    rij = [(-1, 0), (0, 0), (1, 0), (-1, 1), (0, 1), (1, 1), (0, 2)] if groot == 1 else \
          [(u, v) for v in range(0, 5) for u in range(-2, 3) if (u * u) / 6.5 + ((v - 1) ** 2) / 10 <= 1]
    for (u, v) in rij:
        binnen = abs(u) < groot and v < 1 + groot
        blok = "minecraft:pink_concrete" if binnen else "minecraft:pink_wool"
        if as_ == "x":
            S.set(x + u, y + v, z, blok)
        else:
            S.set(x, y + v, z + u, blok)


def gezicht(put, w, h, oogkleur="minecraft:light_blue_wool"):
    """Pixel-guhgezicht op een vlak. put(u, v, blok): u = 0..w-1 links->rechts (zoals de kijker ziet),
    v = 0..h-1 onder->boven. Grote ogen met glimlichtje, blosjes en een klein w-mondje."""
    s = w / 14.0
    ro = max(1.6, 2.4 * s)
    for (ou, kant) in ((w * 0.3, -1), (w * 0.7, 1)):
        ov = h * 0.55
        for u in range(int(ou - ro - 1), int(ou + ro + 2)):
            for v in range(int(ov - ro * 1.2 - 1), int(ov + ro * 1.2 + 2)):
                du, dv = (u + 0.5 - ou) / ro, (v + 0.5 - ov) / (ro * 1.2)
                dd = du * du + dv * dv
                if dd > 1:
                    continue
                blok = "minecraft:black_wool"
                if 0.12 < dd < 0.62 and dv < 0.2:
                    blok = oogkleur
                if du < -0.15 and dv > 0.2 and (du + 0.45) ** 2 + (dv - 0.5) ** 2 < 0.09:
                    blok = "minecraft:white_concrete"
                put(u, v, blok)
        # blosje
        bu, bv = ou + kant * ro * 0.9, h * 0.55 - ro * 1.75
        for u in range(int(bu - ro * 0.8), int(bu + ro * 0.8) + 1):
            for v in range(int(bv - 0.6), int(bv + 0.6) + 1):
                put(u, v, "minecraft:pink_terracotta" if s > 0.6 else "minecraft:pink_concrete")
    # mondje (klein 'w')
    mu, mv = w / 2.0, h * 0.55 - ro * 1.6
    for du, dv in ((-1, 0), (0, 1), (1, 0)):
        put(int(mu + du * max(1, s)), int(mv + dv), "minecraft:black_wool")


def guhbeeld(S, cx, y0, cz, w, lijf="knuffelblok_roze", oog="minecraft:light_blue_wool"):
    """Knuffelig guhbeeld (kijkt naar het zuiden). w = breedte in blokken."""
    hh, dd = int(w * 0.85), int(w * 0.8)
    x1, z1 = cx - w // 2, cz - dd // 2
    for x in range(x1, x1 + w):
        for y in range(y0, y0 + hh):
            for z in range(z1, z1 + dd):
                # afgeronde doos
                ex = max(0, abs(x + 0.5 - cx) - (w / 2 - 2.2)) / 2.2
                ey = max(0, abs(y + 0.5 - (y0 + hh / 2)) - (hh / 2 - 2.2)) / 2.2
                ez = max(0, abs(z + 0.5 - cz) - (dd / 2 - 2.2)) / 2.2
                if ex * ex + ey * ey + ez * ez <= 1:
                    S.set(x, y, z, lijf)
    # oren
    for kant in (-1, 1):
        ox = cx + kant * w * 0.32
        for x in range(int(ox - w * 0.2), int(ox + w * 0.2) + 1):
            for y in range(y0 + hh - 2, y0 + hh + int(w * 0.32)):
                du, dv = (x + 0.5 - ox) / (w * 0.2), (y + 0.5 - (y0 + hh)) / (w * 0.3)
                if du * du + dv * dv <= 1:
                    for z in range(cz - 1, cz + 1):
                        binnen = du * du + dv * dv < 0.35 and z == cz
                        S.set(x, y, z + 1 if binnen else z, "minecraft:pink_wool" if binnen else lijf)
    # pootjes
    for kant in (-1, 1):
        S.fill(int(cx + kant * w * 0.25) - 1, y0, cz + dd // 2 - 1, int(cx + kant * w * 0.25) + 1, y0,
               cz + dd // 2, lijf)
    zf = z1 + dd  # voorkant (zuid), net ervoor

    def put(u, v, b):
        S.set(x1 + u, y0 + v, zf - 1 if S.get(x1 + u, y0 + v, zf - 1) != "minecraft:air" else zf, b)
    gezicht(put, w, hh, oog)


# ---------------------------------------------------------------- huisjes
PALETTEN = [
    dict(wand="pleister_roze", hoek="roze_lakhout_planken", dak="guh_dakpan_roze"),
    dict(wand="pleister_vanille", hoek="pastelbaksteen_framboos", dak="guh_dakpan_roze"),
    dict(wand="pastelbaksteen_roze", hoek="roze_lakhout_planken", dak="guh_dakpan_wit"),
    dict(wand="knuffelsteen", hoek="pastelbaksteen_framboos", dak="guh_dakpan_roze"),
    dict(wand="pleister_lavendel", hoek="roze_lakhout_planken", dak="guh_dakpan_roze"),
]


def huis(S, x0, y0, z0, w=7, d=7, h=5, voor="south", pal=None, oren=True, tapijt="minecraft:pink_carpet"):
    """Guhhuisje op vloer y0 (eerste blok boven de vloer = y0). Zadeldak met de gevel aan de voorkant,
    guhgezicht in de voorgevel en twee guh-oortjes op het dak."""
    pal = pal or PALETTEN[0]
    x1, z1 = x0 + w - 1, z0 + d - 1
    S.fill(x0, y0 - 1, z0, x1, y0 - 1, z1, "roze_lakhout_planken")
    S.walls(x0, y0, z0, x1, y0 + h - 1, z1, pal["wand"])
    for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        S.fill(x, y0, z, x, y0 + h - 1, z, pal["hoek"])
    S.fill(x0 + 1, y0, z0 + 1, x1 - 1, y0 + h - 1, z1 - 1, LUCHT)   # cave_air: the map terrain never grows inside
    langs_z = voor in ("north", "south")  # nok loopt langs z -> gevels noord/zuid
    dak, trap = pal["dak"], pal["dak"] + "_trap"
    if langs_z:
        lo, hi = x0 - 1, x1 + 1
    else:
        lo, hi = z0 - 1, z1 + 1
    i = 0
    while lo + i <= hi - i:
        y = y0 + h + i - 1
        a, b = lo + i, hi - i
        for k in range((z0 - 1) if langs_z else (x0 - 1), ((z1 + 1) if langs_z else (x1 + 1)) + 1):
            if a == b:
                S.set(*((a, y, k) if langs_z else (k, y, a)), dak)
            else:
                S.set(*((a, y, k) if langs_z else (k, y, a)),
                      SK.stairs("guhs:" + trap, "east" if langs_z else "south"))
                S.set(*((b, y, k) if langs_z else (k, y, b)),
                      SK.stairs("guhs:" + trap, "west" if langs_z else "north"))
        # gevelvulling
        for g in range(a + 1, b):
            for kk in ((z0, z1) if langs_z else (x0, x1)):
                S.set(*((g, y, kk) if langs_z else (kk, y, g)), pal["wand"])
        i += 1
    top = y0 + h + i - 1
    # voorkant: deur, gezicht in de gevel, lantaarns
    mid = (x0 + x1) // 2 if langs_z else (z0 + z1) // 2
    vz = {"south": z1, "north": z0, "east": x1, "west": x0}[voor]
    step = {"south": 1, "north": -1, "east": 1, "west": -1}[voor]

    def P(u, y, off=0):  # u = positie langs de gevel, off = naar buiten
        return (u, y, vz + off * step) if langs_z else (vz + off * step, y, u)
    for k in range(1, (d if langs_z else w) - 2 if tapijt else 1):  # carpet runner from the door inwards
        S.set(*P(mid, y0, -k), tapijt)
    S.set(*P(mid, y0), f"roze_lakhout_deur[facing={voor},half=lower,hinge=left,open=false]")
    S.set(*P(mid, y0 + 1), f"roze_lakhout_deur[facing={voor},half=upper,hinge=left,open=false]")
    S.set(*P(mid, y0 + h), f"knuffelsteen_gezicht[facing={voor},stemming=0]")
    S.set(*P(mid - 2, y0 + 1, 1), LANT)
    S.set(*P(mid + 2, y0 + 1, 1), LANT)
    for u in (mid - 2, mid + 2):
        S.set(*P(u, y0 + 2), RAAM)
    # zijramen
    for kk in ((x0, x1) if langs_z else (z0, z1)):
        for g in range((z0 + 2) if langs_z else (x0 + 2), ((z1 - 1) if langs_z else (x1 - 1)), 2):
            S.set(*((kk, y0 + 1, g) if langs_z else (g, y0 + 1, kk)), RAAM)
    # oortjes op het dak
    if oren:
        mk = (z0 + z1) // 2 if langs_z else (x0 + x1) // 2
        for u in (lo + 2, hi - 2):
            yy = y0 + h + 1 + min(u - lo, hi - u) - 1
            if langs_z:
                oortje(S, u, yy, mk, "z")
            else:
                oortje(S, mk, yy, u, "x")
    return top


# ---------------------------------------------------------------- verbindingen
MAX_HELLING = 0.85   # a bridge never drops faster than this per block (half-slab steps stay <= 1 block)


def wolkentoren(S, x, z, y_onder, y_boven, naar):
    """A small wolkenlift tower: up column from a floor at y_onder (the pad stands on it) to a landing at y_boven,
    the puff at the top pushes you towards `naar` onto the landing; a down column right next to it."""
    sx, sz = {"east": (1, 0), "west": (-1, 0), "south": (0, 1), "north": (0, -1)}[naar]
    ox, oz = sz, sx                       # down column: next to it, across the way out
    for y in range(y_onder + 1, y_boven + 4):
        for (dx, dz) in ((0, 0), (ox, oz)):
            S.set(x + dx, y, z + dz, "minecraft:air")
    S.set(x, y_onder + 1, z, f"wolkenlift[down=false,facing={naar}]")
    S.set(x + ox, y_onder + 1, z + oz, f"wolkenlift[down=true,facing={naar}]")
    for y in range(y_onder + 2, y_boven + 3):
        S.set(x, y, z, f"wolkenstroom[down=false,facing={naar}]")
        S.set(x + ox, y, z + oz, f"wolkenstroom[down=true,facing={naar}]")
    # landing around both columns, posts at the corners, a roof and lights
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if (dx, dz) not in ((0, 0), (ox, oz)):
                S.set(x + dx, y_boven, z + dz, PLANK)
                for hy in (1, 2, 3):
                    S.set(x + dx, y_boven + hy, z + dz, "minecraft:air")
    for (dx, dz) in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        for y in range(y_onder + 1, y_boven + 5):
            S.set(x + dx, y, z + dz, "minecraft:stripped_cherry_log[axis=y]" if y <= y_boven else HEK)
        S.set(x + dx, y_boven + 4, z + dz, LANT_H)          # lights at the corners, never above a column
    S.fill(x - 2, y_boven + 5, z - 2, x + 2, y_boven + 5, z + 2, "guh_dakpan_roze")
    # the foot: a little paved square, free on every side
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if (dx, dz) not in ((0, 0), (ox, oz)) and abs(dx) + abs(dz) < 4:
                for hy in (1, 2, 3):
                    S.set(x + dx, y_onder + hy, z + dz, "minecraft:air")
                S.set(x + dx, y_onder, z + dz, KLINK)


def brug(S, a, b, zw):
    """Touwbrug tussen twee hoedplateaus (doorhangend, kersenhout + touwhekjes + lantaarntjes).
    Walkable: the deck goes in half-slab steps (never more than 1 block between two cells), headroom over the deck is
    kept free (no fence or lantern of the cap rim in the way), and a bridge that would be too steep ends in a small
    wolkenlift tower on the lower cap instead."""
    if zw[a][2] < zw[b][2]:
        a, b = b, a                       # a = the high end
    ax, az, aT, ar, _, _, ap, _ = zw[a]
    bx, bz, bT, br, _, _, bp, _ = zw[b]
    L = math.hypot(bx - ax, bz - az)
    ux, uz = (bx - ax) / L, (bz - az) / L
    sx, sz = ax + ux * (ap * ar - 1), az + uz * (ap * ar - 1)
    ex, ez = bx - ux * (bp * br - 1), bz - uz * (bp * br - 1)
    D = math.hypot(ex - sx, ez - sz)
    over = (aT - bT) - MAX_HELLING * D
    eind = bT + max(0.0, over)            # deck height at the low end (above the low cap when too steep)
    sag = 0.07 * D if over <= 0 else 0.0
    n = int(D * 2)
    px, pz = -uz, ux
    for i in range(n + 1):
        t = i / n
        x, z = sx + (ex - sx) * t, sz + (ez - sz) * t
        opp = aT + (eind - aT) * t - sag * 4 * t * (1 - t) + 1.0      # wanted walking height
        q = round(opp * 2) / 2
        if q == int(q):
            y, dek = int(q) - 1, "minecraft:cherry_slab[type=top,waterlogged=false]"
        else:
            y, dek = int(math.floor(q)), DEK
        for o in (-1, 0, 1):
            S.set(x + px * o, y, z + pz * o, dek)
            for hy in (1, 2, 3):
                S.set(x + px * o, y + hy, z + pz * o, "minecraft:air")
        for o in (-2, 2):
            S.set(x + px * o, y, z + pz * o, PLANK)
            S.set(x + px * o, y + 1, z + pz * o, HEK)
            if i % 16 == 8:
                S.set(x + px * o, y - 1, z + pz * o, LANT_H)        # lanterns hanging under the bridge
        if i % 24 == 12:
            for o in (-2, 2):
                S.set(x + px * o, y + 2, z + pz * o, HEK)
                S.set(x + px * o, y + 3, z + pz * o, LANT)
    if over > 0:
        # tower on the low cap, 2 blocks in from the deck end; the puff at the top faces back up the bridge
        tx, tz = int(round(ex + ux * 2)), int(round(ez + uz * 2))
        naar = ("west" if ux > 0 else "east") if abs(ux) > abs(uz) else ("north" if uz > 0 else "south")
        wolkentoren(S, tx, tz, bT, int(round(eind)), naar)
        TORENS.append((tx, tz))


TORENS = []


def sporenpad(S, a, b, zw):
    """Pad van roze wolkblokjes (met glimzwammetjes) tussen twee hoeden: aaneengesloten, 2 breed, nooit meer dan
    1 blok stijgen per stap (het waren losse springstenen met gaten en 2+ blokken hoogte)."""
    ax, az, aT, ar, _, _, ap, _ = zw[a]
    bx, bz, bT, br, _, _, bp, _ = zw[b]
    L = math.hypot(bx - ax, bz - az)
    ux, uz = (bx - ax) / L, (bz - az) / L
    sx, sz = ax + ux * (ap * ar - 1), az + uz * (ap * ar - 1)
    ex, ez = bx - ux * (bp * br - 1), bz - uz * (bp * br - 1)
    D = math.hypot(ex - sx, ez - sz)
    n = int(D * 3)
    for i in range(n + 1):
        t = i / n
        x = sx + (ex - sx) * t + 2.2 * math.sin(t * math.pi * 2) * math.sin(t * math.pi)
        z = sz + (ez - sz) * t + 2.2 * math.cos(t * math.pi * 2) * math.sin(t * math.pi)
        y = int(round(aT + (bT - aT) * t))
        for (dx, dz) in ((0, 0), (1, 0), (0, 1), (1, 1)):
            S.set(int(x) + dx, y, int(z) + dz, "wolkenblok_roze")
            for hy in (1, 2, 3):
                S.set(int(x) + dx, y + hy, int(z) + dz, "minecraft:air")
        if i % 9 == 0:
            S.set(int(x), y - 1, int(z), "minecraft:shroomlight")
        if i % 14 == 7:
            S.set(int(x) - 1, y + 1, int(z), "glimpaddenstoel")


def lift(S, naam, hoek, zw):
    """Liftje: open schacht met kersenhouten palen, ketting en een cabine halverwege; boven een stationnetje."""
    cx, cz, T, r, rs, _, p, _ = zw[naam]
    a = math.radians(hoek)
    lx, lz = int(round(cx + (r * 0.84) * math.cos(a))), int(round(cz + (r * 0.84) * math.sin(a)))
    d = math.hypot(lx - cx, lz - cz)
    top, _ = hoed_profiel(d, T, r, p)
    for y in range(G + 1, top + 5):
        for (dx, dz) in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            S.set(lx + dx, y, lz + dz, HEK if y % 12 else "roze_lakhout_planken")
        for (dx, dz) in ((0, -1), (0, 1), (-1, 0), (1, 0), (0, 0)):
            if y <= top:
                S.set(lx + dx, y, lz + dz, "minecraft:air")
        S.set(lx, y, lz, "minecraft:iron_chain[axis=y]")
    cy = G + 1 + (top - G) * 2 // 3
    S.fill(lx - 1, cy, lz - 1, lx + 1, cy, lz + 1, PLANK)
    S.fill(lx - 1, cy + 3, lz - 1, lx + 1, cy + 3, lz + 1, "guh_dakpan_roze")
    S.set(lx, cy + 1, lz, "minecraft:air")
    S.set(lx, cy + 2, lz, LANT_H)
    S.set(lx, G + 1, lz, "wolkenlift[down=false,facing=south]")
    # bovenstation
    S.fill(lx - 2, top + 5, lz - 2, lx + 2, top + 5, lz + 2, "guh_dakpan_roze")
    S.set(lx, top + 6, lz, LANT)
    S.fill(lx - 2, top, lz - 2, lx + 2, top, lz + 2, PLANK)
    # pad van het station naar het plateau
    for t in range(0, int(d - p * r) + 3):
        x, z = lx - math.cos(a) * t, lz - math.sin(a) * t
        S.fill(int(x) - 1, T if t > 2 else top, int(z) - 1, int(x) + 1, T if t > 2 else top, int(z) + 1, PLANK)


def wenteltrap(S, naam, zw, start=0.0):
    """Sporenpad om de steel: kersenhouten wenteltrap met touwhekje en lantaarntjes."""
    cx, cz, T, r, rs, _, p, krom = zw[naam]
    _, onder = hoed_profiel(0, T, r, p)
    h = onder + 2 - G
    a = start
    # it used to stop under the cap (dead end): now it winds on through the cap and comes out on the plateau
    rr_top = steel_straal(rs, (onder - 1 - G) / h)
    for y in range(G + 1, T + 1):
        t = min(1.0, (y - G) / h)
        rr = steel_straal(rs, t) if y < onder - 1 else rr_top
        sx = cx + krom * math.sin(math.pi * t)
        R0 = rr + 2.0
        door_kap = y >= onder - 4
        for k in range(4):
            aa = a + k * 0.5 / R0
            for dr in (0.6, 1.5, 2.4):
                px, pz = sx + (rr + dr) * math.cos(aa), cz + (rr + dr) * math.sin(aa)
                if door_kap:
                    S.set(px, y, pz, PLANK)
                    for hy in (1, 2, 3):
                        if y + hy <= T + 3:
                            S.set(px, y + hy, pz, "minecraft:air")
                else:
                    S.set(px, y, pz, PLANK, keep=True)
            if not door_kap:
                S.set(sx + (rr + 3.3) * math.cos(aa), y + 1, cz + (rr + 3.3) * math.sin(aa), HEK, keep=True)
        if (y - G) % 10 == 0 and not door_kap:
            S.set(sx + (rr + 3.3) * math.cos(a), y + 2, cz + (rr + 3.3) * math.sin(a), LANT, keep=True)
        a += 1.8 / R0
    WENTEL_UIT[naam] = a


# ---------------------------------------------------------------- tempel van De Knuffelorde
def tempel(S, zw):
    """Tempel op de hoogste hoed. Voorgevel (zuid) met torens, een boogpoort met een deur van 30 hoog en
    een reuzen-guhgezicht erop; erachter een koepel in de vorm van een guhhoofd."""
    cx, cz, T, r, _, _, p, _ = zw["tempel"]
    y0 = T + 1
    # bordes (3 treden)
    for i, (hw, hd) in enumerate(((19, 15), (17, 13), (15, 11))):
        S.fill(cx - hw, y0 + i, cz - hd - 2, cx + hw, y0 + i, cz + hd + 1, "knuffelsteen" if i < 2 else "tegel_hartjes")
    yv = y0 + 3  # tempelvloer
    # trap naar het bordes (zuid)
    for i in range(3):
        S.fill(cx - 6, y0 + i, cz + 16 + 2 - i, cx + 6, y0 + i, cz + 16 + 2 - i,
               SK.stairs("guhs:knuffelsteen_trap", "north"))
    # hal
    hx1, hx2, hz1, hz2 = cx - 13, cx + 13, cz - 11, cz + 8
    hh = 24
    S.walls(hx1, yv, hz1, hx2, yv + hh, hz2, "knuffelsteen")
    for x in range(hx1, hx2 + 1, 4):
        S.fill(x, yv, hz1, x, yv + hh, hz1, "roze_knuffelsteen")
    for z in range(hz1, hz2 + 1, 4):
        S.fill(hx1, yv, z, hx1, yv + hh, z, "roze_knuffelsteen")
        S.fill(hx2, yv, z, hx2, yv + hh, z, "roze_knuffelsteen")
    for z in range(hz1 + 2, hz2 - 1, 4):  # hoge glas-in-loodramen
        for xx in (hx1, hx2):
            S.fill(xx, yv + 6, z, xx, yv + 16, z, RAAM)
    S.fill(hx1 - 1, yv + hh + 1, hz1 - 1, hx2 + 1, yv + hh + 1, hz2 + 1, "minecraft:gold_block")
    S.fill(hx1, yv + hh + 1, hz1, hx2, yv + hh + 1, hz2, "pastelbaksteen_framboos")
    # trommel + koepel = guhhoofd (steekt boven de voorgevel uit)
    kcz = cz - 2
    ky = yv + 42
    S.cylinder(cx, kcz, 11, yv + hh + 1, ky - 1, "knuffelsteen")
    for y in range(yv + hh + 4, ky - 3, 5):
        for k in range(10):
            a = k * math.pi / 5
            S.set(cx + 10.7 * math.cos(a), y, kcz + 10.7 * math.sin(a), RAAM)
            S.set(cx + 10.7 * math.cos(a), y + 1, kcz + 10.7 * math.sin(a), RAAM)
    S.cylinder(cx, kcz, 12.5, ky - 1, ky - 1, "minecraft:gold_block")
    guhkop(S, cx, ky, kcz, 12)
    # voorgevel (pylon) met torens
    fz1, fz2 = hz2 + 1, hz2 + 4
    fx1, fx2 = cx - 16, cx + 16
    fh = 40
    S.fill(fx1, yv, fz1, fx2, yv + fh, fz2, "knuffelsteen")
    # getrapte top
    for i in range(6):
        S.fill(fx1 + 3 + i * 2, yv + fh + 1 + i, fz1, fx2 - 3 - i * 2, yv + fh + 1 + i, fz2, "knuffelsteen")
    S.fill(fx1, yv + fh, fz2, fx2, yv + fh, fz2, "minecraft:gold_block")
    for x in range(fx1, fx2 + 1, 4):
        if abs(x - cx) <= 7 + 3:   # niet voor de poort
            continue
        S.fill(x, yv, fz2 + 1, x, yv + fh - 1, fz2 + 1, "roze_knuffelsteen")
    # torens
    for tx in (fx1 - 3, fx2 + 3):
        toren(S, tx, yv, fz1 + 1, 5, fh + 10)
    # boogpoort met deur (15 breed, 30 hoog)
    dw, dh = 15, 30
    dx1 = cx - dw // 2
    boog = []
    for u in range(-2, dw + 2):
        for v in range(0, dh + 4):
            uu = u + 0.5 - dw / 2
            # rechte zijkanten tot dh - dw/2, dan halfrond
            vb = dh - dw / 2
            if v < vb:
                binnen = 0 <= u < dw
                rand = (-2 <= u < dw + 2) and not binnen
            else:
                rr = math.hypot(uu, v + 0.5 - vb)
                binnen = rr <= dw / 2
                rand = dw / 2 < rr <= dw / 2 + 2
            if binnen:
                boog.append((u, v))
            elif rand:
                S.fill(dx1 + u, yv + v, fz2 + 1, dx1 + u, yv + v, fz2 + 2,
                       "minecraft:gold_block" if (u + v) % 5 == 0 else "pastelbaksteen_framboos")
    for (u, v) in boog:
        S.fill(dx1 + u, yv + v, fz1, dx1 + u, yv + v, fz2, "minecraft:air")
    # deurvleugels (iets terug in de boog, op z = fz1 + 1)
    for (u, v) in boog:
        mid = u in (dw // 2,)
        rand = u in (0, dw - 1) or v == 0
        blok = "minecraft:gold_block" if (mid or rand) else \
            ("minecraft:pink_concrete" if (u % 7 in (1, 2, 3, 4, 5) and v % 9 not in (0,)) else "roze_lakhout_planken")
        S.set(dx1 + u, yv + v, fz1 + 1, blok)
    # het guhgezicht over beide deurvleugels
    def put(u, v, b):
        if (u, v + 6) in set(boog):
            S.set(dx1 + u, yv + v + 6, fz1 + 2, b)
    gezicht(put, dw, dh - 10)
    # deurklinken: twee gouden ringetjes
    for u in (dw // 2 - 1, dw // 2 + 1):
        S.set(dx1 + u, yv + 8, fz1 + 2, "minecraft:gold_block")
    # roosvenster boven de poort
    ry, rr = yv + dh + 6, 3.6
    for u in range(-4, 5):
        for v in range(-4, 5):
            d = math.hypot(u, v)
            if d <= rr:
                S.set(cx + u, ry + v, fz2 + 1, "glasinlood_hartje" if d > 2 else RAAM)
            elif d <= rr + 1:
                S.set(cx + u, ry + v, fz2 + 1, "minecraft:gold_block")
    # sluitsteen: guhgezicht
    S.set(cx, yv + dh + 1, fz2 + 2, "knuffelsteen_gezicht[facing=south,stemming=0]")
    # oren op de voorgevel
    for kant in (-1, 1):
        oortje(S, cx + kant * 9, yv + fh + 4, fz1 + 1, "x", groot=2)
        oortje(S, cx + kant * 9, yv + fh + 4, fz1 + 2, "x", groot=2)
    # vuurschalen en wachtbeeldjes voor de deur
    for kant in (-1, 1):
        bx = cx + kant * 11
        S.fill(bx - 1, yv, fz2 + 4, bx + 1, yv + 2, fz2 + 6, "pastelbaksteen_framboos")
        guhbeeld(S, bx, yv + 3, fz2 + 5, 5)
        S.set(cx + kant * 5, yv, fz2 + 8, "minecraft:gold_block")
        S.set(cx + kant * 5, yv + 1, fz2 + 8, "minecraft:shroomlight")
    # lantaarnpaden over het plateau
    for kant in (-1, 1):
        lantaarnpaal(S, cx + kant * 8, y0, cz + 19)          # beside the foot of the stair, not on it
    return dict(deur=(dx1, yv, fz1, dw, dh))


def toren(S, cx, y0, cz, r, h):
    """Ronde tempeltoren met een kegeldak met guh-oortjes en een lichtje in de spits."""
    S.cylinder(cx, cz, r, y0, y0 + h, "knuffelsteen", hollow=1.5)
    S.cylinder(cx, cz, r - 1.5, y0, y0 + h, "minecraft:air")
    for y in range(y0 + 6, y0 + h - 3, 8):
        for k in range(4):
            a = k * math.pi / 2 + 0.4
            S.set(cx + (r - 0.3) * math.cos(a), y, cz + (r - 0.3) * math.sin(a), RAAM)
            S.set(cx + (r - 0.3) * math.cos(a), y + 1, cz + (r - 0.3) * math.sin(a), RAAM)
    S.cylinder(cx, cz, r + 1, y0 + h, y0 + h, "minecraft:gold_block")
    k = 0
    rr = r + 1.0
    while rr > 0.3:
        S.cylinder(cx, cz, rr, y0 + h + 1 + k, y0 + h + 1 + k, "guh_dakpan_roze")
        rr -= 0.55
        k += 1
    S.set(cx, y0 + h + 1 + k, cz, "minecraft:shroomlight")
    S.set(cx, y0 + h + 2 + k, cz, "minecraft:end_rod[facing=up]")
    oortje(S, cx - 2, y0 + h + 2, cz, "x")
    oortje(S, cx + 2, y0 + h + 2, cz, "x")


def guhkop(S, cx, y0, cz, r):
    """Koepel als guhhoofd: halve bol van roze wol met twee grote oren en een gezicht naar het zuiden."""
    for x in range(cx - r - 1, cx + r + 2):
        for z in range(cz - r - 1, cz + r + 2):
            for y in range(y0, y0 + int(r * 1.05) + 2):
                d = ((x - cx) / r) ** 2 + ((y - y0) / (r * 1.05)) ** 2 + ((z - cz) / r) ** 2
                if d <= 1:
                    S.set(x, y, z, "minecraft:pink_wool" if d > 0.7 else "minecraft:pink_concrete")
    for kant in (-1, 1):
        ox, oy = cx + kant * int(r * 0.62), y0 + int(r * 0.85)
        for x in range(ox - 6, ox + 7):
            for y in range(oy, oy + 14):
                du, dv = (x - ox) / 4.6, (y - oy - 5) / 8.0
                if du * du + dv * dv <= 1:
                    for z in (cz - 1, cz, cz + 1):
                        binnen = du * du + dv * dv < 0.45 and z == cz + 1
                        S.set(x, y, z, "knuffelblok_roze" if binnen else "minecraft:pink_wool")
    # gezicht: projecteer op het zuidelijke boloppervlak
    w, h = int(r * 1.5), int(r * 0.9)

    def put(u, v, b):
        x = cx - w // 2 + u
        y = y0 + 1 + v
        dd = 1 - ((x - cx) / r) ** 2 - ((y - y0) / (r * 1.05)) ** 2
        if dd <= 0:
            return
        z = int(cz + r * math.sqrt(dd))
        S.set(x, y, z, b)
    gezicht(put, w, h)
    S.set(cx, y0 + int(r * 1.05) + 1, cz, "minecraft:shroomlight")


# ---------------------------------------------------------------- plein, markt, poort
def plein(S):
    px, pz, pr = PLEIN
    for x in range(px - pr - 1, px + pr + 2):
        for z in range(pz - pr - 1, pz + pr + 2):
            d = math.hypot(x - px, z - pz)
            if d <= pr:
                b = KLINK
                if pr - 1.6 < d:
                    b = "tegel_guhgezicht"
                elif 9 < d <= 10.4:
                    b = "tegel_hartjes"
                S.set(x, G, z, b)
    # sokkel + knuffelbeeld
    S.fill(px - 5, G + 1, pz - 5, px + 5, G + 2, pz + 5, "knuffelsteen")
    S.fill(px - 4, G + 3, pz - 4, px + 4, G + 3, pz + 4, "pastelbaksteen_framboos")
    S.fill(px - 5, G + 2, pz - 5, px + 5, G + 2, pz - 5, "minecraft:gold_block")
    S.fill(px - 5, G + 2, pz + 5, px + 5, G + 2, pz + 5, "minecraft:gold_block")
    guhbeeld(S, px, G + 4, pz, 11)
    # lamp posts + planters on the rim of the plaza (not in the walking area), never in front of an exit
    uitgangen = [(math.pi / 2, 0.42), (-math.pi / 2, 0.42)] +         [(math.atan2(z_[1] - pz, z_[0] - px), 0.3) for n_, z_ in ZWAMMEN.items() if n_ != "tempel"]
    for k in range(24):
        a = k * math.pi / 12 + math.pi / 24
        if any(abs(math.atan2(math.sin(a - u), math.cos(a - u))) < w for (u, w) in uitgangen):
            continue
        x, z = int(round(px + (pr - 1) * math.cos(a))), int(round(pz + (pr - 1) * math.sin(a)))
        if k % 2:
            lantaarnpaal(S, x, G + 1, z)
        else:
            S.set(x, G + 1, z, "minecraft:cherry_leaves[distance=7,persistent=true,waterlogged=false]")
            S.set(x, G + 2, z, "minecraft:pink_petals[facing=north,flower_amount=4]")
    # vaandels (wool flags on poles) at the two big exits
    for (u, _) in uitgangen[:2]:
        for kant in (-1, 1):
            x = px + kant * 6
            z = int(round(pz + (pr - 1) * math.sin(u)))
            vaandel(S, x, G + 1, z, "x")


def kraam(S, x, z, kleur, voor="south"):
    """Marktkraampje 4x3 met tentdoek-dakje."""
    S.fill(x, G + 1, z, x + 3, G + 1, z + 2, "minecraft:barrel[facing=up,open=false]")
    S.fill(x + 1, G + 1, z + 1, x + 2, G + 1, z + 1, "minecraft:air")
    for (dx, dz) in ((0, 0), (3, 0), (0, 2), (3, 2)):
        S.fill(x + dx, G + 2, z + dz, x + dx, G + 3, z + dz, HEK)
    doek = "campingmarkt_tentdoek_" + kleur
    S.fill(x, G + 4, z, x + 3, G + 4, z + 2, doek)
    S.fill(x - 1, G + 4, z + 3, x + 4, G + 4, z + 3, SK.stairs("guhs:" + doek + "_trap", "north"))
    S.fill(x - 1, G + 4, z - 1, x + 4, G + 4, z - 1, SK.stairs("guhs:" + doek + "_trap", "south"))
    S.set(x + 1, G + 2, z + 2, "roze_guh_koek" if SK.has_block("roze_guh_koek") else "minecraft:cake")
    S.set(x + 2, G + 2, z + 2, LANT)
    # goods on the back counter (what the stall sells follows its cloth colour)
    waar = {"rood": ("minecraft:melon", "minecraft:pumpkin"), "geel": ("minecraft:hay_block[axis=y]", "minecraft:honey_block"),
            "blauw": ("minecraft:potted_blue_orchid", "minecraft:potted_pink_tulip"),
            "groen": ("minecraft:potted_fern", "minecraft:moss_block"),
            "creme": ("minecraft:white_wool", "minecraft:pink_wool")}[kleur]
    S.set(x + 1, G + 2, z, waar[0])
    S.set(x + 2, G + 2, z, waar[1])


def markt(S, rng):
    kleuren = ["rood", "geel", "blauw", "groen", "creme"]
    px, pz, pr = PLEIN
    k = 0
    # rij langs de boulevard naar de haven
    for z in range(pz + pr + 3, 206, 6):
        for x in (px - 11, px + 8):
            kraam(S, x, z, kleuren[k % 5]); k += 1
    # ring om het plein
    for i in range(14):
        a = i * 2 * math.pi / 14
        if abs(math.sin(a)) > 0.92:   # vrij voor de boulevard (noord/zuid)
            continue
        x, z = int(px + (pr + 5) * math.cos(a)) - 2, int(pz + (pr + 5) * math.sin(a)) - 1
        kraam(S, x, z, kleuren[k % 5]); k += 1


def guhpoort(S, cx, z, breed=13, hoog=20):
    """Stadspoort aan de haven: twee pilaren en een boog met een groot guhgezicht en oren erop."""
    for kant in (-1, 1):
        px = cx + kant * (breed // 2 + 3)
        S.fill(px - 2, G + 1, z - 2, px + 2, G + hoog, z + 2, "knuffelsteen")
        S.fill(px - 2, G + 1, z - 2, px + 2, G + 1, z + 2, "pastelbaksteen_framboos")
        S.fill(px - 3, G + hoog + 1, z - 3, px + 3, G + hoog + 1, z + 3, "minecraft:gold_block")
        S.cylinder(px, z, 2.5, G + hoog + 2, G + hoog + 4, "guh_dakpan_roze")
        S.set(px, G + hoog + 5, z, "minecraft:shroomlight")
    x1, x2 = cx - breed // 2 - 1, cx + breed // 2 + 1
    yb = G + hoog - 8
    S.fill(x1, yb, z - 2, x2, G + hoog, z + 2, "knuffelblok_roze")
    S.fill(x1, yb, z - 2, x2, yb, z + 2, "minecraft:gold_block")
    w = x2 - x1 + 1

    def put(u, v, b):
        S.set(x1 + u, yb + 1 + v, z + 3, b)
    gezicht(put, w, hoog - (yb - G) - 1)
    for kant in (-1, 1):
        oortje(S, cx + kant * 5, G + hoog + 1, z, "x", groot=2)


# ---------------------------------------------------------------- haven + luchtschip
def haven(S):
    # steigers
    for px in (84, 160):
        for z in range(214, 246):
            S.fill(px - 1, G, z, px + 1, G, z, PLANK)
            if z % 4 == 0:
                S.set(px - 2, G - 1, z, "minecraft:cherry_log[axis=y]")
                S.set(px + 2, G - 1, z, "minecraft:cherry_log[axis=y]")
                S.set(px - 2, G, z, "minecraft:cherry_log[axis=y]")
                S.set(px + 2, G, z, "minecraft:cherry_log[axis=y]")
            if z % 8 == 0:
                S.set(px + 2, G + 1, z, LANT)
    for (bx, bz) in ((92, 228), (152, 236), (70, 240)):
        bootje(S, bx, bz)
    vuurtoren(S, 34, 234)
    mast(S, 236, 238)
    luchtschip(S, 206, 238, 64)


def bootje(S, x, z):
    S.fill(x - 3, G, z - 1, x + 3, G, z + 1, PLANK)
    S.fill(x - 4, G, z, x + 4, G, z, PLANK)
    S.fill(x - 3, G + 1, z - 1, x + 3, G + 1, z - 1, "roze_lakhout_planken")
    S.fill(x - 3, G + 1, z + 1, x + 3, G + 1, z + 1, "roze_lakhout_planken")
    S.fill(x, G + 1, z, x, G + 7, z, HEK)
    for i in range(5):
        S.fill(x + 1, G + 2 + i, z, x + 1 + (4 - i) // 1 // 1 - 0, G + 2 + i, z, "minecraft:white_wool")
    S.set(x, G + 8, z, "minecraft:pink_wool")


def vuurtoren(S, cx, cz):
    S.cylinder(cx, cz, 7, 0, G, "knuffelsteen")
    for y in range(G + 1, G + 38):
        r = 4.5 - (y - G) * 0.04
        S.cylinder(cx, cz, r, y, y, "minecraft:white_concrete" if ((y - G) // 5) % 2 else "minecraft:pink_concrete",
                   hollow=1.2)
    yt = G + 38
    S.cylinder(cx, cz, 4.5, yt, yt, "minecraft:gold_block")
    S.cylinder(cx, cz, 2.5, yt + 1, yt + 3, "minecraft:glowstone")
    S.cylinder(cx, cz, 4, yt + 4, yt + 4, "guh_dakpan_roze")
    S.cylinder(cx, cz, 3, yt + 5, yt + 5, "guh_dakpan_roze")
    S.cylinder(cx, cz, 1.8, yt + 6, yt + 6, "guh_dakpan_roze")
    oortje(S, cx - 2, yt + 5, cz, "x")
    oortje(S, cx + 2, yt + 5, cz, "x")


def mast(S, cx, cz):
    """Luchtschip-meermast: rotseilandje, open toren van kersenhout, platform met lantaarns."""
    S.cylinder(cx, cz, 6, 0, G, "knuffelsteen")
    S.cylinder(cx, cz, 6, G, G, KLINK)
    top = 64
    for y in range(G + 1, top):
        for (dx, dz) in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
            S.set(cx + dx, y, cz + dz, "minecraft:stripped_cherry_log[axis=y]")
        if y % 8 == 0:
            S.walls(cx - 2, y, cz - 2, cx + 2, y, cz + 2, PLANK)
    for y in range(G + 1, top, 2):  # trapje binnenin
        S.set(cx + (1 if (y // 2) % 2 else -1), y, cz, PLANK)
    S.fill(cx - 4, top, cz - 4, cx + 4, top, cz + 4, PLANK)
    S.walls(cx - 4, top + 1, cz - 4, cx + 4, top + 1, cz + 4, HEK)
    for (dx, dz) in ((-4, -4), (4, -4), (-4, 4), (4, 4)):
        S.set(cx + dx, top + 2, cz + dz, LANT)
    S.fill(cx, top + 1, cz, cx, top + 8, cz, "minecraft:cherry_log[axis=y]")
    S.set(cx, top + 9, cz, "minecraft:shroomlight")
    oortje(S, cx, top + 10, cz, "z")


def luchtschip(S, cx, cz, cy):
    """Roze guh-luchtschip aan de mast: ballon met witte banden en oortjes, gondel, staartvinnen."""
    rx, ry, rz = 22, 8, 8
    for x in range(cx - rx, cx + rx + 1):
        for y in range(cy - ry, cy + ry + 1):
            for z in range(cz - rz, cz + rz + 1):
                d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
                if 0.72 < d <= 1:
                    S.set(x, y, z, "minecraft:white_wool" if (x - cx) % 9 in (0,) else "minecraft:pink_wool")
    for kant in (-1, 1):  # oortjes
        oortje(S, cx + 12, cy + ry - 1, cz + kant * 4, "x", groot=2)
    # vinnen
    for v in range(0, 8):
        for u in range(0, 8 - v):
            S.set(cx - rx - 1 + u, cy + 3 + v, cz, "minecraft:pink_concrete")
            S.set(cx - rx - 1 + u, cy, cz - 6 - v, "minecraft:pink_concrete")
            S.set(cx - rx - 1 + u, cy, cz + 6 + v, "minecraft:pink_concrete")
    # gondel
    gy = cy - ry - 5
    S.fill(cx - 8, gy, cz - 2, cx + 8, gy + 3, cz + 2, "roze_lakhout_planken")
    S.fill(cx - 7, gy + 1, cz - 2, cx + 7, gy + 2, cz + 2, RAAM)
    S.fill(cx - 8, gy + 4, cz - 2, cx + 8, gy + 4, cz + 2, "guh_dakpan_roze")
    for x in (cx - 6, cx + 6):
        for z in (cz - 2, cz + 2):
            S.fill(x, gy + 5, z, x, cy - ry + 1, z, "minecraft:iron_chain[axis=y]")
    S.fill(cx - 10, gy + 1, cz - 2, cx - 10, gy + 3, cz + 2, "minecraft:white_wool")  # propeller
    S.fill(cx - 10, gy + 2, cz - 3, cx - 10, gy + 2, cz + 3, "minecraft:white_wool")
    # meertouw naar de mast
    for x in range(cx + rx, 236 - 4):
        S.set(x, cy, cz, HEK)


# ---------------------------------------------------------------- bomen, losse huisjes, guhs
def kersenboom(S, x, z, rng):
    h = rng.randint(4, 7)
    for y in range(G + 1, G + 1 + h):
        S.set(x, y, z, "minecraft:cherry_log[axis=y]")
    blad = "minecraft:cherry_leaves[distance=7,persistent=true,waterlogged=false]" if rng.random() < 0.6 else \
        "guhbloesem_leaves"
    S.ellipsoid(x, G + h + 1, z, 3.2, 2.2, 3.2, blad, keep=True)


def vrij(S, x1, z1, x2, z2, y=G + 1):
    if x1 < 1 or z1 < 1 or x2 >= S.v.shape[0] - 1 or z2 >= S.v.shape[2] - 1:
        return False
    if np.any(S.v[x1:x2 + 1, y:y + 6, z1:z2 + 1]):
        return False
    return bool(np.all(S.v[x1:x2 + 1, G, z1:z2 + 1] == S.idx.get("guhs:knuffelgras", -1)))


def grondhuisjes(S, rng):
    px, pz, pr = PLEIN
    plekken = [(px - 34, pz - 6, "east"), (px + 28, pz - 6, "west"), (px - 34, pz + 8, "east"),
               (px + 28, pz + 8, "west"), (px - 22, pz - 34, "south"), (px + 16, pz - 34, "south"),
               (px - 50, pz + 18, "east"), (px + 44, pz + 18, "west"), (px - 46, pz - 24, "south"),
               (px + 40, pz - 24, "south")]
    for i, (x, z, v) in enumerate(plekken):
        if vrij(S, x - 1, z - 1, x + 8, z + 8):
            huis(S, x, G + 1, z, 7, 7, 5, v, PALETTEN[i % len(PALETTEN)])


def hoedhuisjes(S, zw, rng):
    """Huisjes op elk hoedplateau, rondom, met de voordeur naar het midden."""
    for naam, (cx, cz, T, r, rs, kleur, p, _) in zw.items():
        if naam == "tempel":
            continue
        pr = p * r
        brugrichting = []
        for (a, b) in BRUGGEN + SPORENPADEN:
            if naam in (a, b):
                o = b if a == naam else a
                brugrichting.append((math.atan2(zw[o][1] - cz, zw[o][0] - cx), 0.5))
        for (n_, hk) in LIFTEN:
            if n_ == naam:
                brugrichting.append((math.radians(hk), 1.0))   # the lift ramp runs along an axis: keep a wide gap
        if naam in WENTEL_UIT:                      # where the spiral stair comes up through the cap
            brugrichting.append((WENTEL_UIT[naam], 0.6))
        n = 5 if pr > 11 else 4
        k = 0
        for i in range(n):
            a = i * 2 * math.pi / n + 0.3
            if any(abs(math.atan2(math.sin(a - b), math.cos(a - b))) < bw for (b, bw) in brugrichting):
                continue
            w = 5 if pr < 10 else 6
            hx, hz = cx + (pr - w * 0.75) * math.cos(a), cz + (pr - w * 0.75) * math.sin(a)
            vx, vz = -math.cos(a), -math.sin(a)
            voor = ("east" if vx > 0 else "west") if abs(vx) > abs(vz) else ("south" if vz > 0 else "north")
            huis(S, int(hx - w / 2), T + 1, int(hz - w / 2), w, w, 4, voor, PALETTEN[(k + len(naam)) % 5])
            k += 1
        # midden: een bloesemboompje en een lantaarnpaal
        S.set(cx, T + 1, cz, "minecraft:cherry_log[axis=y]")
        S.set(cx, T + 2, cz, "minecraft:cherry_log[axis=y]")
        S.ellipsoid(cx, T + 4, cz, 2.6, 1.8, 2.6, "minecraft:cherry_leaves[distance=7,persistent=true,waterlogged=false]",
                    keep=True)


def padlampen(S, stap=13):
    """Lamp posts NEXT to the ground paths (one block past the edge tiles), alternating sides; only on free grass,
    never on the plaza, a door front or another lamp's spot."""
    px, pz, pr = PLEIN
    gezet = []
    kant = 1
    for (x1, z1, x2, z2, b) in PADEN:
        L = math.hypot(x2 - x1, z2 - z1)
        if L < 1:
            continue
        ux, uz = (x2 - x1) / L, (z2 - z1) / L
        for i in range(6, int(L) - 3, stap):
            kant = -kant
            x = int(round(x1 + ux * i + (-uz) * kant * (b + 1.5)))
            z = int(round(z1 + uz * i + ux * kant * (b + 1.5)))
            if math.hypot(x - px, z - pz) < pr + 4 or any(abs(x - a) + abs(z - c) < 7 for (a, c) in gezet):
                continue
            if S.get(x, G, z) != "guhs:knuffelgras":
                continue
            if any(not S.is_air(x + dx, y, z + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1) for y in (G + 1, G + 2, G + 3)):
                continue
            lantaarnpaal(S, x, G + 1, z)
            gezet.append((x, z))
    return gezet


def guhtjes(S, rng):
    px, pz, pr = PLEIN
    for i in range(16):
        a = rng.random() * 2 * math.pi
        d = rng.uniform(7, pr - 2)
        S.guh(px + d * math.cos(a), G + 1, pz + d * math.sin(a), rng.choice([0, 90, 180, 270, 135]))
    for z in range(pz + pr + 4, 206, 5):
        S.guh(px + rng.uniform(-3, 3), G + 1, z, rng.choice([0, 180]))


# ---------------------------------------------------------------- alles
def bouw(S, seed=11):
    rng = random.Random(seed)
    terrein(S)
    px, pz, pr = PLEIN
    # hoofdas: haven -> plein -> tempelzwam
    pad(S, px, 212, px, pz + pr, 4)
    pad(S, px, pz - pr, px, 96, 4)
    for naam, z_ in ZWAMMEN.items():
        if naam != "tempel":
            pad(S, px, pz, z_[0], z_[1], 2)
    for (cx, cz, h, r, kl) in KLEIN:
        lampzwam(S, cx, cz, h, r, kl)
    for naam, (cx, cz, T, r, rs, kl, p, krom) in ZWAMMEN.items():
        zwam(S, cx, cz, T, r, rs, kl, p, krom)
    for naam in WENTEL:
        # start just past the stem door (south = pi/2), so the first steps never stand in front of it
        wenteltrap(S, naam, ZWAMMEN, start=math.pi / 2 + 0.6 + rng.random() * 4.4)
    info = tempel(S, ZWAMMEN)
    hoedhuisjes(S, ZWAMMEN, rng)
    for (a, b) in BRUGGEN:
        brug(S, a, b, ZWAMMEN)
    for (a, b) in SPORENPADEN:
        sporenpad(S, a, b, ZWAMMEN)
    for (n, hk) in LIFTEN:
        lift(S, n, hk, ZWAMMEN)
    plein(S)
    markt(S, rng)
    guhpoort(S, px, 206)
    haven(S)
    grondhuisjes(S, rng)
    padlampen(S)
    for _ in range(400):
        x, z = rng.randint(4, 251), rng.randint(4, 205)
        if vrij(S, x - 3, z - 3, x + 3, z + 3):
            kersenboom(S, x, z, rng)
    for _ in range(900):  # bloemetjes
        x, z = rng.randint(2, 253), rng.randint(2, 207)
        if S.get(x, G, z) == "guhs:knuffelgras" and S.is_air(x, G + 1, z):
            S.set(x, G + 1, z, rng.choice(["minecraft:pink_petals[facing=north,flower_amount=4]", "roze_guhbloem",
                                           "minecraft:short_grass", "minecraft:pink_tulip"]))
    deuren_vrij(S)
    guhtjes(S, rng)
    return info


def check(S):
    mis = sorted({s for s in S.pal if not SK.has_block(s.split("[")[0])})
    if mis:
        print("ONTBREKENDE BLOKKEN:", mis)
    return mis


def nieuw():
    S = SK.Scene(*MAAT)
    info = bouw(S)
    check(S)
    return S, info
