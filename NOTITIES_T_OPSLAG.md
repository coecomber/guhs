# NOTITIES_T_OPSLAG — night round T, slice "opslag" (branch 110-t-opslag)

Virtual storage (VOORSTEL_ROADMAP §6.2 + decision R5) and the HUD additions (§6.1). Package
`nl.juiced.guhs.feature.vadsrijk.opslag` (+ `.client`), generator `tools/features/vadsrijk_t_opslag.py`.

## API for other slices (read this first)

### HUD (t-items: drankjesriem; anyone with HUD bits)
- `RijkHud.voegToe(String id, int volgorde, RijkHud.Element e)` — `Element.teken(GuiGraphicsExtractor g, Font f, int w, int h, float pt)`.
  Drawn after kern's HUD, only in the dimension with the HUD on, by `volgorde` (T-OPSLAG uses 100/110/120). Same id = replace.
- `RijkHud.plek(String id, int w, int h)` reserved spots (GUI coords, every GUI scale):
  - `"drankjesriem"` → `{x, y, vak, stap}` = 3 slots of 16x16 above the skill slots: slot i at (x + i*stap, y). **t-items draws the belt there.**
  - `"buffs"` → `{xLinks, y, xRechts, vak}` (T-OPSLAG's buff row), `"doelframe"`, `"gietbalk"`, `"baasgiet"` → `{x, y, breed, hoog}`.
- Drink cooldowns (or any icon) in the buff row: `OpslagHud.EXTRA.add(() -> List.of(new OpslagHud.ExtraIcoon(stack, restFractie, seconden, buffKant)))` (client).
- Cast bars (server): `Gietbalk.speler(p, naamKey, ticks, rgb)` / `stopSpeler(p, onderbroken)`; boss wind-up
  `Gietbalk.baas(baasEntity, naamKey, ticks, rgb, onderbreekbaar, straal)` / `stopBaas(...)`; or polled: `Gietbalk.BRONNEN.add(p -> kanaal-or-null)`.
- `RijkHud.BOVEN_EXTRA` (IntSupplier): extra pixels under the boss bar; `bovenVrij()` includes it (quest tracker moves down).

### Materials (t-ambacht and everyone who adds gathering/crafting items)
- A material = item tag `guhs:vr_materiaal`, OR `Materiaaltas.registreer(ItemLike...)`, OR `Materiaaltas.regel(Predicate<ItemStack>)`.
  Gear with `vr_uitrusting` never is. Kinds are item + components (crystal qualities stay apart).
- Picking up a material in the Vadsrijk puts it in the bag (even with a full inventory; respects pickup delay + personal-loot target).
- Giving: `Materiaaltas.geef(p, stack)` (bag when allowed, else inventory, else kern's found items).
- Using: **`Opslag.tel(p, item)` / `Opslag.neem(p, item, n)`** = inventory + bag (check-then-take, false = took nothing).
  Bag only: `Materiaaltas.tel(p, item|stack)`, `Materiaaltas.neem(p, item, n)`.
- Already hooked: economie `EcoUtil.tel/neem` (Smid etc.) and npc `Questlog.telItems/neemItems` (VERZAMEL quests) count the bag too.
- First materials (OpslagRegistry): Verbetersteentje, Herrolknabbel, Pluisdraad, every `Kristallen.KristalItem` (zone 2 + 3 crystals).

### Bank (zone-4/5 agents: Lampionhaven, Hoogvads)
- **One line:** `Bank.bankier("<npcId>")` — the NPC's talk menu gets "Naar mijn kluis" (QuestMotor.EXTRA). Or give the NPC `rol "bank"`.
- Current bankers (`Bank.STANDAARD`): `bankier` (Zwamzoet), `kok` (Knuffelkamp: his pantry), `havenmeester` (De Haven),
  `z2_veilingmeester` (Kandijkom, stands in the bank building), `z3_juwelier` (Honingtak).

### Knapzak / characters (t-karakter)
- `Opslag.KARAKTER` (Function<ServerPlayer,String>, default `p -> "1"`): set it to your character key; the knapzak is per key.
- **Call `Opslag.sluit(p)` before switching characters** (closes an open storage screen; cursor item goes back to the inventory).
- Bank + materiaaltas are account-wide (one per player UUID), in their own attachment `guhs:vr_t_opslag` (NOT inside SpelerData),
  so a SpelerData-per-slot swap leaves them alone.
- `Knapzak.ontgrendel(p, "<bron>")` = one more tab (quest/achievement reward; each id once). `Knapzak.BRONNEN` = built-in conditions.


## What was built
- `OpslagData` — ONE player attachment `guhs:vr_t_opslag` (copyOnDeath), saved in player.dat with the live inventory and
  kern's SpelerData → moving between inventory and storage is atomic. Containers are LIVE (`Vak`): the open menu edits them
  directly, no copy-on-open/write-on-close, so there is nothing to duplicate. Unreadable items (removed mods) stay as raw tags
  (`onleesbaar`) and are written back unchanged.
- `Knapzak` — per character (`Opslag.KARAKTER` key), tabs of 27, max 5: free + zone1_klaar + zone2_klaar + bought (2500 brons)
  + `ontgrendel(p, bron)` sources. Key **I** (`key.guhs.vr_knapzak`, category Vadsrijk), also a "▤" button left of the
  inventory screen in the dimension.
- `Bank` — account-wide, tabs of 54, max 5 (1 free; 10z / 50z / 2g / 5g). Closes when you walk > 8 blocks away.
  Bankers: see API above. `NpcRollen.koppel("bank", Bank::open)` too.
- `Materiaaltas` — account-wide, item+components → long count; auto-pickup (ItemEntityPickupEvent.Pre, respects pickup delay
  and personal-loot target, works with a full inventory, toggle in the screen); screen with search/scroll (`MateriaalMenu` +
  `MateriaalScherm`, reuses `storage.BankContents` for the sync), "✿" button left of the inventory.
- `OpslagMenu`/`OpslagScherm` — tabbed chest screen (flaps above, Ctrl+1..5), Sorteren, Materialen ➜ tas, Materiaaltas.
  Buttons go through vanilla `clickMenuButton` (server-checked). Locked flap shows how to unlock; buy = click twice.
- HUD (`HudBron` server, `OpslagHud`/`StatusIcoon` client): buff row (pixel-glyph icons, sweep + seconds, new-status name
  flash), target frame (look-at 24 blocks, else last hit 5 s; hidden for a boss with its bar), cast bars (own + boss,
  gold frame = interruptible). Losknuffelen (U) now shows its channel as a cast bar.
- Dev commands `/guhs vadsrijk opslag ...` (see SPIEK_T_OPSLAG.md), gametests `VadsrijkTOpslagGameTests` (batch vadsrijk_t_opslag).

## Decisions I made myself
- Storage only works inside the Vadsrijk with the Vadsrijk inventory live (`Opslag.magOpenen`): no smuggling between worlds.
- The bag holds only declared materials; quest items are NOT materials (so quests still see them in the inventory), but the
  Questlog VERZAMEL hook counts the bag anyway, for professions materials used in quests.
- No new NPCs for banks: existing NPCs in each hub get a "Naar mijn kluis" option (Knuffelkamp = the cook's pantry; De Haven =
  havenmeester; Honingtak = juwelier). A dedicated banker NPC per town can be added later with one line.
- Knapzak tab price 25 zilver; bank tab prices 10z/50z/2g/5g — tunable (`Knapzak.PRIJS`, `Bank.PRIJZEN`), not balanced yet.
- Kern's Duizelig/Bevroren text lines stay; the buff row keeps the centre free (max 3 icons per side, then a 2nd row up).
- No sorting of the materiaaltas by category / no "mark junk" / no loadouts (§6.2 extras) — not in this brief.

## Shared-file changes (all marked `// T-OPSLAG`, add-only)
- `VadsrijkFeature` (+2 lines), `client/VadsrijkClient` (+1), `tools/features/__init__.py` (+1 FEATURES entry).
- `kern/client/RijkHud`: HUD API (`Element`, `voegToe`, `plek`, `BOVEN_EXTRA`), the loop at the end of `teken`, `bovenVrij` + extra.
- `kern/veilig/Losknuffelen`: `public static long kanaalBegin(UUID)` (read-only getter).
- `economie/EcoUtil.tel/neem`: + the materiaaltas (`Opslag.tasVoor` / `neemRestUitTas`).
- `npc/Questlog.telItems/neemItems`: + the materiaaltas.
- lang nl_nl/en_us (+57 keys `vadsrijk.opslag.*`, `key.guhs.vr_knapzak`), new `data/guhs/tags/item/vr_materiaal.json`.

## Keys
- I = knapzak (free before; taken list was R V G Z C U K J X N P B).

## Open points / resume
- Not tested in a client yet (no AutoCheck run by me): screens layout at GUI scale 2-4, flap clicks, the inventory side buttons
  (they don't follow the recipe-book shift), HUD positions vs. the held-item name.
- A boss cast bar is only an API: no boss calls `Gietbalk.baas(...)` yet (bazen/vfx agents can add it to their wind-ups).
- Affixes are not synced to the client, so the target frame shows element + Elite, not the affix list.
- t-karakter: set `Opslag.KARAKTER` and call `Opslag.sluit(p)` before a switch (see API).
- t-items: draw the drankjesriem at `RijkHud.plek("drankjesriem", w, h)` via `RijkHud.voegToe`; drink cooldowns can go in
  the buff row via `OpslagHud.EXTRA`.

## Tests
- `runGameTestServer -Pgt=VadsrijkTOpslag`: 5/5 pass (geenDupeInHetScherm, wisselMetSchermOpen, bewarenEnLaden, materiaaltas,
  tabsPerKarakter). Found + fixed on the way: vanilla `Inventory.add` deletes a stack that does not fit for a CREATIVE player →
  storage now uses its own `Opslag.inInventaris` (never eats a stack).
- No AutoCheck/client run done by me (not visually checked).
