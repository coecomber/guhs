# NOTITIES_I_AUDIO: town music + town ambience (fix round 2)

The user asked for sweet tunes in the towns. This overrides design round 9 ("music only at bosses/scenes"). The wilderness stays ambient only.

## What is there
- **Java** `feature/vadsrijk/stadsmuziek/`:
  - `StadsMuziek` (common): sound events, the area→town mapping `stadVan(gebiedId)` / `stadOp(x, z)`.
    - zwamzoet, knuffelkamp and haven map to themselves; z2_kandijkom and z2_kandijkom_dok map to kandijkom; everything else gives null.
  - `client/StadsMuziekSpeler`:
    - **Tunes** (`SoundSource.MUSIC`, so the Music slider applies): the town's two tunes alternate, with 10-25 s of quiet between them.
      - Entering a town: the first tune starts after 2 s and fades in over 4 s.
      - Leaving: a 3 s grace, then a 3 s fade-out.
      - Vanilla music is paused in towns: `SelectMusicEvent` (LOWEST priority) gets `overrideMusic(null)`, plus `stopPlaying()` when a tune starts.
      - Never during a boss bar (`RijkHud.baasActief()`), a Vadsrijk scene (`SceneSpeler`) or a story cutscene (`CutsceneSpeler`). In those cases the tune fades out in 1 s.
    - **Ambience** (`SoundSource.AMBIENT`, so the Ambient slider applies):
      - a bed loop per town: haven = golfjes, kandijkom = haardvuur, zwamzoet/knuffelkamp = briesje;
      - every 4-10 s, a sweet sound 6-16 blocks away: `vr.stad.sfeer.<stad>` by day, or mostly `vr.stad.sfeer.nacht` at night;
      - crackling near the town campfires (within 10 blocks).
- **Config**: `stadsmuziek = true|false` in `config/guhs-vadsrijk-sfeer.toml` (`SfeerConfig.STADSMUZIEK`), with lang `guhs.configuration.stadsmuziek(.tooltip)`.
- **Generator** `tools/features/vadsrijk_stadsmuziek.py` (in FEATURES, last entry): writes the sounds.json events and the lang. It fails if the OGGs are missing.
- **Synth** `tools/features/vadsrijk_stadsmuziek_geluid.py` (numpy/scipy/soundfile, fixed seeds): writes the 11 OGGs to `assets/guhs/sounds/vadsrijk/stad/`. They are mono Vorbis, about 5.9 MB in total.

## Edits outside my own package (small, marked "I-AUDIO")
- `VadsrijkFeature.register` and `VadsrijkClient.init`: one line each.
- `RijkHud.baasActief()`: a new public accessor.
- `SfeerConfig`: the `stadsmuziek` toggle.
- `WereldSfeer` and `Z2Sfeer`: their random sounds now skip the towns, to avoid doubling up. Their particles still run there.
  - Their town campfire crackle used to live in that skipped branch. StadsMuziekSpeler now plays it.
- wezens `Sfeer`/`SfeerClient`: untouched. They spawn no critters in the towns and add no town sounds.

## Track list (for the user's listen check)
| file | town | style | key / tempo | length |
|---|---|---|---|---|
| zwamzoet_1 | Zwamzoet | waltz: music box, plucked bass, chord plucks, pad | F major, 3/4 at 104 | 89 s |
| zwamzoet_2 | Zwamzoet | waltz, octave sparkle on the melody | Bb major, 3/4 at 96 | 96 s |
| knuffelkamp_1 | Knuffelkamp | morning folk: flute, plucked guitar, shaker, soft drum | G major, 4/4 at 100 | 102 s |
| knuffelkamp_2 | Knuffelkamp | morning folk, different arpeggio | D major, 4/4 at 92 | 111 s |
| haven_1 | De Haven | 6/8 shanty: flute + celesta echo, concertina pad, plucked bass, frame drum | D major | 111 s |
| haven_2 | De Haven | 6/8 shanty, more celesta | G major | 121 s |
| kandijkom_1 | Kandijkom | winter music-box waltz, glockenspiel, sleigh bells, pad | Eb major, 3/4 at 96 | 81 s |
| kandijkom_2 | Kandijkom | winter music box in 4/4, busier sleigh bells | Ab major, 4/4 at 84 | 121 s |
| bed_golfjes / bed_briesje / bed_haardvuur | harbour / zwamzoet+kamp / kandijkom | 20 s seamless loops | | |

All files are in `src/main/resources/assets/guhs/sounds/vadsrijk/stad/`. You can play them directly in any player before testing in-game.

## Not done / open choices
- **Fountain sound**: Zwamzoet has no fountain (the plaza has a guh statue on a plinth), so there is none. Market chatter is `guh_babbel` at a higher pitch.
- **Levels**: tunes play at 0.75 of the Music slider, beds at 0.55 of Ambient. If the user wants them louder or quieter, change `MUZIEK_VOLUME` / `BED_VOLUME` in StadsMuziekSpeler.
- **Retuning**: the tunes are generated, so retuning means editing `TUNES` in the synth script and rerunning it. The seeds are fixed.

## Tests
- Gametest `-Pgt=StadsMuziek` (`StadsMuziekGameTests.gebiedNaarDeuntje`) checks:
  - the town plazas, the arrival point and the Kandijkom dock map to the right town;
  - four wilderness points give no music;
  - every town has at least 2 registered tunes, a bed and day sounds.
