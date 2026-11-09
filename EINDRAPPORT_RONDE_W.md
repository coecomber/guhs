# Eindrapport ronde W (2026-11-08/09): jouw playtest-feedback + release-veiligheid

**Testclient:** `guhs_vadsrijk\w-int\run-client.bat` (branch `110-w-int`). Nieuwe wereld maken: de kaart is opnieuw gegenereerd.
**Spiekbrief:** `SPIEKBRIEF.md` (hoofdstuk 0 Questlopen, 1 al je feedback, 2 de nieuwe systemen). **Testlijst:** `TESTEN_VADSRIJK.md`.

## Wat er gedaan is

### De slices (allemaal samengevoegd in 110-w-int)
| slice | wat |
|---|---|
| w-modellen | de oude vierkante klodders en de vierkante Klodderreus terug; de blauwe stadsdiertjes zijn echte Blubs |
| w-leven | huisdiertje verder achter de guh, rustiger steigers/pieren, pieppiepmuisjes ~30 % (Kandijkom niet), minder figuranten en goed gedraaid |
| w-spel | "ver in het oosten"-film gevuld, laden vóór elke cutscene, echte zonenamen (+ taalcheck), Questlopen [Sla stap over] / [Naar veilige plek], kussen van Roosje |
| w-grot | dungeons: echte dichte deur, dungeonvenster, solo/groep/wachtrij, klaar-check, wachttijd |
| w-party | Party-tab, party bewaard na herstart, nep-spelers (aan/uit per speler en per server) |
| w-balans | meer xp in zone 4 en 6; antwoord op de Lampionkust-vraag (lv 56 haalbaar in de zone) |
| w-schepen | elk luchtschip strak tegen een open pierpunt, instappen door oplopen/klikken, vangnet; lijn 0 aan; lijn E vanaf de nieuwe Staarthaven |
| w-engels | Engels voor alle Vadsrijk-teksten + `engelscheck.py` |
| w-bereik | de bereik-check over de hele kaart + generator-fixes (Kootje, walvistrap, Duimdop, kathedraal, Saffraanpoort-deuren, Opstijgpier, Nestkapel, werf) + `vrijpad` |
| u-site | geen nepdata meer op de site, wiki begint altijd bij zone 1 |

### Integratie (w-int)
- Alle merges, de koppeling party ↔ dungeon (`DungeonRijBrug`), de nep-spelers vullen de dungeon-wachtrij.
- De 20 ontbrekende teksten (nl + en), Engels bijgewerkt (engelscheck OK), flash F4 begint nu bij de toren.
- Opstijgpier: beide openingen in de ring (oost voor het schip, west voor de pier).
- **Alle geheimpjes vindbaar** (jouw release-eis), elk eerst op de echte kaart nagekeken:
  - zone 1: knuffel Mosje op de bosgrond (zat 36 hoog op een paddenstoelhoed); de smalle plankenloop naar de Hoedenschat zonder
    "dichte hoekjes"; de geheime tempelkamer achter de gebarsten muur heeft nu een bres (niets opende hem);
  - zone 2: sneeuwtrap naar de Galmgrot; schacht met steigerwerk door het dak van de IJsgrot; aanwijzing "boorgaten" van de richel af;
  - zone 3: kuiken 3 aan de bereikbare rand van vijver 3; bladeren weg boven het pad naar het uitkijkpunt op de Westtak;
  - zone 4: vliegerspul 1 en 3 op het pad onder de vliegerrotsen.
- **Kaartpasses:** de nieuwe `vrijpad`-pas (lantaarns/kettingen/zwevende hekjes uit de looplijnen) brak de hekverbindingen
  (verbindcheck 0 → ~1.300 meldingen). Opgelost: vrijpad laat hoekstukken van een reling staan en de verbind-pas draait daarna nog
  een keer. Nu verbindcheck 0.
- Volledige kaart-regen van zone 1-6 (zone 4 vóór zone 5), alle outputs gecommit.
- Website opnieuw gebouwd uit w-int en de verborgen preview bijgewerkt.

## Tests
- **Volledige suite:** `runGameTestServer -Pgt=Vadsrijk,Dubbel,Geluid,Geroezemoes,Indeling,StadsMuziek,Stadsleven,TModels,UDeco,VInt,Wolkwal,Z2Npc,Z3Npc,Z4,Z5,Z6,WLeven,SceneLaden,BereikDoelen,Taal`
  gaf **407 tests, 6 rood**:
  - **2 echte fouten, gerepareerd:** `schadegetallen` en `huisdierNaamVanSpeler` verwachtten Nederlandse tekst, maar de testserver
    laadt en_us, en dat is sinds w-engels echt Engels. De tests accepteren nu nl of en. Los daarna: 6/6 groen.
  - **4 slagen los** (volgorde-afhankelijk): `spiegelzuchtSchaduwenstraal` (Z2Bazen 6/6), de speeltuin-test van Stadsleven (5/5),
    `samenNeurienEnRoze` (Z5Droom 7/7; de droomwolk wordt alleen in lucht gebouwd en botst met buurtests) en `geenDubbelen`
    (Ruiters 3/3).
- **Onderweg:** de party/dungeon/HUD/raid/menu/kern/taal-set 36/36 (met de gerepareerde `nepVultRijEnStart`), BereikDoelen 2/2.
- **Questrobot (zone 1-6):** 41/19/24/23/24/25 quests gehaald, **0 vast, 0 niet gestart**, 0 statische fouten, 0 staanplek-fouten.
  Alle zonevlaggen staan en alle luchtschiplijnen (ook lijn E) gaan open. 328 NPC's, 0 ontbrekende teksten.
- **AutoCheck-wandeling** (`tools/autocheck/vadsrijk_w_int.txt`): {{AUTOCHECK}}

## Checkers (na de laatste regen)
| check | uitslag | vorige keer (v-int) |
|---|---|---|
| bereikcheck `--strikt` | **exit 0**: alle hoofdquest-doelen en NPC's lopend bereikbaar | (nieuw) |
| schepen_check | **23 instappunten, 0 fouten** | (nieuw) |
| luchtschip_check | lijnen 0, A, B, C, D, E OK | A-D OK, lijn 0 uit |
| kijkcheck5 / kijkcheck6 | OK / OK | (nieuw) |
| verbindcheck | **0** | 8 |
| audit (zone 1) | kuil 4, poi 16, stap 5, water 20 | kuil 4, poi 20, stap 5, water 20 |
| grotcheck | 0 problemen in alle grotten | 0 |
| loopcheck3 | PAD 395, VAL 2 | PAD 397, VAL 1 |
| loopcheck4 | 0 | 0 |
| loopcheck5 | VAL 475, LEEG 662 (open staartvin, het Wolkvangnet vangt je) | VAL 475, LEEG 660 |
| zweefcheck | z1 43, z2 20, z3 0, z4 26, z5 129, z6 0 | z1 115, z2 20, z3 0, z4 26, z5 141, z6 0 |
| poicheck3 / 4 | 0 / 0 | 0 / 0 |
| poicheck6 (gepubliceerde plekken) | 0 | 0 |
| taalcheck | 2.510 sleutels, 0 ontbreken | (nieuw) |
| engelscheck | OK (na aanvulling + apply_en + `--merk`) | (nieuw) |
| `vadsrijk_w_grot.py --check` | OK (bekend: Wolkengrotten-luik 22 van 28 cellen open, zo bedoeld) | (nieuw) |
| kaartPak | 0 verschillen | 0 |

Toelichting:
- loopcheck3 VAL +1: een dicht 1-celsgaatje in de werftoren (1677, 214, -805), je kunt er niet in.
- poicheck6 / loopcheck6 lezen `_cache/plekken_z6.json`, dat zijn de ONgesnapte planplekken (37 / 30 meldingen op y 70). De
  plekken die het spel echt gebruikt (Z6WereldPlekken, gesnapt) geven 0 fouten.

## Bereik (bereikcheck, lopend in survival)
| zone | hoofdquest | zijquest | NPC's | geheimen | arena's |
|---|---|---|---|---|---|
| 1 | 24/24 | 40/40 | 52/52 | **61/61** | 1/1 |
| 2 | 28/28 | 13/13 | 62/62 | 62/63 | - |
| 3 | 43/43 | 11/11 | 61/61 | **83/83** | 8/8 |
| 4 | 24/24 | 26/26 | 48/48 | **121/121** | 1/1 |
| 5 | 23/23 | 28/28 | 50/50 | 36/36 | 3/3 |
| 6 | 37/37 | 37/38 | 53/53 | 45/47 | 2/2 |

- Vóór ronde W: zone 6 helemaal 0 % (geen weg naar de staart), zone 5 hoofd 21/23, zone 3 41/43, zone 4 21/24, zone 2 27/28.
- **Wat er nog over is:**
  - zone 2 `uitkijk_galmtop`: het uitkijkpunt op de ijsnaald bij de Galmtop. De onderste treden van de wenteltrap sluiten niet aan
    op de grond (2 blokken gat). Het is decor: geen quest, geen teller, alleen een dev-teleport. Kleine generatorfix voor later.
  - zone 6 `zoutgrot_schat` / zijquest `z6_s12_zoutgrot`: achter de kristalmuur die de puzzel in de Zoutgrot tijdens het spel
    opent. Zo bedoeld (de check kent de puzzel niet).
- **Luchtschip-instappen:** elke haven 0,4-3,0 blokken van oog tot romp (was: Lampionhaven 6,4, Opstijgpier 8,4).
- **Deuren:** 196 goed, 33 ijzeren nepdeuren, 0 tegen een muur, 0 met een onbereikbare binnenkant.
- Mob-spawners die de check "niet bereikt" noemt (zone 2/3, in grotten en holle kolommen) zijn geen questdoelen; mobs spawnen
  daar op de vloer die er wel is.

## Nog NIET in een client gezien
De AutoCheck-wandeling maakte schermafdrukken (zie Tests), maar met de hand bekeken heeft nog niemand:
- de dungeondeur, het dungeonvenster, de klaar-check en de wachtrij met nep-spelers;
- de Party-tab (en met twee echte spelers: uitnodigen, leider, party na herstart);
- nep-spelers in de wildernis, rekruteren, en uitzetten in Instellingen;
- het oplopen van de luchtschepen, het vangnet aan de pierpunt, lijn 0 en de Staarthaven;
- de walvistrap, de Kootje-ladder, de nieuwe geheimpjes-routes (sneeuwtrap, dakschacht, plankenloop, tempelbres);
- de vierkante klodders, de Klodderreus en de Blubs in de stad;
- de cutscenes met "De wereld wordt geladen...", F2 en F4.

## Open punten (voor jou)
- **Xp:** zone 2 geeft te veel xp, en wie hoofd + zij doet, zit eind zone 5 al op het plafond 70. Nog niet aangepast (jouw keuze).
- **Engels:** ~1.955 oude 1.9.x-sleutels hebben nog geen Engels (alle Vadsrijk-teksten wel).
- **live.guhs.nl:** de DNS-records (A/AAAA) bestaan, maar het serverdeel (Caddy, LIVE_DATA_PLAN fase A in u-site) is niet gedaan:
  SSH mocht niet. Tot dan zegt de site "Nog niet live".
- **Nep-spelers:** de spawn-aantallen (max 2-3 per zone, vullen na ~1 min) zijn een eerste gok.
- **Dungeons:** elke dungeon is één gedeelde ruimte; groepen wachten op elkaar in de wachtrij.
- **Party:** uitloggen haalt je niet meer uit de party (bewuste gedragswijziging van w-party).
- **Kaart:** het uitkijkpunt Galmtop (zie boven); `poicheck6`/`loopcheck6` lezen nog de ongesnapte cache (checker-fix, geen
  kaartfout).

## Advies voor de release
1. Speel de client volgens `TESTEN_VADSRIJK.md` (W1-W18), vooral de dungeons, de Party-tab en de luchtschepen in survival.
2. Beslis over de xp (zone 2 omlaag, of het plafond later bereiken).
3. Daarna: de release-stappen met jouw OK per stap (server-backup, jar, pack, site: preview weg + `--vadsrijk`, live.guhs.nl fase A
   en B, Discord). Niet gedaan in deze ronde: geen push naar main, geen release, geen server.
4. De oude 1.9.x-teksten zonder Engels kunnen in een aparte Engels-ronde; ze blokkeren de release niet (ze vallen terug op Nederlands).
