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

## Stap 2 API (S0, the shared safari core) - all safari slices build on this
User (2026-10-20): "ga door met alle andere functionaliteit binnen de guh safari" (not the building overhaul except what
the safari needs, not the Guhmension bus station structure).

Package `nl.juiced.guhs.feature.safari.kern` (wired by `SafariKern.register()` from `SafariFeature.register`, payloads
via `SafariFeature.payloads` in Features.java):
- `SafariData` (SavedData `guhs:safari_spelers`, one tag per player, offline too). Slices never touch it directly.
- `SafariSpeler` (every method as `(MinecraftServer, UUID, ...)`; most also `(ServerPlayer, ...)`):
  - dex: `dex(..., soort) -> DexRegel(gezien, sterren 0..3, foto tag)`, `spot(...)`, `registreerFoto(..., soort, sterren 1..3,
    CompoundTag foto) -> previous best stars` (kept only when better; fires dexNieuw and, when a zone gets full, zoneCompleet),
    `gefotografeerd(...)`, `zoneCompleet(..., zone)`, `telt(soort)` (dex species: not aaihoek, not gouden_dikdik).
    The `foto` tag is owned by S1 (camera): put whatever the album needs in it.
  - `vindGeheimpje(..., id) -> new?` (fires geheimpje), `geheimpjes(...)`; `claimBadge(..., zone)`, `heeftBadge`;
    `vlag/zetVlag(..., naam)` (e.g. `welkom`, `lodge`); `quest/zetQuest(..., key, stand)` (free strings, S4 owns the keys);
    `vandaag/telDag(..., teller, n, max)` (per REAL day, guhpixel `Klok`); `bordVrijOp/kanBordKopen/bordGekocht(..., soort)`
    (7 real days, `BORD_COOLDOWN`); `wis(...)` (dev/tests).
- `SafariMunten` (balance number like guhpixel Muntjes, NOT an item): `saldo`, `totaal`, `geef(..., n, reden)` (overlay
  "+n Safarimunten" + sound), `kan`, `neem` (false = nothing taken), `verdienEens(..., sleutel, n, reden)` (key pays once
  forever), `verdienDagelijks(..., pot, n, dagMax, reden)`, `zet` (dev). Amounts: design 1.8, passed in by the slices.
- `SafariEvents.opDexNieuw / opZoneCompleet / opGeheimpje / opMunten(listener)`: register in your slice's register().
- `SafariSync`: the whole player tag goes to the client (`guhs:safari_stand`) on login, dimension change and every change;
  client reads `SafariSync.stand` (CompoundTag) and may set `SafariSync.ontvanger`. Tag keys: `Dex.<soort>.{Gezien,Sterren,
  Foto}`, `Munten`, `MuntenTotaal`, `MuntenEens`, `Geheimpjes`, `Badges`, `Vlaggen`, `Quests`, `Dag` (+`DagNr`), `Bord`.
- Dev (ops): `/guhs safari munten [n]`, `/guhs safari wis`.
- Gametests: `SafariKernGameTests` (munten, dex + board cooldown).
- Park: `plekken.vermenigvuldig` keeps every home in the zone the GAME sees (zones.json cell, `plekken.cel_zone`); park VERSIE 4.

### File ownership in step 2 (parallel slices, each in its own worktree `op-safari-s<N>` from `op-safari` after S0)
| Slice | Owns |
|---|---|
| S1 camera, dex, Guhdex | `feature/safari/camera/` (+client), the Guhdex screen (`client/screen/GuhDexScreen.java` + its tabs), the `picked_up_guh` tooltip, guh numbering by tame order (F3b) |
| S2 lobby | `feature/safari/lobby/` (+client): NPC roles (`NpcRole`), giftshop UI + stock, expertenpaviljoen (badges via `claimBadge`), aaihoek chores, lodge (spawn + vroege-vogel), welcome cutscene, visitor guhs, ranger tips |
| S3 vervoer & momenten | `feature/safari/rit/` + `feature/safari/momenten/` (+client): truck (hop-on hop-off), kano, kabelbaan, voedertijden, rangerpraatje, nachtsafari, dierenverjaardag, schemabord + meldingen, weather/ambience per zone |
| S4 quests & extra's | `feature/safari/quests/` (+client): vermomde guh, te vahe olifant, geheimpjes (blocks/logic, park markers), titles Speurneus/Beverbaas (`Titels`), the FTB chapter, pootafdrukken |
| S5 verblijfsbord & thuisdieren | `feature/safari/verblijf/` (+client): the board block + menu (all tabs except the Bouwopdracht content), family of 4, petting/feeding/toys/friendships, cooldown via `SafariSpeler.bord*`, ooievaar delivery, struisvogel riding/race/egg, narwal water home |
| S6 assets | `tools/features/safari_geluid*.py` (all sounds), safari clothes (BONES for guhs + player hat/verrekijker items), safari blocks + posters/paintings, the safarivoer item (`tools/features/safari_blokken.py`, `feature/safari/spullen/`) |
| everyone | may only APPEND to shared files: lang json (`tools/features/safari.py` TEKSTEN or own generator + `tools/lang/en/c16x_*.json`), `Features.java`/`FeaturesClient.java` registration lines, `SafariFeature.register` (one line per slice) |

Merge order: S0 → S6 (assets other slices may reference) → S1 → S2 → S5 → S3 → S4 (quests use the most). Shared-file
conflicts are solved at the merge. The park generator (tools/features/safari_park*) stays owned by the park round fixes;
a slice that needs a block/marker in the park adds a zone hook in a NEW file `tools/features/safari_park/zone_<slice>_*.py`
and raises nothing (VERSIE is raised at the merge).

### S6 assets (ids the other slices use)
Java: `nl.juiced.guhs.feature.safari.spullen` (`SafariSpullen`, `SafariGeluid`, `DunBlok`, client `SafariSfeer`).
- **Sounds** (`SafariGeluid`, events `guhs:<name>`):
  - `safari.dier.<groep>`: per-species calls; every species plays its own via soorten.json `"geluid"` (SafariDier ambient).
    Groups: grote_kat, olifant, grazer, klein, vogel, groot_vogel, pinguin, zee, beer, aap, reptiel, nijlpaard, geit, ezel, cavia.
  - `safari.sfeer.<zone>` (savanne, delta, regenwoud, bamboe, ijskust, oertijd, europa, australie, entree): played by the
    client `SafariSfeer` from the biome under the player. S3 does NOT need to play ambience, only weather/particles.
  - `safari.muziek.lobby`: played by `SafariSfeer` in the entree zone.
  - `safari.camera` (S1), `safari.munt` (S0 may switch its "+n" sound to it), `safari.truck.motor` (loop) +
    `safari.truck.toeter` (S3), `safari.kano` (S3).
  - Java holders: `SafariGeluid.CAMERA/MUNT/TRUCK_MOTOR/TRUCK_TOETER/KANO/MUZIEK_LOBBY`, `SafariGeluid.DIER.get(groep)`,
    `SafariGeluid.SFEER.get(zone)`, `SafariGeluid.voorSoort(soort)`.
- **Items**:
  - `guhs:safarivoer` (tag `#guhs:safarivoer`, S5 home-animal feeding counts double);
  - `guhs:safarihoed` (player hat, head slot);
  - `guhs:verrekijker` (zooms like a spyglass; SpyglassItem).
- **Guh clothes**: `GuhClothes.SAFARI_HOEDJE`, `SAFARI_RANGERPET`, `SAFARI_VESTJE`, `SAFARI_VERREKIJKER` (items `guhs:safari_hoedje`,
  `safari_rangerpet`, `safari_vestje`, `safari_verrekijker`; KledingBronnen source `safari`). S2 dresses the visitor guhs and
  NPCs with them, and sells them.
- **Blocks**: `guhs:rieten_dak`, `rieten_dak_trap`, `rieten_dak_plaat`, `bamboewand`, `bamboe_omheining`, `safari_acaciaplanken`,
  `dierentuinbordje` (`SafariSpullen.*`, all craftable; S2 sells them too).
- **Paintings**: painting variants `guhs:safari_<naam>` (placeable, title with the size, e.g. "Rode panda (1×1)"):
  rode_panda, pinguin, koala, stokstaartje (1×1), giraffe, flamingo (1×2), pinguinfamilie, otters (2×1), leeuwenfamilie,
  reuzenpanda (2×2), savanne, regenwoud (3×2), olifant (3×3), grote_trek (4×2). S2 sells a painting item with the
  `minecraft:painting/variant` component.
- **Dierpatronen** (later pattern brush): PNGs in `tools/features/safari_patronen/` only.
- **Generators**:
  - `tools/features/safari_blokken.py` (make_resources);
  - by hand: `tools/features/safari_geluid.py` (OGGs) and `safari_schilderijen.py` (painting PNGs).
### S1 hooks (camera, dex) for the other slices
- `camera.CameraFeature.CAMERA` (item `guhs:safaricamera`): S2 gives it at the welcome (and the fotograaf-guh may sell/replace it).
- `camera.Fotograaf.JARIG` (`Predicate<String>`): S3 sets it to "is this species today's birthday species" → photo coins x2.
- `camera.Fotograaf.VROEGE_VOGEL` (flag `vroege_vogel` via `SafariSpeler.zetVlag`): S2 sets it when a player wakes in the lodge;
  the first photo after that pays double (or 10 when it pays nothing), then the camera clears it.
- `camera.Fotograaf.kijk(level, eye, look, zoom)` → `Foto` (or null): reusable "is this animal in view" (S4 pootafdrukken etc.).
- Daily missions: `camera.Missies.van(Klok.dag())`, done flags in the day counters `missie0..2`. Home animals (S5) count.
- Guh numbers (F3b): `quest.GuhNummers.nummer(server, owner, bandId)` / `getemd(...)`.
### S2 lobby: hooks for the other slices (`nl.juiced.guhs.feature.safari.lobby`)
- `Rollen.camera` (Supplier<ItemStack>): the camera the fotograaf/welcome hands out. Default: the first existing item of
  `guhs:safari_camera`, `guhs:safaricamera`, `guhs:camera`. S1 may set its own supplier.
- `Rollen.haak(npcId, (npc, player) -> handled)`: runs before a lobby character's own talk (npc ids: `hoofdranger`,
  `fotograaf`, `verzorger`, `verkoper`, `lodgebeheerder`, `expert_<zone>`). S4 hangs its quests on `hoofdranger`.
- `SafariLobby.VROEGE_VOGEL` (`"vroege_vogel"`): a SafariSpeler flag set when waking up in a lodge hut. S1 doubles the
  first photo of the day and clears it.
- Giftshop (`SafariWinkel`):
  - `data/guhs/safari/winkel.json` (written by tools/features/safari_lobby.py WINKEL) lists items by registry id; an
    entry whose item does not exist is hidden. Ids promised for S6: `guhs:safari_hoed`, `guhs:safari_verrekijker`,
    `guhs:safari_hoedje`, `guhs:kaki_vestje`, `guhs:rangerpet`, `guhs:safarivoer`, `guhs:rieten_dak`, `guhs:bamboewand`,
    `guhs:safarihek`, `guhs:dierentuinbordje`, `guhs:safari_poster`, `guhs:safari_schilderij` (rename freely in WINKEL).
  - `SafariWinkel.aanbod(id)...registreer()`: an offer with its own delivery.
  - `SafariWinkel.borden` (Function<soort, ItemStack>): the board item per species (S5). Default `guhs:verblijfsbord_<soort>`
    or `guhs:verblijfsbord`. The shop shows one board per dex species (price by soorten.json "bord": 100/175/300) and
    calls `SafariSpeler.bordGekocht` itself.
  - The zone experts also give `guhs:safari_schilderij_<zone>` (or `guhs:safari_zoneschilderij_<zone>`) when S6 makes it.
- `SafariDier.speelActie(actie, ticks)`, `SafariDier.zetThuis(pos, straal, zone)`, `SafariDier.zoneIndex()` (added for
  the aaihoek; usable by S3/S5).
- Lobby spots: `data/guhs/safari/lobby.json` from `tools/features/safari_park/zone_s2_lobby.py` (also puts a bed in the
  five lodge huts: re-stamp needed, raise VERSIE at the merge).
- Visitor guhs: wild `GuhEntity` with tag `guhs_safari_bezoeker` (can not be picked up/tamed), 8 around the square.
### S3 API (vervoer & momenten) - for the other slices
- `momenten.Momenten` (server and client): `noorderlicht(Level, BlockPos)` (IJskust at night, most nights: S1's bonus star),
  `muntFactor(MinecraftServer, soort)` (2 for today's birthday species: S1 doubles photo coins), `jarigeSoort()`,
  `voedertijd(Level, zone)` (shy animals of that zone stop hiding: `SafariDier.KALM`), `nachtsafari(Level)`, `schema(Level)`.
- Day plan in `momenten.MomentSchema` (pure; feeding 07:24..17:24 one zone after another, talks 12:00 + 15:36, night safari
  every 3rd night). Moment rangers are `GuhNpcEntity` BOSWACHTERGUH tagged `guhs_safari_ranger` (clicks only talk).
- Rides in `rit/`: `TruckEntity` (hop-on hop-off loop, guide + visitor guhs drawn client-side, own guhs in the trailer),
  `KanoEntity` (moored at the village jetty; click = guided trip, sneak-click = free canoe), `GondelEntity` (kabelbaan
  IJskust-Bamboebergen), `RaceStruisvogelEntity` (Struisvogelrace, Scorebord board `safari_struisvogelrace`; S3 owns the race
  - S5 keeps ostrich RIDING at home and the egg). Paths: `assets/guhs/safari/{route,kano,kabelbaan,race,sfeer}.json`
  (written by `tools/features/safari_park/zone_s3_rit.py` + `route_data.py`), read by `rit.RitData` on both sides.
- Visitor guhs on the truck wear `GuhClothes.SAFARIHOEDJE`/`VERREKIJKER` when S6 adds them (looked up by name), else stand-ins.
### S4 (quests & extra's) - hooks for the other slices (feature/safari/quests)
- S2 head ranger: on click call `SafariQuests.hoofdranger(player)` (true = it spoke about de vermomde guh). S2 aaihoek: after a
  chore call `SafariQuests.aaihoekKlusje(player)` (FTB quest "Aaien maar!").
- Building help (bever helpers): `SafariQuests.beverdamGebouwd(player)` per finished dam (5 = title Beverbaas).
- S6 verrekijker: set `SafariQuests.VERREKIJKER = p -> ...` (client; a vanilla spyglass always lights the pootafdrukken).
- S5 giftshop/board: the full dex gives `guhs:bordbon_gouden_dikdik` (redeem it for the Gouden dikdik's board) and the
  flags `safarikoning` / `gouden_camera` (`SafariSpeler.vlag`); S1 may give the golden camera's photos golden frames.
- Park: `tools/features/safari_park/zone_zz_s4.py` builds Dikkie's stal + olifantenloopband, places the pootafdrukken on
  the trails to the hiders and writes `geheimpjes.json` (8, one per zone) and `quests.json`.
- GuhClothes: ZEBRAPAKJE, PANDAPAKJE, PINGUINPAKJE (marker `<safari_quests>`), saved by number: at the merge they stay
  AFTER S6's safari clothes.
- Shared tool fixes: taal.py skips feature/safari (the safari generators write their own command texts, like guhpixel);
  kleding.py: markers starting with "safari" register their own clothes sources.
- FTB: chapter `guhs_safari` (make_ftbquests.py CHAPTERS, last; CHAPTER_VERSION 42), quests in features/safari_quests.py.

## Ronde 3 (test round 3 + ideas, design doc chapter 5) - contract
Merge order: R3-A first; then J1, J2, J3 (each rebased/merged on the latest op-safari); then the zone forks Z1-Z4; then the final pass.
- **R3-A rivier-pijplijn** (owner: terrein.py, kit.py, controle.py, plekken.py; ONLY the water/bridge parts of zone_*.py):
  - the single river bed + banks (always above water), the single bridge builder `kit.brug(...)` (standard design, 2-3 free for boats, ramps without jumps);
  - the zones lose their own water/bridge fixes;
  - the check gets plates, buried trees, path jumps > 1, void view from walkable ground, and free passage along `route.json`/`kano.json`/`boot.json`/`kabelbaan.json`.
  - Kit API for everyone:
    - `park.kijkplek(naam, zone, oog=(x,y,z), richting, stoel=(x,y,z))` registers a viewpoint (the J3 kijk-guh sits on `stoel`);
    - `park.favoriet(naam, kijkplek, pos, soorten=[...], straal)` registers a favourite spot in its view (used by J1's AI);
    - `park.lamp(x,y,z, soort)` registers a light (or simply places it).
  - Written to `data/guhs/safari/kijkplekken.json` (viewpoints with their favourite spots).
- **J1 diergedrag** (owner: SafariDier + feature/safari AI classes, Populatie, client renderer, tools/features/safari_atelier + safari_dieren.py, soorten.json):
  - lively AI with rest moments;
  - favourite spots from kijkplekken.json;
  - 2-3 behaviours per species (data `gedrag` + new animations);
  - sleep like guhs (sleep-eye textures, pose, zzz);
  - penguin belly slide + slide loop (uses `glijbaan` points from the ijskust zone data);
  - beavers swim/carry;
  - badgers use a tunnel path (points from the europa zone data `dassentunnel`);
  - own guhs react to animals (emotes);
  - "bekende bewoners" (named animals, data + Guhdex list in the Safari tab).
- **J2 ritten** (owner: feature/safari/rit/*, zone_s3_rit.py, route_data.py, new block `safaripoort`):
  - **safaripoort** 4x6x1 multiblock (lion-guh emblem, pink), right click, auto open/close for the truck, auto-close after 10 s, animals never pass;
  - placed at every route ∩ zone border;
  - **truck** longer with 3 rows, no trailer, own guhs walk along (teleport if > 24 behind), no clipping or damage;
  - **truck stop entree** as an open platform along the route;
  - **kabelbaan** as a loop with 2 parallel lines, T-masts, open stations with a turning loop;
  - **glass-bottom boat** (delta + ijskust bay);
  - **truck quiz** + **Safari FM**.
  - J2 must not build bridges; it asks R3-A's kit.brug if it needs one.
- **J3 lobby & beleving** (owner: zone_entree.py buildings/interiors, feature/safari/lobby/*, momenten scheduling/messages, safety):
  - ALL lobby interiors (cosy, guhs furniture; museum rangerstation; stair to the side + hole fixed);
  - ijskraam + guh-speeltuin, kampvuuravond at the lodge, pinguïnparade;
  - kijk-guhs on every kijkplek (from kijkplekken.json);
  - messages park-wide ~1 per 2-3 min with no catch-up spam;
  - **no damage/hunger in the safari dimension**.
- **Z1-Z4 zone forks** (after R3-A): savanne+delta / regenwoud+bamboe / ijskust+oertijd / europa+australie.
  - Make the existing spots bigger, wilder and more beautiful.
  - Interiors of their huts.
  - Lighting (natural + guh lamps).
  - Register kijkplekken + favorieten.
  - Specials: dome higher + sea lanterns + entrance + iglo walls + east edge closed (ijskust); beverburcht landscape + dassentunnel (europa); soft banks (australie, everywhere).
  - Never their own water/bridge fixes.

### J1 leest (kijkplekken.json + punten, read leniently by `gedrag/Kijkplekken.java`)
- Root: `{"kijkplekken": [...], "punten": {...}}` (or just the list). Missing file or keys: the animals simply stroll at home.
- Viewpoint: `naam`, `zone`, `oog` [x,y,z], `richting` (yaw degrees / "north".. / [dx,dz] / or `kijk_naar` [x,y,z]; default: toward its favourites), `stoel` [x,y,z], `favorieten` [{`naam`, `pos` [x,y,z] (ground level, where the animal stands), `soorten` [ids] (empty = any species), `straal`}]. A viewpoint may also carry its own `punten`.
- Favourites are visited by animals of those species whose home lies within (home radius + 20) blocks and in the same zone. Hiders never hide inside a viewpoint's view cone (28 blocks, 50 degrees half-angle).
- `punten` (name -> list of [x,y,z], or a nested object whose lists are flattened, e.g. `{"dassentunnel": {"a": [...], "b": [...]}}`):
  - `dassentunnel`: ordered from the entrance at the top down to the viewing room (each point walkable, ≤ 6 blocks apart; a badger that is stuck hops to the next point);
  - `glijbaan`: ordered from the BOTTOM of the slide (splash spot) up the side steps to the top;
  - `beverdam`: spots on/at the dam where beavers bring branches;
  - `beverburcht`: spots in/at the lodge (in and out of the water).
  - A list is used by an animal only when all its points lie within (home radius + 40) and in the animal's zone.
- Note for R3-A / Z3-Z4: more beavers around the dam please (plekken.py, bever is in the ZELDZAAM list; e.g. 2 homes of 3-4 near the dam + lodge).
- `data/guhs/safari/bewoners.json` (J1, safari_dieren.py): the named residents; the first home (lowest id) of that species in that zone gets the name.
- **Z3 note: the glijbaan points (for J1's pinguin slide loop)** are in `kijkplekken.json`.
  - The viewpoint is `"glijbaan_ijskust"` (stoel null, so no kijk-guh), with favourites named:
    - `glijbaan_top`: the platform, where they slide down from;
    - `glijbaan_bodem`: the foot in the water;
    - `glijbaan_trap_0` … `glijbaan_trap_n`: the side steps, in order from the bottom up (1 per 2 blocks, snow slabs as half steps).
  - All of them are for `soorten ["pinguin"]`.
  - Other ijskust/oertijd viewpoints: `onderwaterkoepel_ijskust` (all-round, chair in the middle), `kijkhut_ijskust_west`, `poolstation_ijskust`, `kijkhut_oertijd` and the towers.
- **Z4 note, the `dassentunnel` format (for J1)**:
  - It is in `kijkplekken.json`, on the viewpoint `dassen-kijkkamer`, as the field `pad = {"naam": "dassentunnel", "soorten": ["das"], "punten": [[x, y, z], ...]}`.
  - The points run from the burrow chamber through the tunnel (2 wide, 1-1.5 high, rising half a block per block, so players cannot follow) to the hill top. y = the standing surface.
  - The das home (straal 8) covers the chamber and the mouth. The viewpoint's favourite `burchtkamer` lies INSIDE the chamber.
  - Any other zone path can use the same `pad` field on its viewpoint (e.g. `glijbaan` for the pinguïns).
- **Round 4 note: the village walk of the pinguïnparade (`data/guhs/safari/parade.json`, for the new lobby)**:
  - Format: `{"dorp": [[x, y, z], ...]}`: block positions in walking order, y = the walking surface (a point may also be `{"x":..,"y":..,"z":..}`). At least ~30 blocks long (the stoet is 20 blocks: tamboer, flag, 6 penguins, 3 dancers, 2 blocks apart), at least 3 wide, no stairs steeper than 1 block per step.
  - The lobby generator writes it. Without the file the old loop `beleving.json` `parade_dorp` is used. The IJskust walk stays fixed (the truck route in the IJskust).
  - Reader: `momenten/ParadeRoute`; walker: `momenten/Parade` (members scripted 2 blocks apart along the walk; guhs tagged `guhs_safari_parade_guh`, penguins `guhs_safari_parade`).

## Ronde 4: lobby + Kriebelhuis build (design: chapter 6 rounds 47-56 + safari-ontwerp/lobby2/ontwerp.md + renders _v2 + lobby2_overzicht_legenda.png)
- **L1 lobby** (owns zone_entree.py, rewritten to the lobby2 layout; also lobby.json/beleving.json/parade.json writers). It writes **`data/guhs/safari/lobby_aansluiting.json`**:
  - `{"bus": [x,y,z], "hoofdsteiger": [x,y,z], "kano": [x,y,z], "truckperron": [x,y,z], "parkpoort_voet": [x,y,z], "boot_start": [[x,y,z],...]}`, plus the water connection from the pond to the river;
  - the truck route and the boat line must connect to these (R4-B2 boat line, J2 truck route).
- **L2 Kriebelhuis** (owns zone_kriebelhuis.py, a new module, building + the 4 aquarium parts + homes via the plekken hook).
  - New block ids it may use (L3 makes them): `guhs:terrariumglas`, `guhs:kasglas`, `guhs:mierenboerderij`, `guhs:infobord`, `guhs:warmtelamp`, `guhs:nepkoraal`, `guhs:glimpaddenstoel`, `guhs:sterrenplafond`, `guhs:schorsplaat`, `guhs:glasvloer`.
- **L3 blocks** (owns those blocks: Java + tools generator) + the weerder test fix (radius steps 24/32/48/64/128).
- **L4 Kriebelhuis animals** (owns soorten.json entries for the 36 new species, their behaviour, the dex chapter "Kriebelhuis" with subsections + the 9th expert, terrarium boards; plus the guhs critters spawning in the aquarium/vlindertuin/kikkerhoek).

## Deel 2: bouwhulp (design doc part 2, chapters 2.1-2.10 + rounds B1-B13, 29-30)
Package `nl.juiced.guhs.feature.bouwhulp` (+ client). Generators `tools/features/bouwhulp*.py`. Merge order: F8 → F11 (∥) → F9 → F10.
- **F8 kern** (owner: bouwhulp/kern, bouwhulp/keet):
  - the `Spookbouw` API: a project = a block map (relative positions → states) + origin + rotation/mirror + owner;
  - ghost rendering (client, translucent, visible to all, weak);
  - shortage list; material from inventory → linked Bank Guh → (level ≥1) all chests on the vadsnet;
  - "alsnog bouwen" (missing stays ghost) after a yes/no; undo 5; open ghosts list; place block by block over ticks (budget per tick);
  - never in Protected structures.
  - The Bouwkeet (3×3×3 multiblock, yellow-pink, crane bones), the tabbed menu shell (Vormen | Daken | Blauwdrukken | Bouwguhs | Materiaal | Niveau);
  - levels 0-4 with tier upgrades (the upgrade items' names/recipes are Claude's choice → doc chapter 9), VK consumption;
  - bouwguhs (max 4, via the existing guh hooks), sfeer (whistling guhs, the crane turning, a progress bar, a party), Pof! at level 4.
  - API for the others: `Spookbouw.start(ServerPlayer, Ontwerp, BlockPos, Rotation, Mirror)`, `Ontwerp` (a block map + size), `Materiaal.zoek/neem`, `BouwNiveau.van(keet)`.
- **F9 gereedschap** (owner: bouwhulp/vormen, bouwhulp/blauwdruk, bouwhulp/kwast):
  - the vormstaf (radial menu + screen), all shapes, roofs (dakmal), the kwasten (vervang/mix/verloop+patroon/verweren; animal patterns unlocked via the safari dex), capture (2 corners + auto);
  - own blueprints, the catalogue (~25 + placeholders, preview renders), the server library (SavedData, maker name, likes, max 10/player);
  - egaliseerder (lvl 2), verfmachine + tunnelboor (lvl 3).
  - Name: "Ontwerpbureau" (not Tekentafel).
- **F11 blokken** (owner: bouwhulp/blokken):
  - vertical slabs + corners, beams & posts (auto-connect), sloped roof parts (½, ¼);
  - guh window frames & shutters, pastel bricks/plaster (16), guh tiles, wallpaper + skirting, plush blocks, stained-glass guh windows + portholes, gutter/chimney/dormer;
  - garden & street (hekjes with ears, lamppost with guh cap, a "Welkom guh" doormat, bench, birdhouse);
  - pattern textures from tools/features/safari_patronen.
- **F10 helpers** (owner: bouwhulp/helpers + the verblijfsbord Bouwopdracht tab + Beverbeek biome generator):
  - adaptive templates per helper animal (beaver dam with the water level, graafploeg, olifant, orang-oetan, pinguïn, capibara, zebra paths, koala/toekan planting, neushoorn demolishing, otter, flamingo, kangoeroe, mammoet);
  - build animals (specht, octopus, gibbon, gekko, glimworm, prieelvogel);
  - 1 project per board, up to 48 blocks;
  - the "Bouw passend verblijf" button;
  - Beverbaas (SafariQuests.beverdamGebouwd);
  - the Beverbeek biome + a ranger post with a pettable beaver (Guhmension, new chunks only).
