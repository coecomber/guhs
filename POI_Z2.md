# POI_Z2 - coordinates of zone 2 "De Splinterpieken" (z2-wereld slice)

**v1, GENERATED** by `tools/vadsrijk/maak_kaart.py` (zone-2 part `tools/vadsrijk/zone2/`) from the real map; the x/z/y of the plan (v0) are kept. Do not edit by hand; requests go to `../POI_VERZOEKEN_Z2.md`. Java: `nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken` (`plek("id")`, `GEBIEDEN`, `MOBZONES`, `KAMPVUREN`, `DEUREN`).

All positions are **stand positions** (the air block a player/NPC stands in) in `guhs:vadsrijk`, unless noted (`doos` = box corners, door `min/max` = the door's own blocks, `*_straal` = radius in x). Cloud sea level 62; Kandijkom floor stand y 91; Spiegelburcht floor stand y 176. Dev: `/guhs vadsrijk z2wereld plek <id>` teleports there.

## Areas (kern.Gebieden, first match wins)

| id | box (minX, minZ)..(maxX, maxZ) | levels | town |
|---|---|---|---|
| `z2_spiegelburcht` | (814,-1431)..(894,-1274) | 28-30 | no |
| `z2_kandijkom_dok` | (664,-973)..(744,-856) | 20-30 | yes |
| `z2_kandijkom` | (620,-1140)..(788,-974) | 20-30 | yes |
| `z2_splinterkloof` | (444,-1431)..(964,-1161) | 26-29 | no |
| `z2_galmhellingen` | (789,-1160)..(964,-856) | 20-23 | no |
| `z2_knuffelmeer` | (444,-1160)..(619,-856) | 23-26 | no |
| `z2_ring` | (404,-1476)..(1004,-806) | 20-30 | no |

## Mob zones (boxes for the wezens slice; `Z2WereldPlekken.MOBZONES`)

| id | box | levels |
|---|---|---|
| `mob_galm_voet` | (790,-1056)..(954,-856) | 20-21 |
| `mob_galm_top` | (790,-1160)..(954,-1057) | 22-23 |
| `mob_meer_oever` | (454,-986)..(618,-856) | 23-24 |
| `mob_meer_ijs` | (478,-1104)..(586,-987) | 24-25 |
| `mob_meer_noord` | (454,-1160)..(618,-1105) | 25-26 |
| `mob_kloof_zuid` | (468,-1184)..(940,-1161) | 26-27 |
| `mob_kloof_bodem` | (468,-1221)..(940,-1185) | 27-28 |
| `mob_kloof_noord` | (468,-1431)..(813,-1222) | 28-29 |
| `mob_burcht` | (835,-1418)..(873,-1290) | 28-30 |

## Moving door

`spiegelburcht` (WereldDeuren id): blocks [851, 176, -1290]..[857, 184, -1290] (wings slide along x), stand in front [854, 176, -1285], hidden copy at y 8. `WereldDeuren.open("spiegelburcht", player)` / `open(level, id)` / `sluit(level, id)`.

## Travel (airship dock, gondola)

| id | x, y, z |
|---|---|
| `dok_bankje` | 700, 91, -940 |
| `dok_bel` | 708, 91, -888 |
| `dok_eind` | 704, 91, -882 |
| `dok_kapitein` | 700, 91, -896 |
| `dok_kiosk` | 713, 91, -922 |
| `dok_landing` | 704, 91, -900 |
| `dok_welkom` | 707, 91, -912 |
| `gondel_berg` | 886, 151, -1132 |
| `gondel_berg_kabel` | 886, 159, -1134 |
| `gondel_berg_knop` | 889, 151, -1131 |
| `gondel_dal` | 734, 101, -1105 |
| `gondel_dal_kabel` | 734, 109, -1107 |
| `gondel_dal_knop` | 737, 101, -1104 |
| `gondel_kabel_berg` | 886, 159, -1134 |
| `gondel_kabel_dal` | 734, 109, -1107 |
| `npc_gondel` | 732, 101, -1104 |

## Kandijkom: plaza, NPC spots, secrets in town

| id | x, y, z |
|---|---|
| `cafe_terras` | 680, 91, -1042 |
| `geheim_chocokelder` | 678, 85, -1033 |
| `kristalkapel_altaar` | 674, 101, -1111 |
| `npc_bank` | 727, 91, -1030 |
| `npc_burgemeester` | 704, 91, -1056 |
| `npc_cafe` | 680, 91, -1030 |
| `npc_ijsvisser` | 520, 88, -1048 |
| `npc_juwelier` | 681, 91, -1083 |
| `npc_kristalkapel` | 674, 101, -1109 |
| `npc_schaatsguh` | 713, 91, -1041 |
| `npc_smid` | 727, 91, -1083 |
| `npc_stal` | 724, 91, -953 |
| `npc_winkel_kandij` | 745, 96, -1078 |
| `npc_winkel_wantjes` | 663, 96, -1078 |
| `plein` | 704, 91, -1068 |
| `questbord` | 716, 91, -1075 |
| `schaatsvijver` | 704, 91, -1041 |
| `sneeuwguhspar` | 688, 91, -1061 |

## Kandijkom buildings (deur = stand outside the door, binnen = interior centre, npc = behind the counter, doos = box)

| id | x, y, z |
|---|---|
| `gebouw.bank.binnen` | 727, 91, -1033 |
| `gebouw.bank.deur` | 727, 91, -1040 |
| `gebouw.bank.doos.1` | 722, 90, -1038 |
| `gebouw.bank.doos.2` | 732, 97, -1028 |
| `gebouw.bank.npc` | 727, 91, -1030 |
| `gebouw.bank.toonbank` | 727, 91, -1032 |
| `gebouw.cafe.binnen` | 680, 91, -1033 |
| `gebouw.cafe.deur` | 680, 91, -1040 |
| `gebouw.cafe.doos.1` | 674, 90, -1038 |
| `gebouw.cafe.doos.2` | 686, 97, -1028 |
| `gebouw.cafe.npc` | 680, 91, -1030 |
| `gebouw.cafe.toonbank` | 680, 91, -1032 |
| `gebouw.huis_1.binnen` | 726, 96, -1094 |
| `gebouw.huis_1.deur` | 726, 96, -1088 |
| `gebouw.huis_1.doos.1` | 721, 95, -1098 |
| `gebouw.huis_1.doos.2` | 731, 101, -1090 |
| `gebouw.huis_10.binnen` | 674, 101, -1005 |
| `gebouw.huis_10.deur` | 674, 101, -1011 |
| `gebouw.huis_10.doos.1` | 669, 100, -1009 |
| `gebouw.huis_10.doos.2` | 679, 106, -1001 |
| `gebouw.huis_11.binnen` | 755, 101, -1026 |
| `gebouw.huis_11.deur` | 748, 101, -1026 |
| `gebouw.huis_11.doos.1` | 750, 100, -1030 |
| `gebouw.huis_11.doos.2` | 760, 106, -1022 |
| `gebouw.huis_12.binnen` | 734, 101, -1005 |
| `gebouw.huis_12.deur` | 734, 101, -1011 |
| `gebouw.huis_12.doos.1` | 729, 100, -1009 |
| `gebouw.huis_12.doos.2` | 739, 106, -1001 |
| `gebouw.huis_2.binnen` | 682, 96, -1094 |
| `gebouw.huis_2.deur` | 682, 96, -1088 |
| `gebouw.huis_2.doos.1` | 677, 95, -1098 |
| `gebouw.huis_2.doos.2` | 687, 101, -1090 |
| `gebouw.huis_3.binnen` | 666, 96, -1034 |
| `gebouw.huis_3.deur` | 673, 96, -1034 |
| `gebouw.huis_3.doos.1` | 661, 95, -1038 |
| `gebouw.huis_3.doos.2` | 671, 101, -1030 |
| `gebouw.huis_4.binnen` | 682, 96, -1018 |
| `gebouw.huis_4.deur` | 682, 96, -1024 |
| `gebouw.huis_4.doos.1` | 677, 95, -1022 |
| `gebouw.huis_4.doos.2` | 687, 101, -1014 |
| `gebouw.huis_5.binnen` | 742, 96, -1034 |
| `gebouw.huis_5.deur` | 735, 96, -1034 |
| `gebouw.huis_5.doos.1` | 737, 95, -1038 |
| `gebouw.huis_5.doos.2` | 747, 101, -1030 |
| `gebouw.huis_6.binnen` | 726, 96, -1018 |
| `gebouw.huis_6.deur` | 726, 96, -1024 |
| `gebouw.huis_6.doos.1` | 721, 95, -1022 |
| `gebouw.huis_6.doos.2` | 731, 101, -1014 |
| `gebouw.huis_7.binnen` | 755, 101, -1086 |
| `gebouw.huis_7.deur` | 748, 101, -1086 |
| `gebouw.huis_7.doos.1` | 750, 100, -1090 |
| `gebouw.huis_7.doos.2` | 760, 106, -1082 |
| `gebouw.huis_8.binnen` | 653, 101, -1086 |
| `gebouw.huis_8.deur` | 660, 101, -1086 |
| `gebouw.huis_8.doos.1` | 648, 100, -1090 |
| `gebouw.huis_8.doos.2` | 658, 106, -1082 |
| `gebouw.huis_9.binnen` | 653, 101, -1026 |
| `gebouw.huis_9.deur` | 660, 101, -1026 |
| `gebouw.huis_9.doos.1` | 648, 100, -1030 |
| `gebouw.huis_9.doos.2` | 658, 106, -1022 |
| `gebouw.juwelier.binnen` | 681, 91, -1080 |
| `gebouw.juwelier.deur` | 681, 91, -1073 |
| `gebouw.juwelier.doos.1` | 676, 90, -1085 |
| `gebouw.juwelier.doos.2` | 686, 97, -1075 |
| `gebouw.juwelier.npc` | 681, 91, -1083 |
| `gebouw.juwelier.toonbank` | 681, 91, -1081 |
| `gebouw.kristalkapel.altaar` | 674, 101, -1111 |
| `gebouw.kristalkapel.binnen` | 674, 101, -1107 |
| `gebouw.kristalkapel.deur` | 674, 101, -1100 |
| `gebouw.kristalkapel.doos.1` | 668, 100, -1112 |
| `gebouw.kristalkapel.doos.2` | 680, 111, -1102 |
| `gebouw.kristalkapel.npc` | 674, 101, -1109 |
| `gebouw.smid.binnen` | 727, 91, -1080 |
| `gebouw.smid.deur` | 727, 91, -1073 |
| `gebouw.smid.doos.1` | 722, 90, -1085 |
| `gebouw.smid.doos.2` | 732, 97, -1075 |
| `gebouw.smid.npc` | 727, 91, -1083 |
| `gebouw.smid.toonbank` | 727, 91, -1081 |
| `gebouw.stal.binnen` | 730, 91, -952 |
| `gebouw.stal.deur` | 723, 91, -952 |
| `gebouw.stal.doos.1` | 725, 90, -956 |
| `gebouw.stal.doos.2` | 735, 101, -948 |
| `gebouw.stal.npc` | 724, 91, -953 |
| `gebouw.stal.wei` | 730, 91, -940 |
| `gebouw.winkel_kandij.binnen` | 742, 96, -1078 |
| `gebouw.winkel_kandij.deur` | 735, 96, -1078 |
| `gebouw.winkel_kandij.doos.1` | 737, 95, -1082 |
| `gebouw.winkel_kandij.doos.2` | 747, 102, -1074 |
| `gebouw.winkel_kandij.npc` | 745, 96, -1078 |
| `gebouw.winkel_kandij.toonbank` | 743, 96, -1078 |
| `gebouw.winkel_wantjes.binnen` | 666, 96, -1078 |
| `gebouw.winkel_wantjes.deur` | 673, 96, -1078 |
| `gebouw.winkel_wantjes.doos.1` | 661, 95, -1082 |
| `gebouw.winkel_wantjes.doos.2` | 671, 102, -1074 |
| `gebouw.winkel_wantjes.npc` | 663, 96, -1078 |
| `gebouw.winkel_wantjes.toonbank` | 665, 96, -1078 |

## Arenas and bosses

| id | x, y, z |
|---|---|
| `arena_boormeester` | 674, 66, -1270 |
| `arena_boormeester_deur` | 674, 66, -1215 |
| `arena_boormeester_straal` | 724, 0, 1344 |
| `arena_galmwolf` | 886, 117, -974 |
| `arena_galmwolf_bord` | 851, 117, -970 |
| `arena_galmwolf_deur` | 850, 115, -974 |
| `arena_spiegelzucht` | 854, 176, -1397 |
| `arena_spiegelzucht_bord` | 858, 176, -1370 |
| `arena_spiegelzucht_deur` | 854, 176, -1371 |
| `arena_spiegelzucht_straal` | 726, 0, 1344 |
| `boormeester_kop_1` | 674, 66, -1258 |
| `boormeester_kop_2` | 664, 66, -1276 |
| `boormeester_kop_3` | 684, 66, -1276 |
| `galmwolf_naald` | 886, 146, -1014 |
| `galmwolf_pilaar_1` | 905, 117, -970 |
| `galmwolf_pilaar_2` | 896, 117, -958 |
| `galmwolf_pilaar_3` | 882, 117, -955 |
| `galmwolf_pilaar_4` | 870, 117, -964 |
| `galmwolf_pilaar_5` | 867, 117, -978 |
| `galmwolf_pilaar_6` | 876, 117, -990 |
| `galmwolf_pilaar_7` | 890, 117, -993 |
| `galmwolf_pilaar_8` | 902, 117, -984 |
| `spiegelzucht_spiegel_1` | 871, 176, -1397 |
| `spiegelzucht_spiegel_2` | 863, 176, -1382 |
| `spiegelzucht_spiegel_3` | 846, 176, -1382 |
| `spiegelzucht_spiegel_4` | 837, 176, -1397 |
| `spiegelzucht_spiegel_5` | 845, 176, -1412 |
| `spiegelzucht_spiegel_6` | 863, 176, -1412 |

## De Spiegelburcht (dungeon): door, rooms, gates (open AIR, bazen fills), puzzle spots

| id | x, y, z |
|---|---|
| `burcht_lichtkamer` | 854, 176, -1324 |
| `burcht_prismazaal` | 854, 176, -1362 |
| `burcht_spiegelgang` | 854, 176, -1343 |
| `burcht_splinter` | 854, 176, -1412 |
| `burcht_trap_voet` | 854, 137, -1217 |
| `burcht_voorhal` | 854, 176, -1302 |
| `burcht_voorplein` | 854, 176, -1267 |
| `deur_spiegelburcht_voor` | 854, 176, -1285 |
| `poort_1` | 854, 176, -1314 |
| `poort_2` | 854, 176, -1334 |
| `poort_3` | 854, 176, -1352 |
| `poort_4` | 854, 176, -1372 |
| `puzzel_lichtkamer_1` | 847, 176, -1328 |
| `puzzel_lichtkamer_2` | 861, 176, -1328 |
| `puzzel_lichtkamer_3` | 847, 176, -1319 |
| `puzzel_lichtkamer_4` | 861, 176, -1319 |
| `puzzel_lichtkamer_5` | 854, 176, -1324 |
| `puzzel_lichtkamer_bron` | 840, 178, -1324 |
| `puzzel_lichtkamer_doel` | 854, 182, -1333 |
| `puzzel_prismazaal_1` | 854, 176, -1362 |
| `puzzel_prismazaal_2` | 846, 176, -1357 |
| `puzzel_prismazaal_3` | 862, 176, -1357 |
| `puzzel_prismazaal_4` | 846, 176, -1367 |
| `puzzel_prismazaal_5` | 862, 176, -1367 |
| `puzzel_spiegelgang_1` | 849, 176, -1338 |
| `puzzel_spiegelgang_2` | 859, 176, -1338 |
| `puzzel_spiegelgang_3` | 849, 176, -1343 |
| `puzzel_spiegelgang_4` | 859, 176, -1343 |
| `puzzel_spiegelgang_5` | 849, 176, -1348 |
| `puzzel_spiegelgang_6` | 859, 176, -1348 |

## Campfires (unlit, kern checkpoints) + ridder spots

| id | x, y, z |
|---|---|
| `kampvuur_galmhellingen_1` | 814, 100, -1046 |
| `kampvuur_galmhellingen_2` | 846, 103, -918 |
| `kampvuur_galmhellingen_3` | 912, 144, -1084 |
| `kampvuur_galmhellingen_4` | 936, 125, -952 |
| `kampvuur_kandijkom_dok_1` | 695, 92, -918 |
| `kampvuur_knuffelmeer_1` | 604, 100, -986 |
| `kampvuur_knuffelmeer_2` | 468, 103, -998 |
| `kampvuur_knuffelmeer_3` | 576, 106, -1114 |
| `kampvuur_knuffelmeer_4` | 528, 92, -948 |
| `kampvuur_spiegelburcht_1` | 866, 160, -1246 |
| `kampvuur_splinterkloof_1` | 508, 114, -1168 |
| `kampvuur_splinterkloof_2` | 832, 123, -1164 |
| `kampvuur_splinterkloof_3` | 712, 115, -1160 |
| `kampvuur_splinterkloof_4` | 608, 65, -1202 |
| `kampvuur_splinterkloof_5` | 568, 131, -1260 |
| `ridder_galmhellingen_1` | 816, 100, -1044 |
| `ridder_galmhellingen_2` | 848, 103, -916 |
| `ridder_galmhellingen_3` | 914, 144, -1082 |
| `ridder_galmhellingen_4` | 938, 125, -950 |
| `ridder_kandijkom_dok_1` | 697, 92, -916 |
| `ridder_knuffelmeer_1` | 606, 100, -984 |
| `ridder_knuffelmeer_2` | 470, 103, -996 |
| `ridder_knuffelmeer_3` | 578, 106, -1112 |
| `ridder_knuffelmeer_4` | 530, 92, -946 |
| `ridder_spiegelburcht_1` | 868, 160, -1244 |
| `ridder_splinterkloof_1` | 510, 114, -1166 |
| `ridder_splinterkloof_2` | 834, 123, -1162 |
| `ridder_splinterkloof_3` | 714, 115, -1158 |
| `ridder_splinterkloof_4` | 610, 65, -1200 |
| `ridder_splinterkloof_5` | 570, 131, -1258 |

## Viewpoints (needle tops, panorama on first visit)

| id | x, y, z |
|---|---|
| `uitkijk_galmkam` | 983, 117, -860 |
| `uitkijk_galmtop` | 928, 195, -1106 |
| `uitkijk_kandijkom_balkon` | 704, 91, -882 |
| `uitkijk_kloofkijk` | 616, 170, -1356 |
| `uitkijk_meerkijk` | 468, 142, -936 |

## De Galmhellingen

| id | x, y, z |
|---|---|
| `galm_lawine_1` | 930, 171, -1134 |
| `galm_lawine_1_voet` | 864, 124, -1084 |
| `galm_lawine_2` | 948, 122, -1036 |
| `galm_lawine_2_voet` | 930, 129, -906 |
| `galm_lawine_3` | 944, 125, -918 |
| `galm_lawine_3_voet` | 870, 115, -874 |
| `galm_naalden_1` | 824, 96, -918 |
| `galm_naalden_2` | 936, 135, -884 |
| `galm_naalden_3` | 832, 104, -1006 |
| `galm_naalden_4` | 940, 124, -1064 |

## Het Bevroren Knuffelmeer (knuffel_ijs = stand on the ice above a frozen plushie)

| id | x, y, z |
|---|---|
| `draak_zadel` | 499, 113, -1148 |
| `draak_zadel_kijk` | 499, 113, -1130 |
| `knuffel_ijs_1` | 554, 85, -1006 |
| `knuffel_ijs_10` | 544, 85, -1026 |
| `knuffel_ijs_2` | 539, 85, -1076 |
| `knuffel_ijs_3` | 504, 85, -1016 |
| `knuffel_ijs_4` | 494, 85, -1076 |
| `knuffel_ijs_5` | 564, 85, -1056 |
| `knuffel_ijs_6` | 509, 85, -991 |
| `knuffel_ijs_7` | 529, 85, -1096 |
| `knuffel_ijs_8` | 574, 85, -1026 |
| `knuffel_ijs_9` | 489, 85, -1041 |
| `meer_eiland` | 518, 88, -1048 |
| `meer_midden` | 532, 85, -1034 |
| `meer_scheur` | 534, 85, -1088 |
| `meer_schotsen` | 554, 85, -978 |

## De Splinterkloof (bridges: stand at each end; stairways: the foot on the ravine floor)

| id | x, y, z |
|---|---|
| `boortoren_1` | 554, 126, -1178 |
| `boortoren_1_top` | 554, 146, -1178 |
| `boortoren_2` | 764, 132, -1228 |
| `boortoren_2_top` | 764, 152, -1228 |
| `boortoren_3` | 908, 144, -1178 |
| `boortoren_3_top` | 908, 164, -1178 |
| `brug_1_noord` | 514, 137, -1220 |
| `brug_1_zuid` | 514, 123, -1180 |
| `brug_2_noord` | 604, 131, -1226 |
| `brug_2_zuid` | 604, 123, -1188 |
| `brug_3_noord` | 854, 137, -1216 |
| `brug_3_zuid` | 854, 129, -1184 |
| `brug_4_noord` | 744, 131, -1219 |
| `brug_4_zuid` | 744, 122, -1184 |
| `brug_5_noord` | 826, 124, -1170 |
| `brug_5_zuid` | 805, 122, -1170 |
| `kloof_bodem_1` | 488, 66, -1197 |
| `kloof_bodem_10` | 812, 65, -1204 |
| `kloof_bodem_11` | 848, 65, -1201 |
| `kloof_bodem_12` | 884, 65, -1198 |
| `kloof_bodem_13` | 920, 66, -1197 |
| `kloof_bodem_2` | 524, 65, -1201 |
| `kloof_bodem_3` | 560, 66, -1205 |
| `kloof_bodem_4` | 596, 65, -1207 |
| `kloof_bodem_5` | 632, 65, -1204 |
| `kloof_bodem_6` | 668, 65, -1201 |
| `kloof_bodem_7` | 704, 66, -1198 |
| `kloof_bodem_8` | 740, 65, -1201 |
| `kloof_bodem_9` | 776, 65, -1204 |
| `kloof_trap_1` | 470, 66, -1186 |
| `kloof_trap_2` | 666, 66, -1189 |
| `kloof_trap_3` | 814, 66, -1195 |
| `kloof_trap_4` | 500, 66, -1211 |

## Secrets

| id | x, y, z |
|---|---|
| `geheim_boorplaats` | 674, 66, -1219 |
| `geheim_burchtdak` | 864, 195, -1337 |
| `geheim_galmgrot` | 940, 121, -1002 |
| `geheim_galmgrot_ingang` | 929, 118, -1002 |
| `geheim_ijsgrot` | 464, 105, -1066 |
| `geheim_ijsgrot_ingang` | 453, 99, -1066 |

## Other

| id | x, y, z |
|---|---|
| `aantal_bomen` | 887, 0, 1344 |
| `aanwijzing.boorgaten` | 868, 131, -1081 |
| `aanwijzing.boorschema` | 660, 104, -1083 |
| `aanwijzing.bouwtekening` | 558, 126, -1174 |
| `aanwijzing.olievlek` | 912, 144, -1174 |
| `aanwijzing.warme_spleet` | 674, 66, -1219 |
| `chocomelkkraam` | 715, 91, -1039 |
| `gehucht_galmdorpje` | 832, 104, -1070 |
| `gehucht_meerkamp` | 592, 101, -962 |
| `gehucht_plateaukamp` | 764, 130, -1256 |
| `haven_z2_z1_bord` | 697, 91, -888 |
| `haven_z2_z1_vertrek` | 704, 91, -900 |
| `haven_z2_z3_bord` | 974, 117, -859 |
| `haven_z2_z3_eind` | 983, 117, -856 |
| `haven_z2_z3_mast` | 983, 129, -851 |
| `haven_z2_z3_plein` | 940, 117, -856 |
| `haven_z2_z3_vertrek` | 980, 117, -856 |
| `ijsval_1` | 544, 66, -1181 |
| `ijsval_2` | 774, 66, -1182 |
| `ijsval_3` | 644, 66, -1216 |
| `ijsval_4` | 884, 66, -1214 |
| `klokkentoren` | 704, 91, -1062 |
| `klokkentoren_top` | 704, 126, -1062 |
| `kristalbos_1` | 854, 105, -884 |
| `kristalbos_10` | 764, 96, -956 |
| `kristalbos_11` | 644, 94, -956 |
| `kristalbos_12` | 724, 120, -1356 |
| `kristalbos_2` | 594, 94, -918 |
| `kristalbos_3` | 644, 128, -1306 |
| `kristalbos_4` | 936, 123, -1036 |
| `kristalbos_5` | 468, 115, -1106 |
| `kristalbos_6` | 784, 152, -1296 |
| `kristalbos_7` | 504, 134, -1296 |
| `kristalbos_8` | 904, 138, -1076 |
| `kristalbos_9` | 554, 132, -1356 |
| `lawine_golf` | 854, 114, -1074 |
| `markt_kramen.1` | 719, 91, -1044 |
| `markt_kramen.2` | 689, 91, -1044 |
| `markt_kramen.3` | 698, 91, -1074 |
| `markt_kramen.4` | 710, 91, -1074 |
| `markt_kramen.5` | 719, 91, -1068 |
| `mika_fort` | 534, 135, -1305 |
| `mika_fort_poort` | 534, 135, -1290 |
| `mika_fort_toren` | 544, 150, -1317 |
| `npcplek.z2_bakker` | 683, 96, -1024 |
| `npcplek.z2_beeldhouwer` | 573, 106, -1113 |
| `npcplek.z2_berggids` | 811, 101, -1045 |
| `npcplek.z2_galmpje` | 696, 91, -1078 |
| `npcplek.z2_gondel_boven` | 884, 162, -1131 |
| `npcplek.z2_kasguh` | 673, 105, -1033 |
| `npcplek.z2_kwarts` | 719, 91, -1074 |
| `npcplek.z2_marktguh_1` | 719, 91, -1044 |
| `npcplek.z2_marktguh_2` | 689, 91, -1044 |
| `npcplek.z2_marktguh_3` | 698, 91, -1074 |
| `npcplek.z2_opa_sneeuwbal` | 682, 91, -1041 |
| `npcplek.z2_pinguguh_1` | 691, 91, -1061 |
| `npcplek.z2_pinguguh_2` | 713, 91, -1047 |
| `npcplek.z2_pinguguh_3` | 714, 91, -1050 |
| `npcplek.z2_poollicht` | 727, 96, -1088 |
| `npcplek.z2_postguh` | 735, 96, -1033 |
| `npcplek.z2_prisma` | 858, 176, -1267 |
| `npcplek.z2_schaatser_1` | 701, 91, -1043 |
| `npcplek.z2_schaatser_2` | 707, 91, -1039 |
| `npcplek.z2_slee_1` | 720, 91, -960 |
| `npcplek.z2_slee_2` | 718, 91, -958 |
| `npcplek.z2_sneeuwbal_1` | 694, 91, -1052 |
| `npcplek.z2_sneeuwbal_2` | 700, 91, -1048 |
| `npcplek.z2_sneeuwpop` | 728, 96, -1024 |
| `npcplek.z2_touwtje` | 857, 129, -1184 |
| `npcplek.z2_uitvinder` | 660, 101, -1085 |
| `npcplek.z2_versteend_ijsje` | 518, 88, -1048 |
| `npcplek.z2_versteend_pluk` | 500, 85, -1026 |
| `npcplek.z2_versteend_snoek` | 554, 85, -978 |
| `npcplek.z2_versteend_zwier` | 532, 85, -1034 |
| `npcplek.z2_vlokje` | 695, 91, -1039 |
| `npcplek.z2_wacht_burcht` | 868, 160, -1244 |
| `npcplek.z2_wacht_dok` | 697, 92, -916 |
| `npcplek.z2_wacht_galm_1` | 816, 100, -1044 |
| `npcplek.z2_wacht_galm_2` | 848, 103, -916 |
| `npcplek.z2_wacht_galm_3` | 914, 144, -1082 |
| `npcplek.z2_wacht_galm_4` | 938, 125, -950 |
| `npcplek.z2_wacht_kloof_1` | 510, 114, -1166 |
| `npcplek.z2_wacht_kloof_2` | 834, 123, -1162 |
| `npcplek.z2_wacht_kloof_3` | 714, 115, -1158 |
| `npcplek.z2_wacht_kloof_4` | 610, 65, -1200 |
| `npcplek.z2_wacht_kloof_5` | 570, 131, -1258 |
| `npcplek.z2_wacht_meer_1` | 606, 100, -984 |
| `npcplek.z2_wacht_meer_2` | 470, 103, -996 |
| `npcplek.z2_wacht_meer_3` | 578, 106, -1112 |
| `npcplek.z2_wacht_meer_4` | 530, 92, -946 |
| `npcplek.z2_wacht_plein_oost` | 718, 95, -1068 |
| `npcplek.z2_wacht_plein_west` | 690, 91, -1068 |
| `npcplek.z2_wapenhandelaar` | 727, 91, -1080 |
| `npcplek.z2_winterslaap` | 683, 96, -1088 |
| `opening_naald` | 750, 145, -1000 |
| `poiplek.z2_brug_1` | 854, 129, -1184 |
| `poiplek.z2_brug_2` | 604, 123, -1188 |
| `poiplek.z2_brug_3` | 514, 123, -1180 |
| `poiplek.z2_lawinepad` | 864, 124, -1084 |
| `poiplek.z2_meer_scheur` | 534, 85, -1088 |
| `poiplek.z2_uitkijk_poollicht` | 616, 170, -1356 |
| `poiplek.z2_warmwaterbron` | 900, 120, -908 |
| `schat.onder_de_brug` | 608, 114, -1192 |
| `schat_onder_de_brug` | 608, 114, -1192 |
| `warmwaterbron` | 900, 120, -908 |
