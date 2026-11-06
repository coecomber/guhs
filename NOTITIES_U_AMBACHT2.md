# NOTITIES u-ambacht2: the Ambachten grow with zones 4 and 5

Branch `110-u-ambacht2` (from eaee06d08, 110-t-int). Java: `feature/vadsrijk/ambacht/` (+ `client/`). Generator:
`tools/features/vadsrijk_u_ambacht2.py` (on top of t-ambacht's `vadsrijk_t_ambacht.py`, imports its painters). Cheat sheet:
`SPIEK_U_AMBACHT2.md`. User rule: **the professions ALWAYS grow along with every zone.**

## What is built
### T4 (zone 4, De Lampionkust) and T5 (zone 5, Hoogvads), cap 60
- **Cap 60** (`AmbachtXp.CAP`, +5 per zone: 50 -> 55 -> 60). Tier bands T4 50-55, T5 55-60 (xp 75 / 90 per action, ~2 h per band).
  `AmbachtXp.MAX_TIER = 5`; T6 band 60-70 + xp 105 are already in the arrays (zone 6 volgt).
- **Gathering spots** (`AmbachtPlekken`, POI ids of `Z4WereldPlekken` / `Z5WereldPlekken`, scanned at runtime like zones 1-3):
  - zone 4: Plukken (Bloemeneiland, Knuffeltij, Strandfeest), Houthakken (Vliegereiland, Vuurtoreneiland, Mysterie-eiland),
    Mijnen (Pierkop, Vuurtoreneiland, Mysterie-strand), Vissen (haven, kade, Koraalbaai, wrak, Vuurtoreneiland),
    **coral diving** (duiksteiger + Koraalbaai), hives (Bijenboerderij on the Bloemeneiland, the Strandfeest dunes);
  - zone 5: Plukken (Staartpark, Ooggaarde, wolkbessen), Houthakken (Staartpark x2, Ooggaarde), Mijnen (noordvin, zuidvin,
    staartvin of Moeder Wolkwal), Vissen (Staartpark pond, Marktlaan fountain, the blowhole), hives (Staartpark, Ooggaarde).
  - The t-int zone-4 fishing spots (haven, kade, Koraalbaai) are now T4 (were T3).
- **Materials:** lampionbrons / hemelzilver, drijfhout + vliegerbamboe / wolkenhout, zeekraal + zeegaren / wolkbes + wolkenwol,
  fish koraalbaars, maanrog (night), regenboogmakreel (rain) / wolkenvis, sterrenspiering (night), donderforel (rain),
  dive finds roze_koraal + zeesponsje (`wanneer = "duik"`, not fish), honey lampionhoning + duinhoning / wolkenhoning + hemelhoning.
  Boss cores: Lampionkern (zone-4 bosses), Wolkkern (zone-5 bosses); `AmbachtRegistry.kernVoor` picks by WHERE the boss falls
  (Z5 box, then Z4 box), else by level.
- **Coral diving** (`Zoek.KORAAL`, `PlekWereld`): a sea floor with still water above and the surface 2-8 blocks up (never deeper:
  the walkability rule). Client: brain coral model, a block-sized hit box, the hold-release minigame ("Duiken!"). 6 % a parelschelp.
  Dive finds never count as fish (book, fish collection, the z4 story's fish count).
- **Recipes** (`Recepten`): Koken 4 + 3 dishes (incl. Zeemanshapje = the T4 gatherer snack, Hemelfeestmaal = party feast), Brouwen
  2 + 3 potions (incl. Hemelelixer), Smeden gear T4/T5 + Meesterwerk T4 (Lampionkern) / T5 (Wolkkern) + core-from-splinters,
  Naaien broek/schoentjes T4/T5 + **kite building** `post_vlieger` (vliegerbamboe + zeegaren -> a Guh-vlieger for home by Post).
- **Maatwerk** at the zone-4/5 set levels: gear level 50 (T4) and 56 (T5); bases = the Schelpenset / Wolkwachtset (incl. their
  Verdediging extras) x 0.9; weapons on the zone-4/5 DPS lines. Meesterwerk jewellery x1.18 / x1.36 of the T3 numbers (above the
  zone-4/5 drops: BiS stays true).
- **Imkeren:** hive level gates are SOFT (Imkeren starts in zone 3 for everyone): zone 3 = 1, zone 4 = 20, zone 5 = 35
  (`Imkeren.korfLevel`); a 5th hive at Imkeren 55.
- **Hoogvads = the professions capital** (`HoogvadsGilde`, `Werkplaats.magHier`):
  - T4/T5 recipes are made ONLY at the Gilde teachers (`z5_leermeester_*`); T4 also at Lampionhaven's workshop (`z4_werkplaats`).
    The hub workshops (zones 1-3) show them with a red line "Alleen in het Ambachtsgilde van Hoogvads".
  - **Story steps 6 + 7** (lv 55 / 60) at the Gilde: the teachers of Koken/Brouwen/Smeden/Naaien/Imkeren tell their own; the
    Gildemeester tells the gatherers' (Mijnen, Houthakken, Plukken, Vissen). Option "Vertel eens..." on every Gilde guh. The hub
    teachers send you there after step 5 ("naar_gilde"). Steps at 65/70 are listed as "zone 6 volgt" (`HoogvadsGilde.LEVELS`).
  - Two new board guhs: Klerk Kwastje (Lampionhaven plein, `amb_bord_z4`) and Klerk Kroontje (by the Gilde entrance, `amb_bord_z5`).
- **The Ambachten book (key Y):** bigger (400x250), 7 tabs (+ Opdrachten, Knutselwinkel); the tab row always fits.
  **Fix:** the Plekken tab + the Verkenningsboek page loop over ALL zones (`AmbachtPlekken.maxZone()`); the zone-4 fishing spots
  (and everything of zones 4/5) are in the overview now. Fish count = real fish only; honey count = all meadows.
### Deferred features, now built
- **Knutselpenningen + Knutselwinkel** (`Knutselwinkel`): penningen ONLY from orders (Bestelbord order `1 + tier`, the weekly chest
  10, a player order you fulfil 3, a story step 5, a Meester moment 20). Not an item: can't be traded or farmed on the market.
  Shop: kernsplinter 6, the rare materials 8-10, Uitrustthee (+30 min rested) 12, the 5 boss cores 25-55, Ambachtskist 40,
  home deco by Post (vaandel, Guh-vlieger, klodderknuffel).
- **Opdrachtenbord** (`Opdrachten`, SavedData `guhs:vr_ambacht_opdrachten`): players post "N x <recipe result> for <reward>";
  the reward is taken at once (escrow); another player who knows the recipe hands the items in and gets the reward + penningen;
  the asker picks the items up (also after being offline). Dishes, potions, Maatwerk (exact kind + tier, never Meesterwerk) and boss
  cores; max 5 open per player; min 25 brons per item; open orders expire after 7 days (escrow back as "tegoed").
  **Dupe-safe:** one server-thread call per action; items leave the crafter BEFORE the state changes and are stored as saved
  tags; the escrow moves exactly once; withdraw/expiry refund exactly the escrow.
- **Meesterschort path** (`Meesterschap`): an ambacht at the CURRENT cap = title "Meester-<ambacht>" (+ "Grootmeester der
  Ambachten" for all nine) and, the first time, the **Meesterschort** cape in the Kledingkast (t-kleding `Cosmetica.registreer`,
  texture `textures/vadsrijk/kleding/kaap_meesterschort.png`). Scales with the cap: remembered per ambacht AND cap
  (`AmbachtData "meester"`: "koken@60"), so at cap 70 reaching 70 is a new moment; the title always follows the cap. Login check
  for players who were already there.
### Zone 6 volgt (data hooks, NOT active)
- `Zone6Haken`: 9 T6 materials (sterrengoud, rozenzandsteen, dadelpalmhout, wierookhars, saffraan, zijdedraad, oasekarper,
  sterrenschubvis, zandaal), honey `saffraanhoning`, core `sterrenkern`, 9 spot slots with placeholder POI names, cap 70, gear
  level 62, and the steps to switch it on. Their lang names are already written (generator). ZONE6_PLAN.md did not exist yet, so
  the names follow FEEDBACK_RONDE4 (Zijdeland: rose dunes, oases, saffron, caravans, Rozenkloof, Saffraanpoort, Sterrenkaravaan).

## Decisions I made myself
- All zone-4 fishing is T4 (the zone-4 story's fishing side quest is about a vanilla hengel and counts those too).
- T4 can be made in Lampionhaven's workshop too (so zone-4 players don't need to fly up for everything); T5 only at the Gilde.
- Gathering ambachten have no Gilde teacher NPC of their own, so the Gildemeester tells their stories.
- Board guh of zone 5 at `gilde_ingang` (a ring round the entrance), not inside the hall.
- Kite building = a Naaien recipe for a home decoration (A2: decoration only); no flying kite entity.
- Penningen are a number, not an item (no market abuse); the shop never sells power directly beyond cores/splinters/rest.
- Player orders pay out in Vadsmunten (existing currency) with escrow; no fee.

## Shared-file changes (add-only, marked U-AMBACHT2)
- `tools/features/__init__.py`: `'vadsrijk_u_ambacht2'` in FEATURES (after `vadsrijk_t_z4dieren`, i.e. after t-ambacht + t-kleding).
- lang `nl_nl.json`/`en_us.json`: ~310 keys added; 3 of t-ambacht's texts overridden by my generator (`bestel.kist_open`,
  `npc.bord.hallo`, `boek.hint.plek`).
- Uses (no edits): `Cosmetica.registreer` + `Kledingkast.ontgrendel` (t-kleding), `Z4WereldPlekken.plek`, `Z5WereldPlekken.plek/inZone5`,
  `Z4WezensPlekken.inZone`, `Z4Loot.KRISTALLEN`, `QuestMotor.EXTRA`, `Titels.registreer`.
- Everything else is inside `feature/vadsrijk/ambacht`.

## Test status
- See the end of this file.

## Open / not done
- Not seen in a client: the new spot models (bamboo/coral/calcite), whether the zone-4/5 scans find good spots in every group
  (`/guhs vadsrijk ambacht tp <groep>` + `plekken`), the bigger book screen and the two new tabs, the Meesterschort cape texture.
- The zone-5 spots sit on the whale's back: the STEEN scan falls back to "any natural floor" if the fins aren't rock blocks.
- Zone 6: switch on via `Zone6Haken` once ZONE6_PLAN.md + the zone-6 map exist.

## Test status (2026-11-06)
- `compileJava` clean; `runGameTestServer -Pgt=VadsrijkAmbacht` (t-ambacht's 11 + my 8 in `VadsrijkAmbachtZonesGameTests`):
  **All 19 required tests passed**.
  - Mine: zone45Data (spots per gatherer in zones 4/5, the Plekken overview has zone-4 fishing + zone 5, 15 fish, gear levels,
    cores per zone), koraalDuiken, imkerenZone45, maakT4T5 (T5 sword lv 56, workshop gates, kite), knutselwinkel,
    opdrachtenbord (escrow, deliver once, pick up once, refund once, gear matching), meesterEnGilde, zone6Haken.
- NOT run: an AutoCheck client run (no visual check).
