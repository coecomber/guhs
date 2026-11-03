## Moving doors (`WereldDeuren`)
| id | blocks | open with | notes |
|---|---|---|---|
| `ordetempel` | x -7..7, y 177..206, z -32..-31 (the Knuffeldeur, 15 x 30) | `WereldDeuren.open("ordetempel", p)` | the ordetempel hall is also reachable through a normal side door (`ordetempel_zijdeur`), so class change never needs the ceremony |
| `zwamtempel` | x 324..336, y 152..175, z -392 (13 x 24) | `WereldDeuren.open("zwamtempel", p)` | the dungeon entrance (npc/bazen decide when a player may open it) |
| `zwamtempel_binnen` | x 328..332, y 152..158, z -447 | `WereldDeuren.open("zwamtempel_binnen", p)` | corridor -> Opperzwam hall; bazen opens it when both puzzles are solved and may keep it shut during the fight (`WereldDeuren.sluit(sl, id, true)`) |

First open per player = a 7.5 s non-click ceremony scene (camera + title card), then the wings slide into the wall (dust, rumble).
Doors close again after 30 s (never on a player). A player who unlocked a door opens it by right-clicking it.
Every door has a hidden copy at y 8.. under the same x/z (the generator writes it; never build there).

## Opperzwam hall ceiling
The dome over the hall (centre 330, -472) is the "breakable ceiling" (phase 3): everything inside radius 23 between y 177 and y 194
is the dome shell (`guhs:guhpaddenstoel_hoed` + spots); removing it opens the hall to the sky (there is no rock above it).

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
