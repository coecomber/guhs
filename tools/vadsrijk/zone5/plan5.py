"""Zone 5 "Hoogvads" (the capital on the back of Moeder Wolkwal): placement, areas and EVERY POI of the map (T-Z5WERELD).

The kit (kit/: the approved round-2 render generators of renders-hoogvads/_bron, adapted where marked [zone 5 kaart]) builds
in LOCAL coordinates: head = +x, axis z = 0, y = render y. The game placement turns it 180 degrees (head WEST, over the
Opstijgpier of zone 4) and lowers it DY blocks:
    gx = GX0 - x,   gy = y + DY,   gz = GZ0 - z          (no mirroring; block states are turned with kit/export.draai180)
DY = -20 (night round T, decision 2): the belly hangs at y ~134 (31 above the Opstijgpier's mast top y 103, zone 4 is below
y ~110 everywhere under the whale), so the cathedral could grow: the Grote Poort is ~58 high now, the spires reach y ~316.
"""
GX0, GZ0, DY = 2702, -520, -20
T, HOOG = 64, 320
# the tiles that can hold zone-5 blocks (tx 38..47, tz -12..-5; tx 38..39 are shared with zone 4: below NAAD_Y zone 4 wins)
TX0, TX1, TZ0, TZ1 = 38, 48, -12, -5
NAAD_Y = 118          # game y: zone-4 blocks below, zone-5 blocks from here up (the whale's lowest point: waterfall mist ~y 104 is clipped)
ZONE_MIN_Y = 118      # areas/biomes of zone 5 start here (zone 4 below)
LEVELS = (56, 60)


def spel(x, y, z):
    """Local (kit) -> game block position."""
    return int(GX0 - x), int(y + DY), int(GZ0 - z)


def lokaal(gx, gy, gz):
    return GX0 - gx, gy - DY, GZ0 - gz


def spel_doos(x1, z1, x2, z2):
    """Local box -> game box (min/max swapped by the 180 degree turn)."""
    a, b = GX0 - x2, GX0 - x1
    c, d = GZ0 - z2, GZ0 - z1
    return min(a, b), min(c, d), max(a, b), max(c, d)


# ------------------------------------------------------------------------------------------------ the districts (LOCAL boxes)
# id -> (x1, z1, x2, z2, biome id, NL name, EN name). First match wins; z5_hoogvads (the whole walled city) is last;
# z5_moeder_wolkwal = the whale outside the walls (head, fins, tail, belly).
WIJKEN = [
    ("z5_paleisheuvel", (-156, -50, -76, 50), "hoogvads_paleis", "Hoogvads - de Paleisheuvel", "Hoogvads - Palace Hill"),
    ("z5_universiteit", (-78, -86, 8, -26), "hoogvads_universiteit", "Hoogvads - de Universiteit", "Hoogvads - the University"),
    ("z5_academie", (-78, 26, 8, 86), "hoogvads_academie", "Hoogvads - de Academie van de Wolkwacht", "Hoogvads - Academy of the Cloud Guard"),
    ("z5_marktlaan", (-76, -25, -6, 25), "hoogvads_markt", "Hoogvads - de Marktlaan", "Hoogvads - Market Lane"),
    ("z5_kathedraal", (-6, -52, 130, 52), "hoogvads_kathedraal", "Hoogvads - de Ordekathedraal", "Hoogvads - the Order Cathedral"),
    ("z5_luchthaven", (20, -175, 124, -53), "hoogvads_luchthaven", "Hoogvads - de Luchthaven", "Hoogvads - the Airport"),
    ("z5_staartpark", (-202, -64, -153, 64), "hoogvads_staartpark", "Hoogvads - het Staartpark", "Hoogvads - Tail Park"),
    ("z5_ooggaarde", (126, 26, 176, 100), "hoogvads_ooggaarde", "Hoogvads - de Ooggaarde", "Hoogvads - the Eye Garden"),
    ("z5_hoogvads", (-205, -105, 176, 105), "hoogvads", "Hoogvads", "Hoogvads"),
    ("z5_moeder_wolkwal", (-320, -182, 270, 182), "moeder_wolkwal", "Moeder Wolkwal", "Mother Cloudwhale"),
]


def gebieden():
    """Game areas: id, x1, z1, x2, z2 (game), biome, names; y from ZONE_MIN_Y up."""
    out = []
    for (gid, (x1, z1, x2, z2), biome, nl, en) in WIJKEN:
        a, b, c, d = spel_doos(x1, z1, x2, z2)
        out.append(dict(id=gid, x1=a, z1=b, x2=c, z2=d, biome=biome, nl=nl, en=en))
    return out


# ------------------------------------------------------------------------------------------------ POIs (LOCAL stand spots)
# Every spot is a STAND spot (the air block a player/NPC stands in). maak5 snaps each onto the real blocks (nearest standable
# spot within a few blocks) after the build; the published coordinates are the snapped ones.
# The ids of t-z5verhaal (Z5VerhaalPlekken) and t-z5wezens (Z5WezensPlekken) are all here.

def plekken():
    import kathedraal as KA
    P = {}
    # ---- arrival / airport (Wh = shift (-56, 30, 0): station hall x 40..94, z -78..-58, floor 236; piers x 52/72/92 to z -165)
    hy = 237
    P["luchthaven_station"] = (67, hy, -68)
    P["luchthaven_havenmeester"] = (79, hy, -74)           # behind the counter
    P["luchthaven_balie"] = (79, hy, -71)                  # in front of the counter
    P["luchthaven_logboek"] = (84, hy, -72)
    P["luchthaven_aankomst"] = (72, hy, -100)              # on pier 2, where the line-D ship lets you off
    P["luchthaven_pier_west"] = (52, hy, -120)             # pier 1 (game east... the names follow the kit: west = -x local)
    P["luchthaven_pier_oost"] = (92, hy, -120)
    P["luchthaven_klokkentoren"] = (67, hy, -63)
    for i, px in enumerate((52, 72, 92), 1):
        P[f"haven_5_pier_{i}"] = (px, hy, -155)
    # line D (zone 4 <-> 5): pier 2 is free (no anchored ship), the moving airship docks off its end
    P["haven_z5_z4_vertrek"] = (72, hy, -160)
    P["haven_5_4_landing"] = (72, hy, -158)
    P["haven_5_4_bel"] = (70, hy, -161)
    P["haven_5_4_vertrekbord"] = (74, hy, -152)
    P["haven_5_4_kapitein"] = (70, hy, -150)
    P["haven_5_4_luchtschip"] = (72, hy - 1, -170)         # middle of the docked ship (deck height)
    P["haven_5_4_mast"] = (69, hy + 10, -166)
    # pier 3: future line E (zone 5 -> 6 Stormrand), board only
    P["haven_5_6_vertrekbord"] = (94, hy, -152)
    P["kampvuur_luchthaven"] = (34, hy, -66)
    P["z5w_schuilnest_luik"] = (91, hy, -76)               # loose plank behind the crates (station corner)
    P["z5w_schuilnest_uit"] = (89, hy, -75)
    # ---- cathedral (KA: X0 west front, Y0 floor, nave z -12..12, crossing X_KRUIS, apse X_KOOR)
    Y0 = KA.Y0
    P["kathedraal_plein"] = (6, 241, 0)
    P["kathedraal_poort"] = (KA.X0 - 3, Y0 + 1, 0)
    P["kathedraal_schip"] = (KA.X0 + 20, Y0 + 1, 0)
    xk = (KA.X_KRUIS[0] + KA.X_KRUIS[1]) // 2
    P["kathedraal_altaar"] = (xk - 5, Y0 + 4, 0)
    P["kathedraal_apsis"] = (KA.X_KOOR - 4, Y0 + 1, 0)
    P["kathedraal_roos_noord"] = (xk, Y0 + 1, -(KA.TRANSEPT_Z - 4))
    P["kathedraal_roos_zuid"] = (xk, Y0 + 1, KA.TRANSEPT_Z - 4)
    P["kathedraal_geveltoren"] = (KA.X0 + 6, Y0 + KA.GALM_Y, 20)      # the belfry floor of the south front tower (spiral stair)
    P["kathedraal_geveltoren_voet"] = (KA.X0 + 6, Y0 + 1, 20)
    P["kampvuur_kathedraal"] = (8, 241, 14)
    # ---- market + soup kitchen (markt2: x -72..-8, y 238; fountain x -46; soup kitchen x -34..-10, z 6..24)
    my = 239
    P["zachtmoedlaan"] = (-2, 241, 0)
    P["markt_fontein"] = (-46, my, 9)
    P["markt_bakker"] = (-62, my, -12)
    P["markt_kleermaker"] = (-54, my, 12)
    P["markt_smid"] = (-38, my, -12)
    P["markt_juwelier"] = (-66, my, 12)
    P["kampvuur_markt"] = (-58, my, -4)
    P["soepkeuken_ketel"] = (-22, my, 12)
    P["soepkeuken_tafel"] = (-27, my, 9)
    P["soepkeuken_hoek"] = (-14, my, 20)
    P["soepkeuken_bank"] = (-29, my, 11)
    P["soepkeuken_ingang"] = (-22, my, 4)
    P["z5w_buik_luik"] = (-31, my, 21)
    P["z5w_buik_uit"] = (-29, my, 19)
    # ---- palace hill (Wp = shift (-18, 30, 0): plateau 252; Raadszaal cella x -112..-90, floor 256; palace x -152..-122)
    P["paleis_trap"] = (-60, 241, 0)                      # foot of the monumental stair (market side)
    P["paleis_hof"] = (-100, 253, 26)
    P["raadszaal_ingang"] = (-86, 256, 0)
    P["raadszaal_lessenaar"] = (-100, 257, 0)
    P["raadszaal_zaal"] = (-95, 257, 8)
    P["raadszaal_zijkamer"] = (-109, 257, -11)
    P["paleis_poort"] = (-120, 253, 0)
    P["troonzaal_ingang"] = (-129, 253, 0)
    P["troonzaal_troon"] = (-145, 258, 0)
    P["troonzaal_audientie"] = (-138, 253, 0)
    P["paleis_kanselarij"] = (-137, 253, 30)
    P["paleis_kanselarij_bureau"] = (-141, 253, 34)
    P["kampvuur_paleis"] = (-96, 253, -26)
    # ---- university (Wu = shift (-30, 30, 0): x -76..6, z -80..-30, floor 234; courtyard x -63..-7, z -67..-43)
    uy = 235
    P["universiteit_poort"] = (-35, uy, -26)
    P["universiteit_binnenhof"] = (-35, uy, -49)
    P["universiteit_bibliotheek"] = (-49, uy, -74)
    P["universiteit_archief"] = (-70, uy, -60)
    P["universiteit_collegezaal"] = (-70, uy, -40)
    P["universiteit_sterrenwacht"] = (-28, uy, -76)       # foot of the observatory stair
    P["universiteit_sterrenwacht_top"] = (-28, uy + 47, -76)
    # the guild house (Gildehuis) in the east wing of the university (x -6..6), workshops along the wing
    P["gilde_hal"] = (0, uy, -36)
    P["gilde_keuken"] = (0, uy, -44)
    P["gilde_brouwerij"] = (0, uy, -52)
    P["gilde_smidse"] = (0, uy, -60)
    P["gilde_atelier"] = (0, uy, -68)
    P["gilde_bijenstal"] = (0, uy, -76)
    # ---- academy of the Cloud Guard (Wk = shift (-30, 30, 0): walls x -76..6, z 30..80; keep x -49..-21, z 66..78)
    ay = 235
    P["academie_poort"] = (-35, ay, 26)
    P["academie_oefenplein"] = (-50, ay, 48)
    P["academie_kapitein"] = (-35, ay, 72)
    P["academie_kazerne"] = (-44, ay, 70)
    P["academie_kastjes"] = (-26, ay, 75)
    P["academie_toren"] = (-21, ay + 32, 66)
    P["academie_toren_voet"] = (-21, ay, 66)
    P["z5w_proefarena"] = (-12, ay, 50)
    P["z5w_leider_strobaal"] = (-12, ay, 58)
    P["kampvuur_academie"] = (-60, ay, 36)
    # ---- parks
    P["staartpark_vijver"] = (-174, None, 4)
    P["staartpark_prieel"] = (-178, None, -18)
    P["kampvuur_staartpark"] = (-166, None, -10)
    P["staartpark_westeinde"] = (-194, None, 0)
    P["ooggaarde_tuin"] = (150, None, 60)
    P["kampvuur_ooggaarde"] = (146, None, 52)
    P["spuitgat_midden"] = (150, None, 0)
    P["spuitgat_noord"] = (140, None, -25)
    P["spuitgat_zuid"] = (140, None, 25)
    # ---- t-z5wezens: the whale outside the walls (fins, tail fluke: cloud platforms built by bouw5.wolkplatformen)
    for k, kant in (("zuidvin", 1), ("noordvin", -1)):
        P[f"z5w_{k}"] = (92, None, kant * 128)
        P[f"z5w_lift_{k}_boven"] = (92, None, kant * 86)
        P[f"z5w_lift_{k}_onder"] = (100, None, kant * 108)
    P["z5w_lift_staart_boven"] = (-196, None, 22)
    P["z5w_lift_staart_onder"] = (-240, None, 30)
    P["z5w_staartvlakte_noord"] = (-266, None, -62)
    P["z5w_leider_mistmoeder"] = (-268, None, -70)
    P["z5w_donderbuik_arena"] = (-266, None, 62)
    P["z5w_donderbuik_deur"] = (-248, None, 50)
    P.update(VERHAAL)
    # ---- the inn, the bank and the shops: houses of the map get these roles (bouw5.ROLLEN); filled in by bouw5
    return P


# t-z5verhaal's numbered spots (their placeholders, GAME x/z + render y = local y): snapped onto the real blocks
_V = {
    "soepkeuken_rij": [(2718, 239, -527), (2720, 239, -527), (2722, 239, -527), (2724, 239, -527)],
    "burger": [(2700, 241, -510), (2760, 239, -512), (2690, 241, -530), (2860, 236, -514), (2560, 240, -570), (2640, 237, -452)],
    "wolkwacht": [(2742, 253, -504), (2742, 253, -536), (2683, 245, -512), (2645, 237, -452), (2868, 236, -506), (2557, 240, -568)],
    "academie_parcours": [(2722, 235, -565), (2752, 235, -565), (2752, 235, -590), (2722, 235, -590)],
    "staartpark_boom": [(2870, 236, -540), (2890, 236, -535), (2890, 236, -505), (2870, 236, -500)],
    "geheim_knuffel": [(2690, 290, -505), (2905, 240, -540), (2545, 241, -600), (2665, 236, -418), (2775, 262, -438), (2760, 251, -548),
                       (2835, 260, -520), (2655, 248, -540)],
    "geheim_schat": [(2912, 236, -520), (2838, 253, -530), (2540, 241, -560)],
    "wolkbes": [(2860, 236, -545), (2885, 236, -548), (2900, 236, -520), (2884, 236, -492), (2862, 236, -495), (2540, 240, -575),
                (2548, 240, -590), (2565, 240, -585)],
    "wolbol": [(2745, 239, -528), (2818, 253, -512), (2735, 235, -472)],
}
# (their render y is ignored: in the city the spot goes on the city ground, outside onto the whale's top; then snapped)
VERHAAL = {f"{k}_{i + 1}": (GX0 - gx, None, GZ0 - gz) for k, lijst in _V.items() for i, (gx, gy, gz) in enumerate(lijst)}
VERHAAL["staartvin_top"] = (-203, None, 0)
VERHAAL["geheim_knuffel_5"] = (-60, 235, -72)     # (placeholder lay on the university roof: now in the library)
VERHAAL["geheim_knuffel_6"] = (-70, 235, 72)      # (placeholder lay in the academy wall: now in a corner of the yard)
# the same spot under a second id (the story's name -> the map's)
ALIAS = {"bank_balie": "bank_hoogvads_balie", "luchthaven_vertrekbord": "haven_5_4_vertrekbord"}


# cloud platforms (bouw5.wolkplatformen): id -> (x, z, r) local; the top is the whale's skin height at the centre
PLATFORMS = {"z5w_zuidvin": (92, 128, 9), "z5w_noordvin": (92, -128, 9), "z5w_staartvlakte_noord": (-266, -62, 13),
             "z5w_donderbuik_arena": (-266, 62, 20)}


# t-z5wezens' rooms inside the whale (their stand-ins, built by THEM into air): published as box corners, local y follow DY
WEZENS_KAMERS = {
    # id: (x1, y1, z1, x2, y2, z2) in GAME coordinates of their placeholder (render y): shifted by DY on publish
    "z5w_buikgangen": (2704, 184, -530, 2808, 196, -508),
    "z5w_schuilnest": (2588, 206, -440, 2624, 214, -414),
}
