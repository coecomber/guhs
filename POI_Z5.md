# POI_Z5 - coordinates of zone 5 "Hoogvads" (t-z5wereld slice)

**GENERATED** by `tools/vadsrijk/zone5/poi5.py` (via `python tools/vadsrijk/maak_kaart.py --zone 5`). Do not edit by hand. Java: `nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken` (`plek("id")`, `WIJKEN` / `wijk(x, y, z)`, `inZone5`, `HUIZEN`, `BEWONERS`, `VANGNET`, `KAMPVUREN`). Dev: `/guhs vadsrijk z5wereld plek <id>` teleports there.

**Layout.** Hoogvads lies on the back of Moeder Wolkwal, ABOVE the east end of zone 4 (same x/z): whale x 2440..3015 (head WEST at x ~2440 over the Opstijgpier, tail fluke east to ~3015), axis z -520, ~240 wide. The whale is **lowered 20 blocks** compared to the concept export (night round T, decision 2): belly y ~134, city y ~214..236, cathedral floor y 224, Grote Poort ~58 high, spires to y ~316. Everything of zone 5 is from y 118 up (`MIN_Y`); tiles tx 38..48, tz -12..-5 (tx 38..39 shared with zone 4: zone 4 below y 118).

**Placeholders of other slices:** every id of `Z5VerhaalPlekken` (t-z5verhaal) and `Z5WezensPlekken` (t-z5wezens) is in the list below with its real spot. t-z5wezens: your rooms inside the whale (Buikgangen, Schuilnest) and every render-y placeholder move down 20 with the whale: `z5w_buikgangen_min/_max`, `z5w_schuilnest_min/_max` are your boxes shifted; subtract 20 from BUIK_Y / NEST_Y and from your area y-ranges (wolkrand/staartvlakte).

All positions are **stand positions** (the air block a player/NPC stands in) in `guhs:vadsrijk`, unless noted (`haven_5_4_luchtschip` = the middle of the docked ship, `haven_5_4_mast` = top of the mooring mast).

## Districts (`Z5WereldPlekken.WIJKEN`, first match wins, from y 118 up; biome `guhs:vadsrijk_<biome>`)

| id | x1, z1 .. x2, z2 | biome | name |
|---|---|---|---|
| `z5_paleisheuvel` | 2778, -570 .. 2858, -470 | hoogvads_paleis | Hoogvads - de Paleisheuvel |
| `z5_universiteit` | 2694, -494 .. 2780, -434 | hoogvads_universiteit | Hoogvads - de Universiteit |
| `z5_academie` | 2694, -606 .. 2780, -546 | hoogvads_academie | Hoogvads - de Academie van de Wolkwacht |
| `z5_marktlaan` | 2708, -545 .. 2778, -495 | hoogvads_markt | Hoogvads - de Marktlaan |
| `z5_kathedraal` | 2572, -572 .. 2708, -468 | hoogvads_kathedraal | Hoogvads - de Ordekathedraal |
| `z5_luchthaven` | 2578, -467 .. 2682, -345 | hoogvads_luchthaven | Hoogvads - de Luchthaven |
| `z5_staartpark` | 2855, -584 .. 2904, -456 | hoogvads_staartpark | Hoogvads - het Staartpark |
| `z5_ooggaarde` | 2526, -620 .. 2576, -546 | hoogvads_ooggaarde | Hoogvads - de Ooggaarde |
| `z5_hoogvads` | 2526, -625 .. 2907, -415 | hoogvads | Hoogvads |
| `z5_moeder_wolkwal` | 2432, -702 .. 3022, -338 | moeder_wolkwal | Moeder Wolkwal |

## All spots

| id | x, y, z |
|---|---|
| `academie_kapitein` | 2737, 215, -591 |
| `academie_kastjes` | 2728, 215, -595 |
| `academie_kazerne` | 2746, 215, -590 |
| `academie_oefenplein` | 2752, 215, -568 |
| `academie_parcours_1` | 2722, 215, -565 |
| `academie_parcours_2` | 2752, 215, -565 |
| `academie_parcours_3` | 2752, 215, -590 |
| `academie_parcours_4` | 2722, 215, -590 |
| `academie_poort` | 2737, 215, -548 |
| `academie_toren` | 2723, 248, -581 |
| `academie_toren_voet` | 2723, 215, -582 |
| `balkon_oog` | 2552, 214, -614 |
| `bank_balie` | 2662, 219, -474 |
| `bank_hoogvads_balie` | 2662, 219, -474 |
| `bank_hoogvads_boven` | 2665, 224, -478 |
| `bank_hoogvads_deur` | 2663, 219, -472 |
| `burger_1` | 2700, 221, -510 |
| `burger_2` | 2760, 219, -512 |
| `burger_3` | 2690, 221, -530 |
| `burger_4` | 2863, 204, -514 |
| `burger_5` | 2560, 222, -570 |
| `burger_6` | 2640, 217, -452 |
| `geheim_knuffel_1` | 2690, 221, -505 |
| `geheim_knuffel_2` | 2905, 203, -540 |
| `geheim_knuffel_3` | 2545, 212, -600 |
| `geheim_knuffel_4` | 2665, 204, -418 |
| `geheim_knuffel_5` | 2762, 215, -448 |
| `geheim_knuffel_6` | 2772, 215, -591 |
| `geheim_knuffel_7` | 2835, 233, -520 |
| `geheim_knuffel_8` | 2655, 225, -540 |
| `geheim_schat_1` | 2912, 206, -520 |
| `geheim_schat_2` | 2838, 233, -530 |
| `geheim_schat_3` | 2540, 220, -560 |
| `gilde_atelier` | 2702, 215, -459 |
| `gilde_bijenstal` | 2702, 215, -454 |
| `gilde_brouwerij` | 2702, 215, -469 |
| `gilde_hal` | 2702, 215, -484 |
| `gilde_ingang` | 2695, 215, -483 |
| `gilde_keuken` | 2702, 215, -474 |
| `gilde_smidse` | 2702, 215, -464 |
| `haven_5_1_vertrekbord` | 2646, 217, -441 |
| `haven_5_4_bel` | 2631, 217, -359 |
| `haven_5_4_kapitein` | 2632, 217, -370 |
| `haven_5_4_landing` | 2630, 217, -362 |
| `haven_5_4_luchtschip` | 2630, 216, -342 |
| `haven_5_4_mast` | 2633, 225, -352 |
| `haven_5_4_vertrekbord` | 2626, 217, -441 |
| `haven_5_6_vertrekbord` | 2606, 217, -441 |
| `haven_5_pier_1` | 2650, 217, -365 |
| `haven_5_pier_2` | 2630, 217, -365 |
| `haven_5_pier_3` | 2610, 217, -365 |
| `haven_z5_z4_vertrek` | 2630, 217, -352 |
| `haven_z5_z6_vertrek` | 2966, 210, -520 |
| `herberg_hemelbed_balie` | 2690, 218, -572 |
| `herberg_hemelbed_boven` | 2690, 223, -564 |
| `herberg_hemelbed_deur` | 2689, 218, -574 |
| `huis_bank_hoogvads` | 2662, 219, -476 |
| `huis_herberg_hemelbed` | 2690, 218, -570 |
| `huis_winkel_bakker` | 2563, 224, -494 |
| `huis_winkel_juwelen` | 2617, 214, -602 |
| `huis_winkel_kaarten` | 2543, 220, -469 |
| `huis_winkel_kleding` | 2677, 218, -474 |
| `huis_winkel_kruiden` | 2639, 215, -598 |
| `huis_winkel_smid` | 2681, 218, -568 |
| `huis_winkel_speelgoed` | 2569, 224, -541 |
| `huis_winkel_wapens` | 2681, 215, -589 |
| `huis_woning_1` | 2659, 214, -597 |
| `huis_woning_10` | 2655, 219, -566 |
| `huis_woning_11` | 2583, 218, -589 |
| `huis_woning_12` | 2669, 214, -597 |
| `huis_woning_13` | 2592, 215, -600 |
| `huis_woning_14` | 2628, 214, -598 |
| `huis_woning_15` | 2619, 216, -588 |
| `huis_woning_16` | 2650, 219, -477 |
| `huis_woning_17` | 2582, 222, -477 |
| `huis_woning_18` | 2597, 221, -475 |
| `huis_woning_19` | 2640, 219, -477 |
| `huis_woning_2` | 2591, 218, -588 |
| `huis_woning_20` | 2560, 224, -542 |
| `huis_woning_21` | 2569, 222, -472 |
| `huis_woning_22` | 2545, 222, -480 |
| `huis_woning_23` | 2569, 216, -442 |
| `huis_woning_24` | 2574, 218, -591 |
| `huis_woning_25` | 2560, 223, -478 |
| `huis_woning_26` | 2544, 223, -508 |
| `huis_woning_27` | 2544, 223, -494 |
| `huis_woning_28` | 2544, 221, -567 |
| `huis_woning_29` | 2544, 223, -530 |
| `huis_woning_3` | 2668, 218, -569 |
| `huis_woning_30` | 2543, 217, -450 |
| `huis_woning_31` | 2565, 224, -507 |
| `huis_woning_32` | 2560, 222, -568 |
| `huis_woning_33` | 2563, 216, -596 |
| `huis_woning_34` | 2544, 216, -592 |
| `huis_woning_35` | 2569, 224, -531 |
| `huis_woning_36` | 2571, 219, -452 |
| `huis_woning_37` | 2543, 222, -542 |
| `huis_woning_38` | 2559, 224, -530 |
| `huis_woning_39` | 2690, 216, -461 |
| `huis_woning_4` | 2641, 219, -569 |
| `huis_woning_40` | 2687, 218, -473 |
| `huis_woning_41` | 2689, 215, -449 |
| `huis_woning_5` | 2602, 215, -598 |
| `huis_woning_6` | 2585, 221, -570 |
| `huis_woning_7` | 2597, 220, -570 |
| `huis_woning_8` | 2577, 215, -600 |
| `huis_woning_9` | 2650, 213, -601 |
| `kampvuur_academie` | 2762, 215, -556 |
| `kampvuur_kathedraal` | 2694, 221, -534 |
| `kampvuur_luchthaven` | 2668, 217, -454 |
| `kampvuur_markt` | 2760, 219, -516 |
| `kampvuur_ooggaarde` | 2556, 220, -572 |
| `kampvuur_paleis` | 2798, 233, -494 |
| `kampvuur_staartpark` | 2868, 204, -510 |
| `kathedraal_altaar` | 2624, 228, -520 |
| `kathedraal_apsis` | 2600, 225, -520 |
| `kathedraal_geveltoren` | 2682, 270, -544 |
| `kathedraal_geveltoren_voet` | 2669, 225, -540 |
| `kathedraal_plein` | 2696, 221, -520 |
| `kathedraal_poort` | 2683, 225, -520 |
| `kathedraal_roos_noord` | 2619, 225, -478 |
| `kathedraal_roos_zuid` | 2619, 225, -562 |
| `kathedraal_schip` | 2664, 225, -520 |
| `luchthaven_aankomst` | 2630, 217, -420 |
| `luchthaven_balie` | 2623, 217, -449 |
| `luchthaven_havenmeester` | 2623, 217, -446 |
| `luchthaven_klokkentoren` | 2635, 217, -457 |
| `luchthaven_logboek` | 2618, 217, -448 |
| `luchthaven_pier_oost` | 2610, 217, -400 |
| `luchthaven_pier_west` | 2650, 217, -400 |
| `luchthaven_station` | 2635, 217, -452 |
| `luchthaven_vertrekbord` | 2626, 217, -441 |
| `markt_bakker` | 2764, 219, -508 |
| `markt_fontein` | 2748, 219, -529 |
| `markt_juwelier` | 2764, 219, -532 |
| `markt_kleermaker` | 2756, 219, -532 |
| `markt_smid` | 2741, 219, -508 |
| `ooggaarde_tuin` | 2552, 219, -580 |
| `paleis_hof` | 2802, 233, -546 |
| `paleis_kanselarij` | 2839, 233, -550 |
| `paleis_kanselarij_bureau` | 2843, 233, -557 |
| `paleis_poort` | 2822, 233, -520 |
| `paleis_trap` | 2762, 219, -520 |
| `raadszaal_ingang` | 2788, 236, -520 |
| `raadszaal_lessenaar` | 2805, 237, -520 |
| `raadszaal_zaal` | 2798, 237, -528 |
| `raadszaal_zijkamer` | 2811, 237, -509 |
| `soepkeuken_bank` | 2731, 219, -532 |
| `soepkeuken_hoek` | 2716, 219, -540 |
| `soepkeuken_ingang` | 2724, 219, -524 |
| `soepkeuken_ketel` | 2724, 219, -532 |
| `soepkeuken_rij_1` | 2719, 219, -527 |
| `soepkeuken_rij_2` | 2721, 219, -527 |
| `soepkeuken_rij_3` | 2722, 219, -527 |
| `soepkeuken_rij_4` | 2724, 219, -527 |
| `soepkeuken_tafel` | 2729, 219, -528 |
| `spuitgat_midden` | 2552, 223, -520 |
| `spuitgat_noord` | 2563, 224, -495 |
| `spuitgat_zuid` | 2563, 223, -545 |
| `staartpark_boom_1` | 2866, 203, -542 |
| `staartpark_boom_2` | 2890, 204, -535 |
| `staartpark_boom_3` | 2890, 204, -505 |
| `staartpark_boom_4` | 2870, 203, -500 |
| `staartpark_prieel` | 2880, 203, -502 |
| `staartpark_vijver` | 2876, 204, -524 |
| `staartpark_westeinde` | 2896, 205, -520 |
| `staarttrap_boven` | 2856, 233, -531 |
| `staarttrap_onder` | 2860, 204, -530 |
| `staartvin_top` | 2905, 206, -520 |
| `troonzaal_audientie` | 2840, 233, -520 |
| `troonzaal_ingang` | 2831, 233, -520 |
| `troonzaal_troon` | 2847, 238, -520 |
| `universiteit_archief` | 2772, 215, -459 |
| `universiteit_bibliotheek` | 2751, 215, -446 |
| `universiteit_bibliotheek_galerij` | 2752, 221, -443 |
| `universiteit_binnenhof` | 2737, 215, -471 |
| `universiteit_collegezaal` | 2773, 215, -480 |
| `universiteit_poort` | 2737, 215, -492 |
| `universiteit_sterrenwacht` | 2706, 215, -444 |
| `universiteit_sterrenwacht_top` | 2702, 260, -446 |
| `winkel_bakker_balie` | 2561, 224, -494 |
| `winkel_bakker_boven` | 2566, 229, -495 |
| `winkel_bakker_deur` | 2559, 224, -495 |
| `winkel_juwelen_balie` | 2617, 214, -604 |
| `winkel_juwelen_deur` | 2617, 214, -606 |
| `winkel_kaarten_balie` | 2543, 220, -467 |
| `winkel_kaarten_deur` | 2544, 220, -465 |
| `winkel_kleding_balie` | 2677, 218, -472 |
| `winkel_kleding_deur` | 2678, 218, -470 |
| `winkel_kruiden_balie` | 2639, 215, -600 |
| `winkel_kruiden_boven` | 2638, 220, -593 |
| `winkel_kruiden_deur` | 2639, 215, -602 |
| `winkel_smid_balie` | 2681, 218, -570 |
| `winkel_smid_boven` | 2678, 223, -565 |
| `winkel_smid_deur` | 2680, 218, -572 |
| `winkel_speelgoed_balie` | 2569, 224, -543 |
| `winkel_speelgoed_deur` | 2569, 224, -545 |
| `winkel_wapens_balie` | 2681, 215, -587 |
| `winkel_wapens_boven` | 2683, 220, -592 |
| `winkel_wapens_deur` | 2682, 215, -585 |
| `wolbol_1` | 2745, 219, -528 |
| `wolbol_2` | 2820, 233, -512 |
| `wolbol_3` | 2735, 215, -472 |
| `wolkbes_1` | 2860, 202, -545 |
| `wolkbes_2` | 2885, 200, -548 |
| `wolkbes_3` | 2899, 205, -520 |
| `wolkbes_4` | 2884, 200, -492 |
| `wolkbes_5` | 2862, 203, -495 |
| `wolkbes_6` | 2540, 218, -575 |
| `wolkbes_7` | 2548, 216, -590 |
| `wolkbes_8` | 2565, 218, -585 |
| `wolkwacht_1` | 2742, 219, -504 |
| `wolkwacht_2` | 2742, 219, -536 |
| `wolkwacht_3` | 2683, 225, -512 |
| `wolkwacht_4` | 2645, 217, -452 |
| `wolkwacht_5` | 2868, 204, -506 |
| `wolkwacht_6` | 2556, 221, -568 |
| `wolkwaldroom_arena` | 2602, 167, -520 |
| `z5w_buik_ladder_onder` | 2733, 165, -539 |
| `z5w_buik_luik` | 2732, 219, -541 |
| `z5w_buik_uit` | 2731, 219, -539 |
| `z5w_buikgangen_max` | 2808, 176, -508 |
| `z5w_buikgangen_min` | 2704, 164, -530 |
| `z5w_donderbuik_arena` | 2968, 206, -582 |
| `z5w_donderbuik_deur` | 2951, 207, -569 |
| `z5w_leider_mistmoeder` | 2970, 206, -450 |
| `z5w_leider_strobaal` | 2714, 215, -578 |
| `z5w_lift_noordvin_boven` | 2609, 217, -432 |
| `z5w_lift_noordvin_onder` | 2610, 154, -429 |
| `z5w_lift_staart_boven` | 2896, 205, -522 |
| `z5w_lift_staart_onder` | 2952, 212, -520 |
| `z5w_lift_zuidvin_boven` | 2609, 210, -608 |
| `z5w_lift_zuidvin_onder` | 2610, 154, -611 |
| `z5w_noordvin` | 2610, 154, -392 |
| `z5w_proefarena` | 2714, 215, -570 |
| `z5w_schuilnest_ladder_onder` | 2611, 187, -441 |
| `z5w_schuilnest_luik` | 2612, 217, -444 |
| `z5w_schuilnest_max` | 2624, 194, -414 |
| `z5w_schuilnest_min` | 2588, 186, -440 |
| `z5w_schuilnest_uit` | 2613, 217, -445 |
| `z5w_staartvlakte_noord` | 2968, 206, -458 |
| `z5w_wolkwaldroom_deur` | 2587, 167, -520 |
| `z5w_wolkwaldroom_uitluik` | 2586, 167, -520 |
| `z5w_zuidvin` | 2610, 154, -648 |
| `zachtmoedlaan` | 2704, 221, -520 |

## Houses with a role (`HUIZEN`)

| id | role | door | inside |
|---|---|---|---|
| `winkel_wapens` | winkel | 2682, 215, -585 | 2681, 215, -589 |
| `woning_1` | woning | 2658, 214, -601 | 2659, 214, -597 |
| `woning_2` | woning | 2592, 218, -585 | 2591, 218, -588 |
| `winkel_kruiden` | winkel | 2639, 215, -602 | 2639, 215, -598 |
| `woning_3` | woning | 2667, 218, -574 | 2668, 218, -569 |
| `winkel_juwelen` | winkel | 2617, 214, -606 | 2617, 214, -602 |
| `woning_4` | woning | 2640, 219, -573 | 2641, 219, -569 |
| `woning_5` | woning | 2601, 215, -604 | 2602, 215, -598 |
| `woning_6` | woning | 2584, 221, -574 | 2585, 221, -570 |
| `woning_7` | woning | 2597, 220, -573 | 2597, 220, -570 |
| `woning_8` | woning | 2577, 215, -603 | 2577, 215, -600 |
| `woning_9` | woning | 2649, 213, -606 | 2650, 213, -601 |
| `woning_10` | woning | 2655, 219, -572 | 2655, 219, -566 |
| `woning_11` | woning | 2584, 218, -585 | 2583, 218, -589 |
| `woning_12` | woning | 2669, 214, -602 | 2669, 214, -597 |
| `woning_13` | woning | 2591, 215, -603 | 2592, 215, -600 |
| `winkel_smid` | winkel | 2680, 218, -572 | 2681, 218, -568 |
| `woning_14` | woning | 2627, 214, -601 | 2628, 214, -598 |
| `woning_15` | woning | 2620, 216, -585 | 2619, 216, -588 |
| `woning_16` | woning | 2649, 219, -480 | 2650, 219, -477 |
| `winkel_kleding` | winkel | 2678, 218, -470 | 2677, 218, -474 |
| `woning_17` | woning | 2581, 222, -480 | 2582, 222, -477 |
| `woning_18` | woning | 2598, 221, -470 | 2597, 221, -475 |
| `bank_hoogvads` | bank | 2663, 219, -472 | 2662, 219, -476 |
| `woning_19` | woning | 2639, 219, -481 | 2640, 219, -477 |
| `woning_20` | woning | 2557, 224, -542 | 2560, 224, -542 |
| `winkel_bakker` | winkel | 2559, 224, -495 | 2563, 224, -494 |
| `woning_21` | woning | 2569, 222, -467 | 2569, 222, -472 |
| `woning_22` | woning | 2548, 222, -480 | 2545, 222, -480 |
| `woning_23` | woning | 2569, 216, -437 | 2569, 216, -442 |
| `woning_24` | woning | 2575, 218, -587 | 2574, 218, -591 |
| `woning_25` | woning | 2557, 223, -478 | 2560, 223, -478 |
| `woning_26` | woning | 2541, 223, -509 | 2544, 223, -508 |
| `woning_27` | woning | 2540, 223, -494 | 2544, 223, -494 |
| `winkel_speelgoed` | winkel | 2569, 224, -545 | 2569, 224, -541 |
| `woning_28` | woning | 2541, 221, -567 | 2544, 221, -567 |
| `woning_29` | woning | 2540, 223, -531 | 2544, 223, -530 |
| `woning_30` | woning | 2540, 217, -451 | 2543, 217, -450 |
| `woning_31` | woning | 2565, 224, -512 | 2565, 224, -507 |
| `woning_32` | woning | 2557, 222, -569 | 2560, 222, -568 |
| `woning_33` | woning | 2559, 216, -596 | 2563, 216, -596 |
| `woning_34` | woning | 2541, 216, -592 | 2544, 216, -592 |
| `woning_35` | woning | 2569, 224, -527 | 2569, 224, -531 |
| `woning_36` | woning | 2571, 219, -455 | 2571, 219, -452 |
| `woning_37` | woning | 2540, 222, -542 | 2543, 222, -542 |
| `woning_38` | woning | 2560, 224, -525 | 2559, 224, -530 |
| `winkel_kaarten` | winkel | 2544, 220, -465 | 2543, 220, -469 |
| `herberg_hemelbed` | herberg | 2689, 218, -574 | 2690, 218, -570 |
| `woning_39` | woning | 2687, 216, -461 | 2690, 216, -461 |
| `woning_40` | woning | 2686, 218, -477 | 2687, 218, -473 |
| `woning_41` | woning | 2690, 215, -444 | 2689, 215, -449 |
