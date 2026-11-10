# NOTITIES_Y_COSMETICA (round Y, 2026-11-10)

Branch `110-y-cosmetica` from 65cd32467 (110-x-int). Cheat sheet: `SPIEK_Y_COSMETICA.md`. Lang chunk: c465.

Goal (user decision): Vadsrijk outfits can be worn as pure cosmetics in survival. Looks only: no stats, no armour value, no items
crossing.

## 1. What already existed (investigated first)
- **Vadsrijk, `feature/vadsrijk/kleding/` (T-KLEDING, round T)**: a full account-wide wardrobe, the Kledingkast.
  - `KledingData` is the unlock record per player UUID: overworld SavedData `guhs:vr_kleding`, with `heeft` (unlock ids), `keuze`,
    `looks` and `kills`. No items. `KarakterRegistry` keeps it account-wide, not per character slot.
  - `Cosmetica` is the catalogue of ~60 pieces: wings (10 + raid wings), capes (8, incl. the **Knuffelorde cape** for the zone-1
    story), head accessories (7), dyes, name colours, cheer-up effects, trails and auras. Each piece has a source (`Bronnen`) and a zone.
  - `Kledingkast.uiterlijk` holds only the chosen looks. It goes to every client on change and on login (payload `Uiterlijk`),
    keyed by UUID.
  - `KledingLaag` (render layer) draws wings, the cosmetic cape and the head accessory on every player in **every dimension**
    already. Transmog (armour looks) only draws in the Vadsrijk.
  - Key **O** (`key.guhs.vr_kledingkast`) worked at home already, and so did `/guhs vadsrijk kledingkast`, the hub tailors and the
    collection tab.
  - Wing tricks, gliding and the soft landing are server-checked with `Vadsrijk.in` (`Zweven.mag`, `Zweven.val`), and so is the
    client trick.
- **Mount looks** ("garderobe" in `rijdier/Aankleden`, the economie account tag): fur, pattern and harness for Vadsrijk mounts.
  Mounts never leave the Vadsrijk, so they are out of scope and unchanged.
- **Survival**: no player cosmetic system. `feature/kleding` + `GuhWardrobeScreen` is clothing for guhs (pets/NPCs), and the
  titles feature (`Titels`) is the only player cosmetic. No general Guhs menu exists outside the Vadsrijk (F9/F10 are
  Erfenis-specific), so the opening ways are the O key + a command.

So the bridge mostly existed. This slice closes the gaps and leaks and makes the home side clear.

## 2. What I changed
- **`/guhs garderobe`** (any player, no permission): opens the wardrobe anywhere (`Garderobe.commando`).
- **At home the screen is "Garderobe"**:
  - The title changes (the Vadsrijk keeps "Kledingkast").
  - There is no "Harnas" (transmog) tab, because that only dresses Vadsrijk gear.
  - Each list starts with one line: "Thuis is je Vadsrijk-kleding alleen voor de looks: geen krachten, geen harnas.", or, with
    nothing unlocked, "Nog niks hier! Kleding verdien je in het Vadsrijk."
- **Unlocks only from the Vadsrijk**: `Kledingkast.open` used to run the source check + armour-look collection wherever it was
  opened. Now it only does that in the Vadsrijk (`open(p, via, binnen)`). At home it only shows, so not even the starters unlock
  from survival. The 10 s check pass was already Vadsrijk-only.
- **No survival -> Vadsrijk leak**: only Vadsrijk armour (`guhs:vr_*`) ever becomes a transmog look (`Kledingkast.vadsrijkHarnas`
  in `onthoud`). Before, opening the wardrobe at home stored your diamond helmet as a Vadsrijk look.
- **Zone gate, no spoilers**: a LOCKED piece whose zone is closed is not listed at all (no "???" row, no hint). A piece you own
  always shows. A soft line closes the list instead: "Meer kleding vind je later, in nieuwe delen van het Vadsrijk."
  - Zone = max(the piece's zone, the zone in its source). Puzzle rewards follow their puzzle file (z1..z3).
  - The secrets "zweefvlucht" and "kleermakers" count as zone 3.
  - The gate is `ZonePoort.open()` itself, so ops and creative get no spoilers either. It is sent as `poort` in the screen data.
- **Elytra**: with a real elytra on (survival), the cosmetic wings are not drawn through it.

### Pieces you can wear at home (all from the Vadsrijk, choose per slot, each slot can be set to "niets")
- **Rug**: wings, as looks only (their trick works only in the Vadsrijk) or a cape, e.g. the Knuffelorde cape (zone-1 story),
  the Roze cape (zone-1 book 50%), the Reiscape (secret)...
- **Hoofd**: Guh-oortjes (first visit), Strikje, Paddenhoedje, Feestmuts (level 20)...
- **Verf** (tints wings + head accessory), **Naamkleur**, **Titel**, and the effects **Opvrolijken** / **Spoor** / **Aura**
  (client particles, same sliders).

At gate 1 a new player can earn, all in zone 1 (or general): Knuffelorde cape, Roze cape, Guh-oortjes, Strikje, Paddenhoedje,
Feestmuts, roze name colour, the dyes, Confetti/Regenboog/Giechel, bloemblaadjes trail, rozegloed aura and Sterrenstof wings
(zone-1 puzzle, looks only at home).

## 3. Decisions made alone
- **No new catalogue**: the existing T-KLEDING set already is the "outfit" concept. Wings, capes and hats draw on a player outside
  the Vadsrijk, and the Knuffelorde cape is the order tabard (zone-1 story). I kept that rather than inventing a second system.
- **Transmog stays Vadsrijk-only** (it dresses Vadsrijk gear; at home your armour is your armour), hence no armour tab at home.
- **Name colour + effects** count as looks and show at home too (they already did). Movement tricks never do.
- **The zone filter also applies in the Vadsrijk list** (same screen). That is a no-spoiler fix, not a gear change.
- The key stays **O** in the "Vadsrijk" key category (an existing key, no rebinding for players).

## 4. Shared-file changes (all marked `// Y-COSMETICA`)
- `feature/vadsrijk/kleding/Kledingkast.java`: `open(p, via, binnen)` overload (check/collect only inside), `vadsrijkHarnas`
  filter in `onthoud`, `poort` in `schermData`.
- `feature/vadsrijk/kleding/KledingRegistry.java`: one listener line for `Garderobe::commando`.
- `feature/vadsrijk/kleding/client/KledingScherm.java`: title / tabs at home, hint line, zone filter + "meer" line.
- `feature/vadsrijk/kleding/client/KledingLaag.java`: no wings through an elytra.
- `tools/features/vadsrijk_t_kleding.py`: 4 texts `garderobe.*` (NL + EN).
- `assets/guhs/lang/nl_nl.json` + `en_us.json`: 4 keys `vadsrijk.kleding.garderobe.*` (inserted after `harnas.thuis`).
- `tools/lang/chunks.json`: chunk `c465_vr_y_cosmetica` (prefix `vadsrijk.kleding.garderobe`) + `tools/lang/en/c465_vr_y_cosmetica.json`.
- New files: `kleding/Garderobe.java`, `kleding/VadsrijkYCosmeticaGameTests.java`.

## 5. Tests
`runGameTestServer -Pgt=VadsrijkYCosmetica,VadsrijkTKleding`. Batch `vadsrijk_y_cosmetica`, 6 tests, plus the 8 existing T-KLEDING tests:
- `ontgrendelAlleenInRijk`: at home opening unlocks nothing. In the Vadsrijk the zone-1 flag gives the Knuffelorde cape + the
  starters. They are stored in the account store, and no item is handed out.
- `thuisAlleenUiterlijk`: everything unlocked and worn. Every attribute and the armour value are unchanged, and the inventory and
  equipment are unchanged. At home there is no glide, a glide claim is ignored, and the soft-landing wings do not cancel fall damage.
- `syncData`: others get the cape/hat/dye but no unlock list. Taking a piece off removes it for them too. Checks the payload and
  that the screen data has `binnen=false` + `poort`.
- `geenOverlevingsHarnas`: a diamond helmet never becomes a look (not at home, not inside). A Vadsrijk helmet does, but only inside.
- `zonepoort`: zone mapping (story/book/puzzle/secret/general), gate 1 hides the closed zones' locked pieces (incl. the raid
  wings), owned pieces always show, gate 6 shows everything.
- `commando`: `/guhs garderobe` exists and needs no permission.

The gametest level is the overworld, so "home" is real. "Inside" is the `binnen=true` branch of `open`. The real dimension
teleport was not used (it swaps inventories). Last run 2026-11-10: "All 15 required tests passed" (6 Y-COSMETICA + 8 T-KLEDING + 1 matched by the filter).

## 6. Open / for integration
- Not checked in a client (no client runs this round). Integration should check:
  - the Garderobe title + 8 tabs at home;
  - another player sees your cape/wings at home;
  - wings are hidden while wearing an elytra.
- y-scheiding (c455) may audit the same channels. The wardrobe channel is covered here: unlocks only inside, looks only outside,
  no survival armour inside.
- Mount looks (`rijdier/Aankleden`) are unchanged and stay in the Vadsrijk.
- Old saves: transmog looks that were already collected from survival armour at home stay in the store (harmless: transmog only
  draws over Vadsrijk gear inside). `/guhs vadsrijk kleding wis` clears a test account.
