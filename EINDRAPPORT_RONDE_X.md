# Eindrapport ronde X (Guhs 1.10.0 "Het Vadsrijk"): laatste fixes + klaar voor een release met alleen zone 1

Testclient: **`guhs_vadsrijk\x-int\run-client.bat`** (tak `110-x-int`). Nieuwe wereld maken; de client staat op alle zones
open. Spiekbrief: `SPIEKBRIEF.md` (hoofdstuk 1 = je feedback, hoofdstuk 2 = release met alleen zone 1). Afvinklijst:
`TESTEN_RONDE_X.md`. Releasestappen: `RELEASE_CHECKLIST.md` (elke stap wacht op jouw OK).

## Wat er in zit
| Slice | Wat |
|---|---|
| x-labels | geen ruwe sleutels meer (5 Kwartiermeesters, 8 loot-namen, ...), een vangnet in de taal, een test over ~4.100 sleutels |
| x-engels | Engels voor 1.955 oude sleutels (Erfenis, Sherlock, mijnen, FTB) |
| x-dungeon | naamborden bij alle dungeondeuren, Koraalgrot-zeebodem = dichtgegroeid, dungeondeur-item, gloed van de stadsblubs |
| x-zone1 | de zone-poort (`openZones`), "Wordt vervolgd" na zone 1, zacht level-plafond + spaarpot, xp-balans zone 1-6 |
| x-kaart | de walvisingang (poort + trap), zone 6 op de grond (funderingspass), wegen door water, Galmtop-start |
| x-raid | De Sterrendovende Toren: dichte toren, poort = de hele nis, Heraut + Zaalwachter, naamteksten, tracker-regel |
| x-z1grond | zone 1 (+ Knuffeleiland) en zone 2 op de grond (hoogte-checker + funderingspass) |
| integratie | zie hieronder |

## Wat de integratie zelf deed
- Alles gemerged; de taal-conflicten sleutel voor sleutel. De Engelse chunk van x-kaart heette c416, net als die van w-party:
  hernoemd naar c418 (er ging niets verloren). Engels voor x-dungeons 6 nieuwe teksten.
- **De raid-naamteksten** hebben nu dezelfde stijl als de dungeonborden (grote gekleurde naam + kleine witte regel).
- **Galmtop** (regressie van de nieuwe trapstart): de trap komt nu via treden net buiten het platform en een brede opening in
  de reling boven. loopcheck zone 2: VAL 33 -> **30** (basis was 32).
- **Zone 1, sprongen op paden:** de 303 TRAP-meldingen nagelopen. Echt waren alleen de twee stenen zwemtrappen uit de baai bij
  De Haven (aan het eind 1 blok omhoog naar de kade): opgelost (TRAP 215 -> 209). De rest zijn dakpannen, gevels, tentdoek
  en stoelen (trappen als meubel) in huizen.
- **Stadsleven-tests** (`budget`, `speeltuin`) zijn nu betrouwbaar. Ze hingen niet af van de klok of de wereldtijd, maar van
  achtergebleven nep-spelers van andere tests en van te kort wachten op het laden van chunks.
- **De hotfixes 1.9.3 t/m 1.9.7 van main zitten erin** (de 1.10-tak was op 1.9.2 gebouwd). Engels voor hun nieuwe teksten erbij.
- **Twee dev-commando's** om de release te bekijken als op: `/guhs vadsrijk zones alsspeler aan|uit` en
  `/guhs vadsrijk zones zone1klaar`.

## Controles (na de volledige regen van zone 1-6)
| Check | Uitkomst |
|---|---|
| bereikcheck `--strikt` | **exit 0**; zone 1: hoofd 24/24, zij 40/40, npc 52/52, geheim 61/61; zone 2: 28/28, 13/13, 62/62, 63/63; zone 3: 43/43, 11/11, 61/61, 83/83; zone 4: 24/24, 26/26, 48/48, 121/121; zone 5: 23/23, 28/28, 50/50, 36/36; zone 6: 37/37, zij 37/38, npc 54/54, geheim 45/47 (gelijk aan ronde W) |
| verbindcheck | **0** (los 0, fout 0, as 0, gat 0) |
| schepen_check | 23 instappunten, 0 fout |
| kijkcheck5 / kijkcheck6 | OK / OK |
| torencheck (raid) | LEK 0, ZWEEF 0, NAAD 0, deur open |
| funderingcheck zone 1 | GAT 13, ZWEEFT 0, LEK 57 (waterval-voorkant, bewust), BROK 41 (gelijk aan x-z1grond) |
| funderingcheck zone 2 | GAT 27, ZWEEFT 2, LEK 0, BROK 295 (gelijk aan x-z1grond) |
| funderingcheck zone 6 | GAT 0, ZWEEFT 0, LEK 48, BROK 18 (gelijk aan x-kaart) |
| `vadsrijk_w_grot.py --check` | alles goed (7 deuren, alle zegels incl. zone 6, alle bordplekken) |
| poicheck3/4/6 | 0 van 430 / 374 / 334 slecht |
| loopcheck | zone 1: PAD 39, TRAP 209, TRAP- 88, VAL 2 (beide al bekend); zone 2: VAL 30; zone 6: VAL 0, alleen `toren_top` (bewust) |
| zweefcheck | 43 / 20 / 0 / 26 / 129 / 0 (niet slechter) |
| taalcheck | 2.956 sleutels, 0 ontbreken |
| engelscheck `--alles` | 0 zonder Engels, 0 NL gewijzigd, 0 formaat; Vadsrijk-scope OK |

## Tests
- **Volledige suite** (Vadsrijk + alle 1.10-klassen + Erfenis/Sherlock/Spiesburcht): **503 tests, 500 groen**. De 3 rode
  slagen los wel (volgorde-afhankelijk, bekend): Z2Bazen 6/6, Ruiters 3/3, Z3Bazen 6/6.
- **Questrobot:** zone 1-6 41/19/24/23/24/25 gehaald, 0 vast. **Poort 6:** hoofdverhaal zone 1-6 13/12/15/7/10/10.
  **Poort 1:** zone 1 hoofd 13/13, de rest 26/26 (alleen de twee rijdier-terugkeerquests wachten, die horen bij zone 3).
- Gerichte runs tijdens de integratie: 90 tests (89 + de opgeloste speeltuin), daarna 30/30.
- **AutoCheck** (`tools/autocheck/vadsrijk_x_int.txt`): alle zones laden, geen crash; schermafdrukken in
  `run\screenshots\autocheck\xi_*.png`: walvispoort en -trap, raid-poort + heraut, dungeonborden, Kwartiermeester,
  einde zone 1 (tracker + de kapitein zegt "Het schip naar De Splinterpieken vaart binnenkort!").

## Website
- u-site bijgewerkt met ronde X (kaarttegels, runtime-dump, renders). Previews (gh-pages 477b071f2):
  - https://guhs.nl/vadsrijk-preview/ : volledig (946 pagina's);
  - https://guhs.nl/vadsrijk-preview-zone1/ : de publieke release (380 bestanden, **0 lekken**, 0 kapotte links;
    `zones/zone2.html` geeft 404).

## Wat jij nog moet bekijken (niemand heeft het met eigen ogen gezien)
- De walvispoort met het bord (bordtekst in de kaarttegels is nieuw: als het bord leeg is, zeg het).
- De raid-toren (banden, lens), de naamborden (raid + dungeons), de heraut op de trap.
- Zone 1/2 op de grond, de Galmtop-aankomst, de zwemtrappen bij De Haven.
- Het einde van zone 1 als speler (SPIEKBRIEF 2.2) en het level-plafond.

## Open punten (bewust niet gedaan)
- Zone 1 loopcheck VAL 2: de vuurtorenkelder (ladder + luik, de checker ziet het luik niet als klimbaar) en een 1-cel-nis
  naast de wenteltrap in een paddenstoelsteel in Zwamzoet (83,95,27): je kunt er alleen uit door te breken.
- Zone 6: zij 37/38, geheim 45/47 (al zo sinds ronde W). Zone 6 LEK 48 (een treetje in de zeespiegel aan de westrand).
- Met poort 1 lopen hoofd+zij-spelers 2-5 levels voor (x-zone1); de spaarpot vangt dat op.
- `gradle.properties` staat op 1.9.7: de versie 1.10.0 hoort bij de release-tak (RELEASE_CHECKLIST stap 0).
- live.guhs.nl: de Caddy-kant (fase A) vraagt SSH.
