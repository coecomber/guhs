"""Hoogvads: paleis + Raadszaal, universiteit, markt + Grote Soepkeuken, kazerne/academie, parken, luchthaven.
Alle functies in wereldcoördinaten (k=1). Blokken via palet.P (zie palet.py)."""
import math
import numpy as np
import palet
from palet import P, trap, plaat
import bouwhulp as BH
import figuren as FG
import wolkwal as WW

GOUDPLAAT = lambda: plaat("goud_plaat")  # noqa: E731


# =================================================================================== paleis + raadszaal
def ui_koepel(W, cx, cy, cz, r, h, dak="dak_goud", rib="wit_glad", ribben=12, top=True):
    """Uienkoepel: buikig, loopt uit in een punt (profiel r(t))."""
    for i in range(int(h)):
        t = i / float(h)
        rr = r * (0.82 + 0.55 * math.sin(math.pi * min(1, t * 1.45))) * (1 - t) ** 0.55 if t < 0.98 else 0.4
        if t < 0.15:
            rr = max(rr, r * 0.86)
        for dx in range(-int(r * 1.5) - 1, int(r * 1.5) + 2):
            for dz in range(-int(r * 1.5) - 1, int(r * 1.5) + 2):
                d = math.hypot(dx, dz)
                if rr - 1.6 < d <= rr:
                    a = math.atan2(dz, dx)
                    blok = rib if abs(math.sin(a * ribben / 2)) < 0.14 else dak
                    BH.zet(W, cx + dx, cy + i, cz + dz, blok)
    if top:
        BH.spits(W, cx, cz, cy + int(h), 1.2, 5, dak="goudblok", hoeken=4, rand=None)


def paleis(W, S):
    y0 = 222
    BH.keermuur(W, -134, -48, -58, 48, y0, kanten=("n", "z", "w"))
    BH.keermuur(W, -58, -48, -58, -17, y0, kanten=("o",))
    BH.keermuur(W, -58, 17, -58, 48, y0, kanten=("o",), bodem_fn=lambda x, z: 207)
    BH.keermuur(W, -58, -48, -58, -17, y0, kanten=("o",), bodem_fn=lambda x, z: 207)
    # monumentale trap van de markt (208) naar de paleisheuvel (222)
    for k in range(15):
        x = -44 - k
        BH.doos(W, x, 207, -16, x, 208 + k, 16, "wit_glad")
        BH.doos(W, x, 208 + k, -15, x, 208 + k, 15, trap("wit_trap", "west"))
        for s in (-1, 1):
            BH.doos(W, x, 208 + k, s * 16, x, 209 + k, s * 16, "marmer_glad")
    for s in (-1, 1):
        for k in (0, 7, 14):
            BH.lantaarnpaal(W, -44 - k, 210 + k, s * 16, h=3)
        FG.ridder(W, -58.5 + 0.0, y0 + 1, s * 13.5, yaw=270)
        FG.ridder(W, -46.5, 210, s * 18.5, yaw=270)
    # plein voor de raadszaal
    BH.doos(W, -100, y0, -30, -59, y0, 30, "vloer")
    # ---------------- Raadszaal (tempel) x -100..-66, z -20..20
    xa, xb, za, zb = -98, -66, -20, 20
    for k in range(3):
        BH.doos(W, xa - k, y0 + 1 + k - 0, za - 2 + k, xb + 2 - k, y0 + 1 + k, zb + 2 - k, "wit_glad")
    yv = y0 + 4
    BH.doos(W, xa + 3, yv, za + 3, xb - 5, yv + 21, zb - 3, "marmer")       # cella
    for x in range(xa + 6, xb - 6, 5):
        for s in (-1, 1):
            BH.boograam(W, "z", s * (zb - 3) if s > 0 else za + 3, x + 0.5, yv + 4, 3, 10, glas="glas", kader="goud",
                        diep=1)
    # zuilen rondom (peristyle)
    for x in range(xa, xb + 1, 4):
        for z in (za, zb):
            BH.zuil(W, x, z, yv, yv + 20, dik=1, schacht="zuil_wit", kop="zuilkop", kap=None)
    for z in range(za + 4, zb, 4):
        for x in (xa, xb):
            BH.zuil(W, x, z, yv, yv + 20, dik=1, schacht="zuil_wit", kop="zuilkop", kap=None)
    # hoofdgestel
    BH.doos(W, xa - 1, yv + 21, za - 1, xb + 1, yv + 21, zb + 1, "goud")
    BH.doos(W, xa - 1, yv + 22, za - 1, xb + 1, yv + 23, zb + 1, "wit_glad")
    BH.doos(W, xa - 1, yv + 24, za - 1, xb + 1, yv + 24, zb + 1, "goud")
    # fronton (oostkant) + dak (nok langs x)
    BH.zadeldak(W, xa - 1, za - 1, xb + 1, zb + 1, yv + 25, nok="x", dak="dak_goud", overstek=1, gevel=P["wit_glad"])
    for k in range(0, 21):
        hw = 21 - k
        if hw < 0:
            break
        BH.doos(W, xb + 1, yv + 25 + k, -hw, xb + 1, yv + 25 + k, hw, "wit_glad")
    BH.doos(W, xb + 2, yv + 25, -21, xb + 2, yv + 25, 21, "goud")
    # reliëf in het fronton: guhgezicht in goud + oortjes
    for (dz, dy, blok) in ((0, 6, "marmer_gezicht"), (-1, 6, "goud"), (1, 6, "goud"), (0, 7, "goud"), (0, 5, "goud"),
                           (-3, 9, "goud"), (3, 9, "goud"), (-3, 10, "goud"), (3, 10, "goud")):
        BH.zet(W, xb + 2, yv + 25 + dy, dz, blok)
    for z in range(-15, 16, 6):
        BH.zet(W, xb + 2, yv + 27, z, "beeld")
        BH.zet(W, xb + 2, yv + 28, z, "beeld_gezicht")
    BH.zet(W, xb + 2, yv + 46, 0, "goudblok")
    # deur
    BH.doos(W, xb - 5, yv, -3, xb - 5, yv + 12, 3, "goudblok")
    W.label(xb, yv + 32, 0, "de Raadszaal (Hoge Raad)")
    # ---------------- Paleis x -134..-102
    # middenblok
    ma, mb = -134, -104
    BH.doos(W, ma, y0 + 1, -22, mb, y0 + 34, 22, "marmer")
    for y in (y0 + 1, y0 + 12, y0 + 23, y0 + 34):
        BH.doos(W, ma - 1, y, -23, mb + 1, y, 23, "wit_glad")
    BH.doos(W, ma - 1, y0 + 35, -23, mb + 1, y0 + 35, 23, "goud")
    BH.muren(W, ma - 1, y0 + 36, -23, mb + 1, y0 + 36, 23, "wit_muur")
    # pilasters + ramen op de oostgevel en zijkanten
    for z in range(-20, 21, 5):
        BH.doos(W, mb + 1, y0 + 2, z, mb + 1, y0 + 33, z, "wit_glad")
    for z in range(-18, 19, 5):
        for v in range(3):
            yb = y0 + 3 + v * 11
            BH.boograam(W, "x", mb, z + 0.5, yb, 2, 6, glas="raam", kader="goud", diep=1, spits=0.8)
    for x in range(ma + 3, mb - 1, 5):
        for v in range(3):
            for zz in (-22, 22):
                BH.boograam(W, "z", zz, x + 0.5, y0 + 3 + v * 11, 2, 6, glas="raam", kader="goud", diep=1, spits=0.8)
    # grote balkondeur + balkon
    BH.boograam(W, "x", mb, 0.5, y0 + 13, 6, 12, glas="glas_guh", kader="goud", diep=1)
    BH.doos(W, mb + 1, y0 + 12, -5, mb + 4, y0 + 12, 5, "wit_glad")
    BH.balustrade(W, mb + 4, -5, mb + 4, 5, y0 + 13, elke=5)
    BH.doos(W, mb + 1, y0 + 1, -4, mb + 1, y0 + 9, 4, "goudblok")
    for s in (-1, 1):
        BH.vaandel(W, mb + 2, y0 + 15, s * 9, "east")
        BH.vaandel(W, mb + 2, y0 + 26, s * 14, "east")
    # trommel + grote uienkoepel
    cx = (ma + mb) // 2
    BH.koepel(W, cx, y0 + 37, 0, 12, ry=1, dak="marmer_glad", rib="wit_glad", trommel=5, trommel_blok="marmer_glad",
              ramen="glas", lantaarn=False)
    ui_koepel(W, cx, y0 + 43, 0, 13, 22, dak="dak_goud", rib="wit_glad", ribben=14)
    W.label(cx, y0 + 70, 0, "paleis van Koningin Zachtmoed")
    # vleugels
    for s in (-1, 1):
        za_, zb_ = (23, 44) if s > 0 else (-44, -23)
        BH.doos(W, ma + 4, y0 + 1, za_, mb - 4, y0 + 24, zb_, "marmer")
        BH.doos(W, ma + 3, y0 + 25, za_ - (0 if s > 0 else 1), mb - 3, y0 + 25, zb_ + (1 if s > 0 else 0), "goud")
        BH.muren(W, ma + 3, y0 + 26, min(za_, zb_) - 1, mb - 3, y0 + 26, max(za_, zb_) + 1, "wit_muur")
        for x in range(ma + 6, mb - 5, 5):
            for v in range(2):
                BH.boograam(W, "z", s * 44 if s > 0 else -44, x + 0.5, y0 + 4 + v * 11, 2, 6, glas="raam",
                            kader="goud", diep=1, spits=0.8)
        for z in range(min(za_, zb_) + 2, max(za_, zb_) - 1, 5):
            for v in range(2):
                BH.boograam(W, "x", mb - 4, z + 0.5, y0 + 4 + v * 11, 2, 6, glas="raam", kader="goud", diep=1,
                            spits=0.8)
        BH.koepel(W, (ma + mb) // 2, y0 + 26, s * 33, 7, ry=8, dak="dak_goud", rib="wit_glad", ribben=8, trommel=4,
                  trommel_blok="marmer_glad", ramen="glas")
        # hoektorens met guhgezicht
        for xt in (ma + 2, mb - 2):
            BH.ronde_toren(W, xt, s * 44, y0 + 1, 36, 6, wand=P["marmer"], rand=P["wit_glad"], dak="dak_roze",
                           dak_h=22, gezicht=True)
    # troonzaal binnenin (x -131..-107, z -12..12, y0+1..y0+30)
    troonzaal(W, y0)


def troonzaal(W, y0):
    xa, xb, za, zb = -131, -108, -13, 13
    BH.leeg(W, xa, y0 + 1, za, xb, y0 + 31, zb)
    BH.doos(W, xa, y0, za, xb, y0, zb, "vloer")
    BH.doos(W, xa + 4, y0, -2, xb, y0, 2, "minecraft:pink_wool")
    BH.doos(W, xa + 4, y0, -3, xb, y0, -3, "goud")
    BH.doos(W, xa + 4, y0, 3, xb, y0, 3, "goud")
    # zuilen langs de zaal + bogen
    for x in range(xa + 3, xb, 5):
        for s in (-1, 1):
            BH.zuil(W, x, s * 9, y0, y0 + 22, dik=1, schacht="zuil", kop="zuilkop", kap=None)
            BH.doos(W, x, y0 + 23, s * 9, x, y0 + 23, s * 12, "goud")
    # gewelf
    for x in range(xa, xb + 1):
        for dz in range(za, zb + 1):
            q = abs(dz) / 13.0
            y = y0 + 24 + 7 * (1 - q ** 2) ** 0.5
            BH.zet(W, x, int(y), dz, "goud" if (x - xa) % 5 == 3 else "marmer_tegels")
    # ramen naar buiten (zuid en noord) met kathedraalglas
    for x in range(xa + 5, xb - 1, 5):
        for zz in (-22, 22):
            BH.boograam(W, "z", zz, x + 0.5, y0 + 13, 2, 7, glas="glas_hart", kader="goud", diep=10 if zz < 0 else 1,
                        spits=0.8)
    # verhoging + troon + baldakijn aan de westkant
    for k in range(4):
        BH.doos(W, xa + 1, y0 + 1 + k, -6 + k, xa + 6 - k, y0 + 1 + k, 6 - k, "wit_glad" if k < 3 else "vloer")
    tx = xa + 3
    # troon: hoge rugleuning met guh-oortjes, armleuningen, roze kussens (3 breed)
    BH.doos(W, tx, y0 + 5, -1, tx, y0 + 11, 1, "goudblok")
    BH.doos(W, tx, y0 + 6, 0, tx, y0 + 10, 0, "minecraft:pink_wool")
    for s_ in (-1, 1):
        BH.doos(W, tx, y0 + 12, s_, tx, y0 + 13, s_, "goudblok")
        BH.doos(W, tx + 1, y0 + 5, s_ * 2, tx + 2, y0 + 6, s_ * 2, "goudblok")
        BH.zet(W, tx + 2, y0 + 7, s_ * 2, "minecraft:white_candle[candles=1,lit=true,waterlogged=false]")
    BH.zet(W, tx, y0 + 12, 0, "goudblok")
    BH.doos(W, tx + 1, y0 + 5, -1, tx + 2, y0 + 5, 1, "minecraft:pink_wool")
    BH.doos(W, tx + 3, y0 + 4, -1, tx + 3, y0 + 4, 1, "minecraft:pink_carpet")
    # baldakijn
    for (x, z) in ((xa + 1, -6), (xa + 1, 6), (xa + 6, -6), (xa + 6, 6)):
        BH.doos(W, x, y0 + 4, z, x, y0 + 18, z, "goudblok" if palet.MODUS == "vanilla" else P["zuil_wit"])
    BH.doos(W, xa + 1, y0 + 19, -6, xa + 6, y0 + 19, 6, "goud")
    BH.doos(W, xa + 1, y0 + 20, -5, xa + 6, y0 + 20, 5, "minecraft:pink_wool")
    BH.doos(W, xa + 2, y0 + 21, -3, xa + 5, y0 + 21, 3, "minecraft:pink_wool")
    BH.zet(W, xa + 3, y0 + 22, 0, "goudblok")
    for z in range(-5, 6, 2):
        BH.zet(W, xa + 6, y0 + 18, z, "minecraft:pink_wool")
    # het wandkleed met het wapen erachter
    BH.doos(W, xa, y0 + 5, -4, xa, y0 + 24, 4, "minecraft:pink_wool")
    for y in range(y0 + 8, y0 + 22, 2):
        BH.vaandel(W, xa + 1, y, -4, "east")
        BH.vaandel(W, xa + 1, y, 4, "east")
    # kroonluchters
    import kathedraal as KA
    for x in (xa + 8, xa + 15, xa + 21):
        KA.luchter(W, x, y0 + 16, 0)
    W.troon = (tx + 1.5, y0 + 6.0, 0.5)



def paleis_figuren(W):
    y0 = 222
    tx, ty, tz = W.troon
    FG.koningin(W, tx, ty, tz, yaw=270)
    for x in range(-122, -108, 4):
        for s in (-1, 1):
            FG.ridder(W, x + 0.5, y0 + 1, s * 6.5, yaw=0 if s > 0 else 180)
    FG.menigte(W, -118, -4, -110, 4, y0 + 1, 6, seed=3, kijk=270)
    BH.npc(W, "guh_npc_burgemeesterguh", -122.5, y0 + 1, 5.5, yaw=300)
    BH.npc(W, "guh_npc_professor_zei_guh", -122.5, y0 + 1, -5.5, yaw=240)
    # raadsleden en bezoekers op het plein
    FG.menigte(W, -96, -26, -60, 26, y0 + 1, 40, seed=8)


# =================================================================================== universiteit
def universiteit(W, S):
    y0 = 204
    xa, xb, za, zb = -46, 36, -80, -30
    d = 12
    BH.doos(W, xa, y0, za, xb, y0, zb, P["vloer2"])
    hoog = 20
    for (x1, z1, x2, z2) in ((xa, za, xb, za + d), (xa, zb - d, xb, zb), (xa, za, xa + d, zb), (xb - d, za, xb, zb)):
        BH.doos(W, x1, y0 + 1, z1, x2, y0 + hoog, z2, "guhs:pastelbaksteen_roze" if palet.MODUS != "vanilla"
                else "minecraft:bricks")
        for y in (y0 + 1, y0 + 8, y0 + 15):
            BH.doos(W, x1, y, z1, x2, y, z2, "wit_glad")
        BH.doos(W, x1, y0 + hoog, z1, x2, y0 + hoog, z2, "goud")
    # binnenplaats leeg + gras + kloostergang
    ia, ib, ja, jb = xa + d + 1, xb - d - 1, za + d + 1, zb - d - 1
    BH.leeg(W, ia, y0 + 1, ja, ib, y0 + 30, jb)
    BH.doos(W, ia + 3, y0, ja + 3, ib - 3, y0, jb - 3, "guhs:wolkenweide_gras" if palet.MODUS != "vanilla"
            else "minecraft:grass_block")
    for x in range(ia, ib + 1, 4):
        for z in (ja, jb):
            BH.zuil(W, x, z, y0, y0 + 6, dik=1, schacht="zuil_wit", kop="zuilkop", kap=None)
    for z in range(ja, jb + 1, 4):
        for x in (ia, ib):
            BH.zuil(W, x, z, y0, y0 + 6, dik=1, schacht="zuil_wit", kop="zuilkop", kap=None)
    BH.doos(W, ia, y0 + 7, ja, ib, y0 + 7, ja + 2, "wit_glad")
    BH.doos(W, ia, y0 + 7, jb - 2, ib, y0 + 7, jb, "wit_glad")
    BH.doos(W, ia, y0 + 7, ja, ia + 2, y0 + 7, jb, "wit_glad")
    BH.doos(W, ib - 2, y0 + 7, ja, ib, y0 + 7, jb, "wit_glad")
    # fontein in het midden
    cx, cz = (ia + ib) // 2, (ja + jb) // 2
    for dx in range(-4, 5):
        for dz in range(-4, 5):
            dd = math.hypot(dx, dz)
            if dd <= 4.3:
                BH.zet(W, cx + dx, y0 + 1, cz + dz, "wit_glad" if dd > 3.3 else "minecraft:water")
    BH.doos(W, cx, y0 + 1, cz, cx, y0 + 4, cz, "wit_glad")
    BH.zet(W, cx, y0 + 5, cz, "goudblok")
    for (x, z) in ((ia + 6, ja + 6), (ib - 6, jb - 6), (ia + 6, jb - 6), (ib - 6, ja + 6)):
        BH.bloesemboom(W, x, y0 + 1, z, h=5, r=3.6, seed=x * 7 + z)
    # ramen
    for x in range(xa + 2, xb - 1, 4):
        for v in range(3):
            for zz in (za, zb):
                BH.boograam(W, "z", zz, x + 0.5, y0 + 2 + v * 7, 2, 3, glas="raam", kader="wit_glad", diep=1,
                            spits=0.8)
    for z in range(za + 2, zb - 1, 4):
        for v in range(3):
            for xx in (xa, xb):
                BH.boograam(W, "x", xx, z + 0.5, y0 + 2 + v * 7, 2, 3, glas="raam", kader="wit_glad", diep=1,
                            spits=0.8)
    # daken (dakpan wit) per vleugel
    BH.zadeldak(W, xa, za, xb, za + d, y0 + hoog + 1, nok="x", dak="dak_wit", overstek=1)
    BH.zadeldak(W, xa, zb - d, xb, zb, y0 + hoog + 1, nok="x", dak="dak_wit", overstek=1)
    BH.zadeldak(W, xa, za + d + 1, xa + d, zb - d - 1, y0 + hoog + 1, nok="z", dak="dak_wit", overstek=0)
    BH.zadeldak(W, xb - d, za + d + 1, xb, zb - d - 1, y0 + hoog + 1, nok="z", dak="dak_wit", overstek=0)
    # poortgebouw (zuid, naar de markt) met klok
    gx = (xa + xb) // 2
    BH.doos(W, gx - 7, y0 + 1, zb - 2, gx + 7, y0 + 34, zb + 2, "marmer")
    BH.boograam(W, "z", zb + 2, gx + 0.5, y0 + 1, 7, 9, glas="minecraft:air", kader="goud", diep=1)
    BH.leeg(W, gx - 3, y0 + 1, zb - 12, gx + 3, y0 + 10, zb + 2)
    BH.rond_raam(W, "z", zb + 2, gx + 0.5, y0 + 24, 4.5, glas="wit_glad", lood="goud", spaken=12, kader="goud")
    BH.zadeldak(W, gx - 7, zb - 2, gx + 7, zb + 2, y0 + 35, nok="z", dak="dak_roze", overstek=1)
    BH.spits(W, gx, zb, y0 + 42, 3, 12, dak="dak_goud", hoeken=4)
    BH.vaandel(W, gx - 5, y0 + 14, zb + 3, "south")
    BH.vaandel(W, gx + 5, y0 + 14, zb + 3, "south")
    # hoektorentjes met kegeldak (minder vierkant silhouet)
    for (cx_, cz_) in ((xa, za), (xb, za), (xa, zb)):
        BH.ronde_toren(W, cx_, cz_, y0 + 1, hoog + 8, 4, wand=P["marmer"], rand=P["wit_glad"], dak="dak_roze",
                       dak_h=14, gezicht=False)
    # dakkapellen op de lange vleugels
    for x in range(xa + 4, xb - 3, 7):
        for (zz, f) in ((za - 1, "north"), (zb + 1, "south")):
            pass
    # bibliotheek-rotonde in de noordvleugel
    bx, bz = gx - 14, za + 6
    BH.koepel(W, bx, y0 + hoog + 1, bz, 11, ry=12, dak="dak_goud", rib="wit_glad", ribben=12, trommel=8,
              trommel_blok="marmer_glad", ramen="glas")
    W.label(bx, y0 + hoog + 30, bz, "Grote Bibliotheek")
    # sterrenwacht-toren op de noordoosthoek
    tx, tz = xb - 4, za + 4
    BH.doos(W, tx - 5, y0 + 1, tz - 5, tx + 5, y0 + 46, tz + 5, "marmer")
    for y in range(y0 + 10, y0 + 46, 9):
        BH.doos(W, tx - 6, y, tz - 6, tx + 6, y, tz + 6, "wit_glad")
        for (vl, c, u) in (("x", tx - 5, tz + 0.5), ("x", tx + 5, tz + 0.5), ("z", tz - 5, tx + 0.5),
                           ("z", tz + 5, tx + 0.5)):
            BH.boograam(W, vl, c, u, y + 2, 2, 4, glas="raam", kader="goud", diep=1)
    BH.koepel(W, tx, y0 + 47, tz, 6, ry=6, dak="wit_glad", rib="goud", ribben=6, lantaarn=False)
    for k in range(7):
        BH.zet(W, tx + 2 + k // 2, y0 + 50 + k, tz, "minecraft:copper_block" if True else None)
    W.label(tx, y0 + 62, tz, "sterrenwacht")
    W.label(gx, y0 + 50, zb, "Universiteit van Hoogvads")
    W.uni = dict(binnen=(ia, ib, ja, jb), y0=y0, poort=(gx, zb))


def uni_figuren(W):
    u = W.uni
    ia, ib, ja, jb = u["binnen"]
    y0 = u["y0"]
    FG.menigte(W, ia + 2, ja + 2, ib - 2, jb - 2, y0 + 1, 40, seed=21,
               skins=["guh", "guh_mint", "guh_wolk", "guh_uiltje", "guh_bloesemguh", "guh_pluisguh"],
               vrij=lambda x, z: math.hypot(x - (ia + ib) / 2, z - (ja + jb) / 2) > 5)
    for k, (x, z) in enumerate(((ia + 8, ja + 10), (ib - 10, jb - 6), (ia + 20, jb - 8))):
        BH.npc(W, "guh_npc_professor_zei_guh", x, y0 + 1, z, yaw=k * 120)
        for j in range(4):
            a = j * 1.4
            W.wguh(x + 2.5 * math.cos(a), y0 + 1, z + 2.5 * math.sin(a), math.degrees(-a) + 90, skin="guh_uiltje")


# =================================================================================== markt + soepkeuken
def kraam(W, x, y, z, rng, as_="x"):
    """Marktkraam 5x3 met gestreepte luifel en koopwaar."""
    strepen = [("minecraft:pink_wool", "minecraft:white_wool"), ("minecraft:light_blue_wool", "minecraft:white_wool"),
               ("minecraft:yellow_wool", "minecraft:white_wool"), ("minecraft:lime_wool", "minecraft:white_wool"),
               ("minecraft:magenta_wool", "minecraft:pink_wool")][rng.randint(5)]
    waar = ["minecraft:melon", "minecraft:pumpkin", "minecraft:hay_block", "minecraft:cake[bites=0]",
            "minecraft:honey_block", "guhs:gouden_kaasje", "minecraft:barrel[facing=up,open=false]",
            "minecraft:red_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]"]
    w, d = (5, 3) if as_ == "x" else (3, 5)
    for (dx, dz) in ((0, 0), (w - 1, 0), (0, d - 1), (w - 1, d - 1)):
        BH.doos(W, x + dx, y, z + dz, x + dx, y + 3, z + dz, "minecraft:pale_oak_fence")
    for dx in range(-1, w + 1):
        for dz in range(-1, d + 1):
            k = (dx if as_ == "x" else dz)
            BH.zet(W, x + dx, y + 4, z + dz, strepen[k % 2])
    for dx in range(w):
        BH.zet(W, x + dx, y, z + (0 if as_ == "x" else 0), "minecraft:stripped_cherry_wood[axis=y]")
    for dx in range(1, w - 1):
        BH.zet(W, x + dx, y + 1, z, waar[rng.randint(len(waar))])
    BH.zet(W, x + w // 2, y + 3, z + d // 2, "minecraft:lantern[hanging=true,waterlogged=false]")


def markt(W, S):
    y0 = 208
    xa, xb = -42, 40
    BH.doos(W, xa, y0, -24, xb, y0, 24, P["vloer2"])
    BH.doos(W, xa, y0, -5, xb, y0, 5, P["steen"])
    rng = np.random.RandomState(4)
    for x in range(xa + 2, 10, 7):
        for z in (-21, -15, 9, 15):
            if -16 < x < -4 and z > 0:
                continue
            kraam(W, x, y0 + 1, z, rng)
    # fontein van de koningin op het midden
    cx, cz = -10, 0
    for dx in range(-7, 8):
        for dz in range(-7, 8):
            dd = math.hypot(dx, dz)
            if dd <= 7.3:
                BH.zet(W, cx + dx, y0 + 1, cz + dz, "wit_glad" if dd > 6.2 else "minecraft:water")
    BH.doos(W, cx - 1, y0 + 1, cz - 1, cx + 1, y0 + 3, cz + 1, "wit_glad")
    import kathedraal as KA
    KA.beeld_guh(W, cx, y0 + 5, cz, 9, "beeld", "east")
    # lampionslingers over de laan
    for x in range(xa + 4, xb, 10):
        for z in range(-9, 10):
            y = y0 + 11 - int(3 * (1 - (z / 9.0) ** 2))
            if z % 3 == 0:
                BH.zet(W, x, y, z, ["guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint"][(z // 3) % 3]
                       if palet.MODUS != "vanilla" else "minecraft:pearlescent_froglight")
            else:
                BH.zet(W, x, y, z, "minecraft:iron_chain[axis=z,waterlogged=false]")
        for z in (-10, 10):
            BH.doos(W, x, y0 + 1, z, x, y0 + 11, z, "minecraft:pale_oak_fence")
    soepkeuken(W, 14, 6, 38, 24, y0)
    W.label(-10, y0 + 18, 0, "de Marktlaan + fontein")


def soepkeuken(W, xa, za, xb, zb, y0):
    """De Grote Soepkeuken: open hal met een reuzenketel, lange tafels en een rij vluchtelingen."""
    wand = "guhs:pastelbaksteen_perzik" if palet.MODUS != "vanilla" else "minecraft:orange_terracotta"
    BH.doos(W, xa, y0, za, xb, y0, zb, "minecraft:spruce_planks")
    # achterwand + zijwanden, open naar de laan (noordkant za)
    BH.doos(W, xa, y0 + 1, zb, xb, y0 + 12, zb, wand)
    BH.doos(W, xa, y0 + 1, za, xa, y0 + 12, zb, wand)
    BH.doos(W, xb, y0 + 1, za, xb, y0 + 12, zb, wand)
    for x in range(xa, xb + 1, 4):
        BH.doos(W, x, y0 + 1, za, x, y0 + 11, za, "minecraft:stripped_cherry_log[axis=y]")
    BH.doos(W, xa, y0 + 12, za, xb, y0 + 12, za, "minecraft:stripped_cherry_log[axis=x]")
    BH.zadeldak(W, xa, za, xb, zb, y0 + 13, nok="x", dak="dak_roze", overstek=1, gevel=wand)
    # luifel boven de opening
    for x in range(xa - 1, xb + 2):
        BH.zet(W, x, y0 + 9, za - 1, "minecraft:pink_wool" if x % 2 else "minecraft:white_wool")
        BH.zet(W, x, y0 + 8, za - 2, "minecraft:pink_wool" if x % 2 else "minecraft:white_wool")
    # bord
    BH.doos(W, xa + 6, y0 + 10, za - 1, xb - 6, y0 + 11, za - 1, "minecraft:stripped_cherry_wood[axis=x]")
    # reuzenketel achterin
    kx, kz = (xa + xb) // 2, zb - 5
    for dy in range(0, 5):
        for dx in range(-4, 5):
            for dz in range(-4, 5):
                dd = math.hypot(dx, dz)
                r = 3.5 + (0.6 if dy in (2, 3) else 0)
                if r - 1 < dd <= r:
                    BH.zet(W, kx + dx, y0 + 1 + dy, kz + dz, "minecraft:copper_block" if dy < 4 else "goud")
                elif dd <= r - 1 and dy == 3:
                    BH.zet(W, kx + dx, y0 + 1 + dy, kz + dz, "guhs:kaas_saus" if palet.MODUS != "vanilla"
                           else "minecraft:orange_concrete")
    BH.doos(W, kx - 1, y0 + 1, kz - 1, kx + 1, y0 + 1, kz + 1, "minecraft:campfire[facing=north,lit=true,"
                                                                "signal_fire=false,waterlogged=false]")
    W.bol((kx, y0 + 9, kz), 2.5, "guhs:wolkenblok_wit")
    W.bol((kx - 2, y0 + 12, kz - 3), 1.8, "guhs:wolkenblok_wit")
    # lange tafels met banken
    for z in (za + 4, za + 8):
        BH.doos(W, xa + 2, y0 + 1, z, xb - 2, y0 + 1, z, "minecraft:stripped_cherry_wood[axis=x]")
        for x in range(xa + 2, xb - 1, 2):
            BH.zet(W, x, y0 + 2, z, "minecraft:bowl" if False else "minecraft:air")
    W.soep = dict(box=(xa, za, xb, zb), y0=y0, ketel=(kx, kz))
    W.label((xa + xb) / 2, y0 + 22, (za + zb) / 2, "de Grote Soepkeuken")


def markt_figuren(W):
    y0 = 208
    FG.menigte(W, -40, -24, 12, 24, y0 + 1, 120, seed=31,
               vrij=lambda x, z: math.hypot(x + 10, z) > 8)
    s = W.soep
    xa, za, xb, zb = s["box"]
    # vluchtelingen (magere guhs) aan de tafels en in de rij
    for z in (za + 3, za + 5, za + 7, za + 9):
        for x in range(xa + 3, xb - 2, 2):
            if (x + z) % 3:
                W.wguh(x + 0.5, y0 + 1, z + 0.5, 0 if z in (za + 3, za + 7) else 180,
                       skin=["guh_mager", "guh_hongerig_3", "guh_hongerig_5", "guh_mager"][(x + z) % 4])
    for k in range(14):
        W.wguh(xa - 2 - k * 1.3, y0 + 1, za - 2.5 + 0.4 * math.sin(k), 90,
               skin=["guh_mager", "guh_hongerig_2", "guh_hongerig_6"][k % 3])
    kx, kz = s["ketel"]
    BH.npc(W, "guh_npc_bakkerguh", kx - 4.5, y0 + 1, kz - 0.5, yaw=180)
    BH.npc(W, "guh_npc_omaguh", kx + 4.5, y0 + 1, kz - 1.5, yaw=180)
    BH.npc(W, "guh_npc_luchtnon_guh", xa + 4.5, y0 + 1, za - 1.5, yaw=270)


# =================================================================================== kazerne / academie
def kazerne(W, S):
    y0 = 204
    xa, xb, za, zb = -46, 36, 30, 80
    muur = P["steen_bak"]
    BH.doos(W, xa, y0, za, xb, y0, zb, "guhs:geharkt_zand" if palet.MODUS != "vanilla" else "minecraft:sand")
    # dikke muren met weergang
    for (x1, z1, x2, z2) in ((xa, za, xb, za + 2), (xa, zb - 2, xb, zb), (xa, za, xa + 2, zb), (xb - 2, za, xb, zb)):
        BH.doos(W, x1, y0 + 1, z1, x2, y0 + 12, z2, muur)
        BH.doos(W, x1, y0 + 13, z1, x2, y0 + 13, z2, "wit_glad")
        BH.doos(W, x1, y0 + 6, z1, x2, y0 + 6, z2, "goudrand")
    for x in range(xa, xb + 1, 2):
        for z in (za, zb):
            BH.zet(W, x, y0 + 14, z, "wit_glad")
    for z in range(za, zb + 1, 2):
        for x in (xa, xb):
            BH.zet(W, x, y0 + 14, z, "wit_glad")
    # poortgebouw naar de laan (noordmuur)
    gx = (xa + xb) // 2
    BH.doos(W, gx - 6, y0 + 1, za - 2, gx + 6, y0 + 22, za + 3, "marmer")
    BH.boograam(W, "z", za - 2, gx + 0.5, y0 + 1, 5, 6, glas="minecraft:air", kader="goud", diep=6)
    BH.zadeldak(W, gx - 6, za - 2, gx + 6, za + 3, y0 + 23, nok="x", dak="dak_roze", overstek=1)
    for s in (-1, 1):
        BH.vaandel(W, gx + s * 4, y0 + 12, za - 3, "north")
    # vierkante hoektorens
    for (tx, tz) in ((xa + 2, za + 2), (xb - 2, za + 2), (xa + 2, zb - 2), (xb - 2, zb - 2)):
        BH.doos(W, tx - 4, y0 + 1, tz - 4, tx + 4, y0 + 26, tz + 4, muur)
        BH.doos(W, tx - 5, y0 + 27, tz - 5, tx + 5, y0 + 27, tz + 5, "wit_glad")
        BH.zadeldak(W, tx - 4, tz - 4, tx + 4, tz + 4, y0 + 28, nok="x", dak="dak_roze", overstek=1)
        BH.spits(W, tx, tz, y0 + 33, 1.5, 6, dak="dak_goud", hoeken=4, rand=None)
        for (vl, c, u) in (("x", tx - 4, tz + 0.5), ("x", tx + 4, tz + 0.5), ("z", tz - 4, tx + 0.5),
                           ("z", tz + 4, tx + 0.5)):
            BH.boograam(W, vl, c, u, y0 + 16, 2, 4, glas="raam", kader="goud", diep=1)
    # donjon / academie aan de zuidkant (buitenrand)
    BH.doos(W, gx - 14, y0 + 1, zb - 14, gx + 14, y0 + 30, zb - 2, "marmer")
    for y in (y0 + 10, y0 + 20, y0 + 30):
        BH.doos(W, gx - 15, y, zb - 15, gx + 15, y, zb - 1, "wit_glad")
    for x in range(gx - 12, gx + 13, 4):
        for v in range(3):
            BH.boograam(W, "z", zb - 14, x + 0.5, y0 + 2 + v * 10, 2, 5, glas="raam", kader="goud", diep=1)
    BH.zadeldak(W, gx - 14, zb - 14, gx + 14, zb - 2, y0 + 31, nok="x", dak="dak_roze", overstek=1)
    BH.ronde_toren(W, gx, zb - 8, y0 + 31, 18, 5, wand=P["marmer"], rand=P["wit_glad"], dak="dak_goud", dak_h=14)
    for s_ in (-1, 1):
        BH.ronde_toren(W, gx + s_ * 14, zb - 14, y0 + 1, 36, 3, wand=P["marmer"], rand=P["wit_glad"], dak="dak_roze",
                       dak_h=10)
    BH.vaandel(W, gx, y0 + 22, zb - 15, "north")
    # oefenplaats: doelen, poppen, wapenrekken
    for x in range(xa + 8, xb - 6, 6):
        BH.zet(W, x, y0 + 1, za + 6, "minecraft:target")
        BH.zet(W, x, y0 + 2, za + 6, "minecraft:target")
    for x in range(xa + 6, xa + 30, 5):
        BH.zet(W, x, y0 + 1, zb - 20, "minecraft:hay_block[axis=y]")
        BH.zet(W, x, y0 + 2, zb - 20, "minecraft:pink_wool")
    for x in range(xb - 26, xb - 4, 3):
        BH.zet(W, x, y0 + 1, za + 3, "minecraft:pale_oak_fence")
        BH.zet(W, x, y0 + 2, za + 3, "minecraft:iron_bars")
    W.kaz = dict(box=(xa, za, xb, zb), y0=y0)
    W.label(gx, y0 + 52, zb - 8, "Academie van de Wolkwacht")
    W.label(gx, y0 + 6, (za + zb) / 2, "oefenplein (elite-ridders)")


def kazerne_figuren(W):
    k = W.kaz
    xa, za, xb, zb = k["box"]
    y0 = k["y0"]
    # formatie van ridders in rijen (kijken naar de drilmeester in het westen)
    for i in range(10):
        for j in range(5):
            FG.ridder(W, xa + 18 + i * 2.2, y0 + 1, za + 14 + j * 2.4, yaw=270)
    BH.npc(W, "guh_npc_wachterguh", xa + 44.5, y0 + 1, za + 19, yaw=90)
    FG.ridder(W, xa + 44.5, y0 + 1, za + 16, yaw=90, pluim=(255, 220, 90), cape=(255, 220, 90))
    # duel in een cirkel
    for (x, z, yw) in ((xb - 16, zb - 26, 90), (xb - 13, zb - 26, 270)):
        FG.ridder(W, x, y0 + 1, z, yaw=yw)
    FG.menigte(W, xb - 20, zb - 31, xb - 9, zb - 21, y0 + 1, 10, seed=12,
               vrij=lambda x, z: math.hypot(x - (xb - 14.5), z - (zb - 26)) > 3.5)
    # wachters op de muur
    for x in range(xa + 6, xb - 4, 8):
        FG.ridder(W, x + 0.5, y0 + 14, za + 1.5, yaw=180 + 180)


# =================================================================================== parken
def staartpark(W, S):
    """Het Staartpark: bloesembomen, een vijver met brug, een gouden prieel en een uitkijkterras op de staart."""
    rng = np.random.RandomState(8)
    for x in range(-197, -153):
        for z in range(-60, 61):
            g = S.g(x, z)
            if g is None or S.rand[S.ix(x), S.iz(z)] or S.straat[S.ix(x), S.iz(z)]:
                continue
            # vijver
            if ((x + 174) / 13.0) ** 2 + ((z - 16) / 9.0) ** 2 < 1:
                BH.zet(W, x, g, z, "minecraft:water")
                BH.zet(W, x, g - 1, z, P["steen"])
                continue
            r = rng.rand()
            if r < 0.05:
                BH.zet(W, x, g + 1, z, ["minecraft:pink_tulip", "minecraft:allium", "minecraft:white_tulip",
                                        "minecraft:cornflower", "minecraft:oxeye_daisy"][rng.randint(5)])
    for k in range(26):
        x, z = rng.randint(-195, -155), rng.randint(-40, 41)
        g = S.g(x, z)
        if g is None or not S.vrij(x - 3, z - 3, x + 3, z + 3) or ((x + 174) / 15.0) ** 2 + ((z - 16) / 11.0) ** 2 < 1:
            continue
        BH.bloesemboom(W, x, g + 1, z, h=rng.randint(5, 8), r=rng.uniform(3.5, 5.5), seed=k)
        S.reserveer(x - 2, z - 2, x + 2, z + 2)
    # brug over de vijver
    for x in range(-179, -168):
        g = S.g(x, 16) or 190
        BH.zet(W, x, g + 1, 16, plaat("wit_plaat"))
        BH.zet(W, x, g + 1, 15, "minecraft:pale_oak_fence")
        BH.zet(W, x, g + 1, 17, "minecraft:pale_oak_fence")
    # prieel
    px, pz = -178, -18
    g = S.g(px, pz) or 190
    BH.doos(W, px - 5, g, pz - 5, px + 5, g, pz + 5, "wit_glad")
    for (dx, dz) in ((-4, -4), (4, -4), (-4, 4), (4, 4), (0, -5), (0, 5), (-5, 0), (5, 0)):
        BH.doos(W, px + dx, g + 1, pz + dz, px + dx, g + 6, pz + dz, P["zuil_wit"])
    BH.koepel(W, px, g + 7, pz, 6, ry=5, dak="dak_goud", rib="wit_glad", ribben=8, lantaarn=True)
    W.label(px, g + 18, pz, "prieel in het Staartpark")
    W.park = dict(prieel=(px, g, pz))
    S.reserveer(-199, -60, -153, 60)


def ooggaarde(W, S):
    """Kleine tuin met balkon aan de kopkant, uitkijkend op het oog en de vin."""
    rng = np.random.RandomState(3)
    pts = []
    for x in range(130, 173):
        for z in range(30, 96):
            g = S.g(x, z)
            if g is None or S.rand[S.ix(x), S.iz(z)]:
                continue
            pts.append((x, z, g))
            if rng.rand() < 0.06:
                BH.zet(W, x, g + 1, z, ["minecraft:pink_tulip", "minecraft:allium", "minecraft:peony[half=lower]"][
                    rng.randint(2)])
    for k in range(6):
        x, z = rng.randint(132, 168), rng.randint(32, 70)
        g = S.g(x, z)
        if g and S.vrij(x - 3, z - 3, x + 3, z + 3):
            BH.bloesemboom(W, x, g + 1, z, h=5, r=3.8, seed=k + 40)
            S.reserveer(x - 2, z - 2, x + 2, z + 2)
    # uitkijkbalkon: een halfrond balkon buiten de muur
    import wolkwal as WW_
    bx, bz = 190, 0
    for x in range(180, 201):
        zr = max(z for (xx, z, g) in pts if xx == x) if any(xx == x for (xx, z, g) in pts) else None
        if zr is None:
            continue
    W.ooggaarde = (150, 72)


def balkon(W, S, x, z, kant=1, r=7):
    """Halfrond balkon bovenop de stadsmuur dat over de rand steekt (voor het uitzicht).
    Geeft (x, y_vloer, z_rand_muur) terug."""
    g = None
    for zz in range(z, z + 40 * kant, kant):
        gg = S.g(x, zz)
        if gg is None:
            break
        g, z0 = gg, zz
    if g is None:
        return None
    yv = g + 5
    for dx in range(-r - 1, r + 2):
        for dz in range(-1, r + 2):
            d = math.hypot(dx, max(dz, 0))
            if d <= r + 0.5:
                BH.zet(W, x + dx, yv, z0 + kant * dz, "wit_glad")
                BH.zet(W, x + dx, yv - 1, z0 + kant * dz, "goud")
                BH.zet(W, x + dx, yv + 1, z0 + kant * dz, "minecraft:air")
                if d > r - 0.5 and dz > 0:
                    BH.zet(W, x + dx, yv + 1, z0 + kant * dz, "wit_muur")
    for k in range(1, 12):
        zz = z0 + kant * max(0, int(r - k * 0.8))
        BH.doos(W, x - 1, yv - 1 - k, zz, x + 1, yv - 1 - k, zz, "wit_glad")
    # trapje van de straat op de muur
    for k in range(6):
        BH.doos(W, x - 2, g + k, z0 - kant * (6 - k), x + 2, g + k, z0 - kant * (6 - k), "wit_glad")
    BH.lantaarnpaal(W, x - r + 1, yv + 1, z0 + kant * 2, h=3)
    BH.lantaarnpaal(W, x + r - 1, yv + 1, z0 + kant * 2, h=3)
    return (x, yv, z0)


# =================================================================================== luchthaven + luchtschepen
def luchtschip(W, x, y, z, richting=(1, 0), L=60, ballon=("minecraft:pink_wool", "minecraft:white_wool"),
               romp="minecraft:cherry_planks", gezicht=True, seed=0):
    """Luchtschip met ballon (guhgezicht + oortjes), gondel met dek, kajuit, propeller en wimpels.
    (x, y, z) = midden van het dek; richting = (dx, dz) eenheidsvector waar de neus naar wijst."""
    fx, fz = richting
    rx, rz = -fz, fx  # rechts

    def put(l, h, s, blok):
        BH.zet(W, x + fx * l + rx * s, y + h, z + fz * l + rz * s, blok)
    Lg = int(L * 0.55)
    # gondel (bootvorm)
    for l in range(-Lg // 2, Lg // 2 + 1):
        t = (l + Lg / 2) / Lg
        hw = 4.6 * math.sin(math.pi * min(1, 0.12 + t * 0.95)) ** 0.6
        diep = 5 * math.sin(math.pi * min(1, 0.05 + t)) ** 0.7
        for s in range(-5, 6):
            if abs(s) > hw:
                continue
            bodem = -diep * math.sqrt(max(0, 1 - (s / (hw + 0.6)) ** 2))
            for h in range(int(math.floor(bodem)), 1):
                blok = romp
                if h == 0:
                    blok = "minecraft:stripped_cherry_wood[axis=y]" if abs(s) >= hw - 0.8 else "guhs:bleekhout_planken" \
                        if palet.MODUS != "vanilla" else "minecraft:pale_oak_planks"
                elif h == -1:
                    blok = P["goud"]
                put(l, h, s, blok)
            if abs(s) >= hw - 0.8:
                put(l, 1, s, "minecraft:pale_oak_fence")
    # kajuit
    for l in range(-Lg // 2 + 3, -Lg // 2 + 12):
        for s in range(-3, 4):
            for h in range(1, 5):
                if l in (-Lg // 2 + 3, -Lg // 2 + 11) or abs(s) == 3:
                    put(l, h, s, P["raam"] if h in (2, 3) and (s == 0 or l % 3 == 0) else P["wit_glad"])
            put(l, 5, s, P["dak_roze"])
    # propeller
    for k in range(-3, 4):
        put(-Lg // 2 - 1, -2 + k, 0, "minecraft:spruce_planks")
        put(-Lg // 2 - 1, -2, k, "minecraft:spruce_planks")
    # ballon
    by = 16 + int(L * 0.05)
    rl, rh = L / 2.0, 10 + L * 0.06
    cx, cy, cz = x, y + by, z
    for l in range(-int(rl) - 1, int(rl) + 2):
        for s in range(-int(rh) - 1, int(rh) + 2):
            for h in range(-int(rh) - 1, int(rh) + 2):
                d = (l / rl) ** 2 + (s / rh) ** 2 + (h / rh) ** 2
                if d <= 1 and (l / (rl - 1.6)) ** 2 + (s / (rh - 1.6)) ** 2 + (h / (rh - 1.6)) ** 2 > 1:
                    blok = ballon[0] if (l // 6) % 2 == 0 else ballon[1]
                    if abs(h) <= 0 and True:
                        blok = P["goud"]
                    put(l, by + h, s, blok)
    # gezicht op de neus
    if gezicht:
        lf = int(rl) - 1
        for (s, h, blok) in ((-3, 2, "minecraft:black_concrete"), (3, 2, "minecraft:black_concrete"),
                             (-3, 3, "minecraft:black_concrete"), (3, 3, "minecraft:black_concrete"),
                             (-5, 0, "minecraft:pink_concrete"), (5, 0, "minecraft:pink_concrete"),
                             (0, 0, "minecraft:purple_terracotta")):
            for dl in range(0, 4):
                ll = lf - dl
                if (ll / rl) ** 2 + (s / rh) ** 2 + (h / rh) ** 2 <= 1.02:
                    put(ll + 1, by + h, s, blok)
                    break
        for sg in (-1, 1):
            for k in range(5):
                for w in range(-2 + k // 2, 3 - k // 2):
                    put(int(rl * 0.45) + w, by + int(rh) + k, sg * 5, ballon[0])
    # staartvinnen
    for k in range(7):
        for w in range(0, 7 - k):
            put(-int(rl) - 1 + w, by + k + 2, 0, ballon[0])
            put(-int(rl) - 1 + w, by - k - 2, 0, ballon[0])
            put(-int(rl) - 1 + w, by, k + 2, ballon[0])
            put(-int(rl) - 1 + w, by, -k - 2, ballon[0])
    # touwen
    for l in (-Lg // 2 + 2, 0, Lg // 2 - 2):
        for sg in (-1, 1):
            for h in range(2, by - int(rh) + 2):
                put(l, h, sg * 3, "minecraft:iron_chain[axis=y,waterlogged=false]")
    # wimpels
    for k in range(6):
        put(Lg // 2 + 1 + k // 2, 1 + k, 0, "minecraft:stripped_cherry_log[axis=y]" if k < 5 else P["goud"])


def haven(W, S):
    """De Luchthaven aan de noordflank: een station met glazen kap en drie pieren over de rand."""
    y0 = 206
    xa, xb, za, zb = 78, 168, -80, -56
    BH.doos(W, xa, y0, za, xb, y0, zb, P["vloer2"])
    # stationshal
    ha, hb = 96, 150
    BH.doos(W, ha, y0 + 1, za + 2, hb, y0 + 10, zb - 2, "marmer")
    BH.leeg(W, ha + 1, y0 + 1, za + 3, hb - 1, y0 + 10, zb - 3)
    for x in range(ha, hb + 1, 6):
        BH.boograam(W, "z", za + 2, x + 0.5, y0 + 1, 4, 5, glas="minecraft:air", kader="goud", diep=1)
        BH.boograam(W, "z", zb - 2, x + 0.5, y0 + 1, 4, 5, glas="minecraft:air", kader="goud", diep=1)
    # tongewelf van glas met gouden spanten
    cz = (za + zb) / 2.0
    hw = (zb - za) / 2.0 - 2
    for x in range(ha, hb + 1):
        for dz in range(-int(hw), int(hw) + 1):
            y = y0 + 10 + 9 * math.sqrt(max(0, 1 - (dz / (hw + 0.5)) ** 2))
            spant = (x - ha) % 6 == 0
            BH.zet(W, x, int(y), int(cz + dz), "goud" if spant or abs(dz) >= hw - 0.5 else
                   ("glas" if palet.MODUS != "vanilla" else "minecraft:white_stained_glass"))
    for x in (ha, hb):
        for dz in range(-int(hw), int(hw) + 1):
            ytop = y0 + 10 + 9 * math.sqrt(max(0, 1 - (dz / (hw + 0.5)) ** 2))
            for y in range(y0 + 1, int(ytop)):
                BH.zet(W, x, y, int(cz + dz), "marmer")
        BH.rond_raam(W, "x", x, cz + 0.5, y0 + 12, 5, glas="glas_roos", diep=1, kader="wit_glad")
    # klokkentoren
    tx, tz = (ha + hb) // 2, zb - 2
    BH.doos(W, tx - 4, y0 + 1, tz - 3, tx + 4, y0 + 36, tz + 3, "marmer")
    BH.rond_raam(W, "z", tz + 3, tx + 0.5, y0 + 28, 3.4, glas="wit_glad", lood="goud", spaken=12, kader="goud")
    BH.spits(W, tx, tz, y0 + 37, 5, 14, dak="dak_goud", hoeken=4)
    # pieren over de rand (naar het noorden)
    W.pieren = []
    for px in (108, 128, 148):
        zrand = za
        for z in range(za, -165, -1):
            BH.doos(W, px - 2, y0, z, px + 2, y0, z, "minecraft:stripped_cherry_wood[axis=y]" if z % 4 else P["goud"])
            BH.zet(W, px - 3, y0 + 1, z, "minecraft:pale_oak_fence")
            BH.zet(W, px + 3, y0 + 1, z, "minecraft:pale_oak_fence")
            if z % 10 == 0:
                BH.lantaarnpaal(W, px - 3, y0 + 1, z, h=3)
                BH.lantaarnpaal(W, px + 3, y0 + 1, z, h=3)
        # schoren naar de huid
        for z in range(za - 10, -125, -14):
            ex, ez = W.echt(px, z) if hasattr(W, "echt") else (px, z)
            r = WW.rug(ex + 0.5, ez + 0.5)
            yb = (r if not np.isnan(r) else WW.as_y(ex)) - (W._d[1] if hasattr(W, "_d") else 0)
            for k in range(0, 40):
                y = y0 - 1 - k
                zz = z + k * 0.6
                if y < yb:
                    break
                BH.zet(W, px, y, int(zz), "minecraft:stripped_cherry_log[axis=y]")
        # meertoren aan het eind
        BH.doos(W, px - 2, y0 + 1, -166, px + 2, y0 + 26, -162, P["wit_glad"])
        BH.doos(W, px - 3, y0 + 27, -167, px + 3, y0 + 27, -161, "goud")
        BH.zet(W, px, y0 + 28, -164, "minecraft:lantern")
        BH.vaandel(W, px, y0 + 20, -167, "north")
        W.pieren.append(px)
    W.label((ha + hb) / 2, y0 + 30, cz, "Luchthaven Hoogvads")
    W.haven = dict(box=(xa, za, xb, zb), y0=y0)


def haven_schepen(W):
    y0 = 206
    luchtschip(W, 117, y0 + 3, -128, richting=(0, -1), L=58, seed=1)
    luchtschip(W, 157, y0 + 3, -140, richting=(0, -1), L=48, ballon=("minecraft:light_blue_wool", "minecraft:white_wool"),
               seed=2)
    luchtschip(W, 99, y0 + 3, -150, richting=(0, -1), L=44, ballon=("minecraft:yellow_wool", "minecraft:white_wool"),
               seed=3)


def haven_figuren(W):
    y0 = 206
    xa, za, xb, zb = W.haven["box"]
    FG.menigte(W, xa + 2, za + 1, xb - 2, zb - 1, y0 + 1, 50, seed=41)
    for px in W.pieren:
        FG.menigte(W, px - 1.5, -150, px + 1.5, za, y0 + 1, 10, seed=px)
    BH.npc(W, "guh_npc_kapiteinguh", 128.5, y0 + 1, -100.5, yaw=0)
