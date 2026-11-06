"""Zone 6 "Het Zijdeland": placement, areas and ALL planned spots (U-Z6WERELD).

The kit (zone6/kit = the approved concept generators of renders-zijdeland/_bron) works in REAL game coordinates (x east,
z south): no rotation, no shift. Tiles are plain slices of the local world.

Spots: PLEKKEN below is the PLAN (x, y or None = ground, z). The builders (rozenkloof/saffraanpoort/landschap/bouw6) record
the exact spots they build with zijbouw.plek(W, id, ...) and those win; poi6 snaps everything onto real stand blocks.
Every id the other zone-6 slices asked for (u-z6wezens, u-z6verhaal, u-z6raid) is in this list.
"""
VAK = (3280, -1060, 4180, -460)          # the zone box (x1, z1, x2, z2): never visible
TX0, TX1, TZ0, TZ1 = 51, 66, -18, -7      # tiles (x 3264..4287, z -1152..-385): the zone box + a coast margin (the land runs
                                          # into the shared open sea at the north/south/east tile edges; zone 7 replaces the east later)
T, HOOG = 64, 320
Y_MIN, Y_MAX = 0, 260                     # the local window (the raid tower's lens tops out at ~236)
LEVELS = (60, 70)
VENSTER = (3264, Y_MIN, -1152, 4288, Y_MAX, -384)   # x1, y1, z1, x2, y2, z2 of the one local world (= the tiles)
RAND = (3264, -1140, 4276, -396)          # the outer coast: the land sinks into the sea towards these lines (zijwereld.velden)

# areas: id, x1, z1, x2, z2, biome, NL name, levels, town (safe)? -- FIRST MATCH WINS (cities, the tower first; the zone last)
GEBIEDEN = [
    ("z6_rozenkloof", 3236, -800, 3500, -652, "zijdeland_rozenkloof", "Rozenkloof", (60, 60), True),
    ("z6_saffraanpoort", 3634, -950, 3890, -742, "zijdeland_saffraanpoort", "Saffraanpoort", (62, 62), True),
    ("z6_toren", 4100, -730, 4180, -646, "zijdeland_toren", "De Sterrendovende Toren", (70, 70), False),
    ("z6_moederoase", 4000, -900, 4180, -600, "zijdeland_moederoase", "De Moederoase", (68, 70), False),
    ("z6_ruinedal", 3850, -1060, 4180, -900, "zijdeland_ruinedal", "Het Ruïnedal", (66, 68), False),
    ("z6_zoutvlakte", 3833, -740, 4060, -460, "zijdeland_zoutvlakte", "De Zoutvlakte", (64, 66), False),
    ("z6_palmenkloven", 3236, -1060, 3850, -800, "zijdeland_palmenkloven", "De Palmenkloven", (62, 64), False),
    ("z6_roze_duinen", 3236, -800, 3833, -460, "zijdeland_roze_duinen", "De Roze Duinen", (60, 62), False),
    ("z6_zijdeland", 3264, -1152, 4287, -385, "zijdeland", "Het Zijdeland", (60, 70), False),
]

# ------------------------------------------------------------------------------------------------ the planned spots
P = {}


def _p(pid, x, y, z):
    P[pid] = (x, y, z)


# Rozenkloof (arrival, line E). The cities record their own exact spots; these are the plan values.
for k, v in {
    "haven_z6_z5_vertrek": (3398, None, -768), "haven_6_5_landing": (3398, None, -766), "haven_6_5_bel": (3394, None, -782),
    "haven_6_5_vertrekbord": (3402, None, -778), "haven_6_5_kapitein": (3396, None, -770),
    "haven_6_5_luchtschip": (3400, 150, -753), "haven_6_5_mast": (3398, 172, -786),
    "rozenkloof_aankomst": (3398, None, -760), "rozenkloof_havenmeester": (3392, None, -778),
    "rozenkloof_plein": (3400, 70, -714), "rozenkloof_fontein": (3406, 70, -714),
    "rozenkloof_schatkamer": (3470, 75, -699), "rozenkloof_schatkamer_hal": (3484, 75, -699),
    "rozenkloof_kloofmoeder": (3488, 75, -699), "rozenkloof_stal": (3436, 71, -720), "rozenkloof_stalmeester": (3433, 71, -716),
    "rozenkloof_oma_dadel": (3412, 70, -720), "rozenkloof_bobbel": (3414, 70, -722),
    "rozenkloof_tapijtwinkel": (3384, 70, -708), "rozenkloof_tuinguh": (3372, 70, -712),
    "rozenkloof_postguh": (3390, 70, -722), "rozenkloof_zeemond": (3270, 64, -745),
    "rozenkloof_siq_onder": (3456, 73, -718), "rozenkloof_siq_boven": (3564, None, -794),
    "rozenkloof_richel_1": (3380, 85, -738), "rozenkloof_richel_2": (3380, 101, -742), "rozenkloof_richel_3": (3380, 119, -748),
    "rozenkloof_brug_1": (3352, None, -726), "rozenkloof_brug_2": (3446, None, -705),
    "rozenkloof_herberg": (3364, 70, -706), "rozenkloof_herberg_balie": (3364, 70, -702),
    "rozenkloof_bank": (3420, 70, -706), "rozenkloof_recruitbord": (3396, 70, -722),
    "kampvuur_rozenkloof_plein": (3394, 70, -718), "kampvuur_rozenkloof_luchthaven": (3404, None, -772),
    "uitkijk_zeemond": (3300, None, -770),
}.items():
    _p(k, *v)
for i, (x, y, z) in enumerate([(3340, 70, -732), (3366, 86, -738), (3428, 86, -690), (3352, 102, -742), (3440, 102, -688),
                               (3410, 120, -752)], 1):
    _p(f"rozenkloof_post_{i}", x, y, z)            # 6 rock houses on different ledges (side quest 3, Postguh Stempel)
for i, (x, y, z) in enumerate([(3360, 86, -738), (3400, 102, -744), (3430, 120, -688)], 1):
    _p(f"rozenkloof_tuin_{i}", x, y, z)            # the hanging gardens of ledge 1-3 (side quest 2)

# Saffraanpoort (capital)
for k, v in {
    "saffraanpoort_westpoort": (3644, 71, -806), "saffraanpoort_zuidpoort": (3758, 71, -748),
    "saffraanpoort_paleisplein": (3762, 71, -846), "paleis_poort": (3762, 71, -884), "paleis_spiegelzaal": (3760, 74, -905),
    "paleis_troon": (3782, 74, -905), "paleis_audientie": (3776, 74, -905), "paleis_grootvizier": (3778, 74, -900),
    "paleis_glans": (3748, 74, -900), "paleis_tuin": (3762, 71, -872), "paleis_iwan": (3762, 71, -888),
    "paleis_dak": (3762, None, -920),
    "bazaar_west": (3702, 71, -812), "bazaar_kruising": (3762, 71, -812), "bazaar_oost": (3822, 71, -812),
    "bazaar_knoopje": (3740, 71, -809), "bazaar_koopman_1": (3720, 71, -815), "bazaar_koopman_2": (3780, 71, -809),
    "bazaar_koopman_3": (3800, 71, -815), "bazaar_smidse": (3712, 71, -809), "bazaar_naaiatelier": (3730, 71, -815),
    "bazaar_dak": (3762, None, -812),
    "saffraanbrug": (3810, None, -846), "saffraanbrug_paviljoen": (3810, None, -846),
    "noria_1": (3700, 71, -850), "noria_2": (3790, 71, -836), "noria_3": (3840, 71, -858), "saffraanpoort_knars": (3704, 71, -846),
    "kanaalwijk": (3700, 71, -770), "kanaalwijk_pluk": (3702, 71, -772),
    "karavanserai": (3679, 71, -797), "karavanserai_hof": (3679, 71, -797), "karavanserai_gildebord": (3672, 71, -790),
    "karavanserai_keuken": (3688, 71, -790), "karavanserai_stal": (3679, 71, -785),
    "sterrenwacht": (3846, 71, -898), "sterrenwacht_twinkel": (3846, 71, -902), "sterrenwacht_top": (3846, None, -906),
    "sterrenwacht_brouwerij": (3838, 71, -896),
    "saffraanpoort_herberg": (3730, 71, -780), "saffraanpoort_herberg_balie": (3730, 71, -776),
    "saffraanpoort_bank": (3790, 71, -782), "saffraanpoort_recruitbord": (3650, 71, -812),
    "kampvuur_saffraanpoort_karavanserai": (3684, 71, -800), "kampvuur_saffraanpoort_paleis": (3770, 71, -846),
}.items():
    _p(k, *v)
for i, (x, z) in enumerate([(3762, -812), (3700, -776), (3820, -760), (3740, -790), (3800, -880)], 1):
    _p(f"kwastje_{i}", x, None, z)                # side quest 4: 5 carpet tassels (roofs, bazaar, canal)
for i in range(10):
    _p(f"kanaalwijk_eend_{i + 1}", 3680 + (i % 5) * 9, 70, -778 + (i // 5) * 12)   # side quest 7: rubber ducks on the canals

# The star beacons (x, z from the concept)
for naam, (x, z) in {"palmbaken": (3478, -906), "duinbaken": (3655, -560), "zoutbaken": (3930, -612),
                     "ruinebaken": (4045, -1000), "oasebaken": (4062, -842)}.items():
    _p(naam, x, None, z + 13)                      # stand spot in front of the beacon (the plinth's south side)
    _p(naam + "_ster", x, None, z)                 # (no snap) the star itself, filled in by landschap
    _p(naam + "_puzzel", x + 18, None, z)          # the constellation puzzle on the ground (centre)

# De Roze Duinen
for k, v in {"duinguh_rimpel": (3646, None, -548), "wormelien_arena": (3700, None, -600), "bobbeltje_duin": (3684, None, -662),
             "duinen_karavaanweg": (3726, None, -625), "duinput": (3612, None, -672), "kampvuur_duinput": (3616, None, -668),
             "uitkijk_duinkam": (3596, None, -540)}.items():
    _p(k, *v)
for i, (x, z) in enumerate([(3566, -794), (3596, -806), (3622, -826), (3640, -846), (3700, -530), (3716, -580), (3738, -672),
                            (3758, -744)], 1):
    _p(f"karavaan_weg_{i}", x, None, z)           # escort waypoints (hq02/hq03): Siq top -> Westpoort, and the Duinroute

# De Palmenkloven
for k, v in {"palmguh_dadelpit": (3486, None, -902), "palmbaken_spiegel_1": (3470, None, -896),
             "palmbaken_spiegel_2": (3488, None, -896), "palmbaken_spiegel_3": (3478, None, -918),
             "moerbei_1": (3430, None, -960), "moerbei_2": (3460, None, -1000), "moerbei_3": (3520, None, -950),
             "watervallen_voet": (3540, None, -956), "watervallen_top": (3560, None, -930), "koerier_spetter": (3536, None, -960),
             "kampvuur_palmenkloven": (3500, None, -962), "uitkijk_palmwaterval": (3566, None, -926)}.items():
    _p(k, *v)
for i, (x, z) in enumerate([(3420, -984), (3470, -972), (3520, -955), (3580, -930), (3630, -900)], 1):
    _p(f"waterval_{i}", x, None, z)               # side quest 10: the carpet time trial checkpoints

# De Zoutvlakte (+ the cave below)
for k, v in {"karavaankamp": (3906, None, -584), "kampvuur_karavaankamp": (3906, None, -584),
             "zoutzieder_korrel": (3880, None, -540), "zoutziedershuisje": (3874, None, -548),
             "zoutkolos_arena": (3995, None, -560), "zoutgrot_ingang": (3874, None, -505),
             "zoutgrot": (3874, 50, -520), "zoutgrot_meer": (3880, 44, -540), "zoutgrot_puzzel": (3866, 48, -532),
             "zoutgrot_elite": (3890, 46, -548), "zoutgrot_schat": (3858, 46, -548), "zoutgrot_mijn": (3884, 48, -524),
             "zoutgrot_vis": (3876, 45, -536), "uitkijk_zoutrots": (4030, None, -640)}.items():
    _p(k, *v)
for i in range(5):
    _p(f"zoutgrot_kristal_{i + 1}", 3860 + i * 3, 48, -528)   # the singing crystals (melody puzzle)

# Het Ruïnedal
for k, v in {"kerker_ingang": (3985, None, -949), "kerker_poort": (3985, None, -955), "professor_stof": (3970, None, -944),
             "ruinedal_kamp": (3960, None, -940), "kampvuur_ruinedal": (3958, None, -936), "uitkijk_mesa": (3920, None, -1010)}.items():
    _p(k, *v)
for i, (x, z) in enumerate([(3880, -930), (3920, -955), (3960, -985), (4010, -975), (4070, -1000), (4110, -1030)], 1):
    _p(f"tablet_{i}", x, None, z)                 # side quest 14: the stone tablets of the Sterrenguhs

# De Moederoase + the raid tower (exterior; the inside is u-z6raid's)
for k, v in {"moederoase": (4060, None, -790), "moederpalm": (4080, None, -812), "moeder_bron": (4072, None, -806),
             "toren_voet": (4116, None, -688), "toren_poort": (4122, None, -688), "toren_raid_ingang": (4124, None, -688),
             "mager_mientje": (4150, None, -760), "oostroute": (4170, None, -730), "kampvuur_moederoase": (4054, None, -820),
             "uitkijk_oase": (4040, None, -720)}.items():
    _p(k, *v)

# secrets (u-z6verhaal's ids + their themed spots; the city ones are recorded by the city builders)
GEHEIM_KNUFFEL = [(3436, None, -684), (3612, None, -660), (3480, None, -986), (3770, None, -872), (3950, None, -540),
                  (3826, None, -910), (3754, None, -894), (4090, None, -800)]
GEHEIM_SCHAT = [(3476, None, -708), (3740, None, -892), (4012, None, -928)]      # schatkamer, minaret, mesa
GEHEIM_IN_STAD = {1, 4, 6, 7}           # knuffels the city builders place (no rock nook)
for i, (x, y, z) in enumerate(GEHEIM_KNUFFEL, 1):
    _p(f"geheim_knuffel_{i}", x, y, z)
for i, (x, y, z) in enumerate(GEHEIM_SCHAT, 1):
    _p(f"geheim_schat_{i}", x, y, z)

# ------------------------------------------------------------------------------------------------ ids of the other zone-6 slices
# u-z6verhaal (Z6VerhaalPlekken placeholders): new spots
for k, v in {
    "palmenkloven_monding": (3306, None, -1000), "palmenkloven_palmenbos": (3450, None, -972),
    "zoutvlakte_mikakamp": (3972, None, -582), "oostroute_luchtspiegeling": (4020, None, -662),
    "duinen_tent": (3640, None, -620), "duinen_rustplek": (3598, None, -742), "duinen_kam": (3622, None, -690),
    "ruinedal_tafelmesa": (4010, None, -930), "duinen": (3660, None, -640), "palmenkloven": (3460, None, -960),
    "zoutvlakte": (3945, None, -598), "ruinedal": (3985, None, -975), "dak_2": (3800, None, -820),
    "toren_top": (4140, 236, -688),
}.items():
    _p(k, *v)
for i in range(1, 4):
    _p(f"oase_tent_{i}", 4140 + 4 * i, None, -762)                       # the refugees (z6_vluchteling_i)
for i, (x, y, z) in enumerate([(3396, 70, -708), (3372, 86, -744), (3746, 71, -818), (3780, 71, -852), (3768, 71, -908),
                               (3664, 71, -834)], 1):
    _p(f"z6_burger_{i}", x, y, z)
for i, (x, y, z) in enumerate([(3404, 70, -710), (3400, None, -778), (3646, 71, -840), (3768, 71, -842), (3924, None, -646),
                               (4064, None, -778)], 1):
    _p(f"z6_wacht_{i}", x, y, z)
# u-z6wezens (Z6WezensPlekken placeholders): mob regions, leaders, safe spots, field bosses, the dungeon hatch, the cave elite
for gebied, pts in {
    "duinen": [(3640, -700), (3690, -520), (3760, -560), (3600, -600)],
    "palmenkloven": [(3470, -985), (3560, -948), (3420, -990)],
    "zoutvlakte": [(3880, -640), (3990, -610), (3940, -520)],
    "ruinedal": [(3920, -960), (4020, -1010), (4090, -1020)],
    "moederoase": [(4040, -760), (4110, -820), (4060, -720)],
}.items():
    for i, (x, z) in enumerate(pts, 1):
        _p(f"z6w_mobs_{gebied}_{i}", x, None, z)
for k, v in {
    "z6w_zout_nacht": (3860, None, -620), "z6w_leider_oorschelp": (3620, None, -560), "z6w_leider_pitje": (3520, None, -962),
    "z6w_leider_korstje": (3960, None, -560), "z6w_leider_oudstewachter": (4040, None, -1000), "z6w_leider_brie": (4110, None, -790),
    "z6w_veilig_rozenkloof": (3400, 70, -714), "z6w_veilig_saffraanpoort": (3762, 71, -846), "z6w_veilig_karavaankamp": (3906, None, -580),
    "z6w_veilig_toren": (4100, None, -690),
    "z6w_wormelien": (3640, None, -610), "z6w_wormelien_deur": (3640, None, -582), "z6w_wormelien_bord": (3637, None, -580),
    "z6w_zoutkolos": (3995, None, -560), "z6w_zoutkolos_deur": (3995, None, -586), "z6w_zoutkolos_bord": (3992, None, -588),
    "z6w_test": (3762, 71, -846),
}.items():
    _p(k, *v)
# u-z6raid: the pocket anchor under the tower (their template, y 2..68; keep caves/water out of x 4115..4174, z -713..-663)
_p("raid_toren_binnen", 4140, 16, -688)

GEEN_SNAP = ("haven_6_5_luchtschip", "haven_6_5_mast", "palmbaken_ster", "duinbaken_ster", "zoutbaken_ster", "ruinebaken_ster",
             "oasebaken_ster", "toren_top", "raid_toren_binnen", "zoutgrot_kristalwand")
# alias -> id (the other slices' names for the same spot)
ALIAS = {"luchthaven_rozenkloof": "haven_z6_z5_vertrek", "zoutgrot_ingang_buiten": "zoutgrot_ingang",
         # u-z6verhaal
         "rozenkloof_luchthaven": "haven_z6_z5_vertrek", "rozenkloof_siq_top": "rozenkloof_siq_boven", "rozenkloof_karavaan": "rozenkloof_oma_dadel",
         "rozenkloof_post": "rozenkloof_postguh", "rozenkloof_tuin_richel1": "rozenkloof_tuin_1",
         "saffraanpoort_plein": "saffraanpoort_paleisplein", "saffraanpoort_paleis": "paleis_poort", "saffraanpoort_spiegelzaal": "paleis_spiegelzaal",
         "spiegelzaal_troon": "paleis_troon", "saffraanpoort_bazaar": "bazaar_kruising", "saffraanpoort_poort_west": "saffraanpoort_westpoort",
         "saffraanpoort_poort_oost": "saffraanpoort_oostpoort", "saffraanpoort_karavanserai": "karavanserai",
         "saffraanpoort_sterrenwacht": "sterrenwacht", "saffraanpoort_sterrenwacht_kijker": "sterrenwacht_top",
         "saffraanpoort_paradijstuin": "paleis_tuin", "saffraanpoort_saffraanbrug": "saffraanbrug", "bazaar_atelier": "bazaar_naaiatelier",
         "dak_1": "bazaar_dak", "examen_1": "sterrenwacht_top", "examen_2": "saffraanbrug_paviljoen", "examen_3": "paleis_minaret",
         "verzonken_sterrenwacht": "kerker_ingang", "wormelien": "wormelien_arena", "palmenkloven_waterval_onder": "watervallen_voet",
         "palmenkloven_moerbei": "moerbei_1", "zoutvlakte_ziedershuisje": "zoutzieder_korrel", "zoutvlakte_ziedershuisje_huis": "zoutziedershuisje",
         "zoutvlakte_kamp": "karavaankamp", "zoutgrot_kristallen": "zoutgrot_puzzel", "duinen_bobbeltje": "bobbeltje_duin",
         "oase_tenten": "mager_mientje",
         "kampvuur_rozenkloof": "kampvuur_rozenkloof_plein", "kampvuur_saffraanpoort": "kampvuur_saffraanpoort_paleis",
         "kampvuur_duinen": "kampvuur_duinput", "kampvuur_zoutvlakte": "kampvuur_karavaankamp",
         # u-z6wezens
         "z6w_sterrenwacht_ingang": "kerker_ingang", "z6w_zoutgrot_elite": "zoutgrot_elite",
         # u-z6raid
         "raid_toren_poort": "toren_raid_ingang", "raid_toren_buiten": "toren_poort"}
UITKIJKEN = ["uitkijk_zeemond", "uitkijk_duinkam", "uitkijk_palmwaterval", "uitkijk_zoutrots", "uitkijk_mesa", "uitkijk_oase"]


def plekken():
    return dict(P)


def gebied(x, z):
    for g in GEBIEDEN:
        if g[1] <= x <= g[3] and g[2] <= z <= g[4]:
            return g
    return None
