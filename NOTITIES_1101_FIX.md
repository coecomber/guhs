# NOTITIES 1.10.1 FIX (branch 1101-fix, vanaf release-1.10.0 9bd6a4052)

Twee bugs uit het live spel:
1. "Ik kan niet door bij de quest van Mijnwerker Mopje" (escorte-NPC door een andere speler "meegenomen").
2. "In druipgrotten spawnen er super veel mobs net buiten de cave (net eronder of erboven)."

## Bug 1: escorte-NPC's

### Oorzaak
- Er was precies EEN Mopje-guh. Een klik bij de escortestap deed `npc.volg(speler)`: de echte NPC liep met die ene speler mee.
  Een tweede speler die klikte *stal* haar (volg wisselde), of vond haar niet (ze liep ver weg met speler A).
- Logde A uit of liep A weg, dan bleef `volgt` gezet: de VolgGoal deed niets meer, `Npcs.tick` zette haar niet terug (alleen als
  `volgt == null`) en ze strandde. Na aankomst bleef ze 5 minuten (`rust(naar, 20*60*5)`) bij de bestemming: de "na 5 min was
  ze terug" van de melding.
- Alleen wie zij volgde kreeg de aankomst; partyleden niet.

### Fix (`npc/Escortes.java`, nieuw)
- De echte NPC verlaat NOOIT zijn plek. De escortestap maakt een **persoonlijke kopie** (`RijkGuh.kopie`, nooit opgeslagen, niet in
  `Npcs.levend`, uitgezonderd van de U-DUBBEL-heuristiek) die de speler volgt.
- **Party**: een partylid op dezelfde stap dat met de NPC praat krijgt de kopie van de party; bij aankomst krijgen alle partyleden op
  die stap binnen 48 blokken van de bestemming de escorte.
- **Weg = meteen opnieuw**: uitloggen, dood, andere dimensie, > 64 blokken van de kopie, quest opgegeven of stap al klaar: de kopie
  verdwijnt binnen 0,5 s (ticker) en de speler krijgt "... is teruggegaan naar de vaste plek. Praat opnieuw ...". Uitloggen ruimt
  ook direct op (PlayerLoggedOutEvent).
- Na aankomst rust de kopie 45 s bij de bestemming (inleveren daar, zoals hq09 bij Mopje zelf), dan verdwijnt ze.
- Een vreemde die op andermans lopende kopie klikt: "... loopt al met iemand anders mee. Haal je eigen begeleiding op ...".
- Klikken op de NPC terwijl je al een kopie hebt: de kopie komt naar je toe (vastgelopen kopie).
- Aangepast: `QuestMotor.klik` / `startEscorte`, `NpcHaken` (oude `escortes()` weg), `QuestStap` (tracker naar je kopie),
  `questlopen/QlDoe`, `questrobot/QuestRobot` (robot speelt de echte aankomstcode via `Escortes.kijk`).
- Teksten NL in `tools/features/vadsrijk_npc.py` + nl_nl, EN in `tools/lang/en/c403_vr_npc.json`; engelscheck en taalcheck 0.

## Audit: alle quests van zone 1-6 voor meerdere spelers

De quest-motor zelf is per speler (Questlog, gesprekken, scenes, reizen: allemaal per UUID). De problemen zaten in gedeelde
NPC's, gedeelde bazen met lange respawn en gedeelde dungeon-toestand. Methode: quest-JSON per zone (`quests*.json`, staptypes),
alle `meld`-bronnen, acties en bazen nagelopen; daarna de questrobot.

### Generieke fixes (gelden voor elke boss-stap in elke zone)
- **Toeschouwers tellen mee voor quests**: wie bij de nederlaag IN de arena staat krijgt de quest-credit (niet alleen wie raakte),
  maar geen xp/loot (`Haken.Verslagen.kijkers` / `voorQuests()`, `VadsrijkBaas.ookVoorQuests`; de 6 quest-luisteraars gebruiken
  `voorQuests()`, loot-luisteraars nog steeds `helpers`).
- **Nieuwkomer = snelle respawn**: staat er iemand in een wachtende arena die NIET bij de laatste kill was, dan komt de baas binnen
  30 s terug i.p.v. 5-20 min (`Arenas.NIEUWKOMER_SECONDEN`). Wie bij de kill was (farmen) wacht de volle tijd.

### Tabel

| quest | staptype | probleem met meerdere spelers | fix |
|---|---|---|---|
| hq09_druipgrotten (Mopje) | escorte | een NPC, volgt een speler; tweede speler kan niet; strandt bij uitloggen; 5 min weg na aankomst | persoonlijke kopie per speler/party (Escortes) |
| z10_rammel (Ridder Rammel) | escorte | idem | idem |
| z3_s02_opa_bast | escorte (gever = escorte-NPC) | idem, start al bij aannemen | idem (`startEscorte` maakt een kopie) |
| z5_s07_kadet (Kadet Dons) | escorte (gever = escorte-NPC) | idem | idem |
| hq05_klodderreus | baas | toeschouwer zonder klap kreeg niets (20 s respawn) | toeschouwers tellen mee |
| hq11_opperzwam | meld zwamtempel:deur + baas | 5 min respawn na andermans kill | toeschouwers + nieuwkomer-respawn |
| hq11 (Zwamtempel-dungeon) | gedeelde puzzel/poorten | (1) volgorde kamer 1 maar een keer getoond, nieuwkomer ziet hem nooit; (2) poort kamer 2 ging dicht achter de groep tijdens de golven: latere speler minutenlang buitengesloten; (3) poort-resets keken niet naar de Opperzwam-hal: spelers konden opgesloten raken | (1) nieuwkomer in kamer 1 (stap 0) krijgt de volgorde opnieuw; (2) poort 2 blijft open, latere spelers doen mee met de golven; (3) resets alleen als de hal leeg is (`halLeeg`) |
| z17_koning (Koning Klodder) | baas | 20 min respawn; wie alleen de splitsklodders raakte kreeg geen credit | toeschouwers + nieuwkomer-respawn |
| z13_snurk | elite | geen blokkade (veel bronnen) | - |
| geheim_spion | meld spion:ontmaskerd / gevangen | een gedeelde Mika-spion, 10 min weg na vangst (2 min na vlucht) | staat er een speler die mag ontmaskeren bij de kraam: terug binnen 30 s |
| hq03b, hq04, hq06, hq12, rijdier-quests | quiz / scenes / acties | geen: per speler | - |
| z2_hq05_galmwolf | baas | 20 min respawn | toeschouwers + nieuwkomer-respawn |
| z2_hq07 | elite | geen blokkade (6 % elites, 30-45 s) | - |
| z2_hq11_spiegelzucht | baas | 5 min respawn | toeschouwers + nieuwkomer-respawn |
| z2_geheim_boormeester | meld z2_boormeester:verslagen | gedeelde machine, 10 min weg na nederlaag | ontgrendelde speler met de quest bij de boorplek (of die ontgrendelt): terug binnen 30 s |
| z2_hq09, z2 scenes/vluchten | meld/acties | geen: per speler | - |
| z3_hq02_bezoek | actie (bladrovers-terugval) | elke speler die aannam spawnde 8 extra klodders op het plein (stapelden op) | vult aan tot 8 (tag `z3_bladrover`) |
| z3_hq08_werf / z3_hq13_hart | meld <baas>:verslagen | 5 min respawn per baas; **Vahoeg-modus**: een lv-50-zegel maakte ALLE bazen van die dungeon lv 52 voor iedereen (30 min), ook voor verhaalspelers lv 38-50 | toeschouwers + nieuwkomer-respawn; Vahoeg-vorm alleen als iemand van de zegelgroep (houder + party bij start) in de arena staat, anders normaal (alleen onaangeraakte bazen wisselen) |
| z3_hq10_piepvel | meld z3_piepvel:verslagen | 10 min respawn | toeschouwers + nieuwkomer-respawn |
| z3_hq12_duister | meld z3_grotlantaarn:aan | credit per speler; de lantaarns blijven voor iedereen aan (cosmetisch) | niet aangepast (blokkeert niets) |
| z3_hq14_vahoegnir | meld z3_vahoegnir:opgevrolijkt | start tijdens de eindscene van de vorige spawnde een tweede Vahoegnir; opruimen van de oude haalde de schildtakken van het nieuwe gevecht weg | geen nieuwe start zolang er een (ook verslagen) Vahoegnir bestaat; de speler probeert na 30 s opnieuw |
| z3_geheim_imker | meld z3_imker:verslagen | 10 min respawn, farmers blokkeerden verhaalspelers | toeschouwers + nieuwkomer-respawn |
| overige z3 / z4 | scenes, ringen, ketels, boot, vissen, volleybal, ... | geen: per speler | - |
| z4_hq05 / z4_s05 | meld vliegervanger / zeeslurf | nu per-speler terugval (niet aan het gevecht gebonden); als het gevecht ooit gebonden wordt geldt de generieke fix | - |
| z5_hq08_wolkwaldroom | meld z5_zilverstaart / z5_droom | gedeeld gevecht; wie laat kwam zonder klap kreeg niets en moest 45 s wachten | toeschouwers in de arena tellen mee |
| z5 verhoren, soep, scenes | meld | geen: per speler | - |
| z6_hq07 | meld z6:zoutkolos | `forceerZoutkolos` werd ook gezet als de baas al bestond (volgende natuurlijke spawn negeerde de nacht-regel) | alleen zetten bij een echte reset |
| z6_hq08_sterrenwacht | meld z6_sterrenwacht:kompas (dungeon) | gedeelde dungeon zonder reset: na andermans run een lege koepel met een bord van 5 min | bij de start: elke baas van de route (Astrolabium, Glimmertje, Sterrenguh) die er niet is komt meteen terug (lopend gevecht nooit) |
| z6_s13_wormelien | meld z6_baas:wormelien | toeschouwer zonder klap kreeg niets; `forceerWormelien` ook bij bestaande baas | toeschouwers tellen mee; vlag alleen bij reset |
| z6 puzzels, bakens, tapijt, soep, scenes | meld | geen: per speler (geen wereldblokken) | - |
| z6 raid | - | een groep tegelijk met wachtrij (bewust ontwerp, geen quest hangt ervan af) | - |

Niet aangepast (cosmetisch, geen blokkade): de draken van scenes zijn voor iedereen zichtbaar; de z3-grotlantaarns blijven aan;
de Zwamtempel-ceremonie speelt voor iedereen binnen 24 blokken; Z3Kerkers-puzzels (geen quest-doel) kunnen door anderen worden
teruggedraaid.

## Bug 2: druipgrotten

### Oorzaak
- `WezensPlekken.gangen()` week af van de generator (`bouwsels.grotten`): de gangen tussen knopen eindigden op de knoop-MIDDEN-y
  (4 blokken te hoog, de generator gebruikt vloer + 2), de mondgangen begonnen op de mond zelf 3 blokken te laag (generator: 7 blokken
  buiten de mond, terrein + 2).
- De gang-spawners zochten van y+2 tot y-6 rond die verkeerde lijn, de knoop-spawners tot r onder het midden. De zoekregel pakte
  de EERSTE staanplek van boven in de kolom. De kaart heeft uitgeholde rots (dichte, pikdonkere luchtzakken), dus die eerste plek
  lag vaak in een zak in de rots net boven de gang, of (mondgangen) eronder, of buiten op de heuvel.

### Fix
- `WezensPlekken.gangen()` volgt de generator exact (`knoopVoet`); gang-spawners zoeken alleen in de gang (vloer c-2 .. c+1), knopen
  vanaf hun vlakke vloer (voet-1 .. voet+4).
- `kern/mob/GrotRuimte` (nieuw), voor elke grot-spawner: de staanplek moet in de lucht liggen die VERBONDEN is met het midden van de
  spawner (flood fill door vrije blokken binnen de cilinder + 10 en y-bereik + 8, 5 min gecachet, gedeeld per vorm). Druipgrotten-gangen,
  -knopen en de kristalkamer staan in `GrotRuimte.ONDERDAK`: nooit onder open hemel (heightmap). Terugval op de oude regel als het midden niet in
  de lucht ligt of de ruimte te weinig staanplekken heeft (geen spawner valt stil); niet spawnen zolang de chunks rond de grot niet
  geladen zijn.
- Opruimen: elke 10 s gaan mobs van een wakkere grot-spawner die vlak bij hem maar buiten zijn grotruimte staan (overblijvers in de
  rots/op de heuvel van voor de fix) weg, behalve als ze vechten; de spawner vult in de grot aan (`Spawners.verdwaaldWeg` telt mee).

## Tests
- `npc/VadsrijkEscorteGameTests`: twee spelers tegelijk (hq09), na elkaar, reset als de eerste wegloopt / opgeeft / uitlogt (de
  tweede begint meteen), party deelt de kopie (vreemde niet), en generiek alle 4 escortequests met twee spelers.
- `kern/baas/VadsrijkArenaSamenGameTests`: toeschouwer in de arena krijgt de baas-stap, iemand erbuiten niet; nieuwkomer = snelle respawn.
- `kern/mob/VadsrijkGrotRuimteGameTests`: testgrot met gang + dichte zak erboven: oude regel vindt de zak, nieuwe spawnt alleen op de
  gangvloer, zak-mob = verdwaald, onder een gat naar de hemel geen spawn (ONDERDAK); en de druipgrotten-spawners liggen in hun gang
  (smal y-venster, gangen eindigen op knoopvloer + 2).
- Questrobot (`VadsrijkQuestRobotGameTests`, speelt de escortes nu via de echte `Escortes.kijk`): zone 1-6 = 41/19/24/23/24/25
  gehaald, **0 vast**. QUESTROBOT_RAPPORT.md opnieuw gemaakt.
- Gedraaid: `-Pgt=VadsrijkEscorteGameTests,VadsrijkGrotRuimteGameTests,VadsrijkArenaSamenGameTests,VadsrijkQuestRobotGameTests,VadsrijkNpcGameTests`:
  alles groen (de grottest na een fix in de test: open hemel via de heightmap, niet het hemellicht dat achterloopt).
- Niet in een client getest; de echte druipgrotten kan de gametest-server niet laden (geen Vadsrijk-dimensie). Na de update op de
  server: `Spawners.verdwaaldWeg` laat zien hoeveel oude mobs uit de rots zijn opgeruimd.
