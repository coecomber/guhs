# NOTITIES_T_GELUID: area music, boss themes, stingers, ambience (roadmap §5.2 + §5.3, decision R3)

## What is built
- **The pipeline** (`tools/vadsrijk/geluid/`), all Python:
  - `componist.py`: a seeded rule-based composer. It builds 8-bar periods from a 1-bar motif (A A' A half-cadence | A A'' new closed cadence) and snaps strong beats to chord tones. It also does voice-led pads, arpeggios, bass lines, ostinati, percussion, and the "Vadsrijk" leitmotif (4 bars). Its output is a MIDI file through mido.
  - `vormen.py`: the forms.
    - Area theme: intro, A, A2 (2nd instrument), B, A3, outro; ~60-120 s; it ends on the tonic.
    - Boss theme: intro, A, B, A2, bridge; a seamless loop of ~36-72 s. The tail is folded onto the head and the master filter is circular.
  - `stukken.py`: every piece as data (20 area themes, 19 boss themes) plus 5 hand-written stingers.
  - `sf2.py`: my own pure-numpy SoundFont-2 player. It handles zones, loops, the volume envelope, the filter + mod-envelope sweep, vibrato, and cubic interpolation.
  - `render.py`: MIDI → audio, a synthetic hall reverb, the master (RMS level + soft limiter).
  - `maak_muziek.py`: compose → `midi/<groep>/<naam>.mid` (committed) → OGG mono 32 kHz.
  - `maak_sfeer.py`: the ambience. Numpy synthesis (birds, crickets, drips, bees, waves, leaves, creaks) plus SoundFont effects (stream, seashore, wind chimes, celesta shimmers). It makes 13 seamless 22 s beds and 19 one-shots.
- **The SoundFont:** GeneralUser GS (free, also commercially; the rendered audio is ours). It is NOT in the repo (32 MB). It lives at `guhs_vadsrijk/_geluidkit/GeneralUser-GS.sf2`, or wherever `GUHS_SF2` points.
  - fluidsynth was not allowed: running a downloaded exe was blocked. That is why I wrote my own player.
  - Any other GM .sf2 works (it sounds different).
  - A foreign MIDI can be rendered too: `--van-midi`. Track name `naam|instr|gain|galm` = the mix sheet.
- **Sizes:** music ~20 MB (area 12 MB, bosses 7.6 MB, stingers 0.3 MB), ambience 2.2 MB.
  - To shrink: raise `kwaliteit` in `maak_muziek.py` (0.68 now; 0.8 ≈ -25%).
- **Java** `feature/vadsrijk/geluid/`:
  - `GeluidPlan` (common, gametested): area id → streek (themes + bed); boss entity type → theme; the sound ids.
    - Zone 4/5: an id prefix `z4_`/`z5_` or the W-3 boxes. Unknown `z2_`/`z3_` ids fall back to the zone's ring streek.
  - `GeluidRegistry`: the sound events, the payload `Speel`, the server stingers, and the dev commands `/guhs vadsrijk geluid ...`.
    - Server stingers: `NpcApi.QUEST_KLAAR`, `Haken.LEVEL_OP` (once per tick), `Haken.VERSLAGEN` with a `VadsrijkBaas` (to all helpers), and `Haken.GEBIED_BETREDEN` (first visit, registered at index 0 so it runs before the Verkenningsboek marks the area).
  - `client/GeluidConfig`: `guhs-vadsrijk-geluid.toml`, sliders 0-100: gebiedsmuziek 70, bazenmuziek 85, stingers 80, omgeving 70, pauze_min 60, pauze_max 180.
  - `client/GeluidSpeler`:
    - Area music: 5-15 s after arriving; afterwards 1-3 min of silence; new streek = a 3 s grace + 6 s fade.
    - Boss themes: by entity type, or the boss's own `muziek()` id if it is not kern's generic one.
    - Stingers: duck the music, queue at most 2.
    - Vanilla music is off in the Vadsrijk while gebiedsmuziek > 0.
    - Silent during scenes/cutscenes/boss bars.
  - `client/GebiedSfeerSpeler`: two bed slots (main + night crickets on water beds) and now-and-then one-shots every 5-12 s.
    - Underground: below the surface −6 AND sky light < 4; not in zone 3, whose layers already say where you are.
  - `client/GeluidClient`: config, receivers, and the "geheim" stinger. For that it wraps kern's `titelOntvanger`: every `...schat.titel` card counts.
  - `client/Lus`: a tickable sound with live volume, fades and ducking.
- **Generator** `tools/features/vadsrijk_t_geluid.py` (in FEATURES, last entry): writes the sounds.json events and the config lang (en_us real English). It checks that all 76 OGGs exist.

## Decisions I made myself
- **One theme per wilderness area.**
  - The zone rings reuse a neighbour: ring → klodderweide, z2_ring → galmhellingen, z3_ring → wortelvelden.
  - The towns keep the I-AUDIO town tunes; no new town music.
  - Schorsstad (not a town in Gebieden) gets its own playful theme.
- **Zone 4 = "Lampionkust" with a day + evening theme; zone 5 = Hoogvads with a grand theme + a market waltz.** The leitmotif is in both, as the roadmap's "leidmotief" idea. One theme per island/district can come later.
- **Boss themes are one loop each**, no phase-2 layer yet. Bosses without a mapping get `algemeen`. Spiegelbeeld → spiegelzucht, kk_klodder → koning_klodder.
- **Stinger set:** ontdekking (first visit of an area, not the rings/proeftuin), quest_klaar, level_op, baas_verslagen, geheim (schat titles).
  - There is no "uitkijkpunt" stinger: no clean hook.
- **Sound categories:** all music is `SoundSource.MUSIC` (Minecraft's Music slider on top of ours); the ambience is `AMBIENT`.
- **No wind:** Z2Sfeer's Balto-wind one-shots now become a soft amethyst chime. The new beds have only a high, soft leaf rustle.

## Shared-file changes (all marked `T-GELUID`)
- `VadsrijkFeature.register` + `payloads`: one line each.
- `client/VadsrijkClient.init`: one line, last (after kern: it wraps the title receiver).
- `kern/client/RijkHud`: the boss bar calls `GeluidSpeler.zet(entityId, muziek)`. The old `BaasMuziek.zet` is now only the fallback (zet returns true, so it is never reached).
- `voltooiing/Voltooiing`: a new `gemarkeerd(p, gebied)` (add-only).
- `zone2/wereld/client/Z2Sfeer`: wind → chime (5 lines).
- `tools/features/__init__.py`: + `vadsrijk_t_geluid`.
- `sounds.json`, `en_us.json`, `nl_nl.json`: generated entries (`vr.geluid.*`, `guhs.configuration.{gebiedsmuziek,...}`).

## Tests
- Gametest `-Pgt=Geluid` (`GeluidGameTests`):
  - `streken`: the area → theme mapping, no Gebieden id without a streek, towns, zone-4/5 boxes;
  - `bazen`: the entity id → theme mapping, and that the entity types exist;
  - `geluidenEnBestanden`: every event registered + every OGG on the classpath;
  - `stingers`: first visit once, level-up once per tick.
- Listening is for the user (SPIEK_T_GELUID.md). I could not listen myself. I checked with numbers instead: pitch of the SF2 player (FFT within 1%), levels, peaks, loop seams (sample jump < 0.012 after the circular-filter fix), no NaN.

## Open points / not done
- Taste: the user should listen to everything; retuning = edit `stukken.py`, then `python tools/vadsrijk/geluid/maak_muziek.py <naam>`.
- No phase-2 variation per boss, and no separate victory fanfare per boss (one shared `baas_verslagen`).
- No §5.3 point sources from the POI lists (waterfalls, hives, smithy), and no cave-echo on footsteps.
- Zone 4/5 area ids are guessed by prefix (t-z4wereld / Hoogvads had not published ids yet). If they use other ids, add them to `GeluidPlan.STREKEN`.
- Zone-4 bosses are mapped by "zeeslurf"/"vliegervanger" in the entity id. Alternatively a boss can return `guhs:vr.geluid.baas.zeeslurf` from `muziek()`.
- Pack size +22 MB.

## Resume point
- Head = the last commit on 110-t-geluid.
- Re-render everything: `python tools/vadsrijk/geluid/maak_muziek.py && python tools/vadsrijk/geluid/maak_sfeer.py && PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_geluid.py`.
