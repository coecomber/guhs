# POI_Z3 - coordinates of zone 3 "De Schemerwortel" (z3-wereld slice)

**v2 (map, stand spots snapped onto the tiles), GENERATED** by `tools/vadsrijk/zone3/poi3.py` (via `python tools/vadsrijk/maak_kaart.py --zone 3`). Do not edit by hand; requests go to `../POI_VERZOEKEN_Z3.md`. Java: `nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken` (`plek("id")`, `GEBIEDEN` / `gebied(x, y, z)`, `MOBZONES` / `mobzone(x, y, z)`, `PADEN`, `KAMPVUREN`, `WINDKOLOMMEN`, `LIFTEN`, `UITKIJKEN`). Dev: `/guhs vadsrijk z3wereld plek <id>` teleports there.

**Layout (v2, revamp round 20, connected world layout).** Zone 3 = the revamp, moved with the world layout (guhs1100-avontuur/wereldindeling/WERELDINDELING.md) by (-896, -576): trunk axis at (1404, -576), tiles tx 16..26, tz -16..-3 (x 1024..1727, z -1024..-129). Every older zone-3 tile (v1 tx 25..46 / tz -11..10, the revamp's first place tx 29..42 / tz -7..6) is deleted by the map build. Not mirrored: world = rel + (1404, -576). West (towards zone 2, across the Wolkenkloof) a root cape with the Weststeiger (`dok_*`, line B), east (towards zone 4) the Oosttaksteiger (`oosttaksteiger_*`, line C); no water ring any more. 4 layers on one axis: WORTELS y 4-78 (Wortelduister hall under the trunk, the Hart dungeon ring r 112 floor 28, the Wortelpoort on the surface east), STAM y 80-160 (the Schorsstad in the hollow trunk, floor 80; the Pelszaal UNDER the floor, stand 61), TAKKEN y 160-216 (Honingtak = a ring town round the trunk, deck stand 167; 5 big branches with 3 beekeeper villages), KRUIN y 216-312 (3 stepped crown storeys r 130/95/60, tops 232/260/284 at the trunk, the nest on top: floor stand 293). Connections: the inner spiral stair (floor 80 -> nest, landings 80/166/232/260/284/292), the riding spiral outside (ground 82 -> Honingtak 166 -> kruin 1 232), Honinglift A (ground -> Honingtak) and B (Honingtak -> kruin 1), 2 lifts in the hollow trunk, wind columns. The Kaaszeppelinwerf is its own island north-east (centre 1655, -827) with a cable bridge from Honingtak's Werftak.

All positions are **stand positions** (the air block a player/NPC stands in) in `guhs:vadsrijk`, unless noted (`doos` = box corners, `*_kooi`/`piepvel_kooi_*` = the hanging cage block, `wind_*_boven` = landing at the top).

## Areas (with y-ranges; `Z3WereldPlekken.GEBIEDEN`, first match wins)

| id | shape | y | levels | town |
|---|---|---|---|---|
| `z3_zeppelinwerf` | cylinder around (1655,-827), r 0..95 | 60-290 | 38-40 | no |
| `z3_hart` | cylinder around (1404,-576), r 76..150 | 14-52 | 49-50 | no |
| `z3_wortelduister` | cylinder around (1404,-576), r 0..160 | 0-64 | 46-49 | no |
| `z3_schorsstad` | cylinder around (1404,-576), r 0..26 | 55-165 | 40-43 | no |
| `z3_honingtak` | cylinder around (1404,-576), r 0..160 | 145-200 | 30-50 | yes |
| `z3_stamspiraal` | cylinder around (1404,-576), r 26..64 | 75-232 | 40-43 | no |
| `z3_kruinzee` | cylinder around (1404,-576), r 0..200 | 212-320 | 30-34 | no |
| `z3_takkenbanen` | cylinder around (1404,-576), r 0..260 | 140-320 | 34-38 | no |
| `z3_wortelvelden` | cylinder around (1404,-576), r 0..600 | 0-320 | 43-46 | no |

## Mob zones (`Z3WereldPlekken.MOBZONES`; spawn only inside, never within ~5 blocks of `PADEN`)

| id | shape | y | levels |
|---|---|---|---|
| `z3_kruinzee_zeppelin` | cylinder around (1459,-671), r 0..34 | 220-250 | 31-34 |
| `z3_kruinzee_noord` | box (1274,-706)..(1534,-586) | 222-300 | 30-33 |
| `z3_kruinzee_zuid` | box (1274,-566)..(1534,-446) | 222-300 | 30-34 |
| `z3_takkenbanen_west` | box (1244,-626)..(1354,-526) | 170-222 | 34-37 |
| `z3_takkenbanen_noordwest` | box (1344,-736)..(1554,-626) | 170-222 | 35-38 |
| `z3_takkenbanen_zuidwest` | box (1264,-526)..(1444,-416) | 170-222 | 34-37 |
| `z3_schorsstad_ringen` | cylinder around (1404,-576), r 0..24 | 172-228 | 40-43 |
| `z3_wortelvelden_lichtkeverbos` | box (1324,-791)..(1484,-686) | 50-110 | 43-45 |
| `z3_wortelvelden_schimmelboerderij` | box (1189,-656)..(1294,-496) | 50-110 | 44-46 |
| `z3_wortelvelden_paddenbloesemdal` | box (1324,-466)..(1484,-361) | 50-110 | 43-45 |
| `z3_wortelvelden_honingmoeras` | box (1514,-656)..(1619,-536) | 50-110 | 44-46 |
| `z3_wortelduister_gang` | box (1444,-588)..(1554,-560) | 18-72 | 46-47 |
| `z3_wortelduister_grotten` | cylinder around (1404,-576), r 24..86 | 14-58 | 47-49 |

## Wind columns (`WINDKOLOMMEN`: x, z, y0 -> y1 = landing stand y, radius, landing x, z)

| id | x, z | y | r | landing |
|---|---|---|---|---|
| `werf_tak` | 1295, -619 | 213 -> 223 | 3.0 | 1302, -617 |
| `hangnest` | 1463, -481 | 203 -> 226 | 3.0 | 1459, -487 |
| `paddenbloesem_tak` | 1342, -482 | 209 -> 227 | 3.0 | 1346, -488 |
| `spiraal_kruin` | 1378, -680 | 191 -> 228 | 3.0 | 1379, -673 |
| `mosvoet_kruin` | 1378, -648 | 233 -> 257 | 3.0 | 1380, -641 |
| `lichtkever_kruin` | 1443, -600 | 261 -> 285 | 3.0 | 1437, -596 |
| `honingtak` | 1333, -609 | 75 -> 167 | 3.0 | 1346, -603 |

## Travel: airship dock (Imkerstraat tip), lifts, Honinglift A/B, wind columns, cable bridge, riding spiral

| id | x, y, z |
|---|---|
| `binnenhof` | 1404, 167, -570 |
| `dok_bel` | 1196, 167, -604 |
| `dok_kapitein` | 1199, 167, -600 |
| `dok_landing` | 1201, 167, -601 |
| `dok_luchtschip` | 1169, 165, -605 |
| `dok_mast` | 1193, 167, -602 |
| `honinglift_boven` | 1509, 167, -523 |
| `honinglift_kruin_boven` | 1316, 231, -540 |
| `honinglift_kruin_onder` | 1316, 167, -534 |
| `honinglift_onder` | 1509, 71, -517 |
| `kabelbrug_begin` | 1511, 167, -676 |
| `kabelbrug_eind` | 1603, 160, -776 |
| `lift_cafe_boven` | 1340, 167, -588 |
| `lift_cafe_onder` | 1343, 155, -587 |
| `lift_kapel_boven` | 1394, 167, -642 |
| `lift_kapel_onder` | 1395, 152, -638 |
| `lift_raattros_boven` | 1464, 167, -566 |
| `lift_raattros_onder` | 1461, 157, -566 |
| `rijpad_eind` | 1417, 232, -548 |
| `rijpad_honingplein` | 1404, 167, -539 |
| `rijpad_start` | 1423, 84, -618 |
| `schorslift_1_boven` | 1403, 167, -585 |
| `schorslift_1_onder` | 1403, 81, -585 |
| `schorslift_2_boven` | 1412, 233, -576 |
| `schorslift_2_onder` | 1412, 167, -576 |
| `wind_hangnest_boven` | 1459, 226, -487 |
| `wind_hangnest_onder` | 1463, 203, -481 |
| `wind_honingtak_boven` | 1346, 167, -603 |
| `wind_honingtak_onder` | 1333, 75, -609 |
| `wind_lichtkever_kruin_boven` | 1437, 285, -596 |
| `wind_lichtkever_kruin_onder` | 1443, 261, -600 |
| `wind_mosvoet_kruin_boven` | 1380, 257, -641 |
| `wind_mosvoet_kruin_onder` | 1378, 233, -648 |
| `wind_paddenbloesem_tak_boven` | 1346, 227, -488 |
| `wind_paddenbloesem_tak_onder` | 1342, 209, -482 |
| `wind_spiraal_kruin_boven` | 1379, 228, -673 |
| `wind_spiraal_kruin_onder` | 1378, 190, -680 |
| `wind_werf_tak_boven` | 1302, 223, -617 |
| `wind_werf_tak_onder` | 1295, 213, -619 |

## The inner spiral stair: one stand spot per landing (1 = Schorsstad floor 80, 2 = Honingtak 166, 3/4/5 = kruin 1/2/3, 6 = nest floor)

| id | x, y, z |
|---|---|
| `trap_bordes_1` | 1389, 81, -562 |
| `trap_bordes_2` | 1404, 167, -556 |
| `trap_bordes_3` | 1423, 233, -575 |
| `trap_bordes_4` | 1394, 261, -594 |
| `trap_bordes_5` | 1420, 285, -565 |
| `trap_bordes_6` | 1391, 293, -592 |

## Honingtak: plaza, market, NPC spots

| id | x, y, z |
|---|---|
| `bloesemboompje` | 1374, 167, -536 |
| `knuffelbeeld` | 1404, 167, -524 |
| `markt` | 1404, 167, -516 |
| `npc_kraam_1n` | 1423, 167, -533 |
| `npc_kraam_1z` | 1429, 167, -520 |
| `npc_kraam_2n` | 1413, 167, -530 |
| `npc_kraam_2z` | 1416, 167, -516 |
| `npc_kraam_3n` | 1404, 167, -529 |
| `npc_kraam_3z` | 1404, 167, -515 |
| `npc_kraam_4n` | 1394, 167, -530 |
| `npc_kraam_4z` | 1391, 167, -516 |
| `npc_kraam_5n` | 1384, 167, -533 |
| `npc_kraam_5z` | 1378, 167, -520 |
| `npc_raatje_plein` | 1409, 167, -519 |
| `plein` | 1396, 167, -525 |
| `questbord` | 1398, 167, -518 |
| `respawn` | 1412, 167, -525 |

## Honingtak buildings (npc = behind the counter/at the back, binnen = interior centre, deur = stand outside the door, doos = box)

| id | x, y, z |
|---|---|
| `gebouw.algemeen.binnen` | 1427, 167, -628 |
| `gebouw.algemeen.deur` | 1425, 167, -623 |
| `gebouw.algemeen.doos_max` | 1435, 178, -622 |
| `gebouw.algemeen.doos_min` | 1421, 166, -636 |
| `gebouw.algemeen.npc` | 1429, 167, -631 |
| `gebouw.bank.binnen` | 1456, 167, -596 |
| `gebouw.bank.deur` | 1452, 167, -594 |
| `gebouw.bank.doos_max` | 1465, 178, -590 |
| `gebouw.bank.doos_min` | 1451, 166, -604 |
| `gebouw.bank.npc` | 1460, 167, -597 |
| `gebouw.burgemeester.binnen` | 1436, 167, -530 |
| `gebouw.burgemeester.deur` | 1433, 167, -535 |
| `gebouw.burgemeester.doos_max` | 1444, 178, -522 |
| `gebouw.burgemeester.doos_min` | 1430, 166, -536 |
| `gebouw.burgemeester.npc` | 1438, 167, -527 |
| `gebouw.cafe.binnen` | 1349, 155, -586 |
| `gebouw.cafe.deur` | 1342, 155, -587 |
| `gebouw.cafe.npc` | 1352, 155, -586 |
| `gebouw.cafe.podium` | 1354, 155, -582 |
| `gebouw.drankjes.binnen` | 1384, 167, -629 |
| `gebouw.drankjes.deur` | 1386, 167, -625 |
| `gebouw.drankjes.doos_max` | 1390, 178, -624 |
| `gebouw.drankjes.doos_min` | 1376, 166, -638 |
| `gebouw.drankjes.npc` | 1383, 167, -633 |
| `gebouw.jagershut.binnen` | 1460, 167, -582 |
| `gebouw.jagershut.deur` | 1456, 167, -581 |
| `gebouw.jagershut.doos_max` | 1468, 178, -575 |
| `gebouw.jagershut.doos_min` | 1454, 166, -589 |
| `gebouw.jagershut.jachtbord` | 1461, 167, -578 |
| `gebouw.jagershut.npc` | 1462, 167, -582 |
| `gebouw.jagershut.premiebord` | 1460, 167, -585 |
| `gebouw.juwelier.binnen` | 1352, 167, -600 |
| `gebouw.juwelier.deur` | 1357, 167, -598 |
| `gebouw.juwelier.doos_max` | 1358, 178, -594 |
| `gebouw.juwelier.doos_min` | 1344, 166, -608 |
| `gebouw.juwelier.npc` | 1349, 167, -602 |
| `gebouw.nestkapel.advancement` | 1401, 152, -630 |
| `gebouw.nestkapel.binnen` | 1396, 152, -631 |
| `gebouw.nestkapel.deur` | 1395, 152, -639 |
| `gebouw.nestkapel.droomsteen` | 1391, 154, -628 |
| `gebouw.nestkapel.eiersokkel` | 1397, 154, -625 |
| `gebouw.nestkapel.npc` | 1396, 152, -628 |
| `gebouw.pluimstal.binnen` | 1376, 75, -677 |
| `gebouw.pluimstal.deur` | 1379, 75, -668 |
| `gebouw.pluimstal.npc` | 1378, 75, -672 |
| `gebouw.pluimstal.voer` | 1383, 75, -679 |
| `gebouw.raattros.binnen` | 1458, 157, -567 |
| `gebouw.raattros.deur` | 1462, 157, -566 |
| `gebouw.raattros.npc` | 1455, 157, -567 |
| `gebouw.smid.binnen` | 1367, 167, -533 |
| `gebouw.smid.deur` | 1370, 167, -536 |
| `gebouw.smid.doos_max` | 1373, 178, -525 |
| `gebouw.smid.doos_min` | 1359, 166, -539 |
| `gebouw.smid.npc` | 1366, 167, -531 |
| `gebouw.uitrusting.binnen` | 1413, 167, -632 |
| `gebouw.uitrusting.deur` | 1412, 167, -627 |
| `gebouw.uitrusting.doos_max` | 1420, 178, -627 |
| `gebouw.uitrusting.doos_min` | 1406, 166, -641 |
| `gebouw.uitrusting.npc` | 1414, 167, -636 |
| `gebouw.woning_1.binnen` | 1485, 167, -543 |
| `gebouw.woning_1.deur` | 1483, 167, -539 |
| `gebouw.woning_1.doos_max` | 1493, 178, -538 |
| `gebouw.woning_1.doos_min` | 1479, 166, -552 |
| `gebouw.woning_1.npc` | 1487, 167, -546 |
| `gebouw.woning_2.binnen` | 1491, 167, -510 |
| `gebouw.woning_2.deur` | 1494, 167, -514 |
| `gebouw.woning_2.doos_max` | 1498, 178, -502 |
| `gebouw.woning_2.doos_min` | 1484, 166, -516 |
| `gebouw.woning_2.npc` | 1489, 167, -508 |
| `gebouw.woning_3.binnen` | 1333, 167, -523 |
| `gebouw.woning_3.deur` | 1330, 167, -527 |
| `gebouw.woning_3.doos_max` | 1341, 178, -515 |
| `gebouw.woning_3.doos_min` | 1327, 166, -529 |
| `gebouw.woning_3.npc` | 1336, 167, -520 |
| `gebouw.woning_4.binnen` | 1301, 167, -535 |
| `gebouw.woning_4.deur` | 1304, 167, -531 |
| `gebouw.woning_4.doos_max` | 1307, 178, -530 |
| `gebouw.woning_4.doos_min` | 1293, 166, -544 |
| `gebouw.woning_4.npc` | 1299, 167, -538 |
| `gebouw.woning_5.binnen` | 1351, 167, -648 |
| `gebouw.woning_5.deur` | 1347, 167, -644 |
| `gebouw.woning_5.doos_max` | 1359, 178, -642 |
| `gebouw.woning_5.doos_min` | 1345, 166, -656 |
| `gebouw.woning_5.npc` | 1353, 167, -650 |
| `gebouw.woning_6.binnen` | 1319, 167, -647 |
| `gebouw.woning_6.deur` | 1322, 167, -650 |
| `gebouw.woning_6.doos_max` | 1325, 178, -638 |
| `gebouw.woning_6.doos_min` | 1311, 166, -652 |
| `gebouw.woning_6.npc` | 1316, 168, -644 |

## Pluimstal (starter choice, z3_hq03)

| id | x, y, z |
|---|---|
| `pluimstal_keuze` | 1372, 74, -692 |
| `pluimstal_keuze_pluimguh_1` | 1365, 74, -697 |
| `pluimstal_keuze_pluimguh_2` | 1370, 74, -698 |
| `pluimstal_keuze_pluimguh_3` | 1376, 74, -700 |
| `pluimstal_kruinring_start` | 1423, 84, -618 |
| `pluimstal_oefenbaan` | 1386, 69, -717 |
| `pluimstal_zweefplatform` | 1357, 109, -594 |

## Arenas (world boss, Pelszaal, Vahoegnir, dungeon bosses, Mika-imker)

| id | x, y, z |
|---|---|
| `arena_baron_bleu` | 1515, 29, -565 |
| `arena_brokkel` | 1655, 192, -867 |
| `arena_brokkel_kanon_1` | 1647, 192, -864 |
| `arena_brokkel_kanon_2` | 1663, 192, -864 |
| `arena_kruinroofvogel` | 1259, 226, -530 |
| `arena_kruinroofvogel_bord` | 1289, 227, -539 |
| `arena_kruinroofvogel_deur` | 1288, 229, -549 |
| `arena_nestwachter` | 1481, 29, -496 |
| `arena_piepvel` | 1404, 61, -576 |
| `arena_piepvel_deur` | 1405, 61, -590 |
| `arena_roquefort` | 1692, 224, -821 |
| `arena_taleggio` | 1615, 168, -821 |
| `arena_vahoegnir` | 1404, 293, -576 |
| `arena_wortelwurger` | 1411, 29, -465 |
| `kruinroofvogel_zit_1` | 1471, 210, -452 |
| `kruinroofvogel_zit_2` | 1530, 200, -587 |
| `mika_imker_luik` | 1457, 157, -565 |
| `mika_imker_raatzaal` | 1458, 145, -567 |
| `nest_eierkuil` | 1404, 293, -576 |
| `nest_rand` | 1396, 293, -587 |
| `nest_tak_1` | 1415, 293, -571 |
| `nest_tak_2` | 1393, 293, -571 |
| `nest_tak_3` | 1404, 293, -563 |
| `nest_tak_4` | 1415, 293, -583 |
| `nest_tak_5` | 1393, 293, -583 |
| `nest_tak_6` | 1404, 293, -590 |
| `piepvel_grote_kooi` | 1410, 61, -587 |
| `piepvel_kooi_1` | 1411, 63, -569 |
| `piepvel_kooi_2` | 1396, 65, -569 |
| `piepvel_kooi_3` | 1396, 67, -584 |
| `piepvel_kooi_4` | 1411, 69, -584 |
| `piepvel_lantaarnpaal_1` | 1417, 61, -576 |
| `piepvel_lantaarnpaal_2` | 1410, 61, -565 |
| `piepvel_lantaarnpaal_3` | 1397, 61, -565 |
| `piepvel_lantaarnpaal_4` | 1390, 61, -576 |
| `piepvel_lantaarnpaal_5` | 1397, 61, -588 |
| `piepvel_lantaarnpaal_6` | 1410, 61, -588 |
| `piepvel_troon` | 1404, 61, -588 |

## Kaaszeppelinwerf (dungeon 1)

| id | x, y, z |
|---|---|
| `roquefort_kajuit` | 1695, 224, -827 |
| `roquefort_wrak` | 1686, 221, -805 |
| `werf_bord` | 1606, 160, -785 |
| `werf_ingang` | 1609, 160, -782 |
| `werf_kamer_bewapening` | 1664, 165, -838 |
| `werf_kamer_gieterij` | 1636, 157, -884 |
| `werf_kamer_touwwerk` | 1621, 158, -795 |
| `werf_loods_poort` | 1664, 165, -829 |
| `werf_trap_ab` | 1647, 158, -884 |
| `werf_trap_bc` | 1675, 160, -824 |
| `werf_vahoeg_altaar` | 1612, 161, -779 |

## Hart van de Boom (dungeon 2) + the egg

| id | x, y, z |
|---|---|
| `ei` | 1404, 26, -576 |
| `hart_bord` | 1330, 29, -571 |
| `hart_grotlantaarn_1` | 1331, 29, -573 |
| `hart_grotlantaarn_2` | 1324, 32, -573 |
| `hart_grotlantaarn_3` | 1324, 32, -580 |
| `hart_grotlantaarn_4` | 1331, 29, -580 |
| `hart_kamer_aderloop` | 1305, 29, -553 |
| `hart_kamer_ringenzaal` | 1348, 29, -480 |
| `hart_kamer_schimmelkwekerij` | 1305, 29, -524 |
| `hart_kwekerij_poort` | 1305, 29, -542 |
| `hart_nestkamer` | 1413, 21, -576 |
| `hart_poort` | 1327, 31, -576 |
| `hart_ringenzaal_poort` | 1348, 29, -498 |
| `hart_vahoeg_altaar` | 1330, 29, -581 |

## Hartsplinters + Keizer flashes

| id | x, y, z |
|---|---|
| `flash_f1_kwastgat` | 1423, 128, -568 |
| `flash_f2_kijkpunt` | 1396, 152, -630 |
| `grauwe_brief_1` | 1694, 224, -829 |
| `hartsplinter_2` | 1397, 293, -570 |
| `hartsplinter_3` | 1697, 224, -827 |
| `hartsplinter_4` | 1407, 61, -572 |
| `hartsplinter_5` | 1487, 29, -502 |
| `vingerafdruk` | 1423, 130, -564 |

## Campfires (unlit, kern checkpoints)

| id | x, y, z |
|---|---|
| `kampvuur_hangnest` | 1446, 197, -517 |
| `kampvuur_hartslag` | 1464, 25, -590 |
| `kampvuur_kruin_1` | 1503, 230, -534 |
| `kampvuur_kruin_2` | 1360, 261, -626 |
| `kampvuur_kruin_3` | 1385, 284, -536 |
| `kampvuur_kruin_nest` | 1427, 285, -547 |
| `kampvuur_mosvoet` | 1526, 67, -479 |
| `kampvuur_schorsstad_ring4` | 1402, 132, -550 |
| `kampvuur_schorsstad_vloer` | 1394, 81, -569 |
| `kampvuur_tak_noordwest_60` | 1391, 185, -653 |
| `kampvuur_tak_west_30` | 1351, 209, -595 |
| `kampvuur_tak_west_62` | 1311, 210, -607 |
| `kampvuur_tak_zuidwest_45` | 1354, 204, -517 |
| `kampvuur_wortelgang_1` | 1507, 58, -575 |
| `kampvuur_wortelgang_2` | 1472, 38, -570 |
| `kampvuur_wortelgewelf` | 1365, 66, -739 |
| `kampvuur_wortelweg_1` | 1557, 67, -488 |
| `kampvuur_wortelweg_2` | 1404, 66, -399 |
| `kampvuur_wortelweg_3` | 1250, 67, -488 |
| `kampvuur_wortelweg_4` | 1250, 65, -665 |
| `kampvuur_wortelweg_5` | 1403, 65, -753 |
| `kampvuur_wortelweg_6` | 1557, 66, -665 |

## Viewpoints

| id | x, y, z |
|---|---|
| `uitkijk_kroon` | 1469, 270, -530 |
| `uitkijk_nest` | 1406, 293, -559 |
| `uitkijk_oosttak` | 1620, 167, -486 |
| `uitkijk_takpunt` | 1295, 213, -619 |
| `uitkijk_westtak` | 1189, 167, -600 |
| `uitkijk_wortelgewelf` | 1350, 81, -743 |

## Kruinzee

| id | x, y, z |
|---|---|
| `bloesemwaterval_top` | 1365, 222, -465 |
| `grauwe_vlek_1` | 1494, 229, -525 |
| `grauwe_vlek_2` | 1350, 260, -532 |
| `grauwe_vlek_3` | 1365, 226, -682 |
| `honingvijver_1` | 1448, 224, -478 |
| `honingvijver_2` | 1356, 228, -484 |
| `honingvijver_3` | 1305, 226, -626 |
| `honingvijver_4` | 1428, 229, -680 |
| `honingvijver_5` | 1503, 227, -624 |
| `mika_bevel` | 1453, 229, -668 |
| `mika_steiger` | 1459, 227, -672 |
| `zweefplatform_1` | 1434, 224, -464 |
| `zweefplatform_2` | 1337, 226, -672 |

## Takkenbanen + the beekeeper villages (hangnest_* = Imkerdorp Zoemrust on the Bijentak; hangnest_woning_1..3 = Raatweide, 4..6 = Molenhoek)

| id | x, y, z |
|---|---|
| `hangnest_herberg` | 1444, 200, -503 |
| `hangnest_ladder_top` | 1433, 197, -541 |
| `hangnest_lusje` | 1453, 198, -535 |
| `hangnest_plein` | 1455, 199, -506 |
| `hangnest_stalpost` | 1462, 201, -493 |
| `hangnest_winkeltje` | 1475, 204, -481 |
| `hangnest_woning_1` | 1374, 205, -521 |
| `hangnest_woning_2` | 1341, 207, -509 |
| `hangnest_woning_3` | 1352, 212, -474 |
| `hangnest_woning_4` | 1382, 185, -633 |
| `hangnest_woning_5` | 1395, 188, -667 |
| `hangnest_woning_6` | 1364, 193, -685 |
| `kaasschutterpost_1` | 1303, 211, -616 |
| `kaasschutterpost_2` | 1499, 194, -587 |
| `kaasschutterpost_3` | 1345, 208, -488 |
| `lianenbrug` | 1454, 191, -581 |
| `molenhoek_plein` | 1384, 187, -662 |
| `pluimpost_westtak` | 1338, 209, -600 |
| `raatweide_plein` | 1352, 206, -504 |
| `valbrug` | 1355, 210, -589 |

## Riding spiral (spiraal_*) + Schorsstad (Lantaarntoren rings: stand on the tower ring platform; schorshuis.N = the houses in the trunk wall)

| id | x, y, z |
|---|---|
| `boomvolk_lichtje_1` | 1391, 173, -591 |
| `boomvolk_lichtje_2` | 1392, 186, -561 |
| `boomvolk_lichtje_3` | 1420, 199, -568 |
| `boomvolk_lichtje_4` | 1409, 212, -595 |
| `lantaarnring_1` | 1408, 93, -576 |
| `lantaarnring_1_kooi` | 1400, 93, -576 |
| `lantaarnring_2` | 1408, 103, -576 |
| `lantaarnring_2_kooi` | 1400, 103, -576 |
| `lantaarnring_3` | 1408, 113, -576 |
| `lantaarnring_3_kooi` | 1400, 113, -576 |
| `lantaarnring_4` | 1408, 123, -576 |
| `lantaarnring_4_kooi` | 1400, 123, -576 |
| `lantaarnring_5` | 1408, 133, -576 |
| `lantaarnring_5_kooi` | 1400, 133, -576 |
| `lantaarnring_6` | 1408, 143, -576 |
| `lantaarnring_6_kooi` | 1400, 143, -576 |
| `lantaarnring_7` | 1408, 153, -576 |
| `lantaarnring_7_kooi` | 1400, 153, -576 |
| `muurverhaal_1` | 1382, 101, -581 |
| `muurverhaal_2` | 1425, 110, -572 |
| `muurverhaal_3` | 1382, 119, -581 |
| `muurverhaal_4` | 1425, 128, -572 |
| `muurverhaal_5` | 1382, 137, -581 |
| `muurverhaal_6` | 1425, 146, -572 |
| `muurverhaal_7` | 1417, 195, -593 |
| `muurverhaal_8` | 1390, 204, -560 |
| `nestlantaarn_knoest` | 1404, 123, -572 |
| `npc_oude_knoest` | 1405, 123, -603 |
| `pelszaal_trap_boven` | 1384, 81, -568 |
| `schorshuis.1` | 1430, 92, -575 |
| `schorshuis.10` | 1402, 132, -550 |
| `schorshuis.11` | 1377, 137, -578 |
| `schorshuis.12` | 1405, 141, -603 |
| `schorshuis.13` | 1430, 146, -575 |
| `schorshuis.14` | 1402, 150, -550 |
| `schorshuis.15` | 1377, 155, -578 |
| `schorshuis.2` | 1402, 96, -550 |
| `schorshuis.3` | 1377, 101, -578 |
| `schorshuis.4` | 1405, 105, -603 |
| `schorshuis.5` | 1430, 110, -575 |
| `schorshuis.6` | 1402, 114, -550 |
| `schorshuis.7` | 1377, 119, -578 |
| `schorshuis.8` | 1405, 123, -603 |
| `schorshuis.9` | 1430, 128, -575 |
| `schorsstad_poort_128` | 1403, 81, -596 |
| `schorsstad_poort_176` | 1404, 167, -555 |
| `schorsstad_poort_75` | 1393, 81, -561 |
| `schorsstad_ring_1` | 1410, 95, -557 |
| `schorsstad_ring_2` | 1404, 105, -597 |
| `schorsstad_ring_3` | 1396, 115, -558 |
| `schorsstad_ring_4` | 1417, 125, -592 |
| `schorsstad_ring_5` | 1385, 135, -567 |
| `schorsstad_ring_6` | 1424, 145, -579 |
| `schorsstad_ring_7` | 1383, 155, -581 |
| `schorsstad_vloer` | 1413, 81, -584 |
| `spiraal_balkon_1` | 1360, 109, -592 |
| `spiraal_balkon_2` | 1442, 127, -554 |
| `spiraal_balkon_3` | 1390, 182, -614 |
| `spiraal_poort_128` | 1403, 80, -616 |
| `spiraal_poort_176` | 1404, 168, -531 |
| `spiraal_poort_75` | 1380, 80, -542 |
| `spiraal_top` | 1417, 232, -548 |
| `spiraal_voet` | 1423, 84, -618 |
| `spiraal_zweefsprong_1` | 1357, 109, -594 |
| `spiraal_zweefsprong_2` | 1446, 127, -552 |
| `spiraal_zweefsprong_3` | 1388, 182, -618 |

## Wortelvelden (Mosvoet hamlet, Pluimposten, camps, valleys)

| id | x, y, z |
|---|---|
| `honingmoeras` | 1559, 69, -633 |
| `kamp.1` | 1376, 69, -404 |
| `kamp.2` | 1241, 63, -655 |
| `kamp.3` | 1529, 67, -684 |
| `lichtkeverbos` | 1403, 70, -726 |
| `mosvoet_burgemeester` | 1543, 67, -480 |
| `mosvoet_plein` | 1531, 67, -484 |
| `mosvoet_stalpost` | 1520, 68, -487 |
| `mosvoet_theehuis` | 1534, 67, -472 |
| `mosvoet_waterrad` | 1529, 67, -495 |
| `mosvoet_winkel` | 1523, 68, -475 |
| `mosvoet_woning` | 1540, 67, -492 |
| `nachtgeheim_zadel` | 1339, 59, -755 |
| `paddenbloesemdal` | 1390, 69, -417 |
| `pluimpost_1` | 1492, 66, -729 |
| `pluimpost_2` | 1251, 65, -665 |
| `pluimpost_3` | 1303, 67, -432 |
| `pluimpost_4` | 1492, 68, -424 |
| `schimmelboerderij` | 1253, 69, -631 |
| `schimmelketel_1` | 1272, 69, -640 |
| `schimmelketel_2` | 1245, 69, -620 |
| `schimmelketel_3` | 1267, 68, -675 |
| `schimmelketel_4` | 1253, 68, -590 |
| `tijdrit_start` | 1422, 84, -619 |
| `wortelgewelf` | 1362, 65, -743 |
| `wortelpoort` | 1550, 71, -576 |

## Wortelduister

| id | x, y, z |
|---|---|
| `grotlantaarn_1` | 1540, 67, -579 |
| `grotlantaarn_2` | 1528, 65, -582 |
| `grotlantaarn_3` | 1516, 62, -584 |
| `grotlantaarn_4` | 1505, 57, -580 |
| `grotlantaarn_5` | 1495, 51, -575 |
| `grotlantaarn_6` | 1485, 45, -574 |
| `grotlantaarn_7` | 1475, 39, -576 |
| `grotlantaarn_8` | 1466, 34, -578 |
| `grotlantaarn_9` | 1456, 28, -579 |
| `hartslag_zaal` | 1456, 20, -576 |

## Secrets (knuffels, schatten, Mika-imker clues)

| id | x, y, z |
|---|---|
| `geheim_knuffel_1` | 1354, 229, -490 |
| `geheim_knuffel_10` | 1364, 23, -546 |
| `geheim_knuffel_2` | 1403, 261, -616 |
| `geheim_knuffel_3` | 1380, 190, -678 |
| `geheim_knuffel_4` | 1250, 61, -448 |
| `geheim_knuffel_5` | 1494, 229, -629 |
| `geheim_knuffel_6` | 1605, 67, -541 |
| `geheim_knuffel_7` | 1386, 59, -776 |
| `geheim_knuffel_8` | 1434, 23, -616 |
| `geheim_knuffel_9` | 1405, 105, -605 |
| `geheim_schat_1` | 1347, 208, -493 |
| `geheim_schat_2` | 1408, 293, -592 |
| `geheim_schat_3` | 1402, 150, -548 |
| `geheim_schat_4` | 1344, 25, -566 |
| `geheim_schat_5` | 1598, 63, -593 |
| `imker_aanwijzing_1` | 1414, 167, -528 |
| `imker_aanwijzing_2` | 1311, 167, -527 |
| `imker_aanwijzing_3` | 1464, 167, -543 |
| `imker_aanwijzing_4` | 1364, 167, -545 |
| `imker_aanwijzing_5` | 1460, 157, -565 |

## Landmarks

| id | x, y, z |
|---|---|
| `landmark_hoogvads_richting` | 2104, 260, -576 |

## Other

| id | x, y, z |
|---|---|
| `kuiken_1` | 1467, 226, -489 |
| `kuiken_2` | 1374, 228, -476 |
| `kuiken_3` | 1297, 213, -605 |
| `kuiken_4` | 1408, 227, -683 |
| `kuiken_5` | 1493, 228, -641 |
| `kuiken_6` | 1346, 231, -658 |
| `oosttaksteiger_bel` | 1609, 167, -485 |
| `oosttaksteiger_kapitein` | 1610, 167, -490 |
| `oosttaksteiger_landing` | 1607, 165, -488 |
| `oosttaksteiger_luchtschip` | 1637, 165, -476 |
| `oosttaksteiger_mast` | 1614, 167, -485 |
