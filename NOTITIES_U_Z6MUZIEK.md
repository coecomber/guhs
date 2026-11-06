# NOTITIES_U_Z6MUZIEK: the music + ambience of zone 6, Het Zijdeland

Branch 110-u-z6muziek from 110-u-audio2 (5e8e108cf: boss phase layers + fanfares). Pipeline: T-GELUID's `tools/vadsrijk/geluid`
(own pure-numpy SF2 player, GeneralUser GS at `guhs_vadsrijk/_geluidkit/GeneralUser-GS.sf2`).

## What is built
- **The pieces** (`tools/vadsrijk/geluid/zijdeland.py`, hooked into `stukken.alle()`): 39 rendered pieces.
  - Timbres: oud = nylon guitar (`oed`), buzuq tremolo = mandolin (`mandoline`), qanun = dulcimer (`kanun`), ney = shakuhachi
    (`ney`), darbuka/riq = GM kit congas/bongos/tambourine (maqsum, baladi, sama'i, 6/8 patterns), plus music box, harp, celesta,
    tinker bells (`tinkel`) for the sweetness.
  - Maqam colour: a new mode `hijaz` in `componist.MODI` (phrygian dominant; the b2 major chord is the cadence), nahawand = `harm`,
    rast = `mixo` (no quarter tones), ajam = `majeur`.
  - Ornaments: `ZijdeStuk` adds maqam-style graces and turns to the ney/qanun/oud/buzuq melodies WHILE they are written (own
    seeded random), so echoes and `_fase` doublings copy them and the base/fase loops stay note-identical (checked).
  - The zone's leitmotif is **Oma Dadel's karavaanlied** (`KaravaanStuk.LEIDMOTIEF`, 4 bars): in the B section of Roze Duinen,
    Zoutvlakte-nacht and Rozenkloof 1, in the A section of De Moederoase, in both stingers, sung by a choir in Ringeling's last
    phase, and in Ringeling's sweet fanfare.
- **Area themes** (`vr.geluid.gebied.*`, ~66-122 s, long silences as before): roze_duinen, palmenkloven (6/8 river lilt),
  zoutvlakte (day) + zoutvlakte_nacht (night, picked by time of day: `GeluidPlan.dagNacht`), ruinedal, moederoase.
- **Town tunes** (`vr.stad.muziek.*`, via stadsmuziek): rozenkloof_1 (Rozenplein, happy), rozenkloof_2 (a ney with canyon echo),
  saffraanpoort_1 (De Grote Bazaar bustle: buzuq + clarinet + oud ostinato + maqsum/riq), saffraanpoort_2 (palace courtyard waltz).
- **Boss themes + `_fase` + own fanfare** (9): wormelien, zoutkolos (field); sterrenwacht (= Astrolabiumwachter), glimmertje,
  verzonken_sterrenguh (dungeon); zandwachter (ticking hourglass woodblocks, faster in the last phase), spiegelvizier (qanun +
  music box; last phase shattering crystal + crashes), sterrenbeelden (three voices: low horn / fast strings / celesta), ringeling
  (pompous waltz with jingling rings; last phase "Ringdans" = the whole oasis sings the karavaanlied). Ringeling's fanfare is
  hand-written: a grumpy hijaz trumpet run, a breath, a harp glissando, then the karavaanlied's end in sweet D major (13 s).
- **Stingers**: `baken` (a star beacon lit) and `moederoase` (the oasis found; plays automatically on the FIRST visit of
  `z6_moederoase` instead of the generic `ontdekking`).
- **Ambience** (`maak_sfeer.py`, appended so old seeds are unchanged): beds `duinen` (very soft high sand hiss in slow swells +
  far camel bells; NO wind roar), `oase` (lapping water, birds, drips), `bazaar` (synthesised far-away babble of 14 voices + cups),
  `sterren` (crickets + star twinkles; the zone's NIGHT bed), `toren` (the grey tower's low sucking drone); one-shots belletje,
  sterretje, zoutzang (singing salt crystal).
  - Night: `GeluidPlan.nachtBed`: duinen is replaced by sterren at night, oase gets sterren on top; the night one-shots there are
    crickets, twinkles, singing salt, the owl.

## Wiring
- `GeluidPlan.STREKEN` (ids of u-z6wereld `Z6WereldPlekken.GEBIEDEN`):

| area id | themes | bed |
|---|---|---|
| z6_roze_duinen | roze_duinen | duinen |
| z6_palmenkloven | palmenkloven | oase |
| z6_zoutvlakte | zoutvlakte / zoutvlakte_nacht | duinen |
| z6_ruinedal | ruinedal | duinen |
| z6_moederoase | moederoase | oase |
| z6_toren | (none: drone only) | toren |
| z6_zijdeland (+ any other `z6*` id, + the box x 3236..4287, z -1152..-385) | roze_duinen | duinen |
| z6_rozenkloof / z6_saffraanpoort | towns: `StadsMuziek` rozenkloof_1/2, saffraanpoort_1/2; beds briesje / **bazaar** | |

- **Bosses**: entity path -> theme in `GeluidPlan.baasThema` (prefixes `vr_z6_baas_`, `vr_z6raid_baas_`; `vr_z6raid_baas_ster_*`
  -> sterrenbeelden; astrolabiumwachter -> sterrenwacht; sterrenguh -> verzonken_sterrenguh).
  - u-z6wezens' `Z6Muziek` already uses these theme names (wormelien, zoutkolos, sterrenwacht, glimmertje, verzonken_sterrenguh)
    and switches from its stand-ins to them by itself once they are in `BAAS_THEMAS` (they are now). Nothing to do.
  - u-z6raid's `Z6RaidMuziek` ids (`guhs:vr.z6raid.muziek.<baas>[_n]`) are read by `GeluidPlan.alias`: one theme per boss; the
    last numbered phase (Ringeling `_3`, the others `_2`) crossfades into the `_fase` loop, a lower number fades back (the
    constellations' window goes both ways). Done client-side in `GeluidSpeler.zet` and server-side via `themaVanId` (fanfare).
    Nothing to change in the raid slice.
  - `vr.z6raid.muziek.opgevrolijkt` (Ringeling's fanfare) and `.wachtzaal` (the tower drone) exist in sounds.json (not in the
    registry, so the raid slice may register them later without a clash). The raid does not play them yet.
- **Beacon stinger**: no hook exists yet. Whoever lights a beacon (u-z6verhaal `Z6Puzzels`, the line with `PLAYER_LEVELUP` ~l.212)
  should call `GeluidRegistry.stinger(p, "baken")`.

## Size
**+15.5 MB** of OGG (53 files): area 4.2 MB, towns 2.1 MB, bosses 4.2 MB + fase 3.4 MB, fanfares 0.5 MB, stingers 0.16 MB,
ambience 0.85 MB. To shrink: raise `kwaliteit` (0.68 -> 0.8 ≈ -25 %) in `zijdeland.alle()`, or drop the 2 extra dungeon themes.

## Decisions I made myself
- "The dungeon boss" = all three dungeon bosses got a theme (u-z6wezens named 3 theme ids; cheaper than borrowed themes).
- Ringeling has 3 phases but only one extra layer: phase 2 (De Lege Lens) keeps the base loop, phase 3 (Ringdans) = `_fase`.
- The constellations: one theme for all three; `_fase` = the re-light window.
- Zone-6 field/raid bosses keep u-audio2's phase hook (`VadsrijkBaas.krijgSchade`, last phase = fases().length+1) too.
- The dunes are the "duinen" bed: sand hiss at -36 dB, no wind (user dislikes strong wind). Bazaar murmur is synthesised (no
  guh takes in the bed) so it does not sound like a crowd of the same guh.
- Added a dev preview for town tunes: `/guhs vadsrijk geluid stad <naam>` (all towns).

## Shared-file changes (all marked `U-Z6MUZIEK`)
- `geluid/GeluidPlan.java`: lists, streken, `ZONE6` + `inZone6`, `nachtBed`, `dagNacht`, `Alias`/`alias`, baasThema prefixes.
- `geluid/GeluidRegistry.java`: moederoase stinger on first visit; `stad` dev command + list line.
- `geluid/client/GeluidSpeler.java`: day/night theme pick; raid alias + two-way phase crossfade in `zet`; `stad` preview.
- `geluid/client/GebiedSfeerSpeler.java`: night bed via `nachtBed` (2 lines).
- `geluid/client/GeluidClient.java`: `stad` preview case.
- `stadsmuziek/StadsMuziek.java`: STEDEN + rozenkloof/saffraanpoort, `vr.stad.bed.bazaar`, `stadVan` z6 ids.
- `tools/vadsrijk/geluid/`: componist (hijaz + 5 instruments), vormen (`klasse` hook in 3 places), stukken (2 lines),
  maak_sfeer (beds/one-shots + name filter).
- `tools/features/__init__.py`: + `vadsrijk_u_z6muziek` (after u_audio2); `sounds.json` generated entries.

## Tests
- `-Pgt=Geluid`: the old ones (they cover all new themes: registered, OGG, fase loop, fanfare) + new `zijdeland` (streken, towns,
  tower, box, day/night, night beds, boss paths, raid alias, town tunes, the oasis stinger). Result: see the commit message.
- Checked by numbers: base/fase loops identical length + base notes a subset of fase (all 9); peaks ≤ 0.62; bed seams < 0.005.
- Listening is for the user (SPIEK_U_Z6MUZIEK.md). I could not listen.

## Open points
- Taste: everything needs a listen (especially how "maqam" the shakuhachi-ney sounds, and the bazaar murmur bed).
- The beacon stinger needs the one call in the story slice; the tower drone in the raid's Wachtzaal is not played by the raid.
- Ringeling phase 2 has no own colour (dark lens): would need a 3-way sync crossfade.

## Resume point
- Re-render: `python tools/vadsrijk/geluid/maak_muziek.py <names from zijdeland.py>` and
  `python tools/vadsrijk/geluid/maak_sfeer.py duinen oase bazaar sterren toren belletje sterretje zoutzang`,
  then `PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_z6muziek.py`.
