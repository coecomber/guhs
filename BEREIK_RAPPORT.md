# BEREIK_RAPPORT: kan een survival-speler overal komen? (W-BEREIK)

**GENERATED** by `python tools/vadsrijk/bereikcheck.py` (2026-11-09 12:04). Global flood over all 1305+ tiles from the start spawn, airship lines, lifts and hatches as edges. Player model and sources: see the docstring of the script.

A target counts as **reached** when a reached stand cell lies within its radius (NPC 3.5 blocks, quest POI 5, area 10, boss 12, spawner = its radius) and height window. "via" = which zone entries reach it (the start reaches everything that is reached).

## Samenvatting per zone (bereikt / totaal)

| zone | hoofdquest-doelen | zijquest-doelen | NPC's | geheimen/verzamelplekken | baasarena's (deur) | mob-spawners | puzzels | overige plekken |
|---|---|---|---|---|---|---|---|---|
| 1 | 24/24 | 40/40 | 52/52 | 61/61 | 1/1 | **318/325** | 7/7 | **308/320** |
| 2 | 28/28 | 13/13 | 62/62 | **62/63** | - | **50/112** | 7/7 | **420/432** |
| 3 | 43/43 | 11/11 | 61/61 | 83/83 | 8/8 | **92/147** | 7/7 | **558/565** |
| 4 | 24/24 | 26/26 | 48/48 | 121/121 | 1/1 | 20/20 | 1/1 | **344/346** |
| 5 | 23/23 | 28/28 | 50/50 | 36/36 | 3/3 | 21/21 | 1/1 | **406/408** |
| 6 | 37/37 | **37/38** | 53/53 | **45/47** | 2/2 | 39/39 | - | 420/420 |

Reached ONLY with a gap jump (1-2 blocks, the hat paths / rooftops): 1 quest/NPC/secret targets: `npc:z2_kasguh`

Runtime-built areas (not in the tiles, not checked here): the guild valley (-12000, 12000), De Verzonken Sterrenwacht (Z6Bouw.sterrenwacht: rooms under the Ruinedal, entered by the hatch link); Raid binnentoren (Z6RaidBouw: the template raid_binnentoren, placed at runtime); Raid binnentoren top (Z6RaidBouw).

Targets outside the map tiles (areas built at runtime, e.g. the guild valley; not checked): `npc:bond_pleinwachter`, `npc:bond_bord_vallei`, `arena:z6wezens:astrolabiumwachter`, `arena:z6wezens:glimmertje`, `arena:z6wezens:sterrenguh`, `arena:z6raid:zandwachter`, `arena:z6raid:spiegelvizier`, `arena:z6raid:sterrenbeelden`, `arena:z6raid:ringeling`, `mobs:kern:proeftuin_oost`, `mobs:kern:proeftuin_west`, `mobs:z6wezens:kerker_ingang#dover`, `mobs:z6wezens:kerker_ingang#wachter`, `mobs:z6wezens:kerker_kaarten#dover`, `mobs:z6wezens:kerker_kaarten#ring`, `mobs:z6wezens:kerker_kaarten#ster`, `plek:zone2.wereld.Z2WereldPlekken.ALLE.arena_spiegelzucht_straal`, `plek:zone2.wereld.Z2WereldPlekken.ALLE.arena_boormeester_straal`, `plek:zone2.wereld.Z2WereldPlekken.ALLE.aantal_bomen`, `plek:zone6.verhaal.Z6VerhaalPlekken.TOREN_TOP`, `plek:zone6.wereld.Z6WereldPlekken.ALLE.raid_toren_binnen`, `plek:zone6.wereld.Z6WereldPlekken.ALLE.toren_top`, `plek:zone6.wezens.Z6WezensPlekken.KERKER_AANKOMST`, `plek:zone6.wezens.Z6WezensPlekken.KERKER_UITGANG`, `plek:zone6.wezens.Z6WezensPlekken.INGANGSHAL`, `plek:zone6.wezens.Z6WezensPlekken.RINGENZAAL`, `plek:zone6.wezens.Z6WezensPlekken.KAARTENZAAL`, `plek:zone6.wezens.Z6WezensPlekken.VUURSCHALENZAAL`, `plek:zone6.wezens.Z6WezensPlekken.KOEPEL`

Quest targets the game cannot place (no NPC/POI with that id): 0

## Zone 1 - Zwamzoet + Knuffelkamp

### Niet bereikt: mob-spawners (7)

| doel | plek | dichtstbijzijnde bereikte cel (afstand) | quests |
|---|---|---|---|
| `mobs:wezens:druipgrotten_elite_schacht_balkon#kristalklodder` | -500, 86, -487 | -511, 92, -493 (13.9) |  |
| `mobs:wezens:druipgrotten_schacht_bodem#glimzwam` | -496, 33, -487 | -476, 44, -496 (24.5) |  |
| `mobs:wezens:druipgrotten_schacht_bodem#kristalklodder` | -496, 33, -487 | -476, 44, -496 (24.5) |  |
| `mobs:wezens:druipgrotten_schacht_richel_1#knaag` | -484, 43, -473 | -468, 43, -469 (16.5) |  |
| `mobs:wezens:druipgrotten_schacht_richel_2#kristalklodder` | -507, 56, -479 | -523, 77, -490 (28.6) |  |
| `mobs:wezens:druipgrotten_schacht_richel_3#knaag` | -483, 68, -477 | -466, 44, -476 (29.4) |  |
| `mobs:wezens:druipgrotten_schacht_richel_4#kristalklodder` | -507, 81, -476 | -519, 87, -468 (15.6) |  |

### Niet bereikt: overige plekken (12)

| doel | plek | dichtstbijzijnde bereikte cel (afstand) | quests |
|---|---|---|---|
| `plek:bazen.BazenPlekken.K2_HINTS.2` | 334, 157, -446 | 334, 152, -446 (5.0) |  |
| `plek:bazen.BazenPlekken.K2_HINTS.3` | 326, 157, -446 | 326, 152, -446 (5.0) |  |
| `plek:wereld.WereldPlekken.ALLE.deur_zwamtempel.max` | 336, 175, -392 | 336, 152, -391 (23.0) |  |
| `plek:wereld.WereldPlekken.ALLE.druipschacht_balkon` | -500, 85, -487 | -511, 89, -495 (14.2) |  |
| `plek:wereld.WereldPlekken.ALLE.druipschacht_bodem` | -496, 32, -487 | -476, 44, -496 (25.0) |  |
| `plek:wereld.WereldPlekken.ALLE.druipschacht_meer` | -495, 31, -477 | -471, 43, -488 (29.0) |  |
| `plek:wereld.WereldPlekken.ALLE.druipschacht_richel_1` | -484, 42, -473 | -468, 43, -469 (16.5) |  |
| `plek:wereld.WereldPlekken.ALLE.druipschacht_richel_2` | -507, 55, -479 | -523, 77, -490 (29.3) |  |
| `plek:wereld.WereldPlekken.ALLE.druipschacht_richel_3` | -483, 67, -477 | -466, 44, -476 (28.6) |  |
| `plek:wereld.WereldPlekken.ALLE.druipschacht_richel_4` | -507, 80, -476 | -519, 87, -468 (16.0) |  |
| `plek:wereld.WereldPlekken.ALLE.hendelkamer.hints.2` | 334, 157, -446 | 334, 152, -446 (5.0) |  |
| `plek:wereld.WereldPlekken.ALLE.hendelkamer.hints.3` | 326, 157, -446 | 326, 152, -446 (5.0) |  |


## Zone 2 - De Splinterpieken

### Niet bereikt: geheimen/verzamelplekken (1)

| doel | plek | dichtstbijzijnde bereikte cel (afstand) | quests |
|---|---|---|---|
| `plek:zone2.wereld.Z2WereldPlekken.ALLE.uitkijk_galmtop` | 928, 195, -1106 | 924, 179, -1115 (18.8) |  |

### Niet bereikt: mob-spawners (62)

| doel | plek | dichtstbijzijnde bereikte cel (afstand) | quests |
|---|---|---|---|
| `mobs:z2wezens:galmhellingen_0_kristalnaaldkruiper` | 797, 140, -1126 | 801, 114, -1149 (34.9) |  |
| `mobs:z2wezens:galmhellingen_10_galmvleermuis` | 841, 140, -1088 | 848, 117, -1089 (24.1) |  |
| `mobs:z2wezens:galmhellingen_12_galmvleermuis` | 860, 140, -1089 | 861, 123, -1088 (17.1) |  |
| `mobs:z2wezens:galmhellingen_13_sneeuwknabbelaar` | 862, 140, -1058 | 876, 121, -1056 (23.7) |  |
| `mobs:z2wezens:galmhellingen_18_galmvleermuis` | 904, 140, -1135 | 899, 152, -1135 (13.0) |  |
| `mobs:z2wezens:galmhellingen_1_splinterling` | 806, 140, -1113 | 783, 110, -1103 (39.1) |  |
| `mobs:z2wezens:galmhellingen_23_splinterling` | 941, 140, -1040 | 941, 123, -1040 (17.0) |  |
| `mobs:z2wezens:galmhellingen_2_splinterling` | 798, 140, -967 | 791, 102, -978 (40.2) |  |
| `mobs:z2wezens:galmhellingen_3_sneeuwknabbelaar` | 797, 140, -948 | 801, 100, -950 (40.2) |  |
| `mobs:z2wezens:galmhellingen_4_galmvleermuis` | 797, 140, -931 | 809, 100, -937 (42.2) |  |
| `mobs:z2wezens:galmhellingen_5_splinterling` | 803, 140, -871 | 823, 98, -868 (46.6) |  |
| `mobs:z2wezens:galmhellingen_6_kristalnaaldkruiper` | 820, 140, -1109 | 836, 109, -1105 (35.1) |  |
| `mobs:z2wezens:galmhellingen_7_splinterling` | 817, 140, -998 | 819, 103, -993 (37.4) |  |
| `mobs:z2wezens:galmhellingen_8_kristalnaaldkruiper` | 818, 140, -876 | 833, 103, -879 (40.0) |  |
| `mobs:z2wezens:galmhellingen_9_kristalnaaldkruiper` | 841, 140, -1110 | 855, 118, -1107 (26.2) |  |
| `mobs:z2wezens:knuffelmeer_0_ijspuffer` | 458, 140, -1136 | 461, 116, -1144 (25.5) |  |
| `mobs:z2wezens:knuffelmeer_10_ijzelgeest` | 478, 140, -1067 | 466, 114, -1067 (28.6) |  |
| `mobs:z2wezens:knuffelmeer_11_ijspuffer` | 479, 140, -1048 | 468, 112, -1061 (32.8) |  |
| `mobs:z2wezens:knuffelmeer_12_ijspuffer` | 481, 140, -903 | 472, 122, -925 (29.5) |  |
| `mobs:z2wezens:knuffelmeer_13_versteende_wacht` | 489, 140, -887 | 489, 103, -887 (37.0) |  |
| `mobs:z2wezens:knuffelmeer_14_ijzelgeest` | 510, 140, -1126 | 501, 117, -1129 (24.9) |  |
| `mobs:z2wezens:knuffelmeer_15_versteende_wacht` | 504, 140, -1100 | 500, 111, -1118 (34.4) |  |
| `mobs:z2wezens:knuffelmeer_16_schotsbijter` | 507, 140, -1067 | 472, 110, -1067 (46.1) |  |
| `mobs:z2wezens:knuffelmeer_17_versteende_wacht` | 510, 140, -1047 | 471, 110, -1062 (51.4) |  |
| `mobs:z2wezens:knuffelmeer_18_versteende_wacht` | 515, 140, -897 | 521, 105, -901 (35.7) |  |
| `mobs:z2wezens:knuffelmeer_19_versteende_wacht` | 500, 140, -874 | 493, 104, -877 (36.8) |  |
| `mobs:z2wezens:knuffelmeer_1_schotsbijter` | 462, 140, -1125 | 463, 112, -1129 (28.3) |  |
| `mobs:z2wezens:knuffelmeer_20_ijzelgeest` | 526, 140, -1079 | 511, 108, -1119 (53.4) |  |
| `mobs:z2wezens:knuffelmeer_21_versteende_wacht` | 528, 140, -1044 | 525, 88, -1047 (52.2) |  |
| `mobs:z2wezens:knuffelmeer_22_ijzelgeest` | 539, 140, -1020 | 539, 85, -1020 (55.0) |  |
| `mobs:z2wezens:knuffelmeer_23_ijzelgeest` | 535, 140, -1000 | 535, 85, -1000 (55.0) |  |
| `mobs:z2wezens:knuffelmeer_24_ijzelgeest` | 538, 140, -888 | 538, 106, -896 (34.9) |  |
| `mobs:z2wezens:knuffelmeer_25_versteende_wacht` | 550, 140, -1075 | 574, 106, -1110 (54.4) |  |
| `mobs:z2wezens:knuffelmeer_26_ijspuffer` | 561, 140, -1041 | 579, 91, -1041 (52.2) |  |
| `mobs:z2wezens:knuffelmeer_27_versteende_wacht` | 552, 140, -1031 | 552, 85, -1031 (55.0) |  |
| `mobs:z2wezens:knuffelmeer_28_versteende_wacht` | 552, 140, -995 | 552, 85, -995 (55.0) |  |
| `mobs:z2wezens:knuffelmeer_29_ijzelgeest` | 564, 140, -912 | 549, 104, -921 (40.0) |  |
| `mobs:z2wezens:knuffelmeer_2_versteende_wacht` | 468, 140, -1098 | 468, 116, -1098 (24.0) |  |
| `mobs:z2wezens:knuffelmeer_30_ijzelgeest` | 552, 140, -880 | 542, 104, -890 (38.7) |  |
| `mobs:z2wezens:knuffelmeer_31_schotsbijter` | 584, 140, -1051 | 599, 99, -1056 (43.9) |  |
| `mobs:z2wezens:knuffelmeer_32_ijzelgeest` | 580, 140, -1028 | 599, 97, -1032 (47.2) |  |
| `mobs:z2wezens:knuffelmeer_33_ijspuffer` | 579, 140, -928 | 579, 100, -939 (41.5) |  |
| `mobs:z2wezens:knuffelmeer_34_ijzelgeest` | 578, 140, -902 | 582, 95, -908 (45.6) |  |
| `mobs:z2wezens:knuffelmeer_35_schotsbijter` | 573, 140, -882 | 569, 96, -890 (44.9) |  |
| `mobs:z2wezens:knuffelmeer_36_ijspuffer` | 604, 140, -928 | 596, 98, -939 (44.1) |  |
| `mobs:z2wezens:knuffelmeer_37_ijspuffer` | 608, 140, -909 | 605, 96, -914 (44.4) |  |
| `mobs:z2wezens:knuffelmeer_38_schotsbijter` | 612, 140, -879 | 602, 92, -891 (50.5) |  |
| `mobs:z2wezens:knuffelmeer_3_ijspuffer` | 457, 140, -1074 | 463, 114, -1068 (27.3) |  |
| `mobs:z2wezens:knuffelmeer_4_ijspuffer` | 464, 140, -1051 | 464, 114, -1064 (29.1) |  |
| `mobs:z2wezens:knuffelmeer_6_schotsbijter` | 455, 140, -909 | 461, 129, -929 (23.4) |  |
| `mobs:z2wezens:knuffelmeer_7_versteende_wacht` | 456, 140, -874 | 468, 100, -879 (42.1) |  |
| `mobs:z2wezens:knuffelmeer_8_ijspuffer` | 489, 140, -1117 | 493, 114, -1122 (26.8) |  |
| `mobs:z2wezens:knuffelmeer_9_schotsbijter` | 492, 140, -1101 | 477, 115, -1106 (29.6) |  |
| `mobs:z2wezens:splinterkloof_0_kloofgier` | 488, 140, -1413 | 509, 125, -1400 (28.9) |  |
| `mobs:z2wezens:splinterkloof_16_kristalspin` | 666, 140, -1197 | 662, 123, -1186 (20.6) |  |
| `mobs:z2wezens:splinterkloof_17_mika_mijnwerker` | 718, 140, -1399 | 719, 123, -1399 (17.0) |  |
| `mobs:z2wezens:splinterkloof_18_kloofgier` | 699, 140, -1340 | 694, 125, -1342 (15.9) |  |
| `mobs:z2wezens:splinterkloof_24_kristalspin` | 801, 140, -1301 | 792, 157, -1297 (19.6) |  |
| `mobs:z2wezens:splinterkloof_25_kloofgier` | 819, 140, -1273 | 810, 158, -1266 (21.3) |  |
| `mobs:z2wezens:splinterkloof_27_kloofgier` | 859, 140, -1322 | 859, 176, -1322 (36.0) |  |
| `mobs:z2wezens:splinterkloof_28_mika_boorder` | 905, 140, -1348 | 921, 160, -1348 (25.6) |  |
| `mobs:z2wezens:splinterkloof_29_kristalspin` | 899, 140, -1309 | 914, 160, -1305 (25.3) |  |

### Niet bereikt: overige plekken (12)

| doel | plek | dichtstbijzijnde bereikte cel (afstand) | quests |
|---|---|---|---|
| `plek:npc.NpcPlekken.POI.z2_galmnaald` | 886, 146, -1014 | 898, 125, -1019 (24.7) |  |
| `plek:zone2.npc.Z2NpcPlekken.POI.z2_galmnaald` | 886, 146, -1014 | 898, 125, -1019 (24.7) |  |
| `plek:zone2.wereld.Z2WereldPlekken.ALLE.galmwolf_naald` | 886, 146, -1014 | 898, 125, -1019 (24.7) |  |
| `plek:zone2.wereld.Z2WereldPlekken.ALLE.gondel_berg_kabel` | 886, 159, -1134 | 886, 151, -1134 (8.0) |  |
| `plek:zone2.wereld.Z2WereldPlekken.ALLE.gondel_kabel_berg` | 886, 159, -1134 | 886, 151, -1134 (8.0) |  |
| `plek:zone2.wereld.Z2WereldPlekken.ALLE.ijsval_1` | 544, 66, -1181 | 545, 66, -1192 (11.0) |  |
| `plek:zone2.wereld.Z2WereldPlekken.ALLE.ijsval_2` | 774, 66, -1182 | 770, 67, -1190 (9.0) |  |
| `plek:zone2.wereld.Z2WereldPlekken.ALLE.ijsval_4` | 884, 66, -1214 | 886, 67, -1210 (4.6) |  |
| `plek:zone2.wereld.Z2WereldPlekken.ALLE.klokkentoren_top` | 704, 126, -1062 | 704, 91, -1062 (35.0) |  |
| `plek:zone2.wereld.Z2WereldPlekken.ALLE.opening_naald` | 750, 145, -1000 | 740, 121, -1004 (26.3) |  |
| `plek:zone2.wereld.Z2WereldPlekken.ALLE.puzzel_lichtkamer_doel` | 854, 182, -1333 | 854, 176, -1333 (6.0) |  |
| `plek:zone2.wereld.Z2WereldPlekken.KABEL_BERG` | 886, 159, -1134 | 886, 151, -1134 (8.0) |  |


## Zone 3 - De Schemerwortel

### Niet bereikt: mob-spawners (55)

| doel | plek | dichtstbijzijnde bereikte cel (afstand) | quests |
|---|---|---|---|
| `mobs:z3wezens:kruinzee_0_grauwe_rups` | 1287, 261, -685 | 1297, 226, -659 (44.7) |  |
| `mobs:z3wezens:kruinzee_14_bijtbloesem` | 1405, 261, -468 | 1411, 253, -480 (15.6) |  |
| `mobs:z3wezens:kruinzee_19_bijtbloesem` | 1430, 261, -460 | 1423, 253, -478 (20.9) |  |
| `mobs:z3wezens:kruinzee_1_grauwe_rups` | 1283, 261, -655 | 1278, 236, -640 (29.6) |  |
| `mobs:z3wezens:kruinzee_29_bijtbloesem` | 1489, 261, -495 | 1481, 255, -513 (20.6) |  |
| `mobs:z3wezens:kruinzee_2_grauwe_rups` | 1288, 261, -475 | 1306, 229, -475 (36.7) |  |
| `mobs:z3wezens:kruinzee_30_bijtbloesem` | 1505, 261, -684 | 1491, 226, -679 (38.0) |  |
| `mobs:z3wezens:kruinzee_31_bijtbloesem` | 1519, 261, -669 | 1500, 225, -665 (40.9) |  |
| `mobs:z3wezens:kruinzee_32_grauwe_rups` | 1510, 261, -641 | 1485, 253, -624 (31.3) |  |
| `mobs:z3wezens:kruinzee_33_bladrover` | 1515, 261, -471 | 1498, 223, -480 (42.6) |  |
| `mobs:z3wezens:kruinzee_3_grauwe_rups` | 1312, 261, -683 | 1326, 226, -684 (37.7) |  |
| `mobs:z3wezens:kruinzee_4_bijtbloesem` | 1311, 261, -664 | 1326, 252, -647 (24.4) |  |
| `mobs:z3wezens:kruinzee_6_bijtbloesem` | 1314, 261, -460 | 1319, 229, -455 (32.8) |  |
| `mobs:z3wezens:kruinzee_7_grauwe_rups` | 1337, 261, -687 | 1345, 247, -663 (28.9) |  |
| `mobs:z3wezens:kruinzee_9_bijtbloesem` | 1363, 261, -693 | 1370, 249, -674 (23.5) |  |
| `mobs:z3wezens:takkenbanen_35_schorsmarter` | 1327, 203, -470 | 1335, 207, -481 (14.2) |  |
| `mobs:z3wezens:takkenbanen_36_schorsmarter` | 1323, 203, -464 | 1312, 214, -462 (15.7) |  |
| `mobs:z3wezens:takkenbanen_37_schorsmarter` | 1337, 203, -454 | 1339, 209, -464 (11.8) |  |
| `mobs:z3wezens:takkenbanen_38_grauwe_wesp` | 1331, 203, -446 | 1336, 217, -442 (15.4) |  |
| `mobs:z3wezens:takkenbanen_39_grauwe_wesp` | 1317, 203, -456 | 1312, 214, -462 (13.5) |  |
| `mobs:z3wezens:takkenbanen_42_grauwe_wesp` | 1265, 203, -622 | 1273, 211, -615 (13.3) |  |
| `mobs:z3wezens:takkenbanen_43_schorsmarter` | 1380, 203, -702 | 1378, 193, -694 (13.0) |  |
| `mobs:z3wezens:takkenbanen_44_schorsmarter` | 1364, 203, -698 | 1368, 192, -694 (12.4) |  |
| `mobs:z3wezens:takkenbanen_45_grauwe_wesp` | 1361, 203, -708 | 1360, 215, -708 (12.0) |  |
| `mobs:z3wezens:takkenbanen_46_grauwe_wesp` | 1377, 203, -712 | 1373, 218, -714 (15.7) |  |
| `mobs:z3wezens:wortelduister_121_mika_schimmelboer` | 1369, 36, -530 | 1369, 23, -530 (13.0) |  |
| `mobs:z3wezens:wortelduister_125_mika_schimmelboer` | 1377, 36, -604 | 1379, 23, -607 (13.5) |  |
| `mobs:z3wezens:wortelduister_134_wortelschim` | 1419, 36, -615 | 1423, 22, -617 (14.7) |  |
| `mobs:z3wezens:wortelvelden_102_rotzwijn` | 1445, 80, -379 | 1445, 63, -375 (17.5) |  |
| `mobs:z3wezens:wortelvelden_103_rotzwijn` | 1465, 80, -783 | 1465, 63, -783 (17.0) |  |
| `mobs:z3wezens:wortelvelden_105_wortelslang` | 1472, 80, -366 | 1472, 63, -366 (17.0) |  |
| `mobs:z3wezens:wortelvelden_108_wortelslang` | 1582, 80, -637 | 1580, 67, -641 (13.7) |  |
| `mobs:z3wezens:wortelvelden_109_mika_schimmelboer` | 1590, 80, -591 | 1590, 65, -591 (15.0) |  |
| `mobs:z3wezens:wortelvelden_110_wortelslang` | 1604, 80, -627 | 1604, 63, -627 (17.0) |  |
| `mobs:z3wezens:wortelvelden_111_rotzwijn` | 1606, 80, -608 | 1607, 63, -609 (17.1) |  |
| `mobs:z3wezens:wortelvelden_112_wortelslang` | 1605, 80, -586 | 1605, 63, -586 (17.0) |  |
| `mobs:z3wezens:wortelvelden_113_mika_schimmelboer` | 1614, 80, -569 | 1614, 63, -569 (17.0) |  |
| `mobs:z3wezens:wortelvelden_114_wortelslang` | 1609, 80, -540 | 1606, 68, -538 (12.5) |  |
| `mobs:z3wezens:wortelvelden_72_mika_schimmelboer` | 1206, 80, -652 | 1206, 63, -652 (17.0) |  |
| `mobs:z3wezens:wortelvelden_73_rotzwijn` | 1205, 80, -640 | 1205, 63, -640 (17.0) |  |
| `mobs:z3wezens:wortelvelden_74_mika_schimmelboer` | 1194, 80, -613 | 1194, 63, -613 (17.0) |  |
| `mobs:z3wezens:wortelvelden_75_mika_schimmelboer` | 1199, 80, -587 | 1201, 67, -585 (13.3) |  |
| `mobs:z3wezens:wortelvelden_76_mika_schimmelboer` | 1198, 80, -559 | 1201, 67, -560 (13.4) |  |
| `mobs:z3wezens:wortelvelden_77_mika_schimmelboer` | 1200, 80, -545 | 1204, 67, -546 (13.6) |  |
| `mobs:z3wezens:wortelvelden_78_mika_schimmelboer` | 1207, 80, -513 | 1211, 66, -514 (14.6) |  |
| `mobs:z3wezens:wortelvelden_79_mika_schimmelboer` | 1218, 80, -630 | 1221, 67, -629 (13.4) |  |
| `mobs:z3wezens:wortelvelden_80_mika_schimmelboer` | 1217, 80, -611 | 1217, 67, -611 (13.0) |  |
| `mobs:z3wezens:wortelvelden_81_rotzwijn` | 1221, 80, -593 | 1221, 68, -593 (12.0) |  |
| `mobs:z3wezens:wortelvelden_82_mika_schimmelboer` | 1226, 80, -517 | 1228, 69, -517 (11.2) |  |
| `mobs:z3wezens:wortelvelden_84_mika_schimmelboer` | 1257, 80, -533 | 1260, 69, -531 (11.6) |  |
| `mobs:z3wezens:wortelvelden_90_wortelslang` | 1347, 80, -388 | 1347, 67, -389 (13.0) |  |
| `mobs:z3wezens:wortelvelden_91_wortelslang` | 1341, 80, -370 | 1341, 63, -370 (17.0) |  |
| `mobs:z3wezens:wortelvelden_93_rotzwijn` | 1362, 80, -390 | 1362, 67, -390 (13.0) |  |
| `mobs:z3wezens:wortelvelden_94_wortelslang` | 1364, 80, -368 | 1364, 63, -368 (17.0) |  |
| `mobs:z3wezens:wortelvelden_98_rotzwijn` | 1434, 80, -784 | 1434, 63, -785 (17.0) |  |

### Niet bereikt: overige plekken (7)

| doel | plek | dichtstbijzijnde bereikte cel (afstand) | quests |
|---|---|---|---|
| `plek:zone3.bazen.Z3BazenPlekken.ADERLOOP_A` | 1294, 29, -564 | 1303, 29, -564 (9.0) |  |
| `plek:zone3.bazen.Z3BazenPlekken.ROOF_ZIT_2` | 1530, 200, -587 | 1526, 199, -588 (4.2) |  |
| `plek:zone3.wereld.Z3WereldPlekken.ALLE.ei` | 1404, 26, -576 | 1406, 21, -574 (5.7) |  |
| `plek:zone3.wereld.Z3WereldPlekken.ALLE.kruinroofvogel_zit_2` | 1530, 200, -587 | 1526, 199, -588 (4.2) |  |
| `plek:zone3.wereld.Z3WereldPlekken.ALLE.piepvel_kooi_3` | 1396, 67, -584 | 1396, 61, -584 (6.0) |  |
| `plek:zone3.wereld.Z3WereldPlekken.ALLE.piepvel_kooi_4` | 1411, 69, -584 | 1411, 61, -584 (8.0) |  |
| `plek:zone3.wezens.Z3WezensPlekken.HOLEN.de_harsreus` | 1409, 218, -595 | 1407, 213, -593 (5.3) |  |


## Zone 4 - De Lampionkust

### Niet bereikt: overige plekken (2)

| doel | plek | dichtstbijzijnde bereikte cel (afstand) | quests |
|---|---|---|---|
| `plek:zone3.wereld.Z3WereldPlekken.ALLE.landmark_hoogvads_richting` | 2104, 260, -576 | 2132, 72, -585 (190.3) |  |
| `plek:zone4.wereld.Z4WereldPlekken.ALLE.wrak_oorlogsvlieger` | 2154, 78, -700 | 2154, 67, -700 (11.0) |  |


## Zone 5 - Hoogvads

### Niet bereikt: overige plekken (2)

| doel | plek | dichtstbijzijnde bereikte cel (afstand) | quests |
|---|---|---|---|
| `plek:zone5.wereld.Z5WereldPlekken.ALLE.kathedraal_roos_noord` | 2619, 225, -478 | 2619, 225, -485 (7.0) |  |
| `plek:zone5.wereld.Z5WereldPlekken.ALLE.kathedraal_roos_zuid` | 2619, 225, -562 | 2619, 225, -555 (7.0) |  |


## Zone 6 - Het Zijdeland

### Niet bereikt: zijquest-doelen (1)

| doel | plek | dichtstbijzijnde bereikte cel (afstand) | quests |
|---|---|---|---|
| `poi:z6_grot_schat` | 3848, 50, -528 | 3848, 50, -538 (10.0) | z6_s12_zoutgrot |

### Niet bereikt: geheimen/verzamelplekken (2)

| doel | plek | dichtstbijzijnde bereikte cel (afstand) | quests |
|---|---|---|---|
| `plek:zone6.verhaal.Z6VerhaalPlekken.POI.z6_grot_schat` | 3848, 50, -528 | 3848, 50, -538 (10.0) |  |
| `plek:zone6.wereld.Z6WereldPlekken.ALLE.zoutgrot_schat` | 3848, 50, -528 | 3848, 50, -538 (10.0) |  |


## Baasarena's: binnen bereikt, en is er een uitgang?

| arena | deur | binnen (bereikt) | cellen zonder weg terug | Questlopen-plek terug? |
|---|---|---|---|---|
| `bazen:koning_klodder` | 374, 69, 90 | 3209 (3209) | 0 | ja |
| `bazen:opperzwam` | 330, 152, -470 | 1539 (1482) | **57** bv. (311, 176, -504) | ja |
| `z2bazen:galmwolf` | 850, 115, -974 | 3209 (3137) | **72** bv. (866, 127, -979) | ja |
| `z2bazen:spiegelzucht` | 854, 176, -1371 | 3319 (1870) | **1449** bv. (832, 185, -1397) | ja |
| `z3bazen:kruinroofvogel` | 1288, 229, -549 | 4609 (1507) | **3091** bv. (1237, 201, -530) | ja |
| `z3bazen:taleggio` | 1609, 160, -782 | 983 (704) | **276** bv. (1601, 162, -818) | ja |
| `z3bazen:brokkel` | 1609, 160, -782 | 792 (689) | **103** bv. (1642, 201, -873) | ja |
| `z3bazen:roquefort` | 1609, 160, -782 | 996 (822) | **197** bv. (1677, 232, -826) | **NEE** (1646, 224, -799) |
| `z3bazen:piepvel` | 1405, 61, -590 | 2014 (770) | **1130** bv. (1389, 75, -581) | ja |
| `z3bazen:wortelwurger` | 1327, 31, -576 | 1345 (1009) | **336** bv. (1399, 21, -454) | ja |
| `z3bazen:nestwachter` | 1327, 31, -576 | 1253 (1009) | **244** bv. (1478, 34, -497) | ja |
| `z3bazen:bleu` | 1327, 31, -576 | 1529 (1257) | **272** bv. (1495, 34, -565) | ja |
| `z3bazen:vahoegnir` | 1396, 293, -587 | 2690 (2353) | **302** bv. (1382, 303, -576) | ja |
| `z3imker:mika_imker` | 1457, 157, -565 | 509 (509) | **199** bv. (1450, 155, -577) | ja |
| `z4bazen:zeeslurf` | 2185, 65, -585 | 3009 (2800) | **203** bv. (2162, 48, -565) | ja |
| `z4bazen:vliegervanger` | 2165, 67, -700 | 431 (363) | **6** bv. (2154, 79, -704) | ja |
| `z5wezens:zilverstaart` | 2587, 167, -520 | 1538 (797) | **741** bv. (2586, 172, -520) | ja |
| `z5wezens:gorgonzola` | 2780, 166, -520 | 503 (503) | 0 | ja |
| `z5wezens:donderbuik` | 2951, 207, -569 | 1843 (1259) | **556** bv. (2955, 197, -567) | ja |
| `z6wezens:wormelien` | 3640, 71, -582 | 2179 (2177) | 0 | ja |
| `z6wezens:zoutkolos` | 3995, 69, -586 | 1543 (1507) | 0 | ja |
| `npc:kampvijver` | -360, 87, 1061 | 2130 (2130) | 0 | ja |

## Luchtschepen: kun je instappen? (bereikte cel binnen ~4 blokken van het schip)

Boarding geometry belongs to slice w-schepen; this only reports.

| lijn | haven | pier (bereikt?) | dichtste cel bij het schip | afstand oog-romp |
|---|---|---|---|---|
| kamp_z1 | `haven_kamp_z1_vertrek` | -229, 87, 1008 (ja) | (-228, 85, 1011) | 2.0 |
| kamp_z1 | `haven_z1_kamp_vertrek` | 30, 65, 470 (ja) | (13, 63, 467) | 0.4 |
| z1_z2 | `haven_z1_z2_vertrek` | 150, 127, -657 (ja) | (148, 127, -657) | 3.0 |
| z1_z2 | `haven_z2_z1_vertrek` | 704, 91, -880 (ja) | (701, 91, -880) | 3.0 |
| z2_z3 | `haven_z2_z3_vertrek` | 984, 117, -856 (ja) | (984, 117, -859) | 3.0 |
| z2_z3 | `haven_z3_z2_vertrek` | 1186, 167, -602 (ja) | (1185, 165, -604) | 2.0 |
| z3_z4 | `haven_z3_z4_vertrek` | 1622, 167, -485 (ja) | (1623, 165, -485) | 2.0 |
| z3_z4 | `haven_z4_z3_vertrek` | 1791, 71, -470 (ja) | (1791, 71, -470) | 3.0 |
| z4_z5 | `haven_z4_z5_vertrek` | 2524, 73, -565 (ja) | (2524, 73, -567) | 3.0 |
| z4_z5 | `haven_z5_z4_vertrek` | 2630, 217, -352 (ja) | (2628, 217, -352) | 3.0 |
| z5_z6 | `haven_z5_z6_vertrek` | 2966, 210, -520 (ja) | (2966, 210, -521) | 3.0 |
| z5_z6 | `haven_z6_z5_vertrek` | 3398, 142, -740 (ja) | (3398, 142, -740) | 3.0 |

## Verbindingen (links) in de graaf

- luchtschip kamp_z1: (-229, 87, 1008) <-> (30, 65, 470): ok (van-kant bereikt)
- luchtschip z1_z2: (150, 127, -657) <-> (704, 91, -880): ok (van-kant bereikt)
- luchtschip z2_z3: (984, 117, -856) <-> (1186, 167, -602): ok (van-kant bereikt)
- luchtschip z3_z4: (1622, 167, -485) <-> (1791, 71, -470): ok (van-kant bereikt)
- luchtschip z4_z5: (2524, 73, -565) <-> (2630, 217, -352): ok (van-kant bereikt)
- luchtschip z5_z6: (2966, 210, -520) <-> (3398, 142, -740): ok (van-kant bereikt)
- lift lift_oost: (50, 64, -42) <-> (52, 151, -42): ok (van-kant bereikt)
- lift lift_tempel: (0, 64, -18) <-> (0, 171, -20): ok (van-kant bereikt)
- lift lift_west: (-50, 64, -33) <-> (-52, 141, -33): ok (van-kant bereikt)
- lift honinglift_kruin: (1316, 167, -534) <-> (1316, 231, -540): ok (van-kant bereikt)
- lift honinglift: (1509, 71, -517) <-> (1509, 167, -523): ok (van-kant bereikt)
- lift lift_cafe: (1343, 155, -587) <-> (1340, 167, -588): ok (van-kant bereikt)
- lift lift_kapel: (1395, 152, -638) <-> (1394, 167, -641): ok (van-kant bereikt)
- lift lift_raattros: (1461, 157, -566) <-> (1464, 167, -566): ok (van-kant bereikt)
- lift schorslift_1: (1403, 81, -585) <-> (1403, 167, -585): ok (van-kant bereikt)
- lift schorslift_2: (1412, 167, -576) <-> (1412, 233, -576): ok (van-kant bereikt)
- windkolom hangnest: (1463, 203, -481) -> (1459, 226, -487): ok (van-kant bereikt)
- windkolom honingtak: (1333, 75, -609) -> (1346, 167, -603): ok (van-kant bereikt)
- windkolom lichtkever_kruin: (1443, 261, -600) -> (1437, 285, -596): ok (van-kant bereikt)
- windkolom mosvoet_kruin: (1378, 233, -648) -> (1380, 257, -641): ok (van-kant bereikt)
- windkolom paddenbloesem_tak: (1342, 209, -482) -> (1346, 227, -488): ok (van-kant bereikt)
- windkolom spiraal_kruin: (1378, 190, -680) -> (1379, 228, -673): ok (van-kant bereikt)
- windkolom werf_tak: (1295, 213, -619) -> (1302, 223, -617): ok (van-kant bereikt)
- lift z5w_lift_noordvin: (2610, 154, -429) <-> (2609, 217, -432): ok (van-kant bereikt)
- lift z5w_lift_staart: (2952, 212, -520) <-> (2896, 205, -522): ok (van-kant bereikt)
- lift z5w_lift_zuidvin: (2610, 154, -611) <-> (2609, 210, -608): ok (van-kant bereikt)
- gondel z2: (734, 101, -1105) <-> (886, 151, -1132): ok (van-kant bereikt)
- lift z3 lift_cafe: (1343, 155, -587) <-> (1340, 167, -588): ok (van-kant bereikt)
- lift z3 lift_kapel: (1395, 152, -638) <-> (1394, 167, -641): ok (van-kant bereikt)
- lift z3 lift_raattros: (1461, 157, -566) <-> (1464, 167, -566): ok (van-kant bereikt)
- lift z3 honinglift: (1509, 71, -517) <-> (1509, 167, -523): ok (van-kant bereikt)
- lift z3 honinglift_kruin: (1316, 167, -534) <-> (1316, 231, -540): ok (van-kant bereikt)
- lift z3 schorslift_1: (1403, 81, -585) <-> (1403, 167, -585): ok (van-kant bereikt)
- lift z3 schorslift_2: (1412, 167, -576) <-> (1412, 233, -576): ok (van-kant bereikt)
- luik z5 buik_in: (2732, 219, -541) -> (2711, 166, -520): ok (van-kant bereikt)
- luik z5 buik_uit: (2708, 166, -524) -> (2731, 219, -539): ok (van-kant bereikt)
- luik z5 nest_in: (2612, 217, -444) -> (2594, 188, -427): ok (van-kant bereikt)
- luik z5 nest_uit: (2591, 188, -430) -> (2613, 217, -445): ok (van-kant bereikt)
- luik z5 zuidvin_af: (2609, 210, -608) -> (2610, 154, -611): ok (van-kant bereikt)
- luik z5 zuidvin_op: (2612, 154, -611) -> (2609, 211, -608): ok (van-kant NIET bereikt)
- luik z5 noordvin_af: (2609, 217, -432) -> (2610, 154, -429): ok (van-kant bereikt)
- luik z5 noordvin_op: (2612, 154, -429) -> (2609, 218, -432): ok (van-kant NIET bereikt)
- luik z5 staart_af: (2896, 205, -522) -> (2952, 212, -520): ok (van-kant bereikt)
- luik z5 staart_op: (2954, 212, -520) -> (2896, 206, -522): ok (van-kant bereikt)
- luik z5 wolkwaldroom_uit: (2586, 167, -520) -> (2558, 224, -520): ok (van-kant bereikt)
- luik z6 in: (3985, 70, -950) -> (3943, 30, -971): ok (van-kant bereikt)
- luik z6 uit: (3940, 30, -971) -> (3985, 70, -950): ok (van-kant bereikt)
- luik z6 uit: (4061, 30, -971) -> (3985, 70, -950): ok (van-kant NIET bereikt)
- raid z6 in: (4123, 85, -689) -> (4148, 4, -688): ok (van-kant bereikt)
- raid z6 uit: (4120, 4, -688) -> (4122, 85, -689): ok (van-kant bereikt)
- verhaal z5 spuitgat->wolkwaldroom: (2552, 223, -520) -> (2587, 167, -520): ok (van-kant bereikt)

## Eenrichtingsgebieden (bereikt, maar geen weg terug naar een zone-ingang; >= 6 kolommen)

134 groepen. De grootste 60:

| plek (laagste) | kolommen | y | water | onder de kaart |
|---|---|---|---|---|
| -388, 99, 189 | 1700 | 99..109 |  |  |
| 2650, 132, -328 | 1065 | 132..140 |  |  |
| 2657, 131, -714 | 889 | 131..140 |  |  |
| 1531, 202, -575 | 724 | 202..214 |  |  |
| 3672, 78, -804 | 678 | 78..86 |  |  |
| 879, 79, -823 | 604 | 79..119 |  |  |
| -423, 106, 176 | 579 | 106..112 |  |  |
| 1302, 208, -642 | 398 | 208..218 |  |  |
| -439, 98, 161 | 354 | 98..104 |  |  |
| 1442, 153, -575 | 342 | 153..163 |  |  |
| -361, 97, 103 | 329 | 97..102 |  |  |
| 432, 75, -614 | 290 | 75..111 |  |  |
| -326, 90, 56 | 245 | 90..94 |  |  |
| -368, 96, 156 | 185 | 96..100 |  |  |
| 528, 73, -604 | 170 | 73..109 |  |  |
| -351, 91, 53 | 160 | 91..94 |  |  |
| 1, 174, -54 | 129 | 174..175 |  |  |
| 1404, 209, -705 | 124 | 209..215 |  |  |
| -12, 174, -46 | 123 | 174..175 |  |  |
| 1428, 215, -453 | 123 | 215..218 |  |  |
| 613, 170, -1360 | 96 | 170..170 |  |  |
| 394, 122, -599 | 84 | 122..138 |  |  |
| 274, 74, -603 | 81 | 74..84 |  |  |
| 239, 83, -610 | 78 | 83..118 |  |  |
| 2589, 180, -401 | 77 | 180..187 |  |  |
| 1337, 81, -644 | 67 | 81..84 |  |  |
| 1280, 215, -648 | 66 | 215..220 |  |  |
| 1402, 204, -610 | 66 | 204..210 |  |  |
| 393, 112, -601 | 52 | 112..137 |  |  |
| 720, 94, -1041 | 41 | 94..96 |  |  |
| 2606, 225, -568 | 37 | 225..225 |  |  |
| 3834, 77, -794 | 37 | 77..82 |  |  |
| 38, 73, -652 | 36 | 73..97 |  |  |
| 3518, 87, -992 | 35 | 87..90 |  |  |
| 830, 191, -1404 | 34 | 191..199 |  |  |
| 847, 191, -1421 | 34 | 191..199 |  |  |
| 878, 191, -1404 | 34 | 191..199 |  |  |
| 47, 73, -655 | 33 | 73..98 |  |  |
| 1375, 167, -603 | 32 | 167..168 |  |  |
| 395, 87, -605 | 31 | 87..125 |  |  |
| 21, 81, -649 | 28 | 81..100 |  |  |
| 3481, 109, -691 | 28 | 109..114 |  |  |
| 23, 82, -650 | 27 | 82..99 |  |  |
| 395, 106, -602 | 27 | 106..132 |  |  |
| 38, 80, -651 | 26 | 80..98 |  |  |
| 3457, 71, -998 | 26 | 71..75 |  |  |
| 22, 89, -648 | 25 | 89..101 |  |  |
| 51, 69, -658 | 25 | 69..102 |  |  |
| 162, 86, -664 | 25 | 86..108 |  |  |
| 394, 94, -604 | 25 | 94..122 |  |  |
| 395, 100, -603 | 25 | 100..126 |  |  |
| 1472, 266, -589 | 25 | 266..268 |  |  |
| 9, 81, -640 | 23 | 81..101 |  |  |
| 22, 93, -647 | 22 | 93..104 |  |  |
| 1401, 159, -611 | 22 | 159..165 |  |  |
| 1689, 211, -817 | 22 | 211..218 |  |  |
| 1691, 218, -810 | 22 | 218..219 |  |  |
| 465, 97, -401 | 21 | 97..98 |  |  |
| 642, 95, -1060 | 21 | 95..98 |  |  |
| 700, 95, -1118 | 21 | 95..98 |  |  |

## Deuren

- ok (beide kanten open en bereikt): 196
- ijzeren (nep/op slot) deuren: 33
- deur tegen een muur / zonder binnenkant (houten deur, nep): 0
- deur bereikt maar de binnenkant niet: 0
- deur naast een bereikte straat maar zelf niet bereikbaar: 0


## Lantaarns/kettingen op looplijnen (75)

Blocking deco standing on a walked floor (2+ walked neighbours at that height) or hanging at head height over a walked floor.
per zone: zone 1: 38, zone 2: 20, zone 3: 6, zone 5: 10, zone 6: 1

- -1, 64, -26 `lantern` staat
- -19, 175, -51 `lantern` staat
- -13, 175, -26 `lantern` staat
- -20, 64, -68 `lantern` staat
- -18, 64, -68 `lantern` staat
- -105, 64, -68 `lantern` staat
- -103, 64, -68 `lantern` staat
- -88, 64, 50 `lantern` staat
- -88, 64, 52 `lantern` staat
- -277, 66, -5 `lantern` staat
- -301, 66, 75 `lantern` staat
- -297, 66, 76 `lantern` staat
- -293, 66, 77 `lantern` staat
- -304, 66, 79 `lantern` staat
- -381, 66, 1148 `lantern` staat
- -373, 105, 183 `iron_chain` staat
- -369, 105, 194 `iron_chain` staat
- -382, 107, 217 `iron_chain` staat
- -375, 107, 223 `iron_chain` staat
- -348, 108, 194 `iron_chain` staat
- -425, 76, -306 `lantern` staat
- -445, 73, 8 `lantern` staat
- -415, 73, 220 `lantern` staat
- 1, 64, -26 `lantern` staat
- 62, 64, -8 `lantern` staat
- 18, 175, -51 `lantern` staat
- 41, 64, -85 `lantern` staat
- 16, 64, -68 `lantern` staat
- 18, 64, -68 `lantern` staat
- 658, 67, -1279 `lantern` staat
- 690, 67, -1279 `lantern` staat
- 656, 67, -1270 `lantern` staat
- 692, 67, -1270 `lantern` staat
- 658, 67, -1261 `lantern` staat
- 690, 67, -1261 `lantern` staat
- 665, 67, -1254 `lantern` staat
- 683, 67, -1254 `lantern` staat
- 674, 67, -1252 `lantern` staat
- 674, 67, -1288 `lantern` staat
- 665, 67, -1286 `lantern` staat
- 683, 67, -1286 `lantern` staat
- 715, 92, -919 `elftocht_lampion` staat
- 740, 111, -1110 `iron_chain` staat
- 741, 111, -1110 `iron_chain` staat
- 739, 111, -1109 `iron_chain` staat
- 740, 111, -1107 `iron_chain` staat
- 739, 111, -1106 `iron_chain` staat
- 746, 120, -1190 `iron_chain` staat
- 769, 133, -1252 `elftocht_lampion` hangt
- 64, 64, -8 `lantern` staat
- 103, 64, -80 `lantern` staat
- 105, 64, -80 `lantern` staat
- 86, 64, 50 `lantern` staat
- 86, 64, 52 `lantern` staat
- 70, 65, 467 `lantern` staat
- 70, 65, 473 `lantern` staat
- 1337, 156, -590 `iron_chain` hangt
- 1407, 124, -603 `lantern` staat
- 1400, 97, -550 `lantern` staat
- 1400, 115, -550 `lantern` staat
- 1400, 151, -550 `lantern` staat
- 1469, 232, -636 `lantern` staat
- 143, 127, -611 `lantern` staat
- 157, 127, -611 `lantern` staat
- 2608, 226, -556 `lantern` staat
- 2762, 226, -510 `iron_chain` staat
- 2762, 226, -509 `iron_chain` staat
- 2769, 226, -530 `iron_chain` staat
- 2764, 224, -536 `lantern` staat
- 2829, 198, -465 `iron_chain` staat
- 2831, 198, -465 `iron_chain` staat
- 2874, 198, -554 `iron_chain` staat
- 2879, 197, -555 `iron_chain` staat
- 2875, 197, -555 `iron_chain` staat
- 3487, 107, -674 `messing_lantaarn` staat

## Spookrelingen: gouden kaarsenrand (geen botsing) op een looprand (0)

