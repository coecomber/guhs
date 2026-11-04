# POI_Z3 - coordinates of zone 3 "De Schemerwortel" (z3-wereld slice)

**v1 (map, stand spots snapped onto the tiles), GENERATED** by `tools/vadsrijk/zone3/poi3.py` (via `python tools/vadsrijk/maak_kaart.py --zone 3`). Do not edit by hand; requests go to `../POI_VERZOEKEN_Z3.md`. Java: `nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken` (`plek("id")`, `GEBIEDEN` / `gebied(x, y, z)`, `MOBZONES` / `mobzone(x, y, z)`, `PADEN`, `KAMPVUREN`, `WINDKOLOMMEN`, `LIFTEN`, `UITKIJKEN`). Dev: `/guhs vadsrijk z3wereld plek <id>` teleports there.

**Layout.** Zone 3 lies in the EAST: box x 1600..3000, z -700..700, trunk axis at (2300, 0); tiles tx 25..46, tz -11..10. It is the render design (renders-schemerwortel) MIRRORED in x: Honingtak sits on the WEST branch (x 1634..2014, pointing to zone 1/2, airship mast at x ~1677), the W/NW/ZW riding branches reach EAST (to x ~2975), Hoogvads lies further east. Layers: crown 200-291 (nest ~280), branches 150-235, hollow trunk (Schorsstad) 75-196, ground ~64-95 (root hill rises to ~90 at the trunk), roots/Wortelduister 6-60, Hart dungeon ring y 50-72. Cloud sea around the root island (r ~660).

All positions are **stand positions** (the air block a player/NPC stands in) in `guhs:vadsrijk`, unless noted (`doos` = box corners, `*_kooi`/`piepvel_kooi_*` = the hanging cage block, `wind_*_boven` = landing at the top).

## Areas (with y-ranges; `Z3WereldPlekken.GEBIEDEN`, first match wins)

| id | shape | y | levels | town |
|---|---|---|---|---|
| `z3_honingtak` | box (1634,-100)..(2014,110) | 150-290 | 30-50 | yes |
| `z3_zeppelinwerf` | box (2819,-660)..(2979,-480) | 95-185 | 38-40 | no |
| `z3_hart` | cylinder around (2300,0), r 0..120 | 40-74 | 49-50 | no |
| `z3_schorsstad` | cylinder around (2300,0), r 0..76 | 75-199 | 40-43 | no |
| `z3_stamspiraal` | cylinder around (2300,0), r 76..132 | 66-205 | 40-43 | no |
| `z3_kruinzee` | cylinder around (2300,0), r 0..400 | 200-320 | 30-34 | no |
| `z3_takkenbanen` | cylinder around (2300,0), r 0..820 | 120-320 | 34-38 | no |
| `z3_wortelduister` | cylinder around (2300,0), r 0..470 | 0-61 | 46-49 | no |
| `z3_wortelvelden` | cylinder around (2300,0), r 0..900 | 0-320 | 43-46 | no |

## Mob zones (`Z3WereldPlekken.MOBZONES`; spawn only inside, never within ~5 blocks of `PADEN`)

| id | shape | y | levels |
|---|---|---|---|
| `z3_kruinzee_zeppelin` | cylinder around (2489,-200), r 0..60 | 230-300 | 31-34 |
| `z3_kruinzee_noord` | cylinder around (2300,-150), r 0..160 | 205-300 | 30-33 |
| `z3_kruinzee_zuid` | cylinder around (2300,140), r 0..170 | 205-300 | 30-34 |
| `z3_takkenbanen_west` | box (2719,-40)..(2999,110) | 120-230 | 34-37 |
| `z3_takkenbanen_noordwest` | box (2559,-560)..(2859,-250) | 120-230 | 35-38 |
| `z3_takkenbanen_zuidwest` | box (2559,240)..(2859,520) | 120-230 | 34-37 |
| `z3_schorsstad_ringen` | cylinder around (2300,0), r 8..66 | 76-182 | 40-43 |
| `z3_wortelvelden_lichtkeverbos` | box (2139,-640)..(2459,-330) | 60-140 | 43-45 |
| `z3_wortelvelden_schimmelboerderij` | box (2479,-420)..(2719,-180) | 60-140 | 44-46 |
| `z3_wortelvelden_paddenbloesemdal` | box (2479,200)..(2819,520) | 60-140 | 43-45 |
| `z3_wortelvelden_honingmoeras` | box (1779,150)..(2139,520) | 60-140 | 44-46 |
| `z3_wortelduister_gang` | cylinder around (2300,160), r 0..140 | 10-62 | 46-47 |
| `z3_wortelduister_grotten` | cylinder around (2300,0), r 70..450 | 6-60 | 47-49 |

## Wind columns (`WINDKOLOMMEN`: x, z, y0 -> y1 = landing stand y, radius, landing x, z)

| id | x, z | y | r | landing |
|---|---|---|---|---|
| `hangnest` | 2807, 56 | 66 -> 195 | 3.0 | 2807, 40 |
| `paddenbloesem_tak` | 2715, 349 | 69 -> 198 | 3.0 | 2705, 361 |
| `werf_tak` | 2734, -390 | 71 -> 197 | 3.0 | 2724, -401 |
| `mosvoet_kruin` | 2094, 195 | 73 -> 258 | 3.5 | 2101, 188 |
| `lichtkever_kruin` | 2259, -300 | 77 -> 251 | 3.5 | 2261, -291 |
| `spiraal_kruin` | 2299, -102 | 201 -> 270 | 3.0 | 2299, -90 |
| `honingtak` | 1897, 38 | 71 -> 239 | 3.0 | 1903, 30 |

## Travel: airship dock (Honingtak tip), lifts, Honinglift, wind columns

| id | x, y, z |
|---|---|
| `dok_bel` | 1681, 239, 3 |
| `dok_kapitein` | 1685, 239, -3 |
| `dok_landing` | 1689, 239, 2 |
| `dok_luchtschip` | 1695, 237, 24 |
| `dok_mast` | 1677, 239, 0 |
| `honinglift_boven` | 1907, 239, 24 |
| `honinglift_onder` | 2042, 70, 400 |
| `lift_cafe_boven` | 1871, 239, -20 |
| `lift_cafe_onder` | 1871, 182, -24 |
| `lift_kapel_boven` | 1794, 239, 21 |
| `lift_kapel_onder` | 1794, 182, 25 |
| `lift_raattros_boven` | 1731, 239, -18 |
| `lift_raattros_onder` | 1731, 191, -22 |
| `schorslift_1_boven` | 2349, 177, 29 |
| `schorslift_1_onder` | 2349, 76, 29 |
| `schorslift_2_boven` | 2272, 143, -52 |
| `schorslift_2_onder` | 2272, 76, -52 |
| `wind_hangnest_boven` | 2807, 195, 40 |
| `wind_hangnest_onder` | 2807, 66, 56 |
| `wind_honingtak_boven` | 1903, 239, 30 |
| `wind_honingtak_onder` | 1897, 71, 38 |
| `wind_lichtkever_kruin_boven` | 2261, 251, -292 |
| `wind_lichtkever_kruin_onder` | 2259, 77, -300 |
| `wind_mosvoet_kruin_boven` | 2101, 258, 188 |
| `wind_mosvoet_kruin_onder` | 2094, 73, 195 |
| `wind_paddenbloesem_tak_boven` | 2705, 198, 361 |
| `wind_paddenbloesem_tak_onder` | 2715, 69, 349 |
| `wind_spiraal_kruin_boven` | 2299, 270, -90 |
| `wind_spiraal_kruin_onder` | 2299, 201, -102 |
| `wind_werf_tak_boven` | 2724, 197, -402 |
| `wind_werf_tak_onder` | 2734, 71, -391 |

## Honingtak: plaza, market, NPC spots

| id | x, y, z |
|---|---|
| `bloesemboompje` | 1751, 239, 9 |
| `knuffelbeeld` | 1829, 239, -2 |
| `markt` | 1879, 239, 0 |
| `npc_kraam_1n` | 1901, 239, -16 |
| `npc_kraam_1z` | 1901, 239, 13 |
| `npc_kraam_2n` | 1889, 239, -16 |
| `npc_kraam_2z` | 1889, 239, 10 |
| `npc_kraam_3n` | 1877, 239, -16 |
| `npc_kraam_3z` | 1877, 239, 11 |
| `npc_kraam_4n` | 1865, 239, -16 |
| `npc_kraam_4z` | 1865, 239, 11 |
| `npc_kraam_5n` | 1853, 239, -16 |
| `npc_kraam_5z` | 1853, 239, 11 |
| `npc_raatje_plein` | 1823, 239, -8 |
| `plein` | 1839, 239, -10 |
| `questbord` | 1841, 239, -5 |
| `respawn` | 1839, 239, 4 |

## Honingtak buildings (npc = behind the counter/at the back, binnen = interior centre, deur = stand outside the door, doos = box)

| id | x, y, z |
|---|---|
| `gebouw.algemeen.binnen` | 1779, 224, -25 |
| `gebouw.algemeen.deur` | 1779, 224, -32 |
| `gebouw.algemeen.doos_max` | 1786, 235, -18 |
| `gebouw.algemeen.doos_min` | 1772, 223, -32 |
| `gebouw.algemeen.npc` | 1779, 224, -22 |
| `gebouw.bank.binnen` | 1753, 220, 21 |
| `gebouw.bank.deur` | 1753, 220, 28 |
| `gebouw.bank.doos_max` | 1760, 231, 28 |
| `gebouw.bank.doos_min` | 1746, 219, 14 |
| `gebouw.bank.npc` | 1753, 220, 18 |
| `gebouw.burgemeester.binnen` | 1699, 226, 20 |
| `gebouw.burgemeester.deur` | 1699, 226, 27 |
| `gebouw.burgemeester.doos_max` | 1706, 237, 27 |
| `gebouw.burgemeester.doos_min` | 1692, 225, 13 |
| `gebouw.burgemeester.npc` | 1699, 226, 18 |
| `gebouw.cafe.binnen` | 1871, 182, 1 |
| `gebouw.cafe.deur` | 1871, 182, -12 |
| `gebouw.cafe.npc` | 1871, 182, -6 |
| `gebouw.cafe.podium` | 1870, 182, 2 |
| `gebouw.drankjes.binnen` | 1841, 218, -28 |
| `gebouw.drankjes.deur` | 1841, 218, -35 |
| `gebouw.drankjes.doos_max` | 1848, 229, -21 |
| `gebouw.drankjes.doos_min` | 1834, 217, -35 |
| `gebouw.drankjes.npc` | 1841, 218, -25 |
| `gebouw.jagershut.binnen` | 1715, 214, -20 |
| `gebouw.jagershut.deur` | 1715, 214, -27 |
| `gebouw.jagershut.doos_max` | 1722, 225, -13 |
| `gebouw.jagershut.doos_min` | 1708, 213, -27 |
| `gebouw.jagershut.jachtbord` | 1712, 214, -18 |
| `gebouw.jagershut.npc` | 1715, 214, -18 |
| `gebouw.jagershut.premiebord` | 1718, 214, -18 |
| `gebouw.juwelier.binnen` | 1859, 222, 21 |
| `gebouw.juwelier.deur` | 1859, 222, 28 |
| `gebouw.juwelier.doos_max` | 1866, 233, 28 |
| `gebouw.juwelier.doos_min` | 1852, 221, 14 |
| `gebouw.juwelier.npc` | 1859, 222, 19 |
| `gebouw.nestkapel.advancement` | 1790, 182, 2 |
| `gebouw.nestkapel.binnen` | 1794, 182, 3 |
| `gebouw.nestkapel.deur` | 1794, 182, 13 |
| `gebouw.nestkapel.droomsteen` | 1799, 182, 2 |
| `gebouw.nestkapel.eiersokkel` | 1794, 182, -2 |
| `gebouw.nestkapel.npc` | 1794, 182, -6 |
| `gebouw.pluimstal.binnen` | 1973, 238, 34 |
| `gebouw.pluimstal.deur` | 1973, 236, 44 |
| `gebouw.pluimstal.npc` | 1973, 236, 43 |
| `gebouw.pluimstal.voer` | 1983, 236, 43 |
| `gebouw.raattros.binnen` | 1731, 187, -1 |
| `gebouw.raattros.deur` | 1731, 191, -10 |
| `gebouw.smid.binnen` | 1887, 212, -28 |
| `gebouw.smid.deur` | 1887, 212, -35 |
| `gebouw.smid.doos_max` | 1894, 223, -21 |
| `gebouw.smid.doos_min` | 1880, 211, -35 |
| `gebouw.smid.npc` | 1887, 212, -26 |
| `gebouw.uitrusting.binnen` | 1807, 214, 21 |
| `gebouw.uitrusting.deur` | 1807, 214, 27 |
| `gebouw.uitrusting.doos_max` | 1814, 225, 28 |
| `gebouw.uitrusting.doos_min` | 1800, 213, 14 |
| `gebouw.uitrusting.npc` | 1807, 214, 18 |
| `gebouw.woning_1.binnen` | 1911, 216, 24 |
| `gebouw.woning_1.deur` | 1911, 216, 31 |
| `gebouw.woning_1.doos_max` | 1918, 227, 31 |
| `gebouw.woning_1.doos_min` | 1904, 215, 17 |
| `gebouw.woning_1.npc` | 1911, 216, 21 |
| `gebouw.woning_2.binnen` | 1897, 228, 25 |
| `gebouw.woning_2.deur` | 1897, 228, 32 |
| `gebouw.woning_2.doos_max` | 1904, 239, 32 |
| `gebouw.woning_2.doos_min` | 1890, 227, 18 |
| `gebouw.woning_2.npc` | 1897, 228, 23 |
| `gebouw.woning_3.binnen` | 1869, 228, -30 |
| `gebouw.woning_3.deur` | 1869, 228, -37 |
| `gebouw.woning_3.doos_max` | 1876, 239, -23 |
| `gebouw.woning_3.doos_min` | 1862, 227, -37 |
| `gebouw.woning_3.npc` | 1869, 228, -28 |
| `gebouw.woning_4.binnen` | 1763, 210, -24 |
| `gebouw.woning_4.deur` | 1763, 210, -30 |
| `gebouw.woning_4.doos_max` | 1770, 221, -17 |
| `gebouw.woning_4.doos_min` | 1756, 209, -31 |
| `gebouw.woning_4.npc` | 1763, 210, -21 |
| `gebouw.woning_5.binnen` | 1733, 230, 23 |
| `gebouw.woning_5.deur` | 1733, 230, 30 |
| `gebouw.woning_5.doos_max` | 1740, 241, 30 |
| `gebouw.woning_5.doos_min` | 1726, 229, 16 |
| `gebouw.woning_5.npc` | 1733, 230, 20 |
| `gebouw.woning_6.binnen` | 1823, 230, -29 |
| `gebouw.woning_6.deur` | 1823, 230, -36 |
| `gebouw.woning_6.doos_max` | 1830, 241, -22 |
| `gebouw.woning_6.doos_min` | 1816, 229, -36 |
| `gebouw.woning_6.npc` | 1823, 230, -26 |
| `gebouw.woning_7.binnen` | 1789, 230, 23 |
| `gebouw.woning_7.deur` | 1789, 230, 30 |
| `gebouw.woning_7.doos_max` | 1796, 241, 30 |
| `gebouw.woning_7.doos_min` | 1782, 229, 16 |
| `gebouw.woning_7.npc` | 1789, 230, 20 |
| `gebouw.woning_8.binnen` | 1705, 230, -21 |
| `gebouw.woning_8.deur` | 1705, 230, -28 |
| `gebouw.woning_8.doos_max` | 1712, 241, -14 |
| `gebouw.woning_8.doos_min` | 1698, 229, -28 |
| `gebouw.woning_8.npc` | 1705, 230, -19 |

## Pluimstal (starter choice, z3_hq03)

| id | x, y, z |
|---|---|
| `pluimstal_keuze` | 1969, 236, 58 |
| `pluimstal_keuze_pluimguh_1` | 1977, 236, 68 |
| `pluimstal_keuze_pluimguh_2` | 1969, 236, 68 |
| `pluimstal_keuze_pluimguh_3` | 1961, 236, 68 |
| `pluimstal_kruinring_start` | 2001, 228, 19 |
| `pluimstal_oefenbaan` | 1943, 236, 66 |
| `pluimstal_zweefplatform` | 1997, 236, 102 |

## Arenas (world boss, Pelszaal, Vahoegnir, dungeon bosses, Mika-imker)

| id | x, y, z |
|---|---|
| `arena_baron_bleu` | 2204, 51, 0 |
| `arena_brokkel` | 2875, 126, -554 |
| `arena_brokkel_kanon_1` | 2883, 128, -546 |
| `arena_brokkel_kanon_2` | 2867, 128, -546 |
| `arena_kruinroofvogel` | 2509, 268, -80 |
| `arena_kruinroofvogel_bord` | 2489, 268, -60 |
| `arena_kruinroofvogel_deur` | 2479, 267, -80 |
| `arena_nestwachter` | 2232, 51, 67 |
| `arena_piepvel` | 2299, 184, 0 |
| `arena_piepvel_deur` | 2295, 184, 13 |
| `arena_roquefort` | 2899, 111, -570 |
| `arena_taleggio` | 2921, 141, -584 |
| `arena_vahoegnir` | 2299, 279, -40 |
| `arena_wortelwurger` | 2299, 51, 95 |
| `kruinroofvogel_zit_1` | 2359, 260, -250 |
| `kruinroofvogel_zit_2` | 2549, 244, 110 |
| `mika_imker_luik` | 1728, 191, 2 |
| `mika_imker_raatzaal` | 1729, 178, 0 |
| `nest_eierkuil` | 2299, 279, -40 |
| `nest_rand` | 2299, 280, -18 |
| `nest_tak_1` | 2288, 279, -35 |
| `nest_tak_2` | 2310, 279, -35 |
| `nest_tak_3` | 2299, 279, -27 |
| `nest_tak_4` | 2288, 279, -47 |
| `nest_tak_5` | 2310, 279, -47 |
| `nest_tak_6` | 2299, 279, -54 |
| `piepvel_grote_kooi` | 2294, 184, -12 |
| `piepvel_kooi_1` | 2292, 186, 7 |
| `piepvel_kooi_2` | 2307, 189, 7 |
| `piepvel_kooi_3` | 2307, 192, -8 |
| `piepvel_kooi_4` | 2292, 195, -8 |
| `piepvel_lantaarnpaal_1` | 2285, 184, 0 |
| `piepvel_lantaarnpaal_2` | 2292, 184, 12 |
| `piepvel_lantaarnpaal_3` | 2306, 184, 12 |
| `piepvel_lantaarnpaal_4` | 2313, 184, 0 |
| `piepvel_lantaarnpaal_5` | 2307, 184, -13 |
| `piepvel_lantaarnpaal_6` | 2292, 184, -13 |
| `piepvel_troon` | 2299, 184, -12 |

## Kaaszeppelinwerf (dungeon 1)

| id | x, y, z |
|---|---|
| `roquefort_kajuit` | 2887, 111, -570 |
| `roquefort_wrak` | 2899, 108, -587 |
| `werf_bord` | 2858, 170, -534 |
| `werf_ingang` | 2861, 169, -537 |
| `werf_kamer_bewapening` | 2927, 126, -554 |
| `werf_kamer_gieterij` | 2873, 126, -586 |
| `werf_kamer_touwwerk` | 2881, 141, -554 |
| `werf_loods_poort` | 2927, 126, -546 |
| `werf_trap_ab` | 2895, 141, -596 |
| `werf_trap_bc` | 2905, 126, -544 |
| `werf_vahoeg_altaar` | 2863, 168, -540 |

## Hart van de Boom (dungeon 2) + the egg

| id | x, y, z |
|---|---|
| `ei` | 2299, 50, 0 |
| `hart_bord` | 2293, 13, -46 |
| `hart_grotlantaarn_1` | 2294, 13, -55 |
| `hart_grotlantaarn_2` | 2294, 13, -46 |
| `hart_grotlantaarn_3` | 2305, 13, -46 |
| `hart_grotlantaarn_4` | 2305, 13, -55 |
| `hart_kamer_aderloop` | 2367, 51, -68 |
| `hart_kamer_ringenzaal` | 2367, 51, 67 |
| `hart_kamer_schimmelkwekerij` | 2394, 51, 0 |
| `hart_kwekerij_poort` | 2393, 51, 14 |
| `hart_nestkamer` | 2293, 51, 0 |
| `hart_poort` | 2299, 13, -50 |
| `hart_ringenzaal_poort` | 2355, 54, 76 |
| `hart_vahoeg_altaar` | 2305, 13, -46 |

## Hartsplinters + Keizer flashes

| id | x, y, z |
|---|---|
| `flash_f1_kwastgat` | 2234, 178, 23 |
| `flash_f2_kijkpunt` | 1794, 182, 4 |
| `grauwe_brief_1` | 2886, 111, -572 |
| `hartsplinter_2` | 2313, 279, -49 |
| `hartsplinter_3` | 2886, 111, -568 |
| `hartsplinter_4` | 2297, 184, 3 |
| `hartsplinter_5` | 2228, 51, 67 |
| `vingerafdruk` | 2240, 172, 30 |

## Campfires (unlit, kern checkpoints)

| id | x, y, z |
|---|---|
| `kampvuur_hangnest` | 2787, 140, 33 |
| `kampvuur_hartslag` | 2269, 13, 20 |
| `kampvuur_kruin_1` | 2044, 255, 147 |
| `kampvuur_kruin_2` | 2442, 265, 82 |
| `kampvuur_kruin_3` | 2300, 255, -230 |
| `kampvuur_kruin_nest` | 2265, 282, -10 |
| `kampvuur_mosvoet` | 2055, 69, 414 |
| `kampvuur_schorsstad_ring4` | 2276, 129, 59 |
| `kampvuur_schorsstad_vloer` | 2273, 76, 34 |
| `kampvuur_tak_noordwest_60` | 2704, 197, -405 |
| `kampvuur_tak_west_30` | 2568, 225, 5 |
| `kampvuur_tak_west_62` | 2760, 201, 20 |
| `kampvuur_tak_zuidwest_45` | 2616, 214, 279 |
| `kampvuur_wortelgang_1` | 2244, 47, 213 |
| `kampvuur_wortelgang_2` | 2207, 31, 126 |
| `kampvuur_wortelgewelf` | 1956, 73, -147 |
| `kampvuur_wortelweg_1` | 1827, 67, 83 |
| `kampvuur_wortelweg_2` | 2135, 67, 451 |
| `kampvuur_wortelweg_3` | 2608, 67, 367 |
| `kampvuur_wortelweg_4` | 2772, 66, -84 |
| `kampvuur_wortelweg_5` | 2464, 68, -452 |
| `kampvuur_wortelweg_6` | 1991, 68, -368 |

## Viewpoints

| id | x, y, z |
|---|---|
| `uitkijk_kroon` | 2449, 276, 150 |
| `uitkijk_takpunt` | 2851, 162, 492 |
| `uitkijk_wortelgewelf` | 1957, 106, -148 |

## Kruinzee

| id | x, y, z |
|---|---|
| `bloesemwaterval_top` | 2391, 223, 280 |
| `grauwe_vlek_1` | 2149, 264, 120 |
| `grauwe_vlek_2` | 2409, 253, 205 |
| `grauwe_vlek_3` | 2239, 254, -250 |
| `honingvijver_1` | 2157, 262, -90 |
| `honingvijver_2` | 2423, 265, 60 |
| `honingvijver_3` | 2219, 273, 170 |
| `honingvijver_4` | 2341, 271, -200 |
| `honingvijver_5` | 2070, 267, 90 |
| `mika_bevel` | 2495, 258, -204 |
| `mika_steiger` | 2483, 258, -197 |
| `zweefplatform_1` | 2599, 230, 70 |
| `zweefplatform_2` | 2534, 232, 215 |

## Takkenbanen + Hangnest Dauwdrup

| id | x, y, z |
|---|---|
| `hangnest_herberg` | 2801, 137, 47 |
| `hangnest_ladder_top` | 2777, 199, 33 |
| `hangnest_lusje` | 2807, 144, 27 |
| `hangnest_plein` | 2783, 140, 37 |
| `hangnest_stalpost` | 2769, 134, 45 |
| `hangnest_winkeltje` | 2785, 146, 54 |
| `hangnest_woning_1` | 2765, 143, 27 |
| `hangnest_woning_2` | 2779, 139, 13 |
| `hangnest_woning_3` | 2795, 148, 11 |
| `hangnest_woning_4` | 2817, 151, 39 |
| `hangnest_woning_5` | 2757, 150, 37 |
| `hangnest_woning_6` | 2789, 152, 65 |
| `kaasschutterpost_1` | 2640, 221, 24 |
| `kaasschutterpost_2` | 2620, 218, -314 |
| `kaasschutterpost_3` | 2665, 207, 337 |
| `lianenbrug` | 2679, 196, -40 |
| `pluimpost_westtak` | 2690, 210, 36 |
| `valbrug` | 2752, 187, 160 |

## Stamspiraal + Schorsstad (Lantaarntoren rings: stand on the tower ring platform)

| id | x, y, z |
|---|---|
| `boomvolk_lichtje_1` | 2245, 100, 36 |
| `boomvolk_lichtje_2` | 2343, 132, 48 |
| `boomvolk_lichtje_3` | 2342, 164, -50 |
| `boomvolk_lichtje_4` | 2245, 84, -37 |
| `lantaarnring_1` | 2299, 84, 5 |
| `lantaarnring_1_kooi` | 2303, 84, -3 |
| `lantaarnring_2` | 2299, 100, 5 |
| `lantaarnring_2_kooi` | 2303, 100, -3 |
| `lantaarnring_3` | 2299, 116, 5 |
| `lantaarnring_3_kooi` | 2303, 116, -3 |
| `lantaarnring_4` | 2299, 132, 5 |
| `lantaarnring_4_kooi` | 2303, 132, -3 |
| `lantaarnring_5` | 2299, 148, 5 |
| `lantaarnring_5_kooi` | 2303, 148, -3 |
| `lantaarnring_6` | 2299, 164, 5 |
| `lantaarnring_6_kooi` | 2303, 164, -3 |
| `lantaarnring_7` | 2299, 180, 5 |
| `lantaarnring_7_kooi` | 2303, 180, -3 |
| `muurverhaal_1` | 2235, 81, 9 |
| `muurverhaal_2` | 2261, 97, 52 |
| `muurverhaal_3` | 2309, 113, 64 |
| `muurverhaal_4` | 2352, 129, 38 |
| `muurverhaal_5` | 2364, 145, -10 |
| `muurverhaal_6` | 2338, 161, -53 |
| `muurverhaal_7` | 2290, 177, -65 |
| `muurverhaal_8` | 2247, 81, -39 |
| `nestlantaarn_knoest` | 2299, 132, -4 |
| `npc_oude_knoest` | 2254, 130, 40 |
| `schorsstad_poort_128` | 2260, 129, 44 |
| `schorsstad_poort_176` | 2285, 177, 58 |
| `schorsstad_poort_75` | 2265, 75, 49 |
| `schorsstad_ring_1` | 2238, 81, 18 |
| `schorsstad_ring_2` | 2282, 97, 61 |
| `schorsstad_ring_3` | 2342, 113, 47 |
| `schorsstad_ring_4` | 2363, 129, -11 |
| `schorsstad_ring_5` | 2325, 145, -59 |
| `schorsstad_ring_6` | 2264, 161, -54 |
| `schorsstad_ring_7` | 2236, 177, 1 |
| `schorsstad_vloer` | 2279, 76, 30 |
| `spiraal_balkon_1` | 2209, 191, -35 |
| `spiraal_balkon_2` | 2322, 151, -93 |
| `spiraal_balkon_3` | 2400, 111, 12 |
| `spiraal_poort_128` | 2233, 129, 77 |
| `spiraal_poort_176` | 2276, 177, 96 |
| `spiraal_poort_75` | 2241, 76, 83 |
| `spiraal_top` | 2299, 201, -100 |
| `spiraal_voet` | 2299, 81, 156 |
| `spiraal_zweefsprong_1` | 2200, 191, -12 |
| `spiraal_zweefsprong_2` | 2299, 151, -100 |
| `spiraal_zweefsprong_3` | 2404, 111, -14 |

## Wortelvelden (Mosvoet, Pluimposten, valleys)

| id | x, y, z |
|---|---|
| `honingmoeras` | 1922, 68, 301 |
| `lichtkeverbos` | 2299, 74, -460 |
| `mosvoet_burgemeester` | 2087, 67, 430 |
| `mosvoet_plein` | 2049, 69, 424 |
| `mosvoet_stalpost` | 2009, 67, 438 |
| `mosvoet_theehuis` | 2079, 68, 394 |
| `mosvoet_waterrad` | 2029, 68, 446 |
| `mosvoet_winkel` | 2015, 72, 398 |
| `mosvoet_woning` | 2067, 65, 458 |
| `nachtgeheim_zadel` | 2239, 69, -520 |
| `paddenbloesemdal` | 2659, 70, 360 |
| `pluimpost_1` | 2300, 65, -514 |
| `pluimpost_2` | 2667, 70, -362 |
| `pluimpost_3` | 2653, 70, 359 |
| `pluimpost_4` | 1918, 71, 387 |
| `schimmelboerderij` | 2599, 70, -300 |
| `schimmelketel_1` | 2610, 67, -280 |
| `schimmelketel_2` | 2570, 73, -330 |
| `schimmelketel_3` | 2635, 68, -335 |
| `schimmelketel_4` | 2550, 69, -270 |
| `tijdrit_start` | 2300, 66, -517 |
| `wortelgewelf` | 1959, 76, -150 |
| `wortelpoort` | 2299, 63, 283 |

## Wortelduister

| id | x, y, z |
|---|---|
| `grotlantaarn_1` | 2293, 56, 268 |
| `grotlantaarn_2` | 2277, 53, 252 |
| `grotlantaarn_3` | 2253, 50, 230 |
| `grotlantaarn_4` | 2226, 45, 201 |
| `grotlantaarn_5` | 2203, 40, 170 |
| `grotlantaarn_6` | 2199, 34, 139 |
| `grotlantaarn_7` | 2205, 28, 110 |
| `grotlantaarn_8` | 2225, 22, 84 |
| `grotlantaarn_9` | 2243, 18, 66 |
| `hartslag_zaal` | 2299, 13, 30 |

## Secrets (knuffels, schatten, Mika-imker clues)

| id | x, y, z |
|---|---|
| `geheim_knuffel_1` | 2599, 230, 70 |
| `geheim_knuffel_10` | 2359, 24, 120 |
| `geheim_knuffel_2` | 2099, 253, -120 |
| `geheim_knuffel_3` | 2839, 137, 470 |
| `geheim_knuffel_4` | 2909, 141, -540 |
| `geheim_knuffel_5` | 2179, 215, 300 |
| `geheim_knuffel_6` | 1999, 67, 360 |
| `geheim_knuffel_7` | 2299, 68, -540 |
| `geheim_knuffel_8` | 2729, 66, 420 |
| `geheim_knuffel_9` | 2337, 161, 51 |
| `geheim_schat_1` | 2814, 140, 465 |
| `geheim_schat_2` | 2299, 286, -78 |
| `geheim_schat_3` | 2342, 177, -50 |
| `geheim_schat_4` | 2439, 22, 180 |
| `geheim_schat_5` | 1889, 67, 270 |
| `imker_aanwijzing_1` | 1847, 239, -12 |
| `imker_aanwijzing_2` | 1699, 226, 30 |
| `imker_aanwijzing_3` | 1739, 196, 8 |
| `imker_aanwijzing_4` | 1853, 239, 12 |
| `imker_aanwijzing_5` | 1728, 191, 2 |

## Landmarks

| id | x, y, z |
|---|---|
| `landmark_hoogvads_richting` | 2999, 260, 0 |

## Other

| id | x, y, z |
|---|---|
| `kuiken_1` | 2161, 257, -71 |
| `kuiken_2` | 2465, 270, 86 |
| `kuiken_3` | 2264, 270, 163 |
| `kuiken_4` | 2360, 265, -223 |
| `kuiken_5` | 2066, 264, 83 |
| `kuiken_6` | 2548, 235, 209 |
