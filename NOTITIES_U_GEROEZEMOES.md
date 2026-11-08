# NOTITIES_U_GEROEZEMOES: guh crowd murmur in the busy places

Branch 110-u-geroezemoes from 110-v-int (3349c1d52: geluid package, u-audio2 world sounds, u-z6muziek bazaar bed).

## What is built
- **Bed generator** `tools/vadsrijk/geluid/maak_geroezemoes.py` (numpy/scipy, same style as maak_sfeer):
  - Sources: ONLY existing takes: guh_ambient1..16 split into syllable grains (njeg 5/9 and lach 6/12 kept out of the bed grains),
    plus piep_ambient 1/2/3/5 and waterdiertjes/blub1-3 for the accents.
  - Each "voice" = an own pitch (0.82-1.38, by resampling: the guh way), an own distance (gain + low-pass 3.8 kHz near ->
    0.9 kHz far), phrases of 2-6 syllables, then a pause (turn taking).
  - Plus a faint room tone of feet, a circular room reverb, and a 6.5 kHz roll-off.
  - Three beds, 30 s each, exact circular mixes (events and reverb wrap around), so the loop has no seam (measured jump ≤ 0.002):
    - rustig: 6 voices, far, 1 far giggle;
    - druk: 13 voices, 1 njeg, 1 piep, 2 giggles;
    - markt: 22 voices, 2 njeg, 1 piep, 1 blub, 3 giggles.
  - Encoding: mono 32 kHz Vorbis, the first quality level ≤ 50 kbps (the u-krimp rule) → 43/47/47 kbps.
- **Logic** `geluid/Geroezemoes.java` (common, pure, gametested):
  - The places (`PLEKKEN`): hoogvads 1.0 (every TOWN area of zone 5), saffraanpoort .9, kandijkom .8, zwamzoet .72,
    lampionhaven .66, honingtak .6, rozenkloof .54, knuffelkamp .48, haven .42.
  - Market spots: z5_marktlaan, z6_saffraanpoort, z2_kandijkom (×1.15 + 0.05).
  - Intensity = place × (0.3 + 0.7 × min(1, guhs/14)) (×0.4 at night). Outside the places: only with a group of 3+ guhs, max 0.35
    (this absorbs u-audio2's old "markt" murmur).
  - `bedMix`: rustig alone up to 0.35 (fading in from 0), then rustig→druk, then druk→markt; the total grows 0.35→1 with the intensity.
  - One-shot pause 25-40 s (quiet) → 8-14 s (full), never under 6 s.
  - One-shot choice: njeg 28 / guh 26 / lach 18 / oeh 12 / piep 9 / blub 7 % (blub 18 % in Lampionhaven).
- **Player** `geluid/client/GeroezemoesSpeler.java`:
  - Every second it measures the area (Gebieden.op x/y/z), the guhs within 22 blocks and day/night. Guhs = `vr_npc*`,
    `vr_z3_baas_figurant` (Hoogvads' townsfolk props), `vr_ruiter_guh`.
  - The intensity glides (~2 s).
  - 3 `Lus` loops (AMBIENT, everywhere at once, BASIS 0.45 × slider × mix; slider 60 → max 0.27). A loop is released after 5 s of
    silence. Boss fight → ×0.25.
  - One-shots sound at a random guh 3-18 blocks away (pitch = `StemPlan.toon(name)`, the NPC's own voice), else 6-12 blocks
    around you.
  - Budget: a one-shot waits for `WereldGeluiden.losVrij()` and then claims the global pause (`losGebruikt()`).
- **Slider** `geroezemoes` (default 60) appended to guhs-vadsrijk-geluid.toml.
- **Dev command** `/guhs vadsrijk geluid geroezemoes <info|auto|rustig|druk|markt|njeg|lach|guh|oeh|piep|blub>` +
  `... geroezemoes kracht <0-100>`. Forcing works anywhere (outside the Vadsrijk too). It is also in the clickable list.
- **Generator** `tools/features/vadsrijk_u_geroezemoes.py` (in FEATURES after vadsrijk_u_z6muziek):
  - sounds.json `vr.geroezemoes.bed.*` (static, not streamed, so the loops are gapless) + `vr.geroezemoes.<soort>`
    (existing takes, attenuation 16);
  - slider lang nl/en.

## Size
**+0.49 MB** (3 OGGs: 156 + 172 + 170 kB). The one-shots reuse existing files (0 MB).

## Decisions I made myself
- **Replace, not stack:** the old `markt` kind of WereldGeluiden is no longer found (`markt = null`). The one-shots keep its budget
  (shared global pause). `wereld markt` still works as a preview of the old event.
- **Saffraanpoort:** u-z6muziek's synthesised bazaar bed keeps playing (town bed of StadsMuziek). The guh murmur goes on top: the
  synthesised one is a far babble, ours is guhs. If it is too much together, lower `saffraanpoort` in PLEKKEN or drop the
  bazaar bed.
- **Extra place:** De Haven (zone 1) at 0.42, as "another busy place". The user's order is kept for the listed places.
- A town is never fully silent (0.3 × place even with 0 guhs loaded): townsfolk and NPCs are not always loaded or near.
- **Night:** ×0.4 (not off): a few late guhs.
- **Source:** AMBIENT (follows Omgeving) for bed and one-shots. The NPC talk voices of u-audio2 stay NEUTRAL.
- **Levels:** chosen on the soft side (wind lesson). Max bed volume 0.27 at the default slider, below the town bed (0.4) and far
  below the town music.

## Shared-file changes (all marked `U-GEROEZEMOES`)
- `geluid/client/WereldGeluiden.java`: markt detection off (1 line) + `losVrij()` / `losGebruikt()` (add-only).
- `geluid/client/GeluidClient.java`: init line + `geroezemoes` case.
- `geluid/client/GeluidConfig.java`: slider GEROEZEMOES + `geroezemoes()`.
- `geluid/GeluidRegistry.java`: events registered; dev command + list line; no "Geluid:" echo for `info`.
- `tools/features/__init__.py`: + `vadsrijk_u_geroezemoes`.
- `sounds.json`, `en_us.json`, `nl_nl.json`: generated entries.
- New: `geluid/Geroezemoes.java`, `geluid/GeroezemoesGameTests.java`, `geluid/client/GeroezemoesSpeler.java`,
  `tools/vadsrijk/geluid/maak_geroezemoes.py`, `sounds/vadsrijk/geroezemoes/*.ogg`.

## Tests
- `-Pgt=Geroezemoes` (GeroezemoesGameTests), 2 tests:
  - `intensiteit`: places + the user's order, guh count, night, market, wilderness group, bedMix crossfade/limits, pauses,
    one-shot choice;
  - `geluiden`: events registered + OGGs present.
- Listening is for the user (SPIEK_U_GEROEZEMOES.md). I could not listen. Checked by numbers: seams, peaks ≤ 0.23, RMS −31/−30/−29 dB.

## Open points
- Taste: the beds need a listen; the volume (`BASIS`), the one-shot frequency and the place weights are guesses on the soft side.
- The u-audio2 slider tooltip "wereldgeluiden" still mentions "marktgeroezemoes" (that text is u-audio2's generator). It is
  harmless; it can be removed at integration.
- u-leven-stad (if it adds walking townsfolk with another entity type) should be added to `GeroezemoesSpeler.guh`.

## Resume point
- Re-render: `python tools/vadsrijk/geluid/maak_geroezemoes.py` (or with names: `rustig druk markt`).
- Then the generator: `PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_geroezemoes.py`.
