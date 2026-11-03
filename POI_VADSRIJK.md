# POI_VADSRIJK — coordinates of zone 1 (wereld slice)

Status: **v0 (planned, 2026-11-03)**. x/z are final unless marked `~`; y values marked `~` get exact after the first tile
generation (v1). The Java mirror of this list is `nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken` (wereld slice).
All positions are block positions in `guhs:vadsrijk`; "stand" = the air block a player/NPC stands in (floor + 1).
Sea level = 62 (water top). Mainland world coords = zonekaart.py coords - 600.

## 1. Arrival + travel
| what | stand pos | notes |
|---|---|---|
| **AANKOMST** (first entry, Knuffelkamp dock, land end of the steiger) | ~(96, 87, 1015) | kern's `Gebieden.AANKOMST` (0,65,1130) should move here (adapter note). The camp sits on a cliff plateau at y 86 |
| Airship dock Knuffelkamp (bell + captain) | ~(104, 87, 1012) bell, captain ~(102, 87, 1018) | moored balloon ship north of the steiger |
| Airship dock De Haven (bell + captain at the foot of the mast) | (86, 66, 488) bell, captain (84, 66, 492) | mast top platform ~(92, 115, 500) |
| Zwamzoet city gate (Guhpoort) | (0, 67, 78) | the main axis runs north: gate -> market -> plein -> temple mushroom |

## 2. Campfires (unlit vanilla `minecraft:campfire[lit=false]`, kern checkpoint)
| area | positions (campfire block) |
|---|---|
| knuffelkamp | ~(0, 87, 1025) the camp fire pit (4 campfires around it, all unlit), ~(-74, 92, 946) lighthouse |
| zwamzoet | (-14, 67, 58) plein SW |
| haven | (100, 66, 452) |
| klodderweide | ~(230, y, -130), ~(410, y, 200), ~(480, y, -80) |
| sporenwoud | ~(-220, y, 10), ~(-430, y, 230), ~(-300, y, 340) |
| druipgrotten | ~(-260, y, -300), ~(-460, y, -460), ~(-180, y, -480) |
| zwamtempel | ~(200, y, -240) (before the fossil bridge, ring), ~(400, y, -300) mountain foot, ~(330, ~152, -380) temple stairs top |

## 3. Arenas (centre / radius / door = where you stand at the door, outside)
| arena | centre | radius | door | sign (respawn board) |
|---|---|---|---|---|
| Grauwe Klodderreus — Knuffelkamp pond | ~(54, 87, 1069) | 18 | ~(38, 87, 1058) (path from the camp) | ~(36, 88, 1056) |
| Koning Klodder — Klodderweide | (410, ~70, 90) | 32 | (374, ~70, 90) (west, the path from Zwamzoet) | (372, ~71, 86) |
| Opperzwam — hall in the Zwamtempel | (330, 152, -470) | 22 | (330, 152, -446) (south, from puzzle room 2) | (334, 153, -445) |

## 4. Zwamtempel (dungeon), zone 1 kerker
| what | pos | notes |
|---|---|---|
| Temple outer door (moving door id `zwamtempel`) | wings x 324..336, y 152..175, z -392 | 13 wide x 24 high, opens with `WereldDeuren.open("zwamtempel", p)` |
| Voorhal | centre (330, 152, -408) | |
| Puzzle room 1 "Lichtzwammen" | centre (302, 152, -430) | 4 light-mushroom pedestals at (302±7, 153, -430±7); door to the hall corridor |
| Puzzle room 2 "Hendels en platen" | centre (358, 152, -430) | 3 levers on the north wall (352/358/364, 154, -439), 4 pressure plates at (358±4, 152, -430±4) |
| Inner door (moving door id `zwamtempel_binnen`) | x 327..333, y 152..159, z -447 | between the corridor and the Opperzwam hall; bazen/npc open it |
| Opperzwam hall | centre (330, 152, -470), radius 22 | floor y 151 (stand 152); 4 light-mushroom spots for phase 2 at (316/344, 153, -456/-484); ceiling dome y 176..192 over r<=20: everything in that dome above y 176 is "breakable ceiling" (bazen removes it in phase 3; the sky is open above it) |
| Hidden room behind puzzle room 1 | (288, 152, -430) | cracked wall at x 293 (blocks `minecraft:cracked_stone_bricks`) |

## 5. Zwamzoet (city, ground stand y 67) — NPC spots + buildings (interiors hollow, floor + counter + lights)
| what | door (outside, stand) | interior centre (stand) | notes |
|---|---|---|---|
| Ordetempel (Tempel van De Knuffelorde, class change/respec, job advancement) | Knuffeldeur wings x -7..7, y 180..209, z -32 (moving door id `ordetempel`); stand in front (0, 180, -29) | (0, 180, -44) | on the highest cap; lift foot at (0, 67, -18) |
| Smidse (smid) | (-35, 67, 49) | (-44, 67, 49) | anvil + forge |
| Veilinghuis / Marktguh (auction) | (33, 67, 49) | (43, 67, 49) | counter + 4 boards |
| Bank | (29, 67, 25) | (29, 67, 18) | vault door at the back |
| Uitrustingwinkel (weapons/armour) | (-29, 67, 25) | (-29, 67, 18) | |
| Kruidenier/drankjes (consumables) | (-31, 67, 71) | (-38, 67, 71) | |
| Kleermaker (cosmetics that go home) | (31, 67, 71) | (38, 67, 71) | |
| Questbord | (-6, 67, 31) | - | big board north side of the plein |
| Marktplein (plein + ~20 stalls) | centre (0, 67, 50), r 22; stalls ring r 27 | - | Mika-spion disguise stall = the stall at ~(-27, 67, 50) |
| Stal (mounts) | (-135, 67, 2) | (-146, 67, 2) | paddock x -158..-136, z 16..34 |
| Havenmeester | (-64, 67, 79) | (-64, 67, 74) | at the kade (city harbour, bay north end) |
| Citizens' houses | listed in v1 (ground houses + 3-5 houses on every cap) | | doors face the cap centre |

## 6. De Haven (airship harbour, bay mouth east shore)
Village x 70..120, z 420..560, ground stand y 66. Mast (86..98, 66..115, 494..506), warehouse (104, 66, 520), pier into the bay at x 40..70, z 470.

## 7. Viewpoints (rock needles; panorama scene on first arrival; top platform stand)
| name | top stand |
|---|---|
| Klodderkijk | (520, ~150, -240) |
| Zwamnaald | (-510, ~150, -40) |
| Druipspits | (-100, ~160, -510) |
| Haventand | (160, ~140, 380) |
| Slijmnaald | (460, ~140, 380) |

## 8. Grey dragon scars (block `guhs:vr_w_litteken_gras` + `guhs:vr_w_litteken_plant`; grey until `zone1_klaar`)
Strips ~40 wide along polylines (world x,z):
1. (-640,580) (-420,440) (-240,300) (-130,220) (-40,160)
2. (20,-130) (160,-280) (300,-380) (640,-640)
3. (100,630) (220,450) (380,320) (640,220)

## 9. Secrets
| what | pos | notes |
|---|---|---|
| Mika-clue 1: black fluff tuft under a market stall | (-27, 67, 54) | block `minecraft:black_wool` carpet under the stall counter |
| Mika-clue 2: Mika footprints in the haven warehouse | (106, 66, 522) | `guhs:mika_steen` tiles |
| Mika-clue 3: torn disguise cloak in a hollow mushroom (Sporenwoud) | ~(-380, y, 60) | |
| Mika-clue 4: Mika scratches in the crystal chamber (Druipgrotten) | (-490, 52, -520) | |
| Mika-clue 5: spy's lookout on the roof of the east cap (Zwamzoet) | ~(66, 157, -36) | |
| Hidden crystal chamber (Druipgrotten) | centre (-490, 52, -520) | reached through a narrow crack from cave 2 |
| Waterfall room (Sporenwoud) | (-470, ~70, 0) | behind the waterfall |
| Slime-hill treasure (in the sleeping giant slime hill) | (300, ~66, 300) | tunnel from its north side |
| Turtle island treasure | (520, ~66, 720) | far out at sea |
| Lighthouse cellar (Knuffelkamp) | ~(-84, ~86, 937) | |

## 10. Mob spawn zones (boxes x1,z1..x2,z2; ground)
| area | mobs | boxes |
|---|---|---|
| knuffelkamp | (lieve slijmpjes, passive deco) | pond rim around (54,1069) r 24 |
| klodderweide | Klodder, Bubbelklodder (near ponds), Stuiterklodder, Moederklodder (elite) | (180,-280)..(580,380) minus arena r40 and the slime hill r50; ponds: (330,-70) r30, (470,160) r25, (250,260) r25, (520,40) r20 |
| sporenwoud | Hoedjesbol, Sporenpuffer, Zwamwachter, Mosgeest | (-580,-140)..(-180,380) |
| druipgrotten | Druipslijm, Kristalklodder, Grauwe Knaag, Glimzwam | surface (-580,-580)..(-180,-180); caves under y 75 around the 3 entrances (-300,-220), (-420,-300), (-180,-360) |
| zwamtempel | Tempelzwam-ridders, Sporenpriester | temple interior x 280..380, z -500..-395, y 150..170; mountain path (180,-580)..(580,-310) |

## 11. Other landmarks
- Sleeping giant slime hill: centre (300, ~66, 300), 100 x 70 wide, ~28 high, face to the south.
- The Kloof (ravine): polyline (40,-130) (100,-170) (170,-230) (240,-300) (280,-350) (360,-260) (450,-220), ~30 wide, floor y ~30.
- Dragon-fossil bridge: from (150,-215) to (215,-285) at y ~70.
- Zwamtempel mountain: summit (330, ~215, -470); temple plateau y 151.
- Turtle island: (520, 62, 720).
- Map edge: sea to |x| 704, z -704..1215, then a cloud bank.
