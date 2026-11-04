"""The fixed plan of zone 2 "De Splinterpieken" (Guhs 1.10, Het Vadsrijk): every coordinate the other slices rely on.

Fix round 2 (g-z2, 2026-11-04): the zone is rebuilt SMALL and COMPACT (~500x550): Kandijkom in the middle (same centre as
before, so the airship dock and arrival stay), the three areas tight around it (Galmhellingen E, Bevroren Knuffelmeer W,
Splinterkloof N), the Spiegelburcht NE on its peak, the dock S. The rest of the old tile grid is cloud sea (an island in the
clouds). All POI ids of v1 are kept (new positions).

World coordinates (x east, z south = +z). All y values are STAND positions (the air block a player/NPC stands in) unless
named *_top (the block itself).
"""

# ------------------------------------------------------------------------------------------------ grid
TX0, TX1 = -11, 10            # tile range x (x -704..703): unchanged, so no stale tiles stay behind (outside = cloud sea)
TZ0, TZ1 = -48, -28           # tile range z (z -3072..-1729)
T = 64
X0, Z0 = TX0 * T, TZ0 * T
NX, NZ = (TX1 - TX0 + 1) * T, (TZ1 - TZ0 + 1) * T
ZEE = 62                      # sea surface (the cloud sea around the massif)
ZONE = (-250, -2775, 250, -2200)

# ------------------------------------------------------------------------------------------------ areas (kern.Gebieden boxes)
# (id, minX, minZ, maxX, maxZ, minLevel, maxLevel, stad). First match wins (small ones first).
GEBIEDEN = [
    ("z2_spiegelburcht", 110, -2775, 190, -2618, 28, 30, False),
    ("z2_kandijkom_dok", -40, -2317, 40, -2200, 20, 30, True),
    ("z2_kandijkom", -84, -2484, 84, -2318, 20, 30, True),
    ("z2_splinterkloof", -260, -2775, 260, -2505, 26, 29, False),
    ("z2_galmhellingen", 85, -2504, 260, -2200, 20, 23, False),
    ("z2_knuffelmeer", -260, -2504, -85, -2200, 23, 26, False),
    ("z2_ring", -300, -2820, 300, -2150, 20, 30, False),
]

# ------------------------------------------------------------------------------------------------ Kandijkom (the geode town)
STAD = (0, -2400)
STAD_TOP = 90                 # town floor block; stand y 91
STAD_Y = STAD_TOP + 1
GEODE_R = 66                  # inner face of the geode wall
GEODE_DIK = 14                # wall thickness at the foot
GEODE_HOOG = 48               # rim height above the floor at its highest (north)
POORTEN = {"zuid": (0, 1), "oost": (1, 0), "west": (-1, 0), "noord": (0, -1)}   # gates in the geode wall
POORT_HALF = 4                # gate street half width (x -3..3 = 7 wide) + 1 margin
PLEIN_R = 26
TOREN = (0, -2406)            # the crystal clock tower in the middle of the plaza
VIJVER = (0, -2385)           # the skating pond (packed ice, flush with the plaza)
VIJVER_R = 7
KERSTBOOM = (-16, -2414)      # the big decorated snow spruce
# the tiers (terraces against the geode wall): (r inner, r outer, floor top)
TIER_A = (34, 50, 95)
TIER_B = (50, 66, 100)

# buildings: id -> (centre x, centre z, width x, depth z, door side, kind, name, floor top)
_A, _B = TIER_A[2], TIER_B[2]
GEBOUWEN = {
    # ground floor (y 90) on the four diagonals of the plaza
    "juwelier": (-23, -2424, 11, 11, "zuid", "winkel", "De Glinsterkist (juweliersguh)", STAD_TOP),
    "smid": (23, -2424, 11, 11, "zuid", "smid", "Het Warme Aambeeld (smid)", STAD_TOP),
    "cafe": (-24, -2377, 13, 11, "noord", "cafe", "Het Dampende Kopje (chocomelkcafe)", STAD_TOP),
    "bank": (23, -2377, 11, 11, "noord", "winkel", "De Spaarsok (bank + post)", STAD_TOP),
    # tier A (y 95)
    "winkel_kandij": (38, -2422, 11, 9, "west", "winkel", "De Kandijkraam (snoep + drankjes)", _A),
    "huis_1": (22, -2438, 11, 9, "zuid", "huis", "huisje", _A),
    "winkel_wantjes": (-38, -2422, 11, 9, "oost", "winkel", "Warme Wantjes (kleding)", _A),
    "huis_2": (-22, -2438, 11, 9, "zuid", "huis", "huisje", _A),
    "huis_3": (-38, -2378, 11, 9, "oost", "huis", "huisje", _A),
    "huis_4": (-22, -2362, 11, 9, "noord", "huis", "huisje", _A),
    "huis_5": (38, -2378, 11, 9, "west", "huis", "huisje", _A),
    "huis_6": (22, -2362, 11, 9, "noord", "huis", "huisje", _A),
    # tier B (y 100)
    "huis_7": (51, -2430, 11, 9, "west", "huis", "huisje", _B),
    "kristalkapel": (-30, -2451, 13, 11, "zuid", "tempel", "De Kristalkapel (ordetempel-bijkantoor)", _B),
    "huis_8": (-51, -2430, 11, 9, "oost", "huis", "huisje", _B),
    "huis_9": (-51, -2370, 11, 9, "oost", "huis", "huisje", _B),
    "huis_10": (-30, -2349, 11, 9, "noord", "huis", "huisje", _B),
    "huis_11": (51, -2370, 11, 9, "west", "huis", "huisje", _B),
    "huis_12": (30, -2349, 11, 9, "noord", "huis", "huisje", _B),
}
STAL = (26, -2296)             # the stable with a paddock, along the dock road outside the south gate
QUESTBORD = (12, -2420)
GONDEL_DAL = (30, -2451)       # valley station (platform centre) on tier B, NE
GONDEL_DAL_TOP = _B
GONDEL_KABEL = (12, 10)        # cable height above the valley / top station floor
GONDEL_BERG = (182, -2478)     # top station on the Galmtop shoulder (the cable crosses the Galmspleet)
GONDEL_BERG_TOP = 150
SNEEUWGUHSPAR = KERSTBOOM

# airship dock (south, over the cliff): unchanged
DOK = (0, -2236)
DOK_TOP = 90

# ------------------------------------------------------------------------------------------------ De Galmhellingen (east)
GALM_ARENA = (182, -2318, 32)  # Galmwolf: centre x, z, radius
GALM_ARENA_TOP = 116
GALM_PILAREN = 8               # frozen pillars on a ring r 19
GALMTOP = (226, -2452)         # viewpoint needle
GALMPIEK = (206, -2470)        # the peak (gondola top station on its shoulder)
GALMSPLEET = [(110, -2545), (112, -2500), (108, -2468), (110, -2446)]   # the gorge under the gondola (N -> S, floor rises south)
GALMSPLEET_BREED = 16
LAWINES = [[(226, -2478), (196, -2440), (160, -2428)],
           [(244, -2380), (232, -2300), (226, -2250)],
           [(240, -2262), (204, -2232), (166, -2218)]]
LAWINE_GOLF = (150, -2432)     # the frozen avalanche wave over the path (set piece)
NAALD_VELDEN = [(120, -2262, 22), (232, -2228, 16), (128, -2350, 10), (236, -2408, 14)]
GALMGROT = (236, -2346)        # secret: a crystal geode grotto behind a frozen waterfall
WARMWATERBRON = (196, -2252)   # the steaming hot spring (side quest)
KRISTALBOS_GALM = (150, -2228, 26)

# ------------------------------------------------------------------------------------------------ Het Bevroren Knuffelmeer (west)
MEER = (-172, -2378, 54, 70)   # centre x, z, radius x, radius z
MEER_IJS = 84                  # top of the ice (stand 85)
MEER_EILAND = (-186, -2396, 10)
SCHOTSEN = (-150, -2322, 14)   # open water with ice floes (south outflow)
KNUFFELS_ONDER_IJS = [(-150, -2350), (-165, -2420), (-200, -2360), (-210, -2420), (-140, -2400), (-195, -2335),
                      (-175, -2440), (-130, -2370), (-215, -2385), (-160, -2370)]
MEER_SCHEUR = (-170, -2430)    # the big crack in the ice (north part)
MEERKIJK = (-234, -2282)       # viewpoint needle
DRAKENZADEL = (-205, -2492, 20)  # the dragon encounter: a wide flat snowy saddle over the ravine
DRAKENZADEL_TOP = 112
IJSGROT = (-240, -2410)        # secret: a little glowing ice geode on the west coast
KRISTALBOS_MEER = (-110, -2262, 22)

# ------------------------------------------------------------------------------------------------ De Splinterkloof (north)
KLOOF = [(-330, -2548), (-220, -2541), (-120, -2552), (0, -2542), (90, -2549), (200, -2540), (330, -2547)]
KLOOF_BODEM = 64               # ravine floor top
KLOOF_BREED = 32
KLOOF_RAND_ZUID = 122          # rim heights (south / north)
KLOOF_RAND_NOORD = 132
KLOOF_X = 236                  # the ravine is closed beyond |x| > this (no pit at the coast)
# hanging bridges: (x) over the ravine (N-S); index 2 (x 150) is the wide one to the Spiegelburcht.
BRUGGEN = [-190, -100, 150, 40]
BRUG_TOP = 136                 # deck top of the main bridge (x 150)
SPLEETBRUG_Z = -2514           # brug_5: the hanging bridge E-W over the Galmspleet
# ledge ramps: (x start, direction +1 east / -1 west, wall +1 south / -1 north)
KLOOF_TRAPPEN = [(-236, 1, 1), (-40, 1, 1), None, (-206, 1, -1)]   # index 2 = the Galmspleet floor (rises south)
BOORTORENS = [(-150, -2522), (60, -2572), (204, -2522)]   # Mika drilling rigs
BOORMEESTER = (-30, -2614, 20)  # secret drill site: a cave in the north wall (centre, radius); floor top 65
BOORMEESTER_INGANG = (-30, -2560)
KLOOFKIJK = (-86, -2702)       # viewpoint needle
MIKA_FORT = (-170, -2652)      # the Mika fort on the north plateau
MIKA_FORT_TOP = 134

# ------------------------------------------------------------------------------------------------ De Spiegelburcht (NE peak)
# The castle is the v1 castle moved as one piece (bazen's puzzle rows/columns are relative to it): every v1 coordinate +
# (BURCHT_DX, 0, BURCHT_DZ). bazen/wezens/npc read the offset from arena_spiegelzucht.
BURCHT_DX, BURCHT_DZ = 150, 222
BURCHT = (0 + BURCHT_DX, -2915 + BURCHT_DZ)   # peak centre
BURCHT_TOP = 175               # plateau / floor top; stand 176
BURCHT_DEUR_Z = -2856 + BURCHT_DZ
BURCHT_KAMERS = {
    "voorhal": (-16, -2879, 16, -2857, 16),
    "lichtkamer": (-14, -2899, 14, -2881, 12),
    "spiegelgang": (-9, -2917, 9, -2901, 12),
    "prismazaal": (-14, -2937, 14, -2919, 12),
}   # v1 coordinates (relative x, v1 z); bouw2 adds the offset
SPIEGELZAAL = (0, -2963, 22)   # v1 coordinates

# ------------------------------------------------------------------------------------------------ campfires (x, z) per area
KAMPVUREN = {
    "kandijkom_dok": [(-9, -2262)],
    "galmhellingen": [(110, -2390), (142, -2262), (198, -2418), (232, -2296)],
    "knuffelmeer": [(-100, -2330), (-236, -2342), (-128, -2458), (-176, -2292)],
    "splinterkloof": [(-196, -2512), (128, -2510), (0, -2506), (-96, -2546), (-136, -2604)],
    "spiegelburcht": [(162, -2590)],
}
UITKIJK = {"galmtop": GALMTOP, "meerkijk": MEERKIJK, "kloofkijk": KLOOFKIJK}
UITKIJK_HOOG = {"galmtop": 30, "meerkijk": 34, "kloofkijk": 30}

# little guh hamlets / warm camps along the way: (id, x, z, n houses)
GEHUCHTEN = [("galmdorpje", 128, -2414, 3), ("meerkamp", -112, -2306, 3), ("kloofkamp", -40, -2510, 2), ("plateaukamp", 60, -2600, 2)]
# crystal forests (x, z, r, colour mix)
KRISTALBOSSEN = [(150, -2228, 26, "roze"), (-110, -2262, 22, "blauw"), (-60, -2650, 26, "paars"), (232, -2380, 14, "paars"),
                 (-236, -2450, 14, "blauw"), (80, -2640, 20, "roze"), (-200, -2640, 24, "roze"), (200, -2420, 14, "blauw"),
                 (-150, -2700, 18, "blauw"), (60, -2300, 16, "paars"), (-60, -2300, 16, "roze"), (20, -2700, 22, "paars")]
# ice waterfalls (frozen cascades) down cliffs: (x, z, facing) on the ravine walls and the Galm cliffs
IJSVALLEN = [(-160, -2525, 1), (70, -2526, 1), (-60, -2560, -1), (180, -2558, -1), (114, -2470, 0)]

# ------------------------------------------------------------------------------------------------ paths (polylines, x z)
PADEN = {
    "oost": [(60, -2400), (86, -2399), (110, -2398), (128, -2370), (144, -2320)],
    "oost_zuid": [(128, -2370), (132, -2300), (142, -2262), (190, -2246)],
    "oost_noord": [(110, -2398), (150, -2418), (198, -2418), (186, -2466)],
    "west": [(-60, -2400), (-86, -2398), (-96, -2370), (-100, -2330), (-150, -2300), (-176, -2292)],
    "west_zuid": [(-176, -2292), (-222, -2306), (-236, -2342)],
    "west_noord": [(-86, -2398), (-102, -2436), (-128, -2458)],
    "zadel": [(-128, -2458), (-166, -2474), (-205, -2490)],
    "noord": [(0, -2460), (0, -2484), (0, -2506)],
    "kloof_west": [(0, -2506), (-40, -2512), (-96, -2512), (-150, -2514), (-190, -2516)],
    "kloof_oost": [(0, -2506), (50, -2512), (96, -2514)],
    "kloof_oost2": [(124, -2514), (150, -2518)],
    "zadel_kloof": [(-205, -2490), (-196, -2512)],
    "plateau": [(-190, -2574), (-160, -2600), (-136, -2604), (-110, -2576), (-60, -2600), (-30, -2582), (40, -2578), (100, -2582),
                (138, -2578)],
    "plateau_fort": [(-160, -2600), (-170, -2630)],
    "plateau_kijk": [(-60, -2600), (-72, -2650), (-78, -2684)],
}
