# Notities 1.10.1 balans (branch `1101-balans`, vanaf release-1.10.0 9bd6a4052)

Bron: `BALANS_1101_ANALYSE.md` + de beslissingen van de gebruiker. Alles is code (geen losse server-tweaks nodig), behalve het
eenmalige opruim-commando bij de uitrol. Niet aangeraakt (gebruiker: "dit is gewoon de combat triangle"): Roze x1,5, bazen-loot,
Zwamgrot-moeilijkheid, smidsterren.

## 1. Loot-pool bug (Gorgonzola's moersleutel in zone 1)

- Nieuw: `kern/loot/ZonePools.java`. De pool van zone N = alle basistypes in de wezens-tabellen van die zone (`wezens:*`,
  `z2wezens:*` .. `z6wezens:*`, ook de `_elite`-tabellen; niet `contract_*`/`wapens`), **zonder** benoemde baas/raid/droom-items
  (`baas_*`, `zNbaas_*`, `z5droom_*`, `z6raid_*`) en **zonder** Maatwerk (`mw_*`). Wapens alleen van je eigen klasse.
- Gebruikt door alle "stukVoor"-plekken (die kozen uit alle 274 types van alle zones):
  - questbeloningen `npc/Beloningen` (zone = `ZoneBoeken.zoneVan(quest-id)`), zone-1 schatten `NpcHaken`;
  - kerkerkist `grotkerker/KerkerLoot` (zone van de kerker);
  - grot-elites + grotschatten `grotten/Grotten` (zone van de grot);
  - de schatten-haken van zone 2-6 (`Z2Haken`, `Z3Haken`, `Z4Haken`, `Z5Haken`, `Z6Haken`).
- Pool-groottes en "alleen eigen zone" worden getest (`VadsrijkBalans1101GameTests.lootPoolsPerZone`).

## 2. Eenmalig opruimen: `/guhs vadsrijk loot opruimen [speler|alle] [--echt]`

- Code: `zonepoort/LootOpruimen.java` (+ 1 regel in `ZonePoortRegistry.register`). Op-only (gamemaster).
- Een item is "te hoog" als `ZonePools.zoneVan(type) > hoogste open zone van de speler`. zoneVan: benoemde items via hun prefix
  (`baas_` = 1, `z5baas_` = 5, ...), anders de LAAGSTE zone wiens wezens-tabellen het laten vallen; Maatwerk/onbekend = 0 = nooit.
- Hoogste open zone = min(server `openZones` uit `guhs-vadsrijk-zones.toml`, de hoogste zone waar de speler een quest van begon of
  afrondde / `zone<N-1>_klaar`), minimaal 1. (Er is geen poortstand per speler in de code.) Inactieve karakters tellen met hun
  eigen questdata.
- Bereik **online**: live inventaris (incl. harnas/offhand), enderkist, Curios-vakken, opgeslagen Vadsrijk-inventaris en
  thuis-inventaris, de stash (`gevonden`, waar `geefOfBewaar` heen gaat), sieraden, Post (`economie.post`), drankjesriem,
  inactieve karakters + archief, alle knapzak- en banktabs, materiaaltas.
- **Offline**: de hele `playerdata/<uuid>.dat` (zelfde plekken). Met `--echt` eerst een eenmalige back-up
  `playerdata/<uuid>.dat_voor_opruimen`, dan veilig terugschrijven. Logt de speler in tijdens de run, dan wordt zijn bestand
  niet geschreven (staat in de chat).
- **Niet bereikbaar** (niet per speler opgeslagen, staat in de proefrun-uitvoer): marktaanbiedingen, bondbank, items op de grond
  of in geplaatste blokken.
- Zonder `--echt`: alleen een lijst (max 60 regels in de chat, alles in de serverlog). Met `--echt`: weg + per item een
  INFO-regel "REMOVED" (naam, uuid, plek, type, zeldzaamheid, level, zones) + totalen.

## 3. Mobs: +30 % schade, +40 % leven, regen in gevecht 0,3 %/s

- `kern/Balans`: `MOB_SCHADE_STANDAARD` 1,3 -> **1,69** (1,3 x 1,3), nieuw `MOB_LEVEN_STANDAARD` **1,4** (`[mobs] leven`).
- Leven gaat via `VadsrijkMob.vadsOp(level) = basisVads x levenFactor`: open-wereld wezens (met een loottabel, geen baas) krijgen
  `[mobs] leven` x de hellingfix (zie 6); bazen en baas-hulpjes alleen de compensatie (zie 6). Elites x2,5 blijft erbovenop.
- Regen: nieuw `kern/Gevecht.java`. In gevecht = binnen 5 s na een klap van of op een Vadsrijk-wezen/baas (`Schade.doe` zet het).
  In gevecht **0,3 %/s**, daarbuiten **1 %/s** (zoals voorheen). `KernEvents` gebruikt `Gevecht.regen(p)`.

### Config-migratie (`config/guhs-vadsrijk-balans.toml`)

De live server schreef dit bestand bij de eerste start met `[mobs] schade = 1.3` en zonder `leven`. Nieuw: `[mobs] versie`.
- Bestand zonder `versie` (of < 2) = 1.10.0. Bij het laden: `schade` die nog precies 1.3 is -> 1.69 (met de hand gezet = blijft),
  ontbrekende `leven` -> 1.4, `versie = 2`; daarna wordt het bestand opnieuw geschreven en staat er een regel in de log
  ("Vadsrijk balans: migratie [mobs] versie 1 -> 2: ...").
- Een nieuw bestand krijgt meteen `versie = 2`. Daarna verandert de mod niets meer aan de waarden.
- Handmatig controleren: `/guhs vadsrijk balans toon` -> `[mobs] schade=1.69 leven=1.40 versie=2.00`.

## 4. Drops

`kern/loot/DropBalans.java`, toegepast in de wezens-tabellen van alle zones (`WezensLoot`, `Z2Loot` .. `Z6Loot`):

| | Vóór | Na |
|---|---|---|
| gewoon wezen, kans per kill | 0,14-0,35 (zone 1 gem. 0,224) | x0,2 (zone 1 gem. ~0,045) |
| elite, kans per rol (2 rollen) | min(1, kans x 3,5) ~0,78 | min(1, kans x 3,5) x 0,45 ~0,35 |
| elite-geluk | z1 2,5 / z2 2,8 / z3 3,0 / z4 3,2 / z5 3,0 / z6 3,4 | x0,64: 1,6 / 1,79 / 1,92 / 2,05 / 1,92 / 2,18 |
| items per uur (zone 1, 4 kills/min) | ~73 | **~20** |
| Legendarisch+ per uur | ~4,5 | **~0,6** |
| kerkerkist | 1 stuk (1e clear Episch, daarna 88/12 Zeldzaam/Episch) | **2 stukken** (1e clear: het eerste Episch) |

Bazen, munten, de tutorial-proefklodder en questbeloningen: ongewijzigd (behalve de pool, zie 1).

## 5. Quest-xp en kerker-xp

Zie `XP_BALANS.md` sectie "Ronde 1.10.1" voor alle tabellen.
- Hoofdquests (generators, blok `BALANS_1101_HOOFD`): zone 1 vol t/m hq07, x0,6 vanaf hq08 (hq08 3000 -> 1800, hq09 5900 -> 3540,
  hq10 4400 -> 2640, hq11 3000 -> 1800, hq12 2000 -> 1200); zone 2 x0,6, zone 3 x0,8, zone 4 ongewijzigd, zone 5 x0,65, zone 6 x0,7.
  Alleen de `xp`-waarden in de quest-JSON's veranderden (52 regels).
- Kerkerbaas `GrotBaas.xpFactor`: 0,15 -> **0,50** (zone 1-2) / **0,63** (zone 3-6) van stap(baaslevel): een solo-run = ~0,8 level.
  Zwamgrot 791 -> ~1.400 xp per run.
- Resultaat (los, 5 kills onderweg, 2 kills/min): alleen hoofd zone 1 **30 min** grind, zone 2 23 min, zone 4 1 min; hoofd + 3 runs:
  zone 1/2/4 **0 min**. **Zone 3 (72), 5 (39) en 6 (64 min)** zitten boven de 10-30 min voor alleen-hoofd en hebben ook met runs
  nog 13/28/18 min (het model doet runs pas vanaf kerker-minLv - 1; de grind zit bij poorten daarvóór). De factoren zijn niet
  aangepast (beslissing gebruiker); zone 2+ is nog dicht, dus later bij te stellen.

## 6. Schadecurve (optie A) + TTK

- `Schade.aanval`: hoofdstat **1 % per punt** (was 2 %). Spelers krijgen daarbovenop een vaste basis **x1,7** (`Schade.HOOFD_BASIS`,
  in `Schade.doe`, alleen als de bron een speler is; mobs hebben geen hoofdstat en raken dus niet harder). Zonder die basis zou
  zone 1 met +40 % HP en de steilere helling ~x2,7 langer duren; met 1,7 komt lv 14 uit op ~x1,6 (analyse-advies ~x1,5).
- Wezens-HP-helling 0,30 -> **0,45** per level: `MobBalans.helling(lv) = (1 + 0,45(lv-1)) / (1 + 0,30(lv-1))` bovenop basisVads (alle
  zones gebruiken 0,30, dus één plek in plaats van ~60 formules).
- Bazen: HP x `MobBalans.compensatie(lv)` = (nieuwe / oude hoofdstat-factor) bij de model-Kracht `14 + 3,2 x lv`, zodat een baas
  even lang duurt als in 1.10.0 (x1,48 op lv 1, x1,24 op lv 14, x1,0 op lv 70). Geldt ook voor de kerkerbazen en baas-hulpjes.
- `klassen/Balans.dps` (de DPS-tabel van de klassen-test) gebruikt dezelfde `Schade.hoofdFactor`.

**Speler-klap vs mob-HP** (model: zone-wapen Zeldzaam, Kracht 14 + 3,2 lv, mob basis 45 HP, gewone aanvallen; TTK-verhouding is
wat telt):

| Lv | Kracht | x hoofdstat oud | x hoofdstat nieuw | DPS oud | DPS nieuw | Mob-HP oud | Mob-HP nieuw | TTK oud | TTK nieuw | TTK x | Baas-HP x |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 17 | x1,34 | x1,99 | 11 | 16 | 45 | 63 | 4,2 s | 4,0 s | x0,94 | x1,48 |
| 5 | 30 | x1,60 | x2,21 | 21 | 29 | 99 | 176 | 4,6 s | 6,0 s | x1,29 | x1,38 |
| 10 | 46 | x1,92 | x2,48 | 38 | 50 | 166 | 318 | 4,3 s | 6,4 s | x1,48 | x1,29 |
| 14 | 59 | x2,18 | x2,70 | 57 | 70 | 221 | 432 | 3,9 s | 6,1 s | x1,58 | x1,24 |
| 20 | 78 | x2,56 | x3,03 | 87 | 103 | 302 | 602 | 3,5 s | 5,8 s | x1,69 | x1,18 |
| 40 | 142 | x3,84 | x4,11 | 263 | 282 | 572 | 1169 | 2,2 s | 4,1 s | x1,91 | x1,07 |
| 70 | 238 | x5,76 | x5,75 | 760 | 758 | 976 | 2019 | 1,3 s | 2,7 s | x2,07 | x1,00 |

In de zone-1 termen van de analyse (Knuffelridder met skills, tabel 2): TTK lv 5 1,7 -> ~2,2 s, lv 10 1,7 -> ~2,5 s,
lv 14 1,7 -> ~2,7 s, lv 20 1,5 -> ~2,5 s (advies was 2,2-2,6 s). Over het hele spel: DPS lv 1 -> 70 was x69, nu x47; TTK zakte
van 4,2 naar 1,3 s (x3,2 korter), nu van 4,0 naar 2,7 s (x1,5).
Met +30 % mobschade en 0,3 %/s regen in gevecht worden 3 mobs tegelijk weer gevaarlijk (analyse sectie 2, marge ~x1,0-1,5).

## 7. Inhaalbonus klassen

- `Spelers.inhaalFactor`: klasse-xp x (1 + 0,15 x (hoogste klasselevel - dit klasselevel)), max x3. Generieke xp ongewijzigd.
  Geen dagelijkse proef.
- Karakter-tab (`menu/client/KarakterPaneel`): onder een klasse die achterloopt "Inhaalbonus ×1,6" (groen, met uitleg-tooltip).
  Teksten `vadsrijk.menu.karakter.inhaal(.uitleg)` in `vadsrijk_u_tabs.py` + Engels in `tools/lang/en/c310_vr_u_tabs.json`.

## Tests

- Nieuw `kern/VadsrijkBalans1101GameTests` (batch `vadsrijk_balans1101`): `lootPoolsPerZone`, `mobVermenigvuldigers`,
  `configMigratie`, `dropKansen` (seed 1101, 20.000 rollen), `regenInEnUitGevecht`, `inhaalXp`, `statFormule`.
- Nieuw `zonepoort/VadsrijkLootOpruimenGameTests` (batch `vadsrijk_loot_opruimen`): `hoogsteOpenZone`,
  `proefrunVindtMaarLaatStaan`, `echtVerwijdertPrecies`, `offlineSpelerbestand`.
- Aangepast: `VadsrijkKernGameTests.schadeEnLevels` (1 %/punt), `VadsrijkBalansGameTests.verdedigingPerLevel` (1,69),
  `VadsrijkGrotKerkerGameTests.lootPerClear` (2 stukken, zone-1 pool), `VadsrijkZ6RaidGameTests.schaal` (baas-HP x compensatie).
- Uitslag (2026-11-14): 173 relevante tests (kern, balans, klassen, melee, bazen z1-z6, raid, wezens z1-z6, grotkerker, dungeon,
  items, npc, menu, grotten, imker, zonepoort, economie, questrobot, opruimen): alles groen na de raid-testfix. De questrobot
  speelt zone 1-6: 41/19/24/23/24/25 gehaald, **0 vast**. Tweede ronde (raid, z2-bazen, Z2/Z3 Npc, Z4/Z5/Z6 Verhaal, taal,
  alle labels, grotten): groen op `zandloper` na, die bij een herhaling slaagde (wisselvallig, net als `spiegelzuchtSchaduwEnStraal`
  in ronde 1; beide raken niets van deze branch). engelscheck OK, taalcheck 0 ontbreken.

## Serverstappen bij de uitrol

1. Jar + pack zoals altijd. **Niets** in `config/guhs-vadsrijk-balans.toml` met de hand doen: bij de eerste start migreert de mod
   `[mobs]` (log: "Vadsrijk balans: migratie [mobs] versie 1 -> 2"). Controle in de console: `guhs vadsrijk balans toon` ->
   `[mobs] schade=1.69 leven=1.40 versie=2.00`.
   - Fallback (als de migratie om wat voor reden niet liep): in dat bestand onder `[mobs]` zetten: `schade = 1.69`, `leven = 1.4`,
     `versie = 2` (wordt binnen 5 s live gelezen).
2. Opruimen (één keer, als op, liefst als er weinig spelers online zijn):
   - `/guhs vadsrijk loot opruimen alle` -> lijst nalezen (serverlog heeft alles);
   - `/guhs vadsrijk loot opruimen alle --echt` -> weg + log; offline spelers krijgen een back-up `playerdata/<uuid>.dat_voor_opruimen`.
   - Terugdraaien voor één offline speler: server stoppen, `.dat_voor_opruimen` terugzetten als `<uuid>.dat`.
3. Wiki: de drop-/xp-cijfers komen uit de runtime-export; opnieuw genereren zoals bij 1.10.0 als de wiki die toont.
