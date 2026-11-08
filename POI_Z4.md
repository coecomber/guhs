# POI_Z4 - coordinates of zone 4 "De Lampionkust" (t-z4wereld slice)

**v2 (map, stand spots snapped onto the tiles), GENERATED** by `tools/vadsrijk/zone4/poi4.py` (via `python tools/vadsrijk/maak_kaart.py --zone 4`). Do not edit by hand. Java: `nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken` (`plek("id")`, `GEBIEDEN` / `gebied(x, z)`, `MOBZONES` / `mobzone(x, z)`, `ARENAS`, `DOKKEN`, `KAMPVUREN`, `UITKIJKEN`, `SCHILDPAD_ROUTE`, `stroming(x, z)`, `mist(x, z)`). Dev: `/guhs vadsrijk z4wereld plek <id>` teleports there.

**Layout.** Zone 4 = a string of islands in a pink sea, x 1740..2540, z -716..-266 (tiles tx 27..39, tz -12..-5 = x 1728..2559, z -768..-257). Sea: water up to y 62 (surface 63), sea floor ~35-60, solid ground from y 0. West = Lampionhaven (hub, 1850, -470), east = Pierkop with the Opstijgpier (pier end 2515, -565, deck 72) under the head of Moeder Wolkwal (zone 5, y 170+, NOT in these tiles). The Kringstroom (current ring, ellipse (2150, -470) 470 x 255) pushes swimmers/boats back inside; the Grauwe Mistbank (south-east, around the Mysterie-eiland) is client fog.

All positions are **stand positions** (the air block a player/NPC stands in) in `guhs:vadsrijk`, unless noted.

## Areas (`Z4WereldPlekken.GEBIEDEN`, ellipses, first match wins)

| id | centre | half axes | levels | town |
|---|---|---|---|---|
| `z4_lampionhaven` | 1846, -470 | 78 x 72 | 50-50 | yes |
| `z4_knuffeltij` | 1935, -345 | 88 x 62 | 50-51 | no |
| `z4_strandfeest` | 1995, -605 | 78 x 62 | 51-52 | no |
| `z4_bloemeneiland` | 2085, -405 | 74 x 66 | 51-52 | no |
| `z4_koraalbaai` | 2185, -565 | 88 x 86 | 53-54 | no |
| `z4_vliegereiland` | 2300, -665 | 70 x 56 | 52-53 | no |
| `z4_vuurtoreneiland` | 2400, -500 | 56 x 52 | 54-55 | no |
| `z4_pierkop` | 2470, -565 | 50 x 46 | 55-56 | no |
| `z4_mysterie` | 2480, -330 | 60 x 54 | 55-56 | no |
| `z4_wrak` | 2165, -700 | 30 x 22 | 54-55 | no |
| `z4_schildpad` | 2290, -420 | 92 x 74 | 52-53 | no |
| `z4_zee` | 2150, -470 | 510 x 315 | 50-56 | no |

## Mob zones (`Z4WereldPlekken.MOBZONES`)

| id | centre | half axes | levels |
|---|---|---|---|
| `z4_bloemeneiland` | 2085, -405 | 42 x 34 | 51-52 |
| `z4_koraalbaai` | 2185, -565 | 56 x 54 | 53-54 |
| `z4_vliegereiland` | 2300, -665 | 38 x 24 | 52-53 |
| `z4_mysterie` | 2480, -330 | 28 x 22 | 55-56 |
| `z4_wrak` | 2165, -700 | 18 x 8 | 54-55 |

## Travel: airship ports (line C zone 3 -> 4 = `haven_3_4_*` at Lampionhaven's luchthaven; line D zone 4 -> 5 = `haven_4_5_*` on the Opstijgpier) + the Opstijgpier

| id | x, y, z |
|---|---|
| `haven_3_4_bel` | 1799, 71, -472 |
| `haven_3_4_kapitein` | 1798, 71, -467 |
| `haven_3_4_landing` | 1796, 71, -468 |
| `haven_3_4_luchtschip` | 1770, 70, -470 |
| `haven_3_4_trap_onder` | 1802, 67, -477 |
| `haven_3_4_vertrekbord` | 1799, 71, -469 |
| `haven_4_5_bel` | 2510, 73, -570 |
| `haven_4_5_kapitein` | 2512, 73, -564 |
| `haven_4_5_landing` | 2517, 73, -562 |
| `haven_4_5_luchtschip` | 2539, 72, -565 |
| `haven_4_5_mast` | 2515, 103, -565 |
| `haven_4_5_vertrekbord` | 2510, 73, -570 |
| `opstijgpier` | 2511, 73, -565 |
| `opstijgpier_begin` | 2472, 71, -565 |
| `opstijgpier_cliffhanger` | 2521, 73, -565 |

## Sailboat docks (`boot_dok_<island>` = stand at the jetty end, `_water` = the water block the boat floats in, `_wal` = the beach end; `Z4WereldPlekken.DOKKEN`)

| id | x, y, z |
|---|---|
| `boot_dok_bloemeneiland` | 2028, 65, -405 |
| `boot_dok_bloemeneiland_wal` | 2038, 65, -405 |
| `boot_dok_bloemeneiland_water` | 2026, 63, -403 |
| `boot_dok_knuffeltij` | 2002, 65, -345 |
| `boot_dok_knuffeltij_wal` | 1992, 65, -345 |
| `boot_dok_knuffeltij_water` | 2004, 63, -343 |
| `boot_dok_koraalbaai` | 2114, 65, -565 |
| `boot_dok_koraalbaai_wal` | 2124, 64, -565 |
| `boot_dok_koraalbaai_water` | 2112, 63, -563 |
| `boot_dok_lampionhaven` | 1891, 65, -464 |
| `boot_dok_lampionhaven_water` | 1894, 63, -464 |
| `boot_dok_mysterie` | 2441, 65, -330 |
| `boot_dok_mysterie_wal` | 2451, 65, -330 |
| `boot_dok_mysterie_water` | 2439, 63, -328 |
| `boot_dok_pierkop` | 2470, 65, -530 |
| `boot_dok_pierkop_wal` | 2470, 64, -540 |
| `boot_dok_pierkop_water` | 2472, 63, -528 |
| `boot_dok_schildpad` | 2258, 64, -431 |
| `boot_dok_schildpad_water` | 2260, 63, -429 |
| `boot_dok_strandfeest` | 1995, 65, -558 |
| `boot_dok_strandfeest_wal` | 1995, 65, -568 |
| `boot_dok_strandfeest_water` | 1997, 63, -556 |
| `boot_dok_vliegereiland` | 2300, 65, -621 |
| `boot_dok_vliegereiland_wal` | 2300, 64, -631 |
| `boot_dok_vliegereiland_water` | 2302, 63, -619 |
| `boot_dok_vuurtoreneiland` | 2365, 65, -500 |
| `boot_dok_vuurtoreneiland_wal` | 2375, 65, -500 |
| `boot_dok_vuurtoreneiland_water` | 2363, 63, -498 |
| `boot_dok_wrak` | 2192, 64, -700 |
| `boot_dok_wrak_water` | 2192, 63, -697 |

## Lampionhaven: plaza, quay, jetties, workshops (respawn = Havenplein)

| id | x, y, z |
|---|---|
| `bank_kluis` | 1825, 72, -480 |
| `kade` | 1866, 66, -470 |
| `lampionduin_top` | 1860, 90, -516 |
| `lampionmast` | 1849, 67, -457 |
| `plein` | 1848, 67, -467 |
| `plein_fontein` | 1848, 67, -466 |
| `questbord` | 1842, 67, -466 |
| `respawn` | 1848, 67, -467 |
| `stal_wei` | 1807, 66, -452 |
| `steiger_1` | 1880, 65, -478 |
| `steiger_1_eind` | 1891, 65, -478 |
| `steiger_2` | 1880, 65, -464 |
| `steiger_2_eind` | 1891, 65, -464 |
| `steiger_3` | 1880, 65, -451 |
| `steiger_3_eind` | 1891, 65, -451 |
| `visrestaurant_terras` | 1909, 66, -489 |
| `vuurtorentje` | 1912, 64, -436 |
| `werkplaats_kookvuur` | 1853, 68, -440 |
| `werkplaats_koraalbank` | 2186, 65, -595 |
| `werkplaats_lampion` | 1837, 67, -447 |
| `werkplaats_visplek` | 1880, 65, -451 |
| `werkplaats_vlieger` | 2287, 68, -644 |

## Buildings (deur = stand outside the door, binnen = interior centre, npc = at the back wall, doos_min/doos_max = box). Lampionhaven: vliegerwinkel (shop), winkel (general shop), herberg (inn), bank, lampionwerkplaats + kookhuis (workshops), havenmeester, botenverhuur (boat dock office), stal, visrestaurant 'De Blije Bot', woning_N (houses). Islands: opvangcentrum (Knuffeltij), bijenboerderij (Bloemeneiland), vliegerwerkplaats, vuurtorenwachter, schildpadhutje

| id | x, y, z |
|---|---|
| `gebouw.bank.binnen` | 1825, 72, -480 |
| `gebouw.bank.deur` | 1829, 72, -480 |
| `gebouw.bank.doos_max` | 1828, 82, -476 |
| `gebouw.bank.doos_min` | 1822, 71, -484 |
| `gebouw.bank.npc` | 1824, 73, -480 |
| `gebouw.bijenboerderij.binnen` | 2083, 74, -414 |
| `gebouw.bijenboerderij.deur` | 2083, 74, -411 |
| `gebouw.bijenboerderij.doos_max` | 2087, 83, -412 |
| `gebouw.bijenboerderij.doos_min` | 2079, 73, -417 |
| `gebouw.bijenboerderij.npc` | 2083, 74, -415 |
| `gebouw.botenverhuur.binnen` | 1870, 66, -494 |
| `gebouw.botenverhuur.deur` | 1870, 66, -490 |
| `gebouw.botenverhuur.doos_max` | 1872, 76, -491 |
| `gebouw.botenverhuur.doos_min` | 1868, 65, -497 |
| `gebouw.botenverhuur.npc` | 1870, 67, -495 |
| `gebouw.havenmeester.binnen` | 1860, 66, -443 |
| `gebouw.havenmeester.deur` | 1860, 66, -447 |
| `gebouw.havenmeester.doos_max` | 1862, 75, -440 |
| `gebouw.havenmeester.doos_min` | 1857, 65, -446 |
| `gebouw.havenmeester.npc` | 1860, 67, -442 |
| `gebouw.herberg.binnen` | 1825, 68, -466 |
| `gebouw.herberg.deur` | 1829, 68, -466 |
| `gebouw.herberg.doos_max` | 1828, 77, -463 |
| `gebouw.herberg.doos_min` | 1822, 67, -470 |
| `gebouw.herberg.npc` | 1824, 68, -466 |
| `gebouw.kookhuis.binnen` | 1852, 67, -441 |
| `gebouw.kookhuis.deur` | 1852, 67, -445 |
| `gebouw.kookhuis.doos_max` | 1854, 79, -438 |
| `gebouw.kookhuis.doos_min` | 1849, 66, -444 |
| `gebouw.kookhuis.npc` | 1852, 67, -440 |
| `gebouw.lampionwerkplaats.binnen` | 1830, 67, -448 |
| `gebouw.lampionwerkplaats.deur` | 1835, 67, -448 |
| `gebouw.lampionwerkplaats.doos_max` | 1834, 76, -445 |
| `gebouw.lampionwerkplaats.doos_min` | 1826, 66, -452 |
| `gebouw.lampionwerkplaats.npc` | 1828, 67, -448 |
| `gebouw.opvangcentrum.binnen` | 1933, 67, -361 |
| `gebouw.opvangcentrum.deur` | 1933, 67, -356 |
| `gebouw.opvangcentrum.doos_max` | 1940, 78, -357 |
| `gebouw.opvangcentrum.doos_min` | 1925, 66, -365 |
| `gebouw.opvangcentrum.npc` | 1933, 68, -363 |
| `gebouw.schildpadhutje.binnen` | 2234, 72, -444 |
| `gebouw.schildpadhutje.deur` | 2234, 72, -441 |
| `gebouw.schildpadhutje.doos_max` | 2236, 80, -442 |
| `gebouw.schildpadhutje.doos_min` | 2231, 71, -447 |
| `gebouw.schildpadhutje.npc` | 2234, 72, -445 |
| `gebouw.stal.binnen` | 1815, 67, -452 |
| `gebouw.stal.deur` | 1818, 67, -452 |
| `gebouw.stal.doos_max` | 1817, 77, -449 |
| `gebouw.stal.doos_min` | 1812, 66, -456 |
| `gebouw.stal.npc` | 1814, 67, -452 |
| `gebouw.visrestaurant.binnen` | 1909, 66, -496 |
| `gebouw.visrestaurant.deur` | 1909, 66, -492 |
| `gebouw.visrestaurant.doos_max` | 1914, 76, -493 |
| `gebouw.visrestaurant.doos_min` | 1904, 65, -500 |
| `gebouw.visrestaurant.npc` | 1909, 67, -498 |
| `gebouw.vliegerwerkplaats.binnen` | 2287, 77, -650 |
| `gebouw.vliegerwerkplaats.deur` | 2287, 77, -646 |
| `gebouw.vliegerwerkplaats.doos_max` | 2291, 86, -647 |
| `gebouw.vliegerwerkplaats.doos_min` | 2282, 76, -653 |
| `gebouw.vliegerwerkplaats.npc` | 2287, 77, -651 |
| `gebouw.vliegerwinkel.binnen` | 1844, 75, -485 |
| `gebouw.vliegerwinkel.deur` | 1844, 75, -481 |
| `gebouw.vliegerwinkel.doos_max` | 1848, 85, -482 |
| `gebouw.vliegerwinkel.doos_min` | 1840, 74, -488 |
| `gebouw.vliegerwinkel.npc` | 1844, 76, -486 |
| `gebouw.vuurtorenwachter.binnen` | 2411, 85, -495 |
| `gebouw.vuurtorenwachter.deur` | 2411, 85, -492 |
| `gebouw.vuurtorenwachter.doos_max` | 2414, 93, -493 |
| `gebouw.vuurtorenwachter.doos_min` | 2408, 84, -498 |
| `gebouw.vuurtorenwachter.npc` | 2411, 85, -496 |
| `gebouw.winkel.binnen` | 1843, 67, -441 |
| `gebouw.winkel.deur` | 1843, 67, -445 |
| `gebouw.winkel.doos_max` | 1846, 78, -438 |
| `gebouw.winkel.doos_min` | 1840, 66, -444 |
| `gebouw.winkel.npc` | 1843, 68, -440 |
| `gebouw.woning_1.binnen` | 1809, 70, -485 |
| `gebouw.woning_1.deur` | 1809, 70, -482 |
| `gebouw.woning_1.doos_max` | 1812, 79, -483 |
| `gebouw.woning_1.doos_min` | 1806, 69, -488 |
| `gebouw.woning_1.npc` | 1809, 70, -486 |
| `gebouw.woning_10.binnen` | 1840, 67, -427 |
| `gebouw.woning_10.deur` | 1840, 67, -431 |
| `gebouw.woning_10.doos_max` | 1843, 76, -424 |
| `gebouw.woning_10.doos_min` | 1836, 66, -430 |
| `gebouw.woning_10.npc` | 1840, 67, -426 |
| `gebouw.woning_11.binnen` | 1839, 67, -413 |
| `gebouw.woning_11.deur` | 1839, 67, -417 |
| `gebouw.woning_11.doos_max` | 1842, 76, -410 |
| `gebouw.woning_11.doos_min` | 1836, 66, -416 |
| `gebouw.woning_11.npc` | 1839, 67, -412 |
| `gebouw.woning_12.binnen` | 1853, 67, -429 |
| `gebouw.woning_12.deur` | 1853, 67, -433 |
| `gebouw.woning_12.doos_max` | 1856, 77, -426 |
| `gebouw.woning_12.doos_min` | 1850, 66, -432 |
| `gebouw.woning_12.npc` | 1853, 67, -428 |
| `gebouw.woning_13.binnen` | 1855, 83, -494 |
| `gebouw.woning_13.deur` | 1855, 83, -491 |
| `gebouw.woning_13.doos_max` | 1858, 92, -492 |
| `gebouw.woning_13.doos_min` | 1852, 82, -497 |
| `gebouw.woning_13.npc` | 1855, 83, -495 |
| `gebouw.woning_14.binnen` | 1855, 67, -415 |
| `gebouw.woning_14.deur` | 1855, 67, -419 |
| `gebouw.woning_14.doos_max` | 1857, 77, -413 |
| `gebouw.woning_14.doos_min` | 1852, 66, -418 |
| `gebouw.woning_14.npc` | 1855, 67, -414 |
| `gebouw.woning_15.binnen` | 1865, 85, -497 |
| `gebouw.woning_15.deur` | 1865, 85, -494 |
| `gebouw.woning_15.doos_max` | 1867, 94, -495 |
| `gebouw.woning_15.doos_min` | 1862, 84, -500 |
| `gebouw.woning_15.npc` | 1865, 85, -498 |
| `gebouw.woning_16.binnen` | 1867, 67, -429 |
| `gebouw.woning_16.deur` | 1867, 67, -433 |
| `gebouw.woning_16.doos_max` | 1869, 78, -427 |
| `gebouw.woning_16.doos_min` | 1864, 66, -432 |
| `gebouw.woning_16.npc` | 1867, 67, -428 |
| `gebouw.woning_17.binnen` | 1875, 87, -505 |
| `gebouw.woning_17.deur` | 1875, 87, -502 |
| `gebouw.woning_17.doos_max` | 1878, 96, -503 |
| `gebouw.woning_17.doos_min` | 1872, 86, -508 |
| `gebouw.woning_17.npc` | 1875, 87, -506 |
| `gebouw.woning_18.binnen` | 1877, 66, -494 |
| `gebouw.woning_18.deur` | 1877, 66, -490 |
| `gebouw.woning_18.doos_max` | 1879, 79, -491 |
| `gebouw.woning_18.doos_min` | 1874, 65, -497 |
| `gebouw.woning_18.npc` | 1877, 66, -495 |
| `gebouw.woning_19.binnen` | 1877, 66, -438 |
| `gebouw.woning_19.deur` | 1877, 66, -442 |
| `gebouw.woning_19.doos_max` | 1879, 76, -435 |
| `gebouw.woning_19.doos_min` | 1874, 65, -441 |
| `gebouw.woning_19.npc` | 1877, 66, -437 |
| `gebouw.woning_2.binnen` | 1814, 66, -437 |
| `gebouw.woning_2.deur` | 1814, 66, -441 |
| `gebouw.woning_2.doos_max` | 1817, 77, -434 |
| `gebouw.woning_2.doos_min` | 1810, 65, -440 |
| `gebouw.woning_2.npc` | 1814, 66, -436 |
| `gebouw.woning_20.binnen` | 1879, 86, -517 |
| `gebouw.woning_20.deur` | 1879, 86, -514 |
| `gebouw.woning_20.doos_max` | 1881, 95, -515 |
| `gebouw.woning_20.doos_min` | 1876, 85, -520 |
| `gebouw.woning_20.npc` | 1879, 86, -518 |
| `gebouw.woning_21.binnen` | 1884, 66, -494 |
| `gebouw.woning_21.deur` | 1884, 66, -490 |
| `gebouw.woning_21.doos_max` | 1887, 78, -491 |
| `gebouw.woning_21.doos_min` | 1881, 65, -497 |
| `gebouw.woning_21.npc` | 1884, 66, -495 |
| `gebouw.woning_22.binnen` | 1883, 66, -438 |
| `gebouw.woning_22.deur` | 1883, 66, -442 |
| `gebouw.woning_22.doos_max` | 1885, 77, -435 |
| `gebouw.woning_22.doos_min` | 1881, 65, -441 |
| `gebouw.woning_22.npc` | 1883, 66, -437 |
| `gebouw.woning_23.binnen` | 1890, 66, -438 |
| `gebouw.woning_23.deur` | 1890, 66, -442 |
| `gebouw.woning_23.doos_max` | 1892, 78, -435 |
| `gebouw.woning_23.doos_min` | 1887, 65, -441 |
| `gebouw.woning_23.npc` | 1890, 66, -437 |
| `gebouw.woning_24.binnen` | 1891, 66, -494 |
| `gebouw.woning_24.deur` | 1891, 66, -490 |
| `gebouw.woning_24.doos_max` | 1893, 77, -491 |
| `gebouw.woning_24.doos_min` | 1889, 65, -497 |
| `gebouw.woning_24.npc` | 1891, 66, -495 |
| `gebouw.woning_25.binnen` | 1896, 66, -438 |
| `gebouw.woning_25.deur` | 1896, 66, -442 |
| `gebouw.woning_25.doos_max` | 1898, 76, -435 |
| `gebouw.woning_25.doos_min` | 1894, 65, -441 |
| `gebouw.woning_25.npc` | 1896, 66, -437 |
| `gebouw.woning_26.binnen` | 1898, 66, -494 |
| `gebouw.woning_26.deur` | 1898, 66, -490 |
| `gebouw.woning_26.doos_max` | 1900, 76, -491 |
| `gebouw.woning_26.doos_min` | 1895, 65, -497 |
| `gebouw.woning_26.npc` | 1898, 66, -495 |
| `gebouw.woning_27.binnen` | 1903, 66, -438 |
| `gebouw.woning_27.deur` | 1903, 66, -442 |
| `gebouw.woning_27.doos_max` | 1905, 77, -435 |
| `gebouw.woning_27.doos_min` | 1900, 65, -441 |
| `gebouw.woning_27.npc` | 1903, 66, -437 |
| `gebouw.woning_28.binnen` | 1905, 66, -494 |
| `gebouw.woning_28.deur` | 1905, 66, -490 |
| `gebouw.woning_28.doos_max` | 1908, 79, -491 |
| `gebouw.woning_28.doos_min` | 1902, 65, -497 |
| `gebouw.woning_28.npc` | 1905, 66, -495 |
| `gebouw.woning_3.binnen` | 1815, 75, -497 |
| `gebouw.woning_3.deur` | 1815, 75, -494 |
| `gebouw.woning_3.doos_max` | 1817, 84, -495 |
| `gebouw.woning_3.doos_min` | 1812, 74, -500 |
| `gebouw.woning_3.npc` | 1815, 75, -498 |
| `gebouw.woning_4.binnen` | 1819, 80, -513 |
| `gebouw.woning_4.deur` | 1819, 80, -510 |
| `gebouw.woning_4.doos_max` | 1821, 89, -511 |
| `gebouw.woning_4.doos_min` | 1816, 79, -516 |
| `gebouw.woning_4.npc` | 1819, 80, -514 |
| `gebouw.woning_5.binnen` | 1823, 66, -417 |
| `gebouw.woning_5.deur` | 1823, 66, -421 |
| `gebouw.woning_5.doos_max` | 1826, 78, -414 |
| `gebouw.woning_5.doos_min` | 1820, 65, -420 |
| `gebouw.woning_5.npc` | 1823, 66, -416 |
| `gebouw.woning_6.binnen` | 1825, 82, -499 |
| `gebouw.woning_6.deur` | 1825, 82, -496 |
| `gebouw.woning_6.doos_max` | 1828, 91, -497 |
| `gebouw.woning_6.doos_min` | 1822, 81, -502 |
| `gebouw.woning_6.npc` | 1825, 82, -500 |
| `gebouw.woning_7.binnen` | 1827, 67, -433 |
| `gebouw.woning_7.deur` | 1827, 67, -437 |
| `gebouw.woning_7.doos_max` | 1830, 79, -430 |
| `gebouw.woning_7.doos_min` | 1824, 66, -436 |
| `gebouw.woning_7.npc` | 1827, 67, -432 |
| `gebouw.woning_8.binnen` | 1833, 87, -517 |
| `gebouw.woning_8.deur` | 1833, 87, -514 |
| `gebouw.woning_8.doos_max` | 1836, 96, -515 |
| `gebouw.woning_8.doos_min` | 1830, 86, -520 |
| `gebouw.woning_8.npc` | 1833, 87, -518 |
| `gebouw.woning_9.binnen` | 1835, 86, -502 |
| `gebouw.woning_9.deur` | 1835, 86, -499 |
| `gebouw.woning_9.doos_max` | 1837, 95, -500 |
| `gebouw.woning_9.doos_min` | 1832, 85, -505 |
| `gebouw.woning_9.npc` | 1835, 86, -503 |

## Islands (centre)

| id | x, y, z |
|---|---|
| `eiland_bloemeneiland` | 2085, 71, -405 |
| `eiland_knuffeltij` | 1935, 67, -345 |
| `eiland_koraalbaai` | 2185, 64, -565 |
| `eiland_mysterie` | 2480, 79, -330 |
| `eiland_pierkop` | 2470, 73, -565 |
| `eiland_strandfeest` | 1995, 68, -605 |
| `eiland_vliegereiland` | 2299, 89, -665 |
| `eiland_vuurtoreneiland` | 2404, 83, -490 |

## Campfires (unlit `minecraft:campfire[lit=false]`, kern checkpoints)

| id | x, y, z |
|---|---|
| `kampvuur_bloemeneiland` | 2089, 70, -401 |
| `kampvuur_knuffeltij` | 1949, 67, -343 |
| `kampvuur_koraalbaai` | 2177, 71, -615 |
| `kampvuur_lampionhaven` | 1839, 67, -460 |
| `kampvuur_mysterie` | 2466, 77, -324 |
| `kampvuur_pierkop` | 2466, 71, -573 |
| `kampvuur_strandfeest` | 1989, 68, -595 |
| `kampvuur_vliegereiland` | 2278, 64, -641 |
| `kampvuur_vuurtoreneiland` | 2404, 82, -486 |

## Arenas (`Z4WereldPlekken.ARENAS`: centre / deur = where you wait / bord = respawn board)

| id | x, y, z |
|---|---|
| `arena_vliegervanger` | 2158, 67, -700 |
| `arena_vliegervanger_bord` | 2178, 67, -697 |
| `arena_vliegervanger_deur` | 2165, 67, -700 |
| `arena_zeeslurf` | 2185, 64, -565 |
| `arena_zeeslurf_bord` | 2184, 65, -587 |
| `arena_zeeslurf_deur` | 2185, 65, -585 |

## Knuffeltij-strand (story core: a FIXED set of knuffels, quest only)

| id | x, y, z |
|---|---|
| `droogveld` | 1933, 67, -346 |
| `knuffeltij_feest` | 1937, 67, -339 |
| `knuffeltij_knuffel_1` | 1895, 60, -323 |
| `knuffeltij_knuffel_2` | 1905, 60, -319 |
| `knuffeltij_knuffel_3` | 1917, 58, -315 |
| `knuffeltij_knuffel_4` | 1931, 57, -313 |
| `knuffeltij_knuffel_5` | 1943, 57, -312 |
| `knuffeltij_knuffel_6` | 1955, 62, -315 |
| `knuffeltij_knuffel_7` | 1967, 66, -321 |
| `knuffeltij_knuffel_8` | 1979, 64, -331 |
| `knuffelwas` | 1931, 67, -352 |
| `npc_opvangleider` | 1933, 67, -361 |
| `soepkraam` | 1944, 66, -359 |

## Strandfeesteiland

| id | x, y, z |
|---|---|
| `npc_limonadeguh` | 2003, 68, -611 |
| `strandfeest_bar` | 2003, 68, -606 |
| `strandfeest_dansvloer` | 1989, 68, -600 |
| `strandfeest_volleybal` | 2021, 67, -603 |

## Bloemeneiland

| id | x, y, z |
|---|---|
| `bijenkorven` | 2094, 71, -406 |
| `kransenbank` | 2087, 72, -403 |
| `npc_imker` | 2083, 74, -415 |
| `picknick_1` | 2096, 70, -392 |
| `picknick_2` | 2072, 71, -394 |
| `picknick_3` | 2104, 72, -406 |

## Koraalbaai (diving jetty, lagoon raft arena, 3 underwater grottos; `_uitgang` = the water surface above it)

| id | x, y, z |
|---|---|
| `duiksteiger` | 2185, 64, -605 |
| `duiksteiger_eind` | 2185, 65, -585 |
| `grotje_1` | 2171, 43, -565 |
| `grotje_1_uitgang` | 2171, 63, -565 |
| `grotje_2` | 2201, 48, -563 |
| `grotje_2_uitgang` | 2201, 63, -563 |
| `grotje_3` | 2183, 46, -579 |
| `grotje_3_uitgang` | 2183, 63, -579 |
| `npc_duikmeester` | 2186, 65, -603 |
| `zeeslurf_grotje` | 2171, 47, -565 |

## Vliegereiland

| id | x, y, z |
|---|---|
| `vliegerwedstrijd_finish` | 2335, 75, -660 |
| `vliegerwedstrijd_start` | 2294, 85, -659 |
| `wedstrijdpaal` | 2302, 91, -665 |

## Vuurtoreneiland (the big lighthouse: door south, spiral stair to the gallery)

| id | x, y, z |
|---|---|
| `vuurtoren_binnen` | 2398, 89, -500 |
| `vuurtoren_deur` | 2400, 89, -492 |
| `vuurtoren_lamp` | 2400, 128, -500 |

## Mysterie-eiland (in the Grauwe Mistbank: client fog)

| id | x, y, z |
|---|---|
| `mysterie_flash` | 2480, 79, -332 |
| `mysterie_poort` | 2480, 80, -327 |
| `mysterie_strand` | 2454, 66, -326 |

## Schildpadeiland (static fallback; the loop = `SCHILDPAD_ROUTE`)

| id | x, y, z |
|---|---|
| `schildpad` | 2239, 72, -445 |
| `schildpad_lus_midden` | 2290, 63, -420 |
| `schildpad_rug` | 2243, 71, -449 |

## Wrak van de Vliegervanger (small dungeon: deck ramp -> hatch -> 3 rooms below deck -> boss on the fore deck)

| id | x, y, z |
|---|---|
| `wrak_aanleg` | 2192, 64, -700 |
| `wrak_ingang` | 2178, 67, -700 |
| `wrak_knuffelkooi` | 2155, 57, -700 |
| `wrak_kombuis` | 2165, 57, -700 |
| `wrak_luik` | 2176, 67, -705 |
| `wrak_oorlogsvlieger` | 2154, 78, -700 |
| `wrak_ruim` | 2175, 57, -700 |
| `wrak_schat` | 2153, 58, -697 |

## Viewpoints

| id | x, y, z |
|---|---|
| `uitkijk_lampionduin` | 1860, 90, -517 |
| `uitkijk_pierkop` | 2509, 73, -559 |
| `uitkijk_vliegerheuvel` | 2301, 91, -663 |
| `uitkijk_vuurtoren` | 2400, 128, -495 |

## Secrets

| id | x, y, z |
|---|---|
| `geheim_knuffel_1` | 1916, 63, -426 |
| `geheim_knuffel_2` | 2201, 48, -563 |
| `geheim_knuffel_3` | 2350, 70, -585 |
| `geheim_knuffel_4` | 2488, 81, -326 |
| `geheim_knuffel_5` | 1790, 69, -610 |
| `geheim_knuffel_6` | 2241, 69, -457 |
| `geheim_schat_1` | 2519, 64, -564 |
| `geheim_schat_2` | 2240, 64, -640 |
| `geheim_schat_3` | 2183, 46, -579 |
| `geheim_schat_4` | 2425, 63, -400 |
