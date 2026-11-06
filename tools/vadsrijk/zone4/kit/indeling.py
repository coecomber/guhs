"""Wereldindeling van Het Vadsrijk (voorstel, 2026-11-05): alle coordinaten op een plek.

Wereldcoordinaten in blokken (x = oost, z = zuid; op de kaart staat noorden boven). Alle verschuivingen zijn
veelvouden van 64, zodat een tegel (64x64 kolommen) een hele tegel blijft: tx' = tx + dx/64, tz' = tz + dz/64.
Wordt gebruikt door bovenaanzicht.py (beide kaarten) en door tabellen.py (de getallen in WERELDINDELING.md).
"""
import math

TEGEL = 64
SNELHEID = 8.0          # luchtschip, blokken per seconde (kruissnelheid; ~ een draf te paard)

# ------------------------------------------------------------------------------------------------ zones
# naam, oude oorsprong (het punt dat we volgen), dx, dz, landvak NA verschuiving (minx, minz, maxx, maxz), tegelvak NA
ZONES = {
    "kamp": dict(naam="Knuffelkamp", lv="start · lv 1-3", oud=(11, 996), dx=-384, dz=0,
                 land=(-488, 886, -258, 1106), tegels=(-8, 13, -4, 18)),
    "z1": dict(naam="Zone 1 · Zwamzoet", lv="lv 1-20", oud=(0, 0), dx=0, dz=0,
               land=(-600, -615, 600, 615), tegels=(-11, -11, 11, 9)),
    "z2": dict(naam="Zone 2 · De Splinterpieken", lv="lv 20-30 · Kandijkom", oud=(0, -2465), dx=704, dz=1344,
               land=(434, -1436, 974, -806), tegels=(6, -23, 15, -13)),
    "z3": dict(naam="Zone 3 · De Schemerwortel", lv="lv 30-50 · Honingtak", oud=(2300, 0), dx=-896, dz=-576,
               land=(1166, -897, 1725, -338), tegels=(16, -16, 26, -3)),
    "z4": dict(naam="Zone 4 · De Lampionkust", lv="lv 50-56 · Lampionhaven", oud=None, dx=None, dz=None,
               land=(1740, -716, 2540, -266), tegels=(27, -12, 39, -5)),
    "z5": dict(naam="Zone 5 · Hoogvads", lv="lv 56-62 · de hoofdstad", oud=None, dx=None, dz=None,
               land=(2440, -660, 3150, -380), tegels=(38, -12, 49, -5)),
    "z6": dict(naam="Zone 6 · De Stormrand", lv="later · Windhoek", oud=None, dx=None, dz=None,
               land=(3264, -1280, 3904, -704), tegels=(51, -20, 60, -12)),
}

STAM = (1404, -576)                 # stam van de Schemerwortel (was 2300, 0)
EILAND_R = 238                      # wortel-eiland (boom2.EILAND_R 222 + rand-ruis 16)
KRUIN_R = 160
WERF = (STAM[0] + 251, STAM[1] - 251)   # Kaaszeppelinwerf (los eiland, werf2.C)
WERF_R = 70
Z3_OUD_VAK = (1900, -400, 2700, 400)    # revamp-vak voor de verschuiving

# Hoogvads: Moeder Wolkwal (kop west, staart oost), stad op de rug, y ~170-320
WALVIS = dict(kop=2440, staart=3080, z=-520, breed=250, y=(170, 320))

# ------------------------------------------------------------------------------------------------ plekken
KANDIJKOM = (690, -1058)
ZWAMZOET = (6, -9)
LAMPIONHAVEN = (1850, -470)
HOOGVADS_KATHEDRAAL = (2740, -520)

# Luchtschiphavens: id -> (naam, x, z, y-steiger, zone)
HAVENS = {
    "kamp": ("Kampsteiger", -235, 1008, 87, "kamp"),
    "z1_haven": ("Haven Zwamzoet", 92, 500, 65, "z1"),
    "z1_noord": ("Noordklifhaven", 150, -600, 120, "z1"),
    "z2_zuid": ("Kandijkom-dok", 704, -900, 91, "z2"),
    "z2_oost": ("Galmdok", 955, -835, 110, "z2"),
    "z3_west": ("Weststeiger", 1190, -590, 170, "z3"),
    "z3_oost": ("Oosttaksteiger", 1630, -500, 170, "z3"),
    "z4_haven": ("Lampionhaven (luchthaven)", 1800, -470, 70, "z4"),
    "z4_pier": ("De Opstijgpier", 2515, -565, 72, "z4"),
    "z5_rug": ("Walvisrughaven", 2630, -520, 262, "z5"),
    "z5_staart": ("Staarthaven", 3000, -540, 250, "z5"),
    "z6_windhoek": ("Windhoek", 3420, -960, 90, "z6"),
}

# Luchtschiplijnen: id, van, naar, bocht (zijdelingse uitbuiging in blokken van de boog), wat de vlucht laat zien
LIJNEN = [
    ("0", "kamp", "z1_haven", 90, "bestaat al; over de Zoete Zee"),
    ("A", "z1_noord", "z2_zuid", -110, "klimt langs de Splinterwand, rondje om de spitsen"),
    ("B", "z2_oost", "z3_west", 190, "duikt de Wolkenkloof in en klimt langs de wortels omhoog"),
    ("C", "z3_oost", "z4_haven", 230, "rondje om de kruin, dan omlaag door de Wolkenval"),
    ("D", "z4_pier", "z5_rug", 225, "spiraal omhoog langs de kop van Moeder Wolkwal"),
    ("E", "z5_staart", "z6_windhoek", -150, "later: door de Stormmuur (zone 6)"),
]

# Uitkijkpunten: id -> (naam, x, z, y, zone, [doelen])
UITKIJK = {
    "U1": ("Noordkliftop", 300, -560, 150, "z1", ["z2", "z3"]),
    "U2": ("Kandijkom-balkon", 690, -880, 100, "z2", ["z1"]),
    "U3": ("Galmkam", 940, -980, 150, "z2", ["z3"]),
    "U4": ("Westtak-uitkijk", 1200, -640, 175, "z3", ["z2"]),
    "U5": ("Het Nest", 1404, -576, 292, "z3", ["z4", "z5"]),
    "U6": ("Oosttak-uitkijk", 1645, -470, 180, "z3", ["z4", "z5"]),
    "U7": ("Lampionduin", 1860, -520, 90, "z4", ["z3"]),
    "U8": ("Vuurtoren", 2400, -500, 110, "z4", ["z5"]),
    "U9": ("Walvisstaart", 3060, -520, 260, "z5", ["z4", "z6"]),
}

# Zone 4: de eilandjes (naam, x, z, rx, rz, soort)
EILANDEN = [      # een slinger van west (de Boom) naar oost (Hoogvads)
    ("Lampionhaven", 1850, -470, 70, 82, "hub"),
    ("Knuffeltij-strand", 1935, -345, 62, 36, "strand"),
    ("Strandfeesteiland", 1995, -605, 52, 36, "strand"),
    ("Bloemeneiland", 2085, -405, 48, 40, "bloem"),
    ("Koraalbaai", 2185, -565, 62, 60, "koraal"),
    ("Vliegereiland", 2300, -665, 44, 30, "vlieger"),
    ("Vuurtoreneiland", 2400, -500, 30, 26, "vuurtoren"),
    ("Pierkop", 2470, -565, 24, 20, "pier"),
    ("Mysterie-eiland", 2480, -330, 34, 28, "mysterie"),
]
SLINGER = [(1850, -470), (1935, -345), (2085, -405), (1995, -605), (2185, -565), (2300, -665), (2400, -500), (2290, -400),
           (2480, -330), (2470, -565)]          # aanbevolen vaarroute (de hoofdlijn)
SCHILDPAD_LUS = (2290, -420, 62, 44)    # middelpunt + halve assen van de zwemronde van het Schildpadeiland
WRAK = (2165, -700)                     # Wrak van de Vliegervanger (kleine kerker)

# Barrieres: id -> (naam, soort, overgang)
BARRIERES = {
    "splinterwand": ("De Splinterwand", "kliffen", "zone 1 -> 2"),
    "wolkenkloof": ("De Wolkenkloof", "wolkenkloof", "zone 2 -> 3"),
    "wolkenval": ("De Wolkenval + Tijstroom", "stroming", "zone 3 -> 4"),
    "kringstroom": ("De Kringstroom", "stroming", "rand zone 4"),
    "mistbank": ("De Grauwe Mistbank", "mist", "zone 4 zuidoost (naar het keizerrijk)"),
    "hoogte": ("De Hemelkloof (hoogte)", "wolkenkloof", "zone 4 -> 5"),
    "stormmuur": ("De Stormmuur", "mist", "zone 5 -> 6"),
    "zoetezee": ("Zoete-Zeestroom", "stroming", "rond zone 1 + Knuffelkamp"),
}


def boog(p0, p1, bocht, n=64):
    """Kwadratische boog van p0 naar p1, het midden zijdelings verschoven met `bocht` blokken."""
    (x0, z0), (x1, z1) = p0, p1
    mx, mz = (x0 + x1) / 2, (z0 + z1) / 2
    dx, dz = x1 - x0, z1 - z0
    L = math.hypot(dx, dz) or 1
    cx, cz = mx - dz / L * bocht * 2, mz + dx / L * bocht * 2
    pts = []
    for i in range(n + 1):
        t = i / n
        pts.append(((1 - t) ** 2 * x0 + 2 * (1 - t) * t * cx + t * t * x1,
                    (1 - t) ** 2 * z0 + 2 * (1 - t) * t * cz + t * t * z1))
    return pts


def lijn_lengte(lijn):
    _, a, b, bocht, _ = lijn
    ha, hb = HAVENS[a], HAVENS[b]
    pts = boog((ha[1], ha[2]), (hb[1], hb[2]), bocht)
    hor = sum(math.dist(pts[i], pts[i + 1]) for i in range(len(pts) - 1))
    vert = abs(ha[3] - hb[3])
    # opstijgen/dalen gebeurt onderweg: schuine lengte, plus 2 x 8 s aanloop/afremmen aan de steigers
    return math.hypot(hor, vert), math.dist((ha[1], ha[2]), (hb[1], hb[2]))
