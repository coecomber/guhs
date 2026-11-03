# Notities deco-slice: Guhs 1.9 "Een opknapper!" (branch `190-opk-deco`, vanaf `190-opknapper`)

Fase 2: decowinkel, paden en hekjes trekken per tegel, deco kiezen/verplaatsen/draaien, Knusheid-mijlpalen en titels.
Voor de gebruiker: sectie "Deco, paden en hekjes" in `TESTEN_OPKNAPPER.md`.

## Wat er is

- **`DecoWinkel`** (nieuw): de winkelcatalogus uit deco.md (9 paden, 8 hekjes + boogpoortje, en alle "Winkel"-stukken),
  per categorie (Paden, Hekjes, Bloemen, Zitjes, Licht, Water, Beelden, Boerderij, Bijzonder), prijs, Hoeve-level, icoon.
  Bijzondere stukken (bestelling/vondst/extra/Sherlock/Burenhulp) zijn niet te koop. `koop` = Guhlden → decoschuurtje.
- **`Paden`** (nieuw): de regels van het trekken. Paden in kern's padlaag (`Base.paden`/`padSoorten`), bitmask N1 E2 S4 W8
  (`verbindingen`; soorten verbinden onderling). Hekjes = 1x1 decostukken (vormen verbindt ze). Stock eerst, dan Guhlden;
  geld op = streek stopt daar (wat er ligt blijft). Gum: paden → `Base.padVoorraad`, hekjes → decoschuurtje. `wijk`: een
  gebouw/decostuk over paden neemt de tegels op (naar de voorraad). `stap` = 4-verbonden lijn (een diagonaal slepen blijft één pad).
- **`DecoActies`** (nieuw, server): alle `Actie.DECO`-subacties (`DECO_PLAATS/WEG` uit de integratie verhuisd, nieuw
  `DECO_KOOP` (+ vlaggen PLAATS/BON), `DECO_TREK`, `DECO_GUM`, `DECO_VERPLAATS`, `DECO_DRAAI`). Na elke verandering:
  `stempelTegels` (decostuk opnieuw, kale tegel → gras, paden in en rond het vak opnieuw met hoekjes, hekjes/muurtjes/tralies
  eromheen opnieuw verbonden); `rand` na gebouw plaatsen/verplaatsen/draaien (in `Erfenis.acties`).
- **`Knusheid`**: `gebouwen/paden` (opdeling voor de tooltip), `MIJLPALEN` 50..5000 (één keer een berichtje via
  `Erfenis.meldKnus`, dat ook `Base.knusRecord` bijhoudt), drie titels via `Titels.registreer` (zoals Sherlock):
  `erfenis_knus_250` Knusse Guh, `erfenis_knus_1000` Knusvads, `erfenis_knus_2500` Knusser Bestaat Niet — op het record,
  dus opbergen kost nooit een titel. Het moment `knusheid:<totaal>` vuurde de kern al bij elke verandering.
- **`ErfenisDeco.knusheid`**: Knuffelhaag en vadshekje tellen 1 per 4 (deco.md), de andere hekjes 1 per 5.
- **Base**: `padVoorraad`, `knusRecord` (opgeslagen), `decobonnen` (alleen client, uit de sync), `decoPlekFout`,
  `verplaatsDeco`; `plaatsDeco` en `decoWijkt` nemen paden onder het stuk/gebouw op.
- **Client** `DecoBalk` (nieuw) + `OverzichtScherm`: laatste winkeltab "Deco" (categorierij, kaartjes 52x34, gum, Decobon-
  schakelaar), `Modus.DECO` met kopen-bij-neerzetten (spookje blijft), shift-klik = in het schuurtje, `Modus.TREK` (slepen,
  spookjes groen/goud/rood/wit, kostenchip naast de muis), deco kiezen (paneel Verplaatsen/Draaien/Opbergen, slepen = verplaatsen),
  Knusheid-chip klikbaar (→ decowinkel) met opdeling en volgende mijlpaal. Decoschuurtje-venster toont de padvoorraad en
  een knop naar de winkel; hekjes staan er niet meer als losse regels bij "Staat in je dorp" (die gaan met de gum).
- **Dev** (`/guhs erfenis doe ...`): `koop <id> [x z rot]`, `bon <id> x z [rot]`, `trek <soort> x,z;...`, `lijn <soort> x0 z0 x1 z1`,
  `gum x,z;...`, `decoweg x z`, `decodraai x z`, `decoverplaats x z nx nz [rot]`. `ui`: `decowinkel [categorie]`, `gebouwwinkel`,
  `koop <id>`, `trek <soort|gum>`, `streek x,z;...`, `hover x,z`, `decokies x,z`.
- **Teksten**: `tools/features/erfenis_deco.py` (in de features-lijst), padnamen `erfenis.pad.<soort>`.
- **Hekjes in vormen**: bestonden al (nr 10-15, 1x1, soort "hek"); exporter niet aangeraakt.

## Keuzes waar de documenten stil waren

- **Paden weghalen = voorraad**, net als deco: gegumde of door een gebouw/decostuk opgenomen tegels liggen per soort klaar
  en worden bij het volgende trekken van die soort eerst (gratis) gebruikt. Een andere padsoort eroverheen wisselt om
  (de oude tegel naar de voorraad).
- **Hekje niet op een pad**, pad niet onder deco/gebouwen. Deco of een gebouw over een pad: de tegels gaan opzij (voorraad).
- **Kopen = meteen neerzetten** (Hay Day-stijl, betalen bij de klik), shift-klik voor het schuurtje. Ligt hetzelfde stuk al in
  het schuurtje, dan gebruikt het spookje eerst dat.
- **Decobon**: alleen voor een gewoon decostuk dat op jouw Hoeve-level al in de winkel ligt (niet voor pad-/hekjestegels).
- **Mijlpalen/titels**: 250 / 1.000 / 2.500 (FTB-getallen) als titels; berichtjes ook op 50, 100, 500, 1.500, 2.000, 5.000.
- Geen ranglijst, geen halve Knusheid vanaf het 11e (ronde 15/16: niet gekozen).

## Stubs

Geen.

## Testen

- Gametests `DecoGameTests` (batch `erfenis_deco`): catalogus, kopen (+ Decobon), opslag/voorraad/gebouw erover/verplaatsen,
  padHoekjes (bitmask + echte blokken: recht/hoek/kruising/eindje), hekjesVerbinden (in de wereld, en loslaten na gummen),
  knusheid (per 10/5/4, mijlpalen, record, opslaan), streek.
- AutoCheck `tools/autocheck/190_deco.txt` (demodorp, winkel, echte klik + sleep met de muis, streek-spookje, tuintje met
  kruising/hoekjes/hekjes/fontein, gekozen stuk, koop-spookje, schuurtje met padvoorraad, gum-spookje, Knusheid-tooltip).

## Stand bij oplevering

- compileJava groen. `runGameTestServer -Pgt=DecoGameTests,ErfenisGameTests,TaalsleutelsGameTests,VormenGameTests,KoppelingGameTests`:
  **37/37 groen** (7 nieuwe deco-tests).
- AutoCheck `190_deco.txt` 2x gedraaid (0 problemen), alle 12 shots bekeken: winkel (Paden met H7/H9-slotjes, Bloemen-tooltip,
  Hekjes), echte klik op een kaartje + echt slepen met de muis (15 tegels zandpaadje, toast), streek-spookje met kostenchip,
  kruising + T + hoek tussen klinkguhpad/zandpaadje/knarspad, tuintje met roze hekjes (sluiten aan), fontein, bankjes,
  bloemen, lantaarns; gekozen fontein met paneel; koop-spookje; schuurtje; gum-spookje; Knusheid-tooltip.
  Gerepareerd na het kijken: "Hier kan het niet" boven een tegel waar het pad al lag (nu "Dat ligt er al."), toast
  "0 gratis uit je voorraad" (nu alleen als er voorraad gebruikt is), lange namen op kaartjes onleesbaar klein (nu afgekapt met "...").
- Niet gedaan: geen volledige gametest-suite (alleen de relevante batches, volgens de teststrategie).
