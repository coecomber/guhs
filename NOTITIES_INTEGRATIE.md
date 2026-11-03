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

- **Verhaal over het bovenaanzicht**: een praatscherm (Sherlocks proloog, Guhrrit...) dat binnenkomt terwijl het
  bovenaanzicht open is, wacht tot je het sluit (`ErfenisClient`, `ScreenEvent.Opening` op `PraatScherm`), i.p.v. je eruit
  te gooien midden in het bouwen. De makelaarstour praat alleen bij aankomst in het praatscherm; latere stappen als chatregel
  (klik op Guhrrit = de stap nog eens). Guhrrit verschijnt 4,5 blok voor je, iets opzij.

## Stand

- compileJava groen.
- Gametests Erfenis/Vormen/Koppeling/Sherlock/Kaart + Taalsleutels: **43/43 groen** (incl. nieuwe `integratieDeco`).
- Volledige `runGameTestServer` (2310 tests): **7 verplichte rood, geen ervan in erfenis-code**: `gidsminigamesrijen` +
  `gidstekstenbestaan` (kussengevecht), `vadsschaarIsEenSchaar...` (weide_roze_gras), `wereldBescherming` (test verwacht 2
  blokken rand, 1.7.2-hotfix maakte het 1), `ftbquestsChapterInstalls...`, `techquestGebouw`, en de bekende
  `upstreamsamenkindsvoorguhpixel` (SNOES). De branch raakt die code niet (diff t.o.v. main = feature/erfenis + 1 regel in
  gedeelde registraties), dus vrijwel zeker al rood op main; niet nagejaagd. 9 optionele (dimensie bestaat niet op de testserver).
- **AutoCheck** `tools/autocheck/190_opknapper_samen.txt` 4x gedraaid, laatste ronde volledig (21 shots) en bekeken:
  poort zonder brief (melding in het rapport), Postguh, brief + kopen, hekje + Leverkist, aankomst met Guhrrit, bovenaanzicht
  met echte vormen (Hoeve L1 krakkemikkig, plukjes), kippenhok gebouwd + mandje, verbeterd naar L2 (Hoeve 3), Decoschuurtje-
  venster (Kaartjesbord, Borstbeeld, Brievenbus), deco neergezet, demodorp, bestelscherm, wereldkaart met 3 buren op afstand,
  Sherlock-welkom in de chat, vergrootglas-wolkje boven het bureau, Speurboekje, naar huis, terug voor het hekje, Leverkist
  met vlaggetje omhoog. Gerepareerd na het kijken: praatschermen gooiden je uit het bovenaanzicht; Guhrrit stond vlak voor je neus.
- Kleine dingen gezien, niet gerepareerd: na `/guhs erfenis demo` staat de speler soms ín een gebouw (demo stempelt over de
  aankomstplek; alleen dev); het bestelscherm zegt "Al 0 bestellingen geleverd" (telt de kaart-eigen teller, de demo zet alleen
  kern's teller); `use` op de Leverkist opende in de AutoCheck het kistscherm niet (vlaggetje = post is er wel).
## Nog open

- Decowinkel / paden en hekjes trekken (deco.md "Winkel") bestaat niet; alleen gekregen stukken.
- Wereldkaart (M) niet achter de brief.
- Sherlock: `bureauPlek` niet gezet (personages naast de speler); beloningsvlaggen zonder effect (kelder in de Hoeve,
  eindlook bureau, Oma-bestelling, theetaartje-recept).
- Makelaar op vakantie stuurt nog geen ansichtkaartjes per post (alleen de vaste plukjes).
- Makelaarstour heeft geen eigen gametest (hangt aan een echte speler); in de AutoCheck alleen de eerste stap zichtbaar.
