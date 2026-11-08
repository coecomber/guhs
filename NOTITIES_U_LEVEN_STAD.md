# NOTITIES U-LEVEN-STAD (round U, 2026-11-08, branch 110-u-leven-stad from 110-v-int 3349c1d52)

The user's wish (FEEDBACK_RONDE4 V7, "levendigheid, alles erbij"): more life, coziness and guh-ness in the towns of zones 1-6:
- pieppiepmuisjes, plus ~30% light-blue guh-blobs;
- guhs' pets;
- street life and mini-events;
- child-guhs on playground equipment;
- terraces with chatting guhs.

Parallel slices: u-leven-ritme (day/night rhythm, weather, emote reactions, town paper) and u-geroezemoes (background chatter). They are not touched here; see "Integration" below.

## What is built (package `feature/vadsrijk/stadsleven/`)

### Entities
All three are `noSave` types (MISC), never hurt, not pushable, and never saved.

- **`vr_sl_prop` = `LevenProp`.** Any Guhs GeckoLib model, texture and animation, synced, with a render scale.
  - It also has a synced **tilt** (`kantel`, degrees around the model's x axis at its origin). The seesaw and the swings use it; the merry-go-round uses yaw.
  - Uses: the critters, the ducklings, the birds, the ice-cream cart (the existing `ijscoguh` model), the playground's moving parts and the picnic blanket.
- **`vr_sl_guh` = `LevenGuh extends npc.RijkGuh`.** The real guh with clothes, but without AI (`registerGoals` is empty) and without gravity.
  - It is moved kinematically by its scene and can sit (`zit`).
  - `houd(Emote)` keeps an emote looping (ukulele, eating, dancing, petting). This is needed because `GuhEmotes` only runs for the wild guh type, so the emote data is re-applied every tick.
  - `zeg(text, ticks)` shows a speech bubble: the custom name, shown for a few seconds. `shouldShowName` returns true only then.
  - Children are the same guh with `Attributes.SCALE` 0.55.
- **`vr_sl_kat` = `LevenKat extends Cat`.** The vanilla cat with a random coat and a pink collar.
  - No AI, silent (purring would fill the street), sitting, no gravity.
  - A click gives hearts, a purreow and a chat line.

### Towns (`Steden`)
Keyed by kern area ids, so zone 6 works as soon as u-z6wereld's areas are merged.

| Zone | Towns (area ids) |
|---|---|
| 1 | knuffelkamp, zwamzoet, haven |
| 2 | kandijkom (z2_kandijkom + _dok; snowy, so a soup cart) |
| 3 | honingtak (layered area, z3_honingtak) |
| 4 | lampionhaven (+ z4_havenplein); every island its own little town with its own caps: knuffeltij, strandfeest, bloemeneiland, vliegereiland, vuurtoreneiland, schildpadeiland (z4_schildpad) |
| 5 | hoogvads (z5_hoogvads + all its districts) |
| 6 | rozenkloof (z6_rozenkloof), saffraanpoort (z6_saffraanpoort) |

### Spots (`Plekken`)
Spots are read from the REAL world near the players. Nothing is generated, so every zone works and no regen is needed.
- **The scan:** one scan per chunk and height band (8 blocks; scans y-12..y+20), with an LRU cache of 900 entries in memory, never saved. 3 chunks per planning pass (every 10 ticks).
- **Kinds of spot:**
  - GROND: standable, not leaves, fences or lamps;
  - GRAS: the grassy ones;
  - DAK: a roof-ish block at least 3 above the lowest surface of its column;
  - WATER: a water surface with air above;
  - LAMP: the top of a lantern/lamp/lampion block;
  - ZIT: Guhs benches and chairs (`guh_bankje_*`, `guh_bank`, `bank_guh`, `guh_stoel`, `guh_hoofdbank`, `buizenbank`), with their `facing`.
- **Walking:** `grondY` and `loopbaar` keep walkers on the ground: at most 1 step up, never through walls or water. Kinematic walkers can never get stuck; a wall means "arrived".

### Scenes (`Groep`, `Beestjes`, `Straat`, `Speeltuin`)
Caps are per town at VOL. ZACHT = half, rounded up.

| Scene | Cap (VOL) | What it does |
|---|---|---|
| DIERTJE | 9 | A pieppiepmuisje (dashes, eats, squeaks; 1 in 4 pink) or, with 30% chance, a light-blue guh-blob (the zone-1 klodder model + its happy face texture recoloured light blue, scale 0.5, hopping). |
| WANDELAAR | 2 | A guh walking its pet along a walkable path: a mouse, a guh-konijntje, a blue blob or a duckling. The pet follows on the trail 1.2 blocks behind. The guh sometimes stops to pet it (emote + heart). |
| KAT | 3 | A cat on a roof. Now and then it turns its head or stands up; a soft meow when someone is near. |
| EENDJES | 1 | A mother `guh_eendje` + 3 kuikens (scale 0.55) swimming in a line on water with at least 14 water cells around. They keep 0.7 blocks from the edge. |
| VOGEL | 4 | A pluisvinkje/kaasmeesje flies in, lands on a lamp top or a bench backrest, pecks, hops to up to 4 perches, then flies off. |
| MUZIKANT | 1 per town | A guh with the ukulele emote and a cute tune on note-block harp/bell (volume 0.16, music notes). Sometimes a dancing listener comes for a while. |
| KAR | 1 per town | The `ijscoguh` model (scale 0.7) rides slowly along the streets (max step 0.55, so no stairs). At each stop it rings the bell and shouts "IJsjes!" (in Kandijkom: "Warme soep!"). Daytime only. |
| PICKNICK | 1 | 2-3 guhs sitting round a pink-white checked blanket with a basket on a lawn. They eat and chat. Day, no rain, 2.5-5 min. |
| TIKKERTJE | 1 | 3 child-guhs play tag in a 4.5-block field. The tagger chases, the others flee along the edge. A tag gives a giggle + "Tikkie! Jij bent 'm!" and the tagger counts to three. Day, no rain. |
| TERRAS | 2 | 2-4 guhs on a cluster of Guhs benches/chairs (within 3 blocks). Without benches: 2-3 guhs chatting standing on the street. They eat and drink (smakken), laugh, and say lines with speech bubbles. |
| SPEELTUIN | 1 per hub town | See below. |

### Speech bubbles (`Babbels`)
- **Content:** Dutch lines, with real English in en_us.
- **Pools:**

  | Pool | Lines |
  |---|---|
  | njeg | 6 |
  | klets | 14 |
  | roddel.z1..z6 | 10 per zone (the dragon, the Keizer's "long arms", crystals, Graaf Piepvel, the Vliegervanger and the Knuffeltij, Moeder Wolkwal and the torn page, the star caravan and Hertogin Ringeling) |
  | kind | 8 |
  | speel | 6 |
  | kar.ijs | 3 |
  | kar.soep | 3 |
  | muziek | 4 |
  | picknick | 5 |
  | wandel | 5 |
  | kat | 3 |

- **Throttle:**
  - a bubble only when a player with Stadsleven on is within 20 blocks;
  - at most 3 at once per town;
  - per scene one try every 70-180 ticks.
- **A click on a town guh:** a fitting line in chat, "Stadsguh/Guhtje: ...".

### The playgrounds
- **Map side:** `tools/vadsrijk/stadsleven/speeltuin.py`.
  - It is a pass over the written tiles, like `verbind.py`. Seven hub towns come from six different generators (zone 6 in its own branch), so one deterministic pass replaces six hooks.
  - **Finding the spot:** a free 13x10 spot near the town centre.
    - The floor is the highest solid block with 5 free cells above; garlands, lamps and crowns higher up do not count.
    - Every footprint column is at F-2..F+1. Only natural ground (grass/dirt/sand/snow/gras) is dug down one block. As a fallback, at most half the footprint may be a paved step.
    - The ring around is within one step for at least 85%, and 3 of the 4 openings lead out at +-1.
    - No published stand spot (POI_*.md / `_cache/plekken*.json`) lies inside.
    - Lawns are preferred over plazas (paving penalty) and dirt paths are avoided (max 15%).
  - **The stamp:**
    - the floor: pink and white concrete, with a white edge;
    - a cherry fence ring with 3-wide openings in the middle of all 4 sides, and lanterns on the corner posts;
    - the slide: a ladder, a pink concrete tower and a cherry stair/slab slide (walkable in half steps);
    - the seesaw post, and the swing frame (posts + a beam at F+4);
    - a flush sandpit;
    - 2 cherry benches.
  - Plants in a clear box over the footprint and ring are removed. Columns at F-1/F-2 get soil under the floor.
  - It writes `data/guhs/vadsrijk_stadsleven/speeltuinen.txt` (town, corner, floor y).
  - It is idempotent: a town whose playground is already in the tiles is kept (marker blocks checked).
  - It runs inside `maak_kaart.py` after `overgang` and before the connection pass, so the fences come out connected. Run alone, it calls `verbind.na` on its own tiles.
- **Placed now (on the tiles of v-int 3349c1d52):**

  | Town | Corner (x, floor y, z) | Floor | Distance from centre |
  |---|---|---|---|
  | knuffelkamp | -391, 86, 996 | grass | 24 |
  | zwamzoet | -44, 63, 16 | knuffelgras lawn | 53 |
  | kandijkom | 697, 90, -1036 | parelmoer_tegels | 37 |
  | honingtak | 1350, 166, -557 | the honeycomb deck | - |
  | lampionhaven | 1806, 66, -468 | knuffelgras | - |
  | hoogvads | 2695, 220, -507 | the checkerboard plaza at the west end of the Marktlaan | - |

  - Hoogvads: the Staartpark is cut by park paths, so no 13x10 lawn fits; the search box is the Marktlaan/Universiteit/Staartpark side, not the cathedral/academy.
  - saffraanpoort: tiles are not in this tree. It is found automatically once zone 6 is in the map.
- **Java side (`Speeltuin`).**
  - The offsets mirror speeltuin.py.
  - Props: the seesaw plank (yaw 90, tilt up to 14 degrees), two swings (pivot F+4.45, chain 2.56, up to 30 degrees, each with its own phase) and the merry-go-round (2.6 degrees per tick).
  - Children:
    - 2 on the seesaw: the ends move opposite, the same formula as the render tilt;
    - 2 on the swings: their position is computed from the same angle as the prop;
    - 2 on the merry-go-round: at radius 0.95 at the same yaw as the disc, facing the pole;
    - 1 in the sandpit: sand particles;
    - 2 on the slide loop: walk to the ladder, climb, onto the tower, sit + slide, cheer (VAHOEG), again.
  - A grown-up sits on a bench.
  - VOL = 9 children + 1 grown-up. ZACHT = 5 children.
  - At night or in rain the children leave once nobody is within 20 blocks, and they come back by day the same way. The equipment props stay.
  - Giggles (`creche.babygiechel`), "Wiiiii!" at the bottom of the slide, and bubbles from the speel pool.

### Budget, LOD and no doubles (`StadsLeven`)
- **Planning:** every 10 ticks, and only for players in a town with Stadsleven not UIT. One try per kind per town per pass, up to the cap.
- **Radius:** a scene lives within its kind's radius of a player in that town:

  | Kinds | Radius (blocks) |
  |---|---|
  | critters | 34 |
  | birds | 36 |
  | cats, ducks, terraces | 44 |
  | walkers, picnic, tag, musician | 48 |
  | cart | 56 |
  | playground | 64 |

  A scene goes the moment no player (anywhere, Stadsleven on) is within radius + 16.
- **Dimension caps:**
  - 140 entities and 48 town guhs; above 48 guhs only animal scenes may still start;
  - a full town at VOL is roughly 55 entities, of which 20-25 guhs.
- **Client LOD (renderer `shouldRender`):**

  | Setting | Critters | Guhs, cart, playground |
  |---|---|---|
  | VOL | 48 | 80 |
  | ZACHT | 28 | 56 |
  | UIT | nothing | nothing |

- **Network:**
  - the props, guhs and cats use updateInterval 2 / 2 / 10 and tracking range 5 / 6 / 5 chunks;
  - `broadcastToPlayer` is false for players who set UIT, so nothing is even sent to them.
- **Client setting:** "Stadsleven: VOL / ZACHT / UIT".
  - It is stored in `config/guhs-vadsrijk-stadsleven.toml` and shown as a choice in the Instellingen tab of the Vadsrijk-menu (key H).
  - The client sends it to the server (`StadslevenPayloads.Stand`) on join and whenever it changes (checked once per second).
  - The server uses the richest setting of the players in a town for the caps.
- **Never saved, never twice:**
  - noSave types;
  - a join guard (EntityJoinLevelEvent, LOWEST) refuses every town-life entity the manager did not just spawn;
  - EntityLeaveLevelEvent: a ghost is discarded next tick;
  - a 5 s sweep discards everything no scene owns;
  - unique scenes have one key each (`speeltuin:<town>`, `kar:<town>`, `muzikant:<town>`, `terras@<bench>`);
  - spawning waits for `Dubbel.klaar` (u-dubbel);
  - `Dubbel.NIET_HEURISTISCH` excludes town life from u-dubbel's heuristic scan;
  - a scene that throws is removed and never takes the server down.

### Dev commands
`/guhs vadsrijk leven status | aan | uit | wis | niveau vol|zacht|uit|eigen | spawn <soort> | tp <stad> | speeltuin <stad>`.

## Decisions I made myself
- **Runtime spot scanning instead of generated spot data.** It works for every zone and zone 6 without a regen. The cost is about 8k block reads per chunk, once.
- **Playgrounds as a tile post-pass, not inside six generators:**
  - one 13x10 layout for every town (the bigger 17x12 one fit almost nowhere);
  - one per HUB town: Knuffelkamp, Zwamzoet, Kandijkom, Honingtak, Lampionhaven, Hoogvads and Saffraanpoort, which makes 7. Not De Haven and not Rozenkloof.
- **Speech bubbles = the guh's name tag** (custom name, shown for 2.5-4 s). No extra entities, a native background, vanilla distance culling.
- **Cats are vanilla cats** (Guhs has no cat model), with a pink collar.
- **The ice-cream cart is the existing `ijscoguh` model** (a guh on a bicycle cart with a parasol). Its wheels do not turn (the model has only an idle animation).
- **Night (time 12800-23200) and rain:** no new kids/picnics/cart, and the existing ones leave when nobody looks. Animals, terraces and the musician stay (an evening serenade). u-leven-ritme may later hook a proper day rhythm into `StadsLeven.nacht`.
- **Zone 4 islands are their own towns** (own caps), so a player on an island gets life there too.

## Shared-file changes (all marked `U-LEVEN-STAD`)
- **`VadsrijkFeature`:** one register line and one payloads line.
- **`client/VadsrijkClient`:** one init line.
- **`menu/client/InstellingenPaneel`:** one `Keuze` option, "stadsleven", in the picture column. It uses the existing `niveau.vol/zacht/uit` texts.
- **`tools/features/__init__.py`:** `vadsrijk_u_leven_stad` appended. It writes:
  - 142 lang keys in nl_nl/en_us;
  - `textures/vadsrijk/stadsleven/` (blauwe_klodder, sl_speeltoestel, sl_kleed);
  - `geckolib/{models,animations}/entity/vadsrijk/stadsleven/` (sl_wipwap, sl_schommel, sl_draaimolen, sl_kleed).
- **`tools/vadsrijk/maak_kaart.py`:** 5 lines. It runs `stadsleven/speeltuin.py` after `overgang` and before `verbind`, with `GUHS_VERBIND_LATER=1` so the main pass covers its tiles.
- **New data:** `src/main/resources/data/guhs/vadsrijk_stadsleven/speeltuinen.txt` (committed: it is code data, like the riders' routes.txt).
- **Tiles:** NOT committed (restored to 3349c1d52). I only stamped them locally to test the pass, so the playgrounds appear in the game only after the integration regen, or after running `speeltuin.py` on the integrated tiles.

## Integration (v-int)
1. Merge, then run the final regen as usual. `maak_kaart.py --zone alle` runs the playground pass by itself.
   - If zones 3/4/5/6 are rebuilt with their own `maakN.py` directly, run `python tools/vadsrijk/stadsleven/speeltuin.py` afterwards. It calls the connection pass on its tiles.
   - Zone 6: run it after the zone-6 tiles exist, so Saffraanpoort gets its playground. Then commit the new `speeltuinen.txt`.
2. On fresh tiles the pass searches again (deterministic, so the same spots come out for the same map). With the committed speeltuinen.txt and tiles that already have the playground, it keeps them.
3. **u-leven-ritme / u-geroezemoes** may want hooks:
   - `StadsLeven.nacht(sl)` is the single day/night switch;
   - `Steden.op(pos)` gives the town;
   - `Babbels` has the zone gossip pools (`vadsrijk.stadsleven.roddel.z<N>.<i>`) if the geroezemoes slice wants to reuse them.
4. **AutoCheck:** in the gametest/AutoCheck worlds the manager only runs in the Vadsrijk dimension. Players see life only in a town area.

## Tests
- `runGameTestServer -Pgt=Stadsleven` (`StadslevenGameTests`, 4 tests). A test town is built in the test level (lawn, pond, bench cluster, lantern, roof on posts, playground corner) and the test drives the manager (`StadsLeven.testModus`; `testNacht` / `testRegen`; `Steden.testStad` / `testDoos`).
  - **`budget`:**
    - 40 planning passes at VOL keep every kind under its cap;
    - at least 4 critters, including mice; exactly 1 playground; at most 1 cart and 1 musician; at least 1 terrace or chat;
    - the dimension caps hold, and every entity in the box belongs to a scene;
    - ZACHT stays under the halved caps; UIT gives nothing;
    - a player far away removes everything.
  - **`geenDubbelen`:**
    - a foreign prop or guh is refused by the join guard;
    - a second playground scene is refused;
    - a "reload" (scenes forgotten): the sweep removes exactly the orphans, and the next passes give exactly the scenes' entities.
  - **`speeltuin`:**
    - VOL = 9 children + 1 grown-up;
    - the seesaw ends move opposite, the swing swings at least 0.8 blocks both ways, the merry-go-round children stay on radius 0.95;
    - a slider climbs to F+3 and comes back down;
    - at night: 0 children, the props stay; ZACHT = 5 children.
  - **`stedenEnSpeeltuinen`:**
    - every area maps to its town;
    - all 6 zone 1-5 playgrounds are in speeltuinen.txt, and each lies in its town (checked where the area is registered).
- **Results:** `-Pgt=Stadsleven` **all passed**, 3 runs in a row; the tests wait until the forced test chunks are really entity-ticking, which avoids a first-run flake. `-Pgt=Dubbel,VadsrijkRuiters` also passes (join guard + heuristic exclusion live side by side).

## Open / for later
- **Looks are for the user in the dev client:**
  - the sitting height of guhs on benches (`+0.3`), on the seesaw/swings/merry-go-round (`+0.12` / `+0.02` / `1.19`) and of the parent on the bench;
  - the blob scale (0.5) and colour;
  - the cart scale (0.7);
  - the swing chain length;
  - speeds;
  - density (caps in `Groep.Soort`).
- The swing and seesaw rotation direction follows the derivation in the code comments (Y(180-yaw) then X(tilt)). If a seesaw kid floats opposite to the plank in the client, flip the sign of `dy` in `Speeltuin.tick`.
- The ice-cream cart wheels do not turn. A "rij" animation could be added to an own copy of the ijscoguh animation file.
- A De Haven playground or a Rozenkloof playground: add the town to `STEDEN` in speeltuin.py with its centre and box.
- No AutoCheck client run (RAM; and looks are judged by the user).
