# NOTITIES U-Z6RAID (round U, 2026-11-06): De Sterrendovende Toren

The first raid. Branch `110-u-z6raid` from eaee06d08, with **110-u-dubbel** and **110-u-hekken** merged in.
- Package: `feature/vadsrijk/zone6/raid/` (+ `client/`).
- Generators: `tools/features/vadsrijk_u_z6raid.py` (in FEATURES) and `tools/vadsrijk/zone6/raid/binnentoren.py`.
- Test steps: `SPIEK_U_Z6RAID.md`.

## What was built
- **The raid system (`Z6Raid`)**:
  - a group of 2-10 players;
  - a lock + a queue + a waiting room (De Wachtzaal);
  - 4 floors, each with a checkpoint (its landing);
  - wipe detection: no living group member in the arena of a started fight for 3 s resets the floor, and the wipe counter goes +1;
  - the clear: a chest for every member in the tower (EVERY clear), the stars, titles, and the hook.
  - Not saved: a restart frees the tower, like parties.
- **Scaling (`Z6Schaal`)**, with n = the group size at the START:
  - HP x (0.55 + 0.45 n);
  - mechanic counts ceil(n/2), at least 2;
  - add cap 1 + n/3.
- **The bosses** (`Z6RaidBaas` extends Z3Baas, lv 70). Each has stack/spread/interrupt/adds/puzzle/enrage. See the class javadocs and SPIEK.
  1. **Zandwachter**: zandhuid x0.5; the hourglass, where two plates must be held at the same time, turns him (vulnerable, back x1.8); pillars (m) or a sand wall; lanes, stack, spread, interrupt.
  2. **Spiegelvizier**: he hides among m+1 images; only the real one sparkles. Turning mirrors (right-click = aim at the next image); 2 beams = unmask; a wrong image = a clone + 10 %. Swaps two players every 30 s; the hall breaks at 20 %. One class with three roles (ECHT/BEELD/KLOON).
  3. **Gedoofde Sterrenbeelden**:
     - Grote Knuffel (the arena boss, tank buster + donut), Vliegende Bultguh (lane charges), Kleine Pluis (heals, interruptible Sterrenlied).
     - At 0 a constellation is "uit" (vadsVloer) and a 10 s window opens: all three out in it = cheered up together, else they relight at 40 %.
  4. **Ringeling**:
     - Fase 1: kicked rings; a jump in the pink circle catches one (it flies back, 3 %, and is gone); otherwise the ring's element effect hits.
     - Fase 2: the Lege Lens (darkness; carry shards to m beacons; floor at 25 % until all are lit).
     - Fase 3: the ring dance (low = jump, high = sneak; after 3 rings she falls, x2).
     - The end (user decision): she is CHEERED UP and turns against the Keizer.
- **Interrupts**: `Z6RaidBaas.giet` (a purple cast bar + break meter via T-VFX's `Vfx.cast/castBreuk/castStop`; X % of max Vads breaks it).
- **Telegraphs**: all through Telegraaf/Vfx decals (circles, lanes, cones, donut, the pink plates/catch circles).
- **Raid UI (`client/Z6RaidHud`)**:
  - raid frames for up to 10 (class colour, Vads, flauw/weg/buiten, made-up dev members in grey);
  - floor + boss, the fight clock counting to the enrage (orange in the last minute, red past it), wipes, total time, n;
  - on floor 3 the three bars + the window.
- **Loot (`Z6RaidLoot`)**:
  - the Sterrenzijde set (4 pieces, Legendarisch, lv 70; set bonus 2/4 with clear stat names);
  - Ringbreker / Komeetboog / Sterrenstaf (class weapon), Ringelings Teenring (Guhddelijk 1.5 %), Ringreplica (guild trophy, 12 %);
  - the Sterrenring wings (t-kleding cosmetic `vleugel:sterrenring`, trick LUCHTDASH, 3 %), the pet Sterrenpluisje (4 %), the Sterrennacht-halster (2 %).
  - The first clear guarantees a weapon + the replica.
  - Bosses 1-3 give a small chest (coins + 30 % a set piece).
  - Titles (Titels): `z6raid_ringbreker` (a clear), `z6raid_sterrenaansteker` (a clear without fainting).
- **Music ids (`Z6RaidMuziek`) for u-audio2**:
  - `guhs:vr.z6raid.muziek.<baas>` (phase 1) and `<baas>_2` (Ringeling also `_3`);
  - the bosses: zandwachter, spiegelvizier, sterrenbeelden (`_2` while the window runs), ringeling;
  - plus `opgevrolijkt` and `wachtzaal` (not wired yet).
  - The boss bar asks `muziek()` per phase.
- **Placement (`Z6RaidPlekken`)**: POI ids by reflection on `zone6.wereld.Z6WereldPlekken.plek(String)`:
  - `raid_toren_poort`: the door box that teleports into the Wachtzaal; placeholder 4123,71,-688;
  - `raid_toren_buiten`: where you come out; placeholder 4118,71,-688;
  - `raid_toren_binnen`: the anchor (optional); placeholder 4140,16,-688.
- **The inside (`binnentoren.py` -> `data/guhs/structure/vadsrijk/zone6/raid_binnentoren.nbt`, placed by `Z6RaidBouw`)**:
  - 5 octagonal halls stacked 12 apart: the Wachtzaal, then floors 1-4, the roof under a star dome with the Lege Lens.
  - Depth: a pillar ring, 2-deep alcoves with grille windows/mirror panels, lanterns on chains, coffered ceilings, benches, a theme per floor.
  - A switchback stair annex with a centre wall.
  - The u-hekken connection pass `verbind.verbind_vol` runs over the volume (758 states fixed). The built-in loopcheck gives 0 problems.
  - Placed once, when a player comes within 160 blocks (saved version `Z6RaidData.bouwVersie`; bump `Z6RaidBouw.VERSIE` after a generator change), or with `/guhs vadsrijk z6raid bouw`.

## Decisions I made myself
- **Lock per group instead of shared**: needed for a raid. The scaling at the start, the mechanic counts per group and the floor reset on a wipe do not work with two groups in one arena. One group at a time, a queue, the rest wait in the shared Wachtzaal.
- **The inside is a pocket UNDER the tower, entered through the door.** u-z6wereld's tower shaft (r 8-16) is too small for 10 players + spread mechanics. The pocket is stacked like the real tower, so you still climb real stairs. It is self-contained (own stone shell, y 2-68).
  - **u-z6wereld: please keep caves/water out of x 4115..4174, z -713..-663, y 2..68.**
- **Damage per hit does not scale x f(n)**: a 10-player hit would be 5x as hard. Hits only grow x(0.9 + 0.02 n); the group's damage grows through more targets, adds and mechanics. HP follows the plan exactly.
- **Death rules**:
  - fainting wakes you on the current floor's landing (kern's arena door is the same spot) and you may rejoin;
  - Eénvads does NOT retire in the raid (`KarakterRegistry.ZACHT_ALS`);
  - fainting only costs the "Sterrenaansteker" title for that run.
- **Spiegelvizier = Meester Glans**, mirror polisher of the Spiegelzaal, Mika illusionist and Kootje of Ringeling (the plan's recommendation). **u-z6verhaal**: please keep this or tell me; it is only a name + 4 lines of text here.
- **Ring catching = jumping** in the pink circle in the last 0.5 s (server-checked `!onGround`). Ringdans: low ring = in the air, high ring = sneaking.
- Mirrors turn per click to the NEXT image (no free angles), so 2 beams can really meet on one image.
- The mount (Sterrennacht-bultguh) is a **halter item**. Right-click asks `Rijdieren.geef(p, "bultguh:sterrennacht")` (then `"bultguh_sterrennacht"`). It stays in the bag until the z6-rijdieren slice adds that mount. **z6-rijdieren: accept one of these ids.**
- The pet Sterrenpluisje uses the Kleine Pluis model (own `Huisdier`, never unlocked by a kill).
- Dev "start with N fake players" = you + made-up members (they count for the scaling and the frames only).
- Z3Baas leaves T-VFX's "vfx" GeckoLib controller out (zone 3/4 bosses never play vfx_*). My base adds it; the vfx_* anims are merged into my animation files by my generator (`vadsrijk_t_vfx.generiek`).

## Shared-file changes (add-only, marked U-Z6RAID)
- `VadsrijkFeature`: register + payloads (2 lines).
- `client/VadsrijkClient`: `Z6RaidClient.init` (1 line).
- `tools/features/__init__.py`: `'vadsrijk_u_z6raid'` at the end of FEATURES.
- lang nl_nl/en_us: +186 keys (`vadsrijk.z6raid.*`, entity/item names, titles, wing, pet, vfx attack names, and z2/z3 tip keys for my boss ids).
- No change in kern/zone files: everything goes through the existing hooks:
  - `Arenas.registreer`, `Flauwvallen.CHECKPOINT_KIEZER`, `Haken.FLAUW`, `KarakterRegistry.ZACHT_ALS`;
  - `Spelers.EXTRA_STATS`, `Schade.VOOR_KLAP`;
  - `Titels.registreer`, `Cosmetica.registreer`, `Huisdieren.registreer`, `RijkHud.voegToe`.
- Merges: 110-u-dubbel, 110-u-hekken (unchanged).

## Hooks for other slices
- `Z6RaidHaken.OPGEVROLIJKT` (add-only list) and the reflection call `zone6.verhaal.Z6Verhaal.ringelingOpgevrolijkt(ServerPlayer)`, once per group member in the tower at a clear.
- `Z6RaidData.sterrenAan`: true after the first clear ever (for a sky effect over the desert by u-z6wereld/u-z6verhaal).
- u-hud: the raid frames are a separate RijkHud element `z6raid` (right edge) and only show inside a raid group. When u-hud's editor is merged, add `case "z6raid"` to `RijkHud.standaardPlek` to make it movable. u-hud's 4-player party frames are untouched.
- u-audio2: the music ids above.

## Tests
- `runGameTestServer -Pgt=VadsrijkZ6Raid`: schaal, verdiepingEnWipe, lootPerClear, zandloper, spiegels, sterrenbeelden. Results: see below.
- Not checked in a client (the user tests in the dev client): looks of the models/textures, the binnentoren in the world, the HUD layout, decal readability, the walk through the stairs in survival.

## Open points / resume
- The tower door POI + the real anchor come from u-z6wereld (placeholders now). After their POI_Z6 lands, nothing to do (reflection), unless the anchor box collides with their terrain.
- `Z6RaidMuziek.WACHTZAAL` / `OPGEVROLIJKT` are ids only (no ambience loop wired).
- Guild xp for a clear (ZONE6_PLAN §10.1): no guild system on this base. Hook it into `Z6RaidHaken.OPGEVROLIJKT` once u-gilden is merged.
- Ringeling as an NPC in Saffraanpoort after the clear: u-z6verhaal.
