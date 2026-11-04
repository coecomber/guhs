"""The fixed plan of zone 2 "De Splinterpieken" (Guhs 1.10, Het Vadsrijk): every coordinate the other slices rely on.

World coordinates (x east, z south = +z). The zone box (Z2_BRIEF): x -600..600, z -3000..-1800, north of zone 1. The map tiles cover
a sea/cloud margin around it. The generator (kaart2.py / bouw2.py) shapes the terrain so the heights below hold (pads are
flattened to exactly these heights), and writes POI_Z2.md + Z2WereldPlekken.java from the real result.

All y values are STAND positions (the air block a player/NPC stands in) unless named *_top (the block itself).
"""

# ------------------------------------------------------------------------------------------------ grid
TX0, TX1 = -11, 10            # tile range x (x -704..703)
TZ0, TZ1 = -48, -28           # tile range z (z -3072..-1729)
T = 64
X0, Z0 = TX0 * T, TZ0 * T
NX, NZ = (TX1 - TX0 + 1) * T, (TZ1 - TZ0 + 1) * T
ZEE = 62                      # sea surface (the cloud sea around the massif)
ZONE = (-600, -3000, 600, -1800)

# ------------------------------------------------------------------------------------------------ areas (kern.Gebieden boxes)
# (id, minX, minZ, maxX, maxZ, minLevel, maxLevel, stad). First match wins (small ones first).
GEBIEDEN = [
    ("z2_spiegelburcht", -70, -2995, 70, -2850, 33, 35, False),
    ("z2_kandijkom_dok", -40, -2249, 40, -2200, 20, 35, True),
    ("z2_kandijkom", -150, -2550, 150, -2250, 20, 35, True),
    ("z2_galmhellingen", 151, -2700, 600, -1800, 20, 25, False),
    ("z2_knuffelmeer", -600, -2700, -151, -1800, 25, 30, False),
    ("z2_splinterkloof", -600, -3000, 600, -2701, 30, 34, False),
    ("z2_ring", -600, -3000, 600, -1800, 20, 35, False),
]

# ------------------------------------------------------------------------------------------------ Kandijkom (the geode town)
STAD = (0, -2400)
STAD_TOP = 90                 # town floor block; stand y 91
STAD_Y = STAD_TOP + 1
GEODE_R = 124                 # inner face of the geode wall (floor radius ~118)
GEODE_DIK = 18                # wall thickness at the foot
GEODE_HOOG = 58               # rim height above the floor at its highest (north)
POORTEN = {"zuid": (0, 1), "oost": (1, 0), "west": (-1, 0), "noord": (0, -1)}   # gates in the geode wall
PLEIN_R = 26
VIJVER_R = 11                 # the skating pond (packed ice, flush with the plaza)

# buildings: id -> (centre x, centre z, width x, depth z, door side, kind, name)
GEBOUWEN = {
    "kristalkapel": (0, -2470, 23, 17, "zuid", "tempel", "De Kristalkapel (ordetempel-bijkantoor)"),
    "juwelier": (-50, -2440, 15, 11, "oost", "winkel", "De Glinsterkist (juweliersguh)"),
    "smid": (50, -2440, 15, 11, "west", "smid", "Het Warme Aambeeld (smid)"),
    "winkel_wantjes": (-64, -2392, 13, 11, "oost", "winkel", "Warme Wantjes (kleding)"),
    "winkel_kandij": (64, -2392, 13, 11, "west", "winkel", "De Kandijkraam (snoep + drankjes)"),
    "bank": (-50, -2344, 15, 11, "oost", "winkel", "De Spaarsok (bank + post)"),
    "cafe": (52, -2344, 19, 13, "west", "cafe", "Het Dampende Kopje (chocomelkcafe)"),
    "huis_1": (-92, -2440, 11, 9, "oost", "huis", "huisje"),
    "huis_2": (92, -2436, 11, 9, "west", "huis", "huisje"),
    "huis_3": (-98, -2390, 11, 9, "oost", "huis", "huisje"),
    "huis_4": (100, -2390, 11, 9, "west", "huis", "huisje"),
    "huis_5": (-30, -2300, 11, 9, "oost", "huis", "huisje"),
    "huis_6": (30, -2300, 11, 9, "west", "huis", "huisje"),
    "huis_7": (-80, -2296, 11, 9, "noord", "huis", "huisje"),
    "huis_8": (-52, -2496, 11, 9, "zuid", "huis", "huisje"),
}
STAL = (-96, -2336)            # open stable with a fenced paddock
QUESTBORD = (14, -2424)
GONDEL_DAL = (78, -2468)       # valley station (platform centre)
GONDEL_KABEL = (12, 10)        # cable height above the valley / top station floor
GONDEL_BERG = (306, -2618)     # top station on the Galmtop shoulder
GONDEL_BERG_TOP = 150
SNEEUWGUHSPAR = (0, -2436)     # the big snow spruce north of the plaza

# airship dock (south, over the cliff)
DOK = (0, -2236)
DOK_TOP = 90

# ------------------------------------------------------------------------------------------------ De Galmhellingen (east)
GALM_ARENA = (430, -2280, 32)  # Galmwolf: centre x, z, radius; floor top 120
GALM_ARENA_TOP = 120
GALM_PILAREN = 8               # frozen pillars on a ring r 19
GALMTOP = (520, -2585)         # viewpoint needle
LAWINES = [[(500, -2640), (430, -2520), (360, -2430)],
           [(590, -2380), (540, -2240), (500, -2120)],
           [(560, -2050), (470, -1960), (400, -1880)]]
NAALD_VELDEN = [(330, -2200, 70), (500, -2000, 70), (260, -1980, 50), (420, -2480, 60)]
GALMGROT = (566, -1990)        # secret: a crystal grotto behind a frozen waterfall

# ------------------------------------------------------------------------------------------------ Het Bevroren Knuffelmeer (west)
MEER = (-380, -2230, 140, 170)  # centre x, z, radius x, radius z
MEER_IJS = 84                  # top of the ice (stand 85)
MEER_EILAND = (-420, -2265, 15)
SCHOTSEN = (-330, -2078, 24)   # open water with ice floes (south outflow)
KNUFFELS_ONDER_IJS = [(-300, -2160), (-350, -2300), (-430, -2180), (-470, -2320), (-380, -2380), (-300, -2340),
                      (-440, -2090), (-360, -2110), (-500, -2220), (-270, -2250)]
MEERKIJK = (-540, -2060)       # viewpoint needle
DRAKENZADEL = (-330, -2610, 30)  # the dragon encounter: a wide flat snowy saddle (top 125)
DRAKENZADEL_TOP = 125
IJSGROT = (-575, -2400)        # secret: a little ice cave on the west coast

# ------------------------------------------------------------------------------------------------ De Splinterkloof (north)
KLOOF = [(-720, -2772), (-560, -2768), (-430, -2758), (-300, -2790), (-160, -2772), (0, -2785), (150, -2774), (300, -2796),
         (450, -2770), (560, -2778), (720, -2780)]
KLOOF_BODEM = 64               # ravine floor top
KLOOF_BREED = 40
KLOOF_RAND_ZUID = 138          # rim heights (south / north)
KLOOF_RAND_NOORD = 146
BRUGGEN = [-430, -180, 0, 220, 460]   # hanging bridges (x); the x 0 one leads to the Spiegelburcht
BRUG_TOP = 150                 # deck top of the main bridge (x 0)
KLOOF_TRAPPEN = [-400, -110, 160, 420]  # stairways from the floor up to the south rim
BOORTORENS = [(-330, -2738), (130, -2822), (400, -2744)]   # Mika drilling rigs
BOORMEESTER = (-500, -2836, 20)  # secret drill site: a cave in the north wall (centre, radius); floor top 65
BOORMEESTER_INGANG = (-500, -2795)
KLOOFKIJK = (-210, -2930)      # viewpoint needle
MIKA_FORT = (300, -2885)       # the Mika fort on the north plateau (defeated Mika's flee here in a smoke puff)

# ------------------------------------------------------------------------------------------------ De Spiegelburcht (far north peak)
BURCHT = (0, -2915)            # peak centre
BURCHT_TOP = 175               # plateau / floor top; stand 176
BURCHT_DEUR_Z = -2856          # the moving entrance door plane (x -3..3, y 176..184)
# rooms (x0, z0, x1, z1 inclusive inner boxes, height)
BURCHT_KAMERS = {
    "voorhal": (-16, -2879, 16, -2857, 16),
    "lichtkamer": (-14, -2899, 14, -2881, 12),
    "spiegelgang": (-9, -2917, 9, -2901, 12),
    "prismazaal": (-14, -2937, 14, -2919, 12),
}
SPIEGELZAAL = (0, -2963, 22)   # boss hall: centre x, z, radius (dome)

# ------------------------------------------------------------------------------------------------ campfires (x, z) per area
KAMPVUREN = {
    "kandijkom_dok": [(-9, -2262)],
    "galmhellingen": [(232, -2330), (330, -2120), (480, -2440), (250, -2560)],
    "knuffelmeer": [(-200, -2120), (-560, -2240), (-250, -2440), (-470, -2500)],
    "splinterkloof": [(-260, -2700), (300, -2702), (24, -2730), (-100, -2774), (-360, -2880)],
    "spiegelburcht": [(10, -2808)],
}
UITKIJK = {"galmtop": GALMTOP, "meerkijk": MEERKIJK, "kloofkijk": KLOOFKIJK}

# ------------------------------------------------------------------------------------------------ paths (polylines, x z)
PADEN = {
    "oost": [(118, -2400), (180, -2380), (232, -2330), (330, -2290), (398, -2282)],
    "oost_zuid": [(232, -2330), (300, -2200), (330, -2120), (400, -1990)],
    "oost_noord": [(232, -2330), (250, -2450), (250, -2560), (306, -2618)],
    "oost_noord2": [(330, -2290), (480, -2440), (520, -2560)],
    "west": [(-118, -2400), (-170, -2390), (-220, -2300), (-200, -2120), (-260, -2060)],
    "west_noord": [(-170, -2390), (-250, -2440), (-330, -2470), (-470, -2500), (-540, -2380), (-560, -2240), (-540, -2080)],
    "zadel": [(-250, -2440), (-290, -2540), (-330, -2610)],
    "noord": [(0, -2522), (10, -2600), (30, -2680), (24, -2730), (0, -2752)],
    "kloof_west": [(24, -2730), (-120, -2715), (-260, -2700), (-430, -2722)],
    "kloof_oost": [(24, -2730), (160, -2716), (300, -2702), (460, -2726)],
    "zadel_kloof": [(-330, -2610), (-300, -2660), (-260, -2700)],
}
