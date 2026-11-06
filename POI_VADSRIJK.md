# POI_VADSRIJK - coordinates of zone 1 (wereld slice)

**v1, GENERATED** by `tools/vadsrijk/maak_kaart.py` from the real map (do not edit by hand; requests go to `../POI_VERZOEKEN.md`). Java: `nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken` (`WereldPlekken.plek("id")`, ids below). Dev: `/guhs vadsrijk wereld plek <id>` teleports there.

All positions are **stand positions** (the air block a player/NPC stands in) in `guhs:vadsrijk`, unless a key says otherwise (`doos` = bounding box corners, door `min/max` = the door's own blocks). Sea level 62; mainland ground ~64-70; Zwamzoet ground stand y 64; Knuffelkamp plateau stand y 87.

## Arrival, travel, ways home

| id | x, y, z |
|---|---|
| `aankomst` | -384, 65, 1130 |
| `aankomst_trap_top` | -384, 88, 1102 |
| `lift_oost_top` | 52, 151, -42 |
| `lift_oost_voet` | 50, 64, -42 |
| `lift_tempel_top` | 0, 171, -20 |
| `lift_tempel_voet` | 0, 64, -18 |
| `lift_west_top` | -52, 141, -33 |
| `lift_west_voet` | -50, 64, -33 |
| `luchtschip_bel` | -235, 87, 1008 |
| `luchtschip_haven_bel` | 86, 65, 488 |
| `luchtschip_haven_kapitein` | 86, 65, 491 |
| `luchtschip_haven_landing` | 92, 65, 484 |
| `luchtschip_haven_mast_top` | 92, 115, 500 |
| `luchtschip_kapitein` | -235, 87, 1010 |
| `steiger_eind` | -233, 87, 1015 |
| `terugportaal_haven` | 111, 65, 474 |
| `terugportaal_kamp` | -384, 65, 1155 |
| `terugportaal_zwamzoet` | -43, 64, 30 |

## Campfires (unlit `minecraft:campfire[lit=false]`, kern checkpoints) + ridder-guh spots

| id | x, y, z |
|---|---|
| `kampvuur_druipgrotten_1` | -260, 98, -311 |
| `kampvuur_druipgrotten_2` | -468, 99, -460 |
| `kampvuur_druipgrotten_3` | -180, 74, -488 |
| `kampvuur_haven` | 100, 65, 452 |
| `kampvuur_kamp` | -384, 87, 1025 |
| `kampvuur_klodderweide_1` | 240, 69, -126 |
| `kampvuur_klodderweide_2` | 418, 73, 198 |
| `kampvuur_klodderweide_3` | 489, 65, -81 |
| `kampvuur_sporenwoud_1` | -228, 65, 17 |
| `kampvuur_sporenwoud_2` | -436, 72, 224 |
| `kampvuur_sporenwoud_3` | -300, 72, 348 |
| `kampvuur_vuurtoren` | -457, 92, 946 |
| `kampvuur_zwamtempel_1` | 138, 79, -236 |
| `kampvuur_zwamtempel_2` | 225, 85, -346 |
| `kampvuur_zwamtempel_trap` | 318, 152, -381 |
| `kampvuur_zwamzoet` | -14, 64, 58 |
| `ridder_druipgrotten_1` | -258, 98, -309 |
| `ridder_druipgrotten_2` | -466, 99, -458 |
| `ridder_druipgrotten_3` | -178, 74, -486 |
| `ridder_klodderweide_1` | 242, 69, -124 |
| `ridder_klodderweide_2` | 420, 73, 200 |
| `ridder_klodderweide_3` | 491, 65, -79 |
| `ridder_sporenwoud_1` | -226, 65, 19 |
| `ridder_sporenwoud_2` | -434, 72, 226 |
| `ridder_sporenwoud_3` | -298, 72, 350 |
| `ridder_zwamtempel_1` | 140, 79, -234 |
| `ridder_zwamtempel_2` | 227, 85, -344 |

## Arenas (centre / straal / deur = stand outside the door / bord = respawn board)

| id | x, y, z |
|---|---|
| `arena_klodderreus.bord` | -361, 88, 1059 |
| `arena_klodderreus.centrum` | -330, 87, 1069 |
| `arena_klodderreus.deur` | -360, 87, 1061 |
| `arena_koning_klodder.bord` | 375, 69, 85 |
| `arena_koning_klodder.centrum` | 410, 69, 90 |
| `arena_koning_klodder.deur` | 374, 69, 90 |
| `arena_koning_klodder.veilige_plekken.1` | 410, 69, 110 |
| `arena_koning_klodder.veilige_plekken.2` | 392, 69, 80 |
| `arena_koning_klodder.veilige_plekken.3` | 427, 69, 79 |
| `arena_opperzwam.bord` | 334, 152, -469 |
| `arena_opperzwam.centrum` | 330, 152, -497 |
| `arena_opperzwam.deur` | 330, 152, -470 |
| `arena_opperzwam.lichtzwammen.1` | 317, 152, -484 |
| `arena_opperzwam.lichtzwammen.2` | 343, 152, -484 |
| `arena_opperzwam.lichtzwammen.3` | 317, 152, -510 |
| `arena_opperzwam.lichtzwammen.4` | 343, 152, -510 |
| `arena_opperzwam.plafond.centrum` | 330, 177, -497 |

## Zwamtempel (dungeon)

| id | x, y, z |
|---|---|
| `deur_zwamtempel.max` | 336, 175, -392 |
| `deur_zwamtempel.min` | 324, 152, -392 |
| `deur_zwamtempel.voor` | 330, 152, -388 |
| `geheim_tempelkamer` | 311, 152, -416 |
| `geheim_tempelkamer_muur` | 317, 152, -416 |
| `zwamtempel_voorhal` | 330, 152, -398 |

## Zwamzoet: ordetempel, plein, questbord, hats (cap tops)

| id | x, y, z |
|---|---|
| `guhpoort` | 0, 64, 82 |
| `hoed_hoeko` | 104, 118, -88 |
| `hoed_hoekw` | -104, 116, -76 |
| `hoed_noordo` | 40, 120, -94 |
| `hoed_noordw` | -34, 128, -92 |
| `hoed_oost` | 66, 154, -36 |
| `hoed_tempel` | 0, 174, -42 |
| `hoed_west` | -66, 144, -30 |
| `hoed_zuido` | 78, 130, 32 |
| `hoed_zuidw` | -78, 122, 32 |
| `ordetempel_altaar` | 0, 177, -47 |
| `ordetempel_binnen` | 0, 177, -44 |
| `ordetempel_zijdeur` | 14, 177, -44 |
| `plein` | 0, 64, 59 |
| `questbord` | -7, 64, 31 |

## Zwamzoet buildings (deur = stand outside the door, binnen = interior centre, doos = bounding box)

| id | x, y, z |
|---|---|
| `gebouw.bank.1.binnen` | 25, 64, 12 |
| `gebouw.bank.1.deur` | 25, 64, 17 |
| `gebouw.bank.1.doos.1` | 20, 63, 8 |
| `gebouw.bank.1.doos.2` | 30, 76, 16 |
| `gebouw.havenmeester.1.binnen` | -68, 64, 72 |
| `gebouw.havenmeester.1.deur` | -68, 64, 77 |
| `gebouw.havenmeester.1.doos.1` | -72, 63, 68 |
| `gebouw.havenmeester.1.doos.2` | -64, 74, 76 |
| `gebouw.huis.1.binnen` | -89, 64, 51 |
| `gebouw.huis.1.deur` | -85, 64, 51 |
| `gebouw.huis.1.doos.1` | -92, 63, 48 |
| `gebouw.huis.1.doos.2` | -86, 73, 54 |
| `gebouw.huis.2.binnen` | 87, 64, 51 |
| `gebouw.huis.2.deur` | 83, 64, 51 |
| `gebouw.huis.2.doos.1` | 84, 63, 48 |
| `gebouw.huis.2.doos.2` | 90, 73, 54 |
| `gebouw.huis.3.binnen` | 63, 64, -9 |
| `gebouw.huis.3.deur` | 63, 64, -5 |
| `gebouw.huis.3.doos.1` | 60, 63, -12 |
| `gebouw.huis.3.doos.2` | 66, 73, -6 |
| `gebouw.huis.4.binnen` | -19, 64, -69 |
| `gebouw.huis.4.deur` | -19, 64, -65 |
| `gebouw.huis.4.doos.1` | -22, 63, -72 |
| `gebouw.huis.4.doos.2` | -16, 73, -66 |
| `gebouw.huis.5.binnen` | 17, 64, -69 |
| `gebouw.huis.5.deur` | 17, 64, -65 |
| `gebouw.huis.5.doos.1` | 14, 63, -72 |
| `gebouw.huis.5.doos.2` | 20, 73, -66 |
| `gebouw.kleermaker.1.binnen` | 41, 64, 68 |
| `gebouw.kleermaker.1.deur` | 35, 64, 68 |
| `gebouw.kleermaker.1.doos.1` | 36, 63, 64 |
| `gebouw.kleermaker.1.doos.2` | 46, 75, 72 |
| `gebouw.kruidenier.1.binnen` | -41, 64, 68 |
| `gebouw.kruidenier.1.deur` | -35, 64, 68 |
| `gebouw.kruidenier.1.doos.1` | -46, 63, 64 |
| `gebouw.kruidenier.1.doos.2` | -36, 75, 72 |
| `gebouw.smidse.1.binnen` | -45, 64, 48 |
| `gebouw.smidse.1.deur` | -39, 64, 48 |
| `gebouw.smidse.1.doos.1` | -50, 63, 43 |
| `gebouw.smidse.1.doos.2` | -40, 77, 53 |
| `gebouw.uitrusting.1.binnen` | -25, 64, 12 |
| `gebouw.uitrusting.1.deur` | -25, 64, 17 |
| `gebouw.uitrusting.1.doos.1` | -30, 63, 8 |
| `gebouw.uitrusting.1.doos.2` | -20, 76, 16 |
| `gebouw.veilinghuis.1.binnen` | 41, 64, 47 |
| `gebouw.veilinghuis.1.deur` | 33, 64, 47 |
| `gebouw.veilinghuis.1.doos.1` | 34, 63, 40 |
| `gebouw.veilinghuis.1.doos.2` | 48, 80, 54 |

## Knuffelkamp

| id | x, y, z |
|---|---|
| `ordetent` | -384, 87, 991 |
| `tempeltje` | -436, 94, 1065 |
| `training` | -322, 87, 981 |
| `vijver` | -330, 87, 1069 |
| `vuurtoren_top` | -468, 125, 932 |

## De Haven + Stal

| id | x, y, z |
|---|---|
| `haven_pakhuis` | 103, 65, 520 |
| `haven_plein` | 90, 65, 470 |
| `haven_z1_z2_bord` | 147, 127, -647 |
| `haven_z1_z2_eind` | 150, 127, -656 |
| `haven_z1_z2_mast` | 155, 139, -656 |
| `haven_z1_z2_plein` | 150, 127, -598 |
| `haven_z1_z2_vertrek` | 150, 127, -653 |
| `stal.binnen` | -149, 65, -4 |
| `stal.deur` | -143, 65, -4 |
| `stal.wei` | -146, 65, 25 |

## Viewpoints (top platform, panorama on first visit)

| id | x, y, z |
|---|---|
| `uitkijk_druipspits` | -100, 190, -510 |
| `uitkijk_haventand` | 160, 134, 380 |
| `uitkijk_klodderkijk` | 520, 147, -240 |
| `uitkijk_noordkliftop` | 154, 127, -656 |
| `uitkijk_slijmnaald` | 460, 142, 380 |
| `uitkijk_zwamnaald` | -510, 153, -40 |

## Mika-spion clues + secrets

| id | x, y, z |
|---|---|
| `geheim_hoedenschat` | -318, 96, 59 |
| `geheim_schildpad` | 524, 76, 722 |
| `geheim_slaapheuvel` | 300, 69, 296 |
| `geheim_slaapheuvel_ingang` | 300, 69, 266 |
| `geheim_vuurtorenkelder` | -470, 87, 935 |
| `geheim_waterval` | -470, 71, 0 |
| `mika_aanwijzing_1` | -28, 64, 50 |
| `mika_aanwijzing_2` | 108, 65, 520 |
| `mika_aanwijzing_3` | -380, 64, 60 |
| `mika_aanwijzing_4` | -487, 46, -520 |
| `mika_aanwijzing_5` | 77, 154, -36 |
| `mika_spion_kraam` | -28, 64, 52 |

## Landmarks

| id | x, y, z |
|---|---|
| `fossielbrug.naar` | 119, 79, -219 |
| `fossielbrug.van` | 151, 79, -181 |
| `grot_ingangen.1` | -300, 90, -220 |
| `grot_ingangen.2` | -420, 83, -300 |
| `grot_ingangen.3` | -180, 111, -360 |
| `grot_knopen.N1` | -320, 46, -300 |
| `grot_knopen.N2` | -400, 41, -380 |
| `grot_knopen.N3` | -260, 44, -420 |
| `grot_knopen.N4` | -460, 41, -470 |
| `kristalkamer` | -490, 46, -520 |
| `lichtstralen.1` | -340, 65, 120 |
| `lichtstralen.2` | -420, 71, 40 |
| `lichtstralen.3` | -240, 70, 220 |
| `lichtstralen.4` | -480, 75, 160 |
| `lichtstralen.5` | -300, 65, -40 |
| `lichtstralen.6` | -360, 72, 300 |
| `schildpad_eiland` | 520, 76, 720 |
| `slaapheuvel` | 300, 96, 300 |
| `waterval` | -470, 71, 12 |

## Other

| id | x, y, z |
|---|---|
| `druipschacht_balkon` | -500, 85, -487 |
| `druipschacht_bodem` | -496, 32, -487 |
| `druipschacht_ingang` | -463, 41, -471 |
| `druipschacht_lichtschacht` | -495, 121, -477 |
| `druipschacht_meer` | -495, 31, -477 |
| `druipschacht_plafond_1` | -507, 92, -483 |
| `druipschacht_plafond_2` | -504, 102, -477 |
| `druipschacht_plafond_3` | -497, 96, -465 |
| `druipschacht_plafond_4` | -488, 103, -481 |
| `druipschacht_plafond_5` | -484, 97, -481 |
| `druipschacht_plafond_6` | -485, 99, -474 |
| `druipschacht_richel_1` | -484, 42, -473 |
| `druipschacht_richel_2` | -507, 55, -479 |
| `druipschacht_richel_3` | -483, 67, -477 |
| `druipschacht_richel_4` | -507, 80, -476 |
| `druipschacht_waterval_1` | -489, 32, -475 |
| `druipschacht_waterval_2` | -500, 32, -473 |
| `druipschacht_waterval_3` | -496, 32, -483 |
| `hendelkamer.centrum` | 330, 152, -436 |
| `hendelkamer.hendels.1` | 322, 152, -441 |
| `hendelkamer.hendels.2` | 326, 152, -441 |
| `hendelkamer.hendels.3` | 334, 152, -441 |
| `hendelkamer.hendels.4` | 338, 152, -441 |
| `hendelkamer.hints.1` | 338, 157, -446 |
| `hendelkamer.hints.2` | 334, 157, -446 |
| `hendelkamer.hints.3` | 326, 157, -446 |
| `hendelkamer.hints.4` | 322, 157, -446 |
| `hendelkamer.plaat` | 330, 152, -444 |
| `hoedenpad_elitenest` | -409, 115, 180 |
| `hoedenpad_netwerk.hoeden.1` | -428, 106, 165 |
| `hoedenpad_netwerk.hoeden.10` | -352, 111, 201 |
| `hoedenpad_netwerk.hoeden.11` | -351, 104, 105 |
| `hoedenpad_netwerk.hoeden.12` | -343, 114, 142 |
| `hoedenpad_netwerk.hoeden.13` | -343, 96, 54 |
| `hoedenpad_netwerk.hoeden.14` | -332, 112, 166 |
| `hoedenpad_netwerk.hoeden.2` | -409, 115, 180 |
| `hoedenpad_netwerk.hoeden.3` | -392, 100, 151 |
| `hoedenpad_netwerk.hoeden.4` | -389, 110, 212 |
| `hoedenpad_netwerk.hoeden.5` | -377, 107, 191 |
| `hoedenpad_netwerk.hoeden.6` | -373, 113, 120 |
| `hoedenpad_netwerk.hoeden.7` | -369, 102, 82 |
| `hoedenpad_netwerk.hoeden.8` | -368, 111, 228 |
| `hoedenpad_netwerk.hoeden.9` | -360, 102, 159 |
| `hoedenpad_schat_kist` | -317, 96, 59 |
| `hoedenpad_top_holle_steel` | -314, 112, 131 |
| `hoedenpad_top_wenteltrap` | -421, 110, 199 |
| `hoedenpad_top_wortelpad` | -393, 100, 130 |
| `hoedenpad_trap_holle_steel` | -319, 66, 127 |
| `hoedenpad_trap_wenteltrap` | -428, 71, 196 |
| `hoedenpad_trap_wortelpad` | -395, 69, 135 |
| `hoedenpad_uitkijkhut_1` | -389, 110, 212 |
| `hoedenpad_uitkijkhut_2` | -373, 113, 120 |
| `hoedenpad_uitkijkhut_3` | -332, 112, 166 |
| `hoedenpad_wezen_1` | -426, 106, 165 |
| `hoedenpad_wezen_10` | -341, 96, 54 |
| `hoedenpad_wezen_2` | -390, 100, 151 |
| `hoedenpad_wezen_3` | -375, 107, 191 |
| `hoedenpad_wezen_4` | -367, 102, 82 |
| `hoedenpad_wezen_5` | -366, 111, 228 |
| `hoedenpad_wezen_6` | -358, 102, 159 |
| `hoedenpad_wezen_7` | -350, 111, 201 |
| `hoedenpad_wezen_8` | -349, 104, 105 |
| `hoedenpad_wezen_9` | -341, 114, 142 |
| `hoedhupperwei` | 520, 73, 300 |
| `knuffel_1` | -452, 89, 952 |
| `knuffel_10` | 326, 150, -374 |
| `knuffel_2` | -354, 86, 1040 |
| `knuffel_3` | -36, 64, 33 |
| `knuffel_4` | 70, 64, -70 |
| `knuffel_5` | 116, 65, 466 |
| `knuffel_6` | 300, 69, 250 |
| `knuffel_7` | 470, 75, 185 |
| `knuffel_8` | -400, 108, -100 |
| `knuffel_9` | -300, 90, -226 |
| `lichtzwamkamer.centrum` | 330, 152, -415 |
| `lichtzwamkamer.lichtzwammen.1` | 322, 152, -421 |
| `lichtzwamkamer.lichtzwammen.2` | 326, 152, -424 |
| `lichtzwamkamer.lichtzwammen.3` | 330, 152, -425 |
| `lichtzwamkamer.lichtzwammen.4` | 334, 152, -424 |
| `lichtzwamkamer.lichtzwammen.5` | 338, 152, -421 |
| `naaldtrap_druipspits` | -97, 108, -497 |
| `naaldtrap_haventand` | 155, 67, 368 |
| `naaldtrap_klodderkijk` | 508, 74, -234 |
| `naaldtrap_slijmnaald` | 450, 72, 372 |
| `naaldtrap_zwamnaald` | -497, 73, -39 |
| `npc_mopje` | -400, 41, -380 |
| `poorten.1` | 328, 152, -405 |
| `poorten.2` | 328, 152, -426 |
| `poorten.3` | 328, 152, -447 |
| `poorten.4` | 328, 152, -474 |
| `quest_dobber_hoed` | -444, 87, 955 |
| `quest_tuba` | -467, 68, 330 |
| `ridderzaal.centrum` | 330, 152, -460 |
| `ridderzaal.spawns.1` | 320, 152, -469 |
| `ridderzaal.spawns.2` | 330, 152, -471 |
| `ridderzaal.spawns.3` | 340, 152, -469 |
| `ridderzaal.spawns.4` | 318, 152, -458 |
| `ridderzaal.spawns.5` | 342, 152, -458 |
| `rondvlucht_schip` | -237, 106, 763 |

## Moving doors (`WereldDeuren`)
| id | blocks | open with | notes |
|---|---|---|---|
| `ordetempel` | x -7..7, y 177..206, z -32..-31 (the Knuffeldeur, 15 x 30) | `WereldDeuren.open("ordetempel", p)` | the ordetempel hall is also reachable through a normal side door (`ordetempel_zijdeur`), so class change never needs the ceremony |
| `zwamtempel` | x 324..336, y 152..175, z -392 (13 x 24) | `WereldDeuren.open("zwamtempel", p)` | the dungeon entrance (npc/bazen decide when a player may open it) |

First open per player = a 7.5 s non-click ceremony scene (camera + title card), then the wings slide into the wall (dust, rumble).
Doors close again after 30 s (never on a player). A player who unlocked a door opens it by right-clicking it.
Every door has a hidden copy at y 8.. under the same x/z (the generator writes it; never build there).

## Zwamtempel inside (as asked by bazen in POI_VERZOEKEN, moved onto the mountain: floor block y 151, stand y 152)
A straight chain north behind the big door: voorhal (x 314..346, z -404..-394, 18 high) -> gate at z -405 -> room 1
Lichtzwamkamer (x 318..342, z -425..-406, 10 high) -> gate z -426 -> room 2 Hendelkamer (z -446..-427) -> gate z -447 -> room 3
Ridderzaal (x 314..346, z -472..-448, 12 high) -> gate z -472..-476 -> the Opperzwam hall. The gates are open AIR (x 328..332,
y 152..155; the last one x 327..333, y 152..156): bazen fills them with its own gate blocks. The rooms have empty floors (no levers,
plates or mushrooms of mine; the listed spots are free).

## Opperzwam hall ceiling
The hall (centre 330, -497, open floor radius 22, walls 24 high) has a pink mushroom-cap dome: everything inside radius 23
from y 175 (`plafond.van_y`) to y 194 is dome shell or air under it (`guhs:guhpaddenstoel_hoed` + spots, all breakable);
removing it opens the hall to the sky (there is no rock above it).

## Area boxes (= kern `Gebieden`, contract §2) and what the terrain looks like
| area | box | terrain |
|---|---|---|
| zwamzoet | (-160,-160)..(160,160) | flat knuffelgras at y 63, 9 giant mushrooms, harbour on the bay (south) |
| haven | (-120,400)..(120,600) | bay mouth; the village on the east shore x 60..125, z 420..560, ground 64 |
| klodderweide | (160,-300)..(600,400) | green rolling meadow with pink patches, 5 ponds, the slime hill, the arena |
| sporenwoud | (-600,-160)..(-160,400) | moss/podzol/mycelium, ~200 giant mushrooms 25-60 high, 6 light beams, a waterfall rock |
| druipgrotten | (-600,-600)..(-160,-160) | grey ridged rock hills 70-130 with grassy hollows; caves at y 40-60 from 3 entrances |
| zwamtempel | (160,-600)..(600,-300) | pink-terracotta badlands mountain; temple plateau y 151; ramp path from the fossil bridge |
| knuffelkamp | (-128,900)..(128,1150) | cliff island, plateau y 86 |
| ring | rest | meadows, paths, the Kloof (ravine, y 30, stream) between the city and the mountain |

## Grey dragon scars (`guhs:vr_w_litteken_gras` + `guhs:vr_w_litteken_plant` + a few dead pale-oak trees)
Strips ~40 wide (noisy) along these polylines (world x,z). Grey until `SpelerData.extra("npc").zone1_klaar` = true for that player
(client tint, see `Littekens`):
1. (-640,580) (-420,440) (-240,300) (-130,220) (-40,160)
2. (20,-130) (160,-280) (300,-380) (640,-640)
3. (100,630) (220,450) (380,320) (640,220)

## Mob spawn zones (suggested boxes; ground y = terrain)
| area | mobs | where |
|---|---|---|
| knuffelkamp | (sweet pond slimes, passive) | pond rim around `vijver` r 24 |
| klodderweide | Klodder, Bubbelklodder (at the ponds), Stuiterklodder, Moederklodder (elite) | (180,-280)..(580,380) minus arena r 40 and slime hill r 50; ponds (330,-70) (470,160) (250,260) (520,40) (300,120) |
| sporenwoud | Hoedjesbol, Sporenpuffer, Zwamwachter, Mosgeest | (-580,-140)..(-180,380), between the mushrooms |
| druipgrotten | Druipslijm, Kristalklodder, Grauwe Knaag, Glimzwam | surface (-580,-580)..(-180,-180); caves: around `grot_knopen.*` (r 10) and along the tunnels between them |
| zwamtempel | Tempelzwam-ridders, Sporenpriester | voorhal + puzzle rooms (x 290..370, z -444..-394, y 152); mountain path |

## Map edge
Sea out to x -704..767 and z -704..1215 (the tile grid), a ring of pink/white cloud banks on the edge, an invisible barrier
at the outermost blocks so the sea never runs into the void.
