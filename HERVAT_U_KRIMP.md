# HERVAT U-KRIMP (pause 2026-11-06, PC shutdown)

Branch `110-u-krimp` (worktree `guhs_vadsrijk/u-krimp`, from 110-u-grootte 90689737b). Task: see the U-KRIMP brief (OGG ~48 kbps,
halve textures >= 1024x1024, lossless PNG, repeatable `tools/krimp.py`, jar build + gametests, NOTITIES_U_KRIMP.md).

## Done (in the WIP commit)
- `tools/krimp.py`: steps `groot` / `png` / `ogg`, manifest `tools/krimp_manifest.json` (sha1 after handling; lossy steps never run
  twice on the same hash), `--dry`, `--alleen <stap>`, `--opnieuw` (png only), `--rapport`, `--proc N`.
  - png: Pillow optimize + own brute-force writer (5 fixed filters + min-sum, zlib 9 default/filtered), exact palette (1-8 bit,
    tRNS), RGB/L/LA; every candidate decoded and checked RGBA-identical before writing.
- **Vadsrijk music re-rendered from the MIDI** (`maak_muziek.py --van-midi`) with `render.py` now flooring libsndfile
  compression_level at 0.85 (`OGG_MIN_COMPRESSIE`, env `GUHS_OGG_COMPRESSIE`): **32.85 -> 28.12 MB**, all 97 pieces same length,
  correlation with the old render >= 0.97 (same music).
- **groot**: 46 textures >= 1024x1024 halved (2x2 box, premultiplied alpha; glowmasks/gloed with their base): **15.08 -> 4.41 MB**.
  GeckoLib geo NOT changed (UVs are normalised by geo texture_width, e.g. draak geo says 512 while the png was 1024x1128).
  Offline previewer before/after (draak, luchtbizon, wolkwacht ridder, roquefort): same look.
- **png**: all 7930 PNGs: 161.28 -> 148.68 MB (Pillow pass) -> **128.13 MB** (brute-force pass). 300 random files re-checked
  pixel-identical vs HEAD: 0 differences. Total PNG ~172 -> 128 MB.

## Left
1. `python tools/krimp.py --alleen ogg --proc 10` (decode -> re-encode >52 kbps files to ~48 kbps, mono; levels 0.85/0.92/0.97;
   sample count checked; only when >= 5% smaller). Expected ~116 -> ~95 MB (of which Vadsrijk music already done).
   Check afterwards: listen to 2-3 tracks? (no client); `git status` count; 2 stereo files: see what the script decided.
2. Build: `JAVA_HOME="$APPDATA/PrismLauncher/java/java-runtime-delta" GUHS_SLOTS=3 bash ../gradle_slot.sh <u-krimp> jar runGameTestServer -Pgt=Geluid,VadsrijkGrootte`
   (copy run dir from t-int/run without saves/logs if missing). Report jar size + breakdown (was 334 MB: PNG 169, OGG 114).
3. Write `NOTITIES_U_KRIMP.md` (+ SPIEK_U_KRIMP.md short: what to listen/look at in Dutch) with the numbers above; note:
   `keep_unchanged.py` restores PNGs with equal pixels to git's (compact) version, generators that rewrite a big texture or an OGG
   get caught by re-running `tools/krimp.py` on the final tree (integrator).
4. Final commit (no "WIP"), report to the lead.
