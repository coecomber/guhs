# NOTITIES U-GILDEN (round U, branch 110-u-gilden, from eaee06d08 = 110-t-int)

The guilds of FEEDBACK_RONDE4 V1 ("guild-wijk met een eigen kasteel, samen-doelen + kleine perks, oprichten pas in zone 5, rekruteren
vanaf zone 1"). Package `feature/vadsrijk/gilden` (+ `.client`), texts `tools/features/vadsrijk_u_gilden.py`, structures
`tools/vadsrijk/gilden/kasteel.py`. Test steps for the user: `SPIEK_U_GILDEN.md`.

## Name decision
**Knuffelbond** (short "bond"), the valley is the **Bondenvallei**. Why not "gilde": Hoogvads already has the **Ambachtsgilde**
(t-z5verhaal: Gildemeester, `gilde_*` POIs, `HoogvadsGilde`), so "gilde" would mean two things in one city. "Broederschap" is long
and not guh-like; "Knuffelbond" is clear ("bond" = a club you join) and fits the world. The dev commands still say `gilde`
(brief) and the package is `gilden`.

## What was built
- **Data** `Bonden` (SavedData `guhs:vr_gilden`, overworld data folder) + `Bond`: name, tag, banner, members (per CHARACTER key
  `Karakter.sleutel(p).tekst()` = "uuid/slot"), applications, xp, plot, castle level (+ what is built), kas, depot, trophies,
  week tasks, the vault. The index character -> bond is rebuilt from the member lists on load (one source of truth).
- **Ranks** `Rang` LEIDER / OFFICIER / LID / REKRUUT with `Recht`s; you only act on lower ranks; promote to at most one below you.
- **Actions** `BondActies` (all rules in one place, used by commands, panel, NPCs and tests): found (at the Bondsklerk only,
  1 goud, zone 4 done (`z4_hq07_opstijgpier`) or lv 56+), invite/accept/decline (5 min), apply on the board (open bond = join at
  once, else an officer accepts, also when the applicant is offline), leave, kick, promote/demote, hand over, disband (only with
  an empty vault; kas + depot back to the leader), recruiting settings, banner, kas donations. Rekruut -> lid automatically after
  200 bond xp contributed. Max 50 members.
- **Bond xp** `BondXp`: from `Haken.VERSLAGEN` (+1, elite +3, boss +40 + trophy), `NpcApi.QUEST_KLAAR` (+15), `Haken.LEVEL_OP` (+10),
  `AmbachtData.LEVEL_OP` (+10) and polled ambacht xp (1 per 50). Daily cap 600 per member. Levels 1-10 (0/1k/3k/6k/10k/16k/24k/
  35k/50k/70k). Perks only convenience: vault tabs 1-5, travel discount 5/10/15 % (luxury travel-map flight), castle teleport
  every 30/15/10/5 min, **Bondsrust** (offline hours fill a pool; +50 % fighting xp until empty; via `Spelers.XP_FACTOR`, the pool
  is drained by polling the generic-xp delta). Week tasks: 3 of 5 kinds per bond per week (Monday 06:00 Amsterdam), targets
  scale with members; each +400 xp + 10 zilver in the kas, all three +500.
- **Chat + names** `BondChat` (`/bc`, `/bond chat` toggle via ServerChatEvent, "!" = once to all), `BondTags` (⟨TAG⟩ in the banner
  colour after the name: server NameFormat/TabListNameFormat + a payload map for the client name above the head, like t-karakter's
  Badges; only in the Vadsrijk).
- **Banner** `Banier`: base colour + max 3 layers from 14 friendly vanilla patterns; recolours every banner block on the plot.
- **The panel** `BondPaneel`: the "UI" is a clickable chat panel (`/bond`), plus a "⚑" button left of the inventory (client).
  Chosen over a new screen: no free good key (see keys), works at the NPCs and from anywhere, and every button is a normal
  command (same rules).
- **NPCs** `BondNpcs`: Bondsklerk Zegeltje (`bond_klerk`, Hoogvads POI `paleis_kanselarij_bureau`), Poortwachter Wolkenpoort
  (`bond_poortwachter`, POI `staartpark_prieel` = the Bondenpoort, a particle ring), Pleinwachter Pluim + a board on the valley
  square, and a Bondenbord guh per hub (`bond_bord_<hub>`, placed at runtime on a free floor 6-14 blocks round the hub POI:
  Knuffelkamp `kampvuur_kamp`, Zwamzoet `plein`, Kandijkom `plein`, Honingtak `markt`, Lampionhaven `plein`, Hoogvads
  `markt_fontein`). Talk = the Vadsrijk talking screen (`Praat`, sleutel `vr_gilden`).
- **The valley** `Vallei`: ONE shared valley **inside the Vadsrijk dimension**, far out in the pink sky (centre -12000, 12000,
  ground y 120). Square (56x56, the walk-in gate north back to Hoogvads) + a grid of plot cells (72x72: roads north + west, a
  64x64 plot with a hedge), 6 columns filled from the middle out, rows grow south. A bond takes the lowest free cell; cells are
  stamped only when claimed (1000 bonds = 167 rows; chunks only load when someone is there). Railings at open road edges are
  code (`randen`, they follow which neighbours exist). Falling off -> back on the square. Area `bondenvallei` (town: character
  switching allowed). **Why not an own dimension:** kern swaps the inventory to the normal world when you leave `guhs:vadsrijk`;
  inside it the inventory, HUD, adventure rules (no breaking/placing), mounts, karakter rules all just work.
- **Castles** `Kasteel`: 5 pre-made templates (`guhs:vadsrijk/gilden/kasteel_1..5`, markers `data/guhs/vr_gilden/kasteel_<n>.json`).
  Upgrade = bond level (2/4/6/8) + kas (2g/5g/10g/25g) + profession materials in the depot (T1 zwamsteel/pluisijzer/pluiskatoen ->
  T3 schemerhout/amberkoper/... + roze_hars/glinstergruis/bijenwas for the Knuffelslot); members donate from inventory + materiaaltas
  (`Opslag.tel/neem`), officers build. A rebuild clears the plot (not the hedge ring) and stamps the new template, recolours the
  banners, re-places the trophies (ItemDisplay + TextDisplay, tag `vr_bond_trofee`, removed before every rebuild).
  Clicks (RightClickBlock, only in the valley, only members of that plot's bond): lodestone = teleport stone, chest/barrel = vault,
  smithing table/smoker/loom/brewing stand = t-ambacht's workshop of SMEDEN/KOKEN/NAAIEN/BROUWEN (`AmbachtSchermen.werkplaats(p, a,
  "bond_werk")`), other utility blocks just do nothing.
- **Vault** `BondBank`: 5 tabs x 45, vanilla 6-row chest screen (no client code), the bottom row = tab buttons / info / sort
  (server-side clicks). Live containers inside `Bonden` (no copy), server thread only; every take-out path goes through
  `Slot.mayPickup` (rank check); bundles/shulkers/bound gear refused; closes when kicked / 8 blocks away / castle rebuilt.
- **Teleports** `BondTeleport`: hub -> castle (`/bond thuis`, from any town area or in the valley), stone -> back (free) / square /
  unlocked hubs (`QuestMotor.reisSlot`), shared cooldown per character.
- **Website** `BondExport`: `hiscores/bonden.json` through `WebExport.EXTRA_HISCORES` (same folder/timer/switch/format: lijst, naam,
  tijd, cap, rijen[plek, naam, waarde = level, xp] + tag, leden, kasteel). Names are limited to `[A-Za-z0-9 -]`, safe on a page.

## Structures (tools/vadsrijk/gilden/kasteel.py)
Pink-guh palette: stone-brick foundation, pink terracotta walls, quartz corners/bands, cherry floors, purpur roofs and spires with a
gold + end-rod finial, pink glass panes, cherry fences, lanterns on iron chains, cherry-fence chandeliers with lanterns and candles,
wall banners (white in the file, recoloured by Java). Own connection pass (fences/panes/bars/walls/chains with their states) +
the Java side places with known shape OFF so vanilla recomputes them too (u-hekken's `verbind.py` was not merged in this tree).
Walkability: tower stairs along the east inside wall with a railing round the opening, open 3-wide arches, the courtyard and the
plot at ground level, water 1 deep, nothing on the paths. Generated .nbt + .json ARE committed (small assets, not map tiles).

## Shared-file changes (add-only, marked U-GILDEN)
- `VadsrijkFeature` (+2: register + payloads), `client/VadsrijkClient` (+1).
- `luchtschip/Luchtschepen`: `PRIJS_FACTOR` list + `luxePrijs(p)`; `luxe()` charges/announces that price instead of the constant
  (the travel discount perk).
- `tools/features/__init__.py`: +1 FEATURES line `vadsrijk_u_gilden` (before `vadsrijk_t_vfx`, which stays last); lang nl_nl/en_us +282 keys `vadsrijk.gilden.*`,
  `vadsrijk.npc.naam|praat.bond_*`, `vadsrijk.kern.gebied.bondenvallei` (by the generator's `_direct`), EN overlay
  `tools/lang/en/c300_vr_u_gilden.json`.

## Keys
No new key (the free ones are awkward and H clashes with einde2). The ⚑ button left of the inventory + `/bond`.

## Tests
`runGameTestServer -Pgt=VadsrijkUGilden` (batch `vadsrijk_u_gilden`): **all pass** - rechten, bankDupeVeilig (every click path,
rekruut/lid/tab rules, kicked = screen invalid, save+load, no disband with a full vault), bondsXp (cap, levels, week task once,
perks, castle level 2 + export), perKarakter, kasteelInDeWereld (square + 2 cells + castle 1 and 5 really stamped in the test
level: lodestone/chest in place, every banner pink, the railing fences connected, member vs stranger clicks, trophy displays).
The gametest server has no Vadsrijk dimension: `Vallei.TEST` points the stamping at the test level.

## Open points / resume
- Not seen in a client: the castles in the world (look, the stairs, the banners recolouring, trophies), the panel layout in chat,
  the ⟨TAG⟩ above heads, the board guh spots per hub (runtime ring search), the Hoogvads gate keeper's spot in the gazebo.
- The site has no tab for `bonden.json` yet (s-site/u-site can add it; the file format is the same as the other lists).
- The vault lives in SavedData (like a vanilla chest), the player's inventory in player.dat: a hard crash between the two saves can
  roll one back (same as any chest). t-opslag's own bank is atomic because it lives on the player; a shared vault cannot.
- Raids/zone 6 and housing are other slices.
