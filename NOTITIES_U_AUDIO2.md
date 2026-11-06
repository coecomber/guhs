# NOTITIES_U_AUDIO2: boss phase music + own fanfares, world point sounds, NPC voices (V4)

Branch 110-u-audio2 from eaee06d08 (110-t-int). Continues T-GELUID (`feature/vadsrijk/geluid`, `tools/vadsrijk/geluid`).

## What is built
- **Boss music with phases** (pipeline + Java):
  - `vormen.baas(spec, fase=True)`: the SAME piece (same seed, same notes, same loop length, checked sample-exact) plus the
    last-phase layers (`fase_lagen`: double-time drums, timpani pulse, high string tremolo-ostinato, choir pad, melody an octave up
    on brass; or the spec's own `fase` / `fase_dubbel`).
  - Rendered as `muziek/baas/<baas>_fase.ogg` (kwaliteit 0.8).
  - Client (`GeluidSpeler`): when a boss theme starts, its `_fase` loop starts in the same tick at volume ~0 (`Lus.meng`).
    On the last phase both crossfade linearly over 2 s; the base keeps running silent, so they stay in sync.
  - Phase signal: one hook line in `VadsrijkBaas.krijgSchade` (the only phase path; all Z2/Z3/Z4/Z5 bosses inherit it). It calls
    `GeluidRegistry.fase(boss, fase, fases().length+1)`, which sends `Speel("fase", "id:fase:max")` to players within 64 blocks.
    A player who joins late in the last phase starts directly on the fase loop (client remembers per entity id).
- **5 new boss themes**: mika_imker, grauwe_droom (lullaby boss: music-box waltz; its own dark last phase), donderbuik,
  gorgonzola, zilverstaart (Kootje).
  - `GrauweDroom.muziek()` now returns `vr.geluid.baas.grauwe_droom` (it used Vahoegnir's).
  - The imker maps via `vr_z3_imker_mika`; the zone-5 bosses map via `vr_z5_baas_*`.
- **Own victory fanfare per boss** (`vormen.fanfare`): key turned major, the boss's lead instrument, a figure picked by seed,
  V → I with timpani/cymbal/bells, ~5 s; the Grauwe Droom gets a soft music-box version.
  - Server: `Haken.VERSLAGEN` now sends `Speel("fanfare", thema)` instead of the shared `baas_verslagen` stinger (that one
    stays as the fallback for an unknown theme).
  - Client: it goes through the stinger queue (stinger slider) and fades the boss music out in 1 s.
- **World point sounds** (`WereldBron` common + `client/WereldGeluiden`):
  - a client block scan 25x13x25 around the player (700 blocks/tick, rest 5 s between full scans unless you walk);
  - the fixed points from the committed world lists (Z2 gondola stations, Z3 lifts);
  - every 2 s an NPC-cluster check (≥3 `vr_npc*` within 6 blocks = market murmur).
  - **Budget**: the 4 nearest sources within 12 blocks (one per kind per 6 blocks); ≥2-4 s between any two one-shots; long
    per-kind pauses (6-27 s); one fountain loop at most, its volume falls to 0 at 12 blocks; base volumes 0.28-0.35 × slider.
  - **Big waterfalls are NOT done here**: `bio.blokkenwolk.WatervalEffecten` already puts a rush on every fall ≥4 in every
    dimension (incl. the Vadsrijk), so `fontein` is only small 1-3 block falls (fountains/spouts).
  - **Cave echo**: underground (below the surface −6 and sky light < 4), every ~1.7 blocks walked on the ground (not sneaking or
    swimming) the step sound of the block below is replayed 4 and 9 ticks later at 7% / 3.5% volume, pitch ×0.82.
- **NPC voices** (`StemPlan` common + `client/StemSpeler`): sound events made only of EXISTING takes, so no new audio.
  - The events: groet = guh_ambient 3/4/7/10/11/16, vraag = 1/8/5/9, dank = 6/12 + guh.happy, mika = mika_ambient, piep =
    piep_ambient, blub = waterdiertjes/blub.
  - When they play:
    - a client `EntityInteract` on a `vr_npc*` entity (main hand) → groet;
    - one hook line in `RijkPraatScherm.init` (when the page changes): opening → groet (or vraag if the text asks; skipped if the
      click already greeted within 1.5 s); a happy page → dank; a page ending in "?" → vraag (60%).
  - Flavour: blub 4% (15% in zone 4), piep 7%.
  - Narrator and the player's own lines are silent; draak = pitch ×0.6.
  - Pitch per NPC: a hash of its name (0.82-1.40). Rate limit 1.2 s. Source NEUTRAL, positioned at the NPC.
- **Sliders**: `wereldgeluiden` (60) and `stemmen` (70), appended to `guhs-vadsrijk-geluid.toml`. The old ones are unchanged.
- **Dev commands** (`/guhs vadsrijk geluid ...`): `fase <baas>` (theme, last phase after 12 s), `fanfare <baas>`,
  `stem <soort>`, `wereld <soort>`, `wereldinfo`. All are in the clickable list. `stop` now also stops a boss preview.
- **Generator** `tools/features/vadsrijk_u_audio2.py` (last in FEATURES): the sounds.json events, the slider lang (en_us
  real English). It checks the new OGGs and the reused takes exist.

## Size
**+12.25 MB** of OGG: 24 fase loops ≈ 8.5 MB, 5 new themes ≈ 2.2 MB, 24 fanfares ≈ 1.0 MB. Voices and world sounds add 0 MB
(they reuse existing files).
- To shrink: raise `kwaliteit` of the `_fase` entries in `stukken.alle()` (0.8 now) or drop fase loops for minor bosses.

## Decisions I made myself
- "Every major boss" = every boss with a theme (24 incl. algemeen), so all get a fase loop + fanfare.
- "Last phase" = phase `fases().length + 1` (Grauwe Droom 3, Zeeslurf 3, ...). Earlier phases keep the normal loop.
- Crossfade of full mixes (not additive stems): after the 2 s fade only one mix is heard, so a few ms start offset between the
  two streams can never flam.
- The campfire crackle is extra on top of vanilla's (soft, every 6-14 s at most). Wind chimes = bells + windgong blocks.
- I left the gondola/lift creak at fixed stations only (the cabins are a ride scene, not entities).
- I used no POI_*.md lists: they are generated and not committed. The block scan finds the same things at runtime.

## Shared-file changes (all marked `U-AUDIO2`)
- `kern/baas/VadsrijkBaas.java`: one line after `opFase(fase)`.
- `zone5/droom/GrauweDroom.java`: `muziek()` returns grauwe_droom.
- `dialoog/client/RijkPraatScherm.java`: one line in `init()` (StemSpeler.pagina).
- `tools/features/__init__.py`: + `vadsrijk_u_audio2`.
- `sounds.json`, `en_us.json`, `nl_nl.json`: generated entries.
- Within the geluid package (mine/T-GELUID's): GeluidPlan, GeluidRegistry, GeluidClient, GeluidSpeler, GeluidConfig, Lus.

## Tests
- `-Pgt=Geluid` (GeluidGameTests): the old 4, plus:
  - `fasesEnFanfares`: every boss has a theme, a fase loop and a fanfare, registered + OGG; the new boss mapping;
  - `faseHaak`: the Grauwe Droom's own theme; the phase hook + fanfare reach the player;
  - `wereldBronnen`: hive/anvil/bell/campfire/small fall classified, big fall + mid-column ignored; fixed points; voice choice.
- Listening is for the user (SPIEK_U_AUDIO2.md). Checked by numbers: base and fase loops have identical frame counts; peaks ≤ 0.64.

## Open points
- Taste: all 5 new themes + 24 fanfares + the fase layers need a listen. The world-sound volumes are guesses on the soft side.
- The phase hook is per damage; a dev command that forces a phase (`Beleving.faseMoment(..., true)`) does NOT trigger the music
  (it does not change the boss phase either).
- Late-join detection only works when the client saw the phase packet. A player who logs in mid-last-phase hears the normal loop.

## Resume point
Head = the last commit on 110-u-audio2.
- Re-render: `python tools/vadsrijk/geluid/maak_muziek.py _fase fanfare mika_imker grauwe_droom donderbuik gorgonzola zilverstaart`
- Then the generator: `PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_audio2.py`.
