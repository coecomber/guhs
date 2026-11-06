# NOTITIES_U_HUIZEN (round U, branch 110-u-huizen, from eaee06d08 = 110-t-int)

Decision V2 (FEEDBACK_RONDE4): player housing as an FF14-style neighbourhood with plots, as an instance. User-facing test steps:
`SPIEK_U_HUIZEN.md`.

## What was built (package `feature/vadsrijk/huizen`, registry `HuizenRegistry`)
- **Dimension `guhs:knuffelkwartier`** (`tools/features/vadsrijk_u_huizen.py` writes dimension + type + biome):
  - a flat generator: grass top at y 63, pink grass (same tint as Knuffeldal);
  - no mobs, no features, day/night like the overworld, beds may be slept in but never set the spawn.
  - The flat ground costs nothing; a ward only writes its streets, square, signs and houses.
- **Wards (`WijkPlan`, `WijkBouw`):**
  - Ward `w` is centred at (w * 1024, 64, 0). Square |x|,|z| <= 12:
    - fountain;
    - 3 market stalls with Meubelmeester Mollie;
    - benches;
    - the notice board;
    - the pink gate back (walk through it);
    - lamp posts.
  - Four streets (E/S/W/N), each with 6 plots of 20x20 (3 per side), 24 per ward, plus sidewalks, lamp posts between the plots,
    street name signs, a hedge at the end and 4 parks.
  - Wards are built on first need. `HuizenData.eersteVrije()/wijkMetPlek()`: when every ward is full, the next one is built.
- **Houses (`Ontwerp`):** generated in code in the plot's canonical frame (street NORTH) and turned per plot (`BlockState.rotate`).
  - Sizes (`HuisSoort`):
    - huisje 7x7, 1 floor;
    - huis 9x9, 2 floors + ladder;
    - villa 13x10, 2 floors + ladder.
  - Looks (`Ontwerp.PALETTEN`):
    - Snoepje (cherry, ridge across);
    - Muntje (calcite/warped, gable to the street with a round window);
    - Lavendel (quartz/purpur, porch with log pillars + hanging lantern, brick chimney with a lit campfire).
  - Every house has:
    - a fence round the plot with a gate and 2 lamp posts;
    - a flat path to the door;
    - floors, ceilings and hanging lanterns;
    - the trophy shelf (slabs on the back wall);
    - a garden with flowers and a cherry tree;
    - the stable corner (hay, posts, side rail, water troughs, lantern);
    - the mailbox (fence post + barrel).
  - **Connected blocks:** every written block goes through `Block.updateFromNeighbourShapes` at the end (`Pen.verbind`,
    `Schrijver.verbind`). Fences, panes, gates and stairs get their real states. The test checks: no lone fence at floor level, no
    lone pane. u-hekken's `verbind.py` is for the Python map tiles; housing is built in Java, so it uses Minecraft's own shape
    update (the same result).
- **One house per ACCOUNT** (the Minecraft player UUID, shared by all characters; confirmed by the user via the lead).
  - Storage: the house itself in `HuizenData` keyed by the account UUID; on the player side `Karakter.account(p, "huizen")`
    (account-wide, never switched) holds `kavel` (mirror of the plot key) and `trofeeen` (every boss any character beat).
  - The trophies of all characters are merged into that account list on login, on a character switch (`Karakter.VOOR_WISSEL`)
    and while the owner is home; decoration is per house, so it is shared by all characters anyway.
  - A house is a place, not a character stat.
  - Coins and the level requirement come from the character you play when you buy or upgrade.
  - Rested xp is per character (the character you log in with).
- **Inventory: the Knuffelkwartier counts as HOME, not as the Vadsrijk.**
  - Kern's reconciler swaps to the home inventory there, and back when you return.
  - So you furnish with your Guhs deco from home, and the Vadsrijk Post (cosmetics) can be collected at the mailbox.
  - Nothing of the Vadsrijk bag can leave the Vadsrijk.
  - Until the swap is done (<= 1 s after arriving) nothing can be placed, broken or clicked (`Regels.nogNietGewisseld`).
  - Zero kern changes were needed for this.
- **Permissions (`Rechten`, `Regels`):**
  - Eigenaar: everything inside the own fence.
  - Vriend:
    - comes in unless the door is "op slot";
    - decorates only with "Vrienden mogen inrichten".
  - Bezoeker: looks; comes in only when the door is "open voor iedereen".
  - Doors, trapdoors and gates of a house you may not enter don't open. Anyone inside a house they may not be in is walked to the
    gate (checked every 10 ticks).
  - Building:
    - only inside the fence, from the floor up to 18 blocks;
    - up to the deco limit (150/300/500);
    - only blocks that were placed can be broken (`Huis.geplaatst`, also both halves of doors/beds).
  - Refused, to keep the rules no-dupe and no-grief:
    - blocks: block entities with an inventory (Container), pistons, TNT, falling blocks, liquids;
    - items: buckets, ender pearls, spawn eggs, boats, minecarts, armour stands, frames/paintings, flint and steel, fire charges,
      end crystals;
    - also axe/shovel/hoe tool modifications, bone meal outside your own plot, and any attack.
  - Outside the plots nothing changes. Creative players are never stopped.
- **Menus (`Menus`, the Vadsrijk talking screen via sleutel `vr_kk`):**
  - Kwartiermeester (hubs): naar mijn huis / naar de buurt / bezoek (friends' houses + houses of online players) / uitleg.
  - "Te koop" sign: Makelaar Muis, size → look → bought (pay, claim, build, teleport to the gate, hearts).
  - Mailbox, for the owner:
    - post (Post.bezorg + the account's post box for deco out of a house);
    - door (open → vrienden → slot, which closes the front door);
    - vrienden (players within 10 blocks become friends, inrichten on/off, all away);
    - groter (pay the difference);
    - ander uiterlijk;
    - verkopen (half back);
    - terug naar de stad.
  - Mailbox, for others: whose house, door, your role.
  - Notice board: who lives where, plots free, number of wards.
  - Mollie: 17 wares (Guhs chair/table/bench/beanbag/cushion/flags/cake/flowers, lanterns, carpet, bed, pots, planks, panes,
    bookshelves, Vadsrijk banner) for Vadsmunten, straight into the (home) inventory, 6 per page.
- **Way in (`Poorten`):** a Kwartiermeester NPC guh in every hub.
  - Hubs: Zwamzoet z1 `plein`, Kandijkom z2 `plein`, Honingtak z3 `plein`, Lampionhaven z4 `plein`, Hoogvads z5 `markt_fontein`.
  - The spot is found at runtime in a ring of 6-13 blocks round the square (standable, lit, not in water, >= 3 from other NPCs),
    like t-ambacht's workshop guhs.
  - It is then handed to the npc slice's keeper (`Npcs.voegToe`). Clicks: `QuestMotor.KLIK_VOOR`.
  - The way back: where you clicked the Kwartiermeester (`HuizenData.terug`, persisted); else Zwamzoet; else the respawn point.
- **Figures (`Figuranten`):** only while a player is within 48 blocks of a plot, removed beyond 72; never saved (dropped on load
  from disk). Per house:
  - the owner's active mount in the stable (no AI, invulnerable, not rideable);
  - up to 3 pets roaming the garden (the real pet entity, driven by `tuinTick` through a new hook);
  - one figure + name per boss on the trophy shelf: the mini Koning Klodder / Opperzwam / Klodderreus deco items, else a golden
    totem;
  - plus Mollie per ward (within 64 of the square).

  What the owner has is copied into the house while the owner is home (`bijwerkenVan`), so visitors see it too.
- **Rested xp (`RustXp`):**
  - Log out inside your own house (the house box, not the garden): `thuisUit[uuid] = time`.
  - On login: +5% of a level per hour, at most 1.5 levels, in `SpelerData.extra("huizen").rust` of the character you log in with.
  - Every xp gain is doubled from that pool (`Spelers.XP_EXTRA`).
- **Persistence:** `HuizenData` (SavedData `guhs:vr_huizen`, overworld) holds the wards, the houses (owner, name, size, look, door,
  friends, inrichten, placed blocks, trophies, mount/pets, paid), account → plot, the way back, logged-out-home time and the post
  box. The blocks themselves live in the dimension's region files.
- **Server-friendly:**
  - nothing force-loads chunks; the figures, the keeper and the gate shimmer only run near players;
  - a ward is built once, synchronously (~150 flat chunks get generated, then about 10k block writes);
  - a house is about 11k block writes (clearing 20x20x27 + the template).

## Shared-file changes (all add-only, marked U-HUIZEN)
- `kern/Spelers.java`: `XP_EXTRA` (`List<ToLongBiFunction<ServerPlayer, Long>>`), applied in `geefXp` after the factors.
- `huisdieren/HuisdierEntity.java`: `ZELFSTANDIG` (`List<Predicate<HuisdierEntity>>`), asked first in `tick()` (true = handled).
- `VadsrijkFeature.java`: `HuizenRegistry.register(modBus)`.
- (uses, no change) `Karakter.account(p, "huizen")` + `Karakter.VOOR_WISSEL`.
- `tools/features/__init__.py`: `vadsrijk_u_huizen` (before `vadsrijk_t_vfx`, which must stay last).
- lang nl_nl/en_us: 117 keys `vadsrijk.huizen.*` + `vadsrijk.npc.naam.kk_poort|kk_mollie` + Mollie's idle lines.

## Decisions I made myself
- Per ACCOUNT (one house, all characters live there); rested xp per character.
- The Knuffelkwartier uses the HOME inventory (see above); deco therefore comes from home, from Mollie (coins) and from the Post.
- Prices and levels:
  - huisje 15z / lv 10;
  - huis 60z / lv 25;
  - villa 2.5g / lv 45.
- Upgrade = pay the difference. Selling gives half of what was paid. Changing the look is free.
- Plots are all the same size (20x20); the house size decides the footprint.
- No containers in houses (storage = the bank). Moving/rebuilding a house never loses deco (it waits in the post box).
- Visitors may still use blocks that are not doors (sit on a bench); they can never place or break.

## Tests
- `runGameTestServer -Pgt=VadsrijkUHuizen` (7 tests):
  - kavelsPassen;
  - huizenBouwenVerbonden (3 sizes x 3 looks x 3 street directions, door facing, no lone fences/panes, path, floor);
  - rechten;
  - opslaanEnLaden;
  - nieuweWijkAlsVol;
  - opruimenZonderDupes;
  - rustXpThuis.
- The test server has no datapack dimensions, so the dimension itself, the menus, the hub NPCs and the figures are not covered by
  gametests (see SPIEK).

## Open / not done
- Not looked at in a client yet: the look of the houses and the ward, the figures, the menus.
- The house interior is empty on purpose (owner furnishes). There is no starter furniture.
- Lamp posts are vanilla (cherry fence + lantern). When u-deco's guhlantaarnpaal block lands, `WijkBouw.Schrijver.lantaarnpaal`
  is the one place to swap.
- Items and blocks:
  - entity deco (armour stands, item frames, paintings) is refused, because hanging entities have no "placed by" record yet;
  - t-ambacht's professions deco are mostly Post items (vaandel, beeldjes): they work.
- `/guhs vadsrijk` (kern's enter/leave) used from inside the Knuffelkwartier enters the Vadsrijk and remembers the Knuffelkwartier as
  "terug". Harmless, but the gate is the intended way.
- No website/wiki entry yet (blokuitleg: the housing has no new blocks).
