# POI_Z3 - coordinates of zone 3 "De Schemerwortel" (z3-wereld slice)

**v0 (plan), GENERATED** by `tools/vadsrijk/zone3/poi3.py` (via `python tools/vadsrijk/maak_kaart.py --zone 3`). Do not edit by hand; requests go to `../POI_VERZOEKEN_Z3.md`. Java: `nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken` (`plek("id")`, `GEBIEDEN` / `gebied(x, y, z)`, `MOBZONES` / `mobzone(x, y, z)`, `PADEN`, `KAMPVUREN`, `WINDKOLOMMEN`, `LIFTEN`, `UITKIJKEN`). Dev: `/guhs vadsrijk z3wereld plek <id>` teleports there.

**Layout (v2, revamp round 20).** Zone 3 lies in the EAST: box x 1900..2700, z -400..400 (800 x 800), trunk axis at (2300, 0); tiles tx 29..42, tz -7..6 (the v1 tiles outside this grid are deleted by the map build). The approved revamp renders (renders-schemerwortel2) NOT mirrored: world x = 2300 + rel x, z = rel z. 4 layers on one axis: WORTELS y 4-78 (Wortelduister hall under the trunk, the Hart dungeon ring r 112 floor 28, the Wortelpoort on the surface east at x 2432), STAM y 80-160 (the Schorsstad inside the hollow trunk, floor 80; the Pelszaal UNDER the floor, stand 61), TAKKEN y 160-216 (Honingtak = a ring town round the trunk, deck stand 167; 5 big branches with 3 beekeeper villages), KRUIN y 216-312 (3 stepped crown storeys r 130/95/60, tops 232/260/284 at the trunk, the nest on top: floor stand 294). Connections: the inner spiral stair (floor 80 -> nest, landings 80/166/232/260/284/293), the riding spiral outside (ground 82 -> Honingtak 166 -> kruin 1 232), Honinglift A (ground -> Honingtak) and B (Honingtak -> kruin 1), 2 lifts in the hollow trunk, wind columns. The Kaaszeppelinwerf is its own island north-east (centre 2551, -251) with a cable bridge from Honingtak's Werftak. A cloud sea (water top y 24) lies under the floating island; 6 wind columns lift you back to the rim.

All positions are **stand positions** (the air block a player/NPC stands in) in `guhs:vadsrijk`, unless noted (`doos` = box corners, `*_kooi`/`piepvel_kooi_*` = the hanging cage block, `wind_*_boven` = landing at the top).

## Areas (with y-ranges; `Z3WereldPlekken.GEBIEDEN`, first match wins)

| id | shape | y | levels | town |
|---|---|---|---|---|
| `z3_zeppelinwerf` | cylinder around (2551,-251), r 0..95 | 60-290 | 38-40 | no |
| `z3_hart` | cylinder around (2300,0), r 76..150 | 14-52 | 49-50 | no |
| `z3_wortelduister` | cylinder around (2300,0), r 0..160 | 0-64 | 46-49 | no |
| `z3_schorsstad` | cylinder around (2300,0), r 0..26 | 55-165 | 40-43 | no |
| `z3_honingtak` | cylinder around (2300,0), r 0..160 | 145-200 | 30-50 | yes |
| `z3_stamspiraal` | cylinder around (2300,0), r 26..64 | 75-232 | 40-43 | no |
| `z3_kruinzee` | cylinder around (2300,0), r 0..200 | 212-320 | 30-34 | no |
| `z3_takkenbanen` | cylinder around (2300,0), r 0..260 | 140-320 | 34-38 | no |
| `z3_wortelvelden` | cylinder around (2300,0), r 0..600 | 0-320 | 43-46 | no |

## Mob zones (`Z3WereldPlekken.MOBZONES`; spawn only inside, never within ~5 blocks of `PADEN`)

| id | shape | y | levels |
|---|---|---|---|
| `z3_kruinzee_zeppelin` | cylinder around (2355,-95), r 0..34 | 220-250 | 31-34 |
| `z3_kruinzee_noord` | box (2170,-130)..(2430,-10) | 222-300 | 30-33 |
| `z3_kruinzee_zuid` | box (2170,10)..(2430,130) | 222-300 | 30-34 |
| `z3_takkenbanen_west` | box (2140,-50)..(2250,50) | 170-222 | 34-37 |
| `z3_takkenbanen_noordwest` | box (2240,-160)..(2450,-50) | 170-222 | 35-38 |
| `z3_takkenbanen_zuidwest` | box (2160,50)..(2340,160) | 170-222 | 34-37 |
| `z3_schorsstad_ringen` | cylinder around (2300,0), r 0..24 | 172-228 | 40-43 |
| `z3_wortelvelden_lichtkeverbos` | box (2220,-215)..(2380,-110) | 50-110 | 43-45 |
| `z3_wortelvelden_schimmelboerderij` | box (2085,-80)..(2190,80) | 50-110 | 44-46 |
| `z3_wortelvelden_paddenbloesemdal` | box (2220,110)..(2380,215) | 50-110 | 43-45 |
| `z3_wortelvelden_honingmoeras` | box (2410,-80)..(2515,40) | 50-110 | 44-46 |
| `z3_wortelduister_gang` | box (2340,-12)..(2450,16) | 18-72 | 46-47 |
| `z3_wortelduister_grotten` | cylinder around (2300,0), r 24..86 | 14-58 | 47-49 |

## Wind columns (`WINDKOLOMMEN`: x, z, y0 -> y1 = landing stand y, radius, landing x, z)

| id | x, z | y | r | landing |
|---|---|---|---|---|
| `werf_tak` | 2191, -43 | 213 -> 220 | 3.0 | 2198, -41 |
| `hangnest` | 2359, 95 | 203 -> 223 | 3.0 | 2355, 89 |
| `paddenbloesem_tak` | 2238, 94 | 209 -> 224 | 3.0 | 2242, 88 |
| `spiraal_kruin` | 2274, -104 | 191 -> 225 | 3.0 | 2275, -97 |
| `mosvoet_kruin` | 2274, -72 | 233 -> 254 | 3.0 | 2276, -65 |
| `lichtkever_kruin` | 2339, -24 | 261 -> 284 | 3.0 | 2333, -20 |
| `honingtak` | 2229, -33 | 75 -> 167 | 3.0 | 2242, -27 |
| `zee_1` | 2518, 58 | 25 -> 57 | 3.0 | 2493, 51 |
| `zee_2` | 2359, 223 | 25 -> 56 | 3.0 | 2353, 198 |
| `zee_3` | 2133, 166 | 25 -> 56 | 3.0 | 2151, 148 |
| `zee_4` | 2081, -59 | 25 -> 55 | 3.0 | 2107, -52 |
| `zee_5` | 2241, -220 | 25 -> 55 | 3.0 | 2247, -195 |
| `zee_6` | 2462, -163 | 25 -> 55 | 3.0 | 2443, -144 |

## Travel: airship dock (Imkerstraat tip), lifts, Honinglift A/B, wind columns, cable bridge, riding spiral

| id | x, y, z |
|---|---|
| `binnenhof` | 2300, 167, 6 |
| `dok_bel` | 2213, 167, -92 |
| `dok_kapitein` | 2212, 167, -88 |
| `dok_landing` | 2215, 167, -88 |
| `dok_luchtschip` | 2192, 165, -111 |
| `dok_mast` | 2209, 167, -94 |
| `honinglift_boven` | 2405, 167, 53 |
| `honinglift_kruin_boven` | 2212, 231, 36 |
| `honinglift_kruin_onder` | 2212, 167, 42 |
| `honinglift_onder` | 2405, 70, 59 |
| `kabelbrug_begin` | 2407, 167, -100 |
| `kabelbrug_eind` | 2499, 161, -200 |
| `lift_cafe_boven` | 2236, 167, -12 |
| `lift_cafe_onder` | 2239, 155, -11 |
| `lift_kapel_boven` | 2290, 167, -65 |
| `lift_kapel_onder` | 2291, 152, -62 |
| `lift_raattros_boven` | 2360, 167, 10 |
| `lift_raattros_onder` | 2357, 157, 10 |
| `rijpad_eind` | 2313, 232, 28 |
| `rijpad_honingplein` | 2300, 167, 37 |
| `rijpad_start` | 2319, 84, -42 |
| `schorslift_1_boven` | 2299, 167, -9 |
| `schorslift_1_onder` | 2299, 81, -9 |
| `schorslift_2_boven` | 2308, 233, 0 |
| `schorslift_2_onder` | 2308, 167, 0 |
| `wind_hangnest_boven` | 2355, 223, 89 |
| `wind_hangnest_onder` | 2359, 203, 95 |
| `wind_honingtak_boven` | 2242, 167, -27 |
| `wind_honingtak_onder` | 2229, 75, -33 |
| `wind_lichtkever_kruin_boven` | 2333, 284, -20 |
| `wind_lichtkever_kruin_onder` | 2339, 261, -24 |
| `wind_mosvoet_kruin_boven` | 2276, 254, -65 |
| `wind_mosvoet_kruin_onder` | 2274, 233, -72 |
| `wind_paddenbloesem_tak_boven` | 2242, 224, 88 |
| `wind_paddenbloesem_tak_onder` | 2238, 209, 94 |
| `wind_spiraal_kruin_boven` | 2275, 225, -97 |
| `wind_spiraal_kruin_onder` | 2274, 191, -104 |
| `wind_werf_tak_boven` | 2198, 220, -41 |
| `wind_werf_tak_onder` | 2191, 213, -43 |
| `wind_zee_1_boven` | 2493, 57, 51 |
| `wind_zee_1_onder` | 2518, 25, 58 |
| `wind_zee_2_boven` | 2353, 56, 198 |
| `wind_zee_2_onder` | 2359, 25, 223 |
| `wind_zee_3_boven` | 2151, 56, 148 |
| `wind_zee_3_onder` | 2133, 25, 166 |
| `wind_zee_4_boven` | 2107, 55, -52 |
| `wind_zee_4_onder` | 2081, 25, -59 |
| `wind_zee_5_boven` | 2247, 55, -195 |
| `wind_zee_5_onder` | 2241, 25, -220 |
| `wind_zee_6_boven` | 2443, 55, -144 |
| `wind_zee_6_onder` | 2462, 25, -163 |

## The inner spiral stair: one stand spot per landing (1 = Schorsstad floor 80, 2 = Honingtak 166, 3/4/5 = kruin 1/2/3, 6 = nest floor)

| id | x, y, z |
|---|---|
| `trap_bordes_1` | 2285, 81, 14 |
| `trap_bordes_2` | 2300, 167, 20 |
| `trap_bordes_3` | 2319, 233, 1 |
| `trap_bordes_4` | 2290, 261, -18 |
| `trap_bordes_5` | 2316, 285, 11 |
| `trap_bordes_6` | 2293, 294, -20 |

## Honingtak: plaza, market, NPC spots

| id | x, y, z |
|---|---|
| `bloesemboompje` | 2270, 167, 40 |
| `knuffelbeeld` | 2300, 167, 52 |
| `markt` | 2300, 167, 60 |
| `npc_kraam_1n` | 2319, 167, 43 |
| `npc_kraam_1z` | 2325, 167, 56 |
| `npc_kraam_2n` | 2309, 167, 46 |
| `npc_kraam_2z` | 2312, 167, 60 |
| `npc_kraam_3n` | 2300, 167, 47 |
| `npc_kraam_3z` | 2300, 167, 61 |
| `npc_kraam_4n` | 2290, 167, 46 |
| `npc_kraam_4z` | 2287, 167, 60 |
| `npc_kraam_5n` | 2280, 167, 43 |
| `npc_kraam_5z` | 2274, 167, 56 |
| `npc_raatje_plein` | 2305, 167, 57 |
| `plein` | 2292, 167, 51 |
| `questbord` | 2294, 167, 58 |
| `respawn` | 2308, 167, 51 |

## Honingtak buildings (npc = behind the counter/at the back, binnen = interior centre, deur = stand outside the door, doos = box)

| id | x, y, z |
|---|---|
| `gebouw.algemeen.binnen` | 2279, 167, -55 |
| `gebouw.algemeen.deur` | 2282, 167, -49 |
| `gebouw.algemeen.doos_max` | 2286, 178, -48 |
| `gebouw.algemeen.doos_min` | 2272, 166, -62 |
| `gebouw.algemeen.npc` | 2279, 167, -56 |
| `gebouw.bank.binnen` | 2309, 167, -58 |
| `gebouw.bank.deur` | 2308, 167, -51 |
| `gebouw.bank.doos_max` | 2316, 178, -51 |
| `gebouw.bank.doos_min` | 2302, 166, -65 |
| `gebouw.bank.npc` | 2310, 167, -59 |
| `gebouw.burgemeester.binnen` | 2354, 167, -21 |
| `gebouw.burgemeester.deur` | 2348, 167, -18 |
| `gebouw.burgemeester.doos_max` | 2361, 178, -14 |
| `gebouw.burgemeester.doos_min` | 2347, 166, -28 |
| `gebouw.burgemeester.npc` | 2355, 167, -21 |
| `gebouw.cafe.binnen` | 2245, 155, -10 |
| `gebouw.cafe.deur` | 2238, 155, -11 |
| `gebouw.cafe.npc` | 2248, 155, -10 |
| `gebouw.cafe.podium` | 2250, 155, -6 |
| `gebouw.drankjes.binnen` | 2242, 167, -10 |
| `gebouw.drankjes.deur` | 2249, 167, -9 |
| `gebouw.drankjes.doos_max` | 2249, 178, -3 |
| `gebouw.drankjes.doos_min` | 2235, 166, -17 |
| `gebouw.drankjes.npc` | 2241, 167, -11 |
| `gebouw.jagershut.binnen` | 2324, 167, -53 |
| `gebouw.jagershut.deur` | 2321, 167, -47 |
| `gebouw.jagershut.doos_max` | 2331, 178, -46 |
| `gebouw.jagershut.doos_min` | 2317, 166, -60 |
| `gebouw.jagershut.jachtbord` | 2327, 167, -56 |
| `gebouw.jagershut.npc` | 2324, 167, -55 |
| `gebouw.jagershut.premiebord` | 2324, 167, -57 |
| `gebouw.juwelier.binnen` | 2242, 167, 5 |
| `gebouw.juwelier.deur` | 2247, 167, 4 |
| `gebouw.juwelier.doos_max` | 2249, 178, 12 |
| `gebouw.juwelier.doos_min` | 2235, 166, -2 |
| `gebouw.juwelier.npc` | 2240, 167, 5 |
| `gebouw.nestkapel.advancement` | 2297, 152, -54 |
| `gebouw.nestkapel.binnen` | 2292, 152, -55 |
| `gebouw.nestkapel.deur` | 2291, 152, -63 |
| `gebouw.nestkapel.droomsteen` | 2287, 152, -52 |
| `gebouw.nestkapel.eiersokkel` | 2293, 152, -49 |
| `gebouw.nestkapel.npc` | 2292, 152, -52 |
| `gebouw.pluimstal.binnen` | 2272, 75, -101 |
| `gebouw.pluimstal.deur` | 2275, 75, -92 |
| `gebouw.pluimstal.npc` | 2274, 75, -96 |
| `gebouw.pluimstal.voer` | 2279, 75, -103 |
| `gebouw.raattros.binnen` | 2354, 157, 9 |
| `gebouw.raattros.deur` | 2358, 157, 10 |
| `gebouw.raattros.npc` | 2351, 157, 9 |
| `gebouw.smid.binnen` | 2262, 167, 44 |
| `gebouw.smid.deur` | 2266, 167, 40 |
| `gebouw.smid.doos_max` | 2269, 178, 51 |
| `gebouw.smid.doos_min` | 2255, 166, 37 |
| `gebouw.smid.npc` | 2261, 167, 45 |
| `gebouw.uitrusting.binnen` | 2247, 167, -25 |
| `gebouw.uitrusting.deur` | 2253, 167, -22 |
| `gebouw.uitrusting.doos_max` | 2254, 178, -18 |
| `gebouw.uitrusting.doos_min` | 2240, 166, -32 |
| `gebouw.uitrusting.npc` | 2245, 167, -25 |
| `gebouw.woning_1.binnen` | 2357, 167, -6 |
| `gebouw.woning_1.deur` | 2352, 167, -5 |
| `gebouw.woning_1.doos_max` | 2364, 178, 1 |
| `gebouw.woning_1.doos_min` | 2350, 166, -13 |
| `gebouw.woning_1.npc` | 2359, 167, -6 |
| `gebouw.woning_2.binnen` | 2357, 167, 9 |
| `gebouw.woning_2.deur` | 2350, 167, 8 |
| `gebouw.woning_2.doos_max` | 2364, 178, 16 |
| `gebouw.woning_2.doos_min` | 2350, 166, 2 |
| `gebouw.woning_2.npc` | 2358, 167, 10 |
| `gebouw.woning_3.binnen` | 2333, 167, 47 |
| `gebouw.woning_3.deur` | 2329, 167, 41 |
| `gebouw.woning_3.doos_max` | 2340, 178, 54 |
| `gebouw.woning_3.doos_min` | 2326, 166, 40 |
| `gebouw.woning_3.npc` | 2334, 167, 48 |
| `gebouw.woning_4.binnen` | 2382, 167, 31 |
| `gebouw.woning_4.deur` | 2379, 167, 37 |
| `gebouw.woning_4.doos_max` | 2389, 178, 38 |
| `gebouw.woning_4.doos_min` | 2375, 166, 24 |
| `gebouw.woning_4.npc` | 2383, 167, 30 |
| `gebouw.woning_5.binnen` | 2387, 167, 67 |
| `gebouw.woning_5.deur` | 2390, 167, 62 |
| `gebouw.woning_5.doos_max` | 2394, 178, 74 |
| `gebouw.woning_5.doos_min` | 2380, 166, 60 |
| `gebouw.woning_5.npc` | 2386, 167, 68 |
| `gebouw.woning_6.binnen` | 2230, 167, 54 |
| `gebouw.woning_6.deur` | 2226, 167, 49 |
| `gebouw.woning_6.doos_max` | 2237, 178, 61 |
| `gebouw.woning_6.doos_min` | 2223, 166, 47 |
| `gebouw.woning_6.npc` | 2231, 167, 55 |
| `gebouw.woning_7.binnen` | 2196, 167, 39 |
| `gebouw.woning_7.deur` | 2200, 167, 45 |
| `gebouw.woning_7.doos_max` | 2203, 178, 46 |
| `gebouw.woning_7.doos_min` | 2189, 166, 32 |
| `gebouw.woning_7.npc` | 2195, 167, 38 |
| `gebouw.woning_8.binnen` | 2248, 167, -73 |
| `gebouw.woning_8.deur` | 2243, 167, -68 |
| `gebouw.woning_8.doos_max` | 2255, 178, -66 |
| `gebouw.woning_8.doos_min` | 2241, 166, -80 |
| `gebouw.woning_8.npc` | 2249, 167, -74 |
| `gebouw.woning_9.binnen` | 2214, 167, -69 |
| `gebouw.woning_9.deur` | 2218, 167, -74 |
| `gebouw.woning_9.doos_max` | 2221, 178, -62 |
| `gebouw.woning_9.doos_min` | 2207, 166, -76 |
| `gebouw.woning_9.npc` | 2213, 167, -68 |

## Pluimstal (starter choice, z3_hq03)

| id | x, y, z |
|---|---|
| `pluimstal_keuze` | 2268, 74, -116 |
| `pluimstal_keuze_pluimguh_1` | 2261, 73, -121 |
| `pluimstal_keuze_pluimguh_2` | 2266, 73, -122 |
| `pluimstal_keuze_pluimguh_3` | 2272, 73, -124 |
| `pluimstal_kruinring_start` | 2319, 84, -42 |
| `pluimstal_oefenbaan` | 2282, 69, -141 |
| `pluimstal_zweefplatform` | 2253, 109, -18 |

## Arenas (world boss, Pelszaal, Vahoegnir, dungeon bosses, Mika-imker)

| id | x, y, z |
|---|---|
| `arena_baron_bleu` | 2411, 29, 11 |
| `arena_brokkel` | 2551, 192, -291 |
| `arena_brokkel_kanon_1` | 2543, 192, -288 |
| `arena_brokkel_kanon_2` | 2559, 192, -288 |
| `arena_kruinroofvogel` | 2155, 226, 46 |
| `arena_kruinroofvogel_bord` | 2185, 227, 37 |
| `arena_kruinroofvogel_deur` | 2184, 227, 27 |
| `arena_nestwachter` | 2377, 29, 80 |
| `arena_piepvel` | 2300, 61, 0 |
| `arena_piepvel_deur` | 2301, 61, -13 |
| `arena_roquefort` | 2588, 224, -245 |
| `arena_taleggio` | 2511, 168, -245 |
| `arena_vahoegnir` | 2300, 294, 0 |
| `arena_wortelwurger` | 2307, 29, 111 |
| `kruinroofvogel_zit_1` | 2367, 210, 124 |
| `kruinroofvogel_zit_2` | 2426, 202, -11 |
| `mika_imker_luik` | 2353, 157, 11 |
| `mika_imker_raatzaal` | 2354, 145, 9 |
| `nest_eierkuil` | 2300, 294, 0 |
| `nest_rand` | 2296, 294, -13 |
| `nest_tak_1` | 2311, 294, 5 |
| `nest_tak_2` | 2289, 294, 5 |
| `nest_tak_3` | 2300, 294, 13 |
| `nest_tak_4` | 2311, 294, -7 |
| `nest_tak_5` | 2289, 294, -7 |
| `nest_tak_6` | 2300, 294, -14 |
| `piepvel_grote_kooi` | 2306, 61, -11 |
| `piepvel_kooi_1` | 2307, 63, 7 |
| `piepvel_kooi_2` | 2292, 65, 7 |
| `piepvel_kooi_3` | 2292, 67, -8 |
| `piepvel_kooi_4` | 2307, 69, -8 |
| `piepvel_lantaarnpaal_1` | 2313, 61, 0 |
| `piepvel_lantaarnpaal_2` | 2306, 61, 11 |
| `piepvel_lantaarnpaal_3` | 2293, 61, 11 |
| `piepvel_lantaarnpaal_4` | 2286, 61, 0 |
| `piepvel_lantaarnpaal_5` | 2293, 61, -12 |
| `piepvel_lantaarnpaal_6` | 2306, 61, -12 |
| `piepvel_troon` | 2300, 61, -12 |

## Kaaszeppelinwerf (dungeon 1)

| id | x, y, z |
|---|---|
| `roquefort_kajuit` | 2591, 224, -251 |
| `roquefort_wrak` | 2582, 221, -229 |
| `werf_bord` | 2502, 161, -209 |
| `werf_ingang` | 2505, 161, -206 |
| `werf_kamer_bewapening` | 2567, 163, -254 |
| `werf_kamer_gieterij` | 2543, 162, -272 |
| `werf_kamer_touwwerk` | 2532, 162, -245 |
| `werf_loods_poort` | 2567, 163, -245 |
| `werf_trap_ab` | 2551, 162, -281 |
| `werf_trap_bc` | 2578, 159, -247 |
| `werf_vahoeg_altaar` | 2508, 161, -203 |

## Hart van de Boom (dungeon 2) + the egg

| id | x, y, z |
|---|---|
| `ei` | 2300, 26, 0 |
| `hart_bord` | 2226, 29, 5 |
| `hart_grotlantaarn_1` | 2227, 29, 3 |
| `hart_grotlantaarn_2` | 2220, 32, 3 |
| `hart_grotlantaarn_3` | 2220, 32, -4 |
| `hart_grotlantaarn_4` | 2227, 29, -4 |
| `hart_kamer_aderloop` | 2192, 29, 0 |
| `hart_kamer_ringenzaal` | 2244, 29, 96 |
| `hart_kamer_schimmelkwekerij` | 2201, 29, 52 |
| `hart_kwekerij_poort` | 2201, 29, 34 |
| `hart_nestkamer` | 2309, 21, 0 |
| `hart_poort` | 2223, 31, 0 |
| `hart_ringenzaal_poort` | 2244, 29, 78 |
| `hart_vahoeg_altaar` | 2226, 29, -5 |

## Hartsplinters + Keizer flashes

| id | x, y, z |
|---|---|
| `flash_f1_kwastgat` | 2319, 128, 8 |
| `flash_f2_kijkpunt` | 2292, 152, -54 |
| `grauwe_brief_1` | 2590, 224, -253 |
| `hartsplinter_2` | 2293, 294, 6 |
| `hartsplinter_3` | 2593, 224, -251 |
| `hartsplinter_4` | 2303, 61, 4 |
| `hartsplinter_5` | 2383, 29, 74 |
| `vingerafdruk` | 2319, 130, 12 |

## Campfires (unlit, kern checkpoints)

| id | x, y, z |
|---|---|
| `kampvuur_hangnest` | 2342, 197, 59 |
| `kampvuur_hartslag` | 2360, 25, -14 |
| `kampvuur_kruin_1` | 2399, 230, 42 |
| `kampvuur_kruin_2` | 2256, 261, -50 |
| `kampvuur_kruin_3` | 2281, 284, 40 |
| `kampvuur_kruin_nest` | 2323, 285, 29 |
| `kampvuur_mosvoet` | 2422, 67, 97 |
| `kampvuur_schorsstad_ring4` | 2298, 132, 26 |
| `kampvuur_schorsstad_vloer` | 2290, 81, 7 |
| `kampvuur_tak_noordwest_60` | 2287, 185, -77 |
| `kampvuur_tak_west_30` | 2247, 209, -19 |
| `kampvuur_tak_west_62` | 2207, 210, -31 |
| `kampvuur_tak_zuidwest_45` | 2250, 204, 59 |
| `kampvuur_wortelgang_1` | 2403, 58, 1 |
| `kampvuur_wortelgang_2` | 2368, 38, 6 |
| `kampvuur_wortelgewelf` | 2261, 66, -163 |
| `kampvuur_wortelweg_1` | 2453, 64, 88 |
| `kampvuur_wortelweg_2` | 2300, 64, 177 |
| `kampvuur_wortelweg_3` | 2146, 66, 88 |
| `kampvuur_wortelweg_4` | 2146, 65, -89 |
| `kampvuur_wortelweg_5` | 2299, 65, -177 |
| `kampvuur_wortelweg_6` | 2453, 66, -89 |

## Viewpoints

| id | x, y, z |
|---|---|
| `uitkijk_kroon` | 2365, 270, 46 |
| `uitkijk_takpunt` | 2163, 217, -55 |
| `uitkijk_wortelgewelf` | 2246, 81, -167 |

## Kruinzee

| id | x, y, z |
|---|---|
| `bloesemwaterval_top` | 2261, 222, 111 |
| `grauwe_vlek_1` | 2390, 228, 51 |
| `grauwe_vlek_2` | 2246, 259, 44 |
| `grauwe_vlek_3` | 2261, 226, -106 |
| `honingvijver_1` | 2344, 225, 98 |
| `honingvijver_2` | 2252, 228, 92 |
| `honingvijver_3` | 2201, 226, -49 |
| `honingvijver_4` | 2324, 230, -104 |
| `honingvijver_5` | 2399, 227, -48 |
| `mika_bevel` | 2349, 228, -92 |
| `mika_steiger` | 2355, 227, -96 |
| `zweefplatform_1` | 2330, 224, 112 |
| `zweefplatform_2` | 2233, 226, -96 |

## Takkenbanen + the beekeeper villages (hangnest_* = Imkerdorp Zoemrust on the Bijentak; hangnest_woning_1..3 = Raatweide, 4..6 = Molenhoek)

| id | x, y, z |
|---|---|
| `hangnest_herberg` | 2340, 200, 74 |
| `hangnest_ladder_top` | 2329, 197, 35 |
| `hangnest_lusje` | 2349, 198, 41 |
| `hangnest_plein` | 2351, 199, 70 |
| `hangnest_stalpost` | 2358, 201, 83 |
| `hangnest_winkeltje` | 2371, 204, 95 |
| `hangnest_woning_1` | 2270, 205, 55 |
| `hangnest_woning_2` | 2237, 207, 67 |
| `hangnest_woning_3` | 2248, 210, 102 |
| `hangnest_woning_4` | 2278, 185, -57 |
| `hangnest_woning_5` | 2291, 188, -91 |
| `hangnest_woning_6` | 2260, 193, -109 |
| `kaasschutterpost_1` | 2199, 212, -40 |
| `kaasschutterpost_2` | 2395, 195, -11 |
| `kaasschutterpost_3` | 2232, 212, 107 |
| `lianenbrug` | 2350, 192, -5 |
| `molenhoek_plein` | 2280, 187, -86 |
| `pluimpost_westtak` | 2234, 209, -24 |
| `raatweide_plein` | 2248, 206, 72 |
| `valbrug` | 2251, 210, -13 |

## Riding spiral (spiraal_*) + Schorsstad (Lantaarntoren rings: stand on the tower ring platform; schorshuis.N = the houses in the trunk wall)

| id | x, y, z |
|---|---|
| `boomvolk_lichtje_1` | 2287, 173, -15 |
| `boomvolk_lichtje_2` | 2288, 186, 15 |
| `boomvolk_lichtje_3` | 2316, 199, 8 |
| `boomvolk_lichtje_4` | 2305, 212, -19 |
| `lantaarnring_1` | 2303, 93, 0 |
| `lantaarnring_1_kooi` | 2296, 93, 0 |
| `lantaarnring_2` | 2303, 103, 0 |
| `lantaarnring_2_kooi` | 2296, 103, 0 |
| `lantaarnring_3` | 2303, 113, 0 |
| `lantaarnring_3_kooi` | 2296, 113, 0 |
| `lantaarnring_4` | 2303, 123, 0 |
| `lantaarnring_4_kooi` | 2296, 123, 0 |
| `lantaarnring_5` | 2303, 133, 0 |
| `lantaarnring_5_kooi` | 2296, 133, 0 |
| `lantaarnring_6` | 2303, 143, 0 |
| `lantaarnring_6_kooi` | 2296, 143, 0 |
| `lantaarnring_7` | 2303, 153, 0 |
| `lantaarnring_7_kooi` | 2296, 153, 0 |
| `muurverhaal_1` | 2278, 101, -5 |
| `muurverhaal_2` | 2321, 110, 4 |
| `muurverhaal_3` | 2278, 119, -5 |
| `muurverhaal_4` | 2321, 128, 4 |
| `muurverhaal_5` | 2278, 137, -5 |
| `muurverhaal_6` | 2321, 146, 4 |
| `muurverhaal_7` | 2313, 195, -17 |
| `muurverhaal_8` | 2286, 204, 16 |
| `nestlantaarn_knoest` | 2300, 123, 3 |
| `npc_oude_knoest` | 2301, 123, -27 |
| `pelszaal_trap_boven` | 2288, 81, -18 |
| `schorshuis.1` | 2301, 87, -27 |
| `schorshuis.10` | 2326, 128, 1 |
| `schorshuis.11` | 2298, 132, 26 |
| `schorshuis.12` | 2273, 137, -2 |
| `schorshuis.13` | 2301, 141, -27 |
| `schorshuis.14` | 2326, 146, 1 |
| `schorshuis.15` | 2298, 150, 26 |
| `schorshuis.16` | 2273, 155, -2 |
| `schorshuis.2` | 2326, 92, 1 |
| `schorshuis.3` | 2298, 96, 26 |
| `schorshuis.4` | 2273, 101, -2 |
| `schorshuis.5` | 2301, 105, -27 |
| `schorshuis.6` | 2326, 110, 1 |
| `schorshuis.7` | 2298, 114, 26 |
| `schorshuis.8` | 2273, 119, -2 |
| `schorshuis.9` | 2301, 123, -27 |
| `schorsstad_poort_128` | 2299, 81, -19 |
| `schorsstad_poort_176` | 2300, 167, 21 |
| `schorsstad_poort_75` | 2289, 81, 15 |
| `schorsstad_ring_1` | 2306, 95, 19 |
| `schorsstad_ring_2` | 2300, 105, -21 |
| `schorsstad_ring_3` | 2292, 115, 18 |
| `schorsstad_ring_4` | 2313, 125, -16 |
| `schorsstad_ring_5` | 2281, 135, 9 |
| `schorsstad_ring_6` | 2320, 145, -3 |
| `schorsstad_ring_7` | 2279, 155, -5 |
| `schorsstad_vloer` | 2309, 81, -8 |
| `spiraal_balkon_1` | 2256, 109, -16 |
| `spiraal_balkon_2` | 2337, 161, 21 |
| `spiraal_balkon_3` | 2286, 216, -37 |
| `spiraal_poort_128` | 2299, 80, -40 |
| `spiraal_poort_176` | 2300, 167, 45 |
| `spiraal_poort_75` | 2276, 80, 34 |
| `spiraal_top` | 2313, 232, 28 |
| `spiraal_voet` | 2319, 84, -42 |
| `spiraal_zweefsprong_1` | 2253, 109, -18 |
| `spiraal_zweefsprong_2` | 2341, 161, 23 |
| `spiraal_zweefsprong_3` | 2285, 216, -41 |

## Wortelvelden (Mosvoet hamlet, Pluimposten, camps, valleys)

| id | x, y, z |
|---|---|
| `honingmoeras` | 2455, 68, -57 |
| `kamp.1` | 2272, 68, 172 |
| `kamp.2` | 2137, 62, -79 |
| `kamp.3` | 2425, 66, -108 |
| `lichtkeverbos` | 2299, 69, -150 |
| `mosvoet_burgemeester` | 2439, 66, 96 |
| `mosvoet_plein` | 2427, 66, 92 |
| `mosvoet_stalpost` | 2416, 68, 89 |
| `mosvoet_theehuis` | 2430, 67, 104 |
| `mosvoet_waterrad` | 2425, 67, 81 |
| `mosvoet_winkel` | 2419, 67, 101 |
| `mosvoet_woning` | 2436, 67, 84 |
| `nachtgeheim_zadel` | 2235, 59, -179 |
| `paddenbloesemdal` | 2286, 68, 159 |
| `pluimpost_1` | 2388, 66, -153 |
| `pluimpost_2` | 2147, 65, -89 |
| `pluimpost_3` | 2199, 66, 144 |
| `pluimpost_4` | 2388, 68, 152 |
| `schimmelboerderij` | 2149, 68, -55 |
| `schimmelketel_1` | 2168, 68, -64 |
| `schimmelketel_2` | 2141, 69, -44 |
| `schimmelketel_3` | 2163, 67, -99 |
| `schimmelketel_4` | 2149, 68, -14 |
| `tijdrit_start` | 2318, 84, -43 |
| `wortelgewelf` | 2258, 65, -167 |
| `wortelpoort` | 2446, 70, 0 |

## Wortelduister

| id | x, y, z |
|---|---|
| `grotlantaarn_1` | 2436, 67, -3 |
| `grotlantaarn_2` | 2424, 65, -6 |
| `grotlantaarn_3` | 2412, 62, -8 |
| `grotlantaarn_4` | 2401, 57, -4 |
| `grotlantaarn_5` | 2391, 51, 1 |
| `grotlantaarn_6` | 2381, 45, 2 |
| `grotlantaarn_7` | 2371, 39, 0 |
| `grotlantaarn_8` | 2362, 34, -2 |
| `grotlantaarn_9` | 2352, 28, -3 |
| `hartslag_zaal` | 2352, 23, 0 |

## Secrets (knuffels, schatten, Mika-imker clues)

| id | x, y, z |
|---|---|
| `geheim_knuffel_1` | 2254, 230, 79 |
| `geheim_knuffel_10` | 2260, 23, 30 |
| `geheim_knuffel_2` | 2253, 260, -18 |
| `geheim_knuffel_3` | 2272, 194, -117 |
| `geheim_knuffel_4` | 2146, 60, 128 |
| `geheim_knuffel_5` | 2402, 226, -60 |
| `geheim_knuffel_6` | 2501, 59, 35 |
| `geheim_knuffel_7` | 2282, 59, -200 |
| `geheim_knuffel_8` | 2330, 23, -40 |
| `geheim_knuffel_9` | 2271, 101, -2 |
| `geheim_schat_1` | 2220, 217, 125 |
| `geheim_schat_2` | 2304, 294, -16 |
| `geheim_schat_3` | 2298, 150, 28 |
| `geheim_schat_4` | 2240, 25, 10 |
| `geheim_schat_5` | 2494, 62, -17 |
| `imker_aanwijzing_1` | 2310, 167, 48 |
| `imker_aanwijzing_2` | 2207, 167, 49 |
| `imker_aanwijzing_3` | 2360, 167, 33 |
| `imker_aanwijzing_4` | 2260, 167, 31 |
| `imker_aanwijzing_5` | 2356, 157, 11 |

## Landmarks

| id | x, y, z |
|---|---|
| `landmark_hoogvads_richting` | 3000, 260, 0 |

## Other

| id | x, y, z |
|---|---|
| `kuiken_1` | 2350, 229, 80 |
| `kuiken_2` | 2269, 229, 86 |
| `kuiken_3` | 2208, 227, -31 |
| `kuiken_4` | 2309, 229, -94 |
| `kuiken_5` | 2382, 230, -53 |
| `kuiken_6` | 2238, 228, -89 |
