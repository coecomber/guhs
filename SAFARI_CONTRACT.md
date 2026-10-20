# Op Safari! (1.6.0) - contract between the slices (temporary, removed at release)

Design: ../../ontwerp-safaripark-en-bouwen.md (Dutch). Models/renders from phase 0: ../../safari-ontwerp/.
Base branch: `op-safari` (from main 1.5.2 + wiki-blokuitleg). Every slice works in its own worktree/branch
`op-safari-<slice>` from `op-safari` and only touches ITS files (listed below). Merge back one at a time.

## Scope of step 1 (user: "eerst het safaripark zelf en de dieren die daar rondlopen")
Park dimension + the park terrain/zones/fences/river/paths/decor + the animals living in it. NOT yet: bus/station,
camera/dex, lobby npcs/shop, truck, quests, verblijfsbord, building help. Entry for testing = dev command.

## Coordinates and sizes
- Dimension `guhs:safaripark` (dimension + dimension_type JSON like guhpixel: flat, no layers, own biomes, normal
  day/night cycle, has skylight, `bed_works` true). min_y 0, height 256 (logical 256).
- The whole stamped area is **768 x 768** blocks: x,z in [0, 768). The park itself (inside the outer fence, natural
  outline) lies roughly in [128, 640); the ring around it is unreachable decor to the horizon.
- Tiles: **6 x 6 tiles of 128 x 128**, height **160** (y 0..159). Template id `guhs:safari/park_<tx>_<tz>`
  (file `data/guhs/structure/safari/park_<tx>_<tz>.nbt`), tile (tx,tz) min corner = (tx*128, 0, tz*128).
- Water level (sea/river surface) **y = 62**. Typical ground y 63..75; mountains (bamboo, oertijd, ice cliffs) higher.
- Plattegrond C: park coordinate (px,pz) in the design maps (0..512) = world (px+128, pz+128).

## Data files written by the PARK generator (slice B: tools/features/safari_park/…)
- `data/guhs/safari/park.json`:
  `{"versie": n, "tegels": 6, "tegelgrootte": 128, "hoogte": 160,
    "ingang": [x, y, z], "ingang_yaw": deg,          // where /guhs safari puts you (entreedorp square)
    "grens": "safari/grens.png"? (optional)}`
  versie goes up on every change of the park; the game re-stamps when the jar's versie > stored versie.
- `data/guhs/safari/zones.json`: `{"cel": 16, "breedte": 48, "zones": ["entree","savanne",…], "raster": "<48*48 chars>"}`
  one char per 16x16 column: index into zones, '.' = outside the park (decor). Used for biomes AND for keeping animals
  in their zone.
- `data/guhs/safari/plekken.json`: animal homes, `[{"soort": "leeuw", "zone": "savanne", "centrum": [x,y,z],
  "straal": r, "volwassenen": 2, "jongen": 2, "water": false, "boom": false}, …]` — derived from the species sheet
  (design 1.6), every species at least 2 animals, herds bigger. The GAME only reads this; the park generator owns it.
- Biomes `guhs:safari_<zone>` (entree, savanne, delta, regenwoud, bamboe, ijskust, oertijd, europa, australie,
  decor): JSON written by the JAVA slice (A) (colours: grass/foliage/water/sky/fog per zone).

## Species data (slice C: tools/features/safari_dieren*.py)
- Assets: `assets/guhs/geckolib/models/entity/safari/<soort>[_man|_baby].geo.json`,
  `assets/guhs/geckolib/animations/entity/safari/<soort>.animation.json`,
  `assets/guhs/textures/entity/safari/<soort>[_man|_baby][_glow].png` (same names as the phase-0 viewer).
- `data/guhs/safari/soorten.json`: `{"<soort>": {"zone": "...", "breedte": w, "hoogte": h (hitbox in blocks, adult),
  "baby_schaal": 0.55?, "man": true/false (own _man model), "gedrag": ["kudde"|"verstopper"|"boom"|"water"|"vlieg"|
  "territorium"|"nacht"|...], "animaties": ["idle","walk","blij","eet", extra…], "snelheid": 0.2, "glow": bool,
  "bord": "gewoon|middel|zeldzaam"|null}}` for all 51 soorten (46 safari + gouden_dikdik + 4 aaihoek).

## Java (slice A: feature/safari/…)
- `feature/safari/SafariFeature` (+client) hooked in Features/FeaturesClient like other features.
- Park stamping (like guhpixel Lobby/Stempel, versioned, tile by tile over ticks), biome fill from zones.json,
  protection (no breaking/placing in the dimension for non-creative players; doors/gates usable).
- `SafariDier` GeckoLib entity: ONE entity type with synced soort + variant (man/vrouw/baby) driven by soorten.json
  (like the phase-0 proefdier, see branch safari-voorwerk feature/safarivoorwerk — port, don't merge that branch).
  Passive, invulnerable, no leash/boat, never despawns; stays inside its home (centrum+straal) AND its zone; behaviours
  from `gedrag`; idle/walk/eet/blij + random extra animations; sleeps more at night; glow layer when `glow`.
- Spawner: when the park is (re)stamped, place the populations from plekken.json (♂/♀/babies) and keep counts.
- Dev/op commands: `/guhs safari` (to the entrance), `/guhs safari terug` (back to where you came from),
  `/guhs safari herbouw` (force re-stamp), `/guhs safari dieren` (respawn populations).
- NL + EN lang for everything (Dutch with guh humour; EN only needs to exist).
- Relevant gametests only (full suite once at the end of the update).
