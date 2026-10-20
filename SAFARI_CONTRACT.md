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
