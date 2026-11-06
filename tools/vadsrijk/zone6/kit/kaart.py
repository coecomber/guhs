"""Bovenaanzicht (kaart) van zone 6 "Het Zijdeland", gemaakt uit precies dezelfde generators als de renders.

python kaart.py [snel]  -> ../00_kaart.png (zonder namen) en ../00_kaart_namen.png
Bouwt de hele zone in één venster (spelcoördinaten x 3200..4200, z -1080..-440), neemt per kolom het bovenste blok
(gemiddelde textuurkleur) met reliëfschaduw, en tekent de namen, levelbanden, routes en plekken erop.
1 blok = 2 px (snel: 1 px).
"""
import os, sys, math, time
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
import hoogkit  # noqa
import vibekit as V
import blokstudie as BS
import opname as O
import schemerkit as SK
import zijwereld as ZW

SNEL = "snel" in sys.argv
X1, Z1, X2, Z2 = 3200, -1080, 4200, -440


def kleuren(pal):
    r, bk = V.res()
    out = np.zeros((len(pal), 3), np.float32)
    for i, s in enumerate(pal):
        n = s.split("[")[0]
        if n == "minecraft:air":
            continue
        if n == "minecraft:water":
            out[i] = (96, 186, 226)
            continue
        try:
            t = BS.textuur_van(r, n)
            im = BS.laad(r, t) if t else None
        except Exception:
            im = None
        if im is None:
            out[i] = (240, 170, 200)
            continue
        a = np.asarray(im, np.float32)
        m = a[..., 3] > 128
        c = a[..., :3][m].mean(0) if m.any() else np.array((200, 200, 200))
        if "grass_block" in n or "short_grass" in n:
            c = np.array(c) * np.array((150, 214, 110)) / 255
        if "leaves" in n and "cherry" not in n and "bloesem" not in n:
            c = np.array(c) * np.array((120, 196, 90)) / 255
        out[i] = c
    return out


def bouw_alles():
    import rozenkloof as RK
    import saffraanpoort as SP
    import landschap as LS
    t = time.time()
    W = SK.Wereld(X1, 40, Z1, X2, 262, Z2, 1.0)
    ZW.bouw_land(W)
    O.log("land %.0fs" % (time.time() - t))
    RK.bouw(W)
    O.log("rozenkloof %.0fs" % (time.time() - t))
    SP.bouw(W, figuren=False)
    O.log("saffraanpoort %.0fs" % (time.time() - t))
    LS.bouw(W)
    import maak_renders as MR
    ZW.groen(W, uitsluit=MR._vrij_van_steden)
    O.log("alles gebouwd %.0fs" % (time.time() - t))
    return W


def main():
    W = bouw_alles()
    v = W.v
    X, Y, Z = v.shape
    nz = v != 0
    top = Y - 1 - np.argmax(nz[:, ::-1, :], axis=1)
    idx = np.take_along_axis(v, top[:, None, :], axis=1)[:, 0, :]
    kl = kleuren(W.pal)[idx]
    h = top.astype(np.float32)
    gx = np.gradient(h, axis=0)
    gz = np.gradient(h, axis=1)
    shade = np.clip(1 + (-gx * 0.55 - gz * 0.45) * 0.09, 0.55, 1.35)
    hoogte = np.clip((h - 30) / 160.0, 0, 1) * 0.3 + 0.82
    rgb = kl * shade[..., None] * hoogte[..., None]
    # zachte schaduw van hoge dingen (torens, koepels) naar het zuidoosten
    img = Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8).transpose(1, 0, 2))
    s = 1 if SNEL else 2
    img = img.resize((X * s, Z * s), Image.NEAREST)
    # rand: buiten het vak een tikje vervagen (het tegelvak bestaat niet in het spel; alleen ter oriëntatie)
    pad0 = os.path.join(O.OUT, "00_kaart.png")
    img.save(pad0)
    namen(img.copy(), s)
    O.log("kaart klaar", img.size)


def P(x, z, s):
    return ((x - X1) * s, (z - Z1) * s)


def tekst(d, xy, t, sz=26, kleur=(255, 255, 255), rand=(110, 40, 90), anker="mm"):
    f = O._font(sz)
    d.text(xy, t, font=f, fill=kleur, stroke_width=max(3, sz // 7), stroke_fill=rand, anchor=anker)


def namen(img, s):
    d = ImageDraw.Draw(img, "RGBA")
    # routes
    def lijn(pts, kleur, w=4, streep=None):
        q = [P(x, z, s) for (x, z) in pts]
        if streep:
            for i in range(len(q) - 1):
                (ax, ay), (bx, by) = q[i], q[i + 1]
                L = math.hypot(bx - ax, by - ay)
                n = max(1, int(L / streep))
                for k in range(0, n, 2):
                    t0, t1 = k / n, min(1, (k + 1) / n)
                    d.line([(ax + (bx - ax) * t0, ay + (by - ay) * t0), (ax + (bx - ax) * t1, ay + (by - ay) * t1)],
                           fill=kleur, width=w)
        else:
            d.line(q, fill=kleur, width=w, joint="curve")
    # karavaanroute (de sterrenkaravaan): Rozenkloof -> Siq -> Westpoort -> stad -> Oostpoort -> Zoutvlakte -> Moederoase
    route = [(3400, -716), (3452, -716)] + [(x, z) for (x, z, *_r) in ZW.SIQ] + ZW.WEG + [(3700, -812), (3830, -820)] + ZW.WEG_OOST
    lijn(route, (255, 236, 160, 230), w=3 * s, streep=7 * s)
    lijn(ZW.WEG_ZUID, (255, 236, 160, 200), w=2 * s, streep=5 * s)
    # lijn E (luchtschip) naar Hoogvads
    lijn([(3398, -786), (3300, -700), (3200, -600)], (255, 255, 255, 230), w=2 * s, streep=9 * s)
    tekst(d, P(3238, -628, s), "lijn E  ->  Hoogvads (Staarthaven)", sz=13 * s, anker="lm")
    tekst(d, P(4170, -560, s), "naar zone 7  ->", sz=12 * s, anker="rm")
    # gebieden + levels
    verplaats = {"De Zoutvlakte": (3945, -660), "Het Ruïnedal": (3940, -1036), "De Moederoase": (4070, -872),
                 "De Roze Duinen": (3640, -660)}
    for naam, (x, z), lv in ZW.GEBIEDEN:
        x, z = verplaats.get(naam, (x, z))
        tekst(d, P(x, z, s), naam, sz=19 * s)
        tekst(d, (P(x, z, s)[0], P(x, z, s)[1] + 20 * s), "lv " + lv, sz=12 * s, kleur=(255, 240, 200))
    # steden
    for (t, x, z) in (("ROZENKLOOF", 3392, -760), ("SAFFRAANPOORT", 3762, -955)):
        tekst(d, P(x, z, s), t, sz=17 * s, kleur=(255, 250, 230), rand=(150, 30, 80))
    # plekken
    plekken = [("Luchthaven (lijn E)", 3398, -786), ("de Schatkamer", 3478, -698), ("Rozenplein", 3400, -714),
               ("de Siq", 3500, -762), ("Rozijnenpaleis", 3762, -905), ("Spiegelzaal", 3762, -916),
               ("Grote Bazaar", 3762, -812), ("Saffraanbrug", 3810, -848), ("Karavanserai", 3679, -797),
               ("Sterrenwacht", 3846, -906), ("Westpoort", 3640, -806),
               ("De Sterrendovende Toren (raid)", ZW.TOREN[0], ZW.TOREN[1] + 12),
               ("De Verzonken Sterrenwacht (kerker)", 3985, -962), ("De Zingende Zoutgrot", 3874, -505),
               ("veldbaas: Koningin Wormelien", 3700, -600), ("veldbaas: De Zoutkolos (nacht)", 3995, -560),
               ("karavaankamp", 3906, -584), ("Zuidpoort + duinroute", 3758, -742)]
    for (t, x, z) in plekken:
        px, py = P(x, z, s)
        d.ellipse((px - 4 * s, py - 4 * s, px + 4 * s, py + 4 * s), fill=(255, 90, 150, 255), outline=(255, 255, 255),
                  width=s)
        if x > 4040:
            tekst(d, (px - 7 * s, py), t, sz=11 * s, anker="rm")
        else:
            tekst(d, (px + 7 * s, py), t, sz=11 * s, anker="lm")
    import landschap as LS
    for naam, (x, z, aan) in LS.BAKENS.items():
        px, py = P(x, z, s)
        r = 6 * s
        pts = []
        for k in range(10):
            a = k * math.pi / 5 - math.pi / 2
            rr = r if k % 2 == 0 else r * 0.45
            pts.append((px + rr * math.cos(a), py + rr * math.sin(a)))
        d.polygon(pts, fill=(255, 230, 120, 255) if aan else (170, 170, 190, 255), outline=(110, 40, 90))
        tekst(d, (px, py + 12 * s), naam.capitalize(), sz=10 * s, kleur=(255, 245, 200))
    # titel, legenda, schaal, noordpijl
    tekst(d, (20 * s, 18 * s), "Zone 6: Het Zijdeland (lv 60-70)", sz=24 * s, anker="lm")
    tekst(d, (20 * s, 42 * s), "x 3280..4180, z -1060..-460 (900 x 600)   boven = noord", sz=11 * s, anker="lm",
          kleur=(255, 240, 220))
    y = img.height - 26 * s
    d.rectangle((20 * s, y, 120 * s, y + 6 * s), fill=(255, 255, 255), outline=(110, 40, 90), width=s)
    tekst(d, (20 * s, y - 12 * s), "100 blokken", sz=11 * s, anker="lm")
    lx = 20 * s
    ly = 58 * s
    d.rounded_rectangle((lx, ly, lx + 280 * s, ly + 56 * s), 8 * s, fill=(60, 30, 60, 150))
    tekst(d, (lx + 10 * s, ly + 12 * s), "- - -  karavaanroute (hoofdverhaal)", sz=10 * s, anker="lm", kleur=(255, 236, 160))
    tekst(d, (lx + 10 * s, ly + 28 * s), "geel/grijs sterretje: sterrenbaken aan/gedoofd", sz=10 * s, anker="lm")
    tekst(d, (lx + 10 * s, ly + 44 * s), "roze stip: plek, baas, kerker, grot", sz=10 * s, anker="lm")
    img.save(os.path.join(O.OUT, "00_kaart_namen.png"))


if __name__ == "__main__":
    main()
