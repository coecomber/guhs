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

(The rest of this file — what was built, decisions, shared-file changes, tests, open points — follows below.)
