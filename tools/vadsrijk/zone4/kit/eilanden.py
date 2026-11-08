"""De eilandthema's van de Lampionkust (plus het bootje, de Schildpad en de Opstijgpier).

Elk eiland is een functie die een Plek (bouwsels.Plek, spelcoördinaten) bouwt; bouw(W) stempelt alle plekken die
het venster raken. De plekken worden één keer gebouwd en gecachet. Posities uit kust.EILANDEN (wereldindeling).

    Strandfeesteiland  parasols, kampvuur, schelpenlimonadebar, beachvolleybal, lampionsnoeren
    Vliegereiland      vliegerheuvel met guh-vliegers, vliegerwerkplaats, wedstrijdpaal
    Koraalbaai         atol met roze lagune, koraaltuinen, duiksteiger, schelpenhutjes, onderwatergrotjes, Zeeslurf
    Vuurtoreneiland    grote vuurtoren met oortjes + het huisje van de oude vuurtorenwachter
    Bloemeneiland      bloemenvelden in stroken, bijenboerderij, picknick
    Schildpadeiland    een eilandje op een reuzenschildpad die rondzwemt (SCHILDPAD_LUS)
    Knuffeltij-strand  grauwe knuffels die aanspoelen + het opvangcentrum
    Mysterie-eiland    grauw, mistig, een vervallen poort
    Pierkop            de Opstijgpier met de meermast voor het luchtschip naar Hoogvads
    Wrak               het Wrak van de Vliegervanger (kleine kerker)
"""
import math, os
import numpy as np
import kust as KU
import bouwsels as BW
import mountkit as MK
import schemerkit as SK

ZEE = KU.ZEE
EIL = KU.EIL
_CACHE = {}
_BEZET = set()
# schildpad: een punt op de zwemronde (hoek in graden) en zijn zwemrichting (raaklijn)
_SL = KU.SCHILDPAD_LUS
_SA = math.radians(215)
SCHILDPAD_POS = (int(_SL[0] + _SL[2] * math.cos(_SA)), int(_SL[1] + _SL[3] * math.sin(_SA)))
# het bootje van de speler: tussen het Knuffeltij-strand en het Bloemeneiland, op weg naar het oosten
BOOT_POS = (2016, -382, 270)


def _y(x, z):
    return KU.hoogte(x, z)[0]


def _ymax(x1, z1, x2, z2):
    return max(_y(x, z) for x in range(x1, x2 + 1, 2) for z in range(z1, z2 + 1, 2))


def _vak(naam, marge=20, y1=30, y2=140):
    e = EIL[naam]
    return (e[1] - e[3] - marge, y1, e[2] - e[4] - marge, e[1] + e[3] + marge, y2, e[2] + e[4] + marge)


# ============================================================================================ Strandfeesteiland
def strandfeest(leven=True):
    e = EIL["Strandfeesteiland"]
    cx, cz = e[1], e[2]
    P = BW.Plek(*_vak("Strandfeesteiland", 26))
    # kampvuur in het midden van het zuidstrand
    kx, kz = cx - 6, cz + 10
    ky = _y(kx, kz) + 1
    BW.kampvuur(P, kx, ky, kz)
    P.lab.append((kx, ky + 6, kz, "kampvuur"))
    # parasols + strandlakens langs de zuidkust
    for i, (dx, dz) in enumerate(((-30, 14), (-22, 20), (-12, 22), (4, 24), (14, 20), (24, 14), (-34, 4))):
        x, z = cx + dx, cz + dz
        y = _y(x, z) + 1
        if y <= ZEE + 1:
            continue
        BW.parasol(P, x, y, z, ["minecraft:pink_wool", "minecraft:light_blue_wool", "minecraft:yellow_wool",
                                "minecraft:lime_wool", "minecraft:magenta_wool"][i % 5], r=2)
        for j in range(3):
            P.s(x + 1, y, z + 1 + j, ["minecraft:pink_carpet", "minecraft:white_carpet"][j % 2])
        if leven:
            P.guhje(x + 1.5, y, z + 2.5, 200 + i * 20, ["guh", "guh_mint", "guh_golden", "guh_pinguh"][i % 4])
    # schelpenlimonadebar: tiki-hutje met rietdak en schelpen
    bx, bz = cx + 8, cz - 6
    by = _ymax(bx - 4, bz - 3, bx + 4, bz + 3) + 1
    P.f(bx - 4, by - 1, bz - 3, bx + 4, by - 1, bz + 3, "minecraft:cherry_planks")
    for (dx, dz) in ((-4, -3), (4, -3), (-4, 3), (4, 3)):
        P.f(bx + dx, by, bz + dz, bx + dx, by + 3, bz + dz, "guhs:guhwaii_palm_stam[axis=y]")
    P.f(bx - 3, by, bz + 2, bx + 3, by, bz + 2, "guhs:guhwaii_palm_planken")
    for x in range(bx - 3, bx + 4, 2):
        P.s(x, by + 1, bz + 2, "guhs:reuzenschelp[facing=south,parel=true]")
    for i in range(4):
        for x in range(bx - 5 + i, bx + 6 - i):
            P.s(x, by + 4 + i, bz - 4 + i, "guhs:tiki_rietdak_trap[facing=south,half=bottom,shape=straight]")
            P.s(x, by + 4 + i, bz + 4 - i, "guhs:tiki_rietdak_trap[facing=north,half=bottom,shape=straight]")
        P.f(bx - 5 + i, by + 4 + i, bz - 3 + i, bx + 5 - i, by + 4 + i, bz + 3 - i, "guhs:tiki_rietdak")
    P.f(bx - 1, by + 8, bz, bx + 1, by + 8, bz, "guhs:tiki_rietdak_plaat[type=bottom]")
    P.s(bx - 4, by + 3, bz + 4, "guhs:tiki_schelpjeslampion")
    P.s(bx + 4, by + 3, bz + 4, "guhs:tiki_schelpjeslampion")
    P.s(bx, by + 4, bz + 4, "guhs:tiki_masker_roze")
    for x in (bx - 2, bx, bx + 2):
        P.s(x, by, bz + 4, "guhs:tiki_kruk")
    if leven:
        P.guhje(bx + 0.5, by, bz + 0.5, 180, "guh_golden")
        P.guhje(bx - 1.5, by, bz + 4.5, 0, "guh", )
        P.guhje(bx + 2.5, by, bz + 4.5, 20, "guh_pluisguh")
    P.lab.append((bx, by + 11, bz, "Schelpenlimonadebar"))
    # beachvolleybal: zandveld met lijnen, net, bal
    vx, vz = cx + 26, cz + 2
    vy = _y(vx, vz) + 1
    for x in range(vx - 8, vx + 9):
        for z in range(vz - 4, vz + 5):
            if abs(x - vx) == 8 or abs(z - vz) == 4:
                P.s(x, vy - 1, z, "guhs:pastelbaksteen_wolkje_plaat[type=top]")
            else:
                P.s(x, vy - 1, z, "guhs:roze_zand")
            P.f(x, vy, z, x, vy + 4, z, "minecraft:air")
    P.f(vx, vy, vz - 5, vx, vy + 3, vz - 5, "minecraft:cherry_fence")
    P.f(vx, vy, vz + 5, vx, vy + 3, vz + 5, "minecraft:cherry_fence")
    P.f(vx, vy + 2, vz - 4, vx, vy + 3, vz + 4, "minecraft:white_stained_glass_pane")
    P.dl.append((vx - 3.5, vy + 6, vz + 1.5, (255, 255, 255), 0.9, 0, "stip"))
    if leven:
        for (dx, dz, yaw) in ((-5, -2, 270), (-4, 2, 260), (4, 1, 90), (5, -2, 100)):
            P.guhje(vx + dx + 0.5, vy, vz + dz + 0.5, yaw, ["guh", "guh_mint", "guh_golden", "guh_pinguh"][dx % 4])
    P.lab.append((vx, vy + 8, vz, "beachvolleybal"))
    # lampionsnoeren tussen palen rond het feestterrein
    paal = [(cx - 26, cz + 8), (cx - 14, cz + 2), (cx - 2, cz + 6), (cx + 10, cz + 4), (cx + 18, cz + 10)]
    for i, (x, z) in enumerate(paal):
        y = _y(x, z) + 1
        P.f(x, y, z, x, y + 5, z, "minecraft:stripped_cherry_log[axis=y]")
        P.s(x, y + 6, z, "guhs:tiki_fakkel")
        if i:
            px, pz = paal[i - 1]
            BW.lampionsnoer(P, (px + 0.5, _y(px, pz) + 6.5, pz + 0.5), (x + 0.5, y + 5.5, z + 0.5), zak=1.4, seed=i,
                            slinger=True)
    # surfplankrek en tiki-fakkels langs het strand
    P.s(cx - 18, _y(cx - 18, cz + 16) + 1, cz + 16, "guhs:tiki_surfplankrek")
    for (dx, dz) in ((-8, 18), (2, 16), (12, 16)):
        P.s(cx + dx, _y(cx + dx, cz + dz) + 1, cz + dz, "guhs:tiki_fakkel")
    if leven:  # dansende guhs rond het kampvuur
        for a in range(6):
            h = a * math.pi / 3
            P.guhje(kx + 0.5 + math.cos(h) * 4.6, ky, kz + 0.5 + math.sin(h) * 4.6, (math.degrees(h) + 90) % 360,
                    ["guh", "guh_mint", "guh_golden", "guh_pluisguh", "guh_mager", "guh_pinguh"][a])
    P.lab.append((cx, 90, cz, "Strandfeesteiland"))
    return P


# ============================================================================================ Vliegereiland
VLIEGER_KLEUREN = [(255, 120, 170), (140, 220, 250), (255, 230, 120), (180, 150, 250), (130, 230, 170),
                   (255, 170, 120), (255, 255, 255), (250, 90, 140)]


def vliegereiland(leven=True):
    e = EIL["Vliegereiland"]
    cx, cz = e[1], e[2]
    P = BW.Plek(*_vak("Vliegereiland", 30, 30, 170))
    top = (cx, cz)
    ty = _y(*top) + 1
    # wedstrijdpaal bovenop met wimpels en een windzak
    P.f(cx, ty, cz, cx, ty + 14, cz, "minecraft:stripped_cherry_log[axis=y]")
    P.s(cx, ty + 15, cz, "guhs:knuffelblok_roze")
    for i in range(4):
        for j in range(1, 5 - i):
            P.s(cx + j, ty + 13 - i, cz, "minecraft:pink_wool" if (i + j) % 2 else "minecraft:white_wool")
    P.f(cx - 2, ty - 1, cz - 2, cx + 2, ty - 1, cz + 2, "guhs:pastelbaksteen_wolkje")
    P.lab.append((cx, ty + 18, cz, "wedstrijdpaal"))
    # vliegerwerkplaats aan de voet (zuidkant)
    wx, wz = cx - 18, cz + 12
    wy = _ymax(wx, wz, wx + 9, wz + 7) + 1
    BW.huis(P, wx, wy, wz, 10, 7, 5, "south", "pastelbaksteen_aqua", "guh_dakpan_roze", oortjes=True,
            luifel="minecraft:pink_wool")
    P.lab.append((wx + 5, wy + 12, wz + 3, "Vliegerwerkplaats"))
    # vliegerframes tegen de muur
    for i in range(3):
        BW.vlieger(P, None, (wx + 2.5 + i * 3, wy + 2.6, wz + 7.7), 1.2, VLIEGER_KLEUREN[i + 3], richting=(0, 0, -1),
                   staart=False)
    # de vliegers: lijnen vanaf guhs op de helling, hoog in de wind (wind uit het westen -> vliegers naar het oosten)
    rng = np.random.default_rng(8)
    ankers = []
    for i in range(14):
        a = rng.uniform(0, 2 * math.pi)
        r = rng.uniform(0.25, 0.8)
        x = int(cx + math.cos(a) * e[3] * r)
        z = int(cz + math.sin(a) * e[4] * r)
        y = _y(x, z) + 1
        if y <= ZEE + 1:
            continue
        ankers.append((x, y, z))
        kpos = (x + 12 + rng.uniform(0, 16), y + 20 + rng.uniform(0, 26), z + 4 + rng.uniform(0, 14))
        BW.vlieger(P, (x + 0.5, y + 1.0, z + 0.5), kpos, rng.uniform(2.8, 4.6), VLIEGER_KLEUREN[i % 8],
                   VLIEGER_KLEUREN[(i + 3) % 8], richting=(0.6, 0.12, 0.8), seed=i)
        if leven:
            P.guhje(x + 0.5, y, z + 0.5, 270 + rng.uniform(-30, 30), ["guh", "guh_mint", "guh_golden", "guh_pluisguh",
                                                                      "guh_pinguh"][i % 5])
    # een reuzenvlieger (de wedstrijdvlieger) vanaf de paal
    BW.vlieger(P, (cx + 0.5, ty + 8, cz + 0.5), (cx + 30, ty + 44, cz + 16), 7.0, (255, 120, 180), (255, 255, 255),
               richting=(0.6, 0.15, 0.8), seed=99)
    P.lab.append((cx + 30, ty + 52, cz + 16, "de wedstrijdvlieger"))
    # windstreepjes
    for i in range(40):
        x = cx - 50 + rng.uniform(0, 110)
        y = ZEE + 10 + rng.uniform(0, 60)
        z = cz + rng.uniform(-40, 40)
        P.q += MK.balk_quads((x, y, z), (x + rng.uniform(3, 7), y + 0.3, z), 0.08, (255, 255, 255))
    P.lab.append((cx, ty + 30, cz, "Vliegereiland"))
    return P


# ============================================================================================ Koraalbaai
KORAAL = ["minecraft:brain_coral_block", "minecraft:bubble_coral_block", "minecraft:horn_coral_block",
          "minecraft:tube_coral_block", "guhs:kaaskoraalblok"]
KORAAL_KL = ["guhs:nepkoraal[kleur=roze]", "guhs:nepkoraal[kleur=geel]", "guhs:nepkoraal[kleur=blauw]",
             "guhs:nepkoraal[kleur=oranje]", "minecraft:brain_coral", "minecraft:bubble_coral", "minecraft:horn_coral_fan",
             "minecraft:tube_coral_fan", "minecraft:sea_pickle[pickles=3,waterlogged=true]", "minecraft:seagrass"]
GROTJES = [(-14, 0, 8), (16, 2, 4), (-2, -14, 6)]   # (dx, dz, straal) t.o.v. het midden


def koraalbaai(leven=True):
    e = EIL["Koraalbaai"]
    cx, cz = e[1], e[2]
    P = BW.Plek(*_vak("Koraalbaai", 18, 26, 120))
    rng = np.random.default_rng(4)
    # koraaltuinen op de lagunebodem (bulten van koraalblokken met losse koralen ertussen)
    for i in range(70):
        a = rng.uniform(0, 2 * math.pi)
        r = rng.uniform(0, 0.55) * min(e[3], e[4])
        x, z = int(cx + math.cos(a) * r), int(cz + math.sin(a) * r)
        yb = _y(x, z)
        if yb >= ZEE - 1:
            continue
        b = KORAAL[i % len(KORAAL)]
        hh = rng.integers(1, 5)
        for j in range(hh):
            for (dx, dz) in ((0, 0), (1, 0), (0, 1)) if j < 2 else ((0, 0),):
                if yb + 1 + j < ZEE - 1:
                    P.s(x + dx, yb + 1 + j, z + dz, b)
        if yb + hh + 1 < ZEE:
            P.s(x, yb + hh + 1, z, KORAAL_KL[i % 8])
    for i in range(260):
        a = rng.uniform(0, 2 * math.pi)
        r = rng.uniform(0, 0.6) * min(e[3], e[4])
        x, z = int(cx + math.cos(a) * r), int(cz + math.sin(a) * r)
        yb = _y(x, z)
        if yb < ZEE - 1:
            P.s(x, yb + 1, z, KORAAL_KL[i % len(KORAAL_KL)])
    # onderwatergrotjes onder de lagunebodem: holtes met water, kristallen en zeekomkommerlicht
    for gi, (dx, dz, r) in enumerate(GROTJES):
        gx, gz = cx + dx, cz + dz
        gy = _y(gx, gz) - r + 2
        for x in range(gx - r - 1, gx + r + 2):
            for y in range(gy - r, gy + r):
                for z in range(gz - r - 1, gz + r + 2):
                    d = ((x - gx) / r) ** 2 + ((y - gy) / (r * 0.7)) ** 2 + ((z - gz) / r) ** 2
                    if d <= 1:
                        P.s(x, y, z, "minecraft:water")
        for j in range(12):
            a = rng.uniform(0, 2 * math.pi)
            x, z = int(gx + math.cos(a) * r * 0.75), int(gz + math.sin(a) * r * 0.75)
            y = int(gy - r * 0.55)
            P.s(x, y, z, ["guhs:weidekristal_roze", "minecraft:sea_pickle[pickles=4,waterlogged=true]",
                          "guhs:guh_kristal_cluster[facing=up]"][j % 3])
        P.lab.append((gx, gy, gz, "grotje %d" % (gi + 1)))
    # de Grauwe Zeeslurf (wereldbaas, nog grauw) in grotje 1
    gx, gz, r = cx + GROTJES[0][0], cz + GROTJES[0][1], GROTJES[0][2]
    gy = _y(gx, gz) - r + 2
    for x in range(gx - 4, gx + 4):
        for y in range(gy - 3, gy + 2):
            for z in range(gz - 3, gz + 3):
                if ((x - gx + 0.5) / 4) ** 2 + ((y - gy + 0.5) / 2.6) ** 2 + ((z - gz + 0.5) / 3) ** 2 <= 1:
                    P.s(x, y, z, "minecraft:gray_wool" if y > gy - 2 else "minecraft:light_gray_wool")
    for k in range(5):                                      # slurfjes / tentakeltjes
        P.f(gx - 3 + k * 1, gy - 5, gz + 2, gx - 3 + k, gy - 3, gz + 2, "minecraft:light_gray_wool")
    P.s(gx - 2, gy, gz + 3, "minecraft:black_concrete")
    P.s(gx + 1, gy, gz + 3, "minecraft:black_concrete")
    P.f(gx - 5, gy - 1, gz, gx - 7, gy - 1, gz, "minecraft:gray_wool")   # de slurf
    P.lab.append((gx, gy + 3, gz, "Grauwe Zeeslurf (wereldbaas)"))
    # duiksteiger van de noordring de lagune in
    BW.steiger(P, cx - 2, cz - 44, cx + 2, cz - 22, ZEE + 2, hek=True)
    P.f(cx - 1, ZEE + 2, cz - 21, cx + 1, ZEE + 2, cz - 19, "minecraft:cherry_planks")
    P.f(cx, ZEE + 3, cz - 19, cx, ZEE + 3, cz - 19, "minecraft:cherry_fence")
    P.lab.append((cx, ZEE + 9, cz - 30, "duiksteiger"))
    # schelpenhutjes op de ring
    for i, a in enumerate((20, 140, 200, 320)):
        h = math.radians(a)
        rr = min(e[3], e[4]) * 0.82
        x, z = int(cx + math.cos(h) * rr), int(cz + math.sin(h) * rr)
        y = _ymax(x - 2, z - 2, x + 2, z + 2) + 1
        if y <= ZEE + 1:
            continue
        for xx in range(x - 2, x + 3):
            for zz in range(z - 2, z + 3):
                for yy in range(y, y + 4):
                    d = ((xx - x) / 2.6) ** 2 + ((yy - y) / 4.2) ** 2 + ((zz - z) / 2.6) ** 2
                    if d <= 1 and d > 0.35:
                        P.s(xx, yy, zz, ["guhs:pleister_roze", "guhs:pastelbaksteen_wolkje"][(yy + xx) % 2])
        P.s(x, y + 4, z, "guhs:reuzenschelp[facing=south,parel=true]")
        P.s(x, y, z + 2, "minecraft:air")
        P.s(x, y + 1, z + 2, "minecraft:air")
        P.s(x + 1, y + 2, z, "guhs:lampion_roze[hanging=false]", keep=True)
    if leven:
        P.guhje(cx + 0.5, ZEE + 3, cz - 40.5, 180, "guh_zeemeerguh" if False else "guh_mint")
        P.guhje(cx + 0.5, ZEE + 3, cz - 21.5, 180, "guh")
        for i in range(20):  # belletjes in de lagune
            x = cx + rng.uniform(-25, 25)
            z = cz + rng.uniform(-25, 25)
            P.dl.append((x, ZEE + 1.1, z, (255, 255, 255), 0.35, 0, "stip"))
    P.lab.append((cx, 82, cz, "Koraalbaai"))
    P.lab.append((cx + 8, ZEE + 4, cz + 8, "roze lagune"))
    return P


def duiker():
    """Een zwemmende speler (horizontaal) in de lagune boven grotje 3, als quads."""
    e = EIL["Koraalbaai"]
    cx, cz = e[1], e[2]
    sp = MK.laad(os.path.join(MK.MODELS, "speler.geo.json"), os.path.join(MK.MODELS, "speler.png"))
    x, z = cx + 4, cz + 1
    y = _y(x, z) + 5
    base = MK.base_mat((x, y, z), 270, MK.SPELER_SCHAAL) @ MK.M4(MK.rotm([-75, 0, 0]))
    pose = {"arm_r": {"rot": [-170, 0, 10]}, "arm_l": {"rot": [-170, 0, -10]},
            "leg_r": {"rot": [18, 0, 0]}, "leg_l": {"rot": [-14, 0, 0]}, "head": {"rot": [40, 0, 0]}}
    q = MK.quads(sp, base, pose)
    bel = []
    for i in range(8):
        bel.append((x - 0.5 + 0.2 * math.sin(i), y + 1.6 + i * 1.1, z + 0.3 * math.cos(i), (235, 250, 255),
                    0.25 + 0.04 * i, 0.3, "stip"))
    return q, bel


def onderwater_post(kleur=(0.30, 0.66, 0.86), snij_z=None):
    """Doorsnede-nabewerking: alles onder de waterlijn blauwgroen met diepte en lichtvlekjes (caustics); het
    snijvlak door de bodem warm en iets donkerder, zodat je de grotjes als holtes ziet."""
    kl = np.array(kleur, np.float32)

    def f(rgb, al, P):
        y = np.nan_to_num(P[..., 1], nan=ZEE + 10)
        x = np.nan_to_num(P[..., 0])
        z = np.nan_to_num(P[..., 2], nan=-1e9)
        onder = np.clip((ZEE + 1 - y) / 1.5, 0, 1)
        snij = (snij_z is not None) & (z > (snij_z or 0) - 1.05)
        diep = np.clip((ZEE - y) / 26.0, 0, 1)
        t = (0.18 + 0.38 * diep) * onder * ~snij
        c = SK.vnoise(x + y * 0.4, z, cell=5, seed=91, oct=2)
        lichtje = np.clip((c - 0.2) * 3, 0, 1) * onder * (1 - diep) * 0.22 * ~snij
        out = rgb * (1 - t[..., None]) + kl * t[..., None] + lichtje[..., None] * np.array((0.75, 0.95, 1.0))
        s_ = snij[..., None] & (y[..., None] < ZEE + 0.5)
        out = np.where(s_, out * np.array((0.92, 0.84, 0.88)) + np.array((0.12, 0.07, 0.09)), out)
        return out
    return f


# ============================================================================================ Vuurtoreneiland
def vuurtoreneiland(leven=True):
    e = EIL["Vuurtoreneiland"]
    cx, cz = e[1], e[2]
    P = BW.Plek(*_vak("Vuurtoreneiland", 20, 50, 160))
    y = _ymax(cx - 6, cz - 6, cx + 6, cz + 6) + 1
    P.f(cx - 7, y - 1, cz - 7, cx + 7, y - 1, cz + 7, "guhs:gladde_knuffelsteen", )
    for xx in range(cx - 7, cx + 8):
        for zz in range(cz - 7, cz + 8):
            yb = _y(xx, zz)
            if yb < y - 1:
                P.f(xx, yb, zz, xx, y - 2, zz, "minecraft:calcite")
    BW.vuurtoren(P, cx, y, cz, h=38, r=6, label="De Grote Vuurtoren (met oortjes)")
    # huisje van de oude vuurtorenwachter
    hx, hz = cx + 8, cz + 2
    hy = _ymax(hx, hz, hx + 6, hz + 5) + 1
    BW.huis(P, hx, hy, hz, 7, 6, 4, "south", "pastelbaksteen_wolkje", "guh_dakpan_roze", oortjes=True)
    if leven:
        P.guhje(hx + 3.5, hy, hz + 7.5, 160, "guh_koorguh")
    # trapje naar beneden en een steigertje
    BW.steiger(P, cx - 3, cz + e[4] - 4, cx - 1, cz + e[4] + 10, ZEE + 2)
    BW.bootje(P, cx + 2, ZEE + 1, cz + e[4] + 6, "south")
    P.lab.append((cx, y + 60, cz, "Vuurtoreneiland"))
    return P


# ============================================================================================ Bloemeneiland
BLOEMEN = ["minecraft:pink_tulip", "guhs:roze_guhbloem", "minecraft:white_tulip", "minecraft:allium",
           "minecraft:oxeye_daisy", "guhs:roze_hibiscus", "minecraft:cornflower", "minecraft:lily_of_the_valley"]


def bloemeneiland(leven=True):
    e = EIL["Bloemeneiland"]
    cx, cz = e[1], e[2]
    P = BW.Plek(*_vak("Bloemeneiland", 20, 50, 120))
    # bloemenvelden in stroken (diagonaal)
    for x in range(cx - e[3], cx + e[3]):
        for z in range(cz - e[4], cz + e[4]):
            y, soort = KU.hoogte(x, z)
            if soort != "bloem" or y <= ZEE + 2:
                continue
            strook = ((x + z) // 4) % 6
            if strook == 5:
                continue
            if (x - cx) ** 2 + (z - cz) ** 2 < 14 ** 2 and z < cz:
                continue  # boerenerf
            P.s(x, y + 1, z, BLOEMEN[[0, 1, 2, 3, 5, 6][strook]])
    # bijenboerderij op het erf
    fx, fz = cx - 6, cz - 12
    fy = _ymax(fx, fz, fx + 8, fz + 6) + 1
    BW.huis(P, fx, fy, fz, 9, 6, 5, "south", "pastelbaksteen_boter", "guh_dakpan_roze", oortjes=True)
    for i in range(6):
        x, z = fx - 4 + (i % 3) * 3 + 12, fz + 8 + (i // 3) * 3
        y = _y(x, z) + 1
        P.s(x, y, z, "minecraft:beehive[facing=south,honey_level=5]")
        for j in range(3):
            P.dl.append((x + 0.5 + math.sin(i + j) * 1.4, y + 1.6 + j * 0.5, z + 0.5 + math.cos(i * 2 + j),
                         (250, 200, 40), 0.35, 0, "bij"))
    for i in range(3):
        P.s(fx - 2, _y(fx - 2, fz + 2 + i) + 1, fz + 2 + i, "guhs:roze_hooibaal[axis=y]")
    P.lab.append((fx + 4, fy + 11, fz + 3, "bijenboerderij"))
    # picknickkleedjes met taart
    for i, (dx, dz) in enumerate(((10, 12), (-14, 10), (18, -2))):
        x, z = cx + dx, cz + dz
        y = _y(x, z) + 1
        for a in range(3):
            for b in range(3):
                P.s(x + a, y, z + b, "minecraft:pink_carpet" if (a + b) % 2 else "minecraft:white_carpet")
        P.s(x + 1, y, z + 1, "guhs:guh_taart")
        if leven:
            P.guhje(x - 0.5, y, z + 1.5, 90, ["guh", "guh_mint", "guh_golden"][i], zit=True)
            P.guhje(x + 3.5, y, z + 1.5, 270, ["guh_pluisguh", "guh", "guh_mager"][i], zit=True)
    P.lab.append((cx, 86, cz, "Bloemeneiland"))
    return P


# ============================================================================================ Schildpadeiland
def schildpad(leven=True):
    x0, z0 = SCHILDPAD_POS
    # zwemrichting = raaklijn aan de ronde (tegen de klok in)
    tx, tz = -math.sin(_SA) * _SL[2], math.cos(_SA) * _SL[3]
    L = math.hypot(tx, tz)
    fx, fz = tx / L, tz / L
    P = BW.Plek(x0 - 40, 30, z0 - 40, x0 + 40, 110, z0 + 40)
    R, Rz, H = 24.0, 18.0, 12.0
    yb = ZEE - 3

    def lok(u, v):  # lokale (vooruit u, opzij v) -> spel
        return x0 + fx * u - fz * v, z0 + fz * u + fx * v
    for x in range(x0 - 26, x0 + 27):
        for z in range(z0 - 26, z0 + 27):
            u = (x - x0) * fx + (z - z0) * fz
            v = -(x - x0) * fz + (z - z0) * fx
            d = (u / R) ** 2 + (v / Rz) ** 2
            if d > 1:
                continue
            top = yb + H * math.sqrt(1 - d)
            for y in range(yb - 2, int(top) + 1):
                rand = d > 0.84
                # schildplaten: zeshoekig-achtig rooster met donkere naden
                cu, cv = u / 6.0, v / 6.0 + 0.5 * (int(math.floor(u / 6.0)) % 2)
                naad = (abs(cu - round(cu)) < 0.12) or (abs(cv - round(cv)) < 0.12)
                if rand:
                    b = "guhs:zeegroen_steen"
                elif y >= int(top) - 1 and d < 0.4:
                    b = "guhs:knuffelgras" if y == int(top) else "minecraft:pink_terracotta"
                else:
                    b = "minecraft:green_terracotta" if naad else "minecraft:lime_terracotta"
                P.s(x, y, z, b)
    # kop (vooruit), met lief gezicht
    hu = R + 4
    for x in range(x0 - 39, x0 + 40):
        for z in range(z0 - 39, z0 + 40):
            for y in range(ZEE - 3, ZEE + 5):
                u = (x - x0) * fx + (z - z0) * fz
                v = -(x - x0) * fz + (z - z0) * fx
                if ((u - hu) / 6.5) ** 2 + (v / 5.0) ** 2 + ((y - (ZEE + 2)) / 4.2) ** 2 <= 1 or                         (R - 3 < u < hu and abs(v) < 3.2 and ZEE - 2 < y < ZEE + 3):
                    P.s(x, y, z, "guhs:pastelbaksteen_pistache")
    for sgn in (-1, 1):
        for (du, dv, dy, b) in ((3.0, 2.6, 3, "minecraft:black_concrete"), (3.0, 2.6, 4, "minecraft:black_concrete"),
                                (3.4, 2.0, 4, "minecraft:white_concrete"), (2.8, 3.8, 2, "guhs:knuffelblok_roze")):
            ex, ez = lok(hu + du, sgn * dv)
            P.s(ex, ZEE + dy, ez, b)
    # vinnen
    for (u, v) in ((10, 1), (10, -1), (-12, 1), (-12, -1)):
        for t in range(9):
            x, z = lok(u + (2 if u > 0 else -1) * t * 0.3, v * (Rz - 1 + t))
            P.f(x, ZEE - 1, z, x + 1, ZEE, z + 1, "guhs:pastelbaksteen_pistache")
    # op de rug: hutje, palm, bloementuintje, lampionpaaltjes
    top = yb + int(H)
    hx, hz = lok(-4, -3)
    BW.huis(P, int(hx) - 3, top + 1, int(hz) - 3, 6, 6, 4, "south", "pleister_roze", "tiki_rietdak", oortjes=True)
    def boven(x, z):
        """U-DECO: the stand height on the shell at block (x, z) (the shell is a dome: its top varies per cell; the old code
        used the dome's peak for everything, so the flowers and lamp posts floated 1 block above the lower cells)."""
        x, z = int(math.floor(x)), int(math.floor(z))
        u_ = (x - x0) * fx + (z - z0) * fz
        v_ = -(x - x0) * fz + (z - z0) * fx
        d_ = (u_ / R) ** 2 + (v_ / Rz) ** 2
        return int(yb + H * math.sqrt(max(0.0, 1 - d_))) + 1

    for (u, v, b) in ((6, 4, "minecraft:pink_tulip"), (7, 3, "guhs:roze_guhbloem"), (5, 5, "minecraft:allium"),
                      (8, 5, "guhs:roze_hibiscus"), (6, 6, "minecraft:oxeye_daisy")):
        x, z = lok(u, v)
        P.s(x, boven(x, z), z, b)
    for (u, v) in ((4, -6), (-8, 6)):
        x, z = lok(u, v)
        BW.lantaarnpaal(P, int(x), boven(int(x), int(z)), int(z), u)
    if leven:
        x, z = lok(8, -2)
        P.guhje(x, boven(x, z), z, (math.degrees(math.atan2(-fx, -fz))) % 360, "guh_golden")
        # golfjes/schuim achter de schildpad
        for i in range(14):
            x, z = lok(-R - 2 - i * 1.6, (i % 3 - 1) * (3 + i * 0.6))
            P.s(x, ZEE + 1, z, "minecraft:white_carpet")
    P.palm_op = (lok(2, 7), boven(*lok(2, 7)))   # U-DECO: the real shell height
    P.lab.append((x0, top + 16, z0, "Schildpadeiland (zwemt zijn rondje)"))
    return P


# ============================================================================================ Knuffeltij-strand
GRAUW = ["guhs:knuffel_mager[facing=%s]", "guhs:knuffel_ghost[facing=%s]", "guhs:knuffel_snow[facing=%s]"]


def knuffeltij(leven=True):
    e = EIL["Knuffeltij-strand"]
    cx, cz = e[1], e[2]
    P = BW.Plek(*_vak("Knuffeltij-strand", 26, 40, 120))
    rng = np.random.default_rng(12)
    # grauwe knuffels langs de vloedlijn en in de branding (ook een paar grote, half in het water)
    for i in range(900):
        x = int(cx + rng.uniform(-e[3] - 14, e[3] + 14))
        z = int(cz + rng.uniform(-e[4] - 14, e[4] + 14))
        y, soort = KU.hoogte(x, z)
        if not (ZEE - 2 <= y <= ZEE + 1):
            continue
        if rng.random() < 0.55:
            continue
        fac = ["north", "east", "south", "west"][i % 4]
        P.s(x, max(y + 1, ZEE + 1) if y >= ZEE else ZEE, z, GRAUW[i % 3] % fac)
    for (dx, dz, r) in ((-30, 24, 2.4), (14, 30, 2.0), (36, 16, 1.8), (-44, 6, 2.0)):
        x, z = cx + dx, cz + dz
        y = max(_y(x, z), ZEE - 1)
        for xx in range(int(x - r - 2), int(x + r + 3)):
            for yy in range(y - 1, int(y + r * 2 + 2)):
                for zz in range(int(z - r - 2), int(z + r + 3)):
                    if ((xx - x) / r) ** 2 + ((yy - y - r) / r) ** 2 + ((zz - z) / r) ** 2 <= 1:
                        P.s(xx, yy, zz, "minecraft:light_gray_wool" if yy > y else "minecraft:gray_wool")
        for sgn in (-1, 1):   # oortjes
            P.f(int(x + sgn * r * 0.6), int(y + r * 2), int(z), int(x + sgn * r * 0.6), int(y + r * 2 + 1), int(z),
                "minecraft:gray_wool")
        P.s(int(x - 1), int(y + r * 1.2), int(z + r), "minecraft:black_concrete")
        P.s(int(x + 1), int(y + r * 1.2), int(z + r), "minecraft:black_concrete")
    P.lab.append((cx - 30, ZEE + 9, cz + 24, "Knuffeltij: grauwe knuffels spoelen aan"))
    # Opvangcentrum: groot pastelhuis met oortjes, wasplaats, droogveld
    ox, oz = cx - 10, cz - 20
    oy = _ymax(ox, oz, ox + 15, oz + 9) + 1
    BW.huis(P, ox, oy, oz, 16, 9, 7, "south", "pastelbaksteen_roze", "guh_dakpan_roze", oortjes=True,
            luifel="minecraft:pink_wool")
    P.f(ox + 5, oy + 7, oz + 9, ox + 10, oy + 8, oz + 9, "guhs:pastelbaksteen_framboos")  # bord
    P.s(ox + 7, oy + 9, oz + 9, "guhs:knuffelhart")
    P.lab.append((ox + 8, oy + 16, oz + 4, "Opvangcentrum voor knuffels"))
    # wastobbes met schuim
    for i in range(4):
        x, z = ox + 2 + i * 3, oz + 12
        y = _y(x, z) + 1
        P.s(x, y, z, "guhs:guh_wastobbe[facing=south,vulling=schuim]")
        for j in range(3):
            P.dl.append((x + 0.5 + 0.4 * math.sin(j + i), y + 1.2 + j * 0.6, z + 0.5, (255, 240, 250), 0.35, 0.2,
                         "stip"))
        if leven:
            P.guhje(x + 0.5, y, z + 1.5, 0, ["guh", "guh_mint", "guh_golden", "guh_pluisguh"][i])
    P.lab.append((ox + 6, oy + 5, oz + 12, "Knuffelwas"))
    # droogveld: lijnen met wasknijpers en knuffels eraan (de helft al weer roze!)
    for li, z in enumerate((oz + 17, oz + 21)):
        y1 = _y(ox, z) + 1
        y2 = _y(ox + 16, z) + 1
        P.f(ox, y1, z, ox, y1 + 3, z, "minecraft:cherry_fence")
        P.f(ox + 16, y2, z, ox + 16, y2 + 3, z, "minecraft:cherry_fence")
        BW.touw(P, (ox + 0.5, y1 + 3.6, z + 0.5), (ox + 16.5, y2 + 3.6, z + 0.5), zak=0.6, n=16)
        for k in range(1, 16, 2):
            yy = int(y1 + (y2 - y1) * k / 16 + 2.4 - 0.6 * 4 * (k / 16) * (1 - k / 16))
            b = GRAUW[k % 3] % "south" if (k + li) % 3 else "guhs:knuffel_normal[facing=south]"
            if (k + li * 2) % 4 == 0:
                b = "guhs:knuffel_pinguh[facing=south]"
            P.s(ox + k, yy, z, b)
    # guhs die knuffels naar binnen dragen en de zee afzoeken
    if leven:
        for i, (dx, dz, yaw) in enumerate(((-20, 18, 160), (-6, 16, 20), (6, 22, 170), (20, 14, 200), (-34, 18, 120))):
            x, z = cx + dx, cz + dz
            P.guhje(x + 0.5, _y(x, z) + 1, z + 0.5, yaw, ["guh", "guh_mint", "guh_golden", "guh_mager", "guh"][i])
    # soepkraampje voor de vluchtelingen
    sx, sz = ox + 18, oz + 4
    sy = _y(sx, sz) + 1
    P.f(sx, sy, sz, sx + 3, sy, sz, "guhs:roze_lakhout_planken")
    for (dx) in (0, 3):
        P.f(sx + dx, sy + 1, sz, sx + dx, sy + 2, sz, "minecraft:cherry_fence")
    P.f(sx - 1, sy + 3, sz - 1, sx + 4, sy + 3, sz + 1, "minecraft:pink_wool")
    if leven:
        P.guhje(sx + 1.5, sy, sz + 2.5, 180, "guh_mager")
        P.guhje(sx + 2.5, sy, sz + 3.5, 200, "guh_mager")
    P.lab.append((cx, 82, cz, "Knuffeltij-strand"))
    return P


# ============================================================================================ Mysterie-eiland
def mysterie(leven=True):
    e = EIL["Mysterie-eiland"]
    cx, cz = e[1], e[2]
    P = BW.Plek(*_vak("Mysterie-eiland", 20, 50, 120))
    y = _ymax(cx - 4, cz - 2, cx + 4, cz + 2) + 1
    # een vervallen grauwe poort met een verbleekt guhgezicht
    for dx in (-4, 4):
        for yy in range(y, y + 9 - (2 if dx > 0 else 0)):
            P.s(cx + dx, yy, cz, "guhs:gladde_knuffelsteen")
            P.s(cx + dx, yy, cz + 1, "minecraft:light_gray_concrete")
    for dx in range(-4, 2):
        P.s(cx + dx, y + 9, cz, "guhs:gladde_knuffelsteen")
    P.s(cx - 1, y + 8, cz, "guhs:knuffelsteen_gezicht[facing=south,stemming=2]")
    for i in range(6):
        P.s(cx + 6 + i % 3, _y(cx + 6 + i % 3, cz + i) + 1, cz + i, "guhs:gladde_knuffelsteen")
    P.lab.append((cx, y + 14, cz, "Mysterie-eiland (mist, flash van het keizerrijk)"))
    return P


# ============================================================================================ Pierkop + Opstijgpier
def opstijgpier(leven=True):
    e = EIL["Pierkop"]
    cx, cz = e[1], e[2]
    hv = KU.HAVENS["z4_pier"]
    px, pz, py = hv[1], hv[2], hv[3]
    P = BW.Plek(cx - 40, 30, cz - 40, px + 40, 160, pz + 40)
    # de pier: van het eilandje naar het oosten, op hoge palen, eindigend in een rond platform
    for x in range(cx, px + 1):
        for dz in range(-2, 3):
            P.s(x, py, pz + dz, "guhs:roze_lakhout_planken" if abs(dz) < 2 else "guhs:pastelbaksteen_wolkje")
        if (x - cx) % 6 == 0:
            for dz in (-2, 2):
                BW.palen(P, x, pz + dz, py - 1, "guhs:pastelbaksteen_wolkje")
                P.s(x, py + 1, pz + dz, "minecraft:cherry_fence")
                P.s(x, py + 2, pz + dz, BW.LAMPIONS[x % 3].replace("true", "false"))
    # verbinding met het eiland
    for x in range(cx - 6, cx + 1):
        for dz in range(-2, 3):
            yb = _y(x, pz + dz)
            P.f(x, yb + 1, pz + dz, x, py, pz + dz, "minecraft:air")
            P.s(x, max(yb + 1, py - (cx - x)), pz + dz, "guhs:roze_lakhout_planken")
    for x in range(px - 9, px + 10):
        for z in range(pz - 9, pz + 10):
            d = math.hypot(x - px, z - pz)
            if d <= 9.4:
                P.s(x, py, z, "guhs:pastelbaksteen_wolkje" if d > 7.5 else "guhs:pastelbaksteen_roze")
                if d > 8.5 and not (x < px and abs(z - pz) <= 2):   # W-BEREIK: open where the pier walkway comes in (west)
                    P.s(x, py + 1, z, "guhs:guh_hekje_wit")
            if d <= 9.4 and (round(math.degrees(math.atan2(z - pz, x - px))) % 45 == 0) and d > 8:
                BW.palen(P, x, z, py - 1, "guhs:pastelbaksteen_wolkje")
    # meermast voor het luchtschip (roze-wit, met oortjes)
    for y in range(py + 1, py + 30):
        b = "guhs:pastelbaksteen_roze" if ((y - py) // 3) % 2 else "guhs:pastelbaksteen_wolkje"
        P.f(px - 1, y, pz - 1, px + 1, y, pz + 1, b)
    P.f(px - 3, py + 30, pz - 3, px + 3, py + 30, pz + 3, "guhs:pastelbaksteen_wolkje")
    BW.oortjes_dak(P, px, py + 31, pz, "z", gap=4, h=4)
    P.s(px, py + 31, pz, "guhs:torenpeper_vuurtorenlamp[lit=true]")
    # vertrekbord
    P.f(px - 5, py + 1, pz - 6, px - 5, py + 3, pz - 6, "minecraft:cherry_fence")
    P.f(px - 6, py + 4, pz - 6, px - 4, py + 5, pz - 6, "guhs:pastelbaksteen_framboos")
    if leven:
        for i, (dx, dz) in enumerate(((-4, 1), (-2, -3), (3, 4), (-20, 0), (-34, 1))):
            P.guhje(px + dx + 0.5, py + 1, pz + dz + 0.5, 270, ["guh", "guh_mint", "guh_golden", "guh", "guh_mager"][i])
    P.lab.append((px, py + 40, pz, "De Opstijgpier"))
    return P


# ============================================================================================ Wrak van de Vliegervanger
def wrak(leven=True):
    wx, wz = KU.WRAK
    P = BW.Plek(wx - 20, 40, wz - 20, wx + 20, 100, wz + 20)
    # rotsje + scheef gestrand Mika-oorlogsvliegerschip (grauw, gescheurd vlieger-zeil)
    for x in range(wx - 8, wx + 9):
        for z in range(wz - 6, wz + 7):
            if ((x - wx) / 8) ** 2 + ((z - wz) / 6) ** 2 <= 1:
                yb = _y(x, z)
                P.f(x, yb, z, x, ZEE + 2 + int(2 * math.cos((x - wx) / 4)), z, "minecraft:tuff")
    for i in range(16):
        x = wx - 8 + i
        y = ZEE + 3 + i // 4
        P.f(x, y, wz - 2, x, y, wz + 2, "minecraft:dark_oak_planks")
        P.s(x, y + 1, wz - 2, "minecraft:gray_concrete")
        P.s(x, y + 1, wz + 2, "minecraft:gray_concrete")
    P.f(wx + 1, ZEE + 6, wz, wx + 1, ZEE + 20, wz, "minecraft:dark_oak_fence")
    for i in range(8):
        for j in range(1, 7 - i // 2):
            if (i * j) % 5 != 3:
                P.s(wx + 1 + j, ZEE + 19 - i, wz, "minecraft:gray_wool" if (i + j) % 3 else "minecraft:black_wool")
    P.lab.append((wx, ZEE + 26, wz, "Wrak van de Vliegervanger (kerker)"))
    return P


# ============================================================================================ het bootje van de speler
def speler_boot(leven=True):
    x, z, _ = BOOT_POS
    P = BW.Plek(x - 40, 50, z - 20, x + 20, 90, z + 20)
    BW.guh_zeilbootje(P, x, z, "east", speler=True, matroos=leven)
    # een paar knuffels die meedobberen (op weg naar het Knuffeltij-strand) en een zeemeeuw-guh
    for i, (dx, dz) in enumerate(((-12, 6), (-18, -5), (8, 9), (14, -7))):
        P.s(x + dx, ZEE + 1, z + dz, BW.LAMPIONS[0].replace("lampion_roze[hanging=true]", "knuffel_mager[facing=east]")
            if i % 2 == 0 else "guhs:knuffel_ghost[facing=south]")
    for i in range(5):
        P.s(x + 4 + i * 3, ZEE + 1, z - 12 + (i % 2) * 2, "guhs:guh_waterlelie" if i % 2 else "minecraft:lily_pad")
    return P


# ============================================================================================ alles
EILANDBOUW = {"strandfeest": strandfeest, "vlieger": vliegereiland, "koraal": koraalbaai, "vuurtoren": vuurtoreneiland,
              "bloemen": bloemeneiland, "schildpad": schildpad, "knuffeltij": knuffeltij, "mysterie": mysterie,
              "pier": opstijgpier, "wrak": wrak, "bootje": speler_boot}


def plek(naam, leven=True):
    if naam not in _CACHE:
        P = EILANDBOUW[naam](leven)
        _CACHE[naam] = P
        ax, ay, az = P.anker
        bez = np.argwhere((P.v[:, max(0, ZEE + 2 - ay):, :] > 0).any(axis=1))
        _BEZET.update((int(i) + ax, int(k) + az) for (i, k) in bez)
        _BEZET.update(P.ankers)
    return _CACHE[naam]


def vrij(x, z):
    return (x, z) not in _BEZET and (x + 1, z) not in _BEZET and (x, z + 1) not in _BEZET


def vak_van(naam):
    """Ruwe doos van een plek (zonder te bouwen), om te zien of hij een venster raakt."""
    if naam == "schildpad":
        x, z = SCHILDPAD_POS
        return (x - 40, 30, z - 40, x + 40, 110, z + 40)
    if naam == "pier":
        e = EIL["Pierkop"]
        hv = KU.HAVENS["z4_pier"]
        return (e[1] - 40, 30, e[2] - 40, hv[1] + 40, 160, hv[2] + 40)
    if naam == "wrak":
        wx, wz = KU.WRAK
        return (wx - 20, 40, wz - 20, wx + 20, 100, wz + 20)
    if naam == "bootje":
        x, z, _ = BOOT_POS
        return (x - 40, 50, z - 20, x + 20, 90, z + 20)
    e = EIL[{"strandfeest": "Strandfeesteiland", "vlieger": "Vliegereiland", "koraal": "Koraalbaai",
             "vuurtoren": "Vuurtoreneiland", "bloemen": "Bloemeneiland", "knuffeltij": "Knuffeltij-strand",
             "mysterie": "Mysterie-eiland"}[naam]]
    m = 80 if naam == "vlieger" else 40
    return (e[1] - e[3] - m, 26, e[2] - e[4] - m, e[1] + e[3] + m, 170, e[2] + e[4] + m)


def bouw(W, leven=True, alleen=None):
    for naam in (alleen or EILANDBOUW):
        if not W.raakt(*vak_van(naam)):
            continue
        P = plek(naam, leven)
        P.zet(W)
        if naam == "schildpad" and hasattr(P, "palm_op"):
            (x, z), y = P.palm_op
            KU.palm(W, int(x), y, int(z), h=8, seed=3)
