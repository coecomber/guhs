# NOTITIES_Z2_WEZENS — zone 2 "De Splinterpieken", slice wezens

Branch `110-z2-wezens`, worktree `guhs_vadsrijk/z2-wezens`. Package `feature/vadsrijk/zone2/wezens`, ids `guhs:vr_z2_mob_*`
(entities + items), loot tables `z2wezens:*`, lang `vadsrijk.z2wezens.*`, generator `tools/features/vadsrijk_z2_wezens.py`
(run: `PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_z2_wezens.py`; reuses the zone-1 helpers of `vadsrijk_wezens.py`).

## What is there
| part | where |
|---|---|
| base | `mob/Z2Wezen` (on zone-1 `RijkWezen`): no pink plof. `aard()` KRISTAL = shatters into grey glass shards that melt away (block displays shrink in 3 s), MIKA = smoke puff + a trail to the Mika fort + a shout, BEEST = snow dust. `versteend()` = kern's ontvadst (Roze x1.5). `verborgen()` = hidden: no nameplate, not hittable/pushable. Safe-spot backstop (forgets target, walks back). Extra drop: a raw crystal. `mob/Vliegwezen` = code-steered flyers/floaters. |
| Galmhellingen lv 20-25 | Splinterling (golem, GEEN+versteend: every hit it takes fires a fan of 3 shards back), Galmvleermuis (NATUUR, fights ~4 s, goes invisible + only audible echo, rising chime = tell, reappears behind you with a 1.6x bite), Sneeuwknabbelaar (WATER, pack of 3-4 per spawn, a howl alerts the pack, telegraphed lunge, one lunger at a time), Kristalnaaldkruiper (GEEN+versteend, buried with only needles showing; within 5: red circle 0.7 s then bursts out under you + knock-up; re-buries after 10 s idle) |
| Bevroren Knuffelmeer lv 25-30 | IJzelgeest (WATER floater, ice bolts, every 7 s an ice-blue circle under you that freezes for 5 s → Bevroren), Schotsbijter (WATER, waits under the ice; cracks + red circle 1 s under you, bursts up 1.6x + knock-up, fights 8 s, dives again), Versteende Wacht (GEEN+versteend, slow, 95 base HP, red circle slam 1.2 s → 2.4x + Verdoofd 1 s), IJspuffer (WATER, puffs up 0.75 s then a frost cloud 5 s: Bevroren + chip damage) |
| Splinterkloof lv 30-34 | Mika-mijnwerker (VUUR, pickaxe; 1 in 3 attacks a telegraphed overhead blow; < 30 % HP: panics "Mamaaa!", runs 5 s, comes back with 1-2 friends; friends go home after 30 s idle), Mika-boorder (VUUR, keeps 7-10 away, red aim line 1 s, drill bit → Brandend), Kloofgier (NATUUR, circles 9 up, red circle 1.25 s then dives; winded on the ground 2 s), Grauwe Kristalspin (GEEN+versteend, web shot → Plakkerig + a sticky patch 5 s) |
| Spiegelburcht lv 33-35 | Spiegelwacht (GEEN+versteend, mirror shield: ranged/magic from the front bounce back 40 % (WAAR), melee from the front x0.5; halberd strip telegraph), Scherfgeest (VUUR, blinks around you then a 3-shard fan) |
| Bevroren | `Bevroren` (adapter, see below): -30 % Loopsnelheid (players via `Spelers.EXTRA_STATS`, mobs via an attribute modifier); a HEAVY hit (>= 12 % of max Vads in one hit) does +30 % and breaks the ice ("IJs gebroken!"); vanilla frost overlay (ticksFrozen kept < 140: no vanilla freeze damage) + snowflakes; cleared on fainting; first-time tip |
| Elites | kern affixes via the spawners (6 %), zone-1 look (x1.25 + additive gold `_elite` layer + affix particles), `_elite` tables (2 rolls, x3.5 chance, luck 2.8, x3 coins, + jewellery), 35 % crystal chance |
| Spawners | `Z2WezensSpawners`: a jittered grid (105 apart, fixed seeds) per area, kind drawn from the area mix, max 2-4, 35-45 s respawn; ALL use kern's floor search from y 240 down (`grot(40, 240)`: first floor from above = snow/ledge/bridge, "player near" flat). Max alive: see `/guhs vadsrijk z2wezens spawners` (gametest asserts 55-105 per area). Burcht: 6 room spawners (floor search 24 down from a guessed room y). |
| Critters | `mob/Critter` + `Z2Critters`: sneeuwhaas (hops away), ijsvogel (flits low, lands), kristalmot (glows, mostly at night); up to 6 within 40 of a player in zone 2; untouchable, no loot, leave when nobody within 80 |
| Loot | `loot/Z2Loot`: 15 base types (4 loose armour, Kristalset 4, Mika-buitset 4, 3 jewellery: kristalring/spiegelketting/galmoorbel), a table + elite table per creature (zone-1 weapons via `WezensLoot.extraWapens`), crystals |
| Sets | `loot/Z2Sets`: Kristalset 2 = +8 Elementweerstand + Bevroren half as long + no heavy-hit extra; 4 = 20 % Splinterslag (25 % of the hit to everything within 3). Mika-buitset 2 = +5 Kritiek; 4 = +15 % vs versteend, +25 % first hit on a full-HP creature. Tooltip lines in `client/Z2WezensClient`. |
| Client | `client/Z2WezensClient`: one renderer (model + texture + always-on glowmask + elite layer, no shadow when hidden), critter renderers, nameplate hidden for hidden/defeated/invisible creatures (LOWEST priority after kern) |
| Dev command | `/guhs vadsrijk z2wezens spawn|affix|alle|bevroren|set|sieraden|loot|opruimen|spawners` |
| Gametests | `VadsrijkZ2WezensGameTests` (4): every kind spawns+defeats+vanishes (shards made), loot tables/items/jewellery, Bevroren heavy hit, spawner density + no spawner near a safe spot |
| AutoCheck | `tools/autocheck/vadsrijk_z2_wezens.txt` (line-up on a snow platform in the proeftuin, elite + Bevroren, shards/smoke, modelcheck) |

## Dependencies / things integration must lift
1. **Crystals (z2-systemen)**: drops reference `guhs:vr_z2_kristal_<soort>` with soorten `roze, ijs, galm, grauw, spiegel`
   (`Z2Loot.KRISTALLEN` + `kristalSoort`). A missing item is skipped silently. If z2-systemen picks other names, change
   `Z2Loot.kristalSoort` (one switch).
2. **Bevroren is an adapter** (kern `Status` has no BEVROREN): `zone2/wezens/Bevroren`. Kern can lift it into `Status`/`Statussen`
   (+ a HUD icon); the API (`geef/heeft/wis/klap/stats`) can then delegate. Other z2 slices (bazen: Galmwolf etc.) may call
   `Bevroren.geef(entity, ticks, bron)` directly.
3. **Wiring (kern-owned files)**: one line each in `VadsrijkFeature.register` and `client/VadsrijkClient.init`, plus one entry
   `'vadsrijk_z2_wezens'` in `tools/features/__init__.py` FEATURES.
4. **Coordinates are guesses** (POI_Z2.md did not exist yet): `Z2WezensPlekken` — area boxes per the brief, `VEILIG` (Kandijkom
   r200, dock, 9 guessed campfires r26, Galmwolf arena (470,-2150) r55, Spiegelburcht (0,-2900) r95, Mika-boormeester (-440,-2930) r40),
   `BURCHT` room spots (y 181/189 guessed), `MIKA_FORT`. Integration: replace them with Z2WereldPlekken/POI_Z2 values. Spawners
   are computed from these at startup (no other change needed).
5. **Splinterkloof ravine floors**: the surface floor search finds the top (rim/bridges), never the ravine bottom. If the ravine
   floor should have creatures, add spawners with a y-range around the floor (`.grot(minY, maxY)`) once POI_Z2 has heights.
6. Equipment textures for the worn armour need the minecraft jar in `build/` (run the generator after a gradle build).

## Decisions (made alone)
- Elements: Splinterling/Kristalnaaldkruiper/Versteende Wacht/Kristalspin/Spiegelwacht = GEEN + versteend (Roze x1.5); Water:
  Sneeuwknabbelaar, IJzelgeest, Schotsbijter, IJspuffer; Natuur: Galmvleermuis, Kloofgier; Vuur: both Mika's, Scherfgeest.
- Numbers: the zone-1 curve (HP base x (1 + 0.3/level), damage x (1 + 0.22/level)) with bases 34-95 → ~300-900 HP at lv 25-34.
- Looks: real monsters; glowing slit/fierce eyes, teeth, crystals; Mika's = upright pink cat-villains (round ears, thin tail,
  red evil eyes, black brows, one fang) with hard hats; only the critters have small friendly dot eyes (the "guh touch").
- Defeated bodies vanish after 4 ticks (zone 1 hops away; round 14's "~10 s visible" was for the happy plof, not for zone 2).
- Hidden creatures are not hittable (also not by AoE) so nobody wastes skills on an invisible target.

## Open choices for the user
- Colours/looks of all creatures (generated; judge in the dev client).
- Should the Mika's flee trail point to a real fort (POI) — now a guessed spot north-west.
- Spawner density (now ~70-85 max alive per area) and the elite rate (6 %).

## Resume point
See `git log -1`. Remaining: align coordinates with POI_Z2.md; tuning after the user's look.

## Test results (2026-11-04)
- `compileJava` OK; `runGameTestServer -Pgt=VadsrijkZ2Wezens`: all 4 pass ("All 5 required tests passed").
- One AutoCheck run (`tools/autocheck/vadsrijk_z2_wezens.txt`): no crash, no zone-2 model/texture/animation errors (only the 4
  pre-existing problems: droombed, bliksemsmidse_onweerswolkje, guhmon_badgedoos; FTB "Missing entity icon settings" as for every
  guhs entity). All 17 entities render on the snow platform; elite gold layer, Bevroren frost overlay + tip, crystal shards and
  the Mika smoke puff visible. Screenshots: `run/screenshots/autocheck/vr_z2_*.png` (not committed).
