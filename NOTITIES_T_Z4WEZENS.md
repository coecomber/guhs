# NOTITIES_T_Z4WEZENS (night round T, branch 110-t-z4wezens, from dbf82899d)

Zone 4 "De Lampionkust" (lv 50-56, a rest zone, LIGHT combat), ZONE4_PLAN Z4-1..3: the creatures, the world boss, the small
dungeon and their loot.
- **Packages:** `feature/vadsrijk/zone4/wezens` (+ `mob/`, `loot/`, `client/`) and `feature/vadsrijk/zone4/bazen` (+ `client/`).
- **Generator:** `tools/features/vadsrijk_t_z4wezens.py` (in FEATURES). Run it with
  `PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_z4wezens.py`.
- **Ids:**
  - creatures + gear: `guhs:vr_z4_mob_*`;
  - weapons: `guhs:vr_z4_<wapen>`;
  - crystals: `guhs:vr_z4_kristal_*`;
  - bosses/objects/boss items: `guhs:vr_z4_baas_*`.
- **Loot tables:** `z4wezens:*`.
- **Lang:** `vadsrijk.z4wezens.*`, `vadsrijk.z4bazen.*`.
- **Arenas:** `z4bazen:zeeslurf`, `z4bazen:vliegervanger`.

## What was built
### Creatures (`wezens/mob/Z4Wezen`, `wezens/mob/Kust`)
- **The base, `Z4Wezen`**, is built on the zone-1 `RijkWezen`, NOT on Z2/Z3Wezen, because those have no pink plof.
  - Ontvadst creatures: grey and grumpy; on defeat the pink plof, then the `_blij` texture (bright, blush) and ~10 s of happy hops.
  - The Mika crew instead leaves in a smoke puff (`mika()`).
  - Numbers: the shared curve (HP base x (1 + 0.3/lv), hit x (1 + 0.22/lv)) on gentle bases.
  - Xp: `Z3Xp.mob(level)` x a weight.
  - Plus: own tips, safe spots (`Z4WezensPlekken.veilig`), and flyers steering themselves over land or water (`vliegt()`/`stuur`).
- **The 10 kinds:**
  - Grauwe Mopperkrab: claws up = x0.5 from the front, in the Schade hook;
  - Koraalkobold: keeps its distance and throws shards;
  - Stekelpluis: never starts a fight; spike puff;
  - Zeurslak: sticky trail; hides in its shell once below 50 %;
  - Grauwe Lampionkwal: floats; sting circle;
  - Mokmeeuw: circles, dives along a red line, then sits for 2 s;
  - Bromvlieger: a kite; purple wind strip + tail lash;
  - Pruilrob: belly slide along a red line;
  - Mistmopje: melts into the fog and comes back behind you (Darkness);
  - Vliegerknecht: a Mika with a hook (Plakkerig + a tug).
- **Every special is telegraphed** (red = damage, purple = push).
- **Elites:** kern affixes at 4 % (rarer than zone 3), the zone-1 look.
- **Spawners (`Z4WezensSpawners`, spots in `Z4WezensPlekken`):**
  - one small spawner per region, max 2-3, slow respawn, ~40 alive max in the whole zone;
  - the regions follow t-z4wereld's MOBZONES: Koraalbaai ring, Vliegereiland, the outer Bloemeneiland fields, Mysterie-eiland, the
    Wrak, plus 3 rocks/sandbanks;
  - the Wrak's 3 rooms below deck and its deck are in cave mode (`grot(y)`);
  - NO creatures on Lampionhaven, the Knuffeltij-strand, the Strandfeest-, Vuurtoren- and Schildpadeiland or the Pierkop;
  - a kern VERBODEN rule and the campfires (t-z4wereld's KAMPVUREN, by reflection) keep safe spots free.

### World boss: De Grauwe Zeeslurf (`bazen/Zeeslurf`, lv 54, arena `z4bazen:zeeslurf` = the lagoon raft, r 26, window -22/+14)
- **The engine:** Z3Baas via `Z4Baas` (titles `vadsrijk.z4bazen.*`, chests from `Z4BazenLoot`, enrage 8 min, cheer-up instead of falling apart).
- **Movement:** it swims by itself with no gravity, always between the lagoon floor and the water surface; both are found by
  scanning, so it also works in a test pool.
- **Fase 1:** Slurfspuit (red strip, 18 % + push), Mokwolk (2-3 red circles, 12 % + 3 s Darkness).
- **Fase 2 (66 %):** it sinks into its grotto (POI `zeeslurf_grotje`, offset from the arena centre) and is IMMUNE until the 3 grey polyps
  (`vr_z4_baas_poliep`, a `Z3Doel`, 5 % of its HP each) on the floor are broken.
  - Pink bubble rings next to them refill air + 3 % Vads.
  - A purple undertow every 6 s.
  - After the last polyp: 5 s gasping + 25 % damage.
- **Fase 3 (33 %):** Golfslag (a red ring of 9: being under the surface is safe) and Samen aaien (a yellow stack: held = 4 s giggle stun +25 %).
- **Defeat:** `_blij` texture, notes, stays 10 s. Moments `baas:zeeslurf` + `zeeslurf_opgevrolijkt` + `NpcApi.meld(p, "z4_zeeslurf", "opgevrolijkt")`.

### Dungeon: Het Wrak van de Vliegervanger (`bazen/Vliegervanger`, lv 56, arena `z4bazen:vliegervanger` = the fore deck, r 12)
- **Trash:** the Vliegerknechten (+1 Bromvlieger) in het ruim / de kombuis / bij de knuffelkooi (below deck) and on the deck
  (`z4_mobs_wrak_*`, POI aliases `wrak_ruim|wrak_kombuis|wrak_knuffelkooi|wrak_ingang`).
- **The boss is Kootje de Vliegervanger.** I first called him Gorgonzola, but t-z4verhaal already calls him **Kootje**, so I took
  their name.
  - **Fase 1:** Knuffelnet (red strip, 15 % + Plakkerig), Kaasbom (2 red circles); at 80 % he whistles 2 Vliegerknechten.
  - **Fase 2 (60 %):** he hangs under his war kite (the model's `vlieger` bone is shown) 9 blocks above the deck and is IMMUNE.
    - 3 winches (`vr_z4_baas_lier`, right-click 3x each, the rope coils show per click) bring the kite down: KRAK, 6 s stunned, +50 %.
    - While he hangs: plushie sacks (red circles) and gusts (a purple strip, push).
  - **Fase 3 (25 %):** Knuffelregen = yellow stack circles; caught plushies are counted (max 3).
  - **Defeat:** he flees in smoke. Moments `baas:vliegervanger`, `wrak_knuffels:<n>` + `NpcApi.meld(p, "z4_vliegervanger", "verslagen")`.
- **The chest** = kern's personal boss chest at the arena (first kill: a named Legendary + trophy).

### Loot (`wezens/loot/Z4Loot`, `Z4Sets`, `bazen/Z4BazenLoot`, `bazen/Z4BazenEffecten`)
- **Schelpenset** (4 pieces: Kracht/Behendigheid/Verdediging):
  - 2 = +12 Verdediging;
  - 4 = "Schade tegen bazen +8%".
- **Lampionset** (4 pieces: Wijsheid):
  - 2 = +8 Wijsheid;
  - 4 = +10 Heal-kracht + you carry light (via Lantaarnlicht.DRAGERS).
- **Jewellery:** Parelring, Schelpenketting, Lampionoorbel.
- **Weapons** (2 per class, ~7 % above zone 3):
  - Ankerknots, Golfbreker;
  - Vliegerboog, Harpoenkruisboog;
  - Lampionstaf (1.6 s), Koraalstaf.
- **Crystals** (zone-2 gem system):
  - Parelkristal (Vads +60) and Zeeglas (Elementweerstand +5);
  - dropped by every lv-50+ foe inside the zone-4 box: normal 8 % ruw, elite 35 % geslepen, boss always stralend.
- **Named boss items** (all with a real effect):
  - Slurfstaf, Zeekoeienhuid, Lagunelampion (Guhddelijk 2 %);
  - Oorlogsvliegerboog, Kootjes Enterhaak, Kaasdoek;
  - plus 2 trophies.
- **Pets** (`Huisdieren.registreer`):
  - `zeeslurfje`: the boss model at x0.16 with the pink texture + its own hop/sprong anims;
  - `vliegertje`: its own small kite model.
- **t-items' new slots:** 5 pieces in `Z4Loot.NIEUW`:
  - Schelpenwanten (set), Lampioncape (set), Koraalwantjes, Vliegercape, Zeeglasarmband;
  - icons/lang are generated.
  - **The slots exist only on t-items' branch**, so Java looks them up BY NAME (`Z4Loot.slot("HANDSCHOENEN")`). Before the merge,
    no items or base types are made. After it, they are registered, in the tables, and the set glove/cape counts toward the
    set (`Z4Sets.stukken`, max 4).

### Models (`vadsrijk_t_z4wezens.py`)
- **Creatures:** zone-1 style (`vw.Model` + `vw.schilder`): `<naam>.png` grey/grumpy, `_blij.png` bright/happy, `_gloed`, `_elite`.
- **Bosses and objects:**
  - Zeeslurf: a sea cow with a trunk, guh ears, and a jellyfish veil with lantern bulbs (glowmasks for grey and pink);
  - Kootje: the z2 Mika plus a captain's hat, goggles, cape and the `vlieger` bone;
  - the polyp, the winch (bones `touw_1..3`, `vlag`), and the pet Vliegertje.
- **t-models' kit v2 is opt-in AUTOMATICALLY:** `with _v2()` / `_naloop()` use `vw.stijl(2)` and `vw._kit().naloop` when this
  tree's vadsrijk_wezens has them. So after the t-models merge, re-running my generator gives rounded v2 models.
- **Preview:** I checked a render sheet with tools/wiki_renders.py (it ignores cube rotations and crops tall textures, so the kite
  diamonds and urchin spikes only show in-game). Looks unchecked in a client.

### VFX (`wezens/Z4Vfx`)
- All telegraphs go through `Z4Vfx` → kern `Telegraaf`. t-vfx turns every Telegraaf into a ground decal on its branch, so after
  the merge they are decals automatically.
- **One-liners by reflection** on t-vfx's `vfx.BaasVfx` (`windup`, `cast`, `treffer`) are already in the bosses:
  - Zeeslurf: spuit/golf/aaien;
  - Kootje: net/windvlaag/knuffelregen.
  - Lang `vadsrijk.vfx.aanval.{golfslag,samen_aaien,knuffelregen,windvlaag}`.
- They do nothing before the merge.

## Decisions I made myself
- **Ontvadst + pink plof like zone 1** (the brief), so the base is RijkWezen, with own Z4 helpers.
- **Islands with creatures** = t-z4wereld's MOBZONES.
  - I moved the Vuurtoreneiland to the safe list (the old keeper lives there; not a mob zone in their plan).
  - The Bloemeneiland gets only 2 gentle spots on its outer fields.
- **Levels** per t-z4wereld's mob zones: Koraalbaai 53-54, Vliegereiland 52-53, Bloemeneiland 51-52, Mysterie 55-56, rocks 50-52,
  Wrak trash 54-55, and the bosses 54 (Zeeslurf) and 56 (Kootje).
- **The Zeeslurf fight centre is the raft** (POI `arena_zeeslurf`, y 64); the boss stays in the water.
- **Winches:** right-click (not damage), so melee and ranged both have a job; no time pressure (immune until done; enrage at 8 min).
- **Named items and crystals:** my own names. **Boss name:** Kootje (from t-z4verhaal).
- **No area leaders or contract elites in zone 4** (rest zone).

## Shared-file changes
- `VadsrijkFeature.register`: + Z4WezensRegistry + Z4BazenRegistry (2 lines, `// T-Z4WEZENS`).
- `client/VadsrijkClient.init`: + Z4WezensClient + Z4BazenClient (2 lines, `// T-Z4WEZENS`).
- `tools/features/__init__.py`: + `'vadsrijk_t_z4wezens'` after `vadsrijk_z3_wezens`.
  - **Integration:** keep it BEFORE `vadsrijk_t_vfx` (t-vfx wants to be last).
  - **t-vfx's generator** only merges `vfx_*` anims into `vr_baas_*`, `vr_z2_baas_*` and `vr_z3_baas_*` files. Add `vr_z4_baas_*` to its
    glob if the zone-4 bosses should get the generic vfx poses (GeckoLib skips missing anims, so nothing breaks without it).
- Lang nl_nl/en_us: add-only (~244 keys, from the generator).
- **No changes in kern/zone 1-3 code.** The hooks are the existing lists: Schade.VOOR_KLAP/NA_KLAP, Haken.VERSLAGEN,
  Spawners.VERBODEN/VEILIGE_PLEKKEN, Lantaarnlicht.DRAGERS, Spelers.EXTRA_STATS, Huisdieren.registreer, Arenas.registreer.

## POI requests (for t-z4wereld; everything is optional, the defaults already match POI_Z4 v2)
- **Used now:** `arena_zeeslurf(_deur|_bord)`, `zeeslurf_grotje`, `arena_vliegervanger(_deur|_bord)`, `wrak_ruim`, `wrak_kombuis`,
  `wrak_knuffelkooi`, `wrak_ingang`, and the KAMPVUREN list.
- **Optional extras:**
  - `wrak_lier_1..3` (winch spots on the fore deck; otherwise offsets (-5,0,-3), (5,0,-3), (0,0,4) from the arena centre);
  - `z4_mobs_<eiland>_<n>` to move a creature group.
- **The lagoon must be open water** from the raft down to the grotto (y 47), wide enough for a 4-block sea cow.

## Tests
- `VadsrijkZ4WezensGameTests` (`-Pgt=VadsrijkZ4Wezens`):
  - all 10 kinds: lv 52, Vads, tables; cheered up and still hopping vs the Mika gone;
  - loot: items/base types/crystals, elites drop;
  - the light spawner plan (<= 50 alive, none in safe spots);
  - the Mopperkrab's claws (x0.5 from the front) and the Zeurslak's shell.
- `VadsrijkZ4BazenGameTests` (`-Pgt=VadsrijkZ4Bazen`):
  - Zeeslurf in a test pool: fights, grotto + immune, 3 polyps, +25 %, cheered up and stays;
  - Kootje: fights, kite + immune, 3 winches x3, +50 %, fase 3, defeat;
  - arenas/pets/named items/first chest.
- Results: see the bottom.

## Resume point / open
- Looks unchecked in a client (the user judges).
- The Wrak's room layout is t-z4wereld's; if their rooms are elsewhere, move `z4_mobs_wrak_*` (or add the POI aliases).
- The Schildpadeiland turtle (moving) is not mine.
- No AutoCheck run.

## Test results (2026-11-06)
- `runGameTestServer -Pgt=VadsrijkZ4`: **"All 8 required tests passed"** (4 wezens + 3 bazen + the krab/slak test; the first run caught 2
  compile errors, now fixed: a trophy list type and a name clash with Z2Baas.inStrook → `Zeeslurf.inBaan`).
- No AutoCheck client run (3 gradle slots were busy all night; looks unverified).
