# tools/remix — the Guhdisco's songs (2.9)

The disco game plays real songs and follows their beat (the song is the level, see `feature/disco/DiscoLiedje.java`).
The OGGs are committed in `src/main/resources/assets/guhs/sounds/disco/`; these scripts made them (run separately,
not part of `make_resources.py`; needs numpy, scipy, av).

| Song | Level | BPM | Script | Loop (beats) |
|---|---|---|---|---|
| `ze_hangen_disco70.ogg` — 70's disco version of "Ze hangen aan me vet" | medium (+ idle music) | 110 | `make_disco70.py` | 200 |
| `vadsige_tango.ogg` — Vadsige Tango | makkelijk | 100 | `make_tango.py` | 144 |
| `mika_mambo.ogg` — Mika-Mambo | lastig | 140 | `make_mambo.py` | 208 |
| `njeg_njeg_boogie.ogg` — Njeg-Njeg Boogie | bonus | 128 | `make_boogie.py` | 208 |

- `guhmix.py` + `lyrics.py` + `orig.npy` (the a cappella of the guh record, 48 kHz) + `make_disco70.py`: the
  user-approved remix. **The committed `ze_hangen_disco70.ogg` is the approved file itself** (copied from the design
  round); `make_disco70.py` writes to this folder, so re-running it never overwrites the approved song by accident.
  `guhmix.py` stays exactly as it was (the remix depends on it).
- `guhband.py`: extra instruments for the new songs (bandoneon, pizzicato, golpe/chicharra, cowbell, clave, guiro,
  timbales, brass section, Hammond, clavinet, synth bass) and `master_rms` (same loudness as the remix).
- `guhstem.py`: the guh choir. Cuts the mod's own `guh_ambient*.ogg` / `mika_ambient*.ogg` into syllables, measures their
  pitch (YIN), and sings them on notes by varispeed with the swoop flattened (`Choir.note`), tuned to the band
  (A ≈ 450 Hz). `python guhstem.py` prints the syllable table.
- `make_tango.py`, `make_mambo.py`, `make_boogie.py`: the three new songs, written to `sounds/disco/` (or to the path
  given as the first argument). Deterministic (fixed seeds).
- `check_songs.py`: checks all four (mono vorbis 48 kHz, container duration == decoded duration, loudness close to the
  remix, onset tempo == the song's BPM, beat phase 0 at t = 0, loop point inside the track). Exit code 1 on a problem.

Every song has beat 0 at t = 0; the game (`DiscoGame`) counts beats with a fractional tick accumulator
(`DiscoLiedje.ticksPerBeat()`, e.g. 10.909 ticks at 110 BPM) and flashes each colour on the nearest server tick.
