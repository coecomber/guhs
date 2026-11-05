# NOTITIES T-ITEMS: itemisatie 2.0 (night round T)

Branch `110-t-items` (worktree `guhs_vadsrijk/t-items`, from dbf82899d). Source: VOORSTEL_ROADMAP §2.1/2.3/2.4/2.5 + decisions R2-R5.
Code: `feature/vadsrijk/items/` (+ `items/client/`). Generator: `tools/features/vadsrijk_t_items.py` (one line in FEATURES, after vadsrijk_kern).
Tests: `VadsrijkItemsGameTests` (`-Pgt=VadsrijkItems`), plus the updated kern test `sieraadShiftKlik` (`-Pgt=VadsrijkKern`).

## What was built
1. **New slots** (`kern/loot/Slot`): RING2, ARMBAND, HANDSCHOENEN, CAPE, all virtual like the jewellery
   (`SpelerData.sieraden`, never in the live inventory, so the inventory swap can't touch them).
   - `Slot.SIERADEN` = Ring, Ring 2, Ketting, Oorbel, Armband, Handschoenen, Cape (screen order).
   - `Slot.past()`: a ring (base type slot RING) fits RING and RING2.
   - `Sieraden.doelSlot()`: a 2nd ring fills RING2 first; with both full it swaps with RING.
   - The inventory column (kern `SieradenScherm`) now draws 7 slots, with short labels `vadsrijk.kern.slot.<x>.kort` (R, R2, K, O, A, H, C).
   - The cape LOOK is not ours: t-kleding's wardrobe draws wings/cosmetic capes over it. Gloves/capes are icons only.
   - **NO Knuffeltalisman slot** (R4). The talisman is a consumable (see 4).
2. **17 new base types** (`ItemsRegistry.STUKKEN`, items `guhs:vr_t_<id>`), zone 1-3:
   - handschoenen: pluiswantjes, kloddergrijpers (Klodderset), ijswantjes, kristalhandschoenen (Kristalset), bloesemhandjes (Bloesemset), schorshandschoenen (Schorsset);
   - capes: knuffelcapeje, sporencape (Zwamset), galmcape, buitcape (Mika-buitset), wortelcape (Wortelset), schemercape;
   - armbanden: pluisarmband, rozenarmbandje, kristalarmband, honingarmband, harsarmband.
   - Growth per level like zone 2 (main stats 25 % of the base, side stats 6 %).
   - **Sets:** a set glove/cape counts as one set piece (`items.Sets.extraStukken`), the count is capped at 4. So the glove stands in for a missing piece; the 2/4 thresholds are unchanged.
   - **Drops:** at `ServerAboutToStart` every table with prefix `wezens:`, `kern:`, `economie:baas_` (zone 1), `z2wezens:` (zone 2) and `z3wezens:` (zone 3, incl. contracts) gets:
     - the loose pieces of its zone (weight 1);
     - a table that drops a set piece also gets that set's glove/cape, at the same weight.
     - Weapon tables (`klassen:wapens`, `z*systemen:wapens`) are left alone.
3. **The 4 new stats** (`kern/Stat`, at the end of the enum), combat in `items/NieuweStats`:
   - **Verdediging negeren %**: creatures have no armour number, so it takes back that share of everything that makes your hit smaller on a creature:
     - a weak element match (x0.7 → closer to x1);
     - VOOR_KLAP reductions (blocks/shields; never a full dodge or reflect, n=0 stays 0);
     - the share that goes through a creature's Knuffelschild.
     - Max 50 %.
   - **Schade tegen bazen %**: x(1+%) on `VadsrijkBaas`.
   - **Leven per treffer** (flat Vads): every damaging hit (not DoT ticks), at most 4 heals per tick (area skills).
   - **Kans op effect %**: the hit's element sets its status: Vuur → Brandend 3 s (10 %/s of the hit), Natuur → Gesporen 3 s (8 %), Water → Plakkerig 2 s, Roze → Verdoofd 0.5 s (not on bosses). Max 40 %.
   - **Slot pools** (`Bonussen.ALLEEN_OP`; the old stats still roll everywhere):
     - Negeren: weapon / gloves / ring / armband;
     - Bazen: weapon / ring / ketting / cape;
     - Leven: weapon / gloves / armband / oorbel;
     - Effect: weapon / gloves / ketting / oorbel / armband.
     - The armband has double weight on Aanvalssnelheid and kortere wachttijd.
   - **Clear names (R4)**, also for the old stats, via the generator. No class resources, no perfect dodge.
4. **Upgrade safety nets** (`items/Vangnet`, the smid's server side `items/SmidExtra`):
   - **Pity:** +5 % per failure in a row on that piece (`Erfstuk.pity`), only for attempts below star 7; reset on success. A meter in the screen.
   - **Knuffeltalisman** (`guhs:vr_t_knuffeltalisman`, consumable):
     - with the switch on (in the star tab) a failure that would cost a star costs nothing, and only then one talisman is used;
     - verpluisd still happens (from +6, as before);
     - sources: bosses 20 % per helper, elites 3 %, and buy it at the smid for 150 + 15×lv brons (a coin sink).
   - **Polijsten** (tab Bonussen): pick a bonus, it gets a new value within its own range (rarity-biased like rolling, so it can also go down). Cost 25 + 6×lv brons.
   - **Previews:**
     - the chance incl. pity;
     - the risk;
     - "Als het lukt:" with the stat/damage changes of the next star;
     - growing shows old→new level and stats;
     - overvadsen shows the value and the chance of the sweet miss;
     - polijsten shows the range.
   - **NO Sterrenvangen** (R2).
5. **Erfstukken** (`items/Erfstukken`, data in `kern/loot/Erfstuk`, a field `erf` on `Uitrusting`, left out of the saved data while empty):
   - **Vertrouwd:**
     - hand weapon, the 4 armour pieces and the 7 virtual slots get item xp per cheered-up creature (1 / elite 3 / boss 15);
     - levels 1-5 at 40/120/280/560/1000 xp;
     - +2 % base stats AND weapon damage per level (`Rollen.ERF_FACTOR`);
     - a chat line on each level, gold at 5.
   - **Doorgroeien** (tab Groeien):
     - only Legendarisch (max 2×) and Guhddelijk (unlimited up to lv 50), needs Vertrouwd 5;
     - +10 item levels; rolls move to 95 % of their place in the new range;
     - the piece becomes Gebonden, Vertrouwd starts over;
     - cost: Groeipluis (1 + times grown; every boss gives each helper 1) + 400 brons per level (Guhddelijk ×2).
   - **Overvadsen** (tab Overvadsen):
     - pick an old piece, one of its bonuses, and the target bonus it replaces (or "add" when the rarity has room);
     - the value lands at the same place in the target's range; 15 % "lieve mislukking" = 70 % of the place;
     - the old piece is used up; the target is never lost; no double stats; slot pools respected;
     - cost 300 + 20×lv brons.
6. **Drankjesriem** (`items/Drankjesriem`, data `SpelerData.extra("t_items").riem`):
   - 3 slots, only drinkable items (consumable with the DRINK animation);
   - keys **Y / I / O** (free; documented, rebindable);
   - HUD: 3 slots left of the hotbar with key + cooldown sweep;
   - inventory: column "Riem" right of the jewellery column (click with a potion on the cursor = in / swap, click empty = out, shift-click a potion in the bag = in);
   - drinking from the belt is instant but uses the item's own (shared) potion cooldown; Vadsrijk only.
7. **Slot budget** (balance): everything a piece in a virtual slot gives is multiplied by its slot budget (`Rollen.SLOT_BUDGET`, also shown that way in the tooltip and the smid; the bonuses are stored unscaled, so reroll/polish/overvads work on one scale).

## Balans (all in `config/guhs-vadsrijk-balans.toml` section `[items]`, live: `/guhs vadsrijk balans items <sleutel> <waarde>`)
| sleutel | standaard | waarom |
|---|---|---|
| budget_sieraad | 0.70 | ring, ring 2, ketting, oorbel, armband |
| budget_handschoenen / budget_cape | 0.60 | they also carry Verdediging/Vads base |
| pity_per_mislukking | 0.05 | roadmap |
| pity_tot_ster | 7 | roadmap: above 7 it stays exciting |
| talisman_prijs | 150 | + 15 × lv brons |
| polijst_prijs | 25 | + 6 × lv brons |
| vertrouwd_per_level | 0.02 | roadmap (+10 % at 5) |
| vertrouwd_xp_schaal | 1.0 | |
| doorgroei_levels | 10 | one zone tier |
| doorgroei_roll | 0.95 | roadmap: just under the best new Legendaries |
| doorgroei_prijs | 400 | brons per new level |
| overvads_mislukking | 0.15 | |
| overvads_prijs | 300 | + 20 × lv brons |
| negeren_max / effect_max | 50 / 40 | caps |
| leven_per_tik | 4 | |

**Budget reasoning:**
- Before: 8 pieces (weapon, 4 armour, 3 jewellery) = 8 "units".
- Now: 5 + 5×0.7 + 2×0.6 = 9.7 units with everything filled (+21 % at full gear, of which the 2nd ring/armband/gloves/cape are the reward).
- The 3 old jewellery slots alone drop from 3.0 to 2.1 units, so a player without the new pieces is ~10 % below the old totals in side stats.
- Rolled bonuses per piece: unchanged ranges; the 4 new stats only on their slots, so the existing pools are hardly diluted (weights 4 against a pool sum of ~96).

**Not done here (by choice):**
- the class multipliers `[knuffelridder]` etc. are untouched;
- iNv ("Kracht 142") is not built (not in the brief's list).

## Decisions I made myself
- **Polijsten = re-roll within the range** (as the brief says), not "+10 % per time" (roadmap). It can go down. It's cheap, and herrollen/polijsten share the "Bonussen" tab.
- **Talisman:** used only when it actually saved a star (sweeter than Maple). Not Gebonden (items can't carry that), but it's sold only by the smid for munten.
- **Drinking from the belt is instant** (no 2 s drink): the shared 10 s potion cooldown keeps it fair.
- **Verdediging negeren** works on what creatures DO have (element penalty, blocks/shields, Knuffelschild), because creatures have no Verdediging.
- **Schade tegen bazen** counts only bosses (the name says bazen), not elites.
- **Sets:** a glove/cape is a stand-in piece (count capped at 4), not a 5th/6th piece with new bonuses.
- **Item xp** also goes to verpluisd-free pieces only; Vertrouwd is lost on doorgroeien (you bond again).
- **The smid's tab state** is client-side; the preview uses the client's default balance values (if the server file changes the numbers, the preview may differ a little; the server is the truth).

## Shared-file changes (all marked `// T-ITEMS`)
- `kern/Stat.java`: +4 enum values at the end.
- `kern/loot/Slot.java`: +4 slots, SIERADEN (7), `past()`.
- `kern/loot/Erfstuk.java` (new, data only); `kern/loot/Uitrusting.java`: field `erf` (optional in the codec), 11-arg constructor kept, `metErf`, `metLevel`.
- `kern/loot/Bonussen.java`: 4 pool entries, `ALLEEN_OP` slot limits, `mag()`, a slot-aware `rol(…, slot)`, `waarde()`, `rond()` (old `rol` unchanged = old stats only).
- `kern/loot/Rollen.java`: hooks `SLOT_BUDGET`, `ERF_FACTOR`, `budget()`; `rol` passes the slot; `stats()`/`wapenSchade()` apply them.
- `kern/loot/RijkItem.java`: tooltip values × budget × Vertrouwd; `regel` public.
- `kern/Sieraden.java`: `doelSlot()` (RING2); `zet`, `afZonderCheck`, `wisselMetInventaris` public.
- `kern/Spelers.java`: virtual slots use `slot.past()`.
- `kern/Schade.java`: 4 small hooks (negeren on the element factor / VOOR_KLAP / Knuffelschild, Schade tegen bazen via `NieuweStats.naAanval`).
- `kern/client/SieradenScherm.java`: 7 slots, short labels, list size from SIERADEN.
- `kern/VadsrijkKernGameTests.java`: `sieraadShiftKlik` updated (the 2nd ring now goes to RING2).
- `economie/Smid.java`: pity/talisman around the star roll; unknown actions → `SmidExtra.doe`; extra screen data; herrol uses the slot pool.
- `economie/client/SmidScherm.java`: tab row + bridge to `items.client.SmidTabs`; the star part and the bonus part are tabs; result cards for the new outcomes.
- `klassen/KlasseBalans.java`: section `[items]` (keys from `ItemsBalans`).
- `wezens/loot/SetBonussen.java`, `zone2/wezens/loot/Z2Sets.java`, `zone3/wezens/loot/Z3Sets.java`: `stukken()` + `Sets.extraStukken` (max 4).
- `VadsrijkFeature.java` (register last + payloads), `client/VadsrijkClient.java` (ItemsClient.init), `tools/features/__init__.py`.
- Lang: the generator overrides the kern stat names + 4 jewellery texts ("sieraad" → "stuk").

## t-karakter (per-character data)
t-karakter had not started when I worked (no NOTITIES there). Everything of mine lives **inside `SpelerData`**:
- `sieraden` map: the 4 new slots;
- `extra("t_items")`: the belt;
- the item components: Vertrouwd/pity.

So when t-karakter re-keys/splits SpelerData per `Karakter.sleutel`, my data follows with no change. Only `Vangnet.TALISMAN_AAN` and `NieuweStats.HEALS` are per UUID, but they are runtime-only (not saved): fine.

## Keys
Y / I / O = drankjesriem 1/2/3 (category Vadsrijk). Other T-slices may pick the same free keys: check at integration.

## Resume point / open
- Head: see the git log. Gametests: see the report.
- No AutoCheck client run (not needed for this slice's logic; the screen layout is untested in a client: **check the smid tabs and the inventory columns in the dev client**, especially at a large GUI scale; the star tab is ~124 px tall under the anvil).
- Open:
  - iNv ("Kracht") not built;
  - website export of erfstuk data not built;
  - Overvadsen/doorgroeien have no ftb/wiki text;
  - `blokuitleg` not needed (no blocks).
