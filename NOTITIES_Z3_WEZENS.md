# NOTITIES_Z3_WEZENS: zone 3 "De Schemerwortel", slice wezens

Branch `110-z3-wezens`, worktree `guhs_vadsrijk/z3-wezens` (from ab6d952ac). Package `feature/vadsrijk/zone3/wezens`, ids
`guhs:vr_z3_mob_*` (entities + items), loot tables `z3wezens:*`, lang `vadsrijk.z3wezens.*`, generator
`tools/features/vadsrijk_z3_wezens.py` (run: `PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_z3_wezens.py`;
it reuses the zone-1 helpers of `vadsrijk_wezens.py` and the zone-2 builders of `vadsrijk_z2_wezens.py`).

## What is there
| part | where |
|---|---|
| base | `mob/Z3Wezen` (on zone-2 `Z2Wezen`): `wezenAard()` ROT = grey fluff + dead leaves blown away, MIKA = smoke puff to its zeppelin + "Dit komt in mijn rapport!", BEEST = dust + leaves, GEEST = dark wisps, HARS = amber shards (`Z3Verslagen`, particles only, nothing gory). Synced flags: `donker` (lantern rule), `contract` + `eigenaars` (contract elites). Layer-aware safe-spot backstop. Own tips `vadsrijk.z3wezens.tip.<naam>`. `standaardDoelen` uses the zone-1 `Aggro` goal (aggro range, not vanilla's 34-block follow range). `mob/Z3Vliegwezen` = code-steered flyers over the LOCAL floor. |
| Kruinzee lv 30-34 | Bladrover (MIKA, Vuur: hook along a red line 1 s, yanks you in + slash; crew of 3 at the zeppelin dock), Grauwe Rups (ROT, Natuur: curls (red line 1 s), rolls, 2 s dizzy = x1.5), Bijtbloesem (ROT, Natuur: disguised as blossom without nameplate, never moves, bite + yellow spore cone 0.9 s → Gesporen) |
| Takkenbanen lv 34-38 | Kaasschutter (MIKA, Vuur: keeps 8-12 away, seeks height, red aim line 1 s, cheese ball → Brandend, flees when close), Schorsmarter (BEEST, Natuur: pairs, leaps from above onto a red circle 0.8 s), Grauwe Wesp (ROT, Natuur: one swarm entity of 5 wasps, stings → Gesporen) |
| Schorsstad lv 40-43 | Schorskruiper (BEEST: hidden as bark, bursts out within 4, web → Plakkerig patch, re-hides after 12 s idle), Lantaarnloze (GEEST, Water: floater, every ~9 s blows the light out (grey circle 1 s → Darkness 6 s, its bolts x1.5 while out); CLICK it to relight = 5 s stunned), Harsgolem (HARS, Water: slam red circle 1.2 s + sticky puddle; below 30 % an Amberroze crystal = x1.5 from behind) |
| Wortelvelden lv 43-46 | Rotzwijn (ROT: paws 1.5 s along a long red line, charges, wall = 2 s dizzy), Wortelslang (BEEST, Water: hidden underground, dust trail + red circle 0.8 s, bursts up (knock-up), 6 s above then dives; only hittable above), Mika-schimmelboer (MIKA, Water: sprays mould fields (Gesporen, purple circle 0.9 s; max 4, they die with it) + calls 2 Schimmelbolletjes every 20 s) |
| Wortelduister lv 46-49 | Wortelschim (GEEST, Water: in the dark only a shimmer, slips behind you (smoke) + short red line 0.6 s + claw; fleeting guh ears), De Dove Ziener (ROT: blind, hears sprinting/fighting within 16, walking within 5, never sneaking; roars + 2 Wortelschimmen; sneak attack on an unaware Ziener x2; loses you after 3 s of silence; slam red circle 1.2 s) |
| Lantern rule | `Lantaarnlicht`: in the Wortelduister (y < 62, r ≤ 470) a creature outside every light is `donker`: drawn near-black (Wortelschim: almost invisible, eyes/ears glow), -50 % damage taken ("Te donker!"). Light = a player carrying any item whose id contains `nestlantaarn` (hand or inventory, cached 1 s) within 12, block light ≥ 10 (pink veins, lit lanterns), or `Lantaarnlicht.BRONNEN` / `DRAGERS` (hooks for bazen/systemen). `Lantaarnlicht.inLicht(level, pos)` is public for the bosses. |
| Elites | kern affixes (6 % from spawners), zone-1 look (x1.25 + gold layer). 5 named area leaders (`z3wezens:leider_*`, always elite, lv area max + 3, 5 min respawn): Rupsenmoeder, Kapitein-Bladrover, Harskolos, Wilde Rotkeiler, Ziener-oudste. |
| Contract elites | `Z3Contracten` (8): Ouwe Rotkeiler (Rotzwijn, charges twice), Moeder Wesp (splits into 3 small swarms at 50 %), De Harsreus (3 rings of shockwaves), Kapitein Kaaskop (3 balls in a fan), Grootvader Rups (rolls 3x), De Stille Ziener (hears sneaking within 6, 3 shadows), Slangenkoningin Sapsie (sap pools), Bramzwijn de Brede (3-wide charge + bramble patch). Each 2 fixed affixes, lv 45-50, ~6x health, x1.5 size, own colours (`contract_<id>.png`). NEVER in a spawner. |
| Spawners | `Z3WezensSpawners`: per LAYER, only inside z3-wereld's MOBZONES, all in kern cave mode `grot(minY, maxY)` (floor search inside the layer, "player near" inside the y-range). Crown: grid over the 3 crown zones; branches: beside the Takbaan ridge in the branch zones; Schorsstad: 5 per ring terrace (y 81-177, r 58); Wortelvelden: grid over the 4 valley zones with a mix per valley; Wortelduister: grid over tunnel + caves. Spacing chosen per layer for ≤ 100 alive. Paths: plan-time no circle within 5 of a path AT ITS HEIGHT (path heights by name: kruinring 196-320, takbaan 120-250, wortelweg 50-145, wortelgang 0-80) and a kern `Spawners.VERBODEN` rule at run time (same check + safe spots). `/guhs vadsrijk z3wezens spawners` shows the numbers. |
| Critters | `mob/Z3Critter` + `Z3Critters`: honingbij (the existing guh_bee model/texture/anim, 70 % size), sporenmot (glows), lantaarnkever (glowing tail), wortelmuis; up to 6 within 32 (3D) of a zone-3 player, floor near the player's own height; kind by layer and day/night; flee from sprinting players; untouchable, no loot. |
| Loot | `loot/Z3Loot`: 15 base types: Bloesemset 4 (Behendigheid/Kritiek), Schorsset 4 (Verdediging/Kracht, plan §9.4), Wortelset 4 (Wijsheid/Elementweerstand), jewellery honingring / harsketting / sporenoorbel. A table + `_elite` per kind, `z3wezens:contract_<id>` (3 rolls, luck 6). 8 trophies `vr_z3_mob_trofee_<id>` (EPIC, stack 1). `Z3Loot.voegWapenToe(basisType, gewicht)` for another slice's zone-3 weapons. |
| Sets | `loot/Z3Sets`: Bloesem 2 = +6 Kritiek, 4 = crits leave the creature Gesporen; Schors 2 = +12 Verdediging, 4 = below 50 % a resin shield (Knuffelschild 15 % max Vads, 6 s, once per 30 s); Wortel 2 = +8 Elementweerstand, 4 = you are your own light in the Wortelduister + 10 % vs rot creatures. Tooltips in `client/Z3WezensClient`. |
| Client | `client/Z3WezensClient`: one renderer for all monsters (model + texture + glowmask + elite layer; contract texture/size; silhouette colour when `donker`; `shouldRender` false for players outside a contract), critter renderers, bee renderer, nameplates hidden for hidden/disguised/dark-Wortelschim/other people's contract. |
| Dev command | `/guhs vadsrijk z3wezens spawn|affix|alle|contract|jacht|set|sieraden|loot|licht|opruimen|spawners` |
| Gametests | `VadsrijkZ3WezensGameTests` (5): every kind spawns + defeats + vanishes; loot tables/items/jewellery/contract tables/trophies; contract elite (affixes, ~6x health, only its owner hurts it, board hook fires); lantern rule (dark = half damage, a froglight is light); spawners per layer (55-105 alive, cave mode inside the layer, 5 from paths at their height, not in safe spots, no contract target) + a two-floor cave-mode spawn lands on the right floor |
| AutoCheck | `tools/autocheck/vadsrijk_z3_wezens.txt` (line-up on moss in the proeftuin, an elite, a contract elite, fluff/smoke, the sets, modelcheck) |

## Merged: 110-z3-systemen (ca18781a8) into this branch
- Merged for compile-time use (conflicts only in the one-line wiring lists and the lang files; lang = theirs + this generator re-run).
- **Xp:** every creature's `xp()` = `Z3Wezen.xp3(weight)` = `Z3Xp.mob(level)` (1/55 of a level) x a weight per kind (Grauwe Rups/Rotzwijn 1.2, Wortelslang 1.1, Harsgolem 1.5, Dove Ziener 1.6, Schimmelbolletje 0.25); elites x3 as before. Contract xp is the board's (`Z3Xp.contract`, paid by z3-systemen's `Contracten.verslagen` through my `VERSLAGEN` hook).
- **Layered areas:** the lantern rule's "is this the Wortelduister/Hart" uses `Gebieden.op(x, y, z)` (z3-systemen's `Z3Lagen`); kern's `Spawners.magHier` is now 3D too (towns by height), so the note about Honingtak blocking the ground below no longer applies.
- **Weapons:** the creature tables drop z3-systemen's six zone-3 class weapons (`Z3Wapens.ALLE`) instead of the zone-1 ones.
- **Contracts:** z3-systemen wires itself by reflection to `Z3Contracten.spawnElite(ServerPlayer, String)`, `VERSLAGEN` and `VERTROKKEN` (names/signatures kept).
- **Bounties:** not used here (event bounties are `Premies.activeer/meld`; z3-systemen counts kills itself via Haken.VERSLAGEN).

## API for other slices
- **Bounty board (z3-npc / z3-systemen):** `Z3Contracten.spawnElite(ServerPlayer p, String id)` → the target at its lair (`Z3WezensPlekken.HOLEN`, near a Pluimpost) for p + their party, or the one already out, or null. Ids: `Z3Contracten.IDS`. Lair for the map trail: `Z3Contracten.hol(id)`. Active? `Z3Contracten.actief(p, id)`. Hooks: `Z3Contracten.VERSLAGEN` (player, id) per contract player who helped — the board pays Jachtpenningen, the daily chest and the 3 % Pluimguh pattern there; `Z3Contracten.VERTROKKEN` (id, players) when it left (12 min or 30 s without its players). The trophy + a rich loot roll drop here.
- **Lantern light (bazen: Pelszaal phase 3, the Hart):** `Lantaarnlicht.inLicht(level, pos)`, add light sources with `Lantaarnlicht.BRONNEN.add((level, pos) -> ...)`; the Nestlantaarn just has to have "nestlantaarn" in its item id.
- **Weapons in the drops:** `Z3Loot.voegWapenToe(basisTypeId, gewicht)`.

## Dependencies / things integration must lift
1. **Wiring (kern-owned files, done here, one line each):** `VadsrijkFeature.register` (Z3WezensRegistry), `client/VadsrijkClient.init` (Z3WezensClient), `tools/features/__init__.py` FEATURES (`vadsrijk_z3_wezens`).
2. **z3-wereld:** this branch carries a COPY of POI_Z3 v1 (mob zones, paths, campfires, arenas, towns) in `Z3WezensPlekken`; once `zone3.wereld.Z3WereldPlekken` is on the classpath its live `MOBZONES`, `PADEN`, `KAMPVUREN` and `plek(id)` are read by REFLECTION (no compile dependency) and win. Optional plek ids it may add: `leider_<naam>` (the 5 leaders) and `hol_<id>` (the 8 lairs); else the defaults in `Z3WezensPlekken.LEIDERS/HOLEN` are used (guesses inside the right zones, the cave/branch ones unverified).
3. **Cave/branch spots are unverified on the real map:** spawners whose circle has no floor in their y-range simply never spawn (kern skips). Run `/guhs vadsrijk z3wezens spawners` and walk the Wortelduister after the map exists; a leader whose landmark has no floor 24 below never appears (move its `leider_*` spot).
4. **Kern 2D things that ignore height:** `Spawners.magHier` refuses spots within 12 FLAT of any player (also a player on another layer) and kern's 2D `Gebieden.op(x,z).stad()`: if z3-systemen registers Honingtak as a flat kern area, nothing spawns on the ground under Honingtak either (no problem: Honingmoeras is south of it). Do NOT register zone-3 paths in kern's 2D `Spawners.registreerPad` (it would block every layer); this slice checks paths per height itself.
5. **Crystals:** none dropped here (as in zone 2 after integration, z3-systemen drops zone-3 crystals itself via Haken.VERSLAGEN).
6. Equipment textures for the worn armour need the minecraft jar in `build/` (run the generator after a gradle build).

## Decisions (made alone)
- Real monsters, look generated (judge in the dev client): Mika's = the zone-2 Mika body with a pirate patch/coat, a tall hat + cheese barrel, a straw hat + spray tank; rot creatures grey with pale fluff tufts; the Ziener has no eyes at all; only critters have the small friendly dot eyes; the Wortelschim keeps two tiny guh ears (plan §6).
- ZONE3_PLAN lists "Ouwe Rotkeiler" both as the Wortelvelden area elite and as a contract target; the area leader is called **Wilde Rotkeiler** so the contract one stays unique (round 13: no rare named random spawns).
- Elements per the plan table (Harsgolem/Lantaarnloze/Wortelslang/Schimmelboer/Wortelschim Water, Mika pirates Vuur, the rest Natuur).
- Numbers: the zones 1-2 curve (HP base x (1 + 0.3/lv), damage x (1 + 0.22/lv)), bases 30-100 → ~360-1200 HP at lv 30-49.
- Sets: the plan's Schorsset (wild drops) + two wild sets of my own (Bloesemset, Wortelset); the dungeon sets (Bladrover, Nestwachter) and the Premiejagerset are bazen's / systemen's.
- The Lantaarnloze's "relight" = clicking it while its lantern is out (no item needed).

## Open choices for the user
- Looks and colours of all creatures (generated; dev client).
- Spawn density (≤ 100 alive per layer) and the elite rate (6 %).
- Contract elite strength (~6x, lv 45-50) and lifetime (12 min).

## Resume point
See `git log -1`.

## Test results (2026-11-04)
- `runGameTestServer -Pgt=VadsrijkZ3Wezens` on the merged tree: "All 6 required tests passed" (spawner plan: kruinzee 101, takkenbanen 87, schorsstad 63, wortelvelden 99, wortelduister 98 max alive; 202 spawners; z3-systemen logs "hunting contracts wired to z3-wezens Z3Contracten").
- AutoCheck NOT run (the machine is low on memory; all 3 gradle slots were busy all session). Script ready: `tools/autocheck/vadsrijk_z3_wezens.txt`. Looks unverified in a client.
