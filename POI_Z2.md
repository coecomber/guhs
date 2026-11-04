# POI_Z2 - coordinates of zone 2 "De Splinterpieken" (z2-wereld slice)

**v1, GENERATED** by `tools/vadsrijk/maak_kaart.py` (zone-2 part `tools/vadsrijk/zone2/`) from the real map; the x/z/y of the plan (v0) are kept. Do not edit by hand; requests go to `../POI_VERZOEKEN_Z2.md`. Java: `nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken` (`plek("id")`, `GEBIEDEN`, `MOBZONES`, `KAMPVUREN`, `DEUREN`).

All positions are **stand positions** (the air block a player/NPC stands in) in `guhs:vadsrijk`, unless noted (`doos` = box corners, door `min/max` = the door's own blocks, `*_straal` = radius in x). Cloud sea level 62; Kandijkom floor stand y 91; Spiegelburcht floor stand y 176. Dev: `/guhs vadsrijk z2wereld plek <id>` teleports there.

## Areas (kern.Gebieden, first match wins)

| id | box (minX, minZ)..(maxX, maxZ) | levels | town |
|---|---|---|---|
| `z2_spiegelburcht` | (110,-2775)..(190,-2618) | 28-30 | no |
| `z2_kandijkom_dok` | (-40,-2317)..(40,-2200) | 20-30 | yes |
| `z2_kandijkom` | (-84,-2484)..(84,-2318) | 20-30 | yes |
| `z2_splinterkloof` | (-260,-2775)..(260,-2505) | 26-29 | no |
| `z2_galmhellingen` | (85,-2504)..(260,-2200) | 20-23 | no |
| `z2_knuffelmeer` | (-260,-2504)..(-85,-2200) | 23-26 | no |
| `z2_ring` | (-300,-2820)..(300,-2150) | 20-30 | no |

## Mob zones (boxes for the wezens slice; `Z2WereldPlekken.MOBZONES`)

| id | box | levels |
|---|---|---|
| `mob_galm_voet` | (86,-2400)..(250,-2200) | 20-21 |
| `mob_galm_top` | (86,-2504)..(250,-2401) | 22-23 |
| `mob_meer_oever` | (-250,-2330)..(-86,-2200) | 23-24 |
| `mob_meer_ijs` | (-226,-2448)..(-118,-2331) | 24-25 |
| `mob_meer_noord` | (-250,-2504)..(-86,-2449) | 25-26 |
| `mob_kloof_zuid` | (-236,-2528)..(236,-2505) | 26-27 |
| `mob_kloof_bodem` | (-236,-2565)..(236,-2529) | 27-28 |
| `mob_kloof_noord` | (-236,-2775)..(109,-2566) | 28-29 |
| `mob_burcht` | (131,-2762)..(169,-2634) | 28-30 |

## Moving door

`spiegelburcht` (WereldDeuren id): blocks [147, 176, -2634]..[153, 184, -2634] (wings slide along x), stand in front [150, 176, -2629], hidden copy at y 8. `WereldDeuren.open("spiegelburcht", player)` / `open(level, id)` / `sluit(level, id)`.

## Travel (airship dock, gondola)

| id | x, y, z |
|---|---|
| `dok_bel` | 4, 91, -2232 |
| `dok_eind` | 0, 91, -2226 |
| `dok_kapitein` | -4, 91, -2240 |
| `dok_landing` | 0, 91, -2244 |
| `gondel_berg` | 182, 151, -2476 |
| `gondel_berg_kabel` | 182, 159, -2478 |
| `gondel_berg_knop` | 185, 151, -2475 |
| `gondel_dal` | 30, 101, -2449 |
| `gondel_dal_kabel` | 30, 109, -2451 |
| `gondel_dal_knop` | 33, 101, -2448 |
| `gondel_kabel_berg` | 182, 159, -2478 |
| `gondel_kabel_dal` | 30, 109, -2451 |
| `npc_gondel` | 28, 101, -2448 |

## Kandijkom: plaza, NPC spots, secrets in town

| id | x, y, z |
|---|---|
| `cafe_terras` | -24, 91, -2386 |
| `geheim_chocokelder` | -26, 85, -2377 |
| `kristalkapel_altaar` | -30, 101, -2455 |
| `npc_bank` | 23, 91, -2374 |
| `npc_burgemeester` | 0, 91, -2400 |
| `npc_cafe` | -24, 91, -2374 |
| `npc_ijsvisser` | -184, 88, -2392 |
| `npc_juwelier` | -23, 91, -2427 |
| `npc_kristalkapel` | -30, 101, -2453 |
| `npc_schaatsguh` | 9, 91, -2385 |
| `npc_smid` | 23, 91, -2427 |
| `npc_stal` | 20, 91, -2297 |
| `npc_winkel_kandij` | 41, 96, -2422 |
| `npc_winkel_wantjes` | -41, 96, -2422 |
| `plein` | 0, 91, -2412 |
| `questbord` | 12, 91, -2419 |
| `schaatsvijver` | 0, 91, -2385 |
| `sneeuwguhspar` | -16, 91, -2405 |

## Kandijkom buildings (deur = stand outside the door, binnen = interior centre, npc = behind the counter, doos = box)

| id | x, y, z |
|---|---|
| `gebouw.bank.binnen` | 23, 91, -2377 |
| `gebouw.bank.deur` | 23, 91, -2384 |
| `gebouw.bank.doos.1` | 18, 90, -2382 |
| `gebouw.bank.doos.2` | 28, 97, -2372 |
| `gebouw.bank.npc` | 23, 91, -2374 |
| `gebouw.bank.toonbank` | 23, 91, -2376 |
| `gebouw.cafe.binnen` | -24, 91, -2377 |
| `gebouw.cafe.deur` | -24, 91, -2384 |
| `gebouw.cafe.doos.1` | -30, 90, -2382 |
| `gebouw.cafe.doos.2` | -18, 97, -2372 |
| `gebouw.cafe.npc` | -24, 91, -2374 |
| `gebouw.cafe.toonbank` | -24, 91, -2376 |
| `gebouw.huis_1.binnen` | 22, 96, -2438 |
| `gebouw.huis_1.deur` | 22, 96, -2432 |
| `gebouw.huis_1.doos.1` | 17, 95, -2442 |
| `gebouw.huis_1.doos.2` | 27, 101, -2434 |
| `gebouw.huis_10.binnen` | -30, 101, -2349 |
| `gebouw.huis_10.deur` | -30, 101, -2355 |
| `gebouw.huis_10.doos.1` | -35, 100, -2353 |
| `gebouw.huis_10.doos.2` | -25, 106, -2345 |
| `gebouw.huis_11.binnen` | 51, 101, -2370 |
| `gebouw.huis_11.deur` | 44, 101, -2370 |
| `gebouw.huis_11.doos.1` | 46, 100, -2374 |
| `gebouw.huis_11.doos.2` | 56, 106, -2366 |
| `gebouw.huis_12.binnen` | 30, 101, -2349 |
| `gebouw.huis_12.deur` | 30, 101, -2355 |
| `gebouw.huis_12.doos.1` | 25, 100, -2353 |
| `gebouw.huis_12.doos.2` | 35, 106, -2345 |
| `gebouw.huis_2.binnen` | -22, 96, -2438 |
| `gebouw.huis_2.deur` | -22, 96, -2432 |
| `gebouw.huis_2.doos.1` | -27, 95, -2442 |
| `gebouw.huis_2.doos.2` | -17, 101, -2434 |
| `gebouw.huis_3.binnen` | -38, 96, -2378 |
| `gebouw.huis_3.deur` | -31, 96, -2378 |
| `gebouw.huis_3.doos.1` | -43, 95, -2382 |
| `gebouw.huis_3.doos.2` | -33, 101, -2374 |
| `gebouw.huis_4.binnen` | -22, 96, -2362 |
| `gebouw.huis_4.deur` | -22, 96, -2368 |
| `gebouw.huis_4.doos.1` | -27, 95, -2366 |
| `gebouw.huis_4.doos.2` | -17, 101, -2358 |
| `gebouw.huis_5.binnen` | 38, 96, -2378 |
| `gebouw.huis_5.deur` | 31, 96, -2378 |
| `gebouw.huis_5.doos.1` | 33, 95, -2382 |
| `gebouw.huis_5.doos.2` | 43, 101, -2374 |
| `gebouw.huis_6.binnen` | 22, 96, -2362 |
| `gebouw.huis_6.deur` | 22, 96, -2368 |
| `gebouw.huis_6.doos.1` | 17, 95, -2366 |
| `gebouw.huis_6.doos.2` | 27, 101, -2358 |
| `gebouw.huis_7.binnen` | 51, 101, -2430 |
| `gebouw.huis_7.deur` | 44, 101, -2430 |
| `gebouw.huis_7.doos.1` | 46, 100, -2434 |
| `gebouw.huis_7.doos.2` | 56, 106, -2426 |
| `gebouw.huis_8.binnen` | -51, 101, -2430 |
| `gebouw.huis_8.deur` | -44, 101, -2430 |
| `gebouw.huis_8.doos.1` | -56, 100, -2434 |
| `gebouw.huis_8.doos.2` | -46, 106, -2426 |
| `gebouw.huis_9.binnen` | -51, 101, -2370 |
| `gebouw.huis_9.deur` | -44, 101, -2370 |
| `gebouw.huis_9.doos.1` | -56, 100, -2374 |
| `gebouw.huis_9.doos.2` | -46, 106, -2366 |
| `gebouw.juwelier.binnen` | -23, 91, -2424 |
| `gebouw.juwelier.deur` | -23, 91, -2417 |
| `gebouw.juwelier.doos.1` | -28, 90, -2429 |
| `gebouw.juwelier.doos.2` | -18, 97, -2419 |
| `gebouw.juwelier.npc` | -23, 91, -2427 |
| `gebouw.juwelier.toonbank` | -23, 91, -2425 |
| `gebouw.kristalkapel.altaar` | -30, 101, -2455 |
| `gebouw.kristalkapel.binnen` | -30, 101, -2451 |
| `gebouw.kristalkapel.deur` | -30, 101, -2444 |
| `gebouw.kristalkapel.doos.1` | -36, 100, -2456 |
| `gebouw.kristalkapel.doos.2` | -24, 111, -2446 |
| `gebouw.kristalkapel.npc` | -30, 101, -2453 |
| `gebouw.smid.binnen` | 23, 91, -2424 |
| `gebouw.smid.deur` | 23, 91, -2417 |
| `gebouw.smid.doos.1` | 18, 90, -2429 |
| `gebouw.smid.doos.2` | 28, 97, -2419 |
| `gebouw.smid.npc` | 23, 91, -2427 |
| `gebouw.smid.toonbank` | 23, 91, -2425 |
| `gebouw.stal.binnen` | 26, 91, -2296 |
| `gebouw.stal.deur` | 19, 91, -2296 |
| `gebouw.stal.doos.1` | 21, 90, -2300 |
| `gebouw.stal.doos.2` | 31, 101, -2292 |
| `gebouw.stal.npc` | 20, 91, -2297 |
| `gebouw.stal.wei` | 26, 91, -2284 |
| `gebouw.winkel_kandij.binnen` | 38, 96, -2422 |
| `gebouw.winkel_kandij.deur` | 31, 96, -2422 |
| `gebouw.winkel_kandij.doos.1` | 33, 95, -2426 |
| `gebouw.winkel_kandij.doos.2` | 43, 102, -2418 |
| `gebouw.winkel_kandij.npc` | 41, 96, -2422 |
| `gebouw.winkel_kandij.toonbank` | 39, 96, -2422 |
| `gebouw.winkel_wantjes.binnen` | -38, 96, -2422 |
| `gebouw.winkel_wantjes.deur` | -31, 96, -2422 |
| `gebouw.winkel_wantjes.doos.1` | -43, 95, -2426 |
| `gebouw.winkel_wantjes.doos.2` | -33, 102, -2418 |
| `gebouw.winkel_wantjes.npc` | -41, 96, -2422 |
| `gebouw.winkel_wantjes.toonbank` | -39, 96, -2422 |

## Arenas and bosses

| id | x, y, z |
|---|---|
| `arena_boormeester` | -30, 66, -2614 |
| `arena_boormeester_deur` | -30, 66, -2559 |
| `arena_boormeester_straal` | 20, 0, 0 |
| `arena_galmwolf` | 182, 117, -2318 |
| `arena_galmwolf_bord` | 147, 117, -2314 |
| `arena_galmwolf_deur` | 146, 115, -2318 |
| `arena_spiegelzucht` | 150, 176, -2741 |
| `arena_spiegelzucht_bord` | 154, 176, -2714 |
| `arena_spiegelzucht_deur` | 150, 176, -2715 |
| `arena_spiegelzucht_straal` | 22, 0, 0 |
| `boormeester_kop_1` | -30, 66, -2602 |
| `boormeester_kop_2` | -40, 66, -2620 |
| `boormeester_kop_3` | -20, 66, -2620 |
| `galmwolf_naald` | 182, 146, -2358 |
| `galmwolf_pilaar_1` | 201, 117, -2314 |
| `galmwolf_pilaar_2` | 192, 117, -2302 |
| `galmwolf_pilaar_3` | 178, 117, -2299 |
| `galmwolf_pilaar_4` | 166, 117, -2308 |
| `galmwolf_pilaar_5` | 163, 117, -2322 |
| `galmwolf_pilaar_6` | 172, 117, -2334 |
| `galmwolf_pilaar_7` | 186, 117, -2337 |
| `galmwolf_pilaar_8` | 198, 117, -2328 |
| `spiegelzucht_spiegel_1` | 167, 176, -2741 |
| `spiegelzucht_spiegel_2` | 159, 176, -2726 |
| `spiegelzucht_spiegel_3` | 142, 176, -2726 |
| `spiegelzucht_spiegel_4` | 133, 176, -2741 |
| `spiegelzucht_spiegel_5` | 141, 176, -2756 |
| `spiegelzucht_spiegel_6` | 159, 176, -2756 |

## De Spiegelburcht (dungeon): door, rooms, gates (open AIR, bazen fills), puzzle spots

| id | x, y, z |
|---|---|
| `burcht_lichtkamer` | 150, 176, -2668 |
| `burcht_prismazaal` | 150, 176, -2706 |
| `burcht_spiegelgang` | 150, 176, -2687 |
| `burcht_splinter` | 150, 176, -2756 |
| `burcht_trap_voet` | 150, 137, -2561 |
| `burcht_voorhal` | 150, 176, -2646 |
| `burcht_voorplein` | 150, 176, -2611 |
| `deur_spiegelburcht_voor` | 150, 176, -2629 |
| `poort_1` | 150, 176, -2658 |
| `poort_2` | 150, 176, -2678 |
| `poort_3` | 150, 176, -2696 |
| `poort_4` | 150, 176, -2716 |
| `puzzel_lichtkamer_1` | 143, 176, -2672 |
| `puzzel_lichtkamer_2` | 157, 176, -2672 |
| `puzzel_lichtkamer_3` | 143, 176, -2663 |
| `puzzel_lichtkamer_4` | 157, 176, -2663 |
| `puzzel_lichtkamer_5` | 150, 176, -2668 |
| `puzzel_lichtkamer_bron` | 136, 178, -2668 |
| `puzzel_lichtkamer_doel` | 150, 182, -2677 |
| `puzzel_prismazaal_1` | 150, 176, -2706 |
| `puzzel_prismazaal_2` | 142, 176, -2701 |
| `puzzel_prismazaal_3` | 158, 176, -2701 |
| `puzzel_prismazaal_4` | 142, 176, -2711 |
| `puzzel_prismazaal_5` | 158, 176, -2711 |
| `puzzel_spiegelgang_1` | 145, 176, -2682 |
| `puzzel_spiegelgang_2` | 155, 176, -2682 |
| `puzzel_spiegelgang_3` | 145, 176, -2687 |
| `puzzel_spiegelgang_4` | 155, 176, -2687 |
| `puzzel_spiegelgang_5` | 145, 176, -2692 |
| `puzzel_spiegelgang_6` | 155, 176, -2692 |

## Campfires (unlit, kern checkpoints) + ridder spots

| id | x, y, z |
|---|---|
| `kampvuur_galmhellingen_1` | 110, 100, -2390 |
| `kampvuur_galmhellingen_2` | 142, 103, -2262 |
| `kampvuur_galmhellingen_3` | 208, 144, -2428 |
| `kampvuur_galmhellingen_4` | 232, 125, -2296 |
| `kampvuur_kandijkom_dok_1` | -9, 91, -2262 |
| `kampvuur_knuffelmeer_1` | -100, 100, -2330 |
| `kampvuur_knuffelmeer_2` | -236, 104, -2342 |
| `kampvuur_knuffelmeer_3` | -128, 106, -2458 |
| `kampvuur_knuffelmeer_4` | -176, 93, -2292 |
| `kampvuur_spiegelburcht_1` | 162, 160, -2590 |
| `kampvuur_splinterkloof_1` | -196, 114, -2512 |
| `kampvuur_splinterkloof_2` | 128, 123, -2508 |
| `kampvuur_splinterkloof_3` | 8, 115, -2504 |
| `kampvuur_splinterkloof_4` | -96, 65, -2546 |
| `kampvuur_splinterkloof_5` | -136, 131, -2604 |
| `ridder_galmhellingen_1` | 112, 100, -2388 |
| `ridder_galmhellingen_2` | 144, 103, -2260 |
| `ridder_galmhellingen_3` | 210, 144, -2426 |
| `ridder_galmhellingen_4` | 234, 125, -2294 |
| `ridder_kandijkom_dok_1` | -7, 91, -2260 |
| `ridder_knuffelmeer_1` | -98, 100, -2328 |
| `ridder_knuffelmeer_2` | -234, 104, -2340 |
| `ridder_knuffelmeer_3` | -126, 106, -2456 |
| `ridder_knuffelmeer_4` | -174, 93, -2290 |
| `ridder_spiegelburcht_1` | 164, 160, -2588 |
| `ridder_splinterkloof_1` | -194, 114, -2510 |
| `ridder_splinterkloof_2` | 130, 123, -2506 |
| `ridder_splinterkloof_3` | 10, 115, -2502 |
| `ridder_splinterkloof_4` | -94, 65, -2544 |
| `ridder_splinterkloof_5` | -134, 131, -2602 |

## Viewpoints (needle tops, panorama on first visit)

| id | x, y, z |
|---|---|
| `uitkijk_galmtop` | 224, 195, -2450 |
| `uitkijk_kloofkijk` | -88, 170, -2700 |
| `uitkijk_meerkijk` | -236, 143, -2280 |

## De Galmhellingen

| id | x, y, z |
|---|---|
| `galm_lawine_1` | 226, 171, -2478 |
| `galm_lawine_1_voet` | 160, 124, -2428 |
| `galm_lawine_2` | 244, 117, -2380 |
| `galm_lawine_2_voet` | 226, 129, -2250 |
| `galm_lawine_3` | 240, 130, -2262 |
| `galm_lawine_3_voet` | 166, 113, -2218 |
| `galm_naalden_1` | 120, 96, -2262 |
| `galm_naalden_2` | 232, 135, -2228 |
| `galm_naalden_3` | 128, 104, -2350 |
| `galm_naalden_4` | 236, 124, -2408 |

## Het Bevroren Knuffelmeer (knuffel_ijs = stand on the ice above a frozen plushie)

| id | x, y, z |
|---|---|
| `draak_zadel` | -205, 113, -2492 |
| `draak_zadel_kijk` | -205, 113, -2474 |
| `knuffel_ijs_1` | -150, 85, -2350 |
| `knuffel_ijs_10` | -160, 85, -2370 |
| `knuffel_ijs_2` | -165, 85, -2420 |
| `knuffel_ijs_3` | -200, 85, -2360 |
| `knuffel_ijs_4` | -210, 85, -2420 |
| `knuffel_ijs_5` | -140, 85, -2400 |
| `knuffel_ijs_6` | -195, 85, -2335 |
| `knuffel_ijs_7` | -175, 85, -2440 |
| `knuffel_ijs_8` | -130, 85, -2370 |
| `knuffel_ijs_9` | -215, 85, -2385 |
| `meer_eiland` | -186, 88, -2392 |
| `meer_midden` | -172, 85, -2378 |
| `meer_scheur` | -170, 85, -2432 |
| `meer_schotsen` | -150, 85, -2322 |

## De Splinterkloof (bridges: stand at each end; stairways: the foot on the ravine floor)

| id | x, y, z |
|---|---|
| `boortoren_1` | -150, 126, -2522 |
| `boortoren_1_top` | -150, 146, -2522 |
| `boortoren_2` | 60, 132, -2572 |
| `boortoren_2_top` | 60, 152, -2572 |
| `boortoren_3` | 204, 144, -2522 |
| `boortoren_3_top` | 204, 164, -2522 |
| `brug_1_noord` | -190, 135, -2566 |
| `brug_1_zuid` | -190, 120, -2522 |
| `brug_2_noord` | -100, 131, -2572 |
| `brug_2_zuid` | -100, 122, -2530 |
| `brug_3_noord` | 150, 137, -2562 |
| `brug_3_zuid` | 150, 127, -2526 |
| `brug_4_noord` | 40, 135, -2565 |
| `brug_4_zuid` | 40, 121, -2526 |
| `brug_5_noord` | 124, 124, -2514 |
| `brug_5_zuid` | 99, 122, -2514 |
| `kloof_bodem_1` | -216, 66, -2541 |
| `kloof_bodem_10` | 108, 65, -2548 |
| `kloof_bodem_11` | 144, 65, -2545 |
| `kloof_bodem_12` | 180, 65, -2542 |
| `kloof_bodem_13` | 216, 66, -2541 |
| `kloof_bodem_2` | -180, 65, -2545 |
| `kloof_bodem_3` | -144, 66, -2549 |
| `kloof_bodem_4` | -108, 65, -2551 |
| `kloof_bodem_5` | -72, 65, -2548 |
| `kloof_bodem_6` | -36, 65, -2545 |
| `kloof_bodem_7` | 0, 66, -2542 |
| `kloof_bodem_8` | 36, 65, -2545 |
| `kloof_bodem_9` | 72, 65, -2548 |
| `kloof_trap_1` | -234, 66, -2530 |
| `kloof_trap_2` | -38, 66, -2533 |
| `kloof_trap_3` | 110, 66, -2539 |
| `kloof_trap_4` | -204, 66, -2555 |

## Secrets

| id | x, y, z |
|---|---|
| `geheim_boorplaats` | -30, 66, -2563 |
| `geheim_burchtdak` | 160, 195, -2681 |
| `geheim_galmgrot` | 236, 121, -2346 |
| `geheim_galmgrot_ingang` | 225, 118, -2346 |
| `geheim_ijsgrot` | -240, 111, -2410 |
| `geheim_ijsgrot_ingang` | -251, 122, -2410 |

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
| `aantal_bomen` | 194, 0, 0 |
| `aanwijzing.boorgaten` | 164, 131, -2425 |
| `aanwijzing.boorschema` | -44, 104, -2427 |
| `aanwijzing.bouwtekening` | -146, 126, -2518 |
| `aanwijzing.olievlek` | 208, 144, -2518 |
| `aanwijzing.warme_spleet` | -30, 66, -2563 |
| `chocomelkkraam` | 11, 91, -2383 |
| `gehucht_galmdorpje` | 128, 104, -2414 |
| `gehucht_meerkamp` | -112, 101, -2306 |
| `gehucht_plateaukamp` | 60, 130, -2600 |
| `ijsval_1` | -160, 66, -2525 |
| `ijsval_2` | 70, 66, -2526 |
| `ijsval_3` | -60, 66, -2560 |
| `ijsval_4` | 180, 66, -2558 |
| `klokkentoren` | 0, 91, -2406 |
| `klokkentoren_top` | 0, 126, -2406 |
| `kristalbos_1` | 150, 105, -2228 |
| `kristalbos_10` | 60, 96, -2300 |
| `kristalbos_11` | -60, 94, -2300 |
| `kristalbos_12` | 20, 120, -2700 |
| `kristalbos_2` | -110, 94, -2262 |
| `kristalbos_3` | -60, 128, -2650 |
| `kristalbos_4` | 232, 123, -2380 |
| `kristalbos_5` | -236, 115, -2450 |
| `kristalbos_6` | 80, 152, -2640 |
| `kristalbos_7` | -200, 134, -2640 |
| `kristalbos_8` | 200, 138, -2420 |
| `kristalbos_9` | -150, 132, -2700 |
| `lawine_golf` | 150, 114, -2418 |
| `markt_kramen.1` | 15, 91, -2388 |
| `markt_kramen.2` | -15, 91, -2388 |
| `markt_kramen.3` | -6, 91, -2418 |
| `markt_kramen.4` | 6, 91, -2418 |
| `markt_kramen.5` | 15, 91, -2412 |
| `mika_fort` | -170, 135, -2649 |
| `mika_fort_poort` | -170, 135, -2634 |
| `mika_fort_toren` | -160, 150, -2661 |
| `npcplek.z2_bakker` | -21, 96, -2368 |
| `npcplek.z2_beeldhouwer` | -131, 106, -2457 |
| `npcplek.z2_berggids` | 107, 101, -2389 |
| `npcplek.z2_galmpje` | -8, 91, -2422 |
| `npcplek.z2_gondel_boven` | 180, 162, -2475 |
| `npcplek.z2_kasguh` | -31, 105, -2377 |
| `npcplek.z2_kwarts` | 15, 91, -2418 |
| `npcplek.z2_marktguh_1` | 15, 91, -2388 |
| `npcplek.z2_marktguh_2` | -15, 91, -2388 |
| `npcplek.z2_marktguh_3` | -6, 91, -2418 |
| `npcplek.z2_opa_sneeuwbal` | -22, 91, -2385 |
| `npcplek.z2_pinguguh_1` | -13, 91, -2405 |
| `npcplek.z2_pinguguh_2` | 9, 91, -2391 |
| `npcplek.z2_pinguguh_3` | 10, 91, -2394 |
| `npcplek.z2_poollicht` | 23, 96, -2432 |
| `npcplek.z2_postguh` | 31, 96, -2377 |
| `npcplek.z2_prisma` | 154, 176, -2611 |
| `npcplek.z2_schaatser_1` | -3, 91, -2387 |
| `npcplek.z2_schaatser_2` | 3, 91, -2383 |
| `npcplek.z2_slee_1` | 16, 91, -2304 |
| `npcplek.z2_slee_2` | 14, 91, -2302 |
| `npcplek.z2_sneeuwbal_1` | -10, 91, -2396 |
| `npcplek.z2_sneeuwbal_2` | -4, 91, -2392 |
| `npcplek.z2_sneeuwpop` | 24, 96, -2368 |
| `npcplek.z2_touwtje` | 153, 127, -2526 |
| `npcplek.z2_uitvinder` | -44, 101, -2429 |
| `npcplek.z2_versteend_ijsje` | -186, 88, -2392 |
| `npcplek.z2_versteend_pluk` | -204, 85, -2370 |
| `npcplek.z2_versteend_snoek` | -150, 85, -2322 |
| `npcplek.z2_versteend_zwier` | -172, 85, -2378 |
| `npcplek.z2_vlokje` | -9, 91, -2383 |
| `npcplek.z2_wacht_burcht` | 164, 160, -2588 |
| `npcplek.z2_wacht_dok` | -7, 91, -2260 |
| `npcplek.z2_wacht_galm_1` | 112, 100, -2388 |
| `npcplek.z2_wacht_galm_2` | 144, 103, -2260 |
| `npcplek.z2_wacht_galm_3` | 210, 144, -2426 |
| `npcplek.z2_wacht_galm_4` | 234, 125, -2294 |
| `npcplek.z2_wacht_kloof_1` | -194, 114, -2510 |
| `npcplek.z2_wacht_kloof_2` | 130, 123, -2506 |
| `npcplek.z2_wacht_kloof_3` | 10, 115, -2502 |
| `npcplek.z2_wacht_kloof_4` | -94, 65, -2544 |
| `npcplek.z2_wacht_kloof_5` | -134, 131, -2602 |
| `npcplek.z2_wacht_meer_1` | -98, 100, -2328 |
| `npcplek.z2_wacht_meer_2` | -234, 104, -2340 |
| `npcplek.z2_wacht_meer_3` | -126, 106, -2456 |
| `npcplek.z2_wacht_meer_4` | -174, 93, -2290 |
| `npcplek.z2_wacht_plein_oost` | 14, 95, -2412 |
| `npcplek.z2_wacht_plein_west` | -14, 91, -2412 |
| `npcplek.z2_wapenhandelaar` | 23, 91, -2424 |
| `npcplek.z2_winterslaap` | -21, 96, -2432 |
| `opening_naald` | 46, 145, -2344 |
| `poiplek.z2_brug_1` | 150, 127, -2526 |
| `poiplek.z2_brug_2` | -100, 122, -2530 |
| `poiplek.z2_brug_3` | -190, 120, -2522 |
| `poiplek.z2_lawinepad` | 160, 124, -2428 |
| `poiplek.z2_meer_scheur` | -170, 85, -2432 |
| `poiplek.z2_uitkijk_poollicht` | -88, 170, -2700 |
| `poiplek.z2_warmwaterbron` | 196, 120, -2252 |
| `schat.onder_de_brug` | -96, 114, -2536 |
| `schat_onder_de_brug` | -96, 114, -2536 |
| `warmwaterbron` | 196, 120, -2252 |
