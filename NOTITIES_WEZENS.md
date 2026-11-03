# NOTITIES_WEZENS — slice "wezens" (Guhs 1.10.0 Het Vadsrijk, zone 1)

Branch `110-vr-wezens`, worktree `guhs_vadsrijk/w-wezens`. Package `feature/vadsrijk/wezens`, ids `vr_mob_*`,
generator `tools/features/vadsrijk_wezens.py` (run: `PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_wezens.py`).

## What is there
| part | where |
|---|---|
| 15 creatures on `VadsrijkMob` (+ GeoEntity) | `mob/RijkWezen` (base: anims, elite flag, split copies, elite loot, tips), `mob/Slijmwezen` (hop AI), `mob/Klodders`, `mob/Zwammen`, `mob/Anderen` |
| Klodderweide | Klodder, Kloddertje (small, from the Moederklodder), Bubbelklodder (bubble shots → Plakkerig, keeps 4.5 blocks away), Stuiterklodder (telegraphed high bounce, AoE + Plakkerig), Moederklodder (always elite, spits 2 kloddertjes at 66 % and 33 %, bursts into 3 on the plof) |
| Sporenwoud | Hoedjesbol (red line telegraph, then a ram; a wall = dizzy), Sporenpuffer (spore cloud 5 s → Gesporen), Zwamwachter (front shield 90 % block; side/back or the 3 s after its telegraphed slam are open), Mosgeest (floats, circles, moss balls, 30 % dodge every 3 s) |
| Druipgrotten | Druipslijm (hangs upside down from a ceiling ≤8 up, drops on you), Kristalklodder (35 % of spells bounce back for 50 %), Grauwe Knaag (grey rodent, NOT a guh; packs of 2-3, bite-and-hop-back, alerts its pack), Glimzwam (runs away, heals all creatures within 8 for 12 % every 4 s, glowing cap) |
| Zwamtempel | Tempelzwam-ridder (red strip telegraph, 0.9 s wind-up, heavy swing), Sporenpriester (keeps 6-13 away, spore bolts + Gesporen, every 8 s Vahoeg on all nearby + a Knuffelschild on the most hurt) |
| Elites | any creature 6 % (spawner); 1-2 affixes; synced flag → 25 % bigger + an additive gold glow layer + affix particles; SPLITSEND = two small copies on the plof; better loot (own `_elite` tables: 2 rolls, x3.5 chance, luck 2.5, x3 coins); a first-time tip |
| Plof | kern's default (pink particles, squeak, hop away 1.5 s, vanish) + an own happy sound + pink dust; the model switches to the bright `_blij` texture with a happy guh face and plays "blij" |
| Kampklodder | `Kampklodder` (PathfinderMob, passive, invulnerable, hops lazily round its home, right click = hearts + "aai" wobble + a Dutch line). `Kampklodder.plaats(level, pos)` (moves an existing one within 64, never makes two) |
| Vahoegnir | `draak/Vahoegnir` (no AI, not saved, not hittable; model x2.5 ≈ 15 blocks long, ~20 wingspan; flap/glide/roar anims; grey+black fluff, ash, witch sparks raining) + `draak/DraakVlucht` API (below) |
| Loot | `loot/WezensLoot`: 14 armour base types (items `guhs:vr_mob_<id>`), a table + elite table per creature `wezens:<naam>`; the kern weapons (kussenzwaard/knuffelknots/pluishamer) are in every table via `WezensLoot.extraWapens` |
| Sets | `loot/SetBonussen` (adapter, see below), tooltip lines "Klodderset (n/4)" in `client/WezensClient` |
| Spawners | `WezensSpawners` + all spots in `WezensPlekken` (aligned with POI_VADSRIJK v0 §10) |
| Dev command | `/guhs vadsrijk wezens spawn|affix|draak|overvlucht|kampklodder|set|loot|opruimen|spawners` |
| Gametests | `VadsrijkWezensGameTests` (4): floor spawner, Moederklodder plof + split, loot tables valid, set counting |

### DraakVlucht API (for npc: opening cutscene; for wereld/kern: far circles)
```java
Vahoegnir d = DraakVlucht.vlieg(level, List.of(a, b, c, d), 200);   // Catmull-Rom spline through all points, roars at ~1/3
DraakVlucht.vlieg(level, pad, ticks, false);                         // no automatic roar
DraakVlucht.cirkel(level, midden, straal, hoogteY, ticks);           // one circle
DraakVlucht.brul(d, true);                                           // roar now; true = also heard by far-away players
```
Keep paths within ~150 blocks of the watching players (entities only track/tick in loaded chunks). Nothing schedules
far circles automatically: wereld/kern decide when (design: "af en toe").

## Kern adapters / things integration must lift
1. **Set stats**: `SetBonussen.stats(p)` (+4 Behendigheid / +4 Wijsheid at 2 pieces) is NOT counted yet: kern should add
   `s.plus(SetBonussen.stats(p))` in `Spelers.stats` (or a generic stat hook). The effect parts (dodge, +12 % damage,
   -10 % damage, Plakkerig/Gesporen procs) already work through `Schade.VOOR_KLAP`/`NA_KLAP`.
2. **Floor-search spawning**: kern's `Spawners` always uses the heightmap and a 3D 64-block player check from the centre.
   Caves and the temple rooms need a floor search (else mobs spawn on the cave roof) and the mountain path needs a flat
   distance check. `WezensSpawners` does this itself (own tick every second in the Vadsrijk level). Kern could add a mode.
3. **Elite flag not synced** in `VadsrijkMob` (only the nameplate shows it): `RijkWezen` syncs its own `ELITE`.
4. **Spawner max is private** in kern: the Knaag pack uses `Anderen.WezensSpawnersMax`.
5. **Jewellery**: kern's `Slot` has no ring/ketting/oorbel, so there is no jewellery. Add slots (+ an accessory UI) first.
6. **Generator stubs were broken**: kern's stub `tools/features/vadsrijk_<slice>.py` files contain a raw line break inside
   `newline="\n"` (SyntaxError) → `make_resources.py` would crash on every stub not yet rewritten. Mine is rewritten.
7. **Armour has no look on the player**: kern's `harnas` items (and mine, same pattern) have an `Equippable` without an
   equipment asset, so worn armour is invisible. Fine for now; a later visual pass could add equipment assets.
8. Item icons live under `textures/item/vadsrijk/wezens/` (the blocks/items atlas only takes `textures/item|block/`),
   entity textures under `textures/vadsrijk/wezens/` as the contract says.
9. The Kampklodder is one shared world entity: npc/kern call `Kampklodder.plaats(level, WezensPlekken.KAMPKLODDER)` when the
   Klodderreus is beaten (per-player visibility is not possible with one entity).
10. Base type ids are plain (`klodderkapje`, ...); if economie also registers e.g. a "klodderkapje", rename one.

## Decisions (made alone, the user was away)
- Ontvadst look = the creature's own colours pulled to grey-purple + a grumpy guh face (heavy eyelids, brows, frown); after
  the plof its bright colours + a happy guh face with blush (not every creature turns pink; the plof dust/hearts are pink).
- Every slime/mushroom/mosgeest has little guh ears; the Knaag deliberately has rodent ears, beady eyes and buck teeth.
- Numbers: HP base x (1 + 0.3/level), damage base x (1 + 0.22/level), xp 12-34 + 2-4/level; elites x2.5 HP (kern), x1.4 damage.
- Spawning "lively, not crowded": 1-5 alive per spawner, 30-60 s respawn (Moederklodder 150 s), 6 % elites; 42 kern spawners + 19 own (caves, temple, mountain path).
- Set bonuses: Klodderset 2 = 15 % dodge, 4 = +12 % damage + 20 % Plakkerig proc; Zwamset 2 = -10 % damage taken,
  4 = +12 % spell damage + Gesporen on spell hits.
- Sounds: vanilla (slime squish/jump, bubbles, amethyst, mooshroom, goat, allay, bat, evoker, shield, mace, ender dragon).
- No Blockbench `.bbmodel` sources: the models are generated (allowed by the brief); they open fine in Blockbench as geo.json.

## Open choices for the user
- Colours per creature, the bow on the Moederklodder, the flower on the Kampklodder, the dragon's palette.
- Should the happy (plof) look be pink for every creature instead of its own bright colours?
- Druipslijm/Kristalklodder cave spots: y is a guess until the wereld slice publishes cave stand positions (POI_VERZOEKEN).
- Knaag element: GEEN (neutral). Could be Water (cave) if the triangle should matter there.

## Resume point
All done and committed; head = see `git log -1`. Remaining only if wanted: tuning after the user's dev-client look.

## Test results (2026-11-03)
- `compileJava` OK; gametests `-Pgt=VadsrijkWezens`: all 4 pass (+ kern's required one: "All 5 required tests passed").
- One AutoCheck client run (`tools/autocheck/vadsrijk_wezens.txt`): no crash, no errors for any wezens model/texture/item
  (modelcheck: only 4 pre-existing non-wezens problems: bliksemsmidse_onweerswolkje, droombed, guhmon_badgedoos). All 17 entities
  summoned fine. BUT the proeftuin was still void in that run, so the player and mobs fell out of the world and the screenshots
  show only sky: no visual check of the models yet. The script now lays its own grass platform first (not re-run: one client
  run per slice). FTB's "Missing entity icon settings" errors are the same for every guhs entity (harmless).
