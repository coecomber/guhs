# NOTITIES_U_GROTKERKER: every zone cave a repeatable mini-dungeon (round U, user decision)

Branch `110-u-grotkerker`, from 110-v-int (ab254bad1, contains U-GROTTEN's caves). The user's cheat sheet is `SPIEK_U_GROTKERKER.md`.

## What is built

### The run (`feature/vadsrijk/grotkerker/GrotKerker.java`)
One run per cave in the shared world, state machine per cave (in memory; a restart = every cave asleep):
- **SLAAPT** (asleep): nothing spawned, gate open. The first player in the cave box starts a run.
- **BEZIG**: room 1 = one pack (3 + 1 per extra player, max 6), room 2 = a bigger pack (4 + 1 per extra player, max 7) + the
  mid-elite (the cave's old elite type, spawner id `grot:<id>#elite`, so U-GROTTEN's elite bonus + book entry still work; 1 affix
  with 2 players, 2 with 3+). The pack creatures alternate between two cave-themed kinds of the zone. The GATE is shut.
- **BAAS**: both rooms beaten -> the gate opens, the end boss wakes in the boss room (the old elite den).
- **KLAAR**: boss beaten -> personal end chests (kern's BaasKist + contribution > 5 %), clear counted per player, title card,
  the gate board counts down 10:00, then asleep again.
- Players: the count at the START scales the packs/elite; the boss's HP uses the count in the cave when the boss wakes
  (kern's +75 % per extra player, fixed for that boss: `GrotBaas.vasteSpelers`).
- **Shared world model:** one run per cave, newcomers JOIN the run in progress (chat line), nobody waits, never a second copy.
  Late joiners get a chest only when they help > 5 % against the boss.
- **Reset:** unfinished run: 1 min after the last player left. Cleared: 10 min after the clear, and only when nobody stands behind
  the gate (it waits otherwise). `LEEG_RESET_NA_KLAAR = false`: a CLEARED cave does NOT reset early when everyone leaves (else a
  clear could be farmed by stepping out for a minute). Flip the constant to follow the brief literally.
- **No doubles:** every run creature/boss gets a U-DUBBEL key `grotkerker:<cave>:<slot>` and the run tag
  `guhs_gk_<cave>_<nonce>` (+ `guhs_grotkerker`). An EntityJoinLevelEvent guard refuses any tagged creature whose nonce is not the
  live run's (chunks saved mid-run, restarts, after a reset). Missing slots are only respawned when the room chunk's entities are
  really loaded (`Dubbel.klaar`) and the key is not alive, 5 s after the last spawn.
- **The gate:** barrier blocks in the AIR cells of the tunnel into the boss room (columns computed by the generator, 2 rows deep;
  the generator floods the cave grid with the gate shut and fails if any other room is still reachable). Pink dust + a hologram
  board ("Grotpoort - Versla eerst de grotbewoners: N over" / "Open! Volgende run over m:ss"), an overlay line when you bump
  into it. It only shuts when nobody is behind it (boss room, treasure nook, the tunnel between) or standing in it, and only
  replaces air. After a crash a left-over gate is opened on first use (`herstelPoort`, only barriers in the gate columns).
- U-GROTTEN's free spawners (`grot:<id>#mob`, `grot:<id>#elite`) are gone: the run spawns everything.

### The bosses (`GrotBaas`, `GrotBazen`, `client/GrotKerkerClient`)
- `GrotBaas` = kern `VadsrijkBaas` in loose mode (no registered Arena: the run manager owns spawning/reset): boss bar near it,
  fight = the boss room (`inGevecht`, random telegraph spots within 0.75 x the room radius), leash back to the middle, full reset
  after 30 s with nobody in the room, 2 phases (50 %), at phase 2 two cave creatures come to help (`GrotKerker.hulp`, part of the
  run). Own rotation of VFX-kit (bouwdoos) attacks every ~4-6 s, the phase-2 signature first when ready. HP ~14 cave creatures of
  its level; title card + first-time camera swoop through kern's intro when you step into the boss room.
- Looks: an existing cave creature model scaled up + a soft tint (no new models): z1 Sporenpriester x1.9, z2 Kristalspin x2.8,
  z3 Harsgolem x1.5, z4 Zeurslak x4, z5 Grauwe Pluismijt x7, z6 Zoutkreeft x5. Its own "actie" anims are the model's.
- Music: `muziek()` = `vr.geluid.baas.<theme>` with a borrowed theme as fallback (opperzwam, spiegelzucht, wortelwurger, zeeslurf,
  donderbuik, zoutkolos; "algemeen" if a theme is missing). Phase layer + fanfare come from U-AUDIO2's hooks by themselves.
- Signature mechanics: Glimschuil (safe spots), Kristalzang (interrupt or heal), Samen tillen! (stack), Getij (in-out) +
  Schelpzeur (interrupt), Wolkenschuil (out-in), Kristalschuil (safe spots). Every cave has 3 attacks; telegraphs >= 32 ticks.

### Loot (`KerkerLoot`)
On EVERY clear for every chest earner: 400·zone²+300 brons, one usable gear piece at boss level (first clear of that cave EPISCH,
else ZELDZAAM with 12 % EPISCH), 3-5 ores of the zone's mining tier (30 % vadsig), 25 % the zone's boss core (z1-5).
Rare, handed out at once: the cave PET (mini boss, 6 %, sure after 25 misses; its "boss" key `guhs:grotpet_<cave>` is never
killed, so the first-kill rule never gives it) and the cave MOUNT = a new Pluimguh colour via `vint.RijdierBuit` (10 % per player
per clear, sure after 15; only once you own a Pluimguh, like the Kristal colour).

### Book
U-GROTTEN's zone 1-5 "Grotten" category gets a 5th line per cave: "Grotbaas van ... verslagen" (`GrotKerker.boekDing`). Zone 6
gets its own "Grotten" category (only when the zone-6 cave is in the build).

### Zone 6 (De Zingende Zoutgrot)
Built by u-z6wereld's own generator (tools/vadsrijk/zone6/bouw6.py `zoutgrot()`), not by grotplan. KerkerData has a fixed entry
(hall west/east rooms, boss room = the elite chamber, no gate: the hall and chamber have no tunnel in the plan we can seal). The
game switches it on only when `zone6.wereld.Z6WereldPlekken` is on the class path (reflection, `Kerkers.zone6`), and then moves
the entry onto its POIs `zoutgrot` (hall) and `zoutgrot_elite`. While active, u-z6wezens' cave elite spawner
`z6wezens:grot_zoutslang` is blocked (`Spawners.VERBODEN_VOOR`); the Zoutslang becomes the run's mid-elite. **After merging
110-u-z6wereld nothing else is needed**; if its cave gets a tunnel into the elite chamber, a gate can be added to the Z6 dict in
the generator (columns `{x, y0, z, y1}`).

### Generator `tools/features/vadsrijk_u_grotkerker.py` (in FEATURES after vadsrijk_u_grotten)
- Builds the caves in memory (grot.py, no tiles) and writes `KerkerData.java` (GENERATED): rooms, boss room, gate columns + board,
  "behind" circles, box, levels. Run standalone: `PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_grotkerker.py`.
- The six cave Pluimguh textures (recoloured from the pink one with t-rijdier's mask) + egg icons/models.
- Lang nl + en (109 keys).

## Decisions I made myself
- **No map regen:** the existing caves already have 2 rooms + an elite den on the main line; the elite den became the boss room,
  the mid-elite moved to room 2. grotcheck/loopcheck results of U-GROTTEN stay valid (no geometry change). Zone 4: room 1/2 are the
  east/west half of the one big Koraalhal. Zone 3: the boss tunnel starts at room 1, the hal is a side room you clear first.
- The "door" is an invisible barrier wall with sparkles + board (survival players could break a visible block; barriers can't be
  broken and are only placed in air).
- One run per cave + join, no queue/instances (simplest robust model).
- A cleared cave always waits the 10 min (see reset).
- Rare reward per cave: BOTH a pet (6 %, pity 25) and a mount colour (the 10 %/15 rule).
- Cave Pluimguh colours are hidden in the colour row until owned (like Goud); the row wraps after 9 swatches.
- Boss looks reuse cave creature models (no new Blockbench work); the user judges the looks.
- Old dev command `grot reset <id>` (player data) renamed to `grot wis <id>`; `grot reset <id>` is the run reset (brief).

## Shared-file changes (add-only / small, marked U-GROTKERKER)
- `VadsrijkFeature.java`: 1 line `GrotKerker.register`. `client/VadsrijkClient.java`: 1 line `GrotKerkerClient.init`.
- `economie/RijdierSoort.java`: 6 PluimKleur values appended (GLIMZWAM, AMETHIST, WORTELHOUT, KORAAL, ONWEER, ZOUTROZE; bron
  "grot"), `verborgen()` also for bron "grot". Appended at the end: saved ordinals of the old colours stay valid.
- `economie/client/RijdierScherm.java`: the colour swatches wrap to a second row (4 lines).
- `rijdier/VadsrijkTRijdierGameTests.java`: "== 9 colours" -> ">= 9".
- `grotten/Grotten.java` (U-GROTTEN's, this slice builds on it): no free spawners any more (`spawners()` left unused), the book
  gets the boss line, `reset` -> `wis`, `elite` spawns through the run. `grotten/VadsrijkGrottenGameTests.java`: spawner checks ->
  "no free spawner + a run", book 4 -> 5 entries.
- `tools/features/__init__.py`: `'vadsrijk_u_grotkerker'` after `vadsrijk_u_grotten`.
- Lang (nl/en): `vadsrijk.grotkerker.*`, `entity.guhs.vr_grot_baas_*`, `vadsrijk.grotten.boek.baas.*`, `vadsrijk.vfx.aanval.grot_*`,
  `vadsrijk.huisdier.<pet>.*`, `vadsrijk.economie.pluimkleur(.bron).<kleur>`, `item.guhs.vr_item_pluimei_<kleur>`.
- New assets: `textures/entity/vr_eco_pluimguh_<kleur>.png`, egg item textures/models/items (6 each).

## Tests
- `VadsrijkGrotKerkerGameTests` (`-Pgt=VadsrijkGrotKerker`): `data`, `runEnReset` (a whole run in a test cave: packs once, no
  doubles on re-tick, old-run creature refused on load, gate shut/open, one boss, clear counted, reset after 10 min leaves nothing,
  the next run starts with the gate shut again), `lootPerClear` (coins + gear + ore every clear, pet pity, mount within 16 tries,
  nothing twice).
- Also run: `VadsrijkGrotten` (changed), `VadsrijkTRijdier`, `VadsrijkRijdieren` (new colours).
- Results (2026-11-08): `compileJava` clean; `runGameTestServer -Pgt=VadsrijkGrotKerker,VadsrijkGrotten,VadsrijkTRijdier,VadsrijkRijdieren`:
  **17/17 pass**. (`GrotKerker.TEST_KLAAR`: in the gametest world the test chunks are not "entity ticking", so the test skips
  U-DUBBEL's klaar check; the real game always uses it.)

## Open points / not done
- Not seen in a client: boss sizes/tints/hitboxes in the rooms, whether telegraph spots ever land in deco, the gate look.
- Zone 6: not testable in this tree (no Z6WereldPlekken); dormant until u-z6wereld is merged. No gate there.
- Unopened end chests stay in the boss room after a reset (they are personal; the next clear stacks a new chest above if the
  spot is taken). Could be cleaned on reset later.
- The pet's pity is not settable by command (only by the gametest); the mount pity is (`/guhs vadsrijk loot pech grot_<cave>_pluim 15`).
- Boss attack anims: the bouwdoos `vfx_*` generic anims are not in the creature animation files (only the model's own
  aanval/spreuk/... play). Could be merged by t-vfx's generator later.
