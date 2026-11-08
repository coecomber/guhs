# NOTITIES U-GROOTTE (round U, 2026-11-06)

Branch `110-u-grootte` (worktree `guhs_vadsrijk/u-grootte`, from 110-v-int 16206e964). Task: make the pack (mod jar) substantially
smaller without changing the world, plus an FPS check of the heavy client systems.
Code: `feature/vadsrijk/grootte/` (KaartPak, KaartPakLader, StructuurPakLader, GrootteRegistry, VadsrijkGrootteGameTests),
`mixin/StructuurPakMixin`, `tools/vadsrijk/kaartpak.py`. Cheat sheet: `SPIEK_U_GROOTTE.md`.

## 1. Pack size

### Measured before (src/main/resources, 2026-11-06, zones 1-5)
| part | files | MB |
|---|---|---|
| PNG textures | 7930 | 172.0 |
| Vadsrijk map tiles (`structure/vadsrijk/tegels/*.nbt`) | 1116 | 130.7 |
| OGG audio | 666 | 116.6 |
| other guhs structures (`structure/**.nbt`) | 1263 | 70.1 |
| geo/model json | 8135 | 42.7 (compresses ~10x in the jar) |
| other json (lang 2x 3.9 MB, ...) | 13211 | 23.9 (idem) |
| rest | | ~2.7 |

Tiles per zone (`python tools/vadsrijk/kaartpak.py --rapport`):
```
zone (rough, by tile range)           tiles   .nbt MB   pack MB   (a deduplicated tile counts once, at its first zone)
edges / open sea                         18      2.65      0.13
zone 1                                  690     63.13      2.24
zone 2                                  110      6.80      0.71
zone 3                                  154     30.85      2.45
zone 4                                   88     20.64      0.28
zone 5 (+ shared z4 tiles 38..39)        56      6.61      0.35
total                                  1116    130.67      6.25   (937 unique tiles)
```

### What was done
- **Compact pack format (KaartPak, pure Java).** Per entry exactly what vanilla `StructureTemplate.load` reads: DataVersion, size,
  the palette compounds VERBATIM in the entry's own order (so state indices do not change; one global table of distinct palette
  entries), every block as a dense y-z-x grid of state+1 (0 = no block, so "absent" and "air" stay different) cut to the used
  layers, block-entity compounds verbatim, entity list verbatim; deflate level 9 per entry; identical entries stored once (179 of
  the 1116 tiles are duplicates: open sea / cloud sea). Anything the grid cannot hold exactly (duplicate or out-of-range positions,
  multi-palette, odd tag types) is stored as its original gzip bytes ("raw" mode); currently 0 entries need it.
- **Two packs**, built by gradle task `kaartPak` (JavaExec right after compileJava, input = the structure folder; it reruns only
  when a structure or the packer changes; ~30 s) into `build/generated/kaartpak`, added to processResources:
  - `data/guhs/vadsrijk/kaart.gpak`: the map tiles, **130.7 MB -> 6.25 MB (20.9x)**.
  - `data/guhs/structuren.gpak`: all other guhs structures, **70.1 MB -> 3.79 MB (18.5x)**.
  - The jar excludes `data/guhs/structure/**/*.nbt` (build.gradle `jar` block). **Jar: ~200.8 MB of structures -> 9.0 MB (jar 525 -> 334 MB).**
- **Runtime:** `KaartPakLader` rebuilds the same CompoundTag and loads it exactly like vanilla's `TemplateSource.readStructure`
  (DataFixTypes.STRUCTURE + StructureTemplate.load with the server's feature-filtered block lookup), then KaartFeature places it
  with the unchanged settings. Map tiles: the pack wins (also in dev, when it exists); no pack -> the old .nbt path.
  Other structures: `mixin.StructuurPakMixin` on vanilla's `ResourceManagerTemplateSource.load`: ONLY when the structure's .nbt is
  not in the resources (the shipped jar) and the pack has it. Dev trees, datapack overrides and the world's own saved
  structures keep working as before. `list()` also lists the pack's names.
- **Generators unchanged:** they keep writing the vanilla `.nbt` tiles (the working format every checker reads: loopcheck, verbind,
  overgang, audit, ...). `maak_kaart.py` now ends with `tools/vadsrijk/kaartpak.py` (pack + verify + per-zone report; `--geen-pak`
  skips it); gradle packs anyway before every build, so a forgotten run cannot ship stale tiles.
- **Verification (three layers):**
  1. `gradlew kaartPakControle` / `kaartpak.py`: decodes EVERY entry of both packs and compares it with its .nbt (palette bytes,
     every block, every block entity, size, DataVersion, entities): 1116 + 1263 entries, 0 differences.
  2. Gametest `pakTemplatesGelijk`: every 8th tile loaded by VANILLA from the .nbt vs from the pack: `template.save()` equal.
     `structuurPakGelijk`: the same for every 6th other structure.
  3. Gametest `pakWereldGelijk`: one chunk of 12 sample tiles placed twice far away with KaartFeature's exact settings/box (old
     .nbt vs pack), every block state + block entity of the 16x384x16 column compared.

### Checked, NOT changed (need the user's OK: they change looks/sound)
- **OGG** (116.6 MB, 269 minutes): already mono (only 2 stereo files, 1 MB), mostly 44.1/32 kHz at ~60 kbps (77 MB in 32-64 kbps,
  34 MB in 64-96 kbps). Re-encoding to ~48 kbps would save ~20-25 MB but is a lossy re-encode of lossy audio and no encoder is
  installed here (no ffmpeg/oggenc). Proposal: re-encode the long music tracks (einde2_audio/muziek_*, ~2 MB each) at q2.
- **PNG** (172 MB): lossless re-compression (PIL optimize + exact palette when <= 256 colours) gains only ~4.5% (~8 MB) on a
  300-file sample: the textures are noisy fur. The real lever is resolution: 46 textures >= 1024x1024 (15 MB; luchtbizon/appa
  1024x1024 ~1.1 MB each, beeld_* 512x512 ~0.58 MB each). Halving those is a looks decision -> ask.

## 2. FPS check (code-level; no client run: the user's client runs from t-int and RAM was shared with ~6 gradle runs)
Audit of the per-frame / per-tick client work in busy places, ranked. Fixed (FPS-only, no visible change near the camera):
1. **Hoogvads townsfolk / all props (`Z3Figurant`, up to 160 full animated guh models)**: render distance was 256 for every prop;
   now 96 for normal props (the server removes them at 80 from any player anyway), 256 only for giants (scale >= 2: Wolkwal fin
   tips, statues). Per frame per prop the renderer built 3 Identifiers (string concat), split the hidden-bone string, scanned all
   GuhVariants and allocated a String[]: all cached now (`Z3BazenClient.FigurantRenderer`).
2. **Guh NPCs / riders / Wolkwacht (`GuhRenderer`)**: clothes and the hooks' extra model passes (each = a full model walk) are
   skipped beyond 64 blocks (`GuhRenderer.VER_SQ`); the hook-pass bone masks (`GuhBoneMasks.Masks.of(Predicate)`) were rebuilt
   every frame (2 boolean[] + predicate test per bone): now cached per test object (weak keys).
3. **Mounts (`UiterlijkLaag`, `EconomieClient`)**: texture Identifiers cached; the pattern pass (second full model walk) skipped
   beyond 64 blocks; its "all bones" mask is now a constant predicate (cached).
4. **NPC quest markers (`NpcClient`)**: the "!"/"?" texts were rebuilt per NPC per frame; now built once (still visible at any distance).
5. **Moeder Wolkwal spout**: ~800 forced particles/packets per player per spout; a viewer farther than 96 blocks now gets half.
6. **World point sounds (`WereldBron.soortOp`)**: the windgong name test (registry key + string contains) ran for every scanned
   block (700/tick); now once per block type.
Not changed (already budgeted or minor): t-kleding effects (budget 48/tick, 32 blocks), u-zichtbaar (12 players/frame, LOD
16/32/48), t-vfx (particle budget 700/250, LOD 48/64, decal cap 64), kites (LOD 64), airships/silhouettes (6 quads), Ouwe Dobber
(1 entity), party frames (5 members), u-ruiters (server budget 14, dust LOD 24).
Open (bigger, not done): GeckoLib animation LOD for far props (freeze the idle animation beyond ~32 blocks); VFX decal height
cache (HashMap<Long,Float> lookups per vertex, boss arenas only); kites: batch the ~25 geometry submits per near kite;
WereldGeluiden.bronnen() every tick -> only when a sound is due.

## Shared-file changes (all marked `U-GROOTTE`)
- `build.gradle`: jar excludes `data/guhs/structure/**/*.nbt`; tasks `kaartPak` + `kaartPakControle`; processResources adds the packs.
- `kern/wereld/KaartFeature.java`: `tegel(sl, tx, tz)` + `vergeet(...)` (pack first, else structure manager); place/ruimOp use them.
- `kern/VadsrijkCommando.java`: the arrival-tile check uses `KaartFeature.tegel`.
- `wereld/VadsrijkWereldGameTests.java`: uses `KaartFeature.tegel` (the jar has no .nbt tiles any more).
- `VadsrijkFeature.java`: one line `GrootteRegistry.register(modBus)`.
- `guhs.mixins.json`: `StructuurPakMixin`.
- `tools/vadsrijk/maak_kaart.py`: calls `kaartpak.py` at the end (`--geen-pak`).
- FPS: `client/GuhRenderer.java`, `client/GuhBoneMasks.java`, `zone3/bazen/Z3Figurant.java`, `zone3/bazen/client/Z3BazenClient.java`,
  `rijdier/client/UiterlijkLaag.java`, `economie/client/EconomieClient.java`, `npc/client/NpcClient.java`,
  `zone5/wolkwal/WolkwalBeweging.java`, `geluid/WereldBron.java`.

## For integration
- Nothing generated is committed: the packs are built by gradle into build/generated (never in git).
- Any code that reads a guhs structure .nbt DIRECTLY through the ResourceManager (not via the structure manager) breaks in the jar.
  Today only two gametests do (Vadsmijnstadje, Spiesburcht; gametests run from the dev resources, so fine). New code: always use
  `level.getStructureManager().get(id)` (or `KaartFeature.tegel` for map tiles).
- Dev command: `/guhs vadsrijk grootte status`.

## Tests
- `gradlew kaartPakControle jar runGameTestServer -Pgt=VadsrijkGrootte,VadsrijkWereld`: BUILD SUCCESSFUL, **all 8 tests passed**
  (VadsrijkGrootte 3: pakTemplatesGelijk, pakWereldGelijk = 1,176,576 positions / 29,002 blocks identical, structuurPakGelijk = 211
  structures identical; VadsrijkWereld 5, incl. the tile loading + placement tests, now via the pack). Pack load: 11 ms.
- kaartPakControle: 1116 + 1263 entries, 0 differences.
- **Real jar** (`build/libs/guhs-1.9.2.jar`): **334 MB** (PNG 169, OGG 114, classes 26.5, packs 9.0, json 8.2); without the packs it
  would be ~525 MB (the .nbt are already gzip, the jar cannot squeeze them). No .nbt left in the jar.
- FPS fixes: compiled + the gametest server starts; NOT measured in a client (no client run: shared RAM). The user checks F3 fps.
