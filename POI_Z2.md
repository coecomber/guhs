# POI_Z2 - coordinates of zone 2 "De Splinterpieken" (z2-wereld slice)

**v1, GENERATED** by `tools/vadsrijk/maak_kaart.py` (zone-2 part `tools/vadsrijk/zone2/`) from the real map; the x/z/y of the plan (v0) are kept. Do not edit by hand; requests go to `../POI_VERZOEKEN_Z2.md`. Java: `nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken` (`plek("id")`, `GEBIEDEN`, `MOBZONES`, `KAMPVUREN`, `DEUREN`).

All positions are **stand positions** (the air block a player/NPC stands in) in `guhs:vadsrijk`, unless noted (`doos` = box corners, door `min/max` = the door's own blocks, `*_straal` = radius in x). Cloud sea level 62; Kandijkom floor stand y 91; Spiegelburcht floor stand y 176. Dev: `/guhs vadsrijk z2wereld plek <id>` teleports there.

## Areas (kern.Gebieden, first match wins)

| id | box (minX, minZ)..(maxX, maxZ) | levels | town |
|---|---|---|---|
| `z2_spiegelburcht` | (-70,-2995)..(70,-2850) | 33-35 | no |
| `z2_kandijkom_dok` | (-40,-2249)..(40,-2200) | 20-35 | yes |
| `z2_kandijkom` | (-150,-2550)..(150,-2250) | 20-35 | yes |
| `z2_galmhellingen` | (151,-2700)..(600,-1800) | 20-25 | no |
| `z2_knuffelmeer` | (-600,-2700)..(-151,-1800) | 25-30 | no |
| `z2_splinterkloof` | (-600,-3000)..(600,-2701) | 30-34 | no |
| `z2_ring` | (-600,-3000)..(600,-1800) | 20-35 | no |

## Mob zones (boxes for the wezens slice; `Z2WereldPlekken.MOBZONES`)

| id | box | levels |
|---|---|---|
| `mob_galm_voet` | (160,-2360)..(360,-1850) | 20-22 |
| `mob_galm_naalden` | (360,-2200)..(590,-1850) | 22-24 |
| `mob_galm_top` | (200,-2690)..(590,-2370) | 23-25 |
| `mob_meer_oever` | (-590,-2420)..(-160,-1850) | 25-27 |
| `mob_meer_ijs` | (-520,-2400)..(-240,-2060) | 27-29 |
| `mob_meer_noord` | (-590,-2690)..(-160,-2430) | 28-30 |
| `mob_kloof_zuid` | (-590,-2750)..(590,-2700) | 30-31 |
| `mob_kloof_bodem` | (-590,-2810)..(590,-2745) | 31-33 |
| `mob_kloof_noord` | (-590,-2990)..(590,-2810) | 32-34 |
| `mob_burcht` | (-19,-2984)..(19,-2856) | 33-35 |

## Moving door

`spiegelburcht` (WereldDeuren id): blocks [-3, 176, -2856]..[3, 184, -2856] (wings slide along x), stand in front [0, 176, -2851], hidden copy at y 8. `WereldDeuren.open("spiegelburcht", player)` / `open(level, id)` / `sluit(level, id)`.

## Travel (airship dock, gondola)

| id | x, y, z |
|---|---|
| `dok_bel` | 4, 91, -2232 |
| `dok_eind` | 0, 91, -2226 |
| `dok_kapitein` | -4, 91, -2240 |
| `dok_landing` | 0, 91, -2244 |
| `gondel_berg` | 306, 151, -2616 |
| `gondel_berg_kabel` | 306, 159, -2618 |
| `gondel_berg_knop` | 309, 151, -2615 |
| `gondel_dal` | 78, 91, -2466 |
| `gondel_dal_kabel` | 78, 99, -2468 |
| `gondel_dal_knop` | 81, 91, -2465 |
| `gondel_kabel_berg` | 306, 159, -2618 |
| `gondel_kabel_dal` | 78, 99, -2468 |
| `npc_gondel` | 76, 91, -2465 |

## Kandijkom: plaza, NPC spots, secrets in town

| id | x, y, z |
|---|---|
| `cafe_terras` | 38, 91, -2344 |
| `geheim_chocokelder` | 52, 85, -2344 |
| `kristalkapel_altaar` | 0, 91, -2476 |
| `npc_bank` | -55, 91, -2344 |
| `npc_burgemeester` | 0, 91, -2418 |
| `npc_cafe` | 59, 91, -2344 |
| `npc_ijsvisser` | -418, 88, -2261 |
| `npc_juwelier` | -55, 91, -2440 |
| `npc_kristalkapel` | 0, 91, -2474 |
| `npc_schaatsguh` | 13, 91, -2400 |
| `npc_smid` | 55, 91, -2440 |
| `npc_stal` | -90, 91, -2337 |
| `npc_winkel_kandij` | 68, 91, -2392 |
| `npc_winkel_wantjes` | -68, 91, -2392 |
| `plein` | 0, 91, -2412 |
| `questbord` | 14, 91, -2423 |
| `schaatsvijver` | 0, 91, -2400 |
| `sneeuwguhspar` | 0, 91, -2440 |

## Kandijkom buildings (deur = stand outside the door, binnen = interior centre, npc = behind the counter, doos = box)

| id | x, y, z |
|---|---|
| `gebouw.bank.binnen` | -50, 91, -2344 |
| `gebouw.bank.deur` | -41, 91, -2344 |
| `gebouw.bank.doos.1` | -57, 90, -2349 |
| `gebouw.bank.doos.2` | -43, 97, -2339 |
| `gebouw.bank.npc` | -55, 91, -2344 |
| `gebouw.bank.toonbank` | -53, 91, -2344 |
| `gebouw.cafe.binnen` | 52, 91, -2344 |
| `gebouw.cafe.deur` | 41, 91, -2344 |
| `gebouw.cafe.doos.1` | 43, 90, -2350 |
| `gebouw.cafe.doos.2` | 61, 97, -2338 |
| `gebouw.cafe.npc` | 59, 91, -2344 |
| `gebouw.cafe.toonbank` | 57, 91, -2344 |
| `gebouw.huis_1.binnen` | -92, 91, -2440 |
| `gebouw.huis_1.deur` | -85, 91, -2440 |
| `gebouw.huis_1.doos.1` | -97, 90, -2444 |
| `gebouw.huis_1.doos.2` | -87, 96, -2436 |
| `gebouw.huis_2.binnen` | 92, 91, -2436 |
| `gebouw.huis_2.deur` | 85, 91, -2436 |
| `gebouw.huis_2.doos.1` | 87, 90, -2440 |
| `gebouw.huis_2.doos.2` | 97, 96, -2432 |
| `gebouw.huis_3.binnen` | -98, 91, -2390 |
| `gebouw.huis_3.deur` | -91, 91, -2390 |
| `gebouw.huis_3.doos.1` | -103, 90, -2394 |
| `gebouw.huis_3.doos.2` | -93, 96, -2386 |
| `gebouw.huis_4.binnen` | 100, 91, -2390 |
| `gebouw.huis_4.deur` | 93, 91, -2390 |
| `gebouw.huis_4.doos.1` | 95, 90, -2394 |
| `gebouw.huis_4.doos.2` | 105, 96, -2386 |
| `gebouw.huis_5.binnen` | -30, 91, -2300 |
| `gebouw.huis_5.deur` | -23, 91, -2300 |
| `gebouw.huis_5.doos.1` | -35, 90, -2304 |
| `gebouw.huis_5.doos.2` | -25, 96, -2296 |
| `gebouw.huis_6.binnen` | 30, 91, -2300 |
| `gebouw.huis_6.deur` | 23, 91, -2300 |
| `gebouw.huis_6.doos.1` | 25, 90, -2304 |
| `gebouw.huis_6.doos.2` | 35, 96, -2296 |
| `gebouw.huis_7.binnen` | -80, 91, -2296 |
| `gebouw.huis_7.deur` | -80, 91, -2302 |
| `gebouw.huis_7.doos.1` | -85, 90, -2300 |
| `gebouw.huis_7.doos.2` | -75, 96, -2292 |
| `gebouw.huis_8.binnen` | -52, 91, -2496 |
| `gebouw.huis_8.deur` | -52, 91, -2490 |
| `gebouw.huis_8.doos.1` | -57, 90, -2500 |
| `gebouw.huis_8.doos.2` | -47, 96, -2492 |
| `gebouw.juwelier.binnen` | -50, 91, -2440 |
| `gebouw.juwelier.deur` | -41, 91, -2440 |
| `gebouw.juwelier.doos.1` | -57, 90, -2445 |
| `gebouw.juwelier.doos.2` | -43, 97, -2435 |
| `gebouw.juwelier.npc` | -55, 91, -2440 |
| `gebouw.juwelier.toonbank` | -53, 91, -2440 |
| `gebouw.kristalkapel.altaar` | 0, 91, -2476 |
| `gebouw.kristalkapel.binnen` | 0, 91, -2470 |
| `gebouw.kristalkapel.deur` | 0, 91, -2460 |
| `gebouw.kristalkapel.doos.1` | -11, 90, -2478 |
| `gebouw.kristalkapel.doos.2` | 11, 103, -2462 |
| `gebouw.kristalkapel.npc` | 0, 91, -2474 |
| `gebouw.smid.binnen` | 50, 91, -2440 |
| `gebouw.smid.deur` | 41, 91, -2440 |
| `gebouw.smid.doos.1` | 43, 90, -2445 |
| `gebouw.smid.doos.2` | 57, 97, -2435 |
| `gebouw.smid.npc` | 55, 91, -2440 |
| `gebouw.smid.toonbank` | 53, 91, -2440 |
| `gebouw.stal.binnen` | -96, 91, -2336 |
| `gebouw.stal.deur` | -89, 91, -2336 |
| `gebouw.stal.doos.1` | -101, 90, -2340 |
| `gebouw.stal.doos.2` | -91, 101, -2332 |
| `gebouw.stal.npc` | -90, 91, -2337 |
| `gebouw.stal.wei` | -96, 91, -2325 |
| `gebouw.winkel_kandij.binnen` | 64, 91, -2392 |
| `gebouw.winkel_kandij.deur` | 56, 91, -2392 |
| `gebouw.winkel_kandij.doos.1` | 58, 90, -2397 |
| `gebouw.winkel_kandij.doos.2` | 70, 97, -2387 |
| `gebouw.winkel_kandij.npc` | 68, 91, -2392 |
| `gebouw.winkel_kandij.toonbank` | 66, 91, -2392 |
| `gebouw.winkel_wantjes.binnen` | -64, 91, -2392 |
| `gebouw.winkel_wantjes.deur` | -56, 91, -2392 |
| `gebouw.winkel_wantjes.doos.1` | -70, 90, -2397 |
| `gebouw.winkel_wantjes.doos.2` | -58, 97, -2387 |
| `gebouw.winkel_wantjes.npc` | -68, 91, -2392 |
| `gebouw.winkel_wantjes.toonbank` | -66, 91, -2392 |

## Arenas and bosses

| id | x, y, z |
|---|---|
| `arena_boormeester` | -500, 66, -2836 |
| `arena_boormeester_deur` | -500, 66, -2779 |
| `arena_boormeester_straal` | 20, 0, 0 |
| `arena_galmwolf` | 430, 121, -2280 |
| `arena_galmwolf_bord` | 393, 121, -2276 |
| `arena_galmwolf_deur` | 392, 121, -2280 |
| `arena_spiegelzucht` | 0, 176, -2961 |
| `arena_spiegelzucht_bord` | 4, 176, -2936 |
| `arena_spiegelzucht_deur` | 0, 176, -2937 |
| `arena_spiegelzucht_straal` | 22, 0, 0 |
| `boormeester_kop_1` | -500, 66, -2824 |
| `boormeester_kop_2` | -510, 66, -2842 |
| `boormeester_kop_3` | -490, 66, -2842 |
| `galmwolf_naald` | 430, 155, -2320 |
| `galmwolf_pilaar_1` | 449, 121, -2276 |
| `galmwolf_pilaar_2` | 440, 121, -2264 |
| `galmwolf_pilaar_3` | 426, 121, -2261 |
| `galmwolf_pilaar_4` | 414, 121, -2270 |
| `galmwolf_pilaar_5` | 411, 121, -2284 |
| `galmwolf_pilaar_6` | 420, 121, -2296 |
| `galmwolf_pilaar_7` | 434, 121, -2299 |
| `galmwolf_pilaar_8` | 446, 121, -2290 |
| `spiegelzucht_spiegel_1` | 15, 176, -2952 |
| `spiegelzucht_spiegel_2` | 0, 176, -2944 |
| `spiegelzucht_spiegel_3` | -15, 176, -2952 |
| `spiegelzucht_spiegel_4` | -15, 176, -2970 |
| `spiegelzucht_spiegel_5` | 0, 176, -2978 |
| `spiegelzucht_spiegel_6` | 15, 176, -2970 |

## De Spiegelburcht (dungeon): door, rooms, gates (open AIR, bazen fills), puzzle spots

| id | x, y, z |
|---|---|
| `burcht_lichtkamer` | 0, 176, -2890 |
| `burcht_prismazaal` | 0, 176, -2928 |
| `burcht_spiegelgang` | 0, 176, -2909 |
| `burcht_splinter` | 0, 177, -2976 |
| `burcht_trap_voet` | 0, 151, -2807 |
| `burcht_voorhal` | 0, 176, -2868 |
| `burcht_voorplein` | 0, 176, -2843 |
| `deur_spiegelburcht_voor` | 0, 176, -2851 |
| `poort_1` | 0, 176, -2880 |
| `poort_2` | 0, 176, -2900 |
| `poort_3` | 0, 176, -2918 |
| `poort_4` | 0, 176, -2938 |
| `puzzel_lichtkamer_1` | -7, 176, -2894 |
| `puzzel_lichtkamer_2` | 7, 176, -2894 |
| `puzzel_lichtkamer_3` | -7, 176, -2885 |
| `puzzel_lichtkamer_4` | 7, 176, -2885 |
| `puzzel_lichtkamer_5` | 0, 176, -2890 |
| `puzzel_lichtkamer_bron` | -14, 178, -2890 |
| `puzzel_lichtkamer_doel` | 0, 182, -2899 |
| `puzzel_prismazaal_1` | 0, 176, -2928 |
| `puzzel_prismazaal_2` | -8, 176, -2923 |
| `puzzel_prismazaal_3` | 8, 176, -2923 |
| `puzzel_prismazaal_4` | -8, 176, -2933 |
| `puzzel_prismazaal_5` | 8, 176, -2933 |
| `puzzel_spiegelgang_1` | -5, 176, -2904 |
| `puzzel_spiegelgang_2` | 5, 176, -2904 |
| `puzzel_spiegelgang_3` | -5, 176, -2909 |
| `puzzel_spiegelgang_4` | 5, 176, -2909 |
| `puzzel_spiegelgang_5` | -5, 176, -2914 |
| `puzzel_spiegelgang_6` | 5, 176, -2914 |

## Campfires (unlit, kern checkpoints) + ridder spots

| id | x, y, z |
|---|---|
| `kampvuur_galmhellingen_1` | 232, 99, -2330 |
| `kampvuur_galmhellingen_2` | 330, 127, -2120 |
| `kampvuur_galmhellingen_3` | 480, 150, -2440 |
| `kampvuur_galmhellingen_4` | 250, 115, -2560 |
| `kampvuur_kandijkom_dok_1` | -9, 91, -2262 |
| `kampvuur_knuffelmeer_1` | -200, 89, -2120 |
| `kampvuur_knuffelmeer_2` | -560, 96, -2240 |
| `kampvuur_knuffelmeer_3` | -250, 107, -2440 |
| `kampvuur_knuffelmeer_4` | -470, 114, -2500 |
| `kampvuur_spiegelburcht_1` | 10, 152, -2808 |
| `kampvuur_splinterkloof_1` | -260, 139, -2700 |
| `kampvuur_splinterkloof_2` | 300, 140, -2702 |
| `kampvuur_splinterkloof_3` | 24, 132, -2730 |
| `kampvuur_splinterkloof_4` | -100, 65, -2774 |
| `kampvuur_splinterkloof_5` | -360, 147, -2880 |
| `ridder_galmhellingen_1` | 234, 99, -2328 |
| `ridder_galmhellingen_2` | 332, 127, -2118 |
| `ridder_galmhellingen_3` | 482, 150, -2438 |
| `ridder_galmhellingen_4` | 252, 115, -2558 |
| `ridder_kandijkom_dok_1` | -7, 91, -2260 |
| `ridder_knuffelmeer_1` | -198, 89, -2118 |
| `ridder_knuffelmeer_2` | -558, 96, -2238 |
| `ridder_knuffelmeer_3` | -248, 107, -2438 |
| `ridder_knuffelmeer_4` | -468, 114, -2498 |
| `ridder_spiegelburcht_1` | 12, 152, -2806 |
| `ridder_splinterkloof_1` | -258, 139, -2698 |
| `ridder_splinterkloof_2` | 302, 140, -2700 |
| `ridder_splinterkloof_3` | 26, 132, -2728 |
| `ridder_splinterkloof_4` | -98, 65, -2772 |
| `ridder_splinterkloof_5` | -358, 147, -2878 |

## Viewpoints (needle tops, panorama on first visit)

| id | x, y, z |
|---|---|
| `uitkijk_galmtop` | 520, 221, -2585 |
| `uitkijk_kloofkijk` | -210, 208, -2930 |
| `uitkijk_meerkijk` | -540, 157, -2060 |

## De Galmhellingen

| id | x, y, z |
|---|---|
| `galm_lawine_1` | 500, 168, -2640 |
| `galm_lawine_1_voet` | 360, 149, -2430 |
| `galm_lawine_2` | 590, 158, -2380 |
| `galm_lawine_2_voet` | 500, 145, -2120 |
| `galm_lawine_3` | 560, 134, -2050 |
| `galm_lawine_3_voet` | 400, 137, -1880 |
| `galm_naalden_1` | 330, 130, -2200 |
| `galm_naalden_2` | 500, 147, -2000 |
| `galm_naalden_3` | 260, 95, -1980 |
| `galm_naalden_4` | 420, 165, -2480 |

## Het Bevroren Knuffelmeer (knuffel_ijs = stand on the ice above a frozen plushie)

| id | x, y, z |
|---|---|
| `draak_zadel` | -330, 126, -2610 |
| `draak_zadel_kijk` | -330, 126, -2582 |
| `knuffel_ijs_1` | -300, 85, -2160 |
| `knuffel_ijs_10` | -270, 85, -2250 |
| `knuffel_ijs_2` | -350, 85, -2300 |
| `knuffel_ijs_3` | -430, 85, -2180 |
| `knuffel_ijs_4` | -470, 85, -2320 |
| `knuffel_ijs_5` | -380, 85, -2380 |
| `knuffel_ijs_6` | -300, 85, -2340 |
| `knuffel_ijs_7` | -440, 85, -2090 |
| `knuffel_ijs_8` | -360, 85, -2110 |
| `knuffel_ijs_9` | -500, 85, -2220 |
| `meer_eiland` | -420, 88, -2261 |
| `meer_midden` | -380, 85, -2230 |
| `meer_schotsen` | -330, 85, -2078 |

## De Splinterkloof (bridges: stand at each end; stairways: the foot on the ravine floor)

| id | x, y, z |
|---|---|
| `boortoren_1` | -330, 140, -2738 |
| `boortoren_1_top` | -330, 160, -2738 |
| `boortoren_2` | 130, 137, -2822 |
| `boortoren_2_top` | 130, 157, -2822 |
| `boortoren_3` | 400, 136, -2744 |
| `boortoren_3_top` | 400, 156, -2744 |
| `brug_1_noord` | -430, 139, -2781 |
| `brug_1_zuid` | -430, 138, -2734 |
| `brug_2_noord` | -180, 141, -2797 |
| `brug_2_zuid` | -180, 140, -2750 |
| `brug_3_noord` | 0, 151, -2807 |
| `brug_3_zuid` | 0, 150, -2765 |
| `brug_4_noord` | 220, 142, -2810 |
| `brug_4_zuid` | 220, 142, -2758 |
| `brug_5_noord` | 460, 132, -2791 |
| `brug_5_zuid` | 460, 132, -2750 |
| `kloof_trap_1` | -399, 66, -2753 |
| `kloof_trap_2` | -109, 66, -2764 |
| `kloof_trap_3` | 161, 66, -2763 |
| `kloof_trap_4` | 421, 66, -2763 |

## Secrets

| id | x, y, z |
|---|---|
| `geheim_boorplaats` | -500, 66, -2783 |
| `geheim_burchtdak` | 10, 195, -2903 |
| `geheim_galmgrot` | 566, 140, -1990 |
| `geheim_galmgrot_ingang` | 555, 140, -1990 |
| `geheim_ijsgrot` | -575, 122, -2400 |
| `geheim_ijsgrot_ingang` | -586, 122, -2400 |

## Landmarks

| id | x, y, z |
|---|---|
| `landmark_spits_1` | -668, 158, -1790 |
| `landmark_spits_2` | 664, 182, -2940 |
| `landmark_spits_3` | -662, 146, -2980 |
| `landmark_spits_4` | 670, 132, -1850 |

## Other

| id | x, y, z |
|---|---|
| `aantal_bomen` | 2264, 0, 0 |
| `mika_fort` | 300, 153, -2882 |
| `mika_fort_poort` | 300, 153, -2867 |
| `mika_fort_toren` | 310, 168, -2894 |
