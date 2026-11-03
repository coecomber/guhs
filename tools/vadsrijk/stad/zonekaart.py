"""Zone 1 van Het Vadsrijk als geschilderde kaart (PIL, geen blokken). python zonekaart.py

Alle plekken staan in KAART (blokcoördinaten, x oost / z zuid, zone = 0..1200, Knuffelkamp ten zuiden ervan),
zodat dezelfde lijst later de basis kan zijn voor de echte indeling.
"""
import os, math, random
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.abspath(os.path.join(HERE, "..", ".."))
PX = 1.5                      # pixels per blok
OFF = 70                      # zeerand rondom (blokken)
BW, BH = 1200 + 2 * OFF, 1640 + OFF   # kaartgebied in blokken (zone 1200 x 1200 + zee + Knuffelkamp)
W, H = int(BW * PX), int(BH * PX)

# gebieden: naam, niveau, centrum, "straal" (voor de kleurvelden), kleur
GEBIEDEN = {
    "stad":     dict(naam="Zwamzoet", lv="stad", c=(600, 600), kl=(246, 196, 214)),
    "weide":    dict(naam="Klodderweide", lv="lv 1-6", c=(960, 620), kl=(170, 222, 130)),
    "woud":     dict(naam="Sporenwoud", lv="lv 6-13", c=(250, 700), kl=(96, 132, 104)),
    "grotten":  dict(naam="Druipgrotten", lv="lv 12-18", c=(240, 210), kl=(132, 140, 164)),
    "tempel":   dict(naam="De Zwamtempel", lv="lv 18-20 · kerker", c=(930, 190), kl=(176, 160, 150)),
    "zuid":     dict(naam="", lv="", c=(600, 1000), kl=(186, 220, 140)),
}
KAMP = dict(naam="Knuffelkamp", lv="intro · lv 1-3", c=(600, 1470), r=125)
STAD = dict(c=(600, 600), r=125)
HAVEN = (600, 742)
KAMPVUREN = {  # 2-3 per gebied
    "weide": [(830, 470), (1010, 800), (1080, 520)],
    "woud": [(380, 610), (170, 830), (300, 940)],
    "grotten": [(340, 300), (140, 140), (420, 120)],
    "tempel": [(800, 360), (1000, 300)],
}
UITKIJK = [(1120, 360, "Klodderkijk"), (90, 560, "Zwamnaald"), (500, 90, "Druipspits"), (760, 980, "Haventand"),
           (1060, 980, "Slijmnaald")]
ARENA = (1010, 690)            # Koning Klodder
SLAAPHEUVEL = (900, 900)       # slapende reuzenslijm-heuvel
FOSSIEL = [(735, 400), (810, 330)]  # drakenfossiel-brug over de kloof
KLOOF = [(640, 470), (700, 430), (770, 370), (840, 300), (880, 250), (960, 340), (1050, 380)]
BERG = (930, 190)
LITTEKENS = [  # het spoor van de draak (grauw)
    [(-40, 1180), (180, 1040), (360, 900), (470, 820), (560, 760)],
    [(620, 470), (760, 320), (900, 220), (1240, -40)],
    [(700, 1230), (820, 1050), (980, 920), (1240, 820)],
]
GROT_INGANGEN = [(300, 380), (180, 300), (420, 240)]
KRISTALKAMER = (110, 80)
LICHTSTRALEN = [(260, 720), (180, 640), (360, 820), (120, 760), (300, 560), (240, 900)]


def ruis(w, h, schalen=((6, 1.0), (14, 0.5), (36, 0.25), (90, 0.12)), seed=1):
    rng = np.random.default_rng(seed)
    out = np.zeros((h, w), np.float32)
    for (n, amp) in schalen:
        g = rng.random((n, max(2, int(n * w / h)))).astype(np.float32)
        im = Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
        out += amp * (np.asarray(im, np.float32) / 255.0 - 0.5)
    return out


def P(x, z):
    return ((x + OFF) * PX, (z + OFF) * PX)


def font(sz, bold=False, italic=False):
    naam = "segoeuib.ttf" if bold else ("segoeuii.ttf" if italic else "segoeui.ttf")
    for n in (naam, "georgiab.ttf" if bold else "georgia.ttf"):
        try:
            return ImageFont.truetype(n, sz)
        except OSError:
            pass
    return ImageFont.load_default()


def tekst(d, xy, s, sz=28, kl=(70, 30, 60), rand=(255, 248, 240), bold=True, anchor="mm", italic=False):
    d.text(xy, s, font=font(sz, bold, italic), fill=kl, stroke_width=max(2, sz // 9), stroke_fill=rand, anchor=anchor)


def teken(pad):
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    bx, bz = xx / PX - OFF, yy / PX - OFF
    n1 = ruis(W, H, seed=2)
    n2 = ruis(W, H, seed=5, schalen=((40, 1.0), (120, 0.6), (300, 0.3)))
    # ---- land: zone 1 (afgerond vierkant met grillige kust) + zuidbaai naar de haven + Knuffelkamp-eiland
    q = np.maximum(np.abs(bx - 600) / 600, np.abs(bz - 600) / 600)
    rond = np.hypot(np.maximum(np.abs(bx - 600) - 420, 0), np.maximum(np.abs(bz - 600) - 420, 0)) / 180
    land = np.maximum(q, rond) + n1 * 0.22 < 0.97
    baai = (np.abs(bx - 600 - 30 * np.sin(bz / 90)) < 70 + 30 * (bz - 740) / 460 + n1 * 30) & (bz > 745)
    land &= ~baai
    kamp = np.hypot(bx - KAMP["c"][0], (bz - KAMP["c"][1]) * 1.1) + n1 * 60 < KAMP["r"]
    land |= kamp
    # ---- gebiedskleuren (Voronoi met ruis, daarna zacht)
    namen = list(GEBIEDEN)
    dist = np.stack([np.hypot(bx - GEBIEDEN[k]["c"][0] + n1 * 160, bz - GEBIEDEN[k]["c"][1] + n2 * 160)
                     * (1.5 if k == "stad" else 1.0) for k in namen])
    idx = np.argmin(dist, 0)
    kl = np.zeros((H, W, 3), np.float32)
    for i, k in enumerate(namen):
        kl[idx == i] = GEBIEDEN[k]["kl"]
    kl[kamp] = (200, 230, 150)
    kl = np.asarray(Image.fromarray(kl.astype(np.uint8)).filter(ImageFilter.GaussianBlur(28)), np.float32)
    # ---- hoogte voor schaduw: berg NO, rotsen in de grotten, heuvels
    hgt = n2 * 8 + n1 * 10
    hgt += 260 * np.exp(-(((bx - BERG[0]) / 150) ** 2 + ((bz - BERG[1]) / 130) ** 2))
    hgt += 90 * np.exp(-(((bx - 230) / 260) ** 2 + ((bz - 200) / 200) ** 2)) * (1 + n1)
    hgt += 40 * np.exp(-(((bx - SLAAPHEUVEL[0]) / 70) ** 2 + ((bz - SLAAPHEUVEL[1]) / 50) ** 2))
    gy, gx = np.gradient(hgt)
    licht = np.clip(1 + (-gx * 0.7 - gy * 0.9) * 0.7, 0.72, 1.25)
    # kloof
    kloofm = np.zeros((H, W), np.uint8)
    km = Image.fromarray(kloofm)
    ImageDraw.Draw(km).line([P(*p) for p in KLOOF], fill=255, width=int(34 * PX), joint="curve")
    kloof = np.asarray(km.filter(ImageFilter.GaussianBlur(6)), np.float32) / 255.0
    # ---- littekens (grauw)
    lm = Image.new("L", (W, H), 0)
    ld = ImageDraw.Draw(lm)
    for lijn in LITTEKENS:
        ld.line([P(*p) for p in lijn], fill=255, width=int(70 * PX), joint="curve")
    lit = np.asarray(lm.filter(ImageFilter.GaussianBlur(30)), np.float32) / 255.0
    lit = np.clip(lit * (1.5 + n1 * 1.8), 0, 1)
    # ---- samenstellen
    tex = 1 + n1 * 0.14 + ruis(W, H, seed=9, schalen=((300, 0.5), (700, 0.4))) * 0.08
    rgb = kl * licht[..., None] * tex[..., None]
    grijs = rgb.mean(2, keepdims=True) * np.array([0.92, 0.9, 0.95])
    rgb = rgb * (1 - lit[..., None] * 0.85) + grijs * lit[..., None] * 0.85
    rgb = rgb * (1 - kloof[..., None]) + np.array([92, 70, 90]) * kloof[..., None]
    zee = np.array([150, 196, 226]) * (1 + n2[..., None] * 0.12)
    kust = np.asarray(Image.fromarray((land * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(18)),
                      np.float32) / 255.0
    ondiep = np.clip(kust * 2.2, 0, 1)[..., None]
    zee = zee * (1 - ondiep) + np.array([178, 222, 236]) * ondiep
    img = np.where(land[..., None], rgb, zee)
    # zandrandje
    rand = (kust > 0.25) & (kust < 0.62) & land
    img[rand] = img[rand] * 0.4 + np.array([246, 226, 196]) * 0.6
    # perkament-vignet
    vig = 1 - 0.25 * (((xx / W - 0.5) * 2) ** 2 + ((yy / H - 0.5) * 2) ** 2)
    img = np.clip(img * vig[..., None] * np.array([1.0, 0.98, 0.95]), 0, 255)
    im = Image.fromarray(img.astype(np.uint8)).convert("RGBA")
    d = ImageDraw.Draw(im, "RGBA")
    rng = random.Random(4)

    def binnen(x, z):
        px_, py_ = P(x, z)
        return 0 <= px_ < W and 0 <= py_ < H and land[int(py_), int(px_)]

    def gebied(x, z):
        px_, py_ = P(x, z)
        return namen[idx[int(py_), int(px_)]]

    # golflijntjes op zee
    for _ in range(420):
        x, z = rng.uniform(-OFF, BW - OFF), rng.uniform(-OFF, BH - OFF)
        if not binnen(x, z):
            cx, cy = P(x, z)
            d.arc((cx - 9, cy - 4, cx + 9, cy + 4), 200, 340, fill=(255, 255, 255, 120), width=2)
    # ---- bebossing en details per gebied
    for _ in range(5200):
        x, z = rng.uniform(0, BW), rng.uniform(0, 1220)
        if not binnen(x, z) or math.hypot(x - 600, z - 600) < 150:
            continue
        g = gebied(x, z)
        cx, cy = P(x, z)
        li = lit[int(cy), int(cx)] > 0.45
        if g == "woud" and rng.random() < 0.75:
            s = rng.uniform(6, 13)
            kap = (150, 150, 160) if li else rng.choice([(196, 80, 120), (214, 120, 160), (150, 90, 170), (120, 160, 190)])
            d.rectangle((cx - s * 0.18, cy - s * 0.2, cx + s * 0.18, cy + s * 0.8), fill=(236, 226, 214))
            d.pieslice((cx - s, cy - s * 0.9, cx + s, cy + s * 0.7), 180, 360, fill=kap, outline=(60, 40, 60, 160))
            if rng.random() < 0.3:
                d.ellipse((cx - 2, cy - s * 0.6, cx + 2, cy - s * 0.6 + 4), fill=(255, 255, 255, 220))
        elif g == "weide" and rng.random() < 0.18:
            s = rng.uniform(4, 7)
            if li:
                kk = (160, 160, 165)
            else:
                kk = rng.choice([(255, 150, 200), (120, 200, 255), (255, 220, 110), (150, 230, 140)])
            d.ellipse((cx - s, cy - s * 0.75, cx + s, cy + s * 0.5), fill=kk + (230,), outline=(70, 60, 80, 150))
            d.point([(cx - s * 0.3, cy - s * 0.2), (cx + s * 0.3, cy - s * 0.2)], fill=(30, 20, 40))
        elif g == "weide" and rng.random() < 0.12:
            s = rng.uniform(6, 9)
            d.ellipse((cx - s, cy - s, cx + s, cy + s * 0.6), fill=((150, 160, 150) if li else (110, 180, 90)) + (220,))
        elif g == "grotten" and rng.random() < 0.35:
            s = rng.uniform(7, 15)
            d.polygon([(cx - s, cy + s * 0.4), (cx - s * 0.2, cy - s), (cx + s * 0.6, cy - s * 0.3), (cx + s, cy + s * 0.4)],
                      fill=(150, 150, 172, 235) if not li else (160, 160, 160, 235), outline=(70, 70, 95, 200))
        elif g in ("zuid", "stad") and rng.random() < 0.2:
            s = rng.uniform(6, 10)
            d.ellipse((cx - s, cy - s, cx + s, cy + s * 0.7),
                      fill=((165, 165, 165) if li else rng.choice([(250, 190, 214), (244, 170, 200), (120, 186, 100)])) + (230,))
        elif g == "tempel" and rng.random() < 0.25:
            s = rng.uniform(8, 16)
            d.polygon([(cx - s, cy + s * 0.5), (cx, cy - s), (cx + s, cy + s * 0.5)], fill=(186, 170, 160, 230),
                      outline=(90, 70, 70, 200))
            d.polygon([(cx - s * 0.3, cy - s * 0.4), (cx, cy - s), (cx + s * 0.3, cy - s * 0.4)], fill=(255, 250, 252, 230))
    # vijvertjes in de weide
    for (x, z, r) in ((900, 520, 26), (1050, 600, 18), (870, 760, 22), (1110, 700, 15), (980, 470, 14), (1000, 860, 18)):
        cx, cy = P(x, z)
        rr = r * PX
        d.ellipse((cx - rr * 1.3, cy - rr, cx + rr * 1.3, cy + rr), fill=(140, 200, 236), outline=(90, 150, 200), width=3)
        d.ellipse((cx - rr * 0.6, cy - rr * 0.5, cx - rr * 0.1, cy - rr * 0.2), fill=(220, 240, 255, 180))
    # lichtstralen in het Sporenwoud
    straal = Image.new("RGBA", im.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(straal)
    for (x, z) in LICHTSTRALEN:
        cx, cy = P(x, z)
        sd.polygon([(cx - 12, cy), (cx + 12, cy), (cx + 70, cy - 220), (cx + 30, cy - 230)], fill=(255, 240, 170, 90))
        sd.ellipse((cx - 22, cy - 10, cx + 22, cy + 10), fill=(255, 245, 190, 140))
    im.alpha_composite(straal.filter(ImageFilter.GaussianBlur(6)))
    d = ImageDraw.Draw(im, "RGBA")
    # slapende reuzenslijm-heuvel
    cx, cy = P(*SLAAPHEUVEL)
    d.ellipse((cx - 95, cy - 70, cx + 95, cy + 45), fill=(120, 214, 150), outline=(50, 120, 80), width=4)
    d.ellipse((cx - 60, cy - 58, cx - 10, cy - 30), fill=(200, 255, 220, 160))
    d.arc((cx - 40, cy - 20, cx - 12, cy), 20, 160, fill=(30, 50, 40), width=4)
    d.arc((cx + 12, cy - 20, cx + 40, cy), 20, 160, fill=(30, 50, 40), width=4)
    for i, s in enumerate(("z", "z", "Z")):
        tekst(d, (cx + 70 + i * 22, cy - 80 - i * 26), s, 24 + i * 6, kl=(80, 120, 170), rand=(255, 255, 255))
    # Koning Klodder-arena
    cx, cy = P(*ARENA)
    d.ellipse((cx - 72, cy - 55, cx + 72, cy + 55), fill=(230, 214, 170), outline=(150, 110, 70), width=5)
    for k in range(16):
        a = k * math.pi / 8
        d.ellipse((cx + 72 * math.cos(a) - 6, cy + 55 * math.sin(a) - 6, cx + 72 * math.cos(a) + 6,
                   cy + 55 * math.sin(a) + 6), fill=(255, 150, 200))
    d.ellipse((cx - 30, cy - 26, cx + 30, cy + 18), fill=(255, 130, 190), outline=(120, 40, 80), width=3)
    d.polygon([(cx - 18, cy - 24), (cx - 18, cy - 44), (cx - 8, cy - 32), (cx, cy - 48), (cx + 8, cy - 32),
               (cx + 18, cy - 44), (cx + 18, cy - 24)], fill=(255, 210, 70), outline=(150, 100, 20))
    # berg + Zwamtempel
    cx, cy = P(*BERG)
    d.polygon([(cx - 40, cy + 10), (cx - 34, cy - 40), (cx, cy - 64), (cx + 34, cy - 40), (cx + 40, cy + 10)],
              fill=(220, 120, 160), outline=(110, 40, 70), width=3)
    d.ellipse((cx - 46, cy - 92, cx + 46, cy - 52), fill=(196, 60, 110), outline=(110, 40, 70), width=3)
    d.rectangle((cx - 9, cy - 22, cx + 9, cy + 10), fill=(120, 40, 70))
    for kant in (-1, 1):
        d.ellipse((cx + kant * 30 - 9, cy - 108, cx + kant * 30 + 9, cy - 80), fill=(255, 170, 205), outline=(110, 40, 70), width=2)
    # drakenfossiel-brug
    (ax, az), (bx_, bz_) = FOSSIEL
    A, B = P(ax, az), P(bx_, bz_)
    d.line([A, B], fill=(245, 240, 225), width=12)
    for t in np.linspace(0.08, 0.92, 9):
        mx, my = A[0] + (B[0] - A[0]) * t, A[1] + (B[1] - A[1]) * t
        nx, ny = -(B[1] - A[1]), (B[0] - A[0])
        L = math.hypot(nx, ny)
        nx, ny = nx / L * 22, ny / L * 22
        d.line([(mx - nx, my - ny), (mx + nx, my + ny)], fill=(245, 240, 225), width=6)
    d.ellipse((B[0] - 22, B[1] - 16, B[0] + 22, B[1] + 16), fill=(245, 240, 225), outline=(140, 120, 100), width=2)
    d.ellipse((B[0] - 4, B[1] - 6, B[0] + 6, B[1] + 2), fill=(70, 60, 60))
    # grotingangen + kristalkamer
    for (x, z) in GROT_INGANGEN:
        cx, cy = P(x, z)
        d.pieslice((cx - 20, cy - 20, cx + 20, cy + 20), 180, 360, fill=(50, 45, 70), outline=(30, 25, 40), width=2)
        for k in range(3):
            d.line([(cx - 10 + k * 10, cy - 18), (cx - 10 + k * 10, cy - 8 - (k % 2) * 4)], fill=(150, 220, 160), width=3)
    cx, cy = P(*KRISTALKAMER)
    for (u, v, s) in ((0, 0, 18), (-14, 6, 11), (13, 7, 12)):
        d.polygon([(cx + u - s * 0.4, cy + v), (cx + u, cy + v - s * 1.6), (cx + u + s * 0.4, cy + v)],
                  fill=(190, 160, 255, 230), outline=(110, 80, 190))
    # ---- paden
    def padlijn(pts, kl=(250, 240, 220), w=7):
        q_ = [P(*p) for p in pts]
        d.line(q_, fill=(120, 90, 80, 120), width=w + 4, joint="curve")
        d.line(q_, fill=kl, width=w, joint="curve")
        for p_ in q_:
            d.ellipse((p_[0] - w / 2, p_[1] - w / 2, p_[0] + w / 2, p_[1] + w / 2), fill=kl)
    padlijn([(690, 560), (820, 520), (960, 620), (1010, 690)])
    padlijn([(510, 620), (380, 640), (250, 720), (170, 830)])
    padlijn([(530, 520), (420, 380), (300, 300), (180, 200)])
    padlijn([(660, 500), (720, 410), (735, 400)])
    padlijn([(810, 330), (860, 280), (930, 230)])
    padlijn([(600, 700), (600, 742)])
    # ---- stad
    cx, cy = P(*STAD["c"])
    r = STAD["r"] * PX
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(250, 214, 228, 235), outline=(200, 90, 140), width=5)
    for (u, v, s, k) in ((0, -30, 52, (230, 90, 150)), (-70, -10, 34, (220, 50, 60)), (72, -18, 36, (236, 110, 170)),
                          (-80, 55, 28, (150, 100, 80)), (85, 55, 30, (220, 50, 60)), (-36, -86, 28, (150, 100, 80)),
                          (44, -90, 26, (236, 110, 170))):
        x0, y0 = cx + u * PX, cy + v * PX
        d.rectangle((x0 - s * 0.15, y0, x0 + s * 0.15, y0 + s * 1.1), fill=(240, 232, 220), outline=(120, 90, 90))
        d.pieslice((x0 - s, y0 - s * 0.8, x0 + s, y0 + s * 0.6), 180, 360, fill=k, outline=(90, 40, 60), width=3)
        for kk in range(3):
            d.ellipse((x0 - s * 0.5 + kk * s * 0.45, y0 - s * 0.45, x0 - s * 0.38 + kk * s * 0.45, y0 - s * 0.33),
                      fill=(255, 250, 240))
    tx, ty = cx, cy - 30 * PX
    for kant in (-1, 1):
        d.ellipse((tx + kant * 22 - 9, ty - 92, tx + kant * 22 + 9, ty - 64), fill=(255, 190, 215), outline=(90, 40, 60), width=2)
    # plein
    d.ellipse((cx - 22, cy + 105, cx + 22, cy + 140), fill=(250, 236, 220), outline=(170, 90, 130), width=2)
    # haven + luchtschiplijn
    hx, hy = P(*HAVEN)
    d.rectangle((hx - 60, hy - 6, hx + 60, hy + 6), fill=(236, 220, 200), outline=(140, 100, 90), width=2)
    kx, ky = P(*KAMP["c"])
    pts = [(hx + 60, hy + 10), (hx + 150, hy + 400), (kx + 60, ky - 230)]
    def bez(t):
        a_, b_, c_ = pts
        return ((1 - t) ** 2 * a_[0] + 2 * (1 - t) * t * b_[0] + t * t * c_[0],
                (1 - t) ** 2 * a_[1] + 2 * (1 - t) * t * b_[1] + t * t * c_[1])
    for i in range(46):
        t0, t1 = i / 46, (i + 0.55) / 46
        d.line([bez(t0), bez(t1)], fill=(170, 60, 120), width=5)
    ax_, ay_ = bez(0.5)
    d.ellipse((ax_ - 46, ay_ - 18, ax_ + 46, ay_ + 18), fill=(255, 160, 200), outline=(120, 40, 80), width=3)
    d.rectangle((ax_ - 16, ay_ + 20, ax_ + 16, ay_ + 30), fill=(190, 110, 90), outline=(120, 40, 80))
    for kant in (-1, 1):
        d.ellipse((ax_ + 18 + kant * 10 - 6, ay_ - 30, ax_ + 18 + kant * 10 + 6, ay_ - 12), fill=(255, 190, 215),
                  outline=(120, 40, 80), width=2)
    tekst(d, (ax_ + 60, ay_ + 58), "luchtschiplijn", 26, kl=(150, 50, 110), italic=True, bold=False)
    # Knuffelkamp
    cx, cy = P(*KAMP["c"])
    for (u, v) in ((-40, -10), (-10, 20), (25, -15), (50, 25)):
        d.polygon([(cx + u * PX - 16, cy + v * PX + 10), (cx + u * PX, cy + v * PX - 16), (cx + u * PX + 16, cy + v * PX + 10)],
                  fill=(255, 245, 230), outline=(160, 90, 110), width=2)
    d.ellipse((cx + 20, cy + 70, cx + 70, cy + 100), fill=(140, 200, 236), outline=(90, 150, 200), width=2)
    lx, ly = cx + 120, cy - 110
    d.rectangle((lx - 7, ly - 50, lx + 7, ly + 10), fill=(255, 255, 255), outline=(160, 80, 110), width=2)
    for k in range(3):
        d.rectangle((lx - 7, ly - 44 + k * 18, lx + 7, ly - 36 + k * 18), fill=(255, 150, 190))
    d.ellipse((lx - 10, ly - 62, lx + 10, ly - 46), fill=(255, 230, 120))
    # ---- kampvuren
    for g, lijst in KAMPVUREN.items():
        for (x, z) in lijst:
            cx, cy = P(x, z)
            d.ellipse((cx - 17, cy - 17, cx + 17, cy + 17), fill=(255, 250, 235, 230), outline=(160, 90, 60), width=3)
            d.polygon([(cx - 8, cy + 8), (cx, cy - 12), (cx + 8, cy + 8)], fill=(255, 150, 40))
            d.polygon([(cx - 4, cy + 8), (cx, cy - 3), (cx + 4, cy + 8)], fill=(255, 230, 90))
            d.line([(cx - 10, cy + 10), (cx + 10, cy + 10)], fill=(120, 70, 40), width=3)
    # ---- uitkijk-rotsnaalden
    for (x, z, n) in UITKIJK:
        cx, cy = P(x, z)
        d.polygon([(cx - 13, cy + 14), (cx - 5, cy - 40), (cx + 4, cy - 46), (cx + 13, cy + 14)], fill=(170, 150, 170),
                  outline=(80, 60, 80), width=2)
        d.regular_polygon((cx, cy - 58, 11), 5, fill=(255, 220, 90), outline=(150, 100, 20))
        tekst(d, (cx, cy + 32), n, 20, kl=(90, 60, 90), bold=False, italic=True)
    # schildpad-eiland in de verte
    cx, cy = P(1110, 1300)
    d.ellipse((cx - 60, cy - 30, cx + 60, cy + 30), fill=(140, 180, 120, 200), outline=(80, 110, 80), width=3)
    d.ellipse((cx + 52, cy - 14, cx + 80, cy + 8), fill=(160, 200, 140, 200), outline=(80, 110, 80), width=2)
    tekst(d, (cx, cy + 56), "Schildpad-eiland (in de verte)", 20, kl=(70, 90, 100), bold=False, italic=True)
    # ---- namen
    tekst(d, P(600, 428), "ZWAMZOET", 54, kl=(150, 30, 90))
    tekst(d, P(600, 456), "hoofdstad · haven · Tempel van De Knuffelorde", 22, kl=(120, 50, 90), bold=False)
    tekst(d, P(600, 772), "Haven + luchtschipmast", 24, kl=(80, 60, 110))
    for k, (x, z) in (("weide", (1010, 450)), ("woud", (260, 470)), ("grotten", (260, 40)), ("tempel", (930, 60))):
        g = GEBIEDEN[k]
        tekst(d, P(x, z), g["naam"].upper(), 46, kl=(60, 40, 70))
        tekst(d, P(x, z + 32), g["lv"], 28, kl=(130, 40, 100), bold=False)
    tekst(d, P(1010, 760), "Arena van Koning Klodder", 24, kl=(120, 50, 40))
    tekst(d, P(1010, 782), "wereldbaas · respawn ~20 min", 18, kl=(120, 80, 60), bold=False)
    tekst(d, P(900, 960), "Slapende reuzenslijm-heuvel", 22, kl=(40, 100, 70))
    tekst(d, P(690, 395), "Drakenfossiel-brug", 22, kl=(110, 80, 60))
    tekst(d, P(905, 330), "de Kloof", 22, kl=(90, 60, 90), bold=False, italic=True)
    tekst(d, P(110, 112), "verborgen kristalkamer", 18, kl=(110, 80, 190), bold=False, italic=True)
    tekst(d, P(290, 655), "lichtstralen", 18, kl=(150, 120, 40), bold=False, italic=True)
    tekst(d, P(380, 900), "grauw litteken", 24, kl=(90, 90, 100), bold=False, italic=True)
    tekst(d, P(800, 270), "grauw litteken", 22, kl=(90, 90, 100), bold=False, italic=True)
    tekst(d, P(940, 1010), "grauw litteken", 22, kl=(90, 90, 100), bold=False, italic=True)
    tekst(d, P(600, 1300), "KNUFFELKAMP", 40, kl=(60, 40, 70))
    tekst(d, P(600, 1326), KAMP["lv"] + " · rekrutenkamp op een klif-eiland", 22, kl=(130, 40, 100), bold=False)
    tekst(d, P(720, 1395), "vuurtoren", 18, kl=(90, 60, 90), bold=False, italic=True)
    # ---- titel, legenda, kompas, schaal
    d.rounded_rectangle((24, 24, 760, 132), 18, fill=(255, 248, 242, 230), outline=(200, 90, 140), width=3)
    tekst(d, (44, 64), "Het Vadsrijk · Zone 1", 44, kl=(150, 30, 90), anchor="lm")
    tekst(d, (46, 108), "slijmpjes en paddenstoelen · ca. 1200 x 1200 blokken · schets", 22, kl=(120, 70, 100),
          bold=False, anchor="lm")
    lx0, ly0 = 30, H - 520
    d.rounded_rectangle((lx0, ly0, lx0 + 430, ly0 + 480), 18, fill=(255, 248, 242, 235), outline=(200, 90, 140), width=3)
    tekst(d, (lx0 + 20, ly0 + 32), "Legenda", 30, anchor="lm")
    rij = ly0 + 80

    def leg(icoon, s):
        nonlocal rij
        icoon(lx0 + 40, rij)
        tekst(d, (lx0 + 74, rij), s, 21, bold=False, anchor="lm")
        rij += 44

    def i_vuur(x, y):
        d.ellipse((x - 15, y - 15, x + 15, y + 15), fill=(255, 250, 235), outline=(160, 90, 60), width=3)
        d.polygon([(x - 7, y + 7), (x, y - 10), (x + 7, y + 7)], fill=(255, 150, 40))

    def i_naald(x, y):
        d.polygon([(x - 9, y + 14), (x - 3, y - 12), (x + 3, y - 14), (x + 9, y + 14)], fill=(170, 150, 170), outline=(80, 60, 80))
        d.regular_polygon((x + 12, y - 12, 7), 5, fill=(255, 220, 90))

    def i_lit(x, y):
        d.rounded_rectangle((x - 18, y - 9, x + 18, y + 9), 8, fill=(165, 165, 170))

    def i_pad(x, y):
        d.line([(x - 18, y), (x + 18, y)], fill=(120, 90, 80), width=10)
        d.line([(x - 18, y), (x + 18, y)], fill=(250, 240, 220), width=6)

    def i_lucht(x, y):
        for k in range(3):
            d.line([(x - 18 + k * 13, y), (x - 11 + k * 13, y)], fill=(170, 60, 120), width=5)

    def i_arena(x, y):
        d.ellipse((x - 16, y - 12, x + 16, y + 12), fill=(230, 214, 170), outline=(150, 110, 70), width=3)

    def i_grot(x, y):
        d.pieslice((x - 15, y - 12, x + 15, y + 18), 180, 360, fill=(50, 45, 70))

    leg(i_vuur, "kampvuur-checkpoint (respawn)")
    leg(i_naald, "uitkijkpunt op een rotsnaald")
    leg(i_lit, "grauw litteken (drakenspoor)")
    leg(i_pad, "hoofdpad")
    leg(i_lucht, "luchtschiplijn")
    leg(i_arena, "baasarena")
    leg(i_grot, "grotingang")
    tekst(d, (lx0 + 20, rij + 10), "Stad -> tempel: ca. 10 min lopen", 19, kl=(120, 70, 100), bold=False, anchor="lm")
    # kompas
    kx, ky = W - 110, 200
    d.ellipse((kx - 70, ky - 70, kx + 70, ky + 70), fill=(255, 248, 242, 220), outline=(200, 90, 140), width=3)
    d.polygon([(kx, ky - 60), (kx - 14, ky), (kx + 14, ky)], fill=(200, 60, 120))
    d.polygon([(kx, ky + 60), (kx - 14, ky), (kx + 14, ky)], fill=(230, 210, 220))
    tekst(d, (kx, ky - 88), "N", 30)
    # schaalstok
    sx, sy = W - 380, H - 70
    for k in range(4):
        d.rectangle((sx + k * 50 * PX, sy, sx + (k + 1) * 50 * PX, sy + 12),
                    fill=(150, 40, 100) if k % 2 == 0 else (255, 240, 245), outline=(120, 40, 80))
    tekst(d, (sx + 100 * PX, sy - 22), "200 blokken", 22, bold=False)
    im.convert("RGB").save(pad)
    print(pad, im.size)


if __name__ == "__main__":
    teken(os.path.join(OUT, "zone_vadsrijk_zone1_kaart.png"))
