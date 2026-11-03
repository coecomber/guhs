# Notities afwerking: Guhs 1.9 "Een opknapper!" (branch `190-opk-afwerking`)

Fase 2: de "open"-lijst van `NOTITIES_INTEGRATIE.md` (behalve decowinkel/paden: dat doet de deco-slice). Voor de gebruiker:
`TESTEN_OPKNAPPER.md` (bijgewerkt).

## Wat er nu is

- **Wereldkaart (M) pas na de brief**: `KaartServer.stuur` vraagt `Erfenis.magErfenis` (brief gekocht, creative, of al in de
  dimensie), net als het bovenaanzicht. Zonder: actiebalk "Je hebt nog geen erfenis, njeg..." (geldt ook voor `/guhs erfenis kaart`).
- **Sherlock bij het bureau** (`Afwerking.bureauDeur`, `Sherlock.bureauPlek` + nieuw `Sherlock.bureauUit`): de deur wordt in de
  wereld gezocht (onderste deurhelft in de voetafdruk, "naar buiten" = de kant weg van het midden). Geen deur (L1 is een open
  kraampje, of de vorm staat nog in de rij): het midden van de voorkant (zuid bij rot 0, draait mee). `Cast.bijSpeler` zet de
  personages in een halve cirkel vóór die deur als je in je dorp binnen 48 blokken bent; anders naast je zoals eerst.
  Getuigen op hun vaste tegel blijven waar ze waren.
- **Sherlocks beloningen doen iets** (`Afwerking.beloning`, gespiegeld in `Base.beloningen`, opgeslagen + naar de client;
  `Afwerking.spiegel` haalt bij elke sync vlaggen in die Sherlock al eerder gaf):
  - `recept_theetaartje` (zaak 2): Bakkerij-recept **Opa's theetaartjes**: meel + ei + honing + melk → 2 koek. Verborgen in
    het paneel en geweigerd door `Base.kies` (`erfenis.fout.recept_geheim`) zolang je het niet hebt (`Productie.GEHEIM`).
  - `oma_bestelling` (zaak 4): Oma Breiguh rijdt mee met 1 op de 3 gewone karretjes (nooit de sterbestelling): kaartje heet
    "Oma Breiguh (dorp)", +50% Guhlden, en bij leveren een extraatje (1 op 4 een Decobon, anders 25 + 1/5 van de beloning).
    (`Bestelling.oma`, `Omgeving.oma()` met standaard false: de kaart-tests hoeven niks.)
  - `opas_kelder` (zaak 5): **Opa's kelder** onder de Hoeve (`OpasKelder`): 7x7x3 kaaskelder, 5 diep, luik in de vloer van
    de Hoeve (midden van een zijmuur van de kelder; geen plek op de vloer → in het erf), ladder omlaag. Dagboek van Opa op
    de lessenaar, tonnen met kaarsjes, kaasknabbelblokken + gatenkaas, boekenkasten, versierde potten "voor je vondsten",
    roze/rood kleed, hanglantaarn, bordje "Opa's kelder / Kaas en knabbels / Geen dassen! / Njeg!". Je krijgt het
    dagboek ook als boek.
  - `bureau_eindlook` (rechtszaak): het Detectivebureau wordt gestempeld met de L10-vorm, op elk level (`Stempel.vorm`).
  - `bertha_optreden` (zaak 6): Bertha de koe (naambordje "Bertha (zangeres)") staat op het podium van elke Muziektent naast
    de zanger; af en toe muzieknootjes en een trotse boe. Decoratief zoals de vorm-wezens (getagd, geen AI, onkwetsbaar).
  - `testament` (zaak 9): **Opa Knabbels testament** als boek (3 bladzijden, met pootafdrukje-handtekening).
  - `paaltjes_terug`: alleen de melding (er werden nooit echt paaltjes verzet; niks om terug te zetten).
  Kelder/eindlook/Bertha worden meteen opnieuw gestempeld als er iemand in het dorp is, anders bij het volgende bezoek.
- **Ansichtkaartjes van de makelaar** (`Afwerking.opMoment`): na de tour (Hoeve 2) stuurt Guhrrit er 5, bij Hoeve 3, 4, 5, 6
  en 7: Guhcapulco, de Knabbelalpen, Parijs-sur-Guh, de Guhcruise, Guhwaii (de laatste: "ik kom gauw terug", hij is bij zaak
  10 terug met sombrero). De **Postguh** brengt hem (zelfde pad als de brief, nu `Makelaarsbrief.bezorgPost`; ook in het dorp;
  niet tijdens een scène/rit/slaap: dan zonder Postguh): een papiertje "Ansichtkaart uit ..." met de tekst als lore, de tekst
  ook in de chat, de kaart in het vondstenkistje. Met de 3 gevonden kaartjes (plukjes) zijn het er 8 = het **Kaartjesbord**
  (deco.md #64): +2 Knusheid per extra kaartje als het bord staat. Het bord komt bij het eerste kaartje (gevonden of per post)
  en nooit een tweede (ook niet als het al neerstaat: dat was een bugje).
- **Fixes**:
  - Na `/guhs erfenis demo` (en elke aankomst) sta je nooit meer ín een gebouw: `Erfenis.veiligeAankomst` zoekt op de tegels
    (de vorm staat nog in de rij, de heightmap weet het nog niet) de dichtste vrije open tegel zonder deco; `demo` zet wie er
    staat daarheen.
  - Bestelscherm "Al n bestellingen geleverd": het grootste van de kaart-teller en kern's `Base.bestellingen`.
  - Leverkist: de rechtsklik werkte wel; de AutoCheck sloot het kistscherm zelf (geen `gui keep` voor `use`). Script
    `190_opknapper_samen.txt` aangepast; in `190_afwerking.txt` gecontroleerd met een screenshot.
- **Dev-commando's** (op): `/guhs erfenis ansicht <4-8>` (kaartje nu, ook opnieuw), `/guhs erfenis beloning <vlag>` (een
  Sherlock-beloning meteen, met suggesties), `/guhs erfenis kelder` (naar Opa's kelder).
- **Gametests** `AfwerkingGameTests` (batch `erfenis_afwerking`): `makelaarstour` (elke stap alleen op zijn eigen moment, in
  volgorde; Hoeve 2 eerder = meteen klaar, maar niet vóór aankomst; afscheidscadeau één keer; daarna de 5 ansichtkaartjes
  met bord en Knusheid), `sherlockBeloningen` (theetaartje geheim/gekozen, opslaan, testament + dagboek als boek, vlag één
  keer, stempelen bij volgend bezoek), `omaBestelt` (vaak, niet altijd, nooit de ster, opgeslagen), `opasKelder` (luik in de
  vloer, ladder die blijft hangen, ruimte, dagboek op de lessenaar, twee keer stempelen = hetzelfde).
- **Teksten**: `tools/features/erfenis_afwerking.py` (52 sleutels, NL; en_us dezelfde tekst), in de featurelijst.

## Stand

- compileJava groen. Gametests `-Pgt=ErfenisGameTests,AfwerkingGameTests,KaartGameTests,SherlockGameTests,KoppelingGameTests,
  VormenGameTests,TaalsleutelsGameTests` groen (na de fixes hieronder opnieuw: Afwerking/Erfenis/Kaart/Taalsleutels groen).
- AutoCheck `tools/autocheck/190_afwerking.txt` 2x gedraaid, shots bekeken (`run/screenshots/autocheck/afw_*`): kaart geweigerd
  zonder brief, Leverkist-scherm open na `use`, demo: speler buiten op een gangetje, bestelscherm "Al 40 bestellingen
  geleverd", Sherlock + Vadson voor de rode deur van het bureau, ansichtkaart (Postguh-regel + tekst in de chat, kaartje in
  de hotbar), Bakkerij met 5e recept (theetaartje), Bertha met naambordje op het podium naast de zanger, Opa's kelder
  (lessenaar met dagboek, bordje, kaarsjes op de ton, kaas, potten, boekenkasten, ladder naar het luik).
  Gerepareerd na het kijken: `GebouwVormen.daarna` gaf een NPE (crashte de client bij kelder/Bertha), de deur-kant van het
  bureau (personages stonden achter het bureau; nu: de kant zonder dak), aankomstplek liefst met vrije tegels rondom,
  bordtekst te lang. Eindlook: in de shot lastig te zien, daarom een eigen gametest (`bureauEindlook`: de L10-vorm is hoger).

## Voor-bestaande rode tests (full suite)

Op een tijdelijke worktree van main (v1.8.1, `922ed8cf1`, daarna weer weggehaald) gedraaid met
`-Pgt=GidsGameTests,VadsSchaarGameTests,WereldGameTests,GuhGameTests,TechquestGameTests,UpstreamSamenGameTests`:
**alle 7 falen ook op main**, met dezelfde meldingen: `gidsminigamesrijen` + `gidstekstenbestaan` (rij kussengevecht),
`vadsschaarIsEenSchaarDieNooitKapotGaat` (weide_roze_gras), `wereldBescherming` ("two blocks around it"),
`ftbquestsChapterInstallsOnceAndMergesTheTexts` (hoofdstukkenlijst; de FTB-slice repareerde dit al in 190-opknapper),
`techquestGebouw` ("the building stays whole"), `upstreamsamenkindsvoorguhpixel` (SNOES zonder Guhdex-pagina). Onze
branch veroorzaakt er geen; niet gerepareerd (buiten de opknapper).

## Keuzes waar de documenten stil waren

- Ansichtkaartjes: 5 per post (Hoeve 3-7) + 3 gevonden = de 8 van het Kaartjesbord. Alleen na de tour; geen kaartje dubbel.
- Oma: 1 op 3 gewone karretjes, +50%, extraatje 1 op 4 een Decobon (past bij de decowinkel), anders een zakje Guhlden.
- Theetaartje: geen nieuw goed (de goed-ids liggen vast): een beter koek-recept (2 koek per baksel, kost ook melk).
- Kelder: ingang = luik in de Hoeve-vloer (de Hoeve-vormen hebben op L6/L8 geen vrij erf binnen de voetafdruk). Wordt de Hoeve
  verplaatst, dan blijft de oude kelder dicht onder de grond (luik weg, niks kapot); de nieuwe komt onder de nieuwe plek.
- Bertha staat er altijd (er bestaan geen dorpsfeestjes): "treedt voortaan op" = vaste zangeres op de Muziektent.
- Bureau-eindlook = de L10-vorm, ook als het bureau lager is; het level zelf (effect, kosten) blijft gewoon.

## Gedeelde bestanden / buiten mijn map aangeraakt (klein)

`Base` (veld `beloningen`, `ansichtkaarten()`, `kaartjesbordErbij()`, Knusheid van het bord, geheim recept in `kies`),
`Productie` (recept + `GEHEIM`/`magRecept`), `Stempel.vorm` (eindlook + `Afwerking.naStempel`), `Erfenis` (`veiligeAankomst`,
`sync` → `spiegel`), `ErfenisCommando.demo`, `ErfenisFeature` (1 regel), `client/GebouwPaneel` (geheim recept verbergen),
`vorm/GebouwVormen` (`daarna`: een klus achter de stempel-rij), `vorm/DecoWezens` (Bertha's nootjes),
`koppeling/Makelaarsbrief` (`bezorgPost`), `kaart/KaartServer` + `kaart/Bestellingen` + `kaart/Bestelling` (oma, teller, toegang),
`sherlock/Sherlock` + `sherlock/Cast` (bureau-plek, beloning-haak), `tools/features/__init__.py` (1 regel),
`tools/autocheck/190_opknapper_samen.txt` (`gui keep`). Niets in deco/paden/bovenaanzicht-UI.
