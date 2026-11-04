# NOTITIES_G_BIOMEN: one biome per Vadsrijk area (fix round 2)

JourneyMap and F3 showed "Het Vadsrijk" everywhere. Each area now has its own biome named "Het Vadsrijk - <plek>".

## What changed
- `tools/features/vadsrijk_biomen.py` (new, registered last in `tools/features/__init__.py`) generates 16 biomes
  `data/guhs/worldgen/biome/vadsrijk_<id>.json` and the lang keys `biome.guhs.vadsrijk_<id>` = "Het Vadsrijk - <Naam>"
  (nl and en are the same Dutch text):
  - zone 1: knuffelkamp, zwamzoet, haven, klodderweide, sporenwoud, druipgrotten, kloof, zwamtempel, tussenland (the zone-1
    ring and the proeftuin), zee (everything outside all boxes);
  - zone 2: kandijkom (including the dock), galmhellingen, knuffelmeer ("Bevroren Knuffelmeer"), splinterkloof, spiegelburcht,
    splinterpieken (the zone-2 ring).
  - Every biome has only `guhs:vr_kaart` (step 0), no spawners, no carvers, and no precipitation.
  - The fog colour matches the GebiedMist profile of the same area, so the client fog blends into it.
  - Water: Druipgrotten #3fc4d4 (blue-turquoise, for the Druipschacht waterfalls), Sporenwoud #8cc0c8, elsewhere soft pink-blue
    (#b4a8f0 / #a4b4f0).
- `vadsrijk_kern.py` DIM + `data/guhs/dimension/vadsrijk.json`: the generator is now `guhs:vadsrijk_kaart` with biome source
  `{"type": "guhs:vadsrijk_gebieden", "oud": "guhs:vadsrijk"}`. The old biome `guhs:vadsrijk` is kept unchanged so old chunks
  still load. It is also a "possible biome", so a half-generated old chunk still gets the map feature.
- Java `feature/vadsrijk/biomen/`:
  - `VadsrijkBiomen`: the pure x/z → biome mapping. The order is: zone-2 boxes (Z2WereldPlekken.GEBIEDEN), then the Kloof
    polyline (radius 24, copied from GebiedMist.KLOOF_LIJN), then zone-1 boxes (Gebieden.van of the fixed ids), then zee.
    It never reads the runtime Gebieden list, which dungeons are pushed into.
  - `VadsrijkBiomeSource`: the biome source `guhs:vadsrijk_gebieden`.
  - `VadsrijkGenerator`: the chunk generator `guhs:vadsrijk_kaart`. It is a copy of vanilla flat with one bedrock layer, no
    structures and no carvers. It exists because vanilla flat always uses a fixed biome.
  - `BiomenRegistry`: registered with one line in VadsrijkFeature.
  - `VadsrijkBiomenGameTests`.
- `dev/AutoCheck.java`: new script command `f3 on|off`, used to take a screenshot with the debug screen.
- `tools/autocheck/vadsrijk_biomen.txt`: the AutoCheck walk.

## Open points / choices
- The names "Tussenland" (zone-1 land ring) and "De Zee" are my choice. The ring box also contains some coast water; the biome
  source cannot see blocks.
- If a zone-2 area id is unknown (after the map is regenerated with new areas), it falls back to De Splinterpieken. Map new ids
  in `VadsrijkBiomen.zone2Biome`.
- Old worlds: chunks that are already generated keep `guhs:vadsrijk`.

## Tests (2026-11-04)
- `runGameTestServer -Pgt=VadsrijkBiomen`: 1/1 passed. The test decodes dimension/vadsrijk.json into VadsrijkGenerator +
  VadsrijkBiomeSource, checks 18 sample points (every area, the Druipschacht, the Kloof, the sea, zone 2), and checks that every
  biome has vr_kaart.
- AutoCheck `tools/autocheck/vadsrijk_biomen.txt` on a fresh world: 7 shots, 0 problems. The JourneyMap minimap shows
  "Het Vadsrijk - Knuffelkamp", "Het Vadsrijk - Druipgrotten" and "Het Vadsrijk - Kandijkom", and the map is still placed. The
  Druipschacht waterfalls are turquoise.
- The 26.1 F3 screen does not show the biome line by default; turn it on with F3+F6. JourneyMap does show the name.
- The `b_schacht_waterval` shot (from druipschacht_bodem) looks at the rock; `b_schacht_balkon` shows the falls.
