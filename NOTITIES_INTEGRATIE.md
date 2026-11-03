# Notities integratie: Guhs 1.9 "Een opknapper!" (branch `190-opknapper`)

De vijf slices (kern, vormen, koppeling, sherlock, kaart) samen tot één spel. Voor de gebruiker: `TESTEN_OPKNAPPER.md`.

## Wat ik veranderd heb

- **Compileren**: enige breuk was de kaart-stub `ErfenisHulp` (POC-`Base.bouw`). Vormen' `minHoek` = grondblok (y = 0 van de
  vorm), net als kern aannam (`Plot.Y`, gebouw vanaf `minHoek.above()`): geen verschuiving nodig.
- **`ErfenisHulp.versnel`** (echt): de klus die het eerst klaar is (gebouw, verbetering, plukje) tot `minuten` sneller, max 25%
  van zijn tijd; is er geen klus, dan maakt het eerste bron-gebouw met ruimte die minuten productie meteen (max 25% mandje).
- **`ErfenisDeco`** (echt): `geef` zet het stuk in `Base.decoOpslag` onder het **vormen-id**; `vorm(id)` vertaalt de namen van
  de slices (`mika_schrik`→`mikaschrik`, `knabbelbessenstruik`→`knabbelbessentobbe`, `de_eerste_knabbel`→`eerste_knabbel`,
  `mammoetje_van_heg`→`mammoetje_heg`, kern's vondsten `weerhaan`/`melkkar`→`roestige_weerhaan`/`oude_melkkar`).
  `knusheid(deco)` = Knusheid per stuk uit deco.md (hekjes 1 per 5).
- **13 nieuwe deco-vormen** in `renders-erfenis/_bron/erfdeco.py` (nr 68-80, zelfde stijl) en opnieuw geëxporteerd:
  `burenhulp_bankje`, `burenhulp_vlaggetjes` en Sherlocks 11 (`hartjesnest`, `theekransje_tafeltje`, `uilenhuisje`,
  `reuzensjaal`, `koeienbel_lantaarn`, `moestuintje_pim`, `glimwormguhtjes`, `oudste_boompje`, `getuigenbank`,
  `standbeeld_opa_knabbel`, `knabbelparkinkje`). Export van alle gebouwen bleef byte-gelijk; index.json heeft nu 73 deco.
  (Let op: `export_vormen.py <uit> <naam>` schrijft een index.json met alleen die gebouwen: altijd zonder namen draaien.)
- **Deco neerzetten** (nieuw, kleinste versie): knop **Deco (n)** in het bovenaanzicht → venster Decoschuurtje (Neerzetten /
  Opbergen), spookje + R draaien (`Modus.DECO`), actie `Actie.DECO` (`DECO_PLAATS` / `DECO_WEG`), `Base.plaatsDeco/
  kanDeco/bergDecoOp/decoOp`; een gebouw dat geplaatst/verplaatst/gedraaid wordt over deco zet die terug in het schuurtje.
  Namen: `erfenis.deco.<id>` voor de hele catalogus, gegenereerd uit index.json (`erfenis.py`).
- **Makelaarstour** (`Makelaarstour.java`, nieuw): de intro tot Hoeve 2 uit het ontwerp, op kern's momenten, met Sherlocks
  `Guhrrit` (verschijn/zeg/bijKlik). Stap in `Base.tutorial` (opgeslagen). Afscheid: `borstbeeld_makelaar` + `brievenbus` in het
  schuurtje. Buiten het dorp komt de stap als chatregel.
- **Toegang**: `Erfenis.magErfenis` = brief gekocht (`Makelaarsbrief.gekocht`) of creative of al in de dimensie; geldt voor
  de bovenaanzicht-toets buiten de dimensie, de "ga erheen"-vraag en `/guhs erfenis`. Dev-subcommando's blijven werken.
- **Naar huis**: na kern's eigen tegoed gaat de lijst door `LeverkistPlafond.beperk` (laatste woord); wat niet mag gaat terug in
  de opslag (+ tegoed terug). `guhs:glasscherf` bestaat nu, dus glasscherven mogen mee.
- **Sherlock**: `Sync` stuurt `ZaakKlaar` (`Sherlock.zaakKlaar`) mee → vergrootglas-wolkje boven het Detectivebureau in het
  bovenaanzicht (klik = `Actie.BUREAU` → `Sherlock.openBureau`). Rechtsklik in de wereld op het eigen bureau opent het
  Speurboekje, op het Marktplein de bestellingen. Kern's momenten (incl. `vondst:roestig_sleuteltje`) gingen al via
  `ErfenisMomenten` naar Sherlocks luisteraar.
- **Kaart**: `hoeve` = `hoeveLevel()`, Marktplein = `besteLevel(MARKTPLEIN)`, plan mapt 1:1 op kern-ids (POC-terugval weg),
  `npcWeg` → nieuwe `ErfenisData.verwijder(UUID)` (geen reflectie meer); een nieuwe speler krijgt zijn cel via
  `KaartLeven.spelerPlek` (≥ 3 cellen van elk dorp); een levering telt via `ErfenisEconomie.bestellingGeleverd` (Hoeve-doelen +
  moment met kern's totaal). `Handje.BURENSTUKKEN` publiek (voor de test).
- **Dev**: `/guhs erfenis doe ...` (zelfde serverpad als een klik, voor AutoCheck), `ui venster deco`, `ui deco <id>`.
- **Opgeruimd**: 39 oude POC-taalsleutels (stadhuis/schuurtje/muur/boompje, Bouwspul, aanvallen, `.de`-vormen, POC-nieuws),
  `Base.stadhuisLevel`, `GebouwType.MAX_STADHUIS`, ongebruikte `ErfenisData.maakNpcBase(plan)`. `GebouwType.van` houdt
  `stadhuis`/`schuurtje` als alias om POC-saves te laden.
- **Gametest** `ErfenisGameTests.integratieDeco`: alle deco-ids van alle slices hebben een vorm; neerzetten/Knusheid/overlap;
  gebouw erover → terug in het schuurtje.

## Stand

- compileJava groen.
- Gametests Erfenis/Vormen/Koppeling/Sherlock/Kaart (+ Taalsleutels): zie het rapport/commit; volledige run: zie onder.
- **AutoCheck NIET gedraaid** (gebruikersregel tijdens het werk: geen gameclient). Het script staat klaar:
  `gradlew runAutocheckClient -PautocheckScript=tools/autocheck/190_opknapper_samen.txt` (survival-poort → brief → kopen →
  hekje + Leverkist → aankomst → bovenaanzicht → bouwen/opruimen/ophalen/verbeteren → opslag/decoschuurtje → deco
  neerzetten → demodorp → bestellingen → kaart → Sherlock-welkom, wolkje, Speurboekje → naar huis → terug → Leverkist).
  Nog doen: draaien en alle 21 shots bekijken (vooral: Deco-venster/-knop layout, het wolkje, Guhrrits praatscherm bij aankomst).

## Nog open

- Decowinkel / paden en hekjes trekken (deco.md "Winkel") bestaat niet; alleen gekregen stukken.
- Wereldkaart (M) niet achter de brief.
- Sherlock: `bureauPlek` niet gezet (personages naast de speler); beloningsvlaggen zonder effect (kelder in de Hoeve,
  eindlook bureau, Oma-bestelling, theetaartje-recept).
- Makelaar op vakantie stuurt nog geen ansichtkaartjes per post (alleen de vaste plukjes).
- Makelaarstour heeft geen eigen gametest (hangt aan een echte speler); in de AutoCheck alleen de eerste stap zichtbaar.
